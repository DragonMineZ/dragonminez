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

/**
 * Builds a big straight rock elevation/cliff for the desolate rocky desert: a roughly cylindrical column
 * that is straight (vertical) at the base and grows wider toward the top, capped by a flat plateau. Not a
 * peak — a sheer, blocky rise with cliff walls. {@code rocky_stone} body with {@code rocky_cobblestone}
 * strata bands and a flat {@code rocky_dirt} top.
 */
public class RockyCliffFeature extends Feature<NoneFeatureConfiguration> {

    private static final float CYLINDER_FRACTION = 0.45F; // bottom share that stays a straight cylinder
    private static final float TOP_GROWTH = 1.3F;         // how much wider the top is vs the base

    public RockyCliffFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos pos = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();

        if (!level.getBlockState(pos.below()).isSolid()) {
            return false;
        }

        // Wide size range: small risers through big cliffs, all sharing the same cylinder-then-widen shape.
        int height = 8 + random.nextInt(28);                 // 8..35 tall
        float baseRadius = 2.5F + random.nextFloat() * 9.5F; // 2.5..12

        // Mostly round footprint with only a faint, coherent wobble so the cliff face isn't a clean tube.
        int lobes = 3 + random.nextInt(3);
        double phase = random.nextDouble() * Math.PI * 2.0;
        float wobble = 0.04F + random.nextFloat() * 0.06F;

        BlockState stone = MainBlocks.ROCKY_STONE.get().defaultBlockState();
        BlockState cobble = MainBlocks.ROCKY_COBBLESTONE.get().defaultBlockState();
        BlockState dirt = MainBlocks.ROCKY_DIRT.get().defaultBlockState();

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
                    if (dist > edge) {
                        continue;
                    }

                    BlockPos placePos = pos.offset(x, y, z);
                    if (!level.isEmptyBlock(placePos) && !level.getBlockState(placePos).is(BlockTags.REPLACEABLE)) {
                        continue;
                    }

                    BlockState toPlace;
                    if (topLayer) {
                        toPlace = dirt;
                    } else if (strataBand && random.nextInt(4) != 0) {
                        toPlace = cobble;
                    } else {
                        toPlace = stone;
                    }
                    level.setBlock(placePos, toPlace, 2);

                    // Ground the base: extend each base column down to solid terrain so nothing floats
                    // when the surrounding ground is lower than the origin.
                    if (y == 0) {
                        BlockPos below = placePos.below();
                        int safety = 0;
                        while (safety < 48 && (level.isEmptyBlock(below)
                                || level.getBlockState(below).canBeReplaced()
                                || level.getBlockState(below).liquid())) {
                            level.setBlock(below, stone, 2);
                            below = below.below();
                            safety++;
                        }
                    }
                }
            }
        }
        return true;
    }

    /** Straight cylinder for the lower portion, then a smooth widening toward the flat top. */
    private static float radiusAt(float t, float baseRadius) {
        if (t <= CYLINDER_FRACTION) {
            return baseRadius;
        }
        float grow = (t - CYLINDER_FRACTION) / (1.0F - CYLINDER_FRACTION); // 0..1 over the upper portion
        return baseRadius * (1.0F + (TOP_GROWTH - 1.0F) * grow);
    }
}
