package com.dragonminez.client.gui.hair;

import com.dragonminez.common.init.MainSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

final class HairEditorSounds {
	private static final long LIMIT_COOLDOWN_MS = 120L;
	private static long lastLimitMs;

	private HairEditorSounds() {}

	static void click() {
		play(MainSounds.PIP_MENU.get(), 1.0f);
	}

	static void toggle(boolean on) {
		play(on ? MainSounds.SWITCH_ON.get() : MainSounds.SWITCH_OFF.get(), 1.0f);
	}

	static void select() {
		play(MainSounds.UI_MENU_SWITCH.get(), 1.0f);
	}

	static void deselect() {
		play(MainSounds.UI_MENU_SWITCH.get(), 0.8f);
	}

	static void limit() {
		long now = System.currentTimeMillis();
		if (now - lastLimitMs < LIMIT_COOLDOWN_MS) return;
		lastLimitMs = now;
		play(MainSounds.UI_NAVE_COOLDOWN.get(), 1.0f);
	}

	private static void play(SoundEvent sound, float pitch) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
	}
}
