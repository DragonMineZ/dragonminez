package com.dragonminez.mixin.client;

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


		if (!detached && entity instanceof LocalPlayer player && FirstPersonManager.shouldRenderFirstPerson(player)) {
			Vec3 baseSmoothedPos = this.getPosition();
			Vector3f offset = FirstPersonManager.offsetFirstPersonView(player);

			Vec3 forward = Vec3.directionFromRotation(0, player.getViewYRot(partialTick));
			Vec3 left = Vec3.directionFromRotation(0, player.getViewYRot(partialTick) - 90.0F);
			Vec3 targetPos = baseSmoothedPos.add(forward.scale(offset.z())).add(left.scale(offset.x())).add(0, offset.y(), 0);

			DMZCameraBuffer.updateTarget(targetPos);
			this.setPosition(DMZCameraBuffer.getSmoothedPosition(0.6f));
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