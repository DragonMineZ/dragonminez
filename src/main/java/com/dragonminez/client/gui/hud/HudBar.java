package com.dragonminez.client.gui.hud;

import com.dragonminez.common.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.text.NumberFormat;
import java.util.Locale;

public final class HudBar {
	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
	private static final int DAMAGE_CHIP = 0xFF4646;
	private static final int SPENT_CHIP = 0xE6E9F2;
	private static final int HEAL_CHIP = 0x57FF6B;

	private final HudBarAnimator animator = new HudBarAnimator();
	private final HudSmoother number = new HudSmoother(0.11f, 0.45f);
	private final HudSmoother motion = new HudSmoother(0.18f);
	private final HudStatNumberAnimator numberAnimator;
	private final int lossChipRgb;
	private float lastMax = -1.0f;
	private float current;
	private float max = 1.0f;
	private float lastFront = -1.0f;
	private boolean mirrored;

	public HudBar(HudStatNumberAnimator.StatKind kind) {
		this.numberAnimator = new HudStatNumberAnimator(kind);
		this.lossChipRgb = kind == HudStatNumberAnimator.StatKind.HEALTH || kind == HudStatNumberAnimator.StatKind.KISENSE_HEALTH ? DAMAGE_CHIP : SPENT_CHIP;
	}

	public void update(float current, float max) {
		this.max = Math.max(1.0f, max);
		this.current = Mth.clamp(current, 0.0f, this.max);
		float fraction = this.current / this.max;

		if (lastMax != this.max) {
			animator.reset(fraction);
			number.snap(this.current);
			lastMax = this.max;
		}
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

			float glint = motion.value();
			float frontWidth = front * width;
			if (glint > 0.02f && frontWidth < width - 0.5f) {
				float glintWidth = Math.min(frontWidth, 5.0f);
				int clear = HudRender.argb(0.0f, 0xFFFFFF);
				int bright = HudRender.argb(0.75f * glint, 0xFFFFFF);
				if (mirrored) HudRender.rectHorizontal(graphics, x + width - frontWidth, y, glintWidth, height, bright, clear);
				else HudRender.rectHorizontal(graphics, x + frontWidth - glintWidth, y, glintWidth, height, clear, bright);
			}
		}
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
			HudRender.rect(graphics, x, y + height * (1.0f - front), width, Math.min(1.0f, front * height), HudRender.argb(0.7f, 0xFFFFFF));
		}
	}

	public static void drawStatic(GuiGraphics graphics, HudSprites.BarSkin skin, float x, float y, float width, float height, float fraction, int fillRgb, boolean mirrored) {
		drawFrame(graphics, skin, x, y, width, height, 0.0f, mirrored);
		drawFill(graphics, skin, x, y, width, height, 0.0f, 0.0f, Mth.clamp(fraction, 0.0f, 1.0f), 1.0f, fillRgb, 1.0f, mirrored);
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
		if (!ConfigManager.getUserConfig().getAdvancedDescription()) return;
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
