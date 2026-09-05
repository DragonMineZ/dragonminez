package com.dragonminez.common;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.ModCommonEvents;
import com.dragonminez.common.init.*;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.racial.RacialRegistry;
import com.dragonminez.common.racial.impl.BioAndroidEvolution;
import com.dragonminez.common.racial.impl.FrostDemonReserve;
import com.dragonminez.common.racial.impl.HumanAdaptation;
import com.dragonminez.common.racial.impl.MajinAbsorption;
import com.dragonminez.common.racial.impl.NamekAssimilation;
import com.dragonminez.common.racial.impl.SaiyanZenkai;
import com.dragonminez.common.wish.WishManager;
import com.dragonminez.server.world.feature.OverworldFeatures;
import com.dragonminez.server.world.feature.SacredKaiFeatures;
import com.dragonminez.server.world.structure.placement.MainStructurePlacements;
import com.dragonminez.server.world.structure.processor.MainStructureProcessors;
import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;

public class DMZCommon {

    public static void init() {
		LogUtil.info(Env.COMMON, "Initializing DragonMineZ Common...");
		registerRacialAbilities();
        ConfigManager.initialize();
        QuestRegistry.init();
		WishManager.init();
        NetworkHandler.register();
        GeckoLib.initialize();

		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		MainAttributes.ATTRIBUTES.register(modEventBus);
		EntityAttributes.ATTRIBUTES.register(modEventBus);
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
		MainPotions.register(modEventBus);
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

	private static void registerRacialAbilities() {
		RacialRegistry.register(new SaiyanZenkai());
		RacialRegistry.register(new NamekAssimilation());
		RacialRegistry.register(new MajinAbsorption());
		RacialRegistry.register(new HumanAdaptation());
		RacialRegistry.register(new FrostDemonReserve());
		RacialRegistry.register(new BioAndroidEvolution());
	}
}
