package com.dragonminez.server.world.feature;

import com.dragonminez.common.init.MainBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class GrassyCliffFeature extends RockyCliffFeature {

	public GrassyCliffFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	protected BlockState capState() {
		return MainBlocks.SACRED_PLANET_GRASS_BLOCK.get().defaultBlockState();
	}

	@Override
	protected void decorateTop(WorldGenLevel level, BlockPos capPos, RandomSource random) {
		FeatureUtil.scatterSacredKaiPlant(level, capPos.above(), random);
	}
}
