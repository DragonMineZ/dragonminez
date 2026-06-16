package com.dragonminez.client.render.layer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuraTintTracker {
	public static final float FADE_SPEED = 0.025f;

	public static final float DARK_TINT_THRESHOLD = 0.35f;
	public static final float DARK_TINT_FLOOR = 0.15f;

	private static final Map<Integer, Float> PROGRESS = new ConcurrentHashMap<>();
	private static final Map<Integer, Long>	LAST_UPDATE = new ConcurrentHashMap<>();
	private static final Map<Integer, Long>	LAST_SEEN = new ConcurrentHashMap<>();

	private AuraTintTracker() {}

	public static float update(int entityId, long gameTime, boolean shouldFadeIn) {
		float progress = PROGRESS.getOrDefault(entityId, 0.0f);

		Long lastSeen = LAST_SEEN.get(entityId);
		if (lastSeen == null || gameTime - lastSeen > 2L) progress = 0.0f;
		LAST_SEEN.put(entityId, gameTime);

		Long lastUpdate = LAST_UPDATE.get(entityId);
		if (lastUpdate == null || lastUpdate != gameTime) {
			if (shouldFadeIn) progress = Math.min(1.0f, progress + FADE_SPEED);
			else progress = Math.max(0.0f, progress - FADE_SPEED);
			PROGRESS.put(entityId, progress);
			LAST_UPDATE.put(entityId, gameTime);
		}

		return progress;
	}

	public static float get(int entityId) {
		return PROGRESS.getOrDefault(entityId, 0.0f);
	}

	public static float darkTintScale(float r, float g, float b) {
		float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
		float scale = luminance >= DARK_TINT_THRESHOLD ? 1.0f : luminance / DARK_TINT_THRESHOLD;
		return DARK_TINT_FLOOR + (1.0f - DARK_TINT_FLOOR) * scale;
	}

	public static float darkTintScale(float[] rgb) {
		if (rgb == null || rgb.length < 3) return 1.0f;
		return darkTintScale(rgb[0], rgb[1], rgb[2]);
	}
}