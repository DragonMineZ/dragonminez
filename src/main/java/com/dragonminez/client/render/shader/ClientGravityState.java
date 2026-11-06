package com.dragonminez.client.render.shader;

import com.dragonminez.common.config.ConfigManager;

public final class ClientGravityState {
	private static volatile float machineGravity = 0.0F;
	private static volatile float environmentalGravity = 1.0F;
	private static volatile float netGravity = 0.0F;
	private static volatile float statMult = 1.0F;
	private static volatile float tpGravityMult = 1.0F;
	private static volatile int idealWeight = 0;
	private static volatile int totalWeight = 0;
	private static volatile int effectiveWeight = 0;
	private static volatile float loadRatio = 0.0F;
	private static volatile float weightTpMult = 1.0F;
	private static volatile int zone = 0;

	private ClientGravityState() {
	}

	public static void update(float machineGravity, float environmentalGravity, float netGravity, float statMult, float tpGravityMult, int idealWeight, int totalWeight, int effectiveWeight, float loadRatio, float weightTpMult, int zone) {
		ClientGravityState.machineGravity = Math.max(0.0F, machineGravity);
		ClientGravityState.environmentalGravity = environmentalGravity;
		ClientGravityState.netGravity = netGravity;
		ClientGravityState.statMult = statMult;
		ClientGravityState.tpGravityMult = tpGravityMult;
		ClientGravityState.idealWeight = idealWeight;
		ClientGravityState.totalWeight = totalWeight;
		ClientGravityState.effectiveWeight = effectiveWeight;
		ClientGravityState.loadRatio = loadRatio;
		ClientGravityState.weightTpMult = weightTpMult;
		ClientGravityState.zone = zone;
	}

	public static void reset() {
		update(0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0, 0, 0, 0.0F, 1.0F, 0);
	}

	public static float getShaderIntensity() {
		if (machineGravity <= 0.0F) return 0.0F;
		double forMax = ConfigManager.getServerConfig().getGravity().getDeviceShaderGravityForMax();
		double curved = Math.sqrt(Math.min(1.0, machineGravity / forMax));
		return (float) Math.min(1.0, Math.max(0.35, curved));
	}

	public static float getEnvironmentalGravity() {
		return ClientGravityState.environmentalGravity;
	}

	public static float getNetGravity() {
		return ClientGravityState.netGravity;
	}

	public static float getStatMult() {
		return ClientGravityState.statMult;
	}

	public static float getTpGravityMult() {
		return ClientGravityState.tpGravityMult;
	}

	public static int getIdealWeight() {
		return ClientGravityState.idealWeight;
	}

	public static int getTotalWeight() {
		return ClientGravityState.totalWeight;
	}

	public static int getEffectiveWeight() {
		return ClientGravityState.effectiveWeight;
	}

	public static float getLoadRatio() {
		return ClientGravityState.loadRatio;
	}

	public static float getWeightTpMult() {
		return ClientGravityState.weightTpMult;
	}

	public static int getZone() {
		return ClientGravityState.zone;
	}
}
