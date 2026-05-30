package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TrainingConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class RythmGameScreen extends BaseMinigameScreen {

	private enum Direction {
		LEFT("←", 0xFF55FFFF, GLFW.GLFW_KEY_LEFT),
		DOWN("↓", 0xFF55FF55, GLFW.GLFW_KEY_DOWN),
		UP("↑", 0xFFFFFF55, GLFW.GLFW_KEY_UP),
		RIGHT("→", 0xFFFF55FF, GLFW.GLFW_KEY_RIGHT);

		final String symbol;
		final int color;
		final int arrowKey;

		Direction(String symbol, int color, int arrowKey) {
			this.symbol = symbol;
			this.color = color;
			this.arrowKey = arrowKey;
		}
	}

	private final Random random = new Random();
	private final List<Note> notes = new ArrayList<>();
	private final Deque<Boolean> outcomes = new ArrayDeque<>();
	private final Set<Direction> holdLockedDirections = new HashSet<>();
	private final Set<Integer> downKeys = new HashSet<>();

	private TrainingConfig.RhythmConfig cfg;

	private double progress = 0;
	private float spawnTimer = 0;
	private boolean spawnLeftLane = true;

	private int lineY;
	private int leftTargetX;
	private int rightTargetX;

	public RythmGameScreen() {
		super("rhythm", "gui.dragonminez.minigame.rhythm");
	}

	@Override
	protected void init() {
		super.init();
		this.cfg = ConfigManager.getTrainingConfig().getRhythm();
		this.lineY = this.height / 2;
		this.leftTargetX = this.width / 2 - 40;
		this.rightTargetX = this.width / 2 + 40;
		this.progress = 0;
		this.spawnTimer = 0;
		this.notes.clear();
		this.outcomes.clear();
		this.holdLockedDirections.clear();
		this.downKeys.clear();
	}

	private float noteSpeed() {
		return (float) (cfg.getBaseNoteSpeed() + (levelsCleared * cfg.getNoteSpeedPerLevel()));
	}

	private int spawnInterval() {
		int interval = cfg.getBaseSpawnIntervalTicks() - (levelsCleared * cfg.getSpawnIntervalDecreasePerLevel());
		return Math.max(cfg.getMinSpawnIntervalTicks(), interval);
	}

	@Override
	protected void tickGame() {
		progress = Math.max(0, progress - cfg.getProgressDecayPerTick());

		if (spawnTimer-- <= 0) {
			spawnNote();
			spawnTimer = spawnInterval();
		}

		float movement = noteSpeed();
		int goodWindow = cfg.getGoodWindow();
		long window = Minecraft.getInstance().getWindow().getWindow();

		Iterator<Note> it = notes.iterator();
		while (it.hasNext()) {
			Note note = it.next();
			int targetX = note.leftLane ? leftTargetX : rightTargetX;

			if (note.activated) {
				if (isHeld(window, note.direction)) {
					note.holdRemaining--;
					if (note.holdRemaining <= 0) {
						it.remove();
						holdLockedDirections.remove(note.direction);
						progress += cfg.getProgressGainHold();
						clampProgress();
						recordOutcome(true);
						playHit(true);
					}
				} else {
					it.remove();
					holdLockedDirections.remove(note.direction);
					handleMiss(true);
				}
				continue;
			}

			if (note.leftLane) note.x += movement;
			else note.x -= movement;

			boolean passed = note.leftLane ? note.x > targetX + goodWindow : note.x < targetX - goodWindow;
			if (passed) {
				it.remove();
				holdLockedDirections.remove(note.direction);
				handleMiss(true);
			}
		}
	}

	private void spawnNote() {
		boolean leftLane = spawnLeftLane;
		spawnLeftLane = !spawnLeftLane;

		if (laneHasHold(leftLane)) {
			if (!laneHasHold(!leftLane)) leftLane = !leftLane;
			else return;
		}

		boolean wantHold = random.nextDouble() < cfg.getHoldNoteChance();
		Direction dir = randomDirUnlocked();
		if (dir == null) return;
		addNote(leftLane, dir, 0, wantHold);

		if (!wantHold && random.nextDouble() < cfg.getDoubleNoteChance()) {
			Direction doubleDir = randomDirUnlocked();
			if (doubleDir != null) addNote(leftLane, doubleDir, cfg.getDoubleNoteGap(), false);
		}
	}

	private boolean laneHasHold(boolean leftLane) {
		for (Note n : notes) if (n.isHold && n.leftLane == leftLane) return true;
		return false;
	}

	private Direction randomDirUnlocked() {
		Direction[] all = Direction.values();
		int start = random.nextInt(all.length);
		for (int i = 0; i < all.length; i++) {
			Direction d = all[(start + i) % all.length];
			if (!holdLockedDirections.contains(d)) return d;
		}
		return null;
	}

	private void addNote(boolean leftLane, Direction dir, int behindOffset, boolean isHold) {
		int travel = cfg.getNoteTravelDistance();
		float baseX = leftLane ? leftTargetX - travel : rightTargetX + travel;
		float startX = leftLane ? baseX - behindOffset : baseX + behindOffset;
		notes.add(new Note(startX, leftLane, dir, isHold, cfg.getHoldDurationTicks()));
		if (isHold) holdLockedDirections.add(dir);
	}

	private void clampProgress() {
		if (progress >= cfg.getProgressMax()) {
			levelCleared();
		}
	}

	@Override
	protected void onLevelCleared() {
		progress = cfg.getProgressOnLevelUp();
	}

	private boolean isHeld(long window, Direction dir) {
		if (GLFW.glfwGetKey(window, dir.arrowKey) == GLFW.GLFW_PRESS) return true;
		int mapped = mappedKey(dir);
		return mapped > 0 && GLFW.glfwGetKey(window, mapped) == GLFW.GLFW_PRESS;
	}

	private int mappedKey(Direction dir) {
		return switch (dir) {
			case LEFT -> KeyBinds.RHYTHM_LEFT.getKey().getValue();
			case DOWN -> KeyBinds.RHYTHM_DOWN.getKey().getValue();
			case UP -> KeyBinds.RHYTHM_UP.getKey().getValue();
			case RIGHT -> KeyBinds.RHYTHM_RIGHT.getKey().getValue();
		};
	}

	@Override
	protected boolean onKey(int keyCode) {
		Direction dir = directionForKey(keyCode);
		if (dir == null) return false;
		if (!downKeys.add(keyCode)) return true;
		checkInput(dir);
		return true;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		downKeys.remove(keyCode);
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	private Direction directionForKey(int keyCode) {
		for (Direction d : Direction.values()) {
			if (keyCode == d.arrowKey || keyCode == mappedKey(d)) return d;
		}
		return null;
	}

	private void checkInput(Direction dir) {
		Note best = null;
		double minDist = Double.MAX_VALUE;

		for (Note note : notes) {
			if (note.activated || note.direction != dir) continue;
			int targetX = note.leftLane ? leftTargetX : rightTargetX;
			double dist = Math.abs(note.x - targetX);
			if (dist < cfg.getGoodWindow() && dist < minDist) {
				minDist = dist;
				best = note;
			}
		}

		if (best == null) {
			progress = Math.max(0, progress - cfg.getProgressLossOnMiss());
			playMiss();
			return;
		}

		boolean perfect = minDist <= cfg.getPerfectWindow();
		progress += perfect ? cfg.getProgressGainPerfect() : cfg.getProgressGainGood();
		playHit(perfect);

		if (best.isHold) {
			best.activated = true;
			best.x = best.leftLane ? leftTargetX : rightTargetX;
			best.holdRemaining = best.holdTicksTotal;
			recordOutcome(true);
		} else {
			holdLockedDirections.remove(best.direction);
			notes.remove(best);
			recordOutcome(true);
		}
		clampProgress();
	}

	private void handleMiss(boolean countNote) {
		progress = Math.max(0, progress - cfg.getProgressLossOnMiss());
		playMiss();
		if (countNote) recordOutcome(false);
	}

	private void recordOutcome(boolean hit) {
		outcomes.addLast(hit);
		while (outcomes.size() > cfg.getLoseMissWindow()) outcomes.removeFirst();
		int misses = 0;
		for (boolean o : outcomes) if (!o) misses++;
		if (misses >= cfg.getLoseMissThreshold()) endGame();
	}

	@Override
	protected void renderGame(GuiGraphics graphics) {
		int cx = this.width / 2;
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.training.level", level()), cx, lineY - 64, 0xFFFFD700);

		int pbLeft = cx - 90, pbRight = cx + 90, pbY = lineY - 50;
		graphics.fill(pbLeft, pbY, pbRight, pbY + 8, 0xFF333333);
		float pct = (float) Math.min(1.0, progress / cfg.getProgressMax());
		graphics.fill(pbLeft, pbY, pbLeft + (int) ((pbRight - pbLeft) * pct), pbY + 8, 0xFF55FF55);
		graphics.renderOutline(pbLeft - 1, pbY - 1, (pbRight - pbLeft) + 2, 10, 0xFFFFFFFF);

		int travel = cfg.getNoteTravelDistance();
		int leftEnd = leftTargetX - travel - 10;
		int rightEnd = rightTargetX + travel + 10;

		graphics.fill(leftEnd, lineY - 1, leftTargetX - 14, lineY + 1, 0x40FFFFFF);
		graphics.fill(rightTargetX + 14, lineY - 1, rightEnd, lineY + 1, 0x40FFFFFF);

		drawTarget(graphics, leftTargetX);
		drawTarget(graphics, rightTargetX);

		float speed = noteSpeed();
		for (Note note : notes) {
			if (note.isHold) drawHoldTail(graphics, note, speed);
			drawArrow(graphics, (int) note.x, lineY, note.direction.symbol, note.direction.color);
		}
	}

	private void drawHoldTail(GuiGraphics graphics, Note note, float speed) {
		int remainingTicks = note.activated ? note.holdRemaining : note.holdTicksTotal;
		int tailLen = (int) (remainingTicks * speed);
		int headX = (int) note.x;
		int color = (note.direction.color & 0x00FFFFFF) | 0x80000000;
		if (note.leftLane) graphics.fill(headX - tailLen, lineY - 4, headX, lineY + 4, color);
		else graphics.fill(headX, lineY - 4, headX + tailLen, lineY + 4, color);
	}

	private void drawTarget(GuiGraphics graphics, int x) {
		graphics.fill(x - 1, lineY - 18, x + 1, lineY + 18, 0x90FFFFFF);
		graphics.renderOutline(x - 14, lineY - 14, 28, 28, 0x80FFFFFF);
	}

	private void drawArrow(GuiGraphics graphics, int x, int y, String symbol, int color) {
		float scale = cfg.getArrowScale();
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(scale, scale, 1.0f);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, Component.literal(symbol), 0, -4, color);
		graphics.pose().popPose();
	}

	private static class Note {
		float x;
		final boolean leftLane;
		final Direction direction;
		final boolean isHold;
		final int holdTicksTotal;
		boolean activated;
		int holdRemaining;

		Note(float x, boolean leftLane, Direction direction, boolean isHold, int holdTicksTotal) {
			this.x = x;
			this.leftLane = leftLane;
			this.direction = direction;
			this.isHold = isHold;
			this.holdTicksTotal = holdTicksTotal;
		}
	}
}
