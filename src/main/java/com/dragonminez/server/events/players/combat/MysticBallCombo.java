package com.dragonminez.server.events.players.combat;

import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ImpactBurstVfxS2C;
import com.dragonminez.common.network.S2C.KiBurstVfxS2C;
import com.dragonminez.common.network.S2C.ShockwaveVfxS2C;
import com.dragonminez.common.network.S2C.TriggerImpactFrameS2C;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class MysticBallCombo {

	public static final String TECHNIQUE_ID = "mystic_ball";
	public static final String ANIMATION = "skp.mystic_ball";
	private static final int LEAD = 4;
	public static final int DURATION = LEAD + 92;
	public static final int COLOR_MAIN = 0xFF82F3;
	public static final int COLOR_DARK = 0xFF1AEC;

	private static final int KICK_TICK = LEAD + 5;
	private static final int LAUNCH_END = LEAD + 15;
	private static final int CURL_TICK = LEAD + 14;
	private static final int[] BOUNCE_TICKS = {LEAD + 20, LEAD + 26, LEAD + 32, LEAD + 38, LEAD + 44, LEAD + 50};
	private static final int RISE_END = LEAD + 54;
	private static final int DIVE_START = LEAD + 56;
	private static final int STOMP_TICK = LEAD + 61;
	private static final int QUAKE_TICK = LEAD + 63;
	private static final int PRESS_END = LEAD + 66;
	private static final int LAND_TICK = LEAD + 73;
	private static final int SQUASH_TICKS = 3;
	private static final double LAUNCH_HEIGHT = 4.5D;
	private static final double CASTER_LIFT_RATIO = 0.75D;
	private static final double BOUNCE_RADIUS = 5.0D;
	private static final double MIN_BOUNCE_RADIUS = 2.5D;
	private static final double CONTACT_GAP = 0.45D;
	private static final double BALL_CENTER = 0.75D;
	private static final double BALL_RADIUS = 0.8D;
	private static final double RISE_HEIGHT = 4.5D;
	private static final double HIT_PUSH = 0.5D;
	private static final double FLIP_BACK = 2.8D;
	private static final double FLIP_ARC = 1.8D;
	private static final double MAX_VICTIM_DISTANCE = 48.0D;
	private static final double MAX_LAND_DROP = 10.0D;
	private static final double MAX_SLAM_DROP = 24.0D;
	private static final double STOMP_SPEED = 3.2D;
	private static final float KICK_SHARE = 0.17F;
	private static final float BOUNCE_SHARE = 0.08F;
	private static final float STOMP_SHARE = 0.35F;

	public interface Performer {
		LivingEntity self();

		LivingEntity target();

		void place(Vec3 feet);

		float strikeBudget();

		void strikeHit(LivingEntity victim, float amount, boolean lethal);
	}

	public static final class State {
		private HeldVictim victim;
		private LivingEntity stomped;
		private Vec3 base;
		private Vec3 start;
		private Vec3 apex;
		private Vec3 hover;
		private Vec3 hoverGoal;
		private Vec3 ball;
		private Vec3 from;
		private Vec3 fromDir;
		private Vec3 toDir;
		private double reach;
		private int bounce;
		private Vec3 above;
		private Vec3 press;
		private Vec3 slamFrom;
		private Vec3 ground;
		private Vec3 landing;
		private float yaw;

		public LivingEntity victim() {
			return this.victim != null ? this.victim.entity() : null;
		}
	}

	private MysticBallCombo() {}

	public static boolean tick(ServerLevel level, Performer performer, State state, int tick) {
		LivingEntity self = performer.self();
		self.fallDistance = 0.0F;

		if (state.base == null) {
			LivingEntity target = performer.target();
			if (!HeldVictim.canTarget(self, target) || target.level() != level) return false;
			state.base = self.position();
			state.yaw = self.getYRot();
			state.victim = HeldVictim.canHold(self, target) ? HeldVictim.capture(target, DURATION + 20) : HeldVictim.root(target);
			state.start = target.position();
			state.hover = state.start;
			state.hoverGoal = state.start;
		}

		if (tick > STOMP_TICK) return finish(level, performer, state, tick);

		if (state.victim == null || !state.victim.isValid(self, MAX_VICTIM_DISTANCE)) return false;
		LivingEntity victim = state.victim.entity();

		if (tick < CURL_TICK) {
			kick(level, performer, state, tick);
			return true;
		}

		if (state.victim.isPinned()) {
			state.hover = state.hover.lerp(state.hoverGoal, 0.5D);
			state.victim.hold(state.hover, yawTowards(state.hover, state.ball != null ? state.ball : self.position()));
		}

		if (tick == CURL_TICK) {
			state.ball = self.position().add(0.0D, BALL_CENTER, 0.0D);
			state.from = state.ball;
			state.fromDir = flatten(state.ball.subtract(center(state)), state.yaw + 180.0F);
			state.toDir = pickDirection(level, self, state, state.fromDir);
			state.reach = clearReach(level, self, center(state), state.toDir, BOUNCE_RADIUS + contactRadius(victim));
			state.bounce = 0;
			playSound(level, state.ball, self, MainSounds.STAFF_SPIN.get(), 1.4F, 1.3F);
		}

		if (state.bounce < BOUNCE_TICKS.length) {
			bounce(level, performer, state, tick);
		} else if (tick <= RISE_END) {
			int startTick = BOUNCE_TICKS[BOUNCE_TICKS.length - 1];
			double u = (tick - startTick) / (double) (RISE_END - startTick);
			Vec3 dir = slerp(state.fromDir, new Vec3(0.0D, 1.0D, 0.0D), smooth(u));
			double radius = Mth.lerp(easeOut(u), state.from.subtract(center(state)).length(), state.reach);
			moveBall(performer, state, center(state).add(dir.scale(radius)));
			if (tick == RISE_END) {
				state.above = state.ball;
				playSound(level, state.ball, self, MainSounds.KI_EXPLOSION_CHARGE.get(), 1.2F, 1.6F);
			}
		} else if (tick < DIVE_START) {
			moveBall(performer, state, state.above);
		} else {
			double u = (tick - DIVE_START) / (double) (STOMP_TICK - DIVE_START);
			Vec3 top = center(state).add(0.0D, victim.getBbHeight() * 0.5D + BALL_CENTER, 0.0D);
			moveBall(performer, state, state.above.lerp(top, u * u * u));
			if (tick == DIVE_START) playSound(level, state.above, self, MainSounds.EVASION2.get(), 1.6F, 0.7F);
			if (tick == STOMP_TICK) stomp(level, performer, state);
		}
		return true;
	}

	public static boolean isBallTick(int tick) {
		return tick >= CURL_TICK && tick <= STOMP_TICK;
	}

	public static float squash(float tick) {
		for (int hit : BOUNCE_TICKS) {
			float since = tick - hit;
			if (since >= 0.0F && since < SQUASH_TICKS) return 1.0F - since / SQUASH_TICKS;
		}
		return 0.0F;
	}

	public static void release(State state) {
		if (state.victim != null) {
			state.victim.release();
			state.victim = null;
		}
		state.stomped = null;
		state.base = null;
		state.ball = null;
		state.press = null;
		state.slamFrom = null;
		state.ground = null;
		state.landing = null;
	}

	private static void kick(ServerLevel level, Performer performer, State state, int tick) {
		LivingEntity self = performer.self();
		float faceBack = state.yaw + 180.0F;

		if (tick < KICK_TICK) {
			performer.place(state.base);
			state.victim.hold(state.start, faceBack);
			return;
		}

		if (tick == KICK_TICK) {
			LivingEntity victim = state.victim.entity();
			victim.invulnerableTime = 0;
			performer.strikeHit(victim, performer.strikeBudget() * KICK_SHARE, false);
			state.victim.launched();
			state.apex = clearAbove(level, victim, state.start, LAUNCH_HEIGHT);

			Vec3 at = victim.position().add(0.0D, victim.getBbHeight() * 0.35D, 0.0D);
			NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(at, new Vec3(0.0D, 1.0D, 0.0D), 4.2F,
					COLOR_MAIN, COLOR_DARK, false, 14), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.65F, 0.05F, 2, false), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), false, 8.0F), self);
			playSound(level, at, self, MainSounds.CRITICO2.get(), 2.2F, 1.0F);
			playSound(level, at, self, MainSounds.EVASION1.get(), 1.4F, 0.7F);
		}

		double u = Mth.clamp((tick - KICK_TICK) / (double) (LAUNCH_END - KICK_TICK), 0.0D, 1.0D);
		double lift = easeOut(u);
		state.hover = state.start.lerp(state.apex, lift);
		state.hoverGoal = state.apex;
		state.victim.hold(state.hover, faceBack);
		performer.place(state.base.add(0.0D, (state.apex.y - state.start.y) * CASTER_LIFT_RATIO * lift, 0.0D));
	}

	private static void bounce(ServerLevel level, Performer performer, State state, int tick) {
		LivingEntity self = performer.self();
		LivingEntity victim = state.victim.entity();
		int startTick = state.bounce == 0 ? CURL_TICK : BOUNCE_TICKS[state.bounce - 1];
		int hitTick = BOUNCE_TICKS[state.bounce];
		double u = Mth.clamp((tick - startTick) / (double) (hitTick - startTick), 0.0D, 1.0D);

		Vec3 center = center(state);
		double startRadius = state.from.subtract(center).length();
		double contact = contactRadius(victim);
		double inbound = (u - 0.5D) * 2.0D;
		double radius = u < 0.5D
				? Mth.lerp(easeOut(u * 2.0D), startRadius, state.reach)
				: Mth.lerp(inbound * inbound * inbound, state.reach, contact);
		Vec3 dir = slerp(state.fromDir, state.toDir, smooth(u));
		moveBall(performer, state, center.add(dir.scale(radius)));

		if (tick != hitTick) return;

		victim.invulnerableTime = 0;
		performer.strikeHit(victim, performer.strikeBudget() * BOUNCE_SHARE, false);
		state.victim.flinch();

		RandomSource random = self.getRandom();
		Vec3 at = center.add(state.toDir.scale(contact - CONTACT_GAP));
		NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(at, state.toDir.scale(-1.0D), 3.0F,
				COLOR_MAIN, COLOR_DARK, false, 10), self);
		NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), false, 6.0F), self);
		SoundEvent sound = switch (random.nextInt(3)) {
			case 0 -> MainSounds.GOLPE4.get();
			case 1 -> MainSounds.GOLPE5.get();
			default -> MainSounds.GOLPE6.get();
		};
		playSound(level, at, self, sound, 2.0F, 0.8F + random.nextFloat() * 0.3F);
		playSound(level, at, self, MainSounds.KNOCKBACK_CHARACTER.get(), 0.8F, 1.4F + random.nextFloat() * 0.2F);

		if (state.victim.isPinned()) {
			Vec3 pushed = state.hoverGoal.add(state.toDir.scale(-HIT_PUSH));
			if (level.noCollision(victim, victim.getBoundingBox().move(pushed.subtract(victim.position())))) state.hoverGoal = pushed;
		}

		state.bounce++;
		state.from = state.ball;
		state.fromDir = state.toDir;
		if (state.bounce < BOUNCE_TICKS.length) {
			state.toDir = pickDirection(level, self, state, state.fromDir);
			state.reach = clearReach(level, self, center(state), state.toDir, BOUNCE_RADIUS + contact);
		} else {
			state.reach = clearReach(level, self, center(state), new Vec3(0.0D, 1.0D, 0.0D), RISE_HEIGHT + contact);
		}
		playSound(level, state.ball, self, MainSounds.STAFF_SPIN.get(), 1.3F, 1.2F + random.nextFloat() * 0.3F);
	}

	private static void stomp(ServerLevel level, Performer performer, State state) {
		LivingEntity self = performer.self();
		LivingEntity victim = state.victim.entity();
		boolean pinned = state.victim.isPinned();
		Vec3 feet = state.ball.subtract(0.0D, BALL_CENTER, 0.0D);
		state.press = feet;

		victim.invulnerableTime = 0;
		performer.strikeHit(victim, performer.strikeBudget() * STOMP_SHARE, true);

		NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(feet, new Vec3(0.0D, -1.0D, 0.0D), 6.5F,
				COLOR_MAIN, COLOR_DARK, false, 18), self);
		NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.8F, 0.05F, 3, true), self);
		NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), true, 14.0F), self);
		NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(feet.x, feet.y, feet.z, 4.0F, COLOR_DARK, 10), self);
		playSound(level, feet, self, MainSounds.CRITICO1.get(), 3.0F, 0.75F);
		playSound(level, feet, self, MainSounds.ANCHOR_SLAM.get(), 2.0F, 1.1F);

		if (!victim.isAlive()) {
			state.victim.release();
			state.victim = null;
			return;
		}
		state.stomped = victim;
		state.victim.knockDown();
		if (!pinned) return;

		state.slamFrom = victim.position();
		double floor = groundHeight(level, state.slamFrom.x, state.slamFrom.z, state.slamFrom.y + 0.5D);
		if (state.slamFrom.y - floor <= MAX_SLAM_DROP) {
			state.ground = new Vec3(state.slamFrom.x, floor, state.slamFrom.z);
			return;
		}

		state.victim.release();
		state.victim = null;
		KnockbackHelper.apply(victim, new Vec3(0.0D, -STOMP_SPEED, 0.0D));
	}

	private static boolean finish(ServerLevel level, Performer performer, State state, int tick) {
		LivingEntity self = performer.self();
		if (state.press == null) return false;

		LivingEntity stomped = state.stomped;
		boolean riding = stomped != null && stomped.isAlive() && stomped.level() == level
				&& stomped.distanceToSqr(state.press) < 64.0D;

		if (state.victim != null && !riding) {
			state.victim.release();
			state.victim = null;
		}
		if (state.victim != null && state.ground != null) {
			double u = Mth.clamp((tick - STOMP_TICK) / (double) (QUAKE_TICK - STOMP_TICK), 0.0D, 1.0D);
			state.victim.hold(state.slamFrom.lerp(state.ground, u * u), state.yaw + 180.0F);
		}

		if (tick <= PRESS_END) {
			if (riding) state.press = stomped.position().add(0.0D, stomped.getBbHeight(), 0.0D);
			performer.place(state.press);
			if (tick == QUAKE_TICK) {
				Vec3 at = state.ground != null ? state.ground : riding ? stomped.position() : state.press;
				groundImpact(level, self, new Vec3(at.x, groundHeight(level, at.x, at.z, at.y + 0.5D), at.z));
			}
			return true;
		}

		if (state.landing == null) {
			Vec3 back = Vec3.directionFromRotation(0.0F, state.yaw).scale(-FLIP_BACK);
			Vec3 wanted = clearPoint(level, self, state.press.add(0.0D, BALL_CENTER, 0.0D), state.press.add(back).add(0.0D, BALL_CENTER, 0.0D))
					.subtract(0.0D, BALL_CENTER, 0.0D);
			double floor = groundHeight(level, wanted.x, wanted.z, state.press.y + 1.0D);
			state.landing = state.press.y - floor <= MAX_LAND_DROP ? new Vec3(wanted.x, floor, wanted.z) : wanted;
			playSound(level, state.press, self, MainSounds.EVASION1.get(), 1.2F, 1.2F);
		}

		if (tick < LAND_TICK) {
			double u = (tick - PRESS_END) / (double) (LAND_TICK - PRESS_END);
			Vec3 at = state.press.lerp(state.landing, u).add(0.0D, Math.sin(u * Math.PI) * FLIP_ARC, 0.0D);
			performer.place(at);
			return true;
		}

		performer.place(state.landing);
		if (tick == LAND_TICK) {
			level.sendParticles(MainParticles.DUST.get(), state.landing.x, state.landing.y + 0.1D, state.landing.z,
					16, 0.6D, 0.1D, 0.6D, 0.05D);
			playSound(level, state.landing, self, MainSounds.KNOCKBACK_CHARACTER.get(), 1.0F, 1.3F);
		}
		return true;
	}

	private static void moveBall(Performer performer, State state, Vec3 ball) {
		state.ball = ball;
		performer.place(ball.subtract(0.0D, BALL_CENTER, 0.0D));
	}

	private static void groundImpact(ServerLevel level, LivingEntity self, Vec3 at) {
		level.sendParticles(MainParticles.ROCK.get(), at.x, at.y + 0.2D, at.z, 45, 1.6D, 0.3D, 1.6D, 0.35D);
		level.sendParticles(MainParticles.DUST.get(), at.x, at.y + 0.2D, at.z, 70, 2.2D, 0.4D, 2.2D, 0.12D);
		level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5D, at.z, 2, 0.6D, 0.2D, 0.6D, 0.0D);
		NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(at.x, at.y + 0.2D, at.z, 7.0F, COLOR_MAIN, 16), self);
		NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(at.x, at.y + 0.2D, at.z, 11.0F, COLOR_DARK, 20), self);
		playSound(level, at, self, MainSounds.KI_EXPLOSION_IMPACT.get(), 2.2F, 0.8F);
	}

	private static Vec3 pickDirection(ServerLevel level, LivingEntity self, State state, Vec3 previous) {
		RandomSource random = self.getRandom();
		Vec3 center = center(state);
		double contact = contactRadius(state.victim.entity());
		float baseYaw = (float) (Mth.atan2(previous.z, previous.x) * (180.0D / Math.PI));

		Vec3 best = null;
		double bestReach = -1.0D;
		for (int attempt = 0; attempt < 6; attempt++) {
			float turn = 110.0F + random.nextFloat() * 60.0F;
			float yaw = baseYaw + (random.nextBoolean() ? turn : -turn);
			float pitch = -20.0F + random.nextFloat() * 60.0F;
			double cosPitch = Mth.cos(pitch * Mth.DEG_TO_RAD);
			Vec3 dir = new Vec3(Mth.cos(yaw * Mth.DEG_TO_RAD) * cosPitch, Mth.sin(pitch * Mth.DEG_TO_RAD),
					Mth.sin(yaw * Mth.DEG_TO_RAD) * cosPitch);
			double reach = clearReach(level, self, center, dir, BOUNCE_RADIUS + contact);
			if (reach > bestReach) {
				best = dir;
				bestReach = reach;
			}
			if (reach >= BOUNCE_RADIUS + contact - 0.01D) break;
		}
		return best;
	}

	private static double clearReach(ServerLevel level, LivingEntity self, Vec3 from, Vec3 dir, double wanted) {
		Vec3 to = from.add(dir.scale(wanted));
		BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		double reach = hit.getType() == HitResult.Type.MISS ? wanted : hit.getLocation().distanceTo(from) - BALL_RADIUS;
		return Math.max(MIN_BOUNCE_RADIUS, reach);
	}

	private static Vec3 clearPoint(ServerLevel level, LivingEntity self, Vec3 from, Vec3 wanted) {
		BlockHitResult hit = level.clip(new ClipContext(from, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		if (hit.getType() == HitResult.Type.MISS) return wanted;
		Vec3 dir = wanted.subtract(from);
		double length = dir.length();
		if (length < 1.0E-4D) return from;
		return from.add(dir.scale(Math.max(0.0D, hit.getLocation().distanceTo(from) - BALL_RADIUS) / length));
	}

	private static Vec3 clearAbove(ServerLevel level, LivingEntity victim, Vec3 from, double height) {
		for (double h = height; h > 0.0D; h -= 0.5D) {
			if (level.noCollision(victim, victim.getBoundingBox().move(from.add(0.0D, h, 0.0D).subtract(victim.position())))) {
				return from.add(0.0D, h, 0.0D);
			}
		}
		return from;
	}

	private static Vec3 center(State state) {
		LivingEntity victim = state.victim.entity();
		if (state.victim.isPinned()) return state.hover.add(0.0D, victim.getBbHeight() * 0.5D, 0.0D);
		return victim.getBoundingBox().getCenter();
	}

	private static double contactRadius(LivingEntity victim) {
		return victim.getBbWidth() * 0.5D + CONTACT_GAP + BALL_RADIUS * 0.5D;
	}

	private static Vec3 flatten(Vec3 dir, float fallbackYaw) {
		Vec3 flat = new Vec3(dir.x, 0.0D, dir.z);
		if (flat.lengthSqr() < 1.0E-4D) return Vec3.directionFromRotation(0.0F, fallbackYaw);
		return flat.normalize();
	}

	private static Vec3 slerp(Vec3 a, Vec3 b, double t) {
		double dot = Mth.clamp(a.dot(b), -1.0D, 1.0D);
		double angle = Math.acos(dot);
		if (angle < 1.0E-3D) return a.lerp(b, t).normalize();
		double sin = Math.sin(angle);
		if (Math.abs(sin) < 1.0E-3D) {
			Vec3 side = a.cross(new Vec3(0.0D, 1.0D, 0.0D));
			if (side.lengthSqr() < 1.0E-4D) side = new Vec3(1.0D, 0.0D, 0.0D);
			Vec3 mid = side.normalize();
			return t < 0.5D ? slerp(a, mid, t * 2.0D) : slerp(mid, b, t * 2.0D - 1.0D);
		}
		double wa = Math.sin((1.0D - t) * angle) / sin;
		double wb = Math.sin(t * angle) / sin;
		return a.scale(wa).add(b.scale(wb)).normalize();
	}

	private static double smooth(double t) {
		return t * t * (3.0D - 2.0D * t);
	}

	private static double easeOut(double t) {
		double inv = 1.0D - t;
		return 1.0D - inv * inv;
	}

	private static float yawTowards(Vec3 from, Vec3 to) {
		double dx = to.x - from.x;
		double dz = to.z - from.z;
		if (dx * dx + dz * dz < 1.0E-6D) return 0.0F;
		return (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
	}

	private static void playSound(ServerLevel level, Vec3 at, LivingEntity self, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, at.x, at.y, at.z, sound, self.getSoundSource(), volume, pitch);
	}

	private static double groundHeight(ServerLevel level, double x, double z, double fromY) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		int ix = Mth.floor(x);
		int iz = Mth.floor(z);
		int bottom = Math.max(level.getMinBuildHeight(), Mth.floor(fromY) - 64);
		for (int y = Mth.floor(fromY) + 1; y >= bottom; y--) {
			cursor.set(ix, y, iz);
			if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) return y + 1.0D;
		}
		return level.getMinBuildHeight();
	}
}
