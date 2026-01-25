package com.dragonminez.client.gui.character;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.C2S.ClaimRewardC2S;
import com.dragonminez.common.network.C2S.StartQuestC2S;
import com.dragonminez.common.network.C2S.UnlockSagaC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.*;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class QuestsMenuScreen extends BaseMenuScreen {

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
	private static final Map<String, Long> QUEST_COOLDOWNS = new HashMap<>();
	private static final long START_QUEST_COOLDOWN = 30000;
	private long lastClickTime = 0;

    private int currentSagaIndex = 0;
    private final List<Saga> availableSagas = new ArrayList<>();
    private Quest selectedQuest = null;
    private int scrollOffset = 0;
    private int maxScroll = 0;
	private int objectivesScrollOffset = 0;
	private int maxObjectivesScroll = 0;
	private int objAreaX, objAreaY, objAreaWidth, objAreaHeight;

    public QuestsMenuScreen(int oldGuiScale) {
        super(Component.translatable("gui.dragonminez.quests.title"), oldGuiScale);
    }

    @Override
    protected void init() {
        super.init();
        updateStatsData();
        loadAvailableSagas();
        initSagaNavigationButtons();
        updateQuestsList();
    }

    private void loadAvailableSagas() {
        availableSagas.clear();
        if (statsData == null) return;

        Map<String, Saga> allSagas = SagaManager.getClientSagas();

        if (allSagas.isEmpty()) {
            LogUtil.warn(Env.CLIENT, "No sagas loaded from SagaManager");
            return;
        }

        availableSagas.addAll(allSagas.values());

        availableSagas.sort((s1, s2) -> {
            if (s1.getRequirements() == null) return -1;
            if (s2.getRequirements() == null) return 1;

            String prev1 = s1.getRequirements().getPreviousSagaId();
            String prev2 = s2.getRequirements().getPreviousSagaId();

            if (prev1 == null || prev1.isEmpty()) return -1;
            if (prev2 == null || prev2.isEmpty()) return 1;

            return 0;
        });

        if (currentSagaIndex >= availableSagas.size()) {
            currentSagaIndex = Math.max(0, availableSagas.size() - 1);
        }
    }

	private void initSagaNavigationButtons() {
		if (availableSagas.isEmpty()) return;

		int leftPanelX = 12;
		int centerY = this.height / 2;
		int leftPanelY = centerY - 105;
		int bottomPanelY = leftPanelY + 213;

		if (currentSagaIndex > 0) {
			CustomTextureButton leftArrow = createArrowButton(leftPanelX + 10, bottomPanelY - 25, true, btn -> {
				currentSagaIndex--;
				selectedQuest = null;
				scrollOffset = 0;
				objectivesScrollOffset = 0;
				updateQuestsList();
				refreshButtons();
			});
			this.addRenderableWidget(leftArrow);
		}

		if (currentSagaIndex < availableSagas.size() - 1) {
			Saga currentSaga = availableSagas.get(currentSagaIndex);
			Saga nextSaga = availableSagas.get(currentSagaIndex + 1);

			boolean canAdvance = statsData.getQuestData().isSagaUnlocked(nextSaga.getId())
					|| isSagaCompleted(currentSaga);

			if (canAdvance) {
				CustomTextureButton rightArrow = createArrowButton(leftPanelX + 122, bottomPanelY - 25, false, btn -> {
					currentSagaIndex++;

					if (!statsData.getQuestData().isSagaUnlocked(nextSaga.getId())) {
						NetworkHandler.sendToServer(new UnlockSagaC2S(nextSaga.getId()));
					}

					selectedQuest = null;
					scrollOffset = 0;
					objectivesScrollOffset = 0;
					updateQuestsList();
					refreshButtons();
				});
				this.addRenderableWidget(rightArrow);
			}
		}
	}

	private boolean isSagaCompleted(Saga saga) {
		if (statsData == null || saga == null) return false;
		QuestData data = statsData.getQuestData();

		for (Quest quest : saga.getQuests()) {
			if (!data.isQuestCompleted(saga.getId(), quest.getId())) {
				return false;
			}
		}
		return true;
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
                updateQuestsList();
            });
        }
    }

    private void updateQuestsList() {
        List<Quest> quests = getVisibleQuests();
        maxScroll = Math.max(0, quests.size() - MAX_VISIBLE_QUESTS);
    }

    private List<Quest> getVisibleQuests() {
        if (availableSagas.isEmpty() || currentSagaIndex >= availableSagas.size()) {
            return new ArrayList<>();
        }

        Saga currentSaga = availableSagas.get(currentSagaIndex);
        List<Quest> allQuests = currentSaga.getQuests();
        List<Quest> visibleQuests = new ArrayList<>();

        if (statsData == null) return allQuests;

        QuestData questData = statsData.getQuestData();

        for (int i = 0; i < allQuests.size(); i++) {
            Quest quest = allQuests.get(i);
            boolean isCompleted = questData.isQuestCompleted(currentSaga.getId(), quest.getId());

            if (isCompleted) {
                visibleQuests.add(quest);
            } else {
                visibleQuests.add(quest);
                if (i < allQuests.size() - 1) {
                    visibleQuests.add(null);
                }
                break;
            }
        }

        return visibleQuests;
    }

    private void refreshButtons() {
        this.clearWidgets();
        initSagaNavigationButtons();
        initNavigationButtons();
        initActionButton();
    }

    private void initActionButton() {
		if (selectedQuest == null || statsData == null) return;

		int rightPanelX = this.width - 158;
		int centerY = this.height / 2;
		int rightPanelY = centerY - 105;

		QuestData questData = statsData.getQuestData();
		Saga currentSaga = availableSagas.get(currentSagaIndex);
		boolean isCompleted = questData.isQuestCompleted(currentSaga.getId(), selectedQuest.getId());
		boolean canStart = canStartQuest(selectedQuest, currentSaga.getId());

		Component buttonText;
		boolean buttonActive = true;
		boolean isClaimAction = false;

		String cooldownKey = currentSaga.getId() + ":" + selectedQuest.getId();

		if (isCompleted) {
			boolean hasUnclaimedRewards = false;
			for (int i = 0; i < selectedQuest.getRewards().size(); i++) {
				if (!questData.isRewardClaimed(currentSaga.getId(), selectedQuest.getId(), i)) {
					hasUnclaimedRewards = true;
					break;
				}
			}

			if (hasUnclaimedRewards) {
				buttonText = Component.translatable("gui.dragonminez.quests.claim_rewards");
				isClaimAction = true;
				buttonActive = true;
			} else {
				return;
			}
		} else if (canStart) {
			buttonText = Component.translatable("gui.dragonminez.quests.start");
			isClaimAction = false;

			long now = System.currentTimeMillis();
			long lastRun = QUEST_COOLDOWNS.getOrDefault(cooldownKey, 0L);

			if (now - lastRun < START_QUEST_COOLDOWN) {
				buttonActive = false;
			} else {
				buttonActive = true;
			}
		} else {
			return;
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
					long now = System.currentTimeMillis();
					if (now - lastClickTime < 500) return;
					lastClickTime = now;

					if (finalIsClaimAction) {
						NetworkHandler.sendToServer(new ClaimRewardC2S(
								currentSaga.getId(),
								selectedQuest.getId()
						));
						this.onClose();
					} else {
						QUEST_COOLDOWNS.put(cooldownKey, now);

						boolean isHard = ConfigManager.getUserConfig().getHud().isStoryHardDifficulty();
						NetworkHandler.sendToServer(new StartQuestC2S(
								currentSaga.getId(),
								selectedQuest.getId(),
								isHard
						));
						this.onClose();
					}
					refreshButtons();
				})
				.build();

		actionButton.active = buttonActive;
		this.addRenderableWidget(actionButton);
	}

    private boolean canStartQuest(Quest quest, String sagaId) {
        if (statsData == null) return false;

        QuestData questData = statsData.getQuestData();
        if (questData.isQuestCompleted(sagaId, quest.getId())) return false;

        List<QuestObjective> objectives = quest.getObjectives();
        for (int i = 0; i < objectives.size(); i++) {
            QuestObjective objective = objectives.get(i);
            if (objective.getType() == QuestObjective.ObjectiveType.KILL) {
                for (int j = 0; j < i; j++) {
                    if (questData.getQuestObjectiveProgress(sagaId, quest.getId(), j) < objectives.get(j).getRequired()) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

		FirstPersonManager.isRenderingInGui = true;
        renderPlayerModel(graphics, this.width / 2 + 5, this.height / 2 + 70, 75, mouseX, mouseY);
		FirstPersonManager.isRenderingInGui = false;

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

        renderSagaTitle(graphics, leftPanelX, leftPanelY);
        renderQuestsList(graphics, leftPanelX, leftPanelY, mouseX, mouseY);
    }

    private void renderSagaTitle(GuiGraphics graphics, int panelX, int panelY) {
        if (availableSagas.isEmpty()) return;

        int titleY = panelY + 10;
        Saga currentSaga = availableSagas.get(currentSagaIndex);
        String sagaName = Component.translatable("dmz.saga." + currentSaga.getId()).withStyle(ChatFormatting.BOLD).getString();

        boolean sagaUnlocked = statsData != null && statsData.getQuestData().isSagaUnlocked(currentSaga.getId());
        int sagaColor = sagaUnlocked ? 0xFFFFFFFF : 0xFF888888;

        drawCenteredStringWithBorder(graphics, Component.literal(sagaName),
                panelX + 70, titleY + 6, sagaColor);
    }

    private void renderQuestsList(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY) {
        List<Quest> quests = getVisibleQuests();

        int startY = panelY + 30;
        int visibleStart = scrollOffset;
        int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_QUESTS, quests.size());

        graphics.enableScissor(panelX + 5, startY, panelX + 144, startY + (MAX_VISIBLE_QUESTS * QUEST_ITEM_HEIGHT));

        for (int i = visibleStart; i < visibleEnd; i++) {
            Quest quest = quests.get(i);
            int itemY = startY + ((i - visibleStart) * QUEST_ITEM_HEIGHT);

            if (quest == null) {
                drawStringWithBorder(graphics, Component.literal("???"),
                        panelX + 15, itemY + 5, 0xFF888888);
                continue;
            }

            boolean isSelected = selectedQuest != null && selectedQuest.getId() == quest.getId();
            boolean isHovered = mouseX >= panelX + 10 && mouseX <= panelX + 110 &&
                    mouseY >= itemY && mouseY <= itemY + QUEST_ITEM_HEIGHT - 5;

            int color = isSelected ? 0xFFFFAA00 : (isHovered ? 0xFFAAAAAA : 0xFFFFFFFF);

            String displayName = Component.translatable(quest.getTitle()).getString();

            drawStringWithBorder(graphics, Component.literal(displayName),
                    panelX + 15, itemY + 5, color);

            if (statsData != null) {
                Saga currentSaga = availableSagas.get(currentSagaIndex);
                if (statsData.getQuestData().isQuestCompleted(currentSaga.getId(), quest.getId())) {
                    String checkMark = "✓";
                    int checkX = panelX + 130 - this.font.width(checkMark);
                    drawStringWithBorder(graphics, Component.literal(checkMark),
                            checkX, itemY + 5, 0xFF00FF00);
                }
            }
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            int scrollBarX = panelX + 140;
            int scrollBarHeight = MAX_VISIBLE_QUESTS * QUEST_ITEM_HEIGHT;
            int totalItems = quests.size();

            graphics.fill(scrollBarX, startY, scrollBarX + 3, startY + scrollBarHeight, 0xFF333333);

            float scrollPercent = (float) scrollOffset / maxScroll;
            float visiblePercent = (float) MAX_VISIBLE_QUESTS / totalItems;
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
		graphics.blit(MENU_SMALL, this.width - 158, centerY + 76, 0, 95, 145, 58, 256, 256);
		graphics.blit(MENU_BIG, this.width - 158, centerY - 105, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, this.width - 141, centerY - 95, 142, 22, 107, 21, 256, 256);

        drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.character_stats.info").withStyle(ChatFormatting.BOLD),
                rightPanelX + 70, rightPanelY + 16, 0xFFFFD700);

        if (selectedQuest != null && statsData != null) {
            renderQuestDetails(graphics, rightPanelX, rightPanelY);
        }
    }

    private void renderQuestDetails(GuiGraphics graphics, int panelX, int panelY) {
        if (selectedQuest == null) return;

        Saga currentSaga = availableSagas.get(currentSagaIndex);
        QuestData questData = statsData.getQuestData();
        boolean isCompleted = questData.isQuestCompleted(currentSaga.getId(), selectedQuest.getId());

        String displayName = Component.translatable(selectedQuest.getTitle()).getString();
        String description = Component.translatable(selectedQuest.getDescription()).getString();

        int startY = panelY + 35;

        drawCenteredStringWithBorder(graphics, Component.literal(displayName).withStyle(ChatFormatting.BOLD),
                panelX + 70, startY, 0xFFFFFFFF);

        String statusKey = isCompleted ? "gui.dragonminez.quests.status.complete" : "gui.dragonminez.quests.status.incomplete";
        String statusText = Component.translatable(statusKey).getString();
        int statusColor = isCompleted ? 0xFF00FF00 : 0xFFFFFF00;

        drawCenteredStringWithBorder(graphics, Component.literal(statusText),
                panelX + 70, startY + 15, statusColor);

        int descY = startY + 32;
        List<String> wrappedDesc = wrapText(description, 130);
        for (String line : wrappedDesc) {
            drawStringWithBorder(graphics, Component.literal(line), panelX + 15, descY, 0xFFCCCCCC);
            descY += 10;
        }

        drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.quests.objectives").withStyle(ChatFormatting.BOLD),
                panelX + 15, descY + 5, 0xFFFFD700);

		int objStartY = descY + 20;
		int objVisibleHeight = 65;

		this.objAreaX = panelX + 5;
		this.objAreaY = objStartY;
		this.objAreaWidth = 140;
		this.objAreaHeight = objVisibleHeight;

		List<QuestObjective> objectives = selectedQuest.getObjectives();

		int totalContentHeight = 0;
		for (QuestObjective objective : objectives) {
			int progress = questData.getQuestObjectiveProgress(currentSaga.getId(), selectedQuest.getId(), objectives.indexOf(objective));
			String objText = getObjectiveText(objective, progress);
			List<String> wrappedObj = wrapText(objText, 105);

			totalContentHeight += (wrappedObj.size() * 10) + 2;
		}

		this.maxObjectivesScroll = Math.max(0, totalContentHeight - objVisibleHeight);

		graphics.enableScissor(panelX + 5, objStartY, panelX + 144, objStartY + objVisibleHeight);

		int currentRenderY = objStartY - objectivesScrollOffset;

		for (int i = 0; i < objectives.size(); i++) {
			QuestObjective objective = objectives.get(i);
			int progress = questData.getQuestObjectiveProgress(currentSaga.getId(), selectedQuest.getId(), i);
			boolean objCompleted = progress >= objective.getRequired();

			String objText = getObjectiveText(objective, progress);
			String marker = objCompleted ? "✓" : "✕";
			int markerColor = objCompleted ? 0xFF00FF00 : 0xFFFF0000;

			drawStringWithBorder(graphics, Component.literal(marker),
					panelX + 15, currentRenderY, markerColor);

			List<String> wrappedObj = wrapText(objText, 105);
			for (String line : wrappedObj) {
				drawStringWithBorder(graphics, Component.literal(line),
						panelX + 30, currentRenderY, 0xFFCCCCCC);
				currentRenderY += 10;
			}
			currentRenderY += 2;
		}

		graphics.disableScissor();

		if (maxObjectivesScroll > 0) {
			int scrollBarX = panelX + 138;
			int scrollBarHeight = objVisibleHeight;
			graphics.fill(scrollBarX, objStartY, scrollBarX + 2, objStartY + scrollBarHeight, 0xFF333333);
			float scrollPercent = (float) objectivesScrollOffset / maxObjectivesScroll;
			int indicatorHeight = Math.max(10, (int)((float)objVisibleHeight / totalContentHeight * objVisibleHeight));
			int indicatorY = objStartY + (int)((scrollBarHeight - indicatorHeight) * scrollPercent);
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
		int leftPanelX = 12;
		int centerY = this.height / 2;
		int leftPanelY = centerY - 105;
		if (mouseX >= leftPanelX && mouseX <= leftPanelX + 148 &&
				mouseY >= leftPanelY + 40 && mouseY <= leftPanelY + 219) {
			scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)delta));
			return true;
		}

		if (selectedQuest != null && maxObjectivesScroll > 0 &&
				mouseX >= objAreaX && mouseX <= objAreaX + objAreaWidth &&
				mouseY >= objAreaY && mouseY <= objAreaY + objAreaHeight) {
			int scrollAmount = (int)(delta * 10);
			objectivesScrollOffset = Math.max(0, Math.min(maxObjectivesScroll, objectivesScrollOffset - scrollAmount));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int leftPanelX = 12;
        int centerY = this.height / 2;
        int leftPanelY = centerY - 105;
        List<Quest> quests = getVisibleQuests();
        int startY = leftPanelY + 30;
        int visibleStart = scrollOffset;
        int visibleEnd = Math.min(visibleStart + MAX_VISIBLE_QUESTS, quests.size());
        for (int i = visibleStart; i < visibleEnd; i++) {
            int itemY = startY + ((i - visibleStart) * QUEST_ITEM_HEIGHT);

            if (mouseX >= leftPanelX + 10 && mouseX <= leftPanelX + 110 &&
                    mouseY >= itemY && mouseY <= itemY + QUEST_ITEM_HEIGHT) {

                Quest quest = quests.get(i);
                if (quest != null) {
                    selectedQuest = quest;
                    refreshButtons();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderPlayerModel(GuiGraphics graphics, int x, int y, int scale, float mouseX, float mouseY) {
        LivingEntity player = Minecraft.getInstance().player;
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
}
