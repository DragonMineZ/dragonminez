package com.dragonminez.server.world.worldboss;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.block.custom.DemonRealmBlocks;
import com.dragonminez.common.init.entities.worldboss.AllWorldBossesEntity;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.server.world.data.StructurePlanSavedData;
import com.dragonminez.server.world.dimension.DemonRealmDimension;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import com.dragonminez.server.world.feature.DemonRealmFeatures;
import com.dragonminez.server.world.gen.DemonRealmGeneration;
import com.dragonminez.server.world.structure.BossStructures.GeteStarShape;
import com.dragonminez.server.world.structure.BossStructures.GeteStarStructure;
import com.dragonminez.server.world.structure.helper.DMZStructureSets;
import com.dragonminez.server.world.structure.placement.StructureSpawnPlanner;
import com.dragonminez.server.world.structure.BossStructures.TreeOfMightShape;
import com.dragonminez.server.world.structure.BossStructures.TreeOfMightShapes;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

public interface WorldBossLair {
	String key();

	String displayName();

	ResourceKey<Level> dimension();

	boolean isReady(ServerLevel level);

	BlockPos pickColumn(ServerLevel level);

	boolean isAreaLoaded(ServerLevel level, BlockPos column);

	BlockPos resolveGround(ServerLevel level, BlockPos column);

	void buildArena(ServerLevel level, BlockPos ground);

	WorldBossEntity createBoss(ServerLevel level);

	default long respawnTicks() {
		return 72000L;
	}

	default BlockPos teleportTarget(ServerLevel level, BlockPos lair) {
		return lair;
	}

	final class Janemba implements WorldBossLair {
		private static final int LAIR_DISTANCE = 1000;
		private static final long LAIR_SALT = 0x4A414E454D4241L;
		private static final int HELL_SCAN_TOP = 92;
		private static final int HELL_SCAN_BOTTOM = -50;

		private static final int FLOOR_RADIUS = 16;
		private static final int RIM_RADIUS = 22;
		private static final int HEADROOM = 22;
		private static final int FOUNDATION_DEPTH = 4;
		private static final int RIM_HEIGHT = 7;
		private static final int RING_SPIKES = 18;
		private static final int INNER_SPIKES = 9;

		@Override
		public String key() {
			return WorldBossEntity.JANEMBA;
		}

		@Override
		public String displayName() {
			return "Janemba";
		}

		@Override
		public ResourceKey<Level> dimension() {
			return OtherworldDimension.OTHERWORLD_KEY;
		}

		@Override
		public boolean isReady(ServerLevel level) {
			return true;
		}

		@Override
		public BlockPos pickColumn(ServerLevel level) {
			RandomSource random = RandomSource.create(level.getSeed() ^ LAIR_SALT);
			double angle = random.nextDouble() * Math.PI * 2.0D;
			int x = Mth.floor(Math.cos(angle) * LAIR_DISTANCE);
			int z = Mth.floor(Math.sin(angle) * LAIR_DISTANCE);
			return new BlockPos(x, HELL_SCAN_TOP, z);
		}

		@Override
		public boolean isAreaLoaded(ServerLevel level, BlockPos column) {
			for (int dx = -RIM_RADIUS; dx <= RIM_RADIUS; dx += 16) {
				for (int dz = -RIM_RADIUS; dz <= RIM_RADIUS; dz += 16) {
					if (!level.isLoaded(column.offset(dx, 0, dz))) return false;
				}
			}
			return level.isLoaded(column);
		}

		@Override
		public BlockPos resolveGround(ServerLevel level, BlockPos column) {
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			for (int y = HELL_SCAN_TOP; y >= HELL_SCAN_BOTTOM; y--) {
				cursor.set(column.getX(), y, column.getZ());
				if (!level.getBlockState(cursor).isAir()) return new BlockPos(column.getX(), y, column.getZ());
			}
			return null;
		}

