package com.dragonminez.client.render.hair;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairMath;
import com.dragonminez.common.hair.HairPoseBuilder;
import com.dragonminez.common.hair.HairStrand;
import com.dragonminez.common.hair.StrandPose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

public final class HairOcclusion {
	public static final int CORNERS = 4;

	private static final float HEAD_MIN_X = -4.0f;
	private static final float HEAD_MAX_X = 4.0f;
	private static final float HEAD_MIN_Y = 0.0f;
	private static final float HEAD_MAX_Y = 8.0f;
	private static final float HEAD_MIN_Z = -4.0f;
	private static final float HEAD_MAX_Z = 4.0f;
	private static final float HEAD_RANGE = 3.0f;
	private static final float OCCLUDER_RANGE = 3.5f;
	private static final float SPHERE_SPACING = 1.5f;
	private static final float SAMPLE_OFFSET = 0.05f;
	private static final float PIXELS_PER_UNIT = 1.0f / HairMath.PIXEL;
	private static final float[] CORNER_X = {-1.0f, 1.0f, 1.0f, -1.0f};
	private static final float[] CORNER_Z = {-1.0f, -1.0f, 1.0f, 1.0f};

	private static final Map<CustomHair, Shading> CACHE = new WeakHashMap<>();
	private static final Workspace WORKSPACE = new Workspace();

	private HairOcclusion() {}

	public static Shading of(CustomHair hair) {
		Shading cached = CACHE.get(hair);
		if (cached != null && cached.revision == hair.getRevision()) return cached;
		Shading computed = WORKSPACE.compute(hair);
		CACHE.put(hair, computed);
		return computed;
	}

	public static final class Shading {
		private final long revision;
		private final float[][] params = new float[HairEntityState.STRAND_COUNT][];
		private final float[][] occlusion = new float[HairEntityState.STRAND_COUNT][];

		private Shading(long revision) {
			this.revision = revision;
		}

		public boolean has(int flat) {
			return params[flat] != null;
		}

		public void sample(int flat, float param, float[] out, int outOffset) {
			float[] ringParams = params[flat];
			float[] values = occlusion[flat];
			int last = ringParams.length - 1;
			int ring = 0;
			while (ring < last - 1 && ringParams[ring + 1] < param) ring++;
			float span = ringParams[ring + 1] - ringParams[ring];
			float fraction = span > 1.0e-6f ? HairMath.clamp((param - ringParams[ring]) / span, 0.0f, 1.0f) : 0.0f;
			int a = ring * CORNERS;
			int b = a + CORNERS;
			for (int corner = 0; corner < CORNERS; corner++) {
				out[outOffset + corner] = values[a + corner] + (values[b + corner] - values[a + corner]) * fraction;
			}
		}
	}

	public static void ringParams(StrandPose pose, float[] out) {
		float total = 0.0f;
		for (int k = 0; k < pose.segments; k++) total += pose.lengths[k];
		out[0] = 0.0f;
		float travelled = 0.0f;
		for (int k = 0; k < pose.segments; k++) {
			travelled += pose.lengths[k];
			out[k + 1] = total > 0.0f ? travelled / total : (float) (k + 1) / pose.segments;
		}
	}

	private static final class Workspace {
		private final StrandPose[] poses = new StrandPose[HairEntityState.STRAND_COUNT];
		private final boolean[] present = new boolean[HairEntityState.STRAND_COUNT];
		private final int[] occluders = new int[HairEntityState.STRAND_COUNT];
		private final int[] firstSphere = new int[HairEntityState.STRAND_COUNT];
		private final int[] lastSphere = new int[HairEntityState.STRAND_COUNT];
		private final float[] reachBounds = new float[HairEntityState.STRAND_COUNT * 6];
		private float[] spheres = new float[256 * 4];
		private int[] segmentsOf = new int[256];
		private int sphereCount;
		private int occluderCount;

		private final Quaternionf ringRotation = new Quaternionf();
		private final Vector3f ringCenter = new Vector3f();
		private final Vector3f end = new Vector3f();
		private final Vector3f axisX = new Vector3f();
		private final Vector3f axisZ = new Vector3f();
		private final Vector3f point = new Vector3f();
		private final Vector3f normal = new Vector3f();

		private Shading compute(CustomHair hair) {
			Shading shading = new Shading(hair.getRevision());
			collect(hair);
			for (CustomHair.HairFace face : CustomHair.HairFace.values()) {
				for (int index = 0; index < face.maxStrands; index++) {
					int flat = HairEntityState.flatIndex(face, index);
					if (!present[flat]) continue;
					StrandPose pose = poses[flat];
					int rings = pose.segments + 1;
					float[] params = new float[rings];
					float[] values = new float[rings * CORNERS];
					ringParams(pose, params);
					for (int ring = 0; ring < rings; ring++) shadeRing(flat, pose, ring, values);
					shading.params[flat] = params;
					shading.occlusion[flat] = values;
				}
			}
			return shading;
		}

