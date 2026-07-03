package com.dragonminez.common.quest;

import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class QuestObjective {
    private final ObjectiveType type;
    private final int required;
    private int progress;
    @Setter
    private boolean completed;

    private String customType = null;

    public QuestObjective(ObjectiveType type, int required) {
        this.type = type;
        this.required = required;
        this.progress = 0;
        this.completed = false;
    }

    /** Constructor for addon-registered objective types (see QuestObjectiveRegistry). */
    protected QuestObjective(String customType, int required) {
        this(ObjectiveType.CUSTOM, required);
        this.customType = customType;
    }

    /** Stable string key: the enum name for built-ins, the registered key for addon objectives. */
    public String getTypeKey() {
        return type == ObjectiveType.CUSTOM && customType != null ? customType : type.name();
    }

	public void setProgress(int progress) {
        this.progress = Math.min(progress, required);
        checkCompletion();
    }

    public void addProgress(int amount) {
        this.progress = Math.min(this.progress + amount, required);
        checkCompletion();
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
        DIMENSION,
        COORDS,
        TALK_TO,
        DRAGON_SUMMON,
        SKILL,
        DELIVER,
        SURVIVE_WAVES,
        ESCORT,
        CHECKPOINT_RACE,
        CUSTOM
    }
}

