package com.dragonminez.common.combat.util;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainGameRules;
import com.dragonminez.common.init.block.custom.DragonBallBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KiTerrainDestruction {

	private static final int BLOCK_BUDGET_PER_TICK = 8192;
	private static final int MAX_DEBRIS_PER_PASS = 6;
	private static final float INDESTRUCTIBLE_RESISTANCE = 1000.0F;
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	private static final Map<ServerLevel, LevelQueue> QUEUES = new HashMap<>();

	private KiTerrainDestruction() {}

	private enum Mode {
		CARVE(false, false, false),
		EAT(true, true, true),
		SLICE(true, true, false);

		private final boolean keepFluids;
		private final boolean debris;
		private final boolean smoke;

		Mode(boolean keepFluids, boolean debris, boolean smoke) {
			this.keepFluids = keepFluids;
			this.debris = debris;
			this.smoke = smoke;
		}
	}

	public static void carve(ServerLevel level, BlockPos center, float innerRadius, float radius, int flags, Entity source) {
		if (radius <= 0.0F) return;
		Job job = Job.create(level, new SphereRegion(center, innerRadius, radius, null, 0.0F), flags, Mode.CARVE, source);
		if (job != null) submit(level, job);
	}

	public static boolean eat(ServerLevel level, BlockPos center, float radius, BlockPos skipCenter, float skipRadius, Entity source) {
		if (radius <= 0.0F) return false;
		Job job = Job.create(level, new SphereRegion(center, 0.0F, radius, skipCenter, skipRadius), Block.UPDATE_CLIENTS, Mode.EAT, source);
		if (job == null || !job.probe(level)) return false;
		submit(level, job);
		return true;
	}

	public static void slice(ServerLevel level, Sweep sweep, double fromDistance, double toDistance, Entity source) {
		if (sweep == null || toDistance <= fromDistance) return;
		SweepRegion region = new SweepRegion(sweep, fromDistance, toDistance);
		if (region.isEmpty()) return;
		Job job = Job.create(level, region, Block.UPDATE_CLIENTS, Mode.SLICE, source);
		if (job != null) submit(level, job);
	}

	public static Sweep diskSweep(Vec3 origin, Vec3 direction, Vec3 lateral, float radius) {
		return new Sweep(origin, direction, lateral, radius, radius, false, true, Double.NEGATIVE_INFINITY, 0.0F);
	}

	public static Sweep crescentSweep(Vec3 origin, Vec3 direction, Vec3 lateral, float radius, float halfWidth, float maxDepth) {
		return new Sweep(origin, direction, lateral, radius, Math.min(halfWidth, radius), true, false, 0.0D, maxDepth);
	}

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
		LevelQueue queue = QUEUES.get(level);
		if (queue == null) return;
		drain(level, queue);
		if (queue.jobs.isEmpty()) QUEUES.remove(level);
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel level) QUEUES.remove(level);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		QUEUES.clear();
	}

	private static void submit(ServerLevel level, Job job) {
		LevelQueue queue = QUEUES.computeIfAbsent(level, l -> new LevelQueue());
		queue.jobs.addLast(job);
		drain(level, queue);
	}

	private static void drain(ServerLevel level, LevelQueue queue) {
		if (queue.draining) return;
		int tick = level.getServer().getTickCount();
		if (queue.tick != tick) {
			queue.tick = tick;
			queue.budget = BLOCK_BUDGET_PER_TICK;
		}
		queue.draining = true;
		try {
			while (queue.budget > 0 && !queue.jobs.isEmpty()) {
				Job job = queue.jobs.peekFirst();
				queue.budget -= job.process(level, queue.budget);
				if (job.isDone()) queue.jobs.pollFirst();
			}
		} finally {
			queue.draining = false;
		}
	}

	private static final class LevelQueue {
		private final ArrayDeque<Job> jobs = new ArrayDeque<>();
		private int tick = Integer.MIN_VALUE;
		private int budget;
		private boolean draining;
	}

	private interface Region {
		BoundingBox bounds();

		boolean mayIntersectSection(int x0, int y0, int z0);

		long sectionOrder(int x0, int y0, int z0);

		boolean contains(int x, int y, int z);

		default boolean accept(int x, int y, int z) {
			return true;
		}
	}

	private static final class SphereRegion implements Region {
		private final int cx, cy, cz, r;
		private final float outerSq, innerSq;
		private final int px, py, pz;
		private final float skipSq;

		private SphereRegion(BlockPos center, float innerRadius, float radius, BlockPos skipCenter, float skipRadius) {
			this.cx = center.getX();
			this.cy = center.getY();
			this.cz = center.getZ();
			this.r = Mth.ceil(radius);
			this.outerSq = radius * radius;
			this.innerSq = innerRadius > 0.0F ? innerRadius * innerRadius : -1.0F;
			boolean skip = skipCenter != null && skipRadius > 0.0F;
			this.px = skip ? skipCenter.getX() : 0;
			this.py = skip ? skipCenter.getY() : 0;
			this.pz = skip ? skipCenter.getZ() : 0;
			this.skipSq = skip ? skipRadius * skipRadius : -1.0F;
		}

		@Override
		public BoundingBox bounds() {
			return new BoundingBox(this.cx - this.r, this.cy - this.r, this.cz - this.r,
					this.cx + this.r, this.cy + this.r, this.cz + this.r);
		}

		@Override
		public boolean mayIntersectSection(int x0, int y0, int z0) {
			if (this.sectionOrder(x0, y0, z0) > this.outerSq) return false;
			if (this.innerSq >= 0.0F && axisFar(this.cx, x0) + axisFar(this.cy, y0) + axisFar(this.cz, z0) <= this.innerSq) return false;
			return !(this.skipSq >= 0.0F && axisFar(this.px, x0) + axisFar(this.py, y0) + axisFar(this.pz, z0) <= this.skipSq);
		}

		@Override
		public long sectionOrder(int x0, int y0, int z0) {
			return axisNear(this.cx, x0) + axisNear(this.cy, y0) + axisNear(this.cz, z0);
		}

		@Override
		public boolean contains(int x, int y, int z) {
			int dx = x - this.cx, dy = y - this.cy, dz = z - this.cz;
			int distSq = dx * dx + dy * dy + dz * dz;
			if (distSq > this.outerSq || distSq <= this.innerSq) return false;
			if (this.skipSq < 0.0F) return true;
			int sx = x - this.px, sy = y - this.py, sz = z - this.pz;
			return sx * sx + sy * sy + sz * sz > this.skipSq;
		}

		private static long axisNear(int c, int lo) {
			int d = c < lo ? lo - c : (c > lo + 15 ? c - (lo + 15) : 0);
			return (long) d * d;
		}

		private static long axisFar(int c, int lo) {
			int d = Math.max(Math.abs(c - lo), Math.abs(c - (lo + 15)));
			return (long) d * d;
		}
	}

	public static final class Sweep {
		private final double ox, oy, oz;
		private final double dx, dy, dz;
		private final double sx, sy, sz;
		private final double nx, ny, nz;
		private final double halfThickness;
		private final double radius;
		private final double halfWidth;
		private final boolean crescent;
		private final boolean straightTail;
		private final double minForward;
		private final double maxDepth;
		private final float[] firstContact;
		private final int binOffset;

		private Sweep(Vec3 origin, Vec3 direction, Vec3 lateral, double radius, double halfWidth,
					  boolean crescent, boolean straightTail, double minForward, double maxDepth) {
			Vec3 d = direction.normalize();
			Vec3 s = lateral.subtract(d.scale(lateral.dot(d)));
			if (s.lengthSqr() < 1.0E-6) {
				s = Math.abs(d.y) < 0.9D ? new Vec3(-d.z, 0.0D, d.x) : new Vec3(1.0D, 0.0D, 0.0D).subtract(d.scale(d.x));
			}
			s = s.normalize();
			Vec3 n = d.cross(s).normalize();

			this.ox = origin.x;
			this.oy = origin.y;
			this.oz = origin.z;
			this.dx = d.x;
			this.dy = d.y;
			this.dz = d.z;
			this.sx = s.x;
			this.sy = s.y;
			this.sz = s.z;
			this.nx = n.x;
			this.ny = n.y;
			this.nz = n.z;
			this.halfThickness = Math.max(Math.abs(n.x), Math.max(Math.abs(n.y), Math.abs(n.z))) * 0.5D;
			this.radius = radius;
			this.halfWidth = halfWidth;
			this.crescent = crescent;
			this.straightTail = straightTail;
			this.minForward = minForward;
			this.maxDepth = maxDepth;

			int bins = Mth.ceil(halfWidth);
			this.binOffset = bins;
			if (maxDepth > 0.0D) {
				this.firstContact = new float[bins * 2 + 1];
				Arrays.fill(this.firstContact, Float.NaN);
			} else {
				this.firstContact = null;
			}
		}

		private double lead(double u) {
			double q = this.radius * this.radius - u * u;
			double edge = q > 0.0D ? Math.sqrt(q) : 0.0D;
			return this.crescent ? edge - this.radius : edge;
		}

		private boolean withinDepth(double u, double w) {
			if (this.firstContact == null) return true;
			int bin = Mth.clamp(Mth.floor(u) + this.binOffset, 0, this.firstContact.length - 1);
			float first = this.firstContact[bin];
			if (Float.isNaN(first)) {
				this.firstContact[bin] = (float) w;
				return true;
			}
			return w < first + this.maxDepth;
		}
	}

	private static final class SweepRegion implements Region {
		private final Sweep sweep;
		private final double t0, t1;
		private final double minW, maxW;

		private SweepRegion(Sweep sweep, double t0, double t1) {
			this.sweep = sweep;
			this.t0 = t0;
			this.t1 = t1;
			double tail = sweep.straightTail ? t0 : t0 + sweep.lead(sweep.halfWidth);
			this.minW = Math.max(tail, sweep.minForward);
			this.maxW = t1 + sweep.lead(0.0D);
		}

		private boolean isEmpty() {
			return this.maxW < this.minW;
		}

		@Override
		public BoundingBox bounds() {
			Sweep s = this.sweep;
			double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
			for (int iu = -1; iu <= 1; iu += 2) {
				for (int iw = 0; iw <= 1; iw++) {
					for (int in = -1; in <= 1; in += 2) {
						double u = iu * s.halfWidth;
						double w = iw == 0 ? this.minW : this.maxW;
						double n = in * s.halfThickness;
						double x = s.ox + s.sx * u + s.dx * w + s.nx * n;
						double y = s.oy + s.sy * u + s.dy * w + s.ny * n;
						double z = s.oz + s.sz * u + s.dz * w + s.nz * n;
						minX = Math.min(minX, x);
						minY = Math.min(minY, y);
						minZ = Math.min(minZ, z);
						maxX = Math.max(maxX, x);
						maxY = Math.max(maxY, y);
						maxZ = Math.max(maxZ, z);
					}
				}
			}
			return new BoundingBox(Mth.floor(minX), Mth.floor(minY), Mth.floor(minZ),
					Mth.floor(maxX), Mth.floor(maxY), Mth.floor(maxZ));
		}

		@Override
		public boolean mayIntersectSection(int x0, int y0, int z0) {
			Sweep s = this.sweep;
			double cx = x0 + 8.0D - s.ox, cy = y0 + 8.0D - s.oy, cz = z0 + 8.0D - s.oz;
			double n = cx * s.nx + cy * s.ny + cz * s.nz;
			if (Math.abs(n) > s.halfThickness + 8.0D * (Math.abs(s.nx) + Math.abs(s.ny) + Math.abs(s.nz))) return false;
			double u = cx * s.sx + cy * s.sy + cz * s.sz;
			if (Math.abs(u) > s.halfWidth + 8.0D * (Math.abs(s.sx) + Math.abs(s.sy) + Math.abs(s.sz))) return false;
			double w = cx * s.dx + cy * s.dy + cz * s.dz;
			double reach = 8.0D * (Math.abs(s.dx) + Math.abs(s.dy) + Math.abs(s.dz));
			return w >= this.minW - reach && w <= this.maxW + reach;
		}

		@Override
		public long sectionOrder(int x0, int y0, int z0) {
			Sweep s = this.sweep;
			double w = (x0 + 8.0D - s.ox) * s.dx + (y0 + 8.0D - s.oy) * s.dy + (z0 + 8.0D - s.oz) * s.dz;
			return Math.round(w * 16.0D);
		}

		@Override
		public boolean contains(int x, int y, int z) {
			Sweep s = this.sweep;
			double cx = x + 0.5D - s.ox, cy = y + 0.5D - s.oy, cz = z + 0.5D - s.oz;
			double n = cx * s.nx + cy * s.ny + cz * s.nz;
			if (n < -s.halfThickness || n >= s.halfThickness) return false;
			double u = cx * s.sx + cy * s.sy + cz * s.sz;
			if (Math.abs(u) > s.halfWidth) return false;
			double w = cx * s.dx + cy * s.dy + cz * s.dz;
			if (w < s.minForward) return false;
			double lead = s.lead(u);
			if (w > this.t1 + lead) return false;
			return s.straightTail ? w >= this.t0 : w > this.t0 + lead;
		}

		@Override
		public boolean accept(int x, int y, int z) {
			Sweep s = this.sweep;
			double cx = x + 0.5D - s.ox, cy = y + 0.5D - s.oy, cz = z + 0.5D - s.oz;
			return s.withinDepth(cx * s.sx + cy * s.sy + cz * s.sz, cx * s.dx + cy * s.dy + cz * s.dz);
		}
	}

	private static final class Job {
		private final Region region;
		private final BoundingBox bounds;
		private final int flags;
		private final Mode mode;
		private final MainGameRules.KiGriefGate gate;
		private final long[] sections;
		private int next;
		private int debrisLeft;

		private Job(Region region, BoundingBox bounds, int flags, Mode mode, MainGameRules.KiGriefGate gate, long[] sections) {
			this.region = region;
			this.bounds = bounds;
			this.flags = flags;
			this.mode = mode;
			this.gate = gate;
			this.sections = sections;
		}

		private static Job create(ServerLevel level, Region region, int flags, Mode mode, Entity source) {
			BoundingBox bounds = region.bounds();
			int minSecY = Math.max(level.getMinSection(), SectionPos.blockToSectionCoord(bounds.minY()));
			int maxSecY = Math.min(level.getMaxSection() - 1, SectionPos.blockToSectionCoord(bounds.maxY()));
			if (minSecY > maxSecY) return null;

			List<long[]> found = new ArrayList<>();
			for (int sx = SectionPos.blockToSectionCoord(bounds.minX()); sx <= SectionPos.blockToSectionCoord(bounds.maxX()); sx++) {
				for (int sz = SectionPos.blockToSectionCoord(bounds.minZ()); sz <= SectionPos.blockToSectionCoord(bounds.maxZ()); sz++) {
					LevelChunk chunk = level.getChunkSource().getChunkNow(sx, sz);
					if (chunk == null) continue;
					int x0 = sx << 4, z0 = sz << 4;

					for (int sy = minSecY; sy <= maxSecY; sy++) {
						int y0 = sy << 4;
						if (!region.mayIntersectSection(x0, y0, z0)) continue;
						if (chunk.getSection(chunk.getSectionIndexFromSectionY(sy)).hasOnlyAir()) continue;
						found.add(new long[]{SectionPos.asLong(sx, sy, sz), region.sectionOrder(x0, y0, z0)});
					}
				}
			}
			if (found.isEmpty()) return null;

			MainGameRules.KiGriefGate gate = MainGameRules.griefGate(level, bounds, source);
			if (gate.deniesEverything()) return null;

			found.sort(Comparator.comparingLong(entry -> entry[1]));
			long[] sections = new long[found.size()];
			for (int i = 0; i < sections.length; i++) sections[i] = found.get(i)[0];

			return new Job(region, bounds, flags, mode, gate, sections);
		}

		private boolean isDone() {
			return this.next >= this.sections.length;
		}

		private boolean probe(ServerLevel level) {
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			for (long sectionPos : this.sections) {
				if (this.visitSection(level, sectionPos, cursor, false) > 0) return true;
			}
			return false;
		}

		private int process(ServerLevel level, int budget) {
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			this.debrisLeft = MAX_DEBRIS_PER_PASS;
			int spent = 0;
			while (this.next < this.sections.length && spent < budget) {
				spent += this.visitSection(level, this.sections[this.next++], cursor, true);
			}
			return spent;
		}

		private int visitSection(ServerLevel level, long sectionPos, BlockPos.MutableBlockPos cursor, boolean destroy) {
			int sx = SectionPos.x(sectionPos), sy = SectionPos.y(sectionPos), sz = SectionPos.z(sectionPos);
			LevelChunk chunk = level.getChunkSource().getChunkNow(sx, sz);
			if (chunk == null) return 0;
			LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
			if (section.hasOnlyAir()) return 0;

			int x0 = sx << 4, y0 = sy << 4, z0 = sz << 4;
			int minX = Math.max(x0, this.bounds.minX()), maxX = Math.min(x0 + 15, this.bounds.maxX());
			int minY = Math.max(y0, this.bounds.minY()), maxY = Math.min(y0 + 15, this.bounds.maxY());
			int minZ = Math.max(z0, this.bounds.minZ()), maxZ = Math.min(z0 + 15, this.bounds.maxZ());

			int checked = 0;
			int destroyed = 0;
			BlockPos sample = null;
			BlockState sampleState = null;

			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					for (int y = minY; y <= maxY; y++) {
						if (!this.region.contains(x, y, z)) continue;

						BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
						if (state.isAir()) continue;
						cursor.set(x, y, z);
						BlockState replacement = this.replacementFor(level, cursor, state);
						if (replacement == null) continue;

						checked++;
						if (!this.gate.canGrief(cursor)) continue;
						if (!destroy) return checked;
						if (!this.region.accept(x, y, z)) continue;

						BlockPos pos = cursor.immutable();
						if (!level.setBlock(pos, replacement, this.flags)) continue;
						destroyed++;
						if (level.random.nextInt(destroyed) == 0) {
							sample = pos;
							sampleState = state;
						}
					}
				}
			}

			if (!destroy) return 0;
			if (this.mode.debris && sample != null && this.debrisLeft > 0) {
				this.debrisLeft--;
				level.levelEvent(2001, sample, Block.getId(sampleState));
				if (this.mode.smoke) {
					level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
							sample.getX() + 0.5D, sample.getY() + 0.5D, sample.getZ() + 0.5D,
							Math.min(1 + destroyed / 64, 4), 1.5D, 1.5D, 1.5D, 0.05D);
				}
			}
			return checked;
		}

		private BlockState replacementFor(ServerLevel level, BlockPos pos, BlockState state) {
			if (state.getBlock() instanceof DragonBallBlock) return null;
			BlockState replacement = AIR;
			if (this.mode.keepFluids) {
				FluidState fluid = state.getFluidState();
				if (!fluid.isEmpty()) {
					if (state.getBlock() instanceof LiquidBlock) return null;
					replacement = fluid.createLegacyBlock();
				}
			}
			if (state.getExplosionResistance(level, pos, null) >= INDESTRUCTIBLE_RESISTANCE) return null;
			return replacement;
		}
	}
}
