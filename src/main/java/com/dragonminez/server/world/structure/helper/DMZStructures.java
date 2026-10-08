package com.dragonminez.server.world.structure.helper;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainTags;
import com.dragonminez.server.world.biome.DemonRealmBiomes;
import com.dragonminez.server.world.structure.DemonVillageStructure;
import com.dragonminez.server.world.structure.TallJigsawStructure;
import com.dragonminez.server.world.structure.BossStructures.SaiyanCraterStructure;
import com.dragonminez.server.world.structure.BossStructures.GeteStarStructure;
import com.dragonminez.server.world.structure.BossStructures.GomahCampStructure;
import com.dragonminez.server.world.structure.BossStructures.GomahCradleStructure;
import com.dragonminez.server.world.structure.BossStructures.NamekRuinsStructure;
import com.dragonminez.server.world.structure.BossStructures.TreeOfMightStructure;
import com.dragonminez.server.world.structure.fitted.FittedTemplateStructure;
import com.dragonminez.server.world.structure.fitted.TerrainFit;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.Map;
import java.util.Optional;

public class DMZStructures {
	public static final ResourceKey<Structure> GOKU_HOUSE = createKey("goku_house"),
			ROSHI_HOUSE = createKey("roshi_house"), TIMECHAMBER = createKey("timechamber"),
			ELDER_GURU = createKey("elder_guru"), KAMILOOKOUT = createKey("kamilookout"),
			GERO_LAB = createKey("gero_lab"), BABIDI = createKey("babidi"),
			CELL_ARENA = createKey("cell_arena"), FRIEZA_SHIP = createKey("frieza_ship"),
			PICCOLO_HOUSE = createKey("piccolo_house"), OLDKAI_PILLAR = createKey("oldkai_pillar"),
			YAMCHA_HOUSE = createKey("yamcha_house"), TRUNKS_SHIP = createKey("trunks_ship"),
			VEGETA_POD = createKey("vegeta_pod"),
			BABA_PALACE = createKey("baba_palace"), TREE_OF_MIGHT = createKey("tree_of_might"),
			SAIYAN_CRATER = createKey("saiyan_crater"), NAMEK_RUINS = createKey("namek_ruins"),
			GETE_STAR = createKey("gete_star"), DEMON_VILLAGE = createKey("demon_village"),
			GOMAH_CAMP = createKey("gomah_camp"), BUU_HOUSE = createKey("buu_house"),
			GOMAH_CRADLE = createKey("gomah_cradle");

	public static void bootstrap(BootstapContext<Structure> context) {
		HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
		HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);

		HolderSet<Biome> overworld = biomes.getOrThrow(BiomeTags.IS_OVERWORLD);
		HolderSet<Biome> land = biomes.getOrThrow(MainTags.Biomes.IS_LAND);
		HolderSet<Biome> namek = biomes.getOrThrow(MainTags.Biomes.IS_NAMEK);

