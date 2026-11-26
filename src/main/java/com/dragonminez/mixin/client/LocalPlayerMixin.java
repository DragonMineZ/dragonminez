package com.dragonminez.mixin.client;

import com.dragonminez.client.systems.SilentDamageState;
import com.dragonminez.common.combat.util.MathHelper;
import com.dragonminez.common.combat.util.Minecraft_DMZ;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

	@Inject(method = "hurtTo", at = @At("HEAD"), cancellable = true)
	private void dragonminez$preventSilentDamageHurtAnimation(float pHealth, CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		float healthLoss = self.getHealth() - pHealth;
		if (healthLoss <= 0) return;

		if (SilentDamageState.consume(healthLoss)) {
			self.setHealth(pHealth);
			ci.cancel();
		}
	}

	@Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/Input;tick(ZF)V", shift = At.Shift.AFTER))
	private void dragonminez$aiStep_ModifyInput(CallbackInfo ci) {
		double baseMultiplier = 0.5D;
		boolean applySmoothly = true;
		boolean affectWhileMounting = false;

		double multiplier = Math.min(Math.max(baseMultiplier, 0.0), 1.0);
		if (multiplier == 1.0) return;

		LocalPlayer clientPlayer = (LocalPlayer) (Object) this;
		if (clientPlayer.getVehicle() != null && !affectWhileMounting) return;

		var client = (Minecraft_DMZ) Minecraft.getInstance();
		float swingProgress = client.getSwingProgress();

		if (swingProgress < 0.98F) {
			if (clientPlayer.isSprinting()) {
				clientPlayer.setSprinting(false);
			}
			if (applySmoothly) {
				double p2 = 0;
				if (swingProgress <= 0.5F) p2 = MathHelper.easeOutCubic(swingProgress * 2);
				else p2 = MathHelper.easeOutCubic(1 - ((swingProgress - 0.5F) * 2));
				multiplier = 1.0 - (1.0 - multiplier) * p2;
			}
			clientPlayer.input.forwardImpulse *= (float) multiplier;
			clientPlayer.input.leftImpulse *= (float) multiplier;
		}
	}
}