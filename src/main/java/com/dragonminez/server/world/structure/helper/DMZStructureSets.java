package com.dragonminez.server.world.structure.helper;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainTags;
import com.dragonminez.server.world.biome.NamekBiomes;
import com.dragonminez.server.world.biome.SacredKaiBiomes;
import com.dragonminez.server.world.structure.placement.BiomeAwareUniquePlacement;
import com.dragonminez.server.world.structure.placement.FixedStructurePlacement;
import com.dragonminez.server.world.structure.placement.UniqueNearSpawnPlacement;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
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
			VEGETA_POD = createKey("vegeta_pod"),
			BABA_PALACE = createKey("baba_palace"), TREE_OF_MIGHT = createKey("tree_of_might"),
			SAIYAN_CRATER = createKey("saiyan_crater"), GETE_STAR = createKey("gete_star"),
			DEMON_VILLAGE = createKey("demon_village"),
			GOMAH_CAMP = createKey("gomah_camp"), BUU_HOUSE = createKey("buu_house");

	public static void bootstrap(BootstapContext<StructureSet> context) {
		HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
		HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

		unique(context, GOKU_HOUSE, structures.getOrThrow(DMZStructures.GOKU_HOUSE),
				12345678, biomes.getOrThrow(MainTags.Biomes.IS_PLAINSLIKE));
		unique(context, ROSHI_HOUSE, structures.getOrThrow(DMZStructures.ROSHI_HOUSE),
				87654321, biomes.getOrThrow(BiomeTags.IS_OCEAN));
		unique(context, BABA_PALACE, structures.getOrThrow(DMZStructures.BABA_PALACE),
				31415926, biomes.getOrThrow(BiomeTags.IS_OCEAN));

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

		unique(context, ELDER_GURU, structures.getOrThrow(DMZStructures.ELDER_GURU),
				44332211, biomes.getOrThrow(MainTags.Biomes.IS_SACREDLAND));

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

		unique(context, GERO_LAB, structures.getOrThrow(DMZStructures.GERO_LAB),
				99887766, biomes.getOrThrow(MainTags.Biomes.IS_GEROLAB));
		unique(context, BABIDI, structures.getOrThrow(DMZStructures.BABIDI),
				18273645, biomes.getOrThrow(MainTags.Biomes.IS_MOUNTAINLIKE));
		unique(context, CELL_ARENA, structures.getOrThrow(DMZStructures.CELL_ARENA),
				13572468, biomes.getOrThrow(MainTags.Biomes.IS_PLAINSLIKE));
		unique(context, FRIEZA_SHIP, structures.getOrThrow(DMZStructures.FRIEZA_SHIP),
				24681357, HolderSet.direct(biomes.getOrThrow(NamekBiomes.AJISSA_PLAINS)));
		unique(context, PICCOLO_HOUSE, structures.getOrThrow(DMZStructures.PICCOLO_HOUSE),
				36925814, biomes.getOrThrow(MainTags.Biomes.IS_PLAINSLIKE));
		unique(context, OLDKAI_PILLAR, structures.getOrThrow(DMZStructures.OLDKAI_PILLAR),
				41258963, HolderSet.direct(biomes.getOrThrow(SacredKaiBiomes.SACREDKAI_PLAINS)));
		unique(context, YAMCHA_HOUSE, structures.getOrThrow(DMZStructures.YAMCHA_HOUSE),
				55114477, biomes.getOrThrow(MainTags.Biomes.IS_DESERTLIKE));
		unique(context, TRUNKS_SHIP, structures.getOrThrow(DMZStructures.TRUNKS_SHIP),
				66332211, biomes.getOrThrow(MainTags.Biomes.IS_LAND));
		unique(context, VEGETA_POD, structures.getOrThrow(DMZStructures.VEGETA_POD),
				77889900, biomes.getOrThrow(MainTags.Biomes.IS_ROCKYBIOME));
		unique(context, BUU_HOUSE, structures.getOrThrow(DMZStructures.BUU_HOUSE),
				71727374, biomes.getOrThrow(MainTags.Biomes.IS_PLAINSLIKE));
		unique(context, TREE_OF_MIGHT, structures.getOrThrow(DMZStructures.TREE_OF_MIGHT),
				TREE_OF_MIGHT_SALT, biomes.getOrThrow(MainTags.Biomes.IS_SWAMPLIKE), TREE_OF_MIGHT_MIN_DISTANCE);
		unique(context, GETE_STAR, structures.getOrThrow(DMZStructures.GETE_STAR),
				GETE_STAR_SALT, HolderSet.direct(biomes.getOrThrow(NamekBiomes.AJISSA_PLAINS), biomes.getOrThrow(NamekBiomes.SACRED_LAND)),
				GETE_STAR_MIN_DISTANCE);

		HolderGetter<StructureSet> sets = context.lookup(Registries.STRUCTURE_SET);
		context.register(SAIYAN_CRATER, new StructureSet(
				structures.getOrThrow(DMZStructures.SAIYAN_CRATER),
				new RandomSpreadStructurePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						SAIYAN_CRATER_SALT,
						Optional.of(new StructurePlacement.ExclusionZone(sets.getOrThrow(BuiltinStructureSets.VILLAGES), 6)),
						SAIYAN_CRATER_SPACING,
						SAIYAN_CRATER_SEPARATION,
						RandomSpreadType.LINEAR
				)
		));

		context.register(DEMON_VILLAGE, new StructureSet(
				structures.getOrThrow(DMZStructures.DEMON_VILLAGE),
				new RandomSpreadStructurePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						DEMON_VILLAGE_SALT,
						Optional.empty(),
						DEMON_VILLAGE_SPACING,
						DEMON_VILLAGE_SEPARATION,
						RandomSpreadType.TRIANGULAR
				)
		));

		context.register(GOMAH_CAMP, new StructureSet(
				structures.getOrThrow(DMZStructures.GOMAH_CAMP),
				new RandomSpreadStructurePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						GOMAH_CAMP_SALT,
						Optional.of(new StructurePlacement.ExclusionZone(sets.getOrThrow(DEMON_VILLAGE), 6)),
						GOMAH_CAMP_SPACING,
						GOMAH_CAMP_SEPARATION,
						RandomSpreadType.LINEAR
				)
		));
	}

	public static final int TREE_OF_MIGHT_SALT = 28475016;
	public static final int GETE_STAR_SALT = 61937482;
	private static final int GETE_STAR_MIN_DISTANCE = 1000;
	private static final int SAIYAN_CRATER_SALT = 51738264;
	private static final int SAIYAN_CRATER_SPACING = 40;
	private static final int SAIYAN_CRATER_SEPARATION = 20;
	private static final int TREE_OF_MIGHT_MIN_DISTANCE = 1000;
	private static final int DEMON_VILLAGE_SALT = 73019284;
	private static final int DEMON_VILLAGE_SPACING = 24;
	private static final int DEMON_VILLAGE_SEPARATION = 8;
	private static final int GOMAH_CAMP_SALT = 82640175;
	private static final int GOMAH_CAMP_SPACING = 40;
	private static final int GOMAH_CAMP_SEPARATION = 20;

	private static void unique(BootstapContext<StructureSet> context, ResourceKey<StructureSet> key,
							   Holder<Structure> structure, int salt, HolderSet<Biome> validBiomes) {
		unique(context, key, structure, salt, validBiomes, 0);
	}

	private static void unique(BootstapContext<StructureSet> context, ResourceKey<StructureSet> key,
							   Holder<Structure> structure, int salt, HolderSet<Biome> validBiomes, int minDistanceFromSpawn) {
		context.register(key, new StructureSet(
				structure,
				new BiomeAwareUniquePlacement(
						Vec3i.ZERO,
						StructurePlacement.FrequencyReductionMethod.DEFAULT,
						1.0f,
						salt,
						Optional.empty(),
						validBiomes,
						Rotation.NONE,
						minDistanceFromSpawn
				)
		));
	}

	private static ResourceKey<StructureSet> createKey(String name) {
		return ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}
}
