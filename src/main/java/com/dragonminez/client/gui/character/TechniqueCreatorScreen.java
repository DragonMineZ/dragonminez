package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ColorSlider;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.network.C2S.CreateTechniqueC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.text.NumberFormat;
import java.util.Locale;

public class TechniqueCreatorScreen extends ScaledScreen {
	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final NumberFormat COST_NUMBER_FORMAT = NumberFormat.getIntegerInstance(new Locale("es", "ES"));

	private final Screen parent;

	private EditBox nameField;
	private EditBox hexField;
	private CustomTextureButton interiorColorButton;
	private CustomTextureButton exteriorColorButton;
	private CustomTextureButton outlineColorButton;
	private CustomTextureButton utilityLeftArrow;
	private CustomTextureButton utilityRightArrow;
	private ColorSlider hueSlider;
	private ColorSlider saturationSlider;
	private ColorSlider valueSlider;
	private boolean updatingFromHex = false;
	private boolean colorPickerVisible = false;
	private boolean auraDefaultsApplied = false;
	private String colorTarget = "exterior";

	private String creatorName;
	private KiAttackData.KiType creatorType = KiAttackData.KiType.SMALL_BALL;
	private KiAttackData.Utility creatorUtility = KiAttackData.Utility.DAMAGE;
	private float creatorDamage = 1.0f;
	private float creatorSize = 1.0f;
	private float creatorSpeed = 1.0f;
	private int creatorArmorPen = 0;
	private int creatorCast = 20;
	private int creatorCooldown = 20;
	private float kiCost = 25;
	private float tpCost = 120;
	private int creatorColorInterior = 0xFFFFFF;
	private int creatorColorExterior = 0x00AEEF;
	private int creatorColorOutline = 0xFFFFFF;

	public TechniqueCreatorScreen(Screen parent) {
		super(Component.translatable("gui.dragonminez.skills.creator.title"));
		this.parent = parent;
		this.creatorName = Component.translatable("gui.dragonminez.skills.new_skill").getString();
	}

