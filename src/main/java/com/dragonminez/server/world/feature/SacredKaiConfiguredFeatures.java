package com.dragonminez.server.world.feature;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;

import java.util.List;

public class SacredKaiConfiguredFeatures {
	public static final ResourceKey<ConfiguredFeature<?, ?>> ROCK_CLUSTER = key("sacredkai_rock_cluster");
	public static final ResourceKey<ConfiguredFeature<?, ?>> GRASSY_PEAK = key("sacredkai_grassy_peak");
	public static final ResourceKey<ConfiguredFeature<?, ?>> GRASSY_CLIFF = key("sacredkai_grassy_cliff");
	public static final ResourceKey<ConfiguredFeature<?, ?>> SAND_PATCH = key("sacredkai_sand_patch");
	public static final ResourceKey<ConfiguredFeature<?, ?>> WATER_LAKE = key("sacredkai_water_lake");
	public static final ResourceKey<ConfiguredFeature<?, ?>> GRASS_PATCH = key("sacredkai_grass_patch");
	public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWERS = key("sacredkai_flowers");

	public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
		register(context, ROCK_CLUSTER, SacredKaiFeatures.ROCK_CLUSTER.get(), NoneFeatureConfiguration.INSTANCE);
		register(context, GRASSY_PEAK, SacredKaiFeatures.GRASSY_PEAK.get(), NoneFeatureConfiguration.INSTANCE);
		register(context, GRASSY_CLIFF, SacredKaiFeatures.GRASSY_CLIFF.get(), NoneFeatureConfiguration.INSTANCE);

		register(context, SAND_PATCH, SacredKaiFeatures.SAND_PATCH.get(), NoneFeatureConfiguration.INSTANCE);

		register(context, WATER_LAKE, Feature.LAKE, new LakeFeature.Configuration(
				BlockStateProvider.simple(Blocks.WATER.defaultBlockState()),
				BlockStateProvider.simple(MainBlocks.ROCKY_STONE.get().defaultBlockState())));

		register(context, GRASS_PATCH, Feature.RANDOM_PATCH, new RandomPatchConfiguration(32, 7, 3,
				PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
						BlockStateProvider.simple(Blocks.GRASS)))));

		register(context, FLOWERS, Feature.FLOWER, new RandomPatchConfiguration(64, 6, 2,
				PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
						new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
								.add(Blocks.DANDELION.defaultBlockState(), 2)
								.add(Blocks.POPPY.defaultBlockState(), 2)
								.add(Blocks.AZURE_BLUET.defaultBlockState(), 1)
								.add(Blocks.OXEYE_DAISY.defaultBlockState(), 1)
								.add(Blocks.CORNFLOWER.defaultBlockState(), 1)
								.build())))));
	}

	public static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}

	private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
		context.register(key, new ConfiguredFeature<>(feature, configuration));
	}
}
