package com.dragonminez.common.training;

import com.dragonminez.common.config.TrainingConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RhythmLogic extends MinigameLogic {
	public static final String ID = "rhythm";
	public static final int LANES = 4;
	public static final int LEAD_IN_TICKS = 60;
	public static final int MS_PER_TICK = 50;
	private static final int SONG_END_GRACE_MS = 1500;
	private static final int FREE_FIRST_NOTE_MS = 800;
	private static final double PERFORMANCE_WEIGHT = 0.06;

	public enum NoteState { PENDING, HOLDING, DONE }

	public static final int JUDGE_MISS = 0;
	public static final int JUDGE_GOOD = 1;
	public static final int JUDGE_PERFECT = 2;

	private final TrainingConfig.RhythmConfig cfg;
	private final RhythmChart chart;
	private final List<Note> notes = new ArrayList<>();
	private final int[] laneJudgeTick = new int[LANES];
	private final int[] laneJudge = new int[LANES];

	private static final double[] FREE_NOTES_PER_BEAT = {0.425, 0.85, 1.30, 1.66};
	private final double[] chartTierDensity = {0.0, 0.0, 0.0, 0.0};
	private int chartIndex;
	private double freeStepTimeMs = FREE_FIRST_NOTE_MS;
	private int freeStep;
	private final double[] freeLaneBusyUntil = new double[LANES];
	private int freeLastLane = -1;

	private double progress;
	private double health;
	private int combo;
	private int maxCombo;
	private int perfects;
	private int goods;
	private int misses;
	private int holdsCompleted;
	private int lastJudgement = -1;
	private int lastJudgementTick = -100;

	public RhythmLogic(TrainingConfig config, long seed, RhythmChart chart) {
		super(config, config.getRhythm(), seed);
		this.cfg = config.getRhythm();
		this.chart = chart;
		this.health = cfg.getStartingHealth();
		for (int i = 0; i < LANES; i++) laneJudgeTick[i] = -100;
		if (chart != null && !chart.notes.isEmpty()) {
			int first = chart.notes.get(0).timeMs();
			int last = chart.notes.get(chart.notes.size() - 1).timeMs();
			double seconds = Math.max(10.0, (last - first) / 1000.0);
			for (int tier = 0; tier < 4; tier++) {
				int count = 0;
				for (RhythmChart.Note note : chart.notes) if (note.tier() <= tier) count++;
				chartTierDensity[tier] = count / seconds;
			}
		}
	}

	@Override
	public String id() {
		return ID;
	}

	public RhythmChart chart() {
		return chart;
	}

	public boolean isFreeMode() {
		return chart == null;
	}

	@Override
	public double rewardRate() {
		double rate = super.rewardRate();
		return chart != null ? rate * Math.max(0.0, cfg.getSongRewardMultiplier()) : rate;
	}

	public static double songTimeMs(int tick, float frac) {
		return (tick - LEAD_IN_TICKS + frac) * MS_PER_TICK;
	}

	public double songTimeMs() {
		return songTimeMs(tickCount(), 0f);
	}

	public int travelMs() {
		return Math.max(cfg.getMinTravelMs(), cfg.getBaseTravelMs() - difficulty() * cfg.getTravelMsDecreasePerLevel());
	}

	public int allowedTier() {
		int every = Math.max(1, cfg.getDensityTierEveryLevels());
		return Math.min(3, difficulty() / every);
	}

	@Override
	protected void tickGame() {
		double now = songTimeMs();
		progress = Math.max(0, progress - cfg.getProgressDecayPerTick());
		spawnNotes(now);

		Iterator<Note> it = notes.iterator();
		while (it.hasNext() && !isFinished()) {
			Note note = it.next();
			if (note.state == NoteState.HOLDING) {
				if (now >= note.timeMs + note.lengthMs) {
					note.state = NoteState.DONE;
					holdsCompleted++;
					progress += noteProgress() * cfg.getHoldProgressFactor();
					samplePerformance(1.0, PERFORMANCE_WEIGHT);
					checkLevel();
				}
			} else if (note.state == NoteState.PENDING && now > note.timeMs + cfg.getGoodWindowMs()) {
				note.state = NoteState.DONE;
				miss(note.lane);
			}
			if (note.state == NoteState.DONE && now > note.timeMs + note.lengthMs + 400) it.remove();
		}
		if (isFinished()) return;

		if (chart != null && chartIndex >= chart.notes.size() && now > chart.durationMs + SONG_END_GRACE_MS) {
			boolean pending = false;
			for (Note note : notes) if (note.state != NoteState.DONE) pending = true;
			if (!pending) complete();
		}
	}

	private void spawnNotes(double now) {
		int travel = travelMs();
		int tier = allowedTier();
		if (chart != null) {
			while (chartIndex < chart.notes.size() && chart.notes.get(chartIndex).timeMs() - travel <= now + MS_PER_TICK) {
				RhythmChart.Note n = chart.notes.get(chartIndex++);
				if (n.tier() <= tier) notes.add(new Note(n.timeMs(), n.lane(), n.lengthMs(), travel));
			}
			return;
		}
		while (freeStepTimeMs - travel <= now + MS_PER_TICK) generateFreeStep(travel, tier);
	}

	private void generateFreeStep(int travel, int tier) {
		double bpm = Math.min(cfg.getFreeMaxBpm(), cfg.getFreeBaseBpm() + difficulty() * cfg.getFreeBpmPerLevel());
		double beatMs = 60000.0 / bpm;
		int position = freeStep % 4;
		double chance;
		int noteTier;
		if (position == 0) {
			chance = 0.85;
			noteTier = (freeStep / 4) % 2 == 0 ? 0 : 1;
		} else if (position == 2) {
			chance = 0.45;
			noteTier = 2;
		} else {
			chance = 0.18;
			noteTier = 3;
		}
		int time = (int) Math.round(freeStepTimeMs);
		if (noteTier <= tier && random.nextDouble() < chance) {
			int lane = pickFreeLane(time);
			if (lane >= 0) {
				int length = 0;
				if (position == 0 && random.nextDouble() < cfg.getFreeHoldChance()) {
					length = (int) Math.round(beatMs * (1 + random.nextInt(2)) - 80);
				}
				notes.add(new Note(time, lane, length, travel));
				freeLaneBusyUntil[lane] = time + length + 150;
				freeLastLane = lane;
				if (position == 0 && length == 0 && tier >= 2 && random.nextDouble() < cfg.getFreeChordChance()) {
					int partner = (lane + 2) % LANES;
					if (freeLaneBusyUntil[partner] <= time) {
						notes.add(new Note(time, partner, 0, travel));
						freeLaneBusyUntil[partner] = time + 150;
					}
				}
			}
		}
		freeStep++;
		freeStepTimeMs += beatMs / 4.0;
	}

	private int pickFreeLane(int time) {
		int start = random.nextInt(LANES);
		for (int i = 0; i < LANES; i++) {
			int lane = (start + i) % LANES;
			if (freeLaneBusyUntil[lane] > time) continue;
			if (lane == freeLastLane && random.nextDouble() < 0.7) continue;
			return lane;
		}
		for (int lane = 0; lane < LANES; lane++) if (freeLaneBusyUntil[lane] <= time) return lane;
		return -1;
	}

	@Override
	protected void onKeyDown(int key, float frac) {
		if (key < 0 || key >= LANES) return;
		double pressMs = songTimeMs(tickCount(), frac);
		Note best = null;
		double bestDist = Double.MAX_VALUE;
		for (Note note : notes) {
			if (note.state != NoteState.PENDING || note.lane != key) continue;
			double dist = Math.abs(pressMs - note.timeMs);
			if (dist <= cfg.getGoodWindowMs() && dist < bestDist) {
				bestDist = dist;
				best = note;
			}
		}
		if (best == null) {
			ghost();
			return;
		}
		boolean perfect = bestDist <= cfg.getPerfectWindowMs();
		if (perfect) perfects++;
		else goods++;
		combo++;
		maxCombo = Math.max(maxCombo, combo);
		progress += noteProgress() * (perfect ? 1.0 : cfg.getGoodProgressFactor());
		health = Math.min(1.0, health + (perfect ? cfg.getHealthGainPerfect() : cfg.getHealthGainGood()));
		samplePerformance(perfect ? 1.0 : 0.6, PERFORMANCE_WEIGHT);
		judge(key, perfect ? JUDGE_PERFECT : JUDGE_GOOD);
		best.hitTick = tickCount();
		best.state = best.lengthMs > 0 ? NoteState.HOLDING : NoteState.DONE;
		checkLevel();
	}

	@Override
	protected void onKeyUp(int key, float frac) {
		if (key < 0 || key >= LANES) return;
		double releaseMs = songTimeMs(tickCount(), frac);
		for (Note note : notes) {
			if (note.state != NoteState.HOLDING || note.lane != key) continue;
			note.state = NoteState.DONE;
			if (releaseMs >= note.timeMs + note.lengthMs - cfg.getHoldReleaseGraceMs()) {
				holdsCompleted++;
				progress += noteProgress() * cfg.getHoldProgressFactor();
				samplePerformance(1.0, PERFORMANCE_WEIGHT);
				checkLevel();
			} else {
				note.dropped = true;
				miss(key);
			}
			return;
		}
	}

	private double currentDensity() {
		int tier = allowedTier();
		if (chart != null) return Math.max(0.3, chartTierDensity[tier]);
		double bpm = Math.min(cfg.getFreeMaxBpm(), cfg.getFreeBaseBpm() + difficulty() * cfg.getFreeBpmPerLevel());
		return FREE_NOTES_PER_BEAT[tier] * bpm / 60.0;
	}

	private double noteProgress() {
		double needed = cfg.getProgressMax() - cfg.getProgressOnLevelUp();
		return needed / (Math.max(1.0, cfg.getTargetSecondsPerLevel()) * currentDensity());
	}

	private void ghost() {
		combo = 0;
		health -= cfg.getHealthLossOnGhost();
		if (health <= 0) fail();
	}

	private void miss(int lane) {
		misses++;
		combo = 0;
		progress = Math.max(0, progress - cfg.getProgressLossOnMiss());
		health -= cfg.getHealthLossOnMiss();
		samplePerformance(0.0, PERFORMANCE_WEIGHT);
		judge(lane, JUDGE_MISS);
		if (health <= 0) {
			health = 0;
			fail();
		}
	}

	private void judge(int lane, int value) {
		lastJudgement = value;
		lastJudgementTick = tickCount();
		laneJudge[lane] = value;
		laneJudgeTick[lane] = tickCount();
	}

	private void checkLevel() {
		if (isFinished()) return;
		if (progress >= cfg.getProgressMax()) levelUp();
	}

	@Override
	protected void onLevelUp() {
		progress = cfg.getProgressOnLevelUp();
	}

	public List<Note> notes() {
		return Collections.unmodifiableList(notes);
	}

	public TrainingConfig.RhythmConfig cfg() {
		return cfg;
	}

	public double progress() {
		return progress;
	}

	public double health() {
		return health;
	}

	public int combo() {
		return combo;
	}

	public int maxCombo() {
		return maxCombo;
	}

	public int perfects() {
		return perfects;
	}

	public int goods() {
		return goods;
	}

	public int misses() {
		return misses;
	}

	public int holdsCompleted() {
		return holdsCompleted;
	}

	public int judgedNotes() {
		return perfects + goods + misses;
	}

	public double accuracy() {
		int judged = judgedNotes();
		return judged == 0 ? 1.0 : (perfects + goods * 0.6) / judged;
	}

	public int lastJudgement() {
		return lastJudgement;
	}

	public int lastJudgementTick() {
		return lastJudgementTick;
	}

	public int laneJudgeTick(int lane) {
		return laneJudgeTick[lane];
	}

	public int laneJudge(int lane) {
		return laneJudge[lane];
	}

	@Override
	protected long gameChecksum() {
		return notes.size() * 31L + Math.round(progress * 1000.0) + Math.round(health * 100000.0) * 7L + combo * 13L;
	}

	public static final class Note {
		public final int timeMs;
		public final int lane;
		public final int lengthMs;
		public final int travelMs;
		public NoteState state = NoteState.PENDING;
		public int hitTick = -1;
		public boolean dropped;

		Note(int timeMs, int lane, int lengthMs, int travelMs) {
			this.timeMs = timeMs;
			this.lane = lane;
			this.lengthMs = lengthMs;
			this.travelMs = travelMs;
		}
	}
}
