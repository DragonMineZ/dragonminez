package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.common.training.MinigameLogics;
import com.dragonminez.common.training.MinigameOrigin;
import net.minecraft.client.Minecraft;

public class UltimateChallenge {

	private int index = 0;

	public void start() {
		index = 0;
		openCurrent();
	}

	public void onStageCleared() {
		index++;
		if (index >= MinigameLogics.CHALLENGE_STAGES.size()) {
			Minecraft.getInstance().setScreen(null);
			return;
		}
		openCurrent();
	}

	public void onCompleted() {
		Minecraft.getInstance().setScreen(null);
	}

	public void restart() {
		start();
	}

	private void openCurrent() {
		BaseMinigameScreen screen = BaseMinigameScreen.create(MinigameLogics.CHALLENGE_STAGES.get(index), MinigameOrigin.CHALLENGE);
		screen.setChallenge(this, index);
		Minecraft.getInstance().setScreen(screen);
	}
}
