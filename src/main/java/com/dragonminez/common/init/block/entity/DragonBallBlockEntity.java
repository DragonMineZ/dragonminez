package com.dragonminez.common.init.block.entity;

import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.common.init.block.custom.DragonBallType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.RenderUtils;

public class DragonBallBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final DragonBallType ballType;
    private final boolean isNamekian;

    public DragonBallBlockEntity(BlockPos pPos, BlockState pBlockState, DragonBallType ballType, boolean isNamekian) {
        super(MainBlockEntities.DRAGON_BALL_BLOCK_ENTITY.get(), pPos, pBlockState);
        this.ballType = ballType;
        this.isNamekian = isNamekian;
    }

    public DragonBallType getBallType() {
        return ballType;
    }

    public boolean isNamekian() {
        return isNamekian;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        String animationName = getAnimationName();
        tAnimationState.getController().setAnimation(RawAnimation.begin().then(animationName, Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    private String getAnimationName() {
        String prefix = isNamekian ? "dball_namek" : "dball";
        int starNumber = ballType.getStars();
        return "animation." + prefix + starNumber + ".idle";
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object blockEntity) {
        return RenderUtils.getCurrentTick();
    }
}

