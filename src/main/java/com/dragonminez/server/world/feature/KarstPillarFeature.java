package com.dragonminez.server.world.feature;

import com.dragonminez.common.init.MainBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class KarstPillarFeature extends Feature<NoneFeatureConfiguration> {

    public KarstPillarFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();

        BlockState stone = MainBlocks.ROCKY_STONE.get().defaultBlockState();
        BlockState cobble = MainBlocks.ROCKY_COBBLESTONE.get().defaultBlockState();

        int pillars = 4 + random.nextInt(6);
        int clusterRadius = 5 + random.nextInt(6);
        boolean placedAny = false;

        for (int i = 0; i < pillars; i++) {
            int dx = Mth.nextInt(random, -clusterRadius, clusterRadius);
            int dz = Mth.nextInt(random, -clusterRadius, clusterRadius);
            int gx = origin.getX() + dx;
            int gz = origin.getZ() + dz;
            int gy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, gx, gz);
            BlockPos base = new BlockPos(gx, gy, gz);

            if (!level.getBlockState(base.below()).isSolid()) continue;

            int pillarHeight = 9 + random.nextInt(19);
            float baseRadius = 2.2F + random.nextFloat() * 2.5F;
            float leanX = (random.nextFloat() - 0.5F) * 0.35F;
            float leanZ = (random.nextFloat() - 0.5F) * 0.35F;
            float wobblePhase = random.nextFloat() * Mth.TWO_PI;

            if (buildPinnacle(level, random, base, pillarHeight, baseRadius, leanX, leanZ, wobblePhase, stone, cobble)) placedAny = true;
        }
        return placedAny;
    }

    private boolean buildPinnacle(WorldGenLevel level, RandomSource random, BlockPos base, int height, float baseRadius, float leanX, float leanZ, float wobblePhase, BlockState stone, BlockState cobble) {
        for (int y = 0; y < height; y++) {
            float t = (float) y / (float) (height - 1);
            float radius = baseRadius * (float) Math.pow(1.0 - t, 1.1);
            if (radius < 1.2F) radius = 1.2F;
            int cx = base.getX() + Math.round(leanX * y);
            int cz = base.getZ() + Math.round(leanZ * y);
            int r = Mth.ceil(radius);

            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    double edge = radius + Math.cos(Math.atan2(z, x) * 3 + wobblePhase) * 0.25;
                    if (dist <= edge) {
                        BlockPos placePos = new BlockPos(cx + x, base.getY() + y, cz + z);
                        if (level.isEmptyBlock(placePos) || level.getBlockState(placePos).is(BlockTags.REPLACEABLE)) {
                            level.setBlock(placePos, random.nextInt(5) == 0 ? cobble : stone, 2);
                            if (y == 0) FeatureUtil.groundColumn(level, placePos, stone);
                        }
                    }
                }
            }
        }
        return true;
    }
}
