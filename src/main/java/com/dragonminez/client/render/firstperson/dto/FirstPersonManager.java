package com.dragonminez.client.render.firstperson.dto;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

public class FirstPersonManager {
	public static boolean isRenderingInGui = false;

	public static boolean shouldRenderFirstPerson(Player player) {
		if (isRenderingInGui) return false;
		return Minecraft.getInstance().options.getCameraType().isFirstPerson();
	}

	public static Vector3f offsetFirstPersonView(Player player) {
		return new Vector3f(0, 0.1F, 0.5F);
	}
}
