package com.dragonminez.common.stats;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class StatsData {
    private final Player player;
    private final Stats stats;
    private final Status status;
    private final Cooldowns cooldowns;
    private final Character character;
    private final Resources resources;

    private boolean hasInitializedHealth = false;

    public StatsData(Player player) {
        this.player = player;
        this.stats = new Stats();
        this.stats.setPlayer(player);
        this.status = new Status();
        this.cooldowns = new Cooldowns();
        this.character = new Character();
        this.resources = new Resources();
    }

    public Stats getStats() { return stats; }
    public Status getStatus() { return status; }
    public Cooldowns getCooldowns() { return cooldowns; }
    public Character getCharacter() { return character; }
    public Resources getResources() { return resources; }
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
        return 20 + (int) (stats.getVitality() * vitScaling);
    }

    public int getMaxEnergy() {
        double eneScaling = getStatScaling("ENE");
        return 100 + (int) (stats.getEnergy() * eneScaling);
    }

    public int getMaxStamina() {
        double stmScaling = getStatScaling("STM");
        return 100 + (int) (stats.getResistance() * stmScaling);
    }

	public double getMaxMeleeDamage() {
		double strScaling = getStatScaling("STR");
		return 1 + stats.getStrength() * strScaling;
	}

    public double getMeleeDamage() {
        double strScaling = getStatScaling("STR");
        double releaseMultiplier = resources.getPowerRelease() / 100.0;
        return 1 + ((stats.getStrength() * strScaling) * releaseMultiplier);
    }

	public double getMaxStrikeDamage() {
		double skpScaling = getStatScaling("SKP");
		double strScaling = getStatScaling("STR");
		return 1 + (stats.getStrikePower() * skpScaling + (stats.getStrength() * strScaling) * 0.25);
	}

	public double getStrikeDamage() {
		double skpScaling = getStatScaling("SKP");
		double strScaling = getStatScaling("STR");
        double releaseMultiplier = resources.getPowerRelease() / 100.0;
		double baseDamage = stats.getStrikePower() * skpScaling + (stats.getStrength() * strScaling) * 0.25;
        return 1 + baseDamage * releaseMultiplier;
	}

	public double getMaxKiDamage() {
		double pwrScaling = getStatScaling("PWR");
		return stats.getKiPower() * pwrScaling;
	}

    public double getKiDamage() {
        double pwrScaling = getStatScaling("PWR");
        double releaseMultiplier = resources.getPowerRelease() / 100.0;
        return (stats.getKiPower() * pwrScaling) * releaseMultiplier;
    }

	public double getMaxDefense() {
		double defScaling = getStatScaling("DEF");
		return stats.getResistance() * defScaling;
	}

    public double getDefense() {
        double defScaling = getStatScaling("DEF");
        double releaseMultiplier = resources.getPowerRelease() / 100.0;
        return (stats.getResistance() * defScaling) * releaseMultiplier;
    }

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("Stats", stats.save());
        nbt.put("Status", status.save());
        nbt.put("Cooldowns", cooldowns.save());
        nbt.put("Character", character.save());
        nbt.put("Resources", resources.save());
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
        if (nbt.contains("HasInitializedHealth")) {
            hasInitializedHealth = nbt.getBoolean("HasInitializedHealth");
        }
    }

    public void copyFrom(StatsData other) {
        this.stats.copyFrom(other.stats);
        this.status.copyFrom(other.status);
        this.cooldowns.copyFrom(other.cooldowns);
        this.character.copyFrom(other.character);
        this.resources.copyFrom(other.resources);
        this.hasInitializedHealth = other.hasInitializedHealth;
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
}
