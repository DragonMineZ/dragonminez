package com.dragonminez.client.util;

import com.dragonminez.client.gui.hud.HudRender;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class ScrollbarState {
    public static final int DEFAULT_BAR_WIDTH = 3;
    public static final int BAR_GAP = 3;
    public static final int DEFAULT_TRACK_COLOR = 0xFF333333;
    public static final int DEFAULT_THUMB_COLOR = 0xFFAAAAAA;
    public static final int DEFAULT_ACTIVE_COLOR = 0xFFFFFFFF;
    public static final float DEFAULT_MIN_THUMB = 14.0f;
    public static final float DEFAULT_STEP = 20.0f;

    private static final float SMOOTH_TAU = 0.08f;
    private static final long ACTIVE_WINDOW_NANOS = 250_000_000L;
    private static final int GRAB_SLOP = 4;

    private boolean horizontal;
    private boolean smooth = true;
    private boolean drawTrack = true;
    private float step = DEFAULT_STEP;
    private float minThumb = DEFAULT_MIN_THUMB;
    private int barWidth = DEFAULT_BAR_WIDTH;
    private int trackColor = DEFAULT_TRACK_COLOR;
    private int thumbColor = DEFAULT_THUMB_COLOR;
    private int activeColor = DEFAULT_ACTIVE_COLOR;

    private int viewX, viewY, viewWidth, viewHeight;
    private int barCross, trackStart, trackLength;
    private float maxScroll;
    private float scroll, targetScroll;
    private boolean dragging;
    private float grabOffset, grabThumb;
    private long lastLayoutNanos, lastTickNanos;
    private int clipDepth;

    public ScrollbarState step(float step) {
        this.step = step;
        return this;
    }

    public ScrollbarState barWidth(int width) {
        this.barWidth = width;
        return this;
    }

    public ScrollbarState minThumb(float size) {
        this.minThumb = size;
        return this;
    }

    public ScrollbarState colors(int track, int thumb, int active) {
        this.trackColor = track;
        this.thumbColor = thumb;
        this.activeColor = active;
        return this;
    }

    public ScrollbarState noTrack() {
        this.drawTrack = false;
        return this;
    }

    public ScrollbarState instant() {
        this.smooth = false;
        return this;
    }

    public ScrollbarState horizontal(boolean horizontal) {
        this.horizontal = horizontal;
        return this;
    }

    public ScrollbarState layout(int x, int y, int width, int height, float contentSize) {
        viewX = x;
        viewY = y;
        viewWidth = width;
        viewHeight = height;
        maxScroll = Math.max(0.0f, contentSize - viewLength());
        if (horizontal) {
            barCross = y + height - barWidth;
            trackStart = x;
            trackLength = width;
        } else {
            barCross = x + width - barWidth;
            trackStart = y;
            trackLength = height;
        }
        lastLayoutNanos = System.nanoTime();
        advance();
        return this;
    }

    public ScrollbarState barAt(int cross) {
        this.barCross = cross;
        return this;
    }

    public ScrollbarState track(int start, int length) {
        this.trackStart = start;
        this.trackLength = length;
        return this;
    }

    private void advance() {
        long now = System.nanoTime();
        boolean first = lastTickNanos == 0L;
        float dt = first ? 0.0f : Math.min(0.1f, (now - lastTickNanos) / 1_000_000_000.0f);
        lastTickNanos = now;
        targetScroll = Mth.clamp(targetScroll, 0.0f, maxScroll);
        if (dragging || !smooth || first) scroll = targetScroll;
        else if (dt > 0.0f) scroll += (targetScroll - scroll) * (1.0f - (float) Math.exp(-dt / SMOOTH_TAU));
        if (Math.abs(scroll - targetScroll) < 0.05f) scroll = targetScroll;
        scroll = Mth.clamp(scroll, 0.0f, maxScroll);
    }

    private boolean isActiveAt(long nanos) {
        return lastLayoutNanos != 0L && nanos - lastLayoutNanos < ACTIVE_WINDOW_NANOS;
    }

    public boolean isActive() {
        return isActiveAt(System.nanoTime());
    }

    public void clear() {
        lastLayoutNanos = 0L;
    }

    public void reset() {
        scroll = 0.0f;
        targetScroll = 0.0f;
        dragging = false;
    }

    public float scroll() {
        return scroll;
    }

    public int scrollPixels() {
        return Math.round(scroll);
    }

    public float targetScroll() {
        return targetScroll;
    }

    public float maxScroll() {
        return maxScroll;
    }

    public boolean canScroll() {
        return isActive() && maxScroll > 0.0f;
    }

    public boolean isDragging() {
        return dragging;
    }

    public int reserve() {
        return barWidth + BAR_GAP;
    }

    public int barWidth() {
        return barWidth;
    }

    public void scrollTo(float value) {
        targetScroll = Math.max(0.0f, isActive() ? Math.min(value, maxScroll) : value);
    }

    public void jumpTo(float value) {
        scrollTo(value);
        scroll = targetScroll;
    }

    public void scrollBy(float delta) {
        scrollTo(targetScroll + delta);
    }

    public void ensureVisible(float start, float end) {
        float viewStart = horizontal ? viewX : viewY;
        float offsetStart = start - viewStart;
        float offsetEnd = end - viewStart;
        if (offsetStart < targetScroll) scrollTo(offsetStart);
        else if (offsetEnd > targetScroll + viewLength()) scrollTo(offsetEnd - viewLength());
    }

    public boolean isVisible(float start, float size) {
        float viewStart = horizontal ? viewX : viewY;
        float shown = start - scroll;
        return shown + size > viewStart && shown < viewStart + viewLength();
    }

    public boolean isInView(double mouseX, double mouseY) {
        return mouseX >= viewX && mouseX < viewX + viewWidth && mouseY >= viewY && mouseY < viewY + viewHeight;
    }

    public boolean contains(double mouseX, double mouseY) {
        return isInView(mouseX, mouseY) || overTrack(mouseX, mouseY);
    }

    public double toContent(double mouse) {
        return mouse + scroll;
    }

    private int viewLength() {
        return horizontal ? viewWidth : viewHeight;
    }

    private float thumbSize() {
        if (maxScroll <= 0.0f || trackLength <= 0) return trackLength;
        float ratio = viewLength() / (viewLength() + maxScroll);
        return Mth.clamp(trackLength * ratio, Math.min(minThumb, trackLength), trackLength);
    }

    private float thumbStart(float thumb) {
        if (maxScroll <= 0.0f) return trackStart;
        return trackStart + (trackLength - thumb) * Mth.clamp(scroll / maxScroll, 0.0f, 1.0f);
    }

    private boolean overTrack(double mouseX, double mouseY) {
        if (maxScroll <= 0.0f) return false;
        double along = horizontal ? mouseX : mouseY;
        double cross = horizontal ? mouseY : mouseX;
        return cross >= barCross - GRAB_SLOP && cross <= barCross + barWidth + GRAB_SLOP && along >= trackStart && along <= trackStart + trackLength;
    }

    private boolean overThumb(double mouseX, double mouseY) {
        if (!overTrack(mouseX, mouseY)) return false;
        float thumb = thumbSize();
        float start = thumbStart(thumb);
        double along = horizontal ? mouseX : mouseY;
        return along >= start && along <= start + thumb;
    }

    private float scrollForPointer(double along) {
        float travel = trackLength - grabThumb;
        if (travel <= 0.0f || maxScroll <= 0.0f) return 0.0f;
        return Mth.clamp((float) (along - grabOffset - trackStart) / travel, 0.0f, 1.0f) * maxScroll;
    }

    public boolean scrollWheel(double delta) {
        if (!isActive() || !canScroll() || delta == 0.0) return false;
        scrollBy((float) (-Math.signum(delta) * step));
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!contains(mouseX, mouseY)) return false;
        return scrollWheel(delta);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isActive() || !overTrack(mouseX, mouseY)) return false;
        double along = horizontal ? mouseX : mouseY;
        grabThumb = thumbSize();
        float start = thumbStart(grabThumb);
        boolean onThumb = along >= start && along <= start + grabThumb;
        grabOffset = onThumb ? (float) (along - start) : grabThumb / 2.0f;
        dragging = true;
        jumpTo(scrollForPointer(along));
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!dragging) return false;
        jumpTo(scrollForPointer(horizontal ? mouseX : mouseY));
        return true;
    }

    public boolean mouseReleased() {
        if (!dragging) return false;
        dragging = false;
        return true;
    }

    public static boolean scrolled(double mouseX, double mouseY, double delta, ScrollbarState... bars) {
        for (ScrollbarState bar : bars) if (bar.mouseScrolled(mouseX, mouseY, delta)) return true;
        return false;
    }

    public static boolean clicked(double mouseX, double mouseY, int button, ScrollbarState... bars) {
        for (ScrollbarState bar : bars) if (bar.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }

    public static boolean dragged(double mouseX, double mouseY, ScrollbarState... bars) {
        for (ScrollbarState bar : bars) if (bar.mouseDragged(mouseX, mouseY)) return true;
        return false;
    }

    public static boolean released(ScrollbarState... bars) {
        boolean any = false;
        for (ScrollbarState bar : bars) any |= bar.mouseReleased();
        return any;
    }

    public static boolean anyDragging(ScrollbarState... bars) {
        for (ScrollbarState bar : bars) if (bar.isDragging()) return true;
        return false;
    }

    public void beginClip(GuiGraphics graphics) {
        beginClip(graphics, true);
    }

    public void beginClip(GuiGraphics graphics, boolean applyScroll) {
        if (horizontal) beginClip(graphics, viewY, viewHeight, applyScroll);
        else beginClip(graphics, viewX, viewWidth, applyScroll);
    }

    public void beginClip(GuiGraphics graphics, int crossStart, int crossLength, boolean applyScroll) {
        int x = horizontal ? viewX : crossStart;
        int y = horizontal ? crossStart : viewY;
        int width = horizontal ? viewWidth : crossLength;
        int height = horizontal ? crossLength : viewHeight;
        HudRender.scissor(graphics, x - 1, y - 1, x + width + 1, y + height);
        graphics.pose().pushPose();
        if (applyScroll) {
            if (horizontal) graphics.pose().translate(-scroll, 0.0f, 0.0f);
            else graphics.pose().translate(0.0f, -scroll, 0.0f);
        }
        clipDepth++;
    }

    public void endClip(GuiGraphics graphics) {
        if (clipDepth <= 0) return;
        clipDepth--;
        graphics.pose().popPose();
        graphics.disableScissor();
    }

    public void renderBar(GuiGraphics graphics, double mouseX, double mouseY) {
        if (!canScroll() || trackLength <= 0) return;
        float thumb = thumbSize();
        float start = thumbStart(thumb);
        boolean active = dragging || overThumb(mouseX, mouseY);
        int color = active ? activeColor : thumbColor;
        if (horizontal) {
            if (drawTrack) HudRender.rect(graphics, trackStart, barCross, trackLength, barWidth, trackColor);
            HudRender.rect(graphics, start, barCross, thumb, barWidth, color);
        } else {
            if (drawTrack) HudRender.rect(graphics, barCross, trackStart, barWidth, trackLength, trackColor);
            HudRender.rect(graphics, barCross, start, barWidth, thumb, color);
        }
    }
}
