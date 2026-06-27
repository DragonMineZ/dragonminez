package com.dragonminez.common.init.block.entity;

import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.common.init.block.custom.DragonBallType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.RenderUtils;

public class DragonBallBlockEntity extends BlockEntity implements GeoBlockEntity {
	private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
	private final DragonBallType ballType;
	private final String ballSetId;

	public DragonBallBlockEntity(BlockPos pos, BlockState state, DragonBallType ballType, String ballSetId) {
		super(MainBlockEntities.DRAGON_BALL_BLOCK_ENTITY.get(), pos, state);
		this.ballType = ballType;
		this.ballSetId = ballSetId;
	}

	public DragonBallType getBallType() {
		return ballType;
	}

	public String getBallSetId() {
		return ballSetId;
	}

	public boolean isNamekian() {
		return "namek".equals(ballSetId);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
	}

	@Override
	public double getTick(Object blockEntity) {
		return RenderUtils.getCurrentTick();
	}
}
