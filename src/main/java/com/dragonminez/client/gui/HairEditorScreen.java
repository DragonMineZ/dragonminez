package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.AxisSlider;
import com.dragonminez.client.gui.buttons.ColorSlider;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.SwitchButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.render.hair.HairRenderer;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.CustomHair.HairFace;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.hair.HairStrand;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.StatsSyncC2S;
import com.dragonminez.common.network.C2S.UpdateCustomHairC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
import com.dragonminez.common.stats.character.Character;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class HairEditorScreen extends ScaledScreen {
	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final ResourceLocation STAT_BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	private static final ResourceLocation PANORAMA_SAIYAN = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/roshi");

	private static final Set<String> DEV_NAMES = Set.of("Dev", "ImYuseix", "ezShokkoh", "narukebaransu");
	private final PanoramaRenderer panorama = new PanoramaRenderer(new CubeMap(PANORAMA_SAIYAN));

	private final Screen previousScreen;
	private final Character character;

	private enum Tab { OVERVIEW, STYLE, STRAND }
	private Tab currentTab = Tab.OVERVIEW;

	private final CustomHair[] workingHairs = new CustomHair[4];
	private final CustomHair[] backupHairs = new CustomHair[4];

	private int selectedStyle = 0;
	private HairFace currentFace = HairFace.FRONT;
	private int selectedStrandIndex = 0;

	private boolean physicsEnabled = true;
	private boolean mirrorEnabled = false;
	private boolean hairBase = true;

	private float playerRotation = 180.0f;
	private float playerPitch = 0.0f;
	private boolean isDraggingModel = false;
	private double lastMouseX = 0;
	private double lastMouseY = 0;

	private float targetZoom = 150.0f;
	private float currentZoom = 150.0f;

	private EditBox fullCodeBox;
	private EditBox individualCodeBox;

	private AxisSlider lengthSlider;
	private AxisSlider widthSlider;
	private AxisSlider xAxisSlider;
	private AxisSlider zAxisSlider;
	private AxisSlider xBendSlider;
	private AxisSlider zBendSlider;

	private boolean colorPickerVisible = false;
	private ColorSlider hueSlider;
	private ColorSlider saturationSlider;
	private ColorSlider valueSlider;
	private EditBox hexColorField;
	private TexturedTextButton colorButton;
	private boolean isUpdatingFromCode = false;

	private Component actionStatusText = Component.empty();
    private int actionStatusTimer = 0;
    private int actionStatusColor = 0xFFFFFF;

	public HairEditorScreen(Screen previousScreen, Character character) {
		super(Component.translatable("gui.dragonminez.hair_editor.title").withStyle(Style.EMPTY.withFont(DMZ_FONT)));
		this.previousScreen = previousScreen;
		this.character = character;

		if (character.getHairId() > 0) {
			int id = character.getHairId();
			String color = character.getHairColor();
			character.setHairBase(HairManager.getPresetHair(id, color).copy());
			character.setHairSSJ(HairManager.getPresetHairSSJ(id, color).copy());
			character.setHairSSJ2(HairManager.getPresetHairSSJ2(id, color).copy());
			character.setHairSSJ3(HairManager.getPresetHairSSJ3(id, color).copy());
			character.setHairId(0);
			this.hairBase = character.isRenderHairBase();
		}

		workingHairs[0] = character.getHairBase() != null ? character.getHairBase().copy() : new CustomHair();
		workingHairs[1] = character.getHairSSJ() != null ? character.getHairSSJ().copy() : workingHairs[0].copy();
		workingHairs[2] = character.getHairSSJ2() != null ? character.getHairSSJ2().copy() : workingHairs[1].copy();
		workingHairs[3] = character.getHairSSJ3() != null ? character.getHairSSJ3().copy() : workingHairs[2].copy();

		for (int i = 0; i < 4; i++) backupHairs[i] = workingHairs[i].copy();

		HairRenderer.EDITING_STRAND_ID = -1;
	}

	@Override
	protected void init() {
		super.init();
		clearWidgets();
		initGlobalUI();

		switch (currentTab) {
			case OVERVIEW -> initOverviewTab();
			case STYLE -> initStyleTab();
			case STRAND -> initStrandTab();
		}
	}

    @Override
    public void tick() {
        super.tick();
        if (actionStatusTimer > 0) {
            actionStatusTimer--;
        }
    }

    private void initGlobalUI() {
		addRenderableWidget(new TexturedTextButton.Builder()
				.position(12, 12)
				.size(74, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(currentTab == Tab.OVERVIEW ? tr("gui.dragonminez.hair_editor.cancel") : tr("gui.dragonminez.customization.back"))
				.onPress(btn -> navigateBack())
				.build());

		int rightEdge = getUiWidth() - 12;

		addRenderableWidget(new SwitchButton(rightEdge - 30, 15, mirrorEnabled, Component.empty(), btn -> {
			mirrorEnabled = !mirrorEnabled;
			((SwitchButton) btn).toggle();
			Minecraft.getInstance().player.playSound(mirrorEnabled ? MainSounds.SWITCH_ON.get() : MainSounds.SWITCH_OFF.get());
		}));
		addRenderableWidget(new SwitchButton(rightEdge - 90, 15, physicsEnabled, Component.empty(), btn -> {
			physicsEnabled = !physicsEnabled;
			((SwitchButton) btn).toggle();
			Minecraft.getInstance().player.playSound(physicsEnabled ? MainSounds.SWITCH_ON.get() : MainSounds.SWITCH_OFF.get());
		}));
		addRenderableWidget(new SwitchButton(rightEdge - 170, 15, hairBase, Component.empty(), btn -> {
			hairBase = !hairBase;
			((SwitchButton) btn).toggle();
			character.setRenderHairBase(hairBase);
			NetworkHandler.sendToServer(new StatsSyncC2S(character));
			Minecraft.getInstance().player.playSound(hairBase ? MainSounds.SWITCH_ON.get() : MainSounds.SWITCH_OFF.get());
		}));

		addRenderableWidget(new TexturedTextButton.Builder()
				.position(rightEdge - 156, getUiHeight() - 32)
				.size(74, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.hair_editor.save"))
				.onPress(btn -> saveChanges())
				.build());

		addRenderableWidget(new TexturedTextButton.Builder()
				.position(rightEdge - 74, getUiHeight() - 32)
				.size(74, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.hair_editor.hair_salon"))
				.onPress(btn -> openHairSalon())
				.build());
	}

	private void initOverviewTab() {
		HairRenderer.EDITING_STRAND_ID = -1;
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int panelY = centerY - 105;

		String[] styleNames = {"gui.dragonminez.hair_editor.style.0", "gui.dragonminez.hair_editor.style.1", "gui.dragonminez.hair_editor.style.2", "gui.dragonminez.hair_editor.style.3"};
		for (int i = 0; i < 4; i++) {
			int finalI = i;
			int yOffset = panelY + 40 + (i * 25);

			addRenderableWidget(new TexturedTextButton.Builder()
					.position(leftPanelX + 15, yOffset)
					.size(74, 20)
					.texture(STAT_BUTTONS)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(tr(styleNames[i]))
					.onPress(btn -> {
						selectedStyle = finalI;
						currentTab = Tab.STYLE;
						rebuildWidgets();
					})
					.build());

			addRenderableWidget(new CustomTextureButton.Builder()
					.position(leftPanelX + 105, yOffset + 4)
					.size(14, 11)
					.texture(STAT_BUTTONS)
					.textureCoords(10, 0, 10, 10)
					.textureSize(10, 10)
					.onPress(btn -> {
						workingHairs[finalI].clear();
						updateFullCodeBox();
					})
					.build());
		}

		fullCodeBox = new EditBox(font, leftPanelX + 15, panelY + 150, 110, 16, txt(""));
		fullCodeBox.setMaxLength(65536);
		addRenderableWidget(fullCodeBox);
		updateFullCodeBox();

		addRenderableWidget(new CustomTextureButton.Builder()
				.position(leftPanelX + 45, panelY + 170)
				.size(20, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(182, 0, 182, 20)
				.textureSize(20, 20)
				.message(Component.empty())
				.onPress(btn -> {
					fillEmptyStyles(workingHairs);
					updateFullCodeBox();
					Minecraft.getInstance().keyboardHandler.setClipboard(fullCodeBox.getValue());

					actionStatusText = tr("gui.dragonminez.hair_editor.status.copied");
                    actionStatusTimer = 60;
                    actionStatusColor = 0x55FF55;
				})
				.build());

		addRenderableWidget(new CustomTextureButton.Builder()
				.position(leftPanelX + 75, panelY + 170)
				.size(20, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(162, 0, 162, 20)
				.textureSize(20, 20)
				.message(Component.empty())
				.onPress(btn -> {
					String code = fullCodeBox.getValue();
                    if (HairManager.isFullSetCode(code)) {
                        CustomHair[] set = HairManager.fromFullSetCode(code);
                        if (set != null) {
                            System.arraycopy(set, 0, workingHairs, 0, 4);
                            syncHairToServer();

							actionStatusText = tr("gui.dragonminez.hair_editor.status.imported");
                            actionStatusTimer = 60;
                            actionStatusColor = 0x55FF55;

                        }
                    } else {
						actionStatusText = tr("gui.dragonminez.hair_editor.status.invalid");
                        actionStatusTimer = 60;
                        actionStatusColor = 0xFF5555;
                    }
				})
				.build());

        addRenderableWidget(new CustomTextureButton.Builder()
                .position(leftPanelX + 127, panelY + 153)
                .size(14, 11)
                .texture(STAT_BUTTONS)
                .textureCoords(10, 0, 10, 10)
                .textureSize(10, 10)
                .onPress(btn -> {
                    if (fullCodeBox != null) {
                        fullCodeBox.setValue("");
                    }
                })
                .build());
	}

	private void initStyleTab() {
		HairRenderer.EDITING_STRAND_ID = -1;
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int panelY = centerY - 82;

		individualCodeBox = new EditBox(font, leftPanelX + 15, panelY + 150, 110, 16, txt(""));
		individualCodeBox.setMaxLength(65536);
		addRenderableWidget(individualCodeBox);
		updateIndividualCodeBox();

		addRenderableWidget(new CustomTextureButton.Builder()
				.position(leftPanelX + 45, panelY + 175)
				.size(20, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(182, 0, 182, 20)
				.textureSize(20, 20)
				.message(Component.empty())
				.onPress(btn -> Minecraft.getInstance().keyboardHandler.setClipboard(individualCodeBox.getValue()))
				.build());

		addRenderableWidget(new CustomTextureButton.Builder()
				.position(leftPanelX + 75, panelY + 175)
				.size(20, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(162, 0, 162, 20)
				.textureSize(20, 20)
				.message(Component.empty())
				.onPress(btn -> {
					String code = individualCodeBox.getValue().trim();
					CustomHair imported;
					if (HairManager.isFullSetCode(code)) {
						CustomHair[] set = HairManager.fromFullSetCode(code);
						imported = (set != null && selectedStyle < set.length) ? set[selectedStyle] : null;
					} else {
						imported = HairManager.fromCode(code);
					}
					if (imported != null) {
						workingHairs[selectedStyle] = imported;
						syncHairToServer();
						actionStatusText = tr("gui.dragonminez.hair_editor.status.imported");
						actionStatusTimer = 60;
						actionStatusColor = 0x55FF55;
						rebuildWidgets();
					} else {
						actionStatusText = tr("gui.dragonminez.hair_editor.status.invalid");
						actionStatusTimer = 60;
						actionStatusColor = 0xFF5555;
					}
				})
				.build());
	}

	private void initStrandTab() {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int panelY = centerY - 105;

		HairStrand strand = getSelectedStrand();
		if (strand != null) {
			HairRenderer.EDITING_STRAND_ID = strand.getId();
		}

		int startY = panelY + 44;
		int sliderX = leftPanelX + 16;
		int sliderWidth = 85;

		boolean isDev = DEV_NAMES.contains(minecraft.getUser().getName());
		boolean isSSJ3 = (selectedStyle == 3);
		int maxCubes = isDev ? 10 : (isSSJ3 ? 8 : 4);
		float maxWidth = isDev ? 3.0f : 1.5f;

		float curLenMap = strand != null ? strand.getLength() + (strand.getLengthScale() - 1.0f) * 2.0f : 0;

		lengthSlider = new AxisSlider.Builder()
				.position(sliderX, startY)
				.size(sliderWidth, 11)
				.range(0, maxCubes + 1)
				.value(curLenMap)
				.step(0.5f)
				.axis(AxisSlider.Axis.Y)
				.onValueChange(val -> {
					HairStrand s = getSelectedStrand();
					if (s == null) return;
					int len = Math.min(maxCubes, (int) Math.floor(val));
					float scale = val > maxCubes ? 1.0f + (val - maxCubes) * 0.5f : 1.0f;
					s.setLength(len);
					s.setLengthScale(scale);
					applyMirror();
					syncHairToServer();
				})
				.build();

		widthSlider = new AxisSlider.Builder()
				.position(sliderX, startY + 25)
				.size(sliderWidth, 11)
				.range(0.5f, maxWidth)
				.value(strand != null ? strand.getScaleX() : 1.0f)
				.step(0.1f)
				.axis(AxisSlider.Axis.Y)
				.onValueChange(val -> {
					HairStrand s = getSelectedStrand();
					if (s != null) {
						s.setScale(val, s.getScaleY(), val);
						applyMirror();
						syncHairToServer();
					}
				})
				.build();

		xAxisSlider = new AxisSlider.Builder()
				.position(sliderX, startY + 50)
				.size(sliderWidth, 11)
				.range(-180f, 180f)
				.value(strand != null ? strand.getRotationX() : 0)
				.step(1.0f)
				.axis(AxisSlider.Axis.X)
				.onValueChange(val -> {
					HairStrand s = getSelectedStrand();
					if (s != null) {
						s.setRotation(val, s.getRotationY(), s.getRotationZ());
						applyMirror();
						syncHairToServer();
					}
				})
				.build();

		zAxisSlider = new AxisSlider.Builder()
				.position(sliderX, startY + 75)
				.size(sliderWidth, 11)
				.range(-180f, 180f)
				.value(strand != null ? strand.getRotationZ() : 0)
				.step(1.0f)
				.axis(AxisSlider.Axis.Z)
				.onValueChange(val -> {
					HairStrand s = getSelectedStrand();
					if (s != null) {
						s.setRotation(s.getRotationX(), s.getRotationY(), val);
						applyMirror();
						syncHairToServer();
					}
				})
				.build();

		xBendSlider = new AxisSlider.Builder()
				.position(sliderX, startY + 100)
				.size(sliderWidth, 11)
				.range(-180f, 180f)
				.value(strand != null ? strand.getCurveX() : 0)
				.step(1.0f)
				.axis(AxisSlider.Axis.X)
				.onValueChange(val -> {
					HairStrand s = getSelectedStrand();
					if (s != null) {
						s.setCurve(val, s.getCurveY(), s.getCurveZ());
						applyMirror();
						syncHairToServer();
					}
				})
				.build();

		zBendSlider = new AxisSlider.Builder()
				.position(sliderX, startY + 125)
				.size(sliderWidth, 11)
				.range(-180f, 180f)
				.value(strand != null ? strand.getCurveZ() : 0)
				.step(1.0f)
				.axis(AxisSlider.Axis.Z)
				.onValueChange(val -> {
					HairStrand s = getSelectedStrand();
					if (s != null) {
						s.setCurve(s.getCurveX(), s.getCurveY(), val);
						applyMirror();
						syncHairToServer();
					}
				})
				.build();

		addRenderableWidget(lengthSlider);
		addRenderableWidget(widthSlider);
		addRenderableWidget(xAxisSlider);
		addRenderableWidget(zAxisSlider);
		addRenderableWidget(xBendSlider);
		addRenderableWidget(zBendSlider);

		String currentColor = getCurrentStrandColor();
		colorButton = new TexturedTextButton.Builder()
				.position(sliderX + 30, startY + 140)
				.size(20, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(42, 15, 42, 15)
				.textureSize(5, 5)
				.message(Component.empty())
				.backgroundColor(ColorUtils.hexToInt(currentColor))
				.onPress(btn -> toggleColorPicker())
				.build();
		addRenderableWidget(colorButton);

		addRenderableWidget(new CustomTextureButton.Builder()
				.position(sliderX + 60, startY + 145)
				.size(14, 11)
				.texture(STAT_BUTTONS)
				.textureCoords(10, 0, 10, 10)
				.textureSize(10, 10)
				.onPress(btn -> {
					HairStrand s = getSelectedStrand();
					if (s != null) {
						s.setColor(null);
						applyMirrorColor(null);
						colorButton.setBackgroundColor(ColorUtils.hexToInt(getCurrentStrandColor()));
						syncHairToServer();
					}
				})
				.build());

		initColorPicker();
	}

	private HairStrand getMirrorTarget() {
		int col = selectedStrandIndex % currentFace.cols;
		int row = selectedStrandIndex / currentFace.cols;
		int mirrorCol = (currentFace.cols - 1) - col;

		HairFace mirrorFace;
		if (currentFace == HairFace.LEFT) {
			mirrorFace = HairFace.RIGHT;
		} else if (currentFace == HairFace.RIGHT) {
			mirrorFace = HairFace.LEFT;
		} else {
			if (mirrorCol == col) return null;
			mirrorFace = currentFace;
		}

		int mirrorIndex = row * currentFace.cols + mirrorCol;
		return workingHairs[selectedStyle].getStrand(mirrorFace, mirrorIndex);
	}

	private void applyMirror() {
		if (!mirrorEnabled) return;
		HairStrand source = getSelectedStrand();
		if (source == null) return;

		HairStrand target = getMirrorTarget();
		if (target != null && target != source) {
			target.setLength(source.getLength());
			target.setLengthScale(source.getLengthScale());
			target.setScale(source.getScaleX(), source.getScaleY(), source.getScaleZ());
			target.setRotation(source.getRotationX(), source.getRotationY(), -source.getRotationZ());
			target.setCurve(source.getCurveX(), source.getCurveY(), -source.getCurveZ());
			target.setColor(source.getColor());
		}
	}

	private void applyMirrorColor(String color) {
		if (!mirrorEnabled) return;
		HairStrand target = getMirrorTarget();
		if (target != null && target != getSelectedStrand()) {
			target.setColor(color);
		}
	}

	private void initColorPicker() {
		int leftPanelX = 12;
		int sliderX = leftPanelX + 150;
		int sliderY = getUiHeight() / 2 - 40;

		hueSlider = new ColorSlider.Builder()
				.position(sliderX, sliderY)
				.size(100, 10)
				.range(0, 360)
				.value(0)
				.message(txt("H"))
				.onValueChange(val -> updateColorFromSliders())
				.build();

		saturationSlider = new ColorSlider.Builder()
				.position(sliderX, sliderY + 12)
				.size(100, 10)
				.range(100, 0)
				.value(100)
				.message(txt("S"))
				.onValueChange(val -> updateColorFromSliders())
				.build();

		valueSlider = new ColorSlider.Builder()
				.position(sliderX, sliderY + 24)
				.size(100, 10)
				.range(100, 0)
				.value(100)
				.message(txt("V"))
				.onValueChange(val -> updateColorFromSliders())
				.build();

		hexColorField = new EditBox(font, sliderX, sliderY + 36, 100, 12, txt("Hex"));
		hexColorField.setMaxLength(7);
		hexColorField.setResponder(hex -> {
			if (isUpdatingFromCode) return;
			if (hex.startsWith("#")) hex = hex.substring(1);
			if (hex.length() == 6) {
				isUpdatingFromCode = true;
				try {
					float[] hsv = ColorUtils.hexToHsv("#" + hex);
					hueSlider.setValue((int) hsv[0]);
					saturationSlider.setValue(hsv[1] == 0 ? 100 : (int) hsv[1]);
					valueSlider.setValue(hsv[2] == 0 ? 100 : (int) hsv[2]);
					saturationSlider.setCurrentHue(hsv[0]);
					valueSlider.setCurrentHue(hsv[0]);
					valueSlider.setCurrentSaturation(hsv[1] == 0 ? 100 : hsv[1]);
					applyColorToStrand("#" + hex);
				} catch (Exception ignored) {}
				isUpdatingFromCode = false;
			}
		});

		addRenderableWidget(hueSlider);
		addRenderableWidget(saturationSlider);
		addRenderableWidget(valueSlider);
		addRenderableWidget(hexColorField);

		setSlidersVisible(false);
	}

	private void toggleColorPicker() {
		colorPickerVisible = !colorPickerVisible;
		if (colorPickerVisible) {
			String currentColor = getCurrentStrandColor();
			float[] hsv = ColorUtils.hexToHsv(currentColor);
			hueSlider.setValue((int) hsv[0]);
			saturationSlider.setValue(hsv[1] == 0 ? 100 : (int) hsv[1]);
			valueSlider.setValue(hsv[2] == 0 ? 100 : (int) hsv[2]);
			saturationSlider.setCurrentHue(hsv[0]);
			valueSlider.setCurrentHue(hsv[0]);
			valueSlider.setCurrentSaturation(hsv[1] == 0 ? 100 : hsv[1]);
			isUpdatingFromCode = true;
			hexColorField.setValue(currentColor);
			isUpdatingFromCode = false;
		}
		setSlidersVisible(colorPickerVisible);
	}

	private void updateColorFromSliders() {
		if (!colorPickerVisible) return;
		float h = hueSlider.getValue();
		float s = saturationSlider.getValue();
		float v = valueSlider.getValue();
		saturationSlider.setCurrentHue(h);
		valueSlider.setCurrentHue(h);
		valueSlider.setCurrentSaturation(s);
		String newColor = ColorUtils.hsvToHex(h, s, v);

		isUpdatingFromCode = true;
		if (!hexColorField.isFocused()) hexColorField.setValue(newColor);
		isUpdatingFromCode = false;

		applyColorToStrand(newColor);
	}

	private void applyColorToStrand(String color) {
		HairStrand strand = getSelectedStrand();
		if (strand != null) {
			strand.setColor(color);
			colorButton.setBackgroundColor(ColorUtils.hexToInt(color));
			applyMirrorColor(color);
			syncHairToServer();
		}
	}

	private void setSlidersVisible(boolean visible) {
		if (hueSlider != null) hueSlider.visible = visible;
		if (saturationSlider != null) saturationSlider.visible = visible;
		if (valueSlider != null) valueSlider.visible = visible;
		if (hexColorField != null) hexColorField.visible = visible;
	}

	private HairStrand getSelectedStrand() {
		return workingHairs[selectedStyle].getStrand(currentFace, selectedStrandIndex);
	}

	private String getCurrentStrandColor() {
		HairStrand strand = getSelectedStrand();
		if (strand != null && strand.hasCustomColor()) return strand.getColor();
		return workingHairs[selectedStyle].getGlobalColor();
	}

	private void fillEmptyStyles(CustomHair[] set) {
		if (set[0] == null || set[0].isEmpty()) set[0] = new CustomHair();
		if (set[1] == null || set[1].isEmpty()) set[1] = set[0].copy();
		if (set[2] == null || set[2].isEmpty()) set[2] = set[1].copy();
		if (set[3] == null || set[3].isEmpty()) set[3] = set[2].copy();
	}

	private void updateFullCodeBox() {
		if (fullCodeBox != null) {
			CustomHair[] temp = new CustomHair[4];
			for (int i = 0; i < 4; i++) temp[i] = workingHairs[i].copy();
			fillEmptyStyles(temp);
			fullCodeBox.setValue(HairManager.toFullSetCode(temp[0], temp[1], temp[2], temp[3]));
		}
	}

	private void updateIndividualCodeBox() {
		if (individualCodeBox != null) {
			individualCodeBox.setValue(HairManager.toCode(workingHairs[selectedStyle]));
		}
	}

	private void syncHairToServer() {
		character.setHairId(0);
		for (int i = 0; i < 4; i++) {
			switch (i) {
				case 0 -> character.setHairBase(workingHairs[i]);
				case 1 -> character.setHairSSJ(workingHairs[i]);
				case 2 -> character.setHairSSJ2(workingHairs[i]);
				case 3 -> character.setHairSSJ3(workingHairs[i]);
			}
			NetworkHandler.sendToServer(new UpdateCustomHairC2S(i, workingHairs[i]));
		}
	}

	private void navigateBack() {
		if (currentTab == Tab.STRAND) {
			currentTab = Tab.STYLE;
			colorPickerVisible = false;
			HairRenderer.EDITING_STRAND_ID = -1;
			rebuildWidgets();
		} else if (currentTab == Tab.STYLE) {
			currentTab = Tab.OVERVIEW;
			rebuildWidgets();
		} else {
			for (int i = 0; i < 4; i++) {
				workingHairs[i] = backupHairs[i].copy();
			}
			syncHairToServer();
			Minecraft.getInstance().setScreen(previousScreen);
		}
	}

	private void saveChanges() {
		if (currentTab != Tab.OVERVIEW) {
			syncHairToServer();
			updateFullCodeBox();
		} else {
			syncHairToServer();
			Minecraft.getInstance().setScreen(previousScreen);
		}
	}

	private void openHairSalon() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(new ConfirmLinkScreen(
					confirmed -> {
						if (confirmed) Util.getPlatform().openUri("https://dragonminez.com/hairsalon");
						this.minecraft.setScreen(this);
					},
					"https://dragonminez.com/hairsalon", true));
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		panorama.render(partialTick, 1.0F);
		renderCinematicBars(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);

		currentZoom = Mth.lerp(0.3f, currentZoom, targetZoom);

		int previewZoneLeft = 12 + 141 + 16;
		int previewZoneRight = getUiWidth() - 16;
		int baseX = previewZoneLeft + (previewZoneRight - previewZoneLeft) / 2;
		int baseY = (int) (getUiHeight() / 2 + 112 + (currentZoom - 95.0f) * 2.436f);

		renderPlayerModel(graphics, baseX, baseY, (int) currentZoom);

		renderPanelBackground(graphics);

		if (currentTab == Tab.OVERVIEW) renderOverviewContent(graphics);
		else if (currentTab == Tab.STYLE) renderStyleContent(graphics, uiMouseX, uiMouseY);
		else renderStrandContent(graphics);

		if (colorPickerVisible) renderColorPickerBackground(graphics);

		drawTopRightLabels(graphics);

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 400.0D);
		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		graphics.pose().popPose();

		endUiScale(graphics);
	}

	private void renderCinematicBars(GuiGraphics graphics) {
		int barH = (int) (height * 0.12);
		graphics.fill(0, 0, width, barH - 60, 0xFF000000);
		graphics.fillGradient(0, barH - 60, width, barH, 0xFF000000, 0x00000000);
		graphics.fillGradient(0, height - barH, width, height - barH + 60, 0x00000000, 0xFF000000);
		graphics.fill(0, height - barH + 60, width, height, 0xFF000000);
	}

	private void renderPanelBackground(GuiGraphics graphics) {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int panelY = centerY - 105;

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(MENU_BIG, leftPanelX, panelY, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, leftPanelX + 17, panelY + 10, 142, 22, 107, 21, 256, 256);
	}

	private void drawTopRightLabels(GuiGraphics graphics) {
		int rightEdge = getUiWidth() - 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.mirror"), rightEdge - 30 - font.width(tr("gui.dragonminez.hair_editor.mirror")) - 5, 17, 0xFFFFFF);
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.physics"), rightEdge - 90 - font.width(tr("gui.dragonminez.hair_editor.physics")) - 5, 17, 0xFFFFFF);
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.hairbase"), rightEdge - 170 - font.width(tr("gui.dragonminez.hair_editor.hairbase")) - 5, 17, 0xFFFFFF);
	}

	private void renderOverviewContent(GuiGraphics graphics) {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int panelY = centerY - 105;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.styles").withStyle(ChatFormatting.BOLD), leftPanelX + 70, panelY + 17, 0xFFFFD700);
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.fullcode"), leftPanelX + 15, panelY + 138, 0xFFFFFF);

		if (actionStatusTimer > 0) TextUtil.drawCenteredStringWithBorder(graphics, this.font, actionStatusText, leftPanelX + 70, panelY + 195, actionStatusColor);
    }

	private void renderStyleContent(GuiGraphics graphics, int mouseX, int mouseY) {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int panelY = centerY - 105;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.hair_strands").withStyle(ChatFormatting.BOLD), leftPanelX + 70, panelY + 17, 0xFFFFD700);
		renderFaceSelector(graphics, leftPanelX, panelY, mouseX, mouseY);
		renderStrandsGrid(graphics, leftPanelX, panelY, mouseX, mouseY);

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.stylecode"), leftPanelX + 15, panelY + 162, 0xFFFFFF);

		if (actionStatusTimer > 0) TextUtil.drawCenteredStringWithBorder(graphics, this.font, actionStatusText, leftPanelX + 70, panelY + 205, actionStatusColor);
	}

	private void renderStrandContent(GuiGraphics graphics) {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int panelY = centerY - 105;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.edit_values").withStyle(ChatFormatting.BOLD), leftPanelX + 70, panelY + 17, 0xFFFFD700);

		if (lengthSlider != null) {
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.length"), lengthSlider.getX(), lengthSlider.getY() - 10, 0xFFFFFF);
			TextUtil.drawStringWithBorder(graphics, this.font, txt(String.format("%.1f", lengthSlider.getValue())), lengthSlider.getX() + lengthSlider.getWidth() + 2, lengthSlider.getY() + 2, 0xFFFFFF);
		}
		if (widthSlider != null) {
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.width"), widthSlider.getX(), widthSlider.getY() - 10, 0xFFFFFF);
			TextUtil.drawStringWithBorder(graphics, this.font, txt(String.format("%.1f", widthSlider.getValue())), widthSlider.getX() + widthSlider.getWidth() + 2, widthSlider.getY() + 2, 0xFFFFFF);
		}
		if (xAxisSlider != null) {
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.x_axis"), xAxisSlider.getX(), xAxisSlider.getY() - 10, 0xFFFFFF);
			TextUtil.drawStringWithBorder(graphics, this.font, txt(String.format("%.1f", xAxisSlider.getValue())), xAxisSlider.getX() + xAxisSlider.getWidth() + 2, xAxisSlider.getY() + 2, 0xFFFFFF);
		}
		if (zAxisSlider != null) {
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.z_axis"), zAxisSlider.getX(), zAxisSlider.getY() - 10, 0xFFFFFF);
			TextUtil.drawStringWithBorder(graphics, this.font, txt(String.format("%.1f", zAxisSlider.getValue())), zAxisSlider.getX() + zAxisSlider.getWidth() + 2, zAxisSlider.getY() + 2, 0xFFFFFF);
		}
		if (xBendSlider != null) {
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.x_bend"), xBendSlider.getX(), xBendSlider.getY() - 10, 0xFFFFFF);
			TextUtil.drawStringWithBorder(graphics, this.font, txt(String.format("%.1f", xBendSlider.getValue())), xBendSlider.getX() + xBendSlider.getWidth() + 2, xBendSlider.getY() + 2, 0xFFFFFF);
		}
		if (zBendSlider != null) {
			TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.hair_editor.z_bend"), zBendSlider.getX(), zBendSlider.getY() - 10, 0xFFFFFF);
			TextUtil.drawStringWithBorder(graphics, this.font, txt(String.format("%.1f", zBendSlider.getValue())), zBendSlider.getX() + zBendSlider.getWidth() + 2, zBendSlider.getY() + 2, 0xFFFFFF);
		}
	}

	private void renderFaceSelector(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
		int btnY = panelY + 35;
		int btnX = panelX + 28;
		String[] faceNames = {"F", "B", "L", "R", "T"};
		HairFace[] faces = HairFace.values();

		for (int i = 0; i < faces.length; i++) {
			boolean isSelected = currentFace == faces[i];
			int width = font.width(faceNames[i]) + 6;
			boolean hovered = mouseX >= btnX && mouseX < btnX + width && mouseY >= btnY && mouseY < btnY + 14;
			int bgColor = isSelected ? 0xFF00AA00 : (hovered ? 0xFF555555 : 0xFF333333);

			graphics.fill(btnX, btnY, btnX + width, btnY + 14, bgColor);
			graphics.fill(btnX + 1, btnY + 1, btnX + width - 1, btnY + 13, isSelected ? 0xFF005500 : 0xFF222222);
			graphics.drawString(font, faceNames[i], btnX + 3, btnY + 3, isSelected ? 0xFFFFFF : 0xAAAAAA, false);
			btnX += width + 6;
		}
	}

	private void renderStrandsGrid(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
		int startY = panelY + 55;
		HairStrand[] strands = workingHairs[selectedStyle].getStrands(currentFace);
		if (strands == null) return;

		int cols = currentFace.cols;
		int boxSize = 24, spacing = 3;
		int gridStartX = panelX + (141 - (cols * boxSize + (cols - 1) * spacing)) / 2;

		for (int i = 0; i < strands.length; i++) {
			int boxX = gridStartX + (i % cols) * (boxSize + spacing);
			int boxY = startY + (i / cols) * (boxSize + spacing);
			boolean isSelected = i == selectedStrandIndex;
			boolean isVisible = strands[i].isVisible();
			boolean hovered = mouseX >= boxX && mouseX < boxX + boxSize && mouseY >= boxY && mouseY < boxY + boxSize;

			int bgColor = isSelected ? 0xFF00AA00 : (hovered ? 0xFF555555 : (isVisible ? 0xFF666600 : 0xFF333333));
			graphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, bgColor);
			graphics.fill(boxX + 1, boxY + 1, boxX + boxSize - 1, boxY + boxSize - 1, isSelected ? 0xFF005500 : 0xFF222222);

			String numText = String.valueOf(i);
			graphics.drawString(font, numText, boxX + (boxSize - font.width(numText)) / 2, boxY + (boxSize - font.lineHeight) / 2, isSelected ? 0x00FF00 : (isVisible ? 0xFFFF00 : 0x888888), false);
		}
	}

	private void renderColorPickerBackground(GuiGraphics graphics) {
		int sliderX = 12 + 150;
		int sliderY = getUiHeight() / 2 - 40;
		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 200.0D);
		graphics.fill(sliderX - 5, sliderY - 5, sliderX + 110, sliderY + 56, 0x88000000);
		graphics.pose().popPose();
	}

	private void renderPlayerModel(GuiGraphics graphics, int x, int y, int scale) {
		LivingEntity player = minecraft.player;
		if (player == null) return;

		boolean oldPhysics = HairRenderer.PHYSICS_ENABLED;
		HairRenderer.PHYSICS_ENABLED = physicsEnabled;
		int originalHairId = character.getHairId();
		CustomHair originalBaseHair = character.getHairBase();

		character.setHairId(0);
		if (currentTab == Tab.OVERVIEW) {
			character.setHairBase(workingHairs[0]);
		} else {
			character.setHairBase(workingHairs[selectedStyle]);
		}

		Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).mul(new Quaternionf().rotateX(0));

		float yBodyRotO = player.yBodyRot;
		float yBodyRotO_field = player.yBodyRotO;
		float yRotO = player.getYRot();
		float xRotO = player.getXRot();
		float xRotO_field = player.xRotO;
		float yHeadRotO = player.yHeadRotO;
		float yHeadRot = player.yHeadRot;

		player.yBodyRot = playerRotation;
		player.yBodyRotO = playerRotation;
		player.setYRot(playerRotation);
		player.setXRot(playerPitch);
		player.xRotO = playerPitch;
		player.yHeadRot = playerRotation;
		player.yHeadRotO = playerRotation;

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 150.0D);
		InventoryScreen.renderEntityInInventory(graphics, x, y, scale, pose, new Quaternionf().rotateX(0), player);
		graphics.pose().popPose();

		player.yBodyRot = yBodyRotO;
		player.yBodyRotO = yBodyRotO_field;
		player.setYRot(yRotO);
		player.setXRot(xRotO);
		player.xRotO = xRotO_field;
		player.yHeadRotO = yHeadRotO;
		player.yHeadRot = yHeadRot;

		HairRenderer.PHYSICS_ENABLED = oldPhysics;
		character.setHairBase(originalBaseHair);
		character.setHairId(originalHairId);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;

		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		if (colorPickerVisible) {
			int sliderX = 12 + 150;
			int sliderY = getUiHeight() / 2 - 40;
			if (uiMouseX < sliderX - 5 || uiMouseX > sliderX + 110 || uiMouseY < sliderY - 5 || uiMouseY > sliderY + 56) {
				setSlidersVisible(false);
				colorPickerVisible = false;
				return true;
			}
			return false;
		}

		if (currentTab == Tab.STYLE) {
			if (handleFaceSelectorClick(uiMouseX, uiMouseY)) return true;
			if (handleStrandGridClick(uiMouseX, uiMouseY)) return true;
		}

		int previewZoneLeft = 12 + 141 + 16;
		int previewZoneRight = getUiWidth() - 16;

		if (uiMouseX >= previewZoneLeft && uiMouseX <= previewZoneRight && uiMouseY >= 45) {
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
			double deltaX = toUiX(mouseX) - lastMouseX;
			double deltaY = toUiY(mouseY) - lastMouseY;
			playerRotation -= (float) deltaX;
			playerPitch = Math.max(-90.0f, Math.min(90.0f, playerPitch + (float) deltaY));
			lastMouseX = toUiX(mouseX);
			lastMouseY = toUiY(mouseY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiMouseX = toUiX(mouseX);
		int previewZoneLeft = 12 + 141 + 16;

		if (uiMouseX >= previewZoneLeft && !colorPickerVisible) {
			targetZoom += (float) delta * 20.0f;
			targetZoom = Mth.clamp(targetZoom, 95.0f, 250.0f);
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	private boolean handleFaceSelectorClick(double mouseX, double mouseY) {
		int btnY = (getUiHeight() / 2 - 105) + 35;
		int btnX = 12 + 28;
		HairFace[] faces = HairFace.values();
		String[] names = {"F", "B", "L", "R", "T"};

		for (int i = 0; i < faces.length; i++) {
			int width = font.width(names[i]) + 6;
			if (mouseX >= btnX && mouseX < btnX + width && mouseY >= btnY && mouseY < btnY + 14) {
				currentFace = faces[i];
				selectedStrandIndex = 0;
				rebuildWidgets();
				return true;
			}
			btnX += width + 6;
		}
		return false;
	}

	private boolean handleStrandGridClick(double mouseX, double mouseY) {
		HairStrand[] strands = workingHairs[selectedStyle].getStrands(currentFace);
		if (strands == null) return false;

		int cols = currentFace.cols;
		int boxSize = 24, spacing = 3;
		int gridStartX = 12 + (141 - (cols * boxSize + (cols - 1) * spacing)) / 2;
		int startY = (getUiHeight() / 2 - 105) + 55;

		for (int i = 0; i < strands.length; i++) {
			int boxX = gridStartX + (i % cols) * (boxSize + spacing);
			int boxY = startY + (i / cols) * (boxSize + spacing);
			if (mouseX >= boxX && mouseX < boxX + boxSize && mouseY >= boxY && mouseY < boxY + boxSize) {
				selectedStrandIndex = i;
				currentTab = Tab.STRAND;
				rebuildWidgets();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) {
			navigateBack();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		navigateBack();
	}

	public MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	public MutableComponent txt(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}
}