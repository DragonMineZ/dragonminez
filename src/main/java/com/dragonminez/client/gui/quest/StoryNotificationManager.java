package com.dragonminez.client.gui.quest;

import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.util.LocalizationUtil;
import com.dragonminez.common.network.S2C.StoryToastS2C;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestTextFormatter;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class StoryNotificationManager {

	private StoryNotificationManager() {
	}

	public static void push(StoryToastS2C message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;

		Component title;
		Component description;
		StoryToast.Tone tone;

		switch (message.getEventType()) {
			case QUEST_STARTED -> {
				Quest quest = QuestRegistry.getClientQuest(message.getQuestId());
				title = Component.translatable("toast.dragonminez.story.quest_started.title");
				description = quest != null
						? toComponent(quest.getTitle())
						: Component.literal(message.getQuestId());
				tone = StoryToast.Tone.PROGRESS;
			}
			case QUEST_FAILED -> {
				Quest quest = QuestRegistry.getClientQuest(message.getQuestId());
				title = Component.translatable("toast.dragonminez.story.quest_failed.title");
				description = quest != null
						? Component.translatable("toast.dragonminez.story.quest_failed.desc", toComponent(quest.getTitle()))
						: Component.translatable("toast.dragonminez.story.quest_failed.desc", Component.literal(message.getQuestId()));
				tone = StoryToast.Tone.FAILURE;
			}
			case OBJECTIVE_COMPLETE -> {
				Quest quest = QuestRegistry.getClientQuest(message.getQuestId());
				title = Component.translatable("toast.dragonminez.story.objective_complete.title");
				description = resolveObjectiveText(mc, message, quest);
				tone = StoryToast.Tone.PROGRESS;
			}
			case QUEST_COMPLETE -> {
				Quest quest = QuestRegistry.getClientQuest(message.getQuestId());
				title = Component.translatable("toast.dragonminez.story.quest_complete.title");
				description = quest != null
						? Component.translatable("toast.dragonminez.story.quest_complete.desc", toComponent(quest.getTitle()))
						: Component.translatable("toast.dragonminez.story.quest_complete.desc", Component.literal(message.getQuestId()));
				tone = StoryToast.Tone.SUCCESS;
			}
			default -> {
				return;
			}
		}

		mc.getToasts().addToast(new StoryToast(title, description, tone));
	}

	private static Component resolveObjectiveText(Minecraft mc, StoryToastS2C message, Quest quest) {
		if (quest == null) return Component.literal(message.getQuestId());
		int idx = message.getObjectiveIndex();
		if (idx < 0 || idx >= quest.getObjectives().size()) {
			return toComponent(quest.getTitle());
		}

		QuestObjective objective = quest.getObjectives().get(idx);
		Component objectiveText = QuestTextFormatter.describeObjective(objective);
		int progress = message.getObjectiveProgress();
		int required = message.getObjectiveRequired();

		if (progress < 0 || required < 0) {
			final int[] fallbackProgress = {0};
			final int[] fallbackRequired = {objective.getRequired()};
			StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
				fallbackProgress[0] = data.getPlayerQuestData().getObjectiveProgress(message.getQuestId(), idx);
				fallbackRequired[0] = quest.getObjectiveRequired(data.getPlayerQuestData(), message.getQuestId(), idx);
			});
			progress = fallbackProgress[0];
			required = fallbackRequired[0];
		}

		return Component.translatable(
				"toast.dragonminez.story.objective_complete.desc",
				objectiveText,
				progress,
				required
		);
	}

	private static Component toComponent(String raw) {
		return LocalizationUtil.localizedOrReadable(raw);
	}
}


