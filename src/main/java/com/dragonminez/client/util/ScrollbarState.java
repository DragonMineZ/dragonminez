package com.dragonminez.client.util;

import lombok.Getter;

public class ScrollbarState {
    private int barX, barWidth, trackY, trackHeight;
    private float maxScroll;
    @Getter
    private boolean dragging;

    public void update(int barX, int barWidth, int trackY, int trackHeight, float maxScroll) {
        this.barX = barX;
        this.barWidth = barWidth;
        this.trackY = trackY;
        this.trackHeight = trackHeight;
        this.maxScroll = maxScroll;
    }

    public boolean tryStartDrag(double mouseX, double mouseY) {
        if (maxScroll > 0 && TextUtil.overScrollBar(mouseX, mouseY, barX, barWidth, trackY, trackHeight)) {
            dragging = true;
            return true;
        }
        return false;
    }

    public void clear() {
        update(0, 0, 0, 0, 0);
    }

	public void stopDrag() {
        dragging = false;
    }

    public float scrollFor(double mouseY) {
        return TextUtil.scrollFromBar(mouseY, trackY, trackHeight, maxScroll);
    }
}
