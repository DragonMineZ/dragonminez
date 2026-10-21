package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.GodRitualHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class GodRitualShellEffect {
	private static final float MIN_ALPHA = 0.003f;

	private static final List<Capture> POOL = new ArrayList<>();
	private static final Map<Integer, Capture> FRAME = new LinkedHashMap<>();
	private static boolean capturing;

	private static final class Capture {
		private final Map<RenderType, BufferBuilder> builders = new HashMap<>();
		private float alpha;

		private BufferBuilder builder(RenderType type) {
			BufferBuilder builder = builders.computeIfAbsent(type, t -> new BufferBuilder(t.bufferSize()));
			if (!builder.building()) builder.begin(type.mode(), type.format());
			return builder;
		}
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
			return VertexMultiConsumer.create(main, new WhiteConsumer(target.builder(shell)));
		};
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
			FRAME.clear();
			for (Capture capture : POOL) {
				for (BufferBuilder builder : capture.builders.values()) {
					if (!builder.building()) continue;
					BufferBuilder.RenderedBuffer stale = builder.endOrDiscardIfEmpty();
					if (stale != null) stale.release();
				}
			}
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
				for (Map.Entry<RenderType, BufferBuilder> entry : capture.builders.entrySet()) {
					if (entry.getValue().building()) entry.getKey().end(entry.getValue(), RenderSystem.getVertexSorting());
				}
			}
		} finally {
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			FRAME.clear();
		}
	}

	private record WhiteConsumer(VertexConsumer delegate) implements VertexConsumer {
		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			delegate.vertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			delegate.color(255, 255, 255, 255);
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
			delegate.defaultColor(255, 255, 255, 255);
		}

		@Override
		public void unsetDefaultColor() {
			delegate.unsetDefaultColor();
		}
	}
}
