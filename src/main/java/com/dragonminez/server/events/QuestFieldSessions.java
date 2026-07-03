package com.dragonminez.server.events;

import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StoryToastS2C;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestService;
import com.dragonminez.common.quest.objectives.EscortObjective;
import com.dragonminez.common.quest.objectives.SurviveWavesObjective;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side runtime state for "field" objectives that own live entities: SURVIVE_WAVES and
 * ESCORT. Sessions are keyed by quest controller + quest key + objective index, are re-created
 * from persisted objective progress after relog (waves resume at the current wave), and are torn
 * down — despawning their entities — on completion, failure, restart, or logout.
 */
public final class QuestFieldSessions {

	private static final String SESSION_TAG = "dmz_quest_session";
	private static final double WAVE_SPAWN_RADIUS = 6.0;
	private static final double ESCORT_TELEPORT_DISTANCE_SQR = 48.0 * 48.0;

	private static final Map<UUID, Map<String, WaveSession>> WAVE_SESSIONS = new ConcurrentHashMap<>();
	private static final Map<UUID, Map<String, EscortSession>> ESCORT_SESSIONS = new ConcurrentHashMap<>();

	private QuestFieldSessions() {
	}

	// ====================================================================================
	// Waves
	// ====================================================================================

	static void tickWaves(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest,
						  int objectiveIndex, SurviveWavesObjective objective) {
		if (!isController(player)) return;

		String sessionKey = sessionKey(questKey, objectiveIndex);
		int progress = pqd.getObjectiveProgress(questKey, objectiveIndex);
		int required = quest.getObjectiveRequired(pqd, questKey, objectiveIndex);
		if (progress >= required) {
			clearWaveSession(player, sessionKey);
			return;
		}

		Map<String, WaveSession> sessions = WAVE_SESSIONS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
		long gameTime = player.serverLevel().getGameTime();
		WaveSession session = sessions.get(sessionKey);
		if (session == null) {
			session = new WaveSession(gameTime + objective.getWaveDelaySeconds() * 20L);
			sessions.put(sessionKey, session);
			broadcast(player, Component.translatable("message.dragonminez.quest.wave_incoming",
					progress + 1, required));
			return;
		}

		if (!session.aliveMobs.isEmpty()) {
			pruneDeadMobs(player.serverLevel(), session);
			if (session.aliveMobs.isEmpty()) {
				QuestEvents.updateProgress(player, pqd, questKey, quest, objectiveIndex, progress + 1);
				QuestEvents.checkAndComplete(player, pqd, questKey, quest);
				if (progress + 1 < required) {
					session.nextWaveGameTime = gameTime + objective.getWaveDelaySeconds() * 20L;
					broadcast(player, Component.translatable("message.dragonminez.quest.wave_cleared",
							progress + 1, required));
				} else {
					clearWaveSession(player, sessionKey);
				}
			}
			return;
		}

		if (gameTime >= session.nextWaveGameTime) {
			spawnWave(player, questKey, objectiveIndex, objective, session);
			broadcast(player, Component.translatable("message.dragonminez.quest.wave_started",
					progress + 1, required));
		}
	}

