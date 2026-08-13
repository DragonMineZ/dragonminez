package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, Reference.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> KI_BLAST_DAMAGE = register("ki_blast_damage", "attribute.dragonminez.ki_blast_damage", 20.0, 0.0, 2000000000.0);
    public static final DeferredHolder<Attribute, Attribute> FLY_SPEED = register("fly_speed", "attribute.dragonminez.fly_speed", 0.35, 0.0, 1024.0);
    public static final DeferredHolder<Attribute, Attribute> KI_BLAST_SPEED = register("ki_blast_speed", "attribute.dragonminez.ki_blast_speed", 0.6, 0.0, 1024.0);

    private static DeferredHolder<Attribute, Attribute> register(String registryName, String translationKey, double defaultValue, double min, double max) {
        return ATTRIBUTES.register(registryName,
            () -> new RangedAttribute(translationKey, defaultValue, min, max).setSyncable(true));
    }
}
