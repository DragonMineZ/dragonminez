package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Reference.MOD_ID);

    public static final RegistryObject<Attribute> KI_BLAST_DAMAGE = register("ki_blast_damage", "attribute.dragonminez.ki_blast_damage", 20.0, 0.0, 2000000000.0);
    public static final RegistryObject<Attribute> FLY_SPEED = register("fly_speed", "attribute.dragonminez.fly_speed", 0.35, 0.0, 1024.0);
    public static final RegistryObject<Attribute> KI_BLAST_SPEED = register("ki_blast_speed", "attribute.dragonminez.ki_blast_speed", 0.6, 0.0, 1024.0);

    private static RegistryObject<Attribute> register(String registryName, String translationKey, double defaultValue, double min, double max) {
        return ATTRIBUTES.register(registryName,
            () -> new RangedAttribute(translationKey, defaultValue, min, max).setSyncable(true));
    }
}
