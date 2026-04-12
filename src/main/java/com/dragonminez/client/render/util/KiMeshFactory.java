package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class KiMeshFactory {
	private static VertexBuffer cachedSphereMesh;
	private static VertexBuffer cachedCylinderMesh;

	public static VertexBuffer getSphereMesh() {
		if (cachedSphereMesh == null) {
			cachedSphereMesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder builder = tesselator.getBuilder();

			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);

			int segments = 32;
			int rings = 16;

			for (int i = 0; i < rings; i++) {
				float theta1 = (float) (Math.PI * i / rings);
				float theta2 = (float) (Math.PI * (i + 1) / rings);

				for (int j = 0; j < segments; j++) {
					float phi1 = (float) (2.0 * Math.PI * j / segments);
					float phi2 = (float) (2.0 * Math.PI * (j + 1) / segments);
					float u1 = (float) j / segments;
					float u2 = (float) (j + 1) / segments;
					float v1 = (float) i / rings;
					float v2 = (float) (i + 1) / rings;

					float x1 = (float) (Math.sin(theta1) * Math.cos(phi1));
					float y1 = (float) Math.cos(theta1);
					float z1 = (float) (Math.sin(theta1) * Math.sin(phi1));

					float x2 = (float) (Math.sin(theta1) * Math.cos(phi2));
					float y2 = (float) Math.cos(theta1);
					float z2 = (float) (Math.sin(theta1) * Math.sin(phi2));

					float x3 = (float) (Math.sin(theta2) * Math.cos(phi2));
					float y3 = (float) Math.cos(theta2);
					float z3 = (float) (Math.sin(theta2) * Math.sin(phi2));

					float x4 = (float) (Math.sin(theta2) * Math.cos(phi1));
					float y4 = (float) Math.cos(theta2);
					float z4 = (float) (Math.sin(theta2) * Math.sin(phi1));

					builder.vertex(x1, y1, z1).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(x1, y1, z1).endVertex();
					builder.vertex(x2, y2, z2).color(255, 255, 255, 255).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(x2, y2, z2).endVertex();
					builder.vertex(x3, y3, z3).color(255, 255, 255, 255).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(x3, y3, z3).endVertex();
					builder.vertex(x4, y4, z4).color(255, 255, 255, 255).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(x4, y4, z4).endVertex();
				}
			}

			cachedSphereMesh.bind();
			cachedSphereMesh.upload(builder.end());
			VertexBuffer.unbind();
		}
		return cachedSphereMesh;
	}

	public static VertexBuffer getCylinderMesh() {
		if (cachedCylinderMesh == null) {
			cachedCylinderMesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder builder = tesselator.getBuilder();

			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);

			int segments = 32;
			for (int i = 0; i < segments; i++) {
				float theta1 = (float) (2.0 * Math.PI * i / segments);
				float theta2 = (float) (2.0 * Math.PI * (i + 1) / segments);

				float x1 = (float) Math.cos(theta1);
				float y1 = (float) Math.sin(theta1);
				float x2 = (float) Math.cos(theta2);
				float y2 = (float) Math.sin(theta2);

				float u1 = (float) i / segments;
				float u2 = (float) (i + 1) / segments;
				float v1 = 0.0f;
				float v2 = 1.0f;

				builder.vertex(x1, y1, 0).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(x1, y1, 0).endVertex();
				builder.vertex(x2, y2, 0).color(255, 255, 255, 255).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(x2, y2, 0).endVertex();
				builder.vertex(x2, y2, 1).color(255, 255, 255, 255).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(x2, y2, 0).endVertex();
				builder.vertex(x1, y1, 1).color(255, 255, 255, 255).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(x1, y1, 0).endVertex();

				builder.vertex(0, 0, 0).color(255, 255, 255, 255).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, -1).endVertex();
				builder.vertex(x1, y1, 0).color(255, 255, 255, 255).uv(x1 * 0.5f + 0.5f, y1 * 0.5f + 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, -1).endVertex();
				builder.vertex(x2, y2, 0).color(255, 255, 255, 255).uv(x2 * 0.5f + 0.5f, y2 * 0.5f + 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, -1).endVertex();
				builder.vertex(0, 0, 0).color(255, 255, 255, 255).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, -1).endVertex();

				builder.vertex(0, 0, 1).color(255, 255, 255, 255).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, 1).endVertex();
				builder.vertex(x2, y2, 1).color(255, 255, 255, 255).uv(x2 * 0.5f + 0.5f, y2 * 0.5f + 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, 1).endVertex();
				builder.vertex(x1, y1, 1).color(255, 255, 255, 255).uv(x1 * 0.5f + 0.5f, y1 * 0.5f + 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, 1).endVertex();
				builder.vertex(0, 0, 1).color(255, 255, 255, 255).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, 1).endVertex();
			}

			cachedCylinderMesh.bind();
			cachedCylinderMesh.upload(builder.end());
			VertexBuffer.unbind();
		}
		return cachedCylinderMesh;
	}
}