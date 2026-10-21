package com.dragonminez.client.gui.character.util;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.SubpixelWidget;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.gui.tutorial.TutorialHost;
import com.dragonminez.client.gui.tutorial.TutorialManager;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralUserConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ScaledScreen extends Screen implements TutorialHost {
	public static final float REFERENCE_HEIGHT = 270.0f;
	public static final float MIN_MENU_SCALE_MULTIPLIER = 0.5f;
	public static final float MAX_MENU_SCALE_MULTIPLIER = 2.5f;
	private static final double HIDDEN_MOUSE = -10000.0;

	protected static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private float uiScale = 1.0f;
	private float fitScale = 1.0f;
	private int uiWidth;
	private int uiHeight;

	private int cachedGuiWidth = -1;
	private int cachedGuiHeight = -1;
	private float cachedMultiplier = Float.NaN;

	private long lastFrameNanos;
	private float frameDelta;

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
		int currentWidth = window.getGuiScaledWidth();
		int currentHeight = window.getGuiScaledHeight();
		float multiplier = getMenuScaleMultiplier();

		if (currentWidth == cachedGuiWidth && currentHeight == cachedGuiHeight && multiplier == cachedMultiplier) return;

		float newScale = calculateUiScale(Math.max(1, currentWidth), Math.max(1, currentHeight), multiplier);
		if (newScale <= 0.0f || Float.isNaN(newScale) || Float.isInfinite(newScale)) newScale = 1.0f;

		uiScale = newScale;
		uiWidth = Math.max(1, Math.round(currentWidth / uiScale));
		uiHeight = Math.max(1, Math.round(currentHeight / uiScale));

		cachedGuiWidth = currentWidth;
		cachedGuiHeight = currentHeight;
		cachedMultiplier = multiplier;
	}

	private float calculateUiScale(int guiWidth, int guiHeight, float multiplier) {
		float desiredScale = computeDynamicScale(guiWidth, guiHeight) * multiplier;
		float minScale = getMinUiScale();
		fitScale = Math.max(minScale, Math.min(guiWidth / (float) getMinGuiWidth(), guiHeight / (float) getMinGuiHeight()));
		return Mth.clamp(desiredScale, minScale, fitScale);
	}

	protected float computeDynamicScale(int guiWidth, int guiHeight) {
		return guiHeight / getReferenceHeight();
	}

	protected float getReferenceHeight() {
		return REFERENCE_HEIGHT;
	}

	protected float getMinUiScale() {
		return 0.25f;
	}

	private float getMenuScaleMultiplier() {
		float multiplier = ConfigManager.getUserConfig().getMenuScaleMultiplier();
		if (!Float.isFinite(multiplier)) return GeneralUserConfig.DEFAULT_MENU_SCALE;
		return Mth.clamp(multiplier, MIN_MENU_SCALE_MULTIPLIER, MAX_MENU_SCALE_MULTIPLIER);
	}

	protected int getMinGuiWidth() {
		return 320;
	}

	protected int getMinGuiHeight() {
		return 240;
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
		if (TutorialManager.isHoverBlocked(this)) return HIDDEN_MOUSE;
		return mouseX / uiScale;
	}

	protected double toUiY(double mouseY) {
		updateUiScale();
		if (TutorialManager.isHoverBlocked(this)) return HIDDEN_MOUSE;
		return mouseY / uiScale;
	}

	protected int toScreenCoord(double uiCoord) {
		updateUiScale();
		return (int) Math.round(uiCoord * uiScale);
	}

	protected void beginUiScale(GuiGraphics graphics) {
		updateUiScale();
		long now = System.nanoTime();
		frameDelta = lastFrameNanos == 0L ? 0.0f : Math.min(0.1f, (now - lastFrameNanos) / 1_000_000_000.0f);
		lastFrameNanos = now;
		graphics.pose().pushPose();
		graphics.pose().scale(uiScale, uiScale, 1.0f);
	}

	protected void endUiScale(GuiGraphics graphics) {
		graphics.pose().popPose();
	}

	protected float frameDelta() {
		return frameDelta;
	}

	protected float frameEase() {
		return frameEase(0.115f);
	}

	protected float frameEase(float tauSeconds) {
		return frameDelta <= 0.0f ? 0.0f : 1.0f - (float) Math.exp(-frameDelta / tauSeconds);
	}

	protected static void blit(GuiGraphics graphics, ResourceLocation texture, float x, float y, float u, float v, float width, float height) {
		HudRender.blit(graphics, texture, x, y, u, v, width, height, 256, 256);
	}

	protected static void blit(GuiGraphics graphics, ResourceLocation texture, float x, float y, float u, float v, float width, float height, int textureWidth, int textureHeight) {
		HudRender.blit(graphics, texture, x, y, u, v, width, height, textureWidth, textureHeight);
	}

	protected static void slideX(AbstractWidget widget, int baseX, float offset) {
		if (widget == null) return;
		int whole = Mth.floor(offset);
		widget.setX(baseX + whole);
		if (widget instanceof SubpixelWidget subpixel) subpixel.setSubpixelX(offset - whole);
	}

	protected static void slideY(AbstractWidget widget, int baseY, float offset) {
		if (widget == null) return;
		int whole = Mth.floor(offset);
		widget.setY(baseY + whole);
		if (widget instanceof SubpixelWidget subpixel) subpixel.setSubpixelY(offset - whole);
	}

	public void applyMenuScaleChange() {
		rebuildWidgets();
	}

	public boolean adjustMenuScale(int direction) {
		updateUiScale();
		if (direction > 0 && uiScale >= fitScale - 0.001f) return false;
		if (direction < 0 && uiScale <= getMinUiScale() + 0.001f) return false;

		float current = getMenuScaleMultiplier();
		float next = Mth.clamp(Math.round((current + direction * 0.05f) * 100.0f) / 100.0f, MIN_MENU_SCALE_MULTIPLIER, MAX_MENU_SCALE_MULTIPLIER);
		if (next == current) return false;

		ConfigManager.getUserConfig().setMenuScaleMultiplier(next);
		ConfigManager.saveGeneralUserConfig();
		applyMenuScaleChange();
		return true;
	}

	public int menuScalePercent() {
		return Math.round(getMenuScaleMultiplier() * 100.0f);
	}

	@Override
	public float tutorialScale() {
		return getUiScale();
	}

	@Override
	public int tutorialWidth() {
		return getUiWidth();
	}

	@Override
	public int tutorialHeight() {
		return getUiHeight();
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

	public MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	public MutableComponent txt(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}
}
