package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.events.LockOnEvent;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class AfterimageEffect {

	private static final int BAND_PASSES = 3;
	private static final float BASE_ALPHA = 0.85f;
	private static final float FADE_START = 0.45f;
	private static final float MIN_SPLIT = 0.05f;
	private static final float MAX_SPLIT = 0.55f;
	private static final float START_CENTER_WEIGHT = 0.72f;
	private static final float END_CENTER_WEIGHT = 0.34f;
	private static final float MAX_DROPOUT = 0.8f;
	private static final double MAX_RENDER_DISTANCE_SQR = 96.0 * 96.0;
	private static final int MAX_ENTRIES_PER_ENTITY = 8;

	private static final class Entry {
		final int entityId;
		final Vec3[] positions;
		final float[] yaws;
		final int totalTicks;
		int ticksRemaining;

		private Entry(int entityId, Vec3[] positions, float[] yaws, int totalTicks) {
			this.entityId = entityId;
			this.positions = positions;
			this.yaws = yaws;
			this.totalTicks = totalTicks;
			this.ticksRemaining = totalTicks;
		}

		float getProgress(float partialTick) {
			if (totalTicks <= 0) return 1.0f;
			return Mth.clamp((totalTicks - ticksRemaining + partialTick) / (float) totalTicks, 0.0f, 1.0f);
		}
	}

	private static final List<Entry> ACTIVE = new ArrayList<>();
	private static MultiBufferSource.BufferSource buffers;
	private static final GhostBufferSource GHOST_SOURCE = new GhostBufferSource();

	private static boolean rendering = false;

	private AfterimageEffect() {}

	public static boolean isRendering() {
		return rendering;
	}

	public static void start(int entityId, int durationTicks, Vec3[] positions, float[] yaws, boolean breakLockOn) {
		if (breakLockOn) {
			LivingEntity locked = LockOnEvent.getLockedTarget();
			if (locked != null && locked.getId() == entityId) LockOnEvent.unlock();
		}
		if (durationTicks <= 0 || positions == null || yaws == null || positions.length == 0) return;
		int owned = 0;
		for (Entry entry : ACTIVE) if (entry.entityId == entityId) owned++;
		if (owned >= MAX_ENTRIES_PER_ENTITY) {
			for (Iterator<Entry> it = ACTIVE.iterator(); it.hasNext(); ) {
				if (it.next().entityId == entityId) {
					it.remove();
					break;
				}
			}
		}
		ACTIVE.add(new Entry(entityId, positions, yaws, durationTicks));
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			ACTIVE.clear();
			return;
		}
		if (mc.isPaused() || ACTIVE.isEmpty()) return;

		Iterator<Entry> it = ACTIVE.iterator();
		while (it.hasNext()) {
			Entry entry = it.next();
			entry.ticksRemaining--;
			if (entry.ticksRemaining <= 0) it.remove();
		}
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
		if (ACTIVE.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		ShaderInstance shader = DMZShaders.afterimageShader;
		boolean customShader = shader != null && !IrisCompat.isShaderPackInUse();

		float partialTick = event.getPartialTick();
		Vec3 camPos = event.getCamera().getPosition();
		Vector3f camLeft = event.getCamera().getLeftVector();
		PoseStack poseStack = event.getPoseStack();
		float time = (mc.level.getGameTime() % 24000L + partialTick) / 20.0f;
		float bandHeight = Math.max(2.0f, mc.getWindow().getHeight() / 220.0f);

		for (Entry entry : ACTIVE) {
			Entity entity = mc.level.getEntity(entry.entityId);
			if (!(entity instanceof LivingEntity player) || !player.isAlive() || player.isSpectator()) continue;

			EntityRenderer renderer = player instanceof AbstractClientPlayer acp ? DMZRendererCache.getTPRenderer(acp) : mc.getEntityRenderDispatcher().getRenderer(player);
			if (renderer == null) continue;

			float progress = entry.getProgress(partialTick);
			float fade = Mth.clamp((progress - FADE_START) / (1.0f - FADE_START), 0.0f, 1.0f);
			float alpha = BASE_ALPHA * (1.0f - fade * fade * (3.0f - 2.0f * fade));
			if (alpha <= 0.01f) continue;

			float split = Mth.lerp(progress, MIN_SPLIT, MAX_SPLIT);
			float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
			float entityYaw = Mth.lerp(partialTick, player.yRotO, player.getYRot());

			if (customShader) {
				shader.safeGetUniform("centerWeight").set(Mth.lerp(progress, START_CENTER_WEIGHT, END_CENTER_WEIGHT));
				shader.safeGetUniform("bandHeight").set(bandHeight);
				shader.safeGetUniform("dropout").set(progress * progress * MAX_DROPOUT);
				shader.safeGetUniform("whiten").set(0.12f + 0.38f * progress);
				shader.safeGetUniform("globalAlpha").set(alpha);
				shader.safeGetUniform("time").set(time);
			}

			int passes = customShader ? BAND_PASSES : 1;
			for (int i = 0; i < entry.positions.length; i++) {
				Vec3 pos = entry.positions[i];
				if (pos.distanceToSqr(camPos) > MAX_RENDER_DISTANCE_SQR) continue;
				int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(pos.x, pos.y + player.getBbHeight() * 0.5, pos.z));

				for (int pass = 0; pass < passes; pass++) {
					float shift = pass == 0 ? 0.0f : (pass == 1 ? split : -split);
					if (customShader) shader.safeGetUniform("bandPass").set((float) pass);

					poseStack.pushPose();
					poseStack.translate(pos.x - camPos.x + camLeft.x() * shift, pos.y - camPos.y + camLeft.y() * shift, pos.z - camPos.z + camLeft.z() * shift);
					poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw - entry.yaws[i]));
					renderGhost(renderer, player, entityYaw, partialTick, poseStack, light, customShader, alpha);
					poseStack.popPose();
				}
			}
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void renderGhost(EntityRenderer renderer, LivingEntity player, float entityYaw, float partialTick,
									PoseStack poseStack, int light, boolean customShader, float alpha) {
		rendering = true;
		try {
			if (buffers == null) buffers = MultiBufferSource.immediate(new BufferBuilder(4096));
			GHOST_SOURCE.prepare(customShader, alpha);
			renderer.render(player, entityYaw, partialTick, poseStack, GHOST_SOURCE, light);
			buffers.endBatch();
		} finally {
			rendering = false;
		}
	}

	private static final class GhostBufferSource implements MultiBufferSource {
		private boolean customShader;
		private float alpha;

		void prepare(boolean customShader, float alpha) {
			this.customShader = customShader;
			this.alpha = alpha;
		}

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			RenderType mapped = ModRenderTypes.afterimage(renderType, customShader);
			if (mapped == null) return EmptyConsumer.INSTANCE;
			VertexConsumer consumer = buffers.getBuffer(mapped);
			return customShader ? consumer : new AlphaConsumer(consumer, alpha);
		}
	}

	private static final class AlphaConsumer implements VertexConsumer {
		private final VertexConsumer delegate;
		private final float alpha;

		private AlphaConsumer(VertexConsumer delegate, float alpha) {
			this.delegate = delegate;
			this.alpha = alpha;
		}

		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			delegate.vertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			delegate.color(red, green, blue, Math.round(alpha * this.alpha));
			return this;
		}

		@Override
		public VertexConsumer uv(float u, float v) {
			delegate.uv(u, v);
			return this;
		}

		@Override
		public VertexConsumer overlayCoords(int u, int v) {
			delegate.overlayCoords(u, v);
			return this;
		}

		@Override
		public VertexConsumer uv2(int u, int v) {
			delegate.uv2(u, v);
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			delegate.normal(x, y, z);
			return this;
		}

		@Override
		public void endVertex() {
			delegate.endVertex();
		}

		@Override
		public void defaultColor(int red, int green, int blue, int alpha) {
			delegate.defaultColor(red, green, blue, Math.round(alpha * this.alpha));
		}

		@Override
		public void unsetDefaultColor() {
			delegate.unsetDefaultColor();
		}
	}

	private static final class EmptyConsumer implements VertexConsumer {
		private static final EmptyConsumer INSTANCE = new EmptyConsumer();

		@Override
		public VertexConsumer vertex(double x, double y, double z) { return this; }

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) { return this; }

		@Override
		public VertexConsumer uv(float u, float v) { return this; }

		@Override
		public VertexConsumer overlayCoords(int u, int v) { return this; }

		@Override
		public VertexConsumer uv2(int u, int v) { return this; }

		@Override
		public VertexConsumer normal(float x, float y, float z) { return this; }

		@Override
		public void endVertex() {}

		@Override
		public void defaultColor(int red, int green, int blue, int alpha) {}

		@Override
		public void unsetDefaultColor() {}
	}
}
