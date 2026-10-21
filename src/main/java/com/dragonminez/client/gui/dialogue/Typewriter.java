package com.dragonminez.client.gui.dialogue;

import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
final class Typewriter {
	private static final double CHAR_MS = 1000.0 / 68.0;
	private static final double SENTENCE_PAUSE_MS = 240.0;
	private static final double CLAUSE_PAUSE_MS = 85.0;
	private static final double START_DELAY_MS = 110.0;

	private List<FormattedCharSequence> lines = List.of();
	private int[] lineStarts = new int[0];
	private int[] codePoints = new int[0];
	private int revealed;
	private double nextAtMs;

	void start(List<FormattedCharSequence> newLines, long now) {
		load(newLines);
		revealed = 0;
		nextAtMs = now + START_DELAY_MS;
	}

	void replace(List<FormattedCharSequence> newLines) {
		load(newLines);
		revealed = Math.min(revealed, codePoints.length);
	}

	void finish() {
		revealed = codePoints.length;
	}

	boolean finished() {
		return revealed >= codePoints.length;
	}

	void update(long now) {
		if (finished()) return;
		if (nextAtMs < now - 100.0) nextAtMs = now - 100.0;
		while (revealed < codePoints.length && now >= nextAtMs) {
			int current = codePoints[revealed];
			int next = revealed + 1 < codePoints.length ? codePoints[revealed + 1] : ' ';
			revealed++;
			nextAtMs += delayAfter(current, next);
		}
	}

	List<FormattedCharSequence> lines() {
		return lines;
	}

	int cursorLine() {
		for (int i = lines.size() - 1; i >= 0; i--) {
			if (revealed > lineStarts[i]) return i;
		}
		return 0;
	}

	FormattedCharSequence visible(int index) {
		FormattedCharSequence line = lines.get(index);
		int start = lineStarts[index];
		int length = (index + 1 < lineStarts.length ? lineStarts[index + 1] : codePoints.length) - start;
		int shown = Math.max(0, Math.min(length, revealed - start));
		if (shown >= length) return line;
		if (shown <= 0) return FormattedCharSequence.EMPTY;
		return sink -> {
			int[] count = {0};
			return line.accept((position, style, codePoint) -> count[0]++ < shown && sink.accept(position, style, codePoint));
		};
	}

	private void load(List<FormattedCharSequence> newLines) {
		lines = newLines;
		lineStarts = new int[newLines.size()];
		List<Integer> points = new ArrayList<>();
		for (int i = 0; i < newLines.size(); i++) {
			lineStarts[i] = points.size();
			newLines.get(i).accept((position, style, codePoint) -> {
				points.add(codePoint);
				return true;
			});
			if (i + 1 < newLines.size()) points.add((int) ' ');
		}
		codePoints = points.stream().mapToInt(Integer::intValue).toArray();
	}

	private static double delayAfter(int current, int next) {
		boolean breakFollows = next == ' ' || next == '\n';
		if (breakFollows && (current == '.' || current == '!' || current == '?' || current == '\u2026')) return SENTENCE_PAUSE_MS;
		if (breakFollows && (current == ',' || current == ';' || current == ':')) return CLAUSE_PAUSE_MS;
		return CHAR_MS;
	}
}
