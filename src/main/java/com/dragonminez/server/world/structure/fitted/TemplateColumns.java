package com.dragonminez.server.world.structure.fitted;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

final class TemplateColumns {
	private static final Map<StructureTemplate.Palette, TemplateColumns> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

	private final List<StructureTemplate.StructureBlockInfo> blocks;
	private final int sizeX;
	private final int sizeZ;
	private final int[] starts;
	private final int[] indices;

	private TemplateColumns(List<StructureTemplate.StructureBlockInfo> blocks, Vec3i size) {
		this.blocks = blocks;
		this.sizeX = Math.max(1, size.getX());
		this.sizeZ = Math.max(1, size.getZ());
		int columns = this.sizeX * this.sizeZ;
		int[] cursor = new int[columns + 1];
		for (StructureTemplate.StructureBlockInfo info : blocks) cursor[this.column(info.pos()) + 1]++;
		for (int c = 0; c < columns; c++) cursor[c + 1] += cursor[c];
		this.starts = cursor.clone();
		this.indices = new int[blocks.size()];
		for (int i = 0; i < blocks.size(); i++) this.indices[cursor[this.column(blocks.get(i).pos())]++] = i;
	}

	static TemplateColumns of(StructureTemplate.Palette palette, Vec3i size) {
		return CACHE.computeIfAbsent(palette, key -> new TemplateColumns(key.blocks(), size));
	}

	List<StructureTemplate.StructureBlockInfo> within(int minX, int minZ, int maxX, int maxZ) {
		int x0 = Math.max(0, minX);
		int x1 = Math.min(this.sizeX - 1, maxX);
		int z0 = Math.max(0, minZ);
		int z1 = Math.min(this.sizeZ - 1, maxZ);
		if (x0 > x1 || z0 > z1) return List.of();

		int total = 0;
		for (int z = z0; z <= z1; z++) {
			int row = z * this.sizeX;
			total += this.starts[row + x1 + 1] - this.starts[row + x0];
		}
		if (total == 0) return List.of();

		int[] picked = new int[total];
		int n = 0;
		for (int z = z0; z <= z1; z++) {
			int row = z * this.sizeX;
			int from = this.starts[row + x0];
			int to = this.starts[row + x1 + 1];
			System.arraycopy(this.indices, from, picked, n, to - from);
			n += to - from;
		}
		Arrays.sort(picked);

		List<StructureTemplate.StructureBlockInfo> out = new ArrayList<>(total);
		for (int index : picked) out.add(this.blocks.get(index));
		return out;
	}

	private int column(BlockPos pos) {
		return Mth.clamp(pos.getX(), 0, this.sizeX - 1) + Mth.clamp(pos.getZ(), 0, this.sizeZ - 1) * this.sizeX;
	}
}
