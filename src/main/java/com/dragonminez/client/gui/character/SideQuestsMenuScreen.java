package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.C2S.AcceptSideQuestC2S;
import com.dragonminez.common.network.C2S.ClaimSideQuestRewardC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.sidequest.SideQuest;
import com.dragonminez.common.quest.sidequest.SideQuestData;
import com.dragonminez.common.quest.sidequest.SideQuestManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class SideQuestsMenuScreen extends BaseMenuScreen {
	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menubig.png");
	private static final ResourceLocation MENU_SMALL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menusmall.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");

	private TexturedTextButton actionButton;
	private static final int QUEST_ITEM_HEIGHT = 20;
	private static final int MAX_VISIBLE_QUESTS = 8;

	private StatsData statsData;
	private int tickCount = 0;

	private final List<SideQuest> availableQuests = new ArrayList<>();
	private SideQuest selectedQuest = null;
	private int scrollOffset = 0;
	private int maxScroll = 0;

	private int objectivesScrollOffset = 0;
	private int maxObjectivesScroll = 0;
	private int rewardsScrollOffset = 0;
	private int maxRewardsScroll = 0;
	private int objAreaX, objAreaY, objAreaWidth, objAreaHeight;
	private int rewardsAreaX, rewardsAreaY, rewardsAreaWidth, rewardsAreaHeight;

	private int descriptionScrollOffset = 0;
	private int maxDescriptionScroll = 0;
	private int descAreaX, descAreaY, descAreaWidth, descAreaHeight;
	private static final int MAX_DESC_LINES = 5;

	private static DetailPage currentPage = DetailPage.OBJECTIVES;

	// Category filter why not
	private final List<String> categories = new ArrayList<>();
	private int currentCategoryIndex = 0;

	public SideQuestsMenuScreen() {
		super(Component.translatable("gui.dragonminez.story.sidequests.title"));
	}

	private enum DetailPage { OBJECTIVES, REWARDS }

	@Override
	protected void init() {
		super.init();
		updateStatsData();
		loadAvailableQuests();
		updateQuestsList();

		this.scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
		if (this.selectedQuest != null) refreshButtons();
	}

	private void loadAvailableQuests() {
		availableQuests.clear();
		categories.clear();
		if (statsData == null) return;

		Map<String, SideQuest> allQuests = SideQuestManager.getClientSideQuests();

		if (allQuests == null || allQuests.isEmpty()) {
			return;
		}

		SideQuestData sqData = statsData.getSideQuestData();

		// Build category list and filter: only show accepted (in-progress) or completed quests
		categories.add("all");
		for (SideQuest quest : allQuests.values()) {
			boolean isAccepted = sqData.isQuestAccepted(quest.getId());
			if (!isAccepted) continue; // Skip quests that haven't been accepted

			String cat = quest.getCategory();
			if (!categories.contains(cat)) {
				categories.add(cat);
			}
		}

		// Filter by current category, only including accepted quests
		String selectedCategory = getCurrentCategory();
		for (SideQuest quest : allQuests.values()) {
			boolean isAccepted = sqData.isQuestAccepted(quest.getId());
			if (!isAccepted) continue; // Skip quests that haven't been accepted

			if (selectedCategory.equals("all") || quest.getCategory().equals(selectedCategory)) {
				availableQuests.add(quest);
			}
		}

		// Sort: in-progress first, then completed
		availableQuests.sort((a, b) -> {
			int orderA = getSortOrder(a, sqData);
			int orderB = getSortOrder(b, sqData);
			if (orderA != orderB) return Integer.compare(orderA, orderB);
			return a.getName().compareTo(b.getName());
		});
	}

	private int getSortOrder(SideQuest quest, SideQuestData sqData) {
		if (sqData.isQuestAccepted(quest.getId()) && !sqData.isQuestCompleted(quest.getId())) return 0; // In progress
		if (sqData.isQuestCompleted(quest.getId())) return 1; // Completed
		return 2; // Fallback
	}

	private String getCurrentCategory() {
		if (categories.isEmpty()) return "all";
		if (currentCategoryIndex >= categories.size()) currentCategoryIndex = 0;
		return categories.get(currentCategoryIndex);
	}

	private void initCategoryButtons() {
		if (categories.size() <= 1) return;

		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;
		int bottomPanelY = leftPanelY + 213;

		if (currentCategoryIndex > 0) {
			CustomTextureButton leftArrow = createArrowButton(leftPanelX + 10, bottomPanelY - 25, true, btn -> {
				currentCategoryIndex--;
				selectedQuest = null;
				scrollOffset = 0;
				loadAvailableQuests();
				updateQuestsList();
				refreshButtons();
			});
			this.addRenderableWidget(leftArrow);
		}

		if (currentCategoryIndex < categories.size() - 1) {
			CustomTextureButton rightArrow = createArrowButton(leftPanelX + 122, bottomPanelY - 25, false, btn -> {
				currentCategoryIndex++;
				selectedQuest = null;
				scrollOffset = 0;
				loadAvailableQuests();
				updateQuestsList();
				refreshButtons();
			});
			this.addRenderableWidget(rightArrow);
		}
	}

	private void initDetailNavigationButtons() {
		if (selectedQuest == null) return;

		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;
		int bottomPanelY = leftPanelY + 213;

		switch (currentPage) {
			case REWARDS -> {
				CustomTextureButton leftArrow = createArrowButton(rightPanelX + 10, bottomPanelY - 25, true, btn -> {
					currentPage = DetailPage.OBJECTIVES;
					refreshButtons();
				});
				this.addRenderableWidget(leftArrow);
			}
			case OBJECTIVES -> {
				CustomTextureButton rightArrow = createArrowButton(rightPanelX + 122, bottomPanelY - 25, false, btn -> {
					currentPage = DetailPage.REWARDS;
					refreshButtons();
				});
				this.addRenderableWidget(rightArrow);
			}
		}
	}

	private void initTabButtons() {
		int centerX = getUiWidth() / 2;
		int topY = getUiHeight() / 2 - 115;

		// "Sagas" tab — switches back to QuestsMenuScreen
		TexturedTextButton sagasTab = new TexturedTextButton.Builder()
				.position(centerX - 40, topY)
				.size(38, 14)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(Component.translatable("gui.dragonminez.story.sidequests.tab.sagas"))
				.onPress(btn -> switchMenu(new QuestsMenuScreen()))
				.build();
		this.addRenderableWidget(sagasTab);

		// "Side Quests" tab — current screen (inactive look)
		TexturedTextButton sideQuestsTab = new TexturedTextButton.Builder()
				.position(centerX + 2, topY)
				.size(38, 14)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(Component.translatable("gui.dragonminez.story.sidequests.tab.sidequests"))
				.onPress(btn -> {})
				.build();
		sideQuestsTab.active = false;
		this.addRenderableWidget(sideQuestsTab);
	}

	private void initActionButton() {
		if (selectedQuest == null || statsData == null) return;

		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int rightPanelY = centerY - 105;

		SideQuestData sqData = statsData.getSideQuestData();
		boolean isAccepted = sqData.isQuestAccepted(selectedQuest.getId());
		boolean isCompleted = sqData.isQuestCompleted(selectedQuest.getId());

		Component buttonText;
		boolean buttonActive = true;
		boolean isClaimAction = false;

		if (isCompleted) {
			// Check for unclaimed rewards
			boolean hasUnclaimedRewards = false;
			for (int i = 0; i < selectedQuest.getRewards().size(); i++) {
				if (!sqData.isRewardClaimed(selectedQuest.getId(), i)) {
					hasUnclaimedRewards = true;
					break;
				}
			}
			if (hasUnclaimedRewards) {
				buttonText = Component.translatable("gui.dragonminez.quests.claim_rewards");
				isClaimAction = true;
			} else {
				return; // All rewards claimed, no button needed
			}
		} else if (isAccepted) {
			// Quest in progress — show start button for kill objectives (same as saga quests)
			boolean hasKillObjectives = selectedQuest.getObjectives().stream()
					.anyMatch(obj -> obj.getType() == QuestObjective.ObjectiveType.KILL);
			if (hasKillObjectives) {
				buttonText = Component.translatable("gui.dragonminez.quests.start");
			} else {
				return; // No actionable button for non-kill in-progress quests
			}
		} else {
			return; // Not in a state that needs a button
		}

		boolean finalIsClaimAction = isClaimAction;

		actionButton = new TexturedTextButton.Builder()
				.position(rightPanelX + 35, rightPanelY + 212)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(buttonText)
				.onPress(btn -> {
					if (finalIsClaimAction) {
						NetworkHandler.sendToServer(new ClaimSideQuestRewardC2S(selectedQuest.getId()));
						this.onClose();
					} else {
						// Start quest (spawn kill entities) — reuse accept packet since it handles spawning
						boolean isHard = ConfigManager.getUserConfig().getHud().getStoryHardDifficulty();
						NetworkHandler.sendToServer(new AcceptSideQuestC2S(selectedQuest.getId(), isHard));
						this.onClose();
					}
					refreshButtons();
				})
				.build();

		actionButton.active = buttonActive;
		this.addRenderableWidget(actionButton);
	}

	private CustomTextureButton createArrowButton(int x, int y, boolean isLeft, CustomTextureButton.OnPress onPress) {
		return new CustomTextureButton.Builder()
				.position(x, y)
				.size(10, 15)
				.texture(BUTTONS_TEXTURE)
				.textureSize(8, 14)
				.textureCoords(isLeft ? 32 : 20, 0, isLeft ? 32 : 20, 14)
				.onPress(onPress)
				.build();
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

				Map<String, SideQuest> clientQuests = SideQuestManager.getClientSideQuests();
				int newSize = (clientQuests != null) ? clientQuests.size() : 0;

				if (newSize != availableQuests.size()) {
					loadAvailableQuests();
					if (selectedQuest != null) {
						// Re-find selected quest after reload
						selectedQuest = SideQuestManager.getClientSideQuest(selectedQuest.getId());
					}
					refreshButtons();
				}

				updateQuestsList();
			});
		}
	}

	private void updateQuestsList() {
		maxScroll = Math.max(0, availableQuests.size() - MAX_VISIBLE_QUESTS);
	}

	private void refreshButtons() {
		this.clearWidgets();
		initCategoryButtons();
		initNavigationButtons();
		initTabButtons();
		initActionButton();
		initDetailNavigationButtons();
	}

	@Override
	public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);

		renderPlayerModel(graphics, getUiWidth() / 2 + 5, getUiHeight() / 2 + 70, 75, uiMouseX, uiMouseY);

		renderLeftPanel(graphics, uiMouseX, uiMouseY);
		renderRightPanel(graphics, uiMouseX, uiMouseY);

		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	private void renderLeftPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(MENU_BIG, 12, centerY - 105, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, 29, centerY - 95, 142, 22, 107, 21, 256, 256);

		renderCategoryTitle(graphics, leftPanelX, leftPanelY);
		renderQuestsList(graphics, leftPanelX, leftPanelY, mouseX, mouseY);
	}

	private void renderCategoryTitle(GuiGraphics graphics, int panelX, int panelY) {
		int titleY = panelY + 10;
		String categoryName = getCurrentCategory();
		String displayName;

		if (categoryName.equals("all")) {
			displayName = Component.translatable("gui.dragonminez.story.sidequests.category.all").getString();
		} else {
			displayName = Component.translatable("gui.dragonminez.story.sidequests.category." + categoryName).getString();
		}

		drawCenteredStringWithBorder(graphics,
				Component.literal(displayName).withStyle(ChatFormatting.BOLD),
				panelX + 70, titleY + 6, 0xFFFFFFFF);
	}

	private void renderQuestsList(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
		int startY = panelY + 30;
		int visibleStart = scrollOffset;
		int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_QUESTS, availableQuests.size());

		graphics.enableScissor(
				toScreenCoord(panelX + 5),
				toScreenCoord(startY),
				toScreenCoord(panelX + 144),
				toScreenCoord(startY + (MAX_VISIBLE_QUESTS * QUEST_ITEM_HEIGHT))
		);

		SideQuestData sqData = statsData != null ? statsData.getSideQuestData() : null;

		for (int i = visibleStart; i < visibleEnd; i++) {
			SideQuest quest = availableQuests.get(i);
			int itemY = startY + ((i - visibleStart) * QUEST_ITEM_HEIGHT);

			boolean isSelected = selectedQuest != null && selectedQuest.getId().equals(quest.getId());
			boolean isHovered = mouseX >= panelX + 10 && mouseX <= panelX + 110 &&
					mouseY >= itemY && mouseY <= itemY + QUEST_ITEM_HEIGHT - 5;

			int color;
			if (sqData != null && sqData.isQuestCompleted(quest.getId())) {
				color = isSelected ? 0xFFFFAA00 : (isHovered ? 0xFF88CC88 : 0xFF66AA66);
			} else {
				// In progress
				color = isSelected ? 0xFFFFAA00 : (isHovered ? 0xFFCCCC88 : 0xFFAAAA66);
			}

			drawStringWithBorder(graphics, Component.literal(quest.getName()),
					panelX + 15, itemY + 5, color);

			// Status marker
			if (sqData != null) {
				if (sqData.isQuestCompleted(quest.getId())) {
					drawStringWithBorder(graphics, Component.literal("✓"),
							panelX + 130 - this.font.width("✓"), itemY + 5, 0xFF00FF00);
				} else if (sqData.isQuestAccepted(quest.getId())) {
					drawStringWithBorder(graphics, Component.literal("●"),
							panelX + 130 - this.font.width("●"), itemY + 5, 0xFFFFFF00);
				}
			}
		}

		graphics.disableScissor();

		if (maxScroll > 0) {
			int scrollBarX = panelX + 140;
			int scrollBarHeight = MAX_VISIBLE_QUESTS * QUEST_ITEM_HEIGHT;
			int totalItems = availableQuests.size();

			graphics.fill(scrollBarX, startY, scrollBarX + 3, startY + scrollBarHeight, 0xFF333333);

			float scrollPercent = (float) scrollOffset / maxScroll;
			float visiblePercent = (float) MAX_VISIBLE_QUESTS / totalItems;
			int indicatorHeight = Math.max(20, (int) (scrollBarHeight * visiblePercent));
			int indicatorY = startY + (int) ((scrollBarHeight - indicatorHeight) * scrollPercent);

			graphics.fill(scrollBarX, indicatorY, scrollBarX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}
	}

	private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int rightPanelY = centerY - 105;

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(MENU_SMALL, getUiWidth() - 158, centerY + 76, 0, 95, 145, 58, 256, 256);
		graphics.blit(MENU_BIG, getUiWidth() - 158, centerY - 105, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, getUiWidth() - 141, centerY - 95, 142, 22, 107, 21, 256, 256);

		drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.character_stats.info").withStyle(ChatFormatting.BOLD),
				rightPanelX + 70, rightPanelY + 16, 0xFFFFD700);

		if (selectedQuest != null && statsData != null) {
			switch (currentPage) {
				case OBJECTIVES -> renderQuestDetails(graphics, rightPanelX, rightPanelY);
				case REWARDS -> renderQuestRewards(graphics, rightPanelX, rightPanelY);
			}
		}
	}

	private void renderQuestDetails(GuiGraphics graphics, int panelX, int panelY) {
		if (selectedQuest == null) return;

		SideQuestData sqData = statsData.getSideQuestData();
		boolean isCompleted = sqData.isQuestCompleted(selectedQuest.getId());
		boolean isAccepted = sqData.isQuestAccepted(selectedQuest.getId());

		int startY = panelY + 35;

		drawCenteredStringWithBorder(graphics, Component.literal(selectedQuest.getName()).withStyle(ChatFormatting.BOLD),
				panelX + 70, startY, 0xFFFFFFFF);

		String statusText;
		int statusColor;
		if (isCompleted) {
			statusText = Component.translatable("gui.dragonminez.quests.status.complete").getString();
			statusColor = 0xFF00FF00;
		} else {
			statusText = Component.translatable("gui.dragonminez.story.sidequests.status.in_progress").getString();
			statusColor = 0xFFFFFF00;
		}

		drawCenteredStringWithBorder(graphics, Component.literal(statusText), panelX + 70, startY + 15, statusColor);

		int descY = startY + 32;
		List<String> wrappedDesc = wrapText(selectedQuest.getDescription(), 120);

		int descVisibleHeight = MAX_DESC_LINES * 10;
		int totalDescHeight = wrappedDesc.size() * 10;
		this.maxDescriptionScroll = Math.max(0, totalDescHeight - descVisibleHeight);

		this.descAreaX = panelX + 5;
		this.descAreaY = descY;
		this.descAreaWidth = 140;
		this.descAreaHeight = descVisibleHeight;

		graphics.enableScissor(
				toScreenCoord(panelX + 5),
				toScreenCoord(descY),
				toScreenCoord(panelX + 144),
				toScreenCoord(descY + descVisibleHeight)
		);

		int currentDescY = descY - descriptionScrollOffset;
		for (String line : wrappedDesc) {
			drawStringWithBorder(graphics, Component.literal(line), panelX + 15, currentDescY, 0xFFCCCCCC);
			currentDescY += 10;
		}

		graphics.disableScissor();

		if (maxDescriptionScroll > 0) {
			int scrollBarX = panelX + 138;
			graphics.fill(scrollBarX, descY, scrollBarX + 2, descY + descVisibleHeight, 0xFF333333);
			float scrollPercent = (float) descriptionScrollOffset / maxDescriptionScroll;
			int indicatorHeight = Math.max(10, (int) ((float) descVisibleHeight / totalDescHeight * descVisibleHeight));
			int indicatorY = descY + (int) ((descVisibleHeight - indicatorHeight) * scrollPercent);
			graphics.fill(scrollBarX, indicatorY, scrollBarX + 2, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}

		int objTitleY = descY + descVisibleHeight + 5;

		drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.quests.objectives").withStyle(ChatFormatting.BOLD),
				panelX + 15, objTitleY, 0xFFFFD700);

		int objStartY = objTitleY + 15;
		int objVisibleHeight = 65;

		this.objAreaX = panelX + 5;
		this.objAreaY = objStartY;
		this.objAreaWidth = 140;
		this.objAreaHeight = objVisibleHeight;

		List<QuestObjective> objectives = selectedQuest.getObjectives();

		int totalContentHeight = 0;
		for (QuestObjective objective : objectives) {
			int progress = sqData.getObjectiveProgress(selectedQuest.getId(), objectives.indexOf(objective));
			String objText = getObjectiveText(objective, progress);
			List<String> wrappedObj = wrapText(objText, 100);
			totalContentHeight += (wrappedObj.size() * 10) + 2;
		}

		this.maxObjectivesScroll = Math.max(0, totalContentHeight - objVisibleHeight);

		graphics.enableScissor(
				toScreenCoord(panelX + 5),
				toScreenCoord(objStartY),
				toScreenCoord(panelX + 144),
				toScreenCoord(objStartY + objVisibleHeight)
		);

		int currentRenderY = objStartY - objectivesScrollOffset;

		for (int i = 0; i < objectives.size(); i++) {
			QuestObjective objective = objectives.get(i);
			int progress = sqData.getObjectiveProgress(selectedQuest.getId(), i);
			boolean objCompleted = progress >= objective.getRequired();

			String objText = getObjectiveText(objective, progress);
			String marker = objCompleted ? "✓" : "✕";
			int markerColor = objCompleted ? 0xFF00FF00 : 0xFFFF0000;

			drawStringWithBorder(graphics, Component.literal(marker), panelX + 15, currentRenderY, markerColor);

			List<String> wrappedObj = wrapText(objText, 105);
			for (String line : wrappedObj) {
				drawStringWithBorder(graphics, Component.literal(line), panelX + 30, currentRenderY, 0xFFCCCCCC);
				currentRenderY += 10;
			}
			currentRenderY += 2;
		}

		graphics.disableScissor();

		if (maxObjectivesScroll > 0) {
			int scrollBarX = panelX + 138;
			graphics.fill(scrollBarX, objStartY, scrollBarX + 2, objStartY + objVisibleHeight, 0xFF333333);
			float scrollPercent = (float) objectivesScrollOffset / maxObjectivesScroll;
			int indicatorHeight = Math.max(10, (int) ((float) objVisibleHeight / totalContentHeight * objVisibleHeight));
			int indicatorY = objStartY + (int) ((objVisibleHeight - indicatorHeight) * scrollPercent);
			graphics.fill(scrollBarX, indicatorY, scrollBarX + 2, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}
	}

	private void renderQuestRewards(GuiGraphics graphics, int panelX, int panelY) {
		if (selectedQuest == null) return;

		SideQuestData sqData = statsData.getSideQuestData();
		boolean isCompleted = sqData.isQuestCompleted(selectedQuest.getId());

		int startY = panelY + 35;

		drawCenteredStringWithBorder(graphics, Component.literal(selectedQuest.getName()).withStyle(ChatFormatting.BOLD),
				panelX + 70, startY, 0xFFFFFFFF);

		String statusKey = isCompleted ? "gui.dragonminez.quests.status.complete" : "gui.dragonminez.quests.status.incomplete";
		int statusColor = isCompleted ? 0xFF00FF00 : 0xFFFFFF00;

		drawCenteredStringWithBorder(graphics, Component.translatable(statusKey), panelX + 70, startY + 15, statusColor);

		int rewardsTitleY = startY + 32;

		drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.quests.rewards").withStyle(ChatFormatting.BOLD),
				panelX + 15, rewardsTitleY, 0xFFFFD700);

		int rewardsStartY = rewardsTitleY + 15;
		int rewardsVisibleHeight = 65;

		this.rewardsAreaX = panelX + 5;
		this.rewardsAreaY = rewardsStartY;
		this.rewardsAreaWidth = 140;
		this.rewardsAreaHeight = rewardsVisibleHeight;

		List<QuestReward> rewards = selectedQuest.getRewards();

		int totalContentHeight = 0;
		for (QuestReward reward : rewards) {
			String rewardText = reward.getDescription().getString();
			List<String> wrappedObj = wrapText(rewardText, 100);
			totalContentHeight += (wrappedObj.size() * 10) + 2;
		}

		this.maxRewardsScroll = Math.max(0, totalContentHeight - rewardsVisibleHeight);

		graphics.enableScissor(
				toScreenCoord(panelX + 5),
				toScreenCoord(rewardsStartY),
				toScreenCoord(panelX + 144),
				toScreenCoord(rewardsStartY + rewardsVisibleHeight)
		);

		int currentRenderY = rewardsStartY - rewardsScrollOffset;

		for (int i = 0; i < rewards.size(); i++) {
			QuestReward reward = rewards.get(i);
			boolean rewardClaimed = sqData.isRewardClaimed(selectedQuest.getId(), i);

			String rewardText = reward.getDescription().getString();
			String marker = rewardClaimed ? "✓" : "✕";
			int markerColor = rewardClaimed ? 0xFF00FF00 : 0xFFFF0000;

			drawStringWithBorder(graphics, Component.literal(marker), panelX + 15, currentRenderY, markerColor);

			List<String> wrappedReward = wrapText(rewardText, 105);
			for (String line : wrappedReward) {
				drawStringWithBorder(graphics, Component.literal(line), panelX + 30, currentRenderY, 0xFFCCCCCC);
				currentRenderY += 10;
			}
			currentRenderY += 2;
		}

		graphics.disableScissor();

		if (maxRewardsScroll > 0) {
			int scrollBarX = panelX + 138;
			graphics.fill(scrollBarX, rewardsStartY, scrollBarX + 2, rewardsStartY + rewardsVisibleHeight, 0xFF333333);
			float scrollPercent = (float) rewardsScrollOffset / maxRewardsScroll;
			int indicatorHeight = Math.max(10, (int) ((float) rewardsVisibleHeight / totalContentHeight * rewardsVisibleHeight));
			int indicatorY = rewardsStartY + (int) ((rewardsVisibleHeight - indicatorHeight) * scrollPercent);
			graphics.fill(scrollBarX, indicatorY, scrollBarX + 2, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}
	}

	private String getObjectiveText(QuestObjective objective, int currentProgress) {
		String description = Component.translatable(objective.getDescription()).getString();
		int required = objective.getRequired();

		if (objective.getType() == QuestObjective.ObjectiveType.KILL ||
				objective.getType() == QuestObjective.ObjectiveType.ITEM) {
			return description + " (" + currentProgress + "/" + required + ")";
		}

		return description;
	}

	private List<String> wrapText(String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		String[] words = text.split(" ");
		StringBuilder currentLine = new StringBuilder();

		for (String word : words) {
			String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
			if (this.font.width(testLine) <= maxWidth) {
				if (!currentLine.isEmpty()) currentLine.append(" ");
				currentLine.append(word);
			} else {
				if (!currentLine.isEmpty()) {
					lines.add(currentLine.toString());
					currentLine = new StringBuilder(word);
				} else {
					lines.add(word);
				}
			}
		}

		if (!currentLine.isEmpty()) {
			lines.add(currentLine.toString());
		}

		return lines;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		if (uiMouseX >= leftPanelX && uiMouseX <= leftPanelX + 148 &&
				uiMouseY >= leftPanelY + 40 && uiMouseY <= leftPanelY + 219) {
			scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) delta));
			return true;
		}

		if (selectedQuest != null && maxDescriptionScroll > 0 &&
				uiMouseX >= descAreaX && uiMouseX <= descAreaX + descAreaWidth &&
				uiMouseY >= descAreaY && uiMouseY <= descAreaY + descAreaHeight) {
			int scrollAmount = (int) (delta * 10);
			descriptionScrollOffset = Math.max(0, Math.min(maxDescriptionScroll, descriptionScrollOffset - scrollAmount));
			return true;
		}

		if (selectedQuest != null && maxObjectivesScroll > 0 &&
				uiMouseX >= objAreaX && uiMouseX <= objAreaX + objAreaWidth &&
				uiMouseY >= objAreaY && uiMouseY <= objAreaY + objAreaHeight) {
			int scrollAmount = (int) (delta * 10);
			objectivesScrollOffset = Math.max(0, Math.min(maxObjectivesScroll, objectivesScrollOffset - scrollAmount));
			return true;
		}

		if (selectedQuest != null && maxRewardsScroll > 0 &&
				uiMouseX >= rewardsAreaX && uiMouseX <= rewardsAreaX + rewardsAreaWidth &&
				uiMouseY >= rewardsAreaY && uiMouseY <= rewardsAreaY + rewardsAreaHeight) {
			int scrollAmount = (int) (delta * 10);
			rewardsScrollOffset = Math.max(0, Math.min(maxRewardsScroll, rewardsScrollOffset - scrollAmount));
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;
		int startY = leftPanelY + 30;
		int visibleStart = scrollOffset;
		int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_QUESTS, availableQuests.size());

		for (int i = visibleStart; i < visibleEnd; i++) {
			int itemY = startY + ((i - visibleStart) * QUEST_ITEM_HEIGHT);

			if (uiMouseX >= leftPanelX + 10 && uiMouseX <= leftPanelX + 110 && uiMouseY >= itemY && uiMouseY <= itemY + QUEST_ITEM_HEIGHT) {
				SideQuest quest = availableQuests.get(i);
				selectedQuest = quest;
				descriptionScrollOffset = 0;
				objectivesScrollOffset = 0;
				rewardsScrollOffset = 0;
				refreshButtons();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void renderPlayerModel(GuiGraphics graphics, int x, int y, int scale, float mouseX, float mouseY) {
		LivingEntity player = Minecraft.getInstance().player;
		if (player == null) return;

		int adjustedScale = getAdjustedModelScale(scale);

		float xRotation = (float) Math.atan((y - mouseY) / 40.0F);
		float yRotation = (float) Math.atan((x - mouseX) / 40.0F);
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
}


