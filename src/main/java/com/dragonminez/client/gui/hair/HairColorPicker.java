package com.dragonminez.client.gui.hair;

import com.dragonminez.client.util.TextUtil;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.hair.HairColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

final class HairColorPicker {
	private static final int WIDTH = 136;
	private static final int HEIGHT = 132;
	private static final int PADDING = 9;
	private static final int FIELD_SIZE = 84;
	private static final int HUE_WIDTH = 12;
	private static final int DRAG_NONE = 0;
	private static final int DRAG_FIELD = 1;
	private static final int DRAG_HUE = 2;

	private boolean open;
	private int x;
	private int y;
	private float hue;
	private float saturation;
	private float value;
	private int dragMode = DRAG_NONE;
	private boolean editingHex;
	private final StringBuilder hexText = new StringBuilder();
	private Consumer<String> onChange;

	void open(int anchorX, int anchorY, int maxX, int maxY, String hex, String fallback, Consumer<String> listener) {
		open = true;
		x = Math.max(4, Math.min(anchorX, maxX - WIDTH - 4));
		y = Math.max(4, Math.min(anchorY, maxY - HEIGHT - 4));
		onChange = listener;
		String initial = hex != null ? hex : (fallback != null ? fallback : "#FFFFFF");
		float[] hsv = ColorUtils.hexToHsv(initial);
		hue = hsv[0];
		saturation = hsv[1];
		value = hsv[2];
		editingHex = false;
		hexText.setLength(0);
		hexText.append(currentHex());
	}

	boolean isOpen() {
		return open;
	}

	void close() {
		open = false;
		dragMode = DRAG_NONE;
		editingHex = false;
	}

