package com.dragonminez.mixin.client;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

	@Inject(method = "hurtTo", at = @At("HEAD"), cancellable = true)
	private void dragonminez$preventKaiokenHurtAnimation(float pHealth, CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;

		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, self).orElse(null);
		if (data == null) return;

		if (data.getCharacter().hasActiveForm() || data.getCharacter().hasActiveStackForm()) {
			float currentHealth = self.getHealth();
			float healthLoss = currentHealth - pHealth;

			if (healthLoss > 0) {
				double expectedDrain = data.getAdjustedHealthDrain();
				if (Math.abs(healthLoss - expectedDrain) < 1.0f) {
					self.setHealth(pHealth);
					ci.cancel();
				}
			}
		}
	}
}