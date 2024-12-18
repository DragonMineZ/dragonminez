package com.yuseix.dragonminez.storyline.sagas;

import com.yuseix.dragonminez.storyline.missions.Quest;

import java.util.ArrayList;
import java.util.List;

public class Saga {
	private final String id;
	private final String name;
	private final List<Quest> quests;

	public Saga(String id, String name) {
		this.id = id;
		this.name = name;
		this.quests = new ArrayList<>();
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<Quest> getQuests() {
		return quests;
	}

	public Quest getQuestbyId(String id) {
		return quests.stream().filter(quest -> quest.getId().equals(id)).findFirst().orElse(null);
	}

	public void addQuest(Quest quest) {
		quests.add(quest);
	}

	public boolean isCompleted() {
		return quests.stream().allMatch(Quest::isCompleted);
	}

}
