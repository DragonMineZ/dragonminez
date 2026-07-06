package com.dragonminez.client.util;

import com.dragonminez.client.gui.tooltip.CustomTooltipNodes;
import com.dragonminez.client.gui.tooltip.CustomTooltipRenderers;
import com.dragonminez.client.gui.tooltip.TooltipDecor;
import com.dragonminez.mixin.client.GuiGraphicsInvoker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Vector2i;

import java.util.*;

public class TextUtil {
    public static int width(Font font, String text, ResourceLocation fontId) {
        return width(font, text, Style.EMPTY.withFont(fontId));
    }

    public static List<String> wrap(Font font, String text, int maxWidth, ResourceLocation fontId) {
        return wrap(font, text, maxWidth, Style.EMPTY.withFont(fontId));
    }

    public static int width(Font font, String text, Style style) {
        return font.width(Component.literal(text).withStyle(style));
    }

    public static List<String> wrap(Font font, String text, int maxWidth, Style style) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            out.add("");
            return out;
        }
        for (FormattedCharSequence seq : font.split(Component.literal(text).withStyle(style), maxWidth)) {
            StringBuilder sb = new StringBuilder();
            seq.accept((index, st, codePoint) -> {
                sb.appendCodePoint(codePoint);
                return true;
            });
            out.add(sb.toString());
        }
        if (out.isEmpty()) out.add("");
        return out;
    }
    public static FormattedCharSequence overrideColor(FormattedCharSequence sequence, int color) {
        return sink -> sequence.accept((index, style, codePoint) ->
                sink.accept(index, style.withColor(net.minecraft.network.chat.TextColor.fromRgb(color)), codePoint)
        );
    }

    public static void drawStringWithBorder(GuiGraphics graphics, Font font, String text, int x, int y, int textColor, int borderColor) {
        graphics.drawString(font, text, x + 1, y, borderColor, false);
        graphics.drawString(font, text, x - 1, y, borderColor, false);
        graphics.drawString(font, text, x, y + 1, borderColor, false);
        graphics.drawString(font, text, x, y - 1, borderColor, false);
        graphics.drawString(font, text, x, y, textColor, false);
    }

    public static void drawStringWithBorder(GuiGraphics graphics, Font font, Component text, int x, int y, int textColor, int borderColor) {
        FormattedCharSequence sequence = text.getVisualOrderText();
        drawStringWithBorder(graphics, font, sequence, x, y, textColor, borderColor);
    }

    public static void drawStringWithBorder(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int textColor, int borderColor) {
        FormattedCharSequence borderSeq = overrideColor(text, borderColor);
        graphics.drawString(font, borderSeq, x + 1, y, borderColor, false);
        graphics.drawString(font, borderSeq, x - 1, y, borderColor, false);
        graphics.drawString(font, borderSeq, x, y + 1, borderColor, false);
        graphics.drawString(font, borderSeq, x, y - 1, borderColor, false);
        graphics.drawString(font, text, x, y, textColor, false);
    }

    public static void drawStringWithBorder(GuiGraphics graphics, Font font, String text, int x, int y, int textColor) {
        drawStringWithBorder(graphics, font, text, x, y, textColor, 0xFF000000);
    }

    public static void drawStringWithBorder(GuiGraphics graphics, Font font, Component text, int x, int y, int textColor) {
        drawStringWithBorder(graphics, font, text, x, y, textColor, 0xFF000000);
    }

    public static void drawStringWithBorder(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int textColor) {
        drawStringWithBorder(graphics, font, text, x, y, textColor, 0xFF000000);
    }

    public static void drawCenteredStringWithBorder(GuiGraphics graphics, Font font, String text, int centerX, int y, int textColor, int borderColor) {
        int x = centerX - (font.width(text) / 2);
        drawStringWithBorder(graphics, font, text, x, y, textColor, borderColor);
    }

    public static void drawCenteredStringWithBorder(GuiGraphics graphics, Font font, Component text, int centerX, int y, int textColor, int borderColor) {
        int x = centerX - (font.width(text) / 2);
        drawStringWithBorder(graphics, font, text, x, y, textColor, borderColor);
    }

    public static void drawCenteredStringWithBorder(GuiGraphics graphics, Font font, FormattedCharSequence text, int centerX, int y, int textColor, int borderColor) {
        int x = centerX - (font.width(text) / 2);
        drawStringWithBorder(graphics, font, text, x, y, textColor, borderColor);
    }

    public static void drawCenteredStringWithBorder(GuiGraphics graphics, Font font, String text, int centerX, int y, int textColor) {
        drawCenteredStringWithBorder(graphics, font, text, centerX, y, textColor, 0xFF000000);
    }

    public static void drawCenteredStringWithBorder(GuiGraphics graphics, Font font, Component text, int centerX, int y, int textColor) {
        drawCenteredStringWithBorder(graphics, font, text, centerX, y, textColor, 0xFF000000);
    }

    public static void drawCenteredStringWithBorder(GuiGraphics graphics, Font font, FormattedCharSequence text, int centerX, int y, int textColor) {
        drawCenteredStringWithBorder(graphics, font, text, centerX, y, textColor, 0xFF000000);
    }

    public static void renderAdvancedTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, int screenWidth, int screenHeight, Component title, List<Component> description, List<Component> extras, int color) {
        if (title == null && (description == null || description.isEmpty()) && (extras == null || extras.isEmpty())) return;

        List<ClientTooltipComponent> components = new ArrayList<>();
        int allowedMaxWidth = Math.max(screenWidth / 3, 200);

        if (title != null) {
            components.add(new CenteredTextComponent(title.getVisualOrderText()));
            components.add(new CustomTooltipRenderers.PaddingRenderer(new CustomTooltipNodes.PaddingNode(8)));
            components.add(new CustomTooltipRenderers.SeparatorRenderer(new CustomTooltipNodes.SeparatorNode()));
            components.add(new CustomTooltipRenderers.PaddingRenderer(new CustomTooltipNodes.PaddingNode(0)));
        }

        if (description != null && !description.isEmpty()) {
            for (Component c : description) {
                String raw = c.getString();
                if (raw.contains("\n")) {
                    for (String part : raw.split("\n")) {
                        for (FormattedCharSequence wrapped : font.split(Component.literal(part).setStyle(c.getStyle()), allowedMaxWidth)) {
                            components.add(new CenteredTextComponent(wrapped));
                        }
                    }
                } else {
                    for (FormattedCharSequence wrapped : font.split(c, allowedMaxWidth)) {
                        components.add(new CenteredTextComponent(wrapped));
                    }
                }
            }
        }

        if (extras != null && !extras.isEmpty()) {
            components.add(new CustomTooltipRenderers.PaddingRenderer(new CustomTooltipNodes.PaddingNode(12)));
            components.add(new CustomTooltipRenderers.SeparatorRenderer(new CustomTooltipNodes.SeparatorNode()));
            components.add(new CustomTooltipRenderers.PaddingRenderer(new CustomTooltipNodes.PaddingNode(0)));

            for (Component c : extras) {
                String raw = c.getString();
                if (raw.contains("\n")) {
                    for (String part : raw.split("\n")) {
                        for (FormattedCharSequence wrapped : font.split(Component.literal(part).setStyle(c.getStyle()), allowedMaxWidth)) {
                            components.add(new CenteredTextComponent(wrapped));
                        }
                    }
                } else {
                    for (FormattedCharSequence wrapped : font.split(c, allowedMaxWidth)) {
                        components.add(new CenteredTextComponent(wrapped));
                    }
                }
            }
        }

        ClientTooltipPositioner positioner = (ignoredW, ignoredH, x, y, width, height) -> {
            if (height > screenHeight) return new Vector2i(x, 4);

            int modX = Mth.clamp(x - width / 2, 4, screenWidth - width - 4);
            int modY = y + 12;

            int belowObstruction = (modY + height) - screenHeight;
            if (belowObstruction > 0) {
                int aboveY = y - height - 4;
                int aboveObstruction = -aboveY;
                modY = (belowObstruction > aboveObstruction) ? Math.max(aboveY, 4) : modY;
            }

            return new Vector2i(modX, modY);
        };

        TooltipDecor.forceCustomBorder = true;
        TooltipDecor.forcedColor = color;

        try {
            ((GuiGraphicsInvoker) graphics).invokeRenderTooltipInternal(font, components, mouseX, mouseY, positioner);
        } finally {
            TooltipDecor.forceCustomBorder = false;
        }
    }

    public static void renderScrollableText(GuiGraphics graphics, Font font, List<String> lines, int x, int y, int width, int height, float currentScroll, float maxScroll, int color) {
        renderScrollableText(graphics, font, lines, x, y, width, height, currentScroll, maxScroll, color, net.minecraft.network.chat.Style.EMPTY);
    }

    public static void renderScrollableText(GuiGraphics graphics, Font font, List<String> lines, int x, int y, int width, int height, float currentScroll, float maxScroll, int color, net.minecraft.network.chat.Style style) {
        int lineHeight = font.lineHeight + 2;
        int viewHeight = height;
        int totalContentHeight = lines.size() * lineHeight;

        graphics.pose().pushPose();
        graphics.pose().translate(0, -currentScroll, 0);

        for (int i = 0; i < lines.size(); i++) {
            float lineY = y + (i * lineHeight);
            if (lineY + lineHeight >= y + currentScroll && lineY <= y + viewHeight + currentScroll) drawStringWithBorder(graphics, font, Component.literal(lines.get(i)).setStyle(style), x, (int)lineY, color);
        }

        graphics.pose().popPose();

        if (maxScroll > 0) {
            int scrollBarX = x + width - 4;
            graphics.fill(scrollBarX, y, scrollBarX + 3, y + height, 0xFF333333);
            float scrollPercent = maxScroll == 0 ? 0.0f : currentScroll / maxScroll;
            int indicatorHeight = Math.max(10, (int) ((float) viewHeight / totalContentHeight * height));
            int indicatorY = y + (int) ((height - indicatorHeight) * scrollPercent);
            graphics.fill(scrollBarX, indicatorY, scrollBarX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
        }
    }
}