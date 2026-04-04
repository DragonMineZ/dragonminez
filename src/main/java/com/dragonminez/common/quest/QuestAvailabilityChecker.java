package com.dragonminez.common.quest;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

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

	public static boolean isStartConditionMet(QuestPrerequisites.Condition condition, String questKey, Player player, StatsData statsData) {
		if (condition == null || statsData == null || player == null) return false;
		return evaluateCondition(condition, new EvaluationContext(statsData, player, questKey), false);
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
		return describeCondition(condition, context);
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
		};
	}

	private static Component describeCondition(QuestPrerequisites.Condition condition, EvaluationContext context) {
		return switch (condition.getType()) {
			case SAGA_QUEST -> Component.translatable(
					"gui.dragonminez.quests.requirement.complete_saga",
					resolveSagaQuestName(condition.getSagaId(), condition.getQuestId())
			);
			case QUEST -> Component.translatable(
					"gui.dragonminez.quests.requirement.complete_quest",
					resolveQuestName(condition.getRequiredQuestId())
			);
			case STAT -> Component.translatable(
					"gui.dragonminez.quests.requirement.stat",
					condition.getMinValue(),
					humanizeIdentifier(condition.getStat())
			);
			case LEVEL -> Component.translatable(
					"gui.dragonminez.quests.requirement.level",
					condition.getMinLevel()
			);
			case BIOME -> Component.translatable(
					"gui.dragonminez.quests.requirement.biome",
					humanizeResourceIdentifier(condition.getBiomeId())
			);
			case STRUCTURE -> buildStructureRequirement(condition);
			case DIMENSION -> Component.translatable(
					"gui.dragonminez.quests.requirement.dimension",
					humanizeResourceIdentifier(condition.getDimensionId())
			);
			case TIME -> buildTimeRequirement(condition, context);
		};
	}

	private static Component buildStructureRequirement(QuestPrerequisites.Condition condition) {
		MutableComponent base = Component.translatable(
				"gui.dragonminez.quests.requirement.structure",
				humanizeResourceIdentifier(condition.getStructureId())
		);

		QuestPrerequisites.StructureHint hint = condition.getStructureHint();
		if (hint == null || (!hint.hasCoordinates() && hint.dimensionId() == null)) {
			return base;
		}

		if (hint.dimensionId() != null) {
			base.append(Component.literal(" ("))
					.append(Component.translatable("gui.dragonminez.quests.requirement.dimension", humanizeResourceIdentifier(hint.dimensionId())))
					.append(Component.literal(")"));
		}

		if (hint.hasCoordinates()) {
			base.append(Component.literal(" - "))
					.append(Component.translatable("gui.dragonminez.quests.requirement.coords", hint.x(), hint.y(), hint.z()));
		}

		return base;
	}

	private static Component buildTimeRequirement(QuestPrerequisites.Condition condition, EvaluationContext context) {
		long duration = condition.getDuration() != null ? condition.getDuration() : 0L;
		MutableComponent base = condition.getTimeMode() == QuestPrerequisites.TimeMode.REAL_TIME
				? Component.translatable("gui.dragonminez.quests.requirement.time_real", formatRealTimeDuration(duration))
				: Component.translatable("gui.dragonminez.quests.requirement.time_game", formatGameTimeDuration(duration));

		if (context.questKey() == null || context.questKey().isBlank() || duration <= 0L) {
			return base;
		}

		PlayerQuestData.QuestStartRequirementTiming timing = context.data().getPlayerQuestData().getStartRequirementTiming(context.questKey());
		if (timing == null) {
			return base;
		}

		long remaining;
		if (condition.getTimeMode() == QuestPrerequisites.TimeMode.REAL_TIME) {
			long elapsed = Math.max(0L, context.realTimeMs() - timing.getRealTimeStartedMs());
			remaining = Math.max(0L, duration - elapsed);
			if (remaining > 0L) {
				return base.append(Component.literal(" ("))
						.append(Component.translatable("gui.dragonminez.quests.requirement.remaining", formatRealTimeDuration(remaining)))
						.append(Component.literal(")"));
			}
			return base.append(Component.literal(" ("))
					.append(Component.translatable("gui.dragonminez.quests.requirement.ready"))
					.append(Component.literal(")"));
		}

		long elapsed = Math.max(0L, context.gameTime() - timing.getGameTimeStarted());
		remaining = Math.max(0L, duration - elapsed);
		if (remaining > 0L) {
			return base.append(Component.literal(" ("))
					.append(Component.translatable("gui.dragonminez.quests.requirement.remaining", formatGameTimeDuration(remaining)))
					.append(Component.literal(")"));
		}
		return base.append(Component.literal(" ("))
				.append(Component.translatable("gui.dragonminez.quests.requirement.ready"))
				.append(Component.literal(")"));
	}

	private static Component resolveSagaQuestName(String sagaId, Integer questId) {
		if (sagaId == null || questId == null) {
			return Component.literal("?");
		}

		Saga saga = QuestRegistry.getSaga(sagaId);
		if (saga == null) {
			saga = QuestRegistry.getClientSaga(sagaId);
		}
		if (saga != null) {
			Quest quest = saga.getQuestById(questId);
			if (quest != null) {
				return displayText(quest.getTitle());
			}
		}
		return Component.literal(humanizeIdentifier(sagaId) + " " + questId);
	}

	private static Component resolveQuestName(String questId) {
		if (questId == null || questId.isBlank()) {
			return Component.literal("?");
		}

		Quest quest = QuestRegistry.getQuest(questId);
		if (quest == null) {
			quest = QuestRegistry.getClientQuest(questId);
		}
		if (quest != null) {
			return displayText(quest.getTitle());
		}
		return Component.literal(humanizeResourceIdentifier(questId));
	}

	private static Component displayText(String raw) {
		if (raw == null || raw.isBlank()) {
			return Component.literal("?");
		}
		if (!raw.contains(" ") && raw.contains(".")) {
			return Component.translatable(raw);
		}
		return Component.literal(raw);
	}

	private static String humanizeResourceIdentifier(String raw) {
		if (raw == null || raw.isBlank()) return "?";
		String value = raw.startsWith("#") ? raw.substring(1) : raw;
		int colon = value.indexOf(':');
		String token = colon >= 0 ? value.substring(colon + 1) : value;
		return humanizeIdentifier(token);
	}

	private static String humanizeIdentifier(String raw) {
		if (raw == null || raw.isBlank()) return "?";
		String normalized = raw.replace('_', ' ').replace('-', ' ');
		String[] parts = normalized.split("\\s+");
		StringBuilder builder = new StringBuilder();
		for (String part : parts) {
			if (part.isBlank()) continue;
			if (!builder.isEmpty()) builder.append(' ');
			builder.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) {
				builder.append(part.substring(1).toLowerCase());
			}
		}
		return builder.toString();
	}

	private static String formatGameTimeDuration(long ticks) {
		if (ticks <= 0L) return "0s";
		long totalSeconds = Math.max(1L, ticks / 20L);
		return formatSeconds(totalSeconds);
	}

	private static String formatRealTimeDuration(long millis) {
		if (millis <= 0L) return "0s";
		long totalSeconds = Math.max(1L, millis / 1000L);
		return formatSeconds(totalSeconds);
	}

	private static String formatSeconds(long totalSeconds) {
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		long seconds = totalSeconds % 60L;

		if (hours > 0L) {
			return minutes > 0L ? hours + "h " + minutes + "m" : hours + "h";
		}
		if (minutes > 0L) {
			return seconds > 0L ? minutes + "m " + seconds + "s" : minutes + "m";
		}
		return seconds + "s";
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
	}
}
