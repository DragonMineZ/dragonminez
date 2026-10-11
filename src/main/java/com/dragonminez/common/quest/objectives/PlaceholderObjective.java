package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import lombok.Getter;

@Getter
public class PlaceholderObjective extends QuestObjective {

	private final String originalType;
	private final String reason;
	private final boolean typeKnown;

	public PlaceholderObjective(String originalType, String reason, boolean typeKnown) {
		super(ObjectiveType.PLACEHOLDER, 0);
		this.originalType = originalType;
		this.reason = reason;
		this.typeKnown = typeKnown;
	}

	@Override
	public boolean checkProgress(Object... params) {
		return false;
	}
}
