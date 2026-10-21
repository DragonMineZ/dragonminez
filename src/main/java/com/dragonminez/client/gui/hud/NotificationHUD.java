package com.dragonminez.client.gui.hud;

import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class NotificationHUD {
	public static final float WIDTH = 200.0f;
	public static final float HEIGHT = 56.0f;

	public static final int PRIORITY_LOW = 100;
	public static final int PRIORITY_NORMAL = 300;
	public static final int PRIORITY_HIGH = 600;
	public static final int PRIORITY_TOURNAMENT = 900;
	public static final int PRIORITY_TUTORIAL = 950;
	public static final int PRIORITY_TRACKED_QUEST = 1000;

	public static final long DURATION_MS = 5000L;
	private static final int MAX_TRANSIENT = 5;
	private static final float PADDING = 6.0f;
	private static final float GAP = 4.0f;
	private static final float LINE_HEIGHT = 10.0f;
	private static final float EXTRA_SCALE = 0.75f;
	private static final float HIDDEN_PREVIEW_ALPHA = 0.35f;
	private static final int TITLE_COLOR = 0xFFE066;
	private static final int SUBTITLE_COLOR = 0x9FD8FF;
	private static final int DESCRIPTION_COLOR = 0xFFFFFF;
	private static final int EXTRA_COLOR = 0xA0A6B0;

	public record Content(Component title, Component subtitle, Component description, Component extra) {
		public static Content of(Component title, Component subtitle, Component description, Component extra) {
			return new Content(title, subtitle, description, extra);
		}
	}

	private static final Map<String, Entry> PINNED = new LinkedHashMap<>();
	private static final List<Entry> TRANSIENT = new ArrayList<>();
	private static final List<Entry> PREVIEW = List.of(
			new Entry(null, () -> Content.of(
					Component.translatable("gui.dragonminez.hud_editor.sample.quest_title"),
					Component.translatable("gui.dragonminez.story.hud.tracked"),
					Component.translatable("gui.dragonminez.hud_editor.sample.quest_objective"),
					Component.translatable("gui.dragonminez.story.hud.time_left", "1:35")), PRIORITY_TRACKED_QUEST),
			new Entry(null, () -> Content.of(
					Component.translatable("toast.dragonminez.story.objective_complete.title"),
					Component.translatable("gui.dragonminez.hud_editor.sample.quest_title"),
					Component.translatable("gui.dragonminez.hud_editor.sample.quest_objective"),
					null), PRIORITY_NORMAL));
	private static long sequence;

	public static final IGuiOverlay HUD_NOTIFICATIONS = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	private NotificationHUD() {
	}

	public static void push(Component title, Component subtitle, Component description, Component extra, int priority) {
		push(Content.of(title, subtitle, description, extra), priority);
	}

	public static void push(Content content, int priority) {
		if (content == null) return;
		Minecraft.getInstance().execute(() -> {
			Entry entry = new Entry(null, () -> content, priority);
			entry.startMs = System.currentTimeMillis();
			TRANSIENT.add(entry);
			int live = 0;
			for (int i = TRANSIENT.size() - 1; i >= 0; i--) {
				Entry other = TRANSIENT.get(i);
				if (other.removing) continue;
				if (++live > MAX_TRANSIENT) other.startMs = 0L;
			}
		});
	}

	public static void pin(String key, Supplier<Content> content, int priority) {
		Minecraft.getInstance().execute(() -> {
			Entry existing = PINNED.get(key);
			if (existing != null) {
				existing.content = content;
				existing.priority = priority;
				return;
			}
			PINNED.put(key, new Entry(key, content, priority));
		});
	}

	public static void unpin(String key) {
		Minecraft.getInstance().execute(() -> PINNED.remove(key));
	}

	public static void clear() {
		TRANSIENT.clear();
		for (Entry entry : PINNED.values()) entry.reset();
	}

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;

		boolean preview = HudLayout.isPreview();
		HudLayout.Box box = HudLayout.resolve(HudElement.NOTIFICATIONS, width, height);
		if (!box.visible() && !preview) {
			clear();
			return;
		}

		boolean upward = HudLayout.anchorY(HudElement.NOTIFICATIONS) >= 1.0f;
		boolean fromRight = box.centerX() >= width / 2.0f;
		float edgeDistance = fromRight
				? (width - box.x()) / box.scale() + 8.0f
				: box.right() / box.scale() + 8.0f;
		float direction = fromRight ? 1.0f : -1.0f;
		Font font = mc.font;

		if (preview) {
			float alpha = box.visible() ? 1.0f : HIDDEN_PREVIEW_ALPHA;
			float cursor = 0.0f;
			for (Entry entry : PREVIEW) {
				Layout layout = measure(font, entry.content.get());
				float y = upward ? -(cursor + layout.height) : cursor;
				draw(guiGraphics, font, box, layout, 0.0f, y, alpha);
				cursor += layout.height + GAP;
			}
			return;
		}

		long now = System.currentTimeMillis();
		List<Entry> ordered = new ArrayList<>();
		for (Entry entry : PINNED.values()) {
			Content content = entry.content.get();
			entry.present = content != null;
			entry.lastContent = content;
			if (entry.present || entry.appear.value() > 0.02f) ordered.add(entry);
		}
		for (Iterator<Entry> it = TRANSIENT.iterator(); it.hasNext(); ) {
			Entry entry = it.next();
			if (!entry.removing && now - entry.startMs >= DURATION_MS) entry.removing = true;
			if (entry.removing && entry.appear.value() <= 0.02f && entry.shown) {
				it.remove();
				continue;
			}
			entry.present = !entry.removing;
			entry.lastContent = entry.content.get();
			ordered.add(entry);
		}
		ordered.sort(Comparator.<Entry>comparingInt(entry -> -entry.priority).thenComparingLong(entry -> -entry.seq));

		float cursor = 0.0f;
		for (Entry entry : ordered) {
			Content content = entry.lastContent;
			if (content == null) continue;
			Layout layout = measure(font, content);
			float targetY = upward ? -(cursor + layout.height) : cursor;
			if (!entry.shown) {
				entry.y.snap(targetY);
				entry.appear.snap(0.0f);
				entry.shown = true;
			}
			float y = entry.y.update(targetY);
			float appear = entry.appear.update(entry.present ? 1.0f : 0.0f);
			if (appear <= 0.005f && !entry.present) continue;
			float slide = (1.0f - ease(appear)) * edgeDistance * direction;
			draw(guiGraphics, font, box, layout, slide, y, appear);
			cursor += (layout.height + GAP) * Mth.clamp(appear * 1.5f, 0.0f, 1.0f);
		}
	}

	private static float ease(float t) {
		t = Mth.clamp(t, 0.0f, 1.0f);
		return 1.0f - (1.0f - t) * (1.0f - t);
	}

	private static Layout measure(Font font, Content content) {
		int textWidth = (int) (WIDTH - PADDING * 2.0f);
		Layout layout = new Layout();
		if (content.title() != null) layout.title = font.split(HudRender.dmz(content.title()), textWidth);
		if (content.subtitle() != null) layout.subtitle = font.split(HudRender.dmz(content.subtitle()), textWidth);
		if (content.description() != null) layout.description = font.split(HudRender.dmz(content.description()), textWidth);
		if (content.extra() != null) layout.extra = HudRender.dmz(content.extra());
		float height = PADDING;
		height += layout.title.size() * LINE_HEIGHT;
		height += layout.subtitle.size() * LINE_HEIGHT;
		if (!layout.description.isEmpty()) height += (layout.title.isEmpty() && layout.subtitle.isEmpty() ? 0.0f : 1.0f) + layout.description.size() * LINE_HEIGHT;
		if (layout.extra != null) height += LINE_HEIGHT * EXTRA_SCALE + 2.0f;
		layout.height = height + PADDING - 2.0f;
		return layout;
	}

	private static void draw(GuiGraphics guiGraphics, Font font, HudLayout.Box box, Layout layout, float slide, float y, float alpha) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(box.x(), box.y(), 0.0f);
		guiGraphics.pose().scale(box.scale(), box.scale(), 1.0f);
		guiGraphics.pose().translate(slide, y, 0.0f);

		HudRender.panel(guiGraphics, 0.0f, 0.0f, WIDTH, layout.height, alpha);

		float textY = PADDING;
		for (FormattedCharSequence line : layout.title) {
			HudRender.dmzText(guiGraphics, line, PADDING, textY, 1.0f, 0.0f, TITLE_COLOR, alpha);
			textY += LINE_HEIGHT;
		}
		for (FormattedCharSequence line : layout.subtitle) {
			HudRender.dmzText(guiGraphics, line, PADDING, textY, 1.0f, 0.0f, SUBTITLE_COLOR, alpha);
			textY += LINE_HEIGHT;
		}
		if (!layout.description.isEmpty() && (!layout.title.isEmpty() || !layout.subtitle.isEmpty())) textY += 1.0f;
		for (FormattedCharSequence line : layout.description) {
			HudRender.dmzText(guiGraphics, line, PADDING, textY, 1.0f, 0.0f, DESCRIPTION_COLOR, alpha);
			textY += LINE_HEIGHT;
		}
		if (layout.extra != null) {
			HudRender.dmzText(guiGraphics, layout.extra, WIDTH - PADDING, textY + 2.0f, EXTRA_SCALE, 1.0f, EXTRA_COLOR, alpha);
		}
		guiGraphics.pose().popPose();
	}

	private static final class Layout {
		private List<FormattedCharSequence> title = List.of();
		private List<FormattedCharSequence> subtitle = List.of();
		private List<FormattedCharSequence> description = List.of();
		private Component extra;
		private float height;
	}

	private static final class Entry {
		private final String key;
		private Supplier<Content> content;
		private Content lastContent;
		private int priority;
		private final long seq = sequence++;
		private final HudSmoother appear = new HudSmoother(0.16f, 0.005f);
		private final HudSmoother y = new HudSmoother(0.14f, 0.01f);
		private long startMs;
		private boolean removing;
		private boolean present;
		private boolean shown;

		private Entry(String key, Supplier<Content> content, int priority) {
			this.key = key;
			this.content = content;
			this.priority = priority;
		}

		private void reset() {
			appear.snap(0.0f);
			shown = false;
		}
	}
}
