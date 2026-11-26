package com.dragonminez.client.render;

import com.dragonminez.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CameraProjectionCapture {
	private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
	private static final Matrix4f VIEW = new Matrix4f();
	private static final Matrix3f VIEW_NORMAL = new Matrix3f();
	private static Vec3 cameraPosition = Vec3.ZERO;
	private static long capturedFrame = -1L;
	private static long frameCounter;
	private static ClientLevel capturedLevel;
	private static Entity capturedEntity;
	private static CameraType capturedCameraType;

	private CameraProjectionCapture() {}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			reset();
			return;
		}
		VIEW.set(event.getModelViewMatrix());
		VIEW_NORMAL.set(VIEW);
		VIEW_PROJECTION.set(event.getProjectionMatrix()).mul(VIEW);
		cameraPosition = event.getCamera().getPosition();
		capturedLevel = mc.level;
		capturedEntity = mc.getCameraEntity();
		capturedCameraType = mc.options.getCameraType();
		capturedFrame = ++frameCounter;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Pre event) {
		if (!isAvailable()) reset();
	}

	private static void reset() {
		capturedFrame = -1L;
		capturedLevel = null;
		capturedEntity = null;
		capturedCameraType = null;
		cameraPosition = Vec3.ZERO;
	}

	public static PoseStack viewPoseStack(PoseStack fallback) {
		if (!isAvailable()) return fallback;
		PoseStack view = new PoseStack();
		view.last().pose().set(VIEW);
		view.last().normal().set(VIEW_NORMAL);
		return view;
	}

	public static boolean isAvailable() {
		Minecraft mc = Minecraft.getInstance();
		return capturedFrame >= 0L && mc.level != null && capturedLevel == mc.level
				&& capturedEntity == mc.getCameraEntity() && capturedCameraType == mc.options.getCameraType();
	}

	public static boolean project(Vec3 worldPosition, float screenWidth, float screenHeight, float[] out) {
		if (!isAvailable()) return false;
		return project(VIEW_PROJECTION, cameraPosition, worldPosition, screenWidth, screenHeight, out);
	}

	static boolean project(Matrix4f viewProjection, Vec3 camera, Vec3 worldPosition,
			float screenWidth, float screenHeight, float[] out) {
		Vector4f clip = new Vector4f(
				(float) (worldPosition.x - camera.x),
				(float) (worldPosition.y - camera.y),
				(float) (worldPosition.z - camera.z), 1.0f);
		viewProjection.transform(clip);
		if (!Float.isFinite(clip.w) || clip.w <= 1.0e-4f) return false;
		float ndcX = clip.x / clip.w;
		float ndcY = clip.y / clip.w;
		if (!Float.isFinite(ndcX) || !Float.isFinite(ndcY)) return false;
		out[0] = (ndcX + 1.0f) * 0.5f * screenWidth;
		out[1] = (1.0f - ndcY) * 0.5f * screenHeight;
		out[2] = ndcX;
		out[3] = ndcY;
		return true;
	}
}