		@Override
		public void buildArena(ServerLevel level, BlockPos center) {
			RandomSource random = RandomSource.create(center.asLong());

			BlockState floor = MainBlocks.HELL_STONE.get().defaultBlockState();
			BlockState ground = MainBlocks.HELL_GROUND.get().defaultBlockState();
			BlockState deep = MainBlocks.HELL_DEEPSTONE.get().defaultBlockState();

			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			int baseY = center.getY();

			for (int dx = -RIM_RADIUS; dx <= RIM_RADIUS; dx++) {
				for (int dz = -RIM_RADIUS; dz <= RIM_RADIUS; dz++) {
					double dist = Math.sqrt(dx * dx + dz * dz);
					if (dist > RIM_RADIUS) continue;

					int x = center.getX() + dx;
					int z = center.getZ() + dz;

					if (dist <= FLOOR_RADIUS) {
						BlockState surface = random.nextInt(4) == 0 ? ground : floor;
						cursor.set(x, baseY, z);
						level.setBlock(cursor, surface, 2);

						for (int depth = 1; depth <= FOUNDATION_DEPTH; depth++) {
							cursor.set(x, baseY - depth, z);
							if (level.getBlockState(cursor).isAir()) level.setBlock(cursor, deep, 2);
						}

						for (int up = 1; up <= HEADROOM; up++) {
							cursor.set(x, baseY + up, z);
							if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
						}
					} else {
						double ramp = (dist - FLOOR_RADIUS) / (double) (RIM_RADIUS - FLOOR_RADIUS);
						int wallTop = baseY + Mth.ceil(ramp * RIM_HEIGHT);

						for (int y = baseY - FOUNDATION_DEPTH; y <= wallTop; y++) {
							cursor.set(x, y, z);
							BlockState existing = level.getBlockState(cursor);
							if (existing.isAir() || y >= baseY) {
								level.setBlock(cursor, y >= wallTop - 1 ? floor : deep, 2);
							}
						}

						for (int up = wallTop + 1; up <= baseY + HEADROOM; up++) {
							cursor.set(x, up, z);
							if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
			}

			for (int i = 0; i < RING_SPIKES; i++) {
				double angle = (Math.PI * 2.0D / RING_SPIKES) * i + random.nextDouble() * 0.15D;
				int radius = RIM_RADIUS - 1 - random.nextInt(2);
				int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
				int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);
				int top = surfaceAt(level, x, z, baseY + RIM_HEIGHT + 2, baseY - FOUNDATION_DEPTH);
				if (top != Integer.MIN_VALUE) spike(level, x, top + 1, z, 5 + random.nextInt(6), random);
			}

			for (int i = 0; i < INNER_SPIKES; i++) {
				double angle = random.nextDouble() * Math.PI * 2.0D;
				int radius = 4 + random.nextInt(FLOOR_RADIUS - 5);
				int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
				int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);
				if (Math.abs(x - center.getX()) < 4 && Math.abs(z - center.getZ()) < 4) continue;
				spike(level, x, baseY + 1, z, 3 + random.nextInt(4), random);
			}
		}

		@Override
		public WorldBossEntity createBoss(ServerLevel level) {
			return MainEntities.WORLDBOSS_JANEMBA_FAT.get().create(level);
		}

		private static int surfaceAt(ServerLevel level, int x, int z, int from, int to) {
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			for (int y = from; y >= to; y--) {
				cursor.set(x, y, z);
				if (!level.getBlockState(cursor).isAir()) return y;
			}
			return Integer.MIN_VALUE;
		}

		private static void spike(ServerLevel level, int x, int baseY, int z, int height, RandomSource random) {
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			BlockState bone = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);

			for (int i = 0; i < height; i++) {
				int width = i < height / 3 ? 1 : 0;
				for (int dx = -width; dx <= width; dx++) {
					for (int dz = -width; dz <= width; dz++) {
						if (width == 1 && Math.abs(dx) + Math.abs(dz) == 2) continue;
						cursor.set(x + dx, baseY + i, z + dz);
						if (level.getBlockState(cursor).isAir()) level.setBlock(cursor, bone, 2);
					}
				}
				if (i > height / 2 && random.nextInt(4) == 0) break;
			}
		}
	}

	final class Turles implements WorldBossLair {
		private static final int AREA_CHECK_RADIUS = TreeOfMightShape.ARENA_RADIUS + 8;
		private static final int GROUND_SCAN = 24;

		@Override
		public String key() {
			return WorldBossEntity.TURLES;
		}

		@Override
		public String displayName() {
			return "Turles";
		}

