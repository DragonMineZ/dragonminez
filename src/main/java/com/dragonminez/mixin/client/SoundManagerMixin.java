package com.dragonminez.mixin.client;

import com.dragonminez.common.config.ConfigManager;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
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
			return;
		}

		String soundPath = pSound.getLocation().getPath();
		if (isKiSound(soundPath) && pSound instanceof AbstractSoundInstance sound) {
			AbstractSoundInstanceAccessor accessor = (AbstractSoundInstanceAccessor) sound;
			accessor.dmz$setVolume(accessor.dmz$getVolume() * ConfigManager.getUserConfig().getKiSoundVolume() / 100.0F);
		}
	}

	@Unique
	private static boolean isKiSound(String path) {
		return path.startsWith("ki_") || path.equals("kiblast_shoot") || path.equals("laserbeam");
	}
}
