package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.*;

public class AuraRenderer {
	private static final float HALF_SQRT_3 = (float) (Math.sqrt(3.0D) / 2.0D);
	private static final float FADE_SPEED = 0.005f;
	private static final float PULSE_SPEED = 0.01f;

	private static final Map<Integer, Long> FUSION_START_TIME = new HashMap<>();
	private static final Map<Integer, Boolean> WAS_FUSED_CACHE = new HashMap<>();
	private static final Map<Integer, Float> COLOR_PROGRESS_MAP = new HashMap<>();
	private static final Map<Integer, Long> COLOR_TICK_MAP = new HashMap<>();
	private static final Map<Integer, Float> PULSE_PROGRESS = new HashMap<>();
	private static final Map<Integer, Long> PULSE_LAST_RENDER_TIME = new HashMap<>();
	private static final Map<Integer, CachedAuraData> AURA_CACHE = new HashMap<>();
	private static final Map<Integer, Long> LAST_RENDER_TIME = new HashMap<>();
	private static VertexBuffer cachedLightningMesh;

	public static class AuraLayer {
		public String type;
		public int layerId;
		public float[] color;

		public AuraLayer(String type, int layerId, float[] color) {
			this.type = type;
			this.layerId = layerId;
			this.color = color;
		}
	}

	private static class CachedAuraData {
		float auraScaleX, auraScaleY, auraScaleZ;
		float bodyScaleX, bodyScaleY, bodyScaleZ;
		float alphaProgress;
		BakedGeoModel playerModel;
		List<AuraLayer> lastLayers;
	}

