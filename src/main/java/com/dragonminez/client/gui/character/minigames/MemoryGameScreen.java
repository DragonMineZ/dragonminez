package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TrainingConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MemoryGameScreen extends BaseMinigameScreen {

	private enum Direction {
		LEFT("←", 0xFF55FFFF),
		DOWN("↓", 0xFF55FF55),
		UP("↑", 0xFFFFFF55),
		RIGHT("→", 0xFFFF55FF);

		final String symbol;
		final int color;

		Direction(String symbol, int color) {
			this.symbol = symbol;
			this.color = color;
		}
	}

	private enum Phase { SHOWING, INPUT }

	private TrainingConfig.MemoryConfig cfg;
	private final Random random = new Random();
	private final List<Direction> pattern = new ArrayList<>();

	private Phase memPhase = Phase.SHOWING;
	private int showTimer;
	private int patternIndex;

	public MemoryGameScreen() {
		super("memory", "gui.dragonminez.minigame.memory");
	}

	@Override
	protected void init() {
		super.init();
		this.cfg = ConfigManager.getTrainingConfig().getMemory();
		buildSequence();
	}

	private int sequenceLength() {
		return cfg.getBaseSequenceLength() + ((levelsCleared / 2) * cfg.getSequenceLengthPerLevel());
	}

	private int showTicks() {
		return Math.max(cfg.getMinShowTicks(), cfg.getBaseShowTicks() - (levelsCleared * cfg.getShowTicksDecreasePerLevel()));
	}

	private void buildSequence() {
		pattern.clear();
		int len = sequenceLength();
		Direction[] all = Direction.values();
		for (int i = 0; i < len; i++) pattern.add(all[random.nextInt(all.length)]);
		memPhase = Phase.SHOWING;
		showTimer = showTicks();
		patternIndex = 0;
	}

	@Override
	protected void tickGame() {
		if (memPhase == Phase.SHOWING) {
			showTimer--;
			if (showTimer <= 0) memPhase = Phase.INPUT;
		}
	}

	@Override
	protected boolean onKey(int keyCode) {
		if (memPhase != Phase.INPUT) return false;
		Direction dir = directionForKey(keyCode);
		if (dir == null) return false;

		if (dir == pattern.get(patternIndex)) {
			patternIndex++;
			playHit(false);
			if (patternIndex >= pattern.size()) levelCleared();
		} else {
			playMiss();
			endGame();
		}
		return true;
	}

	private Direction directionForKey(int keyCode) {
		if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == KeyBinds.RHYTHM_LEFT.getKey().getValue()) return Direction.LEFT;
		if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == KeyBinds.RHYTHM_DOWN.getKey().getValue()) return Direction.DOWN;
		if (keyCode == GLFW.GLFW_KEY_UP || keyCode == KeyBinds.RHYTHM_UP.getKey().getValue()) return Direction.UP;
		if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == KeyBinds.RHYTHM_RIGHT.getKey().getValue()) return Direction.RIGHT;
		return null;
	}

	@Override
	protected void onLevelCleared() {
		buildSequence();
	}

	@Override
	protected void renderGame(GuiGraphics graphics) {
		int cx = this.width / 2;
		int cy = this.height / 2;

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.training.level", level()), cx, cy - 50, 0xFFFFD700);
		Component prompt = memPhase == Phase.SHOWING ? tr("gui.dragonminez.minigame.memory.memorize") : tr("gui.dragonminez.minigame.memory.repeat");
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, prompt, cx, cy - 36, 0xFFFFFFFF);

		int spacing = 26;
		int rowSpacing = 28;
		int maxRowWidth = (int) (this.width * 0.75f);
		int perRow = Math.max(1, maxRowWidth / spacing);
		int rowCount = (int) Math.ceil((double) pattern.size() / perRow);
		int gridTop = cy - ((rowCount - 1) * rowSpacing) / 2;

		for (int i = 0; i < pattern.size(); i++) {
			int row = i / perRow;
			int col = i % perRow;
			int notesInRow = Math.min(perRow, pattern.size() - row * perRow);
			int startX = cx - ((notesInRow - 1) * spacing) / 2;
			int x = startX + col * spacing;
			int y = gridTop + row * rowSpacing;
			Direction dir = pattern.get(i);
			if (memPhase == Phase.SHOWING) {
				drawArrow(graphics, x, y, dir.symbol, dir.color);
			} else {
				if (i < patternIndex) drawArrow(graphics, x, y, dir.symbol, 0xFF55FF55);
				else drawArrow(graphics, x, y, "?", 0xFF777777);
			}
		}

		if (memPhase == Phase.SHOWING) {
			int pbY = gridTop + (rowCount - 1) * rowSpacing + 30;
			int pbLeft = cx - 60, pbRight = cx + 60;
			graphics.fill(pbLeft, pbY, pbRight, pbY + 4, 0xFF333333);
			float pct = Math.min(1.0f, (float) showTimer / showTicks());
			graphics.fill(pbLeft, pbY, pbLeft + (int) ((pbRight - pbLeft) * pct), pbY + 4, 0xFFFFD700);
		}
	}

	private void drawArrow(GuiGraphics graphics, int x, int y, String symbol, int color) {
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(2.0f, 2.0f, 1.0f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, Component.literal(symbol), 0, -4, color);
		graphics.pose().popPose();
	}
}
