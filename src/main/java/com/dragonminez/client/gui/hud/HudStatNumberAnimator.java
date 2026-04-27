package com.dragonminez.client.gui.hud;

public class HudStatNumberAnimator {
	public static final int WHITE_RGB = 0xFFFFFF;

	private static final int INCREASE_RGB = 0x55FF55;
	private static final int DAMAGE_RGB = 0xFF5555;
	private static final float VISIBLE_HOLD_TICKS = 50.0f;
	private static final float FADE_OUT_TICKS = 20.0f;
	private static final float FADE_IN_TICKS = 5.0f;
	private static final float PULSE_TICKS = 14.0f;
	private static final float MAX_ELAPSED_TICKS = 5.0f;
	private static final float CHANGE_EPSILON = 0.001f;
	private static final float MIN_CHANGE_ALPHA = 0.35f;

	private final StatKind statKind;
	private boolean initialized;
	private String lastText = "";
	private float lastValue;
	private float lastChangeTick;
	private float lastUpdateTick;
	private float pulseStartTick = -PULSE_TICKS;
	private ChangeDirection pulseDirection = ChangeDirection.NONE;
	private float alpha = 1.0f;

	public HudStatNumberAnimator(StatKind statKind) {
		this.statKind = statKind;
	}

	public RenderState update(String text, float value, float tickTime) {
		if (!initialized) {
			initialized = true;
			lastText = text;
			lastValue = value;
			lastChangeTick = tickTime;
			lastUpdateTick = tickTime;
			alpha = 1.0f;
			return renderState(tickTime);
		}

		if (tickTime < lastUpdateTick) {
			lastChangeTick = tickTime;
			lastUpdateTick = tickTime;
			pulseDirection = ChangeDirection.NONE;
			alpha = 1.0f;
		}

		float elapsed = clamp(tickTime - lastUpdateTick, 0.0f, MAX_ELAPSED_TICKS);
		lastUpdateTick = Math.max(lastUpdateTick, tickTime);

		ChangeDirection direction = getChangeDirection(text, value);
		if (direction != ChangeDirection.NONE || !text.equals(lastText)) {
			lastText = text;
			lastValue = value;
			lastChangeTick = tickTime;
			alpha = Math.max(alpha, MIN_CHANGE_ALPHA);

			if (direction == ChangeDirection.UP || shouldPulseDamage(direction)) {
				pulseDirection = direction;
				pulseStartTick = tickTime;
			} else {
				pulseDirection = ChangeDirection.NONE;
			}
		}

		float targetAlpha = tickTime - lastChangeTick <= VISIBLE_HOLD_TICKS ? 1.0f : 0.0f;
		float rate = targetAlpha > alpha ? elapsed / FADE_IN_TICKS : elapsed / FADE_OUT_TICKS;
		alpha = approach(alpha, targetAlpha, rate);

		return renderState(tickTime);
	}

	private boolean shouldPulseDamage(ChangeDirection direction) {
		return statKind == StatKind.HEALTH && direction == ChangeDirection.DOWN;
	}

	private ChangeDirection getChangeDirection(String text, float value) {
		if (Math.abs(value - lastValue) <= CHANGE_EPSILON) return ChangeDirection.NONE;
		return value > lastValue ? ChangeDirection.UP : ChangeDirection.DOWN;
	}

	private RenderState renderState(float tickTime) {
		float pulseAge = tickTime - pulseStartTick;
		float pulse = pulseAge >= 0.0f && pulseAge <= PULSE_TICKS ? 1.0f - smoothStep(pulseAge / PULSE_TICKS) : 0.0f;
		int rgbColor = pulseColor(pulse);
		float offsetX = damageShakeOffsetX(pulseAge, pulse);
		float offsetY = damageShakeOffsetY(pulseAge, pulse);
		return new RenderState(clamp(alpha, 0.0f, 1.0f), rgbColor, offsetX, offsetY);
	}

	private int pulseColor(float pulse) {
		if (pulse <= 0.0f) return WHITE_RGB;
		if (pulseDirection == ChangeDirection.UP) return lerpColor(WHITE_RGB, INCREASE_RGB, pulse);
		if (shouldPulseDamage(pulseDirection)) return lerpColor(WHITE_RGB, DAMAGE_RGB, pulse);
		return WHITE_RGB;
	}

	private float damageShakeOffsetX(float pulseAge, float pulse) {
		if (!shouldPulseDamage(pulseDirection) || pulse <= 0.0f) return 0.0f;
		return (float) Math.sin(pulseAge * 3.2f) * 1.25f * pulse;
	}

	private float damageShakeOffsetY(float pulseAge, float pulse) {
		if (!shouldPulseDamage(pulseDirection) || pulse <= 0.0f) return 0.0f;
		return (float) Math.cos(pulseAge * 4.1f) * 0.45f * pulse;
	}

	private static float approach(float current, float target, float amount) {
		if (current < target) return Math.min(target, current + amount);
		if (current > target) return Math.max(target, current - amount);
		return current;
	}

	private static float smoothStep(float value) {
		float clamped = clamp(value, 0.0f, 1.0f);
		return clamped * clamped * (3.0f - 2.0f * clamped);
	}

	private static int lerpColor(int from, int to, float amount) {
		float clamped = clamp(amount, 0.0f, 1.0f);
		int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, clamped);
		int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, clamped);
		int b = lerpChannel(from & 0xFF, to & 0xFF, clamped);
		return (r << 16) | (g << 8) | b;
	}

	private static int lerpChannel(int from, int to, float amount) {
		return Math.round(from + (to - from) * amount);
	}

	private static float clamp(float value, float min, float max) {
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}

	public enum StatKind {
		HEALTH,
		KI,
		STAMINA
	}

	private enum ChangeDirection {
		NONE,
		UP,
		DOWN
	}

	public record RenderState(float alpha, int rgbColor, float offsetX, float offsetY) {
		public boolean isHidden() {
			return alpha <= 0.01f;
		}
	}
}
