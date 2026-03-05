package com.dragonminez.client.gui.quest;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.ClippableTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.BaseMenuScreen;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.AcceptSideQuestC2S;
import com.dragonminez.common.network.C2S.ClaimRewardC2S;
import com.dragonminez.common.network.C2S.ClaimSideQuestRewardC2S;
import com.dragonminez.common.network.C2S.StartQuestC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.*;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Full-screen tree-like quest visualization.
 * <p>
 * The entire screen is the pannable/zoomable quest grid. A detail panel sits
 * on the LEFT side (like SkillsMenuScreen), with sliding saga-switch buttons
 * on its right edge. The saga title is centered at the top and the action
 * button is centered at the bottom.
 *
 * @since 2.1
 */
@OnlyIn(Dist.CLIENT)
public class QuestTreeScreen extends BaseMenuScreen {

	// ========================================================================================
	// Textures
	// ========================================================================================

	private static final ResourceLocation MENU_BIG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menubig.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation EXCLAMATION_MARK = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/quest/exclamation_mark_quest.png");

	// ========================================================================================
	// Constants
	// ========================================================================================

	/** Size of each quest node square in pixels. */
	private static final int NODE_SIZE = 18;
	/** Half the node size, for centering calculations. */
	private static final int HALF_NODE = NODE_SIZE / 2;
	/** Thickness of the connection lines between nodes. */
	private static final int LINE_THICKNESS = 2;
	/** Padding around the tree canvas edges. */
	private static final int CANVAS_PADDING = 40;
	/** Display width of the exclamation mark icon on claimable nodes. */
	private static final int EXCLAMATION_DRAW_WIDTH = 6;
	/** Display height of the exclamation mark icon on claimable nodes. */
	private static final int EXCLAMATION_DRAW_HEIGHT = 15;
	/** Actual pixel dimensions of the exclamation_mark_quest.png file. */
	private static final int EXCLAMATION_TEX_WIDTH = 97;
	private static final int EXCLAMATION_TEX_HEIGHT = 250;

	/** X-position of the left detail panel. */
	private static final int LEFT_PANEL_X = 12;

	// ========================================================================================
	// Node Status Colors
	// ========================================================================================

	private static final int COLOR_COMPLETED = 0xFF00CC00;
	private static final int COLOR_COMPLETED_BORDER = 0xFF009900;
	private static final int COLOR_ACTIVE = 0xFF3399FF;
	private static final int COLOR_ACTIVE_BORDER = 0xFF2266CC;
	private static final int COLOR_AVAILABLE = 0xFFFFCC00;
	private static final int COLOR_AVAILABLE_BORDER = 0xFFCC9900;
	private static final int COLOR_LOCKED = 0xFF555555;
	private static final int COLOR_LOCKED_BORDER = 0xFF333333;
	private static final int COLOR_SELECTED_GLOW = 0xAAFFFFFF;

	// Line colors
	private static final int LINE_COLOR_COMPLETED = 0xFF00AA00;
	private static final int LINE_COLOR_DEFAULT = 0xFF444444;
	private static final int LINE_COLOR_FADED = 0x55444444;

	// Background
	private static final int GRID_COLOR = 0xFF222244;

	// ========================================================================================
	// State
	// ========================================================================================

	private StatsData statsData;
	private int tickCount = 0;
	private int pendingRefreshTicks = 0;

	// Action button (start quest / claim rewards)
	private TexturedTextButton actionButton;
	private static final Map<String, Long> QUEST_COOLDOWNS = new HashMap<>();
	private static final long START_QUEST_COOLDOWN = 30000;
	private long lastClickTime = 0;

	// Saga navigation
	private int currentSagaIndex = 0;
	private final List<Saga> availableSagas = new ArrayList<>();

	// Tree layout
	private QuestTreeLayoutHelper.TreeLayout currentLayout;
	private Quest selectedQuest = null;

	// Panning
	private float panX = 0;
	private float panY = 0;
	private boolean isDragging = false;
	private double dragStartX, dragStartY;
	private float dragStartPanX, dragStartPanY;

	// Smooth pan animation (slide to selected node)
	private float targetPanX = 0;
	private float targetPanY = 0;
	private boolean isAnimatingPan = false;
	private static final float PAN_ANIM_SPEED = 0.15f;

	// Zoom
	private float zoom = 1.0f;
	private static final float MIN_ZOOM = 0.25f;
	private static final float MAX_ZOOM = 2.0f;
	private static final float ZOOM_STEP = 0.1f;

	// Detail panel scrolling
	private int descriptionScrollOffset = 0;
	private int maxDescriptionScroll = 0;
	private int objectivesScrollOffset = 0;
	private int maxObjectivesScroll = 0;
	private static final int MAX_DESC_LINES = 4;

	// Sliding saga buttons (like SkillsMenuScreen — on RIGHT edge of left panel)
	private static final int SAGA_BUTTON_ANIM_TIME = 5;
	private final List<ClippableTextureButton> sagaButtons = new ArrayList<>();
	private int sagaAnimTick = 0;
	private boolean isSagaHotZoneHovered = false;

	/**
	 * UV coordinates into menubig.png for each saga button icon.
	 * Format: {normalU, normalV, hoverU, hoverV} — update these once the real icons
	 * are placed in the menubig.png spritesheet.
	 */
	private static final int[][] SAGA_BUTTON_UVS = {
			{142, 44, 142, 44},   // Saiyan Saga   (placeholder)
			{170, 44, 170, 44},   // Frieza Saga    (placeholder)
			{198, 44, 198, 44},   // Android Saga   (placeholder)
	};

	// ========================================================================================
	// Constructor
	// ========================================================================================

	public QuestTreeScreen() {
		super(Component.translatable("gui.dragonminez.quest_tree.title"));
	}

	// ========================================================================================
	// Initialization
	// ========================================================================================

	@Override
	protected void init() {
		super.init();
		updateStatsData();
		loadAvailableSagas();

		if (currentSagaIndex < availableSagas.size()) {
			rebuildLayout();
		}

		refreshButtons();
	}

