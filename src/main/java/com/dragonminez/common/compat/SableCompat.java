package com.dragonminez.common.compat;

import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional bridge to Sable's sublevel coordinate projection API. */
public final class SableCompat {
	private static final Object HELPER;
	private static final Method PROJECT_OUT_OF_SUBLEVEL;
	private static final Method GET_TRACKING_OR_VEHICLE_SUBLEVEL;
	private static final Method GET_CONTAINING;
	private static final Method LOGICAL_POSE;
	private static final Method TRANSFORM_NORMAL_INVERSE;

	static {
		Object helper = null;
		Method project = null;
		Method getTracking = null;
		Method getContaining = null;
		Method logicalPose = null;
		Method transformNormalInverse = null;
		try {
			Class<?> sable = Class.forName("dev.ryanhcode.sable.Sable");
			Field helperField = sable.getField("HELPER");
			helper = helperField.get(null);
			project = helper.getClass().getMethod("projectOutOfSubLevel", Level.class, Position.class);
			getTracking = helper.getClass().getMethod("getTrackingOrVehicleSubLevel", Entity.class);
			getContaining = helper.getClass().getMethod("getContaining", Level.class, Position.class);
			Class<?> subLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
			logicalPose = subLevelClass.getMethod("logicalPose");
			Class<?> poseClass = Class.forName("dev.ryanhcode.sable.companion.math.Pose3d");
			transformNormalInverse = poseClass.getMethod("transformNormalInverse", Vec3.class);
		} catch (ReflectiveOperationException ignored) {
			helper = null;
			project = null;
			getTracking = null;
			getContaining = null;
			logicalPose = null;
			transformNormalInverse = null;
		}
		HELPER = helper;
		PROJECT_OUT_OF_SUBLEVEL = project;
		GET_TRACKING_OR_VEHICLE_SUBLEVEL = getTracking;
		GET_CONTAINING = getContaining;
		LOGICAL_POSE = logicalPose;
		TRANSFORM_NORMAL_INVERSE = transformNormalInverse;
	}

	private SableCompat() {}

	/** Converts a Sable clip result from ship-local plot coordinates back to world coordinates. */
	public static Vec3 projectToWorld(Level level, Vec3 position) {
		if (HELPER == null || PROJECT_OUT_OF_SUBLEVEL == null) return position;
		try {
			Object projected = PROJECT_OUT_OF_SUBLEVEL.invoke(HELPER, level, position);
			return projected instanceof Vec3 vec ? vec : position;
		} catch (ReflectiveOperationException ignored) {
			return position;
		}
	}

	/** Converts a world-space camera/aim vector into the local axes of the ship carrying an entity. */
	public static Vec3 worldDirectionToEntitySpace(Entity entity, Vec3 direction) {
		if (HELPER == null || GET_TRACKING_OR_VEHICLE_SUBLEVEL == null
				|| LOGICAL_POSE == null || TRANSFORM_NORMAL_INVERSE == null) return direction;
		try {
			Object subLevel = resolveEntitySubLevel(entity);
			if (subLevel == null) return direction;
			Object pose = LOGICAL_POSE.invoke(subLevel);
			Object transformed = TRANSFORM_NORMAL_INVERSE.invoke(pose, direction);
			return transformed instanceof Vec3 vec ? vec : direction;
		} catch (ReflectiveOperationException ignored) {
			return direction;
		}
	}

	public static boolean isEntityInSubLevel(Entity entity) {
		if (HELPER == null) return false;
		try {
			return resolveEntitySubLevel(entity) != null;
		} catch (ReflectiveOperationException ignored) {
			return false;
		}
	}

	private static Object resolveEntitySubLevel(Entity entity) throws ReflectiveOperationException {
		Object subLevel = null;
		if (GET_TRACKING_OR_VEHICLE_SUBLEVEL != null) {
			subLevel = GET_TRACKING_OR_VEHICLE_SUBLEVEL.invoke(HELPER, entity);
		}
		if (subLevel == null && GET_CONTAINING != null) {
			subLevel = GET_CONTAINING.invoke(HELPER, entity.level(), entity.position());
		}
		return subLevel;
	}
}
