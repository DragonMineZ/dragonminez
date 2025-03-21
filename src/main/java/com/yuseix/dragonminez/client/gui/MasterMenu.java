package com.yuseix.dragonminez.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.client.gui.buttons.TextButton;
import com.yuseix.dragonminez.network.C2S.MasterSkillsC2S;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import com.yuseix.dragonminez.stats.skills.DMZSkill;
import com.yuseix.dragonminez.utils.DMZClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.text.NumberFormat;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class MasterMenu extends Screen {
	private static final ResourceLocation menuTexture = new ResourceLocation(DragonMineZ.MOD_ID,
			"textures/gui/menulargo2.png");
	private static final ResourceLocation menuGoku = new ResourceLocation(DragonMineZ.MOD_ID,
			"textures/gui/menugoku.png");
	private static final ResourceLocation menuKaio = new ResourceLocation(DragonMineZ.MOD_ID,
			"textures/gui/menukaio.png");
	private static final ResourceLocation menuRoshi = new ResourceLocation(DragonMineZ.MOD_ID,
			"textures/gui/menuroshi.png");
	private int altoTexto, anchoTexto;
	private TextButton flyBoton, kiControlBoton, kiManipulationBoton, jumpBoton, meditationBoton, potUnlockBoton, kaioKenBoton;
	private String masterName;

	NumberFormat numFor = NumberFormat.getInstance(Locale.US);

	public MasterMenu(String masterName) {
		super(Component.empty());
		this.masterName = masterName;
	}

	@Override
	protected void init() {
		super.init();
	}

	@Override
	public void tick() {
		super.tick();
		botonesSkills();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		renderBackground(graphics);
		menuPanel(graphics);
		menuSkills(graphics);
		super.render(graphics, mouseX, mouseY, partialTicks);
	}

	private void menuSkills(GuiGraphics graphics) {
		String masterName = this.masterName;

		switch (masterName) {
			case "goku":
				altoTexto = (this.height / 2) - 60;
				anchoTexto = (this.width / 2) + 1;
				drawStringWithBorder(graphics, font, Component.translatable("master.name.goku").append(Component.translatable("master.name.skills")), anchoTexto, altoTexto, 0xF91E64);

				altoTexto = (this.height / 2) - 17;
				anchoTexto = (this.width / 2) - 70;
				drawStringWithBorder2(graphics, font, Component.translatable("dmz.skill.fly.name"), anchoTexto, altoTexto, 0xFFFFFF);
				drawStringWithBorder2(graphics, font, Component.translatable("dmz.skill.ki_control.name"), anchoTexto, altoTexto + 27, 0xFFFFFF);
				drawStringWithBorder2(graphics, font, Component.translatable("dmz.skill.ki_manipulation.name"), anchoTexto, altoTexto + 54, 0xFFFFFF);
				break;
			case "roshi":
				altoTexto = (this.height / 2) - 60;
				anchoTexto = (this.width / 2) + 1;
				drawStringWithBorder(graphics, font, Component.translatable("master.name.roshi").append(Component.translatable("master.name.skills")), anchoTexto, altoTexto, 0xF91E64);

				altoTexto = (this.height / 2) - 17;
				anchoTexto = (this.width / 2) - 70;
				drawStringWithBorder2(graphics, font, Component.translatable("dmz.skill.jump.name"), anchoTexto, altoTexto, 0xFFFFFF);
				drawStringWithBorder2(graphics, font, Component.translatable("dmz.skill.meditation.name"), anchoTexto, altoTexto + 25, 0xFFFFFF);
				break;
			case "kaio":
				altoTexto = (this.height / 2) - 60;
				anchoTexto = (this.width / 2) + 1;
				drawStringWithBorder(graphics, font, Component.translatable("master.name.kaio").append(Component.translatable("master.name.skills")), anchoTexto, altoTexto, 0xF91E64);

				altoTexto = (this.height / 2) - 17;
				anchoTexto = (this.width / 2) - 70;
				drawStringWithBorder2(graphics, font, Component.translatable("dmz.skill.potential_unlock.name"), anchoTexto, altoTexto, 0xFFFFFF);
				break;
		}
	}

	private void botonesSkills() {
		String masterName = this.masterName;
		switch (masterName) {
			case "goku":
				removeWidget(flyBoton);
				removeWidget(kiControlBoton);
				removeWidget(kiManipulationBoton);
				break;
			case "roshi":
				removeWidget(jumpBoton);
				removeWidget(meditationBoton);
				break;
			case "kaio":
				removeWidget(potUnlockBoton);
				removeWidget(kaioKenBoton);
				break;
		}
		altoTexto = (this.height / 2) - 13;
		anchoTexto = (this.width / 2) + 25;
		Player player = this.minecraft.player;

		int flyCost = DMZClientConfig.getFlyMaster();
		int kiControlCost = DMZClientConfig.getKiControlMaster();
		int kiManipulationCost = DMZClientConfig.getKiManipMaster();
		int jumpCost = DMZClientConfig.getJumpMaster();
		int meditationCost = DMZClientConfig.getMeditationMaster();
		int potUnlockCost = DMZClientConfig.getPotUnlockMaster();
		//int kaioKenCost = DMZGeneralConfig.KAIOKEN_TP_COST_MASTER.get();

		DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
			int tpActual = cap.getIntValue("tps");

			switch (masterName) {
				case "goku":
					if (cap.getDMZSkills().get("fly") == null || cap.getDMZSkills().get("fly").getLevel() == 0) {
						this.flyBoton = (TextButton) this.addRenderableWidget(new TextButton(anchoTexto, altoTexto - 10, Component.literal(formatNumber(flyCost) + " TPs"), wa -> {
							if (tpActual >= flyCost) {
								ModMessages.sendToServer(new MasterSkillsC2S("fly", new DMZSkill("dmz.skill.fly.name", "dmz.skill.fly.desc", 1, false)));
							} else
								player.sendSystemMessage(Component.translatable("master.name.notenoughpoints", numFor.format(flyCost)));
						}));
					}
					if (cap.getDMZSkills().get("ki_control") == null || cap.getDMZSkills().get("ki_control").getLevel() == 0) {
						this.kiControlBoton = (TextButton) this.addRenderableWidget(new TextButton(anchoTexto, altoTexto + 17, Component.literal(formatNumber(kiControlCost) + " TPs"), wa -> {
							if (tpActual >= kiControlCost) {
								ModMessages.sendToServer(new MasterSkillsC2S("ki_control", new DMZSkill("dmz.skill.ki_control.name", "dmz.skill.ki_control.desc", 1, true)));
							} else
								player.sendSystemMessage(Component.translatable("master.name.notenoughpoints", numFor.format(kiControlCost)));
						}));

					}
					if (cap.getDMZSkills().get("ki_manipulation") == null || cap.getDMZSkills().get("ki_manipulation").getLevel() == 0) {
						this.kiManipulationBoton = (TextButton) this.addRenderableWidget(new TextButton(anchoTexto, altoTexto + 44, Component.literal(formatNumber(kiManipulationCost) + " TPs"), wa -> {
							if (tpActual >= kiManipulationCost) {
								ModMessages.sendToServer(new MasterSkillsC2S("ki_manipulation", new DMZSkill("dmz.skill.ki_manipulation.name", "dmz.skill.ki_manipulation.desc", 1, false)));
							} else
								player.sendSystemMessage(Component.translatable("master.name.notenoughpoints", numFor.format(kiManipulationCost)));
						}));
					}
					break;
				case "roshi":
					if (cap.getDMZSkills().get("jump") == null || cap.getDMZSkills().get("jump").getLevel() == 0) {
						this.jumpBoton = (TextButton) this.addRenderableWidget(new TextButton(anchoTexto, altoTexto - 10, Component.literal(formatNumber(jumpCost) + " TPs"), wa -> {
							if (tpActual >= jumpCost) {
								ModMessages.sendToServer(new MasterSkillsC2S("jump", new DMZSkill("dmz.skill.jump.name", "dmz.skill.jump.desc", 1, false)));
							} else
								player.sendSystemMessage(Component.translatable("master.name.notenoughpoints", numFor.format(jumpCost)));
						}));
					}
					if (cap.getDMZSkills().get("meditation") == null || cap.getDMZSkills().get("meditation").getLevel() == 0) {
						this.meditationBoton = (TextButton) this.addRenderableWidget(new TextButton(anchoTexto, altoTexto + 17, Component.literal(formatNumber(meditationCost) + " TPs"), wa -> {
							if (tpActual >= meditationCost) {
								ModMessages.sendToServer(new MasterSkillsC2S("meditation", new DMZSkill("dmz.skill.meditation.name", "dmz.skill.meditation.desc", 1, true)));
							} else
								player.sendSystemMessage(Component.translatable("master.name.notenoughpoints", numFor.format(meditationCost)));
						}));
					}
					break;
				case "kaio":
					if (cap.getDMZSkills().get("potential_unlock") == null || cap.getDMZSkills().get("potential_unlock").getLevel() == 0) {
						this.potUnlockBoton = (TextButton) this.addRenderableWidget(new TextButton(anchoTexto, altoTexto - 10, Component.literal(formatNumber(potUnlockCost) + " TPs"), wa -> {
							if (tpActual >= potUnlockCost) {
								ModMessages.sendToServer(new MasterSkillsC2S("potential_unlock", new DMZSkill("dmz.skill.potential_unlock.name", "dmz.skill.potential_unlock.desc", 1, true)));
							} else
								player.sendSystemMessage(Component.translatable("master.name.notenoughpoints", numFor.format(potUnlockCost)));
						}));
					}
					break;
			}
		});
	}


	private void menuPanel(GuiGraphics graphics) {
		altoTexto = (this.height - 168) / 2;
		anchoTexto = (this.width - 250) / 2;
		ResourceLocation menutexture = switch (this.masterName) {
			case "goku" -> menuGoku;
			case "kaio" -> menuKaio;
			case "roshi" -> menuRoshi;
			default -> menuTexture;
		};

		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(menutexture, anchoTexto, altoTexto, 0, 0, 250, 168);
		RenderSystem.disableBlend();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	public static void drawStringWithBorder(GuiGraphics guiGraphics, Font font, Component texto, int x, int y, int ColorTexto, int ColorBorde) {
		// Calcular la posición centrada
		int textWidth = font.width(texto);
		int centeredX = x - (textWidth / 2);
		// Dibujar el texto con el borde
		guiGraphics.drawString(font, texto, centeredX + 1, y, ColorBorde, false);
		guiGraphics.drawString(font, texto, centeredX - 1, y, ColorBorde, false);
		guiGraphics.drawString(font, texto, centeredX, y + 1, ColorBorde, false);
		guiGraphics.drawString(font, texto, centeredX, y - 1, ColorBorde, false);
		guiGraphics.drawString(font, texto, centeredX, y, ColorTexto);
	}

	public static void drawStringWithBorder2(GuiGraphics guiGraphics, Font font, Component texto, int x, int y, int ColorTexto, int ColorBorde) {
		guiGraphics.drawString(font, texto, x + 1, y, ColorBorde, false);
		guiGraphics.drawString(font, texto, x - 1, y, ColorBorde, false);
		guiGraphics.drawString(font, texto, x, y + 1, ColorBorde, false);
		guiGraphics.drawString(font, texto, x, y - 1, ColorBorde, false);
		guiGraphics.drawString(font, texto, x, y, ColorTexto, false);
	}

	public static void drawStringWithBorder(GuiGraphics guiGraphics, Font font, Component texto, int x, int y, int ColorTexto) {
		drawStringWithBorder(guiGraphics, font, texto, x, y, ColorTexto, 0);
	}

	public static void drawStringWithBorder2(GuiGraphics guiGraphics, Font font, Component texto, int x, int y, int ColorTexto) {
		drawStringWithBorder2(guiGraphics, font, texto, x, y, ColorTexto, 0);
	}

	private static String formatNumber(double value) {
		if (value >= 1_000_000_000) {
			return String.format("%.2fb", value / 1_000_000_000);
		} else if (value >= 1_000_000) {
			return String.format("%.2fm", value / 1_000_000);
		} else if (value >= 1_000) {
			return String.format("%.1fk", value / 1_000);
		} else {
			return String.format("%.0f", value);
		}
	}
}
