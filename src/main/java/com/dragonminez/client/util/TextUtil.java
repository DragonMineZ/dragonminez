package com.dragonminez.client.util;

import com.dragonminez.mixin.client.GuiGraphicsInvoker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Vector2i;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TextUtil {
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

    public static void renderAdvancedTooltip(GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY, int screenWidth, int screenHeight, float scrollY) {
        if (lines == null || lines.isEmpty()) return;

        List<Component> initialSplitLines = new ArrayList<>();
        for (Component c : lines) {
            String raw = c.getString();
            if (raw.contains("\n")) {
                String[] parts = raw.split("\n");
                for (String part : parts) {
                    initialSplitLines.add(Component.literal(part).setStyle(c.getStyle()));
                }
            } else {
                initialSplitLines.add(c);
            }
        }

        int allowedMaxWidth = Math.max(screenWidth / 2, 200);
        List<FormattedCharSequence> wrappedLines = new ArrayList<>();
        for (Component line : initialSplitLines) {
            wrappedLines.addAll(font.split(line, allowedMaxWidth));
        }

        List<ClientTooltipComponent> components = wrappedLines.stream()
                .map(ClientTooltipComponent::create)
                .collect(Collectors.toList());

        ClientTooltipPositioner positioner = (scrW, scrH, x, y, width, height) -> {
            if (height > scrH) return new Vector2i(x, 4);

            int modX = Mth.clamp(x - width / 2, 4, scrW - width - 4);
            int modY = y + 12;

            int belowObstruction = (modY + height) - scrH;
            if (belowObstruction > 0) {
                int aboveY = y - height - 4;
                int aboveObstruction = -aboveY;
                modY = (belowObstruction > aboveObstruction) ? Math.max(aboveY, 4) : modY;
            }

            return new Vector2i(modX, modY);
        };

        graphics.pose().pushPose();
        graphics.pose().translate(0, scrollY, 0);
        ((GuiGraphicsInvoker) graphics).invokeRenderTooltipInternal(font, components, mouseX, mouseY, positioner);
        graphics.pose().popPose();
    }

    public static void renderScrollableText(GuiGraphics graphics, Font font, List<String> lines, int x, int y, int width, int height, float currentScroll, float maxScroll, int color) {
        int lineHeight = font.lineHeight + 2;
        int viewHeight = height;
        int totalContentHeight = lines.size() * lineHeight;

        graphics.enableScissor(x, y, x + width, y + height);
        graphics.pose().pushPose();
        graphics.pose().translate(0, -currentScroll, 0);

        for (int i = 0; i < lines.size(); i++) {
            float lineY = y + (i * lineHeight);
            if (lineY + lineHeight >= y + currentScroll && lineY <= y + viewHeight + currentScroll) drawStringWithBorder(graphics, font, lines.get(i), x, (int)lineY, color);
        }

        graphics.pose().popPose();
        graphics.disableScissor();

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
