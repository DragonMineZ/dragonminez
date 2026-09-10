package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.concurrent.ConcurrentHashMap;

public final class AuraTrailRenderer {
	private static final int MAX_SAMPLES = 26;
	private static final float FADE_IN_SAMPLES = 4.0f;
	private static final float MAX_HALF_WIDTH = 0.85f;
	private static final float MIN_SEGMENT_SQR = 1.0e-6f;

	private static final ResourceLocation DUMMY_TEXTURE =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");

	private static final Map<Integer, Deque<Vec3>> TRAILS = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_SAMPLE_TICK = new ConcurrentHashMap<>();
	private static VertexBuffer buffer;

	private AuraTrailRenderer() {}

	public static void update(Player player, boolean recording) {
		int id = player.getId();
		Deque<Vec3> points = TRAILS.get(id);
		if (points == null) {
			if (!recording) return;
			points = new ArrayDeque<>();
			TRAILS.put(id, points);
		}

		long tick = player.tickCount;
		if (LAST_SAMPLE_TICK.getOrDefault(id, Long.MIN_VALUE) == tick) return;
		LAST_SAMPLE_TICK.put(id, tick);

		if (recording) {
			points.addFirst(new Vec3(player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ()));
			while (points.size() > MAX_SAMPLES) points.removeLast();
		} else {
			if (!points.isEmpty()) points.removeLast();
			if (points.isEmpty()) {
				TRAILS.remove(id);
				LAST_SAMPLE_TICK.remove(id);
			}
		}
	}

	public static void forget(Predicate<Integer> keep) {
		TRAILS.keySet().removeIf(id -> !keep.test(id));
		LAST_SAMPLE_TICK.keySet().removeIf(id -> !TRAILS.containsKey(id));
	}

	public static void render(Player player, float[] color, float alpha, PoseStack poseStack,
							  Matrix4f projectionMatrix, float partialTick) {
		Deque<Vec3> recorded = TRAILS.get(player.getId());
		if (recorded == null || recorded.size() < 2 || alpha <= 0.01f) return;

		ShaderInstance shader = DMZShaders.auraTrailShader;
		if (shader == null) return;

		Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

		List<Vec3> points = new ArrayList<>(recorded.size() + 1);
		points.add(new Vec3(
				Mth.lerp(partialTick, player.xo, player.getX()),
				Mth.lerp(partialTick, player.yo, player.getY()) + player.getBbHeight() * 0.5,
				Mth.lerp(partialTick, player.zo, player.getZ())));
		points.addAll(recorded);

		int count = points.size();
		Vec3[] left = new Vec3[count];
		Vec3[] right = new Vec3[count];
		float[] alphas = new float[count];

		for (int i = 0; i < count; i++) {
			Vec3 here = points.get(i).subtract(camera);
			Vec3 ahead = points.get(Math.max(0, i - 1)).subtract(camera);
			Vec3 behind = points.get(Math.min(count - 1, i + 1)).subtract(camera);

			Vec3 along = ahead.subtract(behind);
			if (along.lengthSqr() < MIN_SEGMENT_SQR) along = new Vec3(0.0, 1.0, 0.0);
			Vec3 toCamera = here.lengthSqr() < MIN_SEGMENT_SQR ? new Vec3(0.0, 0.0, 1.0) : here.normalize().scale(-1.0);

			Vec3 side = along.normalize().cross(toCamera);
			if (side.lengthSqr() < MIN_SEGMENT_SQR) side = new Vec3(1.0, 0.0, 0.0);
			side = side.normalize();

			float age = (float) i / (count - 1);
			float halfWidth = MAX_HALF_WIDTH * (float) Math.pow(1.0f - age, 0.65);
			float fadeIn = Math.min(1.0f, i / FADE_IN_SAMPLES);
			alphas[i] = alpha * fadeIn * (float) Math.pow(1.0f - age, 1.6);

			left[i] = here.subtract(side.scale(halfWidth));
			right[i] = here.add(side.scale(halfWidth));
		}

		int lastVisible = -1;
		for (int i = count - 1; i > 0; i--) if (alphas[i] > 0.002f || alphas[i - 1] > 0.002f) { lastVisible = i; break; }
		if (lastVisible < 1) return;

		BufferBuilder builder = Tesselator.getInstance().getBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

		for (int i = 0; i < lastVisible; i++) {
			float u0 = (float) i / (count - 1);
			float u1 = (float) (i + 1) / (count - 1);

			vertex(builder, left[i], alphas[i], u0, 0.0f);
			vertex(builder, right[i], alphas[i], u0, 1.0f);
			vertex(builder, right[i + 1], alphas[i + 1], u1, 1.0f);
			vertex(builder, left[i + 1], alphas[i + 1], u1, 0.0f);
		}

		if (buffer == null) buffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);

		shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
		shader.safeGetUniform("ProjMat").set(projectionMatrix);
		shader.safeGetUniform("time").set((player.tickCount + partialTick) / 20.0f);
		shader.safeGetUniform("alp1").set(1.0f);
		shader.safeGetUniform("color1").set(
				Mth.lerp(0.55f, color[0], 1.0f), Mth.lerp(0.55f, color[1], 1.0f), Mth.lerp(0.55f, color[2], 1.0f));
		shader.safeGetUniform("color2").set(color[0] * 1.25f, color[1] * 1.25f, color[2] * 1.25f);

		RenderType renderType = AuraRenderer.auraType(DUMMY_TEXTURE);
		AuraRenderer.customSetup(renderType, DUMMY_TEXTURE, shader);

		buffer.bind();
		buffer.upload(builder.end());
		buffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
		VertexBuffer.unbind();
		shader.clear();

		AuraRenderer.customClear(renderType);
	}

	private static void vertex(BufferBuilder builder, Vec3 pos, float alpha, float u, float v) {
		builder.vertex(pos.x, pos.y, pos.z).color(1.0f, 1.0f, 1.0f, alpha).uv(u, v).endVertex();
	}
}
