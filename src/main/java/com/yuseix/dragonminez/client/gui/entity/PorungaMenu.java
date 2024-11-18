package com.yuseix.dragonminez.client.gui.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.client.gui.buttons.DMZButton;
import com.yuseix.dragonminez.client.gui.buttons.DMZRightButton;
import com.yuseix.dragonminez.client.gui.buttons.GlowButton;
import com.yuseix.dragonminez.init.MainEntity;
import com.yuseix.dragonminez.init.entity.custom.PorungaEntity;
import com.yuseix.dragonminez.network.C2S.PorungaC2S;
import com.yuseix.dragonminez.network.ModMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.List;

public class PorungaMenu extends Screen {

    private static final ResourceLocation textoCuadro = new ResourceLocation(DragonMineZ.MOD_ID,
            "textures/gui/texto.png");

    private GlowButton capSTR, capDEF, capCON, capENE, capKIPW, senzu, radar;
    private DMZButton AcceptButton,DeclineButton;
    private DMZRightButton rightButton, leftButton;

    private String PageOption = "";
    private int PageButtons;
    public int wishesCount = 0;

    public PorungaMenu(int wishesCount) {
        super(Component.literal("porungawa"));
        this.wishesCount = wishesCount;
    }

    @Override
    protected void init() {super.init();}

    private void menuInicio(){
        PageOption = "";
        PageButtons = 0;
    }

    private void acceptButtonPressed() {
        if (wishesCount < 2) {
            wishesCount++;
            menuInicio();
        } else {
            wishesCount++;
            closeMenu();
        }
    }

    private void rejectButtonPressed() {menuInicio();}

    private void closeMenu() {this.minecraft.setScreen(null);}

    @Override
    public void tick() {
        super.tick();
        paginaBotones();
        PaginaOpciones();
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        int centerX = (this.width / 2);
        int centerY = (this.height);

        LivingEntity porungaEntity = new PorungaEntity(MainEntity.PORUNGA.get(), this.minecraft.level);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, textoCuadro);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(centerX-140, centerY + 250, 0.0D).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(centerX+140, centerY + 250, 0.0D).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(centerX+140, centerY-90, 0.0D).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(centerX-140, centerY-90, 0.0D).uv(0.0F, 0.0F).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.disableBlend();

        //NOMBRE DE LA ENTIDAD
        pGuiGraphics.drawString(font, Component.literal(porungaEntity.getName().getString()).withStyle(ChatFormatting.BOLD), centerX-120, centerY-88, 0xFFFFFF);

        // TEXTO QUE DIRÁ LA ENTIDAD
        switch (PageOption) {
            case "" -> {
                switch (wishesCount) {
                    case 0 -> {
                        List<FormattedCharSequence> lines = font.split(Component.translatable("lines.porunga.menu"), 250);
                        for (int i = 0; i < lines.size(); i++) {
                            pGuiGraphics.drawString(font, lines.get(i), (centerX - 120), (centerY - 73) + i * font.lineHeight, 0xFFFFFF);
                        }
                    }
                    case 1 -> {
                        List<FormattedCharSequence> lines = font.split(Component.translatable("lines.porunga.menu_sec"), 250);
                        for (int i = 0; i < lines.size(); i++) {
                            pGuiGraphics.drawString(font, lines.get(i), (centerX - 120), (centerY - 73) + i * font.lineHeight, 0xFFFFFF);
                        }
                    }
                    case 2 -> {
                        List<FormattedCharSequence> lines = font.split(Component.translatable("lines.porunga.menu_trd"), 250);
                        for (int i = 0; i < lines.size(); i++) {
                            pGuiGraphics.drawString(font, lines.get(i), (centerX - 120), (centerY - 73) + i * font.lineHeight, 0xFFFFFF);
                        }
                    }
                }
            }
            case "capstr" -> {
                List<FormattedCharSequence> lines = font.split(Component.translatable("lines.shenron.capstr"), 250);
                for (int i = 0; i < lines.size(); i++) {
                    pGuiGraphics.drawString(font, lines.get(i), (centerX - 120), (centerY - 73) + i * font.lineHeight, 0xFFFFFF);
                }
            }
            case "capdef" -> {
                List<FormattedCharSequence> lines = font.split(Component.translatable("lines.shenron.capdef"), 250);
                for (int i = 0; i < lines.size(); i++) {
                    pGuiGraphics.drawString(font, lines.get(i), (centerX - 120), (centerY - 73) + i * font.lineHeight, 0xFFFFFF);
                }
            }
            case "capcon" -> {
                List<FormattedCharSequence> lines = font.split(Component.translatable("lines.shenron.capcon"), 250);
                for (int i = 0; i < lines.size(); i++) {
                    pGuiGraphics.drawString(font, lines.get(i), (centerX - 120), (centerY - 73) + i * font.lineHeight, 0xFFFFFF);
                }
            }
            case "capene" -> {
                List<FormattedCharSequence> lines = font.split(Component.translatable("lines.shenron.capene"), 250);
                for (int i = 0; i < lines.size(); i++) {
                    pGuiGraphics.drawString(font, lines.get(i), (centerX - 120), (centerY - 73) + i * font.lineHeight, 0xFFFFFF);
                }
            }
            case "capkipw" -> {
                List<FormattedCharSequence> lines = font.split(Component.translatable("lines.shenron.capkipw"), 250);
                for (int i = 0; i < lines.size(); i++) {
                    pGuiGraphics.drawString(font, lines.get(i), (centerX - 120), (centerY - 73) + i * font.lineHeight, 0xFFFFFF);
                }
            }
            case "senzu" -> {
                List<FormattedCharSequence> lines = font.split(Component.translatable("lines.porunga.senzu"), 250);
                for (int i = 0; i < lines.size(); i++) {
                    pGuiGraphics.drawString(font, lines.get(i), (centerX - 120), (centerY - 73) + i * font.lineHeight, 0xFFFFFF);
                }
            }
            case "gete" -> {
                List<FormattedCharSequence> lines = font.split(Component.translatable("lines.porunga.gete"), 250);
                for (int i = 0; i < lines.size(); i++) {
                    pGuiGraphics.drawString(font, lines.get(i), (centerX - 120), (centerY - 73) + i * font.lineHeight, 0xFFFFFF);
                }
            }
        }

