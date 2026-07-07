package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class GeneralUserConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	@Setter
	private String configVersion;

	private Boolean firstPersonAnimated = true;
	private boolean impactFramesEnabled = false;
	private Boolean techniqueHotbarRightSide = false;
	private Boolean alwaysVisibleHudValues = false;
	private Boolean hideHudNumbers = false;
	private Integer xenoverseHudPosX = 5;
	private Integer xenoverseHudPosY = 5;
	private Float xenoverseHudScale = 1.0f;
	private Boolean advancedDescription = true;
	private Boolean advancedDescriptionPercentage = true;
	private Boolean alternativeHud = false;
	private Boolean hexagonStatsDisplay = false;
	private Float menuScaleMultiplier = 1.0f;
	private Float utilityMenuScaleMultiplier = 1.0f;
	private Integer healthBarPosX = 10;
	private Integer healthBarPosY = 20;
	private Integer energyBarPosX = 10;
	private Integer energyBarPosY = 10;
	private Integer staminaBarPosX = 10;
	private Integer staminaBarPosY = 10;
	private Boolean cameraMovementDuringFlight = true;
	private Boolean liveCrowdinTranslations = true;
	private Boolean showAccumulativeDamage = true;
	private Boolean taiyokenInvertPalette = false;

	private Integer overShoulderMode = 2;
	private Boolean overShoulderLeft = false;
	private Float overShoulderBack = 3.0f;
	private Float overShoulderUp = 0.35f;
	private Float overShoulderSide = 1.45f;
	private Float overShoulderSmoothing = 0.4f;

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

	public Float getXenoverseHudScale() {
		if (xenoverseHudScale == null || !Float.isFinite(xenoverseHudScale) || xenoverseHudScale <= 0.0f) xenoverseHudScale = 1.0f;
		return xenoverseHudScale;
	}

	public void setXenoverseHudScale(Float xenoverseHudScale) {
		if (xenoverseHudScale == null || !Float.isFinite(xenoverseHudScale) || xenoverseHudScale <= 0.0f) {
			this.xenoverseHudScale = 1.0f;
			return;
		}
		this.xenoverseHudScale = xenoverseHudScale;
	}

	public Boolean getTechniqueHotbarRightSide() {
		if (techniqueHotbarRightSide == null) techniqueHotbarRightSide = false;
		return techniqueHotbarRightSide;
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

