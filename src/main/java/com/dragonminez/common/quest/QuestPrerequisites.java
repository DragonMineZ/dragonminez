package com.dragonminez.common.quest;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines prerequisites that must be met before a quest can be accepted.
 * <p>
 * Supports nested {@link Operator#AND}/{@link Operator#OR} logic trees with leaf conditions
 * that check saga progress, other quest completions, player stats, race, or level.
 * <p>
 * Prerequisites are parsed from JSON by {@link QuestParser#parsePrerequisites} and evaluated
 * at runtime by {@link QuestAvailabilityChecker}.
 *
 * <h3>JSON Example</h3>
 * <pre>{@code
 * {
 *   "operator": "AND",
 *   "conditions": [
 *     { "type": "QUEST", "questId": "roshi_basic_training" },
 *     { "type": "LEVEL", "minLevel": 5 },
 *     {
 *       "operator": "OR",
 *       "conditions": [
 *         { "type": "RACE", "race": "saiyan" },
 *         { "type": "RACE", "race": "human" }
 *       ]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @since 2.0
 * @see QuestAvailabilityChecker
 * @see QuestParser#parsePrerequisites
 */
@Getter
public class QuestPrerequisites {
	private final Operator operator;
	private final List<Condition> conditions;

	public QuestPrerequisites(Operator operator, List<Condition> conditions) {
		this.operator = operator != null ? operator : Operator.AND;
		this.conditions = conditions != null ? conditions : new ArrayList<>();
	}

	public enum Operator {
		AND, OR
	}

	public enum ConditionType {
		/** A specific saga quest must be completed */
		SAGA_QUEST,
		/** Another quest (by string ID) must be completed */
		QUEST,
		/** A player stat must meet a minimum threshold */
		STAT,
		/** Player must be a specific race */
		RACE,
		/** Player level must meet a minimum threshold */
		LEVEL
	}

	/**
	 * A prerequisite condition. Can be either a leaf (type-based check) or a nested group.
	 * <p>
	 * If {@code type} is non-null, this is a leaf condition and the relevant fields for that type are used.
	 * If {@code type} is null and {@code nested} is non-null, this is a nested AND/OR group.
	 */
	@Getter
	public static class Condition {
		private final ConditionType type;

		// SAGA_QUEST fields
		private final String sagaId;
		private final Integer questId;

		// QUEST fields (by string ID — replaces old SIDE_QUEST)
		private final String requiredQuestId;

		// STAT fields
		private final String stat;
		private final Integer minValue;

		// RACE fields
		private final String race;

		// LEVEL fields
		private final Integer minLevel;

		// Nested group
		private final QuestPrerequisites nested;

		private Condition(ConditionType type, String sagaId, Integer questId, String requiredQuestId,
						  String stat, Integer minValue, String race, Integer minLevel,
						  QuestPrerequisites nested) {
			this.type = type;
			this.sagaId = sagaId;
			this.questId = questId;
			this.requiredQuestId = requiredQuestId;
			this.stat = stat;
			this.minValue = minValue;
			this.race = race;
			this.minLevel = minLevel;
			this.nested = nested;
		}

		public boolean isNestedGroup() {
			return type == null && nested != null;
		}

		public static Condition sagaQuest(String sagaId, int questId) {
			return new Condition(ConditionType.SAGA_QUEST, sagaId, questId, null, null, null, null, null, null);
		}

		/** Requires another quest (by string ID) to be completed. Replaces the old sideQuest() factory. */
		public static Condition quest(String requiredQuestId) {
			return new Condition(ConditionType.QUEST, null, null, requiredQuestId, null, null, null, null, null);
		}


		public static Condition stat(String stat, int minValue) {
			return new Condition(ConditionType.STAT, null, null, null, stat, minValue, null, null, null);
		}

		public static Condition race(String race) {
			return new Condition(ConditionType.RACE, null, null, null, null, null, race, null, null);
		}

		public static Condition level(int minLevel) {
			return new Condition(ConditionType.LEVEL, null, null, null, null, null, null, minLevel, null);
		}

		public static Condition nestedGroup(QuestPrerequisites nested) {
			return new Condition(null, null, null, null, null, null, null, null, nested);
		}
	}
}

