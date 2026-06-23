package com.dragonminez.server.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side registry of active Gravity Device zones. Each active device publishes the
 * axis-aligned bounds of its sealed room together with the gravity it enforces; players
 * standing inside a zone receive that gravity through {@link GravityLogic#getMachineGravity}.
 */
public final class GravityDeviceManager {

	private record Zone(String dimension, AABB bounds, double gravity) {}

	private static final Map<String, Zone> ZONES = new ConcurrentHashMap<>();

	private GravityDeviceManager() {}

	private static String key(Level level, BlockPos pos) {
		return level.dimension().location() + "@" + pos.asLong();
	}

	public static void register(Level level, BlockPos pos, AABB bounds, double gravity) {
		ZONES.put(key(level, pos), new Zone(level.dimension().location().toString(), bounds, gravity));
	}

	public static void unregister(Level level, BlockPos pos) {
		ZONES.remove(key(level, pos));
	}

	/**
	 * @return the strongest machine gravity currently affecting the given player, or 0 if none.
	 */
	public static double getGravityFor(Player player) {
		if (ZONES.isEmpty()) return 0.0;
		String dim = player.level().dimension().location().toString();
		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();
		double max = 0.0;
		for (Zone zone : ZONES.values()) {
			if (!zone.dimension.equals(dim)) continue;
			if (zone.bounds.contains(x, y, z) && zone.gravity > max) max = zone.gravity;
		}
		return max;
	}
}