	@Override
	protected void init() {
		super.init();
		if (!auraDefaultsApplied) applyAuraDefaults();
		recomputeDerivedValues();
		clearWidgets();
		int x = getUiWidth() / 2 - 70;
		int y = getUiHeight() / 2 - 106;

		nameField = new EditBox(this.font, x + 18, y + 24, 105, 12, Component.empty());
		nameField.setMaxLength(24);
		nameField.setValue(creatorName);
		nameField.setResponder(v -> creatorName = v);
		addRenderableWidget(nameField);

		addRenderableWidget(createArrowButton(x + 8, y + 41, true, btn -> setCreatorType(prevEnum(creatorType, KiAttackData.KiType.values()))));
		addRenderableWidget(createArrowButton(x + 122, y + 41, false, btn -> setCreatorType(nextEnum(creatorType, KiAttackData.KiType.values()))));
		utilityLeftArrow = createArrowButton(x + 8, y + 56, true, btn -> toggleUtility());
		utilityRightArrow = createArrowButton(x + 122, y + 56, false, btn -> toggleUtility());
		addRenderableWidget(utilityLeftArrow);
		addRenderableWidget(utilityRightArrow);
		updateUtilityArrowsVisibility();

		addAdjusters(x, y + 76, () -> creatorDamage = Mth.clamp(creatorDamage - 0.1f, 0.1f, 20.0f), () -> creatorDamage = Mth.clamp(creatorDamage + 0.1f, 0.1f, 20.0f));
		addAdjusters(x, y + 91, () -> creatorSize = Mth.clamp(creatorSize - 0.1f, 0.1f, 20.0f), () -> creatorSize = Mth.clamp(creatorSize + 0.1f, 0.1f, 20.0f));
		addAdjusters(x, y + 106, () -> creatorSpeed = Mth.clamp(creatorSpeed - 0.1f, 0.1f, 20.0f), () -> creatorSpeed = Mth.clamp(creatorSpeed + 0.1f, 0.1f, 20.0f));
		addAdjusters(x, y + 121, () -> creatorArmorPen = Mth.clamp(creatorArmorPen - 1, 0, 100), () -> creatorArmorPen = Mth.clamp(creatorArmorPen + 1, 0, 100));

		interiorColorButton = new CustomTextureButton.Builder()
				.position(x + 14, y + 160)
				.size(20, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(42, 15, 42, 15)
				.textureSize(5, 5)
				.message(Component.empty())
				.onPress(btn -> showColorPicker("interior"))
				.build();
		addRenderableWidget(interiorColorButton);

		exteriorColorButton = new CustomTextureButton.Builder()
				.position(x + 60, y + 160)
				.size(20, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(42, 15, 42, 15)
				.textureSize(5, 5)
				.message(Component.empty())
				.onPress(btn -> showColorPicker("exterior"))
				.build();
		addRenderableWidget(exteriorColorButton);

		outlineColorButton = new CustomTextureButton.Builder()
				.position(x + 106, y + 160)
				.size(20, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(42, 15, 42, 15)
				.textureSize(5, 5)
				.message(Component.empty())
				.onPress(btn -> showColorPicker("outline"))
				.build();
		addRenderableWidget(outlineColorButton);

		addRenderableWidget(new TexturedTextButton.Builder()
				.position(x - 8, y + 215)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.hair_editor.cancel"))
				.onPress(btn -> onClose())
				.build());

		addRenderableWidget(new TexturedTextButton.Builder()
				.position(x + 75, y + 215)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.skills.create_skill"))
				.onPress(btn -> createSkill())
				.build());

		initColorPickerSliders();

		if (colorPickerVisible) showColorPicker(colorTarget);
	}

	private void initColorPickerSliders() {
		int x = getUiWidth() / 2 - 70;
		int y = getUiHeight() / 2 - 106;

		int sliderX = x + 141 + 10;
		int sliderY = y + 130;
		int sliderWidth = 90;

		hueSlider = new ColorSlider.Builder()
				.position(sliderX, sliderY)
				.size(sliderWidth, 10)
				.range(0, 360)
				.value(0)
				.message(tr("gui.dragonminez.customization.hue"))
				.onValueChange(val -> updateColorFromSliders())
				.build();

		saturationSlider = new ColorSlider.Builder()
				.position(sliderX, sliderY + 12)
				.size(sliderWidth, 10)
				.range(100, 0)
				.value(100)
				.message(tr("gui.dragonminez.customization.saturation"))
				.onValueChange(val -> updateColorFromSliders())
				.build();

		valueSlider = new ColorSlider.Builder()
				.position(sliderX, sliderY + 24)
				.size(sliderWidth, 10)
				.range(100, 0)
				.value(100)
				.message(tr("gui.dragonminez.customization.value"))
				.onValueChange(val -> updateColorFromSliders())
				.build();

		hexField = new EditBox(this.font, sliderX, sliderY + 36, sliderWidth, 12, tr("gui.dragonminez.common.hex"));
		hexField.setMaxLength(7);
		hexField.setResponder(this::onHexChanged);

		addRenderableWidget(hueSlider);
		addRenderableWidget(saturationSlider);
		addRenderableWidget(valueSlider);
		addRenderableWidget(hexField);

		setSlidersVisible();
	}

	private void addAdjusters(int x, int y, Runnable dec, Runnable inc) {
		addRenderableWidget(createArrowButton(x + 8, y - 5, true, btn -> {
			dec.run();
			recomputeDerivedValues();
		}));
		addRenderableWidget(createArrowButton(x + 122, y - 5, false, btn -> {
			inc.run();
			recomputeDerivedValues();
		}));
	}

	private CustomTextureButton createArrowButton(int x, int y, boolean left, CustomTextureButton.OnPress onPress) {
		return new CustomTextureButton.Builder()
				.position(x, y - 4)
				.size(10, 15)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(left ? 32 : 20, 0, left ? 32 : 20, 14)
				.textureSize(8, 14)
				.message(Component.empty())
				.onPress(onPress)
				.build();
	}

	private <T extends Enum<T>> T nextEnum(T current, T[] values) {
		return values[(current.ordinal() + 1) % values.length];
	}

	private <T extends Enum<T>> T prevEnum(T current, T[] values) {
		return values[(current.ordinal() - 1 + values.length) % values.length];
	}

	private void toggleUtility() {
		if (!allowsUtility(creatorType)) return;
		creatorUtility = creatorUtility == KiAttackData.Utility.DAMAGE
				? KiAttackData.Utility.HEAL
				: KiAttackData.Utility.DAMAGE;
		recomputeDerivedValues();
	}

	private boolean allowsUtility(KiAttackData.KiType type) {
		return type == KiAttackData.KiType.SHIELD || type == KiAttackData.KiType.AREA;
	}

	private void setCreatorType(KiAttackData.KiType newType) {
		creatorType = newType;
		creatorUtility = KiAttackData.Utility.DAMAGE;
		updateUtilityArrowsVisibility();
		recomputeDerivedValues();
	}

	private void recomputeDerivedValues() {
		float[] values = KiAttackData.previewDerivedValues(
				creatorType, creatorUtility,
				creatorDamage, creatorSize, creatorSpeed, creatorArmorPen
		);
		kiCost = values[0];
		tpCost = values[1];
		creatorCast = (int) values[2];
		creatorCooldown = (int) values[3];
	}

	private void updateUtilityArrowsVisibility() {
		boolean canUseUtility = allowsUtility(creatorType);
		if (utilityLeftArrow != null) {
			utilityLeftArrow.visible = canUseUtility;
			utilityLeftArrow.active = canUseUtility;
		}
		if (utilityRightArrow != null) {
			utilityRightArrow.visible = canUseUtility;
			utilityRightArrow.active = canUseUtility;
		}
	}

	private void applyAuraDefaults() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			String auraColor = data.getCharacter().getAuraColor();
			if (auraColor != null && !auraColor.isEmpty()) {
				int aura = ColorUtils.hexToInt(auraColor);
				creatorColorInterior = aura;
				creatorColorExterior = ColorUtils.darkenColor(aura, 0.75f);
			}
		});
		auraDefaultsApplied = true;
	}

