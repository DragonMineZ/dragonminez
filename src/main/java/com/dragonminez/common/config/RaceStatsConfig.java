package com.dragonminez.common.config;

import com.fasterxml.jackson.annotation.JsonProperty;


public class RaceStatsConfig {

    @JsonProperty("race_name")
    private String raceName;

    @JsonProperty("base_stats")
    private BaseStats baseStats = new BaseStats();

    @JsonProperty("stat_scaling")
    private StatScaling statScaling = new StatScaling();

    public RaceStatsConfig() {}

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public BaseStats getBaseStats() {
        return baseStats;
    }

    public void setBaseStats(BaseStats baseStats) {
        this.baseStats = baseStats;
    }

    public StatScaling getStatScaling() {
        return statScaling;
    }

    public void setStatScaling(StatScaling statScaling) {
        this.statScaling = statScaling;
    }

    public static class BaseStats {
        @JsonProperty("STR")
        private int strength = 5;

        @JsonProperty("SKP")
        private int strikePower = 5;

        @JsonProperty("RES")
        private int resistance = 5;

        @JsonProperty("VIT")
        private int vitality = 5;

        @JsonProperty("PWR")
        private int kiPower = 5;

        @JsonProperty("ENE")
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
        @JsonProperty("STR_scaling")
        private double strengthScaling = 1.0;

        @JsonProperty("SKP_scaling")
        private double strikePowerScaling = 1.0;

        @JsonProperty("STM_scaling")
        private double staminaScaling = 2.0;

        @JsonProperty("DEF_scaling")
        private double defenseScaling = 0.5;

        @JsonProperty("VIT_scaling")
        private double vitalityScaling = 1.0;

        @JsonProperty("PWR_scaling")
        private double kiPowerScaling = 1.0;

        @JsonProperty("ENE_scaling")
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

