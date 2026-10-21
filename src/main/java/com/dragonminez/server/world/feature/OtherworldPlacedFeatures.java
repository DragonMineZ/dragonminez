package com.dragonminez.server.world.feature;

import com.dragonminez.Reference;
import com.dragonminez.server.world.gen.OtherworldGeneration;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class OtherworldPlacedFeatures {
	public static final ResourceKey<PlacedFeature> CANDY_ORB_PLACED = registerKey("otherworld_candy_orb_placed");
	public static final ResourceKey<PlacedFeature> CRYSTAL_SPIKE_PLACED = registerKey("otherworld_crystal_spike_placed");
	public static final ResourceKey<PlacedFeature> BONE_SPINE_PLACED = registerKey("otherworld_bone_spine_placed");
	public static final ResourceKey<PlacedFeature> THORN_BALL_PLACED = registerKey("otherworld_thorn_ball_placed");

	public static void bootstrap(BootstapContext<PlacedFeature> context) {
		HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);

		register(context, CANDY_ORB_PLACED, configured.getOrThrow(OtherworldConfiguredFeatures.CANDY_ORB),
				List.of(
						CountPlacement.of(UniformInt.of(1, 3)),
						InSquarePlacement.spread(),
						HeightRangePlacement.uniform(VerticalAnchor.absolute(-20), VerticalAnchor.absolute(OtherworldGeneration.LOWER_CLOUD_DECK - 12)),
						BiomeFilter.biome()
				));

		register(context, CRYSTAL_SPIKE_PLACED, configured.getOrThrow(OtherworldConfiguredFeatures.CRYSTAL_SPIKE),
				List.of(
						RarityFilter.onAverageOnceEvery(4),
						InSquarePlacement.spread(),
						HeightRangePlacement.uniform(VerticalAnchor.absolute(OtherworldGeneration.HELL_CEILING - 4), VerticalAnchor.absolute(OtherworldGeneration.HELL_CEILING - 2)),
						BiomeFilter.biome()
				));

		register(context, BONE_SPINE_PLACED, configured.getOrThrow(OtherworldConfiguredFeatures.BONE_SPINE),
				List.of(
						RarityFilter.onAverageOnceEvery(28),
						InSquarePlacement.spread(),
						HeightRangePlacement.uniform(VerticalAnchor.absolute(OtherworldGeneration.HELL_CEILING - 4), VerticalAnchor.absolute(OtherworldGeneration.HELL_CEILING - 2)),
						BiomeFilter.biome()
				));

		register(context, THORN_BALL_PLACED, configured.getOrThrow(OtherworldConfiguredFeatures.THORN_BALL),
				List.of(
						RarityFilter.onAverageOnceEvery(9),
						InSquarePlacement.spread(),
						HeightRangePlacement.uniform(VerticalAnchor.absolute(OtherworldGeneration.HELL_CEILING - 4), VerticalAnchor.absolute(OtherworldGeneration.HELL_CEILING - 2)),
						BiomeFilter.biome()
				));
	}

	private static ResourceKey<PlacedFeature> registerKey(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}

	private static void register(BootstapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration, List<PlacementModifier> modifiers) {
		context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
	}
}