		@Override
		public ResourceKey<Level> dimension() {
			return Level.OVERWORLD;
		}

		@Override
		public boolean isReady(ServerLevel level) {
			return StructurePlanSavedData.get(level).isBuilt(DMZStructureSets.TREE_OF_MIGHT_SALT);
		}

		@Override
		public BlockPos pickColumn(ServerLevel level) {
			ChunkPos origin = StructureSpawnPlanner.publishedPositions(level).get(DMZStructureSets.TREE_OF_MIGHT_SALT);
			if (origin == null) return null;
			TreeOfMightShape tree = TreeOfMightShapes.planned(level, level.getChunkSource().getGenerator(), origin);
			return new BlockPos(tree.arenaX(), tree.arenaFloor(), tree.arenaZ());
		}

		@Override
		public boolean isAreaLoaded(ServerLevel level, BlockPos column) {
			int minX = (column.getX() - AREA_CHECK_RADIUS) >> 4;
			int maxX = (column.getX() + AREA_CHECK_RADIUS) >> 4;
			int minZ = (column.getZ() - AREA_CHECK_RADIUS) >> 4;
			int maxZ = (column.getZ() + AREA_CHECK_RADIUS) >> 4;
			for (int cx = minX; cx <= maxX; cx++) {
				for (int cz = minZ; cz <= maxZ; cz++) {
					if (!level.hasChunk(cx, cz)) return false;
				}
			}
			return true;
		}

		@Override
		public BlockPos resolveGround(ServerLevel level, BlockPos column) {
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			for (int y = column.getY() + GROUND_SCAN; y >= column.getY() - GROUND_SCAN; y--) {
				cursor.set(column.getX(), y, column.getZ());
				BlockState state = level.getBlockState(cursor);
				if (!state.isAir() && !state.canBeReplaced()) return cursor.immutable();
			}
			return column;
		}

		@Override
		public void buildArena(ServerLevel level, BlockPos ground) {
		}

		@Override
		public WorldBossEntity createBoss(ServerLevel level) {
			return MainEntities.WORLDBOSS_TURLES.get().create(level);
		}
	}

	final class GeteStar implements WorldBossLair {
		private static final int AREA_CHECK_RADIUS = GeteStarShape.RING_OUTER;

		@Override
		public String key() {
			return WorldBossEntity.METAL_COOLER_CORE;
		}

		@Override
		public String displayName() {
			return "Metal Cooler Core";
		}

		@Override
		public ResourceKey<Level> dimension() {
			return NamekDimension.NAMEK_KEY;
		}

		@Override
		public boolean isReady(ServerLevel level) {
			return StructureSpawnPlanner.publishedPositions(level).containsKey(DMZStructureSets.GETE_STAR_SALT);
		}

		@Override
		public BlockPos teleportTarget(ServerLevel level, BlockPos lair) {
			GeteStarShape shape = shape(level);
			return shape == null ? lair : shape.entrancePosition();
		}

		private static GeteStarShape shape(ServerLevel level) {
			ChunkPos origin = StructureSpawnPlanner.publishedPositions(level).get(DMZStructureSets.GETE_STAR_SALT);
			if (origin == null) return null;
			return GeteStarShape.planned(level, level.getChunkSource().getGenerator(), origin);
		}

		@Override
		public BlockPos pickColumn(ServerLevel level) {
			GeteStarShape shape = shape(level);
			return shape == null ? null : shape.corePosition();
		}

		@Override
		public boolean isAreaLoaded(ServerLevel level, BlockPos column) {
			int minX = (column.getX() - AREA_CHECK_RADIUS) >> 4;
			int maxX = (column.getX() + AREA_CHECK_RADIUS) >> 4;
			int minZ = (column.getZ() - AREA_CHECK_RADIUS) >> 4;
			int maxZ = (column.getZ() + AREA_CHECK_RADIUS) >> 4;
			for (int cx = minX; cx <= maxX; cx++) {
				for (int cz = minZ; cz <= maxZ; cz++) {
					if (!level.hasChunk(cx, cz)) return false;
				}
			}
			return true;
		}

		@Override
		public BlockPos resolveGround(ServerLevel level, BlockPos column) {
			return GeteStarStructure.isStarBlock(level.getBlockState(column)) ? column : null;
		}

