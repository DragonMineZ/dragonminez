package com.dragonminez.common.hair;

import net.minecraft.nbt.CompoundTag;

public final class HairSegmentOverride {
	public static final float MAX_OFFSET = 2.0f;
	public static final float MAX_ROTATION = 180.0f;
	public static final float MIN_SCALE = 0.1f;
	public static final float MAX_SCALE = 16.0f;

	private static final float EPSILON = 1.0e-4f;

	private final int index;
	private float offsetX;
	private float offsetY;
	private float offsetZ;
	private float rotationX;
	private float rotationY;
	private float rotationZ;
	private float lengthScale = 1.0f;
	private float widthScale = 1.0f;
	private float depthScale = 1.0f;
	private String color;

	public HairSegmentOverride(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public float getOffsetX() {
		return offsetX;
	}

	public float getOffsetY() {
		return offsetY;
	}

	public float getOffsetZ() {
		return offsetZ;
	}

	public void setOffset(float x, float y, float z) {
		this.offsetX = HairMath.clampFinite(x, -MAX_OFFSET, MAX_OFFSET, offsetX);
		this.offsetY = HairMath.clampFinite(y, -MAX_OFFSET, MAX_OFFSET, offsetY);
		this.offsetZ = HairMath.clampFinite(z, -MAX_OFFSET, MAX_OFFSET, offsetZ);
	}

	public float getRotationX() {
		return rotationX;
	}

	public float getRotationY() {
		return rotationY;
	}

	public float getRotationZ() {
		return rotationZ;
	}

	public void setRotation(float x, float y, float z) {
		this.rotationX = HairMath.clampFinite(x, -MAX_ROTATION, MAX_ROTATION, rotationX);
		this.rotationY = HairMath.clampFinite(y, -MAX_ROTATION, MAX_ROTATION, rotationY);
		this.rotationZ = HairMath.clampFinite(z, -MAX_ROTATION, MAX_ROTATION, rotationZ);
	}

	public float getLengthScale() {
		return lengthScale;
	}

	public void setLengthScale(float lengthScale) {
		this.lengthScale = HairMath.clampFinite(lengthScale, MIN_SCALE, MAX_SCALE, this.lengthScale);
	}

	public float getWidthScale() {
		return widthScale;
	}

	public void setWidthScale(float widthScale) {
		this.widthScale = HairMath.clampFinite(widthScale, MIN_SCALE, MAX_SCALE, this.widthScale);
	}

	public float getDepthScale() {
		return depthScale;
	}

	public void setDepthScale(float depthScale) {
		this.depthScale = HairMath.clampFinite(depthScale, MIN_SCALE, MAX_SCALE, this.depthScale);
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = HairColors.normalize(color);
	}

	public boolean isIdentity() {
		return Math.abs(offsetX) < EPSILON && Math.abs(offsetY) < EPSILON && Math.abs(offsetZ) < EPSILON
				&& Math.abs(rotationX) < EPSILON && Math.abs(rotationY) < EPSILON && Math.abs(rotationZ) < EPSILON
				&& Math.abs(lengthScale - 1.0f) < EPSILON && Math.abs(widthScale - 1.0f) < EPSILON
				&& Math.abs(depthScale - 1.0f) < EPSILON && color == null;
	}

	public HairSegmentOverride copy() {
		return copyAs(index);
	}

	public HairSegmentOverride copyAs(int newIndex) {
		HairSegmentOverride copy = new HairSegmentOverride(newIndex);
		copy.offsetX = offsetX;
		copy.offsetY = offsetY;
		copy.offsetZ = offsetZ;
		copy.rotationX = rotationX;
		copy.rotationY = rotationY;
		copy.rotationZ = rotationZ;
		copy.lengthScale = lengthScale;
		copy.widthScale = widthScale;
		copy.depthScale = depthScale;
		copy.color = color;
		return copy;
	}

	public void mirror() {
		offsetX = -offsetX;
		rotationY = -rotationY;
		rotationZ = -rotationZ;
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("k", index);
		if (offsetX != 0.0f) tag.putFloat("ox", offsetX);
		if (offsetY != 0.0f) tag.putFloat("oy", offsetY);
		if (offsetZ != 0.0f) tag.putFloat("oz", offsetZ);
		if (rotationX != 0.0f) tag.putFloat("rx", rotationX);
		if (rotationY != 0.0f) tag.putFloat("ry", rotationY);
		if (rotationZ != 0.0f) tag.putFloat("rz", rotationZ);
		if (lengthScale != 1.0f) tag.putFloat("ls", lengthScale);
		if (widthScale != 1.0f) tag.putFloat("ws", widthScale);
		if (depthScale != 1.0f) tag.putFloat("ds", depthScale);
		if (color != null) tag.putString("c", color);
		return tag;
	}

	public static HairSegmentOverride load(CompoundTag tag) {
		HairSegmentOverride override = new HairSegmentOverride(tag.getInt("k"));
		override.setOffset(tag.getFloat("ox"), tag.getFloat("oy"), tag.getFloat("oz"));
		override.setRotation(tag.getFloat("rx"), tag.getFloat("ry"), tag.getFloat("rz"));
		if (tag.contains("ls")) override.setLengthScale(tag.getFloat("ls"));
		if (tag.contains("ws")) override.setWidthScale(tag.getFloat("ws"));
		if (tag.contains("ds")) override.setDepthScale(tag.getFloat("ds"));
		if (tag.contains("c")) override.setColor(tag.getString("c"));
		return override;
	}
}
