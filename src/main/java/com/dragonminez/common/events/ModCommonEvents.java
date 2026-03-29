package com.dragonminez.common.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.*;
import com.dragonminez.common.init.entities.animal.*;
import com.dragonminez.common.init.entities.dragon.PorungaEntity;
import com.dragonminez.common.init.entities.dragon.ShenronEntity;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import com.dragonminez.common.init.entities.redribbon.BanditEntity;
import com.dragonminez.common.init.entities.redribbon.RedRibbonSoldierEntity;
import com.dragonminez.common.init.entities.redribbon.RobotEntity;
import com.dragonminez.common.init.entities.sagas.*;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.server.world.data.DragonBallSavedData;
import com.dragonminez.server.world.gen.OverworldSurfaceRules;
import com.dragonminez.server.world.region.OverworldRegion;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.RegistryObject;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCommonEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // MAESTROS
        regAttr(event, MastersEntity.createAttributes().build(),
                MainEntities.MASTER_KARIN, MainEntities.MASTER_GOKU, MainEntities.MASTER_KAIOSAMA, MainEntities.MASTER_ROSHI,
                MainEntities.MASTER_URANAI, MainEntities.MASTER_ENMA, MainEntities.MASTER_DENDE, MainEntities.MASTER_GERO,
                MainEntities.MASTER_POPO, MainEntities.MASTER_GURU, MainEntities.MASTER_TORIBOT);

        // Quest NPC — single entity type for all data-driven quest NPCs | Usa un solo tipo de entidad para todos los NPCs de misiones basados en datos
        event.put(MainEntities.QUEST_NPC.get(), MastersEntity.createAttributes().build());

		event.put(MainEntities.SHENRON.get(), ShenronEntity.createAttributes().build());
		event.put(MainEntities.PORUNGA.get(), PorungaEntity.createAttributes().build());

        // SAIBAMANS
        regAttr(event, SagaSaibamanEntity.createAttributes().build(),
                MainEntities.SAGA_SAIBAMAN, MainEntities.SAGA_SAIBAMAN2, MainEntities.SAGA_SAIBAMAN3,
                MainEntities.SAGA_SAIBAMAN4, MainEntities.SAGA_SAIBAMAN5, MainEntities.SAGA_SAIBAMAN6);
        // SOLDADOS DE FRIEZA Y MORO
        regAttr(event, SagaFriezaSoldier01Entity.createAttributes().build(),
                MainEntities.SAGA_FRIEZA_SOLDIER, MainEntities.SAGA_FRIEZA_SOLDIER2,
                MainEntities.SAGA_FRIEZA_SOLDIER3, MainEntities.SAGA_MORO_SOLDIER);

        // SAGA ESPECIALES
        event.put(MainEntities.SAGA_OZARU_VEGETA.get(), SagaOzaruEntity.createAttributes().build());
        event.put(MainEntities.SAGA_OZARU.get(), SagaOzaruEntity.createAttributes().build());
        event.put(MainEntities.SAGA_BURTER.get(), SagaBurterEntity.createAttributes().build());

        // SAGAS
        regAttr(event, DBSagasEntity.createAttributes().build(),
                MainEntities.SAGA_GOKU_EARLY, MainEntities.SAGA_GOKU_EARLY_NOWEIGHTS, MainEntities.SAGA_PICCOLO_EARLY,
                MainEntities.SAGA_RADITZ, MainEntities.SAGA_NAPPA, MainEntities.SAGA_VEGETA,
                MainEntities.SAGA_CUI, MainEntities.SAGA_DODORIA, MainEntities.SAGA_VEGETA_NAMEK, MainEntities.SAGA_ZARBON, MainEntities.SAGA_ZARBON_TRANSF,
                MainEntities.SAGA_GULDO, MainEntities.SAGA_RECOOME, MainEntities.SAGA_JEICE, MainEntities.SAGA_GINYU, MainEntities.SAGA_GINYU_GOKU,
                MainEntities.SAGA_FREEZER_FIRST, MainEntities.SAGA_FREEZER_SECOND, MainEntities.SAGA_FREEZER_THIRD, MainEntities.SAGA_FREEZER_BASE, MainEntities.SAGA_FREEZER_FP,
                MainEntities.SAGA_MECHA_FRIEZA, MainEntities.SAGA_KING_COLD, MainEntities.SAGA_GOKU_YARDRAT, MainEntities.SAGA_DRGERO, MainEntities.SAGA_A19, MainEntities.SAGA_A18, MainEntities.SAGA_A17, MainEntities.SAGA_A16,
                MainEntities.SAGA_CELL_IMPERFECT, MainEntities.SAGA_PICCOLO_KAMI, MainEntities.SAGA_CELL_SEMIPERFECT, MainEntities.SAGA_SUPER_VEGETA, MainEntities.SAGA_TRUNKS_SSJ, MainEntities.SAGA_CELL_PERFECT,
                MainEntities.SAGA_GOHAN_SSJ, MainEntities.SAGA_CELL_SUPERPERFECT, MainEntities.SAGA_CELL_JR, MainEntities.SHADOW_DUMMY);


        event.put(MainEntities.DINOSAUR1.get(), Dino1Entity.createAttributes().build());
        event.put(MainEntities.DINOSAUR2.get(), Dino2Entity.createAttributes().build());
        event.put(MainEntities.DINOSAUR3.get(), DinoFlyEntity.createAttributes().build());
        event.put(MainEntities.DINO_KID.get(), DinoKidEntity.createAttributes().build());
        event.put(MainEntities.NAMEK_FROG.get(), NamekFrogEntity.createAttributes());
        event.put(MainEntities.NAMEK_FROG_GINYU.get(), NamekFrogGinyuEntity.createAttributes());
        event.put(MainEntities.NAMEK_TRADER.get(), NamekTraderEntity.createAttributes().build());
        event.put(MainEntities.NAMEK_WARRIOR.get(), NamekWarriorEntity.createAttributes().build());
        event.put(MainEntities.SABERTOOTH.get(), SabertoothEntity.createAttributes().build());

        event.put(MainEntities.BANDIT.get(), BanditEntity.createAttributes().build());
        event.put(MainEntities.RED_RIBBON_ROBOT1.get(), RobotEntity.createAttributes().build());
        event.put(MainEntities.RED_RIBBON_ROBOT2.get(), RobotEntity.createAttributes().build());
        event.put(MainEntities.RED_RIBBON_ROBOT3.get(), RobotEntity.createAttributes().build());
        event.put(MainEntities.RED_RIBBON_SOLDIER.get(), RedRibbonSoldierEntity.createAttributes().build());
        event.put(MainEntities.SPACE_POD.get(), SpacePodEntity.createAttributes());
        event.put(MainEntities.FLYING_NIMBUS.get(), FlyingNimbusEntity.createAttributes());
        event.put(MainEntities.BLACK_NIMBUS.get(), BlackNimbusEntity.createAttributes());
        event.put(MainEntities.ROBOT_XENOVERSE.get(), RobotEntity.createAttributes().build());
        event.put(MainEntities.PUNCH_MACHINE.get(), PunchMachineEntity.createAttributes().build());
        event.put(MainEntities.MAJIN_SKILL.get(), MajinSkillEntity.createAttributes().build());

    }

	public static void commonSetup(final FMLCommonSetupEvent event) {
		new PredefinedTechniques().init();

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

			Regions.register(new OverworldRegion(14));
			SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, Reference.MOD_ID, OverworldSurfaceRules.makeRules());
		});
	}


    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MainAttributes.DMZ_HEALTH.get());
    }

	@SubscribeEvent
	public void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
		event.register(DragonBallSavedData.class);
	}

    @SafeVarargs
    private static <T extends LivingEntity> void regAttr(EntityAttributeCreationEvent event, AttributeSupplier attributes, RegistryObject<? extends EntityType<? extends T>>... entities) {
        for (RegistryObject<? extends EntityType<? extends T>> reg : entities) {
            event.put(reg.get(), attributes);
        }
    }
}
