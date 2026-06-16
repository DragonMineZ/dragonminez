package com.dragonminez.client.gui.quest;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.MasterTextScreen;
import com.dragonminez.client.gui.MastersSkillsScreen;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.minigames.*;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.NPCActionC2S;
import com.dragonminez.common.network.C2S.QuestActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.QuestTextFormatter;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
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
	private static final Set<String> TEXT_MASTERS = Set.of("karin", "guru", "dende", "enma", "baba", "popo", "gero", "toribot", "babidi");
	private static final Set<String> SERVICE_MASTERS = Set.of("piccolo", "roshi", "kingkai", "oldkai", "babidi");

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

	private float dialogueScroll = 0, dialogueTargetScroll = 0, dialogueMaxScroll = 0;
	private float listScroll = 0, listTargetScroll = 0, listMaxScroll = 0;
	private float descScroll = 0, descTargetScroll = 0, descMaxScroll = 0;
	private float objScroll = 0, objTargetScroll = 0, objMaxScroll = 0;
	private float rewardScroll = 0, rewardTargetScroll = 0, rewardMaxScroll = 0;

	private boolean isTrainingMode = false;

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
		switch (minigameId) {
			case "rhythm" -> Minecraft.getInstance().setScreen(new RythmGameScreen());
			case "control" -> Minecraft.getInstance().setScreen(new ControlGameScreen());
			case "memory" -> Minecraft.getInstance().setScreen(new MemoryGameScreen());
			case "precision" -> Minecraft.getInstance().setScreen(new PrecisionGameScreen());
			case "gravity" -> Minecraft.getInstance().setScreen(new GravityGameScreen());
		}
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

		if (masterNpc) {
			String minigameId = getMinigameForNpc(npcId);
			boolean isSkillMaster = !TEXT_MASTERS.contains(npcId);

			if (isTrainingMode && isSkillMaster) {
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
		if (actionType == EntryType.OFFER) {
			// Hard mode is resolved server-side.
			NetworkHandler.sendToServer(new QuestActionC2S(QuestActionC2S.ActionType.START, questId, false, ""));
		} else if (actionType == EntryType.TURN_IN)
			NetworkHandler.sendToServer(new QuestActionC2S(QuestActionC2S.ActionType.TURN_IN, questId, false, npcId));

		if (Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.playSound(MainSounds.UI_MENU_SWITCH.get());
		}
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

		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();
		dialogueScroll = Mth.lerp(tickDelta * 0.4f, dialogueScroll, dialogueTargetScroll);
		listScroll = Mth.lerp(tickDelta * 0.4f, listScroll, listTargetScroll);
		descScroll = Mth.lerp(tickDelta * 0.4f, descScroll, descTargetScroll);
		objScroll = Mth.lerp(tickDelta * 0.4f, objScroll, objTargetScroll);
		rewardScroll = Mth.lerp(tickDelta * 0.4f, rewardScroll, rewardTargetScroll);

		beginUiScale(guiGraphics);

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		guiGraphics.blit(DIALOGUE_BG, panelX, panelY, 0, 0, panelW, panelH, 512, 512);

		Component npcName = npcName().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		TextUtil.drawCenteredStringWithBorder(guiGraphics, this.font, npcName, panelX + panelW / 2, panelY + 12, 0xFFFFFF);

		renderDialogueSection(guiGraphics);
		renderQuestListSection(guiGraphics, uiMouseX, uiMouseY);
		renderQuestDetails(guiGraphics);

		super.render(guiGraphics, uiMouseX, uiMouseY, partialTick);

		endUiScale(guiGraphics);
	}

	private void renderDialogueSection(GuiGraphics guiGraphics) {
		int diagX = panelX + 14;
		int diagY = panelY + 28;
		int diagW = panelW - 28;
		int diagH = 55;

		List<FormattedCharSequence> diagLines = this.font.split(dialogueLine(), diagW - 10);
		dialogueMaxScroll = Math.max(0, diagLines.size() * (this.font.lineHeight + 2) - diagH);
		dialogueTargetScroll = Mth.clamp(dialogueTargetScroll, 0, dialogueMaxScroll);

		renderScrollableFormatted(guiGraphics, diagLines, diagX, diagY, diagW, diagH, dialogueScroll, dialogueMaxScroll);
	}

	private void renderQuestListSection(GuiGraphics guiGraphics, int uiMouseX, int uiMouseY) {
		int listY = panelY + 120;
		int listX = panelX + 14;
		int listW = Math.min(150, panelW - 20);
		int viewHeight = MAX_VISIBLE * ENTRY_HEIGHT;

		TextUtil.drawStringWithBorder(guiGraphics, this.font, tr("gui.dragonminez.sidequest.available_quests").withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.BOLD), listX + 2, listY - 12, 0xFFFFFF);

		if (questEntries.isEmpty()) {
			TextUtil.drawStringWithBorder(guiGraphics, this.font, tr("gui.dragonminez.sidequest.no_quests").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.BOLD), listX + 4, listY + 4, 0xFF888888);
			return;
		}

		listMaxScroll = Math.max(0, questEntries.size() * ENTRY_HEIGHT - viewHeight);
		listTargetScroll = Mth.clamp(listTargetScroll, 0, listMaxScroll);

		guiGraphics.enableScissor(toScreenCoord(listX), toScreenCoord(listY), toScreenCoord(listX + listW + 6), toScreenCoord(listY + viewHeight));
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(0, -listScroll, 0);

		for (int i = 0; i < questEntries.size(); i++) {
			int entryY = listY + i * ENTRY_HEIGHT;

			if (entryY + ENTRY_HEIGHT >= listY + listScroll && entryY <= listY + viewHeight + listScroll) {
				QuestEntry entry = questEntries.get(i);
				boolean isSelected = (i == selectedIndex);
				boolean isHovered = uiMouseX >= listX && uiMouseX <= listX + listW && uiMouseY >= entryY - listScroll && uiMouseY < entryY + ENTRY_HEIGHT - listScroll;

				MutableComponent titleComp = tr(entry.quest.getTitle());
				if (isSelected) titleComp.withStyle(ChatFormatting.YELLOW);
				else if (isHovered) titleComp.withStyle(ChatFormatting.GRAY);
				else titleComp.withStyle(ChatFormatting.WHITE);

				Component questName = statusPrefix(entry.type).append(titleComp);
				TextUtil.drawStringWithBorder(guiGraphics, this.font, questName, listX + 4, entryY + 4, 0xFFFFFF);
			}
		}

		guiGraphics.pose().popPose();
		guiGraphics.disableScissor();

		if (listMaxScroll > 0) {
			int scrollBarX = listX + listW;
			guiGraphics.fill(scrollBarX, listY, scrollBarX + 2, listY + viewHeight, 0xFF333333);
			float scrollPercent = listScroll / listMaxScroll;
			int indicatorHeight = Math.max(10, (int) ((float) viewHeight / (questEntries.size() * ENTRY_HEIGHT) * viewHeight));
			int indicatorY = listY + (int) ((viewHeight - indicatorHeight) * scrollPercent);
			guiGraphics.fill(scrollBarX, indicatorY, scrollBarX + 2, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}
	}

	private void renderQuestDetails(GuiGraphics guiGraphics) {
		if (selectedIndex < 0 || selectedIndex >= questEntries.size()) return;

		int listX = panelX + 14;
		int listW = Math.min(150, panelW - 20);
		int detailX = listX + listW + 24;
		int detailW = panelX + panelW - detailX - 14;
		int detailY = panelY + 120;

		QuestEntry selected = questEntries.get(selectedIndex);

		List<FormattedCharSequence> titleLines = this.font.split(tr(selected.quest.getTitle()).withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD), detailW);
		int titleY = detailY;
		for (FormattedCharSequence seq : titleLines) {
			TextUtil.drawCenteredStringWithBorder(guiGraphics, this.font, seq, detailX + detailW / 2 - 2, titleY, 0xFFFFFF);
			titleY += this.font.lineHeight + 2;
		}

		int descY = detailY + 26;
		List<FormattedCharSequence> descLines = this.font.split(tr(selected.quest.getDescription()).withStyle(ChatFormatting.GRAY), detailW - 8);
		descMaxScroll = Math.max(0, descLines.size() * (this.font.lineHeight + 2) - 33);
		descTargetScroll = Mth.clamp(descTargetScroll, 0, descMaxScroll);
		renderScrollableFormatted(guiGraphics, descLines, detailX, descY, detailW, 33, descScroll, descMaxScroll);

		int objY = detailY + 63;
		List<FormattedCharSequence> objLines = new ArrayList<>();
		for (QuestObjective objective : selected.quest.getObjectives()) {
			Component objText = txt("- ").withStyle(ChatFormatting.GRAY).append(QuestTextFormatter.describeObjective(objective).copy().withStyle(ChatFormatting.WHITE));
			objLines.addAll(this.font.split(objText, detailW - 8));
		}
		objMaxScroll = Math.max(0, objLines.size() * (this.font.lineHeight + 2) - 33);
		objTargetScroll = Mth.clamp(objTargetScroll, 0, objMaxScroll);
		renderScrollableFormatted(guiGraphics, objLines, detailX, objY, detailW, 33, objScroll, objMaxScroll);

		int rewTitleY = detailY + 100;
		TextUtil.drawStringWithBorder(guiGraphics, this.font, tr("gui.dragonminez.sidequest.rewards").withStyle(ChatFormatting.GOLD), detailX, rewTitleY, 0xFFFFFF);

		int rewY = rewTitleY + 11;
		List<FormattedCharSequence> rewLines = new ArrayList<>();
		for (QuestReward reward : selected.quest.getRewards()) {
			Component rewText = txt("  ").append(reward.getDescription()).withStyle(ChatFormatting.GREEN);
			rewLines.addAll(this.font.split(rewText, detailW - 8));
		}
		rewardMaxScroll = Math.max(0, rewLines.size() * (this.font.lineHeight + 2) - 33);
		rewardTargetScroll = Mth.clamp(rewardTargetScroll, 0, rewardMaxScroll);
		renderScrollableFormatted(guiGraphics, rewLines, detailX, rewY, detailW, 33, rewardScroll, rewardMaxScroll);
	}

	private void renderScrollableFormatted(GuiGraphics guiGraphics, List<FormattedCharSequence> lines, int x, int y, int width, int height, float currentScroll, float maxScroll) {
		int lineHeight = this.font.lineHeight + 2;
		int totalContentHeight = lines.size() * lineHeight;

		guiGraphics.enableScissor(toScreenCoord(x), toScreenCoord(y), toScreenCoord(x + width), toScreenCoord(y + height));
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(0, -currentScroll, 0);

		for (int i = 0; i < lines.size(); i++) {
			float lineY = y + (i * lineHeight);
			if (lineY + lineHeight >= y + currentScroll && lineY <= y + height + currentScroll) {
				TextUtil.drawStringWithBorder(guiGraphics, this.font, lines.get(i), x, (int)lineY, 0xFFFFFF);
			}
		}

		guiGraphics.pose().popPose();
		guiGraphics.disableScissor();

		if (maxScroll > 0) {
			int scrollBarX = x + width - 4;
			guiGraphics.fill(scrollBarX, y, scrollBarX + 2, y + height, 0xFF333333);
			float scrollPercent = maxScroll == 0 ? 0.0f : currentScroll / maxScroll;
			int indicatorHeight = Math.max(10, (int) ((float) height / totalContentHeight * height));
			int indicatorY = y + (int) ((height - indicatorHeight) * scrollPercent);
			guiGraphics.fill(scrollBarX, indicatorY, scrollBarX + 2, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}
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

		int listY = panelY + 120;
		int listX = panelX + 14;
		int listW = Math.min(150, panelW - 20);
		int viewHeight = MAX_VISIBLE * ENTRY_HEIGHT;

		if (uiMouseX >= listX && uiMouseX <= listX + listW && uiMouseY >= listY && uiMouseY <= listY + viewHeight) {
			int relativeY = (int) (uiMouseY - listY + listScroll);
			int index = relativeY / ENTRY_HEIGHT;
			if (index >= 0 && index < questEntries.size()) {
				selectedIndex = index;
				descTargetScroll = 0;
				objTargetScroll = 0;
				rewardTargetScroll = 0;
				initButtons();
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int scrollAmount = (int) Math.signum(delta);

		if (uiMouseX >= panelX + 14 && uiMouseX <= panelX + panelW - 6 && uiMouseY >= panelY + 28 && uiMouseY <= panelY + 83) {
			dialogueTargetScroll = Mth.clamp(dialogueTargetScroll - (scrollAmount * 13), 0, dialogueMaxScroll);
			return true;
		}

		int listY = panelY + 120;
		int listX = panelX + 14;
		int listW = Math.min(150, panelW - 20);
		if (uiMouseX >= listX && uiMouseX <= listX + listW && uiMouseY >= listY && uiMouseY <= listY + (MAX_VISIBLE * ENTRY_HEIGHT)) {
			listTargetScroll = Mth.clamp(listTargetScroll - (scrollAmount * ENTRY_HEIGHT), 0, listMaxScroll);
			return true;
		}

		int detailX = listX + listW + 10;
		int detailW = panelX + panelW - detailX - 14;
		int detailY = panelY + 120;

		if (uiMouseX >= detailX && uiMouseX <= detailX + detailW) {
			int descY = detailY + 26;
			if (uiMouseY >= descY && uiMouseY <= descY + 33) {
				descTargetScroll = Mth.clamp(descTargetScroll - (scrollAmount * 13), 0, descMaxScroll);
				return true;
			}
			int objY = detailY + 63;
			if (uiMouseY >= objY && uiMouseY <= objY + 33) {
				objTargetScroll = Mth.clamp(objTargetScroll - (scrollAmount * 13), 0, objMaxScroll);
				return true;
			}
			int rewY = detailY + 111;
			if (uiMouseY >= rewY && uiMouseY <= rewY + 33) {
				rewardTargetScroll = Mth.clamp(rewardTargetScroll - (scrollAmount * 13), 0, rewardMaxScroll);
				return true;
			}
		}

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

	private void openServicesScreen() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		mc.setScreen(new MasterTextScreen(npcId));
	}

	private MutableComponent npcName() {
		String questNpcKey = "entity.dragonminez.questnpc." + npcId;
		if (I18n.exists(questNpcKey)) {
			return tr(questNpcKey);
		}

		String masterKey = "gui.dragonminez.lines." + npcId + ".name";
		if (I18n.exists(masterKey)) {
			return tr(masterKey);
		}
		return Component.literal(npcId).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	private MutableComponent dialogueLine() {
		String stage = getDialogueStage();
		String npcLine = "dialogue.dragonminez.story.sidequest." + npcId + "." + stage;
		if (I18n.exists(npcLine)) return tr(npcLine);
		return tr("dialogue.dragonminez.story.sidequest.generic_npc." + stage);
	}

	private String getDialogueStage() {
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