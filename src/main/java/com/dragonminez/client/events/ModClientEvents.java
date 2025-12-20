package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.XenoverseHUD;
import com.dragonminez.client.init.blocks.renderer.DragonBallBlockRenderer;
import com.dragonminez.client.init.entities.model.ki.KiBallPlaneModel;
import com.dragonminez.client.init.entities.renderer.*;
import com.dragonminez.client.init.entities.renderer.ki.KiProjectileRenderer;
import com.dragonminez.client.init.entities.renderer.rr.RedRibbonRenderer;
import com.dragonminez.client.init.entities.renderer.rr.RobotRRRenderer;
import com.dragonminez.client.init.entities.renderer.sagas.DBSagasRenderer;
import com.dragonminez.client.init.entities.renderer.sagas.SagaSaibamanRenderer;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.armor.client.model.ArmorBaseModel;
import com.dragonminez.common.init.particles.KiFlashParticle;
import com.dragonminez.common.init.particles.KiSplashParticle;
import com.dragonminez.common.init.particles.KiTrailParticle;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

	@SubscribeEvent
	public static void registerGuiOverlays(RegisterGuiOverlaysEvent e) {
		e.registerAboveAll("playerhud", XenoverseHUD.HUD_XENOVERSE);
	}

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        KeyBinds.registerAll(event);
    }

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			//Bloques
			BlockEntityRenderers.register(MainBlockEntities.DRAGON_BALL_BLOCK_ENTITY.get(), DragonBallBlockRenderer::new);

        });
	}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MainEntities.MASTER_KARIN.get(), MasterEntityRenderer::new);
        event.registerEntityRenderer(MainEntities.MASTER_GOKU.get(), MasterEntityRenderer::new);
        event.registerEntityRenderer(MainEntities.MASTER_KAIOSAMA.get(), MasterEntityRenderer::new);
        event.registerEntityRenderer(MainEntities.MASTER_ROSHI.get(), MasterEntityRenderer::new);
        event.registerEntityRenderer(MainEntities.MASTER_URANAI.get(), MasterEntityRenderer::new);
        event.registerEntityRenderer(MainEntities.MASTER_ENMA.get(), MasterEntityRenderer::new);

        event.registerEntityRenderer(MainEntities.SAGA_SAIBAMAN.get(), SagaSaibamanRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_SAIBAMAN2.get(), SagaSaibamanRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_SAIBAMAN3.get(), SagaSaibamanRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_SAIBAMAN4.get(), SagaSaibamanRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_SAIBAMAN5.get(), SagaSaibamanRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_SAIBAMAN6.get(), SagaSaibamanRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_RADITZ.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_NAPPA.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_VEGETA.get(), DBSagasRenderer::new);

        event.registerEntityRenderer(MainEntities.DINOSAUR1.get(), DinosRenderer::new);
        event.registerEntityRenderer(MainEntities.DINOSAUR2.get(), GranDinoRenderer::new);
        event.registerEntityRenderer(MainEntities.DINOSAUR3.get(), DinoFlyRenderer::new);
        event.registerEntityRenderer(MainEntities.DINO_KID.get(), DinosRenderer::new);

        event.registerEntityRenderer(MainEntities.BANDIT.get(), RedRibbonRenderer::new);
        event.registerEntityRenderer(MainEntities.RED_RIBBON_ROBOT1.get(), RobotRRRenderer::new);
        event.registerEntityRenderer(MainEntities.RED_RIBBON_ROBOT2.get(), RobotRRRenderer::new);
        event.registerEntityRenderer(MainEntities.RED_RIBBON_ROBOT3.get(), RobotRRRenderer::new);

        event.registerEntityRenderer(MainEntities.KI_BLAST.get(), KiProjectileRenderer::new);
    }

    @SubscribeEvent
    public static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(ArmorBaseModel.LAYER_LOCATION, ArmorBaseModel::createBodyLayer);
        e.registerLayerDefinition(KiBallPlaneModel.LAYER_LOCATION, KiBallPlaneModel::createBodyLayer);

    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(MainParticles.KI_FLASH.get(), KiFlashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_SPLASH.get(), KiSplashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_TRAIL.get(), KiTrailParticle.Provider::new);

    }
}
