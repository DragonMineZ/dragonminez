package com.dragonminez.server.world.feature;

import com.dragonminez.common.init.MainBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class RockClusterFeature extends Feature<NoneFeatureConfiguration> {

	private static final int MIN_SEPARATION = 7;

	public RockClusterFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		BlockPos origin = context.origin();
		WorldGenLevel level = context.level();
		RandomSource random = context.random();

		if (FeatureUtil.isInsideDmzStructure(level, origin)) return false;
		if (hasNearbyCluster(level, origin)) return false;

		BlockState stone = MainBlocks.ROCKY_STONE.get().defaultBlockState();
		BlockState cobble = MainBlocks.ROCKY_COBBLESTONE.get().defaultBlockState();

		int boulders = 1 + random.nextInt(3);
		int clusterRadius = 2 + random.nextInt(2);
		boolean placedAny = false;

		for (int i = 0; i < boulders; i++) {
			int gx = origin.getX() + Mth.nextInt(random, -clusterRadius, clusterRadius);
			int gz = origin.getZ() + Mth.nextInt(random, -clusterRadius, clusterRadius);
			int gy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, gx, gz);
			BlockPos base = new BlockPos(gx, gy, gz);

			if (!level.getBlockState(base.below()).isSolid()) continue;
			if (!level.getFluidState(base).isEmpty()) continue;

			float radius = 1.3F + random.nextFloat() * 1.5F;
			int height = 1 + random.nextInt(2);

			for (int y = 0; y <= height; y++) {
				float layerRadius = radius * (1.0F - (float) y / (float) (height + 1));
				int lr = Mth.ceil(layerRadius);
				for (int x = -lr; x <= lr; x++) {
					for (int z = -lr; z <= lr; z++) {
						if (Math.sqrt(x * x + z * z) > layerRadius + 0.3) continue;
						BlockPos placePos = base.offset(x, y, z);
						if (level.isEmptyBlock(placePos) || level.getBlockState(placePos).is(BlockTags.REPLACEABLE)) {
							level.setBlock(placePos, random.nextInt(4) == 0 ? cobble : stone, 2);
							if (y == 0) FeatureUtil.groundColumn(level, placePos, stone);
						}
					}
				}
			}
			placedAny = true;
		}
		return placedAny;
	}

	private boolean hasNearbyCluster(WorldGenLevel level, BlockPos origin) {
		for (int dx = -MIN_SEPARATION; dx <= MIN_SEPARATION; dx += 2) {
			for (int dz = -MIN_SEPARATION; dz <= MIN_SEPARATION; dz += 2) {
				if (dx == 0 && dz == 0) continue;
				int gx = origin.getX() + dx;
				int gz = origin.getZ() + dz;
				int topY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, gx, gz) - 1;
				BlockState topState = level.getBlockState(new BlockPos(gx, topY, gz));
				if (topState.is(MainBlocks.ROCKY_STONE.get()) || topState.is(MainBlocks.ROCKY_COBBLESTONE.get())) {
					return true;
				}
			}
		}
		return false;
	}
}
