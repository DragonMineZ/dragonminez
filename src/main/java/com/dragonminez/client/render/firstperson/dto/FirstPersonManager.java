package com.dragonminez.client.render.firstperson.dto;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

public class FirstPersonManager {

	public static boolean shouldRenderFirstPerson(Player player) {
		if (player != Minecraft.getInstance().player) return false;
		if (!ConfigManager.getUserConfig().getHud().isFirstPersonAnimated()) return false;
		if (Minecraft.getInstance().screen instanceof ChatScreen) return Minecraft.getInstance().options.getCameraType().isFirstPerson();
		if (Minecraft.getInstance().screen != null) return false;
		return Minecraft.getInstance().options.getCameraType().isFirstPerson();
	}

	public static Vector3f offsetFirstPersonView(Player player) {
		final float BASE_OFFSET_Y = 0.1F;
		final float BASE_OFFSET_Z = 0.5F;
		final float BASE_SCALE = 0.9375f;

		float[] scaling = {BASE_SCALE, BASE_SCALE, BASE_SCALE};

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			float[] modelScaling = data.getCharacter().getModelScaling();
			if (modelScaling != null && modelScaling.length >= 2) {
				scaling[0] = modelScaling[0];
				scaling[1] = modelScaling[1];
			}

			if (data.getCharacter().hasActiveForm()) {
				FormConfig.FormData activeForm = data.getCharacter().getActiveFormData();
				if (activeForm != null) {
					float[] formMultiplier = activeForm.getModelScaling();
					scaling[0] *= formMultiplier[0];
					scaling[1] *= formMultiplier[1];
				}
			}
		});

		float ratioY = scaling[1] / BASE_SCALE;
		float adjustedOffsetY = BASE_OFFSET_Y + (1.0f - ratioY) * -2.0f;

		return new Vector3f(0, adjustedOffsetY, BASE_OFFSET_Z);
	}
}
