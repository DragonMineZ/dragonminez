package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.MainItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MightTreeFruitBlock extends Block {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(2.0D, 2.0D, 2.0D, 14.0D, 14.0D, 14.0D),
			Block.box(7.0D, 14.0D, 7.0D, 9.0D, 16.0D, 9.0D));

	public MightTreeFruitBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
		return new ItemStack(MainItems.MIGHT_TREE_FRUIT.get());
	}
}
