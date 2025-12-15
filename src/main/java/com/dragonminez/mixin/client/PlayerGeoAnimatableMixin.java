package com.dragonminez.mixin.client;

import com.dragonminez.client.animation.IPlayerAnimatable;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

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