        //Botones
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public boolean isPauseScreen() {return false;}

    private void removerBotones(){
        removeWidget(this.capSTR);
        removeWidget(this.capDEF);
        removeWidget(this.capCON);
        removeWidget(this.capENE);
        removeWidget(this.capKIPW);
        removeWidget(this.senzu);
        removeWidget(this.radar);
        removeWidget(this.AcceptButton);
        removeWidget(this.DeclineButton);
        removeWidget(this.rightButton);
        removeWidget(this.leftButton);
    }
    private void paginaBotones(){
        switch (PageButtons) {
            case 0 -> {
                removerBotones();
                // Capsula STR
                this.capSTR = (GlowButton) this.addRenderableWidget(new GlowButton((this.width / 2) - 105, (this.height - 23),
                        Component.translatable("lines.shenron.wish.capstr"), wa -> {
                    PageOption = "capstr";
                }));
                // Capsula DEX
                this.capDEF = (GlowButton) this.addRenderableWidget(new GlowButton((this.width / 2) + 5, (this.height - 23),
                        Component.translatable("lines.shenron.wish.capdef"), wa -> {
                    PageOption = "capdef";
                }));

                this.rightButton = (DMZRightButton) this.addRenderableWidget(new DMZRightButton("right", (this.width / 2) + 120, (this.height - 22),
                        Component.empty(), wa -> {
                    PageButtons = 1;
                }));
            }
            case 1 -> {
                removerBotones();
                this.leftButton = (DMZRightButton) this.addRenderableWidget(new DMZRightButton("left", (this.width / 2) - 120, (this.height - 22),
                        Component.empty(), wa -> {
                    PageButtons = 0;
                }));

                // Capsula CON
                this.capCON = (GlowButton) this.addRenderableWidget(new GlowButton((this.width / 2) - 105, (this.height - 23),
                        Component.translatable("lines.shenron.wish.capcon"), wa -> {
                    PageOption = "capcon";
                }));
                // Capsula ENE
                this.capENE = (GlowButton) this.addRenderableWidget(new GlowButton((this.width / 2) + 5, (this.height - 23),
                        Component.translatable("lines.shenron.wish.capene"), wa -> {
                    PageOption = "capene";
                }));

                this.rightButton = (DMZRightButton) this.addRenderableWidget(new DMZRightButton("right", (this.width / 2) + 120, (this.height - 22),
                        Component.empty(), wa -> {
                    PageButtons = 2;
                }));
            }
            case 2 -> {
                removerBotones();
                this.leftButton = (DMZRightButton) this.addRenderableWidget(new DMZRightButton("left", (this.width / 2) - 120, (this.height - 22),
                        Component.empty(), wa -> {
                    PageButtons = 1;
                }));

                // Capsula KIPW
                this.capKIPW = (GlowButton) this.addRenderableWidget(new GlowButton((this.width / 2) - 105, (this.height - 23),
                        Component.translatable("lines.shenron.wish.capkipw"), wa -> {
                    PageOption = "capkipw";
                }));

                // Senzus
                this.senzu = (GlowButton) this.addRenderableWidget(new GlowButton((this.width / 2) + 5, (this.height - 23),
                        Component.translatable("lines.shenron.wish.senzu"), wa -> {
                    PageOption = "senzu";
                }));
                this.rightButton = (DMZRightButton) this.addRenderableWidget(new DMZRightButton("right", (this.width / 2) + 120, (this.height - 22),
                        Component.empty(), wa -> {
                    PageButtons = 3;
                }));
            }
            case 3 -> {
                removerBotones();
                this.leftButton = (DMZRightButton) this.addRenderableWidget(new DMZRightButton("left", (this.width / 2) - 120, (this.height - 22),
                        Component.empty(), wa -> {
                    PageButtons = 2;
                }));
                // Radar
                this.radar = (GlowButton) this.addRenderableWidget(new GlowButton((this.width / 2) - 105, (this.height - 23),
                        Component.translatable("lines.porunga.wish.gete"), wa -> {
                    PageOption = "gete";
                }));
            }
        }
    }

