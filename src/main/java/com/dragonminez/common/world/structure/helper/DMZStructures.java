package com.dragonminez.common.world.structure.helper;

import com.dragonminez.Reference;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.Map;

public class DMZStructures {
	public static final ResourceKey<Structure> GOKU_HOUSE = createKey("goku_house");
	public static final ResourceKey<Structure> ROSHI_HOUSE = createKey("roshi_house");

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
				ConstantHeight.of(VerticalAnchor.absolute(150)),
				false,
				net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG
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
				ConstantHeight.of(VerticalAnchor.absolute(150)),
				false,
				net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG
		));
	}

	private static ResourceKey<Structure> createKey(String name) {
		return ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(Reference.MOD_ID, name));
	}
}
