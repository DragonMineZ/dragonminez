package com.dragonminez.client.render.hair;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairJointStyle;
import com.dragonminez.common.hair.HairMath;
import com.dragonminez.common.hair.StrandPose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class HairMeshBuilder {
	private static final float BASE_OVERLAP = 0.015f;
	private static final float OFFSET_EPSILON = 1.0e-5f;
	private static final float LENGTH_SHADE = 0.55f;
	private static final float OCCLUSION_SHADE = 1.3f;
	private static final float OCCLUSION_PIVOT = 0.12f;
	private static final float FACING_SHADE = 0.35f;
	private static final float FACING_PIVOT = 0.3f;
	private static final float SHADE_STEP = 0.5f;
	private static final float DARKEST_SHADE = -0.5f;
	private static final float COMB_JITTER = 0.22f;
	private static final float CELLS_PER_UNIT = 1.0f / HairMath.PIXEL;
	private static final float TEXTURE_SIZE = 16.0f;
	private static final float TEXEL_UV = 1.0f / TEXTURE_SIZE;
	private static final float CAP_U = 0.5f;
	private static final float CAP_V = 0.5f;
	private static final int CAP_SEED = 4;

	public interface HighlightSource {
		float intensity(CustomHair.HairFace face, int index, int segment);
	}

	public interface VisibilityFilter {
		boolean isHidden(CustomHair.HairFace face, int index);
	}

	private Quaternionf[] rotations = new Quaternionf[0];
	private Vector3f[] starts = new Vector3f[0];
	private Vector3f[] ends = new Vector3f[0];
	private float[] travelled = new float[0];

	private final HairColorRamp colorRamp = new HairColorRamp();
	private final Quaternionf ringRotation = new Quaternionf();
	private final Quaternionf inverse = new Quaternionf();
	private final Vector3f axisX = new Vector3f();
	private final Vector3f axisY = new Vector3f();
	private final Vector3f axisZ = new Vector3f();
	private final Vector3f localDirection = new Vector3f();
	private final Vector3f normal = new Vector3f();
	private final Vector3f edgeA = new Vector3f();
	private final Vector3f edgeB = new Vector3f();
	private final Vector3f center = new Vector3f();
	private final Vector3f[] bottomRing = createRing();
	private final Vector3f[] topRing = createRing();
	private final Vector3f[] box = createBox();
	private final Vector3f cell = new Vector3f();
	private final float[] cornerShades = new float[8];
	private final float[] faceCenters = new float[4];
	private final float[] rgb = new float[3];
	private int[] rowLevels = new int[16];

	private VertexConsumer buffer;
	private Matrix4f pose;
	private Matrix3f normalMatrix;
	private int light;
	private int overlay;
	private float alpha;
	private float strandV;
	private int ramp;
	private int strandSeed;
	private float highlightIntensity;
	private boolean detailed;

	public void emit(VertexConsumer buffer, Matrix4f pose, Matrix3f normalMatrix, HairEntityState state, boolean useSimulation,
					 int globalFrom, int globalTo, boolean forceFrom, boolean forceTo, int packedLight, int packedOverlay, float baseAlpha,
					 boolean pixelDetail, HighlightSource highlight, VisibilityFilter visibility, HairPickRecorder recorder) {
		this.buffer = buffer;
		this.detailed = pixelDetail;
		this.pose = pose;
		this.normalMatrix = normalMatrix;
		this.overlay = packedOverlay;
		this.alpha = baseAlpha;
		if (recorder != null) recorder.begin(pose);

		for (CustomHair.HairFace face : CustomHair.HairFace.values()) {
			for (int index = 0; index < face.maxStrands; index++) {
				int flat = HairEntityState.flatIndex(face, index);
				if (!state.isPresent(flat)) continue;
				if (visibility != null && visibility.isHidden(face, index)) continue;
				StrandPose strand = state.pose(flat);
				if (strand.segments <= 0) continue;
				emitStrand(strand, state, flat, useSimulation && state.hasSimulatedFrames(flat), globalFrom, globalTo, forceFrom, forceTo, packedLight, highlight, recorder);
			}
		}
		this.buffer = null;
	}

	private void emitStrand(StrandPose strand, HairEntityState state, int flat, boolean simulated, int globalFrom, int globalTo,
							boolean forceFrom, boolean forceTo, int packedLight, HighlightSource highlight, HairPickRecorder recorder) {
		int segments = strand.segments;
		ensureCapacity(segments);

		Vector3f cursor = center.set(strand.root);
		Quaternionf parent = strand.baseRotation;
		float length = 0.0f;
		for (int k = 0; k < segments; k++) {
			if (simulated) {
				state.renderRotation(flat, k, rotations[k]);
			} else {
				rotations[k].set(strand.restRotations[k]);
			}
			parent.transform(starts[k].set(strand.localOffsets[k])).add(cursor);
			rotations[k].transform(ends[k].set(0.0f, strand.lengths[k], 0.0f)).add(starts[k]);
			cursor.set(ends[k]);
			parent = rotations[k];
			travelled[k] = length;
			length += strand.lengths[k];
		}

		strandSeed = mix(strand.id);
		strandV = (strandSeed & 0xF) * TEXEL_UV;
		for (int side = 0; side < faceCenters.length; side++) faceCenters[side] = ((strandSeed >>> (8 + side * 4)) & 0xF) * TEXEL_UV;

		float[] params = state.ringParams(flat);
		float[] occlusion = state.ringOcclusion(flat);
		for (int k = 0; k < segments; k++) {
			highlightIntensity = highlight != null ? highlight.intensity(strand.face, strand.index, k) : 0.0f;
			ramp = colorRamp.resolve(strand.resolveColor(k, globalFrom, globalTo, forceFrom, forceTo));
			shadeRing(params, occlusion, k, 0);
			shadeRing(params, occlusion, k + 1, 4);
			light = HairHighlight.applyLight(packedLight, highlightIntensity);

			float halfWidth = strand.widths[k] * 0.5f;
			float halfDepth = strand.depths[k] * 0.5f;
			if (strand.jointStyle == HairJointStyle.CONNECTED) {
				emitConnected(strand, k);
			} else {
				emitBlock(strand, k);
			}
			if (recorder != null) recorder.record(strand.face, strand.index, k, starts[k], rotations[k], halfWidth, 0.0f, strand.lengths[k], halfDepth);
		}
	}

	private void shadeRing(float[] params, float[] occlusion, int ring, int firstCorner) {
		float lengthShade = LENGTH_SHADE * (2.0f * params[ring] - 1.0f);
		for (int corner = 0; corner < HairOcclusion.CORNERS; corner++) {
			cornerShades[firstCorner + corner] = lengthShade + OCCLUSION_SHADE * (OCCLUSION_PIVOT - occlusion[ring * HairOcclusion.CORNERS + corner]);
		}
	}

	private void emitBlock(StrandPose strand, int k) {
		Quaternionf rotation = rotations[k];
		rotation.transform(axisX.set(1.0f, 0.0f, 0.0f));
		rotation.transform(axisY.set(0.0f, 1.0f, 0.0f));
		rotation.transform(axisZ.set(0.0f, 0.0f, 1.0f));

		float halfWidth = strand.widths[k] * 0.5f;
		float halfDepth = strand.depths[k] * 0.5f;
		float bottom = 0.0f;
		if (k > 0 && isOffsetZero(strand, k)) {
			rotations[k - 1].conjugate(inverse).transform(localDirection.set(axisY));
			bottom = -(halfWidth * Math.abs(localDirection.x) + halfDepth * Math.abs(localDirection.z) + BASE_OVERLAP);
		}
		float top = strand.lengths[k];
		Vector3f origin = starts[k];

		corner(box[0], origin, -halfWidth, bottom, -halfDepth);
		corner(box[1], origin, halfWidth, bottom, -halfDepth);
		corner(box[2], origin, halfWidth, bottom, halfDepth);
		corner(box[3], origin, -halfWidth, bottom, halfDepth);
		corner(box[4], origin, -halfWidth, top, -halfDepth);
		corner(box[5], origin, halfWidth, top, -halfDepth);
		corner(box[6], origin, halfWidth, top, halfDepth);
		corner(box[7], origin, -halfWidth, top, halfDepth);

		float height = top - bottom;
		float vBase = strandV + Math.round((travelled[k] + bottom) * CELLS_PER_UNIT) * TEXEL_UV;
		normal.set(axisY).negate();
		pixelFace(box[0], box[1], box[2], box[3], 0, 1, 2, 3, halfDepth * 2.0f, halfWidth * 2.0f, CAP_SEED, CAP_U, CAP_V, k == 0);
		normal.set(axisY);
		pixelFace(box[7], box[6], box[5], box[4], 7, 6, 5, 4, halfDepth * 2.0f, halfWidth * 2.0f, CAP_SEED + 1, CAP_U, CAP_V, k == strand.segments - 1);
		normal.set(axisZ).negate();
		pixelFace(box[0], box[4], box[5], box[1], 0, 4, 5, 1, halfWidth * 2.0f, height, 0, faceCenters[0], vBase, true);
		normal.set(axisX);
		pixelFace(box[1], box[5], box[6], box[2], 1, 5, 6, 2, halfDepth * 2.0f, height, 1, faceCenters[1], vBase, true);
		normal.set(axisZ);
		pixelFace(box[2], box[6], box[7], box[3], 2, 6, 7, 3, halfWidth * 2.0f, height, 2, faceCenters[2], vBase, true);
		normal.set(axisX).negate();
		pixelFace(box[3], box[7], box[4], box[0], 3, 7, 4, 0, halfDepth * 2.0f, height, 3, faceCenters[3], vBase, true);
	}

	private void emitConnected(StrandPose strand, int k) {
		int segments = strand.segments;
		boolean joinedBelow = k > 0 && isOffsetZero(strand, k);
		boolean joinedAbove = k < segments - 1 && isOffsetZero(strand, k + 1);

		float bottomHalfWidth;
		float bottomHalfDepth;
		if (joinedBelow) {
			rotations[k - 1].slerp(rotations[k], 0.5f, ringRotation);
			bottomHalfWidth = (strand.widths[k - 1] + strand.widths[k]) * 0.25f;
			bottomHalfDepth = (strand.depths[k - 1] + strand.depths[k]) * 0.25f;
			ring(bottomRing, starts[k], ringRotation, bottomHalfWidth, bottomHalfDepth);
		} else {
			bottomHalfWidth = strand.widths[k] * 0.5f;
			bottomHalfDepth = strand.depths[k] * 0.5f;
			ring(bottomRing, starts[k], rotations[k], bottomHalfWidth, bottomHalfDepth);
			rotations[k].transform(normal.set(0.0f, -1.0f, 0.0f));
			pixelFace(bottomRing[0], bottomRing[1], bottomRing[2], bottomRing[3], 0, 1, 2, 3, bottomHalfDepth * 2.0f, bottomHalfWidth * 2.0f, CAP_SEED, CAP_U, CAP_V, k == 0);
		}

		float topHalfWidth;
		float topHalfDepth;
		if (joinedAbove) {
			rotations[k].slerp(rotations[k + 1], 0.5f, ringRotation);
			topHalfWidth = (strand.widths[k] + strand.widths[k + 1]) * 0.25f;
			topHalfDepth = (strand.depths[k] + strand.depths[k + 1]) * 0.25f;
			ring(topRing, ends[k], ringRotation, topHalfWidth, topHalfDepth);
		} else {
			topHalfWidth = strand.widths[k] * 0.5f;
			topHalfDepth = strand.depths[k] * 0.5f;
			ring(topRing, ends[k], rotations[k], topHalfWidth, topHalfDepth);
			rotations[k].transform(normal.set(0.0f, 1.0f, 0.0f));
			pixelFace(topRing[3], topRing[2], topRing[1], topRing[0], 7, 6, 5, 4, topHalfDepth * 2.0f, topHalfWidth * 2.0f, CAP_SEED + 1, CAP_U, CAP_V, k == segments - 1);
		}

		float vBase = strandV + Math.round(travelled[k] * CELLS_PER_UNIT) * TEXEL_UV;
		center.set(starts[k]).add(ends[k]).mul(0.5f);
		for (int side = 0; side < 4; side++) {
			int nextSide = (side + 1) % 4;
			boolean alongWidth = (side & 1) == 0;
			float span = alongWidth ? bottomHalfWidth + topHalfWidth : bottomHalfDepth + topHalfDepth;
			faceNormal(bottomRing[side], bottomRing[nextSide], topRing[nextSide], topRing[side]);
			pixelFace(bottomRing[side], topRing[side], topRing[nextSide], bottomRing[nextSide], side, 4 + side, 4 + nextSide, nextSide,
					span, strand.lengths[k], side, faceCenters[side], vBase, true);
		}
	}

	private void pixelFace(Vector3f bottomLeft, Vector3f topLeft, Vector3f topRight, Vector3f bottomRight,
						   int shadeBottomLeft, int shadeTopLeft, int shadeTopRight, int shadeBottomRight,
						   float width, float height, int seed, float uBase, float vBase, boolean visible) {
		boolean split = detailed && visible;
		int columns = split ? cells(width) : 1;
		int rows = split ? cells(height) : 1;
		ensureRows(rows);
		float facing = FACING_SHADE * (normal.y - FACING_PIVOT);
		for (int column = 0; column < columns; column++) {
			float across = (column + 0.5f) / columns;
			float bottomShade = HairMath.lerp(across, cornerShades[shadeBottomLeft], cornerShades[shadeBottomRight]);
			float topShade = HairMath.lerp(across, cornerShades[shadeTopLeft], cornerShades[shadeTopRight]);
			float offset = facing + (split ? COMB_JITTER * signedNoise(strandSeed + seed * 131 + column * 31) : 0.0f);
			for (int row = 0; row < rows; row++) {
				rowLevels[row] = level(HairMath.lerp((row + 0.5f) / rows, bottomShade, topShade) + offset);
			}
			int runStart = 0;
			for (int row = 1; row <= rows; row++) {
				if (row < rows && rowLevels[row] == rowLevels[runStart]) continue;
				emitCell(bottomLeft, topLeft, topRight, bottomRight, (float) column / columns, (float) (column + 1) / columns,
						(float) runStart / rows, (float) row / rows, rowLevels[runStart],
						uBase + column * TEXEL_UV, uBase + (column + 1) * TEXEL_UV, vBase + runStart * TEXEL_UV, vBase + row * TEXEL_UV);
				runStart = row;
			}
		}
	}

	private void emitCell(Vector3f bottomLeft, Vector3f topLeft, Vector3f topRight, Vector3f bottomRight, float across0, float across1,
						  float along0, float along1, int shadeLevel, float u0, float u1, float v0, float v1) {
		colorRamp.sample(ramp, shadeLevel * SHADE_STEP, rgb, 0);
		if (highlightIntensity > 0.0f) {
			rgb[0] = HairHighlight.applyColor(rgb[0], highlightIntensity);
			rgb[1] = HairHighlight.applyColor(rgb[1], highlightIntensity);
			rgb[2] = HairHighlight.applyColor(rgb[2], highlightIntensity);
		}
		vertex(interpolate(bottomLeft, topLeft, topRight, bottomRight, across0, along0), u0, v0);
		vertex(interpolate(bottomLeft, topLeft, topRight, bottomRight, across0, along1), u0, v1);
		vertex(interpolate(bottomLeft, topLeft, topRight, bottomRight, across1, along1), u1, v1);
		vertex(interpolate(bottomLeft, topLeft, topRight, bottomRight, across1, along0), u1, v0);
	}

	private Vector3f interpolate(Vector3f bottomLeft, Vector3f topLeft, Vector3f topRight, Vector3f bottomRight, float across, float along) {
		edgeA.set(bottomLeft).lerp(bottomRight, across);
		edgeB.set(topLeft).lerp(topRight, across);
		return cell.set(edgeA).lerp(edgeB, along);
	}

	private static int level(float shade) {
		return Math.round(HairMath.clamp(shade, DARKEST_SHADE, 1.0f) / SHADE_STEP);
	}

	private static int cells(float units) {
		return Math.max(1, Math.round(units * CELLS_PER_UNIT));
	}

	private static float signedNoise(int value) {
		return (mix(value) & 0xFFFF) / 32767.5f - 1.0f;
	}

	private void ensureRows(int rows) {
		if (rowLevels.length < rows) rowLevels = new int[Math.max(rows, rowLevels.length * 2)];
	}

	private void faceNormal(Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
		edgeA.set(c).sub(a);
		edgeB.set(d).sub(b);
		edgeA.cross(edgeB, normal);
		if (normal.lengthSquared() < 1.0e-12f) {
			normal.set(a).add(b).add(c).add(d).mul(0.25f).sub(center);
		}
		edgeB.set(a).add(b).add(c).add(d).mul(0.25f).sub(center);
		if (normal.dot(edgeB) < 0.0f) normal.negate();
		normal.normalize();
	}

	private void ring(Vector3f[] out, Vector3f origin, Quaternionf rotation, float halfWidth, float halfDepth) {
		rotation.transform(axisX.set(1.0f, 0.0f, 0.0f));
		rotation.transform(axisZ.set(0.0f, 0.0f, 1.0f));
		out[0].set(origin).add(-halfWidth * axisX.x - halfDepth * axisZ.x, -halfWidth * axisX.y - halfDepth * axisZ.y, -halfWidth * axisX.z - halfDepth * axisZ.z);
		out[1].set(origin).add(halfWidth * axisX.x - halfDepth * axisZ.x, halfWidth * axisX.y - halfDepth * axisZ.y, halfWidth * axisX.z - halfDepth * axisZ.z);
		out[2].set(origin).add(halfWidth * axisX.x + halfDepth * axisZ.x, halfWidth * axisX.y + halfDepth * axisZ.y, halfWidth * axisX.z + halfDepth * axisZ.z);
		out[3].set(origin).add(-halfWidth * axisX.x + halfDepth * axisZ.x, -halfWidth * axisX.y + halfDepth * axisZ.y, -halfWidth * axisX.z + halfDepth * axisZ.z);
	}

	private void corner(Vector3f out, Vector3f origin, float x, float y, float z) {
		out.set(origin)
				.add(axisX.x * x, axisX.y * x, axisX.z * x)
				.add(axisY.x * y, axisY.y * y, axisY.z * y)
				.add(axisZ.x * z, axisZ.y * z, axisZ.z * z);
	}

	private void vertex(Vector3f position, float u, float v) {
		buffer.vertex(pose, position.x, position.y, position.z)
				.color(rgb[0], rgb[1], rgb[2], alpha)
				.uv(u, v)
				.overlayCoords(overlay)
				.uv2(light)
				.normal(normalMatrix, normal.x, normal.y, normal.z)
				.endVertex();
	}

	private static boolean isOffsetZero(StrandPose strand, int segment) {
		return strand.localOffsets[segment].lengthSquared() < OFFSET_EPSILON * OFFSET_EPSILON;
	}

	private static int mix(int value) {
		int hash = value * 0x9E3779B1;
		hash ^= hash >>> 15;
		hash *= 0x85EBCA6B;
		return hash ^ (hash >>> 13);
	}

	private void ensureCapacity(int count) {
		if (rotations.length >= count) return;
		int capacity = Math.max(count, 8);
		Quaternionf[] newRotations = new Quaternionf[capacity];
		Vector3f[] newStarts = new Vector3f[capacity];
		Vector3f[] newEnds = new Vector3f[capacity];
		for (int i = 0; i < capacity; i++) {
			newRotations[i] = i < rotations.length ? rotations[i] : new Quaternionf();
			newStarts[i] = i < starts.length ? starts[i] : new Vector3f();
			newEnds[i] = i < ends.length ? ends[i] : new Vector3f();
		}
		rotations = newRotations;
		starts = newStarts;
		ends = newEnds;
		travelled = new float[capacity];
	}

	private static Vector3f[] createRing() {
		return new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
	}

	private static Vector3f[] createBox() {
		Vector3f[] corners = new Vector3f[8];
		for (int i = 0; i < corners.length; i++) corners[i] = new Vector3f();
		return corners;
	}
}
