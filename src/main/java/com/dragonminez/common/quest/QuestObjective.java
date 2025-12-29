package com.dragonminez.common.quest;

public abstract class QuestObjective {
    private final ObjectiveType type;
    private final String description;
    private int progress;
    private int required;
    private boolean completed;

    public QuestObjective(ObjectiveType type, String description, int required) {
        this.type = type;
        this.description = description;
        this.required = required;
        this.progress = 0;
        this.completed = false;
    }

    public ObjectiveType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.min(progress, required);
        checkCompletion();
    }

    public void addProgress(int amount) {
        this.progress = Math.min(this.progress + amount, required);
        checkCompletion();
    }

    public int getRequired() {
        return required;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    private void checkCompletion() {
        if (progress >= required) {
            completed = true;
        }
    }

    public abstract boolean checkProgress(Object... params);

    public enum ObjectiveType {
        ITEM,
        KILL,
        INTERACT,
        STRUCTURE,
        BIOME,
        COORDS
    }
}

