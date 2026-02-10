package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.ScaledScreen;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class BaseMenuScreen extends ScaledScreen {
	protected static boolean GLOBAL_SWITCHING = false;
	protected boolean isSwitchingMenu = false;
    private static final ResourceLocation SCREEN_BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/menubuttons.png");
	private long animationStartTime;
	private boolean suppressOpenAnimation = false;
	private static final long ANIMATION_DURATION = 100;

	protected BaseMenuScreen(Component title) {
		super(title);
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
		int centerX = getUiWidth() / 2;
		int bottomY = getUiHeight() - 30;

        this.addRenderableWidget(
            new CustomTextureButton.Builder()
                .position(centerX - 70, bottomY)
                .size(20, 20)
                .texture(SCREEN_BUTTONS)
                .textureSize(20, 20)
                .textureCoords(0, 0, 0, 20)
				.onPress(btn -> switchMenu(new CharacterStatsScreen()))
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
				.onPress(btn -> switchMenu(new SkillsMenuScreen()))
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
				.onPress(btn -> switchMenu(new QuestsMenuScreen()))
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
				.onPress(btn -> switchMenu(new ConfigMenuScreen()))
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
		int uiWidth = getUiWidth();
		int uiHeight = getUiHeight();
		pose.translate(uiWidth / 2.0, uiHeight / 2.0, 0);
		pose.scale(scale, scale, 1.0f);
		pose.translate(-uiWidth / 2.0, -uiHeight / 2.0, 0);
	}

	protected int getAdjustedModelScale(int baseScale) {
		var player = Minecraft.getInstance().player;
		if (player == null) return baseScale;

		final float[] inverseScale = {1.0f};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			var character = stats.getCharacter();
			var activeForm = character.getActiveFormData();

			float currentScale;
			if (activeForm != null) {
				float[] formScaling = activeForm.getModelScaling();
				float[] charScaling = character.getModelScaling();
				currentScale = (formScaling[0] * charScaling[0] + formScaling[1] * charScaling[1]) / 2.0f;
			} else {
				float[] charScaling = character.getModelScaling();
				currentScale = (charScaling[0] + charScaling[1]) / 2.0f;
			}

			if (currentScale > 1.0f) inverseScale[0] = 0.9375f / currentScale;
		});

		return (int)(baseScale * inverseScale[0]);
	}
}
