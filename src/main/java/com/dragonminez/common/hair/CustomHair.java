package com.dragonminez.common.hair;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Map;

public class CustomHair {
    public static final int FRONT_STRANDS = 4;
    public static final int SIDE_STRANDS = 16;

    public enum HairFace {
        FRONT(FRONT_STRANDS, 1, 4),
        BACK(SIDE_STRANDS, 4, 4),
        LEFT(SIDE_STRANDS, 4, 4),
        RIGHT(SIDE_STRANDS, 4, 4),
        TOP(SIDE_STRANDS, 4, 4);

        public final int maxStrands;
        public final int rows;
        public final int cols;

        HairFace(int maxStrands, int rows, int cols) {
            this.maxStrands = maxStrands;
            this.rows = rows;
            this.cols = cols;
        }
    }

    private final Map<HairFace, HairStrand[]> strandsByFace = new EnumMap<>(HairFace.class);
    private String globalColor = "#000000";
    private String name = "Custom";
    private int version = 1;

    public CustomHair() {
        initializeStrands();
    }


    private void initializeStrands() {
        int idCounter = 0;
        for (HairFace face : HairFace.values()) {
            HairStrand[] strands = new HairStrand[face.maxStrands];
            for (int i = 0; i < face.maxStrands; i++) {
                strands[i] = new HairStrand(idCounter++);
                initializeStrandPosition(strands[i], face, i);
            }
            strandsByFace.put(face, strands);
        }
    }

    private void initializeStrandPosition(HairStrand strand, HairFace face, int index) {
        int row = index / face.cols;
        int col = index % face.cols;
        float[] positions = {-3f, -1f, 1f, 3f};
        float[] yOffsets = {0f, -1.5f, -3f, -4.5f};

        float gridX = positions[col % 4];
        float gridZ = positions[row % 4];
        float rowYOffset = yOffsets[row % 4];

        switch (face) {
            case FRONT -> {
                strand.setOffset(gridX, 5, 3.5f);
                strand.setRotation(90, 0, 0);
            }
            case BACK -> {
                strand.setOffset(gridX, 5 + rowYOffset, -gridZ);
                strand.setRotation(-90, 0, 0);
            }
            case LEFT -> {
                strand.setOffset(-3f, 5 + rowYOffset, gridX);
                strand.setRotation(0, 0, 90);
            }
            case RIGHT -> {
                strand.setOffset(3f, 5 + rowYOffset, -gridX);
                strand.setRotation(0, 0, -90);
            }
            case TOP -> {
                strand.setOffset(gridX, 6f, gridZ);
                strand.setRotation(0, 0, 0);
            }
        }
    }

    public HairStrand[] getStrands(HairFace face) {
        return strandsByFace.get(face);
    }

    public HairStrand getStrand(HairFace face, int index) {
        HairStrand[] strands = strandsByFace.get(face);
        if (strands != null && index >= 0 && index < strands.length) {
            return strands[index];
        }
        return null;
    }

    public HairStrand getStrand(HairFace face, int row, int col) {
        int index = row * face.cols + col;
        return getStrand(face, index);
    }

    public int getVisibleStrandCount() {
        int count = 0;
        for (HairStrand[] strands : strandsByFace.values()) {
            for (HairStrand strand : strands) {
                if (strand.isVisible()) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getTotalCubeCount() {
        int count = 0;
        for (HairStrand[] strands : strandsByFace.values()) {
            for (HairStrand strand : strands) {
                count += strand.getLength();
            }
        }
        return count;
    }

    public boolean isEmpty() {
        return getVisibleStrandCount() == 0;
    }

    public String getGlobalColor() { return globalColor; }

    public void setGlobalColor(String color) {
        this.globalColor = color;
    }

    public String getEffectiveColor(HairStrand strand) {
        if (strand.hasCustomColor()) {
            return strand.getColor();
        }
        return globalColor;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public void clear() {
        for (HairFace face : HairFace.values()) {
            HairStrand[] strands = strandsByFace.get(face);
            for (int i = 0; i < strands.length; i++) {
                int id = strands[i].getId();
                strands[i] = new HairStrand(id);
                initializeStrandPosition(strands[i], face, i);
            }
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", version);
        tag.putString("Name", name);
        tag.putString("GlobalColor", globalColor);

        for (HairFace face : HairFace.values()) {
            ListTag strandsList = new ListTag();
            for (HairStrand strand : strandsByFace.get(face)) {
                strandsList.add(strand.save());
            }
            tag.put(face.name(), strandsList);
        }

        return tag;
    }

    public void load(CompoundTag tag) {
        this.version = tag.getInt("Version");
        this.name = tag.getString("Name");
        this.globalColor = tag.getString("GlobalColor");

        if (globalColor == null || globalColor.isEmpty()) {
            globalColor = "#000000";
        }

        for (HairFace face : HairFace.values()) {
            if (tag.contains(face.name())) {
                ListTag strandsList = tag.getList(face.name(), Tag.TAG_COMPOUND);
                HairStrand[] strands = strandsByFace.get(face);

                for (int i = 0; i < Math.min(strandsList.size(), strands.length); i++) {
                    strands[i].load(strandsList.getCompound(i));
                }
            }
        }
    }

    public CustomHair copy() {
        CustomHair copy = new CustomHair();
        copy.version = this.version;
        copy.name = this.name;
        copy.globalColor = this.globalColor;

        for (HairFace face : HairFace.values()) {
            HairStrand[] sourceStrands = this.strandsByFace.get(face);
            HairStrand[] destStrands = copy.strandsByFace.get(face);
            for (int i = 0; i < sourceStrands.length; i++) {
                destStrands[i] = sourceStrands[i].copy();
            }
        }

        return copy;
    }

    public void writeToBuffer(net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeNbt(this.save());
    }

    public static CustomHair readFromBuffer(net.minecraft.network.FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        if (tag == null) {
            return new CustomHair();
        }
        CustomHair hair = new CustomHair();
        hair.load(tag);
        return hair;
    }
}
