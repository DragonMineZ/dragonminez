package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
public class GeneralUserConfig {
	public static final int CURRENT_VERSION = 2;

	@Setter
	private int configVersion;

    private HudConfig hud = new HudConfig();

	@Setter
	@Getter
	@NoArgsConstructor
	public static class HudConfig {
        private Boolean firstPersonAnimated = true;
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
        private Boolean storyHardDifficulty = false;
        private Boolean cameraMovementDuringFlight = true;

        public Float getMenuScaleMultiplier() {
			if (!Float.isFinite(menuScaleMultiplier) || menuScaleMultiplier <= 0.0f) menuScaleMultiplier = 1.0f;
			return menuScaleMultiplier;
		}

		public void setMenuScaleMultiplier(Float menuScaleMultiplier) {
			if (!Float.isFinite(menuScaleMultiplier) || menuScaleMultiplier <= 0.0f) {
				this.menuScaleMultiplier = 1.0f;
				return;
			}
			this.menuScaleMultiplier = menuScaleMultiplier;
		}
    }
}

