package com.dragonminez.server.world.feature;

import com.dragonminez.Reference;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class OverworldPlacedFeatures {
	public static final ResourceKey<PlacedFeature> STONE_SPIKE_PLACED_KEY = createKey("stone_spike_placed");

	public static void bootstrap(BootstapContext<PlacedFeature> context) {
		HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

		Holder<ConfiguredFeature<?, ?>> stoneSpikeHolder = configuredFeatures.getOrThrow(OverworldConfiguredFeatures.STONE_SPIKE_KEY);

		context.register(STONE_SPIKE_PLACED_KEY, new PlacedFeature(stoneSpikeHolder,
				ImmutableList.<PlacementModifier>builder()
						.add(NoiseThresholdCountPlacement.of(-0.8f, 15, 5))
						.add(RarityFilter.onAverageOnceEvery(15))
						.add(InSquarePlacement.spread())
						.add(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG))
						.add(BiomeFilter.biome())
						.build()
		));
	}

	private static ResourceKey<PlacedFeature> createKey(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(Reference.MOD_ID, name));
	}
}