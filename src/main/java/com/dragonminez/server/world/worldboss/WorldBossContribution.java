package com.dragonminez.server.world.worldboss;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WorldBossContribution {

	public static final double WEIGHT_DAMAGE = 0.40D;
	public static final double WEIGHT_BLOCKED = 0.25D;
	public static final double WEIGHT_HEALED = 0.20D;
	public static final double WEIGHT_TANK = 0.15D;

	public static final double MIN_PAYOUT_RATIO = 0.5D;

	private static final Map<String, Map<UUID, Score>> SESSIONS = new HashMap<>();

	private static final class Score {
		double damage;
		double blocked;
		double healed;
		double tankTicks;
	}

	private WorldBossContribution() {}

	private static Score score(String bossKey, Player player) {
		if (bossKey == null || bossKey.isEmpty() || player == null) return null;
		return SESSIONS.computeIfAbsent(bossKey, k -> new HashMap<>())
				.computeIfAbsent(player.getUUID(), k -> new Score());
	}

	public static void addDamage(String bossKey, Player player, double amount) {
		if (amount <= 0.0D) return;
		Score s = score(bossKey, player);
		if (s != null) s.damage += amount;
	}

	public static void addBlocked(String bossKey, Player player, double amount) {
		if (amount <= 0.0D) return;
		Score s = score(bossKey, player);
		if (s != null) s.blocked += amount;
	}

	public static void addHealed(String bossKey, Player player, double amount) {
		if (amount <= 0.0D) return;
		Score s = score(bossKey, player);
		if (s != null) s.healed += amount;
	}

	public static void addTankTick(String bossKey, Player player) {
		Score s = score(bossKey, player);
		if (s != null) s.tankTicks += 1.0D;
	}

	public static void clear(String bossKey) {
		SESSIONS.remove(bossKey);
	}

	public static boolean hasContributors(String bossKey) {
		Map<UUID, Score> session = SESSIONS.get(bossKey);
		return session != null && !session.isEmpty();
	}

	public static Map<UUID, Double> computeShares(String bossKey) {
		Map<UUID, Double> result = new HashMap<>();
		Map<UUID, Score> session = SESSIONS.get(bossKey);
		if (session == null || session.isEmpty()) return result;

		double totalDamage = 0.0D;
		double totalBlocked = 0.0D;
		double totalHealed = 0.0D;
		double totalTank = 0.0D;

		for (Score s : session.values()) {
			totalDamage += s.damage;
			totalBlocked += s.blocked;
			totalHealed += s.healed;
			totalTank += s.tankTicks;
		}

		double activeWeight = 0.0D;
		if (totalDamage > 0.0D) activeWeight += WEIGHT_DAMAGE;
		if (totalBlocked > 0.0D) activeWeight += WEIGHT_BLOCKED;
		if (totalHealed > 0.0D) activeWeight += WEIGHT_HEALED;
		if (totalTank > 0.0D) activeWeight += WEIGHT_TANK;
		if (activeWeight <= 0.0D) return result;

		double normalize = 1.0D / activeWeight;

		for (Map.Entry<UUID, Score> entry : session.entrySet()) {
			Score s = entry.getValue();
			double points = 0.0D;

			if (totalDamage > 0.0D) points += WEIGHT_DAMAGE * normalize * (s.damage / totalDamage);
			if (totalBlocked > 0.0D) points += WEIGHT_BLOCKED * normalize * (s.blocked / totalBlocked);
			if (totalHealed > 0.0D) points += WEIGHT_HEALED * normalize * (s.healed / totalHealed);
			if (totalTank > 0.0D) points += WEIGHT_TANK * normalize * (s.tankTicks / totalTank);

			result.put(entry.getKey(), points);
		}
		return result;
	}

	public static Map<ServerPlayer, Double> resolvePayouts(ServerLevel level, String bossKey, double baseReward) {
		Map<ServerPlayer, Double> payouts = new HashMap<>();
		Map<UUID, Double> shares = computeShares(bossKey);
		if (shares.isEmpty()) return payouts;

		double best = 0.0D;
		for (double value : shares.values()) best = Math.max(best, value);
		if (best <= 0.0D) return payouts;

		for (Map.Entry<UUID, Double> entry : shares.entrySet()) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
			if (player == null) continue;

			double ratio = MIN_PAYOUT_RATIO + (1.0D - MIN_PAYOUT_RATIO) * (entry.getValue() / best);
			payouts.put(player, baseReward * ratio);
		}
		return payouts;
	}
}
