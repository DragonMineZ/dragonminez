package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.SwitchButton;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.IncreaseStatC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

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

    private StatsData statsData;
    private int tickCount = 0;
    private int tpMultiplier = 1;
    private final NumberFormat numberFormatter = NumberFormat.getInstance(Locale.US);
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
    }

    @Override
    protected void init() {
        super.init();

        useHexagonView = ConfigManager.getUserConfig().getHud().isHexagonStatsDisplay();

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
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                this.statsData = data;
            });
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (!isAnimating()) this.renderBackground(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);
		applyZoom(graphics);

        renderPlayerModel(graphics, getUiWidth() / 2 + 5, getUiHeight() / 2 + 70, 75, uiMouseX, uiMouseY);
        renderMenuPanels(graphics);
        renderPlayerInfo(graphics, uiMouseX, uiMouseY);
        renderStatsInfo(graphics, uiMouseX, uiMouseY);
        renderStatisticsInfo(graphics, uiMouseX, uiMouseY);
        renderTPCost(graphics);

		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
    }

    private void initStatButtons() {
        if (statsData == null) return;

        int centerY = getUiHeight() / 2;
        int buttonX = 27;
        int startY = centerY - 4;

        int maxStats = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
        int availableTPs = statsData.getResources().getTrainingPoints();
        double multiplier = ConfigManager.getServerConfig().getGameplay().getTpCostMultiplier();
        int baseCost = (int) Math.round((statsData.getLevel() * multiplier) * multiplier * 1.5);

        int tpCost = statsData.calculateRecursiveCost(tpMultiplier, baseCost, maxStats, multiplier);

        multiplierButton = new CustomTextureButton.Builder()
                .position(buttonX, startY + 75)
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

        if (availableTPs >= tpCost && statsData.getStats().getStrength() < maxStats) {
            strButton = createStatButton(buttonX, startY, "STR");
            this.addRenderableWidget(strButton);
        }

        if (availableTPs >= tpCost && statsData.getStats().getStrikePower() < maxStats) {
            skpButton = createStatButton(buttonX, startY + 12, "SKP");
            this.addRenderableWidget(skpButton);
        }

        if (availableTPs >= tpCost && statsData.getStats().getResistance() < maxStats) {
            resButton = createStatButton(buttonX, startY + 24, "RES");
            this.addRenderableWidget(resButton);
        }

        if (availableTPs >= tpCost && statsData.getStats().getVitality() < maxStats) {
            vitButton = createStatButton(buttonX, startY + 36, "VIT");
            this.addRenderableWidget(vitButton);
        }

        if (availableTPs >= tpCost && statsData.getStats().getKiPower() < maxStats) {
            pwrButton = createStatButton(buttonX, startY + 48, "PWR");
            this.addRenderableWidget(pwrButton);
        }

        if (availableTPs >= tpCost && statsData.getStats().getEnergy() < maxStats) {
            eneButton = createStatButton(buttonX, startY + 60, "ENE");
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
                    NetworkHandler.sendToServer(new IncreaseStatC2S(statName, tpMultiplier));
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

    private void renderTPCost(GuiGraphics graphics) {
        if (statsData == null) return;

        int centerY = getUiHeight() / 2;
        int tpcY = centerY + 73;

        int maxStats = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
        double multiplier = ConfigManager.getServerConfig().getGameplay().getTpCostMultiplier();
        int baseCost = (int) Math.round((statsData.getLevel() * multiplier) * multiplier * 1.5);

        int tpCost = statsData.calculateRecursiveCost(tpMultiplier, baseCost, maxStats, multiplier);

        Component tpcValue = Component.literal(numberFormatter.format(tpCost));

        drawStringWithBorder2(graphics, tpcValue, 80, tpcY, 0xFFCE41, 0x000000);
        drawStringWithBorder2(graphics, Component.literal("x" + tpMultiplier), 80, tpcY + 10, 0x2BFFE2, 0x000000);
    }

    private void renderMenuPanels(GuiGraphics graphics) {
        int centerX = getUiWidth() / 2;
        int centerY = getUiHeight() / 2;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        graphics.blit(MENU_BIG, 12, centerY - 105, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, 29, centerY - 95, 142, 22, 107, 21, 256, 256);
		graphics.blit(MENU_BIG, 43, centerY - 28, 142, 0, 79, 21, 256, 256);

        graphics.blit(MENU_BIG, getUiWidth() - 158, centerY - 105, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, getUiWidth() - 141, centerY - 95, 142, 22, 107, 21, 256, 256);

		graphics.blit(MENU_SMALL, centerX - 70, 8, 0, 95, 145, 58, 256, 256);

		RenderSystem.disableBlend();
    }

    private void renderPlayerInfo(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = getUiWidth() / 2;

        if (Minecraft.getInstance().player == null) return;
        String playerName = Minecraft.getInstance().player.getName().getString();
        String raceName = statsData.getCharacter().getRaceName();
        String gender = statsData.getCharacter().getGender();
        int alignment = statsData.getResources().getAlignment();

        int nameColor = gender.equals("male") ? 0x63FFFF : 0xFF69B4;
        String genderSymbol = gender.equals("male") ? "♂" : "♀";

        Component nameBold = Component.literal(playerName).withStyle(style -> style.withBold(true));

        int totalWidth = font.width(nameBold) + font.width(" " + genderSymbol);
        int startX = centerX - (totalWidth / 2);

        graphics.drawString(font, nameBold, startX + 1, 19, 0x000000, false);
        graphics.drawString(font, nameBold, startX - 1, 19, 0x000000, false);
        graphics.drawString(font, nameBold, startX, 20, 0x000000, false);
        graphics.drawString(font, nameBold, startX, 18, 0x000000, false);
        graphics.drawString(font, nameBold, startX, 19, nameColor, false);

        int symbolX = startX + font.width(nameBold);
        graphics.drawString(font, " " + genderSymbol, symbolX + 1, 19, 0x000000, false);
        graphics.drawString(font, " " + genderSymbol, symbolX - 1, 19, 0x000000, false);
        graphics.drawString(font, " " + genderSymbol, symbolX, 20, 0x000000, false);
        graphics.drawString(font, " " + genderSymbol, symbolX, 18, 0x000000, false);
        graphics.drawString(font, " " + genderSymbol, symbolX, 19, nameColor, false);

        if (mouseX >= centerX - 40 && mouseX <= centerX + 40 && mouseY >= 19 && mouseY <= 19 + font.lineHeight) {
            List<FormattedCharSequence> tooltip = new ArrayList<>();
            if (alignment > 60) {
                tooltip.add(Component.translatable("gui.dragonminez.character_stats.alignment.good", alignment).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            } else if (alignment > 40) {
                tooltip.add(Component.translatable("gui.dragonminez.character_stats.alignment.neutral", alignment).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            } else {
                tooltip.add(Component.translatable("gui.dragonminez.character_stats.alignment.evil", alignment).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            }
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }

        Component raceComponent = Component.translatable("race.dragonminez." + raceName);
        drawStringWithBorder(graphics, raceComponent, centerX, 46, 0xFFFFFF, 0x000000);
    }

    private void renderStatsInfo(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerY = getUiHeight() / 2;
        int titleY = centerY - 88;

        drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.character_stats.info").withStyle(style -> style.withBold(true)), 85, titleY, 0xFBC51C, 0x000000);

        int level = statsData.getLevel();
        int tps = statsData.getResources().getTrainingPoints();
        String characterClass = statsData.getCharacter().getCharacterClass();
        String form = statsData.getCharacter().getActiveForm();

        int labelX = 30;
        int valueX = 70;
        int startY = centerY - 72;

        drawStringWithBorder2(graphics, Component.translatable("gui.dragonminez.character_stats.level").withStyle(style -> style.withBold(true)), labelX, startY, 0xD7FEF5, 0x000000);
        drawStringWithBorder2(graphics, Component.literal(numberFormatter.format(level)), valueX, startY, 0xFFFFFF, 0x000000);

        drawStringWithBorder2(graphics, Component.translatable("gui.dragonminez.character_stats.tps").withStyle(style -> style.withBold(true)), labelX, startY + 11, 0xD7FEF5, 0x000000);
        drawStringWithBorder2(graphics, Component.literal(numberFormatter.format(tps)), valueX, startY + 11, 0xFFE593, 0x000000);

        drawStringWithBorder2(graphics, Component.translatable("gui.dragonminez.character_stats.form").withStyle(style -> style.withBold(true)), labelX, startY + 22, 0xD7FEF5, 0x000000);
        Component formComponent;
		if (form == null || form.isEmpty()) formComponent = Component.translatable("race.dragonminez.base");
		else formComponent = Component.translatable("race.dragonminez." + statsData.getCharacter().getRaceName() + ".form." + statsData.getCharacter().getActiveFormGroup() + "." + form);
        drawStringWithBorder2(graphics, formComponent, valueX, startY + 22, 0xC7EAFC, 0x000000);

        if (!form.equals("base") && mouseX >= valueX && mouseX <= valueX + 60 && mouseY >= startY + 22 && mouseY <= startY + 22 + font.lineHeight) {
            String currentFormGroup = statsData.getCharacter().getActiveFormGroup();
            if (currentFormGroup != null && !currentFormGroup.isEmpty()) {
                var formConfig = ConfigManager.getFormGroup(statsData.getCharacter().getRaceName(), currentFormGroup);
                if (formConfig != null) {
                    var formData = formConfig.getForm(form);
                    if (formData != null) {
                        double mastery = statsData.getCharacter().getFormMasteries().getMastery(currentFormGroup, form);
                        double maxMastery = formData.getMaxMastery();

                        List<FormattedCharSequence> tooltip = new ArrayList<>();
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.form.mastery",
                            String.format(Locale.US, "%.2f", mastery),
                            String.format(Locale.US, "%.0f", maxMastery))
                            .withStyle(ChatFormatting.AQUA).getVisualOrderText());

                        graphics.renderTooltip(font, tooltip, mouseX, mouseY);
                    }
                }
            }
        }

        drawStringWithBorder2(graphics, Component.translatable("gui.dragonminez.character_stats.class").withStyle(style -> style.withBold(true)), labelX, startY + 33, 0xD7FEF5, 0x000000);
        Component classComponent = Component.translatable("class.dragonminez." + characterClass);
        drawStringWithBorder2(graphics, classComponent, valueX, startY + 33, 0xFFFFFF, 0x000000);

        int statsStartY = centerY - 21;
        drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.character_stats.stats"), 82, statsStartY, 0x68CCFF, 0x000000);

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
            double modifiedValue = baseValue * (1.0 + totalMult);

            Component statComponent = Component.translatable("gui.dragonminez.character_stats." + statNames[i]).withStyle(style -> style.withBold(true));
            drawStringWithBorder2(graphics, statComponent, statLabelX, yPos, 0xD71432, 0x000000);

            int statColor = totalMult > 0.1 ? 0xFFFF00 : 0xFFD7AB;
            String statText = totalMult > 0.1
                ? numberFormatter.format((int)modifiedValue) + " x" + String.format(Locale.US, "%.1f", 1.0 + totalMult)
                : numberFormatter.format(baseValue);

            drawStringWithBorder2(graphics, Component.literal(statText), valueX + 5, yPos, statColor, 0x000000);

            if (mouseX >= statLabelX && mouseX <= statLabelX + 25 && mouseY >= yPos && mouseY <= yPos + font.lineHeight) {
                List<FormattedCharSequence> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("gui.dragonminez.character_stats." + statNames[i] + ".desc").getVisualOrderText());

                if (totalMult > 0.1) {
                    tooltip.add(Component.literal("").getVisualOrderText());
                    tooltip.add(Component.translatable("gui.dragonminez.character_stats.base_value")
                        .append(": " + numberFormatter.format(baseValue))
                        .withStyle(ChatFormatting.GRAY).getVisualOrderText());
                    tooltip.add(Component.translatable("gui.dragonminez.character_stats.modified_value")
                        .append(": " + numberFormatter.format((int)modifiedValue))
                        .withStyle(ChatFormatting.YELLOW).getVisualOrderText());
                }

                var bonuses = statsData.getBonusStats().getBonuses(statNamesUpper[i]);
                if (!bonuses.isEmpty()) {
                    tooltip.add(Component.literal("").getVisualOrderText());
                    tooltip.add(Component.translatable("gui.dragonminez.character_stats.bonus")
                        .withStyle(ChatFormatting.AQUA).getVisualOrderText());
                    for (var bonus : bonuses) {
                        String bonusText = bonus.name + ": " + bonus.operation +
                            (bonus.operation.equals("*") ? String.format(Locale.US, "%.2f", bonus.value) : java.lang.String.format(Locale.US, "%.0f", bonus.value));
                        tooltip.add(Component.literal("  " + bonusText)
                            .withStyle(ChatFormatting.GREEN).getVisualOrderText());
                    }
                }

                graphics.renderTooltip(font, tooltip, mouseX, mouseY);
            }
        }

        Component tpcComponent = Component.translatable("gui.dragonminez.character_stats.tpc").withStyle(style -> style.withBold(true));
        drawStringWithBorder2(graphics, tpcComponent, 42, statY + 76, 0x2BFFE2, 0x000000);
    }

    private void renderStatisticsInfo(GuiGraphics graphics, int mouseX, int mouseY) {
        if (useHexagonView) {
            renderStatisticsInfoHexagon(graphics, mouseX, mouseY);
        } else {
            renderStatisticsInfoList(graphics, mouseX, mouseY);
        }
    }

    private void renderStatisticsInfoList(GuiGraphics graphics, int mouseX, int mouseY) {
        int rightX = getUiWidth() - 137;
        int centerY = getUiHeight() / 2;
        int titleY = centerY - 88;

        drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.character_stats.statistics").withStyle(style -> style.withBold(true)), getUiWidth() - 85, titleY, 0xF91E64, 0x000000);

        int labelStartY = centerY - 64;
        int valueX = getUiWidth() - 65;

        double meleeDamage = statsData.getMeleeDamage();
        double maxMeleeDamage = statsData.getMaxMeleeDamage();
        double strikeDamage = statsData.getStrikeDamage();
        double maxStrikeDamage = statsData.getMaxStrikeDamage();
        int stamina = statsData.getMaxStamina();
        double defense = statsData.getDefense();
        double maxDefense = statsData.getMaxDefense();
        double health = Minecraft.getInstance().player.getMaxHealth();
        double kiDamage = statsData.getKiDamage();
        double maxKiDamage = statsData.getMaxKiDamage();
        int energy = statsData.getMaxEnergy();

        double strScaling = statsData.getStatScaling("STR");
        double skpScaling = statsData.getStatScaling("SKP");
        double resScaling = statsData.getStatScaling("DEF");
        double vitScaling = statsData.getStatScaling("VIT");
        double pwrScaling = statsData.getStatScaling("PWR");
        double eneScaling = statsData.getStatScaling("ENE");
        double stmScaling = statsData.getStatScaling("STM");

        String[] labels = {
            "gui.dragonminez.character_stats.melee_damage",
            "gui.dragonminez.character_stats.strike_damage",
            "gui.dragonminez.character_stats.stamina",
            "gui.dragonminez.character_stats.defense",
            "gui.dragonminez.character_stats.health",
            "gui.dragonminez.character_stats.ki_damage",
            "gui.dragonminez.character_stats.max_energy"
        };

        for (int i = 0; i < labels.length; i++) {
            int yPos = labelStartY + (i * 12);
            Component labelComponent = Component.translatable(labels[i]);
            drawStringWithBorder2(graphics, labelComponent, rightX, yPos, 0x7CFDD6, 0x000000);

            if (mouseX >= rightX && mouseX <= rightX + 60 && mouseY >= yPos && mouseY <= yPos + font.lineHeight) {
                List<FormattedCharSequence> tooltip = new ArrayList<>();

                switch (i) {
                    case 0 -> {
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.melee_damage.tooltip1").getVisualOrderText());
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.melee_damage.tooltip2",
                            String.format(Locale.US, "%.2f", strScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.max_value",
                            String.format(Locale.US, "%.1f", maxMeleeDamage)).withStyle(ChatFormatting.GREEN).getVisualOrderText());
                    }
                    case 1 -> {
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.strike_damage.tooltip1").getVisualOrderText());
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.strike_damage.tooltip2",
                            String.format(Locale.US, "%.2f", skpScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.max_value",
                            String.format(Locale.US, "%.1f", maxStrikeDamage)).withStyle(ChatFormatting.GREEN).getVisualOrderText());
                    }
                    case 2 -> {
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.stamina.tooltip1").getVisualOrderText());
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.stamina.tooltip2",
                            String.format(Locale.US, "%.2f", stmScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
                    }
                    case 3 -> {
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.defense.tooltip1").getVisualOrderText());
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.defense.tooltip2",
                            String.format(Locale.US, "%.2f", resScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.max_value",
                            String.format(Locale.US, "%.1f", maxDefense)).withStyle(ChatFormatting.GREEN).getVisualOrderText());
                    }
                    case 4 -> {
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.health.tooltip1").getVisualOrderText());
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.health.tooltip2",
                            String.format(Locale.US, "%.2f", vitScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
                    }
                    case 5 -> {
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.ki_damage.tooltip1").getVisualOrderText());
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.ki_damage.tooltip2",
                            String.format(Locale.US, "%.2f", pwrScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.max_value",
                            String.format(Locale.US, "%.1f", maxKiDamage)).withStyle(ChatFormatting.GREEN).getVisualOrderText());
                    }
                    case 6 -> {
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.max_energy.tooltip1").getVisualOrderText());
                        tooltip.add(Component.translatable("gui.dragonminez.character_stats.max_energy.tooltip2",
                            String.format(Locale.US, "%.2f", eneScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
                    }
                }

                graphics.renderTooltip(font, tooltip, mouseX, mouseY);
            }
        }

        double strTotalMult = statsData.getTotalMultiplier("STR");
        double skpTotalMult = statsData.getTotalMultiplier("SKP");
        double resTotalMult = statsData.getTotalMultiplier("RES");
        double vitTotalMult = statsData.getTotalMultiplier("VIT");
        double pwrTotalMult = statsData.getTotalMultiplier("PWR");
        double eneTotalMult = statsData.getTotalMultiplier("ENE");

        int meleeDamageColor = strTotalMult > 0.1 ? 0xFFFF00 : 0xFFD7AB;
        int strikeDamageColor = skpTotalMult > 0.1 ? 0xFFFF00 : 0xFFD7AB;
        int staminaColor = resTotalMult > 0.1 ? 0xFFFF00 : 0xFFD7AB;
        int defenseColor = resTotalMult > 0.1 ? 0xFFFF00 : 0xFFD7AB;
        int healthColor = vitTotalMult > 0.1 ? 0xFFFF00 : 0xFFD7AB;
        int kiDamageColor = pwrTotalMult > 0.1 ? 0xFFFF00 : 0xFFD7AB;
        int energyColor = eneTotalMult > 0.1 ? 0xFFFF00 : 0xFFD7AB;

        drawStringWithBorder(graphics, Component.literal(java.lang.String.format(Locale.US, "%.1f", meleeDamage)), valueX + 15, labelStartY, meleeDamageColor, 0x000000);
        drawStringWithBorder(graphics, Component.literal(java.lang.String.format(Locale.US, "%.1f", strikeDamage)), valueX + 15, labelStartY + 12, strikeDamageColor, 0x000000);
        drawStringWithBorder(graphics, Component.literal(numberFormatter.format(stamina)), valueX + 15, labelStartY + 24, staminaColor, 0x000000);
        drawStringWithBorder(graphics, Component.literal(java.lang.String.format(Locale.US, "%.1f", defense)), valueX + 15, labelStartY + 36, defenseColor, 0x000000);
        drawStringWithBorder(graphics, Component.literal(numberFormatter.format(health)), valueX + 15, labelStartY + 48, healthColor, 0x000000);
        drawStringWithBorder(graphics, Component.literal(java.lang.String.format(Locale.US, "%.1f", kiDamage)), valueX + 15, labelStartY + 60, kiDamageColor, 0x000000);
        drawStringWithBorder(graphics, Component.literal(numberFormatter.format(energy)), valueX + 15, labelStartY + 72, energyColor, 0x000000);
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
        graphics.pose().translate(0.0D, 0.0D, 150.0D);
        InventoryScreen.renderEntityInInventory(graphics, x, y, scale, pose, cameraOrientation, player);
        graphics.pose().popPose();

        player.yBodyRot = yBodyRotO;
        player.setYRot(yRotO);
        player.setXRot(xRotO);
        player.yHeadRotO = yHeadRotO;
        player.yHeadRot = yHeadRot;
    }


    private void drawStringWithBorder(GuiGraphics graphics, Component text, int centerX, int y, int textColor) {
        drawStringWithBorder(graphics, text, centerX, y, textColor, 0x000000);
    }

    private void drawStringWithBorder(GuiGraphics graphics, Component text, int centerX, int y, int textColor, int borderColor) {
        int textWidth = font.width(text);
        int x = centerX - (textWidth / 2);

        String stripped = ChatFormatting.stripFormatting(text.getString());
        Component borderComponent = Component.literal(stripped != null ? stripped : text.getString());

        if (text.getStyle().isBold()) {
            borderComponent = borderComponent.copy().withStyle(style -> style.withBold(true));
        }

        graphics.drawString(font, borderComponent, x + 1, y, borderColor, false);
        graphics.drawString(font, borderComponent, x - 1, y, borderColor, false);
        graphics.drawString(font, borderComponent, x, y + 1, borderColor, false);
        graphics.drawString(font, borderComponent, x, y - 1, borderColor, false);

        graphics.drawString(font, text, x, y, textColor, false);
    }

    private void drawStringWithBorder2(GuiGraphics graphics, Component text, int x, int y, int textColor) {
        drawStringWithBorder2(graphics, text, x, y, textColor, 0x000000);
    }

    private void drawStringWithBorder2(GuiGraphics graphics, Component text, int x, int y, int textColor, int borderColor) {
        String stripped = ChatFormatting.stripFormatting(text.getString());
        Component borderComponent = Component.literal(stripped != null ? stripped : text.getString());

        if (text.getStyle().isBold()) {
            borderComponent = borderComponent.copy().withStyle(style -> style.withBold(true));
        }

        graphics.drawString(font, borderComponent, x + 1, y, borderColor, false);
        graphics.drawString(font, borderComponent, x - 1, y, borderColor, false);
        graphics.drawString(font, borderComponent, x, y + 1, borderColor, false);
        graphics.drawString(font, borderComponent, x, y - 1, borderColor, false);

        graphics.drawString(font, text, x, y, textColor, false);
    }

    private void initViewSwitchButton() {
        int centerY = getUiHeight() / 2;
        int buttonX = getUiWidth() - 45;
        int buttonY = centerY + 90;
		LivingEntity player = Minecraft.getInstance().player;

        viewSwitchButton = new SwitchButton(buttonX, buttonY, useHexagonView, Component.empty(), button -> {
            useHexagonView = !useHexagonView;
            ConfigManager.getUserConfig().getHud().setHexagonStatsDisplay(useHexagonView);
            ConfigManager.saveGeneralUserConfig();
            ((SwitchButton) button).toggle();
			if (useHexagonView) player.playSound(MainSounds.SWITCH_OFF.get());
			else player.playSound(MainSounds.SWITCH_ON.get());
        });
        this.addRenderableWidget(viewSwitchButton);
    }

    private void renderStatisticsInfoHexagon(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerY = getUiHeight() / 2;
        int titleY = centerY - 88;
        int centerX = getUiWidth() - 85;

        drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.character_stats.statistics").withStyle(style -> style.withBold(true)), centerX, titleY, 0xF91E64, 0x000000);

        double meleeDamage = statsData.getMeleeDamage();
        double maxMeleeDamage = statsData.getMaxMeleeDamage();
        double strikeDamage = statsData.getStrikeDamage();
        double maxStrikeDamage = statsData.getMaxStrikeDamage();
        int stamina = statsData.getMaxStamina();
        double defense = statsData.getDefense();
        double maxDefense = statsData.getMaxDefense();
		float health = statsData.getMaxHealth();
        double kiDamage = statsData.getKiDamage();
        double maxKiDamage = statsData.getMaxKiDamage();
        int energy = statsData.getMaxEnergy();

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

        int absoluteMaxStats = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
        float referenceValue;

        if (maxStatValue >= absoluteMaxStats * 0.9f) {
            referenceValue = absoluteMaxStats;
        } else {
            referenceValue = maxStatValue * 1.1f;
        }

        float[] statRadii = new float[6];
        statRadii[0] = maxRadius * ((float) strValue / referenceValue);
        statRadii[1] = maxRadius * ((float) resValue / referenceValue);
        statRadii[2] = maxRadius * ((float) eneValue / referenceValue);
        statRadii[3] = maxRadius * ((float) vitValue / referenceValue);
        statRadii[4] = maxRadius * ((float) pwrValue / referenceValue);
        statRadii[5] = maxRadius * ((float) skpValue / referenceValue);

        float[] hexPointsX = new float[6];
        float[] hexPointsY = new float[6];
        float[] hexPointsMaxX = new float[6];
        float[] hexPointsMaxY = new float[6];

        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i - 90);
            hexPointsX[i] = centerX + (float)(statRadii[i] * Math.cos(angle));
            hexPointsY[i] = hexCenterY + (float)(statRadii[i] * Math.sin(angle));

            hexPointsMaxX[i] = centerX + (float)(maxRadius * Math.cos(angle));
            hexPointsMaxY[i] = hexCenterY + (float)(maxRadius * Math.sin(angle));
        }

        drawHexagon(graphics, centerX, hexCenterY, hexPointsX, hexPointsY, hexPointsMaxX, hexPointsMaxY);

        float textOffset = 10.0f;

        int strX = (int)(centerX + (maxRadius + textOffset) * Math.cos(Math.toRadians(-90)));
        int strY = (int)(hexCenterY + (maxRadius + textOffset) * Math.sin(Math.toRadians(-90)));

        int resX = (int)(centerX + (maxRadius + textOffset) * Math.cos(Math.toRadians(-30)));
        int resY = (int)(hexCenterY + (maxRadius + textOffset) * Math.sin(Math.toRadians(-30)));

        int eneX = (int)(centerX + (maxRadius + textOffset) * Math.cos(Math.toRadians(30)));
        int eneY = (int)(hexCenterY + (maxRadius + textOffset) * Math.sin(Math.toRadians(30)));

        int vitX = (int)(centerX + (maxRadius + textOffset) * Math.cos(Math.toRadians(90)));
        int vitY = (int)(hexCenterY + (maxRadius + textOffset) * Math.sin(Math.toRadians(90)));

        int pwrX = (int)(centerX + (maxRadius + textOffset) * Math.cos(Math.toRadians(150)));
        int pwrY = (int)(hexCenterY + (maxRadius + textOffset) * Math.sin(Math.toRadians(150)));

        int skpX = (int)(centerX + (maxRadius + textOffset) * Math.cos(Math.toRadians(210)));
        int skpY = (int)(hexCenterY + (maxRadius + textOffset) * Math.sin(Math.toRadians(210)));

        Component strComponent = Component.translatable("gui.dragonminez.character_stats.str").withStyle(style -> style.withBold(true));
        drawStringWithBorder(graphics, strComponent, strX, strY, 0xD71432, 0x000000);

        Component skpComponent = Component.translatable("gui.dragonminez.character_stats.skp").withStyle(style -> style.withBold(true));
        drawStringWithBorder(graphics, skpComponent, skpX, skpY, 0xD71432, 0x000000);

        Component resComponent = Component.translatable("gui.dragonminez.character_stats.res").withStyle(style -> style.withBold(true));
        drawStringWithBorder(graphics, resComponent, resX, resY, 0xD71432, 0x000000);

        Component pwrComponent = Component.translatable("gui.dragonminez.character_stats.pwr").withStyle(style -> style.withBold(true));
        drawStringWithBorder(graphics, pwrComponent, pwrX, pwrY, 0xD71432, 0x000000);

        Component eneComponent = Component.translatable("gui.dragonminez.character_stats.ene").withStyle(style -> style.withBold(true));
        drawStringWithBorder(graphics, eneComponent, eneX, eneY, 0xD71432, 0x000000);

        Component vitComponent = Component.translatable("gui.dragonminez.character_stats.vit").withStyle(style -> style.withBold(true));
        drawStringWithBorder(graphics, vitComponent, vitX, vitY, 0xD71432, 0x000000);

        int strTextWidth = font.width(strComponent);
        int skpTextWidth = font.width(skpComponent);
        int resTextWidth = font.width(resComponent);
        int pwrTextWidth = font.width(pwrComponent);
        int eneTextWidth = font.width(eneComponent);
        int vitTextWidth = font.width(vitComponent);

        if (mouseX >= strX - strTextWidth/2 && mouseX <= strX + strTextWidth/2 && mouseY >= strY && mouseY <= strY + font.lineHeight) {
            List<FormattedCharSequence> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.melee_damage.tooltip1").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.melee_damage.tooltip2",
                String.format(Locale.US, "%.2f", strScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.max_value",
                String.format(Locale.US, "%.1f", maxMeleeDamage)).withStyle(ChatFormatting.GREEN).getVisualOrderText());
            tooltip.add(Component.literal("").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.melee_damage").append(": ")
                .append(Component.literal(String.format(Locale.US, "%.1f", meleeDamage)))
                .withStyle(ChatFormatting.AQUA).getVisualOrderText());
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }

        if (mouseX >= skpX - skpTextWidth/2 && mouseX <= skpX + skpTextWidth/2 && mouseY >= skpY && mouseY <= skpY + font.lineHeight) {
            List<FormattedCharSequence> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.strike_damage.tooltip1").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.strike_damage.tooltip2",
                String.format(Locale.US, "%.2f", skpScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.max_value",
                String.format(Locale.US, "%.1f", maxStrikeDamage)).withStyle(ChatFormatting.GREEN).getVisualOrderText());
            tooltip.add(Component.literal("").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.strike_damage").append(": ")
                .append(Component.literal(String.format(Locale.US, "%.1f", strikeDamage)))
                .withStyle(ChatFormatting.AQUA).getVisualOrderText());
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }

        if (mouseX >= resX - resTextWidth/2 && mouseX <= resX + resTextWidth/2 && mouseY >= resY && mouseY <= resY + font.lineHeight) {
            List<FormattedCharSequence> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.defense.tooltip1").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.defense.tooltip2",
                String.format(Locale.US, "%.2f", resScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.max_value",
                String.format(Locale.US, "%.1f", maxDefense)).withStyle(ChatFormatting.GREEN).getVisualOrderText());
            tooltip.add(Component.literal("").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.stamina.tooltip1").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.stamina.tooltip2",
                String.format(Locale.US, "%.2f", resScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            tooltip.add(Component.literal("").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.defense").append(": ")
                .append(Component.literal(String.format(Locale.US, "%.1f", defense)))
                .withStyle(ChatFormatting.AQUA).getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.stamina").append(": ")
                .append(Component.literal(numberFormatter.format(stamina)))
                .withStyle(ChatFormatting.AQUA).getVisualOrderText());
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }

        if (mouseX >= pwrX - pwrTextWidth/2 && mouseX <= pwrX + pwrTextWidth/2 && mouseY >= pwrY && mouseY <= pwrY + font.lineHeight) {
            List<FormattedCharSequence> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.ki_damage.tooltip1").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.ki_damage.tooltip2",
                String.format(Locale.US, "%.2f", pwrScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.max_value",
                String.format(Locale.US, "%.1f", maxKiDamage)).withStyle(ChatFormatting.GREEN).getVisualOrderText());
            tooltip.add(Component.literal("").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.ki_damage").append(": ")
                .append(Component.literal(String.format(Locale.US, "%.1f", kiDamage)))
                .withStyle(ChatFormatting.AQUA).getVisualOrderText());
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }

        if (mouseX >= eneX - eneTextWidth/2 && mouseX <= eneX + eneTextWidth/2 && mouseY >= eneY && mouseY <= eneY + font.lineHeight) {
            List<FormattedCharSequence> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.max_energy.tooltip1").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.max_energy.tooltip2",
                String.format(Locale.US, "%.2f", eneScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            tooltip.add(Component.literal("").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.max_energy").append(": ")
                .append(Component.literal(numberFormatter.format(energy)))
                .withStyle(ChatFormatting.AQUA).getVisualOrderText());
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }

        if (mouseX >= vitX - vitTextWidth/2 && mouseX <= vitX + vitTextWidth/2 && mouseY >= vitY && mouseY <= vitY + font.lineHeight) {
            List<FormattedCharSequence> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.health.tooltip1").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.health.tooltip2",
                String.format(Locale.US, "%.2f", vitScaling)).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            tooltip.add(Component.literal("").getVisualOrderText());
            tooltip.add(Component.translatable("gui.dragonminez.character_stats.health").append(": ")
                .append(Component.literal(numberFormatter.format(health)))
                .withStyle(ChatFormatting.AQUA).getVisualOrderText());
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
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
