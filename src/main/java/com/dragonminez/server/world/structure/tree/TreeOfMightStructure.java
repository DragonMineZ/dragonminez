package com.dragonminez.server.world.structure.tree;

import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import com.dragonminez.server.world.structure.placement.IncrementalStructure;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class TreeOfMightStructure extends Structure implements IncrementalStructure {
	public static final Codec<TreeOfMightStructure> CODEC = simpleCodec(TreeOfMightStructure::new);

	public TreeOfMightStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunkPos = context.chunkPos();
		int centerX = chunkPos.getMiddleBlockX();
		int centerZ = chunkPos.getMiddleBlockZ();
		ChunkGenerator generator = context.chunkGenerator();
		LevelHeightAccessor heightAccessor = context.heightAccessor();
		RandomState randomState = context.randomState();

		TreeOfMightShape shape = TreeOfMightShape.create(centerX, centerZ, TreeOfMightShapes.seedFor(context.seed(), chunkPos),
				heightAccessor.getMinBuildHeight(), heightAccessor.getMaxBuildHeight(), generator.getSeaLevel(),
				(x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState));
		TreeOfMightPiece piece = new TreeOfMightPiece(shape);
		return Optional.of(new GenerationStub(new BlockPos(centerX, shape.baseY(), centerZ), builder -> builder.addPiece(piece)));
	}

	@Override
	public StructureType<?> type() {
		return MainStructureTypes.TREE_OF_MIGHT.get();
	}
}
