package com.dragonminez.client.gui.hud;

import net.minecraft.client.Minecraft;

public class HUDManager {
	private static final double DESIGNED_SCALE = 2.0;

	public static float getScaleFactor() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getWindow() == null) return 1.0f;
		double currentScale = mc.getWindow().getGuiScale();
		if (currentScale == 0) return 1.0f;
		return (float) (DESIGNED_SCALE / currentScale);
	}
}
