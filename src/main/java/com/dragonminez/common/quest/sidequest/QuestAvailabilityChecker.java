package com.dragonminez.common.quest.sidequest;

import com.dragonminez.common.stats.Stats;
import com.dragonminez.common.stats.StatsData;

public class QuestAvailabilityChecker {

	/**
	 * Checks whether a player meets all prerequisites for the given side-quest.
	 *
	 * @param quest     the side-quest to check
	 * @param statsData the player's stats data
	 * @return true if the player is eligible to accept this side-quest
	 */
	public static boolean isAvailable(SideQuest quest, StatsData statsData) {
		if (quest == null || statsData == null) return false;
		if (!quest.hasPrerequisites()) return true;
		return evaluate(quest.getPrerequisites(), statsData);
	}

	private static boolean evaluate(SideQuestPrerequisites prereqs, StatsData data) {
		if (prereqs == null || prereqs.getConditions().isEmpty()) return true;

		if (prereqs.getOperator() == SideQuestPrerequisites.Operator.AND) {
			for (SideQuestPrerequisites.Condition condition : prereqs.getConditions()) {
				if (!evaluateCondition(condition, data)) return false;
			}
			return true;
		} else {
			// OR
			for (SideQuestPrerequisites.Condition condition : prereqs.getConditions()) {
				if (evaluateCondition(condition, data)) return true;
			}
			return false;
		}
	}

	private static boolean evaluateCondition(SideQuestPrerequisites.Condition condition, StatsData data) {
		// Nested group — recurse
		if (condition.isNestedGroup()) {
			return evaluate(condition.getNested(), data);
		}

		if (condition.getType() == null) return false;

		return switch (condition.getType()) {
			case SAGA_QUEST -> {
				String sagaId = condition.getSagaId();
				Integer questId = condition.getQuestId();
				if (sagaId == null || questId == null) yield false;
				yield data.getQuestData().isQuestCompleted(sagaId, questId);
			}
			case SIDE_QUEST -> {
				String sideQuestId = condition.getSideQuestId();
				if (sideQuestId == null) yield false;
				yield data.getSideQuestData().isQuestCompleted(sideQuestId);
			}
			case STAT -> {
				String stat = condition.getStat();
				Integer minValue = condition.getMinValue();
				if (stat == null || minValue == null) yield false;
				yield getStatValue(data.getStats(), stat) >= minValue;
			}
			case RACE -> {
				String race = condition.getRace();
				if (race == null) yield false;
				yield data.getCharacter().getRaceName().equalsIgnoreCase(race);
			}
			case LEVEL -> {
				Integer minLevel = condition.getMinLevel();
				if (minLevel == null) yield false;
				yield data.getLevel() >= minLevel;
			}
		};
	}

	private static int getStatValue(Stats stats, String statName) {
		return switch (statName.toUpperCase()) {
			case "STR" -> stats.getStrength();
			case "SKP" -> stats.getStrikePower();
			case "RES" -> stats.getResistance();
			case "VIT" -> stats.getVitality();
			case "PWR" -> stats.getKiPower();
			case "ENE" -> stats.getEnergy();
			default -> 0;
		};
	}
}

