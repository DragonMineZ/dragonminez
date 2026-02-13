package com.dragonminez.mixin.common;

import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Shadow public abstract AABB getBoundingBox();

	@Inject(method = "canEnterPose", at = @At("HEAD"), cancellable = true)
	private void onCanEnterPose(Pose pose, CallbackInfoReturnable<Boolean> cir) {
		Entity self = (Entity) (Object) this;
		if (!(self instanceof Player player)) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			float[] scaling = data.getCharacter().getModelScaling();
			if (scaling == null || scaling.length < 2) scaling = new float[]{0.9375f, 0.9375f, 0.9375f};

			float currentScaleY = scaling[1];

			if (data.getCharacter().hasActiveForm()) {
				FormConfig.FormData activeForm = data.getCharacter().getActiveFormData();
				if (activeForm != null) {
					float[] formMultiplier = activeForm.getModelScaling();
					currentScaleY *= formMultiplier[1];
				}
			}

			final float BASE_SCALE = 0.9375f;
			float ratioY = currentScaleY / BASE_SCALE;

			if (Math.abs(ratioY - 1.0f) > 0.001F) {
				float baseHeight = 1.8F;
				float poseMultiplier = 1.0F;

				if (pose == Pose.CROUCHING) {
					poseMultiplier = 1.5F / 1.8F;
				} else if (pose == Pose.SWIMMING || pose == Pose.FALL_FLYING || pose == Pose.SPIN_ATTACK) {
					poseMultiplier = 0.6F / 1.8F;
				}

				float actualHeight = baseHeight * ratioY * poseMultiplier;

				AABB currentBox = this.getBoundingBox();
				AABB testBox = new AABB(
					currentBox.minX,
					currentBox.minY,
					currentBox.minZ,
					currentBox.maxX,
					currentBox.minY + actualHeight,
					currentBox.maxZ
				);

				boolean canFit = player.level().noCollision(player, testBox);
				cir.setReturnValue(canFit);
			}
		});
	}
}

