package com.dragonminez.common.stats.character;

public class FlightSpeedLimitCheck {
	public static void main(String[] args) {
		assert FlightSpeedLimit.clampPercent(37) == 35;
		assert FlightSpeedLimit.clampPercent(0) == 5;
		assert FlightSpeedLimit.clampPercent(105) == 100;
		assert FlightSpeedLimit.scaleSpeed(1.0F, 50) == 0.5F;
		assert Math.abs(FlightSpeedLimit.decelerateTowardLimit(1.0F, 0.5F, 0.1F) - 0.9F) < 0.0001F;
		assert FlightSpeedLimit.decelerateTowardLimit(0.55F, 0.5F, 0.1F) == 0.5F;
	}
}
