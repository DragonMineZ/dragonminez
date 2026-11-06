package com.dragonminez.client.gui.tutorial;

import java.util.List;

public record TutorialRect(float x, float y, float width, float height) {
	public float right() {
		return x + width;
	}

	public float bottom() {
		return y + height;
	}

	public float centerX() {
		return x + width / 2.0f;
	}

	public float centerY() {
		return y + height / 2.0f;
	}

	public boolean contains(double px, double py) {
		return px >= x && px < right() && py >= y && py < bottom();
	}

	public TutorialRect inflate(float amount) {
		return new TutorialRect(x - amount, y - amount, width + amount * 2.0f, height + amount * 2.0f);
	}

	public static TutorialRect of(float x, float y, float width, float height) {
		return new TutorialRect(x, y, width, height);
	}

	public static TutorialRect corners(float left, float top, float right, float bottom) {
		return new TutorialRect(left, top, right - left, bottom - top);
	}

	public static TutorialRect union(List<TutorialRect> rects) {
		if (rects == null || rects.isEmpty()) return null;
		float left = Float.MAX_VALUE, top = Float.MAX_VALUE, right = -Float.MAX_VALUE, bottom = -Float.MAX_VALUE;
		for (TutorialRect rect : rects) {
			left = Math.min(left, rect.x());
			top = Math.min(top, rect.y());
			right = Math.max(right, rect.right());
			bottom = Math.max(bottom, rect.bottom());
		}
		return corners(left, top, right, bottom);
	}
}
