package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ColorSlider;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TechniqueCreatorScreen extends ScaledScreen {
	private static final ResourceLocation MENU_NPC = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menunpc.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final NumberFormat COST_NUMBER_FORMAT = NumberFormat.getIntegerInstance(new Locale("es", "ES"));

	private static final int PANEL_W = 345;
	private static final int PANEL_H = 273;

	private final Screen parent;

	private int panelX, panelY;

	private static final int HOLD_DELAY = 5;
	private static final int HOLD_INTERVAL = 2;
	private Runnable heldAdjuster;
	private int holdTicks;

	private EditBox nameField;
	private EditBox hexField;
	private CustomTextureButton interiorColorButton;
	private CustomTextureButton exteriorColorButton;
	private CustomTextureButton outlineColorButton;
	private CustomTextureButton utilityLeft, utilityRight;
	private CustomTextureButton sizeLeft, sizeRight;
	private CustomTextureButton speedLeft, speedRight;
	private CustomTextureButton armorLeft, armorRight;
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
	private float creatorDamage = KiAttackData.getDefaultDamageForType(KiAttackData.KiType.SMALL_BALL);
	private float creatorSize = KiAttackData.getDefaultSizeForType(KiAttackData.KiType.SMALL_BALL);
	private float creatorSpeed = 1.0f;
	private int creatorArmorPen = 0;
	private int creatorCast = 20;
	private int creatorCooldown = 20;
	private float kiCost = 25;
	private float tpCost = 120;
	private int creatorColorInterior = 0xFFFFFF;
	private int creatorColorExterior = 0x00AEEF;
	private int creatorColorOutline = 0xFFFFFF;
	private KiAttackData.SecondaryEffectType creatorSecondaryType = KiAttackData.SecondaryEffectType.NONE;
	private KiAttackData.AffectedStat creatorAffectedStat = KiAttackData.AffectedStat.STR;
	private int creatorSecondaryIntensity = KiAttackData.MIN_SECONDARY_INTENSITY;
	private int creatorSecondaryDuration = KiAttackData.MIN_SECONDARY_DURATION;

	public TechniqueCreatorScreen(Screen parent) {
		super(Component.translatable("gui.dragonminez.skills.creator.title"));
		this.parent = parent;
		this.creatorName = Component.translatable("gui.dragonminez.skills.new_skill").getString();
	}

	@Override
	protected int getMinGuiWidth() {
		return 365;
	}

	@Override
	protected int getMinGuiHeight() {
		return 293;
	}

	@Override
	protected void init() {
		super.init();
		if (!auraDefaultsApplied) applyAuraDefaults();
		recomputeDerivedValues();
		clearWidgets();

		panelX = (getUiWidth() - PANEL_W) / 2;
		panelY = (getUiHeight() - PANEL_H) / 2;

		int r1 = panelY + 32;
		int r2 = panelY + 46;
		int r3 = panelY + 58;
		int r4 = panelY + 70;

		nameField = new EditBox(this.font, panelX + 138, panelY + 30, 70, 12, Component.empty());
		nameField.setMaxLength(24);
		nameField.setValue(creatorName);
		nameField.setResponder(v -> creatorName = v);
		addRenderableWidget(nameField);

		addRenderableWidget(createArrowButton(panelX + 16, r1 + 2, true, btn -> setCreatorType(prevEnum(creatorType, KiAttackData.KiType.values()))));
		addRenderableWidget(createArrowButton(panelX + 118, r1 + 2, false, btn -> setCreatorType(nextEnum(creatorType, KiAttackData.KiType.values()))));

		utilityLeft = createArrowButton(panelX + 16, r2 + 2, true, btn -> toggleUtility());
		utilityRight = createArrowButton(panelX + 118, r2 + 2, false, btn -> toggleUtility());
		addRenderableWidget(utilityLeft);
		addRenderableWidget(utilityRight);
		updateUtilityArrowsVisibility();

		interiorColorButton = createSwatchButton(panelX + 300, r1 - 2, "interior");
		exteriorColorButton = createSwatchButton(panelX + 300, r2 - 2, "exterior");
		outlineColorButton = createSwatchButton(panelX + 300, r3 - 2, "outline");
		addRenderableWidget(interiorColorButton);
		addRenderableWidget(exteriorColorButton);
		addRenderableWidget(outlineColorButton);

		int beLeft = panelX + 18;
		int beRight = panelX + 150;
		addRenderableWidget(arrow(beLeft, panelY + 128, true, () -> adjustDamage(false)));
		addRenderableWidget(arrow(beRight, panelY + 128, false, () -> adjustDamage(true)));

		sizeLeft = arrow(beLeft, panelY + 148, true, () -> adjustSize(false));
		sizeRight = arrow(beRight, panelY + 148, false, () -> adjustSize(true));
		addRenderableWidget(sizeLeft); addRenderableWidget(sizeRight);

		speedLeft = arrow(beLeft, panelY + 168, true, () -> adjustSpeed(false));
		speedRight = arrow(beRight, panelY + 168, false, () -> adjustSpeed(true));
		addRenderableWidget(speedLeft); addRenderableWidget(speedRight);

		armorLeft = arrow(beLeft, panelY + 188, true, () -> adjustArmor(false));
		armorRight = arrow(beRight, panelY + 188, false, () -> adjustArmor(true));
		addRenderableWidget(armorLeft); addRenderableWidget(armorRight);

		updateAdjusterVisibility();

		int seLeft = panelX + 192;
		int seRight = panelX + 326;
		addRenderableWidget(createArrowButton(seLeft, panelY + 128 + 2, true, btn -> cycleSecondaryType()));
		addRenderableWidget(createArrowButton(seRight, panelY + 128 + 2, false, btn -> cycleSecondaryType()));
		addRenderableWidget(createArrowButton(seLeft, panelY + 148 + 2, true, btn -> { creatorAffectedStat = prevEnum(creatorAffectedStat, KiAttackData.AffectedStat.values()); recomputeDerivedValues(); }));
		addRenderableWidget(createArrowButton(seRight, panelY + 148 + 2, false, btn -> { creatorAffectedStat = nextEnum(creatorAffectedStat, KiAttackData.AffectedStat.values()); recomputeDerivedValues(); }));
		addRenderableWidget(arrow(seLeft, panelY + 168, true, () -> adjustIntensity(false)));
		addRenderableWidget(arrow(seRight, panelY + 168, false, () -> adjustIntensity(true)));
		addRenderableWidget(arrow(seLeft, panelY + 188, true, () -> adjustDuration(false)));
		addRenderableWidget(arrow(seRight, panelY + 188, false, () -> adjustDuration(true)));

		int btnY = getUiHeight() - 28;
		addRenderableWidget(new TexturedTextButton.Builder()
				.position(panelX + PANEL_W / 2 - 78, btnY)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.skills.create_skill"))
				.onPress(btn -> createSkill())
				.build());

		addRenderableWidget(new TexturedTextButton.Builder()
				.position(panelX + PANEL_W / 2 + 4, btnY)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.hair_editor.cancel"))
				.onPress(btn -> onClose())
				.build());

		initColorPickerSliders();

		if (colorPickerVisible) showColorPicker(colorTarget);
	}

	private int pickerOriginX() { return panelX + 232; }
	private int pickerOriginY() { return panelY + 120; }

	private void initColorPickerSliders() {
		int sliderX = pickerOriginX();
		int sliderY = pickerOriginY();
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

	private CustomTextureButton arrow(int x, int textY, boolean left, Runnable repeatable) {
		return createArrowButton(x, textY + 2, left, btn -> beginHold(repeatable));
	}

	private void beginHold(Runnable action) {
		this.heldAdjuster = action;
		this.holdTicks = 0;
		action.run();
	}

	@Override
	public void tick() {
		super.tick();
		if (heldAdjuster != null) {
			holdTicks++;
			if (holdTicks >= HOLD_DELAY && (holdTicks - HOLD_DELAY) % HOLD_INTERVAL == 0) heldAdjuster.run();
		}
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		heldAdjuster = null;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void adjustDamage(boolean inc) {
		float step = hasShiftDown() ? 0.25f : 0.05f;
		creatorDamage = Mth.clamp(creatorDamage + (inc ? step : -step),
				KiAttackData.getMinDamageForType(creatorType), KiAttackData.getMaxDamageForType(creatorType));
		recomputeDerivedValues();
	}

	private void adjustSize(boolean inc) {
		if (!KiAttackData.usesCustomSize(creatorType)) return;
		float step = hasShiftDown() ? 5.0f : 0.5f;
		creatorSize = Mth.clamp(creatorSize + (inc ? step : -step), KiAttackData.getMinSizeForType(creatorType), KiAttackData.getMaxSizeForType(creatorType));
		recomputeDerivedValues();
	}

	private void adjustSpeed(boolean inc) {
		if (!KiAttackData.usesCustomSpeed(creatorType)) return;
		float step = hasShiftDown() ? 0.5f : 0.1f;
		creatorSpeed = Mth.clamp(creatorSpeed + (inc ? step : -step),
				KiAttackData.getMinSpeedForType(creatorType), KiAttackData.getMaxSpeedForType(creatorType));
		recomputeDerivedValues();
	}

	private void adjustArmor(boolean inc) {
		if (!KiAttackData.usesCustomArmorPen(creatorType)) return;
		int step = hasShiftDown() ? 5 : 1;
		creatorArmorPen = Mth.clamp(creatorArmorPen + (inc ? step : -step), 0, KiAttackData.getMaxArmorPenForType(creatorType));
		recomputeDerivedValues();
	}

	private void adjustIntensity(boolean inc) {
		int step = hasShiftDown() ? 25 : 5;
		creatorSecondaryIntensity = Mth.clamp(creatorSecondaryIntensity + (inc ? step : -step), KiAttackData.MIN_SECONDARY_INTENSITY, KiAttackData.MAX_SECONDARY_INTENSITY);
		recomputeDerivedValues();
	}

	private void adjustDuration(boolean inc) {
		int step = hasShiftDown() ? 4 : 1;
		creatorSecondaryDuration = Mth.clamp(creatorSecondaryDuration + (inc ? step : -step), KiAttackData.MIN_SECONDARY_DURATION, KiAttackData.MAX_SECONDARY_DURATION);
		recomputeDerivedValues();
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

	private CustomTextureButton createSwatchButton(int x, int y, String target) {
		return new CustomTextureButton.Builder()
				.position(x, y)
				.size(12, 12)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(42, 15, 42, 15)
				.textureSize(5, 5)
				.message(Component.empty())
				.onPress(btn -> showColorPicker(target))
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
		return KiAttackData.allowsHealUtility(type);
	}

	private void updateAdjusterVisibility() {
		boolean useSize = KiAttackData.usesCustomSize(creatorType);
		if (sizeLeft != null) sizeLeft.visible = useSize;
		if (sizeRight != null) sizeRight.visible = useSize;

		boolean useSpeed = KiAttackData.usesCustomSpeed(creatorType);
		if (speedLeft != null) speedLeft.visible = useSpeed;
		if (speedRight != null) speedRight.visible = useSpeed;

		boolean useArmor = KiAttackData.usesCustomArmorPen(creatorType);
		if (armorLeft != null) armorLeft.visible = useArmor;
		if (armorRight != null) armorRight.visible = useArmor;
	}

	private void setCreatorType(KiAttackData.KiType newType) {
		creatorType = newType;
		creatorUtility = KiAttackData.Utility.DAMAGE;
		creatorDamage = KiAttackData.getDefaultDamageForType(creatorType);
		creatorSize = KiAttackData.getDefaultSizeForType(creatorType);
		creatorSpeed = KiAttackData.getDefaultSpeedForType(creatorType);
		creatorArmorPen = KiAttackData.getDefaultArmorPenForType(creatorType);
		updateUtilityArrowsVisibility();
		updateAdjusterVisibility();
		recomputeDerivedValues();
	}

	private void cycleSecondaryType() {
		KiAttackData.SecondaryEffectType valid = creatorUtility == KiAttackData.Utility.HEAL
				? KiAttackData.SecondaryEffectType.BUFF
				: KiAttackData.SecondaryEffectType.DEBUFF;
		creatorSecondaryType = creatorSecondaryType == KiAttackData.SecondaryEffectType.NONE
				? valid
				: KiAttackData.SecondaryEffectType.NONE;
		recomputeDerivedValues();
	}

	private void recomputeDerivedValues() {
		if (creatorSecondaryType != KiAttackData.SecondaryEffectType.NONE) {
			boolean ok = (creatorSecondaryType == KiAttackData.SecondaryEffectType.BUFF && creatorUtility == KiAttackData.Utility.HEAL)
					|| (creatorSecondaryType == KiAttackData.SecondaryEffectType.DEBUFF && creatorUtility == KiAttackData.Utility.DAMAGE);
			if (!ok) creatorSecondaryType = KiAttackData.SecondaryEffectType.NONE;
		}

		float[] normalized = KiAttackData.normalizeStatsForType(creatorType, creatorDamage, creatorSize, creatorSpeed, creatorArmorPen);
		creatorDamage = normalized[0];
		creatorSize = normalized[1];
		creatorSpeed = normalized[2];
		creatorArmorPen = Math.round(normalized[3]);

		KiAttackData preview = buildPreviewTechnique(normalized);
		preview.calculateDerivedValues();

		tpCost = Math.max(0, Math.round(preview.getTpCost()));
		creatorCast = preview.getActualCastTime();
		creatorCooldown = preview.getCooldown();
		kiCost = computePreviewKiCost(preview);
	}

	private KiAttackData buildPreviewTechnique(float[] normalized) {
		KiAttackData ki = new KiAttackData();
		ki.setKiType(creatorType);
		ki.setUtility(allowsUtility(creatorType) ? creatorUtility : KiAttackData.Utility.DAMAGE);
		ki.setDamageMultiplier(normalized[0]);
		ki.setSize(normalized[1]);
		ki.setSpeed(normalized[2]);
		ki.setArmorPenetration(Math.round(normalized[3]));
		ki.setSecondaryEffectType(creatorSecondaryType);
		if (creatorSecondaryType != KiAttackData.SecondaryEffectType.NONE) {
			ki.setAffectedStat(creatorAffectedStat);
			ki.setSecondaryIntensity(creatorSecondaryIntensity);
			ki.setSecondaryDuration(creatorSecondaryDuration);
		}
		return ki;
	}

	private float computePreviewKiCost(KiAttackData preview) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return 0f;
		final float[] cost = {0f};
		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> cost[0] = (float) preview.getCalculatedCost(data));
		return cost[0];
	}

	private void updateUtilityArrowsVisibility() {
		boolean canUseUtility = allowsUtility(creatorType);
		if (utilityLeft != null) {
			utilityLeft.visible = canUseUtility;
			utilityLeft.active = canUseUtility;
		}
		if (utilityRight != null) {
			utilityRight.visible = canUseUtility;
			utilityRight.active = canUseUtility;
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
		float[] normalized = KiAttackData.normalizeStatsForType(creatorType, creatorDamage, creatorSize, creatorSpeed, creatorArmorPen);
		String defaultName = tr("gui.dragonminez.skills.new_skill").getString();
		String finalName = creatorName == null ? defaultName : creatorName.trim();
		if (finalName.isEmpty()) finalName = defaultName;

		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			String generatedId = com.dragonminez.common.stats.techniques.TechniqueData.generateId(mc.player.getName().getString(), finalName);
			boolean duplicate = StatsProvider.get(StatsCapability.INSTANCE, mc.player)
					.map(data -> data.getTechniques().getUnlockedTechniques().containsKey(generatedId))
					.orElse(false);
			if (duplicate) {
				mc.player.displayClientMessage(tr("gui.dragonminez.skills.creator.duplicate", finalName), true);
				return;
			}
		}

		NetworkHandler.INSTANCE.sendToServer(new CreateTechniqueC2S(
				finalName,
				creatorType.name(),
				creatorUtility.name(),
				normalized[0],
				normalized[2],
				normalized[1],
				Math.round(normalized[3]),
				creatorCast,
				creatorCooldown,
				creatorColorInterior,
				creatorColorExterior,
				creatorColorOutline,
				creatorSecondaryType.name(),
				creatorSecondaryType == KiAttackData.SecondaryEffectType.NONE ? "" : creatorAffectedStat.name(),
				creatorSecondaryIntensity,
				creatorSecondaryDuration
		));
		onClose();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));
		beginUiScale(graphics);

		graphics.blit(MENU_NPC, panelX, panelY, 0, 0, PANEL_W, PANEL_H, 512, 512);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.creator.title"), panelX + PANEL_W / 2, panelY + 12, 0xFFFFD700);

		renderHeader(graphics);
		renderBaseEffects(graphics);
		renderSecondaryEffects(graphics);

		if (colorPickerVisible) renderColorPickerBackground(graphics);

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 400.0D);
		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		graphics.pose().popPose();

		if (!colorPickerVisible) renderEffectTooltip(graphics, uiMouseX, uiMouseY);

		endUiScale(graphics);
	}

	private void renderHeader(GuiGraphics graphics) {
		int r1 = panelY + 32;
		int r2 = panelY + 46;
		int r3 = panelY + 58;
		int r4 = panelY + 70;
		int leftCx = panelX + 72;
		int centerCx = panelX + 172;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.skills.creator.type").append(": ").append(tr("technique.type." + creatorType.name().toLowerCase(Locale.ROOT))),
				leftCx, r1, 0xFFFFFFFF);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.skills.creator.utility").append(": ").append(tr("technique.utility." + creatorUtility.name().toLowerCase(Locale.ROOT))),
				leftCx, r2, allowsUtility(creatorType) ? 0xFFFFFFFF : 0xFF777777);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.skills.creator.ki_cost_label").append(" ").append(txt(COST_NUMBER_FORMAT.format(kiCost))),
				centerCx, r2, 0xFFDDDDDD);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.cooldown").append(": ").append(txt(String.valueOf(creatorCooldown))),
				centerCx, r3, 0xFFDDDDDD);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.skills.creator.tp_cost_label").append(" ").append(txt(COST_NUMBER_FORMAT.format(tpCost))),
				centerCx, r4, 0xFFDDDDDD);

		drawColorRow(graphics, tr("gui.dragonminez.skills.creator.color.interior"), creatorColorInterior, r1);
		drawColorRow(graphics, tr("gui.dragonminez.skills.creator.color.exterior"), creatorColorExterior, r2);
		drawColorRow(graphics, tr("gui.dragonminez.skills.creator.color.outline"), creatorColorOutline, r3);
	}

	private void drawColorRow(GuiGraphics graphics, Component label, int color, int rowY) {
		TextUtil.drawStringWithBorder(graphics, this.font, label, panelX + 234, rowY, 0xFFCCCCCC);
		int sx = panelX + 300;
		int sy = rowY - 2;
		graphics.fill(sx - 1, sy - 1, sx + 13, sy + 13, 0xFF000000);
		graphics.fill(sx, sy, sx + 12, sy + 12, 0xFF000000 | color);
	}

	private void renderBaseEffects(GuiGraphics graphics) {
		int cx = panelX + 84;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.base_effects"), cx, panelY + 108, 0xFFFFD700);

		int damagePercent = Math.round(creatorDamage * 100.0f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.damage").append(": ").append(txt(damagePercent + "%")),
				cx, panelY + 128, 0xFFFFFFFF);

		int sizeColor = KiAttackData.usesCustomSize(creatorType) ? 0xFFFFFFFF : 0xFF777777;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.size").append(": ").append(txt(String.format(Locale.US, "%.1f", creatorSize))),
				cx, panelY + 148, sizeColor);

		int speedColor = KiAttackData.usesCustomSpeed(creatorType) ? 0xFFFFFFFF : 0xFF777777;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.speed").append(": ").append(txt(String.format(Locale.US, "%.1f", creatorSpeed))),
				cx, panelY + 168, speedColor);

		int armorColor = KiAttackData.usesCustomArmorPen(creatorType) ? 0xFFFFFFFF : 0xFF777777;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.armor_pen").append(": ").append(txt(creatorArmorPen + "%")),
				cx, panelY + 188, armorColor);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.cast_time").append(": ").append(txt(String.valueOf(creatorCast / 20)).append("s")),
				cx, panelY + 208, 0xFFAACCFF);
	}

	private void renderSecondaryEffects(GuiGraphics graphics) {
		int cx = panelX + 259;
		boolean hasSec = creatorSecondaryType != KiAttackData.SecondaryEffectType.NONE;
		int active = 0xFFFFFFFF;
		int inactive = 0xFF777777;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.secondary_effects"), cx, panelY + 108, 0xFFFFD700);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.effect_type").append(": ").append(tr("gui.dragonminez.technique.effect_type." + creatorSecondaryType.name().toLowerCase(Locale.ROOT))),
				cx, panelY + 128, active);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.affected_stat").append(": ").append(tr("gui.dragonminez.technique.affected_stat." + creatorAffectedStat.name().toLowerCase(Locale.ROOT))),
				cx, panelY + 148, hasSec ? active : inactive);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.intensity").append(": ").append(txt(creatorSecondaryIntensity + "%")),
				cx, panelY + 168, hasSec ? active : inactive);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.technique.duration").append(": ").append(txt(creatorSecondaryDuration + "s")),
				cx, panelY + 188, hasSec ? active : inactive);
	}

	private void renderEffectTooltip(GuiGraphics graphics, int uiMouseX, int uiMouseY) {
		int rowY = panelY + 128;
		boolean hovering = uiMouseX >= panelX + 30 && uiMouseX <= panelX + 150
				&& uiMouseY >= rowY - 2 && uiMouseY <= rowY + 9;
		if (!hovering) return;

		boolean heal = creatorUtility == KiAttackData.Utility.HEAL && allowsUtility(creatorType);
		String valueKey = heal ? "gui.dragonminez.technique.effect.tooltip.heal" : "gui.dragonminez.technique.effect.tooltip.damage";

		List<Component> desc = new ArrayList<>();
		desc.add(tr(valueKey, getDamageHealingExpression()));
		desc.add(tr("gui.dragonminez.technique.effect.tooltip.desc"));

		TextUtil.renderAdvancedTooltip(graphics, this.font, uiMouseX, uiMouseY, getUiWidth(), getUiHeight(),
				tr("gui.dragonminez.technique.damage"), desc, null, 0xFFFFD700);
	}

	private void renderColorPickerBackground(GuiGraphics graphics) {
		var poseStack = graphics.pose();
		poseStack.pushPose();
		poseStack.translate(0.0D, 0.0D, 200.0D);
		int sliderX = pickerOriginX();
		int sliderY = pickerOriginY();
		graphics.fill(sliderX - 5, sliderY - 5, sliderX + 95, sliderY + 56, 0xCC000000);
		poseStack.popPose();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;

		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		if (colorPickerVisible) {
			int sliderX = pickerOriginX();
			int sliderY = pickerOriginY();
			int pickerW = 90;
			int pickerH = 56;

			boolean insidePicker = uiMouseX >= sliderX - 5 && uiMouseX <= sliderX + pickerW + 5
					&& uiMouseY >= sliderY - 5 && uiMouseY <= sliderY + pickerH + 5;

			int sx = panelX + 300;
			boolean insideColorButtons =
					(uiMouseX >= sx && uiMouseX <= sx + 12 && uiMouseY >= panelY + 30 && uiMouseY <= panelY + 42)
					|| (uiMouseX >= sx && uiMouseX <= sx + 12 && uiMouseY >= panelY + 44 && uiMouseY <= panelY + 56)
					|| (uiMouseX >= sx && uiMouseX <= sx + 12 && uiMouseY >= panelY + 56 && uiMouseY <= panelY + 68);

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
		double configDamage = Math.max(0.0, ConfigManager.getTechniqueConfig().getKiTypeConfig(creatorType).getDamageMultiplier());
		boolean heal = creatorUtility == KiAttackData.Utility.HEAL && allowsUtility(creatorType);
		double output = heal ? KiAttackData.HEAL_OUTPUT_FACTOR : 1.0;
		return String.format(Locale.US, "%.1f", baseKiDamage * creatorDamage * configDamage * output);
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
