package com.dragonminez.server.world.structure.helper;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainTags;
import com.dragonminez.server.world.structure.TallJigsawStructure;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.Optional;

import java.util.Map;

public class DMZStructures {
	public static final ResourceKey<Structure> GOKU_HOUSE = createKey("goku_house"),
			ROSHI_HOUSE = createKey("roshi_house"), TIMECHAMBER = createKey("timechamber"),
			ELDER_GURU = createKey("elder_guru"), KAMILOOKOUT = createKey("kamilookout"),
			GERO_LAB = createKey("gero_lab"), BABIDI = createKey("babidi"),
			CELL_ARENA = createKey("cell_arena"), FRIEZA_SHIP = createKey("frieza_ship"),
			PICCOLO_HOUSE = createKey("piccolo_house"), OLDKAI_PILLAR = createKey("oldkai_pillar"),
			YAMCHA_HOUSE = createKey("yamcha_house"), TRUNKS_SHIP = createKey("trunks_ship"),
			VEGETA_POD = createKey("vegeta_pod");

	public static void bootstrap(BootstapContext<Structure> context) {
		HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
		HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);

		context.register(GOKU_HOUSE, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.GOKU_HOUSE),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(ROSHI_HOUSE, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.ROSHI_HOUSE),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(TIMECHAMBER, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(MainTags.Biomes.IS_HTC),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.TIMECHAMBER),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(ELDER_GURU, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(MainTags.Biomes.IS_SACREDLAND),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.ELDER_GURU),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(KAMILOOKOUT, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.KAMILOOKOUT),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(GERO_LAB, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(MainTags.Biomes.IS_ROCKYBIOME),
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
						biomes.getOrThrow(MainTags.Biomes.IS_LAND),
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
				90
		));

		context.register(CELL_ARENA, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.CELL_ARENA),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(FRIEZA_SHIP, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(MainTags.Biomes.IS_NAMEK),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.FRIEZA_SHIP),
				1,
				UniformHeight.of(VerticalAnchor.absolute(40), VerticalAnchor.absolute(80)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(PICCOLO_HOUSE, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(BiomeTags.IS_MOUNTAIN),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.PICCOLO_HOUSE),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(OLDKAI_PILLAR, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(MainTags.Biomes.IS_SACREDKAI),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.OLDKAI_PILLAR),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(YAMCHA_HOUSE, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.YAMCHA_HOUSE),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(TRUNKS_SHIP, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(MainTags.Biomes.IS_LAND),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.TRUNKS_SHIP),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(VEGETA_POD, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(MainTags.Biomes.IS_LAND),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.VEGETA_POD),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));
	}

	private static ResourceKey<Structure> createKey(String name) {
		return ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}
}
