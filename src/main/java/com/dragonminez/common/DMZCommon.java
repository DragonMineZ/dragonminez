package com.dragonminez.common;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.*;
import com.dragonminez.common.network.NetworkHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;

public class DMZCommon {

    public static void init() {
        ConfigManager.initialize();
        NetworkHandler.register();
        GeckoLib.initialize();

		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		MainBlocks.register(modEventBus);
		MainBlockEntities.register(modEventBus);
		MainItems.register(modEventBus);
		MainFluids.register(modEventBus);
		MainSounds.register(modEventBus);
		MainTabs.register(modEventBus);
        MainEntities.register(modEventBus);
    }
}

