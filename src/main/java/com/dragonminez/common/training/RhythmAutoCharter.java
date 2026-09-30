package com.dragonminez.common.training;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

public final class RhythmAutoCharter {
	public static final int SAMPLE_RATE = 22050;
	private static final int N_FFT = 2048;
	private static final int HOP = 128;
	private static final double FRAME_MS = HOP * 1000.0 / SAMPLE_RATE;
	private static final double BASE_FRAME_MS = 256 * 1000.0 / SAMPLE_RATE;
	private static final double FRAME_SCALE = BASE_FRAME_MS / FRAME_MS;
	private static final double ONSET_LAG_MS = 33.0;
	private static final int FLUX_LAG = frames(2);
	private static final int ONSET_REFINE_BACK = frames(3);
	private static final int ONSET_REFINE_FORWARD = frames(1);
	private static final double CENTER_MS = N_FFT / 2.0 * 1000.0 / SAMPLE_RATE;
	private static final double BIN_HZ = (double) SAMPLE_RATE / N_FFT;
	private static final int MIDI_LO = 33;
	private static final int MIDI_HI = 100;
	private static final double HF_CUTOFF = 2700.0;
	private static final double HF_WEIGHT = 0.6;
	private static final double[] TIER_BUDGET = {1.0, 1.8, 2.7, 3.6};
	private static final double[] TIER_GAP_MS = {260, 180, 130, 100};
	private static final double BUDGET_WINDOW_MS = 4000;
	private static final double[] POSITION_WEIGHT = {1.6, 0.9, 1.2, 0.9};
	private static final double LANE_JACK_MS = 140;
	private static final double[] GLOBAL_TIER_CUTS = {0.30, 0.55, 0.78};
	private static final double FAST_START_FRACTION = 0.25;
	private static final double FAST_START_DENSITY = 1.6;
	private static final double FAST_START_SCALE = 0.7;

	private static final double SILENCE_DB = -32.0;
	private static final double TEMPO_WINDOW_MS = 6000;
	private static final double TEMPO_HOP_MS = 1500;
	private static final double TEMPO_MIN_BPM = 60;
	private static final double TEMPO_MAX_BPM = 180;
	private static final double TEMPO_STEP_BPM = 0.5;
	private static final double TEMPO_CHANGE_COST = 6.0;
	private static final double CHORD_STRONG_RATIO = 0.92;
	private static final int CHORD_MIN_SEMITONES = 3;
	private static final int PEAK_PRE_MAX = frames(3);
	private static final int PEAK_POST_MAX = frames(3);
	private static final int PEAK_PRE_MEAN = frames(10);
	private static final int PEAK_POST_MEAN = frames(8);
	private static final int PEAK_WAIT = frames(4);
	private static final int SILENCE_SMOOTH_FRAMES = frames(21);
	private static final int VOCAL_BIN_LO = (int) Math.ceil(100.0 / BIN_HZ);
	private static final int VOCAL_BIN_HI = (int) Math.floor(5000.0 / BIN_HZ);
	private static final int ENERGY_BIN_LO = (int) Math.ceil(150.0 / BIN_HZ);
	private static final int ENERGY_BIN_HI = (int) Math.floor(4000.0 / BIN_HZ);
	private static final int HPSS_HALF = 7;
	private static final int HPSS_TIME_HALF = frames(5);
	private static final double F0_LO_MIDI = 43.0;
	private static final double F0_STEP = 0.5;
	private static final int F0_COUNT = 83;
	private static final int HARMONICS = 6;
	private static final double HARMONIC_DECAY = 0.8;
	private static final double PITCH_JUMP_COST = 0.25 / FRAME_SCALE;
	private static final double PITCH_JUMP_CAP = 3.0;
	private static final double PEAKINESS_VOICED = 3.0;
	private static final int ACTIVITY_SMOOTH_FRAMES = frames(15);
	private static final double GLIDE_MIN = 0.04 / FRAME_SCALE;
	private static final double GLIDE_MAX = 0.5 / FRAME_SCALE;
	private static final double VOCAL_NOTE_MIN_PEAKINESS = 2.5;
	private static final double VOCAL_ACTIVE_MIN_FRACTION = 0.18;
	private static final double VOCAL_GLIDE_MIN = 0.30;
	private static final double VOCAL_GAP_CLOSE_MS = 350;
	private static final double VOCAL_SEGMENT_MIN_MS = 500;
	private static final double VOCAL_NOTE_MIN_MS = 70;
	private static final double VOCAL_SPLIT_SEMITONES = 1.0;
	private static final int VOCAL_SPLIT_FRAMES = frames(5);
	private static final int VOCAL_ONSET_MIN_FRAMES = frames(8);
	private static final double VOCAL_ONSET_MIN_STRENGTH = 0.25;
	private static final int VOCAL_ONSET_ENERGY_FRAMES = frames(6);
	private static final double VOCAL_ONSET_ENERGY_RISE = 1.3;
	private static final double VOCAL_SNAP_MS = 45;
	private static final double VOCAL_HOLD_MAX_MS = 2500;

	public static final int PHASE_ANALYZING = 0;
	public static final int PHASE_TEMPO = 1;
	public static final int PHASE_SEPARATING = 2;
	public static final int PHASE_MAPPING = 3;
	public static final int PHASE_BUILDING = 4;
	public static final int PHASE_COUNT = 5;

	public static java.util.function.Consumer<String> DEBUG;
	public static FeatureSink FEATURES;

	public interface FeatureSink {
		void accept(int frame, float[] features, boolean silent);
	}

	public static final int FEATURE_BANDS = 24;
	private static final int FEATURE_CONTEXT_FRAMES = frames(8);
	private static final double FEATURE_LO_HZ = 100.0;
	private static final double FEATURE_HI_HZ = 5000.0;
	private static final double STEMS_VOCAL_RATIO = 0.12;
	private static final double STEMS_VOCAL_FLOOR_DB = -30.0;
	private static final double MODEL_ACTIVE_PROBABILITY = 0.5;
	private static final double MODEL_ACTIVE_MIN_FRACTION = 0.08;
	private static final double MODEL_MEAN_PROBABILITY = 0.40;
	private static final double STEMS_ACTIVE_MIN_FRACTION = 0.15;

	private static int frames(int baseFrames) {
		return Math.max(1, (int) Math.round(baseFrames * FRAME_SCALE));
	}

	private RhythmAutoCharter() {}

	public static float[] resample(float[] input, int sourceRate) {
		if (sourceRate == SAMPLE_RATE) return input;
		double ratio = sourceRate / (double) SAMPLE_RATE;
		int length = (int) (input.length / ratio);
		float[] out = new float[length];
		for (int i = 0; i < length; i++) {
			double pos = i * ratio;
			int a = (int) pos;
			int b = Math.min(input.length - 1, a + 1);
			double t = pos - a;
			double acc = input[a] * (1 - t) + input[b] * t;
			if (ratio > 1.5) {
				int span = (int) ratio;
				double sum = 0;
				int count = 0;
				for (int k = 0; k < span && a + k < input.length; k++) {
					sum += input[a + k];
					count++;
				}
				acc = count > 0 ? sum / count : acc;
			}
			out[i] = (float) acc;
		}
		return out;
	}

	public static RhythmChart generate(String id, String item, String sound, float[] mono) {
		return generate(id, item, sound, mono, null, null);
	}

	public static RhythmChart generate(String id, String item, String sound, float[] left, float[] right, IntConsumer phase) {
		return generate(id, item, sound, left, right, null, null, null, phase);
	}

