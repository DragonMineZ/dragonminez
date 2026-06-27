package com.dragonminez.client.render.shader;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public final class SearchGrayscaleManager {

	private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shaders/post/kisense_grayscale.json");

	private static PostChain chain = null;
	private static RenderTarget depthHolder = null;
	private static int lastWidth = 0;
	private static int lastHeight = 0;

	private SearchGrayscaleManager() {}

	public static void process(float partialTick) {
		process(partialTick, true);
	}

	public static void process(float partialTick, boolean preserveDepth) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;

		RenderTarget main = mc.getMainRenderTarget();
		int width = main.width;
		int height = main.height;

		if (chain == null) {
			try {
				chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, EFFECT);
				chain.resize(width, height);
			} catch (IOException e) {
				LogUtil.error(Env.CLIENT, "Failed to load ki sense grayscale shader", e);
				chain = null;
				return;
			}
		}

		if (preserveDepth && depthHolder == null) {
			depthHolder = new TextureTarget(width, height, true, Minecraft.ON_OSX);
			depthHolder.enableStencil();
		}

		if (width != lastWidth || height != lastHeight) {
			chain.resize(width, height);
			if (depthHolder != null) depthHolder.resize(width, height, Minecraft.ON_OSX);
			lastWidth = width;
			lastHeight = height;
		}

		if (preserveDepth) depthHolder.copyDepthFrom(main);

		RenderSystem.disableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.disableCull();

		chain.process(partialTick);

		main.bindWrite(false);
		if (preserveDepth) main.copyDepthFrom(depthHolder);
		main.bindWrite(true);

		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
	}

	public static void reset() {
		if (chain != null) {
			chain.close();
			chain = null;
		}
		if (depthHolder != null) {
			depthHolder.destroyBuffers();
			depthHolder = null;
		}
		lastWidth = 0;
		lastHeight = 0;
	}
}