	private static void spawnWave(ServerPlayer player, String questKey, int objectiveIndex,
								  SurviveWavesObjective objective, WaveSession session) {
		EntityType<?> entityType = objective.resolveEntityType();
		if (entityType == null) return;

		for (int i = 0; i < objective.getMobsPerWave(); i++) {
			Entity entity = entityType.create(player.level());
			if (entity == null) continue;

			double offsetX = (Math.random() - 0.5) * 2 * WAVE_SPAWN_RADIUS;
			double offsetZ = (Math.random() - 0.5) * 2 * WAVE_SPAWN_RADIUS;
			entity.setPos(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);

			entity.getPersistentData().putString(QuestService.QUEST_KEY_TAG, questKey);
			entity.getPersistentData().putInt(QuestService.QUEST_OBJECTIVE_INDEX_TAG, objectiveIndex);
			entity.getPersistentData().putString(QuestService.QUEST_OWNER_TAG, player.getStringUUID());
			entity.getPersistentData().putString(SESSION_TAG, sessionKey(questKey, objectiveIndex));
			if (objective.getHealth() > 0) entity.getPersistentData().putDouble("dmz_quest_hp", objective.getHealth());
			if (objective.getMeleeDamage() > 0) entity.getPersistentData().putDouble("dmz_quest_melee", objective.getMeleeDamage());
			if (objective.getKiDamage() > 0) entity.getPersistentData().putDouble("dmz_quest_ki", objective.getKiDamage());
			if (objective.getTextureVariant() >= 0) entity.getPersistentData().putInt("dmz_quest_texture_variant", objective.getTextureVariant());
			if (objective.getAiTier() > 0) entity.getPersistentData().putInt("dmz_quest_ai_tier", objective.getAiTier());
			if (!objective.isCanTransform()) entity.getPersistentData().putBoolean("dmz_quest_no_transform", true);

			if (entity instanceof Mob mob) {
				mob.setTarget(player);
				mob.setPersistenceRequired();
			}

			player.serverLevel().addFreshEntity(entity);
			session.aliveMobs.add(entity.getUUID());
		}
	}

	private static void pruneDeadMobs(ServerLevel level, WaveSession session) {
		Iterator<UUID> iterator = session.aliveMobs.iterator();
		while (iterator.hasNext()) {
			Entity entity = level.getEntity(iterator.next());
			if (entity == null || !entity.isAlive()) iterator.remove();
		}
	}

	// ====================================================================================
	// Escorts
	// ====================================================================================

	static void tickEscort(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest,
						   int objectiveIndex, EscortObjective objective) {
		if (!isController(player)) return;

		String sessionKey = sessionKey(questKey, objectiveIndex);
		int progress = pqd.getObjectiveProgress(questKey, objectiveIndex);
		if (progress >= quest.getObjectiveRequired(pqd, questKey, objectiveIndex)) {
			clearEscortSession(player, sessionKey);
			return;
		}

		Map<String, EscortSession> sessions = ESCORT_SESSIONS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
		EscortSession session = sessions.get(sessionKey);
		if (session == null || player.serverLevel().getEntity(session.escortId) == null) {
			if (session != null && session.escortDied) {
				failQuest(player, pqd, questKey, quest, DMZEvent.QuestFailEvent.FailureReason.ESCORT_FAILED);
				sessions.remove(sessionKey);
				return;
			}
			LivingEntity escort = spawnEscort(player, questKey, objectiveIndex, objective);
			if (escort != null) {
				sessions.put(sessionKey, new EscortSession(escort.getUUID()));
				broadcast(player, Component.translatable("message.dragonminez.quest.escort_started"));
			}
			return;
		}

		Entity escort = player.serverLevel().getEntity(session.escortId);
		if (escort == null || !escort.isAlive()) {
			failQuest(player, pqd, questKey, quest, DMZEvent.QuestFailEvent.FailureReason.ESCORT_FAILED);
			clearEscortSession(player, sessionKey);
			return;
		}

		double distSqr = escort.distanceToSqr(objective.getTargetPos().getX() + 0.5,
				objective.getTargetPos().getY() + 0.5, objective.getTargetPos().getZ() + 0.5);
		if (distSqr <= (double) objective.getRadius() * objective.getRadius()) {
			escort.discard();
			sessions.remove(sessionKey);
			QuestEvents.updateProgress(player, pqd, questKey, quest, objectiveIndex, progress + 1);
			QuestEvents.checkAndComplete(player, pqd, questKey, quest);
			broadcast(player, Component.translatable("message.dragonminez.quest.escort_arrived"));
		} else if (escort.distanceToSqr(player) > ESCORT_TELEPORT_DISTANCE_SQR) {
			escort.teleportTo(player.getX(), player.getY(), player.getZ());
		}
	}

