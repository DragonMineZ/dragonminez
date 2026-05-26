package com.dragonminez.client.render.layer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuraTintTracker {
	public static final float FADE_SPEED = 0.005f;

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
}