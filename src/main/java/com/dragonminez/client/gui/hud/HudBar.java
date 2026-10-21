package com.dragonminez.client.gui.hud;

import com.dragonminez.common.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.util.Mth;

import java.text.NumberFormat;
import java.util.Locale;

public final class HudBar {
	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
	private static final int DAMAGE_CHIP = 0xFF4646;
	private static final int SPENT_CHIP = 0xE6E9F2;
	private static final int HEAL_CHIP = 0x57FF6B;
	private static final int SOFT_LOSS_CHIP = 0xE4E8FF;
	private static final float SOFT_LOSS_ALPHA = 0.74f;
	private static final float SOFT_SPENT_ALPHA = 0.55f;
	private static final float SOFT_HEAL_ALPHA = 0.4f;
	private static final float IMPACT_THRESHOLD = 0.01f;
	private static final float GLINT_WIDTH = 6.0f;
	private static final int PROFILE_SEGMENTS = 8;
	private static final float GLINT_ALPHA = 0.6f;
	private static final float SWEEP_HALF_WIDTH = 8.0f;
	private static final float SWEEP_ALPHA = 0.45f;
	private static final float SHINE_PERIOD = 5.5f;
	private static final float SHINE_DURATION = 0.9f;
	private static final float SHINE_HALO = 16.0f;
	private static final float SHINE_HALO_ALPHA = 0.2f;
	private static final float SHINE_CORE = 4.5f;
	private static final float SHINE_CORE_ALPHA = 0.28f;
	private static final float SHINE_CASCADE = 0.16f;
	private static final float FLOW_WAVE_A = 38.0f;
	private static final float FLOW_SPEED_A = 9.0f;
	private static final float FLOW_WAVE_B = 23.0f;
	private static final float FLOW_SPEED_B = 14.0f;
	private static final float FLOW_ALPHA = 0.2f;
	private static final float FLOW_BOOST_ALPHA = 0.6f;
	private static final float FLOW_BOOST_SPEED = 0.5f;
	private static final float FLOW_BOTTOM = 0.35f;
	private static final float FLOW_STEP = 3.5f;
	private static final float FLOW_EDGE_FADE = 6.0f;
	private static final float MAX_CLOCK_STEP = 0.25f;
	private static double hudTime;
	private static long lastClockNanos;
	private static final float BEAT_THRESHOLD = 0.25f;
	private static final float BEAT_FADE = 0.04f;
	private static final float BEAT_TRAVEL = 0.4f;
	private static final float BEAT_HALO = 9.0f;
	private static final float BEAT_HALO_ALPHA = 0.22f;
	private static final float BEAT_CORE = 2.5f;
	private static final float BEAT_CORE_ALPHA = 0.45f;

	private final HudBarAnimator animator = new HudBarAnimator();
	private final HudSmoother number = new HudSmoother(0.11f, 0.45f);
	private final HudSmoother motion = new HudSmoother(0.18f);
	private final HudSmoother impact = new HudSmoother(0.14f);
	private final HudStatNumberAnimator numberAnimator;
	private final int lossChipRgb;
	private final boolean health;
	private float shineOffset;
	private boolean flowing;
	private float flowBoost;
	private float flowPhaseA;
	private float flowPhaseB;
	private float lastFlowTime = -1.0f;
	private float lastMax = -1.0f;
	private float current;
	private float max = 1.0f;
	private float lastFront = -1.0f;
	private float lastTarget = -1.0f;
	private boolean mirrored;

	public HudBar(HudStatNumberAnimator.StatKind kind) {
		this.numberAnimator = new HudStatNumberAnimator(kind);
		this.health = kind == HudStatNumberAnimator.StatKind.HEALTH || kind == HudStatNumberAnimator.StatKind.KISENSE_HEALTH;
		this.lossChipRgb = health ? DAMAGE_CHIP : SPENT_CHIP;
		this.shineOffset = switch (kind) {
			case KI -> SHINE_CASCADE;
			case STAMINA -> SHINE_CASCADE * 2.0f;
			default -> 0.0f;
		};
	}

	public HudBar flowing() {
		this.flowing = true;
		return this;
	}

	public HudBar shineOffset(float seconds) {
		this.shineOffset = seconds;
		return this;
	}

	public void setFlowBoost(float boost) {
		this.flowBoost = Mth.clamp(boost, 0.0f, 1.0f);
	}

