package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
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

	public static final IGuiOverlay HUD_BABA_RETURN = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null || mc.level == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;
			if (data.getStatus().isAlive()) return;
			int timer = data.getStatus().getTempReturnTimer();
			if (timer <= 0) return;

			int totalTicks = Math.max(1, ConfigManager.getServerConfig().getGameplay().getBabaTempReturnSeconds() * 20);
			float fillRatio = Mth.clamp(timer / (float) totalTicks, 0.0f, 1.0f);
			int seconds = timer / 20;
			boolean critical = seconds < 60;
			float[] tint = critical ? CRITICAL_RED : HALO_GOLD;

			float pulse = critical
					? 0.7f + 0.3f * Mth.sin((mc.level.getGameTime() + partialTicks) * 0.35f)
					: 1.0f;

			int barW = 145;
			int barH = 14;
			int x = (width - barW) / 2;
			int y = 16;
			int fillPixels = Math.round(barW * fillRatio);

			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, BAR_TEXTURE);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.9f);

			guiGraphics.blit(BAR_TEXTURE, x, y, 0, 0, barW, barH, 256, 256);
			if (fillPixels > 0) {
				RenderSystem.setShaderColor(tint[0], tint[1], tint[2], pulse);
				guiGraphics.blit(BAR_TEXTURE, x, y, 0, 14, fillPixels, barH, 256, 256);
			}
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			RenderSystem.disableBlend();

			Font font = mc.font;
			MutableComponent label = Component.translatable("gui.dragonminez.baba_timer.label")
					.withStyle(Style.EMPTY.withFont(DMZ_FONT));
			MutableComponent time = Component.literal(formatSeconds(seconds))
					.withStyle(Style.EMPTY.withFont(DMZ_FONT));

			int labelColor = critical ? 0xFFFF5555 : 0xFFFFF461;
			guiGraphics.drawString(font, label, (width - font.width(label)) / 2, y - font.lineHeight - 3, labelColor, true);
			guiGraphics.drawString(font, time, (width - font.width(time)) / 2, y + barH + 3, labelColor, true);
		});
	};

	private static String formatSeconds(int totalSeconds) {
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;
		if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
		return String.format("%d:%02d", minutes, seconds);
	}
}
