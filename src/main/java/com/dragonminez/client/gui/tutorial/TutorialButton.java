package com.dragonminez.client.gui.tutorial;

public record TutorialButton(String labelKey, Icon icon, Runnable action, Behavior behavior) {
	public enum Behavior { STAY, NEXT, FINISH }

	public enum Icon {
		NONE(0, 0, 0, 0),
		DECREASE(142, 0, 10, 10),
		INCREASE(0, 0, 10, 10);

		public final int u, v, width, height;

		Icon(int u, int v, int width, int height) {
			this.u = u;
			this.v = v;
			this.width = width;
			this.height = height;
		}
	}

	public static TutorialButton text(String labelKey, Behavior behavior, Runnable action) {
		return new TutorialButton(labelKey, Icon.NONE, action, behavior);
	}

	public static TutorialButton icon(Icon icon, Runnable action) {
		return new TutorialButton(null, icon, action, Behavior.STAY);
	}

	public boolean isIcon() {
		return icon != Icon.NONE;
	}

	public float width() {
		return isIcon() ? 14.0f : 74.0f;
	}
}
