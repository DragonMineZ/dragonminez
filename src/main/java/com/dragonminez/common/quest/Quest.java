package com.dragonminez.common.quest;

import java.util.ArrayList;
import java.util.List;

public class Quest {
    private final int id;
    private final String title;
    private final String description;
    private final List<QuestObjective> objectives;
    private final List<QuestReward> rewards;
    private boolean completed;
    private int currentObjectiveIndex;

    public Quest(int id, String title, String description, List<QuestObjective> objectives, List<QuestReward> rewards) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.objectives = objectives != null ? objectives : new ArrayList<>();
        this.rewards = rewards != null ? rewards : new ArrayList<>();
        this.completed = false;
        this.currentObjectiveIndex = 0;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<QuestObjective> getObjectives() {
        return objectives;
    }

    public List<QuestReward> getRewards() {
        return rewards;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getCurrentObjectiveIndex() {
        return currentObjectiveIndex;
    }

    public void setCurrentObjectiveIndex(int index) {
        this.currentObjectiveIndex = index;
    }

    public QuestObjective getCurrentObjective() {
        if (currentObjectiveIndex >= 0 && currentObjectiveIndex < objectives.size()) {
            return objectives.get(currentObjectiveIndex);
        }
        return null;
    }

    public boolean advanceObjective() {
        if (currentObjectiveIndex < objectives.size() - 1) {
            currentObjectiveIndex++;
            return true;
        }
        return false;
    }

    public boolean isAllObjectivesCompleted() {
        return currentObjectiveIndex >= objectives.size() - 1 &&
               (getCurrentObjective() == null || getCurrentObjective().isCompleted());
    }
}

