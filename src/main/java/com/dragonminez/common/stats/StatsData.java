package com.dragonminez.common.stats;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.stats.character.*;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.Training;
import com.dragonminez.common.stats.skills.Skills;
import com.dragonminez.common.stats.techniques.Techniques;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.util.GravityLogic;
import com.dragonminez.server.util.PotionEffectHelper;
import com.dragonminez.server.world.dimension.HTCDimension;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Collection;
import java.util.List;

@Getter
public class StatsData {
	private final Player player;
	private final Stats stats;
	private final Status status;
	private final Cooldowns cooldowns;
	private final Character character;
	private final Resources resources;
	private final Skills skills;
	private final Effects effects;
	private final PlayerQuestData playerQuestData;
	private final BonusStats bonusStats;
	private final Training training;
	private final Techniques techniques;

	private boolean hasInitializedHealth = false;
	private boolean isDataLoaded = false;

	public StatsData(Player player) {
		this.player = player;
		this.stats = new Stats();
		this.stats.setPlayer(player);
		this.status = new Status();
		this.cooldowns = new Cooldowns();
		this.character = new Character();
		this.resources = new Resources();
		this.resources.setPlayer(player);
		this.skills = new Skills();
		this.effects = new Effects();
		this.playerQuestData = new PlayerQuestData();
		this.bonusStats = new BonusStats();
		this.training = new Training();
		this.techniques = new Techniques();
	}

	public boolean hasInitializedHealth() {
		return hasInitializedHealth;
	}

	public void setInitializedHealth(boolean initialized) {
		this.hasInitializedHealth = initialized;
	}

	public int getLevel() {
		int maxLevel = getConfiguredMaxValue();
		if (maxLevel <= 1) return 1;

		int initialStats = getInitialTotalStats();
		int totalStats = Math.max(initialStats, stats.getTotalStats());

		long maxTotalStatsForLevel = Math.max(initialStats, getConfiguredMaxTotalStatsRaw());
		double denominator = Math.max(1.0, (double) maxTotalStatsForLevel - initialStats);
		double progress = (totalStats - initialStats) / denominator;
		progress = Math.max(0.0, Math.min(1.0, progress));

		int computedLevel = 1 + (int) Math.floor(progress * (maxLevel - 1));
		return Math.max(1, Math.min(maxLevel, computedLevel));
	}

	public int getConfiguredMaxValue() {
		return ConfigManager.getServerConfig().getGameplay().getMaxValue();
	}

	public boolean isMaxLevelValueInsteadOfStats() {
		return ConfigManager.getServerConfig().getGameplay().getMaxLevelValueInsteadOfStats();
	}

	public int getConfiguredMaxTotalStats() {
		long rawMax = getConfiguredMaxTotalStatsRaw();
		return rawMax > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rawMax;
	}

