package com.yuseix.dragonminez;

import com.mojang.logging.LogUtils;
import com.yuseix.dragonminez.character.FaceModel;
import com.yuseix.dragonminez.character.models.ModeloPrueba;
import com.yuseix.dragonminez.character.renders.RenderPrueba;
import com.yuseix.dragonminez.config.DMCAttrConfig;
import com.yuseix.dragonminez.init.*;
import com.yuseix.dragonminez.init.blocks.entity.MainBlockEntities;
import com.yuseix.dragonminez.init.blocks.entity.client.*;
import com.yuseix.dragonminez.init.entity.client.renderer.DinoRenderer;
import com.yuseix.dragonminez.network.ModMessages;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

@Mod(DragonMineZ.MOD_ID)
public class DragonMineZ {

    public static final String MOD_ID = "dragonminez";

    private static final Logger LOGGER = LogUtils.getLogger();

    public DragonMineZ() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

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

        MinecraftForge.EVENT_BUS.register(this);

        GeckoLib.initialize();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMCAttrConfig.SPEC, "dragonminez-common.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {

            SpawnPlacements.register(MainEntity.DINO1.get(),
                    SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Animal::checkAnimalSpawnRules);

            ModMessages.register();
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Hacer algo cuando el servidor empiece???
        LOGGER.info("HOLA SOY DRAGON BLOCK TEST UWU");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            //ENTIDADES
            EntityRenderers.register(MainEntity.DINO1.get(), DinoRenderer::new);

            //BLOQUES
            BlockEntityRenderers.register(MainBlockEntities.DBALL1_BLOCK_ENTITY.get(), Dball1BlockRenderer::new);
            BlockEntityRenderers.register(MainBlockEntities.DBALL2_BLOCK_ENTITY.get(), Dball2BlockRenderer::new);
            BlockEntityRenderers.register(MainBlockEntities.DBALL3_BLOCK_ENTITY.get(), Dball3BlockRenderer::new);
            BlockEntityRenderers.register(MainBlockEntities.DBALL4_BLOCK_ENTITY.get(), Dball4BlockRenderer::new);
            BlockEntityRenderers.register(MainBlockEntities.DBALL5_BLOCK_ENTITY.get(), Dball5BlockRenderer::new);
            BlockEntityRenderers.register(MainBlockEntities.DBALL6_BLOCK_ENTITY.get(), Dball6BlockRenderer::new);
            BlockEntityRenderers.register(MainBlockEntities.DBALL7_BLOCK_ENTITY.get(), Dball7BlockRenderer::new);

        }


            @SubscribeEvent
        public static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
            e.registerLayerDefinition(FaceModel.LAYER_LOCATION, FaceModel::createBodyLayer);

            e.registerLayerDefinition(ModeloPrueba.LAYER_LOCATION, ModeloPrueba::createBodyLayer);
        }


    }
}
