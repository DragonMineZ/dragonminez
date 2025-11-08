package com.dragonminez.common.stats;

import com.dragonminez.common.config.ClassStatsConfig;
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

        int raceId = character.getRace();
        String className = character.getCharacterClass();

        RaceStatsConfig raceConfig = ConfigManager.getRaceConfig(raceId);
        ClassStatsConfig classConfig = raceConfig.getClassConfig(className);
        ClassStatsConfig.BaseStats baseStats = classConfig.getBaseStats();

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

        double releaseMultiplier = (double) resources.getRelease() / 100.0;

        return (int) ((str + skp + res + vit + pwr) * releaseMultiplier);
    }

    public int getMaxHealth() {
        double vitScaling = getStatScaling("VIT");
        return 20 + (int)(stats.getVitality() * vitScaling);
    }

    public int getMaxEnergy() {
        double eneScaling = getStatScaling("ENE");
        return 100 + (int)(stats.getEnergy() * eneScaling);
    }

    public int getMaxStamina() {
        double stmScaling = getStatScaling("STM");
        return 100 + (int)(stats.getResistance() * stmScaling);
    }

    public double getAttackDamage() {
        double strScaling = getStatScaling("STR");
        double skpScaling = getStatScaling("SKP");
        return (stats.getStrength() * strScaling) + (stats.getStrikePower() * skpScaling);
    }

    public double getKiDamage() {
        double pwrScaling = getStatScaling("PWR");
        return stats.getKiPower() * pwrScaling;
    }

    public double getDefense() {
        double defScaling = getStatScaling("DEF");
        return stats.getResistance() * defScaling;
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

    public void initializeWithRaceAndClass(int raceId, String characterClass, String gender) {
        character.setRace(raceId);
        character.setGender(gender);
        character.setCharacterClass(characterClass);
        status.setCreatedCharacter(true);

        RaceStatsConfig raceConfig = ConfigManager.getRaceConfig(raceId);
        ClassStatsConfig classConfig = raceConfig.getClassConfig(characterClass);

        ClassStatsConfig.BaseStats baseStats = classConfig.getBaseStats();
        stats.setStrength(baseStats.getStrength());
        stats.setStrikePower(baseStats.getStrikePower());
        stats.setResistance(baseStats.getResistance());
        stats.setVitality(baseStats.getVitality());
        stats.setKiPower(baseStats.getKiPower());
        stats.setEnergy(baseStats.getEnergy());

        resources.setCurrentEnergy(getMaxEnergy());
        resources.setCurrentStamina(getMaxStamina());
        resources.setRelease(5);
        resources.setAlignment(100);
    }

    public double getStatScaling(String statName) {
        int raceId = character.getRace();
        String className = character.getCharacterClass();

        RaceStatsConfig raceConfig = ConfigManager.getRaceConfig(raceId);
        ClassStatsConfig classConfig = raceConfig.getClassConfig(className);
        ClassStatsConfig.StatScaling scaling = classConfig.getStatScaling();

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

    public void tick() {
        cooldowns.tick();
    }
}

