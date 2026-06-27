package com.dragonminez.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Shared safe-landing logic for Instant Transmission teleports (masters and players).
 */
public final class ITTeleportHelper {

	private ITTeleportHelper() {}

	/** Finds a safe block to land on in a ring around {@code center}, falling back to {@code center.above()}. */
	public static BlockPos findSafeTeleportPos(ServerLevel level, BlockPos center) {
		for (int r = 3; r <= 4; r++) {
			for (int x = -r; x <= r; x++) {
				for (int z = -r; z <= r; z++) {
					double dist = Math.sqrt(x * x + z * z);
					if (dist >= 3.0 && dist <= 4.5) {
						for (int y = 3; y >= -3; y--) {
							BlockPos testPos = center.offset(x, y, z);
							if (isSafe(level, testPos)) {
								return testPos;
							}
						}
					}
				}
			}
		}

		return center.above(1);
	}

	public static boolean isSafe(ServerLevel level, BlockPos pos) {
		boolean hasFloor = !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty();

		boolean bodyClear = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
		boolean headClear = level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty() && level.getFluidState(pos.above()).isEmpty();

		return hasFloor && bodyClear && headClear;
	}
}
