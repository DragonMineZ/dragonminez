package com.dragonminez.common.quest.sidequest;

import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestReward;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SideQuest {
	private final String id;
	private final String name;
	private final String description;
	private final String category;
	private final boolean parallelObjectives;
	private final List<QuestObjective> objectives;
	private final List<QuestReward> rewards;
	private final SideQuestPrerequisites prerequisites;

	/** NPC ID that gives/offers this quest. Null = available from menu only. */
	private final String questGiver;

	/** NPC ID where the player must turn in this quest. Null = auto-complete when objectives are done. */
	private final String turnIn;

	public SideQuest(String id, String name, String description, String category, boolean parallelObjectives,
					 List<QuestObjective> objectives, List<QuestReward> rewards, SideQuestPrerequisites prerequisites,
					 String questGiver, String turnIn) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.category = category != null ? category : "general";
		this.parallelObjectives = parallelObjectives;
		this.objectives = objectives != null ? objectives : new ArrayList<>();
		this.rewards = rewards != null ? rewards : new ArrayList<>();
		this.prerequisites = prerequisites;
		this.questGiver = questGiver;
		this.turnIn = turnIn;
	}

	/** Returns whether this side-quest has any prerequisites at all. */
	public boolean hasPrerequisites() {
		return prerequisites != null && !prerequisites.getConditions().isEmpty();
	}
}

