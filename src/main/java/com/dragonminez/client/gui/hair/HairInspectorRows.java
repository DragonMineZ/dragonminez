package com.dragonminez.client.gui.hair;

import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.hair.HairColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

final class HairInspectorRows {
	static final int ROW_HEIGHT = 13;
	static final int SECTION_HEIGHT = 15;
	static final int LABEL_WIDTH = 62;
	static final int VALUE_WIDTH = 38;
	static final float SCRUB_PIXELS = 240.0f;

	private HairInspectorRows() {}

	interface Context {
		HairEditorState state();

		void beginTextEdit(NumberRow row);

		boolean isEditing(NumberRow row);

		String editText();

		void openColorPicker(ColorRow row, int anchorX, int anchorY);

		void requestRebuild();
	}

	abstract static class Row {
		int height() {
			return ROW_HEIGHT;
		}

		abstract void render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, Context context);

		boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width, Context context) {
			return false;
		}

		void mouseDragged(double mouseX, double mouseY, int x, int y, int width, Context context) {}

		void mouseReleased(Context context) {}
	}

	static final class SectionRow extends Row {
		private final Component label;

		SectionRow(Component label) {
			this.label = label;
		}

		int height() {
			return SECTION_HEIGHT;
		}

		void render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, Context context) {
			TextUtil.drawStringWithBorder(graphics, font, label, x + 4, y + 5, HairEditorUi.SECTION);
			int lineStart = x + 8 + font.width(label);
			HairEditorUi.divider(graphics, lineStart, x + width - 4, y + 9);
		}
	}

	static final class InfoRow extends Row {
		private final Supplier<Component> text;
		private final int color;

		InfoRow(Supplier<Component> text, int color) {
			this.text = text;
			this.color = color;
		}

		void render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, Context context) {
			String value = HairEditorUi.trimToWidth(font, text.get().getString(), width - 8);
			TextUtil.drawStringWithBorder(graphics, font, HairEditorUi.txt(value), x + 4, y + 3, color);
		}
	}

	static final class NumberRow extends Row {
		private static final int DRAG_NONE = 0;
		private static final int DRAG_BAR = 1;
		private static final int DRAG_SCRUB = 2;
		private static final int APPLY_RESET = 0;
		private static final int APPLY_DRAG = 1;
		private static final int APPLY_COMMIT = 2;

		private final Component label;
		private final int axisColor;
		private final float min;
		private final float max;
		private final float step;
		private final boolean integer;
		private final DoubleSupplier getter;
		private final Consumer<Float> setter;
		private final float defaultValue;
		private int dragMode = DRAG_NONE;
		private double dragStartX;
		private float dragStartValue;

		NumberRow(Component label, int axisColor, float min, float max, float step, boolean integer, DoubleSupplier getter, Consumer<Float> setter, float defaultValue) {
			this.label = label;
			this.axisColor = axisColor;
			this.min = min;
			this.max = max;
			this.step = step;
			this.integer = integer;
			this.getter = getter;
			this.setter = setter;
			this.defaultValue = defaultValue;
		}

		void render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, Context context) {
			boolean hovered = HairEditorUi.inside(mouseX, mouseY, x, y, width, ROW_HEIGHT);
			if (hovered || dragMode != DRAG_NONE) graphics.fill(x + 1, y, x + width - 1, y + ROW_HEIGHT, HairEditorUi.ROW_HOVER);
			if (axisColor != 0) graphics.fill(x + 2, y + 2, x + 4, y + ROW_HEIGHT - 2, axisColor);

			String labelText = HairEditorUi.trimToWidth(font, label.getString(), LABEL_WIDTH - 8);
			TextUtil.drawStringWithBorder(graphics, font, HairEditorUi.txt(labelText), x + 6, y + 3, dragMode == DRAG_SCRUB ? HairEditorUi.TITLE : HairEditorUi.MUTED);

			float value = (float) getter.getAsDouble();
			int barX = barX(x);
			int barWidth = barWidth(width);
			HairEditorUi.track(graphics, barX, y + 2, barWidth, ROW_HEIGHT - 4);
			float ratio = max > min ? (Math.max(min, Math.min(max, value)) - min) / (max - min) : 0.0f;
			if (min < 0.0f && max > 0.0f) {
				int zero = barX + Math.round((-min) / (max - min) * barWidth);
				int current = barX + Math.round(ratio * barWidth);
				HairEditorUi.trackFill(graphics, Math.min(zero, current), y + 3, Math.abs(current - zero), ROW_HEIGHT - 6);
				graphics.fill(zero, y + 2, zero + 1, y + ROW_HEIGHT - 2, HairEditorUi.DIVIDER);
			} else {
				HairEditorUi.trackFill(graphics, barX, y + 3, Math.round(ratio * barWidth), ROW_HEIGHT - 6);
			}
			int handle = barX + Math.round(ratio * (barWidth - 2));
			graphics.fill(handle, y + 2, handle + 2, y + ROW_HEIGHT - 2, HairEditorUi.TEXT);

			int valueX = x + width - VALUE_WIDTH - 3;
			boolean editing = context.isEditing(this);
			HairEditorUi.field(graphics, valueX, y, VALUE_WIDTH, ROW_HEIGHT, editing);
			String text = editing ? context.editText() + ((System.currentTimeMillis() / 400L) % 2L == 0L ? "_" : "") : format(value);
			text = HairEditorUi.trimToWidth(font, text, VALUE_WIDTH - 4);
			TextUtil.drawStringWithBorder(graphics, font, text, valueX + VALUE_WIDTH - 3 - font.width(text), y + 3, HairEditorUi.TEXT);
		}

		boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width, Context context) {
			if (!HairEditorUi.inside(mouseX, mouseY, x, y, width, ROW_HEIGHT)) return false;
			if (button == 1) {
				context.state().recordStep();
				apply(defaultValue, APPLY_RESET);
				HairEditorSounds.click();
				return true;
			}
			if (button != 0) return false;

			int valueX = x + width - VALUE_WIDTH - 3;
			if (mouseX >= valueX) {
				context.beginTextEdit(this);
				return true;
			}
			context.state().beginGesture();
			if (mouseX >= barX(x)) {
				dragMode = DRAG_BAR;
				applyFromBar(mouseX, x, width);
			} else {
				dragMode = DRAG_SCRUB;
				dragStartX = mouseX;
				dragStartValue = (float) getter.getAsDouble();
			}
			return true;
		}

		void mouseDragged(double mouseX, double mouseY, int x, int y, int width, Context context) {
			if (dragMode == DRAG_BAR) {
				applyFromBar(mouseX, x, width);
			} else if (dragMode == DRAG_SCRUB) {
				float sensitivity = Screen.hasShiftDown() ? 0.1f : (Screen.hasControlDown() ? 5.0f : 1.0f);
				float delta = (float) (mouseX - dragStartX) * (max - min) / SCRUB_PIXELS * sensitivity;
				apply(dragStartValue + delta, APPLY_DRAG);
			}
		}

		void mouseReleased(Context context) {
			if (dragMode != DRAG_NONE) context.state().endGesture();
			dragMode = DRAG_NONE;
		}

		String format(float value) {
			if (integer) return Integer.toString(Math.round(value));
			if (step >= 1.0f) return String.format(Locale.ROOT, "%.0f", value);
			if (step >= 0.1f) return String.format(Locale.ROOT, "%.1f", value);
			return String.format(Locale.ROOT, "%.2f", value);
		}

		String currentText() {
			return format((float) getter.getAsDouble());
		}

		void commitText(String text, Context context) {
			try {
				float parsed = Float.parseFloat(text.trim().replace(',', '.'));
				context.state().recordStep();
				apply(parsed, APPLY_COMMIT);
			} catch (NumberFormatException ignored) {
				context.requestRebuild();
			}
		}

		private void applyFromBar(double mouseX, int x, int width) {
			float ratio = (float) ((mouseX - barX(x)) / Math.max(1, barWidth(width)));
			apply(min + Math.max(0.0f, Math.min(1.0f, ratio)) * (max - min), APPLY_DRAG);
		}

		private void apply(float value, int mode) {
			if (!Float.isFinite(value)) return;
			float before = (float) getter.getAsDouble();
			float clamped = Math.max(min, Math.min(max, value));
			if (step > 0.0f && !Screen.hasShiftDown()) clamped = Math.round(clamped / step) * step;
			if (integer) clamped = Math.round(clamped);
			setter.accept(Math.max(min, Math.min(max, clamped)));
			if (mode == APPLY_RESET) return;
			boolean atLimit = isAtLimit((float) getter.getAsDouble());
			boolean pushedPast = mode == APPLY_COMMIT && (value < min || value > max);
			if (atLimit && (pushedPast || !isAtLimit(before))) HairEditorSounds.limit();
		}

		private boolean isAtLimit(float value) {
			float tolerance = integer ? 0.5f : Math.max(1.0e-3f, (max - min) * 1.0e-4f);
			return Math.abs(value - min) <= tolerance || Math.abs(value - max) <= tolerance;
		}

		private static int barX(int x) {
			return x + LABEL_WIDTH;
		}

		private static int barWidth(int width) {
			return Math.max(10, width - LABEL_WIDTH - VALUE_WIDTH - 8);
		}
	}

	static final class ChoiceRow extends Row {
		private final Component label;
		private final Supplier<Component> value;
		private final Runnable next;
		private final Runnable reset;

		ChoiceRow(Component label, Supplier<Component> value, Runnable next, Runnable reset) {
			this.label = label;
			this.value = value;
			this.next = next;
			this.reset = reset;
		}

		void render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, Context context) {
			TextUtil.drawStringWithBorder(graphics, font, HairEditorUi.txt(HairEditorUi.trimToWidth(font, label.getString(), LABEL_WIDTH - 8)), x + 6, y + 3, HairEditorUi.MUTED);
			int buttonX = x + LABEL_WIDTH;
			int buttonWidth = width - LABEL_WIDTH - 3;
			boolean hovered = HairEditorUi.inside(mouseX, mouseY, buttonX, y + 1, buttonWidth, ROW_HEIGHT - 2);
			HairEditorUi.button(graphics, font, value.get(), buttonX, y + 1, buttonWidth, ROW_HEIGHT - 2, hovered, false, true);
		}

		boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width, Context context) {
			if (!HairEditorUi.inside(mouseX, mouseY, x, y, width, ROW_HEIGHT)) return false;
			context.state().recordStep();
			if (button == 1 && reset != null) reset.run();
			else if (button == 0) next.run();
			HairEditorSounds.click();
			context.requestRebuild();
			return true;
		}
	}

	static final class ColorRow extends Row {
		private final Component label;
		private final Supplier<String> getter;
		private final Supplier<String> fallback;
		private final Consumer<String> setter;

		ColorRow(Component label, Supplier<String> getter, Supplier<String> fallback, Consumer<String> setter) {
			this.label = label;
			this.getter = getter;
			this.fallback = fallback;
			this.setter = setter;
		}

		String currentHex() {
			return getter.get();
		}

		String fallbackHex() {
			return fallback.get();
		}

		void set(String hex) {
			setter.accept(hex);
		}

		void render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, Context context) {
			boolean hovered = HairEditorUi.inside(mouseX, mouseY, x, y, width, ROW_HEIGHT);
			if (hovered) graphics.fill(x + 1, y, x + width - 1, y + ROW_HEIGHT, HairEditorUi.ROW_HOVER);
			TextUtil.drawStringWithBorder(graphics, font, HairEditorUi.txt(HairEditorUi.trimToWidth(font, label.getString(), LABEL_WIDTH - 8)), x + 6, y + 3, HairEditorUi.MUTED);
			String hex = getter.get();
			String shown = hex != null ? hex : fallback.get();
			int rgb = HairColors.parse(shown);
			int swatchX = x + LABEL_WIDTH;
			graphics.fill(swatchX, y + 2, swatchX + 18, y + ROW_HEIGHT - 2, 0xFF000000 | (rgb == HairColors.INHERIT ? 0xFFFFFF : rgb));
			HairEditorUi.outline(graphics, swatchX, y + 2, 18, ROW_HEIGHT - 4, HairEditorUi.DIVIDER);
			Component text = hex != null ? HairEditorUi.txt(hex) : HairEditorUi.tr("gui.dragonminez.hair_editor.color_auto");
			TextUtil.drawStringWithBorder(graphics, font, text, swatchX + 23, y + 3, hex != null ? HairEditorUi.TEXT : HairEditorUi.MUTED);
		}

		boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width, Context context) {
			if (!HairEditorUi.inside(mouseX, mouseY, x, y, width, ROW_HEIGHT)) return false;
			context.state().recordStep();
			if (button == 1) {
				setter.accept(null);
				HairEditorSounds.click();
				return true;
			}
			if (button == 0) {
				context.openColorPicker(this, x + LABEL_WIDTH, y + ROW_HEIGHT);
				HairEditorSounds.click();
			}
			return true;
		}
	}

	record Action(Component label, Runnable action, BooleanSupplier enabled) {}

	static final class ActionRow extends Row {
		private static final int GAP = 3;
		private final List<Action> actions;

		ActionRow(List<Action> actions) {
			this.actions = actions;
		}

		int height() {
			return ROW_HEIGHT + 3;
		}

		void render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, Context context) {
			int buttonWidth = buttonWidth(width);
			for (int i = 0; i < actions.size(); i++) {
				Action action = actions.get(i);
				int buttonX = x + 4 + i * (buttonWidth + GAP);
				boolean enabled = action.enabled() == null || action.enabled().getAsBoolean();
				boolean hovered = HairEditorUi.inside(mouseX, mouseY, buttonX, y + 1, buttonWidth, ROW_HEIGHT);
				Component label = HairEditorUi.txt(HairEditorUi.trimToWidth(font, action.label().getString(), buttonWidth - 4));
				HairEditorUi.button(graphics, font, label, buttonX, y + 1, buttonWidth, ROW_HEIGHT, hovered, false, enabled);
			}
		}

		boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width, Context context) {
			if (button != 0) return false;
			int buttonWidth = buttonWidth(width);
			for (int i = 0; i < actions.size(); i++) {
				Action action = actions.get(i);
				int buttonX = x + 4 + i * (buttonWidth + GAP);
				if (!HairEditorUi.inside(mouseX, mouseY, buttonX, y + 1, buttonWidth, ROW_HEIGHT)) continue;
				if (action.enabled() != null && !action.enabled().getAsBoolean()) return true;
				action.action().run();
				HairEditorSounds.click();
				context.requestRebuild();
				return true;
			}
			return false;
		}

		private int buttonWidth(int width) {
			return (width - 8 - GAP * (actions.size() - 1)) / Math.max(1, actions.size());
		}
	}
}