		private void collect(CustomHair hair) {
			sphereCount = 0;
			occluderCount = 0;
			for (CustomHair.HairFace face : CustomHair.HairFace.values()) {
				for (int index = 0; index < face.maxStrands; index++) {
					int flat = HairEntityState.flatIndex(face, index);
					HairStrand strand = hair.getStrand(face, index);
					present[flat] = strand != null && strand.isVisible();
					if (!present[flat]) continue;
					StrandPose pose = poses[flat];
					if (pose == null) pose = poses[flat] = new StrandPose();
					HairPoseBuilder.buildGeometry(strand, face, index, pose);
					if (pose.segments <= 0) {
						present[flat] = false;
						continue;
					}
					firstSphere[flat] = sphereCount;
					for (int k = 0; k < pose.segments; k++) addSegment(k, pose);
					lastSphere[flat] = sphereCount;
					computeReach(flat);
					occluders[occluderCount++] = flat;
				}
			}
		}

		private void addSegment(int segment, StrandPose pose) {
			Vector3f start = pose.restStarts[segment];
			pose.restEnd(segment, end);
			float radius = (float) Math.sqrt(pose.widths[segment] * pose.depths[segment] / Math.PI) * PIXELS_PER_UNIT;
			float length = start.distance(end) * PIXELS_PER_UNIT;
			int count = Math.max(1, Math.round(length / (radius * SPHERE_SPACING)));
			for (int i = 0; i < count; i++) {
				float fraction = (i + 0.5f) / count;
				ensureSpheres(sphereCount + 1);
				int base = sphereCount * 4;
				spheres[base] = (start.x + (end.x - start.x) * fraction) * PIXELS_PER_UNIT;
				spheres[base + 1] = (start.y + (end.y - start.y) * fraction) * PIXELS_PER_UNIT;
				spheres[base + 2] = (start.z + (end.z - start.z) * fraction) * PIXELS_PER_UNIT;
				spheres[base + 3] = radius;
				segmentsOf[sphereCount] = segment;
				sphereCount++;
			}
		}

		private void computeReach(int flat) {
			int bounds = flat * 6;
			reachBounds[bounds] = Float.MAX_VALUE;
			reachBounds[bounds + 1] = Float.MAX_VALUE;
			reachBounds[bounds + 2] = Float.MAX_VALUE;
			reachBounds[bounds + 3] = -Float.MAX_VALUE;
			reachBounds[bounds + 4] = -Float.MAX_VALUE;
			reachBounds[bounds + 5] = -Float.MAX_VALUE;
			for (int sphere = firstSphere[flat]; sphere < lastSphere[flat]; sphere++) {
				int base = sphere * 4;
				float reach = spheres[base + 3] * OCCLUDER_RANGE;
				for (int axis = 0; axis < 3; axis++) {
					reachBounds[bounds + axis] = Math.min(reachBounds[bounds + axis], spheres[base + axis] - reach);
					reachBounds[bounds + 3 + axis] = Math.max(reachBounds[bounds + 3 + axis], spheres[base + axis] + reach);
				}
			}
		}

		private boolean withinReach(int flat, Vector3f position) {
			int bounds = flat * 6;
			return position.x >= reachBounds[bounds] && position.x <= reachBounds[bounds + 3]
					&& position.y >= reachBounds[bounds + 1] && position.y <= reachBounds[bounds + 4]
					&& position.z >= reachBounds[bounds + 2] && position.z <= reachBounds[bounds + 5];
		}

		private void shadeRing(int flat, StrandPose pose, int ring, float[] out) {
			int segments = pose.segments;
			int below = Math.max(0, ring - 1);
			int above = Math.min(segments - 1, ring);
			if (ring == 0) {
				ringCenter.set(pose.restStarts[0]);
			} else if (ring == segments) {
				pose.restEnd(segments - 1, ringCenter);
			} else {
				pose.restEnd(below, ringCenter).add(pose.restStarts[above]).mul(0.5f);
			}
			pose.restRotations[below].slerp(pose.restRotations[above], below == above ? 0.0f : 0.5f, ringRotation);
			float halfWidth = (pose.widths[below] + pose.widths[above]) * 0.25f * PIXELS_PER_UNIT;
			float halfDepth = (pose.depths[below] + pose.depths[above]) * 0.25f * PIXELS_PER_UNIT;
			ringCenter.mul(PIXELS_PER_UNIT);
			ringRotation.transform(axisX.set(1.0f, 0.0f, 0.0f));
			ringRotation.transform(axisZ.set(0.0f, 0.0f, 1.0f));

			for (int corner = 0; corner < CORNERS; corner++) {
				float sx = CORNER_X[corner];
				float sz = CORNER_Z[corner];
				normal.set(axisX).mul(sx * halfDepth).add(axisZ.x * sz * halfWidth, axisZ.y * sz * halfWidth, axisZ.z * sz * halfWidth).normalize();
				point.set(ringCenter)
						.add(axisX.x * sx * halfWidth, axisX.y * sx * halfWidth, axisX.z * sx * halfWidth)
						.add(axisZ.x * sz * halfDepth, axisZ.y * sz * halfDepth, axisZ.z * sz * halfDepth)
						.add(normal.x * SAMPLE_OFFSET, normal.y * SAMPLE_OFFSET, normal.z * SAMPLE_OFFSET);
				float visibility = 1.0f - headOcclusion(point, normal);
				for (int i = 0; i < occluderCount && visibility > 0.0f; i++) {
					int occluder = occluders[i];
					if (!withinReach(occluder, point)) continue;
					boolean self = occluder == flat;
					for (int sphere = firstSphere[occluder]; sphere < lastSphere[occluder]; sphere++) {
						if (self && (segmentsOf[sphere] == below || segmentsOf[sphere] == above)) continue;
						visibility *= 1.0f - sphereOcclusion(point, normal, sphere);
					}
				}
				out[ring * CORNERS + corner] = HairMath.clamp(1.0f - visibility, 0.0f, 1.0f);
			}
		}

