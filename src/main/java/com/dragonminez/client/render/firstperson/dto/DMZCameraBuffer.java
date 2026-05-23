package com.dragonminez.client.render.firstperson.dto;

import org.joml.Vector3f;

public class DMZCameraBuffer {
	private static final Vector3f currentOffset = new Vector3f(0, 0, 0);

	public static Vector3f getSmoothedOffset(Vector3f targetOffset, float smoothFactor) {
		currentOffset.lerp(targetOffset, smoothFactor);
		return currentOffset;
	}

	public static void reset() {
		currentOffset.set(0, 0, 0);
	}
}