package com.dragonminez.common.quest;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Shared evaluator for quest unlock prerequisites and quest start requirements.
 */
public class QuestAvailabilityChecker {

	public static boolean isAvailable(Quest quest, StatsData statsData) {
		if (quest == null || statsData == null) return false;
		if (!quest.hasPrerequisites()) return true;
		return evaluate(quest.getPrerequisites(), new EvaluationContext(statsData, statsData.getPlayer(), null), false);
	}

	public static boolean areStartRequirementsMet(Quest quest, String questKey, Player player, StatsData statsData) {
		if (quest == null || statsData == null) return false;
		if (!quest.hasStartRequirements()) return true;
		if (player == null) return false;

		if (!player.level().isClientSide) {
			primeStartRequirementTiming(quest, questKey, player, statsData);
		}
		return evaluate(quest.getStartRequirements(), new EvaluationContext(statsData, player, questKey), false);
	}

	public static boolean primeStartRequirementTiming(Quest quest, String questKey, Player player, StatsData statsData) {
		if (quest == null || statsData == null || player == null) return false;
		if (player.level().isClientSide) return false;
		if (!quest.hasStartRequirements() || questKey == null || questKey.isBlank()) return false;
		if (!containsTimeCondition(quest.getStartRequirements())) return false;

		EvaluationContext context = new EvaluationContext(statsData, player, questKey);
		if (!evaluate(quest.getStartRequirements(), context, true)) {
			return false;
		}

		return statsData.getPlayerQuestData().ensureStartRequirementTiming(
				questKey,
				context.gameTime(),
				context.realTimeMs()
		);
	}

	public static Component describeAvailabilityFailure(Quest quest, StatsData statsData) {
		if (quest == null || statsData == null || !quest.hasPrerequisites()) {
			return null;
		}
		return describeFailure(quest.getPrerequisites(), new EvaluationContext(statsData, statsData.getPlayer(), null), false);
	}

	public static Component describeStartRequirementFailure(Quest quest, String questKey, Player player, StatsData statsData) {
		if (quest == null || statsData == null || !quest.hasStartRequirements() || player == null) {
			return null;
		}
		if (!player.level().isClientSide) {
			primeStartRequirementTiming(quest, questKey, player, statsData);
		}
		return describeFailure(quest.getStartRequirements(), new EvaluationContext(statsData, player, questKey), false);
	}

	public static Component describeQuestStartBlocker(Quest quest, String questKey, Player player, StatsData statsData) {
		Component availabilityFailure = describeAvailabilityFailure(quest, statsData);
		if (availabilityFailure != null) {
			return availabilityFailure;
		}
		return describeStartRequirementFailure(quest, questKey, player, statsData);
	}

	static boolean matchesAlignmentCondition(QuestPrerequisites.Condition condition, int alignment) {
		if (condition == null || condition.getType() != QuestPrerequisites.ConditionType.ALIGNMENT) {
			return false;
		}
		Integer min = condition.getMinAlignment();
		Integer max = condition.getMaxAlignment();
		if (min != null && alignment < min) return false;
		return max == null || alignment <= max;
	}

	public static boolean isSagaQuestAvailable(Quest quest, Saga saga, int questIndex, StatsData statsData) {
		if (quest == null || saga == null || statsData == null) {
			return false;
		}

		if (!isSequentiallyReachable(saga, questIndex, statsData.getPlayerQuestData())) {
			return false;
		}

		return !quest.hasPrerequisites() || isAvailable(quest, statsData);
	}

	private static boolean isSequentiallyReachable(Saga saga, int questIndex, PlayerQuestData pqd) {
		if (questIndex <= 0) {
			return true;
		}

		List<Quest> sagaQuests = saga.getQuests();
		if (questIndex >= sagaQuests.size()) {
			return false;
		}

		Quest previous = sagaQuests.get(questIndex - 1);
		String previousKey = PlayerQuestData.sagaQuestKey(saga.getId(), previous.getId());
		return pqd.isQuestCompleted(previousKey);
	}

	private static boolean containsTimeCondition(QuestPrerequisites prereqs) {
		if (prereqs == null || prereqs.conditions().isEmpty()) return false;
		for (QuestPrerequisites.Condition condition : prereqs.conditions()) {
			if (condition == null) continue;
			if (condition.isNestedGroup()) {
				if (containsTimeCondition(condition.getNested())) {
					return true;
				}
				continue;
			}

			if (condition.getType() == QuestPrerequisites.ConditionType.TIME) {
				return true;
			}
		}
		return false;
	}

	private static boolean evaluate(QuestPrerequisites prereqs, EvaluationContext context, boolean ignoreTimeConditions) {
		if (prereqs == null || prereqs.conditions().isEmpty()) return true;

		if (prereqs.operator() == QuestPrerequisites.Operator.AND) {
			for (QuestPrerequisites.Condition condition : prereqs.conditions()) {
				if (!evaluateCondition(condition, context, ignoreTimeConditions)) return false;
			}
			return true;
		}

		for (QuestPrerequisites.Condition condition : prereqs.conditions()) {
			if (evaluateCondition(condition, context, ignoreTimeConditions)) return true;
		}
		return false;
	}

	private static Component describeFailure(QuestPrerequisites prereqs, EvaluationContext context, boolean ignoreTimeConditions) {
		if (prereqs == null || prereqs.conditions().isEmpty()) {
			return null;
		}

		if (prereqs.operator() == QuestPrerequisites.Operator.AND) {
			for (QuestPrerequisites.Condition condition : prereqs.conditions()) {
				Component failure = describeConditionFailure(condition, context, ignoreTimeConditions);
				if (failure != null) {
					return failure;
				}
			}
			return null;
		}

		Component firstFailure = null;
		for (QuestPrerequisites.Condition condition : prereqs.conditions()) {
			Component failure = describeConditionFailure(condition, context, ignoreTimeConditions);
			if (failure == null) {
				return null;
			}
			if (firstFailure == null) {
				firstFailure = failure;
			}
		}
		return firstFailure;
	}

