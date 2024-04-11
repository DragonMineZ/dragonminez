package com.yuseix.dragonminez.init;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.init.entity.custom.DinoEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public interface MainEntity {
    DeferredRegister<EntityType<?>> ENTITY_TYPES_REGISTER =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DragonMineZ.MOD_ID);


    RegistryObject<EntityType<DinoEntity>> DINO1 =
            ENTITY_TYPES_REGISTER.register("dino",
                    () -> EntityType.Builder.of(DinoEntity::new, MobCategory.AMBIENT)
                            .sized(4.5f, 4.8f)
                            .build(new ResourceLocation(DragonMineZ.MOD_ID, "dino").toString())
            );

}
