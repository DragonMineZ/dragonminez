package com.dragonminez.common.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.HashSet;
import java.util.Set;

/** Side-local storage for the actual render-camera direction (Aero Cam Sync compatible). */
public final class CameraAimHelper {
	private static final String AIM_X = "dmz_camera_aim_x";
	private static final String AIM_Y = "dmz_camera_aim_y";
	private static final String AIM_Z = "dmz_camera_aim_z";
	private static final String AIM_TIME = "dmz_camera_aim_time";
	private static final Set<Integer> LOCAL_SOKIDANS = new HashSet<>();

	private CameraAimHelper() {}

	public static void store(LivingEntity entity, Vec3 aim) {
		if (!isValid(aim)) return;
		Vec3 normalized = aim.normalize();
		var tag = entity.getPersistentData();
		tag.putDouble(AIM_X, normalized.x);
		tag.putDouble(AIM_Y, normalized.y);
		tag.putDouble(AIM_Z, normalized.z);
		tag.putLong(AIM_TIME, entity.level().getGameTime());
	}

	/**
	 * How long a stored camera aim stays valid (ticks). Refreshed while charging or steering.
	 *
	 * <p>This used to be 80 — four seconds — so an aim recorded nearly four seconds ago still beat
	 * the entity's live rotation. Look somewhere, hold still, snap the camera to a new target and
	 * fire, and the blast followed where you <em>were</em> pointing. It only misfired when a stale
	 * entry happened to still sit inside that window, which is what made it intermittent rather
	 * than constant.
	 *
	 * <p>A camera aim is only meaningful for roughly the tick it was sampled on. Three covers the
	 * every-other-tick refresh cadence in {@code ClientStatsEvents} plus a little jitter; past that
	 * the live look angle is the better answer, being at worst slightly behind rather than wrong.
	 */
	private static final long AIM_FRESH_TICKS = 3L;

	/**
	 * The entity's stored camera aim, falling back to its look angle.
	 *
	 * <p>The tag itself is not synchronized. Shared prediction may use this only when its control
	 * path explicitly refreshes both copies, as Sokidan steering does. Other shared tick paths must
	 * use entity rotation; one-shot server launch decisions may always use this helper.
	 */
	public static Vec3 resolve(LivingEntity entity) {
		var tag = entity.getPersistentData();
		if (entity.level().getGameTime() - tag.getLong(AIM_TIME) <= AIM_FRESH_TICKS) {
			Vec3 aim = new Vec3(tag.getDouble(AIM_X), tag.getDouble(AIM_Y), tag.getDouble(AIM_Z));
			if (isValid(aim)) return aim.normalize();
		}
		return entity.getLookAngle().normalize();
	}

	public static void trackLocalSokidan(int entityId, boolean controlled) {
		if (controlled) LOCAL_SOKIDANS.add(entityId); else LOCAL_SOKIDANS.remove(entityId);
	}

	public static boolean hasLocalSokidan(Level level) {
		LOCAL_SOKIDANS.removeIf(id -> {
			var entity = level.getEntity(id);
			return entity == null || entity.isRemoved();
		});
		return !LOCAL_SOKIDANS.isEmpty();
	}

	public static void clearLocalSokidans() { LOCAL_SOKIDANS.clear(); }

	public static float yaw(Vec3 direction) {
		return (float) (Mth.atan2(direction.z, direction.x) * (180.0D / Math.PI) - 90.0D);
	}

	/**
	 * Yaw of an aim vector, falling back to the entity's own when the vector is vertical.
	 *
	 * <p>{@link #yaw(Vec3)} is {@code atan2(z, x)}, which is undefined looking straight up or down:
	 * both components go to zero and the result is whatever the rounding error happened to be —
	 * {@code (0,1,0)} and {@code (0,1,-1e-9)} give -90 and -180 for the same direction. It barely
	 * moves a near-vertical shot, but there is no reason to feed noise into a launch angle when the
	 * entity already knows which way it faces.
	 */
	public static float yaw(LivingEntity entity, Vec3 direction) {
		double horizontalSqr = direction.x * direction.x + direction.z * direction.z;
		if (horizontalSqr < VERTICAL_EPSILON) return entity.getYRot();
		return yaw(direction);
	}

	/** Below this squared horizontal length an aim is vertical and its yaw is meaningless. */
	private static final double VERTICAL_EPSILON = 1.0E-6D;

	public static float pitch(Vec3 direction) {
		double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
		return (float) -(Mth.atan2(direction.y, horizontal) * (180.0D / Math.PI));
	}

	private static boolean isValid(Vec3 aim) {
		return aim != null && Double.isFinite(aim.x) && Double.isFinite(aim.y)
				&& Double.isFinite(aim.z) && aim.lengthSqr() > 1.0E-6D;
	}
}
