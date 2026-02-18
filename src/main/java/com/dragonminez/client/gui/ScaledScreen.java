package com.dragonminez.client.gui;

import com.dragonminez.common.config.ConfigManager;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ScaledScreen extends Screen {
	private static final int MIN_GUI_WIDTH = 320;
	private static final int MIN_GUI_HEIGHT = 240;
	private static final float MIN_MENU_SCALE_MULTIPLIER = 0.25f;
	private static final float MAX_MENU_SCALE_MULTIPLIER = 3.0f;

	private float uiScale = 1.0f;
	private int uiWidth;
	private int uiHeight;

	protected ScaledScreen(Component title) {
		super(title);
	}

	protected void updateUiScale() {
		if (this.minecraft == null) {
			uiScale = 1.0f;
			uiWidth = this.width;
			uiHeight = this.height;
			return;
		}

		Window window = this.minecraft.getWindow();
		float newScale = calculateUiScale(window);
		if (newScale <= 0.0f || Float.isNaN(newScale) || Float.isInfinite(newScale)) {
			newScale = 1.0f;
		}

		uiScale = newScale;
		int currentWidth = window.getGuiScaledWidth();
		int currentHeight = window.getGuiScaledHeight();
		uiWidth = Math.max(1, Math.round(currentWidth / uiScale));
		uiHeight = Math.max(1, Math.round(currentHeight / uiScale));
	}

	private float calculateUiScale(Window window) {
		float availableScale = getAvailableScale(window);
		float dynamicScale = (float) Math.sqrt(availableScale);
		float desiredScale = dynamicScale * getMenuScaleMultiplier();
		return clamp(desiredScale, 1.0f, availableScale);
	}

	private float getAvailableScale(Window window) {
		int guiWidth = Math.max(1, window.getGuiScaledWidth());
		int guiHeight = Math.max(1, window.getGuiScaledHeight());
		float widthScale = guiWidth / (float) MIN_GUI_WIDTH;
		float heightScale = guiHeight / (float) MIN_GUI_HEIGHT;
		float availableScale = Math.min(widthScale, heightScale);

		if (!Float.isFinite(availableScale) || availableScale <= 0.0f) {
			return 1.0f;
		}

		return Math.max(1.0f, availableScale);
	}

	private float getMenuScaleMultiplier() {
		float multiplier = ConfigManager.getUserConfig().getHud().getMenuScaleMultiplier();
		if (!Float.isFinite(multiplier)) {
			return 1.0f;
		}
		return clamp(multiplier, MIN_MENU_SCALE_MULTIPLIER, MAX_MENU_SCALE_MULTIPLIER);
	}

	private float clamp(float value, float min, float max) {
		if (max < min) {
			return min;
		}
		return Math.max(min, Math.min(max, value));
	}

	protected float getUiScale() {
		updateUiScale();
		return uiScale;
	}

	protected int getUiWidth() {
		updateUiScale();
		return uiWidth;
	}

	protected int getUiHeight() {
		updateUiScale();
		return uiHeight;
	}

	protected double toUiX(double mouseX) {
		updateUiScale();
		return mouseX / uiScale;
	}

	protected double toUiY(double mouseY) {
		updateUiScale();
		return mouseY / uiScale;
	}

	protected int toScreenCoord(double uiCoord) {
		updateUiScale();
		return (int) Math.round(uiCoord * uiScale);
	}

	protected void beginUiScale(GuiGraphics graphics) {
		updateUiScale();
		graphics.pose().pushPose();
		graphics.pose().scale(uiScale, uiScale, 1.0f);
	}

	protected void endUiScale(GuiGraphics graphics) {
		graphics.pose().popPose();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return super.mouseClicked(toUiX(mouseX), toUiY(mouseY), button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return super.mouseReleased(toUiX(mouseX), toUiY(mouseY), button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return super.mouseDragged(toUiX(mouseX), toUiY(mouseY), button, dragX / getUiScale(), dragY / getUiScale());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		return super.mouseScrolled(toUiX(mouseX), toUiY(mouseY), delta);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		super.mouseMoved(toUiX(mouseX), toUiY(mouseY));
	}
}
