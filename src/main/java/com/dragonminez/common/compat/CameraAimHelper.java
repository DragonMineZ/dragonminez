package com.dragonminez.common.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

/** Server-side storage for the client's actual render-camera direction (Aero Cam Sync compatible). */
public final class CameraAimHelper {
	private static final String AIM_X = "dmz_camera_aim_x";
	private static final String AIM_Y = "dmz_camera_aim_y";
	private static final String AIM_Z = "dmz_camera_aim_z";
	private static final String AIM_TIME = "dmz_camera_aim_time";

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

	public static Vec3 resolve(LivingEntity entity) {
		var tag = entity.getPersistentData();
		if (entity.level().getGameTime() - tag.getLong(AIM_TIME) <= 40L) {
			Vec3 aim = new Vec3(tag.getDouble(AIM_X), tag.getDouble(AIM_Y), tag.getDouble(AIM_Z));
			if (isValid(aim)) return aim.normalize();
		}
		return entity.getLookAngle().normalize();
	}

	public static float yaw(Vec3 direction) {
		return (float) (Mth.atan2(direction.z, direction.x) * (180.0D / Math.PI) - 90.0D);
	}

	public static float pitch(Vec3 direction) {
		double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
		return (float) -(Mth.atan2(direction.y, horizontal) * (180.0D / Math.PI));
	}

	private static boolean isValid(Vec3 aim) {
		return aim != null && Double.isFinite(aim.x) && Double.isFinite(aim.y)
				&& Double.isFinite(aim.z) && aim.lengthSqr() > 1.0E-6D;
	}
}
