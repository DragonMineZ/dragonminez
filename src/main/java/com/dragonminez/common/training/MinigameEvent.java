package com.dragonminez.common.training;

public final class MinigameEvent {
	public static final int KEY_DOWN = 0;
	public static final int KEY_UP = 1;
	public static final int CLICK = 2;

	public static final int LEFT = 0;
	public static final int DOWN = 1;
	public static final int UP = 2;
	public static final int RIGHT = 3;
	public static final int ACTION = 4;
	public static final int CHAR_BASE = 5;
	public static final int CHAR_COUNT = 36;
	public static final int KEY_COUNT = CHAR_BASE + CHAR_COUNT;

	public static final int FRAC_STEPS = 256;
	public static final int POS_STEPS = 4;

	public final int tick;
	public final int type;
	public final int key;
	public final int frac;
	public final int x;
	public final int y;

	public MinigameEvent(int tick, int type, int key, int frac, int x, int y) {
		this.tick = tick;
		this.type = type;
		this.key = key;
		this.frac = Math.max(0, Math.min(FRAC_STEPS - 1, frac));
		this.x = x;
		this.y = y;
	}

	public static MinigameEvent key(int tick, boolean down, int key, float partialTick) {
		return new MinigameEvent(tick, down ? KEY_DOWN : KEY_UP, key, quantizeFrac(partialTick), 0, 0);
	}

	public static MinigameEvent click(int tick, double fieldX, double fieldY, float partialTick) {
		return new MinigameEvent(tick, CLICK, 0, quantizeFrac(partialTick),
				(int) Math.round(fieldX * POS_STEPS), (int) Math.round(fieldY * POS_STEPS));
	}

	public static int quantizeFrac(float partialTick) {
		return (int) Math.floor(Math.max(0f, Math.min(0.999f, partialTick)) * FRAC_STEPS);
	}

	public float fraction() {
		return frac / (float) FRAC_STEPS;
	}

	public float fieldX() {
		return x / (float) POS_STEPS;
	}

	public float fieldY() {
		return y / (float) POS_STEPS;
	}

	public boolean isValid() {
		if (tick < 0) return false;
		return switch (type) {
			case KEY_DOWN, KEY_UP -> key >= 0 && key < KEY_COUNT;
			case CLICK -> x >= Short.MIN_VALUE && x <= Short.MAX_VALUE && y >= Short.MIN_VALUE && y <= Short.MAX_VALUE;
			default -> false;
		};
	}
}
