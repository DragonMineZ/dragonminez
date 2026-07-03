package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import lombok.Getter;
import net.minecraft.core.BlockPos;

import java.util.List;

@Getter
public class CheckpointRaceObjective extends QuestObjective {

	private final List<BlockPos> checkpoints;
	private final int radius;

	public CheckpointRaceObjective(List<BlockPos> checkpoints, int radius) {
		super(ObjectiveType.CHECKPOINT_RACE, checkpoints.size());
		this.checkpoints = checkpoints;
		this.radius = Math.max(1, radius);
	}

	/** The checkpoint the player must reach next, or null when all are passed. */
	public BlockPos getCheckpoint(int index) {
		return index >= 0 && index < checkpoints.size() ? checkpoints.get(index) : null;
	}

	@Override
	public boolean checkProgress(Object... params) {
		return isCompleted();
	}
}
