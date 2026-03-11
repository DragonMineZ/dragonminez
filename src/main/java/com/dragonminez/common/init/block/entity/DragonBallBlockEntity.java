package com.dragonminez.common.init.block.entity;

import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.common.init.block.custom.DragonBallType;
import lombok.Getter;
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
    @Getter
    private final DragonBallType ballType;
    @Getter
    private final String ballName;

    public DragonBallBlockEntity(BlockPos pPos, BlockState pBlockState, DragonBallType ballType, String ballName) {
        super(MainBlockEntities.DRAGON_BALL_ENTITIES.get(ballName + ballType.getStars()).get(), pPos, pBlockState);
        this.ballType = ballType;
        this.ballName = ballName;
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
        return "animation.dball1.idle";
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

