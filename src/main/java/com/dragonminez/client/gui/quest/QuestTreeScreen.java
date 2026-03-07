package com.dragonminez.client.gui.quest;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.BaseMenuScreen;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.AcceptSideQuestC2S;
import com.dragonminez.common.network.C2S.ClaimRewardC2S;
import com.dragonminez.common.network.C2S.ClaimSideQuestRewardC2S;
import com.dragonminez.common.network.C2S.StartQuestC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestPrerequisites;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.quest.rewards.ItemReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

	// Shared panel texture used on both sides (placeholder until final left/right assets are delivered).
	private static final ResourceLocation QUEST_MENU = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/questmenu.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation EXCLAMATION_MARK = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/quest/exclamation_mark_quest.png");
	private static final ResourceLocation REWARD_GENERIC_ICON = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/quest/reward_generic.png");

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

	private static final int QUEST_MENU_TEX_WIDTH = 512;
	private static final int QUEST_MENU_TEX_HEIGHT = 512;
	// questmenu.png stores the panel inside the atlas; visible frame is not full 512x512.
	private static final int QUEST_MENU_PANEL_U = 1;
	private static final int QUEST_MENU_PANEL_V = 1;
	private static final int QUEST_MENU_PANEL_WIDTH = 282;
	private static final int QUEST_MENU_PANEL_HEIGHT = 426;
	private static final int PANEL_BORDER_BLEED_PX = 3;

	private static final long PANEL_INTRO_DURATION_MS = 700L;
	private static final int PANEL_INTRO_EXTRA_TRAVEL_PX = 22;
	private static final float PANEL_INTRO_BACK_OVERSHOOT = 1.35f;

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

	// Navigator
	private static final int NAV_ITEM_HEIGHT = 13;
	private static final float SIDE_PANEL_HEIGHT_RATIO = 0.90f;
	private static final float SIDE_PANEL_WIDTH_RATIO = 0.90f;
	private static final int SIDE_PANEL_BG = 0xCC0F1020;
	private static final int SIDE_PANEL_BORDER = 0xAA5A5F7A;

	// Typewriter reveal
	private static final long TYPEWRITER_COOLDOWN_MS = 5L * 60L * 1000L;
	private static final int TYPEWRITER_CHARS_PER_SECOND = 55;

	// Section keys for typewriter reveal
	private static final String SECTION_DESC = "desc";
	private static final String SECTION_REWARDS = "rewards";

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
	private boolean isDraggingTree = false;
	private double dragStartX, dragStartY;
	private float dragStartPanX, dragStartPanY;

	// Smooth pan animation (slide to selected node)
	private float targetPanX = 0;
	private float targetPanY = 0;
	private boolean isAnimatingPan = false;
	private long lastPanAnimNanos = 0L;
	private static final float PAN_SMOOTHING = 14.0f;
	private static final float PAN_STOP_EPSILON = 0.35f;

	// Zoom
	private float zoom = 1.0f;
	private static final float MIN_ZOOM = 0.25f;
	private static final float MAX_ZOOM = 2.0f;
	private static final float ZOOM_STEP = 0.1f;

	// Navigator (left panel) scrolling
	private final List<NavigatorEntry> navigatorEntries = new ArrayList<>();
	private int navScrollOffset = 0;
	private int navMaxScroll = 0;

	// Reward tooltips
	private final List<RewardHitbox> rewardHitboxes = new ArrayList<>();
	private final Map<String, Long> sectionLastReveal = new HashMap<>();
	private final Map<String, Long> sectionAnimationStart = new HashMap<>();

	private long panelIntroStartMs = 0L;
	private boolean panelIntroActive = false;

	// ========================================================================================
	// Constructor
	// ========================================================================================

	public QuestTreeScreen() {
		super(Component.translatable("gui.dragonminez.quest_tree.title"));
	}

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

	private enum NavEntryType {
		SAGA,
		MAIN_QUEST,
		SIDE_QUEST
	}

	private record PanelRect(int x, int y, int width, int height) {
		int right() {
			return x + width;
		}

		int bottom() {
			return y + height;
		}

		boolean contains(double px, double py) {
			return px >= x && px <= right() && py >= y && py <= bottom();
		}
	}

	private record NavigatorEntry(NavEntryType type, int depth, Saga saga, Quest quest) {
	}

	private record RewardHitbox(int x, int y, int size, ItemStack stack, Component tooltip) {
		boolean contains(int mx, int my) {
			return mx >= x && mx <= x + size && my >= y && my <= y + size;
		}
	}

	private record DetailPanelLayout(int titleH, int rewardsH, int descH, int objectivesH) {
	}

	// ========================================================================================
	// Initialization
	// ========================================================================================

	@Override
	protected void init() {
		super.init();
		startPanelIntroAnimation();
		updateStatsData();
		loadAvailableSagas();
		rebuildLayout();
		rebuildNavigatorEntries();
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
			selectedQuest = null;
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

		PanelRect tree = getTreePanelRect();
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
						targetNode = node; // keep updating to last completed
						if (!completed) {
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
			panX = (tree.x + tree.width / 2.0f) - targetNode.getPixelX() - HALF_NODE;
			panY = (tree.y + tree.height / 2.0f) - targetNode.getPixelY() - HALF_NODE;
		} else {
			panX = tree.x + CANVAS_PADDING;
			panY = tree.y + (tree.height - currentLayout.getTotalHeight()) / 2.0f;
		}
	}

	/**
	 * Starts a smooth pan animation that slides the view to center on the given node.
	 */
	private void slideToNode(QuestTreeLayoutHelper.NodePosition node) {
		PanelRect tree = getTreePanelRect();
		targetPanX = (tree.x + tree.width / 2.0f) - (node.getPixelX() * zoom) - HALF_NODE * zoom;
		targetPanY = (tree.y + tree.height / 2.0f) - (node.getPixelY() * zoom) - HALF_NODE * zoom;
		isAnimatingPan = true;
		lastPanAnimNanos = System.nanoTime();
	}

	private static float expSmoothingAlpha(float smoothing, float dtSeconds) {
		return (float) (1.0 - Math.exp(-smoothing * dtSeconds));
	}

	private void rebuildNavigatorEntries() {
		navigatorEntries.clear();
		if (availableSagas.isEmpty()) {
			navMaxScroll = 0;
			navScrollOffset = 0;
			return;
		}

		Saga currentSaga = availableSagas.get(currentSagaIndex);
		for (int i = 0; i < availableSagas.size(); i++) {
			Saga saga = availableSagas.get(i);
			navigatorEntries.add(new NavigatorEntry(NavEntryType.SAGA, 0, saga, null));
			if (i != currentSagaIndex) {
				continue;
			}

			Map<String, List<Quest>> sideBranches = buildSideBranchesForSaga(currentSaga);
			for (Quest mainQuest : currentSaga.getQuests()) {
				if (getNodeVisibility(mainQuest) != NodeVisibility.VISIBLE) {
					continue;
				}
				navigatorEntries.add(new NavigatorEntry(NavEntryType.MAIN_QUEST, 1, currentSaga, mainQuest));
				addSideBranchEntries(mainQuest, sideBranches, currentSaga, 2);
			}
		}

		PanelRect left = getLeftPanelRect();
		int usableHeight = Math.max(32, left.height - 40);
		int visibleCount = Math.max(1, usableHeight / NAV_ITEM_HEIGHT);
		navMaxScroll = Math.max(0, navigatorEntries.size() - visibleCount);
		navScrollOffset = Math.max(0, Math.min(navScrollOffset, navMaxScroll));
	}

	private Map<String, List<Quest>> buildSideBranchesForSaga(Saga saga) {
		Map<String, List<Quest>> byParent = new LinkedHashMap<>();
		if (currentLayout == null || saga == null) return byParent;

		Map<String, Quest> nodeByKey = new HashMap<>();
		for (QuestTreeLayoutHelper.NodePosition node : currentLayout.getNodes()) {
			nodeByKey.put(questProgressKey(saga, node.getQuest()), node.getQuest());
		}

		for (QuestTreeLayoutHelper.NodeConnection connection : currentLayout.getConnections()) {
			Quest to = connection.getTo().getQuest();
			if (!to.isSideQuest()) continue;
			Quest from = connection.getFrom().getQuest();
			String parentKey = questProgressKey(saga, from);
			byParent.computeIfAbsent(parentKey, k -> new ArrayList<>()).add(to);
		}

		for (List<Quest> sideList : byParent.values()) {
			sideList.sort((a, b) -> {
				String aKey = a.getStringId() != null ? a.getStringId() : "";
				String bKey = b.getStringId() != null ? b.getStringId() : "";
				return aKey.compareTo(bKey);
			});
		}

		return byParent;
	}

	private void addSideBranchEntries(Quest parentQuest, Map<String, List<Quest>> sideBranches, Saga saga, int depth) {
		List<Quest> children = sideBranches.get(questProgressKey(saga, parentQuest));
		if (children == null || children.isEmpty()) return;
		for (Quest child : children) {
			if (getNodeVisibility(child) != NodeVisibility.VISIBLE) {
				continue;
			}
			navigatorEntries.add(new NavigatorEntry(NavEntryType.SIDE_QUEST, depth, saga, child));
			addSideBranchEntries(child, sideBranches, saga, depth + 1);
		}
	}

	private boolean isSagaUnlockedByPreviousCompletion(Saga saga) {
		if (saga == null || saga.getRequirements() == null) return true;
		String previousSagaId = saga.getRequirements().getPreviousSagaId();
		if (previousSagaId == null || previousSagaId.isEmpty()) return true;
		if (statsData == null) return false;

		Map<String, Saga> allSagas = QuestRegistry.getClientSagas();
		Saga previousSaga = allSagas.get(previousSagaId);
		if (previousSaga == null) return true;

		PlayerQuestData pqd = statsData.getPlayerQuestData();
		for (Quest q : previousSaga.getQuests()) {
			if (!pqd.isQuestCompleted(previousSaga.getId(), q.getId())) {
				return false;
			}
		}
		return true;
	}

	// ========================================================================================
	// Buttons
	// ========================================================================================

	private void refreshButtons() {
		this.clearWidgets();
		actionButton = null;
		initNavigationButtons();
		initActionButton();
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
		PanelRect right = getRightPanelRect();
		int buttonX = right.x + (right.width - 74) / 2;
		int buttonY = right.bottom() - 28;

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
		if (statsData == null || availableSagas.isEmpty()) return false;
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

		if (isAnimatingPan) {
			long now = System.nanoTime();
			if (lastPanAnimNanos == 0L) {
				lastPanAnimNanos = now;
			}

			float dt = (now - lastPanAnimNanos) / 1_000_000_000.0f;
			lastPanAnimNanos = now;
			dt = Math.max(0.0f, Math.min(0.05f, dt));

			float alpha = expSmoothingAlpha(PAN_SMOOTHING, dt);
			panX += (targetPanX - panX) * alpha;
			panY += (targetPanY - panY) * alpha;

			if (Math.abs(targetPanX - panX) < PAN_STOP_EPSILON && Math.abs(targetPanY - panY) < PAN_STOP_EPSILON) {
				panX = targetPanX;
				panY = targetPanY;
				isAnimatingPan = false;
				lastPanAnimNanos = 0L;
			}
		}
	}

	private void updateStatsData() {
		var player = Minecraft.getInstance().player;
		if (player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			this.statsData = data;
			if (availableSagas.isEmpty()) return;
			if (currentSagaIndex >= availableSagas.size()) {
				currentSagaIndex = Math.max(0, availableSagas.size() - 1);
			}
		});
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
		syncActionButtonPosition();
		rewardHitboxes.clear();

		renderTreeCanvas(graphics, uiMouseX, uiMouseY);
		renderLeftNavigatorPanel(graphics, uiMouseX, uiMouseY);
		renderRightDetailPanel(graphics, uiMouseX, uiMouseY);

		super.render(graphics, uiMouseX, uiMouseY, partialTick);

		if (actionButton != null && actionButton.visible && actionButton.active) {
			renderActionButtonGlow(graphics);
		}

		renderRewardTooltips(graphics, uiMouseX, uiMouseY);
		endUiScale(graphics);
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

		PanelRect tree = getTreePanelRect();
		graphics.enableScissor(toScreenCoord(tree.x), toScreenCoord(tree.y), toScreenCoord(tree.right()), toScreenCoord(tree.bottom()));
		renderBackgroundGrid(graphics, tree);

		Saga currentSaga = availableSagas.get(currentSagaIndex);
		drawCenteredStringWithBorder(graphics,
				Component.translatable(currentSaga.getName()).copy().withStyle(ChatFormatting.BOLD),
				tree.x + tree.width / 2,
				tree.y + 8,
				0xFFFFD700);

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
		drawStringWithBorder(graphics, Component.literal(zoomText), 6, tree.bottom() - 12, 0xFF888888);

		graphics.disableScissor();
	}

	private void renderBackgroundGrid(GuiGraphics graphics, PanelRect tree) {
		int spacing = 20;
		int offsetX = ((int) panX) % spacing;
		int offsetY = ((int) panY) % spacing;

		for (int x = tree.x + offsetX; x < tree.right(); x += spacing) {
			graphics.fill(x, tree.y, x + 1, tree.bottom(), GRID_COLOR);
		}
		for (int y = tree.y + offsetY; y < tree.bottom(); y += spacing) {
			graphics.fill(tree.x, y, tree.right(), y + 1, GRID_COLOR);
		}
	}

	// ========================================================================================
	// Navigator (Left Panel)
	// ========================================================================================

	private void renderLeftNavigatorPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		PanelRect panel = getLeftPanelRect();
		renderSidePanelBackground(graphics, panel, true, false);

		drawCenteredStringWithBorder(graphics,
				Component.translatable("gui.dragonminez.quest_tree.title").copy().withStyle(ChatFormatting.BOLD),
				panel.x + panel.width / 2,
				panel.y + 10,
				0xFFFFD700);

		int listX = panel.x + 10;
		int listY = panel.y + 28;
		int listW = panel.width - 20;
		int listH = panel.height - 38;

		int visibleCount = Math.max(1, listH / NAV_ITEM_HEIGHT);
		int visibleStart = navScrollOffset;
		int visibleEnd = Math.min(navigatorEntries.size(), visibleStart + visibleCount);

		graphics.enableScissor(toScreenCoord(listX), toScreenCoord(listY), toScreenCoord(listX + listW), toScreenCoord(listY + listH));

		for (int i = visibleStart; i < visibleEnd; i++) {
			NavigatorEntry entry = navigatorEntries.get(i);
			int rowY = listY + ((i - visibleStart) * NAV_ITEM_HEIGHT);
			boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= rowY && mouseY <= rowY + NAV_ITEM_HEIGHT;
			renderNavigatorEntry(graphics, entry, listX, rowY, listW, hovered);
		}

		graphics.disableScissor();

		if (navMaxScroll > 0) {
			int scrollBarX = listX + listW - 3;
			graphics.fill(scrollBarX, listY, scrollBarX + 2, listY + listH, 0xFF333333);
			float scrollPercent = (float) navScrollOffset / navMaxScroll;
			int indicatorHeight = Math.max(10, (int) ((float) visibleCount / navigatorEntries.size() * listH));
			int indicatorY = listY + (int) ((listH - indicatorHeight) * scrollPercent);
			graphics.fill(scrollBarX, indicatorY, scrollBarX + 2, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}
	}

	private void renderNavigatorEntry(GuiGraphics graphics, NavigatorEntry entry, int x, int y, int rowWidth, boolean hovered) {
		int color = 0xFFE0E0E0;
		Component text;
		int textY = y + Math.max(0, (NAV_ITEM_HEIGHT - 8) / 2);

		if (entry.type() == NavEntryType.SAGA) {
			boolean selectedSaga = entry.saga() == availableSagas.get(currentSagaIndex);
			boolean unlocked = isSagaUnlockedByPreviousCompletion(entry.saga());
			color = selectedSaga ? 0xFFFFCC55 : (unlocked ? 0xFFFFFFFF : 0xFF888888);
			if (hovered && unlocked) color = 0xFFFFE08A;
			String prefix = selectedSaga ? "v " : (unlocked ? "> " : "[L] ");
			String raw = prefix + Component.translatable(entry.saga().getName()).getString();
			String clipped = fitSingleLineEllipsis(raw, Math.max(24, rowWidth - 8));
			text = Component.literal(clipped).withStyle(ChatFormatting.BOLD);
		} else {
			Quest q = entry.quest();
			String label = q.isSideQuest()
					? "- " + Component.translatable(q.getTitle()).getString()
					: q.getId() + ". " + Component.translatable(q.getTitle()).getString();

			int indent = entry.depth() * 10;
			String clipped = fitSingleLineEllipsis(label, Math.max(24, rowWidth - indent - 8));
			text = Component.literal(clipped);

			QuestNodeStatus status = getNodeStatus(q);
			color = getStatusColor(status);
			if (selectedQuest != null && sameQuestIdentity(selectedQuest, q)) color = 0xFFFFFFFF;
			if (hovered) color = 0xFFFFD070;
		}

		int indent = entry.depth() * 10;
		drawStringWithBorder(graphics, text, x + indent, textY, color);
	}

	// ========================================================================================
	// Detail Panel (RIGHT Side)
	// ========================================================================================

	private void renderRightDetailPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		PanelRect panel = getRightPanelRect();
		renderSidePanelBackground(graphics, panel, false, true);

		if (selectedQuest == null || statsData == null || availableSagas.isEmpty()) {
			drawCenteredStringWithBorder(graphics,
					Component.translatable("gui.dragonminez.quest_tree.select_quest"),
					panel.x + panel.width / 2,
					panel.y + panel.height / 2,
					0xFFAAAAAA);
			return;
		}

		Saga saga = availableSagas.get(currentSagaIndex);
		String questKey = questProgressKey(saga, selectedQuest);
		QuestNodeStatus status = getNodeStatus(selectedQuest);

		int innerX = panel.x + 10;
		int innerY = panel.y + 10;
		int innerW = panel.width - 20;
		int innerH = panel.height - 40;

		DetailPanelLayout layout = computeDetailPanelLayout(innerW, innerH, questKey, saga);

		int rewardsY = innerY + layout.titleH();
		int descY = rewardsY + layout.rewardsH();
		int objectivesY = descY + layout.descH();

		renderTopSection(graphics, innerX, innerY, innerW, layout.titleH(), status);
		renderRewardsSection(graphics, innerX, rewardsY, innerW, layout.rewardsH(), questKey, mouseX, mouseY);
		renderDescriptionSection(graphics, innerX, descY, innerW, layout.descH(), questKey);
		renderObjectivesSection(graphics, innerX, objectivesY, innerW, layout.objectivesH(), saga);
	}

	private DetailPanelLayout computeDetailPanelLayout(int width, int totalHeight, String questKey, Saga saga) {
		int titleH = estimateTitleSectionHeight(width);
		int rewardsMin = selectedQuest.getRewards().isEmpty() ? 24 : 36;
		int objectivesMin = selectedQuest.getObjectives().isEmpty() ? 24 : 36;
		int descMin = 66;

		int rewardsDesired = estimateRewardsSectionHeight(width, questKey);
		int objectivesDesired = estimateObjectivesSectionHeight(width, saga);

		int rewardsCap = Math.max(rewardsMin, (int) (totalHeight * 0.35f));
		int objectivesCap = Math.max(objectivesMin, (int) (totalHeight * 0.35f));

		int rewardsH = Math.min(rewardsCap, Math.max(rewardsMin, rewardsDesired));
		int objectivesH = Math.min(objectivesCap, Math.max(objectivesMin, objectivesDesired));
		int descH = totalHeight - titleH - rewardsH - objectivesH;

		if (descH < descMin) {
			int deficit = descMin - descH;
			int shrinkRewards = Math.max(0, rewardsH - rewardsMin);
			int shrinkObjectives = Math.max(0, objectivesH - objectivesMin);

			int takeFromRewards = Math.min(deficit, shrinkRewards);
			rewardsH -= takeFromRewards;
			deficit -= takeFromRewards;

			int takeFromObjectives = Math.min(deficit, shrinkObjectives);
			objectivesH -= takeFromObjectives;
		}

		descH = Math.max(40, totalHeight - titleH - rewardsH - objectivesH);
		return new DetailPanelLayout(titleH, rewardsH, descH, objectivesH);
	}

	private int estimateTitleSectionHeight(int width) {
		String title = Component.translatable(selectedQuest.getTitle()).getString();
		List<String> lines = limitLinesWithEllipsis(wrapText(title, width - 10), 2, width - 10);
		return Math.max(32, 18 + (lines.size() * 10) + 10);
	}

	private int estimateRewardsSectionHeight(int width, String questKey) {
		if (selectedQuest.getRewards().isEmpty()) return 28;
		String visibleRewardsText = resolveTypewriterText(questKey, SECTION_REWARDS, buildRewardsText(selectedQuest.getRewards()));
		List<String> lines = wrapText(visibleRewardsText, width - 36);
		int rows = Math.max(selectedQuest.getRewards().size(), lines.size());
		return 24 + (rows * 10) + 6;
	}

	private int estimateObjectivesSectionHeight(int width, Saga saga) {
		if (selectedQuest.getObjectives().isEmpty()) return 28;
		List<String> objectiveLines = buildObjectiveRenderLines(saga, width - 24);
		return 24 + (objectiveLines.size() * 10) + 6;
	}

	private void renderTopSection(GuiGraphics graphics, int x, int y, int width, int height, QuestNodeStatus status) {
		graphics.fill(x, y, x + width, y + height, 0x44111122);
		graphics.renderOutline(x, y, width, height, 0x88444466);

		List<String> wrappedTitle = limitLinesWithEllipsis(
				wrapText(Component.translatable(selectedQuest.getTitle()).getString(), width - 10),
				2,
				width - 10
		);

		int titleStartY = y + 6;
		for (String line : wrappedTitle) {
			drawCenteredStringWithBorder(graphics, Component.literal(line).withStyle(ChatFormatting.BOLD), x + width / 2, titleStartY, 0xFFFFFFFF);
			titleStartY += 10;
		}

		drawCenteredStringWithBorder(graphics,
				Component.literal(fitSingleLineEllipsis(getStatusText(status), width - 12)),
				x + width / 2,
				height >= 40 ? y + height - 14 : y + 20,
				getStatusColor(status));
	}

	private void renderRewardsSection(GuiGraphics graphics, int x, int y, int width, int height, String questKey, int mouseX, int mouseY) {
		graphics.fill(x, y, x + width, y + height, 0x44111122);
		graphics.renderOutline(x, y, width, height, 0x88444466);

		drawStringWithBorder(graphics,
				Component.translatable("gui.dragonminez.quests.rewards").copy().withStyle(ChatFormatting.BOLD),
				x + 6,
				y + 4,
				0xFFFFD700);

		List<QuestReward> rewards = selectedQuest.getRewards();
		if (rewards.isEmpty()) {
			drawStringWithBorder(graphics, Component.literal("-"), x + 8, y + 18, 0xFF999999);
			return;
		}

		String rawRewardsText = buildRewardsText(rewards);
		String visibleRewardsText = resolveTypewriterText(questKey, SECTION_REWARDS, rawRewardsText);

		int iconSize = 16;
		int startY = y + 18;
		int maxRows = Math.max(1, (height - 22) / 18);
		int rows = Math.min(rewards.size(), maxRows);

		List<String> wrapped = wrapText(visibleRewardsText, width - 36);
		int wrappedIdx = 0;

		for (int i = 0; i < rows; i++) {
			QuestReward reward = rewards.get(i);
			int iconX = x + 8;
			int iconY = startY + (i * 18);

			Component tooltip = reward.getDescription();
			ItemStack stack = null;

			if (reward instanceof ItemReward itemReward) {
				Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemReward.getItemId()));
				stack = new ItemStack(item, Math.max(1, itemReward.getCount()));
				graphics.renderItem(stack, iconX, iconY);
			} else {
				graphics.blit(REWARD_GENERIC_ICON, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
			}

			rewardHitboxes.add(new RewardHitbox(iconX, iconY, iconSize, stack, tooltip));

			String line = wrappedIdx < wrapped.size() ? wrapped.get(wrappedIdx++) : "";
			drawStringWithBorder(graphics, Component.literal(line), x + 28, iconY + 4, 0xFFCCCCCC);
		}
	}

	private void renderDescriptionSection(GuiGraphics graphics, int x, int y, int width, int height, String questKey) {
		graphics.fill(x, y, x + width, y + height, 0x44111122);
		graphics.renderOutline(x, y, width, height, 0x88444466);

		drawStringWithBorder(graphics,
				Component.translatable("gui.dragonminez.quest_tree.description").copy().withStyle(ChatFormatting.BOLD),
				x + 6,
				y + 4,
				0xFFFFD700);

		String fullDescription = Component.translatable(selectedQuest.getDescription()).getString();
		String visibleDescription = resolveTypewriterText(questKey, SECTION_DESC, fullDescription);
		List<String> lines = wrapText(visibleDescription, width - 14);

		int lineY = y + 18;
		int maxLines = Math.max(1, (height - 24) / 10);
		drawJustifiedTextBlock(graphics, lines, x + 6, lineY, width - 14, maxLines, 0xFFCCCCCC);
	}

	private void renderObjectivesSection(GuiGraphics graphics, int x, int y, int width, int height, Saga saga) {
		graphics.fill(x, y, x + width, y + height, 0x44111122);
		graphics.renderOutline(x, y, width, height, 0x88444466);

		drawStringWithBorder(graphics,
				Component.translatable("gui.dragonminez.quests.objectives").copy().withStyle(ChatFormatting.BOLD),
				x + 6,
				y + 4,
				0xFFFFD700);

		List<QuestObjective> objectives = selectedQuest.getObjectives();
		if (objectives.isEmpty()) {
			drawStringWithBorder(graphics, Component.literal("-"), x + 8, y + 18, 0xFF999999);
			return;
		}

		List<String> lines = buildObjectiveRenderLines(saga, width - 24);
		int maxLines = Math.max(1, (height - 24) / 10);
		int drawY = y + 18;
		for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
			drawStringWithBorder(graphics, Component.literal(lines.get(i)), x + 8, drawY, 0xFFCCCCCC);
			drawY += 10;
		}
	}

	private List<String> buildObjectiveRenderLines(Saga saga, int textWidth) {
		List<String> lines = new ArrayList<>();
		if (statsData == null || selectedQuest == null) return lines;

		PlayerQuestData pqd = statsData.getPlayerQuestData();
		String questKey = questProgressKey(saga, selectedQuest);
		List<QuestObjective> objectives = selectedQuest.getObjectives();

		for (int i = 0; i < objectives.size(); i++) {
			QuestObjective objective = objectives.get(i);
			int progress = pqd.getObjectiveProgress(questKey, i);
			boolean completed = progress >= objective.getRequired();
			String marker = completed ? "✓ " : "✕ ";
			String baseText = getObjectiveText(objective, progress);
			List<String> wrapped = wrapText(baseText, Math.max(12, textWidth - this.font.width(marker)));
			if (wrapped.isEmpty()) {
				lines.add(marker);
				continue;
			}
			lines.add(marker + wrapped.get(0));
			for (int j = 1; j < wrapped.size(); j++) {
				lines.add("  " + wrapped.get(j));
			}
		}

		return lines;
	}

	private String getObjectiveText(QuestObjective objective, int currentProgress) {
		String description = Component.translatable(objective.getDescription()).getString();
		int required = objective.getRequired();
		if (objective.getType() == QuestObjective.ObjectiveType.KILL || objective.getType() == QuestObjective.ObjectiveType.ITEM) {
			return description + " (" + currentProgress + "/" + required + ")";
		}
		return description;
	}

	private void renderRewardTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
		for (RewardHitbox hitbox : rewardHitboxes) {
			if (!hitbox.contains(mouseX, mouseY)) continue;
			if (hitbox.stack != null && !hitbox.stack.isEmpty()) {
				graphics.renderTooltip(this.font, hitbox.stack, mouseX, mouseY);
			} else {
				renderSimpleTooltip(graphics, hitbox.tooltip.getString(), mouseX, mouseY);
			}
			return;
		}
	}

	private void renderSimpleTooltip(GuiGraphics graphics, String text, int mouseX, int mouseY) {
		int tooltipWidth = this.font.width(text) + 10;
		int tooltipX = mouseX + 8;
		int tooltipY = mouseY - 14;
		graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 14, 0xEE111122);
		graphics.renderOutline(tooltipX, tooltipY, tooltipWidth, 14, 0xFF555577);
		graphics.drawString(this.font, text, tooltipX + 5, tooltipY + 3, 0xFFFFFFFF, false);
	}

	private void renderSidePanelBackground(GuiGraphics graphics, PanelRect panel, boolean flushLeft, boolean flushRight) {
		graphics.fill(panel.x, panel.y, panel.right(), panel.bottom(), SIDE_PANEL_BG);

		int drawX = panel.x - (flushLeft ? PANEL_BORDER_BLEED_PX : 0);
		int drawW = panel.width + (flushLeft ? PANEL_BORDER_BLEED_PX : 0) + (flushRight ? PANEL_BORDER_BLEED_PX : 0);

		float srcPixelsPerDstPixel = panel.width <= 0 ? 1.0f : (QUEST_MENU_PANEL_WIDTH / (float) panel.width);
		int srcBleed = Math.max(0, Math.round(PANEL_BORDER_BLEED_PX * srcPixelsPerDstPixel));
		int srcU = QUEST_MENU_PANEL_U - (flushLeft ? srcBleed : 0);
		int srcW = QUEST_MENU_PANEL_WIDTH + (flushLeft ? srcBleed : 0) + (flushRight ? srcBleed : 0);
		int srcV = QUEST_MENU_PANEL_V;
		int srcH = QUEST_MENU_PANEL_HEIGHT;

		if (srcU < 0) {
			srcW += srcU;
			srcU = 0;
		}
		srcW = Math.max(1, Math.min(srcW, QUEST_MENU_TEX_WIDTH - srcU));
		srcV = Math.max(0, Math.min(srcV, QUEST_MENU_TEX_HEIGHT - 1));
		srcH = Math.max(1, Math.min(srcH, QUEST_MENU_TEX_HEIGHT - srcV));

		graphics.blit(QUEST_MENU,
				drawX,
				panel.y,
				drawW,
				panel.height,
				srcU,
				srcV,
				srcW,
				srcH,
				QUEST_MENU_TEX_WIDTH,
				QUEST_MENU_TEX_HEIGHT);

		graphics.fill(panel.x, panel.y, panel.right(), panel.y + 1, SIDE_PANEL_BORDER);
		graphics.fill(panel.x, panel.bottom() - 1, panel.right(), panel.bottom(), SIDE_PANEL_BORDER);
		if (!flushLeft) {
			graphics.fill(panel.x, panel.y, panel.x + 1, panel.bottom(), SIDE_PANEL_BORDER);
		}
		if (!flushRight) {
			graphics.fill(panel.right() - 1, panel.y, panel.right(), panel.bottom(), SIDE_PANEL_BORDER);
		}
	}

	private String resolveTypewriterText(String questKey, String section, String fullText) {
		if (fullText == null || fullText.isEmpty()) return "";

		String key = questKey + "#" + section;
		long now = System.currentTimeMillis();
		long lastReveal = sectionLastReveal.getOrDefault(key, 0L);
		boolean shouldAnimate = now - lastReveal >= TYPEWRITER_COOLDOWN_MS;

		if (!shouldAnimate) {
			sectionAnimationStart.remove(key);
			return fullText;
		}

		long start = sectionAnimationStart.computeIfAbsent(key, ignored -> now);
		long elapsed = Math.max(0L, now - start);
		int visibleChars = (int) ((elapsed / 1000.0f) * TYPEWRITER_CHARS_PER_SECOND);
		if (visibleChars >= fullText.length()) {
			sectionLastReveal.put(key, now);
			sectionAnimationStart.remove(key);
			return fullText;
		}

		return fullText.substring(0, Math.max(0, visibleChars));
	}

	private String buildRewardsText(List<QuestReward> rewards) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < rewards.size(); i++) {
			if (i > 0) builder.append('\n');
			builder.append(rewards.get(i).getDescription().getString());
		}
		return builder.toString();
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

		if (toVis == NodeVisibility.BLURRED) color = LINE_COLOR_FADED;

		boolean sameRow = conn.getFrom().getGridRow() == conn.getTo().getGridRow() && conn.getFrom().getPixelY() == conn.getTo().getPixelY();
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
		boolean isBlurred = vis == NodeVisibility.BLURRED;

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
				case COMPLETED, CLAIMABLE -> icon = "✓";
				case ACTIVE -> {
					icon = "!";
					iconColor = 0xFFFFFF00;
				}
				case AVAILABLE -> icon = "?";
				default -> {
					icon = "✕";
					iconColor = 0xFF999999;
				}
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
			String hiddenLabel = "???";
			int numX = x + (NODE_SIZE - this.font.width(hiddenLabel)) / 2;
			drawStringWithBorder(graphics, Component.literal(hiddenLabel), numX, y + NODE_SIZE + 2, 0x55888888);
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
			title = "§kUnknown Quest§r";
			statusText = Component.translatable("gui.dragonminez.quest_tree.status.locked").getString();
		} else {
			title = Component.translatable(quest.getTitle()).getString();
			statusText = getStatusText(getNodeStatus(quest));
		}

		int tooltipWidth = Math.max(this.font.width(title), this.font.width(statusText)) + 12;
		int tooltipHeight = 26;
		int tooltipX = mouseX + 10;
		int tooltipY = mouseY - tooltipHeight - 5;

		graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xEE111122);
		graphics.renderOutline(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 0xFF555577);
		graphics.drawString(this.font, title, tooltipX + 5, tooltipY + 3, 0xFFFFFFFF, false);
		int statusColor = vis == NodeVisibility.BLURRED ? 0xFF888888 : getStatusColor(getNodeStatus(quest));
		graphics.drawString(this.font, statusText, tooltipX + 5, tooltipY + 14, statusColor, false);
	}

	// ========================================================================================
	// Action Button Glow
	// ========================================================================================

	/**
	 * Renders a soft pulsing glow effect around the action button.
	 */
	private void renderActionButtonGlow(GuiGraphics graphics) {
		if (actionButton == null) return;
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

		if (button != 0) return false;

		if (handleNavigatorClick(uiMouseX, uiMouseY)) {
			return true;
		}

		// Keep panel interactions isolated while the tree renders under them.
		if (getLeftPanelRect().contains(uiMouseX, uiMouseY) || getRightPanelRect().contains(uiMouseX, uiMouseY)) {
			return false;
		}

		PanelRect tree = getTreePanelRect();
		if (!tree.contains(uiMouseX, uiMouseY)) {
			return false;
		}

		int zoomedMouseX = (int) (uiMouseX / zoom);
		int zoomedMouseY = (int) (uiMouseY / zoom);
		if (currentLayout != null) {
			for (QuestTreeLayoutHelper.NodePosition node : currentLayout.getNodes()) {
				if (!isNodeHovered(node, zoomedMouseX, zoomedMouseY)) continue;
				if (getNodeVisibility(node.getQuest()) == NodeVisibility.BLURRED) return true;
				selectQuest(node.getQuest(), true);
				slideToNode(node);
				return true;
			}
		}

		isDraggingTree = true;
		isAnimatingPan = false;
		lastPanAnimNanos = 0L;
		dragStartX = uiMouseX;
		dragStartY = uiMouseY;
		dragStartPanX = panX;
		dragStartPanY = panY;
		return true;
	}

	private boolean handleNavigatorClick(double uiMouseX, double uiMouseY) {
		PanelRect panel = getLeftPanelRect();
		if (!panel.contains(uiMouseX, uiMouseY)) return false;

		int listX = panel.x + 10;
		int listY = panel.y + 28;
		int listW = panel.width - 20;
		int listH = panel.height - 38;
		if (uiMouseX < listX || uiMouseX > listX + listW || uiMouseY < listY || uiMouseY > listY + listH) return false;

		int visibleCount = Math.max(1, listH / NAV_ITEM_HEIGHT);
		int index = navScrollOffset + (int) ((uiMouseY - listY) / NAV_ITEM_HEIGHT);
		if (index < 0 || index >= navigatorEntries.size() || index >= navScrollOffset + visibleCount) return true;

		NavigatorEntry entry = navigatorEntries.get(index);
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MainSounds.PIP_MENU.get(), 1.0F));
		if (entry.type() == NavEntryType.SAGA) {
			if (!isSagaUnlockedByPreviousCompletion(entry.saga())) {
				return true;
			}
			int newIndex = availableSagas.indexOf(entry.saga());
			if (newIndex >= 0 && newIndex != currentSagaIndex) {
				currentSagaIndex = newIndex;
				selectedQuest = null;
				rebuildLayout();
				rebuildNavigatorEntries();
				refreshButtons();
			}
			return true;
		}

		if (entry.quest() != null) {
			selectQuest(entry.quest(), true);
			QuestTreeLayoutHelper.NodePosition node = findNodeForQuest(entry.quest());
			if (node != null) slideToNode(node);
		}
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (isDraggingTree && button == 0) {
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
		if (isDraggingTree) {
			isDraggingTree = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int scrollAmount = (int) Math.signum(delta);

		PanelRect left = getLeftPanelRect();
		if (left.contains(uiMouseX, uiMouseY)) {
			navScrollOffset = Math.max(0, Math.min(navMaxScroll, navScrollOffset - scrollAmount));
			return true;
		}

		// Do not zoom when scrolling over the right info panel.
		if (getRightPanelRect().contains(uiMouseX, uiMouseY)) {
			return super.mouseScrolled(mouseX, mouseY, delta);
		}

		PanelRect tree = getTreePanelRect();
		if (!tree.contains(uiMouseX, uiMouseY)) {
			return super.mouseScrolled(mouseX, mouseY, delta);
		}

		float oldZoom = zoom;
		float zoomDelta = scrollAmount * ZOOM_STEP;
		zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom + zoomDelta));

		if (zoom != oldZoom) {
			float scale = zoom / oldZoom;
			panX = (float) (uiMouseX - scale * (uiMouseX - panX));
			panY = (float) (uiMouseY - scale * (uiMouseY - panY));
		}

		return true;
	}

	private void selectQuest(Quest quest, boolean playClickSound) {
		if (quest == null) return;
		selectedQuest = quest;
		resetTypewriterForSelectedQuest();
		refreshButtons();
		if (playClickSound) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MainSounds.PIP_MENU.get(), 1.0F));
		}
	}

	private void resetTypewriterForSelectedQuest() {
		if (selectedQuest == null || availableSagas.isEmpty()) return;
		Saga saga = availableSagas.get(currentSagaIndex);
		String key = questProgressKey(saga, selectedQuest);
		initializeTypewriterSection(key, SECTION_DESC);
		initializeTypewriterSection(key, SECTION_REWARDS);
	}

	private void initializeTypewriterSection(String questKey, String section) {
		String key = questKey + "#" + section;
		long now = System.currentTimeMillis();
		long lastReveal = sectionLastReveal.getOrDefault(key, 0L);
		if (now - lastReveal >= TYPEWRITER_COOLDOWN_MS) {
			sectionAnimationStart.put(key, now);
		} else {
			sectionAnimationStart.remove(key);
		}
	}

	private QuestTreeLayoutHelper.NodePosition findNodeForQuest(Quest quest) {
		if (quest == null || currentLayout == null) return null;
		for (QuestTreeLayoutHelper.NodePosition node : currentLayout.getNodes()) {
			if (sameQuestIdentity(node.getQuest(), quest)) return node;
		}
		return null;
	}

	private NodeVisibility getNodeVisibility(Quest quest) {
		if (statsData == null || availableSagas.isEmpty()) return NodeVisibility.HIDDEN;
		Saga saga = availableSagas.get(currentSagaIndex);
		PlayerQuestData pqd = statsData.getPlayerQuestData();

		if (isQuestCompleted(pqd, saga, quest)) return NodeVisibility.VISIBLE;

		List<Quest> sagaQuests = saga.getQuests();
		int currentAvailableIndex = -1;
		for (int i = 0; i < sagaQuests.size(); i++) {
			if (!pqd.isQuestCompleted(saga.getId(), sagaQuests.get(i).getId())) {
				currentAvailableIndex = i;
				break;
			}
		}

		for (int i = 0; i < sagaQuests.size(); i++) {
			if (sagaQuests.get(i).getId() == quest.getId()) {
				if (i == currentAvailableIndex) return NodeVisibility.VISIBLE;
				if (i == currentAvailableIndex + 1) return NodeVisibility.BLURRED;
				return NodeVisibility.HIDDEN;
			}
		}

		if (quest.isSideQuest() && quest.hasPrerequisites()) {
			var conditions = quest.getPrerequisites().getConditions();
			for (var cond : conditions) {
				if (cond.getType() == QuestPrerequisites.ConditionType.SAGA_QUEST && saga.getId().equals(cond.getSagaId())) {
					Integer reqId = cond.getQuestId();
					if (reqId != null && pqd.isQuestCompleted(saga.getId(), reqId)) return NodeVisibility.VISIBLE;
					if (reqId != null && currentAvailableIndex >= 0 && sagaQuests.get(currentAvailableIndex).getId() == reqId) {
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
			for (int i = 0; i < quest.getRewards().size(); i++) {
				if (!isRewardClaimed(pqd, saga, quest, i)) {
					return QuestNodeStatus.CLAIMABLE;
				}
			}
			return QuestNodeStatus.COMPLETED;
		}

		if (quest.isSideQuest() && quest.hasPrerequisites()) {
			boolean allMet = true;
			for (var cond : quest.getPrerequisites().getConditions()) {
				if (cond.getType() == QuestPrerequisites.ConditionType.SAGA_QUEST) {
					Integer reqId = cond.getQuestId();
					String reqSaga = cond.getSagaId();
					if (reqId != null && reqSaga != null && !pqd.isQuestCompleted(reqSaga, reqId)) {
						allMet = false;
						break;
					}
				}
			}
			return allMet ? QuestNodeStatus.AVAILABLE : QuestNodeStatus.LOCKED;
		}

		for (Quest q : saga.getQuests()) {
			if (!pqd.isQuestCompleted(saga.getId(), q.getId())) {
				return q.getId() == quest.getId() ? QuestNodeStatus.AVAILABLE : QuestNodeStatus.LOCKED;
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
		if (quest.isSideQuest() && quest.getStringId() != null) return quest.getStringId();
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
			return a.isSideQuest() == b.isSideQuest() && a.getStringId() != null && a.getStringId().equals(b.getStringId());
		}
		return a.getId() == b.getId();
	}

	private List<String> wrapText(String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		if (text == null || text.isEmpty()) {
			lines.add("");
			return lines;
		}

		String[] paragraphs = text.split("\\n", -1);
		for (String paragraph : paragraphs) {
			if (paragraph.isEmpty()) {
				lines.add("");
				continue;
			}

			String[] words = paragraph.split(" ");
			StringBuilder currentLine = new StringBuilder();
			for (String word : words) {
				if (word.isEmpty()) continue;

				if (this.font.width(word) > maxWidth) {
					if (!currentLine.isEmpty()) {
						lines.add(currentLine.toString());
						currentLine = new StringBuilder();
					}
					for (String split : splitWordToWidth(word, maxWidth)) {
						lines.add(split);
					}
					continue;
				}

				String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
				if (this.font.width(testLine) <= maxWidth) {
					if (!currentLine.isEmpty()) currentLine.append(" ");
					currentLine.append(word);
				} else {
					lines.add(currentLine.toString());
					currentLine = new StringBuilder(word);
				}
			}
			if (!currentLine.isEmpty()) lines.add(currentLine.toString());
		}

		return lines;
	}

	private List<String> splitWordToWidth(String word, int maxWidth) {
		List<String> parts = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			String test = current.toString() + c;
			if (this.font.width(test) > maxWidth && !current.isEmpty()) {
				parts.add(current.toString());
				current = new StringBuilder().append(c);
			} else {
				current.append(c);
			}
		}
		if (!current.isEmpty()) parts.add(current.toString());
		return parts;
	}

	private String fitSingleLineEllipsis(String text, int maxWidth) {
		if (text == null) return "";
		if (this.font.width(text) <= maxWidth) return text;
		String ellipsis = "...";
		int ellipsisWidth = this.font.width(ellipsis);
		if (ellipsisWidth >= maxWidth) return ellipsis;

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			String candidate = builder.toString() + c;
			if (this.font.width(candidate) + ellipsisWidth > maxWidth) break;
			builder.append(c);
		}
		return builder + ellipsis;
	}

	private List<String> limitLinesWithEllipsis(List<String> lines, int maxLines, int maxWidth) {
		if (lines.size() <= maxLines) return lines;
		List<String> limited = new ArrayList<>();
		for (int i = 0; i < maxLines - 1; i++) {
			limited.add(lines.get(i));
		}
		StringBuilder last = new StringBuilder(lines.get(maxLines - 1));
		for (int i = maxLines; i < lines.size(); i++) {
			last.append(" ").append(lines.get(i));
		}
		limited.add(fitSingleLineEllipsis(last.toString(), maxWidth));
		return limited;
	}

	private void drawJustifiedTextBlock(GuiGraphics graphics, List<String> lines, int x, int y, int width, int maxLines, int color) {
		int count = Math.min(maxLines, lines.size());
		for (int i = 0; i < count; i++) {
			boolean lastLine = i == count - 1;
			drawJustifiedLine(graphics, lines.get(i), x, y + (i * 10), width, color, lastLine);
		}
	}

	private void drawJustifiedLine(GuiGraphics graphics, String line, int x, int y, int width, int color, boolean isLastLine) {
		String trimmed = line == null ? "" : line.trim();
		if (trimmed.isEmpty() || isLastLine) {
			drawStringWithBorder(graphics, Component.literal(trimmed), x, y, color);
			return;
		}

		String[] words = trimmed.split(" ");
		if (words.length <= 1) {
			drawStringWithBorder(graphics, Component.literal(trimmed), x, y, color);
			return;
		}

		int wordsWidth = 0;
		for (String word : words) wordsWidth += this.font.width(word);
		int spaces = words.length - 1;
		int baseSpace = this.font.width(" ");
		int totalBase = wordsWidth + (spaces * baseSpace);
		if (totalBase >= width || totalBase < (int) (width * 0.75f)) {
			drawStringWithBorder(graphics, Component.literal(trimmed), x, y, color);
			return;
		}

		int extra = width - totalBase;
		int extraPerSpace = extra / spaces;
		int remainder = extra % spaces;

		int cursorX = x;
		for (int i = 0; i < words.length; i++) {
			drawStringWithBorder(graphics, Component.literal(words[i]), cursorX, y, color);
			cursorX += this.font.width(words[i]);
			if (i < spaces) {
				cursorX += baseSpace + extraPerSpace + (i < remainder ? 1 : 0);
			}
		}
	}

	private PanelRect getLeftPanelRect() {
		PanelRect base = getBaseLeftPanelRect();
		int xOffset = getPanelSlideOffsetX(true, base.width);
		return new PanelRect(base.x + xOffset, base.y, base.width, base.height);
	}

	private PanelRect getRightPanelRect() {
		PanelRect base = getBaseRightPanelRect();
		int xOffset = getPanelSlideOffsetX(false, base.width);
		return new PanelRect(base.x + xOffset, base.y, base.width, base.height);
	}

	private PanelRect getBaseLeftPanelRect() {
		int third = getUiWidth() / 3;
		int width = Math.max(140, (int) (third * SIDE_PANEL_WIDTH_RATIO));
		int height = Math.min(getUiHeight(), Math.max(120, (int) (getUiHeight() * SIDE_PANEL_HEIGHT_RATIO)));
		int x = 0;
		int y = Math.max(0, (getUiHeight() - height) / 2);
		return new PanelRect(x, y, width, height);
	}

	private PanelRect getBaseRightPanelRect() {
		int third = getUiWidth() / 3;
		int width = Math.max(140, (int) (third * SIDE_PANEL_WIDTH_RATIO));
		int height = Math.min(getUiHeight(), Math.max(120, (int) (getUiHeight() * SIDE_PANEL_HEIGHT_RATIO)));
		int x = getUiWidth() - width;
		int y = Math.max(0, (getUiHeight() - height) / 2);
		return new PanelRect(x, y, width, height);
	}

	private void startPanelIntroAnimation() {
		panelIntroStartMs = System.currentTimeMillis();
		panelIntroActive = true;
	}

	private float getPanelIntroProgress() {
		if (!panelIntroActive) return 1.0f;
		long elapsed = System.currentTimeMillis() - panelIntroStartMs;
		if (elapsed >= PANEL_INTRO_DURATION_MS) {
			panelIntroActive = false;
			return 1.0f;
		}
		return Math.max(0.0f, Math.min(1.0f, elapsed / (float) PANEL_INTRO_DURATION_MS));
	}

	private int getPanelSlideOffsetX(boolean isLeftPanel, int panelWidth) {
		float t = getPanelIntroProgress();
		float eased = easeOutBack(t, PANEL_INTRO_BACK_OVERSHOOT);
		float travel = (1.0f - eased) * (panelWidth + PANEL_INTRO_EXTRA_TRAVEL_PX);
		int offset = Math.round(travel);
		return isLeftPanel ? -offset : offset;
	}

	private float easeOutBack(float t, float overshoot) {
		float shifted = t - 1.0f;
		float c3 = overshoot + 1.0f;
		return 1.0f + c3 * shifted * shifted * shifted + overshoot * shifted * shifted;
	}

	private void syncActionButtonPosition() {
		if (actionButton == null) return;
		PanelRect right = getRightPanelRect();
		actionButton.setX(right.x + (right.width - 74) / 2);
		actionButton.setY(right.bottom() - 28);
	}

	private PanelRect getTreePanelRect() {
		return new PanelRect(0, 0, getUiWidth(), getUiHeight());
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
		drawStringWithBorder(graphics, text, centerX - (textWidth / 2), y, textColor);
	}
}

