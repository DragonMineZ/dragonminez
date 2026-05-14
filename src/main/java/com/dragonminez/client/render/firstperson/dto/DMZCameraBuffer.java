package com.dragonminez.client.render.firstperson.dto;

import net.minecraft.world.phys.Vec3;

public class DMZCameraBuffer {
	private static Vec3 targetPosition = Vec3.ZERO;
	private static Vec3 lastPosition = Vec3.ZERO;
	private static boolean initialized = false;

	public static void updateTarget(Vec3 pos) {
		targetPosition = pos;
	}

	public static Vec3 getSmoothedPosition(float smoothFactor) {
		if (!initialized || lastPosition.distanceToSqr(targetPosition) > 10.0) {
			lastPosition = targetPosition;
			initialized = true;
		}
		lastPosition = lastPosition.add(targetPosition.subtract(lastPosition).scale(smoothFactor));
		return lastPosition;
	}

	public static void reset() {
		initialized = false;
	}
}