package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.util.PlayerEffectQueue.KiRenderTask;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.List;

public final class EffectBloomRenderer {
	private static final ResourceLocation BLOOM_EFFECT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shaders/post/ki_bloom.json");
	private static final String BLOOM_SCENE_TARGET = "ki_scene";

	private static PostChain chain;
	private static RenderTarget chainScreenTarget;
	private static int chainWidth = -1;
	private static int chainHeight = -1;

	private EffectBloomRenderer() {}

	public static void render(List<KiRenderTask> kiTasks, PoseStack poseStack, Matrix4f projectionMatrix, float partialTick) {
		boolean hasKi = kiTasks != null && !kiTasks.isEmpty();
		boolean hasAuras = AuraRenderer.hasBloomDraws();
		if (!hasKi && !hasAuras) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.gameRenderer == null) return;

		RenderTarget main = mc.getMainRenderTarget();
		PostChain bloomChain = ensureChain(mc, main);
		if (bloomChain == null) return;

		RenderTarget bloomTarget = bloomChain.getTempTarget(BLOOM_SCENE_TARGET);
		if (bloomTarget == null) return;

		if (main.isStencilEnabled() && !bloomTarget.isStencilEnabled()) bloomTarget.enableStencil();

		bloomTarget.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		bloomTarget.clear(Minecraft.ON_OSX);
		copyDepthAndStencil(bloomTarget, main);
		bloomTarget.bindWrite(true);

		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();

		if (hasKi) {
			if (DMZShaders.ki3dShader != null) DMZShaders.ki3dShader.safeGetUniform("bloomMode").set(1.0f);
			for (KiRenderTask task : kiTasks) {
				task.render(poseStack, projectionMatrix);
			}
			if (DMZShaders.ki3dShader != null) DMZShaders.ki3dShader.safeGetUniform("bloomMode").set(0.0f);
		}

		if (hasAuras) AuraRenderer.renderBloomDraws();

		RenderSystem.enableCull();
		RenderSystem.disableBlend();

		main.bindWrite(false);

		bloomChain.process(partialTick);
		main.bindWrite(false);

		RenderSystem.depthMask(true);
	}

	private static void copyDepthAndStencil(RenderTarget dst, RenderTarget src) {
		int mask = GL11.GL_DEPTH_BUFFER_BIT;
		if (src.isStencilEnabled() && dst.isStencilEnabled()) mask |= GL11.GL_STENCIL_BUFFER_BIT;

		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src.frameBufferId);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst.frameBufferId);
		GlStateManager._glBlitFrameBuffer(0, 0, src.width, src.height, 0, 0, dst.width, dst.height, mask, GL11.GL_NEAREST);
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}

	private static PostChain ensureChain(Minecraft mc, RenderTarget main) {
		if (chain != null && chainScreenTarget == main && chainWidth == main.width && chainHeight == main.height) {
			return chain;
		}
		try {
			if (chain != null) chain.close();
			chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, BLOOM_EFFECT);
			chain.resize(main.width, main.height);
			chainScreenTarget = main;
			chainWidth = main.width;
			chainHeight = main.height;
		} catch (Exception e) {
			chain = null;
			chainScreenTarget = null;
			chainWidth = -1;
			chainHeight = -1;
		}
		return chain;
	}

	public static void reset() {
		if (chain != null) {
			chain.close();
			chain = null;
			chainScreenTarget = null;
			chainWidth = -1;
			chainHeight = -1;
		}
	}
}
