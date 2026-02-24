package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
public class FormConfig {
    public static final int CURRENT_VERSION = 2;
    private int configVersion;

    private String groupName;
    private String formType = "super";
    private Map<String, FormData> forms = new LinkedHashMap<>();

    public FormData getForm(String formName) {
        for (FormData formData : forms.values()) {
            if (formData.getName().equalsIgnoreCase(formName)) {
                return formData;
            }
        }
        return null;
    }

    public FormData getFormByKey(String key) {
        return forms.get(key);
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class FormData {
        private String name = "";
        private Integer unlockOnSkillLevel = 0;
        private String customModel = "";
        private String bodyColor1 = "";
        private String bodyColor2 = "";
        private String bodyColor3 = "";
        private String hairType = "";
        private String forcedHairCode = "";
        private String hairColor = "";
        private String eye1Color = "";
        private String eye2Color = "";
        private String auraColor = "";
        private Boolean hasLightnings = false;
        private String lightningColor = "";
        private Float[] modelScaling = {0.9375f, 0.9375f, 0.9375f};
        private Double strMultiplier = 1.0;
        private Double skpMultiplier = 1.0;
        private Double stmMultiplier = 1.0;
        private Double defMultiplier = 1.0;
        private Double vitMultiplier = 1.0;
        private Double pwrMultiplier = 1.0;
        private Double eneMultiplier = 1.0;
        private Double speedMultiplier = 1.0;
        private Double staminaDrainMultiplier = 1.0;
        private Double energyDrain = 0.0;
        private Double staminaDrain = 0.0;
        private Double healthDrain = 0.0;
        private Double attackSpeed = 1.0;
        private Double maxMastery = 100.0;
        private Double masteryPerHit = 0.01;
        private Double masteryPerDamageReceived = 0.01;
        private Double statMultPerMasteryPoint = 0.02;
        private Double costDecreasePerMasteryPoint = 0.02;
        private Double passiveMasteryGainEveryFiveSeconds = 0.001;
        private Boolean formStackable = true;
        private Double stackDrainMultiplier = 2.0;
        private Boolean canAlwaysTransform = false;
        private Boolean directTransformation = false;

        public Double getStrMultiplier() { return Math.max(0.01, strMultiplier); }
        public Double getSkpMultiplier() { return Math.max(0.01, skpMultiplier); }
        public Double getStmMultiplier() { return Math.max(0.01, stmMultiplier); }
        public Double getDefMultiplier() { return Math.max(0.01, defMultiplier); }
        public Double getVitMultiplier() { return Math.max(0.01, vitMultiplier); }
        public Double getPwrMultiplier() { return Math.max(0.01, pwrMultiplier); }
        public Double getEneMultiplier() { return Math.max(0.01, eneMultiplier); }
        public Double getSpeedMultiplier() { return Math.max(0.01, speedMultiplier); }
        public Double getStaminaDrainMultiplier() { return Math.max(0, staminaDrainMultiplier); }
        public Double getEnergyDrain() { return Math.max(0, energyDrain); }
        public Double getStaminaDrain() { return Math.max(0, staminaDrain); }
        public Double getHealthDrain() { return Math.max(0, healthDrain); }
        public Double getAttackSpeed() { return Math.max(0.1, attackSpeed); }
        public Double getMasteryPerHit() { return Math.max(0,  masteryPerHit); }
        public Double getMasteryPerDamageReceived() { return Math.max(0, masteryPerDamageReceived); }
        public Double getStatMultPerMasteryPoint() { return Math.max(0, statMultPerMasteryPoint); }
        public Double getCostDecreasePerMasteryPoint() { return Math.max(0, costDecreasePerMasteryPoint); }
        public Double getPassiveMasteryGainEveryFiveSeconds() { return Math.max(0, passiveMasteryGainEveryFiveSeconds); }
        public Double getStackDrainMultiplier() { return Math.max(0.01, stackDrainMultiplier); }

        public Boolean hasCustomModel() {
            return customModel != null && !customModel.isEmpty();
        }

        public Boolean hasBodyColorOverride() {
            return !bodyColor1.isEmpty() || !bodyColor2.isEmpty() || !bodyColor3.isEmpty();
        }

        public Boolean hasDefinedHairType() {
            return hairType != null && !hairType.isEmpty();
        }

        public Boolean hasHairCodeOverride() {
            return !forcedHairCode.isEmpty();
        }

        public Boolean hasHairColorOverride() {
            return hairColor != null && !hairColor.isEmpty();
        }

        public Boolean hasEyeColorOverride() {
            return !eye1Color.isEmpty() || !eye2Color.isEmpty();
        }

        public Boolean hasAuraColorOverride() {
            return auraColor != null && !auraColor.isEmpty();
        }
    }
}