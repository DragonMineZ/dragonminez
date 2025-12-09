package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.MainBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;

import javax.annotation.Nullable;

public class FlammableRotatedPillarBlock extends RotatedPillarBlock {
	public FlammableRotatedPillarBlock(Properties properties) {
		super(properties);
	}

	@Override
	public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return true;
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return 5;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return 5;
	}

	@Override
	public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction toolAction, boolean simulate) {
		if(context.getItemInHand().getItem() instanceof AxeItem) {
			if(state.is(MainBlocks.NAMEK_AJISSA_WOOD.get())) {
				return MainBlocks.NAMEK_STRIPPED_AJISSA_WOOD.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
			}
			if(state.is(MainBlocks.NAMEK_SACRED_WOOD.get())) {
				return MainBlocks.NAMEK_STRIPPED_SACRED_WOOD.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
			}
		}

		return super.getToolModifiedState(state, context, toolAction, simulate);
	}
}
