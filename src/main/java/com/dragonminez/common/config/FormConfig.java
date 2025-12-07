package com.dragonminez.common.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

public class FormConfig {

    @JsonProperty("group_name")
    private String groupName;

    @JsonProperty("forms")
    private Map<String, FormData> forms = new LinkedHashMap<>();

    public FormConfig() {}

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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
        @JsonProperty("name")
        private String name = "";

        @JsonProperty("custom_model")
        private String customModel = "";

        @JsonProperty("body_color_1")
        private String bodyColor1 = "";

        @JsonProperty("body_color_2")
        private String bodyColor2 = "";

        @JsonProperty("body_color_3")
        private String bodyColor3 = "";

        @JsonProperty("hair_type")
        private int hairType = -1;

        @JsonProperty("hair_color")
        private String hairColor = "";

        @JsonProperty("eye1_color")
        private String eye1Color = "";

        @JsonProperty("eye2_color")
        private String eye2Color = "";

        @JsonProperty("aura_color")
        private String auraColor = "";

        @JsonProperty("model_scaling")
        private double modelScaling = 1.0;

        @JsonProperty("str_mult")
        private double strMultiplier = 1.0;

        @JsonProperty("skp_mult")
        private double skpMultiplier = 1.0;

        @JsonProperty("stm_mult")
        private double stmMultiplier = 1.0;

        @JsonProperty("def_mult")
        private double defMultiplier = 1.0;

        @JsonProperty("vit_mult")
        private double vitMultiplier = 1.0;

        @JsonProperty("pwr_mult")
        private double pwrMultiplier = 1.0;

        @JsonProperty("ene_mult")
        private double eneMultiplier = 1.0;

        @JsonProperty("speed_mult")
        private double speedMultiplier = 1.0;

        @JsonProperty("energy_drain")
        private double energyDrain = 1.0;

        public FormData() {}

        public String getName() { return name; }
        public String getCustomModel() { return customModel; }
        public String getBodyColor1() { return bodyColor1; }
        public String getBodyColor2() { return bodyColor2; }
        public String getBodyColor3() { return bodyColor3; }
        public int getHairType() { return hairType; }
        public String getHairColor() { return hairColor; }
        public String getEye1Color() { return eye1Color; }
        public String getEye2Color() { return eye2Color; }
        public String getAuraColor() { return auraColor; }
        public double getModelScaling() { return modelScaling; }
        public double getStrMultiplier() { return strMultiplier; }
        public double getSkpMultiplier() { return skpMultiplier; }
        public double getStmMultiplier() { return stmMultiplier; }
        public double getDefMultiplier() { return defMultiplier; }
        public double getVitMultiplier() { return vitMultiplier; }
        public double getPwrMultiplier() { return pwrMultiplier; }
        public double getEneMultiplier() { return eneMultiplier; }
        public double getSpeedMultiplier() { return speedMultiplier; }
        public double getEnergyDrain() { return energyDrain; }

        public void setName(String name) { this.name = name; }
        public void setCustomModel(String customModel) { this.customModel = customModel; }
        public void setBodyColor1(String bodyColor1) { this.bodyColor1 = bodyColor1; }
        public void setBodyColor2(String bodyColor2) { this.bodyColor2 = bodyColor2; }
        public void setBodyColor3(String bodyColor3) { this.bodyColor3 = bodyColor3; }
        public void setHairType(int hairType) { this.hairType = hairType; }
        public void setHairColor(String hairColor) { this.hairColor = hairColor; }
        public void setEye1Color(String eye1Color) { this.eye1Color = eye1Color; }
        public void setEye2Color(String eye2Color) { this.eye2Color = eye2Color; }
        public void setAuraColor(String auraColor) { this.auraColor = auraColor; }
        public void setModelScaling(double modelScaling) { this.modelScaling = modelScaling; }
        public void setStrMultiplier(double strMultiplier) { this.strMultiplier = strMultiplier; }
        public void setSkpMultiplier(double skpMultiplier) { this.skpMultiplier = skpMultiplier; }
        public void setStmMultiplier(double stmMultiplier) { this.stmMultiplier = stmMultiplier; }
        public void setDefMultiplier(double defMultiplier) { this.defMultiplier = defMultiplier; }
        public void setVitMultiplier(double vitMultiplier) { this.vitMultiplier = vitMultiplier; }
        public void setPwrMultiplier(double pwrMultiplier) { this.pwrMultiplier = pwrMultiplier; }
        public void setEneMultiplier(double eneMultiplier) { this.eneMultiplier = eneMultiplier; }
        public void setSpeedMultiplier(double speedMultiplier) { this.speedMultiplier = speedMultiplier; }
        public void setEnergyDrain(double energyDrain) { this.energyDrain = energyDrain; }

        public boolean hasCustomModel() {
            return customModel != null && !customModel.isEmpty();
        }

        public boolean hasBodyColorOverride() {
            return !bodyColor1.isEmpty() || !bodyColor2.isEmpty() || !bodyColor3.isEmpty();
        }

        public boolean hasHairTypeOverride() {
            return hairType >= 0;
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