	private void loadAvailableSagas() {
		availableSagas.clear();
		if (statsData == null) return;

		Map<String, Saga> allSagas = QuestRegistry.getClientSagas();
		if (allSagas == null || allSagas.isEmpty()) return;

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

	private void rebuildLayout() {
		if (availableSagas.isEmpty() || currentSagaIndex >= availableSagas.size()) {
			currentLayout = null;
			return;
		}

		Saga saga = availableSagas.get(currentSagaIndex);
		currentLayout = QuestTreeLayoutHelper.computeLayout(saga);

		centerViewOnProgress();
	}

	/**
	 * Centers the view on the last completed saga quest (or first quest if none completed).
	 */
	private void centerViewOnProgress() {
		if (currentLayout == null) return;

		int canvasWidth = getUiWidth();
		int canvasHeight = getUiHeight() - 60;

		zoom = 1.0f;

		// Find the last completed (or first available) saga quest node to center on
		QuestTreeLayoutHelper.NodePosition targetNode = null;
		if (statsData != null && !availableSagas.isEmpty()) {
			Saga saga = availableSagas.get(currentSagaIndex);
			PlayerQuestData pqd = statsData.getPlayerQuestData();
			List<Quest> sagaQuests = saga.getQuests();

			// Walk through saga quests, find the last completed or the first incomplete
			for (Quest q : sagaQuests) {
				boolean completed = pqd.isQuestCompleted(saga.getId(), q.getId());

				// Find the matching node in the layout
				for (QuestTreeLayoutHelper.NodePosition node : currentLayout.getNodes()) {
					if (node.getQuest().getId() == q.getId() && !node.isSidequest()) {
						if (completed) {
							targetNode = node; // keep updating to last completed
						} else {
							// This is the first incomplete — center on it
							targetNode = node;
							break;
						}
					}
				}
				if (!completed) break;
			}

			// Fallback to first node if nothing found
			if (targetNode == null && !currentLayout.getNodes().isEmpty()) {
				targetNode = currentLayout.getNodes().get(0);
			}
		}

		if (targetNode != null) {
			// Center this node in the middle of the canvas
			panX = (canvasWidth / 2.0f) - targetNode.getPixelX() - HALF_NODE;
			panY = (canvasHeight / 2.0f) - targetNode.getPixelY() - HALF_NODE;
		} else {
			panX = CANVAS_PADDING;
			panY = (canvasHeight - currentLayout.getTotalHeight()) / 2.0f;
		}
	}

	/**
	 * Starts a smooth pan animation that slides the view to center on the given node.
	 */
	private void slideToNode(QuestTreeLayoutHelper.NodePosition node) {
		int canvasWidth = getUiWidth();
		int canvasHeight = getUiHeight() - 60;

		targetPanX = (canvasWidth / 2.0f) - (node.getPixelX() * zoom) - HALF_NODE * zoom;
		targetPanY = (canvasHeight / 2.0f) - (node.getPixelY() * zoom) - HALF_NODE * zoom;
		isAnimatingPan = true;
	}

	// ========================================================================================
	// Buttons
	// ========================================================================================

	private void refreshButtons() {
		this.clearWidgets();
		actionButton = null;
		initNavigationButtons();
		initSagaNavigationButtons();
		initActionButton();
	}

	private void initSagaNavigationButtons() {
		sagaButtons.clear();
		if (availableSagas.isEmpty()) return;

		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		// Buttons sit on the RIGHT edge of the left panel (like SkillsMenuScreen)
		int hiddenX = LEFT_PANEL_X + 122;
		int buttonY = leftPanelY + 6;

		// Scissor: only show the area to the RIGHT of the panel edge
		int scissorX = LEFT_PANEL_X + 141;
		int scissorXScreen = toScreenCoord(scissorX);
		int scissorYScreen = toScreenCoord(0);
		int scissorRight = toScreenCoord(getUiWidth());
		int scissorBottom = toScreenCoord(getUiHeight());

		for (int i = 0; i < availableSagas.size(); i++) {
			final int sagaIdx = i;

			int[] uv = (i < SAGA_BUTTON_UVS.length) ? SAGA_BUTTON_UVS[i] : SAGA_BUTTON_UVS[0];

			ClippableTextureButton btn = new ClippableTextureButton.Builder()
					.position(hiddenX, buttonY + (i * 32))
					.size(26, 32)
					.texture(MENU_BIG)
					.textureCoords(uv[0], uv[1], uv[2], uv[3])
					.clipping(true, scissorXScreen, scissorYScreen, scissorRight, scissorBottom)
					.onPress(b -> {
						currentSagaIndex = sagaIdx;
						selectedQuest = null;
						rebuildLayout();
						refreshButtons();
					})
					.build();

			sagaButtons.add(btn);
			this.addRenderableWidget(btn);
		}
	}

	private void initActionButton() {
		if (selectedQuest == null || statsData == null || availableSagas.isEmpty()) return;

		PlayerQuestData questData = statsData.getPlayerQuestData();
		Saga currentSaga = availableSagas.get(currentSagaIndex);
		String selectedKey = questProgressKey(currentSaga, selectedQuest);
		boolean isCompleted = isQuestCompleted(questData, currentSaga, selectedQuest);
		boolean canStart = canStartQuest(selectedQuest, currentSaga.getId());

		Component buttonText;
		boolean buttonActive = true;
		boolean isClaimAction = false;

		String cooldownKey = currentSaga.getId() + ":" + selectedKey;

		if (isCompleted) {
			boolean hasUnclaimedRewards = false;
			for (int i = 0; i < selectedQuest.getRewards().size(); i++) {
				if (!isRewardClaimed(questData, currentSaga, selectedQuest, i)) {
					hasUnclaimedRewards = true;
					break;
				}
			}

			if (hasUnclaimedRewards) {
				buttonText = Component.translatable("gui.dragonminez.quests.claim_rewards");
				isClaimAction = true;
			} else {
				return;
			}
		} else if (canStart) {
			buttonText = Component.translatable("gui.dragonminez.quests.start");
			long now = System.currentTimeMillis();
			long lastRun = QUEST_COOLDOWNS.getOrDefault(cooldownKey, 0L);
			buttonActive = now - lastRun >= START_QUEST_COOLDOWN;
		} else {
			return;
		}

		boolean finalIsClaimAction = isClaimAction;
		int buttonX = (getUiWidth() - 74) / 2;
		int buttonY = getUiHeight() - 50;

		actionButton = new TexturedTextButton.Builder()
				.position(buttonX, buttonY)
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
						if (selectedQuest.isSideQuest()) {
							NetworkHandler.sendToServer(new ClaimSideQuestRewardC2S(selectedKey));
						} else {
							NetworkHandler.sendToServer(new ClaimRewardC2S(currentSaga.getId(), selectedQuest.getId()));
						}
						btn.visible = false;
						pendingRefreshTicks = 5;
					} else {
						QUEST_COOLDOWNS.put(cooldownKey, now);
						boolean isHard = ConfigManager.getUserConfig().getHud().getStoryHardDifficulty();
						if (selectedQuest.isSideQuest()) {
							NetworkHandler.sendToServer(new AcceptSideQuestC2S(selectedKey, isHard));
						} else {
							NetworkHandler.sendToServer(new StartQuestC2S(currentSaga.getId(), selectedQuest.getId(), isHard));
						}
						this.onClose();
					}
				})
				.build();

		actionButton.active = buttonActive;
		this.addRenderableWidget(actionButton);
	}

	private boolean canStartQuest(Quest quest, String sagaId) {
		if (statsData == null) return false;

		PlayerQuestData questData = statsData.getPlayerQuestData();
		Saga currentSaga = availableSagas.get(currentSagaIndex);
		String questKey = questProgressKey(currentSaga, quest);
		if (questData.isQuestCompleted(questKey)) return false;

		List<QuestObjective> objectives = quest.getObjectives();
		for (int i = 0; i < objectives.size(); i++) {
			QuestObjective objective = objectives.get(i);
			if (objective.getType() == QuestObjective.ObjectiveType.KILL) {
				for (int j = 0; j < i; j++) {
					if (questData.getObjectiveProgress(questKey, j) < objectives.get(j).getRequired()) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	// ========================================================================================
	// Tick
	// ========================================================================================

	@Override
	public void tick() {
		super.tick();
		tickCount++;

		if (tickCount >= 10) {
			tickCount = 0;
			updateStatsData();
		}

		if (pendingRefreshTicks > 0) {
			pendingRefreshTicks--;
			if (pendingRefreshTicks == 0) {
				updateStatsData();
				refreshButtons();
			}
		}

		// Saga sliding button animation
		if (isSagaHotZoneHovered) {
			if (sagaAnimTick < SAGA_BUTTON_ANIM_TIME) sagaAnimTick++;
		} else {
			if (sagaAnimTick > 0) sagaAnimTick--;
		}
	}

	private void updateStatsData() {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data ->
				this.statsData = data
			);
		}
	}

	// ========================================================================================
	// Rendering
	// ========================================================================================

	@Override
	public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);

		updateSagaButtonAnimations(uiMouseX, uiMouseY, partialTick);

		// Smooth pan animation — lerp towards target
		if (isAnimatingPan) {
			panX += (targetPanX - panX) * PAN_ANIM_SPEED;
			panY += (targetPanY - panY) * PAN_ANIM_SPEED;

			// Stop animating when close enough
			if (Math.abs(targetPanX - panX) < 0.5f && Math.abs(targetPanY - panY) < 0.5f) {
				panX = targetPanX;
				panY = targetPanY;
				isAnimatingPan = false;
			}
		}

		// Full-screen tree canvas (rendered first, behind the panel)
		renderTreeCanvas(graphics, uiMouseX, uiMouseY);

		// Left detail panel (on top of the grid)
		renderDetailPanel(graphics, uiMouseX, uiMouseY);

		super.render(graphics, uiMouseX, uiMouseY, partialTick);

		// Render glow effect on action button (after widgets are drawn)
		if (actionButton != null && actionButton.visible && actionButton.active) {
			renderActionButtonGlow(graphics);
		}

		endUiScale(graphics);
	}

	// ========================================================================================
	// Saga Button Animation (SkillsMenuScreen pattern — right edge of left panel)
	// ========================================================================================

	private void updateSagaButtonAnimations(int mouseX, int mouseY, float partialTick) {
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		// Hot zone: area to the RIGHT of the left panel where hovering triggers the slide
		int hotZoneX = LEFT_PANEL_X + 122;
		int hotZoneY = leftPanelY + 6;
		int hotZoneWidth = 48;
		int hotZoneHeight = Math.max(32 * availableSagas.size(), 32);

		isSagaHotZoneHovered = mouseX >= hotZoneX && mouseX < hotZoneX + hotZoneWidth &&
				mouseY >= hotZoneY && mouseY < hotZoneY + hotZoneHeight;

		float animProgress = (sagaAnimTick + (isSagaHotZoneHovered ? partialTick : -partialTick)) / SAGA_BUTTON_ANIM_TIME;
		animProgress = Mth.clamp(animProgress, 0.0f, 1.0f);

		// hiddenX: behind the panel; visibleX: slid out to the right
		int hiddenX = LEFT_PANEL_X + 122;
		int visibleX = LEFT_PANEL_X + 141;

		int newX = hiddenX + (int) ((visibleX - hiddenX) * animProgress);
		for (ClippableTextureButton btn : sagaButtons) {
			btn.setX(newX);
		}
	}

	// ========================================================================================
	// Tree Canvas Rendering (full screen)
	// ========================================================================================

	private void renderTreeCanvas(GuiGraphics graphics, int mouseX, int mouseY) {
		if (currentLayout == null || availableSagas.isEmpty()) {
			drawCenteredStringWithBorder(graphics,
					Component.translatable("gui.dragonminez.quest_tree.no_sagas"),
					getUiWidth() / 2, getUiHeight() / 2, 0xFFAAAAAA);
			return;
		}

		int canvasRight = getUiWidth();
		int canvasBottom = getUiHeight() - 25;

		// Full-screen canvas clip
		graphics.enableScissor(
				toScreenCoord(0),
				toScreenCoord(0),
				toScreenCoord(canvasRight),
				toScreenCoord(canvasBottom)
		);

		// Draw subtle background grid
		renderBackgroundGrid(graphics, canvasRight, canvasBottom);

		// Saga title centered at the very top
		Saga currentSaga = availableSagas.get(currentSagaIndex);
		String sagaName = Component.translatable(currentSaga.getName()).getString();
		drawCenteredStringWithBorder(graphics,
				Component.literal(sagaName).withStyle(ChatFormatting.BOLD),
				getUiWidth() / 2, 8, 0xFFFFD700);

		// Apply zoom transform
		graphics.pose().pushPose();
		graphics.pose().scale(zoom, zoom, 1.0f);

		int zoomedMouseX = (int) (mouseX / zoom);
		int zoomedMouseY = (int) (mouseY / zoom);

		// Render connections first (behind nodes)
		for (QuestTreeLayoutHelper.NodeConnection conn : currentLayout.getConnections()) {
			renderConnection(graphics, conn);
		}

		// Render nodes
		Quest hoveredQuest = null;
		for (QuestTreeLayoutHelper.NodePosition node : currentLayout.getNodes()) {
			boolean isHovered = isNodeHovered(node, zoomedMouseX, zoomedMouseY);
			if (isHovered) hoveredQuest = node.getQuest();
			renderNode(graphics, node, isHovered);
		}

		graphics.pose().popPose();

		// Render tooltip outside zoom
		if (hoveredQuest != null) {
			renderNodeTooltip(graphics, hoveredQuest, mouseX, mouseY);
		}

		// Zoom indicator in bottom-left of canvas
		String zoomText = (int) (zoom * 100) + "%";
		drawStringWithBorder(graphics, Component.literal(zoomText), 6, canvasBottom - 12, 0xFF888888);

		graphics.disableScissor();
	}

	private void renderBackgroundGrid(GuiGraphics graphics, int canvasRight, int canvasBottom) {
		int spacing = 20;
		int offsetX = ((int) panX) % spacing;
		int offsetY = ((int) panY) % spacing;

		for (int x = offsetX; x < canvasRight; x += spacing) {
			graphics.fill(x, 0, x + 1, canvasBottom, GRID_COLOR);
		}
		for (int y = offsetY; y < canvasBottom; y += spacing) {
			graphics.fill(0, y, canvasRight, y + 1, GRID_COLOR);
		}
	}

	private void renderConnection(GuiGraphics graphics, QuestTreeLayoutHelper.NodeConnection conn) {
		float zPanX = panX / zoom;
		float zPanY = panY / zoom;

		NodeVisibility fromVis = getNodeVisibility(conn.getFrom().getQuest());
		NodeVisibility toVis = getNodeVisibility(conn.getTo().getQuest());

		if (fromVis == NodeVisibility.HIDDEN && toVis == NodeVisibility.HIDDEN) return;

		int x1 = (int) (conn.getFrom().getPixelX() + zPanX) + HALF_NODE;
		int y1 = (int) (conn.getFrom().getPixelY() + zPanY) + HALF_NODE;
		int x2 = (int) (conn.getTo().getPixelX() + zPanX) + HALF_NODE;
		int y2 = (int) (conn.getTo().getPixelY() + zPanY) + HALF_NODE;

		int color = LINE_COLOR_DEFAULT;
		if (statsData != null && !availableSagas.isEmpty()) {
			Saga saga = availableSagas.get(currentSagaIndex);
			PlayerQuestData pqd = statsData.getPlayerQuestData();
			boolean fromCompleted = isQuestCompleted(pqd, saga, conn.getFrom().getQuest());
			boolean toCompleted = isQuestCompleted(pqd, saga, conn.getTo().getQuest());
			if (fromCompleted && toCompleted) {
				color = LINE_COLOR_COMPLETED;
			}
		}

		if (toVis == NodeVisibility.BLURRED) {
			color = LINE_COLOR_FADED;
		}

		boolean sameRow = conn.getFrom().getGridRow() == conn.getTo().getGridRow()
				&& conn.getFrom().getPixelY() == conn.getTo().getPixelY();

		if (sameRow) {
			int lineY = y1 - LINE_THICKNESS / 2;

			if (toVis == NodeVisibility.HIDDEN) {
				int midX = (x1 + x2) / 2;
				graphics.fill(Math.min(x1, midX), lineY, Math.max(x1, midX), lineY + LINE_THICKNESS, LINE_COLOR_FADED);
			} else {
				graphics.fill(Math.min(x1, x2), lineY, Math.max(x1, x2), lineY + LINE_THICKNESS, color);
			}
		} else {
			if (toVis == NodeVisibility.HIDDEN) {
				int midY = (y1 + y2) / 2;
				int vLineX = x1 - LINE_THICKNESS / 2;
				graphics.fill(vLineX, Math.min(y1, midY), vLineX + LINE_THICKNESS, Math.max(y1, midY), LINE_COLOR_FADED);
			} else {
				int vLineX = x1 - LINE_THICKNESS / 2;
				graphics.fill(vLineX, Math.min(y1, y2), vLineX + LINE_THICKNESS, Math.max(y1, y2), color);

				if (x1 != x2) {
					int hLineY = y2 - LINE_THICKNESS / 2;
					graphics.fill(Math.min(x1, x2), hLineY, Math.max(x1, x2), hLineY + LINE_THICKNESS, color);
				}
			}
		}
	}


	private void renderNode(GuiGraphics graphics, QuestTreeLayoutHelper.NodePosition node, boolean isHovered) {
		NodeVisibility vis = getNodeVisibility(node.getQuest());
		if (vis == NodeVisibility.HIDDEN) return;

		float zPanX = panX / zoom;
		float zPanY = panY / zoom;

		int x = (int) (node.getPixelX() + zPanX);
		int y = (int) (node.getPixelY() + zPanY);

		boolean isBlurred = (vis == NodeVisibility.BLURRED);

		QuestNodeStatus status = getNodeStatus(node.getQuest());

		int bgColor;
		int borderColor;
		if (isBlurred) {
			bgColor = 0x55555555;
			borderColor = 0x55333333;
		} else {
			switch (status) {
				case COMPLETED, CLAIMABLE -> {
					bgColor = COLOR_COMPLETED;
					borderColor = COLOR_COMPLETED_BORDER;
				}
				case ACTIVE -> {
					bgColor = COLOR_ACTIVE;
					borderColor = COLOR_ACTIVE_BORDER;
				}
				case AVAILABLE -> {
					bgColor = COLOR_AVAILABLE;
					borderColor = COLOR_AVAILABLE_BORDER;
				}
				default -> {
					bgColor = COLOR_LOCKED;
					borderColor = COLOR_LOCKED_BORDER;
				}
			}
		}

		// Selected glow
		boolean isSelected = selectedQuest != null && sameQuestIdentity(selectedQuest, node.getQuest());
		if (isSelected && !isBlurred) {
			graphics.fill(x - 2, y - 2, x + NODE_SIZE + 2, y + NODE_SIZE + 2, COLOR_SELECTED_GLOW);
		}

		// Hover effect
		if (isHovered && !isBlurred) {
			graphics.fill(x - 1, y - 1, x + NODE_SIZE + 1, y + NODE_SIZE + 1, 0x66FFFFFF);
		}

		// Border
		graphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE, borderColor);
		// Inner fill
		graphics.fill(x + 1, y + 1, x + NODE_SIZE - 1, y + NODE_SIZE - 1, bgColor);

		// Status icon inside the node
		String icon;
		int iconColor;
		if (isBlurred) {
			icon = "?";
			iconColor = 0x55999999;
		} else {
			iconColor = 0xFFFFFFFF;
			switch (status) {
				case COMPLETED:
				case CLAIMABLE:
					icon = "✓";
					break;
				case ACTIVE:
					icon = "!";
					iconColor = 0xFFFFFF00;
					break;
				case AVAILABLE:
					icon = "?";
					break;
				default:
					icon = "✕";
					iconColor = 0xFF999999;
					break;
			}
		}

		int iconX = x + (NODE_SIZE - this.font.width(icon)) / 2;
		int iconY = y + (NODE_SIZE - 8) / 2;
		graphics.drawString(this.font, icon, iconX, iconY, iconColor, false);

		// Label below node — use quest title for saga quests, "S" badge label for sidequests
		if (!isBlurred) {
			if (node.isSidequest()) {
				// Sidequest indicator (small "S" badge)
				int badgeX = x - 4;
				int badgeY = y - 4;
				graphics.fill(badgeX, badgeY, badgeX + 8, badgeY + 8, 0xFF6644AA);
				graphics.drawString(this.font, "S", badgeX + 1, badgeY, 0xFFFFFFFF, false);
			} else {
				// Saga quest number below node
				String questNum = String.valueOf(node.getQuest().getId());
				int numX = x + (NODE_SIZE - this.font.width(questNum)) / 2;
				drawStringWithBorder(graphics, Component.literal(questNum), numX, y + NODE_SIZE + 2, 0xFFCCCCCC);
			}
		} else {
			// Blurred: show "???"
			String questNum = "???";
			int numX = x + (NODE_SIZE - this.font.width(questNum)) / 2;
			drawStringWithBorder(graphics, Component.literal(questNum), numX, y + NODE_SIZE + 2, 0x55888888);
		}

		// Exclamation mark icon on top-right for claimable quests
		if (status == QuestNodeStatus.CLAIMABLE && !isBlurred) {
			float pulse = (float) (Math.sin(System.currentTimeMillis() / 350.0) * 0.5 + 0.5);
			float alpha = 0.3f + pulse * 0.7f;
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			graphics.setColor(1.0f, 1.0f, 1.0f, alpha);
			int iconDrawX = x + NODE_SIZE - 2;
			int iconDrawY = y - EXCLAMATION_DRAW_HEIGHT + 4;
			graphics.blit(EXCLAMATION_MARK, iconDrawX, iconDrawY,
					EXCLAMATION_DRAW_WIDTH, EXCLAMATION_DRAW_HEIGHT,
					0, 0,
					EXCLAMATION_TEX_WIDTH, EXCLAMATION_TEX_HEIGHT,
					EXCLAMATION_TEX_WIDTH, EXCLAMATION_TEX_HEIGHT);
			graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
		}
	}

	private void renderNodeTooltip(GuiGraphics graphics, Quest quest, int mouseX, int mouseY) {
		NodeVisibility vis = getNodeVisibility(quest);
		if (vis == NodeVisibility.HIDDEN) return;

		String title;
		String statusText;

		if (vis == NodeVisibility.BLURRED) {
			title = "§k" + "Unknown Quest" + "§r";
			statusText = Component.translatable("gui.dragonminez.quest_tree.status.locked").getString();
		} else {
			title = Component.translatable(quest.getTitle()).getString();
			QuestNodeStatus status = getNodeStatus(quest);
			statusText = getStatusText(status);
		}

		int tooltipWidth = Math.max(this.font.width(title), this.font.width(statusText)) + 12;
		int tooltipHeight = 26;
		int tooltipX = mouseX + 10;
		int tooltipY = mouseY - tooltipHeight - 5;

		graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xEE111122);
		graphics.renderOutline(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 0xFF555577);

		if (vis == NodeVisibility.BLURRED) {
			Component obfuscated = Component.literal("Unknown Quest").withStyle(ChatFormatting.OBFUSCATED);
			graphics.drawString(this.font, obfuscated, tooltipX + 5, tooltipY + 3, 0xFF555555, false);
		} else {
			graphics.drawString(this.font, title, tooltipX + 5, tooltipY + 3, 0xFFFFFFFF, false);
		}
		int statusColor = vis == NodeVisibility.BLURRED ? 0xFF888888 : getStatusColor(getNodeStatus(quest));
		graphics.drawString(this.font, statusText, tooltipX + 5, tooltipY + 14, statusColor, false);
	}

	// ========================================================================================
	// Detail Panel (LEFT Side — like SkillsMenuScreen)
	// ========================================================================================

	private void renderDetailPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(MENU_BIG, LEFT_PANEL_X, leftPanelY, 0, 0, 141, 213, 256, 256);
		graphics.blit(MENU_BIG, LEFT_PANEL_X + 17, leftPanelY + 10, 142, 22, 107, 21, 256, 256);

		// Saga name at top of panel
		if (!availableSagas.isEmpty() && currentSagaIndex < availableSagas.size()) {
			Saga saga = availableSagas.get(currentSagaIndex);
			String sagaName = Component.translatable(saga.getName()).getString();
			drawCenteredStringWithBorder(graphics,
					Component.literal(sagaName).withStyle(ChatFormatting.BOLD),
					LEFT_PANEL_X + 70, leftPanelY + 16, 0xFFFFD700);
		}

		if (selectedQuest != null && statsData != null) {
			renderQuestDetails(graphics, LEFT_PANEL_X, leftPanelY);
		} else {
			drawCenteredStringWithBorder(graphics,
					Component.translatable("gui.dragonminez.quest_tree.select_quest"),
					LEFT_PANEL_X + 70, leftPanelY + 90, 0xFF888888);
		}
	}

	private void renderQuestDetails(GuiGraphics graphics, int panelX, int panelY) {
		if (selectedQuest == null || statsData == null || availableSagas.isEmpty()) return;

		Saga currentSaga = availableSagas.get(currentSagaIndex);
		PlayerQuestData questData = statsData.getPlayerQuestData();
		String selectedKey = questProgressKey(currentSaga, selectedQuest);

		String displayName = Component.translatable(selectedQuest.getTitle()).getString();
		String description = Component.translatable(selectedQuest.getDescription()).getString();

		int startY = panelY + 35;

		// Quest title
		drawCenteredStringWithBorder(graphics,
				Component.literal(displayName).withStyle(ChatFormatting.BOLD),
				panelX + 70, startY, 0xFFFFFFFF);

		// Status
		QuestNodeStatus status = getNodeStatus(selectedQuest);
		String statusText = getStatusText(status);
		int statusColor = getStatusColor(status);
		drawCenteredStringWithBorder(graphics, Component.literal(statusText),
				panelX + 70, startY + 13, statusColor);

		// Description (scrollable)
		int descY = startY + 28;
		List<String> wrappedDesc = wrapText(description, 115);
		int descVisibleHeight = MAX_DESC_LINES * 10;
		int totalDescHeight = wrappedDesc.size() * 10;
		this.maxDescriptionScroll = Math.max(0, totalDescHeight - descVisibleHeight);

		graphics.enableScissor(
				toScreenCoord(panelX + 5),
				toScreenCoord(descY),
				toScreenCoord(panelX + 140),
				toScreenCoord(descY + descVisibleHeight)
		);

		int currentDescY = descY - descriptionScrollOffset;
		for (String line : wrappedDesc) {
			drawStringWithBorder(graphics, Component.literal(line), panelX + 12, currentDescY, 0xFFCCCCCC);
			currentDescY += 10;
		}

		graphics.disableScissor();

		// Description scroll bar
		if (maxDescriptionScroll > 0) {
			int scrollBarX = panelX + 135;
			graphics.fill(scrollBarX, descY, scrollBarX + 2, descY + descVisibleHeight, 0xFF333333);
			float scrollPercent = (float) descriptionScrollOffset / maxDescriptionScroll;
			int indicatorHeight = Math.max(6, (int) ((float) descVisibleHeight / totalDescHeight * descVisibleHeight));
			int indicatorY = descY + (int) ((descVisibleHeight - indicatorHeight) * scrollPercent);
			graphics.fill(scrollBarX, indicatorY, scrollBarX + 2, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}

		// Objectives header
		int objTitleY = descY + descVisibleHeight + 5;
		drawStringWithBorder(graphics,
				Component.translatable("gui.dragonminez.quests.objectives").withStyle(ChatFormatting.BOLD),
				panelX + 12, objTitleY, 0xFFFFD700);

		// Objectives list (scrollable)
		int objStartY = objTitleY + 12;
		int objVisibleHeight = 55;

		List<QuestObjective> objectives = selectedQuest.getObjectives();
		int totalObjHeight = 0;
		for (QuestObjective obj : objectives) {
			int progress = questData.getObjectiveProgress(selectedKey, objectives.indexOf(obj));
			String objText = getObjectiveText(obj, progress);
			List<String> wrappedObj = wrapText(objText, 95);
			totalObjHeight += (wrappedObj.size() * 10) + 2;
		}
		this.maxObjectivesScroll = Math.max(0, totalObjHeight - objVisibleHeight);

		graphics.enableScissor(
				toScreenCoord(panelX + 5),
				toScreenCoord(objStartY),
				toScreenCoord(panelX + 140),
				toScreenCoord(objStartY + objVisibleHeight)
		);

		int currentObjY = objStartY - objectivesScrollOffset;
		for (int i = 0; i < objectives.size(); i++) {
			QuestObjective obj = objectives.get(i);
			int progress = questData.getObjectiveProgress(selectedKey, i);
			boolean objCompleted = progress >= obj.getRequired();

			String marker = objCompleted ? "✓" : "✕";
			int markerColor = objCompleted ? 0xFF00FF00 : 0xFFFF0000;

			drawStringWithBorder(graphics, Component.literal(marker), panelX + 12, currentObjY, markerColor);

			String objText = getObjectiveText(obj, progress);
			List<String> wrappedObj = wrapText(objText, 95);
			for (String line : wrappedObj) {
				drawStringWithBorder(graphics, Component.literal(line), panelX + 25, currentObjY, 0xFFCCCCCC);
				currentObjY += 10;
			}
			currentObjY += 2;
		}

		graphics.disableScissor();

		// Objectives scroll bar
		if (maxObjectivesScroll > 0) {
			int scrollBarX = panelX + 135;
			graphics.fill(scrollBarX, objStartY, scrollBarX + 2, objStartY + objVisibleHeight, 0xFF333333);
			float scrollPercent = (float) objectivesScrollOffset / maxObjectivesScroll;
			int indicatorHeight = Math.max(6, (int) ((float) objVisibleHeight / totalObjHeight * objVisibleHeight));
			int indicatorY = objStartY + (int) ((objVisibleHeight - indicatorHeight) * scrollPercent);
			graphics.fill(scrollBarX, indicatorY, scrollBarX + 2, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}

		// Rewards summary
		int rewardsY = objStartY + objVisibleHeight + 5;
		drawStringWithBorder(graphics,
				Component.translatable("gui.dragonminez.quests.rewards").withStyle(ChatFormatting.BOLD),
				panelX + 12, rewardsY, 0xFFFFD700);

		int rewardRenderY = rewardsY + 12;
		List<QuestReward> rewards = selectedQuest.getRewards();
		for (int i = 0; i < rewards.size(); i++) {
			QuestReward reward = rewards.get(i);
			boolean claimed = questData.isRewardClaimed(selectedKey, i);

			String marker = claimed ? "✓" : "✕";
			int markerColor = claimed ? 0xFF00FF00 : 0xFFFF0000;
			drawStringWithBorder(graphics, Component.literal(marker), panelX + 12, rewardRenderY, markerColor);

			String rewardText = reward.getDescription().getString();
			List<String> wrappedReward = wrapText(rewardText, 95);
			for (String line : wrappedReward) {
				drawStringWithBorder(graphics, Component.literal(line), panelX + 25, rewardRenderY, 0xFFCCCCCC);
				rewardRenderY += 10;
			}
			rewardRenderY += 2;

			if (rewardRenderY > panelY + 205) break;
		}
	}

	/**
	 * Renders a soft pulsing glow effect around the action button.
	 */
	private void renderActionButtonGlow(GuiGraphics graphics) {
		float pulse = (float) (Math.sin(System.currentTimeMillis() / 500.0) * 0.5 + 0.5);

		int btnX = actionButton.getX();
		int btnY = actionButton.getY();
		int btnW = actionButton.getWidth();
		int btnH = actionButton.getHeight();

		int glowAlpha = (int) (pulse * 45);
		int glowColor = (glowAlpha << 24) | 0x88CCDD;
		graphics.fill(btnX - 2, btnY - 2, btnX + btnW + 2, btnY + btnH + 2, glowColor);
	}

	// ========================================================================================
	// Input Handling
	// ========================================================================================

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		// Let widgets (buttons) handle clicks first
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		if (button == 0) {
			// Convert to zoomed space for node hit-testing
			int zoomedMouseX = (int) (uiMouseX / zoom);
			int zoomedMouseY = (int) (uiMouseY / zoom);

			// Check if a node was clicked
			if (currentLayout != null) {
				for (QuestTreeLayoutHelper.NodePosition node : currentLayout.getNodes()) {
					if (isNodeHovered(node, zoomedMouseX, zoomedMouseY)) {
						if (getNodeVisibility(node.getQuest()) == NodeVisibility.BLURRED) {
							return true;
						}
						selectedQuest = node.getQuest();
						descriptionScrollOffset = 0;
						objectivesScrollOffset = 0;
						Minecraft.getInstance().getSoundManager().play(
								SimpleSoundInstance.forUI(MainSounds.PIP_MENU.get(), 1.0F));
						slideToNode(node);
						refreshButtons();
						return true;
					}
				}
			}

			// Start panning
			isDragging = true;
			isAnimatingPan = false;
			dragStartX = uiMouseX;
			dragStartY = uiMouseY;
			dragStartPanX = panX;
			dragStartPanY = panY;
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (isDragging && button == 0) {
			double uiMouseX = toUiX(mouseX);
			double uiMouseY = toUiY(mouseY);

			panX = dragStartPanX + (float) (uiMouseX - dragStartX);
			panY = dragStartPanY + (float) (uiMouseY - dragStartY);
			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (isDragging) {
			isDragging = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		// Scroll on the left detail panel area (for description/objectives)
		int panelRight = LEFT_PANEL_X + 141;
		if (uiMouseX <= panelRight) {
			int scrollAmount = (int) Math.signum(delta);
			if (maxDescriptionScroll > 0) {
				descriptionScrollOffset = Math.max(0, Math.min(maxDescriptionScroll, descriptionScrollOffset - (scrollAmount * 10)));
				return true;
			}
			if (maxObjectivesScroll > 0) {
				objectivesScrollOffset = Math.max(0, Math.min(maxObjectivesScroll, objectivesScrollOffset - (scrollAmount * 10)));
				return true;
			}
			return super.mouseScrolled(mouseX, mouseY, delta);
		}

		// Zoom on the canvas area — pivot around mouse position
		float oldZoom = zoom;
		float zoomDelta = (float) Math.signum(delta) * ZOOM_STEP;
		zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom + zoomDelta));

		if (zoom != oldZoom) {
			float scale = zoom / oldZoom;
			panX = (float) (uiMouseX - scale * (uiMouseX - panX));
			panY = (float) (uiMouseY - scale * (uiMouseY - panY));
		}

		return true;
	}

	// ========================================================================================
	// Helpers
	// ========================================================================================

	private enum QuestNodeStatus {
		LOCKED,
		AVAILABLE,
		ACTIVE,
		COMPLETED,
		CLAIMABLE
	}

	private enum NodeVisibility {
		VISIBLE,
		BLURRED,
		HIDDEN
	}

	private NodeVisibility getNodeVisibility(Quest quest) {
		if (statsData == null || availableSagas.isEmpty()) return NodeVisibility.HIDDEN;

		Saga saga = availableSagas.get(currentSagaIndex);
		PlayerQuestData pqd = statsData.getPlayerQuestData();

		if (isQuestCompleted(pqd, saga, quest)) {
			return NodeVisibility.VISIBLE;
		}

		List<Quest> sagaQuests = saga.getQuests();
		int currentAvailableIndex = -1;
		for (int i = 0; i < sagaQuests.size(); i++) {
			if (!pqd.isQuestCompleted(saga.getId(), sagaQuests.get(i).getId())) {
				currentAvailableIndex = i;
				break;
			}
		}

		// Is this a main saga quest?
		for (int i = 0; i < sagaQuests.size(); i++) {
			if (sagaQuests.get(i).getId() == quest.getId()) {
				if (i == currentAvailableIndex) return NodeVisibility.VISIBLE;
				if (i == currentAvailableIndex + 1) return NodeVisibility.BLURRED;
				return NodeVisibility.HIDDEN;
			}
		}

		// Side-quest visibility
		if (quest.isSideQuest() && quest.hasPrerequisites()) {
			var conditions = quest.getPrerequisites().getConditions();
			for (var cond : conditions) {
				if (cond.getType() == QuestPrerequisites.ConditionType.SAGA_QUEST
						&& saga.getId().equals(cond.getSagaId())) {
					Integer reqId = cond.getQuestId();
					if (reqId != null && pqd.isQuestCompleted(saga.getId(), reqId)) {
						return NodeVisibility.VISIBLE;
					}
					if (reqId != null && currentAvailableIndex >= 0
							&& sagaQuests.get(currentAvailableIndex).getId() == reqId) {
						return NodeVisibility.BLURRED;
					}
				}
			}
		}

		return NodeVisibility.HIDDEN;
	}

	private QuestNodeStatus getNodeStatus(Quest quest) {
		if (statsData == null || availableSagas.isEmpty()) return QuestNodeStatus.LOCKED;

		Saga saga = availableSagas.get(currentSagaIndex);
		PlayerQuestData pqd = statsData.getPlayerQuestData();

		boolean isCompleted = isQuestCompleted(pqd, saga, quest);
		if (isCompleted) {
			List<QuestReward> rewards = quest.getRewards();
			for (int i = 0; i < rewards.size(); i++) {
				if (!isRewardClaimed(pqd, saga, quest, i)) {
					return QuestNodeStatus.CLAIMABLE;
				}
			}
			return QuestNodeStatus.COMPLETED;
		}

		// Side-quests: check if their saga prereqs are met
		if (quest.isSideQuest() && quest.hasPrerequisites()) {
			var conditions = quest.getPrerequisites().getConditions();
			boolean allMet = true;
			for (var cond : conditions) {
				if (cond.getType() == QuestPrerequisites.ConditionType.SAGA_QUEST) {
					Integer reqId = cond.getQuestId();
					String reqSaga = cond.getSagaId();
					if (reqId != null && reqSaga != null
							&& !pqd.isQuestCompleted(reqSaga, reqId)) {
						allMet = false;
						break;
					}
				}
			}
			return allMet ? QuestNodeStatus.AVAILABLE : QuestNodeStatus.LOCKED;
		}

		// Saga quests: check if this is the current (first incomplete) quest
		for (Quest q : saga.getQuests()) {
			if (!pqd.isQuestCompleted(saga.getId(), q.getId())) {
				if (q.getId() == quest.getId()) {
					return QuestNodeStatus.AVAILABLE;
				} else {
					return QuestNodeStatus.LOCKED;
				}
			}
		}

		return QuestNodeStatus.LOCKED;
	}

	private String getStatusText(QuestNodeStatus status) {
		return switch (status) {
			case COMPLETED -> Component.translatable("gui.dragonminez.quests.status.complete").getString();
			case CLAIMABLE -> Component.translatable("gui.dragonminez.quests.claim_rewards").getString();
			case ACTIVE -> Component.translatable("gui.dragonminez.quest_tree.status.active").getString();
			case AVAILABLE -> Component.translatable("gui.dragonminez.quest_tree.status.available").getString();
			case LOCKED -> Component.translatable("gui.dragonminez.quest_tree.status.locked").getString();
		};
	}

	private int getStatusColor(QuestNodeStatus status) {
		return switch (status) {
			case COMPLETED -> 0xFF00FF00;
			case CLAIMABLE -> 0xFFFFAA00;
			case ACTIVE -> 0xFF3399FF;
			case AVAILABLE -> 0xFFFFCC00;
			case LOCKED -> 0xFF888888;
		};
	}

	private boolean isNodeHovered(QuestTreeLayoutHelper.NodePosition node, int mouseX, int mouseY) {
		if (getNodeVisibility(node.getQuest()) == NodeVisibility.HIDDEN) return false;
		float zPanX = panX / zoom;
		float zPanY = panY / zoom;
		int x = (int) (node.getPixelX() + zPanX);
		int y = (int) (node.getPixelY() + zPanY);
		return mouseX >= x && mouseX <= x + NODE_SIZE && mouseY >= y && mouseY <= y + NODE_SIZE;
	}

	private String questProgressKey(Saga saga, Quest quest) {
		if (quest.isSideQuest() && quest.getStringId() != null) {
			return quest.getStringId();
		}
		return PlayerQuestData.sagaQuestKey(saga.getId(), quest.getId());
	}

	private boolean isQuestCompleted(PlayerQuestData pqd, Saga saga, Quest quest) {
		return pqd.isQuestCompleted(questProgressKey(saga, quest));
	}

	private boolean isRewardClaimed(PlayerQuestData pqd, Saga saga, Quest quest, int rewardIndex) {
		return pqd.isRewardClaimed(questProgressKey(saga, quest), rewardIndex);
	}

	private boolean sameQuestIdentity(Quest a, Quest b) {
		if (a == null || b == null) return false;
		if (a.isSideQuest() || b.isSideQuest()) {
			return a.isSideQuest() == b.isSideQuest()
					&& a.getStringId() != null
					&& a.getStringId().equals(b.getStringId());
		}
		return a.getId() == b.getId();
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

	private void drawStringWithBorder(GuiGraphics graphics, Component text, int x, int y, int textColor) {
		int borderColor = 0xFF000000;
		String stripped = ChatFormatting.stripFormatting(text.getString());
		Component borderComponent = Component.literal(stripped != null ? stripped : text.getString());

		if (text.getStyle().isBold()) {
			borderComponent = borderComponent.copy().withStyle(style -> style.withBold(true));
		}

		graphics.drawString(this.font, borderComponent, x + 1, y, borderColor, false);
		graphics.drawString(this.font, borderComponent, x - 1, y, borderColor, false);
		graphics.drawString(this.font, borderComponent, x, y + 1, borderColor, false);
		graphics.drawString(this.font, borderComponent, x, y - 1, borderColor, false);
		graphics.drawString(this.font, text, x, y, textColor, false);
	}

	private void drawCenteredStringWithBorder(GuiGraphics graphics, Component text, int centerX, int y, int textColor) {
		int textWidth = this.font.width(text);
		int x = centerX - (textWidth / 2);
		drawStringWithBorder(graphics, text, x, y, textColor);
	}
}

