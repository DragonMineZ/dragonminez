package com.dragonminez.common.quest;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable AND/OR condition tree used by both quest unlock prerequisites and quest start requirements.
 * <p>
 * Supports nested {@link Operator#AND}/{@link Operator#OR} logic groups with leaf conditions
 * for progression, player stats, location, dimension, and elapsed time.
 * <p>
 * Conditions are parsed from JSON by {@link QuestParser#parsePrerequisites} and evaluated
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
 *         { "type": "BIOME", "biome": "minecraft:plains" },
 *         { "type": "STRUCTURE", "structure": "dragonminez:roshi_house" }
 *       ]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @see QuestAvailabilityChecker
 * @see QuestParser#parsePrerequisites
 * @since 2.0
 */
public record QuestPrerequisites(Operator operator, List<Condition> conditions) {
	public QuestPrerequisites(Operator operator, List<Condition> conditions) {
		this.operator = operator != null ? operator : Operator.AND;
		this.conditions = conditions != null ? conditions : new ArrayList<>();
	}

	public enum Operator {
		AND, OR
	}

	public enum ConditionType {
		/**
		 * A specific saga quest must be completed
		 */
		SAGA_QUEST,
		/**
		 * Another quest (by string ID) must be completed
		 */
		QUEST,
		/**
		 * A player stat must meet a minimum threshold
		 */
		STAT,
		/**
		 * Player level must meet a minimum threshold
		 */
		LEVEL,
		/**
		 * Player must currently be in a specific biome.
		 */
		BIOME,
		/**
		 * Player must currently be inside a specific structure.
		 */
		STRUCTURE,
		/**
		 * Player must currently be inside a specific dimension.
		 */
		DIMENSION,
		/**
		 * A duration must have elapsed since this quest first became start-eligible.
		 */
		TIME
	}

	public enum TimeMode {
		GAME_TIME,
		REAL_TIME
	}

	public record StructureHint(String dimensionId, Integer x, Integer y, Integer z) {
		public boolean hasCoordinates() {
			return x != null && y != null && z != null;
		}
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

		// QUEST fields
		private final String requiredQuestId;

		private final String stat;
		private final Integer minValue;
		private final Integer minLevel;
		private final String biomeId;
		private final String structureId;
		private final StructureHint structureHint;
		private final String dimensionId;
		private final TimeMode timeMode;
		private final Long duration;

		// Nested group
		private final QuestPrerequisites nested;

		private Condition(ConditionType type, String sagaId, Integer questId, String requiredQuestId,
						  String stat, Integer minValue, Integer minLevel, String biomeId,
						  String structureId, StructureHint structureHint, String dimensionId,
						  TimeMode timeMode, Long duration, QuestPrerequisites nested) {
			this.type = type;
			this.sagaId = sagaId;
			this.questId = questId;
			this.requiredQuestId = requiredQuestId;
			this.stat = stat;
			this.minValue = minValue;
			this.minLevel = minLevel;
			this.biomeId = biomeId;
			this.structureId = structureId;
			this.structureHint = structureHint;
			this.dimensionId = dimensionId;
			this.timeMode = timeMode;
			this.duration = duration;
			this.nested = nested;
		}

		public boolean isNestedGroup() {
			return type == null && nested != null;
		}

		public static Condition sagaQuest(String sagaId, int questId) {
			return new Condition(ConditionType.SAGA_QUEST, sagaId, questId, null,
					null, null, null, null,
					null, null, null, null, null,
					null);
		}

		/**
		 * Requires another quest (by string ID) to be completed.
		 */
		public static Condition quest(String requiredQuestId) {
			return new Condition(ConditionType.QUEST, null, null, requiredQuestId,
					null, null, null, null,
					null, null, null, null, null,
					null);
		}

		public static Condition stat(String stat, int minValue) {
			return new Condition(ConditionType.STAT, null, null, null,
					stat, minValue, null, null,
					null, null, null, null, null,
					null);
		}

		public static Condition level(int minLevel) {
			return new Condition(ConditionType.LEVEL, null, null, null,
					null, null, minLevel, null,
					null, null, null, null, null,
					null);
		}

		public static Condition biome(String biomeId) {
			return new Condition(ConditionType.BIOME, null, null, null,
					null, null, null, biomeId,
					null, null, null, null, null,
					null);
		}

		public static Condition structure(String structureId, StructureHint structureHint) {
			return new Condition(ConditionType.STRUCTURE, null, null, null,
					null, null, null, null,
					structureId, structureHint, null, null, null,
					null);
		}

		public static Condition dimension(String dimensionId) {
			return new Condition(ConditionType.DIMENSION, null, null, null,
					null, null, null, null,
					null, null, dimensionId, null, null,
					null);
		}

		public static Condition time(TimeMode timeMode, long duration) {
			return new Condition(ConditionType.TIME, null, null, null,
					null, null, null, null,
					null, null, null, timeMode, duration,
					null);
		}

		public static Condition nestedGroup(QuestPrerequisites nested) {
			return new Condition(null, null, null, null,
					null, null, null, null,
					null, null, null, null, null,
					nested);
		}
	}
}
