package com.dragonminez.client.render.effects;

import com.dragonminez.client.render.shader.EffectBloomRenderer;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

import java.util.ArrayList;
import java.util.List;

public final class SolarisSmokeRenderer {
	private static final float PX = 1.0f / 16.0f;
	private static final float RIBBON_MARGIN = 1.6f;
	private static final float RADIUS_SCALE = 8.0f;
	private static final int CURVE_SAMPLES = 8;

	private static final float[][] PLUME = {
			{4.5f, 25.0f, 2.0f}, {8.5f, 28.5f, 4.0f}, {10.5f, 33.0f, 5.0f}, {8.5f, 37.5f, 5.5f},
			{9.5f, 41.5f, 6.0f}, {13.5f, 43.0f, 6.0f}, {15.5f, 40.0f, 6.0f}, {13.5f, 38.5f, 6.0f}
	};
	private static final float PLUME_RADIUS_START = 3.0f;
	private static final float PLUME_RADIUS_END = 1.4f;

	private static final float[][] WISP = {
			{-5.0f, 25.0f, 2.0f}, {-9.0f, 27.5f, 3.5f}, {-10.5f, 31.0f, 4.0f}, {-9.0f, 33.5f, 4.5f}
	};
	private static final float WISP_RADIUS_START = 2.0f;
	private static final float WISP_RADIUS_END = 1.0f;

	private static final float COLLAR_Y = 24.6f;
	private static final float COLLAR_Z = 0.5f;
	private static final float COLLAR_RADIUS_X = 7.0f;
	private static final float COLLAR_RADIUS_Z = 4.2f;
	private static final float COLLAR_THICKNESS = 2.0f;
	private static final float COLLAR_SHOULDER_BULGE = 0.5f;
	private static final int COLLAR_SAMPLES = 48;

	private static final float WRIST_LIFT = 1.4f;
	private static final float WRIST_GAP = 0.9f;
	private static final float WRIST_THICKNESS = 1.15f;
	private static final int WRIST_SAMPLES = 28;
	private static final float[][] WRIST_WISP = {{2.2f, 0.4f, 0.0f}, {3.4f, 2.2f, 0.5f}, {2.8f, 4.2f, 1.0f}, {3.8f, 5.8f, 1.2f}};
	private static final float WRIST_WISP_RADIUS_START = 0.75f;
	private static final float WRIST_WISP_RADIUS_END = 0.35f;

	private static final float SWAY_AMOUNT = 0.6f;
	private static final float SWAY_SPEED = 0.08f;

	private SolarisSmokeRenderer() {}

	public static void renderBody(PoseStack poseStack, MultiBufferSource bufferSource, int entityId, float ageInTicks, boolean bloom) {
		Matrix4f pose = new Matrix4f(poseStack.last().pose());
		float seed = seed(entityId);
		List<Ribbon> ribbons = new ArrayList<>();
		ribbons.add(collar(seed));
		ribbons.add(path(PLUME, PLUME_RADIUS_START, PLUME_RADIUS_END, seed + 0.31f, ageInTicks));
		ribbons.add(path(WISP, WISP_RADIUS_START, WISP_RADIUS_END, seed + 0.57f, ageInTicks));
		draw(pose, ribbons, bufferSource, bloom);
	}

	public static void renderWrist(PoseStack poseStack, GeoBone armBone, MultiBufferSource bufferSource, int entityId, float ageInTicks, boolean bloom) {
		float[] bounds = boneBounds(armBone);
		if (bounds == null) return;
		Matrix4f pose = new Matrix4f(poseStack.last().pose());
		float cx = (bounds[0] + bounds[1]) * 0.5f / PX;
		float cz = (bounds[4] + bounds[5]) * 0.5f / PX;
		float y = bounds[2] / PX + WRIST_LIFT;
		float rx = (bounds[1] - bounds[0]) * 0.5f / PX + WRIST_GAP;
		float rz = (bounds[5] - bounds[4]) * 0.5f / PX + WRIST_GAP;
		float outward = cx >= 0.0f ? 1.0f : -1.0f;
		float seed = seed(entityId) + (outward > 0 ? 0.13f : 0.71f);

		List<Ribbon> ribbons = new ArrayList<>();
		ribbons.add(ring(cx, y, cz, rx, rz, WRIST_THICKNESS, outward > 0 ? (float) Math.PI : 0.0f, WRIST_SAMPLES, seed));
		float[][] wisp = new float[WRIST_WISP.length][];
		for (int i = 0; i < WRIST_WISP.length; i++) {
			wisp[i] = new float[]{cx + outward * (rx - WRIST_GAP + WRIST_WISP[i][0]), y + WRIST_WISP[i][1], cz + WRIST_WISP[i][2]};
		}
		ribbons.add(path(wisp, WRIST_WISP_RADIUS_START, WRIST_WISP_RADIUS_END, seed + 0.4f, ageInTicks));
		draw(pose, ribbons, bufferSource, bloom);
	}

