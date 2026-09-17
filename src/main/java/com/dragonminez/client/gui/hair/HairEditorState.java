package com.dragonminez.client.gui.hair;

import com.dragonminez.client.render.hair.HairEntityState;
import com.dragonminez.client.render.hair.HairHighlight;
import com.dragonminez.client.render.hair.HairPickRecorder;
import com.dragonminez.client.render.hair.HairRenderContext;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairLimits;
import com.dragonminez.common.hair.HairStrand;
import com.dragonminez.common.hair.HairStyleSlot;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

final class HairEditorState implements HairRenderContext.Preview {
	private final EnumMap<HairStyleSlot, CustomHair> styles;
	private final EnumMap<HairStyleSlot, CompoundTag> initialTags = new EnumMap<>(HairStyleSlot.class);
	private final HairEditHistory history = new HairEditHistory();
	private final HairEntityState simulation = new HairEntityState();
	private final HairPickRecorder pickRecorder = new HairPickRecorder();
	private final Set<Integer> hidden = new HashSet<>();
	private final Set<Integer> locked = new HashSet<>();
	private final HairLimits limits = HairLimits.current();
	private final boolean initialHairBase;

	private HairStyleSlot slot = HairStyleSlot.BASE;
	private CustomHair.HairFace face = CustomHair.HairFace.FRONT;
	private CustomHair.HairFace selectedFace;
	private int selectedIndex = -1;
	private int selectedSegment = -1;
	private CustomHair.HairFace hoverFace;
	private int hoverIndex = -1;
	private boolean mirror;
	private boolean physics = true;
	private boolean hairBase;
	private boolean gestureActive;
	private long selectionStartMs = Util.getMillis();
	private HairStrand strandClipboard;
	private boolean forceDirty;
	private long dirtyCacheKey = Long.MIN_VALUE;
	private boolean dirtyCache;

	HairEditorState(EnumMap<HairStyleSlot, CustomHair> styles, boolean hairBase, boolean forceDirty) {
		this.styles = styles;
		this.hairBase = hairBase;
		this.initialHairBase = hairBase;
		this.forceDirty = forceDirty;
		for (HairStyleSlot styleSlot : HairStyleSlot.values()) initialTags.put(styleSlot, styles.get(styleSlot).save());
	}

	public CustomHair style() {
		return styles.get(slot);
	}

	public HairStyleSlot slot() {
		return slot;
	}

	public boolean physicsEnabled() {
		return physics;
	}

	public HairEntityState simulationState() {
		return simulation;
	}

	public HairPickRecorder pickRecorder() {
		return pickRecorder;
	}

	public boolean isHidden(CustomHair.HairFace strandFace, int index) {
		return hidden.contains(HairEntityState.flatIndex(strandFace, index));
	}

	public float highlight(CustomHair.HairFace strandFace, int index, int segment) {
		if (strandFace == selectedFace && index == selectedIndex) {
			float pulse = HairHighlight.envelope(Util.getMillis() - selectionStartMs);
			if (selectedSegment < 0) return pulse;
			return segment == selectedSegment ? pulse : HairHighlight.SEGMENT_CONTEXT_TINT;
		}
		if (strandFace == hoverFace && index == hoverIndex) return HairHighlight.HOVER_TINT;
		return 0.0f;
	}

	EnumMap<HairStyleSlot, CustomHair> styles() {
		return styles;
	}

	HairLimits limits() {
		return limits;
	}

	void setSlot(HairStyleSlot newSlot) {
		if (newSlot == slot) return;
		slot = newSlot;
		clampSegmentSelection();
		restartPulse();
	}

	CustomHair.HairFace face() {
		return face;
	}

	void setFace(CustomHair.HairFace newFace) {
		face = newFace;
	}

	void select(CustomHair.HairFace strandFace, int index) {
		selectedFace = strandFace;
		selectedIndex = index;
		selectedSegment = -1;
		if (strandFace != null) face = strandFace;
		restartPulse();
	}

	void selectSegment(int segment) {
		selectedSegment = segment;
		clampSegmentSelection();
		restartPulse();
	}

	void clearSelection() {
		selectedFace = null;
		selectedIndex = -1;
		selectedSegment = -1;
	}

	boolean hasSelection() {
		return selectedFace != null && selectedIndex >= 0;
	}

	CustomHair.HairFace selectedFace() {
		return selectedFace;
	}

	int selectedIndex() {
		return selectedIndex;
	}

	int selectedSegment() {
		return selectedSegment;
	}

	HairStrand selectedStrand() {
		return hasSelection() ? style().getStrand(selectedFace, selectedIndex) : null;
	}

	void setHover(CustomHair.HairFace strandFace, int index) {
		hoverFace = strandFace;
		hoverIndex = index;
	}

	boolean isLocked(CustomHair.HairFace strandFace, int index) {
		return locked.contains(HairEntityState.flatIndex(strandFace, index));
	}

