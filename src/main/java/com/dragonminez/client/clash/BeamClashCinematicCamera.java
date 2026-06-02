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

/**
 * Client-side cinematic camera for beam clashes. On clash start it forces a third-person
 * view (so both fighters' models render) and frames a side-profile shot of the two beam
 * origins, layered with a struggle shake and a FOV push-in. Everything is restored when
 * the clash ends.
 */
@OnlyIn(Dist.CLIENT)
public final class BeamClashCinematicCamera {

	// --- framing ---
	private static final double SIDE_DISTANCE_BASE = 5.0;   // base side offset (blocks)
	private static final double SIDE_DISTANCE_SPAN = 0.55;  // extra per block of fighter separation
	private static final double SIDE_DISTANCE_MAX = 16.0;
	private static final double CAMERA_HEIGHT = 1.6;        // raise above the beam line
	private static final double WALL_MARGIN = 0.5;

	// --- effects ---
	private static final float FOV_TARGET = 0.84f;          // multiplier when fully zoomed in
	private static final float FOV_EASE = 0.18f;
	private static final double SHAKE_POS = 0.16;           // blocks
	private static final float SHAKE_ANGLE = 0.9f;          // degrees

	private static volatile boolean active = false;
	private static CameraType previousType = null;
	private static float fovFactor = 1.0f;

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
		active = true;
	}

	public static void deactivate() {
		if (!active) return;
		Minecraft mc = Minecraft.getInstance();
		if (previousType != null) mc.options.setCameraType(previousType);
		previousType = null;
		active = false;
		DMZCameraBuffer.reset();
	}

	/** Eases the FOV factor toward its target; call once per client tick. */
	public static void tickFov() {
		float target = active ? FOV_TARGET : 1.0f;
		fovFactor += (target - fovFactor) * FOV_EASE;
		if (Math.abs(fovFactor - target) < 0.001f) fovFactor = target;
	}

	public static double applyFov(double baseFov) {
		return baseFov * fovFactor;
	}

	/** A computed cinematic camera placement. */
	public record Shot(Vec3 pos, float yaw, float pitch) {
	}

	/**
	 * Builds the side-profile shot framing both fighters, or null if it can't be framed
	 * (no opponent yet) — in which case the vanilla camera is left untouched.
	 */
	public static Shot computeShot(BlockGetter level, LocalPlayer player, float partialTick) {
		if (!active) return null;

		Vec3 selfPos = player.getEyePosition(partialTick);

		Vec3 oppPos = null;
		int oppId = ClientBeamClashState.opponentId();
		if (oppId >= 0 && player.level() != null) {
			Entity opp = player.level().getEntity(oppId);
			if (opp != null) oppPos = opp.getEyePosition(partialTick);
		}
		// Fallback: aim down the player's beam if the opponent isn't available.
		if (oppPos == null) {
			oppPos = selfPos.add(player.getViewVector(partialTick).scale(8.0));
		}

		Vec3 mid = selfPos.add(oppPos).scale(0.5);

		// Horizontal axis between fighters (fall back to look direction if degenerate).
		Vec3 axis = oppPos.subtract(selfPos);
		Vec3 axisHoriz = new Vec3(axis.x, 0, axis.z);
		double separation = axisHoriz.length();
		if (separation < 0.1) {
			Vec3 look = player.getViewVector(partialTick);
			axisHoriz = new Vec3(look.x, 0, look.z);
			separation = Math.max(axisHoriz.length(), 0.1);
		}
		axisHoriz = axisHoriz.normalize();

		Vec3 side = new Vec3(0, 1, 0).cross(axisHoriz).normalize();

		double dist = Math.min(SIDE_DISTANCE_MAX, SIDE_DISTANCE_BASE + separation * SIDE_DISTANCE_SPAN);

		// Shake intensity ramps up as the struggle becomes decisive.
		float decisiveness = Math.abs(ClientBeamClashState.advantage() - 0.5f) * 2.0f;
		double shake = SHAKE_POS * (0.5 + decisiveness);
		float angleShake = SHAKE_ANGLE * (0.5f + decisiveness);
		float t = player.tickCount + partialTick;

		Vec3 camPos = mid
				.add(side.scale(dist))
				.add(0, CAMERA_HEIGHT, 0)
				.add(side.scale(Math.sin(t * 1.7) * shake))
				.add(0, Math.cos(t * 2.3) * shake * 0.6, 0);

		camPos = clampToLineOfSight(level, player, mid, camPos);

		Vec3 dir = mid.subtract(camPos);
		double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
		float pitch = (float) (-(Mth.atan2(dir.y, horiz) * (180.0 / Math.PI)));
		float yaw = (float) (Mth.atan2(dir.z, dir.x) * (180.0 / Math.PI) - 90.0);

		yaw += (float) (Math.sin(t * 3.1) * angleShake);
		pitch += (float) (Math.cos(t * 2.7) * angleShake * 0.7);

		return new Shot(camPos, yaw, pitch);
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
