package com.dragonminez.common;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.ModCommonEvents;
import com.dragonminez.common.init.*;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.SagaManager;
import com.dragonminez.common.wish.WishManager;
import com.dragonminez.server.world.feature.OverworldFeatures;
import com.dragonminez.server.world.structure.placement.MainStructurePlacements;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;

public class DMZCommon {

    public static void init() {
		LogUtil.info(Env.COMMON, "Initializing DragonMineZ Common...");
        ConfigManager.initialize();
        SagaManager.init();
		WishManager.init();
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
		MainRecipes.register(modEventBus);
		MainMenus.register(modEventBus);
        MainEffects.register(modEventBus);
        MainStructurePlacements.register(modEventBus);
		modEventBus.addListener(ModCommonEvents::commonSetup);
		OverworldFeatures.register(modEventBus);

		MainGameRules.register();
		MainDamageTypes.register();

    }
}
