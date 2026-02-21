package com.dragonminez.common.config;

import com.google.gson.annotations.SerializedName;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class RaceStatsConfig {
    public static final int CURRENT_VERSION = 2;
    private int configVersion = CURRENT_VERSION;
    public int getConfigVersion() { return configVersion; }
    public void setConfigVersion(int configVersion) { this.configVersion = configVersion; }

    private final Map<String, ClassStats> classes = new HashMap<>();
    public RaceStatsConfig() {}

    public ClassStats getClassStats(String characterClass) {
        if (!this.classes.containsKey(characterClass)) {
            this.classes.put(characterClass, new ClassStats());
        }
        return this.classes.get(characterClass);
    }

    public Collection<String> getAllClasses() {
        return this.classes.keySet();
    }

    public static class ClassStats {
        private BaseStats baseStats = new BaseStats();
        private StatScaling statScaling = new StatScaling();
        private double healthRegenRate = 0.0025;
        private double energyRegenRate = 0.01;
        private double staminaRegenRate = 0.01;

        public BaseStats getBaseStats() { return baseStats; }
        public StatScaling getStatScaling() { return statScaling; }
        public double getHealthRegenRate() { return healthRegenRate; }
        public void setHealthRegenRate(double healthRegenRate) { this.healthRegenRate = healthRegenRate; }
        public double getEnergyRegenRate() { return energyRegenRate; }
        public void setEnergyRegenRate(double energyRegenRate) { this.energyRegenRate = energyRegenRate; }
        public double getStaminaRegenRate() { return staminaRegenRate; }
        public void setStaminaRegenRate(double staminaRegenRate) { this.staminaRegenRate = staminaRegenRate; }
    }

    public static class BaseStats {
        @SerializedName("STR")
        private int strength = 5;
        @SerializedName("SKP")
        private int strikePower = 5;
        @SerializedName("RES")
        private int resistance = 5;
        @SerializedName("VIT")
        private int vitality = 5;
        @SerializedName("PWR")
        private int kiPower = 5;
        @SerializedName("ENE")
        private int energy = 5;

        public int getStrength() { return strength; }
        public int getStrikePower() { return strikePower; }
        public int getResistance() { return resistance; }
        public int getVitality() { return vitality; }
        public int getKiPower() { return kiPower; }
        public int getEnergy() { return energy; }

        public void setStrength(int strength) { this.strength = strength; }
        public void setStrikePower(int strikePower) { this.strikePower = strikePower; }
        public void setResistance(int resistance) { this.resistance = resistance; }
        public void setVitality(int vitality) { this.vitality = vitality; }
        public void setKiPower(int kiPower) { this.kiPower = kiPower; }
        public void setEnergy(int energy) { this.energy = energy; }
    }

    public static class StatScaling {
        @SerializedName("STR_scaling")
        private double strengthScaling = 1.0;
        @SerializedName("SKP_scaling")
        private double strikePowerScaling = 1.0;
        @SerializedName("STM_scaling")
        private double staminaScaling = 1.0;
        @SerializedName("DEF_scaling")
        private double defenseScaling = 1.0;
        @SerializedName("VIT_scaling")
        private double vitalityScaling = 1.0;
        @SerializedName("PWR_scaling")
        private double kiPowerScaling = 1.0;
        @SerializedName("ENE_scaling")
        private double energyScaling = 1.0;

        public double getStrengthScaling() { return strengthScaling; }
        public double getStrikePowerScaling() { return strikePowerScaling; }
        public double getStaminaScaling() { return staminaScaling; }
        public double getDefenseScaling() { return defenseScaling; }
        public double getVitalityScaling() { return vitalityScaling; }
        public double getKiPowerScaling() { return kiPowerScaling; }
        public double getEnergyScaling() { return energyScaling; }

        public void setStrengthScaling(double strengthScaling) { this.strengthScaling = strengthScaling; }
        public void setStrikePowerScaling(double strikePowerScaling) { this.strikePowerScaling = strikePowerScaling; }
        public void setStaminaScaling(double staminaScaling) { this.staminaScaling = staminaScaling; }
        public void setDefenseScaling(double defenseScaling) { this.defenseScaling = defenseScaling; }
        public void setVitalityScaling(double vitalityScaling) { this.vitalityScaling = vitalityScaling; }
        public void setKiPowerScaling(double kiPowerScaling) { this.kiPowerScaling = kiPowerScaling; }
        public void setEnergyScaling(double energyScaling) { this.energyScaling = energyScaling; }
    }
}

