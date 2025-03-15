package com.yuseix.dragonminez.client.gui.cc;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.client.gui.buttons.TextButton;
import com.yuseix.dragonminez.network.C2S.CharacterC2S;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ColorPickerScreen extends Screen {

    private static final ResourceLocation IMAGENES = new ResourceLocation(DragonMineZ.MOD_ID, "textures/gui/colores.png");
    private static final ResourceLocation menu = new ResourceLocation(DragonMineZ.MOD_ID,
            "textures/gui/menulargo.png");
    private static final ResourceLocation texto = new ResourceLocation(DragonMineZ.MOD_ID,
            "textures/gui/menutexto.png");
    //Textura de los panoramas
    private static final ResourceLocation PANORAMA_PATH = new ResourceLocation(DragonMineZ.MOD_ID, "textures/gui/background/panorama");
    private static final ResourceLocation PANORAMA_BUU = new ResourceLocation(DragonMineZ.MOD_ID, "textures/gui/background/buu_panorama");
    private static final ResourceLocation PANORAMA_BIO = new ResourceLocation(DragonMineZ.MOD_ID, "textures/gui/background/bio_panorama");
    private static final ResourceLocation PANORAMA_SAIYAN = new ResourceLocation(DragonMineZ.MOD_ID, "textures/gui/background/s_panorama");
    private static final ResourceLocation PANORAMA_NAMEK = new ResourceLocation(DragonMineZ.MOD_ID, "textures/gui/background/n_panorama");
    private static final ResourceLocation PANORAMA_COLD = new ResourceLocation(DragonMineZ.MOD_ID, "textures/gui/background/c_panorama");

    //Panoramas
    private final PanoramaRenderer customPanorama = new PanoramaRenderer(new CubeMap(PANORAMA_PATH));
    private final PanoramaRenderer panoramaBuu = new PanoramaRenderer(new CubeMap(PANORAMA_BUU));
    private final PanoramaRenderer panoramaBio = new PanoramaRenderer(new CubeMap(PANORAMA_BIO));
    private final PanoramaRenderer panoramaSai = new PanoramaRenderer(new CubeMap(PANORAMA_SAIYAN));
    private final PanoramaRenderer panoramaNam = new PanoramaRenderer(new CubeMap(PANORAMA_NAMEK));
    private final PanoramaRenderer panoramaCold = new PanoramaRenderer(new CubeMap(PANORAMA_COLD));

    private int selectedColor = 0xFFFFFFFF; // Color blanco por defecto
    private float brightness = 1.0f; // Nivel de brillo, 1.0 por defecto (máximo brillo)
    private String tipoColor = "";

    // Nuevas variables para almacenar los componentes de color
    private int ColorR; private int ColorG; private int ColorB; private int decimalColor = 16777215;

    // BufferedImage para almacenar el cuadro de colores y la barra de brillo
    private BufferedImage colorPickerImage; private BufferedImage brightnessBarImage;
    private TextButton backButton, setColor;

    public ColorPickerScreen(String tipoColor) {
        super(Component.literal("Color Picker"));
        this.tipoColor = tipoColor;
    }

    @Override
    protected void init() {
        if (this.minecraft.level.isClientSide()) {
            int posX = this.width/2+20;

            switch (tipoColor){
                case "BodyColor1":
                    this.setColor = (TextButton) this.addRenderableWidget(new TextButton(posX, (this.height/2) + 13, Component.literal("SET"), wa -> {
                        ModMessages.sendToServer(new CharacterC2S("BodyColor1", decimalColor));
                        this.minecraft.setScreen(new CCustomizationPage(Component.empty()));
                    }));
                    break;
                case "BodyColor2":
                    this.setColor = (TextButton) this.addRenderableWidget(new TextButton(posX, (this.height/2) + 13, Component.literal("SET"), wa -> {
                        ModMessages.sendToServer(new CharacterC2S("BodyColor2", decimalColor));
                        this.minecraft.setScreen(new CCustomizationPage(Component.empty()));
                    }));
                    break;
                case "BodyColor3":
                    this.setColor = (TextButton) this.addRenderableWidget(new TextButton(posX, (this.height/2) + 13, Component.literal("SET"), wa -> {
                        ModMessages.sendToServer(new CharacterC2S("BodyColor3", decimalColor));
                        this.minecraft.setScreen(new CCustomizationPage(Component.empty()));
                    }));
                    break;
                case "eye1Color":
                    this.setColor = (TextButton) this.addRenderableWidget(new TextButton(posX, (this.height/2) + 13, Component.literal("SET"), wa -> {
                        ModMessages.sendToServer(new CharacterC2S("eye1Color", decimalColor));
                        this.minecraft.setScreen(new CCustomizationPage(Component.empty()));
                    }));
                    break;
                case "eye2Color":
                    this.setColor = (TextButton) this.addRenderableWidget(new TextButton(posX, (this.height/2) + 13, Component.literal("SET"), wa -> {
                        ModMessages.sendToServer(new CharacterC2S("eye2Color", decimalColor));
                        this.minecraft.setScreen(new CCustomizationPage(Component.empty()));
                    }));
                    break;
                case "hairColor":
                    this.setColor = (TextButton) this.addRenderableWidget(new TextButton(posX, (this.height/2) + 13, Component.literal("SET"), wa -> {
                        ModMessages.sendToServer(new CharacterC2S("hairColor", decimalColor));
                        this.minecraft.setScreen(new CCustomizationPage(Component.empty()));
                    }));
                    break;
                case "auraColor":
                    this.setColor = (TextButton) this.addRenderableWidget(new TextButton(posX, (this.height/2) + 13, Component.literal("SET"), wa -> {
                        ModMessages.sendToServer(new CharacterC2S("auraColor", decimalColor));
                        this.minecraft.setScreen(new CCustomizationPage(Component.empty()));
                    }));
                    break;
            }

            this.backButton = (TextButton) this.addRenderableWidget(new TextButton(this.width/2-110, (this.height/2) + 42, Component.translatable("dmz.ccreation.back").withStyle(ChatFormatting.BOLD), button -> {
                this.minecraft.setScreen(new CCustomizationPage(Component.empty()));
            }));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        //Panoramas
        panoramas(guiGraphics, partialTicks);

        //MenuLargo
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.blit(menu, (this.width/2) - 125, (this.height/2) - 75, 0, 0, 250, 150);
        RenderSystem.disableBlend();
        //Cuadro para Texto
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, texto);
        guiGraphics.blit(texto, this.width/2+20, this.height/2-61, 0, 0, 73, 15);
        RenderSystem.disableBlend();
        //Texto wa
        guiGraphics.drawString(font, Component.literal("COLOR").withStyle(ChatFormatting.BOLD), this.width/2+40, this.height/2-57, decimalColor);


        int ColorPickerX = (this.width / 2) - 110; int ColorPickerY = (this.height / 2) - 61;

        renderColorPickerImage(guiGraphics, ColorPickerX, ColorPickerY);

        int SaturationX = (this.width / 2) - 5; int SaturationY = (this.height / 2) - 61;

        // Dibuja la barra de saturación
        renderBrightnessBarImage(guiGraphics, SaturationX, SaturationY);
        // Dibuja la vista previa del color seleccionado
        drawColorPreview(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
    // Renderiza la imagen del cuadro de colores
    private void renderColorPickerImage(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0f); // Aplica brillo
        RenderSystem.setShaderTexture(0, IMAGENES);
        guiGraphics.blit(IMAGENES, x, y, 0, 0, 100, 100); // 100x100 es el tamaño de la imagen
        RenderSystem.disableBlend();
    }

    // Renderiza la imagen de la barra de brillo
    private void renderBrightnessBarImage(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0f); // Aplica brillo
        RenderSystem.setShaderTexture(0, IMAGENES);
        guiGraphics.blit(IMAGENES, x, y, 100, 0, 10, 100); // 10x100 es el tamaño de la barra
        RenderSystem.disableBlend();
    }


    // Método para obtener el color basado en la posición dentro del cuadro de colores
    private int getColorFromPosition(int x, int y) {
        float hue = (float) x / 100f;
        float saturation = (float) y / 100f;
        // Aplica el brillo actual en el cálculo del color
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    private void drawColorPreview(GuiGraphics guiGraphics) {
        int PreviewX = (this.width / 2) + 32;
        int PreviewY = (this.height / 2) - 42;
        // Dibuja un rectángulo de 50x50 con el color seleccionado
        guiGraphics.fill(PreviewX, PreviewY, PreviewX + 50, PreviewY + 50, selectedColor);

        // Puedes agregar un borde alrededor de la vista previa si quieres
        drawRectBorder(guiGraphics, PreviewX, PreviewY, PreviewX + 50, PreviewY + 50, 0xFF000000); // Color negro para el borde
    }

    private void drawRectBorder(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int borderColor) {
        guiGraphics.fill(x1 - 1, y1 - 1, x2 + 1, y1, borderColor); // Línea superior
        guiGraphics.fill(x1 - 1, y2, x2 + 1, y2 + 1, borderColor);  // Línea inferior
        guiGraphics.fill(x1 - 1, y1, x1, y2, borderColor);          // Línea izquierda
        guiGraphics.fill(x2, y1, x2 + 1, y2, borderColor);          // Línea derecha
    }

    public void updateColorComponents() {
        ColorR = (selectedColor >> 16) & 0xFF; // Rojo
        ColorG = (selectedColor >> 8) & 0xFF;  // Verde
        ColorB = selectedColor & 0xFF;         // Azul

        // También puedes convertir a decimal aquí si lo necesitas
        decimalColor = calcularColor((int) ColorR, (int) ColorG, (int) ColorB);
    }
    public static int calcularColor(int r, int g, int b) {
        // Convertir el número a decimal
        return (r << 16) + (g << 8) + b;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int ColorPickerX = (this.width / 2) - 110; int ColorPickerY = (this.height / 2) - 61;
        if (mouseX >= ColorPickerX && mouseX <= ColorPickerX + 100 && mouseY >= ColorPickerY && mouseY <= ColorPickerY + 100) {
            int x = (int) (mouseX - ColorPickerX);
            int y = (int) (mouseY - ColorPickerY);
            selectedColor = getColorFromPosition(x, y);
            updateColorComponents();
        }

        int SaturationX = (this.width / 2) - 5; int SaturationY = (this.height / 2) - 61;
        if (mouseX >= SaturationX && mouseX <= SaturationX + 10 && mouseY >= SaturationY && mouseY <= SaturationY + 100) {
            int i = (int) (mouseY - SaturationY);
            brightness = (float) i / 100f;
            selectedColor = getColorFromPosition((int)(mouseX - ColorPickerX), (int)(mouseY - ColorPickerY));
            updateColorComponents();
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void panoramas(GuiGraphics graphics, float partialtick){
        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, Minecraft.getInstance().player).ifPresent(cap -> {
            var race = cap.getIntValue("race");

            if(race == 0){
                this.customPanorama.render(partialtick, 1.0f);
            }else if(race == 1){
                this.panoramaSai.render(partialtick, 1.0f);
            }else if(race == 2){
                this.panoramaNam.render(partialtick, 1.0f);
            }else if(race == 3){
                this.panoramaBio.render(partialtick, 1.0f);
            }else if(race == 4){
                this.panoramaCold.render(partialtick, 1.0f);
            }else {
                this.panoramaBuu.render(partialtick, 1.0f);
            }
        });
    }
}
