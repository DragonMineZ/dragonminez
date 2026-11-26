package com.dragonminez.client.render.shader;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.mixin.client.PostChainAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.function.Consumer;

public final class FullscreenPostEffect {
	private final ResourceLocation effect;
	private final String label;

	private PostChain chain;
	private RenderTarget depthHolder;
	private int lastWidth;
	private int lastHeight;
	private boolean loadFailed;

	public FullscreenPostEffect(ResourceLocation effect, String label) {
		this.effect = effect;
		this.label = label;
	}

	public boolean isLoadFailed() {
		return loadFailed;
	}

	public void onResourceReload() {
		loadFailed = false;
		reset();
	}

	public void process(float partialTick, boolean preserveDepth, Consumer<EffectInstance> uniforms) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || loadFailed) return;

		RenderTarget main = mc.getMainRenderTarget();
		int width = main.width;
		int height = main.height;

		if (chain == null) {
			try {
				chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, effect);
				chain.resize(width, height);
			} catch (IOException | RuntimeException e) {
				loadFailed = true;
				if (chain != null) chain.close();
				chain = null;
				LogUtil.error(Env.CLIENT, "Failed to load the " + label + " post shader; falling back to the flat overlay until the next resource reload", e);
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
			uniforms.accept(pass.getEffect());
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

	public void reset() {
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
