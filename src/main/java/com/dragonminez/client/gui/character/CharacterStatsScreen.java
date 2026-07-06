package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.SwitchButton;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dragonminez.client.render.shader.ClientGravityState;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.init.MainEnchants;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.IncreaseStatC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.SecondaryStatEffects;
import com.dragonminez.common.stats.extras.DynamicGrowthMath;
import com.dragonminez.common.stats.extras.DynamicGrowthStat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class CharacterStatsScreen extends BaseMenuScreen {

	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menubig.png");
	private static final ResourceLocation MENU_SMALL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menusmall.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");
	private int tpMultiplier = 1;

	private StatsData statsData;
	private int tickCount = 0;
	private final NumberFormat numberFormatter = NumberFormat.getInstance(Locale.US);
	private final DecimalFormat oneDecimalFormatter;
	private final DecimalFormat twoDecimalFormatter;
	private final DecimalFormat scientificFormatter;
	private final DecimalFormat fullTpsFormatter;
	private final DecimalFormat compactBpFormatter;
	private boolean useHexagonView = false;

	private CustomTextureButton strButton;
	private CustomTextureButton skpButton;
	private CustomTextureButton resButton;
	private CustomTextureButton vitButton;
	private CustomTextureButton pwrButton;
	private CustomTextureButton eneButton;
	private CustomTextureButton multiplierButton;
	private SwitchButton viewSwitchButton;

	public CharacterStatsScreen() {
		super(Component.translatable("gui.dragonminez.character_stats.title"));
		DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
		this.oneDecimalFormatter = new DecimalFormat("#,##0.#", symbols);
		this.twoDecimalFormatter = new DecimalFormat("#,##0.00", symbols);
		this.scientificFormatter = new DecimalFormat("0.###E0", symbols);
		this.fullTpsFormatter = new DecimalFormat("#,##0.######", symbols);
		this.compactBpFormatter = new DecimalFormat("0.##", symbols);
	}

	@Override
	protected void init() {
		super.init();

		useHexagonView = ConfigManager.getUserConfig().getHexagonStatsDisplay();
		this.tpMultiplier = 1;

		updateStatsData();
		initStatButtons();
		initViewSwitchButton();
	}

	@Override
	public void tick() {
		super.tick();
		tickCount++;

		if (tickCount >= 10) {
			tickCount = 0;
			updateStatsData();
			refreshStatButtons();
		}
	}

	private void updateStatsData() {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> this.statsData = data);
		}
	}

	@Override
	public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (isNotAnimating()) this.renderBackground(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);
		applyZoom(graphics, partialTick);
		int leftOffset = getLeftPanelSwitchOffset(partialTick);
		int rightOffset = getRightPanelSwitchOffset(partialTick);
		int topOffset = getTopPanelSwitchOffset(partialTick);
		updatePanelWidgetOffsets(leftOffset, rightOffset);

		renderPlayerModel(graphics, getUiWidth() / 2 + 5, getUiHeight() / 2 + 70, 75, uiMouseX, uiMouseY);
		renderMenuPanels(graphics, leftOffset, rightOffset, topOffset);
		renderPlayerInfo(graphics, uiMouseX, uiMouseY - topOffset, topOffset);

		graphics.pose().pushPose();
		graphics.pose().translate(leftOffset, 0, 0);
		renderStatsInfo(graphics, uiMouseX - leftOffset, uiMouseY);
		graphics.pose().popPose();

		graphics.pose().pushPose();
		graphics.pose().translate(rightOffset, 0, 0);
		renderStatisticsInfo(graphics, uiMouseX - rightOffset, uiMouseY);
		graphics.pose().popPose();

		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	private void initStatButtons() {
		if (statsData == null) return;

		int centerY = getUiHeight() / 2;
		int buttonX = 27;
		int startY = centerY - 15;

		int maxStats = ConfigManager.getServerConfig().getGameplay().getMaxValue();
		boolean maxByLevel = ConfigManager.getServerConfig().getGameplay().getMaxLevelValueInsteadOfStats();
		float availableTPs = statsData.getResources().getTrainingPoints();
		int pendingAP = statsData.getPendingAttributePoints();
		int freeCount = Math.min(pendingAP, tpMultiplier);
		int tpCost = statsData.calculateRecursiveCost(tpMultiplier, maxStats) - statsData.calculateRecursiveCost(freeCount, maxStats);
		int remainingTotal = statsData.getRemainingAssignableStats();

		boolean hasAP = pendingAP >= 1;
		boolean hasEnoughTPs = availableTPs >= tpCost;
		boolean canGrowAnyStat = (hasAP || hasEnoughTPs) && (!maxByLevel || remainingTotal > 0);

		multiplierButton = new CustomTextureButton.Builder()
				.position(buttonX, startY + 86)
				.size(14, 11)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 0, 0, 10)
				.textureSize(10, 10)
				.onPress(button -> {
					tpMultiplier = switch (tpMultiplier) {
						case 1 -> 10;
						case 10 -> 100;
						case 100 -> 1000;
						case 1000 -> 1;
						default -> 1;
					};
					refreshStatButtons();
				})
				.build();
		this.addRenderableWidget(multiplierButton);

		if (canGrowAnyStat && statsData.getMaxAllowedIncreaseForStat("STR", 1) > 0) {
			strButton = createStatButton(buttonX, startY + 11, "STR");
			this.addRenderableWidget(strButton);
		}

		if (canGrowAnyStat && statsData.getMaxAllowedIncreaseForStat("SKP", 1) > 0) {
			skpButton = createStatButton(buttonX, startY + 23, "SKP");
			this.addRenderableWidget(skpButton);
		}

		if (canGrowAnyStat && statsData.getMaxAllowedIncreaseForStat("RES", 1) > 0) {
			resButton = createStatButton(buttonX, startY + 35, "RES");
			this.addRenderableWidget(resButton);
		}

		if (canGrowAnyStat && statsData.getMaxAllowedIncreaseForStat("VIT", 1) > 0) {
			vitButton = createStatButton(buttonX, startY + 47, "VIT");
			this.addRenderableWidget(vitButton);
		}

		if (canGrowAnyStat && statsData.getMaxAllowedIncreaseForStat("PWR", 1) > 0) {
			pwrButton = createStatButton(buttonX, startY + 59, "PWR");
			this.addRenderableWidget(pwrButton);
		}

		if (canGrowAnyStat && statsData.getMaxAllowedIncreaseForStat("ENE", 1) > 0) {
			eneButton = createStatButton(buttonX, startY + 71, "ENE");
			this.addRenderableWidget(eneButton);
		}
	}

	private CustomTextureButton createStatButton(int x, int y, String statName) {
		return new CustomTextureButton.Builder()
				.position(x, y)
				.size(14, 11)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 0, 0, 10)
				.textureSize(10, 10)
				.onPress(button -> {
					IncreaseStatC2S.StatType statEnum = IncreaseStatC2S.StatType.valueOf(statName.toUpperCase());
					NetworkHandler.sendToServer(new IncreaseStatC2S(statEnum, tpMultiplier));
				})
				.build();
	}

	private void refreshStatButtons() {
		if (strButton != null) this.removeWidget(strButton);
		if (skpButton != null) this.removeWidget(skpButton);
		if (resButton != null) this.removeWidget(resButton);
		if (vitButton != null) this.removeWidget(vitButton);
		if (pwrButton != null) this.removeWidget(pwrButton);
		if (eneButton != null) this.removeWidget(eneButton);
		if (multiplierButton != null) this.removeWidget(multiplierButton);

		strButton = null;
		skpButton = null;
		resButton = null;
		vitButton = null;
		pwrButton = null;
		eneButton = null;
		multiplierButton = null;

		initStatButtons();
	}

	private double[] getDamageReductionPercentages() {
		double baseDefense = statsData.getDefense();

		int maxValue = statsData.getConfiguredMaxValue();
		double expectedMaxStats = statsData.isMaxLevelValueInsteadOfStats() ? (maxValue * 6.0) / 2.0 : maxValue;
		double expectedMaxDef = expectedMaxStats * statsData.getStatScaling("DEF");
		double k_factor = Math.max(12.0, expectedMaxDef * ConfigManager.getCombatConfig().getDefenseReductionScale());

		double baseReduction;
		if (baseDefense >= 0) baseReduction = baseDefense / (k_factor + baseDefense);
		else baseReduction = baseDefense / (k_factor - baseDefense);

		double baseCap = ConfigManager.getCombatConfig().getBaseDamageReductionCap();
		baseReduction = Mth.clamp(baseReduction, 0.0, baseCap);

		int totalProtection = 0;
		if (Minecraft.getInstance().player != null) {
			for (var stack : Minecraft.getInstance().player.getArmorSlots())
				totalProtection += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, stack);
		}

		double enchReduction = 0.0;
		if (totalProtection > 0) {
			double effectiveProtection = 0.0;
			int remaining = totalProtection;
			double mult = 1.0;
			while (remaining > 0) {
				int chunk = Math.min(remaining, 4);
				effectiveProtection += chunk * mult;
				remaining -= chunk;
				mult *= 0.5;
			}
			double k_ench = 20.0;
			enchReduction = effectiveProtection / (k_ench + effectiveProtection);

			double totalCap = ConfigManager.getCombatConfig().getEnchantmentDamageReductionCap();
			double maxEnchReductionAllowed = (totalCap - baseReduction) / (1.0 - baseReduction);

			enchReduction = Mth.clamp(enchReduction, 0.0, Math.max(0, maxEnchReductionAllowed));
		}

		double mitigationReduction = 1.0 - ((1.0 - baseReduction) * (1.0 - enchReduction));
		double mitigationReductionPct = Mth.clamp(mitigationReduction * 100.0, 0.0, 100.0);

		return new double[]{mitigationReductionPct, enchReduction * 100.0};
	}

	private void renderMenuPanels(GuiGraphics graphics, int leftOffset, int rightOffset, int topOffset) {
		int centerX = getUiWidth() / 2;
		int centerY = getUiHeight() / 2;

		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		graphics.blit(MENU_BIG, 12 + leftOffset, centerY - 105, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, 29 + leftOffset, centerY - 95, 142, 22, 107, 21, 256, 256);
		graphics.blit(MENU_BIG, 43 + leftOffset, centerY - 28, 142, 0, 79, 21, 256, 256);

		graphics.blit(MENU_BIG, getUiWidth() - 158 + rightOffset, centerY - 105, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, getUiWidth() - 141 + rightOffset, centerY - 95, 142, 22, 107, 21, 256, 256);

		graphics.blit(MENU_SMALL, centerX - 70, 8 + topOffset, 0, 95, 145, 58, 256, 256);

		RenderSystem.disableBlend();
	}

	private void renderPlayerInfo(GuiGraphics graphics, int mouseX, int mouseY, int topOffset) {
		int centerX = getUiWidth() / 2;

		if (Minecraft.getInstance().player == null) return;
		String playerName = Minecraft.getInstance().player.getName().getString();
		String raceName = statsData.getCharacter().getRaceName();
		String gender = statsData.getCharacter().getGender();
		int alignment = statsData.getResources().getAlignment();

		int nameColor = gender.equals("male") ? 0x63FFFF : 0xFF69B4;
		String genderSymbol = gender.equals("male") ? "♂" : "♀";

		Component nameBold = txt(playerName).withStyle(style -> style.withBold(true));

		int totalWidth = font.width(nameBold) + font.width(" " + genderSymbol);
		int startX = centerX - (totalWidth / 2);

		graphics.drawString(font, nameBold, startX + 1, 19 + topOffset, 0x000000, false);
		graphics.drawString(font, nameBold, startX - 1, 19 + topOffset, 0x000000, false);
		graphics.drawString(font, nameBold, startX, 20 + topOffset, 0x000000, false);
		graphics.drawString(font, nameBold, startX, 18 + topOffset, 0x000000, false);
		graphics.drawString(font, nameBold, startX, 19 + topOffset, nameColor, false);

		int symbolX = startX + font.width(nameBold);
		graphics.drawString(font, " " + genderSymbol, symbolX + 1, 19 + topOffset, 0x000000, false);
		graphics.drawString(font, " " + genderSymbol, symbolX - 1, 19 + topOffset, 0x000000, false);
		graphics.drawString(font, " " + genderSymbol, symbolX, 20 + topOffset, 0x000000, false);
		graphics.drawString(font, " " + genderSymbol, symbolX, 18 + topOffset, 0x000000, false);
		graphics.drawString(font, " " + genderSymbol, symbolX, 19 + topOffset, nameColor, false);

		if (mouseX >= centerX - 40 && mouseX <= centerX + 40 && mouseY >= 19 && mouseY <= 19 + font.lineHeight) {
			Component title = tr("gui.dragonminez.character_stats.alignment").withStyle(ChatFormatting.GOLD);
			List<Component> tooltip = new ArrayList<>();
			if (alignment > 60) {
				tooltip.add(tr("gui.dragonminez.character_stats.alignment.good", alignment).withStyle(ChatFormatting.YELLOW));
			} else if (alignment > 40) {
				tooltip.add(tr("gui.dragonminez.character_stats.alignment.neutral", alignment).withStyle(ChatFormatting.YELLOW));
			} else {
				tooltip.add(tr("gui.dragonminez.character_stats.alignment.evil", alignment).withStyle(ChatFormatting.YELLOW));
			}
			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, tooltip, null, 0xFFFF00);
		}

		Component raceComponent = tr("race.dragonminez." + raceName);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, raceComponent, centerX, 46 + topOffset, 0xFFFFFF, 0x000000);
	}

	private void updatePanelWidgetOffsets(int leftOffset, int rightOffset) {
		int leftButtonX = 27 + leftOffset;
		if (strButton != null) strButton.setX(leftButtonX);
		if (skpButton != null) skpButton.setX(leftButtonX);
		if (resButton != null) resButton.setX(leftButtonX);
		if (vitButton != null) vitButton.setX(leftButtonX);
		if (pwrButton != null) pwrButton.setX(leftButtonX);
		if (eneButton != null) eneButton.setX(leftButtonX);
		if (multiplierButton != null) multiplierButton.setX(leftButtonX);

		int rightSwitchX = getUiWidth() - 45 + rightOffset;
		if (viewSwitchButton != null) viewSwitchButton.setX(rightSwitchX);
	}

	private void renderStatsInfo(GuiGraphics graphics, int mouseX, int mouseY) {
		int centerY = getUiHeight() / 2;
		int titleY = centerY - 88;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.info").withStyle(style -> style.withBold(true)), 85, titleY, 0xFBC51C, 0x000000);

		int level = statsData.getLevel();
		float tps = statsData.getResources().getTrainingPoints();
		String characterClass = statsData.getCharacter().getCharacterClass();
		String form = statsData.getCharacter().getActiveForm();
		String stackForm = statsData.getCharacter().getActiveStackForm();

		int labelX = 30;
		int valueX = 70;
		int startY = centerY - 72;

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.level").withStyle(style -> style.withBold(true)), labelX, startY, 0xD7FEF5, 0x000000);
		TextUtil.drawStringWithBorder(graphics, this.font, txt(numberFormatter.format(level)), valueX + 5, startY, 0xFFFFFF, 0x000000);

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.tps").withStyle(style -> style.withBold(true)), labelX, startY + 11, 0xD7FEF5, 0x000000);
		String displayedTps = formatTpsDisplay(tps);
		int tpsX = valueX + 5;
		int tpsY = startY + 11;
		TextUtil.drawStringWithBorder(graphics, this.font, txt(displayedTps), tpsX, tpsY, 0xFFE593, 0x000000);

		if (shouldUseScientificForTps(tps)) {
			int tpsWidth = font.width(displayedTps);
			if (mouseX >= tpsX && mouseX <= tpsX + tpsWidth && mouseY >= tpsY && mouseY <= tpsY + font.lineHeight) {
				List<Component> tooltip = new ArrayList<>();
				tooltip.add(txt(fullTpsFormatter.format(tps)).withStyle(ChatFormatting.YELLOW));
				TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), null, tooltip, null, 0xFFFF00);
			}
		}

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.form").withStyle(style -> style.withBold(true)), labelX, startY + 22, 0xD7FEF5, 0x000000);
		boolean isBase = form == null || form.isEmpty() || form.equals("base");
		boolean hasActiveStack = stackForm != null && !stackForm.isEmpty();
		String activeStackGroup = statsData.getCharacter().getActiveStackFormGroup();

		Component baseFormComponent = isBase
				? tr("race.dragonminez.base")
				: tr("race.dragonminez." + statsData.getCharacter().getRaceName() + ".form." + statsData.getCharacter().getActiveFormGroup() + "." + form);

		Component formComponent;
		if (!isBase) {
			if (hasActiveStack) formComponent = baseFormComponent.copy()
					.append(" ")
					.append(tr("race.dragonminez.stack.form." + activeStackGroup + "." + stackForm));
			else formComponent = baseFormComponent;
		} else if (hasActiveStack) {
			formComponent = tr("race.dragonminez.stack.group." + activeStackGroup)
					.append(" ")
					.append(tr("race.dragonminez.stack.form." + activeStackGroup + "." + stackForm));
		} else {
			formComponent = baseFormComponent;
		}
		TextUtil.drawStringWithBorder(graphics, this.font, formComponent, valueX + 5, startY + 22, 0xC7EAFC, 0x000000);

		if (mouseX >= valueX + 5 && mouseX <= valueX + 85 && mouseY >= startY + 22 && mouseY <= startY + 22 + font.lineHeight) {
			Component title = tr("gui.dragonminez.character_stats.form.mastery").withStyle(ChatFormatting.GOLD);
			List<Component> tooltip = new ArrayList<>();
			boolean hasTitle = false;

			if (!isBase) {
				String currentFormGroup = statsData.getCharacter().getActiveFormGroup();
				if (currentFormGroup != null && !currentFormGroup.isEmpty()) {
					var formConfig = ConfigManager.getFormGroup(statsData.getCharacter().getRaceName(), currentFormGroup);
					if (formConfig != null) {
						var formData = formConfig.getForm(form);
						if (formData != null) {
							if (!hasTitle) {
								tooltip.add(tr("gui.dragonminez.character_stats.form.mastery").withStyle(ChatFormatting.GOLD));
								hasTitle = true;
							}

							double mastery = statsData.getCharacter().getFormMasteries().getMastery(currentFormGroup, form);
							double maxMastery = formData.getMaxMastery();

							tooltip.add(txt(" ")
									.append(baseFormComponent.copy().withStyle(ChatFormatting.GRAY))
									.append(txt(": " + String.format(Locale.US, "%.2f", mastery) + " / " + String.format(Locale.US, "%.0f", maxMastery)).withStyle(ChatFormatting.AQUA)));
						}
					}
				}
			}

			if (stackForm != null && !stackForm.isEmpty()) {
				String currentStackGroup = statsData.getCharacter().getActiveStackFormGroup();
				var stackFormConfig = ConfigManager.getStackFormGroup(currentStackGroup);

				if (currentStackGroup != null && !currentStackGroup.isEmpty() && stackFormConfig != null) {
					var formData = stackFormConfig.getForm(stackForm);
					if (formData != null) {
						if (!hasTitle) {
							tooltip.add(tr("gui.dragonminez.character_stats.form.mastery").withStyle(ChatFormatting.GOLD));
							hasTitle = true;
						}

						double mastery = statsData.getCharacter().getStackFormMasteries().getMastery(currentStackGroup, stackForm);
						double maxMastery = formData.getMaxMastery();

						Component stackFormComponent = tr("race.dragonminez.stack.group." + currentStackGroup)
								.append(" ")
								.append(tr("race.dragonminez.stack.form." + currentStackGroup + "." + stackForm));

						tooltip.add(txt(" ")
								.append(stackFormComponent.copy().withStyle(ChatFormatting.GRAY))
								.append(txt(": " + String.format(Locale.US, "%.2f", mastery) + " / " + String.format(Locale.US, "%.0f", maxMastery)).withStyle(ChatFormatting.AQUA)));
					}
				}
			}

			if (!tooltip.isEmpty()) {
				TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, tooltip, null, 0xFFCC00);
			}
		}

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.class").withStyle(style -> style.withBold(true)), labelX, startY + 33, 0xD7FEF5, 0x000000);
		Component classComponent = tr("class.dragonminez." + characterClass);
		TextUtil.drawStringWithBorder(graphics, this.font, classComponent, valueX + 5, startY + 33, 0xFFFFFF, 0x000000);

		int statsStartY = centerY - 21;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.stats").withStyle(ChatFormatting.BOLD), 82, statsStartY, 0x68CCFF, 0x000000);

		String[] statNames = {"str", "skp", "res", "vit", "pwr", "ene"};
		String[] statNamesUpper = {"STR", "SKP", "RES", "VIT", "PWR", "ENE"};
		int[] statValues = {
				statsData.getStats().getStrength(),
				statsData.getStats().getStrikePower(),
				statsData.getStats().getResistance(),
				statsData.getStats().getVitality(),
				statsData.getStats().getKiPower(),
				statsData.getStats().getEnergy()
		};

		int statY = centerY - 3;
		for (int i = 0; i < statNames.length; i++) {
			int statLabelX = 42;
			int yPos = statY + (i * 12);

			double totalMult = statsData.getTotalMultiplier(statNamesUpper[i]);

			int baseValue = statValues[i];
			double modifiedValue = baseValue * totalMult;

			Component statComponent = tr("gui.dragonminez.character_stats." + statNames[i]).withStyle(style -> style.withBold(true));
			TextUtil.drawStringWithBorder(graphics, this.font, statComponent, statLabelX, yPos, 0xD71432, 0x000000);

			boolean hasMult = Math.abs(totalMult - 1.0) > 0.01;
			int statColor = hasMult ? 0xFFFF00 : 0xFFD7AB;
			String statText = hasMult
					? numberFormatter.format((int) modifiedValue) + " x" + String.format(Locale.US, "%.1f", totalMult)
					: numberFormatter.format(baseValue);

			TextUtil.drawStringWithBorder(graphics, this.font, txt(statText), valueX + 5, yPos, statColor, 0x000000);

			if (mouseX >= statLabelX && mouseX <= statLabelX + 25 && mouseY >= yPos && mouseY <= yPos + font.lineHeight) {
				Component title = tr("gui.dragonminez.character_stats." + statNames[i]).withStyle(ChatFormatting.BOLD);
				List<Component> desc = new ArrayList<>();
				desc.add(tr("gui.dragonminez.character_stats." + statNames[i] + ".desc"));

				List<Component> extras = new ArrayList<>();
				if (hasMult) {
					extras.add(tr("gui.dragonminez.character_stats.base_value").append(": " + numberFormatter.format(baseValue)).withStyle(ChatFormatting.GRAY));
					extras.add(tr("gui.dragonminez.character_stats.modified_value").append(": " + numberFormatter.format((int) modifiedValue)).withStyle(ChatFormatting.YELLOW));

					if (statNamesUpper[i].equals("RES")) {
						double formDef = statsData.getFormMultiplier("DEF");
						double formStm = statsData.getFormMultiplier("STM");
						double stackDef = statsData.getStackFormMultiplier("DEF");
						double stackStm = statsData.getStackFormMultiplier("STM");
						double effectsDef = statsData.getEffectsMultiplier("DEF");
						double effectsStm = statsData.getEffectsMultiplier("STM");
						double secondaryDef = statsData.getSecondaryStatEffects().getMultiplier(SecondaryStatEffects.DEF);

						boolean hasForm = Math.abs(formDef - 1.0) > 0.01 || Math.abs(formStm - 1.0) > 0.01;
						boolean hasStack = Math.abs(stackDef - 1.0) > 0.01 || Math.abs(stackStm - 1.0) > 0.01;
						boolean hasEffects = Math.abs(effectsDef - 1.0) > 0.01 || Math.abs(effectsStm - 1.0) > 0.01;
						boolean hasSecondary = Math.abs(secondaryDef - 1.0) > 0.01;

						if (hasForm || hasStack || hasEffects || hasSecondary) {
							extras.add(tr("gui.dragonminez.character_stats.multipliers").withStyle(ChatFormatting.AQUA));
							if (hasForm) {
								extras.add(tr("gui.dragonminez.character_stats.form_multiplier")
										.append(txt(" ("))
										.append(tr("gui.dragonminez.character_stats.def")).append(txt(": x" + String.format(Locale.US, "%.2f", formDef) + ", "))
										.append(tr("gui.dragonminez.character_stats.stm")).append(txt(": x" + String.format(Locale.US, "%.2f", formStm) + ")"))
										.withStyle(ChatFormatting.GOLD));
							}
							if (hasStack) {
								extras.add(tr("gui.dragonminez.character_stats.stack_multiplier")
										.append(txt(" ("))
										.append(tr("gui.dragonminez.character_stats.def")).append(txt(": x" + String.format(Locale.US, "%.2f", stackDef) + ", "))
										.append(tr("gui.dragonminez.character_stats.stm")).append(txt(": x" + String.format(Locale.US, "%.2f", stackStm) + ")"))
										.withStyle(ChatFormatting.RED));
							}
							if (hasEffects) {
								extras.add(tr("gui.dragonminez.character_stats.effects_multiplier")
										.append(txt(" ("))
										.append(tr("gui.dragonminez.character_stats.def")).append(txt(": x" + String.format(Locale.US, "%.2f", effectsDef) + ", "))
										.append(tr("gui.dragonminez.character_stats.stm")).append(txt(": x" + String.format(Locale.US, "%.2f", effectsStm) + ")"))
										.withStyle(ChatFormatting.LIGHT_PURPLE));
							}
							if (hasSecondary) {
								extras.add(tr("gui.dragonminez.character_stats.secondary_multiplier")
										.append(txt(" ("))
										.append(tr("gui.dragonminez.character_stats.def")).append(txt(": x" + String.format(Locale.US, "%.2f", secondaryDef) + ")"))
										.withStyle(ChatFormatting.DARK_AQUA));
							}
						}
					} else {
						double formMultiplier = statsData.getFormMultiplier(statNamesUpper[i]);
						double stackMultiplier = statsData.getStackFormMultiplier(statNamesUpper[i]);
						double effectsMultiplier = statsData.getEffectsMultiplier(statNamesUpper[i]);
						double secondaryMultiplier = statsData.getSecondaryStatEffects().getMultiplier(statNamesUpper[i]);
						boolean hasForm = Math.abs(formMultiplier - 1.0) > 0.01;
						boolean hasStack = Math.abs(stackMultiplier - 1.0) > 0.01;
						boolean hasEffects = Math.abs(effectsMultiplier - 1.0) > 0.01;
						boolean hasSecondary = Math.abs(secondaryMultiplier - 1.0) > 0.01;

						if (hasForm || hasStack || hasEffects || hasSecondary) {
							extras.add(tr("gui.dragonminez.character_stats.multipliers").withStyle(ChatFormatting.AQUA));
							if (hasForm) extras.add(tr("gui.dragonminez.character_stats.form_multiplier").append(" x" + String.format(Locale.US, "%.2f", formMultiplier)).withStyle(ChatFormatting.GOLD));
							if (hasStack) extras.add(tr("gui.dragonminez.character_stats.stack_multiplier").append(" x" + String.format(Locale.US, "%.2f", stackMultiplier)).withStyle(ChatFormatting.RED));
							if (hasEffects) extras.add(tr("gui.dragonminez.character_stats.effects_multiplier").append(" x" + String.format(Locale.US, "%.2f", effectsMultiplier)).withStyle(ChatFormatting.LIGHT_PURPLE));
							if (hasSecondary) extras.add(tr("gui.dragonminez.character_stats.secondary_multiplier").append(" x" + String.format(Locale.US, "%.2f", secondaryMultiplier)).withStyle(ChatFormatting.DARK_AQUA));
						}
					}
				}

				var bonuses = new ArrayList<>(statsData.getBonusStats().getBonuses(statNamesUpper[i]));
				if (statNamesUpper[i].equals("RES")) {
					List<String> seenNames = new ArrayList<>();
					for (var b : bonuses) seenNames.add(b.name);
					for (var b : statsData.getBonusStats().getBonuses("DEF")) {
						if (!seenNames.contains(b.name)) { bonuses.add(b); seenNames.add(b.name); }
					}
					for (var b : statsData.getBonusStats().getBonuses("STM")) {
						if (!seenNames.contains(b.name)) { bonuses.add(b); seenNames.add(b.name); }
					}
				}
				bonuses.sort((a, b) -> a.name.compareTo(b.name));
				if (!bonuses.isEmpty()) {
					extras.add(tr("gui.dragonminez.character_stats.bonus").withStyle(ChatFormatting.AQUA));
					for (var bonus : bonuses) {
						String opDisplay = bonus.operation.equals("*") ? "x" : bonus.operation;
						String bonusText = bonus.name.replace("_", " ") + ": " + opDisplay + (bonus.operation.equals("*") ? String.format(Locale.US, "%.2f", bonus.value) : String.format(Locale.US, "%.0f", bonus.value));
						extras.add(txt("  " + bonusText).withStyle(ChatFormatting.GREEN));
					}
				}

				appendDynamicGrowthProgress(extras, statNamesUpper[i], baseValue);

				TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, desc, extras, 0xD71432);
			}
		}

		int pendingAP = statsData.getPendingAttributePoints();
		boolean showAP = pendingAP > 0;
		int bottomY = statY + 76;
		int bottomValueX = 75;

		Component bottomLabel = (showAP
				? tr("gui.dragonminez.character_stats.ap")
				: tr("gui.dragonminez.character_stats.tpc")).withStyle(style -> style.withBold(true));
		int labelColor = showAP ? 0xB36BFF : 0x2BFFE2;
		TextUtil.drawStringWithBorder(graphics, this.font, bottomLabel, 42, bottomY, labelColor, 0x000000);

		int maxStats = ConfigManager.getServerConfig().getGameplay().getMaxValue();
		int tpCost = statsData.calculateRecursiveCost(tpMultiplier, maxStats);
		Component bottomValue = showAP ? txt(numberFormatter.format(pendingAP)) : txt(numberFormatter.format(tpCost));
		int valueColor = showAP ? 0xFFFF00 : 0xFFCE41;
		TextUtil.drawStringWithBorder(graphics, this.font, bottomValue, bottomValueX, bottomY, valueColor, 0x000000);
		TextUtil.drawStringWithBorder(graphics, this.font, txt("x" + tpMultiplier), bottomValueX, bottomY + 10, 0x2BFFE2, 0x000000);

		if (mouseX >= 42 && mouseX <= bottomValueX + font.width(bottomValue) && mouseY >= bottomY && mouseY <= bottomY + font.lineHeight) {
			List<Component> desc = new ArrayList<>();
			List<Component> extras = new ArrayList<>();
			Component title;
			int color;
			if (showAP) {
				title = tr("gui.dragonminez.character_stats.ap").withStyle(ChatFormatting.LIGHT_PURPLE);
				color = 0xB36BFF;
				desc.add(tr("gui.dragonminez.character_stats.ap.desc"));
				extras.add(tr("gui.dragonminez.character_stats.ap.pending", numberFormatter.format(pendingAP)).withStyle(ChatFormatting.YELLOW));
			} else {
				title = tr("gui.dragonminez.character_stats.tpc").withStyle(ChatFormatting.AQUA);
				color = 0x2BFFE2;
				desc.add(tr("gui.dragonminez.character_stats.tpc.desc"));
				if (hasShiftDown()) {
					extras.add(tr("gui.dragonminez.character_stats.tpc.formula1").withStyle(ChatFormatting.GRAY));
					extras.add(tr("gui.dragonminez.character_stats.tpc.formula2").withStyle(ChatFormatting.GRAY));
					extras.add(tr("gui.dragonminez.character_stats.tpc.formula3").withStyle(ChatFormatting.GRAY));
				} else {
					appendAdvancedHint(extras, true);
				}
			}
			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, desc, extras, color);
		}
	}

	private void appendDynamicGrowthProgress(List<Component> extras, String statName, int currentStat) {
		if (statsData == null) return;
		if (!ConfigManager.getServerConfig().getDynamicGrowth().isEnabled()) return;

		DynamicGrowthStat stat;
		try {
			stat = DynamicGrowthStat.valueOf(statName);
		} catch (IllegalArgumentException e) {
			return;
		}

		extras.add(tr("dynamicgrowth.dragonminez.title").withStyle(ChatFormatting.AQUA));
		if (statsData.getMaxAllowedIncreaseForStat(statName, 1) <= 0) {
			extras.add(txt("  ").append(tr("dynamicgrowth.dragonminez.maxed")).withStyle(ChatFormatting.GREEN));
			return;
		}

		int requiredXp = DynamicGrowthMath.requiredXp(currentStat);
		double currentXp = statsData.getDynamicGrowth().getPracticeXp(stat);
		double percent = requiredXp <= 0 ? 100.0 : currentXp / requiredXp * 100.0;
		if (!Double.isFinite(percent)) percent = 0.0;
		percent = Math.max(0.0, Math.min(100.0, percent));
		extras.add(txt("  " + String.format(Locale.US, "%.1f", currentXp) + " / " + requiredXp + " XP ("
				+ String.format(Locale.US, "%.1f", percent) + "%)").withStyle(ChatFormatting.GREEN));
	}

	private void renderStatisticsInfo(GuiGraphics graphics, int mouseX, int mouseY) {
		if (useHexagonView) renderStatisticsInfoHexagon(graphics, mouseX, mouseY);
		else renderStatisticsInfoList(graphics, mouseX, mouseY);
		renderBattlePowerInfo(graphics, mouseX, mouseY);
		renderGravityInfo(graphics, mouseX, mouseY);
		renderTpMultiplierInfo(graphics, mouseX, mouseY);
	}

	private void renderBattlePowerInfo(GuiGraphics graphics, int mouseX, int mouseY) {
		if (statsData == null) return;

		int centerY = getUiHeight() / 2;
		int labelX = getUiWidth() - 137;
		int y = centerY + 54;

		boolean androidUpgraded = statsData.getStatus().isAndroidUpgraded();
		long bp = (long) statsData.getBattlePowerExact();
		String displayedBp = androidUpgraded ? "???" : formatBattlePower(bp);

		Component label = tr("gui.dragonminez.character_stats.power_level");
		Component separator = txt(": ");
		Component value = txt(displayedBp);

		TextUtil.drawStringWithBorder(graphics, this.font, label, labelX, y, 0x7CFDD6, 0x000000);
		int separatorX = labelX + font.width(label);
		TextUtil.drawStringWithBorder(graphics, this.font, separator, separatorX, y, 0x7CFDD6, 0x000000);
		int valueX = separatorX + font.width(separator);
		TextUtil.drawStringWithBorder(graphics, this.font, value, valueX, y, 0xFFE593, 0x000000);

		int textWidth = font.width(label) + font.width(separator) + font.width(value);
		if (!androidUpgraded && shouldUseCompactForBp(bp) && mouseX >= labelX && mouseX <= labelX + textWidth && mouseY >= y && mouseY <= y + font.lineHeight) {
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(txt(numberFormatter.format(bp)).withStyle(ChatFormatting.YELLOW));
			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), null, tooltip, null, 0xFFFF00);
		}
	}

	private boolean shouldUseCompactForBp(double bp) {
		return bp > 9_999_999L;
	}

	private String formatBattlePower(double bp) {
		if (!shouldUseCompactForBp(bp)) return numberFormatter.format(bp);

		final String[] suffixes = {"M", "B", "T", "Qa", "Qi"};
		final double[] scales = {1e6, 1e9, 1e12, 1e15, 1e18};
		int i = scales.length - 1;
		while (i > 0 && bp < scales[i]) i--;
		return compactBpFormatter.format(bp / scales[i]) + suffixes[i];
	}

	private void renderStatisticsInfoList(GuiGraphics graphics, int mouseX, int mouseY) {
		int rightX = getUiWidth() - 137;
		int centerY = getUiHeight() / 2;
		int titleY = centerY - 88;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.statistics").withStyle(style -> style.withBold(true)), getUiWidth() - 85, titleY, 0xF91E64, 0x000000);

		int labelStartY = centerY - 64;
		int valueX = getUiWidth() - 65;

		double meleeDamage = statsData.getMeleeDamage();
		double maxMeleeDamage = statsData.getMaxMeleeDamage();
		double strikeDamage = statsData.getStrikeDamage();
		double maxStrikeDamage = statsData.getMaxStrikeDamage();
		float stamina = statsData.getMaxStamina();
		double defense = statsData.getFlatMitigation();
		double maxDefense = statsData.getMaxFlatMitigation();
		double health = Minecraft.getInstance().player.getMaxHealth();
		double kiDamage = statsData.getKiDamage();
		double maxKiDamage = statsData.getMaxKiDamage();
		float energy = statsData.getMaxEnergy();

		double strScaling = statsData.getStatScaling("STR");
		double skpScaling = statsData.getStatScaling("SKP");
		double resScaling = statsData.getStatScaling("DEF");
		double vitScaling = statsData.getStatScaling("VIT");
		double pwrScaling = statsData.getStatScaling("PWR");
		double eneScaling = statsData.getStatScaling("ENE");
		double stmScaling = statsData.getStatScaling("STM");

		RaceStatsConfig statsConfig = ConfigManager.getRaceStats(statsData.getCharacter().getRaceName());
		RaceStatsConfig.ClassStats classStats = statsConfig != null ? statsConfig.getClassStats(statsData.getCharacter().getCharacterClass()) : null;

		String[] labels = {
				"gui.dragonminez.character_stats.melee_damage",
				"gui.dragonminez.character_stats.strike_damage",
				"gui.dragonminez.character_stats.stamina",
				"gui.dragonminez.character_stats.defense",
				"gui.dragonminez.character_stats.health",
				"gui.dragonminez.character_stats.ki_damage",
				"gui.dragonminez.character_stats.max_energy"
		};

		boolean isTransformed = statsData.getCharacter().hasActiveForm() || statsData.getCharacter().hasActiveStackForm();

		for (int i = 0; i < labels.length; i++) {
			int yPos = labelStartY + (i * 12);
			Component labelComponent = tr(labels[i]);
			TextUtil.drawStringWithBorder(graphics, this.font, labelComponent, rightX, yPos, 0x7CFDD6, 0x000000);

			if (mouseX >= rightX && mouseX <= rightX + 60 && mouseY >= yPos && mouseY <= yPos + font.lineHeight) {
				Component title = tr(labels[i]).withStyle(ChatFormatting.BOLD);
				List<Component> desc = new ArrayList<>();
				List<Component> extras = new ArrayList<>();

				switch (i) {
					case 0 -> {
						desc.add(tr("gui.dragonminez.character_stats.melee_damage.tooltip1"));
						desc.add(tr("gui.dragonminez.character_stats.melee_damage.tooltip2", formatUpToOneDecimal(strScaling)).withStyle(ChatFormatting.YELLOW));
						desc.add(tr("gui.dragonminez.character_stats.max_value", formatUpToOneDecimal(maxMeleeDamage)).withStyle(ChatFormatting.GREEN));
						double defensePen = getDefensePenetrationPercentage();
						if (defensePen > 0) extras.add(tr("gui.dragonminez.character_stats.defense_penetration").append(txt(": " + formatUpToOneDecimal(defensePen) + "%")).withStyle(ChatFormatting.RED));
					}
					case 1 -> {
						desc.add(tr("gui.dragonminez.character_stats.strike_damage.tooltip1"));
						desc.add(tr("gui.dragonminez.character_stats.strike_damage.tooltip2", formatUpToOneDecimal(skpScaling)).withStyle(ChatFormatting.YELLOW));
						desc.add(tr("gui.dragonminez.character_stats.max_value", formatUpToOneDecimal(maxStrikeDamage)).withStyle(ChatFormatting.GREEN));
						double defensePen = getDefensePenetrationPercentage();
						if (defensePen > 0) extras.add(tr("gui.dragonminez.character_stats.defense_penetration").append(txt(": " + formatUpToOneDecimal(defensePen) + "%")).withStyle(ChatFormatting.RED));
					}
					case 2 -> {
						desc.add(tr("gui.dragonminez.character_stats.stamina.tooltip1"));
						desc.add(tr("gui.dragonminez.character_stats.stamina.tooltip2", formatUpToOneDecimal(stmScaling)).withStyle(ChatFormatting.YELLOW));
						if (classStats != null) {
							double currentRegenSec = (classStats.getBaseSp5() + (statsData.getStats().getResistance() * statsData.getTotalMultiplier("RES") * classStats.getSp5StmScaling())) * 0.2;
							extras.add(Component.translatable("gui.dragonminez.customization.stat.regen.stm").append(": ")
									.append(txt(String.format(Locale.US, "%.1f/s", currentRegenSec)))
									.withStyle(ChatFormatting.AQUA));
						}
						extras.add(tr("gui.dragonminez.character_stats.stamina_per_hit").append(": ")
								.append(txt(formatUpToOneDecimal(statsData.getStaminaPerHit())))
								.withStyle(ChatFormatting.GOLD));
						if (isTransformed) {
							double stamDrain = statsData.getEffectiveStaminaDrain();
							if (stamDrain > 0) extras.add(tr("gui.dragonminez.character_stats.form_drain.stamina.cost", formatUpToOneDecimal(stamDrain)).withStyle(ChatFormatting.RED));
							else if (stamDrain < 0) extras.add(tr("gui.dragonminez.character_stats.form_drain.stamina.regen", formatUpToOneDecimal(Math.abs(stamDrain))).withStyle(ChatFormatting.GREEN));

							double stamMult = statsData.getAdjustedStaminaDrainMultiplier();
							if (stamMult != 1.0) {
								ChatFormatting color = stamMult > 1.0 ? ChatFormatting.RED : ChatFormatting.GREEN;
								extras.add(tr("gui.dragonminez.character_stats.form_drain.stamina.multiplier", formatUpToOneDecimal(stamMult)).withStyle(color));
							}
						}
					}
					case 3 -> {
						desc.add(tr("gui.dragonminez.character_stats.defense.tooltip1"));
						desc.add(tr("gui.dragonminez.character_stats.defense.tooltip2", formatUpToOneDecimal(resScaling)).withStyle(ChatFormatting.YELLOW));
						desc.add(tr("gui.dragonminez.character_stats.max_value", formatUpToOneDecimal(maxDefense)).withStyle(ChatFormatting.GREEN));

						double[] pcts = getDamageReductionPercentages();
						extras.add(tr("gui.dragonminez.character_stats.defense").append(": ")
								.append(txt(formatUpToTwoDecimals(pcts[0]) + "% "))
								.append(tr("gui.dragonminez.character_stats.dmg_reduction"))
								.withStyle(ChatFormatting.AQUA));

						if (pcts[1] > 0) {
							extras.add(tr("gui.dragonminez.character_stats.protection").append(": ")
									.append(txt(formatUpToTwoDecimals(pcts[1]) + "% "))
									.append(tr("gui.dragonminez.character_stats.dmg_reduction"))
									.withStyle(ChatFormatting.LIGHT_PURPLE));
						}
					}
					case 4 -> {
						desc.add(tr("gui.dragonminez.character_stats.health.tooltip1"));
						desc.add(tr("gui.dragonminez.character_stats.health.tooltip2", formatUpToOneDecimal(vitScaling)).withStyle(ChatFormatting.YELLOW));
						if (classStats != null) {
							double currentRegenSec = (classStats.getBaseHp5() + (statsData.getStats().getVitality() * statsData.getTotalMultiplier("VIT") * classStats.getHp5VitScaling())) * 0.2;
							extras.add(Component.translatable("gui.dragonminez.customization.stat.regen.hp").append(": ")
									.append(txt(String.format(Locale.US, "%.1f/s", currentRegenSec)))
									.withStyle(ChatFormatting.AQUA));
						}
						if (isTransformed) {
							double hpDrain = statsData.getEffectiveHealthDrain();
							if (hpDrain > 0) extras.add(tr("gui.dragonminez.character_stats.form_drain.health.cost", formatUpToOneDecimal(hpDrain)).withStyle(ChatFormatting.RED));
							else if (hpDrain < 0) extras.add(tr("gui.dragonminez.character_stats.form_drain.health.regen", formatUpToOneDecimal(Math.abs(hpDrain))).withStyle(ChatFormatting.GREEN));
						}
					}
					case 5 -> {
						desc.add(tr("gui.dragonminez.character_stats.ki_damage.tooltip1"));
						desc.add(tr("gui.dragonminez.character_stats.ki_damage.tooltip2", formatUpToOneDecimal(pwrScaling)).withStyle(ChatFormatting.YELLOW));
						desc.add(tr("gui.dragonminez.character_stats.max_value", formatUpToOneDecimal(maxKiDamage)).withStyle(ChatFormatting.GREEN));
						double defensePen = getDefensePenetrationPercentage();
						if (defensePen > 0) extras.add(tr("gui.dragonminez.character_stats.ki_damage").append(txt(": ")).append(txt(formatUpToOneDecimal(kiDamage))).withStyle(ChatFormatting.AQUA));
					}
					case 6 -> {
						desc.add(tr("gui.dragonminez.character_stats.max_energy.tooltip1"));
						desc.add(tr("gui.dragonminez.character_stats.max_energy.tooltip2", formatUpToOneDecimal(eneScaling)).withStyle(ChatFormatting.YELLOW));
						if (classStats != null) {
							double currentRegenSec = (classStats.getBaseEp5() + (statsData.getStats().getEnergy() * statsData.getTotalMultiplier("ENE") * classStats.getEp5EneScaling())) * 0.2;
							extras.add(Component.translatable("gui.dragonminez.customization.stat.regen.ki").append(": ")
									.append(txt(String.format(Locale.US, "%.1f/s", currentRegenSec)))
									.withStyle(ChatFormatting.AQUA));
						}
						if (isTransformed) {
							double eneDrain = statsData.getEffectiveEnergyDrain();
							if (eneDrain > 0) extras.add(tr("gui.dragonminez.character_stats.form_drain.energy.cost", formatUpToOneDecimal(eneDrain)).withStyle(ChatFormatting.RED));
							else if (eneDrain < 0) extras.add(tr("gui.dragonminez.character_stats.form_drain.energy.regen", formatUpToOneDecimal(Math.abs(eneDrain))).withStyle(ChatFormatting.GREEN));
						}
					}
				}

				if (i == 0 || i == 1) appendAttackSkillInfo(extras);
				if (i == 3) appendKiProtectionInfo(extras);
				if (i == 2) appendMeditationInfo(extras, false);
				if (i == 6) appendMeditationInfo(extras, true);
				boolean hasAdvanced = ((i == 0 || i == 1) && hasAttackSkillInfo())
						|| (i == 3 && hasKiProtectionInfo())
						|| ((i == 2 || i == 6) && hasMeditationInfo());
				appendAdvancedHint(extras, hasAdvanced);

				TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, desc, extras, 0x7CFDD6);
			}
		}

		double strTotalMult = statsData.getTotalMultiplier("STR");
		double skpTotalMult = statsData.getTotalMultiplier("SKP");
		double stmTotalMult = statsData.getTotalMultiplier("STM");
		double defTotalMult = statsData.getTotalMultiplier("DEF");
		double vitTotalMult = statsData.getTotalMultiplier("VIT");
		double pwrTotalMult = statsData.getTotalMultiplier("PWR");
		double eneTotalMult = statsData.getTotalMultiplier("ENE");

		int meleeDamageColor = Math.abs(strTotalMult - 1.0) > 0.01 ? 0xFFFF00 : 0xFFD7AB;
		int strikeDamageColor = Math.abs(skpTotalMult - 1.0) > 0.01 ? 0xFFFF00 : 0xFFD7AB;
		int staminaColor = Math.abs(stmTotalMult - 1.0) > 0.01 ? 0xFFFF00 : 0xFFD7AB;
		int defenseColor = Math.abs(defTotalMult - 1.0) > 0.01 ? 0xFFFF00 : 0xFFD7AB;
		int healthColor = Math.abs(vitTotalMult - 1.0) > 0.01 ? 0xFFFF00 : 0xFFD7AB;
		int kiDamageColor = Math.abs(pwrTotalMult - 1.0) > 0.01 ? 0xFFFF00 : 0xFFD7AB;
		int energyColor = Math.abs(eneTotalMult - 1.0) > 0.01 ? 0xFFFF00 : 0xFFD7AB;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(formatUpToOneDecimal(meleeDamage)), valueX + 15, labelStartY, meleeDamageColor, 0x000000);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(formatUpToOneDecimal(strikeDamage)), valueX + 15, labelStartY + 12, strikeDamageColor, 0x000000);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(formatUpToOneDecimal(stamina)), valueX + 15, labelStartY + 24, staminaColor, 0x000000);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(formatUpToOneDecimal(defense)), valueX + 15, labelStartY + 36, defenseColor, 0x000000);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(formatUpToOneDecimal(health)), valueX + 15, labelStartY + 48, healthColor, 0x000000);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(formatUpToOneDecimal(kiDamage)), valueX + 15, labelStartY + 60, kiDamageColor, 0x000000);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(formatUpToOneDecimal(energy)), valueX + 15, labelStartY + 72, energyColor, 0x000000);
	}

	private void renderStatisticsInfoHexagon(GuiGraphics graphics, int mouseX, int mouseY) {
		int centerY = getUiHeight() / 2;
		int titleY = centerY - 88;
		int centerX = getUiWidth() - 85;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.statistics").withStyle(style -> style.withBold(true)), centerX, titleY, 0xF91E64, 0x000000);

		double meleeDamage = statsData.getMeleeDamage();
		double maxMeleeDamage = statsData.getMaxMeleeDamage();
		double strikeDamage = statsData.getStrikeDamage();
		double maxStrikeDamage = statsData.getMaxStrikeDamage();
		float stamina = statsData.getMaxStamina();
		double defense = statsData.getFlatMitigation();
		double maxDefense = statsData.getMaxFlatMitigation();
		float health = statsData.getMaxHealth();
		double kiDamage = statsData.getKiDamage();
		double maxKiDamage = statsData.getMaxKiDamage();
		float energy = statsData.getMaxEnergy();

		double strScaling = statsData.getStatScaling("STR");
		double skpScaling = statsData.getStatScaling("SKP");
		double resScaling = (statsData.getStatScaling("DEF") + statsData.getStatScaling("STM")) / 2;
		double vitScaling = statsData.getStatScaling("VIT");
		double pwrScaling = statsData.getStatScaling("PWR");
		double eneScaling = statsData.getStatScaling("ENE");

		int hexCenterY = centerY - 20;
		float maxRadius = 35.0f;

		int strValue = statsData.getStats().getStrength();
		int skpValue = statsData.getStats().getStrikePower();
		int resValue = statsData.getStats().getResistance();
		int vitValue = statsData.getStats().getVitality();
		int pwrValue = statsData.getStats().getKiPower();
		int eneValue = statsData.getStats().getEnergy();

		int maxStatValue = Math.max(strValue, Math.max(skpValue, Math.max(resValue, Math.max(vitValue, Math.max(pwrValue, eneValue)))));

		if (maxStatValue == 0) {
			maxStatValue = 1;
		}

		int absoluteMaxStats = ConfigManager.getServerConfig().getGameplay().getMaxValue();
		float referenceValue;
		if (statsData.isMaxLevelValueInsteadOfStats()) {
			referenceValue = Math.max(1.0f, (float) maxStatValue);
		} else if (maxStatValue >= absoluteMaxStats * 0.9f) {
			referenceValue = absoluteMaxStats;
		} else {
			referenceValue = maxStatValue * 1.1f;
		}

		float[] statRadii = new float[6];
		statRadii[0] = maxRadius * Math.min(1.0f, ((float) strValue / referenceValue));
		statRadii[1] = maxRadius * Math.min(1.0f, ((float) resValue / referenceValue));
		statRadii[2] = maxRadius * Math.min(1.0f, ((float) eneValue / referenceValue));
		statRadii[3] = maxRadius * Math.min(1.0f, ((float) vitValue / referenceValue));
		statRadii[4] = maxRadius * Math.min(1.0f, ((float) pwrValue / referenceValue));
		statRadii[5] = maxRadius * Math.min(1.0f, ((float) skpValue / referenceValue));

		float[] hexPointsX = new float[6];
		float[] hexPointsY = new float[6];
		float[] hexPointsMaxX = new float[6];
		float[] hexPointsMaxY = new float[6];

		for (int i = 0; i < 6; i++) {
			double angle = Math.toRadians(60 * i - 90);
			hexPointsX[i] = centerX + (float) (statRadii[i] * Math.cos(angle));
			hexPointsY[i] = hexCenterY + (float) (statRadii[i] * Math.sin(angle));

			hexPointsMaxX[i] = centerX + (float) (maxRadius * Math.cos(angle));
			hexPointsMaxY[i] = hexCenterY + (float) (maxRadius * Math.sin(angle));
		}

		drawHexagon(graphics, centerX, hexCenterY, hexPointsX, hexPointsY, hexPointsMaxX, hexPointsMaxY);

		float textOffset = 10.0f;

		int strX = (int) (centerX + (maxRadius + textOffset) * Math.cos(Math.toRadians(-90)));
		int strY = (int) (hexCenterY + (maxRadius + textOffset) * Math.sin(Math.toRadians(-90)));

		int resX = (int) (centerX + (maxRadius + textOffset) * Math.cos(Math.toRadians(-30)));
		int resY = (int) (hexCenterY + (maxRadius + textOffset) * Math.sin(Math.toRadians(-30)));

		int eneX = (int) (centerX + (maxRadius + textOffset) * Math.cos(Math.toRadians(30)));
		int eneY = (int) (hexCenterY + (maxRadius + textOffset) * Math.sin(Math.toRadians(30)));

		int vitX = (int) (centerX + (maxRadius + textOffset) * Math.cos(Math.toRadians(90)));
		int vitY = (int) (hexCenterY + (maxRadius + textOffset) * Math.sin(Math.toRadians(90)));

		int pwrX = (int) (centerX + (maxRadius + textOffset) * Math.cos(Math.toRadians(150)));
		int pwrY = (int) (hexCenterY + (maxRadius + textOffset) * Math.sin(Math.toRadians(150)));

		int skpX = (int) (centerX + (maxRadius + textOffset) * Math.cos(Math.toRadians(210)));
		int skpY = (int) (hexCenterY + (maxRadius + textOffset) * Math.sin(Math.toRadians(210)));

		Component strComponent = tr("gui.dragonminez.character_stats.str").withStyle(style -> style.withBold(true));
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, strComponent, strX, strY, 0xD71432, 0x000000);

		Component skpComponent = tr("gui.dragonminez.character_stats.skp").withStyle(style -> style.withBold(true));
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, skpComponent, skpX, skpY, 0xD71432, 0x000000);

		Component resComponent = tr("gui.dragonminez.character_stats.res").withStyle(style -> style.withBold(true));
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, resComponent, resX, resY, 0xD71432, 0x000000);

		Component pwrComponent = tr("gui.dragonminez.character_stats.pwr").withStyle(style -> style.withBold(true));
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, pwrComponent, pwrX, pwrY, 0xD71432, 0x000000);

		Component eneComponent = tr("gui.dragonminez.character_stats.ene").withStyle(style -> style.withBold(true));
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, eneComponent, eneX, eneY, 0xD71432, 0x000000);

		Component vitComponent = tr("gui.dragonminez.character_stats.vit").withStyle(style -> style.withBold(true));
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, vitComponent, vitX, vitY, 0xD71432, 0x000000);

		int strTextWidth = font.width(strComponent);
		int skpTextWidth = font.width(skpComponent);
		int resTextWidth = font.width(resComponent);
		int pwrTextWidth = font.width(pwrComponent);
		int eneTextWidth = font.width(eneComponent);
		int vitTextWidth = font.width(vitComponent);

		boolean isTransformed = statsData.getCharacter().hasActiveForm() || statsData.getCharacter().hasActiveStackForm();

		if (mouseX >= strX - strTextWidth / 2 && mouseX <= strX + strTextWidth / 2 && mouseY >= strY && mouseY <= strY + font.lineHeight) {
			Component title = tr("gui.dragonminez.character_stats.str").withStyle(ChatFormatting.BOLD);
			List<Component> desc = new ArrayList<>();
			desc.add(tr("gui.dragonminez.character_stats.melee_damage.tooltip1"));
			desc.add(tr("gui.dragonminez.character_stats.melee_damage.tooltip2",
					formatUpToOneDecimal(strScaling)).withStyle(ChatFormatting.YELLOW));
			desc.add(tr("gui.dragonminez.character_stats.max_value",
					formatUpToOneDecimal(maxMeleeDamage)).withStyle(ChatFormatting.GREEN));

			List<Component> extras = new ArrayList<>();
			extras.add(tr("gui.dragonminez.character_stats.melee_damage").append(": ")
					.append(txt(formatUpToOneDecimal(meleeDamage)))
					.withStyle(ChatFormatting.AQUA));
			double defensePen = getDefensePenetrationPercentage();
			if (defensePen > 0) {
				extras.add(tr("gui.dragonminez.character_stats.defense_penetration").append(txt(": " + formatUpToOneDecimal(defensePen) + "%"))
						.withStyle(ChatFormatting.RED));
			}
			appendAttackSkillInfo(extras);
			appendAdvancedHint(extras, hasAttackSkillInfo());
			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, desc, extras, 0xD71432);
		}

		if (mouseX >= skpX - skpTextWidth / 2 && mouseX <= skpX + skpTextWidth / 2 && mouseY >= skpY && mouseY <= skpY + font.lineHeight) {
			Component title = tr("gui.dragonminez.character_stats.skp").withStyle(ChatFormatting.BOLD);
			List<Component> desc = new ArrayList<>();
			desc.add(tr("gui.dragonminez.character_stats.strike_damage.tooltip1"));
			desc.add(tr("gui.dragonminez.character_stats.strike_damage.tooltip2",
					formatUpToOneDecimal(skpScaling)).withStyle(ChatFormatting.YELLOW));
			desc.add(tr("gui.dragonminez.character_stats.max_value",
					formatUpToOneDecimal(maxStrikeDamage)).withStyle(ChatFormatting.GREEN));

			List<Component> extras = new ArrayList<>();
			extras.add(tr("gui.dragonminez.character_stats.strike_damage").append(": ")
					.append(txt(formatUpToOneDecimal(strikeDamage)))
					.withStyle(ChatFormatting.AQUA));
			double defensePen = getDefensePenetrationPercentage();
			if (defensePen > 0) {
				extras.add(tr("gui.dragonminez.character_stats.defense_penetration").append(txt(": " + formatUpToOneDecimal(defensePen) + "%"))
						.withStyle(ChatFormatting.RED));
			}
			appendAttackSkillInfo(extras);
			appendAdvancedHint(extras, hasAttackSkillInfo());
			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, desc, extras, 0xD71432);
		}

		if (mouseX >= resX - resTextWidth / 2 && mouseX <= resX + resTextWidth / 2 && mouseY >= resY && mouseY <= resY + font.lineHeight) {
			Component title = tr("gui.dragonminez.character_stats.res").withStyle(ChatFormatting.BOLD);
			List<Component> desc = new ArrayList<>();
			desc.add(tr("gui.dragonminez.character_stats.defense.tooltip1"));
			desc.add(tr("gui.dragonminez.character_stats.defense.tooltip2",
					formatUpToOneDecimal(resScaling)).withStyle(ChatFormatting.YELLOW));
			desc.add(tr("gui.dragonminez.character_stats.max_value",
					formatUpToOneDecimal(maxDefense)).withStyle(ChatFormatting.GREEN));
			desc.add(Component.empty());
			desc.add(tr("gui.dragonminez.character_stats.stamina.tooltip1"));
			desc.add(tr("gui.dragonminez.character_stats.stamina.tooltip2",
					formatUpToOneDecimal(resScaling)).withStyle(ChatFormatting.YELLOW));

			List<Component> extras = new ArrayList<>();

			extras.add(tr("gui.dragonminez.character_stats.defense").append(": ")
					.append(txt(formatUpToOneDecimal(defense)))
					.withStyle(ChatFormatting.AQUA));
			extras.add(tr("gui.dragonminez.character_stats.stamina").append(": ")
					.append(txt(formatUpToOneDecimal(stamina)))
					.withStyle(ChatFormatting.AQUA));
			extras.add(tr("gui.dragonminez.character_stats.stamina_per_hit").append(": ")
					.append(txt(formatUpToOneDecimal(statsData.getStaminaPerHit())))
					.withStyle(ChatFormatting.GOLD));

			if (isTransformed) {
				double stamDrain = statsData.getEffectiveStaminaDrain();
				if (stamDrain > 0) extras.add(tr("gui.dragonminez.character_stats.form_drain.stamina.cost", formatUpToOneDecimal(stamDrain)).withStyle(ChatFormatting.RED));
				else if (stamDrain < 0) extras.add(tr("gui.dragonminez.character_stats.form_drain.stamina.regen", formatUpToOneDecimal(Math.abs(stamDrain))).withStyle(ChatFormatting.GREEN));

				double stamMult = statsData.getAdjustedStaminaDrainMultiplier();
				if (stamMult != 1.0) {
					ChatFormatting color = stamMult > 1.0 ? ChatFormatting.RED : ChatFormatting.GREEN;
					extras.add(tr("gui.dragonminez.character_stats.form_drain.stamina.multiplier", formatUpToOneDecimal(stamMult)).withStyle(color));
				}
			}

			double[] pcts = getDamageReductionPercentages();
			extras.add(txt(""));

			extras.add(tr("gui.dragonminez.character_stats.defense").append(": ")
					.append(txt(formatUpToTwoDecimals(pcts[0]) + "% "))
					.append(tr("gui.dragonminez.character_stats.dmg_reduction"))
					.withStyle(ChatFormatting.AQUA));

			if (pcts[1] > 0) {
				extras.add(tr("gui.dragonminez.character_stats.protection").append(": ")
						.append(txt(formatUpToTwoDecimals(pcts[1]) + "% "))
						.append(tr("gui.dragonminez.character_stats.dmg_reduction"))
						.withStyle(ChatFormatting.LIGHT_PURPLE));
			}
			appendKiProtectionInfo(extras);
			appendMeditationInfo(extras, false);
			appendAdvancedHint(extras, hasKiProtectionInfo() || hasMeditationInfo());
			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, desc, extras, 0xD71432);
		}

		if (mouseX >= pwrX - pwrTextWidth / 2 && mouseX <= pwrX + pwrTextWidth / 2 && mouseY >= pwrY && mouseY <= pwrY + font.lineHeight) {
			Component title = tr("gui.dragonminez.character_stats.pwr").withStyle(ChatFormatting.BOLD);
			List<Component> desc = new ArrayList<>();
			desc.add(tr("gui.dragonminez.character_stats.ki_damage.tooltip1"));
			desc.add(tr("gui.dragonminez.character_stats.ki_damage.tooltip2",
					formatUpToOneDecimal(pwrScaling)).withStyle(ChatFormatting.YELLOW));
			desc.add(tr("gui.dragonminez.character_stats.max_value",
					formatUpToOneDecimal(maxKiDamage)).withStyle(ChatFormatting.GREEN));

			List<Component> extras = new ArrayList<>();
			extras.add(tr("gui.dragonminez.character_stats.ki_damage").append(": ")
					.append(txt(formatUpToOneDecimal(kiDamage)))
					.withStyle(ChatFormatting.AQUA));
			double defensePen = getDefensePenetrationPercentage();
			if (defensePen > 0) {
				extras.add(tr("gui.dragonminez.character_stats.defense_penetration").append(txt(": " + formatUpToOneDecimal(defensePen) + "%"))
						.withStyle(ChatFormatting.RED));
			}
			appendAdvancedHint(extras, false);
			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, desc, extras, 0xD71432);
		}

		if (mouseX >= eneX - eneTextWidth / 2 && mouseX <= eneX + eneTextWidth / 2 && mouseY >= eneY && mouseY <= eneY + font.lineHeight) {
			Component title = tr("gui.dragonminez.character_stats.ene").withStyle(ChatFormatting.BOLD);
			List<Component> desc = new ArrayList<>();
			desc.add(tr("gui.dragonminez.character_stats.max_energy.tooltip1"));
			desc.add(tr("gui.dragonminez.character_stats.max_energy.tooltip2",
					formatUpToOneDecimal(eneScaling)).withStyle(ChatFormatting.YELLOW));

			List<Component> extras = new ArrayList<>();
			extras.add(tr("gui.dragonminez.character_stats.max_energy").append(": ")
					.append(txt(formatUpToOneDecimal(energy)))
					.withStyle(ChatFormatting.AQUA));

			if (isTransformed) {
				double eneDrain = statsData.getEffectiveEnergyDrain();
				if (eneDrain > 0) extras.add(tr("gui.dragonminez.character_stats.form_drain.energy.cost", formatUpToOneDecimal(eneDrain)).withStyle(ChatFormatting.RED));
				else if (eneDrain < 0) extras.add(tr("gui.dragonminez.character_stats.form_drain.energy.regen", formatUpToOneDecimal(Math.abs(eneDrain))).withStyle(ChatFormatting.GREEN));
			}

			appendMeditationInfo(extras, true);
			appendAdvancedHint(extras, hasMeditationInfo());
			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, desc, extras, 0xD71432);
		}

		if (mouseX >= vitX - vitTextWidth / 2 && mouseX <= vitX + vitTextWidth / 2 && mouseY >= vitY && mouseY <= vitY + font.lineHeight) {
			Component title = tr("gui.dragonminez.character_stats.vit").withStyle(ChatFormatting.BOLD);
			List<Component> desc = new ArrayList<>();
			desc.add(tr("gui.dragonminez.character_stats.health.tooltip1"));
			desc.add(tr("gui.dragonminez.character_stats.health.tooltip2",
					formatUpToOneDecimal(vitScaling)).withStyle(ChatFormatting.YELLOW));

			List<Component> extras = new ArrayList<>();
			extras.add(tr("gui.dragonminez.character_stats.health").append(": ")
					.append(txt(formatUpToOneDecimal(health)))
					.withStyle(ChatFormatting.AQUA));

			if (isTransformed) {
				double hpDrain = statsData.getEffectiveHealthDrain();
				if (hpDrain > 0) extras.add(tr("gui.dragonminez.character_stats.form_drain.health.cost", formatUpToOneDecimal(hpDrain)).withStyle(ChatFormatting.RED));
				else if (hpDrain < 0) extras.add(tr("gui.dragonminez.character_stats.form_drain.health.regen", formatUpToOneDecimal(Math.abs(hpDrain))).withStyle(ChatFormatting.GREEN));
			}

			appendAdvancedHint(extras, false);
			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, desc, extras, 0xD71432);
		}
	}

	private void renderPlayerModel(GuiGraphics graphics, int x, int y, int scale, float mouseX, float mouseY) {
		LivingEntity player = Minecraft.getInstance().player;
		if (player == null) return;

		int adjustedScale = getAdjustedModelScale(scale);

		float xRotation = (float) Math.atan((double) ((float) y - mouseY) / 40.0F);
		float yRotation = (float) Math.atan((double) ((float) x - mouseX) / 40.0F);

		Quaternionf pose = (new Quaternionf()).rotateZ((float) Math.PI);
		Quaternionf cameraOrientation = (new Quaternionf()).rotateX(xRotation * 20.0F * ((float) Math.PI / 180F));
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
		graphics.pose().translate(0.0D, 0.0D, 150.0D);
		InventoryScreen.renderEntityInInventory(graphics, x, y, adjustedScale, pose, cameraOrientation, player);
		graphics.pose().popPose();

		player.yBodyRot = yBodyRotO;
		player.setYRot(yRotO);
		player.setXRot(xRotO);
		player.yHeadRotO = yHeadRotO;
		player.yHeadRot = yHeadRot;
	}

	private void initViewSwitchButton() {
		int centerY = getUiHeight() / 2;
		int buttonX = getUiWidth() - 45;
		int buttonY = centerY + 90;
		LivingEntity player = Minecraft.getInstance().player;

		viewSwitchButton = new SwitchButton(buttonX, buttonY, useHexagonView, Component.empty(), button -> {
			useHexagonView = !useHexagonView;
			ConfigManager.getUserConfig().setHexagonStatsDisplay(useHexagonView);
			ConfigManager.saveGeneralUserConfig();
			((SwitchButton) button).toggle();
			if (useHexagonView) player.playSound(MainSounds.SWITCH_OFF.get());
			else player.playSound(MainSounds.SWITCH_ON.get());
		});
		this.addRenderableWidget(viewSwitchButton);
	}
	
	private void renderGravityInfo(GuiGraphics graphics, int mouseX, int mouseY) {
		if (statsData == null) return;

		int centerY = getUiHeight() / 2;
		int labelX = getUiWidth() - 137;
		int valueX = getUiWidth() - 65;
		int y = centerY + 66;

		double envGravity = ClientGravityState.getEnvironmentalGravity();
		double netGravity = ClientGravityState.getNetGravity();
		double gravityStatMult = ClientGravityState.getStatMult();
		int totalWeight = ClientGravityState.getTotalWeight();
		int effectiveWeight = ClientGravityState.getEffectiveWeight();
		int idealWeight = ClientGravityState.getIdealWeight();
		int zone = ClientGravityState.getZone();
		double weightTpMult = ClientGravityState.getWeightTpMult();
		double gravityTpBonus = ClientGravityState.getTpGravityMult();

		boolean hasGravity = netGravity > 0.01;
		boolean hasPenalty = gravityStatMult < 0.999;

		Component label = tr("gui.dragonminez.character_stats.gravity");
		TextUtil.drawStringWithBorder(graphics, this.font, label, labelX, y, hasGravity ? 0xFF7722 : 0x7CFDD6, 0x000000);

		String penStr = hasPenalty
				? " -" + formatUpToOneDecimal((1.0 - gravityStatMult) * 100.0) + "%"
				: "";
		Component valueComp = hasGravity
				? txt(formatUpToOneDecimal(netGravity) + "g" + penStr)
				: txt("--");
		int valueColor = hasGravity ? 0xFF9944 : 0xFFD7AB;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, valueComp, valueX, y, valueColor, 0x000000);

		int totalTextWidth = font.width(label) + font.width(valueComp) + 20;
		if (mouseX >= labelX && mouseX <= labelX + totalTextWidth && mouseY >= y && mouseY <= y + font.lineHeight) {
			Component title = tr("gui.dragonminez.character_stats.gravity").withStyle(ChatFormatting.GOLD);

			List<Component> desc = new ArrayList<>();
			desc.add(tr("gui.dragonminez.character_stats.gravity.tooltip.environmental",
					formatUpToOneDecimal(envGravity)).withStyle(ChatFormatting.YELLOW));
			if (totalWeight > 0) {
				if (effectiveWeight > totalWeight) {
					desc.add(tr("gui.dragonminez.character_stats.gravity.tooltip.weight_load",
							numberFormatter.format(totalWeight), numberFormatter.format(effectiveWeight)).withStyle(ChatFormatting.YELLOW));
				} else {
					desc.add(tr("gui.dragonminez.character_stats.gravity.tooltip.weight",
							numberFormatter.format(totalWeight)).withStyle(ChatFormatting.YELLOW));
				}
			}

			List<Component> extras = new ArrayList<>();
			if (idealWeight > 0) {
				var gravityCfg = ConfigManager.getServerConfig().getGravity();
				int low = (int) Math.round(idealWeight * gravityCfg.getTpIdealRatioLow());
				int high = (int) Math.round(idealWeight * gravityCfg.getTpIdealRatioHigh());
				String range = numberFormatter.format(low) + " - " + numberFormatter.format(high);
				extras.add(tr("gui.dragonminez.character_stats.gravity.tooltip.ideal_weight", range).withStyle(ChatFormatting.GOLD));
			}
			if (totalWeight > 0 && zone > 0) {
				extras.add(tr("gui.dragonminez.character_stats.gravity.tooltip.zone",
						zoneName(zone)).withStyle(zoneColor(zone)));
			}
			extras.add(tr("gui.dragonminez.character_stats.gravity.tooltip.net",
					formatUpToOneDecimal(netGravity)).withStyle(hasGravity ? ChatFormatting.RED : ChatFormatting.GREEN));

			if (hasPenalty) {
				extras.add(tr("gui.dragonminez.character_stats.gravity.tooltip.stat_penalty",
						formatUpToOneDecimal((1.0 - gravityStatMult) * 100.0)).withStyle(ChatFormatting.RED));
			}
			if (gravityTpBonus > 1.0) {
				extras.add(tr("gui.dragonminez.character_stats.gravity.tooltip.tp_bonus",
						formatUpToTwoDecimals(gravityTpBonus)).withStyle(ChatFormatting.GREEN));
			}
			if (weightTpMult > 1.01 && totalWeight > 0) {
				extras.add(tr("gui.dragonminez.character_stats.gravity.tooltip.weight_bell",
						formatUpToTwoDecimals(weightTpMult)).withStyle(ChatFormatting.AQUA));
			}

			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(),
					title, desc, extras, 0xFF7722);
		}
	}

	private Component zoneName(int zone) {
		String key = switch (zone) {
			case 1 -> "gui.dragonminez.character_stats.gravity.zone.light";
			case 2 -> "gui.dragonminez.character_stats.gravity.zone.ideal";
			case 3 -> "gui.dragonminez.character_stats.gravity.zone.heavy";
			case 4 -> "gui.dragonminez.character_stats.gravity.zone.overload";
			default -> "gui.dragonminez.character_stats.gravity.zone.none";
		};
		return tr(key);
	}

	private ChatFormatting zoneColor(int zone) {
		return switch (zone) {
			case 2 -> ChatFormatting.GREEN;
			case 3 -> ChatFormatting.GOLD;
			case 4 -> ChatFormatting.RED;
			default -> ChatFormatting.GRAY;
		};
	}

	private void renderTpMultiplierInfo(GuiGraphics graphics, int mouseX, int mouseY) {
		int centerY = getUiHeight() / 2;
		int labelX = getUiWidth() - 137;
		int y = centerY + 78;
		double totalMultiplier = statsData.getTpTotalMultiplier();
		String totalMult = formatUpToTwoDecimals(totalMultiplier);

		Component label = tr("gui.dragonminez.character_stats.tp_multiplier");
		Component separator = txt(": ");
		Component value = txt("x" + totalMult);

		TextUtil.drawStringWithBorder(graphics, this.font, label, labelX, y, 0x7CFDD6, 0x000000);
		int separatorX = labelX + font.width(label);
		TextUtil.drawStringWithBorder(graphics, this.font, separator, separatorX, y, 0x7CFDD6, 0x000000);

		int valueColor = totalMultiplier > 1.0 ? 0xFFFF00 : 0xFFE593;
		int valueX = separatorX + font.width(separator);
		TextUtil.drawStringWithBorder(graphics, this.font, value, valueX, y, valueColor, 0x000000);

		int textWidth = font.width(label) + font.width(separator) + font.width(value);
		if (mouseX >= labelX && mouseX <= labelX + textWidth && mouseY >= y && mouseY <= y + font.lineHeight) {
			Component title = tr("gui.dragonminez.character_stats.tp_multiplier").withStyle(ChatFormatting.GOLD);
			List<Component> desc = new ArrayList<>();
			desc.add(tr("gui.dragonminez.character_stats.tp_multiplier.tooltip.total", totalMult).withStyle(ChatFormatting.YELLOW));

			List<Component> extras = new ArrayList<>();
			extras.add(tr("gui.dragonminez.character_stats.tp_multiplier.tooltip.general", formatUpToTwoDecimals(statsData.getTpGlobalMultiplier())).withStyle(ChatFormatting.GRAY));
			extras.add(tr("gui.dragonminez.character_stats.tp_multiplier.tooltip.class", formatUpToTwoDecimals(statsData.getTpClassMultiplier())).withStyle(ChatFormatting.AQUA));

			if (statsData.isFrostDemonTpPassiveActive()) {
				extras.add(tr("gui.dragonminez.character_stats.tp_multiplier.tooltip.frost_demon", formatUpToTwoDecimals(statsData.getTpFrostDemonMultiplier())).withStyle(ChatFormatting.LIGHT_PURPLE));
			}

			double htc = statsData.getTpHTCMultiplier();
			if (htc > 1.0) {
				extras.add(tr("gui.dragonminez.character_stats.tp_multiplier.tooltip.htc", formatUpToTwoDecimals(htc)).withStyle(ChatFormatting.GOLD));
			}

			double gravity = ClientGravityState.getTpGravityMult();
			if (gravity > 1.0) {
				extras.add(tr("gui.dragonminez.character_stats.tp_multiplier.tooltip.gravity", formatUpToTwoDecimals(gravity)).withStyle(ChatFormatting.GREEN));
			}

			double weightBell = ClientGravityState.getWeightTpMult();
			if (weightBell > 1.01) {
				extras.add(tr("gui.dragonminez.character_stats.tp_multiplier.tooltip.weight", formatUpToTwoDecimals(weightBell)).withStyle(ChatFormatting.YELLOW));
			}

			double potionEffect = statsData.getTpPotionEffectMultiplier();
			if (potionEffect > 1.0) {
				extras.add(tr("gui.dragonminez.character_stats.tp_multiplier.tooltip.effect", formatUpToTwoDecimals(potionEffect)).withStyle(ChatFormatting.LIGHT_PURPLE));
			}

			double mutantTp = statsData.getMutantTpMultiplier();
			if (mutantTp > 1.0) {
				extras.add(tr("gui.dragonminez.character_stats.tp_multiplier.tooltip.mutant", formatUpToTwoDecimals(mutantTp)).withStyle(ChatFormatting.DARK_PURPLE));
			}

			double progressionTp = statsData.getProgressionTpGainMultiplier();
			if (progressionTp > 1.0) {
				extras.add(tr("gui.dragonminez.character_stats.tp_multiplier.tooltip.progression", formatUpToTwoDecimals(progressionTp)).withStyle(ChatFormatting.GOLD));
			}
			TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, desc, extras, 0x7CFDD6);
		}
	}

	private void appendAdvancedHint(List<Component> extras, boolean hasAdvancedContent) {
		if (hasShiftDown() || !hasAdvancedContent) return;
		extras.add(tr("gui.dragonminez.character_stats.shift_hint").withStyle(ChatFormatting.DARK_GRAY));
	}

	private boolean hasAttackSkillInfo() {
		var skills = statsData.getSkills();
		if (skills.hasSkill("ki_infusion") && skills.getSkillLevel("ki_infusion") > 0) return true;
		if (skills.hasSkill("kimanipulation") && skills.getSkillLevel("kimanipulation") > 0) {
			String weaponType = statsData.getStatus().getKiWeaponType();
			return weaponType != null && ConfigManager.getCombatConfig().getKiWeaponConfig(weaponType) != null;
		}
		return false;
	}

	private boolean hasKiProtectionInfo() {
		var skills = statsData.getSkills();
		return skills.hasSkill("kiprotection") && skills.getSkillLevel("kiprotection") > 0;
	}

	private boolean hasMeditationInfo() {
		return statsData.getSkills().getSkillLevel("meditation") > 0;
	}

	private MutableComponent skillBonusLine(String labelKey, String valuePart, boolean active) {
		MutableComponent line = tr(labelKey).append(": ").append(txt(valuePart))
				.withStyle(active ? ChatFormatting.AQUA : ChatFormatting.GRAY);
		if (!active) line.append(tr("gui.dragonminez.character_stats.inactive_suffix").withStyle(ChatFormatting.DARK_GRAY));
		return line;
	}

	private void appendAttackSkillInfo(List<Component> extras) {
		if (!hasShiftDown()) return;
		var skills = statsData.getSkills();

		int infusionLevel = skills.getSkillLevel("ki_infusion");
		if (skills.hasSkill("ki_infusion") && infusionLevel > 0) {
			double dmgPerLevel = ConfigManager.getCombatConfig().getKiInfusionDamagePerLevel();
			double infuseDamage = statsData.getMaxEnergy() * dmgPerLevel * infusionLevel;
			extras.add(skillBonusLine("gui.dragonminez.character_stats.infuse_damage",
					"+" + formatUpToOneDecimal(infuseDamage), skills.isSkillActive("ki_infusion")));
		}

		int weaponLevel = skills.getSkillLevel("kimanipulation");
		if (skills.hasSkill("kimanipulation") && weaponLevel > 0) {
			String weaponType = statsData.getStatus().getKiWeaponType();
			var kiCfg = weaponType != null ? ConfigManager.getCombatConfig().getKiWeaponConfig(weaponType) : null;
			if (kiCfg != null) {
				double weaponMult = weaponLevel * 0.1;
				double weaponDamage = kiCfg.getBaseDamage() + (statsData.getKiDamage() * kiCfg.getKiScalingDamage() * weaponMult);
				extras.add(skillBonusLine("gui.dragonminez.character_stats.ki_weapon_damage",
						"+" + formatUpToOneDecimal(weaponDamage), skills.isSkillActive("kimanipulation")));
			}
		}
	}

	private void appendKiProtectionInfo(List<Component> extras) {
		if (!hasShiftDown()) return;
		var skills = statsData.getSkills();
		int level = skills.getSkillLevel("kiprotection");
		if (!skills.hasSkill("kiprotection") || level <= 0) return;
		double pct = level * ConfigManager.getCombatConfig().getKiProtectionMitigationPerLevel() * 100.0;
		boolean active = skills.isSkillActive("kiprotection");
		MutableComponent line = tr("gui.dragonminez.character_stats.ki_protection").append(": ")
				.append(txt(formatUpToOneDecimal(pct) + "% "))
				.append(tr("gui.dragonminez.character_stats.dmg_reduction"))
				.withStyle(active ? ChatFormatting.AQUA : ChatFormatting.GRAY);
		if (!active) line.append(" ").append(tr("gui.dragonminez.character_stats.inactive_suffix").withStyle(ChatFormatting.DARK_GRAY));
		extras.add(line);
	}

	private void appendMeditationInfo(List<Component> extras, boolean energy) {
		if (!hasShiftDown()) return;
		int level = statsData.getSkills().getSkillLevel("meditation");
		if (level <= 0) return;
		double base = energy ? baseEnergyRegenPerSec() : baseStaminaRegenPerSec();
		double bonus = base * (level * 0.05);
		extras.add(tr("gui.dragonminez.character_stats.meditation_bonus",
				formatUpToOneDecimal(bonus), formatUpToOneDecimal(level * 5.0)).withStyle(ChatFormatting.GREEN));
	}

	private RaceStatsConfig.ClassStats currentClassStats() {
		RaceStatsConfig sc = ConfigManager.getRaceStats(statsData.getCharacter().getRaceName());
		return sc != null ? sc.getClassStats(statsData.getCharacter().getCharacterClass()) : null;
	}

	private double baseStaminaRegenPerSec() {
		RaceStatsConfig.ClassStats cs = currentClassStats();
		if (cs == null) return 0.0;
		return (cs.getBaseSp5() + (statsData.getStats().getResistance() * statsData.getTotalMultiplier("RES") * cs.getSp5StmScaling())) * 0.2;
	}

	private double baseEnergyRegenPerSec() {
		RaceStatsConfig.ClassStats cs = currentClassStats();
		if (cs == null) return 0.0;
		return (cs.getBaseEp5() + (statsData.getStats().getEnergy() * statsData.getTotalMultiplier("ENE") * cs.getEp5EneScaling())) * 0.2;
	}

	private String formatUpToOneDecimal(double value) {
		return oneDecimalFormatter.format(value);
	}

	private String formatUpToTwoDecimals(double value) {
		return twoDecimalFormatter.format(value);
	}

	private boolean shouldUseScientificForTps(float tps) {
		if (!Float.isFinite(tps)) return false;
		return Math.floor(Math.abs(tps)) >= 10_000_000_000d;
	}

	private String formatTpsDisplay(float tps) {
		if (Float.isNaN(tps) || Float.isInfinite(tps)) return String.valueOf(tps);
		return shouldUseScientificForTps(tps) ? scientificFormatter.format(tps) : fullTpsFormatter.format(tps);
	}

	private double getDefensePenetrationPercentage() {
		int skillLevel = statsData.getSkills().getSkillLevel("defense_penetration");
		int enchLevel = 0;

		if (Minecraft.getInstance().player != null) enchLevel = EnchantmentHelper.getEnchantmentLevel(MainEnchants.DEFENSE_PENETRATION.get(), Minecraft.getInstance().player);

		return Math.min(0.50, (skillLevel * 0.025) + (enchLevel * 0.025)) * 100.0;
	}

	private void drawHexagon(GuiGraphics graphics, int centerX, int centerY, float[] pointsX, float[] pointsY, float[] maxPointsX, float[] maxPointsY) {
		var pose = graphics.pose();
		var matrix = pose.last().pose();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();

		var tesselator = Tesselator.getInstance();
		var buffer = tesselator.getBuilder();

		String auraColorHex = statsData.getCharacter().getAuraColor();
		float[] auraRgb = ColorUtils.hexToRgb(auraColorHex);

		float fillR = auraRgb[0];
		float fillG = auraRgb[1];
		float fillB = auraRgb[2];
		float fillAlpha = 0.3f;

		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

		for (int i = 0; i < 6; i++) {
			int next = (i + 1) % 6;
			buffer.vertex(matrix, centerX, centerY, 0).color(fillR, fillG, fillB, fillAlpha).endVertex();
			buffer.vertex(matrix, pointsX[i], pointsY[i], 0).color(fillR, fillG, fillB, fillAlpha).endVertex();
			buffer.vertex(matrix, pointsX[next], pointsY[next], 0).color(fillR, fillG, fillB, fillAlpha).endVertex();
		}

		tesselator.end();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

		float outlineR = 0.0f;
		float outlineG = 0.0f;
		float outlineB = 0.0f;
		float outlineAlpha = 1.0f;
		float borderWidth = 0.5f;

		for (int i = 0; i < 6; i++) {
			int next = (i + 1) % 6;
			float x1 = maxPointsX[i];
			float y1 = maxPointsY[i];
			float x2 = maxPointsX[next];
			float y2 = maxPointsY[next];

			float dx = x2 - x1;
			float dy = y2 - y1;
			float length = (float) Math.sqrt(dx * dx + dy * dy);
			float perpX = -dy / length * borderWidth;
			float perpY = dx / length * borderWidth;

			buffer.vertex(matrix, x1 + perpX, y1 + perpY, 0).color(outlineR, outlineG, outlineB, outlineAlpha).endVertex();
			buffer.vertex(matrix, x1 - perpX, y1 - perpY, 0).color(outlineR, outlineG, outlineB, outlineAlpha).endVertex();
			buffer.vertex(matrix, x2 + perpX, y2 + perpY, 0).color(outlineR, outlineG, outlineB, outlineAlpha).endVertex();
			buffer.vertex(matrix, x2 + perpX, y2 + perpY, 0).color(outlineR, outlineG, outlineB, outlineAlpha).endVertex();
			buffer.vertex(matrix, x1 - perpX, y1 - perpY, 0).color(outlineR, outlineG, outlineB, outlineAlpha).endVertex();
			buffer.vertex(matrix, x2 - perpX, y2 - perpY, 0).color(outlineR, outlineG, outlineB, outlineAlpha).endVertex();
		}

		tesselator.end();
		buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

		float lineR = auraRgb[0];
		float lineG = auraRgb[1];
		float lineB = auraRgb[2];
		float lineAlpha = 0.8f;

		for (int i = 0; i < 6; i++) {
			int next = (i + 1) % 6;
			buffer.vertex(matrix, pointsX[i], pointsY[i], 0).color(lineR, lineG, lineB, lineAlpha).endVertex();
			buffer.vertex(matrix, pointsX[next], pointsY[next], 0).color(lineR, lineG, lineB, lineAlpha).endVertex();
		}

		tesselator.end();
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}
}