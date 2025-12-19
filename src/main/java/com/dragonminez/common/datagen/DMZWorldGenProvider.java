package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.world.biome.NamekBiomes;
import com.dragonminez.common.world.dimension.NamekDimension;
import com.dragonminez.common.world.gen.NamekConfiguredFeatures;
import com.dragonminez.common.world.gen.NamekGeneration;
import com.dragonminez.common.world.gen.NamekPlacedFeatures;
import com.dragonminez.common.world.structure.helper.DMZPools;
import com.dragonminez.common.world.structure.helper.DMZStructureSets;
import com.dragonminez.common.world.structure.helper.DMZStructures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DMZWorldGenProvider extends DatapackBuiltinEntriesProvider {

	public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
			.add(Registries.DIMENSION_TYPE, NamekDimension::bootstrap)
			.add(Registries.CONFIGURED_FEATURE, NamekConfiguredFeatures::bootstrap)
			.add(Registries.PLACED_FEATURE, NamekPlacedFeatures::bootstrap)
			.add(Registries.BIOME, NamekBiomes::bootstrap)
			.add(Registries.NOISE_SETTINGS, NamekGeneration::bootstrapNoise)
			.add(Registries.LEVEL_STEM, NamekGeneration::bootstrap)
			.add(Registries.TEMPLATE_POOL, DMZPools::bootstrap)
			.add(Registries.STRUCTURE, DMZStructures::bootstrap)
			.add(Registries.STRUCTURE_SET, DMZStructureSets::bootstrap);

	public DMZWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, BUILDER, Set.of(Reference.MOD_ID));
	}
}