package com.dragonminez.compat;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Bridges legacy raw registry values to the holder-based Minecraft 1.21.1 APIs. */
public final class LegacyRegistryHolders {
    private static final net.minecraft.core.HolderLookup.Provider BUILTIN_LOOKUP =
            net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private LegacyRegistryHolders() {}

    public static Holder<Attribute> attribute(Attribute value) {
        return holder(BuiltInRegistries.ATTRIBUTE, value, "attribute");
    }

    public static Holder<MobEffect> effect(MobEffect value) {
        return holder(BuiltInRegistries.MOB_EFFECT, value, "mob effect");
    }

    public static net.minecraft.core.HolderLookup.Provider provider() {
        return BUILTIN_LOOKUP;
    }

    private static <T> Holder<T> holder(Registry<T> registry, T value, String kind) {
        if (value == null) return null;
        return registry.getResourceKey(value)
                .flatMap(registry::getHolder)
                .orElseThrow(() -> new IllegalArgumentException("Unregistered " + kind + " value: " + value));
    }
}
