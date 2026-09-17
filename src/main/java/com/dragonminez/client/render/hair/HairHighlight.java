package com.dragonminez.client.render.hair;

import net.minecraft.client.renderer.LightTexture;

public final class HairHighlight {
	public static final long PERIOD_MS = 2000L;
	public static final long RISE_MS = 200L;
	public static final long HOLD_MS = 800L;
	public static final long FALL_MS = 350L;
	public static final float PEAK_WHITE = 0.85f;
	public static final float SEGMENT_CONTEXT_TINT = 0.2f;
	public static final float HOVER_TINT = 0.12f;

	private HairHighlight() {}

	public static float envelope(long elapsedMs) {
		long t = Math.floorMod(elapsedMs, PERIOD_MS);
		if (t < RISE_MS) {
			float x = (float) t / RISE_MS;
			float inverse = 1.0f - x;
			return 1.0f - inverse * inverse * inverse;
		}
		if (t < RISE_MS + HOLD_MS) return 1.0f;
		if (t < RISE_MS + HOLD_MS + FALL_MS) {
			float x = (float) (t - RISE_MS - HOLD_MS) / FALL_MS;
			return 1.0f - (float) (-(Math.cos(Math.PI * x) - 1.0) / 2.0);
		}
		return 0.0f;
	}

	public static int applyLight(int packedLight, float intensity) {
		if (intensity <= 0.0f) return packedLight;
		int block = LightTexture.block(packedLight);
		int sky = LightTexture.sky(packedLight);
		return LightTexture.pack(Math.round(block + (15 - block) * intensity), Math.round(sky + (15 - sky) * intensity));
	}

	public static int applyColor(int rgb, float intensity) {
		if (intensity <= 0.0f) return rgb;
		float white = intensity * PEAK_WHITE;
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		r = Math.round(r + (255 - r) * white);
		g = Math.round(g + (255 - g) * white);
		b = Math.round(b + (255 - b) * white);
		return (r << 16) | (g << 8) | b;
	}
}
