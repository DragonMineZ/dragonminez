package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.clash.ClientBeamClashState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

	private static final int PANEL_W = 244;
	private static final int PANEL_H = 84;
	private static final int BAR_W = 208;
	private static final int BAR_H = 16;

	private static final int PANEL_BG = 0xC00A0E18;
	private static final int PANEL_INNER = 0x60000000;
	private static final int YOU_LABEL = 0xFF7FE0FF;
	private static final int FOE_LABEL = 0xFFFF6B61;
	private static final int FOE_FILL = 0xE0443B;
	private static final int SWEET_BASE = 0x0038E04B; // RGB only; alpha supplied by the pulse
	private static final int KNOB = 0xFFFFFFFF;

	public static final IGuiOverlay HUD_BEAM_CLASH = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.renderDebug) return;
		if (!ClientBeamClashState.isActive()) return;

		float advantage = Mth.clamp(ClientBeamClashState.advantage(), 0.0f, 1.0f);
		float phase = Mth.clamp(ClientBeamClashState.meterPhase(), 0.0f, 1.0f);
		float sweetLow = Mth.clamp(ClientBeamClashState.sweetLow(), 0.0f, 1.0f);
		float sweetHigh = Mth.clamp(ClientBeamClashState.sweetHigh(), 0.0f, 1.0f);
		int beamRgb = ClientBeamClashState.beamColor() & 0xFFFFFF;
		int beamArgb = 0xFF000000 | beamRgb;

		int panelX = (width - PANEL_W) / 2;
		int panelY = height - PANEL_H - 34;
		int barX = panelX + (PANEL_W - BAR_W) / 2;
		int tugY = panelY + 24;
		int sweepY = panelY + 52;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		// --- panel backdrop ---
		guiGraphics.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, PANEL_BG);
		guiGraphics.fill(panelX + 2, panelY + 2, panelX + PANEL_W - 2, panelY + PANEL_H - 2, PANEL_INNER);
		// accent borders tinted by the beam color
		guiGraphics.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, beamArgb);
		guiGraphics.fill(panelX, panelY + PANEL_H - 1, panelX + PANEL_W, panelY + PANEL_H, beamArgb);

		// --- title ---
		Component title = Component.translatable("hud." + Reference.MOD_ID + ".beam_clash_title");
		guiGraphics.drawString(mc.font, title, (width - mc.font.width(title)) / 2, panelY + 6, 0xFFFFFFFF, true);

		// --- tug-of-war bar ---
		int split = Math.round(BAR_W * advantage);
		drawBarFrame(guiGraphics, barX, tugY);
		setColor(beamRgb);
		drawBarFill(guiGraphics, barX, tugY, 0, split);
		setColor(FOE_FILL);
		drawBarFill(guiGraphics, barX, tugY, split, BAR_W - split);
		resetColor();
		// moving knob at the split
		guiGraphics.fill(barX + split - 1, tugY - 2, barX + split + 1, tugY + BAR_H + 2, KNOB);
		// end labels
		guiGraphics.drawString(mc.font, "YOU", barX, tugY - 10, YOU_LABEL, true);
		guiGraphics.drawString(mc.font, "FOE", barX + BAR_W - mc.font.width("FOE"), tugY - 10, FOE_LABEL, true);

		// --- sweep bar ---
		drawBarFrame(guiGraphics, barX, sweepY);
		// pulsing sweet-spot
		int sweetX1 = barX + Math.round(BAR_W * sweetLow);
		int sweetX2 = barX + Math.round(BAR_W * sweetHigh);
		float pulse = 0.55f + 0.45f * Mth.sin((System.currentTimeMillis() % 1000L) / 1000.0f * Mth.TWO_PI);
		int sweetAlpha = (int) (0x40 + pulse * 0x70) << 24;
		guiGraphics.fill(sweetX1, sweepY, sweetX2, sweepY + BAR_H, SWEET_BASE | sweetAlpha);
		guiGraphics.fill(sweetX1, sweepY, sweetX1 + 1, sweepY + BAR_H, 0xFF38E04B);
		guiGraphics.fill(sweetX2 - 1, sweepY, sweetX2, sweepY + BAR_H, 0xFF38E04B);

		// arrow marker
		int markerX = barX + Math.round(BAR_W * phase);
		for (int r = 0; r < 5; r++) {
			int hw = 5 - r;
			guiGraphics.fill(markerX - hw, sweepY - 7 + r, markerX + hw + 1, sweepY - 6 + r, beamArgb);
		}
		guiGraphics.fill(markerX, sweepY - 2, markerX + 1, sweepY + BAR_H + 2, 0xFFFFFFFF);

		// --- hint ---
		Component hint = Component.translatable("hud." + Reference.MOD_ID + ".beam_clash_hint");
		guiGraphics.drawString(mc.font, hint, (width - mc.font.width(hint)) / 2, sweepY + BAR_H + 4, 0xFFB9C7D6, true);
	};

	private static void drawBarFrame(GuiGraphics g, int x, int y) {
		setColor(0xFFFFFF);
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

	private static void setColor(int rgb) {
		RenderSystem.setShaderColor(
				((rgb >> 16) & 0xFF) / 255.0f,
				((rgb >> 8) & 0xFF) / 255.0f,
				(rgb & 0xFF) / 255.0f,
				1.0f);
	}

	private static void resetColor() {
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}
}
