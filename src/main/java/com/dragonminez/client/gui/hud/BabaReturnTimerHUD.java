package com.dragonminez.client.gui.hud;

import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.util.BabaReviveService;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class BabaReturnTimerHUD {
	public static final float WIDTH = 112.0f;
	public static final float HEIGHT = 36.0f;

	private static final float TITLE_Y = 3.5f;
	private static final float TIME_Y = 13.0f;
	private static final float TAG_Y = 25.5f;
	private static final float TITLE_SCALE = 0.75f;
	private static final float TIME_SCALE = 1.1f;
	private static final float TAG_SCALE = 0.75f;
	private static final float HIDDEN_PREVIEW_ALPHA = 0.35f;
	private static final int TITLE_COLOR = 0xFFD54F;
	private static final int TIME_COLOR = 0xFFFFFF;
	private static final int CRITICAL_COLOR = 0xFF5555;
	private static final int LOW_MULTIPLIER_COLOR = 0x7CFF7C;
	private static final int MID_MULTIPLIER_COLOR = 0xFFB74D;
	private static final int HIGH_MULTIPLIER_COLOR = 0xFF5555;
	private static final int PAUSED_COLOR = 0x9FD8FF;
	private static final int KO_COLOR = 0xFF7A7A;
	private static final int PREVIEW_SECONDS = 754;
	private static final double PREVIEW_MULTIPLIER = 2.25;

	private static final HudSmoother PANEL_ALPHA = new HudSmoother(0.18f, 0.005f);
	private static final HudSmoother TAG_ALPHA = new HudSmoother(0.22f, 0.005f);
	private static final HudSmoother SECONDS = new HudSmoother(0.3f, 0.01f);
	private static String lastTag = "";
	private static int lastTagColor = LOW_MULTIPLIER_COLOR;

	public static final IGuiOverlay HUD_BABA_RETURN = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null || mc.level == null) return;

		boolean preview = HudLayout.isPreview();
		HudLayout.Box box = HudLayout.resolve(HudElement.BABA_TIMER, width, height);
		if (!box.visible() && !preview) return;

		int targetSeconds;
		double multiplier;
		boolean paused = false;
		boolean knockedOut = false;
		boolean active;
		if (preview) {
			targetSeconds = PREVIEW_SECONDS;
			multiplier = PREVIEW_MULTIPLIER;
			active = true;
		} else {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, mc.player).orElse(null);
			active = data != null && data.getStatus().isHasCreatedCharacter() && BabaReviveService.isTempReviveActive(data);
			if (active) {
				targetSeconds = data.getStatus().getTempReturnTimer() / 20;
				knockedOut = BabaReviveService.isKnockedOut(data);
				paused = mc.level.dimension().equals(OtherworldDimension.OTHERWORLD_KEY);
				multiplier = paused ? 0.0 : BabaReviveService.totalTimeMultiplier(mc.player, data);
			} else {
				targetSeconds = 0;
				multiplier = 1.0;
			}
		}

		float panelAlpha = preview ? 1.0f : PANEL_ALPHA.update(active ? 1.0f : 0.0f);
		if (panelAlpha <= 0.01f) {
			SECONDS.snap(targetSeconds);
			return;
		}
		float alpha = panelAlpha * (box.visible() ? 1.0f : HIDDEN_PREVIEW_ALPHA);

		int seconds = preview ? targetSeconds : Math.round(SECONDS.update(targetSeconds));
		if (Math.abs(seconds - targetSeconds) > 30) {
			SECONDS.snap(targetSeconds);
			seconds = targetSeconds;
		}

		String tag = null;
		int tagColor = LOW_MULTIPLIER_COLOR;
		if (knockedOut) {
			tag = Component.translatable("gui.dragonminez.baba_timer.knocked_out").getString();
			tagColor = KO_COLOR;
		} else if (paused) {
			tag = Component.translatable("gui.dragonminez.baba_timer.paused").getString();
			tagColor = PAUSED_COLOR;
		} else if (multiplier > 1.0001) {
			tag = "(" + BabaReviveService.formatMultiplier(multiplier) + ")";
			tagColor = multiplierColor(multiplier);
		}
		if (tag != null) {
			lastTag = tag;
			lastTagColor = tagColor;
		}
		float tagAlpha = preview ? 1.0f : TAG_ALPHA.update(tag != null ? 1.0f : 0.0f);

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(box.x(), box.y(), 0.0f);
		guiGraphics.pose().scale(box.scale(), box.scale(), 1.0f);
		HudRender.panel(guiGraphics, 0.0f, 0.0f, WIDTH, HEIGHT, alpha);

		float centerX = WIDTH / 2.0f;
		String header = Component.translatable("gui.dragonminez.baba_timer.label").getString();
		dmzText(guiGraphics, header, centerX, TITLE_Y, TITLE_SCALE, TITLE_COLOR, alpha);

		boolean critical = seconds < 60 && !paused && !knockedOut;
		float pulse = critical ? 0.7f + 0.3f * Mth.sin((System.nanoTime() / 1_000_000L % 3_600_000L) / 1000.0f * 7.0f) : 1.0f;
		dmzText(guiGraphics, BabaReviveService.formatSeconds(Math.max(0, seconds)), centerX, TIME_Y, TIME_SCALE,
				critical ? CRITICAL_COLOR : TIME_COLOR, alpha * pulse);

		if (tagAlpha > 0.02f && !lastTag.isEmpty()) {
			dmzText(guiGraphics, lastTag, centerX, TAG_Y, TAG_SCALE, lastTagColor, alpha * tagAlpha);
		}

		guiGraphics.pose().popPose();
	}

	private static int multiplierColor(double multiplier) {
		if (multiplier > 3.0) return HIGH_MULTIPLIER_COLOR;
		if (multiplier > 2.0) return MID_MULTIPLIER_COLOR;
		return LOW_MULTIPLIER_COLOR;
	}

	private static void dmzText(GuiGraphics graphics, String text, float centerX, float y, float scale, int rgb, float alpha) {
		HudRender.dmzText(graphics, text, centerX, y, scale, 0.5f, rgb, alpha);
	}
}
