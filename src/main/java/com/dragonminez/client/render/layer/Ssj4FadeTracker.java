package com.dragonminez.client.render.layer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Ssj4FadeTracker {
	public static final float FADE_OUT_SPEED = 0.12f;

	private static final Map<Integer, Float> PROGRESS = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_UPDATE = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_SEEN = new ConcurrentHashMap<>();
	private static final Map<Integer, String> LAST_KEY = new ConcurrentHashMap<>();
	private static final Map<Integer, float[]> LAST_COLOR = new ConcurrentHashMap<>();

	private Ssj4FadeTracker() {}

	public static float update(int entityId, long gameTime, float target, String key, float[] color) {
		float progress = PROGRESS.getOrDefault(entityId, 0.0f);

		Long lastSeen = LAST_SEEN.get(entityId);
		if (lastSeen == null || gameTime - lastSeen > 2L) progress = target;
		LAST_SEEN.put(entityId, gameTime);

		Long lastUpdate = LAST_UPDATE.get(entityId);
		if (lastUpdate == null || lastUpdate != gameTime) {
			if (target >= progress) progress = target;
			else progress = Math.max(target, progress - FADE_OUT_SPEED);

			PROGRESS.put(entityId, progress);
			LAST_UPDATE.put(entityId, gameTime);
		}

		if (key != null && color != null) {
			LAST_KEY.put(entityId, key);
			LAST_COLOR.put(entityId, color);
		}

		return progress;
	}

	public static String lastKey(int entityId) {
		return LAST_KEY.get(entityId);
	}

	public static float[] lastColor(int entityId) {
		return LAST_COLOR.get(entityId);
	}
}
