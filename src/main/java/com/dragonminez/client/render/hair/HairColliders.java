package com.dragonminez.client.render.hair;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

import java.util.Map;
import java.util.WeakHashMap;

public final class HairColliders {
	private static final Map<GeoBone, float[]> BOUNDS_CACHE = new WeakHashMap<>();

	private final Box head = new Box();
	private final Box body = new Box();
	private final Vector3f local = new Vector3f();

	public static float[] boundsOf(GeoBone bone) {
		return BOUNDS_CACHE.computeIfAbsent(bone, HairColliders::computeBounds);
	}

	private static float[] computeBounds(GeoBone bone) {
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		float maxZ = -Float.MAX_VALUE;
		boolean found = false;
		for (GeoCube cube : bone.getCubes()) {
			for (GeoQuad quad : cube.quads()) {
				if (quad == null) continue;
				for (GeoVertex vertex : quad.vertices()) {
					Vector3f position = vertex.position();
					minX = Math.min(minX, position.x);
					minY = Math.min(minY, position.y);
					minZ = Math.min(minZ, position.z);
					maxX = Math.max(maxX, position.x);
					maxY = Math.max(maxY, position.y);
					maxZ = Math.max(maxZ, position.z);
					found = true;
				}
			}
		}
		return found ? new float[]{minX, minY, minZ, maxX, maxY, maxZ} : null;
	}

	public void setHead(Matrix4f boneToAnchor, float[] bounds) {
		head.set(boneToAnchor, bounds);
	}

	public void setBody(Matrix4f boneToAnchor, float[] bounds) {
		body.set(boneToAnchor, bounds);
	}

	public boolean isInsideHead(Vector3f point, float margin) {
		return head.distance(point, local) < margin;
	}

	public void pushOutOfHead(Vector3f point, float margin) {
		head.pushOut(point, margin, local);
	}

	public void pushOutOfBody(Vector3f point, float margin) {
		body.pushOut(point, margin, local);
	}

	private static final class Box {
		private final Matrix4f toWorld = new Matrix4f();
		private final Matrix4f toLocal = new Matrix4f();
		private final Vector3f center = new Vector3f();
		private final Vector3f half = new Vector3f();
		private float scale = 1.0f;
		private boolean active;

		private void set(Matrix4f boneToAnchor, float[] bounds) {
			if (bounds == null) {
				active = false;
				return;
			}
			toWorld.set(boneToAnchor);
			boneToAnchor.invert(toLocal);
			center.set((bounds[0] + bounds[3]) * 0.5f, (bounds[1] + bounds[4]) * 0.5f, (bounds[2] + bounds[5]) * 0.5f);
			half.set((bounds[3] - bounds[0]) * 0.5f, (bounds[4] - bounds[1]) * 0.5f, (bounds[5] - bounds[2]) * 0.5f);
			Vector3f scales = boneToAnchor.getScale(new Vector3f());
			scale = Math.max(1.0e-3f, (scales.x + scales.y + scales.z) / 3.0f);
			active = true;
		}

		private float distance(Vector3f point, Vector3f scratch) {
			if (!active) return Float.MAX_VALUE;
			toLocal.transformPosition(point, scratch).sub(center);
			float qx = Math.abs(scratch.x) - half.x;
			float qy = Math.abs(scratch.y) - half.y;
			float qz = Math.abs(scratch.z) - half.z;
			float outside = (float) Math.sqrt(Math.max(qx, 0.0f) * Math.max(qx, 0.0f) + Math.max(qy, 0.0f) * Math.max(qy, 0.0f) + Math.max(qz, 0.0f) * Math.max(qz, 0.0f));
			float inside = Math.min(Math.max(qx, Math.max(qy, qz)), 0.0f);
			return (outside + inside) * scale;
		}

		private void pushOut(Vector3f point, float margin, Vector3f scratch) {
			if (!active) return;
			float localMargin = margin / scale;
			toLocal.transformPosition(point, scratch).sub(center);
			float cx = clamp(scratch.x, -half.x, half.x);
			float cy = clamp(scratch.y, -half.y, half.y);
			float cz = clamp(scratch.z, -half.z, half.z);
			float dx = scratch.x - cx;
			float dy = scratch.y - cy;
			float dz = scratch.z - cz;
			float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

			if (distance > 1.0e-6f) {
				if (distance >= localMargin) return;
				float factor = localMargin / distance;
				scratch.set(cx + dx * factor, cy + dy * factor, cz + dz * factor);
			} else {
				float penetrationX = half.x - Math.abs(scratch.x);
				float penetrationY = half.y - Math.abs(scratch.y);
				float penetrationZ = half.z - Math.abs(scratch.z);
				if (penetrationX <= penetrationY && penetrationX <= penetrationZ) {
					scratch.x = Math.copySign(half.x + localMargin, scratch.x);
				} else if (penetrationY <= penetrationZ) {
					scratch.y = Math.copySign(half.y + localMargin, scratch.y);
				} else {
					scratch.z = Math.copySign(half.z + localMargin, scratch.z);
				}
			}
			toWorld.transformPosition(scratch.add(center), point);
		}

		private static float clamp(float value, float min, float max) {
			return value < min ? min : Math.min(value, max);
		}
	}
}
