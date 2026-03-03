package com.dragonminez.mixin.client;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.flight.FlightOrientationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MouseHandlerMixin {

	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void dragonminez$onTurn(double yawDelta, double pitchDelta, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player) {
			if (FlySkillEvent.isFlyingFast()) {
				FlightOrientationHandler.applyMouseDelta(player, yawDelta, pitchDelta);
				ci.cancel();
			} else {
				FlightOrientationHandler.reset();
			}
		}
	}
}