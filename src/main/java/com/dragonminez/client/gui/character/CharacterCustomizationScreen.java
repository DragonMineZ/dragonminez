package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.events.ForgeClientEvents;
import com.dragonminez.client.gui.HairEditorScreen;
import com.dragonminez.client.gui.buttons.ColorSlider;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.hair.HairRenderer;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.client.util.TextureCounter;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.network.C2S.CreateCharacterC2S;
import com.dragonminez.common.network.C2S.StatsSyncC2S;
import com.dragonminez.common.network.C2S.UpdateCharacterC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.function.IntConsumer;

@OnlyIn(Dist.CLIENT)
public class CharacterCustomizationScreen extends ScaledScreen {
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");

	private static final int LEFT_PANEL_X = 12;
	private static final int LEFT_PANEL_WIDTH = 141;
	private static final int LEFT_PANEL_HEIGHT = 213;
	private static final int LEFT_PANEL_PADDING = 12;
	private static final int PREVIEW_GRID_COLUMNS = 3;
	private static final int PREVIEW_GRID_VISIBLE_ROWS = 3;
	private static final int PREVIEW_CARD_WIDTH = 34;
	private static final int PREVIEW_CARD_HEIGHT = 44;
	private static final int PREVIEW_CARD_GAP = 5;
	private float displayedProgress = 0.0f;
	private float displayedScale = 95.0f;
	private float displayedBaseY = 0.0f;
	private boolean initializedAnimations = false;
	private static final List<String> PREVIEW_FORM_TYPE_ORDER = List.of("superforms", "androidforms", "legendaryforms", "godforms");
	private final List<TabId> activeTabs = new ArrayList<>();

	private final Map<String, PanoramaRenderer> panoramaCache = new HashMap<>();

	private final Screen previousScreen;
	private final Character character;

	private int currentClassIndex = 0;

	private final List<PreviewFormOption> previewFormOptions = new ArrayList<>();
	private final Map<String, TexturedTextButton> colorButtons = new HashMap<>();
	private int previewFormIndex = -1;

	private int currentTabIndex = 0;
	private int bodyTypePreviewScrollRows = 0;
	private int hairPreviewScrollRows = 0;
	private int eyesPreviewScrollRows = 0;
	private int nosePreviewScrollRows = 0;
	private int mouthPreviewScrollRows = 0;
	private int tattooPreviewScrollRows = 0;
	private float playerRotation = 180.0f;
	private float playerPitch = 12.0f;
	private boolean isDraggingModel = false;
	private double lastMouseX = 0;
	private double lastMouseY = 0;

	private ColorSlider hueSlider;
	private ColorSlider saturationSlider;
	private ColorSlider valueSlider;
	private EditBox hexColorField;
	private boolean colorPickerVisible = false;
	private String currentColorField = "";
	private boolean isUpdatingFromCode = false;

	private enum TabId { PRESET, HAIR, EYES, FACE, BODY, AURA_CLASS }

	private enum PreviewRenderMode {
		FULL_BODY, HAIR_ONLY, EYES_ONLY, NOSE_ONLY, MOUTH_ONLY, TATTOO_ONLY
	}

	private static final class PreviewFormOption {
		private final String groupName;
		private final String formName;
		private PreviewFormOption(String groupName, String formName) {
			this.groupName = groupName;
			this.formName = formName;
		}
	}

	private static final class ActiveFormSnapshot {
		private final String activeFormGroup;
		private final String activeForm;
		private final String activeStackFormGroup;
		private final String activeStackForm;

		private ActiveFormSnapshot(String activeFormGroup, String activeForm, String activeStackFormGroup, String activeStackForm) {
			this.activeFormGroup = activeFormGroup;
			this.activeForm = activeForm;
			this.activeStackFormGroup = activeStackFormGroup;
			this.activeStackForm = activeStackForm;
		}
	}

	public CharacterCustomizationScreen(Screen previousScreen, Character character) {
		super(Component.translatable("gui.dragonminez.customization.title"));
		this.previousScreen = previousScreen;
		this.character = character;
		initializeDefaultColors();
	}

	@Override
	protected void init() {
		super.init();
		activeTabs.clear();
		activeTabs.add(TabId.PRESET);
		if (getMaxHairForCurrentState() > 0) activeTabs.add(TabId.HAIR);
		activeTabs.add(TabId.EYES);
		activeTabs.add(TabId.FACE);
		activeTabs.add(TabId.BODY);
		activeTabs.add(TabId.AURA_CLASS);

		resolveClassIndex();
		reloadPreviewFormOptions();
		refreshScreenWidgets();
	}

	private PanoramaRenderer getPanorama(String raceName) {
		return panoramaCache.computeIfAbsent(raceName, k -> {
			ResourceLocation testLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/" + k + "_panorama_0.png");
			boolean exists = false;
			if (Minecraft.getInstance().getResourceManager() != null) exists = Minecraft.getInstance().getResourceManager().getResource(testLoc).isPresent();

			ResourceLocation baseLoc = exists
					? ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/" + k + "_panorama")
					: ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/roshi");

			return new PanoramaRenderer(new CubeMap(baseLoc));
		});
	}

	protected void refreshScreenWidgets() {
		colorButtons.clear();
		clearWidgets();
		initTabWidgets();
		initNavigationButtons();
		initColorPickerSliders();
	}

	private void initTabWidgets() {
		int top = getUiHeight() / 2 - LEFT_PANEL_HEIGHT / 2 + LEFT_PANEL_PADDING;
		TabId tab = activeTabs.get(currentTabIndex);
		switch (tab) {
			case PRESET -> initPresetTab(top);
			case HAIR -> initHairTab(top);
			case EYES -> initEyesTab(top);
			case AURA_CLASS -> initAuraClassTab(top);
		}
	}

	private void initPresetTab(int top) {
		int bodyColorY = top + 12;

		boolean isBioAndroid = character.getRace().equalsIgnoreCase("bioandroid");
		int buttonCount = isBioAndroid ? 4 : 3;

		int buttonWidth = 20;
		int spacing = 10;
		int totalWidth = (buttonCount * buttonWidth) + ((buttonCount - 1) * spacing);
		int startX = LEFT_PANEL_X + (LEFT_PANEL_WIDTH / 2) - (totalWidth / 2);

		addRenderableWidget(createColorButton(startX, bodyColorY, "bodyColor"));
		addRenderableWidget(createColorButton(startX + (buttonWidth + spacing), bodyColorY, "bodyColor2"));
		addRenderableWidget(createColorButton(startX + (buttonWidth + spacing) * 2, bodyColorY, "bodyColor3"));

		if (isBioAndroid) addRenderableWidget(createColorButton(startX + (buttonWidth + spacing) * 3, bodyColorY, "hairColor"));

		if (isFemaleBody()) {
			addRenderableWidget(new ColorSlider.Builder()
					.position(LEFT_PANEL_X + 20, top + 160)
					.size(100, 8)
					.range(15, 25)
					.value(Math.round(Math.max(0.75f, Math.min(1.25f, character.getBoobScale())) * 20f))
					.message(txt("BoobScale"))
					.onValueChange(v -> {
						float newScale = v / 20f;
						if (character.getBoobScale() == newScale) return;
						character.setBoobScale(newScale);
						syncCharacter();
					})
					.build());
		}

		if (shouldRenderFormPreviewInPreset()) {
			initPreviewTransformationArrows(top + 174);
		}
	}

	private boolean isFemaleBody() {
		return Character.GENDER_FEMALE.equalsIgnoreCase(character.getGender()) || character.getBodyType() == 1;
	}

	private void initHairTab(int top) {
		int y = top + 28;
		addRenderableWidget(createColorButton(LEFT_PANEL_X + 60, y - 18, "hairColor"));
		if (HairManager.canUseHair(character)) {
			addRenderableWidget(new TexturedTextButton.Builder()
					.position(LEFT_PANEL_X + 33, getUiHeight() - 40)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(tr("gui.dragonminez.customization.edit"))
					.onPress(btn -> {
						if (this.minecraft != null) {
							this.minecraft.setScreen(new HairEditorScreen(this, character));
						}
					})
					.build());
		}

		initPreviewTransformationArrows(top + 174);
	}

	private void initPreviewTransformationArrows(int arrowsY) {
		if (previewFormIndex > 0) {
			addRenderableWidget(createArrowButton(LEFT_PANEL_X + 18, arrowsY, true, btn -> {
				changePreviewTransformation(-1);
				refreshScreenWidgets();
			}));
		}
		if (previewFormIndex >= 0 && previewFormIndex < previewFormOptions.size() - 1) {
			addRenderableWidget(createArrowButton(LEFT_PANEL_X + LEFT_PANEL_WIDTH - 28, arrowsY, false, btn -> {
				changePreviewTransformation(1);
				refreshScreenWidgets();
			}));
		}
	}

	private boolean shouldRenderFormPreviewInPreset() {
		return !activeTabs.contains(TabId.HAIR);
	}

	private void initEyesTab(int top) {
		int y = top + 8;
		addRenderableWidget(createColorButton(LEFT_PANEL_X + 33, y + 2, "eye1Color"));
		addRenderableWidget(new CustomTextureButton.Builder()
				.position(LEFT_PANEL_X + 65, y + 7)
				.size(10, 10)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(102, 0, 102, 10)
				.textureSize(10, 10)
				.message(Component.empty())
				.onPress(btn -> {
					character.setEye2Color(character.getEye1Color());
					syncCharacter();
					refreshScreenWidgets();
				})
				.build());
		addRenderableWidget(createColorButton(LEFT_PANEL_X + 85, y + 2, "eye2Color"));
	}

