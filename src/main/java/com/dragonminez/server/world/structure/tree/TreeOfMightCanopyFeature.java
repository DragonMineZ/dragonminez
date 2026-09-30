package com.dragonminez.server.world.structure.tree;

import com.dragonminez.server.world.structure.helper.DMZStructureSets;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import com.dragonminez.server.world.structure.placement.StructureSpawnPlanner;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

public class TreeOfMightCanopyFeature extends Feature<NoneFeatureConfiguration> {
	private static final int VANILLA_REFERENCE_CHUNKS = 8;

	public TreeOfMightCanopyFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		ServerLevel serverLevel = level.getLevel();
		ChunkPos origin = StructureSpawnPlanner.publishedPositions(serverLevel).get(DMZStructureSets.TREE_OF_MIGHT_SALT);
		if (origin == null) return false;
		ChunkPos chunk = new ChunkPos(context.origin());
		if (Math.max(Math.abs(chunk.x - origin.x), Math.abs(chunk.z - origin.z)) <= VANILLA_REFERENCE_CHUNKS) return false;

		TreeOfMightShape tree = TreeOfMightShapes.planned(serverLevel, context.chunkGenerator(), origin);
		if (!tree.intersectsColumns(chunk.getMinBlockX(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), chunk.getMaxBlockZ())) return false;

		TreeOfMightWriter.place(level, tree, chunk, new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
				chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ()));
		Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(DMZStructures.TREE_OF_MIGHT);
		if (structure != null) level.getChunk(chunk.x, chunk.z).addReferenceForStructure(structure, origin.toLong());
		return true;
	}
}
