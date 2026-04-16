package com.dragonminez.common.config;

import com.dragonminez.common.init.item.CapsuleType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class GeneralServerConfig {
	public static final int CURRENT_VERSION = 5;

	@Setter
	private int configVersion;

	private WorldGenConfig worldGen = new WorldGenConfig();
	private GameplayConfig gameplay = new GameplayConfig();
	private RacialSkillsConfig racialSkills = new RacialSkillsConfig();
	private StorageConfig storage = new StorageConfig();

	@Getter
	@NoArgsConstructor
	public static class WorldGenConfig {
		private Boolean generateCustomStructures = true;
		private Boolean generateDragonBalls = true;
		private Boolean otherworldActive = true;
		private Integer dbSpawnRange = 1000;
		private Integer dragonBallSets = 1;

		public Integer getDBSpawnRange() {
			return Math.max(100, Math.min(dbSpawnRange, 6000));
		}

		public Integer getDragonBallSets() {
			return Math.max(0, Math.min(dragonBallSets, 10));
		}
	}

	@Getter
	@NoArgsConstructor
	public static class GameplayConfig {
		private Boolean forceCharacterCreation = true;
		private Boolean commandOutputOnConsole = true;
		private Integer reviveCooldownSeconds = 300;
		private Double tpGainMultiplier = 1.0;
		private Double globalTPCostMultiplier = 1.0;
		private Integer minTPCost = 16;
		private Integer maxTPDiscount = 140;
		private Double tpHealthRatio = 0.10;
		private Integer tpPerHit = 2;
		private Double HTCTpMultiplier = 2.5;
		private Boolean maxLevelValueInsteadOfStats = true;
		private Integer maxValue = 10000;
		private CapsulesConfig capsules = new CapsulesConfig();
		private Boolean storyModeEnabled = true;
		private Boolean createDefaultSagas = true;
		private Boolean sideQuestsEnabled = true;
		private Boolean createDefaultSideQuests = true;
		private Double defaultQuestPartyMultiplier = 1.45;
		private Integer senzuCooldownTicks = 240;
		private Integer senzuGiftCooldownTicks = 18000;
		private Integer senzuGiftAmount = 5;
		private FoodConfig food = new FoodConfig();
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

		public Boolean getForceCharacterCreation() {
			return forceCharacterCreation != null ? forceCharacterCreation : true;
		}

		public Double getTpsGainMultiplier() {
			return Math.max(0, Math.min(tpGainMultiplier, Double.MAX_VALUE));
		}

		public Double getGlobalTpCostMultiplier() {
			return Math.max(0.01, Math.min(globalTPCostMultiplier, Double.MAX_VALUE));
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

		public Integer getMaxValue() {
			return Math.max(1000, Math.min(maxValue, Integer.MAX_VALUE));
		}

		public Boolean getMaxLevelValueInsteadOfStats() {
			return maxLevelValueInsteadOfStats != null ? maxLevelValueInsteadOfStats : true;
		}

		public Integer getSenzuCooldownTicks() {
			return Math.max(0, Math.min(senzuCooldownTicks, Integer.MAX_VALUE));
		}

		public Double getDefaultQuestPartyMultiplier() {
			return Math.max(1.0, Math.min(defaultQuestPartyMultiplier, 5.0));
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
	public static class CapsulesConfig {
		private String statSeparator = ", ";
		private Map<CapsuleType, CapsuleValues> values = new HashMap<>();

		public CapsulesConfig() {
			Arrays.stream(CapsuleType.values()).forEach(type -> {
				values.put(
						type,
						new CapsuleValues(
								type.getStatName(),
								5
						)
				);
			});
		}

		public CapsuleValues getCapsuleValues(CapsuleType type) {
			return values.getOrDefault(
					type,
					new CapsuleValues(
							type.getStatName(),
							type.getStatPoints()
					)
			);
		}
	}

	@Getter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class CapsuleValues {
		private String stats;
		private Integer points;
	}

	@Getter
	@NoArgsConstructor
	public static class FoodConfig {
		// Lower and upper bounds on hunger points provided by
		// the food item, for it to recover health, ki and stamina;
		// Food points above the upper bound do not contribute
		// to recovery values;
		// Food points below the lower bound do not contribute
		// to recovery values;
		private Integer minHungerPoints = 4;
		private Integer maxHungerPoints = 20;

		// Lower and upper bounds on saturation points provided by
		// the food item, for it to recover health, ki and stamina;
		// Saturation points above the upper bound do not contribute
		// to recovery values;
		// Saturation points below the lower bound do not contribute
		// to recovery values;
		private Float minSaturationPoints = 0.4f;
		private Float maxSaturationPoints = 2.0f;

		// Health, ki and stamina percentage recovered by
		// consumed food, per hunger point provided
		private Float healthPercentageRecoveredPerHungerPoint = 1.0f;
		private Float kiPercentageRecoveredPerHungerPoint = 1.0f;
		private Float staminaPercentageRecoveredPerHungerPoint = 1.0f;

		// Health, ki and stamina percentage recovered by
		// consumed food, per saturation point provided
		private Float healthPercentageRecoveredPerSaturationPoint = 1.0f;
		private Float kiPercentageRecoveredPerSaturationPoint = 1.0f;
		private Float staminaPercentageRecoveredPerSaturationPoint = 1.0f;

		// Whitelisted mods and items
		private List<String> whitelistedNamespaces = new ArrayList();
		private List<String> whitelistedItems = new ArrayList<>();

		// Blacklisted mods and items
		private List<String> blacklistedNamespaces = new ArrayList<>();
		private List<String> blacklistedItems = new ArrayList<>();
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
