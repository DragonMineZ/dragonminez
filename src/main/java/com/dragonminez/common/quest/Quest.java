package com.dragonminez.common.quest;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Quest model used by both the saga system and the side-quest system.
 * <p>
 * <b>Saga quests</b> use the numeric {@link #id} field and are part of a {@link Saga} chain.
 * <b>Side-quests</b> use the string-based {@link #stringId} and are data-driven from JSON files.
 * <p>
 * The {@link QuestType} discriminator identifies which system owns a given quest instance,
 * and future types ({@code DAILY}, {@code EVENT}) can be added without changing existing code.
 *
 * @since 2.0
 * @see QuestParser
 * @see QuestRegistry
 * @see PlayerQuestData
 */
@Getter
public class Quest {

    // ========================================================================================
    // Quest Type Enum
    // ========================================================================================

    /**
     * Discriminator for different quest systems.
     */
    public enum QuestType {
        /** Linear saga quest — part of a {@link Saga} chain, keyed by int ID. */
        SAGA,
        /** Side-quest — standalone or chained, data-driven from JSON, keyed by string ID. */
        SIDEQUEST,
        /** Future: daily repeatable quests. */
        DAILY,
        /** Future: limited-time event quests. */
        EVENT
    }

    // ========================================================================================
    // Core Fields (shared by all quest types)
    // ========================================================================================

    /** Numeric ID used by saga quests. {@code -1} for non-saga quests. */
    private final int id;

    /** String-based unique ID (e.g. {@code "roshi_basic_training"}). {@code null} for legacy saga quests. */
    private final String stringId;

    /** The quest type discriminator. */
    private final QuestType type;

    /** Display name — translation key or literal text. */
    private final String title;

    /** Description text — translation key or literal text. */
    private final String description;

    /** The quest's objectives that must be completed. */
    private final List<QuestObjective> objectives;

    /** Rewards granted upon quest completion. */
    private final List<QuestReward> rewards;

    /** Whether this quest has been completed. Used by the saga progression system. */
    @Setter
    private boolean completed;

    /** Index of the current objective in sequential (non-parallel) quests. Used by the saga system. */
    @Setter
    private int currentObjectiveIndex;

    // ========================================================================================
    // Side-Quest / Extended Fields
    // ========================================================================================

    /** Category for grouping (e.g. "training", "exploration", "combat"). {@code null} for saga quests. */
    private final String category;

    /** If {@code true}, all objectives can be progressed simultaneously; if {@code false}, sequential. */
    private final boolean parallelObjectives;

    /** NPC ID that offers this quest. {@code null} = available from menu / auto-accepted. */
    private final String questGiver;

    /** NPC ID where the player must turn in. {@code null} = auto-complete when objectives done. */
    private final String turnIn;

    /** Prerequisites for accepting this quest. {@code null} = no prerequisites. */
    private final QuestPrerequisites prerequisites;

    /** Optional branch group key. Quests in the same saga+group become mutually exclusive by selected path. */
    private final String branchGroup;

    /** Optional branch path key inside {@link #branchGroup} (e.g. "good", "bad"). */
    private final String branchPath;

    // ========================================================================================
    // Chain Fields (saga chaining — works for both saga and side-quest types)
    // ========================================================================================

    /** The saga this quest belongs to, or {@code null} if standalone. */
    private final String sagaId;

    /** Position within the saga chain. {@code -1} if standalone. */
    private final int chainOrder;

    /** The string ID of the next quest in the chain, or {@code null} if this is the last. */
    private final String nextQuestId;

    // ========================================================================================
    // Constructors
    // ========================================================================================

    /**
     * Saga quest constructor — creates a quest with {@link QuestType#SAGA}.
     * Used by {@link QuestParser#parseSagaQuest} and the saga definition system.
     *
     * @param id          the numeric quest ID within the saga
     * @param title       the display name (translation key or literal)
     * @param description the quest description
     * @param objectives  list of objectives (may be empty)
     * @param rewards     list of rewards (may be empty)
     */
    public Quest(int id, String title, String description, List<QuestObjective> objectives, List<QuestReward> rewards) {
        this.id = id;
        this.stringId = null;
        this.type = QuestType.SAGA;
        this.title = title;
        this.description = description;
        this.objectives = objectives != null ? objectives : new ArrayList<>();
        this.rewards = rewards != null ? rewards : new ArrayList<>();
        this.completed = false;
        this.currentObjectiveIndex = 0;
        this.category = null;
        this.parallelObjectives = false;
        this.questGiver = null;
        this.turnIn = null;
        this.prerequisites = null;
        this.sagaId = null;
        this.chainOrder = -1;
        this.nextQuestId = null;
        this.branchGroup = null;
        this.branchPath = null;
    }

    /**
     * Side-quest constructor — creates a quest with {@link QuestType#SIDEQUEST}.
     * Used by {@link QuestParser#parseSideQuest} for data-driven quests.
     *
     * @param stringId           the unique string ID (e.g. "roshi_basic_training")
     * @param title              the display name (translation key or literal)
     * @param description        the quest description
     * @param category           grouping category (e.g. "training")
     * @param parallelObjectives whether objectives can be progressed simultaneously
     * @param objectives         list of objectives
     * @param rewards            list of rewards
     * @param prerequisites      prerequisites (may be null)
     * @param questGiver         NPC ID that gives the quest (may be null)
     * @param turnIn             NPC ID for turn-in (may be null)
     */
    public Quest(String stringId, String title, String description, String category,
                 boolean parallelObjectives, List<QuestObjective> objectives, List<QuestReward> rewards,
                 QuestPrerequisites prerequisites, String questGiver, String turnIn) {
        this(-1, stringId, QuestType.SIDEQUEST, title, description, category,
                parallelObjectives, objectives, rewards, prerequisites, questGiver, turnIn,
                null, -1, null, null, null);
    }

    /**
     * Universal constructor — allows full control over all fields including chain membership.
     * Intended for future quest types ({@code DAILY}, {@code EVENT}) or advanced usage.
     */
    public Quest(int id, String stringId, QuestType type, String title, String description,
                 String category, boolean parallelObjectives,
                 List<QuestObjective> objectives, List<QuestReward> rewards,
                 QuestPrerequisites prerequisites, String questGiver, String turnIn,
                 String sagaId, int chainOrder, String nextQuestId) {
        this(id, stringId, type, title, description, category, parallelObjectives,
                objectives, rewards, prerequisites, questGiver, turnIn,
                sagaId, chainOrder, nextQuestId, null, null);
    }

    /**
     * Universal constructor with optional branch metadata.
     */
    public Quest(int id, String stringId, QuestType type, String title, String description,
                 String category, boolean parallelObjectives,
                 List<QuestObjective> objectives, List<QuestReward> rewards,
                 QuestPrerequisites prerequisites, String questGiver, String turnIn,
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
        this.sagaId = sagaId;
        this.chainOrder = chainOrder;
        this.nextQuestId = nextQuestId;
        this.branchGroup = branchGroup;
        this.branchPath = branchPath;
    }

    /**
     * Constructor for standalone quests without chain fields (no saga chain).
     */
    public Quest(int id, String stringId, QuestType type, String title, String description,
                 String category, boolean parallelObjectives,
                 List<QuestObjective> objectives, List<QuestReward> rewards,
                 QuestPrerequisites prerequisites, String questGiver, String turnIn) {
        this(id, stringId, type, title, description, category, parallelObjectives,
                objectives, rewards, prerequisites, questGiver, turnIn, null, -1, null, null, null);
    }

    // ========================================================================================
    // Convenience Getters
    // ========================================================================================

    /**
     * Alias for {@link #getTitle()}. Kept for convenience.
     */
    public String getName() {
        return title;
    }

    /** Returns {@code true} if this quest has non-empty prerequisites. */
    public boolean hasPrerequisites() {
        return prerequisites != null && !prerequisites.getConditions().isEmpty();
    }

    /** Returns {@code true} if this is a {@link QuestType#SIDEQUEST}. */
    public boolean isSideQuest() {
        return type == QuestType.SIDEQUEST;
    }

    /** Returns {@code true} if this is a {@link QuestType#SAGA} quest. */
    public boolean isSagaQuest() {
        return type == QuestType.SAGA;
    }

    /**
     * Returns a unique string identifier for this quest, regardless of type.
     * <p>
     * For side-quests, returns {@link #stringId}. For saga quests with no stringId,
     * returns a composite key {@code "sagaId:numericId"}.
     *
     * @return the effective string ID (never null for properly constructed quests)
     */
    public String getEffectiveId() {
        if (stringId != null) return stringId;
        if (sagaId != null) return sagaId + ":" + id;
        return String.valueOf(id);
    }

    /** Returns {@code true} if this quest belongs to a saga chain. */
    public boolean isPartOfChain() {
        return sagaId != null;
    }

    /** Returns {@code true} if this quest is the last in its chain (no next quest). */
    public boolean isChainEnd() {
        return nextQuestId == null;
    }

    /** Returns {@code true} when this quest is part of a branch choice set. */
    public boolean isBranchingQuest() {
        return branchGroup != null && !branchGroup.isBlank() && branchPath != null && !branchPath.isBlank();
    }

    // ========================================================================================
    // Objective Progression (used by the saga system)
    // ========================================================================================

    /**
     * Returns the current objective in sequential mode, or {@code null} if all objectives are done.
     */
    public QuestObjective getCurrentObjective() {
        if (currentObjectiveIndex >= 0 && currentObjectiveIndex < objectives.size()) {
            return objectives.get(currentObjectiveIndex);
        }
        return null;
    }

    /**
     * Advances to the next objective index (sequential mode).
     *
     * @return {@code true} if advanced successfully, {@code false} if already at the last objective
     */
    public boolean advanceObjective() {
        if (currentObjectiveIndex < objectives.size() - 1) {
            currentObjectiveIndex++;
            return true;
        }
        return false;
    }

    /**
     * Checks whether all objectives in this quest are completed (sequential mode).
     */
    public boolean areAllObjectivesCompleted() {
        return currentObjectiveIndex >= objectives.size() - 1 &&
               (getCurrentObjective() == null || getCurrentObjective().isCompleted());
    }
}
