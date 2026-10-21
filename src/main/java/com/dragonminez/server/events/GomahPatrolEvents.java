package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.sagas.SagaDaimaEntity;
import com.dragonminez.server.world.dimension.DemonRealmDimension;
import com.dragonminez.server.world.structure.DemonVillageStructure;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GomahPatrolEvents {

	private static final int SCAN_INTERVAL_TICKS = 400;
	private static final int SCAN_RADIUS_CHUNKS = 6;
	private static final float WAVE_CHANCE = 0.3F;
	private static final int MAX_SOLDIERS_PER_VILLAGE = 4;
	private static final int MIN_WAVE_SIZE = 1;
	private static final int MAX_WAVE_SIZE = 3;
	private static final int MIN_OUTSKIRT_DISTANCE = 8;
	private static final int MAX_OUTSKIRT_DISTANCE = 24;
	private static final int MAX_VILLAGE_RADIUS = 80;
	private static final double SOLDIER_SEARCH_MARGIN = 48.0D;
	private static final double MIN_PLAYER_DISTANCE = 16.0D;
	private static final float WAVE_SPREAD = 0.8F;
	private static final int PLACEMENT_ATTEMPTS = 8;
	private static final int SURFACE_SCAN_UP = 16;
	private static final int SURFACE_SCAN_DOWN = 24;

	private static final double BASE_HEALTH = 600.0D;
	private static final double BASE_MELEE = 16.0D;
	private static final double BASE_KI = 12.0D;
	private static final float RESIZED_CHANCE = 0.4F;
	private static final float MIN_SCALE = 0.85F;
	private static final float MAX_SCALE = 1.35F;

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
		if (!level.dimension().equals(DemonRealmDimension.DEMON_REALM_KEY) || level.players().isEmpty()) return;
		if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
		if (level.getGameTime() % SCAN_INTERVAL_TICKS == 0L) scanVillages(level);
	}

	private static void scanVillages(ServerLevel level) {
		Structure village = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(DMZStructures.DEMON_VILLAGE);
		if (village == null) return;

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

					StructureStart start = chunk.getStartForStructure(village);
					if (start != null && start.isValid()) trySpawnWave(level, start);
				}
			}
		}
	}

	private static void trySpawnWave(ServerLevel level, StructureStart start) {
		RandomSource random = level.getRandom();
		if (random.nextFloat() >= WAVE_CHANCE) return;

		BlockPos plaza = DemonVillageStructure.plaza(start);
		if (plaza == null || !level.areEntitiesLoaded(ChunkPos.asLong(plaza))) return;

		BoundingBox village = start.getBoundingBox();
		AABB area = AABB.of(village).inflate(SOLDIER_SEARCH_MARGIN);
		int alive = level.getEntitiesOfClass(SagaDaimaEntity.GomahSoldierEntity.class, area, Entity::isAlive).size();
		int room = MAX_SOLDIERS_PER_VILLAGE - alive;
		if (room <= 0) return;

		int radius = Math.min(MAX_VILLAGE_RADIUS, Math.max(village.getXSpan(), village.getZSpan()) / 2);
		int size = Math.min(room, MIN_WAVE_SIZE + random.nextInt(MAX_WAVE_SIZE - MIN_WAVE_SIZE + 1));
		float angle = random.nextFloat() * Mth.TWO_PI;

		for (int i = 0; i < size; i++) {
			BlockPos spot = findSpot(level, plaza, radius + MIN_OUTSKIRT_DISTANCE, radius + MAX_OUTSKIRT_DISTANCE, angle, random);
			if (spot != null) spawnSoldier(level, spot, random);
		}
	}

	private static BlockPos findSpot(ServerLevel level, BlockPos plaza, int minDistance, int maxDistance, float angle, RandomSource random) {
		for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
			float a = angle + (random.nextFloat() - 0.5F) * WAVE_SPREAD;
			int distance = minDistance + random.nextInt(Math.max(1, maxDistance - minDistance + 1));
			int x = plaza.getX() + Mth.floor(Mth.cos(a) * distance);
			int z = plaza.getZ() + Mth.floor(Mth.sin(a) * distance);

			if (!level.isPositionEntityTicking(new BlockPos(x, plaza.getY(), z))) continue;

			BlockPos pos = surface(level, x, z, plaza.getY());
			if (pos == null) continue;
			BlockState ground = level.getBlockState(pos.below());
			if (!ground.isFaceSturdy(level, pos.below(), Direction.UP)) continue;
			if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty()) continue;
			if (!level.noCollision(MainEntities.SAGA_GOMAH_SOLDIER_1.get().getAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D))) continue;
			if (level.getNearestPlayer(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, MIN_PLAYER_DISTANCE, false) != null) continue;
			return pos;
		}
		return null;
	}

	private static BlockPos surface(ServerLevel level, int x, int z, int hint) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean openAbove = false;
		for (int y = hint + SURFACE_SCAN_UP; y >= hint - SURFACE_SCAN_DOWN; y--) {
			BlockState state = level.getBlockState(pos.set(x, y, z));
			if (state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty())) {
				openAbove = true;
			} else if (openAbove) {
				return pos.above().immutable();
			}
		}
		return null;
	}

	private static void spawnSoldier(ServerLevel level, BlockPos pos, RandomSource random) {
		EntityType<SagaDaimaEntity.GomahSoldierEntity> type = random.nextBoolean()
				? MainEntities.SAGA_GOMAH_SOLDIER_1.get() : MainEntities.SAGA_GOMAH_SOLDIER_2.get();
		SagaDaimaEntity.GomahSoldierEntity soldier = type.create(level);
		if (soldier == null) return;

		float scale = random.nextFloat() < RESIZED_CHANCE ? MIN_SCALE + random.nextFloat() * (MAX_SCALE - MIN_SCALE) : 1.0F;
		soldier.setSoldierScale(scale);
		soldier.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
		soldier.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
		soldier.getPersistentData().putBoolean("dmz_stats_configured", true);
		if (!level.addFreshEntity(soldier)) return;

		setBase(soldier.getAttribute(Attributes.MAX_HEALTH), BASE_HEALTH * scale);
		setBase(soldier.getAttribute(Attributes.ATTACK_DAMAGE), BASE_MELEE * scale);
		setBase(soldier.getAttribute(EntityAttributes.KI_BLAST_DAMAGE.get()), BASE_KI * scale);
		soldier.setHealth(soldier.getMaxHealth());
	}

	private static void setBase(AttributeInstance attribute, double value) {
		if (attribute != null) attribute.setBaseValue(value);
	}
}
