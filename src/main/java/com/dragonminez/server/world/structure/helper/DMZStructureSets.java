package com.dragonminez.server.world.structure.helper;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainTags;
import com.dragonminez.server.world.biome.NamekBiomes;
import com.dragonminez.server.world.biome.OverworldBiomes;
import com.dragonminez.server.world.biome.SacredKaiBiomes;
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
			GERO_LAB = createKey("gero_lab"), BABIDI = createKey("babidi"),
			CELL_ARENA = createKey("cell_arena"), FRIEZA_SHIP = createKey("frieza_ship"),
			PICCOLO_HOUSE = createKey("piccolo_house"), OLDKAI_PILLAR = createKey("oldkai_pillar"),
			YAMCHA_HOUSE = createKey("yamcha_house"), TRUNKS_SHIP = createKey("trunks_ship"),
			VEGETA_POD = createKey("vegeta_pod");

	private static final TagKey<Biome> VILLAGE_PLAINS_TAG = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "has_structure/village_plains"));

	public static void bootstrap(BootstapContext<StructureSet> context) {
		HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
		HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

		context.register(GOKU_HOUSE, new StructureSet(
				structures.getOrThrow(DMZStructures.GOKU_HOUSE),
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
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
						1.0f,
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
						1.0f,
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
						1.0f,
						55667788,
						Optional.empty()
				)
		));

		context.register(GERO_LAB, new StructureSet(
				structures.getOrThrow(DMZStructures.GERO_LAB),
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						99887766,
						Optional.empty(),
						biomes.getOrThrow(MainTags.Biomes.IS_ROCKYBIOME)
				)
		));

		context.register(BABIDI, new StructureSet(
				structures.getOrThrow(DMZStructures.BABIDI),
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						18273645,
						Optional.empty(),
						biomes.getOrThrow(BiomeTags.IS_MOUNTAIN)
				)
		));

		context.register(CELL_ARENA, new StructureSet(
				structures.getOrThrow(DMZStructures.CELL_ARENA),
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						13572468,
						Optional.empty(),
						biomes.getOrThrow(BiomeTags.HAS_VILLAGE_PLAINS)
				)
		));

		context.register(FRIEZA_SHIP, new StructureSet(
				structures.getOrThrow(DMZStructures.FRIEZA_SHIP),
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						24681357,
						Optional.empty(),
						HolderSet.direct(biomes.getOrThrow(NamekBiomes.AJISSA_PLAINS))
				)
		));

		context.register(PICCOLO_HOUSE, new StructureSet(
				structures.getOrThrow(DMZStructures.PICCOLO_HOUSE),
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						36925814,
						Optional.empty(),
						biomes.getOrThrow(BiomeTags.IS_MOUNTAIN)
				)
		));

		context.register(OLDKAI_PILLAR, new StructureSet(
				structures.getOrThrow(DMZStructures.OLDKAI_PILLAR),
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						41258963,
						Optional.empty(),
						HolderSet.direct(biomes.getOrThrow(SacredKaiBiomes.SACREDKAI_PLAINS))
				)
		));

		context.register(YAMCHA_HOUSE, new StructureSet(
				structures.getOrThrow(DMZStructures.YAMCHA_HOUSE),
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						55114477,
						Optional.empty(),
						biomes.getOrThrow(BiomeTags.HAS_VILLAGE_DESERT)
				)
		));

		context.register(TRUNKS_SHIP, new StructureSet(
				structures.getOrThrow(DMZStructures.TRUNKS_SHIP),
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						66332211,
						Optional.empty(),
						biomes.getOrThrow(MainTags.Biomes.IS_LAND)
				)
		));

		context.register(VEGETA_POD, new StructureSet(
				structures.getOrThrow(DMZStructures.VEGETA_POD),
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						77889900,
						Optional.empty(),
						biomes.getOrThrow(MainTags.Biomes.IS_ROCKYBIOME)
				)
		));
	}

	private static ResourceKey<StructureSet> createKey(String name) {
		return ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}
}
