package com.dragonminez.client.systems.worldboss;

import net.minecraft.core.BlockPos;

public final class ClientWorldBossState {

	private static boolean lairKnown;
	private static BlockPos lair = BlockPos.ZERO;
	private static boolean bossAlive;
	private static long respawnTicks;
	private static long syncedAtMillis;

	private ClientWorldBossState() {}

	public static void update(boolean known, BlockPos pos, boolean alive, long remainingTicks) {
		lairKnown = known;
		lair = pos;
		bossAlive = alive;
		respawnTicks = remainingTicks;
		syncedAtMillis = System.currentTimeMillis();
	}

	public static void clear() {
		lairKnown = false;
		lair = BlockPos.ZERO;
		bossAlive = false;
		respawnTicks = 0L;
		syncedAtMillis = 0L;
	}

	public static boolean isLairKnown() {
		return lairKnown;
	}

	public static BlockPos getLair() {
		return lair;
	}

	public static boolean isBossAlive() {
		return bossAlive;
	}

	public static long getRespawnTicksRemaining() {
		if (respawnTicks <= 0L || syncedAtMillis == 0L) return 0L;
		long elapsed = (System.currentTimeMillis() - syncedAtMillis) / 50L;
		return Math.max(0L, respawnTicks - elapsed);
	}
}