	void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		if (!open) return;
		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, 0.0f, 450.0f);
		HairEditorUi.smallPanel(graphics, x, y, WIDTH, HEIGHT);

		int fieldX = x + PADDING;
		int fieldY = y + PADDING;
		for (int column = 0; column < FIELD_SIZE; column++) {
			int top = 0xFF000000 | ColorUtils.hexToInt(ColorUtils.hsvToHex(hue, column * 100.0f / (FIELD_SIZE - 1), 100.0f));
			graphics.fillGradient(fieldX + column, fieldY, fieldX + column + 1, fieldY + FIELD_SIZE, top, 0xFF000000);
		}
		HairEditorUi.outline(graphics, fieldX - 1, fieldY - 1, FIELD_SIZE + 2, FIELD_SIZE + 2, HairEditorUi.DIVIDER);
		int markerX = fieldX + Math.round(saturation / 100.0f * (FIELD_SIZE - 1));
		int markerY = fieldY + Math.round((1.0f - value / 100.0f) * (FIELD_SIZE - 1));
		HairEditorUi.outline(graphics, markerX - 2, markerY - 2, 5, 5, value > 50.0f ? 0xFF000000 : 0xFFFFFFFF);

		int hueX = fieldX + FIELD_SIZE + PADDING;
		for (int row = 0; row < FIELD_SIZE; row++) {
			int color = 0xFF000000 | ColorUtils.hexToInt(ColorUtils.hsvToHex(row * 360.0f / FIELD_SIZE, 100.0f, 100.0f));
			graphics.fill(hueX, fieldY + row, hueX + HUE_WIDTH, fieldY + row + 1, color);
		}
		HairEditorUi.outline(graphics, hueX - 1, fieldY - 1, HUE_WIDTH + 2, FIELD_SIZE + 2, HairEditorUi.DIVIDER);
		int hueMarker = fieldY + Math.round(hue / 360.0f * (FIELD_SIZE - 1));
		graphics.fill(hueX - 2, hueMarker, hueX + HUE_WIDTH + 2, hueMarker + 1, 0xFFFFFFFF);

		int swatchX = hueX + HUE_WIDTH + 4;
		graphics.fill(swatchX, fieldY, x + WIDTH - PADDING, fieldY + 18, 0xFF000000 | ColorUtils.hexToInt(currentHex()));
		HairEditorUi.outline(graphics, swatchX, fieldY, x + WIDTH - PADDING - swatchX, 18, HairEditorUi.DIVIDER);

		int rowY = fieldY + FIELD_SIZE + 6;
		int hexWidth = 56;
		HairEditorUi.field(graphics, fieldX, rowY, hexWidth, 14, editingHex);
		String hex = editingHex ? hexText + ((System.currentTimeMillis() / 400L) % 2L == 0L ? "_" : "") : currentHex();
		TextUtil.drawStringWithBorder(graphics, font, hex, fieldX + 3, rowY + 3, HairEditorUi.TEXT);

		int autoX = fieldX + hexWidth + 3;
		int buttonWidth = (x + WIDTH - PADDING - autoX - 3) / 2;
		HairEditorUi.button(graphics, font, HairEditorUi.tr("gui.dragonminez.hair_editor.color_auto"), autoX, rowY, buttonWidth, 14,
				HairEditorUi.inside(mouseX, mouseY, autoX, rowY, buttonWidth, 14), false, true);
		int okX = autoX + buttonWidth + 3;
		HairEditorUi.button(graphics, font, HairEditorUi.tr("gui.dragonminez.hair_editor.color_ok"), okX, rowY, buttonWidth, 14,
				HairEditorUi.inside(mouseX, mouseY, okX, rowY, buttonWidth, 14), false, true);
		graphics.pose().popPose();
	}

	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!open) return false;
		if (!HairEditorUi.inside(mouseX, mouseY, x, y, WIDTH, HEIGHT)) {
			commitHex();
			close();
			return true;
		}
		int fieldX = x + PADDING;
		int fieldY = y + PADDING;
		int hueX = fieldX + FIELD_SIZE + PADDING;
		int rowY = fieldY + FIELD_SIZE + 6;
		int hexWidth = 56;
		int autoX = fieldX + hexWidth + 3;
		int buttonWidth = (x + WIDTH - PADDING - autoX - 3) / 2;
		int okX = autoX + buttonWidth + 3;

		if (HairEditorUi.inside(mouseX, mouseY, fieldX, fieldY, FIELD_SIZE, FIELD_SIZE)) {
			commitHex();
			dragMode = DRAG_FIELD;
			updateField(mouseX, mouseY);
		} else if (HairEditorUi.inside(mouseX, mouseY, hueX, fieldY, HUE_WIDTH, FIELD_SIZE)) {
			commitHex();
			dragMode = DRAG_HUE;
			updateHue(mouseY);
		} else if (HairEditorUi.inside(mouseX, mouseY, fieldX, rowY, hexWidth, 14)) {
			editingHex = true;
			hexText.setLength(0);
			hexText.append(currentHex());
		} else if (HairEditorUi.inside(mouseX, mouseY, autoX, rowY, buttonWidth, 14)) {
			if (onChange != null) onChange.accept(null);
			HairEditorSounds.click();
			close();
		} else if (HairEditorUi.inside(mouseX, mouseY, okX, rowY, buttonWidth, 14)) {
			commitHex();
			HairEditorSounds.click();
			close();
		}
		return true;
	}

	boolean mouseDragged(double mouseX, double mouseY) {
		if (!open) return false;
		if (dragMode == DRAG_FIELD) updateField(mouseX, mouseY);
		else if (dragMode == DRAG_HUE) updateHue(mouseY);
		return true;
	}

	boolean mouseReleased() {
		if (!open) return false;
		dragMode = DRAG_NONE;
		return true;
	}

	boolean keyPressed(int keyCode) {
		if (!open) return false;
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			close();
			return true;
		}
		if (!editingHex) return keyCode != GLFW.GLFW_KEY_LEFT_SHIFT;
		if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !hexText.isEmpty()) hexText.setLength(hexText.length() - 1);
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) commitHex();
		return true;
	}

	boolean charTyped(char character) {
		if (!open || !editingHex) return open;
		if ((Character.digit(character, 16) >= 0 || character == '#') && hexText.length() < 7) hexText.append(character);
		return true;
	}

	private void commitHex() {
		if (!editingHex) return;
		editingHex = false;
		String normalized = HairColors.normalize(hexText.toString().startsWith("#") ? hexText.toString() : "#" + hexText);
		if (normalized == null) return;
		float[] hsv = ColorUtils.hexToHsv(normalized);
		hue = hsv[0];
		saturation = hsv[1];
		value = hsv[2];
		notifyChange();
	}

	private void updateField(double mouseX, double mouseY) {
		saturation = clamp((float) ((mouseX - (x + PADDING)) / (FIELD_SIZE - 1)) * 100.0f, 0.0f, 100.0f);
		value = clamp((1.0f - (float) ((mouseY - (y + PADDING)) / (FIELD_SIZE - 1))) * 100.0f, 0.0f, 100.0f);
		notifyChange();
	}

	private void updateHue(double mouseY) {
		hue = clamp((float) ((mouseY - (y + PADDING)) / (FIELD_SIZE - 1)) * 360.0f, 0.0f, 359.9f);
		notifyChange();
	}

	private void notifyChange() {
		if (onChange != null) onChange.accept(currentHex());
	}

	private String currentHex() {
		return ColorUtils.hsvToHex(hue, saturation, value);
	}

	private static float clamp(float input, float min, float max) {
		return Math.max(min, Math.min(max, input));
	}
}