    public void PaginaOpciones(){
        switch (PageOption){
            case "capstr":
                //Aceptar
                this.AcceptButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)-5, (this.height-47),
                        Component.translatable("lines.menu.accept"), wa -> {

                    switch (wishesCount) {
                        case 0 -> {
                            ModMessages.sendToServer(new PorungaC2S(1, 1)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 1 -> {
                            ModMessages.sendToServer(new PorungaC2S(1, 2)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 2 -> {
                            ModMessages.sendToServer(new PorungaC2S(1, 3)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                    }

                }));
                //Rechazar
                this.DeclineButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)+60, (this.height - 47),
                        Component.translatable("lines.menu.decline"), wa -> {
                    rejectButtonPressed();
                }));
                break;

            case "capdef":
                //Aceptar
                this.AcceptButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)-5, (this.height-47),
                        Component.translatable("lines.menu.accept"), wa -> {

                    switch (wishesCount) {
                        case 0 -> {
                            ModMessages.sendToServer(new PorungaC2S(2, 1)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 1 -> {
                            ModMessages.sendToServer(new PorungaC2S(2, 2)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 2 -> {
                            ModMessages.sendToServer(new PorungaC2S(2, 3)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                    }

                }));
                //Rechazar
                this.DeclineButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)+60, (this.height - 47),
                        Component.translatable("lines.menu.decline"), wa -> {
                    rejectButtonPressed();
                }));
                break;

            case "capcon":
                //Aceptar
                this.AcceptButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)-5, (this.height-47),
                        Component.translatable("lines.menu.accept"), wa -> {

                    switch (wishesCount) {
                        case 0 -> {
                            ModMessages.sendToServer(new PorungaC2S(3, 1)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 1 -> {
                            ModMessages.sendToServer(new PorungaC2S(3, 2)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 2 -> {
                            ModMessages.sendToServer(new PorungaC2S(3, 3)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                    }

                }));
                //Rechazar
                this.DeclineButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)+60, (this.height - 47),
                        Component.translatable("lines.menu.decline"), wa -> {
                    rejectButtonPressed();
                }));
                break;

            case "capene":
                //Aceptar
                this.AcceptButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)-5, (this.height-47),
                        Component.translatable("lines.menu.accept"), wa -> {

                    switch (wishesCount) {
                        case 0 -> {
                            ModMessages.sendToServer(new PorungaC2S(4, 1)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 1 -> {
                            ModMessages.sendToServer(new PorungaC2S(4, 2)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 2 -> {
                            ModMessages.sendToServer(new PorungaC2S(4, 3)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                    }

                }));
                //Rechazar
                this.DeclineButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)+60, (this.height - 47),
                        Component.translatable("lines.menu.decline"), wa -> {
                    rejectButtonPressed();
                }));
                break;

            case "capkipw":
                //Aceptar
                this.AcceptButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)-5, (this.height-47),
                        Component.translatable("lines.menu.accept"), wa -> {

                    switch (wishesCount) {
                        case 0 -> {
                            ModMessages.sendToServer(new PorungaC2S(5, 1)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 1 -> {
                            ModMessages.sendToServer(new PorungaC2S(5, 2)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 2 -> {
                            ModMessages.sendToServer(new PorungaC2S(5, 3)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                    }

                }));
                //Rechazar
                this.DeclineButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)+60, (this.height - 47),
                        Component.translatable("lines.menu.decline"), wa -> {
                    rejectButtonPressed();
                }));
                break;

            case "senzu":
                //Aceptar
                this.AcceptButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)-5, (this.height-47),
                        Component.translatable("lines.menu.accept"), wa -> {

                    switch (wishesCount) {
                        case 0 -> {
                            ModMessages.sendToServer(new PorungaC2S(6, 1)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 1 -> {
                            ModMessages.sendToServer(new PorungaC2S(6, 2)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 2 -> {
                            ModMessages.sendToServer(new PorungaC2S(6, 3)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                    }

                }));
                //Rechazar
                this.DeclineButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)+60, (this.height-47),
                        Component.translatable("lines.menu.decline"), wa -> {
                    rejectButtonPressed();
                }));
                break;

            case "gete":
                //Aceptar
                this.AcceptButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)-5, (this.height-47),
                        Component.translatable("lines.menu.accept"), wa -> {

                    switch (wishesCount) {
                        case 0 -> {
                            ModMessages.sendToServer(new PorungaC2S(7, 1)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 1 -> {
                            ModMessages.sendToServer(new PorungaC2S(7, 2)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                        case 2 -> {
                            ModMessages.sendToServer(new PorungaC2S(7, 3)); //Recibir capsula STR
                            acceptButtonPressed();
                        }
                    }

                }));
                //Rechazar
                this.DeclineButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width/2)+60, (this.height-47),
                        Component.translatable("lines.menu.decline"), wa -> {
                    rejectButtonPressed();
                }));

            default:
                break;
        }
    }
}