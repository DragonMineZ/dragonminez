package com.dragonminez.common.training;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

public final class RhythmAutoCharter {
	public static final int SAMPLE_RATE = 22050;
	private static final int N_FFT = 2048;
	private static final int HOP = 256;
	private static final double FRAME_MS = HOP * 1000.0 / SAMPLE_RATE;
	private static final double CENTER_MS = N_FFT / 2.0 * 1000.0 / SAMPLE_RATE;
	private static final double[] BANDS = {30, 150, 400, 1500, 5000, 11000};
	private static final double[] BAND_WEIGHTS = {1.3, 1.0, 1.0, 0.9, 0.7};

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
		int frames = 1 + Math.max(0, (mono.length - N_FFT) / HOP);
		if (frames < 200) return null;
		int bins = N_FFT / 2 + 1;
		int bandCount = BANDS.length - 1;
		int[] binBand = new int[bins];
		for (int k = 0; k < bins; k++) {
			double freq = k * (double) SAMPLE_RATE / N_FFT;
			binBand[k] = -1;
			for (int b = 0; b < bandCount; b++) if (freq >= BANDS[b] && freq < BANDS[b + 1]) binBand[k] = b;
		}

		double[][] flux = new double[frames][bandCount];
		double[][] energy = new double[frames][bandCount];
		double[] window = new double[N_FFT];
		for (int i = 0; i < N_FFT; i++) window[i] = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (N_FFT - 1));
		double[] re = new double[N_FFT];
		double[] im = new double[N_FFT];
		double[] previousLog = new double[bins];
		for (int f = 0; f < frames; f++) {
			int offset = f * HOP;
			for (int i = 0; i < N_FFT; i++) {
				re[i] = mono[offset + i] * window[i];
				im[i] = 0;
			}
			fft(re, im);
			for (int k = 0; k < bins; k++) {
				double mag = Math.hypot(re[k], im[k]);
				double log = Math.log1p(100.0 * mag);
				int band = binBand[k];
				if (band >= 0) {
					if (f > 0) flux[f][band] += Math.max(0.0, log - previousLog[k]);
					energy[f][band] += mag;
				}
				previousLog[k] = log;
			}
		}
		for (int b = 0; b < bandCount; b++) {
			double fluxScale = percentile(column(flux, b), 95) + 1e-9;
			double energyScale = percentile(column(energy, b), 95) + 1e-9;
			for (int f = 0; f < frames; f++) {
				flux[f][b] /= fluxScale;
				energy[f][b] /= energyScale;
			}
		}

		double[] raw = new double[frames];
		for (int f = 0; f < frames; f++) for (int b = 0; b < bandCount; b++) raw[f] += flux[f][b] * BAND_WEIGHTS[b];
		double[] novelty = new double[frames];
		for (int f = 0; f < frames; f++) {
			double sum = 0;
			int count = 0;
			for (int d = -1; d <= 1; d++) {
				int g = f + d;
				if (g >= 0 && g < frames) sum += raw[g];
				count++;
			}
			novelty[f] = sum / count;
		}
		double top = percentile(novelty, 99) + 1e-9;
		for (int f = 0; f < frames; f++) novelty[f] /= top;

		double period = estimatePeriod(novelty);
		if (period <= 0) return null;
		double[] beats = trackBeats(novelty, period);
		if (beats.length < 8) return null;
		double bpm = 60000.0 / (period * FRAME_MS);

		List<double[]> grid = new ArrayList<>();
		for (int i = 0; i + 1 < beats.length; i++) {
			for (int k = 0; k < 4; k++) grid.add(new double[]{beats[i] + (beats[i + 1] - beats[i]) * k / 4.0, k});
		}
		grid.add(new double[]{beats[beats.length - 1], 0});
		double[] gridTimes = new double[grid.size()];
		for (int i = 0; i < gridTimes.length; i++) gridTimes[i] = grid.get(i)[0];

		int[] peaks = pickPeaks(novelty, (int) (70 / FRAME_MS));
		if (peaks.length < 16) return null;
		double[] strengths = new double[peaks.length];
		for (int i = 0; i < peaks.length; i++) strengths[i] = novelty[peaks[i]];
		double strongCut = percentile(strengths, 60);
		double chordCut = percentile(strengths, 92);
		double snapLimit = 70 / FRAME_MS;

		TreeMap<Integer, double[]> candidates = new TreeMap<>();
		for (int p : peaks) {
			int gi = nearest(gridTimes, p);
			if (Math.abs(gridTimes[gi] - p) > snapLimit) continue;
			int position = (int) grid.get(gi)[1];
			double s = novelty[p];
			int tier;
			if (position == 0) tier = s >= strongCut ? 0 : 1;
			else if (position == 2) tier = s >= strongCut ? 1 : 2;
			else tier = 3;
			double weighted = 0, total = 0;
			int dominant = 0, broad = 0;
			for (int b = 0; b < bandCount; b++) {
				weighted += flux[p][b] * b;
				total += flux[p][b];
				if (flux[p][b] > flux[p][dominant]) dominant = b;
				if (flux[p][b] > 0.6) broad++;
			}
			double centroid = weighted / (total + 1e-9);
			double[] previous = candidates.get(gi);
			if (previous == null || s > previous[2]) candidates.put(gi, new double[]{gridTimes[gi], tier, s, centroid, dominant, broad, p, 0});
		}

		double[] frameEnergy = new double[frames];
		for (int f = 0; f < frames; f++) for (int b = 0; b < bandCount; b++) frameEnergy[f] += energy[f][b];
		double silence = percentile(frameEnergy, 50) * 0.3;
		for (int beat = 0; beat * 4 < gridTimes.length; beat++) {
			int gi = beat * 4;
			if (candidates.containsKey(gi)) continue;
			int frame = (int) Math.round(gridTimes[gi]);
			if (frame < 0 || frame >= frames || frameEnergy[frame] < silence) continue;
			double weighted = 0, total = 0;
			int dominant = 0;
			for (int b = 0; b < bandCount; b++) {
				weighted += energy[frame][b] * b;
				total += energy[frame][b];
				if (energy[frame][b] > energy[frame][dominant]) dominant = b;
			}
			candidates.put(gi, new double[]{gridTimes[gi], beat % 2 == 0 ? 0 : 1, novelty[frame], weighted / (total + 1e-9), dominant, 0, frame, 1});
		}

		List<double[]> ordered = new ArrayList<>(candidates.values());
		Random rng = new Random(id.hashCode());
		List<RhythmChart.Note> notes = new ArrayList<>();
		double durationMs = mono.length * 1000.0 / SAMPLE_RATE;
		double lastTime = -1e9;
		double[] laneLast = {-1e9, -1e9, -1e9, -1e9};
		int previousLane = -1, repeat = 0;
		for (int i = 0; i < ordered.size(); i++) {
			double[] n = ordered.get(i);
			double time = n[0] * FRAME_MS + CENTER_MS;
			int tier = (int) n[1];
			double minGap = tier < 3 ? 90 : 120;
			if (time - lastTime < minGap) continue;
			int lane = (int) Math.max(0, Math.min(3, Math.round(n[3] / 4.0 * 3.0)));
			if (lane == previousLane) {
				repeat++;
				if (repeat >= 2 || time - laneLast[lane] < 180) {
					lane = lane == 0 ? 1 : lane == 3 ? 2 : lane + (rng.nextBoolean() ? 1 : -1);
					repeat = 0;
				}
			} else {
				repeat = 0;
			}
			if (time - laneLast[lane] < 140) {
				int best = -1;
				for (int l = 0; l < 4; l++) {
					if (time - laneLast[l] >= 140 && (best < 0 || Math.abs(l - lane) < Math.abs(best - lane))) best = l;
				}
				if (best < 0) continue;
				lane = best;
			}

			int length = 0;
			double next = i + 1 < ordered.size() ? ordered.get(i + 1)[0] * FRAME_MS + CENTER_MS : durationMs;
			if (tier <= 1 && n[7] == 0 && next - time >= 520) {
				int band = (int) n[4];
				int p = (int) n[6];
				double peak = 1e-9;
				for (int k = p; k < Math.min(frames, p + 4); k++) peak = Math.max(peak, energy[k][band]);
				int end = p;
				int limit = (int) Math.min(frames - 1, Math.min((next - 180 - CENTER_MS) / FRAME_MS, p + 2400 / FRAME_MS));
				while (end < limit && energy[end][band] >= 0.45 * peak) end++;
				double holdMs = (end - p) * FRAME_MS;
				double sustain = 0;
				int count = 0;
				for (int k = p + (int) (120 / FRAME_MS); k < Math.min(frames, p + (int) (360 / FRAME_MS)); k++) {
					sustain += energy[k][band];
					count++;
				}
				if (holdMs >= 350 && count > 0 && sustain / count >= 0.55 * peak) {
					length = (int) Math.min(holdMs, next - time - 180);
					if (length < 350) length = 0;
				}
			}

			notes.add(new RhythmChart.Note((int) Math.round(time), lane, length, tier));
			laneLast[lane] = time + length;
			lastTime = time;
			previousLane = lane;
			if (tier == 0 && n[2] >= chordCut && n[5] >= 3 && length == 0) {
				int partner = (lane + 2) % 4;
				if (time - laneLast[partner] >= 140) {
					notes.add(new RhythmChart.Note((int) Math.round(time), partner, 0, 2));
					laneLast[partner] = time;
				}
			}
		}
		if (notes.size() < 20) return null;
		return new RhythmChart(id, item, sound, (float) bpm, (int) durationMs, notes);
	}

	private static double[] column(double[][] matrix, int col) {
		double[] out = new double[matrix.length];
		for (int i = 0; i < matrix.length; i++) out[i] = matrix[i][col];
		return out;
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

	private static double estimatePeriod(double[] novelty) {
		int n = novelty.length;
		double mean = 0;
		for (double v : novelty) mean += v;
		mean /= n;
		double[] v = new double[n];
		for (int i = 0; i < n; i++) v[i] = novelty[i] - mean;
		int maxLag = (int) Math.ceil(60000.0 / 70.0 / FRAME_MS) + 2;
		int minLag = (int) Math.floor(60000.0 / 190.0 / FRAME_MS) - 1;
		double[] ac = new double[maxLag + 2];
		for (int lag = Math.max(1, minLag); lag <= maxLag + 1 && lag < n; lag++) {
			double sum = 0;
			for (int i = 0; i + lag < n; i++) sum += v[i] * v[i + lag];
			ac[lag] = sum;
		}
		double best = -1, bestScore = -Double.MAX_VALUE;
		for (double bpm = 70.0; bpm < 190.0; bpm += 0.25) {
			double lag = 60000.0 / bpm / FRAME_MS;
			int i = (int) lag;
			double frac = lag - i;
			if (i + 1 >= ac.length) continue;
			double score = ac[i] * (1 - frac) + ac[i + 1] * frac;
			double prior = Math.exp(-0.5 * Math.pow(Math.log(bpm / 120.0) / Math.log(2) / 0.9, 2));
			score *= prior;
			if (score > bestScore) {
				bestScore = score;
				best = lag;
			}
		}
		return best;
	}

	private static double[] trackBeats(double[] novelty, double period) {
		int n = novelty.length;
		double[] score = novelty.clone();
		int[] back = new int[n];
		Arrays.fill(back, -1);
		int loOff = (int) Math.round(period * 2);
		int hiOff = (int) Math.round(period / 2);
		for (int t = 0; t < n; t++) {
			int lo = Math.max(0, t - loOff), hi = t - hiOff;
			if (hi <= lo) continue;
			double best = -Double.MAX_VALUE;
			int bestIdx = -1;
			for (int prev = lo; prev < hi; prev++) {
				double cost = -100.0 * Math.pow(Math.log((t - prev) / period), 2);
				double cand = score[prev] + cost;
				if (cand > best) {
					best = cand;
					bestIdx = prev;
				}
			}
			score[t] = novelty[t] + best;
			back[t] = bestIdx;
		}
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

	private static int[] pickPeaks(double[] novelty, int minGap) {
		int n = novelty.length;
		int window = (int) (500 / FRAME_MS);
		int half = window / 2;
		double p99 = percentile(novelty, 99);
		double[] buffer = new double[window];
		List<Integer> peaks = new ArrayList<>();
		for (int t = 1; t < n - 1; t++) {
			for (int k = 0; k < window; k++) {
				int idx = Math.max(0, Math.min(n - 1, t - half + k));
				buffer[k] = novelty[idx];
			}
			double[] sorted = buffer.clone();
			Arrays.sort(sorted);
			double median = sorted[window / 2];
			double threshold = median * 1.25 + 0.08 * p99;
			if (novelty[t] > threshold && novelty[t] >= novelty[t - 1] && novelty[t] > novelty[t + 1]) {
				if (!peaks.isEmpty() && t - peaks.get(peaks.size() - 1) < minGap) {
					if (novelty[t] > novelty[peaks.get(peaks.size() - 1)]) peaks.set(peaks.size() - 1, t);
					continue;
				}
				peaks.add(t);
			}
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
		int full = chart.notes.size();
		if (full < seconds * 0.5 || full > seconds * 8) return "density";
		int tierZero = 0;
		int[] laneLast = {-100000, -100000, -100000, -100000};
		for (RhythmChart.Note note : chart.notes) {
			if (note.timeMs() < 0 || note.timeMs() > chart.durationMs) return "time";
			if (note.lengthMs() > 3000) return "hold";
			if (note.tier() == 0) tierZero++;
			if (note.timeMs() - laneLast[note.lane()] < 90) return "spacing";
			laneLast[note.lane()] = note.timeMs() + note.lengthMs();
		}
		if (tierZero < full * 0.08) return "tiers";
		return null;
	}
}
