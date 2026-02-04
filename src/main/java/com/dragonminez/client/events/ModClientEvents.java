package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.crowdin.CrowdinManager;
import com.dragonminez.client.crowdin.CrowdinPackResources;
import com.dragonminez.client.gui.hud.AlternativeHUD;
import com.dragonminez.client.gui.hud.XenoverseHUD;
import com.dragonminez.client.init.blocks.renderer.DragonBallBlockRenderer;
import com.dragonminez.client.init.blocks.renderer.EnergyCableBlockRenderer;
import com.dragonminez.client.init.blocks.renderer.FuelGeneratorBlockRenderer;
import com.dragonminez.client.init.blocks.renderer.KikonoStationBlockRenderer;
import com.dragonminez.client.init.entities.model.ki.KiBallPlaneModel;
import com.dragonminez.client.init.entities.model.ki.KiLaserExplosion2Model;
import com.dragonminez.client.init.entities.model.ki.KiLaserExplosionModel;
import com.dragonminez.client.init.entities.model.ki.KiLaserModel;
import com.dragonminez.client.init.entities.renderer.*;
import com.dragonminez.client.init.entities.renderer.ki.*;
import com.dragonminez.client.init.entities.renderer.rr.RedRibbonRenderer;
import com.dragonminez.client.init.entities.renderer.rr.RedRibbonSoldierRenderer;
import com.dragonminez.client.init.entities.renderer.rr.RobotRRRenderer;
import com.dragonminez.client.init.entities.renderer.sagas.*;
import com.dragonminez.client.model.KiBladeModel;
import com.dragonminez.client.model.KiScytheModel;
import com.dragonminez.client.model.KiTridentModel;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.init.*;
import com.dragonminez.common.init.armor.client.model.ArmorBaseModel;
import com.dragonminez.client.init.menu.screens.FuelGeneratorScreen;
import com.dragonminez.client.init.menu.screens.KikonoStationScreen;
import com.dragonminez.common.init.particles.*;
import com.dragonminez.server.world.dimension.CustomSpecialEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

	@SubscribeEvent
	public static void registerGuiOverlays(RegisterGuiOverlaysEvent e) {
		e.registerAboveAll("xenoversehud", XenoverseHUD.HUD_XENOVERSE);
		e.registerAboveAll("alternativehud", AlternativeHUD.HUD_ALTERNATIVE);
	}

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        KeyBinds.registerAll(event);
    }

	@SubscribeEvent
	public static void onAddPackFinders(AddPackFindersEvent event) {
		if (event.getPackType() == PackType.CLIENT_RESOURCES) {
			String currentLang = Minecraft.getInstance().options.languageCode;
			CrowdinManager.fetchLanguage(currentLang);

			event.addRepositorySource((packConsumer) -> {
				Pack crowdinPack = Pack.readMetaAndCreate("dmz_crowdin_ota", Component.literal("DMZ Live Translations"), true,
						CrowdinPackResources::new, PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);

				if (crowdinPack != null) packConsumer.accept(crowdinPack);
			});
		}
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			//Bloques
			BlockEntityRenderers.register(MainBlockEntities.DRAGON_BALL_BLOCK_ENTITY.get(), DragonBallBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.ENERGY_CABLE_BE.get(), EnergyCableBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.KIKONO_STATION_BE.get(), KikonoStationBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.FUEL_GENERATOR_BE.get(), FuelGeneratorBlockRenderer::new);
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.NAMEK_AJISSA_LOG.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.NAMEK_SACRED_LOG.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.INVISIBLE_LADDER_BLOCK.get(), RenderType.translucent());

			//MENÚS
			MenuScreens.register(MainMenus.KIKONO_STATION_MENU.get(), KikonoStationScreen::new);
			MenuScreens.register(MainMenus.FUEL_GENERATOR_MENU.get(), FuelGeneratorScreen::new);

			// Fluids
			ItemBlockRenderTypes.setRenderLayer(MainFluids.SOURCE_NAMEK.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(MainFluids.FLOWING_NAMEK.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(MainFluids.FLOWING_HEALING.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(MainFluids.SOURCE_HEALING.get(), RenderType.translucent());

			//Vegetacion
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.CHRYSANTHEMUM_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.AMARYLLIS_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.MARIGOLD_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.CATHARANTHUS_ROSEUS_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.TRILLIUM_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.LOTUS_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.NAMEK_FERN.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.SACRED_CHRYSANTHEMUM_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.SACRED_AMARYLLIS_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.SACRED_MARIGOLD_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.SACRED_CATHARANTHUS_ROSEUS_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.SACRED_TRILLIUM_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.SACRED_FERN.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.NAMEK_AJISSA_SAPLING.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.NAMEK_SACRED_SAPLING.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_CHRYSANTHEMUM_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_AMARYLLIS_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_MARIGOLD_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_CATHARANTHUS_ROSEUS_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_TRILLIUM_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_NAMEK_FERN.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_SACRED_CHRYSANTHEMUM_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_SACRED_AMARYLLIS_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_SACRED_MARIGOLD_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_SACRED_CATHARANTHUS_ROSEUS_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_SACRED_TRILLIUM_FLOWER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_SACRED_FERN.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_AJISSA_SAPLING.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.POTTED_SACRED_SAPLING.get(), RenderType.cutout());
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
        event.registerEntityRenderer(MainEntities.MASTER_DENDE.get(), MasterEntityRenderer::new);
        event.registerEntityRenderer(MainEntities.MASTER_GERO.get(), MasterEntityRenderer::new);
        event.registerEntityRenderer(MainEntities.MASTER_POPO.get(), MasterEntityRenderer::new);

        event.registerEntityRenderer(MainEntities.SAGA_SAIBAMAN.get(), SagaSaibamanRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_SAIBAMAN2.get(), SagaSaibamanRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_SAIBAMAN3.get(), SagaSaibamanRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_SAIBAMAN4.get(), SagaSaibamanRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_SAIBAMAN5.get(), SagaSaibamanRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_SAIBAMAN6.get(), SagaSaibamanRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_RADITZ.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_NAPPA.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_VEGETA.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_OZARU_VEGETA.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_FRIEZA_SOLDIER.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_FRIEZA_SOLDIER2.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_FRIEZA_SOLDIER3.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_MORO_SOLDIER.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_CUI.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_DODORIA.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_VEGETA_NAMEK.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_ZARBON.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_ZARBON_TRANSF.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_GULDO.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_RECOOME.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_BURTER.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_JEICE.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_GINYU.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_GINYU_GOKU.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_FREEZER_FIRST.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_FREEZER_SECOND.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_FREEZER_THIRD.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_FREEZER_BASE.get(), DBSagasRenderer::new);
        event.registerEntityRenderer(MainEntities.SAGA_FREEZER_FP.get(), DBSagasRenderer::new);

        event.registerEntityRenderer(MainEntities.DINOSAUR1.get(), DinosRenderer::new);
        event.registerEntityRenderer(MainEntities.DINOSAUR2.get(), GranDinoRenderer::new);
        event.registerEntityRenderer(MainEntities.DINOSAUR3.get(), DinoFlyRenderer::new);
        event.registerEntityRenderer(MainEntities.DINO_KID.get(), DinosRenderer::new);
        event.registerEntityRenderer(MainEntities.NAMEK_FROG.get(), NamekFrogRenderer::new);
        event.registerEntityRenderer(MainEntities.NAMEK_FROG_GINYU.get(), NamekFrogRenderer::new);
        event.registerEntityRenderer(MainEntities.NAMEK_TRADER.get(), NamekianRenderer::new);
        event.registerEntityRenderer(MainEntities.NAMEK_WARRIOR.get(), NamekianWarriorRenderer::new);
        event.registerEntityRenderer(MainEntities.SABERTOOTH.get(), DinosRenderer::new);

        event.registerEntityRenderer(MainEntities.BANDIT.get(), RedRibbonRenderer::new);
        event.registerEntityRenderer(MainEntities.RED_RIBBON_ROBOT1.get(), RobotRRRenderer::new);
        event.registerEntityRenderer(MainEntities.RED_RIBBON_ROBOT2.get(), RobotRRRenderer::new);
        event.registerEntityRenderer(MainEntities.RED_RIBBON_ROBOT3.get(), RobotRRRenderer::new);
        event.registerEntityRenderer(MainEntities.RED_RIBBON_SOLDIER.get(), RedRibbonSoldierRenderer::new);
        event.registerEntityRenderer(MainEntities.SPACE_POD.get(), SpacePodRenderer::new);
        event.registerEntityRenderer(MainEntities.FLYING_NIMUS.get(), FlyingNimbusRenderer::new);
        event.registerEntityRenderer(MainEntities.ROBOT_XENOVERSE.get(), RedRibbonRenderer::new);
        event.registerEntityRenderer(MainEntities.PUNCH_MACHINE.get(), PunchMachineRenderer::new);

		event.registerEntityRenderer(MainEntities.SHENRON.get(), DragonDBRenderer::new);
		event.registerEntityRenderer(MainEntities.PORUNGA.get(), DragonDBRenderer::new);

        event.registerEntityRenderer(MainEntities.KI_BLAST.get(), KiProjectileRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_VOLLEY.get(), KiProjectileRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_EXPLOSION.get(), KiExplosionRenderer::new);
        event.registerEntityRenderer(MainEntities.SP_BLUE_HURRICANE.get(), SPSkillsRenderer::new);
        event.registerEntityRenderer(MainEntities.SP_BLUE_HURRICANE.get(), SPSkillsRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_LASER.get(), KiLaserRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_WAVE.get(), KiWaveRenderer::new);

    }

    @SubscribeEvent
    public static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(ArmorBaseModel.LAYER_LOCATION, ArmorBaseModel::createBodyLayer);
        e.registerLayerDefinition(KiBallPlaneModel.LAYER_LOCATION, KiBallPlaneModel::createBodyLayer);
        e.registerLayerDefinition(KiLaserModel.LAYER_LOCATION, KiLaserModel::createBodyLayer);
        e.registerLayerDefinition(KiLaserExplosionModel.LAYER_LOCATION, KiLaserExplosionModel::createBodyLayer);
        e.registerLayerDefinition(KiLaserExplosion2Model.LAYER_LOCATION, KiLaserExplosion2Model::createBodyLayer);

        e.registerLayerDefinition(KiScytheModel.LAYER_LOCATION, KiScytheModel::createBodyLayer);
        e.registerLayerDefinition(KiBladeModel.LAYER_LOCATION, KiBladeModel::createBodyLayer);
        e.registerLayerDefinition(KiTridentModel.LAYER_LOCATION, KiTridentModel::createBodyLayer);

    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(MainParticles.KI_FLASH.get(), KiFlashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_SPLASH.get(), KiSplashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_TRAIL.get(), KiTrailParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_EXPLOSION_FLASH.get(), KiExplosionFlashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_EXPLOSION_SPLASH.get(), KiExplosionSplashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KINTON.get(), KintonParticle.Provider::new);
        event.registerSpriteSet(MainParticles.PUNCH_PARTICLE.get(), PunchParticle.Provider::new);
        event.registerSpriteSet(MainParticles.BLOCK_PARTICLE.get(), BlockParticle.Provider::new);
        event.registerSpriteSet(MainParticles.GUARD_BLOCK.get(), GuardBlockParticle.Provider::new);
        event.registerSpriteSet(MainParticles.SPARKS.get(), KiSplashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.AURA.get(), AuraParticle.Provider::new);

    }

	@SubscribeEvent
	public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
		CustomSpecialEffects.registerSpecialEffects(event);
	}
}
