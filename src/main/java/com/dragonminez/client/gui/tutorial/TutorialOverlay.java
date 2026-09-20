package com.dragonminez.client.gui.tutorial;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.common.init.MainSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class TutorialOverlay {
	private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menusmall.png");
	private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final Style DMZ_STYLE = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth"));

	private static final float PANEL_SOURCE_WIDTH = 141.0f, PANEL_SOURCE_HEIGHT = 94.0f, PANEL_BORDER = 8.0f;
	private static final float POPUP_WIDTH = 184.0f, POPUP_PADDING = 10.0f, POPUP_GAP = 8.0f, SCREEN_MARGIN = 4.0f;
	private static final float LINE_HEIGHT = 10.0f, TITLE_HEIGHT = 13.0f, BUTTON_HEIGHT = 20.0f, BUTTON_GAP = 4.0f;
	private static final float DIM_ALPHA = 0.74f;
	private static final float INTRO_DELAY = 0.22f;
	private static final float Z_LEVEL = 900.0f;
	private static final int ACCENT = 0x7CFDD6;

	private static final String KEY_NEXT = "gui.dragonminez.tutorial.next";
	private static final String KEY_DONE = "gui.dragonminez.tutorial.done";
	private static final String KEY_SKIP = "gui.dragonminez.tutorial.skip";

	private record ButtonBox(float x, float y, float width, float height, TutorialButton button, int kind) {
		boolean contains(double px, double py) {
			return px >= x && px < x + width && py >= y && py < y + height;
		}
	}

	private static final int KIND_CUSTOM = 0, KIND_NEXT = 1, KIND_SKIP = 2;

	private static final List<float[]> animatedRects = new ArrayList<>();
	private static final List<ButtonBox> buttonBoxes = new ArrayList<>();
	private static List<TutorialRect> targetRects = List.of();
	private static long lastFrameNanos;
	private static float elapsed;
	private static float dim;
	private static float popupAlpha;
	private static float popupX, popupY;
	private static boolean popupPlaced;

	private static String cachedText;
	private static float cachedTextWidth;
	private static List<FormattedCharSequence> cachedLines = List.of();

	private TutorialOverlay() {}

	static void reset() {
		animatedRects.clear();
		buttonBoxes.clear();
		targetRects = List.of();
		lastFrameNanos = 0L;
		elapsed = 0.0f;
		dim = 0.0f;
		popupAlpha = 0.0f;
		popupPlaced = false;
		cachedText = null;
	}

	static void onStepChanged() {
		popupAlpha = 0.0f;
		buttonBoxes.clear();
	}

	static void playClick() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MainSounds.PIP_MENU.get(), 1.0F));
	}

	static boolean insideHighlight(double mouseX, double mouseY) {
		for (TutorialRect rect : targetRects) if (rect.contains(mouseX, mouseY)) return true;
		return false;
	}

	static boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0 || popupAlpha < 0.5f) return false;
		for (ButtonBox box : new ArrayList<>(buttonBoxes)) {
			if (!box.contains(mouseX, mouseY)) continue;
			playClick();
			switch (box.kind()) {
				case KIND_NEXT -> TutorialManager.advance();
				case KIND_SKIP -> TutorialManager.finish();
				default -> TutorialManager.press(box.button());
			}
			return true;
		}
		return false;
	}

	static void render(GuiGraphics graphics, TutorialStep step, float scale, int width, int height, double mouseX, double mouseY) {
		long now = System.nanoTime();
		float dt = lastFrameNanos == 0L ? 0.0f : Math.min(0.1f, (now - lastFrameNanos) / 1_000_000_000.0f);
		lastFrameNanos = now;
		elapsed += dt;

		List<TutorialRect> resolved = step.resolveHighlights();
		List<TutorialRect> targets = new ArrayList<>(resolved.size());
		for (TutorialRect rect : resolved) targets.add(clip(rect.inflate(step.padding()), width, height));
		targetRects = targets;

		float intro = Mth.clamp((elapsed - INTRO_DELAY) / 0.2f, 0.0f, 1.0f);
		dim += (intro - dim) * ease(dt, 0.12f);
		if (elapsed >= INTRO_DELAY) popupAlpha += (1.0f - popupAlpha) * ease(dt, 0.09f);
		animateRects(targets, dt, width, height);

		graphics.flush();
		RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, 0.0f, Z_LEVEL);
		graphics.pose().scale(scale, scale, 1.0f);

		renderDim(graphics, width, height);
		renderBorders(graphics);
		renderPopup(graphics, step, targets, width, height, dt, mouseX, mouseY);

		graphics.flush();
		graphics.pose().popPose();
	}

	private static float ease(float dt, float tau) {
		return dt <= 0.0f ? 0.0f : 1.0f - (float) Math.exp(-dt / tau);
	}

	private static TutorialRect clip(TutorialRect rect, int width, int height) {
		float left = Mth.clamp(rect.x(), 0.0f, width), top = Mth.clamp(rect.y(), 0.0f, height);
		float right = Mth.clamp(rect.right(), 0.0f, width), bottom = Mth.clamp(rect.bottom(), 0.0f, height);
		return TutorialRect.corners(left, top, right, bottom);
	}

	private static void animateRects(List<TutorialRect> targets, float dt, int width, int height) {
		float amount = ease(dt, 0.085f);
		while (animatedRects.size() < targets.size()) {
			TutorialRect target = targets.get(animatedRects.size());
			if (animatedRects.isEmpty()) {
				animatedRects.add(new float[]{target.x() - 14.0f, target.y() - 14.0f, target.width() + 28.0f, target.height() + 28.0f});
			} else {
				float[] origin = animatedRects.get(animatedRects.size() - 1);
				animatedRects.add(new float[]{origin[0] + origin[2] / 2.0f, origin[1] + origin[3] / 2.0f, 0.0f, 0.0f});
			}
		}

		for (int i = animatedRects.size() - 1; i >= 0; i--) {
			float[] rect = animatedRects.get(i);
			float tx, ty, tw, th;
			if (i < targets.size()) {
				TutorialRect target = targets.get(i);
				tx = target.x(); ty = target.y(); tw = target.width(); th = target.height();
			} else {
				tx = rect[0] + rect[2] / 2.0f; ty = rect[1] + rect[3] / 2.0f; tw = 0.0f; th = 0.0f;
			}
			rect[0] += (tx - rect[0]) * amount;
			rect[1] += (ty - rect[1]) * amount;
			rect[2] += (tw - rect[2]) * amount;
			rect[3] += (th - rect[3]) * amount;
			if (i >= targets.size() && rect[2] < 0.75f && rect[3] < 0.75f) animatedRects.remove(i);
		}
	}

	private static void renderDim(GuiGraphics graphics, int width, int height) {
		int alpha = Math.round(DIM_ALPHA * dim * 255.0f);
		if (alpha <= 0) return;

		float[] xs = new float[animatedRects.size() * 2 + 2];
		float[] ys = new float[animatedRects.size() * 2 + 2];
		int count = 0;
		xs[count] = 0.0f; ys[count++] = 0.0f;
		xs[count] = width; ys[count++] = height;
		for (float[] rect : animatedRects) {
			if (rect[2] < 0.5f || rect[3] < 0.5f) continue;
			xs[count] = Mth.clamp(rect[0], 0.0f, width); ys[count++] = Mth.clamp(rect[1], 0.0f, height);
			xs[count] = Mth.clamp(rect[0] + rect[2], 0.0f, width); ys[count++] = Mth.clamp(rect[1] + rect[3], 0.0f, height);
		}
		xs = Arrays.copyOf(xs, count);
		ys = Arrays.copyOf(ys, count);
		Arrays.sort(xs);
		Arrays.sort(ys);

		Quads quads = Quads.begin(graphics);
		for (int row = 0; row + 1 < ys.length; row++) {
			float top = ys[row], bottom = ys[row + 1];
			if (bottom - top <= 0.0001f) continue;
			float runStart = Float.NaN;
			for (int column = 0; column + 1 < xs.length; column++) {
				float left = xs[column], right = xs[column + 1];
				if (right - left <= 0.0001f) continue;
				boolean lit = isLit((left + right) / 2.0f, (top + bottom) / 2.0f);
				if (!lit && Float.isNaN(runStart)) runStart = left;
				if (lit && !Float.isNaN(runStart)) {
					quads.rect(runStart, top, left, bottom, 0x000000, alpha);
					runStart = Float.NaN;
				}
			}
			if (!Float.isNaN(runStart)) quads.rect(runStart, top, width, bottom, 0x000000, alpha);
		}
		quads.end();
	}

	private static boolean isLit(float x, float y) {
		for (float[] rect : animatedRects) {
			if (rect[2] < 0.5f || rect[3] < 0.5f) continue;
			if (x >= rect[0] && x < rect[0] + rect[2] && y >= rect[1] && y < rect[1] + rect[3]) return true;
		}
		return false;
	}

	private static void renderBorders(GuiGraphics graphics) {
		if (dim <= 0.01f || animatedRects.isEmpty()) return;
		float pulse = 0.72f + 0.28f * Mth.sin(elapsed * 3.6f);
		Quads quads = Quads.begin(graphics);
		for (float[] rect : animatedRects) {
			if (rect[2] < 2.0f || rect[3] < 2.0f) continue;
			frame(quads, rect[0] - 3.0f, rect[1] - 3.0f, rect[2] + 6.0f, rect[3] + 6.0f, 1.0f, ACCENT, Math.round(40 * dim * pulse));
			frame(quads, rect[0] - 2.0f, rect[1] - 2.0f, rect[2] + 4.0f, rect[3] + 4.0f, 1.0f, ACCENT, Math.round(95 * dim * pulse));
			frame(quads, rect[0] - 1.0f, rect[1] - 1.0f, rect[2] + 2.0f, rect[3] + 2.0f, 1.0f, ACCENT, Math.round(255 * dim * pulse));
		}
		quads.end();
	}

	private static void frame(Quads quads, float x, float y, float width, float height, float thickness, int rgb, int alpha) {
		if (alpha <= 0) return;
		quads.rect(x, y, x + width, y + thickness, rgb, alpha);
		quads.rect(x, y + height - thickness, x + width, y + height, rgb, alpha);
		quads.rect(x, y + thickness, x + thickness, y + height - thickness, rgb, alpha);
		quads.rect(x + width - thickness, y + thickness, x + width, y + height - thickness, rgb, alpha);
	}

	private static void renderPopup(GuiGraphics graphics, TutorialStep step, List<TutorialRect> targets, int width, int height, float dt, double mouseX, double mouseY) {
		Font font = Minecraft.getInstance().font;
		float popupWidth = Math.min(step.width() > 0.0f ? step.width() : POPUP_WIDTH, width - SCREEN_MARGIN * 2.0f);
		float textWidth = popupWidth - POPUP_PADDING * 2.0f;

		List<FormattedCharSequence> lines = lines(font, step, textWidth);
		boolean hasTitle = step.titleKey() != null;
		float popupHeight = POPUP_PADDING + (hasTitle ? TITLE_HEIGHT : 0.0f) + lines.size() * LINE_HEIGHT + 6.0f + BUTTON_HEIGHT + POPUP_PADDING - 2.0f;

		float[] place = place(step.centered() ? null : TutorialRect.union(targets), popupWidth, popupHeight, width, height);
		if (!popupPlaced) {
			popupX = place[0];
			popupY = place[1];
			popupPlaced = true;
		} else {
			float amount = ease(dt, 0.10f);
			popupX += (place[0] - popupX) * amount;
			popupY += (place[1] - popupY) * amount;
		}

		float alpha = Mth.clamp(popupAlpha, 0.0f, 1.0f);
		if (alpha <= 0.02f) {
			buttonBoxes.clear();
			return;
		}

		float x = popupX;
		float y = popupY + (1.0f - alpha) * 6.0f;

		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
		nineSlice(graphics, x, y, popupWidth, popupHeight);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		int textAlpha = Math.max(4, Math.round(alpha * 255.0f));
		float cursorY = y + POPUP_PADDING;

		if (hasTitle) {
			Component title = Component.translatable(step.titleKey()).withStyle(DMZ_STYLE).withStyle(ChatFormatting.BOLD);
			text(graphics, font, title.getVisualOrderText(), x + POPUP_PADDING, cursorY, 0xFFD700, textAlpha);
			cursorY += TITLE_HEIGHT;
		}

		String counter = TutorialManager.position() + "/" + TutorialManager.total();
		Component counterComponent = Component.literal(counter).withStyle(DMZ_STYLE);
		text(graphics, font, counterComponent.getVisualOrderText(), x + popupWidth - POPUP_PADDING - font.width(counterComponent), y + POPUP_PADDING, 0x9FB8AE, textAlpha);

		for (FormattedCharSequence line : lines) {
			text(graphics, font, line, x + POPUP_PADDING, cursorY, 0xFFFFFF, textAlpha);
			cursorY += LINE_HEIGHT;
		}

		layoutButtons(step, x, y, popupWidth, popupHeight);
		for (ButtonBox box : buttonBoxes) renderButton(graphics, font, box, step, alpha, textAlpha, mouseX, mouseY);
	}

	private static List<FormattedCharSequence> lines(Font font, TutorialStep step, float textWidth) {
		Component body = Component.translatable(step.textKey(), step.args()).withStyle(DMZ_STYLE);
		String resolved = body.getString();
		if (!resolved.equals(cachedText) || cachedTextWidth != textWidth) {
			cachedText = resolved;
			cachedTextWidth = textWidth;
			cachedLines = font.split(body, Math.max(40, (int) textWidth));
		}
		return cachedLines;
	}

	private static float[] place(TutorialRect target, float popupWidth, float popupHeight, int width, int height) {
		float maxX = Math.max(SCREEN_MARGIN, width - SCREEN_MARGIN - popupWidth);
		float maxY = Math.max(SCREEN_MARGIN, height - SCREEN_MARGIN - popupHeight);
		if (target == null) return new float[]{(width - popupWidth) / 2.0f, (height - popupHeight) / 2.0f};

		float centeredX = Mth.clamp(target.centerX() - popupWidth / 2.0f, SCREEN_MARGIN, maxX);
		float centeredY = Mth.clamp(target.centerY() - popupHeight / 2.0f, SCREEN_MARGIN, maxY);

		float[][] candidates = {
				{target.right() + POPUP_GAP, centeredY, width - target.right()},
				{target.x() - POPUP_GAP - popupWidth, centeredY, target.x()},
				{centeredX, target.bottom() + POPUP_GAP, height - target.bottom()},
				{centeredX, target.y() - POPUP_GAP - popupHeight, target.y()}
		};

		for (float[] candidate : candidates) {
			if (candidate[0] >= SCREEN_MARGIN && candidate[0] <= maxX && candidate[1] >= SCREEN_MARGIN && candidate[1] <= maxY) {
				return new float[]{candidate[0], candidate[1]};
			}
		}

		float[] best = candidates[0];
		for (float[] candidate : candidates) if (candidate[2] > best[2]) best = candidate;
		return new float[]{Mth.clamp(best[0], SCREEN_MARGIN, maxX), Mth.clamp(best[1], SCREEN_MARGIN, maxY)};
	}

	private static void layoutButtons(TutorialStep step, float x, float y, float popupWidth, float popupHeight) {
		buttonBoxes.clear();
		float rowY = y + popupHeight - POPUP_PADDING - BUTTON_HEIGHT + 2.0f;
		float cursor = x + popupWidth - POPUP_PADDING;

		boolean hasFlowButton = false;
		for (TutorialButton button : step.buttons()) if (button.behavior() != TutorialButton.Behavior.STAY) hasFlowButton = true;

		if (!hasFlowButton) {
			cursor -= 74.0f;
			buttonBoxes.add(new ButtonBox(cursor, rowY, 74.0f, BUTTON_HEIGHT, null, KIND_NEXT));
			cursor -= BUTTON_GAP;
		}

		for (int i = step.buttons().size() - 1; i >= 0; i--) {
			TutorialButton button = step.buttons().get(i);
			cursor -= button.width();
			float height = button.isIcon() ? 11.0f : BUTTON_HEIGHT;
			buttonBoxes.add(new ButtonBox(cursor, rowY + (BUTTON_HEIGHT - height) / 2.0f, button.width(), height, button, KIND_CUSTOM));
			cursor -= BUTTON_GAP;
		}

		float used = x + popupWidth - POPUP_PADDING - cursor;
		if (step.skippable() && !hasFlowButton && popupWidth - POPUP_PADDING * 2.0f - used >= 74.0f) {
			buttonBoxes.add(new ButtonBox(x + POPUP_PADDING, rowY, 74.0f, BUTTON_HEIGHT, null, KIND_SKIP));
		}
	}

	private static void renderButton(GuiGraphics graphics, Font font, ButtonBox box, TutorialStep step, float alpha, int textAlpha, double mouseX, double mouseY) {
		boolean hovered = box.contains(mouseX, mouseY);
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

		if (box.button() != null && box.button().isIcon()) {
			TutorialButton.Icon icon = box.button().icon();
			HudRender.blit(graphics, BUTTONS, box.x() + (box.width() - icon.width) / 2.0f, box.y() + (box.height() - icon.height) / 2.0f,
					icon.u, icon.v + (hovered ? icon.height : 0), icon.width, icon.height, 256, 256);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			return;
		}

		HudRender.blit(graphics, BUTTONS, box.x(), box.y(), 0, hovered ? 48 : 28, 74, 20, 256, 256);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		String key = switch (box.kind()) {
			case KIND_NEXT -> step.nextKey() != null ? step.nextKey() : TutorialManager.isLastStep() ? KEY_DONE : KEY_NEXT;
			case KIND_SKIP -> KEY_SKIP;
			default -> box.button().labelKey();
		};
		Component label = Component.translatable(key).withStyle(DMZ_STYLE);
		int color = (textAlpha << 24) | (hovered ? 0x7CFDD6 : 0xFFFFFF);
		graphics.pose().pushPose();
		graphics.pose().translate(box.x() + box.width() / 2.0f - font.width(label) / 2.0f, box.y() + (box.height() - 8.0f) / 2.0f, 0.0f);
		graphics.drawString(font, label, 0, 0, color);
		graphics.pose().popPose();
	}

	private static void text(GuiGraphics graphics, Font font, FormattedCharSequence sequence, float x, float y, int rgb, int alpha) {
		int border = alpha << 24;
		FormattedCharSequence outline = sink -> sequence.accept((position, style, codePoint) ->
				sink.accept(position, style.withColor(net.minecraft.network.chat.TextColor.fromRgb(0x000000)), codePoint));
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0.0f);
		graphics.drawString(font, outline, 1, 0, border, false);
		graphics.drawString(font, outline, -1, 0, border, false);
		graphics.drawString(font, outline, 0, 1, border, false);
		graphics.drawString(font, outline, 0, -1, border, false);
		graphics.pose().translate(0.0f, 0.0f, 0.1f);
		graphics.drawString(font, sequence, 0, 0, (alpha << 24) | (rgb & 0xFFFFFF), false);
		graphics.pose().popPose();
	}

	private static void nineSlice(GuiGraphics graphics, float x, float y, float width, float height) {
		HudRender.nineSlice(graphics, PANEL, x, y, width, height, 0.0f, 0.0f, PANEL_SOURCE_WIDTH, PANEL_SOURCE_HEIGHT, PANEL_BORDER, 256, 256);
	}

	private static final class Quads {
		private final BufferBuilder builder;
		private final Matrix4f matrix;
		private boolean any;

		private Quads(BufferBuilder builder, Matrix4f matrix) {
			this.builder = builder;
			this.matrix = matrix;
		}

		static Quads begin(GuiGraphics graphics) {
			graphics.flush();
			BufferBuilder builder = Tesselator.getInstance().getBuilder();
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			return new Quads(builder, graphics.pose().last().pose());
		}

		void rect(float left, float top, float right, float bottom, int rgb, int alpha) {
			if (right <= left || bottom <= top || alpha <= 0) return;
			int a = Math.min(255, alpha), r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
			builder.vertex(matrix, left, top, 0.0f).color(r, g, b, a).endVertex();
			builder.vertex(matrix, left, bottom, 0.0f).color(r, g, b, a).endVertex();
			builder.vertex(matrix, right, bottom, 0.0f).color(r, g, b, a).endVertex();
			builder.vertex(matrix, right, top, 0.0f).color(r, g, b, a).endVertex();
			any = true;
		}

		void end() {
			if (!any) {
				builder.end().release();
				return;
			}
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			BufferUploader.drawWithShader(builder.end());
		}
	}
}
