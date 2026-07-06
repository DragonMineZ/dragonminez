package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
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
	private static final String BLUR_H_PASS_NAME = Reference.MOD_ID + ":transformation_blur_h";
	private static final String BLUR_V_PASS_NAME = Reference.MOD_ID + ":transformation_blur_v";
	private static final String COMPOSITE_PASS_NAME = Reference.MOD_ID + ":transformation_composite";
	private static final String MASK_TARGET = "entity_mask";

	private static final float FIXED_GLOW_STRENGTH = 1.35f;
	private static final float FIXED_BLOOM_STRENGTH = 0.95f;

	private static final Map<UUID, TrackedShaderState> TRACKED_PLAYERS = new HashMap<>();
	private static final Set<UUID> ACTIVE_MASK_PLAYERS = new HashSet<>();

	private static boolean loadedByManager = false;
	@Nullable
	private static ShaderUniformState activeUniformState;
	private static TransformationMaskBufferSource maskBufferSource = new TransformationMaskBufferSource();

	@Nullable
	private static PostChain shaderpackChain;
	private static int shaderpackChainWidth = -1;
	private static int shaderpackChainHeight = -1;
	private static volatile boolean shaderpackMainPass = false;

	public static void setShaderpackMainPass(boolean active) {
		shaderpackMainPass = active;
	}

	public static boolean isShaderpackMainPass() {
		return shaderpackMainPass;
	}

	private TransformationPostShaderManager() {}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			clearState(mc, true);
			return;
		}

		updateTrackedPlayers(mc);

		if (IrisCompat.isShaderPackInUse()) {
			shutdownManagedShader(mc);
			return;
		}

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
		if (mc.player == null || mc.level == null || player == null) return null;
		UUID playerId = player.getUUID();
		if (!ACTIVE_MASK_PLAYERS.contains(playerId)) return null;

		if (!ModRenderTypes.hasTransformationMaskShader()) return null;
		if (!IrisCompat.isShaderPackInUse() && !isTransformationShaderActive(mc)) return null;
		if (player == mc.player && mc.options.getCameraType().isFirstPerson()) return null;
		TrackedShaderState tracked = TRACKED_PLAYERS.get(playerId);
		if (tracked == null || tracked.uniformState == null) return null;

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
		if (activeUniformState == null || ACTIVE_MASK_PLAYERS.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.gameRenderer == null || mc.levelRenderer == null) return;

		PostChain postChain = mc.gameRenderer.currentEffect();
		if (postChain == null || !TRANSFORMATION_EFFECT.toString().equals(postChain.getName())) return;

		RenderTarget maskTarget = postChain.getTempTarget(MASK_TARGET);
		if (maskTarget == null) return;

		maskTarget.clear(Minecraft.ON_OSX);
		maskTarget.copyDepthFrom(mc.getMainRenderTarget());

		TransformationMaskRenderState.setCurrentTargets(maskTarget);
		try {
			maskBufferSource.endMaskBatch();
		} finally {
			TransformationMaskRenderState.setCurrentTargets(null);
			mc.getMainRenderTarget().bindWrite(false);
		}

		applyRuntimeUniforms(postChain, partialTicks, mc);
	}

	public static void processShaderpackOutline(float partialTicks) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.gameRenderer == null) return;
		if (ACTIVE_MASK_PLAYERS.isEmpty() || activeUniformState == null || !ModRenderTypes.hasTransformationMaskShader()) return;

		RenderTarget main = mc.getMainRenderTarget();
		PostChain chain = ensureShaderpackChain(mc, main);
		if (chain == null) return;

		RenderTarget maskTarget = chain.getTempTarget(MASK_TARGET);
		if (maskTarget == null) return;

		maskTarget.clear(Minecraft.ON_OSX);
		maskTarget.copyDepthFrom(main);

		TransformationMaskRenderState.setCurrentTargets(maskTarget);
		try {
			maskBufferSource.endMaskBatch();
		} finally {
			TransformationMaskRenderState.setCurrentTargets(null);
			main.bindWrite(false);
		}

		applyRuntimeUniforms(chain, partialTicks, mc);
		chain.process(partialTicks);
		main.bindWrite(false);
	}

	@Nullable
	private static PostChain ensureShaderpackChain(Minecraft mc, RenderTarget main) {
		if (shaderpackChain != null && shaderpackChainWidth == main.width && shaderpackChainHeight == main.height) {
			return shaderpackChain;
		}
		try {
			if (shaderpackChain != null) shaderpackChain.close();
			shaderpackChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, TRANSFORMATION_EFFECT);
			shaderpackChain.resize(main.width, main.height);
			shaderpackChainWidth = main.width;
			shaderpackChainHeight = main.height;
		} catch (Exception e) {
			shaderpackChain = null;
			shaderpackChainWidth = -1;
			shaderpackChainHeight = -1;
		}
		return shaderpackChain;
	}

	public static void reset() {
		clearState(Minecraft.getInstance(), true);
	}

	private static void updateTrackedPlayers(Minecraft mc) {
		ACTIVE_MASK_PLAYERS.clear();
		activeUniformState = null;

		long frameId = mc.level.getGameTime();
		UUID localPlayerId = mc.player.getUUID();
		boolean localFirstPerson = mc.options.getCameraType().isFirstPerson();
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

			if (!resolvedConfig.signature().equals(tracked.signature)) tracked.signature = resolvedConfig.signature();

			tracked.uniformState = resolvedConfig.uniformState();
			tracked.lastSeenFrame = frameId;

			if (playerId.equals(localPlayerId) && localFirstPerson) continue;

			ACTIVE_MASK_PLAYERS.add(playerId);
			if (fallbackUniform == null) fallbackUniform = tracked.uniformState;
			if (playerId.equals(localPlayerId)) activeUniformState = tracked.uniformState;
		}

		TRACKED_PLAYERS.entrySet().removeIf(entry -> entry.getValue().lastSeenFrame != frameId);
		if (activeUniformState == null) activeUniformState = fallbackUniform;
	}

	private static void applyRuntimeUniforms(PostChain postChain, float partialTicks, Minecraft mc) {
		if (activeUniformState == null) return;
		float animationTime = mc.level != null ? ((float) mc.level.getGameTime() + partialTicks) / 20.0f : partialTicks / 20.0f;

		List<PostPass> passes = ((PostChainAccessor) postChain).dragonminez$getPasses();
		for (PostPass pass : passes) {
			EffectInstance effect = pass.getEffect();
			String passName = pass.getName();

			if (UNPACK_PASS_NAME.equals(passName)) {
				applyUniform(effect, "AnimationTime", animationTime);
				continue;
			}

			if (BLUR_H_PASS_NAME.equals(passName) || BLUR_V_PASS_NAME.equals(passName)) {
				applyUniform(effect, "BloomRadius", outlineThicknessToBlurRadius(activeUniformState.outlineThickness()));
				continue;
			}

			if (COMPOSITE_PASS_NAME.equals(passName)) {
				applyUniform(effect, "BloomStrength", FIXED_BLOOM_STRENGTH);
				applyUniform(effect, "GlowStrength", FIXED_GLOW_STRENGTH);
			}
		}
	}

	private static void ensureShaderLoaded(Minecraft mc) {
		if (mc.gameRenderer == null) return;

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

		if ((loadedByManager || isTransformationShaderActive(mc)) && isTransformationShaderActive(mc)) mc.gameRenderer.shutdownEffect();
		loadedByManager = false;
	}

	private static boolean isTransformationShaderActive(Minecraft mc) {
		if (mc.gameRenderer == null) return false;

		PostChain current = mc.gameRenderer.currentEffect();
		return current != null && TRANSFORMATION_EFFECT.toString().equals(current.getName());
	}

	private static void clearState(Minecraft mc, boolean shutdownShader) {
		TRACKED_PLAYERS.clear();
		ACTIVE_MASK_PLAYERS.clear();
		activeUniformState = null;
		maskBufferSource = new TransformationMaskBufferSource();
		TransformationMaskRenderState.setCurrentTargets(null);

		if (shaderpackChain != null) {
			shaderpackChain.close();
			shaderpackChain = null;
			shaderpackChainWidth = -1;
			shaderpackChainHeight = -1;
		}

		if (shutdownShader) shutdownManagedShader(mc);
	}

	@Nullable
	private static ResolvedShaderConfig resolveActiveShaderConfig(Player player) {
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null) return null;
		Character character = data.getCharacter();
		if (character == null) return null;

		FormConfig.FormData activeFormData = character.getActiveFormData();
		FormConfig.FormData activeStackFormData = character.getActiveStackFormData();

		FormConfig.FormData.OutlineShaderConfig selectedConfig = null;
		String source = "";

		if (activeFormData != null) {
			FormConfig.FormData.OutlineShaderConfig formConfig = activeFormData.getOutlineShader();
			if (formConfig.isEnabled()) {
				selectedConfig = formConfig;
				source = "form";
			}
		}

		if (activeStackFormData != null) {
			FormConfig.FormData.OutlineShaderConfig stackConfig = activeStackFormData.getOutlineShader();
			if (stackConfig.isEnabled()) {
				selectedConfig = stackConfig;
				source = "stack";
			}
		}

		if (selectedConfig == null) return null;

		String signature = character.getRaceName()
				+ "|" + safe(character.getActiveFormGroup()) + ":" + safe(character.getActiveForm())
				+ "|" + safe(character.getActiveStackFormGroup()) + ":" + safe(character.getActiveStackForm())
				+ "|" + source;

		return new ResolvedShaderConfig(signature, ShaderUniformState.fromConfig(selectedConfig));
	}

	private static String safe(@Nullable String value) {
		return value != null ? value : "";
	}

	private static float outlineThicknessToBlurRadius(float outlineThickness) {
		return Math.max(2.0f, Math.min(14.0f, outlineThickness * 3.0f));
	}

	private static void applyUniform(EffectInstance effect, String uniformName, float value) {
		Uniform uniform = effect.getUniform(uniformName);
		if (uniform != null) uniform.set(value);
	}

	public record MaskData(float primaryR, float primaryG, float primaryB, float secondaryR, float secondaryG, float secondaryB) {}

	private record ResolvedShaderConfig(String signature, ShaderUniformState uniformState) {}

	private static final class TrackedShaderState {
		private String signature = "";
		private long lastSeenFrame = -1L;
		private ShaderUniformState uniformState;
	}

	private record ShaderUniformState(float primaryR, float primaryG, float primaryB, float secondaryR, float secondaryG, float secondaryB, float outlineThickness) {
		private static ShaderUniformState fromConfig(FormConfig.FormData.OutlineShaderConfig config) {
			float[] primary = ColorUtils.hexToRgb(config.getPrimaryColor());
			float[] secondary = ColorUtils.hexToRgb(config.getSecondaryColor());

			return new ShaderUniformState(
					primary[0],
					primary[1],
					primary[2],
					secondary[0],
					secondary[1],
					secondary[2],
					(float) config.getOutlineThickness()
			);
		}
	}
}
