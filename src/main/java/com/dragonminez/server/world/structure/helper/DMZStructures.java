package com.dragonminez.server.world.structure.helper;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainTags;
import com.dragonminez.server.world.biome.HTCBiomes;
import com.dragonminez.server.world.biome.NamekBiomes;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
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
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.Map;

public class DMZStructures {
	public static final ResourceKey<Structure> GOKU_HOUSE = createKey("goku_house"),
			ROSHI_HOUSE = createKey("roshi_house"), TIMECHAMBER = createKey("timechamber"),
			ELDER_GURU = createKey("elder_guru");

	public static void bootstrap(BootstapContext<Structure> context) {
		HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
		HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);

		context.register(GOKU_HOUSE, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
						Map.of(),
						GenerationStep.Decoration.RAW_GENERATION,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.GOKU_HOUSE),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(65)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(ROSHI_HOUSE, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
						Map.of(),
						GenerationStep.Decoration.RAW_GENERATION,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.ROSHI_HOUSE),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(65)),
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
				ConstantHeight.of(VerticalAnchor.absolute(31)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(ELDER_GURU, new JigsawStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(MainTags.Biomes.IS_SACREDLAND),
						Map.of(),
						GenerationStep.Decoration.RAW_GENERATION,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.ELDER_GURU),
				1,
				ConstantHeight.of(VerticalAnchor.absolute(65)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));
	}

	private static ResourceKey<Structure> createKey(String name) {
		return ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}
}
