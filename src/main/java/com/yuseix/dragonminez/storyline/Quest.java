package com.yuseix.dragonminez.storyline;

import com.yuseix.dragonminez.registry.IDRegistry;
import com.yuseix.dragonminez.utils.DebugUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Quest {
	private final String id;
	private final String name;
	private final String description;
	private final List<Objective> objectives;
	private final List<Quest> prerequisites;
	private boolean completed;

	public Quest(String id, String name, String description, List<Objective> objectives, List<Quest> prerequisites) {
		IDRegistry.registerQuestId(id);
		this.id = id;
		this.name = name;
		this.description = description;
		this.objectives = objectives;
		this.prerequisites = prerequisites; // List of quest IDs that must be completed before this quest can be started
		this.completed = false;

		for (Objective objective : objectives) {
			objective.setOnCompletion(this::checkQuestCompletion);
		}

	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public List<Objective> getObjectives() {
		return objectives;
	}

	public List<String> getPrerequisites() {
		List<String> QuestIDs = new ArrayList<>();

		for (Quest prerequisite : prerequisites) {
			QuestIDs.add(prerequisite.getId());
		}
		return QuestIDs;
	}

	public void removeAllPrerequisites() {
		prerequisites.clear();
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;

		if (completed) {
			DebugUtils.dmzLog("Quest '" + name + "' is now completed!");
			// Trigger any additional logic for quest completion here
		}
	}

	public Map<String, Object> toSerializable() {
		Map<String, Object> data = new HashMap<>();
		data.put("id", id);
		data.put("completed", completed);
		return data;
	}

	public boolean canStart() {
		return prerequisites.isEmpty() || prerequisites.stream().allMatch(Quest::isCompleted);
	}

	private void checkQuestCompletion() {
		boolean allCompleted = objectives.stream().allMatch(Objective::isCompleted);
		if (allCompleted) {
			setCompleted(true);
		}
	}
}


