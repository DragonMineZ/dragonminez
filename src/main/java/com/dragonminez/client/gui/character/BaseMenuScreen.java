package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class BaseMenuScreen extends Screen {

    protected final int oldGuiScale;
	protected static boolean GLOBAL_SWITCHING = false;
	protected boolean isSwitchingMenu = false;
    private static final ResourceLocation SCREEN_BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/menubuttons.png");
	private long animationStartTime;
	private boolean suppressOpenAnimation = false;
	private static final long ANIMATION_DURATION = 100;

    protected BaseMenuScreen(Component title, int oldGuiScale) {
        super(title);
        this.oldGuiScale = oldGuiScale;
    }

	@Override
	protected void init() {
		super.init();

		this.animationStartTime = System.currentTimeMillis();

		if (GLOBAL_SWITCHING) {
			this.suppressOpenAnimation = true;
			GLOBAL_SWITCHING = false;
		} else {
			this.suppressOpenAnimation = false;
		}

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
				.onPress(btn -> switchMenu(new CharacterStatsScreen(this.oldGuiScale)))
				.sound(MainSounds.UI_MENU_SWITCH.get())
                .build()
        );

        this.addRenderableWidget(
            new CustomTextureButton.Builder()
                .position(centerX - 30, bottomY)
                .size(20, 20)
                .texture(SCREEN_BUTTONS)
                .textureSize(20, 20)
                .textureCoords(20, 0, 20, 20)
				.onPress(btn -> switchMenu(new SkillsMenuScreen(this.oldGuiScale)))
				.sound(MainSounds.UI_MENU_SWITCH.get())
				.build()
        );

        this.addRenderableWidget(
            new CustomTextureButton.Builder()
                .position(centerX + 10, bottomY)
                .size(20, 20)
                .texture(SCREEN_BUTTONS)
                .textureSize(20, 20)
                .textureCoords(60, 0, 60, 20)
                .onPress(btn -> switchMenu(new QuestsMenuScreen(this.oldGuiScale)))
				.sound(MainSounds.UI_MENU_SWITCH.get())
				.build()
        );

        this.addRenderableWidget(
            new CustomTextureButton.Builder()
                .position(centerX + 50, bottomY)
                .size(20, 20)
                .texture(SCREEN_BUTTONS)
                .textureSize(20, 20)
                .textureCoords(100, 0, 100, 20)
                .onPress(btn -> switchMenu(new ConfigMenuScreen(this.oldGuiScale)))
				.sound(MainSounds.UI_MENU_SWITCH.get())
				.build()
        );
    }

	protected void switchMenu(Screen nextScreen) {
		if (this.minecraft != null && !(this.minecraft.screen.getClass().equals(nextScreen.getClass()))) {
			this.isSwitchingMenu = true;
			GLOBAL_SWITCHING = true;
			this.minecraft.setScreen(nextScreen);
		}
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

	public boolean isAnimating() {
		if (suppressOpenAnimation) return false;
		return (System.currentTimeMillis() - animationStartTime) < ANIMATION_DURATION;
	}

	protected void applyZoom(GuiGraphics graphics) {
		if (suppressOpenAnimation) return;

		long elapsed = System.currentTimeMillis() - animationStartTime;
		float scale = (float) elapsed / ANIMATION_DURATION;

		if (scale >= 1.0f) {
			scale = 1.0f;
		}

		PoseStack pose = graphics.pose();
		pose.translate(this.width / 2.0, this.height / 2.0, 0);
		pose.scale(scale, scale, 1.0f);
		pose.translate(-this.width / 2.0, -this.height / 2.0, 0);
	}
}
