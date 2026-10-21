package com.dragonminez.client.gui.character.minigames;

final class SmoothValue {
	private float value;
	private boolean initialized;
	private long lastNanos;

	float update(float target, float speed) {
		long now = System.nanoTime();
		if (!initialized) {
			value = target;
			initialized = true;
			lastNanos = now;
			return value;
		}
		float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
		lastNanos = now;
		value += (target - value) * (1f - (float) Math.exp(-dt * speed));
		return value;
	}

	float get() {
		return value;
	}

	void snap(float target) {
		value = target;
		initialized = true;
		lastNanos = System.nanoTime();
	}
}
