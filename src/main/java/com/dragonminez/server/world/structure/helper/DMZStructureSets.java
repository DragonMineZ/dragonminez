package com.dragonminez.server.world.structure.helper;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainTags;
import com.dragonminez.server.world.biome.NamekBiomes;
import com.dragonminez.server.world.structure.placement.BiomeAwareUniquePlacement;
import com.dragonminez.server.world.structure.placement.FixedStructurePlacement;
import com.dragonminez.server.world.structure.placement.UniqueNearSpawnPlacement;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
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
	public static final ResourceKey<StructureSet> GOKU_HOUSE = createKey("goku_house"),
			ROSHI_HOUSE = createKey("roshi_house"), TIMECHAMBER = createKey("timechamber"),
			ELDER_GURU = createKey("elder_guru"), KAMILOOKOUT = createKey("kamilookout"),
			ENMA_PALACE = createKey("enma_palace");

	private static final TagKey<Biome> VILLAGE_PLAINS_TAG = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "has_structure/village_plains"));

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

		context.register(TIMECHAMBER, new StructureSet(
				structures.getOrThrow(DMZStructures.TIMECHAMBER),
				new FixedStructurePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						11223344,
						Optional.empty(),
						0, 0
				)
		));

		context.register(ELDER_GURU, new StructureSet(
				structures.getOrThrow(DMZStructures.ELDER_GURU),
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						30.0f,
						44332211,
						Optional.empty(),
						biomes.getOrThrow(MainTags.Biomes.IS_SACREDLAND)
				)
		));

		context.register(KAMILOOKOUT, new StructureSet(
				structures.getOrThrow(DMZStructures.KAMILOOKOUT),
				new UniqueNearSpawnPlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						25.0f,
						55667788,
						Optional.empty()
				)
		));

		context.register(ENMA_PALACE, new StructureSet(
				structures.getOrThrow(DMZStructures.ENMA_PALACE),
				new FixedStructurePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						88776655,
						Optional.empty(),
						0, 0
				)
		));
	}

	private static ResourceKey<StructureSet> createKey(String name) {
		return ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}
}
