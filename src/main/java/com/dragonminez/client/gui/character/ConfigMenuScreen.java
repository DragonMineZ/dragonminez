package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.SwitchButton;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralUserConfig;
import com.dragonminez.common.init.MainSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ConfigMenuScreen extends BaseMenuScreen {

    private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
            "textures/gui/menu/menubig.png");
    private static final ResourceLocation STAT_BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
            "textures/gui/buttons/characterbuttons.png");

    private static final int CONFIG_ITEM_HEIGHT = 20;
    private static final int MAX_VISIBLE_CONFIGS = 7;

    private int tickCount = 0;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private GeneralUserConfig.HudConfig hudConfig;
    private final List<ConfigOption> configOptions = new ArrayList<>();
    private final List<CustomTextureButton> decreaseButtons = new ArrayList<>();
    private final List<CustomTextureButton> increaseButtons = new ArrayList<>();
    private final List<SwitchButton> switchButtons = new ArrayList<>();

    public ConfigMenuScreen(int oldGuiScale) {
        super(Component.translatable("gui.dragonminez.config.title"), oldGuiScale);
    }

    @Override
    protected void init() {
        super.init();
        loadConfig();
        initializeConfigOptions();
        initConfigButtons();
        updateConfigsList();
    }

    private void loadConfig() {
        hudConfig = ConfigManager.getUserConfig().getHud();
    }

    private void initializeConfigOptions() {
        configOptions.clear();

        configOptions.add(new ConfigOption("config.xenoverseHudPosX",
            ConfigType.INT, hudConfig.getXenoverseHudPosX(), -1000, 2000,
            v -> hudConfig.setXenoverseHudPosX(v.intValue())));

        configOptions.add(new ConfigOption("config.xenoverseHudPosY",
            ConfigType.INT, hudConfig.getXenoverseHudPosY(), -1000, 2000,
            v -> hudConfig.setXenoverseHudPosY(v.intValue())));

        configOptions.add(new ConfigOption("config.advancedDescription",
            ConfigType.BOOLEAN, hudConfig.isAdvancedDescription() ? 1 : 0, 0, 1,
            v -> hudConfig.setAdvancedDescription(v > 0)));

        configOptions.add(new ConfigOption("config.advancedDescriptionPercentage",
            ConfigType.BOOLEAN, hudConfig.isAdvancedDescriptionPercentage() ? 1 : 0, 0, 1,
            v -> hudConfig.setAdvancedDescriptionPercentage(v > 0)));

        configOptions.add(new ConfigOption("config.alternativeHud",
            ConfigType.BOOLEAN, hudConfig.isAlternativeHud() ? 1 : 0, 0, 1,
            v -> hudConfig.setAlternativeHud(v > 0)));

        configOptions.add(new ConfigOption("config.hexagonStatsDisplay",
            ConfigType.BOOLEAN, hudConfig.isHexagonStatsDisplay() ? 1 : 0, 0, 1,
            v -> hudConfig.setHexagonStatsDisplay(v > 0)));

        configOptions.add(new ConfigOption("config.healthBarPosX",
            ConfigType.INT, hudConfig.getHealthBarPosX(), -1000, 2000,
            v -> hudConfig.setHealthBarPosX(v.intValue())));

        configOptions.add(new ConfigOption("config.healthBarPosY",
            ConfigType.INT, hudConfig.getHealthBarPosY(), -1000, 2000,
            v -> hudConfig.setHealthBarPosY(v.intValue())));

        configOptions.add(new ConfigOption("config.energyBarPosX",
            ConfigType.INT, hudConfig.getEnergyBarPosX(), -1000, 2000,
            v -> hudConfig.setEnergyBarPosX(v.intValue())));

        configOptions.add(new ConfigOption("config.energyBarPosY",
            ConfigType.INT, hudConfig.getEnergyBarPosY(), -1000, 2000,
            v -> hudConfig.setEnergyBarPosY(v.intValue())));

        configOptions.add(new ConfigOption("config.staminaBarPosX",
            ConfigType.INT, hudConfig.getStaminaBarPosX(), -1000, 2000,
            v -> hudConfig.setStaminaBarPosX(v.intValue())));

        configOptions.add(new ConfigOption("config.staminaBarPosY",
            ConfigType.INT, hudConfig.getStaminaBarPosY(), -1000, 2000,
            v -> hudConfig.setStaminaBarPosY(v.intValue())));

        configOptions.add(new ConfigOption("config.isStoryHardDifficulty",
                ConfigType.BOOLEAN, hudConfig.isStoryHardDifficulty() ? 1 : 0, 0, 1,
                v -> hudConfig.setStoryHardDifficulty(v > 0)));

		configOptions.add(new ConfigOption("config.cameraMovementDurintFlight",
				ConfigType.BOOLEAN, hudConfig.isCameraMovementDurintFlight() ? 1 : 0, 0, 1,
				v -> hudConfig.setCameraMovementDurintFlight(v > 0)));
    }

    private void initConfigButtons() {
        clearConfigButtons();
		LivingEntity player = this.minecraft.player;

        int rightPanelX = this.width - 163;
        int centerY = this.height / 2;
        int rightPanelY = centerY - 105;
        int startY = rightPanelY + 35;
        int visibleStart = scrollOffset;
        int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_CONFIGS, configOptions.size());

        for (int i = visibleStart; i < visibleEnd; i++) {
            ConfigOption option = configOptions.get(i);
            int itemY = startY + ((i - visibleStart) * CONFIG_ITEM_HEIGHT);
            final int index = i;

            if (option.type == ConfigType.BOOLEAN) {
                boolean isOn = option.value > 0;
                int switchX = rightPanelX + 65;
                int switchY = itemY + 3;

                SwitchButton switchBtn = new SwitchButton(switchX, switchY, isOn, Component.empty(), button -> {
                    modifyConfigValue(index, 1);
                    ((SwitchButton) button).toggle();
					if (isOn) player.playSound(MainSounds.SWITCH_OFF.get());
					else player.playSound(MainSounds.SWITCH_ON.get());
                });
                switchButtons.add(switchBtn);
                this.addRenderableWidget(switchBtn);

            } else {
                CustomTextureButton decreaseBtn = new CustomTextureButton.Builder()
                        .position(rightPanelX + 25, itemY + 3)
                        .size(14, 11)
                        .texture(STAT_BUTTONS)
                        .textureCoords(142, 0, 142, 10)
                        .textureSize(10, 10)
                        .onPress(button -> modifyConfigValue(index, -1))
                        .build();
                decreaseButtons.add(decreaseBtn);
                this.addRenderableWidget(decreaseBtn);

                CustomTextureButton increaseBtn = new CustomTextureButton.Builder()
                        .position(rightPanelX + 108, itemY + 3)
                        .size(14, 11)
                        .texture(STAT_BUTTONS)
                        .textureCoords(0, 0, 0, 10)
                        .textureSize(10, 10)
                        .onPress(button -> modifyConfigValue(index, 1))
                        .build();
                increaseButtons.add(increaseBtn);
                this.addRenderableWidget(increaseBtn);
            }
        }
    }

    private void clearConfigButtons() {
        for (CustomTextureButton btn : decreaseButtons) {
            this.removeWidget(btn);
        }
        for (CustomTextureButton btn : increaseButtons) {
            this.removeWidget(btn);
        }
        for (SwitchButton btn : switchButtons) {
            this.removeWidget(btn);
        }
        decreaseButtons.clear();
        increaseButtons.clear();
        switchButtons.clear();
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;
    }

    private void updateConfigsList() {
        maxScroll = Math.max(0, configOptions.size() - MAX_VISIBLE_CONFIGS);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        renderPlayerModel(graphics, this.width / 2 + 5, this.height / 2 + 70, 75, mouseX, mouseY);

        renderLeftPanel(graphics, mouseX, mouseY);
        renderRightPanel(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderLeftPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int leftPanelX = 12;
        int centerY = this.height / 2;
        int leftPanelY = centerY - 105;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(MENU_BIG, 12, centerY - 105, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, 29, centerY - 95, 142, 22, 107, 21, 256, 256);

        drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.config.options").withStyle(ChatFormatting.BOLD),
                leftPanelX + 70, leftPanelY + 17, 0xFFFFD700);

        renderConfigsList(graphics, leftPanelX, leftPanelY, mouseX, mouseY);
    }

    private void renderConfigsList(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
        int startY = panelY + 35;
        int visibleStart = scrollOffset;
        int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_CONFIGS, configOptions.size());

        graphics.enableScissor(panelX + 5, startY, panelX + 144, startY + (MAX_VISIBLE_CONFIGS * CONFIG_ITEM_HEIGHT));

        graphics.pose().pushPose();
        graphics.pose().scale(0.75f, 0.75f, 0.75f);

        for (int i = visibleStart; i < visibleEnd; i++) {
            ConfigOption option = configOptions.get(i);
            int itemY = startY + ((i - visibleStart) * CONFIG_ITEM_HEIGHT);

            String displayName = Component.translatable("gui.dragonminez." + option.key).getString();

            drawStringWithBorder(graphics, Component.literal(displayName),
                    (int)((panelX + 15) / 0.75f), (int)(itemY / 0.75f) + 6, 0xFFFFFFFF);
        }

        graphics.pose().popPose();
        graphics.disableScissor();

        if (maxScroll > 0) {
            int scrollBarX = panelX + 140;
            int scrollBarHeight = MAX_VISIBLE_CONFIGS * CONFIG_ITEM_HEIGHT;
            int totalItems = configOptions.size();

            graphics.fill(scrollBarX, startY, scrollBarX + 3, startY + scrollBarHeight, 0xFF333333);

            float scrollPercent = (float) scrollOffset / maxScroll;
            float visiblePercent = (float) MAX_VISIBLE_CONFIGS / totalItems;
            int indicatorHeight = Math.max(20, (int)(scrollBarHeight * visiblePercent));
            int indicatorY = startY + (int)((scrollBarHeight - indicatorHeight) * scrollPercent);

            graphics.fill(scrollBarX, indicatorY, scrollBarX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
        }
    }

    private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int rightPanelX = this.width - 158;
        int centerY = this.height / 2;
        int rightPanelY = centerY - 105;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(MENU_BIG, this.width - 158, centerY - 105, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, this.width - 141, centerY - 95, 142, 22, 107, 21, 256, 256);

        drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.config.values").withStyle(ChatFormatting.BOLD),
                rightPanelX + 70, rightPanelY + 17, 0xFFFFD700);

        renderConfigValues(graphics, rightPanelX, rightPanelY);
    }

    private void renderConfigValues(GuiGraphics graphics, int panelX, int panelY) {
        int startY = panelY + 35;
        int visibleStart = scrollOffset;
        int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_CONFIGS, configOptions.size());

        for (int i = visibleStart; i < visibleEnd; i++) {
            ConfigOption option = configOptions.get(i);
            int itemY = startY + ((i - visibleStart) * CONFIG_ITEM_HEIGHT);

            if (option.type != ConfigType.BOOLEAN) {
                String valueText;
                if (option.type == ConfigType.FLOAT) {
                    valueText = String.format("%.2f", option.value);
                } else {
                    valueText = String.valueOf((int)option.value);
                }

                graphics.pose().pushPose();
                graphics.pose().scale(0.75f, 0.75f, 0.75f);
                drawCenteredStringWithBorder(graphics, Component.literal(valueText),
                        (int)((panelX + 69) / 0.75f), (int)((itemY + 5) / 0.75f), 0xFFFFFFFF);
                graphics.pose().popPose();
            }
        }
    }

    private void modifyConfigValue(int index, int delta) {
        if (index < 0 || index >= configOptions.size()) return;

        ConfigOption option = configOptions.get(index);
        boolean isShiftDown = Screen.hasShiftDown();

        if (option.type == ConfigType.BOOLEAN) {
            option.value = option.value > 0 ? 0 : 1;
        } else if (option.type == ConfigType.INT) {
            int step = isShiftDown ? 5 : 1;
            option.value = Math.max(option.min, Math.min(option.max, option.value + (delta * step)));
        } else if (option.type == ConfigType.FLOAT) {
            float step = isShiftDown ? 1.0f : 0.1f;
            option.value = Math.max(option.min, Math.min(option.max, option.value + (delta * step)));
        }

        option.setter.accept(option.value);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int leftPanelX = 12;
        int centerY = this.height / 2;
        int leftPanelY = centerY - 105;

        if (mouseX >= leftPanelX && mouseX <= leftPanelX + 148 &&
                mouseY >= leftPanelY + 40 && mouseY <= leftPanelY + 219) {

            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)delta));
            initConfigButtons();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void renderPlayerModel(GuiGraphics graphics, int x, int y, int scale, float mouseX, float mouseY) {
        LivingEntity player = this.minecraft.player;
        if (player == null) return;

        float xRotation = (float) Math.atan((y - mouseY) / 40.0F);
        float yRotation = (float) Math.atan((x - mouseX) / 40.0F);

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

    private void drawStringWithBorder(GuiGraphics graphics, Component text, int x, int y, int textColor) {
        int borderColor = 0xFF000000;
        graphics.drawString(this.font, text, x + 1, y, borderColor, false);
        graphics.drawString(this.font, text, x - 1, y, borderColor, false);
        graphics.drawString(this.font, text, x, y + 1, borderColor, false);
        graphics.drawString(this.font, text, x, y - 1, borderColor, false);
        graphics.drawString(this.font, text, x, y, textColor, false);
    }

    private void drawCenteredStringWithBorder(GuiGraphics graphics, Component text, int centerX, int y, int textColor) {
        int textWidth = this.font.width(text);
        int x = centerX - (textWidth / 2);
        drawStringWithBorder(graphics, text, x, y, textColor);
    }

    @Override
    public void removed() {
        if (this.minecraft != null) {
            ConfigManager.saveGeneralUserConfig();
        }
        super.removed();
    }

    private enum ConfigType {
        INT, FLOAT, BOOLEAN
    }

    private static class ConfigOption {
        String key;
        ConfigType type;
        float value;
        float min;
        float max;
        java.util.function.Consumer<Float> setter;

        ConfigOption(String key, ConfigType type, float value, float min, float max, java.util.function.Consumer<Float> setter) {
            this.key = key;
            this.type = type;
            this.value = value;
            this.min = min;
            this.max = max;
            this.setter = setter;
        }
    }
}
