package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ClippableTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class MastersSkillsScreen extends BaseMenuScreen {

	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menubig.png");
	private static final ResourceLocation MENU_SMALL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menusmall.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static final int SKILL_ITEM_HEIGHT = 20;
	private static final int MAX_VISIBLE_SKILLS = 8;

	private enum SkillCategory {SKILLS, KI, FORMS, STRIKE}

	private SkillCategory currentCategory = SkillCategory.SKILLS;

	private StatsData statsData;
	private int tickCount = 0;

	private String selectedSkill = null;
	private float targetScroll = 0;
	private float currentScroll = 0;
	private float maxScroll = 0;

	private float targetDescScroll = 0;
	private float currentDescScroll = 0;
	private float maxDescScroll = 0;
	private boolean isDraggingDescScroll = false;

	private final Map<SkillCategory, ClippableTextureButton> categoryButtons = new LinkedHashMap<>();
	private final List<SkillCategory> activeCategories = new ArrayList<>();
	private float buttonRevealProgress = 0.0f;

	private TexturedTextButton purchaseButton;
	private boolean isDraggingScroll = false;

	private final String masterName;
	private final LivingEntity masterEntity;

	public MastersSkillsScreen(String masterName, LivingEntity masterEntity) {
		super(Component.literal(masterName).withStyle(Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth"))));
		this.masterName = masterName;
		this.masterEntity = masterEntity;
	}

	@Override
	protected void init() {
		super.init();
		updateStatsData();
		initDynamicButtons();
	}

	@Override
	protected void initNavigationButtons() {
	}

	@Override
	public void tick() {
		super.tick();
		tickCount++;

		if (tickCount >= 10) {
			tickCount = 0;
			updateStatsData();
			refreshButtons();
		}
	}

	private void updateStatsData() {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> this.statsData = data);
		}
	}

	private int textureUForCategory(SkillCategory category) {
		switch (category) {
			case SKILLS: return 142;
			case KI: return 170;
			case FORMS: return 198;
			case STRIKE: return 226;
		}
		return 142;
	}

	private void initDynamicButtons() {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		int buttonY = leftPanelY + 6;
		int hiddenX = leftPanelX + 122;

		int scissorX = leftPanelX + 141;
		int scissorXScreen = toScreenCoord(scissorX);
		int scissorYScreen = toScreenCoord(0);
		int scissorRight = toScreenCoord(getUiWidth());
		int scissorBottom = toScreenCoord(getUiHeight());
		activeCategories.clear();
		for (SkillCategory category : new SkillCategory[]{SkillCategory.SKILLS, SkillCategory.KI, SkillCategory.FORMS, SkillCategory.STRIKE}) {
			if (!getSkillsForCategory(category).isEmpty()) activeCategories.add(category);
		}

		if (!activeCategories.isEmpty() && !activeCategories.contains(currentCategory)) {
			currentCategory = activeCategories.get(0);
		}

		categoryButtons.clear();
		int index = 0;
		for (SkillCategory category : activeCategories) {
			int u = textureUForCategory(category);
			ClippableTextureButton button = new ClippableTextureButton.Builder()
					.position(hiddenX, buttonY + index * 32)
					.size(26, 32)
					.texture(MENU_BIG)
					.textureCoords(u, 44, u, 44)
					.clipping(true, scissorXScreen, scissorYScreen, scissorRight, scissorBottom)
					.onPress(btn -> {
						currentCategory = category;
						selectedSkill = null;
						targetScroll = 0;
						currentScroll = 0;
						refreshButtons();
					})
					.build();
			categoryButtons.put(category, button);
			this.addRenderableWidget(button);
			index++;
		}
	}

	private List<String> getMasterSkills() {
		Map<String, List<String>> skillOfferings = ConfigManager.getSkillsConfig().getSkillOfferings();
		return skillOfferings.getOrDefault(masterName.toLowerCase(), skillOfferings.get("default"));
	}

	private List<String> getVisibleSkillNames() {
		return getSkillsForCategory(currentCategory);
	}

	private List<String> getSkillsForCategory(SkillCategory category) {
		if (statsData == null) return new ArrayList<>();
		List<String> masterOfferings = getMasterSkills();
		if (masterOfferings == null) return new ArrayList<>();
		List<String> visibleSkills = new ArrayList<>();
		SkillsConfig skillsConfig = ConfigManager.getSkillsConfig();
		String playerRace = getPlayerRaceName();

		for (String skillId : masterOfferings) {
			if (!skillsConfig.isSkillAllowedForRace(skillId, playerRace)) continue;

			boolean isKi = skillsConfig.getKiSkills().contains(skillId);
			boolean isForm = skillsConfig.getFormSkills().contains(skillId) || skillsConfig.getStackSkills().contains(skillId);
			boolean isStrike = skillsConfig.getStrikeSkills().contains(skillId);

			switch (category) {
				case SKILLS -> { if (!isKi && !isForm && !isStrike) visibleSkills.add(skillId); }
				case KI -> { if (isKi) visibleSkills.add(skillId); }
				case FORMS -> { if (isForm) visibleSkills.add(skillId); }
				case STRIKE -> { if (isStrike) visibleSkills.add(skillId); }
			}
		}

		return visibleSkills;
	}

	private String getPlayerRaceName() {
		if (statsData == null || statsData.getCharacter() == null) return "";
		String raceName = statsData.getCharacter().getRaceName();
		return raceName != null ? raceName.toLowerCase(Locale.ROOT) : "";
	}

	private void refreshButtons() {
		this.clearWidgets();
		initDynamicButtons();
		initPurchaseButton();
	}

	private void initPurchaseButton() {
		if (selectedSkill == null || statsData == null) return;
		if (!ConfigManager.getSkillsConfig().isSkillAllowedForRace(selectedSkill, getPlayerRaceName())) return;

		Skill skill = statsData.getSkills().getSkill(selectedSkill);
		if (skill == null) skill = new Skill(selectedSkill, 0, false, 10);

		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int rightPanelY = centerY - 105;

		if (!statsData.getSkills().hasSkill(selectedSkill) || skill.getLevel() == 0) {
			int cost = getUpgradeCost(selectedSkill, 0);
			float currentTPS = statsData.getResources().getTrainingPoints();
			boolean canAfford = currentTPS >= cost;
			if (cost == -1 || cost == Integer.MAX_VALUE) return;

			purchaseButton = new TexturedTextButton.Builder()
					.position(rightPanelX + 35, rightPanelY + 196)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(tr("gui.dragonminez.skills.purchase"))
					.onPress(btn -> {
						if (canAfford) {
							NetworkHandler.INSTANCE.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.PURCHASE, selectedSkill, cost));
							updateStatsData();
						}
					})
					.build();

			purchaseButton.active = canAfford;
			this.addRenderableWidget(purchaseButton);
		}
	}

	private int getUpgradeCost(String skillName, int currentLevel) {
		var skillConfig = ConfigManager.getSkillsConfig();
		var skillData = skillConfig.getSkills().get(skillName);

		if (skillData != null && skillData.getCosts() != null) {
			var costs = skillData.getCosts();
			if (currentLevel < costs.size()) {
				return costs.get(currentLevel) != null ? costs.get(currentLevel) : Integer.MAX_VALUE;
			}
		}
		return Integer.MAX_VALUE;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (isNotAnimating()) this.renderBackground(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);
		applyZoom(graphics, partialTick);

		updateButtonAnimations(uiMouseX, uiMouseY, partialTick);
		renderMasterEntity(graphics, getUiWidth() / 2 + 5, getUiHeight() / 2 + 90, uiMouseX, uiMouseY);
		renderLeftPanel(graphics, uiMouseX, uiMouseY);
		renderRightPanel(graphics, uiMouseX, uiMouseY);
		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	private void updateButtonAnimations(int mouseX, int mouseY, float partialTick) {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		int hiddenX = leftPanelX + 122;
		int visibleX = leftPanelX + 141;
		int buttonWidth = 26;
		int panelWidth = 141;
		int panelHeight = 213;

		int hotZoneX = hiddenX;
		int hotZoneY = leftPanelY + 6;
		int hotZoneWidth = (visibleX - hiddenX) + buttonWidth;
		int hotZoneHeight = Math.max(1, categoryButtons.size()) * 32;

		boolean overPanel = mouseX >= leftPanelX && mouseX < leftPanelX + panelWidth && mouseY >= leftPanelY && mouseY < leftPanelY + panelHeight;
		boolean overHotZone = mouseX >= hotZoneX && mouseX < hotZoneX + hotZoneWidth && mouseY >= hotZoneY && mouseY < hotZoneY + hotZoneHeight;
		boolean shouldReveal = overPanel || overHotZone;

		float step = Math.max(0.01f, 0.07f + (partialTick * 0.01f));
		buttonRevealProgress = approach01(buttonRevealProgress, shouldReveal ? 1.0f : 0.0f, step);
		float animProgress = easeInOutCubic(buttonRevealProgress);

		int newX = hiddenX + (int) ((visibleX - hiddenX) * animProgress);

		int scissorX = leftPanelX + 141;
		int scissorXScreen = toScreenCoord(scissorX);
		int scissorYScreen = toScreenCoord(0);
		int scissorRight = toScreenCoord(getUiWidth());
		int scissorBottom = toScreenCoord(getUiHeight());

		for (ClippableTextureButton button : categoryButtons.values()) {
			button.setX(newX);
			button.setScissorRect(scissorXScreen, scissorYScreen, scissorRight, scissorBottom);
		}
	}

	private float approach01(float current, float target, float step) {
		if (current < target) return Math.min(target, current + step);
		if (current > target) return Math.max(target, current - step);
		return current;
	}

	private float easeInOutCubic(float t) {
		if (t <= 0.0f) return 0.0f;
		if (t >= 1.0f) return 1.0f;
		return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f;
	}

	private void renderLeftPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(MENU_BIG, leftPanelX, leftPanelY, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, 29, centerY - 95, 142, 22, 107, 21, 256, 256);

		renderSkillsList(graphics, leftPanelX, leftPanelY, mouseX, mouseY);
	}

	private void renderSkillsList(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
		List<String> skillNames = getVisibleSkillNames();

		int startY = panelY + 30;
		int viewHeight = MAX_VISIBLE_SKILLS * SKILL_ITEM_HEIGHT;
		int totalHeight = skillNames.size() * SKILL_ITEM_HEIGHT;

		maxScroll = Math.max(0, totalHeight - viewHeight);
		targetScroll = Mth.clamp(targetScroll, 0, maxScroll);
		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();
		currentScroll = Mth.lerp(tickDelta * 0.4f, currentScroll, targetScroll);

		graphics.enableScissor(
				toScreenCoord(panelX + 5),
				toScreenCoord(startY),
				toScreenCoord(panelX + 179),
				toScreenCoord(startY + viewHeight)
		);

		graphics.pose().pushPose();
		graphics.pose().translate(0, -currentScroll, 0);

		for (int i = 0; i < skillNames.size(); i++) {
			String skillName = skillNames.get(i);
			int itemY = startY + (i * SKILL_ITEM_HEIGHT);

			if (itemY + SKILL_ITEM_HEIGHT >= startY + currentScroll && itemY <= startY + viewHeight + currentScroll) {
				boolean isSelected = skillName.equals(selectedSkill);
				boolean isHovered = mouseX >= panelX + 10 && mouseX <= panelX + 100 &&
						mouseY >= itemY - currentScroll && mouseY <= itemY + SKILL_ITEM_HEIGHT - currentScroll;

				int color = isSelected ? 0xFFFFAA00 : (isHovered ? 0xFFAAAAAA : 0xFFFFFFFF);

				Skill skill = statsData.getSkills().getSkill(skillName);
				String displayName;
				if (currentCategory == SkillCategory.KI || currentCategory == SkillCategory.STRIKE) displayName = Component.translatable("technique.dragonminez." + skillName).getString();
				else displayName = Component.translatable("skill.dragonminez." + skillName).getString();

				TextUtil.drawStringWithBorder(graphics, this.font, txt(displayName), panelX + 15, itemY + 5, color);

				String levelText;
				if (skill != null && skill.getLevel() > 0) levelText = String.valueOf(skill.getLevel());
				else levelText = "0";

				int levelX = panelX + 130 - TextUtil.width(this.font, levelText, DMZ_FONT);
				TextUtil.drawStringWithBorder(graphics, this.font, txt(levelText), levelX, itemY + 5, color);
			}
		}

		graphics.pose().popPose();
		graphics.disableScissor();

		if (maxScroll > 0) {
			int scrollBarX = panelX + 135;
			graphics.fill(scrollBarX, startY, scrollBarX + 3, startY + viewHeight, 0xFF333333);

			float scrollPercent = currentScroll / maxScroll;
			float visiblePercent = (float) viewHeight / totalHeight;
			int indicatorHeight = Math.max(20, (int) (viewHeight * visiblePercent));
			int indicatorY = startY + (int) ((viewHeight - indicatorHeight) * scrollPercent);

			graphics.fill(scrollBarX, indicatorY, scrollBarX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}

		String title = "";
		switch (currentCategory) {
			case SKILLS -> title = "gui.dragonminez.skills.tab.skills";
			case KI -> title = "gui.dragonminez.skills.tab.kiattacks";
			case FORMS -> title = "gui.dragonminez.skills.tab.forms";
			case STRIKE -> title = "gui.dragonminez.skills.tab.strikeattacks";
		}

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(title).withStyle(style -> style.withBold(true)), 80, getUiHeight() / 2 - 88, 0xFBC51C);
	}

	private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int rightPanelY = centerY - 105;

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		if (currentCategory == SkillCategory.KI || currentCategory == SkillCategory.STRIKE) {
			graphics.blit(MENU_BIG, rightPanelX, rightPanelY, 0, 0, 141, 213, 256, 256);
			graphics.blit(MENU_BIG, getUiWidth() - 141, centerY - 95, 142, 22, 107, 21, 256, 256);
		} else {
			graphics.blit(MENU_SMALL, rightPanelX, rightPanelY, 0, 0, 141, 94, 256, 256);
			graphics.blit(MENU_BIG, getUiWidth() - 141, centerY - 95, 142, 22, 107, 21, 256, 256);
			graphics.blit(MENU_SMALL, rightPanelX, rightPanelY + 96, 0, 0, 141, 94, 256, 256);
			graphics.blit(MENU_SMALL, rightPanelX, rightPanelY + 190, 0, 154, 141, 32, 256, 256);
		}

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.character_stats.info")
				.withStyle(style -> style.withBold(true)), rightPanelX + 70, rightPanelY + 16, 0xFFFFD700);

		if (selectedSkill != null && statsData != null) {
			if (currentCategory == SkillCategory.KI) renderKiTechniqueDetails(graphics, rightPanelX, rightPanelY);
			else if (currentCategory == SkillCategory.STRIKE) renderStrikeTechniqueDetails(graphics, rightPanelX, rightPanelY);
			else renderSkillDetails(graphics, rightPanelX, rightPanelY);
		}
	}

	private void renderKiTechniqueDetails(GuiGraphics graphics, int panelX, int panelY) {
		KiAttackData tech = PredefinedTechniques.REGISTRY.get(selectedSkill);
		if (tech == null) return;

		int yOffset = panelY + 40;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(tech.getName()).withStyle(ChatFormatting.BOLD), panelX + 70, yOffset, 0xFFFFFFFF); yOffset += 24;

		int scaledKiDamage = (int) (statsData.getKiDamage() * tech.getDamageMultiplier() * tech.getOutputMultiplier());
		String utilKey = tech.getUtility() == KiAttackData.Utility.HEAL ? "gui.dragonminez.technique.heal" : "gui.dragonminez.technique.damage";

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.type").append(": ").append(tr("technique.type." + tech.getKiType().name().toLowerCase())), panelX + 15, yOffset, 0xDDDDDD); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr(utilKey).append(": ").append(txt(String.valueOf(scaledKiDamage))), panelX + 15, yOffset, 0xFFFFFF); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.size").append(": ").append(txt(String.format(Locale.US, "%.1f", tech.getSize()))), panelX + 15, yOffset, 0xFFFFFF); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.speed").append(": ").append(txt(String.format(Locale.US, "%.1f", tech.getSpeed()))), panelX + 15, yOffset, 0xFFFFFF); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.armor_pen").append(": ").append(txt(String.valueOf(tech.getArmorPenetration()))), panelX + 15, yOffset, 0xFFFFFF); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.cast_time").append(": ").append(txt(String.format(Locale.US, "%.1fs", tech.getActualCastTime() / 20.0f))), panelX + 15, yOffset, 0xFFFFFF); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.cooldown").append(": ").append(txt(String.format(Locale.US, "%.1fs", tech.getActualCooldown() / 20.0f))), panelX + 15, yOffset, 0xFFFFFF); yOffset += 16;

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.energy_cost").append(": ").append(txt(String.format(Locale.US, "%.1f", tech.getCalculatedCost(statsData)))), panelX + 15, yOffset, 0xFFAAAA); yOffset += 16;

		renderLearnedFooter(graphics, panelX, yOffset);
	}

	private void renderStrikeTechniqueDetails(GuiGraphics graphics, int panelX, int panelY) {
		StrikeAttackData tech = PredefinedTechniques.STRIKE_REGISTRY.get(selectedSkill);
		if (tech == null) return;

		int yOffset = panelY + 40;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr(tech.getName()).withStyle(ChatFormatting.BOLD), panelX + 70, yOffset, 0xFFFFFFFF); yOffset += 24;

		int scaledStrikeDamage = (int) (statsData.getStrikeDamage() * tech.getDamageMultiplier());

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.type").append(": ").append(tr("technique.type.strike")), panelX + 15, yOffset, 0xDDDDDD); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.damage").append(": ").append(txt(String.valueOf(scaledStrikeDamage))), panelX + 15, yOffset, 0xFFFFFF); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.cast_time").append(": ").append(txt(String.format(Locale.US, "%.1fs", tech.getActualCastTime() / 20.0f))), panelX + 15, yOffset, 0xFFFFFF); yOffset += 12;
		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.cooldown").append(": ").append(txt(String.format(Locale.US, "%.1fs", tech.getActualCooldown() / 20.0f))), panelX + 15, yOffset, 0xFFFFFF); yOffset += 16;

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.technique.energy_cost").append(": ").append(txt(String.format(Locale.US, "%.1f", tech.getCalculatedCost(statsData)))), panelX + 15, yOffset, 0xFFAAAA); yOffset += 16;

		renderLearnedFooter(graphics, panelX, yOffset);
	}

	private void renderLearnedFooter(GuiGraphics graphics, int panelX, int yOffset) {
		boolean learned = statsData.getTechniques().getUnlockedTechniques().containsKey(selectedSkill);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, learned ? tr("gui.dragonminez.skills.already_learned") : tr("gui.dragonminez.skills.not_learned"), panelX + 70, yOffset, learned ? 0xFF55AA55 : 0xFFAAAAAA);

		if (!learned) {
			int tpCost = getUpgradeCost(selectedSkill, 0);
			if (tpCost != Integer.MAX_VALUE && tpCost != -1) {
				TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(tpCost + " TPS"), panelX + 70, yOffset + 12, 0xFFAAAAAA);
			}
		}
	}

	private void renderSkillDetails(GuiGraphics graphics, int panelX, int panelY) {
		Skill skill = statsData.getSkills().getSkill(selectedSkill);
		if (skill == null) skill = new Skill(selectedSkill, 0, false, 10);

		String displayName = Component.translatable("skill.dragonminez." + selectedSkill).getString();
		String description = Component.translatable("skill.dragonminez." + selectedSkill + ".desc").getString();

		int startY = panelY + 40;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(displayName).withStyle(ChatFormatting.BOLD),
				panelX + 72, startY, 0xFFFFFFFF);

		Component levelComp;
		if (skill.getLevel() > 0) levelComp = tr("gui.dragonminez.skills.level", skill.getLevel(), skill.getMaxLevel());
		else levelComp = tr("gui.dragonminez.skills.not_learned");

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, levelComp, panelX + 72, startY + 12, 0xFFAAAAAA);

		if (skill.getLevel() == 0) {
			int cost = getUpgradeCost(selectedSkill, 0);
			if (cost != Integer.MAX_VALUE && cost != -1) {
				TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt("%d TPS".formatted(cost)), panelX + 72, startY + 24, 0xFFAAAAAA);
			}
		} else {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.skills.already_learned"), panelX + 72, startY + 24, 0xFF55AA55);
		}

		List<String> wrappedDesc = wrapText(description, 120);

		int descY = startY + 70;
		int boxX = panelX + 13;
		int boxW = 130;
		int lineHeight = this.font.lineHeight + 2;
		int viewHeight = 6 * lineHeight;
		int totalContentHeight = wrappedDesc.size() * lineHeight;

		maxDescScroll = Math.max(0, totalContentHeight - viewHeight);
		targetDescScroll = Mth.clamp(targetDescScroll, 0, maxDescScroll);
		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();
		currentDescScroll = Mth.lerp(tickDelta * 0.4f, currentDescScroll, targetDescScroll);

		TextUtil.renderScrollableText(graphics, this.font, wrappedDesc, boxX, descY, boxW, viewHeight, currentDescScroll, maxDescScroll, 0xFFCCCCCC);
	}

	private List<String> wrapText(String text, int maxWidth) {
		return TextUtil.wrap(this.font, text, maxWidth, DMZ_FONT);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		int scrollAmount = (int) Math.signum(delta);

		if (uiMouseX >= leftPanelX && uiMouseX <= leftPanelX + 184 && uiMouseY >= leftPanelY + 40 && uiMouseY <= leftPanelY + 239) {
			targetScroll = Mth.clamp(targetScroll - (scrollAmount * SKILL_ITEM_HEIGHT * 2), 0, maxScroll);
			return true;
		}

		int rightPanelX = getUiWidth() - 158;
		int descBoxX = rightPanelX + 10;
		int descBoxY = leftPanelY + 110;
		int descBoxW = 136;
		int descBoxH = 6 * 12;
		if (uiMouseX >= descBoxX && uiMouseX <= descBoxX + descBoxW && uiMouseY >= descBoxY && uiMouseY <= descBoxY + descBoxH) {
			targetDescScroll = Mth.clamp(targetDescScroll - (scrollAmount * 12 * 2), 0, maxDescScroll);
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	private float calculateScrollPercent(double uiMouseY, int startY, int scrollBarHeight) {
		float percent = (float)(uiMouseY - startY) / scrollBarHeight;
		return Mth.clamp(percent, 0.0f, 1.0f);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		int startY = leftPanelY + 30;
		int viewHeight = MAX_VISIBLE_SKILLS * SKILL_ITEM_HEIGHT;
		int scrollBarX = leftPanelX + 135;

		if (maxScroll > 0 && uiMouseX >= scrollBarX - 5 && uiMouseX <= scrollBarX + 10 && uiMouseY >= startY && uiMouseY <= startY + viewHeight) {
			isDraggingScroll = true;
			targetScroll = calculateScrollPercent(uiMouseY, startY, viewHeight) * maxScroll;
			return true;
		}

		int rightPanelX = getUiWidth() - 158;
		int descBoxY = leftPanelY + 110;
		int descBoxH = 6 * 12;
		int descScrollBarX = rightPanelX + 140;
		if (maxDescScroll > 0 && uiMouseX >= descScrollBarX - 5 && uiMouseX <= descScrollBarX + 10 && uiMouseY >= descBoxY && uiMouseY <= descBoxY + descBoxH) {
			isDraggingDescScroll = true;
			targetDescScroll = calculateScrollPercent(uiMouseY, descBoxY, descBoxH) * maxDescScroll;
			return true;
		}

		List<String> skillNames = getVisibleSkillNames();
		if (uiMouseX >= leftPanelX + 10 && uiMouseX <= leftPanelX + 100 && uiMouseY >= startY && uiMouseY <= startY + viewHeight) {
			int index = (int) ((uiMouseY - startY + currentScroll) / SKILL_ITEM_HEIGHT);

			if (index >= 0 && index < skillNames.size()) {
				selectedSkill = skillNames.get(index);
				targetDescScroll = 0;
				currentDescScroll = 0;
				refreshButtons();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (isDraggingScroll && maxScroll > 0) {
			double uiMouseY = toUiY(mouseY);
			int centerY = getUiHeight() / 2;
			int startY = (centerY - 105) + 30;
			int viewHeight = MAX_VISIBLE_SKILLS * SKILL_ITEM_HEIGHT;

			targetScroll = calculateScrollPercent(uiMouseY, startY, viewHeight) * maxScroll;
			return true;
		}
		if (isDraggingDescScroll && maxDescScroll > 0) {
			double uiMouseY = toUiY(mouseY);
			int descBoxY = (getUiHeight() / 2 - 105) + 110;
			int descBoxH = 6 * 12;

			targetDescScroll = calculateScrollPercent(uiMouseY, descBoxY, descBoxH) * maxDescScroll;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (isDraggingScroll) {
			isDraggingScroll = false;
			return true;
		}
		if (isDraggingDescScroll) {
			isDraggingDescScroll = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void renderMasterEntity(GuiGraphics graphics, int x, int y, float mouseX, float mouseY) {
		if (masterEntity == null) return;

		float xRotation = (float) Math.atan((double) ((float) y - mouseY) / 40.0F);
		float yRotation = (float) Math.atan((double) ((float) x - mouseX) / 40.0F);

		Quaternionf pose = (new Quaternionf()).rotateZ((float) Math.PI);
		Quaternionf cameraOrientation = (new Quaternionf()).rotateX(xRotation * 20.0F * ((float) Math.PI / 180F));
		pose.mul(cameraOrientation);

		float yBodyRotO = masterEntity.yBodyRot;
		float yRotO = masterEntity.getYRot();
		float xRotO = masterEntity.getXRot();
		float yHeadRotO = masterEntity.yHeadRotO;
		float yHeadRot = masterEntity.yHeadRot;

		masterEntity.yBodyRot = 180.0F + yRotation * 20.0F;
		masterEntity.setYRot(180.0F + yRotation * 40.0F);
		masterEntity.setXRot(-xRotation * 20.0F);
		masterEntity.yHeadRot = masterEntity.getYRot();
		masterEntity.yHeadRotO = masterEntity.getYRot();

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 150.0D);
		InventoryScreen.renderEntityInInventory(graphics, x, y, 100, pose, cameraOrientation, masterEntity);
		graphics.pose().popPose();

		masterEntity.yBodyRot = yBodyRotO;
		masterEntity.setYRot(yRotO);
		masterEntity.setXRot(xRotO);
		masterEntity.yHeadRotO = yHeadRotO;
		masterEntity.yHeadRot = yHeadRot;
	}

	public MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	public MutableComponent txt(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}
}