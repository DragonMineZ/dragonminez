package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import com.dragonminez.client.render.util.PlayerEffectQueue.KiRenderTask;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.List;

public final class KiBloomRenderer {
	private static final ResourceLocation KI_BLOOM_EFFECT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shaders/post/ki_bloom.json");
	private static final String KI_SCENE_TARGET = "ki_scene";

	private static PostChain chain;
	private static int chainWidth = -1;
	private static int chainHeight = -1;

	private KiBloomRenderer() {}

	public static void render(List<KiRenderTask> tasks, PoseStack poseStack, Matrix4f projectionMatrix, float partialTick) {
		if (tasks.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameRenderer == null) return;

		RenderTarget main = mc.getMainRenderTarget();
		PostChain bloomChain = ensureChain(mc, main);
		if (bloomChain == null) return;

		RenderTarget kiTarget = bloomChain.getTempTarget(KI_SCENE_TARGET);
		if (kiTarget == null) return;

		kiTarget.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		kiTarget.clear(Minecraft.ON_OSX);
		kiTarget.copyDepthFrom(main);
		kiTarget.bindWrite(true);

		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();

		if (DMZShaders.ki3dShader != null) DMZShaders.ki3dShader.safeGetUniform("bloomMode").set(1.0f);

		for (KiRenderTask task : tasks) {
			task.render(poseStack, projectionMatrix);
		}

		if (DMZShaders.ki3dShader != null) DMZShaders.ki3dShader.safeGetUniform("bloomMode").set(0.0f);

		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();

		main.bindWrite(false);

		bloomChain.process(partialTick);
		main.bindWrite(false);
	}

	private static PostChain ensureChain(Minecraft mc, RenderTarget main) {
		if (chain != null && chainWidth == main.width && chainHeight == main.height) {
			return chain;
		}
		try {
			if (chain != null) chain.close();
			chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, KI_BLOOM_EFFECT);
			chain.resize(main.width, main.height);
			chainWidth = main.width;
			chainHeight = main.height;
		} catch (Exception e) {
			chain = null;
			chainWidth = -1;
			chainHeight = -1;
		}
		return chain;
	}

	public static void reset() {
		if (chain != null) {
			chain.close();
			chain = null;
			chainWidth = -1;
			chainHeight = -1;
		}
	}
}
