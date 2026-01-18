package com.dragonminez.mixin.client;

import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

@Mixin(AbstractClientPlayer.class)
public abstract class PlayerGeoAnimatableMixin implements IPlayerAnimatable {

    @Unique
    private boolean dragonminez$useAttack2 = false;

    @Unique
    private boolean dragonminez$isPlayingAttack = false;

    @Unique
    private boolean dragonminez$isCreativeFlying = false;

    @Override
    public void dragonminez$setUseAttack2(boolean useAttack2) {
        this.dragonminez$useAttack2 = useAttack2;
    }

    @Override
    public boolean dragonminez$useAttack2() {
        return this.dragonminez$useAttack2;
    }

    @Override
    public void dragonminez$setPlayingAttack(boolean playingAttack) {
        dragonminez$isPlayingAttack = playingAttack;
    }

    @Override
    public boolean dragonminez$isPlayingAttack() {
        return dragonminez$isPlayingAttack;
    }

    @Override
    public void dragonminez$isCreativeFlying(boolean flying) {
        this.dragonminez$isCreativeFlying = flying;
    }

    @Override
    public boolean dragonminez$isCreativeFlying() {
        return dragonminez$isCreativeFlying;
    }
}
