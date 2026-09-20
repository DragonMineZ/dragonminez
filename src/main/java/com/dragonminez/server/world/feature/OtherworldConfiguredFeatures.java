package com.dragonminez.server.world.feature;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class OtherworldConfiguredFeatures {
	public static final ResourceKey<ConfiguredFeature<?, ?>> CANDY_ORB = key("otherworld_candy_orb");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CRYSTAL_SPIKE = key("otherworld_crystal_spike");
	public static final ResourceKey<ConfiguredFeature<?, ?>> BONE_SPINE = key("otherworld_bone_spine");
	public static final ResourceKey<ConfiguredFeature<?, ?>> THORN_BALL = key("otherworld_thorn_ball");

	public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
		register(context, CANDY_ORB, OtherworldFeatures.CANDY_ORB.get(), NoneFeatureConfiguration.INSTANCE);
		register(context, CRYSTAL_SPIKE, OtherworldFeatures.CRYSTAL_SPIKE.get(), NoneFeatureConfiguration.INSTANCE);
		register(context, BONE_SPINE, OtherworldFeatures.BONE_SPINE.get(), NoneFeatureConfiguration.INSTANCE);
		register(context, THORN_BALL, OtherworldFeatures.THORN_BALL.get(), NoneFeatureConfiguration.INSTANCE);
	}

	private static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}

	private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
		context.register(key, new ConfiguredFeature<>(feature, configuration));
	}
}
