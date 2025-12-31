package com.dragonminez.server.world.feature;

import com.dragonminez.common.init.MainBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class StoneSpikeFeature extends Feature<NoneFeatureConfiguration> {

	public StoneSpikeFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		BlockPos pos = context.origin();
		RandomSource random = context.random();
		WorldGenLevel level = context.level();

		while (level.isEmptyBlock(pos) && pos.getY() > level.getMinBuildHeight() + 2) pos = pos.below();

		if (!level.getBlockState(pos).is(MainBlocks.ROCKY_STONE.get()) && !level.getBlockState(pos).is(Blocks.GRAVEL) && !level.getBlockState(pos).is(Blocks.COARSE_DIRT)) {
			return false;
		}

		pos = pos.above();
		int height = 12 + random.nextInt(19);
		float maxRadius = 4.0F + random.nextInt(3);
		float minRadius = maxRadius * (0.4F + random.nextFloat() * 0.15F);
		if (minRadius < 2.0F) minRadius = 2.0F;

		for (int i = 0; i < height; ++i) {
			float relativeHeight = (float) i / (float) height;
			float distFromCenter = Math.abs(relativeHeight - 0.5F) * 2.0F;
			float widthFactor = (float) Math.pow(distFromCenter, 1.5);
			float currentRadius = minRadius + (maxRadius - minRadius) * widthFactor;
			if (i == height - 1) {
				currentRadius *= 0.8F;
			}

			int r = Mth.ceil(currentRadius);

			for (int x = -r; x <= r; ++x) {
				for (int z = -r; z <= r; ++z) {
					double distSq = x * x + z * z;
					double maxDistSq = currentRadius * currentRadius;
					if (distSq <= maxDistSq + 1.0) {
						boolean placeBlock = true;
						boolean isEdge = distSq >= (currentRadius - 1.2) * (currentRadius - 1.2);
						if (i >= 4 && isEdge) {
							if (random.nextFloat() < 0.05F) {
								placeBlock = false;
							}
						}

						if (placeBlock) {
							BlockPos placePos = pos.offset(x, i, z);

							if (this.canReplace(level, placePos)) {
								BlockState blockState = MainBlocks.ROCKY_STONE.get().defaultBlockState();

								if (random.nextInt(4) == 0) {
									blockState = MainBlocks.ROCKY_COBBLESTONE.get().defaultBlockState();
								}

								this.setBlock(level, placePos, blockState);

								if (i == 0) {
									BlockPos belowPos = placePos.below();
									int safety = 0;
									while (canReplace(level, belowPos) && safety < 6) {
										this.setBlock(level, belowPos, blockState);
										belowPos = belowPos.below();
										safety++;
									}
								}
							}
						}
					}
				}
			}
		}

		return true;
	}

	public boolean canReplace(WorldGenLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.isAir() || state.is(Blocks.DIRT) || state.is(Blocks.SNOW) || state.is(Blocks.GRASS) || state.liquid() || state.is(MainBlocks.ROCKY_DIRT.get()) || state.canBeReplaced();
	}
}