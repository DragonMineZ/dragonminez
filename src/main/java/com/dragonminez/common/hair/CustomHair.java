package com.dragonminez.common.hair;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class CustomHair {
	public static final int VERSION = 6;
	public static final int FRONT_STRANDS = 4;
	public static final int SIDE_STRANDS = 16;

	private static final AtomicLong REVISIONS = new AtomicLong();
	private static final Map<HairFace, Vector3f[]> BASE_POSITIONS = new EnumMap<>(HairFace.class);

	public enum HairFace {
		FRONT("F", FRONT_STRANDS, 1, 4),
		BACK("B", SIDE_STRANDS, 4, 4),
		LEFT("L", SIDE_STRANDS, 4, 4),
		RIGHT("R", SIDE_STRANDS, 4, 4),
		TOP("T", SIDE_STRANDS, 4, 4);

		public final String key;
		public final int maxStrands;
		public final int rows;
		public final int cols;

		HairFace(String key, int maxStrands, int rows, int cols) {
			this.key = key;
			this.maxStrands = maxStrands;
			this.rows = rows;
			this.cols = cols;
		}

		public int strandId(int index) {
			return ordinal() * 100 + index;
		}
	}

	static {
		for (HairFace face : HairFace.values()) {
			Vector3f[] positions = new Vector3f[face.maxStrands];
			for (int i = 0; i < face.maxStrands; i++) positions[i] = computeStrandBasePosition(face, i);
			BASE_POSITIONS.put(face, positions);
		}
	}

	private final Map<HairFace, HairStrand[]> strandsByFace = new EnumMap<>(HairFace.class);
	private String globalColor = "#000000";
	private String name = "Custom";
	private long revision = REVISIONS.incrementAndGet();

	public CustomHair() {
		for (HairFace face : HairFace.values()) {
			HairStrand[] strands = new HairStrand[face.maxStrands];
			for (int i = 0; i < face.maxStrands; i++) strands[i] = createDefaultStrand(face, i);
			strandsByFace.put(face, strands);
		}
	}

	public static HairStrand createDefaultStrand(HairFace face, int index) {
		HairStrand strand = new HairStrand(face.strandId(index));
		Vector3f rotation = getBaseRotation(face);
		strand.setRotation(rotation.x, rotation.y, rotation.z);
		return strand;
	}

	public static Vector3f getStrandBasePosition(HairFace face, int index) {
		Vector3f[] cached = BASE_POSITIONS.get(face);
		if (cached != null && index >= 0 && index < cached.length) return cached[index];
		return new Vector3f();
	}

	private static Vector3f computeStrandBasePosition(HairFace face, int index) {
		int row = index / face.cols;
		int col = index % face.cols;
		float[] positions = {-3.0f, -1.0f, 1.0f, 3.0f};
		float[] yOffsets = {0.0f, -1.5f, -3.0f, -4.5f};
		float gridX = positions[col % 4];
		float gridZ = positions[row % 4];
		float rowYOffset = yOffsets[row % 4];

		return switch (face) {
			case FRONT -> new Vector3f(gridX, 7.25f, -4.0f);
			case BACK -> new Vector3f(gridX, 7.25f + rowYOffset, 4.0f);
			case LEFT -> new Vector3f(-3.95f, 7.25f + rowYOffset, gridX);
			case RIGHT -> new Vector3f(3.95f, 7.25f + rowYOffset, -gridX);
			case TOP -> new Vector3f(gridX, 7.85f, gridZ);
		};
	}

	public static Vector3f getBaseRotation(HairFace face) {
		return switch (face) {
			case FRONT -> new Vector3f(-90.0f, 0.0f, 0.0f);
			case BACK -> new Vector3f(90.0f, 0.0f, 0.0f);
			case LEFT -> new Vector3f(0.0f, 0.0f, 90.0f);
			case RIGHT -> new Vector3f(0.0f, 0.0f, -90.0f);
			case TOP -> new Vector3f(0.0f, 0.0f, 0.0f);
		};
	}

	public static HairFace mirrorFace(HairFace face) {
		return switch (face) {
			case LEFT -> HairFace.RIGHT;
			case RIGHT -> HairFace.LEFT;
			default -> face;
		};
	}

	public static int mirrorIndex(HairFace face, int index) {
		int row = index / face.cols;
		int col = index % face.cols;
		return row * face.cols + (face.cols - 1 - col);
	}

	public HairStrand[] getStrands(HairFace face) {
		return strandsByFace.get(face);
	}

	public HairStrand getStrand(HairFace face, int index) {
		HairStrand[] strands = strandsByFace.get(face);
		if (strands != null && index >= 0 && index < strands.length) return strands[index];
		return null;
	}

	public void setStrand(HairFace face, int index, HairStrand strand) {
		HairStrand[] strands = strandsByFace.get(face);
		if (strands == null || index < 0 || index >= strands.length) return;
		HairStrand copy = strand.copy();
		copy.setId(face.strandId(index));
		strands[index] = copy;
		markChanged();
	}

	public HairStrand getMirrorStrand(HairFace face, int index) {
		HairFace targetFace = mirrorFace(face);
		int targetIndex = mirrorIndex(face, index);
		if (targetFace == face && targetIndex == index) return null;
		return getStrand(targetFace, targetIndex);
	}

	public int getVisibleStrandCount() {
		int count = 0;
		for (HairStrand[] strands : strandsByFace.values()) {
			for (HairStrand strand : strands) {
				if (strand.isVisible()) count++;
			}
		}
		return count;
	}

	public int getTotalSegmentCount() {
		int count = 0;
		for (HairStrand[] strands : strandsByFace.values()) {
			for (HairStrand strand : strands) {
				if (strand.isVisible()) count += strand.getSegments();
			}
		}
		return count;
	}

	public boolean isEmpty() {
		return getVisibleStrandCount() == 0;
	}

	public String getGlobalColor() {
		return globalColor;
	}

	public void setGlobalColor(String color) {
		String normalized = HairColors.normalize(color);
		this.globalColor = normalized != null ? normalized : "#000000";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name != null ? name : "Custom";
	}

	public long getRevision() {
		return revision;
	}

	public void markChanged() {
		revision = REVISIONS.incrementAndGet();
	}

	public void clear() {
		for (HairFace face : HairFace.values()) {
			HairStrand[] strands = strandsByFace.get(face);
			for (int i = 0; i < strands.length; i++) strands[i] = createDefaultStrand(face, i);
		}
		markChanged();
	}

	public void copyFrom(CustomHair other) {
		this.name = other.name;
		this.globalColor = other.globalColor;
		for (HairFace face : HairFace.values()) {
			HairStrand[] source = other.strandsByFace.get(face);
			HairStrand[] target = strandsByFace.get(face);
			for (int i = 0; i < target.length; i++) target[i] = source[i].copy();
		}
		markChanged();
	}

	public CustomHair copy() {
		CustomHair copy = new CustomHair();
		copy.copyFrom(this);
		return copy;
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("v", VERSION);
		if (name != null && !name.isEmpty()) tag.putString("n", name);
		tag.putString("gc", globalColor);
		for (HairFace face : HairFace.values()) {
			ListTag strandList = new ListTag();
			for (HairStrand strand : strandsByFace.get(face)) {
				if (strand.isVisible()) strandList.add(strand.save());
			}
			if (!strandList.isEmpty()) tag.put(face.key, strandList);
		}
		return tag;
	}

	public void load(CompoundTag tag) {
		int version = tag.contains("v") ? tag.getInt("v") : (tag.contains("Version") ? tag.getInt("Version") : 1);
		this.name = tag.contains("n") ? tag.getString("n") : (tag.contains("Name") ? tag.getString("Name") : "Custom");
		setGlobalColor(tag.contains("gc") ? tag.getString("gc") : tag.getString("GlobalColor"));

		for (HairFace face : HairFace.values()) {
			HairStrand[] strands = strandsByFace.get(face);
			for (int i = 0; i < strands.length; i++) strands[i] = createDefaultStrand(face, i);

			String key = tag.contains(face.key) ? face.key : (tag.contains(face.name()) ? face.name() : null);
			if (key == null) continue;

			ListTag strandList = tag.getList(key, Tag.TAG_COMPOUND);
			for (int i = 0; i < strandList.size(); i++) {
				CompoundTag strandTag = strandList.getCompound(i);
				int index = resolveStrandIndex(face, strandTag, i, version);
				if (index < 0 || index >= strands.length) continue;

				HairStrand strand;
				if (version >= VERSION) {
					strand = new HairStrand();
					strand.load(strandTag);
				} else {
					strand = HairLegacyMigrator.migrateStrand(face, index, strandTag);
				}
				strand.setId(face.strandId(index));
				strands[index] = strand;
			}
		}
		markChanged();
	}

	private static int resolveStrandIndex(HairFace face, CompoundTag strandTag, int listIndex, int version) {
		if (version < 2) return listIndex;
		int id = strandTag.contains("i") ? strandTag.getInt("i") : strandTag.getInt("Id");
		int index = id - face.ordinal() * 100;
		return index >= 0 && index < face.maxStrands ? index : listIndex;
	}

	public static CustomHair fromTag(CompoundTag tag) {
		CustomHair hair = new CustomHair();
		if (tag != null) hair.load(tag);
		return hair;
	}

	public void writeToBuffer(FriendlyByteBuf buf) {
		buf.writeNbt(save());
	}

	public static CustomHair readFromBuffer(FriendlyByteBuf buf) {
		return fromTag(buf.readNbt());
	}
}
