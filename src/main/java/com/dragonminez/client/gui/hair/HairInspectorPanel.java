package com.dragonminez.client.gui.hair;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairJointStyle;
import com.dragonminez.common.hair.HairSegmentOverride;
import com.dragonminez.common.hair.HairStrand;
import com.dragonminez.common.hair.HairStrandSizing;
import com.dragonminez.common.hair.HairStyleSlot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class HairInspectorPanel implements HairInspectorRows.Context {
	private static final int HEADER_HEIGHT = 27;
	private static final int INSET = 8;
	private static final int SCROLL_STEP = 14;
	private static final int SCROLLBAR_WIDTH = 4;
	private static final int SCROLLBAR_HIT_PADDING = 2;
	private static final int MIN_THUMB_HEIGHT = 12;
	private static final float OFFSET_STEP = 0.05f;
	private static final float ANGLE_STEP = 1.0f;
	private static final float MAX_BEND = 720.0f;
	private static final float MAX_TWIST = 360.0f;
	private static final float MAX_SEGMENT_SCALE = 4.0f;

	interface ColorPickerOpener {
		void open(HairInspectorRows.ColorRow row, int anchorX, int anchorY);
	}

	private final HairEditorState state;
	private final Font font;
	private final ColorPickerOpener colorPickerOpener;
	private final List<HairInspectorRows.Row> rows = new ArrayList<>();
	private int x;
	private int y;
	private int width;
	private int height;
	private int scroll;
	private int contentHeight;
	private boolean rebuildRequested = true;
	private HairInspectorRows.Row dragging;
	private boolean draggingScrollbar;
	private int scrollbarGrab;
	private HairInspectorRows.NumberRow editing;
	private final StringBuilder editText = new StringBuilder();
	private HairStyleSlot copySource = HairStyleSlot.BASE;

	HairInspectorPanel(HairEditorState state, Font font, ColorPickerOpener colorPickerOpener) {
		this.state = state;
		this.font = font;
		this.colorPickerOpener = colorPickerOpener;
	}

	void setBounds(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		clampScroll();
	}

	int x() {
		return x;
	}

	boolean contains(double mouseX, double mouseY) {
		return HairEditorUi.inside(mouseX, mouseY, x, y, width, height);
	}

	public HairEditorState state() {
		return state;
	}

	public void beginTextEdit(HairInspectorRows.NumberRow row) {
		commitTextEdit();
		editing = row;
		editText.setLength(0);
		editText.append(row.currentText());
	}

	public boolean isEditing(HairInspectorRows.NumberRow row) {
		return editing == row;
	}

	public String editText() {
		return editText.toString();
	}

	public void openColorPicker(HairInspectorRows.ColorRow row, int anchorX, int anchorY) {
		colorPickerOpener.open(row, anchorX, anchorY - scroll);
	}

	public void requestRebuild() {
		rebuildRequested = true;
	}

	boolean isEditingText() {
		return editing != null;
	}

	void render(GuiGraphics graphics, int mouseX, int mouseY, ScissorBox scissor) {
		if (rebuildRequested) rebuild();
		HairEditorUi.panel(graphics, x, y, width, height);
		HairEditorUi.header(graphics, font, HairEditorUi.tr("gui.dragonminez.hair_editor.inspector"), x, y, width);

		int top = y + HEADER_HEIGHT;
		int visible = visibleHeight();
		scissor.enable(graphics, x + INSET, top, x + width - INSET, top + visible);
		int rowY = top - scroll;
		for (HairInspectorRows.Row row : rows) {
			if (rowY + row.height() >= top && rowY <= top + visible) {
				row.render(graphics, font, rowX(), rowY, rowWidth(), mouseX, mouseY, this);
			}
			rowY += row.height();
		}
		scissor.disable(graphics);

		if (hasScrollbar()) {
			int trackX = trackX();
			graphics.fill(trackX, top, trackX + SCROLLBAR_WIDTH, top + visible, 0x60000000);
			int thumbY = thumbY();
			boolean highlighted = draggingScrollbar || insideScrollbar(mouseX, mouseY);
			graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight(), highlighted ? HairEditorUi.HOVER_TEXT : HairEditorUi.DIVIDER);
		}
	}

	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!contains(mouseX, mouseY)) {
			commitTextEdit();
			return false;
		}
		int top = y + HEADER_HEIGHT;
		if (mouseY < top) return true;
		if (button == 0 && insideScrollbar(mouseX, mouseY)) {
			commitTextEdit();
			int thumbY = thumbY();
			boolean onThumb = mouseY >= thumbY && mouseY < thumbY + thumbHeight();
			scrollbarGrab = onThumb ? (int) Math.round(mouseY) - thumbY : thumbHeight() / 2;
			draggingScrollbar = true;
			scrollToMouse(mouseY);
			return true;
		}
		int rowY = top - scroll;
		HairInspectorRows.NumberRow previousEditing = editing;
		for (HairInspectorRows.Row row : rows) {
			if (mouseY >= rowY && mouseY < rowY + row.height()) {
				if (row.mouseClicked(mouseX, mouseY, button, rowX(), rowY, rowWidth(), this)) {
					if (editing == previousEditing && editing != null && row != editing) commitTextEdit();
					dragging = row;
				}
				return true;
			}
			rowY += row.height();
		}
		commitTextEdit();
		return true;
	}

	boolean mouseDragged(double mouseX, double mouseY) {
		if (draggingScrollbar) {
			scrollToMouse(mouseY);
			return true;
		}
		if (dragging == null) return false;
		int rowY = y + HEADER_HEIGHT - scroll;
		for (HairInspectorRows.Row row : rows) {
			if (row == dragging) {
				row.mouseDragged(mouseX, mouseY, rowX(), rowY, rowWidth(), this);
				return true;
			}
			rowY += row.height();
		}
		return true;
	}

	boolean mouseReleased() {
		if (draggingScrollbar) {
			draggingScrollbar = false;
			return true;
		}
		if (dragging == null) return false;
		dragging.mouseReleased(this);
		dragging = null;
		return true;
	}

	boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (!contains(mouseX, mouseY)) return false;
		scroll -= (int) Math.round(delta * SCROLL_STEP);
		clampScroll();
		return true;
	}

	boolean keyPressed(int keyCode) {
		if (editing == null) return false;
		switch (keyCode) {
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if (!editText.isEmpty()) editText.setLength(editText.length() - 1);
			}
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_TAB -> commitTextEdit();
			case GLFW.GLFW_KEY_ESCAPE -> editing = null;
			default -> {
				return true;
			}
		}
		return true;
	}

	boolean charTyped(char character) {
		if (editing == null) return false;
		if ((Character.isDigit(character) || character == '.' || character == '-' || character == ',') && editText.length() < 12) {
			editText.append(character);
		}
		return true;
	}

	void commitTextEdit() {
		if (editing == null) return;
		HairInspectorRows.NumberRow row = editing;
		editing = null;
		row.commitText(editText.toString(), this);
	}

	private int rowX() {
		return x + INSET;
	}

	private int rowWidth() {
		return width - INSET * 2 - SCROLLBAR_WIDTH - 2;
	}

	private boolean hasScrollbar() {
		return contentHeight > visibleHeight();
	}

	private int trackX() {
		return x + width - INSET - SCROLLBAR_WIDTH;
	}

	private int thumbHeight() {
		int visible = visibleHeight();
		return Math.min(visible, Math.max(MIN_THUMB_HEIGHT, visible * visible / Math.max(1, contentHeight)));
	}

	private int thumbY() {
		int range = contentHeight - visibleHeight();
		float progress = range > 0 ? scroll / (float) range : 0.0f;
		return y + HEADER_HEIGHT + Math.round((visibleHeight() - thumbHeight()) * progress);
	}

	private boolean insideScrollbar(double mouseX, double mouseY) {
		if (!hasScrollbar()) return false;
		int top = y + HEADER_HEIGHT;
		return mouseX >= trackX() - SCROLLBAR_HIT_PADDING && mouseX < trackX() + SCROLLBAR_WIDTH + SCROLLBAR_HIT_PADDING
				&& mouseY >= top && mouseY < top + visibleHeight();
	}

	private void scrollToMouse(double mouseY) {
		int travel = visibleHeight() - thumbHeight();
		int range = contentHeight - visibleHeight();
		if (travel <= 0 || range <= 0) return;
		double thumbTop = mouseY - scrollbarGrab - (y + HEADER_HEIGHT);
		scroll = (int) Math.round(thumbTop / travel * range);
		clampScroll();
	}

	private int visibleHeight() {
		return height - HEADER_HEIGHT - INSET;
	}

	private void clampScroll() {
		int visible = visibleHeight();
		scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - visible)));
	}

	private void rebuild() {
		rebuildRequested = false;
		editing = null;
		dragging = null;
		draggingScrollbar = false;
		rows.clear();

		HairStrand strand = state.selectedStrand();
		if (strand == null) {
			buildStyleRows();
		} else if (!strand.isVisible()) {
			buildEmptySlotRows();
		} else {
			buildStrandRows();
			if (state.selectedSegment() >= 0) buildSegmentRows();
		}

		contentHeight = 0;
		for (HairInspectorRows.Row row : rows) contentHeight += row.height();
		clampScroll();
	}

	private void buildStyleRows() {
		HairStyleSlot slot = state.slot();
		rows.add(section(HairEditorUi.tr("gui.dragonminez.hair_editor.style." + slot.index())));
		rows.add(new HairInspectorRows.InfoRow(() -> HairEditorUi.tr("gui.dragonminez.hair_editor.info.strands", state.style().getVisibleStrandCount()), HairEditorUi.TEXT));
		rows.add(new HairInspectorRows.InfoRow(() -> HairEditorUi.tr("gui.dragonminez.hair_editor.info.segments", state.style().getTotalSegmentCount(), state.limits().getMaxTotalSegments()),
				state.style().getTotalSegmentCount() > state.limits().getMaxTotalSegments() ? HairEditorUi.WARNING : HairEditorUi.TEXT));
		if (copySource == slot) copySource = nextSlot(slot, slot);
		rows.add(new HairInspectorRows.ChoiceRow(HairEditorUi.tr("gui.dragonminez.hair_editor.copy_from"),
				() -> HairEditorUi.tr("gui.dragonminez.hair_editor.style." + copySource.index()),
				() -> copySource = nextSlot(copySource, state.slot()), null));
		rows.add(actions(
				action("gui.dragonminez.hair_editor.action.copy_style", () -> state.copyStyleFrom(copySource)),
				action("gui.dragonminez.hair_editor.action.clear_style", state::clearStyle)));
		rows.add(section(HairEditorUi.tr("gui.dragonminez.hair_editor.help")));
		rows.add(new HairInspectorRows.InfoRow(() -> HairEditorUi.tr("gui.dragonminez.hair_editor.help.select"), HairEditorUi.MUTED));
		rows.add(new HairInspectorRows.InfoRow(() -> HairEditorUi.tr("gui.dragonminez.hair_editor.help.grow"), HairEditorUi.MUTED));
		rows.add(new HairInspectorRows.InfoRow(() -> HairEditorUi.tr("gui.dragonminez.hair_editor.help.rotate"), HairEditorUi.MUTED));
		rows.add(new HairInspectorRows.InfoRow(() -> HairEditorUi.tr("gui.dragonminez.hair_editor.help.curve"), HairEditorUi.MUTED));
		rows.add(new HairInspectorRows.InfoRow(() -> HairEditorUi.tr("gui.dragonminez.hair_editor.help.segments"), HairEditorUi.MUTED));
		rows.add(new HairInspectorRows.InfoRow(() -> HairEditorUi.tr("gui.dragonminez.hair_editor.help.remove_segment"), HairEditorUi.MUTED));
		rows.add(new HairInspectorRows.InfoRow(() -> HairEditorUi.tr("gui.dragonminez.hair_editor.help.orbit"), HairEditorUi.MUTED));
		rows.add(new HairInspectorRows.InfoRow(() -> HairEditorUi.tr("gui.dragonminez.hair_editor.help.reset"), HairEditorUi.MUTED));
	}

	private void buildEmptySlotRows() {
		rows.add(section(strandTitle()));
		rows.add(new HairInspectorRows.InfoRow(() -> HairEditorUi.tr("gui.dragonminez.hair_editor.info.empty_slot"), HairEditorUi.MUTED));
		rows.add(actions(
				action("gui.dragonminez.hair_editor.action.add_strand", () -> {
					state.recordStep();
					state.modifySelected(HairStrand::makeVisibleWithDefaults);
				}),
				new HairInspectorRows.Action(HairEditorUi.tr("gui.dragonminez.hair_editor.action.paste"), state::pasteStrand, state::hasStrandClipboard)));
	}

	private void buildStrandRows() {
		HairStyleSlot slot = state.slot();
		float maxOffset = state.limits().getMaxRootOffset();
		float maxWidth = state.limits().getMaxWidth();

		rows.add(section(strandTitle()));
		rows.add(actions(
				action("gui.dragonminez.hair_editor.action.copy", state::copyStrand),
				new HairInspectorRows.Action(HairEditorUi.tr("gui.dragonminez.hair_editor.action.paste"), state::pasteStrand, state::hasStrandClipboard),
				action("gui.dragonminez.hair_editor.action.to_all_styles", state::copyStrandToOtherStyles),
				action("gui.dragonminez.hair_editor.action.clear", state::clearSelectedStrand)));

		rows.add(section(HairEditorUi.tr("gui.dragonminez.hair_editor.section.transform")));
		rows.add(number("offset_x", HairEditorUi.AXIS_X, -maxOffset, maxOffset, OFFSET_STEP, false, HairStrand::getOffsetX, (s, v) -> s.setOffset(v, s.getOffsetY(), s.getOffsetZ()), 0.0f));
		rows.add(number("offset_y", HairEditorUi.AXIS_Y, -maxOffset, maxOffset, OFFSET_STEP, false, HairStrand::getOffsetY, (s, v) -> s.setOffset(s.getOffsetX(), v, s.getOffsetZ()), 0.0f));
		rows.add(number("offset_z", HairEditorUi.AXIS_Z, -maxOffset, maxOffset, OFFSET_STEP, false, HairStrand::getOffsetZ, (s, v) -> s.setOffset(s.getOffsetX(), s.getOffsetY(), v), 0.0f));
		rows.add(number("rotation_x", HairEditorUi.AXIS_X, -180.0f, 180.0f, ANGLE_STEP, false, HairStrand::getRotationX, (s, v) -> s.setRotation(v, s.getRotationY(), s.getRotationZ()), baseRotation(0)));
		rows.add(number("rotation_y", HairEditorUi.AXIS_Y, -180.0f, 180.0f, ANGLE_STEP, false, HairStrand::getRotationY, (s, v) -> s.setRotation(s.getRotationX(), v, s.getRotationZ()), baseRotation(1)));
		rows.add(number("rotation_z", HairEditorUi.AXIS_Z, -180.0f, 180.0f, ANGLE_STEP, false, HairStrand::getRotationZ, (s, v) -> s.setRotation(s.getRotationX(), s.getRotationY(), v), baseRotation(2)));

		rows.add(section(HairEditorUi.tr("gui.dragonminez.hair_editor.section.shape")));
		int maxSegments = state.limits().getMaxSegments();
		float maxLength = state.limits().getMaxLength(slot);
		rows.add(number("segments", 0, 1.0f, maxSegments, 1.0f, true, strand -> (float) strand.getSegments(), (s, v) -> HairStrandSizing.resizeSegments(s, Math.round(v), maxSegments, maxLength), HairStrand.DEFAULT_SEGMENTS));
		rows.add(number("length", 0, HairStrandSizing.MIN_SEGMENT_LENGTH, maxLength, 0.1f, false, HairStrand::getLength, (s, v) -> HairStrandSizing.resizeLength(s, v, maxSegments, maxLength), HairStrand.DEFAULT_LENGTH));
		rows.add(number("length_falloff", 0, HairStrand.MIN_LENGTH_RATIO, 2.0f, 0.01f, false, HairStrand::getLengthRatio, HairStrand::setLengthRatio, 1.0f));
		rows.add(number("width", 0, 0.25f, maxWidth, OFFSET_STEP, false, HairStrand::getWidth, HairStrand::setWidth, HairStrand.DEFAULT_WIDTH));
		rows.add(number("depth", 0, 0.25f, maxWidth, OFFSET_STEP, false, HairStrand::getDepth, HairStrand::setDepth, HairStrand.DEFAULT_DEPTH));
		rows.add(number("taper", 0, HairStrand.MIN_TAPER, HairStrand.MAX_TAPER, 0.01f, false, HairStrand::getTaper, HairStrand::setTaper, 1.0f));
		rows.add(number("taper_curve", 0, HairStrand.MIN_TAPER_CURVE, HairStrand.MAX_TAPER_CURVE, 0.05f, false, HairStrand::getTaperCurve, HairStrand::setTaperCurve, 1.0f));
		rows.add(new HairInspectorRows.ChoiceRow(HairEditorUi.tr("gui.dragonminez.hair_editor.joints"),
				() -> HairEditorUi.tr("gui.dragonminez.hair_editor.joints." + selectedOr(HairStrand::getJointStyle, HairJointStyle.BLOCKS).name().toLowerCase()),
				() -> state.modifySelected(s -> s.setJointStyle(s.getJointStyle().next())),
				() -> state.modifySelected(s -> s.setJointStyle(HairJointStyle.BLOCKS))));

		rows.add(section(HairEditorUi.tr("gui.dragonminez.hair_editor.section.curve")));
		rows.add(number("bend_x", HairEditorUi.AXIS_X, -MAX_BEND, MAX_BEND, ANGLE_STEP, false, HairStrand::getBendX, (s, v) -> s.setBend(v, s.getBendY(), s.getBendZ()), 0.0f));
		rows.add(number("bend_y", HairEditorUi.AXIS_Y, -MAX_BEND, MAX_BEND, ANGLE_STEP, false, HairStrand::getBendY, (s, v) -> s.setBend(s.getBendX(), v, s.getBendZ()), 0.0f));
		rows.add(number("bend_z", HairEditorUi.AXIS_Z, -MAX_BEND, MAX_BEND, ANGLE_STEP, false, HairStrand::getBendZ, (s, v) -> s.setBend(s.getBendX(), s.getBendY(), v), 0.0f));
		rows.add(number("twist", 0, -MAX_TWIST, MAX_TWIST, ANGLE_STEP, false, HairStrand::getTwist, HairStrand::setTwist, 0.0f));

		rows.add(section(HairEditorUi.tr("gui.dragonminez.hair_editor.section.color")));
		rows.add(new HairInspectorRows.ColorRow(HairEditorUi.tr("gui.dragonminez.hair_editor.color"),
				() -> selectedOr(HairStrand::getColor, null),
				() -> state.style().getGlobalColor(),
				hex -> state.modifySelected(s -> s.setColor(hex))));
		rows.add(new HairInspectorRows.ColorRow(HairEditorUi.tr("gui.dragonminez.hair_editor.tip_color"),
				() -> selectedOr(HairStrand::getTipColor, null),
				() -> {
					String color = selectedOr(HairStrand::getColor, null);
					return color != null ? color : state.style().getGlobalColor();
				},
				hex -> state.modifySelected(s -> s.setTipColor(hex))));
	}

	private void buildSegmentRows() {
		int segment = state.selectedSegment();
		rows.add(section(HairEditorUi.tr("gui.dragonminez.hair_editor.segment", segment + 1)));
		rows.add(actions(action("gui.dragonminez.hair_editor.action.reset_segment", () -> {
			state.recordStep();
			state.modifySelected(s -> s.removeOverride(state.selectedSegment()));
		})));
		float maxOffset = HairSegmentOverride.MAX_OFFSET;
		rows.add(segmentNumber("offset_x", HairEditorUi.AXIS_X, -maxOffset, maxOffset, OFFSET_STEP, HairSegmentOverride::getOffsetX, (o, v) -> o.setOffset(v, o.getOffsetY(), o.getOffsetZ()), 0.0f));
		rows.add(segmentNumber("offset_y", HairEditorUi.AXIS_Y, -maxOffset, maxOffset, OFFSET_STEP, HairSegmentOverride::getOffsetY, (o, v) -> o.setOffset(o.getOffsetX(), v, o.getOffsetZ()), 0.0f));
		rows.add(segmentNumber("offset_z", HairEditorUi.AXIS_Z, -maxOffset, maxOffset, OFFSET_STEP, HairSegmentOverride::getOffsetZ, (o, v) -> o.setOffset(o.getOffsetX(), o.getOffsetY(), v), 0.0f));
		rows.add(segmentNumber("rotation_x", HairEditorUi.AXIS_X, -180.0f, 180.0f, ANGLE_STEP, HairSegmentOverride::getRotationX, (o, v) -> o.setRotation(v, o.getRotationY(), o.getRotationZ()), 0.0f));
		rows.add(segmentNumber("rotation_y", HairEditorUi.AXIS_Y, -180.0f, 180.0f, ANGLE_STEP, HairSegmentOverride::getRotationY, (o, v) -> o.setRotation(o.getRotationX(), v, o.getRotationZ()), 0.0f));
		rows.add(segmentNumber("rotation_z", HairEditorUi.AXIS_Z, -180.0f, 180.0f, ANGLE_STEP, HairSegmentOverride::getRotationZ, (o, v) -> o.setRotation(o.getRotationX(), o.getRotationY(), v), 0.0f));
		rows.add(segmentNumber("length_scale", 0, HairSegmentOverride.MIN_SCALE, MAX_SEGMENT_SCALE, 0.01f, HairSegmentOverride::getLengthScale, HairSegmentOverride::setLengthScale, 1.0f));
		rows.add(segmentNumber("width_scale", 0, HairSegmentOverride.MIN_SCALE, MAX_SEGMENT_SCALE, 0.01f, HairSegmentOverride::getWidthScale, HairSegmentOverride::setWidthScale, 1.0f));
		rows.add(segmentNumber("depth_scale", 0, HairSegmentOverride.MIN_SCALE, MAX_SEGMENT_SCALE, 0.01f, HairSegmentOverride::getDepthScale, HairSegmentOverride::setDepthScale, 1.0f));
		rows.add(new HairInspectorRows.ColorRow(HairEditorUi.tr("gui.dragonminez.hair_editor.color"),
				() -> {
					HairSegmentOverride override = selectedOverride();
					return override != null ? override.getColor() : null;
				},
				() -> {
					String color = selectedOr(HairStrand::getColor, null);
					return color != null ? color : state.style().getGlobalColor();
				},
				hex -> state.modifySelected(s -> s.getOrCreateOverride(state.selectedSegment()).setColor(hex))));
	}

	private HairInspectorRows.NumberRow number(String key, int axisColor, float min, float max, float step, boolean integer,
											   Function<HairStrand, Float> getter, BiConsumer<HairStrand, Float> setter, float defaultValue) {
		return new HairInspectorRows.NumberRow(HairEditorUi.tr("gui.dragonminez.hair_editor.property." + key), axisColor, min, max, step, integer,
				() -> selectedOr(getter, defaultValue),
				value -> state.modifySelected(strand -> setter.accept(strand, value)),
				defaultValue);
	}

	private HairInspectorRows.NumberRow segmentNumber(String key, int axisColor, float min, float max, float step,
													  Function<HairSegmentOverride, Float> getter, BiConsumer<HairSegmentOverride, Float> setter, float defaultValue) {
		return new HairInspectorRows.NumberRow(HairEditorUi.tr("gui.dragonminez.hair_editor.property." + key), axisColor, min, max, step, false,
				() -> {
					HairSegmentOverride override = selectedOverride();
					return override != null ? getter.apply(override) : defaultValue;
				},
				value -> state.modifySelected(strand -> {
					HairSegmentOverride override = strand.getOrCreateOverride(state.selectedSegment());
					setter.accept(override, value);
					if (override.isIdentity()) strand.removeOverride(override.getIndex());
				}),
				defaultValue);
	}

	private HairSegmentOverride selectedOverride() {
		HairStrand strand = state.selectedStrand();
		return strand != null && state.selectedSegment() >= 0 ? strand.getOverride(state.selectedSegment()) : null;
	}

	private <V> V selectedOr(Function<HairStrand, V> getter, V fallback) {
		HairStrand strand = state.selectedStrand();
		return strand != null ? getter.apply(strand) : fallback;
	}

	private float baseRotation(int axis) {
		CustomHair.HairFace face = state.selectedFace();
		if (face == null) return 0.0f;
		return CustomHair.getBaseRotation(face).get(axis);
	}

	private Component strandTitle() {
		CustomHair.HairFace face = state.selectedFace();
		return HairEditorUi.tr("gui.dragonminez.hair_editor.strand", face != null ? face.key : "-", state.selectedIndex() + 1);
	}

	private static HairInspectorRows.SectionRow section(Component label) {
		return new HairInspectorRows.SectionRow(label);
	}

	private static HairInspectorRows.Action action(String key, Runnable runnable) {
		return new HairInspectorRows.Action(HairEditorUi.tr(key), runnable, null);
	}

	private static HairInspectorRows.ActionRow actions(HairInspectorRows.Action... actions) {
		return new HairInspectorRows.ActionRow(List.of(actions));
	}

	private static HairStyleSlot nextSlot(HairStyleSlot current, HairStyleSlot excluded) {
		HairStyleSlot next = current;
		for (int i = 0; i < HairStyleSlot.count(); i++) {
			next = HairStyleSlot.byIndex((next.index() + 1) % HairStyleSlot.count());
			if (next != excluded) return next;
		}
		return current;
	}

	interface ScissorBox {
		void enable(GuiGraphics graphics, int x0, int y0, int x1, int y1);

		void disable(GuiGraphics graphics);
	}
}
