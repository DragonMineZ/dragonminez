package com.dragonminez.common.config;

import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class TechniqueConfig {
	public static final double CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	@Setter
	private double configVersion;

	@SerializedName("KiAttacks")
	private final Map<String, TechniqueTypeConfig> kiAttacks = new HashMap<>();
	@SerializedName("StrikeAttacks")
	private final Map<String, StrikeAttackConfig> strikeAttacks = new HashMap<>();

	public TechniqueConfig() {
		createDefaults();
	}

	private void createDefaults() {
		for (KiAttackData.KiType type : KiAttackData.KiType.values()) {
			TechniqueTypeConfig cfg = TechniqueTypeConfig.defaults();
			cfg.setCastTimeTicks(defaultCastTimeTicks(type));
			kiAttacks.put(type.name().toLowerCase(), cfg);
		}
		for (String strikeId : PredefinedTechniques.STRIKE_IDS) {
			StrikeAttackConfig cfg = StrikeAttackConfig.defaults();
			cfg.setCooldownTicks(defaultStrikeCooldownTicks(strikeId));
			strikeAttacks.put(strikeId, cfg);
		}
	}

	private static int defaultStrikeCooldownTicks(String strikeId) {
		return switch (strikeId) {
			case "dragon_fist" -> 320;
			case "oozaru_fist" -> 280;
			case "super_god_fist", "kaioken_attack" -> 240;
			case "deadly_dance_vegetto" -> 200;
			case "wolf_fang" -> 140;
			default -> 160;
		};
	}

	private static int defaultCastTimeTicks(KiAttackData.KiType type) {
		return switch (type) {
			case SMALL_BALL, LASER -> 0;
			case MEDIUM_BALL, DISK -> 30;
			case BARRAGE, SHIELD, AREA -> 40;
			case WAVE, BEAM -> 50;
			case GIANT_BALL, EXPLOSION -> 60;
		};
	}

	public TechniqueTypeConfig getKiTypeConfig(KiAttackData.KiType type) {
		if (type == null) return TechniqueTypeConfig.defaults();
		TechniqueTypeConfig config = kiAttacks.get(type.name().toLowerCase());
		return config != null ? config : TechniqueTypeConfig.defaults();
	}

	public StrikeAttackConfig getStrikeConfig(String strikeId) {
		if (strikeId == null || strikeId.isEmpty()) return StrikeAttackConfig.defaults();
		StrikeAttackConfig config = strikeAttacks.get(strikeId.toLowerCase());
		return config != null ? config : StrikeAttackConfig.defaults();
	}

	@Getter
	@Setter
	public static class TechniqueTypeConfig {
		private int minXPCost = 100;
		private int maxXPCost = -1;
		private double xpCostMultiplier = 1.0;
		private double xpGainMultiplier = 1.0;
		private int xpGainPerHit = 1;
		private int xpGainPerKill = 3;
		private double kiCostMultiplier = 1.0;
		private double damageMultiplier = 1.0;
		private double destructionMultiplier = 1.0;
		private int castTimeTicks = 30;

		public static TechniqueTypeConfig defaults() {
			return new TechniqueTypeConfig();
		}
	}

	@Getter
	@Setter
	public static class StrikeAttackConfig {
		private int minXPCost = 100;
		private int maxXPCost = -1;
		private double xpCostMultiplier = 1.0;
		private double xpGainMultiplier = 1.0;
		private int xpGainPerHit = 1;
		private int xpGainPerKill = 3;
		private double kiCostMultiplier = 1.0;
		private double damageMultiplier = 1.0;
		private int castTimeTicks = 0;
		private int cooldownTicks = 80;

		public static StrikeAttackConfig defaults() {
			return new StrikeAttackConfig();
		}
	}
}
