package com.dragonminez.mixin.client;

import com.dragonminez.common.init.MainSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
	@Unique
	private static boolean dragonminez$hasPlayedMusic = false;

	@Inject(method = "init", at = @At("HEAD"))
	private void onInit(CallbackInfo info) {
		if (!dragonminez$hasPlayedMusic) {
			SoundManager soundManager = Minecraft.getInstance().getSoundManager();
			soundManager.stop(null, SoundSource.MUSIC);
			dragonminez$hasPlayedMusic = true;
			soundManager.play(SimpleSoundInstance.forMusic(MainSounds.MENU_MUSIC.get()));
		}
	}
}
