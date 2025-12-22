package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.*;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.masters.*;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import com.dragonminez.common.init.entities.redribbon.BanditEntity;
import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;
import com.dragonminez.common.init.entities.redribbon.RedRibbonSoldierEntity;
import com.dragonminez.common.init.entities.redribbon.RobotEntity;
import com.dragonminez.common.init.entities.sagas.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MainEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Reference.MOD_ID);

    public static final RegistryObject<EntityType<MasterKarinEntity>> MASTER_KARIN =
            ENTITY_TYPES.register("master_karin",
                    () -> EntityType.Builder.of(MasterKarinEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 0.8f)
                            .build(new ResourceLocation(Reference.MOD_ID, "master_karin").toString()));
    public static final RegistryObject<EntityType<MasterGokuEntity>> MASTER_GOKU =
            ENTITY_TYPES.register("master_goku",
                    () -> EntityType.Builder.of(MasterGokuEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(new ResourceLocation(Reference.MOD_ID, "master_goku").toString()));
    public static final RegistryObject<EntityType<MasterKaiosamaEntity>> MASTER_KAIOSAMA =
            ENTITY_TYPES.register("master_kaiosama",
                    () -> EntityType.Builder.of(MasterKaiosamaEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.5f)
                            .build(new ResourceLocation(Reference.MOD_ID, "master_kaiosama").toString()));
    public static final RegistryObject<EntityType<MasterRoshiEntity>> MASTER_ROSHI =
            ENTITY_TYPES.register("master_roshi",
                    () -> EntityType.Builder.of(MasterRoshiEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.8f)
                            .build(new ResourceLocation(Reference.MOD_ID, "master_roshi").toString()));
    public static final RegistryObject<EntityType<MasterUranaiEntity>> MASTER_URANAI =
            ENTITY_TYPES.register("master_uranai",
                    () -> EntityType.Builder.of(MasterUranaiEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.4f)
                            .build(new ResourceLocation(Reference.MOD_ID, "master_uranai").toString()));
    public static final RegistryObject<EntityType<MasterEnmaEntity>> MASTER_ENMA =
            ENTITY_TYPES.register("master_enma",
                    () -> EntityType.Builder.of(MasterEnmaEntity::new, MobCategory.CREATURE)
                            .sized(5.5f, 7.5f)
                            .build(new ResourceLocation(Reference.MOD_ID, "master_enma").toString()));

    public static final RegistryObject<EntityType<Dino1Entity>> DINOSAUR1 =
            ENTITY_TYPES.register("dino1",
                    () -> EntityType.Builder.of(Dino1Entity::new, MobCategory.CREATURE)
                            .sized(2.2f, 5.1f)
                            .build(new ResourceLocation(Reference.MOD_ID, "dino1").toString()));
    public static final RegistryObject<EntityType<Dino2Entity>> DINOSAUR2 =
            ENTITY_TYPES.register("dino2",
                    () -> EntityType.Builder.of(Dino2Entity::new, MobCategory.CREATURE)
                            .sized(3.3f, 5.2f)
                            .build(new ResourceLocation(Reference.MOD_ID, "dino2").toString()));
    public static final RegistryObject<EntityType<DinoFlyEntity>> DINOSAUR3 =
            ENTITY_TYPES.register("dino3",
                    () -> EntityType.Builder.of(DinoFlyEntity::new, MobCategory.CREATURE)
                            .sized(1.8f, 1.5f)
                            .build(new ResourceLocation(Reference.MOD_ID, "dino3").toString()));
    public static final RegistryObject<EntityType<Dino1Entity>> DINO_KID =
            ENTITY_TYPES.register("dinokid",
                    () -> EntityType.Builder.of(Dino1Entity::new, MobCategory.CREATURE)
                            .sized(1.0f, 1.0f)
                            .build(new ResourceLocation(Reference.MOD_ID, "dinokid").toString()));
    public static final RegistryObject<EntityType<NamekFrogEntity>> NAMEK_FROG =
            ENTITY_TYPES.register("namek_frog",
                    () -> EntityType.Builder.of(NamekFrogEntity::new, MobCategory.CREATURE)
                            .sized(0.4f, 0.4f)
                            .build(new ResourceLocation(Reference.MOD_ID, "namek_frog").toString()));
    public static final RegistryObject<EntityType<NamekFrogGinyuEntity>> NAMEK_FROG_GINYU =
            ENTITY_TYPES.register("namek_frog_ginyu",
                    () -> EntityType.Builder.of(NamekFrogGinyuEntity::new, MobCategory.CREATURE)
                            .sized(0.4f, 0.4f)
                            .build(new ResourceLocation(Reference.MOD_ID, "namek_frog_ginyu").toString()));

    public static final RegistryObject<EntityType<BanditEntity>> BANDIT =
            ENTITY_TYPES.register("bandit",
                    () -> EntityType.Builder.of(BanditEntity::new, MobCategory.CREATURE)
                            .sized(1.4f, 3.2f)
                            .build(new ResourceLocation(Reference.MOD_ID, "bandit").toString()));
    public static final RegistryObject<EntityType<RobotEntity>> RED_RIBBON_ROBOT1 =
            ENTITY_TYPES.register("robot1",
                    () -> EntityType.Builder.of(RobotEntity::new, MobCategory.CREATURE)
                            .sized(1.7f, 4.5f)
                            .build(new ResourceLocation(Reference.MOD_ID, "robot1").toString()));
    public static final RegistryObject<EntityType<RobotEntity>> RED_RIBBON_ROBOT2 =
            ENTITY_TYPES.register("robot2",
                    () -> EntityType.Builder.of(RobotEntity::new, MobCategory.CREATURE)
                            .sized(1.7f, 4.5f)
                            .build(new ResourceLocation(Reference.MOD_ID, "robot2").toString()));
    public static final RegistryObject<EntityType<RobotEntity>> RED_RIBBON_ROBOT3 =
            ENTITY_TYPES.register("robot3",
                    () -> EntityType.Builder.of(RobotEntity::new, MobCategory.CREATURE)
                            .sized(1.7f, 4.5f)
                            .build(new ResourceLocation(Reference.MOD_ID, "robot3").toString()));
    public static final RegistryObject<EntityType<RedRibbonSoldierEntity>> RED_RIBBON_SOLDIER =
            ENTITY_TYPES.register("red_ribbon_soldier",
                    () -> EntityType.Builder.of(RedRibbonSoldierEntity::new, MobCategory.CREATURE)
                            .sized(1.0f, 2.0f)
                            .build(new ResourceLocation(Reference.MOD_ID, "red_ribbon_soldier").toString()));
    public static final RegistryObject<EntityType<NamekTraderEntity>> NAMEK_TRADER =
            ENTITY_TYPES.register("namek_trader",
                    () -> EntityType.Builder.of(NamekTraderEntity::new, MobCategory.CREATURE)
                            .sized(1.0f, 2.0f)
                            .build(new ResourceLocation(Reference.MOD_ID, "namek_trader").toString()));
    public static final RegistryObject<EntityType<NamekWarriorEntity>> NAMEK_WARRIOR =
            ENTITY_TYPES.register("namek_warrior",
                    () -> EntityType.Builder.of(NamekWarriorEntity::new, MobCategory.CREATURE)
                            .sized(1.0f, 2.0f)
                            .build(new ResourceLocation(Reference.MOD_ID, "namek_warrior").toString()));

    public static final RegistryObject<EntityType<SagaSaibamanEntity>> SAGA_SAIBAMAN =
            ENTITY_TYPES.register("saga_saibaman1",
                    () -> EntityType.Builder.of(SagaSaibamanEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.6f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_saibaman1").toString()));
    public static final RegistryObject<EntityType<SagaSaibamanEntity>> SAGA_SAIBAMAN2 =
            ENTITY_TYPES.register("saga_saibaman2",
                    () -> EntityType.Builder.of(SagaSaibamanEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.6f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_saibaman2").toString()));
    public static final RegistryObject<EntityType<SagaSaibamanEntity>> SAGA_SAIBAMAN3 =
            ENTITY_TYPES.register("saga_saibaman3",
                    () -> EntityType.Builder.of(SagaSaibamanEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.6f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_saibaman3").toString()));
    public static final RegistryObject<EntityType<SagaSaibamanEntity>> SAGA_SAIBAMAN4 =
            ENTITY_TYPES.register("saga_saibaman4",
                    () -> EntityType.Builder.of(SagaSaibamanEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.6f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_saibaman4").toString()));
    public static final RegistryObject<EntityType<SagaSaibamanEntity>> SAGA_SAIBAMAN5 =
            ENTITY_TYPES.register("saga_saibaman5",
                    () -> EntityType.Builder.of(SagaSaibamanEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.6f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_saibaman5").toString()));
    public static final RegistryObject<EntityType<SagaSaibamanEntity>> SAGA_SAIBAMAN6 =
            ENTITY_TYPES.register("saga_saibaman6",
                    () -> EntityType.Builder.of(SagaSaibamanEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.6f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_saibaman6").toString()));
    public static final RegistryObject<EntityType<SagaRaditzEntity>> SAGA_RADITZ =
            ENTITY_TYPES.register("saga_raditz",
                    () -> EntityType.Builder.of(SagaRaditzEntity::new, MobCategory.CREATURE)
                            .sized(1.0f, 2.0f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_raditz").toString()));
    public static final RegistryObject<EntityType<SagaNappaEntity>> SAGA_NAPPA =
            ENTITY_TYPES.register("saga_nappa",
                    () -> EntityType.Builder.of(SagaNappaEntity::new, MobCategory.CREATURE)
                            .sized(1.5f, 2.8f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_nappa").toString()));
    public static final RegistryObject<EntityType<SagaVegetaEntity>> SAGA_VEGETA =
            ENTITY_TYPES.register("saga_vegeta",
                    () -> EntityType.Builder.of(SagaVegetaEntity::new, MobCategory.CREATURE)
                            .sized(1.0f, 2.0f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_vegeta").toString()));
    public static final RegistryObject<EntityType<SagaOzaruVegetaEntity>> SAGA_OZARU_VEGETA =
            ENTITY_TYPES.register("saga_ozaruvegeta",
                    () -> EntityType.Builder.of(SagaOzaruVegetaEntity::new, MobCategory.CREATURE)
                            .sized(3.0f, 6.5f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_ozaruvegeta").toString()));
    public static final RegistryObject<EntityType<SagaFriezaSoldier01Entity>> SAGA_FRIEZA_SOLDIER =
            ENTITY_TYPES.register("saga_friezasoldier01",
                    () -> EntityType.Builder.of(SagaFriezaSoldier01Entity::new, MobCategory.CREATURE)
                            .sized(1.0f, 2.0f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_friezasoldier01").toString()));
    public static final RegistryObject<EntityType<SagaFriezaSoldier02Entity>> SAGA_FRIEZA_SOLDIER2 =
            ENTITY_TYPES.register("saga_friezasoldier02",
                    () -> EntityType.Builder.of(SagaFriezaSoldier02Entity::new, MobCategory.CREATURE)
                            .sized(1.0f, 2.0f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_friezasoldier02").toString()));
    public static final RegistryObject<EntityType<SagaFriezaSoldier02Entity>> SAGA_FRIEZA_SOLDIER3 =
            ENTITY_TYPES.register("saga_friezasoldier03",
                    () -> EntityType.Builder.of(SagaFriezaSoldier02Entity::new, MobCategory.CREATURE)
                            .sized(1.0f, 2.0f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_friezasoldier03").toString()));
    public static final RegistryObject<EntityType<SagaFriezaSoldier01Entity>> SAGA_MORO_SOLDIER =
            ENTITY_TYPES.register("saga_morosoldier",
                    () -> EntityType.Builder.of(SagaFriezaSoldier01Entity::new, MobCategory.CREATURE)
                            .sized(1.0f, 2.0f)
                            .build(new ResourceLocation(Reference.MOD_ID, "saga_morosoldier").toString()));

    public static final RegistryObject<EntityType<KiBlastEntity>> KI_BLAST = ENTITY_TYPES.register("ki_blast",
            () -> EntityType.Builder.<KiBlastEntity>of(KiBlastEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("ki_blast"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
