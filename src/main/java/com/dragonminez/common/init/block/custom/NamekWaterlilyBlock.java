package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.MainFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NamekWaterlilyBlock extends BushBlock {
	protected static final VoxelShape AABB = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.5D, 15.0D);

	public NamekWaterlilyBlock(BlockBehaviour.Properties pProperties) {
		super(pProperties);
	}

	@Override
	public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
		super.entityInside(pState, pLevel, pPos, pEntity);
		if (pLevel instanceof ServerLevel && pEntity instanceof Boat) {
			pLevel.destroyBlock(pPos, true, pEntity);
		}
	}

	@Override
	public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
		return AABB;
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos blockBelow = pos.below();
		FluidState fluidStateBelow = level.getFluidState(blockBelow);

		return fluidStateBelow.is(FluidTags.WATER) || fluidStateBelow.getType() == MainFluids.SOURCE_NAMEK.get();
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		FluidState fluidStateBelow = level.getFluidState(pos);
		return fluidStateBelow.is(FluidTags.WATER) || fluidStateBelow.getType() == MainFluids.SOURCE_NAMEK.get();
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockPos posBelow = pos.below();
		FluidState fluidState = level.getFluidState(posBelow);

		if (fluidState.is(FluidTags.WATER) || fluidState.getType() == MainFluids.SOURCE_NAMEK.get()) {
			return this.defaultBlockState();
		}
		return null;
	}
}
