package com.dragonminez.common.quest;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Quest model used by both the saga system and the side-quest system.
 *
 * @since 2.0
 */
@Getter
public class Quest {

	public enum QuestType {
		SAGA,
		SIDEQUEST,
		DAILY,
		EVENT
	}

	private final int id;
	private final String stringId;
	private final QuestType type;
	private final String title;
	private final String description;
	private final List<QuestObjective> objectives;
	private final List<QuestReward> rewards;

	@Setter
	private boolean completed;

	@Setter
	private int currentObjectiveIndex;

	private final String category;
	private final boolean parallelObjectives;
	private final String questGiver;
	private final String turnIn;
	private final QuestPrerequisites prerequisites;
	private final QuestPrerequisites startRequirements;
	private final String branchGroup;
	private final String branchPath;
	private final String sagaId;
	private final int chainOrder;
	private final String nextQuestId;

	/**
	 * Universal constructor for the unified quest model.
	 */
	public Quest(int id, String stringId, QuestType type, String title, String description,
				 String category, boolean parallelObjectives,
				 List<QuestObjective> objectives, List<QuestReward> rewards,
				 QuestPrerequisites prerequisites, QuestPrerequisites startRequirements,
				 String questGiver, String turnIn,
				 String sagaId, int chainOrder, String nextQuestId,
				 String branchGroup, String branchPath) {
		this.id = id;
		this.stringId = stringId;
		this.type = type != null ? type : QuestType.SAGA;
		this.title = title;
		this.description = description;
		this.objectives = objectives != null ? objectives : new ArrayList<>();
		this.rewards = rewards != null ? rewards : new ArrayList<>();
		this.completed = false;
		this.currentObjectiveIndex = 0;
		this.category = category != null ? category : "general";
		this.parallelObjectives = parallelObjectives;
		this.questGiver = questGiver;
		this.turnIn = turnIn;
		this.prerequisites = prerequisites;
		this.startRequirements = startRequirements;
		this.sagaId = sagaId;
		this.chainOrder = chainOrder;
		this.nextQuestId = nextQuestId;
		this.branchGroup = branchGroup;
		this.branchPath = branchPath;
	}

	public boolean hasPrerequisites() {
		return prerequisites != null && !prerequisites.conditions().isEmpty();
	}

	public boolean hasStartRequirements() {
		return startRequirements != null && !startRequirements.conditions().isEmpty();
	}

	public boolean isSideQuest() {
		return type == QuestType.SIDEQUEST;
	}

	public boolean isSagaQuest() {
		return type == QuestType.SAGA;
	}

	public String getEffectiveId() {
		if (stringId != null) return stringId;
		if (sagaId != null) return sagaId + ":" + id;
		return String.valueOf(id);
	}

	public boolean isBranchingQuest() {
		return branchGroup != null && !branchGroup.isBlank() && branchPath != null && !branchPath.isBlank();
	}
}
