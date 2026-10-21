package com.dragonminez.server.world.structure;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.block.custom.DemonRealmBlocks;
import com.dragonminez.common.init.entities.demon.DemonVillagersEntity;
import com.dragonminez.server.world.gen.DemonRealmGeneration;
import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DemonVillageStructure extends Structure {
	public static final Codec<DemonVillageStructure> CODEC = simpleCodec(DemonVillageStructure::new);

	private static final ResourceLocation CENTER = template("demon_village/center");
	private static final List<ResourceLocation> HOUSES = List.of(
			template("demon_village/house_big"),
			template("demon_village/house_small"));
	private static final String ENTRANCE = Reference.MOD_ID + ":building_entrance";
	private static final String VILLAGER_MARKER = Reference.MOD_ID + ":villager";

	private static final int LAND_PROBE = DemonRealmGeneration.THIRD_DARK_SEA + 16;
	private static final int MIN_GROUND = DemonRealmGeneration.THIRD_BASE - 16;
	private static final int CEILING_SCAN = 40;

	private static final int PLAZA_HALF = 6;
	private static final int PLAZA_SLOPE = 3;
	private static final int PLAZA_CLEARANCE = 10;

	private static final int MIN_HOUSES = 3;
	private static final int MAX_HOUSES = 5;
	private static final int MIN_PLACED_HOUSES = 2;
	private static final int MIN_BUILDINGS = 3;
	private static final int[] DOOR_RADII = {14, 17, 20, 23, 26};
	private static final double ANGLE_JITTER = 0.25D;
	private static final int HOUSE_RISE = 3;
	private static final int HOUSE_DROP = 5;
	private static final int HOUSE_HEADROOM = 2;
	private static final int APPROACH = 3;

	private static final int MIN_FARMS = 1;
	private static final int MAX_FARMS = 2;
	private static final int[] FARM_RADII = {19, 23, 27};
	private static final int FARM_SLOPE = 2;
	private static final int FARM_CLEARANCE = 6;

	private static final int MARGIN = 2;
	private static final int NODE_SPACING = 3;
	private static final int PATH_STEP = 4;
	private static final int PATH_CLEARANCE = 3;
	private static final int LAMP_EVERY = 3;
	private static final int LAMP_OFFSET = 2;

	private static final int PATROLLER_RADIUS = 4;
	private static final int VILLAGE_RADIUS = 56;
	private static final float SECOND_VILLAGER_CHANCE = 0.5F;

	public DemonVillageStructure(StructureSettings settings) {
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
		Terrain terrain = new Terrain(context.chunkGenerator(), context.heightAccessor(), context.randomState());

		int ground = terrain.exactGround(centerX, centerZ);
		if (ground < MIN_GROUND) return Optional.empty();
		Rect plaza = new Rect(centerX - PLAZA_HALF, centerZ - PLAZA_HALF, centerX + PLAZA_HALF, centerZ + PLAZA_HALF);
		if (!terrain.flat(plaza, ground - PLAZA_SLOPE, ground + PLAZA_SLOPE, ground + PLAZA_CLEARANCE)) return Optional.empty();

		RandomSource random = context.random();
		StructureTemplateManager templates = context.structureTemplateManager();
		Layout layout = new Layout(centerX, ground, centerZ, plaza, terrain, templates);

		int houses = MIN_HOUSES + random.nextInt(MAX_HOUSES - MIN_HOUSES + 1);
		double baseAngle = random.nextDouble() * Math.PI * 2.0D;
		for (int i = 0; i < houses; i++) {
			layout.tryHouse(baseAngle + Math.PI * 2.0D * i / houses, random);
		}
		if (layout.buildings.size() < MIN_PLACED_HOUSES) return Optional.empty();

		int farms = MIN_FARMS + random.nextInt(MAX_FARMS - MIN_FARMS + 1);
		int firstGap = random.nextInt(houses);
		for (int i = 0; i < houses && layout.farms.size() < farms; i++) {
			int gap = (firstGap + i) % houses;
			layout.tryFarm(baseAngle + Math.PI * 2.0D * (gap + 0.5D) / houses, random);
		}
		if (layout.buildings.size() + layout.farms.size() < MIN_BUILDINGS) return Optional.empty();

		layout.placeLamps();
		BuildingPiece center = new BuildingPiece(templates, CENTER, new BlockPos(plaza.minX(), ground, plaza.minZ()), Rotation.getRandom(random));
		GroundPiece groundPiece = layout.groundPiece(context.seed() ^ (chunkPos.toLong() * 0x9E3779B97F4A7C15L));
		List<BuildingPiece> buildings = List.copyOf(layout.buildings);

		return Optional.of(new GenerationStub(new BlockPos(centerX, ground, centerZ), builder -> {
			builder.addPiece(groundPiece);
			builder.addPiece(center);
			buildings.forEach(builder::addPiece);
		}));
	}

	@Override
	public StructureType<?> type() {
		return MainStructureTypes.DEMON_VILLAGE.get();
	}

	@Override
	public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
						   BoundingBox box, ChunkPos chunkPos, PiecesContainer pieces) {
		GroundPiece ground = null;
		for (StructurePiece piece : pieces.pieces()) {
			if (piece instanceof GroundPiece groundPiece) ground = groundPiece;
		}
		if (ground == null) return;
		long seed = ground.seed;
		BlockPos home = new BlockPos(ground.centerX, ground.centerY + 1, ground.centerZ);

		for (StructurePiece piece : pieces.pieces()) {
			if (!(piece instanceof BuildingPiece building)) continue;
			List<BlockPos> spots = building.villagerSpots();
			spots.sort(Comparator.comparingDouble(pos -> noise(seed, pos.getX(), pos.getY(), pos.getZ())));
			boolean plaza = building.isCenter();
			for (int i = 0; i < spots.size() && i < 2; i++) {
				BlockPos spot = spots.get(i);
				if (!GroundPiece.inside(box, spot.getX(), spot.getZ())) continue;
				if (plaza && i == 0) {
					spawnResident(level, MainEntities.TIME_PATROLLER.get(), spot, spot.above(), PATROLLER_RADIUS);
				} else if (i == 0 || noise(seed, spot.getX(), spot.getY() + 1, spot.getZ()) < SECOND_VILLAGER_CHANCE) {
					spawnResident(level, MainEntities.DEMON_VILLAGER.get(), spot, home, VILLAGE_RADIUS);
				}
			}
		}
	}

	@Nullable
	public static BlockPos plaza(StructureStart start) {
		for (StructurePiece piece : start.getPieces()) {
			if (piece instanceof GroundPiece ground) return new BlockPos(ground.centerX, ground.centerY, ground.centerZ);
		}
		return null;
	}

	private static void spawnResident(WorldGenLevel level, EntityType<? extends DemonVillagersEntity.Resident> type,
									  BlockPos floor, BlockPos home, int radius) {
		DemonVillagersEntity.Resident resident = type.create(level.getLevel());
		if (resident == null) return;
		BlockPos feet = floor.above();
		resident.moveTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
		resident.finalizeSpawn(level, level.getCurrentDifficultyAt(feet), MobSpawnType.STRUCTURE, null, null);
		resident.setHome(home, radius);
		level.addFreshEntityWithPassengers(resident);
	}

	private static Door door(StructureTemplateManager templates, ResourceLocation location) {
		StructureTemplate template = templates.getOrCreate(location);
		for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW)) {
			CompoundTag nbt = info.nbt();
			if (nbt != null && ENTRANCE.equals(nbt.getString("name"))) {
				return new Door(info.pos(), JigsawBlock.getFrontFacing(info.state()));
			}
		}
		return null;
	}

	private static Rotation facingRotation(Direction facing, int towardX, int towardZ) {
		Rotation best = Rotation.NONE;
		long bestDot = Long.MIN_VALUE;
		for (Rotation rotation : Rotation.values()) {
			Direction rotated = rotation.rotate(facing);
			long dot = (long) rotated.getStepX() * towardX + (long) rotated.getStepZ() * towardZ;
			if (dot > bestDot) {
				bestDot = dot;
				best = rotation;
			}
		}
		return best;
	}

	private static float noise(long seed, int x, int y, int z) {
		long hash = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
		hash ^= hash >>> 33;
		hash *= 0xFF51AFD7ED558CCDL;
		hash ^= hash >>> 33;
		return (hash >>> 40) / (float) (1 << 24);
	}

	private static boolean open(BlockState state) {
		return state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty());
	}

	private record Door(BlockPos pos, Direction facing) {}

	private record Rect(int minX, int minZ, int maxX, int maxZ) {
		static Rect of(BoundingBox box) {
			return new Rect(box.minX(), box.minZ(), box.maxX(), box.maxZ());
		}

		Rect inflate(int amount) {
			return new Rect(this.minX - amount, this.minZ - amount, this.maxX + amount, this.maxZ + amount);
		}

		boolean intersects(Rect other) {
			return this.minX <= other.maxX && this.maxX >= other.minX && this.minZ <= other.maxZ && this.maxZ >= other.minZ;
		}

		boolean contains(int x, int z) {
			return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
		}
	}

	private static final class Terrain {
		private static final int[] NO_LAND = new int[0];

		private final ChunkGenerator generator;
		private final LevelHeightAccessor heightAccessor;
		private final RandomState randomState;
		private final DensityFunction density;
		private final int top;
		private final Map<Long, int[]> columns = new HashMap<>();

		Terrain(ChunkGenerator generator, LevelHeightAccessor heightAccessor, RandomState randomState) {
			this.generator = generator;
			this.heightAccessor = heightAccessor;
			this.randomState = randomState;
			this.density = randomState.router().finalDensity();
			this.top = heightAccessor.getMaxBuildHeight() - 1;
		}

		int exactGround(int x, int z) {
			NoiseColumn column = this.generator.getBaseColumn(x, z, this.heightAccessor, this.randomState);
			if (column.getBlock(LAND_PROBE).isAir()) return Integer.MIN_VALUE;
			int y = LAND_PROBE;
			while (y < this.top && !column.getBlock(y + 1).isAir()) y++;
			return y;
		}

		int[] column(int x, int z) {
			return this.columns.computeIfAbsent(ChunkPos.asLong(x, z), key -> this.scan(x, z));
		}

		boolean flat(Rect rect, int low, int high, int minCeiling) {
			int stepsX = Math.max(2, (rect.maxX() - rect.minX() + 6) / 7);
			int stepsZ = Math.max(2, (rect.maxZ() - rect.minZ() + 6) / 7);
			for (int i = 0; i <= stepsX; i++) {
				int x = rect.minX() + (rect.maxX() - rect.minX()) * i / stepsX;
				for (int j = 0; j <= stepsZ; j++) {
					int z = rect.minZ() + (rect.maxZ() - rect.minZ()) * j / stepsZ;
					int[] column = this.column(x, z);
					if (column == NO_LAND || column[0] < low || column[0] > high || column[1] <= minCeiling) return false;
				}
			}
			return true;
		}

		private boolean solid(int x, int y, int z) {
			return this.density.compute(new DensityFunction.SinglePointContext(x, y, z)) > 0.0D;
		}

		private int[] scan(int x, int z) {
			if (!this.solid(x, LAND_PROBE, z)) return NO_LAND;
			int y = LAND_PROBE;
			while (y + 2 <= this.top && this.solid(x, y + 2, z)) y += 2;
			if (y + 1 <= this.top && this.solid(x, y + 1, z)) y++;
			if (y < MIN_GROUND) return NO_LAND;
			int ceiling = this.top + 1;
			for (int c = y + 2; c <= Math.min(this.top, y + CEILING_SCAN); c += 2) {
				if (this.solid(x, c, z)) {
					ceiling = this.solid(x, c - 1, z) ? c - 1 : c;
					break;
				}
			}
			return new int[]{y, ceiling};
		}
	}

	private static final class Layout {
		private final int centerX;
		private final int ground;
		private final int centerZ;
		private final Rect plaza;
		private final Terrain terrain;
		private final StructureTemplateManager templates;
		private final List<Rect> occupied = new ArrayList<>();
		private final List<int[]> paths = new ArrayList<>();
		private final Set<Long> pathColumns = new HashSet<>();
		private final List<BuildingPiece> buildings = new ArrayList<>();
		private final List<int[]> farms = new ArrayList<>();
		private final List<Integer> lamps = new ArrayList<>();

		Layout(int centerX, int ground, int centerZ, Rect plaza, Terrain terrain, StructureTemplateManager templates) {
			this.centerX = centerX;
			this.ground = ground;
			this.centerZ = centerZ;
			this.plaza = plaza;
			this.terrain = terrain;
			this.templates = templates;
		}

		void tryHouse(double angle, RandomSource random) {
			for (int radius : DOOR_RADII) {
				ResourceLocation location = HOUSES.get(random.nextInt(HOUSES.size()));
				Door door = door(this.templates, location);
				if (door == null) continue;

				double a = angle + (random.nextDouble() - 0.5D) * ANGLE_JITTER;
				int doorX = this.centerX + (int) Math.round(Math.cos(a) * radius);
				int doorZ = this.centerZ + (int) Math.round(Math.sin(a) * radius);
				Rotation rotation = facingRotation(door.facing(), this.centerX - doorX, this.centerZ - doorZ);
				Direction front = rotation.rotate(door.facing());

				StructureTemplate template = this.templates.getOrCreate(location);
				BlockPos pivot = BuildingPiece.pivot(this.templates, location);
				BlockPos outside = StructureTemplate.transform(door.pos().relative(door.facing()), Mirror.NONE, rotation, pivot);
				BlockPos corner = new BlockPos(doorX - outside.getX(), 0, doorZ - outside.getZ());
				Rect rect = Rect.of(template.getBoundingBox(BuildingPiece.settings(rotation, pivot), corner));
				if (this.blocked(rect)) continue;

				int[] doorColumn = this.terrain.column(doorX, doorZ);
				if (doorColumn.length == 0) continue;
				int roof = doorColumn[0] - door.pos().getY() + template.getSize().getY() - 1;
				if (!this.terrain.flat(rect.inflate(-1), doorColumn[0] - HOUSE_DROP, doorColumn[0] + HOUSE_RISE, roof + HOUSE_HEADROOM)) continue;
				int doorGround = this.terrain.exactGround(doorX, doorZ);
				if (doorGround < MIN_GROUND) continue;

				int approachX = doorX + front.getStepX() * APPROACH;
				int approachZ = doorZ + front.getStepZ() * APPROACH;
				int[] path = this.path(rect, new int[]{approachX, approachZ, doorX, doorZ}, doorGround);
				if (path == null) continue;

				this.occupied.add(rect);
				this.addPath(path);
				this.buildings.add(new BuildingPiece(this.templates, location, corner.above(doorGround - door.pos().getY()), rotation));
				return;
			}
		}

		void tryFarm(double angle, RandomSource random) {
			for (int radius : FARM_RADII) {
				boolean longX = random.nextBoolean();
				int halfX = longX ? 5 : 4;
				int halfZ = longX ? 4 : 5;
				double a = angle + (random.nextDouble() - 0.5D) * ANGLE_JITTER;
				int farmX = this.centerX + (int) Math.round(Math.cos(a) * radius);
				int farmZ = this.centerZ + (int) Math.round(Math.sin(a) * radius);
				Rect rect = new Rect(farmX - halfX, farmZ - halfZ, farmX + halfX, farmZ + halfZ);
				if (this.blocked(rect)) continue;

				int[] farmColumn = this.terrain.column(farmX, farmZ);
				if (farmColumn.length == 0) continue;
				if (!this.terrain.flat(rect, farmColumn[0] - FARM_SLOPE, farmColumn[0] + FARM_SLOPE, farmColumn[0] + FARM_CLEARANCE)) continue;
				int farmGround = this.terrain.exactGround(farmX, farmZ);
				if (farmGround < MIN_GROUND) continue;

				int towardX = this.centerX - farmX;
				int towardZ = this.centerZ - farmZ;
				int entranceX = farmX;
				int entranceZ = farmZ;
				if (Math.abs(towardX) >= Math.abs(towardZ)) entranceX = towardX > 0 ? rect.maxX() + 1 : rect.minX() - 1;
				else entranceZ = towardZ > 0 ? rect.maxZ() + 1 : rect.minZ() - 1;
				int[] path = this.path(rect, new int[]{entranceX, entranceZ}, farmGround);
				if (path == null) continue;

				this.occupied.add(rect);
				this.addPath(path);
				this.farms.add(new int[]{rect.minX(), rect.minZ(), rect.maxX(), rect.maxZ(), farmGround, random.nextInt(4), longX ? 1 : 0});
				return;
			}
		}

		private boolean blocked(Rect rect) {
			Rect padded = rect.inflate(MARGIN);
			if (padded.intersects(this.plaza)) return true;
			for (Rect other : this.occupied) {
				if (padded.intersects(other)) return true;
			}
			for (long column : this.pathColumns) {
				if (padded.contains(ChunkPos.getX(column), ChunkPos.getZ(column))) return true;
			}
			return false;
		}

		private int[] path(Rect own, int[] waypoints, int endGround) {
			int firstX = waypoints[0];
			int firstZ = waypoints[1];
			double dirX = firstX - this.centerX;
			double dirZ = firstZ - this.centerZ;
			double reach = (PLAZA_HALF + 1) / Math.max(Math.max(Math.abs(dirX), Math.abs(dirZ)), 1.0E-6D);
			int startX = this.centerX + (int) Math.round(dirX * reach);
			int startZ = this.centerZ + (int) Math.round(dirZ * reach);

			List<int[]> points = new ArrayList<>();
			points.add(new int[]{startX, startZ});
			for (int i = 0; i + 1 < waypoints.length; i += 2) points.add(new int[]{waypoints[i], waypoints[i + 1]});

			List<Integer> nodes = new ArrayList<>();
			int previous = this.ground;
			for (int p = 0; p + 1 < points.size(); p++) {
				int[] from = points.get(p);
				int[] to = points.get(p + 1);
				double length = Math.hypot(to[0] - from[0], to[1] - from[1]);
				int steps = Math.max(1, (int) Math.ceil(length / NODE_SPACING));
				for (int s = p == 0 ? 0 : 1; s <= steps; s++) {
					int x = (int) Math.round(from[0] + (to[0] - from[0]) * (double) s / steps);
					int z = (int) Math.round(from[1] + (to[1] - from[1]) * (double) s / steps);
					int[] column = this.terrain.column(x, z);
					if (column.length == 0 || column[1] - column[0] - 1 < PATH_CLEARANCE) return null;
					if (Math.abs(column[0] - previous) > PATH_STEP) return null;
					previous = column[0];
					nodes.add(x);
					nodes.add(z);
					nodes.add(column[0]);
				}
			}
			if (Math.abs(previous - endGround) > PATH_STEP) return null;

			int[] path = nodes.stream().mapToInt(Integer::intValue).toArray();
			for (long column : rasterize(path)) {
				int x = ChunkPos.getX(column);
				int z = ChunkPos.getZ(column);
				if (own.contains(x, z)) return null;
				for (Rect other : this.occupied) {
					if (other.inflate(1).contains(x, z)) return null;
				}
			}
			return path;
		}

		private void addPath(int[] path) {
			this.paths.add(path);
			this.pathColumns.addAll(rasterize(path));
		}

		void placeLamps() {
			for (int[] path : this.paths) {
				int nodes = path.length / 3;
				int side = 1;
				for (int n = LAMP_EVERY; n < nodes - 1; n += LAMP_EVERY) {
					int x0 = path[(n - 1) * 3];
					int z0 = path[(n - 1) * 3 + 1];
					int x1 = path[(n + 1) * 3];
					int z1 = path[(n + 1) * 3 + 1];
					double length = Math.max(1.0E-6D, Math.hypot(x1 - x0, z1 - z0));
					int x = path[n * 3] + (int) Math.round(-(z1 - z0) / length * LAMP_OFFSET * side);
					int z = path[n * 3 + 1] + (int) Math.round((x1 - x0) / length * LAMP_OFFSET * side);
					side = -side;
					if (this.pathColumns.contains(ChunkPos.asLong(x, z)) || this.plaza.inflate(1).contains(x, z)) continue;
					boolean clear = true;
					for (Rect other : this.occupied) {
						if (other.inflate(1).contains(x, z)) {
							clear = false;
							break;
						}
					}
					if (!clear) continue;
					this.lamps.add(x);
					this.lamps.add(z);
					this.lamps.add(path[n * 3 + 2]);
				}
			}
		}

		GroundPiece groundPiece(long seed) {
			int minX = this.plaza.minX();
			int minZ = this.plaza.minZ();
			int maxX = this.plaza.maxX();
			int maxZ = this.plaza.maxZ();
			int minY = this.ground;
			int maxY = this.ground;
			for (long column : this.pathColumns) {
				minX = Math.min(minX, ChunkPos.getX(column) - 1);
				maxX = Math.max(maxX, ChunkPos.getX(column) + 1);
				minZ = Math.min(minZ, ChunkPos.getZ(column) - 1);
				maxZ = Math.max(maxZ, ChunkPos.getZ(column) + 1);
			}
			for (int[] path : this.paths) {
				for (int i = 2; i < path.length; i += 3) {
					minY = Math.min(minY, path[i]);
					maxY = Math.max(maxY, path[i]);
				}
			}
			for (int[] farm : this.farms) {
				minX = Math.min(minX, farm[0]);
				minZ = Math.min(minZ, farm[1]);
				maxX = Math.max(maxX, farm[2]);
				maxZ = Math.max(maxZ, farm[3]);
				minY = Math.min(minY, farm[4]);
				maxY = Math.max(maxY, farm[4]);
			}
			for (int i = 0; i + 2 < this.lamps.size(); i += 3) {
				minX = Math.min(minX, this.lamps.get(i));
				maxX = Math.max(maxX, this.lamps.get(i));
				minZ = Math.min(minZ, this.lamps.get(i + 1));
				maxZ = Math.max(maxZ, this.lamps.get(i + 1));
			}
			BoundingBox box = new BoundingBox(minX, minY - GroundPiece.FILL_DEPTH, minZ, maxX, maxY + GroundPiece.SCAN_UP + 4, maxZ);
			return new GroundPiece(box, this.centerX, this.ground, this.centerZ, this.paths, this.farms,
					this.lamps.stream().mapToInt(Integer::intValue).toArray(), seed);
		}
	}

	private static Set<Long> rasterize(int[] path) {
		Set<Long> columns = new HashSet<>();
		GroundPiece.walk(path, (x, z, hint, edge) -> columns.add(ChunkPos.asLong(x, z)));
		return columns;
	}

	public static class BuildingPiece extends TemplateStructurePiece {
		private final Rotation rotation;

		public BuildingPiece(StructureTemplateManager templates, ResourceLocation location, BlockPos position, Rotation rotation) {
			super(MainStructureTypes.DEMON_VILLAGE_BUILDING.get(), 0, templates, location, location.toString(),
					settings(rotation, pivot(templates, location)), position);
			this.rotation = rotation;
		}

		public BuildingPiece(StructureTemplateManager templates, CompoundTag tag) {
			super(MainStructureTypes.DEMON_VILLAGE_BUILDING.get(), tag, templates,
					location -> settings(Rotation.valueOf(tag.getString("Rot")), pivot(templates, location)));
			this.rotation = Rotation.valueOf(tag.getString("Rot"));
		}

		private static BlockPos pivot(StructureTemplateManager templates, ResourceLocation location) {
			Vec3i size = templates.getOrCreate(location).getSize();
			return new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
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
		}

		@Override
		protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		}

		boolean isCenter() {
			return CENTER.toString().equals(this.templateName);
		}

		List<BlockPos> villagerSpots() {
			List<BlockPos> spots = new ArrayList<>();
			for (StructureTemplate.StructureBlockInfo info : this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.JIGSAW)) {
				CompoundTag nbt = info.nbt();
				if (nbt != null && VILLAGER_MARKER.equals(nbt.getString("name"))) spots.add(info.pos());
			}
			return spots;
		}
	}

	public static class GroundPiece extends StructurePiece {
		static final int FILL_DEPTH = 12;
		static final int SCAN_UP = 6;
		private static final int SCAN_DOWN = 10;
		private static final int FARM_CLEAR = 5;
		private static final float EDGE_GAPS = 0.25F;
		private static final float EMPTY_FARMLAND = 0.1F;

		private final int centerX;
		private final int centerY;
		private final int centerZ;
		private final List<int[]> paths;
		private final List<int[]> farms;
		private final int[] lamps;
		private final long seed;

		public GroundPiece(BoundingBox box, int centerX, int centerY, int centerZ, List<int[]> paths, List<int[]> farms, int[] lamps, long seed) {
			super(MainStructureTypes.DEMON_VILLAGE_GROUND.get(), 0, box);
			this.centerX = centerX;
			this.centerY = centerY;
			this.centerZ = centerZ;
			this.paths = List.copyOf(paths);
			this.farms = List.copyOf(farms);
			this.lamps = lamps;
			this.seed = seed;
		}

		public GroundPiece(CompoundTag tag) {
			super(MainStructureTypes.DEMON_VILLAGE_GROUND.get(), tag);
			this.centerX = tag.getInt("CenterX");
			this.centerY = tag.getInt("CenterY");
			this.centerZ = tag.getInt("CenterZ");
			this.paths = readArrays(tag.getList("Paths", Tag.TAG_INT_ARRAY));
			this.farms = readArrays(tag.getList("Farms", Tag.TAG_INT_ARRAY));
			this.lamps = tag.getIntArray("Lamps");
			this.seed = tag.getLong("Seed");
		}

		private static List<int[]> readArrays(ListTag list) {
			List<int[]> arrays = new ArrayList<>(list.size());
			for (int i = 0; i < list.size(); i++) arrays.add(list.getIntArray(i));
			return arrays;
		}

		private static ListTag writeArrays(List<int[]> arrays) {
			ListTag list = new ListTag();
			for (int[] array : arrays) list.add(new IntArrayTag(array));
			return list;
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			tag.putInt("CenterX", this.centerX);
			tag.putInt("CenterY", this.centerY);
			tag.putInt("CenterZ", this.centerZ);
			tag.put("Paths", writeArrays(this.paths));
			tag.put("Farms", writeArrays(this.farms));
			tag.putIntArray("Lamps", this.lamps);
			tag.putLong("Seed", this.seed);
		}

		interface ColumnVisitor {
			void visit(int x, int z, int hint, boolean edge);
		}

		static void walk(int[] path, ColumnVisitor visitor) {
			Set<Long> seen = new HashSet<>();
			for (int i = 0; i + 5 < path.length; i += 3) {
				int x0 = path[i];
				int z0 = path[i + 1];
				int h0 = path[i + 2];
				int dx = path[i + 3] - x0;
				int dz = path[i + 4] - z0;
				int dh = path[i + 5] - h0;
				double length = Math.max(1.0E-6D, Math.hypot(dx, dz));
				double sideX = -dz / length;
				double sideZ = dx / length;
				int steps = Math.max(1, (int) Math.ceil(length * 2.0D));
				for (int s = 0; s <= steps; s++) {
					double t = (double) s / steps;
					double px = x0 + dx * t;
					double pz = z0 + dz * t;
					int hint = (int) Math.round(h0 + dh * t);
					for (int w = -2; w <= 2; w++) {
						int x = (int) Math.floor(px + sideX * w * 0.5D + 0.5D);
						int z = (int) Math.floor(pz + sideZ * w * 0.5D + 0.5D);
						if (seen.add(ChunkPos.asLong(x, z))) visitor.visit(x, z, hint, Math.abs(w) == 2);
					}
				}
			}
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
								RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			this.fillPlaza(level, box, pos);
			for (int[] farm : this.farms) this.buildFarm(level, box, pos, farm);

			BlockState bricks = DemonRealmBlocks.natural(MainBlocks.ADOBE_BRICKS.get());
			Set<Long> paved = new HashSet<>();
			for (int[] path : this.paths) {
				walk(path, (x, z, hint, edge) -> {
					if (!inside(box, x, z) || !paved.add(ChunkPos.asLong(x, z))) return;
					if (edge && noise(this.seed, x, 1, z) < EDGE_GAPS) return;
					int y = surface(level, pos, x, z, hint);
					if (y != Integer.MIN_VALUE) level.setBlock(pos.set(x, y, z), bricks, 2);
				});
			}

			for (int i = 0; i + 2 < this.lamps.length; i += 3) {
				this.placeLamp(level, box, pos, this.lamps[i], this.lamps[i + 1], this.lamps[i + 2]);
			}
		}

		private void fillPlaza(WorldGenLevel level, BoundingBox box, BlockPos.MutableBlockPos pos) {
			BlockState dirt = DemonRealmBlocks.natural(MainBlocks.MAKAI_DIRT.get());
			for (int x = this.centerX - PLAZA_HALF; x <= this.centerX + PLAZA_HALF; x++) {
				for (int z = this.centerZ - PLAZA_HALF; z <= this.centerZ + PLAZA_HALF; z++) {
					if (inside(box, x, z)) fillBelow(level, pos, x, this.centerY, z, dirt);
				}
			}
		}

		private void buildFarm(WorldGenLevel level, BoundingBox box, BlockPos.MutableBlockPos pos, int[] farm) {
			int minX = farm[0];
			int minZ = farm[1];
			int maxX = farm[2];
			int maxZ = farm[3];
			int y = farm[4];
			Block crop = switch (farm[5]) {
				case 0 -> Blocks.WHEAT;
				case 1 -> Blocks.CARROTS;
				case 2 -> Blocks.POTATOES;
				default -> Blocks.BEETROOTS;
			};
			boolean channelAlongX = farm[6] == 1;
			int midX = (minX + maxX) / 2;
			int midZ = (minZ + maxZ) / 2;
			BlockState dirt = DemonRealmBlocks.natural(MainBlocks.MAKAI_DIRT.get());
			BlockState bricks = DemonRealmBlocks.natural(MainBlocks.ADOBE_BRICKS.get());
			BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);

			for (int x = Math.max(box.minX(), minX); x <= Math.min(box.maxX(), maxX); x++) {
				for (int z = Math.max(box.minZ(), minZ); z <= Math.min(box.maxZ(), maxZ); z++) {
					fillBelow(level, pos, x, y, z, dirt);
					for (int dy = 1; dy <= FARM_CLEAR; dy++) level.setBlock(pos.set(x, y + dy, z), Blocks.AIR.defaultBlockState(), 2);

					boolean border = x == minX || x == maxX || z == minZ || z == maxZ;
					boolean channel = channelAlongX ? z == midZ : x == midX;
					if (border) {
						level.setBlock(pos.set(x, y, z), bricks, 2);
						if ((x == minX && z == minZ) || (x == maxX && z == maxZ)) {
							level.setBlock(pos.set(x, y + 1, z), MainBlocks.NAMEK_AJISSA_FENCE.get().defaultBlockState(), 2);
							level.setBlock(pos.set(x, y + 2, z), Blocks.LANTERN.defaultBlockState(), 2);
						}
					} else if (channel) {
						level.setBlock(pos.set(x, y, z), Blocks.WATER.defaultBlockState(), 2);
					} else {
						level.setBlock(pos.set(x, y, z), farmland, 2);
						if (noise(this.seed, x, y, z) < EMPTY_FARMLAND || !(crop instanceof CropBlock cropBlock)) continue;
						int age = Math.min(cropBlock.getMaxAge(), (int) (noise(this.seed, x, y + 1, z) * (cropBlock.getMaxAge() + 1)));
						level.setBlock(pos.set(x, y + 1, z), cropBlock.getStateForAge(age), 2);
					}
				}
			}
		}

		private void placeLamp(WorldGenLevel level, BoundingBox box, BlockPos.MutableBlockPos pos, int x, int z, int hint) {
			if (!inside(box, x, z)) return;
			int y = surface(level, pos, x, z, hint);
			if (y == Integer.MIN_VALUE) return;
			for (int dy = 1; dy <= 3; dy++) {
				if (!open(level.getBlockState(pos.set(x, y + dy, z)))) return;
			}
			BlockState fence = MainBlocks.NAMEK_AJISSA_FENCE.get().defaultBlockState();
			level.setBlock(pos.set(x, y + 1, z), fence, 2);
			level.setBlock(pos.set(x, y + 2, z), fence, 2);
			level.setBlock(pos.set(x, y + 3, z), Blocks.LANTERN.defaultBlockState(), 2);
		}

		private static void fillBelow(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z, BlockState fill) {
			for (int depth = 1; depth <= FILL_DEPTH; depth++) {
				if (!open(level.getBlockState(pos.set(x, y - depth, z)))) return;
				level.setBlock(pos, fill, 2);
			}
		}

		private static int surface(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int hint) {
			boolean openAbove = false;
			for (int y = hint + SCAN_UP; y >= hint - SCAN_DOWN; y--) {
				if (open(level.getBlockState(pos.set(x, y, z)))) {
					openAbove = true;
				} else if (openAbove) {
					return y;
				}
			}
			return Integer.MIN_VALUE;
		}

		private static boolean inside(BoundingBox box, int x, int z) {
			return x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ();
		}
	}
}