	public static float heartbeat(float fraction) {
		if (fraction >= BEAT_THRESHOLD) return 0.0f;
		float strength = Mth.clamp((BEAT_THRESHOLD - fraction) / BEAT_FADE, 0.0f, 1.0f);
		float t = clock() % beatPeriod(fraction);
		float beat = gauss(t, 0.07f, 0.05f) + 0.65f * gauss(t, 0.27f, 0.055f);
		return Mth.clamp(beat, 0.0f, 1.0f) * strength;
	}

	public void update(float current, float max) {
		if (paused() && lastMax > 0.0f) return;
		this.max = Math.max(1.0f, max);
		this.current = Mth.clamp(current, 0.0f, this.max);
		float fraction = this.current / this.max;

		if (lastMax != this.max) {
			animator.reset(fraction);
			number.snap(this.current);
			lastMax = this.max;
			lastTarget = fraction;
		}
		if (lastTarget >= 0.0f && lastTarget - fraction >= IMPACT_THRESHOLD) {
			impact.snap(Math.min(1.0f, impact.value() + 0.55f + (lastTarget - fraction) * 3.0f));
		}
		lastTarget = fraction;
		impact.update(0.0f);
		animator.update(fraction);
		number.update(this.current);

		float front = animator.frontFraction();
		float moving = lastFront >= 0.0f && Math.abs(front - lastFront) > 0.0004f ? 1.0f : 0.0f;
		lastFront = front;
		motion.update(moving);
	}

	public void setMirrored(boolean mirrored) {
		this.mirrored = mirrored;
	}

	public float fraction() {
		return animator.frontFraction();
	}

	public float targetFraction() {
		return current / max;
	}

	public void draw(GuiGraphics graphics, HudSprites.BarSkin skin, float x, float y, float width, float height, int fillRgb, float pulse) {
		float front = animator.frontFraction();
		float ghost = animator.ghostFraction();
		drawFrame(graphics, skin, x, y, width, height, pulse, mirrored);

		HudBarAnimator.GapType gap = animator.gapType();
		if (gap != HudBarAnimator.GapType.NONE) {
			int chipRgb = gap == HudBarAnimator.GapType.DAMAGE ? lossChipRgb : HEAL_CHIP;
			float chipAlpha = gap == HudBarAnimator.GapType.DAMAGE ? (lossChipRgb == DAMAGE_CHIP ? 0.95f : 0.6f) : 0.55f;
			drawFill(graphics, skin, x, y, width, height, Math.min(front, ghost), 0.0f, Math.max(front, ghost), 1.0f, chipRgb, chipAlpha, mirrored);
		}

		if (front > 0.0f) {
			drawFill(graphics, skin, x, y, width, height, 0.0f, 0.0f, front, 1.0f, HudRender.mix(fillRgb, 0xFFFFFF, pulse * 0.25f), 1.0f, mirrored);
			drawLife(graphics, skin.fill(), x, y, width, height, front * width, false);

			drawGlint(graphics, skin.fill(), x, y, width, height, front);
		}
	}

	private void drawGlint(GuiGraphics graphics, HudSprites.Sprite fill, float x, float y, float width, float height, float front) {
		float glint = motion.value();
		float frontWidth = front * width;
		if (glint <= 0.02f || frontWidth >= width - 0.5f) return;
		float span = Math.min(frontWidth, GLINT_WIDTH) / width;
		HudRender.setAdditive(true);
		try {
			HudRender.spriteGradient(graphics, fill, x, y, width, height, front - span, front, 0.0f, GLINT_ALPHA * glint, 0xFFFFFF, mirrored, false);
		} finally {
			HudRender.setAdditive(false);
		}
	}

	public void drawFill(GuiGraphics graphics, HudSprites.Sprite fill, HudSprites.Sprite shine, float x, float y, float width, float height, int fillRgb, float pulse) {
		float front = animator.frontFraction();
		float ghost = animator.ghostFraction();
		float hit = impact.value();

		HudBarAnimator.GapType gap = animator.gapType();
		if (gap != HudBarAnimator.GapType.NONE) {
			float from = Math.min(front, ghost);
			float to = Math.max(front, ghost);
			if (gap == HudBarAnimator.GapType.DAMAGE) {
				float baseAlpha = health ? SOFT_LOSS_ALPHA : SOFT_SPENT_ALPHA;
				int chip = HudRender.mix(HudRender.mix(SOFT_LOSS_CHIP, fillRgb, 0.18f), 0xFFFFFF, hit);
				drawLayer(graphics, fill, shine, x, y, width, height, from, to, chip, baseAlpha + (1.0f - baseAlpha) * hit);
			} else {
				float breath = 0.5f + 0.5f * Mth.sin(clock() * 5.0f);
				drawLayer(graphics, fill, shine, x, y, width, height, from, to, HudRender.mix(fillRgb, 0xFFFFFF, 0.5f), SOFT_HEAL_ALPHA + 0.12f * breath);
			}
		}

		if (front <= 0.0f) return;
		drawLayer(graphics, fill, shine, x, y, width, height, 0.0f, front, HudRender.mix(fillRgb, 0xFFFFFF, pulse * 0.25f), 1.0f);
		drawLife(graphics, fill, x, y, width, height, front * width, false);

		if (health && hit > 0.02f) {
			HudRender.setAdditive(true);
			try {
				HudRender.spritePart(graphics, fill, x, y, width, height, 0.0f, 0.0f, front, 1.0f, 0xFFFFFF, hit * 0.22f, mirrored);
			} finally {
				HudRender.setAdditive(false);
			}
		}
		drawGlint(graphics, fill, x, y, width, height, front);
	}

