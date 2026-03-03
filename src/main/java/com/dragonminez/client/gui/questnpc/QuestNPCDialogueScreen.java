package com.dragonminez.client.gui.questnpc;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.C2S.AcceptSideQuestC2S;
import com.dragonminez.common.network.C2S.TurnInSideQuestC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.sidequest.SideQuest;
import com.dragonminez.common.quest.sidequest.SideQuestManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialogue screen shown when a player right-clicks a QuestNPCEntity.
 * Shows NPC name, contextual dialogue, and quest offer/turn-in/in-progress options.
 */
@OnlyIn(Dist.CLIENT)
public class QuestNPCDialogueScreen extends Screen {

	private static final ResourceLocation DIALOGUE_BG = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/menusmall.png");
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");

	private final String npcId;
	private final List<String> offerableQuestIds;
	private final List<String> turnInQuestIds;
	private final List<String> inProgressQuestIds;

	/** All quest IDs combined for the list display */
	private final List<QuestEntry> questEntries = new ArrayList<>();

	private int selectedIndex = -1;
	private int scrollOffset = 0;
	private static final int MAX_VISIBLE = 6;
	private static final int ENTRY_HEIGHT = 18;

	private int panelX, panelY, panelW, panelH;

	public QuestNPCDialogueScreen(String npcId, List<String> offerableQuestIds,
								  List<String> turnInQuestIds, List<String> inProgressQuestIds) {
		super(Component.translatable("entity.dragonminez.questnpc." + npcId));
		this.npcId = npcId;
		this.offerableQuestIds = offerableQuestIds;
		this.turnInQuestIds = turnInQuestIds;
		this.inProgressQuestIds = inProgressQuestIds;
	}

	@Override
	protected void init() {
		super.init();
		questEntries.clear();

		for (String id : offerableQuestIds) {
			SideQuest sq = SideQuestManager.getClientSideQuest(id);
			if (sq != null) questEntries.add(new QuestEntry(id, sq, EntryType.OFFER));
		}
		for (String id : turnInQuestIds) {
			SideQuest sq = SideQuestManager.getClientSideQuest(id);
			if (sq != null) questEntries.add(new QuestEntry(id, sq, EntryType.TURN_IN));
		}
		for (String id : inProgressQuestIds) {
			SideQuest sq = SideQuestManager.getClientSideQuest(id);
			if (sq != null) questEntries.add(new QuestEntry(id, sq, EntryType.IN_PROGRESS));
		}

		panelW = 280;
		panelH = 220;
		panelX = (this.width - panelW) / 2;
		panelY = (this.height - panelH) / 2;

		if (!questEntries.isEmpty() && selectedIndex == -1) {
			selectedIndex = 0;
		}

		initButtons();
	}

	private void initButtons() {
		this.clearWidgets();

		// Close button
		this.addRenderableWidget(new TexturedTextButton.Builder()
				.position(panelX + panelW - 60, panelY + panelH - 25)
				.size(50, 16)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(50, 16)
				.message(Component.translatable("gui.dragonminez.close"))
				.onPress(btn -> this.onClose())
				.build());

		// Action button (Accept / Turn In) — only if a quest is selected
		if (selectedIndex >= 0 && selectedIndex < questEntries.size()) {
			QuestEntry entry = questEntries.get(selectedIndex);
			Component buttonText;
			boolean showAction = false;

			if (entry.type == EntryType.OFFER) {
				buttonText = Component.translatable("gui.dragonminez.story.sidequests.accept");
				showAction = true;
			} else if (entry.type == EntryType.TURN_IN) {
				buttonText = Component.translatable("gui.dragonminez.sidequest.turn_in");
				showAction = true;
			} else {
				buttonText = Component.translatable("gui.dragonminez.sidequest.in_progress");
			}

			if (showAction) {
				EntryType actionType = entry.type;
				String questId = entry.questId;

				this.addRenderableWidget(new TexturedTextButton.Builder()
						.position(panelX + panelW - 130, panelY + panelH - 25)
						.size(60, 16)
						.texture(BUTTONS_TEXTURE)
						.textureCoords(0, 28, 0, 48)
						.textureSize(60, 16)
						.message(buttonText)
						.onPress(btn -> {
							if (actionType == EntryType.OFFER) {
								boolean isHard = ConfigManager.getUserConfig().getHud().getStoryHardDifficulty();
								NetworkHandler.sendToServer(new AcceptSideQuestC2S(questId, isHard));
								if (Minecraft.getInstance().player != null) {
									Minecraft.getInstance().player.playSound(MainSounds.UI_MENU_SWITCH.get());
								}
								this.onClose();
							} else if (actionType == EntryType.TURN_IN) {
								NetworkHandler.sendToServer(new TurnInSideQuestC2S(questId, npcId));
								if (Minecraft.getInstance().player != null) {
									Minecraft.getInstance().player.playSound(MainSounds.UI_MENU_SWITCH.get());
								}
								this.onClose();
							}
						})
						.build());
			}
		}
	}

