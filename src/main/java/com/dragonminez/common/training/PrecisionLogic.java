package com.dragonminez.common.training;

import com.dragonminez.common.config.TrainingConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public class PrecisionLogic extends MinigameLogic {
	public static final String ID = "precision";
	public static final int FIELD_WIDTH = 480;
	public static final int FIELD_HEIGHT = 270;

	private static final double PERFORMANCE_WEIGHT = 0.08;

	private final TrainingConfig.PrecisionConfig cfg;
	private final List<Circle> targets = new ArrayList<>();
	private final Deque<Boolean> outcomes = new ArrayDeque<>();

	private int score;
	private int levelStartScore;
	private int nextThreshold;
	private int spawnTimer;

	public PrecisionLogic(TrainingConfig config, long seed) {
		super(config, config.getPrecision(), seed);
		this.cfg = config.getPrecision();
		this.score = cfg.getStartingScore();
		this.levelStartScore = score;
		this.nextThreshold = score + Math.max(1, cfg.getScorePerLevel());
	}

	@Override
	public String id() {
		return ID;
	}

	public double ringSpeed() {
		return cfg.getBaseRingSpeed() + difficulty() * cfg.getRingSpeedPerLevel();
	}

	@Override
	protected void tickGame() {
		if (--spawnTimer <= 0 && countActive() < cfg.getMaxCircles()) {
			spawnCircle();
			if (countActive() < cfg.getMaxCircles() && random.nextDouble() < cfg.getBurstChance()) spawnCircle();
			spawnTimer = cfg.getSpawnIntervalTicks();
		}

		double speed = ringSpeed();
		Iterator<Circle> it = targets.iterator();
		while (it.hasNext() && !isFinished()) {
			Circle c = it.next();
			if (c.fading) {
				if (--c.fadeRemaining <= 0) it.remove();
				continue;
			}
			c.ringRadius -= (float) speed;
			if (c.ringRadius <= cfg.getTargetRadius() - cfg.getGoodWindow()) missCircle(c);
		}
	}

	private int countActive() {
		int n = 0;
		for (Circle c : targets) if (!c.fading) n++;
		return n;
	}

	private void spawnCircle() {
		int marginX = Math.max(50, FIELD_WIDTH / 6);
		int marginY = Math.max(50, FIELD_HEIGHT / 6);
		int x = marginX + random.nextInt(Math.max(1, FIELD_WIDTH - 2 * marginX));
		int y = marginY + random.nextInt(Math.max(1, FIELD_HEIGHT - 2 * marginY));
		targets.add(new Circle(x, y, cfg.getOuterRingRadius()));
	}

	@Override
	protected void onClick(float mouseX, float mouseY, float frac) {
		Circle target = null;
		double bestTiming = Double.MAX_VALUE;
		int half = cfg.getTargetRadius();
		double speed = ringSpeed();
		for (Circle c : targets) {
			if (c.fading) continue;
			if (Math.abs(mouseX - c.x) <= half && Math.abs(mouseY - c.y) <= half) {
				double ring = c.ringRadius - speed * frac;
				double timing = Math.abs(ring - cfg.getTargetRadius());
				if (timing < bestTiming) {
					bestTiming = timing;
					target = c;
				}
			}
		}
		if (target == null) return;

		if (bestTiming <= cfg.getPerfectWindow()) {
			hit(target, cfg.getPerfectPoints(), 1.0);
		} else if (bestTiming <= cfg.getGoodWindow()) {
			hit(target, cfg.getGoodPoints(), 0.6);
		} else {
			missCircle(target);
		}
	}

	private void hit(Circle c, int points, double quality) {
		score += points;
		targets.remove(c);
		record(true, quality);
		if (!isFinished() && score >= nextThreshold) levelUp();
	}

	private void missCircle(Circle c) {
		c.fading = true;
		c.fadeRemaining = cfg.getFadeOutTicks();
		score -= cfg.getMissPenalty();
		record(false, 0.0);
		if (score < 0) fail();
	}

	@Override
	protected void onLevelUp() {
		levelStartScore = nextThreshold;
		nextThreshold += Math.max(1, cfg.getScorePerLevel());
	}

	private void record(boolean hit, double quality) {
		samplePerformance(quality, PERFORMANCE_WEIGHT);
		outcomes.addLast(hit);
		while (outcomes.size() > cfg.getLoseMissWindow()) outcomes.removeFirst();
		int misses = 0;
		for (boolean o : outcomes) if (!o) misses++;
		if (misses >= cfg.getLoseMissThreshold()) fail();
	}

	public List<Circle> targets() {
		return Collections.unmodifiableList(targets);
	}

	public int score() {
		return score;
	}

	public int levelStartScore() {
		return levelStartScore;
	}

	public int nextThreshold() {
		return nextThreshold;
	}

	public TrainingConfig.PrecisionConfig cfg() {
		return cfg;
	}

	@Override
	protected long gameChecksum() {
		return score * 131L + targets.size();
	}

	public static final class Circle {
		public final int x;
		public final int y;
		public float ringRadius;
		public boolean fading;
		public int fadeRemaining;

		Circle(int x, int y, float ringRadius) {
			this.x = x;
			this.y = y;
			this.ringRadius = ringRadius;
		}
	}
}
