package com.dragonminez.client.render.firstperson.dto;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MapItem;
import org.joml.Vector3f;

public class FirstPersonManager {

	public static boolean shouldRenderFirstPerson(Player player) {
		if (player != Minecraft.getInstance().player) return false;
		if (!ConfigManager.getUserConfig().getHud().isFirstPersonAnimated()) return false;
		if (player.getMainHandItem().getItem() instanceof MapItem || player.getOffhandItem().getItem() instanceof MapItem) return false;
		if (Minecraft.getInstance().screen instanceof ChatScreen) return Minecraft.getInstance().options.getCameraType().isFirstPerson();
		if (Minecraft.getInstance().screen != null) return false;
		return Minecraft.getInstance().options.getCameraType().isFirstPerson();
	}

	public static Vector3f offsetFirstPersonView(Player player) {
		final float BASE_OFFSET_Y = 0.1F;
		final float[] BASE_OFFSET_Z = {0.5F};
		final float BASE_SCALE = 0.9375f;

		final float[][] scaling = {{BASE_SCALE, BASE_SCALE, BASE_SCALE}};

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			float[] modelScaling = data.getCharacter().getModelScaling();
			if (modelScaling != null && modelScaling.length >= 2) {
				scaling[0][0] = modelScaling[0];
				scaling[0][1] = modelScaling[1];
			}

			if (data.getCharacter().hasActiveForm()) {
				FormConfig.FormData activeForm = data.getCharacter().getActiveFormData();
				if (activeForm != null) {
					String formName = activeForm.getName().toLowerCase();
					if (!formName.contains("ozaru")) {
						float[] formScaling = activeForm.getModelScaling();
						scaling[0][0] = formScaling[0];
						scaling[0][1] = formScaling[1];
					}
				}

				if (activeForm != null && activeForm.getName().contains("ozaru")) BASE_OFFSET_Z[0] = 1.5F;
			}
		});

		float ratioY = scaling[0][1] / BASE_SCALE;
		float adjustedOffsetY;
		if (ratioY <= 1.0f) {
			adjustedOffsetY = BASE_OFFSET_Y + (1.0f - ratioY) * -2.0f;
		} else {
			float modelHeightInBlocks = scaling[0][1] * 1.8f;
			float eyeHeightInBlocks = modelHeightInBlocks * 0.85f;
			float defaultEyeHeight = 1.42f;
			adjustedOffsetY = BASE_OFFSET_Y + (eyeHeightInBlocks - defaultEyeHeight);

		}

		return new Vector3f(0, adjustedOffsetY, BASE_OFFSET_Z[0]);
	}
}
