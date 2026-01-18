package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MainAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Reference.MOD_ID);

    public static final RegistryObject<Attribute> DMZ_HEALTH = ATTRIBUTES.register("dmz_health",
        () -> new RangedAttribute("attribute.dragonminez.dmz_health", 5.0, 0.0, 2000000000.0)
            .setSyncable(true));
}