	@Override
	public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics);

		// Draw background panel
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		guiGraphics.blit(DIALOGUE_BG, panelX, panelY, 0, 0, panelW, panelH, panelW, panelH);

		// Draw NPC name at top
		Component npcName = Component.translatable("entity.dragonminez.questnpc." + npcId);
		guiGraphics.drawCenteredString(this.font, npcName.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				panelX + panelW / 2, panelY + 8, 0xFFFFFF);

		// Draw dialogue line
		String dialogueStage = getDialogueStage();
		Component dialogue = Component.translatable("dialogue.dragonminez.story.sidequest." + npcId + "." + dialogueStage);
		int dialogueY = panelY + 22;
		List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(dialogue, panelW - 20);
		for (net.minecraft.util.FormattedCharSequence line : lines) {
			guiGraphics.drawString(this.font, line, panelX + 10, dialogueY, 0xDDDDDD);
			dialogueY += 10;
		}

		// Draw quest list
		int listY = panelY + 65;
		int listX = panelX + 10;
		int listW = panelW - 20;

		guiGraphics.drawString(this.font,
				Component.translatable("gui.dragonminez.sidequest.available_quests").withStyle(ChatFormatting.YELLOW),
				listX, listY - 12, 0xFFFFFF);

		if (questEntries.isEmpty()) {
			guiGraphics.drawString(this.font,
					Component.translatable("gui.dragonminez.sidequest.no_quests").withStyle(ChatFormatting.GRAY),
					listX + 4, listY + 4, 0x888888);
		} else {
			for (int i = 0; i < MAX_VISIBLE && (i + scrollOffset) < questEntries.size(); i++) {
				int idx = i + scrollOffset;
				QuestEntry entry = questEntries.get(idx);
				int entryY = listY + i * ENTRY_HEIGHT;

				// Highlight selected
				if (idx == selectedIndex) {
					guiGraphics.fill(listX, entryY, listX + listW, entryY + ENTRY_HEIGHT - 2, 0x44FFFFFF);
				}

				// Status color prefix
				ChatFormatting statusColor = switch (entry.type) {
					case OFFER -> ChatFormatting.GREEN;
					case TURN_IN -> ChatFormatting.AQUA;
					case IN_PROGRESS -> ChatFormatting.YELLOW;
				};

				String statusPrefix = switch (entry.type) {
					case OFFER -> "[!] ";
					case TURN_IN -> "[?] ";
					case IN_PROGRESS -> "[...] ";
				};

				Component questName = Component.literal(statusPrefix)
						.withStyle(statusColor)
						.append(Component.translatable(entry.quest.getName()).withStyle(ChatFormatting.WHITE));

				guiGraphics.drawString(this.font, questName, listX + 4, entryY + 4, 0xFFFFFF);
			}
		}

		// Draw selected quest details on the right side / below
		if (selectedIndex >= 0 && selectedIndex < questEntries.size()) {
			QuestEntry selected = questEntries.get(selectedIndex);
			int detailY = listY + MAX_VISIBLE * ENTRY_HEIGHT + 8;

			guiGraphics.drawString(this.font,
					Component.translatable(selected.quest.getName()).withStyle(ChatFormatting.GOLD),
					listX, detailY, 0xFFFFFF);
			detailY += 12;

			guiGraphics.drawString(this.font,
					Component.translatable(selected.quest.getDescription()).withStyle(ChatFormatting.GRAY),
					listX, detailY, 0xBBBBBB);
			detailY += 12;

			// Show objectives
			for (QuestObjective obj : selected.quest.getObjectives()) {
				guiGraphics.drawString(this.font,
						Component.literal("• ").append(Component.translatable(obj.getDescription())).withStyle(ChatFormatting.WHITE),
						listX + 4, detailY, 0xCCCCCC);
				detailY += 10;
			}

			// Show rewards
			if (!selected.quest.getRewards().isEmpty()) {
				detailY += 4;
				guiGraphics.drawString(this.font,
						Component.translatable("gui.dragonminez.sidequest.rewards").withStyle(ChatFormatting.GOLD),
						listX, detailY, 0xFFFFFF);
				detailY += 10;
				for (QuestReward reward : selected.quest.getRewards()) {
					guiGraphics.drawString(this.font,
							Component.literal("  ").append(reward.getDescription()).withStyle(ChatFormatting.GREEN),
							listX + 4, detailY, 0xAAFFAA);
					detailY += 10;
				}
			}
		}

		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int listY = panelY + 65;
		int listX = panelX + 10;
		int listW = panelW - 20;

		if (mouseX >= listX && mouseX <= listX + listW) {
			for (int i = 0; i < MAX_VISIBLE && (i + scrollOffset) < questEntries.size(); i++) {
				int entryY = listY + i * ENTRY_HEIGHT;
				if (mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
					selectedIndex = i + scrollOffset;
					initButtons(); // Refresh action button
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

	private String getDialogueStage() {
		if (!turnInQuestIds.isEmpty()) return "complete";
		if (!offerableQuestIds.isEmpty()) return "offer";
		if (!inProgressQuestIds.isEmpty()) return "in_progress";
		return "idle";
	}

	private enum EntryType {
		OFFER, TURN_IN, IN_PROGRESS
	}

	private record QuestEntry(String questId, SideQuest quest, EntryType type) {}
}


