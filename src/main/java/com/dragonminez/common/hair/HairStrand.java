package com.dragonminez.common.hair;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class HairStrand {
	public static final int MAX_SEGMENTS = 32;
	public static final float MIN_LENGTH = 0.1f;
	public static final float MAX_LENGTH = 128.0f;
	public static final float MIN_LENGTH_RATIO = 0.1f;
	public static final float MAX_LENGTH_RATIO = 4.0f;
	public static final float MIN_WIDTH = 0.1f;
	public static final float MAX_WIDTH = 16.0f;
	public static final float MIN_TAPER = 0.05f;
	public static final float MAX_TAPER = 2.0f;
	public static final float MIN_TAPER_CURVE = 0.25f;
	public static final float MAX_TAPER_CURVE = 4.0f;
	public static final float MAX_ROOT_OFFSET = 4.0f;
	public static final float MAX_BEND = 3600.0f;
	public static final float MAX_TWIST = 3600.0f;

	public static final int DEFAULT_SEGMENTS = 4;
	public static final float DEFAULT_LENGTH = 6.0f;
	public static final float DEFAULT_LENGTH_RATIO = 0.6f;
	public static final float DEFAULT_WIDTH = 2.0f;
	public static final float DEFAULT_DEPTH = 2.0f;
	public static final float DEFAULT_TAPER = 0.6f;
	public static final float DEFAULT_TAPER_CURVE = 1.0f;

	private int id;
	private int segments;
	private float length = DEFAULT_LENGTH;
	private float lengthRatio = DEFAULT_LENGTH_RATIO;
	private float width = DEFAULT_WIDTH;
	private float depth = DEFAULT_DEPTH;
	private float taper = DEFAULT_TAPER;
	private float taperCurve = DEFAULT_TAPER_CURVE;
	private float offsetX;
	private float offsetY;
	private float offsetZ;
	private float rotationX;
	private float rotationY;
	private float rotationZ;
	private float bendX;
	private float bendY;
	private float bendZ;
	private float twist;
	private HairJointStyle jointStyle = HairJointStyle.BLOCKS;
	private String color;
	private String tipColor;
	private final TreeMap<Integer, HairSegmentOverride> overrides = new TreeMap<>();

	public HairStrand() {}

	public HairStrand(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	void setId(int id) {
		this.id = id;
	}

	public boolean isVisible() {
		return segments > 0 && length > 0.0f;
	}

	public int getSegments() {
		return segments;
	}

	public void setSegments(int segments) {
		this.segments = Math.max(0, Math.min(MAX_SEGMENTS, segments));
	}

	public float getLength() {
		return length;
	}

	public void setLength(float length) {
		this.length = HairMath.clampFinite(length, MIN_LENGTH, MAX_LENGTH, this.length);
	}

	public float getLengthRatio() {
		return lengthRatio;
	}

	public void setLengthRatio(float lengthRatio) {
		this.lengthRatio = HairMath.clampFinite(lengthRatio, MIN_LENGTH_RATIO, MAX_LENGTH_RATIO, this.lengthRatio);
	}

	public float getWidth() {
		return width;
	}

	public void setWidth(float width) {
		this.width = HairMath.clampFinite(width, MIN_WIDTH, MAX_WIDTH, this.width);
	}

	public float getDepth() {
		return depth;
	}

	public void setDepth(float depth) {
		this.depth = HairMath.clampFinite(depth, MIN_WIDTH, MAX_WIDTH, this.depth);
	}

	public float getTaper() {
		return taper;
	}

	public void setTaper(float taper) {
		this.taper = HairMath.clampFinite(taper, MIN_TAPER, MAX_TAPER, this.taper);
	}

	public float getTaperCurve() {
		return taperCurve;
	}

	public void setTaperCurve(float taperCurve) {
		this.taperCurve = HairMath.clampFinite(taperCurve, MIN_TAPER_CURVE, MAX_TAPER_CURVE, this.taperCurve);
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
		this.offsetX = HairMath.clampFinite(x, -MAX_ROOT_OFFSET, MAX_ROOT_OFFSET, offsetX);
		this.offsetY = HairMath.clampFinite(y, -MAX_ROOT_OFFSET, MAX_ROOT_OFFSET, offsetY);
		this.offsetZ = HairMath.clampFinite(z, -MAX_ROOT_OFFSET, MAX_ROOT_OFFSET, offsetZ);
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
		this.rotationX = Float.isFinite(x) ? HairMath.wrapDegrees(x) : rotationX;
		this.rotationY = Float.isFinite(y) ? HairMath.wrapDegrees(y) : rotationY;
		this.rotationZ = Float.isFinite(z) ? HairMath.wrapDegrees(z) : rotationZ;
	}

	public float getBendX() {
		return bendX;
	}

	public float getBendY() {
		return bendY;
	}

	public float getBendZ() {
		return bendZ;
	}

	public void setBend(float x, float y, float z) {
		this.bendX = HairMath.clampFinite(x, -MAX_BEND, MAX_BEND, bendX);
		this.bendY = HairMath.clampFinite(y, -MAX_BEND, MAX_BEND, bendY);
		this.bendZ = HairMath.clampFinite(z, -MAX_BEND, MAX_BEND, bendZ);
	}

	public float getTwist() {
		return twist;
	}

	public void setTwist(float twist) {
		this.twist = HairMath.clampFinite(twist, -MAX_TWIST, MAX_TWIST, this.twist);
	}

	public HairJointStyle getJointStyle() {
		return jointStyle;
	}

	public void setJointStyle(HairJointStyle jointStyle) {
		this.jointStyle = jointStyle != null ? jointStyle : HairJointStyle.BLOCKS;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = HairColors.normalize(color);
	}

	public String getTipColor() {
		return tipColor;
	}

	public void setTipColor(String tipColor) {
		this.tipColor = HairColors.normalize(tipColor);
	}

	public HairSegmentOverride getOverride(int index) {
		return overrides.get(index);
	}

	public HairSegmentOverride getOrCreateOverride(int index) {
		return overrides.computeIfAbsent(index, HairSegmentOverride::new);
	}

	public void removeOverride(int index) {
		overrides.remove(index);
	}

	public void clearOverrides() {
		overrides.clear();
	}

	public Collection<HairSegmentOverride> getOverrides() {
		return overrides.values();
	}

	public void makeVisibleWithDefaults() {
		if (isVisible()) return;
		segments = DEFAULT_SEGMENTS;
		length = DEFAULT_LENGTH;
		lengthRatio = DEFAULT_LENGTH_RATIO;
		width = DEFAULT_WIDTH;
		depth = DEFAULT_DEPTH;
		taper = DEFAULT_TAPER;
		taperCurve = DEFAULT_TAPER_CURVE;
	}

	public void mirror() {
		rotationY = HairMath.wrapDegrees(-rotationY);
		rotationZ = HairMath.wrapDegrees(-rotationZ);
		bendY = -bendY;
		bendZ = -bendZ;
		twist = -twist;
		offsetX = -offsetX;
		for (HairSegmentOverride override : overrides.values()) override.mirror();
	}

	public void copyFrom(HairStrand other) {
		this.segments = other.segments;
		this.length = other.length;
		this.lengthRatio = other.lengthRatio;
		this.width = other.width;
		this.depth = other.depth;
		this.taper = other.taper;
		this.taperCurve = other.taperCurve;
		this.offsetX = other.offsetX;
		this.offsetY = other.offsetY;
		this.offsetZ = other.offsetZ;
		this.rotationX = other.rotationX;
		this.rotationY = other.rotationY;
		this.rotationZ = other.rotationZ;
		this.bendX = other.bendX;
		this.bendY = other.bendY;
		this.bendZ = other.bendZ;
		this.twist = other.twist;
		this.jointStyle = other.jointStyle;
		this.color = other.color;
		this.tipColor = other.tipColor;
		this.overrides.clear();
		for (Map.Entry<Integer, HairSegmentOverride> entry : other.overrides.entrySet()) {
			this.overrides.put(entry.getKey(), entry.getValue().copy());
		}
	}

	public HairStrand copy() {
		HairStrand copy = new HairStrand(id);
		copy.copyFrom(this);
		return copy;
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("i", id);
		tag.putInt("sg", segments);
		tag.putFloat("ln", length);
		if (lengthRatio != 1.0f) tag.putFloat("lr", lengthRatio);
		if (width != DEFAULT_WIDTH) tag.putFloat("w", width);
		if (depth != DEFAULT_DEPTH) tag.putFloat("d", depth);
		if (taper != 1.0f) tag.putFloat("tp", taper);
		if (taperCurve != 1.0f) tag.putFloat("tg", taperCurve);
		if (offsetX != 0.0f) tag.putFloat("ox", offsetX);
		if (offsetY != 0.0f) tag.putFloat("oy", offsetY);
		if (offsetZ != 0.0f) tag.putFloat("oz", offsetZ);
		if (rotationX != 0.0f) tag.putFloat("rx", rotationX);
		if (rotationY != 0.0f) tag.putFloat("ry", rotationY);
		if (rotationZ != 0.0f) tag.putFloat("rz", rotationZ);
		if (bendX != 0.0f) tag.putFloat("bx", bendX);
		if (bendY != 0.0f) tag.putFloat("by", bendY);
		if (bendZ != 0.0f) tag.putFloat("bz", bendZ);
		if (twist != 0.0f) tag.putFloat("tw", twist);
		if (jointStyle != HairJointStyle.BLOCKS) tag.putByte("js", (byte) jointStyle.ordinal());
		if (color != null) tag.putString("c", color);
		if (tipColor != null) tag.putString("tc", tipColor);

		ListTag overrideList = new ListTag();
		for (HairSegmentOverride override : overrides.values()) {
			if (override.getIndex() < segments && !override.isIdentity()) overrideList.add(override.save());
		}
		if (!overrideList.isEmpty()) tag.put("so", overrideList);
		return tag;
	}

	public void load(CompoundTag tag) {
		this.id = tag.getInt("i");
		setSegments(tag.getInt("sg"));
		this.length = tag.contains("ln") ? HairMath.clampFinite(tag.getFloat("ln"), MIN_LENGTH, MAX_LENGTH, DEFAULT_LENGTH) : DEFAULT_LENGTH;
		this.lengthRatio = tag.contains("lr") ? HairMath.clampFinite(tag.getFloat("lr"), MIN_LENGTH_RATIO, MAX_LENGTH_RATIO, 1.0f) : 1.0f;
		this.width = tag.contains("w") ? HairMath.clampFinite(tag.getFloat("w"), MIN_WIDTH, MAX_WIDTH, DEFAULT_WIDTH) : DEFAULT_WIDTH;
		this.depth = tag.contains("d") ? HairMath.clampFinite(tag.getFloat("d"), MIN_WIDTH, MAX_WIDTH, DEFAULT_DEPTH) : DEFAULT_DEPTH;
		this.taper = tag.contains("tp") ? HairMath.clampFinite(tag.getFloat("tp"), MIN_TAPER, MAX_TAPER, 1.0f) : 1.0f;
		this.taperCurve = tag.contains("tg") ? HairMath.clampFinite(tag.getFloat("tg"), MIN_TAPER_CURVE, MAX_TAPER_CURVE, 1.0f) : 1.0f;
		this.offsetX = 0.0f;
		this.offsetY = 0.0f;
		this.offsetZ = 0.0f;
		setOffset(tag.getFloat("ox"), tag.getFloat("oy"), tag.getFloat("oz"));
		this.rotationX = 0.0f;
		this.rotationY = 0.0f;
		this.rotationZ = 0.0f;
		setRotation(tag.getFloat("rx"), tag.getFloat("ry"), tag.getFloat("rz"));
		this.bendX = 0.0f;
		this.bendY = 0.0f;
		this.bendZ = 0.0f;
		setBend(tag.getFloat("bx"), tag.getFloat("by"), tag.getFloat("bz"));
		this.twist = 0.0f;
		setTwist(tag.getFloat("tw"));
		this.jointStyle = HairJointStyle.byId(tag.getByte("js"));
		this.color = tag.contains("c") ? HairColors.normalize(tag.getString("c")) : null;
		this.tipColor = tag.contains("tc") ? HairColors.normalize(tag.getString("tc")) : null;
		this.overrides.clear();
		if (tag.contains("so", Tag.TAG_LIST)) {
			ListTag overrideList = tag.getList("so", Tag.TAG_COMPOUND);
			for (int i = 0; i < overrideList.size(); i++) {
				HairSegmentOverride override = HairSegmentOverride.load(overrideList.getCompound(i));
				if (override.getIndex() >= 0 && override.getIndex() < MAX_SEGMENTS) overrides.put(override.getIndex(), override);
			}
		}
	}
}
