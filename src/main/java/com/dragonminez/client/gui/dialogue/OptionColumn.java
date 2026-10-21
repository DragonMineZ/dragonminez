package com.dragonminez.client.gui.dialogue;

import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.util.ScrollbarState;
import com.dragonminez.client.util.TextUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class OptionColumn {
	static final int WIDTH = 150;
	static final int ROW_H = 20;
	static final int ROW_STEP = 24;

	private static final long ROW_IN_MS = 210L;
	private static final long ROW_STAGGER_MS = 32L;
	private static final long OPENING_DELAY_MS = 110L;
	private static final long ROLL_MS = 170L;
	private static final long FLASH_MS = 750L;
	private static final float ROW_SLIDE = 22.0f;
	private static final float HOVER_SHIFT = 3.0f;
	private static final int DANGER_TEXT = 0xFF7A66;
	private static final int TOOLTIP_TEXT = 0xB8B8C0;

	private final ScrollbarState bar = new ScrollbarState().step(ROW_STEP).minThumb(10)
			.colors(DialogueSkin.SCROLL_TRACK, DialogueSkin.SCROLL_THUMB, DialogueSkin.SCROLL_ACTIVE);
	private final Map<String, RowState> rows = new HashMap<>();
	private final Map<String, CooldownClock> clocks = new HashMap<>();
	private List<DialogueOption> options = List.of();
	private int x;
	private int y;
	private int height;
	private int focused = -1;

	void sync(List<DialogueOption> fresh, long now, boolean opening) {
		Map<String, RowState> kept = new HashMap<>();
		int added = 0;
		for (DialogueOption option : fresh) {
			RowState state = rows.get(option.id);
			if (state == null) {
				state = new RowState();
				state.appearAt = now + (opening ? OPENING_DELAY_MS : 0L) + added++ * ROW_STAGGER_MS;
			}
			kept.put(option.id, state);
		}
		if (added == fresh.size()) bar.reset();
		rows.clear();
		rows.putAll(kept);
		options = fresh;
		if (focused >= options.size()) focused = -1;
	}

	void place(int x, int y, int height) {
		this.x = x;
		this.y = y;
		this.height = height;
	}

	List<DialogueOption> options() {
		return options;
	}

	DialogueOption focusedOption() {
		return focused >= 0 && focused < options.size() ? options.get(focused) : null;
	}

	void clearFocus() {
		focused = -1;
	}

	void moveFocus(int direction, long now) {
		if (options.isEmpty()) return;
		int start = focused < 0 ? (direction > 0 ? -1 : options.size()) : focused;
		for (int step = 1; step <= options.size(); step++) {
			int index = Math.floorMod(start + direction * step, options.size());
			if (options.get(index).available(now)) {
				focused = index;
				bar.ensureVisible(y + index * ROW_STEP, y + index * ROW_STEP + ROW_H);
				return;
			}
		}
	}

	int rowAt(double mouseX, double mouseY) {
		if (!bar.isInView(mouseX, mouseY) || mouseX > x + WIDTH) return -1;
		double contentY = bar.toContent(mouseY) - y;
		if (contentY < 0) return -1;
		int index = (int) (contentY / ROW_STEP);
		if (index >= options.size() || contentY - index * ROW_STEP > ROW_H) return -1;
		return index;
	}

	boolean mouseClicked(double mouseX, double mouseY, int button) {
		return bar.mouseClicked(mouseX, mouseY, button);
	}

	boolean mouseDragged(double mouseX, double mouseY) {
		return bar.mouseDragged(mouseX, mouseY);
	}

	boolean mouseReleased() {
		return bar.mouseReleased();
	}

	boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		return bar.mouseScrolled(mouseX, mouseY, delta);
	}

	void render(GuiGraphics graphics, Font font, double mouseX, double mouseY, float open, float ease, long now,
				String selectedId, boolean blocked, Runnable onReady) {
		int contentHeight = Math.max(0, options.size() * ROW_STEP - (ROW_STEP - ROW_H));
		bar.layout(x, y, WIDTH, height, contentHeight).barAt(x + WIDTH + 3);
		int hoveredIndex = rowAt(mouseX, mouseY);

		bar.beginClip(graphics, x - 2, WIDTH + 8, true);
		for (int i = 0; i < options.size(); i++) {
			DialogueOption option = options.get(i);
			RowState state = rows.get(option.id);
			if (state == null) continue;
			int rowY = y + i * ROW_STEP;
			boolean available = option.available(now) && !blocked;
			boolean timed = option.cooldownTicks != null || option.cooldownEndMs > 0L;
			if (timed && state.seen && available && !state.wasAvailable) {
				state.flashAt = now;
				onReady.run();
			}
			state.wasAvailable = available;
			state.seen = true;
			if (!bar.isVisible(rowY, ROW_H)) continue;

			boolean active = available && (hoveredIndex == i || focused == i);
			state.hover += ((active ? 1.0f : 0.0f) - state.hover) * ease;
			float appear = NpcDialogueScreen.ease((now - state.appearAt) / (float) ROW_IN_MS) * open;
			float rowX = x + (1.0f - appear) * ROW_SLIDE + state.hover * HOVER_SHIFT;
			renderRow(graphics, font, option, state, rowX, rowY, appear, available, option.id.equals(selectedId), now);
		}
		bar.endClip(graphics);
		bar.renderBar(graphics, mouseX, mouseY);
	}

	void renderTooltip(GuiGraphics graphics, Font font, double mouseX, double mouseY, int screenWidth, int screenHeight, long now) {
		int index = rowAt(mouseX, mouseY);
		if (index < 0) return;
		DialogueOption option = options.get(index);
		if (option.available(now) || option.lockLines.isEmpty()) return;
		Component title = option.lockTitle != null ? option.lockTitle : option.label;
		List<Component> description = new ArrayList<>();
		for (Component line : option.lockLines) {
			description.add(line.copy().withStyle(Style.EMPTY.withFont(NpcDialogueScreen.FONT).withColor(TOOLTIP_TEXT)));
		}
		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, 0.0f, 400.0f);
		TextUtil.renderAdvancedTooltip(graphics, font, (int) mouseX, (int) mouseY, screenWidth, screenHeight, title, description, null, DialogueSkin.ACCENT);
		graphics.pose().popPose();
	}

	private void renderRow(GuiGraphics graphics, Font font, DialogueOption option, RowState state, float rowX, int rowY,
						   float appear, boolean available, boolean selected, long now) {
		DialogueSkin.Row base = !available ? DialogueSkin.Row.DISABLED
				: selected ? DialogueSkin.Row.SELECTED
				: option.tone == DialogueOption.Tone.PRIMARY ? DialogueSkin.Row.PRIMARY : DialogueSkin.Row.NORMAL;
		DialogueSkin.row(graphics, base, rowX, rowY, WIDTH, ROW_H, appear);
		if (state.hover > 0.01f) {
			DialogueSkin.Row hover = option.tone == DialogueOption.Tone.DANGER ? DialogueSkin.Row.DANGER : DialogueSkin.Row.HOVER;
			DialogueSkin.row(graphics, hover, rowX, rowY, WIDTH, ROW_H, appear * state.hover);
		}
		if (state.flashAt > 0L && now - state.flashAt < FLASH_MS) {
			float flash = 1.0f - (now - state.flashAt) / (float) FLASH_MS;
			DialogueSkin.row(graphics, DialogueSkin.Row.HOVER, rowX, rowY, WIDTH, ROW_H, appear * flash * flash);
		}

		boolean lit = state.hover > 0.5f;
		int iconColor = !available ? DialogueSkin.MUTED : lit ? DialogueSkin.TEXT : option.iconColor;
		DialogueSkin.icon(graphics, option.icon, rowX + 8, rowY + 5, iconColor, appear);

		long cooldownMs = cooldownMs(option, now);
		String time = cooldownMs <= 0L ? "" : CooldownClock.format(cooldownMs);
		int chipWidth = time.isEmpty() ? 0 : Math.round(HudRender.dmzWidth(time, 1.0f)) + 20;
		int rightReserve = !time.isEmpty() ? chipWidth + 8 : option.submenu ? 18 : 8;
		int labelX = option.icon != DialogueSkin.Icon.NONE ? 22 : 11;
		int labelColor = !available || option.tone == DialogueOption.Tone.DONE && !lit ? DialogueSkin.MUTED
				: option.tone == DialogueOption.Tone.DANGER && !lit ? DANGER_TEXT : DialogueSkin.TEXT;
		HudRender.dmzText(graphics, fit(font, option.label, WIDTH - labelX - rightReserve), rowX + labelX, rowY + 6, 1.0f, 0.0f, labelColor, appear);

		if (!time.isEmpty()) {
			renderChip(graphics, state, time, rowX + WIDTH - chipWidth - 5, rowY + 4, chipWidth, appear, now);
		} else if (option.submenu) {
			int chevron = !available ? DialogueSkin.MUTED : lit ? DialogueSkin.TEXT : DialogueSkin.METAL;
			DialogueSkin.icon(graphics, DialogueSkin.Icon.CHEVRON, rowX + WIDTH - 14, rowY + 5, chevron, appear);
		}
	}

	private void renderChip(GuiGraphics graphics, RowState state, String time, float chipX, float chipY, int width, float alpha, long now) {
		if (!time.equals(state.time)) {
			state.previousTime = state.time;
			state.time = time;
			state.timeChangedAt = now;
		}
		DialogueSkin.chip(graphics, chipX, chipY, width, 12, alpha);
		DialogueSkin.icon(graphics, DialogueSkin.Icon.CLOCK, chipX + 3, chipY + 1, DialogueSkin.ACCENT, alpha);

		float textX = chipX + 15;
		float progress = Mth.clamp((now - state.timeChangedAt) / (float) ROLL_MS, 0.0f, 1.0f);
		if (progress >= 1.0f || state.previousTime.length() != time.length()) {
			HudRender.dmzText(graphics, time, textX, chipY + 2, 1.0f, 0.0f, DialogueSkin.METAL, alpha);
			return;
		}
		float eased = NpcDialogueScreen.ease(progress);
		HudRender.scissor(graphics, chipX + 2, chipY + 1, chipX + width - 2, chipY + 11);
		for (int i = 0; i < time.length(); i++) {
			float charX = textX + HudRender.dmzWidth(time.substring(0, i), 1.0f);
			char current = time.charAt(i);
			char before = state.previousTime.charAt(i);
			if (before == current) {
				HudRender.dmzText(graphics, String.valueOf(current), charX, chipY + 2, 1.0f, 0.0f, DialogueSkin.METAL, alpha);
				continue;
			}
			HudRender.dmzText(graphics, String.valueOf(before), charX, chipY + 2 - eased * 9.0f, 1.0f, 0.0f, DialogueSkin.METAL, alpha * (1.0f - eased));
			HudRender.dmzText(graphics, String.valueOf(current), charX, chipY + 2 + (1.0f - eased) * 9.0f, 1.0f, 0.0f, DialogueSkin.METAL, alpha * eased);
		}
		graphics.disableScissor();
	}

	private long cooldownMs(DialogueOption option, long now) {
		if (option.cooldownEndMs > 0L) return CooldownClock.remainingMs(option.cooldownEndMs, now);
		if (option.cooldownTicks == null) return 0L;
		return clocks.computeIfAbsent(option.id, id -> new CooldownClock()).remainingMs(option.cooldownTicks.getAsInt(), now);
	}

	private static FormattedCharSequence fit(Font font, Component text, int maxWidth) {
		if (font.width(text) <= maxWidth) return text.getVisualOrderText();
		Component ellipsis = Component.literal("...").withStyle(Style.EMPTY.withFont(NpcDialogueScreen.FONT));
		FormattedText cut = font.substrByWidth(text, Math.max(0, maxWidth - font.width(ellipsis)));
		return Language.getInstance().getVisualOrder(FormattedText.composite(cut, ellipsis));
	}

	private static final class RowState {
		long appearAt;
		float hover;
		boolean seen;
		boolean wasAvailable;
		long flashAt;
		String time = "";
		String previousTime = "";
		long timeChangedAt;
	}
}
