package com.dragonminez.common.stats;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.quest.QuestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

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

    private boolean hasInitializedHealth = false;

    public StatsData(Player player) {
        this.player = player;
        this.stats = new Stats();
        this.stats.setPlayer(player);
        this.status = new Status();
        this.cooldowns = new Cooldowns();
        this.character = new Character();
        this.resources = new Resources();
        this.skills = new Skills();
        this.effects = new Effects();
        this.questData = new QuestData();
    }

    public Stats getStats() { return stats; }
    public Status getStatus() { return status; }
    public Cooldowns getCooldowns() { return cooldowns; }
    public Character getCharacter() { return character; }
    public Resources getResources() { return resources; }
    public Skills getSkills() { return skills; }
    public Effects getEffects() { return effects; }
    public QuestData getQuestData() { return questData; }
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

        int initialStats = baseStats.getStrength() + baseStats.getStrikePower() +
                          baseStats.getResistance() + baseStats.getVitality() +
                          baseStats.getKiPower() + baseStats.getEnergy();

        return ((totalStats - initialStats) / 6) + 1;
    }

    public int getBattlePower() {
        int str = stats.getStrength();
        int skp = stats.getStrikePower();
        int res = stats.getResistance();
        int vit = stats.getVitality();
        int pwr = stats.getKiPower();

        double releaseMultiplier = (double) resources.getPowerRelease() / 100.0;

        return (int) ((str + skp + res + vit + pwr) * releaseMultiplier);
    }

    public int getMaxHealth() {
        double vitScaling = getStatScaling("VIT");
        double vitMult = 1.0 + getTotalMultiplier("VIT");
        return (int) (20 + (stats.getVitality() * vitScaling * vitMult));
    }

    public int getMaxEnergy() {
        double eneScaling = getStatScaling("ENE");
        double eneMult = 1.0 + getTotalMultiplier("ENE");
        return (int) (100 + (stats.getEnergy() * eneScaling * eneMult));
    }

    public int getMaxStamina() {
        double stmScaling = getStatScaling("STM");
        double resMult = 1.0 + getTotalMultiplier("RES");
        return (int) (100 + (stats.getResistance() * stmScaling * resMult));
    }

	public double getMaxMeleeDamage() {
		double strScaling = getStatScaling("STR");
		double strMult = 1.0 + getTotalMultiplier("STR");
		return (1 + stats.getStrength() * strScaling) * strMult;
	}

    public double getMeleeDamage() {
        double strScaling = getStatScaling("STR");
        double strMult = 1.0 + getTotalMultiplier("STR");
        double releaseMultiplier = resources.getPowerRelease() / 100.0;
        return (1 + ((stats.getStrength() * strScaling) * releaseMultiplier)) * strMult;
    }

	public double getMaxStrikeDamage() {
		double skpScaling = getStatScaling("SKP");
		double strScaling = getStatScaling("STR");
		double skpMult = 1.0 + getTotalMultiplier("SKP");
		double strMult = 1.0 + getTotalMultiplier("STR");
		return (1 + (stats.getStrikePower() * skpScaling * skpMult + (stats.getStrength() * strScaling * strMult) * 0.25));
	}

	public double getStrikeDamage() {
		double skpScaling = getStatScaling("SKP");
		double strScaling = getStatScaling("STR");
		double skpMult = 1.0 + getTotalMultiplier("SKP");
		double strMult = 1.0 + getTotalMultiplier("STR");
        double releaseMultiplier = resources.getPowerRelease() / 100.0;
		double baseDamage = stats.getStrikePower() * skpScaling * skpMult + (stats.getStrength() * strScaling * strMult) * 0.25;
        return 1 + baseDamage * releaseMultiplier;
	}

	public double getMaxKiDamage() {
		double pwrScaling = getStatScaling("PWR");
		double pwrMult = 1.0 + getTotalMultiplier("PWR");
		return stats.getKiPower() * pwrScaling * pwrMult;
	}

    public double getKiDamage() {
        double pwrScaling = getStatScaling("PWR");
        double pwrMult = 1.0 + getTotalMultiplier("PWR");
        double releaseMultiplier = resources.getPowerRelease() / 100.0;
        return (stats.getKiPower() * pwrScaling * pwrMult) * releaseMultiplier;
    }

	public double getMaxDefense() {
		double defScaling = getStatScaling("DEF");
		double resMult = 1.0 + getTotalMultiplier("RES");
		return stats.getResistance() * defScaling * resMult;
	}

    public double getDefense() {
        double defScaling = getStatScaling("DEF");
        double resMult = 1.0 + getTotalMultiplier("RES");
        double releaseMultiplier = resources.getPowerRelease() / 100.0;
        return (stats.getResistance() * defScaling * resMult) * releaseMultiplier;
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
        this.hasInitializedHealth = other.hasInitializedHealth;

        if (character.getRaceName() != null && !character.getRaceName().isEmpty()) {
            updateTransformationSkillLimits(character.getRaceName());
        }
    }

    public void initializeWithRaceAndClass(String raceName, String characterClass, String gender,
                                           int hairId, int bodyType, int eyesType, int noseType, int mouthType, int tattooType,
                                           String hairColor, String bodyColor, String bodyColor2, String bodyColor3,
                                           String eye1Color, String eye2Color, String auraColor) {
        character.setRace(raceName);
        character.setGender(gender);
        character.setCharacterClass(characterClass);
        character.setHairId(hairId);
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

        stats.setStrength(baseStats.getStrength());
        stats.setStrikePower(baseStats.getStrikePower());
        stats.setResistance(baseStats.getResistance());
        stats.setVitality(baseStats.getVitality());
        stats.setKiPower(baseStats.getKiPower());
        stats.setEnergy(baseStats.getEnergy());

        resources.setCurrentEnergy(getMaxEnergy());
        resources.setCurrentStamina(getMaxStamina());
        resources.setPowerRelease(5);
        resources.setAlignment(100);

        updateTransformationSkillLimits(raceName);
    }

    public void updateTransformationSkillLimits(String raceName) {
        RaceCharacterConfig charConfig = ConfigManager.getRaceCharacter(raceName);
        if (charConfig != null) {
            int superformMax = charConfig.getSuperformTpCost() != null ? charConfig.getSuperformTpCost().length : 0;
            int godformMax = charConfig.getGodformTpCost() != null ? charConfig.getGodformTpCost().length : 0;
            int legendaryMax = charConfig.getLegendaryformsTpCost() != null ? charConfig.getLegendaryformsTpCost().length : 0;

            skills.updateTransformationMaxLevels(superformMax, godformMax, legendaryMax);
        }
    }

    public double getStatScaling(String statName) {
        String raceName = character.getRaceName();
        String characterClass = character.getCharacterClass();

        RaceStatsConfig raceConfig = ConfigManager.getRaceStats(raceName);
        RaceStatsConfig.ClassStats classStats = getClassStats(raceConfig, characterClass);
        RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();

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
        return switch (characterClass.toLowerCase()) {
            case "warrior" -> config.getWarrior();
            case "spiritualist" -> config.getSpiritualist();
            case "martialartist" -> config.getMartialArtist();
            default -> config.getWarrior();
        };
    }

    public int calculateRecursiveCost(int statsToAdd, int baseMultiplier, int maxStats, double multiplier) {
        int totalCost = 0;
        int currentTotalStats = stats.getTotalStats();

        for (int i = 0; i < statsToAdd; i++) {
            if (currentTotalStats + i >= maxStats * 6) break;
            int statLevel = (currentTotalStats + i) / 6;
            totalCost += (int) Math.round(baseMultiplier + (multiplier * statLevel));
        }
        return totalCost;
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

    public void tick() {
        cooldowns.tick();
    }

    public double getTotalMultiplier(String statName) {
        return getFormMultiplier(statName) + getKaiokenMultiplier(statName) + getEffectsMultiplier();
    }

    public double getFormMultiplier(String statName) {
        String currentForm = character.getCurrentForm();
        String currentFormGroup = character.getCurrentFormGroup();

        if (currentForm == null || currentForm.isEmpty() || currentForm.equals("base")) {
            return 0.0;
        }

        if (currentFormGroup == null || currentFormGroup.isEmpty()) {
            return 0.0;
        }

        var formConfig = ConfigManager.getFormGroup(
            character.getRaceName(), currentFormGroup);

        if (formConfig == null) {
            return 0.0;
        }

        var formData = formConfig.getForm(currentForm);
        if (formData == null) {
            return 0.0;
        }

        double baseMult = switch (statName.toUpperCase()) {
            case "STR" -> formData.getStrMultiplier() - 1.0;
            case "SKP" -> formData.getSkpMultiplier() - 1.0;
			case "STM" -> formData.getStmMultiplier() - 1.0;
			case "RES" -> (formData.getDefMultiplier() - 1.0 + formData.getStmMultiplier() - 1.0) / 2.0;
            case "VIT" -> formData.getVitMultiplier() - 1.0;
            case "PWR" -> formData.getPwrMultiplier() - 1.0;
            case "ENE" -> formData.getEneMultiplier() - 1.0;
            default -> 0.0;
        };

        double mastery = character.getFormMasteries().getMastery(currentFormGroup, currentForm);
        double masteryBonus = mastery * formData.getStatMultPerMasteryPoint();

        return baseMult + masteryBonus;
    }

    public double getKaiokenMultiplier(String statName) {
        var skill = skills.getSkill("kaioken");
        if (skill == null || !skill.isActive()) {
            return 0.0;
        }

        int kaiokenLevel = skill.getLevel();
        if (kaiokenLevel <= 0) {
            return 0.0;
        }

        var skillsConfig = ConfigManager.getSkillsConfig();
        if (skillsConfig == null) {
            return 0.0;
        }

        double baseMultiplier = skillsConfig.getMultiplierForLevel("kaioken", kaiokenLevel) - 1.0;

        return switch (statName.toUpperCase()) {
            case "STR", "SKP", "PWR" -> baseMultiplier;
            case "DEF" -> baseMultiplier * 0.5;
            default -> 0.0;
        };
    }

    public double getEffectsMultiplier() {
        return effects.getTotalEffectMultiplier();
    }

    public double getAdjustedEnergyDrain() {
        if (!character.hasActiveForm()) {
            return 0.0;
        }

        var formData = character.getActiveFormData();
        if (formData == null) {
            return 0.0;
        }

        double baseDrain = formData.getEnergyDrain();
        double mastery = character.getFormMasteries().getMastery(
            character.getCurrentFormGroup(),
            character.getCurrentForm()
        );
        double reduction = mastery * formData.getCostDecreasePerMasteryPoint();

        return Math.max(0.0, baseDrain - reduction);
    }

    public double getAdjustedStaminaDrain() {
        if (!character.hasActiveForm()) {
            return 1.0;
        }

        var formData = character.getActiveFormData();
        if (formData == null) {
            return 1.0;
        }

        double baseDrain = formData.getStaminaDrain();
        double mastery = character.getFormMasteries().getMastery(
            character.getCurrentFormGroup(),
            character.getCurrentForm()
        );
        double reduction = mastery * formData.getCostDecreasePerMasteryPoint();

        return Math.max(1.0, baseDrain - reduction);
    }
}
