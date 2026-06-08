package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.effects.KiWeaponRenderer;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.shader.KiBloomRenderer;
import com.dragonminez.client.render.shader.TransformationPostShaderManager;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class PlayerEffectsRenderHandler {
	private static final Set<Integer> CURRENT_FRAME_PLAYERS = new HashSet<>();

	@SubscribeEvent
	public static void onRenderTick(TickEvent.RenderTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			PlayerEffectQueue.getAndClearAuras();
			PlayerEffectQueue.getAndClearSparks();
			PlayerEffectQueue.getAndClearWeapons();
			PlayerEffectQueue.getAndClearFirstPersonAuras();
			PlayerEffectQueue.getAndClearKiAttacks();
		}
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		boolean shaderPack = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		RenderLevelStageEvent.Stage stage = event.getStage();

		if (shaderPack) {

			if (stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
				PlayerEffectQueue.getAndClearAuras();
				PlayerEffectQueue.getAndClearSparks();
				PlayerEffectQueue.getAndClearWeapons();
				PlayerEffectQueue.getAndClearFirstPersonAuras();
				PlayerEffectQueue.getAndClearKiAttacks();

				TransformationPostShaderManager.setShaderpackMainPass(true);
			} else if (stage == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
				TransformationPostShaderManager.setShaderpackMainPass(false);
				mc.getMainRenderTarget().bindWrite(false);
				renderWeapons(mc, event);
				renderEffects(mc, event);
				TransformationPostShaderManager.processShaderpackOutline(event.getPartialTick());
			}
			return;
		}

		if (stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
			renderWeapons(mc, event);
		} else if (stage == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
			renderEffects(mc, event);
		}
	}

	private static void renderWeapons(Minecraft mc, RenderLevelStageEvent event) {
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		PoseStack poseStack = event.getPoseStack();
		KiWeaponRenderer.processWeapons(buffers, poseStack);
		buffers.endBatch();
	}

	private static void renderEffects(Minecraft mc, RenderLevelStageEvent event) {
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		PoseStack poseStack = event.getPoseStack();
		Matrix4f projectionMatrix = event.getProjectionMatrix();
		float partialTick = event.getPartialTick();
		long gameTime = mc.level.getGameTime();

		CURRENT_FRAME_PLAYERS.clear();

		boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();
		boolean isCameraColliding = false;

		if (!isFirstPerson && mc.cameraEntity != null) {
			Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
			Vec3 entityPos = mc.cameraEntity.getPosition(partialTick);
			if (cameraPos.distanceToSqr(entityPos) < 0.25) isCameraColliding = true;
		}

		AuraRenderer.processFusionFlashes(mc, gameTime, partialTick, poseStack, buffers);
		buffers.endBatch();

		var kiAttacks = PlayerEffectQueue.getAndClearKiAttacks();
		if (!kiAttacks.isEmpty()) {
			float kiAlpha = isFirstPerson ? 0.35f : 0.85f;
			if (DMZShaders.ki3dShader != null) DMZShaders.ki3dShader.safeGetUniform("globalAlpha").set(kiAlpha);

			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();

			for (var task : kiAttacks) {
				task.render(poseStack, projectionMatrix);
			}

			RenderSystem.enableCull();
			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();

			if (!IrisCompat.isShaderPackInUse(gameTime)) {
				KiBloomRenderer.render(kiAttacks, poseStack, projectionMatrix, partialTick);
			}

			if (DMZShaders.ki3dShader != null) DMZShaders.ki3dShader.safeGetUniform("globalAlpha").set(1.0f);
		}

		AuraRenderer.processThirdPersonAuras(mc, poseStack, projectionMatrix, CURRENT_FRAME_PLAYERS, isFirstPerson, isCameraColliding);
		AuraRenderer.processFirstPersonAuras(mc, poseStack, projectionMatrix, partialTick, CURRENT_FRAME_PLAYERS, isFirstPerson);
		AuraRenderer.processGhostAuras(mc, poseStack, projectionMatrix, partialTick, CURRENT_FRAME_PLAYERS);
		AuraRenderer.processSparks(poseStack, projectionMatrix, isFirstPerson);
		AuraRenderer.cleanCaches(CURRENT_FRAME_PLAYERS);
	}
}

