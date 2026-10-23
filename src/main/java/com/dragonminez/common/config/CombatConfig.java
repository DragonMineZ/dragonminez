package com.dragonminez.common.config;

import com.dragonminez.common.diagnostics.RemovedConfigKeys;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RemovedConfigKeys({"flatMitigationFactor"})
public class CombatConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;
	private String configVersion;
	private Double staminaConsumptionRatio = 0.083;
	private Double blockStaminaCost = 0.25;
	private Integer knockdownDurationSeconds = 30;
	private Integer baselineFormDrain = 80;
	private Boolean killPlayersOnCombatLogout = true;
	private Double kiProtectionMitigationPerLevel = 0.025;
	private Double kiProtectionCostRatio = 0.15;
	private Double kiInfusionDamagePerLevel = 0.025;
	private Double kiInfusionBaseCostPct = 2.5;
	private Double kiInfusionMaxCostPct = 7.5;
	private Double baseDamageReductionCap = 0.88;
	private Double enchantmentDamageReductionCap = 0.93;
	private Double defenseDecayOnGuardBreak = 0.66;
	private Double flatMitigationMaxAbsorbFraction = 0.82;
	private Double defenseReductionScale = 0.11;
	private Boolean enableAdaptativeDefenseMitigation = true;
	private Double adaptativeMitigationParityRatio = 1.0;
	private Double adaptativeMitigationParityValue = 0.35;
	private Double adaptativeMitigationZeroRatio = 14.0;
	private Double adaptativeDefenseMitigationCap = 0.65;
	private Boolean cancelDamageEventIfMitigationTooHigh = true;
	private Double cancelDamageMitigationThreshold = 2.5;
	private Boolean enableDamageScaledKnockback = true;
	private Double knockbackMinMultiplier = 0.15;
	private Double knockbackHeavyHitHealthFraction = 0.15;
	private Double knockbackHeavyHitGain = 3.0;
	private Double knockbackMaxMultiplier = 2.5;
	private Boolean accurateMobBattlePower = true;
	private Boolean enableBlocking = true;
	private Boolean enableParrying = true;
	private Integer parryWindowMs = 150;
	private Double parryStaminaCostPenalty = 2.0;
	private Integer blockBreakStunDurationTicks = 60;
	private Double poiseDamageMultiplier = 0.25;
	private Double blockDamageReductionCap = 0.65;
	private Double blockDamageReductionMin = 0.05;
	private Integer poiseRegenCooldown = 100;
	private Integer strikeGlobalCooldownTicks = 60;
	private Boolean enableSpeedSystem = true;
	private Double speedReferenceStat = 10000.0;
	private Double transformedSpeedCap = 2.0;
	private Double speedMovementCap = 3.0;
	private Double speedStepAssistThreshold = 2.0;
	private Double speedStepAssistBonus = 0.5;
	private Boolean enableSpeedDodge = true;
	private Double speedDodgeRatioAtLevel1 = 2.5;
	private Double speedDodgeRatioAtMaxLevel = 1.4;
	private Integer speedDodgeCooldownTicks = 10;
	private Double nonPlayerAttackerSpeed = 1.0;
	private Double speedDodgeBaseChance = 0.1;
	private Double speedDodgeMaxChancePvp = 0.35;
	private Double speedDodgeMaxChancePve = 0.5;
	private Double speedDodgeMeditationChanceFloor = 0.6;
	private Double speedDodgeFullChanceSpeedRatio = 2.0;
	private Double speedDodgeBpRatioAtLevel1 = 2.0;
	private Double speedDodgeBpRatioAtMaxLevel = 1.0;
	private Double speedDodgeFullChanceBpRatio = 10.0;
	private Double speedDodgeStreakMultiplier = 0.75;
	private Integer speedDodgeStreakResetTicks = 60;
	private Double speedDodgeStaminaCostPct = 0.03;
	private Boolean enablePerfectEvasion = true;
	private Integer perfectEvasionWindowMs = 200;
	private Integer dashCooldownSeconds = 4;
	private Integer doubleDashCooldownSeconds = 12;
	private Integer teleportCooldownSeconds = 30;
	private Boolean combatFlyAutoSwitchOnDamage = true;
	private Integer combatFlyLockSeconds = 8;
	private Double combatFlyBaseSpeed = 0.3;
	private Double combatFlySprintSpeed = 0.5;
	private Double combatFlyHoldSpeedMultiplier = 1.6;
	private Double combatFlyDrainMultiplier = 0.5;
	private Double combatFlyImpulseKiCostPct = 0.05;
	private Integer combatFlyImpulseCooldownTicks = 25;
	private Boolean kiChargeInterruptOnHit = true;
	private Boolean kiChargeDeflectsProjectiles = true;
	private Integer kiChargeInterruptLockTicks = 10;
	private Boolean enableKiSurge = true;
	private Double kiBurstRadius = 8.0;
	private Double kiBurstKnockback = 1.4;
	private Double surgeFillSeconds = 4.0;
	private Double surgeDurationSeconds = 12.0;
	private Integer surgeCooldownSeconds = 15;
	private Double surgeDrainPerKiFraction = 1.5;
	private Double surgeBurstDamageRatio = 0.25;
	private Double surgeBurstRadius = 10.0;
	private Double surgeBurstKnockback = 2.2;
	private Double surgeKiCostMultiplier = 1.25;
	private Double surgeKiDamageMultiplier = 1.15;
	private Double surgeKiSpeedMultiplier = 1.25;
	private Double surgeKiChargeSpeedMultiplier = 1.2;
	private Double surgeMoveSpeedMultiplier = 1.25;
	private Double surgeFlySpeedMultiplier = 1.25;
	private Double surgeAttackSpeedMultiplier = 1.4;
	private Double surgeAuraScaleMultiplier = 1.25;
	private Double surgeAuraSpeedMultiplier = 1.5;
	private Map<String, KiWeaponConfig> kiWeaponsConfig = new HashMap<>() {
		{
			put("blade", new KiWeaponConfig(0.0, 0.25, 0.0, 0.075, -2.4, "#FFFFFF", "dragonminez:sword"));
			put("scythe", new KiWeaponConfig(0.0, 0.45, 0.0, 0.105, -2.8, "#FFFFFF", "dragonminez:scythe"));
			put("clawlance", new KiWeaponConfig(0.0, 0.65, 0.0, 0.175, -2.6, "#FFFFFF", "dragonminez:trident"));
		}
	};
	private Float upswingMultiplier = 0.5F;
	private Boolean allowAttackingMount = false;
	private Integer attackIntervalCap = 2;
	private Boolean weaponRegistryLogging = false;
	private Boolean weaponRegistryCompression = true;
	private Map<String, TargetHelper.Relation> playerRelations = new HashMap<>() {
		{
			put("minecraft:player", TargetHelper.Relation.HOSTILE);
			put("minecraft:villager", TargetHelper.Relation.NEUTRAL);
			put("minecraft:iron_golem", TargetHelper.Relation.NEUTRAL);
			put("guardvillagers:guard", TargetHelper.Relation.NEUTRAL);
		}
	};
	private List<String> masteryBlacklistEntities = new ArrayList<>(Arrays.asList("minecraft:silverfish", "dummmmmmy:target_dummy"));
	private TargetHelper.Relation playerRelationToPassives = TargetHelper.Relation.HOSTILE;
	private TargetHelper.Relation playerRelationToHostiles = TargetHelper.Relation.HOSTILE;
	private TargetHelper.Relation playerRelationToOther = TargetHelper.Relation.HOSTILE;
	private Boolean fallbackCompatibilityEnabled = true;
	private String blacklistItemIdRegex = "pickaxe";
	private List<CompatibilitySpecifier> fallbackCompatibility = new ArrayList<>(Arrays.asList(new CompatibilitySpecifier("claymore|great_sword|greatsword", "dragonminez:claymore"), new CompatibilitySpecifier("great_hammer|greathammer|war_hammer|warhammer|maul", "dragonminez:hammer"), new CompatibilitySpecifier("double_axe|doubleaxe|war_axe|waraxe|great_axe|greataxe", "dragonminez:double_axe"), new CompatibilitySpecifier("scythe", "dragonminez:scythe"), new CompatibilitySpecifier("halberd|glaive|pike|lance|naginata", "dragonminez:halberd"), new CompatibilitySpecifier("spear|trident|pitchfork|javelin", "dragonminez:spear"), new CompatibilitySpecifier("battlestaff|staff|quarterstaff|pole", "dragonminez:battlestaff"), new CompatibilitySpecifier("katana|uchigatana|nodachi|tachi", "dragonminez:katana"), new CompatibilitySpecifier("rapier|foil", "dragonminez:rapier"), new CompatibilitySpecifier("dagger|knife|shiv|dirk|kunai|karambit|wakizashi|tanto", "dragonminez:dagger"), new CompatibilitySpecifier("sickle|kama", "dragonminez:sickle"), new CompatibilitySpecifier("soul_knife", "dragonminez:soul_knife"), new CompatibilitySpecifier("claw|katar", "dragonminez:claw"), new CompatibilitySpecifier("wand", "dragonminez:wand"), new CompatibilitySpecifier("mace|hammer|flail", "dragonminez:mace"), new CompatibilitySpecifier("axe", "dragonminez:axe"), new CompatibilitySpecifier("coral_blade", "dragonminez:coral_blade"), new CompatibilitySpecifier("twin_blade|twinblade", "dragonminez:twin_blade"), new CompatibilitySpecifier("cutlass|scimitar|machete", "dragonminez:cutlass"), new CompatibilitySpecifier("sword|blade", "dragonminez:sword")));

	public boolean getEnableSpeedSystem() {
		return enableSpeedSystem == null || enableSpeedSystem;
	}

	public double getSpeedReferenceStat() {
		return speedReferenceStat != null ? Math.max(1.0, speedReferenceStat) : 10000.0;
	}

	public double getTransformedSpeedCap() {
		return transformedSpeedCap != null ? Math.max(0.01, transformedSpeedCap) : 2.0;
	}

	public double getSpeedMovementCap() {
		return speedMovementCap != null ? Math.max(1.0, speedMovementCap) : 3.0;
	}

	public double getSpeedStepAssistThreshold() {
		return speedStepAssistThreshold != null ? Math.max(1.0, speedStepAssistThreshold) : 2.0;
	}

	public double getSpeedStepAssistBonus() {
		return speedStepAssistBonus != null ? Math.max(0.0, speedStepAssistBonus) : 0.5;
	}

	public boolean getEnableSpeedDodge() {
		return enableSpeedDodge == null || enableSpeedDodge;
	}

	public double getSpeedDodgeRatioAtLevel1() {
		return speedDodgeRatioAtLevel1 != null ? Math.max(1.0, speedDodgeRatioAtLevel1) : 2.5;
	}

	public double getSpeedDodgeRatioAtMaxLevel() {
		return speedDodgeRatioAtMaxLevel != null ? Math.max(1.0, speedDodgeRatioAtMaxLevel) : 1.4;
	}

	public int getSpeedDodgeCooldownTicks() {
		return speedDodgeCooldownTicks != null ? Math.max(0, speedDodgeCooldownTicks) : 10;
	}

	public double getNonPlayerAttackerSpeed() {
		return nonPlayerAttackerSpeed != null ? Math.max(0.01, nonPlayerAttackerSpeed) : 1.0;
	}

	public double getSpeedDodgeBaseChance() {
		return speedDodgeBaseChance != null ? Math.max(0.0, Math.min(1.0, speedDodgeBaseChance)) : 0.1;
	}

	public double getSpeedDodgeMaxChancePvp() {
		return speedDodgeMaxChancePvp != null ? Math.max(0.0, Math.min(1.0, speedDodgeMaxChancePvp)) : 0.35;
	}

	public double getSpeedDodgeMaxChancePve() {
		return speedDodgeMaxChancePve != null ? Math.max(0.0, Math.min(1.0, speedDodgeMaxChancePve)) : 0.5;
	}

	public double getSpeedDodgeMeditationChanceFloor() {
		return speedDodgeMeditationChanceFloor != null ? Math.max(0.0, Math.min(1.0, speedDodgeMeditationChanceFloor)) : 0.6;
	}

	public double getSpeedDodgeFullChanceSpeedRatio() {
		return speedDodgeFullChanceSpeedRatio != null ? Math.max(1.01, speedDodgeFullChanceSpeedRatio) : 2.0;
	}

	public double getSpeedDodgeBpRatioAtLevel1() {
		return speedDodgeBpRatioAtLevel1 != null ? Math.max(0.01, speedDodgeBpRatioAtLevel1) : 2.0;
	}

	public double getSpeedDodgeBpRatioAtMaxLevel() {
		return speedDodgeBpRatioAtMaxLevel != null ? Math.max(0.01, speedDodgeBpRatioAtMaxLevel) : 1.0;
	}

	public double getSpeedDodgeFullChanceBpRatio() {
		return speedDodgeFullChanceBpRatio != null ? Math.max(1.01, speedDodgeFullChanceBpRatio) : 10.0;
	}

	public double getSpeedDodgeStreakMultiplier() {
		return speedDodgeStreakMultiplier != null ? Math.max(0.0, Math.min(1.0, speedDodgeStreakMultiplier)) : 0.75;
	}

	public int getSpeedDodgeStreakResetTicks() {
		return speedDodgeStreakResetTicks != null ? Math.max(0, speedDodgeStreakResetTicks) : 60;
	}

	public double getSpeedDodgeStaminaCostPct() {
		return speedDodgeStaminaCostPct != null ? Math.max(0.0, Math.min(1.0, speedDodgeStaminaCostPct)) : 0.03;
	}

	public float getUpswingMultiplier() {
		return Math.max(0.2F, Math.min(1.0F, upswingMultiplier));
	}

	public double getFlatMitigationMaxAbsorbFraction() {
		return flatMitigationMaxAbsorbFraction != null ? Math.max(0.0, Math.min(1.0, flatMitigationMaxAbsorbFraction)) : 0.82;
	}

	public double getDefenseReductionScale() {
		return defenseReductionScale != null ? Math.max(0.01, defenseReductionScale) : 0.11;
	}

	public boolean getEnableAdaptativeDefenseMitigation() {
		return enableAdaptativeDefenseMitigation == null || enableAdaptativeDefenseMitigation;
	}

	public double getAdaptativeMitigationParityRatio() {
		return adaptativeMitigationParityRatio != null ? Math.max(1.0E-4, adaptativeMitigationParityRatio) : 1.0;
	}

	public double getAdaptativeMitigationParityValue() {
		return adaptativeMitigationParityValue != null ? Math.max(0.0, Math.min(1.0, adaptativeMitigationParityValue)) : 0.25;
	}

	public double getAdaptativeMitigationZeroRatio() {
		return adaptativeMitigationZeroRatio != null ? Math.max(getAdaptativeMitigationParityRatio() + 1.0E-4, adaptativeMitigationZeroRatio) : 5.0;
	}

	public double getAdaptativeDefenseMitigationCap() {
		return adaptativeDefenseMitigationCap != null ? Math.max(0.0, Math.min(1.0, adaptativeDefenseMitigationCap)) : 0.7;
	}

	public boolean getCancelDamageEventIfMitigationTooHigh() {
		return cancelDamageEventIfMitigationTooHigh == null || cancelDamageEventIfMitigationTooHigh;
	}

	public double getCancelDamageMitigationThreshold() {
		return cancelDamageMitigationThreshold != null ? Math.max(1.0, cancelDamageMitigationThreshold) : 3.0;
	}

	public boolean getEnableDamageScaledKnockback() {
		return enableDamageScaledKnockback == null || enableDamageScaledKnockback;
	}

	public double getKnockbackMinMultiplier() {
		return knockbackMinMultiplier != null ? Math.max(0.0, Math.min(1.0, knockbackMinMultiplier)) : 0.15;
	}

	public double getKnockbackHeavyHitHealthFraction() {
		return knockbackHeavyHitHealthFraction != null ? Math.max(0.01, Math.min(1.0, knockbackHeavyHitHealthFraction)) : 0.15;
	}

	public double getKnockbackHeavyHitGain() {
		return knockbackHeavyHitGain != null ? Math.max(0.0, knockbackHeavyHitGain) : 3.0;
	}

	public double getKnockbackMaxMultiplier() {
		return knockbackMaxMultiplier != null ? Math.max(1.0, knockbackMaxMultiplier) : 2.5;
	}

	public boolean getAccurateMobBattlePower() {
		return accurateMobBattlePower == null || accurateMobBattlePower;
	}

	public KiWeaponConfig getKiWeaponConfig(String type) {
		if (type == null || kiWeaponsConfig == null) return null;
		return kiWeaponsConfig.get(type.toLowerCase());
	}

	public List<String> getKiWeaponTypes() {
		if (kiWeaponsConfig == null) return new ArrayList<>();
		List<String> keys = new ArrayList<>(kiWeaponsConfig.keySet());
		java.util.Collections.sort(keys);
		return keys;
	}


	public static class CompatibilitySpecifier {
		private String item_id_regex;
		private String weapon_attributes;

		public CompatibilitySpecifier(final String item_id_regex, final String weapon_attributes) {
			this.item_id_regex = item_id_regex;
			this.weapon_attributes = weapon_attributes;
		}

		public CompatibilitySpecifier() {
		}

		public String getItem_id_regex() {
			return this.item_id_regex;
		}

		public String getWeapon_attributes() {
			return this.weapon_attributes;
		}
	}


	public static class KiWeaponConfig {
		private Double baseDamage = 0.0;
		private Double kiScalingDamage = 1.0;
		private Double baseKiCost = 0.0;
		private Double kiScalingCost = 0.05;
		private Double attackSpeed = -2.4;
		private String forcedColor = "#FFFFFF";
		private String weaponCombo = "";

		public double getBaseDamage() {
			return baseDamage != null ? Math.max(0.0, baseDamage) : 0.0;
		}

		public double getKiScalingDamage() {
			return kiScalingDamage != null ? Math.max(0.0, kiScalingDamage) : 0.0;
		}

		public double getBaseKiCost() {
			return baseKiCost != null ? Math.max(0.0, baseKiCost) : 0.0;
		}

		public double getKiScalingCost() {
			return kiScalingCost != null ? Math.max(0.0, kiScalingCost) : 0.0;
		}

		public double getAttackSpeed() {
			return attackSpeed != null ? attackSpeed : 0.0;
		}

		public KiWeaponConfig(final Double baseDamage, final Double kiScalingDamage, final Double baseKiCost, final Double kiScalingCost, final Double attackSpeed, final String forcedColor, final String weaponCombo) {
			this.baseDamage = baseDamage;
			this.kiScalingDamage = kiScalingDamage;
			this.baseKiCost = baseKiCost;
			this.kiScalingCost = kiScalingCost;
			this.attackSpeed = attackSpeed;
			this.forcedColor = forcedColor;
			this.weaponCombo = weaponCombo;
		}

		public KiWeaponConfig() {
		}

		public String getForcedColor() {
			return this.forcedColor;
		}

		public String getWeaponCombo() {
			return this.weaponCombo;
		}
	}

	public String getConfigVersion() {
		return this.configVersion;
	}

	public Double getStaminaConsumptionRatio() {
		return this.staminaConsumptionRatio;
	}

	public Double getBlockStaminaCost() {
		return this.blockStaminaCost;
	}

	public Integer getKnockdownDurationSeconds() {
		return this.knockdownDurationSeconds;
	}

	public Integer getBaselineFormDrain() {
		return this.baselineFormDrain;
	}

	public Boolean getKillPlayersOnCombatLogout() {
		return this.killPlayersOnCombatLogout;
	}

	public Double getKiProtectionMitigationPerLevel() {
		return this.kiProtectionMitigationPerLevel;
	}

	public Double getKiProtectionCostRatio() {
		return this.kiProtectionCostRatio;
	}

	public Double getKiInfusionDamagePerLevel() {
		return this.kiInfusionDamagePerLevel;
	}

	public Double getKiInfusionBaseCostPct() {
		return this.kiInfusionBaseCostPct;
	}

	public Double getKiInfusionMaxCostPct() {
		return this.kiInfusionMaxCostPct;
	}

	public Double getBaseDamageReductionCap() {
		return this.baseDamageReductionCap;
	}

	public Double getEnchantmentDamageReductionCap() {
		return this.enchantmentDamageReductionCap;
	}

	public Double getDefenseDecayOnGuardBreak() {
		return this.defenseDecayOnGuardBreak;
	}

	public Boolean getEnableBlocking() {
		return this.enableBlocking;
	}

	public Boolean getEnableParrying() {
		return this.enableParrying;
	}

	public Integer getParryWindowMs() {
		return this.parryWindowMs;
	}

	public Double getParryStaminaCostPenalty() {
		return this.parryStaminaCostPenalty;
	}

	public Integer getBlockBreakStunDurationTicks() {
		return this.blockBreakStunDurationTicks;
	}

	public Double getPoiseDamageMultiplier() {
		return this.poiseDamageMultiplier;
	}

	public Double getBlockDamageReductionCap() {
		return this.blockDamageReductionCap;
	}

	public Double getBlockDamageReductionMin() {
		return this.blockDamageReductionMin;
	}

	public Integer getPoiseRegenCooldown() {
		return this.poiseRegenCooldown;
	}

	public Integer getStrikeGlobalCooldownTicks() {
		return this.strikeGlobalCooldownTicks;
	}

	public Boolean getEnablePerfectEvasion() {
		return this.enablePerfectEvasion;
	}

	public Integer getPerfectEvasionWindowMs() {
		return this.perfectEvasionWindowMs;
	}

	public Integer getDashCooldownSeconds() {
		return this.dashCooldownSeconds;
	}

	public Integer getDoubleDashCooldownSeconds() {
		return this.doubleDashCooldownSeconds;
	}

	public Integer getTeleportCooldownSeconds() {
		return this.teleportCooldownSeconds;
	}

	public Boolean getCombatFlyAutoSwitchOnDamage() {
		return this.combatFlyAutoSwitchOnDamage;
	}

	public Integer getCombatFlyLockSeconds() {
		return this.combatFlyLockSeconds;
	}

	public Double getCombatFlyBaseSpeed() {
		return this.combatFlyBaseSpeed;
	}

	public Double getCombatFlySprintSpeed() {
		return this.combatFlySprintSpeed;
	}

	public Double getCombatFlyHoldSpeedMultiplier() {
		return this.combatFlyHoldSpeedMultiplier;
	}

	public Double getCombatFlyDrainMultiplier() {
		return this.combatFlyDrainMultiplier;
	}

	public Double getCombatFlyImpulseKiCostPct() {
		return this.combatFlyImpulseKiCostPct;
	}

	public Integer getCombatFlyImpulseCooldownTicks() {
		return this.combatFlyImpulseCooldownTicks;
	}

	public Boolean getKiChargeInterruptOnHit() {
		return this.kiChargeInterruptOnHit;
	}

	public Boolean getKiChargeDeflectsProjectiles() {
		return this.kiChargeDeflectsProjectiles;
	}

	public Integer getKiChargeInterruptLockTicks() {
		return this.kiChargeInterruptLockTicks;
	}

	public Boolean getEnableKiSurge() {
		return this.enableKiSurge;
	}

	public Double getKiBurstRadius() {
		return this.kiBurstRadius;
	}

	public Double getKiBurstKnockback() {
		return this.kiBurstKnockback;
	}

	public Double getSurgeFillSeconds() {
		return this.surgeFillSeconds;
	}

	public Double getSurgeDurationSeconds() {
		return this.surgeDurationSeconds;
	}

	public Integer getSurgeCooldownSeconds() {
		return this.surgeCooldownSeconds;
	}

	public Double getSurgeDrainPerKiFraction() {
		return this.surgeDrainPerKiFraction;
	}

	public Double getSurgeBurstDamageRatio() {
		return this.surgeBurstDamageRatio;
	}

	public Double getSurgeBurstRadius() {
		return this.surgeBurstRadius;
	}

	public Double getSurgeBurstKnockback() {
		return this.surgeBurstKnockback;
	}

	public Double getSurgeKiCostMultiplier() {
		return this.surgeKiCostMultiplier;
	}

	public Double getSurgeKiDamageMultiplier() {
		return this.surgeKiDamageMultiplier;
	}

	public Double getSurgeKiSpeedMultiplier() {
		return this.surgeKiSpeedMultiplier;
	}

	public Double getSurgeKiChargeSpeedMultiplier() {
		return this.surgeKiChargeSpeedMultiplier;
	}

	public Double getSurgeMoveSpeedMultiplier() {
		return this.surgeMoveSpeedMultiplier;
	}

	public Double getSurgeFlySpeedMultiplier() {
		return this.surgeFlySpeedMultiplier;
	}

	public Double getSurgeAttackSpeedMultiplier() {
		return this.surgeAttackSpeedMultiplier;
	}

	public Double getSurgeAuraScaleMultiplier() {
		return this.surgeAuraScaleMultiplier;
	}

	public Double getSurgeAuraSpeedMultiplier() {
		return this.surgeAuraSpeedMultiplier;
	}

	public Map<String, KiWeaponConfig> getKiWeaponsConfig() {
		return this.kiWeaponsConfig;
	}

	public Boolean getAllowAttackingMount() {
		return this.allowAttackingMount;
	}

	public Integer getAttackIntervalCap() {
		return this.attackIntervalCap;
	}

	public Boolean getWeaponRegistryLogging() {
		return this.weaponRegistryLogging;
	}

	public Boolean getWeaponRegistryCompression() {
		return this.weaponRegistryCompression;
	}

	public Map<String, TargetHelper.Relation> getPlayerRelations() {
		return this.playerRelations;
	}

	public List<String> getMasteryBlacklistEntities() {
		return this.masteryBlacklistEntities;
	}

	public TargetHelper.Relation getPlayerRelationToPassives() {
		return this.playerRelationToPassives;
	}

	public TargetHelper.Relation getPlayerRelationToHostiles() {
		return this.playerRelationToHostiles;
	}

	public TargetHelper.Relation getPlayerRelationToOther() {
		return this.playerRelationToOther;
	}

	public Boolean getFallbackCompatibilityEnabled() {
		return this.fallbackCompatibilityEnabled;
	}

	public String getBlacklistItemIdRegex() {
		return this.blacklistItemIdRegex;
	}

	public List<CompatibilitySpecifier> getFallbackCompatibility() {
		return this.fallbackCompatibility;
	}

	public CombatConfig() {
	}

	public void setConfigVersion(final String configVersion) {
		this.configVersion = configVersion;
	}
}
