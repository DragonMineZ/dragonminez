package com.dragonminez.client.gui.buttons;

import com.dragonminez.common.init.MainSounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomTextureButton extends Button {

    private final ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;
    private final int normalU;
    private final int normalV;
    private final int hoverU;
    private final int hoverV;
	private final SoundEvent sound;

    public CustomTextureButton(int x, int y, int width, int height, ResourceLocation texture,
                               int normalU, int normalV, int hoverU, int hoverV,
                               int textureWidth, int textureHeight,
                               Component message, OnPress onPress, SoundEvent sound) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.normalU = normalU;
        this.normalV = normalV;
        this.hoverU = hoverU;
        this.hoverV = hoverV;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
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
        int u = this.isHoveredOrFocused() ? hoverU : normalU;
        int v = this.isHoveredOrFocused() ? hoverV : normalV;

        graphics.blit(texture, this.getX(), this.getY(), u, v, textureWidth, textureHeight);
    }

    public static class Builder {
        private int x, y, width, height;
        private ResourceLocation texture;
        private int normalU, normalV;
        private int hoverU, hoverV;
        private int textureWidth, textureHeight;
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

        public CustomTextureButton build() {
            return new CustomTextureButton(x, y, width, height, texture,
                    normalU, normalV, hoverU, hoverV,
                    textureWidth, textureHeight, message, onPress, sound);
        }
    }
}

