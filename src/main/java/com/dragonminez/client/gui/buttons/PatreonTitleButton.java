package com.dragonminez.client.gui.buttons;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PatreonTitleButton extends Button {
	private static final ResourceLocation PATREON_LOGO = new ResourceLocation("minecraft", "textures/gui/title/patreon_logo.png");

	public PatreonTitleButton(int x, int y, int width, int height, Component message, OnPress onPress) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
	}

	@Override
	public void renderString(GuiGraphics graphics, Font font, int color) {
		int textWidth = font.width(this.getMessage());
		int contentWidth = 12 + 4 + textWidth;
		int startX = this.getX() + Math.max(4, (this.getWidth() - contentWidth) / 2);
		int iconY = this.getY() + (this.getHeight() - 12) / 2;

		this.renderPatreonLogo(graphics, startX, iconY);
		graphics.drawString(font, this.getMessage(), startX + 12 + 4,
				this.getY() + (this.getHeight() - 8) / 2 + 1, color);
	}

	private void renderPatreonLogo(GuiGraphics graphics, int x, int y) {
		graphics.blit(PATREON_LOGO, x, y, 12, 12, 0.0F, 0.0F, 32, 32, 32, 32);
	}
}
