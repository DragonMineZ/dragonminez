package com.dragonminez.client.render.hair;

import com.dragonminez.common.hair.HairMath;

import java.util.Arrays;

public final class HairColorRamp {
	public static final int STOPS = 9;
	private static final int BASE_STOP = STOPS / 2;
	private static final int STRIDE = STOPS * 3;
	private static final int CACHE_SIZE = 256;

	private static final float WARM_HUE = 95.0f * HairMath.DEG_TO_RAD;
	private static final float COOL_HUE = 285.0f * HairMath.DEG_TO_RAD;
	private static final float NEUTRAL_HUE = 265.0f * HairMath.DEG_TO_RAD;
	private static final float SHADOW_DEPTH = 0.24f;
	private static final float SHADOW_FLOOR = 0.30f;
	private static final float SHADOW_CHROMA_GAIN = 0.30f;
	private static final float SHADOW_HUE_SHIFT = 22.0f * HairMath.DEG_TO_RAD;
	private static final float HIGHLIGHT_LIFT = 0.30f;
	private static final float HIGHLIGHT_DESATURATION = 0.30f;
	private static final float HIGHLIGHT_HUE_SHIFT = 12.0f * HairMath.DEG_TO_RAD;
	private static final float NEUTRAL_SHADOW_CHROMA = 0.028f;
	private static final float NEUTRAL_HIGHLIGHT_CHROMA = 0.032f;
	private static final float NEUTRAL_CHROMA_LOW = 0.012f;
	private static final float NEUTRAL_CHROMA_HIGH = 0.05f;
	private static final int GAMUT_ITERATIONS = 14;
	private static final float GAMUT_EPSILON = 1.0e-4f;
	private static final float TWO_PI = (float) (Math.PI * 2.0);

	private final int[] keys = new int[CACHE_SIZE];
	private final float[] colors = new float[CACHE_SIZE * STRIDE];
	private final float[] lab = new float[3];
	private final float[] linear = new float[3];

	public HairColorRamp() {
		Arrays.fill(keys, -1);
	}

	public int resolve(int rgb) {
		int color = rgb & 0xFFFFFF;
		int slot = (color * 0x9E3779B1 >>> 24) & (CACHE_SIZE - 1);
		int offset = slot * STRIDE;
		if (keys[slot] != color) {
			build(color, offset);
			keys[slot] = color;
		}
		return offset;
	}

	public void sample(int ramp, float shade, float[] out, int outOffset) {
		float position = (HairMath.clamp(shade, -1.0f, 1.0f) + 1.0f) * 0.5f * (STOPS - 1);
		int stop = Math.min(STOPS - 2, (int) position);
		float fraction = position - stop;
		int a = ramp + stop * 3;
		int b = a + 3;
		out[outOffset] = colors[a] + (colors[b] - colors[a]) * fraction;
		out[outOffset + 1] = colors[a + 1] + (colors[b + 1] - colors[a + 1]) * fraction;
		out[outOffset + 2] = colors[a + 2] + (colors[b + 2] - colors[a + 2]) * fraction;
	}

	private void build(int rgb, int offset) {
		float red = ((rgb >> 16) & 0xFF) / 255.0f;
		float green = ((rgb >> 8) & 0xFF) / 255.0f;
		float blue = (rgb & 0xFF) / 255.0f;
		toOklab(red, green, blue, lab);
		float lightness = lab[0];
		float chroma = (float) Math.sqrt(lab[1] * lab[1] + lab[2] * lab[2]);
		float hue = (float) Math.atan2(lab[2], lab[1]);
		float chromatic = smoothstep(NEUTRAL_CHROMA_LOW, NEUTRAL_CHROMA_HIGH, chroma);

		for (int stop = 0; stop < STOPS; stop++) {
			int index = offset + stop * 3;
			if (stop == BASE_STOP) {
				colors[index] = red;
				colors[index + 1] = green;
				colors[index + 2] = blue;
				continue;
			}
			float shade = -1.0f + 2.0f * stop / (STOPS - 1);
			shift(lightness, chroma, hue, lab[1], lab[2], chromatic, shade);
			writeSrgb(index);
		}
	}

