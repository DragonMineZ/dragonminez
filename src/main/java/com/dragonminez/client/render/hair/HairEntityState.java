package com.dragonminez.client.render.hair;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairMath;
import com.dragonminez.common.hair.HairPhysicsProfile;
import com.dragonminez.common.hair.HairPoseBuilder;
import com.dragonminez.common.hair.HairStrand;
import com.dragonminez.common.hair.HairStyleSlot;
import com.dragonminez.common.hair.StrandPose;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class HairEntityState {
	public static final int STRAND_COUNT;
	private static final int[] FACE_OFFSETS = new int[CustomHair.HairFace.values().length];

	static {
		int offset = 0;
		for (CustomHair.HairFace face : CustomHair.HairFace.values()) {
			FACE_OFFSETS[face.ordinal()] = offset;
			offset += face.maxStrands;
		}
		STRAND_COUNT = offset;
	}

	private final StrandPose[] poses = new StrandPose[STRAND_COUNT];
	private final StrandChain[] chains = new StrandChain[STRAND_COUNT];
	private final boolean[] present = new boolean[STRAND_COUNT];
	private final HairStrand scratchStrand = new HairStrand();
	private final HairColliders colliders = new HairColliders();

	private CustomHair lastFrom;
	private CustomHair lastTo;
	private long lastFromRevision = Long.MIN_VALUE;
	private long lastToRevision = Long.MIN_VALUE;
	private float lastFactor = Float.NaN;
	private HairStyleSlot lastFromSlot;
	private HairStyleSlot lastToSlot;

	private boolean simulated;
	private long lastNanos;
	private double accumulator;
	private float renderAlpha;
	private float windPhase;
	private long lastSeenMs;
	private final Vector3d particleAnchor = new Vector3d();
	private final Vector3f stepHeadPosition = new Vector3f();
	private final Quaternionf stepHeadRotation = new Quaternionf();
	private float stepHeadScale = 1.0f;

	private final Vector3f frameHeadPosition = new Vector3f();
	private final Quaternionf frameHeadRotation = new Quaternionf();
	private final Quaternionf substepInverse = new Quaternionf();
	private final Quaternionf parentRotation = new Quaternionf();
	private final Quaternionf restRotation = new Quaternionf();
	private final Quaternionf worldRotation = new Quaternionf();
	private final Quaternionf alignment = new Quaternionf();
	private final Quaternionf localRotation = new Quaternionf();
	private final Vector3f startHeadPosition = new Vector3f();
	private final Quaternionf startHeadRotation = new Quaternionf();
	private final Vector3f cursor = new Vector3f();
	private final Vector3f start = new Vector3f();
	private final Vector3f restDirection = new Vector3f();
	private final Vector3f target = new Vector3f();
	private final Vector3f next = new Vector3f();
	private final Vector3f velocity = new Vector3f();
	private final Vector3f targetVelocity = new Vector3f();
	private final Vector3f predicted = new Vector3f();
	private final Vector3f lastTarget = new Vector3f();
	private final Vector3f acceleration = new Vector3f();
	private final Vector3f direction = new Vector3f();
	private final Vector3f scratch = new Vector3f();
	private final Vector3f scales = new Vector3f();

	public HairEntityState() {
		for (int i = 0; i < STRAND_COUNT; i++) poses[i] = new StrandPose();
	}

	public static int flatIndex(CustomHair.HairFace face, int index) {
		return FACE_OFFSETS[face.ordinal()] + index;
	}

	public StrandPose pose(int flat) {
		return poses[flat];
	}

	public boolean isPresent(int flat) {
		return present[flat];
	}

	public HairColliders colliders() {
		return colliders;
	}

	public void touch(long nowMs) {
		lastSeenMs = nowMs;
	}

	public long lastSeenMs() {
		return lastSeenMs;
	}

	public void updatePoses(CustomHair from, CustomHair to, float factor, HairStyleSlot fromSlot, HairStyleSlot toSlot) {
		if (from == null) from = new CustomHair();
		if (to == null) to = from;
		if (from == lastFrom && to == lastTo && from.getRevision() == lastFromRevision && to.getRevision() == lastToRevision
				&& factor == lastFactor && fromSlot == lastFromSlot && toSlot == lastToSlot) return;

		lastFrom = from;
		lastTo = to;
		lastFromRevision = from.getRevision();
		lastToRevision = to.getRevision();
		lastFactor = factor;
		lastFromSlot = fromSlot;
		lastToSlot = toSlot;

		boolean fromOnly = factor <= 0.0001f;
		boolean toOnly = factor >= 0.9999f;
		float blend = fromOnly || toOnly ? factor : HairMath.smoothstep(factor);

		for (CustomHair.HairFace face : CustomHair.HairFace.values()) {
			for (int index = 0; index < face.maxStrands; index++) {
				int flat = flatIndex(face, index);
				HairStrand fromStrand = from.getStrand(face, index);
				HairStrand toStrand = to.getStrand(face, index);
				boolean fromVisible = fromStrand != null && fromStrand.isVisible();
				boolean toVisible = toStrand != null && toStrand.isVisible();

				boolean visible = fromOnly ? fromVisible : (toOnly ? toVisible : (fromVisible || toVisible));
				present[flat] = visible;
				if (!visible) {
					if (chains[flat] != null) chains[flat].segments = -1;
					continue;
				}

				StrandPose pose = poses[flat];
				if (fromOnly) {
					HairPoseBuilder.build(fromStrand, face, index, fromSlot, pose);
				} else if (toOnly) {
					HairPoseBuilder.build(toStrand, face, index, toSlot, pose);
					pose.colorFactor = 1.0f;
				} else {
					HairPoseBuilder.buildBlend(fromStrand, fromVisible, toStrand, toVisible, blend, face, index, fromSlot, toSlot, pose, scratchStrand);
				}
			}
		}
	}

	public boolean matchesInputs(CustomHair from, CustomHair to, float factor, HairStyleSlot fromSlot, HairStyleSlot toSlot) {
		CustomHair resolvedTo = to != null ? to : from;
		return from == lastFrom && resolvedTo == lastTo && from != null && from.getRevision() == lastFromRevision
				&& resolvedTo.getRevision() == lastToRevision && factor == lastFactor && fromSlot == lastFromSlot && toSlot == lastToSlot;
	}

	public void invalidateSimulation() {
		simulated = false;
		for (StrandChain chain : chains) {
			if (chain != null) chain.valid = false;
		}
	}

	public boolean hasSimulatedFrames(int flat) {
		StrandChain chain = chains[flat];
		return chain != null && chain.valid && chain.segments == poses[flat].segments;
	}

	public Quaternionf renderRotation(int flat, int segment, Quaternionf out) {
		StrandChain chain = chains[flat];
		return chain.localPrevious[segment].slerp(chain.localCurrent[segment], renderAlpha, out);
	}

	public void simulate(Matrix4f headToAnchor, double anchorX, double anchorY, double anchorZ, long nowNanos,
						 float chargeProgress, float auraIntensity, int maxSubsteps) {
		headToAnchor.getTranslation(frameHeadPosition);
		headToAnchor.getNormalizedRotation(frameHeadRotation);
		headToAnchor.getScale(scales);
		float frameHeadScale = Math.max(1.0e-3f, (scales.x + scales.y + scales.z) / 3.0f);

		if (!simulated) {
			restart(anchorX, anchorY, anchorZ, frameHeadScale, nowNanos);
			return;
		}

		double elapsed = (nowNanos - lastNanos) / 1.0e9;
		lastNanos = nowNanos;
		double jump = particleAnchor.distance(anchorX, anchorY, anchorZ);
		if (elapsed > HairSimulation.RESET_AFTER_SECONDS || jump > HairSimulation.TELEPORT_DISTANCE) {
			restart(anchorX, anchorY, anchorZ, frameHeadScale, nowNanos);
			return;
		}
		if (elapsed > HairSimulation.MAX_FRAME_SECONDS) {
			zeroVelocities();
			elapsed = HairSimulation.STEP_SECONDS;
		}

		accumulator += elapsed;
		int steps = (int) (accumulator / HairSimulation.STEP_SECONDS);
		if (steps > maxSubsteps) {
			steps = maxSubsteps;
			accumulator = steps * HairSimulation.STEP_SECONDS;
		}

		double startX = particleAnchor.x;
		double startY = particleAnchor.y;
		double startZ = particleAnchor.z;
		startHeadPosition.set(stepHeadPosition);
		startHeadRotation.set(stepHeadRotation);
		float startHeadScale = stepHeadScale;

		for (int i = 1; i <= steps; i++) {
			float fraction = (float) i / steps;
			double stepAnchorX = startX + (anchorX - startX) * fraction;
			double stepAnchorY = startY + (anchorY - startY) * fraction;
			double stepAnchorZ = startZ + (anchorZ - startZ) * fraction;
			shiftParticles((float) (stepAnchorX - particleAnchor.x), (float) (stepAnchorY - particleAnchor.y), (float) (stepAnchorZ - particleAnchor.z));
			particleAnchor.set(stepAnchorX, stepAnchorY, stepAnchorZ);

			startHeadPosition.lerp(frameHeadPosition, fraction, stepHeadPosition);
			startHeadRotation.slerp(frameHeadRotation, fraction, stepHeadRotation);
			stepHeadScale = HairMath.lerp(fraction, startHeadScale, frameHeadScale);

			windPhase += (float) HairSimulation.STEP_SECONDS * HairMath.lerp(chargeProgress, HairSimulation.IDLE_WIND_SPEED, HairSimulation.CHARGE_WIND_SPEED);
			substep(chargeProgress, auraIntensity);
			accumulator -= HairSimulation.STEP_SECONDS;
		}

		if (accumulator < 0.0) accumulator = 0.0;
		renderAlpha = (float) Math.min(1.0, accumulator / HairSimulation.STEP_SECONDS);
	}

	private void restart(double anchorX, double anchorY, double anchorZ, float headScale, long nowNanos) {
		particleAnchor.set(anchorX, anchorY, anchorZ);
		stepHeadPosition.set(frameHeadPosition);
		stepHeadRotation.set(frameHeadRotation);
		stepHeadScale = headScale;
		accumulator = 0.0;
		renderAlpha = 1.0f;
		lastNanos = nowNanos;
		for (int flat = 0; flat < STRAND_COUNT; flat++) {
			if (present[flat]) resetChain(flat);
		}
		simulated = true;
	}

	private void shiftParticles(float dx, float dy, float dz) {
		if (dx == 0.0f && dy == 0.0f && dz == 0.0f) return;
		for (int flat = 0; flat < STRAND_COUNT; flat++) {
			StrandChain chain = chains[flat];
			if (chain == null || chain.segments <= 0) continue;
			chain.root.sub(dx, dy, dz);
			for (int k = 0; k < chain.segments; k++) {
				chain.ends[k].sub(dx, dy, dz);
				chain.previousEnds[k].sub(dx, dy, dz);
				chain.previousTargets[k].sub(dx, dy, dz);
			}
		}
	}

	private void zeroVelocities() {
		for (StrandChain chain : chains) {
			if (chain == null || chain.segments <= 0) continue;
			for (int k = 0; k < chain.segments; k++) chain.previousEnds[k].set(chain.ends[k]);
			chain.targetsValid = false;
		}
	}

	private void resetChain(int flat) {
		StrandPose pose = poses[flat];
		StrandChain chain = chains[flat];
		if (chain == null) chain = chains[flat] = new StrandChain();
		chain.ensure(pose.segments);
		chain.segments = pose.segments;

		stepHeadRotation.transform(cursor.set(pose.root).mul(stepHeadScale)).add(stepHeadPosition);
		chain.root.set(cursor);
		parentRotation.set(stepHeadRotation).mul(pose.baseRotation);
		for (int k = 0; k < pose.segments; k++) {
			parentRotation.transform(start.set(pose.localOffsets[k]).mul(stepHeadScale)).add(cursor);
			restRotation.set(parentRotation).mul(pose.relativeRotations[k]);
			restRotation.transform(cursor.set(0.0f, pose.lengths[k] * stepHeadScale, 0.0f)).add(start);
			chain.ends[k].set(cursor);
			chain.previousEnds[k].set(cursor);
			chain.previousTargets[k].set(cursor);
			chain.localPrevious[k].set(pose.restRotations[k]);
			chain.localCurrent[k].set(pose.restRotations[k]);
			parentRotation.set(restRotation);
		}
		chain.targetsValid = true;
		chain.valid = true;
	}

	private void resampleChain(int flat) {
		StrandPose pose = poses[flat];
		StrandChain chain = chains[flat];
		int oldSegments = chain.segments;
		Vector3f[] polyline = new Vector3f[oldSegments + 1];
		float[] cumulative = new float[oldSegments + 1];
		polyline[0] = new Vector3f(chain.root);
		for (int k = 0; k < oldSegments; k++) {
			polyline[k + 1] = new Vector3f(chain.ends[k]);
			cumulative[k + 1] = cumulative[k] + polyline[k + 1].distance(polyline[k]);
		}

		float totalRest = 0.0f;
		for (int k = 0; k < pose.segments; k++) totalRest += pose.lengths[k];

		chain.ensure(pose.segments);
		chain.segments = pose.segments;
		float oldTotal = cumulative[oldSegments];
		float travelled = 0.0f;
		for (int k = 0; k < pose.segments; k++) {
			travelled += pose.lengths[k];
			float wanted = totalRest > 0.0f ? travelled / totalRest * oldTotal : 0.0f;
			int segment = 0;
			while (segment < oldSegments - 1 && cumulative[segment + 1] < wanted) segment++;
			float span = cumulative[segment + 1] - cumulative[segment];
			float local = span > 1.0e-6f ? (wanted - cumulative[segment]) / span : 0.0f;
			polyline[segment].lerp(polyline[segment + 1], HairMath.clamp(local, 0.0f, 1.0f), chain.ends[k]);
			chain.previousEnds[k].set(chain.ends[k]);
			chain.localPrevious[k].set(pose.restRotations[k]);
			chain.localCurrent[k].set(pose.restRotations[k]);
		}
		chain.targetsValid = false;
		chain.valid = false;
	}

	private void substep(float chargeProgress, float auraIntensity) {
		float dt = (float) HairSimulation.STEP_SECONDS;
		float dtSquared = dt * dt;
		float dragFactor = HairSimulation.AIR_DRAG * dt;
		stepHeadRotation.conjugate(substepInverse);

		for (int flat = 0; flat < STRAND_COUNT; flat++) {
			if (!present[flat]) continue;
			StrandPose pose = poses[flat];
			if (pose.segments <= 0) continue;
			StrandChain chain = chains[flat];
			if (chain == null || chain.segments < 0) {
				resetChain(flat);
				chain = chains[flat];
			} else if (chain.segments != pose.segments) {
				if (chain.segments > 0) resampleChain(flat);
				else resetChain(flat);
			}

			HairPhysicsProfile physics = pose.physics;
			float cosLimit = (float) Math.cos(physics.getMaxDeviation() * HairMath.DEG_TO_RAD);
			float maxStep = HairSimulation.MAX_STEP_DISTANCE * stepHeadScale;
			boolean targetsValid = chain.targetsValid;

			stepHeadRotation.transform(cursor.set(pose.root).mul(stepHeadScale)).add(stepHeadPosition);
			chain.root.set(cursor);
			parentRotation.set(stepHeadRotation).mul(pose.baseRotation);

			for (int k = 0; k < pose.segments; k++) {
				float length = pose.lengths[k] * stepHeadScale;
				parentRotation.transform(start.set(pose.localOffsets[k]).mul(stepHeadScale)).add(cursor);
				restRotation.set(parentRotation).mul(pose.relativeRotations[k]);
				restRotation.transform(restDirection.set(0.0f, 1.0f, 0.0f));
				target.set(restDirection).mul(length).add(start);

				Vector3f current = chain.ends[k];
				Vector3f previousTarget = chain.previousTargets[k];
				lastTarget.set(targetsValid ? previousTarget : target);
				targetVelocity.set(target).sub(lastTarget);
				previousTarget.set(target);

				float stiffness = physics.getStiffness() * HairMath.lerp(pose.params[k], 1.0f, HairSimulation.TIP_STIFFNESS_RATIO);
				velocity.set(current).sub(chain.previousEnds[k]).sub(targetVelocity).mul(retention(stiffness, physics.getDamping()));
				float speed = velocity.length();
				if (speed > maxStep) velocity.mul(maxStep / speed);

				buildAcceleration(flat, k, physics, chargeProgress, auraIntensity);
				next.set(current).sub(lastTarget).mul(1.0f - stiffness)
						.add(target).add(velocity)
						.add(acceleration.x * dtSquared, acceleration.y * dtSquared, acceleration.z * dtSquared)
						.sub(targetVelocity.x * dragFactor, targetVelocity.y * dragFactor, targetVelocity.z * dragFactor);

				projectLength(start, length);
				predicted.set(next);
				limitAngle(start, length, cosLimit);
				float margin = Math.max(pose.widths[k], pose.depths[k]) * 0.5f * stepHeadScale + HairSimulation.COLLISION_PADDING;
				if (!colliders.isInsideHead(target, margin)) colliders.pushOutOfHead(next, margin);
				colliders.pushOutOfBody(next, margin);
				projectLength(start, length);
				limitAngle(start, length, cosLimit);
				boolean corrected = predicted.distanceSquared(next) > HairSimulation.CORRECTION_EPSILON * HairSimulation.CORRECTION_EPSILON;

				direction.set(next).sub(start);
				if (direction.lengthSquared() < 1.0e-12f) direction.set(restDirection);
				direction.normalize();

				if (corrected) chain.previousEnds[k].set(next).sub(targetVelocity);
				else chain.previousEnds[k].set(current);
				current.set(next);

				alignment.rotationTo(restDirection, direction);
				worldRotation.set(alignment).mul(restRotation);
				substepInverse.mul(worldRotation, localRotation);
				chain.localPrevious[k].set(chain.valid ? chain.localCurrent[k] : localRotation);
				chain.localCurrent[k].set(localRotation);

				cursor.set(next);
				parentRotation.set(worldRotation);
			}
			chain.targetsValid = true;
			chain.valid = true;
		}
	}

	private static float retention(float stiffness, float damping) {
		float root = 1.0f - (float) Math.sqrt(Math.max(0.0f, Math.min(1.0f, stiffness)));
		float critical = root * root;
		return 1.0f - HairMath.clamp(damping, 0.0f, 1.0f) * (1.0f - critical);
	}

	private void projectLength(Vector3f segmentStart, float length) {
		direction.set(next).sub(segmentStart);
		float distance = direction.length();
		if (distance < 1.0e-6f) direction.set(restDirection);
		else direction.div(distance);
		next.set(direction).mul(length).add(segmentStart);
	}

	private void limitAngle(Vector3f segmentStart, float length, float cosLimit) {
		direction.set(next).sub(segmentStart).normalize();
		if (!Float.isFinite(direction.x) || direction.dot(restDirection) >= cosLimit) return;
		scratch.set(restDirection).cross(direction);
		float axisLength = scratch.length();
		if (axisLength < 1.0e-6f) {
			scratch.set(Math.abs(restDirection.x) < 0.9f ? 1.0f : 0.0f, Math.abs(restDirection.x) < 0.9f ? 0.0f : 1.0f, 0.0f).cross(restDirection).normalize();
		} else {
			scratch.div(axisLength);
		}
		float limit = (float) Math.acos(HairMath.clamp(cosLimit, -1.0f, 1.0f));
		alignment.fromAxisAngleRad(scratch, limit);
		alignment.transform(direction.set(restDirection));
		next.set(direction).mul(length).add(segmentStart);
	}

	private void buildAcceleration(int flat, int segment, HairPhysicsProfile physics, float chargeProgress, float auraIntensity) {
		float energy = Math.max(auraIntensity, chargeProgress);
		float amplitude = HairSimulation.IDLE_WIND + energy * HairSimulation.AURA_WIND;
		float phase = windPhase + segment * HairSimulation.WIND_SEGMENT_PHASE + flat * HairSimulation.WIND_STRAND_PHASE;
		float gust = 0.65f + 0.35f * (float) Math.sin(windPhase * 0.37f + flat * 1.3f);
		acceleration.set(
				(float) Math.sin(phase) * amplitude * gust,
				-physics.getGravity() * HairSimulation.GRAVITY_ACCELERATION,
				(float) Math.cos(phase * 0.73f + 1.7f) * amplitude * gust * 0.8f);
		if (energy > 0.0f) {
			float flicker = (float) Math.sin(windPhase * 2.3f + segment * 0.9f + flat * 0.7f);
			acceleration.y += energy * HairSimulation.AURA_LIFT + chargeProgress * (HairSimulation.CHARGE_LIFT + flicker * HairSimulation.CHARGE_FLICKER);
		}
	}

	static final class StrandChain {
		private int segments = -1;
		private boolean valid;
		private boolean targetsValid;
		private final Vector3f root = new Vector3f();
		private Vector3f[] ends = new Vector3f[0];
		private Vector3f[] previousEnds = new Vector3f[0];
		private Vector3f[] previousTargets = new Vector3f[0];
		private Quaternionf[] localPrevious = new Quaternionf[0];
		private Quaternionf[] localCurrent = new Quaternionf[0];

		private void ensure(int count) {
			if (ends.length >= count) return;
			int capacity = Math.max(count, 4);
			ends = growVectors(ends, capacity);
			previousEnds = growVectors(previousEnds, capacity);
			previousTargets = growVectors(previousTargets, capacity);
			localPrevious = growQuaternions(localPrevious, capacity);
			localCurrent = growQuaternions(localCurrent, capacity);
		}

		private static Vector3f[] growVectors(Vector3f[] array, int capacity) {
			Vector3f[] grown = new Vector3f[capacity];
			for (int i = 0; i < capacity; i++) grown[i] = i < array.length ? array[i] : new Vector3f();
			return grown;
		}

		private static Quaternionf[] growQuaternions(Quaternionf[] array, int capacity) {
			Quaternionf[] grown = new Quaternionf[capacity];
			for (int i = 0; i < capacity; i++) grown[i] = i < array.length ? array[i] : new Quaternionf();
			return grown;
		}
	}
}