	public void drawFillSweep(GuiGraphics graphics, HudSprites.Sprite fill, float x, float y, float width, float height, float phase, float strength) {
		float frontWidth = animator.frontFraction() * width;
		if (frontWidth <= 2.0f || strength <= 0.01f) return;
		float center = (phase % 1.0f) * (frontWidth + SWEEP_HALF_WIDTH * 2.0f) - SWEEP_HALF_WIDTH;
		HudRender.setAdditive(true);
		try {
			band(graphics, fill, x, y, width, height, center, SWEEP_HALF_WIDTH, frontWidth, SWEEP_ALPHA * strength, mirrored, false);
		} finally {
			HudRender.setAdditive(false);
		}
	}

	private void drawLayer(GuiGraphics graphics, HudSprites.Sprite fill, HudSprites.Sprite shine, float x, float y, float width, float height,
						   float from, float to, int rgb, float alpha) {
		HudRender.spritePart(graphics, fill, x, y, width, height, from, 0.0f, to, 1.0f, rgb, alpha, mirrored);
		if (shine != null) HudRender.spritePart(graphics, shine, x, y, width, height, from, 0.0f, to, 1.0f, 0xFFFFFF, alpha, mirrored);
	}

	public void drawSweep(GuiGraphics graphics, float x, float y, float width, float height, float phase, float strength) {
		float front = animator.frontFraction() * width;
		if (front <= 2.0f || strength <= 0.01f) return;
		float band = Math.min(14.0f, front);
		float start = (phase % 1.0f) * (front + band) - band;
		float from = Math.max(0.0f, start);
		float to = Math.min(front, start + band);
		if (to <= from) return;
		if (mirrored) {
			float mirroredFrom = width - to;
			to = width - from;
			from = mirroredFrom;
		}
		float mid = (from + to) / 2.0f;
		HudRender.rectHorizontal(graphics, x + from, y, mid - from, height, HudRender.argb(0.0f, 0xFFFFFF), HudRender.argb(0.45f * strength, 0xFFFFFF));
		HudRender.rectHorizontal(graphics, x + mid, y, to - mid, height, HudRender.argb(0.45f * strength, 0xFFFFFF), HudRender.argb(0.0f, 0xFFFFFF));
	}

	public void drawVertical(GuiGraphics graphics, HudSprites.BarSkin skin, float x, float y, float width, float height, int fillRgb, float pulse) {
		float front = animator.frontFraction();
		float ghost = animator.ghostFraction();
		drawFrame(graphics, skin, x, y, width, height, 0.0f, false);

		HudBarAnimator.GapType gap = animator.gapType();
		if (gap != HudBarAnimator.GapType.NONE) {
			int chipRgb = gap == HudBarAnimator.GapType.DAMAGE ? lossChipRgb : HEAL_CHIP;
			drawFill(graphics, skin, x, y, width, height, 0.0f, 1.0f - Math.max(front, ghost), 1.0f, 1.0f - Math.min(front, ghost), chipRgb, 0.6f, false);
		}

		if (front > 0.0f) {
			drawFill(graphics, skin, x, y, width, height, 0.0f, 1.0f - front, 1.0f, 1.0f, HudRender.mix(fillRgb, 0xFFFFFF, pulse * 0.35f), 1.0f, false);
			drawLife(graphics, skin.fill(), x, y, width, height, front * height, true);
			HudRender.rect(graphics, x, y + height * (1.0f - front), width, Math.min(1.0f, front * height), HudRender.argb(0.7f, 0xFFFFFF));
		}
	}

