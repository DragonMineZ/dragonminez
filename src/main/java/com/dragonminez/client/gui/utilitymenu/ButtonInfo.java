package com.dragonminez.client.gui.utilitymenu;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

@Getter
public class ButtonInfo {
    protected Component line1 = Component.empty();
    protected Component line2 = Component.empty();
    @Setter
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

	public boolean hasContent() {
		return !line1.getString().isEmpty() || !line2.getString().isEmpty();
	}

}
