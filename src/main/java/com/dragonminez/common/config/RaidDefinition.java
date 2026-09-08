package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RaidDefinition {

	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;
	public static final String DEFAULT_PREPARATION_MUSIC = "";
	public static final String DEFAULT_BATTLE_MUSIC = "dragonminez:raid_ost_1";

	private String configVersion;

	private Boolean enabled;
	private String displayName;

	private Double activationRadius;
	private Double leashDistance;
	private Integer interWaveDelaySeconds;

	private Music music;
	private Trigger trigger;

	private List<Wave> waves = new ArrayList<>();
	private Rewards rewards;

	public boolean isEnabled() { return enabled == null || enabled; }

	public String displayNameOr(String fallbackKey) {
		return displayName != null && !displayName.isBlank() ? displayName : fallbackKey;
	}

	public double activationRadiusOr(double fallback) {
		return activationRadius != null ? Math.max(1.0D, activationRadius) : fallback;
	}

	public double leashDistanceOr(double fallback) {
		return leashDistance != null ? Math.max(1.0D, leashDistance) : fallback;
	}

	public int interWaveDelayTicksOr(int fallbackTicks) {
		return interWaveDelaySeconds != null ? Math.max(0, interWaveDelaySeconds) * 20 : fallbackTicks;
	}

	public String preparationMusic() {
		return music != null && music.preparation != null ? music.preparation : DEFAULT_PREPARATION_MUSIC;
	}

	public String battleMusic() {
		return music != null && music.battle != null ? music.battle : DEFAULT_BATTLE_MUSIC;
	}

	public boolean hasTrigger() {
		return trigger != null && trigger.entityId != null && !trigger.entityId.isBlank();
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class Music {
		private String preparation;
		private String battle;
	}
	@Getter
	@Setter
	@NoArgsConstructor
	public static class Trigger {
		private String entityId;
		private String dimension;
		private Double spawnChance;
		private Boolean supervillain;
		private Integer preparationSeconds;
		private String omenMessage;
		private String startMessage;

		public double spawnChanceOr(double fallback) {
			return spawnChance != null ? Math.max(0.0D, Math.min(1.0D, spawnChance)) : fallback;
		}

		public boolean isSupervillain() { return supervillain == null || supervillain; }

		public int preparationTicksOr(int fallbackTicks) {
			return preparationSeconds != null ? Math.max(1, preparationSeconds) * 20 : fallbackTicks;
		}
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class Wave {
		private List<Spawn> spawns = new ArrayList<>();
		public int totalMobCount() {
			if (spawns == null) return 0;
			int total = 0;
			for (Spawn spawn : spawns) total += spawn.countOr(1);
			return total;
		}
		public boolean isBossWave() {
			if (spawns == null) return false;
			for (Spawn spawn : spawns) if (spawn.isDormantUntilEscortDead()) return true;
			return false;
		}
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class Spawn {
		private String entityId;

		private Integer count;
		private Double health;
		private Double meleeDamage;
		private Double kiDamage;
		private Integer aiTier;
		private Integer textureVariant;
		private Boolean canTransform;
		private Boolean dormantUntilEscortDead;

		public int countOr(int fallback) { return count != null ? Math.max(0, count) : fallback; }

		public boolean hasAbsoluteStats() { return health != null; }

		public double healthOr(double fallback) { return health != null ? Math.max(1.0D, health) : fallback; }
		public double meleeDamageOr(double fallback) { return meleeDamage != null ? Math.max(0.0D, meleeDamage) : fallback; }
		public double kiDamageOr(double fallback) { return kiDamage != null ? Math.max(0.0D, kiDamage) : fallback; }

		public int aiTierOr(int fallback) { return aiTier != null ? aiTier : fallback; }
		public boolean canTransformOr(boolean fallback) { return canTransform != null ? canTransform : fallback; }
		public boolean isDormantUntilEscortDead() { return dormantUntilEscortDead != null && dormantUntilEscortDead; }
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class Rewards {
		private Float trainingPoints;
		private List<EffectReward> effects = new ArrayList<>();
		private List<ItemReward> items = new ArrayList<>();

		public float trainingPointsOr(float fallback) {
			return trainingPoints != null ? Math.max(0.0F, trainingPoints) : fallback;
		}
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class EffectReward {
		private String id;
		private Integer seconds;
		private Integer amplifier;

		public int durationTicksOr(int fallbackTicks) {
			return seconds != null ? Math.max(1, seconds) * 20 : fallbackTicks;
		}

		public int amplifierOr(int fallback) { return amplifier != null ? Math.max(0, amplifier) : fallback; }
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class ItemReward {
		private String id;
		private Integer count;

		public int countOr(int fallback) { return count != null ? Math.max(1, count) : fallback; }
	}
}
