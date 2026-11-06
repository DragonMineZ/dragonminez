package com.dragonminez.common.config;

import com.dragonminez.common.diagnostics.RemovedConfigKeys;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RemovedConfigKeys({"techniqueHotbarRightSide", "xenoverseHudPosX", "xenoverseHudPosY", "xenoverseHudScale", "healthBarPosX", "healthBarPosY", "energyBarPosX", "energyBarPosY", "staminaBarPosX", "staminaBarPosY", "liveCrowdinTranslations"})
public class GeneralUserConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;
	private String configVersion;
	private Boolean firstPersonAnimated = true;
	private boolean impactFramesEnabled = false;
	private boolean reserveFirstHotbarSlot = false;
	private int kiSoundVolume = 100;
	private Boolean alwaysVisibleHudValues = false;
	private Boolean hideHudNumbers = false;
	private Boolean partyMarkers = true;
	private Boolean advancedDescription = true;
	private Boolean advancedDescriptionPercentage = true;
	private String hudStyle = null;
	private Map<String, Map<String, HudPlacement>> hudLayout = null;
	private Boolean alternativeHud = null;
	private Boolean hexagonStatsDisplay = false;
	public static final float DEFAULT_MENU_SCALE = 0.75F;
	private Float menuScaleMultiplier = DEFAULT_MENU_SCALE;
	private Float utilityMenuScaleMultiplier = 1.0F;
	private Boolean cameraMovementDuringFlight = true;
	private Float flightTurnSensitivity = 0.75F;
	private Boolean tutorialsEnabled = true;
	private List<String> tutorialsSeen = new ArrayList<>();
	private Boolean showAccumulativeDamage = true;
	private Integer rhythmAudioOffsetMs = 60;
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
	private Float overShoulderBack = 3.0F;
	private Float overShoulderUp = 0.35F;
	private Float overShoulderSide = 1.45F;
	private Float overShoulderSmoothing = 0.4F;

	public boolean migrateLegacyHud(HudLayoutConfig target, boolean targetIsNew) {
		boolean changed = false;
		if (targetIsNew && (hudStyle != null || alternativeHud != null)) {
			if (hudStyle != null) target.setStyle(HudLayoutConfig.normalizeStyle(hudStyle));
			 else target.setStyle(Boolean.TRUE.equals(alternativeHud) ? HudLayoutConfig.STYLE_VANILLA : HudLayoutConfig.STYLE_XENOVERSE);
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

	public Float getFlightTurnSensitivity() {
		if (flightTurnSensitivity == null || !Float.isFinite(flightTurnSensitivity)) flightTurnSensitivity = 0.75F;
		return Math.max(0.2F, Math.min(flightTurnSensitivity, 2.0F));
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
		if (overShoulderBack == null || !Float.isFinite(overShoulderBack)) overShoulderBack = 3.0F;
		return overShoulderBack;
	}

	public Float getOverShoulderUp() {
		if (overShoulderUp == null || !Float.isFinite(overShoulderUp)) overShoulderUp = 0.35F;
		return overShoulderUp;
	}

	public Float getOverShoulderSide() {
		if (overShoulderSide == null || !Float.isFinite(overShoulderSide)) overShoulderSide = 1.45F;
		return overShoulderSide;
	}

	public Float getOverShoulderSmoothing() {
		if (overShoulderSmoothing == null || !Float.isFinite(overShoulderSmoothing) || overShoulderSmoothing <= 0.0F) overShoulderSmoothing = 0.4F;
		return Math.min(overShoulderSmoothing, 1.0F);
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
		if (menuScaleMultiplier == null || !Float.isFinite(menuScaleMultiplier) || menuScaleMultiplier <= 0.0F) menuScaleMultiplier = DEFAULT_MENU_SCALE;
		return menuScaleMultiplier;
	}

	public Float getUtilityMenuScaleMultiplier() {
		if (utilityMenuScaleMultiplier == null || !Float.isFinite(utilityMenuScaleMultiplier) || utilityMenuScaleMultiplier <= 0.0F) utilityMenuScaleMultiplier = 1.0F;
		return utilityMenuScaleMultiplier;
	}

	public void setUtilityMenuScaleMultiplier(Float utilityMenuScaleMultiplier) {
		if (utilityMenuScaleMultiplier == null || !Float.isFinite(utilityMenuScaleMultiplier) || utilityMenuScaleMultiplier <= 0.0F) {
			this.utilityMenuScaleMultiplier = 1.0F;
			return;
		}
		this.utilityMenuScaleMultiplier = utilityMenuScaleMultiplier;
	}

	public Boolean getHideHudNumbers() {
		if (hideHudNumbers == null) hideHudNumbers = false;
		return hideHudNumbers;
	}

	public Boolean getPartyMarkers() {
		if (partyMarkers == null) partyMarkers = true;
		return partyMarkers;
	}

	public void setMenuScaleMultiplier(Float menuScaleMultiplier) {
		if (menuScaleMultiplier == null || !Float.isFinite(menuScaleMultiplier) || menuScaleMultiplier <= 0.0F) {
			this.menuScaleMultiplier = DEFAULT_MENU_SCALE;
			return;
		}
		this.menuScaleMultiplier = menuScaleMultiplier;
	}

	public String getConfigVersion() {
		return this.configVersion;
	}

	public Boolean getFirstPersonAnimated() {
		return this.firstPersonAnimated;
	}

	public boolean isImpactFramesEnabled() {
		return this.impactFramesEnabled;
	}

	public boolean isReserveFirstHotbarSlot() {
		return this.reserveFirstHotbarSlot;
	}

	public int getKiSoundVolume() {
		return this.kiSoundVolume;
	}

	public Boolean getAlwaysVisibleHudValues() {
		return this.alwaysVisibleHudValues;
	}

	public Boolean getAdvancedDescription() {
		return this.advancedDescription;
	}

	public Boolean getAdvancedDescriptionPercentage() {
		return this.advancedDescriptionPercentage;
	}

	public Boolean getHexagonStatsDisplay() {
		return this.hexagonStatsDisplay;
	}

	public Boolean getCameraMovementDuringFlight() {
		return this.cameraMovementDuringFlight;
	}

	public Integer getRhythmAudioOffsetMs() {
		return this.rhythmAudioOffsetMs;
	}

	public void setFirstPersonAnimated(final Boolean firstPersonAnimated) {
		this.firstPersonAnimated = firstPersonAnimated;
	}

	public void setImpactFramesEnabled(final boolean impactFramesEnabled) {
		this.impactFramesEnabled = impactFramesEnabled;
	}

	public void setReserveFirstHotbarSlot(final boolean reserveFirstHotbarSlot) {
		this.reserveFirstHotbarSlot = reserveFirstHotbarSlot;
	}

	public void setKiSoundVolume(final int kiSoundVolume) {
		this.kiSoundVolume = kiSoundVolume;
	}

	public void setAlwaysVisibleHudValues(final Boolean alwaysVisibleHudValues) {
		this.alwaysVisibleHudValues = alwaysVisibleHudValues;
	}

	public void setHideHudNumbers(final Boolean hideHudNumbers) {
		this.hideHudNumbers = hideHudNumbers;
	}

	public void setPartyMarkers(final Boolean partyMarkers) {
		this.partyMarkers = partyMarkers;
	}

	public void setAdvancedDescription(final Boolean advancedDescription) {
		this.advancedDescription = advancedDescription;
	}

	public void setAdvancedDescriptionPercentage(final Boolean advancedDescriptionPercentage) {
		this.advancedDescriptionPercentage = advancedDescriptionPercentage;
	}

	public void setHexagonStatsDisplay(final Boolean hexagonStatsDisplay) {
		this.hexagonStatsDisplay = hexagonStatsDisplay;
	}

	public void setCameraMovementDuringFlight(final Boolean cameraMovementDuringFlight) {
		this.cameraMovementDuringFlight = cameraMovementDuringFlight;
	}

	public void setFlightTurnSensitivity(final Float flightTurnSensitivity) {
		this.flightTurnSensitivity = flightTurnSensitivity;
	}

	public void setTutorialsEnabled(final Boolean tutorialsEnabled) {
		this.tutorialsEnabled = tutorialsEnabled;
	}

	public void setTutorialsSeen(final List<String> tutorialsSeen) {
		this.tutorialsSeen = tutorialsSeen;
	}

	public void setShowAccumulativeDamage(final Boolean showAccumulativeDamage) {
		this.showAccumulativeDamage = showAccumulativeDamage;
	}

	public void setRhythmAudioOffsetMs(final Integer rhythmAudioOffsetMs) {
		this.rhythmAudioOffsetMs = rhythmAudioOffsetMs;
	}

	public void setTaiyokenInvertPalette(final Boolean taiyokenInvertPalette) {
		this.taiyokenInvertPalette = taiyokenInvertPalette;
	}

	public void setTransformationOutlines(final Boolean transformationOutlines) {
		this.transformationOutlines = transformationOutlines;
	}

	public void setAura3DPersonal(final Boolean aura3DPersonal) {
		this.aura3DPersonal = aura3DPersonal;
	}

	public void setAura3DEntities(final Boolean aura3DEntities) {
		this.aura3DEntities = aura3DEntities;
	}

	public void setAura3DStyle(final String aura3DStyle) {
		this.aura3DStyle = aura3DStyle;
	}

	public void setHairPhysicsQuality(final String hairPhysicsQuality) {
		this.hairPhysicsQuality = hairPhysicsQuality;
	}

	public void setOverShoulderMode(final Integer overShoulderMode) {
		this.overShoulderMode = overShoulderMode;
	}

	public void setOverShoulderLeft(final Boolean overShoulderLeft) {
		this.overShoulderLeft = overShoulderLeft;
	}

	public void setOverShoulderBack(final Float overShoulderBack) {
		this.overShoulderBack = overShoulderBack;
	}

	public void setOverShoulderUp(final Float overShoulderUp) {
		this.overShoulderUp = overShoulderUp;
	}

	public void setOverShoulderSide(final Float overShoulderSide) {
		this.overShoulderSide = overShoulderSide;
	}

	public void setOverShoulderSmoothing(final Float overShoulderSmoothing) {
		this.overShoulderSmoothing = overShoulderSmoothing;
	}

	public void setConfigVersion(final String configVersion) {
		this.configVersion = configVersion;
	}
}
