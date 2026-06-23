package com.dragonminez.common.config;

import com.dragonminez.common.init.item.consumables.CapsuleType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class GeneralServerConfig {
	public static final int CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	@Setter
	private int configVersion;

	private WorldGenConfig worldGen = new WorldGenConfig();
	private GameplayConfig gameplay = new GameplayConfig();
	private RacialSkillsConfig racialSkills = new RacialSkillsConfig();
	private DynamicGrowthConfig dynamicGrowth = new DynamicGrowthConfig();
	private StorageConfig storage = new StorageConfig();
	private GravityConfig gravity = new GravityConfig();
	private MutantConfig mutant = new MutantConfig();

	@Getter
	@NoArgsConstructor
	public static class WorldGenConfig {
		private Boolean generateCustomStructures = true;
		private Boolean generateDragonBalls = true;
		private Boolean otherworldActive = true;
		private Integer dbSpawnRange = 1000;
		private Integer dragonBallSets = 1;
		private Integer structureMinDistanceFromSpawn = 0;
		private Integer structureMaxDistanceFromSpawn = 4000;
		private Integer structureMinDistanceBetween = 250;

		public Integer getDBSpawnRange() {
			return Math.max(100, Math.min(dbSpawnRange, 6000));
		}

		public Integer getDragonBallSets() {
			return Math.max(0, Math.min(dragonBallSets, 10));
		}

		public Integer getStructureMaxDistanceFromSpawn() {
			int value = structureMaxDistanceFromSpawn != null ? structureMaxDistanceFromSpawn : 8000;
			return Math.max(3000, value);
		}

		public Integer getStructureMinDistanceFromSpawn() {
			int value = structureMinDistanceFromSpawn != null ? structureMinDistanceFromSpawn : 0;
			return Math.max(0, Math.min(value, getStructureMaxDistanceFromSpawn() - 16));
		}

		public Integer getStructureMinDistanceBetween() {
			int value = structureMinDistanceBetween != null ? structureMinDistanceBetween : 250;
			return Math.max(0, value);
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
		private Double tpHealthRatio = 0.25;
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
		private Boolean ultimateFormFixedValue = false;
		private Integer partyMaxMembers = -1;
		private Integer partyMaxLevelGap = 500;
		private Double partyTpShareRatio = 0.5;
		private Integer instantTransmissionPlayerRangePerLevel = 200;

		private List<String> helmetsThatKeepHair = new ArrayList<>(Arrays.asList(
				"dragonminez:invencible_armor_helmet",
				"dragonminez:invencible_blue_armor_helmet"
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

		public Integer getPartyMaxMembers() {
			if (partyMaxMembers == null) return -1;
			return partyMaxMembers < -1 ? -1 : partyMaxMembers;
		}

		public Integer getPartyMaxLevelGap() {
			if (partyMaxLevelGap == null) return 500;
			return partyMaxLevelGap < -1 ? -1 : partyMaxLevelGap;
		}

		public Double getPartyTpShareRatio() {
			if (partyTpShareRatio == null) return 0.0;
			return Math.max(0.0, Math.min(partyTpShareRatio, 10.0));
		}

		public Integer getInstantTransmissionPlayerRangePerLevel() {
			if (instantTransmissionPlayerRangePerLevel == null) return 200;
			return Math.max(0, Math.min(instantTransmissionPlayerRangePerLevel, Integer.MAX_VALUE));
		}

		public Boolean getUltimateFormFixedValue() {
			return ultimateFormFixedValue != null && ultimateFormFixedValue;
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
	public static class DynamicGrowthConfig {
		private Boolean enabled = true;
		private Boolean debugChat = false;
		private Boolean practiceCurveEnabled = true;
		private Double practiceXpMultiplier = 2.0;

		private Double strPracticeMultiplier = 1.0;
		private Double skpPracticeMultiplier = 1.0;
		private Double resPracticeMultiplier = 1.0;
		private Double vitPracticeMultiplier = 1.0;
		private Double pwrPracticeMultiplier = 1.0;
		private Double enePracticeMultiplier = 1.0;

		private Double staminaSpentXpRatio = 0.1;
		private Double energySpentXpRatio = 0.1;
		private Double kiWeaponMeleePwrShare = 0.25;

		private Double naturalCombatTpMultiplier = 1.0;
		private Boolean manualTpPurchasesEnabled = true;
		private Double attributeTpCostMultiplier = 1.0;

		private Double passiveAnimalPracticeMultiplier = 0.5;
		private Double villagerPracticeMultiplier = 0.1;
		private Double lowDamagePracticeMultiplier = 0.2;
		private Double shadowDummyPracticeMultiplier = 0.35;
		private Double noRiskPracticeMultiplier = 0.5;
		private Integer repeatTargetWindowSeconds = 30;
		private Integer repeatTargetSoftCap = 8;
		private Integer repeatTargetHardCap = 24;
		private Double repeatTargetSoftMultiplier = 0.5;
		private Double repeatTargetHardMultiplier = 0.15;

		public Boolean isEnabled() {
			return enabled != null ? enabled : true;
		}

		public Boolean isPracticeCurveEnabled() {
			return practiceCurveEnabled == null || practiceCurveEnabled;
		}

		public Double getPracticeXpMultiplier() {
			return clampNonNeg(practiceXpMultiplier, 2.0);
		}

		public Double getStatPracticeMultiplier(String statName) {
			return switch (statName.toUpperCase()) {
				case "STR" -> clampNonNeg(strPracticeMultiplier, 1.0);
				case "SKP" -> clampNonNeg(skpPracticeMultiplier, 1.0);
				case "RES" -> clampNonNeg(resPracticeMultiplier, 1.0);
				case "VIT" -> clampNonNeg(vitPracticeMultiplier, 1.0);
				case "PWR" -> clampNonNeg(pwrPracticeMultiplier, 1.0);
				case "ENE" -> clampNonNeg(enePracticeMultiplier, 1.0);
				default -> 1.0;
			};
		}

		public Double getStaminaSpentXpRatio() {
			return clampNonNeg(staminaSpentXpRatio, 0.1);
		}

		public Double getEnergySpentXpRatio() {
			return clampNonNeg(energySpentXpRatio, 0.1);
		}

		public Double getKiWeaponMeleePwrShare() {
			return clampNonNeg(kiWeaponMeleePwrShare, 0.25);
		}

		public Double getNaturalCombatTpMultiplier() {
			return clampNonNeg(naturalCombatTpMultiplier, 1.0);
		}

		public Boolean isManualTpPurchasesEnabled() {
			return manualTpPurchasesEnabled == null || manualTpPurchasesEnabled;
		}

		public Double getAttributeTpCostMultiplier() {
			return Math.max(1.0, attributeTpCostMultiplier != null ? attributeTpCostMultiplier : 1.0);
		}

		public Double getPassiveAnimalPracticeMultiplier() {
			return clampNonNeg(passiveAnimalPracticeMultiplier, 0.5);
		}

		public Double getVillagerPracticeMultiplier() {
			return clampNonNeg(villagerPracticeMultiplier, 0.1);
		}

		public Double getLowDamagePracticeMultiplier() {
			return clampNonNeg(lowDamagePracticeMultiplier, 0.2);
		}

		public Double getShadowDummyPracticeMultiplier() {
			return clampNonNeg(shadowDummyPracticeMultiplier, 0.35);
		}

		public Double getNoRiskPracticeMultiplier() {
			return clampNonNeg(noRiskPracticeMultiplier, 0.5);
		}

		public Integer getRepeatTargetWindowSeconds() {
			return Math.max(1, repeatTargetWindowSeconds != null ? repeatTargetWindowSeconds : 30);
		}

		public Integer getRepeatTargetSoftCap() {
			return Math.max(0, repeatTargetSoftCap != null ? repeatTargetSoftCap : 8);
		}

		public Integer getRepeatTargetHardCap() {
			return Math.max(0, repeatTargetHardCap != null ? repeatTargetHardCap : 24);
		}

		public Double getRepeatTargetSoftMultiplier() {
			return clampNonNeg(repeatTargetSoftMultiplier, 0.5);
		}

		public Double getRepeatTargetHardMultiplier() {
			return clampNonNeg(repeatTargetHardMultiplier, 0.15);
		}

		private static double clampNonNeg(Double value, double fallback) {
			if (value == null) return fallback;
			return Math.max(0.0, value);
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

	@Getter
	@NoArgsConstructor
	public static class GravityConfig {
		// --- Gravity sources ---
		private Boolean enabled = true;
		private Map<String, Double> gravityPerWorld = defaultGravityPerWorld();
		private Double defaultWorldGravity = 1.0;
		private Double npcGravityValue = 10.0;
		private Double npcGravityRange = 100.0;
		private Boolean machineGravityEnabled = true;

		// --- Weight -> load ---
		private Double weightGravityDivisor = 1000.0;

		// --- Resistance (effective stats) ---
		private Double resistanceStatDivisorRatio = 0.9;
		private Double resistanceScale = 100.0;

		// --- Stat reduction ---
		private Boolean statReductionEnabled = true;
		private String[] affectedStats = {"STR", "SKP", "PWR", "DEF", "STM"};
		private Double statReductionPerGravity = 0.01;
		private Double minStatReduction = 0.0;
		private Double maxStatReduction = 0.9;

		// --- Movement / attack penalties ---
		private Double hardStopThreshold = 75.0;
		private Double maxMovementPenalty = 0.95;
		private Double maxAttackPenalty = 0.9;
		private Double penaltyCurveFactor = 1.6;

		// --- Physical gravity ---
		private Boolean physicalEnabled = true;
		private Double maxJumpPenalty = 0.95;
		private Double extraFallPerGravity = 0.02;
		private Double maxExtraFall = 0.6;
		private Double maxFlyPenalty = 0.95;

		private Boolean tpEnabled = true;
		private Double tpPeakMultiplier = 2.0;
		private Double tpCurveWidth = 7.0;
		private Double tpGravityBonusPerGravity = 0.05;
		private Double masteryBonusPerGravity = 0.1;

		private Double consumptionPerGravity = 0.04;

		// --- Gravity Device machine ---
		private Integer deviceMinRoomSize = 5;
		private Integer deviceMaxRoomSize = 25;
		private Integer deviceMaxGravity = 1000;
		private Integer deviceEnergyCapacity = 20000;
		private Double deviceEnergyPerGravityPerSecond = 1.0;
		private Double deviceShaderGravityForMax = 60.0;

		private static Map<String, Double> defaultGravityPerWorld() {
			Map<String, Double> map = new LinkedHashMap<>();
			map.put("minecraft:overworld", 1.0);
			map.put("minecraft:the_nether", 8.0);
			map.put("minecraft:the_end", 20.0);
			map.put("dragonminez:time_chamber", 10.0);
			map.put("dragonminez:otherworld", 1.0);
			map.put("dragonminez:namek", 1.0);
			return map;
		}

		public Boolean isEnabled() {
			return enabled == null || enabled;
		}

		public Double getWorldGravity(String dimensionId) {
			double fallback = getDefaultWorldGravity();
			if (gravityPerWorld == null) return fallback;
			Double value = gravityPerWorld.get(dimensionId);
			return value != null ? Math.max(0.0, value) : fallback;
		}

		public Double getDefaultWorldGravity() {
			return clampNonNeg(defaultWorldGravity, 1.0);
		}

		public Double getNpcGravityValue() {
			return clampNonNeg(npcGravityValue, 10.0);
		}

		public Double getNpcGravityRange() {
			return clampNonNeg(npcGravityRange, 100.0);
		}

		public Boolean getMachineGravityEnabled() {
			return machineGravityEnabled == null || machineGravityEnabled;
		}

		public Double getWeightGravityDivisor() {
			return Math.max(1.0, weightGravityDivisor != null ? weightGravityDivisor : 1000.0);
		}

		public Double getResistanceStatDivisorRatio() {
			Double value = resistanceStatDivisorRatio != null ? resistanceStatDivisorRatio : 0.9;
			return Math.max(0.01, Math.min(value, 1.0));
		}

		public Double getResistanceScale() {
			return clampNonNeg(resistanceScale, 100.0);
		}

		public Boolean getStatReductionEnabled() {
			return statReductionEnabled == null || statReductionEnabled;
		}

		public String[] getAffectedStats() {
			return affectedStats != null ? affectedStats : new String[]{"STR", "SKP", "PWR", "DEF", "STM"};
		}

		public Double getStatReductionPerGravity() {
			return clampNonNeg(statReductionPerGravity, 0.01);
		}

		public Double getMinStatReduction() {
			Double value = minStatReduction != null ? minStatReduction : 0.0;
			return Math.max(0.0, Math.min(value, 1.0));
		}

		public Double getMaxStatReduction() {
			Double value = maxStatReduction != null ? maxStatReduction : 0.9;
			return Math.max(0.0, Math.min(value, 1.0));
		}

		public Double getHardStopThreshold() {
			return clampNonNeg(hardStopThreshold, 75.0);
		}

		public Double getMaxMovementPenalty() {
			Double value = maxMovementPenalty != null ? maxMovementPenalty : 0.95;
			return Math.max(0.0, Math.min(value, 1.0));
		}

		public Double getMaxAttackPenalty() {
			Double value = maxAttackPenalty != null ? maxAttackPenalty : 0.9;
			return Math.max(0.0, Math.min(value, 1.0));
		}

		public Double getPenaltyCurveFactor() {
			return clampNonNeg(penaltyCurveFactor, 1.6);
		}

		public Boolean getPhysicalEnabled() {
			return physicalEnabled == null || physicalEnabled;
		}

		public Double getMaxJumpPenalty() {
			Double value = maxJumpPenalty != null ? maxJumpPenalty : 0.95;
			return Math.max(0.0, Math.min(value, 1.0));
		}

		public Double getExtraFallPerGravity() {
			return clampNonNeg(extraFallPerGravity, 0.02);
		}

		public Double getMaxExtraFall() {
			return clampNonNeg(maxExtraFall, 0.6);
		}

		public Double getMaxFlyPenalty() {
			Double value = maxFlyPenalty != null ? maxFlyPenalty : 0.95;
			return Math.max(0.0, Math.min(value, 1.0));
		}

		public Boolean getTpEnabled() {
			return tpEnabled == null || tpEnabled;
		}

		public Double getTpPeakMultiplier() {
			return clampNonNeg(tpPeakMultiplier, 2.0);
		}

		public Double getTpCurveWidth() {
			return Math.max(0.0001, tpCurveWidth != null ? tpCurveWidth : 7.0);
		}

		public Double getTpGravityBonusPerGravity() {
			return clampNonNeg(tpGravityBonusPerGravity, 0.05);
		}

		public Double getMasteryBonusPerGravity() {
			return clampNonNeg(masteryBonusPerGravity, 0.1);
		}

		public Double getConsumptionPerGravity() {
			return clampNonNeg(consumptionPerGravity, 0.04);
		}

		public Integer getDeviceMinRoomSize() {
			return Math.max(1, deviceMinRoomSize != null ? deviceMinRoomSize : 5);
		}

		public Integer getDeviceMaxRoomSize() {
			int min = getDeviceMinRoomSize();
			int max = deviceMaxRoomSize != null ? deviceMaxRoomSize : 25;
			return Math.max(min, max);
		}

		public Integer getDeviceMaxGravity() {
			return Math.max(1, deviceMaxGravity != null ? deviceMaxGravity : 1000);
		}

		public Integer getDeviceEnergyCapacity() {
			return Math.max(1, deviceEnergyCapacity != null ? deviceEnergyCapacity : 20000);
		}

		public Double getDeviceEnergyPerGravityPerSecond() {
			return clampNonNeg(deviceEnergyPerGravityPerSecond, 1.0);
		}

		public Double getDeviceShaderGravityForMax() {
			return Math.max(1.0, deviceShaderGravityForMax != null ? deviceShaderGravityForMax : 60.0);
		}

		private static double clampNonNeg(Double value, double fallback) {
			if (value == null) return fallback;
			return Math.max(0.0, value);
		}
	}

	@Getter
	@NoArgsConstructor
	public static class MutantConfig {
		private Boolean enabled = true;
		private Integer rollIntervalMinutes = 30;
		private Integer playersPerRoll = 1;
		private Double chance = 0.20;
		private Integer maxHolders = 1;
		private String legendaryGroupName = "legendaryforms";
		private Double tpGainMultiplier = 1.25;
		private Double masteryGainMultiplier = 1.50;
		private Double powerBonusReductionNoSkill = 0.67;
		private Double powerBonusBoostWithSkill = 0.33;

		public Boolean getEnabled() {
			return enabled == null || enabled;
		}

		public Integer getRollIntervalMinutes() {
			return Math.max(1, rollIntervalMinutes != null ? rollIntervalMinutes : 30);
		}

		public Integer getPlayersPerRoll() {
			return Math.max(1, playersPerRoll != null ? playersPerRoll : 1);
		}

		public Double getChance() {
			Double value = chance != null ? chance : 0.20;
			return Math.max(0.0, Math.min(value, 1.0));
		}

		public Integer getMaxHolders() {
			return Math.max(0, maxHolders != null ? maxHolders : 1);
		}

		public String getLegendaryGroupName() {
			return legendaryGroupName != null && !legendaryGroupName.isEmpty() ? legendaryGroupName : "legendaryforms";
		}

		public Double getTpGainMultiplier() {
			return Math.max(0.0, tpGainMultiplier != null ? tpGainMultiplier : 1.25);
		}

		public Double getMasteryGainMultiplier() {
			return Math.max(0.0, masteryGainMultiplier != null ? masteryGainMultiplier : 1.50);
		}

		public Double getPowerBonusReductionNoSkill() {
			Double value = powerBonusReductionNoSkill != null ? powerBonusReductionNoSkill : 0.33;
			return Math.max(0.0, Math.min(value, 1.0));
		}

		public Double getPowerBonusBoostWithSkill() {
			return Math.max(0.0, powerBonusBoostWithSkill != null ? powerBonusBoostWithSkill : 0.33);
		}
	}
}