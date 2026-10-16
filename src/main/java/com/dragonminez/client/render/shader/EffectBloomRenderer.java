package com.dragonminez.client.render.shader;

import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.effects.KiWeaponRenderer;
import com.dragonminez.client.render.util.PlayerEffectQueue.KiRenderTask;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class EffectBloomRenderer {

	public static boolean bloomPass = false;

	private static final List<Consumer<MultiBufferSource>> ENTITY_DRAWS = new ArrayList<>();

	private EffectBloomRenderer() {}

	public static void captureEntity(Consumer<MultiBufferSource> draw) {
		ENTITY_DRAWS.add(draw);
	}

	public static void clearEntityDraws() {
		ENTITY_DRAWS.clear();
	}

	private static void renderEntityDraws() {
		MultiBufferSource.BufferSource source = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		PoseStack modelView = RenderSystem.getModelViewStack();
		modelView.pushPose();
		modelView.setIdentity();
		RenderSystem.applyModelViewMatrix();
		if (DMZShaders.solarisSmokeShader != null) DMZShaders.solarisSmokeShader.safeGetUniform("bloomMode").set(1.0f);
		try {
			for (Consumer<MultiBufferSource> draw : ENTITY_DRAWS) draw.accept(source);
			source.endBatch();
		} finally {
			if (DMZShaders.solarisSmokeShader != null) DMZShaders.solarisSmokeShader.safeGetUniform("bloomMode").set(0.0f);
			modelView.popPose();
			RenderSystem.applyModelViewMatrix();
			ENTITY_DRAWS.clear();
			restoreKiState(true);
		}
	}

	public static void render(List<KiRenderTask> kiTasks, PoseStack poseStack, Matrix4f projectionMatrix) {
		boolean hasKi = kiTasks != null && !kiTasks.isEmpty();
		boolean hasRedraws = AuraRenderer.hasBloomDraws();
		boolean hasBlades = KiWeaponRenderer.hasBloomBlades();
		boolean hasEntities = !ENTITY_DRAWS.isEmpty();
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();

		if ((hasKi || hasRedraws || hasBlades || hasEntities) && BloomPipeline.beginRedraw(main)) {
			try {
				RenderSystem.depthMask(false);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.disableCull();
				RenderSystem.enableDepthTest();

				if (hasKi) {
					if (DMZShaders.ki3dShader != null) DMZShaders.ki3dShader.safeGetUniform("bloomMode").set(1.0f);
					if (DMZShaders.lightningShader != null) DMZShaders.lightningShader.safeGetUniform("bloomMode").set(1.0f);
					bloomPass = true;
					try {
						for (KiRenderTask task : kiTasks) {
							task.render(poseStack, projectionMatrix);
						}
					} finally {
						bloomPass = false;
						if (DMZShaders.lightningShader != null) DMZShaders.lightningShader.safeGetUniform("bloomMode").set(0.0f);
					}
					if (DMZShaders.ki3dShader != null) DMZShaders.ki3dShader.safeGetUniform("bloomMode").set(0.0f);
				}

				if (hasRedraws) AuraRenderer.renderBloomDraws();
				if (hasBlades) KiWeaponRenderer.renderBloomBlades();
				if (hasEntities) renderEntityDraws();
			} finally {
				RenderSystem.enableCull();
				RenderSystem.disableBlend();
				RenderSystem.depthMask(true);
				BloomPipeline.endRedraw();
			}
		} else {
			if (hasRedraws) AuraRenderer.resetBloomCapture();
			KiWeaponRenderer.clearBloomBlades();
			ENTITY_DRAWS.clear();
		}

		BloomPipeline.resolve(main);
	}

	public static void reset() {
		BloomPipeline.reset();
	}

	public static void restoreKiState(boolean depthTest) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		if (depthTest) RenderSystem.enableDepthTest();
	}
}
