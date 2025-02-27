package com.yuseix.dragonminez.utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class HairSerializationData {
    private static final int COLOR_PALETTE_SIZE = 4;
    private final int[] colorPalette;
    // Bit masks for the control byte
    private static final byte ENABLED_MASK = (byte) 0x80;       // 1000 0000
    private static final byte COLOR_MASK = (byte) 0x60;        // 0110 0000
    private static final byte RESERVED_MASK = (byte) 0x1F;     // 0001 1111

    public static class StackData {
        private byte controlByte;     // Contains enabled flag, color index, and reserved bits
        private float curveX;
        private float curveY;
        private float lengthModifier;
        private byte enabledStrands;  // 4 bits for strand toggles, 4 bits reserved

        public StackData() {
            this.controlByte = ENABLED_MASK; // Enabled by default, color 0
            this.curveX = 0.0f;
            this.curveY = 0.0f;
            this.lengthModifier = 1.0f;
            this.enabledStrands = 0xF; // All strands enabled by default
        }

        public boolean isEnabled() {
            return (controlByte & ENABLED_MASK) != 0;
        }

        public void setEnabled(boolean enabled) {
            if (enabled) {
                controlByte |= ENABLED_MASK;
            } else {
                controlByte &= ~ENABLED_MASK;
            }
        }

        public byte getColorIndex() {
            return (byte)((controlByte & COLOR_MASK) >>> 5);
        }

        public void setColorIndex(byte colorIndex) {
            // Clear existing color bits and set new ones
            controlByte = (byte)((controlByte & ~COLOR_MASK) | ((colorIndex & 0x3) << 5));
        }

        // Reserved bits getters/setters for future use
        public byte getReservedBits() {
            return (byte)(controlByte & RESERVED_MASK);
        }

        public void setReservedBits(byte reservedBits) {
            controlByte = (byte)((controlByte & ~RESERVED_MASK) | (reservedBits & RESERVED_MASK));
        }

        public byte[] serialize() {
            ByteBuffer buffer = ByteBuffer.allocate(10); // 1 + 4 + 4 + 1 bytes
            buffer.put(controlByte);
            buffer.putFloat(curveX);
            buffer.putFloat(curveY);
            buffer.put(enabledStrands);
            return buffer.array();
        }

        public static StackData deserialize(byte[] data, int offset) {
            ByteBuffer buffer = ByteBuffer.wrap(data, offset, 10);
            StackData stack = new StackData();
            stack.controlByte = buffer.get();
            stack.curveX = buffer.getFloat();
            stack.curveY = buffer.getFloat();
            stack.enabledStrands = buffer.get();
            return stack;
        }

        // Basic getters/setters for other fields
        public float getCurveX() { return curveX; }
        public void setCurveX(float curveX) { this.curveX = curveX; }
        public float getCurveY() { return curveY; }
        public void setCurveY(float curveY) { this.curveY = curveY; }
        public float getLengthModifier() { return lengthModifier; }
        public void setLengthModifier(float lengthModifier) { this.lengthModifier = lengthModifier; }
        
        // Strand enable/disable operations using bitwise operations
        public boolean isStrandEnabled(int index) {
            return (enabledStrands & (1 << index)) != 0;
        }

        public void setStrandEnabled(int index, boolean enabled) {
            if (enabled) {
                enabledStrands |= (1 << index);
            } else {
                enabledStrands &= ~(1 << index);
            }
        }
    }

    private List<StackData> stacks; // Four faces with sixteen stacks each. Each stack has eight strands.

    public HairSerializationData() {
        this.stacks = new ArrayList<>();
        this.colorPalette = new int[COLOR_PALETTE_SIZE];
        // Initialize with default colors (black)
        for (int i = 0; i < COLOR_PALETTE_SIZE; i++) {
            colorPalette[i] = 0xFF000000;
        }
    }

    public void setColorInPalette(int index, int color) {
        if (index < 0 || index >= COLOR_PALETTE_SIZE) {
            throw new IllegalArgumentException("Color index must be between 0 and " + (COLOR_PALETTE_SIZE - 1));
        }
        colorPalette[index] = color;
    }

    public int getColorFromPalette(int index) {
        if (index < 0 || index >= COLOR_PALETTE_SIZE) {
            throw new IllegalArgumentException("Color index must be between 0 and " + (COLOR_PALETTE_SIZE - 1));
        }
        return colorPalette[index];
    }

    public byte[] serialize() {
        int strandsCount = stacks.size();
        // Size: palette (4 colors * 4 bytes each) + strands count (2 bytes) + (strand size * count)
        ByteBuffer buffer = ByteBuffer.allocate((COLOR_PALETTE_SIZE * 4) + 2 + (strandsCount * 10));
        
        // Write color palette
        for (int color : colorPalette) {
            buffer.putInt(color);
        }
        
        // Write number of strands as a short
        buffer.putShort((short)strandsCount);
        
        // Write each strand's data
        for (StackData strand : stacks) {
            buffer.put(strand.serialize());
        }
        
        return buffer.array();
    }

    public static HairSerializationData deserialize(byte[] data) {
        HairSerializationData hairData = new HairSerializationData();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        // Read color palette
        for (int i = 0; i < COLOR_PALETTE_SIZE; i++) {
            hairData.colorPalette[i] = buffer.getInt();
        }
        
        // Read number of strands
        int strandsCount = buffer.getShort() & 0xFFFF;
        
        // Read each strand's data
        for (int i = 0; i < strandsCount && buffer.remaining() >= 10; i++) {
            hairData.stacks.add(StackData.deserialize(data, buffer.position()));
            buffer.position(buffer.position() + 10);
        }
        
        return hairData;
    }

    public List<StackData> getStacks() {
        return stacks;
    }

    public void addStack(StackData stack) {
        stacks.add(stack);
    }
}