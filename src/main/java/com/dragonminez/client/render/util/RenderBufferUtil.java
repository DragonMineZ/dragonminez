package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

/** Immediate-mode buffer helpers for 1.21 (Tesselator no longer has getBuilder/end). */
public final class RenderBufferUtil {
	private RenderBufferUtil() {}

	/** Pack RGBA floats (0-1) into ARGB int for GeckoLib 1.21 render APIs. */
	public static int packColor(float r, float g, float b, float a) {
		int ai = Math.max(0, Math.min(255, (int)(a * 255.0f)));
		int ri = Math.max(0, Math.min(255, (int)(r * 255.0f)));
		int gi = Math.max(0, Math.min(255, (int)(g * 255.0f)));
		int bi = Math.max(0, Math.min(255, (int)(b * 255.0f)));
		return (ai << 24) | (ri << 16) | (gi << 8) | bi;
	}

	public static void drawTexturedQuad(Matrix4f matrix, float x0, float y0, float x1, float y1, float z,
			float minU, float minV, float maxU, float maxV) {
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.addVertex(matrix, x0, y1, z).setUv(minU, maxV);
		buffer.addVertex(matrix, x1, y1, z).setUv(maxU, maxV);
		buffer.addVertex(matrix, x1, y0, z).setUv(maxU, minV);
		buffer.addVertex(matrix, x0, y0, z).setUv(minU, minV);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	public static void drawTexturedQuadCentered(Matrix4f matrix, float halfSize, float minU, float minV, float maxU, float maxV) {
		drawTexturedQuad(matrix, -halfSize, -halfSize, halfSize, halfSize, 0.0F, minU, minV, maxU, maxV);
	}
}
