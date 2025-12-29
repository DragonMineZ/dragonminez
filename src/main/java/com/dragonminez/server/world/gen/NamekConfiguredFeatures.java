package com.dragonminez.server.world.gen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainFluids;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.List;

public class NamekConfiguredFeatures {
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_COAL_ORE = registerKey("namek_coal_ore");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_COPPER_ORE = registerKey("namek_copper_ore");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_IRON_ORE = registerKey("namek_iron_ore");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_GOLD_ORE = registerKey("namek_gold_ore");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_REDSTONE_ORE = registerKey("namek_redstone_ore");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_EMERALD_ORE = registerKey("namek_emerald_ore");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_LAPIS_ORE = registerKey("namek_lapis_ore");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_DIAMOND_ORE = registerKey("namek_diamond_ore");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_KIKONO_ORE = registerKey("namek_kikono_ore");

	public static final ResourceKey<ConfiguredFeature<?, ?>> AJISSA_TREE = registerKey("namek_ajissa_tree");
	public static final ResourceKey<ConfiguredFeature<?, ?>> SACRED_TREE = registerKey("namek_sacred_tree");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_LAKE_LAVA = registerKey("namek_lake_lava");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_SPRING_LAVA = registerKey("namek_spring_lava");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_SPRING_WATER = registerKey("namek_spring_water");

	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_PATCH_GRASS_KEY = registerKey("namek_patch_grass_configured");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_FLOWERS_KEY = registerKey("namek_flowers_configured");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_PATCH_SACRED_GRASS_KEY = registerKey("namek_patch_sacred_grass_configured");
	public static final ResourceKey<ConfiguredFeature<?, ?>> NAMEK_SACRED_FLOWERS_KEY = registerKey("namek_sacred_flowers_configured");

	public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
		RuleTest stoneReplaceables = new BlockMatchTest(MainBlocks.NAMEK_STONE.get());
		RuleTest deepslateReplaceables = new BlockMatchTest(MainBlocks.NAMEK_DEEPSLATE.get());
		RuleTest dirtReplaceables = new BlockMatchTest(MainBlocks.NAMEK_DIRT.get());

		List<OreConfiguration.TargetBlockState> coalTargets = List.of(
				OreConfiguration.target(stoneReplaceables, MainBlocks.NAMEK_COAL_ORE.get().defaultBlockState()),
				OreConfiguration.target(deepslateReplaceables, MainBlocks.NAMEK_DEEPSLATE_COAL.get().defaultBlockState())
		);
		register(context, NAMEK_COAL_ORE, Feature.ORE, new OreConfiguration(coalTargets, 17));

		List<OreConfiguration.TargetBlockState> copperTargets = List.of(
				OreConfiguration.target(stoneReplaceables, MainBlocks.NAMEK_COPPER_ORE.get().defaultBlockState()),
				OreConfiguration.target(deepslateReplaceables, MainBlocks.NAMEK_DEEPSLATE_COPPER.get().defaultBlockState())
		);
		register(context, NAMEK_COPPER_ORE, Feature.ORE, new OreConfiguration(copperTargets, 10));

		List<OreConfiguration.TargetBlockState> ironTargets = List.of(
				OreConfiguration.target(stoneReplaceables, MainBlocks.NAMEK_IRON_ORE.get().defaultBlockState()),
				OreConfiguration.target(deepslateReplaceables, MainBlocks.NAMEK_DEEPSLATE_IRON.get().defaultBlockState())
		);
		register(context, NAMEK_IRON_ORE, Feature.ORE, new OreConfiguration(ironTargets, 9));

		List<OreConfiguration.TargetBlockState> goldTargets = List.of(
				OreConfiguration.target(stoneReplaceables, MainBlocks.NAMEK_GOLD_ORE.get().defaultBlockState()),
				OreConfiguration.target(deepslateReplaceables, MainBlocks.NAMEK_DEEPSLATE_GOLD.get().defaultBlockState())
		);
		register(context, NAMEK_GOLD_ORE, Feature.ORE, new OreConfiguration(goldTargets, 9));

		List<OreConfiguration.TargetBlockState> redstoneTargets = List.of(
				OreConfiguration.target(stoneReplaceables, MainBlocks.NAMEK_REDSTONE_ORE.get().defaultBlockState()),
				OreConfiguration.target(deepslateReplaceables, MainBlocks.NAMEK_DEEPSLATE_REDSTONE.get().defaultBlockState())
		);
		register(context, NAMEK_REDSTONE_ORE, Feature.ORE, new OreConfiguration(redstoneTargets, 8));

		List<OreConfiguration.TargetBlockState> emeraldTargets = List.of(
				OreConfiguration.target(stoneReplaceables, MainBlocks.NAMEK_EMERALD_ORE.get().defaultBlockState()),
				OreConfiguration.target(deepslateReplaceables, MainBlocks.NAMEK_DEEPSLATE_EMERALD.get().defaultBlockState())
		);
		register(context, NAMEK_EMERALD_ORE, Feature.ORE, new OreConfiguration(emeraldTargets, 3));

		List<OreConfiguration.TargetBlockState> lapisTargets = List.of(
				OreConfiguration.target(stoneReplaceables, MainBlocks.NAMEK_LAPIS_ORE.get().defaultBlockState()),
				OreConfiguration.target(deepslateReplaceables, MainBlocks.NAMEK_DEEPSLATE_LAPIS.get().defaultBlockState())
		);
		register(context, NAMEK_LAPIS_ORE, Feature.ORE, new OreConfiguration(lapisTargets, 7));