	private void initAuraClassTab(int top) {
		int y = top + 8;
		addRenderableWidget(createColorButton(LEFT_PANEL_X + 60, top + 136, "auraColor"));

		String[] classes = getRaceClasses();
		if (classes.length > 0) {
			character.setCharacterClass(classes[currentClassIndex]);
			if (currentClassIndex > 0) {
				addRenderableWidget(createArrowButton(LEFT_PANEL_X + 18, y + 6, true, btn -> {
					currentClassIndex--;
					character.setCharacterClass(classes[currentClassIndex]);
					syncCharacter();
					refreshScreenWidgets();
				}));
			}
			if (currentClassIndex < classes.length - 1) {
				addRenderableWidget(createArrowButton(LEFT_PANEL_X + LEFT_PANEL_WIDTH - 28, y + 6, false, btn -> {
					currentClassIndex++;
					character.setCharacterClass(classes[currentClassIndex]);
					syncCharacter();
					refreshScreenWidgets();
				}));
			}
		}
	}

	private void onTabChanged() {
		hideColorPicker();
		TabId newTab = activeTabs.get(currentTabIndex);
		if (newTab != TabId.HAIR && !(newTab == TabId.PRESET && shouldRenderFormPreviewInPreset())) {
			previewFormIndex = previewFormOptions.isEmpty() ? -1 : 0;
		}
		refreshScreenWidgets();
	}

	private void initNavigationButtons() {
		int buttonY = getUiHeight() - 28;
		int nextX = getUiWidth() - 86;
		int backX = nextX - 78;

		addRenderableWidget(new TexturedTextButton.Builder()
				.position(backX, buttonY)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.customization.back"))
				.onPress(btn -> {
					if (currentTabIndex > 0) {
						currentTabIndex--;
						onTabChanged();
						return;
					}
					closeToPrevious();
				})
				.build());

		Component rightText = currentTabIndex == activeTabs.size() - 1
				? (previousScreen == null ? tr("gui.dragonminez.customization.update") : tr("gui.dragonminez.customization.confirm"))
				: tr("gui.dragonminez.customization.next");

		addRenderableWidget(new TexturedTextButton.Builder()
				.position(nextX, buttonY)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(rightText)
				.onPress(btn -> {
					if (currentTabIndex < activeTabs.size() - 1) {
						currentTabIndex++;
						onTabChanged();
						return;
					}
					finish();
				})
				.build());
	}

	private void initColorPickerSliders() {
		int sliderX = LEFT_PANEL_X + LEFT_PANEL_WIDTH + 8;
		int sliderY = getUiHeight() / 2 - 40;
		int sliderWidth = 80;

		hueSlider = new ColorSlider.Builder()
				.position(sliderX, sliderY)
				.size(sliderWidth, 10)
				.range(0, 360)
				.value(0)
				.message(txt("Hue"))
				.onValueChange(val -> updateColorFromSliders())
				.build();

		saturationSlider = new ColorSlider.Builder()
				.position(sliderX, sliderY + 12)
				.size(sliderWidth, 10)
				.range(100, 0)
				.value(100)
				.message(txt("Saturation"))
				.onValueChange(val -> updateColorFromSliders())
				.build();

		valueSlider = new ColorSlider.Builder()
				.position(sliderX, sliderY + 24)
				.size(sliderWidth, 10)
				.range(100, 0)
				.value(100)
				.message(txt("Value"))
				.onValueChange(val -> updateColorFromSliders())
				.build();

		addRenderableWidget(hueSlider);
		addRenderableWidget(saturationSlider);
		addRenderableWidget(valueSlider);

		hexColorField = new EditBox(this.font, sliderX, sliderY + 36, sliderWidth, 12, txt("Hex"));
		hexColorField.setMaxLength(7);
		hexColorField.setResponder(this::onHexFieldChange);
		addRenderableWidget(hexColorField);

		setSlidersVisible();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderPanorama(partialTick);
		renderCinematicBars(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);
		renderLeftPanel(graphics);
		renderProgress(graphics);
		renderPlayerModel(graphics, partialTick);
		renderTabText(graphics, uiMouseX, uiMouseY);

		if (colorPickerVisible) {
			renderColorPickerBackground(graphics);
			renderColorPreviewSquare(graphics);
		}

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 400.0D);
		graphics.flush();
		RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		graphics.pose().popPose();

