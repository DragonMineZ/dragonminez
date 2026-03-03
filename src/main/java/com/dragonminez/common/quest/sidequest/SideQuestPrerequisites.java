package com.dragonminez.common.quest.sidequest;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SideQuestPrerequisites {
	private final Operator operator;
	private final List<Condition> conditions;

	public SideQuestPrerequisites(Operator operator, List<Condition> conditions) {
		this.operator = operator != null ? operator : Operator.AND;
		this.conditions = conditions != null ? conditions : new ArrayList<>();
	}

	public enum Operator {
		AND, OR
	}

	public enum ConditionType {
		/** A specific saga quest must be completed */
		SAGA_QUEST,
		/** Another side-quest must be completed */
		SIDE_QUEST,
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

		// SIDE_QUEST fields
		private final String sideQuestId;

		// STAT fields
		private final String stat;
		private final Integer minValue;

		// RACE fields
		private final String race;

		// LEVEL fields
		private final Integer minLevel;

		// Nested group
		private final SideQuestPrerequisites nested;

		private Condition(ConditionType type, String sagaId, Integer questId, String sideQuestId,
						  String stat, Integer minValue, String race, Integer minLevel,
						  SideQuestPrerequisites nested) {
			this.type = type;
			this.sagaId = sagaId;
			this.questId = questId;
			this.sideQuestId = sideQuestId;
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

		public static Condition sideQuest(String sideQuestId) {
			return new Condition(ConditionType.SIDE_QUEST, null, null, sideQuestId, null, null, null, null, null);
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

		public static Condition nestedGroup(SideQuestPrerequisites nested) {
			return new Condition(null, null, null, null, null, null, null, null, nested);
		}
	}
}

