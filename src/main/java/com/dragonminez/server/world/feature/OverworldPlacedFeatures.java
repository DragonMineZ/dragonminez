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
    public static final ResourceKey<PlacedFeature> KARST_PILLAR_PLACED_KEY = createKey("karst_pillar_placed");
    public static final ResourceKey<PlacedFeature> ROCKY_CLIFF_PLACED_KEY = createKey("rocky_cliff_placed");

	public static void bootstrap(BootstapContext<PlacedFeature> context) {
		HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

		Holder<ConfiguredFeature<?, ?>> stoneSpikeHolder = configuredFeatures.getOrThrow(OverworldConfiguredFeatures.STONE_SPIKE_KEY);
		Holder<ConfiguredFeature<?, ?>> rockyPeakHolder = configuredFeatures.getOrThrow(OverworldConfiguredFeatures.ROCKY_PEAK_KEY);
		Holder<ConfiguredFeature<?, ?>> karstPillarHolder = configuredFeatures.getOrThrow(OverworldConfiguredFeatures.KARST_PILLAR_KEY);
		Holder<ConfiguredFeature<?, ?>> rockyCliffHolder = configuredFeatures.getOrThrow(OverworldConfiguredFeatures.ROCKY_CLIFF_KEY);

        context.register(STONE_SPIKE_PLACED_KEY, new PlacedFeature(stoneSpikeHolder,
                ImmutableList.<PlacementModifier>builder()
                        .add(NoiseThresholdCountPlacement.of(-0.8f, 15, 5))
                        .add(RarityFilter.onAverageOnceEvery(80))
                        .add(InSquarePlacement.spread())
                        .add(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG))
                        .add(BiomeFilter.biome())
                        .build()
        ));

        // Mesas: tall and bulky, the dominant landform of the rocky wasteland.
        context.register(ROCKY_PEAK_PLACED_KEY, new PlacedFeature(rockyPeakHolder,
				ImmutableList.<PlacementModifier>builder()
						.add(NoiseThresholdCountPlacement.of(-0.8f, 4, 2))
						.add(RarityFilter.onAverageOnceEvery(32))
						.add(InSquarePlacement.spread())
						.add(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG))
						.add(BiomeFilter.biome())
						.build()
        ));

        // Karst formations / rock pillars: clusters of thin pinnacles scattered between the peaks.
        context.register(KARST_PILLAR_PLACED_KEY, new PlacedFeature(karstPillarHolder,
				ImmutableList.<PlacementModifier>builder()
						.add(NoiseThresholdCountPlacement.of(-0.8f, 6, 2))
						.add(RarityFilter.onAverageOnceEvery(28))
						.add(InSquarePlacement.spread())
						.add(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG))
						.add(BiomeFilter.biome())
						.build()
        ));

        // Cliffs/risers of all sizes — the dominant rock formation of the desolate desert.
        context.register(ROCKY_CLIFF_PLACED_KEY, new PlacedFeature(rockyCliffHolder,
				ImmutableList.<PlacementModifier>builder()
						.add(NoiseThresholdCountPlacement.of(-0.8f, 4, 2))
						.add(RarityFilter.onAverageOnceEvery(18))
						.add(InSquarePlacement.spread())
						.add(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG))
						.add(BiomeFilter.biome())
						.build()
        ));
	}

	private static ResourceKey<PlacedFeature> createKey(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}
}