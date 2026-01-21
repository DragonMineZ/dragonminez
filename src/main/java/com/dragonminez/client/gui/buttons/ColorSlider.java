package com.dragonminez.client.gui.buttons;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ColorSlider extends AbstractSliderButton {

    private final int minValue;
    private final int maxValue;
    private final Consumer<Integer> onValueChange;
    private final ResourceLocation texture;
    private final int sliderU;
    private final int sliderV;
    private final int sliderWidth;
    private final int sliderHeight;
    private float currentHue = 0;
    private float currentSaturation = 100;

    public ColorSlider(int x, int y, int width, int height,
                       int minValue, int maxValue, int currentValue,
                       ResourceLocation texture, int sliderU, int sliderV,
                       int sliderWidth, int sliderHeight,
                       Component message, Consumer<Integer> onValueChange) {
        super(x, y, width, height, message, (double)(currentValue - minValue) / (maxValue - minValue));
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.onValueChange = onValueChange;
        this.texture = texture;
        this.sliderU = sliderU;
        this.sliderV = sliderV;
        this.sliderWidth = sliderWidth;
        this.sliderHeight = sliderHeight;
        this.updateMessage();
    }

    public ColorSlider(int x, int y, int width, int height,
                       int minValue, int maxValue, int currentValue,
                       Component message, Consumer<Integer> onValueChange) {
        this(x, y, width, height, minValue, maxValue, currentValue,
             null, 0, 0, 0, 0, message, onValueChange);
    }

    @Override
    protected void updateMessage() {}

    @Override
    protected void applyValue() {
        if (onValueChange != null) {
            onValueChange.accept(getValue());
        }
    }

    public int getValue() {
        return (int) Math.round(minValue + (maxValue - minValue) * this.value);
    }

    public void setValue(int value) {
        this.value = (double)(value - minValue) / (maxValue - minValue);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, 0xFF000000);

        drawGradientBackground(graphics);

        int sliderX = this.getX() + (int)(this.value * (double)(this.width - 6));
        graphics.fill(sliderX, this.getY() - 1, sliderX + 6, this.getY() + this.height + 1, 0xFFFFFFFF);
        graphics.fill(sliderX + 1, this.getY(), sliderX + 5, this.getY() + this.height, 0xFF808080);
    }

    private void drawGradientBackground(GuiGraphics graphics) {
        Component msg = this.getMessage();
        String messageText = msg.getString();

        if (messageText.equals("Hue") || messageText.equals("H")) {
            drawHueGradient(graphics);
        } else if (messageText.equals("Saturation") || messageText.equals("S")) {
            drawSaturationGradient(graphics);
        } else if (messageText.equals("Value") || messageText.equals("V")) {
            drawValueGradient(graphics);
        } else {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF808080);
        }
    }

    private void drawHueGradient(GuiGraphics graphics) {
        int segments = this.width;
        for (int i = 0; i < segments; i++) {
            float hue = (float)i / segments * 360.0F;
            int color = hsvToRgb(hue, 100, 100);
            graphics.fill(this.getX() + i, this.getY(), this.getX() + i + 1, this.getY() + this.height, 0xFF000000 | color);
        }
    }

    private void drawSaturationGradient(GuiGraphics graphics) {
        int segments = this.width;
        for (int i = 0; i < segments; i++) {
            float saturation = 100.0F - ((float)i / segments * 100.0F);
            int color = hsvToRgb(currentHue, saturation, 100);
            graphics.fill(this.getX() + i, this.getY(), this.getX() + i + 1, this.getY() + this.height, 0xFF000000 | color);
        }
    }

    private void drawValueGradient(GuiGraphics graphics) {
        int segments = this.width;
        for (int i = 0; i < segments; i++) {
            float value = 100.0F - ((float)i / segments * 100.0F);
            int color = hsvToRgb(currentHue, currentSaturation, value);
            graphics.fill(this.getX() + i, this.getY(), this.getX() + i + 1, this.getY() + this.height, 0xFF000000 | color);
        }
    }

    private int hsvToRgb(float h, float s, float v) {
        s = s / 100.0F;
        v = v / 100.0F;

        float c = v * s;
        float x = c * (1 - Math.abs((h / 60.0F) % 2 - 1));
        float m = v - c;

        float r, g, b;
        if (h < 60) {
            r = c; g = x; b = 0;
        } else if (h < 120) {
            r = x; g = c; b = 0;
        } else if (h < 180) {
            r = 0; g = c; b = x;
        } else if (h < 240) {
            r = 0; g = x; b = c;
        } else if (h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }

        int ri = (int)((r + m) * 255);
        int gi = (int)((g + m) * 255);
        int bi = (int)((b + m) * 255);

        return (ri << 16) | (gi << 8) | bi;
    }

    public void setCurrentHue(float hue) {
        this.currentHue = hue;
    }

    public void setCurrentSaturation(float saturation) {
        this.currentSaturation = saturation;
    }

    public static class Builder {
        private int x, y, width, height;
        private int minValue = 0;
        private int maxValue = 255;
        private int currentValue = 0;
        private ResourceLocation texture = null;
        private int sliderU = 0;
        private int sliderV = 0;
        private int sliderWidth = 0;
        private int sliderHeight = 0;
        private Component message = Component.empty();
        private Consumer<Integer> onValueChange;

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder range(int min, int max) {
            this.minValue = min;
            this.maxValue = max;
            return this;
        }

        public Builder value(int value) {
            this.currentValue = value;
            return this;
        }

        public Builder texture(ResourceLocation texture, int u, int v, int width, int height) {
            this.texture = texture;
            this.sliderU = u;
            this.sliderV = v;
            this.sliderWidth = width;
            this.sliderHeight = height;
            return this;
        }

        public Builder message(Component message) {
            this.message = message;
            return this;
        }

        public Builder onValueChange(Consumer<Integer> onValueChange) {
            this.onValueChange = onValueChange;
            return this;
        }

        public ColorSlider build() {
            if (texture != null) {
                return new ColorSlider(x, y, width, height, minValue, maxValue, currentValue,
                        texture, sliderU, sliderV, sliderWidth, sliderHeight, message, onValueChange);
            } else {
                return new ColorSlider(x, y, width, height, minValue, maxValue, currentValue,
                        message, onValueChange);
            }
        }
    }
}