	void toggleHidden(CustomHair.HairFace strandFace, int index) {
		int flat = HairEntityState.flatIndex(strandFace, index);
		if (!hidden.remove(flat)) hidden.add(flat);
	}

	void toggleLocked(CustomHair.HairFace strandFace, int index) {
		int flat = HairEntityState.flatIndex(strandFace, index);
		if (!locked.remove(flat)) locked.add(flat);
	}

	boolean isMirror() {
		return mirror;
	}

	void setMirror(boolean value) {
		mirror = value;
	}

	void setPhysics(boolean value) {
		physics = value;
		if (!value) simulation.invalidateSimulation();
	}

	boolean isHairBase() {
		return hairBase;
	}

	void setHairBase(boolean value) {
		hairBase = value;
	}

	boolean hairBaseChanged() {
		return hairBase != initialHairBase;
	}

	void beginGesture() {
		if (gestureActive) return;
		history.record(styles);
		gestureActive = true;
	}

	void endGesture() {
		gestureActive = false;
	}

	void recordStep() {
		history.record(styles);
	}

	boolean undo() {
		if (!history.undo(styles)) return false;
		clampSegmentSelection();
		return true;
	}

	boolean redo() {
		if (!history.redo(styles)) return false;
		clampSegmentSelection();
		return true;
	}

	boolean canUndo() {
		return history.canUndo();
	}

	boolean canRedo() {
		return history.canRedo();
	}

	void modifySelected(Consumer<HairStrand> action) {
		HairStrand strand = selectedStrand();
		if (strand == null) return;
		action.accept(strand);
		propagateMirror();
		clampSegmentSelection();
		style().markChanged();
	}

	void propagateMirror() {
		if (!mirror || !hasSelection()) return;
		HairStrand source = selectedStrand();
		HairStrand target = style().getMirrorStrand(selectedFace, selectedIndex);
		if (source == null || target == null) return;
		target.copyFrom(source);
		target.mirror();
	}

	void copyStrand() {
		HairStrand strand = selectedStrand();
		if (strand != null) strandClipboard = strand.copy();
	}

	boolean hasStrandClipboard() {
		return strandClipboard != null;
	}

	void pasteStrand() {
		if (strandClipboard == null || !hasSelection()) return;
		recordStep();
		modifySelected(strand -> strand.copyFrom(strandClipboard));
	}

	void clearSelectedStrand() {
		if (!hasSelection()) return;
		recordStep();
		CustomHair.HairFace strandFace = selectedFace;
		int index = selectedIndex;
		modifySelected(strand -> strand.copyFrom(CustomHair.createDefaultStrand(strandFace, index)));
		selectedSegment = -1;
	}

	void copyStrandToOtherStyles() {
		HairStrand strand = selectedStrand();
		if (strand == null) return;
		recordStep();
		for (HairStyleSlot other : HairStyleSlot.values()) {
			if (other == slot) continue;
			styles.get(other).setStrand(selectedFace, selectedIndex, strand);
			if (mirror) {
				HairStrand target = styles.get(other).getMirrorStrand(selectedFace, selectedIndex);
				if (target != null) {
					target.copyFrom(strand);
					target.mirror();
				}
			}
		}
	}

	void copyStyleFrom(HairStyleSlot source) {
		if (source == slot) return;
		recordStep();
		style().copyFrom(styles.get(source));
		clampSegmentSelection();
	}

	void clearStyle() {
		recordStep();
		style().clear();
		selectedSegment = -1;
	}

	void replaceStyles(EnumMap<HairStyleSlot, CustomHair> imported) {
		recordStep();
		for (HairStyleSlot styleSlot : HairStyleSlot.values()) {
			CustomHair source = imported.get(styleSlot);
			if (source != null) styles.get(styleSlot).copyFrom(source);
		}
		clampSegmentSelection();
	}

	void replaceCurrentStyle(CustomHair imported) {
		recordStep();
		style().copyFrom(imported);
		clampSegmentSelection();
	}

	boolean isDirty() {
		if (forceDirty || hairBaseChanged()) return true;
		long key = 0L;
		for (HairStyleSlot styleSlot : HairStyleSlot.values()) key = key * 31L + styles.get(styleSlot).getRevision();
		if (key != dirtyCacheKey) {
			dirtyCacheKey = key;
			dirtyCache = false;
			for (HairStyleSlot styleSlot : HairStyleSlot.values()) {
				if (isSlotDirty(styleSlot)) {
					dirtyCache = true;
					break;
				}
			}
		}
		return dirtyCache;
	}

	boolean isSlotDirty(HairStyleSlot styleSlot) {
		return forceDirty || !styles.get(styleSlot).save().equals(initialTags.get(styleSlot));
	}

	private void clampSegmentSelection() {
		HairStrand strand = selectedStrand();
		if (strand == null || !strand.isVisible()) {
			selectedSegment = -1;
			return;
		}
		if (selectedSegment >= strand.getSegments()) selectedSegment = strand.getSegments() - 1;
	}

	private void restartPulse() {
		selectionStartMs = Util.getMillis();
	}
}
