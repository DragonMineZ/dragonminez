package com.dragonminez.common.config;

import com.fasterxml.jackson.annotation.JsonProperty;


public class RaceCharacterConfig {

    @JsonProperty("race_name")
    private String raceName;

    @JsonProperty("has_gender")
    private boolean hasGender = true;

    @JsonProperty("use_vanilla_skin")
    private boolean useVanillaSkin = false;

    @JsonProperty("default_body_type")
    private int defaultBodyType = 0;

    @JsonProperty("default_hair_type")
    private int defaultHairType = 0;

    @JsonProperty("default_eyes_type")
    private int defaultEyesType = 0;

    @JsonProperty("default_body_color")
    private int defaultBodyColor = 0;

    @JsonProperty("default_body_color_2")
    private int defaultBodyColor2 = 0;

    @JsonProperty("default_body_color_3")
    private int defaultBodyColor3 = 0;

    @JsonProperty("default_hair_color")
    private int defaultHairColor = 921617;

    @JsonProperty("default_eye1_color")
    private int defaultEye1Color = 0;

    @JsonProperty("default_eye2_color")
    private int defaultEye2Color = 0;

    @JsonProperty("default_aura_color")
    private int defaultAuraColor = 8388607;

    public RaceCharacterConfig() {}

    public String getRaceName() { return raceName; }
    public boolean hasGender() { return hasGender; }
    public boolean useVanillaSkin() { return useVanillaSkin; }
    public int getDefaultBodyType() { return defaultBodyType; }
    public int getDefaultHairType() { return defaultHairType; }
    public int getDefaultEyesType() { return defaultEyesType; }
    public int getDefaultBodyColor() { return defaultBodyColor; }
    public int getDefaultBodyColor2() { return defaultBodyColor2; }
    public int getDefaultBodyColor3() { return defaultBodyColor3; }
    public int getDefaultHairColor() { return defaultHairColor; }
    public int getDefaultEye1Color() { return defaultEye1Color; }
    public int getDefaultEye2Color() { return defaultEye2Color; }
    public int getDefaultAuraColor() { return defaultAuraColor; }

    public void setRaceName(String raceName) { this.raceName = raceName; }
    public void setHasGender(boolean hasGender) { this.hasGender = hasGender; }
    public void setUseVanillaSkin(boolean useVanillaSkin) { this.useVanillaSkin = useVanillaSkin; }
    public void setDefaultBodyType(int defaultBodyType) { this.defaultBodyType = defaultBodyType; }
    public void setDefaultHairType(int defaultHairType) { this.defaultHairType = defaultHairType; }
    public void setDefaultEyesType(int defaultEyesType) { this.defaultEyesType = defaultEyesType; }
    public void setDefaultBodyColor(int defaultBodyColor) { this.defaultBodyColor = defaultBodyColor; }
    public void setDefaultBodyColor2(int defaultBodyColor2) { this.defaultBodyColor2 = defaultBodyColor2; }
    public void setDefaultBodyColor3(int defaultBodyColor3) { this.defaultBodyColor3 = defaultBodyColor3; }
    public void setDefaultHairColor(int defaultHairColor) { this.defaultHairColor = defaultHairColor; }
    public void setDefaultEye1Color(int defaultEye1Color) { this.defaultEye1Color = defaultEye1Color; }
    public void setDefaultEye2Color(int defaultEye2Color) { this.defaultEye2Color = defaultEye2Color; }
    public void setDefaultAuraColor(int defaultAuraColor) { this.defaultAuraColor = defaultAuraColor; }
}