	public static RhythmChart generate(String id, String item, String sound, float[] left, float[] right, float[] instrLeft, float[] instrRight, float[] vocalMono, IntConsumer phase) {
		if (phase != null) phase.accept(PHASE_ANALYZING);
		if (right != null && right.length != left.length) right = null;
		boolean stems = vocalMono != null && vocalMono.length == left.length && instrLeft != null && instrLeft.length == left.length;
		if (!stems) {
			vocalMono = null;
			instrLeft = null;
			instrRight = null;
		} else if (instrRight != null && instrRight.length != left.length) {
			instrRight = null;
		}
		int frames = 1 + Math.max(0, (left.length - N_FFT) / HOP);
		if (frames < 200) return null;
		double durationMs = left.length * 1000.0 / SAMPLE_RATE;
		int bands = MIDI_HI - MIDI_LO + 1;
		int[][] bandBins = bandBins(bands);
		Spectra spectra = analyseSpectra(left, right, instrLeft, instrRight, vocalMono, frames, bands, bandBins);

		if (phase != null) phase.accept(PHASE_TEMPO);
		Onsets mixOnsets = onsets(spectra.mixPeak, spectra.hf, bands);
		boolean[] silent = silenceGate(spectra.energy);
		Grid grid = buildGrid(mixOnsets.odf);
		if (grid == null) return null;

		if (phase != null) phase.accept(PHASE_SEPARATING);
		Vocals vocals = analyseVocals(spectra, silent, frames, bands, bandBins);

		if (phase != null) phase.accept(PHASE_MAPPING);
		Onsets instrOnsets = mixOnsets;
		if (spectra.stems) {
			instrOnsets = onsets(spectra.instrPeak, spectra.instrHf, bands);
		} else if (vocals != null) {
			float[][] instrPeak = new float[frames][bands];
			for (int f = 0; f < frames; f++) {
				for (int k = 0; k < bands; k++) instrPeak[f][k] = Math.max(0f, spectra.mixPeak[f][k] - spectra.vocalPeak[f][k]);
			}
			instrOnsets = onsets(instrPeak, spectra.hf, bands);
		}
		List<RhythmChart.Note> notes = buildInstrumental(instrOnsets, silent, grid, bands, frames, durationMs);
		if (notes == null || notes.size() < 20) return null;

		List<RhythmChart.Note> vocalNotes = new ArrayList<>();
		if (vocals != null) {
			vocalNotes = buildVocal(vocals, spectra, grid, durationMs);
			String problem = vocalNotes.size() < 20 ? "count" : validateNotes(vocalNotes, (int) durationMs);
			if (DEBUG != null) DEBUG.accept("vocal notes=" + vocalNotes.size() + " segments=" + vocals.segments.length + " problem=" + problem);
			if (problem != null) vocalNotes = new ArrayList<>();
		}

		if (phase != null) phase.accept(PHASE_BUILDING);
		return new RhythmChart(id, item, sound, (float) grid.bpm, (int) durationMs, notes, vocalNotes);
	}

	private static final class Spectra {
		float[][] mixPeak;
		float[][] vocalPeak;
		float[][] center;
		double[] centerBalance;
		double[] hf;
		double[] energy;
		double[] vocalEnergy;
		float[][] salience;
		float[][] instrPeak;
		double[] instrHf;
		float[][] features;
		boolean stereo;
		boolean stems;
	}

	private static final class Onsets {
		float[][] spec;
		float[][] flux;
		double[] odf;
	}

	private static final class Grid {
		double bpm;
		double[] gridTimes;
		int[] gridPos;
		double[] eighthTimes;
	}

	private static final class Vocals {
		boolean[] active;
		double[] peakiness;
		double[] pitch;
		int[][] segments;
		boolean[] onset;
	}

	private static int[][] bandBins(int bands) {
		int bins = N_FFT / 2 + 1;
		int[][] bandBins = new int[bands][];
		for (int k = 0; k < bands; k++) {
			double f = 440.0 * Math.pow(2, (MIDI_LO + k - 69) / 12.0);
			double lo = f * Math.pow(2, -0.5 / 12), hi = f * Math.pow(2, 0.5 / 12);
			List<Integer> list = new ArrayList<>();
			for (int b = 0; b < bins; b++) {
				double freq = b * BIN_HZ;
				if (freq >= lo && freq < hi) list.add(b);
			}
			if (list.isEmpty()) list.add((int) Math.min(bins - 1, Math.round(f / BIN_HZ)));
			bandBins[k] = list.stream().mapToInt(Integer::intValue).toArray();
		}
		return bandBins;
	}