	public static void drawStatic(GuiGraphics graphics, HudSprites.BarSkin skin, float x, float y, float width, float height, float fraction, int fillRgb, boolean mirrored) {
		float front = Mth.clamp(fraction, 0.0f, 1.0f);
		drawFrame(graphics, skin, x, y, width, height, 0.0f, mirrored);
		drawFill(graphics, skin, x, y, width, height, 0.0f, 0.0f, front, 1.0f, fillRgb, 1.0f, mirrored);
		float shine = shineProgress(clock(), SHINE_CASCADE * 3.0f);
		float frontLength = front * width;
		if (shine < 0.0f || frontLength <= 1.0f) return;
		HudRender.setAdditive(true);
		try {
			drawShine(graphics, skin.fill(), x, y, width, height, shine, frontLength, mirrored, false);
		} finally {
			HudRender.setAdditive(false);
		}
	}

	private static void drawShine(GuiGraphics graphics, HudSprites.Sprite fill, float x, float y, float width, float height,
								  float progress, float frontLength, boolean flip, boolean vertical) {
		float center = -SHINE_HALO + progress * (frontLength + SHINE_HALO * 2.0f);
		band(graphics, fill, x, y, width, height, center, SHINE_HALO, frontLength, SHINE_HALO_ALPHA, flip, vertical);
		band(graphics, fill, x, y, width, height, center, SHINE_CORE, frontLength, SHINE_CORE_ALPHA, flip, vertical);
	}

	private void drawLife(GuiGraphics graphics, HudSprites.Sprite fill, float x, float y, float width, float height, float frontLength, boolean vertical) {
		if (frontLength <= 1.0f) return;
		float now = clock();
		HudRender.setAdditive(true);
		try {
			float shine = shineProgress(now, shineOffset);
			if (shine >= 0.0f) drawShine(graphics, fill, x, y, width, height, shine, frontLength, mirrored, vertical);
			if (flowing) drawFlow(graphics, fill, x, y, width, height, frontLength, vertical, now);
			float fraction = current / max;
			if (health && fraction < BEAT_THRESHOLD) {
				float t = now % beatPeriod(fraction);
				if (t < BEAT_TRAVEL) {
					float progress = t / BEAT_TRAVEL;
					float strength = Mth.clamp((BEAT_THRESHOLD - fraction) / BEAT_FADE, 0.0f, 1.0f) * (1.0f - progress * progress);
					float center = progress * frontLength;
					band(graphics, fill, x, y, width, height, center, BEAT_HALO, frontLength, BEAT_HALO_ALPHA * strength, mirrored, vertical);
					band(graphics, fill, x, y, width, height, center, BEAT_CORE, frontLength, BEAT_CORE_ALPHA * strength, mirrored, vertical);
				}
			}
		} finally {
			HudRender.setAdditive(false);
		}
	}

	private void drawFlow(GuiGraphics graphics, HudSprites.Sprite fill, float x, float y, float width, float height,
						  float frontLength, boolean vertical, float now) {
		float dt = lastFlowTime < 0.0f ? 0.0f : now - lastFlowTime;
		lastFlowTime = now;
		if (dt < 0.0f || dt > MAX_CLOCK_STEP) dt = 0.0f;
		float speed = 1.0f + FLOW_BOOST_SPEED * flowBoost;
		flowPhaseA = (flowPhaseA + dt * FLOW_SPEED_A * speed) % FLOW_WAVE_A;
		flowPhaseB = (flowPhaseB + dt * FLOW_SPEED_B * speed) % FLOW_WAVE_B;
		float alpha = FLOW_ALPHA * (1.0f + FLOW_BOOST_ALPHA * flowBoost);
		float length = vertical ? height : width;
		for (float from = 0.0f; from < frontLength; from += FLOW_STEP) {
			float to = Math.min(frontLength, from + FLOW_STEP);
			float alphaFrom = alpha * flowShape(from, frontLength);
			float alphaTo = alpha * flowShape(to, frontLength);
			HudRender.spriteGradient(graphics, fill, x, y, width, height, from / length, to / length,
					alphaFrom, alphaTo, alphaFrom * FLOW_BOTTOM, alphaTo * FLOW_BOTTOM, 0xFFFFFF, mirrored, vertical);
		}
	}

	private float flowShape(float position, float frontLength) {
		float a = 0.5f + 0.5f * Mth.sin((position - flowPhaseA) / FLOW_WAVE_A * Mth.TWO_PI);
		float b = 0.5f + 0.5f * Mth.sin((position - flowPhaseB) / FLOW_WAVE_B * Mth.TWO_PI);
		float edge = Mth.clamp((frontLength - position) / FLOW_EDGE_FADE, 0.0f, 1.0f);
		return (0.62f * a * a * a + 0.38f * b * b * b) * edge;
	}

