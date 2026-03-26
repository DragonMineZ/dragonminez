package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.ScaledScreen;
import com.dragonminez.client.gui.buttons.ColorSlider;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.util.ColorUtils;
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
	private int dummyKiCost = 25;
	private int dummyTpCost = 120;
	private int creatorColorInterior = 0xFFFFFF;
	private int creatorColorExterior = 0x00AEEF;

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
				.position(x + 26, y + 160)
				.size(20, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(42, 15, 42, 15)
				.textureSize(5, 5)
				.message(Component.empty())
				.onPress(btn -> showColorPicker("interior"))
				.build();
		addRenderableWidget(interiorColorButton);

		exteriorColorButton = new CustomTextureButton.Builder()
				.position(x + 95, y + 160)
				.size(20, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(42, 15, 42, 15)
				.textureSize(5, 5)
				.message(Component.empty())
				.onPress(btn -> showColorPicker("exterior"))
				.build();
		addRenderableWidget(exteriorColorButton);

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

		if (colorPickerVisible) showColorPicker(colorTarget);
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
		float complexity = (creatorDamage * 8.0f) + (creatorSize * 5.0f) + (creatorSpeed * 4.0f) + (creatorArmorPen * 1.5f);
		creatorCast = Mth.clamp(Math.round(8.0f + (complexity / 6.0f)), 5, 120);
		creatorCooldown = Mth.clamp(Math.round(20.0f + (complexity / 3.0f)), 10, 300);
		dummyKiCost = Math.max(5, Math.round(12.0f + complexity));
		dummyTpCost = Math.max(10, Math.round(100.0f + (complexity * 3.0f)));
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
		hideColorPicker();
		colorTarget = target;
		colorPickerVisible = true;
		int x = getUiWidth() / 2 - 70;
		int y = getUiHeight() / 2 - 106;
		int sliderX = x + 18;
		int sliderY = y + 157;
		int sliderWidth = 90;


		int color = "interior".equals(target) ? creatorColorInterior : creatorColorExterior;
		float[] hsv = ColorUtils.rgbToHsv((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF);

		hueSlider = new ColorSlider(sliderX, sliderY, sliderWidth, 8, 0, 360, Math.round(hsv[0]), tr("gui.dragonminez.customization.hue"), v -> updateColorFromSliders());
		saturationSlider = new ColorSlider(sliderX, sliderY + 10, sliderWidth, 8, 0, 100, Math.round(hsv[1]), tr("gui.dragonminez.customization.saturation"), v -> updateColorFromSliders());
		valueSlider = new ColorSlider(sliderX, sliderY + 20, sliderWidth, 8, 0, 100, Math.round(hsv[2]), tr("gui.dragonminez.customization.value"), v -> updateColorFromSliders());
		saturationSlider.setCurrentHue(hueSlider.getValue());
		valueSlider.setCurrentHue(hueSlider.getValue());
		valueSlider.setCurrentSaturation(saturationSlider.getValue());

		hexField = new EditBox(this.font, sliderX, sliderY + 30, sliderWidth, 12, tr("gui.dragonminez.common.hex"));
		hexField.setValue(ColorUtils.hsvToHex(hueSlider.getValue(), saturationSlider.getValue(), valueSlider.getValue()));
		hexField.setResponder(this::onHexChanged);

		addRenderableWidget(hueSlider);
		addRenderableWidget(saturationSlider);
		addRenderableWidget(valueSlider);
		addRenderableWidget(hexField);
	}

	private void hideColorPicker() {
		colorPickerVisible = false;
		if (hueSlider != null) removeWidget(hueSlider);
		if (saturationSlider != null) removeWidget(saturationSlider);
		if (valueSlider != null) removeWidget(valueSlider);
		if (hexField != null) removeWidget(hexField);
		hueSlider = null;
		saturationSlider = null;
		valueSlider = null;
		hexField = null;
	}

	private void onHexChanged(String value) {
		if (updatingFromHex || value == null) return;
		String hex = value.startsWith("#") ? value : "#" + value;
		if (hex.length() != 7 || !hex.substring(1).matches("[0-9a-fA-F]{6}")) return;
		float[] hsv = ColorUtils.hexToHsv(hex);
		hueSlider.setValue(Math.round(hsv[0]));
		saturationSlider.setValue(Math.round(hsv[1]));
		valueSlider.setValue(Math.round(hsv[2]));
		updateColorFromSliders();
	}

	private void updateColorFromSliders() {
		if (hueSlider == null || saturationSlider == null || valueSlider == null) return;
		saturationSlider.setCurrentHue(hueSlider.getValue());
		valueSlider.setCurrentHue(hueSlider.getValue());
		valueSlider.setCurrentSaturation(saturationSlider.getValue());
		String hex = ColorUtils.hsvToHex(hueSlider.getValue(), saturationSlider.getValue(), valueSlider.getValue());
		int color = ColorUtils.hexToInt(hex);
		if ("interior".equals(colorTarget)) creatorColorInterior = color;
		else creatorColorExterior = color;
		updatingFromHex = true;
		if (hexField != null && !hexField.isFocused()) hexField.setValue(hex);
		updatingFromHex = false;
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
				creatorColorExterior
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
		drawCenteredStringWithBorder(graphics, tr("gui.dragonminez.skills.creator.title"), x + 70, y + 10, 0xFFFFD700);

		int rowY = y + 39;
		drawCenteredStringWithBorder(graphics, tr("gui.dragonminez.skills.creator.type").append(": ").append(tr("technique.type." + creatorType.name().toLowerCase(Locale.ROOT))), x + 70, rowY, 0xFFFFFFFF);
		rowY += 15;
		drawCenteredStringWithBorder(graphics, tr("gui.dragonminez.skills.creator.utility").append(": ").append(tr("technique.utility." + creatorUtility.name().toLowerCase(Locale.ROOT))), x + 70, rowY, allowsUtility(creatorType) ? 0xFFFFFFFF : 0xFF777777);
		rowY += 15;
		drawCenteredStringWithBorder(graphics, tr("gui.dragonminez.technique.damage").append(": ").append(txt(getDamageHealingExpression())), x + 70, rowY, 0xFFFFFFFF);
		rowY += 15;
		drawCenteredStringWithBorder(graphics, tr("gui.dragonminez.technique.size").append(": ").append(txt(String.format(Locale.US, "%.1f", creatorSize))), x + 70, rowY, 0xFFFFFFFF);
		rowY += 15;
		drawCenteredStringWithBorder(graphics, tr("gui.dragonminez.technique.speed").append(": ").append(txt(String.format(Locale.US, "%.1f", creatorSpeed))), x + 70, rowY, 0xFFFFFFFF);
		rowY += 15;
		drawCenteredStringWithBorder(graphics, tr("gui.dragonminez.technique.armor_pen").append(": ").append(txt(String.valueOf(creatorArmorPen))), x + 70, rowY, 0xFFFFFFFF);
		rowY += 15;
		drawCenteredStringWithBorder(graphics, tr("gui.dragonminez.technique.cast_time").append(": ").append(txt(String.valueOf(creatorCast))), x + 70, rowY, 0xFFFFFFFF);
		rowY += 15;
		drawCenteredStringWithBorder(graphics, tr("gui.dragonminez.technique.cooldown").append(": ").append(txt(String.valueOf(creatorCooldown))), x + 70, rowY, 0xFFFFFFFF);

		drawCenteredStringWithBorder(graphics, tr("gui.dragonminez.skills.creator.color.interior"), x + 36, y + 155, 0xFFCCCCCC);
		drawCenteredStringWithBorder(graphics, tr("gui.dragonminez.skills.creator.color.exterior"), x + 105, y + 155, 0xFFCCCCCC);
		graphics.fill(x + 30, y + 165, x + 42, y + 178, 0xFF000000 | creatorColorInterior);
		graphics.fill(x + 99, y + 165, x + 111, y + 178, 0xFF000000 | creatorColorExterior);
		drawStringWithBorder(graphics, tr("gui.dragonminez.skills.creator.ki_cost_label"), x + 14, y + 180, 0xFFDDDDDD);
		drawCenteredStringWithBorder(graphics, txt(COST_NUMBER_FORMAT.format(dummyKiCost)), x + 98, y + 180, 0xFFDDDDDD);
		drawStringWithBorder(graphics, tr("gui.dragonminez.skills.creator.tp_cost_label"), x + 14, y + 190, 0xFFDDDDDD);
		drawCenteredStringWithBorder(graphics, txt(COST_NUMBER_FORMAT.format(dummyTpCost)), x + 98, y + 190, 0xFFDDDDDD);

		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		if (colorPickerVisible) {
			int x = getUiWidth() / 2 - 70;
			int y = getUiHeight() / 2 - 106;
			int pickerX = x + 18;
			int pickerY = y + 157;
			int pickerW = 90;
			int pickerH = 42;

			boolean insidePicker = uiMouseX >= pickerX - 4 && uiMouseX <= pickerX + pickerW + 4
					&& uiMouseY >= pickerY - 4 && uiMouseY <= pickerY + pickerH + 4;
			boolean insideColorButtons = (uiMouseX >= x + 26 && uiMouseX <= x + 46 && uiMouseY >= y + 166 && uiMouseY <= y + 186)
					|| (uiMouseX >= x + 95 && uiMouseX <= x + 115 && uiMouseY >= y + 166 && uiMouseY <= y + 186);

			if (!insidePicker && !insideColorButtons) {
				hideColorPicker();
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void drawStringWithBorder(GuiGraphics graphics, Component text, int x, int y, int textColor) {
		int borderColor = 0xFF000000;
		graphics.drawString(this.font, text, x + 1, y, borderColor, false);
		graphics.drawString(this.font, text, x - 1, y, borderColor, false);
		graphics.drawString(this.font, text, x, y + 1, borderColor, false);
		graphics.drawString(this.font, text, x, y - 1, borderColor, false);
		graphics.drawString(this.font, text, x, y, textColor, false);
	}

	private void drawCenteredStringWithBorder(GuiGraphics graphics, Component text, int centerX, int y, int textColor) {
		int textWidth = this.font.width(text);
		drawStringWithBorder(graphics, text, centerX - (textWidth / 2), y, textColor);
	}

	private String getDamageHealingExpression() {
		double baseKiDamage = 0.0;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			final double[] value = {0.0};
			StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> value[0] = data.getKiDamage());
			baseKiDamage = value[0];
		}
		return String.format(Locale.US, "%.1f * %.1f", baseKiDamage, creatorDamage);
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

