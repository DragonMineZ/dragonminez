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
	private static VertexBuffer cachedTriangleBeamMesh;
	private static VertexBuffer cachedQuadMesh;
	private static VertexBuffer cachedTorusMesh;
	private static float cachedTorusRatio;

	public static VertexBuffer getQuadMesh() {
		if (cachedQuadMesh == null) {
			cachedQuadMesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
			BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);

			quadVertex(builder, -1.0F, -1.0F, 0.0F, 0.0F);
			quadVertex(builder,  1.0F, -1.0F, 1.0F, 0.0F);
			quadVertex(builder,  1.0F,  1.0F, 1.0F, 1.0F);
			quadVertex(builder, -1.0F,  1.0F, 0.0F, 1.0F);

			cachedQuadMesh.bind();
			cachedQuadMesh.upload(builder.buildOrThrow());
			VertexBuffer.unbind();
		}
		return cachedQuadMesh;
	}

	private static void quadVertex(BufferBuilder builder, float x, float y, float u, float v) {
		builder.addVertex(x, y, 0.0F).setColor(255, 255, 255, 255).setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 0.0F, 1.0F);
	}

	public static VertexBuffer getSphereMesh() {
		if (cachedSphereMesh == null) {
			cachedSphereMesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);

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

					builder.addVertex(x1, y1, z1).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(x1, y1, z1);
					builder.addVertex(x2, y2, z2).setColor(255, 255, 255, 255).setUv(u2, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(x2, y2, z2);
					builder.addVertex(x3, y3, z3).setColor(255, 255, 255, 255).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(x3, y3, z3);
					builder.addVertex(x4, y4, z4).setColor(255, 255, 255, 255).setUv(u1, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(x4, y4, z4);
				}
			}

			cachedSphereMesh.bind();
			cachedSphereMesh.upload(builder.buildOrThrow());
			VertexBuffer.unbind();
		}
		return cachedSphereMesh;
	}

	public static VertexBuffer getCylinderMesh() {
		if (cachedCylinderMesh == null) {
			cachedCylinderMesh = buildTubeMesh(32, 0.0);
		}
		return cachedCylinderMesh;
	}

	public static VertexBuffer getTriangleBeamMesh() {
		if (cachedTriangleBeamMesh == null) {
			cachedTriangleBeamMesh = buildTubeMesh(3, Math.PI / 2.0);
		}
		return cachedTriangleBeamMesh;
	}

	public static VertexBuffer getTorusMesh(float majorToTube) {
		if (cachedTorusMesh == null || cachedTorusRatio != majorToTube) {
			if (cachedTorusMesh != null) cachedTorusMesh.close();
			cachedTorusMesh = buildTorusMesh(64, 16, majorToTube);
			cachedTorusRatio = majorToTube;
		}
		return cachedTorusMesh;
	}

	private static VertexBuffer buildTorusMesh(int ringSegments, int tubeSegments, float major) {
		VertexBuffer mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
		BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);

		for (int i = 0; i < ringSegments; i++) {
			float u1 = (float) (2.0 * Math.PI * i / ringSegments);
			float u2 = (float) (2.0 * Math.PI * (i + 1) / ringSegments);
			for (int j = 0; j < tubeSegments; j++) {
				float v1 = (float) (2.0 * Math.PI * j / tubeSegments);
				float v2 = (float) (2.0 * Math.PI * (j + 1) / tubeSegments);
				torusVertex(builder, major, u1, v1, (float) i / ringSegments, (float) j / tubeSegments);
				torusVertex(builder, major, u2, v1, (float) (i + 1) / ringSegments, (float) j / tubeSegments);
				torusVertex(builder, major, u2, v2, (float) (i + 1) / ringSegments, (float) (j + 1) / tubeSegments);
				torusVertex(builder, major, u1, v2, (float) i / ringSegments, (float) (j + 1) / tubeSegments);
			}
		}

		mesh.bind();
		mesh.upload(builder.buildOrThrow());
		VertexBuffer.unbind();
		return mesh;
	}

	private static void torusVertex(BufferBuilder builder, float major, float u, float v, float texU, float texV) {
		float cu = (float) Math.cos(u);
		float su = (float) Math.sin(u);
		float cv = (float) Math.cos(v);
		float sv = (float) Math.sin(v);
		float ring = major + cv;
		builder.addVertex(cu * ring, su * ring, sv).setColor(255, 255, 255, 255).setUv(texU, texV)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(cu * cv, su * cv, sv);
	}

	private static VertexBuffer buildTubeMesh(int segments, double angleOffset) {
		VertexBuffer mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);

		for (int i = 0; i < segments; i++) {
			float theta1 = (float) (angleOffset + 2.0 * Math.PI * i / segments);
			float theta2 = (float) (angleOffset + 2.0 * Math.PI * (i + 1) / segments);

			float x1 = (float) Math.cos(theta1);
			float y1 = (float) Math.sin(theta1);
			float x2 = (float) Math.cos(theta2);
			float y2 = (float) Math.sin(theta2);

			float u1 = (float) i / segments;
			float u2 = (float) (i + 1) / segments;
			float v1 = 0.0f;
			float v2 = 1.0f;

			builder.addVertex(x1, y1, 0).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(x1, y1, 0);
			builder.addVertex(x2, y2, 0).setColor(255, 255, 255, 255).setUv(u2, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(x2, y2, 0);
			builder.addVertex(x2, y2, 1).setColor(255, 255, 255, 255).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(x2, y2, 0);
			builder.addVertex(x1, y1, 1).setColor(255, 255, 255, 255).setUv(u1, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(x1, y1, 0);

			builder.addVertex(0, 0, 0).setColor(255, 255, 255, 255).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 0, -1);
			builder.addVertex(x1, y1, 0).setColor(255, 255, 255, 255).setUv(x1 * 0.5f + 0.5f, y1 * 0.5f + 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 0, -1);
			builder.addVertex(x2, y2, 0).setColor(255, 255, 255, 255).setUv(x2 * 0.5f + 0.5f, y2 * 0.5f + 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 0, -1);
			builder.addVertex(0, 0, 0).setColor(255, 255, 255, 255).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 0, -1);

			builder.addVertex(0, 0, 1).setColor(255, 255, 255, 255).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 0, 1);
			builder.addVertex(x2, y2, 1).setColor(255, 255, 255, 255).setUv(x2 * 0.5f + 0.5f, y2 * 0.5f + 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 0, 1);
			builder.addVertex(x1, y1, 1).setColor(255, 255, 255, 255).setUv(x1 * 0.5f + 0.5f, y1 * 0.5f + 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 0, 1);
			builder.addVertex(0, 0, 1).setColor(255, 255, 255, 255).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0, 0, 1);
		}

		mesh.bind();
		mesh.upload(builder.buildOrThrow());
		VertexBuffer.unbind();
		return mesh;
	}
}
