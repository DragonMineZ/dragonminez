package com.dragonminez.common.hair;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class HairPoseBuilder {
	private HairPoseBuilder() {}

	public static void build(HairStrand strand, CustomHair.HairFace face, int index, HairStyleSlot slot, StrandPose out) {
		buildGeometry(strand, face, index, out);
		out.physics.copyFrom(HairPhysicsProfile.defaultFor(slot));
		out.strandColorFrom = HairColors.parse(strand.getColor());
		out.strandColorTo = out.strandColorFrom;
		out.tipColorFrom = HairColors.parse(strand.getTipColor());
		out.tipColorTo = out.tipColorFrom;
		fillSegmentColors(strand, out.segments, out.segmentColorsFrom);
		System.arraycopy(out.segmentColorsFrom, 0, out.segmentColorsTo, 0, out.segments);
		out.colorFactor = 0.0f;
	}

	public static void buildBlend(HairStrand from, boolean fromVisible, HairStrand to, boolean toVisible, float factor,
								  CustomHair.HairFace face, int index, HairStyleSlot fromSlot, HairStyleSlot toSlot,
								  StrandPose out, HairStrand scratch) {
		HairStrand a = fromVisible ? from : to;
		HairStrand b = toVisible ? to : from;

		scratch.clearOverrides();
		scratch.setSegments(Math.max(a.getSegments(), b.getSegments()));
		scratch.setLength(HairMath.lerp(factor, fromVisible ? a.getLength() : HairStrand.MIN_LENGTH, toVisible ? b.getLength() : HairStrand.MIN_LENGTH));
		scratch.setLengthRatio(HairMath.lerp(factor, a.getLengthRatio(), b.getLengthRatio()));
		scratch.setWidth(HairMath.lerp(factor, fromVisible ? a.getWidth() : HairStrand.MIN_WIDTH, toVisible ? b.getWidth() : HairStrand.MIN_WIDTH));
		scratch.setDepth(HairMath.lerp(factor, fromVisible ? a.getDepth() : HairStrand.MIN_WIDTH, toVisible ? b.getDepth() : HairStrand.MIN_WIDTH));
		scratch.setTaper(HairMath.lerp(factor, a.getTaper(), b.getTaper()));
		scratch.setTaperCurve(HairMath.lerp(factor, a.getTaperCurve(), b.getTaperCurve()));
		scratch.setOffset(
				HairMath.lerp(factor, a.getOffsetX(), b.getOffsetX()),
				HairMath.lerp(factor, a.getOffsetY(), b.getOffsetY()),
				HairMath.lerp(factor, a.getOffsetZ(), b.getOffsetZ()));
		scratch.setRotation(
				lerpAngle(factor, a.getRotationX(), b.getRotationX()),
				lerpAngle(factor, a.getRotationY(), b.getRotationY()),
				lerpAngle(factor, a.getRotationZ(), b.getRotationZ()));
		scratch.setBend(
				HairMath.lerp(factor, a.getBendX(), b.getBendX()),
				HairMath.lerp(factor, a.getBendY(), b.getBendY()),
				HairMath.lerp(factor, a.getBendZ(), b.getBendZ()));
		scratch.setTwist(HairMath.lerp(factor, a.getTwist(), b.getTwist()));
		scratch.setJointStyle(factor < 0.5f ? a.getJointStyle() : b.getJointStyle());

		for (HairSegmentOverride override : a.getOverrides()) blendOverride(scratch, override.getIndex(), override, b.getOverride(override.getIndex()), factor);
		for (HairSegmentOverride override : b.getOverrides()) {
			if (a.getOverride(override.getIndex()) == null) blendOverride(scratch, override.getIndex(), null, override, factor);
		}

		buildGeometry(scratch, face, index, out);
		HairPhysicsProfile.defaultFor(fromSlot).lerpInto(out.physics, HairPhysicsProfile.defaultFor(toSlot), factor);

		out.strandColorFrom = HairColors.parse(a.getColor());
		out.strandColorTo = HairColors.parse(b.getColor());
		out.tipColorFrom = HairColors.parse(a.getTipColor());
		out.tipColorTo = HairColors.parse(b.getTipColor());
		fillSegmentColors(a, out.segments, out.segmentColorsFrom);
		fillSegmentColors(b, out.segments, out.segmentColorsTo);
		out.colorFactor = factor;
	}

	public static void buildGeometry(HairStrand strand, CustomHair.HairFace face, int index, StrandPose out) {
		int segments = strand.getSegments();
		out.ensureCapacity(segments);
		out.face = face;
		out.index = index;
		out.id = face.strandId(index);
		out.segments = segments;
		out.jointStyle = strand.getJointStyle();

		Vector3f base = CustomHair.getStrandBasePosition(face, index);
		out.root.set(base.x + strand.getOffsetX(), base.y + strand.getOffsetY(), base.z + strand.getOffsetZ()).mul(HairMath.PIXEL);
		out.baseRotation.identity()
				.rotateX(strand.getRotationX() * HairMath.DEG_TO_RAD)
				.rotateY(strand.getRotationY() * HairMath.DEG_TO_RAD)
				.rotateZ(strand.getRotationZ() * HairMath.DEG_TO_RAD);
		if (segments <= 0) {
			out.restTip.set(out.root);
			return;
		}

		float totalWeight = 0.0f;
		for (int k = 0; k < segments; k++) {
			float param = segments > 1 ? (float) k / (segments - 1) : 0.0f;
			out.params[k] = param;
			totalWeight += (float) Math.pow(strand.getLengthRatio(), param);
		}

		int joints = Math.max(1, segments - 1);
		float bendX = segments > 1 ? strand.getBendX() / joints * HairMath.DEG_TO_RAD : 0.0f;
		float bendY = segments > 1 ? strand.getBendY() / joints * HairMath.DEG_TO_RAD : 0.0f;
		float bendZ = segments > 1 ? strand.getBendZ() / joints * HairMath.DEG_TO_RAD : 0.0f;
		float twist = segments > 1 ? strand.getTwist() / joints * HairMath.DEG_TO_RAD : 0.0f;

		for (int k = 0; k < segments; k++) {
			float param = out.params[k];
			HairSegmentOverride override = strand.getOverride(k);
			float weight = (float) Math.pow(strand.getLengthRatio(), param) / totalWeight;
			float taperFactor = (float) Math.pow(strand.getTaper(), Math.pow(param, strand.getTaperCurve()));

			out.lengths[k] = strand.getLength() * weight * HairMath.PIXEL * (override != null ? override.getLengthScale() : 1.0f);
			out.widths[k] = strand.getWidth() * taperFactor * HairMath.PIXEL * (override != null ? override.getWidthScale() : 1.0f);
			out.depths[k] = strand.getDepth() * taperFactor * HairMath.PIXEL * (override != null ? override.getDepthScale() : 1.0f);

			Quaternionf relative = out.relativeRotations[k].identity();
			if (k > 0) relative.rotateX(bendX).rotateY(bendY).rotateZ(bendZ).rotateY(twist);

			Vector3f offset = out.localOffsets[k];
			if (override != null) {
				offset.set(override.getOffsetX(), override.getOffsetY(), override.getOffsetZ()).mul(HairMath.PIXEL);
				relative.transform(offset);
				relative.rotateX(override.getRotationX() * HairMath.DEG_TO_RAD)
						.rotateY(override.getRotationY() * HairMath.DEG_TO_RAD)
						.rotateZ(override.getRotationZ() * HairMath.DEG_TO_RAD);
			} else {
				offset.zero();
			}
		}

		Quaternionf parent = out.baseRotation;
		Vector3f cursor = out.restTip.set(out.root);
		for (int k = 0; k < segments; k++) {
			Vector3f start = out.restStarts[k];
			parent.transform(start.set(out.localOffsets[k])).add(cursor);
			Quaternionf rotation = out.restRotations[k].set(parent).mul(out.relativeRotations[k]);
			rotation.transform(cursor.set(0.0f, out.lengths[k], 0.0f)).add(start);
			parent = rotation;
		}
	}

	private static void blendOverride(HairStrand target, int index, HairSegmentOverride a, HairSegmentOverride b, float factor) {
		HairSegmentOverride blended = target.getOrCreateOverride(index);
		blended.setOffset(
				HairMath.lerp(factor, a != null ? a.getOffsetX() : 0.0f, b != null ? b.getOffsetX() : 0.0f),
				HairMath.lerp(factor, a != null ? a.getOffsetY() : 0.0f, b != null ? b.getOffsetY() : 0.0f),
				HairMath.lerp(factor, a != null ? a.getOffsetZ() : 0.0f, b != null ? b.getOffsetZ() : 0.0f));
		blended.setRotation(
				HairMath.lerp(factor, a != null ? a.getRotationX() : 0.0f, b != null ? b.getRotationX() : 0.0f),
				HairMath.lerp(factor, a != null ? a.getRotationY() : 0.0f, b != null ? b.getRotationY() : 0.0f),
				HairMath.lerp(factor, a != null ? a.getRotationZ() : 0.0f, b != null ? b.getRotationZ() : 0.0f));
		blended.setLengthScale(HairMath.lerp(factor, a != null ? a.getLengthScale() : 1.0f, b != null ? b.getLengthScale() : 1.0f));
		blended.setWidthScale(HairMath.lerp(factor, a != null ? a.getWidthScale() : 1.0f, b != null ? b.getWidthScale() : 1.0f));
		blended.setDepthScale(HairMath.lerp(factor, a != null ? a.getDepthScale() : 1.0f, b != null ? b.getDepthScale() : 1.0f));
	}

	private static void fillSegmentColors(HairStrand strand, int segments, int[] out) {
		for (int k = 0; k < segments; k++) {
			HairSegmentOverride override = strand.getOverride(k);
			out[k] = override != null ? HairColors.parse(override.getColor()) : HairColors.INHERIT;
		}
	}

	private static float lerpAngle(float factor, float from, float to) {
		return from + HairMath.wrapDegrees(to - from) * factor;
	}
}
