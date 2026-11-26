package com.dragonminez.common.hair;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HairTransformGenerator {
	private static final float HEAD_HALF = 4.0f;
	private static final float HEAD_TOP = 8.0f;
	private static final float HEAD_TOLERANCE = 0.25f;
	private static final float ROOT_CLEARANCE = 1.0f;
	private static final float SAMPLE_STEP = 0.5f;
	private static final float MIN_CHORD = 0.05f;
	private static final float OUTWARD_BIAS = 0.6f;
	private static final float HANGING_ELEVATION = -20.0f;
	private static final float CENTER_BANG_WEIGHT = 1.25f;
	private static final float BANG_PAIR_TOLERANCE = 0.9f;
	private static final float RADIAL_LIMIT = 150.0f * HairMath.DEG_TO_RAD;
	private static final float RADIAL_RAMP = 20.0f;
	private static final float FLIP_STRAIGHTEN = 0.6f;
	private static final float LONG_STRAND = 6.0f;
	private static final float HANG_LIMIT = -30.0f;
	private static final float RISE_MIN = 45.0f;
	private static final float SNAP_PIVOT = 5.0f;
	private static final float BANG_RISE_MIN = 45.0f;
	private static final float FACE_RADIAL_SHARE = 0.4f;
	private static final float RISEN_LENGTH_CAP = 8.0f;
	private static final float RISEN_LENGTH_KEEP = 0.5f;
	private static final float EXPOSURE_DONOR_DISTANCE = 3.5f;
	private static final int VOLUME_FIRST_ROW = 2;
	private static final int COLLISION_STEPS = 6;
	private static final float COLLISION_PUSH = 0.35f;

	private static final float COVER_SKIP = 1.5f;
	private static final float COVER_RADIUS = 1.0f;
	private static final float FILLER_LENGTH = 0.75f;
	private static final float FILLER_MIN_LENGTH = 2.0f;
	private static final float FILLER_SPREAD = 0.35f;

	private static final float LACIO_STRAND_LENGTH = 6.0f;
	private static final float LACIO_SHARE = 0.3f;
	private static final int LACIO_MIN_STRANDS = 4;
	private static final float LACIO_HANG_ELEVATION = -35.0f;
	private static final float LACIO_LIFT_SCALE = 0.5f;
	private static final float CURL_MIN_LENGTH = 3.5f;
	private static final float CURL_FOCUS = 2.5f;
	private static final float CURL_SKIP = 5.0f;
	private static final float CURL_RADIAL = 0.7f;
	private static final float CURL_SIDE_FORWARD = 0.4f;
	private static final int CURL_ATTEMPTS = 3;
	private static final float CURL_RETRY_STEP = 15.0f;
	private static final float LACIO_FILLER_LENGTH = 0.45f;
	private static final float LACIO_FILLER_MAX_LENGTH = 5.0f;
	private static final float LACIO_FILLER_SEGMENT = 1.6f;
	private static final float LACIO_FILLER_RISE = 0.25f;
	private static final int LACIO_VOLUME_FIRST_ROW = 1;

	private static final float TEMPLATE_BANG_LENGTH = 4.5f;
	private static final float TEMPLATE_BANG_ELEVATION = -25.0f;
	private static final float FORELOCK_WIDTH = 0.25f;
	private static final float FORELOCK_LENGTH = 0.6f;
	private static final float LOCK_MIN_LENGTH = 5.0f;
	private static final float LONG_BACK_LENGTH = 9.0f;
	private static final int LONG_BACK_MIN_STRANDS = 4;
	private static final float LOCK_HANG_ELEVATION = -30.0f;
	private static final float MIN_GENERATED_TAPER = 0.25f;

	private static final SpikeProfile SSJ_PROFILE = new SpikeProfile(
			new FaceLift[]{
					new FaceLift(55.0f, 0.4f, 0.45f),
					new FaceLift(60.0f, 0.55f, 0.15f),
					new FaceLift(65.0f, 0.6f, 0.75f),
					new FaceLift(65.0f, 0.6f, 0.75f),
					new FaceLift(70.0f, 0.5f, 0.75f)},
			new FaceCurl[]{
					new FaceCurl(35.0f, Float.NaN),
					new FaceCurl(Float.NaN, Float.NaN),
					new FaceCurl(35.0f, Float.NaN),
					new FaceCurl(35.0f, Float.NaN),
					new FaceCurl(35.0f, Float.NaN)},
			0.5f, 0.7f, 0.92f, 1.0f, false);
	private static final SpikeProfile SSJ2_PROFILE = new SpikeProfile(
			new FaceLift[]{
					new FaceLift(65.0f, 0.8f, 0.9f),
					new FaceLift(60.0f, 0.5f, 0.8f),
					new FaceLift(65.0f, 0.4f, 0.55f),
					new FaceLift(65.0f, 0.4f, 0.55f),
					new FaceLift(70.0f, 0.35f, 0.5f)},
			new FaceCurl[]{
					new FaceCurl(50.0f, Float.NaN),
					new FaceCurl(25.0f, Float.NaN),
					new FaceCurl(50.0f, 20.0f),
					new FaceCurl(50.0f, 20.0f),
					new FaceCurl(50.0f, 20.0f)},
			0.4f, 0.85f, 0.88f, 1.15f, true);

	private static final Vector3fc UP = new Vector3f(0.0f, 1.0f, 0.0f);

	private HairTransformGenerator() {}

	public static EnumMap<HairStyleSlot, CustomHair> generateAll(CustomHair base, HairLimits limits) {
		EnumMap<HairStyleSlot, CustomHair> styles = new EnumMap<>(HairStyleSlot.class);
		CustomHair ssj = spike(base, base, SSJ_PROFILE, HairStyleSlot.SSJ, limits);
		CustomHair ssj2 = spike(ssj, base, SSJ2_PROFILE, HairStyleSlot.SSJ2, limits);
		styles.put(HairStyleSlot.SSJ, ssj);
		styles.put(HairStyleSlot.SSJ2, ssj2);
		styles.put(HairStyleSlot.SSJ3, superSaiyan3(base, limits));
		styles.put(HairStyleSlot.SSJ4, superSaiyan4(base, ssj, limits));
		return styles;
	}

	public static CustomHair generate(CustomHair base, HairStyleSlot slot, HairLimits limits) {
		return switch (slot) {
			case BASE -> base.copy();
			case SSJ -> spike(base, base, SSJ_PROFILE, HairStyleSlot.SSJ, limits);
			case SSJ2 -> superSaiyan2(base, limits);
			case SSJ3 -> superSaiyan3(base, limits);
			case SSJ4 -> superSaiyan4(base, spike(base, base, SSJ_PROFILE, HairStyleSlot.SSJ, limits), limits);
		};
	}

	private static CustomHair superSaiyan2(CustomHair base, HairLimits limits) {
		return spike(spike(base, base, SSJ_PROFILE, HairStyleSlot.SSJ, limits), base, SSJ2_PROFILE, HairStyleSlot.SSJ2, limits);
	}

	private static CustomHair spike(CustomHair source, CustomHair reference, SpikeProfile profile, HairStyleSlot slot, HairLimits limits) {
		CustomHair out = source.copy();
		StrandPose pose = new StrandPose();
		Set<Integer> keptBangs = pickBangs(source, profile, pose);
		boolean lacio = isLacio(reference, pose);

		for (CustomHair.HairFace face : CustomHair.HairFace.values()) {
			for (int index = 0; index < face.maxStrands; index++) {
				HairStrand strand = out.getStrand(face, index);
				if (strand == null || !strand.isVisible()) continue;
				if (face == CustomHair.HairFace.FRONT && keptBangs.contains(index)) continue;
				float originalDepth = headPenetration(strand, face, index, pose);
				HairStrand shape = reference.getStrand(face, index);
				if (lacio && shape != null && shape.isVisible() && measure(shape, face, index, pose, new Vector3f()) >= CURL_MIN_LENGTH) {
					sharpen(strand, profile);
					float tip = profile.curls[face.ordinal()].target(face, index);
					if (!Float.isNaN(tip)) curlWithCollision(strand, face, index, tip, originalDepth, pose);
					continue;
				}
				reshape(strand, profile);
				liftStrand(strand, face, index, profile, lacio, pose);
				resolveHeadCollision(strand, face, index, originalDepth, pose);
			}
		}

		addFillers(source, out, pose, lacio);
		return finish(out, source, slot, limits);
	}

	private static CustomHair superSaiyan3(CustomHair base, HairLimits limits) {
		StrandPose pose = new StrandPose();
		CustomHair out = (hasLongBack(base, pose) ? HairPresets.getLongSsj3() : HairPresets.getDefaultSsj3()).copy();
		Vector3f chord = new Vector3f();
		CustomHair.HairFace front = CustomHair.HairFace.FRONT;
		for (int index = 1; index < front.maxStrands - 1; index++) {
			HairStrand bang = base.getStrand(front, index);
			if (bang == null || !bang.isVisible()) continue;
			float length = measure(bang, front, index, pose, chord);
			if (length < TEMPLATE_BANG_LENGTH || elevation(chord) > TEMPLATE_BANG_ELEVATION) continue;
			HairStrand forelock = bang.copy();
			forelock.setWidth(FORELOCK_WIDTH);
			forelock.setDepth(FORELOCK_WIDTH);
			forelock.setLength(bang.getLength() * FORELOCK_LENGTH);
			out.setStrand(front, index, forelock);
		}
		return finish(out, base, HairStyleSlot.SSJ3, limits);
	}

	private static CustomHair superSaiyan4(CustomHair base, CustomHair ssj, HairLimits limits) {
		CustomHair out = base.copy();
		for (CustomHair.HairFace face : CustomHair.HairFace.values()) {
			for (int index = 0; index < face.maxStrands; index++) {
				HairStrand own = base.getStrand(face, index);
				HairStrand filler = ssj.getStrand(face, index);
				if ((own == null || !own.isVisible()) && filler != null && filler.isVisible()) out.setStrand(face, index, filler);
			}
		}
		CustomHair template = HairPresets.getDefaultSsj4();
		StrandPose pose = new StrandPose();
		Vector3f chord = new Vector3f();
		for (CustomHair.HairFace face : new CustomHair.HairFace[]{CustomHair.HairFace.BACK, CustomHair.HairFace.LEFT, CustomHair.HairFace.RIGHT}) {
			for (int index = 0; index < face.maxStrands; index++) {
				HairStrand lock = template.getStrand(face, index);
				if (!isLock(lock, face, index, pose)) continue;
				CustomHair.HairFace mirrorFace = CustomHair.mirrorFace(face);
				int mirrorIndex = CustomHair.mirrorIndex(face, index);
				if (!isLock(template.getStrand(mirrorFace, mirrorIndex), mirrorFace, mirrorIndex, pose)) continue;
				float lockLength = measure(lock, face, index, pose, chord);
				HairStrand current = out.getStrand(face, index);
				float currentLength = current != null && current.isVisible() ? measure(current, face, index, pose, chord) : 0.0f;
				if (currentLength < lockLength) out.setStrand(face, index, lock);
			}
		}
		return finish(out, base, HairStyleSlot.SSJ4, limits);
	}

	private static boolean hasLongBack(CustomHair base, StrandPose pose) {
		CustomHair.HairFace back = CustomHair.HairFace.BACK;
		Vector3f chord = new Vector3f();
		float total = 0.0f;
		int count = 0;
		for (int index = 0; index < back.maxStrands; index++) {
			HairStrand strand = base.getStrand(back, index);
			if (strand == null || !strand.isVisible()) continue;
			total += measure(strand, back, index, pose, chord);
			count++;
		}
		return count >= LONG_BACK_MIN_STRANDS && total / count >= LONG_BACK_LENGTH;
	}

	private static boolean isLock(HairStrand strand, CustomHair.HairFace face, int index, StrandPose pose) {
		if (strand == null || !strand.isVisible()) return false;
		Vector3f chord = new Vector3f();
		if (measure(strand, face, index, pose, chord) < LOCK_MIN_LENGTH) return false;
		if (elevation(chord) <= LOCK_HANG_ELEVATION) return true;
		return elevation(pose.restRotations[pose.segments - 1].transform(new Vector3f(UP))) <= LOCK_HANG_ELEVATION;
	}

	private static CustomHair finish(CustomHair out, CustomHair base, HairStyleSlot slot, HairLimits limits) {
		out.setName(base.getName());
		out.setGlobalColor(base.getGlobalColor());
		HairSanitizer.sanitize(out, slot, limits);
		out.markChanged();
		return out;
	}

	private static Set<Integer> pickBangs(CustomHair source, SpikeProfile profile, StrandPose pose) {
		CustomHair.HairFace front = CustomHair.HairFace.FRONT;
		List<float[]> candidates = new ArrayList<>();
		Vector3f chord = new Vector3f();
		for (int index = 0; index < front.maxStrands; index++) {
			HairStrand strand = source.getStrand(front, index);
			if (strand == null || !strand.isVisible()) continue;
			float length = measure(strand, front, index, pose, chord);
			float elevation = elevation(chord);
			if (elevation >= HANGING_ELEVATION) continue;
			boolean center = index > 0 && index < front.maxStrands - 1;
			float score = length * (float) -Math.sin(elevation * HairMath.DEG_TO_RAD) * (center ? CENTER_BANG_WEIGHT : 1.0f);
			candidates.add(new float[]{index, score});
		}
		candidates.sort((a, b) -> a[1] != b[1] ? Float.compare(b[1], a[1]) : Float.compare(a[0], b[0]));
		int keep = profile.singleBang ? Math.min(1, candidates.size()) : (candidates.size() + 1) / 2;
		Set<Integer> kept = new HashSet<>();
		for (int i = 0; i < keep; i++) kept.add((int) candidates.get(i)[0]);
		if (!profile.singleBang && keep > 0) {
			float[] last = candidates.get(keep - 1);
			int partner = front.maxStrands - 1 - (int) last[0];
			for (float[] candidate : candidates) {
				if ((int) candidate[0] == partner && candidate[1] >= last[1] * BANG_PAIR_TOLERANCE) kept.add(partner);
			}
		}
		return kept;
	}

	private static void straighten(HairStrand strand, float factor) {
		strand.setBend(strand.getBendX() * factor, strand.getBendY() * factor, strand.getBendZ() * factor);
		for (HairSegmentOverride override : strand.getOverrides()) {
			override.setRotation(override.getRotationX() * factor, override.getRotationY() * factor, override.getRotationZ() * factor);
		}
	}

	private static void reshape(HairStrand strand, SpikeProfile profile) {
		if (profile.lengthScale != 1.0f) strand.setLength(strand.getLength() * profile.lengthScale);
		sharpen(strand, profile);
	}

	private static void sharpen(HairStrand strand, SpikeProfile profile) {
		if (profile.taper != 1.0f && strand.getTaper() > MIN_GENERATED_TAPER) {
			strand.setTaper(Math.max(MIN_GENERATED_TAPER, strand.getTaper() * profile.taper));
		}
	}

	private static void liftStrand(HairStrand strand, CustomHair.HairFace face, int index, SpikeProfile profile, boolean lacio, StrandPose pose) {
		FaceLift lift = profile.lifts[face.ordinal()];
		Vector3f chord = new Vector3f();
		float length = measureDirection(strand, face, index, pose, chord);
		if (length < 0.0f) return;
		float current = elevation(chord);
		float amount = lift.amount(current) * (lacio ? LACIO_LIFT_SCALE : 1.0f);
		float lifted = current >= lift.targetElevation ? current : current + amount * (lift.targetElevation - current);
		if (!lacio && face == CustomHair.HairFace.FRONT) lifted = Math.max(lifted, BANG_RISE_MIN);
		else if (!lacio && length >= LONG_STRAND && lifted > HANG_LIMIT && lifted < RISE_MIN) lifted = lifted >= SNAP_PIVOT ? RISE_MIN : Math.min(current, HANG_LIMIT);
		float radial = profile.radial * HairMath.clamp((lifted - current) / RADIAL_RAMP, 0.0f, 1.0f);
		Vector3f target = liftedDirection(chord, outward(face, pose.root, new Vector3f()), lifted, radial);

		straighten(strand, profile.straighten * (1.0f - FLIP_STRAIGHTEN * HairMath.clamp((lifted - current) / 90.0f, 0.0f, 1.0f)));
		if (lifted > 0.0f && current < 0.0f && length > RISEN_LENGTH_CAP) {
			strand.setLength(strand.getLength() * (RISEN_LENGTH_CAP + (length - RISEN_LENGTH_CAP) * RISEN_LENGTH_KEEP) / length);
		}
		if (measureDirection(strand, face, index, pose, chord) < 0.0f) return;
		rotateStrand(strand, pose.baseRotation, chord, target);
	}

	private static void curlWithCollision(HairStrand strand, CustomHair.HairFace face, int index, float tipElevation, float allowedDepth, StrandPose pose) {
		HairStrand original = strand.copy();
		for (int attempt = 0; attempt < CURL_ATTEMPTS; attempt++) {
			if (!curlStrand(strand, face, index, tipElevation - attempt * CURL_RETRY_STEP, pose)) return;
			if (headPenetration(strand, face, index, pose) <= allowedDepth + HEAD_TOLERANCE) return;
			strand.copyFrom(original);
		}
	}

	private static boolean curlStrand(HairStrand strand, CustomHair.HairFace face, int index, float tipElevation, StrandPose pose) {
		int segments = strand.getSegments();
		if (segments < 2) return false;
		HairPoseBuilder.buildGeometry(strand, face, index, pose);
		Vector3f tip = pose.restRotations[segments - 1].transform(new Vector3f(UP));
		if (elevation(tip) >= tipElevation - CURL_SKIP) return false;
		Vector3f target = liftedDirection(tip, curlOutward(face, pose.root, new Vector3f()), tipElevation, CURL_RADIAL);

		float remaining = 0.0f;
		for (int k = 1; k < segments; k++) remaining += curlWeight(k, segments);
		Vector3f tangent = new Vector3f();
		Vector3f axis = new Vector3f();
		Quaternionf frame = new Quaternionf();
		Vector3f euler = new Vector3f();
		for (int k = 1; k < segments; k++) {
			float weight = curlWeight(k, segments);
			float fraction = weight / remaining;
			remaining -= weight;
			HairPoseBuilder.buildGeometry(strand, face, index, pose);
			pose.restRotations[k].transform(tangent.set(UP));
			float angle = (float) Math.acos(HairMath.clamp(tangent.dot(target), -1.0f, 1.0f));
			tangent.cross(target, axis);
			if (angle < 1.0e-4f || axis.lengthSquared() < 1.0e-8f) continue;
			pose.restRotations[k].invert(frame).transform(axis.normalize());
			HairSegmentOverride override = strand.getOrCreateOverride(k);
			new Quaternionf()
					.rotateX(override.getRotationX() * HairMath.DEG_TO_RAD)
					.rotateY(override.getRotationY() * HairMath.DEG_TO_RAD)
					.rotateZ(override.getRotationZ() * HairMath.DEG_TO_RAD)
					.rotateAxis(angle * fraction, axis)
					.getEulerAnglesXYZ(euler);
			override.setRotation(euler.x * HairMath.RAD_TO_DEG, euler.y * HairMath.RAD_TO_DEG, euler.z * HairMath.RAD_TO_DEG);
		}
		return true;
	}

	private static float curlWeight(int joint, int segments) {
		return (float) Math.pow((float) joint / (segments - 1), CURL_FOCUS);
	}

	private static Vector3f curlOutward(CustomHair.HairFace face, Vector3f rootBlocks, Vector3f out) {
		if (face != CustomHair.HairFace.FRONT) return outward(face, rootBlocks, out);
		return out.set(Math.signum(rootBlocks.x), 0.0f, -CURL_SIDE_FORWARD).normalize();
	}

	private static boolean isLacio(CustomHair hair, StrandPose pose) {
		int visible = 0;
		int hanging = 0;
		for (CustomHair.HairFace face : CustomHair.HairFace.values()) {
			for (int index = 0; index < face.maxStrands; index++) {
				HairStrand strand = hair.getStrand(face, index);
				if (strand == null || !strand.isVisible()) continue;
				visible++;
				if (isLongHanging(strand, face, index, pose)) hanging++;
			}
		}
		return hanging >= LACIO_MIN_STRANDS && hanging >= visible * LACIO_SHARE;
	}

	private static boolean isLongHanging(HairStrand strand, CustomHair.HairFace face, int index, StrandPose pose) {
		Vector3f chord = new Vector3f();
		if (measure(strand, face, index, pose, chord) < LACIO_STRAND_LENGTH) return false;
		if (elevation(chord) <= LACIO_HANG_ELEVATION) return true;
		return pose.segments > 0 && elevation(pose.restRotations[pose.segments - 1].transform(new Vector3f(UP))) <= LACIO_HANG_ELEVATION;
	}

	private static void resolveHeadCollision(HairStrand strand, CustomHair.HairFace face, int index, float allowedDepth, StrandPose pose) {
		Vector3f chord = new Vector3f();
		Vector3f escape = new Vector3f();
		Vector3f target = new Vector3f();
		for (int step = 0; step < COLLISION_STEPS; step++) {
			if (headPenetration(strand, face, index, pose) <= allowedDepth + HEAD_TOLERANCE) return;
			if (measureDirection(strand, face, index, pose, chord) < 0.0f) return;
			outward(face, pose.root, escape).add(UP).normalize();
			slerp(chord, escape, COLLISION_PUSH, target);
			rotateStrand(strand, pose.baseRotation, chord, target);
		}
	}

	private static void addFillers(CustomHair source, CustomHair out, StrandPose pose, boolean lacio) {
		List<CoverSample> before = coverSamples(source, pose);
		List<CoverSample> after = coverSamples(out, pose);
		List<CoverSample> exposed = new ArrayList<>();
		List<int[]> exposedSlots = new ArrayList<>();
		Vector3f point = new Vector3f();
		for (CustomHair.HairFace face : CustomHair.HairFace.values()) {
			for (int index = 0; index < face.maxStrands; index++) {
				HairStrand existing = source.getStrand(face, index);
				if (existing != null && existing.isVisible()) continue;
				point.set(CustomHair.getStrandBasePosition(face, index));
				CoverSample donor = nearestCover(before, point);
				if (donor == null || nearestCover(after, point) != null) continue;
				if (CustomHair.getStrandBasePosition(donor.face, donor.index).distance(point) > EXPOSURE_DONOR_DISTANCE) continue;
				exposed.add(donor);
				exposedSlots.add(new int[]{face.ordinal(), index});
			}
		}
		for (int i = 0; i < exposed.size(); i++) {
			CoverSample donor = exposed.get(i);
			placeFiller(out, CustomHair.HairFace.values()[exposedSlots.get(i)[0]], exposedSlots.get(i)[1], donor.face, donor.index, lacio, pose);
		}

		for (CustomHair.HairFace face : new CustomHair.HairFace[]{CustomHair.HairFace.BACK, CustomHair.HairFace.LEFT, CustomHair.HairFace.RIGHT}) {
			for (int row = lacio ? LACIO_VOLUME_FIRST_ROW : VOLUME_FIRST_ROW; row < face.rows; row++) {
				for (int col = 0; col < face.cols; col++) {
					int index = row * face.cols + col;
					HairStrand existing = out.getStrand(face, index);
					if (existing != null && existing.isVisible()) continue;
					if (!lacio && CustomHair.getStrandBasePosition(face, index).z <= 0.0f) continue;
					int upper = index - face.cols;
					HairStrand original = source.getStrand(face, upper);
					if (original == null || !original.isVisible()) continue;
					placeFiller(out, face, index, face, upper, lacio, pose);
				}
			}
		}
	}

	private static void placeFiller(CustomHair out, CustomHair.HairFace face, int index, CustomHair.HairFace donorFace, int donorIndex, boolean lacio, StrandPose pose) {
		HairStrand donor = out.getStrand(donorFace, donorIndex);
		if (donor == null || !donor.isVisible()) return;
		Vector3f donorDirection = new Vector3f();
		if (measureDirection(donor, donorFace, donorIndex, pose, donorDirection) < 0.0f) return;

		HairStrand filler = donor.copy();
		filler.clearOverrides();
		filler.setOffset(0.0f, 0.0f, 0.0f);
		if (lacio) {
			float length = HairMath.clamp(donor.getLength() * LACIO_FILLER_LENGTH, FILLER_MIN_LENGTH, LACIO_FILLER_MAX_LENGTH);
			filler.setBend(0.0f, 0.0f, 0.0f);
			filler.setTwist(0.0f);
			filler.setSegments(Math.max(1, Math.min(donor.getSegments(), Math.round(length / LACIO_FILLER_SEGMENT))));
			filler.setLength(length);
		} else {
			filler.setLength(Math.max(FILLER_MIN_LENGTH, donor.getLength() * FILLER_LENGTH));
		}
		Vector3f chord = new Vector3f();
		if (measureDirection(filler, face, index, pose, chord) < 0.0f) return;

		Vector3f escape = outward(face, pose.root, new Vector3f()).add(UP).normalize();
		Vector3f target = new Vector3f();
		if (lacio) slerp(escape, new Vector3f(UP), LACIO_FILLER_RISE, target);
		else slerp(donorDirection, escape, FILLER_SPREAD, target);
		rotateStrand(filler, pose.baseRotation, chord, target);
		resolveHeadCollision(filler, face, index, 0.0f, pose);
		out.setStrand(face, index, filler);
	}

	private static List<CoverSample> coverSamples(CustomHair hair, StrandPose pose) {
		List<CoverSample> samples = new ArrayList<>();
		Vector3f start = new Vector3f();
		Vector3f end = new Vector3f();
		for (CustomHair.HairFace face : CustomHair.HairFace.values()) {
			for (int index = 0; index < face.maxStrands; index++) {
				HairStrand strand = hair.getStrand(face, index);
				if (strand == null || !strand.isVisible()) continue;
				HairPoseBuilder.buildGeometry(strand, face, index, pose);
				float arc = 0.0f;
				for (int k = 0; k < pose.segments; k++) {
					start.set(pose.restStarts[k]).div(HairMath.PIXEL);
					pose.restEnd(k, end).div(HairMath.PIXEL);
					float length = start.distance(end);
					float radius = Math.max(pose.widths[k], pose.depths[k]) / HairMath.PIXEL * 0.5f;
					int steps = Math.max(1, (int) Math.ceil(length / SAMPLE_STEP));
					for (int s = 0; s <= steps; s++) {
						float t = (float) s / steps;
						if (arc + length * t < COVER_SKIP) continue;
						samples.add(new CoverSample(face, index, new Vector3f(start).lerp(end, t), radius));
					}
					arc += length;
				}
			}
		}
		return samples;
	}

	private static CoverSample nearestCover(List<CoverSample> samples, Vector3f point) {
		CoverSample best = null;
		float bestGap = Float.MAX_VALUE;
		for (CoverSample sample : samples) {
			float gap = sample.position.distance(point) - sample.radius;
			if (gap <= COVER_RADIUS && gap < bestGap) {
				bestGap = gap;
				best = sample;
			}
		}
		return best;
	}

	private static float headPenetration(HairStrand strand, CustomHair.HairFace face, int index, StrandPose pose) {
		HairPoseBuilder.buildGeometry(strand, face, index, pose);
		Vector3f start = new Vector3f();
		Vector3f end = new Vector3f();
		Vector3f point = new Vector3f();
		float arc = 0.0f;
		float depth = 0.0f;
		for (int k = 0; k < pose.segments; k++) {
			start.set(pose.restStarts[k]).div(HairMath.PIXEL);
			pose.restEnd(k, end).div(HairMath.PIXEL);
			float length = start.distance(end);
			int steps = Math.max(1, (int) Math.ceil(length / SAMPLE_STEP));
			for (int s = 0; s <= steps; s++) {
				float t = (float) s / steps;
				if (arc + length * t < ROOT_CLEARANCE) continue;
				point.set(start).lerp(end, t);
				float inside = Math.min(Math.min(HEAD_HALF - Math.abs(point.x), HEAD_HALF - Math.abs(point.z)), Math.min(point.y, HEAD_TOP - point.y));
				depth = Math.max(depth, inside);
			}
			arc += length;
		}
		return depth;
	}

	private static float measure(HairStrand strand, CustomHair.HairFace face, int index, StrandPose pose, Vector3f chordOut) {
		HairPoseBuilder.buildGeometry(strand, face, index, pose);
		chordOut.set(pose.restTip).sub(pose.root).div(HairMath.PIXEL);
		float length = 0.0f;
		for (int k = 0; k < pose.segments; k++) length += pose.lengths[k];
		return length / HairMath.PIXEL;
	}

	private static float measureDirection(HairStrand strand, CustomHair.HairFace face, int index, StrandPose pose, Vector3f directionOut) {
		float length = measure(strand, face, index, pose, directionOut);
		if (directionOut.length() < MIN_CHORD) return -1.0f;
		directionOut.normalize();
		return length;
	}

	private static float elevation(Vector3f direction) {
		float length = direction.length();
		if (length < 1.0e-6f) return 0.0f;
		return (float) Math.asin(HairMath.clamp(direction.y / length, -1.0f, 1.0f)) * HairMath.RAD_TO_DEG;
	}

	private static Vector3f outward(CustomHair.HairFace face, Vector3f rootBlocks, Vector3f out) {
		out.set(rootBlocks.x, 0.0f, rootBlocks.z).div(HairMath.PIXEL);
		if (out.lengthSquared() < 1.0e-4f) out.set(0.0f, 0.0f, 1.0f);
		out.normalize();
		if (face == CustomHair.HairFace.TOP) return out;
		Vector3f normal = switch (face) {
			case FRONT -> new Vector3f(0.0f, 0.0f, -1.0f);
			case BACK -> new Vector3f(0.0f, 0.0f, 1.0f);
			case LEFT -> new Vector3f(-1.0f, 0.0f, 0.0f);
			default -> new Vector3f(1.0f, 0.0f, 0.0f);
		};
		return out.mul(FACE_RADIAL_SHARE).add(normal).normalize();
	}

	private static Vector3f liftedDirection(Vector3f direction, Vector3f outward, float elevation, float radial) {
		Vector3f horizontal = new Vector3f(direction.x, 0.0f, direction.z);
		float spread = horizontal.length();
		horizontal.fma(OUTWARD_BIAS * Math.max(0.0f, 1.0f - spread), outward);
		if (horizontal.lengthSquared() < 1.0e-6f) horizontal.set(outward);
		float heading = (float) Math.atan2(horizontal.x, horizontal.z);
		float delta = wrapRadians((float) Math.atan2(outward.x, outward.z) - heading);
		if (Math.abs(delta) < RADIAL_LIMIT) heading += delta * radial;
		float radians = elevation * HairMath.DEG_TO_RAD;
		float cos = (float) Math.cos(radians);
		return new Vector3f((float) Math.sin(heading) * cos, (float) Math.sin(radians), (float) Math.cos(heading) * cos).normalize();
	}

	private static float wrapRadians(float radians) {
		float wrapped = (float) (radians % (Math.PI * 2.0));
		if (wrapped > Math.PI) wrapped -= (float) (Math.PI * 2.0);
		if (wrapped < -Math.PI) wrapped += (float) (Math.PI * 2.0);
		return wrapped;
	}

	private static void rotateStrand(HairStrand strand, Quaternionf baseRotation, Vector3f from, Vector3f to) {
		Quaternionf rotation = new Quaternionf().rotationTo(from, to).mul(baseRotation);
		Vector3f euler = rotation.getEulerAnglesXYZ(new Vector3f());
		strand.setRotation(euler.x * HairMath.RAD_TO_DEG, euler.y * HairMath.RAD_TO_DEG, euler.z * HairMath.RAD_TO_DEG);
	}

	private static void slerp(Vector3f from, Vector3f to, float factor, Vector3f out) {
		float dot = HairMath.clamp(from.dot(to), -1.0f, 1.0f);
		float angle = (float) Math.acos(dot);
		if (angle < 1.0e-4f) {
			out.set(from);
			return;
		}
		float sin = (float) Math.sin(angle);
		float a = (float) Math.sin((1.0f - factor) * angle) / sin;
		float b = (float) Math.sin(factor * angle) / sin;
		out.set(from).mul(a).fma(b, to).normalize();
	}

	private record SpikeProfile(FaceLift[] lifts, FaceCurl[] curls, float radial, float straighten, float taper, float lengthScale, boolean singleBang) {}

	private record FaceLift(float targetElevation, float upright, float hanging) {
		float amount(float elevation) {
			return elevation >= 0.0f ? upright : HairMath.lerp(Math.min(1.0f, -elevation / 90.0f), upright, hanging);
		}
	}

	private record FaceCurl(float primary, float secondary) {
		float target(CustomHair.HairFace face, int index) {
			int column = index % face.cols;
			boolean outer = column == 0 || column == face.cols - 1;
			return (index / face.cols + (outer ? 0 : 1)) % 2 == 0 ? primary : secondary;
		}
	}

	private record CoverSample(CustomHair.HairFace face, int index, Vector3f position, float radius) {}
}
