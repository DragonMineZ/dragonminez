package com.dragonminez.client.gui.hud;

public final class HudSmoother {
	private static final float MAX_FRAME_SECONDS = 0.25f;

	private final float tau;
	private final float snapEpsilon;
	private boolean initialized;
	private float value;
	private long lastNanos;

	public HudSmoother(float tauSeconds) {
		this(tauSeconds, 0.0005f);
	}

	public HudSmoother(float tauSeconds, float snapEpsilon) {
		this.tau = Math.max(0.001f, tauSeconds);
		this.snapEpsilon = snapEpsilon;
	}

	public float update(float target) {
		long now = System.nanoTime();
		if (!initialized || !Float.isFinite(value)) {
			snap(target);
			return value;
		}

		float dt = (now - lastNanos) / 1_000_000_000.0f;
		lastNanos = now;
		if (dt <= 0.0f) return value;
		if (dt > MAX_FRAME_SECONDS * 4.0f) {
			value = target;
			return value;
		}
		if (dt > MAX_FRAME_SECONDS) dt = MAX_FRAME_SECONDS;

		value += (target - value) * (1.0f - (float) Math.exp(-dt / tau));
		if (Math.abs(target - value) <= snapEpsilon) value = target;
		return value;
	}

	public void snap(float target) {
		initialized = true;
		value = target;
		lastNanos = System.nanoTime();
	}

	public float value() {
		return value;
	}
}
