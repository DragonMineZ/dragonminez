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

public class RockyCliffFeature extends Feature<NoneFeatureConfiguration> {
    private static final float CYLINDER_FRACTION = 0.45F;
    private static final float TOP_GROWTH = 1.3F;

    public RockyCliffFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    protected BlockState capState() {
        return MainBlocks.ROCKY_DIRT.get().defaultBlockState();
    }

    protected void decorateTop(WorldGenLevel level, BlockPos capPos, RandomSource random) {
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos pos = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();

        if (FeatureUtil.isInsideDmzStructure(level, pos)) return false;
        if (!level.getBlockState(pos.below()).isSolid()) return false;

        int height = 8 + random.nextInt(28);
        float baseRadius = 2.5F + random.nextFloat() * 9.5F;

        int lobes = 3 + random.nextInt(3);
        double phase = random.nextDouble() * Math.PI * 2.0;
        float wobble = 0.04F + random.nextFloat() * 0.06F;

        BlockState stone = MainBlocks.ROCKY_STONE.get().defaultBlockState();
        BlockState cobble = MainBlocks.ROCKY_COBBLESTONE.get().defaultBlockState();
        BlockState cap = capState();

        int maxReach = Mth.ceil(baseRadius * TOP_GROWTH) + 2;
        int topY = height - 1;

        for (int y = 0; y <= topY; y++) {
            float t = (float) y / (float) topY;
            float radius = radiusAt(t, baseRadius);
            boolean topLayer = y == topY;
            boolean strataBand = (y % 5) == 0 || (y % 8) == 0;

            for (int x = -maxReach; x <= maxReach; x++) {
                for (int z = -maxReach; z <= maxReach; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    double angle = Math.atan2(z, x);
                    double edge = radius + Math.cos(angle * lobes + phase) * (radius * wobble);
                    if (dist > edge) continue;

                    BlockPos placePos = pos.offset(x, y, z);
                    if (!level.isEmptyBlock(placePos) && !level.getBlockState(placePos).is(BlockTags.REPLACEABLE)) continue;

                    BlockState toPlace;
                    if (topLayer) toPlace = cap;
                    else if (strataBand && random.nextInt(4) != 0) toPlace = cobble;
                    else toPlace = stone;
                    level.setBlock(placePos, toPlace, 2);

                    if (y == 0) FeatureUtil.groundColumn(level, placePos, stone);
                    if (topLayer) decorateTop(level, placePos, random);
                }
            }
        }
        return true;
    }

    private static float radiusAt(float t, float baseRadius) {
        if (t <= CYLINDER_FRACTION) return baseRadius;
        float grow = (t - CYLINDER_FRACTION) / (1.0F - CYLINDER_FRACTION);
        return baseRadius * (1.0F + (TOP_GROWTH - 1.0F) * grow);
    }
}
