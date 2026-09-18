package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuraRenderer {
	private static final float HALF_SQRT_3 = (float) (Math.sqrt(3.0D) / 2.0D);
	private static final float FADE_SPEED = 0.005f;
	private static final float PULSE_SPEED = 0.01f;
	private static final Vec3 AURA_2D_UP = new Vec3(0.0, 1.0, 0.0);
	private static final float AURA_2D_GROUND_LIFT = 0.05f;
	private static final float AURA_2D_BASE_MULTIPLIER = 2.2f;
	private static final float GUI_AURA_EYE_DISTANCE = 8.0f;
	private static final CachedAuraData GUI_AURA_DATA = new CachedAuraData();
	private static final float AURA_2D_GROUND_OFFSET = 0.7f;
	private static final float AURA_2D_CROSS_START_DEG = 45.0f;
	private static final float AURA_2D_NEAR_FADE_SPAN = 0.9f;
	private static final float AURA_2D_NEAR_FADE_MIN = 1.2f;
	private static final float AURA_2D_NEAR_FADE_MAX = 2.2f;
	private static final float AURA_2D_NEAR_FADE_FLOOR = 0.4f;
	private static final float AURA_RELEASE_CAP = 100.0f;
	private static final float AURA_RELEASE_SCALE_BONUS = 0.35f;
	private static final float AURA_BASE_SCALE = 1.05f;
	private static final float SPARKING_LAYER_WIDTH = 0.26f;
	private static final float AURA_RELEASE_LERP_PER_TICK = 0.02f;
	private static final float AURA_3D_FLIGHT_LEAD = 1.10f;
	private static final float AURA_TRAIL_ALPHA = 0.55f;
	private static final float AURA_2D_FLIGHT_OFFSET = 0.31f;
	private static final float AURA_3D_BACKFACE = 0.02f;
	private static final float AURA_3D_BACKFACE_FIRST_PERSON = 0.85f;
	private static final float AURA_3D_FIRST_PERSON_ALPHA = 0.15f;

	private static final float SMOOTH_GROWTH_RATE = 4.0f;
	private static final float SMOOTH_SHRINK_RATE = 3.0f;
	private static final float SMOOTH_STYLE_BLEND_RATE = 6.0f;
	private static final float SMOOTH_FIRST_PERSON_ALPHA = 0.45f;
	private static final float SMOOTH_FLIGHT_LEAD = 0.25f;
	private static final float MAX_MOTION_STEP = 0.1f;

	private static final float AURA_MERGE_SIZE_DIVISOR = 1.25f;
	private static final float AURA_MERGE_LEADER_COLOR_WEIGHT = 1.5f;
	private static final float AURA_2D_MERGE_WIDTH = 1.1f;
	private static final float AURA_2D_MERGE_HEIGHT = 2.0f;
	private static final float AURA_3D_VISIBLE_HEIGHT = 1.8f;

	private static final Map<Integer, Long> FUSION_START_TIME = new ConcurrentHashMap<>();
	private static final Map<Integer, Boolean> WAS_FUSED_CACHE = new ConcurrentHashMap<>();
	private static final Map<Integer, Float> COLOR_PROGRESS_MAP = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> COLOR_TICK_MAP = new ConcurrentHashMap<>();
	private static final Map<Integer, Float> PULSE_PROGRESS = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> PULSE_LAST_RENDER_TIME = new ConcurrentHashMap<>();
	private static final Map<Integer, Float> RELEASE_SCALE_PROGRESS = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> RELEASE_SCALE_TICK = new ConcurrentHashMap<>();
	private static final Map<Integer, CachedAuraData> AURA_CACHE = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_RENDER_TIME = new ConcurrentHashMap<>();
	private static final Map<Integer, float[]> AURA_PHASE = new ConcurrentHashMap<>();
	private static VertexBuffer cachedLightningMesh;

	public static RenderType auraType(ResourceLocation texture) {
		return IrisCompat.isShaderPackInUse() ? ModRenderTypes.getCustomAuraCompat(texture) : ModRenderTypes.getCustomAura(texture);
	}

	public static RenderType lightningType(ResourceLocation texture) {
		return IrisCompat.isShaderPackInUse() ? ModRenderTypes.getCustomLightningCompat(texture) : ModRenderTypes.getCustomLightning(texture);
	}

	public static void customSetup(RenderType type, ResourceLocation texture, ShaderInstance shader) {
		if (IrisCompat.isShaderPackInUse()) {
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();

			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL11.GL_LEQUAL);
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			RenderSystem.setShaderTexture(0, texture);
			RenderSystem.setShader(() -> shader);
		} else {
			type.setupRenderState();
		}
	}

	public static void customClear(RenderType type) {
		if (IrisCompat.isShaderPackInUse()) {
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
		} else {
			type.clearRenderState();
		}
	}

	private static void applyAndDraw(VertexBuffer mesh, PoseStack poseStack, Matrix4f projectionMatrix, ShaderInstance shader,
									 ResourceLocation texture, float[] color, float alpha, float speed, boolean ground) {
		captureAuraBloom(mesh, texture, poseStack.last().pose(), projectionMatrix, color, alpha, speed);
		mesh.bind();
		mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
	}

	private static final List<Runnable> BLOOM_DRAWS = new ArrayList<>();
	private static boolean capturingBloom = false;

	private static final float AURA_BLOOM_ALPHA = 0.9f;

	public static void beginBloomCapture() {
		BLOOM_DRAWS.clear();
		capturingBloom = true;
	}

	public static void endBloomCapture() {
		capturingBloom = false;
	}

	public static void resetBloomCapture() {
		capturingBloom = false;
		BLOOM_DRAWS.clear();
	}

	public static boolean hasBloomDraws() {
		return !BLOOM_DRAWS.isEmpty();
	}

	public static void captureBloom(Runnable redraw) {
		if (capturingBloom) BLOOM_DRAWS.add(redraw);
	}

	public static void applyAuraColors(ShaderInstance shader, float[] color) {
		shader.safeGetUniform("color1").set(Mth.lerp(0.55f, color[0], 1.0f), Mth.lerp(0.55f, color[1], 1.0f), Mth.lerp(0.55f, color[2], 1.0f), 1.0f);
		shader.safeGetUniform("color2").set(color[0] * 1.75f, color[1] * 1.75f, color[2] * 1.75f, 1.0f);
		shader.safeGetUniform("color3").set(color[0] * 1.45f, color[1] * 1.45f, color[2] * 1.45f, 0.95f);
		shader.safeGetUniform("color4").set(color[0] * 1.1f, color[1] * 1.1f, color[2] * 1.1f, 0.8f);
	}

	public static void captureAuraBloom(VertexBuffer mesh, ResourceLocation texture, Matrix4f modelMatrix,
										Matrix4f projectionMatrix, float[] color, float alpha, float speed) {
		if (!capturingBloom || alpha <= 0.01f) return;
		Matrix4f model = new Matrix4f(modelMatrix);
		Matrix4f proj = new Matrix4f(projectionMatrix);
		float[] c = color.clone();
		BLOOM_DRAWS.add(() -> drawAuraBloom(mesh, texture, model, proj, c, alpha, speed));
	}

	private static void drawAuraBloom(VertexBuffer mesh, ResourceLocation texture, Matrix4f modelMatrix,
									  Matrix4f projectionMatrix, float[] c, float alpha, float speed) {
		ShaderInstance shader = DMZShaders.auraShader;
		if (shader == null) return;

		shader.safeGetUniform("bloomMode").set(1.0f);
		shader.safeGetUniform("speed").set(speed);
		shader.safeGetUniform("ProjMat").set(projectionMatrix);
		shader.safeGetUniform("modelMatrix").set(modelMatrix);
		applyAuraColors(shader, c);
		shader.safeGetUniform("alp1").set(alpha * AURA_BLOOM_ALPHA);

		RenderType type = auraType(texture);
		customSetup(type, texture, shader);
		mesh.bind();
		mesh.drawWithShader(modelMatrix, projectionMatrix, shader);
		customClear(type);

		VertexBuffer.unbind();
		shader.safeGetUniform("bloomMode").set(0.0f);
		shader.clear();
	}

	public static void captureLightningBloom(Matrix4f modelMatrix, Matrix4f normalMatrix, Matrix4f projectionMatrix,
											 float time, float speedModifier, float[] color1, float[] color2, float alpha) {
		if (!capturingBloom || alpha <= 0.01f) return;
		Matrix4f model = new Matrix4f(modelMatrix);
		Matrix4f normal = new Matrix4f(normalMatrix);
		Matrix4f proj = new Matrix4f(projectionMatrix);
		float[] c1 = color1.clone();
		float[] c2 = color2.clone();
		BLOOM_DRAWS.add(() -> drawLightningBloom(model, normal, proj, time, speedModifier, c1, c2, alpha));
	}

	private static void drawLightningBloom(Matrix4f modelMatrix, Matrix4f normalMatrix, Matrix4f projectionMatrix,
										   float time, float speedModifier, float[] c1, float[] c2, float alpha) {
		ShaderInstance shader = DMZShaders.lightningShader;
		if (shader == null) return;

		shader.safeGetUniform("bloomMode").set(1.0f);
		shader.safeGetUniform("projectionMatrix").set(projectionMatrix);
		shader.safeGetUniform("modelMatrix").set(modelMatrix);
		shader.safeGetUniform("normalMatrix").set(normalMatrix);
		shader.safeGetUniform("time").set(time);
		shader.safeGetUniform("speedModifier").set(speedModifier);
		shader.safeGetUniform("color1").set(c1[0], c1[1], c1[2]);
		shader.safeGetUniform("color2").set(c2[0], c2[1], c2[2]);
		shader.safeGetUniform("alp1").set(alpha);

		ResourceLocation lightningTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");
		RenderType type = lightningType(lightningTex);
		customSetup(type, lightningTex, shader);
		shader.apply();

		VertexBuffer mesh = getLightningMesh();
		mesh.bind();
		mesh.drawWithShader(modelMatrix, projectionMatrix, shader);
		VertexBuffer.unbind();

		shader.safeGetUniform("bloomMode").set(0.0f);
		shader.clear();
		customClear(type);
	}

	public static void renderBloomDraws() {
		if (BLOOM_DRAWS.isEmpty()) return;
		try {
			for (Runnable draw : BLOOM_DRAWS) draw.run();
		} finally {
			BLOOM_DRAWS.clear();
		}
	}

	public static class AuraLayer {
		public String type;
		public int layerId;
		public float[] color;
		public float alpha;
		public AuraStyle style;

		public AuraLayer(String type, int layerId, float[] color) {
			this(type, layerId, color, 1.0f);
		}

		public AuraLayer(String type, int layerId, float[] color, float alpha) {
			this.type = type;
			this.layerId = layerId;
			this.color = color;
			this.alpha = alpha;
		}
	}

	private static class CachedAuraData {
		float auraScaleX, auraScaleY, auraScaleZ;
		float bodyScaleX, bodyScaleY, bodyScaleZ;
		float modelScaleX, modelScaleY, modelScaleZ;
		float alphaProgress;
		boolean use3D;
		BakedGeoModel playerModel;
		List<AuraLayer> lastLayers;
		float growth;
		long motionNanos;
		Map<Integer, LayerMotion> motions = new HashMap<>();
	}

	private static final class LayerMotion {
		final AuraStyle style = new AuraStyle();
		boolean ready;
		float phase;
	}

	public static void renderGuiAura(Player player, PoseStack poseStack, Matrix4f projectionMatrix,
									 int x, int y, int scale, float partialTick) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return;

		boolean use3D = useAura3D(player);
		List<AuraLayer> activeLayers = getAuraLayers(player, stats, partialTick, use3D);
		if (activeLayers.isEmpty()) return;

		CachedAuraData data = GUI_AURA_DATA;
		float[] modelScale = getModelScale(stats);
		float[] body = getBodyScale(stats);
		float[] auraScale = getAuraScale(player, stats);

		data.modelScaleX = modelScale[0]; data.modelScaleY = modelScale[1]; data.modelScaleZ = modelScale[2];
		data.bodyScaleX = body[0]; data.bodyScaleY = body[1]; data.bodyScaleZ = body[2];
		data.auraScaleX = auraScale[0]; data.auraScaleY = auraScale[1]; data.auraScaleZ = auraScale[2];
		data.use3D = use3D;
		data.alphaProgress = 1.0f;
		data.lastLayers = activeLayers;
		advanceMotions(player, data, activeLayers, true);
		data.growth = 1.0f;

		Matrix4f placement = new Matrix4f(poseStack.last().pose()).translate(x, y, 50.0f)
				.mul(new Matrix4f().scaling(scale, scale, -scale))
				.rotateZ((float) Math.PI)
				.translate(0.0f, 0.0f, GUI_AURA_EYE_DISTANCE);
		Matrix4f auraProjection = new Matrix4f(projectionMatrix).mul(placement);

		poseStack.pushPose();
		poseStack.last().pose().identity();
		poseStack.last().normal().identity();
		poseStack.translate(0.0f, 0.0f, -GUI_AURA_EYE_DISTANCE);

		for (AuraLayer layer : activeLayers) {
			if (!use3D) {
				drawGuiAura2D(player, data, layer, poseStack, auraProjection, partialTick);
			} else if (Aura3DRenderer.isSmooth(layer.type)) {
				executeSmoothDraw(player, data, layer, poseStack, auraProjection, partialTick, false, true);
			} else {
				executeAura3DDraw(player, data, layer, poseStack, auraProjection, partialTick, false, true);
			}
		}

		poseStack.popPose();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static void drawGuiAura2D(Player player, CachedAuraData data, AuraLayer layer, PoseStack poseStack,
									  Matrix4f projectionMatrix, float partialTick) {
		ShaderInstance shader = DMZShaders.auraShader;
		if (shader == null) return;

		String typeStr = layer.type != null && !layer.type.isEmpty() ? layer.type.toLowerCase() : "kakarot";
		ResourceLocation mainTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/" + typeStr + "_aura.png");
		ResourceLocation sparkingTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/sparking_effects.png");

		float animSpeed = auraPhase(player, partialTick);
		float alpha = layer.alpha;
		float boost = 1.0f + layer.layerId * 0.15f;
		float finalScaleX = data.auraScaleX * AURA_2D_BASE_MULTIPLIER * boost;
		float finalScaleY = data.auraScaleY * AURA_2D_BASE_MULTIPLIER * boost;
		float finalScaleZ = data.auraScaleZ * AURA_2D_BASE_MULTIPLIER * boost;

		VertexBuffer mesh = AuraMeshFactory.getBillboardQuad();
		shader.safeGetUniform("speed").set(animSpeed);
		shader.safeGetUniform("ProjMat").set(projectionMatrix);
		applyAuraColors(shader, layer.color);

		poseStack.pushPose();
		poseStack.translate(0.0, AURA_2D_GROUND_LIFT, 0.0);
		poseStack.scale(finalScaleX, finalScaleY, finalScaleZ);
		poseStack.translate(0.0, AURA_2D_GROUND_OFFSET, 0.0);

		shader.safeGetUniform("alp1").set(alpha);
		shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
		RenderType mainRender = auraType(mainTex);
		customSetup(mainRender, mainTex, shader);
		applyAndDraw(mesh, poseStack, projectionMatrix, shader, mainTex, layer.color, alpha, animSpeed, false);
		customClear(mainRender);

		poseStack.pushPose();
		float pulse = 1.0f + (float) Math.sin((player.tickCount + partialTick) * 0.2f) * 0.05f;
		poseStack.scale(0.8f * pulse, 0.65f * pulse, 0.8f * pulse);
		poseStack.translate(0.0, -0.25, 0.0);

		RenderType sparkingRender = auraType(sparkingTex);
		customSetup(sparkingRender, sparkingTex, shader);
		shader.safeGetUniform("alp1").set(alpha * 0.8f);
		shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
		applyAndDraw(mesh, poseStack, projectionMatrix, shader, sparkingTex, layer.color, alpha * 0.8f, animSpeed, false);
		customClear(sparkingRender);
		poseStack.popPose();

		poseStack.popPose();
		VertexBuffer.unbind();
		shader.clear();
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
						List<AuraLayer> layers = getAuraLayers(player, stats, partialTick, false);
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
		List<PreparedAura> prepared = new ArrayList<>(auras.size());
		for (var entry : auras) {
			Player player = entry.player();
			boolean isLocalPlayer = player == mc.player;

			if (!isFirstPerson || isCameraColliding || !isLocalPlayer) {
				currentFramePlayers.add(player.getId());
				PreparedAura aura = prepareShaderAura(entry);
				if (aura != null) prepared.add(aura);
			}
		}

		Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
		List<List<PreparedAura>> groups = groupTouchingAuras(prepared);
		groups.sort(Comparator.comparingDouble((List<PreparedAura> group) -> group.get(0).position().distanceToSqr(camera)).reversed());
		for (List<PreparedAura> group : groups) {
			if (group.size() == 1) {
				PreparedAura aura = group.get(0);
				drawShaderAura(aura.player(), aura.data(), aura.layers(), aura.position(), aura.partialTick(), poseStack, mc, projectionMatrix);
			} else {
				drawMergedAura(group, poseStack, mc, projectionMatrix);
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
				boolean hasLightning = AuraFxState.hasLightning(stats);

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
				if (!(mc.level.getEntity(playerId) instanceof Player player) || !player.isAlive()) {
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
		RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
		if (!mainTarget.isStencilEnabled()) mainTarget.enableStencil();
		mainTarget.bindWrite(false);
		RenderSystem.stencilMask(0xFF);
		RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
		RenderSystem.stencilMask(0x00);

		LAST_RENDER_TIME.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		COLOR_PROGRESS_MAP.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		COLOR_TICK_MAP.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		PULSE_LAST_RENDER_TIME.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		PULSE_PROGRESS.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		RELEASE_SCALE_PROGRESS.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		RELEASE_SCALE_TICK.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		AURA_PHASE.keySet().removeIf(id -> !currentFramePlayers.contains(id) && !AURA_CACHE.containsKey(id));
		AuraTrailRenderer.forget(id -> currentFramePlayers.contains(id) || AURA_CACHE.containsKey(id));
	}

	private static boolean useAura3D(Player player) {
		return Aura3DRenderer.isAvailable() && AuraModeState.isAura3D(player);
	}

	private static float[] getModelScale(StatsData stats) {
		Float[] resolved = stats.getCharacter().getResolvedModelScaling();
		return new float[]{resolved[0], resolved[1], resolved[2]};
	}

	private static float[] getBodyScale(StatsData stats) {
		float[] modelScale = getModelScale(stats);
		float sX = modelScale[0], sY = modelScale[1], sZ = modelScale[2];
		var character = stats.getCharacter();

		String currentForm = character.getActiveForm() != null ? character.getActiveForm().toLowerCase() : "";
		if (currentForm.contains("ozaru")) {
			sX = Math.max(0.1f, sX - 2.8f);
			sY = Math.max(0.1f, sY - 2.8f);
			sZ = Math.max(0.1f, sZ - 2.8f);
		}

		return new float[]{sX, sY, sZ};
	}

	private static float[] getAuraScale(Player player, StatsData stats) {
		float[] renderScale = getBodyScale(stats);
		float baseScale = AURA_BASE_SCALE;
		var character = stats.getCharacter();
		String currentForm = character.getActiveForm() != null ? character.getActiveForm().toLowerCase() : "";

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) baseScale += 0.1f;
		if (character.hasActiveForm() && character.getActiveFormData() != null) baseScale += 0.1f;
		if (currentForm.contains("oozaru")) baseScale = 1.2f;
		if (currentForm.contains("supersaiyan2") || currentForm.contains("supersaiyan3") || currentForm.contains("ultra") || currentForm.contains("superperfect")) {
			baseScale += 0.2f;
		}

		baseScale += getReleaseScaleBonus(player, stats);
		baseScale *= (float) AuraFxState.auraScaleMultiplier(stats);

		return new float[]{baseScale * renderScale[0], baseScale * renderScale[1], baseScale * renderScale[2]};
	}

	private static float auraPhase(Player player, float partialTick) {
		int entityId = player.getId();
		float now = player.tickCount + partialTick;
		float[] state = AURA_PHASE.computeIfAbsent(entityId, k -> new float[]{now * 0.5f, now});

		float delta = now - state[1];
		if (delta > 0.0f && delta < 40.0f) state[0] += delta * 0.5f * (float) AuraFxState.auraSpeedMultiplier(player);
		else if (delta != 0.0f) state[0] = now * 0.5f;
		state[1] = now;
		return state[0];
	}

	private static float getReleaseScaleBonus(Player player, StatsData stats) {
		int entityId = player.getId();
		float target = stats.getSkills().hasSkill("kicontrol")
				? Mth.clamp(stats.getResources().getPowerRelease() / AURA_RELEASE_CAP, 0.0f, 1.0f) : 0.0f;

		float current = RELEASE_SCALE_PROGRESS.getOrDefault(entityId, target);
		long lastTick = RELEASE_SCALE_TICK.getOrDefault(entityId, 0L);
		long currentTick = player.tickCount;

		if (currentTick != lastTick) {
			long ticksElapsed = lastTick == 0L ? 1L : Math.max(1L, currentTick - lastTick);
			current = Mth.approach(current, target, AURA_RELEASE_LERP_PER_TICK * ticksElapsed);
			RELEASE_SCALE_TICK.put(entityId, currentTick);
			RELEASE_SCALE_PROGRESS.put(entityId, current);
		}

		return AURA_RELEASE_SCALE_BONUS * current;
	}

	private static String defaultAuraType(boolean use3D) {
		return use3D ? Aura3DRenderer.DEFAULT_TYPE : "kakarot";
	}

	private static String formAuraType(FormConfig.FormData form, boolean use3D) {
		if (form == null) return null;
		return use3D ? form.getAuraType3D() : form.getAuraType();
	}

	private static String extraAuraType(FormConfig.FormData form, boolean use3D) {
		if (form == null) return null;
		return use3D ? form.getExtraAuraType3D() : form.getExtraAuraType();
	}

	private static AuraLayer extraLayer(FormConfig.FormData form, boolean use3D) {
		AuraLayer layer = new AuraLayer(extraAuraType(form, use3D), form.getExtraAuraLayer(), form.getRgbExtraAuraColor());
		if (use3D) layer.style = AuraStyle.resolve(form.getExtraAura3DStyle(), layer.color);
		return layer;
	}

	private static List<AuraLayer> getAuraLayers(Player player, StatsData stats, float partialTick, boolean use3D) {
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
			int mastery;
			if (chargingStack) {
				String mGroup = character.hasActiveStackForm() ? character.getActiveStackFormGroup() : character.getSelectedStackFormGroup();
				mastery = (int) character.getStackFormMasteries().getMastery(mGroup, nextForm.getName());
			} else {
				String mGroup = character.hasActiveForm() ? character.getActiveFormGroup() : character.getSelectedFormGroup();
				mastery = (int) character.getFormMasteries().getMastery(mGroup, nextForm.getName());
			}
			float ratePerTick = (5 + Math.min(20, (int)(mastery * 0.2))) / 2000.0f;

			float lastProgress = COLOR_PROGRESS_MAP.getOrDefault(entityId, 0.0f);
			long lastTick = COLOR_TICK_MAP.getOrDefault(entityId, 0L);
			long currentTick = player.tickCount;

			if (currentTick != lastTick) {
				long ticksElapsed = lastTick == 0L ? 1L : Math.max(1L, currentTick - lastTick);
				lastProgress = Math.min(1.0f, lastProgress + ratePerTick * ticksElapsed);
				COLOR_TICK_MAP.put(entityId, currentTick);
				COLOR_PROGRESS_MAP.put(entityId, lastProgress);
			}
			chargeProgress = Math.max(0.0f, Math.min(1.0f, lastProgress + ratePerTick * partialTick));
		} else COLOR_PROGRESS_MAP.put(entityId, 0.0f);

		Map<Integer, AuraLayer> layerMap = new HashMap<>();

		String normalHex = character.getAuraColor();
		float[] normalColor = character.getRgbAuraColor();
		var raceCharacter = ConfigManager.getRaceCharacter(character.getRace());
		String normalType = raceCharacter != null
				? (use3D ? raceCharacter.getAuraType3D() : raceCharacter.getAuraType()) : defaultAuraType(use3D);
		int normalLayerId = 0;
		FormConfig.Aura3DStyle normalStyle = raceCharacter != null ? raceCharacter.getAura3DStyle() : FormConfig.Aura3DStyle.DEFAULT;
		FormConfig.Aura3DStyle normalChargeStyle = null;

		if (character.hasActiveForm() && character.getActiveFormData() != null) {
			var fd = character.getActiveFormData();
			if (fd.getAuraColor() != null && !fd.getAuraColor().isEmpty()) {
				normalHex = fd.getAuraColor();
				normalColor = fd.getRgbAuraColor();
			}
			String formType = formAuraType(fd, use3D);
			if (formType != null && !formType.isEmpty()) normalType = formType;
			normalLayerId = fd.getAuraLayer() != null ? fd.getAuraLayer() : 0;
			normalStyle = fd.getAura3DStyle();
		}

		AuraLayer chargeLayer = null;
		if (chargingNormal && nextForm != null) {
			String targetHex = nextForm.getAuraColor() != null && !nextForm.getAuraColor().isEmpty() ? nextForm.getAuraColor() : normalHex;
			int targetLayer = nextForm.getAuraLayer() != null ? nextForm.getAuraLayer() : normalLayerId;
			if (targetLayer == normalLayerId) {
				normalColor = interpolateColor(normalHex, targetHex, chargeProgress);
				normalChargeStyle = nextForm.getAura3DStyle();
			} else {
				String nextType = formAuraType(nextForm, use3D);
				String targetType = nextType != null && !nextType.isEmpty() ? nextType : normalType;
				chargeLayer = new AuraLayer(targetType, targetLayer, ColorUtils.hexToRgb(targetHex), chargeProgress);
				if (use3D) chargeLayer.style = AuraStyle.resolve(nextForm.getAura3DStyle(), chargeLayer.color);
			}
		}

		AuraLayer normalLayer = new AuraLayer(normalType, normalLayerId, normalColor);
		if (use3D) {
			normalLayer.style = AuraStyle.resolve(normalStyle, normalColor);
			if (normalChargeStyle != null) normalLayer.style.blendTowards(AuraStyle.resolve(normalChargeStyle, normalColor), chargeProgress);
		}
		putLayer(layerMap, normalLayerId, normalLayer);
		if (chargeLayer != null) putLayer(layerMap, chargeLayer.layerId, chargeLayer);

		if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().hasExtraAura()) {
			var fd = character.getActiveFormData();
			putShifting(layerMap, fd.getExtraAuraLayer(), extraLayer(fd, use3D));
		}

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
			var fd = character.getActiveStackFormData();
			String stackHex = fd.getAuraColor() != null && !fd.getAuraColor().isEmpty() ? fd.getAuraColor() : "#FFFFFF";
			String stackFormType = formAuraType(fd, use3D);
			String stackType = stackFormType != null && !stackFormType.isEmpty() ? stackFormType : defaultAuraType(use3D);
			int stackLayerId = fd.getAuraLayer() != null ? fd.getAuraLayer() : 1;

			float[] stackColor = (fd.getAuraColor() != null && !fd.getAuraColor().isEmpty()) ? fd.getRgbAuraColor() : ColorUtils.hexToRgb(stackHex);
			if (chargingStack && nextForm != null) {
				String targetHex = nextForm.getAuraColor() != null && !nextForm.getAuraColor().isEmpty() ? nextForm.getAuraColor() : stackHex;
				stackColor = interpolateColor(stackHex, targetHex, chargeProgress);
			}

			AuraLayer stackLayer = new AuraLayer(stackType, stackLayerId, stackColor);
			if (use3D) {
				stackLayer.style = AuraStyle.resolve(fd.getAura3DStyle(), stackColor);
				if (chargingStack && nextForm != null) stackLayer.style.blendTowards(AuraStyle.resolve(nextForm.getAura3DStyle(), stackColor), chargeProgress);
			}
			putLayer(layerMap, stackLayerId, stackLayer);

			if (fd.hasExtraAura()) {
				putShifting(layerMap, fd.getExtraAuraLayer(), extraLayer(fd, use3D));
			}

		} else if (chargingStack && nextForm != null) {
			String targetHex = nextForm.getAuraColor() != null && !nextForm.getAuraColor().isEmpty() ? nextForm.getAuraColor() : "#FFFFFF";
			String nextStackType = formAuraType(nextForm, use3D);
			String stackType = nextStackType != null && !nextStackType.isEmpty() ? nextStackType : defaultAuraType(use3D);
			int stackLayerId = nextForm.getAuraLayer() != null ? nextForm.getAuraLayer() : 1;

			if (stackLayerId == normalLayerId) {
				AuraLayer base = layerMap.get(normalLayerId);
				if (base != null) {
					base.color = interpolateColor(normalHex, targetHex, chargeProgress);
					if (base.style != null) base.style.blendTowards(AuraStyle.resolve(nextForm.getAura3DStyle(), base.color), chargeProgress);
				}
			} else {
				AuraLayer charge = new AuraLayer(stackType, stackLayerId, ColorUtils.hexToRgb(targetHex), chargeProgress);
				if (use3D) charge.style = AuraStyle.resolve(nextForm.getAura3DStyle(), charge.color);
				putLayer(layerMap, stackLayerId, charge);
			}
		}

		List<AuraLayer> activeLayers = new ArrayList<>(layerMap.values());
		activeLayers.sort(Comparator.comparingInt(l -> l.layerId));

		if (use3D) {
			String preference = AuraModeState.style(player);
			for (AuraLayer layer : activeLayers) {
				layer.type = FormConfig.resolveAura3DType(layer.type, preference);
			}
		}
		return activeLayers;
	}

	private static void putLayer(Map<Integer, AuraLayer> layerMap, int layerId, AuraLayer layer) {
		if (layerId < 0) return;
		layerId = Mth.clamp(layerId, 0, 6);
		layer.layerId = layerId;
		layerMap.put(layerId, layer);
	}

	private static void putShifting(Map<Integer, AuraLayer> layerMap, int layerId, AuraLayer layer) {
		if (layerId < 0) return;
		layerId = Mth.clamp(layerId, 0, 6);
		while (layerMap.containsKey(layerId) && layerId < 6) layerId++;
		layer.layerId = layerId;
		layerMap.put(layerId, layer);
	}

	private static float[] interpolateColor(String hexFrom, String hexTo, float factor) {
		float[] rgbFrom = ColorUtils.hexToRgb(hexFrom);
		float[] rgbTo = ColorUtils.hexToRgb(hexTo);

		float r = Mth.lerp(factor, rgbFrom[0], rgbTo[0]);
		float g = Mth.lerp(factor, rgbFrom[1], rgbTo[1]);
		float b = Mth.lerp(factor, rgbFrom[2], rgbTo[2]);

		return new float[]{r, g, b};
	}

	private static PoseStack shaderpackViewStack(Minecraft mc) {
		PoseStack stack = new PoseStack();
		var cam = mc.gameRenderer.getMainCamera();
		stack.mulPose(Axis.XP.rotationDegrees(cam.getXRot()));
		stack.mulPose(Axis.YP.rotationDegrees(cam.getYRot() + 180.0F));
		return stack;
	}

	public static void renderShaderFirstPersonAura(Player player, float partialTick, PoseStack poseStack, Minecraft mc, Matrix4f projectionMatrix) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return;

		int playerId = player.getId();
		CachedAuraData data = AURA_CACHE.computeIfAbsent(playerId, k -> new CachedAuraData());
		long gameTime = player.level().getGameTime();

		if (gameTime - LAST_RENDER_TIME.getOrDefault(playerId, 0L) > 2) {
			data.alphaProgress = 0.0f;
			data.growth = 0.0f;
		}
		LAST_RENDER_TIME.put(playerId, gameTime);

		if (data.alphaProgress < 1.0f) {
			data.alphaProgress += FADE_SPEED;
			if (data.alphaProgress > 1.0f) data.alphaProgress = 1.0f;
		}

		float[] modelScale = getModelScale(stats);
		float[] body = getBodyScale(stats);
		float[] auraScale = getAuraScale(player, stats);

		data.modelScaleX = modelScale[0]; data.modelScaleY = modelScale[1]; data.modelScaleZ = modelScale[2];
		data.bodyScaleX = body[0]; data.bodyScaleY = body[1]; data.bodyScaleZ = body[2];
		data.auraScaleX = auraScale[0]; data.auraScaleY = auraScale[1]; data.auraScaleZ = auraScale[2];

		data.use3D = useAura3D(player);
		List<AuraLayer> activeLayers = getAuraLayers(player, stats, partialTick, data.use3D);
		if (activeLayers.isEmpty()) return;
		data.lastLayers = activeLayers;
		advanceMotions(player, data, activeLayers, true);

		if (IrisCompat.isShaderPackInUse()) {
			poseStack = shaderpackViewStack(mc);
		}

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

		drawFlightTrail(player, data, activeLayers, poseStack, projectionMatrix, partialTick);

		for (AuraLayer layer : activeLayers) {
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);
			executeAuraShaderDraw(player, data, layer, poseStack, mc, projectionMatrix,
					new Vec3(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z), partialTick, data.alphaProgress, true);
			poseStack.popPose();
		}
	}

	private record PreparedAura(Player player, CachedAuraData data, List<AuraLayer> layers, Vec3 position, float partialTick, float battlePower, boolean mergeable) {}

	private static PreparedAura prepareShaderAura(PlayerEffectQueue.AuraRenderEntry entry) {
		var player = entry.player();
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return null;

		int playerId = player.getId();
		CachedAuraData data = AURA_CACHE.computeIfAbsent(playerId, k -> new CachedAuraData());
		long gameTime = player.level().getGameTime();

		if (gameTime - LAST_RENDER_TIME.getOrDefault(playerId, 0L) > 2) {
			data.alphaProgress = 0.0f;
			data.growth = 0.0f;
		}
		LAST_RENDER_TIME.put(playerId, gameTime);

		if (data.alphaProgress < 1.0f) {
			data.alphaProgress += FADE_SPEED;
			if (data.alphaProgress > 1.0f) data.alphaProgress = 1.0f;
		}

		float[] modelScale = getModelScale(stats);
		float[] body = getBodyScale(stats);
		float[] auraScale = getAuraScale(player, stats);

		data.modelScaleX = modelScale[0]; data.modelScaleY = modelScale[1]; data.modelScaleZ = modelScale[2];
		data.bodyScaleX = body[0]; data.bodyScaleY = body[1]; data.bodyScaleZ = body[2];
		data.auraScaleX = auraScale[0]; data.auraScaleY = auraScale[1]; data.auraScaleZ = auraScale[2];
		data.playerModel = entry.playerModel();

		data.use3D = useAura3D(player);
		List<AuraLayer> activeLayers = getAuraLayers(player, stats, entry.partialTick(), data.use3D);
		if (activeLayers.isEmpty()) return null;
		data.lastLayers = activeLayers;
		advanceMotions(player, data, activeLayers, true);

		float partialTick = entry.partialTick();
		Vec3 position = new Vec3(Mth.lerp(partialTick, player.xo, player.getX()), Mth.lerp(partialTick, player.yo, player.getY()), Mth.lerp(partialTick, player.zo, player.getZ()));

		boolean mergeable = !isFastFlying(player) && player.getSwimAmount(partialTick) <= 0.0f;

		return new PreparedAura(player, data, activeLayers, position, partialTick, stats.getBattlePower(), mergeable);
	}

	private static void drawShaderAura(Player player, CachedAuraData data, List<AuraLayer> activeLayers, Vec3 position,
									   float partialTick, PoseStack poseStack, Minecraft mc, Matrix4f projectionMatrix) {
		if (IrisCompat.isShaderPackInUse()) {
			poseStack = shaderpackViewStack(mc);
		}

		Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
		double offsetX = position.x - cameraPos.x;
		double offsetY = position.y - cameraPos.y;
		double offsetZ = position.z - cameraPos.z;

		if (player.onGround()) {
			AuraLayer topLayer = activeLayers.get(activeLayers.size() - 1);
			poseStack.pushPose();
			poseStack.translate(offsetX, offsetY + 0.05, offsetZ);
			renderShaderPulseAura(player, data, topLayer, poseStack, mc, projectionMatrix, partialTick, data.alphaProgress);
			poseStack.popPose();
		}

		drawFlightTrail(player, data, activeLayers, poseStack, projectionMatrix, partialTick);

		for (AuraLayer layer : activeLayers) {
			poseStack.pushPose();
			poseStack.translate(offsetX, offsetY, offsetZ);
			executeAuraShaderDraw(player, data, layer, poseStack, mc, projectionMatrix, new Vec3(offsetX, offsetY, offsetZ), partialTick, data.alphaProgress, false);
			poseStack.popPose();
		}
	}

	private static List<List<PreparedAura>> groupTouchingAuras(List<PreparedAura> auras) {
		int count = auras.size();
		int[] parent = new int[count];
		for (int i = 0; i < count; i++) parent[i] = i;

		for (int i = 0; i < count; i++) {
			for (int j = i + 1; j < count; j++) {
				if (!aurasTouch(auras.get(i), auras.get(j))) continue;
				int rootI = findRoot(parent, i);
				int rootJ = findRoot(parent, j);
				if (rootI != rootJ) parent[rootJ] = rootI;
			}
		}

		Map<Integer, List<PreparedAura>> groups = new LinkedHashMap<>();
		for (int i = 0; i < count; i++) {
			groups.computeIfAbsent(findRoot(parent, i), k -> new ArrayList<>()).add(auras.get(i));
		}
		return new ArrayList<>(groups.values());
	}

	private static int findRoot(int[] parent, int index) {
		while (parent[index] != index) {
			parent[index] = parent[parent[index]];
			index = parent[index];
		}
		return index;
	}

	private static boolean aurasTouch(PreparedAura a, PreparedAura b) {
		if (!a.mergeable() || !b.mergeable() || a.data().use3D != b.data().use3D) return false;

		double dx = a.position().x - b.position().x;
		double dz = a.position().z - b.position().z;
		double reach = auraMergeRadius(a) + auraMergeRadius(b);
		if (dx * dx + dz * dz > reach * reach) return false;

		return Math.abs(a.position().y - b.position().y) < (auraMergeHeight(a) + auraMergeHeight(b)) * 0.5;
	}

	private static float auraMergeRadius(PreparedAura aura) {
		AuraLayer top = aura.layers().get(aura.layers().size() - 1);
		float boost = 1.0f + top.layerId * 0.15f;
		float width = aura.data().use3D ? Aura3DRenderer.widthFactor(top.type) : AURA_2D_MERGE_WIDTH;
		return aura.data().auraScaleX * width * boost;
	}

	private static float auraMergeHeight(PreparedAura aura) {
		AuraLayer top = aura.layers().get(aura.layers().size() - 1);
		float boost = 1.0f + top.layerId * 0.15f;
		float height = aura.data().use3D ? Aura3DRenderer.heightFactor(top.type) * AURA_3D_VISIBLE_HEIGHT : AURA_2D_MERGE_HEIGHT;
		return aura.data().auraScaleY * height * boost;
	}

	private static void drawMergedAura(List<PreparedAura> group, PoseStack poseStack, Minecraft mc, Matrix4f projectionMatrix) {
		PreparedAura leader = group.get(0);
		for (PreparedAura aura : group) {
			if (aura.battlePower() > leader.battlePower()) leader = aura;
		}

		float scaleX = 0.0f, scaleY = 0.0f, scaleZ = 0.0f;
		double x = 0.0, y = 0.0, z = 0.0;
		for (PreparedAura aura : group) {
			scaleX += aura.data().auraScaleX;
			scaleY += aura.data().auraScaleY;
			scaleZ += aura.data().auraScaleZ;
			x += aura.position().x;
			y += aura.position().y;
			z += aura.position().z;
		}

		CachedAuraData leaderData = leader.data();
		CachedAuraData merged = new CachedAuraData();
		merged.auraScaleX = scaleX / AURA_MERGE_SIZE_DIVISOR;
		merged.auraScaleY = scaleY / AURA_MERGE_SIZE_DIVISOR;
		merged.auraScaleZ = scaleZ / AURA_MERGE_SIZE_DIVISOR;
		merged.bodyScaleX = leaderData.bodyScaleX; merged.bodyScaleY = leaderData.bodyScaleY; merged.bodyScaleZ = leaderData.bodyScaleZ;
		merged.modelScaleX = leaderData.modelScaleX; merged.modelScaleY = leaderData.modelScaleY; merged.modelScaleZ = leaderData.modelScaleZ;
		merged.alphaProgress = leaderData.alphaProgress;
		merged.growth = leaderData.growth;
		merged.motions = leaderData.motions;
		merged.use3D = leaderData.use3D;
		merged.playerModel = leaderData.playerModel;
		merged.lastLayers = blendMergedLayers(leader, group);

		int members = group.size();
		Vec3 centre = new Vec3(x / members, y / members, z / members);
		drawShaderAura(leader.player(), merged, merged.lastLayers, centre, leader.partialTick(), poseStack, mc, projectionMatrix);
	}

	private static List<AuraLayer> blendMergedLayers(PreparedAura leader, List<PreparedAura> group) {
		List<AuraLayer> blended = new ArrayList<>(leader.layers().size());
		for (int i = 0; i < leader.layers().size(); i++) {
			AuraLayer base = leader.layers().get(i);
			float r = 0.0f, g = 0.0f, b = 0.0f, totalWeight = 0.0f;

			for (PreparedAura aura : group) {
				List<AuraLayer> layers = aura.layers();
				float[] color = layers.get(Math.min(i, layers.size() - 1)).color;
				float weight = aura == leader ? AURA_MERGE_LEADER_COLOR_WEIGHT : 1.0f;
				r += color[0] * weight;
				g += color[1] * weight;
				b += color[2] * weight;
				totalWeight += weight;
			}

			blended.add(new AuraLayer(base.type, base.layerId, new float[]{r / totalWeight, g / totalWeight, b / totalWeight}, base.alpha));
		}
		return blended;
	}

	private static boolean renderShaderGhostAura(Player player, CachedAuraData data, PoseStack poseStack, Minecraft mc, float partialTick, Matrix4f projectionMatrix) {
		if (data.alphaProgress > 0.0f) {
			data.alphaProgress -= FADE_SPEED;
			if (data.alphaProgress < 0.0f) data.alphaProgress = 0.0f;
		}

		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return false;

		List<AuraLayer> activeLayers = data.lastLayers;
		if (activeLayers == null || activeLayers.isEmpty()) return false;

		advanceMotions(player, data, activeLayers, false);
		boolean shellVisible = data.use3D && data.growth > 0.001f;
		if (data.alphaProgress <= 0.001f && !shellVisible) return false;

		if (IrisCompat.isShaderPackInUse()) {
			poseStack = shaderpackViewStack(mc);
		}

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

		drawFlightTrail(player, data, activeLayers, poseStack, projectionMatrix, partialTick);

		for (AuraLayer layer : activeLayers) {
			poseStack.pushPose();
			poseStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);
			executeAuraShaderDraw(player, data, layer, poseStack, mc, projectionMatrix,
					new Vec3(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z), partialTick, data.alphaProgress, isFirstPerson);
			poseStack.popPose();
		}
		return true;
	}

	private static void executeAuraShaderDraw(Player player, CachedAuraData data, AuraLayer layer, PoseStack poseStack, Minecraft mc, Matrix4f projectionMatrix, Vec3 cameraOffset, float partialTick, float alphaMultiplier, boolean isFirstPerson) {
		boolean isLocalPlayer = player == mc.player;
		if (data.use3D) {
			if (Aura3DRenderer.isSmooth(layer.type)) {
				executeSmoothDraw(player, data, layer, poseStack, projectionMatrix, partialTick, isLocalPlayer && isFirstPerson, false);
			} else {
				executeAura3DDraw(player, data, layer, poseStack, projectionMatrix, partialTick, isLocalPlayer && isFirstPerson, false);
			}
			return;
		}
		if (alphaMultiplier <= 0.001f) return;

		ShaderInstance shader = DMZShaders.auraShader;
		if (shader == null) return;

		float maxAlpha = (isLocalPlayer && isFirstPerson) ? 0.5f : 1.0f;
		float finalAlpha = maxAlpha * alphaMultiplier * layer.alpha;

		String typeStr = layer.type != null && !layer.type.isEmpty() ? layer.type.toLowerCase() : "kakarot";
		ResourceLocation mainTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/" + typeStr + "_aura.png");
		ResourceLocation crossTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/" + typeStr + "_cross.png");
		ResourceLocation sparkingTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/sparking_effects.png");

		float animSpeed = auraPhase(player, partialTick);

		shader.safeGetUniform("speed").set(animSpeed);
		shader.safeGetUniform("ProjMat").set(projectionMatrix);
		applyAuraColors(shader, layer.color);

		float finalScaleX = data.auraScaleX * AURA_2D_BASE_MULTIPLIER * (1.0f + layer.layerId * 0.15f);
		float finalScaleY = data.auraScaleY * AURA_2D_BASE_MULTIPLIER * (1.0f + layer.layerId * 0.15f);
		float finalScaleZ = data.auraScaleZ * AURA_2D_BASE_MULTIPLIER * (1.0f + layer.layerId * 0.15f);

		VertexBuffer mesh = AuraMeshFactory.getBillboardQuad();

		if (isLocalPlayer && isFirstPerson) {
			poseStack.pushPose();
			poseStack.last().pose().identity();
			poseStack.last().normal().identity();
			poseStack.translate(0.0, -0.6, -0.7);

			float normalizedScaleX = finalScaleX / (data.modelScaleX > 0 ? data.modelScaleX : 1.0f);
			float normalizedScaleY = finalScaleY / (data.modelScaleY > 0 ? data.modelScaleY : 1.0f);

			poseStack.scale(normalizedScaleX * 3.0f, normalizedScaleY * 3.0f, 1.0f);

			shader.safeGetUniform("alp1").set(finalAlpha * 0.45f);
			shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
			RenderType mainRender = auraType(mainTex);
			customSetup(mainRender, mainTex, shader);
			applyAndDraw(mesh, poseStack, projectionMatrix, shader, mainTex, layer.color, finalAlpha * 0.45f, animSpeed, false);
			customClear(mainRender);

			RenderType sparkingRender = auraType(sparkingTex);
			customSetup(sparkingRender, sparkingTex, shader);
			poseStack.scale(0.6f, 0.45f, 0.6f);
			shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
			applyAndDraw(mesh, poseStack, projectionMatrix, shader, sparkingTex, layer.color, finalAlpha * 0.45f, animSpeed, false);
			customClear(sparkingRender);

			poseStack.popPose();
			VertexBuffer.unbind();
			shader.clear();
			return;
		}

		Camera camera = mc.gameRenderer.getMainCamera();
		boolean flightBillboard = isFastFlying(player) && !isFirstPerson;

		Vec3 auraUp = flightBillboard ? flightAxis(player, partialTick) : AURA_2D_UP;
		double baseLift = flightBillboard ? player.getBbHeight() * 0.5f : AURA_2D_GROUND_LIFT;
		double axisLift = (flightBillboard ? AURA_2D_FLIGHT_OFFSET : AURA_2D_GROUND_OFFSET) * finalScaleY;
		Vec3 centre = cameraOffset.add(0.0, baseLift, 0.0).add(auraUp.scale(axisLift));

		double centreDistance = centre.length();

		float crossFactor = 0.0f;
		float pitchSquash = 1.0f;
		if (flightBillboard) {
			float alignment = flightAxisAlignment(player, mc, partialTick);
			if (alignment > 0.707f) {
				crossFactor = (float) Math.pow((alignment - 0.707f) / 0.293f, 2.0);
				pitchSquash = 1.0f - (crossFactor * 0.5f);
			}
		} else if (!isFirstPerson && centreDistance > 1.0e-4) {
			float elevation = (float) Math.toDegrees(Math.asin(Mth.clamp(Math.abs(centre.dot(auraUp) / centreDistance), 0.0, 1.0)));
			if (elevation > AURA_2D_CROSS_START_DEG) {
				float t = (elevation - AURA_2D_CROSS_START_DEG) / (90.0f - AURA_2D_CROSS_START_DEG);
				crossFactor = Mth.clamp(t * t, 0.0f, 1.0f);
				pitchSquash = 1.0f - (crossFactor * 0.5f);
			}
		}

		float fadeStart = Mth.clamp(finalScaleX * AURA_2D_NEAR_FADE_SPAN, AURA_2D_NEAR_FADE_MIN, AURA_2D_NEAR_FADE_MAX);
		float fadeEnd = fadeStart * AURA_2D_NEAR_FADE_FLOOR;
		finalAlpha *= (float) Mth.clamp((centreDistance - fadeEnd) / (fadeStart - fadeEnd), 0.0, 1.0);
		if (finalAlpha <= 0.001f) return;

		if (crossFactor < 1.0f) {
			poseStack.pushPose();

			poseStack.translate(0.0, baseLift, 0.0);
			Vec3 facing = flightBillboard ? centre : new Vec3(camera.getLookVector()).scale(-1.0);
			if (applyAxisBillboard(poseStack, camera, auraUp, facing)) {
				poseStack.scale(finalScaleX, finalScaleY * pitchSquash, finalScaleZ);

				poseStack.translate(0.0, flightBillboard ? AURA_2D_FLIGHT_OFFSET : AURA_2D_GROUND_OFFSET, 0.0);

				shader.safeGetUniform("alp1").set((1.0f - crossFactor) * finalAlpha);
				shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
				RenderType mainRender = auraType(mainTex);
				customSetup(mainRender, mainTex, shader);
				applyAndDraw(mesh, poseStack, projectionMatrix, shader, mainTex, layer.color, (1.0f - crossFactor) * finalAlpha, animSpeed, false);
				customClear(mainRender);

				poseStack.pushPose();
				float sparkingPulse = 1.0f + (float) Math.sin((player.tickCount + partialTick) * 0.2f) * 0.05f;
				poseStack.scale(0.8f * sparkingPulse, 0.65f * sparkingPulse, 0.8f * sparkingPulse);

				poseStack.translate(0.0, -0.25, 0.0);

				RenderType sparkingRender = auraType(sparkingTex);
				customSetup(sparkingRender, sparkingTex, shader);
				shader.safeGetUniform("alp1").set((1.0f - crossFactor) * finalAlpha * 0.8f);
				shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
				applyAndDraw(mesh, poseStack, projectionMatrix, shader, sparkingTex, layer.color, (1.0f - crossFactor) * finalAlpha * 0.8f, animSpeed, false);
				customClear(sparkingRender);
				poseStack.popPose();
			}

			poseStack.popPose();
		}

		if (crossFactor > 0.0f) {
			poseStack.pushPose();
			poseStack.translate(0.0, baseLift, 0.0);
			if (applyAxisBillboard(poseStack, camera, auraUp, new Vec3(camera.getLookVector()))) {
				if (!flightBillboard && centre.dot(auraUp) > 0.0) poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
				poseStack.scale(finalScaleX, 1.0f, finalScaleZ);

				shader.safeGetUniform("alp1").set(crossFactor * finalAlpha);
				shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
				RenderType crossRender = auraType(crossTex);
				customSetup(crossRender, crossTex, shader);
				VertexBuffer groundMesh = AuraMeshFactory.getGroundQuad();
				applyAndDraw(groundMesh, poseStack, projectionMatrix, shader, crossTex, layer.color, crossFactor * finalAlpha, animSpeed, true);
				customClear(crossRender);
			}

			poseStack.popPose();
		}

		VertexBuffer.unbind();
		shader.clear();
	}

	private static Vec3 flightAxis(Player player, float partialTick) {
		Vec3 axis = player.getViewVector(partialTick).scale(-1.0);
		return axis.lengthSqr() < 1.0e-6 ? new Vec3(0.0, 1.0, 0.0) : axis.normalize();
	}

	private static float flightAxisAlignment(Player player, Minecraft mc, float partialTick) {
		Vec3 viewDir = new Vec3(mc.gameRenderer.getMainCamera().getLookVector());
		return (float) Math.abs(viewDir.dot(flightAxis(player, partialTick)));
	}

	private static boolean applyAxisBillboard(PoseStack poseStack, Camera camera, Vec3 up, Vec3 facing) {
		Vec3 forward = facing.subtract(up.scale(facing.dot(up)));
		if (forward.lengthSqr() < 1.0e-6) {
			Vec3 cameraUp = new Vec3(camera.getUpVector());
			forward = cameraUp.subtract(up.scale(cameraUp.dot(up)));
		}
		if (forward.lengthSqr() < 1.0e-6) return false;
		forward = forward.normalize();

		Vec3 right = up.cross(forward).normalize();

		poseStack.mulPoseMatrix(new Matrix4f(
				(float) right.x, (float) right.y, (float) right.z, 0.0f,
				(float) up.x, (float) up.y, (float) up.z, 0.0f,
				(float) forward.x, (float) forward.y, (float) forward.z, 0.0f,
				0.0f, 0.0f, 0.0f, 1.0f));
		return true;
	}

	private static float auraLeanDegrees(Player player, float partialTick) {
		if (isFastFlying(player)) return 90.0f - player.getViewXRot(partialTick);

		float swim = player.getSwimAmount(partialTick);
		if (swim > 0.0f) {
			float laidDown = player.isInWater() ? -90.0f - player.getViewXRot(partialTick) : -90.0f;
			return Mth.lerp(swim, 0.0f, laidDown);
		}
		return 0.0f;
	}

	private static boolean isFastFlying(Player player) {
		return player instanceof AbstractClientPlayer clientPlayer
				&& FlySkillEvent.getInstance().isFlyingFast(clientPlayer);
	}

	private static void advanceMotions(Player player, CachedAuraData data, List<AuraLayer> layers, boolean active) {
		long now = Util.getNanos();
		float dt = data.motionNanos == 0L ? 0.0f : Math.min(MAX_MOTION_STEP, (now - data.motionNanos) / 1.0e9f);
		data.motionNanos = now;
		if (Minecraft.getInstance().isPaused()) dt = 0.0f;

		data.growth = active
				? Math.min(1.0f, data.growth + dt * SMOOTH_GROWTH_RATE)
				: Math.max(0.0f, data.growth - dt * SMOOTH_SHRINK_RATE);
		if (!data.use3D || layers == null) return;

		float speed = (float) AuraFxState.auraSpeedMultiplier(player);
		float blend = 1.0f - (float) Math.exp(-dt * SMOOTH_STYLE_BLEND_RATE);
		for (AuraLayer layer : layers) {
			if (layer.style == null) continue;
			LayerMotion motion = data.motions.computeIfAbsent(layer.layerId, k -> new LayerMotion());
			if (!motion.ready) {
				motion.style.copyFrom(layer.style);
				motion.ready = true;
			} else {
				motion.style.blendTowards(layer.style, blend);
			}
			motion.phase += dt * motion.style.waveSpeed * speed;
			motion.phase -= (float) Math.floor(motion.phase);
		}
	}

	private static float smoothFade(CachedAuraData data) {
		return Mth.clamp(data.growth * 3.0f, 0.0f, 1.0f);
	}

	private static void executeSmoothDraw(Player player, CachedAuraData data, AuraLayer layer, PoseStack poseStack,
										  Matrix4f projectionMatrix, float partialTick, boolean firstPerson, boolean upright) {
		LayerMotion motion = data.motions.get(layer.layerId);
		if (motion == null || !motion.ready) return;

		float bodyRot = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
		float boost = (1.0f + layer.layerId * 0.15f) / Aura3DRenderer.SMOOTH_SCALE_REFERENCE;
		float scaleX = data.auraScaleX * boost;
		float scaleY = data.auraScaleY * boost;
		float scaleZ = data.auraScaleZ * boost;

		boolean fastFlying = !upright && isFastFlying(player);
		boolean laidDown = !upright && (fastFlying || player.getSwimAmount(partialTick) > 0.0f);

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyRot));
		if (laidDown) {
			poseStack.translate(0.0f, player.getBbHeight() * 0.5f, 0.0f);
			poseStack.mulPose(Axis.XP.rotationDegrees(auraLeanDegrees(player, partialTick)));
			poseStack.translate(0.0f, -Aura3DRenderer.SMOOTH_CENTER * scaleY, 0.0f);
			if (fastFlying) poseStack.translate(0.0f, -SMOOTH_FLIGHT_LEAD, 0.0f);
		}

		float alpha = (firstPerson ? SMOOTH_FIRST_PERSON_ALPHA : 1.0f) * layer.alpha * smoothFade(data);
		float time = (player.tickCount + partialTick) / 20.0f;
		Aura3DRenderer.drawSmooth(poseStack, projectionMatrix, motion.style, alpha, data.growth, time, motion.phase,
				scaleX, scaleY, scaleZ, firstPerson ? Aura3DRenderer.SMOOTH_BACKFACE_INSIDE : Aura3DRenderer.SMOOTH_BACKFACE);

		poseStack.popPose();
	}

	private static void executeAura3DDraw(Player player, CachedAuraData data, AuraLayer layer, PoseStack poseStack,
										  Matrix4f projectionMatrix, float partialTick, boolean firstPerson, boolean upright) {
		LayerMotion motion = data.motions.get(layer.layerId);
		if (motion == null || !motion.ready) return;

		float bodyRot = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
		float boost = 1.0f + layer.layerId * 0.15f;
		float time = auraPhase(player, partialTick) / 10.0f;

		float widthBoost = Aura3DRenderer.isSmooth(layer.type)
				? boost
				: 1.0f + layer.layerId * SPARKING_LAYER_WIDTH;
		float width = Aura3DRenderer.widthFactor(layer.type) * widthBoost;
		float height = Aura3DRenderer.heightFactor(layer.type) * boost;
		float scaleX = data.auraScaleX * width;
		float scaleY = data.auraScaleY * height;
		float scaleZ = data.auraScaleZ * width;
		float pivot = Aura3DRenderer.pivotFactor(layer.type);

		boolean fastFlying = !upright && isFastFlying(player);
		boolean laidDown = !upright && (fastFlying || player.getSwimAmount(partialTick) > 0.0f);

		poseStack.pushPose();
		if (laidDown || Aura3DRenderer.followsBodyYaw(layer.type)) {
			poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyRot));
		}

		if (laidDown) {
			poseStack.translate(0.0f, player.getBbHeight() * 0.5f, 0.0f);
			poseStack.mulPose(Axis.XP.rotationDegrees(auraLeanDegrees(player, partialTick)));
			poseStack.translate(0.0f, -Aura3DRenderer.coreFactor(layer.type) * scaleY, 0.0f);
			if (fastFlying) poseStack.translate(0.0f, -AURA_3D_FLIGHT_LEAD, 0.0f);
		}

		float alpha = (firstPerson ? AURA_3D_FIRST_PERSON_ALPHA : 1.0f) * layer.alpha * smoothFade(data);
		Aura3DRenderer.drawSparking(poseStack, projectionMatrix, motion.style, alpha, data.growth, time,
				scaleX, scaleY, scaleZ, pivot,
				firstPerson ? AURA_3D_BACKFACE_FIRST_PERSON : AURA_3D_BACKFACE);

		poseStack.popPose();
	}

	private static void drawFlightTrail(Player player, CachedAuraData data, List<AuraLayer> layers, PoseStack poseStack, Matrix4f projectionMatrix, float partialTick) {
		if (layers == null || layers.isEmpty()) return;
		AuraTrailRenderer.update(player, isFastFlying(player));

		Minecraft mc = Minecraft.getInstance();
		boolean ownTrailInFirstPerson = player == mc.player && mc.options.getCameraType().isFirstPerson();
		if (ownTrailInFirstPerson) return;

		AuraLayer top = layers.get(layers.size() - 1);
		AuraTrailRenderer.render(player, top.color, data.alphaProgress * AURA_TRAIL_ALPHA * top.alpha, poseStack, projectionMatrix, partialTick);
	}

	private static void drawSinglePulse3D(Player player, CachedAuraData data, AuraLayer topLayer, PoseStack poseStack,
										  Matrix4f projectionMatrix, float partialTick, float progress) {
		LayerMotion motion = data.motions.get(topLayer.layerId);
		if (motion == null || !motion.ready) return;

		float expansion = 1.0f + (3.0f * progress);
		float alphaCurve = (float) Math.sin(progress * Math.PI);
		float boost = 1.0f + topLayer.layerId * 0.15f;
		float alpha = alphaCurve * 0.5f * topLayer.alpha * smoothFade(data);
		float spin = (player.level().getGameTime() + partialTick) * 2.5f;

		float radius = data.auraScaleX * expansion * boost * Aura3DRenderer.widthFactor(topLayer.type) * 0.75f;
		if (Aura3DRenderer.isSmooth(topLayer.type)) {
			Aura3DRenderer.drawSmoothGroundPulse(poseStack, projectionMatrix, motion.style, alpha,
					(player.tickCount + partialTick) / 20.0f, motion.phase, radius, data.auraScaleY * 0.22f, spin);
			return;
		}
		Aura3DRenderer.drawSparkingGroundPulse(poseStack, projectionMatrix, motion.style, alpha,
				auraPhase(player, partialTick) / 10.0f, radius, data.auraScaleY * 0.22f, spin);
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
		if (data.use3D) {
			drawSinglePulse3D(player, data, topLayer, poseStack, projectionMatrix, partialTick, progress);
			return;
		}

		float expansion = 1.0f + (6.0f * progress);
		float alphaCurve = (float) Math.sin(progress * Math.PI);

		String typeStr = topLayer.type != null && !topLayer.type.isEmpty() ? topLayer.type.toLowerCase() : "kakarot";
		ResourceLocation crossTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/" + typeStr + "_cross.png");

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

		float animSpeed = auraPhase(player, partialTick);

		shader.safeGetUniform("speed").set(animSpeed);
		shader.safeGetUniform("ProjMat").set(projectionMatrix);
		shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());

		applyAuraColors(shader, topLayer.color);

		shader.safeGetUniform("alp1").set(alphaCurve * 0.6f * alphaMultiplier * topLayer.alpha);

		RenderType pulseRender = auraType(crossTex);
		customSetup(pulseRender, crossTex, shader);

		VertexBuffer mesh = AuraMeshFactory.getGroundQuad();
		applyAndDraw(mesh, poseStack, projectionMatrix, shader, crossTex, topLayer.color, alphaCurve * 0.6f * alphaMultiplier * topLayer.alpha, animSpeed, true);

		customClear(pulseRender);
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
			int segments = 30;
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

		if (!AuraFxState.hasLightning(stats)) return;

		ShaderInstance shader = DMZShaders.lightningShader;
		if (shader == null) return;

		boolean isAuraActive = stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura();
		float speedMod = (isAuraActive ? 1.0f : 0.20f) * AuraFxState.lightningSpeedMultiplier(stats);
		int maxBranches = isAuraActive ? 5 : 3;
		float maxScale = isAuraActive ? 0.5f : 0.25f;

		float[] colorRgb = ColorUtils.hexToRgb(AuraFxState.lightningColor(stats));
		float[] coreRgb = {Mth.lerp(0.8f, colorRgb[0], 1.0f), Mth.lerp(0.8f, colorRgb[1], 1.0f), Mth.lerp(0.8f, colorRgb[2], 1.0f)};
		float time = (player.tickCount + partialTick) / 20.0f;

		shader.safeGetUniform("projectionMatrix").set(projectionMatrix);
		shader.safeGetUniform("time").set(time);
		shader.safeGetUniform("speedModifier").set(speedMod);

		boolean isLocalPlayer = player == Minecraft.getInstance().player;
		boolean isFirstPerson = isLocalPlayer && Minecraft.getInstance().options.getCameraType().isFirstPerson();
		float cameraAlpha = (isLocalPlayer && isFirstPerson) ? 0.25f : 1.0f;

		shader.safeGetUniform("color1").set(coreRgb[0], coreRgb[1], coreRgb[2]);
		shader.safeGetUniform("color2").set(colorRgb[0], colorRgb[1], colorRgb[2]);
		shader.safeGetUniform("alp1").set(cameraAlpha);
		shader.safeGetUniform("alp2").set(0.1f * cameraAlpha);
		shader.safeGetUniform("power").set(3.0f);
		shader.safeGetUniform("divis").set(1.0f);

		ResourceLocation lightningTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");
		RenderType renderType = lightningType(lightningTex);
		customSetup(renderType, lightningTex, shader);

		shader.apply();
		VertexBuffer mesh = getLightningMesh();
		mesh.bind();

		poseStack.pushPose();
		poseStack.last().pose().set(basePose);

		long tickInterval = isAuraActive ? 2L : 20L;
		long timeHash = player.level().getGameTime() / tickInterval;
		Random seededRand = new Random(player.getId() + timeHash);
		float[] bScale = getBodyScale(stats);
		float bbHeight = player.getBbHeight() * bScale[1];

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
			shader.safeGetUniform("normalMatrix").set(poseStack.last().normal());
			shader.apply();

			mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
			captureLightningBloom(poseStack.last().pose(), new Matrix4f(poseStack.last().normal()), projectionMatrix,
					time, speedMod, coreRgb, colorRgb, cameraAlpha);
			poseStack.popPose();
		}

		poseStack.popPose();
		VertexBuffer.unbind();
		shader.clear();
		customClear(renderType);
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