		@Override
		public void buildArena(ServerLevel level, BlockPos ground) {
		}

		@Override
		public WorldBossEntity createBoss(ServerLevel level) {
			AllWorldBossesEntity.MetalCoolerCore core = MainEntities.WORLDBOSS_METAL_COOLER_CORE.get().create(level);
			GeteStarShape shape = shape(level);
			if (core != null && shape != null) {
				core.setChamber(shape.chamberCenter(), shape.capsulePositions());
			}
			return core;
		}
	}

	final class Tamagami implements WorldBossLair {
		public static final int PLATFORM_RADIUS = 15;
		public static final int RISE = 3;
		private static final int DAIS_RADIUS = 4;
		private static final int CLEARANCE = 20;
		private static final int FOUNDATION_LIMIT = 48;
		private static final int RAMP_HALF_WIDTH = 2;
		private static final int RAMP_HEADROOM = 4;
		private static final int PILLARS = 8;
		private static final int FLOOR_LIGHTS = 8;
		private static final int SEARCH_STEP = 24;
		private static final int SEARCH_RINGS = 20;
		private static final int SEARCH_HEADROOM = 20;
		private static final int FLAT_CHECK_POINTS = 8;
		private static final int MAX_SLOPE = 5;
		private static final int TUNNEL_CLEARANCE = DemonRealmGeneration.TUNNEL_RADIUS + DemonRealmGeneration.TUNNEL_EDGE_NOISE + 48;
		private static final long LAIR_SALT = 0x54414D4147414D49L;

		private final int number;
		private final Map<ServerLevel, CompletableFuture<BlockPos>> searches = new WeakHashMap<>();

		public Tamagami(int number) {
			this.number = number;
		}

		@Override
		public String key() {
			return WorldBossEntity.tamagamiKey(this.number);
		}

		@Override
		public String displayName() {
			return "Tamagami Number " + this.number;
		}

		@Override
		public ResourceKey<Level> dimension() {
			return DemonRealmDimension.DEMON_REALM_KEY;
		}

		@Override
		public boolean isReady(ServerLevel level) {
			return !level.players().isEmpty();
		}

		@Override
		public BlockPos pickColumn(ServerLevel level) {
			CompletableFuture<BlockPos> search = this.searches.get(level);
			if (search == null) {
				ChunkGenerator generator = level.getChunkSource().getGenerator();
				RandomState randomState = level.getChunkSource().randomState();
				LevelHeightAccessor heights = LevelHeightAccessor.create(level.getMinBuildHeight(), level.getHeight());
				long seed = level.getSeed();
				this.searches.put(level, CompletableFuture.supplyAsync(() -> this.findSite(generator, randomState, heights, seed), Util.backgroundExecutor()));
				return null;
			}
			if (!search.isDone()) return null;
			this.searches.remove(level);
			try {
				return search.join();
			} catch (RuntimeException e) {
				LogUtil.warn(Env.SERVER, "Tamagami {} lair search failed: {}", this.number, e.getMessage());
				return null;
			}
		}

		@Override
		public boolean isAreaLoaded(ServerLevel level, BlockPos column) {
			int reach = PLATFORM_RADIUS + RISE + 6;
			int minX = (column.getX() - reach) >> 4;
			int maxX = (column.getX() + reach) >> 4;
			int minZ = (column.getZ() - reach) >> 4;
			int maxZ = (column.getZ() + reach) >> 4;
			for (int cx = minX; cx <= maxX; cx++) {
				for (int cz = minZ; cz <= maxZ; cz++) {
					if (!level.hasChunk(cx, cz)) return false;
				}
			}
			return true;
		}

		@Override
		public BlockPos resolveGround(ServerLevel level, BlockPos column) {
			return column.above(RISE + 1);
		}

		@Override
		public BlockPos teleportTarget(ServerLevel level, BlockPos lair) {
			WorldBossManager.Data.Entry entry = WorldBossManager.Data.get(level.getServer()).peek(this.key());
			int top = entry != null && entry.lairResolved ? lair.getY() - 1 : lair.getY() + RISE;
			return new BlockPos(lair.getX(), top, lair.getZ() + PLATFORM_RADIUS - 3);
		}

