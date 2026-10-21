package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.sagas.SagaMoviesEntity;
import com.dragonminez.common.init.entities.worldboss.AllWorldBossesEntity;
import com.dragonminez.server.world.data.StructurePlanSavedData;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.dragonminez.server.world.structure.BossStructures.GeteStarShape;
import com.dragonminez.server.world.structure.helper.DMZStructureSets;
import com.dragonminez.server.world.structure.placement.StructureSpawnPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GeteSpawnEvents {

	private static final List<ResourceKey<Structure>> VILLAGES = List.of(
			ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "village_ajissa")),
			ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "village_sacred")));

	private static final int SCAN_INTERVAL_TICKS = 400;
	private static final int SCAN_RADIUS_CHUNKS = 6;
	private static final float WAVE_CHANCE = 0.25F;
	private static final int MAX_ROBOTS_PER_VILLAGE = 3;
	private static final int MIN_WAVE_SIZE = 1;
	private static final int MAX_WAVE_SIZE = 2;
	private static final int MIN_OUTSKIRT_DISTANCE = 8;
	private static final int MAX_OUTSKIRT_DISTANCE = 20;
	private static final int MAX_VILLAGE_RADIUS = 80;
	private static final double ROBOT_SEARCH_MARGIN = 48.0D;
	private static final double MIN_PLAYER_DISTANCE = 16.0D;
	private static final float WAVE_SPREAD = 0.6F;
	private static final int PLACEMENT_ATTEMPTS = 8;

	private static final int PATROL_INTERVAL_TICKS = 600;
	private static final float PATROL_CHANCE = 0.5F;
	private static final double PATROL_PLAYER_RANGE = 200.0D;
	private static final double PATROL_SEARCH_RADIUS = 150.0D;
	private static final int MAX_PATROLS = 2;
	private static final int PATROL_MIN_DISTANCE = 60;
	private static final int PATROL_MAX_DISTANCE = 100;
	private static final double PATROL_PLAYER_GAP = 24.0D;
	private static final int MIN_PATROL_ROBOTS = 2;
	private static final int MAX_PATROL_ROBOTS = 3;

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
		if (!level.dimension().equals(NamekDimension.NAMEK_KEY) || level.players().isEmpty()) return;
		if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;

		long time = level.getGameTime();
		if (time % SCAN_INTERVAL_TICKS == 0L) scanVillages(level);
		if (time % PATROL_INTERVAL_TICKS == 0L) tickStarPatrols(level);
	}

	private static void scanVillages(ServerLevel level) {
		Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
		List<Structure> villages = new ArrayList<>();
		for (ResourceKey<Structure> key : VILLAGES) {
			Structure structure = registry.get(key);
			if (structure != null) villages.add(structure);
		}
		if (villages.isEmpty()) return;

		Set<Long> visited = new HashSet<>();
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			ChunkPos origin = player.chunkPosition();

			for (int dx = -SCAN_RADIUS_CHUNKS; dx <= SCAN_RADIUS_CHUNKS; dx++) {
				for (int dz = -SCAN_RADIUS_CHUNKS; dz <= SCAN_RADIUS_CHUNKS; dz++) {
					ChunkPos chunkPos = new ChunkPos(origin.x + dx, origin.z + dz);
					if (!visited.add(chunkPos.toLong())) continue;

					LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
					if (chunk == null) continue;

					for (Structure village : villages) {
						StructureStart start = chunk.getStartForStructure(village);
						if (start == null || !start.isValid()) continue;
						trySpawnWave(level, start.getBoundingBox());
						break;
					}
				}
			}
		}
	}

	private static void trySpawnWave(ServerLevel level, BoundingBox village) {
		RandomSource random = level.getRandom();
		if (random.nextFloat() >= WAVE_CHANCE) return;

		BlockPos center = village.getCenter();
		if (!level.areEntitiesLoaded(ChunkPos.asLong(center))) return;

		AABB area = AABB.of(village).inflate(ROBOT_SEARCH_MARGIN);
		int alive = level.getEntitiesOfClass(SagaMoviesEntity.GeteRobotEntity.class, area, Entity::isAlive).size();
		int room = MAX_ROBOTS_PER_VILLAGE - alive;
		if (room <= 0) return;

		BlockPos home = surface(level, center.getX(), center.getZ());
		int radius = Math.min(MAX_VILLAGE_RADIUS, Math.max(village.getXSpan(), village.getZSpan()) / 2);
		int size = Math.min(room, MIN_WAVE_SIZE + random.nextInt(MAX_WAVE_SIZE - MIN_WAVE_SIZE + 1));
		float angle = random.nextFloat() * Mth.TWO_PI;

		for (int i = 0; i < size; i++) {
			BlockPos spot = findSpot(level, center, radius + MIN_OUTSKIRT_DISTANCE, radius + MAX_OUTSKIRT_DISTANCE, angle, MIN_PLAYER_DISTANCE, random);
			if (spot != null) spawnRobot(level, spot, home);
		}
	}

	private static void tickStarPatrols(ServerLevel level) {
		ChunkPos origin = StructureSpawnPlanner.publishedPositions(level).get(DMZStructureSets.GETE_STAR_SALT);
		if (origin == null || !StructurePlanSavedData.get(level).isBuilt(DMZStructureSets.GETE_STAR_SALT)) return;
		BlockPos star = new BlockPos(origin.getMiddleBlockX(), 0, origin.getMiddleBlockZ());

		boolean watched = false;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			double dx = player.getX() - star.getX();
			double dz = player.getZ() - star.getZ();
			double distance = Math.sqrt(dx * dx + dz * dz);
			if (distance <= PATROL_PLAYER_RANGE && distance > GeteStarShape.RADIUS + 4) {
				watched = true;
				break;
			}
		}
		if (!watched) return;

		RandomSource random = level.getRandom();
		if (random.nextFloat() >= PATROL_CHANCE) return;

		AABB area = new AABB(star).inflate(PATROL_SEARCH_RADIUS, 512.0D, PATROL_SEARCH_RADIUS);
		int patrols = level.getEntitiesOfClass(AllWorldBossesEntity.MetalCooler.class, area,
				scout -> scout.isAlive() && scout.isScout()).size();
		if (patrols >= MAX_PATROLS) return;

		float angle = random.nextFloat() * Mth.TWO_PI;
		BlockPos leaderSpot = findSpot(level, star, PATROL_MIN_DISTANCE, PATROL_MAX_DISTANCE, angle, PATROL_PLAYER_GAP, random);
		if (leaderSpot == null) return;

		AllWorldBossesEntity.MetalCooler scout = MainEntities.WORLDBOSS_METAL_COOLER.get().create(level);
		if (scout == null) return;
		scout.makeScout();
		if (!place(level, scout, leaderSpot)) return;

		BlockPos home = surface(level, star.getX(), star.getZ());
		int robots = MIN_PATROL_ROBOTS + random.nextInt(MAX_PATROL_ROBOTS - MIN_PATROL_ROBOTS + 1);
		for (int i = 0; i < robots; i++) {
			BlockPos spot = findSpot(level, star, PATROL_MIN_DISTANCE, PATROL_MAX_DISTANCE, angle, PATROL_PLAYER_GAP, random);
			if (spot != null) spawnRobot(level, spot, home);
		}
	}

	private static BlockPos findSpot(ServerLevel level, BlockPos center, int minDistance, int maxDistance, float angle,
									 double playerGap, RandomSource random) {
		for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
			float a = angle + (random.nextFloat() - 0.5F) * WAVE_SPREAD;
			int distance = minDistance + random.nextInt(Math.max(1, maxDistance - minDistance + 1));
			int x = center.getX() + Mth.floor(Mth.cos(a) * distance);
			int z = center.getZ() + Mth.floor(Mth.sin(a) * distance);

			if (!level.isPositionEntityTicking(new BlockPos(x, center.getY(), z))) continue;

			BlockPos pos = surface(level, x, z);
			BlockState ground = level.getBlockState(pos.below());
			if (ground.is(BlockTags.LOGS) || ground.is(BlockTags.LEAVES)) continue;
			if (!ground.isFaceSturdy(level, pos.below(), Direction.UP)) continue;
			if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty()) continue;
			if (!level.noCollision(MainEntities.SAGA_GETE_ROBOT.get().getAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D))) continue;
			if (level.getNearestPlayer(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, playerGap, false) != null) continue;
			return pos;
		}
		return null;
	}

	private static void spawnRobot(ServerLevel level, BlockPos pos, BlockPos home) {
		SagaMoviesEntity.GeteRobotEntity robot = MainEntities.SAGA_GETE_ROBOT.get().create(level);
		if (robot == null) return;
		robot.getPersistentData().putLong(SagaMoviesEntity.GeteRobotEntity.VILLAGE_TAG, home.asLong());
		place(level, robot, pos);
	}

	private static boolean place(ServerLevel level, Mob mob, BlockPos pos) {
		mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
		return level.addFreshEntity(mob);
	}

	private static BlockPos surface(ServerLevel level, int x, int z) {
		return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
	}
}
