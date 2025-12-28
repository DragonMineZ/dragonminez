package com.dragonminez.common.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.FlyingNimbusEntity;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.common.init.entities.animal.*;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import com.dragonminez.common.init.entities.redribbon.BanditEntity;
import com.dragonminez.common.init.entities.redribbon.RedRibbonSoldierEntity;
import com.dragonminez.common.init.entities.redribbon.RobotEntity;
import com.dragonminez.common.init.entities.sagas.*;
import com.dragonminez.server.world.data.DragonBallSavedData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCommonEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MainEntities.MASTER_KARIN.get(), MastersEntity.createAttributes().build());
        event.put(MainEntities.MASTER_GOKU.get(), MastersEntity.createAttributes().build());
        event.put(MainEntities.MASTER_KAIOSAMA.get(), MastersEntity.createAttributes().build());
        event.put(MainEntities.MASTER_ROSHI.get(), MastersEntity.createAttributes().build());
        event.put(MainEntities.MASTER_URANAI.get(), MastersEntity.createAttributes().build());
        event.put(MainEntities.MASTER_ENMA.get(), MastersEntity.createAttributes().build());
        event.put(MainEntities.MASTER_DENDE.get(), MastersEntity.createAttributes().build());

        event.put(MainEntities.SAGA_SAIBAMAN.get(), SagaSaibamanEntity.createAttributes().build());
        event.put(MainEntities.SAGA_SAIBAMAN2.get(), SagaSaibamanEntity.createAttributes().build());
        event.put(MainEntities.SAGA_SAIBAMAN3.get(), SagaSaibamanEntity.createAttributes().build());
        event.put(MainEntities.SAGA_SAIBAMAN4.get(), SagaSaibamanEntity.createAttributes().build());
        event.put(MainEntities.SAGA_SAIBAMAN5.get(), SagaSaibamanEntity.createAttributes().build());
        event.put(MainEntities.SAGA_SAIBAMAN6.get(), SagaSaibamanEntity.createAttributes().build());
        event.put(MainEntities.SAGA_RADITZ.get(), SagaRaditzEntity.createAttributes().build());
        event.put(MainEntities.SAGA_NAPPA.get(), SagaNappaEntity.createAttributes().build());
        event.put(MainEntities.SAGA_VEGETA.get(), SagaVegetaEntity.createAttributes().build());
        event.put(MainEntities.SAGA_OZARU_VEGETA.get(), SagaOzaruVegetaEntity.createAttributes().build());
        event.put(MainEntities.SAGA_FRIEZA_SOLDIER.get(), SagaFriezaSoldier01Entity.createAttributes().build());
        event.put(MainEntities.SAGA_FRIEZA_SOLDIER2.get(), SagaFriezaSoldier02Entity.createAttributes().build());
        event.put(MainEntities.SAGA_FRIEZA_SOLDIER3.get(), SagaFriezaSoldier02Entity.createAttributes().build());
        event.put(MainEntities.SAGA_MORO_SOLDIER.get(), SagaFriezaSoldier01Entity.createAttributes().build());

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
        event.put(MainEntities.FLYING_NIMUS.get(), FlyingNimbusEntity.createAttributes());

    }

	public static void commonSetup(final FMLCommonSetupEvent event) {
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

			//SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, Reference.MOD_ID, ModSurfaceRules.makeRules());
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
}
