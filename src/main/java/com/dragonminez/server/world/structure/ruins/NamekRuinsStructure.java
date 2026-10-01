package com.dragonminez.server.world.structure.ruins;

import com.dragonminez.Reference;
import com.dragonminez.server.world.biome.NamekBiomes;
import com.dragonminez.server.world.raid.RaidSiteManager;
import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NamekRuinsStructure extends Structure {
	public static final Codec<NamekRuinsStructure> CODEC = simpleCodec(NamekRuinsStructure::new);

	private static final List<ResourceLocation> AJISSA_HOUSES = List.of(
			template("village_ajissa/houses/residence/ajissa_house_big"),
			template("village_ajissa/houses/residence/ajissa_house_small"));
	private static final List<ResourceLocation> SACRED_HOUSES = List.of(
			template("village_sacred/houses/residence/sacred_house_big"),
			template("village_sacred/houses/residence/sacred_house_small"));

	private static final int MIN_HOUSES = 3;
	private static final int MAX_HOUSES = 4;
	private static final int MIN_PLACED_HOUSES = 2;
	private static final double HOUSE_RING_FACTOR = 0.8D;
	private static final double HOUSE_RING_GAP = 6.0D;
	private static final double ANGLE_JITTER = 0.3D;
	private static final int PLAZA_RADIUS = 7;

	public NamekRuinsStructure(StructureSettings settings) {
		super(settings);
	}

	private static ResourceLocation template(String path) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunkPos = context.chunkPos();
		int centerX = chunkPos.getMiddleBlockX();
		int centerZ = chunkPos.getMiddleBlockZ();
		ChunkGenerator generator = context.chunkGenerator();
		LevelHeightAccessor heightAccessor = context.heightAccessor();
		RandomState randomState = context.randomState();

		int centerY = dryHeight(generator, heightAccessor, randomState, centerX, centerZ);
		if (centerY == Integer.MIN_VALUE || centerY <= generator.getSeaLevel()) return Optional.empty();

		Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(centerX), QuartPos.fromBlock(centerY),
				QuartPos.fromBlock(centerZ), randomState.sampler());
		List<ResourceLocation> houses = biome.is(NamekBiomes.SACRED_LAND) ? SACRED_HOUSES : AJISSA_HOUSES;

		StructureTemplateManager templates = context.structureTemplateManager();
		RandomSource random = context.random();
		int count = MIN_HOUSES + random.nextInt(MAX_HOUSES - MIN_HOUSES + 1);
		double baseAngle = random.nextDouble() * Math.PI * 2.0D;

		List<StructurePiece> pieces = new ArrayList<>();
		pieces.add(new PlazaPiece(centerX, centerY, centerZ, random.nextLong()));

		for (int i = 0; i < count; i++) {
			ResourceLocation location = houses.get(random.nextInt(houses.size()));
			Vec3i size = templates.getOrCreate(location).getSize();
			if (size.getX() <= 0 || size.getZ() <= 0) continue;

			double radius = Math.max(size.getX(), size.getZ()) * HOUSE_RING_FACTOR + HOUSE_RING_GAP;
			double angle = baseAngle + Math.PI * 2.0D * i / count + (random.nextDouble() - 0.5D) * ANGLE_JITTER;
			int houseX = centerX + (int) Math.round(Math.cos(angle) * radius);
			int houseZ = centerZ + (int) Math.round(Math.sin(angle) * radius);
			int ground = dryHeight(generator, heightAccessor, randomState, houseX, houseZ);
			if (ground == Integer.MIN_VALUE) continue;

			BlockPos position = new BlockPos(houseX - size.getX() / 2, ground + 1, houseZ - size.getZ() / 2);
			pieces.add(new HousePiece(templates, location, position, Rotation.getRandom(random), random.nextLong()));
		}
		if (pieces.size() - 1 < MIN_PLACED_HOUSES) return Optional.empty();

		return Optional.of(new GenerationStub(new BlockPos(centerX, centerY, centerZ), builder -> pieces.forEach(builder::addPiece)));
	}

	private static int dryHeight(ChunkGenerator generator, LevelHeightAccessor heightAccessor, RandomState randomState, int x, int z) {
		int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
		int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState);
		return surface == floor ? surface - 1 : Integer.MIN_VALUE;
	}

	@Override
	public StructureType<?> type() {
		return MainStructureTypes.NAMEK_RUINS.get();
	}

	private static float noise(long seed, int x, int y, int z) {
		long hash = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
		hash ^= hash >>> 33;
		hash *= 0xFF51AFD7ED558CCDL;
		hash ^= hash >>> 33;
		return (hash >>> 40) / (float) (1 << 24);
	}

	private static boolean breakable(WorldGenLevel level, BlockPos pos, BlockState state) {
		return !state.isAir() && !state.hasBlockEntity() && state.getFluidState().isEmpty()
				&& state.getDestroySpeed(level, pos) >= 0.0F;
	}

	private static BlockState scorched(float roll) {
		if (roll < 0.40F) return Blocks.BLACKSTONE.defaultBlockState();
		if (roll < 0.65F) return Blocks.BASALT.defaultBlockState();
		if (roll < 0.85F) return Blocks.TUFF.defaultBlockState();
		return Blocks.COARSE_DIRT.defaultBlockState();
	}

	public static class PlazaPiece extends StructurePiece {
		private static final int CRATERS = 3;
		private static final float SCORCH_CHANCE = 0.45F;

		private final int centerX;
		private final int centerY;
		private final int centerZ;
		private final long seed;

		public PlazaPiece(int centerX, int centerY, int centerZ, long seed) {
			super(MainStructureTypes.NAMEK_RUINS_PLAZA.get(), 0, new BoundingBox(
					centerX - PLAZA_RADIUS - 2, centerY + 1, centerZ - PLAZA_RADIUS - 2,
					centerX + PLAZA_RADIUS + 2, centerY + 6, centerZ + PLAZA_RADIUS + 2));
			this.centerX = centerX;
			this.centerY = centerY;
			this.centerZ = centerZ;
			this.seed = seed;
		}

		public PlazaPiece(CompoundTag tag) {
			super(MainStructureTypes.NAMEK_RUINS_PLAZA.get(), tag);
			this.centerX = tag.getInt("CenterX");
			this.centerY = tag.getInt("CenterY");
			this.centerZ = tag.getInt("CenterZ");
			this.seed = tag.getLong("Seed");
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			tag.putInt("CenterX", this.centerX);
			tag.putInt("CenterY", this.centerY);
			tag.putInt("CenterZ", this.centerZ);
			tag.putLong("Seed", this.seed);
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
								RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

			for (int x = Math.max(box.minX(), this.centerX - PLAZA_RADIUS); x <= Math.min(box.maxX(), this.centerX + PLAZA_RADIUS); x++) {
				for (int z = Math.max(box.minZ(), this.centerZ - PLAZA_RADIUS); z <= Math.min(box.maxZ(), this.centerZ + PLAZA_RADIUS); z++) {
					int dx = x - this.centerX;
					int dz = z - this.centerZ;
					if (dx * dx + dz * dz > PLAZA_RADIUS * PLAZA_RADIUS) continue;
					float roll = noise(this.seed, x, 0, z);
					if (roll > SCORCH_CHANCE) continue;

					int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
					BlockState state = level.getBlockState(pos.set(x, y, z));
					if (breakable(level, pos, state) && state.isCollisionShapeFullBlock(level, pos)) {
						level.setBlock(pos, scorched(noise(this.seed, x, 1, z)), 2);
					}
				}
			}

			for (int i = 0; i < CRATERS; i++) {
				int x = this.centerX + (int) Math.round((noise(this.seed, i, 2, 0) - 0.5F) * PLAZA_RADIUS * 1.6F);
				int z = this.centerZ + (int) Math.round((noise(this.seed, i, 3, 0) - 0.5F) * PLAZA_RADIUS * 1.6F);
				double radius = 1.5D + noise(this.seed, i, 4, 0) * 0.8D;
				this.crater(level, pos, box, x, z, radius);
			}

			if (box.isInside(this.centerX, box.minY(), this.centerZ)) this.placeScout(level, pos, chunkPos);
		}

		private void crater(WorldGenLevel level, BlockPos.MutableBlockPos pos, BoundingBox box, int x, int z, double radius) {
			if (!box.isInside(x, box.minY(), z)) return;
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			int reach = (int) Math.ceil(radius) + 1;

			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					int bx = x + dx;
					int bz = z + dz;
					if (!box.isInside(bx, box.minY(), bz)) continue;
					for (int dy = -reach; dy <= 1; dy++) {
						double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
						if (distance > radius + 1.0D) continue;

						pos.set(bx, y + dy, bz);
						BlockState state = level.getBlockState(pos);
						if (!breakable(level, pos, state)) continue;

						if (distance <= radius) {
							level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
						} else if (dy <= 0 && state.isCollisionShapeFullBlock(level, pos)) {
							level.setBlock(pos, scorched(noise(this.seed, bx, y + dy, bz)), 2);
						}
					}
				}
			}
		}

		private void placeScout(WorldGenLevel level, BlockPos.MutableBlockPos pos, ChunkPos chunkPos) {
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.centerX, this.centerZ);
			BlockPos stand = new BlockPos(this.centerX, y, this.centerZ);
			Mob scout = RaidSiteManager.createScout(level, RaidSiteManager.Kind.NAMEK_RUINS, chunkPos.toLong(), stand, stand);
			if (scout != null) level.addFreshEntityWithPassengers(scout);
		}
	}

	public static class HousePiece extends TemplateStructurePiece {
		private static final int BLASTS = 2;
		private static final float BASE_COLLAPSE = 0.08F;
		private static final float ROOF_COLLAPSE = 0.6F;
		private static final float BROKEN_GLASS = 0.8F;
		private static final float SCORCH_THRESHOLD = 0.8F;

		private final Rotation rotation;
		private final long seed;

		public HousePiece(StructureTemplateManager templates, ResourceLocation location, BlockPos position, Rotation rotation, long seed) {
			super(MainStructureTypes.NAMEK_RUINS_HOUSE.get(), 0, templates, location, location.toString(),
					settings(rotation, pivot(templates, location)), position);
			this.rotation = rotation;
			this.seed = seed;
		}

		public HousePiece(StructureTemplateManager templates, CompoundTag tag) {
			super(MainStructureTypes.NAMEK_RUINS_HOUSE.get(), tag, templates,
					location -> settings(Rotation.valueOf(tag.getString("Rot")), pivot(templates, location)));
			this.rotation = Rotation.valueOf(tag.getString("Rot"));
			this.seed = tag.getLong("RuinSeed");
		}

		private static BlockPos pivot(StructureTemplateManager templates, ResourceLocation location) {
			StructureTemplate template = templates.getOrCreate(location);
			return new BlockPos(template.getSize().getX() / 2, 0, template.getSize().getZ() / 2);
		}

		private static StructurePlaceSettings settings(Rotation rotation, BlockPos pivot) {
			return new StructurePlaceSettings()
					.setRotation(rotation)
					.setRotationPivot(pivot)
					.setIgnoreEntities(true)
					.addProcessor(JigsawReplacementProcessor.INSTANCE)
					.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			super.addAdditionalSaveData(context, tag);
			tag.putString("Rot", this.rotation.name());
			tag.putLong("RuinSeed", this.seed);
		}

		@Override
		protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
								RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
			super.postProcess(level, structureManager, generator, random, box, chunkPos, pivot);
			this.ruin(level, box);
		}

		private void ruin(WorldGenLevel level, BoundingBox box) {
			BoundingBox house = this.boundingBox;
			int floorY = house.minY();
			int height = Math.max(1, house.maxY() - floorY);
			int[][] blasts = this.blasts(house);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

			for (int x = Math.max(box.minX(), house.minX()); x <= Math.min(box.maxX(), house.maxX()); x++) {
				for (int z = Math.max(box.minZ(), house.minZ()); z <= Math.min(box.maxZ(), house.maxZ()); z++) {
					for (int y = floorY + 1; y <= house.maxY(); y++) {
						BlockState state = level.getBlockState(pos.set(x, y, z));
						if (!breakable(level, pos, state)) continue;

						float roll = noise(this.seed, x, y, z);
						double heightFactor = (y - floorY) / (double) height;

						if (inBlast(blasts, x, y, z) || roll < BASE_COLLAPSE + ROOF_COLLAPSE * heightFactor * heightFactor) {
							level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
						} else if (state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES)) {
							if (roll < BROKEN_GLASS) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
						} else if (roll > SCORCH_THRESHOLD && state.isCollisionShapeFullBlock(level, pos)) {
							level.setBlock(pos, scorched(noise(this.seed, x, y + 7, z)), 2);
						}
					}
				}
			}
		}

		private int[][] blasts(BoundingBox house) {
			int[][] blasts = new int[BLASTS][4];
			for (int i = 0; i < BLASTS; i++) {
				blasts[i][0] = house.minX() + (int) (noise(this.seed, i, 11, 0) * house.getXSpan());
				blasts[i][1] = house.minY() + (int) ((0.4F + noise(this.seed, i, 12, 0) * 0.6F) * house.getYSpan());
				blasts[i][2] = house.minZ() + (int) (noise(this.seed, i, 13, 0) * house.getZSpan());
				blasts[i][3] = 2 + (int) (noise(this.seed, i, 14, 0) * 2.0F);
			}
			return blasts;
		}

		private static boolean inBlast(int[][] blasts, int x, int y, int z) {
			for (int[] blast : blasts) {
				int dx = x - blast[0];
				int dy = y - blast[1];
				int dz = z - blast[2];
				if (dx * dx + dy * dy + dz * dz <= blast[3] * blast[3]) return true;
			}
			return false;
		}
	}
}
