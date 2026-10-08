package com.dragonminez.common.combat.logic.player;

import com.dragonminez.common.combat.collision.CollisionHelper;
import com.dragonminez.common.combat.collision.MeleeHitbox;
import com.dragonminez.common.combat.collision.OrientedBoundingBox;
import com.dragonminez.common.combat.weapon.WeaponAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class MeleeTargetValidator {
	private static final double BOX_SLACK = 0.5;
	private static final double TARGET_SLACK = 0.35;
	private static final float AIM_TOLERANCE_DEGREES = 25.0F;
	private static final double BASE_LAG_TICKS = 4.0;
	private static final double MAX_LAG_TICKS = 24.0;
	private static final double MAX_SWEEP = 12.0;
	private static final float SAME_ROTATION_EPSILON = 0.5F;

	private MeleeTargetValidator() {}

	public record Aim(float pitch, float yaw) {}

	public static List<Aim> candidateAims(ServerPlayer player, float arrivalPitch, float arrivalYaw) {
		List<Aim> aims = new ArrayList<>(4);
		float pitch = player.getXRot();
		float yaw = player.getYRot();
		addAim(aims, pitch, yaw);
		addAim(aims, arrivalPitch, arrivalYaw);
		addAim(aims, player.xRotO, player.yRotO);
		addAim(aims, Mth.clamp(pitch + (pitch - player.xRotO), -90.0F, 90.0F), yaw + Mth.wrapDegrees(yaw - player.yRotO));
		return aims;
	}

	public static boolean canHit(ServerPlayer player, Entity target, List<WeaponAttributes.Attack> attacks, double attackRange, List<Aim> aims) {
		for (WeaponAttributes.Attack attack : attacks) {
			if (attack == null || attack.hitbox() == null) return true;
		}

		Vec3 sweep = lagSweep(player, target);
		AABB current = target.getBoundingBox();
		AABB reachable = current.expandTowards(sweep.x, sweep.y, sweep.z).inflate(TARGET_SLACK);

		for (Aim aim : aims) {
			Vec3 origin = MeleeHitbox.origin(player, aim.pitch(), aim.yaw());
			Aim adjusted = steerToward(aim, origin, reachable.getCenter());
			Vec3 adjustedOrigin = MeleeHitbox.origin(player, adjusted.pitch(), adjusted.yaw());
			for (WeaponAttributes.Attack attack : attacks) {
				OrientedBoundingBox obb = MeleeHitbox.build(player, adjustedOrigin, attack, attackRange, adjusted.pitch(), adjusted.yaw())
						.inflate(BOX_SLACK)
						.updateVertex();
				if (obb.intersects(reachable)) return hasClearPath(player, adjustedOrigin, current, current.move(sweep));
			}
		}
		return false;
	}

	private static void addAim(List<Aim> aims, float pitch, float yaw) {
		if (!Float.isFinite(pitch) || !Float.isFinite(yaw)) return;
		for (Aim aim : aims) {
			if (Math.abs(aim.pitch() - pitch) < SAME_ROTATION_EPSILON && Math.abs(Mth.wrapDegrees(aim.yaw() - yaw)) < SAME_ROTATION_EPSILON) return;
		}
		aims.add(new Aim(pitch, yaw));
	}

	private static Aim steerToward(Aim aim, Vec3 origin, Vec3 point) {
		Vec3 dir = point.subtract(origin);
		double horizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
		if (horizontal < 1.0E-4 && Math.abs(dir.y) < 1.0E-4) return aim;
		float targetYaw = (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
		float targetPitch = (float) (Mth.atan2(-dir.y, horizontal) * Mth.RAD_TO_DEG);
		float yaw = aim.yaw() + Mth.clamp(Mth.wrapDegrees(targetYaw - aim.yaw()), -AIM_TOLERANCE_DEGREES, AIM_TOLERANCE_DEGREES);
		float pitch = Mth.clamp(aim.pitch() + Mth.clamp(targetPitch - aim.pitch(), -AIM_TOLERANCE_DEGREES, AIM_TOLERANCE_DEGREES), -90.0F, 90.0F);
		return new Aim(pitch, yaw);
	}

	private static Vec3 lagSweep(ServerPlayer player, Entity target) {
		Entity mover = TargetHelper.resolveHittable(target);
		Vec3 step = mover.position().subtract(mover.xo, mover.yo, mover.zo);
		double lagTicks = Math.min(MAX_LAG_TICKS, BASE_LAG_TICKS + Math.max(0, player.latency) / 50.0);
		Vec3 back = step.scale(-lagTicks);
		double length = back.length();
		if (!Double.isFinite(length)) return Vec3.ZERO;
		return length > MAX_SWEEP ? back.scale(MAX_SWEEP / length) : back;
	}

	private static boolean hasClearPath(ServerPlayer player, Vec3 origin, AABB current, AABB previous) {
		Vec3 eye = player.getEyePosition();
		return isClear(player, origin, closestPoint(origin, current))
				|| isClear(player, eye, closestPoint(eye, current))
				|| isClear(player, eye, current.getCenter())
				|| isClear(player, origin, closestPoint(origin, previous))
				|| isClear(player, eye, previous.getCenter());
	}

	private static Vec3 closestPoint(Vec3 from, AABB box) {
		return from.add(CollisionHelper.distanceVector(from, box));
	}

	private static boolean isClear(ServerPlayer player, Vec3 from, Vec3 to) {
		if (from.distanceToSqr(to) < 1.0E-6) return true;
		HitResult hit = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		return hit.getType() != HitResult.Type.BLOCK;
	}
}
