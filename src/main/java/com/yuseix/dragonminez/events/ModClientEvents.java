package com.yuseix.dragonminez.events;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.client.character.models.AuraModel;
import com.yuseix.dragonminez.client.character.models.HumanSaiyanModel;
import com.yuseix.dragonminez.client.character.models.NamekianModel;
import com.yuseix.dragonminez.client.character.models.SlimHumanSaiyanModel;
import com.yuseix.dragonminez.client.character.models.bioandroid.BioAndroidModel;
import com.yuseix.dragonminez.client.character.models.demoncold.DemonColdModel;
import com.yuseix.dragonminez.client.character.models.hair.*;
import com.yuseix.dragonminez.client.character.models.kiweapons.KiScytheModel;
import com.yuseix.dragonminez.client.character.models.kiweapons.KiSwordModel;
import com.yuseix.dragonminez.client.character.models.kiweapons.KiTridentModel;
import com.yuseix.dragonminez.client.character.models.majin.MajinFemaleModel;
import com.yuseix.dragonminez.client.character.models.majin.MajinGordoModel;
import com.yuseix.dragonminez.client.hud.PlayerHudOverlay;
import com.yuseix.dragonminez.client.hud.spaceship.SaiyanSpacePodOverlay;
import com.yuseix.dragonminez.init.*;
import com.yuseix.dragonminez.init.armor.client.model.ArmorBaseModel;
import com.yuseix.dragonminez.init.armor.client.model.ArmorPiccoloModel;
import com.yuseix.dragonminez.init.armor.client.model.ArmorSaiyanModel;
import com.yuseix.dragonminez.init.blocks.entity.client.*;
import com.yuseix.dragonminez.init.entity.client.model.namek.FriezaSoldier02Model;
import com.yuseix.dragonminez.init.entity.client.model.namek.FriezaSoldier03Model;
import com.yuseix.dragonminez.init.entity.client.model.namek.NamekNPCModel;
import com.yuseix.dragonminez.init.entity.client.model.projectil.KiBallModel;
import com.yuseix.dragonminez.init.entity.client.renderer.*;
import com.yuseix.dragonminez.init.entity.client.renderer.fpcharacters.*;
import com.yuseix.dragonminez.init.entity.client.renderer.namek.*;
import com.yuseix.dragonminez.init.entity.client.renderer.projectil.KiSmallBallRenderer;
import com.yuseix.dragonminez.init.items.models.BaculoEmptyModel;
import com.yuseix.dragonminez.init.items.models.TrunksSwordBackModel;
import com.yuseix.dragonminez.init.menus.screens.KikonoArmorStationScreen;
import com.yuseix.dragonminez.init.particles.AjissaLeavesParticle;
import com.yuseix.dragonminez.init.particles.SacredLeavesParticle;
import com.yuseix.dragonminez.utils.Keys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

	@SubscribeEvent
	public static void registerGuiOverlays(RegisterGuiOverlaysEvent e) {
		e.registerAboveAll("playerhud", PlayerHudOverlay.HUD_PLAYER);
		e.registerAboveAll("spaceshiphud", SaiyanSpacePodOverlay.HUD_SAIYAN);
	}

	@SubscribeEvent
	public static void registerParticles(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(MainParticles.AJISSA_LEAVES_PARTICLE.get(), AjissaLeavesParticle.Provider::new);
		event.registerSpriteSet(MainParticles.SACRED_LEAVES_PARTICLE.get(), SacredLeavesParticle.Provider::new);
	}

	@SubscribeEvent
	public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        /*
        Usa reflection para registrar todas las teclas de la clase Keys, utilicé esto para no tener que registrar cada tecla manualmente
        También porque los fields son static
         */
		try {
			Field[] fields = Keys.class.getDeclaredFields();

			for (Field field : fields) {
				if (Modifier.isStatic(field.getModifiers()) && field.getType() == KeyMapping.class) {
					KeyMapping keyMapping = (KeyMapping) field.get(null);
					event.register(keyMapping);
				}
			}
		} catch (IllegalAccessException e) {
			//System.out.println("Error al intentar registrar una tecla! " + e.getMessage());
		}
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			//ENTIDADES
			EntityRenderers.register(MainEntity.DINO1.get(), DinoRenderer::new);
			EntityRenderers.register(MainEntity.NUBE_VOLADORA.get(), NubeRenderer::new);
			EntityRenderers.register(MainEntity.MASTER_KARIN.get(), KarinRenderer::new);
			EntityRenderers.register(MainEntity.SHENLONG.get(), ShenlongRenderer::new);
			EntityRenderers.register(MainEntity.PORUNGA.get(), PorungaRenderer::new);
			EntityRenderers.register(MainEntity.MASTER_DENDE.get(), DendeRenderer::new);
			EntityRenderers.register(MainEntity.NAVE_SAIYAN.get(), NaveSaiyanRenderer::new);

			EntityRenderers.register(MainEntity.NAMEK_FROG.get(), NamekFrogRenderer::new);
			EntityRenderers.register(MainEntity.PINK_FROG.get(), PinkFrogRenderer::new);
			EntityRenderers.register(MainEntity.YELLOW_FROG.get(), YellowFrogRenderer::new);
			EntityRenderers.register(MainEntity.GINYU_FROG.get(), GinyuFrogRenderer::new);

			EntityRenderers.register(MainEntity.NAMEKNPC_WARRIOR1.get(), NamekWarrior1Renderer::new);
			EntityRenderers.register(MainEntity.NAMEKNPC_WARRIOR2.get(), NamekWarrior2Renderer::new);
			EntityRenderers.register(MainEntity.NAMEKNPC_TRADER1.get(), NamekTrader1Renderer::new);
			EntityRenderers.register(MainEntity.NAMEKNPC_TRADER2.get(), NamekTrader2Renderer::new);
			EntityRenderers.register(MainEntity.NAMEKNPC_TRADER3.get(), NamekTrader3Renderer::new);
			EntityRenderers.register(MainEntity.FRIEZA_SOLDIER01.get(), FriezaSoldierRenderer::new);
			EntityRenderers.register(MainEntity.FRIEZA_SOLDIER02.get(), FriezaSoldier02Renderer::new);
			EntityRenderers.register(MainEntity.FRIEZA_SOLDIER03.get(), FriezaSoldier03Renderer::new);
			EntityRenderers.register(MainEntity.MORO_SOLDIER.get(), MoroSoldierRenderer::new);

			//FAKEPLAYERS
			EntityRenderers.register(MainEntity.FP_BIOANDROIDE.get(), (context) -> new FPBioAndroidRender(context, new BioAndroidModel<>(context.bakeLayer(BioAndroidModel.LAYER_LOCATION))));
			EntityRenderers.register(MainEntity.FP_DEMONCOLD.get(), (context) -> new FPDemonColdRender(context, new DemonColdModel<>(context.bakeLayer(DemonColdModel.LAYER_LOCATION))));
			EntityRenderers.register(MainEntity.FP_HUMANSAIYAN.get(), (context) -> new FPHumSaiRender(context, new HumanSaiyanModel<>(context.bakeLayer(HumanSaiyanModel.LAYER_LOCATION))));
			EntityRenderers.register(MainEntity.FP_SLIMSAIYANHUM.get(), (context) -> new FPSlimHumSaiRender(context, new SlimHumanSaiyanModel<>(context.bakeLayer(SlimHumanSaiyanModel.LAYER_LOCATION))));
			EntityRenderers.register(MainEntity.FP_NAMEK.get(), FPNamekianRender::new);
			EntityRenderers.register(MainEntity.FP_MAJINGORDO.get(), FPMajinGordRender::new);
			EntityRenderers.register(MainEntity.KI_BLAST.get(), KiSmallBallRenderer::new);


			//BLOQUES
			BlockEntityRenderers.register(MainBlockEntities.DBALL1_NAMEK_BLOCK_ENTITY.get(), Dball1NamekBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.DBALL2_NAMEK_BLOCK_ENTITY.get(), Dball2NamekBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.DBALL3_NAMEK_BLOCK_ENTITY.get(), Dball3NamekBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.DBALL4_NAMEK_BLOCK_ENTITY.get(), Dball4NamekBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.DBALL5_NAMEK_BLOCK_ENTITY.get(), Dball5NamekBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.DBALL6_NAMEK_BLOCK_ENTITY.get(), Dball6NamekBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.DBALL7_NAMEK_BLOCK_ENTITY.get(), Dball7NamekBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.DBALL1_BLOCK_ENTITY.get(), Dball1BlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.DBALL2_BLOCK_ENTITY.get(), Dball2BlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.DBALL3_BLOCK_ENTITY.get(), Dball3BlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.DBALL4_BLOCK_ENTITY.get(), Dball4BlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.DBALL5_BLOCK_ENTITY.get(), Dball5BlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.DBALL6_BLOCK_ENTITY.get(), Dball6BlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.DBALL7_BLOCK_ENTITY.get(), Dball7BlockRenderer::new);

			ItemBlockRenderTypes.setRenderLayer(MainBlocks.KIKONO_ARMOR_STATION.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.NAMEK_AJISSA_LOG.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.NAMEK_SACRED_LOG.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(MainBlocks.INVISIBLE_LADDER_BLOCK.get(), RenderType.translucent());

			//MENÚS
			MenuScreens.register(MainMenus.KIKONO_ARMOR_STATION_MENU.get(), KikonoArmorStationScreen::new);

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


            /*Outline de las Dragon Balls Helper
            MinecraftForge.EVENT_BUS.addListener(DballOutlineRenderer::renderOutlineDball);*/

		});
	}

	@SubscribeEvent
	public static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
		//RAZAS
		e.registerLayerDefinition(HumanSaiyanModel.LAYER_LOCATION, HumanSaiyanModel::createBodyLayer);
		e.registerLayerDefinition(SlimHumanSaiyanModel.LAYER_LOCATION, SlimHumanSaiyanModel::createBodyLayer);
		e.registerLayerDefinition(NamekianModel.LAYER_LOCATION, NamekianModel::createBodyLayer);
		e.registerLayerDefinition(BioAndroidModel.LAYER_LOCATION, BioAndroidModel::createBodyLayer);

		e.registerLayerDefinition(MajinGordoModel.LAYER_LOCATION, MajinGordoModel::createBodyLayer);
		e.registerLayerDefinition(MajinFemaleModel.LAYER_LOCATION, MajinFemaleModel::createBodyLayer);
		e.registerLayerDefinition(DemonColdModel.LAYER_LOCATION, DemonColdModel::createBodyLayer);

		e.registerLayerDefinition(AuraModel.LAYER_LOCATION, AuraModel::createBodyLayer);
		e.registerLayerDefinition(KiBallModel.LAYER_LOCATION, KiBallModel::createBodyLayer);

		//CABELLOS
		e.registerLayerDefinition(GokuHairModel.LAYER_LOCATION, GokuHairModel::createBodyLayer);
		e.registerLayerDefinition(FemHairModel.LAYER_LOCATION, FemHairModel::createBodyLayer);
		e.registerLayerDefinition(VegetaHairModel.LAYER_LOCATION, VegetaHairModel::createBodyLayer);
		e.registerLayerDefinition(GohanDBSHairModel.LAYER_LOCATION, GohanDBSHairModel::createBodyLayer);
		e.registerLayerDefinition(GohanTeenHairModel.LAYER_LOCATION, GohanTeenHairModel::createBodyLayer);
		e.registerLayerDefinition(TrunksHairModel.LAYER_LOCATION, TrunksHairModel::createBodyLayer);
		e.registerLayerDefinition(UBDynamicHair.LAYER_LOCATION, UBDynamicHair::createBodyLayer);

		//ARMADURAS
		e.registerLayerDefinition(ArmorBaseModel.LAYER_LOCATION, ArmorBaseModel::createBodyLayer);
		e.registerLayerDefinition(ArmorSaiyanModel.LAYER_LOCATION, ArmorSaiyanModel::createBodyLayer);
		e.registerLayerDefinition(ArmorPiccoloModel.LAYER_LOCATION, ArmorPiccoloModel::createBodyLayer);

		//ENTIDADES CUSTOM EN BASE A MODELOS DE JAVA
		e.registerLayerDefinition(NamekNPCModel.LAYER_LOCATION, NamekNPCModel::createBodyLayer);
		e.registerLayerDefinition(FriezaSoldier02Model.LAYER_LOCATION, FriezaSoldier02Model::createBodyLayer);
		e.registerLayerDefinition(FriezaSoldier03Model.LAYER_LOCATION, FriezaSoldier03Model::createBodyLayer);

		//Armas en espalda
		e.registerLayerDefinition(BaculoEmptyModel.LAYER_LOCATION, BaculoEmptyModel::createBodyLayer);
		e.registerLayerDefinition(TrunksSwordBackModel.LAYER_LOCATION, TrunksSwordBackModel::createBodyLayer);

		//KI WEAPONS LOCATION
		e.registerLayerDefinition(KiScytheModel.LAYER_LOCATION, KiScytheModel::createBodyLayer);
		e.registerLayerDefinition(KiSwordModel.LAYER_LOCATION, KiSwordModel::createBodyLayer);
		e.registerLayerDefinition(KiTridentModel.LAYER_LOCATION, KiTridentModel::createBodyLayer);

	}


}
