package com.dragonminez.common.quest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class Saga {
    private final String id;
    private final String name;
    private final List<Quest> quests;
    private final SagaRequirements requirements;
    private boolean unlocked;
    private int currentQuestIndex;

    public Saga(String id, String name, List<Quest> quests, SagaRequirements requirements) {
        this.id = id;
        this.name = name;
        this.quests = quests != null ? quests : new ArrayList<>();
        this.requirements = requirements;
        this.unlocked = false;
        this.currentQuestIndex = 0;
    }

    public Quest getCurrentQuest() {
        if (currentQuestIndex >= 0 && currentQuestIndex < quests.size()) {
            return quests.get(currentQuestIndex);
        }
        return null;
    }

    public boolean advanceQuest() {
        if (currentQuestIndex < quests.size() - 1) {
            currentQuestIndex++;
            return true;
        }
        return false;
    }

    public boolean isCompleted() {
        return currentQuestIndex >= quests.size() - 1 &&
               (getCurrentQuest() == null || getCurrentQuest().isCompleted());
    }

    public Quest getQuestById(int questId) {
        return quests.stream()
                .filter(q -> q.getId() == questId)
                .findFirst()
                .orElse(null);
    }

    @Setter
    @Getter
    @AllArgsConstructor
    public static class SagaRequirements {
        private final String previousSagaId;
    }
}

