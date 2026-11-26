package com.dragonminez.client.render;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CameraProjectionCaptureTest {
	private static Matrix4f perspective() {
		return new Matrix4f().perspective((float) Math.PI / 2.0F, 2.0F, 0.1F, 100.0F);
	}

	@Test
	void projectsCameraRelativePointWithCorrectScreenAxes() {
		Vec3 camera = new Vec3(100.0, 64.0, -40.0);
		float[] out = new float[4];
		assertTrue(CameraProjectionCapture.project(perspective(), camera,
				camera.add(0.0, 0.0, -10.0), 800, 400, out));
		assertEquals(400.0F, out[0], 0.001F);
		assertEquals(200.0F, out[1], 0.001F);
		assertTrue(CameraProjectionCapture.project(perspective(), camera,
				camera.add(10.0, 10.0, -10.0), 800, 400, out));
		assertEquals(600.0F, out[0], 0.001F);
		assertEquals(0.0F, out[1], 0.001F);
	}

	@Test
	void usesCapturedViewRotationIncludingCameraRoll() {
		float[] out = new float[4];
		Matrix4f yaw = perspective().rotateY((float) Math.PI / 2.0F);
		assertTrue(CameraProjectionCapture.project(yaw, Vec3.ZERO, new Vec3(10, 0, 0), 800, 400, out));
		assertEquals(400.0F, out[0], 0.001F);
		assertEquals(200.0F, out[1], 0.001F);
		Matrix4f roll = perspective().rotateZ((float) Math.PI / 2.0F);
		assertTrue(CameraProjectionCapture.project(roll, Vec3.ZERO, new Vec3(10, 0, -10), 800, 400, out));
		assertEquals(400.0F, out[0], 0.001F);
		assertEquals(0.0F, out[1], 0.001F);
	}

	@Test
	void rejectsBehindCameraAndInvalidCoordinates() {
		float[] out = new float[4];
		assertFalse(CameraProjectionCapture.project(perspective(), Vec3.ZERO, new Vec3(0, 0, 10), 800, 400, out));
		assertFalse(CameraProjectionCapture.project(perspective(), Vec3.ZERO, Vec3.ZERO, 800, 400, out));
		assertFalse(CameraProjectionCapture.project(perspective(), Vec3.ZERO, new Vec3(Double.NaN, 0, -10), 800, 400, out));
	}
}
