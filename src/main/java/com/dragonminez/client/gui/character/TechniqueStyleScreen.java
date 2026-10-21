package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ColorSlider;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.render.camera.TechniquePreviewCamera;
import com.dragonminez.client.render.effects.TechniquePreview;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.TextUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.text.NumberFormat;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class TechniqueStyleScreen extends ScaledScreen {
	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final NumberFormat COST_FORMAT = NumberFormat.getIntegerInstance(new Locale("es", "ES"));

	private static final int PANEL_W = 141;
	private static final int PANEL_H = 213;
	private static final int MARGIN = 16;
	private static final int BUTTON_W = 74;
	private static final int BUTTON_H = 20;
	private static final int BUTTON_GAP = 4;
	private static final int SLIDER_W = 111;
	private static final int SWATCH = 14;
	private static final int[] SWATCH_CENTERS = {26, 70, 114};

	private static final int PLATE_Y = 10;
	private static final int TYPE_LABEL_Y = 38;
	private static final int TYPE_ROW_Y = 51;
	private static final int CAST_Y = 64;
	private static final int COST_Y = 75;
	private static final int COLORS_LABEL_Y = 92;
	private static final int SWATCH_Y = 104;
	private static final int SWATCH_LABEL_Y = 121;
	private static final int HUE_Y = 135;
	private static final int SATURATION_Y = 149;
	private static final int VALUE_Y = 163;
	private static final int HEX_Y = 177;

	private static final float ORBIT_SENSITIVITY = 0.45F;
	private static final float SLIDE_TAU = 0.06F;
	private static final int GOLD = 0xFFFFD700;
	private static final int ACCENT = 0xFF7CFDD6;
	private static final int MUTED = 0xFFCCCCCC;
	private static final int CAST_COLOR = 0xFFAACCFF;
	private static final int DENIED = 0xFFFF6B6B;

	private final Screen parent;
	private final TechniqueDraft draft;
	private TechniqueDraft.ColorTarget colorTarget = TechniqueDraft.ColorTarget.INTERIOR;

	private float panelX = -PANEL_W - MARGIN;
	private int panelY;
	private boolean orbiting;
	private boolean updatingPicker;

	private CustomTextureButton typeLeft;
	private CustomTextureButton typeRight;
	private ColorSlider hueSlider;
	private ColorSlider saturationSlider;
	private ColorSlider valueSlider;
	private EditBox hexField;
	private TexturedTextButton cancelButton;
	private TexturedTextButton continueButton;

	public TechniqueStyleScreen(Screen parent) {
		this(parent, TechniqueDraft.create());
	}

	public TechniqueStyleScreen(Screen parent, TechniqueDraft draft) {
		super(Component.translatable("gui.dragonminez.skills.creator.title"));
		this.parent = parent;
		this.draft = draft;
	}

	@Override
	protected int getMinGuiHeight() {
		return 300;
	}

	@Override
	protected void init() {
		super.init();

		TechniquePreview.show(draft.buildTechnique());
		LocalPlayer player = this.minecraft != null ? this.minecraft.player : null;
		if (player != null) {
			TechniquePreviewCamera.activate(player.getYRot(), TechniquePreview.frameDistance(player), TechniquePreview.frameHeight(player), TechniquePreview.frameLead(player));
		}

		this.panelY = Math.max(30, (getUiHeight() - (PANEL_H + 6 + BUTTON_H)) / 2);

		this.typeLeft = createArrowButton(true, btn -> changeType(-1));
		this.typeRight = createArrowButton(false, btn -> changeType(1));
		addRenderableWidget(this.typeLeft);
		addRenderableWidget(this.typeRight);

		this.hueSlider = new ColorSlider.Builder()
				.position(0, 0)
				.size(SLIDER_W, 10)
				.range(0, 360)
				.value(0)
				.message(tr("gui.dragonminez.customization.hue"))
				.gradient(ColorSlider.Gradient.HUE)
				.onValueChange(val -> onSlidersChanged())
				.build();
		this.saturationSlider = new ColorSlider.Builder()
				.position(0, 0)
				.size(SLIDER_W, 10)
				.range(100, 0)
				.value(100)
				.message(tr("gui.dragonminez.customization.saturation"))
				.gradient(ColorSlider.Gradient.SATURATION)
				.onValueChange(val -> onSlidersChanged())
				.build();
		this.valueSlider = new ColorSlider.Builder()
				.position(0, 0)
				.size(SLIDER_W, 10)
				.range(100, 0)
				.value(100)
				.message(tr("gui.dragonminez.customization.value"))
				.gradient(ColorSlider.Gradient.VALUE)
				.onValueChange(val -> onSlidersChanged())
				.build();
		this.hexField = new EditBox(this.font, 0, 0, SLIDER_W, 12, tr("gui.dragonminez.common.hex"));
		this.hexField.setMaxLength(7);
		this.hexField.setResponder(this::onHexChanged);
		addRenderableWidget(this.hueSlider);
		addRenderableWidget(this.saturationSlider);
		addRenderableWidget(this.valueSlider);
		addRenderableWidget(this.hexField);

		this.cancelButton = createButton(tr("gui.dragonminez.hair_editor.cancel"), btn -> onClose());
		this.continueButton = createButton(tr("gui.dragonminez.skills.creator.continue"), btn -> openStats());
		addRenderableWidget(this.cancelButton);
		addRenderableWidget(this.continueButton);

		syncPicker();
		layoutWidgets();
	}

	private CustomTextureButton createArrowButton(boolean left, CustomTextureButton.OnPress onPress) {
		return new CustomTextureButton.Builder()
				.position(0, 0)
				.size(10, 15)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(left ? 32 : 20, 0, left ? 32 : 20, 14)
				.textureSize(8, 14)
				.message(Component.empty())
				.onPress(onPress)
				.build();
	}

	private TexturedTextButton createButton(MutableComponent message, TexturedTextButton.OnPress onPress) {
		return new TexturedTextButton.Builder()
				.position(0, 0)
				.size(BUTTON_W, BUTTON_H)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(BUTTON_W, BUTTON_H)
				.message(message)
				.onPress(onPress)
				.build();
	}

	private void layoutWidgets() {
		int sliderX = (PANEL_W - SLIDER_W) / 2;
		int buttonsX = (PANEL_W - (BUTTON_W * 2 + BUTTON_GAP)) / 2;
		int buttonsY = this.panelY + PANEL_H + 6;

		this.typeLeft.setY(this.panelY + TYPE_ROW_Y - 2);
		this.typeRight.setY(this.panelY + TYPE_ROW_Y - 2);
		this.hueSlider.setY(this.panelY + HUE_Y);
		this.saturationSlider.setY(this.panelY + SATURATION_Y);
		this.valueSlider.setY(this.panelY + VALUE_Y);
		this.hexField.setY(this.panelY + HEX_Y);
		this.cancelButton.setY(buttonsY);
		this.continueButton.setY(buttonsY);

		slideX(this.typeLeft, 10, this.panelX);
		slideX(this.typeRight, PANEL_W - 20, this.panelX);
		slideX(this.hueSlider, sliderX, this.panelX);
		slideX(this.saturationSlider, sliderX, this.panelX);
		slideX(this.valueSlider, sliderX, this.panelX);
		slideX(this.hexField, sliderX, this.panelX);
		slideX(this.cancelButton, buttonsX, this.panelX);
		slideX(this.continueButton, buttonsX + BUTTON_W + BUTTON_GAP, this.panelX);
	}

	private void changeType(int direction) {
		draft.cycleType(direction);
		TechniquePreview.show(draft.buildTechnique());
	}

	private void selectTarget(TechniqueDraft.ColorTarget target) {
		if (this.colorTarget == target) return;
		this.colorTarget = target;
		this.hexField.setFocused(false);
		syncPicker();
	}

	private void syncPicker() {
		int color = draft.getColor(colorTarget);
		float[] hsv = ColorUtils.rgbToHsv((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF);
		this.updatingPicker = true;
		this.hueSlider.setValue(Math.round(hsv[0]));
		this.saturationSlider.setValue(Math.round(hsv[1]));
		this.saturationSlider.setCurrentHue(hsv[0]);
		this.valueSlider.setValue(Math.round(hsv[2]));
		this.valueSlider.setCurrentHue(hsv[0]);
		this.valueSlider.setCurrentSaturation(hsv[1] == 0 ? 100 : hsv[1]);
		this.hexField.setValue(String.format(Locale.ROOT, "#%06X", color));
		this.updatingPicker = false;
	}

	private void onSlidersChanged() {
		if (this.updatingPicker) return;
		float h = this.hueSlider.getValue();
		float s = this.saturationSlider.getValue();
		float v = this.valueSlider.getValue();
		this.saturationSlider.setCurrentHue(h);
		this.valueSlider.setCurrentHue(h);
		this.valueSlider.setCurrentSaturation(s);

		String hex = ColorUtils.hsvToHex(h, s, v);
		if (!this.hexField.isFocused()) {
			this.updatingPicker = true;
			this.hexField.setValue(hex);
			this.updatingPicker = false;
		}
		applyColor(ColorUtils.hexToInt(hex));
	}

	private void onHexChanged(String value) {
		if (this.updatingPicker || value == null) return;
		String hex = value.startsWith("#") ? value : "#" + value;
		if (hex.length() != 7 || !hex.substring(1).matches("[0-9a-fA-F]{6}")) return;

		float[] hsv = ColorUtils.hexToHsv(hex);
		this.updatingPicker = true;
		this.hueSlider.setValue(Math.round(hsv[0]));
		this.saturationSlider.setValue(Math.round(hsv[1]));
		this.saturationSlider.setCurrentHue(hsv[0]);
		this.valueSlider.setValue(Math.round(hsv[2]));
		this.valueSlider.setCurrentHue(hsv[0]);
		this.valueSlider.setCurrentSaturation(hsv[1]);
		this.updatingPicker = false;
		applyColor(ColorUtils.hexToInt(hex));
	}

	private void applyColor(int color) {
		draft.setColor(colorTarget, color);
		TechniquePreview.show(draft.buildTechnique());
	}

	private void openStats() {
		if (this.minecraft != null) this.minecraft.setScreen(new TechniqueCreatorScreen(this.parent, this.draft));
	}

	@Override
	public void tick() {
		super.tick();
		this.hexField.tick();
		LocalPlayer player = this.minecraft != null ? this.minecraft.player : null;
		if (player == null) return;
		TechniquePreviewCamera.frame(TechniquePreview.frameDistance(player), TechniquePreview.frameHeight(player), TechniquePreview.frameLead(player));
		TechniquePreview.tick();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));
		beginUiScale(graphics);
		this.panelX += (MARGIN - this.panelX) * frameEase(SLIDE_TAU);
		if (Math.abs(MARGIN - this.panelX) < 0.05f) this.panelX = MARGIN;
		layoutWidgets();

		int centerX = getUiWidth() / 2;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.title"), centerX, 8, GOLD);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.step", 1, 2), centerX, 19, MUTED);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.camera_hint"), centerX, getUiHeight() - 14, MUTED);

		graphics.pose().pushPose();
		graphics.pose().translate(this.panelX, 0.0f, 0.0f);
		renderPanel(graphics, uiMouseX - this.panelX, uiMouseY);
		graphics.pose().popPose();

		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	private void renderPanel(GuiGraphics graphics, float localMouseX, int uiMouseY) {
		int cx = PANEL_W / 2;
		blit(graphics, MENU_BIG, 0, this.panelY, 0, 0, PANEL_W, PANEL_H);
		blit(graphics, MENU_BIG, 17, this.panelY + PLATE_Y, 142, 22, 107, 21);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.appearance"), cx, this.panelY + PLATE_Y + 6, GOLD);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.type"), cx, this.panelY + TYPE_LABEL_Y, GOLD);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(draft.typeKey()), cx, this.panelY + TYPE_ROW_Y, 0xFFFFFFFF);

		int castTicks = draft.getCast();
		MutableComponent castValue = castTicks <= 0
				? tr("gui.dragonminez.skills.creator.instant")
				: txt(String.format(Locale.ROOT, "%.1fs", castTicks / 20.0f));
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.cast_time").append(": ").append(castValue), cx, this.panelY + CAST_Y, CAST_COLOR);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.skills.creator.tp_cost_label").append(" ").append(txt(COST_FORMAT.format(draft.getTpCost()))),
				cx, this.panelY + COST_Y, draft.canAfford() ? MUTED : DENIED);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.colors"), cx, this.panelY + COLORS_LABEL_Y, GOLD);
		TechniqueDraft.ColorTarget[] targets = TechniqueDraft.ColorTarget.values();
		for (int i = 0; i < targets.length; i++) {
			TechniqueDraft.ColorTarget target = targets[i];
			int sx = SWATCH_CENTERS[i] - SWATCH / 2;
			int sy = this.panelY + SWATCH_Y;
			boolean selected = target == this.colorTarget;
			boolean hovered = localMouseX >= sx && localMouseX < sx + SWATCH && uiMouseY >= sy && uiMouseY < sy + SWATCH;
			int frame = selected ? ACCENT : hovered ? 0xFFFFFFFF : 0xFF000000;
			HudRender.rect(graphics, sx - 1, sy - 1, SWATCH + 2, SWATCH + 2, frame);
			HudRender.rect(graphics, sx, sy, SWATCH, SWATCH, 0xFF000000 | draft.getColor(target));
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(colorLabelKey(target)), SWATCH_CENTERS[i],
					this.panelY + SWATCH_LABEL_Y, selected ? ACCENT : MUTED);
		}
	}

	private static String colorLabelKey(TechniqueDraft.ColorTarget target) {
		return switch (target) {
			case INTERIOR -> "gui.dragonminez.skills.creator.color.interior";
			case EXTERIOR -> "gui.dragonminez.skills.creator.color.exterior";
			case OUTLINE -> "gui.dragonminez.skills.creator.color.outline";
		};
	}

	private TechniqueDraft.ColorTarget swatchAt(double uiMouseX, double uiMouseY) {
		double localX = uiMouseX - this.panelX;
		int sy = this.panelY + SWATCH_Y;
		if (uiMouseY < sy - 1 || uiMouseY >= sy + SWATCH + 1) return null;
		TechniqueDraft.ColorTarget[] targets = TechniqueDraft.ColorTarget.values();
		for (int i = 0; i < targets.length; i++) {
			int sx = SWATCH_CENTERS[i] - SWATCH / 2;
			if (localX >= sx - 1 && localX < sx + SWATCH + 1) return targets[i];
		}
		return null;
	}

	private boolean isOverPanel(double uiMouseX, double uiMouseY) {
		double localX = uiMouseX - this.panelX;
		return localX >= 0 && localX < PANEL_W && uiMouseY >= this.panelY && uiMouseY < this.panelY + PANEL_H + 6 + BUTTON_H;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;

		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		setFocused(null);
		this.hexField.setFocused(false);

		TechniqueDraft.ColorTarget swatch = button == 0 ? swatchAt(uiMouseX, uiMouseY) : null;
		if (swatch != null) {
			selectTarget(swatch);
			return true;
		}
		if (isOverPanel(uiMouseX, uiMouseY)) return false;
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return false;
		this.orbiting = true;
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (this.orbiting) {
			TechniquePreviewCamera.orbit(dragX * ORBIT_SENSITIVITY, dragY * ORBIT_SENSITIVITY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		this.orbiting = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (!isOverPanel(toUiX(mouseX), toUiY(mouseY))) {
			TechniquePreviewCamera.zoom(delta);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		if (!this.hexField.isFocused()) {
			if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
				changeType(-1);
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
				changeType(1);
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				openStats();
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) this.minecraft.setScreen(this.parent);
	}

	@Override
	public void removed() {
		TechniquePreview.hide();
		TechniquePreviewCamera.deactivate();
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
