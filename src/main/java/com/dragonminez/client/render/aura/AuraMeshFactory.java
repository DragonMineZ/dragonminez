package com.dragonminez.client.render.aura;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Vector3f;

public class AuraMeshFactory {
	private static VertexBuffer cachedSmoothAuraMesh;
	private static VertexBuffer cachedSharpAuraMesh;
	private static VertexBuffer cachedSparkingAuraMesh;

	public static VertexBuffer getSmoothAuraMesh() {
		if (cachedSmoothAuraMesh == null) {
			cachedSmoothAuraMesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder builder = tesselator.getBuilder();

			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);

			int segments = 60;
			float angleStep = 360.0f / segments;

			for (int i = 0; i < segments; i++) {
				double v1 = (double) i / segments;
				double v2 = (double) (i + 1) / segments;
				double y1 = v1 * 2.0 - 1.0;
				double y2 = v2 * 2.0 - 1.0;

				double r1 = Math.sqrt(1.0 - y1 * y1);
				double r2 = Math.sqrt(1.0 - y2 * y2);

				if (Double.isNaN(r1) || r1 < 0.0) r1 = 0.0;
				if (Double.isNaN(r2) || r2 < 0.0) r2 = 0.0;

				for (int j = 0; j < segments; j++) {
					double a1 = Math.toRadians(j * angleStep);
					double a2 = Math.toRadians((j + 1.0f) * angleStep);

					float x1_y1 = (float) (r1 * Math.cos(a1));
					float z1_y1 = (float) (r1 * Math.sin(a1));
					float x2_y1 = (float) (r1 * Math.cos(a2));
					float z2_y1 = (float) (r1 * Math.sin(a2));

					float x2_y2 = (float) (r2 * Math.cos(a2));
					float z2_y2 = (float) (r2 * Math.sin(a2));
					float x1_y2 = (float) (r2 * Math.cos(a1));
					float z1_y2 = (float) (r2 * Math.sin(a1));

					builder.vertex(x1_y1, (float) y1, z1_y1).color(255, 255, 255, 127).normal(x1_y1, (float) y1, z1_y1).endVertex();
					builder.vertex(x2_y1, (float) y1, z2_y1).color(255, 255, 255, 127).normal(x2_y1, (float) y1, z2_y1).endVertex();
					builder.vertex(x2_y2, (float) y2, z2_y2).color(255, 255, 255, 127).normal(x2_y2, (float) y2, z2_y2).endVertex();
					builder.vertex(x1_y2, (float) y2, z1_y2).color(255, 255, 255, 127).normal(x1_y2, (float) y2, z1_y2).endVertex();
				}
			}

			cachedSmoothAuraMesh.bind();
			cachedSmoothAuraMesh.upload(builder.end());
			VertexBuffer.unbind();
		}
		return cachedSmoothAuraMesh;
	}

	public static VertexBuffer getSharpAuraMesh() {
		if (cachedSharpAuraMesh == null) {
			cachedSharpAuraMesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder builder = tesselator.getBuilder();

			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);

			int segments = 60;
			float angleStep = 360.0f / segments;

			float maxW = 0.95f;
			float baseW = 0.35f;

			for (int i = 0; i < segments; i++) {
				double v1 = (double) i / segments;
				double v2 = (double) (i + 1) / segments;
				double y1 = v1 * 2.0 - 1.0;
				double y2 = v2 * 2.0 - 1.0;

				double pow1 = Math.pow(1.0 - v1, 2.5);
				double pow2 = Math.pow(1.0 - v2, 2.5);

				double r1 = (baseW + (maxW - baseW) * pow1) * Math.sqrt(1.0 - y1 * y1);
				double r2 = (baseW + (maxW - baseW) * pow2) * Math.sqrt(1.0 - y2 * y2);

				if (Double.isNaN(r1) || r1 < 0.0) r1 = 0.0;
				if (Double.isNaN(r2) || r2 < 0.0) r2 = 0.0;

				for (int j = 0; j < segments; j++) {
					double a1 = Math.toRadians(j * angleStep);
					double a2 = Math.toRadians((j + 1.0f) * angleStep);

					float x1_y1 = (float) (r1 * Math.cos(a1));
					float z1_y1 = (float) (r1 * Math.sin(a1));
					float x2_y1 = (float) (r1 * Math.cos(a2));
					float z2_y1 = (float) (r1 * Math.sin(a2));

					float x2_y2 = (float) (r2 * Math.cos(a2));
					float z2_y2 = (float) (r2 * Math.sin(a2));
					float x1_y2 = (float) (r2 * Math.cos(a1));
					float z1_y2 = (float) (r2 * Math.sin(a1));

					Vector3f n1 = new Vector3f(x1_y1, (float)y1, z1_y1).normalize();
					Vector3f n2 = new Vector3f(x2_y1, (float)y1, z2_y1).normalize();
					Vector3f n3 = new Vector3f(x2_y2, (float)y2, z2_y2).normalize();
					Vector3f n4 = new Vector3f(x1_y2, (float)y2, z1_y2).normalize();

					builder.vertex(x1_y1, (float) y1, z1_y1).color(255, 255, 255, 127).normal(n1.x(), n1.y(), n1.z()).endVertex();
					builder.vertex(x2_y1, (float) y1, z2_y1).color(255, 255, 255, 127).normal(n2.x(), n2.y(), n2.z()).endVertex();
					builder.vertex(x2_y2, (float) y2, z2_y2).color(255, 255, 255, 127).normal(n3.x(), n3.y(), n3.z()).endVertex();
					builder.vertex(x1_y2, (float) y2, z1_y2).color(255, 255, 255, 127).normal(n4.x(), n4.y(), n4.z()).endVertex();
				}
			}

			cachedSharpAuraMesh.bind();
			cachedSharpAuraMesh.upload(builder.end());
			VertexBuffer.unbind();
		}
		return cachedSharpAuraMesh;
	}

	public static VertexBuffer getSparkingAuraMesh() {
		if (cachedSparkingAuraMesh == null) {
			cachedSparkingAuraMesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder builder = tesselator.getBuilder();

			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);

			int segments = 90;
			float angleStep = 360.0f / segments;

			for (int i = 0; i < segments; i++) {
				double v1 = (double) i / segments;
				double v2 = (double) (i + 1) / segments;
				double y1 = v1 * 2.0 - 1.0;
				double y2 = v2 * 2.0 - 1.0;

				double r1 = Math.sqrt(1.0 - y1 * y1);
				double r2 = Math.sqrt(1.0 - y2 * y2);

				if (Double.isNaN(r1) || r1 < 0.0) r1 = 0.0;
				if (Double.isNaN(r2) || r2 < 0.0) r2 = 0.0;

				for (int j = 0; j < segments; j++) {
					double a1 = Math.toRadians(j * angleStep);
					double a2 = Math.toRadians((j + 1.0f) * angleStep);

					float x1_y1 = (float) (r1 * Math.cos(a1));
					float z1_y1 = (float) (r1 * Math.sin(a1));
					float x2_y1 = (float) (r1 * Math.cos(a2));
					float z2_y1 = (float) (r1 * Math.sin(a2));

					float x2_y2 = (float) (r2 * Math.cos(a2));
					float z2_y2 = (float) (r2 * Math.sin(a2));
					float x1_y2 = (float) (r2 * Math.cos(a1));
					float z1_y2 = (float) (r2 * Math.sin(a1));

					Vector3f n1 = new Vector3f(x1_y1, (float)y1, z1_y1).normalize();
					Vector3f n2 = new Vector3f(x2_y1, (float)y1, z2_y1).normalize();
					Vector3f n3 = new Vector3f(x2_y2, (float)y2, z2_y2).normalize();
					Vector3f n4 = new Vector3f(x1_y2, (float)y2, z1_y2).normalize();

					builder.vertex(x1_y1, (float) y1, z1_y1).color(255, 255, 255, 127).normal(n1.x(), n1.y(), n1.z()).endVertex();
					builder.vertex(x2_y1, (float) y1, z2_y1).color(255, 255, 255, 127).normal(n2.x(), n2.y(), n2.z()).endVertex();
					builder.vertex(x2_y2, (float) y2, z2_y2).color(255, 255, 255, 127).normal(n3.x(), n3.y(), n3.z()).endVertex();
					builder.vertex(x1_y2, (float) y2, z1_y2).color(255, 255, 255, 127).normal(n4.x(), n4.y(), n4.z()).endVertex();
				}
			}

			cachedSparkingAuraMesh.bind();
			cachedSparkingAuraMesh.upload(builder.end());
			VertexBuffer.unbind();
		}
		return cachedSparkingAuraMesh;
	}
}