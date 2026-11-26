package com.dragonminez.common.training;

import com.dragonminez.common.config.TrainingConfig;

public class KiControlLogic extends MinigameLogic {
	public static final String ID = "control";

	private static final double PERFORMANCE_WEIGHT = 0.01;

	private final TrainingConfig.ControlConfig cfg;
	private final double trackHeight;

	private double barY;
	private double barVelocity;
	private double orbY;
	private double orbVelocity;
	private double orbTarget;
	private int retargetTimer;
	private boolean darting;
	private double progress;
	private boolean inside;

	public KiControlLogic(TrainingConfig config, long seed) {
		super(config, config.getControl(), seed);
		this.cfg = config.getControl();
		this.trackHeight = Math.max(60, cfg.getTrackHeight());
		this.barY = 0;
		this.orbY = barHeight() * 0.5;
		this.orbTarget = orbY;
		this.retargetTimer = cfg.getMinRetargetTicks();
		this.progress = cfg.getInitialProgress();
		this.inside = true;
	}

	@Override
	public String id() {
		return ID;
	}

	public double barHeight() {
		return Math.max(cfg.getMinBarHeight(), cfg.getBaseBarHeight() - difficulty() * cfg.getBarHeightDecreasePerLevel());
	}

	private double orbSpeed() {
		return cfg.getBaseOrbSpeed() + difficulty() * cfg.getOrbSpeedPerLevel();
	}

	private double dartChance() {
		return cfg.getBaseDartChance() + difficulty() * cfg.getDartChancePerLevel();
	}

	@Override
	protected void tickGame() {
		tickBar();
		tickOrb();

		double h = barHeight();
		inside = orbY >= barY && orbY <= barY + h;
		samplePerformance(inside ? 1.0 : 0.0, PERFORMANCE_WEIGHT);
		if (inside) {
			progress += cfg.getProgressFillPerTick();
			if (progress >= 1.0) levelUp();
		} else {
			progress -= cfg.getBaseProgressDrainPerTick() + difficulty() * cfg.getProgressDrainPerLevel();
			if (progress <= 0.0) {
				progress = 0.0;
				fail();
			}
		}
	}

	private void tickBar() {
		double h = barHeight();
		if (isHeld(MinigameEvent.ACTION)) barVelocity += cfg.getLiftAcceleration();
		else barVelocity -= cfg.getFallAcceleration();
		double max = cfg.getMaxBarSpeed();
		barVelocity = Math.max(-max, Math.min(max, barVelocity));
		barY += barVelocity;
		if (barY < 0) {
			barY = 0;
			barVelocity = barVelocity < 0 ? -barVelocity * cfg.getBounceDamping() : barVelocity;
			if (barVelocity < 0.4) barVelocity = 0;
		} else if (barY + h > trackHeight) {
			barY = trackHeight - h;
			if (barVelocity > 0) barVelocity = -barVelocity * cfg.getBounceDamping();
		}
	}

	private void tickOrb() {
		double half = cfg.getOrbSize() / 2.0;
		double min = half;
		double max = trackHeight - half;

		if (!darting && random.nextDouble() < dartChance()) {
			darting = true;
			double span = max - min;
			double away = orbY - min > span / 2 ? min + random.nextDouble() * span * 0.3 : max - random.nextDouble() * span * 0.3;
			orbTarget = away;
			retargetTimer = cfg.getMaxRetargetTicks();
		} else if (--retargetTimer <= 0 || Math.abs(orbTarget - orbY) < 1.5) {
			darting = false;
			orbTarget = min + random.nextDouble() * (max - min);
			int lo = cfg.getMinRetargetTicks();
			int hi = Math.max(lo + 1, cfg.getMaxRetargetTicks());
			retargetTimer = lo + random.nextInt(hi - lo);
		}

		double maxSpeed = orbSpeed() * (darting ? cfg.getDartSpeedMultiplier() : 1.0);
		double desired = Math.max(-maxSpeed, Math.min(maxSpeed, (orbTarget - orbY) * 0.15));
		double accel = cfg.getOrbAcceleration() * (darting ? 2.0 : 1.0);
		orbVelocity += Math.max(-accel, Math.min(accel, desired - orbVelocity));
		orbY += orbVelocity;
		if (orbY < min) {
			orbY = min;
			orbVelocity = 0;
		} else if (orbY > max) {
			orbY = max;
			orbVelocity = 0;
		}
	}

	@Override
	protected void onLevelUp() {
		progress = cfg.getProgressOnLevelUp();
	}

	public double trackHeight() {
		return trackHeight;
	}

	public double barY() {
		return barY;
	}

	public double barVelocity() {
		return barVelocity;
	}

	public double orbY() {
		return orbY;
	}

	public double orbVelocity() {
		return orbVelocity;
	}

	public double progress() {
		return progress;
	}

	public boolean isInside() {
		return inside;
	}

	public boolean isDarting() {
		return darting;
	}

	public TrainingConfig.ControlConfig cfg() {
		return cfg;
	}

	@Override
	protected long gameChecksum() {
		return Math.round(barY * 100.0) * 7919L + Math.round(orbY * 100.0) + Math.round(progress * 10000.0) * 31L;
	}
}
