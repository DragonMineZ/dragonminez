package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.MainBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;

public class NamekAjissaSaplingBlock extends SaplingBlock {

	public NamekAjissaSaplingBlock(AbstractTreeGrower grower, Properties properties) {
		super(grower, properties);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return state.is(MainBlocks.NAMEK_GRASS_BLOCK.get()) || state.is(Blocks.GRASS_BLOCK) || super.mayPlaceOn(state, worldIn, pos);
	}
}
