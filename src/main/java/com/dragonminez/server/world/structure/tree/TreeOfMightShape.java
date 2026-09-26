package com.dragonminez.server.world.structure.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class TreeOfMightShape {
	public static final int VERSION = 1;

	public static final byte NONE = 0;
	public static final byte FOLIAGE = 1;
	public static final byte ROOT = 2;
	public static final byte WOOD = 3;
	public static final byte FRUIT = 4;
	public static final byte LICHEN = 5;

	public static final int ROOT_THIN = 0;
	public static final int ROOT_SINGLE = 1;
	public static final int ROOT_PLUS = 2;
	public static final int ROOT_THICK = 3;

	public static final int[] FACE_DX = {0, 0, 0, 0, -1, 1};
	public static final int[] FACE_DY = {-1, 1, 0, 0, 0, 0};
	public static final int[] FACE_DZ = {0, 0, -1, 1, 0, 0};

	public static final int ARENA_RADIUS = 26;
	public static final int ARENA_BLEND = 6;
	public static final int ARENA_HEADROOM = 40;
	public static final int BASE_ZONE = 12;
	public static final int MAX_BASE_EXTENSION = 48;

	private static final int SHELL = 6;
	private static final double RIM_AMPLITUDE = 0.10D;
	private static final int ROOT_SPACING = 6;
	private static final int ROOT_ARENA_CLEARANCE = 24;
	private static final int LOBE_TABLE = 1024;
	private static final double LICHEN_THRESHOLD = 0.28D;
	private static final int LICHEN_DENSITY = 90;

	public interface Surface {
		int at(int x, int z);
	}

	public interface Obstacle {
		boolean blocks(int x, int y, int z);
	}

	public record HangingRoot(int x, int z, int topY, int bottomY, boolean reachesGround, int kind, boolean fruit) {
		public int[][] footprint() {
			return switch (kind) {
				case ROOT_PLUS -> new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
				case ROOT_THICK -> new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
				default -> new int[][]{{0, 0}};
			};
		}

		public byte code() {
			return kind == ROOT_THIN ? ROOT : WOOD;
		}

		HangingRoot withFruit() {
			return new HangingRoot(x, z, topY, bottomY, reachesGround, kind, true);
		}
	}

	private record Capsule(double ax, double ay, double az, double bx, double by, double bz, double ra, double rb,
						   int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		static Capsule of(double ax, double ay, double az, double ra, double bx, double by, double bz, double rb) {
			double r = Math.max(ra, rb);
			return new Capsule(ax, ay, az, bx, by, bz, ra, rb,
					(int) Math.floor(Math.min(ax, bx) - r), (int) Math.floor(Math.min(ay, by) - r), (int) Math.floor(Math.min(az, bz) - r),
					(int) Math.ceil(Math.max(ax, bx) + r), (int) Math.ceil(Math.max(ay, by) + r), (int) Math.ceil(Math.max(az, bz) + r));
		}

		boolean intersects(int x0, int y0, int z0, int x1, int y1, int z1) {
			return maxX >= x0 && minX <= x1 && maxY >= y0 && minY <= y1 && maxZ >= z0 && minZ <= z1;
		}
	}

	private record PathPoint(double x, double y, double z, double r) {}

	private final int centerX;
	private final int centerZ;
	private final double axisOriginX;
	private final double axisOriginZ;
	private final long seed;
	private final int minBuildY;
	private final int maxBuildY;
	private final int seaLevel;
	private final Map<Long, Integer> samples;

	private final Perlin detailNoise;
	private final Perlin lumpNoise;
	private final Perlin bumpNoise;
	private final Perlin lichenNoise;

	private final int baseY;
	private final int trunkBottom;
	private final int trunkTop;
	private final int solidTop;
	private final double rTop;
	private final double rBottom;
	private final double flare;
	private final double flareHeight;
	private final double leanX;
	private final double leanZ;
	private final double wobbleX;
	private final double wobbleZ;
	private final double wobblePhaseX;
	private final double wobblePhaseZ;
	private final double twist;
	private final int[] ridgeFreq = {7, 11, 17};
	private final double[] ridgeAmp = {0.5, 0.32, 0.18};
	private final double[] ridgePhase = new double[3];
	private final double[] buttressAngles;
	private final double[] lobeTable = new double[LOBE_TABLE];

	private final double[] trunkAxisX;
	private final double[] trunkAxisZ;
	private final double[] trunkCore;
	private final double[] trunkFlare;
	private final double[] trunkShell;
	private final double[] trunkMaxR;

	private final int canopyBase;
	private final double canopyThickness;
	private final double canopyRadius;
	private final int[] rimFreq = {3, 4, 5, 6, 7, 9, 11, 13};
	private final double[] rimAmp = new double[rimFreq.length];
	private final double[] rimPhase = new double[rimFreq.length];

	private final double arenaAngle;
	private final int arenaX;
	private final int arenaZ;
	private final int arenaFloor;

	private final List<Capsule> baseCapsules = new ArrayList<>();
	private final List<Capsule> branchCapsules = new ArrayList<>();
	private final List<HangingRoot> roots = new ArrayList<>();

	private final int minX;
	private final int minY;
	private final int minZ;
	private final int maxX;
	private final int maxY;
	private final int maxZ;

	private TreeOfMightShape(int centerX, int centerZ, long seed, int minBuildY, int maxBuildY, int seaLevel, Surface surface) {
		this.centerX = centerX;
		this.centerZ = centerZ;
		this.axisOriginX = centerX + 0.5D;
		this.axisOriginZ = centerZ + 0.5D;
		this.seed = seed;
		this.minBuildY = minBuildY;
		this.maxBuildY = maxBuildY;
		this.seaLevel = seaLevel;
		this.samples = new LinkedHashMap<>();
		Surface sampler = (x, z) -> this.samples.computeIfAbsent(pack(x, z), key -> surface.at(x, z));

		Random random = new Random(seed * 0x9E3779B97F4A7C15L + VERSION);
		this.detailNoise = new Perlin(random.nextLong());
		this.lumpNoise = new Perlin(random.nextLong());
		this.bumpNoise = new Perlin(random.nextLong());
		this.lichenNoise = new Perlin(random.nextLong());

		int groundSum = sampler.at(centerX, centerZ);
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4.0D;
			groundSum += sampler.at(centerX + (int) StrictMath.round(StrictMath.cos(angle) * 18.0D),
					centerZ + (int) StrictMath.round(StrictMath.sin(angle) * 18.0D));
		}
		this.baseY = Math.floorDiv(groundSum, 9);

		int ceiling = maxBuildY - 8;
		this.canopyBase = clamp(this.baseY + 168, this.baseY + 110, ceiling - 76);
		this.canopyThickness = Math.max(24.0D, Math.min(60.0D, ceiling - 24 - this.canopyBase));
		this.canopyRadius = 165.0D + random.nextDouble() * 15.0D;

		this.trunkTop = this.canopyBase + 14;
		this.trunkBottom = this.baseY - 8;
		this.solidTop = this.baseY + 6;
		this.rTop = 20.0D + random.nextDouble() * 3.0D;
		this.rBottom = 27.0D + random.nextDouble() * 4.0D;
		this.flare = 27.0D + random.nextDouble() * 6.0D;
		this.flareHeight = 16.0D + random.nextDouble() * 6.0D;

		double leanAngle = random.nextDouble() * Math.PI * 2.0D;
		double lean = 3.0D + random.nextDouble() * 4.0D;
		this.leanX = StrictMath.cos(leanAngle) * lean;
		this.leanZ = StrictMath.sin(leanAngle) * lean;
		this.wobbleX = (random.nextDouble() - 0.5D) * 4.0D;
		this.wobbleZ = (random.nextDouble() - 0.5D) * 4.0D;
		this.wobblePhaseX = random.nextDouble() * Math.PI * 2.0D;
		this.wobblePhaseZ = random.nextDouble() * Math.PI * 2.0D;
		this.twist = (random.nextDouble() - 0.5D) * 1.2D;
		for (int i = 0; i < this.ridgePhase.length; i++) this.ridgePhase[i] = random.nextDouble() * Math.PI * 2.0D;

		double rimTotal = 0.0D;
		for (int i = 0; i < this.rimAmp.length; i++) {
			this.rimAmp[i] = (0.4D + random.nextDouble() * 0.6D) / StrictMath.pow(this.rimFreq[i], 0.6D);
			this.rimPhase[i] = random.nextDouble() * Math.PI * 2.0D;
			rimTotal += this.rimAmp[i];
		}
		for (int i = 0; i < this.rimAmp.length; i++) this.rimAmp[i] /= rimTotal;

		this.arenaAngle = random.nextDouble() * Math.PI * 2.0D;
		double arenaDistance = this.rBottom + this.flare * 0.55D + ARENA_RADIUS + 8.0D;
		this.arenaX = (int) Math.floor(this.axisOriginX + StrictMath.cos(this.arenaAngle) * arenaDistance);
		this.arenaZ = (int) Math.floor(this.axisOriginZ + StrictMath.sin(this.arenaAngle) * arenaDistance);
		this.arenaFloor = Math.max(sampler.at(this.arenaX, this.arenaZ) - 1, seaLevel - 1);

		double window = StrictMath.asin(Math.min(1.0D, (ARENA_RADIUS + 10.0D) / arenaDistance)) + 0.1D;
		int buttressCount = 9 + random.nextInt(3);
		this.buttressAngles = new double[buttressCount];
		double span = Math.PI * 2.0D - window * 2.0D;
		for (int i = 0; i < buttressCount; i++) {
			double slot = (i + 0.5D + (random.nextDouble() - 0.5D) * 0.5D) / buttressCount;
			this.buttressAngles[i] = this.arenaAngle + window + span * slot;
		}
		for (int i = 0; i < LOBE_TABLE; i++) {
			double theta = i * Math.PI * 2.0D / LOBE_TABLE;
			double best = 0.0D;
			for (double angle : this.buttressAngles) {
				double d = angleDiff(theta, angle);
				best = Math.max(best, StrictMath.exp(-(d * d) / (2.0D * 0.16D * 0.16D)));
			}
			this.lobeTable[i] = best;
		}

		int trunkLength = this.trunkTop - this.trunkBottom + 1;
		this.trunkAxisX = new double[trunkLength];
		this.trunkAxisZ = new double[trunkLength];
		this.trunkCore = new double[trunkLength];
		this.trunkFlare = new double[trunkLength];
		this.trunkShell = new double[trunkLength];
		this.trunkMaxR = new double[trunkLength];
		for (int i = 0; i < trunkLength; i++) {
			int y = this.trunkBottom + i;
			double t = trunkProgress(y);
			this.trunkAxisX[i] = this.axisOriginX + this.leanX * t * t
					+ this.wobbleX * (StrictMath.sin(Math.PI * 1.3D * t + this.wobblePhaseX) - StrictMath.sin(this.wobblePhaseX));
			this.trunkAxisZ[i] = this.axisOriginZ + this.leanZ * t * t
					+ this.wobbleZ * (StrictMath.sin(Math.PI * 1.3D * t + this.wobblePhaseZ) - StrictMath.sin(this.wobblePhaseZ));
			this.trunkCore[i] = coreRadius(y);
			this.trunkFlare[i] = flareRadius(y);
			double slope = Math.abs(flareRadius(y + 1) - flareRadius(y - 1)) * 0.5D
					+ Math.abs(coreRadius(y + 1) - coreRadius(y - 1)) * 0.5D;
			this.trunkShell[i] = 3.5D + slope * 1.6D;
			this.trunkMaxR[i] = this.trunkCore[i] * 1.1D + this.trunkFlare[i] + 1.0D;
		}

		buildButtressRoots(random, sampler);
		List<PathPoint> branchPoints = new ArrayList<>();
		buildLimbs(random, branchPoints);
		buildHangingRoots(random, branchPoints);
		pickFruits(random);

		int buttressReach = 0;
		for (Capsule capsule : this.baseCapsules) {
			buttressReach = Math.max(buttressReach, Math.max(Math.abs(capsule.minX() - centerX), Math.abs(capsule.maxX() - centerX)));
			buttressReach = Math.max(buttressReach, Math.max(Math.abs(capsule.minZ() - centerZ), Math.abs(capsule.maxZ() - centerZ)));
		}
		int horizontal = (int) Math.ceil(Math.max(this.canopyRadius * (1.0D + RIM_AMPLITUDE) + 7.0D, Math.max(buttressReach + 2, ARENA_RADIUS + ARENA_BLEND
				+ Math.max(Math.abs(this.arenaX - centerX), Math.abs(this.arenaZ - centerZ)) + 2)));
		this.minX = centerX - horizontal;
		this.maxX = centerX + horizontal;
		this.minZ = centerZ - horizontal;
		this.maxZ = centerZ + horizontal;
		this.minY = Math.max(minBuildY + 1, this.baseY - 64);
		this.maxY = Math.min(maxBuildY - 2, (int) Math.ceil(this.canopyBase + 10 + this.canopyThickness + 18));
	}

	public static TreeOfMightShape create(int centerX, int centerZ, long seed, int minBuildY, int maxBuildY, int seaLevel, Surface surface) {
		return new TreeOfMightShape(centerX, centerZ, seed, minBuildY, maxBuildY, seaLevel, surface);
	}

	public static long pack(int x, int z) {
		return ((long) x << 32) | (z & 0xFFFFFFFFL);
	}

	public Map<Long, Integer> groundSamples() {
		return Collections.unmodifiableMap(this.samples);
	}

	public long seed() {
		return this.seed;
	}

	public int centerX() {
		return this.centerX;
	}

	public int centerZ() {
		return this.centerZ;
	}

	public int baseY() {
		return this.baseY;
	}

	public int canopyBase() {
		return this.canopyBase;
	}

	public int arenaX() {
		return this.arenaX;
	}

	public int arenaZ() {
		return this.arenaZ;
	}

	public int arenaFloor() {
		return this.arenaFloor;
	}

	public double canopyRadius() {
		return this.canopyRadius;
	}

	public int seaLevel() {
		return this.seaLevel;
	}

	public int minBuildY() {
		return this.minBuildY;
	}

	public int maxBuildY() {
		return this.maxBuildY;
	}

	public int minX() {
		return this.minX;
	}

	public int minY() {
		return this.minY;
	}

	public int minZ() {
		return this.minZ;
	}

	public int maxX() {
		return this.maxX;
	}

	public int maxY() {
		return this.maxY;
	}

	public int maxZ() {
		return this.maxZ;
	}

	public List<HangingRoot> roots() {
		return Collections.unmodifiableList(this.roots);
	}

	public boolean intersectsColumns(int x0, int z0, int x1, int z1) {
		return x1 >= this.minX && x0 <= this.maxX && z1 >= this.minZ && z0 <= this.maxZ;
	}

	public boolean isUnderCanopy(int x, int z) {
		double ox = x + 0.5D - this.axisOriginX;
		double oz = z + 0.5D - this.axisOriginZ;
		return Math.sqrt(ox * ox + oz * oz) < rimRadius(Math.atan2(oz, ox)) * 0.95D;
	}

	public boolean isArenaColumn(int x, int z, int extra) {
		long dx = x - this.arenaX;
		long dz = z - this.arenaZ;
		long r = ARENA_RADIUS + extra;
		return dx * dx + dz * dz <= r * r;
	}

	public double arenaDistance(int x, int z) {
		double dx = x - this.arenaX;
		double dz = z - this.arenaZ;
		return Math.sqrt(dx * dx + dz * dz);
	}

	public void rasterize(int x0, int y0, int z0, int sx, int sy, int sz, byte[] out) {
		int x1 = x0 + sx - 1;
		int y1 = y0 + sy - 1;
		int z1 = z0 + sz - 1;
		if (x1 < this.minX || x0 > this.maxX || z1 < this.minZ || z0 > this.maxZ || y1 < this.minY || y0 > this.maxY) return;

		ColumnData columns = columnData(x0, z0, sx, sz);
		for (int dz = 0; dz < sz; dz++) {
			for (int dx = 0; dx < sx; dx++) {
				int column = dz * sx + dx;
				if (!columns.canopy[column]) continue;
				double px = x0 + dx + 0.5D;
				double pz = z0 + dz + 0.5D;
				int from = Math.max(y0, (int) Math.floor(columns.low[column]) - 7);
				int to = Math.min(y1, (int) Math.ceil(columns.high[column]) + 7);
				for (int y = from; y <= to; y++) {
					double density = density(px, y + 0.5D, pz, columns.low[column], columns.high[column], columns.radial[column]);
					if (density > 0.0D && density < SHELL) mark(out, sx, sz, dx, y - y0, dz, FOLIAGE);
				}
			}
		}

		rasterizeTrunk(x0, y0, z0, sx, sy, sz, out, columns);

		for (Capsule capsule : this.baseCapsules) {
			if (capsule.intersects(x0, y0, z0, x1, y1, z1)) rasterizeCapsule(capsule, x0, y0, z0, sx, sy, sz, out, null);
		}
		for (Capsule capsule : this.branchCapsules) {
			if (capsule.intersects(x0, y0, z0, x1, y1, z1)) rasterizeCapsule(capsule, x0, y0, z0, sx, sy, sz, out, columns);
		}
	}

	public void applyRoots(int x0, int y0, int z0, int sx, int sy, int sz, byte[] out, Obstacle obstacle) {
		int x1 = x0 + sx - 1;
		int z1 = z0 + sz - 1;
		for (HangingRoot root : this.roots) {
			if (root.x() + 1 < x0 || root.x() - 1 > x1 || root.z() + 1 < z0 || root.z() - 1 > z1) continue;
			byte code = root.code();
			boolean dripTip = !root.fruit() && !root.reachesGround() && root.kind() == ROOT_THIN;
			for (int[] offset : root.footprint()) {
				int x = root.x() + offset[0];
				int z = root.z() + offset[1];
				if (x < x0 || x > x1 || z < z0 || z > z1) continue;
				int bottom = root.reachesGround() ? y0 : Math.max(y0, root.bottomY());
				boolean hanging = false;
				int lowest = -1;
				for (int y = Math.min(root.topY(), y0 + sy - 1); y >= bottom; y--) {
					int index = index(sx, sz, x - x0, y - y0, z - z0);
					byte current = out[index];
					if (current == WOOD) {
						if (hanging) break;
						continue;
					}
					if (obstacle.blocks(x, y, z)) break;
					byte voxel = dripTip && y <= root.bottomY() + 1 ? WOOD : code;
					if (current < voxel) out[index] = voxel;
					hanging = true;
					lowest = index;
				}
				if (root.fruit() && lowest >= 0) out[lowest] = FRUIT;
			}
		}
	}

	public void applyLichen(int x0, int y0, int z0, int sx, int sy, int sz, byte[] out, byte[] faces) {
		int x1 = x0 + sx - 1;
		int z1 = z0 + sz - 1;
		if (x1 < this.minX || x0 > this.maxX || z1 < this.minZ || z0 > this.maxZ) return;
		ColumnData columns = columnData(x0, z0, sx, sz);
		for (int dy = 0; dy < sy; dy++) {
			int y = y0 + dy;
			for (int dz = 0; dz < sz; dz++) {
				for (int dx = 0; dx < sx; dx++) {
					int index = index(sx, sz, dx, dy, dz);
					if (out[index] != NONE) continue;
					int mask = 0;
					for (int face = 0; face < 6; face++) {
						int nx = dx + FACE_DX[face];
						int ny = dy + FACE_DY[face];
						int nz = dz + FACE_DZ[face];
						if (nx < 0 || ny < 0 || nz < 0 || nx >= sx || ny >= sy || nz >= sz) continue;
						if (out[index(sx, sz, nx, ny, nz)] == WOOD) mask |= 1 << face;
					}
					if (mask == 0) continue;
					int x = x0 + dx;
					int z = z0 + dz;
					if ((mix(x, y, z) & 127) >= LICHEN_DENSITY) continue;
					double px = x + 0.5D;
					double py = y + 0.5D;
					double pz = z + 0.5D;
					if (lichenNoise.noise(px / 7.0D, py / 7.0D, pz / 7.0D) < LICHEN_THRESHOLD) continue;
					int column = dz * sx + dx;
					if (columns.canopy[column]
							&& density(px, py, pz, columns.low[column], columns.high[column], columns.radial[column]) > -1.0D) continue;
					if (insideTrunkHollow(x, y, z)) continue;
					out[index] = LICHEN;
					faces[index] = (byte) mask;
				}
			}
		}
	}

	public int baseBottom(byte[] out, int sx, int sy, int sz, int dx, int dz, int y0) {
		int limit = Math.min(sy - 1, this.baseY + BASE_ZONE - y0);
		for (int dy = 0; dy <= limit; dy++) {
			if (out[index(sx, sz, dx, dy, dz)] == WOOD) return y0 + dy;
		}
		return Integer.MIN_VALUE;
	}

	public static int index(int sx, int sz, int dx, int dy, int dz) {
		return (dy * sz + dz) * sx + dx;
	}

	private static void mark(byte[] out, int sx, int sz, int dx, int dy, int dz, byte code) {
		int index = index(sx, sz, dx, dy, dz);
		if (out[index] < code) out[index] = code;
	}

	private static final class ColumnData {
		final double[] low;
		final double[] high;
		final double[] radial;
		final boolean[] canopy;

		ColumnData(int size) {
			this.low = new double[size];
			this.high = new double[size];
			this.radial = new double[size];
			this.canopy = new boolean[size];
		}
	}

	private ColumnData columnData(int x0, int z0, int sx, int sz) {
		ColumnData columns = new ColumnData(sx * sz);
		for (int dz = 0; dz < sz; dz++) {
			for (int dx = 0; dx < sx; dx++) {
				int column = dz * sx + dx;
				double px = x0 + dx + 0.5D;
				double pz = z0 + dz + 0.5D;
				double ox = px - this.axisOriginX;
				double oz = pz - this.axisOriginZ;
				double r = Math.sqrt(ox * ox + oz * oz);
				double rim = rimRadius(Math.atan2(oz, ox));
				double u = r / rim;
				columns.radial[column] = rim - r;
				if (u > 1.08D) continue;
				columns.canopy[column] = true;
				columns.low[column] = lowSurface(px, pz, u);
				columns.high[column] = highSurface(px, pz, u);
			}
		}
		return columns;
	}

	private void rasterizeTrunk(int x0, int y0, int z0, int sx, int sy, int sz, byte[] out, ColumnData columns) {
		int from = Math.max(y0, this.trunkBottom);
		int to = Math.min(y0 + sy - 1, this.trunkTop);
		for (int y = from; y <= to; y++) {
			int i = y - this.trunkBottom;
			double axisX = this.trunkAxisX[i];
			double axisZ = this.trunkAxisZ[i];
			double maxR = this.trunkMaxR[i];
			if (x0 + sx < axisX - maxR || x0 > axisX + maxR || z0 + sz < axisZ - maxR || z0 > axisZ + maxR) continue;
			double t = trunkProgress(y);
			for (int dz = 0; dz < sz; dz++) {
				double oz = z0 + dz + 0.5D - axisZ;
				for (int dx = 0; dx < sx; dx++) {
					double ox = x0 + dx + 0.5D - axisX;
					double r2 = ox * ox + oz * oz;
					if (r2 > maxR * maxR) continue;
					double r = Math.sqrt(r2);
					double radius = trunkRadius(i, Math.atan2(oz, ox), t);
					if (r > radius) continue;
					if (y > this.solidTop && r < radius - this.trunkShell[i]) continue;
					int column = dz * sx + dx;
					if (columns.canopy[column] && insideCanopyCore(x0 + dx + 0.5D, y + 0.5D, z0 + dz + 0.5D, columns, column)) continue;
					mark(out, sx, sz, dx, y - y0, dz, WOOD);
				}
			}
		}
	}

	private double trunkRadius(int i, double theta, double t) {
		double ridge = 0.0D;
		for (int k = 0; k < this.ridgeFreq.length; k++) {
			ridge += this.ridgeAmp[k] * Math.cos(this.ridgeFreq[k] * (theta + this.twist * t) + this.ridgePhase[k]);
		}
		return this.trunkCore[i] * (1.0D + 0.085D * ridge) + this.trunkFlare[i] * (0.55D + 0.45D * lobe(theta));
	}

	private boolean insideTrunkHollow(int x, int y, int z) {
		if (y <= this.solidTop || y > this.trunkTop) return false;
		int i = y - this.trunkBottom;
		double ox = x + 0.5D - this.trunkAxisX[i];
		double oz = z + 0.5D - this.trunkAxisZ[i];
		double r = Math.sqrt(ox * ox + oz * oz);
		if (r > this.trunkMaxR[i]) return false;
		return r < trunkRadius(i, Math.atan2(oz, ox), trunkProgress(y)) - this.trunkShell[i];
	}

	private void rasterizeCapsule(Capsule c, int x0, int y0, int z0, int sx, int sy, int sz, byte[] out, ColumnData columns) {
		int fromX = Math.max(x0, c.minX());
		int toX = Math.min(x0 + sx - 1, c.maxX());
		int fromY = Math.max(y0, c.minY());
		int toY = Math.min(y0 + sy - 1, c.maxY());
		int fromZ = Math.max(z0, c.minZ());
		int toZ = Math.min(z0 + sz - 1, c.maxZ());
		double abx = c.bx() - c.ax();
		double aby = c.by() - c.ay();
		double abz = c.bz() - c.az();
		double length2 = abx * abx + aby * aby + abz * abz;
		for (int y = fromY; y <= toY; y++) {
			for (int z = fromZ; z <= toZ; z++) {
				for (int x = fromX; x <= toX; x++) {
					double px = x + 0.5D - c.ax();
					double py = y + 0.5D - c.ay();
					double pz = z + 0.5D - c.az();
					double t = length2 > 1.0E-9D ? clamp01((px * abx + py * aby + pz * abz) / length2) : 0.0D;
					double qx = px - abx * t;
					double qy = py - aby * t;
					double qz = pz - abz * t;
					double r = c.ra() + (c.rb() - c.ra()) * t;
					if (qx * qx + qy * qy + qz * qz > r * r) continue;
					if (columns != null) {
						int column = (z - z0) * sx + (x - x0);
						if (columns.canopy[column] && insideCanopyCore(x + 0.5D, y + 0.5D, z + 0.5D, columns, column)) continue;
					}
					mark(out, sx, sz, x - x0, y - y0, z - z0, WOOD);
				}
			}
		}
	}

	private boolean insideCanopyCore(double x, double y, double z, ColumnData columns, int column) {
		return density(x, y, z, columns.low[column], columns.high[column], columns.radial[column]) >= SHELL + 2;
	}

	private double density(double x, double y, double z, double low, double high, double radial) {
		double dist = Math.min(Math.min(y - low, high - y), radial);
		if (dist < -6.0D || dist > SHELL + 8.0D) return dist;
		return dist + detailNoise.noise(x / 13.0D, y / 9.0D, z / 13.0D) * 3.8D
				+ detailNoise.noise(x / 5.5D + 100.0D, y / 5.5D, z / 5.5D) * 1.6D;
	}

	private double lowSurface(double x, double z, double u) {
		double lump = Math.max(0.0D, lumpNoise.noise(x / 24.0D, 13.7D, z / 24.0D)) * 6.0D
				+ Math.max(0.0D, lumpNoise.noise(x / 9.0D, 71.3D, z / 9.0D)) * 2.5D;
		return this.canopyBase + 10.0D * u * u - lump;
	}

	private double highSurface(double x, double z, double u) {
		double bumps = bumpNoise.noise(x / 34.0D, 5.1D, z / 34.0D) * 11.0D + bumpNoise.noise(x / 13.0D, 41.9D, z / 13.0D) * 4.0D;
		return this.canopyBase + 10.0D + this.canopyThickness * dome(u) + bumps;
	}

	private static double dome(double u) {
		if (u >= 1.0D) return 0.0D;
		return Math.pow(1.0D - Math.pow(u, 2.4D), 0.55D);
	}

	private double rimRadius(double theta) {
		double value = 0.0D;
		for (int i = 0; i < this.rimFreq.length; i++) {
			value += this.rimAmp[i] * Math.sin(this.rimFreq[i] * theta + this.rimPhase[i]);
		}
		return this.canopyRadius * (1.0D + RIM_AMPLITUDE * value);
	}

	private int undersideAt(int x, int z) {
		double px = x + 0.5D;
		double pz = z + 0.5D;
		double ox = px - this.axisOriginX;
		double oz = pz - this.axisOriginZ;
		double r = Math.sqrt(ox * ox + oz * oz);
		double rim = rimRadius(Math.atan2(oz, ox));
		double u = r / rim;
		double low = lowSurface(px, pz, Math.min(u, 1.08D));
		double high = highSurface(px, pz, Math.min(u, 1.08D));
		int start = (int) Math.floor(low) - 7;
		for (int y = start; y <= start + 21; y++) {
			if (density(px, y + 0.5D, pz, low, high, rim - r) > 0.0D) return y;
		}
		return (int) Math.floor(low);
	}

	private double lobe(double theta) {
		double normalized = theta / (Math.PI * 2.0D);
		normalized -= Math.floor(normalized);
		int index = (int) (normalized * LOBE_TABLE) & (LOBE_TABLE - 1);
		return this.lobeTable[index];
	}

	private double trunkProgress(int y) {
		return clamp01((y - this.baseY) / (double) (this.trunkTop - this.baseY));
	}

	private double coreRadius(int y) {
		return this.rBottom + (this.rTop - this.rBottom) * StrictMath.pow(trunkProgress(y), 0.8D);
	}

	private double flareRadius(int y) {
		double h = Math.max(0, y - this.baseY);
		return this.flare * StrictMath.exp(-h / this.flareHeight);
	}

	private void buildButtressRoots(Random random, Surface sampler) {
		double[] steps = {0.0D, 0.28D, 0.48D, 0.66D, 0.82D, 1.0D};
		for (double angle : this.buttressAngles) {
			double reach = 82.0D + random.nextDouble() * 20.0D;
			double r0 = 11.0D + random.nextDouble() * 4.0D;
			double startY = this.baseY + 8.0D + random.nextDouble() * 6.0D;
			double bend = (random.nextDouble() - 0.5D) * 0.35D;
			double startFraction = (this.rBottom * 0.8D) / reach;
			PathPoint previous = null;
			PathPoint forkFrom = null;
			for (double step : steps) {
				double fraction = Math.max(step, startFraction);
				double distance = reach * fraction;
				double a = angle + bend * fraction * fraction;
				double px = this.axisOriginX + StrictMath.cos(a) * distance;
				double pz = this.axisOriginZ + StrictMath.sin(a) * distance;
				double r = 1.2D + (r0 - 1.2D) * StrictMath.pow(1.0D - fraction, 0.85D);
				double py = step == 0.0D ? startY : sampler.at((int) Math.floor(px), (int) Math.floor(pz)) - 0.35D * r;
				PathPoint point = new PathPoint(px, py, pz, r);
				if (previous != null) this.baseCapsules.add(Capsule.of(previous.x(), previous.y(), previous.z(), previous.r(), px, py, pz, r));
				if (step == 0.48D) forkFrom = point;
				previous = point;
			}
			if (forkFrom != null && random.nextDouble() < 0.55D) {
				double side = random.nextBoolean() ? 1.0D : -1.0D;
				double forkAngle = angle + side * (0.35D + random.nextDouble() * 0.25D);
				double forkReach = reach * (0.45D + random.nextDouble() * 0.15D);
				PathPoint start = forkFrom;
				double fr0 = forkFrom.r() * 0.6D;
				for (double step : new double[]{0.5D, 1.0D}) {
					double px = forkFrom.x() + StrictMath.cos(forkAngle) * forkReach * step;
					double pz = forkFrom.z() + StrictMath.sin(forkAngle) * forkReach * step;
					double r = 1.1D + (fr0 - 1.1D) * (1.0D - step);
					double py = sampler.at((int) Math.floor(px), (int) Math.floor(pz)) - 0.35D * r;
					this.baseCapsules.add(Capsule.of(start.x(), start.y(), start.z(), start.r() * (step == 0.5D ? 0.6D : 1.0D), px, py, pz, r));
					start = new PathPoint(px, py, pz, r);
				}
			}
		}
	}

	private void buildLimbs(Random random, List<PathPoint> branchPoints) {
		int limbCount = 16 + random.nextInt(5);
		double baseAngle = random.nextDouble() * Math.PI * 2.0D;
		for (int i = 0; i < limbCount; i++) {
			double angle = baseAngle + i * Math.PI * 2.0D / limbCount + (random.nextDouble() - 0.5D) * 0.35D * (Math.PI * 2.0D / limbCount);
			double startY = this.canopyBase - 32 + random.nextDouble() * 22.0D;
			double length = this.canopyRadius * (0.80D + random.nextDouble() * 0.13D);
			double bend = (random.nextDouble() - 0.5D) * 0.5D;
			double r0 = 11.0D + random.nextDouble() * 3.0D;
			int axisIndex = clamp((int) Math.round(startY) - this.trunkBottom, 0, this.trunkAxisX.length - 1);
			double originX = this.trunkAxisX[axisIndex];
			double originZ = this.trunkAxisZ[axisIndex];
			double startDistance = this.trunkCore[axisIndex] * 0.6D;

			List<PathPoint> path = new ArrayList<>();
			int steps = 16;
			for (int k = 0; k <= steps; k++) {
				double t = k / (double) steps;
				double distance = Math.max(startDistance, length * t);
				double a = angle + bend * t * t;
				double px = originX + StrictMath.cos(a) * distance;
				double pz = originZ + StrictMath.sin(a) * distance;
				double r = 1.4D + (r0 - 1.4D) * StrictMath.pow(1.0D - t, 1.1D);
				double target = undersideAt((int) Math.floor(px), (int) Math.floor(pz)) + 0.3D * r - 0.5D;
				double py = startY + (target - startY) * smoothstep(0.0D, 0.28D, t);
				path.add(new PathPoint(px, py, pz, r));
			}
			addPath(path, branchPoints);

			int secondaryCount = 3 + random.nextInt(4);
			for (int j = 0; j < secondaryCount; j++) {
				double t = 0.25D + 0.70D * (j + random.nextDouble() * 0.8D) / secondaryCount;
				PathPoint origin = interpolate(path, t);
				double side = (j % 2 == 0) ? 1.0D : -1.0D;
				double branchAngle = angle + bend * t * t + side * (0.45D + random.nextDouble() * 0.6D);
				double branchLength = (22.0D + random.nextDouble() * 38.0D) * (1.15D - 0.5D * t);
				double branchRadius = Math.min(origin.r() * 0.6D, 5.5D);
				double branchBend = (random.nextDouble() - 0.5D) * 0.6D;
				List<PathPoint> branch = growBranch(origin, branchAngle, branchBend, branchLength, branchRadius, 8);
				if (branch.size() < 2) continue;
				addPath(branch, branchPoints);
				int twigs = random.nextDouble() < 0.6D ? 1 + random.nextInt(2) : 0;
				for (int k = 0; k < twigs; k++) {
					PathPoint twigOrigin = interpolate(branch, 0.4D + random.nextDouble() * 0.4D);
					double twigAngle = branchAngle + (random.nextBoolean() ? 1.0D : -1.0D) * (0.5D + random.nextDouble() * 0.4D);
					List<PathPoint> twig = growBranch(twigOrigin, twigAngle, 0.0D, 10.0D + random.nextDouble() * 14.0D,
							Math.min(twigOrigin.r() * 0.7D, 2.2D), 4);
					if (twig.size() >= 2) addPath(twig, branchPoints);
				}
			}
		}
	}

	private List<PathPoint> growBranch(PathPoint origin, double angle, double bend, double length, double r0, int steps) {
		List<PathPoint> path = new ArrayList<>();
		path.add(origin);
		for (int k = 1; k <= steps; k++) {
			double t = k / (double) steps;
			double a = angle + bend * t;
			double px = origin.x() + StrictMath.cos(a) * length * t;
			double pz = origin.z() + StrictMath.sin(a) * length * t;
			double ox = px - this.axisOriginX;
			double oz = pz - this.axisOriginZ;
			if (Math.sqrt(ox * ox + oz * oz) > rimRadius(StrictMath.atan2(oz, ox)) * 0.96D) break;
			double r = 1.05D + (r0 - 1.05D) * StrictMath.pow(1.0D - t, 1.1D);
			double py = undersideAt((int) Math.floor(px), (int) Math.floor(pz)) + 0.3D * r - 0.5D;
			path.add(new PathPoint(px, py, pz, r));
		}
		return path;
	}

	private void addPath(List<PathPoint> path, List<PathPoint> branchPoints) {
		for (int i = 1; i < path.size(); i++) {
			PathPoint a = path.get(i - 1);
			PathPoint b = path.get(i);
			this.branchCapsules.add(Capsule.of(a.x(), a.y(), a.z(), a.r(), b.x(), b.y(), b.z(), b.r()));
			double dx = b.x() - a.x();
			double dz = b.z() - a.z();
			int samplesAlong = Math.max(1, (int) (Math.sqrt(dx * dx + dz * dz) / 7.0D));
			for (int s = 0; s < samplesAlong; s++) {
				double f = (s + 0.5D) / samplesAlong;
				branchPoints.add(new PathPoint(a.x() + dx * f, a.y() + (b.y() - a.y()) * f, a.z() + dz * f, a.r() + (b.r() - a.r()) * f));
			}
		}
	}

	private static PathPoint interpolate(List<PathPoint> path, double t) {
		double position = t * (path.size() - 1);
		int index = Math.min(path.size() - 2, (int) Math.floor(position));
		double f = position - index;
		PathPoint a = path.get(index);
		PathPoint b = path.get(index + 1);
		return new PathPoint(a.x() + (b.x() - a.x()) * f, a.y() + (b.y() - a.y()) * f, a.z() + (b.z() - a.z()) * f, a.r() + (b.r() - a.r()) * f);
	}

	private void buildHangingRoots(Random random, List<PathPoint> branchPoints) {
		int target = 200 + random.nextInt(50);
		Map<Long, List<int[]>> grid = new HashMap<>();
		for (PathPoint point : branchPoints) {
			if (this.roots.size() >= target * 0.6D) break;
			if (random.nextDouble() > 0.5D) continue;
			tryAddRoot(random, grid, point.x(), point.z(), (int) Math.floor(point.y() - point.r() * 0.3D));
		}
		int attempts = 0;
		while (this.roots.size() < target && attempts++ < 12000) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double distance = Math.sqrt(random.nextDouble()) * this.canopyRadius * 0.95D;
			tryAddRoot(random, grid, this.axisOriginX + StrictMath.cos(angle) * distance,
					this.axisOriginZ + StrictMath.sin(angle) * distance, Integer.MIN_VALUE);
		}
	}

	private void pickFruits(Random random) {
		List<Integer> eligible = new ArrayList<>();
		for (int i = 0; i < this.roots.size(); i++) {
			HangingRoot root = this.roots.get(i);
			if (!root.reachesGround() && root.kind() == ROOT_THIN && root.topY() - root.bottomY() >= 14) eligible.add(i);
		}
		Collections.shuffle(eligible, random);
		int count = Math.min(eligible.size(), 6 + random.nextInt(5));
		for (int i = 0; i < count; i++) {
			int index = eligible.get(i);
			this.roots.set(index, this.roots.get(index).withFruit());
		}
	}

	private void tryAddRoot(Random random, Map<Long, List<int[]>> grid, double x, double z, int fixedTop) {
		int ix = (int) Math.floor(x);
		int iz = (int) Math.floor(z);
		double ox = ix + 0.5D - this.axisOriginX;
		double oz = iz + 0.5D - this.axisOriginZ;
		double r = Math.sqrt(ox * ox + oz * oz);
		if (r < this.rTop + 14.0D) return;
		if (r > rimRadius(StrictMath.atan2(oz, ox)) * 0.95D) return;

		int cellX = Math.floorDiv(ix, ROOT_SPACING);
		int cellZ = Math.floorDiv(iz, ROOT_SPACING);
		for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
			for (int cz = cellZ - 1; cz <= cellZ + 1; cz++) {
				List<int[]> cell = grid.get(pack(cx, cz));
				if (cell == null) continue;
				for (int[] other : cell) {
					int ddx = other[0] - ix;
					int ddz = other[1] - iz;
					if (ddx * ddx + ddz * ddz < ROOT_SPACING * ROOT_SPACING) return;
				}
			}
		}

		int top = fixedTop != Integer.MIN_VALUE ? fixedTop : undersideAt(ix, iz) + 1;
		boolean inArena = isArenaColumn(ix, iz, 4);
		double roll = random.nextDouble();
		int kind = roll < 0.74D ? ROOT_THIN : roll < 0.84D ? ROOT_SINGLE : roll < 0.97D ? ROOT_PLUS : ROOT_THICK;
		boolean reachesGround = !inArena && random.nextDouble() < 0.22D;
		int bottom;
		if (reachesGround) {
			bottom = Integer.MIN_VALUE;
		} else {
			int length = 10 + (int) (random.nextDouble() * 0.72D * Math.max(0, top - this.baseY));
			bottom = top - length;
			if (inArena) bottom = Math.max(bottom, this.arenaFloor + ROOT_ARENA_CLEARANCE);
			if (bottom >= top - 3) return;
		}
		if (kind == ROOT_THICK && (!reachesGround || r > 90.0D)) kind = ROOT_PLUS;

		grid.computeIfAbsent(pack(cellX, cellZ), key -> new ArrayList<>()).add(new int[]{ix, iz});
		this.roots.add(new HangingRoot(ix, iz, top, bottom, reachesGround, kind, false));
	}

	private static int mix(int x, int y, int z) {
		int h = x * 0x27D4EB2D ^ y * 0x165667B1 ^ z * 0x61C88647;
		h ^= h >>> 15;
		h *= 0x2C1B3C6D;
		h ^= h >>> 12;
		return h;
	}

	private static double angleDiff(double a, double b) {
		double d = (a - b) % (Math.PI * 2.0D);
		if (d < -Math.PI) d += Math.PI * 2.0D;
		if (d > Math.PI) d -= Math.PI * 2.0D;
		return d;
	}

	private static double smoothstep(double edge0, double edge1, double x) {
		double t = clamp01((x - edge0) / (edge1 - edge0));
		return t * t * (3.0D - 2.0D * t);
	}

	private static double clamp01(double value) {
		return value < 0.0D ? 0.0D : Math.min(value, 1.0D);
	}

	private static int clamp(int value, int min, int max) {
		return value < min ? min : Math.min(value, max);
	}

	private static final class Perlin {
		private final int[] permutation = new int[512];

		Perlin(long seed) {
			Random random = new Random(seed);
			int[] base = new int[256];
			for (int i = 0; i < 256; i++) base[i] = i;
			for (int i = 255; i > 0; i--) {
				int j = random.nextInt(i + 1);
				int swap = base[i];
				base[i] = base[j];
				base[j] = swap;
			}
			for (int i = 0; i < 512; i++) this.permutation[i] = base[i & 255];
		}

		double noise(double x, double y, double z) {
			double fx = Math.floor(x);
			double fy = Math.floor(y);
			double fz = Math.floor(z);
			int ix = (int) fx & 255;
			int iy = (int) fy & 255;
			int iz = (int) fz & 255;
			x -= fx;
			y -= fy;
			z -= fz;
			double u = fade(x);
			double v = fade(y);
			double w = fade(z);
			int[] p = this.permutation;
			int a = p[ix] + iy;
			int aa = p[a] + iz;
			int ab = p[a + 1] + iz;
			int b = p[ix + 1] + iy;
			int ba = p[b] + iz;
			int bb = p[b + 1] + iz;
			return lerp(w,
					lerp(v, lerp(u, grad(p[aa], x, y, z), grad(p[ba], x - 1, y, z)),
							lerp(u, grad(p[ab], x, y - 1, z), grad(p[bb], x - 1, y - 1, z))),
					lerp(v, lerp(u, grad(p[aa + 1], x, y, z - 1), grad(p[ba + 1], x - 1, y, z - 1)),
							lerp(u, grad(p[ab + 1], x, y - 1, z - 1), grad(p[bb + 1], x - 1, y - 1, z - 1))));
		}

		private static double fade(double t) {
			return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
		}

		private static double lerp(double t, double a, double b) {
			return a + t * (b - a);
		}

		private static double grad(int hash, double x, double y, double z) {
			int h = hash & 15;
			double u = h < 8 ? x : y;
			double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
			return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
		}
	}
}
