package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.render.CameraProjectionCapture;
import com.dragonminez.client.render.effects.AuraBorderRenderer;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.effects.AuraTrailRenderer;
import com.dragonminez.client.render.effects.KiWeaponRenderer;
import com.dragonminez.client.render.shader.BloomPipeline;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.shader.EffectBloomRenderer;
import com.dragonminez.client.render.shader.TransformationPostShaderManager;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix3f;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class PlayerEffectsRenderHandler {
	private static final Set<Integer> CURRENT_FRAME_PLAYERS = new HashSet<>();

	@SubscribeEvent
	public static void onRenderTick(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event) {
		AuraRenderer.resetBloomCapture();
		KiWeaponRenderer.clearBloomBlades();
		EffectBloomRenderer.clearEntityDraws();
		BloomPipeline.resetFrame();
		PlayerEffectQueue.getAndClearAuras();
		PlayerEffectQueue.getAndClearSparks();
		PlayerEffectQueue.getAndClearFirstPersonAuras();
		PlayerEffectQueue.getAndClearKiAttacks();
		PlayerEffectQueue.getAndClearEntityEffects();
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		boolean shaderPack = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		RenderLevelStageEvent.Stage stage = event.getStage();

		if (stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
			AuraRenderer.beginBloomCapture();
			AuraBorderRenderer.beginCapture();
		}

		if (stage == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
			renderExternallyCulledKiAttacks(mc, event);
		}

		if (shaderPack) {

			if (stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
				PlayerEffectQueue.getAndClearAuras();
				PlayerEffectQueue.getAndClearSparks();
				PlayerEffectQueue.getAndClearFirstPersonAuras();
				PlayerEffectQueue.getAndClearKiAttacks();
				PlayerEffectQueue.getAndClearEntityEffects();

				TransformationPostShaderManager.setShaderpackMainPass(true);
			} else if (stage == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
				TransformationPostShaderManager.setShaderpackMainPass(false);
				mc.getMainRenderTarget().bindWrite(false);
				renderWeapons(mc, event);
				renderEffects(mc, event);
				TransformationPostShaderManager.processShaderpackOutline(event.getPartialTick().getGameTimeDeltaPartialTick(false));
			}
			return;
		}

		if (stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
			renderWeapons(mc, event);
		}

		if (stage == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
			// 1.21 composites the particles/weather/fabulous targets after their
			// stage callbacks. Drawing DMZ effects there makes terrain and water
			// overwrite them. Draw once into the final main target instead.
			mc.getMainRenderTarget().bindWrite(false);
			renderEffects(mc, event);
		}
	}

	private static void renderWeapons(Minecraft mc, RenderLevelStageEvent event) {
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		PoseStack poseStack = event.getPoseStack();
		// Queued weapon vertices include the entity render's model-view transform.
		// Flush existing world geometry before drawing those vertices with identity.
		buffers.endBatch();
		var modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix().identity();
		RenderSystem.applyModelViewMatrix();
		try {
			KiWeaponRenderer.processWeapons(buffers, poseStack);
			buffers.endBatch();
		} finally {
			modelView.popMatrix();
			RenderSystem.applyModelViewMatrix();
		}
	}

	@SuppressWarnings("unchecked")
	private static void renderExternallyCulledKiAttacks(Minecraft mc, RenderLevelStageEvent event) {
		Frustum frustum = event.getFrustum();
		if (frustum == null || mc.level == null) return;

		EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
		Vec3 camera = event.getCamera().getPosition();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof AbstractKiProjectile)) continue;
			EntityRenderer<Entity> renderer = (EntityRenderer<Entity>) dispatcher.getRenderer(entity);
			if (renderer == null) continue;
			if (dispatcher.shouldRender(entity, frustum, camera.x, camera.y, camera.z)) continue;
			if (!renderer.shouldRender(entity, frustum, camera.x, camera.y, camera.z)) continue;

			dispatcher.render(entity,
					Mth.lerp(partialTick, entity.xOld, entity.getX()) - camera.x,
					Mth.lerp(partialTick, entity.yOld, entity.getY()) - camera.y,
					Mth.lerp(partialTick, entity.zOld, entity.getZ()) - camera.z,
					Mth.lerp(partialTick, entity.yRotO, entity.getYRot()), partialTick,
					poseStack, buffers, dispatcher.getPackedLightCoords(entity, partialTick));
		}
	}

	@SuppressWarnings("unchecked")
	private static void enqueueExplosionBursts(Minecraft mc, PoseStack poseStack,
	                                           MultiBufferSource buffers, float partialTick) {
		if (mc.level == null) return;

		Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof KiExplosionVisualEntity burst)) continue;

			EntityRenderer<KiExplosionVisualEntity> renderer =
					(EntityRenderer<KiExplosionVisualEntity>) mc.getEntityRenderDispatcher().getRenderer(burst);
			if (renderer == null) continue;

			poseStack.pushPose();
			poseStack.translate(
					Mth.lerp(partialTick, burst.xOld, burst.getX()) - camera.x,
					Mth.lerp(partialTick, burst.yOld, burst.getY()) - camera.y,
					Mth.lerp(partialTick, burst.zOld, burst.getZ()) - camera.z);
			renderer.render(burst, 0.0F, partialTick, poseStack, buffers, 15728880);
			poseStack.popPose();
		}
	}
	private static void renderEffects(Minecraft mc, RenderLevelStageEvent event) {
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		PoseStack poseStack = createDeferredEffectPose(event);
		Matrix4f projectionMatrix = event.getProjectionMatrix();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		long gameTime = mc.level.getGameTime();

		CURRENT_FRAME_PLAYERS.clear();

		boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();
		boolean isCameraColliding = false;

		if (!isFirstPerson && mc.cameraEntity != null) {
			Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
			Vec3 entityPos = mc.cameraEntity.getPosition(partialTick);
			if (cameraPos.distanceToSqr(entityPos) < 0.25) isCameraColliding = true;
		}

		AuraRenderer.processFusionFlashes(mc, gameTime, partialTick, CameraProjectionCapture.viewPoseStack(poseStack), buffers);
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		modelView.identity();
		RenderSystem.applyModelViewMatrix();
		try {
			buffers.endBatch();
		} finally {
			modelView.popMatrix();
			RenderSystem.applyModelViewMatrix();
		}

		enqueueExplosionBursts(mc, poseStack, buffers, partialTick);

		var kiAttacks = PlayerEffectQueue.getAndClearKiAttacks();
		if (!kiAttacks.isEmpty()) {
			float kiAlpha = isFirstPerson && mc.getCameraEntity() == mc.player ? 0.35f : 0.85f;
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
		}

		try {
			AuraRenderer.processThirdPersonAuras(mc, poseStack, projectionMatrix, CURRENT_FRAME_PLAYERS, isFirstPerson, isCameraColliding);
			AuraRenderer.processFirstPersonAuras(mc, poseStack, projectionMatrix, partialTick, CURRENT_FRAME_PLAYERS, isFirstPerson);
			AuraRenderer.processGhostAuras(mc, poseStack, projectionMatrix, partialTick, CURRENT_FRAME_PLAYERS);
			AuraTrailRenderer.renderDetached(poseStack, projectionMatrix, partialTick);
			AuraRenderer.processSparks(poseStack, projectionMatrix, isFirstPerson);

			var entityEffects = PlayerEffectQueue.getAndClearEntityEffects();
			for (var task : entityEffects) task.render();

			AuraBorderRenderer.process(mc, poseStack, projectionMatrix);
		} finally {
			AuraRenderer.endBloomCapture();
		}

		EffectBloomRenderer.render(kiAttacks, poseStack, projectionMatrix);

		if (DMZShaders.ki3dShader != null) DMZShaders.ki3dShader.safeGetUniform("globalAlpha").set(1.0f);

		AuraRenderer.cleanCaches(CURRENT_FRAME_PLAYERS);
	}

	/**
	 * Deferred effects need the same model-view transform as terrain and entities.
	 * The stage PoseStack is not reliable at every 1.21 render stage, but NeoForge
	 * exposes the actual model-view matrix explicitly. Rebuilding this from yaw and
	 * pitch keeps X/Y alignment while producing a different view-space Z, causing
	 * terrain behind an aura to incorrectly win the depth test.
	 */
	private static PoseStack createDeferredEffectPose(RenderLevelStageEvent event) {
		PoseStack poseStack = new PoseStack();
		poseStack.last().pose().set(event.getModelViewMatrix());
		poseStack.last().normal().set(new Matrix3f(event.getModelViewMatrix()));
		return poseStack;
	}
}
