package com.dragonminez.mixin.client;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.flight.FlightOrientationHandler;
import com.dragonminez.client.render.camera.OverShoulderCamera;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMouseHandlerMixin {

	@Unique
	private static final double DRAGONMINEZ$STEERABLE_BEAM_TURN_SCALE = 0.2D;
	@Unique
	private static final double DRAGONMINEZ$TURN_UNITS_PER_DEGREE = 1.0D / 0.15D;
	@Unique
	private static final double DRAGONMINEZ$STEERABLE_BEAM_MAX_DEGREES_PER_SECOND = 60.0D;
	@Unique
	private static final double DRAGONMINEZ$STEERABLE_BEAM_RESPONSE = 5.0D;
	@Unique
	private static final double DRAGONMINEZ$STEERABLE_BEAM_MAX_BACKLOG_DEGREES = 45.0D;

	@Unique
	private static boolean dragonminez$turning = false;
	@Unique
	private static double dragonminez$pendingYaw = 0.0D;
	@Unique
	private static double dragonminez$pendingPitch = 0.0D;
	@Unique
	private static long dragonminez$lastTurnNanos = 0L;

	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void dragonminez$onTurn(double yawDelta, double pitchDelta, CallbackInfo ci) {
		if (dragonminez$turning || !((Object) this instanceof LocalPlayer player)) return;

		boolean steering = TechniqueDispatcher.isFiringSteerableKiAttack(player);
		double yaw = yawDelta;
		double pitch = pitchDelta;
		if (steering) {
			double[] weighted = dragonminez$weightedTurn(yawDelta, pitchDelta);
			yaw = weighted[0];
			pitch = weighted[1];
		} else {
			dragonminez$resetWeightedTurn();
		}

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

	@Unique
	private static double[] dragonminez$weightedTurn(double yawDelta, double pitchDelta) {
		long now = System.nanoTime();
		double dt = dragonminez$lastTurnNanos == 0L ? 0.0D : Math.min((now - dragonminez$lastTurnNanos) / 1.0E9D, 0.1D);
		dragonminez$lastTurnNanos = now;

		double backlog = DRAGONMINEZ$STEERABLE_BEAM_MAX_BACKLOG_DEGREES * DRAGONMINEZ$TURN_UNITS_PER_DEGREE;
		dragonminez$pendingYaw = Mth.clamp(dragonminez$pendingYaw + yawDelta * DRAGONMINEZ$STEERABLE_BEAM_TURN_SCALE, -backlog, backlog);
		dragonminez$pendingPitch = Mth.clamp(dragonminez$pendingPitch + pitchDelta * DRAGONMINEZ$STEERABLE_BEAM_TURN_SCALE, -backlog, backlog);

		double release = 1.0D - Math.exp(-dt * DRAGONMINEZ$STEERABLE_BEAM_RESPONSE);
		double yaw = dragonminez$pendingYaw * release;
		double pitch = dragonminez$pendingPitch * release;

		double maxStep = DRAGONMINEZ$STEERABLE_BEAM_MAX_DEGREES_PER_SECOND * DRAGONMINEZ$TURN_UNITS_PER_DEGREE * dt;
		double magnitude = Math.sqrt(yaw * yaw + pitch * pitch);
		if (magnitude > maxStep && magnitude > 0.0D) {
			double scale = maxStep / magnitude;
			yaw *= scale;
			pitch *= scale;
		}

		dragonminez$pendingYaw -= yaw;
		dragonminez$pendingPitch -= pitch;
		return new double[]{yaw, pitch};
	}

	@Unique
	private static void dragonminez$resetWeightedTurn() {
		dragonminez$pendingYaw = 0.0D;
		dragonminez$pendingPitch = 0.0D;
		dragonminez$lastTurnNanos = 0L;
	}
}
