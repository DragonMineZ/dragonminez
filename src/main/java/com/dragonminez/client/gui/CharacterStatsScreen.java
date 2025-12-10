package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.C2S.IncreaseStatC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
public class CharacterStatsScreen extends Screen {

    private static final ResourceLocation MENU_GRANDE = new ResourceLocation(Reference.MOD_ID,
            "textures/gui/menu/menugrande.png");
    private static final ResourceLocation MENU_PEQUENO = new ResourceLocation(Reference.MOD_ID,
            "textures/gui/menu/menupequeno.png");
    private static final ResourceLocation BUTTONS_TEXTURE = new ResourceLocation(Reference.MOD_ID,
            "textures/gui/buttons/characterbuttons.png");


    private StatsData statsData;
    private int tickCount = 0;
    private int tpMultiplier = 1;
    private final NumberFormat numberFormatter = NumberFormat.getInstance(Locale.US);

    private CustomTextureButton strButton;
    private CustomTextureButton skpButton;
    private CustomTextureButton resButton;
    private CustomTextureButton vitButton;
    private CustomTextureButton pwrButton;
    private CustomTextureButton eneButton;
    private CustomTextureButton multiplierButton;

    public CharacterStatsScreen() {
        super(Component.translatable("gui.dragonminez.character_stats.title"));
    }

