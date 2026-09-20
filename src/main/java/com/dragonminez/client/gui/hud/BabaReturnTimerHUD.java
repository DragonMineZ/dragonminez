package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class BabaReturnTimerHUD {
	private static final ResourceLocation BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/kicharge_hud.png");
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static final float[] HALO_GOLD = {1.0f, 0.956f, 0.38f};
	private static final float[] CRITICAL_RED = {1.0f, 0.33f, 0.33f};
	private static final float BAR_WIDTH = 145.0f;
	private static final float BAR_HEIGHT = 14.0f;
	private static final float BAR_Y = 13.0f;
	private static final int PREVIEW_SECONDS = 754;
	private static final float HIDDEN_PREVIEW_ALPHA = 0.35f;

	private static final HudSmoother FILL = new HudSmoother(0.15f);

	public static final IGuiOverlay HUD_BABA_RETURN = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null || mc.level == null) return;

		boolean preview = HudLayout.isPreview();
		HudLayout.Box box = HudLayout.resolve(HudElement.BABA_TIMER, width, height);
		if (!box.visible() && !preview) return;

		float fillRatio;
		int seconds;
		if (preview) {
			fillRatio = 0.6f;
			seconds = PREVIEW_SECONDS;
		} else {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, mc.player).orElse(null);
			if (data == null || !data.getStatus().isHasCreatedCharacter() || data.getStatus().isAlive()) return;
			int timer = data.getStatus().getTempReturnTimer();
			if (timer <= 0) return;
			int totalTicks = Math.max(1, ConfigManager.getServerConfig().getGameplay().getBabaTempReturnSeconds() * 20);
			fillRatio = FILL.update(Mth.clamp(timer / (float) totalTicks, 0.0f, 1.0f));
			seconds = timer / 20;
		}

		boolean critical = seconds < 60;
		float[] tint = critical ? CRITICAL_RED : HALO_GOLD;
		float pulse = critical ? 0.7f + 0.3f * Mth.sin((System.nanoTime() / 1_000_000L % 3_600_000L) / 1000.0f * 7.0f) : 1.0f;
		float alpha = box.visible() ? 1.0f : HIDDEN_PREVIEW_ALPHA;

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(box.x(), box.y(), 0.0f);
		guiGraphics.pose().scale(box.scale(), box.scale(), 1.0f);

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.9f * alpha);
		HudRender.blit(guiGraphics, BAR_TEXTURE, 0.0f, BAR_Y, 0.0f, 0.0f, BAR_WIDTH, BAR_HEIGHT, 256, 256);
		RenderSystem.setShaderColor(tint[0], tint[1], tint[2], pulse * alpha);
		HudRender.blit(guiGraphics, BAR_TEXTURE, 0.0f, BAR_Y, 0.0f, 14.0f, BAR_WIDTH * fillRatio, BAR_HEIGHT, 256, 256);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		Font font = mc.font;
		MutableComponent label = Component.translatable("gui.dragonminez.baba_timer.label").withStyle(Style.EMPTY.withFont(DMZ_FONT));
		MutableComponent time = Component.literal(formatSeconds(seconds)).withStyle(Style.EMPTY.withFont(DMZ_FONT));
		int color = (Math.round(alpha * 255.0f) << 24) | (critical ? 0xFF5555 : 0xFFF461);
		guiGraphics.drawString(font, label, Math.round((BAR_WIDTH - font.width(label)) / 2.0f), 1, color, true);
		guiGraphics.drawString(font, time, Math.round((BAR_WIDTH - font.width(time)) / 2.0f), Math.round(BAR_Y + BAR_HEIGHT + 3.0f), color, true);
		guiGraphics.pose().popPose();
	}

	private static String formatSeconds(int totalSeconds) {
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;
		if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
		return String.format("%d:%02d", minutes, seconds);
	}
}
