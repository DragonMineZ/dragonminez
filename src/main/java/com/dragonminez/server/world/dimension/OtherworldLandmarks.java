package com.dragonminez.server.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public final class OtherworldLandmarks {

	private static final int MARGIN = 2;

	private static final List<BoundingBox> BOXES = List.of(
			box(-61, 148, -49, 61, 178, 82),
			box(-11, 160, 978, 129, 324, 1118),
			box(-10, 149, 70, 10, 161, 127),
			box(-34, 126, 128, 4, 164, 191),
			box(-34, 122, 192, -26, 146, 255),
			box(-64, 137, 256, -26, 164, 319),
			box(-64, 153, 320, -56, 164, 383),
			box(-64, 153, 384, -56, 167, 447),
			box(-64, 158, 448, -21, 195, 511),
			box(-34, 187, 512, 4, 201, 575),
			box(-34, 193, 576, -26, 205, 639),
			box(-34, 156, 640, -26, 207, 703),
			box(-34, 134, 704, -26, 165, 767),
			box(-34, 134, 768, -26, 173, 831),
			box(-34, 162, 832, -26, 173, 895),
			box(-34, 159, 896, -9, 170, 959),
			box(-17, 161, 960, 13, 178, 996)
	);

	private OtherworldLandmarks() {
	}

	private static BoundingBox box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return new BoundingBox(minX - MARGIN, minY - MARGIN, minZ - MARGIN, maxX + MARGIN, maxY + MARGIN, maxZ + MARGIN);
	}

	private static boolean isOtherworld(Level level) {
		return level != null && level.dimension().equals(OtherworldDimension.OTHERWORLD_KEY);
	}

	public static boolean isProtected(Level level, BlockPos pos) {
		if (!isOtherworld(level)) return false;
		for (BoundingBox box : BOXES) {
			if (box.isInside(pos)) return true;
		}
		return false;
	}

	public static List<BoundingBox> intersecting(Level level, BoundingBox area) {
		if (!isOtherworld(level)) return List.of();
		List<BoundingBox> result = new ArrayList<>();
		for (BoundingBox box : BOXES) {
			if (box.intersects(area)) result.add(box);
		}
		return result;
	}
}
