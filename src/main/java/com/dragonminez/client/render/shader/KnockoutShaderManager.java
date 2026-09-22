package com.dragonminez.client.render.shader;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.mixin.client.PostChainAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public final class KnockoutShaderManager {
	private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shaders/post/knockout.json");

	private static PostChain chain;
	private static RenderTarget depthHolder;
	private static int lastWidth;
	private static int lastHeight;
	private static boolean loadFailed;

	private KnockoutShaderManager() {}

	public static boolean isLoadFailed() {
		return loadFailed;
	}

	public static void onResourceReload() {
		loadFailed = false;
		reset();
	}

	public static void process(float partialTick, float intensity, boolean preserveDepth) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || loadFailed) return;

		RenderTarget main = mc.getMainRenderTarget();
		int width = main.width;
		int height = main.height;

		if (chain == null) {
			try {
				chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, EFFECT);
				chain.resize(width, height);
			} catch (IOException | RuntimeException e) {
				loadFailed = true;
				if (chain != null) chain.close();
				chain = null;
				LogUtil.error(Env.CLIENT, "Failed to load the knockout screen shader; falling back to the flat overlay until the next resource reload", e);
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

		for (PostPass pass : ((PostChainAccessor) chain).dragonminez$getPasses()) {
			pass.getEffect().safeGetUniform("Intensity").set(intensity);
		}

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
