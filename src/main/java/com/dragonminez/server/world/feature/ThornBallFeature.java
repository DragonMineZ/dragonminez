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

public class ThornBallFeature extends Feature<NoneFeatureConfiguration> {

	public ThornBallFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();

		BlockPos ground = FeatureUtil.findHellSurface(level, context.origin());
		if (ground == null) return false;
		if (FeatureUtil.isInsideDmzStructure(level, ground)) return false;

		boolean placed = placeBall(level, random, ground, 2 + random.nextInt(2));

		int satellites = random.nextInt(2);
		for (int i = 0; i < satellites; i++) {
			BlockPos offset = ground.offset(random.nextInt(13) - 6, 6, random.nextInt(13) - 6);
			BlockPos satelliteGround = FeatureUtil.findHellSurface(level, offset);
			if (satelliteGround == null) continue;
			placed |= placeBall(level, random, satelliteGround, 1 + random.nextInt(2));
		}
		return placed;
	}

	private boolean placeBall(WorldGenLevel level, RandomSource random, BlockPos ground, int radius) {
		BlockPos center = ground.above(radius);
		if (center.getY() + radius + 2 >= level.getMaxBuildHeight() - 1) return false;

		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		double radiusSq = (radius + 0.45D) * (radius + 0.45D);
		boolean placed = false;

		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					if (x * x + y * y + z * z > radiusSq) continue;
					cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
					placed |= placeThorn(level, cursor, coreState(random));
				}
			}
		}

		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					if (x == 0 && y == 0 && z == 0) continue;
					if (random.nextInt(4) == 0) continue;

					double length = Math.sqrt(x * x + y * y + z * z);
					int spike = 1 + random.nextInt(radius);
					for (int step = 1; step <= spike; step++) {
						double distance = radius + step - 0.5D;
						cursor.set(
								center.getX() + Mth.floor(x / length * distance + 0.5D),
								center.getY() + Mth.floor(y / length * distance + 0.5D),
								center.getZ() + Mth.floor(z / length * distance + 0.5D)
						);
						placed |= placeThorn(level, cursor, spikeState(random));
					}
				}
			}
		}
		return placed;
	}

	private static BlockState coreState(RandomSource random) {
		if (random.nextInt(4) == 0) return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
		if (random.nextInt(9) == 0) return MainBlocks.HELL_DEEPSTONE.get().defaultBlockState();
		return Blocks.BLACKSTONE.defaultBlockState();
	}

	private static BlockState spikeState(RandomSource random) {
		if (random.nextInt(6) == 0) return MainBlocks.HELL_DEEPSTONE.get().defaultBlockState();
		return Blocks.BLACKSTONE.defaultBlockState();
	}

	private boolean placeThorn(WorldGenLevel level, BlockPos pos, BlockState state) {
		if (pos.getY() <= level.getMinBuildHeight() + 1 || pos.getY() >= level.getMaxBuildHeight() - 1) return false;
		BlockState current = level.getBlockState(pos);
		if (!current.isAir() && !current.canBeReplaced()) return false;
		this.setBlock(level, pos, state);
		return true;
	}
}
