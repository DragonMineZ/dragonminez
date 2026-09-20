package com.dragonminez.common.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private String hudStyle = null;
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Map<String, Map<String, HudPlacement>> hudLayout = null;

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
	public static final float DEFAULT_MENU_SCALE = 0.75f;
	private Float menuScaleMultiplier = DEFAULT_MENU_SCALE;
	private Float utilityMenuScaleMultiplier = 1.0f;
	private Boolean cameraMovementDuringFlight = true;
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Boolean liveCrowdinTranslations = null;
	private Boolean tutorialsEnabled = true;
	private List<String> tutorialsSeen = new ArrayList<>();
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

	public boolean migrateLegacyHud(HudLayoutConfig target, boolean targetIsNew) {
		boolean changed = false;
		if (targetIsNew && (hudStyle != null || alternativeHud != null)) {
			String legacyStyle = hudStyle != null ? HudLayoutConfig.normalizeStyle(hudStyle) : HUD_STYLE_DEFAULT;
			if (Boolean.TRUE.equals(alternativeHud) && HUD_STYLE_DEFAULT.equals(legacyStyle)) legacyStyle = HUD_STYLE_MINECRAFT;
			target.setStyle(legacyStyle);
			changed = true;
		}
		if (hudLayout != null && !hudLayout.isEmpty() && target.getLayout().isEmpty()) {
			target.getLayout().putAll(hudLayout);
			changed = true;
		}
		if (hudStyle != null || hudLayout != null || alternativeHud != null) changed = true;
		hudStyle = null;
		hudLayout = null;
		alternativeHud = null;
		xenoverseHudPosX = xenoverseHudPosY = null;
		xenoverseHudScale = null;
		techniqueHotbarRightSide = null;
		liveCrowdinTranslations = null;
		healthBarPosX = healthBarPosY = energyBarPosX = energyBarPosY = staminaBarPosX = staminaBarPosY = null;
		return changed;
	}

	public Boolean getTutorialsEnabled() {
		if (tutorialsEnabled == null) tutorialsEnabled = true;
		return tutorialsEnabled;
	}

	public List<String> getTutorialsSeen() {
		if (tutorialsSeen == null) tutorialsSeen = new ArrayList<>();
		return tutorialsSeen;
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
		if (menuScaleMultiplier == null || !Float.isFinite(menuScaleMultiplier) || menuScaleMultiplier <= 0.0f) menuScaleMultiplier = DEFAULT_MENU_SCALE;
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
		if (menuScaleMultiplier == null || !Float.isFinite(menuScaleMultiplier) || menuScaleMultiplier <= 0.0f) {
			this.menuScaleMultiplier = DEFAULT_MENU_SCALE;
			return;
		}
		this.menuScaleMultiplier = menuScaleMultiplier;
	}
}
