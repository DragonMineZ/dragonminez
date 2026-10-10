package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StoryToastS2C;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.init.entities.ai.AiTierResolver;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.quest.QuestParty;
import com.dragonminez.common.quest.QuestService;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.quest.objectives.EscortObjective;
import com.dragonminez.common.quest.objectives.SurviveWavesObjective;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side runtime state for "field" objectives that own live entities: SURVIVE_WAVES and
 * ESCORT. Sessions are keyed by session owner + quest key + objective index, are re-created
 * from persisted objective progress after relog (waves resume at the current wave), and are torn
 * down — despawning their entities — on completion, failure, restart, or logout.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class QuestFieldSessions {

	private static final String SESSION_TAG = "dmz_quest_session";
	private static final String ESCORT_STATE_TAG = "dmz_quest_escort_state";
	private static final double WAVE_SPAWN_RADIUS = 6.0;
	private static final double WAVE_LEASH_DISTANCE_SQR = 64.0 * 64.0;
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
		if (!isSessionOwner(player, questKey, quest, objectiveIndex, false)) return;

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
			broadcast(player, questKey, Component.translatable("message.dragonminez.quest.wave_incoming",
					progress + 1, required));
			return;
		}

		if (!session.aliveMobs.isEmpty() || session.lost) {
			if (session.lost || session.abandonedBy(player)) {
				despawnSessionMobs(player.getServer().getLevel(session.dimension), session.aliveMobs);
				session.reset(gameTime + objective.getWaveDelaySeconds() * 20L);
				broadcast(player, questKey, Component.translatable("message.dragonminez.quest.wave_reset",
						progress + 1, required));
				return;
			}

			pruneDeadMobs(player.serverLevel(), session);
			if (session.aliveMobs.isEmpty()) {
				creditSessionProgress(player, pqd, questKey, quest, objectiveIndex);
				if (progress + 1 < required) {
					session.reset(gameTime + objective.getWaveDelaySeconds() * 20L);
					broadcast(player, questKey, Component.translatable("message.dragonminez.quest.wave_cleared",
							progress + 1, required));
				} else {
					clearWaveSession(player, sessionKey);
				}
			}
			return;
		}

		if (gameTime >= session.nextWaveGameTime) {
			spawnWave(player, questKey, objectiveIndex, objective, session);
			broadcast(player, questKey, Component.translatable("message.dragonminez.quest.wave_started",
					progress + 1, required));
		}
	}

	private static void spawnWave(ServerPlayer player, String questKey, int objectiveIndex,
								  SurviveWavesObjective objective, WaveSession session) {
		EntityType<?> entityType = objective.resolveEntityType();
		if (entityType == null) return;
		Difficulty waveDifficulty = StatsProvider.get(StatsCapability.INSTANCE, player).resolve()
				.map(d -> d.getPlayerQuestData().getDifficulty()).orElse(Difficulty.NORMAL);

		session.dimension = player.level().dimension();
		session.origin = player.blockPosition();
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
			int waveTier = objective.getAiTier() > 0 ? objective.getAiTier() : AiTierResolver.storyTier(waveDifficulty, entityType);
			entity.getPersistentData().putInt("dmz_quest_ai_tier", waveTier);
			if (!objective.isCanTransform()) entity.getPersistentData().putBoolean("dmz_quest_no_transform", true);

			if (entity instanceof Mob mob) {
				mob.setTarget(player);
				mob.setPersistenceRequired();
			}

			session.aliveMobs.add(entity.getUUID());
			player.serverLevel().addFreshEntity(entity);
		}
	}

	private static void pruneDeadMobs(ServerLevel level, WaveSession session) {
		Iterator<UUID> iterator = session.aliveMobs.iterator();
		while (iterator.hasNext()) {
			Entity entity = level.getEntity(iterator.next());
			if (entity != null && !entity.isAlive()) iterator.remove();
		}
	}

	// ====================================================================================
	// Escorts
	// ====================================================================================

	static void tickEscort(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest,
						   int objectiveIndex, EscortObjective objective) {
		if (!isSessionOwner(player, questKey, quest, objectiveIndex, true)) return;

		String sessionKey = sessionKey(questKey, objectiveIndex);
		int progress = pqd.getObjectiveProgress(questKey, objectiveIndex);
		if (progress >= quest.getObjectiveRequired(pqd, questKey, objectiveIndex)) {
			clearEscortSession(player, sessionKey);
			return;
		}

		Map<String, EscortSession> sessions = ESCORT_SESSIONS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
		EscortSession session = sessions.get(sessionKey);
		if (session != null && session.escortDied) {
			failQuest(player, questKey, quest, DMZEvent.QuestFailEvent.FailureReason.ESCORT_FAILED);
			sessions.remove(sessionKey);
			return;
		}
		if (session == null || player.serverLevel().getEntity(session.escortId) == null) {
			if (session != null) {
				Entity stale = findEscort(player, session.escortId);
				if (stale != null) stale.discard();
			}
			LivingEntity escort = spawnEscort(player, questKey, objectiveIndex, objective);
			if (escort != null) {
				sessions.put(sessionKey, new EscortSession(escort.getUUID()));
				broadcast(player, questKey, Component.translatable("message.dragonminez.quest.escort_started"));
			}
			return;
		}

		Entity escort = player.serverLevel().getEntity(session.escortId);
		if (escort == null || !escort.isAlive()) {
			failQuest(player, questKey, quest, DMZEvent.QuestFailEvent.FailureReason.ESCORT_FAILED);
			clearEscortSession(player, sessionKey);
			return;
		}

		double distSqr = escort.distanceToSqr(objective.getTargetPos().getX() + 0.5,
				objective.getTargetPos().getY() + 0.5, objective.getTargetPos().getZ() + 0.5);
		if (QuestEvents.isInObjectiveDimension(player, pqd, questKey, objective.getDimension())
				&& distSqr <= (double) objective.getRadius() * objective.getRadius()) {
			escort.discard();
			sessions.remove(sessionKey);
			creditSessionProgress(player, pqd, questKey, quest, objectiveIndex);
			broadcast(player, questKey, Component.translatable("message.dragonminez.quest.escort_arrived"));
		} else if (escort.distanceToSqr(player) > ESCORT_TELEPORT_DISTANCE_SQR) {
			escort.teleportTo(player.getX(), player.getY(), player.getZ());
		}
	}

	private static LivingEntity spawnEscort(ServerPlayer player, String questKey, int objectiveIndex, EscortObjective objective) {
		EntityType<?> entityType = objective.resolveEntityType();
		if (entityType == null) return null;

		Entity entity = entityType.create(player.level());
		if (!(entity instanceof LivingEntity living)) return null;

		String sessionKey = sessionKey(questKey, objectiveIndex);
		entity.setPos(player.getX() + 1.0, player.getY(), player.getZ() + 1.0);
		entity.getPersistentData().putString(QuestService.QUEST_KEY_TAG, questKey);
		entity.getPersistentData().putInt(QuestService.QUEST_OBJECTIVE_INDEX_TAG, objectiveIndex);
		entity.getPersistentData().putString(QuestService.QUEST_OWNER_TAG, player.getStringUUID());
		entity.getPersistentData().putString(SESSION_TAG, sessionKey);
		entity.getPersistentData().putBoolean("dmz_quest_escort", true);

		if (objective.getEscortHealth() > 0 && living.getAttribute(Attributes.MAX_HEALTH) != null) {
			living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(objective.getEscortHealth());
			living.setHealth((float) objective.getEscortHealth());
		}
		restoreEscortState(player, sessionKey, living);
		if (living instanceof PathfinderMob mob) {
			mob.setPersistenceRequired();
			mob.goalSelector.addGoal(1, new FollowQuestOwnerGoal(mob, player.getUUID()));
		}

		player.serverLevel().addFreshEntity(entity);
		return living;
	}

	private static void saveEscortState(ServerPlayer player, String sessionKey, LivingEntity escort) {
		CompoundTag states = player.getPersistentData().getCompound(ESCORT_STATE_TAG);
		CompoundTag state = new CompoundTag();
		state.putFloat("health", escort.getHealth() / Math.max(1.0f, escort.getMaxHealth()));
		state.putString("dimension", escort.level().dimension().location().toString());
		state.putDouble("x", escort.getX());
		state.putDouble("y", escort.getY());
		state.putDouble("z", escort.getZ());
		states.put(sessionKey, state);
		player.getPersistentData().put(ESCORT_STATE_TAG, states);
	}

	private static void restoreEscortState(ServerPlayer player, String sessionKey, LivingEntity escort) {
		CompoundTag states = player.getPersistentData().getCompound(ESCORT_STATE_TAG);
		if (!states.contains(sessionKey)) return;
		CompoundTag state = states.getCompound(sessionKey);
		states.remove(sessionKey);
		if (states.isEmpty()) player.getPersistentData().remove(ESCORT_STATE_TAG);
		else player.getPersistentData().put(ESCORT_STATE_TAG, states);

		float fraction = state.getFloat("health");
		if (fraction > 0.0f) escort.setHealth(Math.max(1.0f, escort.getMaxHealth() * Math.min(1.0f, fraction)));
		if (player.level().dimension().location().toString().equals(state.getString("dimension"))) {
			escort.setPos(state.getDouble("x"), state.getDouble("y"), state.getDouble("z"));
		}
	}

	private static void forgetEscortState(ServerPlayer player, String questKey) {
		CompoundTag states = player.getPersistentData().getCompound(ESCORT_STATE_TAG);
		if (states.isEmpty()) return;
		states.getAllKeys().removeIf(key -> key.startsWith(questKey + "#"));
		if (states.isEmpty()) player.getPersistentData().remove(ESCORT_STATE_TAG);
		else player.getPersistentData().put(ESCORT_STATE_TAG, states);
	}

	// ====================================================================================
	// Lifecycle hooks
	// ====================================================================================

	/** Called from LivingDeathEvent so escort deaths fail fast instead of waiting for the tick. */
	static void onEntityDeath(LivingEntity entity) {
		CompoundTag tags = entity.getPersistentData();
		if (!tags.contains(SESSION_TAG)) return;
		String sessionKey = tags.getString(SESSION_TAG);
		UUID owner = parseOwner(tags);
		if (owner == null) return;

		if (tags.getBoolean("dmz_quest_escort")) {
			Map<String, EscortSession> sessions = ESCORT_SESSIONS.get(owner);
			EscortSession session = sessions != null ? sessions.get(sessionKey) : null;
			if (session != null && entity.getUUID().equals(session.escortId)) session.escortDied = true;
			return;
		}

		Map<String, WaveSession> waves = WAVE_SESSIONS.get(owner);
		WaveSession session = waves != null ? waves.get(sessionKey) : null;
		if (session != null) session.aliveMobs.remove(entity.getUUID());
	}

	@SubscribeEvent
	public static void onEntityLeave(EntityLeaveLevelEvent event) {
		Entity entity = event.getEntity();
		if (entity.level().isClientSide() || !entity.getPersistentData().contains(SESSION_TAG)) return;
		Entity.RemovalReason reason = entity.getRemovalReason();
		if (reason == null || reason == Entity.RemovalReason.KILLED || !reason.shouldDestroy() && reason != Entity.RemovalReason.CHANGED_DIMENSION) return;

		UUID owner = parseOwner(entity.getPersistentData());
		if (owner == null || entity.getPersistentData().getBoolean("dmz_quest_escort")) return;
		Map<String, WaveSession> waves = WAVE_SESSIONS.get(owner);
		WaveSession session = waves != null ? waves.get(entity.getPersistentData().getString(SESSION_TAG)) : null;
		if (session != null && session.aliveMobs.remove(entity.getUUID())) session.lost = true;
	}

	@SubscribeEvent
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		Entity entity = event.getEntity();
		if (event.getLevel().isClientSide() || !event.loadedFromDisk()) return;
		CompoundTag tags = entity.getPersistentData();
		if (!tags.contains(SESSION_TAG)) return;

		UUID owner = parseOwner(tags);
		String sessionKey = tags.getString(SESSION_TAG);
		boolean tracked = false;
		if (owner != null) {
			if (tags.getBoolean("dmz_quest_escort")) {
				Map<String, EscortSession> sessions = ESCORT_SESSIONS.get(owner);
				EscortSession session = sessions != null ? sessions.get(sessionKey) : null;
				tracked = session != null && entity.getUUID().equals(session.escortId);
			} else {
				Map<String, WaveSession> sessions = WAVE_SESSIONS.get(owner);
				WaveSession session = sessions != null ? sessions.get(sessionKey) : null;
				tracked = session != null && session.aliveMobs.contains(entity.getUUID());
			}
		}
		if (!tracked) event.setCanceled(true);
	}

	/** Tears down every session (and its live entities) for one quest of a player. */
	public static void clearQuest(ServerPlayer player, String questKey) {
		Map<String, WaveSession> waves = WAVE_SESSIONS.get(player.getUUID());
		if (waves != null) {
			waves.keySet().removeIf(key -> {
				if (!key.startsWith(questKey + "#")) return false;
				WaveSession session = waves.get(key);
				despawnSessionMobs(player.getServer().getLevel(session.dimension), session.aliveMobs);
				return true;
			});
		}
		Map<String, EscortSession> escorts = ESCORT_SESSIONS.get(player.getUUID());
		if (escorts != null) {
			escorts.keySet().removeIf(key -> {
				if (!key.startsWith(questKey + "#")) return false;
				Entity escort = findEscort(player, escorts.get(key).escortId);
				if (escort != null) escort.discard();
				return true;
			});
		}
		forgetEscortState(player, questKey);
	}

	/** Full teardown on logout; sessions rebuild from persisted progress on the next tick after relog. */
	public static void clearAll(ServerPlayer player) {
		Map<String, WaveSession> waves = WAVE_SESSIONS.remove(player.getUUID());
		if (waves != null) {
			waves.values().forEach(session ->
					despawnSessionMobs(player.getServer().getLevel(session.dimension), session.aliveMobs));
		}
		Map<String, EscortSession> escorts = ESCORT_SESSIONS.remove(player.getUUID());
		if (escorts != null) {
			for (Map.Entry<String, EscortSession> entry : escorts.entrySet()) {
				Entity escort = findEscort(player, entry.getValue().escortId);
				if (escort == null) continue;
				if (escort instanceof LivingEntity living && living.isAlive()) saveEscortState(player, entry.getKey(), living);
				escort.discard();
			}
		}
	}

	private static Entity findEscort(ServerPlayer player, UUID escortId) {
		for (ServerLevel level : player.getServer().getAllLevels()) {
			Entity entity = level.getEntity(escortId);
			if (entity != null) return entity;
		}
		return null;
	}

	private static void despawnSessionMobs(ServerLevel level, Set<UUID> mobIds) {
		if (level != null) {
			for (UUID mobId : new ArrayList<>(mobIds)) {
				Entity entity = level.getEntity(mobId);
				if (entity != null) entity.discard();
			}
		}
		mobIds.clear();
	}

	private static void creditSessionProgress(ServerPlayer owner, PlayerQuestData ownerData, String questKey, Quest quest,
											  int objectiveIndex) {
		for (ServerPlayer participant : QuestParty.participants(owner, questKey)) {
			PlayerQuestData data = participant == owner ? ownerData : QuestParty.questData(participant);
			if (data == null) continue;
			int progress = data.getObjectiveProgress(questKey, objectiveIndex);
			if (progress >= quest.getObjectiveRequired(data, questKey, objectiveIndex)) continue;
			if (!quest.isParallelObjectives() && !QuestEvents.isFirstUncompleted(data, questKey, quest, objectiveIndex)) continue;
			QuestEvents.updateProgress(participant, data, questKey, quest, objectiveIndex, progress + 1);
			QuestEvents.checkAndComplete(participant, data, questKey, quest);
		}
	}

	private static void failQuest(ServerPlayer owner, String questKey, Quest quest,
								  DMZEvent.QuestFailEvent.FailureReason reason) {
		for (ServerPlayer participant : QuestParty.allParticipants(owner, questKey)) {
			PlayerQuestData data = QuestParty.questData(participant);
			if (data == null || !data.isQuestAccepted(questKey)) continue;
			if (!QuestEvents.failQuestFor(participant, data, questKey, quest, reason)) continue;
			NetworkHandler.sendToPlayer(StoryToastS2C.questFailed(questKey), participant);
			participant.sendSystemMessage(Component.translatable("message.dragonminez.quest.escort_failed"));
			QuestService.syncQuestState(participant);
		}
	}

	private static void clearWaveSession(ServerPlayer player, String sessionKey) {
		Map<String, WaveSession> sessions = WAVE_SESSIONS.get(player.getUUID());
		if (sessions == null) return;
		WaveSession session = sessions.remove(sessionKey);
		if (session != null) despawnSessionMobs(player.getServer().getLevel(session.dimension), session.aliveMobs);
	}

	private static void clearEscortSession(ServerPlayer player, String sessionKey) {
		Map<String, EscortSession> sessions = ESCORT_SESSIONS.get(player.getUUID());
		if (sessions == null) return;
		EscortSession session = sessions.remove(sessionKey);
		if (session != null) {
			Entity escort = findEscort(player, session.escortId);
			if (escort != null) escort.discard();
		}
	}

	private static boolean isSessionOwner(ServerPlayer player, String questKey, Quest quest, int objectiveIndex, boolean escort) {
		String sessionKey = sessionKey(questKey, objectiveIndex);
		ServerPlayer firstHolder = null;
		ServerPlayer firstNeeding = null;
		for (UUID memberId : PartyManager.getPartyMemberIds(player)) {
			ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
			if (member == null) continue;
			PlayerQuestData data = QuestParty.questData(member);
			if (data == null || !data.isQuestAccepted(questKey)) continue;
			if (data.getObjectiveProgress(questKey, objectiveIndex) >= quest.getObjectiveRequired(data, questKey, objectiveIndex)) continue;
			if (member != player && !QuestParty.inShareRange(player, member)) continue;
			if (firstNeeding == null) firstNeeding = member;
			if (firstHolder == null && holdsSession(member.getUUID(), sessionKey, escort)) firstHolder = member;
		}
		ServerPlayer owner = firstHolder != null ? firstHolder : firstNeeding;
		if (owner == null || owner == player) return true;
		if (escort) clearEscortSession(player, sessionKey);
		else clearWaveSession(player, sessionKey);
		return false;
	}

	private static boolean holdsSession(UUID playerId, String sessionKey, boolean escort) {
		Map<String, ?> sessions = escort ? ESCORT_SESSIONS.get(playerId) : WAVE_SESSIONS.get(playerId);
		return sessions != null && sessions.containsKey(sessionKey);
	}

	private static void broadcast(ServerPlayer owner, String questKey, Component message) {
		for (ServerPlayer member : QuestParty.participants(owner, questKey)) {
			member.displayClientMessage(message, true);
		}
	}

	private static UUID parseOwner(CompoundTag tags) {
		try {
			return UUID.fromString(tags.getString(QuestService.QUEST_OWNER_TAG));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static String sessionKey(String questKey, int objectiveIndex) {
		return questKey + "#" + objectiveIndex;
	}

	private static class WaveSession {
		final Set<UUID> aliveMobs = new HashSet<>();
		long nextWaveGameTime;
		ResourceKey<Level> dimension = Level.OVERWORLD;
		BlockPos origin = BlockPos.ZERO;
		boolean lost = false;

		WaveSession(long nextWaveGameTime) {
			this.nextWaveGameTime = nextWaveGameTime;
		}

		void reset(long nextWaveGameTime) {
			this.aliveMobs.clear();
			this.lost = false;
			this.nextWaveGameTime = nextWaveGameTime;
		}

		boolean abandonedBy(ServerPlayer owner) {
			return owner.level().dimension() != dimension
					|| owner.blockPosition().distSqr(origin) > WAVE_LEASH_DISTANCE_SQR;
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
