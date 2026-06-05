package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.effects.KiWeaponRenderer;
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

		// When a shaderpack (BSL/Complementary via Oculus) is active, the world
		// pipeline is deferred and our custom-shader draws done at AFTER_WEATHER /
		// AFTER_PARTICLES get discarded. In that case we defer every effect batch
		// to AFTER_LEVEL, where Oculus has already composited the final scene to
		// the main render target, and draw on top of it.
		boolean shaderPack = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		RenderLevelStageEvent.Stage stage = event.getStage();

		if (shaderPack) {
			// Oculus renders entities multiple times per frame (shadow passes from
			// the sun's POV + the main camera pass). Each pass re-queues effects
			// with that pass's pose, so a shadow-pass entry would render the effect
			// at a wrong/far position. RenderLevelStageEvent only fires in the main
			// pass, so clearing the queues at AFTER_SKY (before main-pass entities
			// render) drops every shadow-pass entry, leaving only the main ones.
			if (stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
				PlayerEffectQueue.getAndClearAuras();
				PlayerEffectQueue.getAndClearSparks();
				PlayerEffectQueue.getAndClearWeapons();
				PlayerEffectQueue.getAndClearFirstPersonAuras();
				PlayerEffectQueue.getAndClearKiAttacks();
			} else if (stage == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
				mc.getMainRenderTarget().bindWrite(false);
				renderWeapons(mc, event);
				renderEffects(mc, event);
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
		}

		AuraRenderer.processThirdPersonAuras(mc, poseStack, projectionMatrix, CURRENT_FRAME_PLAYERS, isFirstPerson, isCameraColliding);
		AuraRenderer.processFirstPersonAuras(mc, poseStack, projectionMatrix, partialTick, CURRENT_FRAME_PLAYERS, isFirstPerson);
		AuraRenderer.processGhostAuras(mc, poseStack, projectionMatrix, partialTick, CURRENT_FRAME_PLAYERS);
		AuraRenderer.processSparks(poseStack, projectionMatrix, isFirstPerson);
		AuraRenderer.cleanCaches(CURRENT_FRAME_PLAYERS);
	}
}
