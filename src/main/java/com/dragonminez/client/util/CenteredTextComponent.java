package com.dragonminez.client.util;

import com.dragonminez.client.gui.tooltip.TooltipDecor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

public class CenteredTextComponent implements ClientTooltipComponent {
	private final ClientTooltipComponent original;

	public CenteredTextComponent(FormattedCharSequence text) {
		this.original = ClientTooltipComponent.create(text);
	}

	@Override
	public int getWidth(Font font) {
		return this.original.getWidth(font);
	}

	@Override
	public int getHeight() {
		return this.original.getHeight();
	}

	@Override
	public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {
		int finalX = x;

		if (TooltipDecor.hasSpecialBorder && !TooltipDecor.hasItemBox && y <= TooltipDecor.lastTooltipY + 12) {
			int textW = this.getWidth(font);
			finalX = TooltipDecor.lastTooltipX + (TooltipDecor.lastTooltipW / 2) - (textW / 2);
		}

		this.original.renderText(font, finalX, y, matrix, bufferSource);
	}
}