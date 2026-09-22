package com.dragonminez.server.world.worldboss;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WorldBossSessions {
	private static final int DISCOVERY_INTERVAL = 20;
	private static final int MISSING_BOSS_TICKS_BEFORE_END = 200;

	private static final Map<String, WorldBossSession> ACTIVE = new HashMap<>();

	private WorldBossSessions() {}

	public static WorldBossSession get(String bossKey) {
		return bossKey == null ? null : ACTIVE.get(bossKey);
	}

	public static WorldBossSession onBossEngaged(WorldBossEntity boss) {
		if (!(boss.level() instanceof ServerLevel level)) return null;
		WorldBossSession session = ACTIVE.get(boss.getWorldBossKey());
		if (session != null && session.isEnded()) {
			ACTIVE.remove(boss.getWorldBossKey());
			session = null;
		}
		if (session == null) {
			session = new WorldBossSession(boss.getWorldBossKey(), level);
			ACTIVE.put(boss.getWorldBossKey(), session);
			LogUtil.info(Env.SERVER, "World boss fight started for {}", boss.getWorldBossKey());
		}
		session.attachBoss(boss);
		return session;
	}

	public static void onBossReset(WorldBossEntity boss) {
		WorldBossSession session = ACTIVE.get(boss.getWorldBossKey());
		if (session == null || !(boss.level() instanceof ServerLevel level)) return;
		session.finishDefeat(level);
		ACTIVE.remove(boss.getWorldBossKey());
	}

	public static void onBossTransformed(WorldBossEntity next) {
		WorldBossSession session = ACTIVE.get(next.getWorldBossKey());
		if (session == null || session.isEnded() || next.isBossAsleep()) return;
		session.attachBoss(next);
	}

	public static void endFight(String bossKey, ServerLevel level) {
		WorldBossSession session = ACTIVE.remove(bossKey);
		if (session == null || level == null) return;
		session.finishDefeat(level);
	}

	public static boolean isFightEngaged(WorldBossEntity boss, double leashRadius) {
		WorldBossSession session = ACTIVE.get(boss.getWorldBossKey());
		if (session == null || session.isEnded() || !(boss.level() instanceof ServerLevel level)) return false;
		return session.hasEngagedParticipant(level, boss, leashRadius);
	}

	public static void onBossDefeated(WorldBossEntity boss) {
		if (!(boss.level() instanceof ServerLevel level)) return;
		WorldBossSession session = onBossEngaged(boss);
		if (session == null) return;
		session.finishVictory(level, boss);
		ACTIVE.remove(boss.getWorldBossKey());
	}

	public static void tick(ServerLevel level) {
		if (level.getGameTime() % DISCOVERY_INTERVAL == 0) discoverAwakeBosses(level);
		if (ACTIVE.isEmpty()) return;

		List<String> ended = new ArrayList<>();
		for (WorldBossSession session : new ArrayList<>(ACTIVE.values())) {
			if (!session.dimension().equals(level.dimension())) continue;
			WorldBossEntity boss = session.resolveBoss(level);
			if (boss == null) {
				if (session.noteBossMissing() >= MISSING_BOSS_TICKS_BEFORE_END) {
					LogUtil.warn(Env.SERVER, "World boss {} vanished mid-fight, ending the fight as lost", session.bossKey());
					session.finishDefeat(level);
					ended.add(session.bossKey());
				}
				continue;
			}
			if (boss.isBossAsleep()) {
				session.finishDefeat(level);
				ended.add(session.bossKey());
				continue;
			}
			session.tick(level, boss);
			if (session.isEnded()) ended.add(session.bossKey());
		}
		for (String key : ended) ACTIVE.remove(key);
	}

	private static void discoverAwakeBosses(ServerLevel level) {
		WorldBossSavedData data = WorldBossSavedData.get(level.getServer());
		WorldBossSavedData.Entry entry = data.peek(WorldBossManager.JANEMBA);
		if (entry == null || entry.bossId == null || ACTIVE.containsKey(WorldBossManager.JANEMBA)) return;
		Entity entity = level.getEntity(entry.bossId);
		if (entity instanceof WorldBossEntity boss && boss.isAlive() && !boss.isBossAsleep()) onBossEngaged(boss);
	}

	public static WorldBossSession activeFor(ServerPlayer player) {
		if (player == null) return null;
		for (WorldBossSession session : ACTIVE.values()) {
			if (!session.isEnded() && session.isParticipant(player)) return session;
		}
		return null;
	}

	public static boolean tryKnockOut(ServerPlayer victim, StatsData stats, DamageSource source) {
		WorldBossSession session = activeFor(victim);
		if (session == null || !(victim.level() instanceof ServerLevel level)) return false;
		return session.knockOut(level, victim, stats, source);
	}

	public static boolean isKnockedOut(ServerPlayer player) {
		WorldBossSession session = activeFor(player);
		return session != null && session.isKnockedOut(player.getUUID());
	}

	public static boolean isCastingRevive(ServerPlayer player) {
		WorldBossSession session = activeFor(player);
		return session != null && session.isCasting(player.getUUID());
	}

	public static void requestRevive(ServerPlayer caster, int targetEntityId) {
		WorldBossSession session = activeFor(caster);
		if (session == null || !(caster.level() instanceof ServerLevel level)) return;
		session.tryStartRevive(level, caster, targetEntityId);
	}

	public static void interruptRevive(ServerPlayer caster) {
		WorldBossSession session = activeFor(caster);
		if (session != null) session.interruptCast(caster, true);
	}

	public static void onPlayerLogout(ServerPlayer player) {
		for (WorldBossSession session : ACTIVE.values()) session.onPlayerLogout(player);
	}

	public static void onPlayerLogin(ServerPlayer player, StatsData data) {
		boolean handled = false;
		for (WorldBossSession session : ACTIVE.values()) {
			if (session.onPlayerLogin(player, data)) handled = true;
		}
		if (!handled && data.getTechniques().hasRevive()) {
			data.getTechniques().uninstallRevive();
		}
	}

	public static void onPlayerChangedDimension(ServerPlayer player, StatsData data) {
		for (WorldBossSession session : ACTIVE.values()) session.onPlayerLeftFight(player, data);
	}

	public static void clearAll() {
		ACTIVE.clear();
	}
}
