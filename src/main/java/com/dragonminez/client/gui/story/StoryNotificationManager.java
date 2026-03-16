package com.dragonminez.client.gui.story;

import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.network.S2C.StoryToastS2C;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
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
			case INTRO_HINT -> {
				title = Component.translatable("toast.dragonminez.story.intro.title");
				description = Component.translatable("toast.dragonminez.story.intro.desc",
						KeyBinds.STATS_MENU.getTranslatedKeyMessage());
				tone = StoryToast.Tone.INFO;
			}
			case QUEST_STARTED -> {
				Quest quest = QuestRegistry.getClientQuest(message.getQuestId());
				title = Component.translatable("toast.dragonminez.story.quest_started.title");
				description = quest != null
						? toComponent(quest.getTitle())
						: Component.literal(message.getQuestId());
				tone = StoryToast.Tone.PROGRESS;
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

	public static void pushIntroHint() {
		push(StoryToastS2C.introHint());
	}

	private static Component resolveObjectiveText(Minecraft mc, StoryToastS2C message, Quest quest) {
		if (quest == null) return Component.literal(message.getQuestId());
		int idx = message.getObjectiveIndex();
		if (idx < 0 || idx >= quest.getObjectives().size()) {
			return toComponent(quest.getTitle());
		}

		QuestObjective objective = quest.getObjectives().get(idx);
		Component objectiveText = toComponent(objective.getDescription());

		final int[] progress = {0};
		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			progress[0] = data.getPlayerQuestData().getObjectiveProgress(message.getQuestId(), idx);
		});

		return Component.translatable(
				"toast.dragonminez.story.objective_complete.desc",
				objectiveText,
				progress[0],
				objective.getRequired()
		);
	}

	private static Component toComponent(String raw) {
		if (raw == null || raw.isBlank()) return Component.empty();
		return Language.getInstance().has(raw) ? Component.translatable(raw) : Component.literal(raw);
	}
}


