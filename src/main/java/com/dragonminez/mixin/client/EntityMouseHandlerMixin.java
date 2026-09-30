package com.dragonminez.mixin.client;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.flight.FlightOrientationHandler;
import com.dragonminez.client.render.camera.OverShoulderCamera;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMouseHandlerMixin {

	@Unique
	private static final double DRAGONMINEZ$STEERABLE_BEAM_TURN_SCALE = 0.3D;

	@Unique
	private static boolean dragonminez$turning = false;

	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void dragonminez$onTurn(double yawDelta, double pitchDelta, CallbackInfo ci) {
		if (dragonminez$turning || !((Object) this instanceof LocalPlayer player)) return;

		boolean steering = TechniqueDispatcher.isFiringSteerableKiAttack(player);
		double yaw = steering ? yawDelta * DRAGONMINEZ$STEERABLE_BEAM_TURN_SCALE : yawDelta;
		double pitch = steering ? pitchDelta * DRAGONMINEZ$STEERABLE_BEAM_TURN_SCALE : pitchDelta;

		if (FlySkillEvent.getInstance().isFlyingFast(player)) {
			FlightOrientationHandler.applyMouseDelta(yaw, pitch);
			ci.cancel();
			return;
		}

		if (OverShoulderCamera.turn(yaw, pitch)) {
			ci.cancel();
			return;
		}

		if (steering) {
			dragonminez$turning = true;
			try {
				player.turn(yaw, pitch);
			} finally {
				dragonminez$turning = false;
			}
			ci.cancel();
		}
	}
}
