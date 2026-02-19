package com.dragonminez.common.config;

public class GeneralUserConfig {

    private HudConfig hud = new HudConfig();

    public HudConfig getHud() { return hud; }

	public static class HudConfig {
		private boolean firstPersonAnimated = true;
		private int xenoverseHudPosX = 5;
		private int xenoverseHudPosY = 5;
		private boolean advancedDescription = true;
		private boolean advancedDescriptionPercentage = true;
		private boolean alternativeHud = false;
		private boolean hexagonStatsDisplay = false;
		private float menuScaleMultiplier = 1.0f;
        private int healthBarPosX = 10;
        private int healthBarPosY = 20;
        private int energyBarPosX = 10;
        private int energyBarPosY = 10;
        private int staminaBarPosX = 10;
        private int staminaBarPosY = 10;
        private boolean storyHardDifficulty = false;
		private boolean cameraMovementDurintFlight = true;

		public boolean isFirstPersonAnimated() { return firstPersonAnimated; }
		public void setFirstPersonAnimated(boolean firstPersonAnimated) { this.firstPersonAnimated = firstPersonAnimated; }
		public int getXenoverseHudPosX() { return xenoverseHudPosX; }
		public void setXenoverseHudPosX(int xenoverseHudPosX) { this.xenoverseHudPosX = xenoverseHudPosX; }
		public int getXenoverseHudPosY() { return xenoverseHudPosY; }
		public void setXenoverseHudPosY(int xenoverseHudPosY) { this.xenoverseHudPosY = xenoverseHudPosY; }
		public boolean isAdvancedDescription() { return advancedDescription; }
		public void setAdvancedDescription(boolean advancedDescription) { this.advancedDescription = advancedDescription; }
		public boolean isAdvancedDescriptionPercentage() { return advancedDescriptionPercentage; }
		public void setAdvancedDescriptionPercentage(boolean advancedDescriptionPercentage) { this.advancedDescriptionPercentage = advancedDescriptionPercentage; }

		public boolean isAlternativeHud() { return alternativeHud; }
		public void setAlternativeHud(boolean alternativeHud) { this.alternativeHud = alternativeHud; }

		public boolean isHexagonStatsDisplay() { return hexagonStatsDisplay; }
		public void setHexagonStatsDisplay(boolean hexagonStatsDisplay) { this.hexagonStatsDisplay = hexagonStatsDisplay; }

		public float getMenuScaleMultiplier() {
			if (!Float.isFinite(menuScaleMultiplier) || menuScaleMultiplier <= 0.0f) menuScaleMultiplier = 1.0f;
			return menuScaleMultiplier;
		}

		public void setMenuScaleMultiplier(float menuScaleMultiplier) {
			if (!Float.isFinite(menuScaleMultiplier) || menuScaleMultiplier <= 0.0f) {
				this.menuScaleMultiplier = 1.0f;
				return;
			}
			this.menuScaleMultiplier = menuScaleMultiplier;
		}

        public int getHealthBarPosX() { return healthBarPosX; }
        public void setHealthBarPosX(int healthBarPosX) { this.healthBarPosX = healthBarPosX; }

		public int getHealthBarPosY() { return healthBarPosY; }
		public void setHealthBarPosY(int healthBarPosY) { this.healthBarPosY = healthBarPosY; }

        public int getEnergyBarPosX() { return energyBarPosX; }
        public void setEnergyBarPosX(int energyBarPosX) { this.energyBarPosX = energyBarPosX; }

        public int getEnergyBarPosY() { return energyBarPosY; }
        public void setEnergyBarPosY(int energyBarPosY) { this.energyBarPosY = energyBarPosY; }

        public int getStaminaBarPosX() { return staminaBarPosX; }
        public void setStaminaBarPosX(int staminaBarPosX) { this.staminaBarPosX = staminaBarPosX; }

        public int getStaminaBarPosY() { return staminaBarPosY; }
        public void setStaminaBarPosY(int staminaBarPosY) { this.staminaBarPosY = staminaBarPosY; }

        public boolean isStoryHardDifficulty() { return storyHardDifficulty; }
        public void setStoryHardDifficulty(boolean storyHardDifficulty) { this.storyHardDifficulty = storyHardDifficulty; }

		public boolean isCameraMovementDurintFlight() { return cameraMovementDurintFlight; }
		public void setCameraMovementDurintFlight(boolean cameraMovementDurintFlight) { this.cameraMovementDurintFlight = cameraMovementDurintFlight; }
	}
}

