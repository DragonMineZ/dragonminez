package com.dragonminez.client.render.camera;

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
public final class TechniquePreviewCamera {
	private static final float DEFAULT_YAW_OFFSET = -100.0F;
	private static final float DEFAULT_PITCH = 10.0F;
	private static final double PREVIEW_FOV = 70.0;
	private static final float MIN_PITCH = -55.0F;
	private static final float MAX_PITCH = 80.0F;
	private static final float MIN_ZOOM = 0.45F;
	private static final float MAX_ZOOM = 3.0F;
	private static final float ZOOM_STEP = 0.12F;
	private static final double ORBIT_TAU = 0.08;
	private static final double FRAME_TAU = 0.35;
	private static final double MAX_FRAME_SECONDS = 0.25;
	private static final double MIN_DISTANCE = 1.2;
	private static final double WALL_MARGIN = 0.3;

	private static boolean active = false;
	private static CameraType previousType = null;
	private static boolean previousHideGui = false;

	private static float yaw;
	private static float pitch;
	private static float targetYaw;
	private static float targetPitch;
	private static float zoom = 1.0F;
	private static float targetZoom = 1.0F;
	private static double distance;
	private static double targetDistance;
	private static double height;
	private static double targetHeight;
	private static double lead;
	private static double targetLead;
	private static long lastFrameNanos = 0L;

	private TechniquePreviewCamera() {
	}

	public static boolean isActive() {
		return active;
	}

	public static void activate(float playerYaw, double frameDistance, double frameHeight, double frameLead) {
		if (active) return;
		Minecraft mc = Minecraft.getInstance();
		previousType = mc.options.getCameraType();
		previousHideGui = mc.options.hideGui;
		mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		mc.options.hideGui = true;
		yaw = targetYaw = playerYaw + DEFAULT_YAW_OFFSET;
		pitch = targetPitch = DEFAULT_PITCH;
		zoom = targetZoom = 1.0F;
		distance = targetDistance = frameDistance;
		height = targetHeight = frameHeight;
		lead = targetLead = frameLead;
		lastFrameNanos = 0L;
		active = true;
	}

	public static void deactivate() {
		if (!active) return;
		Minecraft mc = Minecraft.getInstance();
		if (previousType != null) mc.options.setCameraType(previousType);
		mc.options.hideGui = previousHideGui;
		previousType = null;
		active = false;
		DMZCameraBuffer.reset();
	}

	public static double fov() {
		return PREVIEW_FOV;
	}

	public static void orbit(double yawDegrees, double pitchDegrees) {
		targetYaw += (float) yawDegrees;
		targetPitch = Mth.clamp(targetPitch + (float) pitchDegrees, MIN_PITCH, MAX_PITCH);
	}

	public static void zoom(double steps) {
		float factor = (float) Math.pow(1.0 - ZOOM_STEP, steps);
		targetZoom = Mth.clamp(targetZoom * factor, MIN_ZOOM, MAX_ZOOM);
	}

	public static void frame(double frameDistance, double frameHeight, double frameLead) {
		targetDistance = frameDistance;
		targetHeight = frameHeight;
		targetLead = frameLead;
	}

	public record Shot(Vec3 pos, float yaw, float pitch) {
	}

	public static Shot computeShot(BlockGetter level, LocalPlayer player, float partialTick) {
		if (!active) return null;

		long now = System.nanoTime();
		double dt = lastFrameNanos == 0L ? 0.0 : Math.min(MAX_FRAME_SECONDS, (now - lastFrameNanos) / 1.0E9);
		lastFrameNanos = now;
		double orbitEase = 1.0 - Math.exp(-dt / ORBIT_TAU);
		double frameEase = 1.0 - Math.exp(-dt / FRAME_TAU);

		yaw += (float) ((targetYaw - yaw) * orbitEase);
		pitch += (float) ((targetPitch - pitch) * orbitEase);
		zoom += (float) ((targetZoom - zoom) * orbitEase);
		distance += (targetDistance - distance) * frameEase;
		height += (targetHeight - height) * frameEase;
		lead += (targetLead - lead) * frameEase;

		Vec3 ahead = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick)).scale(lead);
		Vec3 pivot = player.getPosition(partialTick).add(ahead.x, height, ahead.z);
		Vec3 forward = Vec3.directionFromRotation(pitch, yaw);
		double wanted = Math.max(MIN_DISTANCE, distance * zoom);
		Vec3 camPos = clampToLineOfSight(level, player, pivot, pivot.subtract(forward.scale(wanted)));
		return new Shot(camPos, yaw, pitch);
	}

	private static Vec3 clampToLineOfSight(BlockGetter level, Entity viewer, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		if (length < 1.0e-4) return to;
		Vec3 dir = delta.scale(1.0 / length);
		BlockHitResult hit = level.clip(new ClipContext(from, from.add(dir.scale(length + WALL_MARGIN)),
				ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, viewer));
		if (hit.getType() == HitResult.Type.MISS) return to;
		double allowed = Math.max(0.0, hit.getLocation().distanceTo(from) - WALL_MARGIN);
		return allowed < length ? from.add(dir.scale(allowed)) : to;
	}
}
