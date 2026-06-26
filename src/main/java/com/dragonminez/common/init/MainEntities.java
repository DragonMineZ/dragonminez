package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.*;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import com.dragonminez.common.init.entities.animal.*;
import com.dragonminez.common.init.entities.ki.*;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.common.init.entities.redribbon.BanditEntity;
import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;
import com.dragonminez.common.init.entities.redribbon.RedRibbonSoldierEntity;
import com.dragonminez.common.init.entities.redribbon.RobotEntity;
import com.dragonminez.common.init.entities.sagas.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MainEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Reference.MOD_ID);

    private static final Map<String, RegistryObject<EntityType<DragonWishEntity>>> DRAGON_WISH_ENTITIES = registerDragonWishEntities();

    public static final RegistryObject<EntityType<DragonWishEntity>> SHENRON = getDragonWishEntityOrThrow("shenron");
    public static final RegistryObject<EntityType<DragonWishEntity>> PORUNGA = getDragonWishEntityOrThrow("porunga");

    public static List<RegistryObject<? extends EntityType<?>>> getMasterEntities() {
        return List.of(
                MASTER_KARIN, MASTER_GOKU, MASTER_KAIOSAMA, MASTER_ROSHI, MASTER_URANAI, MASTER_ENMA, MASTER_DENDE,
                MASTER_GERO, MASTER_POPO, MASTER_GURU, MASTER_TORIBOT, MASTER_PICCOLO, MASTER_GOHAN, MASTER_BABIDI,
                MASTER_OLDKAI, MASTER_CELL, MASTER_VEGETA, MASTER_FRIEZA, MASTER_TRUNKS, MASTER_YAMCHA, MASTER_KRILLIN,
                MASTER_BEERUS, MASTER_WHIS
        );
    }

    public static List<RegistryObject<? extends EntityType<? extends Mob>>> getSagaEntities() {
        return List.of(
                // SAGA SAIYAN
                SAGA_GOKU_EARLY, SAGA_GOKU_EARLY_NOWEIGHTS, SAGA_PICCOLO_EARLY, SAGA_CHAOZ,
                SAGA_SAIBAMAN, SAGA_SAIBAMAN2, SAGA_SAIBAMAN3, SAGA_SAIBAMAN4, SAGA_SAIBAMAN5, SAGA_SAIBAMAN6,
                SAGA_RADITZ, SAGA_NAPPA, SAGA_VEGETA, SAGA_OZARU_VEGETA, SAGA_OZARU,

                // SAGA FRIEZA
                SAGA_FRIEZA_SOLDIER, SAGA_FRIEZA_SOLDIER2, SAGA_FRIEZA_SOLDIER3, SAGA_MORO_SOLDIER,
                SAGA_CUI, SAGA_DODORIA, SAGA_VEGETA_NAMEK, SAGA_ZARBON, SAGA_ZARBON_TRANSF,
                SAGA_GULDO, SAGA_RECOOME, SAGA_BURTER, SAGA_JEICE, SAGA_GINYU, SAGA_GINYU_GOKU, SAGA_NAIL,
                SAGA_FREEZER_FIRST, SAGA_FREEZER_SECOND, SAGA_FREEZER_THIRD, SAGA_FREEZER_BASE, SAGA_FREEZER_FP,
                SAGA_KID_GOHAN, SAGA_KRILLIN, SAGA_TIEN_EARLY, SAGA_YAMCHA, SAGA_GOKU_MID_BASE, SAGA_GOKU_MID_SSJ,

                // SAGA ANDROIDES / CELL
                SAGA_MECHA_FRIEZA, SAGA_KING_COLD, SAGA_DRGERO, SAGA_A19, SAGA_A18, SAGA_A17, SAGA_A16,
                SAGA_CELL_IMPERFECT, SAGA_PICCOLO_KAMI, SAGA_CELL_SEMIPERFECT,
                SAGA_VEGETA_MID, SAGA_VEGETA_MID_SSJ, SAGA_VEGETA_MID_SSG2,
                SAGA_FUTURE_TRUNKS_KID_BASE, SAGA_FUTURE_TRUNKS_KID_SSJ, SAGA_FUTURE_TRUNKS_BASE, SAGA_FUTURE_TRUNKS_SSJ, SAGA_FUTURE_TRUNKS_SSG3,
                SAGA_CELL_PERFECT, SAGA_GOHAN_MID_BASE, SAGA_GOHAN_MID_SSJ, SAGA_GOHAN_MID_SSJ2,
                SAGA_FUTURE_GOHAN_BASE, SAGA_FUTURE_GOHAN_SSJ, SAGA_CELL_SUPERPERFECT, SAGA_CELL_JR,

                // SAGA MAJIN BUU
                SAGA_GOKU_END_BASE, SAGA_GOKU_END_SSJ, SAGA_GOKU_END_SSJ2, SAGA_GOKU_END_SSJ3,
                SAGA_VEGETA_END_BASE, SAGA_VEGETA_END_SSJ, SAGA_VEGETA_END_SSJ2, SAGA_VEGETA_MAJIN,
                SAGA_GOHAN_END_BASE, SAGA_GOHAN_END_SSJ, SAGA_GOHAN_END_SSJ2, SAGA_GOHAN_END_ULTIMATE,
                SAGA_GOTEN, SAGA_GOTEN_SSJ, SAGA_KID_TRUNKS, SAGA_KID_TRUNKS_SSJ,
                SAGA_GOTENKS, SAGA_GOTENKS_SSJ, SAGA_GOTENKS_SSJ3,
                SAGA_SHIN, SAGA_VIDEL, SAGA_BULMA, SAGA_KIBITO,
                SAGA_SPOPOVITCH, SAGA_PUIPUI, SAGA_YAKON, SAGA_DABURA, SAGA_BABIDI,
                SAGA_BUU_FAT, SAGA_EVILBUU, SAGA_SUPERBUU, SAGA_SUPERBUU_PICCOLO, SAGA_SUPERBUU_GOTENKS, SAGA_SUPERBUU_GOHAN, SAGA_KIDBUU,

                // PELÍCULAS
                SAGA_GARLICK_JR, SAGA_GARLICK_JR_TRANSFORMED, SAGA_DR_WHEELO, SAGA_TURLES,
                SAGA_SLUG_SOLDIER, SAGA_SLUG, SAGA_SLUG_GIANT,
                SAGA_DORE, SAGA_SALZA, SAGA_NEIZ, SAGA_COOLER, SAGA_COOLER_5TA, SAGA_GETE_ROBOT, SAGA_METAL_COOLER, SAGA_METAL_COOLER_CORE,
                SAGA_A14, SAGA_A15, SAGA_A13, SAGA_SUPER_A13,
                SAGA_PARAGUS, SAGA_BROLY_BASE, SAGA_BROLY_SSJ_RESTRICTED, SAGA_BROLY_SSJ, SAGA_BROLY_LSSJ,
                SAGA_ZANGYA, SAGA_GOKUA, SAGA_BIDO, SAGA_BUJIN, SAGA_BOJACK, SAGA_BOJACK_FP,
                SAGA_BIO_BROLY, SAGA_BIO_BROLY_GIANT, SAGA_PAIKUHAN, SAGA_JANEMBA_FAT, SAGA_SUPER_JANEMBA,
                SAGA_HIRUDEGARN, SAGA_SUPER_HIRUDEGARN,

                // EXTRA
                SHADOW_DUMMY, MINI_BUU
        );
    }

private static Map<String, RegistryObject<EntityType<DragonWishEntity>>> registerDragonWishEntities() {
    Map<String, RegistryObject<EntityType<DragonWishEntity>>> registered = new LinkedHashMap<>();
    for (DragonDefinition definition : DragonBallDefinitions.getBootstrapDragons()) {
        RegistryObject<EntityType<DragonWishEntity>> entity = ENTITY_TYPES.register(definition.getEntityRegistryName(),
                () -> EntityType.Builder.<DragonWishEntity>of((type, level) -> new DragonWishEntity(type, level, definition.getId()), MobCategory.CREATURE)
                        .sized(definition.getEntityWidth(), definition.getEntityHeight())
                        .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, definition.getEntityRegistryName()).toString()));
        registered.put(definition.getId(), entity);
    }
    return Map.copyOf(registered);
}

public static RegistryObject<EntityType<DragonWishEntity>> getDragonWishEntityOrThrow(String dragonId) {
    RegistryObject<EntityType<DragonWishEntity>> entity = DRAGON_WISH_ENTITIES.get(dragonId);
    if (entity == null) {
        throw new IllegalArgumentException("No dragon wish entity registered for dragon '" + dragonId + "'");
    }
    return entity;
}

