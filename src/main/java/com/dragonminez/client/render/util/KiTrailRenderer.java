package com.dragonminez.client.render.util;

import com.dragonminez.client.render.shader.DMZShaders;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KiTrailRenderer {
	private static final int MAX_SAMPLES = 14;
	private static final float MAX_LENGTH = 5.5f;
	private static final float HEAD_HALF_WIDTH = 0.38f;
	private static final float WIDTH_FALLOFF = 0.75f;
	private static final float ALPHA_FALLOFF = 1.15f;
	private static final float HEAD_ALPHA = 1.0f;
	private static final float NOISE_STRETCH = 0.20f;
	private static final float MIN_SEGMENT_SQR = 1.0e-6f;
	private static final int PRUNE_INTERVAL = 40;
	private static final int FULL_BRIGHT = 15728880;

	private static final Map<Integer, Deque<Vec3>> TRAILS = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> LAST_SAMPLE_TICK = new ConcurrentHashMap<>();
	private static final BufferBuilder BUILDER = new BufferBuilder(4096);
	private static VertexBuffer buffer;

	private KiTrailRenderer() {}

	public static void update(Entity entity) {
		int id = entity.getId();
		int tick = entity.tickCount;
		Integer last = LAST_SAMPLE_TICK.put(id, tick);
		if (last != null && last == tick) return;

		Deque<Vec3> points = TRAILS.computeIfAbsent(id, key -> new ArrayDeque<>());
		points.addFirst(centre(entity, 1.0f));
		while (points.size() > MAX_SAMPLES) points.removeLast();

		if (tick % PRUNE_INTERVAL == 0) prune();
	}

	private static void prune() {
		ClientLevel level = Minecraft.getInstance().level;
		TRAILS.keySet().removeIf(id -> {
			Entity entity = level != null ? level.getEntity(id) : null;
			return entity == null || entity.isRemoved();
		});
		LAST_SAMPLE_TICK.keySet().removeIf(id -> !TRAILS.containsKey(id));
	}

	private static Vec3 centre(Entity entity, float partialTick) {
		return new Vec3(
				Mth.lerp(partialTick, entity.xo, entity.getX()),
				Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() * 0.5,
				Mth.lerp(partialTick, entity.zo, entity.getZ()));
	}

	public static void render(Entity entity, Matrix4f entityPose, Matrix4f projection, float[] core, float[] border,
							  float[] outline, float size, float ageInTicks, float partialTick) {
		ShaderInstance shader = DMZShaders.ki3dShader;
		Deque<Vec3> recorded = TRAILS.get(entity.getId());
		if (shader == null || recorded == null || recorded.isEmpty()) return;

		Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		Vec3 head = centre(entity, partialTick);
		Vec3 offset = new Vec3(
				Mth.lerp(partialTick, entity.xo, entity.getX()) - camera.x,
				Mth.lerp(partialTick, entity.yo, entity.getY()) - camera.y,
				Mth.lerp(partialTick, entity.zo, entity.getZ()) - camera.z);
		Matrix4f view = new Matrix4f(entityPose).translate((float) -offset.x, (float) -offset.y, (float) -offset.z);

		List<Vec3> points = new ArrayList<>(recorded.size() + 1);
		points.add(head);
		float length = 0.0f;
		for (Vec3 sample : recorded) {
			Vec3 previous = points.get(points.size() - 1);
			double step = sample.distanceTo(previous);
			if (step * step < MIN_SEGMENT_SQR) continue;
			if (length + step > MAX_LENGTH * size) {
				double keep = (MAX_LENGTH * size - length) / step;
				points.add(previous.lerp(sample, keep));
				length = MAX_LENGTH * size;
				break;
			}
			points.add(sample);
			length += (float) step;
		}
		if (points.size() < 2 || length < 0.05f) return;

		BUILDER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
		int count = points.size();
		Vec3[] left = new Vec3[count];
		Vec3[] right = new Vec3[count];
		float[] ages = new float[count];
		float travelled = 0.0f;
		for (int i = 0; i < count; i++) {
			Vec3 here = points.get(i).subtract(camera);
			Vec3 ahead = points.get(Math.max(0, i - 1)).subtract(camera);
			Vec3 behind = points.get(Math.min(count - 1, i + 1)).subtract(camera);
			if (i > 0) travelled += (float) points.get(i).distanceTo(points.get(i - 1));

			Vec3 along = ahead.subtract(behind);
			if (along.lengthSqr() < MIN_SEGMENT_SQR) along = new Vec3(0.0, 1.0, 0.0);
			Vec3 toCamera = here.lengthSqr() < MIN_SEGMENT_SQR ? new Vec3(0.0, 0.0, 1.0) : here.normalize().scale(-1.0);
			Vec3 side = along.normalize().cross(toCamera);
			if (side.lengthSqr() < MIN_SEGMENT_SQR) side = new Vec3(1.0, 0.0, 0.0);
			side = side.normalize();

			float age = Mth.clamp(travelled / length, 0.0f, 1.0f);
			float halfWidth = HEAD_HALF_WIDTH * size * (float) Math.pow(1.0f - age, WIDTH_FALLOFF);
			ages[i] = age;
			left[i] = here.subtract(side.scale(halfWidth));
			right[i] = here.add(side.scale(halfWidth));
		}

		for (int i = 0; i < count - 1; i++) {
			float along0 = ages[i] * length * NOISE_STRETCH;
			float along1 = ages[i + 1] * length * NOISE_STRETCH;
			int alpha0 = alpha(ages[i]);
			int alpha1 = alpha(ages[i + 1]);
			vertex(left[i], 0, along0, alpha0);
			vertex(right[i], 255, along0, alpha0);
			vertex(right[i + 1], 255, along1, alpha1);
			vertex(left[i + 1], 0, along1, alpha1);
		}

		BufferBuilder.RenderedBuffer rendered = BUILDER.endOrDiscardIfEmpty();
		if (rendered == null) return;
		if (buffer == null) buffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);

		shader.safeGetUniform("colorCore").set(core[0], core[1], core[2]);
		shader.safeGetUniform("colorBorder").set(border[0], border[1], border[2]);
		shader.safeGetUniform("colorOutline").set(outline[0], outline[1], outline[2]);
		shader.safeGetUniform("time").set(ageInTicks / 20.0f);
		shader.safeGetUniform("texBlend").set(0.0f);
		shader.safeGetUniform("alphaMult").set(1.0f);
		shader.safeGetUniform("shapeMode").set(1.0f);
		shader.safeGetUniform("localPosMode").set(1.0f);
		shader.safeGetUniform("flameMode").set(1.0f);

		buffer.bind();
		buffer.upload(rendered);
		buffer.drawWithShader(view, projection, shader);
		VertexBuffer.unbind();

		shader.safeGetUniform("shapeMode").set(0.0f);
		shader.safeGetUniform("localPosMode").set(0.0f);
		shader.safeGetUniform("flameMode").set(0.0f);
	}

	private static int alpha(float age) {
		return Mth.clamp((int) (HEAD_ALPHA * Math.pow(1.0f - age, ALPHA_FALLOFF) * 255.0f), 0, 255);
	}

	private static void vertex(Vec3 pos, int across, float along, int alpha) {
		int alongByte = (int) (Mth.clamp(along, 0.0f, 1.0f) * 255.0f);
		BUILDER.vertex(pos.x, pos.y, pos.z).color(across, 128, alongByte, alpha).uv(0.0f, 0.0f)
				.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(0.0f, 1.0f, 0.0f).endVertex();
	}
}
