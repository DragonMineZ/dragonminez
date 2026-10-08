package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AuraTrailRenderer {
	private static final int MAX_SAMPLES = 40;
	private static final float AURA_WIDTH_FALLOFF = 0.5f;
	private static final float AURA_ALPHA_FALLOFF = 1.1f;
	private static final float AURA_LINGER_RATE = 0.75f;
	private static final float ENTITY_WIDTH_FALLOFF = 0.65f;
	private static final float ENTITY_ALPHA_FALLOFF = 1.6f;
	private static final float FADE_IN_SAMPLES = 4.0f;
	private static final float MAX_HALF_WIDTH = 1.7f;
	private static final float MIN_SEGMENT_SQR = 1.0e-6f;
	private static final float ANCHOR_BACK = 0.75f;
	private static final double ENTITY_MOVING_SQR = 0.0025;
	private static final float CORE_WHITE = 0.55f;
	private static final float ENTITY_CORE_WHITE = 0.20f;
	private static final float ENTITY_BLOOM = 0.35f;

	private static final ResourceLocation DUMMY_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");

	private static final Map<Integer, Trail> TRAILS = new ConcurrentHashMap<>();
	private static VertexBuffer buffer;

	private static final class Trail {
		final Deque<Vec3> points = new ArrayDeque<>();
		final boolean entityTrail;
		final float widthFalloff;
		final float alphaFalloff;
		final float lingerRate;
		long lastTick = Long.MIN_VALUE;
		int idleTicks;
		int span = 1;
		float[] color;
		float alpha;
		boolean drawn;

		Trail(boolean entityTrail) {
			this.entityTrail = entityTrail;
			this.widthFalloff = entityTrail ? ENTITY_WIDTH_FALLOFF : AURA_WIDTH_FALLOFF;
			this.alphaFalloff = entityTrail ? ENTITY_ALPHA_FALLOFF : AURA_ALPHA_FALLOFF;
			this.lingerRate = entityTrail ? 0.0f : AURA_LINGER_RATE;
		}
	}

	private AuraTrailRenderer() {}

	public static void update(Player player, boolean recording) {
		Vec3 back = player.getViewVector(1.0f).scale(-ANCHOR_BACK);
		Vec3 anchor = new Vec3(player.getX() + back.x, player.getY() + player.getBbHeight() * 0.5 + back.y, player.getZ() + back.z);
		update(player, anchor, recording, MAX_SAMPLES, false);
	}

	private static void update(Entity entity, Vec3 anchor, boolean recording, int maxSamples, boolean entityTrail) {
		int id = entity.getId();
		Trail trail = TRAILS.get(id);
		if (trail == null) {
			if (!recording) return;
			trail = new Trail(entityTrail);
			TRAILS.put(id, trail);
		}

		long tick = entity.tickCount;
		if (trail.lastTick == tick) return;
		trail.lastTick = tick;

		Deque<Vec3> points = trail.points;
		if (recording) {
			trail.idleTicks = 0;
			points.addFirst(anchor);
			while (points.size() > maxSamples) points.removeLast();
			trail.span = Math.max(1, points.size() - 1);
			return;
		}

		if (trail.lingerRate <= 0.0f) {
			if (!points.isEmpty()) points.removeLast();
		} else {
			trail.idleTicks++;
			while (!points.isEmpty() && points.size() - 1 + trail.idleTicks * trail.lingerRate >= trail.span) points.removeLast();
		}
		if (points.size() < 2) TRAILS.remove(id);
	}

	public static void submitEntityTrail(Entity entity, Matrix4f entityPose, float partialTick, float[] color, float alpha,
										 int maxSamples, float halfWidth, float anchorBack, float anchorHeight) {
		submitEntityTrail(entity, entityPose, partialTick, color, alpha, maxSamples, halfWidth, anchorBack, anchorHeight, true);
	}

	public static void submitEntityTrail(Entity entity, Matrix4f entityPose, float partialTick, float[] color, float alpha,
										 int maxSamples, float halfWidth, float anchorBack, float anchorHeight, boolean active) {
		if (IrisCompat.isRenderingShadowPass()) return;

		double dx = entity.getX() - entity.xo;
		double dy = entity.getY() - entity.yo;
		double dz = entity.getZ() - entity.zo;
		boolean moving = active && dx * dx + dy * dy + dz * dz > ENTITY_MOVING_SQR;
		Vec3 back = Vec3.directionFromRotation(0.0f, entity.getYRot()).scale(-anchorBack);
		Vec3 anchor = new Vec3(entity.getX() + back.x, entity.getY() + anchorHeight, entity.getZ() + back.z);
		update(entity, anchor, moving, maxSamples, true);
		if (!TRAILS.containsKey(entity.getId())) return;

		Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		Matrix4f view = new Matrix4f(entityPose).translate(
				(float) -(Mth.lerp(partialTick, entity.xo, entity.getX()) - camera.x),
				(float) -(Mth.lerp(partialTick, entity.yo, entity.getY()) - camera.y),
				(float) -(Mth.lerp(partialTick, entity.zo, entity.getZ()) - camera.z));
		Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
		float[] c = color.clone();

		PlayerEffectQueue.addEntityEffect(() -> {
			if (!renderRibbon(entity, c, alpha, view, projection, partialTick, halfWidth, ENTITY_CORE_WHITE, 1.0f, false)) return;
			AuraRenderer.captureBloom(() -> renderRibbon(entity, c, alpha, view, projection, partialTick, halfWidth, ENTITY_CORE_WHITE, ENTITY_BLOOM, true));
		});
	}

	public static void forget() {
		ClientLevel level = Minecraft.getInstance().level;
		TRAILS.keySet().removeIf(id -> {
			Entity entity = level != null ? level.getEntity(id) : null;
			return entity == null || entity.isRemoved();
		});
	}

	public static void render(Player player, float[] color, float alpha, PoseStack poseStack,
							  Matrix4f projectionMatrix, float partialTick) {
		Trail trail = TRAILS.get(player.getId());
		if (trail != null) {
			trail.color = color.clone();
			trail.alpha = alpha;
			trail.drawn = true;
		}
		drawPlayerTrail(player, color, alpha, poseStack, projectionMatrix, partialTick);
	}

	public static void renderDetached(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) return;

		for (Map.Entry<Integer, Trail> entry : TRAILS.entrySet()) {
			Trail trail = entry.getValue();
			if (trail.entityTrail) continue;
			if (trail.drawn) {
				trail.drawn = false;
				continue;
			}
			if (!(level.getEntity(entry.getKey()) instanceof Player player) || player.isRemoved()) {
				TRAILS.remove(entry.getKey());
				continue;
			}

			update(player, false);
			if (trail.color == null || !TRAILS.containsKey(player.getId())) continue;
			if (player == mc.player && mc.options.getCameraType().isFirstPerson()) continue;

			poseStack.pushPose();
			drawPlayerTrail(player, trail.color, trail.alpha, poseStack, projectionMatrix, partialTick);
			poseStack.popPose();
		}
	}

	private static void drawPlayerTrail(Player player, float[] color, float alpha, PoseStack poseStack,
										Matrix4f projectionMatrix, float partialTick) {
		if (!renderRibbon(player, color, alpha, poseStack.last().pose(), projectionMatrix, partialTick, MAX_HALF_WIDTH, CORE_WHITE, 1.0f, false)) return;

		Matrix4f pose = new Matrix4f(poseStack.last().pose());
		Matrix4f proj = new Matrix4f(projectionMatrix);
		float[] c = color.clone();
		AuraRenderer.captureBloom(() -> renderRibbon(player, c, alpha, pose, proj, partialTick, MAX_HALF_WIDTH, CORE_WHITE, 1.0f, true));
	}

	private static boolean renderRibbon(Entity owner, float[] color, float alpha, Matrix4f pose,
										Matrix4f projectionMatrix, float partialTick, float maxHalfWidth, float coreWhite, float bloomAlpha, boolean bloom) {
		Trail trail = TRAILS.get(owner.getId());
		if (trail == null || trail.points.size() < 2 || alpha <= 0.01f) return false;

		ShaderInstance shader = DMZShaders.auraTrailShader;
		if (shader == null) return false;

		Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

		List<Vec3> points = new ArrayList<>(trail.points);

		int count = points.size();
		Vec3[] left = new Vec3[count];
		Vec3[] right = new Vec3[count];
		float[] alphas = new float[count];

		boolean lingering = trail.idleTicks > 0 && trail.lingerRate > 0.0f;
		float span = lingering ? trail.span : count - 1;
		float idleAge = lingering ? (trail.idleTicks + partialTick) * trail.lingerRate : 0.0f;

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

			float slid = lingering ? i : Math.max(0.0f, i - partialTick);
			float age = Mth.clamp((slid + idleAge) / span, 0.0f, 1.0f);
			float halfWidth = maxHalfWidth * (float) Math.pow(1.0f - age, trail.widthFalloff);
			float fadeIn = Math.min(1.0f, slid / FADE_IN_SAMPLES);
			alphas[i] = alpha * (bloom ? bloomAlpha : 1.0f) * fadeIn * (float) Math.pow(1.0f - age, trail.alphaFalloff);

			left[i] = here.subtract(side.scale(halfWidth));
			right[i] = here.add(side.scale(halfWidth));
		}

		int lastVisible = -1;
		for (int i = count - 1; i > 0; i--) if (alphas[i] > 0.002f || alphas[i - 1] > 0.002f) { lastVisible = i; break; }
		if (lastVisible < 1) return false;

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

		shader.safeGetUniform("modelMatrix").set(pose);
		shader.safeGetUniform("ProjMat").set(projectionMatrix);
		shader.safeGetUniform("time").set((owner.tickCount + partialTick) / 20.0f);
		shader.safeGetUniform("alp1").set(1.0f);
		shader.safeGetUniform("bloomMode").set(bloom ? 1.0f : 0.0f);
		shader.safeGetUniform("color1").set(
				Mth.lerp(coreWhite, color[0], 1.0f), Mth.lerp(coreWhite, color[1], 1.0f), Mth.lerp(coreWhite, color[2], 1.0f));
		shader.safeGetUniform("color2").set(color[0] * 1.25f, color[1] * 1.25f, color[2] * 1.25f);

		RenderType renderType = AuraRenderer.auraType(DUMMY_TEXTURE);
		AuraRenderer.customSetup(renderType, DUMMY_TEXTURE, shader);

		buffer.bind();
		buffer.upload(builder.end());
		buffer.drawWithShader(pose, projectionMatrix, shader);
		VertexBuffer.unbind();
		if (bloom) shader.safeGetUniform("bloomMode").set(0.0f);
		shader.clear();

		AuraRenderer.customClear(renderType);
		return true;
	}

	private static void vertex(BufferBuilder builder, Vec3 pos, float alpha, float u, float v) {
		builder.vertex(pos.x, pos.y, pos.z).color(1.0f, 1.0f, 1.0f, alpha).uv(u, v).endVertex();
	}
}
