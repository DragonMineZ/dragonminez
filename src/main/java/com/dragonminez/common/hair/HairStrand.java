package com.dragonminez.common.hair;

import net.minecraft.nbt.CompoundTag;

public class HairStrand {
    public static final int MAX_CUBE_COUNT = 8;
    public static final int MAX_LENGTH = 50;
    private int length = 0;

	private float lengthScale = 1.0f;

    private float rotationX = 0.0f;
    private float rotationY = 0.0f;
    private float rotationZ = 0.0f;

    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float scaleZ = 1.0f;

    private float cubeWidth = 2.0f;
    private float cubeHeight = 2.0f;
    private float cubeDepth = 2.0f;

    private String color = null;

    private float curveX = 0.0f;
    private float curveY = 0.0f;
    private float curveZ = 0.0f;

    private int id = 0;
    
    public HairStrand() {}
    
    public HairStrand(int id) {
        this.id = id;
    }

	public float getLengthScale() { return lengthScale; }
	public void setLengthScale(float scale) { this.lengthScale = scale; }

    public int getLength() { return length; }
    
    public void setLength(int length) {
        this.length = Math.max(0, Math.min(MAX_LENGTH, length));
    }
    
    public void addCube() {
        if (length < MAX_LENGTH) {
            length++;
        }
    }
    
    public void removeCube() {
        if (length > 0) {
            length--;
        }
    }
    
    public boolean isVisible() {
        return length > 0;
    }

    public int getCubeCount() {
        return length;
    }

    public float getStretchFactor() {
		return lengthScale;
    }

    public float getRotationX() { return rotationX; }
    public float getRotationY() { return rotationY; }
    public float getRotationZ() { return rotationZ; }
    
    public void setRotation(float x, float y, float z) {
        this.rotationX = x;
        this.rotationY = y;
        this.rotationZ = z;
    }

    public float getScaleX() { return scaleX; }
    public float getScaleY() { return scaleY; }
    public float getScaleZ() { return scaleZ; }
    
    public void setScale(float x, float y, float z) {
        this.scaleX = Math.max(0.1f, x);
        this.scaleY = Math.max(0.1f, y);
        this.scaleZ = Math.max(0.1f, z);
    }

    public float getCubeWidth() { return cubeWidth; }
    public float getCubeHeight() { return cubeHeight; }
    public float getCubeDepth() { return cubeDepth; }

    public float getCurveX() { return curveX; }
    public float getCurveY() { return curveY; }
    public float getCurveZ() { return curveZ; }
    
    public void setCurve(float x, float y, float z) {
        this.curveX = x;
        this.curveY = y;
        this.curveZ = z;
    }

    public String getColor() { return color; }
    public boolean hasCustomColor() { return color != null && !color.isEmpty(); }
    
    public void setColor(String color) {
        this.color = color;
    }

    public int getId() { return id; }
	protected void setId(int id) { this.id = id; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (id != 0) tag.putInt("i", id);
        if (length != 0) tag.putInt("l", length);
		if (lengthScale != 1.0f) tag.putFloat("ls", lengthScale);

        if (rotationX != 0.0f) tag.putFloat("rx", rotationX);
        if (rotationY != 0.0f) tag.putFloat("ry", rotationY);
        if (rotationZ != 0.0f) tag.putFloat("rz", rotationZ);

        if (scaleX != 1.0f) tag.putFloat("sx", scaleX);
        if (scaleY != 1.0f) tag.putFloat("sy", scaleY);
        if (scaleZ != 1.0f) tag.putFloat("sz", scaleZ);

        if (cubeWidth != 2.0f) tag.putFloat("cw", cubeWidth);
        if (cubeHeight != 2.0f) tag.putFloat("ch", cubeHeight);
        if (cubeDepth != 2.0f) tag.putFloat("cd", cubeDepth);

        if (curveX != 0.0f) tag.putFloat("cx", curveX);
        if (curveY != 0.0f) tag.putFloat("cy", curveY);
        if (curveZ != 0.0f) tag.putFloat("cz", curveZ);

        if (color != null) tag.putString("c", color);

        return tag;
    }
    
    public void load(CompoundTag tag) {
        this.id = tag.contains("i") ? tag.getInt("i") : tag.getInt("Id");
        this.length = tag.contains("l") ? tag.getInt("l") : tag.getInt("Length");
		this.lengthScale = tag.contains("ls") ? tag.getFloat("ls") : (tag.contains("LengthScale") ? tag.getFloat("LengthScale") : 1.0f);

        this.rotationX = tag.contains("rx") ? tag.getFloat("rx") : tag.getFloat("RotX");
        this.rotationY = tag.contains("ry") ? tag.getFloat("ry") : tag.getFloat("RotY");
        this.rotationZ = tag.contains("rz") ? tag.getFloat("rz") : tag.getFloat("RotZ");

        this.scaleX = tag.contains("sx") ? tag.getFloat("sx") :
                     (tag.contains("ScaleX") ? tag.getFloat("ScaleX") : 1.0f);
        this.scaleY = tag.contains("sy") ? tag.getFloat("sy") :
                     (tag.contains("ScaleY") ? tag.getFloat("ScaleY") : 1.0f);
        this.scaleZ = tag.contains("sz") ? tag.getFloat("sz") :
                     (tag.contains("ScaleZ") ? tag.getFloat("ScaleZ") : 1.0f);

        this.cubeWidth = tag.contains("cw") ? tag.getFloat("cw") :
                        (tag.contains("CubeW") ? tag.getFloat("CubeW") : 2.0f);
        this.cubeHeight = tag.contains("ch") ? tag.getFloat("ch") :
                         (tag.contains("CubeH") ? tag.getFloat("CubeH") : 2.0f);
        this.cubeDepth = tag.contains("cd") ? tag.getFloat("cd") :
                        (tag.contains("CubeD") ? tag.getFloat("CubeD") : 2.0f);

        this.curveX = tag.contains("cx") ? tag.getFloat("cx") : tag.getFloat("CurveX");
        this.curveY = tag.contains("cy") ? tag.getFloat("cy") : tag.getFloat("CurveY");
        this.curveZ = tag.contains("cz") ? tag.getFloat("cz") : tag.getFloat("CurveZ");

		this.color = tag.contains("c") ? tag.getString("c") :
                    (tag.contains("Color") ? tag.getString("Color") : null);
    }
    
    public HairStrand copy() {
        HairStrand copy = new HairStrand(this.id);
        copy.length = this.length;
		copy.lengthScale = this.lengthScale;
        copy.rotationX = this.rotationX;
        copy.rotationY = this.rotationY;
        copy.rotationZ = this.rotationZ;
        copy.scaleX = this.scaleX;
        copy.scaleY = this.scaleY;
        copy.scaleZ = this.scaleZ;
        copy.cubeWidth = this.cubeWidth;
        copy.cubeHeight = this.cubeHeight;
        copy.cubeDepth = this.cubeDepth;
        copy.curveX = this.curveX;
        copy.curveY = this.curveY;
        copy.curveZ = this.curveZ;
        copy.color = this.color;
        return copy;
    }
}
