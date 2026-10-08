package com.dragonminez.server.world.structure.fitted;

import com.dragonminez.mixin.common.StructureTemplateAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class TemplateShape {
	private static final Map<StructureTemplate, TemplateShape> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

	private final int sizeX;
	private final int sizeZ;
	private final short[] lowest;
	private final short[] runTop;
	private final BlockState[] base;
	private final int autoGroundLevel;
	private final Map<Long, SkirtField> fields = new ConcurrentHashMap<>();

	private TemplateShape(int sizeX, int sizeZ, short[] lowest, short[] runTop, BlockState[] base, int autoGroundLevel) {
		this.sizeX = sizeX;
		this.sizeZ = sizeZ;
		this.lowest = lowest;
		this.runTop = runTop;
		this.base = base;
		this.autoGroundLevel = autoGroundLevel;
	}

	public static TemplateShape of(StructureTemplate template) {
		TemplateShape shape = CACHE.get(template);
		if (shape != null) return shape;
		shape = analyze(template);
		CACHE.put(template, shape);
		return shape;
	}

	private static TemplateShape analyze(StructureTemplate template) {
		Vec3i size = template.getSize();
		int sx = Math.max(0, size.getX());
		int sy = Math.max(0, size.getY());
		int sz = Math.max(0, size.getZ());
		int columns = sx * sz;
		short[] lowest = new short[columns];
		Arrays.fill(lowest, (short) -1);
		BlockState[] base = new BlockState[columns];
		BitSet solid = new BitSet(Math.max(1, columns * sy));

		List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) template).getPalettes();
		if (!palettes.isEmpty()) {
			for (StructureTemplate.StructureBlockInfo info : palettes.get(0).blocks()) {
				BlockState state = info.state();
				if (state.isAir() || state.is(Blocks.STRUCTURE_VOID)) continue;
				BlockPos pos = info.pos();
				if (pos.getX() < 0 || pos.getX() >= sx || pos.getY() < 0 || pos.getY() >= sy || pos.getZ() < 0 || pos.getZ() >= sz) continue;
				int column = pos.getX() + pos.getZ() * sx;
				if (lowest[column] < 0 || pos.getY() < lowest[column]) {
					lowest[column] = (short) pos.getY();
					base[column] = state;
				}
				if (state.blocksMotion()) solid.set(column * sy + pos.getY());
			}
		}

		short[] runTop = new short[columns];
		int[] counts = new int[sy + 1];
		int grounded = 0;
		for (int column = 0; column < columns; column++) {
			int y = lowest[column];
			if (y < 0) {
				runTop[column] = -1;
				continue;
			}
			while (y < sy && solid.get(column * sy + y)) y++;
			runTop[column] = (short) (y - 1);
			if (lowest[column] == 0 && runTop[column] >= 0) {
				counts[runTop[column]]++;
				grounded++;
			}
		}

		int auto = 0;
		if (grounded > 0) {
			int best = 0;
			for (int y = 1; y < counts.length; y++) {
				if (counts[y] > counts[best]) best = y;
			}
			auto = best > sy / 2 ? 0 : best;
		}
		return new TemplateShape(sx, sz, lowest, runTop, base, auto);
	}

	public int sizeX() {
		return this.sizeX;
	}

	public int sizeZ() {
		return this.sizeZ;
	}

	public int autoGroundLevel() {
		return this.autoGroundLevel;
	}

	private int index(int x, int z) {
		if (x < 0 || z < 0 || x >= this.sizeX || z >= this.sizeZ) return -1;
		return x + z * this.sizeX;
	}

	public boolean isGrounded(int x, int z, int groundLevel) {
		int index = this.index(x, z);
		return index >= 0 && isGrounded(index, groundLevel);
	}

	private boolean isGrounded(int column, int groundLevel) {
		int low = this.lowest[column];
		return low >= 0 && low <= groundLevel + 1 && this.runTop[column] >= low;
	}

	public int groundedCount(int groundLevel) {
		int count = 0;
		for (int column = 0; column < this.lowest.length; column++) {
			if (isGrounded(column, groundLevel)) count++;
		}
		return count;
	}

	public boolean isExteriorAir(BlockPos local) {
		int index = this.index(local.getX(), local.getZ());
		if (index < 0) return true;
		int low = this.lowest[index];
		return low < 0 || local.getY() < low;
	}

	BlockState baseState(int column) {
		return column >= 0 && column < this.base.length ? this.base[column] : null;
	}

	public SkirtField field(int groundLevel, int skirt) {
		long key = ((long) groundLevel << 32) | (skirt & 0xFFFFFFFFL);
		return this.fields.computeIfAbsent(key, k -> new SkirtField(this, groundLevel, Math.max(0, skirt)));
	}

	public static final class SkirtField {
		private static final int ENCLOSURE_REACH = 6;

		private final int margin;
		private final int width;
		private final int depth;
		private final float[] distance;
		private final int[] source;
		private final short[] support;
		private final boolean[] fills;

		private SkirtField(TemplateShape shape, int groundLevel, int margin) {
			this.margin = margin;
			this.width = shape.sizeX + margin * 2;
			this.depth = shape.sizeZ + margin * 2;
			int cells = this.width * this.depth;
			int columns = shape.sizeX * shape.sizeZ;
			this.support = new short[columns];
			this.fills = new boolean[columns];
			int[] source = new int[cells];
			Arrays.fill(source, -1);
			int[] vx = new int[cells];
			int[] vz = new int[cells];

			boolean[] grounded = new boolean[columns];
			for (int column = 0; column < columns; column++) grounded[column] = shape.isGrounded(column, groundLevel);
			for (int z = 0; z < shape.sizeZ; z++) {
				for (int x = 0; x < shape.sizeX; x++) {
					int column = x + z * shape.sizeX;
					int low = shape.lowest[column];
					if (low < 0) continue;
					if (grounded[column]) {
						this.support[column] = (short) Math.min(shape.runTop[column], groundLevel);
						this.fills[column] = true;
					} else if (enclosed(shape, grounded, x, z)) {
						this.support[column] = (short) Math.min(groundLevel, low - 1);
						this.fills[column] = true;
					} else {
						this.support[column] = (short) (low - 1);
						this.fills[column] = false;
					}
					source[(x + margin) + (z + margin) * this.width] = column;
				}
			}

			for (int z = 0; z < this.depth; z++) {
				for (int x = 0; x < this.width; x++) {
					relax(source, vx, vz, x, z, -1, 0);
					relax(source, vx, vz, x, z, 0, -1);
					relax(source, vx, vz, x, z, -1, -1);
					relax(source, vx, vz, x, z, 1, -1);
				}
				for (int x = this.width - 1; x >= 0; x--) relax(source, vx, vz, x, z, 1, 0);
			}
			for (int z = this.depth - 1; z >= 0; z--) {
				for (int x = this.width - 1; x >= 0; x--) {
					relax(source, vx, vz, x, z, 1, 0);
					relax(source, vx, vz, x, z, 0, 1);
					relax(source, vx, vz, x, z, -1, 1);
					relax(source, vx, vz, x, z, 1, 1);
				}
				for (int x = 0; x < this.width; x++) relax(source, vx, vz, x, z, -1, 0);
			}

			this.source = source;
			this.distance = new float[cells];
			for (int i = 0; i < cells; i++) {
				if (source[i] < 0) {
					this.distance[i] = -1.0F;
					continue;
				}
				float d = (float) Math.sqrt((double) vx[i] * vx[i] + (double) vz[i] * vz[i]);
				this.distance[i] = d > margin ? -1.0F : d;
			}
		}

		private static boolean enclosed(TemplateShape shape, boolean[] grounded, int x, int z) {
			return groundedAlong(shape, grounded, x, z, 1, 0) && groundedAlong(shape, grounded, x, z, -1, 0)
					&& groundedAlong(shape, grounded, x, z, 0, 1) && groundedAlong(shape, grounded, x, z, 0, -1);
		}

		private static boolean groundedAlong(TemplateShape shape, boolean[] grounded, int x, int z, int dx, int dz) {
			for (int step = 1; step <= ENCLOSURE_REACH; step++) {
				int index = shape.index(x + dx * step, z + dz * step);
				if (index < 0) return false;
				if (grounded[index]) return true;
			}
			return false;
		}

		private void relax(int[] source, int[] vx, int[] vz, int x, int z, int ox, int oz) {
			int nx = x + ox;
			int nz = z + oz;
			if (nx < 0 || nz < 0 || nx >= this.width || nz >= this.depth) return;
			int neighbour = nx + nz * this.width;
			if (source[neighbour] < 0) return;
			int cx = vx[neighbour] + ox;
			int cz = vz[neighbour] + oz;
			int cell = x + z * this.width;
			if (source[cell] < 0 || cx * cx + cz * cz < vx[cell] * vx[cell] + vz[cell] * vz[cell]) {
				source[cell] = source[neighbour];
				vx[cell] = cx;
				vz[cell] = cz;
			}
		}

		public int cell(int localX, int localZ) {
			int gx = localX + this.margin;
			int gz = localZ + this.margin;
			if (gx < 0 || gz < 0 || gx >= this.width || gz >= this.depth) return -1;
			int cell = gx + gz * this.width;
			return this.distance[cell] < 0.0F ? -1 : cell;
		}

		public float distance(int cell) {
			return this.distance[cell];
		}

		public int source(int cell) {
			return this.source[cell];
		}

		public int support(int column) {
			return this.support[column];
		}

		public boolean fills(int column) {
			return this.fills[column];
		}
	}
}
