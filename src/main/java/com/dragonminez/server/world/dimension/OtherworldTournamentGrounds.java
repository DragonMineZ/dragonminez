package com.dragonminez.server.world.dimension;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.server.world.npc.NPCPlacementManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class OtherworldTournamentGrounds {

	public static final ResourceLocation SCHEMATIC = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "schematics/otherworld_tournament.schem");
	public static final int REVISION = 1;
	public static final BlockPos RING_CENTER = new BlockPos(0, 355, -1200);
	public static final Vec3 ARRIVAL = new Vec3(-25.5, 352, -1161.5);
	public static final float ARRIVAL_YAW = 180.0F;
	public static final Vec3 GRAND_KAI_RETURN = new Vec3(-30.5, 150, 10.5);
	public static final float GRAND_KAI_RETURN_YAW = 0.0F;
	public static final String ANNOUNCER_PLACEMENT_ID = "master_otherworld_announcer";

	private static final long TICK_BUDGET_NANOS = 15_000_000L;
	private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
	private static final int MAX_PALETTE = 1 << 14;
	private static final int MAX_HEIGHT = 1 << 10;
	private static final int LOOKAHEAD_COLUMNS = 8;
	private static final TicketType<ChunkPos> BUILD_TICKET =
			TicketType.create(Reference.MOD_ID + "_tournament_build", Comparator.comparingLong(ChunkPos::toLong));

	private static Build active;
	private static ServerLevel failedLevel;

	private OtherworldTournamentGrounds() {
	}

	public static boolean isBuilt(ServerLevel level) {
		return Data.get(level).revision >= REVISION;
	}

	public static BoundingBox protectedBounds(Level level) {
		if (!(level instanceof ServerLevel serverLevel) || !serverLevel.dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) return null;
		return Data.get(serverLevel).bounds;
	}

	public static boolean isProtected(Level level, BlockPos pos) {
		BoundingBox bounds = protectedBounds(level);
		return bounds != null && bounds.isInside(pos);
	}

	public static void tick(ServerLevel level) {
		if (!level.dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) return;
		if (!ConfigManager.getServerConfig().getWorldGen().getOtherworldActive()) return;
		Data data = Data.get(level);
		if (failedLevel == level) return;
		if (data.revision >= REVISION) {
			if (data.bounds == null) {
				Build built = Build.load(level);
				if (built == null) {
					failedLevel = level;
					return;
				}
				data.bounds = built.bounds;
				data.setDirty();
			}
			return;
		}

		if (active == null || active.level != level) {
			active = Build.load(level);
			if (active == null) {
				failedLevel = level;
				return;
			}
			active.ticketedEnd = data.nextChunk;
			data.bounds = active.bounds;
			data.setDirty();
			LogUtil.info(Env.SERVER, "Otherworld tournament: building {} chunk column(s) around {}, starting at column {}",
					active.chunks.size(), RING_CENTER, data.nextChunk);
		}

		active.requestAhead(data.nextChunk);
		long deadline = System.nanoTime() + TICK_BUDGET_NANOS;
		while (System.nanoTime() < deadline) {
			if (data.nextChunk >= active.chunks.size()) {
				finish(level, data);
				return;
			}
			if (!active.isReady(data.nextChunk)) return;
			if (!active.place(data.nextChunk, deadline)) return;
			active.release(data.nextChunk);
			data.nextChunk++;
			data.setDirty();
			active.requestAhead(data.nextChunk);
		}
	}

	private static int buildPercent(ServerLevel level) {
		Build build = active;
		if (build == null || build.level != level || build.chunks.isEmpty()) return 0;
		return Math.min(99, Data.get(level).nextChunk * 100 / build.chunks.size());
	}

	private static void finish(ServerLevel level, Data data) {
		data.revision = REVISION;
		data.nextChunk = 0;
		data.setDirty();
		active = null;
		LogUtil.info(Env.SERVER, "Otherworld tournament: grounds built around {}", RING_CENTER);
		NPCPlacementManager.spawnForLevel(level);
	}

	public static void teleportFromGrandKai(ServerPlayer player) {
		ServerLevel otherworld = player.server.getLevel(OtherworldDimension.OTHERWORLD_KEY);
		if (otherworld == null) return;
		if (!isBuilt(otherworld)) {
			player.displayClientMessage(Component.translatable("message.dragonminez.grandkai.tournament_not_ready", buildPercent(otherworld)), true);
			return;
		}
		player.teleportTo(otherworld, ARRIVAL.x, ARRIVAL.y, ARRIVAL.z, ARRIVAL_YAW, 0.0F);
	}

	public static void teleportBackToGrandKai(ServerPlayer player) {
		ServerLevel otherworld = player.server.getLevel(OtherworldDimension.OTHERWORLD_KEY);
		if (otherworld == null) return;
		player.teleportTo(otherworld, GRAND_KAI_RETURN.x, GRAND_KAI_RETURN.y, GRAND_KAI_RETURN.z, GRAND_KAI_RETURN_YAW, 0.0F);
	}

	private static final class Build {
		private final ServerLevel level;
		private final BlockState[] palette;
		private final int minY;
		private final BoundingBox bounds;
		private final List<ChunkColumn> chunks;
		private int ticketedEnd;
		private int columnCursor;

		private record ChunkColumn(int chunkX, int chunkZ, IntArrayList entries) {
		}

		private Build(ServerLevel level, BlockState[] palette, BoundingBox bounds, List<ChunkColumn> chunks) {
			this.level = level;
			this.palette = palette;
			this.minY = bounds.minY();
			this.bounds = bounds;
			this.chunks = chunks;
		}

		private static Build load(ServerLevel level) {
			Optional<Resource> resource = level.getServer().getResourceManager().getResource(SCHEMATIC);
			if (resource.isEmpty()) {
				LogUtil.error(Env.SERVER, "Otherworld tournament: schematic {} not found, the grounds will not be built", SCHEMATIC);
				return null;
			}
			try (InputStream in = resource.get().open()) {
				return parse(level, NbtIo.readCompressed(in));
			} catch (Exception e) {
				LogUtil.error(Env.SERVER, "Otherworld tournament: failed to read schematic {}", SCHEMATIC, e);
				return null;
			}
		}

		private static Build parse(ServerLevel level, CompoundTag root) {
			CompoundTag schematic = root.contains("Schematic", Tag.TAG_COMPOUND) ? root.getCompound("Schematic") : root;
			int version = schematic.getInt("Version");
			int width = schematic.getShort("Width") & 0xFFFF;
			int height = schematic.getShort("Height") & 0xFFFF;
			int length = schematic.getShort("Length") & 0xFFFF;

			CompoundTag paletteTag;
			byte[] data;
			int[] offset;
			if (version >= 3) {
				CompoundTag blocks = schematic.getCompound("Blocks");
				paletteTag = blocks.getCompound("Palette");
				data = blocks.getByteArray("Data");
				offset = schematic.getIntArray("Offset");
			} else {
				paletteTag = schematic.getCompound("Palette");
				data = schematic.getByteArray("BlockData");
				CompoundTag meta = schematic.getCompound("Metadata");
				offset = new int[]{meta.getInt("WEOffsetX"), meta.getInt("WEOffsetY"), meta.getInt("WEOffsetZ")};
			}
			if (offset.length < 3) offset = new int[3];
			if (height > MAX_HEIGHT) throw new IllegalStateException("schematic is taller than " + MAX_HEIGHT + " blocks");

			int paletteSize = 0;
			for (String key : paletteTag.getAllKeys()) paletteSize = Math.max(paletteSize, paletteTag.getInt(key) + 1);
			if (paletteSize > MAX_PALETTE) throw new IllegalStateException("schematic palette has more than " + MAX_PALETTE + " states");
			BlockState[] palette = new BlockState[paletteSize];
			for (String key : paletteTag.getAllKeys()) palette[paletteTag.getInt(key)] = parseState(key);

			BlockPos min = RING_CENTER.offset(offset[0], offset[1], offset[2]);
			Long2ObjectOpenHashMap<IntArrayList> buckets = new Long2ObjectOpenHashMap<>();
			int layer = width * length;
			int volume = layer * height;
			int index = 0;
			int cursor = 0;
			while (cursor < data.length && index < volume) {
				int value = 0;
				int shift = 0;
				byte b;
				do {
					b = data[cursor++];
					value |= (b & 0x7F) << shift;
					shift += 7;
				} while ((b & 0x80) != 0);

				BlockState state = value < palette.length ? palette[value] : null;
				if (state != null && !state.isAir() && !state.is(Blocks.STRUCTURE_VOID)) {
					int y = index / layer;
					int rest = index % layer;
					int worldX = min.getX() + rest % width;
					int worldZ = min.getZ() + rest / width;
					long key = ChunkPos.asLong(worldX >> 4, worldZ >> 4);
					IntArrayList bucket = buckets.get(key);
					if (bucket == null) {
						bucket = new IntArrayList();
						buckets.put(key, bucket);
					}
					bucket.add((worldX & 15) | (worldZ & 15) << 4 | y << 8 | value << 18);
				}
				index++;
			}

			List<ChunkColumn> chunks = new ArrayList<>(buckets.size());
			for (Long2ObjectOpenHashMap.Entry<IntArrayList> entry : buckets.long2ObjectEntrySet()) {
				ChunkPos pos = new ChunkPos(entry.getLongKey());
				chunks.add(new ChunkColumn(pos.x, pos.z, entry.getValue()));
			}
			chunks.sort(Comparator.comparingInt(ChunkColumn::chunkZ).thenComparingInt(ChunkColumn::chunkX));
			BoundingBox bounds = new BoundingBox(min.getX(), min.getY(), min.getZ(),
					min.getX() + width - 1, min.getY() + height - 1, min.getZ() + length - 1);
			return new Build(level, palette, bounds, chunks);
		}

		private static BlockState parseState(String key) {
			try {
				return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), key, false).blockState();
			} catch (CommandSyntaxException e) {
				LogUtil.warn(Env.SERVER, "Otherworld tournament: unknown block state '{}' in schematic, skipping it", key);
				return Blocks.AIR.defaultBlockState();
			}
		}

		private void requestAhead(int from) {
			int end = Math.min(chunks.size(), from + LOOKAHEAD_COLUMNS);
			while (ticketedEnd < end) {
				ChunkPos pos = chunkPos(ticketedEnd++);
				level.getChunkSource().addRegionTicket(BUILD_TICKET, pos, 0, pos);
			}
		}

		private void release(int chunkIndex) {
			ChunkPos pos = chunkPos(chunkIndex);
			level.getChunkSource().removeRegionTicket(BUILD_TICKET, pos, 0, pos);
		}

		private boolean isReady(int chunkIndex) {
			ChunkColumn column = chunks.get(chunkIndex);
			return level.getChunkSource().getChunkNow(column.chunkX(), column.chunkZ()) != null;
		}

		private ChunkPos chunkPos(int chunkIndex) {
			ChunkColumn column = chunks.get(chunkIndex);
			return new ChunkPos(column.chunkX(), column.chunkZ());
		}

		private boolean place(int chunkIndex, long deadline) {
			ChunkColumn column = chunks.get(chunkIndex);
			int baseX = column.chunkX() << 4;
			int baseZ = column.chunkZ() << 4;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			IntArrayList entries = column.entries();
			while (columnCursor < entries.size()) {
				int packed = entries.getInt(columnCursor++);
				pos.set(baseX + (packed & 15), minY + (packed >>> 8 & 0x3FF), baseZ + (packed >>> 4 & 15));
				level.setBlock(pos, palette[packed >>> 18], PLACE_FLAGS);
				if ((columnCursor & 511) == 0 && columnCursor < entries.size() && System.nanoTime() >= deadline) return false;
			}
			columnCursor = 0;
			return true;
		}
	}

	public static final class Data extends SavedData {
		private static final String FILE_NAME = "dragonminez_otherworld_tournament";

		private int revision;
		private int nextChunk;
		private BoundingBox bounds;

		public static Data get(ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(Data::load, Data::new, FILE_NAME);
		}

		public static Data load(CompoundTag tag) {
			Data data = new Data();
			data.revision = tag.getInt("Revision");
			data.nextChunk = tag.getInt("NextChunk");
			int[] bounds = tag.getIntArray("Bounds");
			if (bounds.length == 6) data.bounds = new BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			tag.putInt("Revision", revision);
			tag.putInt("NextChunk", nextChunk);
			if (bounds != null) {
				tag.putIntArray("Bounds", new int[]{bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()});
			}
			return tag;
		}
	}
}
