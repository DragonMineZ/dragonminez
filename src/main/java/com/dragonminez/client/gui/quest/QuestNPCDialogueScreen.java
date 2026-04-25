package com.dragonminez.client.gui.quest;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.MasterTextScreen;
import com.dragonminez.client.gui.MastersSkillsScreen;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
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
import net.minecraft.client.gui.screens.Screen;
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
public class QuestNPCDialogueScreen extends Screen {
	private static final ResourceLocation DIALOGUE_BG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menusmall.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	private static final Set<String> TEXT_MASTERS = Set.of("karin", "guru", "dende", "enma", "baba", "popo", "gero", "toribot");

	private static final int MAX_VISIBLE = 6;
	private static final int ENTRY_HEIGHT = 18;

	private final String npcId;
	private final List<String> offerableQuestIds;
	private final List<String> turnInQuestIds;
	private final List<String> inProgressQuestIds;
	private final boolean masterNpc;
	private final int entityId;
	private final List<QuestEntry> questEntries = new ArrayList<>();

	private int selectedIndex = -1;
	private int scrollOffset = 0;
	private int panelX, panelY, panelW, panelH;

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

		panelW = Math.min(390, this.width - 24);
		panelH = Math.min(245, this.height - 24);
		panelX = (this.width - panelW) / 2;
		panelY = (this.height - panelH) / 2;

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

	private void initButtons() {
		this.clearWidgets();

		this.addRenderableWidget(new TexturedTextButton.Builder()
				.position(panelX + panelW - 60, panelY + panelH - 24)
				.size(50, 16)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(50, 16)
				.message(tr("gui.dragonminez.close"))
				.onPress(btn -> this.onClose())
				.build());

		if (masterNpc) {
			this.addRenderableWidget(new TexturedTextButton.Builder()
					.position(panelX + 10, panelY + panelH - 24)
					.size(82, 16)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(82, 16)
					.message(TEXT_MASTERS.contains(npcId)
							? tr("gui.dragonminez.npc.services")
							: tr("gui.dragonminez.npc.train"))
					.onPress(btn -> openMasterScreen())
					.build());
		}

		if (selectedIndex >= 0 && selectedIndex < questEntries.size()) {
			QuestEntry entry = questEntries.get(selectedIndex);
			if (entry.type == EntryType.IN_PROGRESS) {
				return;
			}

			Component buttonText = entry.type == EntryType.OFFER
					? tr("gui.dragonminez.story.sidequests.accept")
					: tr("gui.dragonminez.sidequest.turn_in");
			EntryType actionType = entry.type;
			String questId = entry.questId;

			this.addRenderableWidget(new TexturedTextButton.Builder()
					.position(panelX + panelW - 145, panelY + panelH - 24)
					.size(76, 16)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(76, 16)
					.message(buttonText)
					.onPress(btn -> handleQuestAction(actionType, questId))
					.build());
		}
	}

	private void handleQuestAction(EntryType actionType, String questId) {
		if (actionType == EntryType.OFFER) {
			boolean isHard = ConfigManager.getUserConfig().getStoryHardDifficulty();
			NetworkHandler.sendToServer(new QuestActionC2S(QuestActionC2S.ActionType.START, questId, isHard, ""));
		} else if (actionType == EntryType.TURN_IN) {
			NetworkHandler.sendToServer(new QuestActionC2S(QuestActionC2S.ActionType.TURN_IN, questId, false, npcId));
		}

		if (Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.playSound(MainSounds.UI_MENU_SWITCH.get());
		}
		this.onClose();
	}

