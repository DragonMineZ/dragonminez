package com.yuseix.dragonminez.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuseix.dragonminez.client.gui.buttons.CustomButtons;
import com.yuseix.dragonminez.client.gui.buttons.DMZGuiButtons;
import com.yuseix.dragonminez.client.gui.buttons.TextButton;
import com.yuseix.dragonminez.common.Reference;
import com.yuseix.dragonminez.common.network.C2S.SuperFormsC2S;
import com.yuseix.dragonminez.common.network.C2S.ZPointsC2S;
import com.yuseix.dragonminez.common.network.ModMessages;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import com.yuseix.dragonminez.common.stats.forms.FormsData;
import com.yuseix.dragonminez.client.config.DMZClientConfig;
import com.yuseix.dragonminez.common.util.DMZDatos;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class TransfMenu extends Screen {
	private static final ResourceLocation menu = new ResourceLocation(Reference.MOD_ID,
			"textures/gui/menulargo2.png");
	private static final ResourceLocation menuinfo = new ResourceLocation(Reference.MOD_ID,
			"textures/gui/menulargomitad.png");

	private static boolean infoMenu;
	private static String groupId = "superform";
	private int altoTexto, anchoTexto;

	private List<AbstractWidget> groupButtons = new ArrayList<>();
	private List<DMZGuiButtons> botonesMenus = new ArrayList<>();

	DMZDatos dmzDatos = new DMZDatos();

	private DMZGuiButtons menuButton;
	private CustomButtons infoButton;
	private TextButton upgradeButton;

	public TransfMenu(boolean infoMenu) {
		super(Component.empty());
		this.infoMenu = infoMenu;
	}

	@Override
	protected void init() {
		super.init();
	}

	@Override
	public void tick() {
		super.tick();
		botonesMenus();
	}

	@Override
	public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTicks) {
		renderBackground(pGuiGraphics);
		menuPanel(pGuiGraphics);
		menuTransf(pGuiGraphics);
		botonesGroups(pGuiGraphics);

		super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTicks);
	}

	public void botonesGroups(GuiGraphics guiGraphics) {
		Player player = this.minecraft.player;

		groupButtons.forEach(this::removeWidget);
		groupButtons.clear();

		this.removeWidget(upgradeButton);
		this.removeWidget(menuButton);

		DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
			var tps = cap.getIntValue("tps");

			Map<String, FormsData> forms = cap.getAllDMZForms();
			int raza = cap.getIntValue("race");
			boolean buyableTP = false;
			if (DMZClientConfig.getBuyableTP() == 1) buyableTP = true;


			int startX = (this.width - 250) / 2 + 15;
			int startY = (this.height - 168) / 2 + 45;
			int offsetY = 13;

			for (Map.Entry<String, FormsData> entry : forms.entrySet()) {
				String formId = entry.getKey();
				FormsData form = entry.getValue();
				double mult = DMZClientConfig.getMultiplierZPoints();
				int formsCost = DMZClientConfig.getTransfTPCost();

				switch (formId) {
					case "super_form":
						if (this.infoMenu) {
							switch (raza) {
								case 0, 2, 3, 4, 5:
									if (groupId.equals("superform")) {
										int currentLevel = form.getLevel();
										int maxLevel = 6;
										switch (raza) {
											case 0 -> maxLevel = 4;
											case 2 -> maxLevel = 4;
											case 3 -> maxLevel = 4;
											case 4 -> maxLevel = 6;
											case 5 -> maxLevel = 6;
										}

										int nextLevel = currentLevel + 1;

										Map<Integer, Integer> levelCosts = Map.of(
												1, (int) (formsCost * mult),
												2, (int) (formsCost * mult * nextLevel),
												3, (int) (formsCost * mult * nextLevel),
												4, (int) (formsCost * mult * nextLevel),
												5, (int) (formsCost * mult * nextLevel),
												6, (int) (formsCost * mult * nextLevel),
												7, (int) (formsCost * mult * nextLevel),
												8, (int) (formsCost * mult * nextLevel),
												9, (int) (formsCost * mult * nextLevel),
												10, (int) (formsCost * mult * nextLevel)
										);

										if (buyableTP) {
											if (currentLevel < maxLevel) {
												int cost = levelCosts.getOrDefault(nextLevel, Integer.MAX_VALUE);

												if (tps >= cost) {
													upgradeButton = (TextButton) this.addRenderableWidget(new TextButton(startX + 195, startY + 85, Component.translatable("dmz.skills.upgrade", cost), wa -> {
														ModMessages.sendToServer(new SuperFormsC2S("super_form", nextLevel));
														ModMessages.sendToServer(new ZPointsC2S(1, cost));
														this.removeWidget(upgradeButton);
													}));
												} else {
													drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.upgrade", cost), startX + 210, startY + 85, 0xffc134);
												}
											}

										}
									}
									break;
								case 1:
									if (groupId.equals("oozarus") || groupId.equals("ssgrades") || groupId.equals("ssj")) {
										int currentLevel = form.getLevel();
										int maxLevel = 4;

										Map<Integer, Integer> levelCosts = Map.of(
												1, (int) (formsCost * mult),
												2, (int) (formsCost * mult * 2),
												3, (int) (formsCost * mult * 3),
												4, (int) (formsCost * mult * 4),
												5, (int) (formsCost * mult * 5),
												6, (int) (formsCost * mult * 6),
												7, (int) (formsCost * mult * 7),
												8, (int) (formsCost * mult * 8)
										);
										int nextLevel = currentLevel + 1;

										if (buyableTP) {
											if (currentLevel < maxLevel) {
												int cost = levelCosts.getOrDefault(nextLevel, Integer.MAX_VALUE);

												if (tps >= cost) {
													upgradeButton = (TextButton) this.addRenderableWidget(new TextButton(startX + 195, startY + 85, Component.translatable("dmz.skills.upgrade", cost), wa -> {
														ModMessages.sendToServer(new SuperFormsC2S("super_form", nextLevel));
														ModMessages.sendToServer(new ZPointsC2S(1, cost));
														this.removeWidget(upgradeButton);
													}));
												} else {
													drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.upgrade", cost), startX + 210, startY + 85, 0xffc134);
												}
											}

										}
									}
									break;
							}
						}
						break;
				}

				startY = (this.height - 168) / 2 + 31;

				switch (raza) {
					case 0, 2, 3, 4, 5:
						CustomButtons buttonSF = new CustomButtons("info", this.infoMenu ? startX + 205 - 72 : startX + 205, startY - 2, Component.empty(), btn -> {
							this.infoMenu = !this.infoMenu;
							this.groupId = "superform";
						});

						this.addRenderableWidget(buttonSF);
						groupButtons.add(buttonSF);
						break;
					case 1:
						if (buyableTP) {

						}
						if (form.getLevel() >= 0) {
							CustomButtons buttonOozaru = new CustomButtons("info", this.infoMenu ? startX + 205 - 72 : startX + 205, startY - 2, Component.empty(), btn -> {
								this.infoMenu = !this.infoMenu;
								this.groupId = "oozarus";
							});

							this.addRenderableWidget(buttonOozaru);
							groupButtons.add(buttonOozaru);
						}
						if (form.getLevel() >= 2) {
							CustomButtons buttonSSG = new CustomButtons("info", this.infoMenu ? startX + 205 - 72 : startX + 205, startY + offsetY - 2, Component.empty(), btn -> {
								this.infoMenu = !this.infoMenu;
								this.groupId = "ssgrades";
							});


							this.addRenderableWidget(buttonSSG);
							groupButtons.add(buttonSSG);
						}
						if (form.getLevel() >= 5) {
							CustomButtons buttonSSJ = new CustomButtons("info", this.infoMenu ? startX + 205 - 72 : startX + 205, startY + offsetY * 2 - 2, Component.empty(), btn -> {
								this.infoMenu = !this.infoMenu;
								this.groupId = "ssj";
							});

							this.addRenderableWidget(buttonSSJ);
							groupButtons.add(buttonSSJ);
						}
						break;
				}

				startY += offsetY;
			}
		});

	}

	public void menuPanel(GuiGraphics guiGraphics) {
		if (infoMenu) {
			altoTexto = (this.height - 168)/2;
			anchoTexto = ((this.width - 250)/2) - 72;
			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			guiGraphics.blit(menu, anchoTexto, altoTexto, 0, 0, 250, 168);

			anchoTexto = ((this.width - 250)/2) + 180;
			guiGraphics.blit(menuinfo, anchoTexto, altoTexto, 0, 0, 145, 168);

			int startX = ((this.width - 250) / 2 + 30) - 72;
			int startY = (this.height - 168) / 2 + 18;
			drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.level"),startX, startY, 0xffffff);
			startX = ((this.width - 250) / 2 + 100) - 72;
			drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.transf.form"), startX, startY, 0xffc134);
			startX = ((this.width - 250) / 2 + 180) - 72;
			drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.transf.group"), startX, startY, 0x20e0ff);

			menuGrupos(guiGraphics);
		} else {
			altoTexto = (this.height - 168)/2;
			anchoTexto = (this.width - 250)/2;
			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			guiGraphics.blit(menu, anchoTexto, altoTexto, 0, 0, 250, 168);

			int startX = (this.width - 250) / 2 + 30;
			int startY = (this.height - 168) / 2 + 18;
			drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.level"),startX, startY, 0xffffff);
			startX = (this.width - 250) / 2 + 100;
			drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.transf.form"), startX, startY, 0xffc134);
			startX = (this.width - 250) / 2 + 180;
			drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.transf.group"), startX, startY, 0x20e0ff);
		}
		RenderSystem.disableBlend();
	}

	private void menuTransf(GuiGraphics guiGraphics) {
		// Obtener las habilidades desde la capability del jugador
		Player player = this.minecraft.player;

		DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
			Map<String, FormsData> forms = cap.getAllDMZForms();

			int startX = (this.width - 250) / 2 + 15;
			int startY = (this.height - 168) / 2 + 30;
			int offsetY = 13;

			for (Map.Entry<String, FormsData> entry : forms.entrySet()) {
				int level = cap.getFormSkillLevel("super_form");
				switch (cap.getIntValue("race")) {
					case 0:
						switch (entry.getKey()) {
							case "super_form":
								// Nivel
								drawStringWithBorder(guiGraphics, this.font, Component.literal(String.valueOf(level)), this.infoMenu ? startX + 16 - 72 : startX + 16, startY, 0xffffff);
								// Skill
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.dmzforms.super_form.name"), this.infoMenu ? startX + 85 - 72: startX + 85, startY, 0xffc134);
								// Grupo
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.general.superform"), this.infoMenu ? startX + 165 - 72: startX + 165, startY, 0x20e0ff);
								break;
						}
						break;
					case 1:
						switch (entry.getKey()) {
							case "super_form":
								// Nivel
								drawStringWithBorder(guiGraphics, this.font, Component.literal(String.valueOf(level)), this.infoMenu ? startX + 16 - 72 : startX + 16, startY, 0xffffff);
								// Skill
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.dmzforms.super_form.name"), this.infoMenu ? startX + 85 - 72: startX + 85, startY, 0xffc134);
								// Grupo
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.saiyan.oozarus"), this.infoMenu ? startX + 165 - 72: startX + 165, startY, 0x20e0ff);
								if (level >= 2) {
									drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.saiyan.ssgrades"), this.infoMenu ? startX + 165 - 72: startX + 165, startY + offsetY, 0x20e0ff);
								}
								if (level >= 5) {
									drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.saiyan.ssj"), this.infoMenu ? startX + 165 - 72: startX + 165, startY + offsetY * 2, 0x20e0ff);
								}
								break;
						}
						break;
					case 2:
						switch (entry.getKey()) {
							case "super_form":
								// Nivel
								drawStringWithBorder(guiGraphics, this.font, Component.literal(String.valueOf(level)), this.infoMenu ? startX + 16 - 72 : startX + 16, startY, 0xffffff);
								// Skill
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.dmzforms.super_form.name"), this.infoMenu ? startX + 85 - 72: startX + 85, startY, 0xffc134);
								// Grupo
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.general.superform"), this.infoMenu ? startX + 165 - 72: startX + 165, startY, 0x20e0ff);
								break;
						}
						break;
					case 3:
						switch (entry.getKey()) {
							case "super_form":
								// Nivel
								drawStringWithBorder(guiGraphics, this.font, Component.literal(String.valueOf(level)), this.infoMenu ? startX + 16 - 72 : startX + 16, startY, 0xffffff);
								// Skill
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.dmzforms.super_form.name"), this.infoMenu ? startX + 85 - 72: startX + 85, startY, 0xffc134);
								// Grupo
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.bio.evolutionforms"), this.infoMenu ? startX + 165 - 72: startX + 165, startY, 0x20e0ff);
								break;
						}
						break;
					case 4:
						switch (entry.getKey()) {
							case "super_form":
								// Nivel
								drawStringWithBorder(guiGraphics, this.font, Component.literal(String.valueOf(level)), this.infoMenu ? startX + 16 - 72 : startX + 16, startY, 0xffffff);
								// Skill
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.dmzforms.super_form.name"), this.infoMenu ? startX + 85 - 72: startX + 85, startY, 0xffc134);
								// Grupo
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.colddemon.involutionforms"), this.infoMenu ? startX + 165 - 72: startX + 165, startY, 0x20e0ff);
								break;
						}
						break;
					case 5:
						switch (entry.getKey()) {
							case "super_form":
								// Nivel
								drawStringWithBorder(guiGraphics, this.font, Component.literal(String.valueOf(level)), this.infoMenu ? startX + 16 - 72 : startX + 16, startY, 0xffffff);
								// Skill
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.dmzforms.super_form.name"), this.infoMenu ? startX + 85 - 72: startX + 85, startY, 0xffc134);
								// Grupo
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.majin.majinforms"), this.infoMenu ? startX + 165 - 72: startX + 165, startY, 0x20e0ff);
								break;
						}
						break;

				}

			}
		});
	}

	private void menuGrupos(GuiGraphics guiGraphics) {
		Player player = this.minecraft.player;

		if (infoMenu) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
				int startY = (this.height - 168) / 2 + 18;
				int startX = (this.width - 250) / 2 + 160;
				int raza = cap.getIntValue("race");
				Map<String, FormsData> forms = cap.getAllDMZForms();

				for (Map.Entry<String, FormsData> entry : forms.entrySet()) {
					FormsData form = entry.getValue();
					int currentLevel = form.getLevel();
					int maxLevel = 6;
					switch (raza) {
						case 0 -> maxLevel = 4;
						case 2 -> maxLevel = 4;
						case 3 -> maxLevel = 4;
						case 4 -> maxLevel = 6;
						case 5 -> maxLevel = 6;
					}

					if (this.groupId.equals("superform")) {
						switch (raza) {
							case 0:
								double multHumanBuffed = dmzDatos.transfMultMenu(cap,"buffed");
								double multHumanFP = dmzDatos.transfMultMenu(cap,"full_power");
								double multHumanPU = dmzDatos.transfMultMenu(cap,"potential_unleashed");

								//Nombre de la habilidad
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.general.superform"), startX + 93, startY, 0x20e0ff);
								//Tipo y aca pongo lo de skill
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.type"), startX + 37, startY+ 13, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.transf"), startX + 68, startY+ 13, 0xffc134);
								//Aca pongo lo de nivel
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.level"), startX + 37, startY+24, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.literal(String.valueOf(currentLevel)), startX + 78, startY+24, 0xFFFFFF);
								// Lista de transformaciones
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.transflist"), startX + 93, startY+ 35, 0xffc134);
								//Acá van las transformaciones y sus mults
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.human.buffed").append(" | x").append(String.format("%.2f", multHumanBuffed)), startX + 37, startY+46, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.human.full_power").append(" | x").append(String.format("%.2f", multHumanFP)), startX + 37, startY+57, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.human.potential_unleashed").append(" | x").append(String.format("%.2f", multHumanPU)), startX + 37, startY+68, 0xFFFFFF);
								if (currentLevel >= maxLevel) {
									drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.maxlevel"), startX + 90, startY+116, 0xffc134);
								}
								break;
							case 2:
								double multNamekGiant = dmzDatos.transfMultMenu(cap,"giant");
								double multNamekFP = dmzDatos.transfMultMenu(cap,"full_power");
								double multNamekSuperNamek = dmzDatos.transfMultMenu(cap,"super_namek");

								//Nombre de la habilidad
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.general.superform"), startX + 93, startY, 0x20e0ff);
								//Tipo y aca pongo lo de skill
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.type"), startX + 37, startY+ 13, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.transf"), startX + 68, startY+ 13, 0xffc134);
								//Aca pongo lo de nivel
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.level"), startX + 37, startY+24, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.literal(String.valueOf(currentLevel)), startX + 78, startY+24, 0xFFFFFF);
								// Lista de transformaciones
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.transflist"), startX + 93, startY+ 35, 0xffc134);
								//Acá van las transformaciones y sus mults
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.namek.giant").append(" | x").append(String.format("%.2f", multNamekGiant)), startX + 37, startY+46, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.namek.full_power").append(" | x").append(String.format("%.2f", multNamekFP)), startX + 37, startY+57, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.namek.super_namek").append(" | x").append(String.format("%.2f", multNamekSuperNamek)), startX + 37, startY+68, 0xFFFFFF);
								if (currentLevel >= maxLevel) {
									drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.maxlevel"), startX + 90, startY+116, 0xffc134);
								}
								break;
							case 3:
								double multBioSemiPerfect = dmzDatos.transfMultMenu(cap,"semi_perfect");
								double multBioPerfect = dmzDatos.transfMultMenu(cap,"perfect");

								//Nombre de la habilidad
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.bio.evolutionforms"), startX + 93, startY, 0x20e0ff);
								//Tipo y aca pongo lo de skill
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.type"), startX + 37, startY+ 13, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.transf"), startX + 68, startY+ 13, 0xffc134);
								//Aca pongo lo de nivel
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.level"), startX + 37, startY+24, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.literal(String.valueOf(currentLevel)), startX + 78, startY+24, 0xFFFFFF);
								// Lista de transformaciones
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.transflist"), startX + 93, startY+ 35, 0xffc134);
								//Acá van las transformaciones y sus mults
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.bioandroid.semi_perfect").append(" | x").append(String.format("%.2f", multBioSemiPerfect)), startX + 37, startY+46, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.bioandroid.perfect").append(" | x").append(String.format("%.2f", multBioPerfect)), startX + 37, startY+57, 0xFFFFFF);
								if (currentLevel >= maxLevel) {
									drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.maxlevel"), startX + 90, startY+116, 0xffc134);
								}
								break;
							case 4:
								double multColdSecond = dmzDatos.transfMultMenu(cap,"second_form");
								double multColdThird = dmzDatos.transfMultMenu(cap,"third_form");
								double multColdFourth = dmzDatos.transfMultMenu(cap,"final_form");
								double multColdFullPower = dmzDatos.transfMultMenu(cap,"full_power");

								//Nombre de la habilidad
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.colddemon.involutionforms"), startX + 93, startY, 0x20e0ff);
								//Tipo y aca pongo lo de skill
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.type"), startX + 37, startY+ 13, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.transf"), startX + 68, startY+ 13, 0xffc134);
								//Aca pongo lo de nivel
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.level"), startX + 37, startY+24, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.literal(String.valueOf(currentLevel)), startX + 78, startY+24, 0xFFFFFF);
								// Lista de transformaciones
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.transflist"), startX + 93, startY+ 35, 0xffc134);
								//Acá van las transformaciones y sus mults
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.colddemon.second_form").append(" | x").append(String.format("%.2f", multColdSecond)), startX + 37, startY+46, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.colddemon.third_form").append(" | x").append(String.format("%.2f", multColdThird)), startX + 37, startY+57, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.colddemon.final_form").append(" | x").append(String.format("%.2f", multColdFourth)), startX + 37, startY+68, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.colddemon.full_power").append(" | x").append(String.format("%.2f", multColdFullPower)), startX + 37, startY+79, 0xFFFFFF);
								if (currentLevel >= maxLevel) {
									drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.maxlevel"), startX + 90, startY+116, 0xffc134);
								}
								break;
							case 5:
								double multMajinEvil = dmzDatos.transfMultMenu(cap,"evil");
								double multMajinKid = dmzDatos.transfMultMenu(cap,"kid");
								double multMajinSuper = dmzDatos.transfMultMenu(cap,"super");
								double multMajinUltra = dmzDatos.transfMultMenu(cap,"ultra");

								//Nombre de la habilidad
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.majin.majinforms"), startX + 93, startY, 0x20e0ff);
								//Tipo y aca pongo lo de skill
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.type"), startX + 37, startY+ 13, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.transf"), startX + 68, startY+ 13, 0xffc134);
								//Aca pongo lo de nivel
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.level"), startX + 37, startY+24, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.literal(String.valueOf(currentLevel)), startX + 78, startY+24, 0xFFFFFF);
								// Lista de transformaciones
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.transflist"), startX + 93, startY+ 35, 0xffc134);
								//Acá van las transformaciones y sus mults
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.majin.evil").append(" | x").append(String.format("%.2f", multMajinEvil)), startX + 37, startY+46, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.majin.kid").append(" | x").append(String.format("%.2f", multMajinKid)), startX + 37, startY+57, 0xFFFFFF);
								if (cap.getStringValue("gender").equals("male") || cap.getStringValue("gender").equals("male")) {
									drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.majin.male.super").append(" | x").append(String.format("%.2f", multMajinSuper)), startX + 37, startY+68, 0xFFFFFF);
									drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.majin.male.ultra").append(" | x").append(String.format("%.2f", multMajinUltra)), startX + 37, startY+79, 0xFFFFFF);
								} else {
									drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.majin.female.super").append(" | x").append(String.format("%.2f", multMajinSuper)), startX + 37, startY+68, 0xFFFFFF);
									drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.majin.female.ultra").append(" | x").append(String.format("%.2f", multMajinUltra)), startX + 37, startY+79, 0xFFFFFF);
								}
								if (currentLevel >= maxLevel) {
									drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.maxlevel"), startX + 90, startY+116, 0xffc134);
								}
								break;
						}
					} else if (this.groupId.equals("oozarus") || this.groupId.equals("ssgrades") || this.groupId.equals("ssj")) {
						double multSaiyanOozaru = dmzDatos.transfMultMenu(cap, "oozaru");
						double multSaiyanGoldenOozaru = dmzDatos.transfMultMenu(cap, "golden_oozaru");
						double multSaiyanSSJ = dmzDatos.transfMultMenu(cap, "ssj1");
						double multSaiyanSSG2 = dmzDatos.transfMultMenu(cap, "ssgrade2");
						double multSaiyanSSG3 = dmzDatos.transfMultMenu(cap, "ssgrade3");
						double multSaiyanMSSJ = dmzDatos.transfMultMenu(cap, "mssj");
						double multSaiyanSSJ2 = dmzDatos.transfMultMenu(cap, "ssj2");
						double multSaiyanSSJ3 = dmzDatos.transfMultMenu(cap, "ssj3");

						maxLevel = 8;

						//Tipo y aca pongo lo de skill
						drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.type"), startX + 37, startY+ 13, 0xFFFFFF);
						drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.transf"), startX + 68, startY+ 13, 0xffc134);
						//Aca pongo lo de nivel
						drawStringWithBorder2(guiGraphics, this.font, Component.translatable("dmz.skills.level"), startX + 37, startY+24, 0xFFFFFF);
						drawStringWithBorder2(guiGraphics, this.font, Component.literal(String.valueOf(currentLevel)), startX + 78, startY+24, 0xFFFFFF);
						// Lista de transformaciones
						drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.transflist"), startX + 93, startY+ 35, 0xffc134);

						switch (groupId) {
							case "oozarus":
								//Nombre de la habilidad
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.saiyan.oozarus"), startX + 93, startY, 0x20e0ff);
								//Acá van las transformaciones y sus mults
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.saiyan.oozaru").append(" | x").append(String.format("%.2f", multSaiyanOozaru)), startX + 37, startY+46, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.saiyan.goldenoozaru").append(" | x").append(String.format("%.2f", multSaiyanGoldenOozaru)), startX + 37, startY+57, 0xFFFFFF);
								break;
							case "ssgrades":
								//Nombre de la habilidad
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.saiyan.ssgrades"), startX + 93, startY, 0x20e0ff);
								//Acá van las transformaciones y sus mults
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.saiyan.ssj1").append(" | x").append(String.format("%.2f", multSaiyanSSJ)), startX + 37, startY+46, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.saiyan.ssgrade2").append(" | x").append(String.format("%.2f", multSaiyanSSG2)), startX + 37, startY+57, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.saiyan.ssgrade3").append(" | x").append(String.format("%.2f", multSaiyanSSG3)), startX + 37, startY+68, 0xFFFFFF);
								break;
							case "ssj":
								//Nombre de la habilidad
								drawStringWithBorder(guiGraphics, this.font, Component.translatable("groupforms.dmz.saiyan.ssj"), startX + 93, startY, 0x20e0ff);
								//Acá van las transformaciones y sus mults
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.saiyan.ssjfp").append(" | x").append(String.format("%.2f", multSaiyanMSSJ)), startX + 37, startY+46, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.saiyan.ssj2").append(" | x").append(String.format("%.2f", multSaiyanSSJ2)), startX + 37, startY+57, 0xFFFFFF);
								drawStringWithBorder2(guiGraphics, this.font, Component.translatable("forms.dmz.saiyan.ssj3").append(" | x").append(String.format("%.2f", multSaiyanSSJ3)), startX + 37, startY+68, 0xFFFFFF);
								break;
						}

						if (currentLevel >= maxLevel) {
							drawStringWithBorder(guiGraphics, this.font, Component.translatable("dmz.skills.maxlevel"), startX + 90, startY+116, 0xffc134);
						}
					}
				}
			});
		}
	}

	public void botonesMenus() {
		this.removeWidget(menuButton);

		for (DMZGuiButtons boton : botonesMenus) {
			this.removeWidget(boton);
		}
		botonesMenus.clear();

		altoTexto = (this.height + 168) / 2;
		anchoTexto = this.infoMenu ? (this.width/2) - 72 : this.width/2;

		if (this.minecraft.level.isClientSide) {
			Player player = this.minecraft.player;
			botonesMenus.add(this.addRenderableWidget(new DMZGuiButtons(anchoTexto - 85, altoTexto, "stats", Component.empty(), wa -> {
				DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
					if (playerstats.getBoolean("compactmenu")) {
						this.minecraft.setScreen(new AttributesMenu2());
					} else {
						this.minecraft.setScreen(new AttributesMenu());
					}
				});
			})));

			botonesMenus.add(this.addRenderableWidget(new DMZGuiButtons(anchoTexto - 55, altoTexto, "skills", Component.empty(), wa -> {
				this.minecraft.setScreen(new SkillMenu(false));
			})));

			botonesMenus.add(this.addRenderableWidget(new DMZGuiButtons(anchoTexto - 25, altoTexto, "transf", Component.empty(), wa -> {
				// Es este menú, no hacer nada
			})));

			botonesMenus.add(this.addRenderableWidget(new DMZGuiButtons(anchoTexto + 5, altoTexto, "storyline", Component.empty(), wa -> {
				this.minecraft.setScreen(new StorylineMenu(false));
			})));

			botonesMenus.add(this.addRenderableWidget(new DMZGuiButtons(anchoTexto + 35, altoTexto, "kitech", Component.empty(), wa -> {
				// Agregar menú de Ki Techniques
				// this.minecraft.setScreen(new KiTechMenu());
			})));

			botonesMenus.add(this.addRenderableWidget(new DMZGuiButtons(anchoTexto + 65, altoTexto, "settings", Component.empty(), wa -> {
				this.minecraft.setScreen(new ConfigMenu());
			})));
		}
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
}
