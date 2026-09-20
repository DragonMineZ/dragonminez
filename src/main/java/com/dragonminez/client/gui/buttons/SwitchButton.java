package com.dragonminez.client.gui.buttons;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.HudRender;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SwitchButton extends Button implements SubpixelWidget {

    private boolean isActive;
    private float subpixelX;
    private float subpixelY;
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

        HudRender.blit(graphics, BUTTONS_TEXTURE, this.getX() + subpixelX, this.getY() + subpixelY, buttonX, buttonY, 20, 10, 256, 256);
    }

    @Override
    public void setSubpixelX(float offset) {
        this.subpixelX = offset;
    }

    @Override
    public void setSubpixelY(float offset) {
        this.subpixelY = offset;
    }
}

