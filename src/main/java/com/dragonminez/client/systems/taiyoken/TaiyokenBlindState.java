package com.dragonminez.client.systems.taiyoken;

public final class TaiyokenBlindState {

	private static int ticksRemaining = 0;
	private static int totalTicks = 0;

	private TaiyokenBlindState() {}

	public static void startBlind(int durationTicks) {
		if (durationTicks <= 0) {
			clear();
			return;
		}
		ticksRemaining = durationTicks;
		totalTicks = durationTicks;
	}

	public static void tick() {
		if (ticksRemaining > 0) ticksRemaining--;
	}

	public static boolean isActive() {
		return ticksRemaining > 0;
	}

	public static int getTicksRemaining() {
		return ticksRemaining;
	}

	public static float getProgress() {
		if (totalTicks <= 0) return 0.0f;
		return (float) ticksRemaining / (float) totalTicks;
	}

	public static void clear() {
		ticksRemaining = 0;
		totalTicks = 0;
	}
}
