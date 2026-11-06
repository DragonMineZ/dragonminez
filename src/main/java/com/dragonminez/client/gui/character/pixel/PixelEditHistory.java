package com.dragonminez.client.gui.character.pixel;

import java.util.ArrayDeque;
import java.util.Deque;

final class PixelEditHistory {
	record Snapshot(int size, byte[] data) {}

	private final int limit;
	private final Deque<Snapshot> undo = new ArrayDeque<>();
	private final Deque<Snapshot> redo = new ArrayDeque<>();

	PixelEditHistory(int limit) {
		this.limit = Math.max(1, limit);
	}

	void record(Snapshot before) {
		undo.push(before);
		trim(undo);
		redo.clear();
	}

	Snapshot undo(Snapshot current) {
		if (undo.isEmpty()) return null;
		redo.push(current);
		trim(redo);
		return undo.pop();
	}

	Snapshot redo(Snapshot current) {
		if (redo.isEmpty()) return null;
		undo.push(current);
		trim(undo);
		return redo.pop();
	}

	boolean canUndo() {
		return !undo.isEmpty();
	}

	boolean canRedo() {
		return !redo.isEmpty();
	}

	void clear() {
		undo.clear();
		redo.clear();
	}

	private void trim(Deque<Snapshot> stack) {
		while (stack.size() > limit) stack.removeLast();
	}
}
