package com.dragonminez.server.world.feature;

import com.dragonminez.common.init.MainBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class RockyPeakFeature extends Feature<NoneFeatureConfiguration> {
    public RockyPeakFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos pos = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();

        int height = random.nextInt(20) + 25;
        int baseRadius = random.nextInt(3) + 6;

        if (!level.getBlockState(pos.below()).isSolid()) {
            return false;
        }

        for (int y = 0; y < height; y++) {
            float progress = (float) y / height;
            float currentRadius;

            if (progress < 0.85f) {
                currentRadius = baseRadius - (progress * 1.5f);
            } else {
                currentRadius = Math.max(4.0F, baseRadius - 2.0f);
            }

            int loopRadius = (int) Math.ceil(currentRadius);

            for (int x = -loopRadius; x <= loopRadius; x++) {
                for (int z = -loopRadius; z <= loopRadius; z++) {
                    if (x * x + z * z <= (currentRadius * currentRadius) + random.nextFloat() * 2.0F) {
                        BlockPos placePos = pos.offset(x, y, z);

                        if (level.isEmptyBlock(placePos) || level.getBlockState(placePos).is(BlockTags.REPLACEABLE)) {
                            level.setBlock(placePos, MainBlocks.ROCKY_STONE.get().defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
        return true;
    }
}