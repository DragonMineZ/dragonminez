package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

public class AuraMeshFactory {
	private static VertexBuffer billboardQuad;
	private static VertexBuffer groundQuad;

	// Per-frame POSITION_TEX quads: same geometry as the billboard/ground quads but
	// with the UVs sliced to one column of the 1x4 aura spritesheet. Used by the
	// shaderpack path, which draws with a vanilla position_tex shader (it can't
	// offset UVs by a 'speed' uniform like the custom aura shader does), so the
	// current animation frame is baked into the mesh instead.
	private static final VertexBuffer[] billboardFrames = new VertexBuffer[4];
	private static final VertexBuffer[] groundFrames = new VertexBuffer[4];

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

	public static VertexBuffer getBillboardQuadFrame(int frame) {
		frame = ((frame % 4) + 4) % 4;
		if (billboardFrames[frame] == null) {
			float u0 = frame / 4.0f;
			float u1 = (frame + 1) / 4.0f;
			VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
			BufferBuilder builder = Tesselator.getInstance().getBuilder();
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			builder.vertex(-1.0f, -1.0f, 0.0f).uv(u0, 1.0f).endVertex();
			builder.vertex(1.0f, -1.0f, 0.0f).uv(u1, 1.0f).endVertex();
			builder.vertex(1.0f, 1.0f, 0.0f).uv(u1, 0.0f).endVertex();
			builder.vertex(-1.0f, 1.0f, 0.0f).uv(u0, 0.0f).endVertex();
			vb.bind();
			vb.upload(builder.end());
			VertexBuffer.unbind();
			billboardFrames[frame] = vb;
		}
		return billboardFrames[frame];
	}

	public static VertexBuffer getGroundQuadFrame(int frame) {
		frame = ((frame % 4) + 4) % 4;
		if (groundFrames[frame] == null) {
			float u0 = frame / 4.0f;
			float u1 = (frame + 1) / 4.0f;
			VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
			BufferBuilder builder = Tesselator.getInstance().getBuilder();
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			builder.vertex(-1.0f, 0.0f, -1.0f).uv(u0, 0.0f).endVertex();
			builder.vertex(-1.0f, 0.0f, 1.0f).uv(u0, 1.0f).endVertex();
			builder.vertex(1.0f, 0.0f, 1.0f).uv(u1, 1.0f).endVertex();
			builder.vertex(1.0f, 0.0f, -1.0f).uv(u1, 0.0f).endVertex();
			vb.bind();
			vb.upload(builder.end());
			VertexBuffer.unbind();
			groundFrames[frame] = vb;
		}
		return groundFrames[frame];
	}
}