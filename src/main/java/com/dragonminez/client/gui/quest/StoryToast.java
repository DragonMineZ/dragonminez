package com.dragonminez.client.gui.quest;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class StoryToast implements Toast {

	public enum Tone {
		INFO,
		PROGRESS,
		FAILURE,
		SUCCESS
	}

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
			case FAILURE -> 0xCC321717;
			case SUCCESS -> 0xCC173122;
		};
		int borderColor = switch (tone) {
			case INFO -> 0xFF58B3FF;
			case PROGRESS -> 0xFF7E8BFF;
			case FAILURE -> 0xFFFF6D6D;
			case SUCCESS -> 0xFF4CDB8D;
		};

		Font font = toastComponent.getMinecraft().font;
		guiGraphics.fill(0, 0, width(), height(), backgroundColor);
		guiGraphics.fill(0, 0, width(), 2, borderColor);
		guiGraphics.fill(0, height() - 1, width(), height(), 0x66000000);

		int progressWidth = (int) ((1.0f - Math.min(delta / (float) DURATION_MS, 1.0f)) * (width() - 8));
		guiGraphics.fill(4, height() - 4, 4 + progressWidth, height() - 2, borderColor);

		int textWidth = width() - (16);
		List<FormattedCharSequence> titleLines = font.split(title, textWidth);
		List<FormattedCharSequence> descriptionLines = font.split(description, textWidth);

		int textY = 6;
		int titleHeight = drawWrappedLines(guiGraphics, font, titleLines, 8, textY, 0xFFFFFF, 2);
		textY += titleHeight;
		if (titleHeight > 0 && !descriptionLines.isEmpty()) textY += 2;
		drawWrappedLines(guiGraphics, font, descriptionLines, 8, textY, 0xD8E6FF, 4);

		return delta >= DURATION_MS ? Visibility.HIDE : Visibility.SHOW;
	}

	private static int drawWrappedLines(
			GuiGraphics guiGraphics,
			Font font,
			List<FormattedCharSequence> lines,
			int x,
			int startY,
			int color,
			int maxLines
	) {
		int count = Math.min(maxLines, lines.size());
		for (int i = 0; i < count; i++) {
			guiGraphics.drawString(font, lines.get(i), x, startY + (i * font.lineHeight), color, false);
		}
		return count * font.lineHeight;
	}

	@Override
	public int width() {
		return 220;
	}

	@Override
	public int height() {
		return 52;
	}
}

