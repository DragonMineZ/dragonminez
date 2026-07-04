package com.dragonminez.client.gui.hud;

import net.minecraft.util.Mth;

public class HudBarAnimator {
	private static final float FRONT_TAU = 0.55f;
	private static final float GHOST_TAU = 0.32f;
	private static final float DAMAGE_HOLD_SECONDS = 1.5f;
	private static final float HEAL_HOLD_SECONDS = 0.25f;
	private static final float BURST_HEAL_THRESHOLD = 0.12f;
	private static final float DAMAGE_THRESHOLD = 0.01f;
	private static final float MIN_GAP = 0.0015f;
	private static final float MAX_FRAME_SECONDS = 0.25f;

	public enum GapType { NONE, DAMAGE, HEAL }

	private boolean initialized;
	private float target;
	private float front;
	private float ghost;
	private long lastNanos;
	private float holdTimer;
	private GapType gapType = GapType.NONE;

	public void reset(float fraction) {
		fraction = Mth.clamp(fraction, 0.0f, 1.0f);
		initialized = true;
		target = front = ghost = fraction;
		holdTimer = 0.0f;
		gapType = GapType.NONE;
		lastNanos = System.nanoTime();
	}

	public void update(float fraction) {
		fraction = Mth.clamp(fraction, 0.0f, 1.0f);
		if (!initialized) {
			reset(fraction);
			return;
		}

		long now = System.nanoTime();
		float dt = (now - lastNanos) / 1_000_000_000.0f;
		lastNanos = now;
		if (dt <= 0.0f) return;
		if (dt > MAX_FRAME_SECONDS) dt = MAX_FRAME_SECONDS;

		float previous = target;
		if (previous - fraction >= DAMAGE_THRESHOLD) {
			gapType = GapType.DAMAGE;
			ghost = Math.max(ghost, previous);
			front = fraction;
			holdTimer = DAMAGE_HOLD_SECONDS;
		} else if (fraction - previous >= BURST_HEAL_THRESHOLD) {
			gapType = GapType.HEAL;
			ghost = fraction;
			holdTimer = HEAL_HOLD_SECONDS;
		}
		target = fraction;

		if (holdTimer > 0.0f) holdTimer -= dt;

		float frontBlend = 1.0f - (float) Math.exp(-dt / FRONT_TAU);
		float ghostBlend = 1.0f - (float) Math.exp(-dt / GHOST_TAU);

		boolean frontFrozen = gapType == GapType.HEAL && holdTimer > 0.0f;
		if (!frontFrozen) front += (target - front) * frontBlend;

		switch (gapType) {
			case DAMAGE -> {
				if (holdTimer <= 0.0f) ghost += (target - ghost) * ghostBlend;
				if (ghost - front <= MIN_GAP) { ghost = front; gapType = GapType.NONE; }
			}
			case HEAL -> {
				ghost = target;
				if (front >= ghost - MIN_GAP) { front = ghost; gapType = GapType.NONE; }
			}
			default -> ghost = front;
		}
	}

	public float frontFraction() {
		return front;
	}

	public float ghostFraction() {
		return ghost;
	}

	public GapType gapType() {
		return gapType;
	}
}
