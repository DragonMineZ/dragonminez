package com.dragonminez.common.config;

public class HudPlacement {
	private Float anchorX = 0.0F;
	private Float anchorY = 0.0F;
	private Float offsetX = 0.0F;
	private Float offsetY = 0.0F;
	private Float scale = 1.0F;
	private Boolean visible = true;
	private Boolean hotbar = false;
	private Boolean mirrored = false;

	public HudPlacement() {
	}

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

	public Float getAnchorX() {
		return finite(anchorX, 0.0F);
	}

	public Float getAnchorY() {
		return finite(anchorY, 0.0F);
	}

	public Float getOffsetX() {
		return finite(offsetX, 0.0F);
	}

	public Float getOffsetY() {
		return finite(offsetY, 0.0F);
	}

	public Float getScale() {
		return Math.max(0.25F, Math.min(6.0F, finite(scale, 1.0F)));
	}

	public Boolean getVisible() {
		return visible == null || visible;
	}

	public Boolean getHotbar() {
		return hotbar != null && hotbar;
	}

	public Boolean getMirrored() {
		return mirrored != null && mirrored;
	}

	private static float finite(Float value, float fallback) {
		return value != null && Float.isFinite(value) ? value : fallback;
	}

	public void setAnchorX(final Float anchorX) {
		this.anchorX = anchorX;
	}

	public void setAnchorY(final Float anchorY) {
		this.anchorY = anchorY;
	}

	public void setOffsetX(final Float offsetX) {
		this.offsetX = offsetX;
	}

	public void setOffsetY(final Float offsetY) {
		this.offsetY = offsetY;
	}

	public void setScale(final Float scale) {
		this.scale = scale;
	}

	public void setVisible(final Boolean visible) {
		this.visible = visible;
	}

	public void setHotbar(final Boolean hotbar) {
		this.hotbar = hotbar;
	}

	public void setMirrored(final Boolean mirrored) {
		this.mirrored = mirrored;
	}
}
