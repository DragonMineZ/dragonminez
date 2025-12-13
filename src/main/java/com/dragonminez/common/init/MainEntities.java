package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.Dino1Entity;
import com.dragonminez.common.init.entities.animal.Dino2Entity;
import com.dragonminez.common.init.entities.animal.DinoFlyEntity;
import com.dragonminez.common.init.entities.masters.MasterKarinEntity;
import com.dragonminez.common.init.entities.redribbon.BanditEntity;
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

    public static final RegistryObject<EntityType<BanditEntity>> BANDIT =
            ENTITY_TYPES.register("bandit",
                    () -> EntityType.Builder.of(BanditEntity::new, MobCategory.CREATURE)
                            .sized(1.4f, 3.2f)
                            .build(new ResourceLocation(Reference.MOD_ID, "bandit").toString()));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
