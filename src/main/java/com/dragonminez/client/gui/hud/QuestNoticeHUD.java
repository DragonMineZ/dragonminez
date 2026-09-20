package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.client.gui.quest.StoryToast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public class QuestNoticeHUD {
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	private static final int WIDTH = 220;
	private static final int HEIGHT = 52;
	private static final float ROW_PITCH = HEIGHT + 4.0f;
	private static final float SLIDE_DISTANCE = 60.0f;
	private static final int MAX_VISIBLE = 3;
	private static final int MAX_QUEUED = 12;
	private static final long DURATION_MS = 5000L;
	private static final float HIDDEN_PREVIEW_ALPHA = 0.35f;

	private static final List<Notice> ACTIVE = new ArrayList<>();
	private static final Deque<Notice> QUEUE = new ArrayDeque<>();
	private static final List<Notice> PREVIEW = List.of(
			new Notice(Component.translatable("toast.dragonminez.story.quest_complete.title"),
					Component.translatable("gui.dragonminez.hud_editor.sample.quest_title"), StoryToast.Tone.SUCCESS),
			new Notice(Component.translatable("toast.dragonminez.story.objective_complete.title"),
					Component.translatable("gui.dragonminez.hud_editor.sample.quest_objective"), StoryToast.Tone.PROGRESS));

	public static final IGuiOverlay HUD_QUEST_NOTICE = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void push(Component title, Component description, StoryToast.Tone tone) {
		Minecraft.getInstance().execute(() -> {
			if (QUEUE.size() >= MAX_QUEUED) QUEUE.pollFirst();
			QUEUE.addLast(new Notice(title, description, tone));
		});
	}

	public static void clear() {
		ACTIVE.clear();
		QUEUE.clear();
	}

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;

		boolean preview = HudLayout.isPreview();
		HudLayout.Box box = HudLayout.resolve(HudElement.QUEST_NOTICE, width, height);
		if (!box.visible() && !preview) {
			clear();
			return;
		}

		boolean upward = HudLayout.anchorY(HudElement.QUEST_NOTICE) >= 1.0f;
		float direction = box.centerX() >= width / 2.0f ? 1.0f : -1.0f;

		if (preview) {
			float alpha = box.visible() ? 1.0f : HIDDEN_PREVIEW_ALPHA;
			for (int i = 0; i < PREVIEW.size(); i++) draw(guiGraphics, mc.font, box, PREVIEW.get(i), i, upward, 0.0f, alpha, 0.35f);
			return;
		}

		long now = System.currentTimeMillis();
		while (ACTIVE.size() < MAX_VISIBLE && !QUEUE.isEmpty()) {
			Notice notice = QUEUE.pollFirst();
			notice.startMs = now;
			notice.appear.snap(0.0f);
			notice.slot.snap(ACTIVE.size());
			ACTIVE.add(notice);
		}

		int index = 0;
		for (Iterator<Notice> it = ACTIVE.iterator(); it.hasNext(); ) {
			Notice notice = it.next();
			long elapsed = now - notice.startMs;
			float appear = notice.appear.update(elapsed < DURATION_MS ? 1.0f : 0.0f);
			if (elapsed >= DURATION_MS && appear <= 0.02f) {
				it.remove();
				continue;
			}
			float slot = notice.slot.update(index++);
			float progress = 1.0f - Mth.clamp(elapsed / (float) DURATION_MS, 0.0f, 1.0f);
			draw(guiGraphics, mc.font, box, notice, slot, upward, (1.0f - appear) * SLIDE_DISTANCE * direction, appear, progress);
		}
	}

	private static void draw(GuiGraphics guiGraphics, Font font, HudLayout.Box box, Notice notice, float slot, boolean upward, float slide, float alpha, float progress) {
		int backgroundColor = switch (notice.tone) {
			case INFO -> 0xCC10212B;
			case PROGRESS -> 0xCC1D2338;
			case FAILURE -> 0xCC321717;
			case SUCCESS -> 0xCC173122;
		};
		int borderColor = switch (notice.tone) {
			case INFO -> 0xFF58B3FF;
			case PROGRESS -> 0xFF7E8BFF;
			case FAILURE -> 0xFFFF6D6D;
			case SUCCESS -> 0xFF4CDB8D;
		};

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(box.x(), box.y(), 0.0f);
		guiGraphics.pose().scale(box.scale(), box.scale(), 1.0f);
		guiGraphics.pose().translate(slide, (upward ? -slot : slot) * ROW_PITCH, 0.0f);

		guiGraphics.fill(0, 0, WIDTH, HEIGHT, fade(backgroundColor, alpha));
		guiGraphics.fill(0, 0, WIDTH, 2, fade(borderColor, alpha));
		guiGraphics.fill(0, HEIGHT - 1, WIDTH, HEIGHT, fade(0x66000000, alpha));
		guiGraphics.fill(4, HEIGHT - 4, 4 + Math.round(progress * (WIDTH - 8)), HEIGHT - 2, fade(borderColor, alpha));

		int textWidth = WIDTH - 16;
		List<FormattedCharSequence> titleLines = font.split(styled(notice.title), textWidth);
		List<FormattedCharSequence> descriptionLines = font.split(styled(notice.description), textWidth);
		int textY = 6;
		int titleHeight = drawLines(guiGraphics, font, titleLines, textY, fade(0xFFFFFFFF, alpha), 2);
		textY += titleHeight;
		if (titleHeight > 0 && !descriptionLines.isEmpty()) textY += 2;
		drawLines(guiGraphics, font, descriptionLines, textY, fade(0xFFD8E6FF, alpha), 3);
		guiGraphics.pose().popPose();
	}

	private static int drawLines(GuiGraphics guiGraphics, Font font, List<FormattedCharSequence> lines, int startY, int color, int maxLines) {
		int count = Math.min(maxLines, lines.size());
		for (int i = 0; i < count; i++) guiGraphics.drawString(font, lines.get(i), 8, startY + i * font.lineHeight, color, false);
		return count * font.lineHeight;
	}

	private static Component styled(Component component) {
		return component.copy().withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	private static int fade(int argb, float alpha) {
		int channel = Math.round(((argb >>> 24) & 0xFF) * Mth.clamp(alpha, 0.0f, 1.0f));
		return (Math.max(4, channel) << 24) | (argb & 0xFFFFFF);
	}

	private static final class Notice {
		private final Component title;
		private final Component description;
		private final StoryToast.Tone tone;
		private final HudSmoother appear = new HudSmoother(0.12f, 0.01f);
		private final HudSmoother slot = new HudSmoother(0.12f, 0.01f);
		private long startMs;

		private Notice(Component title, Component description, StoryToast.Tone tone) {
			this.title = title;
			this.description = description;
			this.tone = tone;
		}
	}
}
