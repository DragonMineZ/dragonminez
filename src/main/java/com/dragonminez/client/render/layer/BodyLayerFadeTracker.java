package com.dragonminez.client.render.layer;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BodyLayerFadeTracker {
	public static final float FADE_SPEED = 0.1f;

	public record FadingLayer(String id, ResourceLocation texture, float[] color, float target) {
		public FadingLayer(String id, ResourceLocation texture, float[] color) {
			this(id, texture, color, 1.0f);
		}
	}

	public record RenderEntry(ResourceLocation texture, float[] color, float alpha) {}

	private static final class Entry {
		float progress;
		float target;
		ResourceLocation texture;
		float[] color;
		boolean active;
	}

	private static final Map<Integer, Map<String, Entry>> LAYERS = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_UPDATE = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_SEEN = new ConcurrentHashMap<>();

	private BodyLayerFadeTracker() {}

	public static List<RenderEntry> update(int entityId, long gameTime, List<FadingLayer> activeLayers) {
		Map<String, Entry> layers = LAYERS.computeIfAbsent(entityId, k -> new LinkedHashMap<>());

		Long lastSeen = LAST_SEEN.get(entityId);
		boolean snap = lastSeen == null || gameTime - lastSeen > 2L;
		LAST_SEEN.put(entityId, gameTime);

		for (Entry e : layers.values()) {
			e.active = false;
			e.target = 0.0f;
		}

		for (FadingLayer l : activeLayers) {
			Entry e = layers.get(l.id());
			if (e == null) {
				e = new Entry();
				e.progress = snap ? l.target() : 0.0f;
				layers.put(l.id(), e);
			}
			e.texture = l.texture();
			e.color = l.color();
			e.target = l.target();
			e.active = true;
		}

		Long lastUpdate = LAST_UPDATE.get(entityId);
		boolean advance = lastUpdate == null || lastUpdate != gameTime;
		if (advance) LAST_UPDATE.put(entityId, gameTime);

		List<RenderEntry> result = new ArrayList<>();
		Iterator<Map.Entry<String, Entry>> it = layers.entrySet().iterator();
		while (it.hasNext()) {
			Entry e = it.next().getValue();
			float target = e.target;
			if (snap) {
				e.progress = target;
			} else if (advance) {
				if (e.progress < target) e.progress = Math.min(target, e.progress + FADE_SPEED);
				else e.progress = Math.max(target, e.progress - FADE_SPEED);
			}

			if (e.progress <= 0.001f && !e.active) {
				it.remove();
				continue;
			}
			if (e.progress > 0.001f && e.texture != null) {
				result.add(new RenderEntry(e.texture, e.color, e.progress));
			}
		}
		return result;
	}

	public static float getProgress(int entityId, String layerId) {
		Map<String, Entry> layers = LAYERS.get(entityId);
		if (layers == null) return 0.0f;
		Entry e = layers.get(layerId);
		return e != null ? e.progress : 0.0f;
	}

	public static float[] getColor(int entityId, String layerId) {
		Map<String, Entry> layers = LAYERS.get(entityId);
		if (layers == null) return null;
		Entry e = layers.get(layerId);
		return e != null ? e.color : null;
	}
}
