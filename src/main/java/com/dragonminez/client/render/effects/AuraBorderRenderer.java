package com.dragonminez.client.render.effects;

import com.dragonminez.client.render.shader.BloomPipeline;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.shader.TransformationMaskBufferSource;
import com.dragonminez.client.render.shader.TransformationMaskRenderState;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.AuraNoiseTexture;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.stats.StatsData;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AuraBorderRenderer {
	private static final int MAX_BORDERS = 254;
	private static final int LEVELS = 3;
	private static final float IGNITE_RATE = 4.0f;
	private static final float FADE_RATE = 2.5f;
	private static final float MAX_MOTION_STEP = 0.1f;

	private static final float THICKNESS_PER_HEIGHT = 0.11f;
	private static final float THICKNESS_MIN_BLOCKS = 0.12f;
	private static final float THICKNESS_MAX_BLOCKS = 0.60f;
	private static final float THICKNESS_MIN_PIXELS = 1.5f;
	private static final float THICKNESS_MAX_PIXELS = 96.0f;
	private static final float FAR_FADE_PIXELS = 4.0f;
	private static final float FIELD_BLUR = 1.25f;
	private static final float REACH = 5.6f;
	private static final float BOUNDS_PADDING = 0.75f;
	private static final float OCCLUSION_MARGIN = 0.6f;
	private static final float BLOOM_INTENSITY = 0.9f;

	private static final TransformationMaskBufferSource SOURCE = new TransformationMaskBufferSource();
	private static final Map<Integer, State> STATES = new HashMap<>();
	private static final List<Request> REQUESTS = new ArrayList<>();
	private static final Set<Integer> REQUESTED = new HashSet<>();

	private static TextureTarget mask;
	private static final TextureTarget[] fields = new TextureTarget[LEVELS];
	private static final TextureTarget[] scratch = new TextureTarget[LEVELS];
	private static boolean capturing;

	private static final class State {
		float intensity;
		long lastNanos;
		float[] color = {1.0f, 1.0f, 1.0f};
	}

	private record Request(Entity entity, int index, State state, float partialTick) {}

	private AuraBorderRenderer() {}

	public static boolean isAvailable() {
		return DMZShaders.auraBorderShader != null && DMZShaders.auraBorderFieldShader != null
				&& DMZShaders.bloomBlurShader != null && ModRenderTypes.hasTransformationMaskShader();
	}

	public static float[] playerColor(Player player, StatsData stats) {
		if (stats == null || player.isSpectator() || player.isInvisible()) return null;
		if (!stats.getStatus().isSurgeActive()) return null;
		return ColorUtils.hexToRgb(stats.getCharacter().getActiveAuraColor());
	}

	public static void beginCapture() {
		capturing = true;
	}

	public static MultiBufferSource begin(Entity entity, MultiBufferSource source, float[] color, float partialTick) {
		if (!capturing || !isAvailable() || IrisCompat.isRenderingShadowPass()) return source;
		Minecraft mc = Minecraft.getInstance();
		if (entity == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) return source;
		int id = entity.getId();
		if (REQUESTED.contains(id) || REQUESTS.size() >= MAX_BORDERS) return source;

		State state = STATES.get(id);
		if (state == null) {
			if (color == null) return source;
			state = new State();
			STATES.put(id, state);
		}
		advance(state, color != null);
		if (color != null) state.color = color.clone();
		if (state.intensity <= 0.001f) {
			if (color == null) STATES.remove(id);
			return source;
		}

		int index = REQUESTS.size() + 1;
		REQUESTS.add(new Request(entity, index, state, partialTick));
		REQUESTED.add(id);
		SOURCE.wrap(source);
		SOURCE.setForceCaptureAll(true);
		SOURCE.setRawColor(index, 0, 0);
		return SOURCE;
	}

	public static void end(MultiBufferSource wrapped) {
		if (wrapped == SOURCE) SOURCE.detach();
	}

	private static void advance(State state, boolean active) {
		long now = System.nanoTime();
		float dt = state.lastNanos == 0L ? 1.0f / 60.0f : Math.min(MAX_MOTION_STEP, (now - state.lastNanos) / 1.0e9f);
		state.lastNanos = now;
		float rate = active ? IGNITE_RATE : -FADE_RATE;
		state.intensity = Mth.clamp(state.intensity + rate * dt, 0.0f, 1.0f);
	}

	public static void process(Minecraft mc, PoseStack poseStack, Matrix4f projection) {
		capturing = false;
		if (REQUESTS.isEmpty()) {
			STATES.clear();
			return;
		}

		RenderTarget main = mc.getMainRenderTarget();
		int bound = GlStateManager.getBoundFramebuffer();
		try {
			ensureTargets(main);
			captureMask(main);
			bindScene(bound, main);

			Matrix4f view = viewMatrix(mc, poseStack);
			Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
			REQUESTS.sort(Comparator.comparingDouble((Request request) -> request.entity().distanceToSqr(camera)).reversed());
			for (Request request : REQUESTS) {
				Placement placement = place(mc, request, view, projection, main);
				if (placement != null) draw(request, placement, main, bound);
			}
		} finally {
			STATES.keySet().retainAll(REQUESTED);
			REQUESTS.clear();
			REQUESTED.clear();
			RenderSystem.disableScissor();
			bindScene(bound, main);
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
		}
	}

	private static void captureMask(RenderTarget main) {
		RenderSystem.depthMask(true);
		RenderSystem.colorMask(true, true, true, true);
		mask.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		mask.clear(Minecraft.ON_OSX);
		mask.copyDepthFrom(main);

		PoseStack modelView = RenderSystem.getModelViewStack();
		modelView.pushPose();
		modelView.setIdentity();
		RenderSystem.applyModelViewMatrix();
		TransformationMaskRenderState.setCurrentTargets(mask);
		try {
			SOURCE.endMaskBatch();
		} finally {
			TransformationMaskRenderState.setCurrentTargets(null);
			modelView.popPose();
			RenderSystem.applyModelViewMatrix();
		}
	}

	private static void bindScene(int framebuffer, RenderTarget main) {
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
		GlStateManager._viewport(0, 0, main.viewWidth, main.viewHeight);
	}

	private static Matrix4f viewMatrix(Minecraft mc, PoseStack poseStack) {
		if (!IrisCompat.isShaderPackInUse()) return new Matrix4f(poseStack.last().pose());
		Camera camera = mc.gameRenderer.getMainCamera();
		return new Matrix4f()
				.rotateX(camera.getXRot() * Mth.DEG_TO_RAD)
				.rotateY((camera.getYRot() + 180.0f) * Mth.DEG_TO_RAD);
	}

	private record Placement(float centerX, float centerY, float upX, float upY, float thickness, float alpha,
							 int level, int[] rect, float occlusionDepth, float depthScale, float depthOffset) {}

	private static Placement place(Minecraft mc, Request request, Matrix4f view, Matrix4f projection, RenderTarget main) {
		Entity entity = request.entity();
		float partialTick = request.partialTick();
		Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
		float height = entity.getBbHeight();
		float radius = Math.max(height, entity.getBbWidth()) * 0.5f + BOUNDS_PADDING;

		float x = (float) (Mth.lerp(partialTick, entity.xo, entity.getX()) - camera.x);
		float y = (float) (Mth.lerp(partialTick, entity.yo, entity.getY()) - camera.y) + height * 0.5f;
		float z = (float) (Mth.lerp(partialTick, entity.zo, entity.getZ()) - camera.z);

		Vector4f centre = new Vector4f(x, y, z, 1.0f).mul(view);
		float depth = -centre.z;
		if (depth < -radius) return null;

		int width = main.width;
		int heightPx = main.height;
		boolean surrounds = depth < radius + 0.5f;
		float scaleDepth = Math.max(depth, 0.5f);
		float pixelsPerBlock = Math.abs(projection.m11()) * heightPx * 0.5f / scaleDepth;

		float centerX = width * 0.5f;
		float centerY = heightPx * 0.5f;
		float upX = 0.0f;
		float upY = 1.0f;
		if (depth > 0.1f) {
			Vector4f top = new Vector4f(x, y + 0.5f, z, 1.0f).mul(view).mul(projection);
			Vector4f clip = new Vector4f(centre).mul(projection);
			if (clip.w > 1.0e-4f && top.w > 1.0e-4f) {
				centerX = (clip.x / clip.w * 0.5f + 0.5f) * width;
				centerY = (clip.y / clip.w * 0.5f + 0.5f) * heightPx;
				float dx = (top.x / top.w * 0.5f + 0.5f) * width - centerX;
				float dy = (top.y / top.w * 0.5f + 0.5f) * heightPx - centerY;
				float length = Mth.sqrt(dx * dx + dy * dy);
				if (length > 1.0e-3f) {
					upX = dx / length;
					upY = dy / length;
				}
			}
		}

		float intensity = request.state().intensity;
		float eased = intensity * intensity * (3.0f - 2.0f * intensity);
		float blocks = Mth.clamp(height * THICKNESS_PER_HEIGHT, THICKNESS_MIN_BLOCKS, THICKNESS_MAX_BLOCKS);
		float full = Math.min(blocks * pixelsPerBlock, THICKNESS_MAX_PIXELS);
		if (full < THICKNESS_MIN_PIXELS) return null;
		float alpha = eased * Mth.clamp((full - THICKNESS_MIN_PIXELS) / (FAR_FADE_PIXELS - THICKNESS_MIN_PIXELS), 0.0f, 1.0f);
		float thickness = Math.max(THICKNESS_MIN_PIXELS, full * (0.35f + 0.65f * eased));

		float blur = thickness * FIELD_BLUR;
		int level = blur <= 24.0f ? 1 : blur <= 48.0f ? 2 : 3;

		int[] rect;
		if (surrounds) {
			rect = new int[]{0, 0, width, heightPx};
		} else {
			float extent = radius * pixelsPerBlock + thickness * REACH;
			int x0 = Mth.clamp(Mth.floor(centerX - extent), 0, width);
			int y0 = Mth.clamp(Mth.floor(centerY - extent), 0, heightPx);
			int x1 = Mth.clamp(Mth.ceil(centerX + extent), 0, width);
			int y1 = Mth.clamp(Mth.ceil(centerY + extent), 0, heightPx);
			if (x1 <= x0 || y1 <= y0) return null;
			rect = new int[]{x0, y0, x1, y1};
		}

		float occlusionDepth = Math.max(0.0f, depth - radius - OCCLUSION_MARGIN);
		return new Placement(centerX, centerY, upX, upY, thickness, alpha, level, rect, occlusionDepth, projection.m22(), projection.m32());
	}

	private static void draw(Request request, Placement placement, RenderTarget main, int sceneFramebuffer) {
		ShaderInstance fieldShader = DMZShaders.auraBorderFieldShader;
		ShaderInstance blurShader = DMZShaders.bloomBlurShader;
		ShaderInstance flameShader = DMZShaders.auraBorderShader;
		if (fieldShader == null || blurShader == null || flameShader == null) return;

		VertexBuffer quad = AuraMeshFactory.getFullscreenQuad();
		float id = request.index() / 255.0f;
		int levelIndex = placement.level() - 1;
		int shift = placement.level();
		float footprint = 1 << shift;
		TextureTarget field = fields[levelIndex];
		TextureTarget temp = scratch[levelIndex];
		float radius = Mth.clamp(placement.thickness() * FIELD_BLUR / footprint, 1.0f, 16.0f);
		int pad = Mth.ceil(radius) + 1;

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.disableBlend();

		field.bindWrite(true);
		scissor(placement.rect(), shift, pad * 2, field);
		fieldShader.setSampler("Mask", mask.getColorTextureId());
		fieldShader.safeGetUniform("MaskTexel").set(1.0f / mask.width, 1.0f / mask.height);
		fieldShader.safeGetUniform("Footprint").set(footprint);
		fieldShader.safeGetUniform("Id").set(id);
		drawQuad(fieldShader, quad);

		temp.bindWrite(true);
		scissor(placement.rect(), shift, pad, temp);
		blur(blurShader, quad, field, 1.0f, 0.0f, radius);

		field.bindWrite(true);
		scissor(placement.rect(), shift, 0, field);
		blur(blurShader, quad, temp, 0.0f, 1.0f, radius);

		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, sceneFramebuffer);
		GlStateManager._viewport(0, 0, main.viewWidth, main.viewHeight);
		int[] rect = placement.rect();
		RenderSystem.enableScissor(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1]);

		State state = request.state();
		float time = (request.entity().tickCount + request.partialTick()) / 20.0f;
		float seed = (request.entity().getId() * 0.6180339f) % 1.0f;
		boolean depthReadable = main.getDepthTextureId() > 0;

		flameShader.setSampler("Mask", mask.getColorTextureId());
		flameShader.setSampler("Field", field.getColorTextureId());
		flameShader.setSampler("NoiseTex", AuraNoiseTexture.getId());
		flameShader.setSampler("SceneDepth", depthReadable ? main.getDepthTextureId() : AuraNoiseTexture.getId());
		flameShader.safeGetUniform("ScreenSize").set((float) main.width, (float) main.height);
		flameShader.safeGetUniform("Center").set(placement.centerX(), placement.centerY());
		flameShader.safeGetUniform("Up").set(placement.upX(), placement.upY());
		flameShader.safeGetUniform("Thickness").set(placement.thickness());
		flameShader.safeGetUniform("Id").set(id);
		flameShader.safeGetUniform("Time").set(time);
		flameShader.safeGetUniform("Seed").set(seed);
		flameShader.safeGetUniform("Alpha").set(placement.alpha());
		flameShader.safeGetUniform("Color").set(state.color[0], state.color[1], state.color[2]);
		flameShader.safeGetUniform("DepthParams").set(placement.depthScale(), placement.depthOffset(), depthReadable ? placement.occlusionDepth() : 0.0f);
		flameShader.safeGetUniform("BloomIntensity").set(BLOOM_INTENSITY);

		drawFlame(flameShader, quad, 0.0f);
		if (BloomPipeline.beginRedraw(main)) {
			try {
				drawFlame(flameShader, quad, 1.0f);
			} finally {
				BloomPipeline.endRedraw();
			}
		}
		RenderSystem.disableScissor();
	}

	private static void drawFlame(ShaderInstance shader, VertexBuffer quad, float bloomPass) {
		shader.safeGetUniform("BloomPass").set(bloomPass);
		shader.apply();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		quad.bind();
		quad.draw();
		VertexBuffer.unbind();
		shader.clear();
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
	}

	private static void blur(ShaderInstance shader, VertexBuffer quad, RenderTarget input, float dirX, float dirY, float radius) {
		shader.setSampler("Source", input);
		shader.safeGetUniform("Texel").set(1.0f / input.width, 1.0f / input.height);
		shader.safeGetUniform("Direction").set(dirX, dirY);
		shader.safeGetUniform("Radius").set(radius);
		drawQuad(shader, quad);
	}

	private static void drawQuad(ShaderInstance shader, VertexBuffer quad) {
		shader.apply();
		quad.bind();
		quad.draw();
		VertexBuffer.unbind();
		shader.clear();
	}

	private static void scissor(int[] rect, int shift, int pad, RenderTarget target) {
		int x0 = Mth.clamp((rect[0] >> shift) - pad, 0, target.width);
		int y0 = Mth.clamp((rect[1] >> shift) - pad, 0, target.height);
		int x1 = Mth.clamp(((rect[2] + (1 << shift) - 1) >> shift) + pad, 0, target.width);
		int y1 = Mth.clamp(((rect[3] + (1 << shift) - 1) >> shift) + pad, 0, target.height);
		RenderSystem.enableScissor(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
	}

	private static void ensureTargets(RenderTarget main) {
		if (mask == null || mask.width != main.width || mask.height != main.height) {
			if (mask == null) mask = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
			else mask.resize(main.width, main.height, Minecraft.ON_OSX);
			mask.setFilterMode(GL11.GL_NEAREST);
			for (int i = 0; i < LEVELS; i++) {
				int w = Math.max(1, main.width >> (i + 1));
				int h = Math.max(1, main.height >> (i + 1));
				fields[i] = resize(fields[i], w, h);
				scratch[i] = resize(scratch[i], w, h);
			}
		}
		if (main.isStencilEnabled() && !mask.isStencilEnabled()) mask.enableStencil();
	}

	private static TextureTarget resize(TextureTarget target, int width, int height) {
		if (target == null) target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
		else target.resize(width, height, Minecraft.ON_OSX);
		target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		target.clear(Minecraft.ON_OSX);
		target.setFilterMode(GL11.GL_LINEAR);
		return target;
	}
}
