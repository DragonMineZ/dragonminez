package com.dragonminez.server.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CandyOrbFeature extends Feature<NoneFeatureConfiguration> {
	private static final Block[] SHELLS = {
			Blocks.PINK_CONCRETE,
			Blocks.MAGENTA_CONCRETE,
			Blocks.RED_CONCRETE,
			Blocks.ORANGE_CONCRETE,
			Blocks.YELLOW_CONCRETE,
			Blocks.LIME_CONCRETE,
			Blocks.LIGHT_BLUE_CONCRETE,
			Blocks.BLUE_CONCRETE,
			Blocks.PURPLE_CONCRETE,
			Blocks.WHITE_CONCRETE
	};

	public CandyOrbFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();

		int radius = 1 + random.nextInt(4);
		if (!hasRoom(level, origin, radius)) return false;

		BlockState shell = SHELLS[random.nextInt(SHELLS.length)].defaultBlockState();
		double squash = 1.0D + random.nextDouble() * 0.35D;
		double radiusSq = (radius + 0.1D) * (radius + 0.1D);
		int verticalRadius = Math.max(1, Mth.floor(radius / squash));
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				for (int y = -verticalRadius; y <= verticalRadius; y++) {
					double scaledY = y * squash;
					if (x * x + scaledY * scaledY + z * z > radiusSq) continue;
					cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
					if (!level.isEmptyBlock(cursor)) continue;
					this.setBlock(level, cursor, shell);
				}
			}
		}

		return true;
	}

	private static boolean hasRoom(WorldGenLevel level, BlockPos origin, int radius) {
		int clearance = radius + 3;
		if (origin.getY() - clearance <= level.getMinBuildHeight() + 1) return false;
		if (origin.getY() + clearance >= level.getMaxBuildHeight() - 1) return false;

		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int x = -clearance; x <= clearance; x += 2) {
			for (int z = -clearance; z <= clearance; z += 2) {
				for (int y = -clearance; y <= clearance; y += 2) {
					cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
					if (!level.isEmptyBlock(cursor)) return false;
				}
			}
		}
		return true;
	}
}