	public static void processFusionFlashes(Minecraft mc, long gameTime, float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource buffers) {
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
						List<AuraLayer> layers = getAuraLayers(player, stats, partialTick);
						if (!layers.isEmpty()) {
							float[] color = layers.get(layers.size() - 1).color;
							int r = (int) (color[0] * 255);
							int g = (int) (color[1] * 255);
							int b = (int) (color[2] * 255);
							renderFusionFlash(player, timeSinceStart + partialTick, poseStack, buffers, r, g, b);
						}
					} else if (timeSinceStart > 80) {
						FUSION_START_TIME.remove(playerId);
					}
				}
			}
		}
	}

	public static void processThirdPersonAuras(Minecraft mc, PoseStack poseStack, Matrix4f projectionMatrix, Set<Integer> currentFramePlayers, boolean isFirstPerson, boolean isCameraColliding) {
		var auras = PlayerEffectQueue.getAndClearAuras();
		for (var entry : auras) {
			Player player = entry.player();
			boolean isLocalPlayer = player == mc.player;

			if (!isFirstPerson || isCameraColliding || !isLocalPlayer) {
				currentFramePlayers.add(player.getId());
				renderShaderAura(entry, poseStack, mc, projectionMatrix);
			}
		}
	}

	public static void processFirstPersonAuras(Minecraft mc, PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Set<Integer> currentFramePlayers, boolean isFirstPerson) {
		var firstPersonAuras = PlayerEffectQueue.getAndClearFirstPersonAuras();
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
						fpStack.mulPose(Axis.YP.rotationDegrees(-bodyRot + 180f));
						fpStack.scale(-1.0F, 1.0F, 1.0F);

						renderSparksImpl(localPlayer, fpStack.last().pose(), poseStack, projectionMatrix, partialTick, true);
					}
				}
			}
		}
	}

	public static void processGhostAuras(Minecraft mc, PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Set<Integer> currentFramePlayers) {
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

				if (isAuraActive) continue;

				boolean stillVisible = renderShaderGhostAura(player, data, poseStack, mc, partialTick, projectionMatrix);
				if (!stillVisible) {
					it.remove();
				}
			}
		}
	}

	public static void processSparks(PoseStack poseStack, Matrix4f projectionMatrix, boolean isFirstPerson) {
		var sparks = PlayerEffectQueue.getAndClearSparks();
		if (sparks != null && !sparks.isEmpty()) {
			for (var entry : sparks) {
				if (entry != null) {
					boolean isFirstLocal = isFirstPerson && entry.player() == Minecraft.getInstance().player;
					renderSparksImpl(entry.player(), entry.poseMatrix(), poseStack, projectionMatrix, entry.partialTick(), isFirstLocal);
				}
			}
		}
	}

	public static void cleanCaches(Set<Integer> currentFramePlayers) {
		LAST_RENDER_TIME.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		COLOR_PROGRESS_MAP.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		COLOR_TICK_MAP.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		PULSE_LAST_RENDER_TIME.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		PULSE_PROGRESS.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
	}

	private static float[] getBodyScale(StatsData stats) {
		float sX = 1.0f, sY = 1.0f, sZ = 1.0f;
		var character = stats.getCharacter();

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
		if (currentForm.contains("ozaru")) {
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

	private static List<AuraLayer> getAuraLayers(Player player, StatsData stats, float partialTick) {
		List<AuraLayer> activeLayers = new ArrayList<>();
		var character = stats.getCharacter();
		int entityId = player.getId();

		FormConfig.FormData nextForm = null;
		boolean chargingNormal = false;
		boolean chargingStack = false;

		if (stats.getStatus().isActionCharging()) {
			if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				chargingStack = nextForm != null;
			} else if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				if (!character.hasActiveStackForm()) {
					nextForm = TransformationsHelper.getNextAvailableForm(stats);
					chargingNormal = nextForm != null;
				}
			}
		}

		float chargeProgress = 0.0f;
		if (chargingNormal || chargingStack) {
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
			chargeProgress = Math.max(0.0f, Math.min(1.0f, smoothProgress));
		} else {
			COLOR_PROGRESS_MAP.put(entityId, 0.0f);
		}

		String normalHex = character.getAuraColor();
		String normalType = "kakarot";
		int normalLayerId = 0;

		if (character.hasActiveForm() && character.getActiveFormData() != null) {
			var fd = character.getActiveFormData();
			if (fd.getAuraColor() != null && !fd.getAuraColor().isEmpty()) normalHex = fd.getAuraColor();
			if (fd.getAuraType() != null && !fd.getAuraType().isEmpty()) normalType = fd.getAuraType();
			normalLayerId = fd.getAuraLayer() != null ? fd.getAuraLayer() : 0;
		}

		float[] normalColor = ColorUtils.hexToRgb(normalHex);
		if (chargingNormal && nextForm != null) {
			String targetHex = nextForm.getAuraColor() != null && !nextForm.getAuraColor().isEmpty() ? nextForm.getAuraColor() : normalHex;
			normalColor = interpolateColor(normalHex, targetHex, chargeProgress);
		}
		activeLayers.add(new AuraLayer(normalType, normalLayerId, normalColor));

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
			var fd = character.getActiveStackFormData();
			String stackHex = fd.getAuraColor() != null && !fd.getAuraColor().isEmpty() ? fd.getAuraColor() : "#FFFFFF";
			String stackType = fd.getAuraType() != null && !fd.getAuraType().isEmpty() ? fd.getAuraType() : "kakarot";
			int stackLayerId = fd.getAuraLayer() != null ? fd.getAuraLayer() : 1;

			float[] stackColor = ColorUtils.hexToRgb(stackHex);
			if (chargingStack && nextForm != null) {
				String targetHex = nextForm.getAuraColor() != null && !nextForm.getAuraColor().isEmpty() ? nextForm.getAuraColor() : stackHex;
				stackColor = interpolateColor(stackHex, targetHex, chargeProgress);
			}
			activeLayers.add(new AuraLayer(stackType, stackLayerId, stackColor));
		} else if (chargingStack && nextForm != null) {
			String targetHex = nextForm.getAuraColor() != null && !nextForm.getAuraColor().isEmpty() ? nextForm.getAuraColor() : "#FFFFFF";
			String stackType = nextForm.getAuraType() != null && !nextForm.getAuraType().isEmpty() ? nextForm.getAuraType() : "kakarot";
			int stackLayerId = nextForm.getAuraLayer() != null ? nextForm.getAuraLayer() : 1;

			float[] stackColor = interpolateColor(normalHex, targetHex, chargeProgress);
			activeLayers.add(new AuraLayer(stackType, stackLayerId, stackColor));
		}

		activeLayers.sort(Comparator.comparingInt(l -> l.layerId));
		return activeLayers;
	}

	private static float[] interpolateColor(String hexFrom, String hexTo, float factor) {
		float[] rgbFrom = ColorUtils.hexToRgb(hexFrom);
		float[] rgbTo = ColorUtils.hexToRgb(hexTo);

		float r = Mth.lerp(factor, rgbFrom[0], rgbTo[0]);
		float g = Mth.lerp(factor, rgbFrom[1], rgbTo[1]);
		float b = Mth.lerp(factor, rgbFrom[2], rgbTo[2]);

		return new float[]{r, g, b};
	}

	public static void renderShaderFirstPersonAura(Player player, float partialTick, PoseStack poseStack, Minecraft mc, Matrix4f projectionMatrix) {
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

		data.bodyScaleX = body[0]; data.bodyScaleY = body[1]; data.bodyScaleZ = body[2];
		data.auraScaleX = auraScale[0]; data.auraScaleY = auraScale[1]; data.auraScaleZ = auraScale[2];

		List<AuraLayer> activeLayers = getAuraLayers(player, stats, partialTick);
		if (activeLayers.isEmpty()) return;
		data.lastLayers = activeLayers;

		Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
		double lerpX = Mth.lerp(partialTick, player.xo, player.getX());
		double lerpY = Mth.lerp(partialTick, player.yo, player.getY());
		double lerpZ = Mth.lerp(partialTick, player.zo, player.getZ());

		if (player.onGround()) {
			AuraLayer topLayer = activeLayers.get(activeLayers.size() - 1);
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y + 0.05, lerpZ - cameraPos.z);
			renderShaderPulseAura(player, data, topLayer, poseStack, mc, projectionMatrix, partialTick, data.alphaProgress);
			poseStack.popPose();
		}

		for (AuraLayer layer : activeLayers) {
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);
			executeAuraShaderDraw(player, data, layer, poseStack, mc, projectionMatrix, partialTick, data.alphaProgress, true);
			poseStack.popPose();
		}
	}

	private static void renderShaderAura(PlayerEffectQueue.AuraRenderEntry entry, PoseStack poseStack, Minecraft mc, Matrix4f projectionMatrix) {
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
		data.playerModel = entry.playerModel();

		List<AuraLayer> activeLayers = getAuraLayers(player, stats, entry.partialTick());
		if (activeLayers.isEmpty()) return;
		data.lastLayers = activeLayers;

		Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
		double lerpX = Mth.lerp(entry.partialTick(), player.xo, player.getX());
		double lerpY = Mth.lerp(entry.partialTick(), player.yo, player.getY());
		double lerpZ = Mth.lerp(entry.partialTick(), player.zo, player.getZ());

		if (player.onGround()) {
			AuraLayer topLayer = activeLayers.get(activeLayers.size() - 1);
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y + 0.05, lerpZ - cameraPos.z);
			renderShaderPulseAura(player, data, topLayer, poseStack, mc, projectionMatrix, entry.partialTick(), data.alphaProgress);
			poseStack.popPose();
		}

		for (AuraLayer layer : activeLayers) {
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);
			executeAuraShaderDraw(player, data, layer, poseStack, mc, projectionMatrix, entry.partialTick(), data.alphaProgress, false);
			poseStack.popPose();
		}
	}

	private static boolean renderShaderGhostAura(Player player, CachedAuraData data, PoseStack poseStack, Minecraft mc, float partialTick, Matrix4f projectionMatrix) {
		if (data.alphaProgress > 0.0f) {
			data.alphaProgress -= FADE_SPEED;
			if (data.alphaProgress < 0.0f) data.alphaProgress = 0.0f;
		}
		if (data.alphaProgress <= 0.001f) return false;

		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return false;

		List<AuraLayer> activeLayers = data.lastLayers;
		if (activeLayers == null || activeLayers.isEmpty()) return false;

		Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
		double lerpX = Mth.lerp(partialTick, player.xo, player.getX());
		double lerpY = Mth.lerp(partialTick, player.yo, player.getY());
		double lerpZ = Mth.lerp(partialTick, player.zo, player.getZ());

		if (player.onGround()) {
			AuraLayer topLayer = activeLayers.get(activeLayers.size() - 1);
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y + 0.05, lerpZ - cameraPos.z);
			renderShaderPulseAura(player, data, topLayer, poseStack, mc, projectionMatrix, partialTick, data.alphaProgress);
			poseStack.popPose();
		}

		boolean isLocalPlayer = player == mc.player;
		boolean isFirstPerson = isLocalPlayer && mc.options.getCameraType().isFirstPerson();

		for (AuraLayer layer : activeLayers) {
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);
			executeAuraShaderDraw(player, data, layer, poseStack, mc, projectionMatrix, partialTick, data.alphaProgress, isFirstPerson);
			poseStack.popPose();
		}
		return true;
	}

	private static void executeAuraShaderDraw(Player player, CachedAuraData data, AuraLayer layer, PoseStack poseStack, Minecraft mc, Matrix4f projectionMatrix, float partialTick, float alphaMultiplier, boolean isFirstPerson) {
		ShaderInstance shader = DMZShaders.auraShader;
		if (shader == null || alphaMultiplier <= 0.001f) return;

		boolean isLocalPlayer = player == mc.player;
		float maxAlpha = (isLocalPlayer && isFirstPerson) ? 0.5f : 1.0f;
		float finalAlpha = maxAlpha * alphaMultiplier;

		String typeStr = layer.type != null && !layer.type.isEmpty() ? layer.type.toLowerCase() : "kakarot";
		ResourceLocation mainTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + typeStr + "_aura.png");
		ResourceLocation crossTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + typeStr + "_cross.png");

		float animSpeed = (player.tickCount + partialTick) * 0.5f;
		shader.safeGetUniform("speed").set(animSpeed);
		shader.safeGetUniform("ProjMat").set(projectionMatrix);

		shader.safeGetUniform("color1").set(layer.color[0] * 1.6f, layer.color[1] * 1.6f, layer.color[2] * 1.6f, 1.0f);
		shader.safeGetUniform("color2").set(layer.color[0] * 1.3f, layer.color[1] * 1.3f, layer.color[2] * 1.3f, 1.0f);
		shader.safeGetUniform("color3").set(layer.color[0] * 1.0f, layer.color[1] * 1.0f, layer.color[2] * 1.0f, 0.85f);
		shader.safeGetUniform("color4").set(layer.color[0] * 0.75f, layer.color[1] * 0.75f, layer.color[2] * 0.75f, 0.65f);

		float baseMultiplier = 2.2f;
		float finalScaleX = data.auraScaleX * baseMultiplier * (1.0f + layer.layerId * 0.15f);
		float finalScaleY = data.auraScaleY * baseMultiplier * (1.0f + layer.layerId * 0.15f);
		float finalScaleZ = data.auraScaleZ * baseMultiplier * (1.0f + layer.layerId * 0.15f);

		if (isLocalPlayer && isFirstPerson) {
			poseStack.pushPose();
			poseStack.last().pose().identity();
			poseStack.last().normal().identity();

			poseStack.translate(0.0, -0.6, -0.7);
			poseStack.scale(finalScaleX * 3.0f, finalScaleY * 3.0f, 1.0f);

			shader.safeGetUniform("alp1").set(finalAlpha * 0.45f);
			shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());

			RenderType mainRender = ModRenderTypes.getCustomAura(mainTex);
			mainRender.setupRenderState();
			shader.apply();

			VertexBuffer mesh = AuraMeshFactory.getBillboardQuad();
			mesh.bind();
			mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
			mainRender.clearRenderState();

			poseStack.popPose();

			VertexBuffer.unbind();
			shader.clear();
			return;
		}

		float cameraPitch = mc.gameRenderer.getMainCamera().getXRot();
		float absPitch = Math.abs(cameraPitch);
		float crossFactor = 0.0f;
		float pitchSquash = 1.0f;

		if (absPitch > 45.0f && !isFirstPerson) {
			crossFactor = (float) Math.pow((absPitch - 45.0f) / 45.0f, 2.0);
			pitchSquash = 1.0f - (crossFactor * 0.5f);
		}

		if (crossFactor < 1.0f) {
			poseStack.pushPose();

			poseStack.translate(0.0, data.bodyScaleY * 2f, 0.0);

			poseStack.mulPose(mc.gameRenderer.getMainCamera().rotation());
			poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

			poseStack.scale(finalScaleX, finalScaleY * pitchSquash, finalScaleZ);

			shader.safeGetUniform("alp1").set((1.0f - crossFactor) * finalAlpha);
			shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());

			RenderType mainRender = ModRenderTypes.getCustomAura(mainTex);
			mainRender.setupRenderState();
			shader.apply();

			VertexBuffer mesh = AuraMeshFactory.getBillboardQuad();
			mesh.bind();
			mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
			mainRender.clearRenderState();

			poseStack.popPose();
		}

		if (crossFactor > 0.0f) {
			poseStack.pushPose();
			poseStack.translate(0.0, 0.05, 0.0);
			poseStack.mulPose(Axis.YP.rotationDegrees(-mc.gameRenderer.getMainCamera().getYRot()));

			if (cameraPitch < 0.0f) {
				poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
			}

			poseStack.scale(finalScaleX, 1.0f, finalScaleZ);

			shader.safeGetUniform("alp1").set(crossFactor * finalAlpha);
			shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());

			RenderType crossRender = ModRenderTypes.getCustomAura(crossTex);
			crossRender.setupRenderState();
			shader.apply();

			VertexBuffer mesh = AuraMeshFactory.getGroundQuad();
			mesh.bind();
			mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
			crossRender.clearRenderState();

			poseStack.popPose();
		}

		VertexBuffer.unbind();
		shader.clear();
	}

	private static void renderShaderPulseAura(Player player, CachedAuraData data, AuraLayer topLayer, PoseStack poseStack, Minecraft mc, Matrix4f projectionMatrix, float partialTick, float alphaMultiplier) {
		int playerId = player.getId();
		long gameTime = player.level().getGameTime();

		if (gameTime - PULSE_LAST_RENDER_TIME.getOrDefault(playerId, 0L) > 2) {
			PULSE_PROGRESS.put(playerId, 0.0f);
		}
		PULSE_LAST_RENDER_TIME.put(playerId, gameTime);

		float currentProgress = PULSE_PROGRESS.getOrDefault(playerId, 0.0f);

		if (!mc.isPaused()) {
			currentProgress += PULSE_SPEED;
			if (currentProgress >= 1.0f) currentProgress -= 1.0f;
			PULSE_PROGRESS.put(playerId, currentProgress);
		}

		drawSinglePulseInstance(player, data, topLayer, poseStack, mc, projectionMatrix, partialTick, alphaMultiplier, currentProgress);

		float progressPhase2 = (currentProgress + 0.5f) % 1.0f;
		drawSinglePulseInstance(player, data, topLayer, poseStack, mc, projectionMatrix, partialTick, alphaMultiplier, progressPhase2);
	}

	private static void drawSinglePulseInstance(Player player, CachedAuraData data, AuraLayer topLayer, PoseStack poseStack, Minecraft mc, Matrix4f projectionMatrix, float partialTick, float alphaMultiplier, float progress) {
		float expansion = 1.0f + (6.0f * progress);
		float alphaCurve = (float) Math.sin(progress * Math.PI);

		String typeStr = topLayer.type != null && !topLayer.type.isEmpty() ? topLayer.type.toLowerCase() : "kakarot";
		ResourceLocation crossTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + typeStr + "_cross.png");

		ShaderInstance shader = DMZShaders.auraShader;
		if (shader == null) return;

		poseStack.pushPose();

		float cameraPitch = mc.gameRenderer.getMainCamera().getXRot();
		if (cameraPitch < 0.0f) {
			poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
		}

		float layerScaleBoost = 1.0f + (topLayer.layerId * 0.15f);
		float scaleMultiplier = 0.5f;
		float sX = data.auraScaleX * expansion * scaleMultiplier * layerScaleBoost;
		float sZ = data.auraScaleZ * expansion * scaleMultiplier * layerScaleBoost;

		poseStack.scale(sX, 1.0f, sZ);

		float animSpeed = (player.tickCount + partialTick) * 0.5f;
		shader.safeGetUniform("speed").set(animSpeed);
		shader.safeGetUniform("ProjMat").set(projectionMatrix);
		shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());

		shader.safeGetUniform("color1").set(topLayer.color[0] * 1.6f, topLayer.color[1] * 1.6f, topLayer.color[2] * 1.6f, 1.0f);
		shader.safeGetUniform("color2").set(topLayer.color[0] * 1.3f, topLayer.color[1] * 1.3f, topLayer.color[2] * 1.3f, 1.0f);
		shader.safeGetUniform("color3").set(topLayer.color[0] * 1.0f, topLayer.color[1] * 1.0f, topLayer.color[2] * 1.0f, 0.85f);
		shader.safeGetUniform("color4").set(topLayer.color[0] * 0.75f, topLayer.color[1] * 0.75f, topLayer.color[2] * 0.75f, 0.65f);

		shader.safeGetUniform("alp1").set(alphaCurve * 0.6f * alphaMultiplier);

		RenderType pulseRender = ModRenderTypes.getCustomAura(crossTex);
		pulseRender.setupRenderState();

		shader.apply();

		VertexBuffer mesh = AuraMeshFactory.getGroundQuad();
		mesh.bind();
		mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);

		pulseRender.clearRenderState();
		VertexBuffer.unbind();
		shader.clear();

		poseStack.popPose();
	}

	public static VertexBuffer getLightningMesh() {
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

		boolean isLocalPlayer = player == Minecraft.getInstance().player;
		boolean isFirstPerson = isLocalPlayer && Minecraft.getInstance().options.getCameraType().isFirstPerson();
		float cameraAlpha = (isLocalPlayer && isFirstPerson) ? 0.25f : 1.0f;

		shader.safeGetUniform("color1").set(
				Mth.lerp(0.8f, colorRgb[0], 1.0f),
				Mth.lerp(0.8f, colorRgb[1], 1.0f),
				Mth.lerp(0.8f, colorRgb[2], 1.0f)
		);
		shader.safeGetUniform("color2").set(colorRgb[0], colorRgb[1], colorRgb[2]);
		shader.safeGetUniform("alp1").set(cameraAlpha);
		shader.safeGetUniform("alp2").set(0.1f * cameraAlpha);
		shader.safeGetUniform("power").set(3.0f);
		shader.safeGetUniform("divis").set(1.0f);

		RenderType renderType = ModRenderTypes.getCustomLightning(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png"));
		renderType.setupRenderState();

		shader.apply();
		VertexBuffer mesh = getLightningMesh();
		mesh.bind();

		poseStack.pushPose();
		poseStack.last().pose().set(basePose);

		long tickInterval = isAuraActive ? 2L : 20L;
		long timeHash = player.level().getGameTime() / tickInterval;
		Random seededRand = new Random(player.getId() + timeHash);
		float[] bScale = getBodyScale(stats);
		float bbHeight = bScale[1];

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
}