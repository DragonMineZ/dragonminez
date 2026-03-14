package com.dragonminez.common.quest;

import com.dragonminez.common.stats.character.Stats;
import com.dragonminez.common.stats.StatsData;

/**
 * Evaluates whether a player meets all {@link QuestPrerequisites} for a given quest.
 * Uses {@link PlayerQuestData} for all quest completion checks.
 *
 * @since 2.1
 */
public class QuestAvailabilityChecker {

	public static boolean isAvailable(Quest quest, StatsData statsData) {
		if (quest == null || statsData == null) return false;
		if (!quest.hasPrerequisites()) return true;
		return evaluate(quest.getPrerequisites(), statsData);
	}

	private static boolean evaluate(QuestPrerequisites prereqs, StatsData data) {
		if (prereqs == null || prereqs.getConditions().isEmpty()) return true;

		if (prereqs.getOperator() == QuestPrerequisites.Operator.AND) {
			for (QuestPrerequisites.Condition condition : prereqs.getConditions()) {
				if (!evaluateCondition(condition, data)) return false;
			}
			return true;
		} else {
			for (QuestPrerequisites.Condition condition : prereqs.getConditions()) {
				if (evaluateCondition(condition, data)) return true;
			}
			return false;
		}
	}

	private static boolean evaluateCondition(QuestPrerequisites.Condition condition, StatsData data) {
		if (condition.isNestedGroup()) {
			return evaluate(condition.getNested(), data);
		}

		if (condition.getType() == null) return false;
		PlayerQuestData pqd = data.getPlayerQuestData();

		return switch (condition.getType()) {
			case SAGA_QUEST -> {
				String sagaId = condition.getSagaId();
				Integer questId = condition.getQuestId();
				if (sagaId == null || questId == null) yield false;
				yield pqd.isQuestCompleted(PlayerQuestData.sagaQuestKey(sagaId, questId));
			}
			case QUEST -> {
				String requiredQuestId = condition.getRequiredQuestId();
				if (requiredQuestId == null) yield false;
				yield pqd.isQuestCompleted(requiredQuestId);
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

