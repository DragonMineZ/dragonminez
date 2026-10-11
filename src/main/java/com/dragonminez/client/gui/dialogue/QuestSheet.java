package com.dragonminez.client.gui.dialogue;

import com.dragonminez.client.gui.character.QuestTreeScreen;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.util.ScrollbarState;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.QuestTextFormatter;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
final class QuestSheet {
	private static final int LINE_H = 11;
	private static final int ICON_SIZE = 16;
	private static final int ICON_GAP = 22;
	private static final int GLYPH_GAP = 14;
	private static final float TITLE_SCALE = 1.25f;
	private static final int LOCKED_TIER_COLOR = 0x6A6A70;

	private List<Line> lines = List.of();
	private String key = "";

	void invalidate() {
		key = "";
	}

	ItemStack render(GuiGraphics graphics, Font font, ScrollbarState bar, DialogueQuest quest, int x, int y, int width, int height,
					 int barX, double mouseX, double mouseY, float alpha) {
		DialogueQuest.Status status = quest.status(Util.getMillis());
		String nextKey = quest.id + "|" + width + "|" + status;
		if (!nextKey.equals(key)) {
			lines = build(font, quest, status, width - bar.reserve());
			key = nextKey;
		}

		int total = 0;
		for (Line line : lines) total += line.height();
		bar.layout(x, y, width, height, total).barAt(barX);
		bar.beginClip(graphics);
		ItemStack hovered = null;
		double contentY = bar.toContent(mouseY);
		int lineY = y;
		for (Line line : lines) {
			if (bar.isVisible(lineY, line.height())) {
				int lineX = x + line.indent();
				if (line.item() != null) {
					graphics.renderItem(line.item(), lineX, lineY);
					graphics.renderItemDecorations(font, line.item(), lineX, lineY);
					boolean over = bar.isInView(mouseX, mouseY) && mouseX >= lineX && mouseX < lineX + ICON_SIZE
							&& contentY >= lineY && contentY < lineY + ICON_SIZE;
					if (over) hovered = line.item();
					lineX += ICON_GAP;
				}
				if (line.glyph() != DialogueSkin.Icon.NONE) {
					DialogueSkin.icon(graphics, line.glyph(), lineX, lineY, line.glyphColor(), alpha);
					lineX += GLYPH_GAP;
				}
				if (line.text() != null) {
					float offset = line.item() != null ? 4.0f : 0.0f;
					HudRender.dmzText(graphics, line.text(), lineX, lineY + offset, line.scale(), 0.0f, line.color(), alpha);
				}
			}
			lineY += line.height();
		}
		bar.endClip(graphics);
		bar.renderBar(graphics, mouseX, mouseY);
		return hovered;
	}

	private static List<Line> build(Font font, DialogueQuest entry, DialogueQuest.Status status, int width) {
		List<Line> lines = new ArrayList<>();
		Quest quest = entry.quest;
		int statusColor = statusColor(status);
		lines.add(new Line(NpcDialogueContent.tr(statusKey(status)).getVisualOrderText(), statusColor, 1.0f, 0, 14,
				null, statusIcon(status), statusColor));

		for (FormattedCharSequence sequence : font.split(NpcDialogueContent.ph(NpcDialogueContent.tr(quest.getTitle())),
				Math.round(width / TITLE_SCALE))) {
			lines.add(text(sequence, DialogueSkin.TEXT, TITLE_SCALE, 13));
		}
		lines.add(gap(5));
		for (FormattedCharSequence sequence : font.split(NpcDialogueContent.ph(NpcDialogueContent.tr(quest.getDescription())), width)) {
			lines.add(text(sequence, DialogueSkin.TEXT_SOFT, 1.0f, LINE_H));
		}

		PlayerQuestData questData = questData();
		addObjectives(lines, font, entry, status, questData, width);
		addRewards(lines, font, entry, questData, width);
		return lines;
	}

	private static void addObjectives(List<Line> lines, Font font, DialogueQuest entry, DialogueQuest.Status status,
									  PlayerQuestData questData, int width) {
		List<QuestObjective> objectives = entry.quest.getObjectives();
		if (objectives.stream().allMatch(QuestObjective::isPlaceholder)) return;
		boolean tracked = status == DialogueQuest.Status.IN_PROGRESS || status == DialogueQuest.Status.TURN_IN;
		boolean completed = status == DialogueQuest.Status.COMPLETED;
		lines.add(gap(8));
		lines.add(text(NpcDialogueContent.tr("gui.dragonminez.quests.objectives").getVisualOrderText(), DialogueSkin.ACCENT, 1.0f, 13));
		for (int i = 0; i < objectives.size(); i++) {
			QuestObjective objective = objectives.get(i);
			if (objective.isPlaceholder()) continue;
			MutableComponent description = Component.empty().append(QuestTextFormatter.describeObjective(objective))
					.withStyle(Style.EMPTY.withFont(NpcDialogueScreen.FONT));
			boolean done = completed;
			if (tracked && questData != null && isCounted(objective)) {
				int required = entry.quest.getObjectiveRequired(questData, entry.id, i);
				int progress = Math.min(required, questData.getObjectiveProgress(entry.id, i));
				done = progress >= required;
				description.append(Component.literal("  " + progress + "/" + required)
						.withStyle(Style.EMPTY.withFont(NpcDialogueScreen.FONT).withColor(done ? DialogueSkin.OFFER : DialogueSkin.METAL)));
			}
			List<FormattedCharSequence> wrapped = font.split(description, width - GLYPH_GAP);
			for (int w = 0; w < wrapped.size(); w++) {
				boolean first = w == 0;
				DialogueSkin.Icon glyph = !first ? DialogueSkin.Icon.NONE : done ? DialogueSkin.Icon.CHECK : DialogueSkin.Icon.CHEVRON;
				lines.add(new Line(wrapped.get(w), DialogueSkin.TEXT, 1.0f, first ? 0 : GLYPH_GAP, LINE_H, null, glyph,
						done ? DialogueSkin.OFFER : DialogueSkin.ACCENT));
			}
		}
	}

