package com.dragonminez.client.gui.dialogue;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class CooldownClock {
	private static final long TICK_MS = 50L;
	private static final long DRIFT_MS = 1500L;

	private long endMs;
	private int lastSynced = -1;

	long remainingMs(int syncedTicks, long now) {
		if (syncedTicks <= 0) {
			endMs = 0L;
			lastSynced = -1;
			return 0L;
		}
		if (syncedTicks != lastSynced) {
			long estimate = now + syncedTicks * TICK_MS;
			if (endMs == 0L || Math.abs(estimate - endMs) > DRIFT_MS) endMs = estimate;
			lastSynced = syncedTicks;
		}
		return Math.max(0L, endMs - now);
	}

	static long remainingMs(long endMs, long now) {
		return Math.max(0L, endMs - now);
	}

	static String format(long millis) {
		long seconds = (millis + 999L) / 1000L;
		long hours = seconds / 3600L;
		long minutes = (seconds % 3600L) / 60L;
		long secs = seconds % 60L;
		if (hours > 0L) return String.format("%d:%02d:%02d", hours, minutes, secs);
		return String.format("%d:%02d", minutes, secs);
	}
}
