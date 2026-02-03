package com.dragonminez.client.flight;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FlightOrientationHandler {
	private static final float TURN_SENSITIVITY = 0.15F;
	private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

	private static final Quaternionf ORIENTATION = new Quaternionf();
	private static final Vector3f FORWARD = new Vector3f();

	private static float yaw = 0F;
	private static float pitch = 0F;
	private static boolean active = false;

	public static void applyMouseDelta(LocalPlayer player, double yawDelta, double pitchDelta) {
		if (player == null) return;
		if (!active) {
			syncFromPlayer(player);
			active = true;
		}

		float deltaYaw = (float) yawDelta * TURN_SENSITIVITY;
		float deltaPitch = (float) pitchDelta * TURN_SENSITIVITY;

		yaw += deltaYaw;
		pitch += deltaPitch;
		player.yRotO += deltaYaw;
		player.xRotO += deltaPitch;
		rebaseAngles(player);

		updateOrientation();

		player.setYRot(yaw);
		player.setXRot(pitch);
	}

	public static Vec3 getForwardVector(LocalPlayer player) {
		if (player == null) return Vec3.ZERO;
		if (!active) {
			syncFromPlayer(player);
		}

		FORWARD.set(0F, 0F, 1F);
		ORIENTATION.transform(FORWARD);

		return new Vec3(FORWARD.x(), FORWARD.y(), FORWARD.z());
	}

	public static void reset() {
		active = false;
		yaw = 0F;
		pitch = 0F;
		ORIENTATION.identity();
	}

	private static void syncFromPlayer(LocalPlayer player) {
		yaw = player.getYRot();
		pitch = player.getXRot();
		updateOrientation();
	}

	private static void rebaseAngles(LocalPlayer player) {
		float wrappedYaw = Mth.wrapDegrees(yaw);
		float yawOffset = yaw - wrappedYaw;
		if (yawOffset != 0F) {
			yaw = wrappedYaw;
			player.yRotO -= yawOffset;
		}

		float wrappedPitch = Mth.wrapDegrees(pitch);
		float pitchOffset = pitch - wrappedPitch;
		if (pitchOffset != 0F) {
			pitch = wrappedPitch;
			player.xRotO -= pitchOffset;
		}
	}

	private static void updateOrientation() {
		ORIENTATION.rotationYXZ(-yaw * DEG_TO_RAD, pitch * DEG_TO_RAD, 0F);
	}
}
