package com.dragonminez.client.gui.hud;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

	private final List<Segment> segments = new ArrayList<>();
	private boolean initialized;
	private float target;
	private float front;
	private float healGhost;
	private float healHold;
	private boolean healing;
	private long lastNanos;

	public void reset(float fraction) {
		fraction = Mth.clamp(fraction, 0.0f, 1.0f);
		initialized = true;
		target = front = fraction;
		healGhost = fraction;
		healHold = 0.0f;
		healing = false;
		segments.clear();
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
			segments.add(new Segment(previous - fraction));
			front = fraction;
			healing = false;
		} else if (fraction - previous >= BURST_HEAL_THRESHOLD) {
			segments.clear();
			healing = true;
			healGhost = fraction;
			healHold = HEAL_HOLD_SECONDS;
		}
		target = fraction;

		float frontBlend = 1.0f - (float) Math.exp(-dt / FRONT_TAU);
		float ghostBlend = 1.0f - (float) Math.exp(-dt / GHOST_TAU);

		float previousFront = front;
		boolean frontFrozen = healing && healHold > 0.0f;
		if (!frontFrozen) front += (target - front) * frontBlend;

		if (healing) {
			if (healHold > 0.0f) healHold -= dt;
			healGhost = target;
			if (front >= healGhost - MIN_GAP) {
				front = healGhost;
				healing = false;
			}
		} else if (!segments.isEmpty()) {
			float frontRise = front - previousFront;
			if (frontRise > 0.0f) consumeFromBottom(frontRise);
			for (Iterator<Segment> it = segments.iterator(); it.hasNext(); ) {
				Segment segment = it.next();
				segment.age += dt;
				if (segment.age >= DAMAGE_HOLD_SECONDS) {
					segment.amount -= segment.amount * ghostBlend;
					if (segment.amount <= MIN_GAP) it.remove();
				}
			}
		}
	}

	private void consumeFromBottom(float amount) {
		for (int i = segments.size() - 1; i >= 0 && amount > 0.0f; i--) {
			Segment segment = segments.get(i);
			float take = Math.min(segment.amount, amount);
			segment.amount -= take;
			amount -= take;
			if (segment.amount <= MIN_GAP) segments.remove(i);
		}
	}

	public float frontFraction() {
		return front;
	}

	public float ghostFraction() {
		if (healing) return healGhost;
		float total = 0.0f;
		for (Segment segment : segments) total += segment.amount;
		return Mth.clamp(front + total, 0.0f, 1.0f);
	}

	public GapType gapType() {
		if (healing) return GapType.HEAL;
		return segments.isEmpty() ? GapType.NONE : GapType.DAMAGE;
	}

	private static final class Segment {
		private float amount;
		private float age;

		private Segment(float amount) {
			this.amount = amount;
		}
	}
}
