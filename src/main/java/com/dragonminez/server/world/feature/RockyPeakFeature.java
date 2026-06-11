package com.dragonminez.server.world.feature;

import com.dragonminez.common.init.MainBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
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

        if (FeatureUtil.isInsideDmzStructure(level, pos)) return false;
        if (!level.getBlockState(pos.below()).isSolid()) return false;

        boolean tall = random.nextInt(4) != 0;
        int height = tall ? 30 + random.nextInt(30) : 16 + random.nextInt(14);
        double sharpness = tall ? 1.6 : 2.0;

        float semiMajor = 4.0F + random.nextFloat() * 8.0F;
        float semiMinor = semiMajor * (0.74F + random.nextFloat() * 0.22F);
        double rot = random.nextDouble() * Math.PI;
        double cosR = Math.cos(rot);
        double sinR = Math.sin(rot);

        int lobes = 2 + random.nextInt(3);
        double phase = random.nextDouble() * Math.PI * 2.0;
        float wobble = 0.05F + random.nextFloat() * 0.08F;

        BlockState stone = MainBlocks.ROCKY_STONE.get().defaultBlockState();
        BlockState cobble = MainBlocks.ROCKY_COBBLESTONE.get().defaultBlockState();
        BlockState dirt = MainBlocks.ROCKY_DIRT.get().defaultBlockState();

        int maxReach = Mth.ceil(semiMajor) + 2;
        int topY = height - 1;

        float tipRadius = 2.0F;
        float tipNd = Math.min(0.5F, tipRadius / semiMajor);

        for (int x = -maxReach; x <= maxReach; x++) {
            for (int z = -maxReach; z <= maxReach; z++) {
                double lx = x * cosR + z * sinR;
                double lz = -x * sinR + z * cosR;
                double angle = Math.atan2(lz, lx);
                double threshold = 1.0 + Math.cos(angle * lobes + phase) * wobble;

                double nd = Math.sqrt((lx * lx) / (semiMajor * semiMajor) + (lz * lz) / (semiMinor * semiMinor)) / Math.sqrt(Math.max(0.0001, threshold));
                if (nd > 1.0) continue;
                float maxT;
                if (nd <= tipNd) maxT = 1.0F;
                else {
                    double body = (nd - tipNd) / (1.0 - tipNd);
                    maxT = (float) (1.0 - Math.pow(body, 1.0 / sharpness));
                }
                int colTop = Math.min(topY, Mth.floor(maxT * topY));

                for (int y = 0; y <= colTop; y++) {
                    BlockPos placePos = pos.offset(x, y, z);
                    if (!level.isEmptyBlock(placePos) && !level.getBlockState(placePos).is(BlockTags.REPLACEABLE)) continue;


                    BlockState toPlace;
                    if (y == colTop) toPlace = dirt;
                    else if (((y % 4) == 0 || (y % 7) == 0) && random.nextInt(4) != 0) toPlace = cobble;
                    else toPlace = stone;
                    level.setBlock(placePos, toPlace, 2);

                    if (y == 0) FeatureUtil.groundColumn(level, placePos, stone);
                }
            }
        }
        return true;
    }
}