	private static void addRewards(List<Line> lines, Font font, DialogueQuest entry, PlayerQuestData questData, int width) {
		List<QuestReward> rewards = entry.quest.getRewards();
		List<QuestTextFormatter.RewardGroup> groups = QuestTextFormatter.groupRewardsByDifficulty(rewards, false);
		if (groups.isEmpty()) return;
		lines.add(gap(8));
		lines.add(text(NpcDialogueContent.tr("gui.dragonminez.dialogue.quest.rewards").getVisualOrderText(), DialogueSkin.ACCENT, 1.0f, 14));

		Difficulty difficulty = questData == null ? Difficulty.NORMAL
				: questData.getQuestStatus(entry.id) != PlayerQuestData.QuestStatus.NOT_STARTED
				? questData.getQuestDifficulty(entry.id) : questData.getDifficulty();
		boolean tiered = QuestTextFormatter.hasRewardTiers(rewards);
		for (QuestTextFormatter.RewardGroup group : groups) {
			boolean tierLocked = !group.difficulties().contains(difficulty);
			if (tiered) {
				Component header = QuestTextFormatter.describeRewardDifficulties(group.difficulties()).copy()
						.withStyle(Style.EMPTY.withFont(NpcDialogueScreen.FONT));
				int headerColor = tierLocked ? LOCKED_TIER_COLOR : QuestTextFormatter.rewardDifficultyColor(group.difficulties(), false);
				lines.add(text(header.getVisualOrderText(), headerColor, 1.0f, 13));
			}
			for (QuestReward reward : group.rewards()) {
				double multiplier = questData != null ? questData.rewardMultiplierFor(reward, difficulty) : difficulty.questRewardMultiplier();
				Component description = Component.empty().append(reward.getDescription(multiplier))
						.withStyle(Style.EMPTY.withFont(NpcDialogueScreen.FONT));
				ItemStack icon = QuestTreeScreen.rewardIconStack(reward);
				int color = tierLocked ? LOCKED_TIER_COLOR : DialogueSkin.OFFER;
				List<FormattedCharSequence> wrapped = font.split(description, width - (icon != null ? ICON_GAP : 0));
				for (int w = 0; w < wrapped.size(); w++) {
					boolean first = w == 0;
					int height = first && icon != null ? (wrapped.size() == 1 ? ICON_SIZE + 2 : 14) : LINE_H;
					lines.add(new Line(wrapped.get(w), color, 1.0f, first || icon == null ? 0 : ICON_GAP, height,
							first ? icon : null, DialogueSkin.Icon.NONE, 0));
				}
				List<Component> details = reward.getTooltipLines(multiplier, statsData());
				for (int d = 1; d < details.size(); d++) {
					Component detail = details.get(d).copy().withStyle(Style.EMPTY.withFont(NpcDialogueScreen.FONT));
					for (FormattedCharSequence sequence : font.split(detail, width - ICON_GAP)) {
						lines.add(new Line(sequence, LOCKED_TIER_COLOR, 1.0f, ICON_GAP, LINE_H, null, DialogueSkin.Icon.NONE, 0));
					}
				}
			}
		}
	}

	private static boolean isCounted(QuestObjective objective) {
		return objective.getType() == QuestObjective.ObjectiveType.KILL
				|| objective.getType() == QuestObjective.ObjectiveType.ITEM
				|| objective.getType() == QuestObjective.ObjectiveType.SKILL;
	}

	private static StatsData statsData() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) return null;
		return StatsProvider.get(StatsCapability.INSTANCE, minecraft.player).orElse(null);
	}

	private static PlayerQuestData questData() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) return null;
		return StatsProvider.get(StatsCapability.INSTANCE, minecraft.player).map(StatsData::getPlayerQuestData).orElse(null);
	}

	private static String statusKey(DialogueQuest.Status status) {
		return switch (status) {
			case TURN_IN -> "gui.dragonminez.dialogue.quest.status.turn_in";
			case IN_PROGRESS -> "gui.dragonminez.dialogue.quest.status.in_progress";
			case COMPLETED -> "gui.dragonminez.dialogue.quest.status.completed";
			default -> "gui.dragonminez.dialogue.quest.status.offer";
		};
	}

	private static int statusColor(DialogueQuest.Status status) {
		return switch (status) {
			case TURN_IN -> DialogueSkin.TURN_IN;
			case IN_PROGRESS -> DialogueSkin.PROGRESS;
			case COMPLETED -> DialogueSkin.METAL;
			default -> DialogueSkin.OFFER;
		};
	}

	private static DialogueSkin.Icon statusIcon(DialogueQuest.Status status) {
		return switch (status) {
			case TURN_IN -> DialogueSkin.Icon.TURN_IN;
			case IN_PROGRESS -> DialogueSkin.Icon.PROGRESS;
			case COMPLETED -> DialogueSkin.Icon.CHECK;
			default -> DialogueSkin.Icon.OFFER;
		};
	}

	private static Line text(FormattedCharSequence sequence, int color, float scale, int height) {
		return new Line(sequence, color, scale, 0, height, null, DialogueSkin.Icon.NONE, 0);
	}

	private static Line gap(int height) {
		return new Line(null, 0, 1.0f, 0, height, null, DialogueSkin.Icon.NONE, 0);
	}

	private record Line(FormattedCharSequence text, int color, float scale, int indent, int height,
						ItemStack item, DialogueSkin.Icon glyph, int glyphColor) {
	}
}
