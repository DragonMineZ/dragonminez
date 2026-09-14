package com.dragonminez.mixin.client;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.flight.FlightOrientationHandler;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMouseHandlerMixin {

	@Unique
	private static final double DRAGONMINEZ$STEERABLE_BEAM_TURN_SCALE = 0.3D;

	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void dragonminez$onTurn(double yawDelta, double pitchDelta, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player) {
			if (FlySkillEvent.getInstance().isFlyingFast(player)) {
				FlightOrientationHandler.applyMouseDelta(player, yawDelta, pitchDelta);
				ci.cancel();
			} else FlightOrientationHandler.reset();
		}
	}

	@ModifyVariable(method = "turn", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private double dragonminez$slowYawWhileSteeringBeam(double yawDelta) {
		return dragonminez$scaleTurn(yawDelta);
	}

	@ModifyVariable(method = "turn", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private double dragonminez$slowPitchWhileSteeringBeam(double pitchDelta) {
		return dragonminez$scaleTurn(pitchDelta);
	}

	@Unique
	private double dragonminez$scaleTurn(double delta) {
		if ((Object) this instanceof LocalPlayer player && TechniqueDispatcher.isFiringSteerableKiAttack(player)) {
			return delta * DRAGONMINEZ$STEERABLE_BEAM_TURN_SCALE;
		}
		return delta;
	}
}
