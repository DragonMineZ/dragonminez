package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.effects.KiWeaponRenderer;
import com.dragonminez.client.render.util.PlayerEffectQueue;
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

	@SubscribeEvent
	public static void onRenderTick(TickEvent.RenderTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			PlayerEffectQueue.getAndClearAuras();
			PlayerEffectQueue.getAndClearSparks();
			PlayerEffectQueue.getAndClearWeapons();
			PlayerEffectQueue.getAndClearFirstPersonAuras();
		}
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;

		Minecraft mc = Minecraft.getInstance();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		PoseStack poseStack = event.getPoseStack();
		Matrix4f projectionMatrix = event.getProjectionMatrix();
		float partialTick = event.getPartialTick();
		long gameTime = mc.level.getGameTime();
		Set<Integer> currentFramePlayers = new HashSet<>();

		boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();
		boolean isCameraColliding = false;

		if (!isFirstPerson) {
			Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
			Vec3 entityPos = mc.cameraEntity.getPosition(partialTick);
			if (cameraPos.distanceToSqr(entityPos) < 0.25) {
				isCameraColliding = true;
			}
		}

		AuraRenderer.processFusionFlashes(mc, gameTime, partialTick, poseStack, buffers);
		KiWeaponRenderer.processWeapons(buffers, poseStack);
		buffers.endBatch();

		AuraRenderer.processThirdPersonAuras(mc, poseStack, projectionMatrix, currentFramePlayers, isFirstPerson, isCameraColliding);
		AuraRenderer.processFirstPersonAuras(mc, poseStack, projectionMatrix, partialTick, currentFramePlayers, isFirstPerson);
		AuraRenderer.processGhostAuras(mc, poseStack, projectionMatrix, partialTick, currentFramePlayers);
		AuraRenderer.processSparks(poseStack, projectionMatrix, isFirstPerson);
		AuraRenderer.cleanCaches(currentFramePlayers);
	}
}