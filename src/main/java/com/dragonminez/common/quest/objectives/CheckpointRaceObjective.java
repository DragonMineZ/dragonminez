package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import net.minecraft.core.BlockPos;
import javax.annotation.Nullable;
import java.util.List;

public class CheckpointRaceObjective extends QuestObjective {
	private final List<BlockPos> checkpoints;
	private final int radius;
	@Nullable
	private final String dimension;

	public CheckpointRaceObjective(List<BlockPos> checkpoints, int radius) {
		this(checkpoints, radius, null);
	}

	public CheckpointRaceObjective(List<BlockPos> checkpoints, int radius, @Nullable String dimension) {
		super(ObjectiveType.CHECKPOINT_RACE, checkpoints.size());
		this.checkpoints = checkpoints;
		this.radius = Math.max(1, radius);
		this.dimension = dimension == null || dimension.isBlank() ? null : dimension.trim();
	}

	/**
	 * The checkpoint the player must reach next, or null when all are passed.
	 */
	public BlockPos getCheckpoint(int index) {
		return index >= 0 && index < checkpoints.size() ? checkpoints.get(index) : null;
	}

	@Override
	public boolean checkProgress(Object... params) {
		return isCompleted();
	}

	public List<BlockPos> getCheckpoints() {
		return this.checkpoints;
	}

	public int getRadius() {
		return this.radius;
	}

	@Nullable
	public String getDimension() {
		return this.dimension;
	}
}
