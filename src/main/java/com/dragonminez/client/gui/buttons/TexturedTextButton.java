package com.dragonminez.client.gui.buttons;

import com.dragonminez.common.init.MainSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TexturedTextButton extends Button {

    private final ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;
    private final int normalU;
    private final int normalV;
    private final int hoverU;
    private final int hoverV;
    private final int normalTextColor;
    private final int hoverTextColor;
    private final int backgroundColor;
    private final SoundEvent sound;

    public TexturedTextButton(int x, int y, int width, int height, ResourceLocation texture,
                              int normalU, int normalV, int hoverU, int hoverV,
                              int textureWidth, int textureHeight,
                              int normalTextColor, int hoverTextColor,
                              int backgroundColor,
                              Component message, OnPress onPress, SoundEvent sound) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.normalU = normalU;
        this.normalV = normalV;
        this.hoverU = hoverU;
        this.hoverV = hoverV;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.normalTextColor = normalTextColor;
        this.hoverTextColor = hoverTextColor;
        this.backgroundColor = backgroundColor;
        this.sound = sound;
    }

    @Override
    public void playDownSound(SoundManager handler) {
        if (this.sound != null) {
            handler.play(SimpleSoundInstance.forUI(this.sound, 1.0F));
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft minecraft = Minecraft.getInstance();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        int u = this.isHoveredOrFocused() ? hoverU : normalU;
        int v = this.isHoveredOrFocused() ? hoverV : normalV;

        graphics.blit(texture, this.getX(), this.getY(), u, v, textureWidth, textureHeight);

        if (backgroundColor != 0) {
            graphics.fill(this.getX() + 2, this.getY() + 2,
                         this.getX() + this.width - 2, this.getY() + this.height - 2,
                         0xFF000000 | backgroundColor);
        }

        int textColor = this.active ? (this.isHoveredOrFocused() ? hoverTextColor : normalTextColor) : 0xA0A0A0;
        graphics.drawCenteredString(minecraft.font, this.getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2,
                textColor);
    }

    public static class Builder {
        private int x, y, width, height;
        private ResourceLocation texture;
        private int normalU, normalV;
        private int hoverU, hoverV;
        private int textureWidth, textureHeight;
        private int normalTextColor = 0xFFFFFF;
        private int hoverTextColor = 0x7CFDD6;
        private int backgroundColor = 0;
        private Component message = Component.empty();
        private OnPress onPress;
        private SoundEvent sound = MainSounds.UI_NAVE_COOLDOWN.get();

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

        public Builder texture(ResourceLocation texture) {
            this.texture = texture;
            return this;
        }

        public Builder textureCoords(int normalU, int normalV, int hoverU, int hoverV) {
            this.normalU = normalU;
            this.normalV = normalV;
            this.hoverU = hoverU;
            this.hoverV = hoverV;
            return this;
        }

        public Builder textureSize(int width, int height) {
            this.textureWidth = width;
            this.textureHeight = height;
            return this;
        }

        public Builder textColors(int normalColor, int hoverColor) {
            this.normalTextColor = normalColor;
            this.hoverTextColor = hoverColor;
            return this;
        }

        public Builder backgroundColor(int color) {
            this.backgroundColor = color;
            return this;
        }

        public Builder message(Component message) {
            this.message = message;
            return this;
        }

        public Builder onPress(OnPress onPress) {
            this.onPress = onPress;
            return this;
        }

        public Builder sound(SoundEvent sound) {
            this.sound = sound;
            return this;
        }

        public TexturedTextButton build() {
            return new TexturedTextButton(x, y, width, height, texture,
                    normalU, normalV, hoverU, hoverV,
                    textureWidth, textureHeight,
                    normalTextColor, hoverTextColor, backgroundColor, message, onPress, sound);
        }
    }
}