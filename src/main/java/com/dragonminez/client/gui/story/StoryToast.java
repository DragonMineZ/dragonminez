package com.dragonminez.client.gui.story;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;

public class StoryToast implements Toast {

	public enum Tone {
		INFO,
		PROGRESS,
		SUCCESS
	}

	private static final int WIDTH = 188;
	private static final int HEIGHT = 40;
	private static final long DURATION_MS = 5000L;

	private final Component title;
	private final Component description;
	private final Tone tone;

	public StoryToast(Component title, Component description, Tone tone) {
		this.title = title;
		this.description = description;
		this.tone = tone;
	}

	@Override
	public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long delta) {
		int backgroundColor = switch (tone) {
			case INFO -> 0xCC10212B;
			case PROGRESS -> 0xCC1D2338;
			case SUCCESS -> 0xCC173122;
		};
		int borderColor = switch (tone) {
			case INFO -> 0xFF58B3FF;
			case PROGRESS -> 0xFF7E8BFF;
			case SUCCESS -> 0xFF4CDB8D;
		};

		guiGraphics.fill(0, 0, width(), height(), backgroundColor);
		guiGraphics.fill(0, 0, width(), 2, borderColor);
		guiGraphics.fill(0, height() - 1, width(), height(), 0x66000000);

		int progressWidth = (int) ((1.0f - Math.min(delta / (float) DURATION_MS, 1.0f)) * (width() - 8));
		guiGraphics.fill(4, height() - 4, 4 + progressWidth, height() - 2, borderColor);

		guiGraphics.drawString(toastComponent.getMinecraft().font, title, 8, 7, 0xFFFFFF, false);
		guiGraphics.drawString(toastComponent.getMinecraft().font, description, 8, 20, 0xD8E6FF, false);

		return delta >= DURATION_MS ? Visibility.HIDE : Visibility.SHOW;
	}

	@Override
	public int width() {
		return WIDTH;
	}

	@Override
	public int height() {
		return HEIGHT;
	}
}

