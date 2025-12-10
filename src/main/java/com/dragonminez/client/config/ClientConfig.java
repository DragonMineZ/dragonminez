package com.dragonminez.client.config;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralUserConfig;

public class ClientConfig {

    public static GeneralUserConfig get() {
        return ConfigManager.getUserConfig();
    }

    public static GeneralUserConfig.HudConfig hud() {
        return get().getHud();
    }

    public static void save() {
        ConfigManager.saveGeneralUserConfig();
    }

    public static boolean isCompactHud() {
        return hud().isAlternativeHud();
    }

    public static void setCompactHud(boolean compact) {
        hud().setAlternativeHud(compact);
        save();
    }
}

