package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Locale;

public final class Aura3DRenderer {

	public static final String DEFAULT_TYPE = "smooth";
	private static final String SPARKING_TYPE = "sparking";
	public static final float DEFAULT_BACKFACE = 0.02f;

	private static final ResourceLocation DUMMY_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");

	private Aura3DRenderer() {}

	public static boolean isAvailable() {
		return DMZShaders.auraSmooth3DShader != null;
	}

	public static String resolveType(String type) {
		if (type == null) return DEFAULT_TYPE;
		String normalized = type.trim().toLowerCase(Locale.ROOT);
		return SPARKING_TYPE.equals(normalized) ? SPARKING_TYPE : DEFAULT_TYPE;
	}

	private static ShaderInstance shaderFor(String resolvedType) {
		ShaderInstance shader = SPARKING_TYPE.equals(resolvedType)
				? DMZShaders.auraSparking3DShader : DMZShaders.auraSmooth3DShader;
		return shader != null ? shader : DMZShaders.auraSmooth3DShader;
	}

	private static VertexBuffer meshFor(String resolvedType) {
		return SPARKING_TYPE.equals(resolvedType) ? AuraMeshFactory.getSparkingFlameMesh() : AuraMeshFactory.getSmoothFlameMesh();
	}

	public static float widthFactor(String type) {
		return SPARKING_TYPE.equals(resolveType(type)) ? 1.25f : 1.15f;
	}

	public static boolean followsBodyYaw(String type) {
		return !SPARKING_TYPE.equals(resolveType(type));
	}

	public static float heightFactor(String type) {
		return SPARKING_TYPE.equals(resolveType(type)) ? 1.65f : 2.05f;
	}

	public static float pivotFactor(String type) {
		return 0.80f;
	}

	public static float coreFactor(String type) {
		return 0.45f;
	}

	private static float timeScale(String resolvedType, boolean pulse) {
		if (!SPARKING_TYPE.equals(resolvedType)) return 1.0f;
		return pulse ? 0.50f : 0.67f;
	}

	public static void draw(PoseStack poseStack, Matrix4f projectionMatrix, String type, float[] color,
							float alpha, float time, float scaleX, float scaleY, float scaleZ, float pivot,
							float backFace) {
		if (alpha <= 0.001f) return;

		String resolved = resolveType(type);
		ShaderInstance shader = shaderFor(resolved);
		if (shader == null) return;

		poseStack.pushPose();
		poseStack.scale(scaleX, scaleY, scaleZ);
		poseStack.translate(0.0f, pivot, 0.0f);

		drawAndCaptureBloom(resolved, shader, poseStack, projectionMatrix, color, alpha, time * timeScale(resolved, false), backFace);

		poseStack.popPose();
	}

	public static void drawGroundPulse(PoseStack poseStack, Matrix4f projectionMatrix, String type, float[] color,
									   float alpha, float time, float radius, float height, float spinDegrees) {
		if (alpha <= 0.001f) return;

		String resolved = resolveType(type);
		ShaderInstance shader = shaderFor(resolved);
		if (shader == null) return;

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(spinDegrees));
		poseStack.scale(radius, height, radius);
		poseStack.translate(0.0f, 0.9f, 0.0f);

		drawAndCaptureBloom(resolved, shader, poseStack, projectionMatrix, color, alpha, time * timeScale(resolved, true), DEFAULT_BACKFACE);

		poseStack.popPose();
	}

	private static void drawAndCaptureBloom(String resolved, ShaderInstance shader, PoseStack poseStack, Matrix4f projectionMatrix,
											float[] color, float alpha, float time, float backFace) {
		applyUniforms(shader, poseStack, projectionMatrix, color, alpha, time, backFace);
		drawLayers(resolved, shader, poseStack, projectionMatrix);

		Matrix4f pose = new Matrix4f(poseStack.last().pose());
		Matrix3f normal = new Matrix3f(poseStack.last().normal());
		Matrix4f proj = new Matrix4f(projectionMatrix);
		float[] c = color.clone();
		AuraRenderer.captureBloom(() -> {
			ShaderInstance bloomShader = shaderFor(resolved);
			if (bloomShader == null) return;
			PoseStack replay = new PoseStack();
			replay.last().pose().set(pose);
			replay.last().normal().set(normal);

			applyUniforms(bloomShader, replay, proj, c, alpha, time, backFace);
			bloomShader.safeGetUniform("bloomMode").set(1.0f);
			drawLayers(resolved, bloomShader, replay, proj);
			bloomShader.safeGetUniform("bloomMode").set(0.0f);
		});
	}

	private static void applyUniforms(ShaderInstance shader, PoseStack poseStack, Matrix4f projectionMatrix,
									  float[] color, float alpha, float time, float backFace) {
		shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
		shader.safeGetUniform("ProjMat").set(projectionMatrix);
		shader.safeGetUniform("normalMatrix").set(new Matrix4f(new Matrix3f(poseStack.last().normal())));
		shader.safeGetUniform("time").set(time);
		shader.safeGetUniform("auravar").set(1.0f);

		float core = 0.55f;
		shader.safeGetUniform("color1").set(
				Mth.lerp(core, color[0], 1.0f),
				Mth.lerp(core, color[1], 1.0f),
				Mth.lerp(core, color[2], 1.0f));
		shader.safeGetUniform("color2").set(color[0] * 1.25f, color[1] * 1.25f, color[2] * 1.25f);

		shader.safeGetUniform("alp1").set(0.45f * alpha);
		shader.safeGetUniform("alp2").set(0.85f * (float) Math.pow(alpha, 0.35));
		shader.safeGetUniform("power").set(6.0f);
		shader.safeGetUniform("divis").set(0.02f);
		shader.safeGetUniform("backFace").set(backFace);
	}

	private static void drawLayers(String resolved, ShaderInstance shader, PoseStack poseStack, Matrix4f projectionMatrix) {
		VertexBuffer mesh = meshFor(resolved);
		if (SPARKING_TYPE.equals(resolved)) {
			shader.safeGetUniform("layerPass").set(1.0f);
			drawMesh(mesh, shader, poseStack, projectionMatrix);
			shader.safeGetUniform("layerPass").set(0.0f);
		}
		drawMesh(mesh, shader, poseStack, projectionMatrix);
	}

	private static void drawMesh(VertexBuffer mesh, ShaderInstance shader, PoseStack poseStack, Matrix4f projectionMatrix) {
		RenderType renderType = AuraRenderer.auraType(DUMMY_TEXTURE);
		AuraRenderer.customSetup(renderType, DUMMY_TEXTURE, shader);

		shader.apply();
		mesh.bind();
		mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
		VertexBuffer.unbind();
		shader.clear();

		AuraRenderer.customClear(renderType);
	}
}