	public int getRemainingAssignableStats() {
		long remaining = (long) getConfiguredMaxTotalStats() - stats.getTotalStats();
		return remaining <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, remaining);
	}

	public int getCurrentStatValue(String statName) {
		return switch (statName.toUpperCase()) {
			case "STR" -> stats.getStrength();
			case "SKP" -> stats.getStrikePower();
			case "RES" -> stats.getResistance();
			case "VIT" -> stats.getVitality();
			case "PWR" -> stats.getKiPower();
			case "ENE" -> stats.getEnergy();
			default -> 0;
		};
	}

	public int getMaxAllowedIncreaseForStat(String statName, int requestedAmount) {
		int safeRequested = Math.max(0, requestedAmount);
		if (safeRequested <= 0) return 0;

		int remainingTotal = getRemainingAssignableStats();
		if (remainingTotal <= 0) return 0;

		int allowedByTotal = Math.min(safeRequested, remainingTotal);
		if (isMaxLevelValueInsteadOfStats()) return allowedByTotal;

		int remainingStat = Math.max(0, getConfiguredMaxValue() - getCurrentStatValue(statName));
		return Math.min(allowedByTotal, remainingStat);
	}

	public int getBattlePower() {
		if (status.isAndroidUpgraded()) return Integer.MAX_VALUE;
		int str = stats.getStrength();
		int skp = stats.getStrikePower();
		int res = stats.getResistance();
		int vit = stats.getVitality();
		int pwr = stats.getKiPower();

		double releaseMultiplier = (double) resources.getPowerRelease() / 100.0;

		return (int) ((str + skp + res + vit + pwr) * releaseMultiplier);
	}

	private double getSecondaryAttributeValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double fallback) {
		if (player == null) return fallback;
		var instance = player.getAttribute(attribute);
		return instance != null ? instance.getValue() : fallback;
	}

	private double getSecondaryAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double fallback) {
		if (player == null) return fallback;
		var instance = player.getAttribute(attribute);
		return instance != null ? instance.getBaseValue() : fallback;
	}

	private double getArmorToughnessValue() {
		if (player == null) return 0.0;
		var toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
		return toughness != null ? toughness.getValue() : 0.0;
	}

	public float getHealthBonus() {
		double vitality = stats.getVitality();
		double vitScaling = getStatScaling("VIT");
		double vitMult = getFormMultiplier("VIT");
		double flatBonusVit = bonusStats.calculateBonus("VIT", (int) Math.round(vitality), false);
		double multBonusVit = bonusStats.calculateBonus("VIT", (int) Math.round(vitality), true);
		return (float) (((vitality + multBonusVit) * vitScaling * vitMult) + (flatBonusVit * vitScaling));
	}

	public float getMaxHealth() {
		return (float) getSecondaryAttributeValue(Attributes.MAX_HEALTH, 20.0);
	}

	public int getMaxEnergy() {
		double energy = stats.getEnergy();
		double eneScaling = getStatScaling("ENE");
		double eneMult = getFormMultiplier("ENE");
		double flatBonusEne = bonusStats.calculateBonus("ENE", (int) Math.round(energy), false);
		double multBonusEne = bonusStats.calculateBonus("ENE", (int) Math.round(energy), true);
		double secondaryMaxEnergy = getSecondaryAttributeValue(MainAttributes.MAX_ENERGY.get(), 20.0);
		return (int) (secondaryMaxEnergy + ((energy + multBonusEne) * eneScaling * eneMult) + (flatBonusEne * eneScaling));
	}

	public int getMaxStamina() {
		double resistance = stats.getResistance();
		double stmScaling = getStatScaling("STM");
		double resMult = getTotalMultiplier("RES");
		double flatBonusRes = bonusStats.calculateBonus("RES", (int) Math.round(resistance), false);
		double multBonusRes = bonusStats.calculateBonus("RES", (int) Math.round(resistance), true);
		double secondaryMaxStamina = getSecondaryAttributeValue(MainAttributes.MAX_STAMINA.get(), 20.0);
		return (int) (secondaryMaxStamina + ((resistance + multBonusRes) * stmScaling * resMult) + (flatBonusRes * stmScaling));
	}

	public int getMaxPoise() {
		double secondaryMaxPoise = getSecondaryAttributeValue(MainAttributes.MAX_POISE.get(), 25.0);
		return (int) (secondaryMaxPoise + getDefense());
	}

	public double getMaxMeleeDamage() {
		double strength = stats.getStrength();
		double strScaling = getStatScaling("STR");
		double strMult = getTotalMultiplier("STR");
		double flatBonusStr = bonusStats.calculateBonus("STR", (int) Math.round(strength), false);
		double multBonusStr = bonusStats.calculateBonus("STR", (int) Math.round(strength), true);
		double secondaryMeleeDamage = getSecondaryAttributeValue(MainAttributes.MELEE_DAMAGE.get(), 1.0);
		return secondaryMeleeDamage + ((strength + multBonusStr) * strScaling * strMult) + (flatBonusStr * strScaling);
	}

	public double getMeleeDamage() {
		double strength = stats.getStrength();
		double strScaling = getStatScaling("STR");
		double strMult = getTotalMultiplier("STR");
		double releaseMultiplier = resources.getPowerRelease() / 100.0;
		double flatBonusStr = bonusStats.calculateBonus("STR", (int) Math.round(strength), false);
		double multBonusStr = bonusStats.calculateBonus("STR", (int) Math.round(strength), true);
		double secondaryMeleeDamage = getSecondaryAttributeValue(MainAttributes.MELEE_DAMAGE.get(), 1.0);
		return secondaryMeleeDamage + (((strength + multBonusStr) * strScaling * strMult) + (flatBonusStr * strScaling)) * releaseMultiplier;
	}

	public double getMaxStrikeDamage() {
		double strikePower = stats.getStrikePower();
		double strength = stats.getStrength();
		double skpScaling = getStatScaling("SKP");
		double strScaling = getStatScaling("STR");
		double skpMult = getTotalMultiplier("SKP");
		double strMult = getTotalMultiplier("STR");

		double flatBonusSkp = bonusStats.calculateBonus("SKP", (int) Math.round(strikePower), false);
		double multBonusSkp = bonusStats.calculateBonus("SKP", (int) Math.round(strikePower), true);
		double flatBonusStr = bonusStats.calculateBonus("STR", (int) Math.round(strength), false);
		double multBonusStr = bonusStats.calculateBonus("STR", (int) Math.round(strength), true);

		double secondaryStrikeDamage = getSecondaryAttributeValue(MainAttributes.STRIKE_DAMAGE.get(), 1.0);
		return secondaryStrikeDamage + ((strikePower + multBonusSkp) * skpScaling * skpMult) + (flatBonusSkp * skpScaling) + (((strength + multBonusStr) * strScaling * strMult) + (flatBonusStr * strScaling)) * 0.25;
	}

	public double getStrikeDamage() {
		double strikePower = stats.getStrikePower();
		double strength = stats.getStrength();
		double skpScaling = getStatScaling("SKP");
		double strScaling = getStatScaling("STR");
		double skpMult = getTotalMultiplier("SKP");
		double strMult = getTotalMultiplier("STR");
		double releaseMultiplier = resources.getPowerRelease() / 100.0;

		double flatBonusSkp = bonusStats.calculateBonus("SKP", (int) Math.round(strikePower), false);
		double multBonusSkp = bonusStats.calculateBonus("SKP", (int) Math.round(strikePower), true);
		double flatBonusStr = bonusStats.calculateBonus("STR", (int) Math.round(strength), false);
		double multBonusStr = bonusStats.calculateBonus("STR", (int) Math.round(strength), true);

		double secondaryStrikeDamage = getSecondaryAttributeValue(MainAttributes.STRIKE_DAMAGE.get(), 1.0);
		double baseDamage = ((strikePower + multBonusSkp) * skpScaling * skpMult) + (flatBonusSkp * skpScaling) + (((strength + multBonusStr) * strScaling * strMult) + (flatBonusStr * strScaling)) * 0.25;
		return secondaryStrikeDamage + baseDamage * releaseMultiplier;
	}

	public double getMaxKiDamage() {
		double kiPower = stats.getKiPower();
		double pwrScaling = getStatScaling("PWR");
		double pwrMult = getTotalMultiplier("PWR");
		double flatBonusPwr = bonusStats.calculateBonus("PWR", (int) Math.round(kiPower), false);
		double multBonusPwr = bonusStats.calculateBonus("PWR", (int) Math.round(kiPower), true);
		double secondaryKiDamage = getSecondaryAttributeValue(MainAttributes.KI_DAMAGE.get(), 0.0);
		return secondaryKiDamage + ((kiPower + multBonusPwr) * pwrScaling * pwrMult) + (flatBonusPwr * pwrScaling);
	}

	public double getKiDamage() {
		double kiPower = stats.getKiPower();
		double pwrScaling = getStatScaling("PWR");
		double pwrMult = getTotalMultiplier("PWR");
		double releaseMultiplier = resources.getPowerRelease() / 100.0;
		double flatBonusPwr = bonusStats.calculateBonus("PWR", (int) Math.round(kiPower), false);
		double multBonusPwr = bonusStats.calculateBonus("PWR", (int) Math.round(kiPower), true);
		double secondaryKiDamage = getSecondaryAttributeValue(MainAttributes.KI_DAMAGE.get(), 0.0);
		return secondaryKiDamage + (((kiPower + multBonusPwr) * pwrScaling * pwrMult) + (flatBonusPwr * pwrScaling)) * releaseMultiplier;
	}

	public double getMaxDefense() {
		double resistance = stats.getResistance();
		double defScaling = getStatScaling("DEF");
		double flatBonusRes = bonusStats.calculateBonus("RES", (int) Math.round(resistance), false);
		double multBonusRes = bonusStats.calculateBonus("RES", (int) Math.round(resistance), true);
		double armor = player.getArmorValue();
		double toughness = getArmorToughnessValue();
		double secondaryDefense = getSecondaryAttributeValue(MainAttributes.DEFENSE.get(), 0.0);
		return secondaryDefense + ((resistance + multBonusRes) * defScaling) + (flatBonusRes * defScaling) + armor * 0.75 + toughness;
	}

	public double getDefense() {
		double resistance = stats.getResistance();
		double defScaling = getStatScaling("DEF");
		double releaseMultiplier = resources.getPowerRelease() / 100.0;
		double flatBonusRes = bonusStats.calculateBonus("RES", (int) Math.round(resistance), false);
		double multBonusRes = bonusStats.calculateBonus("RES", (int) Math.round(resistance), true);
		double armor = player.getArmorValue();
		double toughness = getArmorToughnessValue();
		double secondaryDefense = getSecondaryAttributeValue(MainAttributes.DEFENSE.get(), 0.0);
		return (secondaryDefense + ((resistance + multBonusRes) * defScaling) + (flatBonusRes * defScaling) + (armor * 0.75) + toughness) * releaseMultiplier;
	}

	public double calculatePostMitigationDamage(double incomingDamage, boolean isGuardBroken, double armorPenetration) {
		double resTotalMult = getTotalMultiplier("RES");
		double baseDefense = getDefense();

		if (isGuardBroken) baseDefense *= (1.0 - ConfigManager.getCombatConfig().getDefenseDecayOnGuardBreak());
		if (baseDefense > 0) baseDefense *= (1.0 - armorPenetration);

		int maxValue = getConfiguredMaxValue();
		double expectedMaxStats = isMaxLevelValueInsteadOfStats() ? (maxValue * 6.0) / 2.0 : maxValue;
		double expectedMaxDef = expectedMaxStats * getStatScaling("DEF");
		double k_factor = Math.max(100.0, expectedMaxDef * 0.25);

		double baseReduction;
		if (baseDefense >= 0) baseReduction = baseDefense / (k_factor + baseDefense);
		else baseReduction = baseDefense / (k_factor - baseDefense);

		double baseCap = ConfigManager.getCombatConfig().getBaseDamageReductionCap();
		baseReduction = Math.min(baseReduction, baseCap);

		double remainingDamage = incomingDamage * (1.0 - baseReduction);

		if (resTotalMult > 1.0) remainingDamage /= resTotalMult;

		int totalProtection = 0;
		if (player != null) totalProtection = EnchantmentHelper.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, player);

		double enchReduction = 0.0;
		if (totalProtection > 0) {
			double effectiveProtection = totalProtection * (1.0 - armorPenetration);

			double k_ench = 20.0;
			enchReduction = effectiveProtection / (k_ench + effectiveProtection);

			double totalCap = ConfigManager.getCombatConfig().getEnchantmentDamageReductionCap();
			double maxEnchReductionAllowed = (totalCap - baseReduction) / (1.0 - baseReduction);

			enchReduction = Math.min(enchReduction, Math.max(0, maxEnchReductionAllowed));
		}

		return remainingDamage * (1.0 - enchReduction);
	}

	public double getTotalMultiplier(String statName) {
		double form = getFormMultiplier(statName);
		double stack = getStackFormMultiplier(statName);
		double effect = getEffectsMultiplier(statName);

		if (ConfigManager.getServerConfig().getGameplay().getMultiplicationInsteadOfAdditionForMultipliers())
			return form * stack * effect;
		else return 1.0 + (form - 1.0) + (stack - 1.0) + (effect - 1.0);
	}

	public double getFormMultiplier(String statName) {
		String currentForm = character.getActiveForm();
		String currentFormGroup = character.getActiveFormGroup();

		if (currentForm == null || currentForm.isEmpty() || currentForm.equals("base")) return 1.0;
		if (currentFormGroup == null || currentFormGroup.isEmpty()) return 1.0;

		var formConfig = ConfigManager.getFormGroup(character.getRaceName(), currentFormGroup);
		if (formConfig == null) return 1.0;

		var formData = formConfig.getForm(currentForm);
		if (formData == null) return 1.0;

		double baseMult = switch (statName.toUpperCase()) {
			case "STR" -> formData.getStrMultiplier();
			case "SKP" -> formData.getSkpMultiplier();
			case "RES" -> (formData.getDefMultiplier() + formData.getStmMultiplier()) / 2.0;
			case "VIT" -> formData.getVitMultiplier();
			case "PWR" -> formData.getPwrMultiplier();
			case "ENE" -> formData.getEneMultiplier();
			default -> 1.0;
		};

		double mastery = character.getFormMasteries().getMastery(currentFormGroup, currentForm);
		double masteryBonus = mastery * formData.getStatMultPerMasteryPoint();

		return 1.0 + ((baseMult - 1.0) * (1.0 + masteryBonus));
	}

	public double getStackFormMultiplier(String statName) {
		String currentForm = character.getActiveStackForm();
		String currentFormGroup = character.getActiveStackFormGroup();

		if (currentForm == null || currentForm.isEmpty()) return 1.0;
		if (currentFormGroup == null || currentFormGroup.isEmpty()) return 1.0;

		var formConfig = ConfigManager.getStackFormGroup(currentFormGroup);
		if (formConfig == null) return 1.0;

		var formData = formConfig.getForm(currentForm);
		if (formData == null) return 1.0;

		double baseMult = switch (statName.toUpperCase()) {
			case "STR" -> formData.getStrMultiplier();
			case "SKP" -> formData.getSkpMultiplier();
			case "RES" -> (formData.getDefMultiplier() + formData.getStmMultiplier()) / 2.0;
			case "VIT" -> formData.getVitMultiplier();
			case "PWR" -> formData.getPwrMultiplier();
			case "ENE" -> formData.getEneMultiplier();
			default -> 1.0;
		};

		double mastery = character.getStackFormMasteries().getMastery(currentFormGroup, currentForm);
		double masteryBonus = mastery * formData.getStatMultPerMasteryPoint();

		return 1.0 + ((baseMult - 1.0) * (1.0 + masteryBonus));
	}

	public double getEffectsMultiplier(String statName) {
		double rawEffect = effects.getTotalEffectMultiplier();

		return switch (statName.toUpperCase()) {
			case "STR", "SKP", "PWR" -> rawEffect;
			case "DEF" -> 1.0 + ((rawEffect - 1.0) * 0.5);
			default -> 1.0;
		};
	}

	public double getAdjustedStaminaDrainMultiplier() {
		if (!character.hasActiveForm() && !character.hasActiveStackForm()) return 1.0;

		var formData = character.getActiveFormData();
		var stackFormData = character.getActiveStackFormData();
		if (formData == null && stackFormData == null) return 1.0;

		double adjustedBaseDrain = 0;
		if (character.hasActiveForm() && formData != null) {
			double baseDrain = formData.getStaminaDrainMultiplier();
			if (character.hasActiveStackForm() && stackFormData != null) {
				baseDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();
			}
			double mastery = character.getFormMasteries().getMastery(character.getActiveFormGroup(), character.getActiveForm());
			double divisor = 1.0 + (mastery * formData.getCostDecreasePerMasteryPoint());
			adjustedBaseDrain = baseDrain / divisor;
		}

		double adjustedStackDrain = 0;
		if (character.hasActiveStackForm() && stackFormData != null) {
			double stackDrain = stackFormData.getStackDrainMultiplier();
			if (character.hasActiveForm() && formData != null) {
				stackDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();
			}
			double stackMastery = character.getStackFormMasteries().getMastery(character.getActiveStackFormGroup(), character.getActiveStackForm());
			double stackDivisor = 1.0 + (stackMastery * stackFormData.getCostDecreasePerMasteryPoint());
			adjustedStackDrain = stackDrain / stackDivisor;
		}

		return Math.max(0.001, adjustedBaseDrain + adjustedStackDrain);
	}

	public double getAdjustedEnergyDrain() {
		if (!character.hasActiveForm() && !character.hasActiveStackForm()) return 0.0;

		var formData = character.getActiveFormData();
		var stackFormData = character.getActiveStackFormData();
		var powerRelease = resources.getPowerRelease() / 100.0;
		if (formData == null && stackFormData == null) return 0.0;

		double adjustedBaseDrain = 0;
		if (character.hasActiveForm() && formData != null) {
			double baseDrain = formData.getEnergyDrain();
			if (character.hasActiveStackForm() && stackFormData != null) {
				baseDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();
			}
			double mastery = character.getFormMasteries().getMastery(character.getActiveFormGroup(), character.getActiveForm());
			double divisor = 1.0 + (mastery * formData.getCostDecreasePerMasteryPoint());
			adjustedBaseDrain = (baseDrain / divisor) * powerRelease;
		}

		double adjustedStackDrain = 0;
		if (character.hasActiveStackForm() && stackFormData != null) {
			double stackDrain = stackFormData.getEnergyDrain();
			if (character.hasActiveForm() && formData != null) {
				stackDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();
			}
			double stackMastery = character.getStackFormMasteries().getMastery(character.getActiveStackFormGroup(), character.getActiveStackForm());
			double stackDivisor = 1.0 + (stackMastery * stackFormData.getCostDecreasePerMasteryPoint());
			adjustedStackDrain = (stackDrain / stackDivisor) * powerRelease;
		}

		double drainAmount = adjustedBaseDrain + adjustedStackDrain;
		return drainAmount != 0 ? Math.max(1, drainAmount * ConfigManager.getCombatConfig().getBaselineFormDrain()) : 0;
	}

	public double getAdjustedStaminaDrain() {
		if (!character.hasActiveForm() && !character.hasActiveStackForm()) return 0.0;

		var formData = character.getActiveFormData();
		var stackFormData = character.getActiveStackFormData();
		var powerRelease = resources.getPowerRelease() / 100.0;
		if (formData == null && stackFormData == null) return 0.0;

		double adjustedBaseDrain = 0;
		if (character.hasActiveForm() && formData != null) {
			double baseDrain = formData.getStaminaDrain();
			if (character.hasActiveStackForm() && stackFormData != null) {
				baseDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();
			}
			double mastery = character.getFormMasteries().getMastery(character.getActiveFormGroup(), character.getActiveForm());
			double divisor = 1.0 + (mastery * formData.getCostDecreasePerMasteryPoint());
			adjustedBaseDrain = (baseDrain / divisor) * powerRelease;
		}

		double adjustedStackDrain = 0;
		if (character.hasActiveStackForm() && stackFormData != null) {
			double stackDrain = stackFormData.getStaminaDrain();
			if (character.hasActiveForm() && formData != null) {
				stackDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();
			}
			double stackMastery = character.getStackFormMasteries().getMastery(character.getActiveStackFormGroup(), character.getActiveStackForm());
			double stackDivisor = 1.0 + (stackMastery * stackFormData.getCostDecreasePerMasteryPoint());
			adjustedStackDrain = (stackDrain / stackDivisor) * powerRelease;
		}

		double drainAmount = adjustedBaseDrain + adjustedStackDrain;
		return drainAmount != 0 ? Math.max(1, drainAmount * ConfigManager.getCombatConfig().getBaselineFormDrain()) : 0;
	}

	public double getAdjustedHealthDrain() {
		if (!character.hasActiveForm() && !character.hasActiveStackForm()) {
			return 0.0;
		}

		var formData = character.getActiveFormData();
		var stackFormData = character.getActiveStackFormData();
		var powerRelease = resources.getPowerRelease() / 100.0;
		if (formData == null && stackFormData == null) {
			return 0.0;
		}

		double adjustedBaseDrain = 0;
		if (character.hasActiveForm() && formData != null) {
			double baseDrain = formData.getHealthDrain();
			if (character.hasActiveStackForm() && stackFormData != null) {
				baseDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();
			}
			double mastery = character.getFormMasteries().getMastery(character.getActiveFormGroup(), character.getActiveForm());
			double divisor = 1.0 + (mastery * formData.getCostDecreasePerMasteryPoint());
			adjustedBaseDrain = (baseDrain / divisor) * powerRelease;
		}

		double adjustedStackDrain = 0;
		if (character.hasActiveStackForm() && stackFormData != null) {
			double stackDrain = stackFormData.getHealthDrain();
			if (character.hasActiveForm() && formData != null) {
				stackDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();
			}
			double stackMastery = character.getStackFormMasteries().getMastery(character.getActiveStackFormGroup(), character.getActiveStackForm());
			double stackDivisor = 1.0 + (stackMastery * stackFormData.getCostDecreasePerMasteryPoint());
			adjustedStackDrain = (stackDrain / stackDivisor) * powerRelease;
		}

		double drainAmount = adjustedBaseDrain + adjustedStackDrain;
		return drainAmount != 0 ? Math.max(1, drainAmount * ConfigManager.getCombatConfig().getBaselineFormDrain()) : 0;
	}


	public void initializeWithRaceAndClass(String raceName, String characterClass, String gender,
										   int hairId, com.dragonminez.common.hair.CustomHair customHair,
										   int bodyType, int eyesType, int noseType, int mouthType, int tattooType,
									   String activeHeadBone, String hairColor, String bodyColor, String bodyColor2, String bodyColor3,
										   String eye1Color, String eye2Color, String auraColor) {
		character.setRace(raceName);
		character.setGender(gender);
		character.setCharacterClass(characterClass);
		character.setHairId(hairId);
		if (customHair != null) character.setHairBase(customHair);
		character.setBodyType(bodyType);
		character.setEyesType(eyesType);
		character.setNoseType(noseType);
		character.setMouthType(mouthType);
		character.setTattooType(tattooType);
		character.setActiveHeadBone(activeHeadBone);
		character.setHairColor(hairColor);
		character.setBodyColor(bodyColor);
		character.setBodyColor2(bodyColor2);
		character.setBodyColor3(bodyColor3);
		character.setEye1Color(eye1Color);
		character.setEye2Color(eye2Color);
		character.setAuraColor(auraColor);
		status.setHasCreatedCharacter(true);

		RaceStatsConfig raceConfig = ConfigManager.getRaceStats(raceName);
		RaceStatsConfig.ClassStats classStats = getClassStats(raceConfig, characterClass);
		RaceStatsConfig.BaseStats baseStats = classStats.getBaseStats();

		if (baseStats == null) baseStats = new RaceStatsConfig().getClassStats(characterClass).getBaseStats();

		boolean hasDefaultStats = stats.getStrength() <= 5 && stats.getStrikePower() <= 5 &&
				stats.getResistance() <= 5 && stats.getVitality() <= 5 &&
				stats.getKiPower() <= 5 && stats.getEnergy() <= 5;

		if (hasDefaultStats) {
			stats.setStrength(baseStats.getStrength());
			stats.setStrikePower(baseStats.getStrikePower());
			stats.setResistance(baseStats.getResistance());
			stats.setVitality(baseStats.getVitality());
			stats.setKiPower(baseStats.getKiPower());
			stats.setEnergy(baseStats.getEnergy());
		}

		resources.setCurrentEnergy(getMaxEnergy());
		resources.setCurrentStamina(getMaxStamina());
		resources.setCurrentPoise(getMaxPoise());
		resources.setPowerRelease(5);
		resources.setAlignment(100);
		character.setSelectedFormGroup(TransformationsHelper.getGroupWithFirstAvailableForm(this));

		updateTransformationSkillLimits(raceName);
	}

	public void updateTransformationSkillLimits(String raceName) {
		skills.refreshNonFormSkillMaxLevels();

		RaceCharacterConfig charConfig = ConfigManager.getRaceCharacter(raceName);
		if (charConfig != null) {
			Collection<String> formSkills = charConfig.getFormSkills();
			List<String> androidBlacklistedForms = ConfigManager.getSkillsConfig().getAndroidBlacklistedForms();
			for (String skillName : formSkills) {
				if (status.isAndroidUpgraded() && androidBlacklistedForms.contains(skillName)) {
					continue;
				}
				if (!status.isAndroidUpgraded() && "androidforms".equalsIgnoreCase(skillName)) {
					continue;
				}
				Integer[] tpCosts = charConfig.getFormSkillTpCosts(skillName);
				int maxLevel = tpCosts != null ? tpCosts.length : 0;
				skills.registerDefaultSkill(skillName, maxLevel);
			}
		}
	}

	public double getStatScaling(String statName) {
		String raceName = character.getRaceName();
		String characterClass = character.getCharacterClass();

		RaceStatsConfig raceConfig = ConfigManager.getRaceStats(raceName);
		RaceStatsConfig.ClassStats classStats = getClassStats(raceConfig, characterClass);
		RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();

		if (scaling == null) return switch (statName.toUpperCase()) {
			case "STR" -> new RaceStatsConfig().getClassStats(characterClass).getStatScaling().getStrengthScaling();
			case "SKP" -> new RaceStatsConfig().getClassStats(characterClass).getStatScaling().getStrikePowerScaling();
			case "STM" -> new RaceStatsConfig().getClassStats(characterClass).getStatScaling().getStaminaScaling();
			case "DEF" -> new RaceStatsConfig().getClassStats(characterClass).getStatScaling().getDefenseScaling();
			case "VIT" -> new RaceStatsConfig().getClassStats(characterClass).getStatScaling().getVitalityScaling();
			case "PWR" -> new RaceStatsConfig().getClassStats(characterClass).getStatScaling().getKiPowerScaling();
			case "ENE" -> new RaceStatsConfig().getClassStats(characterClass).getStatScaling().getEnergyScaling();
			default -> 1.0;
		};

		return switch (statName.toUpperCase()) {
			case "STR" -> scaling.getStrengthScaling();
			case "SKP" -> scaling.getStrikePowerScaling();
			case "STM" -> scaling.getStaminaScaling();
			case "DEF" -> scaling.getDefenseScaling();
			case "VIT" -> scaling.getVitalityScaling();
			case "PWR" -> scaling.getKiPowerScaling();
			case "ENE" -> scaling.getEnergyScaling();
			default -> 1.0;
		};
	}

	private RaceStatsConfig.ClassStats getClassStats(RaceStatsConfig config, String characterClass) {
		if (config == null) return new RaceStatsConfig().getClassStats(characterClass);
		return config.getClassStats(characterClass);
	}

	private int getInitialTotalStats() {
		String raceName = character.getRaceName();
		String characterClass = character.getCharacterClass();

		RaceStatsConfig raceConfig = ConfigManager.getRaceStats(raceName);
		RaceStatsConfig.ClassStats classStats = getClassStats(raceConfig, characterClass);
		RaceStatsConfig.BaseStats baseStats = classStats.getBaseStats();

		if (baseStats == null) baseStats = new RaceStatsConfig().getClassStats(characterClass).getBaseStats();

		return baseStats.getStrength() + baseStats.getStrikePower() +
				baseStats.getResistance() + baseStats.getVitality() +
				baseStats.getKiPower() + baseStats.getEnergy();
	}

	private long getConfiguredMaxTotalStatsRaw() {
		return getConfiguredMaxValue() * 6L;
	}

	public double getRaceTpCostMultiplier() {
		String raceName = character.getRaceName();
		String characterClass = character.getCharacterClass();
		RaceStatsConfig raceConfig = ConfigManager.getRaceStats(raceName);
		if (raceConfig == null) return 1.0;
		RaceStatsConfig.ClassStats classStats = raceConfig.getClassStats(characterClass);
		if (classStats == null) return 1.0;
		Double classMult = classStats.getTpCostMultiplier();
		return classMult != null ? classMult : 1.0;
	}

	public double getTpAdditiveMultiplier() {
		double additiveMultiplier = 1.0;
		additiveMultiplier += (getTpClassMultiplier() - 1.0);
		additiveMultiplier += (getTpFrostDemonMultiplier() - 1.0);
		additiveMultiplier += (getTpHTCMultiplier() - 1.0);
		if (ConfigManager.getServerConfig().getGameplay() != null)
			if (ConfigManager.getServerConfig().getGameplay().getGravityBonusEnabled())
				additiveMultiplier += (getTpGravityMultiplier() - 1.0);
		return Math.max(0.0, additiveMultiplier);
	}

	public double getTpGlobalMultiplier() {
		return ConfigManager.getServerConfig().getGameplay().getTpsGainMultiplier();
	}

	public double getTpClassMultiplier() {
		String race = character.getRace();
		var raceStats = ConfigManager.getRaceStats(race);
		if (raceStats == null) return 1.0;
		var classStats = raceStats.getClassStats(character.getCharacterClass());
		if (classStats == null) return 1.0;
		Double classMult = classStats.getTpGainMultiplier();
		return classMult != null ? classMult : 1.0;
	}

	public boolean isFrostDemonTpPassiveActive() {
		return ConfigManager.getServerConfig().getRacialSkills().getEnableRacialSkills()
				&& ConfigManager.getServerConfig().getRacialSkills().getFrostDemonRacialSkill()
				&& "frostdemon".equals(character.getRace());
	}

	public double getTpFrostDemonMultiplier() {
		if (!isFrostDemonTpPassiveActive()) return 1.0;
		return ConfigManager.getServerConfig().getRacialSkills().getFrostDemonTPBoost();
	}

	public double getTpHTCMultiplier() {
		if (!player.level().dimension().equals(HTCDimension.HTC_KEY)) return 1.0;
		return ConfigManager.getServerConfig().getGameplay().getHTCTpMultiplier();
	}

	public double getTpGravityMultiplier() {
		if (player.level().dimension().equals(HTCDimension.HTC_KEY)) return 1.0;
		double bonusGravity = GravityLogic.getBonusGravity(player);
		if (bonusGravity <= 0) return 1.0;
		return 1.0 + (bonusGravity * 0.05);
	}

	public double getTpPotionEffectMultiplier() {
		if (player == null) return 1.0;
		return PotionEffectHelper.getMultiplierFromEffect(player, MainEffects.TP_GAIN.get(), "tp_gain");
	}

	public double getTpTotalMultiplier() {
		return getTpAdditiveMultiplier() * getTpGlobalMultiplier() * getTpPotionEffectMultiplier();
	}

	public int calculateTPGain(int baseTP) {
		if (baseTP <= 0) return 0;
		double total = baseTP * getTpTotalMultiplier();
		return (int) Math.max(0.0, total);
	}

	public int getSingleStatCost(int simulatedTotalStats) {
		double globalMult = ConfigManager.getServerConfig().getGameplay().getGlobalTpCostMultiplier();
		double raceMult = getRaceTpCostMultiplier();
		double totalMult = globalMult * raceMult;

		int minCost = ConfigManager.getServerConfig().getGameplay().getMinTPCost();
		int discountThreshold = ConfigManager.getServerConfig().getGameplay().getMaxTPDiscount();

		double baseCost = minCost + (simulatedTotalStats * 1.25);

		int earlyGameDiscount = 0;
		if (simulatedTotalStats < discountThreshold) earlyGameDiscount = discountThreshold - simulatedTotalStats;

		int finalCost = (int) (baseCost * totalMult) - earlyGameDiscount;
		return Math.max(minCost, finalCost);
	}

	public int calculateRecursiveCost(int statsToAdd, int maxStats) {
		int totalCost = 0;
		int currentTotalStats = stats.getTotalStats();
		int totalCap = getConfiguredMaxTotalStats();

		for (int i = 0; i < statsToAdd; i++) {
			if (currentTotalStats + i >= totalCap) break;
			totalCost += getSingleStatCost(currentTotalStats + i);
		}
		return totalCost;
	}

	public int calculateStatIncrease(int maxStatsToAdd, float availableTPs, int maxStats) {
		int statsIncreased = 0;
		int costAccumulated = 0;
		int currentTotalStats = stats.getTotalStats();
		int totalCap = getConfiguredMaxTotalStats();

		while (statsIncreased < maxStatsToAdd) {
			if (currentTotalStats + statsIncreased >= totalCap) break;

			int costForNext = getSingleStatCost(currentTotalStats + statsIncreased);

			if (costAccumulated + costForNext > availableTPs) break;

			costAccumulated += costForNext;
			statsIncreased++;
		}

		return statsIncreased;
	}

	public void tick() {
		cooldowns.tick();
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		nbt.put("Stats", stats.save());
		nbt.put("Status", status.save());
		nbt.put("Cooldowns", cooldowns.save());
		nbt.put("Character", character.save());
		nbt.put("Resources", resources.save());
		nbt.put("Skills", skills.save());
		nbt.put("Effects", effects.save());
		nbt.put("PlayerQuestData", playerQuestData.serializeNBT());
		nbt.put("BonusStats", bonusStats.save());
		nbt.put("Training", training.save());
		nbt.put("Techniques",  techniques.save());
		nbt.putBoolean("HasInitializedHealth", hasInitializedHealth);
		return nbt;
	}

	public void load(CompoundTag nbt) throws ClassNotFoundException {
		if (nbt.contains("Stats")) stats.load(nbt.getCompound("Stats"));
		if (nbt.contains("Status")) status.load(nbt.getCompound("Status"));
		if (nbt.contains("Cooldowns")) cooldowns.load(nbt.getCompound("Cooldowns"));
		if (nbt.contains("Character")) character.load(nbt.getCompound("Character"));
		if (nbt.contains("Resources")) resources.load(nbt.getCompound("Resources"));
		if (nbt.contains("Skills")) skills.load(nbt.getCompound("Skills"));
		if (nbt.contains("Effects")) effects.load(nbt.getCompound("Effects"));
		if (nbt.contains("PlayerQuestData")) playerQuestData.deserializeNBT(nbt.getCompound("PlayerQuestData"));
		else throw new ClassNotFoundException("PlayerQuestData not found in NBT. This is required for quest progression to work correctly. " +
				"Please update the mod or re-generate your config files.");
		if (nbt.contains("BonusStats")) bonusStats.load(nbt.getCompound("BonusStats"));
		if (nbt.contains("Training")) training.load(nbt.getCompound("Training"));
		if (nbt.contains("Techniques")) techniques.load(nbt.getCompound("Techniques"));
		if (nbt.contains("HasInitializedHealth")) hasInitializedHealth = nbt.getBoolean("HasInitializedHealth");
		if (character.getRaceName() != null && !character.getRaceName().isEmpty()) updateTransformationSkillLimits(character.getRaceName());
		this.isDataLoaded = true;
	}

	public void copyFrom(StatsData other) {
		this.stats.copyFrom(other.stats);
		this.status.copyFrom(other.status);
		this.cooldowns.copyFrom(other.cooldowns);
		this.character.copyFrom(other.character);
		this.resources.copyFrom(other.resources);
		this.skills.copyFrom(other.skills);
		this.effects.copyFrom(other.effects);
		this.playerQuestData.deserializeNBT(other.playerQuestData.serializeNBT());
		this.bonusStats.copyFrom(other.bonusStats);
		this.training.copyFrom(other.training);
		this.techniques.copyFrom(other.techniques);
		this.hasInitializedHealth = other.hasInitializedHealth;
		if (character.getRaceName() != null && !character.getRaceName().isEmpty())
			updateTransformationSkillLimits(character.getRaceName());
		this.isDataLoaded = true;
	}
}