package com.dragonminez.common.init.block.custom;

import com.dragonminez.common.init.MainBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.PlantType;

import java.util.function.Supplier;

public class NamekPlantsBlock extends FlowerBlock {

	public NamekPlantsBlock(Supplier<MobEffect> effectSupplier, int p_53513_, Properties p_53514_) {
		super(effectSupplier, p_53513_, p_53514_);
	}

	@Override
	protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
		return pState.is(MainBlocks.NAMEK_GRASS_BLOCK.get()) || pState.is(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get());
	}

	@Override
	public PlantType getPlantType(BlockGetter level, BlockPos pos) {
		return PlantType.get("custom");
	}
}
