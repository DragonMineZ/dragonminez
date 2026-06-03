package com.dragonminez.mixin.client;

import com.dragonminez.client.clash.BeamClashCinematicCamera;
import com.dragonminez.client.flight.FlightRollHandler;
import com.dragonminez.client.flight.RollCamera;
import com.dragonminez.client.render.firstperson.dto.DMZCameraBuffer;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin implements RollCamera {

	@Shadow protected abstract void setPosition(Vec3 pos);

	@Shadow
	public abstract Vec3 getPosition();

	@Shadow protected abstract void setRotation(float yRot, float xRot);

	@Unique private float dragonminez$roll = 0F;
	@Unique private float dragonminez$lastRoll = 0F;
	@Unique private float dragonminez$tickDelta = 0F;

	@Inject(method = "setup", at = @At("HEAD"))
	private void dragonminez$captureTickDelta(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
		this.dragonminez$tickDelta = partialTick;
		this.dragonminez$lastRoll = this.dragonminez$roll;
	}

	@Inject(method = "setup", at = @At("TAIL"))
	private void dragonminez$modifyCamera(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
		if (FlightRollHandler.hasActiveRoll()) {
			float newRoll = FlightRollHandler.getRoll(partialTick);
			float delta = Mth.wrapDegrees(newRoll - this.dragonminez$lastRoll);
			this.dragonminez$roll = this.dragonminez$lastRoll + delta;
			dragonminez$rebaseRoll();
		} else this.dragonminez$roll = Mth.lerp(0.1F, this.dragonminez$roll, 0F);

		if (entity instanceof LocalPlayer clashPlayer && BeamClashCinematicCamera.isActive()) {
			BeamClashCinematicCamera.Shot shot = BeamClashCinematicCamera.computeShot(level, clashPlayer, partialTick);
			if (shot != null) {
				this.setPosition(shot.pos());
				this.setRotation(shot.yaw(), shot.pitch());
				return;
			}
		}

		if (!detached && entity instanceof LocalPlayer player && FirstPersonManager.shouldRenderFirstPerson(player)) {
			Vec3 baseEyePos = this.getPosition();
			Vector3f targetOffset = FirstPersonManager.offsetFirstPersonView(player);

			Vector3f smoothedOffset = DMZCameraBuffer.getSmoothedOffset(targetOffset, 0.6f);

			Vec3 forward = Vec3.directionFromRotation(0, player.getViewYRot(partialTick));
			Vec3 left = Vec3.directionFromRotation(0, player.getViewYRot(partialTick) - 90.0F);

			Vec3 movement = forward.scale(smoothedOffset.z()).add(left.scale(smoothedOffset.x()));

			double movementLen = movement.length();
			Vec3 desiredPos = baseEyePos;

			if (movementLen > 0.001D) {
				Vec3 direction = movement.normalize();
				double safeMargin = 0.35D;

				Vec3 rayEnd = baseEyePos.add(direction.scale(movementLen + safeMargin));
				ClipContext context = new ClipContext(baseEyePos, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
				BlockHitResult hit = level.clip(context);

				if (hit.getType() != HitResult.Type.MISS) {
					double hitDistance = hit.getLocation().distanceTo(baseEyePos);
					double allowedDistance = Math.max(0.0D, hitDistance - safeMargin);
					if (allowedDistance < movementLen) desiredPos = baseEyePos.add(direction.scale(allowedDistance));
					else desiredPos = baseEyePos.add(movement);
				} else desiredPos = baseEyePos.add(movement);
			}

			this.setPosition(desiredPos);
		}
	}

	@Override
	public float dragonminez$getRoll() {
		return Mth.lerp(dragonminez$tickDelta, dragonminez$lastRoll, dragonminez$roll);
	}

	@Unique
	private void dragonminez$rebaseRoll() {
		float wrapped = Mth.wrapDegrees(this.dragonminez$roll);
		float offset = this.dragonminez$roll - wrapped;
		if (offset != 0F) {
			this.dragonminez$roll = wrapped;
			this.dragonminez$lastRoll -= offset;
		}
	}
}