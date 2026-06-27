package com.dragonminez.client.render.firstperson.dto;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class DMZCameraBuffer {
	private static final Vector3f currentOffset = new Vector3f(0, 0, 0);
	@Getter
	@Setter
	private static Vec3 firstPersonShift = Vec3.ZERO;

	public static Vector3f getSmoothedOffset(Vector3f targetOffset, float smoothFactor) {
		currentOffset.lerp(targetOffset, smoothFactor);
		return currentOffset;
	}

	public static void reset() {
		currentOffset.set(0, 0, 0);
		firstPersonShift = Vec3.ZERO;
	}
}