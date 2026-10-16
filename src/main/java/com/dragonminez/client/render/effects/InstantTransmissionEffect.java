package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class InstantTransmissionEffect {

	private static final int VANISH_TICKS = 12;
	private static final int APPEAR_TICKS = 10;
	private static final int ARRIVAL_WAIT_TICKS = 20;
	private static final int TELEPORT_MODE = 1;
	private static final double MAX_RENDER_DISTANCE_SQR = 128.0 * 128.0;

	private static final List<Ghost> GHOSTS = new ArrayList<>();
	private static final Map<Integer, Arrival> ARRIVALS = new HashMap<>();
	private static MultiBufferSource.BufferSource buffers;

	private InstantTransmissionEffect() {
	}

	private record Vertex(float x, float y, float z, int r, int g, int b, int a, float u, float v, int light,
						  float nx, float ny, float nz) {
	}

	private static final class Ghost {
		final Vec3 pos;
		final Map<RenderType, List<Vertex>> meshes;
		int age;

		Ghost(Vec3 pos, Map<RenderType, List<Vertex>> meshes) {
			this.pos = pos;
			this.meshes = meshes;
		}
	}

	private static final class Arrival {
		int age;
		int waited;
	}

	public static void handle(int entityId, boolean arrival) {
		if (arrival) ARRIVALS.put(entityId, new Arrival());
		else depart(entityId);
	}

	private static boolean available() {
		return ModRenderTypes.hasAssimilationSketchShader() && !IrisCompat.isShaderPackInUse();
	}

	private static void depart(int entityId) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || !available()) return;
		Entity entity = level.getEntity(entityId);
		if (entity == null || entity.isInvisible()) return;
		Map<RenderType, List<Vertex>> meshes = capture(entity);
		if (!meshes.isEmpty()) GHOSTS.add(new Ghost(entity.position(), meshes));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Map<RenderType, List<Vertex>> capture(Entity entity) {
		Minecraft mc = Minecraft.getInstance();
		EntityRenderer renderer = entity instanceof AbstractClientPlayer player
				? DMZRendererCache.getTPRenderer(player)
				: mc.getEntityRenderDispatcher().getRenderer(entity);
		if (renderer == null) return Map.of();
		CaptureSource source = new CaptureSource();
		int light = mc.getEntityRenderDispatcher().getPackedLightCoords(entity, 1.0f);
		try {
			renderer.render(entity, entity.getYRot(), 1.0f, new PoseStack(), source, light);
		} catch (Throwable ignored) {
			return Map.of();
		}
		return source.meshes;
	}

	public static MultiBufferSource wrap(Entity entity, MultiBufferSource source, float partialTick) {
		if (ARRIVALS.isEmpty() || DMZSkinLayer.PREVIEW_MODE || IrisCompat.isRenderingShadowPass() || !available()) return source;
		Arrival arrival = ARRIVALS.get(entity.getId());
		if (arrival == null) return source;
		float progress = 1.0f - Mth.clamp((arrival.age + partialTick) / APPEAR_TICKS, 0.0f, 1.0f);
		if (progress <= 0.001f) return source;
		return new NamekAssimilationEffect.SketchSource(source, Math.round(progress * 1000.0f), TELEPORT_MODE);
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) {
			GHOSTS.clear();
			ARRIVALS.clear();
			return;
		}
		if (mc.isPaused()) return;

		GHOSTS.removeIf(ghost -> ++ghost.age > VANISH_TICKS);

		Iterator<Map.Entry<Integer, Arrival>> it = ARRIVALS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Arrival> entry = it.next();
			Arrival arrival = entry.getValue();
			if (level.getEntity(entry.getKey()) == null) {
				if (++arrival.waited > ARRIVAL_WAIT_TICKS) it.remove();
				continue;
			}
			if (++arrival.age > APPEAR_TICKS) it.remove();
		}
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
		if (GHOSTS.isEmpty() || IrisCompat.isRenderingShadowPass() || !available()) return;

		Vec3 cam = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		float partialTick = event.getPartialTick();
		if (buffers == null) buffers = MultiBufferSource.immediate(new BufferBuilder(4096));

		for (Ghost ghost : GHOSTS) {
			if (ghost.pos.distanceToSqr(cam) > MAX_RENDER_DISTANCE_SQR) continue;
			float progress = Mth.clamp((ghost.age + partialTick) / VANISH_TICKS, 0.0f, 1.0f);
			if (progress >= 1.0f) continue;
			int encoded = Math.round(progress * 1000.0f);

			poseStack.pushPose();
			poseStack.translate(ghost.pos.x - cam.x, ghost.pos.y - cam.y, ghost.pos.z - cam.z);
			Matrix4f pose = poseStack.last().pose();
			Matrix3f normal = poseStack.last().normal();
			for (Map.Entry<RenderType, List<Vertex>> mesh : ghost.meshes.entrySet()) {
				RenderType sketch = ModRenderTypes.assimilationSketch(mesh.getKey());
				if (sketch == null) continue;
				VertexConsumer consumer = buffers.getBuffer(sketch);
				for (Vertex v : mesh.getValue()) {
					consumer.vertex(pose, v.x(), v.y(), v.z())
							.color(v.r(), v.g(), v.b(), v.a())
							.uv(v.u(), v.v())
							.overlayCoords(encoded, TELEPORT_MODE)
							.uv2(v.light() & 0xFFFF, v.light() >> 16 & 0xFFFF)
							.normal(normal, v.nx(), v.ny(), v.nz())
							.endVertex();
				}
			}
			poseStack.popPose();
		}
		buffers.endBatch();
	}

	private static final class CaptureSource implements MultiBufferSource {
		final Map<RenderType, List<Vertex>> meshes = new LinkedHashMap<>();

		@Override
		public VertexConsumer getBuffer(RenderType type) {
			if (ModRenderTypes.assimilationSketch(type) == null) return Recorder.DISCARD;
			return new Recorder(meshes.computeIfAbsent(type, t -> new ArrayList<>()));
		}
	}

	private static final class Recorder implements VertexConsumer {
		static final Recorder DISCARD = new Recorder(null);

		private final List<Vertex> out;
		private float x, y, z, u, v, nx, ny, nz;
		private int r = 255, g = 255, b = 255, a = 255, light;
		private boolean colorSet;
		private boolean defaultSet;
		private int dr, dg, db, da;

		Recorder(List<Vertex> out) {
			this.out = out;
		}

		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			this.x = (float) x;
			this.y = (float) y;
			this.z = (float) z;
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			r = red;
			g = green;
			b = blue;
			a = alpha;
			colorSet = true;
			return this;
		}

		@Override
		public VertexConsumer uv(float u, float v) {
			this.u = u;
			this.v = v;
			return this;
		}

		@Override
		public VertexConsumer overlayCoords(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer uv2(int u, int v) {
			light = (u & 0xFFFF) | (v & 0xFFFF) << 16;
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			nx = x;
			ny = y;
			nz = z;
			return this;
		}

		@Override
		public void endVertex() {
			if (out != null) {
				boolean useDefault = !colorSet && defaultSet;
				out.add(new Vertex(x, y, z, useDefault ? dr : r, useDefault ? dg : g, useDefault ? db : b, useDefault ? da : a,
						u, v, light, nx, ny, nz));
			}
			colorSet = false;
		}

		@Override
		public void defaultColor(int red, int green, int blue, int alpha) {
			dr = red;
			dg = green;
			db = blue;
			da = alpha;
			defaultSet = true;
		}

		@Override
		public void unsetDefaultColor() {
			defaultSet = false;
		}
	}
}
