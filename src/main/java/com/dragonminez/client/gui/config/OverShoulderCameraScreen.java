package com.dragonminez.client.gui.config;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.AxisSlider;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.render.camera.OverShoulderCamera;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralUserConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class OverShoulderCameraScreen extends Screen {

	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");

	private static final int PANEL_W = 141;
	private static final int PANEL_H = 213;
	private static final int MARGIN = 16;
	private static final int BUTTON_W = 74;
	private static final int SLIDER_W = 111;

	private final Screen parent;
	private CameraType previousCameraType;

	private float panelX;
	private float targetPanelX;
	private int panelY;

	private TexturedTextButton modeButton;
	private TexturedTextButton sideButton;
	private AxisSlider backSlider;
	private AxisSlider upSlider;
	private AxisSlider sideSlider;
	private AxisSlider smoothingSlider;

	public OverShoulderCameraScreen(Screen parent) {
		super(Component.translatable("gui.dragonminez.overShoulder.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();

		if (this.minecraft != null) {
			this.previousCameraType = this.minecraft.options.getCameraType();
			this.minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		}
		OverShoulderCamera.setPreviewOverride(true);

		this.panelY = (this.height - PANEL_H) / 2;
		this.targetPanelX = computeTargetPanelX();
		this.panelX = this.targetPanelX;

		GeneralUserConfig config = ConfigManager.getUserConfig();

		this.modeButton = buildButton(modeLabel(config.getOverShoulderMode()), b -> {
			GeneralUserConfig c = ConfigManager.getUserConfig();
			c.setOverShoulderMode((c.getOverShoulderMode() + 1) % 3);
			this.modeButton.setMessage(modeLabel(c.getOverShoulderMode()));
		});

		this.sideButton = buildButton(sideLabel(config.getOverShoulderLeft()), b -> {
			GeneralUserConfig c = ConfigManager.getUserConfig();
			c.setOverShoulderLeft(!c.getOverShoulderLeft());
			this.sideButton.setMessage(sideLabel(c.getOverShoulderLeft()));
			this.targetPanelX = computeTargetPanelX();
		});

		this.backSlider = buildSlider(1.0f, 6.0f, config.getOverShoulderBack(), AxisSlider.Axis.Z,
				v -> ConfigManager.getUserConfig().setOverShoulderBack(v));
		this.upSlider = buildSlider(-1.0f, 2.0f, config.getOverShoulderUp(), AxisSlider.Axis.Y,
				v -> ConfigManager.getUserConfig().setOverShoulderUp(v));
		this.sideSlider = buildSlider(0.0f, 3.0f, config.getOverShoulderSide(), AxisSlider.Axis.X,
				v -> ConfigManager.getUserConfig().setOverShoulderSide(v));
		this.smoothingSlider = buildSlider(0.05f, 0.6f, config.getOverShoulderSmoothing(), AxisSlider.Axis.X,
				v -> ConfigManager.getUserConfig().setOverShoulderSmoothing(v));

		this.addRenderableWidget(this.modeButton);
		this.addRenderableWidget(this.sideButton);
		this.addRenderableWidget(this.backSlider);
		this.addRenderableWidget(this.upSlider);
		this.addRenderableWidget(this.sideSlider);
		this.addRenderableWidget(this.smoothingSlider);

		layoutWidgets();
	}

	private TexturedTextButton buildButton(MutableComponent message, TexturedTextButton.OnPress onPress) {
		return new TexturedTextButton.Builder()
				.position(0, 0)
				.size(BUTTON_W, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(BUTTON_W, 20)
				.message(message)
				.onPress(onPress)
				.build();
	}

	private AxisSlider buildSlider(float min, float max, float current, AxisSlider.Axis axis, Consumer<Float> onChange) {
		return new SnapSlider(0, 0, SLIDER_W, 10, min, max, current, axis,
				v -> onChange.accept(Math.round(v * 100.0f) / 100.0f));
	}

	private int computeTargetPanelX() {
		boolean left = ConfigManager.getUserConfig().getOverShoulderLeft();
		return left ? MARGIN : this.width - PANEL_W - MARGIN;
	}

	private void layoutWidgets() {
		int px = Math.round(this.panelX);
		int buttonX = px + (PANEL_W - BUTTON_W) / 2;
		int sliderX = px + (PANEL_W - SLIDER_W) / 2;

		this.modeButton.setPosition(buttonX, this.panelY + 40);
		this.sideButton.setPosition(buttonX, this.panelY + 64);
		this.backSlider.setPosition(sliderX, this.panelY + 102);
		this.upSlider.setPosition(sliderX, this.panelY + 130);
		this.sideSlider.setPosition(sliderX, this.panelY + 158);
		this.smoothingSlider.setPosition(sliderX, this.panelY + 186);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.panelX += (this.targetPanelX - this.panelX) * 0.25f;
		if (Math.abs(this.targetPanelX - this.panelX) < 0.5f) this.panelX = this.targetPanelX;
		layoutWidgets();

		int px = Math.round(this.panelX);
		graphics.blit(MENU_BIG, px, this.panelY, 0, 0, PANEL_W, PANEL_H, 256, 256);
		graphics.blit(MENU_BIG, px + 17, this.panelY + 10, 142, 22, 107, 21, 256, 256);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.overShoulder.title"),
				px + PANEL_W / 2, this.panelY + 15, 0xFFFFD700);

		drawSliderLabel(graphics, "gui.dragonminez.overShoulder.distance", backSlider.getValue(), px, backSlider.getY());
		drawSliderLabel(graphics, "gui.dragonminez.overShoulder.height", upSlider.getValue(), px, upSlider.getY());
		drawSliderLabel(graphics, "gui.dragonminez.overShoulder.side", sideSlider.getValue(), px, sideSlider.getY());
		drawSliderLabel(graphics, "gui.dragonminez.overShoulder.smoothing", smoothingSlider.getValue(), px, smoothingSlider.getY());

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void drawSliderLabel(GuiGraphics graphics, String key, float value, int px, int sliderY) {
		int sliderX = px + (PANEL_W - SLIDER_W) / 2;
		TextUtil.drawStringWithBorder(graphics, this.font, tr(key), sliderX, sliderY - 11, 0xFFFFFFFF);
		MutableComponent valueText = txt(String.format("%.2f", value));
		int textW = this.font.width(valueText);
		TextUtil.drawStringWithBorder(graphics, this.font, valueText, sliderX + SLIDER_W - textW, sliderY - 11, 0xFF7CFDD6);
	}

	private MutableComponent modeLabel(int mode) {
		String key = switch (mode) {
			case OverShoulderCamera.MODE_ALWAYS -> "gui.dragonminez.overShoulder.mode.always";
			case OverShoulderCamera.MODE_LOCK_ON -> "gui.dragonminez.overShoulder.mode.lockOn";
			default -> "gui.dragonminez.overShoulder.mode.none";
		};
		return tr(key);
	}

	private MutableComponent sideLabel(boolean left) {
		return tr(left ? "gui.dragonminez.overShoulder.side.left" : "gui.dragonminez.overShoulder.side.right");
	}

	private MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	private MutableComponent txt(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) this.minecraft.setScreen(this.parent);
	}

	@Override
	public void removed() {
		if (this.minecraft != null && this.previousCameraType != null) {
			this.minecraft.options.setCameraType(this.previousCameraType);
		}
		OverShoulderCamera.setPreviewOverride(false);
		ConfigManager.saveGeneralUserConfig();
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static class SnapSlider extends AxisSlider {
		private SnapSlider(int x, int y, int width, int height, float minValue, float maxValue, float currentValue, Axis axis, Consumer<Float> onValueChange) {
			super(x, y, width, height, minValue, maxValue, currentValue, axis, onValueChange);
		}

		@Override
		protected void applyValue() {
			float step = Screen.hasShiftDown() ? 0.05f : 0.01f;
			float snapped = Math.round(getValue() / step) * step;
			setValue(snapped);
			super.applyValue();
		}
	}
}
