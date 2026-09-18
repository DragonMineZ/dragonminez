package com.dragonminez.client.gui.hair;

import com.dragonminez.Reference;
import com.dragonminez.client.util.TextUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

final class HairEditorUi {
	static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	static final int TITLE = 0xFFFFD700;
	static final int TEXT = 0xFFFFFFFF;
	static final int HOVER_TEXT = 0xFF7CFDD6;
	static final int MUTED = 0xFFB9D6B4;
	static final int DISABLED = 0xFF6E7F6E;
	static final int WARNING = 0xFFFF5555;
	static final int SUCCESS = 0xFF55FF55;
	static final int ROW_HOVER = 0x30FFFFFF;
	static final int DIVIDER = 0xFF8D9BC6;
	static final int AXIS_X = 0xFFE05252;
	static final int AXIS_Y = 0xFF52C45A;
	static final int AXIS_Z = 0xFF5272E0;
	static final int SECTION = 0xFFFFC04D;

	private HairEditorUi() {}

	static MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	static MutableComponent txt(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
		HairEditorTextures.draw(graphics, HairEditorTextures.PANEL, x, y, width, height);
	}

	static void smallPanel(GuiGraphics graphics, int x, int y, int width, int height) {
		HairEditorTextures.draw(graphics, HairEditorTextures.SMALL_PANEL, x, y, width, height);
	}

	static void header(GuiGraphics graphics, Font font, Component title, int x, int y, int width) {
		int plateWidth = Math.min(HairEditorTextures.TITLE_PLATE.width(), width - 16);
		HairEditorTextures.draw(graphics, HairEditorTextures.TITLE_PLATE, x + (width - plateWidth) / 2, y + 5, plateWidth, 17);
		TextUtil.drawCenteredStringWithBorder(graphics, font, title, x + width / 2, y + 10, TITLE);
	}

	static void button(GuiGraphics graphics, Font font, Component label, int x, int y, int width, int height, boolean hovered, boolean active, boolean enabled) {
		HairEditorTextures.Sprite sprite = active ? HairEditorTextures.BUTTON_ACTIVE : (hovered && enabled ? HairEditorTextures.BUTTON_HOVER : HairEditorTextures.BUTTON);
		float tint = enabled ? 1.0f : 0.6f;
		HairEditorTextures.draw(graphics, sprite, x, y, width, height, tint, tint, tint, 1.0f);
		int color = !enabled ? DISABLED : (hovered && !active ? HOVER_TEXT : TEXT);
		TextUtil.drawStringWithBorder(graphics, font, label, x + (width - font.width(label)) / 2, y + (height - 8) / 2 + 1, color);
	}

	static void field(GuiGraphics graphics, int x, int y, int width, int height, boolean editing) {
		HairEditorTextures.draw(graphics, editing ? HairEditorTextures.FIELD_EDITING : HairEditorTextures.FIELD, x, y, width, height);
	}

	static void track(GuiGraphics graphics, int x, int y, int width, int height) {
		HairEditorTextures.draw(graphics, HairEditorTextures.BUTTON, x, y, width, height);
	}

	static void trackFill(GuiGraphics graphics, int x, int y, int width, int height) {
		if (width <= 0) return;
		HairEditorTextures.draw(graphics, HairEditorTextures.BUTTON_ACTIVE, x, y, width, height);
	}

	static void divider(GuiGraphics graphics, int x0, int x1, int y) {
		if (x1 > x0) graphics.fill(x0, y, x1, y + 1, DIVIDER);
	}

	static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y, x + 1, y + height, color);
		graphics.fill(x + width - 1, y, x + width, y + height, color);
	}

	static String trimToWidth(Font font, String text, int width) {
		if (font.width(text) <= width) return text;
		String ellipsis = "..";
		return font.plainSubstrByWidth(text, Math.max(0, width - font.width(ellipsis))) + ellipsis;
	}
}