	@Override
	public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics);

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		guiGraphics.blit(DIALOGUE_BG, panelX, panelY, 0, 0, panelW, panelH, panelW, panelH);

		Component npcName = npcName().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		guiGraphics.drawCenteredString(this.font, npcName, panelX + panelW / 2, panelY + 8, 0xFFFFFF);

		Component dialogue = dialogueLine();
		int dialogueY = panelY + 22;
		for (FormattedCharSequence line : this.font.split(dialogue, panelW - 20)) {
			guiGraphics.drawString(this.font, line, panelX + 10, dialogueY, 0xDDDDDD);
			dialogueY += 10;
			if (dialogueY > panelY + 55) {
				break;
			}
		}

		int listY = panelY + 68;
		int listX = panelX + 10;
		int listW = Math.min(150, panelW - 20);
		int detailX = listX + listW + 12;
		int detailW = panelX + panelW - detailX - 10;

		guiGraphics.drawString(this.font, tr("gui.dragonminez.sidequest.available_quests").withStyle(ChatFormatting.YELLOW), listX, listY - 12, 0xFFFFFF);

		if (questEntries.isEmpty()) {
			guiGraphics.drawString(this.font, tr("gui.dragonminez.sidequest.no_quests").withStyle(ChatFormatting.GRAY), listX + 4, listY + 4, 0x888888);
		} else {
			for (int i = 0; i < MAX_VISIBLE && (i + scrollOffset) < questEntries.size(); i++) {
				int idx = i + scrollOffset;
				QuestEntry entry = questEntries.get(idx);
				int entryY = listY + i * ENTRY_HEIGHT;

				if (idx == selectedIndex) {
					guiGraphics.fill(listX, entryY, listX + listW, entryY + ENTRY_HEIGHT - 2, 0x44FFFFFF);
				}

				Component questName = statusPrefix(entry.type).append(tr(entry.quest.getTitle()).withStyle(ChatFormatting.WHITE));
				guiGraphics.drawString(this.font, questName, listX + 4, entryY + 4, 0xFFFFFF);
			}
		}

		renderQuestDetails(guiGraphics, listY, detailX, detailW);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private MutableComponent statusPrefix(EntryType type) {
		return switch (type) {
			case OFFER -> txt("[!] ").withStyle(ChatFormatting.GREEN);
			case TURN_IN -> txt("[?] ").withStyle(ChatFormatting.AQUA);
			case IN_PROGRESS -> txt("[...] ").withStyle(ChatFormatting.YELLOW);
		};
	}

	private void renderQuestDetails(GuiGraphics guiGraphics, int detailY, int detailX, int detailW) {
		if (selectedIndex < 0 || selectedIndex >= questEntries.size()) {
			return;
		}

		QuestEntry selected = questEntries.get(selectedIndex);
		guiGraphics.drawString(this.font, tr(selected.quest.getTitle()).withStyle(ChatFormatting.GOLD), detailX, detailY, 0xFFFFFF);
		detailY += 12;

		List<FormattedCharSequence> descLines = this.font.split(
				tr(selected.quest.getDescription()).withStyle(ChatFormatting.GRAY), detailW);
		for (int i = 0; i < Math.min(3, descLines.size()); i++) {
			guiGraphics.drawString(this.font, descLines.get(i), detailX, detailY, 0xBBBBBB);
			detailY += 10;
		}
		detailY += 4;

		for (int i = 0; i < Math.min(5, selected.quest.getObjectives().size()); i++) {
			QuestObjective objective = selected.quest.getObjectives().get(i);
			Component objectiveText = txt("- ").withStyle(ChatFormatting.GRAY)
					.append(QuestTextFormatter.describeObjective(objective).copy().withStyle(ChatFormatting.WHITE));
			guiGraphics.drawString(this.font, objectiveText, detailX, detailY, 0xCCCCCC);
			detailY += 10;
		}
		if (selected.quest.getObjectives().size() > 5) {
			guiGraphics.drawString(this.font, txt("...").withStyle(ChatFormatting.GRAY), detailX, detailY, 0x888888);
			detailY += 10;
		}

		if (!selected.quest.getRewards().isEmpty()) {
			detailY += 4;
			guiGraphics.drawString(this.font, tr("gui.dragonminez.sidequest.rewards").withStyle(ChatFormatting.GOLD), detailX, detailY, 0xFFFFFF);
			detailY += 10;
			for (int i = 0; i < Math.min(3, selected.quest.getRewards().size()); i++) {
				QuestReward reward = selected.quest.getRewards().get(i);
				guiGraphics.drawString(this.font, txt("  ").append(reward.getDescription()).withStyle(ChatFormatting.GREEN), detailX, detailY, 0xAAFFAA);
				detailY += 10;
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int listY = panelY + 68;
		int listX = panelX + 10;
		int listW = Math.min(150, panelW - 20);

		if (mouseX >= listX && mouseX <= listX + listW) {
			for (int i = 0; i < MAX_VISIBLE && (i + scrollOffset) < questEntries.size(); i++) {
				int entryY = listY + i * ENTRY_HEIGHT;
				if (mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
					selectedIndex = i + scrollOffset;
					initButtons();
					return true;
				}
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (delta > 0 && scrollOffset > 0) {
			scrollOffset--;
		} else if (delta < 0 && scrollOffset < questEntries.size() - MAX_VISIBLE) {
			scrollOffset++;
		}
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void openMasterScreen() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}

		if (TEXT_MASTERS.contains(npcId)) {
			mc.setScreen(new MasterTextScreen(npcId));
			return;
		}

		Entity entity = entityId >= 0 ? mc.level.getEntity(entityId) : null;
		LivingEntity livingEntity = entity instanceof LivingEntity living ? living : null;
		mc.setScreen(new MastersSkillsScreen(npcId, livingEntity));
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
		if (I18n.exists(npcLine)) {
			return tr(npcLine);
		}
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

	private record QuestEntry(String questId, Quest quest, EntryType type) {
	}

	public MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	public MutableComponent txt(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}
}
