package com.dragonminez.client.gui.dialogue;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.HudRender;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class DialogueSkin {
	static final ResourceLocation SHEET = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/npcdialogue.png");
	private static final int SHEET_SIZE = 256;
	static final int LINE_H = 5;

	static final int ACCENT = 0x7CFDD6;
	static final int METAL = 0xD2D7F1;
	static final int TEXT = 0xFFFFFF;
	static final int TEXT_SOFT = 0xD9D9E0;
	static final int MUTED = 0x8A8A94;
	static final int OFFER = 0x9BE564;
	static final int TURN_IN = ACCENT;
	static final int PROGRESS = 0xFFD54A;
	static final int BAND_RGB = 0x050B06;
	static final int SCROLL_TRACK = 0xFF16241A;
	static final int SCROLL_THUMB = 0xFF909AC3;
	static final int SCROLL_ACTIVE = 0xFFFFFFFF;

	enum Row {
		NORMAL(0), HOVER(1), DISABLED(2), PRIMARY(3), DANGER(4), SELECTED(5);

		final int u;

		Row(int index) {
			this.u = index * 40;
		}
	}

	enum Icon {
		NONE(-1), OFFER(0), TURN_IN(1), PROGRESS(2), LOCK(3), CLOCK(4), CHEVRON(5), DOWN(6), CHECK(7);

		final int u;

		Icon(int index) {
			this.u = index * 10;
		}
	}

	private DialogueSkin() {
	}

	static void band(GuiGraphics graphics, float x, float y, float width, float height, float alpha) {
		tint(0xFFFFFF, alpha);
		HudRender.blit(graphics, SHEET, x, y, 128, 72, width, height, 8, 64, SHEET_SIZE, SHEET_SIZE);
		untint();
	}

	static void shadow(GuiGraphics graphics, float x, float y, float width, float height, float alpha) {
		tint(0xFFFFFF, alpha);
		HudRender.blit(graphics, SHEET, x, y, 136, 72, width, height, 8, 32, SHEET_SIZE, SHEET_SIZE);
		untint();
	}

	static void line(GuiGraphics graphics, float x, float y, float width, float alpha) {
		tint(0xFFFFFF, alpha);
		HudRender.blit(graphics, SHEET, x, y, 144, 72, width, LINE_H, 8, LINE_H, SHEET_SIZE, SHEET_SIZE);
		untint();
	}

	static void tab(GuiGraphics graphics, float x, float y, float width, float alpha) {
		tint(0xFFFFFF, alpha);
		threeSlice(graphics, x, y, width, 18, 64, 0, 48, 18, 6, 20);
		untint();
	}

	static void row(GuiGraphics graphics, Row row, float x, float y, float width, float height, float alpha) {
		if (alpha <= 0.004f) return;
		tint(0xFFFFFF, alpha);
		threeSlice(graphics, x, y, width, height, row.u, 32, 40, 20, 4, 4);
		untint();
	}

	static void icon(GuiGraphics graphics, Icon icon, float x, float y, int rgb, float alpha) {
		if (icon == Icon.NONE || alpha <= 0.004f) return;
		tint(rgb, alpha);
		HudRender.blit(graphics, SHEET, x, y, icon.u, 56, 10, 10, SHEET_SIZE, SHEET_SIZE);
		untint();
	}

	static void input(GuiGraphics graphics, float x, float y, float width, float height, float alpha) {
		tint(0xFFFFFF, alpha);
		HudRender.nineSlice(graphics, SHEET, x, y, width, height, 0, 128, 24, 18, 5, SHEET_SIZE, SHEET_SIZE);
		untint();
	}

	static void chip(GuiGraphics graphics, float x, float y, float width, float height, float alpha) {
		tint(0xFFFFFF, alpha);
		HudRender.nineSlice(graphics, SHEET, x, y, width, height, 0, 160, 16, 12, 4, SHEET_SIZE, SHEET_SIZE);
		untint();
	}

	private static void threeSlice(GuiGraphics graphics, float x, float y, float width, float height,
								   int u, int v, int regionWidth, int regionHeight, int leftCap, int rightCap) {
		float scale = height / regionHeight;
		float left = Math.min(leftCap * scale, width / 2.0f);
		float right = Math.min(rightCap * scale, width - left);
		float middle = Math.max(0.0f, width - left - right);
		HudRender.blit(graphics, SHEET, x, y, u, v, left, height, left / scale, regionHeight, SHEET_SIZE, SHEET_SIZE);
		if (middle > 0.0f) {
			HudRender.blit(graphics, SHEET, x + left, y, u + leftCap, v, middle, height,
					regionWidth - leftCap - rightCap, regionHeight, SHEET_SIZE, SHEET_SIZE);
		}
		HudRender.blit(graphics, SHEET, x + left + middle, y, u + regionWidth - right / scale, v, right, height,
				right / scale, regionHeight, SHEET_SIZE, SHEET_SIZE);
	}

	private static void tint(int rgb, float alpha) {
		RenderSystem.setShaderColor(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f,
				Mth.clamp(alpha, 0.0f, 1.0f));
	}

	private static void untint() {
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}
}
