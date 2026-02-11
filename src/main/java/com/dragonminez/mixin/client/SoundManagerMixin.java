package com.dragonminez.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public class SoundManagerMixin {
	@Unique
	private static final ResourceLocation MENU_MUSIC_RESOURCE = ResourceLocation.fromNamespaceAndPath("minecraft", "music.menu");

	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void onPlay(SoundInstance pSound, CallbackInfo ci) {
		if (pSound.getLocation().equals(MENU_MUSIC_RESOURCE)) {
			ci.cancel();
		}
	}
}
