package com.dragonminez.client.render.shader;

import com.dragonminez.common.config.ConfigManager;

/**
 * Client-side holder for the Gravity Device machine gravity currently affecting the local
 * player. Synced from the server once per second and used to drive the red distortion shader.
 */
public final class ClientGravityState {

	private static volatile float machineGravity = 0.0f;

	private ClientGravityState() {}

	public static void setMachineGravity(float value) {
		machineGravity = Math.max(0.0f, value);
	}

	public static float getMachineGravity() {
		return machineGravity;
	}

	/** @return shader intensity in [0,1] scaled by the configured gravity-for-max value. */
	public static float getShaderIntensity() {
		if (machineGravity <= 0.0f) return 0.0f;
		double forMax = ConfigManager.getServerConfig().getGravity().getDeviceShaderGravityForMax();
		// Square-root ramp so low gravity is already clearly visible, plus a minimum floor
		// so that any active gravity zone is immediately noticeable.
		double curved = Math.sqrt(Math.min(1.0, machineGravity / forMax));
		return (float) Math.min(1.0, Math.max(0.35, curved));
	}
}
