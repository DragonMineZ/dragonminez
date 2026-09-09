package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class TournamentDefinition {

	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	public static final int CONTENDER_COUNT = 1;

	public enum Format {
		BRACKET,
		GAUNTLET
	}

	private String configVersion;

	private Boolean enabled;
	private String displayName;
	private Format format;
	private Integer difficultyStars;

	private Integer qualifierSlots;
	private Integer reentryCooldownSeconds;
	private Integer victoryCooldownSeconds;
	private Integer matchTimeoutSeconds;

	private RingOffset ring;
	private Fighter champion;
	private Fighter semifinalist;
	private List<Fighter> contenders = new ArrayList<>();

	private Rewards rewards;

	public boolean isEnabled() {
		return enabled == null || enabled;
	}

	public Format formatOr(Format fallback) {
		return format != null ? format : fallback;
	}

	public String displayNameOr(String fallbackKey) {
		return displayName != null && !displayName.isBlank() ? displayName : fallbackKey;
	}

	public int difficultyStarsOr(int fallback) {
		return difficultyStars != null ? Math.max(1, Math.min(5, difficultyStars)) : fallback;
	}

	public int qualifierSlotsOr(int fallback) {
		int value = qualifierSlots != null ? qualifierSlots : fallback;
		value = Math.max(2, Math.min(16, value));
		return Integer.highestOneBit(value);
	}

	public int reentryCooldownSecondsOr(int fallback) {
		return reentryCooldownSeconds != null ? Math.max(0, reentryCooldownSeconds) : fallback;
	}

	public int victoryCooldownSecondsOr(int fallback) {
		if (victoryCooldownSeconds != null) return Math.max(0, victoryCooldownSeconds);
		return reentryCooldownSecondsOr(fallback);
	}

	public int matchTimeoutSecondsOr(int fallback) {
		return matchTimeoutSeconds != null ? Math.max(30, matchTimeoutSeconds) : fallback;
	}

	public boolean isUsable() {
		return champion != null && champion.isUsable()
				&& semifinalist != null && semifinalist.isUsable()
				&& contenders != null && contenders.size() >= CONTENDER_COUNT
				&& contenders.stream().allMatch(Fighter::isUsable);
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class RingOffset {
		private Integer x;
		private Integer y;
		private Integer z;
		private Integer radius;

		public int x() { return x != null ? x : 0; }
		public int y() { return y != null ? y : 0; }
		public int z() { return z != null ? z : 0; }
		public int radius() { return radius != null ? Math.max(1, radius) : 4; }

		public boolean isExplicit() {
			return x != null || y != null || z != null;
		}
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class Fighter {
		private String entityId;
		private String displayName;

		private Double health;
		private Double meleeDamage;
		private Double kiDamage;
		private Integer aiTier;
		private Double perRoundScaling;

		public boolean isUsable() {
			return entityId != null && !entityId.isBlank();
		}

		public double healthOr(double fallback) { return health != null ? Math.max(1.0D, health) : fallback; }
		public double meleeDamageOr(double fallback) { return meleeDamage != null ? Math.max(0.0D, meleeDamage) : fallback; }
		public double kiDamageOr(double fallback) { return kiDamage != null ? Math.max(0.0D, kiDamage) : fallback; }
		public int aiTierOr(int fallback) { return aiTier != null ? Math.max(0, aiTier) : fallback; }
		public double perRoundScalingOr(double fallback) {
			return perRoundScaling != null ? Math.max(1.0D, perRoundScaling) : fallback;
		}
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class Rewards {
		private Integer trainingPoints;
		private Integer alignment;
		private List<String> commands = new ArrayList<>();

		public int trainingPointsOr(int fallback) {
			return trainingPoints != null ? Math.max(0, trainingPoints) : fallback;
		}

		public int alignmentOr(int fallback) {
			return alignment != null ? alignment : fallback;
		}
	}


	public static final class Defaults {

		public static final String BABA = "baba";

		private Defaults() {}

		public static Map<String, TournamentDefinition> create() {
			Map<String, TournamentDefinition> defaults = new LinkedHashMap<>();
			defaults.put(BABA, baba());
			return defaults;
		}

		private static TournamentDefinition baba() {
			TournamentDefinition def = new TournamentDefinition();
			def.setConfigVersion(CURRENT_VERSION);
			def.setDisplayName("tournament.dragonminez.baba");
			def.setEnabled(true);
			def.setDifficultyStars(1);
			def.setFormat(TournamentDefinition.Format.GAUNTLET);
			def.setReentryCooldownSeconds(20 * 60);
			def.setMatchTimeoutSeconds(300);

			TournamentDefinition.RingOffset ring = new TournamentDefinition.RingOffset();
			ring.setX(70);
			ring.setY(4);
			ring.setZ(61);
			ring.setRadius(4);
			def.setRing(ring);

			def.setChampion(fighter("dragonminez:saga_kid_goku", "entity.dragonminez.saga_kid_goku",
					400.0D, 34.0D, 30.0D, 3, 1.0D));
			def.setSemifinalist(fighter("dragonminez:saga_masked_warrior", "entity.dragonminez.saga_masked_warrior",
					280.0D, 26.0D, 20.0D, 2, 1.0D));

			def.setContenders(List.of(
					fighter("dragonminez:saga_dracula", "entity.dragonminez.saga_dracula",
							130.0D, 13.0D, 8.0D, 1, 1.1D),
					fighter("dragonminez:saga_invisible_man", "entity.dragonminez.saga_invisible_man",
							140.0D, 14.0D, 10.0D, 1, 1.1D),
					fighter("dragonminez:saga_akkuman", "entity.dragonminez.saga_akkuman",
							155.0D, 15.0D, 14.0D, 1, 1.1D),
					fighter("dragonminez:saga_mummy", "entity.dragonminez.saga_mummy",
							175.0D, 17.0D, 6.0D, 1, 1.1D)
			));

			TournamentDefinition.Rewards rewards = new TournamentDefinition.Rewards();
			rewards.setTrainingPoints(10000);
			rewards.setAlignment(5);
			def.setRewards(rewards);

			return def;
		}

		private static TournamentDefinition.Fighter fighter(String entityId, String displayName,
															double health, double melee, double ki,
															int aiTier, double perRoundScaling) {
			TournamentDefinition.Fighter f = new TournamentDefinition.Fighter();
			f.setEntityId(entityId);
			f.setDisplayName(displayName);
			f.setHealth(health);
			f.setMeleeDamage(melee);
			f.setKiDamage(ki);
			f.setAiTier(aiTier);
			f.setPerRoundScaling(perRoundScaling);
			return f;
		}
	}
}
