package com.dragonminez.server.world.worldboss;

import com.dragonminez.common.worldboss.WorldBossResults;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WorldBossResultsCache {
	private static final long RETENTION_MILLIS = 30L * 60L * 1000L;

	private record Stored(WorldBossResults results, long storedAt) {}

	private static final Map<UUID, Stored> BY_PLAYER = new HashMap<>();
	private static Stored latest;

	private WorldBossResultsCache() {}

	public static void store(WorldBossResults results) {
		Stored stored = new Stored(results, System.currentTimeMillis());
		latest = stored;
		for (WorldBossResults.PlayerEntry entry : results.players()) BY_PLAYER.put(entry.id(), stored);
	}

	public static WorldBossResults get(UUID playerId) {
		long now = System.currentTimeMillis();
		BY_PLAYER.entrySet().removeIf(entry -> now - entry.getValue().storedAt() > RETENTION_MILLIS);
		Stored stored = BY_PLAYER.get(playerId);
		if (stored == null) stored = latest;
		if (stored == null || now - stored.storedAt() > RETENTION_MILLIS) return null;
		return stored.results();
	}

	public static void clear() {
		BY_PLAYER.clear();
		latest = null;
	}
}
