package com.dragonminez.client.flight;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class FlightAttitude {
	private static final float DEG = (float) (Math.PI / 180.0);

	private static final float INPUT_TAU = 0.045F;
	private static final float BODY_FOLLOW_RATE = 13.0F;
	private static final float LEVEL_RATE = 8.0F;
	private static final float MAX_LEVEL_SPEED = 260.0F;
	private static final float LEVEL_FADE_START = 0.15F;
	private static final float LEVEL_FADE_END = 0.6F;
	private static final float YAW_RATE_SMOOTH = 10.0F;
	private static final float BANK_PER_YAW_RATE = 0.3F;
	private static final float BANK_MAX = 50.0F;
	private static final float STRAFE_BANK = 24.0F;
	private static final float BANK_RATE = 7.0F;
	private static final float SPIN_DURATION = 0.55F;

	private final Quaternionf aim = new Quaternionf();
	private final Quaternionf body = new Quaternionf();
	private final Quaternionf scratch = new Quaternionf();
	private final Vector3f forward = new Vector3f();
	private final Vector3f up = new Vector3f();
	private final Vector3f right = new Vector3f();

	private float pendingYaw;
	private float pendingPitch;
	private float yawRate;
	private float bank;
	private float strafe;
	private float spinProgress = 1.0F;
	private float spinDirection;

	private float yaw;
	private float pitch;
	private float roll;

	public void reset(float yawDegrees, float pitchDegrees) {
		euler(yawDegrees, pitchDegrees, aim);
		body.set(aim);
		pendingYaw = 0.0F;
		pendingPitch = 0.0F;
		yawRate = 0.0F;
		bank = 0.0F;
		strafe = 0.0F;
		spinProgress = 1.0F;
		yaw = yawDegrees;
		pitch = pitchDegrees;
		roll = 0.0F;
	}

	public void resync(float yawDegrees, float pitchDegrees) {
		euler(yawDegrees, pitchDegrees, aim).rotateZ(-roll * DEG);
		yaw = yawDegrees;
		pitch = pitchDegrees;
	}

	public void addInput(float yawDegrees, float pitchDegrees) {
		pendingYaw += yawDegrees;
		pendingPitch += pitchDegrees;
	}

	public void setStrafe(float strafe) {
		this.strafe = strafe;
	}

	public void spin(float direction) {
		if (spinProgress < 1.0F) return;
		spinDirection = direction;
		spinProgress = 0.0F;
	}

	public void step(float dt) {
		if (dt <= 0.0F) return;

		float take = 1.0F - (float) Math.exp(-dt / INPUT_TAU);
		float stepYaw = pendingYaw * take;
		float stepPitch = pendingPitch * take;
		pendingYaw -= stepYaw;
		pendingPitch -= stepPitch;

		aim.transform(forward.set(0.0F, 0.0F, -1.0F));
		aim.transform(up.set(0.0F, 1.0F, 0.0F));
		float horizon = smoothstep(LEVEL_FADE_START, LEVEL_FADE_END, (float) Math.sqrt(forward.x * forward.x + forward.z * forward.z));
		float worldYaw = horizon * clamp(Math.abs(up.y) * 4.0F, 0.0F, 1.0F);

		aim.rotateLocalY(-stepYaw * worldYaw * Math.signum(up.y) * DEG);
		aim.rotateY(-stepYaw * (1.0F - worldYaw) * DEG);
		aim.rotateX(-stepPitch * DEG);

		level(dt);
		aim.normalize();

		body.slerp(aim, 1.0F - (float) Math.exp(-dt * BODY_FOLLOW_RATE));

		yawRate += (stepYaw / dt - yawRate) * (1.0F - (float) Math.exp(-dt * YAW_RATE_SMOOTH));
		float bankTarget = clamp(yawRate * BANK_PER_YAW_RATE, -BANK_MAX, BANK_MAX) + strafe * STRAFE_BANK;
		bank += (bankTarget - bank) * (1.0F - (float) Math.exp(-dt * BANK_RATE));

		if (spinProgress < 1.0F) spinProgress = Math.min(1.0F, spinProgress + dt / SPIN_DURATION);

		decompose();
	}

	private void level(float dt) {
		aim.transform(forward.set(0.0F, 0.0F, -1.0F));
		aim.transform(up.set(0.0F, 1.0F, 0.0F));
		aim.transform(right.set(1.0F, 0.0F, 0.0F));

		float horizontal = (float) Math.sqrt(forward.x * forward.x + forward.z * forward.z);
		float weight = smoothstep(LEVEL_FADE_START, LEVEL_FADE_END, horizontal);
		if (weight <= 0.0F) return;

		float idealX = -forward.x * forward.y / horizontal;
		float idealY = (1.0F - forward.y * forward.y) / horizontal;
		float idealZ = -forward.z * forward.y / horizontal;

		float error = (float) Math.atan2(
				-(idealX * right.x + idealY * right.y + idealZ * right.z),
				idealX * up.x + idealY * up.y + idealZ * up.z);

		float half = (float) (Math.PI / 2.0);
		if (error > half) error -= (float) Math.PI;
		else if (error < -half) error += (float) Math.PI;

		float correction = error * (1.0F - (float) Math.exp(-dt * LEVEL_RATE * weight));
		float limit = MAX_LEVEL_SPEED * DEG * dt;
		aim.rotateZ(clamp(correction, -limit, limit));
	}

	private void decompose() {
		aim.transform(forward.set(0.0F, 0.0F, -1.0F));
		float horizontal = (float) Math.sqrt(forward.x * forward.x + forward.z * forward.z);
		pitch = (float) -Math.asin(clamp(forward.y, -1.0F, 1.0F)) / DEG;
		if (horizontal > 1.0E-4F) yaw = (float) Math.atan2(-forward.x, forward.z) / DEG;

		euler(yaw, pitch, scratch).conjugate().mul(aim);
		float twist = 2.0F * (float) Math.atan2(scratch.z, scratch.w) / DEG;
		roll = -wrap(twist);
	}

	public float yaw() {
		return yaw;
	}

	public float pitch() {
		return pitch;
	}

	public float roll() {
		return roll;
	}

	public float bank() {
		return bank;
	}

	public float spinAngle() {
		if (spinProgress >= 1.0F) return 0.0F;
		float t = spinProgress;
		float eased = t * t * (3.0F - 2.0F * t);
		return spinDirection * 360.0F * eased;
	}

	public Vector3f bodyForward(Vector3f dest) {
		return body.transform(dest.set(0.0F, 0.0F, -1.0F));
	}

	public Vector3f bodyRight(Vector3f dest) {
		return body.transform(dest.set(1.0F, 0.0F, 0.0F));
	}

	public Quaternionf modelRotation(Quaternionf dest) {
		return dest.set(body).rotateZ(-(bank + spinAngle()) * DEG);
	}

	private static Quaternionf euler(float yawDegrees, float pitchDegrees, Quaternionf dest) {
		return dest.rotationY((180.0F - yawDegrees) * DEG).rotateX(-pitchDegrees * DEG);
	}

	private static float wrap(float degrees) {
		float wrapped = degrees % 360.0F;
		if (wrapped >= 180.0F) wrapped -= 360.0F;
		if (wrapped < -180.0F) wrapped += 360.0F;
		return wrapped;
	}

	private static float clamp(float value, float min, float max) {
		return value < min ? min : Math.min(value, max);
	}

	private static float smoothstep(float edge0, float edge1, float value) {
		float t = clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}
}
