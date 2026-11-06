package com.dragonminez.common.hair;

public final class HairMath {
	public static final float PIXEL = 1.0f / 16.0f;
	public static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
	public static final float RAD_TO_DEG = (float) (180.0 / Math.PI);

	private HairMath() {}

	public static float lerp(float factor, float from, float to) {
		return from + (to - from) * factor;
	}

	public static float clamp(float value, float min, float max) {
		return value < min ? min : Math.min(value, max);
	}

	public static float clampFinite(float value, float min, float max, float fallback) {
		if (!Float.isFinite(value)) return fallback;
		return clamp(value, min, max);
	}

	public static float smoothstep(float value) {
		float t = clamp(value, 0.0f, 1.0f);
		return t * t * (3.0f - 2.0f * t);
	}

	public static float wrapDegrees(float degrees) {
		float wrapped = degrees % 360.0f;
		if (wrapped >= 180.0f) wrapped -= 360.0f;
		if (wrapped < -180.0f) wrapped += 360.0f;
		return wrapped;
	}
}
