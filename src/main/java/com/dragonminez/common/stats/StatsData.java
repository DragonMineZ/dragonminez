package com.dragonminez.common.stats;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.quest.QuestData;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.List;

public class StatsData {
    private final Player player;
    private final Stats stats;
    private final Status status;
    private final Cooldowns cooldowns;
    private final Character character;
    private final Resources resources;
    private final Skills skills;
    private final Effects effects;
    private final QuestData questData;
    private final BonusStats bonusStats;
    private final Training training;

    private boolean hasInitializedHealth = false;

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
        this.questData = new QuestData();
        this.bonusStats = new BonusStats();
        this.training = new Training();
    }

    public Stats getStats() { return stats; }
    public Status getStatus() { return status; }
    public Cooldowns getCooldowns() { return cooldowns; }
    public Character getCharacter() { return character; }
    public Resources getResources() { return resources; }
    public Skills getSkills() { return skills; }
    public Effects getEffects() { return effects; }
    public QuestData getQuestData() { return questData; }
    public BonusStats getBonusStats() { return bonusStats; }
    public Training getTraining() { return training; }
    public Player getPlayer() { return player; }

    public boolean hasInitializedHealth() { return hasInitializedHealth; }
    public void setInitializedHealth(boolean initialized) { this.hasInitializedHealth = initialized; }

    public int getLevel() {
        int totalStats = stats.getTotalStats();

        String raceName = character.getRaceName();
        String characterClass = character.getCharacterClass();

        RaceStatsConfig raceConfig = ConfigManager.getRaceStats(raceName);
        RaceStatsConfig.ClassStats classStats = getClassStats(raceConfig, characterClass);
        RaceStatsConfig.BaseStats baseStats = classStats.getBaseStats();

        if (baseStats == null) baseStats = new RaceStatsConfig().getClassStats(characterClass).getBaseStats();

        int initialStats = baseStats.getStrength() + baseStats.getStrikePower() +
                baseStats.getResistance() + baseStats.getVitality() +
                baseStats.getKiPower() + baseStats.getEnergy();

        return ((totalStats - initialStats) / 6) + 1;
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

    public float getMaxHealth() {
        double vitScaling = getStatScaling("VIT");
        double vitMult = getFormMultiplier("VIT");
        if (!ConfigManager.getServerConfig().getGameplay().isMultiplicationInsteadOfAdditionForMultipliers()) {
            vitMult += 1.0;
        }
        double bonusVit = bonusStats.calculateBonus("VIT", 0);
        return (float) ((stats.getVitality() * vitScaling * vitMult) + (bonusVit * vitScaling));
    }

    public int getMaxEnergy() {
        double eneScaling = getStatScaling("ENE");
        double eneMult = getFormMultiplier("ENE");
        if (!ConfigManager.getServerConfig().getGameplay().isMultiplicationInsteadOfAdditionForMultipliers()) {
            eneMult += 1.0;
        }
        double bonusEne = bonusStats.calculateBonus("ENE", 0);
        return (int) (20 + (stats.getEnergy() * eneScaling * eneMult) + (bonusEne * eneScaling));
    }

    public int getMaxStamina() {
        double stmScaling = getStatScaling("STM");
        double resMult = getTotalMultiplier("RES");
        if (!ConfigManager.getServerConfig().getGameplay().isMultiplicationInsteadOfAdditionForMultipliers()) {
            resMult += 1.0;
        }
        double bonusRes = bonusStats.calculateBonus("RES", 0);
        return (int) (20 + (stats.getResistance() * stmScaling * resMult) + (bonusRes * stmScaling));
    }

    public int getMaxPoise() {
        return (int) (25 + getDefense() * 0.1);
    }

    public double getMaxMeleeDamage() {
        double strScaling = getStatScaling("STR");
        double strMult = getTotalMultiplier("STR");
        if (!ConfigManager.getServerConfig().getGameplay().isMultiplicationInsteadOfAdditionForMultipliers()) {
            strMult += 1.0;
        }
        double bonusStr = bonusStats.calculateBonus("STR", 0);
        return (1 + (stats.getStrength() * strScaling * strMult) + (bonusStr * strScaling));
    }

    public double getMeleeDamageWithoutMults() {
        double strScaling = getStatScaling("STR");
        double bonusStr = bonusStats.calculateBonus("STR", 0);
        double releaseMultiplier = resources.getPowerRelease() / 100.0;
        return (1 + (stats.getStrength() * strScaling) + (bonusStr * strScaling) * releaseMultiplier);
    }

    public double getMeleeDamage() {
        double strScaling = getStatScaling("STR");
        double strMult = getTotalMultiplier("STR");
        if (!ConfigManager.getServerConfig().getGameplay().isMultiplicationInsteadOfAdditionForMultipliers()) {
            strMult += 1.0;
        }
        double releaseMultiplier = resources.getPowerRelease() / 100.0;
        double bonusStr = bonusStats.calculateBonus("STR", 0);
        return (1 + ((stats.getStrength() * strScaling * strMult) + (bonusStr * strScaling)) * releaseMultiplier);
    }

    public double getMaxStrikeDamage() {
        double skpScaling = getStatScaling("SKP");
        double strScaling = getStatScaling("STR");
        double skpMult = getTotalMultiplier("SKP");
        double strMult = getTotalMultiplier("STR");
        if (!ConfigManager.getServerConfig().getGameplay().isMultiplicationInsteadOfAdditionForMultipliers()) {
            skpMult += 1.0;
            strMult += 1.0;
        }
        double bonusSkp = bonusStats.calculateBonus("SKP", 0);
        double bonusStr = bonusStats.calculateBonus("STR", 0);
        return (1 + (stats.getStrikePower() * skpScaling * skpMult) + (bonusSkp * skpScaling) + ((stats.getStrength() * strScaling * strMult) + (bonusStr * strScaling)) * 0.25);
    }

    public double getStrikeDamage() {
        double skpScaling = getStatScaling("SKP");
        double strScaling = getStatScaling("STR");
        double skpMult = getTotalMultiplier("SKP");
        double strMult = getTotalMultiplier("STR");
        if (!ConfigManager.getServerConfig().getGameplay().isMultiplicationInsteadOfAdditionForMultipliers()) {
            skpMult += 1.0;
            strMult += 1.0;
        }
        double releaseMultiplier = resources.getPowerRelease() / 100.0;
        double bonusSkp = bonusStats.calculateBonus("SKP", 0);
        double bonusStr = bonusStats.calculateBonus("STR", 0);
        double baseDamage = (stats.getStrikePower() * skpScaling * skpMult) + (bonusSkp * skpScaling) + ((stats.getStrength() * strScaling * strMult) + (bonusStr * strScaling)) * 0.25;
        return 1 + baseDamage * releaseMultiplier;
    }

    public double getMaxKiDamage() {
        double pwrScaling = getStatScaling("PWR");
        double pwrMult = getTotalMultiplier("PWR");
        if (!ConfigManager.getServerConfig().getGameplay().isMultiplicationInsteadOfAdditionForMultipliers()) {
            pwrMult += 1.0;
        }
        double bonusPwr = bonusStats.calculateBonus("PWR", 0);
        return (stats.getKiPower() * pwrScaling * pwrMult) + (bonusPwr * pwrScaling);
    }

    public double getKiDamage() {
        double pwrScaling = getStatScaling("PWR");
        double pwrMult = getTotalMultiplier("PWR");
        if (!ConfigManager.getServerConfig().getGameplay().isMultiplicationInsteadOfAdditionForMultipliers()) {
            pwrMult += 1.0;
        }
        double releaseMultiplier = resources.getPowerRelease() / 100.0;
        double bonusPwr = bonusStats.calculateBonus("PWR", 0);
        return ((stats.getKiPower() * pwrScaling * pwrMult) + (bonusPwr * pwrScaling)) * releaseMultiplier;
    }

    public double getMaxDefense() {
        double defScaling = getStatScaling("DEF");
        double resMult = getTotalMultiplier("RES");
        if (!ConfigManager.getServerConfig().getGameplay().isMultiplicationInsteadOfAdditionForMultipliers()) {
            resMult += 1.0;
        }
        double bonusRes = bonusStats.calculateBonus("RES", 0);
        double armor = player.getArmorValue();
        double toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS).getValue();
        return (stats.getResistance() * defScaling * resMult) + (bonusRes * defScaling) + armor * 0.75 + toughness;
    }

    public double getDefense() {
        double defScaling = getStatScaling("DEF");
        double resMult = getTotalMultiplier("RES");
        if (!ConfigManager.getServerConfig().getGameplay().isMultiplicationInsteadOfAdditionForMultipliers()) {
            resMult += 1.0;
        }
        double releaseMultiplier = resources.getPowerRelease() / 100.0;
        double bonusRes = bonusStats.calculateBonus("RES", 0);
        double armor = player.getArmorValue();
        double toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS).getValue();
        return ((stats.getResistance() * defScaling * resMult) + (bonusRes * defScaling) + (armor * 0.75) + toughness) * releaseMultiplier;
    }

    public double getTotalMultiplier(String statName) {
        double form = getFormMultiplier(statName);
        double stack = getStackFormMultiplier(statName);
        double effect = getEffectsMultiplier(statName);

        if (ConfigManager.getServerConfig().getGameplay().isMultiplicationInsteadOfAdditionForMultipliers()) return form * stack * effect;
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
        var powerRelease = (double) (resources.getPowerRelease() / calculateMaxPowerRelease());
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
        return drainAmount != 0 ? Math.max(1, drainAmount * ConfigManager.getServerConfig().getCombat().getBaselineFormDrain()) : 0;
    }

    public double getAdjustedStaminaDrain() {
        if (!character.hasActiveForm() && !character.hasActiveStackForm()) return 0.0;

        var formData = character.getActiveFormData();
        var stackFormData = character.getActiveStackFormData();
        var powerRelease = (double) (resources.getPowerRelease() / calculateMaxPowerRelease());
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
        return drainAmount != 0 ? Math.max(1, drainAmount * ConfigManager.getServerConfig().getCombat().getBaselineFormDrain()) : 0;
    }

    public double getAdjustedHealthDrain() {
        if (!character.hasActiveForm() && !character.hasActiveStackForm()) {
            return 0.0;
        }

        var formData = character.getActiveFormData();
        var stackFormData = character.getActiveStackFormData();
        var powerRelease = (double) (resources.getPowerRelease() / calculateMaxPowerRelease());
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
        return drainAmount != 0 ? Math.max(1, drainAmount * ConfigManager.getServerConfig().getCombat().getBaselineFormDrain()) : 0;
    }



    public void initializeWithRaceAndClass(String raceName, String characterClass, String gender,
                                           int hairId, com.dragonminez.common.hair.CustomHair customHair,
                                           int bodyType, int eyesType, int noseType, int mouthType, int tattooType,
                                           String hairColor, String bodyColor, String bodyColor2, String bodyColor3,
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
        character.setHairColor(hairColor);
        character.setBodyColor(bodyColor);
        character.setBodyColor2(bodyColor2);
        character.setBodyColor3(bodyColor3);
        character.setEye1Color(eye1Color);
        character.setEye2Color(eye2Color);
        character.setAuraColor(auraColor);
        status.setCreatedCharacter(true);

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
        if (config == null) {
            return new RaceStatsConfig().getClassStats(characterClass);
        }
        return config.getClassStats(characterClass);
    }

    public int calculateRecursiveCost(int statsToAdd, int baseMultiplier, int maxStats, double multiplier) {
        int totalCost = 0;
        int currentTotalStats = stats.getTotalStats();

        for (int i = 0; i < statsToAdd; i++) {
            if (currentTotalStats + i >= maxStats * 6) break;
            int statLevel = (currentTotalStats + i) / 6;
            totalCost += (int) Math.round(baseMultiplier + (multiplier * statLevel));
        }
        return (int) Math.round(totalCost * 1.25);
    }

    public int calculateStatIncrease(int baseMultiplier, int statsToAdd, int availableTPs, int maxStats, double multiplier) {
        int statsIncreased = 0;
        int costAccumulated = 0;
        int currentTotalStats = stats.getTotalStats();

        while (statsIncreased < statsToAdd) {
            if (currentTotalStats + statsIncreased >= maxStats * 6) break;

            int statLevel = (currentTotalStats + statsIncreased) / 6;
            int costForStat = (int) Math.round(baseMultiplier + (multiplier * statLevel));

            if (costAccumulated + costForStat > availableTPs) break;

            costAccumulated += costForStat;
            statsIncreased++;
        }

        return statsIncreased;
    }

    public int calculateMaxPowerRelease() {
        return 50 + (skills.getSkillLevel("potentialunlock") * 5);
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
        nbt.put("QuestData", questData.serializeNBT());
        nbt.put("BonusStats", bonusStats.save());
        nbt.put("Training", training.save());
        nbt.putBoolean("HasInitializedHealth", hasInitializedHealth);
        return nbt;
    }

    public void load(CompoundTag nbt) {
        if (nbt.contains("Stats")) {
            stats.load(nbt.getCompound("Stats"));
        }
        if (nbt.contains("Status")) {
            status.load(nbt.getCompound("Status"));
        }
        if (nbt.contains("Cooldowns")) {
            cooldowns.load(nbt.getCompound("Cooldowns"));
        }
        if (nbt.contains("Character")) {
            character.load(nbt.getCompound("Character"));
        }
        if (nbt.contains("Resources")) {
            resources.load(nbt.getCompound("Resources"));
        }
        if (nbt.contains("Skills")) {
            skills.load(nbt.getCompound("Skills"));
        }
        if (nbt.contains("Effects")) {
            effects.load(nbt.getCompound("Effects"));
        }
        if (nbt.contains("QuestData")) {
            questData.deserializeNBT(nbt.getCompound("QuestData"));
        }
        if (nbt.contains("BonusStats")) {
            bonusStats.load(nbt.getCompound("BonusStats"));
        }
        if (nbt.contains("Training")) {
            training.load(nbt.getCompound("Training"));
        }
        if (nbt.contains("HasInitializedHealth")) {
            hasInitializedHealth = nbt.getBoolean("HasInitializedHealth");
        }

        if (character.getRaceName() != null && !character.getRaceName().isEmpty()) {
            updateTransformationSkillLimits(character.getRaceName());
        }
    }

    public void copyFrom(StatsData other) {
        this.stats.copyFrom(other.stats);
        this.status.copyFrom(other.status);
        this.cooldowns.copyFrom(other.cooldowns);
        this.character.copyFrom(other.character);
        this.resources.copyFrom(other.resources);
        this.skills.copyFrom(other.skills);
        this.effects.copyFrom(other.effects);
        this.questData.deserializeNBT(other.questData.serializeNBT());
        this.bonusStats.copyFrom(other.bonusStats);
        this.training.copyFrom(other.training);
        this.hasInitializedHealth = other.hasInitializedHealth;

        if (character.getRaceName() != null && !character.getRaceName().isEmpty()) {
            updateTransformationSkillLimits(character.getRaceName());
        }
    }
}