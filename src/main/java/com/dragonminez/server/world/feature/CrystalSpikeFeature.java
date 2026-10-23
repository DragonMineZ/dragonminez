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

public class CrystalSpikeFeature extends Feature<NoneFeatureConfiguration> {

	public CrystalSpikeFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();

		BlockPos ground = findGround(level, context.origin());
		if (ground == null) return false;
		if (FeatureUtil.isInsideDmzStructure(level, ground)) return false;

		placeSpike(level, random, ground.above(), 12 + random.nextInt(18), 2.2F + random.nextFloat() * 1.8F);

		int satellites = 2 + random.nextInt(4);
		for (int i = 0; i < satellites; i++) {
			BlockPos offset = ground.offset(random.nextInt(15) - 7, 4, random.nextInt(15) - 7);
			BlockPos satelliteGround = findGround(level, offset);
			if (satelliteGround == null) continue;
			placeSpike(level, random, satelliteGround.above(), 5 + random.nextInt(11), 1.2F + random.nextFloat() * 1.3F);
		}
		return true;
	}

	private static BlockPos findGround(WorldGenLevel level, BlockPos start) {
		BlockPos.MutableBlockPos cursor = start.mutable();
		int floor = level.getMinBuildHeight() + 2;
		while (cursor.getY() > floor && level.isEmptyBlock(cursor)) {
			cursor.move(0, -1, 0);
		}
		if (cursor.getY() <= floor) return null;
		return isHellGround(level.getBlockState(cursor)) ? cursor.immutable() : null;
	}

	private static boolean isHellGround(BlockState state) {
		return state.is(MainBlocks.HELL_STONE.get())
				|| state.is(MainBlocks.HELL_GROUND.get())
				|| state.is(MainBlocks.HELL_DEEPSTONE.get())
				|| state.is(Blocks.CALCITE)
				|| state.is(Blocks.PURPLE_CONCRETE)
				|| state.is(Blocks.BLACKSTONE)
				|| state.is(Blocks.AMETHYST_BLOCK)
				|| state.is(Blocks.PEARLESCENT_FROGLIGHT);
	}

	private void placeSpike(WorldGenLevel level, RandomSource random, BlockPos base, int height, float baseRadius) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		for (int i = 0; i < height; i++) {
			float taper = 1.0F - (float) i / (float) height;
			float currentRadius = baseRadius * (float) Math.pow(taper, 0.65D);
			int r = Mth.ceil(currentRadius);
			boolean tip = i >= height - 2;

			for (int x = -r; x <= r; x++) {
				for (int z = -r; z <= r; z++) {
					if (x * x + z * z > currentRadius * currentRadius + 0.6D) continue;
					cursor.set(base.getX() + x, base.getY() + i, base.getZ() + z);
					if (!canReplace(level, cursor)) continue;
					this.setBlock(level, cursor, crystalState(random, i, tip));
				}
			}
		}
	}

	private static BlockState crystalState(RandomSource random, int layer, boolean tip) {
		if (tip) return Blocks.MAGENTA_CONCRETE.defaultBlockState();
		if (layer <= 1 && random.nextInt(3) == 0) return MainBlocks.HELL_DEEPSTONE.get().defaultBlockState();
		if (random.nextInt(5) == 0) return Blocks.MAGENTA_CONCRETE.defaultBlockState();
		if (random.nextInt(9) == 0) return MainBlocks.HELL_STONE.get().defaultBlockState();
		return Blocks.AMETHYST_BLOCK.defaultBlockState();
	}

	private static boolean canReplace(WorldGenLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.isAir() || state.canBeReplaced() || isHellGround(state);
	}
}
