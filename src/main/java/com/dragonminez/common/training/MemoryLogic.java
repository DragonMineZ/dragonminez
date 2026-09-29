package com.dragonminez.common.training;

import com.dragonminez.common.config.TrainingConfig;

public class MemoryLogic extends MinigameLogic {
	public static final String ID = "memory";
	public static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	public enum Phase { TYPING, SUCCESS, TIMEOUT }

	private static final double PERFORMANCE_WEIGHT = 0.08;

	private final TrainingConfig.MemoryConfig cfg;

	private int[] sequence = new int[0];
	private Phase phase = Phase.TYPING;
	private int index;
	private int totalTicks;
	private int totalRemaining;
	private int keyTicks;
	private int keyRemaining;
	private int fastDrain;
	private int phaseTimer;
	private int lives;
	private int wrongTick = -100;
	private int wrongChar = -1;
	private int hitTick = -100;
	private int remapCount;

	public MemoryLogic(TrainingConfig config, long seed) {
		super(config, config.getMemory(), seed);
		this.cfg = config.getMemory();
		this.lives = Math.max(1, cfg.getLives());
		buildSequence();
	}

	@Override
	public String id() {
		return ID;
	}

	public int sequenceLength() {
		int every = Math.max(1, cfg.getLengthEveryLevels());
		int len = cfg.getBaseSequenceLength() + difficulty() / every;
		return Math.max(1, Math.min(cfg.getMaxSequenceLength(), len));
	}

	private int ticksPerKey() {
		return (int) Math.max(cfg.getMinTicksPerKey(), Math.round(cfg.getBaseTicksPerKey() - difficulty() * cfg.getTicksPerKeyDecreasePerLevel()));
	}

	private void buildSequence() {
		int len = sequenceLength();
		sequence = new int[len];
		for (int i = 0; i < len; i++) {
			int c;
			do {
				c = random.nextInt(MinigameEvent.CHAR_COUNT);
			} while (i > 0 && c == sequence[i - 1]);
			sequence[i] = c;
		}
		int perKey = ticksPerKey();
		totalTicks = perKey * len;
		totalRemaining = totalTicks;
		keyTicks = (int) Math.round(perKey * cfg.getKeyTimeMultiplier());
		keyRemaining = keyTicks;
		fastDrain = 0;
		index = 0;
		phase = Phase.TYPING;
		remapCount++;
	}

	@Override
	protected void tickGame() {
		switch (phase) {
			case TYPING -> {
				int drain = fastDrain > 0 ? Math.max(1, cfg.getFastDrainRate()) : 1;
				if (fastDrain > 0) fastDrain--;
				totalRemaining -= drain;
				if (--keyRemaining <= 0) {
					mistake(-1);
					keyRemaining = keyTicks;
				}
				if (totalRemaining <= 0) {
					totalRemaining = 0;
					lives--;
					samplePerformance(0.0, PERFORMANCE_WEIGHT * 2);
					phase = Phase.TIMEOUT;
					phaseTimer = Math.max(1, cfg.getFeedbackTicks() * 2);
				}
			}
			case SUCCESS -> {
				if (--phaseTimer <= 0) {
					levelUp();
					if (!isFinished()) buildSequence();
				}
			}
			case TIMEOUT -> {
				if (--phaseTimer <= 0) {
					if (lives <= 0) fail();
					else buildSequence();
				}
			}
		}
	}

	@Override
	protected void onKeyDown(int key, float frac) {
		if (phase != Phase.TYPING) return;
		int character = key - MinigameEvent.CHAR_BASE;
		if (character < 0 || character >= MinigameEvent.CHAR_COUNT) return;
		if (character == sequence[index]) {
			index++;
			hitTick = tickCount();
			keyRemaining = keyTicks;
			samplePerformance(1.0, PERFORMANCE_WEIGHT);
			if (index >= sequence.length) {
				phase = Phase.SUCCESS;
				phaseTimer = Math.max(1, cfg.getFeedbackTicks());
			}
		} else {
			mistake(character);
		}
	}

	private void mistake(int character) {
		wrongTick = tickCount();
		wrongChar = character;
		fastDrain += Math.max(0, cfg.getWrongKeyDrainTicks());
		samplePerformance(0.0, PERFORMANCE_WEIGHT);
	}

	public int[] sequence() {
		return sequence.clone();
	}

	public Phase phase() {
		return phase;
	}

	public int index() {
		return index;
	}

	public int totalTicks() {
		return totalTicks;
	}

	public int totalRemaining() {
		return totalRemaining;
	}

	public int keyTicks() {
		return keyTicks;
	}

	public int keyRemaining() {
		return keyRemaining;
	}

	public boolean isFastDraining() {
		return fastDrain > 0;
	}

	public int lives() {
		return lives;
	}

	public int wrongTick() {
		return wrongTick;
	}

	public int wrongChar() {
		return wrongChar;
	}

	public int hitTick() {
		return hitTick;
	}

	public int remapCount() {
		return remapCount;
	}

	public TrainingConfig.MemoryConfig cfg() {
		return cfg;
	}

	@Override
	protected long gameChecksum() {
		return phase.ordinal() * 1000L + index * 10L + lives + totalRemaining * 7919L;
	}
}
