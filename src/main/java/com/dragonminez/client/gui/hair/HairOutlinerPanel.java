package com.dragonminez.client.gui.hair;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairStrand;
import com.dragonminez.common.hair.HairStyleSlot;
import com.dragonminez.client.util.TextUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class HairOutlinerPanel {
	private static final int HEADER_HEIGHT = 27;
	private static final int INSET = 9;
	private static final int TAB_HEIGHT = 14;
	private static final int TAB_GAP = 2;
	private static final int CELL_SIZE = 20;
	private static final int CELL_GAP = 3;
	private static final int ROW_HEIGHT = 12;
	private static final int BUTTON_HEIGHT = 14;
	private static final int CODE_AREA_HEIGHT = 2 * BUTTON_HEIGHT + 6;

	interface Listener {
		void onSelectionChanged();

		void onCodeAction(CodeAction action);
	}

	enum CodeAction {
		COPY_STYLE,
		PASTE_STYLE,
		COPY_FULL,
		PASTE_FULL
	}

	private final HairEditorState state;
	private final Font font;
	private final Listener listener;
	private int x;
	private int y;
	private int width;
	private int height;
	private int segmentScroll;

	HairOutlinerPanel(HairEditorState state, Font font, Listener listener) {
		this.state = state;
		this.font = font;
		this.listener = listener;
	}

	void setBounds(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	int right() {
		return x + width;
	}

	boolean contains(double mouseX, double mouseY) {
		return HairEditorUi.inside(mouseX, mouseY, x, y, width, height);
	}

	void render(GuiGraphics graphics, int mouseX, int mouseY) {
		HairEditorUi.panel(graphics, x, y, width, height);
		HairEditorUi.header(graphics, font, HairEditorUi.tr("gui.dragonminez.hair_editor.outliner"), x, y, width);

		int cursor = y + HEADER_HEIGHT + 2;
		renderStyleTabs(graphics, mouseX, mouseY, cursor);
		cursor += TAB_HEIGHT + 3;
		renderFaceTabs(graphics, mouseX, mouseY, cursor);
		cursor += TAB_HEIGHT + 4;
		cursor = renderGrid(graphics, mouseX, mouseY, cursor);
		cursor = renderStrandToolbar(graphics, mouseX, mouseY, cursor + 4);
		renderSegmentList(graphics, mouseX, mouseY, cursor + 3, codeAreaTop() - 3);
		renderCodeButtons(graphics, mouseX, mouseY);
	}

	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!contains(mouseX, mouseY)) return false;
		if (button != 0) return true;

		int cursor = y + HEADER_HEIGHT + 2;
		HairStyleSlot slot = tabHit(mouseX, mouseY, cursor, HairStyleSlot.count()) >= 0 ? HairStyleSlot.byIndex(tabHit(mouseX, mouseY, cursor, HairStyleSlot.count())) : null;
		if (slot != null) {
			state.setSlot(slot);
			HairEditorSounds.click();
			listener.onSelectionChanged();
			return true;
		}
		cursor += TAB_HEIGHT + 3;
		int faceIndex = tabHit(mouseX, mouseY, cursor, CustomHair.HairFace.values().length);
		if (faceIndex >= 0) {
			state.setFace(CustomHair.HairFace.values()[faceIndex]);
			HairEditorSounds.click();
			return true;
		}
		cursor += TAB_HEIGHT + 4;

		CustomHair.HairFace face = state.face();
		int gridX = gridX(face);
		for (int index = 0; index < face.maxStrands; index++) {
			int cellX = gridX + (index % face.cols) * (CELL_SIZE + CELL_GAP);
			int cellY = cursor + (index / face.cols) * (CELL_SIZE + CELL_GAP);
			if (HairEditorUi.inside(mouseX, mouseY, cellX, cellY, CELL_SIZE, CELL_SIZE)) {
				boolean alreadySelected = state.selectedFace() == face && state.selectedIndex() == index;
				if (alreadySelected) {
					state.clearSelection();
					HairEditorSounds.deselect();
				} else {
					state.select(face, index);
					HairEditorSounds.select();
				}
				listener.onSelectionChanged();
				return true;
			}
		}
		cursor += gridHeight(face) + 4;

		if (state.hasSelection()) {
			int buttonWidth = (width - INSET * 2 - TAB_GAP) / 2;
			if (HairEditorUi.inside(mouseX, mouseY, x + INSET, cursor, buttonWidth, BUTTON_HEIGHT)) {
				state.toggleHidden(state.selectedFace(), state.selectedIndex());
				HairEditorSounds.toggle(state.isHidden(state.selectedFace(), state.selectedIndex()));
				return true;
			}
			if (HairEditorUi.inside(mouseX, mouseY, x + INSET + buttonWidth + TAB_GAP, cursor, buttonWidth, BUTTON_HEIGHT)) {
				state.toggleLocked(state.selectedFace(), state.selectedIndex());
				HairEditorSounds.toggle(state.isLocked(state.selectedFace(), state.selectedIndex()));
				return true;
			}
			cursor += BUTTON_HEIGHT + 3;

			HairStrand strand = state.selectedStrand();
			if (strand != null && strand.isVisible()) {
				int listTop = cursor;
				int listBottom = codeAreaTop() - 3;
				if (mouseY >= listTop && mouseY < listBottom) {
					int row = (int) ((mouseY - listTop) / ROW_HEIGHT) + segmentScroll;
					if (row == 0) {
						state.selectSegment(-1);
						HairEditorSounds.select();
					} else if (row - 1 < strand.getSegments()) {
						state.selectSegment(row - 1);
						HairEditorSounds.select();
					}
					listener.onSelectionChanged();
					return true;
				}
			}
		}

		int codeTop = codeAreaTop();
		int buttonWidth = (width - INSET * 2 - TAB_GAP) / 2;
		CodeAction[] actions = CodeAction.values();
		for (int i = 0; i < actions.length; i++) {
			int buttonX = x + INSET + (i % 2) * (buttonWidth + TAB_GAP);
			int buttonY = codeTop + (i / 2) * (BUTTON_HEIGHT + TAB_GAP);
			if (HairEditorUi.inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, BUTTON_HEIGHT)) {
				HairEditorSounds.click();
				listener.onCodeAction(actions[i]);
				return true;
			}
		}
		return true;
	}

	boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (!contains(mouseX, mouseY)) return false;
		HairStrand strand = state.selectedStrand();
		int rows = strand != null && strand.isVisible() ? strand.getSegments() + 1 : 0;
		segmentScroll = Math.max(0, Math.min(Math.max(0, rows - 1), segmentScroll - (int) Math.signum(delta)));
		return true;
	}

	private void renderStyleTabs(GuiGraphics graphics, int mouseX, int mouseY, int top) {
		int count = HairStyleSlot.count();
		int tabWidth = tabWidth(count);
		for (int i = 0; i < count; i++) {
			HairStyleSlot slot = HairStyleSlot.byIndex(i);
			int tabX = x + INSET + i * (tabWidth + TAB_GAP);
			Component label = HairEditorUi.tr("gui.dragonminez.hair_editor.style.short." + i);
			boolean empty = state.styles().get(slot).isEmpty();
			HairEditorUi.button(graphics, font, label, tabX, top, tabWidth, TAB_HEIGHT, HairEditorUi.inside(mouseX, mouseY, tabX, top, tabWidth, TAB_HEIGHT), state.slot() == slot, true);
			if (!empty) graphics.fill(tabX + tabWidth - 4, top + 2, tabX + tabWidth - 2, top + 4, HairEditorUi.TITLE);
		}
	}

	private void renderFaceTabs(GuiGraphics graphics, int mouseX, int mouseY, int top) {
		CustomHair.HairFace[] faces = CustomHair.HairFace.values();
		int tabWidth = tabWidth(faces.length);
		for (int i = 0; i < faces.length; i++) {
			int tabX = x + INSET + i * (tabWidth + TAB_GAP);
			Component label = HairEditorUi.tr("gui.dragonminez.hair_editor.face." + faces[i].name().toLowerCase());
			HairEditorUi.button(graphics, font, label, tabX, top, tabWidth, TAB_HEIGHT, HairEditorUi.inside(mouseX, mouseY, tabX, top, tabWidth, TAB_HEIGHT), state.face() == faces[i], true);
		}
	}

	private int renderGrid(GuiGraphics graphics, int mouseX, int mouseY, int top) {
		CustomHair.HairFace face = state.face();
		CustomHair style = state.style();
		int gridX = gridX(face);
		for (int index = 0; index < face.maxStrands; index++) {
			int cellX = gridX + (index % face.cols) * (CELL_SIZE + CELL_GAP);
			int cellY = top + (index / face.cols) * (CELL_SIZE + CELL_GAP);
			HairStrand strand = style.getStrand(face, index);
			boolean visible = strand != null && strand.isVisible();
			boolean selected = state.selectedFace() == face && state.selectedIndex() == index;
			boolean hovered = HairEditorUi.inside(mouseX, mouseY, cellX, cellY, CELL_SIZE, CELL_SIZE);
			boolean hidden = state.isHidden(face, index);

			HairEditorTextures.Sprite sprite = selected ? HairEditorTextures.BUTTON_ACTIVE
					: (hovered ? HairEditorTextures.BUTTON_HOVER : (visible ? HairEditorTextures.BUTTON_ACCENT : HairEditorTextures.BUTTON));
			float tint = hidden ? 0.55f : 1.0f;
			HairEditorTextures.draw(graphics, sprite, cellX, cellY, CELL_SIZE, CELL_SIZE, tint, tint, tint, 1.0f);

			String number = Integer.toString(index + 1);
			int color = hidden ? HairEditorUi.DISABLED : (visible || selected ? HairEditorUi.TEXT : HairEditorUi.MUTED);
			TextUtil.drawStringWithBorder(graphics, font, number, cellX + (CELL_SIZE - font.width(number)) / 2 + 1, cellY + (CELL_SIZE - 8) / 2 + 1, color);
			if (hidden) graphics.fill(cellX + 3, cellY + CELL_SIZE / 2, cellX + CELL_SIZE - 3, cellY + CELL_SIZE / 2 + 1, HairEditorUi.WARNING);
			if (state.isLocked(face, index)) graphics.fill(cellX + CELL_SIZE - 5, cellY + 2, cellX + CELL_SIZE - 2, cellY + 5, HairEditorUi.SECTION);
		}
		return top + gridHeight(face);
	}

	private int renderStrandToolbar(GuiGraphics graphics, int mouseX, int mouseY, int top) {
		if (!state.hasSelection()) {
			TextUtil.drawStringWithBorder(graphics, font, HairEditorUi.tr("gui.dragonminez.hair_editor.no_selection"), x + INSET, top + 3, HairEditorUi.MUTED);
			return top + BUTTON_HEIGHT;
		}
		int buttonWidth = (width - INSET * 2 - TAB_GAP) / 2;
		boolean hidden = state.isHidden(state.selectedFace(), state.selectedIndex());
		boolean locked = state.isLocked(state.selectedFace(), state.selectedIndex());
		HairEditorUi.button(graphics, font, HairEditorUi.tr(hidden ? "gui.dragonminez.hair_editor.show" : "gui.dragonminez.hair_editor.hide"),
				x + INSET, top, buttonWidth, BUTTON_HEIGHT, HairEditorUi.inside(mouseX, mouseY, x + INSET, top, buttonWidth, BUTTON_HEIGHT), hidden, true);
		int lockX = x + INSET + buttonWidth + TAB_GAP;
		HairEditorUi.button(graphics, font, HairEditorUi.tr(locked ? "gui.dragonminez.hair_editor.unlock" : "gui.dragonminez.hair_editor.lock"),
				lockX, top, buttonWidth, BUTTON_HEIGHT, HairEditorUi.inside(mouseX, mouseY, lockX, top, buttonWidth, BUTTON_HEIGHT), locked, true);
		return top + BUTTON_HEIGHT;
	}

	private void renderSegmentList(GuiGraphics graphics, int mouseX, int mouseY, int top, int bottom) {
		HairStrand strand = state.selectedStrand();
		if (strand == null || !strand.isVisible() || bottom - top < ROW_HEIGHT) return;
		int rows = strand.getSegments() + 1;
		int visibleRows = (bottom - top) / ROW_HEIGHT;
		segmentScroll = Math.max(0, Math.min(segmentScroll, Math.max(0, rows - visibleRows)));
		for (int visibleRow = 0; visibleRow < visibleRows; visibleRow++) {
			int row = visibleRow + segmentScroll;
			if (row >= rows) break;
			int rowY = top + visibleRow * ROW_HEIGHT;
			boolean selected = row == 0 ? state.selectedSegment() < 0 : state.selectedSegment() == row - 1;
			boolean hovered = HairEditorUi.inside(mouseX, mouseY, x + INSET, rowY, width - INSET * 2, ROW_HEIGHT);
			if (selected) HairEditorTextures.draw(graphics, HairEditorTextures.BUTTON_ACTIVE, x + INSET, rowY, width - INSET * 2, ROW_HEIGHT);
			else if (hovered) graphics.fill(x + INSET, rowY, x + width - INSET, rowY + ROW_HEIGHT, HairEditorUi.ROW_HOVER);
			Component label = row == 0
					? HairEditorUi.tr("gui.dragonminez.hair_editor.strand", state.selectedFace().key, state.selectedIndex() + 1)
					: HairEditorUi.tr("gui.dragonminez.hair_editor.segment", row);
			int indent = row == 0 ? 8 : 18;
			boolean overridden = row > 0 && strand.getOverride(row - 1) != null && !strand.getOverride(row - 1).isIdentity();
			TextUtil.drawStringWithBorder(graphics, font, label, x + INSET + indent - 4, rowY + 2, HairEditorUi.TEXT);
			if (overridden) graphics.fill(x + width - 10, rowY + 4, x + width - 7, rowY + 7, HairEditorUi.SECTION);
		}
	}

	private void renderCodeButtons(GuiGraphics graphics, int mouseX, int mouseY) {
		int top = codeAreaTop();
		HairEditorUi.divider(graphics, x + INSET, x + width - INSET, top - 3);
		int buttonWidth = (width - INSET * 2 - TAB_GAP) / 2;
		CodeAction[] actions = CodeAction.values();
		for (int i = 0; i < actions.length; i++) {
			int buttonX = x + INSET + (i % 2) * (buttonWidth + TAB_GAP);
			int buttonY = top + (i / 2) * (BUTTON_HEIGHT + TAB_GAP);
			Component label = HairEditorUi.tr("gui.dragonminez.hair_editor.code." + actions[i].name().toLowerCase());
			HairEditorUi.button(graphics, font, label, buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, HairEditorUi.inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, BUTTON_HEIGHT), false, true);
		}
	}

	private int tabHit(double mouseX, double mouseY, int top, int count) {
		int tabWidth = tabWidth(count);
		for (int i = 0; i < count; i++) {
			int tabX = x + INSET + i * (tabWidth + TAB_GAP);
			if (HairEditorUi.inside(mouseX, mouseY, tabX, top, tabWidth, TAB_HEIGHT)) return i;
		}
		return -1;
	}

	private int tabWidth(int count) {
		return (width - INSET * 2 - TAB_GAP * (count - 1)) / count;
	}

	private int gridX(CustomHair.HairFace face) {
		int gridWidth = face.cols * CELL_SIZE + (face.cols - 1) * CELL_GAP;
		return x + (width - gridWidth) / 2;
	}

	private static int gridHeight(CustomHair.HairFace face) {
		return face.rows * CELL_SIZE + (face.rows - 1) * CELL_GAP;
	}

	private int codeAreaTop() {
		return y + height - CODE_AREA_HEIGHT - INSET;
	}
}