		@Override
		public WorldBossEntity createBoss(ServerLevel level) {
			return switch (this.number) {
				case 1 -> MainEntities.WORLDBOSS_TAMAGAMI_1.get().create(level);
				case 2 -> MainEntities.WORLDBOSS_TAMAGAMI_2.get().create(level);
				default -> MainEntities.WORLDBOSS_TAMAGAMI_3.get().create(level);
			};
		}

		private int distance() {
			return switch (this.number) {
				case 1 -> 920;
				case 2 -> 760;
				default -> 600;
			};
		}

		private int bandTop() {
			return switch (this.number) {
				case 1 -> DemonRealmGeneration.CRUST_A_BOTTOM - 1;
				case 2 -> DemonRealmGeneration.CRUST_B_BOTTOM - 1;
				default -> DemonRealmGeneration.WORLD_TOP - 2;
			};
		}

		private int bandBottom() {
			return switch (this.number) {
				case 1 -> DemonRealmGeneration.FIRST_BASE - 16;
				case 2 -> DemonRealmGeneration.SECOND_SEA + 1;
				default -> DemonRealmGeneration.THIRD_BASE - 16;
			};
		}

		private BlockPos findSite(ChunkGenerator generator, RandomState randomState, LevelHeightAccessor heights, long seed) {
			RandomSource random = RandomSource.create(seed ^ LAIR_SALT ^ (this.number * 0x9E3779B97F4A7C15L));
			double angle = random.nextDouble() * Math.PI * 2.0D;
			int originX = Mth.floor(Math.cos(angle) * this.distance());
			int originZ = Mth.floor(Math.sin(angle) * this.distance());
			int top = this.bandTop();
			int bottom = this.bandBottom();

			BlockPos best = null;
			int bestSpread = Integer.MAX_VALUE;
			for (int ring = 0; ring <= SEARCH_RINGS; ring++) {
				int radius = ring * SEARCH_STEP;
				int points = ring == 0 ? 1 : Math.max(6, Mth.ceil(Math.PI * 2.0D * radius / SEARCH_STEP));
				double offset = random.nextDouble() * Math.PI * 2.0D;
				for (int i = 0; i < points; i++) {
					double a = offset + Math.PI * 2.0D * i / points;
					int x = originX + Mth.floor(Math.cos(a) * radius);
					int z = originZ + Mth.floor(Math.sin(a) * radius);
					if (nearTunnel(x, z)) continue;
					int ground = bandGround(generator.getBaseColumn(x, z, heights, randomState), bottom, top, SEARCH_HEADROOM);
					if (ground == Integer.MIN_VALUE) continue;
					int spread = spread(generator, randomState, heights, x, z, ground, bottom, top);
					if (spread <= MAX_SLOPE) {
						LogUtil.info(Env.SERVER, "Tamagami {} lair found at {} {} {}", this.number, x, ground, z);
						return new BlockPos(x, ground, z);
					}
					if (spread < bestSpread) {
						bestSpread = spread;
						best = new BlockPos(x, ground, z);
					}
				}
			}
			LogUtil.warn(Env.SERVER, "Tamagami {} found no flat ground, using {}", this.number, best);
			return best != null ? best : new BlockPos(originX, (top + bottom) / 2, originZ);
		}

		private static int spread(ChunkGenerator generator, RandomState randomState, LevelHeightAccessor heights,
								  int x, int z, int ground, int bottom, int top) {
			int spread = 0;
			int radius = PLATFORM_RADIUS - 1;
			for (int i = 0; i < FLAT_CHECK_POINTS; i++) {
				double a = Math.PI * 2.0D * i / FLAT_CHECK_POINTS;
				int sx = x + Mth.floor(Math.cos(a) * radius);
				int sz = z + Mth.floor(Math.sin(a) * radius);
				int sample = bandGround(generator.getBaseColumn(sx, sz, heights, randomState), bottom, top, 4);
				if (sample == Integer.MIN_VALUE) return Integer.MAX_VALUE;
				spread = Math.max(spread, Math.abs(sample - ground));
			}
			return spread;
		}

