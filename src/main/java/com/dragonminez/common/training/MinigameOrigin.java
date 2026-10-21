package com.dragonminez.common.training;

public enum MinigameOrigin {
	MENU,
	MASTER,
	CHALLENGE,
	COMMAND;

	public static MinigameOrigin byId(int id) {
		MinigameOrigin[] values = values();
		return id >= 0 && id < values.length ? values[id] : MENU;
	}
}
