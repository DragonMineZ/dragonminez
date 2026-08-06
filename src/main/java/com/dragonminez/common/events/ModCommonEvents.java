package com.dragonminez.common.events;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDataPackResources;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.*;
import com.dragonminez.common.init.entities.animal.*;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import com.dragonminez.common.init.MainBlockEntities;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

import java.util.List;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModCommonEvents {

	@SubscribeEvent
	public static void onAddPackFinders(AddPackFindersEvent event) {
		if (event.getPackType() == PackType.SERVER_DATA) {
			event.addRepositorySource((packConsumer) -> {
				Pack dragonballPack = Pack.readMetaAndCreate(
					new net.minecraft.server.packs.PackLocationInfo("dmz_dragonballs_runtime_data", Component.literal("DMZ Dragonballs Runtime Data"), PackSource.BUILT_IN, java.util.Optional.empty()),
					new Pack.ResourcesSupplier() {
						@Override public net.minecraft.server.packs.PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo location) { return new DragonBallDataPackResources(location); }
						@Override public net.minecraft.server.packs.PackResources openFull(net.minecraft.server.packs.PackLocationInfo location, Pack.Metadata metadata) { return openPrimary(location); }
					},
					PackType.SERVER_DATA,
					new net.minecraft.server.packs.PackSelectionConfig(true, Pack.Position.TOP, false));
				if (dragonballPack != null) packConsumer.accept(dragonballPack);
			});
		}
	}


    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // MAESTROS
        for (var masterEntity : MainEntities.getMasterEntities()) {
            event.put((EntityType<? extends LivingEntity>) masterEntity.get(), MastersEntity.createAttributes().build());
        }

        // Quest NPC — single entity type for all data-driven quest NPCs | Usa un solo tipo de entidad para todos los NPCs de misiones basados en datos
        event.put(MainEntities.QUEST_NPC.get(), MastersEntity.createAttributes().build());

		for (var entity : MainEntities.getDragonWishEntities().values()) {
			event.put(entity.get(), DragonWishEntity.createAttributes().build());
		}

        var saibamans = List.of(
                MainEntities.SAGA_SAIBAMAN, MainEntities.SAGA_SAIBAMAN2, MainEntities.SAGA_SAIBAMAN3,
                MainEntities.SAGA_SAIBAMAN4, MainEntities.SAGA_SAIBAMAN5, MainEntities.SAGA_SAIBAMAN6
        );
        var soldiers = List.of(
                MainEntities.SAGA_FRIEZA_SOLDIER, MainEntities.SAGA_FRIEZA_SOLDIER2,
                MainEntities.SAGA_FRIEZA_SOLDIER3, MainEntities.SAGA_MORO_SOLDIER
        );
        var ozarus = List.of(
                MainEntities.SAGA_OZARU_VEGETA, MainEntities.SAGA_OZARU
        );

        for (var saibaman : saibamans) event.put((EntityType<? extends LivingEntity>) saibaman.get(), SagaSaibamanEntity.createAttributes().build());
        for (var soldier : soldiers) event.put((EntityType<? extends LivingEntity>) soldier.get(), SagaFriezaSoldier01Entity.createAttributes().build());
        for (var ozaru : ozarus) event.put((EntityType<? extends LivingEntity>) ozaru.get(), SagaOzaruEntity.createAttributes().build());


        AttributeSupplier defaultSagaAttributes = DBSagasEntity.createAttributes().build();

        for (var sagaEntity : MainEntities.getSagaEntities()) {
            if (saibamans.contains(sagaEntity) || soldiers.contains(sagaEntity) || ozarus.contains(sagaEntity)) {
                continue;
            }
            event.put((EntityType<? extends LivingEntity>) sagaEntity.get(), defaultSagaAttributes);
        }

        event.put(MainEntities.DINOSAUR1.get(), Dino1Entity.createAttributes().build());
        event.put(MainEntities.DINOSAUR2.get(), Dino2Entity.createAttributes().build());
        event.put(MainEntities.DINOSAUR3.get(), DinoFlyEntity.createAttributes().build());
        event.put(MainEntities.DINO_KID.get(), DinoKidEntity.createAttributes().build());
        event.put(MainEntities.NAMEK_FROG.get(), NamekFrogEntity.createAttributes());
        event.put(MainEntities.NAMEK_FROG_GINYU.get(), NamekFrogGinyuEntity.createAttributes());
        event.put(MainEntities.NAMEK_TRADER.get(), NamekTraderEntity.createAttributes().build());
        event.put(MainEntities.CC_NAMEKIAN.get(), NamekTraderEntity.createAttributes().build());
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

			Regions.register(new OverworldRegion(40));
			SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, Reference.MOD_ID, OverworldSurfaceRules.makeRules());
		});
	}


    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MainAttributes.STRENGTH);
        event.add(EntityType.PLAYER, MainAttributes.STRIKE_POWER);
        event.add(EntityType.PLAYER, MainAttributes.RESISTANCE);
        event.add(EntityType.PLAYER, MainAttributes.VITALITY);
        event.add(EntityType.PLAYER, MainAttributes.KI_POWER);
        event.add(EntityType.PLAYER, MainAttributes.ENERGY);
        event.add(EntityType.PLAYER, MainAttributes.MAX_ENERGY);
        event.add(EntityType.PLAYER, MainAttributes.MAX_STAMINA);
        event.add(EntityType.PLAYER, MainAttributes.MAX_POISE);
        event.add(EntityType.PLAYER, MainAttributes.MELEE_DAMAGE);
        event.add(EntityType.PLAYER, MainAttributes.STRIKE_DAMAGE);
        event.add(EntityType.PLAYER, MainAttributes.DEFENSE);
		event.add(EntityType.PLAYER, MainAttributes.CRIT_CHANCE);
		event.add(EntityType.PLAYER, MainAttributes.CRIT_DAMAGE);
    }

	@SubscribeEvent
	public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MainBlockEntities.KIKONO_STATION_BE.get(), (be, side) -> be.getItemHandler());
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MainBlockEntities.KIKONO_STATION_BE.get(), (be, side) -> be.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MainBlockEntities.FUEL_GENERATOR_BE.get(), (be, side) -> be.getItemHandler());
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MainBlockEntities.FUEL_GENERATOR_BE.get(), (be, side) -> be.getEnergyStorage());
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MainBlockEntities.ENERGY_CABLE_BE.get(), (be, side) -> be.getEnergyStorage());
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MainBlockEntities.GRAVITY_DEVICE_BE.get(), (be, side) -> be.getEnergyStorage());
	}

    @SafeVarargs
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void regAttr(EntityAttributeCreationEvent event, AttributeSupplier attributes, DeferredHolder... entities) {
        for (DeferredHolder reg : entities) {
            event.put((EntityType) reg.get(), attributes);
        }
    }
}
