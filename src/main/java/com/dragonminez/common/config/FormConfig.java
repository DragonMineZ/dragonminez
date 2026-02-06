package com.dragonminez.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class FormConfig {
    private String groupName;
    private String formType = "super";
    private Map<String, FormData> forms = new LinkedHashMap<>();

    public FormConfig() {}

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getFormType() {
        return formType;
    }

    public void setFormType(String formType) {
        this.formType = formType;
    }


    public Map<String, FormData> getForms() {
        return forms;
    }

    public void setForms(Map<String, FormData> forms) {
        this.forms = forms;
    }

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

    public static class FormData {
        private String name = "";
        private int unlockOnSkillLevel = 0;
        private String customModel = "";
        private String bodyColor1 = "";
        private String bodyColor2 = "";
        private String bodyColor3 = "";
        private String hairCode = "";
        private String hairColor = "";
        private String eye1Color = "";
        private String eye2Color = "";
        private String auraColor = "";
		private boolean hasLightnings = false;
		private String lightningColor = "";
        private float[] modelScaling = {0.9375f, 0.9375f, 0.9375f};
        private double strMultiplier = 1.0;
        private double skpMultiplier = 1.0;
        private double stmMultiplier = 1.0;
        private double defMultiplier = 1.0;
        private double vitMultiplier = 1.0;
        private double pwrMultiplier = 1.0;
        private double eneMultiplier = 1.0;
        private double speedMultiplier = 1.0;
        private double energyDrain = 1.0;
        private double staminaDrain = 1.0;
        private double attackSpeed = 1.0;
        private double maxMastery = 100.0;
        private double masteryPerHit = 0.01;
        private double masteryPerDamageReceived = 0.01;
        private double statMultPerMasteryPoint = 0.02;
        private double costDecreasePerMasteryPoint = 0.02;
		private boolean kaiokenStackable = true;
		private double kaiokenDrainMultiplier = 2.0;

        public FormData() {}

        public String getName() { return name; }
        public int getUnlockOnSkillLevel() { return unlockOnSkillLevel; }
        public String getCustomModel() { return customModel; }
        public String getBodyColor1() { return bodyColor1; }
        public String getBodyColor2() { return bodyColor2; }
        public String getBodyColor3() { return bodyColor3; }
        public String getHairCode() { return hairCode; }
        public String getHairColor() { return hairColor; }
        public String getEye1Color() { return eye1Color; }
        public String getEye2Color() { return eye2Color; }
        public String getAuraColor() { return auraColor; }
		public boolean hasLightnings() { return hasLightnings; }
		public String getLightningColor() { return lightningColor; }
        public float[] getModelScaling() { return modelScaling; }
        public double getStrMultiplier() { return Math.max(0.01, strMultiplier); }
        public double getSkpMultiplier() { return Math.max(0.01, skpMultiplier); }
        public double getStmMultiplier() { return Math.max(0.01, stmMultiplier); }
        public double getDefMultiplier() { return Math.max(0.01, defMultiplier); }
        public double getVitMultiplier() { return Math.max(0.01, vitMultiplier); }
        public double getPwrMultiplier() { return Math.max(0.01, pwrMultiplier); }
        public double getEneMultiplier() { return Math.max(0.01, eneMultiplier); }
        public double getSpeedMultiplier() { return Math.max(0.01, speedMultiplier); }
        public double getEnergyDrain() { return Math.max(0, energyDrain); }
        public double getStaminaDrain() { return Math.max(0, staminaDrain); }
        public double getAttackSpeed() { return Math.max(0.1, attackSpeed); }
        public double getMaxMastery() { return maxMastery; }
        public double getMasteryPerHit() { return Math.max(0,  masteryPerHit); }
        public double getMasteryPerDamageReceived() { return Math.max(0, masteryPerDamageReceived); }
        public double getStatMultPerMasteryPoint() { return Math.max(0, statMultPerMasteryPoint); }
        public double getCostDecreasePerMasteryPoint() { return Math.max(0, costDecreasePerMasteryPoint); }
		public boolean isKaiokenStackable() { return kaiokenStackable; }
		public double getKaiokenDrainMultiplier() { return Math.max(0.01, kaiokenDrainMultiplier); }

        public void setName(String name) { this.name = name; }
        public void setUnlockOnSkillLevel(int level) { this.unlockOnSkillLevel = level; }
        public void setCustomModel(String customModel) { this.customModel = customModel; }
        public void setBodyColor1(String bodyColor1) { this.bodyColor1 = bodyColor1; }
        public void setBodyColor2(String bodyColor2) { this.bodyColor2 = bodyColor2; }
        public void setBodyColor3(String bodyColor3) { this.bodyColor3 = bodyColor3; }
        public void setHairCode(String hairCode) { this.hairCode = hairCode; }
        public void setHairColor(String hairColor) { this.hairColor = hairColor; }
        public void setEye1Color(String eye1Color) { this.eye1Color = eye1Color; }
        public void setEye2Color(String eye2Color) { this.eye2Color = eye2Color; }
        public void setAuraColor(String auraColor) { this.auraColor = auraColor; }
		public void setHasLightnings(boolean hasLightnings) { this.hasLightnings = hasLightnings; }
		public void setLightningColor(String lightningColor) { this.lightningColor = lightningColor; }
        public void setModelScaling(float[] modelScaling) { this.modelScaling = modelScaling; }
        public void setStrMultiplier(double strMultiplier) { this.strMultiplier = strMultiplier; }
        public void setSkpMultiplier(double skpMultiplier) { this.skpMultiplier = skpMultiplier; }
        public void setStmMultiplier(double stmMultiplier) { this.stmMultiplier = stmMultiplier; }
        public void setDefMultiplier(double defMultiplier) { this.defMultiplier = defMultiplier; }
        public void setVitMultiplier(double vitMultiplier) { this.vitMultiplier = vitMultiplier; }
        public void setPwrMultiplier(double pwrMultiplier) { this.pwrMultiplier = pwrMultiplier; }
        public void setEneMultiplier(double eneMultiplier) { this.eneMultiplier = eneMultiplier; }
        public void setSpeedMultiplier(double speedMultiplier) { this.speedMultiplier = speedMultiplier; }
        public void setEnergyDrain(double energyDrain) { this.energyDrain = energyDrain; }
        public void setStaminaDrain(double staminaDrain) { this.staminaDrain = staminaDrain; }
        public void setAttackSpeed(double attackSpeed) { this.attackSpeed = attackSpeed; }
        public void setMaxMastery(double maxMastery) { this.maxMastery = maxMastery; }
        public void setMasteryPerHit(double masteryPerHit) { this.masteryPerHit = masteryPerHit; }
        public void setMasteryPerDamageReceived(double masteryPerDamageReceived) { this.masteryPerDamageReceived = masteryPerDamageReceived; }
        public void setStatMultPerMasteryPoint(double statMultPerMasteryPoint) { this.statMultPerMasteryPoint = statMultPerMasteryPoint; }
        public void setCostDecreasePerMasteryPoint(double costDecreasePerMasteryPoint) { this.costDecreasePerMasteryPoint = costDecreasePerMasteryPoint; }
		public void setKaiokenStackable(boolean kaiokenStackable) { this.kaiokenStackable = kaiokenStackable; }
		public void setKaiokenDrainMultiplier(double kaiokenDrainMultiplier) { this.kaiokenDrainMultiplier = kaiokenDrainMultiplier; }

        public boolean hasCustomModel() {
            return customModel != null && !customModel.isEmpty();
        }

        public boolean hasBodyColorOverride() {
            return !bodyColor1.isEmpty() || !bodyColor2.isEmpty() || !bodyColor3.isEmpty();
        }

        public boolean hasHairCodeOverride() {
            return !hairCode.equals("");
        }

        public boolean hasHairColorOverride() {
            return hairColor != null && !hairColor.isEmpty();
        }

        public boolean hasEyeColorOverride() {
            return !eye1Color.isEmpty() || !eye2Color.isEmpty();
        }

        public boolean hasAuraColorOverride() {
            return auraColor != null && !auraColor.isEmpty();
        }
	}
}

