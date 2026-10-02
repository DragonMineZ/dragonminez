package com.dragonminez.client.gui.quest;

import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.Reference;
import com.dragonminez.client.gui.MasterTextScreen;
import com.dragonminez.client.gui.MastersSkillsScreen;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.minigames.BaseMinigameScreen;
import com.dragonminez.common.training.MinigameOrigin;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.util.ScrollbarState;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.NPCActionC2S;
import com.dragonminez.common.network.C2S.QuestActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.QuestTextFormatter;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import com.dragonminez.common.util.DMZTextPlaceholders;
import com.dragonminez.client.systems.worldboss.ClientWorldBossState;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class QuestNPCDialogueScreen extends ScaledScreen {
	private static final ResourceLocation DIALOGUE_BG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menunpc.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");
	private static final Set<String> TEXT_MASTERS = Set.of("karin", "guru", "dende", "enma", "baba", "popo", "gero", "toribot", "babidi",
			"grandkai", "otherworld_announcer");
	private static final Set<String> SERVICE_MASTERS = Set.of("piccolo", "roshi", "kingkai", "oldkai", "babidi");
	private static final Set<String> TOURNAMENT_MASTERS =
			com.dragonminez.common.config.TournamentDefinition.HOST_NPCS.keySet();

	private static final int MAX_VISIBLE = 7;
	private static final int ENTRY_HEIGHT = 18;

	private final String npcId;
	private final List<String> offerableQuestIds;
	private final List<String> turnInQuestIds;
	private final List<String> inProgressQuestIds;
	private final boolean masterNpc;
	private final int entityId;
	private final List<QuestEntry> questEntries = new ArrayList<>();

	private int selectedIndex = -1;
	private int panelX, panelY, panelW, panelH;

	private final ScrollbarState dialogueBar = new ScrollbarState().barWidth(2).minThumb(10).step(13);
	private final ScrollbarState listBar = new ScrollbarState().barWidth(2).minThumb(10).step(ENTRY_HEIGHT);
	private final ScrollbarState descBar = new ScrollbarState().barWidth(2).minThumb(10).step(13);
	private final ScrollbarState objBar = new ScrollbarState().barWidth(2).minThumb(10).step(13);
	private final ScrollbarState rewardBar = new ScrollbarState().barWidth(2).minThumb(10).step(13);

	private boolean isTrainingMode = false;
	/** Replaces the NPC's stage line until the screen closes, e.g. after pressing a service button. */
	private static final String HELL_OGRE_ID = "hell_ogre";

	private MutableComponent overrideLine = null;

	public QuestNPCDialogueScreen(String npcId, List<String> offerableQuestIds,
	                              List<String> turnInQuestIds, List<String> inProgressQuestIds) {
		this(npcId, offerableQuestIds, turnInQuestIds, inProgressQuestIds, false, -1);
	}

	public QuestNPCDialogueScreen(String npcId, List<String> offerableQuestIds,
	                              List<String> turnInQuestIds, List<String> inProgressQuestIds,
	                              boolean masterNpc, int entityId) {
		super(Component.translatable("entity.dragonminez.questnpc." + npcId).withStyle(Style.EMPTY.withFont(DMZ_FONT)));
		this.npcId = npcId;
		this.offerableQuestIds = offerableQuestIds;
		this.turnInQuestIds = turnInQuestIds;
		this.inProgressQuestIds = inProgressQuestIds;
		this.masterNpc = masterNpc;
		this.entityId = entityId;
	}

	@Override
	protected void init() {
		super.init();
		questEntries.clear();

		addEntries(offerableQuestIds, EntryType.OFFER);
		addEntries(turnInQuestIds, EntryType.TURN_IN);
		addEntries(inProgressQuestIds, EntryType.IN_PROGRESS);

		panelW = 345;
		panelH = 273;
		panelX = (getUiWidth() - panelW) / 2;
		panelY = (getUiHeight() - panelH) / 2;

		if (!questEntries.isEmpty() && selectedIndex == -1) {
			selectedIndex = 0;
		}

		initButtons();
	}

	private void addEntries(List<String> questIds, EntryType type) {
		for (String id : questIds) {
			Quest quest = QuestRegistry.getClientQuest(id);
			if (quest != null) {
				questEntries.add(new QuestEntry(id, quest, type));
			}
		}
	}

	private String getMinigameForNpc(String targetNpc) {
		for (String gameId : new String[]{"rhythm", "control", "memory", "precision", "gravity"}) {
			if (ConfigManager.getTrainingConfig().getSettings(gameId).getMasterName().equalsIgnoreCase(targetNpc)) {
				return gameId;
			}
		}
		return null;
	}

	private void openMinigameScreen(String minigameId) {
		Minecraft.getInstance().setScreen(BaseMinigameScreen.create(minigameId, MinigameOrigin.MASTER));
	}

	private void initButtons() {
		this.clearWidgets();
		int btnY = getUiHeight() - 28;

		this.addRenderableWidget(new TexturedTextButton.Builder()
				.position(panelX + panelW - 82, btnY)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr(isTrainingMode ? "gui.dragonminez.customization.back" : "gui.dragonminez.close"))
				.onPress(btn -> {
					if (isTrainingMode) {
						isTrainingMode = false;
						initButtons();
					} else {
						this.onClose();
					}
				})
				.build());

		if (isHellOgre() && ClientWorldBossState.isLairKnown(WorldBossEntity.JANEMBA)) {
			this.addRenderableWidget(new TexturedTextButton.Builder()
					.position(panelX + 8, btnY)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(tr("gui.dragonminez.npc.show_location"))
					.onPress(btn -> {
						BlockPos lair = ClientWorldBossState.getLair(WorldBossEntity.JANEMBA);
						overrideLine = tr("dialogue.dragonminez.story.sidequest." + npcId + ".location",
								lair.getX(), lair.getZ());
						dialogueBar.reset();
					})
					.build());
		}

		if (masterNpc) {
			String minigameId = getMinigameForNpc(npcId);
			boolean popoTrains = isPopoInTimeChamber();
			boolean isSkillMaster = !TEXT_MASTERS.contains(npcId);

			if (isTrainingMode && (isSkillMaster || popoTrains)) {
				if (minigameId != null) {
					this.addRenderableWidget(new TexturedTextButton.Builder()
							.position(panelX + 8, btnY)
							.size(74, 20)
							.texture(BUTTONS_TEXTURE)
							.textureCoords(0, 28, 0, 48)
							.textureSize(74, 20)
							.message(tr("gui.dragonminez.button.popo.shadow"))
							.onPress(btn -> {
								NetworkHandler.sendToServer(new NPCActionC2S("popo", 1));
								this.onClose();
							})
							.build());

					this.addRenderableWidget(new TexturedTextButton.Builder()
							.position(getUiWidth() / 2 - 74, btnY)
							.size(74, 20)
							.texture(BUTTONS_TEXTURE)
							.textureCoords(0, 28, 0, 48)
							.textureSize(74, 20)
							.message(tr("gui.dragonminez.minigame." + minigameId))
							.onPress(btn -> openMinigameScreen(minigameId))
							.build());
				} else {
					this.addRenderableWidget(new TexturedTextButton.Builder()
							.position(panelX + 8, btnY)
							.size(74, 20)
							.texture(BUTTONS_TEXTURE)
							.textureCoords(0, 28, 0, 48)
							.textureSize(74, 20)
							.message(tr("gui.dragonminez.button.popo.shadow"))
							.onPress(btn -> {
								NetworkHandler.sendToServer(new NPCActionC2S("popo", 1));
								this.onClose();
							})
							.build());
				}
			} else if (!isTrainingMode) {
				if (isSkillMaster) {
					this.addRenderableWidget(new TexturedTextButton.Builder()
							.position(panelX + 8, btnY)
							.size(74, 20)
							.texture(BUTTONS_TEXTURE)
							.textureCoords(0, 28, 0, 48)
							.textureSize(74, 20)
							.message(tr("gui.dragonminez.npc.skills"))
							.onPress(btn -> openMasterScreen())
							.build());

					if (SERVICE_MASTERS.contains(npcId)) {
						this.addRenderableWidget(new TexturedTextButton.Builder()
								.position(getUiWidth() / 2 - 74, btnY)
								.size(74, 20)
								.texture(BUTTONS_TEXTURE)
								.textureCoords(0, 28, 0, 48)
								.textureSize(74, 20)
								.message(tr("gui.dragonminez.npc.services"))
								.onPress(btn -> openServicesScreen())
								.build());
					} else if (TOURNAMENT_MASTERS.contains(npcId)) {
						this.addRenderableWidget(new TexturedTextButton.Builder()
								.position(getUiWidth() / 2 - 74, btnY)
								.size(74, 20)
								.texture(BUTTONS_TEXTURE)
								.textureCoords(0, 28, 0, 48)
								.textureSize(74, 20)
								.message(tr("gui.dragonminez.npc.tournament"))
								.onPress(btn -> NetworkHandler.sendToServer(
										new com.dragonminez.common.network.TournamentPackets.ActionC2S(
										com.dragonminez.common.network.TournamentPackets.ActionC2S.Action.OPEN_BRACKET,
												entityId)))
								.build());
					} else {
						this.addRenderableWidget(new TexturedTextButton.Builder()
								.position(getUiWidth() / 2 - 74, btnY)
								.size(74, 20)
								.texture(BUTTONS_TEXTURE)
								.textureCoords(0, 28, 0, 48)
								.textureSize(74, 20)
								.message(tr("gui.dragonminez.npc.train"))
								.onPress(btn -> {
									isTrainingMode = true;
									initButtons();
								})
								.build());
					}
				} else if (popoTrains) {
					this.addRenderableWidget(new TexturedTextButton.Builder()
							.position(panelX + 8, btnY)
							.size(74, 20)
							.texture(BUTTONS_TEXTURE)
							.textureCoords(0, 28, 0, 48)
							.textureSize(74, 20)
							.message(tr("gui.dragonminez.npc.train"))
							.onPress(btn -> {
								isTrainingMode = true;
								initButtons();
							})
							.build());
				} else {
					this.addRenderableWidget(new TexturedTextButton.Builder()
							.position(panelX + 8, btnY)
							.size(74, 20)
							.texture(BUTTONS_TEXTURE)
							.textureCoords(0, 28, 0, 48)
							.textureSize(74, 20)
							.message(tr("gui.dragonminez.npc.services"))
							.onPress(btn -> openMasterScreen())
							.build());
					if (TOURNAMENT_MASTERS.contains(npcId)) {
						this.addRenderableWidget(new TexturedTextButton.Builder()
								.position(getUiWidth() / 2 - 74, btnY)
								.size(74, 20)
								.texture(BUTTONS_TEXTURE)
								.textureCoords(0, 28, 0, 48)
								.textureSize(74, 20)
								.message(tr("gui.dragonminez.npc.tournament"))
								.onPress(btn -> NetworkHandler.sendToServer(
										new com.dragonminez.common.network.TournamentPackets.ActionC2S(
												com.dragonminez.common.network.TournamentPackets.ActionC2S.Action.OPEN_BRACKET,
												entityId)))
								.build());
					}
				}
			}
		}

		if (!isTrainingMode && selectedIndex >= 0 && selectedIndex < questEntries.size()) {
			QuestEntry entry = questEntries.get(selectedIndex);
			if (entry.type != EntryType.IN_PROGRESS) {
				Component buttonText = entry.type == EntryType.OFFER
						? tr("gui.dragonminez.story.sidequests.accept")
						: tr("gui.dragonminez.sidequest.turn_in");
				EntryType actionType = entry.type;
				String questId = entry.questId;

				this.addRenderableWidget(new TexturedTextButton.Builder()
						.position(panelX + panelW - 78 - 74 - 13, btnY)
						.size(74, 20)
						.texture(BUTTONS_TEXTURE)
						.textureCoords(0, 28, 0, 48)
						.textureSize(74, 20)
						.message(buttonText)
						.onPress(btn -> handleQuestAction(actionType, questId))
						.build());
			}
		}
	}

	private void handleQuestAction(EntryType actionType, String questId) {
		if (actionType == EntryType.OFFER) NetworkHandler.sendToServer(new QuestActionC2S(QuestActionC2S.ActionType.START, questId, ""));
		else if (actionType == EntryType.TURN_IN) NetworkHandler.sendToServer(new QuestActionC2S(QuestActionC2S.ActionType.TURN_IN, questId, npcId));

		if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.playSound(MainSounds.UI_MENU_SWITCH.get());
		this.onClose();
	}

	@Override
	protected int getMinGuiWidth() {
		return 365;
	}

	@Override
	protected int getMinGuiHeight() {
		return 293;
	}

	@Override
	public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics);

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(guiGraphics);

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		HudRender.blit(guiGraphics, DIALOGUE_BG, panelX, panelY, 0, 0, panelW, panelH, 512, 512);

		Component npcName = npcName().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		TextUtil.drawCenteredStringWithBorder(guiGraphics, this.font, npcName, panelX + panelW / 2, panelY + 12, 0xFFFFFF);

		renderDialogueSection(guiGraphics, uiMouseX, uiMouseY);
		renderQuestListSection(guiGraphics, uiMouseX, uiMouseY);
		renderQuestDetails(guiGraphics, uiMouseX, uiMouseY);

		super.render(guiGraphics, uiMouseX, uiMouseY, partialTick);

		endUiScale(guiGraphics);
	}

	private void renderDialogueSection(GuiGraphics guiGraphics, int uiMouseX, int uiMouseY) {
		int diagX = panelX + 14;
		int diagY = panelY + 28;
		int diagW = panelW - 28;
		int diagH = 55;
		int lineHeight = this.font.lineHeight + 2;

		List<FormattedCharSequence> diagLines = TextUtil.wrapScrollable(this.font, dialogueLine(), diagW - 10, diagH, lineHeight, dialogueBar);
		TextUtil.renderScrollableText(guiGraphics, this.font, dialogueBar, diagLines, diagX, diagY, diagW - 2, diagH, lineHeight, 0xFFFFFF, false, uiMouseX, uiMouseY);
	}

	private void renderQuestListSection(GuiGraphics guiGraphics, int uiMouseX, int uiMouseY) {
		int listY = panelY + 120;
		int listX = panelX + 14;
		int listW = Math.min(150, panelW - 20);
		int viewHeight = MAX_VISIBLE * ENTRY_HEIGHT;

		TextUtil.drawStringWithBorder(guiGraphics, this.font, tr("gui.dragonminez.sidequest.available_quests").withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.BOLD), listX + 2, listY - 12, 0xFFFFFF);

		if (questEntries.isEmpty()) {
			listBar.clear();
			TextUtil.drawStringWithBorder(guiGraphics, this.font, tr("gui.dragonminez.sidequest.no_quests").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.BOLD), listX + 4, listY + 4, 0xFF888888);
			return;
		}

		listBar.layout(listX, listY, listW, viewHeight, questEntries.size() * ENTRY_HEIGHT).barAt(listX + listW);
		listBar.beginClip(guiGraphics);
		double contentMouseY = listBar.toContent(uiMouseY);

		for (int i = 0; i < questEntries.size(); i++) {
			int entryY = listY + i * ENTRY_HEIGHT;

			if (listBar.isVisible(entryY, ENTRY_HEIGHT)) {
				QuestEntry entry = questEntries.get(i);
				boolean isSelected = (i == selectedIndex);
				boolean isHovered = uiMouseX >= listX && uiMouseX <= listX + listW && contentMouseY >= entryY && contentMouseY < entryY + ENTRY_HEIGHT;

				MutableComponent titleComp = tr(entry.quest.getTitle());
				if (isSelected) titleComp.withStyle(ChatFormatting.YELLOW);
				else if (isHovered) titleComp.withStyle(ChatFormatting.GRAY);
				else titleComp.withStyle(ChatFormatting.WHITE);

				Component questName = statusPrefix(entry.type).append(titleComp);
				TextUtil.drawStringWithBorder(guiGraphics, this.font, questName, listX + 4, entryY + 4, 0xFFFFFF);
			}
		}

		listBar.endClip(guiGraphics);
		listBar.renderBar(guiGraphics, uiMouseX, uiMouseY);
	}

	private void renderQuestDetails(GuiGraphics guiGraphics, int uiMouseX, int uiMouseY) {
		if (selectedIndex < 0 || selectedIndex >= questEntries.size()) {
			descBar.clear();
			objBar.clear();
			rewardBar.clear();
			return;
		}

		int listX = panelX + 14;
		int listW = Math.min(150, panelW - 20);
		int detailX = listX + listW + 24;
		int detailW = panelX + panelW - detailX - 14;
		int detailY = panelY + 120;

		QuestEntry selected = questEntries.get(selectedIndex);

		List<FormattedCharSequence> titleLines = this.font.split(ph(tr(selected.quest.getTitle())).withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD), detailW);
		int titleY = detailY;
		for (FormattedCharSequence seq : titleLines) {
			TextUtil.drawCenteredStringWithBorder(guiGraphics, this.font, seq, detailX + detailW / 2 - 2, titleY, 0xFFFFFF);
			titleY += this.font.lineHeight + 2;
		}

		int lineHeight = this.font.lineHeight + 2;
		int descY = detailY + 26;
		List<FormattedCharSequence> descLines = TextUtil.wrapScrollable(this.font, ph(tr(selected.quest.getDescription())).withStyle(ChatFormatting.GRAY), detailW - 8, 33, lineHeight, descBar);
		TextUtil.renderScrollableText(guiGraphics, this.font, descBar, descLines, detailX, descY, detailW - 2, 33, lineHeight, 0xFFFFFF, false, uiMouseX, uiMouseY);

		int objY = detailY + 63;
		List<Component> objParts = new ArrayList<>();
		for (QuestObjective objective : selected.quest.getObjectives()) {
			Component objText = txt("- ").withStyle(ChatFormatting.GRAY).append(QuestTextFormatter.describeObjective(objective).copy().withStyle(ChatFormatting.WHITE));
			objParts.add(objText);
		}
		List<FormattedCharSequence> objLines = TextUtil.wrapScrollable(this.font, objParts, detailW - 8, 33, lineHeight, objBar);
		TextUtil.renderScrollableText(guiGraphics, this.font, objBar, objLines, detailX, objY, detailW - 2, 33, lineHeight, 0xFFFFFF, false, uiMouseX, uiMouseY);

		int rewTitleY = detailY + 100;
		TextUtil.drawStringWithBorder(guiGraphics, this.font, tr("gui.dragonminez.sidequest.rewards").withStyle(ChatFormatting.GOLD), detailX, rewTitleY, 0xFFFFFF);

		int rewY = rewTitleY + 11;
		List<Component> rewParts = new ArrayList<>();
		PlayerQuestData questData = StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player)
				.map(StatsData::getPlayerQuestData).orElse(null);
		Difficulty difficulty = questData == null ? Difficulty.NORMAL
				: questData.getQuestStatus(selected.questId()) != PlayerQuestData.QuestStatus.NOT_STARTED
				? questData.getQuestDifficulty(selected.questId()) : questData.getDifficulty();
		boolean tiered = QuestTextFormatter.hasRewardTiers(selected.quest.getRewards());
		for (QuestTextFormatter.RewardGroup group : QuestTextFormatter.groupRewardsByDifficulty(selected.quest.getRewards(), false)) {
			List<QuestReward> tierRewards = group.rewards();
			boolean tierLocked = !group.difficulties().contains(difficulty);
			if (tiered) {
				Component header = QuestTextFormatter.describeRewardDifficulties(group.difficulties()).copy()
						.withStyle(tierLocked ? ChatFormatting.DARK_GRAY : QuestTextFormatter.rewardDifficultyStyle(group.difficulties()));
				rewParts.add(header);
			}
			for (QuestReward reward : tierRewards) {
				double rewardMultiplier = questData != null
						? questData.rewardMultiplierFor(reward, difficulty)
						: difficulty.questRewardMultiplier();
				Component rewText = txt("  ").append(reward.getDescription(rewardMultiplier))
						.withStyle(tierLocked ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN);
				rewParts.add(rewText);
			}
		}
		List<FormattedCharSequence> rewLines = TextUtil.wrapScrollable(this.font, rewParts, detailW - 8, 33, lineHeight, rewardBar);
		TextUtil.renderScrollableText(guiGraphics, this.font, rewardBar, rewLines, detailX, rewY, detailW - 2, 33, lineHeight, 0xFFFFFF, false, uiMouseX, uiMouseY);
	}

	private MutableComponent statusPrefix(EntryType type) {
		return switch (type) {
			case OFFER -> txt("[!] ").withStyle(ChatFormatting.GREEN);
			case TURN_IN -> txt("[?] ").withStyle(ChatFormatting.AQUA);
			case IN_PROGRESS -> txt("[...] ").withStyle(ChatFormatting.YELLOW);
		};
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		if (ScrollbarState.clicked(uiMouseX, uiMouseY, button, dialogueBar, listBar, descBar, objBar, rewardBar)) return true;

		int listY = panelY + 120;
		int listX = panelX + 14;
		int listW = Math.min(150, panelW - 20);
		int viewHeight = MAX_VISIBLE * ENTRY_HEIGHT;

		if (uiMouseX >= listX && uiMouseX <= listX + listW && uiMouseY >= listY && uiMouseY <= listY + viewHeight) {
			int relativeY = (int) (uiMouseY - listY + listBar.scroll());
			int index = relativeY / ENTRY_HEIGHT;
			if (index >= 0 && index < questEntries.size()) {
				selectedIndex = index;
				descBar.scrollTo(0);
				objBar.scrollTo(0);
				rewardBar.scrollTo(0);
				initButtons();
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (ScrollbarState.dragged(toUiX(mouseX), toUiY(mouseY), dialogueBar, listBar, descBar, objBar, rewardBar)) return true;
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (ScrollbarState.released(dialogueBar, listBar, descBar, objBar, rewardBar)) return true;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		if (uiMouseX >= panelX + 14 && uiMouseX <= panelX + panelW - 6 && uiMouseY >= panelY + 28 && uiMouseY <= panelY + 83) {
			dialogueBar.scrollWheel(delta);
			return true;
		}

		if (ScrollbarState.scrolled(uiMouseX, uiMouseY, delta, listBar, descBar, objBar, rewardBar)) return true;

		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void openMasterScreen() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;

		if (TEXT_MASTERS.contains(npcId)) {
			mc.setScreen(new MasterTextScreen(npcId));
			return;
		}

		Entity entity = entityId >= 0 ? mc.level.getEntity(entityId) : null;
		LivingEntity livingEntity = entity instanceof LivingEntity living ? living : null;
		mc.setScreen(new MastersSkillsScreen(npcId, livingEntity));
	}

	private boolean isPopoInTimeChamber() {
		Minecraft mc = Minecraft.getInstance();
		return "popo".equals(npcId) && mc.player != null
				&& mc.player.level().dimension().equals(com.dragonminez.server.world.dimension.HTCDimension.HTC_KEY);
	}

	private void openServicesScreen() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		mc.setScreen(new MasterTextScreen(npcId));
	}

	private MutableComponent npcName() {
		String questNpcKey = "entity.dragonminez.questnpc." + npcId;
		String masterKey = "entity.dragonminez.master_" + npcId;
		String linesKey = "gui.dragonminez.lines." + npcId + ".name";

		if (masterNpc) {
			Minecraft mc = Minecraft.getInstance();
			Entity entity = entityId >= 0 && mc.level != null ? mc.level.getEntity(entityId) : null;
			if (entity != null && I18n.exists(entity.getType().getDescriptionId())) return tr(entity.getType().getDescriptionId());
			if (I18n.exists(masterKey)) return tr(masterKey);
		}

		if (I18n.exists(questNpcKey)) return tr(questNpcKey);
		if (I18n.exists(masterKey)) return tr(masterKey);
		if (I18n.exists(linesKey)) return tr(linesKey);
		return txt(readableId(npcId));
	}

	private static String readableId(String id) {
		StringBuilder builder = new StringBuilder();
		for (String word : id.split("[_ ]+")) {
			if (word.isEmpty()) continue;
			if (builder.length() > 0) builder.append(' ');
			builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return builder.toString();
	}

	private MutableComponent dialogueLine() {
		if (overrideLine != null) return ph(overrideLine.copy());
		String stage = getDialogueStage();
		if (isHellOgre() && "boss_respawning".equals(stage)) {
			return ph(tr("dialogue.dragonminez.story.sidequest." + npcId + ".boss_respawning",
					formatRespawn(ClientWorldBossState.getRespawnTicksRemaining(WorldBossEntity.JANEMBA))));
		}
		String npcLine = "dialogue.dragonminez.story.sidequest." + npcId + "." + stage;
		MutableComponent line = I18n.exists(npcLine) ? tr(npcLine)
				: tr("dialogue.dragonminez.story.sidequest.generic_npc." + stage);
		return ph(line);
	}

	private static String formatRespawn(long ticks) {
		long totalSeconds = Math.max(0L, ticks / 20L);
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		if (minutes >= 60L) return (minutes / 60L) + "h " + (minutes % 60L) + "m";
		if (minutes > 0L) return minutes + "m " + seconds + "s";
		return seconds + "s";
	}

	private MutableComponent ph(MutableComponent component) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return component;
		return DMZTextPlaceholders.apply(component, mc.player).copy();
	}

	private boolean isHellOgre() {
		return HELL_OGRE_ID.equals(npcId);
	}

	private String getDialogueStage() {
		if (isHellOgre()) {
			return ClientWorldBossState.getRespawnTicksRemaining(WorldBossEntity.JANEMBA) > 0L ? "boss_respawning" : "boss_alive";
		}
		if (!turnInQuestIds.isEmpty()) return "complete";
		if (!offerableQuestIds.isEmpty()) return "offer";
		if (!inProgressQuestIds.isEmpty()) return "in_progress";
		return "idle";
	}

	private enum EntryType {
		OFFER, TURN_IN, IN_PROGRESS
	}

	private record QuestEntry(String questId, Quest quest, EntryType type) { }
}