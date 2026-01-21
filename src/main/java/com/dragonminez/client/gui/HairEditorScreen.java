package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ColorSlider;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.CharacterCustomizationScreen;
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

    private static final float MIN_SCALE = 0.1f;
    private static final float MAX_SCALE = 3.0f;

    protected static boolean GLOBAL_SWITCHING = false;

    private final Screen previousScreen;
    private final Character character;
    private final CustomHair editingHair;
    private final CustomHair backupHair;
    private final int oldGuiScale;
    private final boolean usePanorama;
    private boolean isSwitchingMenu = false;

    private HairFace currentFace = HairFace.FRONT;
    private int selectedStrandIndex = 0;

    private EditMode editMode = EditMode.LENGTH;
    private float editStep = 1.0f;

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

        // Detectar si viene de CharacterCustomizationScreen para usar panorama
        this.usePanorama = previousScreen instanceof CharacterCustomizationScreen;

        Minecraft mc = Minecraft.getInstance();

        // Si viene de CharacterCustomizationScreen, heredar su oldGuiScale
        if (previousScreen instanceof CharacterCustomizationScreen) {
            this.oldGuiScale = ((CharacterCustomizationScreen) previousScreen).getOldGuiScale();
            // NO cambiar guiScale, CharacterCustomizationScreen ya lo tiene en 3
        } else {
            // Si viene de null u otro lugar, guardar y establecer guiScale
            this.oldGuiScale = mc.options.guiScale().get();
            if (oldGuiScale != 3) {
                mc.options.guiScale().set(3);
                mc.resizeDisplay();
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

        buttonY += 20;

        addRenderableWidget(Button.builder(
            Component.literal("0.5"),
            btn -> editStep = 0.5f
        ).bounds(leftPanelX + 26, buttonY, 28, 16).build());

        addRenderableWidget(Button.builder(
            Component.literal("1"),
            btn -> editStep = 1.0f
        ).bounds(leftPanelX + 56, buttonY, 24, 16).build());

        addRenderableWidget(Button.builder(
            Component.literal("5"),
            btn -> editStep = 5.0f
        ).bounds(leftPanelX + 82, buttonY, 24, 16).build());
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
                            if (s != null) s.removeCube();
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
                            if (s != null) s.addCube();
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
        int btnX = panelX + 30;

        // X-
        controlButtons.add(createControlButton(btnX, btnY, false, () -> applyAxisEdit(-editStep, 0, 0)));
        // X+
        controlButtons.add(createControlButton(btnX + 65, btnY, true, () -> applyAxisEdit(editStep, 0, 0)));

        // Y-
        controlButtons.add(createControlButton(btnX, btnY + 26, false, () -> applyAxisEdit(0, -editStep, 0)));
        // Y+
        controlButtons.add(createControlButton(btnX + 65, btnY + 26, true, () -> applyAxisEdit(0, editStep, 0)));

        // Z-
        controlButtons.add(createControlButton(btnX, btnY + 52, false, () -> applyAxisEdit(0, 0, -editStep)));
        // Z+
        controlButtons.add(createControlButton(btnX + 65, btnY + 52, true, () -> applyAxisEdit(0, 0, editStep)));
    }

    private void createScaleButtons(int panelX, int btnY) {
        int btnX = panelX + 30;

        // X-
        controlButtons.add(createControlButton(btnX, btnY, false, () -> applyScaleEdit(-editStep * 0.1f, 0, 0)));
        // X+
        controlButtons.add(createControlButton(btnX + 65, btnY, true, () -> applyScaleEdit(editStep * 0.1f, 0, 0)));

        // Y-
        controlButtons.add(createControlButton(btnX, btnY + 26, false, () -> applyScaleEdit(0, -editStep * 0.1f, 0)));
        // Y+
        controlButtons.add(createControlButton(btnX + 65, btnY + 26, true, () -> applyScaleEdit(0, editStep * 0.1f, 0)));

        // Z-
        controlButtons.add(createControlButton(btnX, btnY + 52, false, () -> applyScaleEdit(0, 0, -editStep * 0.1f)));
        // Z+
        controlButtons.add(createControlButton(btnX + 65, btnY + 52, true, () -> applyScaleEdit(0, 0, editStep * 0.1f)));
    }

    private CustomTextureButton createControlButton(int x, int y, boolean isIncrease, Runnable action) {
        CustomTextureButton btn = new CustomTextureButton.Builder()
                .position(x, y)
                .size(14, 11)
                .texture(STAT_BUTTONS)
                .textureCoords(isIncrease ? 0 : 142, 0, isIncrease ? 0 : 142, 10)
                .textureSize(10, 10)
                .onPress(button -> action.run())
                .sound(MainSounds.UI_MENU_SWITCH.get())
                .build();
        this.addRenderableWidget(btn);
        return btn;
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
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (usePanorama) {
            renderPanorama(partialTick);
        } else {
            this.renderBackground(graphics);
        }

        renderPlayerModel(graphics, this.width / 2, this.height / 2 + 40, 75);

        renderLeftPanel(graphics);
        renderRightPanel(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
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

        drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.hair_editor.step", editStep),
                panelX + 15, startY, 0x00FF00);
        startY += 20;

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
            case ROTATION -> {
                graphics.pose().pushPose();
                graphics.pose().scale(0.85f, 0.85f, 0.85f);
                int scaledX = (int)(panelX / 0.85f) + 18;
                int scaledY = (int)(startY / 0.85f);
                drawStringWithBorder(graphics, Component.literal(String.format("X: %.1f", strand.getRotationX())), scaledX + 45, scaledY, 0xFF6666);
                drawStringWithBorder(graphics, Component.literal(String.format("Y: %.1f", strand.getRotationY())), scaledX + 45, scaledY + 30, 0x66FF66);
                drawStringWithBorder(graphics, Component.literal(String.format("Z: %.1f", strand.getRotationZ())), scaledX + 45, scaledY + 60, 0x6666FF);
                graphics.pose().popPose();
            }
            case CURVE -> {
                graphics.pose().pushPose();
                graphics.pose().scale(0.85f, 0.85f, 0.85f);
                int scaledX = (int)(panelX / 0.85f) + 18;
                int scaledY = (int)(startY / 0.85f);
                drawStringWithBorder(graphics, Component.literal(String.format("X: %.1f", strand.getCurveX())), scaledX + 45, scaledY, 0xFF6666);
                drawStringWithBorder(graphics, Component.literal(String.format("Y: %.1f", strand.getCurveY())), scaledX + 45, scaledY + 30, 0x66FF66);
                drawStringWithBorder(graphics, Component.literal(String.format("Z: %.1f", strand.getCurveZ())), scaledX + 45, scaledY + 60, 0x6666FF);
                graphics.pose().popPose();
            }
            case SCALE -> {
                graphics.pose().pushPose();
                graphics.pose().scale(0.85f, 0.85f, 0.85f);
                int scaledX = (int)(panelX / 0.85f) + 18;
                int scaledY = (int)(startY / 0.85f);
                drawStringWithBorder(graphics, Component.literal(String.format("X: %.2f", strand.getScaleX())), scaledX + 45, scaledY, 0xFF6666);
                drawStringWithBorder(graphics, Component.literal(String.format("Y: %.2f", strand.getScaleY())), scaledX + 45, scaledY + 30, 0x66FF66);
                drawStringWithBorder(graphics, Component.literal(String.format("Z: %.2f", strand.getScaleZ())), scaledX + 45, scaledY + 60, 0x6666FF);
                graphics.pose().popPose();
            }
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
        int centerY = this.height / 2 + 40;
        int modelRadius = 60;

        if (mouseX >= centerX - modelRadius && mouseX <= centerX + modelRadius &&
            mouseY >= centerY - 100 && mouseY <= centerY + 20) {
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

    private void applyAxisEdit(float dx, float dy, float dz) {
        HairStrand strand = getSelectedStrand();
        if (strand == null) return;

        switch (editMode) {
            case ROTATION -> strand.addRotation(dx * 10, dy * 10, dz * 10);
            case CURVE -> strand.setCurve(
                strand.getCurveX() + dx,
                strand.getCurveY() + dy,
                strand.getCurveZ() + dz
            );
            default -> {}
        }
    }

    private void applyScaleEdit(float dx, float dy, float dz) {
        HairStrand strand = getSelectedStrand();
        if (strand == null) return;

        float newX = Math.max(MIN_SCALE, Math.min(MAX_SCALE, strand.getScaleX() + dx));
        float newY = Math.max(MIN_SCALE, Math.min(MAX_SCALE, strand.getScaleY() + dy));
        float newZ = Math.max(MIN_SCALE, Math.min(MAX_SCALE, strand.getScaleZ() + dz));

        strand.setScale(newX, newY, newZ);
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
