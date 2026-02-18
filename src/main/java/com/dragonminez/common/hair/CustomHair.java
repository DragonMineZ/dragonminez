package com.dragonminez.common.hair;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

public class CustomHair {
	private static final int VERSION = 5;
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
    private int version = VERSION;

    public CustomHair() {
        initializeStrands();
    }


    private void initializeStrands() {
        int idCounter = 0;
        for (HairFace face : HairFace.values()) {
            HairStrand[] strands = new HairStrand[face.maxStrands];
			for (int i = 0; i < face.maxStrands; i++) {
				int staticId = (face.ordinal() * 100) + i;
				strands[i] = new HairStrand(staticId);
				Vector3f rot = getBaseRotation(face);
				strands[i].setRotation(rot.x, rot.y, rot.z);
			}
            strandsByFace.put(face, strands);
        }
    }

	public static Vector3f getStrandBasePosition(HairFace face, int index) {
		int row = index / face.cols;
		int col = index % face.cols;

		float[] positions = {-3f, -1f, 1f, 3f};
		float[] yOffsets = {0f, -1.5f, -3f, -4.5f};

		float gridX = positions[col % 4];
		float gridZ = positions[row % 4];
		float rowYOffset = yOffsets[row % 4];

		switch (face) {
			case FRONT:
				return new Vector3f(gridX, 7.25f, -4.0f);
			case BACK:
				return new Vector3f(gridX, 7.25f + rowYOffset, 4.0f);
			case LEFT:
				return new Vector3f(-3.95f, 7.25f + rowYOffset, gridX);
			case RIGHT:
				return new Vector3f(3.95f, 7.25f + rowYOffset, -gridX);
			case TOP:
				return new Vector3f(gridX, 7.85f, gridZ);
			default:
				return new Vector3f(0, 0, 0);
		}
	}

	public static Vector3f getBaseRotation(HairFace face) {
		switch (face) {
			case FRONT: return new Vector3f(-90, 0, 0);
			case BACK: return new Vector3f(90, 0, 0);
			case LEFT: return new Vector3f(0, 0, 90);
			case RIGHT: return new Vector3f(0, 0, -90);
			case TOP: return new Vector3f(0, 0, 0);
			default: return new Vector3f(0,0,0);
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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

	public void clear() {
		for (HairFace face : HairFace.values()) {
			HairStrand[] strands = strandsByFace.get(face);
			for (int i = 0; i < strands.length; i++) {
				int staticId = (face.ordinal() * 100) + i;
				strands[i] = new HairStrand(staticId);
				Vector3f rot = getBaseRotation(face);
				strands[i].setRotation(rot.x, rot.y, rot.z);
			}
		}
	}

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("v", VERSION);
        if (name != null && !name.isEmpty()) tag.putString("n", name);
        tag.putString("gc", globalColor);
        String[] faceKeys = {"F", "B", "L", "R", "T"};
        int faceIndex = 0;
        for (HairFace face : HairFace.values()) {
            ListTag strandsList = new ListTag();
            for (HairStrand strand : strandsByFace.get(face)) {
                if (strand.isVisible()) {
                    strandsList.add(strand.save());
                }
            }
            if (!strandsList.isEmpty()) {
                tag.put(faceKeys[faceIndex], strandsList);
            }
            faceIndex++;
        }

        return tag;
    }

    public void load(CompoundTag tag) {
		int loadedVersion = tag.contains("v") ? tag.getInt("v") : (tag.contains("Version") ? tag.getInt("Version") : 1);
		this.version = loadedVersion;
        this.name = tag.contains("n") ? tag.getString("n") : tag.getString("Name");
        this.globalColor = tag.contains("gc") ? tag.getString("gc") : tag.getString("GlobalColor");
        if (globalColor == null || globalColor.isEmpty()) globalColor = "#000000";

        String[] shortKeys = {"F", "B", "L", "R", "T"};
        HairFace[] faces = HairFace.values();

		for (int f = 0; f < faces.length; f++) {
            HairFace face = faces[f];
            String shortKey = shortKeys[f];
            String longKey = face.name();
            String keyToUse = tag.contains(shortKey) ? shortKey : (tag.contains(longKey) ? longKey : null);

			if (keyToUse != null) {
				ListTag strandsList = tag.getList(keyToUse, Tag.TAG_COMPOUND);
				HairStrand[] strands = strandsByFace.get(face);

				for (int i = 0; i < strandsList.size(); i++) {
					CompoundTag strandTag = strandsList.getCompound(i);
					int idInTag = strandTag.contains("i") ? strandTag.getInt("i") : strandTag.getInt("Id");

					int targetIndex = i;
					if (loadedVersion >= 2) {
						int calculatedIndex = idInTag - (face.ordinal() * 100);
						if (calculatedIndex >= 0 && calculatedIndex < strands.length) {
							targetIndex = calculatedIndex;
						}
					}

					if (targetIndex < strands.length) {
						strands[targetIndex].load(strandTag);

						int staticId = (face.ordinal() * 100) + targetIndex;
						strands[targetIndex].setId(staticId);
					}
				}
			}
		}

		this.version = VERSION;
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

    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeNbt(this.save());
    }

    public static CustomHair readFromBuffer(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        if (tag == null) {
            return new CustomHair();
        }
        CustomHair hair = new CustomHair();
        hair.load(tag);
        return hair;
    }
}
