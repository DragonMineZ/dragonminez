package com.dragonminez.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

/**
 * 1.21 rendering helpers (BufferBuilder/Tesselator API rewrite).
 * Preserves previous textured-quad drawing behavior used by HUD overlays.
 */
public final class RenderBufferUtil {
	private RenderBufferUtil() {}

	public static void drawTexturedQuad(Matrix4f matrix, float size) {
		drawTexturedQuad(matrix, -size, size, size, -size, 0f, 0f, 1f, 1f);
	}

	public static void drawTexturedQuad(Matrix4f matrix, float x0, float y0, float x1, float y1,
										float u0, float v0, float u1, float v1) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		builder.addVertex(matrix, x0, y0, 0).setUv(u0, v1);
		builder.addVertex(matrix, x1, y0, 0).setUv(u1, v1);
		builder.addVertex(matrix, x1, y1, 0).setUv(u1, v0);
		builder.addVertex(matrix, x0, y1, 0).setUv(u0, v0);
		MeshData mesh = builder.buildOrThrow();
		BufferUploader.drawWithShader(mesh);
	}

	public static void drawColoredQuad(Matrix4f matrix, float x0, float y0, float x1, float y1, int argb) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		builder.addVertex(matrix, x0, y0, 0).setColor(argb);
		builder.addVertex(matrix, x1, y0, 0).setColor(argb);
		builder.addVertex(matrix, x1, y1, 0).setColor(argb);
		builder.addVertex(matrix, x0, y1, 0).setColor(argb);
		MeshData mesh = builder.buildOrThrow();
		BufferUploader.drawWithShader(mesh);
	}

	public static float partialTick(net.minecraft.client.DeltaTracker tracker) {
		return tracker.getGameTimeDeltaPartialTick(false);
	}
}
