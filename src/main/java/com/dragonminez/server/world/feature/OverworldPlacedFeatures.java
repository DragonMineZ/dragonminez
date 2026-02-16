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
    public static final ResourceKey<PlacedFeature> ROCKY_PEAK_PLACED_KEY = createKey("rocky_peak_placed");

	public static void bootstrap(BootstapContext<PlacedFeature> context) {
		HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

		Holder<ConfiguredFeature<?, ?>> stoneSpikeHolder = configuredFeatures.getOrThrow(OverworldConfiguredFeatures.STONE_SPIKE_KEY);

        context.register(STONE_SPIKE_PLACED_KEY, new PlacedFeature(stoneSpikeHolder,
                ImmutableList.<PlacementModifier>builder()
                        .add(NoiseThresholdCountPlacement.of(-0.8f, 15, 5))
                        .add(RarityFilter.onAverageOnceEvery(2))
                        .add(InSquarePlacement.spread())
                        .add(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG))
                        .add(BiomeFilter.biome())
                        .build()
        ));

        context.register(ROCKY_PEAK_PLACED_KEY, new PlacedFeature(
                configuredFeatures.getOrThrow(OverworldConfiguredFeatures.ROCKY_PEAK_KEY),
                List.of(
                        NoiseThresholdCountPlacement.of(-0.8f, 10, 4), // Para que aparezcan en grupos
                        InSquarePlacement.spread(),
                        HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG),
                        BiomeFilter.biome()
                )
        ));
	}

	private static ResourceKey<PlacedFeature> createKey(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}
}