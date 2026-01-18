package com.dragonminez.common.hair;

import net.minecraft.nbt.CompoundTag;

public class HairStrand {
    public static final int MAX_LENGTH = 4;
    private int length = 0;

    private float offsetX = 0.0f;
    private float offsetY = 0.0f;
    private float offsetZ = 0.0f;

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

    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getOffsetZ() { return offsetZ; }
    
    public void setOffset(float x, float y, float z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
    }
    
    public void addOffset(float dx, float dy, float dz) {
        this.offsetX += dx;
        this.offsetY += dy;
        this.offsetZ += dz;
    }

    public float getRotationX() { return rotationX; }
    public float getRotationY() { return rotationY; }
    public float getRotationZ() { return rotationZ; }
    
    public void setRotation(float x, float y, float z) {
        this.rotationX = x;
        this.rotationY = y;
        this.rotationZ = z;
    }
    
    public void addRotation(float dx, float dy, float dz) {
        this.rotationX += dx;
        this.rotationY += dy;
        this.rotationZ += dz;
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
    
    public void setCubeDimensions(float width, float height, float depth) {
        this.cubeWidth = Math.max(0.5f, width);
        this.cubeHeight = Math.max(0.5f, height);
        this.cubeDepth = Math.max(0.5f, depth);
    }

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
    public void setId(int id) { this.id = id; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.putInt("Length", length);
        tag.putFloat("OffsetX", offsetX);
        tag.putFloat("OffsetY", offsetY);
        tag.putFloat("OffsetZ", offsetZ);
        tag.putFloat("RotX", rotationX);
        tag.putFloat("RotY", rotationY);
        tag.putFloat("RotZ", rotationZ);
        tag.putFloat("ScaleX", scaleX);
        tag.putFloat("ScaleY", scaleY);
        tag.putFloat("ScaleZ", scaleZ);
        tag.putFloat("CubeW", cubeWidth);
        tag.putFloat("CubeH", cubeHeight);
        tag.putFloat("CubeD", cubeDepth);
        tag.putFloat("CurveX", curveX);
        tag.putFloat("CurveY", curveY);
        tag.putFloat("CurveZ", curveZ);
        if (color != null) {
            tag.putString("Color", color);
        }
        return tag;
    }
    
    public void load(CompoundTag tag) {
        this.id = tag.getInt("Id");
        this.length = tag.getInt("Length");
        this.offsetX = tag.getFloat("OffsetX");
        this.offsetY = tag.getFloat("OffsetY");
        this.offsetZ = tag.getFloat("OffsetZ");
        this.rotationX = tag.getFloat("RotX");
        this.rotationY = tag.getFloat("RotY");
        this.rotationZ = tag.getFloat("RotZ");
        this.scaleX = tag.contains("ScaleX") ? tag.getFloat("ScaleX") : 1.0f;
        this.scaleY = tag.contains("ScaleY") ? tag.getFloat("ScaleY") : 1.0f;
        this.scaleZ = tag.contains("ScaleZ") ? tag.getFloat("ScaleZ") : 1.0f;
        this.cubeWidth = tag.contains("CubeW") ? tag.getFloat("CubeW") : 2.0f;
        this.cubeHeight = tag.contains("CubeH") ? tag.getFloat("CubeH") : 2.0f;
        this.cubeDepth = tag.contains("CubeD") ? tag.getFloat("CubeD") : 2.0f;
        this.curveX = tag.getFloat("CurveX");
        this.curveY = tag.getFloat("CurveY");
        this.curveZ = tag.getFloat("CurveZ");
        if (tag.contains("Color")) {
            this.color = tag.getString("Color");
        }
    }
    
    public HairStrand copy() {
        HairStrand copy = new HairStrand(this.id);
        copy.length = this.length;
        copy.offsetX = this.offsetX;
        copy.offsetY = this.offsetY;
        copy.offsetZ = this.offsetZ;
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
