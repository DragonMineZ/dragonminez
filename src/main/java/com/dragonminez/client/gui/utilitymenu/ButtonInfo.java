package com.dragonminez.client.gui.utilitymenu;

import net.minecraft.network.chat.Component;

public class ButtonInfo {
    protected Component line1 = Component.empty();
    protected Component line2 = Component.empty();
    protected int color = 0xFFFFFF;
    protected boolean isSelected = false;

    public ButtonInfo() {
        // Default empty constructor
    }

    public ButtonInfo(Component line1, Component line2) {
        this.line1 = line1;
        this.line2 = line2;
    }

    public ButtonInfo(Component line1, Component line2, boolean isSelected) {
        this.line1 = line1;
        this.line2 = line2;
        this.isSelected = isSelected;
    }

    public Component getLine1() {
        return line1;
    }

    public Component getLine2() {
        return line2;
    }

    public int getColor() {
        return color;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
