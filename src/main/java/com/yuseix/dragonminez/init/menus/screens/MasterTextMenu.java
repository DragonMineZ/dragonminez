package com.yuseix.dragonminez.init.menus.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.client.gui.buttons.DMZButton;
import com.yuseix.dragonminez.client.gui.buttons.DMZRightButton;
import com.yuseix.dragonminez.client.gui.buttons.GlowButton;
import com.yuseix.dragonminez.client.gui.MasterMenu;
import com.yuseix.dragonminez.network.C2S.ConfigValuesC2S;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Locale;

public class MasterTextMenu extends Screen {

	private static final ResourceLocation textoCuadro = new ResourceLocation(DragonMineZ.MOD_ID,
			"textures/gui/texto.png");
	private final String mastername;

	private GlowButton master, saludo;
	private DMZButton AcceptButton, DeclineButton;
	private DMZRightButton rightButton, leftButton;

	private String PageOption = "";
	private int PageButtons;

	public MasterTextMenu(String mastername) {
		super(Component.literal("masterwa"));
		this.mastername = mastername;
	}

	@Override
	protected void init() {
		super.init();
	}

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

		String mastername = this.mastername;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, textoCuadro);


		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

		buffer.vertex(centerX - 140, centerY + 250, 0.0D).uv(0.0F, 1.0F).endVertex();
		buffer.vertex(centerX + 140, centerY + 250, 0.0D).uv(1.0F, 1.0F).endVertex();
		buffer.vertex(centerX + 140, centerY - 90, 0.0D).uv(1.0F, 0.0F).endVertex();
		buffer.vertex(centerX - 140, centerY - 90, 0.0D).uv(0.0F, 0.0F).endVertex();
		Tesselator.getInstance().end();

		RenderSystem.disableBlend();


		//NOMBRE DE LA ENTIDAD
		pGuiGraphics.drawString(font, Component.literal(mastername).withStyle(ChatFormatting.BOLD), centerX - 120, centerY - 88, 0xFFFFFF);

		String textoInicial = "", textoSaludo = "";
		switch (mastername.toLowerCase(Locale.ROOT)) {
			case "goku" -> {
				textoInicial = "lines.master_goku.menu";
				textoSaludo = "lines.master_goku.dialogue";
			}
			case "kaio" -> {
				textoInicial = "lines.master_kaio.menu";
				textoSaludo = "lines.master_kaio.dialogue";
			}
			case "roshi" -> {
				textoInicial = "lines.master_roshi.menu";
				textoSaludo = "lines.master_roshi.dialogue";
			}
		}

		//TEXTO QUE DIRA LA ENTIDAD
		if (PageOption.equals("")) {
			List<FormattedCharSequence> lines = font.split(Component.translatable(textoInicial), 250);
			for (int i = 0; i < lines.size(); i++) {
				pGuiGraphics.drawString(font, lines.get(i), (centerX - 120), (centerY - 73) + i * font.lineHeight, 0xFFFFFF);
			}
		} else if (PageOption.equals("saludo")) {
			List<FormattedCharSequence> lines = font.split(Component.translatable(textoSaludo), 250);
			for (int i = 0; i < lines.size(); i++) {
				pGuiGraphics.drawString(font, lines.get(i), (centerX - 120), (centerY - 73) + i * font.lineHeight, 0xFFFFFF);
			}
		}

		//Botones
		super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void removerBotones() {
		removeWidget(this.master);
		removeWidget(this.saludo);
		removeWidget(this.AcceptButton);
		removeWidget(this.DeclineButton);
		removeWidget(this.rightButton);
		removeWidget(this.leftButton);
	}

	private void paginaBotones() {
		String mastername = this.mastername;
		String botonMaestro = "", botonSaludo = "";
		Screen screen = null;
		switch (mastername.toLowerCase(Locale.ROOT)) {
			case "goku" -> {
				botonMaestro = "lines.master_goku.menubutton";
				botonSaludo = "lines.master_goku.dialoguebutton";
				screen = new MasterMenu("goku");
			}
			case "kaio" -> {
				botonMaestro = "lines.master_kaio.menubutton";
				botonSaludo = "lines.master_kaio.dialoguebutton";
				screen = new MasterMenu("kaio");
			}
			case "roshi" -> {
				botonMaestro = "lines.master_roshi.menubutton";
				botonSaludo = "lines.master_roshi.dialoguebutton";
				screen = new MasterMenu("roshi");
			}
		}

		if (PageButtons == 0) {
			removerBotones();

			String finalBotonMaestro = botonMaestro;
			String finalBotonSaludo = botonSaludo;
			Screen finalScreen = screen;

			if (this.minecraft.level.isClientSide()) {
				DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, minecraft.player).ifPresent(cap -> {
					ModMessages.sendToServer(new ConfigValuesC2S());
					if (cap.getBoolean("dmzuser")) {
						this.master = (GlowButton) this.addRenderableWidget(new GlowButton((this.width / 2) - 105, (this.height - 23), Component.translatable(finalBotonMaestro), wa -> {
							this.minecraft.setScreen(finalScreen);
						}));
					}

					this.saludo = (GlowButton) this.addRenderableWidget(new GlowButton((this.width / 2) + 5, (this.height - 23), Component.translatable(finalBotonSaludo), wa -> {
						PageOption = "saludo";
					}));
				});

			}
		}
	}

	public void PaginaOpciones() {
		if (this.minecraft.level.isClientSide()) {

			if (PageOption.equals("saludo")) {
				this.DeclineButton = (DMZButton) this.addRenderableWidget(new DMZButton((this.width / 2) + 60, (this.height - 47), Component.translatable("lines.menu.back"), wa -> {
					PageOption = "";
				}));
			}
		}
	}
}