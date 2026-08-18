package com.dragonminez.common.init.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;


public class StatCropBlock extends CropBlock {

	private final Supplier<? extends ItemLike> seedItem;
	private final Supplier<? extends Block> soilBlock;

	public StatCropBlock(Properties properties, Supplier<? extends ItemLike> seedItem) {
		this(properties, seedItem, null);
	}

	public StatCropBlock(Properties properties, Supplier<? extends ItemLike> seedItem, Supplier<? extends Block> soilBlock) {
		super(properties);
		this.seedItem = seedItem;
		this.soilBlock = soilBlock;
	}

	@Override
	protected @NotNull ItemLike getBaseSeedId() {
		return seedItem.get();
	}

	@Override
	protected boolean mayPlaceOn(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
		if (soilBlock == null) {
			return super.mayPlaceOn(state, level, pos);
		}
		return state.is(soilBlock.get());
	}
}