    @Override
    protected void init() {
        super.init();
        updateStatsData();
        initStatButtons();
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
        this.renderBackground(graphics);
        renderPlayerModel(graphics, this.width / 2 + 5, this.height / 2 + 70, 75, mouseX, mouseY);


        if (statsData == null) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.dragonminez.character_stats.error"),
                    this.width / 2, this.height / 2, 0xFF0000);
            return;
        }

        renderMenuPanels(graphics);

       renderPlayerInfo(graphics, mouseX, mouseY);
        renderStatsInfo(graphics, mouseX, mouseY);
        renderStatisticsInfo(graphics, mouseX, mouseY);
        renderTPCost(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void initStatButtons() {
        if (statsData == null) return;

        int centerY = this.height / 2;
        int buttonX = 17;
        int startY = centerY + 2;

        int maxStats = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
        int availableTPs = statsData.getResources().getTrainingPoints();
        double multiplier = ConfigManager.getServerConfig().getGameplay().getTpCostMultiplier();
        int baseCost = (int) Math.round((statsData.getLevel() * multiplier) * multiplier * 1.5);

        int tpCost = statsData.calculateRecursiveCost(tpMultiplier, baseCost, maxStats, multiplier);

        multiplierButton = new CustomTextureButton.Builder()
                .position(buttonX - 3, startY + 75)
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

        int centerY = this.height / 2;
        int tpcY = centerY + 78;

        int maxStats = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
        double multiplier = ConfigManager.getServerConfig().getGameplay().getTpCostMultiplier();
        int baseCost = (int) Math.round((statsData.getLevel() * multiplier) * multiplier * 1.5);

        int tpCost = statsData.calculateRecursiveCost(tpMultiplier, baseCost, maxStats, multiplier);

        Component tpcValue = Component.literal(numberFormatter.format(tpCost));

        drawStringWithBorder2(graphics, tpcValue, 58, tpcY, 0xFFCE41, 0x000000);
        drawStringWithBorder2(graphics, Component.literal("x" + tpMultiplier), 95, tpcY, 0x2BFFE2, 0x000000);
    }

    private void renderMenuPanels(GuiGraphics graphics) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        graphics.blit(MENU_GRANDE, 2, centerY - 105, 0, 0, 146, 219, 256, 256);

        graphics.blit(MENU_PEQUENO, centerX - 70, 5, 0, 93, 145, 60, 256, 256);

        graphics.blit(MENU_GRANDE, this.width - 148, centerY - 105, 0, 0, 146, 219, 256, 256);

        RenderSystem.disableBlend();
    }

    private void renderPlayerInfo(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = this.width / 2;

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
        int centerY = this.height / 2;
        int titleY = centerY - 83;

        drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.character_stats.info").withStyle(style -> style.withBold(true)), 75, titleY, 0xFBC51C, 0x000000);

        int level = statsData.getLevel();
        int tps = statsData.getResources().getTrainingPoints();
        String characterClass = statsData.getCharacter().getCharacterClass();
        String form = statsData.getCharacter().getCurrentForm();
        if (form == null || form.isEmpty()) {
            form = "base";
        }

        int labelX = 25;
        int valueX = 70;
        int startY = centerY - 67;

        drawStringWithBorder2(graphics, Component.translatable("gui.dragonminez.character_stats.level").withStyle(style -> style.withBold(true)), labelX, startY, 0xD7FEF5, 0x000000);
        drawStringWithBorder2(graphics, Component.literal(numberFormatter.format(level)), valueX, startY, 0xFFFFFF, 0x000000);

        drawStringWithBorder2(graphics, Component.translatable("gui.dragonminez.character_stats.tps").withStyle(style -> style.withBold(true)), labelX, startY + 11, 0xD7FEF5, 0x000000);
        drawStringWithBorder2(graphics, Component.literal(numberFormatter.format(tps)), valueX, startY + 11, 0xFFE593, 0x000000);

        drawStringWithBorder2(graphics, Component.translatable("gui.dragonminez.character_stats.form").withStyle(style -> style.withBold(true)), labelX, startY + 22, 0xD7FEF5, 0x000000);
        drawStringWithBorder2(graphics, Component.translatable("forms.dragonminez." + form), valueX, startY + 22, 0xC7EAFC, 0x000000);

        drawStringWithBorder2(graphics, Component.translatable("gui.dragonminez.character_stats.class").withStyle(style -> style.withBold(true)), labelX, startY + 33, 0xD7FEF5, 0x000000);
        Component classComponent = Component.translatable("class.dragonminez." + characterClass);
        drawStringWithBorder2(graphics, classComponent, 70, startY + 33, 0xFFFFFF, 0x000000);

        int statsStartY = centerY - 16;
        drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.character_stats.stats"), 72, statsStartY, 0x68CCFF, 0x000000);

        String[] statNames = {"str", "skp", "res", "vit", "pwr", "ene"};
        int[] statValues = {
            statsData.getStats().getStrength(),
            statsData.getStats().getStrikePower(),
            statsData.getStats().getResistance(),
            statsData.getStats().getVitality(),
            statsData.getStats().getKiPower(),
            statsData.getStats().getEnergy()
        };

        int statY = centerY + 2;
        for (int i = 0; i < statNames.length; i++) {
            int statLabelX = 32;
            int yPos = statY + (i * 12);

            Component statComponent = Component.translatable("gui.dragonminez.character_stats." + statNames[i]).withStyle(style -> style.withBold(true));
            drawStringWithBorder2(graphics, statComponent, statLabelX, yPos, 0xD71432, 0x000000);
            drawStringWithBorder2(graphics, Component.literal(numberFormatter.format(statValues[i])), valueX, yPos, 0xFFD7AB, 0x000000);

            if (mouseX >= statLabelX && mouseX <= statLabelX + 25 && mouseY >= yPos && mouseY <= yPos + font.lineHeight) {
                List<FormattedCharSequence> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("gui.dragonminez.character_stats." + statNames[i] + ".desc").getVisualOrderText());
                graphics.renderTooltip(font, tooltip, mouseX, mouseY);
            }
        }

        Component tpcComponent = Component.translatable("gui.dragonminez.character_stats.tpc").withStyle(style -> style.withBold(true));
        drawStringWithBorder2(graphics, tpcComponent, 28, statY + 76, 0x2BFFE2, 0x000000);
    }

    private void renderStatisticsInfo(GuiGraphics graphics, int mouseX, int mouseY) {
        int rightX = this.width - 127;
        int centerY = this.height / 2;
        int titleY = centerY - 83;

        drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.character_stats.statistics").withStyle(style -> style.withBold(true)), this.width - 75, titleY, 0xF91E64, 0x000000);

        int labelStartY = centerY - 64;
        int valueX = this.width - 55;

        double meleeDamage = statsData.getMeleeDamage();
        double maxMeleeDamage = statsData.getMaxMeleeDamage();
        double strikeDamage = statsData.getStrikeDamage();
        double maxStrikeDamage = statsData.getMaxStrikeDamage();
        int stamina = statsData.getMaxStamina();
        double defense = statsData.getDefense();
        double maxDefense = statsData.getMaxDefense();
        int health = statsData.getMaxHealth();
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
            drawStringWithBorder2(graphics, labelComponent, rightX, yPos, 0xFFAA00, 0x000000);

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

        drawStringWithBorder(graphics, Component.literal(String.format(Locale.US, "%.1f", meleeDamage)), valueX + 15, labelStartY, 0xFFD7AB, 0x000000);
        drawStringWithBorder(graphics, Component.literal(String.format(Locale.US, "%.1f", strikeDamage)), valueX + 15, labelStartY + 12, 0xFFD7AB, 0x000000);
        drawStringWithBorder(graphics, Component.literal(numberFormatter.format(stamina)), valueX + 15, labelStartY + 24, 0xFFD7AB, 0x000000);
        drawStringWithBorder(graphics, Component.literal(String.format(Locale.US, "%.1f", defense)), valueX + 15, labelStartY + 36, 0xFFD7AB, 0x000000);
        drawStringWithBorder(graphics, Component.literal(numberFormatter.format(health)), valueX + 15, labelStartY + 48, 0xFFD7AB, 0x000000);
        drawStringWithBorder(graphics, Component.literal(String.format(Locale.US, "%.1f", kiDamage)), valueX + 15, labelStartY + 60, 0xFFD7AB, 0x000000);
        drawStringWithBorder(graphics, Component.literal(numberFormatter.format(energy)), valueX + 15, labelStartY + 72, 0xFFD7AB, 0x000000);

        int battlePower = statsData.getBattlePower();
        drawStringWithBorder2(graphics, Component.translatable("gui.dragonminez.character_stats.battle_power"), rightX, labelStartY + 102, 0xC51D1D, 0x000000);
        drawStringWithBorder(graphics, Component.literal(numberFormatter.format(battlePower)), valueX + 15, labelStartY + 102, 0xfebc0d, 0x000000);
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


    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

