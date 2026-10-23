package com.dragonminez.client.render.hair;

import com.dragonminez.common.hair.CustomHair;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class HairPickRecorder {
	private static final float RAY_DEPTH = 4000.0f;

	private final List<Entry> entries = new ArrayList<>();
	private final Matrix4f headMatrix = new Matrix4f();
	private final Matrix4f inverseHead = new Matrix4f();
	private int used;
	private boolean hasMatrix;

	public void begin(Matrix4f head) {
		headMatrix.set(head);
		head.invert(inverseHead);
		used = 0;
		hasMatrix = true;
	}

	public boolean hasFrame() {
		return hasMatrix;
	}

	public Matrix4f getHeadMatrix() {
		return headMatrix;
	}

	public void record(CustomHair.HairFace face, int index, int segment, Vector3f start, Quaternionf rotation,
					   float halfWidth, float bottom, float top, float halfDepth) {
		if (used == entries.size()) entries.add(new Entry());
		Entry entry = entries.get(used++);
		entry.face = face;
		entry.index = index;
		entry.segment = segment;
		entry.start.set(start);
		rotation.conjugate(entry.inverseRotation);
		entry.halfWidth = halfWidth;
		entry.bottom = bottom;
		entry.top = top;
		entry.halfDepth = halfDepth;
	}

	public void rayOrigin(double guiX, double guiY, Vector3f outOrigin, Vector3f outDirection) {
		inverseHead.transformPosition((float) guiX, (float) guiY, RAY_DEPTH, outOrigin);
		inverseHead.transformPosition((float) guiX, (float) guiY, -RAY_DEPTH, outDirection);
		outDirection.sub(outOrigin);
	}

	public Vector3f viewDirection(Vector3f out) {
		return inverseHead.transformDirection(out.set(0.0f, 0.0f, -1.0f)).normalize();
	}

	public Hit pick(double guiX, double guiY, HitFilter filter) {
		if (!hasMatrix || used == 0) return null;
		Vector3f origin = new Vector3f();
		Vector3f direction = new Vector3f();
		rayOrigin(guiX, guiY, origin, direction);

		Vector3f localOrigin = new Vector3f();
		Vector3f localDirection = new Vector3f();
		Entry best = null;
		float bestT = Float.MAX_VALUE;
		for (int i = 0; i < used; i++) {
			Entry entry = entries.get(i);
			if (filter != null && !filter.accept(entry.face, entry.index)) continue;
			entry.inverseRotation.transform(localOrigin.set(origin).sub(entry.start));
			entry.inverseRotation.transform(localDirection.set(direction));
			float t = intersect(localOrigin, localDirection, entry);
			if (t >= 0.0f && t < bestT) {
				bestT = t;
				best = entry;
			}
		}
		if (best == null) return null;
		Vector3f point = new Vector3f(direction).mul(bestT).add(origin);
		return new Hit(best.face, best.index, best.segment, point);
	}

	private static float intersect(Vector3f origin, Vector3f direction, Entry box) {
		float tMin = 0.0f;
		float tMax = 1.0f;
		float[] mins = {-box.halfWidth, box.bottom, -box.halfDepth};
		float[] maxs = {box.halfWidth, box.top, box.halfDepth};
		float[] o = {origin.x, origin.y, origin.z};
		float[] d = {direction.x, direction.y, direction.z};
		for (int axis = 0; axis < 3; axis++) {
			if (Math.abs(d[axis]) < 1.0e-9f) {
				if (o[axis] < mins[axis] || o[axis] > maxs[axis]) return -1.0f;
				continue;
			}
			float t1 = (mins[axis] - o[axis]) / d[axis];
			float t2 = (maxs[axis] - o[axis]) / d[axis];
			if (t1 > t2) {
				float swap = t1;
				t1 = t2;
				t2 = swap;
			}
			tMin = Math.max(tMin, t1);
			tMax = Math.min(tMax, t2);
			if (tMin > tMax) return -1.0f;
		}
		return tMin;
	}

	public interface HitFilter {
		boolean accept(CustomHair.HairFace face, int index);
	}

	public record Hit(CustomHair.HairFace face, int index, int segment, Vector3f headLocalPoint) {}

	private static final class Entry {
		private CustomHair.HairFace face;
		private int index;
		private int segment;
		private final Vector3f start = new Vector3f();
		private final Quaternionf inverseRotation = new Quaternionf();
		private float halfWidth;
		private float bottom;
		private float top;
		private float halfDepth;
	}
}
