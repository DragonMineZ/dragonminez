package com.dragonminez.client.render.effects;

import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.util.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public final class TimeHaloRenderer {
	private static final float PX = 1.0f / 16.0f;
	private static final float CENTER_Y = 26.5f;
	private static final float BACK_OFFSET = 11.0f;
	private static final float RADIUS = 13.65f;
	private static final float HALF_WIDTH = 1.64f;
	private static final float BOTTOM = -1.22f;
	private static final float TOP = 1.46f;
	private static final float THICKNESS = 0.09f;
	private static final float FAR_FACE = 0.0f;
	private static final float NEAR_FACE = 1.0f;

	private static final float IGNITE_RATE = 2.8f;
	private static final float FADE_RATE = 2.2f;
	private static final float IGNITE_SCALE = 0.82f;
	private static final float MAX_STEP = 0.1f;
	private static final long STALE_NANOS = 5_000_000_000L;
	private static final int TIME_WRAP_TICKS = 24000;

	private static final float[] CORE_COLOR = ColorUtils.hexToRgb("#FFFAD9");
	private static final float[] EDGE_COLOR = ColorUtils.hexToRgb("#FFF29A");
	private static final float[] RIM_COLOR = ColorUtils.hexToRgb("#FFEE6B");
	private static final float[] GLOW_COLOR = ColorUtils.hexToRgb("#FFE64D");

	private static final Map<Integer, State> STATES = new HashMap<>();
	private static VertexBuffer buffer;

	private static final class State {
		float intensity;
		long lastNanos;
	}

	private TimeHaloRenderer() {}

	public static void submit(Player player, boolean active, PoseStack poseStack, float partialTick) {
		if (IrisCompat.isRenderingShadowPass() || DMZShaders.timeHaloShader == null) return;
		int id = player.getId();
		long now = System.nanoTime();
		State state = STATES.get(id);
		if (state == null) {
			if (!active) return;
			STATES.values().removeIf(stale -> now - stale.lastNanos > STALE_NANOS);
			state = new State();
			STATES.put(id, state);
		}

		float dt = state.lastNanos == 0L ? 1.0f / 60.0f : Math.min(MAX_STEP, (now - state.lastNanos) / 1.0e9f);
		state.lastNanos = now;
		state.intensity = Mth.clamp(state.intensity + (active ? IGNITE_RATE : -FADE_RATE) * dt, 0.0f, 1.0f);
		if (state.intensity <= 0.0f) {
			STATES.remove(id);
			return;
		}

		Matrix4f pose = new Matrix4f(poseStack.last().pose());
		Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
		float time = (player.tickCount % TIME_WRAP_TICKS + partialTick) / 20.0f;
		float intensity = state.intensity;
		PlayerEffectQueue.addEntityEffect(() -> {
			draw(pose, projection, time, intensity, false);
			AuraRenderer.captureBloom(() -> draw(pose, projection, time, intensity, true));
		});
	}

	private static void draw(Matrix4f pose, Matrix4f projection, float time, float intensity, boolean bloom) {
		ShaderInstance shader = DMZShaders.timeHaloShader;
		if (shader == null) return;

		float ease = 1.0f - (1.0f - intensity) * (1.0f - intensity);
		float radius = RADIUS * PX * Mth.lerp(ease, IGNITE_SCALE, 1.0f);
		float centerY = CENTER_Y * PX;
		float z = BACK_OFFSET * PX;

		Vector3f camera = new Matrix4f(pose).invert().transformPosition(new Vector3f());
		camera.set(camera.x() / radius, (camera.y() - centerY) / radius, (camera.z() - z) / radius);

		BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		if (bloom) {
			quad(builder, radius, centerY, z, NEAR_FACE);
		} else {
			float near = (camera.z() >= 0.0f ? 0.5f : -0.5f) * THICKNESS * radius;
			quad(builder, radius, centerY, z - near, FAR_FACE);
			quad(builder, radius, centerY, z + near, NEAR_FACE);
		}

		shader.safeGetUniform("Time").set(time);
		shader.safeGetUniform("Intensity").set(intensity);
		shader.safeGetUniform("BloomMode").set(bloom ? 1.0f : 0.0f);
		shader.safeGetUniform("CameraPos").set(camera.x(), camera.y(), camera.z());
		shader.safeGetUniform("Bounds").set(-HALF_WIDTH, BOTTOM, HALF_WIDTH, TOP);
		shader.safeGetUniform("Thickness").set(THICKNESS);
		shader.safeGetUniform("CoreColor").set(CORE_COLOR[0], CORE_COLOR[1], CORE_COLOR[2]);
		shader.safeGetUniform("EdgeColor").set(EDGE_COLOR[0], EDGE_COLOR[1], EDGE_COLOR[2]);
		shader.safeGetUniform("RimColor").set(RIM_COLOR[0], RIM_COLOR[1], RIM_COLOR[2]);
		shader.safeGetUniform("GlowColor").set(GLOW_COLOR[0], GLOW_COLOR[1], GLOW_COLOR[2]);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();

		if (buffer == null) buffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
		buffer.bind();
		buffer.upload(builder.buildOrThrow());
		buffer.drawWithShader(pose, projection, shader);
		VertexBuffer.unbind();
		if (bloom) shader.safeGetUniform("BloomMode").set(0.0f);

		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	private static void quad(BufferBuilder builder, float radius, float centerY, float z, float role) {
		corner(builder, radius, centerY, z, -HALF_WIDTH, BOTTOM, role);
		corner(builder, radius, centerY, z, HALF_WIDTH, BOTTOM, role);
		corner(builder, radius, centerY, z, HALF_WIDTH, TOP, role);
		corner(builder, radius, centerY, z, -HALF_WIDTH, TOP, role);
	}

	private static void corner(BufferBuilder builder, float radius, float centerY, float z, float u, float v, float role) {
		builder.addVertex(u * radius, centerY + v * radius, z).setColor(role, 0.0f, 0.0f, 1.0f).setUv(u, v);
	}
}
