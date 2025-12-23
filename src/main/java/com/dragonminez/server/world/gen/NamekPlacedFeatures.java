package com.dragonminez.server.world.gen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class NamekPlacedFeatures {

	public static final ResourceKey<PlacedFeature> NAMEK_COAL_ORE_PLACED = registerKey("namek_coal_ore_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_COPPER_ORE_PLACED = registerKey("namek_copper_ore_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_IRON_ORE_PLACED = registerKey("namek_iron_ore_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_GOLD_ORE_PLACED = registerKey("namek_gold_ore_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_REDSTONE_ORE_PLACED = registerKey("namek_redstone_ore_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_EMERALD_ORE_PLACED = registerKey("namek_emerald_ore_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_LAPIS_ORE_PLACED = registerKey("namek_lapis_ore_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_DIAMOND_ORE_PLACED = registerKey("namek_diamond_ore_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_KIKONO_ORE_PLACED = registerKey("namek_kikono_ore_placed");

	public static final ResourceKey<PlacedFeature> NAMEK_PATCH_GRASS_PLAIN = registerKey("namek_patch_grass_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_PATCH_SACRED_GRASS_PLAIN = registerKey("namek_patch_sacred_grass_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_PLAINS_FLOWERS = registerKey("namek_plains_flowers_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_SACRED_FLOWERS = registerKey("namek_sacred_flowers_placed");

	public static final ResourceKey<PlacedFeature> AJISSA_TREE_PLACED = registerKey("namek_ajissa_tree_placed");
	public static final ResourceKey<PlacedFeature> SACRED_TREE_PLACED = registerKey("namek_sacred_tree_placed");

	public static final ResourceKey<PlacedFeature> NAMEK_LAKE_LAVA_PLACED = registerKey("namek_lake_lava_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_SPRING_LAVA_PLACED = registerKey("namek_spring_lava_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_SPRING_WATER_PLACED = registerKey("namek_spring_water_placed");

	public static void bootstrap(BootstapContext<PlacedFeature> context) {
		var configured = context.lookup(Registries.CONFIGURED_FEATURE);

		register(context, NAMEK_COAL_ORE_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_COAL_ORE),
				commonOrePlacement(30, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(256))));

		register(context, NAMEK_COPPER_ORE_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_COPPER_ORE),
				commonOrePlacement(20, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(128))));

		register(context, NAMEK_IRON_ORE_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_IRON_ORE),
				commonOrePlacement(20, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(128))));

		register(context, NAMEK_GOLD_ORE_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_GOLD_ORE),
				commonOrePlacement(10, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(32))));

		register(context, NAMEK_REDSTONE_ORE_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_REDSTONE_ORE),
				commonOrePlacement(8, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(16))));

		register(context, NAMEK_EMERALD_ORE_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_EMERALD_ORE),
				commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(128))));

		register(context, NAMEK_LAPIS_ORE_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_LAPIS_ORE),
				commonOrePlacement(10, HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(64))));

		register(context, NAMEK_DIAMOND_ORE_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_DIAMOND_ORE),
				commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(16))));

		register(context, NAMEK_KIKONO_ORE_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_KIKONO_ORE),
				commonOrePlacement(6, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(32))));

		register(context, AJISSA_TREE_PLACED, configured.getOrThrow(NamekConfiguredFeatures.AJISSA_TREE),
				VegetationPlacements.treePlacement(PlacementUtils.countExtra(1, 0.1f, 1), MainBlocks.NAMEK_AJISSA_SAPLING.get()));

		register(context, SACRED_TREE_PLACED, configured.getOrThrow(NamekConfiguredFeatures.SACRED_TREE),
				VegetationPlacements.treePlacement(PlacementUtils.countExtra(0, 0.05f, 1), MainBlocks.NAMEK_SACRED_SAPLING.get()));

		register(context, NAMEK_LAKE_LAVA_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_LAKE_LAVA),
				List.of(RarityFilter.onAverageOnceEvery(10), InSquarePlacement.spread(), PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT, BiomeFilter.biome()));

		register(context, NAMEK_SPRING_LAVA_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_SPRING_LAVA),
				List.of(CountPlacement.of(20), InSquarePlacement.spread(), PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT, BiomeFilter.biome()));

		register(context, NAMEK_SPRING_WATER_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_SPRING_WATER),
				List.of(CountPlacement.of(25), InSquarePlacement.spread(), PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT, BiomeFilter.biome()));

		//PLAINS
		register(context, NAMEK_PATCH_GRASS_PLAIN, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_PATCH_GRASS_KEY),
				ImmutableList.<PlacementModifier>builder()
						.add(NoiseThresholdCountPlacement.of(-0.8f, 5, 10))
						.add(InSquarePlacement.spread())
						.add(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG))
						.add(BiomeFilter.biome())
						.build());
		register(context, NAMEK_PLAINS_FLOWERS, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_FLOWERS_KEY),
				ImmutableList.<PlacementModifier>builder()
						.add(NoiseThresholdCountPlacement.of(-0.8f, 15, 4))
						.add(RarityFilter.onAverageOnceEvery(12))
						.add(InSquarePlacement.spread())
						.add(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG))
						.add(BiomeFilter.biome())
						.build());
		//SACRED
		register(context, NAMEK_PATCH_SACRED_GRASS_PLAIN, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_PATCH_SACRED_GRASS_KEY),
				ImmutableList.<PlacementModifier>builder()
						.add(NoiseThresholdCountPlacement.of(-0.8f, 5, 10))
						.add(InSquarePlacement.spread())
						.add(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG))
						.add(BiomeFilter.biome())
						.build());
		register(context, NAMEK_SACRED_FLOWERS, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_SACRED_FLOWERS_KEY),
				ImmutableList.<PlacementModifier>builder()
						.add(NoiseThresholdCountPlacement.of(-0.8f, 15, 4))
						.add(RarityFilter.onAverageOnceEvery(12))
						.add(InSquarePlacement.spread())
						.add(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG))
						.add(BiomeFilter.biome())
						.build());

	}

	private static List<PlacementModifier> orePlacement(PlacementModifier pCount, PlacementModifier pHeightRange) {
		return List.of(pCount, InSquarePlacement.spread(), pHeightRange, BiomeFilter.biome());
	}

	private static List<PlacementModifier> commonOrePlacement(int pCount, PlacementModifier pHeightRange) {
		return orePlacement(CountPlacement.of(pCount), pHeightRange);
	}

	private static ResourceKey<PlacedFeature> registerKey(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}

	private static void register(BootstapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration, List<PlacementModifier> modifiers) {
		context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
	}
}