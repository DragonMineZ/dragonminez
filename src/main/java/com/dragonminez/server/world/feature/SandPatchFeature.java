package com.dragonminez.server.world.feature;

import com.dragonminez.common.init.MainBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SandPatchFeature extends Feature<NoneFeatureConfiguration> {

	public SandPatchFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		BlockPos origin = context.origin();
		WorldGenLevel level = context.level();
		RandomSource random = context.random();

		if (FeatureUtil.isInsideDmzStructure(level, origin)) return false;

		BlockState sand = Blocks.SAND.defaultBlockState();

		float radius = 2.0F + random.nextFloat() * 2.5F;
		int lobes = 3 + random.nextInt(3);
		double phase = random.nextDouble() * Math.PI * 2.0;
		float wobble = 0.3F + random.nextFloat() * 0.3F;
		int maxReach = Mth.ceil(radius * (1.0F + wobble)) + 1;

		boolean placed = false;

		for (int x = -maxReach; x <= maxReach; x++) {
			for (int z = -maxReach; z <= maxReach; z++) {
				double dist = Math.sqrt(x * x + z * z);
				double angle = Math.atan2(z, x);
				double edge = radius + Math.cos(angle * lobes + phase) * (radius * wobble);
				if (dist > edge) continue;

				int gx = origin.getX() + x;
				int gz = origin.getZ() + z;
				int topY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, gx, gz) - 1;
				BlockPos topPos = new BlockPos(gx, topY, gz);
				BlockState topState = level.getBlockState(topPos);

				if (topState.is(MainBlocks.SACRED_PLANET_GRASS_BLOCK.get())
						|| topState.is(MainBlocks.ROCKY_DIRT.get())
						|| topState.is(MainBlocks.ROCKY_STONE.get())) {
					level.setBlock(topPos, sand, 2);
					placed = true;
				}
			}
		}
		return placed;
	}
}
