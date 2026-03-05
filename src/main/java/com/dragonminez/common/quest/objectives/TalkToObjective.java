package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import lombok.Getter;

/**
 * Objective that requires the player to talk to a specific quest NPC.
 * <p>
 * The {@code npcId} matches either a {@code QuestNPCEntity}'s synced npcId field
 * or a {@code MastersEntity}'s masterName (for backward compatibility with master NPCs).
 *
 * @since 2.0
 */
@Getter
public class TalkToObjective extends QuestObjective {
	private final String npcId;

	public TalkToObjective(String description, String npcId) {
		super(ObjectiveType.TALK_TO, description, 1);
		this.npcId = npcId;
	}

	@Override
	public boolean checkProgress(Object... params) {
		if (params.length > 0 && params[0] instanceof String interactedNpcId) {
			if (npcId != null && npcId.equals(interactedNpcId)) {
				setProgress(1);
				return true;
			}
		}
		return false;
	}
}