		private float sphereOcclusion(Vector3f position, Vector3f surfaceNormal, int sphere) {
			int base = sphere * 4;
			float radius = spheres[base + 3];
			float dx = spheres[base] - position.x;
			float dy = spheres[base + 1] - position.y;
			float dz = spheres[base + 2] - position.z;
			float distanceSquared = dx * dx + dy * dy + dz * dz;
			float reach = radius * OCCLUDER_RANGE;
			if (distanceSquared > reach * reach) return 0.0f;
			float distance = (float) Math.sqrt(distanceSquared);
			if (distance <= radius) return 1.0f;
			float cosine = (surfaceNormal.x * dx + surfaceNormal.y * dy + surfaceNormal.z * dz) / distance;
			float ratio = distance / radius;
			float ratioSquared = ratio * ratio;
			float result;
			if (ratioSquared * cosine * cosine < 1.0f) {
				float crossing = (cosine * ratio + 1.0f) / ratioSquared;
				result = 0.33f * crossing * crossing;
			} else {
				result = Math.max(0.0f, cosine) / ratioSquared;
			}
			float falloff = 1.0f - HairMath.smoothstep((distance - radius) / (reach - radius));
			return HairMath.clamp(result * falloff, 0.0f, 1.0f);
		}

		private static float headOcclusion(Vector3f position, Vector3f surfaceNormal) {
			float cx = HairMath.clamp(position.x, HEAD_MIN_X, HEAD_MAX_X);
			float cy = HairMath.clamp(position.y, HEAD_MIN_Y, HEAD_MAX_Y);
			float cz = HairMath.clamp(position.z, HEAD_MIN_Z, HEAD_MAX_Z);
			float dx = position.x - cx;
			float dy = position.y - cy;
			float dz = position.z - cz;
			float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			float nx;
			float ny;
			float nz;
			if (distance > 1.0e-4f) {
				nx = dx / distance;
				ny = dy / distance;
				nz = dz / distance;
			} else {
				distance = 0.0f;
				float toMinX = position.x - HEAD_MIN_X;
				float toMaxX = HEAD_MAX_X - position.x;
				float toMinY = position.y - HEAD_MIN_Y;
				float toMaxY = HEAD_MAX_Y - position.y;
				float toMinZ = position.z - HEAD_MIN_Z;
				float toMaxZ = HEAD_MAX_Z - position.z;
				float nearest = Math.min(Math.min(Math.min(toMinX, toMaxX), Math.min(toMinY, toMaxY)), Math.min(toMinZ, toMaxZ));
				nx = nearest == toMinX ? -1.0f : (nearest == toMaxX ? 1.0f : 0.0f);
				ny = nx != 0.0f ? 0.0f : (nearest == toMinY ? -1.0f : (nearest == toMaxY ? 1.0f : 0.0f));
				nz = nx != 0.0f || ny != 0.0f ? 0.0f : (nearest == toMinZ ? -1.0f : 1.0f);
			}
			float facing = surfaceNormal.x * nx + surfaceNormal.y * ny + surfaceNormal.z * nz;
			return 0.5f * (1.0f - facing) * (float) Math.exp(-distance / HEAD_RANGE);
		}

		private void ensureSpheres(int count) {
			if (segmentsOf.length >= count) return;
			int capacity = Math.max(count, segmentsOf.length * 2);
			float[] grownSpheres = new float[capacity * 4];
			int[] grownSegments = new int[capacity];
			System.arraycopy(spheres, 0, grownSpheres, 0, sphereCount * 4);
			System.arraycopy(segmentsOf, 0, grownSegments, 0, sphereCount);
			spheres = grownSpheres;
			segmentsOf = grownSegments;
		}
	}
}
