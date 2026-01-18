package com.dragonminez.mixin.client;

import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PrimaryLevelData.class})
public abstract class PrimaryLevelDataMixin {
	@Inject(method = {"hasConfirmedExperimentalWarning"}, at = {@At("HEAD")}, cancellable = true, remap = false)
	private void ignoreExperimentalSettingsScreen(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(Boolean.TRUE);
	}
}
