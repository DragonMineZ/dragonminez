package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.common.network.C2S.NPCActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Supplier;

public class UltimateChallenge {

	private static final int TARGET_LEVEL = 5;

	private final List<Supplier<BaseMinigameScreen>> stages = List.of(
			ControlGameScreen::new,
			GravityGameScreen::new,
			MemoryGameScreen::new,
			PrecisionGameScreen::new,
			RythmGameScreen::new
	);

	private int index = 0;

	public int targetLevel() {
		return TARGET_LEVEL;
	}

	public void start() {
		index = 0;
		openCurrent();
	}

	public void onStageComplete() {
		index++;
		if (index >= stages.size()) {
			NetworkHandler.sendToServer(new NPCActionC2S("oldkai", 1));
			Minecraft.getInstance().setScreen(null);
			return;
		}
		openCurrent();
	}

	public void onFail() {
		index = 0;
		openCurrent();
	}

	private void openCurrent() {
		BaseMinigameScreen screen = stages.get(index).get();
		screen.setChallenge(this);
		Minecraft.getInstance().setScreen(screen);
	}
}
