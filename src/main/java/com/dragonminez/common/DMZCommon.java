package com.dragonminez.common;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import software.bernie.geckolib.GeckoLib;

public class DMZCommon {

    public static void init() {
        ConfigManager.initialize();
        NetworkHandler.register();
        GeckoLib.initialize();
    }
}

