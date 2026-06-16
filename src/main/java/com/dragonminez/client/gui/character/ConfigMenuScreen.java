package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.crowdin.CrowdinManager;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.SwitchButton;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralUserConfig;
import com.dragonminez.common.init.MainSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
	private boolean isDraggingScroll = false;
	private int holdTicks = 0;
	private int heldConfigIndex = -1;
	private int heldDelta = 0;

	private GeneralUserConfig userConfig;
	private final List<ConfigOption> configOptions = new ArrayList<>();
	private final List<CustomTextureButton> decreaseButtons = new ArrayList<>();
	private final List<CustomTextureButton> increaseButtons = new ArrayList<>();
	private final List<SwitchButton> switchButtons = new ArrayList<>();

	public ConfigMenuScreen() {
		super(Component.translatable("gui.dragonminez.config.title"));
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
		userConfig = ConfigManager.getUserConfig();
	}

	private void initializeConfigOptions() {
		configOptions.clear();

		configOptions.add(new ConfigOption("config.firstPersonAnimated",
				ConfigType.BOOLEAN, userConfig.getFirstPersonAnimated() ? 1 : 0, 0, 1,
				v -> userConfig.setFirstPersonAnimated(v > 0)));
		
		configOptions.add(new ConfigOption("config.impactFramesEnabled",
				ConfigType.BOOLEAN, userConfig.isImpactFramesEnabled() ? 1 : 0, 0, 1,
				v -> userConfig.setImpactFramesEnabled(v > 0)));

		configOptions.add(new ConfigOption("config.showAccumulativeDamage",
				ConfigType.BOOLEAN, userConfig.getShowAccumulativeDamage() ? 1 : 0, 0, 1,
				v -> userConfig.setShowAccumulativeDamage(v > 0)));

		configOptions.add(new ConfigOption("config.techniqueHotbarRightSide",
				ConfigType.BOOLEAN, userConfig.getTechniqueHotbarRightSide() ? 1 : 0, 0, 1,
				v -> userConfig.setTechniqueHotbarRightSide(v > 0)));

		configOptions.add(new ConfigOption("config.alwaysVisibleHudValues",
				ConfigType.BOOLEAN, userConfig.getAlwaysVisibleHudValues() ? 1 : 0, 0, 1,
				v -> userConfig.setAlwaysVisibleHudValues(v > 0)));

		configOptions.add(new ConfigOption("config.hideHudNumbers",
				ConfigType.BOOLEAN, userConfig.getHideHudNumbers() ? 1 : 0, 0, 1,
				v -> userConfig.setHideHudNumbers(v > 0)));

		configOptions.add(new ConfigOption("config.xenoverseHudPosX",
				ConfigType.INT, userConfig.getXenoverseHudPosX(), -1000, 2000,
				v -> userConfig.setXenoverseHudPosX(v.intValue())));

		configOptions.add(new ConfigOption("config.xenoverseHudPosY",
				ConfigType.INT, userConfig.getXenoverseHudPosY(), -1000, 2000,
				v -> userConfig.setXenoverseHudPosY(v.intValue())));

		configOptions.add(new ConfigOption("config.advancedDescription",
				ConfigType.BOOLEAN, userConfig.getAdvancedDescription() ? 1 : 0, 0, 1,
				v -> userConfig.setAdvancedDescription(v > 0)));

		configOptions.add(new ConfigOption("config.advancedDescriptionPercentage",
				ConfigType.BOOLEAN, userConfig.getAdvancedDescriptionPercentage() ? 1 : 0, 0, 1,
				v -> userConfig.setAdvancedDescriptionPercentage(v > 0)));

		configOptions.add(new ConfigOption("config.alternativeHud",
				ConfigType.BOOLEAN, userConfig.getAlternativeHud() ? 1 : 0, 0, 1,
				v -> userConfig.setAlternativeHud(v > 0)));

		configOptions.add(new ConfigOption("config.hexagonStatsDisplay",
				ConfigType.BOOLEAN, userConfig.getHexagonStatsDisplay() ? 1 : 0, 0, 1,
				v -> userConfig.setHexagonStatsDisplay(v > 0)));

		configOptions.add(new ConfigOption("config.menuScaleMultiplier",
				ConfigType.FLOAT, userConfig.getMenuScaleMultiplier(), 0.75f, 2.5f,
				v -> userConfig.setMenuScaleMultiplier(v)));

		configOptions.add(new ConfigOption("config.healthBarPosX",
				ConfigType.INT, userConfig.getHealthBarPosX(), -1000, 2000,
				v -> userConfig.setHealthBarPosX(v.intValue())));

		configOptions.add(new ConfigOption("config.healthBarPosY",
				ConfigType.INT, userConfig.getHealthBarPosY(), -1000, 2000,
				v -> userConfig.setHealthBarPosY(v.intValue())));

		configOptions.add(new ConfigOption("config.energyBarPosX",
				ConfigType.INT, userConfig.getEnergyBarPosX(), -1000, 2000,
				v -> userConfig.setEnergyBarPosX(v.intValue())));

		configOptions.add(new ConfigOption("config.energyBarPosY",
				ConfigType.INT, userConfig.getEnergyBarPosY(), -1000, 2000,
				v -> userConfig.setEnergyBarPosY(v.intValue())));

		configOptions.add(new ConfigOption("config.staminaBarPosX",
				ConfigType.INT, userConfig.getStaminaBarPosX(), -1000, 2000,
				v -> userConfig.setStaminaBarPosX(v.intValue())));

		configOptions.add(new ConfigOption("config.staminaBarPosY",
				ConfigType.INT, userConfig.getStaminaBarPosY(), -1000, 2000,
				v -> userConfig.setStaminaBarPosY(v.intValue())));

		configOptions.add(new ConfigOption("config.cameraMovementDuringFlight",
				ConfigType.BOOLEAN, userConfig.getCameraMovementDuringFlight() ? 1 : 0, 0, 1,
				v -> userConfig.setCameraMovementDuringFlight(v > 0)));

		configOptions.add(new ConfigOption("config.liveCrowdinTranslations",
				ConfigType.BOOLEAN, userConfig.getLiveCrowdinTranslations() ? 1 : 0, 0, 1,
				v -> userConfig.setLiveCrowdinTranslations(v > 0)));
	}

	private void initConfigButtons() {
		clearConfigButtons();
		LivingEntity player = this.minecraft.player;

		int rightPanelX = getRightPanelX() - 5;
		int centerY = getUiHeight() / 2;
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
						.onPress(button -> {
							modifyConfigValue(index, -1);
							heldConfigIndex = index;
							heldDelta = -1;
							holdTicks = 0;
						})
						.build();
				decreaseButtons.add(decreaseBtn);
				this.addRenderableWidget(decreaseBtn);

				CustomTextureButton increaseBtn = new CustomTextureButton.Builder()
						.position(rightPanelX + 108, itemY + 3)
						.size(14, 11)
						.texture(STAT_BUTTONS)
						.textureCoords(0, 0, 0, 10)
						.textureSize(10, 10)
						.onPress(button -> {
							modifyConfigValue(index, 1);
							heldConfigIndex = index;
							heldDelta = 1;
							holdTicks = 0;
						})
						.build();
				increaseButtons.add(increaseBtn);
				this.addRenderableWidget(increaseBtn);
			}
		}
	}

	private void clearConfigButtons() {
		for (CustomTextureButton btn : decreaseButtons) this.removeWidget(btn);
		for (CustomTextureButton btn : increaseButtons) this.removeWidget(btn);
		for (SwitchButton btn : switchButtons) this.removeWidget(btn);
		decreaseButtons.clear();
		increaseButtons.clear();
		switchButtons.clear();
	}

	@Override
	public void tick() {
		super.tick();
		tickCount++;

		if (heldConfigIndex != -1) {
			holdTicks++;
			if (holdTicks > 10 && holdTicks % 2 == 0) {
				modifyConfigValue(heldConfigIndex, heldDelta);
			}
		}
	}

	private void updateConfigsList() {
		maxScroll = Math.max(0, configOptions.size() - MAX_VISIBLE_CONFIGS);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (isNotAnimating()) this.renderBackground(graphics);
		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);
		applyZoom(graphics, partialTick);

		int leftOffset = getLeftPanelSwitchOffset(partialTick);
		graphics.pose().pushPose();
		graphics.pose().translate(leftOffset, 0, 0);
		renderLeftPanel(graphics, uiMouseX - leftOffset, uiMouseY);
		graphics.pose().popPose();

		int rightOffset = getRightPanelSwitchOffset(partialTick);
		updateRightPanelButtonOffsets(rightOffset);
		graphics.pose().pushPose();
		graphics.pose().translate(rightOffset, 0, 0);
		renderRightPanel(graphics, uiMouseX - rightOffset, uiMouseY);
		graphics.pose().popPose();

		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	private void updateRightPanelButtonOffsets(int rightOffset) {
		int rightPanelX = getRightPanelX() - 5;
		int decreaseX = rightPanelX + 25 + rightOffset;
		int increaseX = rightPanelX + 108 + rightOffset;
		int switchX = rightPanelX + 65 + rightOffset;

		for (CustomTextureButton btn : decreaseButtons) {
			btn.setX(decreaseX);
		}
		for (CustomTextureButton btn : increaseButtons) {
			btn.setX(increaseX);
		}
		for (SwitchButton btn : switchButtons) {
			btn.setX(switchX);
		}
	}

	// The two panels are centered side by side (no player model on this screen),
	// so both X anchors are derived from the screen center.
	private int getLeftPanelX() {
		return getUiWidth() / 2 - 143;
	}

	private int getRightPanelX() {
		return getUiWidth() / 2 + 2;
	}

	private void renderLeftPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int leftPanelX = getLeftPanelX();
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(MENU_BIG, leftPanelX, centerY - 105, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, leftPanelX + 17, centerY - 95, 142, 22, 107, 21, 256, 256);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.config.options").withStyle(ChatFormatting.BOLD),
				leftPanelX + 70, leftPanelY + 17, 0xFFFFD700);

		renderConfigsList(graphics, leftPanelX, leftPanelY, mouseX, mouseY);
	}

	private void renderConfigsList(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
		int startY = panelY + 35;
		int visibleStart = scrollOffset;
		int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_CONFIGS, configOptions.size());

		graphics.enableScissor(
				toScreenCoord(panelX + 5),
				toScreenCoord(startY),
				toScreenCoord(panelX + 144),
				toScreenCoord(startY + (MAX_VISIBLE_CONFIGS * CONFIG_ITEM_HEIGHT))
		);

		graphics.pose().pushPose();
		graphics.pose().scale(0.75f, 0.75f, 0.75f);

		for (int i = visibleStart; i < visibleEnd; i++) {
			ConfigOption option = configOptions.get(i);
			int itemY = startY + ((i - visibleStart) * CONFIG_ITEM_HEIGHT);

			String displayName = tr("gui.dragonminez." + option.key).getString();

			TextUtil.drawStringWithBorder(graphics, this.font, txt(displayName),
					(int) ((panelX + 15) / 0.75f), (int) (itemY / 0.75f) + 6, 0xFFFFFFFF);
		}

		graphics.pose().popPose();
		graphics.disableScissor();

		if (maxScroll > 0) {
			int scrollBarX = panelX + 128;
			int scrollBarHeight = MAX_VISIBLE_CONFIGS * CONFIG_ITEM_HEIGHT;
			int totalItems = configOptions.size();

			graphics.fill(scrollBarX, startY, scrollBarX + 3, startY + scrollBarHeight, 0xFF333333);

			float scrollPercent = (float) scrollOffset / maxScroll;
			float visiblePercent = (float) MAX_VISIBLE_CONFIGS / totalItems;
			int indicatorHeight = Math.max(20, (int) (scrollBarHeight * visiblePercent));
			int indicatorY = startY + (int) ((scrollBarHeight - indicatorHeight) * scrollPercent);

			graphics.fill(scrollBarX, indicatorY, scrollBarX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}
	}

	private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int rightPanelX = getRightPanelX();
		int centerY = getUiHeight() / 2;
		int rightPanelY = centerY - 105;

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(MENU_BIG, rightPanelX, centerY - 105, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, rightPanelX + 17, centerY - 95, 142, 22, 107, 21, 256, 256);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.config.values").withStyle(ChatFormatting.BOLD),
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
					valueText = String.valueOf((int) option.value);
				}

				graphics.pose().pushPose();
				graphics.pose().scale(0.75f, 0.75f, 0.75f);
				TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(valueText),
						(int) ((panelX + 69) / 0.75f), (int) ((itemY + 5) / 0.75f), 0xFFFFFFFF);
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
			float step;
			if ("config.menuScaleMultiplier".equals(option.key)) {
				step = isShiftDown ? 0.25f : 0.05f;
			} else {
				step = isShiftDown ? 1.0f : 0.1f;
			}
			option.value = Math.max(option.min, Math.min(option.max, option.value + (delta * step)));
			option.value = Math.round(option.value * 100.0f) / 100.0f;
		}

		option.setter.accept(option.value);

		if ("config.menuScaleMultiplier".equals(option.key)) {
			rebuildWidgetsWithoutTransition();
		}

		if ("config.liveCrowdinTranslations".equals(option.key) && this.minecraft != null) {
			if (userConfig.getLiveCrowdinTranslations()) {
				CrowdinManager.fetchLanguage(this.minecraft.options.languageCode);
			} else {
				CrowdinManager.clearCache();
			}
			this.minecraft.reloadResourcePacks();
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int leftPanelX = getLeftPanelX();
		int rightPanelX = getRightPanelX();
		int centerY = getUiHeight() / 2;
		int panelY = centerY - 105;

		boolean overLeft = uiMouseX >= leftPanelX && uiMouseX <= leftPanelX + 148;
		boolean overRight = uiMouseX >= rightPanelX && uiMouseX <= rightPanelX + 148;

		if ((overLeft || overRight) &&
				uiMouseY >= panelY + 40 && uiMouseY <= panelY + 219) {

			int scrollAmount = (int) Math.signum(delta);
			scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - scrollAmount));
			initConfigButtons();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int leftPanelX = getLeftPanelX();
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		int startY = leftPanelY + 35;
		int scrollBarHeight = MAX_VISIBLE_CONFIGS * CONFIG_ITEM_HEIGHT;
		int scrollBarX = leftPanelX + 140;

		if (maxScroll > 0 && uiMouseX >= scrollBarX - 5 && uiMouseX <= scrollBarX + 10 &&
				uiMouseY >= startY && uiMouseY <= startY + scrollBarHeight) {
			isDraggingScroll = true;
			scrollOffset = calculateScrollOffset(uiMouseY, startY, scrollBarHeight, maxScroll);
			initConfigButtons();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (isDraggingScroll && maxScroll > 0) {
			double uiMouseY = toUiY(mouseY);
			int centerY = getUiHeight() / 2;
			int startY = (centerY - 105) + 35;
			int scrollBarHeight = MAX_VISIBLE_CONFIGS * CONFIG_ITEM_HEIGHT;

			scrollOffset = calculateScrollOffset(uiMouseY, startY, scrollBarHeight, maxScroll);
			initConfigButtons();
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		heldConfigIndex = -1;

		if (isDraggingScroll) {
			isDraggingScroll = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
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