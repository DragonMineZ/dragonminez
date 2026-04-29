package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.crowdin.CrowdinManager;
import com.dragonminez.client.crowdin.CrowdinPackResources;
import com.dragonminez.client.dragonball.DragonBallPackResources;
import com.dragonminez.client.animation.CombatAnimationResolver;
import com.dragonminez.client.gui.UtilityMenuScreen;
import com.dragonminez.client.gui.hud.*;
import com.dragonminez.client.init.blocks.renderer.DragonBallBlockRenderer;
import com.dragonminez.client.init.blocks.renderer.EnergyCableBlockRenderer;
import com.dragonminez.client.init.blocks.renderer.FuelGeneratorBlockRenderer;
import com.dragonminez.client.init.blocks.renderer.KikonoStationBlockRenderer;
import com.dragonminez.client.init.entities.model.ki.*;
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
import com.dragonminez.client.util.TextureCounter;
import com.dragonminez.common.init.*;
import com.dragonminez.common.init.armor.client.model.ArmorBaseModel;
import com.dragonminez.client.init.menu.screens.FuelGeneratorScreen;
import com.dragonminez.client.init.menu.screens.KikonoStationScreen;
import com.dragonminez.common.init.particles.*;
import com.dragonminez.server.world.dimension.CustomSpecialEffects;
import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

	@SubscribeEvent
	public static void registerGuiOverlays(RegisterGuiOverlaysEvent e) {
		e.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "xenoversehud", XenoverseHUD.HUD_XENOVERSE);
		e.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "alternativehud", AlternativeHUD.HUD_ALTERNATIVE);
		e.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "technique_charge_hud", TechniqueChargeOverlay.HUD_TECHNIQUE_CHARGE);
		e.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "scouterhud", ScouterHUD.HUD_SCOUTER);
		e.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "tracked_quest_hud", TrackedQuestHUD.HUD_TRACKED_QUEST);
		e.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "techniquehud", TechniqueHotbarHUD.HUD_TECHNIQUES);
	}

  @SubscribeEvent
  public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
    event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
      @Override
      protected Void prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        return null;
      }

      @Override
      protected void apply(Void unused, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        TextureCounter.clearCache();
        CombatAnimationResolver.reload(resourceManager);
      }
    });
  }

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        KeyBinds.registerAll(event);
    }

		@SubscribeEvent
	public static void onAddPackFinders(AddPackFindersEvent event) {
		if (event.getPackType() == PackType.CLIENT_RESOURCES) {
      if (CrowdinManager.isLiveTranslationsEnabled()) {
        String currentLang = Minecraft.getInstance().options.languageCode;
        CrowdinManager.fetchLanguage(currentLang);
      }

			event.addRepositorySource((packConsumer) -> {
				Pack crowdinPack = Pack.readMetaAndCreate("dmz_crowdin_ota", Component.literal("DMZ Live Translations"), true,
						CrowdinPackResources::new, PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
				if (crowdinPack != null) packConsumer.accept(crowdinPack);

				Pack dragonBallRuntimePack = Pack.readMetaAndCreate("dmz_dragonballs_runtime", Component.literal("DMZ Dragonballs Runtime Resources"), true,
						DragonBallPackResources::new, PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
				if (dragonBallRuntimePack != null) packConsumer.accept(dragonBallRuntimePack);
			});
		}
	}


	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			setCustomWindowIcon();

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


			ItemProperties.registerGeneric(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "loaded"),
					(stack, level, entity, seed) -> {
						return 1.0F; // Loaded items :D
					});
        });

        UtilityMenuScreen.initMenuSlots();
		Minecraft.getInstance().getMainRenderTarget().enableStencil();
	}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        //MAESTROS
        regRender(event, MasterEntityRenderer::new,
                MainEntities.MASTER_KARIN, MainEntities.MASTER_GOKU, MainEntities.MASTER_KAIOSAMA, MainEntities.MASTER_ROSHI,
                MainEntities.MASTER_URANAI, MainEntities.MASTER_ENMA, MainEntities.MASTER_DENDE, MainEntities.MASTER_GERO,
                MainEntities.MASTER_POPO, MainEntities.MASTER_GURU, MainEntities.MASTER_TORIBOT);

        // Quest NPC — single renderer for all data-driven quest NPCs | usa un renderer genérico para los NPCs de misiones, después usa gráficos.json para asignar modelos/texturas específicos a cada npcId
        event.registerEntityRenderer(MainEntities.QUEST_NPC.get(), QuestNPCRenderer::new);

        //SAIBAMANS
        regRender(event, SagaSaibamanRenderer::new,
                MainEntities.SAGA_SAIBAMAN, MainEntities.SAGA_SAIBAMAN2, MainEntities.SAGA_SAIBAMAN3,
                MainEntities.SAGA_SAIBAMAN4, MainEntities.SAGA_SAIBAMAN5, MainEntities.SAGA_SAIBAMAN6);

        // SAGAS
        regRender(event, DBSagasRenderer::new,
                MainEntities.SAGA_GOKU_EARLY, MainEntities.SAGA_GOKU_EARLY_NOWEIGHTS, MainEntities.SAGA_PICCOLO_EARLY, MainEntities.SAGA_TIEN_EARLY, MainEntities.SAGA_YAMCHA,
                MainEntities.SAGA_RADITZ, MainEntities.SAGA_NAPPA, MainEntities.SAGA_VEGETA, MainEntities.SAGA_OZARU_VEGETA, MainEntities.SAGA_OZARU, MainEntities.SAGA_CHAOZ,
                MainEntities.SAGA_FRIEZA_SOLDIER, MainEntities.SAGA_FRIEZA_SOLDIER2, MainEntities.SAGA_FRIEZA_SOLDIER3, MainEntities.SAGA_MORO_SOLDIER,
                MainEntities.SAGA_CUI, MainEntities.SAGA_DODORIA, MainEntities.SAGA_VEGETA_NAMEK, MainEntities.SAGA_ZARBON, MainEntities.SAGA_ZARBON_TRANSF,
                MainEntities.SAGA_GULDO, MainEntities.SAGA_RECOOME, MainEntities.SAGA_BURTER, MainEntities.SAGA_JEICE, MainEntities.SAGA_GINYU, MainEntities.SAGA_GINYU_GOKU, MainEntities.SAGA_NAIL,
                MainEntities.SAGA_FREEZER_FIRST, MainEntities.SAGA_FREEZER_SECOND, MainEntities.SAGA_FREEZER_THIRD, MainEntities.SAGA_FREEZER_BASE, MainEntities.SAGA_FREEZER_FP,
                MainEntities.SAGA_GOKU_MID_BASE, MainEntities.SAGA_GOKU_MID_SSJ, MainEntities.SAGA_KID_GOHAN, MainEntities.SAGA_KRILLIN,
                MainEntities.SAGA_MECHA_FRIEZA, MainEntities.SAGA_KING_COLD, MainEntities.SAGA_DRGERO, MainEntities.SAGA_A19, MainEntities.SAGA_A18, MainEntities.SAGA_A17, MainEntities.SAGA_A16,
                MainEntities.SAGA_CELL_IMPERFECT, MainEntities.SAGA_PICCOLO_KAMI,  MainEntities.SAGA_VEGETA_MID, MainEntities.SAGA_VEGETA_MID_SSJ, MainEntities.SAGA_VEGETA_MID_SSG2,
                MainEntities.SAGA_FUTURE_TRUNKS_KID_BASE, MainEntities.SAGA_FUTURE_TRUNKS_KID_SSJ, MainEntities.SAGA_FUTURE_TRUNKS_BASE, MainEntities.SAGA_FUTURE_TRUNKS_SSJ,
                MainEntities.SAGA_FUTURE_TRUNKS_SSG3, MainEntities.SAGA_GOHAN_MID_BASE, MainEntities.SAGA_GOHAN_MID_SSJ, MainEntities.SAGA_GOHAN_MID_SSJ2, MainEntities.SAGA_FUTURE_GOHAN_BASE, MainEntities.SAGA_FUTURE_GOHAN_SSJ,
                MainEntities.SAGA_CELL_SEMIPERFECT, MainEntities.SAGA_CELL_PERFECT, MainEntities.SAGA_CELL_SUPERPERFECT, MainEntities.SAGA_CELL_JR,
                MainEntities.SAGA_GOKU_END_BASE, MainEntities.SAGA_GOKU_END_SSJ, MainEntities.SAGA_GOKU_END_SSJ2, MainEntities.SAGA_GOKU_END_SSJ3,
                MainEntities.SAGA_VEGETA_END_BASE, MainEntities.SAGA_VEGETA_END_SSJ, MainEntities.SAGA_VEGETA_END_SSJ2, MainEntities.SAGA_VEGETA_MAJIN,
                MainEntities.SAGA_GOHAN_END_BASE, MainEntities.SAGA_GOHAN_END_SSJ, MainEntities.SAGA_GOHAN_END_SSJ2, MainEntities.SAGA_GOHAN_END_ULTIMATE,
                MainEntities.SAGA_GOTEN, MainEntities.SAGA_GOTEN_SSJ, MainEntities.SAGA_KID_TRUNKS, MainEntities.SAGA_KID_TRUNKS_SSJ,  MainEntities.SAGA_VIDEL,  MainEntities.SAGA_BULMA,
                MainEntities.SAGA_GOTENKS, MainEntities.SAGA_GOTENKS_SSJ, MainEntities.SAGA_GOTENKS_SSJ3,
                MainEntities.SAGA_SHIN, MainEntities.SAGA_KIBITO, MainEntities.SAGA_SPOPOVITCH, MainEntities.SAGA_PUIPUI, MainEntities.SAGA_YAKON, MainEntities.SAGA_DABURA,
                MainEntities.SAGA_BABIDI, MainEntities.SAGA_BUU_FAT, MainEntities.SAGA_EVILBUU, MainEntities.SAGA_SUPERBUU,  MainEntities.SAGA_SUPERBUU_PICCOLO,
                MainEntities.SAGA_SUPERBUU_GOTENKS, MainEntities.SAGA_SUPERBUU_GOHAN, MainEntities.SAGA_KIDBUU,
                MainEntities.SHADOW_DUMMY);



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
        event.registerEntityRenderer(MainEntities.FLYING_NIMBUS.get(), FlyingNimbusRenderer::new);
        event.registerEntityRenderer(MainEntities.BLACK_NIMBUS.get(), BlackNimbusRenderer::new);
        event.registerEntityRenderer(MainEntities.ROBOT_XENOVERSE.get(), RedRibbonRenderer::new);
        event.registerEntityRenderer(MainEntities.PUNCH_MACHINE.get(), PunchMachineRenderer::new);

		for (var entity : MainEntities.getDragonWishEntities().values()) {
			event.registerEntityRenderer(entity.get(), DragonDBRenderer::new);
		}

        event.registerEntityRenderer(MainEntities.KI_BLAST.get(), KiProjectileRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_EXPLOSION.get(), KiExplosionRenderer::new);
        event.registerEntityRenderer(MainEntities.SP_BLUE_HURRICANE.get(), SPBlueHurricaneRenderer::new);
        event.registerEntityRenderer(MainEntities.SP_DRAGON_FIST.get(), SPDragonFistRenderer::new);
        event.registerEntityRenderer(MainEntities.SP_MAJIN_CANDY.get(), SPMajinCandyRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_LASER.get(), KiLaserRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_WAVE.get(), KiWaveRenderer::new);
        event.registerEntityRenderer(MainEntities.MAJIN_SKILL.get(), MajinSkillRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_DISC.get(), KiDiskRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_BARRIER.get(), KiBarrierRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_EXPLOSION_VISUAL.get(), KiExplosionVisualRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_AREA.get(), KiProjectileRenderer::new);

    }

    @SubscribeEvent
    public static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(ArmorBaseModel.LAYER_LOCATION, ArmorBaseModel::createBodyLayer);
        e.registerLayerDefinition(KiBallPlaneModel.LAYER_LOCATION, KiBallPlaneModel::createBodyLayer);
        e.registerLayerDefinition(KiLaserModel.LAYER_LOCATION, KiLaserModel::createBodyLayer);
        e.registerLayerDefinition(KiLaserExplosionModel.LAYER_LOCATION, KiLaserExplosionModel::createBodyLayer);
        e.registerLayerDefinition(KiLaserExplosion2Model.LAYER_LOCATION, KiLaserExplosion2Model::createBodyLayer);
        e.registerLayerDefinition(KiWaveModel.LAYER_LOCATION, KiWaveModel::createBodyLayer);
        e.registerLayerDefinition(KiWave2DModel.LAYER_LOCATION, KiWave2DModel::createBodyLayer);
        e.registerLayerDefinition(KiWaveExplodeModel.LAYER_LOCATION, KiWaveExplodeModel::createBodyLayer);
        e.registerLayerDefinition(KiBallModel.LAYER_LOCATION, KiBallModel::createBodyLayer);
        e.registerLayerDefinition(KiBlockModel.LAYER_LOCATION, KiBlockModel::createBodyLayer);


        e.registerLayerDefinition(KiScytheModel.LAYER_LOCATION, KiScytheModel::createBodyLayer);
        e.registerLayerDefinition(KiBladeModel.LAYER_LOCATION, KiBladeModel::createBodyLayer);
        e.registerLayerDefinition(KiTridentModel.LAYER_LOCATION, KiTridentModel::createBodyLayer);
        e.registerLayerDefinition(KiDiscModel.LAYER_LOCATION, KiDiscModel::createBodyLayer);

    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(MainParticles.KI_FLASH.get(), KiFlashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_SPLASH.get(), KiSplashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_SPLASH_WAVE.get(), KiSplashWaveParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_TRAIL.get(), KiTrailParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_SHEDDING.get(), KiSheddingParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_LIGHTNING.get(), KiLightningParticle.Provider::new);

        event.registerSpriteSet(MainParticles.KI_EXPLOSION_FLASH.get(), KiExplosionFlashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_EXPLOSION_SPLASH.get(), KiExplosionSplashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_EXPLOSION.get(), KiExplosionParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KINTON.get(), KintonParticle.Provider::new);
        event.registerSpriteSet(MainParticles.PUNCH_PARTICLE.get(), PunchParticle.Provider::new);
        event.registerSpriteSet(MainParticles.BLOCK_PARTICLE.get(), BlockParticle.Provider::new);
        event.registerSpriteSet(MainParticles.GUARD_BLOCK.get(), GuardBlockParticle.Provider::new);
        event.registerSpriteSet(MainParticles.SPARKS.get(), KiSplashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.AURA.get(), AuraParticle.Provider::new);
        event.registerSpriteSet(MainParticles.DUST.get(), DustParticle.Provider::new);
        event.registerSpriteSet(MainParticles.ROCK.get(), RockParticle.Provider::new);
        event.registerSpriteSet(MainParticles.DIVINE.get(), DivineParticle.Provider::new);

    }

    @SafeVarargs
    private static <T extends Entity> void regRender(EntityRenderersEvent.RegisterRenderers event, EntityRendererProvider<T> provider, RegistryObject<? extends EntityType<? extends T>>... entities) {
        for (RegistryObject<? extends EntityType<? extends T>> reg : entities) {
            event.registerEntityRenderer(reg.get(), provider);
        }
    }

	@SubscribeEvent
	public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
		CustomSpecialEffects.registerSpecialEffects(event);
	}

	private static void setCustomWindowIcon() {
		Minecraft mc = Minecraft.getInstance();

		if (Minecraft.ON_OSX) {
			try {
				ResourceLocation macLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "icons/minecraft.icns");
				var res = mc.getResourceManager().getResource(macLoc);
				if (res.isPresent()) MacosUtil.loadIcon(res.get()::open);
			} catch (Exception ignored) {}
			return;
		}

		long windowId = mc.getWindow().getWindow();
		String[] iconNames = {"icon_16x16.png", "icon_32x32.png", "icon_48x48.png", "icon_128x128.png", "icon_256x256.png"};
		List<NativeImage> loadedImages = new ArrayList<>();

		for (String name : iconNames) {
			try {
				ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "icons/" + name);
				var resource = mc.getResourceManager().getResource(loc);
				if (resource.isPresent()) {
					try (InputStream is = resource.get().open()) {
						loadedImages.add(NativeImage.read(is));
					}
				}
			} catch (Exception ignored) {}
		}

		if (loadedImages.isEmpty()) return;
		List<ByteBuffer> buffersToFree = new ArrayList<>();

		try (MemoryStack stack = MemoryStack.stackPush()) {
			GLFWImage.Buffer glfwImages = GLFWImage.malloc(loadedImages.size(), stack);

			for (int i = 0; i < loadedImages.size(); i++) {
				NativeImage image = loadedImages.get(i);
				ByteBuffer byteBuffer = MemoryUtil.memAlloc(image.getWidth() * image.getHeight() * 4);
				buffersToFree.add(byteBuffer);
				byteBuffer.asIntBuffer().put(image.getPixelsRGBA());
				glfwImages.position(i);
				glfwImages.width(image.getWidth());
				glfwImages.height(image.getHeight());
				glfwImages.pixels(byteBuffer);
			}
			GLFW.glfwSetWindowIcon(windowId, glfwImages.position(0));
		} finally {
			buffersToFree.forEach(MemoryUtil::memFree);
			loadedImages.forEach(NativeImage::close);
		}
	}
}
