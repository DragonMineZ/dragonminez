package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import lombok.Getter;

@Getter
public class DragonSummonObjective extends QuestObjective {
	private final String dragonId;
	private final String ballSetId;

	public DragonSummonObjective(String dragonId, String ballSetId) {
		super(ObjectiveType.DRAGON_SUMMON, 1);
		this.dragonId = normalize(dragonId);
		this.ballSetId = normalize(ballSetId);
	}

	public boolean matches(String summonedDragonId, String summonedBallSetId) {
		String normalizedDragonId = normalize(summonedDragonId);
		String normalizedBallSetId = normalize(summonedBallSetId);
		return matchesField(dragonId, normalizedDragonId) && matchesField(ballSetId, normalizedBallSetId);
	}

	@Override
	public boolean checkProgress(Object... params) {
		if (params.length >= 2
				&& params[0] instanceof String summonedDragonId
				&& params[1] instanceof String summonedBallSetId
				&& matches(summonedDragonId, summonedBallSetId)) {
			setProgress(1);
			return true;
		}
		return false;
	}

	private static boolean matchesField(String expected, String actual) {
		return expected.isBlank() || expected.equals(actual);
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase();
	}
}
