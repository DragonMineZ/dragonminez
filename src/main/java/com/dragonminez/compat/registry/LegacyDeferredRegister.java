package com.dragonminez.compat.registry;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Deferred register with explicit, optional aliases for legacy addon namespaces. */
public final class LegacyDeferredRegister<T> extends DeferredRegister<T> {
    private final String[] legacyNamespaces;

    private LegacyDeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace, String... legacyNamespaces) {
        super(registryKey, namespace);
        this.legacyNamespaces = legacyNamespaces.clone();
    }

    public static <T> LegacyDeferredRegister<T> create(Registry<T> registry, String namespace) {
        return new LegacyDeferredRegister<>(registry.key(), namespace);
    }

    public static <T> LegacyDeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        return new LegacyDeferredRegister<>(registryKey, namespace);
    }

    public static <T> LegacyDeferredRegister<T> createWithAliases(Registry<T> registry, String namespace, String... legacyNamespaces) {
        return new LegacyDeferredRegister<>(registry.key(), namespace, legacyNamespaces);
    }

    public static <T> LegacyDeferredRegister<T> createWithAliases(ResourceKey<? extends Registry<T>> registryKey, String namespace, String... legacyNamespaces) {
        return new LegacyDeferredRegister<>(registryKey, namespace, legacyNamespaces);
    }

    @Override
    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> supplier) {
        DeferredHolder<T, I> holder = super.register(name, supplier);
        addLegacyAliases(name, holder.getId());
        return holder;
    }

    @Override
    public <I extends T> DeferredHolder<T, I> register(String name, Function<ResourceLocation, ? extends I> factory) {
        DeferredHolder<T, I> holder = super.register(name, factory);
        addLegacyAliases(name, holder.getId());
        return holder;
    }

    private void addLegacyAliases(String path, ResourceLocation target) {
        for (String namespace : legacyNamespaces) {
            addAlias(ResourceLocation.fromNamespaceAndPath(namespace, path), target);
        }
    }
}
