package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ColorSlider;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.TextureCounter;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.network.C2S.CreateCharacterC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.Character;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class CharacterCustomizationScreen extends Screen {

    private static final ResourceLocation BUTTONS_TEXTURE = new ResourceLocation(Reference.MOD_ID,
            "textures/gui/buttons/characterbuttons.png");

    private static final ResourceLocation MENU_GRANDE = new ResourceLocation(Reference.MOD_ID,
            "textures/gui/menu/menugrande.png");

    private static final ResourceLocation PANORAMA_HUMAN = new ResourceLocation(Reference.MOD_ID, "textures/gui/background/panorama");
    private static final ResourceLocation PANORAMA_SAIYAN = new ResourceLocation(Reference.MOD_ID, "textures/gui/background/s_panorama");
    private static final ResourceLocation PANORAMA_NAMEK = new ResourceLocation(Reference.MOD_ID, "textures/gui/background/n_panorama");
    private static final ResourceLocation PANORAMA_BIO = new ResourceLocation(Reference.MOD_ID, "textures/gui/background/bio_panorama");
    private static final ResourceLocation PANORAMA_COLD = new ResourceLocation(Reference.MOD_ID, "textures/gui/background/c_panorama");
    private static final ResourceLocation PANORAMA_MAJIN = new ResourceLocation(Reference.MOD_ID, "textures/gui/background/buu_panorama");

    private final PanoramaRenderer panoramaHuman = new PanoramaRenderer(new CubeMap(PANORAMA_HUMAN));
    private final PanoramaRenderer panoramaSaiyan = new PanoramaRenderer(new CubeMap(PANORAMA_SAIYAN));
    private final PanoramaRenderer panoramaNamek = new PanoramaRenderer(new CubeMap(PANORAMA_NAMEK));
    private final PanoramaRenderer panoramaBio = new PanoramaRenderer(new CubeMap(PANORAMA_BIO));
    private final PanoramaRenderer panoramaCold = new PanoramaRenderer(new CubeMap(PANORAMA_COLD));
    private final PanoramaRenderer panoramaMajin = new PanoramaRenderer(new CubeMap(PANORAMA_MAJIN));

    private final Screen previousScreen;
    private final Character character;
    private int currentPage = 0;

    private ColorSlider hueSlider;
    private ColorSlider saturationSlider;
    private ColorSlider valueSlider;
    private boolean colorPickerVisible = false;
    private String currentColorField = "";

    public CharacterCustomizationScreen(Screen previousScreen, Character character) {
        super(Component.literal("Character Customization"));
        this.previousScreen = previousScreen;
        this.character = character;

        initializeDefaultColors();
    }

    private void initializeDefaultColors() {
        RaceCharacterConfig config = ConfigManager.getRaceCharacter(character.getRace());
        if (config == null) return;

        if (character.getBodyColor() == null || character.getBodyColor().isEmpty()) {
            character.setBodyColor(config.getDefaultBodyColor());
        }
        if (character.getBodyColor2() == null || character.getBodyColor2().isEmpty()) {
            character.setBodyColor2(config.getDefaultBodyColor2());
        }
        if (character.getBodyColor3() == null || character.getBodyColor3().isEmpty()) {
            character.setBodyColor3(config.getDefaultBodyColor3());
        }
        if (character.getHairColor() == null || character.getHairColor().isEmpty()) {
            character.setHairColor(config.getDefaultHairColor());
        }
        if (character.getEye1Color() == null || character.getEye1Color().isEmpty()) {
            character.setEye1Color(config.getDefaultEye1Color());
        }
        if (character.getEye2Color() == null || character.getEye2Color().isEmpty()) {
            character.setEye2Color(config.getDefaultEye2Color());
        }
        if (character.getAuraColor() == null || character.getAuraColor().isEmpty()) {
            character.setAuraColor(config.getDefaultAuraColor());
        }
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        initPage();
    }

    private void initPage() {
        int centerY = this.height / 2;

        if (currentPage == 0) {
            initPage0(centerY);
        } else if (currentPage == 1) {
            initPage1(centerY);
        }

        initNavigationButtons();
        initColorPickerSliders(centerY);
    }

    private void initPage0(int centerY) {
        int eyesPosX = 113;
        int eyesPosY = centerY - 70;

        addRenderableWidget(createArrowButton(eyesPosX - 65, eyesPosY, true,
                btn -> changeEyes(-1)));
        addRenderableWidget(createArrowButton(eyesPosX, eyesPosY, false,
                btn -> changeEyes(1)));

        int nosePosX = 113;
        int nosePosY = centerY - 40;

        addRenderableWidget(createArrowButton(nosePosX - 65, nosePosY, true,
                btn -> changeNose(-1)));
        addRenderableWidget(createArrowButton(nosePosX, nosePosY, false,
                btn -> changeNose(1)));

        int mouthPosX = 113;
        int mouthPosY = centerY - 10;

        addRenderableWidget(createArrowButton(mouthPosX - 65, mouthPosY, true,
                btn -> changeMouth(-1)));
        addRenderableWidget(createArrowButton(mouthPosX, mouthPosY, false,
                btn -> changeMouth(1)));

        if (canChangeBodyType()) {
            int bodyPosX = 113;
            int bodyPosY = centerY + 20;

            addRenderableWidget(createArrowButton(bodyPosX - 65, bodyPosY, true,
                    btn -> changeBodyType(-1)));
            addRenderableWidget(createArrowButton(bodyPosX, bodyPosY, false,
                    btn -> changeBodyType(1)));
        }

        int hairPosX = 113;
        int hairPosY = centerY + 50;

        addRenderableWidget(createArrowButton(hairPosX - 65, hairPosY, true,
                btn -> changeHair(-1)));
        addRenderableWidget(createArrowButton(hairPosX, hairPosY, false,
                btn -> changeHair(1)));

        int tattooPosX = 113;
        int tattooPosY = centerY + 80;

        addRenderableWidget(createArrowButton(tattooPosX - 65, tattooPosY, true,
                btn -> changeTattoo(-1)));
        addRenderableWidget(createArrowButton(tattooPosX, tattooPosY, false,
                btn -> changeTattoo(1)));

        if (character.canHaveGender()) {
            int genderPosX = 113;
            int genderPosY = centerY + 110;

            if (character.getGender().equals(Character.GENDER_MALE)) {
                addRenderableWidget(createArrowButton(genderPosX, genderPosY, false,
                        btn -> {
                            character.setGender(Character.GENDER_FEMALE);
                            if (character.getRace().equals("majin")) {
                                character.setHairId(0);
                            }
                            refreshButtons();
                        }));
            } else {
                addRenderableWidget(createArrowButton(genderPosX - 65, genderPosY, true,
                        btn -> {
                            character.setGender(Character.GENDER_MALE);
                            if (character.getRace().equals("majin")) {
                                character.setHairId(0);
                            }
                            refreshButtons();
                        }));
            }
        }
    }

    private void initPage1(int centerY) {
        int classPosX = 135;
        int classPosY = centerY - 60;

        String currentClass = character.getCharacterClass();

        if (currentClass.equals(Character.CLASS_MARTIALARTIST)) {
            addRenderableWidget(createArrowButton(classPosX, classPosY, false,
                    btn -> {
                        character.setCharacterClass(Character.CLASS_WARRIOR);
                        refreshButtons();
                    }));
        } else if (currentClass.equals(Character.CLASS_WARRIOR)) {
            addRenderableWidget(createArrowButton(classPosX - 105, classPosY, true,
                    btn -> {
                        character.setCharacterClass(Character.CLASS_MARTIALARTIST);
                        refreshButtons();
                    }));
            addRenderableWidget(createArrowButton(classPosX, classPosY, false,
                    btn -> {
                        character.setCharacterClass(Character.CLASS_SPIRITUALIST);
                        refreshButtons();
                    }));
        } else {
            addRenderableWidget(createArrowButton(classPosX - 105, classPosY, true,
                    btn -> {
                        character.setCharacterClass(Character.CLASS_WARRIOR);
                        refreshButtons();
                    }));
        }

        int colorPosX = 72;
        int colorStartY = centerY - 30;

        addRenderableWidget(createColorButton(colorPosX - 25, colorStartY, "bodyColor"));
        addRenderableWidget(createColorButton(colorPosX, colorStartY, "bodyColor2"));
        addRenderableWidget(createColorButton(colorPosX + 25, colorStartY, "bodyColor3"));

        addRenderableWidget(createColorButton(colorPosX - 25, colorStartY + 35, "eye1Color"));
        addRenderableWidget(new CustomTextureButton.Builder()
                .position(colorPosX + 5, colorStartY + 40)
                .size(10, 10)
                .texture(BUTTONS_TEXTURE)
                .textureCoords(102, 0, 102, 10)
                .textureSize(10, 10)
                .message(Component.empty())
                .onPress(btn -> {
                    character.setEye2Color(character.getEye1Color());
                    refreshButtons();
                })
                .build());
        addRenderableWidget(createColorButton(colorPosX + 25, colorStartY + 35, "eye2Color"));
        addRenderableWidget(createColorButton(colorPosX, colorStartY + 70, "hairColor"));
        addRenderableWidget(createColorButton(colorPosX, colorStartY + 105, "auraColor"));
    }

    private void initNavigationButtons() {
        if (currentPage == 0) {
            addRenderableWidget(new TexturedTextButton.Builder()
                    .position(20, this.height - 25)
                    .size(74, 20)
                    .texture(BUTTONS_TEXTURE)
                    .textureCoords(0, 28, 0, 48)
                    .textureSize(74, 20)
                    .message(Component.translatable("gui.dragonminez.customization.back"))
                    .onPress(btn -> {
                        if (this.minecraft != null) {
                            this.minecraft.setScreen(previousScreen);
                        }
                    })
                    .build());

            addRenderableWidget(new TexturedTextButton.Builder()
                    .position(this.width - 85, this.height - 25)
                    .size(74, 20)
                    .texture(BUTTONS_TEXTURE)
                    .textureCoords(0, 28, 0, 48)
                    .textureSize(74, 20)
                    .message(Component.translatable("gui.dragonminez.customization.next"))
                    .onPress(btn -> {
                        currentPage = 1;
                        init();
                    })
                    .build());

        } else if (currentPage == 1) {
            addRenderableWidget(new TexturedTextButton.Builder()
                    .position(20, this.height - 25)
                    .size(74, 20)
                    .texture(BUTTONS_TEXTURE)
                    .textureCoords(0, 28, 0, 48)
                    .textureSize(74, 20)
                    .message(Component.translatable("gui.dragonminez.customization.back"))
                    .onPress(btn -> {
                        currentPage = 0;
                        init();
                    })
                    .build());

            addRenderableWidget(new TexturedTextButton.Builder()
                    .position(this.width - 85, this.height - 25)
                    .size(74, 20)
                    .texture(BUTTONS_TEXTURE)
                    .textureCoords(0, 28, 0, 48)
                    .textureSize(74, 20)
                    .message(Component.translatable("gui.dragonminez.customization.confirm"))
                    .onPress(btn -> finish())
                    .build());
        }
    }

    private void initColorPickerSliders(int centerY) {
        int sliderX = 180;
        int sliderY = centerY - 50;
        int sliderWidth = 80;

        hueSlider = new ColorSlider.Builder()
                .position(sliderX, sliderY)
                .size(sliderWidth, 10)
                .range(0, 360)
                .value(0)
                .message(Component.literal("Hue"))
                .onValueChange(val -> updateColorFromSliders())
                .build();

        saturationSlider = new ColorSlider.Builder()
                .position(sliderX, sliderY + 12)
                .size(sliderWidth, 10)
                .range(100, 0)
                .value(100)
                .message(Component.literal("Saturation"))
                .onValueChange(val -> updateColorFromSliders())
                .build();

        valueSlider = new ColorSlider.Builder()
                .position(sliderX, sliderY + 24)
                .size(sliderWidth, 10)
                .range(100, 0)
                .value(100)
                .message(Component.literal("Value"))
                .onValueChange(val -> updateColorFromSliders())
                .build();

        addRenderableWidget(hueSlider);
        addRenderableWidget(saturationSlider);
        addRenderableWidget(valueSlider);

        setSlidersVisible();
    }

    private void showColorPicker(String fieldName) {
        currentColorField = fieldName;
        colorPickerVisible = true;

        String currentColor = getColorFromField(fieldName);
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

        setSlidersVisible();
    }

    private void hideColorPicker() {
        colorPickerVisible = false;
        currentColorField = "";
        setSlidersVisible();
        refreshButtons();
    }

    private void setSlidersVisible() {
        if (hueSlider != null) hueSlider.visible = colorPickerVisible;
        if (saturationSlider != null) saturationSlider.visible = colorPickerVisible;
        if (valueSlider != null) valueSlider.visible = colorPickerVisible;
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
        applyColor(newColor);
    }

    private CustomTextureButton createArrowButton(int x, int y, boolean isLeft, CustomTextureButton.OnPress onPress) {
        return new CustomTextureButton.Builder()
                .position(x, y)
                .size(20, 20)
                .texture(BUTTONS_TEXTURE)
                .textureCoords(isLeft ? 32 : 20, 0, isLeft ? 32 : 20, 14)
                .textureSize(8, 14)
                .message(Component.empty())
                .onPress(onPress)
                .build();
    }

    private TexturedTextButton createColorButton(int x, int y, String fieldName) {
        String currentColor = getColorFromField(fieldName);
        if (currentColor == null || currentColor.isEmpty()) {
            currentColor = "#FFFFFF";
        }
        int colorInt = ColorUtils.hexToInt(currentColor);

        return new TexturedTextButton.Builder()
                .position(x, y)
                .size(20, 20)
                .texture(BUTTONS_TEXTURE)
                .textureCoords(42, 15, 42, 15)
                .textureSize(5, 5)
                .message(Component.empty())
                .backgroundColor(colorInt)
                .onPress(btn -> showColorPicker(fieldName))
                .build();
    }

    private void changeHair(int delta) {
        int maxHair = TextureCounter.getMaxHairTypes(character.getRace());
        if (maxHair == 0) maxHair = 5;

        int newHair = character.getHairId() + delta;

        if (newHair < 0) {
            newHair = maxHair;
        } else if (newHair > maxHair) {
            newHair = 0;
        }

        character.setHairId(newHair);
        refreshButtons();
    }


    private void changeBodyType(int delta) {
        int maxType = TextureCounter.getMaxBodyTypes(character.getRace(), character.getGender());
        if (maxType < 0) {
            if (character.getRace().equals("human") || character.getRace().equals("saiyan")) {
                maxType = 1;
            } else {
                maxType = 0;
            }
        }

        int currentType = character.getBodyType();

        int newType = currentType + delta;

        if (newType < 0) {
            newType = maxType;
        } else if (newType > maxType) {
            newType = 0;
        }

        character.setBodyType(newType);
        refreshButtons();
    }

    private boolean canChangeBodyType() {
        RaceCharacterConfig config = ConfigManager.getRaceCharacter(character.getRace());

        if (config == null) {
            return false;
        }

        if (config.useVanillaSkin()) {
            return false;
        }

        return true;
    }

    private void changeEyes(int delta) {
        int maxEyes = TextureCounter.getMaxEyesTypes(character.getRace());
        if (maxEyes == 0) maxEyes = 1;

        int newEyes = character.getEyesType() + delta;
        if (newEyes < 0) newEyes = maxEyes;
        if (newEyes > maxEyes) newEyes = 0;
        character.setEyesType(newEyes);
        refreshButtons();
    }

    private void changeNose(int delta) {
        int maxNose = TextureCounter.getMaxNoseTypes(character.getRace());
        if (maxNose == 0) maxNose = 1;

        int newNose = character.getNoseType() + delta;
        if (newNose < 0) newNose = maxNose;
        if (newNose > maxNose) newNose = 0;
        character.setNoseType(newNose);
        refreshButtons();
    }

    private void changeMouth(int delta) {
        int maxMouth = TextureCounter.getMaxMouthTypes(character.getRace());
        if (maxMouth == 0) maxMouth = 1;

        int newMouth = character.getMouthType() + delta;
        if (newMouth < 0) newMouth = maxMouth;
        if (newMouth > maxMouth) newMouth = 0;
        character.setMouthType(newMouth);
        refreshButtons();
    }

	private void changeTattoo(int delta) {
		int maxTattoo = TextureCounter.getMaxTattooTypes(character.getRace());
		if (maxTattoo == 0) maxTattoo = 1;

		int newTattoo = character.getTattooType() + delta;
		if (newTattoo < 0) newTattoo = maxTattoo;
		if (newTattoo > maxTattoo) newTattoo = 0;
		character.setTattooType(newTattoo);
		refreshButtons();
	}

    private void refreshButtons() {
        String savedColorField = currentColorField;
        boolean wasColorPickerVisible = colorPickerVisible;

        float savedH = 0, savedS = 0, savedV = 0;
        if (colorPickerVisible && hueSlider != null) {
            savedH = hueSlider.getValue();
            savedS = saturationSlider.getValue();
            savedV = valueSlider.getValue();
        }

        clearWidgets();
        initPage();

        if (wasColorPickerVisible) {
            currentColorField = savedColorField;
            colorPickerVisible = true;

            if (hueSlider != null) {
                hueSlider.setValue((int) savedH);
                saturationSlider.setValue((int) savedS);
                valueSlider.setValue((int) savedV);

                saturationSlider.setCurrentHue(savedH);
                valueSlider.setCurrentHue(savedH);
                valueSlider.setCurrentSaturation(savedS);
            }

            setSlidersVisible();
        }
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

        String result = (color != null && !color.isEmpty()) ? color : "#FFFFFF";
        return result;
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
    }

    private void finish() {
        if (this.minecraft != null) {
            NetworkHandler.sendToServer(new CreateCharacterC2S(character));
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPanorama(partialTick);

        int centerY = this.height / 2;
        int panelX = 10;
        int panelY = centerY - 110;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(MENU_GRANDE, panelX, panelY, 0, 0, 148, 222);

        if (currentPage == 1) {
            int statsPanelX = this.width - 158;
            int statsPanelY = centerY - 110;
            graphics.blit(MENU_GRANDE, statsPanelX, statsPanelY, 0, 0, 148, 222);
        }

        RenderSystem.disableBlend();

        renderPlayerModel(graphics, this.width / 2 + 5, this.height / 2 + 70, 75, mouseX, mouseY);

        if (colorPickerVisible) {
            renderColorPickerBackground(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        renderPageContent(graphics, centerY);

        if (colorPickerVisible) {
            renderColorPreviewSquare(graphics);
        }

        if (currentPage == 1) {
            renderBaseStats(graphics, centerY);
        }
    }

    private void renderPanorama(float partialTick) {
        String currentRace = character.getRace();

        PanoramaRenderer panorama = switch (currentRace) {
            case "saiyan" -> panoramaSaiyan;
            case "namekian" -> panoramaNamek;
            case "bioandroid" -> panoramaBio;
            case "frostdemon" -> panoramaCold;
            case "majin" -> panoramaMajin;
            default -> panoramaHuman;
        };

        panorama.render(partialTick, 1.0F);
    }

    private void renderPageContent(GuiGraphics graphics, int centerY) {
        int textX = 84;

        if (currentPage == 0) {
            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.eyes").getString(), textX, centerY - 78, 0xFF9B9B);
            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.type", character.getEyesType() + 1).getString(), textX, centerY - 66, 0xFFFFFF);

            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.nose").getString(), textX, centerY - 48, 0xFF9B9B);
            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.type", character.getNoseType() + 1).getString(), textX, centerY - 36, 0xFFFFFF);

            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.mouth").getString(), textX, centerY - 18, 0xFF9B9B);
            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.type", character.getMouthType() + 1).getString(), textX, centerY - 6, 0xFFFFFF);

            if (canChangeBodyType()) {
                drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.body_type").getString(), textX, centerY + 12, 0xFF9B9B);

                String race = character.getRace();
                int bodyType = character.getBodyType();
                String bodyTypeText;

                if (race.equals("human") || race.equals("saiyan")) {
                    bodyTypeText = bodyType == 0 ? Component.translatable("gui.dragonminez.customization.body_type.default").getString()
                                                 : Component.translatable("gui.dragonminez.customization.body_type.custom").getString();
                } else {
                    bodyTypeText = Component.translatable("gui.dragonminez.customization.type", bodyType + 1).getString();
                }

                drawCenteredStringWithBorder(graphics, bodyTypeText, textX, centerY + 24, 0xFFFFFF);
            }

            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.hair").getString(), textX, centerY + 42, 0xFF9B9B);
            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.type", character.getHairId() + 1).getString(), textX, centerY + 54, 0xFFFFFF);

            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.tattoo").getString(), textX, centerY + 72, 0xFF9B9B);
            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.type", character.getTattooType() + 1).getString(), textX, centerY + 84, 0xFFFFFF);

            if (character.canHaveGender()) {
                drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.gender").getString(), textX, centerY + 102, 0xFF9B9B);
                String genderText = Component.translatable("gender.dragonminez." + character.getGender()).getString();
                int genderColor = character.getGender().equals(Character.GENDER_MALE) ? 0x2133A6 : 0xFC63D9;
                drawCenteredStringWithBorder(graphics, genderText, textX, centerY + 114, 0xFFFFFF, genderColor);
            }
        } else if (currentPage == 1) {
            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.class").getString(), textX, centerY - 70, 0xFF9B9B);

            String className = Component.translatable("class.dragonminez." + character.getCharacterClass()).getString();
            drawCenteredStringWithBorder(graphics, className, textX, centerY - 58, 0xFFFFFF);

            int labelX = 84;
            int labelStartY = centerY - 40;

            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.body").getString(), labelX, labelStartY, 0xFF9B9B);
            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.eyes").getString(), labelX, labelStartY + 35, 0xFF9B9B);
            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.hair").getString(), labelX, labelStartY + 70, 0xFF9B9B);
            drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.aura").getString(), labelX, labelStartY + 105, 0xFF9B9B);
        }
    }

    private void renderPlayerModel(GuiGraphics graphics, int x, int y, int scale, float mouseX, float mouseY) {
        LivingEntity player = Minecraft.getInstance().player;
        if (player == null) return;

        float xRotation = (float) Math.atan((double)((float)y - mouseY) / 40.0F);
        float yRotation = (float) Math.atan((double)((float)x - mouseX) / 40.0F);

        Quaternionf pose = (new Quaternionf()).rotateZ((float)Math.PI);
        Quaternionf cameraOrientation = (new Quaternionf()).rotateX(xRotation * 20.0F * ((float)Math.PI / 180F));
        pose.mul(cameraOrientation);

        float yBodyRotO = player.yBodyRot;
        float yRotO = player.getYRot();
        float xRotO = player.getXRot();
        float yHeadRotO = player.yHeadRotO;
        float yHeadRot = player.yHeadRot;

        player.yBodyRot = 180.0F + yRotation * 20.0F;
        player.setYRot(180.0F + yRotation * 40.0F);
        player.setXRot(-xRotation * 20.0F);
        player.yHeadRot = player.getYRot();
        player.yHeadRotO = player.getYRot();

        graphics.pose().pushPose();
        graphics.pose().translate(0.0D, 0.0D, -150.0D);
        InventoryScreen.renderEntityInInventory(graphics, x, y, scale, pose, cameraOrientation, player);
        graphics.pose().popPose();

        player.yBodyRot = yBodyRotO;
        player.setYRot(yRotO);
        player.setXRot(xRotO);
        player.yHeadRotO = yHeadRotO;
        player.yHeadRot = yHeadRot;
    }

    private void renderColorPickerBackground(GuiGraphics graphics) {
        int sliderX = 180;
        int sliderY = this.height / 2 - 50;
        int sliderWidth = 80;
        int sliderHeight = 34;
        int previewSize = 34;

        int totalWidth = sliderWidth + previewSize + 10;
        int totalHeight = sliderHeight + 10;
        graphics.fill(sliderX - 5, sliderY - 5, sliderX + totalWidth, sliderY + totalHeight, 0x66000000);
    }

    private void renderColorPreviewSquare(GuiGraphics graphics) {
        if (hueSlider == null) return;

        int sliderX = 180;
        int sliderY = this.height / 2 - 50;
        int sliderWidth = 80;
        int previewSize = 34;
        int previewX = sliderX + sliderWidth + 5;

        float h = hueSlider.getValue();
        float s = saturationSlider.getValue();
        float v = valueSlider.getValue();

        int[] rgb = ColorUtils.hsvToRgb(h, s, v);
        int color = ColorUtils.rgbToInt(rgb[0], rgb[1], rgb[2]);

        graphics.fill(previewX - 1, sliderY - 1, previewX + previewSize + 1, sliderY + previewSize + 1, 0xFFFFFFFF);
        graphics.fill(previewX, sliderY, previewX + previewSize, sliderY + previewSize, 0xFF000000 | color);
    }

    private void drawStringWithBorder(GuiGraphics graphics, String text, int x, int y, int color) {
        drawStringWithBorder(graphics, text, x, y, color, color);
    }

    private void drawStringWithBorder(GuiGraphics graphics, String text, int x, int y, int textColor, int borderColor) {
        graphics.drawString(this.font, text, x - 1, y, 0x000000);
        graphics.drawString(this.font, text, x + 1, y, 0x000000);
        graphics.drawString(this.font, text, x, y - 1, 0x000000);
        graphics.drawString(this.font, text, x, y + 1, 0x000000);
        graphics.drawString(this.font, text, x, y, textColor);
    }

    private void drawCenteredStringWithBorder(GuiGraphics graphics, String text, int centerX, int y, int color) {
        drawCenteredStringWithBorder(graphics, text, centerX, y, color, color);
    }

    private void drawCenteredStringWithBorder(GuiGraphics graphics, String text, int centerX, int y, int textColor, int borderColor) {
        int textWidth = this.font.width(text);
        int x = centerX - textWidth / 2;
        graphics.drawString(this.font, text, x - 1, y, 0x000000);
        graphics.drawString(this.font, text, x + 1, y, 0x000000);
        graphics.drawString(this.font, text, x, y - 1, 0x000000);
        graphics.drawString(this.font, text, x, y + 1, 0x000000);
        graphics.drawString(this.font, text, x, y, textColor);
    }

    private void renderBaseStats(GuiGraphics graphics, int centerY) {
        RaceStatsConfig statsConfig = ConfigManager.getRaceStats(character.getRace());
        if (statsConfig == null) return;

        String currentClass = character.getCharacterClass();
        RaceStatsConfig.ClassStats classStats = switch (currentClass) {
            case Character.CLASS_WARRIOR -> statsConfig.getWarrior();
            case Character.CLASS_SPIRITUALIST -> statsConfig.getSpiritualist();
            case Character.CLASS_MARTIALARTIST -> statsConfig.getMartialArtist();
            default -> statsConfig.getWarrior();
        };

        if (classStats == null || classStats.getBaseStats() == null || classStats.getStatScaling() == null) return;

        RaceStatsConfig.BaseStats baseStats = classStats.getBaseStats();
		RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();

        int statsPanelX = this.width - 158;
        int centerX = statsPanelX + 74;
        int startY = centerY - 90;

        drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.customization.base_stats").getString(), centerX, startY, 0xFF9B9B);
        startY += 20;

        drawCenteredStringWithBorder(graphics, "STR", centerX - 40, startY, 0x7CFDD6);
        drawCenteredStringWithBorder(graphics, String.valueOf(baseStats.getStrength()), centerX - 40, startY + 12, 0xFFFFFF);

        drawCenteredStringWithBorder(graphics, "SKP", centerX, startY, 0x7CFDD6);
        drawCenteredStringWithBorder(graphics, String.valueOf(baseStats.getStrikePower()), centerX, startY + 12, 0xFFFFFF);

        drawCenteredStringWithBorder(graphics, "RES", centerX + 40, startY, 0x7CFDD6);
        drawCenteredStringWithBorder(graphics, String.valueOf(baseStats.getResistance()), centerX + 40, startY + 12, 0xFFFFFF);

        startY += 35;

        drawCenteredStringWithBorder(graphics, "VIT", centerX - 40, startY, 0x7CFDD6);
        drawCenteredStringWithBorder(graphics, String.valueOf(baseStats.getVitality()), centerX - 40, startY + 12, 0xFFFFFF);

        drawCenteredStringWithBorder(graphics, "PWR", centerX, startY, 0x7CFDD6);
        drawCenteredStringWithBorder(graphics, String.valueOf(baseStats.getKiPower()), centerX, startY + 12, 0xFFFFFF);

        drawCenteredStringWithBorder(graphics, "ENE", centerX + 40, startY, 0x7CFDD6);
        drawCenteredStringWithBorder(graphics, String.valueOf(baseStats.getEnergy()), centerX + 40, startY + 12, 0xFFFFFF);

        startY += 35;

        double maxMeleeDamage = baseStats.getStrength() * scaling.getStrengthScaling();
        double maxStrikeDamage = baseStats.getStrikePower() * scaling.getStrikePowerScaling() + (baseStats.getStrength() * scaling.getStrengthScaling()) * 0.25;
		int maxStamina = 100 + (int) (baseStats.getResistance() * scaling.getStaminaScaling());
		double maxDefense = baseStats.getResistance() * scaling.getDefenseScaling();
		double maxHealth = 20 + (baseStats.getVitality() * scaling.getVitalityScaling());
        double maxKiDamage = baseStats.getKiPower() * scaling.getKiPowerScaling();
		int maxEnergy = 100 + (int) (baseStats.getEnergy() * scaling.getEnergyScaling());

        int rowY = startY;
        int labelX = centerX - 55;
        int valueX = centerX + 25;

        drawStringWithBorder(graphics, "Melee Damage", labelX, rowY, 0x7CFDD6);
        drawStringWithBorder(graphics, String.format(Locale.US, "%.1f", maxMeleeDamage), valueX, rowY, 0xFFFFFF);

        rowY += 12;
        drawStringWithBorder(graphics, "Strike Damage", labelX, rowY, 0x7CFDD6);
        drawStringWithBorder(graphics, String.format(Locale.US, "%.1f", maxStrikeDamage), valueX, rowY, 0xFFFFFF);

		rowY += 12;
		drawStringWithBorder(graphics, "Defense", labelX, rowY, 0x7CFDD6);
		drawStringWithBorder(graphics, String.format(Locale.US, "%.1f", maxDefense), valueX, rowY, 0xFFFFFF);

		rowY += 12;
		drawStringWithBorder(graphics, "Stamina", labelX, rowY, 0x7CFDD6);
		drawStringWithBorder(graphics, String.valueOf(maxStamina), valueX, rowY, 0xFFFFFF);

		rowY += 12;
		drawStringWithBorder(graphics, "Health", labelX, rowY, 0x7CFDD6);
		drawStringWithBorder(graphics, String.format(Locale.US, "%.1f", maxHealth), valueX, rowY, 0xFFFFFF);

		rowY += 12;
		drawStringWithBorder(graphics, "Ki Damage", labelX, rowY, 0x7CFDD6);
		drawStringWithBorder(graphics, String.format(Locale.US, "%.1f", maxKiDamage), valueX, rowY, 0xFFFFFF);

		rowY += 12;
		drawStringWithBorder(graphics, "Energy", labelX, rowY, 0x7CFDD6);
		drawStringWithBorder(graphics, String.valueOf(maxEnergy), valueX, rowY, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPickerVisible) {
            int sliderX = 180;
            int sliderY = this.height / 2 - 50;
            int sliderWidth = 80;
            int previewSize = 34;
            int totalWidth = sliderWidth + previewSize + 10;
            int totalHeight = 44;

            if (mouseX < sliderX - 5 || mouseX > sliderX + totalWidth ||
                mouseY < sliderY - 5 || mouseY > sliderY + totalHeight) {
                hideColorPicker();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (colorPickerVisible) {
                hideColorPicker();
                return true;
            }
            if (this.minecraft != null) {
                this.minecraft.setScreen(previousScreen);
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(previousScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
