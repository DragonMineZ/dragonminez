package com.dragonminez.mixin.client;

import com.dragonminez.client.flight.FlightRollHandler;
import com.dragonminez.client.flight.RollCamera;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class FlightCameraMixin implements RollCamera {

    @Unique
    private float dragonminez$roll = 0F;
    @Unique
    private float dragonminez$lastRoll = 0F;
    @Unique
    private float dragonminez$tickDelta = 0F;

    @Inject(method = "setup", at = @At("HEAD"))
    private void dragonminez$captureTickDelta(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        this.dragonminez$tickDelta = partialTick;
        this.dragonminez$lastRoll = this.dragonminez$roll;
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void dragonminez$updateRoll(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (FlightRollHandler.isRolling() || FlightRollHandler.hasActiveRoll()) {
            this.dragonminez$roll = FlightRollHandler.getRoll(partialTick);
        } else {
            this.dragonminez$roll = Mth.lerp(0.1F, this.dragonminez$roll, 0F);
        }
    }

    @Override
    public float dragonminez$getRoll() {
        return Mth.lerp(dragonminez$tickDelta, dragonminez$lastRoll, dragonminez$roll);
    }
}
