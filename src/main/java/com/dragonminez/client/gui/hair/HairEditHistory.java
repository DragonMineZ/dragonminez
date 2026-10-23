package com.dragonminez.client.gui.hair;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairStyleSlot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;

final class HairEditHistory {
	private static final int MAX_ENTRIES = 64;

	private final Deque<EnumMap<HairStyleSlot, CustomHair>> undo = new ArrayDeque<>();
	private final Deque<EnumMap<HairStyleSlot, CustomHair>> redo = new ArrayDeque<>();

	void record(EnumMap<HairStyleSlot, CustomHair> styles) {
		undo.push(snapshot(styles));
		while (undo.size() > MAX_ENTRIES) undo.removeLast();
		redo.clear();
	}

	boolean canUndo() {
		return !undo.isEmpty();
	}

	boolean canRedo() {
		return !redo.isEmpty();
	}

	boolean undo(EnumMap<HairStyleSlot, CustomHair> styles) {
		if (undo.isEmpty()) return false;
		redo.push(snapshot(styles));
		restore(styles, undo.pop());
		return true;
	}

	boolean redo(EnumMap<HairStyleSlot, CustomHair> styles) {
		if (redo.isEmpty()) return false;
		undo.push(snapshot(styles));
		restore(styles, redo.pop());
		return true;
	}

	static EnumMap<HairStyleSlot, CustomHair> snapshot(EnumMap<HairStyleSlot, CustomHair> styles) {
		EnumMap<HairStyleSlot, CustomHair> copy = new EnumMap<>(HairStyleSlot.class);
		for (HairStyleSlot slot : HairStyleSlot.values()) copy.put(slot, styles.get(slot).copy());
		return copy;
	}

	private static void restore(EnumMap<HairStyleSlot, CustomHair> target, EnumMap<HairStyleSlot, CustomHair> source) {
		for (HairStyleSlot slot : HairStyleSlot.values()) target.get(slot).copyFrom(source.get(slot));
	}
}
