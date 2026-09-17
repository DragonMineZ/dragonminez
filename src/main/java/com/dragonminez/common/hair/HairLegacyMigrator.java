package com.dragonminez.common.hair;

import net.minecraft.nbt.CompoundTag;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class HairLegacyMigrator {
	public static final float LEGACY_DECAY = 0.85f;
	public static final float FIDELITY_EPSILON_PX = 0.1f;

	private HairLegacyMigrator() {}

	public static HairStrand migrateStrand(CustomHair.HairFace face, int index, CompoundTag tag) {
		LegacyStrand legacy = LegacyStrand.read(tag);
		HairStrand strand = CustomHair.createDefaultStrand(face, index);
		strand.setRotation(legacy.rotationX, legacy.rotationY, legacy.rotationZ);
		strand.setColor(legacy.color);

		int segments = Math.min(legacy.length, HairStrand.MAX_SEGMENTS);
		if (segments <= 0) {
			strand.setSegments(0);
			return strand;
		}

		double decaySum = (1.0 - Math.pow(LEGACY_DECAY, segments)) / (1.0 - LEGACY_DECAY);
		float ratio = (float) Math.pow(LEGACY_DECAY, segments - 1);

		strand.setSegments(segments);
		strand.setLength((float) (legacy.cubeHeight * legacy.lengthScale * legacy.scaleY * decaySum));
		strand.setLengthRatio(ratio);
		strand.setTaper(ratio);
		strand.setTaperCurve(1.0f);
		strand.setWidth(legacy.cubeWidth * legacy.scaleX);
		strand.setDepth(legacy.cubeDepth * legacy.scaleZ);
		strand.setBend(legacy.curveX * (segments - 1), legacy.curveY * (segments - 1), legacy.curveZ * (segments - 1));

		Vector3f[] legacyJoints = legacyCenterline(face, index, legacy, segments);
		StrandPose pose = new StrandPose();
		HairPoseBuilder.buildGeometry(strand, face, index, pose);
		if (maxDeviationPx(pose, legacyJoints) > FIDELITY_EPSILON_PX) fitCenterline(strand, face, index, legacyJoints, pose);
		return strand;
	}

	public static Vector3f[] legacyCenterline(CustomHair.HairFace face, int index, LegacyStrand legacy, int segments) {
		Vector3f base = CustomHair.getStrandBasePosition(face, index);
		Matrix4f matrix = new Matrix4f()
				.translate(base.x, base.y, base.z)
				.rotateX(legacy.rotationX * HairMath.DEG_TO_RAD)
				.rotateY(legacy.rotationY * HairMath.DEG_TO_RAD)
				.rotateZ(legacy.rotationZ * HairMath.DEG_TO_RAD)
				.scale(legacy.scaleX, legacy.scaleY, legacy.scaleZ);

		Vector3f[] joints = new Vector3f[segments + 1];
		float decay = 1.0f;
		float previousHeight = 0.0f;
		for (int i = 0; i < segments; i++) {
			float height = legacy.cubeHeight * decay * legacy.lengthScale;
			if (i > 0) {
				matrix.translate(0.0f, previousHeight, 0.0f)
						.rotateX(legacy.curveX * HairMath.DEG_TO_RAD)
						.rotateY(legacy.curveY * HairMath.DEG_TO_RAD)
						.rotateZ(legacy.curveZ * HairMath.DEG_TO_RAD);
			}
			joints[i] = matrix.transformPosition(new Vector3f());
			previousHeight = height;
			decay *= LEGACY_DECAY;
		}
		joints[segments] = matrix.transformPosition(new Vector3f(0.0f, previousHeight, 0.0f));
		return joints;
	}

	public static float maxDeviationPx(StrandPose pose, Vector3f[] legacyJoints) {
		float max = 0.0f;
		Vector3f scratch = new Vector3f();
		for (int k = 0; k < pose.segments; k++) {
			max = Math.max(max, scratch.set(pose.restStarts[k]).mul(16.0f).distance(legacyJoints[k]));
		}
		max = Math.max(max, scratch.set(pose.restTip).mul(16.0f).distance(legacyJoints[pose.segments]));
		return max;
	}

	private static void fitCenterline(HairStrand strand, CustomHair.HairFace face, int index, Vector3f[] legacyJoints, StrandPose pose) {
		strand.clearOverrides();
		int segments = pose.segments;
		float[] baseLengths = new float[segments];
		for (int k = 0; k < segments; k++) baseLengths[k] = pose.lengths[k] * 16.0f;

		Quaternionf parent = new Quaternionf(pose.baseRotation);
		Quaternionf frame = new Quaternionf();
		Quaternionf inverse = new Quaternionf();
		Vector3f direction = new Vector3f();
		int joints = Math.max(1, segments - 1);
		float bendX = segments > 1 ? strand.getBendX() / joints * HairMath.DEG_TO_RAD : 0.0f;
		float bendY = segments > 1 ? strand.getBendY() / joints * HairMath.DEG_TO_RAD : 0.0f;
		float bendZ = segments > 1 ? strand.getBendZ() / joints * HairMath.DEG_TO_RAD : 0.0f;

		for (int k = 0; k < segments; k++) {
			frame.set(parent);
			if (k > 0) frame.rotateX(bendX).rotateY(bendY).rotateZ(bendZ);

			direction.set(legacyJoints[k + 1]).sub(legacyJoints[k]);
			float actualLength = direction.length();
			if (actualLength < 1.0e-5f || baseLengths[k] < 1.0e-5f) {
				parent.set(frame);
				continue;
			}

			frame.invert(inverse).transform(direction).div(actualLength);
			float rotationZ = (float) Math.asin(HairMath.clamp(-direction.x, -1.0f, 1.0f));
			float rotationX = (float) Math.atan2(direction.z, direction.y);

			HairSegmentOverride override = strand.getOrCreateOverride(k);
			override.setRotation(rotationX * HairMath.RAD_TO_DEG, 0.0f, rotationZ * HairMath.RAD_TO_DEG);
			override.setLengthScale(actualLength / baseLengths[k]);
			if (override.isIdentity()) strand.removeOverride(k);

			parent.set(frame).rotateX(rotationX).rotateZ(rotationZ);
		}
	}

	public static final class LegacyStrand {
		public int length;
		public float lengthScale = 1.0f;
		public float rotationX;
		public float rotationY;
		public float rotationZ;
		public float scaleX = 1.0f;
		public float scaleY = 1.0f;
		public float scaleZ = 1.0f;
		public float cubeWidth = 2.0f;
		public float cubeHeight = 2.0f;
		public float cubeDepth = 2.0f;
		public float curveX;
		public float curveY;
		public float curveZ;
		public String color;

		public static LegacyStrand read(CompoundTag tag) {
			LegacyStrand legacy = new LegacyStrand();
			legacy.length = Math.max(0, tag.contains("l") ? tag.getInt("l") : tag.getInt("Length"));
			legacy.lengthScale = readFloat(tag, "ls", "LengthScale", 1.0f);
			legacy.rotationX = readFloat(tag, "rx", "RotX", 0.0f);
			legacy.rotationY = readFloat(tag, "ry", "RotY", 0.0f);
			legacy.rotationZ = readFloat(tag, "rz", "RotZ", 0.0f);
			legacy.scaleX = readFloat(tag, "sx", "ScaleX", 1.0f);
			legacy.scaleY = readFloat(tag, "sy", "ScaleY", 1.0f);
			legacy.scaleZ = readFloat(tag, "sz", "ScaleZ", 1.0f);
			legacy.cubeWidth = readFloat(tag, "cw", "CubeW", 2.0f);
			legacy.cubeHeight = readFloat(tag, "ch", "CubeH", 2.0f);
			legacy.cubeDepth = readFloat(tag, "cd", "CubeD", 2.0f);
			legacy.curveX = readFloat(tag, "cx", "CurveX", 0.0f);
			legacy.curveY = readFloat(tag, "cy", "CurveY", 0.0f);
			legacy.curveZ = readFloat(tag, "cz", "CurveZ", 0.0f);
			legacy.color = tag.contains("c") ? tag.getString("c") : (tag.contains("Color") ? tag.getString("Color") : null);
			return legacy;
		}

		private static float readFloat(CompoundTag tag, String shortKey, String longKey, float fallback) {
			float value = tag.contains(shortKey) ? tag.getFloat(shortKey) : (tag.contains(longKey) ? tag.getFloat(longKey) : fallback);
			return Float.isFinite(value) ? value : fallback;
		}
	}
}
