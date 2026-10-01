package com.dragonminez.server.world.structure.crater;

import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.server.world.raid.RaidSiteManager;
import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.Optional;

public class SaiyanCraterStructure extends Structure {
	public static final Codec<SaiyanCraterStructure> CODEC = simpleCodec(SaiyanCraterStructure::new);

	public static final int RADIUS = 10;
	private static final int DEPTH = 5;
	private static final int RIM_WIDTH = 4;
	private static final int RIM_HEIGHT = 2;
	private static final int CLEAR_HEIGHT = 14;
	private static final int MAX_SLOPE = 6;
	private static final int SAMPLES = 8;

	public SaiyanCraterStructure(StructureSettings settings) {
		super(settings);
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

		int min = centerY;
		int max = centerY;
		for (int i = 0; i < SAMPLES; i++) {
			double angle = Math.PI * 2.0D * i / SAMPLES;
			int x = centerX + (int) Math.round(Math.cos(angle) * (RADIUS + RIM_WIDTH));
			int z = centerZ + (int) Math.round(Math.sin(angle) * (RADIUS + RIM_WIDTH));
			int y = dryHeight(generator, heightAccessor, randomState, x, z);
			if (y == Integer.MIN_VALUE) return Optional.empty();
			min = Math.min(min, y);
			max = Math.max(max, y);
		}
		if (max - min > MAX_SLOPE) return Optional.empty();

		Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(centerX), QuartPos.fromBlock(centerY),
				QuartPos.fromBlock(centerZ), randomState.sampler());
		if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(BiomeTags.IS_RIVER)
				|| biome.is(BiomeTags.IS_BEACH)) return Optional.empty();

		long seed = context.seed() ^ (chunkPos.toLong() * 0x9E3779B97F4A7C15L);
		Piece piece = new Piece(centerX, centerY, centerZ, seed);
		return Optional.of(new GenerationStub(new BlockPos(centerX, centerY, centerZ), builder -> builder.addPiece(piece)));
	}

	private static int dryHeight(ChunkGenerator generator, LevelHeightAccessor heightAccessor, RandomState randomState, int x, int z) {
		int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
		int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState);
		return surface == floor ? surface - 1 : Integer.MIN_VALUE;
	}

	@Override
	public StructureType<?> type() {
		return MainStructureTypes.SAIYAN_CRATER.get();
	}

	public static class Piece extends StructurePiece {
		private final int centerX;
		private final int centerY;
		private final int centerZ;
		private final long seed;

		public Piece(int centerX, int centerY, int centerZ, long seed) {
			super(MainStructureTypes.SAIYAN_CRATER_PIECE.get(), 0, new BoundingBox(
					centerX - RADIUS - RIM_WIDTH - 2, centerY - DEPTH - 4, centerZ - RADIUS - RIM_WIDTH - 2,
					centerX + RADIUS + RIM_WIDTH + 2, centerY + CLEAR_HEIGHT + MAX_SLOPE, centerZ + RADIUS + RIM_WIDTH + 2));
			this.centerX = centerX;
			this.centerY = centerY;
			this.centerZ = centerZ;
			this.seed = seed;
		}

		public Piece(CompoundTag tag) {
			super(MainStructureTypes.SAIYAN_CRATER_PIECE.get(), tag);
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
			int outer = RADIUS + RIM_WIDTH + 1;
			int minX = Math.max(box.minX(), this.centerX - outer);
			int maxX = Math.min(box.maxX(), this.centerX + outer);
			int minZ = Math.max(box.minZ(), this.centerZ - outer);
			int maxZ = Math.min(box.maxZ(), this.centerZ + outer);
			double phase = (this.seed & 1023L) / 1023.0D * Math.PI * 2.0D;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					double dx = x - this.centerX;
					double dz = z - this.centerZ;
					double angle = Math.atan2(dz, dx);
					double wobble = 1.0D + 0.07D * Math.sin(angle * 5.0D + phase) + 0.04D * Math.sin(angle * 9.0D + phase * 2.0D);
					double distance = Math.sqrt(dx * dx + dz * dz) / wobble;
					if (distance > RADIUS + RIM_WIDTH) continue;

					RandomSource columnRandom = RandomSource.create(this.seed ^ (x * 3129871L) ^ (z * 116129781L));
					int ground = groundHeight(level, pos, x, z);

					if (distance <= RADIUS) carveBowl(level, pos, columnRandom, x, z, distance, ground);
					else raiseRim(level, pos, columnRandom, x, z, distance, ground);
				}
			}

			if (this.centerX >= box.minX() && this.centerX <= box.maxX() && this.centerZ >= box.minZ() && this.centerZ <= box.maxZ()) {
				this.placeCrew(level, pos);
			}
		}

		private void placeCrew(WorldGenLevel level, BlockPos.MutableBlockPos pos) {
			BlockPos floor = this.standingPos(level, pos, this.centerX, this.centerZ);
			SpacePodEntity pod = RaidSiteManager.createPod(level, floor);
			if (pod != null) level.addFreshEntity(pod);

			BlockPos stand = this.standingPos(level, pos, this.centerX + RaidSiteManager.SCOUT_OFFSET_X,
					this.centerZ + RaidSiteManager.SCOUT_OFFSET_Z);
			Mob scout = RaidSiteManager.createScout(level, RaidSiteManager.Kind.SAIYAN_CRATER, ChunkPos.asLong(this.centerX >> 4, this.centerZ >> 4), stand, floor);
			if (scout != null) level.addFreshEntityWithPassengers(scout);
		}

		private BlockPos standingPos(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z) {
			int bottom = this.centerY - DEPTH - 4;
			for (int y = this.centerY + RIM_HEIGHT + 2; y > bottom; y--) {
				if (!level.getBlockState(pos.set(x, y, z)).isAir()) return new BlockPos(x, y + 1, z);
			}
			return new BlockPos(x, this.centerY - DEPTH + 1, z);
		}

		private static int groundHeight(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z) {
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			int bottom = level.getMinBuildHeight();
			while (y > bottom) {
				BlockState state = level.getBlockState(pos.set(x, y, z));
				if (!isLoose(state)) break;
				y--;
			}
			return y;
		}

		private static boolean isLoose(BlockState state) {
			return state.isAir() || state.canBeReplaced() || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
					|| !state.getFluidState().isEmpty();
		}

		private void carveBowl(WorldGenLevel level, BlockPos.MutableBlockPos pos, RandomSource random, int x, int z,
							   double distance, int ground) {
			double t = distance / RADIUS;
			int floorY = this.centerY - (int) Math.round(DEPTH * (1.0D - t * t));
			int top = Math.max(ground, this.centerY) + CLEAR_HEIGHT;

			for (int y = floorY + 1; y <= top; y++) {
				if (!level.getBlockState(pos.set(x, y, z)).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
			}

			for (int y = ground + 1; y < floorY - 2; y++) {
				level.setBlock(pos.set(x, y, z), subsurface(random), 2);
			}
			level.setBlock(pos.set(x, floorY - 2, z), subsurface(random), 2);
			level.setBlock(pos.set(x, floorY - 1, z), subsurface(random), 2);
			level.setBlock(pos.set(x, floorY, z), floor(random, t), 2);
		}

		private void raiseRim(WorldGenLevel level, BlockPos.MutableBlockPos pos, RandomSource random, int x, int z,
							  double distance, int ground) {
			double t = (distance - RADIUS) / RIM_WIDTH;
			int rimTop = this.centerY + (int) Math.round(RIM_HEIGHT * (1.0D - t));

			for (int y = ground + 1; y < rimTop; y++) {
				level.setBlock(pos.set(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
			}
			if (rimTop > ground) level.setBlock(pos.set(x, rimTop, z), rim(random), 2);
			else if (random.nextFloat() < 0.6F) level.setBlock(pos.set(x, ground, z), rim(random), 2);

			int surface = Math.max(rimTop, ground);
			for (int y = surface + 1; y <= surface + CLEAR_HEIGHT; y++) {
				BlockState state = level.getBlockState(pos.set(x, y, z));
				if (!state.isAir() && state.getFluidState().isEmpty() && isLoose(state)) {
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}

		private static BlockState floor(RandomSource random, double t) {
			float roll = random.nextFloat();
			if (t < 0.4D) {
				if (roll < 0.35F) return Blocks.BLACKSTONE.defaultBlockState();
				if (roll < 0.55F) return Blocks.BASALT.defaultBlockState();
				if (roll < 0.75F) return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
				return Blocks.TUFF.defaultBlockState();
			}
			if (roll < 0.30F) return Blocks.GRAVEL.defaultBlockState();
			if (roll < 0.55F) return Blocks.TUFF.defaultBlockState();
			if (roll < 0.75F) return Blocks.STONE.defaultBlockState();
			if (roll < 0.90F) return Blocks.ANDESITE.defaultBlockState();
			return Blocks.COBBLESTONE.defaultBlockState();
		}

		private static BlockState subsurface(RandomSource random) {
			float roll = random.nextFloat();
			if (roll < 0.5F) return Blocks.STONE.defaultBlockState();
			if (roll < 0.75F) return Blocks.TUFF.defaultBlockState();
			return Blocks.COBBLESTONE.defaultBlockState();
		}

		private static BlockState rim(RandomSource random) {
			float roll = random.nextFloat();
			if (roll < 0.35F) return Blocks.COARSE_DIRT.defaultBlockState();
			if (roll < 0.65F) return Blocks.GRAVEL.defaultBlockState();
			if (roll < 0.80F) return Blocks.TUFF.defaultBlockState();
			return Blocks.DIRT.defaultBlockState();
		}
	}
}
