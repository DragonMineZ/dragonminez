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

	public static final int DIFFICULTY_EASY = 0;
	public static final int DIFFICULTY_NORMAL = 1;
	public static final int DIFFICULTY_HARD = 2;
	public static final int DIFFICULTY_EXPERT = 3;
	public static final int DIFFICULTY_COUNT = 4;
	private static final char OPTION_SEPARATOR = '|';
	public static final int REST_MIN_MS = 3000;
	private static final int REST_SETTLE_MS = 300;

	public enum NoteState { PENDING, HOLDING, DONE }

	public static final int JUDGE_MISS = 0;
	public static final int JUDGE_GOOD = 1;
	public static final int JUDGE_PERFECT = 2;

	private final TrainingConfig.RhythmConfig cfg;
	private final RhythmChart chart;
	private final int difficultyIndex;
	private final int variant;
	private final List<RhythmChart.Note> chartNotes;
	private final TrainingConfig.SongDifficulty songDifficulty;
	private final List<Note> notes = new ArrayList<>();
	private final int[] laneJudgeTick = new int[LANES];
	private final int[] laneJudge = new int[LANES];

	private static final double[] FREE_NOTES_PER_BEAT = {0.425, 0.85, 1.30, 1.66};
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
	private double lastSpawnedEndMs = Double.NEGATIVE_INFINITY;
	private boolean resting;
	private int restRemainingMs;
	private boolean songCompleted;

	public RhythmLogic(TrainingConfig config, long seed, RhythmChart chart) {
		this(config, seed, chart, DIFFICULTY_NORMAL);
	}

	public RhythmLogic(TrainingConfig config, long seed, RhythmChart chart, int difficulty) {
		this(config, seed, chart, difficulty, RhythmChart.VARIANT_INSTRUMENTAL);
	}

	public RhythmLogic(TrainingConfig config, long seed, RhythmChart chart, int difficulty, int variant) {
		super(config, config.getRhythm(), seed);
		this.cfg = config.getRhythm();
		this.chart = chart;
		this.variant = chart == null ? RhythmChart.VARIANT_INSTRUMENTAL : chart.effectiveVariant(variant);
		this.chartNotes = chart == null ? List.of() : chart.notesFor(this.variant);
		this.difficultyIndex = clampDifficulty(difficulty);
		this.songDifficulty = cfg.getDifficulty(difficultyIndex);
		this.health = cfg.getStartingHealth();
		for (int i = 0; i < LANES; i++) laneJudgeTick[i] = -100;
	}

	public static int clampDifficulty(int difficulty) {
		return Math.max(0, Math.min(DIFFICULTY_COUNT - 1, difficulty));
	}

	public static String buildOption(String item, int difficulty) {
		return buildOption(item, difficulty, RhythmChart.VARIANT_INSTRUMENTAL);
	}

	public static String buildOption(String item, int difficulty, int variant) {
		return buildOption(item, difficulty, variant, "");
	}

	public static String buildOption(String item, int difficulty, int variant, String audioHash) {
		if (item == null || item.isEmpty()) return "";
		String option = item + OPTION_SEPARATOR + clampDifficulty(difficulty) + OPTION_SEPARATOR + RhythmChart.clampVariant(variant);
		return audioHash == null || audioHash.isEmpty() ? option : option + OPTION_SEPARATOR + audioHash;
	}

	public static String optionHash(String option) {
		if (option == null) return "";
		String[] parts = option.split("\\|");
		return parts.length < 4 ? "" : parts[3].trim();
	}

	public static int optionVariant(String option) {
		if (option == null) return RhythmChart.VARIANT_INSTRUMENTAL;
		String[] parts = option.split("\\|");
		if (parts.length < 3) return RhythmChart.VARIANT_INSTRUMENTAL;
		try {
			return RhythmChart.clampVariant(Integer.parseInt(parts[2].trim()));
		} catch (NumberFormatException e) {
			return RhythmChart.VARIANT_INSTRUMENTAL;
		}
	}

	public static String optionTrack(String option) {
		if (option == null) return "";
		int sep = option.indexOf(OPTION_SEPARATOR);
		return sep < 0 ? option : option.substring(0, sep);
	}

	public static int optionDifficulty(String option) {
		if (option == null) return DIFFICULTY_NORMAL;
		String[] parts = option.split("\\|");
		if (parts.length < 2) return DIFFICULTY_NORMAL;
		try {
			return clampDifficulty(Integer.parseInt(parts[1].trim()));
		} catch (NumberFormatException e) {
			return DIFFICULTY_NORMAL;
		}
	}

	public static int recommendedDifficulty(RhythmChart chart, TrainingConfig.RhythmConfig cfg) {
		return recommendedDifficulty(chart, cfg, RhythmChart.VARIANT_INSTRUMENTAL);
	}

	public static int recommendedDifficulty(RhythmChart chart, TrainingConfig.RhythmConfig cfg, int variant) {
		if (chart == null) return DIFFICULTY_NORMAL;
		double limit = Math.max(0.5, cfg.getRecommendedMaxNotesPerSecond());
		for (int d = DIFFICULTY_COUNT - 1; d > 0; d--) {
			if (chart.densityAt(cfg.getDifficulty(d).getEndIntensity(), variant) <= limit) return d;
		}
		return DIFFICULTY_EASY;
	}

	public int variant() {
		return variant;
	}

	public boolean isResting() {
		return resting;
	}

	public int restRemainingMs() {
		return restRemainingMs;
	}

	public boolean isSongCompleted() {
		return songCompleted;
	}

	public int difficultyIndex() {
		return difficultyIndex;
	}

	public TrainingConfig.SongDifficulty songDifficulty() {
		return songDifficulty;
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
		if (chart == null) return rate;
		return rate * Math.max(0.0, cfg.getSongRewardMultiplier()) * Math.max(0.0, songDifficulty.getRewardMultiplier());
	}

	public double intensity() {
		if (chart == null) return (allowedTier() + 1) / 4.0;
		double start = songDifficulty.getStartIntensity();
		return start + (songDifficulty.getEndIntensity() - start) * songProgress();
	}

	public static double songTimeMs(int tick, float frac) {
		return (tick - LEAD_IN_TICKS + frac) * MS_PER_TICK;
	}

	public double songTimeMs() {
		return songTimeMs(tickCount(), 0f);
	}

	public double songProgress() {
		if (chart == null || chart.durationMs <= 0) return 0.0;
		return Math.max(0.0, Math.min(1.0, songTimeMs() / chart.durationMs));
	}

	public int travelMs() {
		if (chart != null) {
			double start = songDifficulty.getStartTravelMs();
			return (int) Math.round(start + (songDifficulty.getEndTravelMs() - start) * songProgress());
		}
		return Math.max(cfg.getMinTravelMs(), cfg.getBaseTravelMs() - difficulty() * cfg.getTravelMsDecreasePerLevel());
	}

	public int allowedTier() {
		if (chart != null) return Math.max(0, Math.min(3, (int) Math.floor(intensity() * 4.0)));
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

		if (chart != null) updateRest(now);

		if (chart != null && chartIndex >= chartNotes.size() && now > chart.durationMs + SONG_END_GRACE_MS) {
			boolean pending = false;
			for (Note note : notes) if (note.state != NoteState.DONE) pending = true;
			if (!pending) {
				songCompleted = true;
				complete();
			}
		}
	}

	private void updateRest(double now) {
		resting = false;
		restRemainingMs = 0;
		if (lastSpawnedEndMs == Double.NEGATIVE_INFINITY || now < lastSpawnedEndMs + REST_SETTLE_MS) return;
		for (Note note : notes) if (note.state != NoteState.DONE) return;
		double intensity = intensity();
		int next = -1;
		for (int i = chartIndex; i < chartNotes.size(); i++) {
			if (chartNotes.get(i).level() <= intensity) {
				next = chartNotes.get(i).timeMs();
				break;
			}
		}
		if (next < 0 || next - lastSpawnedEndMs < REST_MIN_MS) return;
		double visibleAt = next - travelMs();
		if (visibleAt <= now) return;
		resting = true;
		restRemainingMs = (int) Math.ceil(visibleAt - now);
	}

	private void spawnNotes(double now) {
		int travel = travelMs();
		if (chart != null) {
			double intensity = intensity();
			while (chartIndex < chartNotes.size() && chartNotes.get(chartIndex).timeMs() - travel <= now + MS_PER_TICK) {
				RhythmChart.Note n = chartNotes.get(chartIndex++);
				if (n.level() <= intensity) {
					notes.add(new Note(n.timeMs(), n.lane(), n.lengthMs(), travel));
					lastSpawnedEndMs = Math.max(lastSpawnedEndMs, n.timeMs() + n.lengthMs());
				}
			}
			return;
		}
		int tier = allowedTier();
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
		if (key < 0 || key >= LANES || resting) return;
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
		if (chart != null) return Math.max(0.3, chart.densityAt(intensity(), variant));
		int tier = allowedTier();
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
		return notes.size() * 31L + Math.round(progress * 1000.0) + Math.round(health * 100000.0) * 7L + combo * 13L + (resting ? 17L : 0L);
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
