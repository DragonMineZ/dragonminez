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
        private boolean showStats = true;
        private boolean showEnergyBar = true;
        private boolean showStaminaBar = true;
        private int energyBarPosX = 10;
        private int energyBarPosY = 10;
        private int staminaBarPosX = 10;
        private int staminaBarPosY = 30;
        private boolean compactHud = false;

        public boolean isShowStats() { return showStats; }
        public void setShowStats(boolean showStats) { this.showStats = showStats; }

        public boolean isShowEnergyBar() { return showEnergyBar; }
        public void setShowEnergyBar(boolean showEnergyBar) { this.showEnergyBar = showEnergyBar; }

        public boolean isShowStaminaBar() { return showStaminaBar; }
        public void setShowStaminaBar(boolean showStaminaBar) { this.showStaminaBar = showStaminaBar; }

        public int getEnergyBarPosX() { return energyBarPosX; }
        public void setEnergyBarPosX(int energyBarPosX) { this.energyBarPosX = energyBarPosX; }

        public int getEnergyBarPosY() { return energyBarPosY; }
        public void setEnergyBarPosY(int energyBarPosY) { this.energyBarPosY = energyBarPosY; }

        public int getStaminaBarPosX() { return staminaBarPosX; }
        public void setStaminaBarPosX(int staminaBarPosX) { this.staminaBarPosX = staminaBarPosX; }

        public int getStaminaBarPosY() { return staminaBarPosY; }
        public void setStaminaBarPosY(int staminaBarPosY) { this.staminaBarPosY = staminaBarPosY; }

        public boolean isCompactHud() { return compactHud; }
        public void setCompactHud(boolean compactHud) { this.compactHud = compactHud; }
    }
}

