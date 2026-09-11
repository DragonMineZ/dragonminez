package com.dragonminez.common.init.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;


public class StatCropBlock extends CropBlock {

	private static final float NATURAL_GROWTH_DELAY = 3.0F;
	private static final float BONEMEAL_SUCCESS_CHANCE = 0.25F;

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

	@Override
	public void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
		if (!level.isAreaLoaded(pos, 1)) return;
		if (level.getRawBrightness(pos, 0) < 9) return;

		int age = this.getAge(state);
		if (age >= this.getMaxAge()) return;

		float speed = getGrowthSpeed(this, level, pos);
		boolean shouldGrow = random.nextInt((int) (NATURAL_GROWTH_DELAY * 25.0F / speed) + 1) == 0;
		if (ForgeHooks.onCropsGrowPre(level, pos, state, shouldGrow)) {
			level.setBlock(pos, this.getStateForAge(age + 1), 2);
			ForgeHooks.onCropsGrowPost(level, pos, state);
		}
	}

	@Override
	public boolean isBonemealSuccess(@NotNull Level level, @NotNull RandomSource random, @NotNull BlockPos pos, @NotNull BlockState state) {
		return random.nextFloat() < BONEMEAL_SUCCESS_CHANCE;
	}

	@Override
	protected int getBonemealAgeIncrease(@NotNull Level level) {
		return 1;
	}
}
