package com.dragonminez.common.config;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class RaceStatsConfig {
    public static final int CURRENT_VERSION = 2;

    @Setter
    private int configVersion;

    private final Map<String, ClassStats> classes = new HashMap<>();

    public ClassStats getClassStats(String characterClass) {
        if (!this.classes.containsKey(characterClass)) {
            this.classes.put(characterClass, new ClassStats());
        }
        return this.classes.get(characterClass);
    }

    public Collection<String> getAllClasses() {
        return this.classes.keySet();
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class ClassStats {
        private BaseStats baseStats = new BaseStats();
        private StatScaling statScaling = new StatScaling();
        private Double healthRegenRate = 0.0025;
        private Double energyRegenRate = 0.01;
        private Double staminaRegenRate = 0.01;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class BaseStats {
        @SerializedName("STR")
        private Integer strength = 5;
        @SerializedName("SKP")
        private Integer strikePower = 5;
        @SerializedName("RES")
        private Integer resistance = 5;
        @SerializedName("VIT")
        private Integer vitality = 5;
        @SerializedName("PWR")
        private Integer kiPower = 5;
        @SerializedName("ENE")
        private Integer energy = 5;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class StatScaling {
        @SerializedName("STR_scaling")
        private Double strengthScaling = 1.0;
        @SerializedName("SKP_scaling")
        private Double strikePowerScaling = 1.0;
        @SerializedName("STM_scaling")
        private Double staminaScaling = 1.0;
        @SerializedName("DEF_scaling")
        private Double defenseScaling = 1.0;
        @SerializedName("VIT_scaling")
        private Double vitalityScaling = 1.0;
        @SerializedName("PWR_scaling")
        private Double kiPowerScaling = 1.0;
        @SerializedName("ENE_scaling")
        private Double energyScaling = 1.0;
    }
}