	private void showColorPicker(String target) {
		colorTarget = target;
		colorPickerVisible = true;

		int color = "interior".equals(target) ? creatorColorInterior : "exterior".equals(target) ? creatorColorExterior : creatorColorOutline;
		float[] hsv = ColorUtils.rgbToHsv((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF);

		updatingFromHex = true;
		if (hueSlider != null) hueSlider.setValue(Math.round(hsv[0]));
		if (saturationSlider != null) {
			saturationSlider.setValue(Math.round(hsv[1]));
			saturationSlider.setCurrentHue(hsv[0]);
		}
		if (valueSlider != null) {
			valueSlider.setValue(Math.round(hsv[2]));
			valueSlider.setCurrentHue(hsv[0]);
			valueSlider.setCurrentSaturation(hsv[1] == 0 ? 100 : hsv[1]);
		}
		if (hexField != null) hexField.setValue(ColorUtils.hsvToHex(hsv[0], hsv[1], hsv[2]));
		updatingFromHex = false;

		setSlidersVisible();
	}

	private void hideColorPicker() {
		colorPickerVisible = false;
		setSlidersVisible();
	}

	private void setSlidersVisible() {
		if (hueSlider != null) hueSlider.visible = colorPickerVisible;
		if (saturationSlider != null) saturationSlider.visible = colorPickerVisible;
		if (valueSlider != null) valueSlider.visible = colorPickerVisible;
		if (hexField != null) hexField.visible = colorPickerVisible;
	}

	private void onHexChanged(String value) {
		if (updatingFromHex || value == null) return;
		String hex = value.startsWith("#") ? value : "#" + value;
		if (hex.length() != 7 || !hex.substring(1).matches("[0-9a-fA-F]{6}")) return;

		updatingFromHex = true;
		try {
			float[] hsv = ColorUtils.hexToHsv(hex);
			if (hueSlider != null) hueSlider.setValue(Math.round(hsv[0]));
			if (saturationSlider != null) {
				saturationSlider.setValue(Math.round(hsv[1]));
				saturationSlider.setCurrentHue(hsv[0]);
			}
			if (valueSlider != null) {
				valueSlider.setValue(Math.round(hsv[2]));
				valueSlider.setCurrentHue(hsv[0]);
				valueSlider.setCurrentSaturation(hsv[1]);
			}
			applyColor(hex);
		} catch (Exception ignored) {}
		updatingFromHex = false;
	}

	private void updateColorFromSliders() {
		if (!colorPickerVisible || hueSlider == null || saturationSlider == null || valueSlider == null) return;

		float h = hueSlider.getValue();
		float s = saturationSlider.getValue();
		float v = valueSlider.getValue();

		saturationSlider.setCurrentHue(h);
		valueSlider.setCurrentHue(h);
		valueSlider.setCurrentSaturation(s);

		String newColor = ColorUtils.hsvToHex(h, s, v);

		updatingFromHex = true;
		if (hexField != null && !hexField.isFocused()) hexField.setValue(newColor);
		updatingFromHex = false;

		applyColor(newColor);
	}



	private void applyColor(String hex) {
		int color = ColorUtils.hexToInt(hex);
		if ("interior".equals(colorTarget)) creatorColorInterior = color;
		else if ("exterior".equals(colorTarget)) creatorColorExterior = color;
		else creatorColorOutline = color;
	}

	private void createSkill() {
		if (!allowsUtility(creatorType)) creatorUtility = KiAttackData.Utility.DAMAGE;
		String defaultName = tr("gui.dragonminez.skills.new_skill").getString();
		String finalName = creatorName == null ? defaultName : creatorName.trim();
		if (finalName.isEmpty()) finalName = defaultName;
		NetworkHandler.INSTANCE.sendToServer(new CreateTechniqueC2S(
				finalName,
				creatorType.name(),
				creatorUtility.name(),
				creatorDamage,
				creatorSpeed,
				creatorSize,
				creatorArmorPen,
				creatorCast,
				creatorCooldown,
				creatorColorInterior,
				creatorColorExterior,
				creatorColorOutline
		));
		onClose();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));
		beginUiScale(graphics);

