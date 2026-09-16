package com.dragonminez.client.render.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

public final class AuraNoiseTexture {
	private static final int SIZE = 256;
	private static final int[] PERIODS = {8, 16, 32, 64};
	private static final float[] WEIGHTS = {0.55f, 0.25f, 0.13f, 0.07f};

	private static DynamicTexture texture;

	private AuraNoiseTexture() {}

	public static int getId() {
		if (texture == null) texture = build();
		return texture.getId();
	}

	private static DynamicTexture build() {
		float[] red = field(0x5EED1234);
		float[] green = field(0x0DDBA11);

		NativeImage image = new NativeImage(NativeImage.Format.RGBA, SIZE, SIZE, false);
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				int i = y * SIZE + x;
				int r = Math.round(red[i] * 255.0f);
				int g = Math.round(green[i] * 255.0f);
				image.setPixelRGBA(x, y, 0xFF000000 | (g << 8) | r);
			}
		}

		DynamicTexture built = new DynamicTexture(image);
		built.setFilter(true, false);
		GlStateManager._bindTexture(built.getId());
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		GlStateManager._bindTexture(0);
		return built;
	}

	private static float[] field(int seed) {
		float[] out = new float[SIZE * SIZE];
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;

		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				float u = (float) x / SIZE;
				float v = (float) y / SIZE;
				float sum = 0.0f;
				for (int o = 0; o < PERIODS.length; o++) {
					sum += valueNoise(u * PERIODS[o], v * PERIODS[o], PERIODS[o], seed + o * 7919) * WEIGHTS[o];
				}
				out[y * SIZE + x] = sum;
				min = Math.min(min, sum);
				max = Math.max(max, sum);
			}
		}

		float range = Math.max(1.0e-4f, max - min);
		for (int i = 0; i < out.length; i++) out[i] = (out[i] - min) / range;
		return out;
	}

	private static float valueNoise(float x, float y, int period, int seed) {
		int x0 = (int) Math.floor(x);
		int y0 = (int) Math.floor(y);
		float fx = fade(x - x0);
		float fy = fade(y - y0);

		float a = lattice(x0, y0, period, seed);
		float b = lattice(x0 + 1, y0, period, seed);
		float c = lattice(x0, y0 + 1, period, seed);
		float d = lattice(x0 + 1, y0 + 1, period, seed);
		return lerp(lerp(a, b, fx), lerp(c, d, fx), fy);
	}

	private static float lattice(int x, int y, int period, int seed) {
		int px = Math.floorMod(x, period);
		int py = Math.floorMod(y, period);
		int h = px * 374761393 + py * 668265263 + seed * 1442695041;
		h = (h ^ (h >>> 13)) * 1274126177;
		h ^= h >>> 16;
		return (h & 0xFFFF) / 65535.0f;
	}

	private static float fade(float t) {
		return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}
}
