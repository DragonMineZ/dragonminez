package com.dragonminez.common.training;

import com.dragonminez.common.config.TrainingConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GravityLogic extends MinigameLogic {
	public static final String ID = "gravity";
	public static final int VIEW_WIDTH = 560;
	public static final int VIEW_HEIGHT = 180;
	public static final int GROUND_HEIGHT = 22;
	public static final int PLAYER_X = 120;

	public static final int POD_SPRITE_SIZE = 22;
	public static final float POD_HITBOX_RADIUS = 8.0f;

	public static final int BODY_WIDTH = 26;
	public static final int BODY_TILE_HEIGHT = 16;
	public static final int CAP_WIDTH = 38;
	public static final int CAP_HEIGHT = 18;
	private static final float BODY_INSET = 2f;
	private static final float CAP_INSET_X = 3f;
	private static final float CAP_INSET_EDGE = 3f;

	private static final double PERFORMANCE_WEIGHT = 0.2;

	private final TrainingConfig.GravityConfig cfg;
	private final List<Pillar> pillars = new ArrayList<>();

	private double worldX;
	private double podY = (VIEW_HEIGHT - GROUND_HEIGHT) / 2.0;
	private double velocity;
	private int lives;
	private int invulnerable;
	private int flapTick = -100;
	private int passedThisLevel;
	private int totalPassed;
	private double nextPillarX;
	private double lastGapCenter = (VIEW_HEIGHT - GROUND_HEIGHT) / 2.0;

	public GravityLogic(TrainingConfig config, long seed) {
		super(config, config.getGravity(), seed);
		this.cfg = config.getGravity();
		this.lives = Math.max(1, cfg.getLives());
		this.nextPillarX = VIEW_WIDTH + 40;
		fillPillars();
	}

	@Override
	public String id() {
		return ID;
	}

	public double scrollSpeed() {
		return cfg.getBaseScrollSpeed() + difficulty() * cfg.getScrollSpeedPerLevel();
	}

	public double gapSize() {
		return Math.max(cfg.getMinGap(), cfg.getBaseGap() - difficulty() * cfg.getGapDecreasePerLevel());
	}

	private double spacing() {
		return Math.max(cfg.getMinSpacing(), cfg.getBaseSpacing() - difficulty() * cfg.getSpacingDecreasePerLevel());
	}

	private double maxGapShift() {
		return cfg.getBaseMaxGapShift() + difficulty() * cfg.getMaxGapShiftPerLevel();
	}

	private void fillPillars() {
		double playable = VIEW_HEIGHT - GROUND_HEIGHT;
		while (nextPillarX < worldX + VIEW_WIDTH + 80) {
			double gap = gapSize();
			double margin = CAP_HEIGHT + 8 + gap / 2.0;
			double shift = maxGapShift();
			double lo = Math.max(margin, lastGapCenter - shift);
			double hi = Math.min(playable - margin, lastGapCenter + shift);
			double center = lo + random.nextDouble() * Math.max(0.0, hi - lo);
			pillars.add(new Pillar(nextPillarX, center, gap, random.nextInt(4)));
			lastGapCenter = center;
			nextPillarX += spacing() + random.nextDouble() * cfg.getSpacingRandom();
		}
	}

	@Override
	protected void onKeyDown(int key, float frac) {
		if (key != MinigameEvent.ACTION) return;
		velocity = -cfg.getFlapVelocity();
		flapTick = tickCount();
	}

	@Override
	protected void tickGame() {
		if (invulnerable > 0) invulnerable--;

		velocity = Math.min(cfg.getMaxFallSpeed(), velocity + cfg.getGravityAcceleration());
		podY += velocity;
		if (podY < POD_HITBOX_RADIUS) {
			podY = POD_HITBOX_RADIUS;
			velocity = Math.max(0, velocity);
		}

		double groundTop = VIEW_HEIGHT - GROUND_HEIGHT;
		if (podY + POD_HITBOX_RADIUS > groundTop) {
			podY = groundTop - POD_HITBOX_RADIUS;
			velocity = -cfg.getFlapVelocity();
			hit();
			if (isFinished()) return;
		}

		worldX += scrollSpeed();
		double podWorldX = worldX + PLAYER_X;
		Iterator<Pillar> it = pillars.iterator();
		while (it.hasNext() && !isFinished()) {
			Pillar pillar = it.next();
			if (!pillar.hit && invulnerable <= 0 && collides(pillar, podWorldX, podY)) {
				pillar.hit = true;
				hit();
				if (isFinished()) return;
			}
			if (!pillar.passed && pillar.x + CAP_WIDTH / 2.0 < podWorldX - POD_HITBOX_RADIUS) {
				pillar.passed = true;
				samplePerformance(pillar.hit ? 0.0 : 1.0, PERFORMANCE_WEIGHT);
				totalPassed++;
				if (++passedThisLevel >= Math.max(1, cfg.getPillarsPerLevel())) levelUp();
			}
			if (pillar.x + CAP_WIDTH < worldX - 20) it.remove();
		}
		if (!isFinished()) fillPillars();
	}

	private void hit() {
		if (invulnerable > 0) return;
		lives--;
		invulnerable = cfg.getInvulnerabilityTicks();
		samplePerformance(0.0, PERFORMANCE_WEIGHT);
		if (lives <= 0) fail();
	}

	public static boolean collides(Pillar pillar, double podWorldX, double podY) {
		double gapTop = pillar.gapTop();
		double gapBottom = pillar.gapBottom();
		double bodyHalf = BODY_WIDTH / 2.0 - BODY_INSET;
		double capHalf = CAP_WIDTH / 2.0 - CAP_INSET_X;
		double groundTop = VIEW_HEIGHT - GROUND_HEIGHT;
		return circleRect(podWorldX, podY, pillar.x - capHalf, gapBottom + CAP_INSET_EDGE, pillar.x + capHalf, gapBottom + CAP_HEIGHT)
				|| circleRect(podWorldX, podY, pillar.x - bodyHalf, gapBottom + CAP_HEIGHT, pillar.x + bodyHalf, groundTop)
				|| circleRect(podWorldX, podY, pillar.x - capHalf, gapTop - CAP_HEIGHT, pillar.x + capHalf, gapTop - CAP_INSET_EDGE)
				|| circleRect(podWorldX, podY, pillar.x - bodyHalf, -40, pillar.x + bodyHalf, gapTop - CAP_HEIGHT);
	}

	private static boolean circleRect(double cx, double cy, double x0, double y0, double x1, double y1) {
		if (x1 <= x0 || y1 <= y0) return false;
		double nx = Math.max(x0, Math.min(cx, x1));
		double ny = Math.max(y0, Math.min(cy, y1));
		double dx = cx - nx;
		double dy = cy - ny;
		return dx * dx + dy * dy < POD_HITBOX_RADIUS * POD_HITBOX_RADIUS;
	}

	@Override
	protected void onLevelUp() {
		passedThisLevel = 0;
	}

	public List<Pillar> pillars() {
		return Collections.unmodifiableList(pillars);
	}

	public double worldX() {
		return worldX;
	}

	public double podY() {
		return podY;
	}

	public double velocity() {
		return velocity;
	}

	public int lives() {
		return lives;
	}

	public int invulnerableTicks() {
		return invulnerable;
	}

	public int ticksSinceFlap() {
		return tickCount() - flapTick;
	}

	public int passedThisLevel() {
		return passedThisLevel;
	}

	public int totalPassed() {
		return totalPassed;
	}

	public TrainingConfig.GravityConfig cfg() {
		return cfg;
	}

	@Override
	protected long gameChecksum() {
		return Math.round(worldX * 100.0) * 31L + Math.round(podY * 100.0) * 17L + lives * 3L + pillars.size();
	}

	public static final class Pillar {
		public final double x;
		public final double gapCenter;
		public final double gap;
		public final int variant;
		public boolean hit;
		public boolean passed;

		Pillar(double x, double gapCenter, double gap, int variant) {
			this.x = x;
			this.gapCenter = gapCenter;
			this.gap = gap;
			this.variant = variant;
		}

		public double gapTop() {
			return gapCenter - gap / 2.0;
		}

		public double gapBottom() {
			return gapCenter + gap / 2.0;
		}
	}
}