	private static LivingEntity spawnEscort(ServerPlayer player, String questKey, int objectiveIndex, EscortObjective objective) {
		EntityType<?> entityType = objective.resolveEntityType();
		if (entityType == null) return null;

		Entity entity = entityType.create(player.level());
		if (!(entity instanceof LivingEntity living)) return null;

		entity.setPos(player.getX() + 1.0, player.getY(), player.getZ() + 1.0);
		entity.getPersistentData().putString(QuestService.QUEST_KEY_TAG, questKey);
		entity.getPersistentData().putInt(QuestService.QUEST_OBJECTIVE_INDEX_TAG, objectiveIndex);
		entity.getPersistentData().putString(QuestService.QUEST_OWNER_TAG, player.getStringUUID());
		entity.getPersistentData().putString(SESSION_TAG, sessionKey(questKey, objectiveIndex));
		entity.getPersistentData().putBoolean("dmz_quest_escort", true);

		if (objective.getEscortHealth() > 0 && living.getAttribute(Attributes.MAX_HEALTH) != null) {
			living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(objective.getEscortHealth());
			living.setHealth((float) objective.getEscortHealth());
		}
		if (living instanceof PathfinderMob mob) {
			mob.setPersistenceRequired();
			mob.goalSelector.addGoal(1, new FollowQuestOwnerGoal(mob, player.getUUID()));
		}

		player.serverLevel().addFreshEntity(entity);
		return living;
	}

	// ====================================================================================
	// Lifecycle hooks
	// ====================================================================================

	/** Called from LivingDeathEvent so escort deaths fail fast instead of waiting for the tick. */
	static void onEntityDeath(LivingEntity entity) {
		if (!entity.getPersistentData().getBoolean("dmz_quest_escort")) return;
		String owner = entity.getPersistentData().getString(QuestService.QUEST_OWNER_TAG);
		String sessionKey = entity.getPersistentData().getString(SESSION_TAG);
		try {
			Map<String, EscortSession> sessions = ESCORT_SESSIONS.get(UUID.fromString(owner));
			EscortSession session = sessions != null ? sessions.get(sessionKey) : null;
			if (session != null && entity.getUUID().equals(session.escortId)) session.escortDied = true;
		} catch (IllegalArgumentException ignored) {
		}
	}

	/** Tears down every session (and its live entities) for one quest of a player. */
	public static void clearQuest(ServerPlayer player, String questKey) {
		Map<String, WaveSession> waves = WAVE_SESSIONS.get(player.getUUID());
		if (waves != null) {
			waves.keySet().removeIf(key -> {
				if (!key.startsWith(questKey + "#")) return false;
				despawnSessionMobs(player.serverLevel(), waves.get(key).aliveMobs);
				return true;
			});
		}
		Map<String, EscortSession> escorts = ESCORT_SESSIONS.get(player.getUUID());
		if (escorts != null) {
			escorts.keySet().removeIf(key -> {
				if (!key.startsWith(questKey + "#")) return false;
				Entity escort = player.serverLevel().getEntity(escorts.get(key).escortId);
				if (escort != null) escort.discard();
				return true;
			});
		}
	}

	/** Full teardown on logout; sessions rebuild from persisted progress on the next tick after relog. */
	public static void clearAll(ServerPlayer player) {
		Map<String, WaveSession> waves = WAVE_SESSIONS.remove(player.getUUID());
		if (waves != null) waves.values().forEach(session -> despawnSessionMobs(player.serverLevel(), session.aliveMobs));
		Map<String, EscortSession> escorts = ESCORT_SESSIONS.remove(player.getUUID());
		if (escorts != null) {
			for (EscortSession session : escorts.values()) {
				Entity escort = player.serverLevel().getEntity(session.escortId);
				if (escort != null) escort.discard();
			}
		}
	}

