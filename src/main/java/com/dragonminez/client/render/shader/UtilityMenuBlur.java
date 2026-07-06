package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

public final class UtilityMenuBlur {
	public static boolean ENABLED = true;
	private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shaders/post/utility_blur.json");
	private static boolean loadedByUs = false;

	private UtilityMenuBlur() {}

	public static void start() {
		if (!ENABLED) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameRenderer == null) return;

		PostChain current = mc.gameRenderer.currentEffect();
		if (current != null) {
			loadedByUs = EFFECT.toString().equals(current.getName());
			return;
		}
		try {
			mc.gameRenderer.loadEffect(EFFECT);
			PostChain now = mc.gameRenderer.currentEffect();
			loadedByUs = now != null && EFFECT.toString().equals(now.getName());
		} catch (Exception e) {
			loadedByUs = false;
		}
	}

	public static void stop() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameRenderer == null) {
			loadedByUs = false;
			return;
		}
		PostChain current = mc.gameRenderer.currentEffect();
		if (loadedByUs && current != null && EFFECT.toString().equals(current.getName())) {
			mc.gameRenderer.shutdownEffect();
		}
		loadedByUs = false;
	}
}
