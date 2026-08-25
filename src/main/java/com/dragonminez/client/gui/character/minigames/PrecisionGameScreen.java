package com.dragonminez.client.gui.character.minigames;

import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TrainingConfig;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class PrecisionGameScreen extends BaseMinigameScreen {

	private TrainingConfig.PrecisionConfig cfg;
	private final Random random = new Random();
	private final List<Circle> targets = new ArrayList<>();
	private final Deque<Boolean> outcomes = new ArrayDeque<>();

	private int score;
	private int nextThreshold;
	private int spawnTimer;

	public PrecisionGameScreen() {
		super("precision", "gui.dragonminez.minigame.precision");
	}

	@Override
	protected void init() {
		super.init();
		this.cfg = ConfigManager.getTrainingConfig().getPrecision();
		this.score = cfg.getStartingScore();
		this.nextThreshold = cfg.getStartingScore() + cfg.getLevelUpScoreBase();
		this.spawnTimer = 0;
		this.targets.clear();
		this.outcomes.clear();
	}

	private double ringSpeed() {
		return cfg.getBaseRingSpeed() + (levelsCleared * cfg.getRingSpeedPerLevel());
	}

	@Override
	protected void tickGame() {
		if (--spawnTimer <= 0 && countActive() < cfg.getMaxCircles()) {
			spawnCircle();
			if (countActive() < cfg.getMaxCircles() && random.nextDouble() < cfg.getBurstChance()) spawnCircle();
			spawnTimer = cfg.getSpawnIntervalTicks();
		}

		Iterator<Circle> it = targets.iterator();
		while (it.hasNext()) {
			Circle c = it.next();
			if (c.fading) {
				if (--c.fadeRemaining <= 0) it.remove();
				continue;
			}
			c.ringRadius -= (float) ringSpeed();
			if (c.ringRadius <= cfg.getTargetRadius() - cfg.getGoodWindow()) missCircle(c);
		}
	}

	private int countActive() {
		int n = 0;
		for (Circle c : targets) if (!c.fading) n++;
		return n;
	}

	private void spawnCircle() {
		int marginX = Math.max(50, this.width / 6);
		int marginY = Math.max(50, this.height / 6);
		int x = marginX + random.nextInt(Math.max(1, this.width - 2 * marginX));
		int y = marginY + random.nextInt(Math.max(1, this.height - 2 * marginY));
		targets.add(new Circle(x, y, cfg.getOuterRingRadius()));
	}

	@Override
	protected boolean onMouseClick(double mouseX, double mouseY, int button) {
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

		Circle target = null;
		double bestTiming = Double.MAX_VALUE;
		int half = cfg.getTargetRadius();
		for (Circle c : targets) {
			if (c.fading) continue;
			if (Math.abs(mouseX - c.x) <= half && Math.abs(mouseY - c.y) <= half) {
				double timing = Math.abs(c.ringRadius - cfg.getTargetRadius());
				if (timing < bestTiming) {
					bestTiming = timing;
					target = c;
				}
			}
		}

		if (target == null) return true;

		if (bestTiming <= cfg.getPerfectWindow()) {
			score += cfg.getPerfectPoints();
			playHit(true);
			targets.remove(target);
			recordOutcome(true);
			checkLevelUp();
		} else if (bestTiming <= cfg.getGoodWindow()) {
			score += cfg.getGoodPoints();
			playHit(false);
			targets.remove(target);
			recordOutcome(true);
			checkLevelUp();
		} else {
			missCircle(target);
		}
		return true;
	}

	private void missCircle(Circle c) {
		c.fading = true;
		c.fadeRemaining = cfg.getFadeOutTicks();
		score -= cfg.getMissPenalty();
		playMiss();
		recordOutcome(false);
		if (score < 0) endGame();
	}

	private void checkLevelUp() {
		if (score >= nextThreshold) {
			levelCleared();
			nextThreshold += cfg.getLevelUpScorePerLevel() * level();
		}
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
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.training.level", level()), cx, 64, 0xFFFFD700);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.training.score", score, nextThreshold), cx, 76, 0xFFFFFFFF);

		for (Circle c : targets) {
			if (c.fading) {
				int alpha = (int) (255 * ((float) c.fadeRemaining / cfg.getFadeOutTicks()));
				int red = (alpha << 24) | 0x00FF3030;
				drawCircle(graphics, c.x, c.y, cfg.getTargetRadius(), red);
				drawCircle(graphics, c.x, c.y, Math.max(1, (int) c.ringRadius), red);
			} else {
				drawCircle(graphics, c.x, c.y, cfg.getTargetRadius(), 0xFFFFD700);
				drawCircle(graphics, c.x, c.y, Math.max(1, (int) c.ringRadius), 0xFF7CFDD6);
			}
		}
	}

	private void drawCircle(GuiGraphics graphics, int cx, int cy, int radius, int color) {
		int segments = Math.max(16, radius * 3);
		for (int i = 0; i < segments; i++) {
			double angle = (Math.PI * 2 * i) / segments;
			int px = cx + (int) Math.round(Math.cos(angle) * radius);
			int py = cy + (int) Math.round(Math.sin(angle) * radius);
			graphics.fill(px, py, px + 2, py + 2, color);
		}
	}

	private static class Circle {
		final int x, y;
		float ringRadius;
		boolean fading;
		int fadeRemaining;

		Circle(int x, int y, float ringRadius) {
			this.x = x;
			this.y = y;
			this.ringRadius = ringRadius;
		}
	}
}
