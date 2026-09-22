package com.dragonminez.client.systems.worldboss;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.S2C.WorldBossContributionS2C;

import java.util.List;
import java.util.UUID;

public final class ClientWorldBossContribution {
	private static final long STALE_MILLIS = 6000L;

	private static String bossKey = "";
	private static String bossNameKey = "";
	private static long elapsedTicks;
	private static long syncedAtMillis;
	private static boolean finished;
	private static long finishedAtMillis;
	private static volatile List<WorldBossContributionS2C.Entry> entries = List.of();

	private ClientWorldBossContribution() {}

	public static void accept(String key, String nameKey, long elapsed, boolean fin, boolean cleared, List<WorldBossContributionS2C.Entry> incoming) {
		if (cleared) {
			clear();
			return;
		}
		long now = System.currentTimeMillis();
		if (finished && !fin) finished = false;
		bossKey = key == null ? "" : key;
		bossNameKey = nameKey == null ? "" : nameKey;
		elapsedTicks = elapsed;
		syncedAtMillis = now;
		if (fin && !finished) finishedAtMillis = now;
		finished = fin;
		entries = incoming == null ? List.of() : List.copyOf(incoming);
	}

	public static void clear() {
		bossKey = "";
		bossNameKey = "";
		elapsedTicks = 0L;
		syncedAtMillis = 0L;
		finished = false;
		finishedAtMillis = 0L;
		entries = List.of();
	}

	public static boolean isActive() {
		if (entries.isEmpty()) return false;
		long now = System.currentTimeMillis();
		if (finished) return now - finishedAtMillis < ConfigManager.getServerConfig().getWorldBoss().getMeterLingerSeconds() * 1000L;
		return now - syncedAtMillis < STALE_MILLIS;
	}

	public static boolean isFinished() {
		return finished;
	}

	public static float lingerFraction() {
		if (!finished) return 0.0f;
		long linger = ConfigManager.getServerConfig().getWorldBoss().getMeterLingerSeconds() * 1000L;
		if (linger <= 0L) return 1.0f;
		return Math.min(1.0f, (System.currentTimeMillis() - finishedAtMillis) / (float) linger);
	}

	public static List<WorldBossContributionS2C.Entry> entries() {
		return entries;
	}

	public static String bossKey() {
		return bossKey;
	}

	public static String bossNameKey() {
		return bossNameKey;
	}

	public static long elapsedTicks() {
		if (finished || syncedAtMillis == 0L) return elapsedTicks;
		return elapsedTicks + (System.currentTimeMillis() - syncedAtMillis) / 50L;
	}

	public static WorldBossContributionS2C.Entry find(UUID id) {
		for (WorldBossContributionS2C.Entry entry : entries) {
			if (entry.id().equals(id)) return entry;
		}
		return null;
	}
}
