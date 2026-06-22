package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;

public class KiSpiralMesh {
	private static final int MAX_RINGS = 3000;
	private static final int RING_DENSITY = 24;
	private static final int RADIAL = 8;
	private static final int LOCAL_Z_CYCLE = 6;
	private static final int CACHE_CAPACITY = 8;

	private record Key(int type, int lengthBucket, int sizeBucket) {}

	private static final LinkedHashMap<Key, VertexBuffer> CACHE =
			new LinkedHashMap<>(16, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<Key, VertexBuffer> eldest) {
					if (size() > CACHE_CAPACITY) {
						eldest.getValue().close();
						return true;
					}
					return false;
				}
			};

	public static VertexBuffer get(int type, float size, float length,
								   float radius, float tubeRadius, float tubeRadius2,
								   float twistsPerBlock, boolean doubleHelix) {
		Key key = new Key(type, Math.round(length * 4.0f), Math.round(size * 8.0f));
		VertexBuffer buffer = CACHE.get(key);
		if (buffer == null) {
			buffer = build(length, radius, tubeRadius, tubeRadius2, twistsPerBlock, doubleHelix);
			CACHE.put(key, buffer);
		}
		return buffer;
	}

	private static VertexBuffer build(float length, float radius, float tubeRadius,
									  float tubeRadius2, float twistsPerBlock, boolean doubleHelix) {
		VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder builder = tesselator.getBuilder();

		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);

		int rings = Math.max(2, Math.min(MAX_RINGS, (int) (length * RING_DENSITY)));
		float k = (float) (2.0 * Math.PI * twistsPerBlock);

		appendTube(builder, length, radius, tubeRadius, k, rings, 0.0F);
		if (doubleHelix) {
			appendTube(builder, length, radius, tubeRadius2, k, rings, (float) Math.PI);
		}

		BufferBuilder.RenderedBuffer rendered = builder.end();
		buffer.bind();
		buffer.upload(rendered);
		VertexBuffer.unbind();
		return buffer;
	}

	private static void appendTube(BufferBuilder builder, float length, float radius,
								   float tubeRadius, float k, int rings, float phase) {
		float step = length / rings;

		Vector3f center0 = new Vector3f();
		Vector3f center1 = new Vector3f();
		Vector3f tangent = new Vector3f();
		Vector3f side = new Vector3f();
		Vector3f up = new Vector3f();
		Vector3f ref = new Vector3f();

		Vector3f p00 = new Vector3f();
		Vector3f p01 = new Vector3f();
		Vector3f p10 = new Vector3f();
		Vector3f p11 = new Vector3f();
		Vector3f n00 = new Vector3f();
		Vector3f n01 = new Vector3f();
		Vector3f n10 = new Vector3f();
		Vector3f n11 = new Vector3f();

		for (int i = 0; i < rings - 1; i++) {
			float z0 = i * step;
			float z1 = (i + 1) * step;

			helixCenter(center0, radius, k, z0, phase);
			helixCenter(center1, radius, k, z1, phase);

			helixFrame(z0, radius, k, phase, tangent, side, up, ref);
			float lz0 = (float) (i % LOCAL_Z_CYCLE) / LOCAL_Z_CYCLE;

			Vector3f sideA = new Vector3f(side);
			Vector3f upA = new Vector3f(up);

			helixFrame(z1, radius, k, phase, tangent, side, up, ref);
			float lz1 = (float) ((i + 1) % LOCAL_Z_CYCLE) / LOCAL_Z_CYCLE;

			for (int j = 0; j < RADIAL; j++) {
				float a0 = (float) (2.0 * Math.PI * j / RADIAL);
				float a1 = (float) (2.0 * Math.PI * (j + 1) / RADIAL);

				float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
				float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);

				ringPoint(p00, n00, center0, sideA, upA, c0, s0, tubeRadius);
				ringPoint(p01, n01, center0, sideA, upA, c1, s1, tubeRadius);
				ringPoint(p11, n11, center1, side, up, c1, s1, tubeRadius);
				ringPoint(p10, n10, center1, side, up, c0, s0, tubeRadius);

				vertex(builder, p00, n00, c0, s0, lz0, 0.0F);
				vertex(builder, p01, n01, c1, s1, lz0, 1.0F);
				vertex(builder, p11, n11, c1, s1, lz1, 1.0F);
				vertex(builder, p10, n10, c0, s0, lz1, 0.0F);
			}
		}
	}

	private static void helixCenter(Vector3f out, float radius, float k, float z, float phase) {
		float a = k * z + phase;
		out.set(-radius * (float) Math.sin(a), radius * (float) Math.cos(a), z);
	}

	private static void helixFrame(float z, float radius, float k, float phase,
								   Vector3f tangent, Vector3f side, Vector3f up, Vector3f ref) {
		float a = k * z + phase;
		tangent.set(-radius * k * (float) Math.cos(a), -radius * k * (float) Math.sin(a), 1.0F).normalize();

		if (Math.abs(tangent.z) < 0.95F) {
			ref.set(0.0F, 0.0F, 1.0F);
		} else {
			ref.set(1.0F, 0.0F, 0.0F);
		}
		tangent.cross(ref, side).normalize();
		side.cross(tangent, up).normalize();
	}

	private static void ringPoint(Vector3f outPos, Vector3f outNormal, Vector3f center,
								  Vector3f side, Vector3f up, float cos, float sin, float tubeRadius) {
		outNormal.set(
				side.x * cos + up.x * sin,
				side.y * cos + up.y * sin,
				side.z * cos + up.z * sin);
		outPos.set(
				center.x + outNormal.x * tubeRadius,
				center.y + outNormal.y * tubeRadius,
				center.z + outNormal.z * tubeRadius);
	}

	private static void vertex(BufferBuilder builder, Vector3f pos, Vector3f normal,
							   float localX, float localY, float localZ, float u) {
		builder.vertex(pos.x, pos.y, pos.z)
				.color(encode(localX * 0.5F + 0.5F), encode(localY * 0.5F + 0.5F), encode(localZ), 255)
				.uv(u, localZ)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(15728880)
				.normal(normal.x, normal.y, normal.z)
				.endVertex();
	}

	private static int encode(float value) {
		return Math.round(Math.max(0.0F, Math.min(1.0F, value)) * 255.0F);
	}
}
