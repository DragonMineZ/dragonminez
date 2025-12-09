package com.dragonminez.common.config;

public class GeneralUserConfig {

    private HudConfig hud = new HudConfig();

    public HudConfig getHud() {
        return hud;
    }

    public void setHud(HudConfig hud) {
        this.hud = hud;
    }

    public static class HudConfig {
		private int xenoverseHudPosX = 5;
		private int xenoverseHudPosY = 5;
		private float xenoverseHudScale = 1.4f;
		private boolean compactHud = false;
        private int healthBarPosX = 10;
        private int healthBarPosY = 50;
        private int energyBarPosX = 10;
        private int energyBarPosY = 10;
        private int staminaBarPosX = 10;
        private int staminaBarPosY = 30;

		public int getXenoverseHudPosX() { return xenoverseHudPosX; }
		public void setXenoverseHudPosX(int xenoverseHudPosX) { this.xenoverseHudPosX = xenoverseHudPosX; }
		public int getXenoverseHudPosY() { return xenoverseHudPosY; }
		public void setXenoverseHudPosY(int xenoverseHudPosY) { this.xenoverseHudPosY = xenoverseHudPosY; }
		public float getXenoverseHudScale() { return xenoverseHudScale; }
		public void setXenoverseHudScale(float xenoverseHudScale) { this.xenoverseHudScale = xenoverseHudScale; }

		public boolean isCompactHud() { return compactHud; }
		public void setCompactHud(boolean compactHud) { this.compactHud = compactHud; }

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
    }
}

