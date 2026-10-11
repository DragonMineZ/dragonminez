package com.dragonminez.server.events.players.combat;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AfterimageVfxS2C;
import com.dragonminez.common.network.S2C.ImpactBurstVfxS2C;
import com.dragonminez.common.network.S2C.KiBurstVfxS2C;
import com.dragonminez.common.network.S2C.ShockwaveVfxS2C;
import com.dragonminez.common.network.S2C.TriggerImpactFrameS2C;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class MeteorBurstCombo {

	public static final String TECHNIQUE_ID = "meteor_burst";
	public static final String ANIMATION = "skp.meteor_burst";
	private static final int LEAD = 4;
	public static final int DURATION = LEAD + 197;
	public static final double RANGE = 14.0D;
	public static final int COLOR_MAIN = 0xA64DFF;
	public static final int COLOR_BORDER = 0x6A3BFF;
	public static final int COLOR_OUTLINE = 0x4A2CE0;

	private static final int DASH_START = LEAD + 0;
	private static final int DASH_END = LEAD + 6;
	private static final int PUNCH_TICK = LEAD + 10;
	private static final int LAUNCH_TICK = LEAD + 22;
	private static final int LAUNCH_END = LEAD + 32;
	private static final int RISE_START = LEAD + 26;
	private static final int RISE_END = LEAD + 42;
	private static final int DIVE_START = LEAD + 44;
	private static final int STOMP_TICK = LEAD + 48;
	private static final int RIDE_END = LEAD + 53;
	private static final int FALL_END = LEAD + 59;
	private static final int VANISH_TICK = LEAD + 58;
	private static final int KNEE_TICK = LEAD + 63;
	private static final int HAMMER_TICK = LEAD + 74;
	private static final int SLAM_TICK = LEAD + 77;
	private static final int LEAP_START = LEAD + 83;
	private static final int LEAP_END = LEAD + 93;
	private static final int STEP_START = LEAD + 96;
	private static final int STEP_END = LEAD + 100;
	private static final int CHARGE_START = LEAD + 125;
	private static final int THRUST_TICK = LEAD + 157;
	private static final int BLAST_TICK = LEAD + 175;
	private static final int[] BLAST_VISUAL_TICKS = {LEAD + 175, LEAD + 183, LEAD + 191};
	private static final float[] BLAST_VISUAL_SIZES = {5.0F, 6.5F, 5.5F};
	private static final int WHITEOUT_TICK = LEAD + 187;
	private static final int[] SHOT_TICKS = {LEAD + 159, LEAD + 162, LEAD + 165, LEAD + 168, LEAD + 171, LEAD + 174};

	private static final double VICTIM_GAP = 0.35D;
	private static final double HIT_NUDGE = 0.35D;
	private static final double LAUNCH_HEIGHT = 5.0D;
	private static final double OVER_HEIGHT = 1.2D;
	private static final double STOMP_FALL = 3.0D;
	private static final double KNEE_BUMP = 0.4D;
	private static final double MAX_DROP = 30.0D;
	private static final double LEAP_BACK = 1.8D;
	private static final double LEAP_ARC = 1.5D;
	private static final double FOOT_REACH = 0.55D;
	private static final double HAND_HEIGHT = 0.9D;
	private static final double HAND_SIDE = 0.45D;
	private static final float BALL_START_SIZE = 0.2F;
	private static final float BALL_HAND_SIZE = 0.9F;
	private static final float BALL_PRESS_SIZE = 1.6F;
	private static final double MAX_VICTIM_DISTANCE = 48.0D;
	private static final float PUNCH_SHARE = 0.15F;
	private static final float LAUNCH_SHARE = 0.15F;
	private static final float STOMP_SHARE = 0.2F;
	private static final float KNEE_SHARE = 0.15F;
	private static final float SLAM_SHARE = 0.35F;
	private static final float SHOT_SHARE = 0.1F;
	private static final float BLAST_SHARE = 0.4F;
	private static final float SHOT_SIZE = 0.7F;
	private static final float SHOT_SPEED = 1.2F;

	public interface Performer {
		LivingEntity self();

		LivingEntity target();

		void place(Vec3 position, float yaw);

		boolean isInterrupted();

		float strikeBudget();

		float kiBudget();

		void strikeHit(LivingEntity victim, float amount, boolean lethal);

		void kiHit(LivingEntity victim, float amount, boolean lethal);

		boolean canHitArea(LivingEntity entity);
	}

	public static final class State {
		private HeldVictim victim;
		private float yaw;
		private Vec3 origin;
		private Vec3 base;
		private Vec3 start;
		private Vec3 held;
		private Vec3 apex;
		private Vec3 over;
		private Vec3 chest;
		private Vec3 fallEnd;
		private Vec3 below;
		private Vec3 land;
		private Vec3 ground;
		private Vec3 side;
		private Vec3 foot;
		private KiBlastEntity ball;
		private boolean blasted;

		public LivingEntity victim() {
			return this.victim != null ? this.victim.entity() : null;
		}
	}

	private MeteorBurstCombo() {}

	public static boolean tick(ServerLevel level, Performer performer, State state, int tick) {
		LivingEntity self = performer.self();
		self.fallDistance = 0.0F;

		if (tick < DASH_START) {
			LivingEntity target = performer.target();
			if (!HeldVictim.canTarget(self, target)) return false;
			if (state.origin == null) state.origin = self.position();
			state.yaw = yawTowards(self.position(), target.position());
			performer.place(state.origin, state.yaw);
			return true;
		}

		if (tick == DASH_START) {
			LivingEntity target = performer.target();
			if (!HeldVictim.canTarget(self, target) || target.level() != level || self.distanceTo(target) > RANGE + 6.0D) return false;
			state.yaw = yawTowards(self.position(), target.position());
			state.start = target.position();
			state.held = state.start;
			state.base = state.start.subtract(facing(state.yaw).scale(gap(self, target)));
			if (!level.noCollision(self, self.getBoundingBox().move(state.base.subtract(self.position())))) state.base = self.position();
			state.victim = HeldVictim.canHold(self, target) ? HeldVictim.capture(target, DURATION + 20) : HeldVictim.root(target);
			playSound(level, self.position(), self, MainSounds.ZANZOKEN.get(), 1.5F, 0.9F);
		}

		if (state.base == null) return false;

		if (state.blasted) {
			performer.place(state.foot, state.yaw);
			if (state.victim != null) {
				if (state.victim.entity().isAlive()) {
					state.victim.entity().invulnerableTime = 0;
					state.victim.hold(state.ground, state.yaw + 180.0F);
				} else {
					state.victim.release();
					state.victim = null;
				}
			}
			if (tick == WHITEOUT_TICK) NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.9F, 0.05F, 4, false), self);
			for (int i = 0; i < BLAST_VISUAL_TICKS.length; i++) {
				if (BLAST_VISUAL_TICKS[i] == tick && i > 0) blastVisual(level, state.ground, BLAST_VISUAL_SIZES[i]);
			}
			return true;
		}

		if (state.victim == null || !state.victim.isValid(self, MAX_VICTIM_DISTANCE) || performer.isInterrupted()) return false;

		LivingEntity victim = state.victim.entity();
		Vec3 facing = facing(state.yaw);
		float faceBack = state.yaw + 180.0F;

		if (tick < LAUNCH_TICK) {
			double dash = smooth(Mth.clamp((tick - DASH_START) / (double) (DASH_END - DASH_START), 0.0D, 1.0D));
			performer.place(state.origin.lerp(state.base, dash), state.yaw);
			state.held = state.held.lerp(state.start, 0.35D);
			if (tick == PUNCH_TICK) {
				state.held = state.start.add(facing.scale(HIT_NUDGE));
				strike(level, performer, state, PUNCH_SHARE, 1.6F, MainSounds.GOLPE3.get());
				state.victim.flinch();
			}
			state.victim.hold(state.held, faceBack);
			return true;
		}

		if (tick < DIVE_START) {
			if (tick == LAUNCH_TICK) {
				state.apex = clearAbove(level, victim, state.start, LAUNCH_HEIGHT);
				state.over = state.apex.add(0.0D, victim.getBbHeight() + OVER_HEIGHT, 0.0D);
				strike(level, performer, state, LAUNCH_SHARE, 1.8F, MainSounds.CRITICO2.get());
				state.victim.launched();
				playSound(level, state.start, self, MainSounds.EVASION1.get(), 1.4F, 0.8F);
			}
			double lift = easeOut(Mth.clamp((tick - LAUNCH_TICK) / (double) (LAUNCH_END - LAUNCH_TICK), 0.0D, 1.0D));
			state.held = state.start.lerp(state.apex, lift);
			state.victim.hold(state.held, faceBack);
			double rise = smooth(Mth.clamp((tick - RISE_START) / (double) (RISE_END - RISE_START), 0.0D, 1.0D));
			performer.place(state.base.lerp(state.over, rise), state.yaw);
			return true;
		}

		if (tick < VANISH_TICK) {
			if (tick == DIVE_START) {
				state.chest = state.apex.add(0.0D, victim.getBbHeight() * 0.35D, 0.0D);
				playSound(level, state.over, self, MainSounds.EVASION2.get(), 1.4F, 0.9F);
			}
			if (tick == STOMP_TICK) {
				double floor = groundHeight(level, state.apex.x, state.apex.z, state.apex.y);
				double fall = Math.max(0.0D, Math.min(STOMP_FALL, state.apex.y - (floor + 1.5D)));
				state.fallEnd = state.apex.subtract(0.0D, fall, 0.0D);
				strike(level, performer, state, STOMP_SHARE, 2.2F, MainSounds.CRITICO1.get());
				state.victim.knockDown();
			}
			if (tick < STOMP_TICK) {
				double dive = (tick - DIVE_START) / (double) (STOMP_TICK - DIVE_START);
				state.victim.hold(state.apex, faceBack);
				performer.place(state.over.lerp(state.chest, dive * dive), state.yaw);
				return true;
			}
			double fall = easeOut(Mth.clamp((tick - STOMP_TICK) / (double) (FALL_END - STOMP_TICK), 0.0D, 1.0D));
			state.held = state.apex.lerp(state.fallEnd, fall);
			state.victim.hold(state.held, faceBack);
			if (tick <= RIDE_END) performer.place(state.held.add(0.0D, victim.getBbHeight() * 0.35D, 0.0D), state.yaw);
			if (tick == RIDE_END + 1) NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.7F, 0.05F, 2, false), self);
			return true;
		}

		if (tick < HAMMER_TICK) {
			if (tick == VANISH_TICK) {
				state.below = state.fallEnd.subtract(facing.scale(gap(self, victim))).subtract(0.0D, 0.6D, 0.0D);
				NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
						new float[]{state.yaw}, false), self);
				playSound(level, state.below, self, MainSounds.ZANZOKEN.get(), 1.5F, 1.3F);
			}
			double fall = easeOut(Mth.clamp((tick - STOMP_TICK) / (double) (FALL_END - STOMP_TICK), 0.0D, 1.0D));
			Vec3 at = state.apex.lerp(state.fallEnd, fall);
			if (tick >= KNEE_TICK) {
				double bump = easeOut(Mth.clamp((tick - KNEE_TICK) / 2.0D, 0.0D, 1.0D));
				at = state.fallEnd.add(0.0D, KNEE_BUMP * bump, 0.0D);
			}
			if (tick == KNEE_TICK) {
				strike(level, performer, state, KNEE_SHARE, 1.8F, MainSounds.GOLPE6.get());
				state.victim.flinch();
			}
			state.held = at;
			state.victim.hold(state.held, faceBack);
			performer.place(state.below, state.yaw);
			return true;
		}

		if (tick <= SLAM_TICK) {
			if (tick == HAMMER_TICK) {
				state.land = grounded(level, state.below);
				state.ground = grounded(level, state.held);
				NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.75F, 0.05F, 2, true), self);
				playSound(level, state.held, self, MainSounds.EVASION2.get(), 1.6F, 0.7F);
			}
			double dive = Math.pow((tick - HAMMER_TICK) / (double) (SLAM_TICK - HAMMER_TICK), 2.0D);
			Vec3 from = state.fallEnd.add(0.0D, KNEE_BUMP, 0.0D);
			state.victim.hold(from.lerp(state.ground, dive), faceBack);
			performer.place(state.below.lerp(state.land, dive), state.yaw);
			if (tick == SLAM_TICK) {
				strike(level, performer, state, SLAM_SHARE, 3.0F, MainSounds.CRITICO1.get());
				state.victim.knockOut();
				groundImpact(level, self, state.ground, 5.0F);
				NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), true, 12.0F), self);
			}
			return true;
		}

		state.victim.hold(state.ground, faceBack);

		if (tick < LEAP_START) {
			performer.place(state.land, state.yaw);
			return true;
		}

		if (state.side == null) {
			Vec3 wanted = state.ground.subtract(facing.scale(LEAP_BACK + victim.getBbWidth() * 0.5D));
			state.side = grounded(level, wanted);
			if (Math.abs(state.side.y - state.ground.y) > 3.0D) state.side = new Vec3(wanted.x, state.ground.y, wanted.z);
			Vec3 footWanted = state.ground.subtract(facing.scale(victim.getBbWidth() * 0.5D + FOOT_REACH));
			state.foot = new Vec3(footWanted.x, state.side.y, footWanted.z);
		}

		if (tick <= LEAP_END) {
			double u = (tick - LEAP_START) / (double) (LEAP_END - LEAP_START);
			performer.place(state.land.lerp(state.side, u).add(0.0D, Math.sin(u * Math.PI) * LEAP_ARC, 0.0D), state.yaw);
			if (tick == LEAP_END) playSound(level, state.side, self, MainSounds.KNOCKBACK_CHARACTER.get(), 0.8F, 1.3F);
			return true;
		}

		if (tick < STEP_START) {
			performer.place(state.side, state.yaw);
			return true;
		}

		if (tick <= STEP_END) {
			double u = smooth((tick - STEP_START) / (double) (STEP_END - STEP_START));
			performer.place(state.side.lerp(state.foot, u), state.yaw);
			if (tick == STEP_END) playSound(level, state.ground, self, MainSounds.GOLPE2.get(), 1.0F, 0.7F);
			return true;
		}

		performer.place(state.foot, state.yaw);

		if (tick < THRUST_TICK) {
			if (tick >= CHARGE_START) charge(level, self, state, tick);
			return true;
		}

		if (tick == THRUST_TICK) {
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.8F, 0.05F, 3, true), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), false, 10.0F), self);
			playSound(level, state.ground, self, MainSounds.KI_BEAM_FIRE.get(), 2.0F, 0.6F);
		}

		if (tick < BLAST_TICK) {
			if (state.ball != null) {
				float press = (tick - THRUST_TICK) / (float) (BLAST_TICK - THRUST_TICK);
				state.ball.setSize(Mth.lerp(press, BALL_HAND_SIZE, BALL_PRESS_SIZE));
				Vec3 at = state.ground.add(0.0D, 0.4D, 0.0D);
				state.ball.setPos(at.x, at.y - state.ball.getBbHeight() * 0.5D, at.z);
			}
			victim.invulnerableTime = 0;
			for (int shot : SHOT_TICKS) {
				if (shot == tick) fireSphere(level, performer, state);
			}
			return true;
		}

		blast(level, performer, state);
		return true;
	}

	public static void release(State state) {
		discardBall(state);
		if (state.victim != null) {
			state.victim.release();
			state.victim = null;
		}
		state.origin = null;
		state.base = null;
		state.side = null;
		state.ground = null;
		state.blasted = false;
	}

	private static void strike(ServerLevel level, Performer performer, State state, float share, float burst, SoundEvent sound) {
		LivingEntity self = performer.self();
		LivingEntity target = state.victim.entity();
		target.invulnerableTime = 0;
		performer.strikeHit(target, performer.strikeBudget() * share, false);

		Vec3 center = target.getBoundingBox().getCenter();
		NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(center, facing(state.yaw), burst,
				0xFFFFFF, 0xFFD27F, true, 8 + (int) (burst * 2.0F)), self);
		playSound(level, center, self, sound, 1.8F, 0.85F + self.getRandom().nextFloat() * 0.2F);
	}

	private static void charge(ServerLevel level, LivingEntity self, State state, int tick) {
		Vec3 right = Vec3.directionFromRotation(0.0F, state.yaw + 90.0F);
		Vec3 hand = state.foot.add(0.0D, HAND_HEIGHT, 0.0D).add(right.scale(HAND_SIDE)).subtract(facing(state.yaw).scale(0.15D));
		if (state.ball == null) {
			KiBlastEntity ball = new KiBlastEntity(level, self);
			ball.setupKiBlast(self, 0.0F, 0.0F, COLOR_MAIN, COLOR_BORDER, COLOR_OUTLINE, BALL_START_SIZE, 9999);
			ball.setDetached(true);
			ball.setBlockDestructionEnabled(false);
			state.ball = ball;
		}
		float grow = (tick - CHARGE_START) / (float) (THRUST_TICK - CHARGE_START);
		state.ball.setSize(Mth.lerp(grow, BALL_START_SIZE, BALL_HAND_SIZE));
		state.ball.setPos(hand.x, hand.y - state.ball.getBbHeight() * 0.5D, hand.z);
		if ((tick - CHARGE_START) % 20 == 0) playSound(level, hand, self, MainSounds.KI_CHARGE_LOOP.get(), 1.2F, 1.2F);
	}

	private static void blast(ServerLevel level, Performer performer, State state) {
		LivingEntity self = performer.self();
		LivingEntity victim = state.victim.entity();
		discardBall(state);
		state.blasted = true;

		victim.invulnerableTime = 0;
		performer.kiHit(victim, performer.kiBudget() * BLAST_SHARE, true);
		if (victim.isAlive()) {
			state.victim.hold(state.ground, state.yaw + 180.0F);
		} else {
			state.victim.release();
			state.victim = null;
		}

		blastVisual(level, state.ground, BLAST_VISUAL_SIZES[0]);
		NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.85F, 0.05F, 3, true), self);
		NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), true, 16.0F), self);
		NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(state.ground.x, state.ground.y + 0.2D, state.ground.z,
				10.0F, COLOR_BORDER, 18), self);
		playSound(level, state.ground, self, MainSounds.KI_EXPLOSION_IMPACT.get(), 3.5F, 0.7F);
	}

	private static void fireSphere(ServerLevel level, Performer performer, State state) {
		LivingEntity self = performer.self();
		Vec3 facing = facing(state.yaw);
		Vec3 right = Vec3.directionFromRotation(0.0F, state.yaw + 90.0F);
		Vec3 from = state.foot.add(0.0D, 1.1D, 0.0D).add(facing.scale(0.8D)).add(right.scale(HAND_SIDE * 0.7D));
		Vec3 aim = state.ground.add(0.0D, 0.5D, 0.0D);
		Vec3 dir = aim.subtract(from).normalize();

		KiBlastEntity shot = new KiBlastEntity(level, self);
		shot.setKiRenderType(0);
		shot.setSize(SHOT_SIZE);
		shot.setKiSpeed(SHOT_SPEED);
		shot.setKiDamage(performer.kiBudget() * SHOT_SHARE);
		shot.setColors(COLOR_MAIN, COLOR_BORDER, COLOR_OUTLINE);
		shot.setFiring(true);
		shot.setCastTime(0);
		shot.setMaxLife(20);
		shot.setBlockDestructionEnabled(false);
		shot.setPos(from.x, from.y - SHOT_SIZE * 0.5D, from.z);
		shot.setDeltaMovement(dir.scale(SHOT_SPEED));
		shot.hasImpulse = true;
		level.addFreshEntity(shot);

		playSound(level, from, self, MainSounds.KIBLAST_ATTACK.get(), 1.3F, 0.9F + self.getRandom().nextFloat() * 0.3F);
	}

	private static void blastVisual(ServerLevel level, Vec3 at, float size) {
		KiExplosionVisualEntity visual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), level);
		visual.setupExplosion(COLOR_MAIN, COLOR_BORDER, COLOR_OUTLINE, size);
		visual.moveTo(at.x, at.y + 0.5D, at.z, 0.0F, 0.0F);
		level.addFreshEntity(visual);
	}

	private static void groundImpact(ServerLevel level, LivingEntity self, Vec3 at, float scale) {
		level.sendParticles(MainParticles.ROCK.get(), at.x, at.y + 0.2D, at.z, 40, scale * 0.3D, 0.3D, scale * 0.3D, 0.25D);
		NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(at.x, at.y + 0.2D, at.z, scale, 0xFFE0A0, 12), self);
		playSound(level, at, self, MainSounds.KNOCKBACK_CHARACTER.get(), 2.0F, 0.7F);
	}

	private static void discardBall(State state) {
		if (state.ball != null) {
			state.ball.discard();
			state.ball = null;
		}
	}

	private static Vec3 grounded(ServerLevel level, Vec3 at) {
		double floor = groundHeight(level, at.x, at.z, at.y + 0.5D);
		if (at.y - floor > MAX_DROP) return at;
		return new Vec3(at.x, floor, at.z);
	}

	private static Vec3 clearAbove(ServerLevel level, LivingEntity victim, Vec3 from, double height) {
		for (double h = height; h > 0.0D; h -= 0.5D) {
			if (level.noCollision(victim, victim.getBoundingBox().move(from.add(0.0D, h, 0.0D).subtract(victim.position())))) {
				return from.add(0.0D, h, 0.0D);
			}
		}
		return from;
	}

	private static void playSound(ServerLevel level, Vec3 at, LivingEntity self, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, at.x, at.y, at.z, sound, self.getSoundSource(), volume, pitch);
	}

	private static double gap(LivingEntity self, LivingEntity target) {
		return self.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D + VICTIM_GAP;
	}

	private static Vec3 facing(float yaw) {
		return Vec3.directionFromRotation(0.0F, yaw);
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
