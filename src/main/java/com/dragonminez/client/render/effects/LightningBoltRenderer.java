package com.dragonminez.client.render.effects;

import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.IrisCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public final class LightningBoltRenderer {
	public static final float PLAYER_HEIGHT = 1.9f;
	public static final float PLAYER_RADIUS = 0.62f;

	private static final int TRACKS = 2;
	private static final int TRACK_BOLTS = 2;
	private static final int FIRST_PERSON_TRACK_BOLTS = 1;
	private static final float CHARGED_PERIOD = 7.0f;
	private static final float CHARGED_WINDOW = 0.90f;
	private static final float IDLE_PERIOD = 14.0f;
	private static final float IDLE_WINDOW = 0.80f;
	private static final float START_SPREAD = 0.25f;
	private static final int STRIKES_PER_LIFE = 2;
	private static final float FADE_START = 0.70f;

	private static final float WIDTH_PER_RADIUS = 0.085f;
	private static final float CHARGED_WIDTH = 1.35f;
	private static final float IDLE_WIDTH = 0.95f;
	private static final float MITER_LIMIT = 0.40f;
	private static final float BARB_CHANCE = 0.50f;
	private static final int MAX_BARBS = 3;
	private static final float CORE_WHITE = 0.85f;

	private static final float FIRST_PERSON_RADIUS = 2.4f;
	private static final float FIRST_PERSON_WIDTH = 0.45f;
	private static final float FIRST_PERSON_LOW = 0.42f;
	private static final float FIRST_PERSON_HIGH = 0.80f;
	private static final float FIRST_PERSON_ARC = 1.3f;

	private static final Matrix4f IDENTITY = new Matrix4f();
	private static final BufferBuilder BUILDER = new BufferBuilder(8192);
	private static VertexBuffer mesh;

	private LightningBoltRenderer() {}

	public static boolean isAvailable() {
		return DMZShaders.lightningBoltShader != null;
	}

	public static void draw(Matrix4f pose, Matrix4f projection, int seed, float ageTicks, float height, float radius,
							float[] color, boolean charged, float speed, float alpha, boolean firstPerson) {
		if (!isAvailable() || alpha <= 0.01f) return;

		Matrix4f poseCopy = new Matrix4f(pose);
		Matrix4f projectionCopy = new Matrix4f(projection);
		float[] colorCopy = color.clone();

		render(poseCopy, projectionCopy, seed, ageTicks, height, radius, colorCopy, charged, speed, alpha, firstPerson, false);
		AuraRenderer.captureBloom(() -> render(poseCopy, projectionCopy, seed, ageTicks, height, radius, colorCopy, charged, speed, alpha, firstPerson, true));
	}

	private static void render(Matrix4f pose, Matrix4f projection, int seed, float ageTicks, float height, float radius,
							   float[] color, boolean charged, float speed, float alpha, boolean firstPerson, boolean bloom) {
		ShaderInstance shader = DMZShaders.lightningBoltShader;
		if (shader == null) return;

		BUILDER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		for (int track = 0; track < TRACKS; track++) {
			emitBurst(pose, seed, track, ageTicks * speed, height, radius, charged, firstPerson);
		}
		BufferBuilder.RenderedBuffer rendered = BUILDER.endOrDiscardIfEmpty();
		if (rendered == null) return;

		if (mesh == null) mesh = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
		mesh.bind();
		mesh.upload(rendered);

		shader.safeGetUniform("CoreColor").set(Mth.lerp(CORE_WHITE, color[0], 1.0f), Mth.lerp(CORE_WHITE, color[1], 1.0f), Mth.lerp(CORE_WHITE, color[2], 1.0f));
		shader.safeGetUniform("EdgeColor").set(color[0], color[1], color[2]);
		shader.safeGetUniform("Alpha").set(alpha);
		shader.safeGetUniform("BloomPass").set(bloom ? 1.0f : 0.0f);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		if (IrisCompat.isShaderPackInUse()) {
			RenderSystem.disableDepthTest();
		} else {
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL11.GL_LEQUAL);
		}

		mesh.drawWithShader(IDENTITY, projection, shader);
		VertexBuffer.unbind();

		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static void emitBurst(Matrix4f pose, int seed, int track, float clockTicks, float height, float radius,
								  boolean charged, boolean firstPerson) {
		float period = charged ? CHARGED_PERIOD : IDLE_PERIOD;
		float window = charged ? CHARGED_WINDOW : IDLE_WINDOW;
		float clock = clockTicks / period + (float) track / TRACKS;
		long cycle = (long) Math.floor(clock);
		float phase = clock - cycle;

		long burstSeed = mix(seed, track, cycle);
		Random pick = new Random(burstSeed);
		int limit = firstPerson ? FIRST_PERSON_TRACK_BOLTS : TRACK_BOLTS;
		int count = 1 + pick.nextInt(limit);
		float baseAngle = pick.nextFloat() * Mth.TWO_PI;
		float baseLevel = pick.nextFloat();
		float sector = Mth.TWO_PI / count;

		for (int i = 0; i < count; i++) {
			float delay = pick.nextFloat() * START_SPREAD;
			float span = window * (0.6f + 0.4f * pick.nextFloat());
			float angleJitter = (pick.nextFloat() - 0.5f) * 0.6f;
			float life = (phase - delay) / span;
			if (life < 0.0f || life >= 1.0f) continue;

			float angle = firstPerson
					? -Mth.HALF_PI + (i - (count - 1) * 0.5f) * FIRST_PERSON_ARC * 0.6f + angleJitter * FIRST_PERSON_ARC * 0.5f
					: baseAngle + sector * (i + angleJitter);
			float level = (baseLevel + (float) i / count) % 1.0f;
			emitBolt(pose, mix((int) burstSeed, i, cycle), life, angle, level, height, radius, charged, firstPerson);
		}
	}

	private static void emitBolt(Matrix4f pose, long shapeSeed, float life, float centreAngle, float level, float height,
								 float radius, boolean charged, boolean firstPerson) {
		int strike = Math.min(STRIKES_PER_LIFE - 1, (int) (life * STRIKES_PER_LIFE));
		Random shape = new Random(shapeSeed);
		Random jitter = new Random(shapeSeed * 31L + strike);

		float fade = life < FADE_START ? 1.0f : 1.0f - (life - FADE_START) / (1.0f - FADE_START);
		float reach = firstPerson ? radius * FIRST_PERSON_RADIUS : radius;
		float scale = new Vector3f(pose.m00(), pose.m01(), pose.m02()).length();
		float width = reach * WIDTH_PER_RADIUS * (charged ? CHARGED_WIDTH : IDLE_WIDTH) * (0.8f + 0.4f * shape.nextFloat()) * (0.6f + 0.4f * fade) * scale;
		if (firstPerson) width *= FIRST_PERSON_WIDTH;

		float low = firstPerson ? FIRST_PERSON_LOW : 0.10f;
		float high = firstPerson ? FIRST_PERSON_HIGH : 0.90f;
		float sweep = (shape.nextBoolean() ? 1.0f : -1.0f) * (firstPerson ? 0.55f + 0.45f * shape.nextFloat() : 0.9f + 1.0f * shape.nextFloat());
		float theta = centreAngle - sweep * 0.5f;
		float rise = (shape.nextFloat() - 0.5f) * height * (firstPerson ? 0.35f : 1.1f);
		float startY = height * Mth.lerp(level, low, high) - rise * 0.5f;
		float baseRadius = reach * (0.95f + 0.45f * shape.nextFloat());
		int segments = firstPerson ? 5 + shape.nextInt(3) : 6 + shape.nextInt(5);

		float[] stops = new float[segments + 1];
		float total = 0.0f;
		for (int k = 1; k <= segments; k++) {
			total += (shape.nextFloat() < 0.4f ? 0.35f : 1.0f) * (0.6f + 0.8f * shape.nextFloat());
			stops[k] = total;
		}

		Vector3f[] points = new Vector3f[segments + 1];
		float[] widths = new float[segments + 1];
		float side = jitter.nextBoolean() ? 1.0f : -1.0f;
		for (int k = 0; k <= segments; k++) {
			boolean end = k == 0 || k == segments;
			float s = stops[k] / total;
			float angle = theta + sweep * s;
			float r = baseRadius * (1.0f + 0.3f * Mth.sin(Mth.PI * s));
			float y = Mth.clamp(startY + rise * s, height * 0.02f, height * 1.10f);

			if (!end) {
				float bend = jitter.nextFloat();
				float amplitude = (bend < 0.25f ? 0.03f : 0.12f + 0.24f * jitter.nextFloat()) * radius;
				if (jitter.nextFloat() < 0.7f) side = -side;
				y += amplitude * side * (0.5f + 0.5f * jitter.nextFloat());
				r += amplitude * (jitter.nextFloat() - 0.5f) * 1.4f;
			}

			points[k] = new Vector3f(Mth.cos(angle) * r, y, Mth.sin(angle) * r);
			float profile = (float) Math.pow(Mth.sin(Mth.PI * s), 0.8);
			float swell = (k % 2 == 0) ? 0.45f + 0.35f * shape.nextFloat() : 0.9f + 0.5f * shape.nextFloat();
			widths[k] = end ? 0.0f : width * profile * swell;
		}

		int alpha = Mth.clamp((int) (fade * 255.0f), 0, 255);
		int barbs = 0;
		for (int k = 1; k < segments && barbs < MAX_BARBS; k++) {
			if (shape.nextFloat() >= BARB_CHANCE) continue;
			barbs++;
			emitBarb(pose, points[k - 1], points[k], widths[k], radius, shape, jitter, alpha);
		}

		for (Vector3f point : points) pose.transformPosition(point);
		ribbon(points, widths, alpha);
	}

	private static void emitBarb(Matrix4f pose, Vector3f previous, Vector3f origin, float originWidth, float radius,
								 Random shape, Random jitter, int alpha) {
		Vector3f direction = new Vector3f(origin).sub(previous);
		if (direction.lengthSquared() < 1.0e-6f) return;
		direction.normalize();
		direction.add((shape.nextFloat() - 0.5f) * 1.2f, (shape.nextFloat() - 0.5f) * 1.2f, (shape.nextFloat() - 0.5f) * 1.2f);
		if (direction.lengthSquared() < 1.0e-6f) return;
		direction.normalize();

		boolean fork = shape.nextFloat() < 0.35f;
		float length = radius * (fork ? 0.45f + 0.45f * shape.nextFloat() : 0.15f + 0.20f * shape.nextFloat());
		float kink = length * 0.25f;
		Vector3f middle = new Vector3f(direction).mul(length * 0.45f).add(origin)
				.add((jitter.nextFloat() - 0.5f) * kink, (jitter.nextFloat() - 0.5f) * kink * 2.0f, (jitter.nextFloat() - 0.5f) * kink);
		Vector3f tip = new Vector3f(direction).mul(length).add(origin);

		Vector3f[] points = {new Vector3f(origin), middle, tip};
		float[] widths = {originWidth * 0.7f, originWidth * (fork ? 0.45f : 0.3f), 0.0f};
		for (Vector3f point : points) pose.transformPosition(point);
		ribbon(points, widths, alpha);
	}

	private static void ribbon(Vector3f[] points, float[] widths, int alpha) {
		int count = points.length;
		float[] normalX = new float[count - 1];
		float[] normalY = new float[count - 1];
		for (int i = 0; i < count - 1; i++) {
			float dx = points[i + 1].x - points[i].x;
			float dy = points[i + 1].y - points[i].y;
			float length = Mth.sqrt(dx * dx + dy * dy);
			if (length < 1.0e-5f) {
				normalX[i] = i > 0 ? normalX[i - 1] : 0.0f;
				normalY[i] = i > 0 ? normalY[i - 1] : 1.0f;
			} else {
				normalX[i] = -dy / length;
				normalY[i] = dx / length;
			}
		}

		float[] offsetX = new float[count];
		float[] offsetY = new float[count];
		for (int i = 0; i < count; i++) {
			int before = Math.max(i - 1, 0);
			int after = Math.min(i, count - 2);
			float mx = normalX[before] + normalX[after];
			float my = normalY[before] + normalY[after];
			float length = Mth.sqrt(mx * mx + my * my);
			if (length < 1.0e-5f) {
				mx = normalX[after];
				my = normalY[after];
			} else {
				mx /= length;
				my /= length;
			}
			float miter = 1.0f / Math.max(mx * normalX[after] + my * normalY[after], MITER_LIMIT);
			offsetX[i] = mx * widths[i] * miter;
			offsetY[i] = my * widths[i] * miter;
		}

		for (int i = 0; i < count - 1; i++) {
			Vector3f a = points[i];
			Vector3f b = points[i + 1];
			float va = (float) i / (count - 1);
			float vb = (float) (i + 1) / (count - 1);

			vertex(a.x + offsetX[i], a.y + offsetY[i], a.z, -1.0f, va, alpha);
			vertex(a.x, a.y, a.z, 0.0f, va, alpha);
			vertex(b.x, b.y, b.z, 0.0f, vb, alpha);
			vertex(b.x + offsetX[i + 1], b.y + offsetY[i + 1], b.z, -1.0f, vb, alpha);

			vertex(a.x, a.y, a.z, 0.0f, va, alpha);
			vertex(a.x - offsetX[i], a.y - offsetY[i], a.z, 1.0f, va, alpha);
			vertex(b.x - offsetX[i + 1], b.y - offsetY[i + 1], b.z, 1.0f, vb, alpha);
			vertex(b.x, b.y, b.z, 0.0f, vb, alpha);
		}
	}

	private static void vertex(float x, float y, float z, float u, float v, int alpha) {
		BUILDER.vertex(x, y, z).uv(u, v).color(255, 255, 255, alpha).endVertex();
	}

	private static long mix(int seed, int slot, long cycle) {
		long h = seed * 0x9E3779B97F4A7C15L + slot * 0xC2B2AE3D27D4EB4FL + cycle * 0x165667B19E3779F9L;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return h;
	}
}
