package com.dragonminez.client.render.shader;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class BloomPipeline {
	private static final int LEVELS = 3;
	private static final float[] LEVEL_RADIUS = {6.0f, 8.0f, 10.0f};
	private static final float INTENSITY = 1.0f;
	private static final float SPREAD = 0.6f;
	private static final float[] TRANSPARENT = {0.0f, 0.0f, 0.0f, 0.0f};
	private static final int[] REDRAW_BUFFERS = {GL30.GL_COLOR_ATTACHMENT0};
	private static final int MAX_FAILURES = 3;

	private static TextureTarget mask;
	private static final TextureTarget[] levels = new TextureTarget[LEVELS];
	private static final TextureTarget[] scratch = new TextureTarget[LEVELS];

	private static final Attachment redraw = new Attachment();
	private static boolean redrawUnsupported;
	private static int redrawFailures;

	private static boolean maskCleared;
	private static boolean pending;
	private static int restoreFramebuffer;

	private BloomPipeline() {}

	public static void resetFrame() {
		pending = false;
		maskCleared = false;
	}

	public static boolean hasPendingBloom() {
		return pending;
	}

	public static boolean beginRedraw(RenderTarget main) {
		if (!shadersReady() || redrawUnsupported) return false;

		boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
		if (scissor) GL11.glDisable(GL11.GL_SCISSOR_TEST);
		try {
			return bindRedraw(main);
		} finally {
			if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
		}
	}

	private static boolean bindRedraw(RenderTarget main) {
		int bound = GlStateManager.getBoundFramebuffer();
		ensureTargets(main, bound);
		int maskTexture = mask.getColorTextureId();
		if (!bindComplete(redraw, maskTexture, -1, main.getDepthTextureId(), main.isStencilEnabled(), REDRAW_BUFFERS)) {
			if (++redrawFailures >= MAX_FAILURES) {
				redrawUnsupported = true;
				LogUtil.warn(Env.CLIENT, "Bloom redraw framebuffer is not supported by this driver; effects draw without bloom");
			}
			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, bound);
			return false;
		}
		redrawFailures = 0;

		restoreFramebuffer = bound;
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, redraw.framebuffer);
		clearMaskOnce(0);
		pending = true;
		return true;
	}

	public static void endRedraw() {
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, restoreFramebuffer);
	}

	public static int bindMainIfFabulousLayer() {
		if (!Minecraft.useShaderTransparency()) return -1;
		Minecraft mc = Minecraft.getInstance();
		RenderTarget main = mc.getMainRenderTarget();
		int bound = GlStateManager.getBoundFramebuffer();
		if (bound == main.frameBufferId || sceneTarget(mc, main, bound) == null) return -1;
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, main.frameBufferId);
		return bound;
	}

	public static void restoreFramebuffer(int framebuffer) {
		if (framebuffer >= 0) GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
	}

	public static void downsampleBlur(RenderTarget source, TextureTarget out, TextureTarget scratch, float radius) {
		ShaderInstance down = DMZShaders.bloomDownShader;
		ShaderInstance blur = DMZShaders.bloomBlurShader;
		if (down == null || blur == null) return;
		VertexBuffer quad = AuraMeshFactory.getFullscreenQuad();

		out.bindWrite(true);
		down.setSampler("Source", source);
		down.safeGetUniform("SourceTexel").set(1.0f / source.width, 1.0f / source.height);
		drawQuad(down, quad);

		blurInto(blur, quad, out, scratch, 1.0f, 0.0f, radius);
		blurInto(blur, quad, scratch, out, 0.0f, 1.0f, radius);
	}

	public static void resolve(RenderTarget main) {
		if (!pending) return;
		pending = false;

		ShaderInstance composite = DMZShaders.bloomCompositeShader;
		if (!shadersReady() || mask == null) return;
		if (mask.width != main.width || mask.height != main.height) return;

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.disableBlend();
		VertexBuffer quad = AuraMeshFactory.getFullscreenQuad();

		RenderTarget source = mask;
		for (int i = 0; i < LEVELS; i++) {
			downsampleBlur(source, levels[i], scratch[i], LEVEL_RADIUS[i]);
			source = levels[i];
		}

		main.bindWrite(true);
		composite.setSampler("Bloom0", levels[0]);
		composite.setSampler("Bloom1", levels[1]);
		composite.setSampler("Bloom2", levels[2]);
		composite.safeGetUniform("Intensity").set(INTENSITY);
		composite.safeGetUniform("Spread").set(SPREAD);
		composite.apply();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		drawQuad(composite, quad);

		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		main.bindWrite(false);
	}

	public static void reset() {
		if (mask != null) mask.destroyBuffers();
		mask = null;
		for (int i = 0; i < LEVELS; i++) {
			if (levels[i] != null) levels[i].destroyBuffers();
			if (scratch[i] != null) scratch[i].destroyBuffers();
			levels[i] = null;
			scratch[i] = null;
		}
		redraw.release();
		pending = false;
		maskCleared = false;
		redrawFailures = 0;
	}

	private static boolean shadersReady() {
		return DMZShaders.bloomDownShader != null && DMZShaders.bloomBlurShader != null && DMZShaders.bloomCompositeShader != null;
	}

	private static void clearMaskOnce(int drawBuffer) {
		if (maskCleared) return;
		RenderSystem.colorMask(true, true, true, true);
		GL30.glClearBufferfv(GL11.GL_COLOR, drawBuffer, TRANSPARENT);
		maskCleared = true;
	}

	private static void blurInto(ShaderInstance blur, VertexBuffer quad, RenderTarget input, RenderTarget output,
								 float dirX, float dirY, float radius) {
		output.bindWrite(true);
		blur.setSampler("Source", input);
		blur.safeGetUniform("Texel").set(1.0f / input.width, 1.0f / input.height);
		blur.safeGetUniform("Direction").set(dirX, dirY);
		blur.safeGetUniform("Radius").set(radius);
		drawQuad(blur, quad);
	}

	private static void drawQuad(ShaderInstance shader, VertexBuffer quad) {
		shader.apply();
		quad.bind();
		quad.draw();
		VertexBuffer.unbind();
		shader.clear();
	}

	private static RenderTarget sceneTarget(Minecraft mc, RenderTarget main, int bound) {
		if (bound == main.frameBufferId) return main;
		LevelRenderer level = mc.levelRenderer;
		if (level == null) return null;
		if (matches(level.getWeatherTarget(), bound, main)) return level.getWeatherTarget();
		if (matches(level.getParticlesTarget(), bound, main)) return level.getParticlesTarget();
		if (matches(level.getTranslucentTarget(), bound, main)) return level.getTranslucentTarget();
		if (matches(level.getItemEntityTarget(), bound, main)) return level.getItemEntityTarget();
		if (matches(level.getCloudsTarget(), bound, main)) return level.getCloudsTarget();
		return null;
	}

	private static boolean matches(RenderTarget target, int bound, RenderTarget main) {
		return target != null && target.frameBufferId == bound && target.width == main.width && target.height == main.height;
	}

	private static void ensureTargets(RenderTarget main, int bound) {
		if (mask != null && mask.width == main.width && mask.height == main.height) return;

		mask = resize(mask, main.width, main.height);
		for (int i = 0; i < LEVELS; i++) {
			int w = Math.max(1, main.width >> (i + 1));
			int h = Math.max(1, main.height >> (i + 1));
			levels[i] = resize(levels[i], w, h);
			scratch[i] = resize(scratch[i], w, h);
		}
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, bound);
		GlStateManager._viewport(0, 0, main.viewWidth, main.viewHeight);
	}

	private static TextureTarget resize(TextureTarget target, int width, int height) {
		if (target == null) target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
		else target.resize(width, height, Minecraft.ON_OSX);
		target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		target.clear(Minecraft.ON_OSX);
		target.setFilterMode(GL11.GL_LINEAR);
		return target;
	}

	private static boolean bindComplete(Attachment target, int color0, int color1, int depth, boolean stencil, int[] drawBuffers) {
		if (target.matches(color0, color1, depth, stencil)) {
			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebuffer);
			if (GlStateManager.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE) return true;
			target.color0 = -1;
		}
		return attach(target, color0, color1, depth, stencil, drawBuffers);
	}

	private static boolean attach(Attachment target, int color0, int color1, int depth, boolean stencil, int[] drawBuffers) {
		if (target.framebuffer < 0) target.framebuffer = GlStateManager.glGenFramebuffers();
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebuffer);
		GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, color0, 0);
		GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL11.GL_TEXTURE_2D, Math.max(color1, 0), 0);
		GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, 0, 0);
		if (depth > 0) {
			int point = stencil ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : GL30.GL_DEPTH_ATTACHMENT;
			GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, point, GL11.GL_TEXTURE_2D, depth, 0);
		}
		GL20.glDrawBuffers(drawBuffers);

		if (GlStateManager.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
			target.color0 = -1;
			return false;
		}
		target.color0 = color0;
		target.color1 = color1;
		target.depth = depth;
		target.stencil = stencil;
		return true;
	}

	private static final class Attachment {
		int framebuffer = -1;
		int color0 = -1;
		int color1 = -1;
		int depth = -1;
		boolean stencil;

		boolean matches(int color0, int color1, int depth, boolean stencil) {
			return framebuffer >= 0 && this.color0 == color0 && this.color1 == color1 && this.depth == depth && this.stencil == stencil;
		}

		void release() {
			if (framebuffer >= 0) GlStateManager._glDeleteFramebuffers(framebuffer);
			framebuffer = -1;
			color0 = -1;
			color1 = -1;
			depth = -1;
		}
	}
}
