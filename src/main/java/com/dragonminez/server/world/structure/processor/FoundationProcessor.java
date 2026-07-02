package com.dragonminez.server.world.structure.processor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

		Map<Long, StructureBlockInfo> lowestByColumn = new HashMap<>();
		for (StructureBlockInfo info : processedInfos) {
			if (info.state().isAir()) continue;
			BlockPos p = info.pos();
			if (box != null && !box.isInside(p)) continue;

			long column = ChunkPos.asLong(p.getX(), p.getZ());
			StructureBlockInfo current = lowestByColumn.get(column);
			if (current == null || p.getY() < current.pos().getY()) {
				lowestByColumn.put(column, info);
			}
		}
		if (lowestByColumn.isEmpty()) return processedInfos;

		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		int worldFloor = level.getMinBuildHeight();
		for (StructureBlockInfo info : lowestByColumn.values()) {
			BlockPos p = info.pos();
			int bottomY = p.getY();
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
