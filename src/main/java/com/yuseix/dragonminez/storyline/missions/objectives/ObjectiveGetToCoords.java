package com.yuseix.dragonminez.storyline.missions.objectives;

import com.yuseix.dragonminez.storyline.missions.Objective;

public class ObjectiveGetToCoords extends Objective {
	private final int x;
	private final int y;
	private final int z;
	private final int radius;
	private boolean reached;

	public ObjectiveGetToCoords(int x, int y, int z, int radius) {

		super(false);

		this.x = x;
		this.y = y;
		this.z = z;
		this.radius = radius;
		this.reached = false;
	}

	public void onPlayerMove(int x, int y, int z) {

		if (Math.abs(this.x - x) <= radius && Math.abs(this.y - y) <= radius && Math.abs(this.z - z) <= radius) {
			reached = true;
			checkCompletion();
		}
	}

	@Override
	public void checkCompletion() {
		if (reached) {
			setCompleted(); // Mark the objective as complete
		}
	}
}
