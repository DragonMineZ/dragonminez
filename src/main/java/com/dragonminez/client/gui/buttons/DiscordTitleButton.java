package com.dragonminez.client.gui.buttons;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DiscordTitleButton extends Button {

	private static final int ICON_SIZE = 12;
	private static final int ICON_GAP = 4;
	private static final int ICON_TEXTURE_SIZE = 32;
	private static final ResourceLocation DISCORD_LOGO = new ResourceLocation("minecraft", "textures/gui/title/discord_logo.png");

	public DiscordTitleButton(int x, int y, int width, int height, Component message, OnPress onPress) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
	}

	@Override
	public void renderString(GuiGraphics graphics, Font font, int color) {
		int textWidth = font.width(this.getMessage());
		int contentWidth = ICON_SIZE + ICON_GAP + textWidth;
		int startX = this.getX() + Math.max(4, (this.getWidth() - contentWidth) / 2);
		int iconY = this.getY() + (this.getHeight() - ICON_SIZE) / 2;

		this.renderDiscordLogo(graphics, startX, iconY);
		graphics.drawString(font, this.getMessage(), startX + ICON_SIZE + ICON_GAP, this.getY() + (this.getHeight() - 8) / 2 + 1, color);
	}

	private void renderDiscordLogo(GuiGraphics graphics, int x, int y) {
		graphics.blit(DISCORD_LOGO, x, y, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
	}
}
