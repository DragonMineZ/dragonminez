package com.dragonminez.common.training;

import java.util.Arrays;
import java.util.Base64;

public final class RhythmAudioPrint {
	public static final int BLOCK_MS = 500;
	private static final double DB_FLOOR = -100.0;
	private static final double DB_STEP = 0.5;
	private static final double ACTIVE_DB = -60.0;
	private static final int MAX_DURATION_DIFF_MS = 600;
	private static final double MAX_MEAN_ABS_DB = 1.5;
	private static final double MIN_CORRELATION = 0.95;
	private static final double MIN_ACTIVE_FRACTION = 0.5;

	private RhythmAudioPrint() {}

	public static String compute(float[] left, float[] right, int sampleRate) {
		int block = Math.max(1, sampleRate * BLOCK_MS / 1000);
		int blocks = left.length / block;
		byte[] out = new byte[blocks];
		for (int b = 0; b < blocks; b++) {
			double sum = 0;
			int start = b * block;
			for (int i = start; i < start + block; i++) {
				double v = right != null && right.length == left.length ? (left[i] + right[i]) * 0.5 : left[i];
				sum += v * v;
			}
			double db = 10.0 * Math.log10(sum / block + 1e-12);
			int q = (int) Math.round((Math.max(DB_FLOOR, Math.min(DB_FLOOR + 255 * DB_STEP, db)) - DB_FLOOR) / DB_STEP);
			out[b] = (byte) q;
		}
		return Base64.getEncoder().encodeToString(out);
	}

	private static double[] decodeDb(String print) {
		byte[] raw = Base64.getDecoder().decode(print);
		double[] db = new double[raw.length];
		for (int i = 0; i < raw.length; i++) db[i] = DB_FLOOR + (raw[i] & 0xFF) * DB_STEP;
		return db;
	}

	public static String compare(String expectedPrint, int expectedDurationMs, String actualPrint, int actualDurationMs) {
		if (expectedPrint == null || expectedPrint.isEmpty() || actualPrint == null || actualPrint.isEmpty()) return "missing";
		if (Math.abs(expectedDurationMs - actualDurationMs) > MAX_DURATION_DIFF_MS) return "duration";
		double[] a, b;
		try {
			a = decodeDb(expectedPrint);
			b = decodeDb(actualPrint);
		} catch (IllegalArgumentException e) {
			return "format";
		}
		int n = Math.min(a.length, b.length);
		if (n < 8 || Math.abs(a.length - b.length) > 2) return "length";
		double[] diff = new double[n];
		int active = 0;
		for (int i = 0; i < n; i++) {
			if (Math.max(a[i], b[i]) < ACTIVE_DB) continue;
			diff[active++] = a[i] - b[i];
		}
		if (active < MIN_ACTIVE_FRACTION * n) return "silent";
		double[] sorted = Arrays.copyOf(diff, active);
		Arrays.sort(sorted);
		double offset = sorted[active / 2];
		double meanAbs = 0, ma = 0, mb = 0;
		int k = 0;
		double[] xa = new double[active], xb = new double[active];
		for (int i = 0; i < n; i++) {
			if (Math.max(a[i], b[i]) < ACTIVE_DB) continue;
			xa[k] = a[i];
			xb[k] = b[i] + offset;
			meanAbs += Math.abs(xa[k] - xb[k]);
			ma += xa[k];
			mb += xb[k];
			k++;
		}
		meanAbs /= active;
		ma /= active;
		mb /= active;
		double cov = 0, va = 0, vb = 0;
		for (int i = 0; i < active; i++) {
			cov += (xa[i] - ma) * (xb[i] - mb);
			va += (xa[i] - ma) * (xa[i] - ma);
			vb += (xb[i] - mb) * (xb[i] - mb);
		}
		double corr = va > 0 && vb > 0 ? cov / Math.sqrt(va * vb) : 0;
		if (meanAbs > MAX_MEAN_ABS_DB) return String.format(java.util.Locale.ROOT, "level(%.2f dB)", meanAbs);
		if (corr < MIN_CORRELATION) return String.format(java.util.Locale.ROOT, "shape(%.3f)", corr);
		return null;
	}

	public static double[] stats(String expectedPrint, String actualPrint) {
		double[] a = decodeDb(expectedPrint), b = decodeDb(actualPrint);
		int n = Math.min(a.length, b.length);
		double[] diff = new double[n];
		int active = 0;
		for (int i = 0; i < n; i++) if (Math.max(a[i], b[i]) >= ACTIVE_DB) diff[active++] = a[i] - b[i];
		double[] sorted = Arrays.copyOf(diff, Math.max(1, active));
		Arrays.sort(sorted);
		double offset = sorted[sorted.length / 2];
		double meanAbs = 0, ma = 0, mb = 0;
		double[] xa = new double[active], xb = new double[active];
		int k = 0;
		for (int i = 0; i < n; i++) {
			if (Math.max(a[i], b[i]) < ACTIVE_DB) continue;
			xa[k] = a[i];
			xb[k] = b[i] + offset;
			meanAbs += Math.abs(xa[k] - xb[k]);
			ma += xa[k];
			mb += xb[k];
			k++;
		}
		if (active == 0) return new double[]{0, 0};
		meanAbs /= active;
		ma /= active;
		mb /= active;
		double cov = 0, va = 0, vb = 0;
		for (int i = 0; i < active; i++) {
			cov += (xa[i] - ma) * (xb[i] - mb);
			va += (xa[i] - ma) * (xa[i] - ma);
			vb += (xb[i] - mb) * (xb[i] - mb);
		}
		return new double[]{meanAbs, va > 0 && vb > 0 ? cov / Math.sqrt(va * vb) : 0};
	}
}
