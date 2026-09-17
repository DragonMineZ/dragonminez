package com.dragonminez.common.hair;

public final class HairPhysicsProfile {
	private static final HairPhysicsProfile BASE_DEFAULT = new HairPhysicsProfile(0.30f, 1.00f, 0.10f, 50.0f);
	private static final HairPhysicsProfile SPIKY_DEFAULT = new HairPhysicsProfile(0.45f, 1.00f, 0.03f, 30.0f);
	private static final HairPhysicsProfile LONG_DEFAULT = new HairPhysicsProfile(0.32f, 0.95f, 0.12f, 70.0f);
	private static final HairPhysicsProfile WILD_DEFAULT = new HairPhysicsProfile(0.30f, 1.00f, 0.12f, 55.0f);

	private float stiffness;
	private float damping;
	private float gravity;
	private float maxDeviation;

	private HairPhysicsProfile(float stiffness, float damping, float gravity, float maxDeviation) {
		this.stiffness = stiffness;
		this.damping = damping;
		this.gravity = gravity;
		this.maxDeviation = maxDeviation;
	}

	public static HairPhysicsProfile defaultFor(HairStyleSlot slot) {
		if (slot == null) return BASE_DEFAULT;
		return switch (slot) {
			case BASE -> BASE_DEFAULT;
			case SSJ, SSJ2 -> SPIKY_DEFAULT;
			case SSJ3 -> LONG_DEFAULT;
			case SSJ4 -> WILD_DEFAULT;
		};
	}

	public float getStiffness() {
		return stiffness;
	}

	public float getDamping() {
		return damping;
	}

	public float getGravity() {
		return gravity;
	}

	public float getMaxDeviation() {
		return maxDeviation;
	}

	public HairPhysicsProfile copy() {
		return new HairPhysicsProfile(stiffness, damping, gravity, maxDeviation);
	}

	public void lerpInto(HairPhysicsProfile target, HairPhysicsProfile other, float factor) {
		target.stiffness = HairMath.lerp(factor, stiffness, other.stiffness);
		target.damping = HairMath.lerp(factor, damping, other.damping);
		target.gravity = HairMath.lerp(factor, gravity, other.gravity);
		target.maxDeviation = HairMath.lerp(factor, maxDeviation, other.maxDeviation);
	}

	public void copyFrom(HairPhysicsProfile other) {
		this.stiffness = other.stiffness;
		this.damping = other.damping;
		this.gravity = other.gravity;
		this.maxDeviation = other.maxDeviation;
	}
}
