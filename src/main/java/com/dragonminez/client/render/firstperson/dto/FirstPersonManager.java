package com.dragonminez.client.render.firstperson.dto;

import com.dragonminez.client.gui.UtilityMenuScreen;
import com.dragonminez.common.config.ConfigManager;
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
		if (!ConfigManager.getUserConfig().getFirstPersonAnimated()) return false;
		if (player.getMainHandItem().getItem() instanceof MapItem || player.getOffhandItem().getItem() instanceof MapItem) return false;
		if (Minecraft.getInstance().screen instanceof ChatScreen) return Minecraft.getInstance().options.getCameraType().isFirstPerson();
		if (Minecraft.getInstance().screen != null) return false;
		return Minecraft.getInstance().options.getCameraType().isFirstPerson();
	}

	public static Vector3f offsetFirstPersonView(Player player) {
		final float BASE_OFFSET_Y = 0.1F;
		final float[] BASE_OFFSET_Z = {0.3F};
		final float BASE_SCALE = 0.9375f;

		final float[][] scaling = {{BASE_SCALE, BASE_SCALE, BASE_SCALE}};

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			var character = data.getCharacter();

			Float[] modelScaling = character.getModelScaling();
			if (modelScaling != null && modelScaling.length >= 2) {
				scaling[0][0] = modelScaling[0];
				scaling[0][1] = modelScaling[1];
			}

			if (character.isOozaruCached()) {
				BASE_OFFSET_Z[0] = 1.3F;
			} else {
				Float[] resolved = character.getResolvedModelScaling();
				if (resolved != null && resolved.length >= 2) {
					scaling[0][0] = resolved[0];
					scaling[0][1] = resolved[1];
				}
			}
		});

		final float BASE_EYE_FRACTION = 0.85f;
		final float SHRINK_EYE_COMPENSATION = 1.5f;
		float eyeFraction = BASE_EYE_FRACTION + Math.max(0f, BASE_SCALE - scaling[0][1]) * SHRINK_EYE_COMPENSATION;

		float modelHeightInBlocks = scaling[0][1] * 1.8f;
		float eyeHeightInBlocks = modelHeightInBlocks * eyeFraction;
		float defaultEyeHeight = 1.42f;
		float adjustedOffsetY = BASE_OFFSET_Y + (eyeHeightInBlocks - defaultEyeHeight);

		return new Vector3f(0, adjustedOffsetY, BASE_OFFSET_Z[0]);
	}
}
