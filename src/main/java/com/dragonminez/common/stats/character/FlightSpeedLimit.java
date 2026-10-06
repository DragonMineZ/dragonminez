package com.dragonminez.common.stats.character;

public final class FlightSpeedLimit {
	private FlightSpeedLimit() {}

	public static int clampPercent(int percent) {
		return Math.max(5, Math.min(100, (percent / 5) * 5));
	}

	public static float scaleSpeed(float speed, int percent) {
		return speed * clampPercent(percent) / 100.0F;
	}

	public static float decelerateTowardLimit(float speed, float limit, float deceleration) {
		return speed <= limit ? speed : Math.max(limit, speed - deceleration);
	}
}
