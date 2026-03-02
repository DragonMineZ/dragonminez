package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.mixin.client.PostChainAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TransformationPostShaderManager {
	private static final ResourceLocation TRANSFORMATION_EFFECT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shaders/post/transformation_outline.json");
	private static final String UNPACK_PASS_NAME = Reference.MOD_ID + ":transformation_unpack";
	private static final String SOBEL_PASS_NAME = Reference.MOD_ID + ":transformation_sobel";
	private static final String BLUR_H_PASS_NAME = Reference.MOD_ID + ":transformation_blur_h";
	private static final String BLUR_V_PASS_NAME = Reference.MOD_ID + ":transformation_blur_v";
	private static final String COMPOSITE_PASS_NAME = Reference.MOD_ID + ":transformation_composite";
	private static final String MASK_TARGET = "entity_mask";

	private static final Map<UUID, TrackedShaderState> TRACKED_PLAYERS = new HashMap<>();
	private static final Set<UUID> ACTIVE_MASK_PLAYERS = new HashSet<>();

	private static boolean loadedByManager = false;
	@Nullable
	private static ShaderUniformState activeUniformState;
	private static TransformationMaskBufferSource maskBufferSource = new TransformationMaskBufferSource();

	private TransformationPostShaderManager() {
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			clearState(mc, true);
			return;
		}

		updateTrackedPlayers(mc);
		if (ACTIVE_MASK_PLAYERS.isEmpty() || activeUniformState == null || !ModRenderTypes.hasTransformationMaskShader()) {
			shutdownManagedShader(mc);
			return;
		}

		ensureShaderLoaded(mc);
	}

	public static TransformationMaskBufferSource getMaskBufferSource() {
		return maskBufferSource;
	}

	@Nullable
	public static MaskData getEntityMaskData(Player player) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || player == null) {
			return null;
		}

		UUID playerId = player.getUUID();
		if (!ACTIVE_MASK_PLAYERS.contains(playerId)) {
			return null;
		}

		if (!ModRenderTypes.hasTransformationMaskShader() || !isTransformationShaderActive(mc)) {
			return null;
		}

		if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
			return null;
		}

		TrackedShaderState tracked = TRACKED_PLAYERS.get(playerId);
		if (tracked == null || tracked.expired || tracked.uniformState == null) {
			return null;
		}

		ShaderUniformState uniforms = tracked.uniformState;
		return new MaskData(
				uniforms.primaryR(),
				uniforms.primaryG(),
				uniforms.primaryB(),
				uniforms.secondaryR(),
				uniforms.secondaryG(),
				uniforms.secondaryB()
		);
	}

	public static void flushMaskAndApplyUniforms(float partialTicks, PoseStack poseStack, Camera camera, Frustum frustum) {
		if (activeUniformState == null || ACTIVE_MASK_PLAYERS.isEmpty()) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.gameRenderer == null || mc.levelRenderer == null) {
			return;
		}

		PostChain postChain = mc.gameRenderer.currentEffect();
		if (postChain == null || !TRANSFORMATION_EFFECT.toString().equals(postChain.getName())) {
			return;
		}

		RenderTarget maskTarget = postChain.getTempTarget(MASK_TARGET);
		if (maskTarget == null) {
			return;
		}

		maskTarget.clear(Minecraft.ON_OSX);
		maskTarget.copyDepthFrom(mc.getMainRenderTarget());

		TransformationMaskRenderState.setCurrentTarget(maskTarget);
		try {
			maskBufferSource.endMaskBatch();
		} finally {
			TransformationMaskRenderState.setCurrentTarget(null);
			mc.getMainRenderTarget().bindWrite(false);
		}

		applyRuntimeUniforms(postChain);
	}

	public static void reset() {
		clearState(Minecraft.getInstance(), true);
	}

	private static void updateTrackedPlayers(Minecraft mc) {
		ACTIVE_MASK_PLAYERS.clear();
		activeUniformState = null;

		long frameId = mc.level.getGameTime();
		UUID localPlayerId = mc.player.getUUID();
		ShaderUniformState fallbackUniform = null;

		for (Player player : mc.level.players()) {
			ResolvedShaderConfig resolvedConfig = resolveActiveShaderConfig(player);
			UUID playerId = player.getUUID();

			if (resolvedConfig == null) {
				TRACKED_PLAYERS.remove(playerId);
				continue;
			}

			TrackedShaderState tracked = TRACKED_PLAYERS.get(playerId);
			if (tracked == null) {
				tracked = new TrackedShaderState();
				TRACKED_PLAYERS.put(playerId, tracked);
			}

			if (!resolvedConfig.signature().equals(tracked.signature)) {
				tracked.signature = resolvedConfig.signature();
				tracked.remainingTicks = resolvedConfig.uniformState().durationTicks() > 0 ? resolvedConfig.uniformState().durationTicks() : -1;
				tracked.expired = false;
			} else if (!tracked.expired && tracked.remainingTicks > 0) {
				tracked.remainingTicks--;
				if (tracked.remainingTicks <= 0) {
					tracked.expired = true;
				}
			}

			tracked.uniformState = resolvedConfig.uniformState();
			tracked.lastSeenFrame = frameId;

			if (tracked.expired) {
				continue;
			}

			ACTIVE_MASK_PLAYERS.add(playerId);
			if (fallbackUniform == null) {
				fallbackUniform = tracked.uniformState;
			}
			if (playerId.equals(localPlayerId)) {
				activeUniformState = tracked.uniformState;
			}
		}

		TRACKED_PLAYERS.entrySet().removeIf(entry -> entry.getValue().lastSeenFrame != frameId);
		if (activeUniformState == null) {
			activeUniformState = fallbackUniform;
		}
	}

	private static void applyRuntimeUniforms(PostChain postChain) {
		if (activeUniformState == null) {
			return;
		}

		List<PostPass> passes = ((PostChainAccessor) postChain).dragonminez$getPasses();
		for (PostPass pass : passes) {
			EffectInstance effect = pass.getEffect();
			String passName = pass.getName();

			if (UNPACK_PASS_NAME.equals(passName)) {
				applyUniform(effect, "NoiseScale", activeUniformState.noiseScale());
				applyUniform(effect, "NoiseIntensity", activeUniformState.noiseIntensity());
				applyUniform(effect, "NoiseScroll", activeUniformState.noiseScrollX(), activeUniformState.noiseScrollY());
				applyUniform(effect, "ColorMixSpeed", activeUniformState.colorMixSpeed());
				continue;
			}

			if (SOBEL_PASS_NAME.equals(passName)) {
				applyUniform(effect, "OutlineThickness", activeUniformState.outlineThickness());
				applyUniform(effect, "EdgeThreshold", activeUniformState.edgeThreshold());
				applyUniform(effect, "EdgeStrength", activeUniformState.edgeStrength());
				continue;
			}

			if (BLUR_H_PASS_NAME.equals(passName) || BLUR_V_PASS_NAME.equals(passName)) {
				applyUniform(effect, "BloomRadius", activeUniformState.bloomRadius());
				continue;
			}

			if (COMPOSITE_PASS_NAME.equals(passName)) {
				applyUniform(effect, "BloomStrength", activeUniformState.bloomStrength());
				applyUniform(effect, "GlowStrength", activeUniformState.glowStrength());
			}
		}
	}

	private static void ensureShaderLoaded(Minecraft mc) {
		if (mc.gameRenderer == null) {
			return;
		}

		if (isTransformationShaderActive(mc)) {
			loadedByManager = true;
			return;
		}

		mc.gameRenderer.loadEffect(TRANSFORMATION_EFFECT);
		loadedByManager = isTransformationShaderActive(mc);
	}

	private static void shutdownManagedShader(Minecraft mc) {
		if (mc.gameRenderer == null) {
			loadedByManager = false;
			return;
		}

		if ((loadedByManager || isTransformationShaderActive(mc)) && isTransformationShaderActive(mc)) {
			mc.gameRenderer.shutdownEffect();
		}

		loadedByManager = false;
	}

	private static boolean isTransformationShaderActive(Minecraft mc) {
		if (mc.gameRenderer == null) {
			return false;
		}

		PostChain current = mc.gameRenderer.currentEffect();
		return current != null && TRANSFORMATION_EFFECT.toString().equals(current.getName());
	}

	private static void clearState(Minecraft mc, boolean shutdownShader) {
		TRACKED_PLAYERS.clear();
		ACTIVE_MASK_PLAYERS.clear();
		activeUniformState = null;
		maskBufferSource = new TransformationMaskBufferSource();
		TransformationMaskRenderState.setCurrentTarget(null);

		if (shutdownShader) {
			shutdownManagedShader(mc);
		}
	}

	@Nullable
	private static ResolvedShaderConfig resolveActiveShaderConfig(Player player) {
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null) {
			return null;
		}

		com.dragonminez.common.stats.Character character = data.getCharacter();
		if (character == null) {
			return null;
		}

		FormConfig.FormData activeFormData = character.getActiveFormData();
		FormConfig.FormData activeStackFormData = character.getActiveStackFormData();

		FormConfig.FormData.TransformationPostShaderConfig selectedConfig = null;
		String source = "";

		if (activeFormData != null) {
			FormConfig.FormData.TransformationPostShaderConfig formConfig = activeFormData.getTransformationPostShader();
			if (formConfig.isEnabled()) {
				selectedConfig = formConfig;
				source = "form";
			}
		}

		if (activeStackFormData != null) {
			FormConfig.FormData.TransformationPostShaderConfig stackConfig = activeStackFormData.getTransformationPostShader();
			if (stackConfig.isEnabled()) {
				selectedConfig = stackConfig;
				source = "stack";
			}
		}

		if (selectedConfig == null) {
			return null;
		}

		String signature = character.getRaceName()
				+ "|" + safe(character.getActiveFormGroup()) + ":" + safe(character.getActiveForm())
				+ "|" + safe(character.getActiveStackFormGroup()) + ":" + safe(character.getActiveStackForm())
				+ "|" + source;

		return new ResolvedShaderConfig(signature, ShaderUniformState.fromConfig(selectedConfig));
	}

	private static String safe(@Nullable String value) {
		return value != null ? value : "";
	}

	private static void applyUniform(EffectInstance effect, String uniformName, float value) {
		Uniform uniform = effect.getUniform(uniformName);
		if (uniform != null) {
			uniform.set(value);
		}
	}

	private static void applyUniform(EffectInstance effect, String uniformName, float x, float y) {
		Uniform uniform = effect.getUniform(uniformName);
		if (uniform != null) {
			uniform.set(x, y);
		}
	}

	private static void applyUniform(EffectInstance effect, String uniformName, float x, float y, float z) {
		Uniform uniform = effect.getUniform(uniformName);
		if (uniform != null) {
			uniform.set(x, y, z);
		}
	}

	public record MaskData(
			float primaryR,
			float primaryG,
			float primaryB,
			float secondaryR,
			float secondaryG,
			float secondaryB
	) {
	}

	private record ResolvedShaderConfig(String signature, ShaderUniformState uniformState) {
	}

	private static final class TrackedShaderState {
		private String signature = "";
		private int remainingTicks = -1;
		private boolean expired = false;
		private long lastSeenFrame = -1L;
		private ShaderUniformState uniformState;
	}

	private record ShaderUniformState(
			int durationTicks,
			float primaryR,
			float primaryG,
			float primaryB,
			float secondaryR,
			float secondaryG,
			float secondaryB,
			float outlineThickness,
			float edgeThreshold,
			float edgeStrength,
			float glowStrength,
			float bloomStrength,
			float bloomRadius,
			float noiseScale,
			float noiseIntensity,
			float noiseScrollX,
			float noiseScrollY,
			float colorMixSpeed
	) {
		private static ShaderUniformState fromConfig(FormConfig.FormData.TransformationPostShaderConfig config) {
			float[] primary = ColorUtils.hexToRgb(config.getPrimaryColor());
			float[] secondary = ColorUtils.hexToRgb(config.getSecondaryColor());

			return new ShaderUniformState(
					config.getDurationTicks(),
					primary[0],
					primary[1],
					primary[2],
					secondary[0],
					secondary[1],
					secondary[2],
					(float) config.getOutlineThickness(),
					(float) config.getEdgeThreshold(),
					(float) config.getEdgeStrength(),
					(float) config.getGlowStrength(),
					(float) config.getBloomStrength(),
					(float) config.getBloomRadius(),
					(float) config.getNoiseScale(),
					(float) config.getNoiseIntensity(),
					(float) config.getNoiseScrollX(),
					(float) config.getNoiseScrollY(),
					(float) config.getColorMixSpeed()
			);
		}
	}
}
