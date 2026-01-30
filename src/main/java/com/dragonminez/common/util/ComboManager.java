package com.dragonminez.common.util;

import net.minecraft.world.entity.Entity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ComboManager {
	private static final Map<UUID, Integer> comboCount = new HashMap<>();
	private static final Map<UUID, Integer> lastTargetId = new HashMap<>();
	private static final Map<UUID, Boolean> isNextHitCombo = new HashMap<>();
	private static final Map<UUID, Long> teleportWindow = new HashMap<>();
	private static final Map<UUID, Integer> teleportTargetId = new HashMap<>();

	public static int getCombo(UUID player) {
		return comboCount.getOrDefault(player, 0);
	}

	public static void setCombo(UUID player, int combo) {
		comboCount.put(player, combo);
	}

	public static void resetCombo(UUID player) {
		comboCount.put(player, 0);
		lastTargetId.remove(player);
	}

	public static boolean shouldContinueCombo(UUID player, Entity target) {
		int lastId = lastTargetId.getOrDefault(player, -1);
		return lastId == target.getId();
	}

	public static void registerHit(UUID player, Entity target) {
		lastTargetId.put(player, target.getId());
	}

	public static void setNextHitAsCombo(UUID player, boolean isCombo) {
		isNextHitCombo.put(player, isCombo);
	}

	public static boolean isNextHitCombo(UUID player) {
		return isNextHitCombo.getOrDefault(player, false);
	}

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