		fitted(context, GOKU_HOUSE, overworld, "goku_house", TerrainFit.terrain(3, 24, 1.0F, 64, 64));
		fitted(context, ROSHI_HOUSE, overworld, "roshi_house", TerrainFit.terrain(1, 24, 0.75F, 64, 32));
		fitted(context, BABA_PALACE, overworld, "baba_palace", TerrainFit.terrain(1, 16, 0.75F, 64, 32));
		fitted(context, TIMECHAMBER, biomes.getOrThrow(MainTags.Biomes.IS_HTC), "timechamber", TerrainFit.flush(0), Optional.of(Rotation.NONE));
		fitted(context, ELDER_GURU, namek, "elder_guru", TerrainFit.terrain(0, 20, 1.0F, 64, 64));
		fitted(context, KAMILOOKOUT, overworld, "kamilookout", TerrainFit.pillar(0, 0, 64, 0));
		fitted(context, CELL_ARENA, overworld, "cell_arena", TerrainFit.terrain(4, 20, 1.0F, 64, 64));
		fitted(context, FRIEZA_SHIP, namek, "frieza_ship", TerrainFit.pillar(0, 12, 48, 48));
		fitted(context, PICCOLO_HOUSE, land, "piccolo_house", TerrainFit.terrain(1, 16, 1.0F, 64, 64));
		fitted(context, OLDKAI_PILLAR, biomes.getOrThrow(MainTags.Biomes.IS_SACREDKAI), "oldkai_pillar", TerrainFit.pillar(0, 6, 64, 32));
		fitted(context, YAMCHA_HOUSE, overworld, "yamcha_house", TerrainFit.pillar(0, 8, 32, 32));
		fitted(context, BUU_HOUSE, overworld, "buu_house", TerrainFit.terrain(1, 20, 1.0F, 64, 64));
		fitted(context, TRUNKS_SHIP, land, "trunks_ship", TerrainFit.pillar(0, 4, 16, 24));
		fitted(context, VEGETA_POD, land, "vegeta_pod", TerrainFit.terrain(0, 12, 1.0F, 48, 48));

		context.register(GERO_LAB, new JigsawStructure(
				new Structure.StructureSettings(
						land,
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.GERO_LAB),
				3,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(BABIDI, new TallJigsawStructure(
				new Structure.StructureSettings(
						land,
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.BABIDI),
				Optional.empty(),
				3,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Optional.of(Heightmap.Types.WORLD_SURFACE_WG),
				160,
				63
		));

		context.register(TREE_OF_MIGHT, new TreeOfMightStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				)
		));

		context.register(SAIYAN_CRATER, new SaiyanCraterStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
						Map.of(),
						GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
						TerrainAdjustment.NONE
				)
		));

		context.register(NAMEK_RUINS, new NamekRuinsStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(MainTags.Biomes.IS_NAMEK),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.BEARD_THIN
				)
		));

		context.register(GETE_STAR, new GeteStarStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(MainTags.Biomes.IS_NAMEK),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				)
		));

		context.register(DEMON_VILLAGE, new DemonVillageStructure(
				new Structure.StructureSettings(
						HolderSet.direct(biomes.getOrThrow(DemonRealmBiomes.THIRD_DEMON_WORLD)),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				)
		));

		context.register(GOMAH_CAMP, new GomahCampStructure(
				new Structure.StructureSettings(
						HolderSet.direct(biomes.getOrThrow(DemonRealmBiomes.FIRST_DEMON_WORLD),
								biomes.getOrThrow(DemonRealmBiomes.SECOND_DEMON_WORLD),
								biomes.getOrThrow(DemonRealmBiomes.THIRD_DEMON_WORLD)),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.BEARD_THIN
				)
		));

		context.register(GOMAH_CRADLE, new GomahCradleStructure(
				new Structure.StructureSettings(
						HolderSet.direct(biomes.getOrThrow(DemonRealmBiomes.FIRST_DEMON_WORLD),
								biomes.getOrThrow(DemonRealmBiomes.SECOND_DEMON_WORLD),
								biomes.getOrThrow(DemonRealmBiomes.THIRD_DEMON_WORLD)),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				)
		));
	}

	private static void fitted(BootstapContext<Structure> context, ResourceKey<Structure> key, HolderSet<Biome> biomes,
							   String template, TerrainFit fit) {
		fitted(context, key, biomes, template, fit, Optional.empty());
	}

	private static void fitted(BootstapContext<Structure> context, ResourceKey<Structure> key, HolderSet<Biome> biomes,
							   String template, TerrainFit fit, Optional<Rotation> rotation) {
		context.register(key, new FittedTemplateStructure(
				new Structure.StructureSettings(biomes, Map.of(), GenerationStep.Decoration.TOP_LAYER_MODIFICATION, TerrainAdjustment.NONE),
				ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, template), fit, rotation));
	}

	private static ResourceKey<Structure> createKey(String name) {
		return ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}
}
