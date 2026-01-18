package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.server.world.biome.HTCBiomes;
import com.dragonminez.server.world.biome.NamekBiomes;
import com.dragonminez.server.world.biome.OtherworldBiomes;
import com.dragonminez.server.world.biome.OverworldBiomes;
import com.dragonminez.server.world.dimension.HTCDimension;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import com.dragonminez.server.world.feature.NamekConfiguredFeatures;
import com.dragonminez.server.world.feature.NamekPlacedFeatures;
import com.dragonminez.server.world.feature.OverworldConfiguredFeatures;
import com.dragonminez.server.world.feature.OverworldPlacedFeatures;
import com.dragonminez.server.world.gen.*;
import com.dragonminez.server.world.structure.helper.DMZPools;
import com.dragonminez.server.world.structure.helper.DMZStructureSets;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DMZWorldGenProvider extends DatapackBuiltinEntriesProvider {

	public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
			.add(Registries.DIMENSION_TYPE, context -> {
				NamekDimension.bootstrap(context);
				HTCDimension.bootstrap(context);
				OtherworldDimension.bootstrap(context);
			})
			.add(Registries.BIOME, context -> {
				NamekBiomes.bootstrap(context);
				HTCBiomes.bootstrap(context);
				OtherworldBiomes.bootstrap(context);
				OverworldBiomes.bootstrap(context);
			})
			.add(Registries.NOISE_SETTINGS, context -> {
				NamekGeneration.bootstrapNoise(context);
				HTCGeneration.bootstrapNoise(context);
				OtherworldGeneration.bootstrapNoise(context);
			})
			.add(Registries.LEVEL_STEM, context -> {
				NamekGeneration.bootstrap(context);
				HTCGeneration.bootstrap(context);
				OtherworldGeneration.bootstrap(context);
			})
			.add(Registries.CONFIGURED_FEATURE, context -> {
				NamekConfiguredFeatures.bootstrap(context);
				OverworldConfiguredFeatures.bootstrap(context);
			})
			.add(Registries.PLACED_FEATURE, context -> {
				NamekPlacedFeatures.bootstrap(context);
				OverworldPlacedFeatures.bootstrap(context);
			})
			.add(Registries.TEMPLATE_POOL, DMZPools::bootstrap)
			.add(Registries.STRUCTURE, DMZStructures::bootstrap)
			.add(Registries.STRUCTURE_SET, DMZStructureSets::bootstrap);

	public DMZWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, BUILDER, Set.of(Reference.MOD_ID));
	}
}