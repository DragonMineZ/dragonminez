package com.dragonminez.common.config;

import com.fasterxml.jackson.annotation.JsonProperty;


public class RaceCharacterConfig {

    @JsonProperty("race_name")
    private String raceName;

    @JsonProperty("has_gender")
    private boolean hasGender = true;

    @JsonProperty("use_vanilla_skin")
    private boolean useVanillaSkin = false;

    @JsonProperty("custom_model")
    private String customModel = "";

    public String getCustomModel() { return customModel; }
    public void setCustomModel(String customModel) { this.customModel = customModel; }

    @JsonProperty("default_body_type")
    private int defaultBodyType = 0;

    @JsonProperty("default_hair_type")
    private int defaultHairType = 0;

    @JsonProperty("default_eyes_type")
    private int defaultEyesType = 0;

    @JsonProperty("default_body_color")
    private String defaultBodyColor = "#000000";

    @JsonProperty("default_body_color_2")
    private String defaultBodyColor2 = "#000000";

    @JsonProperty("default_body_color_3")
    private String defaultBodyColor3 = "#000000";

    @JsonProperty("default_hair_color")
    private String defaultHairColor = "#0E1051";

    @JsonProperty("default_eye1_color")
    private String defaultEye1Color = "#000000";

    @JsonProperty("default_eye2_color")
    private String defaultEye2Color = "#000000";

    @JsonProperty("default_aura_color")
    private String defaultAuraColor = "#7FFFFF";

    public RaceCharacterConfig() {}

    public String getRaceName() { return raceName; }
    public boolean hasGender() { return hasGender; }
    public boolean useVanillaSkin() { return useVanillaSkin; }
    public int getDefaultBodyType() { return defaultBodyType; }
    public int getDefaultHairType() { return defaultHairType; }
    public int getDefaultEyesType() { return defaultEyesType; }
    public String getDefaultBodyColor() { return defaultBodyColor; }
    public String getDefaultBodyColor2() { return defaultBodyColor2; }
    public String getDefaultBodyColor3() { return defaultBodyColor3; }
    public String getDefaultHairColor() { return defaultHairColor; }
    public String getDefaultEye1Color() { return defaultEye1Color; }
    public String getDefaultEye2Color() { return defaultEye2Color; }
    public String getDefaultAuraColor() { return defaultAuraColor; }

    public void setRaceName(String raceName) { this.raceName = raceName; }
    public void setHasGender(boolean hasGender) { this.hasGender = hasGender; }
    public void setUseVanillaSkin(boolean useVanillaSkin) { this.useVanillaSkin = useVanillaSkin; }
    public void setDefaultBodyType(int defaultBodyType) { this.defaultBodyType = defaultBodyType; }
    public void setDefaultHairType(int defaultHairType) { this.defaultHairType = defaultHairType; }
    public void setDefaultEyesType(int defaultEyesType) { this.defaultEyesType = defaultEyesType; }
    public void setDefaultBodyColor(String defaultBodyColor) { this.defaultBodyColor = defaultBodyColor; }
    public void setDefaultBodyColor2(String defaultBodyColor2) { this.defaultBodyColor2 = defaultBodyColor2; }
    public void setDefaultBodyColor3(String defaultBodyColor3) { this.defaultBodyColor3 = defaultBodyColor3; }
    public void setDefaultHairColor(String defaultHairColor) { this.defaultHairColor = defaultHairColor; }
    public void setDefaultEye1Color(String defaultEye1Color) { this.defaultEye1Color = defaultEye1Color; }
    public void setDefaultEye2Color(String defaultEye2Color) { this.defaultEye2Color = defaultEye2Color; }
    public void setDefaultAuraColor(String defaultAuraColor) { this.defaultAuraColor = defaultAuraColor; }
}
