package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import net.minecraft.resources.ResourceLocation;

public final class KnockoutShaderManager {
	private static final FullscreenPostEffect EFFECT = new FullscreenPostEffect(
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shaders/post/knockout.json"), "knockout screen");

	private KnockoutShaderManager() {}

	public static boolean isLoadFailed() {
		return EFFECT.isLoadFailed();
	}

	public static void onResourceReload() {
		EFFECT.onResourceReload();
	}

	public static void process(float partialTick, float intensity, boolean preserveDepth) {
		EFFECT.process(partialTick, preserveDepth, effect -> effect.safeGetUniform("Intensity").set(intensity));
	}

	public static void reset() {
		EFFECT.reset();
	}
}
