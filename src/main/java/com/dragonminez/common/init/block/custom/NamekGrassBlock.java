package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.MainBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

public class NamekGrassBlock extends Block implements BonemealableBlock {

	public NamekGrassBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		if (!world.isClientSide) {
			if (!world.isAreaLoaded(pos, 2)) return;

			BlockState blockAbove = world.getBlockState(pos.above());

			if (blockAbove.getLightBlock(world, pos.above()) > 2 && world.getMaxLocalRawBrightness(pos.above()) < 4) {
				world.setBlockAndUpdate(pos, MainBlocks.NAMEK_DIRT.get().defaultBlockState());
			} else {
				if (world.getMaxLocalRawBrightness(pos.above()) >= 9) {
					for (int i = 0; i < 4; ++i) {
						BlockPos targetPos = pos.offset(
								random.nextInt(3) - 1,
								random.nextInt(3) - 1,
								random.nextInt(3) - 1
						);
						BlockState targetState = world.getBlockState(targetPos);
						BlockState aboveTargetState = world.getBlockState(targetPos.above());

						if (targetState.is(MainBlocks.NAMEK_DIRT.get()) &&
								aboveTargetState.isAir() &&
								world.getMaxLocalRawBrightness(targetPos.above()) >= 4) {
							world.setBlockAndUpdate(targetPos, MainBlocks.NAMEK_GRASS_BLOCK.get().defaultBlockState());
						}
					}
				}
			}
		}
	}


	@Override
	public boolean isValidBonemealTarget(LevelReader levelReader, BlockPos pos, BlockState state, boolean isClient) {
		return levelReader.getBlockState(pos.above()).isAir();
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		BlockPos abovePos = pos.above();

		for (int i = 0; i < 128; ++i) {
			BlockPos targetPos = abovePos.offset(
					random.nextInt(3) - 1,
					(random.nextInt(3) - 1) * random.nextInt(3) / 2,
					random.nextInt(3) - 1
			);

			if (level.getBlockState(targetPos).isAir() && level.getBlockState(targetPos.below()).is(this)) {
				if (random.nextFloat() < 0.7F) {
					level.setBlock(targetPos, MainBlocks.NAMEK_GRASS.get().defaultBlockState(), 3);
				}
				else if (random.nextFloat() < 0.9F) {
					level.setBlock(targetPos, MainBlocks.NAMEK_FERN.get().defaultBlockState(), 3);
				}
				else {
					BlockState flower = pickRandomFlower(random);
					if (flower != null) {
						level.setBlock(targetPos, flower, 3);
					}
				}
			}
		}
	}

	private BlockState pickRandomFlower(RandomSource random) {
		BlockState[] flowers = {
				MainBlocks.CATHARANTHUS_ROSEUS_FLOWER.get().defaultBlockState(),
				MainBlocks.AMARYLLIS_FLOWER.get().defaultBlockState(),
				MainBlocks.MARIGOLD_FLOWER.get().defaultBlockState(),
				MainBlocks.TRILLIUM_FLOWER.get().defaultBlockState(),
				MainBlocks.CHRYSANTHEMUM_FLOWER.get().defaultBlockState()
		};
		return flowers[random.nextInt(flowers.length)];
	}

}
