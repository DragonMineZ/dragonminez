package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dragonminez.client.gui.config.HudEditorScreen;
import com.dragonminez.client.gui.config.OverShoulderCameraScreen;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.gui.hud.HudSmoother;
import com.dragonminez.client.gui.tutorial.TutorialManager;
import com.dragonminez.client.render.effects.AuraModeState;
import com.dragonminez.client.util.ScrollbarState;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.GeneralUserConfig;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.DynamicGrowthToggleC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.extras.DynamicGrowthStat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ConfigMenuScreen extends BaseMenuScreen {

	private static final String AURA_STYLE_KEY = "config.aura3DStyle";
	private static final String MENU_SCALE_KEY = "config.menuScaleMultiplier";
	private static final String UTILITY_SCALE_KEY = "config.utilityMenuScaleMultiplier";

	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menubig.png");
	private static final ResourceLocation STAT_BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");

	private static final int ROW_HEIGHT = 20;
	private static final int SEARCH_TOP = 34;
	private static final int SEARCH_HEIGHT = 14;
	private static final int LIST_TOP = 52;
	private static final int LIST_HEIGHT = 148;
	private static final float TEXT_SCALE = 0.75f;
	private static final long FLASH_MILLIS = 1500L;

	private static final EnumSet<Category> EXPANDED = EnumSet.noneOf(Category.class);

	private enum Category {
		INTERFACE, GAMEPLAY, VISUALS;

		String translationKey() {
			return "gui.dragonminez.config.category." + name().toLowerCase(Locale.ROOT);
		}
	}

	private enum ConfigType { INT, FLOAT, BOOLEAN, ACTION }

	private record LayoutRow(Category category, ConfigOption option, float y, float blockTop, float blockBottom) {
		boolean isHeader() {
			return option == null;
		}
	}

	private final Map<Category, HudSmoother> openProgress = new EnumMap<>(Category.class);
	private final List<ConfigOption> configOptions = new ArrayList<>();
	private final List<LayoutRow> layoutRows = new ArrayList<>();
	private final ScrollbarState scrollBar = new ScrollbarState();

	private GeneralUserConfig userConfig;
	private EditBox searchBox;
	private String query = "";
	private float scroll;
	private float targetScroll;
	private float maxScroll;
	private long lastFrameNanos;
	private int holdTicks;
	private String heldKey;
	private int heldDelta;

	public ConfigMenuScreen() {
		super(Component.translatable("gui.dragonminez.config.title"));
		for (Category category : Category.values()) openProgress.put(category, new HudSmoother(0.085f));
	}

	@Override
	protected void init() {
		super.init();
		userConfig = ConfigManager.getUserConfig();
		initializeConfigOptions();
		initSearchBox();
	}

	private void initSearchBox() {
		Style style = Style.EMPTY.withFont(DMZ_FONT);
		searchBox = new EditBox(this.font, getLeftPanelX() + 17, getPanelY() + SEARCH_TOP + 3, 106, 10, tr("gui.dragonminez.config.search"));
		searchBox.setBordered(false);
		searchBox.setMaxLength(32);
		searchBox.setTextColor(0xFFFFFF);
		searchBox.setFormatter((text, position) -> FormattedCharSequence.forward(text, style));
		searchBox.setHint(tr("gui.dragonminez.config.search").withStyle(ChatFormatting.GRAY));
		searchBox.setValue(query);
		searchBox.setResponder(value -> {
			String normalized = value.trim().toLowerCase(Locale.ROOT);
			if (normalized.equals(query)) return;
			query = normalized;
			targetScroll = 0.0f;
		});
		this.addRenderableWidget(searchBox);
	}

	private void initializeConfigOptions() {
		configOptions.clear();

		action(Category.INTERFACE, "config.hudEditor", "gui.dragonminez.config.open", () -> this.minecraft.setScreen(new HudEditorScreen(this)));
		toggle(Category.INTERFACE, "config.alwaysVisibleHudValues", userConfig.getAlwaysVisibleHudValues(), userConfig::setAlwaysVisibleHudValues);
		toggle(Category.INTERFACE, "config.hideHudNumbers", userConfig.getHideHudNumbers(), userConfig::setHideHudNumbers);
		toggle(Category.INTERFACE, "config.partyMarkers", userConfig.getPartyMarkers(), userConfig::setPartyMarkers);
		toggle(Category.INTERFACE, "config.showAccumulativeDamage", userConfig.getShowAccumulativeDamage(), userConfig::setShowAccumulativeDamage);
		toggle(Category.INTERFACE, "config.advancedDescription", userConfig.getAdvancedDescription(), userConfig::setAdvancedDescription);
		toggle(Category.INTERFACE, "config.advancedDescriptionPercentage", userConfig.getAdvancedDescriptionPercentage(), userConfig::setAdvancedDescriptionPercentage);
		toggle(Category.INTERFACE, "config.hexagonStatsDisplay", userConfig.getHexagonStatsDisplay(), userConfig::setHexagonStatsDisplay);
		number(Category.INTERFACE, MENU_SCALE_KEY, ConfigType.FLOAT, userConfig.getMenuScaleMultiplier(),
				MIN_MENU_SCALE_MULTIPLIER, MAX_MENU_SCALE_MULTIPLIER, userConfig::setMenuScaleMultiplier);
		number(Category.INTERFACE, UTILITY_SCALE_KEY, ConfigType.FLOAT, userConfig.getUtilityMenuScaleMultiplier(),
				0.5f, 2.5f, userConfig::setUtilityMenuScaleMultiplier);
		toggle(Category.INTERFACE, "config.tutorialsEnabled", userConfig.getTutorialsEnabled(), userConfig::setTutorialsEnabled);
		action(Category.INTERFACE, "config.resetTutorials", "gui.dragonminez.config.reset", TutorialManager::resetSeen);

		toggle(Category.GAMEPLAY, "config.cameraMovementDuringFlight", userConfig.getCameraMovementDuringFlight(), userConfig::setCameraMovementDuringFlight);
		action(Category.GAMEPLAY, "config.overShoulderCamera", "gui.dragonminez.config.open", () -> this.minecraft.setScreen(new OverShoulderCameraScreen(this)));
		initializeDynamicGrowthOptions();

		toggle(Category.VISUALS, "config.aura3DPersonal", userConfig.getAura3DPersonal(), userConfig::setAura3DPersonal);
		number(Category.VISUALS, AURA_STYLE_KEY, ConfigType.INT, FormConfig.AURA_3D_SPARKING.equals(userConfig.getAura3DStyle()) ? 1 : 0, 0, 1,
				v -> userConfig.setAura3DStyle(v > 0 ? FormConfig.AURA_3D_SPARKING : FormConfig.AURA_3D_SMOOTH));
		toggle(Category.VISUALS, "config.aura3DEntities", userConfig.getAura3DEntities(), userConfig::setAura3DEntities);
		toggle(Category.VISUALS, "config.transformationOutlines", userConfig.getTransformationOutlines(), userConfig::setTransformationOutlines);
		toggle(Category.VISUALS, "config.impactFramesEnabled", userConfig.isImpactFramesEnabled(), userConfig::setImpactFramesEnabled);
		toggle(Category.VISUALS, "config.firstPersonAnimated", userConfig.getFirstPersonAnimated(), userConfig::setFirstPersonAnimated);
		toggle(Category.VISUALS, "config.taiyokenInvertPalette", userConfig.getTaiyokenInvertPalette(), userConfig::setTaiyokenInvertPalette);
	}

	private void initializeDynamicGrowthOptions() {
		if (this.minecraft == null || this.minecraft.player == null) return;
		if (!ConfigManager.getServerConfig().getDynamicGrowth().isEnabled()) return;

		StatsProvider.get(StatsCapability.INSTANCE, this.minecraft.player).ifPresent(data -> {
			for (DynamicGrowthStat stat : DynamicGrowthStat.values()) {
				toggle(Category.GAMEPLAY, "config.dynamicGrowthFor" + stat.key(), data.getDynamicGrowth().isGrowthEnabled(stat), enabled -> {
					data.getDynamicGrowth().setGrowthEnabled(stat, enabled);
					NetworkHandler.sendToServer(new DynamicGrowthToggleC2S(stat.key(), enabled));
				});
			}
		});
	}

	private void toggle(Category category, String key, boolean value, Consumer<Boolean> setter) {
		configOptions.add(new ConfigOption(category, key, ConfigType.BOOLEAN, value ? 1 : 0, 0, 1, v -> setter.accept(v > 0), null, null));
	}

	private void number(Category category, String key, ConfigType type, float value, float min, float max, Consumer<Float> setter) {
		configOptions.add(new ConfigOption(category, key, type, value, min, max, setter, null, null));
	}

	private void action(Category category, String key, String labelKey, Runnable action) {
		configOptions.add(new ConfigOption(category, key, ConfigType.ACTION, 0, 0, 0, null, action, labelKey));
	}

	@Override
	public void tick() {
		super.tick();
		if (searchBox != null) searchBox.tick();

		if (heldKey != null) {
			holdTicks++;
			if (holdTicks > 10 && holdTicks % 2 == 0) {
				ConfigOption held = findOption(heldKey);
				if (held != null) modifyConfigValue(held, heldDelta);
			}
		}
	}

	private ConfigOption findOption(String key) {
		for (ConfigOption option : configOptions) if (option.key.equals(key)) return option;
		return null;
	}

	private int getLeftPanelX() {
		return getUiWidth() / 2 - 143;
	}

	private int getRightPanelX() {
		return getUiWidth() / 2 + 2;
	}

	private int getPanelY() {
		return getUiHeight() / 2 - 105;
	}

	private int listTop() {
		return getPanelY() + LIST_TOP;
	}

	private boolean matches(ConfigOption option) {
		if (query.isEmpty()) return true;
		String name = tr("gui.dragonminez." + option.key).getString().toLowerCase(Locale.ROOT);
		String category = tr(option.category.translationKey()).getString().toLowerCase(Locale.ROOT);
		return name.contains(query) || category.contains(query);
	}

	private void updateLayout() {
		layoutRows.clear();
		float cursor = 0.0f;
		boolean searching = !query.isEmpty();

		for (Category category : Category.values()) {
			List<ConfigOption> visible = new ArrayList<>();
			for (ConfigOption option : configOptions) if (option.category == category && matches(option)) visible.add(option);
			if (visible.isEmpty()) {
				openProgress.get(category).update(0.0f);
				continue;
			}

			float progress = openProgress.get(category).update(searching || EXPANDED.contains(category) ? 1.0f : 0.0f);
			layoutRows.add(new LayoutRow(category, null, cursor, cursor, cursor + ROW_HEIGHT));
			cursor += ROW_HEIGHT;

			float blockTop = cursor;
			float blockBottom = cursor + visible.size() * ROW_HEIGHT * easeInOut(progress);
			if (blockBottom - blockTop > 0.5f) {
				float slide = (blockBottom - blockTop) - visible.size() * ROW_HEIGHT;
				for (int i = 0; i < visible.size(); i++) {
					layoutRows.add(new LayoutRow(category, visible.get(i), blockTop + slide + i * ROW_HEIGHT, blockTop, blockBottom));
				}
			}
			cursor = blockBottom;
		}

		maxScroll = Math.max(0.0f, cursor - LIST_HEIGHT);
		targetScroll = Mth.clamp(targetScroll, 0.0f, maxScroll);
	}

	private static float easeInOut(float t) {
		return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3) / 2.0f;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderMenuBackground(graphics, partialTick);
		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		long now = System.nanoTime();
		float dt = lastFrameNanos == 0L ? 0.0f : Math.min(0.1f, (now - lastFrameNanos) / 1_000_000_000.0f);
		lastFrameNanos = now;

		updateLayout();
		if (scrollBar.isDragging()) scroll = targetScroll;
		else scroll += (targetScroll - scroll) * (dt <= 0.0f ? 1.0f : 1.0f - (float) Math.exp(-dt / 0.07f));
		scroll = Mth.clamp(scroll, 0.0f, maxScroll);

		beginUiScale(graphics);
		applyZoom(graphics, partialTick);

		float leftOffset = getLeftPanelSwitchOffset(partialTick);
		float rightOffset = getRightPanelSwitchOffset(partialTick);
		slideX(searchBox, getLeftPanelX() + 17, leftOffset);

		graphics.pose().pushPose();
		graphics.pose().translate(leftOffset, 0.0f, 0.0f);
		renderLeftPanel(graphics, uiMouseX - Math.round(leftOffset), uiMouseY, leftOffset);
		graphics.pose().popPose();

		graphics.pose().pushPose();
		graphics.pose().translate(rightOffset, 0.0f, 0.0f);
		renderRightPanel(graphics, uiMouseX - Math.round(rightOffset), uiMouseY, rightOffset);
		graphics.pose().popPose();

		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	private void renderLeftPanel(GuiGraphics graphics, int mouseX, int mouseY, float slide) {
		int panelX = getLeftPanelX();
		int panelY = getPanelY();

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		blit(graphics, MENU_BIG, panelX, panelY, 0, 0, 141, 213);
		blit(graphics, MENU_BIG, panelX + 17, panelY + 10, 142, 22, 107, 21);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.config.options").withStyle(ChatFormatting.BOLD),
				panelX + 70, panelY + 17, 0xFFFFD700);

		boolean editing = searchBox != null && searchBox.isFocused();
		HudRender.nineSlice(graphics, STAT_BUTTONS, panelX + 12, panelY + SEARCH_TOP, 117, SEARCH_HEIGHT, 0, editing ? 126 : 108, 107, 18, 3, 256, 256);

		int top = listTop();
		enableListScissor(graphics, panelX + 5, panelX + 136, slide);
		for (LayoutRow row : layoutRows) {
			float y = top + row.y() - scroll;
			if (y + ROW_HEIGHT < top || y > top + LIST_HEIGHT) continue;
			boolean hovered = isRowHovered(row, mouseX, mouseY, panelX + 8, panelX + 126) || isRowHovered(row, mouseX + getLeftPanelX() - getRightPanelX(), mouseY, panelX + 8, panelX + 126);

			if (row.isHeader()) {
				renderHeader(graphics, row.category(), panelX, y, hovered);
				continue;
			}

			enableBlockScissor(graphics, row, panelX + 5, panelX + 136, slide);
			if (hovered) HudRender.rect(graphics, panelX + 9, y, 117, ROW_HEIGHT - 1, 0x16FFFFFF);
			drawScaledText(graphics, tr("gui.dragonminez." + row.option().key), panelX + 15, y + 6, 0xFFFFFFFF, false);
			graphics.disableScissor();
		}
		graphics.disableScissor();

		if (layoutRows.isEmpty()) {
			drawScaledText(graphics, tr("gui.dragonminez.config.no_results").withStyle(ChatFormatting.GRAY), panelX + 70, top + 8, 0xFFAAAAAA, true);
		}

		renderScrollBar(graphics, panelX + 128, top);
	}

	private void renderHeader(GuiGraphics graphics, Category category, int panelX, float y, boolean hovered) {
		float progress = easeInOut(Mth.clamp(openProgress.get(category).value(), 0.0f, 1.0f));
		HudRender.rect(graphics, panelX + 9, y + 1, 117, ROW_HEIGHT - 3, hovered ? 0x30FFFFFF : 0x1CFFFFFF);

		graphics.pose().pushPose();
		graphics.pose().translate(panelX + 16.5f, y + 9.5f, 0.0f);
		graphics.pose().mulPose(Axis.ZP.rotationDegrees(90.0f * progress));
		graphics.pose().scale(0.6f, 0.6f, 1.0f);
		blit(graphics, STAT_BUTTONS, -4.0f, -7.0f, 20, hovered ? 14 : 0, 8, 14);
		graphics.pose().popPose();

		int count = 0;
		for (ConfigOption option : configOptions) if (option.category == category && matches(option)) count++;
		Component label = tr(category.translationKey()).withStyle(ChatFormatting.BOLD);
		drawScaledText(graphics, label, panelX + 24, y + 6, hovered ? 0xFF7CFDD6 : 0xFFFFD700, false);
		Component counter = txt(String.valueOf(count));
		drawScaledText(graphics, counter, panelX + 122 - this.font.width(counter) * TEXT_SCALE, y + 6, 0xFF9FB8AE, false);
	}

	private void renderScrollBar(GuiGraphics graphics, int barX, int top) {
		scrollBar.update(barX, 3, top, LIST_HEIGHT, maxScroll);
		if (maxScroll <= 0.0f) return;

		float contentHeight = maxScroll + LIST_HEIGHT;
		float thumbHeight = Math.max(20.0f, LIST_HEIGHT * (LIST_HEIGHT / contentHeight));
		float thumbY = top + (LIST_HEIGHT - thumbHeight) * (scroll / maxScroll);
		HudRender.rect(graphics, barX, top, 3, LIST_HEIGHT, 0xFF333333);
		HudRender.rect(graphics, barX, thumbY, 3, thumbHeight, scrollBar.isDragging() ? 0xFFFFFFFF : 0xFFAAAAAA);
	}

	private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY, float slide) {
		int panelX = getRightPanelX();
		int panelY = getPanelY();

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		blit(graphics, MENU_BIG, panelX, panelY, 0, 0, 141, 213);
		blit(graphics, MENU_BIG, panelX + 17, panelY + 10, 142, 22, 107, 21);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.config.values").withStyle(ChatFormatting.BOLD),
				panelX + 70, panelY + 17, 0xFFFFD700);

		int top = listTop();
		enableListScissor(graphics, panelX + 5, panelX + 136, slide);
		for (LayoutRow row : layoutRows) {
			float y = top + row.y() - scroll;
			if (y + ROW_HEIGHT < top || y > top + LIST_HEIGHT) continue;
			boolean hovered = isRowHovered(row, mouseX, mouseY, panelX + 8, panelX + 132) || isRowHovered(row, mouseX + getRightPanelX() - getLeftPanelX(), mouseY, panelX + 8, panelX + 132);

			if (row.isHeader()) {
				HudRender.rect(graphics, panelX + 15, y + 1, 111, ROW_HEIGHT - 3, hovered ? 0x30FFFFFF : 0x1CFFFFFF);
				HudRender.rect(graphics, panelX + 24, y + 9, 93, 1, 0x558D9BC6);
				continue;
			}

			enableBlockScissor(graphics, row, panelX + 5, panelX + 136, slide);
			if (hovered) HudRender.rect(graphics, panelX + 15, y, 111, ROW_HEIGHT - 1, 0x16FFFFFF);
			renderValue(graphics, row.option(), panelX, y, mouseX, mouseY, isInteractable(row, mouseY));
			graphics.disableScissor();
		}
		graphics.disableScissor();
	}

	private void renderValue(GuiGraphics graphics, ConfigOption option, int panelX, float y, int mouseX, int mouseY, boolean interactable) {
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		switch (option.type) {
			case BOOLEAN -> blit(graphics, STAT_BUTTONS, panelX + 60, y + 3, 122, option.value > 0 ? 0 : 10, 20, 10);
			case ACTION -> {
				boolean hovered = interactable && inside(mouseX, mouseY, panelX + 33, y, 74, ROW_HEIGHT);
				blit(graphics, STAT_BUTTONS, panelX + 33, y, 0, hovered ? 48 : 28, 74, 20);
				boolean flashing = System.currentTimeMillis() < option.flashUntil;
				Component label = tr(flashing ? "gui.dragonminez.config.done" : option.actionLabelKey);
				graphics.pose().pushPose();
				graphics.pose().translate(panelX + 70 - this.font.width(label) / 2.0f, y + 6, 0.0f);
				graphics.drawString(this.font, label, 0, 0, flashing ? 0x55FF55 : hovered ? 0x7CFDD6 : 0xFFFFFF);
				graphics.pose().popPose();
			}
			default -> {
				boolean overDecrease = interactable && inside(mouseX, mouseY, panelX + 20, y + 3, 14, 11);
				boolean overIncrease = interactable && inside(mouseX, mouseY, panelX + 103, y + 3, 14, 11);
				blit(graphics, STAT_BUTTONS, panelX + 20, y + 3, 142, overDecrease ? 10 : 0, 10, 10);
				blit(graphics, STAT_BUTTONS, panelX + 103, y + 3, 0, overIncrease ? 10 : 0, 10, 10);
				drawScaledText(graphics, txt(valueText(option)), panelX + 69, y + 5, 0xFFFFFFFF, true);
			}
		}
	}

	private String valueText(ConfigOption option) {
		if (AURA_STYLE_KEY.equals(option.key)) {
			return tr(option.value > 0 ? "gui.dragonminez.customization.aura.sparking" : "gui.dragonminez.customization.aura.smooth").getString();
		}
		if (option.type == ConfigType.FLOAT) return String.format(Locale.US, "%.2f", option.value);
		return String.valueOf((int) option.value);
	}

	private void drawScaledText(GuiGraphics graphics, Component text, float x, float y, int color, boolean centered) {
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0.0f);
		graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
		if (centered) TextUtil.drawCenteredStringWithBorder(graphics, this.font, text, 0, 0, color);
		else TextUtil.drawStringWithBorder(graphics, this.font, text, 0, 0, color);
		graphics.pose().popPose();
	}

	private void enableListScissor(GuiGraphics graphics, int left, int right, float slide) {
		int top = listTop();
		graphics.enableScissor(toScreenCoord(left + slide), toScreenCoord(top), toScreenCoord(right + slide), toScreenCoord(top + LIST_HEIGHT));
	}

	private void enableBlockScissor(GuiGraphics graphics, LayoutRow row, int left, int right, float slide) {
		int top = listTop();
		graphics.enableScissor(toScreenCoord(left + slide), toScreenCoord(top + row.blockTop() - scroll),
				toScreenCoord(right + slide), toScreenCoord(top + row.blockBottom() - scroll));
	}

	private static boolean inside(double mouseX, double mouseY, float x, float y, float width, float height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private boolean isInteractable(LayoutRow row, double mouseY) {
		int top = listTop();
		if (mouseY < top || mouseY >= top + LIST_HEIGHT) return false;
		return mouseY >= top + row.blockTop() - scroll && mouseY < top + row.blockBottom() - scroll;
	}

	private boolean isRowHovered(LayoutRow row, double mouseX, double mouseY, int left, int right) {
		if (mouseX < left || mouseX >= right || !isInteractable(row, mouseY)) return false;
		float y = listTop() + row.y() - scroll;
		return mouseY >= y && mouseY < y + ROW_HEIGHT;
	}

	private LayoutRow rowAt(double mouseX, double mouseY) {
		int leftX = getLeftPanelX();
		int rightX = getRightPanelX();
		for (LayoutRow row : layoutRows) {
			if (isRowHovered(row, mouseX, mouseY, leftX + 8, leftX + 126) || isRowHovered(row, mouseX, mouseY, rightX + 8, rightX + 132)) return row;
		}
		return null;
	}

	private void modifyConfigValue(ConfigOption option, int delta) {
		boolean shift = Screen.hasShiftDown();

		if (option.type == ConfigType.BOOLEAN) {
			option.value = option.value > 0 ? 0 : 1;
		} else if (option.type == ConfigType.INT) {
			int step = shift ? 5 : 1;
			option.value = Mth.clamp(option.value + delta * step, option.min, option.max);
		} else if (option.type == ConfigType.FLOAT) {
			boolean scaleOption = MENU_SCALE_KEY.equals(option.key) || UTILITY_SCALE_KEY.equals(option.key);
			float step = scaleOption ? (shift ? 0.25f : 0.05f) : (shift ? 1.0f : 0.1f);
			option.value = Mth.clamp(option.value + delta * step, option.min, option.max);
			option.value = Math.round(option.value * 100.0f) / 100.0f;
		}

		option.setter.accept(option.value);

		if (MENU_SCALE_KEY.equals(option.key)) {
			heldKey = null;
			applyMenuScaleChange();
		}
		if ("config.aura3DPersonal".equals(option.key) || AURA_STYLE_KEY.equals(option.key)) AuraModeState.pushLocalPreference();
		if ("config.tutorialsEnabled".equals(option.key)) TutorialManager.setEnabled(option.value > 0);
	}

	private void playUi(SoundEvent sound) {
		if (this.minecraft != null) this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int panelY = getPanelY();

		boolean overPanels = uiMouseX >= getLeftPanelX() && uiMouseX <= getRightPanelX() + 141;
		if (overPanels && uiMouseY >= panelY && uiMouseY <= panelY + 213) {
			targetScroll = Mth.clamp(targetScroll - (float) delta * ROW_HEIGHT, 0.0f, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		if (searchBox != null && !searchBox.isMouseOver(uiMouseX, uiMouseY) && searchBox.isFocused()) {
			searchBox.setFocused(false);
			this.setFocused(null);
		}

		if (button == 0 && scrollBar.tryStartDrag(uiMouseX, uiMouseY)) {
			targetScroll = Mth.clamp(scrollBar.scrollFor(uiMouseY), 0.0f, maxScroll);
			return true;
		}

		if (button == 0) {
			LayoutRow row = rowAt(uiMouseX, uiMouseY);
			if (row != null && handleRowClick(row, uiMouseX, uiMouseY)) return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean handleRowClick(LayoutRow row, double mouseX, double mouseY) {
		if (row.isHeader()) {
			if (!query.isEmpty()) return true;
			if (!EXPANDED.remove(row.category())) EXPANDED.add(row.category());
			playUi(MainSounds.PIP_MENU.get());
			return true;
		}

		ConfigOption option = row.option();
		int panelX = getRightPanelX();
		float y = listTop() + row.y() - scroll;

		switch (option.type) {
			case BOOLEAN -> {
				if (!inside(mouseX, mouseY, panelX + 56, y, 28, ROW_HEIGHT)) return false;
				modifyConfigValue(option, 1);
				playUi(option.value > 0 ? MainSounds.SWITCH_ON.get() : MainSounds.SWITCH_OFF.get());
				return true;
			}
			case ACTION -> {
				if (!inside(mouseX, mouseY, panelX + 33, y, 74, ROW_HEIGHT)) return false;
				playUi(MainSounds.PIP_MENU.get());
				option.flashUntil = option.actionLabelKey.endsWith(".reset") ? System.currentTimeMillis() + FLASH_MILLIS : 0L;
				option.action.run();
				return true;
			}
			default -> {
				int delta = inside(mouseX, mouseY, panelX + 18, y + 1, 18, 15) ? -1 : inside(mouseX, mouseY, panelX + 101, y + 1, 18, 15) ? 1 : 0;
				if (delta == 0) return false;
				playUi(MainSounds.UI_NAVE_COOLDOWN.get());
				heldKey = option.key;
				heldDelta = delta;
				holdTicks = 0;
				modifyConfigValue(option, delta);
				return true;
			}
		}
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (scrollBar.isDragging()) {
			targetScroll = Mth.clamp(scrollBar.scrollFor(toUiY(mouseY)), 0.0f, maxScroll);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		heldKey = null;

		if (scrollBar.isDragging()) {
			scrollBar.stopDrag();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (searchBox != null && searchBox.isFocused()) {
			if (keyCode == 256) {
				searchBox.setFocused(false);
				this.setFocused(null);
				return true;
			}
			searchBox.keyPressed(keyCode, scanCode, modifiers);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void removed() {
		if (this.minecraft != null) ConfigManager.saveGeneralUserConfig();
		super.removed();
	}

	private static class ConfigOption {
		final Category category;
		final String key;
		final ConfigType type;
		final float min;
		final float max;
		final Consumer<Float> setter;
		final Runnable action;
		final String actionLabelKey;
		float value;
		long flashUntil;

		ConfigOption(Category category, String key, ConfigType type, float value, float min, float max,
					 Consumer<Float> setter, Runnable action, String actionLabelKey) {
			this.category = category;
			this.key = key;
			this.type = type;
			this.value = value;
			this.min = min;
			this.max = max;
			this.setter = setter;
			this.action = action;
			this.actionLabelKey = actionLabelKey;
		}
	}
}
