package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class GeneralUserConfig {
	public static final int CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	@Setter
	private int configVersion;

	private Boolean firstPersonAnimated = true;
	private boolean impactFramesEnabled = false;
	private Boolean techniqueHotbarRightSide = false;
	private Boolean alwaysVisibleHudValues = false;
	private Boolean hideHudNumbers = false;
	private Integer xenoverseHudPosX = 5;
	private Integer xenoverseHudPosY = 5;
	private Boolean advancedDescription = true;
	private Boolean advancedDescriptionPercentage = true;
	private Boolean alternativeHud = false;
	private Boolean hexagonStatsDisplay = false;
	private Float menuScaleMultiplier = 1.0f;
	private Integer healthBarPosX = 10;
	private Integer healthBarPosY = 20;
	private Integer energyBarPosX = 10;
	private Integer energyBarPosY = 10;
	private Integer staminaBarPosX = 10;
	private Integer staminaBarPosY = 10;
	private Boolean cameraMovementDuringFlight = true;
	private Boolean liveCrowdinTranslations = true;
	private Boolean showAccumulativeDamage = true;

	public Boolean getShowAccumulativeDamage() {
		if (showAccumulativeDamage == null) showAccumulativeDamage = true;
		return showAccumulativeDamage;
	}

	public Float getMenuScaleMultiplier() {
		if (!Float.isFinite(menuScaleMultiplier) || menuScaleMultiplier <= 0.0f) menuScaleMultiplier = 1.0f;
		return menuScaleMultiplier;
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

