package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.GodRitualHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class GodRitualShellEffect {
	private static final float MIN_ALPHA = 0.003f;

	private static final List<Capture> POOL = new ArrayList<>();
	private static final Map<Integer, Capture> FRAME = new LinkedHashMap<>();
	private static boolean capturing;

	private static final class Capture {
		private final MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(new ByteBufferBuilder(4096));
		private float alpha;
	}

	private GodRitualShellEffect() {}

	public static MultiBufferSource wrap(Player player, StatsData stats, MultiBufferSource source, float partialTick) {
		if (!capturing || DMZSkinLayer.PREVIEW_MODE || IrisCompat.isRenderingShadowPass()) return source;
		float alpha = GodRitualHelper.shellAlpha(stats, player.level().getGameTime() + partialTick);
		if (alpha <= MIN_ALPHA) return source;
		Capture capture = FRAME.get(player.getId());
		if (capture == null) {
			if (POOL.size() <= FRAME.size()) POOL.add(new Capture());
			capture = POOL.get(FRAME.size());
			FRAME.put(player.getId(), capture);
		}
		capture.alpha = alpha;
		Capture target = capture;
		return renderType -> {
			VertexConsumer main = source.getBuffer(renderType);
			RenderType shell = ModRenderTypes.ritualShell(renderType);
			if (shell == null) return main;
			return VertexMultiConsumer.create(main, new WhiteConsumer(target.buffers.getBuffer(shell)));
		};
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
			FRAME.clear();
			capturing = true;
			return;
		}
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
		capturing = false;
		if (FRAME.isEmpty()) return;
		float[] rgb = GodRitualHelper.shellColor();
		try {
			for (Capture capture : FRAME.values()) {
				RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], capture.alpha);
				capture.buffers.endBatch();
			}
		} finally {
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			FRAME.clear();
		}
	}

	private record WhiteConsumer(VertexConsumer delegate) implements VertexConsumer {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			delegate.setColor(255, 255, 255, 255);
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			delegate.setUv(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			delegate.setUv1(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			delegate.setUv2(u, v);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			delegate.setNormal(x, y, z);
			return this;
		}

	}
}
