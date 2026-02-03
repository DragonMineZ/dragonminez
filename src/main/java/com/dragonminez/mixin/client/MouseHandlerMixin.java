package com.dragonminez.mixin.client;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.flight.FlightOrientationHandler;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

	@Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void dragonminez$redirectTurn(LocalPlayer player, double yawDelta, double pitchDelta) {
		if (FlySkillEvent.isFlyingFast()) {
			FlightOrientationHandler.applyMouseDelta(player, yawDelta, pitchDelta);
		} else {
			FlightOrientationHandler.reset();
			player.turn(yawDelta, pitchDelta);
		}
	}
}
