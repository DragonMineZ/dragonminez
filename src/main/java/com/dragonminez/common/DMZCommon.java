package com.dragonminez.common;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.*;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.SagaManager;
import com.dragonminez.common.world.structure.placement.MainStructurePlacements;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;

public class DMZCommon {

    public static void init() {
        ConfigManager.initialize();
        SagaManager.init();
        NetworkHandler.register();
        GeckoLib.initialize();

		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		MainAttributes.ATTRIBUTES.register(modEventBus);
		MainBlocks.register(modEventBus);
		MainBlockEntities.register(modEventBus);
		MainItems.register(modEventBus);
		MainFluids.register(modEventBus);
		MainSounds.register(modEventBus);
		MainTabs.register(modEventBus);
        MainEntities.register(modEventBus);
        MainParticles.register(modEventBus);
        // Solo registramos el placement personalizado, no las estructuras
		MainStructurePlacements.register(modEventBus);
    }
}

