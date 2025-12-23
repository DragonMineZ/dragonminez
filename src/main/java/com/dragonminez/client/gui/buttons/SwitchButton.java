package com.dragonminez.client.gui.buttons;

import com.dragonminez.Reference;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SwitchButton extends Button {

    private boolean isActive;
    private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
            "textures/gui/buttons/characterbuttons.png");

    public SwitchButton(int x, int y, boolean active, Component message, OnPress onPress) {
        super(x, y, 20, 10, message, onPress, DEFAULT_NARRATION);
        this.isActive = active;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public void toggle() {
        this.isActive = !this.isActive;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int buttonX = 122;
        int buttonY = isActive ? 0 : 10;

        graphics.blit(BUTTONS_TEXTURE, this.getX(), this.getY(), buttonX, buttonY, 20, 10);
    }
}

