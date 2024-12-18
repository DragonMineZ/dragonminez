package com.yuseix.dragonminez.storyline.missions.objectives;

import com.yuseix.dragonminez.storyline.missions.Objective;

public class ObjectiveKillEnemy extends Objective {
	private final String enemyName;
	private final int requiredAmount;
	private int currentAmount;

	public ObjectiveKillEnemy(String enemyName, int requiredAmount) {

		super(false);

		this.enemyName = enemyName;
		this.requiredAmount = requiredAmount;
		this.currentAmount = 0;
	}

	public void onEnemyKilled(String killedEnemy) {

		if (killedEnemy.equals(enemyName)) {
			currentAmount++;
			checkCompletion();
		}
	}

	@Override
	public void checkCompletion() {
		if (currentAmount >= requiredAmount) {
			setCompleted(); // Mark the objective as complete
		}
	}
}
