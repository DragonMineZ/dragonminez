package com.dragonminez.common.stats;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.MainEnchants;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.stats.character.*;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.DynamicGrowthData;
import com.dragonminez.common.stats.skills.Skills;
import com.dragonminez.common.stats.techniques.Techniques;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.common.util.lists.StackForms;
import com.dragonminez.server.util.GravityLogic;
import com.dragonminez.server.util.PotionEffectHelper;
import com.dragonminez.server.world.dimension.HTCDimension;
import com.dragonminez.server.events.players.StatsEvents;
import com.dragonminez.server.events.players.TickHandler;
import com.dragonminez.server.util.FusionLogic;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
	private final SecondaryStatEffects secondaryStatEffects;
	private final PlayerQuestData playerQuestData;
	private final BonusStats bonusStats;
	private final Techniques techniques;
	private final DynamicGrowthData dynamicGrowth;

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
		this.resources.setStatsData(this);
		this.skills = new Skills();
		this.effects = new Effects();
		this.secondaryStatEffects = new SecondaryStatEffects();
		this.playerQuestData = new PlayerQuestData();
		this.bonusStats = new BonusStats();
		this.techniques = new Techniques();
		this.dynamicGrowth = new DynamicGrowthData();
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

	public float getMaxEnergy() {
		double energy = stats.getEnergy();
		double eneScaling = getStatScaling("ENE");
		double eneMult = getFormMultiplier("ENE");
		double flatBonusEne = bonusStats.calculateBonus("ENE", (int) Math.round(energy), false);
		double multBonusEne = bonusStats.calculateBonus("ENE", (int) Math.round(energy), true);
		double secondaryMaxEnergy = getSecondaryAttributeValue(MainAttributes.MAX_ENERGY.get(), 20.0);
		return Math.min((float) (secondaryMaxEnergy + ((energy + multBonusEne) * eneScaling * eneMult) + (flatBonusEne * eneScaling)), Float.MAX_VALUE - 1);
	}

	public float getMaxStamina() {
		double resistance = stats.getResistance();
		double stmScaling = getStatScaling("STM");
		double stmMult = getTotalMultiplier("STM");
		double flatBonusStm = bonusStats.calculateBonus("STM", (int) Math.round(resistance), false);
		double multBonusStm = bonusStats.calculateBonus("STM", (int) Math.round(resistance), true);
		double secondaryMaxStamina = getSecondaryAttributeValue(MainAttributes.MAX_STAMINA.get(), 20.0);
		return Math.min((float) (secondaryMaxStamina + ((resistance + multBonusStm) * stmScaling * stmMult) + (flatBonusStm * stmScaling)), Float.MAX_VALUE - 1);
	}

	public double getStaminaRegenPerSecond() {
		RaceStatsConfig raceConfig = ConfigManager.getRaceStats(character.getRaceName());
		RaceStatsConfig.ClassStats classStats = getClassStats(raceConfig, character.getCharacterClass());

		int baseVit = stats.getVitality();
		double flatBonusVit = bonusStats.calculateBonus("VIT", baseVit, false);
		double multBonusVit = bonusStats.calculateBonus("VIT", baseVit, true);
		double vitMult = getFormMultiplier("VIT");
		double effectiveVit = ((baseVit + multBonusVit) * vitMult) + flatBonusVit;
		double sp5 = classStats.getBaseSp5() + (effectiveVit * classStats.getSp5StmScaling());

		int totalEnchLvl = TickHandler.getTotalArmorEnchantmentLevel(MainEnchants.RESISTANCE_RECOVERY.get(), player);
		double enchMult = TickHandler.getRecoveryMultiplier(totalEnchLvl);

		int meditationLevel = skills.getSkillLevel("meditation");
		double meditationBonus = meditationLevel > 0 ? 1.0 + (meditationLevel * 0.05) : 1.0;

		double adjustedStaminaDrain = getAdjustedStaminaDrain();
		double regenMultiplier = 1.0;
		if (adjustedStaminaDrain > 0.0) regenMultiplier = Math.max(0.0, 1.0 - (adjustedStaminaDrain / 50.0));
		else if (adjustedStaminaDrain < 0.0) regenMultiplier = 1.0 + Math.abs(adjustedStaminaDrain);

		double actionMod = (player != null && player.getPersistentData().contains("dmz_stamina_regen_mod"))
				? player.getPersistentData().getDouble("dmz_stamina_regen_mod") : 1.0;

		double regenPerSecond = (sp5 / 5.0) * meditationBonus * enchMult * regenMultiplier * actionMod * secondaryStatEffects.getMultiplier(SecondaryStatEffects.STM_REGEN);
		return PotionEffectHelper.applyStaminaRegenMultiplier(player, regenPerSecond);
	}

	public double getHealthRegenPerSecond() {
		RaceStatsConfig raceConfig = ConfigManager.getRaceStats(character.getRaceName());
		RaceStatsConfig.ClassStats classStats = getClassStats(raceConfig, character.getCharacterClass());

		int baseVit = stats.getVitality();
		double flatBonusVit = bonusStats.calculateBonus("VIT", baseVit, false);
		double multBonusVit = bonusStats.calculateBonus("VIT", baseVit, true);
		double vitMult = getFormMultiplier("VIT");
		double effectiveVit = ((baseVit + multBonusVit) * vitMult) + flatBonusVit;
		double hp5 = classStats.getBaseHp5() + (effectiveVit * classStats.getHp5VitScaling());

		int totalEnchLvl = TickHandler.getTotalArmorEnchantmentLevel(MainEnchants.VITALITY_RECOVERY.get(), player);
		double enchMult = TickHandler.getRecoveryMultiplier(totalEnchLvl);

		double adjustedHealthDrain = getAdjustedHealthDrain();
		double regenMultiplier = 1.0;
		if (adjustedHealthDrain > 0.0) regenMultiplier = Math.max(0.0, 1.0 - (adjustedHealthDrain / 10.0));
		else if (adjustedHealthDrain < 0.0) regenMultiplier = 1.0 + Math.abs(adjustedHealthDrain);

		return (hp5 / 5.0) * enchMult * regenMultiplier * secondaryStatEffects.getMultiplier(SecondaryStatEffects.HP_REGEN);
	}

	public double getEnergyRegenPerSecond(boolean activeCharging) {
		RaceStatsConfig raceConfig = ConfigManager.getRaceStats(character.getRaceName());
		RaceStatsConfig.ClassStats classStats = getClassStats(raceConfig, character.getCharacterClass());

		float currentEnergy = resources.getCurrentEnergy();
		float maxEnergy = getMaxEnergy();

		boolean hasActiveForm = character.hasActiveForm();
		FormConfig.FormData activeForm = hasActiveForm ? character.getActiveFormData() : null;
		boolean hasActiveStackForm = character.hasActiveStackForm();
		FormConfig.FormData activeStackForm = hasActiveStackForm ? character.getActiveStackFormData() : null;

		int baseEne = stats.getEnergy();
		double flatBonusEne = bonusStats.calculateBonus("ENE", baseEne, false);
		double multBonusEne = bonusStats.calculateBonus("ENE", baseEne, true);
		double eneMult = getFormMultiplier("ENE");
		double effectiveEne = ((baseEne + multBonusEne) * eneMult) + flatBonusEne;
		double ep5 = classStats.getBaseEp5() + (effectiveEne * classStats.getEp5EneScaling());

		int totalEnchLvl = TickHandler.getTotalArmorEnchantmentLevel(MainEnchants.ENERGY_RECOVERY.get(), player);
		double enchMult = TickHandler.getRecoveryMultiplier(totalEnchLvl);

		int meditationLevel = skills.getSkillLevel("meditation");
		double meditationBonus = meditationLevel > 0 ? 1.0 + (meditationLevel * 0.05) : 1.0;

		double kiConductivityMult = TickHandler.getRecoveryMultiplier(TickHandler.getTotalArmorEnchantmentLevel(MainEnchants.KI_CONDUCTIVITY.get(), player));
		double baseRegenPerSecond = (ep5 / 5.0) * meditationBonus * enchMult * kiConductivityMult;
		double androidRegenMult = isAndroidRacialActive() ? 2.0 : 1.0;

		double energyChange = 0;

		if (activeCharging) {
			int kiBoostLevel = skills.getSkillLevel("kiboost");
			double kiBoostMult = 1.0 + (kiBoostLevel * 0.25);
			double regenAmount = PotionEffectHelper.applyKiRegenMultiplier(player, baseRegenPerSecond * 1.5) * androidRegenMult * kiBoostMult;
			if (regenAmount < 1.0) regenAmount = 1.0;
			energyChange += regenAmount;
		} else if (currentEnergy < maxEnergy) {
			double regenAmount = PotionEffectHelper.applyKiRegenMultiplier(player, baseRegenPerSecond) * androidRegenMult;

			double formRawDrain = 0.0;
			if (hasActiveForm && activeForm != null) formRawDrain = activeForm.getEnergyDrain();
			else if (hasActiveStackForm && activeStackForm != null) formRawDrain = activeStackForm.getEnergyDrain();

			double regenMultiplier = 1.0;
			if (formRawDrain > 0.0) regenMultiplier = Math.max(0.0, 1.0 - (formRawDrain * 2.5));
			else if (formRawDrain < 0.0) regenMultiplier = 1.0 + Math.abs(formRawDrain);

			energyChange += regenAmount * regenMultiplier;
		}

		return energyChange * secondaryStatEffects.getMultiplier(SecondaryStatEffects.ENE_REGEN);
	}

	public float getMaxPoise() {
		double secondaryMaxPoise = getSecondaryAttributeValue(MainAttributes.MAX_POISE.get(), 25.0);
		return Math.min((float) (secondaryMaxPoise + getDefense()), Float.MAX_VALUE - 1);
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

	public double getStrikeDamageNoForms() {
		double strikePower = stats.getStrikePower();
		double strength = stats.getStrength();
		double skpScaling = getStatScaling("SKP");
		double strScaling = getStatScaling("STR");
		double releaseMultiplier = resources.getPowerRelease() / 100.0;

		double flatBonusSkp = bonusStats.calculateBonus("SKP", (int) Math.round(strikePower), false);
		double multBonusSkp = bonusStats.calculateBonus("SKP", (int) Math.round(strikePower), true);
		double flatBonusStr = bonusStats.calculateBonus("STR", (int) Math.round(strength), false);
		double multBonusStr = bonusStats.calculateBonus("STR", (int) Math.round(strength), true);

		double secondaryStrikeDamage = getSecondaryAttributeValue(MainAttributes.STRIKE_DAMAGE.get(), 1.0);
		double baseDamage = ((strikePower + multBonusSkp) * skpScaling) + (flatBonusSkp * skpScaling) + (((strength + multBonusStr) * strScaling) + (flatBonusStr * strScaling)) * 0.25;
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

	public double getKiDamageNoForms() {
		double kiPower = stats.getKiPower();
		double pwrScaling = getStatScaling("PWR");
		double releaseMultiplier = resources.getPowerRelease() / 100.0;
		double flatBonusPwr = bonusStats.calculateBonus("PWR", (int) Math.round(kiPower), false);
		double multBonusPwr = bonusStats.calculateBonus("PWR", (int) Math.round(kiPower), true);
		double secondaryKiDamage = getSecondaryAttributeValue(MainAttributes.KI_DAMAGE.get(), 0.0);
		return secondaryKiDamage + (((kiPower + multBonusPwr) * pwrScaling) + (flatBonusPwr * pwrScaling)) * releaseMultiplier;
	}

	public double getMaxDefense() {
		double resistance = stats.getResistance();
		double defScaling = getStatScaling("DEF");
		double flatBonusDef = bonusStats.calculateBonus("DEF", (int) Math.round(resistance), false);
		double multBonusDef = bonusStats.calculateBonus("DEF", (int) Math.round(resistance), true);
		double armor = player.getArmorValue();
		double toughness = getArmorToughnessValue();
		double secondaryDefense = getSecondaryAttributeValue(MainAttributes.DEFENSE.get(), 0.0);

		double statDef = ((resistance + multBonusDef) * defScaling) + (flatBonusDef * defScaling);
		double armorComponent = (armor * 0.50) + (toughness * 0.70);

		return (secondaryDefense + statDef + armorComponent) * secondaryStatEffects.getMultiplier(SecondaryStatEffects.DEF);
	}

	public double getDefense() {
		double resistance = stats.getResistance();
		double defScaling = getStatScaling("DEF");
		double releaseMultiplier = resources.getPowerRelease() / 100.0;
		double flatBonusDef = bonusStats.calculateBonus("DEF", (int) Math.round(resistance), false);
		double multBonusDef = bonusStats.calculateBonus("DEF", (int) Math.round(resistance), true);
		double armor = player.getArmorValue();
		double toughness = getArmorToughnessValue();
		double secondaryDefense = getSecondaryAttributeValue(MainAttributes.DEFENSE.get(), 0.0);

		double statDef = ((resistance + multBonusDef) * defScaling) + (flatBonusDef * defScaling);
		double armorComponent = (armor * 0.50) + (toughness * 0.70);

		return (secondaryDefense + statDef + armorComponent) * releaseMultiplier * secondaryStatEffects.getMultiplier(SecondaryStatEffects.DEF);
	}

	public double calculatePostMitigationDamage(double incomingDamage, boolean isGuardBroken, double armorPenetration) {
		double defMult = getTotalMultiplier("DEF");
		double baseDefense = getDefense();

		if (isGuardBroken) baseDefense *= (1.0 - ConfigManager.getCombatConfig().getDefenseDecayOnGuardBreak());
		if (baseDefense > 0) baseDefense *= (1.0 - armorPenetration);

		double flatMitigation = baseDefense * 0.10 * Math.max(1.0, defMult);
		double postFlatDamage = Math.max(0.0, incomingDamage - flatMitigation);

		int maxValue = getConfiguredMaxValue();
		double expectedMaxStats = isMaxLevelValueInsteadOfStats() ? (maxValue * 6.0) / 2.0 : maxValue;
		double expectedMaxDef = expectedMaxStats * getStatScaling("DEF");
		double k_factor = Math.max(100.0, expectedMaxDef * 0.25);

		double baseReduction;
		if (baseDefense >= 0) baseReduction = baseDefense / (k_factor + baseDefense);
		else baseReduction = baseDefense / (k_factor - baseDefense);

		double baseCap = ConfigManager.getCombatConfig().getBaseDamageReductionCap();
		baseReduction = Math.min(baseReduction, baseCap);

		double remainingDamage = postFlatDamage * (1.0 - baseReduction);

		if (defMult > 1.0) remainingDamage /= (1.0 + (defMult - 1.0) * 0.20);

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
		double secondary = statName.equalsIgnoreCase("DEF") ? 1.0 : secondaryStatEffects.getMultiplier(statName);

		if (ConfigManager.getServerConfig().getGameplay().getMultiplicationInsteadOfAdditionForMultipliers()) return form * stack * effect * secondary;
		else return 1.0 + (form - 1.0) + (stack - 1.0) + (effect - 1.0) + (secondary - 1.0);
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
			case "STM" -> formData.getStmMultiplier();
			case "DEF" -> formData.getDefMultiplier();
			case "RES" -> (formData.getDefMultiplier() + formData.getStmMultiplier()) / 2.0;
			case "VIT" -> formData.getVitMultiplier();
			case "PWR" -> formData.getPwrMultiplier();
			case "ENE" -> formData.getEneMultiplier();
			default -> 1.0;
		};

		double mastery = character.getFormMasteries().getMastery(currentFormGroup, currentForm);
		return applyMasteryStatBonus(formData, baseMult, mastery);
	}

	private double getBaseFormMultiplier(FormConfig.FormData formData, String statName) {
		return switch (statName.toUpperCase()) {
			case "STR" -> formData.getStrMultiplier();
			case "SKP" -> formData.getSkpMultiplier();
			case "STM" -> formData.getStmMultiplier();
			case "DEF" -> formData.getDefMultiplier();
			case "RES" -> (formData.getDefMultiplier() + formData.getStmMultiplier()) / 2.0;
			case "VIT" -> formData.getVitMultiplier();
			case "PWR" -> formData.getPwrMultiplier();
			case "ENE" -> formData.getEneMultiplier();
			default -> 1.0;
		};
	}

	private double getMasteryAdjustedMultiplier(FormConfig.FormData formData, String statName, double mastery) {
		double baseMult = getBaseFormMultiplier(formData, statName);
		return applyMasteryStatBonus(formData, baseMult, mastery);
	}

	private double applyMasteryStatBonus(FormConfig.FormData formData, double baseMult, double mastery) {
		if (baseMult <= 1.0) return baseMult;
		double maxMastery = formData.getMaxMastery();
		if (maxMastery <= 0.0) return baseMult;
		double ratio = Math.min(1.0, Math.max(0.0, mastery) / maxMastery);
		double factor = 1.0 + ratio * (formData.getMaxStatsMultiplier() - 1.0);
		return baseMult * factor;
	}

	private double getMasteryCostFactor(FormConfig.FormData formData, double mastery) {
		double maxMastery = formData.getMaxMastery();
		if (maxMastery <= 0.0) return 1.0;
		double ratio = Math.min(1.0, Math.max(0.0, mastery) / maxMastery);
		return 1.0 + ratio * (formData.getMaxCostMultiplier() - 1.0);
	}

	private FormConfig.FormData getBestUltimateBaseForm() {
		String raceName = character.getRaceName();
		Map<String, FormConfig> groups = ConfigManager.getAllFormsForRace(raceName);
		if (groups == null || groups.isEmpty()) return null;

		FormConfig.FormData best = null;
		double bestAverage = -1.0;
		for (Map.Entry<String, FormConfig> entry : groups.entrySet()) {
			String groupName = entry.getKey();
			FormConfig group = entry.getValue();
			if (group == null) continue;

			List<FormConfig.FormData> unlocked = TransformationsHelper.getUnlockedForms(this, raceName, groupName);
			for (FormConfig.FormData formData : unlocked) {
				if (formData == null) continue;
				if (formData.isIncompatibleWith(StackForms.GROUP_ULTIMATE, StackForms.ULTIMATE)) continue;

				double average = (formData.getStrMultiplier() + formData.getSkpMultiplier()
						+ formData.getDefMultiplier() + formData.getVitMultiplier()
						+ formData.getPwrMultiplier() + formData.getEneMultiplier()) / 6.0;
				if (average > bestAverage) {
					bestAverage = average;
					best = formData;
				}
			}
		}
		return best;
	}

	private boolean isUltimateStackFormActive() {
		String group = character.getActiveStackFormGroup();
		return group != null && StackForms.GROUP_ULTIMATE.equalsIgnoreCase(group);
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
		if (StackForms.GROUP_ULTIMATE.equalsIgnoreCase(currentFormGroup) && !ConfigManager.getServerConfig().getGameplay().getUltimateFormFixedValue()) {
			FormConfig.FormData bestForm = getBestUltimateBaseForm();
			double bestMult = bestForm != null ? getBaseFormMultiplier(bestForm, statName) : 1.0;
			double ultimateMult = getBaseFormMultiplier(formData, statName);
			return bestMult + ultimateMult - 1.0;
		}

		double mastery = character.getStackFormMasteries().getMastery(currentFormGroup, currentForm);
		return getMasteryAdjustedMultiplier(formData, statName, mastery);
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
		var powerRelease = resources.getPowerRelease() / 100.0;
		if (formData == null && stackFormData == null) return 1.0;

		double adjustedBaseDrain = 0;
		if (character.hasActiveForm() && formData != null) {
			double baseDrain = formData.getStaminaDrainMultiplier();
			if (character.hasActiveStackForm() && stackFormData != null) baseDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();
			double mastery = character.getFormMasteries().getMastery(character.getActiveFormGroup(), character.getActiveForm());
			double costFactor = getMasteryCostFactor(formData, mastery);

			if (baseDrain < 0) adjustedBaseDrain = (baseDrain / costFactor) * powerRelease;
			else adjustedBaseDrain = (baseDrain * costFactor) * powerRelease;
		}

		double adjustedStackDrain = 0;
		if (character.hasActiveStackForm() && stackFormData != null) {
			double stackDrain = stackFormData.getStackDrainMultiplier();
			if (character.hasActiveForm() && formData != null)
				stackDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();
			double mastery = character.getStackFormMasteries().getMastery(character.getActiveStackFormGroup(), character.getActiveStackForm());
			double costFactor = getMasteryCostFactor(stackFormData, mastery);

			if (stackDrain < 0) adjustedStackDrain = (stackDrain / costFactor) * powerRelease;
			else adjustedStackDrain = (stackDrain * costFactor) * powerRelease;
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
			if (character.hasActiveStackForm() && stackFormData != null)
				baseDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();

			double mastery = character.getFormMasteries().getMastery(character.getActiveFormGroup(), character.getActiveForm());
			double costFactor = getMasteryCostFactor(formData, mastery);

			if (baseDrain < 0) adjustedBaseDrain = (baseDrain / costFactor) * powerRelease;
			else adjustedBaseDrain = (baseDrain * costFactor) * powerRelease;
		}

		double adjustedStackDrain = 0;
		if (character.hasActiveStackForm() && stackFormData != null) {
			double stackDrain = stackFormData.getEnergyDrain();
			if (character.hasActiveForm() && formData != null)
				stackDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();

			double stackMastery = isUltimateStackFormActive() ? 0.0
					: character.getStackFormMasteries().getMastery(character.getActiveStackFormGroup(), character.getActiveStackForm());
			double stackCostFactor = getMasteryCostFactor(stackFormData, stackMastery);

			if (stackDrain < 0) adjustedStackDrain = (stackDrain / stackCostFactor) * powerRelease;
			else adjustedStackDrain = (stackDrain * stackCostFactor) * powerRelease;
		}

		double drainAmount = adjustedBaseDrain + adjustedStackDrain;
		if (drainAmount == 0) return 0.0;
		if (drainAmount < 0) return drainAmount;

		return Math.max(1, drainAmount * ConfigManager.getCombatConfig().getBaselineFormDrain());
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
			if (character.hasActiveStackForm() && stackFormData != null)
				baseDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();

			double mastery = character.getFormMasteries().getMastery(character.getActiveFormGroup(), character.getActiveForm());
			double costFactor = getMasteryCostFactor(formData, mastery);

			if (baseDrain < 0) adjustedBaseDrain = (baseDrain / costFactor) * powerRelease;
			else adjustedBaseDrain = (baseDrain * costFactor) * powerRelease;
		}

		double adjustedStackDrain = 0;
		if (character.hasActiveStackForm() && stackFormData != null) {
			double stackDrain = stackFormData.getStaminaDrain();
			if (character.hasActiveForm() && formData != null)
				stackDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();

			double stackMastery = isUltimateStackFormActive() ? 0.0
					: character.getStackFormMasteries().getMastery(character.getActiveStackFormGroup(), character.getActiveStackForm());
			double stackCostFactor = getMasteryCostFactor(stackFormData, stackMastery);

			if (stackDrain < 0) adjustedStackDrain = (stackDrain / stackCostFactor) * powerRelease;
			else adjustedStackDrain = (stackDrain * stackCostFactor) * powerRelease;
		}

		double drainAmount = adjustedBaseDrain + adjustedStackDrain;
		if (drainAmount == 0) return 0.0;
		if (drainAmount < 0) return drainAmount;

		return Math.max(1, drainAmount * ConfigManager.getCombatConfig().getBaselineFormDrain());
	}

	public double getAdjustedHealthDrain() {
		if (!character.hasActiveForm() && !character.hasActiveStackForm()) return 0.0;

		var formData = character.getActiveFormData();
		var stackFormData = character.getActiveStackFormData();
		var powerRelease = resources.getPowerRelease() / 100.0;
		if (formData == null && stackFormData == null) return 0.0;

		double adjustedBaseDrain = 0;
		if (character.hasActiveForm() && formData != null) {
			double baseDrain = formData.getHealthDrain();
			if (character.hasActiveStackForm() && stackFormData != null)
				baseDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();

			double mastery = character.getFormMasteries().getMastery(character.getActiveFormGroup(), character.getActiveForm());
			double costFactor = getMasteryCostFactor(formData, mastery);

			if (baseDrain < 0) adjustedBaseDrain = (baseDrain / costFactor) * powerRelease;
			else adjustedBaseDrain = (baseDrain * costFactor) * powerRelease;
		}

		double adjustedStackDrain = 0;
		if (character.hasActiveStackForm() && stackFormData != null) {
			double stackDrain = stackFormData.getHealthDrain();
			if (character.hasActiveForm() && formData != null)
				stackDrain *= formData.getStackDrainMultiplier() * stackFormData.getStackDrainMultiplier();

			double stackMastery = isUltimateStackFormActive() ? 0.0
					: character.getStackFormMasteries().getMastery(character.getActiveStackFormGroup(), character.getActiveStackForm());
			double stackCostFactor = getMasteryCostFactor(stackFormData, stackMastery);

			if (stackDrain < 0) adjustedStackDrain = (stackDrain / stackCostFactor) * powerRelease;
			else adjustedStackDrain = (stackDrain * stackCostFactor) * powerRelease;
		}

		double drainAmount = adjustedBaseDrain + adjustedStackDrain;
		if (drainAmount == 0) return 0.0;
		if (drainAmount < 0) return drainAmount;

		return Math.max(1, drainAmount * ConfigManager.getCombatConfig().getBaselineFormDrain());
	}

	public void initializeWithRaceAndClass(String raceName, String characterClass, String gender,
										   int hairId, CustomHair customHair,
										   int bodyType, int eyesType, int noseType, int mouthType, int tattooType, float boobScale,
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
		character.setBoobScale(boobScale);
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
		status.validateKiWeaponType();

		RaceCharacterConfig charConfig = ConfigManager.getRaceCharacter(raceName);
		if (charConfig != null) {
			Collection<String> formSkills = charConfig.getFormSkills();
			List<String> androidBlacklistedForms = ConfigManager.getSkillsConfig().getAndroidBlacklistedForms();
			for (String skillName : formSkills) {
				if (status.isAndroidUpgraded() && androidBlacklistedForms.contains(skillName)) continue;
				if (!status.isAndroidUpgraded() && "androidforms".equalsIgnoreCase(skillName)) continue;
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

	public boolean isHumanRacialActive() {
		return ConfigManager.getServerConfig().getRacialSkills().getEnableRacialSkills()
				&& ConfigManager.getServerConfig().getRacialSkills().getHumanRacialSkill()
				&& ConfigManager.getRaceCharacter(character.getRace()).getRacialSkill().equals("human");
	}

	public boolean isAndroidRacialActive() {
		return isHumanRacialActive() && status.isAndroidUpgraded();
	}

	public double getKiAttackCostModifier() {
		if (isAndroidRacialActive()) return 0.50;
		if (isHumanRacialActive())   return 0.75;
		return 1.0;
	}

	public double getKiAttackDamageModifier() {
		if (isAndroidRacialActive()) return 0.85;
		return 1.0;
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
		var gravityConfig = ConfigManager.getServerConfig().getGravity();
		if (!gravityConfig.getTpEnabled()) return 1.0;
		double bonusGravity = GravityLogic.getBonusGravity(player);
		if (bonusGravity <= 0) return 1.0;
		return 1.0 + (bonusGravity * gravityConfig.getTpGravityBonusPerGravity());
	}

	public double getGravityPenalizationGravity() {
		return GravityLogic.getPenalizationGravity(player);
	}

	public double getGravityEnvironmentalMultiplier() {
		return GravityLogic.getGravityMultiplier(player);
	}

	public double getGravityStatMultiplier() {
		var config = ConfigManager.getServerConfig().getGravity();
		if (!config.getStatReductionEnabled()) return 1.0;
		double pGravity = GravityLogic.getPenalizationGravity(player);
		if (pGravity <= 0) return 1.0;
		double reduction = pGravity * config.getStatReductionPerGravity();
		reduction = Math.max(config.getMinStatReduction(), Math.min(config.getMaxStatReduction(), reduction));
		return 1.0 - reduction;
	}

	public double getTpWeightBellMultiplier() {
		var gravityConfig = ConfigManager.getServerConfig().getGravity();
		if (!gravityConfig.getTpEnabled()) return 1.0;
		int totalWeight = GravityLogic.getTotalWeight(player);
		if (totalWeight <= 0) return 1.0;

		double gravityMultiplier = GravityLogic.getGravityMultiplier(player);
		int effectiveWeight = (int) (totalWeight * gravityMultiplier);

		int currentBaseLevel = getLevel();
		int totalBaseStats = stats.getTotalStats();
		int initialStats = totalBaseStats - (currentBaseLevel - 1) * 6;

		double boostedTotal = stats.getStrength() * getTotalMultiplier("STR")
				+ stats.getStrikePower() * getTotalMultiplier("SKP")
				+ stats.getResistance() * getTotalMultiplier("RES")
				+ stats.getVitality() * getTotalMultiplier("VIT")
				+ stats.getKiPower() * getTotalMultiplier("PWR")
				+ stats.getEnergy() * getTotalMultiplier("ENE");

		double relativeLevel = ((boostedTotal - initialStats) / 6.0) + 1.0;
		double peak = gravityConfig.getTpPeakMultiplier();
		double width = gravityConfig.getTpCurveWidth();
		double exponent = -Math.pow((relativeLevel - 2.0 * effectiveWeight), 2) / (2.0 * Math.pow(width, 2));
		return peak * Math.exp(exponent) + 1.0;
	}

	public int getGravityTotalWeight() {
		return GravityLogic.getTotalWeight(player);
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
		var dynamicGrowthConfig = ConfigManager.getServerConfig().getDynamicGrowth();
		if (!dynamicGrowthConfig.isManualTpPurchasesEnabled()) return Integer.MAX_VALUE;

		double globalMult = ConfigManager.getServerConfig().getGameplay().getGlobalTpCostMultiplier();
		double raceMult = getRaceTpCostMultiplier();
		double totalMult = globalMult * raceMult;

		int minCost = ConfigManager.getServerConfig().getGameplay().getMinTPCost();
		int discountThreshold = ConfigManager.getServerConfig().getGameplay().getMaxTPDiscount();

		double baseCost = minCost + (simulatedTotalStats * 1.25);

		int earlyGameDiscount = 0;
		if (simulatedTotalStats < discountThreshold) earlyGameDiscount = discountThreshold - simulatedTotalStats;

		int finalCost = (int) (baseCost * totalMult) - earlyGameDiscount;
		finalCost = Math.max(minCost, finalCost);

		double tpCostMultiplier = dynamicGrowthConfig.getAttributeTpCostMultiplier();
		if (tpCostMultiplier > 1.0) {
			double scaled = Math.ceil(finalCost * tpCostMultiplier);
			finalCost = scaled >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) scaled;
		}
		return finalCost;
	}

	public int calculateRecursiveCost(int statsToAdd, int maxStats) {
		if (!ConfigManager.getServerConfig().getDynamicGrowth().isManualTpPurchasesEnabled())
			return statsToAdd <= 0 ? 0 : Integer.MAX_VALUE;
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
		if (!ConfigManager.getServerConfig().getDynamicGrowth().isManualTpPurchasesEnabled()) return 0;
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

	public void resetPlayerProgress(ServerPlayer player, Integer keepPercentage, boolean keepSkills, boolean forceSaiyanTail) {
		var currentStats = getStats();
		if (keepPercentage != null) {
			int newStr = (currentStats.getStrength() * keepPercentage) / 100;
			int newSkp = (currentStats.getStrikePower() * keepPercentage) / 100;
			int newRes = (currentStats.getResistance() * keepPercentage) / 100;
			int newVit = (currentStats.getVitality() * keepPercentage) / 100;
			int newPwr = (currentStats.getKiPower() * keepPercentage) / 100;
			int newEne = (currentStats.getEnergy() * keepPercentage) / 100;
			float currentTPs = getResources().getTrainingPoints();
			float newTPs = (currentTPs * keepPercentage) / 100;

			currentStats.setStrength(Math.max(5, newStr));
			currentStats.setStrikePower(Math.max(5, newSkp));
			currentStats.setResistance(Math.max(5, newRes));
			currentStats.setVitality(Math.max(5, newVit));
			currentStats.setKiPower(Math.max(5, newPwr));
			currentStats.setEnergy(Math.max(5, newEne));

			getResources().setTrainingPoints(newTPs);
		} else {
			currentStats.setStrength(5);
			currentStats.setStrikePower(5);
			currentStats.setResistance(5);
			currentStats.setVitality(5);
			currentStats.setKiPower(5);
			currentStats.setEnergy(5);
			getResources().setTrainingPoints(0);
		}

		if (getStatus().isFused()) FusionLogic.endFusion(player, this, false);
		getCharacter().clearActiveForm(player);
		getCharacter().clearActiveStackForm(player);

		getStatus().reset();
		getResources().reset();
		getResources().setPowerRelease(0);
		getPlayerQuestData().resetAll();
		getCharacter().clearInteractedMasters();
		getDynamicGrowth().clear();

		if (!keepSkills) {
			getSkills().removeAllSkills();
			getEffects().removeAllEffects();
			getSecondaryStatEffects().clear();
			getTechniques().clearAllTechniques();
		}

		getCooldowns().clearCooldowns();
		getBonusStats().clearAllStats();
		if (forceSaiyanTail) getCharacter().setHasSaiyanTail(true);

		player.refreshDimensions();
		player.setHealth(20.0F);
		var attribute = player.getAttribute(Attributes.MAX_HEALTH);
		if (attribute != null) attribute.removePermanentModifier(StatsEvents.DMZ_HEALTH_MODIFIER_UUID);
		player.setHealth(20.0F);
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
		nbt.put("SecondaryStatEffects", secondaryStatEffects.save());
		nbt.put("PlayerQuestData", playerQuestData.serializeNBT());
		nbt.put("BonusStats", bonusStats.save());
		nbt.put("Techniques",  techniques.save());
		nbt.put("DynamicGrowth", dynamicGrowth.save());
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
		if (nbt.contains("SecondaryStatEffects")) secondaryStatEffects.load(nbt.getCompound("SecondaryStatEffects"));
		else secondaryStatEffects.clear();
		if (nbt.contains("PlayerQuestData")) playerQuestData.deserializeNBT(nbt.getCompound("PlayerQuestData"));
		else throw new ClassNotFoundException("PlayerQuestData not found in NBT. This is required for quest progression to work correctly. " +
				"Please update the mod or re-generate your config files.");
		if (nbt.contains("BonusStats")) bonusStats.load(nbt.getCompound("BonusStats"));
		if (nbt.contains("Techniques")) techniques.load(nbt.getCompound("Techniques"));
		if (nbt.contains("DynamicGrowth")) dynamicGrowth.load(nbt.getCompound("DynamicGrowth"));
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
		this.secondaryStatEffects.copyFrom(other.secondaryStatEffects);
		this.playerQuestData.deserializeNBT(other.playerQuestData.serializeNBT());
		this.bonusStats.copyFrom(other.bonusStats);
		this.techniques.copyFrom(other.techniques);
		this.dynamicGrowth.copyFrom(other.dynamicGrowth);
		this.hasInitializedHealth = other.hasInitializedHealth;
		if (character.getRaceName() != null && !character.getRaceName().isEmpty())
			updateTransformationSkillLimits(character.getRaceName());
		this.isDataLoaded = true;
	}
}