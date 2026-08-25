package com.dragonminez.mixin.client;

import com.dragonminez.client.flight.FlightRollHandler;
import com.dragonminez.client.flight.RollCamera;
import com.dragonminez.client.gui.tooltip.ScrollTracker;
import com.dragonminez.common.config.ConfigManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow @Final private Camera mainCamera;

	private static final float CAMERA_ROLL_SCALE = 0.8F;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V", shift = At.Shift.AFTER))
    private void dragonminez$applyFlightRoll(float partialTicks, long finishNanoTime, PoseStack poseStack, CallbackInfo ci) {
		if (FlightRollHandler.hasActiveRoll() && ConfigManager.getUserConfig().getCameraMovementDuringFlight()) {
			float roll = ((RollCamera) mainCamera).dragonminez$getRoll() * CAMERA_ROLL_SCALE;
			if (Math.abs(roll) > 0.01F) poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
		}
    }

	@Inject(method = "render", at = @At("RETURN"))
	private void dragonminez$checkTooltipRendered(CallbackInfo ci) {
		if (!ScrollTracker.renderedThisFrame) ScrollTracker.reset();
		ScrollTracker.renderedThisFrame = false;
	}
}
