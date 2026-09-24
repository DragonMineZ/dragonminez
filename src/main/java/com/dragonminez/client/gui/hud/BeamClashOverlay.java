package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.clash.ClientBeamClashState;
import com.dragonminez.common.combat.clash.ClashMeter;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class BeamClashOverlay {
	private static final ResourceLocation BAR_TEXTURE =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/kicharge_hud.png");

	private static final int SRC_W = 148;
	private static final int SRC_H = 14;
	private static final int FILL_V = 14;
	private static final int ATLAS = 256;

	private static final int PANEL_W = 252;
	private static final int PANEL_H = 94;
	private static final int BAR_W = 212;
	private static final int BAR_H = 14;
	private static final int TRAIL_PX = 18;

	private static final int PANEL_BG = 0xC00A0E18;
	private static final int PANEL_INNER = 0x60000000;
	private static final int WHITE = 0xFFFFFF;
	private static final int PERFECT_COLOR = 0xFFD84A;
	private static final int MISS_COLOR = 0xFF5A5A;
	private static final int HINT_COLOR = 0xB9C7D6;
	private static final int FOE_FALLBACK = 0xE0443B;

	private static final float FLASH_SHARE = 0.25f;

	public static final IGuiOverlay HUD_BEAM_CLASH = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.renderDebug) return;
		if (!ClientBeamClashState.isActive()) {
			drawExhausted(guiGraphics, partialTicks, width, height);
			return;
		}

		float advantage = Mth.clamp(ClientBeamClashState.advantage(), 0.0f, 1.0f);
		int self = ClientBeamClashState.selfColor() & 0xFFFFFF;
		int foe = contrastingFoe(self, ClientBeamClashState.foeColor() & 0xFFFFFF);

		float time = ClientBeamClashState.simTime(partialTicks);
		ClashMeter.Sample sample = ClashMeter.sample(ClientBeamClashState.meterSeed(), time);
		ClientBeamClashState.noteRenderTime(time);
		ClashMeter.Cycle cycle = sample.cycle();

		ClashMeter.Grade grade = ClientBeamClashState.feedbackGrade();
		float feedbackAge = ClientBeamClashState.feedbackAge01();

		int panelX = (width - PANEL_W) / 2;
		int panelY = height - PANEL_H - 34;
		int barX = panelX + (PANEL_W - BAR_W) / 2;
		int tugY = panelY + 27;
		int sweepY = panelY + 60;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		HudRender.rect(guiGraphics, panelX, panelY, PANEL_W, PANEL_H, PANEL_BG);
		HudRender.rect(guiGraphics, panelX + 2, panelY + 2, PANEL_W - 4, PANEL_H - 4, PANEL_INNER);

		HudRender.rectHorizontal(guiGraphics, panelX, panelY, PANEL_W, 1, HudRender.argb(1.0f, self), HudRender.argb(1.0f, foe));
		HudRender.rectHorizontal(guiGraphics, panelX, panelY + PANEL_H - 1, PANEL_W, 1, HudRender.argb(1.0f, self), HudRender.argb(1.0f, foe));

		Component title = Component.translatable("hud." + Reference.MOD_ID + ".beam_clash_title");
		guiGraphics.drawString(mc.font, title, (width - mc.font.width(title)) / 2, panelY + 7, 0xFFFFFFFF, true);

		HudRender.text(guiGraphics, I18n.get("hud." + Reference.MOD_ID + ".beam_clash_you"), barX, tugY - 11, 1.0f, 0.0f, HudRender.mix(self, WHITE, 0.45f), 1.0f);
		HudRender.text(guiGraphics, I18n.get("hud." + Reference.MOD_ID + ".beam_clash_foe"), barX + BAR_W, tugY - 11, 1.0f, 1.0f, HudRender.mix(foe, WHITE, 0.45f), 1.0f);
		int split = Math.round(BAR_W * advantage);
		drawBarFrame(guiGraphics, barX, tugY);
		setColor(self, 1.0f);
		drawBarFill(guiGraphics, barX, tugY, 0, split);
		setColor(foe, 1.0f);
		drawBarFill(guiGraphics, barX, tugY, split, BAR_W - split);
		resetColor();

		HudRender.rect(guiGraphics, barX + BAR_W / 2.0f, tugY - 1, 1, BAR_H + 2, HudRender.argb(0.35f, WHITE));
		HudRender.rect(guiGraphics, barX + split - 3, tugY - 3, 6, BAR_H + 6, HudRender.argb(0.25f, WHITE));
		HudRender.rect(guiGraphics, barX + split - 1, tugY - 2, 2, BAR_H + 4, HudRender.argb(1.0f, WHITE));

		drawBarFrame(guiGraphics, barX, sweepY);
		float lowX = barX + BAR_W * cycle.low();
		float highX = barX + BAR_W * cycle.high();
		int windowPx0 = Math.round(BAR_W * cycle.low());
		int windowPxW = Math.round(BAR_W * cycle.high()) - windowPx0;
		setColor(self, 0.55f);
		drawBarFill(guiGraphics, barX, sweepY, windowPx0, windowPxW);
		resetColor();

		float coreHalf = BAR_W * cycle.halfWidth() * ClashMeter.PERFECT_FRACTION;
		float centerX = barX + BAR_W * cycle.center();
		HudRender.rect(guiGraphics, centerX - coreHalf, sweepY + 1, coreHalf * 2.0f, BAR_H - 2, HudRender.argb(0.55f, HudRender.mix(self, WHITE, 0.7f)));

		HudRender.rect(guiGraphics, lowX, sweepY - 1, 1, BAR_H + 2, HudRender.argb(0.9f, WHITE));
		HudRender.rect(guiGraphics, highX - 1, sweepY - 1, 1, BAR_H + 2, HudRender.argb(0.9f, WHITE));

		if (grade != null && feedbackAge < FLASH_SHARE) {
			float flash = 1.0f - feedbackAge / FLASH_SHARE;
			HudRender.rect(guiGraphics, lowX, sweepY, highX - lowX, BAR_H, HudRender.argb(0.65f * flash, gradeColor(grade, self)));
		}

		float markerX = barX + BAR_W * sample.marker();
		if (cycle.reversed()) {
			HudRender.rectHorizontal(guiGraphics, markerX, sweepY, TRAIL_PX, BAR_H, HudRender.argb(0.6f, self), HudRender.argb(0.0f, self));
		} else {
			HudRender.rectHorizontal(guiGraphics, markerX - TRAIL_PX, sweepY, TRAIL_PX, BAR_H, HudRender.argb(0.0f, self), HudRender.argb(0.6f, self));
		}
		HudRender.rect(guiGraphics, markerX - 1, sweepY - 3, 2, BAR_H + 6, HudRender.argb(1.0f, WHITE));
		for (int r = 0; r < 5; r++) {
			int hw = 5 - r;
			HudRender.rect(guiGraphics, markerX - hw, sweepY - 10 + r, hw * 2 + 1, 1, HudRender.argb(1.0f, self));
		}

		if (grade != null) {
			float alpha = 1.0f - feedbackAge * feedbackAge;
			float rise = 8.0f * feedbackAge;
			HudRender.text(guiGraphics, I18n.get(gradeKey(grade)), width / 2.0f, panelY - 16 - rise, 1.3f, 0.5f, gradeColor(grade, self), alpha);
		}

		HudRender.text(guiGraphics, I18n.get("hud." + Reference.MOD_ID + ".beam_clash_hint"), width / 2.0f, sweepY + BAR_H + 7, 1.0f, 0.5f, HINT_COLOR, 1.0f);
	};

	private static void drawExhausted(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		float remaining = ClientBeamClashState.exhaustRemaining01(partialTicks);
		if (remaining <= 0.0f) return;
		float pulse = 0.75f + 0.25f * (float) Math.sin((System.nanoTime() / 1.0e9) * 9.0);
		float alpha = Math.min(1.0f, remaining * 3.0f) * pulse;
		int panelY = height - PANEL_H - 34;
		HudRender.text(guiGraphics, I18n.get("hud." + Reference.MOD_ID + ".beam_clash_exhausted"), width / 2.0f, panelY + 30, 1.6f, 0.5f, MISS_COLOR, alpha);
	}

	private static String gradeKey(ClashMeter.Grade grade) {
		return switch (grade) {
			case PERFECT -> "hud." + Reference.MOD_ID + ".beam_clash_perfect";
			case GOOD -> "hud." + Reference.MOD_ID + ".beam_clash_good";
			case MISS -> "hud." + Reference.MOD_ID + ".beam_clash_miss";
		};
	}

	private static int gradeColor(ClashMeter.Grade grade, int self) {
		return switch (grade) {
			case PERFECT -> PERFECT_COLOR;
			case GOOD -> HudRender.mix(self, WHITE, 0.5f);
			case MISS -> MISS_COLOR;
		};
	}

	private static int contrastingFoe(int self, int foe) {
		int distance = Math.abs(((self >> 16) & 0xFF) - ((foe >> 16) & 0xFF))
				+ Math.abs(((self >> 8) & 0xFF) - ((foe >> 8) & 0xFF))
				+ Math.abs((self & 0xFF) - (foe & 0xFF));
		return distance < 120 ? HudRender.mix(foe, FOE_FALLBACK, 0.75f) : foe;
	}

	private static void drawBarFrame(GuiGraphics g, int x, int y) {
		setColor(WHITE, 1.0f);
		g.blit(BAR_TEXTURE, x, y, BAR_W, BAR_H, 0.0f, 0.0f, SRC_W, SRC_H, ATLAS, ATLAS);
		resetColor();
	}

	private static void drawBarFill(GuiGraphics g, int barX, int y, int pxOffset, int pxWidth) {
		if (pxWidth <= 0) return;
		float frac0 = pxOffset / (float) BAR_W;
		float fracW = pxWidth / (float) BAR_W;
		g.blit(BAR_TEXTURE, barX + pxOffset, y, pxWidth, BAR_H,
				SRC_W * frac0, FILL_V, Math.round(SRC_W * fracW), SRC_H, ATLAS, ATLAS);
	}

	private static void setColor(int rgb, float alpha) {
		RenderSystem.setShaderColor(
				((rgb >> 16) & 0xFF) / 255.0f,
				((rgb >> 8) & 0xFF) / 255.0f,
				(rgb & 0xFF) / 255.0f,
				alpha);
	}

	private static void resetColor() {
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}
}
