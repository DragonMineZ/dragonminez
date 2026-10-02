package com.dragonminez.server.world.structure;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.server.world.biome.NamekBiomes;
import com.dragonminez.server.world.raid.RaidSiteManager;
import com.dragonminez.server.world.structure.helper.DMZStructureSets;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import com.dragonminez.server.world.structure.placement.IncrementalStructure;
import com.dragonminez.server.world.structure.placement.StructureSpawnPlanner;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class BossStructures {

	private BossStructures() {}

	public static class TreeOfMightStructure extends Structure implements IncrementalStructure {
		public static final Codec<TreeOfMightStructure> CODEC = simpleCodec(TreeOfMightStructure::new);

		public TreeOfMightStructure(StructureSettings settings) {
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

			TreeOfMightShape shape = TreeOfMightShape.create(centerX, centerZ, TreeOfMightShapes.seedFor(context.seed(), chunkPos),
					heightAccessor.getMinBuildHeight(), heightAccessor.getMaxBuildHeight(), generator.getSeaLevel(),
					(x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState));
			TreeOfMightPiece piece = new TreeOfMightPiece(shape);
			return Optional.of(new GenerationStub(new BlockPos(centerX, shape.baseY(), centerZ), builder -> builder.addPiece(piece)));
		}

		@Override
		public StructureType<?> type() {
			return MainStructureTypes.TREE_OF_MIGHT.get();
		}
	}

	public static class TreeOfMightPiece extends StructurePiece {
		private final int version;
		private final long seed;
		private final int centerX;
		private final int centerZ;
		private final int minBuildY;
		private final int maxBuildY;
		private final int seaLevel;
		private final long[] sampleKeys;
		private final int[] sampleValues;
		private volatile TreeOfMightShape shape;

		public TreeOfMightPiece(TreeOfMightShape shape) {
			super(MainStructureTypes.TREE_OF_MIGHT_PIECE.get(), 0,
					new BoundingBox(shape.minX(), shape.minY(), shape.minZ(), shape.maxX(), shape.maxY(), shape.maxZ()));
			this.version = TreeOfMightShape.VERSION;
			this.seed = shape.seed();
			this.centerX = shape.centerX();
			this.centerZ = shape.centerZ();
			this.minBuildY = shape.minBuildY();
			this.maxBuildY = shape.maxBuildY();
			this.seaLevel = shape.seaLevel();
			Map<Long, Integer> samples = shape.groundSamples();
			this.sampleKeys = new long[samples.size()];
			this.sampleValues = new int[samples.size()];
			int i = 0;
			for (Map.Entry<Long, Integer> entry : samples.entrySet()) {
				this.sampleKeys[i] = entry.getKey();
				this.sampleValues[i] = entry.getValue();
				i++;
			}
			this.shape = shape;
			TreeOfMightShapes.remember(shape);
		}

		public TreeOfMightPiece(CompoundTag tag) {
			super(MainStructureTypes.TREE_OF_MIGHT_PIECE.get(), tag);
			this.version = tag.getInt("ShapeVersion");
			this.seed = tag.getLong("Seed");
			this.centerX = tag.getInt("CenterX");
			this.centerZ = tag.getInt("CenterZ");
			this.minBuildY = tag.getInt("MinBuildY");
			this.maxBuildY = tag.getInt("MaxBuildY");
			this.seaLevel = tag.getInt("SeaLevel");
			this.sampleKeys = tag.getLongArray("SampleKeys");
			this.sampleValues = tag.getIntArray("SampleValues");
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			tag.putInt("ShapeVersion", this.version);
			tag.putLong("Seed", this.seed);
			tag.putInt("CenterX", this.centerX);
			tag.putInt("CenterZ", this.centerZ);
			tag.putInt("MinBuildY", this.minBuildY);
			tag.putInt("MaxBuildY", this.maxBuildY);
			tag.putInt("SeaLevel", this.seaLevel);
			tag.putLongArray("SampleKeys", this.sampleKeys);
			tag.putIntArray("SampleValues", this.sampleValues);
		}

		public TreeOfMightShape shape() {
			TreeOfMightShape current = this.shape;
			if (current != null) return current;
			current = TreeOfMightShapes.cached(this.seed, this.centerX, this.centerZ);
			if (current == null) {
				Map<Long, Integer> samples = new HashMap<>();
				for (int i = 0; i < Math.min(this.sampleKeys.length, this.sampleValues.length); i++) {
					samples.put(this.sampleKeys[i], this.sampleValues[i]);
				}
				int fallback = samples.getOrDefault(TreeOfMightShape.pack(this.centerX, this.centerZ), this.seaLevel);
				current = TreeOfMightShape.create(this.centerX, this.centerZ, this.seed, this.minBuildY, this.maxBuildY, this.seaLevel,
						(x, z) -> samples.getOrDefault(TreeOfMightShape.pack(x, z), fallback));
				TreeOfMightShapes.remember(current);
			}
			this.shape = current;
			return current;
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
								RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
			TreeOfMightWriter.place(level, shape(), chunkPos, box);
		}
	}

	public static final class TreeOfMightShape {
		public static final int VERSION = 1;

		public static final byte NONE = 0;
		public static final byte FOLIAGE = 1;
		public static final byte ROOT = 2;
		public static final byte WOOD = 3;
		public static final byte FRUIT = 4;
		public static final byte LICHEN = 5;

		public static final int ROOT_THIN = 0;
		public static final int ROOT_SINGLE = 1;
		public static final int ROOT_PLUS = 2;
		public static final int ROOT_THICK = 3;

		public static final int[] FACE_DX = {0, 0, 0, 0, -1, 1};
		public static final int[] FACE_DY = {-1, 1, 0, 0, 0, 0};
		public static final int[] FACE_DZ = {0, 0, -1, 1, 0, 0};

		public static final int ARENA_RADIUS = 26;
		public static final int ARENA_BLEND = 6;
		public static final int ARENA_HEADROOM = 40;
		public static final int BASE_ZONE = 12;
		public static final int MAX_BASE_EXTENSION = 48;

		private static final int SHELL = 6;
		private static final double RIM_AMPLITUDE = 0.10D;
		private static final int ROOT_SPACING = 6;
		private static final int ROOT_ARENA_CLEARANCE = 24;
		private static final int LOBE_TABLE = 1024;
		private static final double LICHEN_THRESHOLD = 0.28D;
		private static final int LICHEN_DENSITY = 90;

		public interface Surface {
			int at(int x, int z);
		}

		public interface Obstacle {
			boolean blocks(int x, int y, int z);
		}

		public record HangingRoot(int x, int z, int topY, int bottomY, boolean reachesGround, int kind, boolean fruit) {
			public int[][] footprint() {
				return switch (kind) {
					case ROOT_PLUS -> new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
					case ROOT_THICK -> new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
					default -> new int[][]{{0, 0}};
				};
			}

			public byte code() {
				return kind == ROOT_THIN ? ROOT : WOOD;
			}

			HangingRoot withFruit() {
				return new HangingRoot(x, z, topY, bottomY, reachesGround, kind, true);
			}
		}

		private record Capsule(double ax, double ay, double az, double bx, double by, double bz, double ra, double rb,
							   int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			static Capsule of(double ax, double ay, double az, double ra, double bx, double by, double bz, double rb) {
				double r = Math.max(ra, rb);
				return new Capsule(ax, ay, az, bx, by, bz, ra, rb,
						(int) Math.floor(Math.min(ax, bx) - r), (int) Math.floor(Math.min(ay, by) - r), (int) Math.floor(Math.min(az, bz) - r),
						(int) Math.ceil(Math.max(ax, bx) + r), (int) Math.ceil(Math.max(ay, by) + r), (int) Math.ceil(Math.max(az, bz) + r));
			}

			boolean intersects(int x0, int y0, int z0, int x1, int y1, int z1) {
				return maxX >= x0 && minX <= x1 && maxY >= y0 && minY <= y1 && maxZ >= z0 && minZ <= z1;
			}
		}

		private record PathPoint(double x, double y, double z, double r) {}

		private final int centerX;
		private final int centerZ;
		private final double axisOriginX;
		private final double axisOriginZ;
		private final long seed;
		private final int minBuildY;
		private final int maxBuildY;
		private final int seaLevel;
		private final Map<Long, Integer> samples;

		private final Perlin detailNoise;
		private final Perlin lumpNoise;
		private final Perlin bumpNoise;
		private final Perlin lichenNoise;

		private final int baseY;
		private final int trunkBottom;
		private final int trunkTop;
		private final int solidTop;
		private final double rTop;
		private final double rBottom;
		private final double flare;
		private final double flareHeight;
		private final double leanX;
		private final double leanZ;
		private final double wobbleX;
		private final double wobbleZ;
		private final double wobblePhaseX;
		private final double wobblePhaseZ;
		private final double twist;
		private final int[] ridgeFreq = {7, 11, 17};
		private final double[] ridgeAmp = {0.5, 0.32, 0.18};
		private final double[] ridgePhase = new double[3];
		private final double[] buttressAngles;
		private final double[] lobeTable = new double[LOBE_TABLE];

		private final double[] trunkAxisX;
		private final double[] trunkAxisZ;
		private final double[] trunkCore;
		private final double[] trunkFlare;
		private final double[] trunkShell;
		private final double[] trunkMaxR;

		private final int canopyBase;
		private final double canopyThickness;
		private final double canopyRadius;
		private final int[] rimFreq = {3, 4, 5, 6, 7, 9, 11, 13};
		private final double[] rimAmp = new double[rimFreq.length];
		private final double[] rimPhase = new double[rimFreq.length];

		private final double arenaAngle;
		private final int arenaX;
		private final int arenaZ;
		private final int arenaFloor;

		private final List<Capsule> baseCapsules = new ArrayList<>();
		private final List<Capsule> branchCapsules = new ArrayList<>();
		private final List<HangingRoot> roots = new ArrayList<>();

		private final int minX;
		private final int minY;
		private final int minZ;
		private final int maxX;
		private final int maxY;
		private final int maxZ;

		private TreeOfMightShape(int centerX, int centerZ, long seed, int minBuildY, int maxBuildY, int seaLevel, Surface surface) {
			this.centerX = centerX;
			this.centerZ = centerZ;
			this.axisOriginX = centerX + 0.5D;
			this.axisOriginZ = centerZ + 0.5D;
			this.seed = seed;
			this.minBuildY = minBuildY;
			this.maxBuildY = maxBuildY;
			this.seaLevel = seaLevel;
			this.samples = new LinkedHashMap<>();
			Surface sampler = (x, z) -> this.samples.computeIfAbsent(pack(x, z), key -> surface.at(x, z));

			Random random = new Random(seed * 0x9E3779B97F4A7C15L + VERSION);
			this.detailNoise = new Perlin(random.nextLong());
			this.lumpNoise = new Perlin(random.nextLong());
			this.bumpNoise = new Perlin(random.nextLong());
			this.lichenNoise = new Perlin(random.nextLong());

			int groundSum = sampler.at(centerX, centerZ);
			for (int i = 0; i < 8; i++) {
				double angle = i * Math.PI / 4.0D;
				groundSum += sampler.at(centerX + (int) StrictMath.round(StrictMath.cos(angle) * 18.0D),
						centerZ + (int) StrictMath.round(StrictMath.sin(angle) * 18.0D));
			}
			this.baseY = Math.floorDiv(groundSum, 9);

			int ceiling = maxBuildY - 8;
			this.canopyBase = clamp(this.baseY + 168, this.baseY + 110, ceiling - 76);
			this.canopyThickness = Math.max(24.0D, Math.min(60.0D, ceiling - 24 - this.canopyBase));
			this.canopyRadius = 165.0D + random.nextDouble() * 15.0D;

			this.trunkTop = this.canopyBase + 14;
			this.trunkBottom = this.baseY - 8;
			this.solidTop = this.baseY + 6;
			this.rTop = 20.0D + random.nextDouble() * 3.0D;
			this.rBottom = 27.0D + random.nextDouble() * 4.0D;
			this.flare = 27.0D + random.nextDouble() * 6.0D;
			this.flareHeight = 16.0D + random.nextDouble() * 6.0D;

			double leanAngle = random.nextDouble() * Math.PI * 2.0D;
			double lean = 3.0D + random.nextDouble() * 4.0D;
			this.leanX = StrictMath.cos(leanAngle) * lean;
			this.leanZ = StrictMath.sin(leanAngle) * lean;
			this.wobbleX = (random.nextDouble() - 0.5D) * 4.0D;
			this.wobbleZ = (random.nextDouble() - 0.5D) * 4.0D;
			this.wobblePhaseX = random.nextDouble() * Math.PI * 2.0D;
			this.wobblePhaseZ = random.nextDouble() * Math.PI * 2.0D;
			this.twist = (random.nextDouble() - 0.5D) * 1.2D;
			for (int i = 0; i < this.ridgePhase.length; i++) this.ridgePhase[i] = random.nextDouble() * Math.PI * 2.0D;

			double rimTotal = 0.0D;
			for (int i = 0; i < this.rimAmp.length; i++) {
				this.rimAmp[i] = (0.4D + random.nextDouble() * 0.6D) / StrictMath.pow(this.rimFreq[i], 0.6D);
				this.rimPhase[i] = random.nextDouble() * Math.PI * 2.0D;
				rimTotal += this.rimAmp[i];
			}
			for (int i = 0; i < this.rimAmp.length; i++) this.rimAmp[i] /= rimTotal;

			this.arenaAngle = random.nextDouble() * Math.PI * 2.0D;
			double arenaDistance = this.rBottom + this.flare * 0.55D + ARENA_RADIUS + 8.0D;
			this.arenaX = (int) Math.floor(this.axisOriginX + StrictMath.cos(this.arenaAngle) * arenaDistance);
			this.arenaZ = (int) Math.floor(this.axisOriginZ + StrictMath.sin(this.arenaAngle) * arenaDistance);
			this.arenaFloor = Math.max(sampler.at(this.arenaX, this.arenaZ) - 1, seaLevel - 1);

			double window = StrictMath.asin(Math.min(1.0D, (ARENA_RADIUS + 10.0D) / arenaDistance)) + 0.1D;
			int buttressCount = 9 + random.nextInt(3);
			this.buttressAngles = new double[buttressCount];
			double span = Math.PI * 2.0D - window * 2.0D;
			for (int i = 0; i < buttressCount; i++) {
				double slot = (i + 0.5D + (random.nextDouble() - 0.5D) * 0.5D) / buttressCount;
				this.buttressAngles[i] = this.arenaAngle + window + span * slot;
			}
			for (int i = 0; i < LOBE_TABLE; i++) {
				double theta = i * Math.PI * 2.0D / LOBE_TABLE;
				double best = 0.0D;
				for (double angle : this.buttressAngles) {
					double d = angleDiff(theta, angle);
					best = Math.max(best, StrictMath.exp(-(d * d) / (2.0D * 0.16D * 0.16D)));
				}
				this.lobeTable[i] = best;
			}

			int trunkLength = this.trunkTop - this.trunkBottom + 1;
			this.trunkAxisX = new double[trunkLength];
			this.trunkAxisZ = new double[trunkLength];
			this.trunkCore = new double[trunkLength];
			this.trunkFlare = new double[trunkLength];
			this.trunkShell = new double[trunkLength];
			this.trunkMaxR = new double[trunkLength];
			for (int i = 0; i < trunkLength; i++) {
				int y = this.trunkBottom + i;
				double t = trunkProgress(y);
				this.trunkAxisX[i] = this.axisOriginX + this.leanX * t * t
						+ this.wobbleX * (StrictMath.sin(Math.PI * 1.3D * t + this.wobblePhaseX) - StrictMath.sin(this.wobblePhaseX));
				this.trunkAxisZ[i] = this.axisOriginZ + this.leanZ * t * t
						+ this.wobbleZ * (StrictMath.sin(Math.PI * 1.3D * t + this.wobblePhaseZ) - StrictMath.sin(this.wobblePhaseZ));
				this.trunkCore[i] = coreRadius(y);
				this.trunkFlare[i] = flareRadius(y);
				double slope = Math.abs(flareRadius(y + 1) - flareRadius(y - 1)) * 0.5D
						+ Math.abs(coreRadius(y + 1) - coreRadius(y - 1)) * 0.5D;
				this.trunkShell[i] = 3.5D + slope * 1.6D;
				this.trunkMaxR[i] = this.trunkCore[i] * 1.1D + this.trunkFlare[i] + 1.0D;
			}

			buildButtressRoots(random, sampler);
			List<PathPoint> branchPoints = new ArrayList<>();
			buildLimbs(random, branchPoints);
			buildHangingRoots(random, branchPoints);
			pickFruits(random);

			int buttressReach = 0;
			for (Capsule capsule : this.baseCapsules) {
				buttressReach = Math.max(buttressReach, Math.max(Math.abs(capsule.minX() - centerX), Math.abs(capsule.maxX() - centerX)));
				buttressReach = Math.max(buttressReach, Math.max(Math.abs(capsule.minZ() - centerZ), Math.abs(capsule.maxZ() - centerZ)));
			}
			int horizontal = (int) Math.ceil(Math.max(this.canopyRadius * (1.0D + RIM_AMPLITUDE) + 7.0D, Math.max(buttressReach + 2, ARENA_RADIUS + ARENA_BLEND
					+ Math.max(Math.abs(this.arenaX - centerX), Math.abs(this.arenaZ - centerZ)) + 2)));
			this.minX = centerX - horizontal;
			this.maxX = centerX + horizontal;
			this.minZ = centerZ - horizontal;
			this.maxZ = centerZ + horizontal;
			this.minY = Math.max(minBuildY + 1, this.baseY - 64);
			this.maxY = Math.min(maxBuildY - 2, (int) Math.ceil(this.canopyBase + 10 + this.canopyThickness + 18));
		}

		public static TreeOfMightShape create(int centerX, int centerZ, long seed, int minBuildY, int maxBuildY, int seaLevel, Surface surface) {
			return new TreeOfMightShape(centerX, centerZ, seed, minBuildY, maxBuildY, seaLevel, surface);
		}

		public static long pack(int x, int z) {
			return ((long) x << 32) | (z & 0xFFFFFFFFL);
		}

		public Map<Long, Integer> groundSamples() {
			return Collections.unmodifiableMap(this.samples);
		}

		public long seed() {
			return this.seed;
		}

		public int centerX() {
			return this.centerX;
		}

		public int centerZ() {
			return this.centerZ;
		}

		public int baseY() {
			return this.baseY;
		}

		public int canopyBase() {
			return this.canopyBase;
		}

		public int arenaX() {
			return this.arenaX;
		}

		public int arenaZ() {
			return this.arenaZ;
		}

		public int arenaFloor() {
			return this.arenaFloor;
		}

		public double canopyRadius() {
			return this.canopyRadius;
		}

		public int seaLevel() {
			return this.seaLevel;
		}

		public int minBuildY() {
			return this.minBuildY;
		}

		public int maxBuildY() {
			return this.maxBuildY;
		}

		public int minX() {
			return this.minX;
		}

		public int minY() {
			return this.minY;
		}

		public int minZ() {
			return this.minZ;
		}

		public int maxX() {
			return this.maxX;
		}

		public int maxY() {
			return this.maxY;
		}

		public int maxZ() {
			return this.maxZ;
		}

		public List<HangingRoot> roots() {
			return Collections.unmodifiableList(this.roots);
		}

		public boolean intersectsColumns(int x0, int z0, int x1, int z1) {
			return x1 >= this.minX && x0 <= this.maxX && z1 >= this.minZ && z0 <= this.maxZ;
		}

		public boolean isUnderCanopy(int x, int z) {
			double ox = x + 0.5D - this.axisOriginX;
			double oz = z + 0.5D - this.axisOriginZ;
			return Math.sqrt(ox * ox + oz * oz) < rimRadius(Math.atan2(oz, ox)) * 0.95D;
		}

		public boolean isArenaColumn(int x, int z, int extra) {
			long dx = x - this.arenaX;
			long dz = z - this.arenaZ;
			long r = ARENA_RADIUS + extra;
			return dx * dx + dz * dz <= r * r;
		}

		public double arenaDistance(int x, int z) {
			double dx = x - this.arenaX;
			double dz = z - this.arenaZ;
			return Math.sqrt(dx * dx + dz * dz);
		}

		public void rasterize(int x0, int y0, int z0, int sx, int sy, int sz, byte[] out) {
			int x1 = x0 + sx - 1;
			int y1 = y0 + sy - 1;
			int z1 = z0 + sz - 1;
			if (x1 < this.minX || x0 > this.maxX || z1 < this.minZ || z0 > this.maxZ || y1 < this.minY || y0 > this.maxY) return;

			ColumnData columns = columnData(x0, z0, sx, sz);
			for (int dz = 0; dz < sz; dz++) {
				for (int dx = 0; dx < sx; dx++) {
					int column = dz * sx + dx;
					if (!columns.canopy[column]) continue;
					double px = x0 + dx + 0.5D;
					double pz = z0 + dz + 0.5D;
					int from = Math.max(y0, (int) Math.floor(columns.low[column]) - 7);
					int to = Math.min(y1, (int) Math.ceil(columns.high[column]) + 7);
					for (int y = from; y <= to; y++) {
						double density = density(px, y + 0.5D, pz, columns.low[column], columns.high[column], columns.radial[column]);
						if (density > 0.0D && density < SHELL) mark(out, sx, sz, dx, y - y0, dz, FOLIAGE);
					}
				}
			}

			rasterizeTrunk(x0, y0, z0, sx, sy, sz, out, columns);

			for (Capsule capsule : this.baseCapsules) {
				if (capsule.intersects(x0, y0, z0, x1, y1, z1)) rasterizeCapsule(capsule, x0, y0, z0, sx, sy, sz, out, null);
			}
			for (Capsule capsule : this.branchCapsules) {
				if (capsule.intersects(x0, y0, z0, x1, y1, z1)) rasterizeCapsule(capsule, x0, y0, z0, sx, sy, sz, out, columns);
			}
		}

		public void applyRoots(int x0, int y0, int z0, int sx, int sy, int sz, byte[] out, Obstacle obstacle) {
			int x1 = x0 + sx - 1;
			int z1 = z0 + sz - 1;
			for (HangingRoot root : this.roots) {
				if (root.x() + 1 < x0 || root.x() - 1 > x1 || root.z() + 1 < z0 || root.z() - 1 > z1) continue;
				byte code = root.code();
				boolean dripTip = !root.fruit() && !root.reachesGround() && root.kind() == ROOT_THIN;
				for (int[] offset : root.footprint()) {
					int x = root.x() + offset[0];
					int z = root.z() + offset[1];
					if (x < x0 || x > x1 || z < z0 || z > z1) continue;
					int bottom = root.reachesGround() ? y0 : Math.max(y0, root.bottomY());
					boolean hanging = false;
					int lowest = -1;
					for (int y = Math.min(root.topY(), y0 + sy - 1); y >= bottom; y--) {
						int index = index(sx, sz, x - x0, y - y0, z - z0);
						byte current = out[index];
						if (current == WOOD) {
							if (hanging) break;
							continue;
						}
						if (obstacle.blocks(x, y, z)) break;
						byte voxel = dripTip && y <= root.bottomY() + 1 ? WOOD : code;
						if (current < voxel) out[index] = voxel;
						hanging = true;
						lowest = index;
					}
					if (root.fruit() && lowest >= 0) out[lowest] = FRUIT;
				}
			}
		}

		public void applyLichen(int x0, int y0, int z0, int sx, int sy, int sz, byte[] out, byte[] faces) {
			int x1 = x0 + sx - 1;
			int z1 = z0 + sz - 1;
			if (x1 < this.minX || x0 > this.maxX || z1 < this.minZ || z0 > this.maxZ) return;
			ColumnData columns = columnData(x0, z0, sx, sz);
			for (int dy = 0; dy < sy; dy++) {
				int y = y0 + dy;
				for (int dz = 0; dz < sz; dz++) {
					for (int dx = 0; dx < sx; dx++) {
						int index = index(sx, sz, dx, dy, dz);
						if (out[index] != NONE) continue;
						int mask = 0;
						for (int face = 0; face < 6; face++) {
							int nx = dx + FACE_DX[face];
							int ny = dy + FACE_DY[face];
							int nz = dz + FACE_DZ[face];
							if (nx < 0 || ny < 0 || nz < 0 || nx >= sx || ny >= sy || nz >= sz) continue;
							if (out[index(sx, sz, nx, ny, nz)] == WOOD) mask |= 1 << face;
						}
						if (mask == 0) continue;
						int x = x0 + dx;
						int z = z0 + dz;
						if ((mix(x, y, z) & 127) >= LICHEN_DENSITY) continue;
						double px = x + 0.5D;
						double py = y + 0.5D;
						double pz = z + 0.5D;
						if (lichenNoise.noise(px / 7.0D, py / 7.0D, pz / 7.0D) < LICHEN_THRESHOLD) continue;
						int column = dz * sx + dx;
						if (columns.canopy[column]
								&& density(px, py, pz, columns.low[column], columns.high[column], columns.radial[column]) > -1.0D) continue;
						if (insideTrunkHollow(x, y, z)) continue;
						out[index] = LICHEN;
						faces[index] = (byte) mask;
					}
				}
			}
		}

		public int baseBottom(byte[] out, int sx, int sy, int sz, int dx, int dz, int y0) {
			int limit = Math.min(sy - 1, this.baseY + BASE_ZONE - y0);
			for (int dy = 0; dy <= limit; dy++) {
				if (out[index(sx, sz, dx, dy, dz)] == WOOD) return y0 + dy;
			}
			return Integer.MIN_VALUE;
		}

		public static int index(int sx, int sz, int dx, int dy, int dz) {
			return (dy * sz + dz) * sx + dx;
		}

		private static void mark(byte[] out, int sx, int sz, int dx, int dy, int dz, byte code) {
			int index = index(sx, sz, dx, dy, dz);
			if (out[index] < code) out[index] = code;
		}

		private static final class ColumnData {
			final double[] low;
			final double[] high;
			final double[] radial;
			final boolean[] canopy;

			ColumnData(int size) {
				this.low = new double[size];
				this.high = new double[size];
				this.radial = new double[size];
				this.canopy = new boolean[size];
			}
		}

		private ColumnData columnData(int x0, int z0, int sx, int sz) {
			ColumnData columns = new ColumnData(sx * sz);
			for (int dz = 0; dz < sz; dz++) {
				for (int dx = 0; dx < sx; dx++) {
					int column = dz * sx + dx;
					double px = x0 + dx + 0.5D;
					double pz = z0 + dz + 0.5D;
					double ox = px - this.axisOriginX;
					double oz = pz - this.axisOriginZ;
					double r = Math.sqrt(ox * ox + oz * oz);
					double rim = rimRadius(Math.atan2(oz, ox));
					double u = r / rim;
					columns.radial[column] = rim - r;
					if (u > 1.08D) continue;
					columns.canopy[column] = true;
					columns.low[column] = lowSurface(px, pz, u);
					columns.high[column] = highSurface(px, pz, u);
				}
			}
			return columns;
		}

		private void rasterizeTrunk(int x0, int y0, int z0, int sx, int sy, int sz, byte[] out, ColumnData columns) {
			int from = Math.max(y0, this.trunkBottom);
			int to = Math.min(y0 + sy - 1, this.trunkTop);
			for (int y = from; y <= to; y++) {
				int i = y - this.trunkBottom;
				double axisX = this.trunkAxisX[i];
				double axisZ = this.trunkAxisZ[i];
				double maxR = this.trunkMaxR[i];
				if (x0 + sx < axisX - maxR || x0 > axisX + maxR || z0 + sz < axisZ - maxR || z0 > axisZ + maxR) continue;
				double t = trunkProgress(y);
				for (int dz = 0; dz < sz; dz++) {
					double oz = z0 + dz + 0.5D - axisZ;
					for (int dx = 0; dx < sx; dx++) {
						double ox = x0 + dx + 0.5D - axisX;
						double r2 = ox * ox + oz * oz;
						if (r2 > maxR * maxR) continue;
						double r = Math.sqrt(r2);
						double radius = trunkRadius(i, Math.atan2(oz, ox), t);
						if (r > radius) continue;
						if (y > this.solidTop && r < radius - this.trunkShell[i]) continue;
						int column = dz * sx + dx;
						if (columns.canopy[column] && insideCanopyCore(x0 + dx + 0.5D, y + 0.5D, z0 + dz + 0.5D, columns, column)) continue;
						mark(out, sx, sz, dx, y - y0, dz, WOOD);
					}
				}
			}
		}

		private double trunkRadius(int i, double theta, double t) {
			double ridge = 0.0D;
			for (int k = 0; k < this.ridgeFreq.length; k++) {
				ridge += this.ridgeAmp[k] * Math.cos(this.ridgeFreq[k] * (theta + this.twist * t) + this.ridgePhase[k]);
			}
			return this.trunkCore[i] * (1.0D + 0.085D * ridge) + this.trunkFlare[i] * (0.55D + 0.45D * lobe(theta));
		}

		private boolean insideTrunkHollow(int x, int y, int z) {
			if (y <= this.solidTop || y > this.trunkTop) return false;
			int i = y - this.trunkBottom;
			double ox = x + 0.5D - this.trunkAxisX[i];
			double oz = z + 0.5D - this.trunkAxisZ[i];
			double r = Math.sqrt(ox * ox + oz * oz);
			if (r > this.trunkMaxR[i]) return false;
			return r < trunkRadius(i, Math.atan2(oz, ox), trunkProgress(y)) - this.trunkShell[i];
		}

		private void rasterizeCapsule(Capsule c, int x0, int y0, int z0, int sx, int sy, int sz, byte[] out, ColumnData columns) {
			int fromX = Math.max(x0, c.minX());
			int toX = Math.min(x0 + sx - 1, c.maxX());
			int fromY = Math.max(y0, c.minY());
			int toY = Math.min(y0 + sy - 1, c.maxY());
			int fromZ = Math.max(z0, c.minZ());
			int toZ = Math.min(z0 + sz - 1, c.maxZ());
			double abx = c.bx() - c.ax();
			double aby = c.by() - c.ay();
			double abz = c.bz() - c.az();
			double length2 = abx * abx + aby * aby + abz * abz;
			for (int y = fromY; y <= toY; y++) {
				for (int z = fromZ; z <= toZ; z++) {
					for (int x = fromX; x <= toX; x++) {
						double px = x + 0.5D - c.ax();
						double py = y + 0.5D - c.ay();
						double pz = z + 0.5D - c.az();
						double t = length2 > 1.0E-9D ? clamp01((px * abx + py * aby + pz * abz) / length2) : 0.0D;
						double qx = px - abx * t;
						double qy = py - aby * t;
						double qz = pz - abz * t;
						double r = c.ra() + (c.rb() - c.ra()) * t;
						if (qx * qx + qy * qy + qz * qz > r * r) continue;
						if (columns != null) {
							int column = (z - z0) * sx + (x - x0);
							if (columns.canopy[column] && insideCanopyCore(x + 0.5D, y + 0.5D, z + 0.5D, columns, column)) continue;
						}
						mark(out, sx, sz, x - x0, y - y0, z - z0, WOOD);
					}
				}
			}
		}

		private boolean insideCanopyCore(double x, double y, double z, ColumnData columns, int column) {
			return density(x, y, z, columns.low[column], columns.high[column], columns.radial[column]) >= SHELL + 2;
		}

		private double density(double x, double y, double z, double low, double high, double radial) {
			double dist = Math.min(Math.min(y - low, high - y), radial);
			if (dist < -6.0D || dist > SHELL + 8.0D) return dist;
			return dist + detailNoise.noise(x / 13.0D, y / 9.0D, z / 13.0D) * 3.8D
					+ detailNoise.noise(x / 5.5D + 100.0D, y / 5.5D, z / 5.5D) * 1.6D;
		}

		private double lowSurface(double x, double z, double u) {
			double lump = Math.max(0.0D, lumpNoise.noise(x / 24.0D, 13.7D, z / 24.0D)) * 6.0D
					+ Math.max(0.0D, lumpNoise.noise(x / 9.0D, 71.3D, z / 9.0D)) * 2.5D;
			return this.canopyBase + 10.0D * u * u - lump;
		}

		private double highSurface(double x, double z, double u) {
			double bumps = bumpNoise.noise(x / 34.0D, 5.1D, z / 34.0D) * 11.0D + bumpNoise.noise(x / 13.0D, 41.9D, z / 13.0D) * 4.0D;
			return this.canopyBase + 10.0D + this.canopyThickness * dome(u) + bumps;
		}

		private static double dome(double u) {
			if (u >= 1.0D) return 0.0D;
			return Math.pow(1.0D - Math.pow(u, 2.4D), 0.55D);
		}

		private double rimRadius(double theta) {
			double value = 0.0D;
			for (int i = 0; i < this.rimFreq.length; i++) {
				value += this.rimAmp[i] * Math.sin(this.rimFreq[i] * theta + this.rimPhase[i]);
			}
			return this.canopyRadius * (1.0D + RIM_AMPLITUDE * value);
		}

		private int undersideAt(int x, int z) {
			double px = x + 0.5D;
			double pz = z + 0.5D;
			double ox = px - this.axisOriginX;
			double oz = pz - this.axisOriginZ;
			double r = Math.sqrt(ox * ox + oz * oz);
			double rim = rimRadius(Math.atan2(oz, ox));
			double u = r / rim;
			double low = lowSurface(px, pz, Math.min(u, 1.08D));
			double high = highSurface(px, pz, Math.min(u, 1.08D));
			int start = (int) Math.floor(low) - 7;
			for (int y = start; y <= start + 21; y++) {
				if (density(px, y + 0.5D, pz, low, high, rim - r) > 0.0D) return y;
			}
			return (int) Math.floor(low);
		}

		private double lobe(double theta) {
			double normalized = theta / (Math.PI * 2.0D);
			normalized -= Math.floor(normalized);
			int index = (int) (normalized * LOBE_TABLE) & (LOBE_TABLE - 1);
			return this.lobeTable[index];
		}

		private double trunkProgress(int y) {
			return clamp01((y - this.baseY) / (double) (this.trunkTop - this.baseY));
		}

		private double coreRadius(int y) {
			return this.rBottom + (this.rTop - this.rBottom) * StrictMath.pow(trunkProgress(y), 0.8D);
		}

		private double flareRadius(int y) {
			double h = Math.max(0, y - this.baseY);
			return this.flare * StrictMath.exp(-h / this.flareHeight);
		}

		private void buildButtressRoots(Random random, Surface sampler) {
			double[] steps = {0.0D, 0.28D, 0.48D, 0.66D, 0.82D, 1.0D};
			for (double angle : this.buttressAngles) {
				double reach = 82.0D + random.nextDouble() * 20.0D;
				double r0 = 11.0D + random.nextDouble() * 4.0D;
				double startY = this.baseY + 8.0D + random.nextDouble() * 6.0D;
				double bend = (random.nextDouble() - 0.5D) * 0.35D;
				double startFraction = (this.rBottom * 0.8D) / reach;
				PathPoint previous = null;
				PathPoint forkFrom = null;
				for (double step : steps) {
					double fraction = Math.max(step, startFraction);
					double distance = reach * fraction;
					double a = angle + bend * fraction * fraction;
					double px = this.axisOriginX + StrictMath.cos(a) * distance;
					double pz = this.axisOriginZ + StrictMath.sin(a) * distance;
					double r = 1.2D + (r0 - 1.2D) * StrictMath.pow(1.0D - fraction, 0.85D);
					double py = step == 0.0D ? startY : sampler.at((int) Math.floor(px), (int) Math.floor(pz)) - 0.35D * r;
					PathPoint point = new PathPoint(px, py, pz, r);
					if (previous != null) this.baseCapsules.add(Capsule.of(previous.x(), previous.y(), previous.z(), previous.r(), px, py, pz, r));
					if (step == 0.48D) forkFrom = point;
					previous = point;
				}
				if (forkFrom != null && random.nextDouble() < 0.55D) {
					double side = random.nextBoolean() ? 1.0D : -1.0D;
					double forkAngle = angle + side * (0.35D + random.nextDouble() * 0.25D);
					double forkReach = reach * (0.45D + random.nextDouble() * 0.15D);
					PathPoint start = forkFrom;
					double fr0 = forkFrom.r() * 0.6D;
					for (double step : new double[]{0.5D, 1.0D}) {
						double px = forkFrom.x() + StrictMath.cos(forkAngle) * forkReach * step;
						double pz = forkFrom.z() + StrictMath.sin(forkAngle) * forkReach * step;
						double r = 1.1D + (fr0 - 1.1D) * (1.0D - step);
						double py = sampler.at((int) Math.floor(px), (int) Math.floor(pz)) - 0.35D * r;
						this.baseCapsules.add(Capsule.of(start.x(), start.y(), start.z(), start.r() * (step == 0.5D ? 0.6D : 1.0D), px, py, pz, r));
						start = new PathPoint(px, py, pz, r);
					}
				}
			}
		}

		private void buildLimbs(Random random, List<PathPoint> branchPoints) {
			int limbCount = 16 + random.nextInt(5);
			double baseAngle = random.nextDouble() * Math.PI * 2.0D;
			for (int i = 0; i < limbCount; i++) {
				double angle = baseAngle + i * Math.PI * 2.0D / limbCount + (random.nextDouble() - 0.5D) * 0.35D * (Math.PI * 2.0D / limbCount);
				double startY = this.canopyBase - 32 + random.nextDouble() * 22.0D;
				double length = this.canopyRadius * (0.80D + random.nextDouble() * 0.13D);
				double bend = (random.nextDouble() - 0.5D) * 0.5D;
				double r0 = 11.0D + random.nextDouble() * 3.0D;
				int axisIndex = clamp((int) Math.round(startY) - this.trunkBottom, 0, this.trunkAxisX.length - 1);
				double originX = this.trunkAxisX[axisIndex];
				double originZ = this.trunkAxisZ[axisIndex];
				double startDistance = this.trunkCore[axisIndex] * 0.6D;

				List<PathPoint> path = new ArrayList<>();
				int steps = 16;
				for (int k = 0; k <= steps; k++) {
					double t = k / (double) steps;
					double distance = Math.max(startDistance, length * t);
					double a = angle + bend * t * t;
					double px = originX + StrictMath.cos(a) * distance;
					double pz = originZ + StrictMath.sin(a) * distance;
					double r = 1.4D + (r0 - 1.4D) * StrictMath.pow(1.0D - t, 1.1D);
					double target = undersideAt((int) Math.floor(px), (int) Math.floor(pz)) + 0.3D * r - 0.5D;
					double py = startY + (target - startY) * smoothstep(0.0D, 0.28D, t);
					path.add(new PathPoint(px, py, pz, r));
				}
				addPath(path, branchPoints);

				int secondaryCount = 3 + random.nextInt(4);
				for (int j = 0; j < secondaryCount; j++) {
					double t = 0.25D + 0.70D * (j + random.nextDouble() * 0.8D) / secondaryCount;
					PathPoint origin = interpolate(path, t);
					double side = (j % 2 == 0) ? 1.0D : -1.0D;
					double branchAngle = angle + bend * t * t + side * (0.45D + random.nextDouble() * 0.6D);
					double branchLength = (22.0D + random.nextDouble() * 38.0D) * (1.15D - 0.5D * t);
					double branchRadius = Math.min(origin.r() * 0.6D, 5.5D);
					double branchBend = (random.nextDouble() - 0.5D) * 0.6D;
					List<PathPoint> branch = growBranch(origin, branchAngle, branchBend, branchLength, branchRadius, 8);
					if (branch.size() < 2) continue;
					addPath(branch, branchPoints);
					int twigs = random.nextDouble() < 0.6D ? 1 + random.nextInt(2) : 0;
					for (int k = 0; k < twigs; k++) {
						PathPoint twigOrigin = interpolate(branch, 0.4D + random.nextDouble() * 0.4D);
						double twigAngle = branchAngle + (random.nextBoolean() ? 1.0D : -1.0D) * (0.5D + random.nextDouble() * 0.4D);
						List<PathPoint> twig = growBranch(twigOrigin, twigAngle, 0.0D, 10.0D + random.nextDouble() * 14.0D,
								Math.min(twigOrigin.r() * 0.7D, 2.2D), 4);
						if (twig.size() >= 2) addPath(twig, branchPoints);
					}
				}
			}
		}

		private List<PathPoint> growBranch(PathPoint origin, double angle, double bend, double length, double r0, int steps) {
			List<PathPoint> path = new ArrayList<>();
			path.add(origin);
			for (int k = 1; k <= steps; k++) {
				double t = k / (double) steps;
				double a = angle + bend * t;
				double px = origin.x() + StrictMath.cos(a) * length * t;
				double pz = origin.z() + StrictMath.sin(a) * length * t;
				double ox = px - this.axisOriginX;
				double oz = pz - this.axisOriginZ;
				if (Math.sqrt(ox * ox + oz * oz) > rimRadius(StrictMath.atan2(oz, ox)) * 0.96D) break;
				double r = 1.05D + (r0 - 1.05D) * StrictMath.pow(1.0D - t, 1.1D);
				double py = undersideAt((int) Math.floor(px), (int) Math.floor(pz)) + 0.3D * r - 0.5D;
				path.add(new PathPoint(px, py, pz, r));
			}
			return path;
		}

		private void addPath(List<PathPoint> path, List<PathPoint> branchPoints) {
			for (int i = 1; i < path.size(); i++) {
				PathPoint a = path.get(i - 1);
				PathPoint b = path.get(i);
				this.branchCapsules.add(Capsule.of(a.x(), a.y(), a.z(), a.r(), b.x(), b.y(), b.z(), b.r()));
				double dx = b.x() - a.x();
				double dz = b.z() - a.z();
				int samplesAlong = Math.max(1, (int) (Math.sqrt(dx * dx + dz * dz) / 7.0D));
				for (int s = 0; s < samplesAlong; s++) {
					double f = (s + 0.5D) / samplesAlong;
					branchPoints.add(new PathPoint(a.x() + dx * f, a.y() + (b.y() - a.y()) * f, a.z() + dz * f, a.r() + (b.r() - a.r()) * f));
				}
			}
		}

		private static PathPoint interpolate(List<PathPoint> path, double t) {
			double position = t * (path.size() - 1);
			int index = Math.min(path.size() - 2, (int) Math.floor(position));
			double f = position - index;
			PathPoint a = path.get(index);
			PathPoint b = path.get(index + 1);
			return new PathPoint(a.x() + (b.x() - a.x()) * f, a.y() + (b.y() - a.y()) * f, a.z() + (b.z() - a.z()) * f, a.r() + (b.r() - a.r()) * f);
		}

		private void buildHangingRoots(Random random, List<PathPoint> branchPoints) {
			int target = 200 + random.nextInt(50);
			Map<Long, List<int[]>> grid = new HashMap<>();
			for (PathPoint point : branchPoints) {
				if (this.roots.size() >= target * 0.6D) break;
				if (random.nextDouble() > 0.5D) continue;
				tryAddRoot(random, grid, point.x(), point.z(), (int) Math.floor(point.y() - point.r() * 0.3D));
			}
			int attempts = 0;
			while (this.roots.size() < target && attempts++ < 12000) {
				double angle = random.nextDouble() * Math.PI * 2.0D;
				double distance = Math.sqrt(random.nextDouble()) * this.canopyRadius * 0.95D;
				tryAddRoot(random, grid, this.axisOriginX + StrictMath.cos(angle) * distance,
						this.axisOriginZ + StrictMath.sin(angle) * distance, Integer.MIN_VALUE);
			}
		}

		private void pickFruits(Random random) {
			List<Integer> eligible = new ArrayList<>();
			for (int i = 0; i < this.roots.size(); i++) {
				HangingRoot root = this.roots.get(i);
				if (!root.reachesGround() && root.kind() == ROOT_THIN && root.topY() - root.bottomY() >= 14) eligible.add(i);
			}
			Collections.shuffle(eligible, random);
			int count = Math.min(eligible.size(), 6 + random.nextInt(5));
			for (int i = 0; i < count; i++) {
				int index = eligible.get(i);
				this.roots.set(index, this.roots.get(index).withFruit());
			}
		}

		private void tryAddRoot(Random random, Map<Long, List<int[]>> grid, double x, double z, int fixedTop) {
			int ix = (int) Math.floor(x);
			int iz = (int) Math.floor(z);
			double ox = ix + 0.5D - this.axisOriginX;
			double oz = iz + 0.5D - this.axisOriginZ;
			double r = Math.sqrt(ox * ox + oz * oz);
			if (r < this.rTop + 14.0D) return;
			if (r > rimRadius(StrictMath.atan2(oz, ox)) * 0.95D) return;

			int cellX = Math.floorDiv(ix, ROOT_SPACING);
			int cellZ = Math.floorDiv(iz, ROOT_SPACING);
			for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
				for (int cz = cellZ - 1; cz <= cellZ + 1; cz++) {
					List<int[]> cell = grid.get(pack(cx, cz));
					if (cell == null) continue;
					for (int[] other : cell) {
						int ddx = other[0] - ix;
						int ddz = other[1] - iz;
						if (ddx * ddx + ddz * ddz < ROOT_SPACING * ROOT_SPACING) return;
					}
				}
			}

			int top = fixedTop != Integer.MIN_VALUE ? fixedTop : undersideAt(ix, iz) + 1;
			boolean inArena = isArenaColumn(ix, iz, 4);
			double roll = random.nextDouble();
			int kind = roll < 0.74D ? ROOT_THIN : roll < 0.84D ? ROOT_SINGLE : roll < 0.97D ? ROOT_PLUS : ROOT_THICK;
			boolean reachesGround = !inArena && random.nextDouble() < 0.22D;
			int bottom;
			if (reachesGround) {
				bottom = Integer.MIN_VALUE;
			} else {
				int length = 10 + (int) (random.nextDouble() * 0.72D * Math.max(0, top - this.baseY));
				bottom = top - length;
				if (inArena) bottom = Math.max(bottom, this.arenaFloor + ROOT_ARENA_CLEARANCE);
				if (bottom >= top - 3) return;
			}
			if (kind == ROOT_THICK && (!reachesGround || r > 90.0D)) kind = ROOT_PLUS;

			grid.computeIfAbsent(pack(cellX, cellZ), key -> new ArrayList<>()).add(new int[]{ix, iz});
			this.roots.add(new HangingRoot(ix, iz, top, bottom, reachesGround, kind, false));
		}

		private static int mix(int x, int y, int z) {
			int h = x * 0x27D4EB2D ^ y * 0x165667B1 ^ z * 0x61C88647;
			h ^= h >>> 15;
			h *= 0x2C1B3C6D;
			h ^= h >>> 12;
			return h;
		}

		private static double angleDiff(double a, double b) {
			double d = (a - b) % (Math.PI * 2.0D);
			if (d < -Math.PI) d += Math.PI * 2.0D;
			if (d > Math.PI) d -= Math.PI * 2.0D;
			return d;
		}

		private static double smoothstep(double edge0, double edge1, double x) {
			double t = clamp01((x - edge0) / (edge1 - edge0));
			return t * t * (3.0D - 2.0D * t);
		}

		private static double clamp01(double value) {
			return value < 0.0D ? 0.0D : Math.min(value, 1.0D);
		}

		private static int clamp(int value, int min, int max) {
			return value < min ? min : Math.min(value, max);
		}

		private static final class Perlin {
			private final int[] permutation = new int[512];

			Perlin(long seed) {
				Random random = new Random(seed);
				int[] base = new int[256];
				for (int i = 0; i < 256; i++) base[i] = i;
				for (int i = 255; i > 0; i--) {
					int j = random.nextInt(i + 1);
					int swap = base[i];
					base[i] = base[j];
					base[j] = swap;
				}
				for (int i = 0; i < 512; i++) this.permutation[i] = base[i & 255];
			}

			double noise(double x, double y, double z) {
				double fx = Math.floor(x);
				double fy = Math.floor(y);
				double fz = Math.floor(z);
				int ix = (int) fx & 255;
				int iy = (int) fy & 255;
				int iz = (int) fz & 255;
				x -= fx;
				y -= fy;
				z -= fz;
				double u = fade(x);
				double v = fade(y);
				double w = fade(z);
				int[] p = this.permutation;
				int a = p[ix] + iy;
				int aa = p[a] + iz;
				int ab = p[a + 1] + iz;
				int b = p[ix + 1] + iy;
				int ba = p[b] + iz;
				int bb = p[b + 1] + iz;
				return lerp(w,
						lerp(v, lerp(u, grad(p[aa], x, y, z), grad(p[ba], x - 1, y, z)),
								lerp(u, grad(p[ab], x, y - 1, z), grad(p[bb], x - 1, y - 1, z))),
						lerp(v, lerp(u, grad(p[aa + 1], x, y, z - 1), grad(p[ba + 1], x - 1, y, z - 1)),
								lerp(u, grad(p[ab + 1], x, y - 1, z - 1), grad(p[bb + 1], x - 1, y - 1, z - 1))));
			}

			private static double fade(double t) {
				return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
			}

			private static double lerp(double t, double a, double b) {
				return a + t * (b - a);
			}

			private static double grad(int hash, double x, double y, double z) {
				int h = hash & 15;
				double u = h < 8 ? x : y;
				double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
				return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
			}
		}
	}

	public static final class TreeOfMightShapes {
		private static final int CACHE_LIMIT = 8;
		private static final Map<Key, TreeOfMightShape> CACHE = new ConcurrentHashMap<>();

		private record Key(long seed, int centerX, int centerZ, int version) {}

		private TreeOfMightShapes() {}

		public static TreeOfMightShape cached(long seed, int centerX, int centerZ) {
			return CACHE.get(new Key(seed, centerX, centerZ, TreeOfMightShape.VERSION));
		}

		public static void remember(TreeOfMightShape shape) {
			if (CACHE.size() >= CACHE_LIMIT) CACHE.clear();
			CACHE.put(new Key(shape.seed(), shape.centerX(), shape.centerZ(), TreeOfMightShape.VERSION), shape);
		}

		public static long seedFor(long levelSeed, ChunkPos origin) {
			WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
			random.setLargeFeatureSeed(levelSeed, origin.x, origin.z);
			return random.nextLong();
		}

		public static TreeOfMightShape planned(ServerLevel level, ChunkGenerator generator, ChunkPos origin) {
			long seed = seedFor(level.getSeed(), origin);
			int centerX = origin.getMiddleBlockX();
			int centerZ = origin.getMiddleBlockZ();
			TreeOfMightShape shape = cached(seed, centerX, centerZ);
			if (shape != null) return shape;
			RandomState randomState = level.getChunkSource().randomState();
			shape = TreeOfMightShape.create(centerX, centerZ, seed, level.getMinBuildHeight(), level.getMaxBuildHeight(),
					generator.getSeaLevel(), (x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState));
			remember(shape);
			return shape;
		}
	}

	public static final class TreeOfMightWriter {
		private static final int LIGHT_SPACING = 6;
		private static final int LIGHT_HEIGHT = 6;
		private static final int LIGHT_LEVEL = 15;
		private static final int GROUND_SCAN_UP = 36;
		private static final int GROUND_SCAN_DOWN = 40;
		private static final int ARENA_FILL_DEPTH = 16;

		private TreeOfMightWriter() {}

		public static void place(WorldGenLevel level, TreeOfMightShape tree, ChunkPos chunkPos, BoundingBox box) {
			int x0 = chunkPos.getMinBlockX();
			int z0 = chunkPos.getMinBlockZ();
			int y0 = tree.minY();
			int sy = tree.maxY() - y0 + 1;
			Region region = new Region(Math.max(box.minX(), x0), Math.min(box.maxX(), x0 + 15),
					Math.max(box.minY(), y0), Math.min(box.maxY(), tree.maxY()),
					Math.max(box.minZ(), z0), Math.min(box.maxZ(), z0 + 15));
			if (region.isEmpty()) return;

			byte[] buffer = new byte[16 * sy * 16];
			tree.rasterize(x0, y0, z0, 16, sy, 16, buffer);
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

			shapeArena(level, tree, buffer, x0, z0, y0, region, cursor);
			tree.applyRoots(x0, y0, z0, 16, sy, 16, buffer, (x, y, z) -> blocksRoot(level.getBlockState(cursor.set(x, y, z))));
			extendBase(level, tree, buffer, x0, z0, y0, sy, cursor);
			byte[] faces = new byte[buffer.length];
			tree.applyLichen(x0, y0, z0, 16, sy, 16, buffer, faces);
			placeLights(level, tree, buffer, x0, z0, y0, region, cursor);
			write(level, buffer, faces, x0, z0, y0, region, cursor);
		}

		public static boolean isTreeBlock(BlockState state) {
			return state.is(MainBlocks.MIGHT_TREE_WOOD.get()) || state.is(MainBlocks.MIGHT_TREE_ROOT.get())
					|| state.is(MainBlocks.MIGHT_TREE_LEAVES.get()) || state.is(MainBlocks.MIGHT_TREE_FRUIT_BLOCK.get());
		}

		private record Region(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
			boolean isEmpty() {
				return minX > maxX || minY > maxY || minZ > maxZ;
			}

			boolean contains(int x, int y, int z) {
				return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
			}
		}

		private static boolean blocksRoot(BlockState state) {
			if (state.isAir() || isTreeBlock(state)) return false;
			return !state.canBeReplaced();
		}

		private static boolean isRemovable(WorldGenLevel level, BlockPos pos, BlockState state) {
			if (state.is(Blocks.BEDROCK) || state.hasBlockEntity()) return false;
			return state.getDestroySpeed(level, pos) >= 0.0F;
		}

		private static int index(int dx, int dy, int dz) {
			return TreeOfMightShape.index(16, 16, dx, dy, dz);
		}

		private static void shapeArena(WorldGenLevel level, TreeOfMightShape tree, byte[] buffer, int x0, int z0, int y0,
									   Region region, BlockPos.MutableBlockPos cursor) {
			int floor = tree.arenaFloor();
			double reach = TreeOfMightShape.ARENA_RADIUS + TreeOfMightShape.ARENA_BLEND;
			for (int z = region.minZ(); z <= region.maxZ(); z++) {
				for (int x = region.minX(); x <= region.maxX(); x++) {
					double distance = tree.arenaDistance(x, z);
					if (distance > reach) continue;
					int dx = x - x0;
					int dz = z - z0;
					if (distance <= TreeOfMightShape.ARENA_RADIUS) {
						for (int y = floor + 1; y <= floor + TreeOfMightShape.ARENA_HEADROOM; y++) {
							if (!region.contains(x, y, z) || buffer[index(dx, y - y0, dz)] != TreeOfMightShape.NONE) continue;
							BlockState state = level.getBlockState(cursor.set(x, y, z));
							if (state.isAir() || isTreeBlock(state) || !isRemovable(level, cursor, state)) continue;
							level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
						}
						fillColumn(level, buffer, x, z, dx, dz, y0, floor, true, region, cursor);
					} else {
						int target = floor - (int) Math.ceil((distance - TreeOfMightShape.ARENA_RADIUS) * 0.6D);
						fillColumn(level, buffer, x, z, dx, dz, y0, target, false, region, cursor);
					}
				}
			}
		}

		private static void fillColumn(WorldGenLevel level, byte[] buffer, int x, int z, int dx, int dz, int y0, int top,
									   boolean resurface, Region region, BlockPos.MutableBlockPos cursor) {
			for (int y = top, depth = 0; y >= y0 && depth <= ARENA_FILL_DEPTH; y--, depth++) {
				BlockState state = level.getBlockState(cursor.set(x, y, z));
				boolean solid = blocksRoot(state);
				if (solid && (y < top || !resurface)) return;
				if (!region.contains(x, y, z) || buffer[index(dx, y - y0, dz)] != TreeOfMightShape.NONE) continue;
				if (isTreeBlock(state) || !isRemovable(level, cursor, state)) continue;
				level.setBlock(cursor, y == top ? surfaceFor(x, z) : Blocks.DIRT.defaultBlockState(), 2);
			}
		}

		private static BlockState surfaceFor(int x, int z) {
			int patch = Math.floorMod(hash(x >> 2, z >> 2) + Math.floorMod(hash(x, z), 3), 10);
			if (patch < 3) return Blocks.ROOTED_DIRT.defaultBlockState();
			if (patch < 6) return Blocks.COARSE_DIRT.defaultBlockState();
			if (patch < 8) return Blocks.MUD.defaultBlockState();
			return Blocks.PODZOL.defaultBlockState();
		}

		private static int hash(int x, int z) {
			int h = x * 73428767 ^ z * 912931;
			h ^= h >>> 13;
			h *= 0x5BD1E995;
			return h ^ (h >>> 15);
		}

		private static void extendBase(WorldGenLevel level, TreeOfMightShape tree, byte[] buffer, int x0, int z0, int y0, int sy,
									   BlockPos.MutableBlockPos cursor) {
			for (int dz = 0; dz < 16; dz++) {
				for (int dx = 0; dx < 16; dx++) {
					int bottom = tree.baseBottom(buffer, 16, sy, 16, dx, dz, y0);
					if (bottom == Integer.MIN_VALUE) continue;
					for (int y = bottom - 1, depth = 0; y >= y0 && depth < TreeOfMightShape.MAX_BASE_EXTENSION; y--, depth++) {
						int index = index(dx, y - y0, dz);
						if (buffer[index] == TreeOfMightShape.WOOD) break;
						if (blocksRoot(level.getBlockState(cursor.set(x0 + dx, y, z0 + dz)))) break;
						buffer[index] = TreeOfMightShape.WOOD;
					}
				}
			}
		}

		private static void placeLights(WorldGenLevel level, TreeOfMightShape tree, byte[] buffer, int x0, int z0, int y0,
										Region region, BlockPos.MutableBlockPos cursor) {
			for (int z = region.minZ(); z <= region.maxZ(); z++) {
				if (Math.floorMod(z, LIGHT_SPACING) != 0) continue;
				for (int x = region.minX(); x <= region.maxX(); x++) {
					if (Math.floorMod(x, LIGHT_SPACING) != 0 || !tree.isUnderCanopy(x, z)) continue;
					int ground = findGround(level, tree, x, z, cursor);
					if (ground == Integer.MIN_VALUE) continue;
					int y = ground + LIGHT_HEIGHT;
					if (!region.contains(x, y, z) || buffer[index(x - x0, y - y0, z - z0)] != TreeOfMightShape.NONE) continue;
					BlockState current = level.getBlockState(cursor.set(x, y, z));
					boolean water = isWaterSource(current);
					if (!current.isAir() && !water) continue;
					level.setBlock(cursor, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LIGHT_LEVEL)
							.setValue(LightBlock.WATERLOGGED, water), 2);
				}
			}
		}

		private static boolean isWaterSource(BlockState state) {
			FluidState fluid = state.getFluidState();
			return fluid.is(Fluids.WATER) && fluid.isSource();
		}

		private static int findGround(WorldGenLevel level, TreeOfMightShape tree, int x, int z, BlockPos.MutableBlockPos cursor) {
			int top = tree.baseY() + GROUND_SCAN_UP;
			if (blocksRoot(level.getBlockState(cursor.set(x, top, z)))) return Integer.MIN_VALUE;
			for (int y = top - 1; y >= tree.baseY() - GROUND_SCAN_DOWN; y--) {
				if (blocksRoot(level.getBlockState(cursor.set(x, y, z)))) return y;
			}
			return Integer.MIN_VALUE;
		}

		private static BlockState lichenState(int mask, boolean water) {
			BlockState state = Blocks.GLOW_LICHEN.defaultBlockState();
			for (int face = 0; face < 6; face++) {
				if ((mask & (1 << face)) != 0) state = state.setValue(MultifaceBlock.getFaceProperty(Direction.from3DDataValue(face)), true);
			}
			return state.setValue(BlockStateProperties.WATERLOGGED, water);
		}

		private static void write(WorldGenLevel level, byte[] buffer, byte[] faces, int x0, int z0, int y0, Region region,
								  BlockPos.MutableBlockPos cursor) {
			BlockState wood = MainBlocks.MIGHT_TREE_WOOD.get().defaultBlockState();
			BlockState root = MainBlocks.MIGHT_TREE_ROOT.get().defaultBlockState();
			BlockState leaves = MainBlocks.MIGHT_TREE_LEAVES.get().defaultBlockState();
			BlockState fruit = MainBlocks.MIGHT_TREE_FRUIT_BLOCK.get().defaultBlockState();
			for (int y = region.minY(); y <= region.maxY(); y++) {
				for (int z = region.minZ(); z <= region.maxZ(); z++) {
					for (int x = region.minX(); x <= region.maxX(); x++) {
						int index = index(x - x0, y - y0, z - z0);
						byte code = buffer[index];
						if (code == TreeOfMightShape.NONE) continue;
						BlockState current = level.getBlockState(cursor.set(x, y, z));
						BlockState next;
						if (code == TreeOfMightShape.WOOD) {
							if (!isTreeBlock(current) && !isRemovable(level, cursor, current)) continue;
							next = wood;
						} else if (code == TreeOfMightShape.ROOT || code == TreeOfMightShape.FRUIT) {
							if (blocksRoot(current)) continue;
							next = code == TreeOfMightShape.ROOT ? root : fruit;
						} else if (code == TreeOfMightShape.LICHEN) {
							boolean water = isWaterSource(current);
							if (!current.isAir() && !water) continue;
							next = lichenState(faces[index], water);
						} else {
							if (!current.isAir() && !current.canBeReplaced() && !current.is(BlockTags.LEAVES) && !isTreeBlock(current)) continue;
							next = leaves;
						}
						if (current == next) continue;
						level.setBlock(cursor, next, 2);
					}
				}
			}
		}
	}

	public static class TreeOfMightCanopyFeature extends Feature<NoneFeatureConfiguration> {
		private static final int VANILLA_REFERENCE_CHUNKS = 8;

		public TreeOfMightCanopyFeature(Codec<NoneFeatureConfiguration> codec) {
			super(codec);
		}

		@Override
		public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
			WorldGenLevel level = context.level();
			ServerLevel serverLevel = level.getLevel();
			ChunkPos origin = StructureSpawnPlanner.publishedPositions(serverLevel).get(DMZStructureSets.TREE_OF_MIGHT_SALT);
			if (origin == null) return false;
			ChunkPos chunk = new ChunkPos(context.origin());
			if (Math.max(Math.abs(chunk.x - origin.x), Math.abs(chunk.z - origin.z)) <= VANILLA_REFERENCE_CHUNKS) return false;

			TreeOfMightShape tree = TreeOfMightShapes.planned(serverLevel, context.chunkGenerator(), origin);
			if (!tree.intersectsColumns(chunk.getMinBlockX(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), chunk.getMaxBlockZ())) return false;

			TreeOfMightWriter.place(level, tree, chunk, new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
					chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ()));
			Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(DMZStructures.TREE_OF_MIGHT);
			if (structure != null) level.getChunk(chunk.x, chunk.z).addReferenceForStructure(structure, origin.toLong());
			return true;
		}
	}

	public static class GeteStarStructure extends Structure implements IncrementalStructure {
		public static final Codec<GeteStarStructure> CODEC = simpleCodec(GeteStarStructure::new);

		private static final int DRAIN_SCAN_UP = 36;
		private static final int DRAIN_SCAN_DOWN = 24;

		public GeteStarStructure(StructureSettings settings) {
			super(settings);
		}

		@Override
		protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
			ChunkPos chunkPos = context.chunkPos();
			ChunkGenerator generator = context.chunkGenerator();
			LevelHeightAccessor heightAccessor = context.heightAccessor();
			RandomState randomState = context.randomState();

			GeteStarShape shape = GeteStarShape.create(chunkPos.getMiddleBlockX(), chunkPos.getMiddleBlockZ(),
					GeteStarShape.seedFor(context.seed(), chunkPos), heightAccessor.getMinBuildHeight(), heightAccessor.getMaxBuildHeight(),
					generator.getSeaLevel(), (x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState));
			Piece piece = new Piece(shape);
			return Optional.of(new GenerationStub(new BlockPos(shape.centerX(), shape.baseY(), shape.centerZ()), builder -> builder.addPiece(piece)));
		}

		@Override
		public StructureType<?> type() {
			return MainStructureTypes.GETE_STAR.get();
		}

		public static boolean isStarBlock(BlockState state) {
			return state.is(MainBlocks.GETE_STAR_HULL.get()) || state.is(MainBlocks.GETE_STAR_CIRCUIT.get());
		}

		static void place(WorldGenLevel level, GeteStarShape shape, ChunkPos chunkPos, BoundingBox box) {
			int minX = Math.max(Math.max(box.minX(), chunkPos.getMinBlockX()), shape.minX());
			int maxX = Math.min(Math.min(box.maxX(), chunkPos.getMaxBlockX()), shape.maxX());
			int minZ = Math.max(Math.max(box.minZ(), chunkPos.getMinBlockZ()), shape.minZ());
			int maxZ = Math.min(Math.min(box.maxZ(), chunkPos.getMaxBlockZ()), shape.maxZ());
			int minY = Math.max(box.minY(), shape.minY());
			int maxY = Math.min(box.maxY(), shape.maxY());
			if (minX > maxX || minZ > maxZ || minY > maxY) return;

			List<GeteStarShape.Cable> cables = shape.cablesIn(minX, minZ, maxX, maxZ);
			BlockState hull = MainBlocks.GETE_STAR_HULL.get().defaultBlockState();
			BlockState circuit = MainBlocks.GETE_STAR_CIRCUIT.get().defaultBlockState();
			BlockState glass = Blocks.RED_STAINED_GLASS.defaultBlockState();
			BlockState air = Blocks.AIR.defaultBlockState();
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

			for (int z = minZ; z <= maxZ; z++) {
				for (int x = minX; x <= maxX; x++) {
					for (int y = minY; y <= maxY; y++) {
						byte code = shape.classify(x, y, z, cables);
						if (code == GeteStarShape.NONE) continue;
						BlockState current = level.getBlockState(cursor.set(x, y, z));
						if (current.is(Blocks.BEDROCK) || current.hasBlockEntity()) continue;
						BlockState next;
						if (code == GeteStarShape.AIR) {
							if (current.isAir()) continue;
							next = air;
						} else if (code == GeteStarShape.SUPPORT) {
							if (!current.isAir() && !current.canBeReplaced() && current.getFluidState().isEmpty()) continue;
							next = hull;
						} else if (code == GeteStarShape.CIRCUIT) {
							next = circuit;
						} else if (code == GeteStarShape.GLASS) {
							next = glass;
						} else {
							next = hull;
						}
						if (current == next) continue;
						level.setBlock(cursor, next, 2);
					}
					drain(level, shape, x, z, minY, maxY, cursor);
				}
			}
		}

		private static void drain(WorldGenLevel level, GeteStarShape shape, int x, int z, int minY, int maxY, BlockPos.MutableBlockPos cursor) {
			double distance = shape.horizontalDistance(x, z);
			if (distance <= GeteStarShape.RADIUS - 1) return;
			double limit = shape.drainRadius(shape.angleOf(x, z));
			if (distance > limit) return;
			double chance = Math.min(1.0D, (limit - distance) / GeteStarShape.DRAIN_FADE);
			if (Math.floorMod(hash(x, z), 1000) >= chance * 1000.0D) return;

			int top = shape.baseY() + DRAIN_SCAN_UP;
			int bottom = shape.baseY() - DRAIN_SCAN_DOWN;
			for (int y = top; y >= bottom; y--) {
				BlockState state = level.getBlockState(cursor.set(x, y, z));
				if (state.isAir()) continue;
				if (isStarBlock(state) || !state.getFluidState().isEmpty()) return;
				if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || !state.blocksMotion()) continue;
				if (!state.is(BlockTags.DIRT)) return;

				if (y >= minY && y <= maxY) level.setBlock(cursor, drainedSurface(x, z), 2);
				int above = y + 1;
				if (above >= minY && above <= maxY) {
					BlockState plant = level.getBlockState(cursor.set(x, above, z));
					if (!plant.isAir() && plant.canBeReplaced() && plant.getFluidState().isEmpty()) {
						level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
					}
				}
				return;
			}
		}

		private static BlockState drainedSurface(int x, int z) {
			int pick = Math.floorMod(hash(x * 3 + 7, z * 5 - 11), 1000);
			if (pick < 6) return MainBlocks.GETE_ORE.get().defaultBlockState();
			int patch = Math.floorMod(hash(x >> 2, z >> 2) + Math.floorMod(hash(x, z), 3), 10);
			if (patch < 4) return Blocks.COARSE_DIRT.defaultBlockState();
			if (patch < 6) return Blocks.GRAVEL.defaultBlockState();
			if (patch < 8) return MainBlocks.NAMEK_DIRT.get().defaultBlockState();
			return Blocks.TUFF.defaultBlockState();
		}

		private static int hash(int x, int z) {
			int h = x * 73428767 ^ z * 912931;
			h ^= h >>> 13;
			h *= 0x5BD1E995;
			return h ^ (h >>> 15);
		}

		public static class Piece extends StructurePiece {
			private final int version;
			private final long seed;
			private final int centerX;
			private final int centerZ;
			private final int minBuildY;
			private final int maxBuildY;
			private final int seaLevel;
			private final long[] sampleKeys;
			private final int[] sampleValues;
			private volatile GeteStarShape shape;

			public Piece(GeteStarShape shape) {
				super(MainStructureTypes.GETE_STAR_PIECE.get(), 0,
						new BoundingBox(shape.minX(), shape.minY(), shape.minZ(), shape.maxX(), shape.maxY(), shape.maxZ()));
				this.version = GeteStarShape.VERSION;
				this.seed = shape.seed();
				this.centerX = shape.centerX();
				this.centerZ = shape.centerZ();
				this.minBuildY = shape.minBuildY();
				this.maxBuildY = shape.maxBuildY();
				this.seaLevel = shape.seaLevel();
				Map<Long, Integer> samples = shape.groundSamples();
				this.sampleKeys = new long[samples.size()];
				this.sampleValues = new int[samples.size()];
				int i = 0;
				for (Map.Entry<Long, Integer> entry : samples.entrySet()) {
					this.sampleKeys[i] = entry.getKey();
					this.sampleValues[i] = entry.getValue();
					i++;
				}
				this.shape = shape;
				GeteStarShape.remember(shape);
			}

			public Piece(CompoundTag tag) {
				super(MainStructureTypes.GETE_STAR_PIECE.get(), tag);
				this.version = tag.getInt("ShapeVersion");
				this.seed = tag.getLong("Seed");
				this.centerX = tag.getInt("CenterX");
				this.centerZ = tag.getInt("CenterZ");
				this.minBuildY = tag.getInt("MinBuildY");
				this.maxBuildY = tag.getInt("MaxBuildY");
				this.seaLevel = tag.getInt("SeaLevel");
				this.sampleKeys = tag.getLongArray("SampleKeys");
				this.sampleValues = tag.getIntArray("SampleValues");
			}

			@Override
			protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
				tag.putInt("ShapeVersion", this.version);
				tag.putLong("Seed", this.seed);
				tag.putInt("CenterX", this.centerX);
				tag.putInt("CenterZ", this.centerZ);
				tag.putInt("MinBuildY", this.minBuildY);
				tag.putInt("MaxBuildY", this.maxBuildY);
				tag.putInt("SeaLevel", this.seaLevel);
				tag.putLongArray("SampleKeys", this.sampleKeys);
				tag.putIntArray("SampleValues", this.sampleValues);
			}

			public GeteStarShape shape() {
				GeteStarShape current = this.shape;
				if (current != null) return current;
				current = GeteStarShape.cached(this.seed, this.centerX, this.centerZ);
				if (current == null) {
					Map<Long, Integer> samples = new HashMap<>();
					for (int i = 0; i < Math.min(this.sampleKeys.length, this.sampleValues.length); i++) {
						samples.put(this.sampleKeys[i], this.sampleValues[i]);
					}
					int fallback = samples.getOrDefault(GeteStarShape.pack(this.centerX, this.centerZ), this.seaLevel);
					current = GeteStarShape.create(this.centerX, this.centerZ, this.seed, this.minBuildY, this.maxBuildY, this.seaLevel,
							(x, z) -> samples.getOrDefault(GeteStarShape.pack(x, z), fallback));
					GeteStarShape.remember(current);
				}
				this.shape = current;
				return current;
			}

			@Override
			public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
									RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
				place(level, shape(), chunkPos, box);
			}
		}
	}

	public static final class GeteStarShape {
		public static final int VERSION = 2;

		public static final byte NONE = 0;
		public static final byte AIR = 1;
		public static final byte HULL = 2;
		public static final byte CIRCUIT = 3;
		public static final byte GLASS = 4;
		public static final byte SUPPORT = 5;

		public static final int RADIUS = 60;
		public static final int SPHERE_LIFT = 20;
		public static final int CHAMBER_RADIUS = 36;
		public static final int FLOOR_DROP = 10;
		public static final int RING_INNER = 41;
		public static final int RING_OUTER = 50;
		public static final int CORRIDOR_HEIGHT = 9;
		public static final int DOOR_HALF_WIDTH = 3;
		public static final int DOOR_HEIGHT = 8;
		public static final int TUNNEL_HALF_WIDTH = 4;
		public static final int RAMP_LENGTH = 20;
		public static final int CORE_DISTANCE = 26;
		public static final int CAPSULE_HEIGHT = 6;
		public static final int DRAIN_FADE = 16;

		private static final double[] DOOR_ANGLES = {0.0D, Math.PI / 2.0D, -Math.PI / 2.0D};
		private static final double[] CHAMBER_CAPSULE_ANGLES = {Math.PI / 4.0D, -Math.PI / 4.0D, Math.PI * 3.0D / 4.0D, -Math.PI * 3.0D / 4.0D};
		private static final double RING_CAPSULE_STEP = Math.toRadians(14.0D);
		private static final double RING_CAPSULE_CLEARANCE = Math.toRadians(10.0D);
		private static final double DRAIN_BASE = 108.0D;
		private static final int LEG_COUNT = 10;
		private static final int TENDRIL_COUNT = 12;
		private static final int CABLE_SEGMENTS = 32;
		private static final int ANTENNA_COUNT = 9;
		private static final double KNEE_T = 0.42D;
		private static final double FOOT_RADIUS = 5.5D;
		private static final int CACHE_LIMIT = 4;
		private static final Map<Key, GeteStarShape> CACHE = new ConcurrentHashMap<>();

		public interface Surface {
			int at(int x, int z);
		}

		public record Cable(double[] xs, double[] ys, double[] zs, double[] radii, double[] lengths,
							int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			boolean intersects(int x0, int z0, int x1, int z1) {
				return this.maxX >= x0 && this.minX <= x1 && this.maxZ >= z0 && this.minZ <= z1;
			}
		}

		private record Antenna(int x, int z, int baseY, int height) {}

		private record Joint(double x, double y, double z, double radius) {}

		private record Foot(double x, double z, int groundY) {}

		private record Key(long seed, int centerX, int centerZ, int version) {}

		private final long seed;
		private final int centerX;
		private final int centerZ;
		private final int minBuildY;
		private final int maxBuildY;
		private final int seaLevel;
		private final Map<Long, Integer> samples = new LinkedHashMap<>();

		private final double sphereX;
		private final double sphereZ;
		private final int baseY;
		private final double sphereY;
		private final int floorY;
		private final double entranceAngle;
		private final double entranceX;
		private final double entranceZ;
		private final int rampEndY;
		private final double[] drainAmp = new double[3];
		private final double[] drainPhase = new double[3];
		private final List<Cable> cables = new ArrayList<>();
		private final List<Joint> joints = new ArrayList<>();
		private final List<Foot> feet = new ArrayList<>();
		private final List<Antenna> antennas = new ArrayList<>();

		private final int minX;
		private final int minY;
		private final int minZ;
		private final int maxX;
		private final int maxY;
		private final int maxZ;

		private GeteStarShape(int centerX, int centerZ, long seed, int minBuildY, int maxBuildY, int seaLevel, Surface surface) {
			this.seed = seed;
			this.centerX = centerX;
			this.centerZ = centerZ;
			this.minBuildY = minBuildY;
			this.maxBuildY = maxBuildY;
			this.seaLevel = seaLevel;
			Surface sampler = (x, z) -> this.samples.computeIfAbsent(pack(x, z), key -> surface.at(x, z));

			Random random = new Random(seed * 0x9E3779B97F4A7C15L + VERSION);
			this.sphereX = centerX + 0.5D;
			this.sphereZ = centerZ + 0.5D;

			int groundSum = sampler.at(centerX, centerZ);
			for (int i = 0; i < 8; i++) {
				double angle = i * Math.PI / 4.0D;
				groundSum += sampler.at(centerX + (int) Math.round(Math.cos(angle) * 40.0D), centerZ + (int) Math.round(Math.sin(angle) * 40.0D));
			}
			this.baseY = Math.max(Math.floorDiv(groundSum, 9), seaLevel);
			this.sphereY = this.baseY + SPHERE_LIFT + 0.5D;
			this.floorY = this.baseY + SPHERE_LIFT - FLOOR_DROP;

			this.entranceAngle = random.nextDouble() * Math.PI * 2.0D;
			this.entranceX = Math.cos(this.entranceAngle);
			this.entranceZ = Math.sin(this.entranceAngle);

			double rampEnd = RADIUS + RAMP_LENGTH;
			int rampGround = sampler.at((int) Math.floor(this.sphereX + this.entranceX * rampEnd), (int) Math.floor(this.sphereZ + this.entranceZ * rampEnd));
			this.rampEndY = Math.max(this.floorY - 16, Math.min(this.floorY, rampGround));

			for (int i = 0; i < this.drainAmp.length; i++) {
				this.drainAmp[i] = 2.0D + random.nextDouble() * 3.0D;
				this.drainPhase[i] = random.nextDouble() * Math.PI * 2.0D;
			}

			buildAntennas(random);
			buildLegs(random, sampler);
			buildTendrils(random, sampler);

			int reach = (int) Math.ceil(maxDrainRadius()) + 2;
			int lowest = this.baseY - 44;
			for (Cable cable : this.cables) {
				reach = Math.max(reach, Math.max(Math.max(Math.abs(cable.minX() - centerX), Math.abs(cable.maxX() - centerX)),
						Math.max(Math.abs(cable.minZ() - centerZ), Math.abs(cable.maxZ() - centerZ))) + 1);
				lowest = Math.min(lowest, cable.minY() - 1);
			}
			for (Foot foot : this.feet) {
				reach = Math.max(reach, (int) Math.ceil(Math.max(Math.abs(foot.x() - centerX), Math.abs(foot.z() - centerZ)) + FOOT_RADIUS) + 1);
				lowest = Math.min(lowest, foot.groundY() - 3);
			}
			int highest = (int) Math.ceil(this.sphereY + RADIUS) + 2;
			for (Antenna antenna : this.antennas) highest = Math.max(highest, antenna.baseY() + antenna.height() + 1);
			for (Joint joint : this.joints) highest = Math.max(highest, (int) Math.ceil(joint.y() + joint.radius()) + 1);

			this.minX = centerX - reach;
			this.maxX = centerX + reach;
			this.minZ = centerZ - reach;
			this.maxZ = centerZ + reach;
			this.minY = Math.max(minBuildY + 1, lowest);
			this.maxY = Math.min(maxBuildY - 2, highest);
		}

		public static GeteStarShape create(int centerX, int centerZ, long seed, int minBuildY, int maxBuildY, int seaLevel, Surface surface) {
			return new GeteStarShape(centerX, centerZ, seed, minBuildY, maxBuildY, seaLevel, surface);
		}

		public static long pack(int x, int z) {
			return ((long) x << 32) | (z & 0xFFFFFFFFL);
		}

		public static GeteStarShape cached(long seed, int centerX, int centerZ) {
			return CACHE.get(new Key(seed, centerX, centerZ, VERSION));
		}

		public static void remember(GeteStarShape shape) {
			if (CACHE.size() >= CACHE_LIMIT) CACHE.clear();
			CACHE.put(new Key(shape.seed, shape.centerX, shape.centerZ, VERSION), shape);
		}

		public static long seedFor(long levelSeed, ChunkPos origin) {
			WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
			random.setLargeFeatureSeed(levelSeed, origin.x, origin.z);
			return random.nextLong();
		}

		public static GeteStarShape planned(ServerLevel level, ChunkGenerator generator, ChunkPos origin) {
			long seed = seedFor(level.getSeed(), origin);
			int centerX = origin.getMiddleBlockX();
			int centerZ = origin.getMiddleBlockZ();
			GeteStarShape shape = cached(seed, centerX, centerZ);
			if (shape != null) return shape;
			RandomState randomState = level.getChunkSource().randomState();
			shape = create(centerX, centerZ, seed, level.getMinBuildHeight(), level.getMaxBuildHeight(), generator.getSeaLevel(),
					(x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState));
			remember(shape);
			return shape;
		}

		private void buildAntennas(Random random) {
			for (int i = 0; i < ANTENNA_COUNT; i++) {
				double azimuth = random.nextDouble() * Math.PI * 2.0D;
				double polar = i == 0 ? 0.0D : Math.toRadians(12.0D + random.nextDouble() * 38.0D);
				double horizontal = RADIUS * Math.sin(polar);
				int x = (int) Math.floor(this.sphereX + Math.cos(azimuth) * horizontal);
				int z = (int) Math.floor(this.sphereZ + Math.sin(azimuth) * horizontal);
				double dx = x + 0.5D - this.sphereX;
				double dz = z + 0.5D - this.sphereZ;
				double inside = RADIUS * RADIUS - dx * dx - dz * dz;
				int surfaceY = (int) Math.floor(this.sphereY + Math.sqrt(Math.max(0.0D, inside)));
				int height = i == 0 ? 34 : 14 + random.nextInt(15);
				this.antennas.add(new Antenna(x, z, surfaceY - 2, height + 2));
			}
		}

		private void buildLegs(Random random, Surface sampler) {
			double span = Math.PI * 2.0D - Math.toRadians(44.0D);
			for (int i = 0; i < LEG_COUNT; i++) {
				double azimuth = this.entranceAngle + Math.toRadians(22.0D) + span * (i + 0.5D) / LEG_COUNT
						+ Math.toRadians((random.nextDouble() - 0.5D) * 10.0D);
				double elevation = Math.toRadians(-18.0D + random.nextDouble() * 16.0D);
				double reach = 92.0D + random.nextDouble() * 20.0D;
				double lift = 26.0D + random.nextDouble() * 8.0D;
				double endAzimuth = azimuth + Math.toRadians((random.nextDouble() - 0.5D) * 12.0D);
				double footX = this.sphereX + Math.cos(endAzimuth) * reach;
				double footZ = this.sphereZ + Math.sin(endAzimuth) * reach;
				int ground = sampler.at((int) Math.floor(footX), (int) Math.floor(footZ));
				Cable leg = curve(azimuth, elevation, endAzimuth, reach, ground - 5.0D, 26.0D, lift, lift * 0.9D, 4.0D,
						4.2D + random.nextDouble() * 0.6D, 2.6D + random.nextDouble() * 0.4D);
				this.cables.add(leg);
				int knee = (int) Math.round(KNEE_T * CABLE_SEGMENTS);
				this.joints.add(new Joint(leg.xs()[knee], leg.ys()[knee], leg.zs()[knee], leg.radii()[knee] + 1.4D));
				this.feet.add(new Foot(footX, footZ, ground));
			}
		}

		private void buildTendrils(Random random, Surface sampler) {
			for (int i = 0; i < TENDRIL_COUNT; i++) {
				double azimuth = random.nextDouble() * Math.PI * 2.0D;
				if (Math.abs(wrap(azimuth - this.entranceAngle)) < Math.toRadians(16.0D)) azimuth += Math.toRadians(32.0D);
				double elevation = Math.toRadians(-26.0D + random.nextDouble() * 34.0D);
				double reach = 64.0D + random.nextDouble() * 20.0D;
				double endAzimuth = azimuth + Math.toRadians((random.nextDouble() - 0.5D) * 30.0D);
				int ground = sampler.at((int) Math.floor(this.sphereX + Math.cos(endAzimuth) * reach),
						(int) Math.floor(this.sphereZ + Math.sin(endAzimuth) * reach));
				double lift = 6.0D + random.nextDouble() * 8.0D;
				this.cables.add(curve(azimuth, elevation, endAzimuth, reach, ground - 3.0D, 12.0D, lift, lift + 6.0D, 8.0D,
						1.8D + random.nextDouble() * 0.4D, 1.0D + random.nextDouble() * 0.3D));
			}
		}

		private Cable curve(double azimuth, double elevation, double endAzimuth, double reach, double endY, double startPush,
							double startLift, double endLift, double endPull, double startRadius, double endRadius) {
			double ox = Math.cos(azimuth);
			double oz = Math.sin(azimuth);
			double startR = RADIUS - 1.5D;
			double p0x = this.sphereX + ox * Math.cos(elevation) * startR;
			double p0y = this.sphereY + Math.sin(elevation) * startR;
			double p0z = this.sphereZ + oz * Math.cos(elevation) * startR;
			double p3x = this.sphereX + Math.cos(endAzimuth) * reach;
			double p3z = this.sphereZ + Math.sin(endAzimuth) * reach;
			double p1x = p0x + ox * startPush;
			double p1y = p0y + startLift;
			double p1z = p0z + oz * startPush;
			double p2x = p3x - Math.cos(endAzimuth) * endPull;
			double p2y = Math.max(endY + endLift, p1y - 4.0D);
			double p2z = p3z - Math.sin(endAzimuth) * endPull;

			double[] xs = new double[CABLE_SEGMENTS + 1];
			double[] ys = new double[CABLE_SEGMENTS + 1];
			double[] zs = new double[CABLE_SEGMENTS + 1];
			double[] radii = new double[CABLE_SEGMENTS + 1];
			double[] lengths = new double[CABLE_SEGMENTS + 1];
			double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
			double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
			for (int s = 0; s <= CABLE_SEGMENTS; s++) {
				double t = s / (double) CABLE_SEGMENTS;
				double u = 1.0D - t;
				double a = u * u * u, b = 3.0D * u * u * t, c = 3.0D * u * t * t, d = t * t * t;
				xs[s] = a * p0x + b * p1x + c * p2x + d * p3x;
				ys[s] = a * p0y + b * p1y + c * p2y + d * endY;
				zs[s] = a * p0z + b * p1z + c * p2z + d * p3z;
				radii[s] = startRadius + (endRadius - startRadius) * t;
				if (s > 0) {
					double lx = xs[s] - xs[s - 1], ly = ys[s] - ys[s - 1], lz = zs[s] - zs[s - 1];
					lengths[s] = lengths[s - 1] + Math.sqrt(lx * lx + ly * ly + lz * lz);
				}
				minX = Math.min(minX, xs[s] - radii[s]);
				minY = Math.min(minY, ys[s] - radii[s]);
				minZ = Math.min(minZ, zs[s] - radii[s]);
				maxX = Math.max(maxX, xs[s] + radii[s]);
				maxY = Math.max(maxY, ys[s] + radii[s]);
				maxZ = Math.max(maxZ, zs[s] + radii[s]);
			}
			return new Cable(xs, ys, zs, radii, lengths, (int) Math.floor(minX), (int) Math.floor(minY), (int) Math.floor(minZ),
					(int) Math.ceil(maxX), (int) Math.ceil(maxY), (int) Math.ceil(maxZ));
		}

		public List<Cable> cablesIn(int x0, int z0, int x1, int z1) {
			List<Cable> result = new ArrayList<>();
			for (Cable cable : this.cables) {
				if (cable.intersects(x0, z0, x1, z1)) result.add(cable);
			}
			return result;
		}

		public byte classify(int x, int y, int z, List<Cable> nearby) {
			double px = x + 0.5D;
			double py = y + 0.5D;
			double pz = z + 0.5D;
			double dx = px - this.sphereX;
			double dy = py - this.sphereY;
			double dz = pz - this.sphereZ;
			double h = Math.sqrt(dx * dx + dz * dz);
			double r = Math.sqrt(h * h + dy * dy);
			double s = dx * this.entranceX + dz * this.entranceZ;
			double t = dz * this.entranceX - dx * this.entranceZ;

			if (r <= RADIUS + RAMP_LENGTH + 2) {
				byte passage = classifyPassages(y, h, r, s, t, dx, dz);
				if (passage != NONE) return passage;
			}

			if (r <= RADIUS) return classifySolid(y, h, r, s, t, dx, dz, dy);

			if (r <= RADIUS + 2.5D && Math.abs(dy) <= 2.5D) {
				if (Math.abs(dy) < 0.5D) {
					double arc = wrap(Math.atan2(dz, dx) - this.entranceAngle) * RADIUS;
					if (Math.floorMod((int) Math.floor(arc), 12) == 0) return CIRCUIT;
				}
				return HULL;
			}

			byte antenna = classifyAntennas(x, y, z);
			if (antenna != NONE) return antenna;

			byte joint = classifyJoints(px, py, pz);
			if (joint != NONE) return joint;

			byte foot = classifyFeet(px, y, pz);
			if (foot != NONE) return foot;

			if (!nearby.isEmpty()) return classifyCables(px, py, pz, nearby);
			return NONE;
		}

		private byte classifyPassages(int y, double h, double r, double s, double t, double dx, double dz) {
			double absT = Math.abs(t);
			if (absT <= TUNNEL_HALF_WIDTH + 0.5D && s >= RING_INNER && s <= RADIUS + RAMP_LENGTH) {
				int floor = rampFloor(s);
				if (y > floor && y <= floor + CORRIDOR_HEIGHT) return AIR;
				if (s > RADIUS - 3) {
					if (y == floor) return absT < 0.5D && Math.floorMod((int) Math.floor(s), 4) == 0 ? CIRCUIT : HULL;
					if (y < floor && y >= floor - 10 && r > RADIUS) return SUPPORT;
				}
			}
			if (absT > TUNNEL_HALF_WIDTH + 0.5D && absT <= TUNNEL_HALF_WIDTH + 1.5D && s > RADIUS - 1 && s <= RADIUS + RAMP_LENGTH && r > RADIUS) {
				int floor = rampFloor(s);
				if (y == floor + 1) return CIRCUIT;
				if (y <= floor && y >= floor - 10) return SUPPORT;
			}

			if (y <= this.floorY) return NONE;

			if (r <= CHAMBER_RADIUS) return AIR;
			if (h >= RING_INNER && h < RING_OUTER && y <= this.floorY + CORRIDOR_HEIGHT) return AIR;

			for (double offset : DOOR_ANGLES) {
				double angle = this.entranceAngle + offset;
				double sd = dx * Math.cos(angle) + dz * Math.sin(angle);
				double td = dz * Math.cos(angle) - dx * Math.sin(angle);
				if (sd >= CHAMBER_RADIUS - 3 && sd <= RING_INNER + 0.5D && Math.abs(td) <= DOOR_HALF_WIDTH + 0.5D
						&& y <= this.floorY + DOOR_HEIGHT) return AIR;
			}

			for (double offset : CHAMBER_CAPSULE_ANGLES) {
				double angle = this.entranceAngle + offset;
				double sd = dx * Math.cos(angle) + dz * Math.sin(angle);
				double td = Math.abs(dz * Math.cos(angle) - dx * Math.sin(angle));
				if (sd < CHAMBER_RADIUS - 3 || sd > CHAMBER_RADIUS + 5) continue;
				if (td <= 2.5D && y <= this.floorY + CAPSULE_HEIGHT) return sd > CHAMBER_RADIUS + 4 ? CIRCUIT : AIR;
				if (td <= 3.5D && y <= this.floorY + CAPSULE_HEIGHT + 1 && r > CHAMBER_RADIUS) return CIRCUIT;
			}

			if (h >= RING_OUTER && h < RING_OUTER + 2 && y <= this.floorY + CAPSULE_HEIGHT) {
				double angle = Math.atan2(dz, dx);
				double relative = wrap(angle - this.entranceAngle);
				long index = Math.round(relative / RING_CAPSULE_STEP);
				double capsule = index * RING_CAPSULE_STEP;
				if (!nearDoor(capsule)) {
					double tangential = Math.abs(h * Math.sin(relative - capsule));
					if (tangential <= 1.5D) return h < RING_OUTER + 1 ? GLASS : CIRCUIT;
				}
			}
			return NONE;
		}

		private boolean nearDoor(double relative) {
			for (double door : DOOR_ANGLES) {
				if (Math.abs(wrap(relative - door)) < RING_CAPSULE_CLEARANCE) return true;
			}
			return false;
		}

		private byte classifySolid(int y, double h, double r, double s, double t, double dx, double dz, double dy) {
			if (r > RADIUS - 1) {
				if (s > RADIUS - 7 && Math.abs(t) <= TUNNEL_HALF_WIDTH + 3.5D && y > this.floorY - 1 && y <= this.floorY + CORRIDOR_HEIGHT + 3) {
					return CIRCUIT;
				}
				return isSeam(r, dx, dz, dy) ? CIRCUIT : HULL;
			}

			if (y == this.floorY && h <= CHAMBER_RADIUS - 1) {
				if (Math.abs(h - 12.0D) < 0.55D || Math.abs(h - 24.0D) < 0.55D || h < 1.5D) return CIRCUIT;
				return HULL;
			}

			if (y > this.floorY && r > CHAMBER_RADIUS && r <= CHAMBER_RADIUS + 1.2D) {
				if (y == this.floorY + 14 || y == this.floorY + 28) return CIRCUIT;
				double coreAngle = this.entranceAngle + Math.PI;
				double off = wrap(Math.atan2(dz, dx) - coreAngle);
				if (Math.abs(off) < 0.3D && y <= this.floorY + 34) {
					int stripe = (int) Math.floor(off * r);
					if (Math.floorMod(stripe, 3) == 0) return CIRCUIT;
				}
				return HULL;
			}

			double ringMiddle = (RING_INNER + RING_OUTER) * 0.5D;
			if (y == this.floorY + CORRIDOR_HEIGHT + 1 && h >= RING_INNER && h < RING_OUTER && Math.abs(h - ringMiddle) < 0.75D) {
				double arc = wrap(Math.atan2(dz, dx) - this.entranceAngle) * ringMiddle;
				if (Math.floorMod((int) Math.floor(arc), 10) == 0) return CIRCUIT;
			}

			if (y == this.floorY + CORRIDOR_HEIGHT + 1 && Math.abs(t) < 0.5D && s >= RING_OUTER && s < RADIUS - 1
					&& Math.floorMod((int) Math.floor(s), 4) == 0) return CIRCUIT;

			return HULL;
		}

		private boolean isSeam(double r, double dx, double dz, double dy) {
			double latitude = Math.asin(Math.max(-1.0D, Math.min(1.0D, dy / r)));
			double latArc = latitude * RADIUS;
			if (Math.abs(latArc - Math.round(latArc / 12.0D) * 12.0D) < 0.5D) return true;
			if (Math.abs(latitude) > 1.15D) return false;
			double lonArc = Math.atan2(dz, dx) * RADIUS;
			double gap = Math.abs(lonArc - Math.round(lonArc / 16.0D) * 16.0D) * Math.cos(latitude);
			return gap < 0.5D;
		}

		private byte classifyAntennas(int x, int y, int z) {
			for (Antenna antenna : this.antennas) {
				if (Math.abs(x - antenna.x()) > 3 || Math.abs(z - antenna.z()) > 3) continue;
				if (y < antenna.baseY() || y > antenna.baseY() + antenna.height()) continue;
				double progress = (y - antenna.baseY()) / (double) antenna.height();
				double radius = 2.2D * (1.0D - progress) + 0.45D;
				double ax = x - antenna.x();
				double az = z - antenna.z();
				if (ax * ax + az * az > radius * radius) continue;
				return y >= antenna.baseY() + antenna.height() - 1 ? CIRCUIT : HULL;
			}
			return NONE;
		}

		private byte classifyJoints(double px, double py, double pz) {
			for (Joint joint : this.joints) {
				double jx = px - joint.x();
				double jy = py - joint.y();
				double jz = pz - joint.z();
				double distance = jx * jx + jy * jy + jz * jz;
				if (distance > joint.radius() * joint.radius()) continue;
				return Math.abs(jy) < 0.5D ? CIRCUIT : HULL;
			}
			return NONE;
		}

		private byte classifyFeet(double px, int y, double pz) {
			for (Foot foot : this.feet) {
				if (y < foot.groundY() - 2 || y > foot.groundY() + 1) continue;
				double fx = px - foot.x();
				double fz = pz - foot.z();
				double distance = Math.sqrt(fx * fx + fz * fz);
				if (distance > FOOT_RADIUS) continue;
				return y == foot.groundY() + 1 && distance > FOOT_RADIUS - 0.9D ? CIRCUIT : HULL;
			}
			return NONE;
		}

		private byte classifyCables(double px, double py, double pz, List<Cable> nearby) {
			for (Cable cable : nearby) {
				if (px < cable.minX() || px > cable.maxX() + 1 || pz < cable.minZ() || pz > cable.maxZ() + 1
						|| py < cable.minY() || py > cable.maxY() + 1) continue;
				double[] xs = cable.xs(), ys = cable.ys(), zs = cable.zs();
				for (int i = 1; i < xs.length; i++) {
					double ax = xs[i - 1], ay = ys[i - 1], az = zs[i - 1];
					double bx = xs[i] - ax, by = ys[i] - ay, bz = zs[i] - az;
					double lengthSqr = bx * bx + by * by + bz * bz;
					double k = lengthSqr < 1.0E-6D ? 0.0D : ((px - ax) * bx + (py - ay) * by + (pz - az) * bz) / lengthSqr;
					k = Math.max(0.0D, Math.min(1.0D, k));
					double cx = ax + bx * k - px, cy = ay + by * k - py, cz = az + bz * k - pz;
					double radius = cable.radii()[i - 1] + (cable.radii()[i] - cable.radii()[i - 1]) * k;
					if (cx * cx + cy * cy + cz * cz > radius * radius) continue;
					double along = cable.lengths()[i - 1] + (cable.lengths()[i] - cable.lengths()[i - 1]) * k;
					return Math.floorMod((int) Math.floor(along), 9) == 0 ? CIRCUIT : HULL;
				}
			}
			return NONE;
		}

		private int rampFloor(double s) {
			double start = RADIUS - 3;
			if (s <= start) return this.floorY;
			double progress = Math.min(1.0D, (s - start) / (RAMP_LENGTH + 3.0D));
			return (int) Math.round(this.floorY + (this.rampEndY - this.floorY) * progress);
		}

		private static double wrap(double angle) {
			double result = angle % (Math.PI * 2.0D);
			if (result > Math.PI) result -= Math.PI * 2.0D;
			if (result < -Math.PI) result += Math.PI * 2.0D;
			return result;
		}

		public double drainRadius(double angle) {
			double radius = DRAIN_BASE;
			int[] frequencies = {3, 5, 7};
			for (int i = 0; i < frequencies.length; i++) radius += this.drainAmp[i] * Math.sin(frequencies[i] * angle + this.drainPhase[i]);
			return radius;
		}

		private double maxDrainRadius() {
			double radius = DRAIN_BASE;
			for (double amplitude : this.drainAmp) radius += amplitude;
			return radius;
		}

		public double horizontalDistance(int x, int z) {
			double dx = x + 0.5D - this.sphereX;
			double dz = z + 0.5D - this.sphereZ;
			return Math.sqrt(dx * dx + dz * dz);
		}

		public double angleOf(int x, int z) {
			return Math.atan2(z + 0.5D - this.sphereZ, x + 0.5D - this.sphereX);
		}

		public BlockPos chamberCenter() {
			return new BlockPos(this.centerX, this.floorY + 1, this.centerZ);
		}

		public BlockPos entrancePosition() {
			double distance = (RING_INNER + RING_OUTER) * 0.5D;
			return new BlockPos((int) Math.floor(this.sphereX + this.entranceX * distance), this.floorY,
					(int) Math.floor(this.sphereZ + this.entranceZ * distance));
		}

		public BlockPos corePosition() {
			double angle = this.entranceAngle + Math.PI;
			return new BlockPos((int) Math.floor(this.sphereX + Math.cos(angle) * CORE_DISTANCE), this.floorY,
					(int) Math.floor(this.sphereZ + Math.sin(angle) * CORE_DISTANCE));
		}

		public List<BlockPos> capsulePositions() {
			List<BlockPos> result = new ArrayList<>();
			for (double offset : CHAMBER_CAPSULE_ANGLES) {
				double angle = this.entranceAngle + offset;
				result.add(new BlockPos((int) Math.floor(this.sphereX + Math.cos(angle) * (CHAMBER_RADIUS + 1)), this.floorY + 1,
						(int) Math.floor(this.sphereZ + Math.sin(angle) * (CHAMBER_RADIUS + 1))));
			}
			return result;
		}

		public Map<Long, Integer> groundSamples() {
			return this.samples;
		}

		public long seed() {
			return this.seed;
		}

		public int centerX() {
			return this.centerX;
		}

		public int centerZ() {
			return this.centerZ;
		}

		public int baseY() {
			return this.baseY;
		}

		public int floorY() {
			return this.floorY;
		}

		public double entranceAngle() {
			return this.entranceAngle;
		}

		public int seaLevel() {
			return this.seaLevel;
		}

		public int minBuildY() {
			return this.minBuildY;
		}

		public int maxBuildY() {
			return this.maxBuildY;
		}

		public int minX() {
			return this.minX;
		}

		public int minY() {
			return this.minY;
		}

		public int minZ() {
			return this.minZ;
		}

		public int maxX() {
			return this.maxX;
		}

		public int maxY() {
			return this.maxY;
		}

		public int maxZ() {
			return this.maxZ;
		}
	}

	public static class SaiyanCraterStructure extends Structure {
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

	public static class NamekRuinsStructure extends Structure {
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
}
