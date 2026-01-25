package com.dragonminez.client.render.firstperson.dto;

import com.dragonminez.common.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

public class FirstPersonManager {

	public static boolean shouldRenderFirstPerson(Player player) {
		if (!ConfigManager.getUserConfig().getHud().isFirstPersonAnimated()) return false;
		if (Minecraft.getInstance().screen instanceof ChatScreen) return Minecraft.getInstance().options.getCameraType().isFirstPerson();
		if (Minecraft.getInstance().screen != null) return false;
		return Minecraft.getInstance().options.getCameraType().isFirstPerson();
	}

	public static Vector3f offsetFirstPersonView(Player player) {
		return new Vector3f(0, 0.1F, 0.5F);
	}
}
