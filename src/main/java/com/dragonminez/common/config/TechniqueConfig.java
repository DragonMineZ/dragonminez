package com.dragonminez.common.config;

import com.dragonminez.common.diagnostics.RemovedConfigKeys;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class TechniqueConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	@Setter
	private String configVersion;

	@SerializedName("KiAttacks")
	private final Map<String, TechniqueTypeConfig> kiAttacks = new HashMap<>();
	@SerializedName("StrikeAttacks")
	private final Map<String, StrikeAttackConfig> strikeAttacks = new HashMap<>();
	@SerializedName("EvasionAttacks")
	private final Map<String, EvasionAttackConfig> evasionAttacks = new HashMap<>();

	public TechniqueConfig() {
		createDefaults();
	}

	private void createDefaults() {
		for (KiAttackData.KiType type : KiAttackData.KiType.values()) {
			TechniqueTypeConfig cfg = TechniqueTypeConfig.defaults();
			cfg.setCastTimeTicks(defaultCastTimeTicks(type));
			cfg.setBaseSizeCap(defaultBaseSizeCap(type));
			cfg.setTransformedSizeCap(defaultTransformedSizeCap(type));
			cfg.setMaxDestructionRadius(defaultMaxDestructionRadius(type));
			kiAttacks.put(type.name().toLowerCase(), cfg);
		}
		for (String strikeId : PredefinedTechniques.STRIKE_IDS) {
			StrikeAttackConfig cfg = StrikeAttackConfig.defaults();
			cfg.setCooldownTicks(defaultStrikeCooldownTicks(strikeId));
			strikeAttacks.put(strikeId, cfg);
		}
		for (String evasionId : PredefinedTechniques.EVASION_IDS) {
			EvasionAttackConfig cfg = EvasionAttackConfig.defaults();
			cfg.setCooldownTicks(defaultEvasionCooldownTicks(evasionId));
			evasionAttacks.put(evasionId, cfg);
		}
	}

	public boolean addMissingDefaults() {
		TechniqueConfig defaults = new TechniqueConfig();
		boolean changed = false;
		for (Map.Entry<String, TechniqueTypeConfig> entry : defaults.kiAttacks.entrySet())
			changed |= kiAttacks.putIfAbsent(entry.getKey(), entry.getValue()) == null;
		for (Map.Entry<String, StrikeAttackConfig> entry : defaults.strikeAttacks.entrySet())
			changed |= strikeAttacks.putIfAbsent(entry.getKey(), entry.getValue()) == null;
		for (Map.Entry<String, EvasionAttackConfig> entry : defaults.evasionAttacks.entrySet())
			changed |= evasionAttacks.putIfAbsent(entry.getKey(), entry.getValue()) == null;
		return changed;
	}

	private static int defaultEvasionCooldownTicks(String evasionId) {
		return switch (evasionId) {
			case "taiyoken" -> 900;
			case "sleep_recovery" -> 1200;
			case "rage_scream" -> 80;
			case "afterimage" -> 200;
			case "candy_beam" -> 600;
			case "gum_bind" -> 900;
			default -> 80;
		};
	}

	private static int defaultStrikeCooldownTicks(String strikeId) {
		return switch (strikeId) {
			case "meteor", "deadly_dance" -> 160;
			case "wolf_fang" -> 140;
			case "kaioken_attack" -> 180;
			case "deadly_dance_vegetto" -> 200;
			case "oozaru_fist" -> 280;
			case "super_god_fist" -> 300;
			case "dragon_fist" -> 320;
			case "oozaru_slam" -> 180;
			case "grab" -> 210;
			case "gum_punch", "blue_hurricane" -> 260;
			case "combo_meteor" -> 290;
			case "shining_sword_attack", "mystic_ball", "death_chaser" -> 320;
			case "spirit_breaking_cannon", "perfect_rush" -> 330;
			case "dimensional_punch" -> 340;
			case "beam_sword_slash" -> 350;
			case "spirit_breaker" -> 370;
			case "dimensional_sword_attack", "android_combo" -> 380;
			case "darkness_eye_beam" -> 410;
			case "meteor_burst" -> 600;
			default -> 160;
		};
	}

	/** 0 means "do not scale this type with ki power" -- lasers keep their authored size. */
	private static double defaultBaseSizeCap(KiAttackData.KiType type) {
		return switch (type) {
			case GIANT_BALL -> 15.0;
			case EXPLOSION -> 12.0;
			case MEDIUM_BALL -> 4.5;
			case WAVE -> 2.0;
			case BEAM, DISK -> 1.2;
			case BARRAGE -> 1.5;
			default -> 0.0;
		};
	}

	private static double defaultTransformedSizeCap(KiAttackData.KiType type) {
		return switch (type) {
			case GIANT_BALL, EXPLOSION -> 25.0;
			case MEDIUM_BALL -> 7.0;
			case WAVE -> 5.0;
			case BEAM, DISK -> 2.3;
			case BARRAGE -> 2.5;
			default -> 0.0;
		};
	}

	private static double defaultMaxDestructionRadius(KiAttackData.KiType type) {
		return switch (type) {
			case GIANT_BALL, EXPLOSION -> 32.0;
			case WAVE, BEAM, AREA -> 12.0;
			default -> 6.0;
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
		if (config != null) return config;
		StrikeAttackConfig fallback = StrikeAttackConfig.defaults();
		fallback.setCooldownTicks(defaultStrikeCooldownTicks(strikeId.toLowerCase()));
		return fallback;
	}

	public EvasionAttackConfig getEvasionConfig(String evasionId) {
		if (evasionId == null || evasionId.isEmpty()) return EvasionAttackConfig.defaults();
		EvasionAttackConfig config = evasionAttacks.get(evasionId.toLowerCase());
		if (config != null) return config;
		EvasionAttackConfig fallback = EvasionAttackConfig.defaults();
		fallback.setCooldownTicks(defaultEvasionCooldownTicks(evasionId.toLowerCase()));
		return fallback;
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
		/** Size ceiling in base form, before the technique's own XP upgrades are added on top. */
		private double baseSizeCap = 0.0;
		/** Size ceiling while transformed, or stacking a form such as Kaioken. */
		private double transformedSizeCap = 0.0;
		/** Ki power at which an attack sits at its authored size, and at which it hits the cap. */
		private double sizePowerFloor = 100.0;
		private double sizePowerCeiling = 1000000.0;
		private double maxDestructionRadius = 12.0;

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

	@Getter
	@Setter
	@RemovedConfigKeys({"xpGainPerKill"})
	public static class EvasionAttackConfig {
		private int minXPCost = 100;
		private int maxXPCost = -1;
		private double xpCostMultiplier = 1.0;
		private double xpGainMultiplier = 1.0;
		private int xpGainPerHit = 2;
		private double kiCostMultiplier = 1.0;
		private double damageMultiplier = 1.0;
		private int cooldownTicks = 80;

		public static EvasionAttackConfig defaults() {
			return new EvasionAttackConfig();
		}
	}
}
