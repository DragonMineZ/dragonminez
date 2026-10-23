package com.dragonminez.common.init.item.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SenzuSeedsItem extends Item {

	private final Supplier<? extends Block> cropBlock;

	public SenzuSeedsItem(Supplier<? extends Block> cropBlock) {
		super(new Properties());
		this.cropBlock = cropBlock;
	}

	@Override
	public @NotNull InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		BlockState clickedState = level.getBlockState(clicked);
		BlockPos plantPos = clicked.above();

		if (clickedState.is(Blocks.FARMLAND) && level.isEmptyBlock(plantPos)) {
			if (!level.isClientSide) {
				level.setBlock(plantPos, cropBlock.get().defaultBlockState(), 3);
			}
			Player player = context.getPlayer();
			if (player == null || !player.getAbilities().instabuild) {
				context.getItemInHand().shrink(1);
			}
			return InteractionResult.sidedSuccess(level.isClientSide());
		}
		return InteractionResult.PASS;
	}
}
