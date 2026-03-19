package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

public class AuraMeshFactory {
	private static VertexBuffer billboardQuad;
	private static VertexBuffer groundQuad;

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
}