	private void shift(float lightness, float chroma, float hue, float baseA, float baseB, float chromatic, float shade) {
		float amount = Math.abs(shade);
		float targetLightness;
		float targetChroma;
		float targetHue;
		float neutralChroma;
		float neutralHue;
		if (shade < 0.0f) {
			targetLightness = lightness - amount * SHADOW_DEPTH * (SHADOW_FLOOR + (1.0f - SHADOW_FLOOR) * lightness);
			targetChroma = chroma * (1.0f + amount * SHADOW_CHROMA_GAIN);
			targetHue = hue + towards(hue, COOL_HUE, SHADOW_HUE_SHIFT * amount);
			neutralChroma = NEUTRAL_SHADOW_CHROMA * amount * (0.4f + 0.6f * lightness);
			neutralHue = COOL_HUE;
		} else {
			targetLightness = lightness + amount * HIGHLIGHT_LIFT * (1.0f - lightness);
			targetChroma = chroma * (1.0f - amount * HIGHLIGHT_DESATURATION * lightness);
			targetHue = hue + towards(hue, WARM_HUE, HIGHLIGHT_HUE_SHIFT * amount);
			neutralChroma = NEUTRAL_HIGHLIGHT_CHROMA * amount * (1.0f - lightness);
			neutralHue = NEUTRAL_HUE;
		}
		float a = targetChroma * (float) Math.cos(targetHue) * chromatic
				+ (neutralChroma * (float) Math.cos(neutralHue) + baseA) * (1.0f - chromatic);
		float b = targetChroma * (float) Math.sin(targetHue) * chromatic
				+ (neutralChroma * (float) Math.sin(neutralHue) + baseB) * (1.0f - chromatic);
		fitToGamut(HairMath.clamp(targetLightness, 0.0f, 1.0f), a, b);
	}

	private void fitToGamut(float lightness, float a, float b) {
		toLinear(lightness, a, b, linear);
		if (inGamut(linear)) return;
		float low = 0.0f;
		float high = 1.0f;
		for (int i = 0; i < GAMUT_ITERATIONS; i++) {
			float middle = (low + high) * 0.5f;
			toLinear(lightness, a * middle, b * middle, linear);
			if (inGamut(linear)) low = middle;
			else high = middle;
		}
		toLinear(lightness, a * low, b * low, linear);
	}

	private void writeSrgb(int index) {
		colors[index] = toSrgb(linear[0]);
		colors[index + 1] = toSrgb(linear[1]);
		colors[index + 2] = toSrgb(linear[2]);
	}

	private static float towards(float from, float to, float maxStep) {
		float delta = (to - from) % TWO_PI;
		if (delta > Math.PI) delta -= TWO_PI;
		if (delta < -Math.PI) delta += TWO_PI;
		return Math.abs(delta) <= maxStep ? delta : Math.copySign(maxStep, delta);
	}

	private static boolean inGamut(float[] rgb) {
		for (float channel : rgb) {
			if (channel < -GAMUT_EPSILON || channel > 1.0f + GAMUT_EPSILON) return false;
		}
		return true;
	}

	private static void toOklab(float red, float green, float blue, float[] out) {
		float r = toLinear(red);
		float g = toLinear(green);
		float b = toLinear(blue);
		float l = (float) Math.cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b);
		float m = (float) Math.cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b);
		float s = (float) Math.cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b);
		out[0] = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s;
		out[1] = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s;
		out[2] = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s;
	}

	private static void toLinear(float lightness, float a, float b, float[] out) {
		float l = lightness + 0.3963377774f * a + 0.2158037573f * b;
		float m = lightness - 0.1055613458f * a - 0.0638541728f * b;
		float s = lightness - 0.0894841775f * a - 1.2914855480f * b;
		l = l * l * l;
		m = m * m * m;
		s = s * s * s;
		out[0] = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
		out[1] = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s;
		out[2] = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s;
	}

	private static float toLinear(float channel) {
		return channel <= 0.04045f ? channel / 12.92f : (float) Math.pow((channel + 0.055f) / 1.055f, 2.4);
	}

	private static float toSrgb(float channel) {
		float clamped = HairMath.clamp(channel, 0.0f, 1.0f);
		return clamped <= 0.0031308f ? clamped * 12.92f : 1.055f * (float) Math.pow(clamped, 1.0 / 2.4) - 0.055f;
	}

	private static float smoothstep(float edge0, float edge1, float value) {
		return HairMath.smoothstep((value - edge0) / (edge1 - edge0));
	}
}
