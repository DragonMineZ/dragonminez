package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.quest.QuestTreeLayoutHelper;
import com.dragonminez.client.util.LocalizationUtil;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.AcceptPartyInviteC2S;
import com.dragonminez.common.network.C2S.ClaimQuestRewardC2S;
import com.dragonminez.common.network.C2S.CreatePartyC2S;
import com.dragonminez.common.network.C2S.InvitePartyMemberC2S;
import com.dragonminez.common.network.C2S.LeavePartyC2S;
import com.dragonminez.common.network.C2S.QuestActionC2S;
import com.dragonminez.common.network.C2S.RejectPartyInviteC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.QuestPrerequisites;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.quest.rewards.ItemReward;
import com.dragonminez.common.quest.QuestAvailabilityChecker;
import com.dragonminez.common.quest.QuestTextFormatter;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class QuestTreeScreen extends BaseMenuScreen {

	private static final ResourceLocation QUEST_MENU = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/questmenu.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation EXCLAMATION_MARK = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/quest/exclamation_mark_quest.png");
	private static final ResourceLocation REWARD_GENERIC_ICON = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/quest/reward_generic.png");

	private static final int NODE_SIZE = 18;

	private StatsData statsData;
	private int tickCount = 0;
	private int pendingRefreshTicks = 0;

	private TexturedTextButton actionButton;
	private TexturedTextButton partyPrimaryButton;
	private TexturedTextButton partySecondaryButton;
	private List<Component> actionButtonTooltip = List.of();
	private long lastClickTime = 0;

	private int currentSagaIndex = 0;
	private final List<Saga> availableSagas = new ArrayList<>();

	private QuestTreeLayoutHelper.TreeLayout currentLayout;
	private Quest selectedQuest = null;

	private float panX = 0;
	private float panY = 0;
	private boolean isDraggingTree = false;
	private double dragStartX, dragStartY;
	private float dragStartPanX, dragStartPanY;

	private float targetPanX = 0;
	private float targetPanY = 0;
	private boolean isAnimatingPan = false;
	private long lastPanAnimNanos = 0L;

	private float zoom = 1.0f;

	private final List<NavigatorEntry> navigatorEntries = new ArrayList<>();
	private final Set<String> expandedSideBranches = new HashSet<>();

	private float targetNavScroll = 0;
	private float currentNavScroll = 0;
	private float navMaxScroll = 0;

	private float targetDescScroll = 0;
	private float currentDescScroll = 0;
	private float descMaxScroll = 0;

	private float targetObjScroll = 0;
	private float currentObjScroll = 0;
	private float objMaxScroll = 0;

	private final List<RewardHitbox> rewardHitboxes = new ArrayList<>();
	private final Map<String, Long> sectionLastReveal = new HashMap<>();
	private final Map<String, Long> sectionAnimationStart = new HashMap<>();

	private long panelIntroStartMs = 0L;
	private boolean panelIntroActive = false;
	private float leftPanelRevealProgress = 0.0f;
	private float rightPanelRevealProgress = 0.0f;
	private boolean treePressStarted = false;
	private boolean treePressMoved = false;
	private double treePressStartX = 0.0;
	private double treePressStartY = 0.0;

	private boolean invitePopupOpen = false;
	private int invitePopupScroll = 0;
	private final List<PartyInviteEntry> inviteEntries = new ArrayList<>();

	private boolean confirmOverlayOpen = false;
	private PartyConfirmAction confirmAction = PartyConfirmAction.NONE;
	private Component confirmTitle = Component.empty();
	private Component confirmBody = Component.empty();

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
		SIDE_QUEST,
		SECRET_SECTION,
		SECRET_SIDE_QUEST
	}

	private enum PartyConfirmAction {
		NONE,
		ACCEPT_INVITE,
		LEAVE_PARTY
	}

	private record SagaCatalogEntry(String id, String label, boolean comingSoon) {
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

	private record NavigatorEntry(NavEntryType type, int depth, Saga saga, Quest quest,
	                              String sagaId, String sagaLabel, boolean comingSoon) {
		boolean isPlaceholderSaga() {
			return type == NavEntryType.SAGA && saga == null && sagaId != null;
		}
	}

	private boolean isReachableNavigatorQuest(Quest quest) {
		return getNodeVisibility(quest) == NodeVisibility.VISIBLE && getNodeStatus(quest) != QuestNodeStatus.LOCKED;
	}

	private record PartyInviteEntry(UUID playerId, String playerName) {
	}

	private record RewardHitbox(int x, int y, int size, ItemStack stack, Component tooltip) {
		boolean contains(int mx, int my) {
			return mx >= x && mx <= x + size && my >= y && my <= y + size;
		}
	}

	private record DetailPanelLayout(int titleH, int rewardsH, int descH, int objectivesH) {
	}

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

	private static final List<SagaCatalogEntry> SAGA_CATALOG = List.of(
			new SagaCatalogEntry("saiyan_saga", "Saiyan Saga", false),
			new SagaCatalogEntry("frieza_saga", "Frieza Saga", false),
			new SagaCatalogEntry("android_saga", "Cell Saga", false),
			new SagaCatalogEntry("future_saga", "Future Saga", false),
			new SagaCatalogEntry("buu_saga", "Buu Saga", false),
			new SagaCatalogEntry("movies_saga", "Movies Saga", false),
			new SagaCatalogEntry("daima_saga", "Daima Saga", true),
			new SagaCatalogEntry("gt_saga", "GT Saga", true),
			new SagaCatalogEntry("dball_saga", "DBall Saga", true),
			new SagaCatalogEntry("beerus_saga", "Beerus Saga", true),
			new SagaCatalogEntry("rof_saga", "RoF Saga", true),
			new SagaCatalogEntry("u7vsu6_saga", "U7vsU6 Saga", true)
	);

	private static final Map<String, Integer> SAGA_UI_ORDER = Map.ofEntries(
			Map.entry("saiyan_saga", 0),
			Map.entry("frieza_saga", 1),
			Map.entry("android_saga", 2),
			Map.entry("cell_saga", 2),
			Map.entry("future_saga", 3),
			Map.entry("buu_saga", 4),
			Map.entry("movies_saga", 5),
			Map.entry("daima_saga", 6),
			Map.entry("gt_saga", 7),
			Map.entry("dball_saga", 8),
			Map.entry("beerus_saga", 9),
			Map.entry("rof_saga", 10),
			Map.entry("u7vsu6_saga", 11)
	);

	private void loadAvailableSagas() {
		availableSagas.clear();
		if (statsData == null) return;

		Map<String, Saga> allSagas = QuestRegistry.getClientSagas();
		if (allSagas.isEmpty()) return;

		availableSagas.addAll(allSagas.values());
		availableSagas.sort((s1, s2) -> {
			int o1 = SAGA_UI_ORDER.getOrDefault(s1.getId(), Integer.MAX_VALUE);
			int o2 = SAGA_UI_ORDER.getOrDefault(s2.getId(), Integer.MAX_VALUE);
			if (o1 != o2) return Integer.compare(o1, o2);
			return s1.getId().compareToIgnoreCase(s2.getId());
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

	private void centerViewOnProgress() {
		if (currentLayout == null) return;

		PanelRect tree = getTreePanelRect();
		zoom = 1.0f;

		QuestTreeLayoutHelper.NodePosition targetNode = null;
		if (statsData != null && !availableSagas.isEmpty()) {
			Saga saga = availableSagas.get(currentSagaIndex);
			PlayerQuestData pqd = statsData.getPlayerQuestData();
			List<Quest> sagaQuests = saga.getQuests();

			for (Quest q : sagaQuests) {
				boolean completed = pqd.isQuestCompleted(PlayerQuestData.sagaQuestKey(saga.getId(), q.getId()));

				for (QuestTreeLayoutHelper.NodePosition node : currentLayout.getNodes()) {
					if (node.getQuest().getId() == q.getId() && !node.isSidequest()) {
						targetNode = node;
						if (!completed) {
							break;
						}
					}
				}
				if (!completed) break;
			}

			if (targetNode == null && !currentLayout.getNodes().isEmpty()) {
				targetNode = currentLayout.getNodes().get(0);
			}
		}

		if (targetNode != null) {
			panX = (tree.x + tree.width / 2.0f) - targetNode.getPixelX() - ((float) NODE_SIZE / 2);
			panY = (tree.y + tree.height / 2.0f) - targetNode.getPixelY() - ((float) NODE_SIZE / 2);
		} else {
			panX = tree.x + 40;
			panY = tree.y + (tree.height - currentLayout.getTotalHeight()) / 2.0f;
		}
	}

	private void slideToNode(QuestTreeLayoutHelper.NodePosition node) {
		PanelRect tree = getTreePanelRect();
		targetPanX = (tree.x + tree.width / 2.0f) - (node.getPixelX() * zoom) - ((float) NODE_SIZE / 2) * zoom;
		targetPanY = (tree.y + tree.height / 2.0f) - (node.getPixelY() * zoom) - ((float) NODE_SIZE / 2) * zoom;
		isAnimatingPan = true;
		lastPanAnimNanos = System.nanoTime();
	}

	private void rebuildNavigatorEntries() {
		navigatorEntries.clear();
		Map<String, Saga> loadedSagas = new LinkedHashMap<>();
		for (Saga saga : availableSagas) {
			loadedSagas.put(saga.getId(), saga);
		}

		List<String> displayedSagaIds = new ArrayList<>();
		Saga currentSaga = availableSagas.isEmpty() ? null : availableSagas.get(currentSagaIndex);
		for (SagaCatalogEntry entry : SAGA_CATALOG) {
			Saga saga = loadedSagas.get(entry.id());
			if (saga == null && "android_saga".equals(entry.id())) {
				saga = loadedSagas.get("cell_saga");
			}
			if (saga != null) {
				navigatorEntries.add(new NavigatorEntry(NavEntryType.SAGA, 0, saga, null,
						saga.getId(), getSagaDisplayName(saga), false));
				displayedSagaIds.add(saga.getId());
				if (currentSaga != null && currentSaga.getId().equals(saga.getId())) {
					appendCurrentSagaQuestEntries(currentSaga);
				}
				continue;
			}

			navigatorEntries.add(new NavigatorEntry(NavEntryType.SAGA, 0, null, null,
					entry.id(), entry.label(), entry.comingSoon()));
			displayedSagaIds.add(entry.id());
		}

		for (Saga saga : availableSagas) {
			if (displayedSagaIds.contains(saga.getId())) continue;
			navigatorEntries.add(new NavigatorEntry(NavEntryType.SAGA, 0, saga, null,
					saga.getId(), getSagaDisplayName(saga), false));
			if (currentSaga != null && currentSaga.getId().equals(saga.getId())) {
				appendCurrentSagaQuestEntries(currentSaga);
			}
		}

		PanelRect left = getLeftPanelRect();
		int usableHeight = Math.max(32, left.height - 40 - getPartyFooterHeight());
		int totalNavHeight = navigatorEntries.size() * 13;
		navMaxScroll = Math.max(0, totalNavHeight - usableHeight);
		targetNavScroll = Math.max(0, Math.min(targetNavScroll, navMaxScroll));
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
			if (!isReachableNavigatorQuest(child)) {
				continue;
			}
			navigatorEntries.add(new NavigatorEntry(NavEntryType.SIDE_QUEST, depth, saga, child,
					null, null, false));
			if (isSideBranchExpanded(saga, child)) {
				addSideBranchEntries(child, sideBranches, saga, depth + 1);
			}
		}
	}

	private void appendCurrentSagaQuestEntries(Saga currentSaga) {
		Map<String, List<Quest>> sideBranches = buildSideBranchesForSaga(currentSaga);
		for (Quest mainQuest : currentSaga.getQuests()) {
			if (!isReachableNavigatorQuest(mainQuest)) {
				continue;
			}
			navigatorEntries.add(new NavigatorEntry(NavEntryType.MAIN_QUEST, 1, currentSaga, mainQuest,
					null, null, false));
			if (isSideBranchExpanded(currentSaga, mainQuest)) {
				addSideBranchEntries(mainQuest, sideBranches, currentSaga, 2);
			}
		}
		appendSecretSideQuestEntries(currentSaga);
	}

	private void appendSecretSideQuestEntries(Saga currentSaga) {
		if (currentSaga == null || statsData == null) return;

		List<Quest> discovered = new ArrayList<>();
		for (Quest quest : QuestRegistry.getClientQuests().values()) {
			if (!quest.isSideQuest() || !quest.isSecret()) continue;
			if (!QuestTreeLayoutHelper.belongsToSaga(quest, currentSaga.getId())) continue;
			if (isSecretQuestDiscovered(currentSaga, quest)) {
				discovered.add(quest);
			}
		}
		if (discovered.isEmpty()) return;

		discovered.sort(Comparator.comparing(q -> q.getStringId() != null ? q.getStringId() : ""));
		navigatorEntries.add(new NavigatorEntry(NavEntryType.SECRET_SECTION, 1, currentSaga, null,
				null, tr("gui.dragonminez.quest_tree.secret_sidequests").getString(), false));
		for (Quest quest : discovered) {
			navigatorEntries.add(new NavigatorEntry(NavEntryType.SECRET_SIDE_QUEST, 2, currentSaga, quest,
					null, null, false));
		}
	}

	private String getSagaDisplayName(Saga saga) {
		if (saga == null) return "?";
		if ("cell_saga".equalsIgnoreCase(saga.getId())) {
			return "Cell Saga";
		}
		SagaCatalogEntry entry = getSagaCatalogEntry(saga.getId());
		if (entry != null) {
			return entry.label();
		}
		return tr(saga.getName()).getString();
	}

	private SagaCatalogEntry getSagaCatalogEntry(String sagaId) {
		if (sagaId == null || sagaId.isBlank()) return null;
		for (SagaCatalogEntry entry : SAGA_CATALOG) {
			if (entry.id().equalsIgnoreCase(sagaId)) {
				return entry;
			}
		}
		return null;
	}

	private boolean isSagaUnlockedByPreviousCompletion(Saga saga) {
		if (saga == null || saga.getRequirements() == null) return true;
		String previousSagaId = saga.getRequirements().previousSagaId();
		if (previousSagaId == null || previousSagaId.isEmpty()) return true;
		if (statsData == null) return false;

		Map<String, Saga> allSagas = QuestRegistry.getClientSagas();
		Saga previousSaga = allSagas.get(previousSagaId);
		if (previousSaga == null) return true;

		PlayerQuestData pqd = statsData.getPlayerQuestData();
		for (Quest q : previousSaga.getQuests()) {
			if (!pqd.isQuestCompleted(PlayerQuestData.sagaQuestKey(previousSaga.getId(), q.getId()))) {
				return false;
			}
		}
		return true;
	}

	private Component getSagaLockTooltip(Saga saga) {
		if (saga == null || isSagaUnlockedByPreviousCompletion(saga) || saga.getRequirements() == null) {
			return null;
		}

		String previousSagaId = saga.getRequirements().previousSagaId();
		if (previousSagaId == null || previousSagaId.isBlank()) {
			return null;
		}

		Saga previousSaga = QuestRegistry.getClientSagas().get(previousSagaId);
		String previousSagaName = previousSaga != null
				? getSagaDisplayName(previousSaga)
				: getSagaDisplayName(previousSagaId);
		return tr("gui.dragonminez.quest_tree.saga_locked.tooltip", previousSagaName);
	}

	private String getSagaDisplayName(String sagaId) {
		SagaCatalogEntry entry = getSagaCatalogEntry(sagaId);
		return entry != null ? entry.label() : QuestTextFormatter.humanizeIdentifier(sagaId);
	}

	private boolean isSecretQuestDiscovered(Saga saga, Quest quest) {
		if (statsData == null || saga == null || quest == null) return false;
		PlayerQuestData pqd = statsData.getPlayerQuestData();
		String questKey = questProgressKey(saga, quest);
		if (pqd.isQuestCompleted(questKey)) return true;
		PlayerQuestData.QuestStatus status = pqd.getQuestStatus(questKey);
		return status == PlayerQuestData.QuestStatus.ACCEPTED || status == PlayerQuestData.QuestStatus.FAILED;
	}

	private boolean hasReachableSideBranch(Saga saga, Quest parentQuest) {
		Map<String, List<Quest>> sideBranches = buildSideBranchesForSaga(saga);
		return hasReachableSideBranch(saga, sideBranches, parentQuest);
	}

	private boolean hasReachableSideBranch(Saga saga, Map<String, List<Quest>> sideBranches, Quest parentQuest) {
		List<Quest> children = sideBranches.get(questProgressKey(saga, parentQuest));
		if (children == null || children.isEmpty()) return false;
		for (Quest child : children) {
			if (isReachableNavigatorQuest(child) || hasReachableSideBranch(saga, sideBranches, child)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSideBranchExpanded(Saga saga, Quest parentQuest) {
		return expandedSideBranches.contains(questProgressKey(saga, parentQuest));
	}

	private void toggleSideBranch(Saga saga, Quest parentQuest) {
		String key = questProgressKey(saga, parentQuest);
		if (!expandedSideBranches.remove(key)) {
			expandedSideBranches.add(key);
		}
		rebuildNavigatorEntries();
	}

	private void refreshButtons() {
		this.clearWidgets();
		actionButton = null;
		partyPrimaryButton = null;
		partySecondaryButton = null;
		actionButtonTooltip = List.of();
		initNavigationButtons();
		initPartyButtons();
		initActionButton();
	}

	private void initActionButton() {
		if (selectedQuest == null || statsData == null || availableSagas.isEmpty()) return;

		PlayerQuestData questData = statsData.getPlayerQuestData();
		Saga currentSaga = availableSagas.get(currentSagaIndex);
		String selectedKey = questProgressKey(currentSaga, selectedQuest);
		boolean isCompleted = isQuestCompleted(questData, currentSaga, selectedQuest);
		boolean canStart = canStartQuest(selectedQuest);
		boolean showDisabledStart = !canStart && getNodeStatus(selectedQuest) == QuestNodeStatus.AVAILABLE;
		Component buttonText;
		boolean buttonActive = true;
		boolean isClaimAction = false;
		List<Component> tooltipLines = List.of();

		if (isCompleted) {
			boolean hasUnclaimedRewards = false;
			for (int i = 0; i < selectedQuest.getRewards().size(); i++) {
				if (!isRewardClaimed(questData, currentSaga, selectedQuest, i)) {
					hasUnclaimedRewards = true;
					break;
				}
			}
			if (hasUnclaimedRewards) {
				isClaimAction = true;
				if (selectedQuest.getClaimMode() == Quest.ClaimMode.NPC_ONLY) {
					buttonText = tr("gui.dragonminez.quests.claim_from_npc");
					buttonActive = false;
					tooltipLines = List.of(tr("gui.dragonminez.quests.claim_from_npc.tooltip"));
				} else {
					buttonText = tr("gui.dragonminez.quests.claim_rewards");
				}
			} else {
				return;
			}
		} else if (canStart || showDisabledStart) {
			buttonText = tr("gui.dragonminez.quests.start");
			buttonActive = canStart;
		} else {
			return;
		}

		if (isClaimAction && buttonActive && isInSharedPartyAsMember()) {
			buttonText = tr("gui.dragonminez.party.leader_only");
			buttonActive = false;
			tooltipLines = List.of(tr("gui.dragonminez.party.leader_only"));
		} else if (!buttonActive && tooltipLines.isEmpty()) {
			tooltipLines = buildQuestBlockerTooltip(selectedQuest, currentSaga, true);
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
						NetworkHandler.sendToServer(new ClaimQuestRewardC2S(selectedKey));
						btn.visible = false;
						pendingRefreshTicks = 5;
					} else {
						boolean isHard = ConfigManager.getUserConfig().getStoryHardDifficulty();
						NetworkHandler.sendToServer(new QuestActionC2S(QuestActionC2S.ActionType.START, selectedKey, isHard, ""));
						this.onClose();
					}
				})
				.build();

		actionButton.active = buttonActive;
		actionButtonTooltip = tooltipLines;
		this.addRenderableWidget(actionButton);
	}

	private void initPartyButtons() {
		if (statsData == null) return;

		PlayerQuestData questData = statsData.getPlayerQuestData();
		PlayerQuestData.PartyInviteData invite = getVisiblePartyInvite();
		boolean inParty = questData.isInParty();
		boolean isLeader = isLocalPartyLeader();
		boolean hasOtherPlayers = hasOtherOnlinePlayers();

		if (invite == null && !inParty && !hasOtherPlayers) {
			return;
		}

		PanelRect footer = getPartyFooterRect();
		if (footer == null) return;

		if (invite != null) {
			partyPrimaryButton = buildPartyButton(
					tr("quest.dmz.party.invite.accept"),
					footer.x + (footer.width - 74) / 2,
					footer.bottom() - 46,
					btn -> {
						if (questData.isInParty()) {
							requestConfirm(
									PartyConfirmAction.ACCEPT_INVITE,
									tr("gui.dragonminez.party.confirm.title"),
									tr("gui.dragonminez.party.confirm.leave_current", txt(resolveInviteName(invite)))
							);
						} else {
							NetworkHandler.sendToServer(new AcceptPartyInviteC2S());
							queuePartyRefresh();
						}
					}
			);

			partySecondaryButton = buildPartyButton(
					tr("quest.dmz.party.invite.reject"),
					footer.x + (footer.width - 74) / 2,
					footer.bottom() - 20,
					btn -> {
						NetworkHandler.sendToServer(new RejectPartyInviteC2S());
						queuePartyRefresh();
					}
			);
			return;
		}

		if (inParty) {
			if (isLeader) {
				partyPrimaryButton = buildPartyButton(
						tr("gui.dragonminez.party.invite_players"),
						footer.x + (footer.width - 74) / 2,
						footer.bottom() - 46,
						btn -> openInvitePopup()
				);
				partySecondaryButton = buildPartyButton(
						tr("gui.dragonminez.party.disband"),
						footer.x + (footer.width - 74) / 2,
						footer.bottom() - 20,
						btn -> requestConfirm(
								PartyConfirmAction.LEAVE_PARTY,
								tr("gui.dragonminez.party.disband"),
								tr("gui.dragonminez.party.confirm.disband")
						)
				);
			} else {
				partyPrimaryButton = buildPartyButton(
						tr("gui.dragonminez.party.leave"),
						footer.x + (footer.width - 74) / 2,
						footer.bottom() - 20,
						btn -> requestConfirm(
								PartyConfirmAction.LEAVE_PARTY,
								tr("gui.dragonminez.party.leave"),
								tr("gui.dragonminez.party.confirm.leave")
						)
				);
			}
			return;
		}

		partyPrimaryButton = buildPartyButton(
				tr("gui.dragonminez.party.create"),
				footer.x + (footer.width - 74) / 2,
				footer.bottom() - 20,
				btn -> {
					NetworkHandler.sendToServer(new CreatePartyC2S());
					openInvitePopup();
					queuePartyRefresh();
				}
		);
	}

	private TexturedTextButton buildPartyButton(Component label, int x, int y, net.minecraft.client.gui.components.Button.OnPress onPress) {
		TexturedTextButton button = new TexturedTextButton.Builder()
				.position(x, y)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(label)
				.onPress(onPress)
				.build();
		this.addRenderableWidget(button);
		return button;
	}

	private boolean canStartQuest(Quest quest) {
		if (statsData == null || availableSagas.isEmpty() || quest == null) return false;
		Saga currentSaga = availableSagas.get(currentSagaIndex);
		PlayerQuestData questData = statsData.getPlayerQuestData();
		String questKey = questProgressKey(currentSaga, quest);
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return false;
		if (questData.isQuestCompleted(questKey)) return false;
		PlayerQuestData.QuestStatus status = questData.getQuestStatus(questKey);
		if (status == PlayerQuestData.QuestStatus.ACCEPTED) return false;
		if (status == PlayerQuestData.QuestStatus.FAILED) return true;
		if (getNodeStatus(quest) != QuestNodeStatus.AVAILABLE) return false;
		return QuestAvailabilityChecker.areStartRequirementsMet(quest, questKey, mc.player, statsData);
	}

	@Override
	public void tick() {
		super.tick();
		tickCount++;

		if (tickCount >= 10) {
			tickCount = 0;
			updateStatsData();
			rebuildNavigatorEntries();
			refreshButtons();
			if (invitePopupOpen) {
				rebuildInviteEntries();
				if (getVisiblePartyInvite() != null) {
					invitePopupOpen = false;
				}
			}
		}

		if (pendingRefreshTicks > 0) {
			pendingRefreshTicks--;
			if (pendingRefreshTicks == 0) {
				updateStatsData();
				rebuildNavigatorEntries();
				refreshButtons();
				if (invitePopupOpen) {
					rebuildInviteEntries();
				}
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

			float alpha = (float) (1.0 - Math.exp(-14.0f * dt));
			panX += (targetPanX - panX) * alpha;
			panY += (targetPanY - panY) * alpha;

			if (Math.abs(targetPanX - panX) < 0.35f && Math.abs(targetPanY - panY) < 0.35f) {
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

	@Override
	public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (isNotAnimating()) this.renderBackground(graphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));
		updatePanelInteractionAnimations(uiMouseX, uiMouseY, partialTick);

		beginUiScale(graphics);
		applyZoom(graphics);
		syncActionButtonPosition();
		syncPartyButtonPositions();
		rewardHitboxes.clear();

		renderTreeCanvas(graphics, uiMouseX, uiMouseY);
		renderLeftNavigatorPanel(graphics, uiMouseX, uiMouseY);
		renderRightDetailPanel(graphics, uiMouseX, uiMouseY);

		super.render(graphics, uiMouseX, uiMouseY, partialTick);

		if (actionButton != null && actionButton.visible && actionButton.active) {
			renderActionButtonGlow(graphics);
		}

		if (invitePopupOpen) {
			renderInvitePopup(graphics, uiMouseX, uiMouseY);
		}
		if (confirmOverlayOpen) {
			renderConfirmOverlay(graphics, uiMouseX, uiMouseY);
		}
		if (!invitePopupOpen && !confirmOverlayOpen) {
			if (!renderActionButtonTooltip(graphics, uiMouseX, uiMouseY)) {
				renderRewardTooltips(graphics, uiMouseX, uiMouseY);
			}
		}
		endUiScale(graphics);
	}

	private void renderTreeCanvas(GuiGraphics graphics, int mouseX, int mouseY) {
		if (currentLayout == null || availableSagas.isEmpty()) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font,
					tr("gui.dragonminez.quest_tree.no_sagas"),
					getUiWidth() / 2, getUiHeight() / 2, 0xFFAAAAAA);
			return;
		}

		PanelRect tree = getTreePanelRect();
		graphics.enableScissor(toScreenCoord(tree.x), toScreenCoord(tree.y), toScreenCoord(tree.right()), toScreenCoord(tree.bottom()));
		renderBackgroundGrid(graphics, tree);

		Saga currentSaga = availableSagas.get(currentSagaIndex);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				txt(getSagaDisplayName(currentSaga)).withStyle(ChatFormatting.BOLD),
				tree.x + tree.width / 2,
				tree.y + 8,
				0xFFFFD700);

		graphics.pose().pushPose();
		graphics.pose().scale(zoom, zoom, 1.0f);

		int zoomedMouseX = (int) (mouseX / zoom);
		int zoomedMouseY = (int) (mouseY / zoom);

		for (QuestTreeLayoutHelper.NodeConnection conn : currentLayout.getConnections()) {
			renderConnection(graphics, conn);
		}

		Quest hoveredQuest = null;
		for (QuestTreeLayoutHelper.NodePosition node : currentLayout.getNodes()) {
			boolean isHovered = isNodeHovered(node, zoomedMouseX, zoomedMouseY);
			if (isHovered) hoveredQuest = node.getQuest();
			renderNode(graphics, node, isHovered);
		}

		graphics.pose().popPose();

		if (hoveredQuest != null) {
			renderNodeTooltip(graphics, hoveredQuest, mouseX, mouseY);
		}

		String zoomText = (int) (zoom * 100) + "%";
		TextUtil.drawStringWithBorder(graphics, this.font, txt(zoomText), 6, tree.bottom() - 12, 0xFF888888);

		graphics.disableScissor();
	}

	private void renderBackgroundGrid(GuiGraphics graphics, PanelRect tree) {
		int spacing = 20;
		int offsetX = ((int) panX) % spacing;
		int offsetY = ((int) panY) % spacing;

		for (int x = tree.x + offsetX; x < tree.right(); x += spacing) {
			graphics.fill(x, tree.y, x + 1, tree.bottom(), 0xFF222244);
		}
		for (int y = tree.y + offsetY; y < tree.bottom(); y += spacing) {
			graphics.fill(tree.x, y, tree.right(), y + 1, 0xFF222244);
		}
	}

	private void renderLeftNavigatorPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		PanelRect panel = getLeftPanelRect();
		renderSidePanelBackground(graphics, panel, true, false, false);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.quest_tree.title").copy().withStyle(ChatFormatting.BOLD),
				panel.x + panel.width / 2,
				panel.y + 10,
				0xFFFFD700);

		int listX = panel.x + 10;
		int listY = panel.y + 28;
		int listW = panel.width - 20;

		int listH = Math.max(32, panel.height - 38 - getPartyFooterHeight());
		int totalNavHeight = navigatorEntries.size() * 13;
		navMaxScroll = Math.max(0, totalNavHeight - listH);
		targetNavScroll = Mth.clamp(targetNavScroll, 0, navMaxScroll);

		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();
		currentNavScroll = Mth.lerp(tickDelta * 0.4f, currentNavScroll, targetNavScroll);

		NavigatorEntry hoveredEntry = null;

		graphics.enableScissor(toScreenCoord(listX), toScreenCoord(listY), toScreenCoord(listX + listW), toScreenCoord(listY + listH));
		graphics.pose().pushPose();
		graphics.pose().translate(0, -currentNavScroll, 0);

		for (int i = 0; i < navigatorEntries.size(); i++) {
			NavigatorEntry entry = navigatorEntries.get(i);
			int rowY = listY + (i * 13);

			if (rowY + 13 >= listY + currentNavScroll && rowY <= listY + listH + currentNavScroll) {
				boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= rowY - currentNavScroll && mouseY <= rowY + 13 - currentNavScroll;
				if (hovered) hoveredEntry = entry;
				renderNavigatorEntry(graphics, entry, listX, rowY, listW, hovered);
			}
		}

		graphics.pose().popPose();
		graphics.disableScissor();

		if (navMaxScroll > 0) {
			int scrollBarX = listX + listW - 3;
			graphics.fill(scrollBarX, listY, scrollBarX + 2, listY + listH, 0xFF333333);
			float scrollPercent = navMaxScroll == 0 ? 0.0f : currentNavScroll / navMaxScroll;
			int indicatorHeight = Math.max(10, (int) ((float) listH / totalNavHeight * listH));
			int indicatorY = listY + (int) ((listH - indicatorHeight) * scrollPercent);
			graphics.fill(scrollBarX, indicatorY, scrollBarX + 2, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}

		if (hoveredEntry != null) {
			if (hoveredEntry.comingSoon()) {
				renderSimpleTooltip(graphics,
						List.of(txt("Coming soon... Follow development in Discord!")),
						mouseX,
						mouseY);
			} else if (hoveredEntry.type() == NavEntryType.SAGA && !hoveredEntry.isPlaceholderSaga()) {
				Component lockTooltip = getSagaLockTooltip(hoveredEntry.saga());
				if (lockTooltip != null) {
					renderSimpleTooltip(graphics, List.of(lockTooltip), mouseX, mouseY);
				}
			} else if (hoveredEntry.type() == NavEntryType.SECRET_SECTION) {
				renderSimpleTooltip(graphics,
						List.of(tr("gui.dragonminez.quest_tree.secret_sidequests.tooltip")),
						mouseX,
						mouseY);
			}
		}

		renderPartyFooter(graphics, panel);
	}

	private void renderNavigatorEntry(GuiGraphics graphics, NavigatorEntry entry, int x, int y, int rowWidth, boolean hovered) {
		int color;
		Component text;
		int textY = y + 2;

		if (entry.type() == NavEntryType.SECRET_SECTION) {
			color = hovered ? 0xFFFFE08A : 0xFFFFCC55;
			String raw = "* " + entry.sagaLabel();
			String clipped = fitSingleLineEllipsis(raw, Math.max(24, rowWidth - 8));
			text = txt(clipped).withStyle(ChatFormatting.BOLD);
			TextUtil.drawStringWithBorder(graphics, this.font, text, x + (entry.depth() * 10), textY, color);
			return;
		}

		if (entry.type() == NavEntryType.SAGA) {
			if (entry.isPlaceholderSaga()) {
				color = hovered && entry.comingSoon() ? 0xFFAAAAAA : 0xFF666666;
				String raw = "[L] " + entry.sagaLabel();
				String clipped = fitSingleLineEllipsis(raw, Math.max(24, rowWidth - 8));
				text = txt(clipped).withStyle(ChatFormatting.BOLD);
				TextUtil.drawStringWithBorder(graphics, this.font, text, x, textY, color);
				return;
			}

			boolean selectedSaga = !availableSagas.isEmpty() && entry.saga() == availableSagas.get(currentSagaIndex);
			boolean unlocked = isSagaUnlockedByPreviousCompletion(entry.saga());
			color = selectedSaga ? 0xFFFFCC55 : (unlocked ? 0xFFFFFFFF : 0xFF888888);
			if (hovered && unlocked) color = 0xFFFFE08A;
			String prefix = selectedSaga ? "v " : (unlocked ? "> " : "[L] ");
			String raw = prefix + entry.sagaLabel();
			String clipped = fitSingleLineEllipsis(raw, Math.max(24, rowWidth - 8));
			text = txt(clipped).withStyle(ChatFormatting.BOLD);
		} else {
			Quest q = entry.quest();
			String branchPrefix = "";
			if ((entry.type() == NavEntryType.MAIN_QUEST || entry.type() == NavEntryType.SIDE_QUEST)
					&& hasReachableSideBranch(entry.saga(), q)) {
				branchPrefix = isSideBranchExpanded(entry.saga(), q) ? "v " : "> ";
			}
			String label = q.isSideQuest()
					? branchPrefix + "- " + LocalizationUtil.localizedOrReadableText(q.getTitle())
					: branchPrefix + q.getId() + ". " + LocalizationUtil.localizedOrReadableText(q.getTitle());

			int indent = entry.depth() * 10;
			String clipped = fitSingleLineEllipsis(label, Math.max(24, rowWidth - indent - 8));
			text = txt(clipped);

			QuestNodeStatus status = getNodeStatus(q);
			color = getStatusColor(status);
			if (sameQuestIdentity(selectedQuest, q)) color = 0xFFFFFFFF;
			if (hovered) color = 0xFFFFD070;
		}

		int indent = entry.depth() * 10;
		TextUtil.drawStringWithBorder(graphics, this.font, text, x + indent, textY, color);
	}

	private void renderPartyFooter(GuiGraphics graphics, PanelRect panel) {
		PanelRect footer = getPartyFooterRect();
		if (footer == null || statsData == null) return;

		PlayerQuestData questData = statsData.getPlayerQuestData();
		PlayerQuestData.PartyInviteData invite = getVisiblePartyInvite();

		graphics.fill(footer.x, footer.y, footer.right(), footer.bottom(), 0x55111122);
		graphics.renderOutline(footer.x, footer.y, footer.width, footer.height, 0x88444466);

		TextUtil.drawStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.party.title").copy().withStyle(ChatFormatting.BOLD),
				footer.x + 6,
				footer.y + 4,
				0xFFFFD700);

		int textY = footer.y + 18;
		int textW = footer.width - 12;
		if (invite != null) {
			TextUtil.drawStringWithBorder(graphics, this.font,
					txt(fitSingleLineEllipsis(tr("gui.dragonminez.party.invited_by", resolveInviteName(invite)).getString(), textW)),
					footer.x + 6,
					textY,
					0xFFFFFFFF);
			textY += 10;

			if (questData.isInParty()) {
				TextUtil.drawStringWithBorder(graphics, this.font,
						txt(fitSingleLineEllipsis(tr("gui.dragonminez.party.invite_warning").getString(), textW)),
						footer.x + 6,
						textY,
						0xFFFFAA66);
			} else {
				TextUtil.drawStringWithBorder(graphics, this.font,
						txt(fitSingleLineEllipsis(tr("gui.dragonminez.party.invite_open_quest").getString(), textW)),
						footer.x + 6,
						textY,
						0xFFBBD6FF);
			}
			return;
		}

		if (questData.isInParty()) {
			String roleKey = isLocalPartyLeader()
					? "gui.dragonminez.party.role.leader"
					: "gui.dragonminez.party.role.member";
			TextUtil.drawStringWithBorder(graphics, this.font,
					txt(fitSingleLineEllipsis(tr(roleKey).getString(), textW)),
					footer.x + 6,
					textY,
					0xFFFFFFFF);
			textY += 10;

			TextUtil.drawStringWithBorder(graphics, this.font,
					txt(fitSingleLineEllipsis(buildPartyMemberSummary(), textW)),
					footer.x + 6,
					textY,
					0xFFBBD6FF);
			return;
		}

		if (hasOtherOnlinePlayers()) {
			TextUtil.drawStringWithBorder(graphics, this.font,
					txt(fitSingleLineEllipsis(tr("gui.dragonminez.party.multiplayer_ready").getString(), textW)),
					footer.x + 6,
					textY,
					0xFFFFFFFF);
			textY += 10;
			TextUtil.drawStringWithBorder(graphics, this.font,
					txt(fitSingleLineEllipsis(tr("gui.dragonminez.party.create_hint").getString(), textW)),
					footer.x + 6,
					textY,
					0xFFBBD6FF);
		}
	}

	private int getPartyFooterHeight() {
		if (statsData == null) return 0;
		PlayerQuestData questData = statsData.getPlayerQuestData();
		if (getVisiblePartyInvite() != null) return 94;
		if (questData.isInParty()) return isLocalPartyLeader() ? 94 : 70;
		return hasOtherOnlinePlayers() ? 64 : 0;
	}

	private PanelRect getPartyFooterRect() {
		int footerHeight = getPartyFooterHeight();
		if (footerHeight <= 0) return null;
		PanelRect panel = getLeftPanelRect();
		return new PanelRect(panel.x + 8, panel.bottom() - footerHeight, panel.width - 16, footerHeight - 6);
	}

	private void renderRightDetailPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		if (rightPanelRevealProgress <= 0.001f && selectedQuest == null) {
			return;
		}

		PanelRect panel = getRightPanelRect();
		renderSidePanelBackground(graphics, panel, false, true, false);

		if (selectedQuest == null || statsData == null || availableSagas.isEmpty()) {
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
		int lineHeight = getDetailLineHeight();
		boolean hasObjectiveContent = selectedQuest.hasStartRequirements() || !selectedQuest.getObjectives().isEmpty();
		int titleH = estimateTitleSectionHeight(width);
		int rewardsMin = selectedQuest.getRewards().isEmpty() ? 24 : Math.max(36, lineHeight + 24);
		int objectivesMin = hasObjectiveContent ? Math.max(36, lineHeight + 24) : 24;
		int descMin = Math.max(66, (lineHeight * 3) + 26);

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
		int lineHeight = getDetailLineHeight();
		String title = LocalizationUtil.localizedOrReadableText(selectedQuest.getTitle());
		List<String> lines = limitLinesWithEllipsis(wrapText(title, width - 10), 2, width - 10);
		return Math.max(32, 16 + (lines.size() * lineHeight) + 8);
	}

	private int estimateRewardsSectionHeight(int width, String questKey) {
		if (selectedQuest.getRewards().isEmpty()) return 28;
		String visibleRewardsText = resolveTypewriterText(questKey, "rewards", buildRewardsText(selectedQuest.getRewards()));
		List<String> lines = wrapText(visibleRewardsText, width - 36);
		int rows = Math.max(selectedQuest.getRewards().size(), lines.size());
		return 24 + (rows * getRewardRowHeight()) + 6;
	}

	private int estimateObjectivesSectionHeight(int width, Saga saga) {
		if (!selectedQuest.hasStartRequirements() && selectedQuest.getObjectives().isEmpty()) return 28;
		List<String> objectiveLines = buildObjectiveRenderLines(saga, width - 30);
		return 24 + (objectiveLines.size() * getDetailLineHeight()) + 6;
	}

	private void renderTopSection(GuiGraphics graphics, int x, int y, int width, int height, QuestNodeStatus status) {
		graphics.fill(x, y, x + width, y + height, 0x44111122);
		graphics.renderOutline(x, y, width, height, 0x88444466);
		int lineHeight = getDetailLineHeight();

		List<String> wrappedTitle = limitLinesWithEllipsis(
				wrapText(LocalizationUtil.localizedOrReadableText(selectedQuest.getTitle()), width - 10),
				2,
				width - 10
		);

		int titleStartY = y + 6;
		for (String line : wrappedTitle) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(line).withStyle(ChatFormatting.BOLD), x + width / 2, titleStartY, 0xFFFFFFFF);
			titleStartY += lineHeight;
		}

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				txt(fitSingleLineEllipsis(getStatusText(status).getString(), width - 12)),
				x + width / 2,
				height >= 40 ? y + height - lineHeight - 4 : y + 20,
				getStatusColor(status));
	}

	private void renderRewardsSection(GuiGraphics graphics, int x, int y, int width, int height, String questKey, int mouseX, int mouseY) {
		graphics.fill(x, y, x + width, y + height, 0x44111122);
		graphics.renderOutline(x, y, width, height, 0x88444466);

		TextUtil.drawStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.quests.rewards").copy().withStyle(ChatFormatting.BOLD),
				x + 6,
				y + 4,
				0xFFFFD700);

		List<QuestReward> rewards = selectedQuest.getRewards();
		if (rewards.isEmpty()) {
			TextUtil.drawStringWithBorder(graphics, this.font, txt("-"), x + 8, y + 18, 0xFF999999);
			return;
		}

		String rawRewardsText = buildRewardsText(rewards);
		String visibleRewardsText = resolveTypewriterText(questKey, "rewards", rawRewardsText);

		int iconSize = 16;
		int startY = y + 18;
		int rowHeight = getRewardRowHeight();
		int maxRows = Math.max(1, (height - 22) / rowHeight);
		int rows = Math.min(rewards.size(), maxRows);

		List<String> wrapped = wrapText(visibleRewardsText, width - 36);
		int wrappedIdx = 0;

		for (int i = 0; i < rows; i++) {
			QuestReward reward = rewards.get(i);
			int iconX = x + 8;
			int rowY = startY + (i * rowHeight);
			int iconY = rowY + Math.max(0, (rowHeight - iconSize) / 2);

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
			int textY = rowY + Math.max(0, (rowHeight - getDetailLineHeight()) / 2);
			TextUtil.drawStringWithBorder(graphics, this.font, txt(line), x + 28, textY, 0xFFCCCCCC);
		}
	}

	private void renderDescriptionSection(GuiGraphics graphics, int x, int y, int width, int height, String questKey) {
		graphics.fill(x, y, x + width, y + height, 0x44111122);
		graphics.renderOutline(x, y, width, height, 0x88444466);

		TextUtil.drawStringWithBorder(graphics, this.font, tr("gui.dragonminez.quest_tree.description").copy().withStyle(ChatFormatting.BOLD), x + 6, y + 4, 0xFFFFD700);

		String fullDescription = tr(selectedQuest.getDescription()).getString();
		String visibleDescription = resolveTypewriterText(questKey, "desc", fullDescription);
		List<String> lines = wrapText(visibleDescription, width - 14);

		int lineHeight = this.font.lineHeight + 2;
		int viewHeight = height - 24;
		int totalContentHeight = lines.size() * lineHeight;

		descMaxScroll = Math.max(0, totalContentHeight - viewHeight);
		targetDescScroll = Mth.clamp(targetDescScroll, 0, descMaxScroll);
		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();
		currentDescScroll = Mth.lerp(tickDelta * 0.4f, currentDescScroll, targetDescScroll);

		graphics.enableScissor(toScreenCoord(x + 4), toScreenCoord(y + 18), toScreenCoord(x + width - 6), toScreenCoord(y + height - 2));
		TextUtil.renderScrollableText(graphics, this.font, lines, x + 6, y + 18, width - 12, viewHeight, currentDescScroll, descMaxScroll, 0xFFCCCCCC);
		graphics.disableScissor();
	}

	private void renderObjectivesSection(GuiGraphics graphics, int x, int y, int width, int height, Saga saga) {
		graphics.fill(x, y, x + width, y + height, 0x44111122);
		graphics.renderOutline(x, y, width, height, 0x88444466);

		String sectionTitle = selectedQuest.hasStartRequirements()
				? tr("gui.dragonminez.quests.objectives_requirements").getString()
				: tr("gui.dragonminez.quests.objectives").getString();
		TextUtil.drawStringWithBorder(graphics, this.font,
				txt(fitSingleLineEllipsis(sectionTitle, width - 14)).withStyle(ChatFormatting.BOLD),
				x + 6,
				y + 4,
				0xFFFFD700);

		List<String> lines = buildObjectiveRenderLines(saga, width - 30);
		if (lines.isEmpty()) {
			TextUtil.drawStringWithBorder(graphics, this.font, txt("-"), x + 8, y + 18, 0xFF999999);
			return;
		}

		int lineHeight = getDetailLineHeight();
		int viewHeight = height - 24;
		int totalContentHeight = lines.size() * lineHeight;

		objMaxScroll = Math.max(0, totalContentHeight - viewHeight);
		targetObjScroll = Mth.clamp(targetObjScroll, 0, objMaxScroll);

		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();
		currentObjScroll = Mth.lerp(tickDelta * 0.4f, currentObjScroll, targetObjScroll);

		int drawY = y + 18;
		graphics.enableScissor(toScreenCoord(x + 4), toScreenCoord(y + 18), toScreenCoord(x + width - 6), toScreenCoord(y + height - 2));
		graphics.pose().pushPose();
		graphics.pose().translate(0, -currentObjScroll, 0);

		for (int i = 0; i < lines.size(); i++) {
			float lineY = drawY + (i * lineHeight);
			if (lineY + lineHeight >= drawY + currentObjScroll && lineY <= drawY + viewHeight + currentObjScroll) {
				drawObjectiveLineWithSymbolColors(graphics, lines.get(i), x + 8, (int)lineY);
			}
		}

		graphics.pose().popPose();
		graphics.disableScissor();

		if (objMaxScroll > 0) {
			int contentY = y + 18;
			int contentH = Math.max(8, height - 24);
			int scrollBarX = x + width - 4;
			graphics.fill(scrollBarX, contentY, scrollBarX + 2, contentY + contentH, 0xFF333333);
			float scrollPercent = objMaxScroll == 0 ? 0.0f : currentObjScroll / objMaxScroll;
			int indicatorHeight = Math.max(10, (int) ((float) viewHeight / totalContentHeight * contentH));
			int indicatorY = contentY + (int) ((contentH - indicatorHeight) * scrollPercent);
			graphics.fill(scrollBarX, indicatorY, scrollBarX + 2, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}
	}

	private List<String> buildObjectiveRenderLines(Saga saga, int textWidth) {
		List<String> lines = new ArrayList<>();
		if (statsData == null || selectedQuest == null) return lines;

		if (selectedQuest.hasStartRequirements()) {
			lines.add(tr("gui.dragonminez.quests.requirements").getString() + ":");
			appendRequirementLines(lines, selectedQuest.getStartRequirements(), questProgressKey(saga, selectedQuest), 0, textWidth);
			if (!selectedQuest.getObjectives().isEmpty()) {
				lines.add(tr("gui.dragonminez.quests.objectives").getString() + ":");
			}
		}

		PlayerQuestData pqd = statsData.getPlayerQuestData();
		String questKey = questProgressKey(saga, selectedQuest);
		List<QuestObjective> objectives = selectedQuest.getObjectives();

		for (int i = 0; i < objectives.size(); i++) {
			QuestObjective objective = objectives.get(i);
			int progress = pqd.getObjectiveProgress(questKey, i);
			boolean completed = progress >= selectedQuest.getObjectiveRequired(pqd, questKey, i);
			String marker = completed ? "✓ " : "✕ ";
			String baseText = getObjectiveText(pqd, questKey, objective, i, progress);
			marker = completed ? "+ " : "x ";
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

	private String getObjectiveText(PlayerQuestData pqd, String questKey, QuestObjective objective, int objectiveIndex, int currentProgress) {
		String description = QuestTextFormatter.describeObjective(objective).getString();
		int required = selectedQuest != null ? selectedQuest.getObjectiveRequired(pqd, questKey, objectiveIndex) : objective.getRequired();
		if (objective.getType() == QuestObjective.ObjectiveType.KILL
				|| objective.getType() == QuestObjective.ObjectiveType.ITEM
				|| objective.getType() == QuestObjective.ObjectiveType.SKILL) {
			return description + " (" + currentProgress + "/" + required + ")";
		}
		return description;
	}

	private void appendRequirementLines(List<String> lines, QuestPrerequisites requirements, String questKey, int depth, int textWidth) {
		if (requirements == null || requirements.conditions() == null || requirements.conditions().isEmpty()) {
			return;
		}

		boolean showGroupLabel = depth > 0 || requirements.conditions().size() > 1;
		if (showGroupLabel) {
			String groupLabel = requirements.operator() == QuestPrerequisites.Operator.AND
					? tr("gui.dragonminez.quests.requirements_all").getString()
					: tr("gui.dragonminez.quests.requirements_any").getString();
			addWrappedRequirementLine(lines, indent(depth), groupLabel, textWidth);
			depth++;
		}

		for (QuestPrerequisites.Condition condition : requirements.conditions()) {
			if (condition == null) continue;
			if (condition.isNestedGroup()) {
				appendRequirementLines(lines, condition.getNested(), questKey, depth, textWidth);
				continue;
			}

			List<String> conditionLines = describeRequirementLines(condition, questKey);
			if (conditionLines.isEmpty()) continue;

			String bulletPrefix = indent(depth) + "- ";
			String continuationPrefix = indent(depth + 1);
			for (int i = 0; i < conditionLines.size(); i++) {
				addWrappedRequirementLine(lines, i == 0 ? bulletPrefix : continuationPrefix, conditionLines.get(i), textWidth);
			}
		}
	}

	private List<String> describeRequirementLines(QuestPrerequisites.Condition condition, String questKey) {
		List<String> lines = new ArrayList<>();
		if (condition == null || condition.getType() == null) return lines;

		Minecraft mc = Minecraft.getInstance();
		lines.add(QuestTextFormatter.describeRequirement(
				condition,
				new QuestTextFormatter.RequirementContext(statsData, mc.player, questKey)
		).getString());
		return lines;
	}

	private void addWrappedRequirementLine(List<String> lines, String prefix, String text, int textWidth) {
		int wrapWidth = Math.max(12, textWidth - this.font.width(prefix));
		List<String> wrapped = wrapText(text, wrapWidth);
		if (wrapped.isEmpty()) {
			lines.add(prefix.trim());
			return;
		}

		String continuationPrefix = indentFromPrefix(prefix);
		lines.add(prefix + wrapped.get(0));
		for (int i = 1; i < wrapped.size(); i++) {
			lines.add(continuationPrefix + wrapped.get(i));
		}
	}

	private String indent(int depth) {
		return "  ".repeat(Math.max(0, depth));
	}

	private String indentFromPrefix(String prefix) {
		if (prefix.endsWith("- ")) {
			return prefix.substring(0, prefix.length() - 2) + "  ";
		}
		return prefix;
	}

	private void renderRewardTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
		for (RewardHitbox hitbox : rewardHitboxes) {
			if (!hitbox.contains(mouseX, mouseY)) continue;
			if (hitbox.stack != null && !hitbox.stack.isEmpty()) {
				graphics.renderTooltip(this.font, hitbox.stack, mouseX, mouseY);
			} else {
				renderSimpleTooltip(graphics, List.of(hitbox.tooltip), mouseX, mouseY);
			}
			return;
		}
	}

	private boolean renderActionButtonTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		if (actionButton == null || actionButtonTooltip.isEmpty() || actionButton.active || !actionButton.visible) {
			return false;
		}
		if (!actionButton.isMouseOver(mouseX, mouseY)) {
			return false;
		}
		renderSimpleTooltip(graphics, actionButtonTooltip, mouseX, mouseY);
		return true;
	}



	private void renderSimpleTooltip(GuiGraphics graphics, List<Component> lines, int mouseX, int mouseY) {
		if (lines == null || lines.isEmpty()) return;
		TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), null, lines, null, 0xFFFFFF);
	}

	private List<Component> buildQuestBlockerTooltip(Quest quest, Saga saga, boolean includePartyNote) {
		List<Component> lines = new ArrayList<>();
		if (quest == null || statsData == null) {
			return lines;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return lines;
		}

		Component availabilityFailure = QuestAvailabilityChecker.describeAvailabilityFailure(quest, statsData);
		if (availabilityFailure != null) {
			lines.add(availabilityFailure);
			return lines;
		}

		if (saga == null && !quest.isSagaQuest()) {
			return lines;
		}

		String questKey = questProgressKey(saga, quest);
		Component startFailure = QuestAvailabilityChecker.describeStartRequirementFailure(quest, questKey, mc.player, statsData);
		if (startFailure != null) {
			lines.add(startFailure);
			if (includePartyNote && statsData.getPlayerQuestData().isInParty()) {
				lines.add(tr("gui.dragonminez.party.start_requirements_all"));
			}
		}

		return lines;
	}

	private void renderSidePanelBackground(GuiGraphics graphics, PanelRect panel, boolean flushLeft, boolean flushRight, boolean drawFrame) {
		if (drawFrame) {
			graphics.fill(panel.x, panel.y, panel.right(), panel.bottom(), 0xCC0F1020);
		}

		int drawX = panel.x - (flushLeft ? 3 : 0);
		int drawW = panel.width + (flushLeft ? 3 : 0) + (flushRight ? 3 : 0);

		float srcPixelsPerDstPixel = panel.width <= 0 ? 1.0f : (282 / (float) panel.width);
		int srcBleed = Math.max(0, Math.round(3 * srcPixelsPerDstPixel));
		int srcU = 1 - (flushLeft ? srcBleed : 0);
		int srcW = 282 + (flushLeft ? srcBleed : 0) + (flushRight ? srcBleed : 0);
		int srcV = 1;
		int srcH = 426;

		if (srcU < 0) {
			srcW += srcU;
			srcU = 0;
		}
		srcW = Math.max(1, Math.min(srcW, 512 - srcU));
		srcV = Math.max(0, Math.min(srcV, 511));
		srcH = Math.max(1, Math.min(srcH, 512 - srcV));

		graphics.blit(QUEST_MENU,
				drawX,
				panel.y,
				drawW,
				panel.height,
				srcU,
				srcV,
				srcW,
				srcH,
				512,
				512);

		if (drawFrame) {
			graphics.fill(panel.x, panel.y, panel.right(), panel.y + 1, 0xAA5A5F7A);
			graphics.fill(panel.x, panel.bottom() - 1, panel.right(), panel.bottom(), 0xAA5A5F7A);
			if (!flushLeft) {
				graphics.fill(panel.x, panel.y, panel.x + 1, panel.bottom(), 0xAA5A5F7A);
			}
			if (!flushRight) {
				graphics.fill(panel.right() - 1, panel.y, panel.right(), panel.bottom(), 0xAA5A5F7A);
			}
		}
	}

	private String resolveTypewriterText(String questKey, String section, String fullText) {
		if (fullText == null || fullText.isEmpty()) return "";

		String key = questKey + "#" + section;
		long now = System.currentTimeMillis();
		long lastReveal = sectionLastReveal.getOrDefault(key, 0L);
		boolean shouldAnimate = now - lastReveal >= 5L * 60L * 1000L;

		if (!shouldAnimate) {
			sectionAnimationStart.remove(key);
			return fullText;
		}

		long start = sectionAnimationStart.computeIfAbsent(key, ignored -> now);
		long elapsed = Math.max(0L, now - start);
		int visibleChars = (int) ((elapsed / 1000.0f) * 55);
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
		if (fromVis == NodeVisibility.HIDDEN || toVis == NodeVisibility.HIDDEN) return;

		int x1 = (int) (conn.getFrom().getPixelX() + zPanX) + (NODE_SIZE / 2);
		int y1 = (int) (conn.getFrom().getPixelY() + zPanY) + (NODE_SIZE / 2);
		int x2 = (int) (conn.getTo().getPixelX() + zPanX) + (NODE_SIZE / 2);
		int y2 = (int) (conn.getTo().getPixelY() + zPanY) + (NODE_SIZE / 2);

		int color = 0xFF444444;
		if (statsData != null && !availableSagas.isEmpty()) {
			Saga saga = availableSagas.get(currentSagaIndex);
			PlayerQuestData pqd = statsData.getPlayerQuestData();
			boolean fromCompleted = isQuestCompleted(pqd, saga, conn.getFrom().getQuest());
			boolean toCompleted = isQuestCompleted(pqd, saga, conn.getTo().getQuest());
			if (fromCompleted && toCompleted) {
				color = 0xFF00AA00;
			}
		}

		if (fromVis == NodeVisibility.BLURRED || toVis == NodeVisibility.BLURRED) {
			color = 0x55444444;
		}

		drawThickLine(graphics, x1, y1, x2, y2, 2, color);
	}

	private void drawThickLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int thickness, int color) {
		int dx = Math.abs(x2 - x1);
		int dy = Math.abs(y2 - y1);
		int steps = Math.max(1, Math.max(dx, dy));
		int half = Math.max(0, thickness / 2);

		for (int i = 0; i <= steps; i++) {
			float t = i / (float) steps;
			int x = Math.round(x1 + (x2 - x1) * t);
			int y = Math.round(y1 + (y2 - y1) * t);
			graphics.fill(x - half, y - half, x + half + 1, y + half + 1, color);
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
					bgColor = 0xFF00CC00;
					borderColor = 0xFF009900;
				}
				case ACTIVE -> {
					bgColor = 0xFF3399FF;
					borderColor = 0xFF2266CC;
				}
				case AVAILABLE -> {
					bgColor = 0xFFFFCC00;
					borderColor = 0xFFCC9900;
				}
				default -> {
					bgColor = 0xFF555555;
					borderColor = 0xFF333333;
				}
			}
		}

		boolean isSelected = selectedQuest != null && sameQuestIdentity(selectedQuest, node.getQuest());
		if (isSelected && !isBlurred) {
			graphics.fill(x - 2, y - 2, x + NODE_SIZE + 2, y + NODE_SIZE + 2, 0xAAFFFFFF);
		}

		if (isHovered && !isBlurred) {
			graphics.fill(x - 1, y - 1, x + NODE_SIZE + 1, y + NODE_SIZE + 1, 0x66FFFFFF);
		}

		graphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE, borderColor);
		graphics.fill(x + 1, y + 1, x + NODE_SIZE - 1, y + NODE_SIZE - 1, bgColor);

		String icon;
		int iconColor;
		if (isBlurred) {
			icon = "?";
			iconColor = 0x55999999;
		} else {
			iconColor = 0xFFFFFFFF;
			switch (status) {
				case COMPLETED, CLAIMABLE -> {
					icon = "✓";
					iconColor = 0xFF55FF55;
				}
				case ACTIVE -> {
					icon = "!";
					iconColor = 0xFFFFFF00;
				}
				case AVAILABLE -> icon = "?";
				default -> {
					icon = "✕";
					iconColor = 0xFFFF5555;
				}
			}
		}

		int iconX = x + (NODE_SIZE - this.font.width(icon)) / 2;
		int iconY = y + (NODE_SIZE - 8) / 2;
		if ("✓".equals(icon) || "✕".equals(icon) || "!".equals(icon) || "-".equals(icon)) {
			graphics.drawString(this.font, Component.literal(icon).withStyle(ChatFormatting.BOLD), iconX, iconY, iconColor, false);
		} else {
			graphics.drawString(this.font, icon, iconX, iconY, iconColor, false);
		}

		if (!isBlurred) {
			if (node.isSidequest()) {
				int badgeX = x - 4;
				int badgeY = y - 4;
				graphics.fill(badgeX, badgeY, badgeX + 8, badgeY + 8, 0xFF6644AA);
				graphics.drawString(this.font, "S", badgeX + 1, badgeY, 0xFFFFFFFF, false);
			} else {
				String questNum = String.valueOf(node.getQuest().getId());
				int numX = x + (NODE_SIZE - this.font.width(questNum)) / 2;
				TextUtil.drawStringWithBorder(graphics, this.font, txt(questNum), numX, y + NODE_SIZE + 2, 0xFFCCCCCC);
			}
		} else {
			String hiddenLabel = "???";
			int numX = x + (NODE_SIZE - this.font.width(hiddenLabel)) / 2;
			TextUtil.drawStringWithBorder(graphics, this.font, txt(hiddenLabel), numX, y + NODE_SIZE + 2, 0x55888888);
		}

		if (status == QuestNodeStatus.CLAIMABLE && !isBlurred) {
			float pulse = (float) (Math.sin(System.currentTimeMillis() / 350.0) * 0.5 + 0.5);
			float alpha = 0.3f + pulse * 0.7f;
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			graphics.setColor(1.0f, 1.0f, 1.0f, alpha);
			int iconDrawX = x + NODE_SIZE - 2;
			int iconDrawY = y - 15 + 4;
			graphics.blit(EXCLAMATION_MARK, iconDrawX, iconDrawY,
					6, 15,
					0, 0,
					97, 250,
					97, 250);
			graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
		}
	}

	private void renderNodeTooltip(GuiGraphics graphics, Quest quest, int mouseX, int mouseY) {
		NodeVisibility vis = getNodeVisibility(quest);
		if (vis == NodeVisibility.HIDDEN) return;

		Component title;
		List<Component> desc = new ArrayList<>();
		List<Component> extras = new ArrayList<>();
		int color;

		if (vis == NodeVisibility.BLURRED) {
			title = txt("Unknown Quest").withStyle(ChatFormatting.OBFUSCATED);
			desc.add(tr("gui.dragonminez.quest_tree.status.locked"));
			color = 0xFF888888;
		} else {
			title = txt(LocalizationUtil.localizedOrReadableText(quest.getTitle())).withStyle(ChatFormatting.BOLD);
			QuestNodeStatus status = getNodeStatus(quest);
			if (status == QuestNodeStatus.CLAIMABLE && quest.getClaimMode() == Quest.ClaimMode.NPC_ONLY) {
				desc.add(tr("gui.dragonminez.quests.claim_from_npc"));
				desc.add(tr("gui.dragonminez.quests.claim_from_npc.tooltip"));
			} else {
				desc.add(getStatusText(status));
			}
			extras.addAll(buildQuestBlockerTooltip(quest, availableSagas.isEmpty() ? null : availableSagas.get(currentSagaIndex), false));
			color = getStatusColor(status);
		}

		TextUtil.renderAdvancedTooltip(graphics, this.font, mouseX, mouseY, getUiWidth(), getUiHeight(), title, desc, extras, color);
	}

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

	private boolean hasOtherOnlinePlayers() {
		Minecraft mc = Minecraft.getInstance();
		return mc.getConnection() != null && mc.getConnection().getOnlinePlayers().size() > 1;
	}

	private boolean isLocalPartyLeader() {
		if (statsData == null || Minecraft.getInstance().player == null) return false;
		return statsData.getPlayerQuestData().isPartyLeader(Minecraft.getInstance().player.getUUID());
	}

	private boolean isInSharedPartyAsMember() {
		if (statsData == null) return false;
		PlayerQuestData questData = statsData.getPlayerQuestData();
		return questData.isInParty() && !isLocalPartyLeader();
	}

	private PlayerQuestData.PartyInviteData getVisiblePartyInvite() {
		if (statsData == null) return null;
		PlayerQuestData.PartyInviteData invite = statsData.getPlayerQuestData().getPendingPartyInviteData();
		if (invite == null || invite.isExpired()) {
			return null;
		}
		return invite;
	}

	private String resolveInviteName(PlayerQuestData.PartyInviteData invite) {
		if (invite == null) return "";
		if (invite.getInviterName() != null && !invite.getInviterName().isBlank()) {
			return invite.getInviterName();
		}
		return resolvePlayerName(invite.getInviterUUID());
	}

	private String resolvePlayerName(UUID playerId) {
		if (playerId == null) return "";
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && playerId.equals(mc.player.getUUID())) {
			return mc.player.getGameProfile().getName();
		}
		if (mc.getConnection() != null) {
			for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
				if (info.getProfile().getId().equals(playerId)) {
					return info.getProfile().getName();
				}
			}
		}
		return playerId.toString().substring(0, 8);
	}

	private String buildPartyMemberSummary() {
		if (statsData == null) return "";
		List<String> names = new ArrayList<>();
		for (UUID memberId : statsData.getPlayerQuestData().getPartyMemberIds()) {
			names.add(resolvePlayerName(memberId));
		}
		return tr("gui.dragonminez.party.members", String.join(", ", names)).getString();
	}

	private void syncPartyButtonPositions() {
		PanelRect footer = getPartyFooterRect();
		if (footer == null) return;
		int centerX = footer.x + (footer.width - 74) / 2;

		if (partySecondaryButton != null) {
			partySecondaryButton.setX(centerX);
			partySecondaryButton.setY(footer.bottom() - 20);
		}
		if (partyPrimaryButton != null) {
			partyPrimaryButton.setX(centerX);
			partyPrimaryButton.setY(partySecondaryButton != null
					? footer.bottom() - 46
					: footer.bottom() - 20);
		}
	}

	private void queuePartyRefresh() {
		pendingRefreshTicks = Math.max(pendingRefreshTicks, 5);
	}

	private void openInvitePopup() {
		if (getVisiblePartyInvite() != null) return;
		invitePopupOpen = true;
		confirmOverlayOpen = false;
		invitePopupScroll = 0;
		rebuildInviteEntries();
	}

	private void rebuildInviteEntries() {
		inviteEntries.clear();
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null || mc.player == null || statsData == null) {
			return;
		}

		List<UUID> currentPartyMembers = statsData.getPlayerQuestData().getPartyMemberIds();
		for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
			UUID playerId = info.getProfile().getId();
			if (playerId == null || playerId.equals(mc.player.getUUID()) || currentPartyMembers.contains(playerId)) {
				continue;
			}
			inviteEntries.add(new PartyInviteEntry(playerId, info.getProfile().getName()));
		}
		inviteEntries.sort(Comparator.comparing(PartyInviteEntry::playerName, String.CASE_INSENSITIVE_ORDER));
		invitePopupScroll = Math.max(0, Math.min(invitePopupScroll, Math.max(0, inviteEntries.size() - getInvitePopupVisibleRows())));
	}

	private void requestConfirm(PartyConfirmAction action, Component title, Component body) {
		confirmAction = action;
		confirmTitle = title;
		confirmBody = body;
		confirmOverlayOpen = true;
		invitePopupOpen = false;
	}

	private void closeTransientOverlay() {
		invitePopupOpen = false;
		confirmOverlayOpen = false;
		confirmAction = PartyConfirmAction.NONE;
		confirmTitle = Component.empty();
		confirmBody = Component.empty();
	}

	private void executeConfirmAction() {
		switch (confirmAction) {
			case ACCEPT_INVITE -> NetworkHandler.sendToServer(new AcceptPartyInviteC2S());
			case LEAVE_PARTY -> NetworkHandler.sendToServer(new LeavePartyC2S());
			case NONE -> {
				closeTransientOverlay();
				return;
			}
		}
		queuePartyRefresh();
		closeTransientOverlay();
	}

	private PanelRect getInvitePopupRect() {
		return new PanelRect((getUiWidth() - 188) / 2, (getUiHeight() - 148) / 2, 188, 148);
	}

	private PanelRect getConfirmRect() {
		return new PanelRect((getUiWidth() - 188) / 2, (getUiHeight() - 112) / 2, 188, 112);
	}

	private int getInvitePopupVisibleRows() {
		return 5;
	}

	private void renderInvitePopup(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.fill(0, 0, getUiWidth(), getUiHeight(), 0x99000000);
		PanelRect popup = getInvitePopupRect();
		renderSidePanelBackground(graphics, popup, false, false, true);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.party.invite_popup").copy().withStyle(ChatFormatting.BOLD),
				popup.x + popup.width / 2,
				popup.y + 8,
				0xFFFFD700);

		int listX = popup.x + 10;
		int listY = popup.y + 24;
		int listW = popup.width - 20;
		int visibleRows = getInvitePopupVisibleRows();

		if (inviteEntries.isEmpty()) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font,
					tr("gui.dragonminez.party.invite_none"),
					popup.x + popup.width / 2,
					popup.y + popup.height / 2 - 6,
					0xFFAAAAAA);
		} else {
			for (int i = 0; i < visibleRows && (i + invitePopupScroll) < inviteEntries.size(); i++) {
				PartyInviteEntry entry = inviteEntries.get(i + invitePopupScroll);
				int rowY = listY + (i * 18);
				boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= rowY && mouseY <= rowY + 16;
				graphics.fill(listX, rowY, listX + listW, rowY + 16, hovered ? 0x66FFFFFF : 0x33000000);
				graphics.renderOutline(listX, rowY, listW, 16, hovered ? 0xFFCCDDFF : 0x55444466);
				TextUtil.drawStringWithBorder(graphics, this.font,
						txt(fitSingleLineEllipsis(entry.playerName(), listW - 10)),
						listX + 5,
						rowY + 5,
						hovered ? 0xFFFFFFFF : 0xFFDCE6FF);
			}
		}

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.party.invite_hint"),
				popup.x + popup.width / 2,
				popup.bottom() - 12,
				0xFFAAAAAA);
	}

	private void renderConfirmOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.fill(0, 0, getUiWidth(), getUiHeight(), 0xAA000000);
		PanelRect popup = getConfirmRect();
		renderSidePanelBackground(graphics, popup, false, false, true);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				confirmTitle.copy().withStyle(ChatFormatting.BOLD),
				popup.x + popup.width / 2,
				popup.y + 10,
				0xFFFFD700);

		List<String> lines = wrapText(confirmBody.getString(), popup.width - 20);
		int bodyY = popup.y + 28;
		for (int i = 0; i < Math.min(3, lines.size()); i++) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, txt(lines.get(i)), popup.x + popup.width / 2, bodyY, 0xFFDCE6FF);
			bodyY += 10;
		}

		PanelRect yesRect = getConfirmYesRect();
		PanelRect noRect = getConfirmNoRect();
		renderModalButton(graphics, yesRect, tr("gui.dragonminez.party.confirm_yes"), yesRect.contains(mouseX, mouseY));
		renderModalButton(graphics, noRect, tr("gui.dragonminez.party.confirm_no"), noRect.contains(mouseX, mouseY));
	}

	private void renderModalButton(GuiGraphics graphics, PanelRect rect, Component label, boolean hovered) {
		int fill = hovered ? 0x66FFFFFF : 0x33000000;
		int outline = hovered ? 0xFFCCDDFF : 0x88444466;
		graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), fill);
		graphics.renderOutline(rect.x, rect.y, rect.width, rect.height, outline);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, label, rect.x + rect.width / 2, rect.y + 6, 0xFFFFFFFF);
	}

	private PanelRect getConfirmYesRect() {
		PanelRect popup = getConfirmRect();
		return new PanelRect(popup.x + 18, popup.bottom() - 30, 66, 20);
	}

	private PanelRect getConfirmNoRect() {
		PanelRect popup = getConfirmRect();
		return new PanelRect(popup.right() - 84, popup.bottom() - 30, 66, 20);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256 && (invitePopupOpen || confirmOverlayOpen)) {
			closeTransientOverlay();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private boolean handleInvitePopupClick(double uiMouseX, double uiMouseY, int button) {
		if (!invitePopupOpen) return false;
		if (button != 0) return true;

		PanelRect popup = getInvitePopupRect();
		if (!popup.contains(uiMouseX, uiMouseY)) {
			invitePopupOpen = false;
			return true;
		}

		if (inviteEntries.isEmpty()) {
			return true;
		}

		int listX = popup.x + 10;
		int listY = popup.y + 24;
		int listW = popup.width - 20;
		int visibleRows = getInvitePopupVisibleRows();

		if (uiMouseX < listX || uiMouseX > listX + listW) {
			return true;
		}

		for (int i = 0; i < visibleRows && (i + invitePopupScroll) < inviteEntries.size(); i++) {
			int rowY = listY + (i * 18);
			if (uiMouseY >= rowY && uiMouseY <= rowY + 16) {
				PartyInviteEntry entry = inviteEntries.get(i + invitePopupScroll);
				NetworkHandler.sendToServer(new InvitePartyMemberC2S(entry.playerId()));
				invitePopupOpen = false;
				queuePartyRefresh();
				return true;
			}
		}

		return true;
	}

	private boolean handleConfirmOverlayClick(double uiMouseX, double uiMouseY, int button) {
		if (!confirmOverlayOpen) return false;
		if (button != 0) return true;

		if (getConfirmYesRect().contains(uiMouseX, uiMouseY)) {
			executeConfirmAction();
			return true;
		}

		if (getConfirmNoRect().contains(uiMouseX, uiMouseY) || !getConfirmRect().contains(uiMouseX, uiMouseY)) {
			closeTransientOverlay();
			return true;
		}

		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		if (handleConfirmOverlayClick(uiMouseX, uiMouseY, button)) {
			return true;
		}
		if (handleInvitePopupClick(uiMouseX, uiMouseY, button)) {
			return true;
		}

		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		if (button != 0) return false;

		if (handleNavigatorClick(uiMouseX, uiMouseY)) {
			return true;
		}

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
		treePressStarted = true;
		treePressMoved = false;
		treePressStartX = uiMouseX;
		treePressStartY = uiMouseY;
		return true;
	}

	private boolean handleNavigatorClick(double uiMouseX, double uiMouseY) {
		PanelRect panel = getLeftPanelRect();
		if (!panel.contains(uiMouseX, uiMouseY)) return false;

		int listX = panel.x + 10;
		int listY = panel.y + 28;
		int listW = panel.width - 20;
		int listH = Math.max(32, panel.height - 38 - getPartyFooterHeight());

		int index = (int) ((uiMouseY - listY + currentNavScroll) / 13);
		if (index < 0 || index >= navigatorEntries.size() || uiMouseY < listY || uiMouseY > listY + listH) return true;

		NavigatorEntry entry = navigatorEntries.get(index);
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MainSounds.PIP_MENU.get(), 1.0F));
		if (entry.type() == NavEntryType.SAGA) {
			if (entry.isPlaceholderSaga()) {
				return true;
			}
			if (!isSagaUnlockedByPreviousCompletion(entry.saga())) {
				return true;
			}
			int newIndex = availableSagas.indexOf(entry.saga());
			if (newIndex >= 0 && newIndex != currentSagaIndex) {
				currentSagaIndex = newIndex;
				selectedQuest = null;
				currentObjScroll = 0;
				rebuildLayout();
				rebuildNavigatorEntries();
				refreshButtons();
			}
			return true;
		}
		if (entry.type() == NavEntryType.SECRET_SECTION) {
			return true;
		}

		if (entry.quest() != null) {
			if ((entry.type() == NavEntryType.MAIN_QUEST || entry.type() == NavEntryType.SIDE_QUEST)
					&& hasReachableSideBranch(entry.saga(), entry.quest())) {
				toggleSideBranch(entry.saga(), entry.quest());
			}
			selectQuest(entry.quest(), true);
			QuestTreeLayoutHelper.NodePosition node = findNodeForQuest(entry.quest());
			if (node != null) slideToNode(node);
		}
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (invitePopupOpen || confirmOverlayOpen) {
			return true;
		}
		if (isDraggingTree && button == 0) {
			double uiMouseX = toUiX(mouseX);
			double uiMouseY = toUiY(mouseY);
			double dx = uiMouseX - treePressStartX;
			double dy = uiMouseY - treePressStartY;
			if ((dx * dx) + (dy * dy) > 16.0) {
				treePressMoved = true;
			}
			panX = dragStartPanX + (float) (uiMouseX - dragStartX);
			panY = dragStartPanY + (float) (uiMouseY - dragStartY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (invitePopupOpen || confirmOverlayOpen) {
			return true;
		}
		if (isDraggingTree && button == 0) {
			double uiMouseX = toUiX(mouseX);
			double uiMouseY = toUiY(mouseY);
			double dx = uiMouseX - treePressStartX;
			double dy = uiMouseY - treePressStartY;
			boolean moved = treePressMoved || ((dx * dx) + (dy * dy) > 16.0);

			if (treePressStarted && !moved && selectedQuest != null) {
				selectedQuest = null;
				currentObjScroll = 0;
				objMaxScroll = 0;
				refreshButtons();
			}

			isDraggingTree = false;
			treePressStarted = false;
			treePressMoved = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (confirmOverlayOpen) {
			return true;
		}
		if (invitePopupOpen) {
			int direction = (int) Math.signum(delta);
			int maxScroll = Math.max(0, inviteEntries.size() - getInvitePopupVisibleRows());
			invitePopupScroll = Math.max(0, Math.min(maxScroll, invitePopupScroll - direction));
			return true;
		}
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int scrollAmount = (int) Math.signum(delta);

		PanelRect left = getLeftPanelRect();
		if (left.contains(uiMouseX, uiMouseY)) {
			targetNavScroll = Mth.clamp(targetNavScroll - (scrollAmount * 26), 0, navMaxScroll);
			return true;
		}

		PanelRect objectivesRect = getObjectivesSectionRect();
		if (objectivesRect != null && objectivesRect.contains(uiMouseX, uiMouseY)) {
			targetObjScroll = Mth.clamp(targetObjScroll - (scrollAmount * getDetailLineHeight() * 2), 0, objMaxScroll);
			return true;
		}

		PanelRect descRect = getDescriptionSectionRect();
		if (descRect != null && descRect.contains(uiMouseX, uiMouseY)) {
			targetDescScroll = Mth.clamp(targetDescScroll - (scrollAmount * (this.font.lineHeight + 2) * 2), 0, descMaxScroll);
			return true;
		}

		if (getRightPanelRect().contains(uiMouseX, uiMouseY)) {
			return true;
		}

		PanelRect tree = getTreePanelRect();
		if (!tree.contains(uiMouseX, uiMouseY)) {
			return super.mouseScrolled(mouseX, mouseY, delta);
		}

		float oldZoom = zoom;
		float zoomDelta = scrollAmount * 0.1f;
		zoom = Math.max(0.25f, Math.min(2.0f, zoom + zoomDelta));

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
		currentObjScroll = 0;
		objMaxScroll = 0;
		resetTypewriterForSelectedQuest();
		refreshButtons();
		if (playClickSound) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MainSounds.PIP_MENU.get(), 1.0F));
		}
	}

	private PanelRect getObjectivesSectionRect() {
		if (selectedQuest == null || statsData == null || availableSagas.isEmpty()) {
			return null;
		}

		PanelRect panel = getRightPanelRect();
		Saga saga = availableSagas.get(currentSagaIndex);
		String questKey = questProgressKey(saga, selectedQuest);
		int innerX = panel.x + 10;
		int innerY = panel.y + 10;
		int innerW = panel.width - 20;
		int innerH = panel.height - 40;
		DetailPanelLayout layout = computeDetailPanelLayout(innerW, innerH, questKey, saga);
		int rewardsY = innerY + layout.titleH();
		int descY = rewardsY + layout.rewardsH();
		int objectivesY = descY + layout.descH();
		return new PanelRect(innerX, objectivesY, innerW, layout.objectivesH());
	}

	private PanelRect getDescriptionSectionRect() {
		if (selectedQuest == null || statsData == null || availableSagas.isEmpty()) {
			return null;
		}

		PanelRect panel = getRightPanelRect();
		Saga saga = availableSagas.get(currentSagaIndex);
		String questKey = questProgressKey(saga, selectedQuest);
		int innerX = panel.x + 10;
		int innerY = panel.y + 10;
		int innerW = panel.width - 20;
		int innerH = panel.height - 40;
		DetailPanelLayout layout = computeDetailPanelLayout(innerW, innerH, questKey, saga);

		int rewardsY = innerY + layout.titleH();
		int descY = rewardsY + layout.rewardsH();

		return new PanelRect(innerX, descY, innerW, layout.descH());
	}

	private void resetTypewriterForSelectedQuest() {
		if (selectedQuest == null || availableSagas.isEmpty()) return;
		Saga saga = availableSagas.get(currentSagaIndex);
		String key = questProgressKey(saga, selectedQuest);
		initializeTypewriterSection(key, "desc");
		initializeTypewriterSection(key, "rewards");
	}

	private void initializeTypewriterSection(String questKey, String section) {
		String key = questKey + "#" + section;
		long now = System.currentTimeMillis();
		long lastReveal = sectionLastReveal.getOrDefault(key, 0L);
		if (now - lastReveal >= 5L * 60L * 1000L) {
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

		String questKey = questProgressKey(saga, quest);
		PlayerQuestData.QuestStatus status = pqd.getQuestStatus(questKey);
		if (status == PlayerQuestData.QuestStatus.ACCEPTED || status == PlayerQuestData.QuestStatus.FAILED) {
			return NodeVisibility.VISIBLE;
		}

		if (quest.isSagaQuest()) {
			int questIndex = findSagaQuestIndex(saga, quest);
			if (questIndex >= 0 && QuestAvailabilityChecker.isSagaQuestAvailable(quest, saga, questIndex, statsData)) {
				return NodeVisibility.VISIBLE;
			}

			return NodeVisibility.BLURRED;
		}

		if (quest.isSideQuest()) {
			if (quest.isSecret()) {
				return status == PlayerQuestData.QuestStatus.ACCEPTED || status == PlayerQuestData.QuestStatus.FAILED
						? NodeVisibility.VISIBLE
						: NodeVisibility.HIDDEN;
			}
			return QuestAvailabilityChecker.isAvailable(quest, statsData)
					? NodeVisibility.VISIBLE
					: NodeVisibility.BLURRED;
		}

		return NodeVisibility.HIDDEN;
	}

	private boolean isImmediateLockedSagaQuest(Saga saga, Quest quest, PlayerQuestData pqd) {
		int questIndex = findSagaQuestIndex(saga, quest);
		if (questIndex < 0) return false;
		return findFirstLockedSagaQuestIndex(saga, pqd) == questIndex;
	}

	private int findFirstLockedSagaQuestIndex(Saga saga, PlayerQuestData pqd) {
		List<Quest> sagaQuests = saga.getQuests();
		for (int i = 0; i < sagaQuests.size(); i++) {
			Quest q = sagaQuests.get(i);
			if (isQuestCompleted(pqd, saga, q)) continue;

			String qKey = questProgressKey(saga, q);
			PlayerQuestData.QuestStatus status = pqd.getQuestStatus(qKey);
			if (status == PlayerQuestData.QuestStatus.ACCEPTED || status == PlayerQuestData.QuestStatus.FAILED) continue;
			if (QuestAvailabilityChecker.isSagaQuestAvailable(q, saga, i, statsData)) continue;

			return i;
		}
		return -1;
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

		String questKey = questProgressKey(saga, quest);
		PlayerQuestData.QuestStatus status = pqd.getQuestStatus(questKey);
		if (status == PlayerQuestData.QuestStatus.ACCEPTED) {
			return QuestNodeStatus.ACTIVE;
		}
		if (status == PlayerQuestData.QuestStatus.FAILED) {
			return QuestNodeStatus.AVAILABLE;
		}

		if (quest.isSagaQuest()) {
			int questIndex = findSagaQuestIndex(saga, quest);
			if (questIndex >= 0 && QuestAvailabilityChecker.isSagaQuestAvailable(quest, saga, questIndex, statsData)) {
				return QuestNodeStatus.AVAILABLE;
			}
			return QuestNodeStatus.LOCKED;
		}

		if (quest.isSideQuest()) {
			return QuestAvailabilityChecker.isAvailable(quest, statsData)
					? QuestNodeStatus.AVAILABLE
					: QuestNodeStatus.LOCKED;
		}

		return QuestNodeStatus.LOCKED;
	}

	private Component getStatusText(QuestNodeStatus status) {
		return switch (status) {
			case COMPLETED -> tr("gui.dragonminez.quests.status.complete").withStyle(ChatFormatting.DARK_GREEN);
			case CLAIMABLE -> tr("gui.dragonminez.quests.claim_rewards").withStyle(ChatFormatting.GOLD);
			case ACTIVE -> tr("gui.dragonminez.quest_tree.status.active").withStyle(ChatFormatting.AQUA);
			case AVAILABLE -> tr("gui.dragonminez.quest_tree.status.available").withStyle(ChatFormatting.GREEN);
			case LOCKED -> tr("gui.dragonminez.quest_tree.status.locked").withStyle(ChatFormatting.RED);
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

	private int findSagaQuestIndex(Saga saga, Quest quest) {
		if (saga == null || quest == null) return -1;
		List<Quest> sagaQuests = saga.getQuests();
		for (int i = 0; i < sagaQuests.size(); i++) {
			if (sameQuestIdentity(sagaQuests.get(i), quest)) {
				return i;
			}
		}
		return -1;
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

	private int getDetailLineHeight() {
		return Math.max(10, this.font.lineHeight + 1);
	}

	private int getRewardRowHeight() {
		return Math.max(18, getDetailLineHeight() + 6);
	}

	private void drawJustifiedTextBlock(GuiGraphics graphics, List<String> lines, int x, int y, int width, int maxLines, int lineHeight, int color) {
		int count = Math.min(maxLines, lines.size());
		for (int i = 0; i < count; i++) {
			boolean lastLine = i == count - 1;
			drawJustifiedLine(graphics, lines.get(i), x, y + (i * lineHeight), width, color, lastLine);
		}
	}

	private void drawJustifiedLine(GuiGraphics graphics, String line, int x, int y, int width, int color, boolean isLastLine) {
		String trimmed = line == null ? "" : line.trim();
		if (trimmed.isEmpty() || isLastLine) {
			TextUtil.drawStringWithBorder(graphics, this.font, txt(trimmed), x, y, color);
			return;
		}

		String[] words = trimmed.split(" ");
		if (words.length <= 1) {
			TextUtil.drawStringWithBorder(graphics, this.font, txt(trimmed), x, y, color);
			return;
		}

		int wordsWidth = 0;
		for (String word : words) wordsWidth += this.font.width(word);
		int spaces = words.length - 1;
		int baseSpace = this.font.width(" ");
		int totalBase = wordsWidth + (spaces * baseSpace);
		if (totalBase >= width || totalBase < (int) (width * 0.75f)) {
			TextUtil.drawStringWithBorder(graphics, this.font, txt(trimmed), x, y, color);
			return;
		}

		int extra = width - totalBase;
		int extraPerSpace = extra / spaces;
		int remainder = extra % spaces;

		int cursorX = x;
		for (int i = 0; i < words.length; i++) {
			TextUtil.drawStringWithBorder(graphics, this.font, txt(words[i]), cursorX, y, color);
			cursorX += this.font.width(words[i]);
			if (i < spaces) {
				cursorX += baseSpace + extraPerSpace + (i < remainder ? 1 : 0);
			}
		}
	}

	private PanelRect getLeftPanelRect() {
		PanelRect base = getBaseLeftPanelRect();
		int xOffset = getPanelIntroOffsetX(true, base.width) + getLeftPanelRevealOffset(base.width);
		return new PanelRect(base.x + xOffset, base.y, base.width, base.height);
	}

	private PanelRect getRightPanelRect() {
		PanelRect base = getBaseRightPanelRect();
		int xOffset = getPanelIntroOffsetX(false, base.width) + getRightPanelRevealOffset(base.width);
		return new PanelRect(base.x + xOffset, base.y, base.width, base.height);
	}

	private PanelRect getBaseLeftPanelRect() {
		int third = getUiWidth() / 3;
		int width = Math.max(140, (int) (third * 0.90f));
		int height = Math.min(getUiHeight(), Math.max(120, (int) (getUiHeight() * 0.90f)));
		int x = 0;
		int y = Math.max(0, (getUiHeight() - height) / 2);
		return new PanelRect(x, y, width, height);
	}

	private PanelRect getBaseRightPanelRect() {
		int third = getUiWidth() / 3;
		int width = Math.max(140, (int) (third * 0.90f));
		int height = Math.min(getUiHeight(), Math.max(120, (int) (getUiHeight() * 0.90f)));
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
		if (elapsed >= 700L) {
			panelIntroActive = false;
			return 1.0f;
		}
		return Math.max(0.0f, Math.min(1.0f, elapsed / 700.0f));
	}

	private int getPanelIntroOffsetX(boolean isLeftPanel, int panelWidth) {
		float t = getPanelIntroProgress();
		float eased = easeOutBack(t, 1.35f);
		float travel = (1.0f - eased) * (panelWidth + 22);
		int offset = Math.round(travel);
		return isLeftPanel ? -offset : offset;
	}

	private int getLeftPanelRevealOffset(int panelWidth) {
		int hiddenTravel = Math.max(0, panelWidth - 24);
		float eased = easeInOutCubic(leftPanelRevealProgress);
		return -Math.round((1.0f - eased) * hiddenTravel);
	}

	private int getRightPanelRevealOffset(int panelWidth) {
		float eased = easeInOutCubic(rightPanelRevealProgress);
		return Math.round((1.0f - eased) * panelWidth);
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
		actionButton.visible = selectedQuest != null && rightPanelRevealProgress > 0.15f;
	}

	private void updatePanelInteractionAnimations(int mouseX, int mouseY, float partialTick) {
		float step = Math.max(0.01f, 0.07f + (partialTick * 0.01f));

		boolean nearLeftEdge = mouseX <= 36;
		boolean overLeftPanel = getLeftPanelRect().contains(mouseX, mouseY);
		boolean keepLeftOpen = invitePopupOpen || confirmOverlayOpen;
		float leftTarget = (nearLeftEdge || overLeftPanel || keepLeftOpen) ? 1.0f : 0.0f;
		leftPanelRevealProgress = approach01(leftPanelRevealProgress, leftTarget, step);

		float rightTarget = selectedQuest != null ? 1.0f : 0.0f;
		rightPanelRevealProgress = approach01(rightPanelRevealProgress, rightTarget, step);
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

	private PanelRect getTreePanelRect() {
		return new PanelRect(0, 0, getUiWidth(), getUiHeight());
	}

	private void drawObjectiveLineWithSymbolColors(GuiGraphics graphics, String line, int x, int y) {
		int symbolIndex = -1;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (!Character.isWhitespace(c)) {
				symbolIndex = i;
				break;
			}
		}

		if (symbolIndex < 0) {
			TextUtil.drawStringWithBorder(graphics, this.font, txt(line), x, y, 0xFFCCCCCC);
			return;
		}

		char symbol = line.charAt(symbolIndex);
		int symbolColor = symbolColor(symbol);
		if (symbolColor == -1) {
			TextUtil.drawStringWithBorder(graphics, this.font, txt(line), x, y, 0xFFCCCCCC);
			return;
		}

		int symbolEnd = symbolIndex + 1;
		if (symbolEnd < line.length() && line.charAt(symbolEnd) == ' ') symbolEnd++;

		String prefix = line.substring(0, symbolIndex);
		String marker = line.substring(symbolIndex, symbolEnd);
		String rest = line.substring(symbolEnd);

		if (!prefix.isEmpty()) {
			TextUtil.drawStringWithBorder(graphics, this.font, txt(prefix), x, y, 0xFFCCCCCC);
		}

		int markerX = x + this.font.width(prefix);
		drawPlainBoldStringWithBorder(graphics, marker, markerX, y, symbolColor);

		if (!rest.isEmpty()) {
			TextUtil.drawStringWithBorder(graphics, this.font, txt(rest), markerX + this.font.width(marker), y, 0xFFCCCCCC);
		}
	}

	private int symbolColor(char symbol) {
		return switch (symbol) {
			case '+' -> 0xFF55FF55;
			case 'x', 'X' -> 0xFFFF5555;
			case '✓' -> 0xFF55FF55;
			case '✕' -> 0xFFFF5555;
			case '!', '-' -> 0xFFFFFF00;
			default -> -1;
		};
	}

	private void drawPlainBoldStringWithBorder(GuiGraphics graphics, String text, int x, int y, int textColor) {
		int borderColor = 0xFF000000;
		Component marker = Component.literal(text).withStyle(ChatFormatting.BOLD);
		graphics.drawString(this.font, marker, x + 1, y, borderColor, false);
		graphics.drawString(this.font, marker, x - 1, y, borderColor, false);
		graphics.drawString(this.font, marker, x, y + 1, borderColor, false);
		graphics.drawString(this.font, marker, x, y - 1, borderColor, false);
		graphics.drawString(this.font, marker, x, y, textColor, false);
	}
}
