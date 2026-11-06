package com.dragonminez.client.gui.hud;

import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.common.config.ConfigManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

public final class HudSideMeters {
	private static final HudSprites.BarSkin SKIN = HudSprites.RESERVE_BAR;
	private static final float BORDER = SKIN.border();
	private static final float BAR_WIDTH = SKIN.width();
	private static final float BAR_HEIGHT = SKIN.height();
	private static final float CAPSULE = HudSprites.RESERVE_CAPSULE.guiWidth();
	private static final float PREVIEW_VALUE = 0.65f;

	private HudSideMeters() {}

	public static float update(HudElement element, HudSmoother visibility, boolean applicable, boolean filled, int screenWidth, int screenHeight) {
		if (HudLayout.isPreview()) {
			visibility.snap(1.0f);
			HudLayout.setMeterVisibility(element, 1.0f);
			return 1.0f;
		}
		boolean shown = applicable && (filled || !HudLayout.isCollapsible(element, screenWidth, screenHeight));
		float value = visibility.update(shown ? 1.0f : 0.0f);
		HudLayout.setMeterVisibility(element, value);
		return value;
	}

	public static void draw(GuiGraphics guiGraphics, HudElement element, int screenWidth, int screenHeight, HudBar bar, HudSprites.Sprite icon,
							float visibility, int color, float pulse) {
		HudLayout.Box box = HudLayout.resolve(element, screenWidth, screenHeight);
		boolean preview = HudLayout.isPreview();
		if (!box.visible() && !preview) return;
		if (visibility <= 0.01f) return;
		if (preview && bar.targetFraction() <= 0.0f) bar.update(PREVIEW_VALUE, 1.0f);

		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		HudRender.setAlphaScale(visibility * (box.visible() ? 1.0f : 0.35f));
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(box.x(), box.y(), 0.0f);
		guiGraphics.pose().scale(box.scale(), box.scale(), 1.0f);
		try {
			bar.drawVertical(guiGraphics, SKIN, BORDER, CAPSULE, BAR_WIDTH, BAR_HEIGHT, color, pulse);
			HudRender.sprite(guiGraphics, HudSprites.RESERVE_CAPSULE, 0.0f, 0.0f, CAPSULE, CAPSULE, 0xFFFFFF, 1.0f);
			HudRender.sprite(guiGraphics, icon, BORDER, BORDER, icon.guiWidth(), icon.guiHeight(), HudRender.mix(color, 0xFFFFFF, 0.45f), 1.0f);
			if (!ConfigManager.getUserConfig().getHideHudNumbers()) {
				HudRender.text(guiGraphics, Math.round(bar.fraction() * 100.0f) + "%", CAPSULE / 2.0f, CAPSULE + BAR_HEIGHT + BORDER + 2.0f, 0.5f, 0.5f, HudRender.mix(color, 0xFFFFFF, 0.7f), 1.0f);
			}
		} finally {
			guiGraphics.pose().popPose();
			HudRender.setAlphaScale(1.0f);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		}
	}
}
