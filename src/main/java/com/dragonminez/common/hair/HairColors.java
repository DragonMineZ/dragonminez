package com.dragonminez.common.hair;

import java.util.Locale;

public final class HairColors {
	public static final int INHERIT = -1;

	private HairColors() {}

	public static int parse(String hex) {
		if (hex == null) return INHERIT;
		String value = hex.trim();
		if (value.startsWith("#")) value = value.substring(1);
		if (value.length() != 6) return INHERIT;
		try {
			return Integer.parseInt(value, 16) & 0xFFFFFF;
		} catch (NumberFormatException exception) {
			return INHERIT;
		}
	}

	public static String normalize(String hex) {
		int rgb = parse(hex);
		return rgb == INHERIT ? null : format(rgb);
	}

	public static String format(int rgb) {
		return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
	}

	public static int lerp(int from, int to, float factor) {
		if (factor <= 0.0f) return from;
		if (factor >= 1.0f) return to;
		int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * factor);
		int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * factor);
		int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * factor);
		return (r << 16) | (g << 8) | b;
	}
}