public static Map<String, RegistryObject<EntityType<DragonWishEntity>>> getDragonWishEntities() {
    return DRAGON_WISH_ENTITIES;
}

    public static final RegistryObject<EntityType<AllMastersEntity.MasterKarinEntity>> MASTER_KARIN =
            ENTITY_TYPES.register("master_karin",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterKarinEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 0.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_karin").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterGokuEntity>> MASTER_GOKU =
            ENTITY_TYPES.register("master_goku",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterGokuEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_goku").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterKaiosamaEntity>> MASTER_KAIOSAMA =
            ENTITY_TYPES.register("master_kaiosama",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterKaiosamaEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_kaiosama").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterRoshiEntity>> MASTER_ROSHI =
            ENTITY_TYPES.register("master_roshi",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterRoshiEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_roshi").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterUranaiEntity>> MASTER_URANAI =
            ENTITY_TYPES.register("master_uranai",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterUranaiEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_uranai").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterEnmaEntity>> MASTER_ENMA =
            ENTITY_TYPES.register("master_enma",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterEnmaEntity::new, MobCategory.CREATURE)
                            .sized(5.5f, 7.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_enma").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterDendeEntity>> MASTER_DENDE =
            ENTITY_TYPES.register("master_dende",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterDendeEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_dende").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterGeroEntity>> MASTER_GERO =
            ENTITY_TYPES.register("master_gero",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterGeroEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_gero").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterPopoEntity>> MASTER_POPO =
            ENTITY_TYPES.register("master_popo",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterPopoEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_popo").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterGuruEntity>> MASTER_GURU =
            ENTITY_TYPES.register("master_guru",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterGuruEntity::new, MobCategory.CREATURE)
                            .sized(1.3f, 3.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_guru").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterToribotEntity>> MASTER_TORIBOT =
            ENTITY_TYPES.register("master_toribot",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterToribotEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_toribot").toString()));

    public static final RegistryObject<EntityType<AllMastersEntity.MasterPiccolo>> MASTER_PICCOLO =
            ENTITY_TYPES.register("master_piccolo",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterPiccolo::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_piccolo").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterGohan>> MASTER_GOHAN =
            ENTITY_TYPES.register("master_gohan",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterGohan::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_gohan").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterBabidi>> MASTER_BABIDI =
            ENTITY_TYPES.register("master_babidi",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterBabidi::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.2f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_babidi").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterOldKai>> MASTER_OLDKAI =
            ENTITY_TYPES.register("master_oldkai",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterOldKai::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_oldkai").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterCell>> MASTER_CELL =
            ENTITY_TYPES.register("master_cell",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterCell::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_cell").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterVegeta>> MASTER_VEGETA =
            ENTITY_TYPES.register("master_vegeta",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterVegeta::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_vegeta").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterFrieza>> MASTER_FRIEZA =
            ENTITY_TYPES.register("master_frieza",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterFrieza::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_frieza").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterTrunks>> MASTER_TRUNKS =
            ENTITY_TYPES.register("master_trunks",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterTrunks::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_trunks").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterYamcha>> MASTER_YAMCHA =
            ENTITY_TYPES.register("master_yamcha",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterYamcha::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_yamcha").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterKrillin>> MASTER_KRILLIN =
            ENTITY_TYPES.register("master_krillin",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterKrillin::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_krillin").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterBeerus>> MASTER_BEERUS =
            ENTITY_TYPES.register("master_beerus",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterBeerus::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_beerus").toString()));
    public static final RegistryObject<EntityType<AllMastersEntity.MasterWhis>> MASTER_WHIS =
            ENTITY_TYPES.register("master_whis",
                    () -> EntityType.Builder.of(AllMastersEntity.MasterWhis::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "master_whis").toString()));

    public static final RegistryObject<EntityType<Dino1Entity>> DINOSAUR1 =
            ENTITY_TYPES.register("dino1",
                    () -> EntityType.Builder.of(Dino1Entity::new, MobCategory.MONSTER)
                            .sized(2.2f, 5.1f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "dino1").toString()));
    public static final RegistryObject<EntityType<Dino2Entity>> DINOSAUR2 =
            ENTITY_TYPES.register("dino2",
                    () -> EntityType.Builder.of(Dino2Entity::new, MobCategory.MONSTER)
                            .sized(3.3f, 5.2f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "dino2").toString()));
    public static final RegistryObject<EntityType<DinoFlyEntity>> DINOSAUR3 =
            ENTITY_TYPES.register("dino3",
                    () -> EntityType.Builder.of(DinoFlyEntity::new, MobCategory.MONSTER)
                            .sized(1.8f, 1.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "dino3").toString()));
    public static final RegistryObject<EntityType<Dino1Entity>> DINO_KID =
            ENTITY_TYPES.register("dinokid",
                    () -> EntityType.Builder.of(Dino1Entity::new, MobCategory.MONSTER)
                            .sized(1.0f, 1.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "dinokid").toString()));
    public static final RegistryObject<EntityType<SabertoothEntity>> SABERTOOTH =
            ENTITY_TYPES.register("sabertooth",
                    () -> EntityType.Builder.of(SabertoothEntity::new, MobCategory.MONSTER)
                            .sized(1.8f, 1.2f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "sabertooth").toString()));
    public static final RegistryObject<EntityType<NamekFrogEntity>> NAMEK_FROG =
            ENTITY_TYPES.register("namek_frog",
                    () -> EntityType.Builder.of(NamekFrogEntity::new, MobCategory.AMBIENT)
                            .sized(0.4f, 0.4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "namek_frog").toString()));
    public static final RegistryObject<EntityType<NamekFrogGinyuEntity>> NAMEK_FROG_GINYU =
            ENTITY_TYPES.register("namek_frog_ginyu",
                    () -> EntityType.Builder.of(NamekFrogGinyuEntity::new, MobCategory.AMBIENT)
                            .sized(0.4f, 0.4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "namek_frog_ginyu").toString()));

    public static final RegistryObject<EntityType<BanditEntity>> BANDIT =
            ENTITY_TYPES.register("bandit",
                    () -> EntityType.Builder.of(BanditEntity::new, MobCategory.MONSTER)
                            .sized(1.4f, 3.2f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "bandit").toString()));
    public static final RegistryObject<EntityType<RobotEntity>> RED_RIBBON_ROBOT1 =
            ENTITY_TYPES.register("robot1",
                    () -> EntityType.Builder.of(RobotEntity::new, MobCategory.MONSTER)
                            .sized(1.7f, 4.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "robot1").toString()));
    public static final RegistryObject<EntityType<RobotEntity>> RED_RIBBON_ROBOT2 =
            ENTITY_TYPES.register("robot2",
                    () -> EntityType.Builder.of(RobotEntity::new, MobCategory.MONSTER)
                            .sized(1.7f, 4.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "robot2").toString()));
    public static final RegistryObject<EntityType<RobotEntity>> RED_RIBBON_ROBOT3 =
            ENTITY_TYPES.register("robot3",
                    () -> EntityType.Builder.of(RobotEntity::new, MobCategory.MONSTER)
                            .sized(1.7f, 4.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "robot3").toString()));
    public static final RegistryObject<EntityType<RedRibbonSoldierEntity>> RED_RIBBON_SOLDIER =
            ENTITY_TYPES.register("red_ribbon_soldier",
                    () -> EntityType.Builder.of(RedRibbonSoldierEntity::new, MobCategory.MONSTER)
                            .sized(1.0f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "red_ribbon_soldier").toString()));
    public static final RegistryObject<EntityType<NamekTraderEntity>> NAMEK_TRADER =
            ENTITY_TYPES.register("namek_trader",
                    () -> EntityType.Builder.of(NamekTraderEntity::new, MobCategory.CREATURE)
                            .sized(1.0f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "namek_trader").toString()));
    public static final RegistryObject<EntityType<NamekWarriorEntity>> NAMEK_WARRIOR =
            ENTITY_TYPES.register("namek_warrior",
                    () -> EntityType.Builder.of(NamekWarriorEntity::new, MobCategory.CREATURE)
                            .sized(1.0f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "namek_warrior").toString()));
    public static final RegistryObject<EntityType<SpacePodEntity>> SPACE_POD =
            ENTITY_TYPES.register("spacepod",
                    () -> EntityType.Builder.of(SpacePodEntity::new, MobCategory.CREATURE)
                            .sized(2.0f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "spacepod").toString()));
    public static final RegistryObject<EntityType<FlyingNimbusEntity>> FLYING_NIMBUS =
            ENTITY_TYPES.register("flying_nimbus",
                    () -> EntityType.Builder.of(FlyingNimbusEntity::new, MobCategory.CREATURE)
                            .sized(2.0f, 1.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "flying_nimbus").toString()));
    public static final RegistryObject<EntityType<BlackNimbusEntity>> BLACK_NIMBUS =
            ENTITY_TYPES.register("black_nimbus",
                    () -> EntityType.Builder.of(BlackNimbusEntity::new, MobCategory.CREATURE)
                            .sized(2.0f, 1.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "black_nimbus").toString()));
    public static final RegistryObject<EntityType<RobotEntity>> ROBOT_XENOVERSE =
            ENTITY_TYPES.register("robotxv",
                    () -> EntityType.Builder.of(RobotEntity::new, MobCategory.CREATURE)
                            .sized(1.5f, 1.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "robotxv").toString()));
    public static final RegistryObject<EntityType<PunchMachineEntity>> PUNCH_MACHINE =
            ENTITY_TYPES.register("punch_machine",
                    () -> EntityType.Builder.of(PunchMachineEntity::new, MobCategory.CREATURE)
                            .sized(1.5f, 1.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "punch_machine").toString()));
    public static final RegistryObject<EntityType<MajinSkillEntity>> MAJIN_SKILL =
            ENTITY_TYPES.register("majin_skill",
                    () -> EntityType.Builder.of(MajinSkillEntity::new, MobCategory.CREATURE)
                            .sized(0.5f, 0.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "majin_skill").toString()));

    // SAGAS ENTITY
    public static final RegistryObject<EntityType<SagaGokuEntity.SagaGokuEarlyEntity>> SAGA_GOKU_EARLY =
            ENTITY_TYPES.register("saga_goku_early",
                    () -> EntityType.Builder.of(SagaGokuEntity.SagaGokuEarlyEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_goku_early").toString()));
    public static final RegistryObject<EntityType<SagaGokuEntity.SagaGokuEarlyEntity>> SAGA_GOKU_EARLY_NOWEIGHTS =
            ENTITY_TYPES.register("saga_goku_early_noweights",
                    () -> EntityType.Builder.of(SagaGokuEntity.SagaGokuEarlyEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_goku_early_noweights").toString()));
    public static final RegistryObject<EntityType<SagaPiccoloEntity.SagaPiccoloEarlyEntity>> SAGA_PICCOLO_EARLY =
            ENTITY_TYPES.register("saga_piccolo",
                    () -> EntityType.Builder.of(SagaPiccoloEntity.SagaPiccoloEarlyEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_piccolo").toString()));
    public static final RegistryObject<EntityType<SagaZFightersEntity.ChaozEntity>> SAGA_CHAOZ =
            ENTITY_TYPES.register("saga_chaoz",
                    () -> EntityType.Builder.of(SagaZFightersEntity.ChaozEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 1.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_chaoz").toString()));

    public static final RegistryObject<EntityType<SagaSaibamanEntity>> SAGA_SAIBAMAN =
            ENTITY_TYPES.register("saga_saibaman1",
                    () -> EntityType.Builder.of(SagaSaibamanEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_saibaman1").toString()));
    public static final RegistryObject<EntityType<SagaSaibamanEntity>> SAGA_SAIBAMAN2 =
            ENTITY_TYPES.register("saga_saibaman2",
                    () -> EntityType.Builder.of(SagaSaibamanEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_saibaman2").toString()));
    public static final RegistryObject<EntityType<SagaSaibamanEntity>> SAGA_SAIBAMAN3 =
            ENTITY_TYPES.register("saga_saibaman3",
                    () -> EntityType.Builder.of(SagaSaibamanEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_saibaman3").toString()));
    public static final RegistryObject<EntityType<SagaSaibamanEntity>> SAGA_SAIBAMAN4 =
            ENTITY_TYPES.register("saga_saibaman4",
                    () -> EntityType.Builder.of(SagaSaibamanEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_saibaman4").toString()));
    public static final RegistryObject<EntityType<SagaSaibamanEntity>> SAGA_SAIBAMAN5 =
            ENTITY_TYPES.register("saga_saibaman5",
                    () -> EntityType.Builder.of(SagaSaibamanEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_saibaman5").toString()));
    public static final RegistryObject<EntityType<SagaSaibamanEntity>> SAGA_SAIBAMAN6 =
            ENTITY_TYPES.register("saga_saibaman6",
                    () -> EntityType.Builder.of(SagaSaibamanEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_saibaman6").toString()));
    public static final RegistryObject<EntityType<SagaRaditzEntity>> SAGA_RADITZ =
            ENTITY_TYPES.register("saga_raditz",
                    () -> EntityType.Builder.of(SagaRaditzEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_raditz").toString()));
    public static final RegistryObject<EntityType<SagaNappaEntity>> SAGA_NAPPA =
            ENTITY_TYPES.register("saga_nappa",
                    () -> EntityType.Builder.of(SagaNappaEntity::new, MobCategory.MONSTER)
                            .sized(1.3f, 2.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_nappa").toString()));
    public static final RegistryObject<EntityType<SagaVegetaEntity.SagaVegetaExplorerEntity>> SAGA_VEGETA =
            ENTITY_TYPES.register("saga_vegeta",
                    () -> EntityType.Builder.of(SagaVegetaEntity.SagaVegetaExplorerEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_vegeta").toString()));
    public static final RegistryObject<EntityType<SagaOzaruEntity>> SAGA_OZARU_VEGETA =
            ENTITY_TYPES.register("saga_ozaruvegeta",
                    () -> EntityType.Builder.of(SagaOzaruEntity::new, MobCategory.MONSTER)
                            .sized(6.5f, 10.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_ozaruvegeta").toString()));
    public static final RegistryObject<EntityType<SagaOzaruEntity>> SAGA_OZARU =
            ENTITY_TYPES.register("saga_ozaru",
                    () -> EntityType.Builder.of(SagaOzaruEntity::new, MobCategory.MONSTER)
                            .sized(6.5f, 10.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_ozaru").toString()));

    /*
    FRIEZA SAGA ENTITIES
    */

    public static final RegistryObject<EntityType<SagaFriezaSoldier01Entity>> SAGA_FRIEZA_SOLDIER =
            ENTITY_TYPES.register("saga_friezasoldier01",
                    () -> EntityType.Builder.of(SagaFriezaSoldier01Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_friezasoldier01").toString()));
    public static final RegistryObject<EntityType<SagaFriezaSoldier02Entity>> SAGA_FRIEZA_SOLDIER2 =
            ENTITY_TYPES.register("saga_friezasoldier02",
                    () -> EntityType.Builder.of(SagaFriezaSoldier02Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_friezasoldier02").toString()));
    public static final RegistryObject<EntityType<SagaFriezaSoldier02Entity>> SAGA_FRIEZA_SOLDIER3 =
            ENTITY_TYPES.register("saga_friezasoldier03",
                    () -> EntityType.Builder.of(SagaFriezaSoldier02Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_friezasoldier03").toString()));
    public static final RegistryObject<EntityType<SagaFriezaSoldier01Entity>> SAGA_MORO_SOLDIER =
            ENTITY_TYPES.register("saga_morosoldier",
                    () -> EntityType.Builder.of(SagaFriezaSoldier01Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_morosoldier").toString()));
    public static final RegistryObject<EntityType<SagaCuiEntity>> SAGA_CUI =
            ENTITY_TYPES.register("saga_cui",
                    () -> EntityType.Builder.of(SagaCuiEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_cui").toString()));
    public static final RegistryObject<EntityType<SagaDodoriaEntity>> SAGA_DODORIA =
            ENTITY_TYPES.register("saga_dodoria",
                    () -> EntityType.Builder.of(SagaDodoriaEntity::new, MobCategory.MONSTER)
                            .sized(1.0f, 2.2f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_dodoria").toString()));
    public static final RegistryObject<EntityType<SagaVegetaEntity.SagaVegetaNamekEntity>> SAGA_VEGETA_NAMEK =
            ENTITY_TYPES.register("saga_vegeta_namek",
                    () -> EntityType.Builder.of(SagaVegetaEntity.SagaVegetaNamekEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_vegeta_namek").toString()));
    public static final RegistryObject<EntityType<SagaZarbonEntity>> SAGA_ZARBON =
            ENTITY_TYPES.register("saga_zarbon",
                    () -> EntityType.Builder.of(SagaZarbonEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_zarbon").toString()));
    public static final RegistryObject<EntityType<SagaZarbonEntity.SagaZarbonT1Entity>> SAGA_ZARBON_TRANSF =
            ENTITY_TYPES.register("saga_zarbont1",
                    () -> EntityType.Builder.of(SagaZarbonEntity.SagaZarbonT1Entity::new, MobCategory.MONSTER)
                            .sized(1.3f, 2.1f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_zarbont1").toString()));
    public static final RegistryObject<EntityType<SagaGinyuForcesEntity.SagaGuldoEntity>> SAGA_GULDO =
            ENTITY_TYPES.register("saga_guldo",
                    () -> EntityType.Builder.of(SagaGinyuForcesEntity.SagaGuldoEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_guldo").toString()));
    public static final RegistryObject<EntityType<SagaGinyuForcesEntity.SagaRecoomeEntity>> SAGA_RECOOME =
            ENTITY_TYPES.register("saga_recoome",
                    () -> EntityType.Builder.of(SagaGinyuForcesEntity.SagaRecoomeEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 2.1f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_recoome").toString()));
    public static final RegistryObject<EntityType<SagaGinyuForcesEntity.SagaBurterEntity>> SAGA_BURTER =
            ENTITY_TYPES.register("saga_burter",
                    () -> EntityType.Builder.of(SagaGinyuForcesEntity.SagaBurterEntity::new, MobCategory.MONSTER)
                            .sized(1.2f, 2.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_burter").toString()));
    public static final RegistryObject<EntityType<SagaGinyuForcesEntity.SagaJeiceEntity>> SAGA_JEICE =
            ENTITY_TYPES.register("saga_jeice",
                    () -> EntityType.Builder.of(SagaGinyuForcesEntity.SagaJeiceEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_jeice").toString()));
    public static final RegistryObject<EntityType<SagaGinyuForcesEntity.SagaGinyuEntity>> SAGA_GINYU =
            ENTITY_TYPES.register("saga_ginyu",
                    () -> EntityType.Builder.of(SagaGinyuForcesEntity.SagaGinyuEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_ginyu").toString()));
    public static final RegistryObject<EntityType<SagaGinyuForcesEntity.SagaGinyuGokuEntity>> SAGA_GINYU_GOKU =
            ENTITY_TYPES.register("saga_ginyu_goku",
                    () -> EntityType.Builder.of(SagaGinyuForcesEntity.SagaGinyuGokuEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_ginyu_goku").toString()));
    public static final RegistryObject<EntityType<SagaPiccoloEntity.SagaNailEntity>> SAGA_NAIL =
            ENTITY_TYPES.register("saga_nail",
                    () -> EntityType.Builder.of(SagaPiccoloEntity.SagaNailEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_nail").toString()));

    public static final RegistryObject<EntityType<SagaFriezaEntity.SagaFriezaFirstForm>> SAGA_FREEZER_FIRST =
            ENTITY_TYPES.register("saga_frieza_first",
                    () -> EntityType.Builder.of(SagaFriezaEntity.SagaFriezaFirstForm::new, MobCategory.MONSTER)
                            .sized(0.5f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_frieza_first").toString()));
    public static final RegistryObject<EntityType<SagaFriezaEntity.SagaFriezaSecondForm>> SAGA_FREEZER_SECOND =
            ENTITY_TYPES.register("saga_frieza_second",
                    () -> EntityType.Builder.of(SagaFriezaEntity.SagaFriezaSecondForm::new, MobCategory.MONSTER)
                            .sized(0.8f, 2.4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_frieza_second").toString()));
    public static final RegistryObject<EntityType<SagaFriezaEntity.SagaFriezaThirdForm>> SAGA_FREEZER_THIRD =
            ENTITY_TYPES.register("saga_frieza_third",
                    () -> EntityType.Builder.of(SagaFriezaEntity.SagaFriezaThirdForm::new, MobCategory.MONSTER)
                            .sized(1.0f, 2.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_frieza_base").toString()));
    public static final RegistryObject<EntityType<SagaFriezaEntity.SagaFriezaFinalForm>> SAGA_FREEZER_BASE =
            ENTITY_TYPES.register("saga_frieza_base",
                    () -> EntityType.Builder.of(SagaFriezaEntity.SagaFriezaFinalForm::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_frieza_base").toString()));
    public static final RegistryObject<EntityType<SagaFriezaEntity.SagaFriezaFPForm>> SAGA_FREEZER_FP =
            ENTITY_TYPES.register("saga_frieza_fp",
                    () -> EntityType.Builder.of(SagaFriezaEntity.SagaFriezaFPForm::new, MobCategory.MONSTER)
                            .sized(0.8f, 2.1f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_frieza_fp").toString()));
    public static final RegistryObject<EntityType<SagaGohanEntity.SagaKidGohanEntity>> SAGA_KID_GOHAN =
            ENTITY_TYPES.register("saga_kid_gohan",
                    () -> EntityType.Builder.of(SagaGohanEntity.SagaKidGohanEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 1.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_kid_gohan").toString()));
    public static final RegistryObject<EntityType<SagaZFightersEntity.SagaKrillinEntity>> SAGA_KRILLIN =
            ENTITY_TYPES.register("saga_krillin",
                    () -> EntityType.Builder.of(SagaZFightersEntity.SagaKrillinEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_krillin").toString()));
    public static final RegistryObject<EntityType<SagaZFightersEntity.SagaTienShinhanEntity>> SAGA_TIEN_EARLY =
            ENTITY_TYPES.register("saga_tien_early",
                    () -> EntityType.Builder.of(SagaZFightersEntity.SagaTienShinhanEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_tien_early").toString()));
    public static final RegistryObject<EntityType<SagaZFightersEntity.SagaYamchaEntity>> SAGA_YAMCHA =
            ENTITY_TYPES.register("saga_yamcha",
                    () -> EntityType.Builder.of(SagaZFightersEntity.SagaYamchaEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_yamcha").toString()));
    public static final RegistryObject<EntityType<SagaGokuEntity.SagaGokuMidBaseEntity>> SAGA_GOKU_MID_BASE =
            ENTITY_TYPES.register("saga_goku_mid_base",
                    () -> EntityType.Builder.of(SagaGokuEntity.SagaGokuMidBaseEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_goku_mid_base").toString()));
    public static final RegistryObject<EntityType<SagaGokuEntity.SagaGokuMidSSJEntity>> SAGA_GOKU_MID_SSJ =
            ENTITY_TYPES.register("saga_goku_mid_ssj",
                    () -> EntityType.Builder.of(SagaGokuEntity.SagaGokuMidSSJEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_goku_mid_ssj").toString()));

    /*
    ANDROID SAGA ENTITIES
    */
    public static final RegistryObject<EntityType<SagaFriezaEntity.SagaMechaFrieza>> SAGA_MECHA_FRIEZA =
            ENTITY_TYPES.register("saga_mecha_frieza",
                    () -> EntityType.Builder.of(SagaFriezaEntity.SagaMechaFrieza::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_mecha_frieza").toString()));
    public static final RegistryObject<EntityType<SagaFriezaEntity.SagaKingCold>> SAGA_KING_COLD =
            ENTITY_TYPES.register("saga_king_cold",
                    () -> EntityType.Builder.of(SagaFriezaEntity.SagaKingCold::new, MobCategory.MONSTER)
                            .sized(0.9f, 2.4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_king_cold").toString()));
    public static final RegistryObject<EntityType<SagaAndroidsEntity.SagaDrGeroEntity>> SAGA_DRGERO =
            ENTITY_TYPES.register("saga_drgero",
                    () -> EntityType.Builder.of(SagaAndroidsEntity.SagaDrGeroEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_drgero").toString()));
    public static final RegistryObject<EntityType<SagaAndroidsEntity.SagaA19Entity>> SAGA_A19 =
            ENTITY_TYPES.register("saga_a19",
                    () -> EntityType.Builder.of(SagaAndroidsEntity.SagaA19Entity::new, MobCategory.MONSTER)
                            .sized(0.9f, 2.1f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_a19").toString()));
    public static final RegistryObject<EntityType<SagaAndroidsEntity.SagaA18Entity>> SAGA_A18 =
            ENTITY_TYPES.register("saga_a18",
                    () -> EntityType.Builder.of(SagaAndroidsEntity.SagaA18Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_a18").toString()));
    public static final RegistryObject<EntityType<SagaAndroidsEntity.SagaA17Entity>> SAGA_A17 =
            ENTITY_TYPES.register("saga_a17",
                    () -> EntityType.Builder.of(SagaAndroidsEntity.SagaA17Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_a17").toString()));
    public static final RegistryObject<EntityType<SagaAndroidsEntity.SagaA16Entity>> SAGA_A16 =
            ENTITY_TYPES.register("saga_a16",
                    () -> EntityType.Builder.of(SagaAndroidsEntity.SagaA16Entity::new, MobCategory.MONSTER)
                            .sized(0.9f, 2.2f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_a16").toString()));
    public static final RegistryObject<EntityType<SagaCellEntity.SagaImperfectCellEntity>> SAGA_CELL_IMPERFECT =
            ENTITY_TYPES.register("saga_cell_imperfect",
                    () -> EntityType.Builder.of(SagaCellEntity.SagaImperfectCellEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_cell_imperfect").toString()));
    public static final RegistryObject<EntityType<SagaPiccoloEntity.SagaPiccoloKamiEntity>> SAGA_PICCOLO_KAMI =
            ENTITY_TYPES.register("saga_piccolo_kami",
                    () -> EntityType.Builder.of(SagaPiccoloEntity.SagaPiccoloKamiEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_piccolo_kami").toString()));
    public static final RegistryObject<EntityType<SagaCellEntity.SagaSemiPerfectCellEntity>> SAGA_CELL_SEMIPERFECT =
            ENTITY_TYPES.register("saga_cell_semiperfect",
                    () -> EntityType.Builder.of(SagaCellEntity.SagaSemiPerfectCellEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_cell_semiperfect").toString()));
    public static final RegistryObject<EntityType<SagaVegetaEntity.SagaVegetaMidBaseEntity>> SAGA_VEGETA_MID =
            ENTITY_TYPES.register("saga_vegeta_mid_base",
                    () -> EntityType.Builder.of(SagaVegetaEntity.SagaVegetaMidBaseEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_vegeta_mid_base").toString()));
    public static final RegistryObject<EntityType<SagaVegetaEntity.SagaVegetaMidSSJEntity>> SAGA_VEGETA_MID_SSJ =
            ENTITY_TYPES.register("saga_vegeta_mid_ssj",
                    () -> EntityType.Builder.of(SagaVegetaEntity.SagaVegetaMidSSJEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_vegeta_mid_ssj").toString()));
    public static final RegistryObject<EntityType<SagaVegetaEntity.SagaVegetaMidSSG2Entity>> SAGA_VEGETA_MID_SSG2 =
            ENTITY_TYPES.register("saga_vegeta_mid_ssg2",
                    () -> EntityType.Builder.of(SagaVegetaEntity.SagaVegetaMidSSG2Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_vegeta_mid_ssg2").toString()));
    public static final RegistryObject<EntityType<SagaTrunksEntity.SagaFutureTrunksKidBaseEntity>> SAGA_FUTURE_TRUNKS_KID_BASE =
            ENTITY_TYPES.register("saga_ftrunks_kid_base",
                    () -> EntityType.Builder.of(SagaTrunksEntity.SagaFutureTrunksKidBaseEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_ftrunks_kid_base").toString()));
    public static final RegistryObject<EntityType<SagaTrunksEntity.SagaFutureTrunksKidSSJEntity>> SAGA_FUTURE_TRUNKS_KID_SSJ =
            ENTITY_TYPES.register("saga_ftrunks_kid_ssj",
                    () -> EntityType.Builder.of(SagaTrunksEntity.SagaFutureTrunksKidSSJEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_ftrunks_kid_ssj").toString()));
    public static final RegistryObject<EntityType<SagaTrunksEntity.SagaFutureTrunksBaseEntity>> SAGA_FUTURE_TRUNKS_BASE =
            ENTITY_TYPES.register("saga_ftrunks_base",
                    () -> EntityType.Builder.of(SagaTrunksEntity.SagaFutureTrunksBaseEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_ftrunks_base").toString()));
    public static final RegistryObject<EntityType<SagaTrunksEntity.SagaFutureTrunksSSJEntity>> SAGA_FUTURE_TRUNKS_SSJ =
            ENTITY_TYPES.register("saga_ftrunks_ssj",
                    () -> EntityType.Builder.of(SagaTrunksEntity.SagaFutureTrunksSSJEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_ftrunks_ssj").toString()));
    public static final RegistryObject<EntityType<SagaTrunksEntity.SagaFutureTrunksSSG3Entity>> SAGA_FUTURE_TRUNKS_SSG3 =
            ENTITY_TYPES.register("saga_ftrunks_ssg3",
                    () -> EntityType.Builder.of(SagaTrunksEntity.SagaFutureTrunksSSG3Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_ftrunks_ssg3").toString()));
    public static final RegistryObject<EntityType<SagaCellEntity.SagaPerfectCellEntity>> SAGA_CELL_PERFECT =
            ENTITY_TYPES.register("saga_cell_perfect",
                    () -> EntityType.Builder.of(SagaCellEntity.SagaPerfectCellEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_cell_perfect").toString()));
    public static final RegistryObject<EntityType<SagaGohanEntity.SagaGohanMidBaseEntity>> SAGA_GOHAN_MID_BASE =
            ENTITY_TYPES.register("saga_gohan_mid_base",
                    () -> EntityType.Builder.of(SagaGohanEntity.SagaGohanMidBaseEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_gohan_mid_base").toString()));
    public static final RegistryObject<EntityType<SagaGohanEntity.SagaGohanMidSSJEntity>> SAGA_GOHAN_MID_SSJ =
            ENTITY_TYPES.register("saga_gohan_mid_ssj",
                    () -> EntityType.Builder.of(SagaGohanEntity.SagaGohanMidSSJEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_gohan_mid_ssj").toString()));
    public static final RegistryObject<EntityType<SagaGohanEntity.SagaGohanMidSSJ2Entity>> SAGA_GOHAN_MID_SSJ2 =
            ENTITY_TYPES.register("saga_gohan_mid_ssj2",
                    () -> EntityType.Builder.of(SagaGohanEntity.SagaGohanMidSSJ2Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_gohan_mid_ssj2").toString()));
    public static final RegistryObject<EntityType<SagaGohanEntity.SagaFutureGohanBaseEntity>> SAGA_FUTURE_GOHAN_BASE =
                ENTITY_TYPES.register("saga_fgohan_base",
                    () -> EntityType.Builder.of(SagaGohanEntity.SagaFutureGohanBaseEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_fgohan_base").toString()));
    public static final RegistryObject<EntityType<SagaGohanEntity.SagaFutureGohanSSJEntity>> SAGA_FUTURE_GOHAN_SSJ =
            ENTITY_TYPES.register("saga_fgohan_ssj",
                    () -> EntityType.Builder.of(SagaGohanEntity.SagaFutureGohanSSJEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_fgohan_ssj").toString()));
    public static final RegistryObject<EntityType<SagaCellEntity.SagaSuperPerfectCellEntity>> SAGA_CELL_SUPERPERFECT =
            ENTITY_TYPES.register("saga_cell_superperfect",
                    () -> EntityType.Builder.of(SagaCellEntity.SagaSuperPerfectCellEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_cell_superperfect").toString()));
    public static final RegistryObject<EntityType<SagaCellEntity.SagaCellJREntity>> SAGA_CELL_JR =
            ENTITY_TYPES.register("saga_cell_jr",
                    () -> EntityType.Builder.of(SagaCellEntity.SagaCellJREntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_cell_jr").toString()));

    /*
     BUU SAGA ENTITIES
     */
    public static final RegistryObject<EntityType<SagaGokuEntity.SagaGokuEndBaseEntity>> SAGA_GOKU_END_BASE =
            ENTITY_TYPES.register("saga_goku_end_base",
                    () -> EntityType.Builder.of(SagaGokuEntity.SagaGokuEndBaseEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_goku_end_base").toString()));
    public static final RegistryObject<EntityType<SagaGokuEntity.SagaGokuEndSSJEntity>> SAGA_GOKU_END_SSJ =
            ENTITY_TYPES.register("saga_goku_end_ssj",
                    () -> EntityType.Builder.of(SagaGokuEntity.SagaGokuEndSSJEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_goku_end_ssj").toString()));
    public static final RegistryObject<EntityType<SagaGokuEntity.SagaGokuEndSSJ2Entity>> SAGA_GOKU_END_SSJ2 =
            ENTITY_TYPES.register("saga_goku_end_ssj2",
                    () -> EntityType.Builder.of(SagaGokuEntity.SagaGokuEndSSJ2Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_goku_end_ssj2").toString()));
    public static final RegistryObject<EntityType<SagaGokuEntity.SagaGokuEndSSJ3Entity>> SAGA_GOKU_END_SSJ3 =
            ENTITY_TYPES.register("saga_goku_end_ssj3",
                    () -> EntityType.Builder.of(SagaGokuEntity.SagaGokuEndSSJ3Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_goku_end_ssj3").toString()));
    public static final RegistryObject<EntityType<SagaVegetaEntity.SagaVegetaEndBaseEntity>> SAGA_VEGETA_END_BASE =
            ENTITY_TYPES.register("saga_vegeta_end_base",
                    () -> EntityType.Builder.of(SagaVegetaEntity.SagaVegetaEndBaseEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_vegeta_end_base").toString()));
    public static final RegistryObject<EntityType<SagaVegetaEntity.SagaVegetaEndSSJEntity>> SAGA_VEGETA_END_SSJ =
            ENTITY_TYPES.register("saga_vegeta_end_ssj",
                    () -> EntityType.Builder.of(SagaVegetaEntity.SagaVegetaEndSSJEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_vegeta_end_ssj").toString()));
    public static final RegistryObject<EntityType<SagaVegetaEntity.SagaVegetaEndSSJ2Entity>> SAGA_VEGETA_END_SSJ2 =
            ENTITY_TYPES.register("saga_vegeta_end_ssj2",
                    () -> EntityType.Builder.of(SagaVegetaEntity.SagaVegetaEndSSJ2Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_vegeta_end_ssj2").toString()));
    public static final RegistryObject<EntityType<SagaVegetaEntity.SagaMajinVegetaEntity>> SAGA_VEGETA_MAJIN =
            ENTITY_TYPES.register("saga_vegeta_majin",
                    () -> EntityType.Builder.of(SagaVegetaEntity.SagaMajinVegetaEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_vegeta_majin").toString()));
    public static final RegistryObject<EntityType<SagaGohanEntity.SagaGohanEndBaseEntity>> SAGA_GOHAN_END_BASE =
            ENTITY_TYPES.register("saga_gohan_end_base",
                    () -> EntityType.Builder.of(SagaGohanEntity.SagaGohanEndBaseEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_gohan_end_base").toString()));
    public static final RegistryObject<EntityType<SagaGohanEntity.SagaGohanEndSSJEntity>> SAGA_GOHAN_END_SSJ =
            ENTITY_TYPES.register("saga_gohan_end_ssj",
                    () -> EntityType.Builder.of(SagaGohanEntity.SagaGohanEndSSJEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_gohan_end_ssj").toString()));
    public static final RegistryObject<EntityType<SagaGohanEntity.SagaGohanEndSSJ2Entity>> SAGA_GOHAN_END_SSJ2 =
            ENTITY_TYPES.register("saga_gohan_end_ssj2",
                    () -> EntityType.Builder.of(SagaGohanEntity.SagaGohanEndSSJ2Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_gohan_end_ssj2").toString()));
    public static final RegistryObject<EntityType<SagaGohanEntity.SagaGohanEndUltimateEntity>> SAGA_GOHAN_END_ULTIMATE =
            ENTITY_TYPES.register("saga_gohan_end_ultimate",
                    () -> EntityType.Builder.of(SagaGohanEntity.SagaGohanEndUltimateEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_gohan_end_ultimate").toString()));
    public static final RegistryObject<EntityType<SagaGotenEntity.SagaGotenKidEntity>> SAGA_GOTEN =
            ENTITY_TYPES.register("saga_goten",
                    () -> EntityType.Builder.of(SagaGotenEntity.SagaGotenKidEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 1.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_goten").toString()));
    public static final RegistryObject<EntityType<SagaGotenEntity.SagaGotenKidSSJEntity>> SAGA_GOTEN_SSJ =
            ENTITY_TYPES.register("saga_goten_ssj",
                    () -> EntityType.Builder.of(SagaGotenEntity.SagaGotenKidSSJEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 1.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_goten_ssj").toString()));
    public static final RegistryObject<EntityType<SagaTrunksEntity.SagaKidTrunksBaseEntity>> SAGA_KID_TRUNKS =
            ENTITY_TYPES.register("saga_kid_trunks",
                    () -> EntityType.Builder.of(SagaTrunksEntity.SagaKidTrunksBaseEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 1.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_kid_trunks").toString()));
    public static final RegistryObject<EntityType<SagaTrunksEntity.SagaKidTrunksSSJEntity>> SAGA_KID_TRUNKS_SSJ =
            ENTITY_TYPES.register("saga_kid_trunks_ssj",
                    () -> EntityType.Builder.of(SagaTrunksEntity.SagaKidTrunksSSJEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 1.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_kid_trunks_ssj").toString()));
    public static final RegistryObject<EntityType<SagaGotenEntity.SagaGotenksBaseEntity>> SAGA_GOTENKS =
            ENTITY_TYPES.register("saga_gotenks",
                    () -> EntityType.Builder.of(SagaGotenEntity.SagaGotenksBaseEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 1.4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_gotenks").toString()));
    public static final RegistryObject<EntityType<SagaGotenEntity.SagaGotenksSSJEntity>> SAGA_GOTENKS_SSJ =
            ENTITY_TYPES.register("saga_gotenks_ssj",
                    () -> EntityType.Builder.of(SagaGotenEntity.SagaGotenksSSJEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 1.4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_gotenks_ssj").toString()));
    public static final RegistryObject<EntityType<SagaGotenEntity.SagaGotenksSSJ3Entity>> SAGA_GOTENKS_SSJ3 =
            ENTITY_TYPES.register("saga_gotenks_ssj3",
                    () -> EntityType.Builder.of(SagaGotenEntity.SagaGotenksSSJ3Entity::new, MobCategory.MONSTER)
                            .sized(0.5f, 1.4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_gotenks_ssj3").toString()));
    public static final RegistryObject<EntityType<SagaZFightersEntity.SagaShinEntity>> SAGA_SHIN =
            ENTITY_TYPES.register("saga_shin",
                    () -> EntityType.Builder.of(SagaZFightersEntity.SagaShinEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_shin").toString()));
    public static final RegistryObject<EntityType<SagaZFightersEntity.SagaShinEntity>> SAGA_VIDEL =
            ENTITY_TYPES.register("saga_videl",
                    () -> EntityType.Builder.of(SagaZFightersEntity.SagaShinEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_videl").toString()));
    public static final RegistryObject<EntityType<SagaZFightersEntity.BasicNPCEntity>> SAGA_BULMA =
            ENTITY_TYPES.register("saga_bulma",
                    () -> EntityType.Builder.of(SagaZFightersEntity.BasicNPCEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_bulma").toString()));
    public static final RegistryObject<EntityType<SagaZFightersEntity.SagaKibitoEntity>> SAGA_KIBITO =
            ENTITY_TYPES.register("saga_kibito",
                    () -> EntityType.Builder.of(SagaZFightersEntity.SagaKibitoEntity::new, MobCategory.MONSTER)
                            .sized(0.9f, 2.2f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_kibito").toString()));
    public static final RegistryObject<EntityType<SagaBabidiSoldiersEntity.SagaSpopovitchEntity>> SAGA_SPOPOVITCH =
            ENTITY_TYPES.register("saga_spopovitch",
                    () -> EntityType.Builder.of(SagaBabidiSoldiersEntity.SagaSpopovitchEntity::new, MobCategory.MONSTER)
                            .sized(0.9f, 2.2f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_spopovitch").toString()));
    public static final RegistryObject<EntityType<SagaBabidiSoldiersEntity.SagaPuiPuiEntity>> SAGA_PUIPUI=
            ENTITY_TYPES.register("saga_puipui",
                    () -> EntityType.Builder.of(SagaBabidiSoldiersEntity.SagaPuiPuiEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_puipui").toString()));
    public static final RegistryObject<EntityType<SagaBabidiSoldiersEntity.SagaYakonEntity>> SAGA_YAKON=
            ENTITY_TYPES.register("saga_yakon",
                    () -> EntityType.Builder.of(SagaBabidiSoldiersEntity.SagaYakonEntity::new, MobCategory.MONSTER)
                            .sized(1.1f, 2.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_yakon").toString()));
    public static final RegistryObject<EntityType<SagaBabidiSoldiersEntity.DaburaEntity>> SAGA_DABURA=
            ENTITY_TYPES.register("saga_dabura",
                    () -> EntityType.Builder.of(SagaBabidiSoldiersEntity.DaburaEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_dabura").toString()));
    public static final RegistryObject<EntityType<SagaBabidiSoldiersEntity.BabidiEntity>> SAGA_BABIDI=
            ENTITY_TYPES.register("saga_babidi",
                    () -> EntityType.Builder.of(SagaBabidiSoldiersEntity.BabidiEntity::new, MobCategory.MONSTER)
                            .sized(0.4f, 1.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_babidi").toString()));
    public static final RegistryObject<EntityType<SagaBuuEntity.BuuFatEntity>> SAGA_BUU_FAT=
            ENTITY_TYPES.register("saga_buufat",
                    () -> EntityType.Builder.of(SagaBuuEntity.BuuFatEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.9f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_buufat").toString()));
    public static final RegistryObject<EntityType<SagaBuuEntity.EvilBuuEntity>> SAGA_EVILBUU=
            ENTITY_TYPES.register("saga_evilbuu",
                    () -> EntityType.Builder.of(SagaBuuEntity.EvilBuuEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 2.1f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_evilbuu").toString()));
    public static final RegistryObject<EntityType<SagaBuuEntity.MiniBuuEntity>> MINI_BUU =
            ENTITY_TYPES.register("mini_buu",
                    () -> EntityType.Builder.of(SagaBuuEntity.MiniBuuEntity::new, MobCategory.MONSTER)
                            .sized(0.4f, 0.9f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "mini_buu").toString()));
    public static final RegistryObject<EntityType<SagaBuuEntity.SuperBuuEntity>> SAGA_SUPERBUU=
            ENTITY_TYPES.register("saga_superbuu",
                    () -> EntityType.Builder.of(SagaBuuEntity.SuperBuuEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.1f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_superbuu").toString()));
    public static final RegistryObject<EntityType<SagaBuuEntity.SuperBuuPiccoloEntity>> SAGA_SUPERBUU_PICCOLO=
            ENTITY_TYPES.register("saga_superbuu_piccolo",
                    () -> EntityType.Builder.of(SagaBuuEntity.SuperBuuPiccoloEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.1f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_superbuu_piccolo").toString()));
    public static final RegistryObject<EntityType<SagaBuuEntity.SuperBuuGotenksEntity>> SAGA_SUPERBUU_GOTENKS=
            ENTITY_TYPES.register("saga_superbuu_gotenks",
                    () -> EntityType.Builder.of(SagaBuuEntity.SuperBuuGotenksEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.1f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_superbuu_gotenks").toString()));
    public static final RegistryObject<EntityType<SagaBuuEntity.SuperBuuGohanEntity>> SAGA_SUPERBUU_GOHAN=
            ENTITY_TYPES.register("saga_superbuu_gohan",
                    () -> EntityType.Builder.of(SagaBuuEntity.SuperBuuGohanEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.1f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_superbuu_gohan").toString()));
    public static final RegistryObject<EntityType<SagaBuuEntity.KidBuuEntity>> SAGA_KIDBUU=
            ENTITY_TYPES.register("saga_kidbuu",
                    () -> EntityType.Builder.of(SagaBuuEntity.KidBuuEntity::new, MobCategory.MONSTER)
                            .sized(0.4f, 1.4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_kidbuu").toString()));

    // Garlick Jr.
    public static final RegistryObject<EntityType<SagaMoviesEntity.GarlickJrEntity>> SAGA_GARLICK_JR =
            ENTITY_TYPES.register("saga_garlick_jr",
                    () -> EntityType.Builder.of(SagaMoviesEntity.GarlickJrEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 1.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_garlick_jr").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.GarlickJrTransformedEntity>> SAGA_GARLICK_JR_TRANSFORMED =
            ENTITY_TYPES.register("saga_garlick_jr_transformed",
                    () -> EntityType.Builder.of(SagaMoviesEntity.GarlickJrTransformedEntity::new, MobCategory.MONSTER)
                            .sized(0.9f, 2.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_garlick_jr_transformed").toString()));

    // Dr. Wheelo
    public static final RegistryObject<EntityType<SagaMoviesEntity.DrWheeloEntity>> SAGA_DR_WHEELO =
            ENTITY_TYPES.register("saga_dr_wheelo",
                    () -> EntityType.Builder.of(SagaMoviesEntity.DrWheeloEntity::new, MobCategory.MONSTER)
                            .sized(3.0f, 4.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_dr_wheelo").toString()));

    // Turles
    public static final RegistryObject<EntityType<SagaMoviesEntity.TurlesEntity>> SAGA_TURLES =
            ENTITY_TYPES.register("saga_turles",
                    () -> EntityType.Builder.of(SagaMoviesEntity.TurlesEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_turles").toString()));

    // Lord Slug
    public static final RegistryObject<EntityType<SagaMoviesEntity.SlugSoldierEntity>> SAGA_SLUG_SOLDIER =
            ENTITY_TYPES.register("saga_slug_soldier",
                    () -> EntityType.Builder.of(SagaMoviesEntity.SlugSoldierEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_slug_soldier").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.SlugEntity>> SAGA_SLUG =
            ENTITY_TYPES.register("saga_slug",
                    () -> EntityType.Builder.of(SagaMoviesEntity.SlugEntity::new, MobCategory.MONSTER)
                            .sized(0.7f, 2.2f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_slug").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.SlugGiantEntity>> SAGA_SLUG_GIANT =
            ENTITY_TYPES.register("saga_slug_giant",
                    () -> EntityType.Builder.of(SagaMoviesEntity.SlugGiantEntity::new, MobCategory.MONSTER)
                            .sized(4.5f, 12.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_slug_giant").toString()));

    // Cooler
    public static final RegistryObject<EntityType<SagaMoviesEntity.CoolerSoldierEntity>> SAGA_SALZA =
            ENTITY_TYPES.register("saga_salza",
                    () -> EntityType.Builder.of(SagaMoviesEntity.CoolerSoldierEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_salza").toString()));

    public static final RegistryObject<EntityType<SagaMoviesEntity.CoolerSoldierEntity>> SAGA_DORE =
            ENTITY_TYPES.register("saga_dore",
                    () -> EntityType.Builder.of(SagaMoviesEntity.CoolerSoldierEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_dore").toString()));

    public static final RegistryObject<EntityType<SagaMoviesEntity.CoolerSoldierEntity>> SAGA_NEIZ =
            ENTITY_TYPES.register("saga_neiz",
                    () -> EntityType.Builder.of(SagaMoviesEntity.CoolerSoldierEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_neiz").toString()));

    public static final RegistryObject<EntityType<SagaMoviesEntity.CoolerEntity>> SAGA_COOLER =
            ENTITY_TYPES.register("saga_cooler",
                    () -> EntityType.Builder.of(SagaMoviesEntity.CoolerEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_cooler").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.Cooler5TAEntity>> SAGA_COOLER_5TA =
            ENTITY_TYPES.register("saga_cooler_5ta",
                    () -> EntityType.Builder.of(SagaMoviesEntity.Cooler5TAEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 2.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_cooler_5ta").toString()));

    // Meta-Cooler
    public static final RegistryObject<EntityType<SagaMoviesEntity.GeteRobotEntity>> SAGA_GETE_ROBOT =
            ENTITY_TYPES.register("saga_gete_robot",
                    () -> EntityType.Builder.of(SagaMoviesEntity.GeteRobotEntity::new, MobCategory.MONSTER)
                            .sized(0.9f, 2.4f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_gete_robot").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.MetalCoolerEntity>> SAGA_METAL_COOLER =
            ENTITY_TYPES.register("saga_metal_cooler",
                    () -> EntityType.Builder.of(SagaMoviesEntity.MetalCoolerEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_metal_cooler").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.MetalCoolerCoreEntity>> SAGA_METAL_COOLER_CORE =
            ENTITY_TYPES.register("saga_metal_cooler_core",
                    () -> EntityType.Builder.of(SagaMoviesEntity.MetalCoolerCoreEntity::new, MobCategory.MONSTER)
                            .sized(4.5f, 12.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_metal_cooler_core").toString()));

    // Super Android 13
    public static final RegistryObject<EntityType<SagaMoviesEntity.A14Entity>> SAGA_A14 =
            ENTITY_TYPES.register("saga_a14",
                    () -> EntityType.Builder.of(SagaMoviesEntity.A14Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.2f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_a14").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.A15Entity>> SAGA_A15 =
            ENTITY_TYPES.register("saga_a15",
                    () -> EntityType.Builder.of(SagaMoviesEntity.A15Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_a15").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.A13Entity>> SAGA_A13 =
            ENTITY_TYPES.register("saga_a13",
                    () -> EntityType.Builder.of(SagaMoviesEntity.A13Entity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_a13").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.SuperA13Entity>> SAGA_SUPER_A13 =
            ENTITY_TYPES.register("saga_super_a13",
                    () -> EntityType.Builder.of(SagaMoviesEntity.SuperA13Entity::new, MobCategory.MONSTER)
                            .sized(1.0f, 2.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_super_a13").toString()));

    // Broly (Z)
    public static final RegistryObject<EntityType<SagaMoviesEntity.ParagusEntity>> SAGA_PARAGUS =
            ENTITY_TYPES.register("saga_paragus",
                    () -> EntityType.Builder.of(SagaMoviesEntity.ParagusEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_paragus").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.BrolyBaseEntity>> SAGA_BROLY_BASE =
            ENTITY_TYPES.register("saga_broly_base",
                    () -> EntityType.Builder.of(SagaMoviesEntity.BrolyBaseEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_broly_base").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.BrolySSJRestringidoEntity>> SAGA_BROLY_SSJ_RESTRICTED =
            ENTITY_TYPES.register("saga_broly_ssj_restricted",
                    () -> EntityType.Builder.of(SagaMoviesEntity.BrolySSJRestringidoEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_broly_ssj_restricted").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.BrolySSJEntity>> SAGA_BROLY_SSJ =
            ENTITY_TYPES.register("saga_broly_ssj",
                    () -> EntityType.Builder.of(SagaMoviesEntity.BrolySSJEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.1f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_broly_ssj").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.BrolySSJLegendarioEntity>> SAGA_BROLY_LSSJ =
            ENTITY_TYPES.register("saga_broly_lssj",
                    () -> EntityType.Builder.of(SagaMoviesEntity.BrolySSJLegendarioEntity::new, MobCategory.MONSTER)
                            .sized(1.0f, 2.7f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_broly_lssj").toString()));

    // Bojack
    public static final RegistryObject<EntityType<SagaMoviesEntity.ZangyaEntity>> SAGA_ZANGYA =
            ENTITY_TYPES.register("saga_zangya",
                    () -> EntityType.Builder.of(SagaMoviesEntity.ZangyaEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_zangya").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.GokuaEntity>> SAGA_GOKUA =
            ENTITY_TYPES.register("saga_gokua",
                    () -> EntityType.Builder.of(SagaMoviesEntity.GokuaEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.2f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_gokua").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.BidoEntity>> SAGA_BIDO =
            ENTITY_TYPES.register("saga_bido",
                    () -> EntityType.Builder.of(SagaMoviesEntity.BidoEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 2.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_bido").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.BujinEntity>> SAGA_BUJIN =
            ENTITY_TYPES.register("saga_bujin",
                    () -> EntityType.Builder.of(SagaMoviesEntity.BujinEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 1.6f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_bujin").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.BojackEntity>> SAGA_BOJACK =
            ENTITY_TYPES.register("saga_bojack",
                    () -> EntityType.Builder.of(SagaMoviesEntity.BojackEntity::new, MobCategory.MONSTER)
                            .sized(0.7f, 2.2f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_bojack").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.BojackFullPowerEntity>> SAGA_BOJACK_FP =
            ENTITY_TYPES.register("saga_bojack_fp",
                    () -> EntityType.Builder.of(SagaMoviesEntity.BojackFullPowerEntity::new, MobCategory.MONSTER)
                            .sized(0.9f, 2.3f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_bojack_fp").toString()));

    // Bio-Broly
    public static final RegistryObject<EntityType<SagaMoviesEntity.BioBrolyEntity>> SAGA_BIO_BROLY =
            ENTITY_TYPES.register("saga_bio_broly",
                    () -> EntityType.Builder.of(SagaMoviesEntity.BioBrolyEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 2.5f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_bio_broly").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.BioBrolyGiganteEntity>> SAGA_BIO_BROLY_GIANT =
            ENTITY_TYPES.register("saga_bio_broly_giant",
                    () -> EntityType.Builder.of(SagaMoviesEntity.BioBrolyGiganteEntity::new, MobCategory.MONSTER)
                            .sized(4.5f, 12.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_bio_broly_giant").toString()));

    // Janemba & Paikuhan
    public static final RegistryObject<EntityType<SagaMoviesEntity.PaikuhanEntity>> SAGA_PAIKUHAN =
            ENTITY_TYPES.register("saga_paikuhan",
                    () -> EntityType.Builder.of(SagaMoviesEntity.PaikuhanEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_paikuhan").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.JanembaGordoEntity>> SAGA_JANEMBA_FAT =
            ENTITY_TYPES.register("saga_janemba_fat",
                    () -> EntityType.Builder.of(SagaMoviesEntity.JanembaGordoEntity::new, MobCategory.MONSTER)
                            .sized(7.5f, 12.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_janemba_fat").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.SuperJanembaEntity>> SAGA_SUPER_JANEMBA =
            ENTITY_TYPES.register("saga_super_janemba",
                    () -> EntityType.Builder.of(SagaMoviesEntity.SuperJanembaEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_super_janemba").toString()));

    // Hirudegarn
    public static final RegistryObject<EntityType<SagaMoviesEntity.HirudegarnEntity>> SAGA_HIRUDEGARN =
            ENTITY_TYPES.register("saga_hirudegarn",
                    () -> EntityType.Builder.of(SagaMoviesEntity.HirudegarnEntity::new, MobCategory.MONSTER)
                            .sized(7.5f, 12.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_hirudegarn").toString()));
    public static final RegistryObject<EntityType<SagaMoviesEntity.SuperHirudegarnEntity>> SAGA_SUPER_HIRUDEGARN =
            ENTITY_TYPES.register("saga_super_hirudegarn",
                    () -> EntityType.Builder.of(SagaMoviesEntity.SuperHirudegarnEntity::new, MobCategory.MONSTER)
                            .sized(7.5f, 12.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "saga_super_hirudegarn").toString()));

    public static final RegistryObject<EntityType<ShadowDummyEntity>> SHADOW_DUMMY =
            ENTITY_TYPES.register("shadow_dummy",
                    () -> EntityType.Builder.of(ShadowDummyEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shadow_dummy").toString()));

    public static final RegistryObject<EntityType<KiBlastEntity>> KI_BLAST = ENTITY_TYPES.register("ki_blast",
            () -> EntityType.Builder.<KiBlastEntity>of(KiBlastEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .fireImmune()
                    .build("ki_blast"));
    public static final RegistryObject<EntityType<SPBlueHurricaneEntity>> SP_BLUE_HURRICANE = ENTITY_TYPES.register("sp_blue_hurricane",
            () -> EntityType.Builder.<SPBlueHurricaneEntity>of(SPBlueHurricaneEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .fireImmune()
                    .build("sp_blue_hurricane"));
    public static final RegistryObject<EntityType<KiLaserEntity>> KI_LASER = ENTITY_TYPES.register("ki_laser",
            () -> EntityType.Builder.<KiLaserEntity>of(KiLaserEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .fireImmune()
                    .build("ki_laser")
    );
    public static final RegistryObject<EntityType<KiWaveEntity>> KI_WAVE = ENTITY_TYPES.register("ki_wave",
            () -> EntityType.Builder.<KiWaveEntity>of(KiWaveEntity::new, MobCategory.MISC)
                    .sized(2.5F, 2.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .fireImmune()
                    .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "ki_wave").toString())
    );
    public static final RegistryObject<EntityType<KiDiskEntity>> KI_DISC = ENTITY_TYPES.register("ki_disc",
            () -> EntityType.Builder.<KiDiskEntity>of(KiDiskEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.1F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .fireImmune()
                    .build("ki_disc")
    );
    public static final RegistryObject<EntityType<KiBarrierEntity>> KI_BARRIER = ENTITY_TYPES.register("ki_barrier",
            () -> EntityType.Builder.<KiBarrierEntity>of(KiBarrierEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .fireImmune()
                    .build("ki_barrier")
    );
    public static final RegistryObject<EntityType<KiExplosionEntity>> KI_EXPLOSION = ENTITY_TYPES.register("ki_explosion",
            () -> EntityType.Builder.<KiExplosionEntity>of(KiExplosionEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .fireImmune()
                    .build("ki_explosion"));

    public static final RegistryObject<EntityType<KiAreaEntity>> KI_AREA = ENTITY_TYPES.register("ki_area",
            () -> EntityType.Builder.<KiAreaEntity>of(KiAreaEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .noSave()
                    .build("ki_area"));

    public static final RegistryObject<EntityType<KiExplosionVisualEntity>> KI_EXPLOSION_VISUAL =
            ENTITY_TYPES.register("ki_explosion_visual",
                    () -> EntityType.Builder.<KiExplosionVisualEntity>of(KiExplosionVisualEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .fireImmune()
                            .noSave()
                            .build("ki_explosion_visual")
            );

    public static final RegistryObject<EntityType<SPDragonFistEntity>> SP_DRAGON_FIST = ENTITY_TYPES.register("sp_dragon_fist",
            () -> EntityType.Builder.<SPDragonFistEntity>of(SPDragonFistEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("sp_dragon_fist")
    );

    public static final RegistryObject<EntityType<OzaruFistEntity>> SP_OZARU_FIST = ENTITY_TYPES.register("sp_ozaru_fist",
            () -> EntityType.Builder.<OzaruFistEntity>of(OzaruFistEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("sp_ozaru_fist")
    );

    public static final RegistryObject<EntityType<SPMajinCandyEntity>> SP_MAJIN_CANDY = ENTITY_TYPES.register("sp_majin_candy",
            () -> EntityType.Builder.<SPMajinCandyEntity>of(SPMajinCandyEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("sp_majin_candy"));

    // Single generic entity type for ALL data-driven quest NPCs
    public static final RegistryObject<EntityType<QuestNPCEntity>> QUEST_NPC =
            ENTITY_TYPES.register("quest_npc",
                    () -> EntityType.Builder.of(QuestNPCEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 2.0f)
                            .build(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "quest_npc").toString()));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        for (RegistryObject<? extends EntityType<? extends Mob>> sE : getSagaEntities()) {
            registerSagaSpawn(event, sE.get());
        }

        List<RegistryObject<? extends EntityType<? extends Mob>>> dinoEntities = List.of(
                DINOSAUR1, DINOSAUR2, DINOSAUR3, DINO_KID, SABERTOOTH);

        for (RegistryObject<? extends EntityType<? extends Mob>> dE : dinoEntities) {
            registerDinoSpawn(event, dE.get());
        }

        List<RegistryObject<? extends EntityType<? extends Mob>>> redRibbonEntities = List.of(
                BANDIT, RED_RIBBON_ROBOT1, RED_RIBBON_ROBOT2, RED_RIBBON_ROBOT3, RED_RIBBON_SOLDIER, MINI_BUU);

        for (RegistryObject<? extends EntityType<? extends Mob>> rrE : redRibbonEntities) {
            registerRedRibbonSpawn(event, rrE.get());
        }
    }

    private static <T extends Mob> void registerSagaSpawn(SpawnPlacementRegisterEvent event, EntityType<T> entityType) {
        event.register(entityType, SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING,
                (e, w, r, p, rand) -> DBSagasEntity.canSpawnHere((EntityType<? extends DBSagasEntity>) e, w, r, p, rand),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private static <T extends Mob> void registerDinoSpawn(SpawnPlacementRegisterEvent event, EntityType<T> entityType) {
        event.register(entityType, SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING,
                (e, w, r, p, rand) -> DinoGlobalEntity.canSpawnHere((EntityType<? extends DinoGlobalEntity>) e, w, r, p, rand),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private static <T extends Mob> void registerRedRibbonSpawn(SpawnPlacementRegisterEvent event, EntityType<T> entityType) {
        event.register(entityType, SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING,
                (e, w, r, p, rand) -> RedRibbonEntity.canSpawnHere((EntityType<? extends RedRibbonEntity>) e, w, r, p, rand),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}