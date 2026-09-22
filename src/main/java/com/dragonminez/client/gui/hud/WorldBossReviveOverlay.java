package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.client.systems.worldboss.ClientWorldBossPlayerState;
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

public class WorldBossReviveOverlay {
	private static final ResourceLocation CHARGE_HUD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/kicharge_hud.png");
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	private static final float SCALE = 1.125f;
	private static final float BAR_WIDTH = 145.0f;
	private static final float FILL_WIDTH = 148.0f;
	private static final float BAR_HEIGHT = 14.0f;
	private static final int BAR_COLOR = 0x5BD64A;
	private static final int TEXT_COLOR = 0xFFB9FFA8;

	private static final HudSmoother PROGRESS = new HudSmoother(0.12f, 0.002f);
	private static final HudSmoother ALPHA = new HudSmoother(0.1f, 0.01f);

	public static final IGuiOverlay HUD_WORLDBOSS_REVIVE = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, width, height);
	};

	private static void render(GuiGraphics guiGraphics, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;

		boolean casting = ClientWorldBossPlayerState.isCasting();
		float alpha = ALPHA.update(casting ? 1.0f : 0.0f);
		if (alpha <= 0.01f) {
			if (!casting) PROGRESS.snap(0.0f);
			return;
		}
		float progress = PROGRESS.update(casting ? Mth.clamp(ClientWorldBossPlayerState.castProgress(), 0.0f, 1.0f) : PROGRESS.value());

		float barWidth = BAR_WIDTH * SCALE;
		float x = (width - barWidth) / 2.0f;
		float y = height - 72.0f;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 0.0f);
		guiGraphics.pose().scale(SCALE, SCALE, 1.0f);

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
		HudRender.blit(guiGraphics, CHARGE_HUD_TEXTURE, 0.0f, 0.0f, 0.0f, 0.0f, BAR_WIDTH, BAR_HEIGHT, 256, 256);
		float r = ((BAR_COLOR >> 16) & 0xFF) / 255.0f;
		float g = ((BAR_COLOR >> 8) & 0xFF) / 255.0f;
		float b = (BAR_COLOR & 0xFF) / 255.0f;
		RenderSystem.setShaderColor(r, g, b, alpha);
		HudRender.blit(guiGraphics, CHARGE_HUD_TEXTURE, 0.0f, 0.0f, 0.0f, 14.0f, FILL_WIDTH * progress, BAR_HEIGHT, 256, 256);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		guiGraphics.pose().popPose();

		Font font = mc.font;
		int alphaChannel = Math.max(4, Math.round(alpha * 255.0f));
		int color = (alphaChannel << 24) | (TEXT_COLOR & 0xFFFFFF);
		MutableComponent label = Component.translatable("gui.dragonminez.worldboss.revive.casting", ClientWorldBossPlayerState.castTargetName())
				.withStyle(Style.EMPTY.withFont(DMZ_FONT));
		MutableComponent percent = Component.literal(Math.round(progress * 100.0f) + "%").withStyle(Style.EMPTY.withFont(DMZ_FONT));
		int percentY = Math.round(y) - 4 - font.lineHeight;
		int labelY = percentY - 2 - font.lineHeight;
		guiGraphics.drawString(font, label, (width - font.width(label)) / 2, labelY, color, true);
		guiGraphics.drawString(font, percent, (width - font.width(percent)) / 2, percentY, color, true);
	}
}
