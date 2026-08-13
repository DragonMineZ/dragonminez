package com.dragonminez.common;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.ModCommonEvents;
import com.dragonminez.common.init.*;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.wish.WishManager;
import com.dragonminez.server.world.feature.OverworldFeatures;
import com.dragonminez.server.world.feature.SacredKaiFeatures;
import com.dragonminez.server.world.structure.placement.MainStructurePlacements;
import com.dragonminez.server.world.structure.processor.MainStructureProcessors;
import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import net.neoforged.bus.api.IEventBus;

public class DMZCommon {

    public static void init(IEventBus modEventBus) {
		LogUtil.info(Env.COMMON, "Initializing DragonMineZ Common...");
        ConfigManager.initialize();
        QuestRegistry.init();
		WishManager.init();
        NetworkHandler.register();
        StatsCapability.register(modEventBus);
        // GeckoLib auto-initializes on NeoForge; no GeckoLib.initialize() call.

		MainAttributes.ATTRIBUTES.register(modEventBus);
		EntityAttributes.ATTRIBUTES.register(modEventBus);
		com.dragonminez.common.init.armor.ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
		MainBlocks.register(modEventBus);
		MainBlockEntities.register(modEventBus);
		MainItems.register(modEventBus);
		MainFluids.register(modEventBus);
		MainSounds.register(modEventBus);
		MainTabs.register(modEventBus);
        MainEntities.register(modEventBus);
        MainVillagers.register(modEventBus);
        MainParticles.register(modEventBus);
		MainRecipes.register(modEventBus);
		MainMenus.register(modEventBus);
        MainEffects.register(modEventBus);
		MainEnchants.register(modEventBus);
		MainLootModifiers.register(modEventBus);
        MainStructurePlacements.register(modEventBus);
		MainStructureProcessors.register(modEventBus);
		MainStructureTypes.register(modEventBus);
		modEventBus.addListener(ModCommonEvents::commonSetup);
		OverworldFeatures.register(modEventBus);
		SacredKaiFeatures.register(modEventBus);

		MainGameRules.register();
		MainDamageTypes.register();

    }
}
