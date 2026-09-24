package com.dragonminez.client.clash;

import com.dragonminez.client.render.firstperson.dto.DMZCameraBuffer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BeamClashCinematicCamera {
	private static final double ORBIT_DISTANCE_BASE = 7.5;
	private static final double ORBIT_DISTANCE_SPAN = 0.825;
	private static final double ORBIT_DISTANCE_MAX = 24.0;
	private static final double CAMERA_HEIGHT_BASE = 1.2;
	private static final double CAMERA_HEIGHT_PER_DIST = 0.12;
	private static final double ORBIT_DEGREES_PER_SECOND = 14.0;
	private static final double FOCUS_SMOOTH_TAU = 0.15;
	private static final double WALL_MARGIN = 0.5;

	private static final float FOV_TARGET = 0.84f;
	private static final float FOV_EASE = 0.18f;
	private static final double SHAKE_POS = 0.16;
	private static final float SHAKE_ANGLE = 0.9f;

	private static volatile boolean active = false;
	private static CameraType previousType = null;
	private static float fovFactor = 1.0f;

	private static double orbitStartAngle = Double.NaN;
	private static float orbitStartTime = 0.0f;
	private static Vec3 focus = null;
	private static long focusNanos = 0L;

	private BeamClashCinematicCamera() {
	}

	public static boolean isActive() {
		return active;
	}

	public static void activate() {
		if (active) return;
		Minecraft mc = Minecraft.getInstance();
		previousType = mc.options.getCameraType();
		mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		orbitStartAngle = Double.NaN;
		focus = null;
		active = true;
	}

	public static void deactivate() {
		if (!active) return;
		Minecraft mc = Minecraft.getInstance();
		if (previousType != null) mc.options.setCameraType(previousType);
		previousType = null;
		active = false;
		orbitStartAngle = Double.NaN;
		focus = null;
		DMZCameraBuffer.reset();
	}

	public static void tickFov() {
		float target = active ? FOV_TARGET : 1.0f;
		fovFactor += (target - fovFactor) * FOV_EASE;
		if (Math.abs(fovFactor - target) < 0.001f) fovFactor = target;
	}

	public static double applyFov(double baseFov) {
		return baseFov * fovFactor;
	}

	public record Shot(Vec3 pos, float yaw, float pitch) {
	}

	public static Shot computeShot(BlockGetter level, LocalPlayer player, float partialTick) {
		if (!active) return null;

		float t = player.tickCount + partialTick;
		Vec3 selfPos = player.getEyePosition(partialTick);

		Vec3 oppPos = null;
		int oppId = ClientBeamClashState.opponentId();
		if (oppId >= 0 && player.level() != null) {
			Entity opp = player.level().getEntity(oppId);
			if (opp != null) oppPos = opp.getEyePosition(partialTick);
		}

		Vec3 target = ClientBeamClashState.clashPoint();
		if (target == null) {
			Vec3 far = oppPos != null ? oppPos : selfPos.add(player.getViewVector(partialTick).scale(8.0));
			target = selfPos.add(far).scale(0.5);
		}
		Vec3 centre = smoothFocus(target);

		Vec3 axis = (oppPos != null ? oppPos : target).subtract(selfPos);
		Vec3 axisHoriz = new Vec3(axis.x, 0, axis.z);
		double separation = axisHoriz.length();
		if (separation < 0.1) {
			Vec3 look = player.getViewVector(partialTick);
			axisHoriz = new Vec3(look.x, 0, look.z);
			separation = Math.max(axisHoriz.length(), 0.1);
		}
		axisHoriz = axisHoriz.normalize();

		if (Double.isNaN(orbitStartAngle)) {
			Vec3 side = new Vec3(0, 1, 0).cross(axisHoriz).normalize();
			orbitStartAngle = Math.atan2(side.z, side.x);
			orbitStartTime = t;
		}
		double angle = orbitStartAngle + Math.toRadians(ORBIT_DEGREES_PER_SECOND) * (t - orbitStartTime) / 20.0;
		Vec3 radial = new Vec3(Math.cos(angle), 0, Math.sin(angle));
		Vec3 tangent = new Vec3(-radial.z, 0, radial.x);

		double dist = Math.min(ORBIT_DISTANCE_MAX, ORBIT_DISTANCE_BASE + separation * ORBIT_DISTANCE_SPAN);
		double height = CAMERA_HEIGHT_BASE + CAMERA_HEIGHT_PER_DIST * dist;

		float decisiveness = Math.abs(ClientBeamClashState.advantage() - 0.5f) * 2.0f;
		double shake = SHAKE_POS * (0.5 + decisiveness);
		float angleShake = SHAKE_ANGLE * (0.5f + decisiveness);

		Vec3 camPos = centre
				.add(radial.scale(dist))
				.add(0, height, 0)
				.add(tangent.scale(Math.sin(t * 1.7) * shake))
				.add(0, Math.cos(t * 2.3) * shake * 0.6, 0);

		camPos = clampToLineOfSight(level, player, centre, camPos);

		Vec3 dir = centre.subtract(camPos);
		double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
		float pitch = (float) (-(Mth.atan2(dir.y, horiz) * (180.0 / Math.PI)));
		float yaw = (float) (Mth.atan2(dir.z, dir.x) * (180.0 / Math.PI) - 90.0);

		yaw += (float) (Math.sin(t * 3.1) * angleShake);
		pitch += (float) (Math.cos(t * 2.7) * angleShake * 0.7);

		return new Shot(camPos, yaw, pitch);
	}

	private static Vec3 smoothFocus(Vec3 target) {
		long now = System.nanoTime();
		if (focus == null) {
			focus = target;
			focusNanos = now;
			return focus;
		}
		double dt = Math.min(0.25, Math.max(0.0, (now - focusNanos) / 1.0e9));
		focusNanos = now;
		double k = 1.0 - Math.exp(-dt / FOCUS_SMOOTH_TAU);
		focus = focus.add(target.subtract(focus).scale(k));
		return focus;
	}

	private static Vec3 clampToLineOfSight(BlockGetter level, Entity viewer, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		double len = delta.length();
		if (len < 1.0e-4) return to;
		Vec3 dir = delta.scale(1.0 / len);
		Vec3 rayEnd = from.add(dir.scale(len + WALL_MARGIN));
		BlockHitResult hit = level.clip(new ClipContext(
				from, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
		if (hit.getType() != HitResult.Type.MISS) {
			double allowed = Math.max(0.0, hit.getLocation().distanceTo(from) - WALL_MARGIN);
			if (allowed < len) return from.add(dir.scale(allowed));
		}
		return to;
	}
}