		private static int bandGround(NoiseColumn column, int bottom, int top, int headroom) {
			if (column.getBlock(bottom).isAir()) return Integer.MIN_VALUE;
			int y = bottom;
			while (y < top && !column.getBlock(y + 1).isAir()) y++;
			if (y >= top || !column.getBlock(y).getFluidState().isEmpty()) return Integer.MIN_VALUE;
			for (int h = 1; h <= headroom; h++) {
				if (!column.getBlock(y + h).isAir()) return Integer.MIN_VALUE;
			}
			return y;
		}

		private static boolean nearTunnel(int x, int z) {
			int spacing = DemonRealmGeneration.TUNNEL_SPACING;
			return DemonRealmFeatures.TunnelDistance.distance(x, z, spacing, DemonRealmGeneration.UPPER_TUNNEL_X, DemonRealmGeneration.UPPER_TUNNEL_Z) < TUNNEL_CLEARANCE
					|| DemonRealmFeatures.TunnelDistance.distance(x, z, spacing, DemonRealmGeneration.LOWER_TUNNEL_X, DemonRealmGeneration.LOWER_TUNNEL_Z) < TUNNEL_CLEARANCE;
		}

		private record Palette(BlockState base, BlockState accent, BlockState trim, BlockState light, Block stairs) {}

		private Palette palette() {
			return switch (this.number) {
				case 1 -> new Palette(MainBlocks.DEMON_ROCK.get().defaultBlockState(), MainBlocks.DEMON_BRICKS.get().defaultBlockState(),
						MainBlocks.RED_ASH.get().defaultBlockState(), MainBlocks.CRIMSON_CRYSTAL.get().defaultBlockState(),
						MainBlocks.DEMON_BRICK_STAIRS.get());
				case 2 -> new Palette(MainBlocks.TURQUOISE_ROCK.get().defaultBlockState(), MainBlocks.NAMEK_DEEPSLATE.get().defaultBlockState(),
						MainBlocks.NAMEK_STONE.get().defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState(),
						MainBlocks.NAMEK_DEEPSLATE_STAIRS.get());
				default -> new Palette(DemonRealmBlocks.natural(MainBlocks.MAKAI_STONE.get()), DemonRealmBlocks.natural(MainBlocks.ADOBE_BRICKS.get()),
						DemonRealmBlocks.natural(MainBlocks.MAKAI_STRATA_STONE.get()), Blocks.SHROOMLIGHT.defaultBlockState(),
						MainBlocks.ADOBE_STAIRS.get());
			};
		}

		@Override
		public void buildArena(ServerLevel level, BlockPos dais) {
			Palette palette = this.palette();
			RandomSource random = RandomSource.create(dais.asLong() ^ LAIR_SALT);
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			int top = dais.getY() - 1;
			int ground = top - RISE;
			int cx = dais.getX();
			int cz = dais.getZ();

			for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
				for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
					double distance = Math.sqrt(dx * dx + dz * dz);
					if (distance > PLATFORM_RADIUS + 0.5D) continue;
					int x = cx + dx;
					int z = cz + dz;
					boolean rim = distance > PLATFORM_RADIUS - 1.5D;
					clearAbove(level, cursor, x, z, top + 1, top + CLEARANCE);
					level.setBlock(cursor.set(x, top, z), floorPattern(palette, distance, dx, dz), 2);
					fillBelow(level, cursor, x, z, top - 1, ground, rim ? palette.accent() : palette.base());
				}
			}

			for (int dx = -DAIS_RADIUS; dx <= DAIS_RADIUS; dx++) {
				for (int dz = -DAIS_RADIUS; dz <= DAIS_RADIUS; dz++) {
					double distance = Math.sqrt(dx * dx + dz * dz);
					if (distance > DAIS_RADIUS + 0.2D) continue;
					level.setBlock(cursor.set(cx + dx, top + 1, cz + dz), distance > DAIS_RADIUS - 1.0D ? palette.accent() : palette.trim(), 2);
				}
			}

			for (int i = 0; i < FLOOR_LIGHTS; i++) {
				double angle = Math.PI * 2.0D * i / FLOOR_LIGHTS;
				int x = cx + (int) Math.round(Math.cos(angle) * (PLATFORM_RADIUS - 4));
				int z = cz + (int) Math.round(Math.sin(angle) * (PLATFORM_RADIUS - 4));
				level.setBlock(cursor.set(x, top, z), palette.light(), 2);
			}