	private static Spectra analyseSpectra(float[] left, float[] right, float[] instrLeft, float[] instrRight, float[] vocalMono, int frames, int bands, int[][] bandBins) {
		int bins = N_FFT / 2 + 1;
		int hfStart = (int) Math.ceil(HF_CUTOFF / BIN_HZ);
		int centerWidth = VOCAL_BIN_HI - VOCAL_BIN_LO + 1;
		Spectra s = new Spectra();
		s.stereo = right != null;
		s.stems = vocalMono != null;
		s.mixPeak = new float[frames][bands];
		s.center = new float[frames][centerWidth];
		s.centerBalance = new double[frames];
		s.hf = new double[frames];
		s.energy = new double[frames];
		s.features = new float[frames][FEATURE_BANDS * 2];
		if (s.stems) {
			s.instrPeak = new float[frames][bands];
			s.instrHf = new double[frames];
		}
		int[] featureBand = new int[bins];
		Arrays.fill(featureBand, -1);
		for (int b = 0; b < bins; b++) {
			double freq = b * BIN_HZ;
			if (freq < FEATURE_LO_HZ || freq >= FEATURE_HI_HZ) continue;
			featureBand[b] = Math.min(FEATURE_BANDS - 1, (int) Math.floor(FEATURE_BANDS * Math.log(freq / FEATURE_LO_HZ) / Math.log(FEATURE_HI_HZ / FEATURE_LO_HZ)));
		}
		double[] window = new double[N_FFT];
		for (int i = 0; i < N_FFT; i++) window[i] = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (N_FFT - 1));
		double[] lr = new double[N_FFT], li = new double[N_FFT];
		double[] rr = new double[N_FFT], ri = new double[N_FFT];
		double[] vr = new double[N_FFT], vi = new double[N_FFT];
		double[] xr = new double[N_FFT], xi = new double[N_FFT];
		double[] mag = new double[bins];
		double[] featMix = new double[FEATURE_BANDS];
		double[] featCenter = new double[FEATURE_BANDS];
		for (int f = 0; f < frames; f++) {
			int offset = f * HOP;
			for (int i = 0; i < N_FFT; i++) {
				lr[i] = left[offset + i] * window[i];
				li[i] = 0;
			}
			fft(lr, li);
			if (s.stereo) {
				for (int i = 0; i < N_FFT; i++) {
					rr[i] = right[offset + i] * window[i];
					ri[i] = 0;
				}
				fft(rr, ri);
			}
			if (s.stems) {
				for (int i = 0; i < N_FFT; i++) {
					vr[i] = vocalMono[offset + i] * window[i];
					vi[i] = 0;
					double inst = instrRight != null ? (instrLeft[offset + i] + instrRight[offset + i]) * 0.5 : instrLeft[offset + i];
					xr[i] = inst * window[i];
					xi[i] = 0;
				}
				fft(vr, vi);
				fft(xr, xi);
			}
			Arrays.fill(featMix, 0);
			Arrays.fill(featCenter, 0);
			double hfSum = 0, energy = 0, balanceWeighted = 0, balanceWeight = 0, instrHfSum = 0;
			for (int b = 0; b < bins; b++) {
				double m;
				double centerValue = 0;
				if (s.stereo) {
					double a = Math.hypot(lr[b], li[b]);
					double c = Math.hypot(rr[b], ri[b]);
					m = Math.hypot(lr[b] + rr[b], li[b] + ri[b]) * 0.5;
					if (!s.stems && b >= VOCAL_BIN_LO && b <= VOCAL_BIN_HI) {
						double coh = Math.max(0.0, (lr[b] * rr[b] + li[b] * ri[b]) / (a * c + 1e-9));
						double balance = 1.0 - Math.abs(a - c) / (a + c + 1e-9);
						centerValue = Math.min(a, c) * coh * balance;
						balanceWeighted += Math.min(a, c) * balance * coh;
						balanceWeight += Math.min(a, c);
					}
				} else {
					m = Math.hypot(lr[b], li[b]);
					if (!s.stems && b >= VOCAL_BIN_LO && b <= VOCAL_BIN_HI) centerValue = m;
				}
				if (s.stems) {
					if (b >= VOCAL_BIN_LO && b <= VOCAL_BIN_HI) centerValue = Math.hypot(vr[b], vi[b]);
					double im = Math.hypot(xr[b], xi[b]);
					if (b >= hfStart) instrHfSum += im;
					xr[b] = im;
				}
				if (b >= VOCAL_BIN_LO && b <= VOCAL_BIN_HI) s.center[f][b - VOCAL_BIN_LO] = (float) centerValue;
				mag[b] = m;
				if (b >= hfStart) hfSum += m;
				if (b >= ENERGY_BIN_LO && b <= ENERGY_BIN_HI) energy += m;
				int fb = featureBand[b];
				if (fb >= 0) {
					featMix[fb] += m;
					featCenter[fb] += centerValue;
				}
			}
			for (int k = 0; k < bands; k++) {
				double peak = 0, instrPeak = 0;
				for (int b : bandBins[k]) {
					peak = Math.max(peak, mag[b]);
					if (s.stems) instrPeak = Math.max(instrPeak, xr[b]);
				}
				s.mixPeak[f][k] = (float) peak;
				if (s.stems) s.instrPeak[f][k] = (float) instrPeak;
			}
			for (int k = 0; k < FEATURE_BANDS; k++) {
				s.features[f][k] = (float) Math.log1p(10.0 * featMix[k]);
				s.features[f][FEATURE_BANDS + k] = (float) Math.log1p(10.0 * featCenter[k]);
			}
			s.hf[f] = Math.log1p(10.0 * hfSum);
			if (s.stems) s.instrHf[f] = Math.log1p(10.0 * instrHfSum);
			s.energy[f] = energy;
			s.centerBalance[f] = s.stems ? 1.0 : s.stereo ? (balanceWeight > 0 ? balanceWeighted / balanceWeight : 0) : 1.0;
		}
		return s;
	}

	private static Onsets onsets(float[][] peak, double[] hf, int bands) {
		int frames = peak.length;
		Onsets o = new Onsets();
		o.spec = new float[frames][bands];
		for (int f = 0; f < frames; f++) for (int k = 0; k < bands; k++) o.spec[f][k] = (float) Math.log1p(10.0 * peak[f][k]);
		o.flux = new float[frames][bands];
		double[] tonal = new double[frames];
		double[] hfFlux = new double[frames];
		for (int f = 0; f < frames; f++) {
			int p = Math.max(0, f - FLUX_LAG);
			double sum = 0;
			for (int k = 0; k < bands; k++) {
				double ref = o.spec[p][k];
				if (k > 0) ref = Math.max(ref, o.spec[p][k - 1]);
				if (k + 1 < bands) ref = Math.max(ref, o.spec[p][k + 1]);
				double d = Math.max(0.0, o.spec[f][k] - ref);
				o.flux[f][k] = (float) d;
				sum += d;
			}
			tonal[f] = sum;
			hfFlux[f] = f > 0 ? Math.max(0.0, hf[f] - hf[f - 1]) : 0.0;
		}
		double tonalScale = percentile(tonal, 99) + 1e-9;
		double hfScale = percentile(hfFlux, 99) + 1e-9;
		o.odf = new double[frames];
		for (int f = 0; f < frames; f++) o.odf[f] = tonal[f] / tonalScale + HF_WEIGHT * hfFlux[f] / hfScale;
		return o;
	}

	private static boolean[] silenceGate(double[] energy) {
		int frames = energy.length;
		double[] smooth = movingAverage(energy, SILENCE_SMOOTH_FRAMES);
		double reference = percentile(smooth, 95);
		double threshold = reference * Math.pow(10.0, SILENCE_DB / 20.0);
		boolean[] silent = new boolean[frames];
		for (int f = 0; f < frames; f++) silent[f] = smooth[f] < threshold;
		return silent;
	}

	private static double[] movingAverage(double[] values, int size) {
		int n = values.length;
		int half = size / 2;
		double[] out = new double[n];
		double sum = 0;
		int count = 0;
		int lo = 0, hi = -1;
		for (int i = 0; i < n; i++) {
			int targetHi = Math.min(n - 1, i + half);
			int targetLo = Math.max(0, i - half);
			while (hi < targetHi) {
				hi++;
				sum += values[hi];
				count++;
			}
			while (lo < targetLo) {
				sum -= values[lo];
				lo++;
				count--;
			}
			out[i] = count > 0 ? sum / count : 0;
		}
		return out;
	}

	private static Grid buildGrid(double[] odf) {
		double[] periods = localPeriods(odf);
		if (periods == null) return null;
		double[] beats = trackBeats(odf, periods);
		if (beats.length < 8) return null;
		Grid g = new Grid();
		double[] sortedPeriods = periods.clone();
		Arrays.sort(sortedPeriods);
		g.bpm = 60000.0 / (sortedPeriods[sortedPeriods.length / 2] * FRAME_MS);
		int gridSize = (beats.length - 1) * 4;
		g.gridTimes = new double[gridSize];
		g.gridPos = new int[gridSize];
		List<Double> eighths = new ArrayList<>();
		for (int i = 0; i + 1 < beats.length; i++) {
			for (int k = 0; k < 4; k++) {
				int idx = i * 4 + k;
				g.gridTimes[idx] = (beats[i] + (beats[i + 1] - beats[i]) * k / 4.0) * FRAME_MS + CENTER_MS;
				g.gridPos[idx] = k;
				if (k % 2 == 0) eighths.add(g.gridTimes[idx]);
			}
		}
		g.eighthTimes = eighths.stream().mapToDouble(Double::doubleValue).toArray();
		return g;
	}

	private static int[] gatedPeaks(double[] odf, boolean[] silent) {
		int[] peaks = pickPeaks(odf);
		int kept = 0;
		for (int p : peaks) if (!silent[p]) peaks[kept++] = p;
		return Arrays.copyOf(peaks, kept);
	}

	private static boolean[] confidence(double[] times, Grid grid, int[] nearestGrid, double[] gridDev) {
		int n = times.length;
		boolean[] aligned = new boolean[n];
		for (int i = 0; i < n; i++) {
			int g = nearest(grid.eighthTimes, times[i]);
			aligned[i] = Math.abs(grid.eighthTimes[g] - times[i]) < 70;
		}
		double chance = 0;
		if (grid.eighthTimes.length > 1) {
			double spacing = (grid.eighthTimes[grid.eighthTimes.length - 1] - grid.eighthTimes[0]) / (grid.eighthTimes.length - 1);
			chance = Math.min(0.95, 140.0 / Math.max(1.0, spacing));
		}
		boolean[] confident = new boolean[n];
		for (int i = 0; i < n; i++) {
			int count = 0, hits = 0;
			for (int j = 0; j < n; j++) {
				if (Math.abs(times[j] - times[i]) < 4000) {
					count++;
					if (aligned[j]) hits++;
				}
			}
			if (count >= 4) confident[i] = ((hits / (double) count) - chance) / (1.0 - chance) >= 0.4;
		}
		return confident;
	}

	private static List<RhythmChart.Note> buildInstrumental(Onsets o, boolean[] silent, Grid grid, int bands, int frames, double durationMs) {
		int[] peaks = gatedPeaks(o.odf, silent);
		int n = peaks.length;
		if (n < 16) return null;
		double[] times = new double[n];
		for (int i = 0; i < n; i++) times[i] = refineOnset(o.spec, peaks[i], bands) * FRAME_MS + CENTER_MS + ONSET_LAG_MS;

		int[] nearestGrid = new int[n];
		double[] gridDev = new double[n];
		for (int i = 0; i < n; i++) {
			nearestGrid[i] = nearest(grid.gridTimes, times[i]);
			gridDev[i] = Math.abs(grid.gridTimes[nearestGrid[i]] - times[i]);
		}
		boolean[] confident = confidence(times, grid, nearestGrid, gridDev);

		double[] tonalOnly = new double[frames];
		double[] hfOnly = new double[frames];
		for (int f = 0; f < frames; f++) {
			double sum = 0;
			for (int k = 0; k < bands; k++) sum += o.flux[f][k];
			tonalOnly[f] = sum;
		}
		double tonalScale = percentile(tonalOnly, 99) + 1e-9;
		for (int f = 0; f < frames; f++) hfOnly[f] = Math.max(0.0, o.odf[f] - tonalOnly[f] / tonalScale);

		double[] snapped = new double[n];
		double[] salience = new double[n];
		boolean[] tonalNote = new boolean[n];
		double[] centroid = new double[n];
		int[] pitch = new int[n];
		int[] second = new int[n];
		double[] secondRatio = new double[n];
		for (int i = 0; i < n; i++) {
			boolean snap = confident[i] && gridDev[i] < 70;
			snapped[i] = snap ? grid.gridTimes[nearestGrid[i]] : times[i];
			salience[i] = o.odf[peaks[i]] * (snap ? POSITION_WEIGHT[grid.gridPos[nearestGrid[i]]] : 1.0);
			float[] row = o.flux[peaks[i]];
			double tonalPart = tonalOnly[peaks[i]] / tonalScale;
			double percussivePart = hfOnly[peaks[i]];
			tonalNote[i] = tonalPart >= percussivePart;
			double weighted = 0, total = 0;
			for (int k = 0; k < bands; k++) {
				weighted += row[k] * k;
				total += row[k];
			}
			centroid[i] = total > 0 ? weighted / total / (bands - 1) : 0.5;
			if (!tonalNote[i]) centroid[i] = Math.min(1.0, centroid[i] * 0.6 + 0.4 * percussivePart / Math.max(1e-9, tonalPart + percussivePart));
			int best = 0;
			for (int k = 1; k < bands; k++) if (row[k] > row[best]) best = k;
			pitch[i] = MIDI_LO + best;
			second[i] = -1;
			secondRatio[i] = 0;
			int secondBand = -1;
			for (int k = 0; k < bands; k++) {
				if (Math.abs(k - best) < CHORD_MIN_SEMITONES || row[k] < 0.7 * row[best]) continue;
				if (secondBand < 0 || row[k] > row[secondBand]) secondBand = k;
			}
			if (secondBand >= 0 && tonalNote[i]) {
				second[i] = MIDI_LO + secondBand;
				secondRatio[i] = row[secondBand] / Math.max(1e-9, row[best]);
			}
		}

		int[] tiers = assignTiers(snapped, salience, durationMs);
		int[] subs = assignSubs(snapped, salience, tiers);

		Integer[] order = new Integer[n];
		int kept = 0;
		for (int i = 0; i < n; i++) if (tiers[i] >= 0) order[kept++] = i;
		Integer[] keep = Arrays.copyOf(order, kept);
		Arrays.sort(keep, (a, b) -> Double.compare(snapped[a], snapped[b]));

		List<RhythmChart.Note> notes = new ArrayList<>();
		double[] laneLast = {-1e9, -1e9, -1e9, -1e9};
		for (int idx = 0; idx < keep.length; idx++) {
			int i = keep[idx];
			double t = snapped[i];
			int below = 0, equal = 0, total = 0;
			for (int j : keep) {
				if (Math.abs(snapped[j] - t) > 3000) continue;
				total++;
				if (pitch[j] < pitch[i]) below++;
				else if (pitch[j] == pitch[i]) equal++;
			}
			int lane = tonalNote[i] ? laneFor(below + 0.5 * equal, total) : (int) Math.max(0, Math.min(3, Math.floor(centroid[i] * 4)));
			if (t - laneLast[lane] < LANE_JACK_MS) {
				int free = -1;
				for (int l = 0; l < 4; l++) {
					if (t - laneLast[l] >= LANE_JACK_MS && (free < 0 || Math.abs(l - lane) < Math.abs(free - lane))) free = l;
				}
				if (free < 0) continue;
				lane = free;
			}

			int length = 0;
			if (tiers[i] <= 1) {
				double next = durationMs;
				for (int j = idx + 1; j < keep.length; j++) {
					if (snapped[keep[j]] > t + 1) {
						next = snapped[keep[j]];
						break;
					}
				}
				if (next - t >= 500) {
					int band = pitch[i] - MIDI_LO;
					int p = peaks[i];
					double peak = 1e-9;
					for (int k = p; k < Math.min(frames, p + frames(3)); k++) peak = Math.max(peak, o.spec[k][band]);
					int end = p;
					int limit = (int) Math.min(frames - 1, Math.min((next - 150 - CENTER_MS) / FRAME_MS, p + 2000 / FRAME_MS));
					while (end < limit && o.spec[end][band] >= 0.72 * peak) end++;
					double hold = (end - p) * FRAME_MS;
					if (hold >= 450) {
						length = (int) Math.min(hold, next - t - 150);
						if (length < 400) length = 0;
					}
				}
			}
			notes.add(new RhythmChart.Note((int) Math.round(t), lane, length, tiers[i], subs[i]));
			laneLast[lane] = t + length;

			if (second[i] >= 0 && tiers[i] <= 1) {
				int partnerBelow = 0;
				for (int j : keep) if (Math.abs(snapped[j] - t) <= 3000 && pitch[j] < second[i]) partnerBelow++;
				int partner = laneFor(partnerBelow, total);
				if (partner == lane) partner = lane < 3 ? lane + 1 : lane - 1;
				if (t - laneLast[partner] >= LANE_JACK_MS) {
					boolean strong = secondRatio[i] >= CHORD_STRONG_RATIO && tiers[i] == 0;
					notes.add(new RhythmChart.Note((int) Math.round(t), partner, 0, strong ? 1 : 3, RhythmChart.SUB_STEPS - 1));
					laneLast[partner] = t;
				}
			}
		}
		return notes;
	}

	private static Vocals analyseVocals(Spectra s, boolean[] silent, int frames, int bands, int[][] bandBins) {
		int width = VOCAL_BIN_HI - VOCAL_BIN_LO + 1;
		float[][] vocal = new float[frames][width];
		float[] column = new float[2 * HPSS_TIME_HALF + 1];
		float[] row = new float[2 * HPSS_HALF + 1];
		for (int f = 0; f < frames; f++) {
			for (int j = 0; j < width; j++) {
				int c = 0;
				for (int d = -HPSS_TIME_HALF; d <= HPSS_TIME_HALF; d++) {
					int ff = Math.max(0, Math.min(frames - 1, f + d));
					column[c++] = s.center[ff][j];
				}
				c = 0;
				for (int d = -HPSS_HALF; d <= HPSS_HALF; d++) {
					int jj = Math.max(0, Math.min(width - 1, j + d));
					row[c++] = s.center[f][jj];
				}
				double medT = median(column);
				double medF = median(row);
				double mask = medT * medT / (medT * medT + medF * medF + 1e-12);
				vocal[f][j] = (float) (s.center[f][j] * mask);
			}
		}
		s.center = null;

		s.vocalPeak = new float[frames][bands];
		s.vocalEnergy = new double[frames];
		s.salience = new float[frames][F0_COUNT];
		int[][] harmonicBins = new int[F0_COUNT][HARMONICS];
		for (int c = 0; c < F0_COUNT; c++) {
			double f0 = 440.0 * Math.pow(2, (F0_LO_MIDI + c * F0_STEP - 69) / 12.0);
			for (int h = 0; h < HARMONICS; h++) {
				double freq = f0 * (h + 1);
				int bin = (int) Math.round(freq / BIN_HZ);
				harmonicBins[c][h] = bin >= VOCAL_BIN_LO && bin <= VOCAL_BIN_HI ? bin - VOCAL_BIN_LO : -1;
			}
		}
		double[] peakiness = new double[frames];
		double[] pitchIndex = new double[frames];
		for (int f = 0; f < frames; f++) {
			float[] v = vocal[f];
			for (int k = 0; k < bands; k++) {
				double peak = 0;
				for (int b : bandBins[k]) {
					int j = b - VOCAL_BIN_LO;
					if (j >= 0 && j < width) peak = Math.max(peak, v[j]);
				}
				s.vocalPeak[f][k] = (float) peak;
			}
			double energy = 0;
			for (int j = Math.max(0, ENERGY_BIN_LO - VOCAL_BIN_LO); j <= Math.min(width - 1, ENERGY_BIN_HI - VOCAL_BIN_LO); j++) energy += v[j];
			s.vocalEnergy[f] = energy;
			double max = 0, sum = 0;
			for (int c = 0; c < F0_COUNT; c++) {
				double sal = 0;
				double weight = 1.0;
				for (int h = 0; h < HARMONICS; h++) {
					int j = harmonicBins[c][h];
					if (j < 0) break;
					double m = v[j];
					if (j > 0) m = Math.max(m, v[j - 1]);
					if (j + 1 < width) m = Math.max(m, v[j + 1]);
					sal += weight * m;
					weight *= HARMONIC_DECAY;
				}
				s.salience[f][c] = (float) sal;
				sum += sal;
				max = Math.max(max, sal);
			}
			peakiness[f] = sum > 0 ? max / (sum / F0_COUNT) : 0;
		}

		int[] track = viterbiPitch(s.salience, frames);
		for (int f = 0; f < frames; f++) {
			int c = track[f];
			double refined = c;
			if (c > 0 && c + 1 < F0_COUNT) {
				double a = s.salience[f][c - 1], b = s.salience[f][c], d = s.salience[f][c + 1];
				double denom = a - 2 * b + d;
				if (Math.abs(denom) > 1e-9) refined = c + Math.max(-0.5, Math.min(0.5, 0.5 * (a - d) / denom));
			}
			pitchIndex[f] = F0_LO_MIDI + refined * F0_STEP;
		}

		double[] ratio = new double[frames];
		double[] rawRatio = new double[frames];
		for (int f = 0; f < frames; f++) {
			double r = s.vocalEnergy[f] / (s.energy[f] + 1e-9);
			rawRatio[f] = r;
			ratio[f] = r * (peakiness[f] >= PEAKINESS_VOICED ? 1.0 : 0.3);
		}
		double[] score = movingAverage(ratio, ACTIVITY_SMOOTH_FRAMES);
		List<Double> live = new ArrayList<>();
		for (int f = 0; f < frames; f++) if (!silent[f]) live.add(score[f]);
		if (live.size() < 200) return null;
		double[] liveArray = live.stream().mapToDouble(Double::doubleValue).toArray();
		double threshold = Math.max(0.08, 0.45 * percentile(liveArray, 90));

		float[][] features = voiceFeatures(s, silent, rawRatio, peakiness, frames);
		boolean[] active = new boolean[frames];
		double minFraction;
		if (s.stems) {
			double floor = percentile(s.energy, 95) * Math.pow(10.0, STEMS_VOCAL_FLOOR_DB / 20.0);
			double[] smoothRatio = movingAverage(rawRatio, ACTIVITY_SMOOTH_FRAMES);
			double[] smoothVocal = movingAverage(s.vocalEnergy, ACTIVITY_SMOOTH_FRAMES);
			for (int f = 0; f < frames; f++) active[f] = !silent[f] && smoothVocal[f] > floor && smoothRatio[f] > STEMS_VOCAL_RATIO;
			minFraction = STEMS_ACTIVE_MIN_FRACTION;
		} else if (RhythmVoiceModel.isReady()) {
			double[] probability = new double[frames];
			for (int f = 0; f < frames; f++) probability[f] = silent[f] ? 0.0 : RhythmVoiceModel.probability(features[f]);
			double[] smoothProbability = movingAverage(probability, ACTIVITY_SMOOTH_FRAMES);
			double meanProbability = 0;
			int liveFrames = 0;
			for (int f = 0; f < frames; f++) {
				active[f] = !silent[f] && smoothProbability[f] > MODEL_ACTIVE_PROBABILITY;
				if (silent[f]) continue;
				meanProbability += smoothProbability[f];
				liveFrames++;
			}
			meanProbability = liveFrames == 0 ? 0 : meanProbability / liveFrames;
			if (DEBUG != null) DEBUG.accept(String.format(java.util.Locale.ROOT, "voice model meanP=%.2f", meanProbability));
			if (meanProbability < MODEL_MEAN_PROBABILITY) return null;
			minFraction = MODEL_ACTIVE_MIN_FRACTION;
		} else {
			for (int f = 0; f < frames; f++) active[f] = !silent[f] && score[f] > threshold;
			minFraction = VOCAL_ACTIVE_MIN_FRACTION;
		}
		int activeCount = 0, glide = 0;
		for (int f = 0; f < frames; f++) {
			if (!active[f]) continue;
			activeCount++;
			if (f > 0 && active[f - 1]) {
				double delta = Math.abs(pitchIndex[f] - pitchIndex[f - 1]);
				if (delta > GLIDE_MIN && delta < GLIDE_MAX) glide++;
			}
		}
		if (FEATURES != null) for (int f = 0; f < frames; f++) FEATURES.accept(f, features[f], silent[f]);
		if (DEBUG != null) {
			double balanceSum = 0;
			List<Double> f0s = new ArrayList<>();
			for (int f = 0; f < frames; f++) {
				if (!active[f]) continue;
				balanceSum += s.centerBalance[f];
				f0s.add(pitchIndex[f]);
			}
			Collections.sort(f0s);
			double f0med = f0s.isEmpty() ? 0 : f0s.get(f0s.size() / 2);
			double f0iqr = f0s.isEmpty() ? 0 : f0s.get(f0s.size() * 3 / 4) - f0s.get(f0s.size() / 4);
			DEBUG.accept(String.format(java.util.Locale.ROOT, "vocal metrics stereo=%b active=%.2f glide=%.2f balance=%.2f dips/s=%.2f f0med=%.1f f0iqr=%.1f threshold=%.3f",
					s.stereo, activeCount / (double) liveArray.length, activeCount == 0 ? 0 : glide / (double) activeCount,
					activeCount == 0 ? 0 : balanceSum / activeCount, dipRate(s.vocalEnergy, active), f0med, f0iqr, threshold));
		}
		if (activeCount < minFraction * liveArray.length) return null;
		if (!s.stems && !RhythmVoiceModel.isReady() && glide < VOCAL_GLIDE_MIN * activeCount) return null;

		int closeFrames = (int) Math.round(VOCAL_GAP_CLOSE_MS / FRAME_MS);
		int minFrames = (int) Math.round(VOCAL_SEGMENT_MIN_MS / FRAME_MS);
		List<int[]> segments = new ArrayList<>();
		int start = -1;
		for (int f = 0; f <= frames; f++) {
			boolean on = f < frames && active[f];
			if (on && start < 0) start = f;
			if (!on && start >= 0) {
				if (!segments.isEmpty() && start - segments.get(segments.size() - 1)[1] <= closeFrames) {
					segments.get(segments.size() - 1)[1] = f;
				} else {
					segments.add(new int[]{start, f});
				}
				start = -1;
			}
		}
		List<int[]> kept = new ArrayList<>();
		for (int[] seg : segments) if (seg[1] - seg[0] >= minFrames) kept.add(seg);
		if (kept.isEmpty()) return null;
		boolean[] finalActive = new boolean[frames];
		for (int[] seg : kept) for (int f = seg[0]; f < seg[1]; f++) finalActive[f] = true;

		Onsets vocalOnsets = onsets(s.vocalPeak, new double[frames], bands);
		int[] onsetPeaks = pickPeaks(vocalOnsets.odf);
		boolean[] onset = new boolean[frames];
		for (int p : onsetPeaks) {
			if (vocalOnsets.odf[p] < VOCAL_ONSET_MIN_STRENGTH) continue;
			double before = 0, after = 0;
			int count = 0;
			for (int d = 1; d <= VOCAL_ONSET_ENERGY_FRAMES; d++) {
				if (p - d < 0 || p + d >= frames) continue;
				before += s.vocalEnergy[p - d];
				after += s.vocalEnergy[p + d];
				count++;
			}
			if (count > 0 && after >= VOCAL_ONSET_ENERGY_RISE * before) onset[p] = true;
		}

		Vocals v = new Vocals();
		v.active = finalActive;
		v.peakiness = peakiness;
		v.pitch = pitchIndex;
		v.segments = kept.toArray(new int[0][]);
		v.onset = onset;
		return v;
	}

	private static float[][] voiceFeatures(Spectra s, boolean[] silent, double[] ratio, double[] peakiness, int frames) {
		int bandFeatures = FEATURE_BANDS * 2;
		float[] median = new float[bandFeatures];
		float[] column = new float[frames];
		for (int k = 0; k < bandFeatures; k++) {
			int count = 0;
			for (int f = 0; f < frames; f++) if (!silent[f]) column[count++] = s.features[f][k];
			if (count == 0) continue;
			float[] values = Arrays.copyOf(column, count);
			Arrays.sort(values);
			median[k] = values[count / 2];
		}
		float[][] base = new float[frames][bandFeatures];
		for (int f = 0; f < frames; f++) for (int k = 0; k < bandFeatures; k++) base[f][k] = s.features[f][k] - median[k];
		int half = FEATURE_CONTEXT_FRAMES;
		float[][] out = new float[frames][RhythmVoiceModel.INPUTS];
		double[] sum = new double[bandFeatures];
		double[] sumSq = new double[bandFeatures];
		int lo = 0, hi = -1, count = 0;
		for (int f = 0; f < frames; f++) {
			int targetLo = Math.max(0, f - half), targetHi = Math.min(frames - 1, f + half);
			while (hi < targetHi) {
				hi++;
				for (int k = 0; k < bandFeatures; k++) {
					sum[k] += base[hi][k];
					sumSq[k] += base[hi][k] * base[hi][k];
				}
				count++;
			}
			while (lo < targetLo) {
				for (int k = 0; k < bandFeatures; k++) {
					sum[k] -= base[lo][k];
					sumSq[k] -= base[lo][k] * base[lo][k];
				}
				lo++;
				count--;
			}
			float[] row = out[f];
			for (int k = 0; k < bandFeatures; k++) {
				double mean = sum[k] / count;
				row[k] = base[f][k];
				row[bandFeatures + k] = (float) mean;
			}
			for (int k = 0; k < FEATURE_BANDS; k++) {
				int c = FEATURE_BANDS + k;
				double mean = sum[c] / count;
				row[2 * bandFeatures + k] = (float) Math.sqrt(Math.max(0.0, sumSq[c] / count - mean * mean));
			}
			int scalars = 2 * bandFeatures + FEATURE_BANDS;
			row[scalars] = (float) Math.min(2.0, ratio[f]);
			row[scalars + 1] = (float) Math.min(1.0, peakiness[f] / 10.0);
			row[scalars + 2] = (float) s.centerBalance[f];
		}
		return out;
	}

	private static double dipRate(double[] energy, boolean[] active) {
		int n = energy.length;
		double[] smooth = movingAverage(energy, frames(3));
		int peakWindow = frames(17);
		int minDip = frames(2), maxDip = frames(17);
		int dips = 0, activeFrames = 0;
		boolean inDip = false;
		int dipStart = 0;
		double dipPeak = 0;
		for (int f = 0; f < n; f++) {
			if (!active[f]) {
				inDip = false;
				continue;
			}
			activeFrames++;
			double recentPeak = 0;
			for (int k = Math.max(0, f - peakWindow); k <= f; k++) if (active[k]) recentPeak = Math.max(recentPeak, smooth[k]);
			if (!inDip && smooth[f] < 0.4 * recentPeak) {
				inDip = true;
				dipStart = f;
				dipPeak = recentPeak;
			} else if (inDip && smooth[f] >= 0.7 * dipPeak) {
				int length = f - dipStart;
				if (length >= minDip && length <= maxDip) dips++;
				inDip = false;
			} else if (inDip && f - dipStart > maxDip) {
				inDip = false;
			}
		}
		return activeFrames == 0 ? 0 : dips / (activeFrames * FRAME_MS / 1000.0);
	}

	private static int[] viterbiPitch(float[][] salience, int frames) {
		double[] prev = new double[F0_COUNT];
		double[] cur = new double[F0_COUNT];
		int[][] back = new int[frames][F0_COUNT];
		double[] cost = new double[F0_COUNT];
		for (int d = 0; d < F0_COUNT; d++) cost[d] = Math.min(PITCH_JUMP_CAP, PITCH_JUMP_COST * d * F0_STEP);
		for (int f = 0; f < frames; f++) {
			double max = 1e-9;
			for (int c = 0; c < F0_COUNT; c++) max = Math.max(max, salience[f][c]);
			for (int c = 0; c < F0_COUNT; c++) {
				double emission = Math.log(salience[f][c] / max + 0.02);
				if (f == 0) {
					cur[c] = emission;
					back[f][c] = -1;
					continue;
				}
				double best = -Double.MAX_VALUE;
				int bestPrev = 0;
				for (int p = 0; p < F0_COUNT; p++) {
					double cand = prev[p] - cost[Math.abs(p - c)];
					if (cand > best) {
						best = cand;
						bestPrev = p;
					}
				}
				cur[c] = best + emission;
				back[f][c] = bestPrev;
			}
			double[] swap = prev;
			prev = cur;
			cur = swap;
		}
		int[] track = new int[frames];
		int state = 0;
		for (int c = 1; c < F0_COUNT; c++) if (prev[c] > prev[state]) state = c;
		for (int f = frames - 1; f >= 0; f--) {
			track[f] = state;
			if (f > 0) state = back[f][state];
		}
		return track;
	}

	private static final class VocalNote {
		double timeMs;
		double durationMs;
		double pitch;
		double salience;
	}

	private static List<RhythmChart.Note> buildVocal(Vocals vocals, Spectra s, Grid grid, double durationMs) {
		List<VocalNote> candidates = new ArrayList<>();
		for (int[] seg : vocals.segments) {
			int start = -1;
			double pitchSum = 0;
			int pitchCount = 0;
			int deviate = 0;
			for (int f = seg[0]; f < seg[1]; f++) {
				double p = vocals.pitch[f];
				if (start < 0) {
					start = f;
					pitchSum = p;
					pitchCount = 1;
					deviate = 0;
					continue;
				}
				double mean = pitchSum / pitchCount;
				boolean onset = vocals.onset[f] && f - start >= VOCAL_ONSET_MIN_FRAMES;
				if (Math.abs(p - mean) > VOCAL_SPLIT_SEMITONES) deviate++;
				else deviate = 0;
				if (onset || deviate >= VOCAL_SPLIT_FRAMES) {
					int end = onset ? f : f - (VOCAL_SPLIT_FRAMES - 1);
					closeVocalNote(candidates, s, vocals, start, end, pitchSum, pitchCount);
					start = end;
					pitchSum = 0;
					pitchCount = 0;
					for (int k = start; k <= f; k++) {
						pitchSum += vocals.pitch[k];
						pitchCount++;
					}
					deviate = 0;
				} else {
					pitchSum += p;
					pitchCount++;
				}
			}
			if (start >= 0) closeVocalNote(candidates, s, vocals, start, seg[1], pitchSum, pitchCount);
		}
		if (candidates.size() < 8) return new ArrayList<>();

		int n = candidates.size();
		double[] times = new double[n];
		double[] salience = new double[n];
		for (int i = 0; i < n; i++) {
			VocalNote c = candidates.get(i);
			int g = nearest(grid.gridTimes, c.timeMs);
			double dev = Math.abs(grid.gridTimes[g] - c.timeMs);
			if (dev < VOCAL_SNAP_MS) {
				c.timeMs = grid.gridTimes[g];
				c.salience *= POSITION_WEIGHT[grid.gridPos[g]];
			}
			times[i] = c.timeMs;
			salience[i] = c.salience;
		}
		int[] tiers = assignTiers(times, salience, durationMs);
		int[] subs = assignSubs(times, salience, tiers);

		List<RhythmChart.Note> merged = new ArrayList<>();
		Integer[] order = new Integer[n];
		for (int i = 0; i < n; i++) order[i] = i;
		Arrays.sort(order, (a, b) -> Double.compare(times[a], times[b]));
		for (int idx = 0; idx < n; idx++) {
			int i = order[idx];
			if (tiers[i] < 0) continue;
			VocalNote c = candidates.get(i);
			int below = 0, total = 0;
			for (int j = 0; j < n; j++) {
				if (tiers[j] < 0 || Math.abs(times[j] - times[i]) > 3000) continue;
				total++;
				if (candidates.get(j).pitch < c.pitch - 0.25) below++;
				else if (Math.abs(candidates.get(j).pitch - c.pitch) <= 0.25 && j < i) below++;
			}
			int lane = laneFor(below, total);
			int length = 0;
			if (tiers[i] <= 1 && c.durationMs >= 450) {
				double next = durationMs;
				for (int k = idx + 1; k < n; k++) {
					if (tiers[order[k]] >= 0 && times[order[k]] > times[i] + 1) {
						next = times[order[k]];
						break;
					}
				}
				length = (int) Math.min(VOCAL_HOLD_MAX_MS, Math.min(c.durationMs - 60, next - times[i] - 150));
				if (length < 400) length = 0;
			}
			merged.add(new RhythmChart.Note((int) Math.round(times[i]), lane, length, tiers[i], subs[i]));
		}

		merged.sort((a, b) -> a.timeMs() != b.timeMs() ? Integer.compare(a.timeMs(), b.timeMs()) : Integer.compare(a.lane(), b.lane()));

		List<RhythmChart.Note> out = new ArrayList<>();
		double[] laneLast = {-1e9, -1e9, -1e9, -1e9};
		for (RhythmChart.Note note : merged) {
			int lane = note.lane();
			double t = note.timeMs();
			if (t - laneLast[lane] < LANE_JACK_MS) {
				int free = -1;
				for (int l = 0; l < 4; l++) {
					if (t - laneLast[l] >= LANE_JACK_MS && (free < 0 || Math.abs(l - lane) < Math.abs(free - lane))) free = l;
				}
				if (free < 0) continue;
				lane = free;
			}
			int length = note.lengthMs();
			out.add(lane == note.lane() ? note : new RhythmChart.Note(note.timeMs(), lane, length, note.tier(), note.sub()));
			laneLast[lane] = t + length;
		}
		return out;
	}

	private static void closeVocalNote(List<VocalNote> out, Spectra s, Vocals vocals, int start, int end, double pitchSum, int pitchCount) {
		if (end <= start || pitchCount <= 0) return;
		double duration = (end - start) * FRAME_MS;
		if (duration < VOCAL_NOTE_MIN_MS) return;
		double energy = 0, peakiness = 0;
		for (int f = start; f < end; f++) {
			energy += s.vocalEnergy[f];
			peakiness += vocals.peakiness[f];
		}
		energy /= (end - start);
		peakiness /= (end - start);
		if (peakiness < VOCAL_NOTE_MIN_PEAKINESS) return;
		VocalNote note = new VocalNote();
		note.timeMs = start * FRAME_MS + CENTER_MS + ONSET_LAG_MS;
		note.durationMs = duration;
		note.pitch = pitchSum / pitchCount;
		note.salience = energy * Math.min(1.5, Math.sqrt(duration / 150.0));
		out.add(note);
	}

	private static double median(float[] values) {
		float[] copy = values.clone();
		Arrays.sort(copy);
		return copy[copy.length / 2];
	}

	private static int refineOnset(float[][] spec, int peak, int bands) {
		int best = peak;
		double bestRise = -1;
		int lo = Math.max(1, peak - ONSET_REFINE_BACK), hi = Math.min(spec.length - 1, peak + ONSET_REFINE_FORWARD);
		for (int f = lo; f <= hi; f++) {
			double rise = 0;
			for (int k = 0; k < bands; k++) rise += Math.max(0.0, spec[f][k] - spec[f - 1][k]);
			if (rise > bestRise) {
				bestRise = rise;
				best = f;
			}
		}
		return best;
	}

	private static int laneFor(double rank, int total) {
		return (int) Math.max(0, Math.min(3, Math.floor(4 * rank / Math.max(1, total))));
	}

	private static int[] assignTiers(double[] times, double[] salience, double durationMs) {
		int n = times.length;
		Integer[] order = new Integer[n];
		for (int i = 0; i < n; i++) order[i] = i;
		Arrays.sort(order, (a, b) -> Double.compare(salience[b], salience[a]));
		int[] globalTier = new int[n];
		for (int rank = 0; rank < n; rank++) {
			double fraction = rank / (double) Math.max(1, n - 1);
			int tier = 3;
			for (int t = 0; t < GLOBAL_TIER_CUTS.length; t++) {
				if (fraction < GLOBAL_TIER_CUTS[t]) {
					tier = t;
					break;
				}
			}
			globalTier[order[rank]] = tier;
		}

		double earlyEnd = durationMs * FAST_START_FRACTION;
		int earlyStrong = 0;
		for (int i = 0; i < n; i++) if (times[i] < earlyEnd && globalTier[i] <= 1) earlyStrong++;
		boolean fastStart = earlyEnd > 0 && earlyStrong / (earlyEnd / 1000.0) > FAST_START_DENSITY;

		int[] tiers = new int[n];
		Arrays.fill(tiers, -1);
		List<List<Double>> placed = new ArrayList<>();
		for (int t = 0; t < 4; t++) placed.add(new ArrayList<>());
		for (int i : order) {
			double time = times[i];
			double scale = 1.0;
			if (fastStart && time < earlyEnd) scale = FAST_START_SCALE + (1.0 - FAST_START_SCALE) * (time / earlyEnd);
			for (int tier = globalTier[i]; tier < 4; tier++) {
				boolean ok = true;
				int count = 0;
				for (int tt = 0; tt <= tier && ok; tt++) {
					for (double other : placed.get(tt)) {
						double d = Math.abs(other - time);
						if (d < TIER_GAP_MS[tier]) {
							ok = false;
							break;
						}
						if (d <= BUDGET_WINDOW_MS / 2) count++;
					}
				}
				if (ok && count < TIER_BUDGET[tier] * scale * BUDGET_WINDOW_MS / 1000.0) {
					tiers[i] = tier;
					placed.get(tier).add(time);
					break;
				}
			}
		}
		return tiers;
	}

	private static int[] assignSubs(double[] times, double[] salience, int[] tiers) {
		int n = times.length;
		int[] subs = new int[n];
		for (int i = 0; i < n; i++) {
			if (tiers[i] < 0) continue;
			int count = 0, higher = 0;
			for (int j = 0; j < n; j++) {
				if (tiers[j] != tiers[i] || Math.abs(times[j] - times[i]) > BUDGET_WINDOW_MS) continue;
				count++;
				if (salience[j] > salience[i] || (salience[j] == salience[i] && j < i)) higher++;
			}
			subs[i] = count <= 1 ? 0 : Math.min(RhythmChart.SUB_STEPS - 1, (int) Math.floor(RhythmChart.SUB_STEPS * higher / (double) count));
		}
		return subs;
	}

	private static double percentile(double[] values, double p) {
		double[] sorted = values.clone();
		Arrays.sort(sorted);
		double pos = p / 100.0 * (sorted.length - 1);
		int lo = (int) Math.floor(pos);
		int hi = Math.min(sorted.length - 1, lo + 1);
		return sorted[lo] + (sorted[hi] - sorted[lo]) * (pos - lo);
	}

	private static int nearest(double[] sorted, double value) {
		int idx = Arrays.binarySearch(sorted, value);
		if (idx >= 0) return idx;
		int ins = -idx - 1;
		if (ins <= 0) return 0;
		if (ins >= sorted.length) return sorted.length - 1;
		return value - sorted[ins - 1] <= sorted[ins] - value ? ins - 1 : ins;
	}

	private static double[] localPeriods(double[] novelty) {
		int n = novelty.length;
		int window = Math.min(n, (int) Math.round(TEMPO_WINDOW_MS / FRAME_MS));
		int hop = Math.max(1, (int) Math.round(TEMPO_HOP_MS / FRAME_MS));
		if (window < 64) return null;
		int maxLag = (int) Math.ceil(60000.0 / TEMPO_MIN_BPM / FRAME_MS) + 2;
		int minLag = Math.max(1, (int) Math.floor(60000.0 / TEMPO_MAX_BPM / FRAME_MS) - 1);
		if (maxLag + 1 >= window) return null;
		int candidates = (int) Math.round((TEMPO_MAX_BPM - TEMPO_MIN_BPM) / TEMPO_STEP_BPM) + 1;
		double[] lags = new double[candidates];
		double[] prior = new double[candidates];
		for (int c = 0; c < candidates; c++) {
			double bpm = TEMPO_MIN_BPM + c * TEMPO_STEP_BPM;
			lags[c] = 60000.0 / bpm / FRAME_MS;
			prior[c] = Math.exp(-0.5 * Math.pow(Math.log(bpm / 110.0) / Math.log(2) / 0.7, 2));
		}
		List<Integer> centers = new ArrayList<>();
		for (int start = 0; start + window <= n; start += hop) centers.add(start + window / 2);
		if (centers.isEmpty()) centers.add(n / 2);
		int windows = centers.size();
		double[][] emission = new double[windows][candidates];
		double[] ac = new double[maxLag + 2];
		for (int w = 0; w < windows; w++) {
			int start = Math.max(0, Math.min(n - window, centers.get(w) - window / 2));
			double mean = 0;
			for (int i = start; i < start + window; i++) mean += novelty[i];
			mean /= window;
			Arrays.fill(ac, 0);
			for (int lag = minLag; lag <= maxLag + 1; lag++) {
				double sum = 0;
				for (int i = start; i + lag < start + window; i++) sum += (novelty[i] - mean) * (novelty[i + lag] - mean);
				ac[lag] = sum;
			}
			double max = 1e-12;
			for (int c = 0; c < candidates; c++) {
				int i = (int) lags[c];
				double frac = lags[c] - i;
				double score = i + 1 < ac.length ? ac[i] * (1 - frac) + ac[i + 1] * frac : 0;
				emission[w][c] = Math.max(0.0, score) * prior[c];
				max = Math.max(max, emission[w][c]);
			}
			for (int c = 0; c < candidates; c++) emission[w][c] /= max;
		}
		double[] prev = new double[candidates];
		double[] cur = new double[candidates];
		int[][] back = new int[windows][candidates];
		System.arraycopy(emission[0], 0, prev, 0, candidates);
		for (int w = 1; w < windows; w++) {
			for (int c = 0; c < candidates; c++) {
				double best = -Double.MAX_VALUE;
				int bestPrev = c;
				for (int q = 0; q < candidates; q++) {
					double change = Math.abs(Math.log((TEMPO_MIN_BPM + c * TEMPO_STEP_BPM) / (TEMPO_MIN_BPM + q * TEMPO_STEP_BPM)) / Math.log(2));
					double cand = prev[q] - TEMPO_CHANGE_COST * change;
					if (cand > best) {
						best = cand;
						bestPrev = q;
					}
				}
				cur[c] = best + emission[w][c];
				back[w][c] = bestPrev;
			}
			double[] swap = prev;
			prev = cur;
			cur = swap;
		}
		int state = 0;
		for (int c = 1; c < candidates; c++) if (prev[c] > prev[state]) state = c;
		double[] windowPeriod = new double[windows];
		for (int w = windows - 1; w >= 0; w--) {
			windowPeriod[w] = lags[state];
			if (w > 0) state = back[w][state];
		}
		double[] periods = new double[n];
		for (int f = 0; f < n; f++) {
			int w = 0;
			while (w + 1 < windows && centers.get(w + 1) <= f) w++;
			if (w + 1 >= windows || f <= centers.get(w)) {
				periods[f] = windowPeriod[w];
			} else {
				double t = (f - centers.get(w)) / (double) (centers.get(w + 1) - centers.get(w));
				periods[f] = windowPeriod[w] + (windowPeriod[w + 1] - windowPeriod[w]) * t;
			}
		}
		return periods;
	}

	private static double[] trackBeats(double[] novelty, double[] periods) {
		int n = novelty.length;
		double[] score = novelty.clone();
		int[] back = new int[n];
		Arrays.fill(back, -1);
		for (int t = 0; t < n; t++) {
			double period = periods[t];
			int lo = Math.max(0, t - (int) Math.round(period * 2)), hi = t - (int) Math.round(period / 2);
			if (hi <= lo) continue;
			double best = -Double.MAX_VALUE;
			int bestIdx = -1;
			for (int prev = lo; prev < hi; prev++) {
				double cand = score[prev] - 100.0 * Math.pow(Math.log((t - prev) / period), 2);
				if (cand > best) {
					best = cand;
					bestIdx = prev;
				}
			}
			score[t] = novelty[t] + best;
			back[t] = bestIdx;
		}
		double period = periods[n - 1];
		int tail = (int) Math.max(0, n - Math.round(period));
		int t = tail;
		for (int i = tail; i < n; i++) if (score[i] > score[t]) t = i;
		List<Integer> beats = new ArrayList<>();
		while (t >= 0) {
			beats.add(t);
			t = back[t];
		}
		double[] out = new double[beats.size()];
		for (int i = 0; i < out.length; i++) out[i] = beats.get(beats.size() - 1 - i);
		return out;
	}

	private static int[] pickPeaks(double[] odf) {
		int n = odf.length;
		List<Integer> peaks = new ArrayList<>();
		int last = Integer.MIN_VALUE / 2;
		for (int t = 0; t < n; t++) {
			int a = Math.max(0, t - PEAK_PRE_MAX), b = Math.min(n, t + PEAK_POST_MAX + 1);
			double max = 0;
			for (int k = a; k < b; k++) max = Math.max(max, odf[k]);
			if (odf[t] < max) continue;
			int c = Math.max(0, t - PEAK_PRE_MEAN), d = Math.min(n, t + PEAK_POST_MEAN);
			double mean = 0;
			for (int k = c; k < d; k++) mean += odf[k];
			mean /= (d - c);
			if (odf[t] < mean + 0.05) continue;
			if (t - last < PEAK_WAIT) continue;
			peaks.add(t);
			last = t;
		}
		return peaks.stream().mapToInt(Integer::intValue).toArray();
	}

	private static void fft(double[] re, double[] im) {
		int n = re.length;
		for (int i = 1, j = 0; i < n; i++) {
			int bit = n >> 1;
			for (; (j & bit) != 0; bit >>= 1) j ^= bit;
			j ^= bit;
			if (i < j) {
				double tr = re[i];
				re[i] = re[j];
				re[j] = tr;
				double ti = im[i];
				im[i] = im[j];
				im[j] = ti;
			}
		}
		for (int len = 2; len <= n; len <<= 1) {
			double angle = -2 * Math.PI / len;
			double wr = Math.cos(angle), wi = Math.sin(angle);
			for (int i = 0; i < n; i += len) {
				double cr = 1, ci = 0;
				for (int k = 0; k < len / 2; k++) {
					int a = i + k, b = i + k + len / 2;
					double xr = re[b] * cr - im[b] * ci;
					double xi = re[b] * ci + im[b] * cr;
					re[b] = re[a] - xr;
					im[b] = im[a] - xi;
					re[a] += xr;
					im[a] += xi;
					double nr = cr * wr - ci * wi;
					ci = cr * wi + ci * wr;
					cr = nr;
				}
			}
		}
	}

	public static String validate(RhythmChart chart, int songLengthMs) {
		if (chart == null) return "empty";
		double seconds = chart.durationMs / 1000.0;
		if (seconds < 10 || (songLengthMs > 0 && chart.durationMs > songLengthMs + 5000)) return "duration";
		return validateNotes(chart.notes, chart.durationMs);
	}

	public static String validateNotes(List<RhythmChart.Note> notes, int durationMs) {
		double seconds = durationMs / 1000.0;
		int full = notes.size();
		if (full < seconds * 0.5 || full > seconds * 8) return "density";
		int tierZero = 0;
		int[] laneLast = {-100000, -100000, -100000, -100000};
		for (RhythmChart.Note note : notes) {
			if (note.timeMs() < 0 || note.timeMs() > durationMs) return "time";
			if (note.lengthMs() > 3000) return "hold";
			if (note.tier() == 0) tierZero++;
			if (note.timeMs() - laneLast[note.lane()] < 90) return "spacing";
			laneLast[note.lane()] = note.timeMs() + note.lengthMs();
		}
		if (tierZero < full * 0.08) return "tiers";
		return null;
	}
}
