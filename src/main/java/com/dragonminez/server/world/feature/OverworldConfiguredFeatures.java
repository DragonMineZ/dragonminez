package com.dragonminez.server.world.feature;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class OverworldConfiguredFeatures {
	public static final ResourceKey<ConfiguredFeature<?, ?>> STONE_SPIKE_KEY = createKey("stone_spike");

	public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
		context.register(STONE_SPIKE_KEY, new ConfiguredFeature<>(OverworldFeatures.STONE_SPIKE.get(), NoneFeatureConfiguration.INSTANCE));
	}

	private static ResourceKey<ConfiguredFeature<?, ?>> createKey(String name) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}
}