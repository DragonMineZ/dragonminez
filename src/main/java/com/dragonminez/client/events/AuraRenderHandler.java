package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.aura.AuraMeshFactory;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.util.AuraRenderQueue;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.particles.AuraParticle;
import com.dragonminez.common.init.particles.DivineParticle;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.BetaWhitelist;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

import java.util.*;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class AuraRenderHandler {
	private static final ResourceLocation DUMMY_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/aura_ki_0.png");
	private static final ResourceLocation KI_WEAPONS_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/kiweapons.geo.json");
	private static final ResourceLocation KI_WEAPONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/kiweapons.png");
	private static final ResourceLocation SPARK_TEX_0 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/rayo_0.png");

	private static final float HALF_SQRT_3 = (float) (Math.sqrt(3.0D) / 2.0D);
	private static final Map<Integer, Long> FUSION_START_TIME = new HashMap<>();
	private static final Map<Integer, Boolean> WAS_FUSED_CACHE = new HashMap<>();

	private static final Map<Integer, Float> COLOR_PROGRESS_MAP = new HashMap<>();
	private static final Map<Integer, Long> COLOR_TICK_MAP = new HashMap<>();

	private static final Map<Integer, Float> PULSE_PROGRESS = new HashMap<>();
	private static final Map<Integer, Long> PULSE_LAST_RENDER_TIME = new HashMap<>();

	private static VertexBuffer cachedLightningMesh;

	private static class CachedAuraData {
		float auraScaleX, auraScaleY, auraScaleZ;
		float bodyScaleX, bodyScaleY, bodyScaleZ;
		float[] color;
		float alphaProgress;
		BakedGeoModel playerModel;
	}

	private static final Map<Integer, CachedAuraData> AURA_CACHE = new HashMap<>();
	private static final Map<Integer, Long> LAST_RENDER_TIME = new HashMap<>();

	private static final float FADE_SPEED = 0.005f;
	private static final float PULSE_SPEED = 0.01f;

	@SubscribeEvent
	public static void onRenderTick(TickEvent.RenderTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			AuraRenderQueue.getAndClearAuras();
			AuraRenderQueue.getAndClearSparks();
			AuraRenderQueue.getAndClearWeapons();
			AuraRenderQueue.getAndClearFirstPersonAuras();
		}
	}

	private static float[] getBodyScale(StatsData stats) {
		float sX = 1.0f, sY = 1.0f, sZ = 1.0f;
		var character = stats.getCharacter();
		String race = character.getRaceName().toLowerCase();

		if (character.hasActiveForm() && character.getActiveFormData() != null) {
			sX = character.getActiveFormData().getModelScaling()[0];
			sY = character.getActiveFormData().getModelScaling()[1];
			sZ = character.getActiveFormData().getModelScaling()[2];
		} else {
			sX = character.getModelScaling()[0];
			sY = character.getModelScaling()[1];
			sZ = character.getModelScaling()[2];
		}

		String currentForm = character.getActiveForm() != null ? character.getActiveForm().toLowerCase() : "";
		boolean isOozaru = currentForm.contains("ozaru");

		if (isOozaru) {
			sX = Math.max(0.1f, sX - 2.8f);
			sY = Math.max(0.1f, sY - 2.8f);
			sZ = Math.max(0.1f, sZ - 2.8f);
		}

		return new float[]{sX, sY, sZ};
	}

	private static float[] getAuraScale(StatsData stats) {
		float scale = 1.05f;

		var character = stats.getCharacter();
		String currentForm = character.getActiveForm() != null ? character.getActiveForm().toLowerCase() : "";

		if (character.hasActiveForm() && character.getActiveFormData() != null) scale += 0.1f;
		if (currentForm.contains("oozaru")) scale = 3.0f;
		if (currentForm.contains("supersaiyan2") || currentForm.contains("supersaiyan3") || currentForm.contains("ultra") || currentForm.contains("superperfect")) {
			scale += 0.2f;
		}

		return new float[]{scale, scale, scale};
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

		Minecraft mc = Minecraft.getInstance();
		EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
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
			double distanceToEntity = cameraPos.distanceToSqr(entityPos);
			if (distanceToEntity < 0.25) {
				isCameraColliding = true;
			}
		}

		for (Player player : mc.level.players()) {
			int playerId = player.getId();
			var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (stats != null) {
				boolean isFused = stats.getStatus().isFused();
				boolean wasFused = WAS_FUSED_CACHE.getOrDefault(playerId, false);

				if (isFused && !wasFused) FUSION_START_TIME.put(playerId, gameTime);
				WAS_FUSED_CACHE.put(playerId, isFused);

				if (FUSION_START_TIME.containsKey(playerId)) {
					long timeSinceStart = gameTime - FUSION_START_TIME.get(playerId);

					if (timeSinceStart < 60) {
						float[] color = getInterpolatedKiColor(player, stats, partialTick);
						int r = (int) (color[0] * 255);
						int g = (int) (color[1] * 255);
						int b = (int) (color[2] * 255);
						renderFusionFlash(player, timeSinceStart + partialTick, poseStack, buffers, r, g, b);
					} else {
						if (timeSinceStart > 80) FUSION_START_TIME.remove(playerId);
					}
				}
			}
		}

		var weapons = AuraRenderQueue.getAndClearWeapons();
		if (weapons != null && !weapons.isEmpty()) {
			for (var entry : weapons) {
				if (entry == null) continue;
				var player = entry.player();
				if (player == null) continue;
				DMZPlayerRenderer renderer = DMZRendererCache.getTPRenderer(player);

				if (renderer != null) {
					BakedGeoModel weaponModel = renderer.getGeoModel().getBakedModel(KI_WEAPONS_MODEL);
					if (weaponModel == null) continue;

					resetModelParts(weaponModel);
					boolean isRight = player.getMainArm() == HumanoidArm.RIGHT;
					String boneName = getWeaponBoneName(entry.weaponType(), isRight);

					if (!boneName.isEmpty()) {
						weaponModel.getBone(boneName).ifPresent(AuraRenderHandler::showBoneChain);
						syncModelToPlayer(weaponModel, entry.playerModel());

						poseStack.pushPose();
						poseStack.last().pose().set(entry.poseMatrix());

						renderer.reRender(weaponModel, poseStack, buffers, (GeoAnimatable) player,
								ModRenderTypes.energy(KI_WEAPONS_TEXTURE),
								buffers.getBuffer(ModRenderTypes.energy(KI_WEAPONS_TEXTURE)),
								entry.partialTick(), 15728880, OverlayTexture.NO_OVERLAY,
								entry.color()[0], entry.color()[1], entry.color()[2], 0.85f);

						poseStack.popPose();
					}
				}
			}
		}

		buffers.endBatch();

		var auras = AuraRenderQueue.getAndClearAuras();
		for (var entry : auras) {
			Player player = entry.player();
			currentFramePlayers.add(player.getId());

			if (!isFirstPerson || isCameraColliding) {
				renderShaderAura(entry, poseStack, mc, projectionMatrix);
			} else {
				int playerId = player.getId();
				CachedAuraData data = AURA_CACHE.computeIfAbsent(playerId, k -> new CachedAuraData());
				updateCachedAuraData(player, data, partialTick);
				renderShaderPulseAura(player, data, poseStack, mc, projectionMatrix, partialTick);
			}
		}

		var firstPersonAuras = AuraRenderQueue.getAndClearFirstPersonAuras();
		for (var entry : firstPersonAuras) {
			Player player = entry.player();
			if (!currentFramePlayers.contains(player.getId())) {
				currentFramePlayers.add(player.getId());
				renderShaderFirstPersonAura(player, entry.partialTick(), poseStack, mc, projectionMatrix);
			}
		}

		Player localPlayer = mc.player;
		if (isFirstPerson && localPlayer != null && !currentFramePlayers.contains(localPlayer.getId())) {
			var stats = StatsProvider.get(StatsCapability.INSTANCE, localPlayer).orElse(null);
			if (stats != null) {
				boolean isAuraActive = stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura();
				var character = stats.getCharacter();
				boolean hasLightning = false;

				if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
					hasLightning = character.getActiveStackFormData().getHasLightnings();
				} else if (character.hasActiveForm() && character.getActiveFormData() != null) {
					hasLightning = character.getActiveFormData().getHasLightnings();
				}

				if (isAuraActive || hasLightning) {
					currentFramePlayers.add(localPlayer.getId());

					if (isAuraActive) {
						renderShaderFirstPersonAura(localPlayer, partialTick, poseStack, mc, projectionMatrix);
					}

					if (hasLightning) {
						PoseStack fpStack = new PoseStack();
						Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
						double lerpX = Mth.lerp(partialTick, localPlayer.xo, localPlayer.getX());
						double lerpY = Mth.lerp(partialTick, localPlayer.yo, localPlayer.getY());
						double lerpZ = Mth.lerp(partialTick, localPlayer.zo, localPlayer.getZ());

						fpStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);
						float bodyRot = Mth.lerp(partialTick, localPlayer.yBodyRotO, localPlayer.yBodyRot);
						fpStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-bodyRot + 180f));
						fpStack.scale(-1.0F, 1.0F, 1.0F);

						renderSparksImpl(localPlayer, fpStack.last().pose(), poseStack, projectionMatrix, partialTick, true);
					}
				}
			}
		}

		Iterator<Map.Entry<Integer, CachedAuraData>> it = AURA_CACHE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, CachedAuraData> entry = it.next();
			int playerId = entry.getKey();
			CachedAuraData data = entry.getValue();

			if (!currentFramePlayers.contains(playerId)) {
				Player player = (Player) mc.level.getEntity(playerId);

				if (player == null || !player.isAlive()) {
					it.remove();
					continue;
				}

				var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
				boolean isAuraActive = stats != null && (stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura());

				if (isAuraActive) {
					continue;
				}

				boolean stillVisible = renderShaderGhostAura(player, data, poseStack, mc, partialTick, projectionMatrix);
				if (!stillVisible) {
					it.remove();
				}
			}
		}

		var sparks = AuraRenderQueue.getAndClearSparks();
		if (sparks != null && !sparks.isEmpty()) {
			for (var entry : sparks) {
				if (entry != null) {
					boolean isFirstLocal = isFirstPerson && entry.player() == localPlayer;
					renderSparksImpl(entry.player(), entry.poseMatrix(), poseStack, projectionMatrix, entry.partialTick(), isFirstLocal);
				}
			}
		}

		LAST_RENDER_TIME.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		COLOR_PROGRESS_MAP.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		COLOR_TICK_MAP.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		PULSE_LAST_RENDER_TIME.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		PULSE_PROGRESS.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
	}

	private static void updateCachedAuraData(Player player, CachedAuraData data, float partialTick) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return;

		float[] body = getBodyScale(stats);
		float[] auraScale = getAuraScale(stats);

		data.bodyScaleX = body[0]; data.bodyScaleY = body[1]; data.bodyScaleZ = body[2];
		data.auraScaleX = auraScale[0]; data.auraScaleY = auraScale[1]; data.auraScaleZ = auraScale[2];
		data.color = getInterpolatedKiColor(player, stats, partialTick);
	}

	private static void renderShaderAura(AuraRenderQueue.AuraRenderEntry entry, PoseStack poseStack, Minecraft mc, Matrix4f projectionMatrix) {
		var player = entry.player();
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return;

		int playerId = player.getId();
		CachedAuraData data = AURA_CACHE.computeIfAbsent(playerId, k -> new CachedAuraData());

		long gameTime = player.level().getGameTime();
		if (gameTime - LAST_RENDER_TIME.getOrDefault(playerId, 0L) > 2) {
			data.alphaProgress = 0.0f;
		}
		LAST_RENDER_TIME.put(playerId, gameTime);

		if (data.alphaProgress < 1.0f) {
			data.alphaProgress += FADE_SPEED;
			if (data.alphaProgress > 1.0f) data.alphaProgress = 1.0f;
		}

		float[] body = getBodyScale(stats);
		float[] auraScale = getAuraScale(stats);

		data.bodyScaleX = body[0]; data.bodyScaleY = body[1]; data.bodyScaleZ = body[2];
		data.auraScaleX = auraScale[0]; data.auraScaleY = auraScale[1]; data.auraScaleZ = auraScale[2];
		data.color = getInterpolatedKiColor(player, stats, entry.partialTick());
		data.playerModel = entry.playerModel();

		if (player.onGround()) {
			spawnGroundDust(player, body[0] * auraScale[0]);
			spawnFloatingRubble(player, body[0] * auraScale[0]);
			renderShaderPulseAura(player, data, poseStack, mc, projectionMatrix, entry.partialTick());
		}

		poseStack.pushPose();
		poseStack.last().pose().set(entry.poseMatrix());

		poseStack.translate(0.0, 1.375, 0.0);
		poseStack.scale(auraScale[0] * 1.5f, auraScale[1] * 2.2f, auraScale[2] * 1.5f);

		executeAuraShaderDraw(player, data, poseStack, mc, projectionMatrix, entry.partialTick(), data.alphaProgress, false);
		poseStack.popPose();
	}

	private static void renderShaderPulseAura(Player player, CachedAuraData data, PoseStack poseStack, Minecraft mc, Matrix4f projectionMatrix, float partialTick) {
		int playerId = player.getId();
		long gameTime = player.level().getGameTime();

		if (gameTime - PULSE_LAST_RENDER_TIME.getOrDefault(playerId, 0L) > 2) {
			PULSE_PROGRESS.put(playerId, 0.0f);
		}
		PULSE_LAST_RENDER_TIME.put(playerId, gameTime);

		float currentProgress = PULSE_PROGRESS.getOrDefault(playerId, 0.0f);
		currentProgress += PULSE_SPEED;
		if (currentProgress > 1.5f) currentProgress = 0.0f;
		PULSE_PROGRESS.put(playerId, currentProgress);

		if (currentProgress >= 1.0f) return;

		float expansion = 1.0f + (3.0f * currentProgress);
		float alphaCurve = (float) Math.sin(currentProgress * Math.PI);

		poseStack.pushPose();

		double lerpX = Mth.lerp(partialTick, player.xo, player.getX());
		double lerpY = Mth.lerp(partialTick, player.yo, player.getY());
		double lerpZ = Mth.lerp(partialTick, player.zo, player.getZ());
		var cameraPos = mc.gameRenderer.getMainCamera().getPosition();

		poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y + 0.26, lerpZ - cameraPos.z);
		float rotationAngle = (gameTime + partialTick) * 2.5f;
		poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotationAngle));

		poseStack.scale(data.bodyScaleX, data.bodyScaleY, data.bodyScaleZ);
		poseStack.scale(data.auraScaleX * expansion, data.auraScaleY * 0.2f, data.auraScaleZ * expansion);

		executeAuraShaderDraw(player, data, poseStack, mc, projectionMatrix, partialTick, alphaCurve * 0.5f, false);
		poseStack.popPose();
	}

	private static void renderShaderFirstPersonAura(Player player, float partialTick, PoseStack poseStack, Minecraft mc, Matrix4f projectionMatrix) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return;

		int playerId = player.getId();
		CachedAuraData data = AURA_CACHE.computeIfAbsent(playerId, k -> new CachedAuraData());

		long gameTime = player.level().getGameTime();
		if (gameTime - LAST_RENDER_TIME.getOrDefault(playerId, 0L) > 2) data.alphaProgress = 0.0f;
		LAST_RENDER_TIME.put(playerId, gameTime);

		if (data.alphaProgress < 1.0f) {
			data.alphaProgress += FADE_SPEED;
			if (data.alphaProgress > 1.0f) data.alphaProgress = 1.0f;
		}

		float[] body = getBodyScale(stats);
		float[] auraScale = getAuraScale(stats);
		data.color = getInterpolatedKiColor(player, stats, partialTick);

		Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
		double lerpX = Mth.lerp(partialTick, player.xo, player.getX());
		double lerpY = Mth.lerp(partialTick, player.yo, player.getY());
		double lerpZ = Mth.lerp(partialTick, player.zo, player.getZ());

		boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();
		float effectiveScale = Math.max(auraScale[0], Math.max(auraScale[1], auraScale[2]));

		if (isFirstPerson && effectiveScale > 1.5f) {
			lerpY += player.getEyeHeight() * 0.25f;
		}

		poseStack.pushPose();
		poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);
		float bodyRot = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
		poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-bodyRot + 180f));
		poseStack.scale(-1.0F, 1.0F, 1.0F);

		poseStack.scale(data.bodyScaleX, data.bodyScaleY, data.bodyScaleZ);
		poseStack.translate(0.0, 1.375, 0.0);
		poseStack.scale(auraScale[0] * 1.5f, auraScale[1] * 2.2f, auraScale[2] * 1.5f);

		executeAuraShaderDraw(player, data, poseStack, mc, projectionMatrix, partialTick, data.alphaProgress, true);
		poseStack.popPose();

		if (player.onGround()) {
			spawnGroundDust(player, body[0] * auraScale[0]);
			spawnFloatingRubble(player, body[0] * auraScale[0]);
		}
	}

	private static boolean renderShaderGhostAura(Player player, CachedAuraData data, PoseStack poseStack, Minecraft mc, float partialTick, Matrix4f projectionMatrix) {
		if (data.alphaProgress > 0.0f) {
			data.alphaProgress -= FADE_SPEED;
			if (data.alphaProgress < 0.0f) data.alphaProgress = 0.0f;
		}
		if (data.alphaProgress <= 0.001f) return false;

		Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
		double lerpX = Mth.lerp(partialTick, player.xo, player.getX());
		double lerpY = Mth.lerp(partialTick, player.yo, player.getY());
		double lerpZ = Mth.lerp(partialTick, player.zo, player.getZ());

		poseStack.pushPose();
		poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);
		float bodyRot = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
		poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-bodyRot + 180f));
		poseStack.scale(-1.0F, 1.0F, 1.0F);

		poseStack.scale(data.bodyScaleX, data.bodyScaleY, data.bodyScaleZ);
		poseStack.translate(0.0, 1.375, 0.0);
		poseStack.scale(data.auraScaleX * 1.5f, data.auraScaleY * 2.2f, data.auraScaleZ * 1.5f);

		boolean isLocalPlayer = player == mc.player;
		boolean isFirstPerson = isLocalPlayer && mc.options.getCameraType().isFirstPerson();

		executeAuraShaderDraw(player, data, poseStack, mc, projectionMatrix, partialTick, data.alphaProgress, isFirstPerson);
		poseStack.popPose();
		return true;
	}

	private static void executeAuraShaderDraw(Player player, CachedAuraData data, PoseStack poseStack, Minecraft mc, Matrix4f projectionMatrix, float partialTick, float alphaMultiplier, boolean isFirstPerson) {
		ShaderInstance shader = DMZShaders.auraShader;
		if (shader == null || alphaMultiplier <= 0.001f) return;

		float time = (player.tickCount + partialTick) / 20.0f;

		shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
		shader.safeGetUniform("ProjMat").set(projectionMatrix);
		shader.safeGetUniform("normalMatrix").set(new Matrix4f(new Matrix3f(poseStack.last().normal())));
		shader.safeGetUniform("time").set(time);
		shader.safeGetUniform("auravar").set(1.0f);

		float coreIntensity = 0.35f;
		shader.safeGetUniform("color1").set(
				Mth.lerp(coreIntensity, data.color[0], 1.0f),
				Mth.lerp(coreIntensity, data.color[1], 1.0f),
				Mth.lerp(coreIntensity, data.color[2], 1.0f)
		);

		float borderIntensity = 1.5f;
		shader.safeGetUniform("color2").set(
				data.color[0] * borderIntensity,
				data.color[1] * borderIntensity,
				data.color[2] * borderIntensity
		);

		boolean isLocalPlayer = player == mc.player;
		float maxAlpha = (isLocalPlayer && isFirstPerson) ? 0.15f : 1.0f;
		float finalAlpha = maxAlpha * alphaMultiplier;

		shader.safeGetUniform("alp1").set(0.2f * finalAlpha);
		shader.safeGetUniform("alp2").set(0.9f * finalAlpha);
		shader.safeGetUniform("power").set(6.0f);
		shader.safeGetUniform("divis").set(0.02f);

		RenderType auraRenderType = ModRenderTypes.getCustomAura(DUMMY_TEXTURE);
		auraRenderType.setupRenderState();

		shader.apply();
		VertexBuffer mesh = AuraMeshFactory.getAuraMesh();
		mesh.bind();
		mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
		VertexBuffer.unbind();
		shader.clear();

		auraRenderType.clearRenderState();
	}

	private static VertexBuffer getLightningMesh() {
		if (cachedLightningMesh == null) {
			cachedLightningMesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder builder = tesselator.getBuilder();
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);

			float w = 0.15f;
			float h = 3.0f;
			int segments = 20;
			float segHeight = h / segments;

			for (int i = 0; i < segments; i++) {
				float y1 = i * segHeight;
				float y2 = (i + 1) * segHeight;

				builder.vertex(-w, y1, 0).color(255, 255, 255, 255).normal(0, 0, 1).endVertex();
				builder.vertex(w, y1, 0).color(255, 255, 255, 255).normal(0, 0, 1).endVertex();
				builder.vertex(w, y2, 0).color(255, 255, 255, 255).normal(0, 0, 1).endVertex();
				builder.vertex(-w, y2, 0).color(255, 255, 255, 255).normal(0, 0, 1).endVertex();

				builder.vertex(0, y1, -w).color(255, 255, 255, 255).normal(1, 0, 0).endVertex();
				builder.vertex(0, y1, w).color(255, 255, 255, 255).normal(1, 0, 0).endVertex();
				builder.vertex(0, y2, w).color(255, 255, 255, 255).normal(1, 0, 0).endVertex();
				builder.vertex(0, y2, -w).color(255, 255, 255, 255).normal(1, 0, 0).endVertex();
			}

			cachedLightningMesh.bind();
			cachedLightningMesh.upload(builder.end());
			VertexBuffer.unbind();
		}
		return cachedLightningMesh;
	}

	private static void renderSparksImpl(Player player, Matrix4f basePose, PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, boolean isFirstPersonLocal) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return;
		var character = stats.getCharacter();

		boolean hasLightning = false;
		String lightningColorHex = "";

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().getHasLightnings()) {
			hasLightning = true;
			lightningColorHex = character.getActiveStackFormData().getLightningColor();
		} else if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().getHasLightnings()) {
			hasLightning = true;
			lightningColorHex = character.getActiveFormData().getLightningColor();
		}

		if (!hasLightning) return;

		ShaderInstance shader = DMZShaders.lightningShader;
		if (shader == null) return;

		boolean isAuraActive = stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura();

		float speedMod = isAuraActive ? 1.0f : 0.20f;
		int maxBranches = isAuraActive ? 5 : 3;
		float maxScale = isAuraActive ? 0.5f : 0.25f;

		float[] colorRgb = ColorUtils.hexToRgb(lightningColorHex);
		float time = (player.tickCount + partialTick) / 20.0f;

		shader.safeGetUniform("projectionMatrix").set(projectionMatrix);
		shader.safeGetUniform("time").set(time);
		shader.safeGetUniform("speedModifier").set(speedMod);

		shader.safeGetUniform("color1").set(
				Mth.lerp(0.8f, colorRgb[0], 1.0f),
				Mth.lerp(0.8f, colorRgb[1], 1.0f),
				Mth.lerp(0.8f, colorRgb[2], 1.0f)
		);
		shader.safeGetUniform("color2").set(colorRgb[0], colorRgb[1], colorRgb[2]);
		shader.safeGetUniform("alp1").set(1.0f);
		shader.safeGetUniform("alp2").set(0.1f);
		shader.safeGetUniform("power").set(3.0f);
		shader.safeGetUniform("divis").set(1.0f);

		RenderType renderType = ModRenderTypes.getCustomLightning(SPARK_TEX_0);
		renderType.setupRenderState();

		shader.apply();
		VertexBuffer mesh = getLightningMesh();
		mesh.bind();

		poseStack.pushPose();
		poseStack.last().pose().set(basePose);

		long tickInterval = isAuraActive ? 2L : 20L;
		long timeHash = player.level().getGameTime() / tickInterval;
		Random seededRand = new Random(player.getId() + timeHash);

		float bbHeight = player.getBbHeight();

		for (int i = 0; i < maxBranches; i++) {
			poseStack.pushPose();

			float spread = isAuraActive ? 1.8f : 1.2f;
			float randomY = seededRand.nextFloat() * bbHeight;

			if (isFirstPersonLocal) {
				randomY *= 0.4f;
				poseStack.translate(0.0, -0.3f, 0.6f);
			}

			poseStack.translate((seededRand.nextFloat() - 0.5f) * spread, randomY, (seededRand.nextFloat() - 0.5f) * spread);

			poseStack.mulPose(Axis.YP.rotationDegrees(seededRand.nextFloat() * 360));
			poseStack.mulPose(Axis.ZP.rotationDegrees(90f + (seededRand.nextFloat() - 0.5f) * 40f));

			float scale = 0.15f + seededRand.nextFloat() * maxScale;
			poseStack.scale(scale, scale, scale);

			shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
			shader.safeGetUniform("normalMatrix").set(new Matrix4f(new Matrix3f(poseStack.last().normal())));
			shader.apply();

			mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
			poseStack.popPose();
		}

		poseStack.popPose();
		VertexBuffer.unbind();
		shader.clear();
		renderType.clearRenderState();
	}

	private static String getWeaponBoneName(String type, boolean isRight) {
		return switch (type.toLowerCase()) {
			case "blade" -> isRight ? "blade_right" : "blade_left";
			case "scythe" -> isRight ? "scythe_right" : "scythe_left";
			case "clawlance" -> isRight ? "trident_right" : "trident_left";
			default -> "";
		};
	}

	private static void syncModelToPlayer(BakedGeoModel auraModel, BakedGeoModel playerModel) {
		for (GeoBone auraBone : auraModel.topLevelBones()) {
			syncBoneRecursively(auraBone, playerModel);
		}
	}

	private static void showBoneChain(GeoBone bone) {
		setHiddenRecursive(bone, false);

		GeoBone parent = bone.getParent();
		while (parent != null) {
			parent.setHidden(false);
			parent = parent.getParent();
		}
	}

	private static void resetModelParts(BakedGeoModel model) {
		for (GeoBone bone : model.topLevelBones()) {
			setHiddenRecursive(bone, true);
		}
	}

	private static void setHiddenRecursive(GeoBone bone, boolean hidden) {
		bone.setHidden(hidden);
		for (GeoBone child : bone.getChildBones()) {
			setHiddenRecursive(child, hidden);
		}
	}

	private static void syncBoneRecursively(GeoBone destBone, BakedGeoModel sourceModel) {
		sourceModel.getBone(destBone.getName()).ifPresent(sourceBone -> {
			destBone.setRotX(sourceBone.getRotX());
			destBone.setRotY(sourceBone.getRotY());
			destBone.setRotZ(sourceBone.getRotZ());
			destBone.setPosX(sourceBone.getPosX());
			destBone.setPosY(sourceBone.getPosY());
			destBone.setPosZ(sourceBone.getPosZ());
		});
		for (GeoBone child : destBone.getChildBones()) syncBoneRecursively(child, sourceModel);
	}

	private static String getBaseKiColorHex(StatsData stats) {
		var character = stats.getCharacter();
		String kiHex = character.getAuraColor();

		if (character.hasActiveStackForm()
				&& character.getActiveStackFormData() != null
				&& character.getActiveStackFormData().getAuraColor() != null
				&& !character.getActiveStackFormData().getAuraColor().isEmpty()) {
			kiHex = character.getActiveStackFormData().getAuraColor();
		} else if (character.hasActiveForm()
				&& character.getActiveFormData() != null
				&& character.getActiveFormData().getAuraColor() != null
				&& !character.getActiveFormData().getAuraColor().isEmpty()) {
			kiHex = character.getActiveFormData().getAuraColor();
		}

		return kiHex;
	}

	private static float[] getInterpolatedKiColor(Player player, StatsData stats, float partialTick) {
		int entityId = player.getId();
		String baseHex = getBaseKiColorHex(stats);

		if (stats.getStatus().isActionCharging()) {
			String targetHex = baseHex;
			FormConfig.FormData nextForm = null;

			boolean hasStackForm = stats.getCharacter().hasActiveStackForm();

			if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
			} else if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				if (!hasStackForm) nextForm = TransformationsHelper.getNextAvailableForm(stats);
			}

			if (nextForm != null && nextForm.getAuraColor() != null && !nextForm.getAuraColor().isEmpty()) {
				targetHex = nextForm.getAuraColor();
			}

			float lastProgress = COLOR_PROGRESS_MAP.getOrDefault(entityId, 0.0f);
			long lastTick = COLOR_TICK_MAP.getOrDefault(entityId, 0L);
			float targetProgress = stats.getResources().getActionCharge() / 100.0f;
			long currentTick = player.tickCount;
			float interpolationSpeed = 0.15f;

			if (currentTick != lastTick) {
				lastProgress = lastProgress + (targetProgress - lastProgress) * interpolationSpeed;
				COLOR_TICK_MAP.put(entityId, currentTick);
				COLOR_PROGRESS_MAP.put(entityId, lastProgress);
			}

			float smoothProgress = Mth.lerp(partialTick * interpolationSpeed, lastProgress, targetProgress);
			smoothProgress = Math.max(0.0f, Math.min(1.0f, smoothProgress));

			return interpolateColor(baseHex, targetHex, smoothProgress);
		} else {
			COLOR_PROGRESS_MAP.put(entityId, 0.0f);
			return ColorUtils.hexToRgb(baseHex);
		}
	}

	private static float[] interpolateColor(String hexFrom, String hexTo, float factor) {
		float[] rgbFrom = ColorUtils.hexToRgb(hexFrom);
		float[] rgbTo = ColorUtils.hexToRgb(hexTo);

		float r = Mth.lerp(factor, rgbFrom[0], rgbTo[0]);
		float g = Mth.lerp(factor, rgbFrom[1], rgbTo[1]);
		float b = Mth.lerp(factor, rgbFrom[2], rgbTo[2]);

		return new float[]{r, g, b};
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.CLIENT) return;

		Player player = event.player;

		if (!BetaWhitelist.isAllowed(player.getGameProfile().getName())) return;

		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return;

		float scale = 1.0f;
		var character = stats.getCharacter();

		if (character.hasActiveForm()) {
			var activeForm = character.getActiveFormData();
			if (activeForm != null) {
				Float[] scales = activeForm.getModelScaling();
				if (scales != null && scales.length >= 1) {
					scale = scales[0];
				}
			}
		}

		if (!stats.getStatus().isHasCreatedCharacter()) return;
		if (!stats.getStatus().isAuraActive() && !stats.getStatus().isPermanentAura()) return;

		float[] rgbColor = getInterpolatedKiColor(player, stats, 1.0f);
		int r = (int) Math.max(0, Math.min(255, rgbColor[0] * 255));
		int g = (int) Math.max(0, Math.min(255, rgbColor[1] * 255));
		int b = (int) Math.max(0, Math.min(255, rgbColor[2] * 255));
		int particleColor = (r << 16) | (g << 8) | b;

		for (int i = 0; i < 1; i++) spawnCalmAuraParticle(player, scale, particleColor);

		if (player.getRandom().nextInt(20) == 0) {
			int divineCount = 5 + player.getRandom().nextInt(10);
			for (int i = 0; i < divineCount; i++) {
				spawnPassiveDivineParticle(player, scale, 0xFFFFFF);
			}
		}
	}

	private static void renderFusionFlash(Player player, float time, PoseStack poseStack, MultiBufferSource buffer, int r, int g, int b) {
		float rotationTime = time * 0.01F;
		float rawSin = Mth.sin(time * 0.1F);
		float normalizedFade = (rawSin + 1.0F) / 2.0F;
		float fade = 0.4F + (normalizedFade * 0.6F);
		float intensity = 0.6F;

		RandomSource randomsource = RandomSource.create(432L);
		VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.lightning());

		poseStack.pushPose();

		Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		poseStack.translate(player.getX() - cameraPos.x, (player.getY() + 1.0) - cameraPos.y, player.getZ() - cameraPos.z);
		poseStack.scale(1.0F, 1.0F, 1.0F);

		for (int i = 0; (float) i < (intensity + intensity * intensity) / 2.0F * 60.0F; ++i) {
			poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
			poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F));
			poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
			poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F + rotationTime * 90.0F));

			float width = randomsource.nextFloat() * 5.0F + 4.0F;
			float length = randomsource.nextFloat() + 0.5F;

			Matrix4f matrix4f = poseStack.last().pose();

			int alpha = (int) (255.0F * fade);

			vertex01(vertexconsumer, matrix4f, alpha, r, g, b);
			vertex2(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
			vertex3(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
			vertex01(vertexconsumer, matrix4f, alpha, r, g, b);
			vertex3(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
			vertex4(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
			vertex01(vertexconsumer, matrix4f, alpha, r, g, b);
			vertex4(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
			vertex2(vertexconsumer, matrix4f, width, length, r, g, b, alpha);
		}

		poseStack.popPose();
	}

	private static void vertex01(VertexConsumer pConsumer, Matrix4f pMatrix, int pAlpha, int r, int g, int b) {
		pConsumer.vertex(pMatrix, 0.0F, 0.0F, 0.0F).color(r, g, b, pAlpha).endVertex();
	}

	private static void vertex2(VertexConsumer pConsumer, Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
		pConsumer.vertex(pMatrix, -HALF_SQRT_3 * pLength, pWidth, -0.5F * pLength).color(r, g, b, alpha).endVertex();
	}

	private static void vertex3(VertexConsumer pConsumer, Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
		pConsumer.vertex(pMatrix, HALF_SQRT_3 * pLength, pWidth, -0.5F * pLength).color(r, g, b, alpha).endVertex();
	}

	private static void vertex4(VertexConsumer pConsumer, Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
		pConsumer.vertex(pMatrix, 0.0F, pWidth, pLength).color(r, g, b, alpha).endVertex();
	}

	private static void spawnCalmAuraParticle(Player player, float totalScale, int colorHex) {
		var mc = Minecraft.getInstance();
		if (mc.isPaused()) return;
		var random = player.getRandom();

		double radius = (0.2f + random.nextDouble() * 0.3f) * totalScale;
		double angle = random.nextDouble() * 2 * Math.PI;

		double offsetX = Math.cos(angle) * radius;
		double offsetZ = Math.sin(angle) * radius;
		double heightOffset = (random.nextDouble() * 1.8f) * totalScale;

		double x = player.getX() + offsetX;
		double y = player.getY() + heightOffset;
		double z = player.getZ() + offsetZ;

		float r = ((colorHex >> 16) & 0xFF) / 255f;
		float g = ((colorHex >> 8) & 0xFF) / 255f;
		float b = (colorHex & 0xFF) / 255f;

		Particle p = mc.particleEngine.createParticle(MainParticles.AURA.get(), x, y, z, r, g, b);

		if (p instanceof AuraParticle auraP) {
			auraP.resize(totalScale);
			double driftSpeed = 0.02f;
			double velX = (offsetX / radius) * driftSpeed;
			double velZ = (offsetZ / radius) * driftSpeed;
			double velY = 0.01f + (random.nextDouble() * 0.02f);
			auraP.setParticleSpeed(velX, velY, velZ);
		}
	}

	private static void spawnPassiveDivineParticle(Player player, float totalScale, int colorHex) {
		if (Minecraft.getInstance().isPaused()) return;
		var random = player.getRandom();

		double widthSpread = player.getBbWidth() * totalScale * 2.0;
		double offsetX = (random.nextDouble() - 0.5) * widthSpread;
		double offsetZ = (random.nextDouble() - 0.5) * widthSpread;

		double x = player.getX() + offsetX;
		double z = player.getZ() + offsetZ;

		double heightSpread = (random.nextDouble() * 1.2) * totalScale;
		double y = player.getY() + heightSpread;

		float r = ((colorHex >> 16) & 0xFF) / 255f;
		float g = ((colorHex >> 8) & 0xFF) / 255f;
		float b = (colorHex & 0xFF) / 255f;

		Particle p = Minecraft.getInstance().particleEngine.createParticle(MainParticles.DIVINE.get(), x, y, z, r, g, b);

		if (p instanceof DivineParticle divineP) {
			divineP.resize(totalScale);
			double velY = 0.02 + (random.nextDouble() * 0.03);
			divineP.setParticleSpeed(0, velY, 0);
		}
	}

	private static void spawnGroundDust(Player player, float totalScale) {
		if (player.getRandom().nextFloat() > 0.3f) return;
		if (Minecraft.getInstance().isPaused()) return;

		var level = player.level();
		var random = player.getRandom();

		double angle = random.nextDouble() * 2 * Math.PI;
		double radius = (0.6f + random.nextDouble() * 0.4f) * totalScale;

		double offsetX = Math.cos(angle) * radius;
		double offsetZ = Math.sin(angle) * radius;

		double x = player.getX() + offsetX;
		double y = player.getY() + 0.3;
		double z = player.getZ() + offsetZ;

		double speedBase = 0.15f;
		double velX = Math.cos(angle) * speedBase;
		double velY = 0.1f;
		double velZ = Math.sin(angle) * speedBase;

		for (int i = 0; i < 3; i++) level.addParticle(MainParticles.DUST.get(), x, y, z, velX, velY, velZ);
	}

	private static void spawnFloatingRubble(Player player, float totalScale) {
		if (player.getRandom().nextFloat() > 0.15f) return;
		if (Minecraft.getInstance().isPaused()) return;

		var level = player.level();
		var random = player.getRandom();

		double angle = random.nextDouble() * 2 * Math.PI;
		double radius = (0.5f + random.nextDouble() * 1.9f) * totalScale;

		double offsetX = Math.cos(angle) * radius;
		double offsetZ = Math.sin(angle) * radius;

		double x = player.getX() + offsetX;
		double y = player.getY() + 0.1;
		double z = player.getZ() + offsetZ;

		double velX = (random.nextDouble() - 0.5) * 0.05;
		double velZ = (random.nextDouble() - 0.5) * 0.05;
		double velY = 0.05 + (random.nextDouble() * 0.1);

		level.addParticle(MainParticles.ROCK.get(), x, y, z, velX, velY, velZ);
	}
}