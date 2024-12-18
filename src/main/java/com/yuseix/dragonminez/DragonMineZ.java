package com.yuseix.dragonminez;

import com.yuseix.dragonminez.config.DMZGeneralConfig;
import com.yuseix.dragonminez.config.races.*;
import com.yuseix.dragonminez.config.races.transformations.*;
import com.yuseix.dragonminez.events.ForgeBusEvents;
import com.yuseix.dragonminez.events.ModBusEvents;
import com.yuseix.dragonminez.events.StorylineEvents;
import com.yuseix.dragonminez.init.*;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.recipes.DMZRecipes;
import com.yuseix.dragonminez.stats.DMZGenericAttributes;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.utils.GenAttRegistry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;

@Mod(DragonMineZ.MOD_ID)
public class DragonMineZ {

	public static final String MOD_ID = "dragonminez";

	public DragonMineZ() {

		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

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
		//Registramos los Fluidos (Tipo de Fluido y Fluido/s)
		MainFluids.register(modEventBus);
		//Register del commonSetup para las Flores y FlowerPots
		modEventBus.addListener(this::commonSetup);
		//Register Menús
		MainMenus.register(modEventBus);
		//Register Recipes
		DMZRecipes.register(modEventBus);
		//Register Particulas
		MainParticles.register(modEventBus);

		//        MinecraftForge.EVENT_BUS.register(ClientModBusEvents.class);

		MinecraftForge.EVENT_BUS.register(this);

		//Registramos el Listener del Mod (Normalmente eventos de Forge y FML más como frontend, realmente son los eventos de renderizado y más cosas de cliente)
		modEventBus.register(new ModBusEvents());
		//Registramos el Listener de Forge (Eventos de Forge que van más allá del juego como backend, conocido como ModEvents)
		MinecraftForge.EVENT_BUS.register(new ForgeBusEvents());
		//Registramos el Listener de Forge para la Storyline
		MinecraftForge.EVENT_BUS.register(new StorylineEvents());
		//Se registran los eventos de las Capabilities de las Stats
		MinecraftForge.EVENT_BUS.register(new DMZStatsCapabilities());

		MinecraftForge.EVENT_BUS.register(GenAttRegistry.class);
		MinecraftForge.EVENT_BUS.register(DMZGenericAttributes.class);

		GeckoLib.initialize();

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

		// Añade una advertencia al cargar el mod si el usuario no está en la lista de usuarios permitidos para testear el mod.
        /*
        IModInfo modInfo = ModLoadingContext.get().getActiveContainer().getModInfo();
        ModLoadingWarning modLoadingWarning = new ModLoadingWarning(modInfo, ModLoadingStage.CONSTRUCT,
                """
                        DragonMineZ:
                        Only the official DMZ development team is allowed to play the mod.
                        If you are not a member of this team, the mod will cause an error.

                        This mod is in a development phase! Expect bugs, errors and crashes!

                        Proceed with caution!""");
        ModLoader.get().addWarning(modLoadingWarning);

         */
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

			ModMessages.register();

		});
	}
}