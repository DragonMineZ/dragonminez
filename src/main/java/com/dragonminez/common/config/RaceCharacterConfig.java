package com.dragonminez.common.config;

import com.google.gson.annotations.SerializedName;


public class RaceCharacterConfig {

    @SerializedName("raceName")
    private String raceName;

    @SerializedName("hasGender")
    private boolean hasGender = true;

    @SerializedName("useVanillaSkin")
    private boolean useVanillaSkin = false;

    @SerializedName("customModel")
    private String customModel = "";

	@SerializedName("defaultModelScaling")
	private float defaultModelScaling = 0.9375f;

    @SerializedName("defaultBodyType")
    private int defaultBodyType = 0;

    @SerializedName("defaultHairType")
    private int defaultHairType = 0;

    @SerializedName("defaultEyesType")
    private int defaultEyesType = 0;

    @SerializedName("defaultNoseType")
    private int defaultNoseType = 0;

    @SerializedName("defaultMouthType")
    private int defaultMouthType = 0;

    @SerializedName("defaultBodyColor")
	private String defaultBodyColor = null;

    @SerializedName("defaultBodyColor2")
    private String defaultBodyColor2 = null;

    @SerializedName("defaultBodyColor3")
    private String defaultBodyColor3 = null;

    @SerializedName("defaultHairColor")
    private String defaultHairColor = null;

    @SerializedName("defaultEye1Color")
    private String defaultEye1Color = null;

    @SerializedName("defaultEye2Color")
    private String defaultEye2Color = null;

    @SerializedName("defaultAuraColor")
    private String defaultAuraColor = null;

    public RaceCharacterConfig() {}

    public String getRaceName() { return raceName; }
    public boolean hasGender() { return hasGender; }
    public boolean useVanillaSkin() { return useVanillaSkin; }
	public String getCustomModel() { return customModel; }
	public float getDefaultModelScaling() { return defaultModelScaling; }
    public int getDefaultBodyType() { return defaultBodyType; }
    public int getDefaultHairType() { return defaultHairType; }
    public int getDefaultEyesType() { return defaultEyesType; }
    public int getDefaultNoseType() { return defaultNoseType; }
    public int getDefaultMouthType() { return defaultMouthType; }
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
	public void setCustomModel(String customModel) { this.customModel = customModel; }
	public void setDefaultModelScaling(float defaultModelScaling) { this.defaultModelScaling = defaultModelScaling; }
    public void setDefaultBodyType(int defaultBodyType) { this.defaultBodyType = defaultBodyType; }
    public void setDefaultHairType(int defaultHairType) { this.defaultHairType = defaultHairType; }
    public void setDefaultEyesType(int defaultEyesType) { this.defaultEyesType = defaultEyesType; }
    public void setDefaultNoseType(int defaultNoseType) { this.defaultNoseType = defaultNoseType; }
    public void setDefaultMouthType(int defaultMouthType) { this.defaultMouthType = defaultMouthType; }
    public void setDefaultBodyColor(String defaultBodyColor) { this.defaultBodyColor = defaultBodyColor; }
    public void setDefaultBodyColor2(String defaultBodyColor2) { this.defaultBodyColor2 = defaultBodyColor2; }
    public void setDefaultBodyColor3(String defaultBodyColor3) { this.defaultBodyColor3 = defaultBodyColor3; }
    public void setDefaultHairColor(String defaultHairColor) { this.defaultHairColor = defaultHairColor; }
    public void setDefaultEye1Color(String defaultEye1Color) { this.defaultEye1Color = defaultEye1Color; }
    public void setDefaultEye2Color(String defaultEye2Color) { this.defaultEye2Color = defaultEye2Color; }
    public void setDefaultAuraColor(String defaultAuraColor) { this.defaultAuraColor = defaultAuraColor; }
}
