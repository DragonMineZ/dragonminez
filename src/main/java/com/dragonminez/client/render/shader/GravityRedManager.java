package com.dragonminez.client.render.shader;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.dragonminez.mixin.client.PostChainAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

/**
 * Full-screen red distortion post effect applied while the local player stands inside an active
 * Gravity Device zone. Mirrors {@link SearchGrayscaleManager} (including the ShaderPack/Iris
 * compatibility handling done by the caller) but tints the frame red proportionally to gravity.
 */
public final class GravityRedManager {

	private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shaders/post/gravity_red.json");

	private static PostChain chain = null;
	private static RenderTarget depthHolder = null;
	private static int lastWidth = 0;
	private static int lastHeight = 0;

	private GravityRedManager() {}

	public static void process(float partialTick, float intensity) {
		process(partialTick, intensity, true);
	}

	public static void process(float partialTick, float intensity, boolean preserveDepth) {
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
				LogUtil.error(Env.CLIENT, "Failed to load gravity red shader", e);
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
