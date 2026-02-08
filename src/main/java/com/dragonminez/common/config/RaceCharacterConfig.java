package com.dragonminez.common.config;

public class RaceCharacterConfig {
    private String raceName;
    private boolean hasGender = true;
    private boolean useVanillaSkin = false;
    private String customModel = "";
	private float[] defaultModelScaling = {0.9375f, 0.9375f, 0.9375f};
    private int defaultBodyType = 0;
    private int defaultHairType = 0;
    private boolean canUseHair = true;
    private int defaultEyesType = 0;
    private int defaultNoseType = 0;
    private int defaultMouthType = 0;
	private int defaultTattooType = 0;
	private String defaultBodyColor = null;
    private String defaultBodyColor2 = null;
    private String defaultBodyColor3 = null;
    private String defaultHairColor = null;
    private String defaultEye1Color = null;
    private String defaultEye2Color = null;
    private String defaultAuraColor = null;
    private int[] superformTpCost = null;
    private int[] godformTpCost = null;
    private int[] legendaryformsTpCost = null;
    private int[] androidformsTpCost = null;

    public RaceCharacterConfig() {}

    public String getRaceName() { return raceName; }
    public boolean hasGender() { return hasGender; }
    public boolean useVanillaSkin() { return useVanillaSkin; }
	public String getCustomModel() { return customModel; }
	public float[] getDefaultModelScaling() { return defaultModelScaling; }
    public int getDefaultBodyType() { return defaultBodyType; }
    public int getDefaultHairType() { return defaultHairType; }
    public boolean canUseHair() { return canUseHair; }
    public int getDefaultEyesType() { return defaultEyesType; }
    public int getDefaultNoseType() { return defaultNoseType; }
    public int getDefaultMouthType() { return defaultMouthType; }
	public int getDefaultTattooType() { return defaultTattooType; }
    public String getDefaultBodyColor() { return defaultBodyColor; }
    public String getDefaultBodyColor2() { return defaultBodyColor2; }
    public String getDefaultBodyColor3() { return defaultBodyColor3; }
    public String getDefaultHairColor() { return defaultHairColor; }
    public String getDefaultEye1Color() { return defaultEye1Color; }
    public String getDefaultEye2Color() { return defaultEye2Color; }
    public String getDefaultAuraColor() { return defaultAuraColor; }
    public int[] getSuperformTpCost() { return superformTpCost; }
    public int[] getGodformTpCost() { return godformTpCost; }
    public int[] getLegendaryformsTpCost() { return legendaryformsTpCost; }
    public int[] getAndroidformsTpCost() { return androidformsTpCost; }

    public void setRaceName(String raceName) { this.raceName = raceName; }
    public void setHasGender(boolean hasGender) { this.hasGender = hasGender; }
    public void setUseVanillaSkin(boolean useVanillaSkin) { this.useVanillaSkin = useVanillaSkin; }
	public void setCustomModel(String customModel) { this.customModel = customModel; }
	public void setDefaultModelScaling(float[] defaultModelScaling) { this.defaultModelScaling = defaultModelScaling; }
    public void setDefaultBodyType(int defaultBodyType) { this.defaultBodyType = defaultBodyType; }
    public void setDefaultHairType(int defaultHairType) { this.defaultHairType = defaultHairType; }
    public void setCanUseHair(boolean canUseHair) { this.canUseHair = canUseHair; }
    public void setDefaultEyesType(int defaultEyesType) { this.defaultEyesType = defaultEyesType; }
    public void setDefaultNoseType(int defaultNoseType) { this.defaultNoseType = defaultNoseType; }
    public void setDefaultMouthType(int defaultMouthType) { this.defaultMouthType = defaultMouthType; }
	public void setDefaultTattooType(int defaultTattooType) { this.defaultTattooType = defaultTattooType; }
    public void setDefaultBodyColor(String defaultBodyColor) { this.defaultBodyColor = defaultBodyColor; }
    public void setDefaultBodyColor2(String defaultBodyColor2) { this.defaultBodyColor2 = defaultBodyColor2; }
    public void setDefaultBodyColor3(String defaultBodyColor3) { this.defaultBodyColor3 = defaultBodyColor3; }
    public void setDefaultHairColor(String defaultHairColor) { this.defaultHairColor = defaultHairColor; }
    public void setDefaultEye1Color(String defaultEye1Color) { this.defaultEye1Color = defaultEye1Color; }
    public void setDefaultEye2Color(String defaultEye2Color) { this.defaultEye2Color = defaultEye2Color; }
    public void setDefaultAuraColor(String defaultAuraColor) { this.defaultAuraColor = defaultAuraColor; }
    public void setSuperformTpCost(int[] costs) { this.superformTpCost = costs; }
    public void setGodformTpCost(int[] costs) { this.godformTpCost = costs; }
    public void setLegendaryformsTpCost(int[] costs) { this.legendaryformsTpCost = costs; }
    public void setAndroidformsTpCost(int[] costs) { this.androidformsTpCost = costs; }
}
