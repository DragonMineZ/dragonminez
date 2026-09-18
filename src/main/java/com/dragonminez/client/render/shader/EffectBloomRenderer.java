package com.dragonminez.client.render.shader;

import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.util.PlayerEffectQueue.KiRenderTask;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

import java.util.List;

public final class EffectBloomRenderer {

	public static boolean bloomPass = false;

	private EffectBloomRenderer() {}

	public static void render(List<KiRenderTask> kiTasks, PoseStack poseStack, Matrix4f projectionMatrix) {
		boolean hasKi = kiTasks != null && !kiTasks.isEmpty();
		boolean hasRedraws = AuraRenderer.hasBloomDraws();
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();

		if ((hasKi || hasRedraws) && BloomPipeline.beginRedraw(main)) {
			try {
				RenderSystem.depthMask(false);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.disableCull();
				RenderSystem.enableDepthTest();

				if (hasKi) {
					if (DMZShaders.ki3dShader != null) DMZShaders.ki3dShader.safeGetUniform("bloomMode").set(1.0f);
					bloomPass = true;
					try {
						for (KiRenderTask task : kiTasks) {
							task.render(poseStack, projectionMatrix);
						}
					} finally {
						bloomPass = false;
					}
					if (DMZShaders.ki3dShader != null) DMZShaders.ki3dShader.safeGetUniform("bloomMode").set(0.0f);
				}

				if (hasRedraws) AuraRenderer.renderBloomDraws();
			} finally {
				RenderSystem.enableCull();
				RenderSystem.disableBlend();
				RenderSystem.depthMask(true);
				BloomPipeline.endRedraw();
			}
		} else if (hasRedraws) {
			AuraRenderer.resetBloomCapture();
		}

		BloomPipeline.resolve(main);
	}

	public static void reset() {
		BloomPipeline.reset();
	}
}
