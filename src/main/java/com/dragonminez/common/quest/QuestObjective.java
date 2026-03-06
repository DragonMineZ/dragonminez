package com.dragonminez.common.quest;

import lombok.Getter;
import lombok.Setter;

/**
 * Abstract base class for all quest objectives.
 * <p>
 * Each objective has a {@link ObjectiveType} discriminator, a description string,
 * a required progress count, and tracks current progress toward completion.
 * <p>
 * Subclasses implement {@link #checkProgress(Object...)} to validate progress
 * against specific game events (kills, item pickup, NPC interaction, etc.).
 *
 * @since 2.0
 * @see QuestParser#parseObjective
 */

@Getter
public abstract class QuestObjective {
    private final ObjectiveType type;
    private final String description;
    private int progress;
    private final int required;
    @Setter
	private boolean completed;

    public QuestObjective(ObjectiveType type, String description, int required) {
        this.type = type;
        this.description = description;
        this.required = required;
        this.progress = 0;
        this.completed = false;
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

    @SuppressWarnings("unused")
	public abstract boolean checkProgress(Object... params);

    /**
     * The types of objectives supported by the quest system.
     * Each type corresponds to a concrete subclass in the {@code objectives} package.
     */
    public enum ObjectiveType {
        /** Collect or possess a certain number of items. */
        ITEM,
        /** Kill a certain number of entities. */
        KILL,
        /** Interact with a specific entity. */
        INTERACT,
        /** Visit or discover a structure. */
        STRUCTURE,
        /** Enter a specific biome. */
        BIOME,
        /** Reach specific world coordinates. */
        COORDS,
        /** Talk to a specific quest NPC or master. */
        TALK_TO
    }
}
