package com.dragonminez.server.world.structure.processor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import javax.annotation.Nullable;
import java.util.List;

public class FoundationProcessor extends StructureProcessor {
	public static final Codec<FoundationProcessor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, 256).optionalFieldOf("max_depth", 32).forGetter(p -> p.maxDepth)
	).apply(instance, FoundationProcessor::new));

	private final int maxDepth;

	public FoundationProcessor(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	@Nullable
	@Override
	public StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos, StructureBlockInfo original, StructureBlockInfo relative, StructurePlaceSettings settings) {
		return relative;
	}

	@Override
	public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos offset, BlockPos pos, List<StructureBlockInfo> originalInfos, List<StructureBlockInfo> processedInfos, StructurePlaceSettings settings) {
		BoundingBox box = settings.getBoundingBox();

		int bottomY = Integer.MAX_VALUE;
		for (StructureBlockInfo info : processedInfos) {
			if (info.state().isAir()) continue;
			if (info.pos().getY() < bottomY) bottomY = info.pos().getY();
		}
		if (bottomY == Integer.MAX_VALUE) return processedInfos;

		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		int worldFloor = level.getMinBuildHeight();
		for (StructureBlockInfo info : processedInfos) {
			BlockPos p = info.pos();
			if (p.getY() != bottomY || info.state().isAir()) continue;
			if (box != null && !box.isInside(p)) continue;

			BlockState fill = info.state();
			for (int y = bottomY - 1; y >= worldFloor && y >= bottomY - this.maxDepth; y--) {
				cursor.set(p.getX(), y, p.getZ());
				BlockState existing = level.getBlockState(cursor);
				if (!existing.isAir() && existing.getFluidState().isEmpty() && !existing.canBeReplaced()) {
					break;
				}
				level.setBlock(cursor, fill, 2);
			}
		}
		return processedInfos;
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return MainStructureProcessors.FOUNDATION.get();
	}
}
