package com.dragonminez.server.world.worldboss;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WorldBossContribution {

	public enum DamageKind { MELEE, STRIKE, KI, OTHER }

	public enum MitigationKind { DEFENSE, BLOCK, SHIELD }

	public enum HealKind { SELF, ALLY }

	public static final class Score {
		private final UUID id;
		private String name = "";
		private int auraRgb = 0xFFFFFF;
		private final double[] damage = new double[DamageKind.values().length];
		private final double[] mitigated = new double[MitigationKind.values().length];
		private final Map<String, Double> received = new LinkedHashMap<>();
		private final double[] healed = new double[HealKind.values().length];

		private Score(UUID id) {
			this.id = id;
		}

		public UUID id() { return id; }
		public String name() { return name; }
		public int auraRgb() { return auraRgb; }
		public double damage(DamageKind kind) { return damage[kind.ordinal()]; }
		public double mitigated(MitigationKind kind) { return mitigated[kind.ordinal()]; }
		public double healed(HealKind kind) { return healed[kind.ordinal()]; }
		public Map<String, Double> received() { return received; }

		public double damageTotal() {
			double total = 0.0;
			for (double value : damage) total += value;
			return total;
		}

		public double mitigatedTotal() {
			double total = 0.0;
			for (double value : mitigated) total += value;
			return total;
		}

		public double receivedTotal() {
			double total = 0.0;
			for (double value : received.values()) total += value;
			return total;
		}

		public double healedTotal() {
			double total = 0.0;
			for (double value : healed) total += value;
			return total;
		}

		public double points() {
			GeneralServerConfig.WorldBossConfig config = ConfigManager.getServerConfig().getWorldBoss();
			return damageTotal() * config.getDamageWeight()
					+ mitigatedTotal() * config.getMitigatedWeight()
					+ receivedTotal() * config.getReceivedWeight()
					+ healedTotal() * config.getHealedWeight();
		}

		private Score copy() {
			Score copy = new Score(id);
			copy.name = name;
			copy.auraRgb = auraRgb;
			System.arraycopy(damage, 0, copy.damage, 0, damage.length);
			System.arraycopy(mitigated, 0, copy.mitigated, 0, mitigated.length);
			copy.received.putAll(received);
			System.arraycopy(healed, 0, copy.healed, 0, healed.length);
			return copy;
		}
	}

	private static final Map<String, Map<UUID, Score>> SESSIONS = new HashMap<>();
	private static final Set<String> DIRTY = new HashSet<>();

	private WorldBossContribution() {}

	private static Score score(String bossKey, ServerPlayer player) {
		if (bossKey == null || bossKey.isEmpty() || player == null) return null;
		Score score = SESSIONS.computeIfAbsent(bossKey, k -> new LinkedHashMap<>())
				.computeIfAbsent(player.getUUID(), Score::new);
		score.name = player.getGameProfile().getName();
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data != null && data.getStatus().isHasCreatedCharacter()) score.auraRgb = parseHex(data.getCharacter().getAuraColor());
		DIRTY.add(bossKey);
		return score;
	}

	public static void addDamage(String bossKey, ServerPlayer player, double amount, DamageKind kind) {
		if (amount <= 0.0 || !Double.isFinite(amount)) return;
		Score score = score(bossKey, player);
		if (score != null) score.damage[kind.ordinal()] += amount;
	}

	public static void addMitigated(String bossKey, ServerPlayer player, double amount, MitigationKind kind) {
		if (amount <= 0.0 || !Double.isFinite(amount)) return;
		Score score = score(bossKey, player);
		if (score != null) score.mitigated[kind.ordinal()] += amount;
	}

	public static void addReceived(String bossKey, ServerPlayer player, double amount, String attackerNameKey) {
		if (amount <= 0.0 || !Double.isFinite(amount)) return;
		Score score = score(bossKey, player);
		if (score != null) score.received.merge(attackerNameKey == null ? "" : attackerNameKey, amount, Double::sum);
	}

	public static void addHealed(String bossKey, ServerPlayer player, double amount, HealKind kind) {
		if (amount <= 0.0 || !Double.isFinite(amount)) return;
		Score score = score(bossKey, player);
		if (score != null) score.healed[kind.ordinal()] += amount;
	}

	public static void touch(String bossKey, ServerPlayer player) {
		score(bossKey, player);
	}

	public static void clear(String bossKey) {
		SESSIONS.remove(bossKey);
		DIRTY.remove(bossKey);
	}

	public static boolean hasContributors(String bossKey) {
		Map<UUID, Score> session = SESSIONS.get(bossKey);
		return session != null && !session.isEmpty();
	}

	public static boolean consumeDirty(String bossKey) {
		return DIRTY.remove(bossKey);
	}

	public static Score peek(String bossKey, UUID id) {
		Map<UUID, Score> session = SESSIONS.get(bossKey);
		return session == null ? null : session.get(id);
	}

	public static List<Score> snapshot(String bossKey) {
		Map<UUID, Score> session = SESSIONS.get(bossKey);
		List<Score> result = new ArrayList<>();
		if (session == null) return result;
		for (Score score : session.values()) result.add(score.copy());
		result.sort(Comparator.comparingDouble(Score::points).reversed());
		return result;
	}

	public static double totalPoints(List<Score> scores) {
		double total = 0.0;
		for (Score score : scores) total += score.points();
		return total;
	}

	private static int parseHex(String hex) {
		if (hex == null || hex.isEmpty()) return 0xFFFFFF;
		try {
			String value = hex.startsWith("#") ? hex.substring(1) : hex;
			return (int) (Long.parseLong(value, 16) & 0xFFFFFF);
		} catch (NumberFormatException e) {
			return 0xFFFFFF;
		}
	}
}
