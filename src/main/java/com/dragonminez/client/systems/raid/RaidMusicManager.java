package com.dragonminez.client.systems.raid;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

@OnlyIn(Dist.CLIENT)
public final class RaidMusicManager {

	private static SoundInstance current;

	private RaidMusicManager() {}

	public static void play(String soundId) {
		Minecraft mc = Minecraft.getInstance();

		stop(mc);
		if (soundId == null || soundId.isBlank()) return;

		ResourceLocation location = ResourceLocation.tryParse(soundId);
		if (location == null) {
			LogUtil.warn(Env.CLIENT, "Raid asked for a malformed sound id '{}'", soundId);
			return;
		}

		SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(location);
		if (event == null) {
			LogUtil.warn(Env.CLIENT, "Raid asked for unknown sound '{}'", soundId);
			return;
		}

		current = new SimpleSoundInstance(event.getLocation(), SoundSource.PLAYERS, 1.0F, 1.0F,
				SoundInstance.createUnseededRandom(), false, 0,
				SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true);
		mc.getSoundManager().play(current);
	}

	public static void stop() {
		stop(Minecraft.getInstance());
	}

	private static void stop(Minecraft mc) {
		if (current == null) return;
		mc.getSoundManager().stop(current);
		current = null;
	}
}
