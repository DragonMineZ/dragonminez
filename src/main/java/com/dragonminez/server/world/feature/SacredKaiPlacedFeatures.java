package com.dragonminez.server.world.feature;

import com.dragonminez.Reference;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class SacredKaiPlacedFeatures {
	public static final ResourceKey<PlacedFeature> OAK_TREE_PLACED = registerKey("sacredkai_oak_tree_placed");
	public static final ResourceKey<PlacedFeature> GRASS_PATCH_PLACED = registerKey("sacredkai_grass_patch_placed");
	public static final ResourceKey<PlacedFeature> FLOWERS_PLACED = registerKey("sacredkai_flowers_placed");
	public static final ResourceKey<PlacedFeature> SAND_PATCH_PLACED = registerKey("sacredkai_sand_patch_placed");
	public static final ResourceKey<PlacedFeature> ROCK_CLUSTER_PLACED = registerKey("sacredkai_rock_cluster_placed");
	public static final ResourceKey<PlacedFeature> ROCK_CLUSTER_SPARSE_PLACED = registerKey("sacredkai_rock_cluster_sparse_placed");
	public static final ResourceKey<PlacedFeature> GRASSY_PEAK_PLACED = registerKey("sacredkai_grassy_peak_placed");
	public static final ResourceKey<PlacedFeature> GRASSY_CLIFF_PLACED = registerKey("sacredkai_grassy_cliff_placed");
	public static final ResourceKey<PlacedFeature> KARST_PILLAR_PLACED = registerKey("sacredkai_karst_pillar_placed");
	public static final ResourceKey<PlacedFeature> STONE_SPIKE_PLACED = registerKey("sacredkai_stone_spike_placed");
	public static final ResourceKey<PlacedFeature> WATER_LAKE_PLACED = registerKey("sacredkai_water_lake_placed");

	public static void bootstrap(BootstapContext<PlacedFeature> context) {
		HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);

		register(context, OAK_TREE_PLACED, configured.getOrThrow(TreeFeatures.OAK),
				VegetationPlacements.treePlacement(PlacementUtils.countExtra(0, 0.2f, 1), Blocks.OAK_SAPLING));

		register(context, GRASS_PATCH_PLACED, configured.getOrThrow(SacredKaiConfiguredFeatures.GRASS_PATCH),
				surfaceSpread(NoiseThresholdCountPlacement.of(-0.8f, 5, 10)));

		register(context, FLOWERS_PLACED, configured.getOrThrow(SacredKaiConfiguredFeatures.FLOWERS),
				ImmutableList.<PlacementModifier>builder()
						.add(NoiseThresholdCountPlacement.of(-0.8f, 8, 3))
						.add(RarityFilter.onAverageOnceEvery(10))
						.add(InSquarePlacement.spread())
						.add(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG))
						.add(BiomeFilter.biome())
						.build());

		register(context, SAND_PATCH_PLACED, configured.getOrThrow(SacredKaiConfiguredFeatures.SAND_PATCH),
				List.of(RarityFilter.onAverageOnceEvery(10), InSquarePlacement.spread(), HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR_WG), BiomeFilter.biome()));

		register(context, ROCK_CLUSTER_PLACED, configured.getOrThrow(SacredKaiConfiguredFeatures.ROCK_CLUSTER),
				surfaceRarity(3));
		register(context, ROCK_CLUSTER_SPARSE_PLACED, configured.getOrThrow(SacredKaiConfiguredFeatures.ROCK_CLUSTER),
				surfaceRarity(8));

		register(context, GRASSY_PEAK_PLACED, configured.getOrThrow(SacredKaiConfiguredFeatures.GRASSY_PEAK),
				surfaceRarity(40));
		register(context, GRASSY_CLIFF_PLACED, configured.getOrThrow(SacredKaiConfiguredFeatures.GRASSY_CLIFF),
				surfaceRarity(32));
		register(context, KARST_PILLAR_PLACED, configured.getOrThrow(OverworldConfiguredFeatures.KARST_PILLAR_KEY),
				surfaceRarity(45));
		register(context, STONE_SPIKE_PLACED, configured.getOrThrow(OverworldConfiguredFeatures.STONE_SPIKE_KEY),
				surfaceRarity(90));

		register(context, WATER_LAKE_PLACED, configured.getOrThrow(SacredKaiConfiguredFeatures.WATER_LAKE),
				List.of(RarityFilter.onAverageOnceEvery(12), InSquarePlacement.spread(), PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT, BiomeFilter.biome()));
	}

	private static List<PlacementModifier> surfaceSpread(PlacementModifier count) {
		return List.of(count, InSquarePlacement.spread(), HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG), BiomeFilter.biome());
	}

	private static List<PlacementModifier> surfaceRarity(int onceEvery) {
		return List.of(RarityFilter.onAverageOnceEvery(onceEvery), InSquarePlacement.spread(), HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG), BiomeFilter.biome());
	}

	private static ResourceKey<PlacedFeature> registerKey(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}

	private static void register(BootstapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration, List<PlacementModifier> modifiers) {
		context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
	}
}
