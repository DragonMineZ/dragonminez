package com.dragonminez.client.render.hair;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairJointStyle;
import com.dragonminez.common.hair.StrandPose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class HairMeshBuilder {
	private static final float BASE_OVERLAP = 0.015f;
	private static final float OFFSET_EPSILON = 1.0e-5f;

	public interface HighlightSource {
		float intensity(CustomHair.HairFace face, int index, int segment);
	}

	public interface VisibilityFilter {
		boolean isHidden(CustomHair.HairFace face, int index);
	}

	private Quaternionf[] rotations = new Quaternionf[0];
	private Vector3f[] starts = new Vector3f[0];
	private Vector3f[] ends = new Vector3f[0];

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

	private VertexConsumer buffer;
	private Matrix4f pose;
	private Matrix3f normalMatrix;
	private int light;
	private int overlay;
	private float red;
	private float green;
	private float blue;
	private float alpha;

	public void emit(VertexConsumer buffer, Matrix4f pose, Matrix3f normalMatrix, HairEntityState state, boolean useSimulation,
					 int globalFrom, int globalTo, boolean forceFrom, boolean forceTo, int packedLight, int packedOverlay, float baseAlpha,
					 HighlightSource highlight, VisibilityFilter visibility, HairPickRecorder recorder) {
		this.buffer = buffer;
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
		}

		for (int k = 0; k < segments; k++) {
			float intensity = highlight != null ? highlight.intensity(strand.face, strand.index, k) : 0.0f;
			int rgb = HairHighlight.applyColor(strand.resolveColor(k, globalFrom, globalTo, forceFrom, forceTo), intensity);
			red = ((rgb >> 16) & 0xFF) / 255.0f;
			green = ((rgb >> 8) & 0xFF) / 255.0f;
			blue = (rgb & 0xFF) / 255.0f;
			light = HairHighlight.applyLight(packedLight, intensity);

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

		quad(box[0], box[1], box[2], box[3], normal.set(axisY).negate());
		corner(box[4], origin, -halfWidth, top, -halfDepth);
		corner(box[5], origin, halfWidth, top, -halfDepth);
		corner(box[6], origin, halfWidth, top, halfDepth);
		corner(box[7], origin, -halfWidth, top, halfDepth);

		quad(box[7], box[6], box[5], box[4], normal.set(axisY));
		quad(box[0], box[4], box[5], box[1], normal.set(axisZ).negate());
		quad(box[2], box[6], box[7], box[3], normal.set(axisZ));
		quad(box[1], box[5], box[6], box[2], normal.set(axisX));
		quad(box[3], box[7], box[4], box[0], normal.set(axisX).negate());
	}

	private void emitConnected(StrandPose strand, int k) {
		int segments = strand.segments;
		boolean joinedBelow = k > 0 && isOffsetZero(strand, k);
		boolean joinedAbove = k < segments - 1 && isOffsetZero(strand, k + 1);

		if (joinedBelow) {
			rotations[k - 1].slerp(rotations[k], 0.5f, ringRotation);
			ring(bottomRing, starts[k], ringRotation, (strand.widths[k - 1] + strand.widths[k]) * 0.25f, (strand.depths[k - 1] + strand.depths[k]) * 0.25f);
		} else {
			ring(bottomRing, starts[k], rotations[k], strand.widths[k] * 0.5f, strand.depths[k] * 0.5f);
			rotations[k].transform(normal.set(0.0f, -1.0f, 0.0f));
			quad(bottomRing[0], bottomRing[1], bottomRing[2], bottomRing[3], normal);
		}

		if (joinedAbove) {
			rotations[k].slerp(rotations[k + 1], 0.5f, ringRotation);
			ring(topRing, ends[k], ringRotation, (strand.widths[k] + strand.widths[k + 1]) * 0.25f, (strand.depths[k] + strand.depths[k + 1]) * 0.25f);
		} else {
			ring(topRing, ends[k], rotations[k], strand.widths[k] * 0.5f, strand.depths[k] * 0.5f);
			rotations[k].transform(normal.set(0.0f, 1.0f, 0.0f));
			quad(topRing[3], topRing[2], topRing[1], topRing[0], normal);
		}

		center.set(starts[k]).add(ends[k]).mul(0.5f);
		for (int side = 0; side < 4; side++) {
			int nextSide = (side + 1) % 4;
			faceNormal(bottomRing[side], bottomRing[nextSide], topRing[nextSide], topRing[side]);
			quad(bottomRing[side], bottomRing[nextSide], topRing[nextSide], topRing[side], normal);
		}
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

	private void quad(Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector3f faceNormal) {
		if (faceNormal != normal) normal.set(faceNormal);
		vertex(a, 0.0f, 0.0f);
		vertex(b, 0.0f, 1.0f);
		vertex(c, 1.0f, 1.0f);
		vertex(d, 1.0f, 0.0f);
	}

	private void vertex(Vector3f position, float u, float v) {
		buffer.vertex(pose, position.x, position.y, position.z)
				.color(red, green, blue, alpha)
				.uv(u, v)
				.overlayCoords(overlay)
				.uv2(light)
				.normal(normalMatrix, normal.x, normal.y, normal.z)
				.endVertex();
	}

	private static boolean isOffsetZero(StrandPose strand, int segment) {
		return strand.localOffsets[segment].lengthSquared() < OFFSET_EPSILON * OFFSET_EPSILON;
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
