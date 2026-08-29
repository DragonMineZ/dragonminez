package com.dragonminez.server.world.feature;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SacredKaiFeatures {
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Reference.MOD_ID);

	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ROCK_CLUSTER = FEATURES.register("rock_cluster",
			() -> new RockClusterFeature(NoneFeatureConfiguration.CODEC));

	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GRASSY_PEAK = FEATURES.register("grassy_peak",
			() -> new GrassyPeakFeature(NoneFeatureConfiguration.CODEC));

	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GRASSY_CLIFF = FEATURES.register("grassy_cliff",
			() -> new GrassyCliffFeature(NoneFeatureConfiguration.CODEC));

	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SAND_PATCH = FEATURES.register("sand_patch",
			() -> new SandPatchFeature(NoneFeatureConfiguration.CODEC));

	public static void register(IEventBus eventBus) {
		FEATURES.register(eventBus);
	}
}
