package com.dragonminez.client.gui.tooltip;

import org.joml.Vector2i;
import org.joml.Vector2ic;
import java.util.Optional;

public class PrioritizeTooltipTopPositionModule implements TooltipPositionModule {
	@Override
	public Optional<Vector2ic> repositionTooltip(int x, int y, int width, int height, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		if (height <= screenHeight) return Optional.empty();
		return Optional.of(new Vector2i(x, 4));
	}
}