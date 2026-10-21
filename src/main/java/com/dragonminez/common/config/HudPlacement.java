package com.dragonminez.common.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HudPlacement {
	private Float anchorX = 0.0f;
	private Float anchorY = 0.0f;
	private Float offsetX = 0.0f;
	private Float offsetY = 0.0f;
	private Float scale = 1.0f;
	private Boolean visible = true;
	private Boolean hotbar = false;
	private Boolean mirrored = false;

	public HudPlacement() {}

	public HudPlacement(float anchorX, float anchorY, float offsetX, float offsetY, float scale) {
		this.anchorX = anchorX;
		this.anchorY = anchorY;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.scale = scale;
	}

	public HudPlacement hotbar() {
		this.hotbar = true;
		return this;
	}

	public HudPlacement copy() {
		HudPlacement copy = new HudPlacement(getAnchorX(), getAnchorY(), getOffsetX(), getOffsetY(), getScale());
		copy.visible = getVisible();
		copy.hotbar = getHotbar();
		copy.mirrored = getMirrored();
		return copy;
	}

	public Float getAnchorX() { return finite(anchorX, 0.0f); }
	public Float getAnchorY() { return finite(anchorY, 0.0f); }
	public Float getOffsetX() { return finite(offsetX, 0.0f); }
	public Float getOffsetY() { return finite(offsetY, 0.0f); }
	public Float getScale() { return Math.max(0.25f, Math.min(6.0f, finite(scale, 1.0f))); }
	public Boolean getVisible() { return visible == null || visible; }
	public Boolean getHotbar() { return hotbar != null && hotbar; }
	public Boolean getMirrored() { return mirrored != null && mirrored; }

	private static float finite(Float value, float fallback) {
		return value != null && Float.isFinite(value) ? value : fallback;
	}
}