		endUiScale(graphics);
	}

	private void renderLeftPanel(GuiGraphics graphics) {
		int panelY = getUiHeight() / 2 - LEFT_PANEL_HEIGHT / 2;
		RenderSystem.enableBlend();
		graphics.blit(MENU_BIG, LEFT_PANEL_X, panelY, 0, 0, LEFT_PANEL_WIDTH, LEFT_PANEL_HEIGHT);
		RenderSystem.disableBlend();
	}

	private void renderProgress(GuiGraphics graphics) {
		int barW = 152;
		int barX = getUiWidth() - 164;
		int barY = getUiHeight() - 40;
		int barH = 6;

		float targetProgress = (currentTabIndex + 1) / (float) activeTabs.size();
		displayedProgress = Mth.lerp(0.15f, displayedProgress, targetProgress);
		int fillW = Mth.floor(barW * displayedProgress);

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 500.0D);

		String text = (currentTabIndex + 1) + "/" + activeTabs.size();
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, text, barX + barW / 2, barY - 12, 0xFFFFFF);

		graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFFFFFFFF);
		graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF111111);
		graphics.fill(barX, barY, barX + fillW, barY + barH, 0xFF006400);

		graphics.pose().popPose();
	}

	private void renderTabText(GuiGraphics graphics, int mouseX, int mouseY) {
		int panelY = getUiHeight() / 2 - LEFT_PANEL_HEIGHT / 2;
		int centerX = LEFT_PANEL_X + LEFT_PANEL_WIDTH / 2;
		int top = panelY + LEFT_PANEL_PADDING;
		TabId tab = activeTabs.get(currentTabIndex);

		switch (tab) {
			case PRESET -> renderPresetText(graphics, centerX, top);
			case HAIR -> renderHairText(graphics, centerX, top);
			case EYES -> renderEyesText(graphics, centerX, top);
			case FACE -> renderFaceText(graphics, centerX, top);
			case BODY -> renderBodyText(graphics, centerX, top);
			case AURA_CLASS -> renderAuraClassText(graphics, centerX, top, mouseX, mouseY);
		}
	}

	private void renderPresetText(GuiGraphics graphics, int centerX, int top) {
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.customization.body_type").getString(), centerX, top + 2, 0xFF9B9B);
		renderPreviewGrid(graphics, top + 40, 0, getCombinedBodyTypeCount(), getCurrentCombinedBodyTypeValue(), PreviewRenderMode.FULL_BODY, false, PREVIEW_GRID_VISIBLE_ROWS, bodyTypePreviewScrollRows);
		if (isFemaleBody()) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.customization.chest_size").getString() + " x" + String.format("%.2f", character.getBoobScale()), centerX, top + 150, 0xFF9B9B);
		}
		if (shouldRenderFormPreviewInPreset()) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, getCurrentPreviewTransformationName(), centerX, top + 178, 0xFFFFFF);
		}
	}

	private int getCurrentHairOrBoneValue() {
		String activeBone = character.getRenderableHeadBone();
		boolean supportsHair = HairManager.canUseHair(character);
		int hairPresets = supportsHair ? HairManager.getPresetCount() : 0;

		if (activeBone == null || activeBone.isEmpty() || activeBone.equals("hair")) {
			int hairId = character.getHairId();
			return hairId > 0 ? hairId - 1 : -1;
		}

		List<String> extraBones = getAvailableExtraHeadBonesForCurrentState();
		if (activeBone.contains("+")) {
			List<String> combos = buildHeadBoneComboOptions(supportsHair, extraBones);
			int comboIdx = combos.indexOf(activeBone);
			return comboIdx >= 0 ? hairPresets + extraBones.size() + comboIdx : -1;
		}

		int boneIndex = extraBones.indexOf(activeBone);
		if (boneIndex >= 0) return hairPresets + boneIndex;
		return 0;
	}

	private void renderHairText(GuiGraphics graphics, int centerX, int top) {
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.customization.hair").getString(), centerX, top + 2, 0xFF9B9B);
		int maxHairIndex = Math.max(0, getMaxHairForCurrentState() - 1);
		renderPreviewGrid(graphics, top + 30, 0, maxHairIndex, getCurrentHairOrBoneValue(), PreviewRenderMode.HAIR_ONLY, true, PREVIEW_GRID_VISIBLE_ROWS, hairPreviewScrollRows);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, getCurrentPreviewTransformationName(), centerX, top + 178, 0xFFFFFF);
	}

	private void renderEyesText(GuiGraphics graphics, int centerX, int top) {
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.customization.eyes").getString(), centerX, top + 2, 0xFF9B9B);
		renderPreviewGrid(graphics, top + 30, 0, Math.max(1, TextureCounter.getMaxEyesTypes(getEffectiveModelBase())), character.getEyesType(), PreviewRenderMode.EYES_ONLY, true, PREVIEW_GRID_VISIBLE_ROWS, eyesPreviewScrollRows);
	}

	private void renderFaceText(GuiGraphics graphics, int centerX, int top) {
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.customization.nose").getString(), centerX, top + 2, 0xFF9B9B);
		renderPreviewGrid(graphics, top + 20, 0, Math.max(1, TextureCounter.getMaxNoseTypes(getEffectiveModelBase())), character.getNoseType(), PreviewRenderMode.NOSE_ONLY, true, 1, nosePreviewScrollRows);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.customization.mouth").getString(), centerX, top + 74, 0xFF9B9B);
		renderPreviewGrid(graphics, top + 94, 0, Math.max(1, TextureCounter.getMaxMouthTypes(getEffectiveModelBase())), character.getMouthType(), PreviewRenderMode.MOUTH_ONLY, true, 2, mouthPreviewScrollRows);
	}

	private void renderBodyText(GuiGraphics graphics, int centerX, int top) {
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.customization.tattoo").getString(), centerX, top + 2, 0xFF9B9B);
		renderPreviewGrid(graphics, top + 30, 0, Math.max(1, TextureCounter.getMaxTattooTypes(getEffectiveModelBase())), character.getTattooType(), PreviewRenderMode.TATTOO_ONLY, false, PREVIEW_GRID_VISIBLE_ROWS, tattooPreviewScrollRows);
	}

	private void renderAuraClassText(GuiGraphics graphics, int centerX, int top, int mouseX, int mouseY) {
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.customization.class").getString(), centerX, top + 8, 0xFF9B9B);
		Component className = tr("class.dragonminez." + character.getCharacterClass());
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, className, centerX, top + 20, 0xFFFFFF);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.customization.aura").getString(), centerX, top + 124, 0xFF9B9B);
		renderBaseStatsInline(graphics, centerX, top + 44, mouseX, mouseY);
	}

	private void renderBaseStatsInline(GuiGraphics graphics, int centerX, int startY, int mouseX, int mouseY) {
		RaceStatsConfig statsConfig = ConfigManager.getRaceStats(character.getRace());
		if (statsConfig == null) return;
		RaceStatsConfig.ClassStats classStats = statsConfig.getClassStats(character.getCharacterClass());
		if (classStats == null || classStats.getBaseStats() == null || classStats.getStatScaling() == null) return;

		RaceStatsConfig.BaseStats base = classStats.getBaseStats();
		RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.base_stats").getString(), centerX, startY, 0xFF9B9B);

		String strStr = tr("gui.dragonminez.character_stats.str").getString();
		String skpStr = tr("gui.dragonminez.character_stats.skp").getString();
		String resStr = tr("gui.dragonminez.character_stats.res").getString();

		int row1Y = startY + 16;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, strStr, centerX - 40, row1Y, 0x7CFDD6);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, skpStr, centerX, row1Y, 0x7CFDD6);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, resStr, centerX + 40, row1Y, 0x7CFDD6);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, String.valueOf(base.getStrength()), centerX - 40, row1Y + 12, 0xFFFFFF);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, String.valueOf(base.getStrikePower()), centerX, row1Y + 12, 0xFFFFFF);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, String.valueOf(base.getResistance()), centerX + 40, row1Y + 12, 0xFFFFFF);

		String vitStr = tr("gui.dragonminez.character_stats.vit").getString();
		String pwrStr = tr("gui.dragonminez.character_stats.pwr").getString();
		String eneStr = tr("gui.dragonminez.character_stats.ene").getString();

		int row2Y = startY + 48;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, vitStr, centerX - 40, row2Y, 0x7CFDD6);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, pwrStr, centerX, row2Y, 0x7CFDD6);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, eneStr, centerX + 40, row2Y, 0x7CFDD6);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, String.valueOf(base.getVitality()), centerX - 40, row2Y + 12, 0xFFFFFF);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, String.valueOf(base.getKiPower()), centerX, row2Y + 12, 0xFFFFFF);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, String.valueOf(base.getEnergy()), centerX + 40, row2Y + 12, 0xFFFFFF);

		int tpY = startY + 116;
		Double tpGain = classStats.getTpGainMultiplier() != null ? classStats.getTpGainMultiplier() : 1.0;
		Double tpCost = classStats.getTpCostMultiplier() != null ? classStats.getTpCostMultiplier() : 1.0;

		Component tpGainComp = Component.empty()
				.append(Component.translatable("gui.dragonminez.character_stats.tp_multiplier").withStyle(ChatFormatting.AQUA))
				.append(Component.literal(": ").withStyle(ChatFormatting.AQUA))
				.append(Component.literal("x" + String.format(Locale.US, "%.2f", tpGain)).withStyle(ChatFormatting.YELLOW));

		Component tpCostComp = Component.empty()
				.append(Component.translatable("gui.dragonminez.character_stats.tpc").withStyle(ChatFormatting.AQUA))
				.append(Component.literal(": ").withStyle(ChatFormatting.AQUA))
				.append(Component.literal("x" + String.format(Locale.US, "%.2f", tpCost)).withStyle(ChatFormatting.YELLOW));

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tpGainComp, centerX, tpY, 0xFFFFFF);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tpCostComp, centerX, tpY + 12, 0xFFFFFF);

		Component title = null;
		List<Component> desc = new ArrayList<>();
		List<Component> extras = new ArrayList<>();
		int headerColor = 0xD71432;

		if (mouseY >= row1Y && mouseY <= row1Y + 22) {
			if (mouseX >= centerX - 55 && mouseX <= centerX - 25) {
				title = Component.translatable("gui.dragonminez.character_stats.str").withStyle(ChatFormatting.BOLD);
				desc.add(Component.translatable("gui.dragonminez.character_stats.str.desc"));
				extras.add(Component.translatable("gui.dragonminez.customization.stat.scaling").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
						.append(Component.literal(String.format(Locale.US, "%.2f", scaling.getStrengthScaling())).withStyle(ChatFormatting.GREEN)));
			} else if (mouseX >= centerX - 15 && mouseX <= centerX + 15) {
				title = Component.translatable("gui.dragonminez.character_stats.skp").withStyle(ChatFormatting.BOLD);
				desc.add(Component.translatable("gui.dragonminez.character_stats.skp.desc"));
				extras.add(Component.translatable("gui.dragonminez.customization.stat.scaling").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
						.append(Component.literal(String.format(Locale.US, "%.2f", scaling.getStrikePowerScaling())).withStyle(ChatFormatting.GREEN)));
			} else if (mouseX >= centerX + 25 && mouseX <= centerX + 55) {
				title = Component.translatable("gui.dragonminez.character_stats.res").withStyle(ChatFormatting.BOLD);
				desc.add(Component.translatable("gui.dragonminez.character_stats.res.desc"));
				extras.add(Component.translatable("gui.dragonminez.customization.stat.scaling.def").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
						.append(Component.literal(String.format(Locale.US, "%.2f", scaling.getDefenseScaling())).withStyle(ChatFormatting.GREEN)));
				extras.add(Component.translatable("gui.dragonminez.customization.stat.scaling.stm").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
						.append(Component.literal(String.format(Locale.US, "%.2f", scaling.getStaminaScaling())).withStyle(ChatFormatting.GREEN)));
				extras.add(Component.translatable("gui.dragonminez.customization.stat.regen.stm").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
						.append(Component.literal(String.format(Locale.US, "%.1f/s", classStats.getBaseSp5() * 0.2)).withStyle(ChatFormatting.YELLOW))
						.append(Component.literal(" (+" + String.format(Locale.US, "%.2f", classStats.getSp5StmScaling() * 0.2) + "/STM)").withStyle(ChatFormatting.DARK_GRAY)));
			}
		}
		else if (mouseY >= row2Y && mouseY <= row2Y + 22) {
			if (mouseX >= centerX - 55 && mouseX <= centerX - 25) {
				title = Component.translatable("gui.dragonminez.character_stats.vit").withStyle(ChatFormatting.BOLD);
				desc.add(Component.translatable("gui.dragonminez.character_stats.vit.desc"));
				extras.add(Component.translatable("gui.dragonminez.customization.stat.scaling.hp").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
						.append(Component.literal(String.format(Locale.US, "%.2f", scaling.getVitalityScaling())).withStyle(ChatFormatting.GREEN)));
				extras.add(Component.translatable("gui.dragonminez.customization.stat.regen.hp").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
						.append(Component.literal(String.format(Locale.US, "%.1f/s", classStats.getBaseHp5() * 0.2)).withStyle(ChatFormatting.YELLOW))
						.append(Component.literal(" (+" + String.format(Locale.US, "%.2f", classStats.getHp5VitScaling() * 0.2) + "/VIT)").withStyle(ChatFormatting.DARK_GRAY)));
			} else if (mouseX >= centerX - 15 && mouseX <= centerX + 15) {
				title = Component.translatable("gui.dragonminez.character_stats.pwr").withStyle(ChatFormatting.BOLD);
				desc.add(Component.translatable("gui.dragonminez.character_stats.pwr.desc"));
				extras.add(Component.translatable("gui.dragonminez.customization.stat.scaling").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
						.append(Component.literal(String.format(Locale.US, "%.2f", scaling.getKiPowerScaling())).withStyle(ChatFormatting.GREEN)));
			} else if (mouseX >= centerX + 25 && mouseX <= centerX + 55) {
				title = Component.translatable("gui.dragonminez.character_stats.ene").withStyle(ChatFormatting.BOLD);
				desc.add(Component.translatable("gui.dragonminez.character_stats.ene.desc"));
				extras.add(Component.translatable("gui.dragonminez.customization.stat.scaling.ki").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
						.append(Component.literal(String.format(Locale.US, "%.2f", scaling.getEnergyScaling())).withStyle(ChatFormatting.GREEN)));
				extras.add(Component.translatable("gui.dragonminez.customization.stat.regen.ki").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
						.append(Component.literal(String.format(Locale.US, "%.1f/s", classStats.getBaseEp5() * 0.2)).withStyle(ChatFormatting.YELLOW))
						.append(Component.literal(" (+" + String.format(Locale.US, "%.2f", classStats.getEp5EneScaling() * 0.2) + "/ENE)").withStyle(ChatFormatting.DARK_GRAY)));
			}
		}

		boolean isTooltipActive = (title != null);
		for (TexturedTextButton colorBtn : colorButtons.values()) {
			if (colorBtn != null) colorBtn.visible = !isTooltipActive;
		}

		if (title != null) {
			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, desc, extras, headerColor);
		}
	}

	private void renderPlayerModel(GuiGraphics graphics, float partialTick) {
		Player player = Minecraft.getInstance().player;
		if (player == null) return;

		ActiveFormSnapshot snapshot = captureLocalPlayerFormSnapshot(player);
		boolean previewApplied = applyPreviewTransformationToPlayer(player);

		int previewZoneLeft = LEFT_PANEL_X + LEFT_PANEL_WIDTH + 16;
		int previewZoneRight = getUiWidth() - 16;
		int baseX = previewZoneLeft + (previewZoneRight - previewZoneLeft) / 2;

		float targetScale = 95.0f;
		float targetBaseY = getUiHeight() / 2 + 112;

		TabId tab = activeTabs.get(currentTabIndex);
		if (tab == TabId.HAIR || tab == TabId.EYES || tab == TabId.FACE) {
			targetScale = 150.0f;
			targetBaseY = getUiHeight() / 2 + 246;
		}

		if (!initializedAnimations) {
			displayedScale = targetScale;
			displayedBaseY = targetBaseY;
			displayedProgress = (currentTabIndex + 1) / (float) activeTabs.size();
			initializedAnimations = true;
		}

		displayedScale = Mth.lerp(0.15f, displayedScale, targetScale);
		displayedBaseY = Mth.lerp(0.15f, displayedBaseY, targetBaseY);

		int adjustedScale = getAdjustedModelScale(player, (int) displayedScale);
		int currentBaseY = (int) displayedBaseY;

		Quaternionf pose = (new Quaternionf()).rotateZ((float) Math.PI);
		Quaternionf cameraOrientation = (new Quaternionf()).rotateX(0);
		pose.mul(cameraOrientation);

		float yBodyRot = player.yBodyRot;
		float yBodyRotO = player.yBodyRotO;
		float yRot = player.getYRot();
		float yRotO = player.yRotO;
		float xRot = player.getXRot();
		float xRotO = player.xRotO;
		float yHeadRotO = player.yHeadRotO;
		float yHeadRot = player.yHeadRot;

		player.yBodyRot = playerRotation;
		player.yBodyRotO = playerRotation;
		player.setYRot(playerRotation);
		player.yRotO = playerRotation;
		player.setXRot(playerPitch);
		player.xRotO = playerPitch;
		player.yHeadRot = playerRotation;
		player.yHeadRotO = playerRotation;

		Minecraft mc = Minecraft.getInstance();
		float sw = (float) mc.getWindow().getGuiScaledWidth();
		float sh = (float) mc.getWindow().getGuiScaledHeight();
		Matrix4f guiProjection = new Matrix4f().ortho(0, sw, sh, 0, -10000, 10000);

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 320.0D);

		InventoryScreen.renderEntityInInventory(graphics, baseX, currentBaseY, adjustedScale, pose, cameraOrientation, player);

		if (tab == TabId.AURA_CLASS) {
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			AuraRenderer.renderGuiAura(player, graphics.pose(), guiProjection, baseX, currentBaseY, adjustedScale, partialTick, true);
		}

		graphics.pose().popPose();

		if (previewApplied && snapshot != null) restoreLocalPlayerFormSnapshot(player, snapshot);

		player.yBodyRot = yBodyRot;
		player.yBodyRotO = yBodyRotO;
		player.setYRot(yRot);
		player.yRotO = yRotO;
		player.setXRot(xRot);
		player.xRotO = xRotO;
		player.yHeadRotO = yHeadRotO;
		player.yHeadRot = yHeadRot;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;

		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		if (colorPickerVisible) {
			int sliderX = LEFT_PANEL_X + LEFT_PANEL_WIDTH + 8;
			int sliderY = getUiHeight() / 2 - 40;
			int totalWidth = 126;
			int totalHeight = 56;
			boolean inside = uiMouseX >= sliderX - 5 && uiMouseX <= sliderX + totalWidth
					&& uiMouseY >= sliderY - 5 && uiMouseY <= sliderY + totalHeight;
			if (!inside) {
				hideColorPicker();
				return true;
			}
			return false;
		}

		if (button == 0 && handlePreviewGridClick(uiMouseX, uiMouseY)) {
			return true;
		}

		int previewZoneLeft = LEFT_PANEL_X + LEFT_PANEL_WIDTH + 16;
		int previewZoneRight = getUiWidth() - 16;
		int centerX = previewZoneLeft + (previewZoneRight - previewZoneLeft) / 2;
		int centerY = getUiHeight() / 2 + 112;
		if (activeTabs.get(currentTabIndex) == TabId.HAIR || activeTabs.get(currentTabIndex) == TabId.EYES || activeTabs.get(currentTabIndex) == TabId.FACE) {
			centerY = getUiHeight() / 2 + 246;
		}

		if (uiMouseX >= centerX - 90 && uiMouseX <= centerX + 90 && uiMouseY >= centerY - 370 && uiMouseY <= centerY + 28) {
			isDraggingModel = true;
			lastMouseX = uiMouseX;
			lastMouseY = uiMouseY;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		isDraggingModel = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (isDraggingModel && !colorPickerVisible) {
			double uiMouseX = toUiX(mouseX);
			double uiMouseY = toUiY(mouseY);
			double deltaX = uiMouseX - lastMouseX;
			double deltaY = uiMouseY - lastMouseY;
			playerRotation -= (float) deltaX;
			playerPitch = Mth.clamp(playerPitch + (float) deltaY, -90.0f, 90.0f);
			lastMouseX = uiMouseX;
			lastMouseY = uiMouseY;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int top = getUiHeight() / 2 - LEFT_PANEL_HEIGHT / 2 + LEFT_PANEL_PADDING;
		int direction = delta < 0 ? 1 : -1;

		TabId tab = activeTabs.get(currentTabIndex);
		switch (tab) {
			case PRESET -> {
				if (tryScrollGrid(uiMouseX, uiMouseY, top + 40, 0, getCombinedBodyTypeCount(), PREVIEW_GRID_VISIBLE_ROWS, direction, bodyTypePreviewScrollRows)) {
					bodyTypePreviewScrollRows = clampScrollRows(0, getCombinedBodyTypeCount(), PREVIEW_GRID_VISIBLE_ROWS, bodyTypePreviewScrollRows + direction);
					return true;
				}
			}
			case HAIR -> {
				int maxHair = Math.max(0, getMaxHairForCurrentState() - 1);
				if (tryScrollGrid(uiMouseX, uiMouseY, top + 30, 0, maxHair, PREVIEW_GRID_VISIBLE_ROWS, direction, hairPreviewScrollRows)) {
					hairPreviewScrollRows = clampScrollRows(0, maxHair, PREVIEW_GRID_VISIBLE_ROWS, hairPreviewScrollRows + direction);
					return true;
				}
			}
			case EYES -> {
				int maxEyes = Math.max(1, TextureCounter.getMaxEyesTypes(getEffectiveModelBase()));
				if (tryScrollGrid(uiMouseX, uiMouseY, top + 30, 0, maxEyes, PREVIEW_GRID_VISIBLE_ROWS, direction, eyesPreviewScrollRows)) {
					eyesPreviewScrollRows = clampScrollRows(0, maxEyes, PREVIEW_GRID_VISIBLE_ROWS, eyesPreviewScrollRows + direction);
					return true;
				}
			}
			case FACE -> {
				int maxNose = Math.max(1, TextureCounter.getMaxNoseTypes(getEffectiveModelBase()));
				if (tryScrollGrid(uiMouseX, uiMouseY, top + 20, 0, maxNose, 1, direction, nosePreviewScrollRows)) {
					nosePreviewScrollRows = clampScrollRows(0, maxNose, 1, nosePreviewScrollRows + direction);
					return true;
				}
				int maxMouth = Math.max(1, TextureCounter.getMaxMouthTypes(getEffectiveModelBase()));
				if (tryScrollGrid(uiMouseX, uiMouseY, top + 94, 0, maxMouth, 2, direction, mouthPreviewScrollRows)) {
					mouthPreviewScrollRows = clampScrollRows(0, maxMouth, 2, mouthPreviewScrollRows + direction);
					return true;
				}
			}
			case BODY -> {
				int maxTattoo = Math.max(1, TextureCounter.getMaxTattooTypes(getEffectiveModelBase()));
				if (tryScrollGrid(uiMouseX, uiMouseY, top + 30, 0, maxTattoo, PREVIEW_GRID_VISIBLE_ROWS, direction, tattooPreviewScrollRows)) {
					tattooPreviewScrollRows = clampScrollRows(0, maxTattoo, PREVIEW_GRID_VISIBLE_ROWS, tattooPreviewScrollRows + direction);
					return true;
				}
			}
			default -> {
			}
		}

		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256
				&& this.minecraft != null
				&& this.minecraft.player != null
				&& ConfigManager.getServerConfig().getGameplay().getForceCharacterCreation()) {
			final boolean[] creationRequired = {false};
			StatsProvider.get(StatsCapability.INSTANCE, this.minecraft.player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter()) creationRequired[0] = true;
			});
			if (creationRequired[0]) {
				ForgeClientEvents.requestCharacterCreationReopen();
				this.minecraft.setScreen(new PauseScreen(true));
				return true;
			}
		}

		if (keyCode == 256) {
			if (colorPickerVisible) {
				hideColorPicker();
				return true;
			}
			if (currentTabIndex > 0) {
				currentTabIndex--;
				onTabChanged();
				return true;
			}
			closeToPrevious();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		clearLocalPlayerTransformState();
		previewFormIndex = -1;
		previewFormOptions.clear();
		closeToPrevious();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void closeToPrevious() {
		if (this.minecraft != null) this.minecraft.setScreen(previousScreen);
	}

	private void finish() {
		if (this.minecraft != null) {
			clearAllTransformSelections(character);
			clearLocalPlayerTransformState();
			previewFormIndex = -1;
			previewFormOptions.clear();
			if (this.previousScreen == null) {
				ForgeClientEvents.markCharacterCreatedLocally();
				NetworkHandler.sendToServer(new UpdateCharacterC2S(character));
			} else {
				ForgeClientEvents.markCharacterCreatedLocally();
				NetworkHandler.sendToServer(new CreateCharacterC2S(character));
			}
			this.minecraft.setScreen(null);
		}
	}

	private void initializeDefaultColors() {
		RaceCharacterConfig config = ConfigManager.getRaceCharacter(character.getRace());
		if (config == null) return;
		boolean hasChanges = false;

		if (character.getBodyColor() == null || character.getBodyColor().isEmpty()) {
			character.setBodyColor(config.getDefaultBodyColor());
			hasChanges = true;
		}
		if (character.getBodyColor2() == null || character.getBodyColor2().isEmpty()) {
			character.setBodyColor2(config.getDefaultBodyColor2());
			hasChanges = true;
		}
		if (character.getBodyColor3() == null || character.getBodyColor3().isEmpty()) {
			character.setBodyColor3(config.getDefaultBodyColor3());
			hasChanges = true;
		}
		if (character.getHairColor() == null || character.getHairColor().isEmpty()) {
			character.setHairColor(config.getDefaultHairColor());
			hasChanges = true;
		}
		if (character.getEye1Color() == null || character.getEye1Color().isEmpty()) {
			character.setEye1Color(config.getDefaultEye1Color());
			hasChanges = true;
		}
		if (character.getEye2Color() == null || character.getEye2Color().isEmpty()) {
			character.setEye2Color(config.getDefaultEye2Color());
			hasChanges = true;
		}
		if (character.getAuraColor() == null || character.getAuraColor().isEmpty()) {
			character.setAuraColor(config.getDefaultAuraColor());
			hasChanges = true;
		}

		if (hasChanges) syncCharacter();
	}

	private void resolveClassIndex() {
		RaceStatsConfig statsConfig = ConfigManager.getRaceStats(character.getRace());
		if (statsConfig == null) return;
		List<String> classes = new ArrayList<>(statsConfig.getAllClasses());
		if (classes.isEmpty()) return;
		if (character.getCharacterClass() == null || character.getCharacterClass().isEmpty()) {
			character.setCharacterClass(classes.get(0));
			currentClassIndex = 0;
			return;
		}
		int idx = classes.indexOf(character.getCharacterClass());
		currentClassIndex = idx >= 0 ? idx : 0;
	}

	private String[] getRaceClasses() {
		RaceStatsConfig statsConfig = ConfigManager.getRaceStats(character.getRace());
		if (statsConfig == null) return new String[0];
		return statsConfig.getAllClasses().toArray(new String[0]);
	}

	private int getCombinedBodyTypeCount() {
		int maleCount = Math.max(0, TextureCounter.getMaxBodyTypes(getEffectiveModelBase(), Character.GENDER_MALE)) + 1;
		int femaleCount = character.canHaveGender() ? Math.max(0, TextureCounter.getMaxBodyTypes(getEffectiveModelBase(), Character.GENDER_FEMALE)) + 1 : 0;
		return maleCount + femaleCount - 1;
	}

	private int getCurrentCombinedBodyTypeValue() {
		int maleCount = Math.max(0, TextureCounter.getMaxBodyTypes(getEffectiveModelBase(), Character.GENDER_MALE)) + 1;
		if (character.getGender().equals(Character.GENDER_FEMALE)) {
			return character.getBodyType() + maleCount;
		}
		return character.getBodyType();
	}

	private int getMaxHairForCurrentState() {
		boolean supportsHair = HairManager.canUseHair(character);
		int count = supportsHair ? HairManager.getPresetCount() : 0;
		List<String> extraBones = getAvailableExtraHeadBonesForCurrentState();
		count += extraBones.size();
		count += buildHeadBoneComboOptions(supportsHair, extraBones).size();
		return count;
	}

	private List<String> buildHeadBoneComboOptions(boolean supportsHair, List<String> extraBones) {
		List<String> allBones = new ArrayList<>();
		if (supportsHair) allBones.add("hair");
		allBones.addAll(extraBones);
		int n = allBones.size();
		List<String> combos = new ArrayList<>();
		for (int i = 0; i < n; i++)
			for (int j = i + 1; j < n; j++)
				combos.add(allBones.get(i) + "+" + allBones.get(j));
		for (int i = 0; i < n; i++)
			for (int j = i + 1; j < n; j++)
				for (int k = j + 1; k < n; k++)
					combos.add(allBones.get(i) + "+" + allBones.get(j) + "+" + allBones.get(k));
		return combos;
	}

	private List<String> getAvailableExtraHeadBonesForCurrentState() {
		RaceCharacterConfig config = ConfigManager.getRaceCharacter(character.getRace());
		if (config == null || config.getHeadBones() == null || !character.areExtraHeadBonesEnabled()) {
			return Collections.emptyList();
		}

		List<String> extraBones = new ArrayList<>();
		for (String bone : config.getHeadBones()) {
			if (bone != null && !bone.isEmpty() && !bone.equals("hair")) {
				extraBones.add(bone);
			}
		}
		return extraBones;
	}

	private void syncCharacter() {
		NetworkHandler.sendToServer(new StatsSyncC2S(character));
	}

	private void setBodyTypeFromPreview(int value) {
		int maleCount = Math.max(0, TextureCounter.getMaxBodyTypes(getEffectiveModelBase(), Character.GENDER_MALE)) + 1;
		String newGender = Character.GENDER_MALE;
		int newBodyType = value;

		if (character.canHaveGender() && value >= maleCount) {
			newGender = Character.GENDER_FEMALE;
			newBodyType = value - maleCount;
		}

		if (character.getGender().equals(newGender) && character.getBodyType() == newBodyType) return;
		character.setGender(newGender);
		character.setBodyType(newBodyType);
		if (getEffectiveModelBase().equals("majin") && character.getHairId() != 0) character.setHairId(0);
		syncCharacter();
		refreshScreenWidgets();
	}

	private void setHairFromPreview(int value) {
		boolean supportsHair = HairManager.canUseHair(character);
		int hairPresets = supportsHair ? HairManager.getPresetCount() : 0;
		List<String> extraBones = getAvailableExtraHeadBonesForCurrentState();
		List<String> combos = buildHeadBoneComboOptions(supportsHair, extraBones);

		if (supportsHair && value < hairPresets) {
			int newHairId = value + 1;
			if (character.getHairId() == newHairId && "hair".equals(character.getActiveHeadBone())) return;
			character.setHairId(newHairId);
			character.setActiveHeadBone("hair");
			character.setHairBase(new CustomHair());
			character.setHairSSJ(new CustomHair());
			character.setHairSSJ2(new CustomHair());
			character.setHairSSJ3(new CustomHair());
		} else if (value < hairPresets + extraBones.size()) {
			String newBone = extraBones.get(value - hairPresets);
			if (character.getHairId() == 0 && newBone.equals(character.getActiveHeadBone())) return;
			character.setHairId(0);
			character.setActiveHeadBone(newBone);
		} else {
			int comboIdx = value - hairPresets - extraBones.size();
			if (comboIdx < 0 || comboIdx >= combos.size()) return;
			String comboStr = combos.get(comboIdx);
			boolean comboHasHair = Arrays.asList(comboStr.split("\\+")).contains("hair");
			int newHairId = comboHasHair ? Math.max(1, character.getHairId()) : 0;
			if (character.getHairId() == newHairId && comboStr.equals(character.getActiveHeadBone())) return;
			character.setHairId(newHairId);
			character.setActiveHeadBone(comboStr);
		}

		syncCharacter();
		refreshScreenWidgets();
	}

	private void setEyesFromPreview(int value) {
		if (character.getEyesType() == value) return;
		character.setEyesType(value);
		syncCharacter();
		refreshScreenWidgets();
	}

	private void setNoseFromPreview(int value) {
		if (character.getNoseType() == value) return;
		character.setNoseType(value);
		syncCharacter();
		refreshScreenWidgets();
	}

	private void setMouthFromPreview(int value) {
		if (character.getMouthType() == value) return;
		character.setMouthType(value);
		syncCharacter();
		refreshScreenWidgets();
	}

	private void setTattooFromPreview(int value) {
		if (character.getTattooType() == value) return;
		character.setTattooType(value);
		syncCharacter();
		refreshScreenWidgets();
	}

	private String getEffectiveModelBase() {
		String race = character.getRace().toLowerCase(Locale.ROOT);
		RaceCharacterConfig config = ConfigManager.getRaceCharacter(race);
		if (config != null && config.hasCustomModel()) return config.getCustomModel().toLowerCase(Locale.ROOT);
		return race;
	}

	private String getBodyTypeText() {
		String baseModel = getEffectiveModelBase();
		int bodyType = character.getBodyType();
		if (baseModel.equals("human") || baseModel.equals("saiyan")) {
			return bodyType == 0
					? tr("gui.dragonminez.customization.body_type.default").getString()
					: tr("gui.dragonminez.customization.body_type.custom").getString();
		}
		return tr("gui.dragonminez.customization.type", bodyType + 1).getString();
	}

	private String getHairTypeText() {
		if (HairManager.canUseHair(character)) {
			return tr("gui.dragonminez.customization.hairtype." + character.getHairId()).getString();
		}
		return tr("gui.dragonminez.customization.type", character.getHairId() + 1).getString();
	}

	private CustomTextureButton createArrowButton(int x, int y, boolean isLeft, CustomTextureButton.OnPress onPress) {
		return new CustomTextureButton.Builder()
				.position(x, y)
				.size(10, 15)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(isLeft ? 32 : 20, 0, isLeft ? 32 : 20, 14)
				.textureSize(8, 14)
				.message(Component.empty())
				.onPress(onPress)
				.build();
	}

	private TexturedTextButton createColorButton(int x, int y, String fieldName) {
		String currentColor = getColorFromField(fieldName);
		if (currentColor.isEmpty()) currentColor = "#FFFFFF";
		int colorInt = ColorUtils.hexToInt(currentColor);

		TexturedTextButton btn = new TexturedTextButton.Builder()
				.position(x, y)
				.size(20, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(42, 15, 42, 15)
				.textureSize(5, 5)
				.message(Component.empty())
				.backgroundColor(colorInt)
				.onPress(b -> showColorPicker(fieldName))
				.build();

		colorButtons.put(fieldName, btn);
		return btn;
	}

	private void onHexFieldChange(String hex) {
		if (isUpdatingFromCode) return;
		if (hex.startsWith("#")) hex = hex.substring(1);
		if (hex.length() == 6) {
			isUpdatingFromCode = true;
			try {
				float[] hsv = ColorUtils.hexToHsv("#" + hex);
				if (hueSlider != null) hueSlider.setValue((int) hsv[0]);
				if (saturationSlider != null) {
					int satValue = (int) hsv[1];
					saturationSlider.setValue(satValue == 0 ? 100 : satValue);
					saturationSlider.setCurrentHue(hsv[0]);
				}
				if (valueSlider != null) {
					int valValue = (int) hsv[2];
					valueSlider.setValue(valValue == 0 ? 100 : valValue);
					valueSlider.setCurrentHue(hsv[0]);
					valueSlider.setCurrentSaturation(hsv[1] == 0 ? 100 : hsv[1]);
				}
				applyColor("#" + hex);
			} catch (Exception ignored) {
			}
			isUpdatingFromCode = false;
		}
	}

	private void showColorPicker(String fieldName) {
		currentColorField = fieldName;
		colorPickerVisible = true;
		String currentColor = getColorFromField(fieldName);
		float[] hsv = ColorUtils.hexToHsv(currentColor);
		if (hueSlider != null) hueSlider.setValue((int) hsv[0]);
		if (saturationSlider != null) {
			int satValue = (int) hsv[1];
			saturationSlider.setValue(satValue == 0 ? 100 : satValue);
			saturationSlider.setCurrentHue(hsv[0]);
		}
		if (valueSlider != null) {
			int valValue = (int) hsv[2];
			valueSlider.setValue(valValue == 0 ? 100 : valValue);
			valueSlider.setCurrentHue(hsv[0]);
			valueSlider.setCurrentSaturation(hsv[1] == 0 ? 100 : hsv[1]);
		}
		isUpdatingFromCode = true;
		if (hexColorField != null) hexColorField.setValue(currentColor);
		isUpdatingFromCode = false;
		setSlidersVisible();
	}

	private void hideColorPicker() {
		colorPickerVisible = false;
		currentColorField = "";
		setSlidersVisible();
	}

	private void setSlidersVisible() {
		if (hueSlider != null) hueSlider.visible = colorPickerVisible;
		if (saturationSlider != null) saturationSlider.visible = colorPickerVisible;
		if (valueSlider != null) valueSlider.visible = colorPickerVisible;
		if (hexColorField != null) hexColorField.visible = colorPickerVisible;
	}

	private void updateColorFromSliders() {
		if (!colorPickerVisible || currentColorField.isEmpty()) return;
		float h = hueSlider.getValue();
		float s = saturationSlider.getValue();
		float v = valueSlider.getValue();
		saturationSlider.setCurrentHue(h);
		valueSlider.setCurrentHue(h);
		valueSlider.setCurrentSaturation(s);
		String newColor = ColorUtils.hsvToHex(h, s, v);
		isUpdatingFromCode = true;
		if (hexColorField != null && !hexColorField.isFocused()) hexColorField.setValue(newColor);
		isUpdatingFromCode = false;
		applyColor(newColor);
	}

	private String getColorFromField(String fieldName) {
		String color = switch (fieldName) {
			case "hairColor" -> character.getHairColor();
			case "bodyColor" -> character.getBodyColor();
			case "bodyColor2" -> character.getBodyColor2();
			case "bodyColor3" -> character.getBodyColor3();
			case "eye1Color" -> character.getEye1Color();
			case "eye2Color" -> character.getEye2Color();
			case "auraColor" -> character.getAuraColor();
			default -> null;
		};
		return (color != null && !color.isEmpty()) ? color : "#FFFFFF";
	}

	private void applyColor(String color) {
		switch (currentColorField) {
			case "hairColor" -> character.setHairColor(color);
			case "bodyColor" -> character.setBodyColor(color);
			case "bodyColor2" -> character.setBodyColor2(color);
			case "bodyColor3" -> character.setBodyColor3(color);
			case "eye1Color" -> character.setEye1Color(color);
			case "eye2Color" -> character.setEye2Color(color);
			case "auraColor" -> character.setAuraColor(color);
		}

		TexturedTextButton btn = colorButtons.get(currentColorField);
		if (btn != null) {
			btn.setBackgroundColor(ColorUtils.hexToInt(color));
		}

		syncCharacter();
	}

	private void renderColorPickerBackground(GuiGraphics graphics) {
		var poseStack = graphics.pose();
		poseStack.pushPose();
		poseStack.translate(0.0D, 0.0D, 200.0D);
		int sliderX = LEFT_PANEL_X + LEFT_PANEL_WIDTH + 8;
		int sliderY = getUiHeight() / 2 - 40;
		graphics.fill(sliderX - 5, sliderY - 5, sliderX + 126, sliderY + 56, 0x88000000);
		poseStack.popPose();
	}

	private void renderColorPreviewSquare(GuiGraphics graphics) {
		if (hueSlider == null) return;
		var poseStack = graphics.pose();
		poseStack.pushPose();
		poseStack.translate(0.0D, 0.0D, 200.0D);
		int sliderX = LEFT_PANEL_X + LEFT_PANEL_WIDTH + 8;
		int sliderY = getUiHeight() / 2 - 40;
		int previewX = sliderX + 85;
		int previewSize = 34;

		float h = hueSlider.getValue();
		float s = saturationSlider.getValue();
		float v = valueSlider.getValue();
		int[] rgb = ColorUtils.hsvToRgb(h, s, v);
		int color = ColorUtils.rgbToInt(rgb[0], rgb[1], rgb[2]);

		graphics.fill(previewX - 1, sliderY - 1, previewX + previewSize + 1, sliderY + previewSize + 1, 0xFFFFFFFF);
		graphics.fill(previewX, sliderY, previewX + previewSize, sliderY + previewSize, 0xFF000000 | color);
		poseStack.popPose();
	}

	private void renderPanorama(float partialTick) {
		String currentRace = character.getRace() != null ? character.getRace().toLowerCase(Locale.ROOT) : "human";
		PanoramaRenderer panorama = getPanorama(currentRace);
		panorama.render(partialTick, 1.0F);
	}

	private void renderCinematicBars(GuiGraphics guiGraphics) {
		int totalBarHeight = (int) (this.height * 0.12);
		int fadeSize = 60;
		if (totalBarHeight <= fadeSize) totalBarHeight = fadeSize + 1;
		int solidHeight = totalBarHeight - fadeSize;
		int colorSolid = 0xFF000000;
		int colorTransparent = 0x00000000;
		guiGraphics.fill(0, 0, this.width, solidHeight, colorSolid);
		guiGraphics.fillGradient(0, solidHeight, this.width, solidHeight + fadeSize, colorSolid, colorTransparent);
		int bottomBarStartY = this.height - totalBarHeight;
		guiGraphics.fillGradient(0, bottomBarStartY, this.width, bottomBarStartY + fadeSize, colorTransparent, colorSolid);
		guiGraphics.fill(0, bottomBarStartY + fadeSize, this.width, this.height, colorSolid);
	}

	protected int getAdjustedModelScale(LivingEntity player, int baseScale) {
		float currentVisualScale = getCurrentVisualModelScale(player);
		if (currentVisualScale <= 0.9375f) return baseScale;
		float normalization = 0.9375f / currentVisualScale;
		return Math.max(1, Math.round(baseScale * normalization));
	}

	private int getNormalizedPreviewScale(LivingEntity player, int baseScale) {
		float currentVisualScale = getCurrentVisualModelScale(player);
		if (currentVisualScale <= 0.0f) return baseScale;
		float normalization = 0.9375f / currentVisualScale;
		return Math.max(1, Math.round(baseScale * normalization));
	}

	private float getCurrentVisualModelScale(LivingEntity player) {
		final float[] currentVisualScale = {0.9375f};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			Character localCharacter = stats.getCharacter();
			var activeForm = localCharacter.getActiveFormData();

			float scaleX;
			float scaleY;
			if (activeForm != null) {
				Float[] formScaling = activeForm.getModelScaling();
				scaleX = getSafeScaleValue(formScaling, 0, 0.9375f);
				scaleY = getSafeScaleValue(formScaling, 1, 0.9375f);
			} else {
				Float[] characterScaling = localCharacter.getModelScaling();
				scaleX = getSafeScaleValue(characterScaling, 0, 0.9375f);
				scaleY = getSafeScaleValue(characterScaling, 1, 0.9375f);
			}

			currentVisualScale[0] = Math.max(0.1f, (scaleX + scaleY) / 2.0f);
		});
		return currentVisualScale[0];
	}

	private float getSafeScaleValue(Float[] scalingValues, int index, float fallback) {
		if (scalingValues == null || index < 0 || index >= scalingValues.length || scalingValues[index] == null || scalingValues[index] <= 0.0f) {
			return fallback;
		}
		return scalingValues[index];
	}

	private void reloadPreviewFormOptions() {
		String previousGroup = null;
		String previousForm = null;
		if (previewFormIndex >= 0 && previewFormIndex < previewFormOptions.size()) {
			PreviewFormOption previous = previewFormOptions.get(previewFormIndex);
			previousGroup = previous.groupName;
			previousForm = previous.formName;
		}
		previewFormOptions.clear();
		previewFormIndex = -1;
		previewFormOptions.add(new PreviewFormOption("", ""));

		List<TransformationsHelper.OrderedFormEntry> orderedForms = TransformationsHelper.getOrderedFormsForRace(character.getRace(), PREVIEW_FORM_TYPE_ORDER);
		for (TransformationsHelper.OrderedFormEntry entry : orderedForms) {
			if (entry == null || entry.getFormData() == null) continue;
			previewFormOptions.add(new PreviewFormOption(entry.getGroupName(), entry.getFormData().getName()));
		}

		if (previousGroup != null && previousForm != null) {
			for (int i = 0; i < previewFormOptions.size(); i++) {
				PreviewFormOption option = previewFormOptions.get(i);
				if (option.groupName.equalsIgnoreCase(previousGroup) && option.formName.equalsIgnoreCase(previousForm)) {
					previewFormIndex = i;
					return;
				}
			}
		}
		previewFormIndex = previewFormOptions.isEmpty() ? -1 : 0;
	}


	private ActiveFormSnapshot captureLocalPlayerFormSnapshot(LivingEntity player) {
		final ActiveFormSnapshot[] snapshot = new ActiveFormSnapshot[1];
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			Character currentCharacter = stats.getCharacter();
			snapshot[0] = new ActiveFormSnapshot(
					currentCharacter.getActiveFormGroup(),
					currentCharacter.getActiveForm(),
					currentCharacter.getActiveStackFormGroup(),
					currentCharacter.getActiveStackForm()
			);
		});
		return snapshot[0];
	}

	private boolean applyPreviewTransformationToPlayer(LivingEntity player) {
		if (previewFormIndex < 0 || previewFormIndex >= previewFormOptions.size()) return false;
		PreviewFormOption option = previewFormOptions.get(previewFormIndex);
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			Character currentCharacter = stats.getCharacter();
			if (option.groupName == null || option.groupName.isEmpty() || option.formName == null || option.formName.isEmpty()) {
				currentCharacter.clearActiveForm();
			} else {
				currentCharacter.setActiveForm(option.groupName, option.formName);
			}
			currentCharacter.clearActiveStackForm();
		});
		return true;
	}

	private void restoreLocalPlayerFormSnapshot(LivingEntity player, ActiveFormSnapshot snapshot) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			Character currentCharacter = stats.getCharacter();
			currentCharacter.setActiveForm(snapshot.activeFormGroup, snapshot.activeForm);
			currentCharacter.setActiveStackForm(snapshot.activeStackFormGroup, snapshot.activeStackForm);
		});
	}

	private void clearLocalPlayerTransformState() {
		var mc = Minecraft.getInstance();
		if (mc.player == null) return;
		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(stats -> clearAllTransformSelections(stats.getCharacter()));
	}

	private void clearAllTransformSelections(Character targetCharacter) {
		targetCharacter.clearActiveForm();
		targetCharacter.clearActiveStackForm();
		targetCharacter.setSelectedFormGroup("");
		targetCharacter.setSelectedForm("");
		targetCharacter.setSelectedStackFormGroup("");
		targetCharacter.setSelectedStackForm("");
	}

	private boolean handlePreviewGridClick(double uiMouseX, double uiMouseY) {
		int top = getUiHeight() / 2 - LEFT_PANEL_HEIGHT / 2 + LEFT_PANEL_PADDING;
		TabId tab = activeTabs.get(currentTabIndex);

		return switch (tab) {
			case PRESET -> handlePreviewGridSelection(uiMouseX, uiMouseY, top + 40, 0, getCombinedBodyTypeCount(), PREVIEW_GRID_VISIBLE_ROWS, bodyTypePreviewScrollRows, this::setBodyTypeFromPreview);
			case HAIR -> handlePreviewGridSelection(uiMouseX, uiMouseY, top + 30, 0, Math.max(0, getMaxHairForCurrentState() - 1), PREVIEW_GRID_VISIBLE_ROWS, hairPreviewScrollRows, this::setHairFromPreview);
			case EYES -> handlePreviewGridSelection(uiMouseX, uiMouseY, top + 30, 0, Math.max(1, TextureCounter.getMaxEyesTypes(getEffectiveModelBase())), PREVIEW_GRID_VISIBLE_ROWS, eyesPreviewScrollRows, this::setEyesFromPreview);
			case FACE -> handlePreviewGridSelection(uiMouseX, uiMouseY, top + 20, 0, Math.max(1, TextureCounter.getMaxNoseTypes(getEffectiveModelBase())), 1, nosePreviewScrollRows, this::setNoseFromPreview)
					|| handlePreviewGridSelection(uiMouseX, uiMouseY, top + 94, 0, Math.max(1, TextureCounter.getMaxMouthTypes(getEffectiveModelBase())), 2, mouthPreviewScrollRows, this::setMouthFromPreview);
			case BODY -> handlePreviewGridSelection(uiMouseX, uiMouseY, top + 30, 0, Math.max(1, TextureCounter.getMaxTattooTypes(getEffectiveModelBase())), PREVIEW_GRID_VISIBLE_ROWS, tattooPreviewScrollRows, this::setTattooFromPreview);
			default -> false;
		};
	}

	private void changePreviewTransformation(int delta) {
		if (previewFormOptions.isEmpty()) {
			previewFormIndex = -1;
			return;
		}
		int nextIndex = previewFormIndex + delta;
		if (nextIndex < 0) nextIndex = previewFormOptions.size() - 1;
		if (nextIndex >= previewFormOptions.size()) nextIndex = 0;
		previewFormIndex = nextIndex;
	}

	private String getCurrentPreviewTransformationName() {
		if (previewFormIndex < 0 || previewFormIndex >= previewFormOptions.size()) {
			return tr("forms.dragonminez.base").getString();
		}
		PreviewFormOption option = previewFormOptions.get(previewFormIndex);
		String rawName = option.formName != null ? option.formName : "";
		if (rawName.isEmpty()) return tr("forms.dragonminez.base").getString();
		String raceName = character.getRace() != null ? character.getRace().toLowerCase(Locale.ROOT) : "human";
		String translationKey = "race.dragonminez." + raceName + ".form." + option.groupName + "." + rawName;
		if (I18n.exists(translationKey)) {
			return tr(translationKey).getString();
		}
		return formatPreviewFormName(rawName);
	}

	private String formatPreviewFormName(String rawName) {
		String[] parts = rawName.replace('-', ' ').replace('_', ' ').split("\\s+");
		StringBuilder builder = new StringBuilder();
		for (String part : parts) {
			if (part == null || part.isEmpty()) continue;
			if (!builder.isEmpty()) builder.append(' ');
			builder.append(java.lang.Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) builder.append(part.substring(1).toLowerCase(Locale.ROOT));
		}
		return builder.isEmpty() ? rawName : builder.toString();
	}

	private boolean handlePreviewGridSelection(double uiMouseX, double uiMouseY, int startY, int minValue, int maxValue, int visibleRows, int scrollRows, IntConsumer onSelect) {
		if (maxValue < minValue) return false;
		int total = maxValue - minValue + 1;
		int startX = LEFT_PANEL_X + (LEFT_PANEL_WIDTH - (PREVIEW_GRID_COLUMNS * PREVIEW_CARD_WIDTH + (PREVIEW_GRID_COLUMNS - 1) * PREVIEW_CARD_GAP)) / 2;
		int firstIndex = Mth.clamp(scrollRows, 0, getMaxScrollRows(minValue, maxValue, visibleRows)) * PREVIEW_GRID_COLUMNS;
		int visible = Math.min(PREVIEW_GRID_COLUMNS * visibleRows, Math.max(0, total - firstIndex));

		for (int i = 0; i < visible; i++) {
			int value = minValue + firstIndex + i;
			int col = i % PREVIEW_GRID_COLUMNS;
			int row = i / PREVIEW_GRID_COLUMNS;
			int cardX = startX + col * (PREVIEW_CARD_WIDTH + PREVIEW_CARD_GAP);
			int cardY = startY + row * (PREVIEW_CARD_HEIGHT + PREVIEW_CARD_GAP);
			if (uiMouseX >= cardX && uiMouseX <= cardX + PREVIEW_CARD_WIDTH && uiMouseY >= cardY && uiMouseY <= cardY + PREVIEW_CARD_HEIGHT) {
				onSelect.accept(value);
				return true;
			}
		}

		return false;
	}

	private void renderPreviewGrid(GuiGraphics graphics, int startY, int minValue, int maxValue, int selectedValue, PreviewRenderMode mode, boolean headZoom, int visibleRows, int scrollRows) {
		if (maxValue < minValue) return;
		int total = maxValue - minValue + 1;
		int startX = LEFT_PANEL_X + (LEFT_PANEL_WIDTH - (PREVIEW_GRID_COLUMNS * PREVIEW_CARD_WIDTH + (PREVIEW_GRID_COLUMNS - 1) * PREVIEW_CARD_GAP)) / 2;
		int firstIndex = Mth.clamp(scrollRows, 0, getMaxScrollRows(minValue, maxValue, visibleRows)) * PREVIEW_GRID_COLUMNS;
		int visible = Math.min(PREVIEW_GRID_COLUMNS * visibleRows, Math.max(0, total - firstIndex));

		for (int i = 0; i < visible; i++) {
			int value = minValue + firstIndex + i;
			int col = i % PREVIEW_GRID_COLUMNS;
			int row = i / PREVIEW_GRID_COLUMNS;
			int cardX = startX + col * (PREVIEW_CARD_WIDTH + PREVIEW_CARD_GAP);
			int cardY = startY + row * (PREVIEW_CARD_HEIGHT + PREVIEW_CARD_GAP);
			boolean selected = value == selectedValue;

			int borderColor = selected ? 0xFFE8D0A1 : 0xFF2A2A2A;
			graphics.fill(cardX - 1, cardY - 1, cardX + PREVIEW_CARD_WIDTH + 1, cardY + PREVIEW_CARD_HEIGHT + 1, borderColor);
			graphics.fill(cardX, cardY, cardX + PREVIEW_CARD_WIDTH, cardY + PREVIEW_CARD_HEIGHT, 0xAA111111);

			int previewScale = headZoom ? 42 : 23;
			int previewY = headZoom ? cardY + PREVIEW_CARD_HEIGHT + 12 : cardY + PREVIEW_CARD_HEIGHT - 2;
			renderPreviewModelVariant(graphics, cardX, cardY, cardX + PREVIEW_CARD_WIDTH / 2, previewY, previewScale, headZoom, mode, value);
		}

		int maxScrollRows = getMaxScrollRows(minValue, maxValue, visibleRows);
		if (maxScrollRows > 0) {
			int gridHeight = visibleRows * PREVIEW_CARD_HEIGHT + (visibleRows - 1) * PREVIEW_CARD_GAP;
			int scrollbarX = startX + PREVIEW_GRID_COLUMNS * (PREVIEW_CARD_WIDTH + PREVIEW_CARD_GAP) - 4;
			int scrollbarW = 3;

			graphics.fill(scrollbarX, startY, scrollbarX + scrollbarW, startY + gridHeight, 0x88000000);

			int thumbHeight = Math.max(8, gridHeight / (maxScrollRows + 1));
			int clampedScrollRows = Mth.clamp(scrollRows, 0, maxScrollRows);
			int thumbY = startY + (clampedScrollRows * (gridHeight - thumbHeight) / maxScrollRows);

			graphics.fill(scrollbarX, thumbY, scrollbarX + scrollbarW, thumbY + thumbHeight, 0xFFFFFFFF);
		}
	}

	private boolean tryScrollGrid(double uiMouseX, double uiMouseY, int startY, int minValue, int maxValue, int visibleRows, int direction, int currentScrollRows) {
		int maxScrollRows = getMaxScrollRows(minValue, maxValue, visibleRows);
		if (maxScrollRows <= 0) return false;
		if (!isPointInsideGrid(uiMouseX, uiMouseY, startY, visibleRows)) return false;
		int next = Mth.clamp(currentScrollRows + direction, 0, maxScrollRows);
		return next != currentScrollRows;
	}

	private boolean isPointInsideGrid(double uiMouseX, double uiMouseY, int startY, int visibleRows) {
		int startX = LEFT_PANEL_X + (LEFT_PANEL_WIDTH - (PREVIEW_GRID_COLUMNS * PREVIEW_CARD_WIDTH + (PREVIEW_GRID_COLUMNS - 1) * PREVIEW_CARD_GAP)) / 2;
		int width = PREVIEW_GRID_COLUMNS * PREVIEW_CARD_WIDTH + (PREVIEW_GRID_COLUMNS - 1) * PREVIEW_CARD_GAP;
		int height = visibleRows * PREVIEW_CARD_HEIGHT + (visibleRows - 1) * PREVIEW_CARD_GAP;
		return uiMouseX >= startX && uiMouseX <= startX + width && uiMouseY >= startY && uiMouseY <= startY + height;
	}

	private int getMaxScrollRows(int minValue, int maxValue, int visibleRows) {
		if (maxValue < minValue) return 0;
		int totalEntries = maxValue - minValue + 1;
		int totalRows = Mth.ceil(totalEntries / (float) PREVIEW_GRID_COLUMNS);
		return Math.max(0, totalRows - visibleRows);
	}

	private int clampScrollRows(int minValue, int maxValue, int visibleRows, int scrollRows) {
		return Mth.clamp(scrollRows, 0, getMaxScrollRows(minValue, maxValue, visibleRows));
	}

	private void renderPreviewModelVariant(GuiGraphics graphics, int cardX, int cardY, int x, int y, int scale, boolean headZoom, PreviewRenderMode mode, int value) {
		LivingEntity player = Minecraft.getInstance().player;
		if (player == null) return;

		ActiveFormSnapshot snapshot = captureLocalPlayerFormSnapshot(player);
		boolean previewApplied = applyPreviewTransformationToPlayer(player);

		int originalBody = character.getBodyType();
		int originalHair = character.getHairId();
		int originalEyes = character.getEyesType();
		int originalNose = character.getNoseType();
		int originalMouth = character.getMouthType();
		int originalTattoo = character.getTattooType();
		String originalActiveBone = character.getActiveHeadBone();
		boolean oldHairPhysics = HairRenderer.PHYSICS_ENABLED;

		boolean[] hadTailState = {false};
		boolean[] oldTailVisible = {true};

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			hadTailState[0] = true;
			oldTailVisible[0] = stats.getStatus().isTailVisible();
			stats.getStatus().setTailVisible(false);
		});

		String originalGender = character.getGender();

		switch (mode) {
			case FULL_BODY -> {
				int maleCount = Math.max(0, TextureCounter.getMaxBodyTypes(getEffectiveModelBase(), Character.GENDER_MALE)) + 1;
				if (character.canHaveGender() && value >= maleCount) {
					character.setGender(Character.GENDER_FEMALE);
					character.setBodyType(value - maleCount);
				} else {
					character.setGender(Character.GENDER_MALE);
					character.setBodyType(value);
				}
			}
			case HAIR_ONLY -> {
				boolean supportsHair = HairManager.canUseHair(character);
				int hairPresets = supportsHair ? HairManager.getPresetCount() : 0;
				List<String> extraBones = getAvailableExtraHeadBonesForCurrentState();
				List<String> combos = buildHeadBoneComboOptions(supportsHair, extraBones);

				if (supportsHair && value < hairPresets) {
					character.setHairId(value + 1);
					character.setActiveHeadBone("hair");
				} else if (value < hairPresets + extraBones.size()) {
					character.setHairId(0);
					character.setActiveHeadBone(extraBones.get(value - hairPresets));
				} else {
					int comboIdx = value - hairPresets - extraBones.size();
					if (comboIdx >= 0 && comboIdx < combos.size()) {
						String comboStr = combos.get(comboIdx);
						boolean comboHasHair = Arrays.asList(comboStr.split("\\+")).contains("hair");
						character.setHairId(comboHasHair ? Math.max(1, originalHair) : 0);
						character.setActiveHeadBone(comboStr);
					}
				}
				character.setEyesType(0);
				character.setNoseType(0);
				character.setMouthType(0);
				character.setTattooType(0);
			}
			case EYES_ONLY -> {
				character.setHairId(0);
				character.setEyesType(value);
				character.setNoseType(0);
				character.setMouthType(0);
			}
			case NOSE_ONLY -> {
				character.setHairId(0);
				character.setEyesType(0);
				character.setNoseType(value);
				character.setMouthType(0);
			}
			case MOUTH_ONLY -> {
				character.setHairId(0);
				character.setEyesType(0);
				character.setNoseType(0);
				character.setMouthType(value);
			}
			case TATTOO_ONLY -> character.setTattooType(value);
		}
		HairRenderer.PHYSICS_ENABLED = false;

		Quaternionf pose = (new Quaternionf()).rotateZ((float) Math.PI);
		Quaternionf cameraOrientation = (new Quaternionf()).rotateX(0);
		pose.mul(cameraOrientation);

		float yBodyRotO = player.yBodyRot;
		float yBodyRotOField = player.yBodyRotO;
		float yRotO = player.getYRot();
		float xRotO = player.getXRot();
		float xRotOField = player.xRotO;
		float yHeadRotO = player.yHeadRotO;
		float yHeadRot = player.yHeadRot;
		int tickCountO = player.tickCount;
		var oldDeltaMovement = player.getDeltaMovement();

		float previewYaw = 180.0f;
		player.yBodyRot = previewYaw;
		player.yBodyRotO = previewYaw;
		player.setYRot(previewYaw);
		float previewPitch = headZoom ? 18.0f : 8.0f;
		player.setXRot(previewPitch);
		player.xRotO = previewPitch;
		player.yHeadRot = previewYaw;
		player.yHeadRotO = previewYaw;

		player.tickCount = 0;
		player.setDeltaMovement(0.0D, 0.0D, 0.0D);
		player.walkAnimation.setSpeed(0.0F);
		player.walkAnimation.position(0.0F);
		player.walkDist = 0.0F;
		player.walkDistO = 0.0F;

		int previewScale = switch (mode) {
			case HAIR_ONLY -> Math.max(26, scale - 6);
			case EYES_ONLY, NOSE_ONLY, MOUTH_ONLY -> scale + 16;
			default -> scale;
		};
		if (mode == PreviewRenderMode.EYES_ONLY || mode == PreviewRenderMode.NOSE_ONLY || mode == PreviewRenderMode.MOUTH_ONLY)
			previewScale = getNormalizedPreviewScale(player, previewScale);
		else previewScale = getAdjustedModelScale(player, previewScale);

		int previewY = 0;
		switch (mode) {
			case HAIR_ONLY -> {
				if (ConfigManager.getRaceCharacter(character.getRace()) != null) {
					if (Arrays.stream(ConfigManager.getRaceCharacter(character.getRace()).getHeadBones()).toList().contains("hair")) {
						previewY = y + 36;
					} else previewY = y + 28;
				} else previewY = y + 36;
			}
			case EYES_ONLY, NOSE_ONLY, MOUTH_ONLY -> previewY = y + 58;
			default -> previewY = y + 8;
		}

		graphics.enableScissor(
				toScreenCoord(cardX + 1),
				toScreenCoord(cardY + 1),
				toScreenCoord(cardX + PREVIEW_CARD_WIDTH - 1),
				toScreenCoord(cardY + PREVIEW_CARD_HEIGHT - 1)
		);

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 320.0D);
		InventoryScreen.renderEntityInInventory(graphics, x, previewY, previewScale, pose, cameraOrientation, player);
		graphics.pose().popPose();
		graphics.disableScissor();

		if (previewApplied && snapshot != null) restoreLocalPlayerFormSnapshot(player, snapshot);

		player.yBodyRot = yBodyRotO;
		player.yBodyRotO = yBodyRotOField;
		player.setYRot(yRotO);
		player.setXRot(xRotO);
		player.xRotO = xRotOField;
		player.yHeadRotO = yHeadRotO;
		player.yHeadRot = yHeadRot;
		player.tickCount = tickCountO;
		player.setDeltaMovement(oldDeltaMovement);
		if (hadTailState[0]) StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> stats.getStatus().setTailVisible(oldTailVisible[0]));

		character.setGender(originalGender);
		character.setBodyType(originalBody);
		character.setHairId(originalHair);
		character.setEyesType(originalEyes);
		character.setNoseType(originalNose);
		character.setMouthType(originalMouth);
		character.setTattooType(originalTattoo);
		character.setActiveHeadBone(originalActiveBone);
		HairRenderer.PHYSICS_ENABLED = oldHairPhysics;
	}
}