	private static Component describeConditionFailure(QuestPrerequisites.Condition condition, EvaluationContext context, boolean ignoreTimeConditions) {
		if (condition == null) return Component.translatable("message.dragonminez.quest.start.unavailable");
		if (condition.isNestedGroup()) {
			return describeFailure(condition.getNested(), context, ignoreTimeConditions);
		}
		if (condition.getType() == null) {
			return Component.translatable("message.dragonminez.quest.start.unavailable");
		}
		if (evaluateCondition(condition, context, ignoreTimeConditions)) {
			return null;
		}
		Component base = QuestTextFormatter.describeRequirement(condition, context.toRequirementContext());
		Component current = describeCurrentLocation(condition.getType(), context);
		if (current == null) {
			return base;
		}
		return Component.empty().append(base)
				.append(Component.translatable("message.dragonminez.quest.start.current_location", current));
	}

	/**
	 * For location-based requirements (biome/dimension), resolves the player's <em>current</em>
	 * location so the failure message can spell out the mismatch — e.g. a party member standing in
	 * {@code sunflower_plains} a few blocks from the required {@code plains}, or a leader who is in a
	 * different biome entirely. Returns {@code null} for non-location conditions.
	 */
	private static Component describeCurrentLocation(QuestPrerequisites.ConditionType type, EvaluationContext context) {
		if (type == null) {
			return null;
		}
		Level level = context.level();
		if (level == null) {
			return null;
		}
		return switch (type) {
			case BIOME -> {
				BlockPos pos = context.pos();
				if (pos == null) {
					yield null;
				}
				yield level.getBiome(pos).unwrapKey()
						.map(key -> QuestTextFormatter.resolveBiomeName(key.location().toString()))
						.orElse(null);
			}
			case DIMENSION -> QuestTextFormatter.resolveDimensionName(level.dimension().location().toString());
			default -> null;
		};
	}

	private static boolean evaluateCondition(QuestPrerequisites.Condition condition, EvaluationContext context, boolean ignoreTimeConditions) {
		if (condition == null) return false;
		if (condition.isNestedGroup()) {
			return evaluate(condition.getNested(), context, ignoreTimeConditions);
		}
		if (condition.getType() == null) return false;

		StatsData data = context.data();
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
				yield data.getCurrentStatValue(stat) >= minValue;
			}
			case LEVEL -> {
				Integer minLevel = condition.getMinLevel();
				if (minLevel == null) yield false;
				yield data.getLevel() >= minLevel;
			}
			case BIOME -> {
				Level level = context.level();
				BlockPos pos = context.pos();
				String biomeId = condition.getBiomeId();
				if (level == null || pos == null || biomeId == null) yield false;
				yield QuestLocationHelper.matchesBiome(level, pos, biomeId);
			}
			case STRUCTURE -> {
				Level level = context.level();
				BlockPos pos = context.pos();
				String structureId = condition.getStructureId();
				if (level == null || pos == null || structureId == null) yield false;
				if (!(level instanceof ServerLevel serverLevel)) {
					yield true;
				}
				yield QuestLocationHelper.isInStructure(serverLevel, pos, structureId);
			}
			case DIMENSION -> {
				Level level = context.level();
				String dimensionId = condition.getDimensionId();
				if (level == null || dimensionId == null) yield false;
				yield QuestLocationHelper.isInDimension(level, dimensionId);
			}
			case TIME -> {
				if (ignoreTimeConditions) yield true;
				if (context.questKey() == null || context.questKey().isBlank()) yield false;

				QuestPrerequisites.TimeMode timeMode = condition.getTimeMode();
				Long duration = condition.getDuration();
				if (timeMode == null || duration == null || duration <= 0L) yield true;

				PlayerQuestData.QuestStartRequirementTiming timing = pqd.getStartRequirementTiming(context.questKey());
				if (timing == null) yield false;

				yield switch (timeMode) {
					case GAME_TIME -> context.gameTime() - timing.getGameTimeStarted() >= duration;
					case REAL_TIME -> context.realTimeMs() - timing.getRealTimeStartedMs() >= duration;
				};
			}
			case ALIGNMENT -> matchesAlignmentCondition(condition, data.getResources().getAlignment());
			case SKILL -> {
				String skill = condition.getSkill();
				Integer skillLevel = condition.getSkillLevel();
				if (skill == null || skillLevel == null) yield false;
				yield data.getSkills().getSkillLevel(skill) >= skillLevel;
			}
			case RACE -> {
				String requiredRace = condition.getRace();
				if (requiredRace == null) yield false;
				yield requiredRace.equalsIgnoreCase(data.getCharacter().getRaceName());
			}
			case CLASS -> {
				String requiredClass = condition.getCharacterClass();
				if (requiredClass == null) yield false;
				String playerClass = data.getCharacter().getCharacterClass();
				yield playerClass != null && playerClass.equalsIgnoreCase(requiredClass);
			}
		};
	}

	private record EvaluationContext(StatsData data, Player player, String questKey) {
		Level level() {
			return player != null ? player.level() : null;
		}

		BlockPos pos() {
			return player != null ? player.blockPosition() : null;
		}

		long gameTime() {
			return level() != null ? level().getGameTime() : 0L;
		}

		long realTimeMs() {
			return System.currentTimeMillis();
		}

		QuestTextFormatter.RequirementContext toRequirementContext() {
			return new QuestTextFormatter.RequirementContext(data, player, questKey);
		}
	}
}
