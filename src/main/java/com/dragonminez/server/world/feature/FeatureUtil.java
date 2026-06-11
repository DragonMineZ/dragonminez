package com.dragonminez.server.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

final class FeatureUtil {
    private static final int MAX_GROUND_DEPTH = 64;
    private FeatureUtil() {}

    static void groundColumn(WorldGenLevel level, BlockPos basePos, BlockState fill) {
        BlockPos.MutableBlockPos cursor = basePos.mutable().move(0, -1, 0);
        for (int depth = 0; depth < MAX_GROUND_DEPTH; depth++) {
            BlockState state = level.getBlockState(cursor);
            if (!(state.isAir() || state.canBeReplaced() || state.liquid())) break;
            level.setBlock(cursor, fill, 2);
            cursor.move(0, -1, 0);
        }
    }
}
