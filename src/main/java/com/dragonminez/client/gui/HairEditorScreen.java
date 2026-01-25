package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.AxisSlider;
import com.dragonminez.client.gui.buttons.ColorSlider;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.CharacterCustomizationScreen;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.CustomHair.HairFace;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.hair.HairStrand;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.UpdateCustomHairC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.Character;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class HairEditorScreen extends Screen {
    private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
            "textures/gui/menu/menubig.png");
    private static final ResourceLocation STAT_BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
            "textures/gui/buttons/characterbuttons.png");

    private static final ResourceLocation PANORAMA_HUMAN = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/panorama");
    private static final ResourceLocation PANORAMA_SAIYAN = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/s_panorama");
    private static final ResourceLocation PANORAMA_NAMEK = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/n_panorama");
    private static final ResourceLocation PANORAMA_BIO = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/bio_panorama");
    private static final ResourceLocation PANORAMA_FROST = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/c_panorama");
    private static final ResourceLocation PANORAMA_MAJIN = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background/buu_panorama");

    private final PanoramaRenderer panoramaHuman = new PanoramaRenderer(new CubeMap(PANORAMA_HUMAN));
    private final PanoramaRenderer panoramaSaiyan = new PanoramaRenderer(new CubeMap(PANORAMA_SAIYAN));
    private final PanoramaRenderer panoramaNamek = new PanoramaRenderer(new CubeMap(PANORAMA_NAMEK));
    private final PanoramaRenderer panoramaBio = new PanoramaRenderer(new CubeMap(PANORAMA_BIO));
    private final PanoramaRenderer panoramaFrost = new PanoramaRenderer(new CubeMap(PANORAMA_FROST));
    private final PanoramaRenderer panoramaMajin = new PanoramaRenderer(new CubeMap(PANORAMA_MAJIN));

    protected static boolean GLOBAL_SWITCHING = false;

    private final Screen previousScreen;
    private final Character character;
    private final CustomHair editingHair;
    private final CustomHair backupHair;
    private final int oldGuiScale;
    private final boolean usePanorama;
    private boolean isSwitchingMenu = false;
    private final int originalHairId;

    private HairFace currentFace = HairFace.FRONT;
    private int selectedStrandIndex = 0;

    private EditMode editMode = EditMode.LENGTH;

    private float playerRotation = 180.0f;
    private boolean isDraggingModel = false;
    private double lastMouseX = 0;

    private EditBox codeField;

    private final List<CustomTextureButton> controlButtons = new ArrayList<>();

    private ColorSlider hueSlider;
    private ColorSlider saturationSlider;
    private ColorSlider valueSlider;
    private boolean colorPickerVisible = false;
    private TexturedTextButton colorButton;


    public enum EditMode {
        LENGTH("gui.dragonminez.hair_editor.mode.length"),
        ROTATION("gui.dragonminez.hair_editor.mode.rotation"),
        CURVE("gui.dragonminez.hair_editor.mode.curve"),
        SCALE("gui.dragonminez.hair_editor.mode.scale");

        private final String translationKey;

        EditMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    public HairEditorScreen(Screen previousScreen, Character character) {
        super(Component.literal("Hair Editor"));
        this.previousScreen = previousScreen;
        this.character = character;

        this.usePanorama = previousScreen instanceof CharacterCustomizationScreen;

        Minecraft mc = Minecraft.getInstance();

        if (previousScreen instanceof CharacterCustomizationScreen) {
            this.oldGuiScale = ((CharacterCustomizationScreen) previousScreen).getOldGuiScale();
        } else {
            this.oldGuiScale = mc.options.guiScale().get();
            if (oldGuiScale != 3) {
                mc.options.guiScale().set(3);
                mc.resizeDisplay();
            }
        }

        this.originalHairId = character.getHairId();

        if (character.getHairId() > 0) {
            CustomHair presetHair = HairManager.getPresetHair(character.getHairId(), character.getHairColor());
            if (presetHair != null) {
                character.setCustomHair(presetHair.copy());
                character.setHairId(0);
                NetworkHandler.sendToServer(new UpdateCustomHairC2S(character.getCustomHair()));
            }
        }

        if (character.getCustomHair() == null) {
            character.setCustomHair(new CustomHair());
        }

        this.editingHair = character.getCustomHair();
        this.backupHair = editingHair.copy();
    }

    @Override
    protected void init() {
        super.init();

        initLeftPanelButtons();
        initControlButtons();
        initColorPicker();
        initBottomButtons();
    }

    private void initLeftPanelButtons() {
        int leftPanelX = 12;
        int centerY = this.height / 2;
        int leftPanelY = centerY - 105;

        int buttonY = leftPanelY + 165;

        int modeX = leftPanelX + 10;
        for (EditMode mode : EditMode.values()) {
            final EditMode m = mode;
            addRenderableWidget(Button.builder(
                Component.translatable(mode.getTranslationKey()),
                btn -> {
                    editMode = m;
                    rebuildWidgets();
                }
            ).bounds(modeX, buttonY, 28, 16).build());
            modeX += 30;
        }
    }

    private void initControlButtons() {
        clearControlButtons();

        int leftPanelX = 12;
        int centerY = this.height / 2;
        int leftPanelY = centerY - 105;
        int startY = leftPanelY + 40 + 35 + 15;

        HairStrand strand = getSelectedStrand();
        if (strand == null) return;

        switch (editMode) {
            case LENGTH -> {
                CustomTextureButton decreaseBtn = new CustomTextureButton.Builder()
                        .position(leftPanelX + 30, startY)
                        .size(14, 11)
                        .texture(STAT_BUTTONS)
                        .textureCoords(142, 0, 142, 10)
                        .textureSize(10, 10)
                        .onPress(button -> {
                            HairStrand s = getSelectedStrand();
                            if (s != null) {
                                s.removeCube();
                                syncHairToServer();
                            }
                        })
                        .sound(MainSounds.UI_MENU_SWITCH.get())
                        .build();
                controlButtons.add(decreaseBtn);
                this.addRenderableWidget(decreaseBtn);

                CustomTextureButton increaseBtn = new CustomTextureButton.Builder()
                        .position(leftPanelX + 95, startY)
                        .size(14, 11)
                        .texture(STAT_BUTTONS)
                        .textureCoords(0, 0, 0, 10)
                        .textureSize(10, 10)
                        .onPress(button -> {
                            HairStrand s = getSelectedStrand();
                            if (s != null) {
                                s.addCube();
                                syncHairToServer();
                            }
                        })
                        .sound(MainSounds.UI_MENU_SWITCH.get())
                        .build();
                controlButtons.add(increaseBtn);
                this.addRenderableWidget(increaseBtn);
            }
            case ROTATION, CURVE -> {
                int btnY = startY + 20;
                createAxisButtons(leftPanelX, btnY - 38);
            }
            case SCALE -> {
                int btnY = startY + 20;
                createScaleButtons(leftPanelX, btnY - 38);
            }
        }
    }

    private void createAxisButtons(int panelX, int btnY) {
        int sliderX = panelX + 30;
        int sliderWidth = 79;
        int sliderHeight = 11;

        HairStrand strand = getSelectedStrand();
        if (strand == null) return;

        float minValue, maxValue;
        if (editMode == EditMode.ROTATION) {
            minValue = -180f;
            maxValue = 180f;
        } else {
            minValue = -50f;
            maxValue = 50f;
        }

        // Slider X
        AxisSlider sliderX_axis = new AxisSlider.Builder()
                .position(sliderX, btnY)
                .size(sliderWidth, sliderHeight)
                .range(minValue, maxValue)
                .value(editMode == EditMode.ROTATION ? strand.getRotationX() : strand.getCurveX())
                .axis(AxisSlider.Axis.X)
                .onValueChange(value -> {
                    HairStrand s = getSelectedStrand();
                    if (s != null) {
                        if (editMode == EditMode.ROTATION) {
                            s.setRotation(value, s.getRotationY(), s.getRotationZ());
                        } else {
                            s.setCurve(value, s.getCurveY(), s.getCurveZ());
                        }
                        syncHairToServer();
                    }
                })
                .build();
        this.addRenderableWidget(sliderX_axis);

        // Slider Y
        AxisSlider sliderY = new AxisSlider.Builder()
                .position(sliderX, btnY + 26)
                .size(sliderWidth, sliderHeight)
                .range(minValue, maxValue)
                .value(editMode == EditMode.ROTATION ? strand.getRotationY() : strand.getCurveY())
                .axis(AxisSlider.Axis.Y)
                .onValueChange(value -> {
                    HairStrand s = getSelectedStrand();
                    if (s != null) {
                        if (editMode == EditMode.ROTATION) {
                            s.setRotation(s.getRotationX(), value, s.getRotationZ());
                        } else {
                            s.setCurve(s.getCurveX(), value, s.getCurveZ());
                        }
                        syncHairToServer();
                    }
                })
                .build();
        this.addRenderableWidget(sliderY);

        // Slider Z
        AxisSlider sliderZ = new AxisSlider.Builder()
                .position(sliderX, btnY + 52)
                .size(sliderWidth, sliderHeight)
                .range(minValue, maxValue)
                .value(editMode == EditMode.ROTATION ? strand.getRotationZ() : strand.getCurveZ())
                .axis(AxisSlider.Axis.Z)
                .onValueChange(value -> {
                    HairStrand s = getSelectedStrand();
                    if (s != null) {
                        if (editMode == EditMode.ROTATION) {
                            s.setRotation(s.getRotationX(), s.getRotationY(), value);
                        } else {
                            s.setCurve(s.getCurveX(), s.getCurveY(), value);
                        }
                        syncHairToServer();
                    }
                })
                .build();
        this.addRenderableWidget(sliderZ);
    }

    private void createScaleButtons(int panelX, int btnY) {
        int sliderX = panelX + 30;
        int sliderWidth = 79;
        int sliderHeight = 11;

        HairStrand strand = getSelectedStrand();
        if (strand == null) return;

        float minValue = 0.5f;
        float maxValue = 3.0f;

        // Slider X
        AxisSlider sliderX_axis = new AxisSlider.Builder()
                .position(sliderX, btnY)
                .size(sliderWidth, sliderHeight)
                .range(minValue, maxValue)
                .value(strand.getScaleX())
                .axis(AxisSlider.Axis.X)
                .onValueChange(value -> {
                    HairStrand s = getSelectedStrand();
                    if (s != null) {
                        s.setScale(value, s.getScaleY(), s.getScaleZ());
                        syncHairToServer();
                    }
                })
                .build();
        this.addRenderableWidget(sliderX_axis);

        // Slider Y
        AxisSlider sliderY = new AxisSlider.Builder()
                .position(sliderX, btnY + 26)
                .size(sliderWidth, sliderHeight)
                .range(minValue, maxValue)
                .value(strand.getScaleY())
                .axis(AxisSlider.Axis.Y)
                .onValueChange(value -> {
                    HairStrand s = getSelectedStrand();
                    if (s != null) {
                        s.setScale(s.getScaleX(), value, s.getScaleZ());
                        syncHairToServer();
                    }
                })
                .build();
        this.addRenderableWidget(sliderY);

        // Slider Z
        AxisSlider sliderZ = new AxisSlider.Builder()
                .position(sliderX, btnY + 52)
                .size(sliderWidth, sliderHeight)
                .range(minValue, maxValue)
                .value(strand.getScaleZ())
                .axis(AxisSlider.Axis.Z)
                .onValueChange(value -> {
                    HairStrand s = getSelectedStrand();
                    if (s != null) {
                        s.setScale(s.getScaleX(), s.getScaleY(), value);
                        syncHairToServer();
                    }
                })
                .build();
        this.addRenderableWidget(sliderZ);
    }

    private void clearControlButtons() {
        for (CustomTextureButton btn : controlButtons) {
            this.removeWidget(btn);
        }
        controlButtons.clear();
    }

    private void initColorPicker() {
        int leftPanelX = 12;
        int centerY = this.height / 2;
        int leftPanelY = centerY - 105;

        int colorBtnX = leftPanelX + 105;
        int colorBtnY = leftPanelY + 140;

        String currentColor = getCurrentStrandColor();
        int colorInt = ColorUtils.hexToInt(currentColor);

        colorButton = new TexturedTextButton.Builder()
                .position(colorBtnX, colorBtnY)
                .size(20, 20)
                .texture(STAT_BUTTONS)
                .textureCoords(42, 15, 42, 15)
                .textureSize(5, 5)
                .message(Component.empty())
                .backgroundColor(colorInt)
                .onPress(btn -> toggleColorPicker())
                .build();
        addRenderableWidget(colorButton);

        int sliderX = leftPanelX + 10;
        int sliderY = leftPanelY + 105;
        int sliderWidth = 115;

        hueSlider = new ColorSlider.Builder()
                .position(sliderX, sliderY)
                .size(sliderWidth, 10)
                .range(0, 360)
                .value(0)
                .message(Component.literal("H"))
                .onValueChange(val -> updateColorFromSliders())
                .build();

        saturationSlider = new ColorSlider.Builder()
                .position(sliderX, sliderY + 12)
                .size(sliderWidth, 10)
                .range(100, 0)
                .value(100)
                .message(Component.literal("S"))
                .onValueChange(val -> updateColorFromSliders())
                .build();

        valueSlider = new ColorSlider.Builder()
                .position(sliderX, sliderY + 24)
                .size(sliderWidth, 10)
                .range(100, 0)
                .value(100)
                .message(Component.literal("V"))
                .onValueChange(val -> updateColorFromSliders())
                .build();

        addRenderableWidget(hueSlider);
        addRenderableWidget(saturationSlider);
        addRenderableWidget(valueSlider);

        setSlidersVisible(false);
    }

    private String getCurrentStrandColor() {
        HairStrand strand = getSelectedStrand();
        if (strand != null && strand.hasCustomColor()) {
            return strand.getColor();
        }
        if (!character.hasActiveForm()) {
            String hairColor = character.getHairColor();
            if (hairColor != null && !hairColor.isEmpty()) {
                return hairColor;
            }
        }
        return editingHair.getGlobalColor();
    }

    private void toggleColorPicker() {
        colorPickerVisible = !colorPickerVisible;

        if (colorPickerVisible) {
            String currentColor = getCurrentStrandColor();
            float[] hsv = ColorUtils.hexToHsv(currentColor);

            if (hueSlider != null) hueSlider.setValue((int) hsv[0]);
            if (saturationSlider != null) {
                int satValue = (int) hsv[1];
                if (satValue == 0) satValue = 100;
                saturationSlider.setValue(satValue);
            }
            if (valueSlider != null) {
                int valValue = (int) hsv[2];
                if (valValue == 0) valValue = 100;
                valueSlider.setValue(valValue);
            }

            if (saturationSlider != null) saturationSlider.setCurrentHue(hsv[0]);
            if (valueSlider != null) {
                valueSlider.setCurrentHue(hsv[0]);
                valueSlider.setCurrentSaturation(hsv[1] == 0 ? 100 : hsv[1]);
            }
        }

        setSlidersVisible(colorPickerVisible);
    }

    private void setSlidersVisible(boolean visible) {
        if (hueSlider != null) hueSlider.visible = visible;
        if (saturationSlider != null) saturationSlider.visible = visible;
        if (valueSlider != null) valueSlider.visible = visible;
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
        applyColorToStrand(newColor);
    }

    private void applyColorToStrand(String color) {
        HairStrand strand = getSelectedStrand();
        if (strand != null) {
            strand.setColor(color);
            updateColorButton();
        }
    }

    private void updateColorButton() {
        if (colorButton != null) {
            this.removeWidget(colorButton);
        }

        int leftPanelX = 12;
        int centerY = this.height / 2;
        int leftPanelY = centerY - 105;
        int colorBtnX = leftPanelX + 105;
        int colorBtnY = leftPanelY + 140;

        String currentColor = getCurrentStrandColor();
        int colorInt = ColorUtils.hexToInt(currentColor);

        colorButton = new TexturedTextButton.Builder()
                .position(colorBtnX, colorBtnY)
                .size(20, 20)
                .texture(STAT_BUTTONS)
                .textureCoords(42, 15, 42, 15)
                .textureSize(5, 5)
                .message(Component.empty())
                .backgroundColor(colorInt)
                .onPress(btn -> toggleColorPicker())
                .build();
        addRenderableWidget(colorButton);
    }

    private void initBottomButtons() {
        int bottomY = this.height - 30;
        int centerX = this.width / 2;

        codeField = new EditBox(this.font, centerX - 70, bottomY - 25, 140, 18,
                Component.translatable("gui.dragonminez.hair_editor.code"));
        codeField.setMaxLength(65536);
        addRenderableWidget(codeField);

        addRenderableWidget(new TexturedTextButton.Builder()
                .position(centerX - 70 - 84, bottomY - 25)
                .size(74, 20)
                .texture(STAT_BUTTONS)
                .textureCoords(0, 28, 0, 48)
                .textureSize(74, 20)
                .message(Component.translatable("gui.dragonminez.hair_editor.export"))
                .onPress(btn -> exportCode())
                .build());

        addRenderableWidget(new TexturedTextButton.Builder()
                .position(centerX + 70 + 14, bottomY - 25)
				.size(74, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
                .message(Component.translatable("gui.dragonminez.hair_editor.import"))
                .onPress(btn -> importCode())
                .build());

        addRenderableWidget(new TexturedTextButton.Builder()
                .position(centerX - 70 - 45, bottomY)
				.size(74, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
                .message(Component.translatable("gui.dragonminez.hair_editor.reset"))
                .onPress(btn -> resetHair())
                .build());

        addRenderableWidget(new TexturedTextButton.Builder()
                .position(centerX - 35, bottomY)
				.size(74, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
                .message(Component.translatable("gui.dragonminez.hair_editor.save"))
                .onPress(btn -> saveAndClose())
                .build());

        addRenderableWidget(new TexturedTextButton.Builder()
                .position(centerX + 35 + 10, bottomY)
				.size(74, 20)
				.texture(STAT_BUTTONS)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
                .message(Component.translatable("gui.dragonminez.hair_editor.cancel"))
                .onPress(btn -> cancelAndClose())
                .build());
    }

    private void resetHair() {
        editingHair.clear();
        selectedStrandIndex = 0;
        colorPickerVisible = false;
        setSlidersVisible(false);
        updateColorButton();
        initControlButtons();
        syncHairToServer();
    }

    private void syncHairToServer() {
        NetworkHandler.sendToServer(new UpdateCustomHairC2S(editingHair));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (usePanorama) {
            renderPanorama(partialTick);
        } else {
            this.renderBackground(graphics);
			this.renderBackground(graphics);
        }
        renderLeftPanel(graphics);
        renderRightPanel(graphics, mouseX, mouseY);
        renderPlayerModel(graphics, this.width / 2, this.height / 2 + 220, 150);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0D, 0.0D, 400.0D);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
    }

    private void renderPanorama(float partialTick) {
        String currentRace = character.getRace();

        PanoramaRenderer panorama = switch (currentRace) {
            case "saiyan" -> panoramaSaiyan;
            case "namekian" -> panoramaNamek;
            case "bioandroid" -> panoramaBio;
            case "frostdemon" -> panoramaFrost;
            case "majin" -> panoramaMajin;
            default -> panoramaHuman;
        };

        panorama.render(partialTick, 1.0F);
    }

    private void renderLeftPanel(GuiGraphics graphics) {
        int leftPanelX = 12;
        int centerY = this.height / 2;
        int leftPanelY = centerY - 105;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(MENU_BIG, leftPanelX, leftPanelY, 0, 0, 141, 213, 256, 256);
        graphics.blit(MENU_BIG, leftPanelX + 17, leftPanelY + 10, 142, 22, 107, 21, 256, 256);

        drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.hair_editor.edit_values").withStyle(ChatFormatting.BOLD),
                leftPanelX + 70, leftPanelY + 17, 0xFFFFD700);

        renderEditInfo(graphics, leftPanelX, leftPanelY);

        graphics.pose().pushPose();
        graphics.pose().scale(0.75f, 0.75f, 0.75f);
        drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.hair_editor.color"),
                (int)((leftPanelX + 80) / 0.75f), (int)((leftPanelY + 149) / 0.75f), 0xFFFFFF);
        graphics.pose().popPose();
    }

    private void renderEditInfo(GuiGraphics graphics, int panelX, int panelY) {
        int startY = panelY + 40;
        HairStrand strand = getSelectedStrand();

        if (strand == null) {
            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.hair_editor.no_strand"),
                    panelX + 70, startY + 20, 0xFF5555);
            return;
        }

        drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.hair_editor.mode",
                Component.translatable(editMode.getTranslationKey())),
                panelX + 15, startY, 0x00FF00);
        startY += 15;

        switch (editMode) {
            case LENGTH -> {
                int cubeCount = strand.getCubeCount();
                float stretchFactor = strand.getStretchFactor();
                Component lengthText;
                if (stretchFactor > 1.0f) {
                    lengthText = Component.translatable("gui.dragonminez.hair_editor.length_stretch",
                            strand.getLength(), cubeCount, String.format("%.2f", stretchFactor));
                } else {
                    lengthText = Component.translatable("gui.dragonminez.hair_editor.length",
                            strand.getLength(), cubeCount);
                }
                drawStringWithBorder(graphics, lengthText, panelX + 15, startY, 0xFFFFFF);
            }
            case ROTATION, CURVE, SCALE -> {}
        }
    }

    private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int rightPanelX = this.width - 158;
        int centerY = this.height / 2;
        int rightPanelY = centerY - 105;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(MENU_BIG, rightPanelX, rightPanelY, 0, 0, 141, 213, 256, 256);
        graphics.blit(MENU_BIG, rightPanelX + 17, rightPanelY + 10, 142, 22, 107, 21, 256, 256);

        drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.hair_editor.hair_strands").withStyle(ChatFormatting.BOLD),
                rightPanelX + 70, rightPanelY + 17, 0xFFFFD700);
        renderFaceSelector(graphics, rightPanelX, rightPanelY, mouseX, mouseY);
        renderStrandsGrid(graphics, rightPanelX, rightPanelY, mouseX, mouseY);
    }

    private void renderFaceSelector(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
        int btnY = panelY + 35;
        int btnX = panelX + 28;

        String[] faceShortNames = {"F", "B", "L", "R", "T"};
        HairFace[] faces = HairFace.values();

        for (int i = 0; i < faces.length; i++) {
            HairFace face = faces[i];
            boolean isSelected = currentFace == face;
            String shortName = faceShortNames[i];
            int width = font.width(shortName) + 6;
            int height = 14;

            boolean hovered = mouseX >= btnX && mouseX < btnX + width && mouseY >= btnY && mouseY < btnY + height;
            int bgColor = isSelected ? 0xFF00AA00 : (hovered ? 0xFF555555 : 0xFF333333);

            graphics.fill(btnX, btnY, btnX + width, btnY + height, bgColor);
            graphics.fill(btnX + 1, btnY + 1, btnX + width - 1, btnY + height - 1, isSelected ? 0xFF005500 : 0xFF222222);

            int textX = btnX + 3;
            int textY = btnY + 3;
            graphics.drawString(font, shortName, textX, textY, isSelected ? 0xFFFFFF : (hovered ? 0xFFFFFF : 0xAAAAAA), false);

            btnX += width + 6;
        }
    }

    private void renderStrandsGrid(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
        int startY = panelY + 55;
        HairStrand[] strands = editingHair.getStrands(currentFace);
        if (strands == null) return;

        int cols = currentFace.cols;
        int rows = currentFace.rows;
        int boxSize = 24;
        int spacing = 3;

        int gridWidth = cols * boxSize + (cols - 1) * spacing;
        int gridStartX = panelX + (141 - gridWidth) / 2;

        for (int i = 0; i < strands.length; i++) {
            HairStrand strand = strands[i];
            int col = i % cols;
            int row = i / cols;

            int boxX = gridStartX + col * (boxSize + spacing);
            int boxY = startY + row * (boxSize + spacing);

            boolean isSelected = i == selectedStrandIndex;
            boolean isVisible = strand.isVisible();

            boolean hovered = mouseX >= boxX && mouseX < boxX + boxSize &&
                    mouseY >= boxY && mouseY < boxY + boxSize;

            int bgColor;
            if (isSelected) {
                bgColor = 0xFF00AA00;
            } else if (hovered) {
                bgColor = 0xFF555555;
            } else if (isVisible) {
                bgColor = 0xFF666600;
            } else {
                bgColor = 0xFF333333;
            }

            graphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, bgColor);
            graphics.fill(boxX + 1, boxY + 1, boxX + boxSize - 1, boxY + boxSize - 1, isSelected ? 0xFF005500 : 0xFF222222);

            String numText = String.valueOf(i);
            int textColor = isSelected ? 0x00FF00 : (isVisible ? 0xFFFF00 : 0x888888);

            int textX = boxX + (boxSize - font.width(numText)) / 2;
            int textY = boxY + (boxSize - font.lineHeight) / 2;
            graphics.drawString(font, numText, textX, textY, textColor, false);
        }

        int infoY = startY + rows * (boxSize + spacing) + 10;

        graphics.pose().pushPose();
        graphics.pose().scale(0.75f, 0.75f, 0.75f);
        drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.hair_editor.visible", editingHair.getVisibleStrandCount()),
                (int)((panelX + 25) / 0.75f), (int)(infoY / 0.75f), 0xFFFFFF);
        drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.hair_editor.cubes", editingHair.getTotalCubeCount()),
                (int)((panelX + 75) / 0.75f), (int)(infoY / 0.75f), 0xFFFFFF);
        graphics.pose().popPose();
    }

    private void renderPlayerModel(GuiGraphics graphics, int x, int y, int scale) {
        LivingEntity player = this.minecraft.player;
        if (player == null) return;

        Quaternionf pose = (new Quaternionf()).rotateZ((float)Math.PI);
        Quaternionf cameraOrientation = (new Quaternionf()).rotateX(0);
        pose.mul(cameraOrientation);

        float yBodyRotO = player.yBodyRot;
        float yRotO = player.getYRot();
        float xRotO = player.getXRot();
        float yHeadRotO = player.yHeadRotO;
        float yHeadRot = player.yHeadRot;

        player.yBodyRot = playerRotation;
        player.setYRot(playerRotation);
        player.setXRot(0);
        player.yHeadRot = playerRotation;
        player.yHeadRotO = playerRotation;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0D, 0.0D, 150.0D);
        InventoryScreen.renderEntityInInventory(graphics, x, y, scale, pose, cameraOrientation, player);
        graphics.pose().popPose();

        player.yBodyRot = yBodyRotO;
        player.setYRot(yRotO);
        player.setXRot(xRotO);
        player.yHeadRotO = yHeadRotO;
        player.yHeadRot = yHeadRot;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleStrandGridClick(mouseX, mouseY)) return true;
        if (handleFaceSelectorClick(mouseX, mouseY)) return true;

        int centerX = this.width / 2;
        int centerY = this.height / 2 + 20;
        int modelRadius = 100;
        int bottomY = this.height - 30;
        int maxDragY = bottomY - 25 - 10;

        if (mouseX >= centerX - modelRadius && mouseX <= centerX + modelRadius &&
            mouseY >= centerY - 400 && mouseY <= maxDragY) {
            isDraggingModel = true;
            lastMouseX = mouseX;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingModel = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingModel) {
            double deltaX = mouseX - lastMouseX;
            playerRotation += (float)(deltaX * 0.8);
            lastMouseX = mouseX;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean handleStrandGridClick(double mouseX, double mouseY) {
        int rightPanelX = this.width - 158;
        int centerY = this.height / 2;
        int rightPanelY = centerY - 105;
        int startY = rightPanelY + 55;

        HairStrand[] strands = editingHair.getStrands(currentFace);
        if (strands == null) return false;

        int cols = currentFace.cols;
        int boxSize = 24;
        int spacing = 3;
        int gridWidth = cols * boxSize + (cols - 1) * spacing;
        int gridStartX = rightPanelX + (141 - gridWidth) / 2;

        for (int i = 0; i < strands.length; i++) {
            int col = i % cols;
            int row = i / cols;

            int boxX = gridStartX + col * (boxSize + spacing);
            int boxY = startY + row * (boxSize + spacing);

            if (mouseX >= boxX && mouseX < boxX + boxSize &&
                mouseY >= boxY && mouseY < boxY + boxSize) {
                selectedStrandIndex = i;
                colorPickerVisible = false;
                setSlidersVisible(false);
                updateColorButton();
                initControlButtons();
                return true;
            }
        }

        return false;
    }

    private boolean handleFaceSelectorClick(double mouseX, double mouseY) {
        int rightPanelX = this.width - 158;
        int centerY = this.height / 2;
        int rightPanelY = centerY - 105;
        int btnY = rightPanelY + 35;
        int btnX = rightPanelX + 28;

        String[] faceShortNames = {"F", "B", "L", "R", "T"};
        HairFace[] faces = HairFace.values();

        for (int i = 0; i < faces.length; i++) {
            HairFace face = faces[i];
            String shortName = faceShortNames[i];
            int width = font.width(shortName) + 6;
            int height = 14;

            if (mouseX >= btnX && mouseX < btnX + width && mouseY >= btnY && mouseY < btnY + height) {
                currentFace = face;
                selectedStrandIndex = 0;
                colorPickerVisible = false;
                setSlidersVisible(false);
                updateColorButton();
                initControlButtons();
                return true;
            }

            btnX += width + 6;
        }

        return false;
    }

    private HairStrand getSelectedStrand() {
        return editingHair.getStrand(currentFace, selectedStrandIndex);
    }


    private void exportCode() {
        String code = HairManager.toCode(editingHair);
        if (code != null) {
            codeField.setValue(code);
            Minecraft.getInstance().keyboardHandler.setClipboard(code);
        }
    }

    private void importCode() {
        String code = codeField.getValue();
        if (code.isEmpty()) {
            code = Minecraft.getInstance().keyboardHandler.getClipboard();
        }

		if (HairManager.isValidCode(code)) {
			copyHairData(HairManager.fromCode(code), editingHair);
			selectedStrandIndex = 0;
			colorPickerVisible = false;
			setSlidersVisible(false);
			updateColorButton();
			initControlButtons();
		}
    }

    private void copyHairData(CustomHair source, CustomHair dest) {
        dest.setGlobalColor(source.getGlobalColor());
        dest.setName(source.getName());

        for (HairFace face : HairFace.values()) {
            HairStrand[] srcStrands = source.getStrands(face);
            HairStrand[] dstStrands = dest.getStrands(face);
            for (int i = 0; i < srcStrands.length && i < dstStrands.length; i++) {
                HairStrand src = srcStrands[i];
                HairStrand dst = dstStrands[i];
                dst.setLength(src.getLength());
                dst.setOffset(src.getOffsetX(), src.getOffsetY(), src.getOffsetZ());
                dst.setRotation(src.getRotationX(), src.getRotationY(), src.getRotationZ());
                dst.setCurve(src.getCurveX(), src.getCurveY(), src.getCurveZ());
                dst.setScale(src.getScaleX(), src.getScaleY(), src.getScaleZ());
                dst.setColor(src.getColor());
            }
        }
    }

    private void saveAndClose() {
        character.setHairId(0);

        NetworkHandler.sendToServer(new UpdateCustomHairC2S(editingHair));

        if (previousScreen != null) {
            isSwitchingMenu = true;
            GLOBAL_SWITCHING = true;
        }
        Minecraft.getInstance().setScreen(previousScreen);
    }

    private void cancelAndClose() {
        copyHairData(backupHair, editingHair);
        character.setHairId(originalHairId);
        if (previousScreen != null) {
            isSwitchingMenu = true;
            GLOBAL_SWITCHING = true;
        }
        Minecraft.getInstance().setScreen(previousScreen);
    }

    @Override
    public void removed() {
        if (!isSwitchingMenu && this.minecraft != null) {
            if (this.minecraft.options.guiScale().get() != oldGuiScale) {
                this.minecraft.options.guiScale().set(oldGuiScale);
                this.minecraft.resizeDisplay();
            }
        }
        super.removed();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            cancelAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        cancelAndClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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
        int x = centerX - (textWidth / 2);
        drawStringWithBorder(graphics, text, x, y, textColor);
    }
}
