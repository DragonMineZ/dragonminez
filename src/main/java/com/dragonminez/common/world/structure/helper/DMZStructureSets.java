package com.dragonminez.common.world.structure.helper;

import com.dragonminez.Reference;
import com.dragonminez.common.world.structure.placement.BiomeAwareUniquePlacement;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.Optional;

public class DMZStructureSets {
	public static final ResourceKey<StructureSet> GOKU_HOUSE = createKey("goku_house");
	public static final ResourceKey<StructureSet> ROSHI_HOUSE = createKey("roshi_house");

	private static final TagKey<Biome> VILLAGE_PLAINS_TAG = TagKey.create(Registries.BIOME, new ResourceLocation("minecraft", "has_structure/village_plains"));

	public static void bootstrap(BootstapContext<StructureSet> context) {
		HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
		HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

		context.register(GOKU_HOUSE, new StructureSet(
				structures.getOrThrow(DMZStructures.GOKU_HOUSE),
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						15.0f,
						12345678,
						Optional.empty(),
						biomes.getOrThrow(VILLAGE_PLAINS_TAG)
				)
		));

		context.register(ROSHI_HOUSE, new StructureSet(
				structures.getOrThrow(DMZStructures.ROSHI_HOUSE),
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						15.0f,
						87654321,
						Optional.empty(),
						biomes.getOrThrow(BiomeTags.IS_OCEAN)
				)
		));
	}

	private static ResourceKey<StructureSet> createKey(String name) {
		return ResourceKey.create(Registries.STRUCTURE_SET, new ResourceLocation(Reference.MOD_ID, name));
	}
}
