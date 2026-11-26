package com.dragonminez.client.gui.character.pixel;

import java.util.List;

public final class SkinAtlas {
	public record Region(int x, int y, int width, int height) {
		public boolean contains(int px, int py) {
			return px >= x && py >= y && px < x + width && py < y + height;
		}
	}

	public static final List<Region> BASE = List.of(
			new Region(0, 0, 32, 16),
			new Region(16, 16, 24, 16),
			new Region(40, 16, 16, 16),
			new Region(0, 16, 16, 16),
			new Region(16, 48, 16, 16),
			new Region(32, 48, 16, 16)
	);

	public static final List<Region> OVERLAY = List.of(
			new Region(32, 0, 32, 16),
			new Region(16, 32, 24, 16),
			new Region(40, 32, 16, 16),
			new Region(0, 32, 16, 16),
			new Region(0, 48, 16, 16),
			new Region(48, 48, 16, 16)
	);

	public static final Region HEAD = BASE.get(0);

	private SkinAtlas() {}

	public static boolean isBase(int x, int y) {
		return contains(BASE, x, y);
	}

	public static boolean isOverlay(int x, int y) {
		return contains(OVERLAY, x, y);
	}

	private static boolean contains(List<Region> regions, int x, int y) {
		for (Region region : regions) if (region.contains(x, y)) return true;
		return false;
	}
}
