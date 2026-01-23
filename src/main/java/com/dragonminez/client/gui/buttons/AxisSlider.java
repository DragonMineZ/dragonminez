package com.dragonminez.client.gui.buttons;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class AxisSlider extends AbstractSliderButton {

    public enum Axis {
        X(0xFFFF5555), Y(0xFF55FF55), Z(0xFF5555FF);
        private final int color;

        Axis(int color) { this.color = color; }

        public int getColor() { return color; }
    }

    private final float minValue;
    private final float maxValue;
    private final Consumer<Float> onValueChange;
    private final Axis axis;

    public AxisSlider(int x, int y, int width, int height, float minValue, float maxValue, float currentValue, Axis axis, Consumer<Float> onValueChange) {
        super(x, y, width, height, Component.empty(), (double)(currentValue - minValue) / (maxValue - minValue));
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.onValueChange = onValueChange;
        this.axis = axis;
    }

    @Override
    protected void updateMessage() {}

    @Override
    protected void applyValue() {
        if (onValueChange != null) {
            onValueChange.accept(getValue());
        }
    }

    public float getValue() {
        return (float)(minValue + (maxValue - minValue) * this.value);
    }

    public void setValue(float value) {
        this.value = (double)(value - minValue) / (maxValue - minValue);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, 0xFF000000);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, axis.getColor());

        int sliderX = this.getX() + (int)(this.value * (double)(this.width - 6));
        graphics.fill(sliderX, this.getY() - 1, sliderX + 6, this.getY() + this.height + 1, 0xFFFFFFFF);
        graphics.fill(sliderX + 1, this.getY(), sliderX + 5, this.getY() + this.height, 0xFF303030);
    }

    public static class Builder {
        private int x, y, width, height;
        private float minValue = -180f;
        private float maxValue = 180f;
        private float currentValue = 0f;
        private Axis axis = Axis.X;
        private Consumer<Float> onValueChange;

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

        public Builder range(float min, float max) {
            this.minValue = min;
            this.maxValue = max;
            return this;
        }

        public Builder value(float value) {
            this.currentValue = value;
            return this;
        }

        public Builder axis(Axis axis) {
            this.axis = axis;
            return this;
        }

        public Builder onValueChange(Consumer<Float> onValueChange) {
            this.onValueChange = onValueChange;
            return this;
        }

        public AxisSlider build() {
            return new AxisSlider(x, y, width, height, minValue, maxValue, currentValue, axis, onValueChange);
        }
    }
}
