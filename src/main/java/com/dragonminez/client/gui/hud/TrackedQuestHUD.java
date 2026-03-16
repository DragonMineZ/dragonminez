package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class TrackedQuestHUD {

	private static final int PANEL_WIDTH = 180;
	private static final int MAX_TEXT_WIDTH = PANEL_WIDTH - 16;

	public static final IGuiOverlay HUD_TRACKED_QUEST = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			PlayerQuestData pqd = data.getPlayerQuestData();
			String trackedQuestId = pqd.getTrackedQuestId();
			if (trackedQuestId == null || trackedQuestId.isBlank()) return;
			if (!pqd.isQuestAccepted(trackedQuestId) || pqd.isQuestCompleted(trackedQuestId)) return;

			Quest quest = QuestRegistry.getClientQuest(trackedQuestId);
			if (quest == null) return;

			renderPanel(guiGraphics, mc.font, pqd, trackedQuestId, quest, width);
		});
	};

	private static void renderPanel(GuiGraphics guiGraphics, Font font, PlayerQuestData pqd, String questId, Quest quest, int screenWidth) {
		List<FormattedCharSequence> objectiveLines = buildObjectiveLines(font, pqd, questId, quest);
		List<FormattedCharSequence> wrappedTitle = font.split(toComponent(quest.getTitle()), MAX_TEXT_WIDTH);
		if (wrappedTitle.isEmpty()) wrappedTitle = List.of(FormattedCharSequence.forward(questId, net.minecraft.network.chat.Style.EMPTY));

		int lineHeight = 10;
		int lineCount = 1 + wrappedTitle.size() + objectiveLines.size();
		int panelHeight = 10 + (lineCount * lineHeight) + 6;
		int x = screenWidth - PANEL_WIDTH - 10;
		int y = 10;

		guiGraphics.fill(x, y, x + PANEL_WIDTH, y + panelHeight, 0xA02A2F40);
		guiGraphics.fill(x, y, x + PANEL_WIDTH, y + 1, 0xCC6D8CFF);
		guiGraphics.fill(x, y + panelHeight - 1, x + PANEL_WIDTH, y + panelHeight, 0x66000000);

		int drawY = y + 5;
		guiGraphics.drawString(font, Component.translatable("gui.dragonminez.story.hud.tracked"), x + 6, drawY, 0xE8F0FF, false);
		drawY += lineHeight;

		for (FormattedCharSequence titleLine : wrappedTitle) {
			guiGraphics.drawString(font, titleLine, x + 6, drawY, 0xFFFFFF, false);
			drawY += lineHeight;
		}

		for (FormattedCharSequence line : objectiveLines) {
			guiGraphics.drawString(font, line, x + 6, drawY, 0xCFE1FF, false);
			drawY += lineHeight;
		}
	}

	private static List<FormattedCharSequence> buildObjectiveLines(Font font, PlayerQuestData pqd, String questId, Quest quest) {
		List<FormattedCharSequence> lines = new ArrayList<>();
		List<QuestObjective> objectives = quest.getObjectives();
		if (objectives.isEmpty()) {
			lines.add(FormattedCharSequence.forward(Component.translatable("gui.dragonminez.story.hud.no_objectives").getString(), net.minecraft.network.chat.Style.EMPTY));
			return lines;
		}

		for (int i = 0; i < objectives.size(); i++) {
			QuestObjective objective = objectives.get(i);
			int progress = pqd.getObjectiveProgress(questId, i);
			if (!quest.isParallelObjectives()) {
				if (progress >= objective.getRequired()) continue;
				lines.addAll(splitObjectiveLine(font, objective, progress));
				return lines;
			}

			if (progress < objective.getRequired()) {
				lines.addAll(splitObjectiveLine(font, objective, progress));
			}
		}

		if (lines.isEmpty()) {
			lines.add(FormattedCharSequence.forward(Component.translatable("gui.dragonminez.quests.status.complete").getString(), net.minecraft.network.chat.Style.EMPTY));
		}

		return lines;
	}

	private static List<FormattedCharSequence> splitObjectiveLine(Font font, QuestObjective objective, int progress) {
		Component text = Component.literal("- ")
				.append(toComponent(objective.getDescription()))
				.append(Component.literal(" (" + progress + "/" + objective.getRequired() + ")"));
		return font.split(text, MAX_TEXT_WIDTH);
	}

	private static Component toComponent(String raw) {
		if (raw == null || raw.isBlank()) return Component.empty();
		return Language.getInstance().has(raw) ? Component.translatable(raw) : Component.literal(raw);
	}
}


