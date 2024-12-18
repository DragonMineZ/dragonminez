package com.yuseix.dragonminez.storyline.missions;

public abstract class Objective {

	private boolean completed;
	private Runnable onCompletion;

	public Objective(boolean completed) {

		this.completed = completed;
	}

	public boolean isCompleted() {
		return this.completed; // Check completion status
	}


	public void setCompleted() {
		this.completed = true; // Update completion status

		if (onCompletion != null) {
			onCompletion.run();
		}
	}

	public void setOnCompletion(Runnable onCompletion) {
		this.onCompletion = onCompletion;
	}

	public abstract void checkCompletion();
}