			for (Direction direction : Direction.Plane.HORIZONTAL) {
				BlockState stair = palette.stairs().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction.getOpposite());
				Direction side = direction.getClockWise();
				for (int step = 1; step <= RISE; step++) {
					int along = PLATFORM_RADIUS + step;
					int y = top - step + 1;
					for (int w = -RAMP_HALF_WIDTH; w <= RAMP_HALF_WIDTH; w++) {
						int x = cx + direction.getStepX() * along + side.getStepX() * w;
						int z = cz + direction.getStepZ() * along + side.getStepZ() * w;
						clearAbove(level, cursor, x, z, y + 1, y + RAMP_HEADROOM);
						level.setBlock(cursor.set(x, y, z), stair, 2);
						fillBelow(level, cursor, x, z, y - 1, ground, palette.base());
					}
				}
			}

			for (int i = 0; i < PILLARS; i++) {
				double angle = Math.PI * 2.0D * (i + 0.5D) / PILLARS;
				int x = cx + (int) Math.round(Math.cos(angle) * (PLATFORM_RADIUS - 1.5D));
				int z = cz + (int) Math.round(Math.sin(angle) * (PLATFORM_RADIUS - 1.5D));
				boolean broken = random.nextInt(3) == 0;
				int height = broken ? 2 + random.nextInt(3) : 6 + random.nextInt(4);
				for (Direction direction : Direction.Plane.HORIZONTAL) {
					level.setBlock(cursor.set(x + direction.getStepX(), top + 1, z + direction.getStepZ()), palette.trim(), 2);
				}
				for (int h = 1; h <= height; h++) {
					level.setBlock(cursor.set(x, top + h, z), palette.accent(), 2);
				}
				if (broken) {
					Direction fallen = Direction.Plane.HORIZONTAL.getRandomDirection(random);
					level.setBlock(cursor.set(x + fallen.getStepX() * 2, top + 1, z + fallen.getStepZ() * 2), palette.accent(), 2);
				} else {
					level.setBlock(cursor.set(x, top + height + 1, z), palette.trim(), 2);
					level.setBlock(cursor.set(x, top + height + 2, z), palette.light(), 2);
				}
			}

			double reach = PLATFORM_RADIUS + RISE + 1.0D;
			for (ServerPlayer player : level.players()) {
				double dx = player.getX() - (cx + 0.5D);
				double dz = player.getZ() - (cz + 0.5D);
				if (dx * dx + dz * dz > reach * reach || player.getY() < ground - 2 || player.getY() >= top + 1) continue;
				player.teleportTo(player.getX(), top + 2, player.getZ());
			}
		}

		private static BlockState floorPattern(Palette palette, double distance, int dx, int dz) {
			if (distance > PLATFORM_RADIUS - 1.5D) return palette.accent();
			if (distance > PLATFORM_RADIUS - 4.5D && distance <= PLATFORM_RADIUS - 3.5D) return palette.trim();
			if (distance > DAIS_RADIUS + 1.5D && distance < PLATFORM_RADIUS - 4.5D
					&& Math.abs(Math.sin(Math.atan2(dz, dx) * 4.0D)) * distance < 0.9D) return palette.trim();
			return palette.base();
		}

		private static void clearAbove(ServerLevel level, BlockPos.MutableBlockPos cursor, int x, int z, int from, int to) {
			for (int y = from; y <= to; y++) {
				cursor.set(x, y, z);
				if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
			}
		}

		private static void fillBelow(ServerLevel level, BlockPos.MutableBlockPos cursor, int x, int z, int from, int ground, BlockState fill) {
			for (int y = from; y >= from - FOUNDATION_LIMIT; y--) {
				cursor.set(x, y, z);
				if (y < ground && isSupport(level.getBlockState(cursor))) return;
				level.setBlock(cursor, fill, 2);
			}
		}

		private static boolean isSupport(BlockState state) {
			return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced() && !state.is(BlockTags.LEAVES)
					&& !state.is(MainBlocks.DARK_SEA_CLOUD.get()) && !state.is(MainBlocks.MAKAI_BUSH.get());
		}
	}
}
