package com.dragonminez.common.quest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestUnlockOrder {
	private static final Gate NONE = new Gate(-1, -1, 0, 0);

	private final Map<String, Quest> quests;
	private final Map<String, Saga> sagas;
	private final Map<String, Integer> sagaRanks = new HashMap<>();
	private final Map<String, Gate> gates = new HashMap<>();
	private final Set<String> resolving = new HashSet<>();

	private QuestUnlockOrder(Map<String, Quest> quests, Map<String, Saga> sagas) {
		this.quests = quests;
		this.sagas = sagas;
		rankSagas();
	}

	public static Comparator<String> comparator(Map<String, Quest> quests, Map<String, Saga> sagas) {
		QuestUnlockOrder order = new QuestUnlockOrder(quests, sagas);
		return Comparator.comparing(order::gate, Gate.ORDER).thenComparing(Comparator.naturalOrder());
	}

	private void rankSagas() {
		Map<String, Integer> depths = new HashMap<>();
		for (String sagaId : sagas.keySet()) sagaDepth(sagaId, depths, new HashSet<>());
		List<String> ordered = new ArrayList<>(sagas.keySet());
		ordered.sort(Comparator.<String>comparingInt(depths::get).thenComparing(String.CASE_INSENSITIVE_ORDER));
		for (int i = 0; i < ordered.size(); i++) sagaRanks.put(ordered.get(i), i);
	}

	private int sagaDepth(String sagaId, Map<String, Integer> depths, Set<String> visiting) {
		Integer known = depths.get(sagaId);
		if (known != null) return known;
		Saga saga = sagas.get(sagaId);
		String previous = saga != null && saga.getRequirements() != null ? saga.getRequirements().previousSagaId() : null;
		int depth = 0;
		if (previous != null && !previous.isBlank() && sagas.containsKey(previous) && visiting.add(sagaId)) {
			depth = sagaDepth(previous, depths, visiting) + 1;
		}
		depths.put(sagaId, depth);
		return depth;
	}

	private Gate gate(String questKey) {
		Gate cached = gates.get(questKey);
		if (cached != null) return cached;
		if (!resolving.add(questKey)) return NONE;
		Gate gate = compute(questKey);
		resolving.remove(questKey);
		gates.put(questKey, gate);
		return gate;
	}

	private Gate compute(String questKey) {
		int separator = questKey.lastIndexOf(':');
		if (separator > 0) {
			Saga saga = sagas.get(questKey.substring(0, separator));
			String number = questKey.substring(separator + 1);
			boolean numeric = !number.isEmpty() && number.length() < 10 && number.chars().allMatch(Character::isDigit);
			Quest step = saga != null && numeric ? saga.getQuestById(Integer.parseInt(number)) : null;
			if (step != null) return sagaQuest(saga, step);
		}
		Quest quest = quests.get(questKey);
		if (quest == null) return NONE;
		return Gate.all(List.of(group(quest.getPrerequisites()), group(quest.getStartRequirements())));
	}

	private Gate sagaQuest(Saga saga, Quest step) {
		List<Quest> steps = saga.getQuests();
		int index = steps.indexOf(step);
		List<Gate> parts = new ArrayList<>();
		parts.add(new Gate(sagaRank(saga.getId()), index, 0, 0));
		String previous = index > 0 ? saga.getId() + ":" + steps.get(index - 1).getId() : lastStepOfPreviousSaga(saga);
		if (previous != null) parts.add(gate(previous).after());
		parts.add(group(step.getPrerequisites()));
		parts.add(group(step.getStartRequirements()));
		return Gate.all(parts);
	}

	private String lastStepOfPreviousSaga(Saga saga) {
		String previousId = saga.getRequirements() != null ? saga.getRequirements().previousSagaId() : null;
		Saga previous = previousId != null && !previousId.isBlank() ? sagas.get(previousId) : null;
		if (previous == null || previous.getQuests().isEmpty()) return null;
		return previous.getId() + ":" + previous.getQuests().get(previous.getQuests().size() - 1).getId();
	}

	private Gate group(QuestPrerequisites prerequisites) {
		if (prerequisites == null || prerequisites.conditions().isEmpty()) return NONE;
		List<Gate> children = new ArrayList<>();
		for (QuestPrerequisites.Condition condition : prerequisites.conditions()) children.add(condition(condition));
		return prerequisites.operator() == QuestPrerequisites.Operator.OR ? Gate.any(children) : Gate.all(children);
	}

	private Gate condition(QuestPrerequisites.Condition condition) {
		if (condition.isNestedGroup()) return group(condition.getNested());
		if (condition.getType() == null) return NONE;
		return switch (condition.getType()) {
			case SAGA_QUEST -> condition.getSagaId() != null && condition.getQuestId() != null
					? gate(condition.getSagaId() + ":" + condition.getQuestId()).after() : NONE;
			case QUEST -> {
				String required = condition.getRequiredQuestId();
				yield required == null || required.isBlank() ? NONE : gate(required).after();
			}
			case LEVEL -> new Gate(-1, -1, condition.getMinLevel() != null ? condition.getMinLevel() : 0, 0);
			default -> NONE;
		};
	}

	private int sagaRank(String sagaId) {
		return sagaRanks.getOrDefault(sagaId, sagaRanks.size());
	}

	private record Gate(int saga, int step, int level, int depth) {
		static final Comparator<Gate> ORDER = Comparator.comparingInt(Gate::saga).thenComparingInt(Gate::step)
				.thenComparingInt(Gate::level).thenComparingInt(Gate::depth);

		Gate after() {
			return new Gate(saga, step, level, depth + 1);
		}

		static Gate all(List<Gate> gates) {
			Gate result = NONE;
			for (Gate gate : gates) {
				boolean later = gate.saga > result.saga || gate.saga == result.saga && gate.step > result.step;
				result = new Gate(later ? gate.saga : result.saga, later ? gate.step : result.step,
						Math.max(result.level, gate.level), Math.max(result.depth, gate.depth));
			}
			return result;
		}

		static Gate any(List<Gate> gates) {
			if (gates.isEmpty()) return NONE;
			Gate result = gates.get(0);
			for (Gate gate : gates) {
				boolean earlier = gate.saga < result.saga || gate.saga == result.saga && gate.step < result.step;
				result = new Gate(earlier ? gate.saga : result.saga, earlier ? gate.step : result.step,
						Math.min(result.level, gate.level), Math.min(result.depth, gate.depth));
			}
			return result;
		}
	}
}
