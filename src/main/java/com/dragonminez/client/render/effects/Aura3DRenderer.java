package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.BloomPipeline;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.AuraNoiseTexture;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.FormConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Aura3DRenderer {

	public static final String DEFAULT_TYPE = "smooth";
	private static final String SPARKING_TYPE = "sparking";
	public static final float DEFAULT_BACKFACE = 0.02f;

	public static final float SMOOTH_SCALE_REFERENCE = 1.05f * 0.9375f;
	public static final float SMOOTH_CENTER = 1.15f;
	private static final float SMOOTH_RADIUS = 0.80f;
	private static final float SMOOTH_HALF_HEIGHT = 1.35f;
	public static final float SMOOTH_BACKFACE = 0.01f;
	public static final float SMOOTH_BACKFACE_INSIDE = 1.0f;

	private static final float SPARKING_TIME_SCALE = 0.67f;
	private static final float SPARKING_PULSE_TIME_SCALE = 0.50f;
	private static final float SPARKING_WIDTH = 0.936f;
	private static final float SPARKING_HEIGHT = 1.325f;
	private static final float SPARKING_PIVOT = 0.889f;

	private static final ResourceLocation DUMMY_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");
	private static final Map<Integer, AuraStyle> ENTITY_STYLES = new HashMap<>();

	private Aura3DRenderer() {}

	public static boolean isAvailable() {
		return DMZShaders.auraSmooth3DShader != null;
	}

	public static String resolveType(String type) {
		if (type == null) return DEFAULT_TYPE;
		String normalized = type.trim().toLowerCase(Locale.ROOT);
		return SPARKING_TYPE.equals(normalized) ? SPARKING_TYPE : DEFAULT_TYPE;
	}

	public static boolean isSmooth(String type) {
		return DEFAULT_TYPE.equals(resolveType(type));
	}

	public static float widthFactor(String type) {
		return isSmooth(type) ? 0.90f : SPARKING_WIDTH;
	}

	public static float heightFactor(String type) {
		return isSmooth(type) ? 1.80f : SPARKING_HEIGHT;
	}

	public static boolean followsBodyYaw(String type) {
		return isSmooth(type);
	}

	public static float pivotFactor(String type) {
		return isSmooth(type) ? 0.80f : SPARKING_PIVOT;
	}

	public static float coreFactor(String type) {
		return 0.45f;
	}

	public static void drawSmooth(PoseStack poseStack, Matrix4f projection, AuraStyle style, float alpha, float growth,
								  float time, float phase, float scaleX, float scaleY, float scaleZ, float backFace) {
		ShaderInstance shader = DMZShaders.auraSmooth3DShader;
		if (shader == null || style == null || alpha <= 0.001f || growth <= 0.001f) return;

		poseStack.pushPose();
		poseStack.translate(0.0f, SMOOTH_CENTER * scaleY, 0.0f);
		poseStack.scale(SMOOTH_RADIUS * scaleX, SMOOTH_HALF_HEIGHT * scaleY, SMOOTH_RADIUS * scaleZ);
		drawShell(shader, poseStack, projection, style, alpha, growth, time, phase, backFace);
		poseStack.popPose();
	}

	public static void drawSmoothGroundPulse(PoseStack poseStack, Matrix4f projection, AuraStyle style, float alpha,
											 float time, float phase, float radius, float height, float spinDegrees) {
		ShaderInstance shader = DMZShaders.auraSmooth3DShader;
		if (shader == null || style == null || alpha <= 0.001f) return;

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(spinDegrees));
		poseStack.scale(radius, height, radius);
		poseStack.translate(0.0f, 0.9f, 0.0f);
		drawShell(shader, poseStack, projection, style, alpha, 1.0f, time, phase, SMOOTH_BACKFACE);
		poseStack.popPose();
	}

	private static void drawShell(ShaderInstance shader, PoseStack poseStack, Matrix4f projection, AuraStyle style,
								  float alpha, float growth, float time, float phase, float backFace) {
		VertexBuffer mesh = AuraMeshFactory.getDropletMesh();
		Matrix4f pose = poseStack.last().pose();
		Matrix3f normal = poseStack.last().normal();
		applySmoothUniforms(shader, normal, style, alpha, growth, time, phase, backFace);

		boolean captured = BloomPipeline.beginCapture();
		try {
			drawShellMesh(mesh, shader, pose, projection, 0.0f);
		} finally {
			if (captured) BloomPipeline.endCapture();
		}
		if (captured) return;

		Matrix4f poseCopy = new Matrix4f(pose);
		Matrix3f normalCopy = new Matrix3f(normal);
		Matrix4f projectionCopy = new Matrix4f(projection);
		AuraStyle styleCopy = new AuraStyle().copyFrom(style);
		AuraRenderer.captureBloom(() -> {
			ShaderInstance bloomShader = DMZShaders.auraSmooth3DShader;
			if (bloomShader == null) return;
			applySmoothUniforms(bloomShader, normalCopy, styleCopy, alpha, growth, time, phase, backFace);
			drawShellMesh(mesh, bloomShader, poseCopy, projectionCopy, 1.0f);
		});
	}

	private static void applySmoothUniforms(ShaderInstance shader, Matrix3f normal, AuraStyle style, float alpha,
											float growth, float time, float phase, float backFace) {
		shader.setSampler("NoiseTex", AuraNoiseTexture.getId());
		shader.safeGetUniform("NormalMat").set(normal);
		shader.safeGetUniform("Size").set(style.sizeX, style.sizeY, style.sizeZ);
		shader.safeGetUniform("Time").set(time);
		shader.safeGetUniform("Phase").set(phase);
		shader.safeGetUniform("Growth").set(Mth.clamp(growth, 0.0f, 1.0f));
		shader.safeGetUniform("Peaks").set((float) Math.max(1, Math.round(style.peaks)));
		shader.safeGetUniform("WaveFrequency").set(style.waveFrequency);
		shader.safeGetUniform("WaveAmplitude").set(style.waveAmplitude);
		shader.safeGetUniform("NoiseDetail").set(style.noiseDetail);
		shader.safeGetUniform("UpwardBias").set(style.upwardBias);
		shader.safeGetUniform("CoreColor").set(style.coreColor[0], style.coreColor[1], style.coreColor[2]);
		shader.safeGetUniform("RimColor").set(style.rimColor[0], style.rimColor[1], style.rimColor[2]);
		shader.safeGetUniform("NoiseColor").set(style.noiseColor[0], style.noiseColor[1], style.noiseColor[2]);
		shader.safeGetUniform("NoiseFactor").set(style.noiseFactor);
		shader.safeGetUniform("CoreAlpha").set(style.coreAlpha);
		shader.safeGetUniform("RimAlpha").set(style.rimAlpha);
		shader.safeGetUniform("RimPower").set(style.rimPower);
		shader.safeGetUniform("RimThreshold").set(style.rimThreshold);
		shader.safeGetUniform("Alpha").set(Mth.clamp(alpha, 0.0f, 1.0f));
		shader.safeGetUniform("BackFace").set(backFace);
		shader.safeGetUniform("BloomIntensity").set(style.bloomIntensity);
	}

	private static void drawShellMesh(VertexBuffer mesh, ShaderInstance shader, Matrix4f pose, Matrix4f projection, float bloomPass) {
		shader.safeGetUniform("BloomPass").set(bloomPass);

		shader.apply();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();

		mesh.bind();
		mesh.drawWithShader(pose, projection, shader);
		VertexBuffer.unbind();

		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
	}

	public static void drawEntity(PoseStack poseStack, Matrix4f projection, String type, float[] color, float ageInTicks, float bodyScale) {
		float time = ageInTicks / 20.0f;
		AuraStyle style = entityStyle(color);
		if (isSmooth(type)) {
			drawSmooth(poseStack, projection, style, 1.0f, 1.0f, time, time * style.waveSpeed,
					bodyScale, bodyScale, bodyScale, SMOOTH_BACKFACE);
			return;
		}
		float width = bodyScale * widthFactor(type);
		drawSparking(poseStack, projection, style, 1.0f, 1.0f, time, width, bodyScale * heightFactor(type), width,
				pivotFactor(type), DEFAULT_BACKFACE);
	}

	public static void drawEntityGroundPulse(PoseStack poseStack, Matrix4f projection, String type, float[] color, float alpha,
											 float ageInTicks, float radius, float height, float spinDegrees) {
		float time = ageInTicks / 20.0f;
		AuraStyle style = entityStyle(color);
		if (isSmooth(type)) {
			drawSmoothGroundPulse(poseStack, projection, style, alpha, time, time * style.waveSpeed, radius, height, spinDegrees);
			return;
		}
		drawSparkingGroundPulse(poseStack, projection, style, alpha, time, radius, height, spinDegrees);
	}

	private static AuraStyle entityStyle(float[] color) {
		int key = ColorUtils.rgbToInt(color[0], color[1], color[2]);
		return ENTITY_STYLES.computeIfAbsent(key, k -> AuraStyle.resolve(FormConfig.Aura3DStyle.DEFAULT, color.clone()));
	}

	public static void drawSparking(PoseStack poseStack, Matrix4f projection, AuraStyle style, float alpha, float growth,
									float time, float scaleX, float scaleY, float scaleZ, float pivot, float backFace) {
		ShaderInstance shader = DMZShaders.auraSparking3DShader;
		if (shader == null || style == null || alpha <= 0.001f || growth <= 0.001f) return;

		poseStack.pushPose();
		poseStack.scale(scaleX, scaleY, scaleZ);
		poseStack.translate(0.0f, pivot, 0.0f);
		drawSparkingShell(shader, poseStack, projection, style, alpha, growth, time * SPARKING_TIME_SCALE, backFace);
		poseStack.popPose();
	}

	public static void drawSparkingGroundPulse(PoseStack poseStack, Matrix4f projection, AuraStyle style,
											   float alpha, float time, float radius, float height, float spinDegrees) {
		ShaderInstance shader = DMZShaders.auraSparking3DShader;
		if (shader == null || style == null || alpha <= 0.001f) return;

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(spinDegrees));
		poseStack.scale(radius, height, radius);
		poseStack.translate(0.0f, 0.9f, 0.0f);
		drawSparkingShell(shader, poseStack, projection, style, alpha, 1.0f, time * SPARKING_PULSE_TIME_SCALE, DEFAULT_BACKFACE);
		poseStack.popPose();
	}

	private static void drawSparkingShell(ShaderInstance shader, PoseStack poseStack, Matrix4f projection, AuraStyle style,
										  float alpha, float growth, float time, float backFace) {
		Matrix4f pose = poseStack.last().pose();
		Matrix3f normal = poseStack.last().normal();
		applySparkingUniforms(shader, normal, style, alpha, growth, time, backFace);

		boolean captured = BloomPipeline.beginCapture();
		try {
			drawSparkingLayers(shader, pose, projection, 0.0f);
		} finally {
			if (captured) BloomPipeline.endCapture();
		}
		if (captured) return;

		Matrix4f poseCopy = new Matrix4f(pose);
		Matrix3f normalCopy = new Matrix3f(normal);
		Matrix4f projectionCopy = new Matrix4f(projection);
		AuraStyle styleCopy = new AuraStyle().copyFrom(style);
		AuraRenderer.captureBloom(() -> {
			ShaderInstance bloomShader = DMZShaders.auraSparking3DShader;
			if (bloomShader == null) return;
			applySparkingUniforms(bloomShader, normalCopy, styleCopy, alpha, growth, time, backFace);
			drawSparkingLayers(bloomShader, poseCopy, projectionCopy, 1.0f);
		});
	}

	private static void applySparkingUniforms(ShaderInstance shader, Matrix3f normal, AuraStyle style, float alpha,
											  float growth, float time, float backFace) {
		shader.safeGetUniform("NormalMat").set(normal);
		shader.safeGetUniform("Size").set(style.sizeX, style.sizeY, style.sizeZ);
		shader.safeGetUniform("Time").set(time);
		shader.safeGetUniform("Growth").set(Mth.clamp(growth, 0.0f, 1.0f));
		shader.safeGetUniform("SpikeDensity").set((float) Math.max(4, Math.round(style.spikeDensity)));
		shader.safeGetUniform("SpikeBurstRate").set(style.spikeBurstRate);
		shader.safeGetUniform("SpikeRarity").set(style.spikeRarity);
		shader.safeGetUniform("BandStart").set(style.bandStart);
		shader.safeGetUniform("WaveSpeed").set(style.waveSpeed);
		shader.safeGetUniform("WaveAmplitude").set(style.waveAmplitude);
		shader.safeGetUniform("NoiseDetail").set(style.noiseDetail);
		shader.safeGetUniform("CoreColor").set(style.coreColor[0], style.coreColor[1], style.coreColor[2]);
		shader.safeGetUniform("RimColor").set(style.rimColor[0], style.rimColor[1], style.rimColor[2]);
		shader.safeGetUniform("CoreAlpha").set(style.coreAlpha);
		shader.safeGetUniform("RimAlpha").set(style.rimAlpha);
		shader.safeGetUniform("RimPower").set(style.rimPower);
		shader.safeGetUniform("RimThreshold").set(style.rimThreshold);
		shader.safeGetUniform("Alpha").set(Mth.clamp(alpha, 0.0f, 1.0f));
		shader.safeGetUniform("BackFace").set(backFace);
		shader.safeGetUniform("BloomIntensity").set(style.bloomIntensity);
	}

	private static void drawSparkingLayers(ShaderInstance shader, Matrix4f pose, Matrix4f projection, float bloomPass) {
		VertexBuffer mesh = AuraMeshFactory.getSparkingFlameMesh();
		shader.safeGetUniform("BloomPass").set(bloomPass);

		RenderType renderType = AuraRenderer.auraType(DUMMY_TEXTURE);
		AuraRenderer.customSetup(renderType, DUMMY_TEXTURE, shader);

		mesh.bind();
		shader.safeGetUniform("LayerPass").set(1.0f);
		mesh.drawWithShader(pose, projection, shader);
		shader.safeGetUniform("LayerPass").set(0.0f);
		mesh.drawWithShader(pose, projection, shader);
		VertexBuffer.unbind();

		AuraRenderer.customClear(renderType);
	}
}
