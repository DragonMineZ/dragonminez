package com.dragonminez.client.gui.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

final class HudRadar {
	private static final float ICON_X = 7.0f;
	private static final float FORM_X = 2.0f, FORM_Y = 12.0f;
	private static final int FORM_COLOR = 0xFA3177;

	private HudRadar() {}

	static void draw(GuiGraphics graphics, HudSprites.RadarSprites sprites, float x, float y, float mirrorWidth,
					 String raceName, float iconY, float release, float formCharge) {
		boolean mirrored = mirrorWidth >= 0.0f;
		float level = Mth.clamp(release / 100.0f, 0.0f, 1.0f);

		ResourceLocation icon = RacialIcons.forRace(raceName);
		float size = RacialIcons.SIZE;
		float iconX = mx(x + ICON_X, size, mirrorWidth);
		HudSprites.Sprite empty = new HudSprites.Sprite(icon, RacialIcons.TEXTURE_WIDTH, RacialIcons.TEXTURE_HEIGHT, 0, 0, RacialIcons.SIZE, RacialIcons.SIZE);
		HudSprites.Sprite full = new HudSprites.Sprite(icon, RacialIcons.TEXTURE_WIDTH, RacialIcons.TEXTURE_HEIGHT, 0, RacialIcons.FILL_V, RacialIcons.SIZE, RacialIcons.SIZE);
		HudRender.sprite(graphics, empty, iconX, y + iconY, size, size, 0xFFFFFF, 1.0f, mirrored);
		if (level > 0.0f) HudRender.spritePart(graphics, full, iconX, y + iconY, size, size, 0.0f, 1.0f - level, 1.0f, 1.0f, 0xFFFFFF, 1.0f, mirrored);

		HudSprites.Sprite radar = sprites.radar();
		HudRender.sprite(graphics, radar, mx(x, radar.guiWidth(), mirrorWidth), y, radar.guiWidth(), radar.guiHeight(), 0xFFFFFF, 1.0f, mirrored);

		HudSprites.Sprite form = sprites.form();
		if (formCharge > 0.005f) {
			HudRender.spritePart(graphics, form, mx(x + FORM_X, form.guiWidth(), mirrorWidth), y + FORM_Y, form.guiWidth(), form.guiHeight(),
					0.0f, 1.0f - Mth.clamp(formCharge, 0.0f, 1.0f), 1.0f, 1.0f, FORM_COLOR, 1.0f, mirrored);
		}
	}

	private static float mx(float x, float width, float mirrorWidth) {
		return mirrorWidth < 0.0f ? x : mirrorWidth - x - width;
	}
}