	private static void draw(Matrix4f pose, List<Ribbon> ribbons, MultiBufferSource bufferSource, boolean bloom) {
		Vector3f camera = new Matrix4f(pose).invert().transformPosition(new Vector3f());
		VertexConsumer buffer = bufferSource.getBuffer(ModRenderTypes.solarisSmoke());
		for (Ribbon ribbon : ribbons) emit(buffer, pose, camera, ribbon);
		if (bloom) {
			EffectBloomRenderer.captureEntity(source -> {
				VertexConsumer vc = source.getBuffer(ModRenderTypes.solarisSmoke());
				for (Ribbon ribbon : ribbons) emit(vc, pose, camera, ribbon);
			});
		}
	}

	private static float seed(int entityId) {
		return Mth.frac(entityId * 0.618034f);
	}

	private record Ribbon(List<Vector3f> points, List<Float> radii, boolean closed, float seed) {}

	private static Ribbon collar(float seed) {
		List<Vector3f> points = new ArrayList<>();
		List<Float> radii = new ArrayList<>();
		for (int i = 0; i < COLLAR_SAMPLES; i++) {
			double a = Math.PI * 0.5 + 2 * Math.PI * i / COLLAR_SAMPLES;
			float x = (float) Math.cos(a) * COLLAR_RADIUS_X;
			float z = COLLAR_Z + (float) Math.sin(a) * COLLAR_RADIUS_Z;
			points.add(new Vector3f(x * PX, COLLAR_Y * PX, z * PX));
			radii.add(COLLAR_THICKNESS * (1.0f + COLLAR_SHOULDER_BULGE * (float) Math.abs(Math.cos(a))));
		}
		return new Ribbon(points, radii, true, seed);
	}

	private static Ribbon ring(float cx, float y, float cz, float rx, float rz, float thickness, float startAngle, int samples, float seed) {
		List<Vector3f> points = new ArrayList<>();
		List<Float> radii = new ArrayList<>();
		for (int i = 0; i < samples; i++) {
			double a = startAngle + 2 * Math.PI * i / samples;
			points.add(new Vector3f((cx + (float) Math.cos(a) * rx) * PX, y * PX, (cz + (float) Math.sin(a) * rz) * PX));
			radii.add(thickness);
		}
		return new Ribbon(points, radii, true, seed);
	}

	private static Ribbon path(float[][] control, float r0, float r1, float seed, float ageInTicks) {
		List<Vector3f> points = new ArrayList<>();
		for (int i = 0; i < control.length - 1; i++) {
			float[] p0 = control[Math.max(0, i - 1)], p1 = control[i], p2 = control[i + 1], p3 = control[Math.min(control.length - 1, i + 2)];
			for (int s = 0; s < CURVE_SAMPLES; s++) {
				float t = s / (float) CURVE_SAMPLES;
				points.add(catmull(p0, p1, p2, p3, t, i + t, ageInTicks));
			}
		}
		float[] last = control[control.length - 1];
		points.add(catmull(last, last, last, last, 0.0f, control.length - 1, ageInTicks));

		float total = 0.0f;
		float[] lengths = new float[points.size()];
		for (int i = 1; i < points.size(); i++) {
			total += points.get(i).distance(points.get(i - 1));
			lengths[i] = total;
		}
		List<Float> radii = new ArrayList<>();
		for (int i = 0; i < points.size(); i++) {
			float f = total > 0.0f ? lengths[i] / total : 0.0f;
			radii.add(Mth.lerp(f, r0, r1));
		}
		return new Ribbon(points, radii, false, seed);
	}

