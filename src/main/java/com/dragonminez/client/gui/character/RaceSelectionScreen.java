package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.ScaledScreen;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.network.C2S.StatsSyncC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.Character;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
public class RaceSelectionScreen extends ScaledScreen {

    private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
            "textures/gui/buttons/characterbuttons.png");
    private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
            "textures/gui/menu/menubig.png");

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

    private final Character character;
    private final String[] availableRaces;
    private int selectedRaceIndex = 0;
	private boolean isSwitchingMenu = false;

    private float playerRotation = 180.0f;
    private boolean isDraggingModel = false;
    private double lastMouseX = 0;

    private CustomTextureButton leftButton;
    private CustomTextureButton rightButton;
    private TexturedTextButton selectButton;

    public RaceSelectionScreen(Character character) {
        super(Component.translatable("gui.dragonminez.character_creation.title"));
        this.character = character;
        this.availableRaces = Character.getRaceNames();

        for (int i = 0; i < availableRaces.length; i++) {
            if (availableRaces[i].equals(character.getRace())) {
                selectedRaceIndex = i;
                break;
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = getUiWidth() / 2;
        int centerY = getUiHeight() / 2;

        leftButton = new CustomTextureButton.Builder()
                .position(centerX - 60 - 25, centerY + 88)
                .size(20, 20)
                .texture(BUTTONS_TEXTURE)
                .textureCoords(32, 0, 32, 14)
                .textureSize(8, 14)
                .message(Component.literal("<"))
                .onPress(btn -> {
					previousRace();
					clearWidgets();
					init();
				})
                .build();

        rightButton = new CustomTextureButton.Builder()
                .position(centerX - 60 + 145, centerY + 88)
                .size(20, 20)
                .texture(BUTTONS_TEXTURE)
                .textureCoords(20, 0, 20, 14)
                .textureSize(8, 14)
                .message(Component.literal(">"))
                .onPress(btn -> {
					nextRace();
					clearWidgets();
					init();
				})
                .build();

        selectButton = new TexturedTextButton.Builder()
                .position(getUiWidth() - 85, getUiHeight() - 25)
                .size(74, 20)
                .texture(BUTTONS_TEXTURE)
                .textureCoords(0, 28, 0, 48)
                .textureSize(74, 20)
                .message(Component.translatable("gui.dragonminez.customization.select"))
                .onPress(btn -> selectRace())
                .build();

        addRenderableWidget(leftButton);
        addRenderableWidget(rightButton);
        addRenderableWidget(selectButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPanorama(graphics, partialTick);
        this.renderCinematicBars(graphics);

        int uiMouseX = (int) Math.round(toUiX(mouseX));
        int uiMouseY = (int) Math.round(toUiY(mouseY));

        beginUiScale(graphics);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        graphics.blit(MENU_BIG, (getUiWidth() / 2) - 70, (getUiHeight() / 2) + 85, 0, 215, 149, 21);
		RenderSystem.disableBlend();

        renderPlayerModel(graphics, getUiWidth() / 2 + 5, getUiHeight() / 2 + 70, 75, uiMouseX, uiMouseY);

        super.render(graphics, uiMouseX, uiMouseY, partialTick);

        renderRaceInfo(graphics);

        endUiScale(graphics);
    }

    private void renderCinematicBars(GuiGraphics guiGraphics) {
        int totalBarHeight = (int) (this.height * 0.12);

        int fadeSize = 60;

        if (totalBarHeight <= fadeSize) {
            totalBarHeight = fadeSize + 1;
        }

        int solidHeight = totalBarHeight - fadeSize;

        int colorSolid = 0xFF000000;
        int colorTransparent = 0x00000000;

        guiGraphics.fill(0, 0, this.width, solidHeight, colorSolid);

        guiGraphics.fillGradient(0, solidHeight, this.width, solidHeight + fadeSize, colorSolid, colorTransparent);

        int bottomBarStartY = this.height - totalBarHeight;

        guiGraphics.fillGradient(0, bottomBarStartY, this.width, bottomBarStartY + fadeSize, colorTransparent, colorSolid);

        guiGraphics.fill(0, bottomBarStartY + fadeSize, this.width, this.height, colorSolid);
    }

    private void renderPanorama(GuiGraphics graphics, float partialTick) {
        String currentRace = availableRaces[selectedRaceIndex];

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

    private void renderRaceInfo(GuiGraphics graphics) {
        String currentRace = availableRaces[selectedRaceIndex];
        int centerX = getUiWidth() / 2;
        int centerY = getUiHeight() / 2;

        Component raceName = Component.translatable("race." + Reference.MOD_ID + "." + currentRace);
		drawCenteredStringWithBorder(graphics, raceName, centerX + 3, centerY + 92, 0x7CFDD6);

        Component description = Component.translatable("race." + Reference.MOD_ID + "." + currentRace + ".desc");

        int descX = 68;
        int descStartY = centerY - 50;
        int maxWidth = 130;

        List<String> wrappedLines = wrapText(description.getString(), maxWidth);
        for (String line : wrappedLines) {
            drawStringWithBorder(graphics, line, descX, descStartY, 0xFFFFFF);
            descStartY += 10;
        }
    }

	private void drawCenteredStringWithBorder(GuiGraphics graphics, Component text, int centerX, int y, int textColor) {
		int textWidth = this.font.width(text);
		int x = centerX - (textWidth / 2);
		drawStringWithBorder(graphics, text, x, y, textColor);
	}

    private void drawStringWithBorder(GuiGraphics graphics, Component text, int centerX, int y, int color) {
		String stripped = ChatFormatting.stripFormatting(text.getString());
		Component borderComponent = Component.literal(stripped != null ? stripped : text.getString());

		if (text.getStyle().isBold()) {
			borderComponent = borderComponent.copy().withStyle(style -> style.withBold(true));
		}

		graphics.drawString(font, borderComponent, centerX + 1, y, 0x000000, false);
		graphics.drawString(font, borderComponent, centerX - 1, y, 0x000000, false);
		graphics.drawString(font, borderComponent, centerX, y + 1, 0x000000, false);
		graphics.drawString(font, borderComponent, centerX, y - 1, 0x000000, false);

		graphics.drawString(font, text, centerX, y, color, false);
	}

	private void drawStringWithBorder(GuiGraphics graphics, String text, int centerX, int y, int color) {
		int textWidth = this.font.width(text);
		int x = centerX - textWidth / 2;

		graphics.drawString(this.font, text, x - 1, y, 0x000000);
		graphics.drawString(this.font, text, x + 1, y, 0x000000);
		graphics.drawString(this.font, text, x, y - 1, 0x000000);
		graphics.drawString(this.font, text, x, y + 1, 0x000000);
		graphics.drawString(this.font, text, x, y, color);
	}

    private void renderPlayerModel(GuiGraphics graphics, int x, int y, int scale, float mouseX, float mouseY) {
        LivingEntity player = Minecraft.getInstance().player;
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

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (this.font.width(testLine) <= maxWidth) {
                if (!currentLine.isEmpty()) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private void previousRace() {
        selectedRaceIndex = (selectedRaceIndex - 1 + availableRaces.length) % availableRaces.length;
        updateCharacterRace();
    }

    private void nextRace() {
        selectedRaceIndex = (selectedRaceIndex + 1) % availableRaces.length;
        updateCharacterRace();
    }

    private void updateCharacterRace() {
        String selectedRace = availableRaces[selectedRaceIndex];
        character.setRace(selectedRace);

        RaceCharacterConfig config = ConfigManager.getRaceCharacter(selectedRace);
        if (config != null) {
            character.setBodyColor(config.getDefaultBodyColor());
            character.setBodyColor2(config.getDefaultBodyColor2());
            character.setBodyColor3(config.getDefaultBodyColor3());
            character.setHairColor(config.getDefaultHairColor());
            character.setEye1Color(config.getDefaultEye1Color());
            character.setEye2Color(config.getDefaultEye2Color());
            character.setAuraColor(config.getDefaultAuraColor());
            character.setBodyType(config.getDefaultBodyType());
            character.setHairId(config.getDefaultHairType());
            character.setEyesType(config.getDefaultEyesType());
            character.setNoseType(config.getDefaultNoseType());
            character.setMouthType(config.getDefaultMouthType());
			character.setTattooType(config.getDefaultTattooType());
        }

		NetworkHandler.sendToServer(new StatsSyncC2S(character));
    }

    private void selectRace() {
        String selectedRace = availableRaces[selectedRaceIndex];
        character.setRace(selectedRace);

        if (this.minecraft != null) {
            isSwitchingMenu = true;
            GLOBAL_SWITCHING = true;
            this.minecraft.setScreen(new CharacterCustomizationScreen(this, character));
        }

		NetworkHandler.sendToServer(new StatsSyncC2S(character));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double uiMouseX = toUiX(mouseX);
        double uiMouseY = toUiY(mouseY);
        int centerX = getUiWidth() / 2 + 5;
        int centerY = getUiHeight() / 2 + 70;
        int modelRadius = 60;

        if (uiMouseX >= centerX - modelRadius && uiMouseX <= centerX + modelRadius &&
            uiMouseY >= centerY - 100 && uiMouseY <= centerY + 20) {
            isDraggingModel = true;
            lastMouseX = uiMouseX;
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
            double uiMouseX = toUiX(mouseX);
            double deltaX = uiMouseX - lastMouseX;
            playerRotation += (float)(deltaX * 0.8);
            lastMouseX = uiMouseX;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.minecraft != null) {
            this.minecraft.setScreen(null);
            return true;
        }

        if (keyCode == 263) {
            previousRace();
            return true;
        }
        if (keyCode == 262) {
            nextRace();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
