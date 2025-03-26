package com.yuseix.dragonminez;

import com.yuseix.dragonminez.common.Reference;
import com.yuseix.dragonminez.common.config.old.DMZGeneralConfig;
import com.yuseix.dragonminez.common.config.old.races.*;
import com.yuseix.dragonminez.common.config.old.races.transformations.*;
import com.yuseix.dragonminez.common.events.ForgeBusEvents;
import com.yuseix.dragonminez.common.events.ModBusEvents;
import com.yuseix.dragonminez.common.events.StoryEvents;
import com.yuseix.dragonminez.common.init.*;
import com.yuseix.dragonminez.common.network.ModMessages;
import com.yuseix.dragonminez.server.recipes.DMZRecipes;
import com.yuseix.dragonminez.common.stats.DMZGenericAttributes;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.storymode.DMZStoryRegistry;
import com.yuseix.dragonminez.common.util.GenAttRegistry;
import com.yuseix.dragonminez.server.worldgen.biome.ModOverworldRegion;
import com.yuseix.dragonminez.server.worldgen.biome.ModSurfaceRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

/*
 * This file uses GeckoLib, licensed under the MIT License.
 * Copyright © 2024 GeckoThePecko.
 */
@Mod(Reference.MOD_ID)
public class DragonMineZ {

	public DragonMineZ() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		GeckoLib.initialize();

		//Registramos Items
		MainItems.register(modEventBus);
		//Registramos Bloques
		MainBlocks.register(modEventBus);
		//Registramos la nueva TAB del Creativo
		MainTabs.register(modEventBus);
		//Registramos las entidades de los bloques
		MainBlockEntities.register(modEventBus);
		//Registramos los sonidos
		MainSounds.register(modEventBus);
		//Registramos las entidades
		MainEntity.register(modEventBus);
		//Registramos los Fluidos
		MainFluids.register(modEventBus);
		//Register del commonSetup para las Flores y FlowerPots + Packets
		modEventBus.addListener(this::commonSetup);
		//Register Menús
		MainMenus.register(modEventBus);
		//Register Recipes
		DMZRecipes.register(modEventBus);
		//Register Particulas
		MainParticles.register(modEventBus);
		//Register biomas de Terrablender
		Regions.register(new ModOverworldRegion());

		MinecraftForge.EVENT_BUS.register(this);

		//Registramos el Listener del Mod
		modEventBus.register(new ModBusEvents());
		//Registramos el Listener de Forge
		MinecraftForge.EVENT_BUS.register(new ForgeBusEvents());
		//Registramos el Listener de Forge para la Storyline
		//Se registran los eventos de las Capabilities de las Stats
		MinecraftForge.EVENT_BUS.register(new DMZStatsCapabilities());
		MinecraftForge.EVENT_BUS.register(new StoryEvents());

		MinecraftForge.EVENT_BUS.register(GenAttRegistry.class);
		MinecraftForge.EVENT_BUS.register(DMZGenericAttributes.class);

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMZGeneralConfig.SPEC, "dragonminez/dragonminez-general.toml");

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMZHumanConfig.SPEC, "dragonminez/races/human/classes-config.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMZSaiyanConfig.SPEC, "dragonminez/races/saiyan/classes-config.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMZNamekConfig.SPEC, "dragonminez/races/namek/classes-config.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMZBioAndroidConfig.SPEC, "dragonminez/races/bioandroid/classes-config.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMZColdDemonConfig.SPEC, "dragonminez/races/colddemon/classes-config.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMZMajinConfig.SPEC, "dragonminez/races/majin/classes-config.toml");

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMZTrHumanConfig.SPEC, "dragonminez/races/human/transformation-config.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMZTrSaiyanConfig.SPEC, "dragonminez/races/saiyan/transformation-config.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMZTrNamekConfig.SPEC, "dragonminez/races/namek/transformation-config.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMZTrBioAndroidConfig.SPEC, "dragonminez/races/bioandroid/transformation-config.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMZTrColdDemonConfig.SPEC, "dragonminez/races/colddemon/transformation-config.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMZTrMajinConfig.SPEC, "dragonminez/races/majin/transformation-config.toml");

	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {

			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.CHRYSANTHEMUM_FLOWER.getId(), MainBlocks.POTTED_CHRYSANTHEMUM_FLOWER);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.AMARYLLIS_FLOWER.getId(), MainBlocks.POTTED_AMARYLLIS_FLOWER);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.MARIGOLD_FLOWER.getId(), MainBlocks.POTTED_MARIGOLD_FLOWER);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.CATHARANTHUS_ROSEUS_FLOWER.getId(), MainBlocks.POTTED_CATHARANTHUS_ROSEUS_FLOWER);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.TRILLIUM_FLOWER.getId(), MainBlocks.POTTED_TRILLIUM_FLOWER);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.NAMEK_FERN.getId(), MainBlocks.POTTED_NAMEK_FERN);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.SACRED_CHRYSANTHEMUM_FLOWER.getId(), MainBlocks.POTTED_SACRED_CHRYSANTHEMUM_FLOWER);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.SACRED_AMARYLLIS_FLOWER.getId(), MainBlocks.POTTED_SACRED_AMARYLLIS_FLOWER);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.SACRED_MARIGOLD_FLOWER.getId(), MainBlocks.POTTED_SACRED_MARIGOLD_FLOWER);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.SACRED_CATHARANTHUS_ROSEUS_FLOWER.getId(), MainBlocks.POTTED_SACRED_CATHARANTHUS_ROSEUS_FLOWER);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.SACRED_TRILLIUM_FLOWER.getId(), MainBlocks.POTTED_SACRED_TRILLIUM_FLOWER);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.SACRED_FERN.getId(), MainBlocks.POTTED_SACRED_FERN);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.NAMEK_AJISSA_SAPLING.getId(), MainBlocks.POTTED_AJISSA_SAPLING);
			((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(MainBlocks.NAMEK_SACRED_SAPLING.getId(), MainBlocks.POTTED_SACRED_SAPLING);

			SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, Reference.MOD_ID, ModSurfaceRules.makeRules());

			ModMessages.register();
			DMZStoryRegistry.registerAll();

		});
	}
}