	private static void despawnSessionMobs(ServerLevel level, Set<UUID> mobIds) {
		for (UUID mobId : mobIds) {
			Entity entity = level.getEntity(mobId);
			if (entity != null) entity.discard();
		}
		mobIds.clear();
	}

	private static void failQuest(ServerPlayer controller, PlayerQuestData pqd, String questKey, Quest quest,
								  DMZEvent.QuestFailEvent.FailureReason reason) {
		List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(controller);
		QuestService.ResolvedQuest resolved = QuestService.resolveQuest(questKey);
		DMZEvent.QuestFailEvent failEvent = new DMZEvent.QuestFailEvent(
				controller, questKey, resolved != null ? resolved.saga() : null, quest, partyMembers, reason);
		if (MinecraftForge.EVENT_BUS.post(failEvent)) return;

		pqd.failQuest(questKey);
		clearQuest(controller, questKey);
		for (ServerPlayer member : partyMembers) {
			NetworkHandler.sendToPlayer(StoryToastS2C.questFailed(questKey), member);
			member.sendSystemMessage(Component.translatable("message.dragonminez.quest.escort_failed"));
		}
		QuestService.syncQuestState(controller);
	}

	private static void clearWaveSession(ServerPlayer player, String sessionKey) {
		Map<String, WaveSession> sessions = WAVE_SESSIONS.get(player.getUUID());
		if (sessions == null) return;
		WaveSession session = sessions.remove(sessionKey);
		if (session != null) despawnSessionMobs(player.serverLevel(), session.aliveMobs);
	}

	private static void clearEscortSession(ServerPlayer player, String sessionKey) {
		Map<String, EscortSession> sessions = ESCORT_SESSIONS.get(player.getUUID());
		if (sessions == null) return;
		EscortSession session = sessions.remove(sessionKey);
		if (session != null) {
			Entity escort = player.serverLevel().getEntity(session.escortId);
			if (escort != null) escort.discard();
		}
	}

	private static boolean isController(ServerPlayer player) {
		ServerPlayer controller = PartyManager.resolveQuestController(player);
		return controller != null && controller.getUUID().equals(player.getUUID());
	}

	private static void broadcast(ServerPlayer controller, Component message) {
		for (ServerPlayer member : PartyManager.getAllPartyMembers(controller)) {
			member.displayClientMessage(message, true);
		}
	}

	private static String sessionKey(String questKey, int objectiveIndex) {
		return questKey + "#" + objectiveIndex;
	}

	private static class WaveSession {
		final Set<UUID> aliveMobs = new HashSet<>();
		long nextWaveGameTime;

		WaveSession(long nextWaveGameTime) {
			this.nextWaveGameTime = nextWaveGameTime;
		}
	}

	private static class EscortSession {
		final UUID escortId;
		boolean escortDied = false;

		EscortSession(UUID escortId) {
			this.escortId = escortId;
		}
	}

	/** Keeps the escort walking after its quest owner, teleport-catching up when left far behind. */
	private static class FollowQuestOwnerGoal extends Goal {
		private final PathfinderMob mob;
		private final UUID ownerId;

		FollowQuestOwnerGoal(PathfinderMob mob, UUID ownerId) {
			this.mob = mob;
			this.ownerId = ownerId;
			this.setFlags(EnumSet.of(Flag.MOVE));
		}

		private Player owner() {
			return mob.level().getPlayerByUUID(ownerId);
		}

		@Override
		public boolean canUse() {
			Player owner = owner();
			return owner != null && owner.isAlive() && mob.distanceToSqr(owner) > 36.0;
		}

		@Override
		public boolean canContinueToUse() {
			Player owner = owner();
			return owner != null && owner.isAlive() && mob.distanceToSqr(owner) > 16.0;
		}

		@Override
		public void tick() {
			Player owner = owner();
			if (owner == null) return;
			mob.getNavigation().moveTo(owner, 1.15);
		}

		@Override
		public void stop() {
			mob.getNavigation().stop();
		}
	}
}