	private static Vector3f catmull(float[] p0, float[] p1, float[] p2, float[] p3, float t, float index, float ageInTicks) {
		float t2 = t * t, t3 = t2 * t;
		float[] out = new float[3];
		for (int k = 0; k < 3; k++) {
			out[k] = 0.5f * ((2 * p1[k]) + (-p0[k] + p2[k]) * t + (2 * p0[k] - 5 * p1[k] + 4 * p2[k] - p3[k]) * t2 + (-p0[k] + 3 * p1[k] - 3 * p2[k] + p3[k]) * t3);
		}
		float sway = SWAY_AMOUNT * Math.min(1.0f, index / 2.0f);
		out[0] += Mth.sin(ageInTicks * SWAY_SPEED + index * 0.9f) * sway;
		out[2] += Mth.cos(ageInTicks * SWAY_SPEED * 0.8f + index * 0.7f) * sway;
		return new Vector3f(out[0] * PX, out[1] * PX, out[2] * PX);
	}

	private static void emit(VertexConsumer buffer, Matrix4f pose, Vector3f camera, Ribbon ribbon) {
		List<Vector3f> pts = ribbon.points();
		int n = pts.size();
		if (n < 2) return;
		int count = ribbon.closed() ? n + 1 : n;
		Vector3f[] left = new Vector3f[count], right = new Vector3f[count];
		float[] u = new float[count], along = new float[count], radius = new float[count];
		float total = 0.0f;
		for (int i = 0; i < count; i++) {
			int idx = i % n;
			Vector3f p = pts.get(idx);
			if (i > 0) total += p.distance(pts.get((i - 1) % n)) / PX;
			u[i] = total;
			Vector3f prev = pts.get(ribbon.closed() ? (idx - 1 + n) % n : Math.max(0, idx - 1));
			Vector3f next = pts.get(ribbon.closed() ? (idx + 1) % n : Math.min(n - 1, idx + 1));
			Vector3f tangent = new Vector3f(next).sub(prev);
			Vector3f toCamera = new Vector3f(camera).sub(p);
			Vector3f side = tangent.cross(toCamera, new Vector3f());
			if (side.lengthSquared() < 1.0E-12f) side.set(0, 1, 0);
			side.normalize(ribbon.radii().get(idx) * RIBBON_MARGIN * PX);
			left[i] = new Vector3f(p).sub(side);
			right[i] = new Vector3f(p).add(side);
			radius[i] = ribbon.radii().get(idx);
		}
		for (int i = 0; i < count; i++) along[i] = total > 0.0f ? u[i] / total : 0.0f;

		float closed = ribbon.closed() ? 1.0f : 0.0f;
		for (int i = 0; i < count - 1; i++) {
			vertex(buffer, pose, left[i], u[i], -RIBBON_MARGIN, radius[i], ribbon.seed(), along[i], closed);
			vertex(buffer, pose, right[i], u[i], RIBBON_MARGIN, radius[i], ribbon.seed(), along[i], closed);
			vertex(buffer, pose, right[i + 1], u[i + 1], RIBBON_MARGIN, radius[i + 1], ribbon.seed(), along[i + 1], closed);
			vertex(buffer, pose, left[i + 1], u[i + 1], -RIBBON_MARGIN, radius[i + 1], ribbon.seed(), along[i + 1], closed);
		}
	}

	private static void vertex(VertexConsumer buffer, Matrix4f pose, Vector3f p, float u, float v, float radius, float seed, float along, float closed) {
		buffer.addVertex(pose, p.x(), p.y(), p.z())
				.setColor(radius / RADIUS_SCALE, seed, along, closed)
				.setUv(u, v)
				;
	}

	private static float[] boneBounds(GeoBone bone) {
		float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (GeoCube cube : bone.getCubes()) {
			for (GeoQuad quad : cube.quads()) {
				if (quad == null) continue;
				for (GeoVertex vertex : quad.vertices()) {
					Vector3f p = vertex.position();
					minX = Math.min(minX, p.x()); maxX = Math.max(maxX, p.x());
					minY = Math.min(minY, p.y()); maxY = Math.max(maxY, p.y());
					minZ = Math.min(minZ, p.z()); maxZ = Math.max(maxZ, p.z());
				}
			}
		}
		if (minX == Float.MAX_VALUE) return null;
		return new float[]{minX, maxX, minY, maxY, minZ, maxZ};
	}
}
