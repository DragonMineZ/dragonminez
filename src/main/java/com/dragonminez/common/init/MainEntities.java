package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.masters.MasterKarinEntity;
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

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