		List<OreConfiguration.TargetBlockState> diamondTargets = List.of(
				OreConfiguration.target(stoneReplaceables, MainBlocks.NAMEK_DIAMOND_ORE.get().defaultBlockState()),
				OreConfiguration.target(deepslateReplaceables, MainBlocks.NAMEK_DEEPSLATE_DIAMOND.get().defaultBlockState())
		);
		register(context, NAMEK_DIAMOND_ORE, Feature.ORE, new OreConfiguration(diamondTargets, 8));

		List<OreConfiguration.TargetBlockState> kikonoTargets = List.of(
				OreConfiguration.target(stoneReplaceables, MainBlocks.NAMEK_KIKONO_ORE.get().defaultBlockState()),
				OreConfiguration.target(deepslateReplaceables, MainBlocks.NAMEK_KIKONO_ORE.get().defaultBlockState()),
				OreConfiguration.target(dirtReplaceables, MainBlocks.NAMEK_KIKONO_ORE.get().defaultBlockState())
		);
		register(context, NAMEK_KIKONO_ORE, Feature.ORE, new OreConfiguration(kikonoTargets, 8));

		register(context, AJISSA_TREE, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(MainBlocks.NAMEK_AJISSA_LOG.get()),
				new StraightTrunkPlacer(5, 2, 0),
				BlockStateProvider.simple(MainBlocks.NAMEK_AJISSA_LEAVES.get()),
				new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
				new TwoLayersFeatureSize(1, 0, 1)
		).dirt(BlockStateProvider.simple(MainBlocks.NAMEK_DIRT.get())).build());

		register(context, SACRED_TREE, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(MainBlocks.NAMEK_SACRED_LOG.get()),
				new StraightTrunkPlacer(4, 2, 4),
				BlockStateProvider.simple(MainBlocks.NAMEK_SACRED_LEAVES.get()),
				new BlobFoliagePlacer(ConstantInt.of(3), ConstantInt.of(2), 3),
				new TwoLayersFeatureSize(1, 0, 2)
		).dirt(BlockStateProvider.simple(MainBlocks.NAMEK_DIRT.get())).build());

		register(context, NAMEK_LAKE_LAVA, Feature.LAKE, new LakeFeature.Configuration(
				BlockStateProvider.simple(Blocks.LAVA.defaultBlockState()),
				BlockStateProvider.simple(MainBlocks.NAMEK_STONE.get().defaultBlockState())
		));

		register(context, NAMEK_SPRING_LAVA, Feature.SPRING, new SpringConfiguration(
				Blocks.LAVA.defaultBlockState().getFluidState(),
				true, 4, 1,
				HolderSet.direct(MainBlocks.NAMEK_STONE.get().builtInRegistryHolder())
		));

		register(context, NAMEK_SPRING_WATER, Feature.SPRING, new SpringConfiguration(
				MainFluids.SOURCE_NAMEK.get().defaultFluidState(),
				true, 4, 1,
				HolderSet.direct(MainBlocks.NAMEK_STONE.get().builtInRegistryHolder(), MainBlocks.NAMEK_DIRT.get().builtInRegistryHolder())
		));

		register(context, NAMEK_PATCH_GRASS_KEY, Feature.RANDOM_PATCH, new RandomPatchConfiguration(32, 7, 3,
				PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
						BlockStateProvider.simple(MainBlocks.NAMEK_GRASS.get().defaultBlockState())
				))));
		register(context, NAMEK_PATCH_SACRED_GRASS_KEY, Feature.RANDOM_PATCH, new RandomPatchConfiguration(32, 7, 3,
				PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
						BlockStateProvider.simple(MainBlocks.NAMEK_SACRED_GRASS.get().defaultBlockState()
						)))));

		register(context, NAMEK_FLOWERS_KEY, Feature.FLOWER, new RandomPatchConfiguration(64, 12, 4,
				PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
						new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()

								.add(MainBlocks.CHRYSANTHEMUM_FLOWER.get().defaultBlockState(), 5)
								.add(MainBlocks.AMARYLLIS_FLOWER.get().defaultBlockState(), 8)
								.add(MainBlocks.MARIGOLD_FLOWER.get().defaultBlockState(), 8)
								.add(MainBlocks.CATHARANTHUS_ROSEUS_FLOWER.get().defaultBlockState(), 8)
								.add(MainBlocks.TRILLIUM_FLOWER.get().defaultBlockState(), 8)
								.add(MainBlocks.NAMEK_FERN.get().defaultBlockState(), 8)

								.build())
				))));

		register(context, NAMEK_SACRED_FLOWERS_KEY, Feature.FLOWER, new RandomPatchConfiguration(64, 12, 4,
				PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
						new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()

								.add(MainBlocks.SACRED_AMARYLLIS_FLOWER.get().defaultBlockState(), 5)
								.add(MainBlocks.SACRED_CHRYSANTHEMUM_FLOWER.get().defaultBlockState(), 8)
								.add(MainBlocks.SACRED_CATHARANTHUS_ROSEUS_FLOWER.get().defaultBlockState(), 8)
								.add(MainBlocks.SACRED_MARIGOLD_FLOWER.get().defaultBlockState(), 8)
								.add(MainBlocks.SACRED_TRILLIUM_FLOWER.get().defaultBlockState(), 8)
								.add(MainBlocks.SACRED_FERN.get().defaultBlockState(), 8)

								.build())
				))));
	}

	public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}

	private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
		context.register(key, new ConfiguredFeature<>(feature, configuration));
	}
}