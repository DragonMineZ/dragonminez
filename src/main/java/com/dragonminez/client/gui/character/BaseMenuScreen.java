package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class BaseMenuScreen extends Screen {

    protected final int oldGuiScale;
	protected boolean isSwitchingMenu = false;
    private static final ResourceLocation SCREEN_BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/menubuttons.png");

    protected BaseMenuScreen(Component title, int oldGuiScale) {
        super(title);
        this.oldGuiScale = oldGuiScale;
    }

    @Override
	protected void init() {
		super.init();
		initNavigationButtons();
	}

    protected void initNavigationButtons() {
        int centerX = this.width / 2;
        int bottomY = this.height - 30;

        this.addRenderableWidget(
            new CustomTextureButton.Builder()
                .position(centerX - 70, bottomY)
                .size(20, 20)
                .texture(SCREEN_BUTTONS)
                .textureSize(20, 20)
                .textureCoords(0, 0, 0, 20)
                .onPress(btn -> {
                    if (this.minecraft != null && !(this.minecraft.screen instanceof CharacterStatsScreen)) {
						this.isSwitchingMenu = true;
                        this.minecraft.setScreen(new CharacterStatsScreen(this.oldGuiScale));
                    }
                })
                .build()
        );

        this.addRenderableWidget(
            new CustomTextureButton.Builder()
                .position(centerX - 30, bottomY)
                .size(20, 20)
                .texture(SCREEN_BUTTONS)
                .textureSize(20, 20)
                .textureCoords(20, 0, 20, 20)
                .onPress(btn -> {
                    if (this.minecraft != null && !(this.minecraft.screen instanceof SkillsMenuScreen)) {
						this.isSwitchingMenu = true;
                        this.minecraft.setScreen(new SkillsMenuScreen(this.oldGuiScale));
                    }
                })
                .build()
        );

        this.addRenderableWidget(
            new CustomTextureButton.Builder()
                .position(centerX + 10, bottomY)
                .size(20, 20)
                .texture(SCREEN_BUTTONS)
                .textureSize(20, 20)
                .textureCoords(60, 0, 60, 20)
                .onPress(btn -> {
                    if (this.minecraft != null && !(this.minecraft.screen instanceof QuestsMenuScreen)) {
						this.isSwitchingMenu = true;
                        this.minecraft.setScreen(new QuestsMenuScreen(this.oldGuiScale));
                    }
                })
                .build()
        );

        this.addRenderableWidget(
            new CustomTextureButton.Builder()
                .position(centerX + 50, bottomY)
                .size(20, 20)
                .texture(SCREEN_BUTTONS)
                .textureSize(20, 20)
                .textureCoords(100, 0, 100, 20)
                .onPress(btn -> {
                    if (this.minecraft != null && !(this.minecraft.screen instanceof ConfigMenuScreen)) {
						this.isSwitchingMenu = true;
                        this.minecraft.setScreen(new ConfigMenuScreen(this.oldGuiScale));
                    }
                })
                .build()
        );
    }

	@Override
	public void removed() {
		if (!this.isSwitchingMenu && this.minecraft != null) {
			if (this.minecraft.options.guiScale().get() != oldGuiScale) {
				this.minecraft.options.guiScale().set(oldGuiScale);
				this.minecraft.resizeDisplay();
			}
		}

		super.removed();
	}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
