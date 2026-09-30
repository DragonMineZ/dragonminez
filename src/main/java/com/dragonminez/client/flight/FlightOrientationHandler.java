package com.dragonminez.client.flight;

import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.common.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FlightOrientationHandler {
	private static final float VANILLA_TURN_SCALE = 0.15F;
	private static final float CAMERA_BANK_SHARE = 0.3F;
	private static final float EXIT_ROLL_RATE = 8.0F;
	private static final float MAX_FRAME_SECONDS = 0.1F;
	private static final float EXTERNAL_TURN_EPSILON = 1.0E-3F;

	private static final FlightAttitude ATTITUDE = new FlightAttitude();
	private static final Quaternionf MODEL_ROTATION = new Quaternionf();
	private static final Vector3f SCRATCH = new Vector3f();

	private static boolean active = false;
	private static long lastFrameNanos = 0L;
	private static float writtenYaw = 0F;
	private static float writtenPitch = 0F;
	private static float cameraRoll = 0F;

	public static void applyMouseDelta(double yawDelta, double pitchDelta) {
		float scale = VANILLA_TURN_SCALE * ConfigManager.getUserConfig().getFlightTurnSensitivity();
		ATTITUDE.addInput((float) yawDelta * scale, (float) pitchDelta * scale);
	}

	public static void frame(LocalPlayer player) {
		long now = System.nanoTime();
		float dt = lastFrameNanos == 0L ? 0F : Mth.clamp((now - lastFrameNanos) / 1.0E9F, 0F, MAX_FRAME_SECONDS);
		lastFrameNanos = now;

		if (player == null || !FlySkillEvent.getInstance().isFlyingFast(player)) {
			active = false;
			cameraRoll *= (float) Math.exp(-dt * EXIT_ROLL_RATE);
			if (Math.abs(cameraRoll) < 0.01F) cameraRoll = 0F;
			return;
		}

		if (!active) {
			ATTITUDE.reset(player.getYRot(), Mth.clamp(player.getXRot(), -90F, 90F));
			active = true;
		} else if (Math.abs(Mth.wrapDegrees(player.getYRot() - writtenYaw)) > EXTERNAL_TURN_EPSILON
				|| Math.abs(player.getXRot() - writtenPitch) > EXTERNAL_TURN_EPSILON) {
			ATTITUDE.resync(player.getYRot(), Mth.clamp(player.getXRot(), -90F, 90F));
		}

		if (!Minecraft.getInstance().isPaused()) ATTITUDE.step(dt);

		writeRotation(player);

		float bank = ConfigManager.getUserConfig().getCameraMovementDuringFlight() ? ATTITUDE.bank() * CAMERA_BANK_SHARE : 0F;
		cameraRoll = ATTITUDE.roll() + bank;
	}

	private static void writeRotation(LocalPlayer player) {
		float yaw = player.getYRot() + Mth.wrapDegrees(ATTITUDE.yaw() - player.getYRot());
		float pitch = ATTITUDE.pitch();
		float yawDelta = yaw - player.getYRot();
		float pitchDelta = pitch - player.getXRot();

		player.setYRot(yaw);
		player.setXRot(pitch);
		player.yRotO += yawDelta;
		player.xRotO += pitchDelta;

		if (Math.abs(yawDelta) > 90F) {
			player.yHeadRot += yawDelta;
			player.yHeadRotO += yawDelta;
			player.yBodyRot += yawDelta;
			player.yBodyRotO += yawDelta;
		}

		writtenYaw = yaw;
		writtenPitch = pitch;
	}

	public static boolean isActive() {
		return active;
	}

	public static float getCameraRoll() {
		return cameraRoll;
	}

	public static Quaternionf getModelRotation() {
		return ATTITUDE.modelRotation(MODEL_ROTATION);
	}

	public static Vec3 getForwardVector(LocalPlayer player) {
		if (player == null) return Vec3.ZERO;
		if (!active) return player.getLookAngle();
		ATTITUDE.bodyForward(SCRATCH);
		return new Vec3(SCRATCH.x(), SCRATCH.y(), SCRATCH.z());
	}

	public static Vec3 getRightVector(LocalPlayer player) {
		if (player == null) return Vec3.ZERO;
		if (!active) {
			Vec3 look = player.getLookAngle();
			Vec3 side = new Vec3(-look.z, 0, look.x);
			return side.lengthSqr() < 1.0E-6 ? Vec3.ZERO : side.normalize();
		}
		ATTITUDE.bodyRight(SCRATCH);
		return new Vec3(SCRATCH.x(), SCRATCH.y(), SCRATCH.z());
	}

	public static void setStrafe(float strafe) {
		ATTITUDE.setStrafe(strafe);
	}

	public static void barrelRoll(float direction) {
		if (active) ATTITUDE.spin(direction);
	}

	public static void reset() {
		active = false;
	}
}
