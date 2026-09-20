package com.dragonminez.client.gui.tutorial;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.gui.hud.HudSmoother;
import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

public final class TutorialHintHUD {
	private static final Style DMZ_STYLE = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth"));
	private static final float PANEL_WIDTH = 180.0f;
	private static final float LINE_HEIGHT = 10.0f;
	private static final HudSmoother VISIBILITY = new HudSmoother(0.18f);

	public static final IGuiOverlay HUD_TUTORIAL_HINT = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, width, height);
	};

	private TutorialHintHUD() {}

	private static void render(GuiGraphics graphics, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		float visibility = VISIBILITY.update(shouldShow(mc) ? 1.0f : 0.0f);
		if (visibility <= 0.01f) return;

		HudLayout.Box box = HudLayout.resolve(HudElement.TRACKED_QUEST, width, height);
		Font font = mc.font;

		Component key = KeyBinds.STATS_MENU.getTranslatedKeyMessage().copy().withStyle(DMZ_STYLE).withStyle(ChatFormatting.BOLD);
		Component message = Component.translatable("gui.dragonminez.tutorial.hint.start", key).withStyle(DMZ_STYLE);
		List<FormattedCharSequence> lines = font.split(message, (int) PANEL_WIDTH - 16);

		float panelHeight = 8.0f + LINE_HEIGHT + lines.size() * LINE_HEIGHT + 6.0f;
		float baseHeight = box.height() / box.scale();
		float anchorY = HudLayout.anchorY(HudElement.TRACKED_QUEST);
		float offsetY = anchorY >= 1.0f ? baseHeight - panelHeight : anchorY >= 0.5f ? (baseHeight - panelHeight) / 2.0f : 0.0f;
		boolean fromRight = box.centerX() > width / 2.0f;
		float slide = (1.0f - visibility) * (PANEL_WIDTH + 12.0f) * (fromRight ? 1.0f : -1.0f);
		float pulse = 0.5f + 0.5f * Mth.sin((System.nanoTime() / 1_000_000L % 3_600_000L) / 1000.0f * 3.2f);

		graphics.pose().pushPose();
		graphics.pose().translate(box.x() + slide * box.scale(), box.y() + offsetY * box.scale(), 0.0f);
		graphics.pose().scale(box.scale(), box.scale(), 1.0f);

		HudRender.setAlphaScale(visibility);
		HudRender.rect(graphics, 0.0f, 0.0f, PANEL_WIDTH, panelHeight, 0xA02A2F40);
		HudRender.rect(graphics, 0.0f, 0.0f, PANEL_WIDTH, 1.0f, HudRender.argb(0.8f, HudRender.mix(0x6D8CFF, 0x7CFDD6, pulse)));
		HudRender.rect(graphics, 0.0f, panelHeight - 1.0f, PANEL_WIDTH, 1.0f, 0x66000000);
		HudRender.setAlphaScale(1.0f);

		int alpha = Math.max(4, Math.round(visibility * 255.0f));
		Component title = Component.translatable("gui.dragonminez.tutorial.hint.title").withStyle(DMZ_STYLE);
		graphics.drawString(font, title, 6, 5, (alpha << 24) | HudRender.mix(0xE8F0FF, 0x7CFDD6, pulse), false);

		int drawY = 5 + (int) LINE_HEIGHT;
		for (FormattedCharSequence line : lines) {
			graphics.drawString(font, line, 6, drawY, (alpha << 24) | 0xFFFFFF, false);
			drawY += (int) LINE_HEIGHT;
		}
		graphics.pose().popPose();
	}

	private static boolean shouldShow(Minecraft mc) {
		if (mc.player == null || mc.options.renderDebug || mc.screen != null) return false;
		if (!TutorialManager.isEnabled()) return false;
		return StatsProvider.get(StatsCapability.INSTANCE, mc.player)
				.map(data -> data.isDataLoaded() && !data.getStatus().isHasCreatedCharacter())
				.orElse(false);
	}
}
