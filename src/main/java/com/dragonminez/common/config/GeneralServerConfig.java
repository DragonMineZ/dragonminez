package com.dragonminez.common.config;

import com.dragonminez.common.init.item.consumables.CapsuleType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class GeneralServerConfig {
	public static final int CURRENT_VERSION = 4;

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
		private Integer passiveTpGain = 1;
		private Integer tpPer20BlocksTraveled = 1;
		private Integer tpPerBlockMined = 1;
		private Integer tpPerItemCrafted = 1;
		private Boolean gravityBonusEnabled = true;
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
		private Integer partyMaxMembers = -1;
		private Double partyTpShareRatio = 0.0;

		private List<String> helmetsThatKeepHair = new ArrayList<>(Arrays.asList(
				"dragonminez:pothala_left",
				"dragonminez:pothala_right",
				"dragonminez:green_pothala_left",
				"dragonminez:green_pothala_right",
				"dragonminez:red_scouter",
				"dragonminez:blue_scouter",
				"dragonminez:green_scouter",
				"dragonminez:purple_scouter"
		));

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

		public Integer getPassiveTpGain() {
			return Math.max(0, Math.min(passiveTpGain, Integer.MAX_VALUE));
		}

		public Integer getTpPer20BlocksTraveled() {
			return Math.max(0, Math.min(tpPer20BlocksTraveled, Integer.MAX_VALUE));
		}

		public Integer getTpPerBlockMined() {
			return Math.max(0, Math.min(tpPerBlockMined, Integer.MAX_VALUE));
		}

		public Integer getTpPerItemCrafted() {
			return Math.max(0, Math.min(tpPerItemCrafted, Integer.MAX_VALUE));
		}

		public Boolean getGravityBonusEnabled() {
			return gravityBonusEnabled != null ? gravityBonusEnabled : true;
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

		public List<String> getHelmetsThatKeepHair() {
			return helmetsThatKeepHair;
		}

		public Integer getPartyMaxMembers() {
			if (partyMaxMembers == null) return -1;
			return partyMaxMembers < -1 ? -1 : partyMaxMembers;
		}

		public Double getPartyTpShareRatio() {
			if (partyTpShareRatio == null) return 0.0;
			return Math.max(0.0, Math.min(partyTpShareRatio, 10.0));
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
		private Integer minHungerPoints = 2;
		private Integer maxHungerPoints = 20;

		private Float minSaturationPoints = 0.4f;
		private Float maxSaturationPoints = 2.0f;

		private Float healthPercentageRecoveredPerHungerPoint = 0.01f;
		private Float kiPercentageRecoveredPerHungerPoint = 0.01f;
		private Float staminaPercentageRecoveredPerHungerPoint = 0.02f;

		private Float healthPercentageRecoveredPerSaturationPoint = 0.001f;
		private Float kiPercentageRecoveredPerSaturationPoint = 0.001f;
		private Float staminaPercentageRecoveredPerSaturationPoint = 0.004f;

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