package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class GeneralServerConfig {
	public static final int CURRENT_VERSION = 2;

	@Setter
	private int configVersion;

	private WorldGenConfig worldGen = new WorldGenConfig();
	private GameplayConfig gameplay = new GameplayConfig();
	private CombatConfig combat = new CombatConfig();
	private RacialSkillsConfig racialSkills = new RacialSkillsConfig();
	private StorageConfig storage = new StorageConfig();

	@Getter
	@NoArgsConstructor
	public static class WorldGenConfig {
		private Boolean generateCustomStructures = true;
		private Boolean generateDragonBalls = true;
		private Boolean otherworldActive = true;
		private Integer dbSpawnRange = 1000;

		public Integer getDBSpawnRange() {
			return Math.max(100, Math.min(dbSpawnRange, 6000));
		}
	}

	@Getter
	@NoArgsConstructor
	public static class GameplayConfig {
		private Boolean commandOutputOnConsole = true;
		private Integer reviveCooldownSeconds = 300;
		private Double tpGainMultiplier = 1.0;
		private Double tpCostMultiplier = 1.0;
		private Double tpHealthRatio = 0.10;
		private Integer tpPerHit = 2;
		private Double HTCTpMultiplier = 2.5;
		private Integer maxStatValue = 10000;
		private Boolean storyModeEnabled = true;
		private Boolean createDefaultSagas = true;
		private Integer senzuCooldownTicks = 240;
		private Map<String, Float[]> foodRegenerations = createDefaultFoodRegenerations();
		private Double mightFruitPower = 1.2;
		private Double majinPower = 1.3;
		private Double metamoruFusionThreshold = 0.5;
		private String[] fusionBoosts = {"STR", "SKP", "PWR"};
		private Integer fusionDurationSeconds = 900;
		private Integer fusionCooldownSeconds = 1800;
		private Boolean multiplicationInsteadOfAdditionForMultipliers = false;

		public Integer getReviveCooldownSeconds() {
			return Math.max(0, Math.min(reviveCooldownSeconds, Integer.MAX_VALUE));
		}

		public Double getTpsGainMultiplier() {
			return Math.max(0, Math.min(tpGainMultiplier, Double.MAX_VALUE));
		}

		public Double getTpCostMultiplier() {
			return Math.max(0.01, Math.min(tpCostMultiplier, Double.MAX_VALUE));
		}

		public Double getTpHealthRatio() {
			return Math.max(0, Math.min(tpHealthRatio, Double.MAX_VALUE));
		}

		public Integer getTpPerHit() {
			return Math.max(0, Math.min(tpPerHit, Integer.MAX_VALUE));
		}

		public Double getHTCTpMultiplier() {
			return Math.max(1.0, Math.min(HTCTpMultiplier, Double.MAX_VALUE));
		}

		public Integer getMaxStatValue() {
			return Math.max(1000, Math.min(maxStatValue, Integer.MAX_VALUE));
		}

		public Integer getSenzuCooldownTicks() {
			return Math.max(0, Math.min(senzuCooldownTicks, Integer.MAX_VALUE));
		}

		public Float[] getFoodRegeneration(String itemId) {
			return foodRegenerations.getOrDefault(itemId, new Float[]{0.0f, 0.0f, 0.0f});
		}

		public Double getMightFruitPower() {
			return Math.max(0, Math.min(mightFruitPower, Double.MAX_VALUE));
		}

		public Double getMajinPower() {
			return Math.max(0, Math.min(majinPower, Double.MAX_VALUE));
		}

		public Double getMetamoruFusionThreshold() {
			return Math.max(0, Math.min(metamoruFusionThreshold, Double.MAX_VALUE));
		}

		public Integer getFusionDurationSeconds() {
			return Math.max(0, Math.min(fusionDurationSeconds, Integer.MAX_VALUE));
		}

		public Integer getFusionCooldownSeconds() {
			return Math.max(0, Math.min(fusionCooldownSeconds, Integer.MAX_VALUE));
		}

		private static Map<String, Float[]> createDefaultFoodRegenerations() {
			Map<String, Float[]> defaults = new HashMap<>();
			defaults.put("dragonminez:raw_dino_meat", new Float[]{0.10f, 0.10f, 0.10f});
			defaults.put("dragonminez:cooked_dino_meat", new Float[]{0.15f, 0.15f, 0.15f});
			defaults.put("dragonminez:dino_tail_raw", new Float[]{0.15f, 0.15f, 0.15f});
			defaults.put("dragonminez:dino_tail_cooked", new Float[]{0.20f, 0.20f, 0.20f});
			defaults.put("dragonminez:frog_legs_raw", new Float[]{0.05f, 0.05f, 0.05f});
			defaults.put("dragonminez:frog_legs_cooked", new Float[]{0.10f, 0.10f, 0.10f});
			defaults.put("dragonminez:senzu_bean", new Float[]{1.0f, 1.0f, 1.0f});
			defaults.put("dragonminez:heart_medicine", new Float[]{1.0f, 1.0f, 1.0f});
			defaults.put("dragonminez:might_tree_fruit", new Float[]{0.035f, 0.35f, 0.35f});
			return defaults;
		}
	}

	@Getter
	@NoArgsConstructor
	public static class CombatConfig {
		private Boolean killPlayersOnCombatLogout = true;
		private Double staminaConsumptionRatio = 0.125;
		private Integer baselineFormDrain = 200;
		private Boolean respectAttackCooldown = true;
		private Boolean enableBlocking = true;
		private Boolean enableParrying = true;
		private Boolean enableComboAttacks = true;
		private Integer comboAttacksCooldownSeconds = 8;
		private Boolean enablePerfectEvasion = true;
		private Integer parryWindowMs = 150;
		private Double blockDamageReductionCap = 0.80;
		private Double blockDamageReductionMin = 0.40;
		private Double poiseDamageMultiplier = 0.25;
		private Integer poiseRegenCooldown = 100;
		private Integer blockBreakStunDurationTicks = 60;
		private Integer perfectEvasionWindowMs = 150;
		private Integer dashCooldownSeconds = 4;
		private Integer doubleDashCooldownSeconds = 12;
		private Double[] kiBladeConfig = {1.0, 0.05};
		private Double[] kiScytheConfig = {1.5, 0.075};
		private Double[] kiClawLanceConfig = {2.0, 0.125};

		public Double getStaminaConsumptionRatio() {
			return Math.max(0, Math.min(staminaConsumptionRatio, Double.MAX_VALUE));
		}

		public Integer getComboAttacksCooldownSeconds() {
			return Math.max(0, Math.min(comboAttacksCooldownSeconds, Integer.MAX_VALUE));
		}

		public Integer getParryWindowMs() {
			return Math.max(0, Math.min(parryWindowMs, Integer.MAX_VALUE));
		}

		public Double getBlockDamageReductionCap() {
			return Math.max(0, Math.min(blockDamageReductionCap, Double.MAX_VALUE));
		}

		public Double getBlockDamageReductionMin() {
			return Math.max(0, Math.min(blockDamageReductionMin, Double.MAX_VALUE));
		}

		public Double getPoiseDamageMultiplier() {
			return Math.max(0, Math.min(poiseDamageMultiplier, Double.MAX_VALUE));
		}

		public Integer getPoiseRegenCooldown() {
			return Math.max(0, Math.min(poiseRegenCooldown, Integer.MAX_VALUE));
		}

		public Integer getBlockBreakStunDurationTicks() {
			return Math.max(0, Math.min(blockBreakStunDurationTicks, Integer.MAX_VALUE));
		}

		public Integer getPerfectEvasionWindowMs() {
			return Math.max(0, Math.min(perfectEvasionWindowMs, Integer.MAX_VALUE));
		}

		public Integer getDashCooldownSeconds() {
			return Math.max(0, Math.min(dashCooldownSeconds, Integer.MAX_VALUE));
		}

		public Integer getDoubleDashCooldownSeconds() {
			return Math.max(0, Math.min(doubleDashCooldownSeconds, Integer.MAX_VALUE));
		}

		public Double[] getKiBladeConfig() {
			return new Double[]{Math.max(0, Math.min(kiBladeConfig[0], Double.MAX_VALUE)), Math.max(0, Math.min(kiBladeConfig[1], Double.MAX_VALUE))};
		}

		public Double[] getKiScytheConfig() {
			return new Double[]{Math.max(0, Math.min(kiScytheConfig[0], Double.MAX_VALUE)), Math.max(0, Math.min(kiScytheConfig[1], Double.MAX_VALUE))};
		}

		public Double[] getKiClawLanceConfig() {
			return new Double[]{Math.max(0, Math.min(kiClawLanceConfig[0], Double.MAX_VALUE)), Math.max(0, Math.min(kiClawLanceConfig[1], Double.MAX_VALUE))};
		}
	}

	@Getter
	@NoArgsConstructor
	public static class RacialSkillsConfig {
		private Boolean enableRacialSkills = true;
		private Boolean humanRacialSkill = true;
		private Double humanKiRegenBoost = 1.40;
		private Boolean saiyanRacialSkill = true;
		private Integer saiyanZenkaiAmount = 3;
		private Double saiyanZenkaiHealthRegen = 0.20;
		private Double saiyanZenkaiStatBoost = 0.10;
		private String[] saiyanZenkaiBoosts = {"STR", "SKP", "PWR"};
		private Integer saiyanZenkaiCooldownSeconds = 900;
		private Boolean namekianRacialSkill = true;
		private Integer namekianAssimilationAmount = 4;
		private Double namekianAssimilationHealthRegen = 0.35;
		private Double namekianAssimilationStatBoost = 0.15;
		private String[] namekianAssimilationBoosts = {"STR", "SKP", "PWR"};
		private Boolean namekianAssimilationOnNamekNpcs = true;
		private Boolean frostDemonRacialSkill = true;
		private Double frostDemonTPBoost = 1.25;
		private Boolean bioAndroidRacialSkill = true;
		private Integer bioAndroidCooldownSeconds = 180;
		private Double bioAndroidDrainRatio = 0.25;
		private Boolean majinAbsoprtionSkill = true;
		private Boolean majinReviveSkill = true;
		private Integer majinAbsorptionAmount = 3;
		private Double majinAbsorptionHealthRegen = 0.30;
		private Double majinAbsorptionStatsCopy = 0.10;
		private String[] majinAbsorptionBoosts = {"STR", "SKP", "PWR"};
		private Boolean majinAbsorptionOnMobs = true;
		private Integer majinReviveCooldownSeconds = 3600;
		private Double majinReviveHealthRatioPerBlop = 0.25;

		public Double getHumanKiRegenBoost() {
			return Math.max(0, Math.min(humanKiRegenBoost, Double.MAX_VALUE));
		}

		public Integer getSaiyanZenkaiAmount() {
			return Math.max(0, Math.min(saiyanZenkaiAmount, Integer.MAX_VALUE));
		}

		public Double getSaiyanZenkaiHealthRegen() {
			return Math.max(0, Math.min(saiyanZenkaiHealthRegen, Double.MAX_VALUE));
		}

		public Double getSaiyanZenkaiStatBoost() {
			return Math.max(0, Math.min(saiyanZenkaiStatBoost, Double.MAX_VALUE));
		}

		public Integer getSaiyanZenkaiCooldownSeconds() {
			return Math.max(0, Math.min(saiyanZenkaiCooldownSeconds, Integer.MAX_VALUE));
		}

		public Integer getNamekianAssimilationAmount() {
			return Math.max(0, Math.min(namekianAssimilationAmount, Integer.MAX_VALUE));
		}

		public Double getNamekianAssimilationHealthRegen() {
			return Math.max(0, Math.min(namekianAssimilationHealthRegen, Double.MAX_VALUE));
		}

		public Double getNamekianAssimilationStatBoost() {
			return Math.max(0, Math.min(namekianAssimilationStatBoost, Double.MAX_VALUE));
		}

		public Double getFrostDemonTPBoost() {
			return Math.max(1, Math.min(frostDemonTPBoost, Double.MAX_VALUE));
		}

		public Integer getBioAndroidCooldownSeconds() {
			return Math.max(0, Math.min(bioAndroidCooldownSeconds, Integer.MAX_VALUE));
		}

		public Double getBioAndroidDrainRatio() {
			return Math.max(0, Math.min(bioAndroidDrainRatio, Double.MAX_VALUE));
		}

		public Integer getMajinAbsorptionAmount() {
			return Math.max(0, Math.min(majinAbsorptionAmount, Integer.MAX_VALUE));
		}

		public Double getMajinAbsorptionHealthRegen() {
			return Math.max(0, Math.min(majinAbsorptionHealthRegen, Double.MAX_VALUE));
		}

		public Double getMajinAbsorptionStatCopy() {
			return Math.max(0, Math.min(majinAbsorptionStatsCopy, Double.MAX_VALUE));
		}

		public Integer getMajinReviveCooldownSeconds() {
			return Math.max(0, Math.min(majinReviveCooldownSeconds, Integer.MAX_VALUE));
		}

		public Double getMajinReviveHealthRatioPerBlop() {
			return Math.max(0, Math.min(majinReviveHealthRatioPerBlop, Double.MAX_VALUE));
		}
	}

	@Getter
	@NoArgsConstructor
	public static class StorageConfig {
		public enum StorageType {
			NBT, JSON, DATABASE
		}

		private StorageType storageType = StorageType.NBT;
		private String host = "localhost";
		private Integer port = 3306;
		private String database = "dragonminez";
		private String table = "player_data";
		private String username = "root";
		private String password = "password";

		private Integer poolSize = 10;
		private Integer threadPoolSize = 4;

		public Integer getPoolSize() {
			return Math.max(1, poolSize);
		}

		public Integer getThreadPoolSize() {
			return Math.max(1, threadPoolSize);
		}
	}
}