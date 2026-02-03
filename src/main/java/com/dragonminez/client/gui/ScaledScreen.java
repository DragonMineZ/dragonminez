package com.dragonminez.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ScaledScreen extends Screen {
	protected static final float TARGET_GUI_SCALE = 3.0f;

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

		double currentScale = this.minecraft.getWindow().getGuiScale();
		if (currentScale <= 0.0D) {
			currentScale = 1.0D;
		}

		float newScale = (float) (TARGET_GUI_SCALE / currentScale);
		if (newScale <= 0.0f || Float.isNaN(newScale) || Float.isInfinite(newScale)) {
			newScale = 1.0f;
		}

		uiScale = newScale;
		uiWidth = Math.round(this.width / uiScale);
		uiHeight = Math.round(this.height / uiScale);
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
