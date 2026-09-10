package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Vector3f;

import java.util.function.DoubleUnaryOperator;

public class AuraMeshFactory {
	private static VertexBuffer billboardQuad;
	private static VertexBuffer groundQuad;
	private static VertexBuffer smoothFlame;
	private static VertexBuffer sparkingFlame;

	public static VertexBuffer getBillboardQuad() {
		if (billboardQuad == null) {
			billboardQuad = new VertexBuffer(VertexBuffer.Usage.STATIC);
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder builder = tesselator.getBuilder();
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			builder.vertex(-1.0f, -1.0f, 0.0f).uv(0.0f, 1.0f).endVertex();
			builder.vertex(1.0f, -1.0f, 0.0f).uv(1.0f, 1.0f).endVertex();
			builder.vertex(1.0f, 1.0f, 0.0f).uv(1.0f, 0.0f).endVertex();
			builder.vertex(-1.0f, 1.0f, 0.0f).uv(0.0f, 0.0f).endVertex();
			billboardQuad.bind();
			billboardQuad.upload(builder.end());
			VertexBuffer.unbind();
		}
		return billboardQuad;
	}

	public static VertexBuffer getGroundQuad() {
		if (groundQuad == null) {
			groundQuad = new VertexBuffer(VertexBuffer.Usage.STATIC);
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder builder = tesselator.getBuilder();
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			builder.vertex(-1.0f, 0.0f, -1.0f).uv(0.0f, 0.0f).endVertex();
			builder.vertex(-1.0f, 0.0f, 1.0f).uv(0.0f, 1.0f).endVertex();
			builder.vertex(1.0f, 0.0f, 1.0f).uv(1.0f, 1.0f).endVertex();
			builder.vertex(1.0f, 0.0f, -1.0f).uv(1.0f, 0.0f).endVertex();
			groundQuad.bind();
			groundQuad.upload(builder.end());
			VertexBuffer.unbind();
		}
		return groundQuad;
	}

	private static VertexBuffer buildFlameMesh(int rings, int segments, float capSpan, DoubleUnaryOperator body) {
		float[] radii = new float[rings + 1];
		float peak = 0.0f;
		for (int i = 0; i <= rings; i++) {
			double t = (double) i / rings;
			radii[i] = (float) (body.applyAsDouble(t) * baseCap(t, capSpan));
			if (radii[i] > peak) peak = radii[i];
		}
		if (peak <= 0.0f) peak = 1.0f;
		for (int i = 0; i <= rings; i++) radii[i] /= peak;

		VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);

		double angleStep = (Math.PI * 2.0) / segments;
		float ringStep = 2.0f / rings;

		for (int i = 0; i < rings; i++) {
			float y1 = -1.0f + i * ringStep;
			float y2 = -1.0f + (i + 1) * ringStep;
			float r1 = radii[i];
			float r2 = radii[i + 1];

			float slope1 = slopeAt(radii, i, rings, ringStep);
			float slope2 = slopeAt(radii, i + 1, rings, ringStep);

			for (int j = 0; j < segments; j++) {
				double a1 = j * angleStep;
				double a2 = (j + 1) * angleStep;
				float cos1 = (float) Math.cos(a1), sin1 = (float) Math.sin(a1);
				float cos2 = (float) Math.cos(a2), sin2 = (float) Math.sin(a2);

				emit(builder, r1 * cos1, y1, r1 * sin1, cos1, sin1, slope1);
				emit(builder, r1 * cos2, y1, r1 * sin2, cos2, sin2, slope1);
				emit(builder, r2 * cos2, y2, r2 * sin2, cos2, sin2, slope2);
				emit(builder, r2 * cos1, y2, r2 * sin1, cos1, sin1, slope2);
			}
		}

		buffer.bind();
		buffer.upload(builder.end());
		VertexBuffer.unbind();
		return buffer;
	}

	/**
	 * Rounds the base off to a closed bowl over the bottom {@code capSpan} of the height. Without it
	 * the mesh is an open tube: you look straight into the hollow interior from below, which reads
	 * as the aura being sliced off at the feet. Quarter ellipse, so it meets the body flat-tangent.
	 */
	private static double baseCap(double t, double capSpan) {
		if (capSpan <= 0.0 || t >= capSpan) return 1.0;
		double u = t / capSpan;
		return Math.sqrt(Math.max(0.0, 1.0 - (1.0 - u) * (1.0 - u)));
	}

	/** Flare from the root up to the widest point of the flame. */
	private static double flare(double t, double baseWidth, double riseSpan) {
		return baseWidth + (1.0 - baseWidth) * Math.sin(Math.PI * 0.5 * Math.min(1.0, t / riseSpan));
	}

	/**
	 * Superellipse quarter. Unlike a plain {@code (1-t)^p} taper — which draws a straight line and
	 * therefore reads as a cone — this bows the outline outwards along its whole length and reaches
	 * the tip with a vertical tangent, so the flame closes in a soft dome instead of a point.
	 */
	private static double superTaper(double t, double exponent) {
		return Math.pow(1.0 - Math.pow(t, exponent), 1.0 / exponent);
	}

	private static float slopeAt(float[] radii, int index, int rings, float ringStep) {
		int lo = Math.max(0, index - 1);
		int hi = Math.min(rings, index + 1);
		if (hi == lo) return 0.0f;
		return (radii[hi] - radii[lo]) / ((hi - lo) * ringStep);
	}

	private static void emit(BufferBuilder builder, float x, float y, float z, float cos, float sin, float slope) {
		Vector3f normal = new Vector3f(cos, -slope, sin).normalize();
		builder.vertex(x, y, z).color(255, 255, 255, 255).normal(normal.x(), normal.y(), normal.z()).endVertex();
	}

	public static VertexBuffer getSmoothFlameMesh() {
		if (smoothFlame == null) {
			smoothFlame = buildFlameMesh(96, 56, 0.10f, t -> flare(t, 0.55, 0.42) * superTaper(t, 1.6));
		}
		return smoothFlame;
	}

	public static VertexBuffer getSparkingFlameMesh() {
		if (sparkingFlame == null) {
			sparkingFlame = buildFlameMesh(96, 64, 0.10f, t -> flare(t, 0.60, 0.35) * Math.pow(1.0 - t, 1.05));
		}
		return sparkingFlame;
	}
}