	private static void band(GuiGraphics graphics, HudSprites.Sprite fill, float x, float y, float width, float height,
							 float center, float halfWidth, float limit, float alpha, boolean flip, boolean vertical) {
		if (alpha <= 0.004f || halfWidth <= 0.0f) return;
		float length = vertical ? height : width;
		for (int i = 0; i < PROFILE_SEGMENTS; i++) {
			float start = center - halfWidth + halfWidth * 2.0f * i / PROFILE_SEGMENTS;
			float end = start + halfWidth * 2.0f / PROFILE_SEGMENTS;
			float from = Math.max(0.0f, start);
			float to = Math.min(limit, end);
			if (to <= from) continue;
			float alphaFrom = alpha * profile((from - center) / halfWidth);
			float alphaTo = alpha * profile((to - center) / halfWidth);
			HudRender.spriteGradient(graphics, fill, x, y, width, height, from / length, to / length, alphaFrom, alphaTo, 0xFFFFFF, flip, vertical);
		}
	}

	private static float profile(float offset) {
		float q = 1.0f - offset * offset;
		return q <= 0.0f ? 0.0f : q * q;
	}

	private static float shineProgress(float now, float offset) {
		float t = ((now - offset) % SHINE_PERIOD + SHINE_PERIOD) % SHINE_PERIOD;
		if (t >= SHINE_DURATION) return -1.0f;
		float progress = t / SHINE_DURATION;
		return progress * progress * (3.0f - 2.0f * progress);
	}

	private static float beatPeriod(float fraction) {
		float danger = Mth.clamp(1.0f - fraction / BEAT_THRESHOLD, 0.0f, 1.0f);
		return 1.05f - 0.4f * danger;
	}

	private static float gauss(float value, float center, float width) {
		float d = (value - center) / width;
		return (float) Math.exp(-d * d);
	}

	public static float time() {
		return clock();
	}

	private static float clock() {
		long now = System.nanoTime();
		float dt = lastClockNanos == 0L ? 0.0f : (now - lastClockNanos) / 1.0E9f;
		lastClockNanos = now;
		if (dt > 0.0f && dt <= MAX_CLOCK_STEP && !paused()) hudTime += dt;
		return (float) (hudTime % 3600.0);
	}

	private static boolean paused() {
		Minecraft mc = Minecraft.getInstance();
		return mc.isPaused() || mc.screen instanceof PauseScreen;
	}

	private static void drawFrame(GuiGraphics graphics, HudSprites.BarSkin skin, float x, float y, float width, float height, float alert, boolean flip) {
		float border = skin.border();
		HudRender.sprite(graphics, skin.frame(), x - border, y - border, width + border * 2.0f, height + border * 2.0f, 0xFFFFFF, 1.0f, flip);
		if (skin.alert() != null && alert > 0.0f) {
			HudRender.sprite(graphics, skin.alert(), x - border, y - border, width + border * 2.0f, height + border * 2.0f, 0xFFFFFF, alert * 0.75f);
		}
	}

	private static void drawFill(GuiGraphics graphics, HudSprites.BarSkin skin, float x, float y, float width, float height,
								 float fromX, float fromY, float toX, float toY, int rgb, float alpha, boolean flip) {
		HudRender.spritePart(graphics, skin.fill(), x, y, width, height, fromX, fromY, toX, toY, rgb, alpha, flip);
		HudRender.spritePart(graphics, skin.shine(), x, y, width, height, fromX, fromY, toX, toY, 0xFFFFFF, alpha, flip);
	}

	public void drawValue(GuiGraphics graphics, float x, float y, float scale, float align, float tickTime) {
		drawValue(graphics, x, y, scale, align, tickTime, false);
	}

	public void drawValue(GuiGraphics graphics, float x, float y, float scale, float align, float tickTime, boolean always) {
		if (!always && !ConfigManager.getUserConfig().getAdvancedDescription()) return;
		boolean percent = ConfigManager.getUserConfig().getAdvancedDescriptionPercentage();
		float shown = number.value();
		String text = percent
				? Math.round(shown / max * 100.0f) + "%"
				: NUMBER_FORMAT.format(Math.round((double) shown)) + " / " + NUMBER_FORMAT.format(Math.round((double) max));
		float changeValue = percent ? Math.round(current / max * 100.0f) : Math.round(current);

		HudStatNumberAnimator.RenderState state = numberAnimator.update(String.valueOf(changeValue), changeValue, tickTime);
		if (state.isHidden()) return;
		HudRender.text(graphics, text, x + state.offsetX(), y + state.offsetY(), scale, align, state.rgbColor(), state.alpha());
	}
}