		int x = getUiWidth() / 2 - 70;
		int y = getUiHeight() / 2 - 106;
		graphics.blit(MENU_BIG, x, y, 0, 0, 141, 213, 256, 256);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.title"), x + 70, y + 10, 0xFFFFD700);

		int rowY = y + 39;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.type").append(": ").append(tr("technique.type." + creatorType.name().toLowerCase(Locale.ROOT))), x + 70, rowY, 0xFFFFFFFF);
		rowY += 15;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.utility").append(": ").append(tr("technique.utility." + creatorUtility.name().toLowerCase(Locale.ROOT))), x + 70, rowY, allowsUtility(creatorType) ? 0xFFFFFFFF : 0xFF777777);
		rowY += 15;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.damage").append(": ").append(txt(getDamageHealingExpression())), x + 70, rowY, 0xFFFFFFFF);
		rowY += 15;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.size").append(": ").append(txt(String.format(Locale.US, "%.1f", creatorSize))), x + 70, rowY, 0xFFFFFFFF);
		rowY += 15;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.speed").append(": ").append(txt(String.format(Locale.US, "%.1f", creatorSpeed))), x + 70, rowY, 0xFFFFFFFF);
		rowY += 15;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.armor_pen").append(": ").append(txt(String.valueOf(creatorArmorPen))), x + 70, rowY, 0xFFFFFFFF);
		rowY += 15;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.cast_time").append(": ").append(txt(String.valueOf(creatorCast))), x + 70, rowY, 0xFFFFFFFF);
		rowY += 15;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.cooldown").append(": ").append(txt(String.valueOf(creatorCooldown))), x + 70, rowY, 0xFFFFFFFF);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.color.interior"), x + 24, y + 152, 0xFFCCCCCC);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.color.exterior"), x + 70, y + 152, 0xFFCCCCCC);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.color.outline"), x + 116, y + 152, 0xFFCCCCCC);
		graphics.fill(x + 18, y + 165, x + 30, y + 178, 0xFF000000 | creatorColorInterior);
		graphics.fill(x + 64, y + 165, x + 76, y + 178, 0xFF000000 | creatorColorExterior);
		graphics.fill(x + 110, y + 165, x + 122, y + 178, 0xFF000000 | creatorColorOutline);
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.ki_cost_label"), x + 14, y + 180, 0xFFDDDDDD);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(COST_NUMBER_FORMAT.format(kiCost)), x + 98, y + 180, 0xFFDDDDDD);
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.tp_cost_label"), x + 14, y + 190, 0xFFDDDDDD);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(COST_NUMBER_FORMAT.format(tpCost)), x + 98, y + 190, 0xFFDDDDDD);

		if (colorPickerVisible) renderColorPickerBackground(graphics);

		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	private void renderColorPickerBackground(GuiGraphics graphics) {
		var poseStack = graphics.pose();
		poseStack.pushPose();
		poseStack.translate(0.0D, 0.0D, 200.0D);
		int x = getUiWidth() / 2 - 70;
		int y = getUiHeight() / 2 - 106;
		int sliderX = x + 141 + 10;
		int sliderY = y + 130;
		graphics.fill(sliderX - 5, sliderY - 5, sliderX + 95, sliderY + 56, 0x88000000);
		poseStack.popPose();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;

		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		if (colorPickerVisible) {
			int x = getUiWidth() / 2 - 70;
			int y = getUiHeight() / 2 - 106;
			int sliderX = x + 141 + 10;
			int sliderY = y + 130;
			int pickerW = 90;
			int pickerH = 56;

			boolean insidePicker = uiMouseX >= sliderX - 5 && uiMouseX <= sliderX + pickerW + 5
					&& uiMouseY >= sliderY - 5 && uiMouseY <= sliderY + pickerH + 5;
			boolean insideColorButtons = (uiMouseX >= x + 14 && uiMouseX <= x + 34 && uiMouseY >= y + 160 && uiMouseY <= y + 180)
					|| (uiMouseX >= x + 60 && uiMouseX <= x + 80 && uiMouseY >= y + 160 && uiMouseY <= y + 180)
					|| (uiMouseX >= x + 106 && uiMouseX <= x + 126 && uiMouseY >= y + 160 && uiMouseY <= y + 180);

			if (!insidePicker && !insideColorButtons) {
				hideColorPicker();
				return true;
			}
			return false;
		}

		return false;
	}

	private String getDamageHealingExpression() {
		double baseKiDamage = 0.0;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			final double[] value = {0.0};
			StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> value[0] = data.getKiDamage());
			baseKiDamage = value[0];
		}
		return String.format(Locale.US, "%.1f", baseKiDamage * creatorDamage);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		hideColorPicker();
		if (this.minecraft != null) this.minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
