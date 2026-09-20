package com.dragonminez.common.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@Setter
public class GeneralUserConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	@Setter
	private String configVersion;

	private Boolean firstPersonAnimated = true;
	private boolean impactFramesEnabled = false;
	private Boolean alwaysVisibleHudValues = false;
	private Boolean hideHudNumbers = false;
	private Boolean advancedDescription = true;
	private Boolean advancedDescriptionPercentage = true;
	public static final String HUD_STYLE_LEGACY_1 = "legacy 1";
	public static final String HUD_STYLE_LEGACY_2 = "legacy 2";
	public static final String HUD_STYLE_DEFAULT = "default";
	public static final String HUD_STYLE_MINECRAFT = "minecraft";

	private String hudStyle = HUD_STYLE_DEFAULT;
	private Map<String, Map<String, HudPlacement>> hudLayout = new LinkedHashMap<>();

	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Boolean alternativeHud = null;
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Boolean techniqueHotbarRightSide = null;
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Integer xenoverseHudPosX = null;
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Integer xenoverseHudPosY = null;
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Float xenoverseHudScale = null;
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Integer healthBarPosX = null;
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Integer healthBarPosY = null;
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Integer energyBarPosX = null;
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Integer energyBarPosY = null;
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Integer staminaBarPosX = null;
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Integer staminaBarPosY = null;
	private Boolean hexagonStatsDisplay = false;
	private Float menuScaleMultiplier = 1.0f;
	private Float utilityMenuScaleMultiplier = 1.0f;
	private Boolean cameraMovementDuringFlight = true;
	private Boolean liveCrowdinTranslations = true;
	private Boolean showAccumulativeDamage = true;
	private Boolean taiyokenInvertPalette = false;
	private Boolean transformationOutlines = true;
	private Boolean aura3DPersonal = false;
	private Boolean aura3DEntities = false;
	private String aura3DStyle = FormConfig.AURA_3D_SMOOTH;

	public static final String HAIR_PHYSICS_HIGH = "HIGH";
	public static final String HAIR_PHYSICS_LOW = "LOW";
	public static final String HAIR_PHYSICS_OFF = "OFF";

	private String hairPhysicsQuality = HAIR_PHYSICS_HIGH;

	public String getHairPhysicsQuality() {
		if (hairPhysicsQuality == null) return HAIR_PHYSICS_HIGH;
		String normalized = hairPhysicsQuality.trim().toUpperCase(Locale.ROOT);
		if (!normalized.equals(HAIR_PHYSICS_LOW) && !normalized.equals(HAIR_PHYSICS_OFF)) return HAIR_PHYSICS_HIGH;
		return normalized;
	}

	private Integer overShoulderMode = 2;
	private Boolean overShoulderLeft = false;
	private Float overShoulderBack = 3.0f;
	private Float overShoulderUp = 0.35f;
	private Float overShoulderSide = 1.45f;
	private Float overShoulderSmoothing = 0.4f;

	public String getHudStyle() {
		if (alternativeHud != null) {
			if (alternativeHud && HUD_STYLE_DEFAULT.equals(hudStyle)) hudStyle = HUD_STYLE_MINECRAFT;
			alternativeHud = null;
		}
		xenoverseHudPosX = xenoverseHudPosY = null;
		xenoverseHudScale = null;
		techniqueHotbarRightSide = null;
		healthBarPosX = healthBarPosY = energyBarPosX = energyBarPosY = staminaBarPosX = staminaBarPosY = null;
		String normalized = hudStyle == null ? "" : hudStyle.trim().toLowerCase(Locale.ROOT).replace("_", " ").replaceAll("\\s+", " ");
		switch (normalized) {
			case "legacy1" -> normalized = HUD_STYLE_LEGACY_1;
			case "legacy2" -> normalized = HUD_STYLE_LEGACY_2;
			case HUD_STYLE_LEGACY_1, HUD_STYLE_LEGACY_2, HUD_STYLE_MINECRAFT -> {}
			default -> normalized = HUD_STYLE_DEFAULT;
		}
		hudStyle = normalized;
		return hudStyle;
	}

	public Map<String, Map<String, HudPlacement>> getHudLayout() {
		if (hudLayout == null) hudLayout = new LinkedHashMap<>();
		return hudLayout;
	}

	public Integer getOverShoulderMode() {
		if (overShoulderMode == null || overShoulderMode < 0 || overShoulderMode > 2) overShoulderMode = 2;
		return overShoulderMode;
	}

	public Boolean getOverShoulderLeft() {
		if (overShoulderLeft == null) overShoulderLeft = false;
		return overShoulderLeft;
	}

	public Float getOverShoulderBack() {
		if (overShoulderBack == null || !Float.isFinite(overShoulderBack)) overShoulderBack = 3.0f;
		return overShoulderBack;
	}

	public Float getOverShoulderUp() {
		if (overShoulderUp == null || !Float.isFinite(overShoulderUp)) overShoulderUp = 0.35f;
		return overShoulderUp;
	}

	public Float getOverShoulderSide() {
		if (overShoulderSide == null || !Float.isFinite(overShoulderSide)) overShoulderSide = 1.45f;
		return overShoulderSide;
	}

	public Float getOverShoulderSmoothing() {
		if (overShoulderSmoothing == null || !Float.isFinite(overShoulderSmoothing) || overShoulderSmoothing <= 0.0f) overShoulderSmoothing = 0.4f;
		return Math.min(overShoulderSmoothing, 1.0f);
	}

	public Boolean getTaiyokenInvertPalette() {
		if (taiyokenInvertPalette == null) taiyokenInvertPalette = false;
		return taiyokenInvertPalette;
	}

	public Boolean getAura3DPersonal() {
		if (aura3DPersonal == null) aura3DPersonal = false;
		return aura3DPersonal;
	}

	public Boolean getAura3DEntities() {
		if (aura3DEntities == null) aura3DEntities = false;
		return aura3DEntities;
	}

	public String getAura3DStyle() {
		aura3DStyle = FormConfig.sanitizeAura3DPreference(aura3DStyle);
		return aura3DStyle;
	}

	public Boolean getTransformationOutlines() {
		if (transformationOutlines == null) transformationOutlines = true;
		return transformationOutlines;
	}

	public Boolean getShowAccumulativeDamage() {
		if (showAccumulativeDamage == null) showAccumulativeDamage = true;
		return showAccumulativeDamage;
	}

	public Float getMenuScaleMultiplier() {
		if (!Float.isFinite(menuScaleMultiplier) || menuScaleMultiplier <= 0.0f) menuScaleMultiplier = 1.0f;
		return menuScaleMultiplier;
	}

	public Float getUtilityMenuScaleMultiplier() {
		if (utilityMenuScaleMultiplier == null || !Float.isFinite(utilityMenuScaleMultiplier) || utilityMenuScaleMultiplier <= 0.0f) utilityMenuScaleMultiplier = 1.0f;
		return utilityMenuScaleMultiplier;
	}

	public void setUtilityMenuScaleMultiplier(Float utilityMenuScaleMultiplier) {
		if (utilityMenuScaleMultiplier == null || !Float.isFinite(utilityMenuScaleMultiplier) || utilityMenuScaleMultiplier <= 0.0f) {
			this.utilityMenuScaleMultiplier = 1.0f;
			return;
		}
		this.utilityMenuScaleMultiplier = utilityMenuScaleMultiplier;
	}

	public Boolean getHideHudNumbers() {
		if (hideHudNumbers == null) hideHudNumbers = false;
		return hideHudNumbers;
	}

	public void setMenuScaleMultiplier(Float menuScaleMultiplier) {
		if (!Float.isFinite(menuScaleMultiplier) || menuScaleMultiplier <= 0.0f) {
			this.menuScaleMultiplier = 1.0f;
			return;
		}
		this.menuScaleMultiplier = menuScaleMultiplier;
	}
}
