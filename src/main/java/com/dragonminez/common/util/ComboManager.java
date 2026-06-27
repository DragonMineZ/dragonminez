package com.dragonminez.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ComboManager {
	private static final Map<UUID, Long> teleportWindow = new HashMap<>();
	private static final Map<UUID, Integer> teleportTargetId = new HashMap<>();

	public static void enableTeleportWindow(UUID player, int targetId) {
		teleportWindow.put(player, System.currentTimeMillis());
		teleportTargetId.put(player, targetId);
	}

	public static boolean canTeleport(UUID player) {
		if (!teleportWindow.containsKey(player)) return false;
		return System.currentTimeMillis() - teleportWindow.get(player) <= 1500;
	}

	public static int getTeleportTarget(UUID player) {
		return teleportTargetId.getOrDefault(player, -1);
	}

	public static void consumeTeleport(UUID player) {
		teleportWindow.remove(player);
		teleportTargetId.remove(player);
	}
}