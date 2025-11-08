package com.dragonminez.client.gui;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CharacterStatsScreen extends Screen {

    private StatsData statsData;
    private int tickCount = 0;

    public CharacterStatsScreen() {
        super(Component.translatable("gui.dragonminez.character_stats.title"));
    }

    @Override
    protected void init() {
        super.init();
        updateStatsData();
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;

        if (tickCount >= 10) {
            tickCount = 0;
            updateStatsData();
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

        if (statsData == null) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.dragonminez.character_stats.error"),
                    this.width / 2, this.height / 2, 0xFF0000);
            return;
        }

        int centerX = this.width / 2;
        int startY = 40;
        int leftX = centerX - 150;
        int rightX = centerX + 50;

        graphics.drawCenteredString(this.font, this.title, centerX, 20, 0xFFFFFF);

        graphics.drawString(this.font, Component.translatable("gui.dragonminez.character_stats.base_stats").getString(), leftX, startY, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("gui.dragonminez.character_stats.output").getString(), rightX, startY, 0xFFFFFF);

        int yOffset = startY + 20;

        drawStat(graphics, leftX, rightX, yOffset, "STR", statsData.getStats().getStrength(),
                Component.translatable("gui.dragonminez.character_stats.physical_damage").getString(), (int)statsData.getAttackDamage());
        yOffset += 20;

        drawStat(graphics, leftX, rightX, yOffset, "SKP", statsData.getStats().getStrikePower(),
                Component.translatable("gui.dragonminez.character_stats.strike_power").getString(), (int)statsData.getAttackDamage());
        yOffset += 20;

        drawStat(graphics, leftX, rightX, yOffset, "RES", statsData.getStats().getResistance(),
                Component.translatable("gui.dragonminez.character_stats.defense").getString(), (int)statsData.getDefense());
        yOffset += 20;

        drawStat(graphics, leftX, rightX, yOffset, "VIT", statsData.getStats().getVitality(),
                Component.translatable("gui.dragonminez.character_stats.max_health").getString(), statsData.getMaxHealth());
        yOffset += 20;

        drawStat(graphics, leftX, rightX, yOffset, "PWR", statsData.getStats().getKiPower(),
                Component.translatable("gui.dragonminez.character_stats.ki_damage").getString(), (int)statsData.getKiDamage());
        yOffset += 20;

        drawStat(graphics, leftX, rightX, yOffset, "ENE", statsData.getStats().getEnergy(),
                Component.translatable("gui.dragonminez.character_stats.max_energy").getString(), statsData.getMaxEnergy());
        yOffset += 20;

        graphics.drawString(this.font, "§7" + Component.translatable("gui.dragonminez.character_stats.max_stamina").getString() + ": §2" + statsData.getMaxStamina(),
                leftX, yOffset, 0xFFFFFF);
        yOffset += 30;

        graphics.drawString(this.font, "§l§e═══════════════════════════",
                leftX, yOffset, 0xFFFFFF);
        yOffset += 15;

        graphics.drawString(this.font, "§6" + Component.translatable("gui.dragonminez.character_stats.level").getString() + ": §f" + statsData.getLevel(),
                leftX, yOffset, 0xFFFFFF);
        graphics.drawString(this.font, "§6" + Component.translatable("gui.dragonminez.character_stats.battle_power").getString() + ": §f" + statsData.getBattlePower(),
                rightX, yOffset, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawStat(GuiGraphics graphics, int leftX, int rightX, int y,
                          String statName, int statValue, String outputName, int outputValue) {
        graphics.drawString(this.font, "§e" + statName + ": §f" + statValue, leftX, y, 0xFFFFFF);
        graphics.drawString(this.font, "§a" + outputName + ": §f" + outputValue, rightX, y, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

