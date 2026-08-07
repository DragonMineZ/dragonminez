package com.dragonminez.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Restores the custom-screen rendering behavior DMZ relied on in 1.20.1. */
public abstract class UnblurredScreen extends Screen {
	protected UnblurredScreen(Component title) {
		super(title);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		for (Renderable renderable : this.renderables) {
			renderable.render(graphics, mouseX, mouseY, partialTick);
		}
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// 1.20.1 used the transparent in-world shade here. 1.21 added a
		// post-process blur that also blurs DMZ's already-rendered pixel panels.
		this.renderTransparentBackground(graphics);
	}
}
