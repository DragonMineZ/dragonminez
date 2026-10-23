package com.dragonminez.server.events.players.combat;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AfterimageVfxS2C;
import com.dragonminez.common.network.S2C.ImpactBurstVfxS2C;
import com.dragonminez.common.network.S2C.KiBladeExtendS2C;
import com.dragonminez.common.network.S2C.KiBurstVfxS2C;
import com.dragonminez.common.network.S2C.RageScreamVfxS2C;
import com.dragonminez.common.network.S2C.ShockwaveVfxS2C;
import com.dragonminez.common.network.S2C.SwordSlashS2C;
import com.dragonminez.common.network.S2C.TriggerImpactFrameS2C;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import org.joml.Vector3f;

public final class StrikeCombos {

	private StrikeCombos() {}

	public static final class MeteorBurstCombo {

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
				ball.setupKiBlast(self, 0.0F, 0.0F, COLOR_MAIN, COLOR_BORDER, COLOR_OUTLINE, BALL_START_SIZE, 1);
				ball.setMaxLife(99999);
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

	public static final class MysticBallCombo {

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

	public static final class DarknessEyeBeamCombo {

		public static final String TECHNIQUE_ID = "darkness_eye_beam";
		public static final String ANIMATION = "skp.darkness_eye_beam";
		private static final int LEAD = 4;
		public static final int DURATION = LEAD + 98;
		public static final double RANGE = 14.0D;
		public static final int COLOR_MAIN = 0xFFD23A;
		public static final int COLOR_BORDER = 0xFF8A00;
		public static final int COLOR_OUTLINE = 0xE84A00;

		private static final int DASH_START = LEAD;
		private static final int DASH_END = LEAD + 3;
		private static final int BACKHAND_TICK = LEAD + 4;
		private static final int GRAB_TICK = LEAD + 8;
		private static final int[] PUNCH_TICKS = {LEAD + 11, LEAD + 15, LEAD + 19};
		private static final int CHOKE_START = LEAD + 22;
		private static final int CHOKE_END = LEAD + 25;
		private static final int THROW_TICK = LEAD + 28;
		private static final int THROW_END = LEAD + 36;
		private static final int STRETCH_START = LEAD + 33;
		private static final int STRETCH_GRAB_TICK = LEAD + 37;
		private static final int RETRACT_START = LEAD + 39;
		private static final int RETRACT_END = LEAD + 44;
		private static final int SWING_END = LEAD + 47;
		private static final int FLING_TICK = LEAD + 50;
		private static final int FLING_END = LEAD + 60;
		private static final int ROAR_TICK = LEAD + 57;
		private static final int GLOW_START = LEAD + 58;
		private static final int BEAM_START = LEAD + 68;
		private static final int BEAM_HIT_INTERVAL = 2;
		private static final int BLAST_TICK = LEAD + 88;
		private static final int[] BLAST_VISUAL_TICKS = {LEAD + 88, LEAD + 92};
		private static final float[] BLAST_VISUAL_SIZES = {4.5F, 6.0F};
		private static final int BEAM_LIFE = BLAST_TICK - BEAM_START + 2;

		private static final double VICTIM_GAP = 0.3D;
		private static final double BACKHAND_PUSH = 0.45D;
		private static final double PUNCH_NUDGE = 0.2D;
		private static final double CHOKE_LIFT = 0.7D;
		private static final double SHOULDER_HEIGHT = 22.0D / 16.0D;
		private static final double SHOULDER_SIDE = 5.0D / 16.0D;
		private static final double ARM_FORWARD = 81.0D / 16.0D;
		private static final double ARM_RISE = 12.0D / 16.0D;
		private static final double GRIP_DROP = 0.75D;
		private static final double SWING_SIDE = 1.4D;
		private static final double SWING_BACK = 0.6D;
		private static final double OVERHEAD_FORWARD = 1.0D;
		private static final double OVERHEAD_HEIGHT = 2.6D;
		private static final double FLING_FORWARD = 9.0D;
		private static final double FLING_HEIGHT = 5.0D;
		private static final double EYE_SIDE = 0.12D;
		private static final double EYE_FORWARD = 0.3D;
		private static final double MAX_VICTIM_DISTANCE = 48.0D;
		private static final float BACKHAND_SHARE = 0.15F;
		private static final float PUNCH_SHARE = 0.1F;
		private static final float THROW_SHARE = 0.15F;
		private static final float STRETCH_SHARE = 0.1F;
		private static final float FLING_SHARE = 0.3F;
		private static final float BEAM_SHARE = 0.04F;
		private static final float BLAST_SHARE = 0.6F;
		private static final float BEAM_SIZE = 0.35F;

		public interface Performer {
			LivingEntity self();

			LivingEntity target();

			void place(Vec3 position, float yaw);

			float strikeBudget();

			float kiBudget();

			void strikeHit(LivingEntity victim, float amount, boolean lethal);

			void kiHit(LivingEntity victim, float amount, boolean lethal);
		}

		public static final class State {
			private HeldVictim victim;
			private float yaw;
			private Vec3 origin;
			private Vec3 base;
			private Vec3 start;
			private Vec3 held;
			private Vec3 front;
			private Vec3 hand;
			private Vec3 far;
			private KiLaserEntity[] beams;
			private boolean blasted;

			public LivingEntity victim() {
				return this.victim != null ? this.victim.entity() : null;
			}
		}

		private DarknessEyeBeamCombo() {}

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
				if (state.origin == null) state.origin = self.position();
				state.yaw = yawTowards(self.position(), target.position());
				state.start = target.position();
				state.held = state.start;
				state.base = state.start.subtract(facing(state.yaw).scale(gap(self, target)));
				if (!level.noCollision(self, self.getBoundingBox().move(state.base.subtract(self.position())))) state.base = self.position();
				state.victim = HeldVictim.canHold(self, target) ? HeldVictim.capture(target, DURATION + 20) : HeldVictim.root(target);
				NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
						new float[]{state.yaw}, false), self);
				playSound(level, self.position(), self, MainSounds.ZANZOKEN.get(), 1.5F, 0.8F);
			}

			if (state.base == null) return false;

			if (state.blasted) {
				performer.place(state.base, state.yaw);
				if (state.victim != null) {
					if (state.victim.entity().isAlive()) {
						state.victim.hold(state.held, state.yaw + 180.0F);
					} else {
						state.victim.release();
						state.victim = null;
					}
				}
				for (int i = 1; i < BLAST_VISUAL_TICKS.length; i++) {
					if (BLAST_VISUAL_TICKS[i] == tick) blastVisual(level, aimPoint(state), BLAST_VISUAL_SIZES[i]);
				}
				return true;
			}

			if (state.victim == null || !state.victim.isValid(self, MAX_VICTIM_DISTANCE)) return false;

			LivingEntity victim = state.victim.entity();
			Vec3 facing = facing(state.yaw);
			Vec3 right = facing(state.yaw + 90.0F);
			float faceBack = state.yaw + 180.0F;

			if (tick < CHOKE_START) {
				double dash = smooth(Mth.clamp((tick - DASH_START) / (double) (DASH_END - DASH_START), 0.0D, 1.0D));
				performer.place(state.origin.lerp(state.base, dash), state.yaw);
				if (state.front == null) state.front = state.start;
				state.held = state.held.lerp(state.front, 0.35D);
				if (tick == BACKHAND_TICK) {
					state.held = state.start.add(right.scale(BACKHAND_PUSH));
					strike(level, performer, state, BACKHAND_SHARE, 2.2F, MainSounds.CRITICO2.get(), right);
					state.victim.flinch();
				}
				if (tick == GRAB_TICK) {
					state.front = state.base.add(facing.scale(gap(self, victim) - 0.1D));
					state.victim.grabbed();
					playSound(level, state.front, self, MainSounds.GOLPE2.get(), 1.0F, 0.7F);
				}
				for (int punch : PUNCH_TICKS) {
					if (punch != tick) continue;
					state.held = state.front.add(facing.scale(PUNCH_NUDGE));
					strike(level, performer, state, PUNCH_SHARE, 1.6F, punchSound(self), facing);
					state.victim.flinch();
				}
				state.victim.hold(state.held, faceBack);
				return true;
			}

			if (tick < THROW_TICK) {
				performer.place(state.base, state.yaw);
				if (tick == CHOKE_START) state.victim.grabbed();
				double lift = smooth(Mth.clamp((tick - CHOKE_START) / (double) (CHOKE_END - CHOKE_START), 0.0D, 1.0D));
				state.held = state.front.add(0.0D, CHOKE_LIFT * lift, 0.0D);
				state.victim.hold(state.held, faceBack);
				if (tick == CHOKE_END) playSound(level, state.held, self, MainSounds.KNOCKBACK_CHARACTER.get(), 1.0F, 0.6F);
				return true;
			}

			if (tick < STRETCH_GRAB_TICK) {
				performer.place(state.base, state.yaw);
				if (tick == THROW_TICK) {
					Vec3 shoulder = state.base.add(0.0D, SHOULDER_HEIGHT, 0.0D).add(right.scale(SHOULDER_SIDE));
					Vec3 tip = clearPoint(level, self, shoulder, shoulder.add(facing.scale(ARM_FORWARD)).add(0.0D, ARM_RISE, 0.0D), victim.getBbWidth() * 0.5D + 0.2D);
					state.hand = tip.subtract(0.0D, victim.getBbHeight() * GRIP_DROP, 0.0D);
					state.front = state.held;
					strike(level, performer, state, THROW_SHARE, 2.4F, MainSounds.CRITICO1.get(), facing);
					state.victim.launched();
					playSound(level, state.held, self, MainSounds.EVASION1.get(), 1.4F, 0.7F);
				}
				double flight = easeOut(Mth.clamp((tick - THROW_TICK) / (double) (THROW_END - THROW_TICK), 0.0D, 1.0D));
				state.held = state.front.lerp(state.hand, flight);
				state.victim.hold(state.held, faceBack);
				if (tick == STRETCH_START) playSound(level, state.base, self, MainSounds.EVASION2.get(), 1.6F, 0.6F);
				return true;
			}

			if (tick < FLING_TICK) {
				performer.place(state.base, state.yaw);
				if (tick == STRETCH_GRAB_TICK) {
					strike(level, performer, state, STRETCH_SHARE, 2.0F, MainSounds.GOLPE6.get(), facing);
					state.victim.grabbed();
					state.front = state.base.add(facing.scale(gap(self, victim))).add(0.0D, CHOKE_LIFT, 0.0D);
				}
				if (tick < RETRACT_START) {
					state.held = state.hand;
				} else if (tick <= RETRACT_END) {
					double pull = smooth((tick - RETRACT_START) / (double) (RETRACT_END - RETRACT_START));
					state.held = state.hand.lerp(state.front, pull);
					if (tick == RETRACT_START) playSound(level, state.hand, self, MainSounds.EVASION1.get(), 1.4F, 1.2F);
				} else if (tick <= SWING_END) {
					double u = smooth((tick - RETRACT_END) / (double) (SWING_END - RETRACT_END));
					Vec3 side = clearPoint(level, self, state.base.add(0.0D, 1.0D, 0.0D),
							state.base.add(right.scale(SWING_SIDE)).subtract(facing.scale(SWING_BACK)).add(0.0D, 1.3D, 0.0D), 0.3D)
							.subtract(0.0D, 1.0D, 0.0D);
					state.held = state.front.lerp(side, u);
					if (tick == SWING_END) {
						state.start = side;
						playSound(level, side, self, MainSounds.STAFF_SPIN.get(), 1.4F, 0.7F);
					}
				} else {
					double u = (tick - SWING_END) / (double) (FLING_TICK - SWING_END);
					Vec3 over = clearPoint(level, self, state.base.add(0.0D, 1.0D, 0.0D),
							state.base.add(facing.scale(OVERHEAD_FORWARD)).add(0.0D, OVERHEAD_HEIGHT + 1.0D, 0.0D), 0.3D)
							.subtract(0.0D, 1.0D, 0.0D);
					Vec3 top = state.base.add(right.scale(SWING_SIDE * 0.5D)).add(0.0D, OVERHEAD_HEIGHT + 0.6D, 0.0D);
					state.held = bezier(state.start, top, over, u * u);
				}
				state.victim.hold(state.held, faceBack);
				return true;
			}

			if (tick < BEAM_START) {
				performer.place(state.base, state.yaw);
				if (tick == FLING_TICK) {
					state.front = state.held;
					Vec3 wanted = state.base.add(facing.scale(FLING_FORWARD)).add(0.0D, FLING_HEIGHT, 0.0D);
					state.far = clearPoint(level, self, state.held.add(0.0D, 1.0D, 0.0D), wanted.add(0.0D, 1.0D, 0.0D),
							victim.getBbWidth() * 0.5D + 0.3D).subtract(0.0D, 1.0D, 0.0D);
					strike(level, performer, state, FLING_SHARE, 3.0F, MainSounds.CRITICO1.get(), facing);
					state.victim.launched();
					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.75F, 0.05F, 2, false), self);
					NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), false, 10.0F), self);
					playSound(level, state.held, self, MainSounds.EVASION2.get(), 1.8F, 0.6F);
				}
				double flight = easeOut(Mth.clamp((tick - FLING_TICK) / (double) (FLING_END - FLING_TICK), 0.0D, 1.0D));
				state.held = state.front.lerp(state.far, flight);
				state.victim.hold(state.held, faceBack);
				if (tick == ROAR_TICK) roar(level, self, state);
				if (tick >= GLOW_START && (tick - GLOW_START) % 2 == 0) eyeGlow(level, state, 0.6F);
				return true;
			}

			performer.place(state.base, state.yaw);
			state.victim.hold(state.held, faceBack);

			if (tick == BEAM_START) fireBeams(level, self, state);
			if (tick < BLAST_TICK) {
				if ((tick - BEAM_START) % BEAM_HIT_INTERVAL == 1) beamHit(level, performer, state);
				eyeGlow(level, state, 1.0F);
				return true;
			}

			blast(level, performer, state);
			return true;
		}

		public static void release(State state) {
			discardBeams(state);
			if (state.victim != null) {
				state.victim.release();
				state.victim = null;
			}
			state.origin = null;
			state.base = null;
			state.front = null;
			state.hand = null;
			state.far = null;
			state.blasted = false;
		}

		private static void strike(ServerLevel level, Performer performer, State state, float share, float burst, SoundEvent sound, Vec3 direction) {
			LivingEntity self = performer.self();
			LivingEntity target = state.victim.entity();
			target.invulnerableTime = 0;
			performer.strikeHit(target, performer.strikeBudget() * share, false);

			Vec3 center = target.getBoundingBox().getCenter();
			NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(center, direction, burst,
					0xFFFFFF, 0xFFD27F, true, 8 + (int) (burst * 2.0F)), self);
			playSound(level, center, self, sound, 1.8F, 0.8F + self.getRandom().nextFloat() * 0.2F);
		}

		private static void roar(ServerLevel level, LivingEntity self, State state) {
			NetworkHandler.sendToTrackingEntityAndSelf(new RageScreamVfxS2C(self.getId(), BEAM_START - ROAR_TICK), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), true, 12.0F), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(state.base.x, state.base.y + 0.2D, state.base.z,
					6.0F, COLOR_BORDER, 16), self);
			level.sendParticles(MainParticles.DUST.get(), state.base.x, state.base.y + 0.1D, state.base.z, 40, 1.8D, 0.1D, 1.8D, 0.08D);
			playSound(level, state.base, self, MainSounds.OOZARU_GROWL_PLAYER.get(), 2.0F, 1.25F);
			playSound(level, state.base, self, MainSounds.KI_EXPLOSION_CHARGE.get(), 1.6F, 0.8F);
		}

		private static void fireBeams(ServerLevel level, LivingEntity self, State state) {
			discardBeams(state);
			Vec3 target = aimPoint(state);
			state.beams = new KiLaserEntity[2];
			for (int i = 0; i < 2; i++) {
				Vec3 eye = eye(state, i == 0 ? 1.0D : -1.0D);
				KiLaserEntity beam = new KiLaserEntity(level, self);
				beam.setupStrike(self, eye, target, 0.0F, 14.0F, BEAM_SIZE, 0.1F, COLOR_MAIN, COLOR_BORDER, COLOR_OUTLINE, BEAM_LIFE);
				state.beams[i] = beam;
			}
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.8F, 0.05F, 3, true), self);
			playSound(level, state.base, self, MainSounds.KI_LASER.get(), 2.5F, 0.8F);
			playSound(level, state.base, self, MainSounds.KI_BEAM_FIRE.get(), 2.0F, 1.3F);
		}

		private static void beamHit(ServerLevel level, Performer performer, State state) {
			LivingEntity self = performer.self();
			LivingEntity victim = state.victim.entity();
			victim.invulnerableTime = 0;
			performer.kiHit(victim, performer.kiBudget() * BEAM_SHARE, false);
			Vec3 at = aimPoint(state);
			NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(at, facing(state.yaw), 2.2F,
					COLOR_MAIN, COLOR_BORDER, false, 8), self);
			level.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.75F, 0.2F), 2.0F), at.x, at.y, at.z, 10, 0.4D, 0.4D, 0.4D, 0.0D);
			playSound(level, at, self, MainSounds.KIBLAST_ATTACK.get(), 1.2F, 1.1F + self.getRandom().nextFloat() * 0.3F);
		}

		private static void blast(ServerLevel level, Performer performer, State state) {
			LivingEntity self = performer.self();
			LivingEntity victim = state.victim.entity();
			discardBeams(state);
			state.blasted = true;

			Vec3 at = aimPoint(state);
			victim.invulnerableTime = 0;
			performer.kiHit(victim, performer.kiBudget() * BLAST_SHARE, true);
			if (victim.isAlive()) {
				state.victim.knockOut();
			} else {
				state.victim.release();
				state.victim = null;
			}

			blastVisual(level, at, BLAST_VISUAL_SIZES[0]);
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.9F, 0.05F, 4, false), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(at.x, at.y, at.z, 9.0F, COLOR_BORDER, 18), self);
			playSound(level, at, self, MainSounds.KI_EXPLOSION_IMPACT.get(), 3.5F, 0.7F);
		}

		private static void eyeGlow(ServerLevel level, State state, float size) {
			for (int i = 0; i < 2; i++) {
				Vec3 eye = eye(state, i == 0 ? 1.0D : -1.0D);
				level.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.3F), size), eye.x, eye.y, eye.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			}
		}

		private static Vec3 eye(State state, double side) {
			Vec3 facing = facing(state.yaw);
			Vec3 right = facing(state.yaw + 90.0F);
			return state.base.add(0.0D, 1.62D, 0.0D).add(facing.scale(EYE_FORWARD)).add(right.scale(EYE_SIDE * side));
		}

		private static Vec3 aimPoint(State state) {
			if (state.victim != null && !state.victim.isPinned()) return state.victim.entity().getBoundingBox().getCenter();
			LivingEntity victim = state.victim != null ? state.victim.entity() : null;
			double half = victim != null ? victim.getBbHeight() * 0.5D : 0.9D;
			return state.held.add(0.0D, half, 0.0D);
		}

		private static void blastVisual(ServerLevel level, Vec3 at, float size) {
			KiExplosionVisualEntity visual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), level);
			visual.setupExplosion(COLOR_MAIN, COLOR_BORDER, COLOR_OUTLINE, size);
			visual.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
			level.addFreshEntity(visual);
		}

		private static void discardBeams(State state) {
			if (state.beams == null) return;
			for (KiLaserEntity beam : state.beams) {
				if (beam != null && !beam.isRemoved()) beam.discard();
			}
			state.beams = null;
		}

		private static SoundEvent punchSound(LivingEntity self) {
			return switch (self.getRandom().nextInt(3)) {
				case 0 -> MainSounds.GOLPE4.get();
				case 1 -> MainSounds.GOLPE5.get();
				default -> MainSounds.GOLPE6.get();
			};
		}

		private static Vec3 clearPoint(ServerLevel level, LivingEntity self, Vec3 from, Vec3 wanted, double margin) {
			BlockHitResult hit = level.clip(new ClipContext(from, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
			if (hit.getType() == HitResult.Type.MISS) return wanted;
			Vec3 dir = wanted.subtract(from);
			double length = dir.length();
			if (length < 1.0E-4D) return from;
			return from.add(dir.scale(Math.max(0.0D, hit.getLocation().distanceTo(from) - margin) / length));
		}

		private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, double t) {
			return a.lerp(b, t).lerp(b.lerp(c, t), t);
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
	}

	public static final class DeathChaserCombo {

		public static final String TECHNIQUE_ID = "death_chaser";
		public static final String ANIMATION = "skp.death_chaser";
		private static final int LEAD = 4;
		public static final int DURATION = LEAD + 68;
		public static final double RANGE = 14.0D;
		public static final int COLOR_MAIN = 0xB57CFF;
		public static final int COLOR_DARK = 0x6A2BD9;

		private static final int POWER_TICK = LEAD + 2;
		private static final int DASH_START = LEAD + 4;
		private static final int DASH_END = LEAD + 6;
		private static final int HOOK_TICK = LEAD + 7;
		private static final int STRAIGHT_TICK = LEAD + 10;
		private static final int SLIDE_END = LEAD + 18;
		private static final int VANISH_TICK = LEAD + 19;
		private static final int KNEE_TICK = LEAD + 21;
		private static final int RISE_TICK = LEAD + 26;
		private static final int RISE_END = LEAD + 34;
		private static final int SECOND_VANISH_TICK = LEAD + 31;
		private static final int APPEAR_ABOVE_TICK = LEAD + 33;
		private static final int HAMMER_TICK = LEAD + 35;
		private static final int SLAM_TICK = LEAD + 39;
		private static final int DIVE_START = LEAD + 41;
		private static final int STOMP_TICK = LEAD + 46;
		private static final int[] GRIND_TICKS = {LEAD + 50, LEAD + 53};
		private static final int CRUSH_TICK = LEAD + 56;
		private static final int LEAP_START = LEAD + 57;
		private static final int LEAP_END = LEAD + 63;

		private static final double VICTIM_GAP = 0.3D;
		private static final double HOOK_NUDGE = 0.3D;
		private static final double KNOCK_DISTANCE = 3.5D;
		private static final double KNEE_BUMP = 0.5D;
		private static final double LAUNCH_HEIGHT = 7.0D;
		private static final double OVER_HEIGHT = 0.8D;
		private static final double STAND_HEIGHT = 0.35D;
		private static final double LEAP_BACK = 2.2D;
		private static final double LEAP_ARC = 1.8D;
		private static final double MAX_DROP = 30.0D;
		private static final double MAX_VICTIM_DISTANCE = 48.0D;
		private static final float HOOK_SHARE = 0.1F;
		private static final float STRAIGHT_SHARE = 0.12F;
		private static final float KNEE_SHARE = 0.12F;
		private static final float RISE_SHARE = 0.13F;
		private static final float HAMMER_SHARE = 0.18F;
		private static final float STOMP_SHARE = 0.15F;
		private static final float GRIND_SHARE = 0.05F;
		private static final float CRUSH_SHARE = 0.1F;

		public interface Performer {
			LivingEntity self();

			LivingEntity target();

			void place(Vec3 position, float yaw);

			float strikeBudget();

			void strikeHit(LivingEntity victim, float amount, boolean lethal);
		}

		public static final class State {
			private HeldVictim victim;
			private float yaw;
			private Vec3 origin;
			private Vec3 base;
			private Vec3 start;
			private Vec3 held;
			private Vec3 knock;
			private Vec3 apex;
			private Vec3 over;
			private Vec3 ground;
			private Vec3 stand;
			private Vec3 land;
			private boolean crushed;

			public LivingEntity victim() {
				return this.victim != null ? this.victim.entity() : null;
			}
		}

		private DeathChaserCombo() {}

		public static boolean tick(ServerLevel level, Performer performer, State state, int tick) {
			LivingEntity self = performer.self();
			self.fallDistance = 0.0F;

			if (tick < DASH_START) {
				LivingEntity target = performer.target();
				if (!HeldVictim.canTarget(self, target)) return false;
				if (state.origin == null) state.origin = self.position();
				state.yaw = yawTowards(self.position(), target.position());
				performer.place(state.origin, state.yaw);
				if (tick == POWER_TICK) {
					NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), true, 8.0F), self);
					NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(self.getX(), self.getY() + 0.1D, self.getZ(), 3.5F, COLOR_MAIN, 10), self);
					playSound(level, self.position(), self, MainSounds.AURA_START.get(), 1.4F, 0.9F);
				}
				return true;
			}

			if (tick == DASH_START) {
				LivingEntity target = performer.target();
				if (!HeldVictim.canTarget(self, target) || target.level() != level || self.distanceTo(target) > RANGE + 6.0D) return false;
				if (state.origin == null) state.origin = self.position();
				state.yaw = yawTowards(self.position(), target.position());
				state.start = target.position();
				state.held = state.start;
				state.base = state.start.subtract(facing(state.yaw).scale(gap(self, target)));
				if (!level.noCollision(self, self.getBoundingBox().move(state.base.subtract(self.position())))) state.base = self.position();
				state.victim = HeldVictim.canHold(self, target) ? HeldVictim.capture(target, DURATION + 20) : HeldVictim.root(target);
				NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
						new float[]{state.yaw}, false), self);
				NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), false, 6.0F), self);
				playSound(level, self.position(), self, MainSounds.ZANZOKEN.get(), 1.5F, 0.9F);
			}

			if (state.base == null) return false;

			if (state.crushed) {
				if (state.victim != null) {
					if (state.victim.entity().isAlive()) {
						state.victim.hold(state.ground, state.yaw + 180.0F);
					} else {
						state.victim.release();
						state.victim = null;
					}
				}
				leap(level, performer, state, tick);
				return true;
			}

			if (state.victim == null || !state.victim.isValid(self, MAX_VICTIM_DISTANCE)) return false;

			LivingEntity victim = state.victim.entity();
			Vec3 facing = facing(state.yaw);
			float faceBack = state.yaw + 180.0F;

			if (tick < VANISH_TICK) {
				double dash = smooth(Mth.clamp((tick - DASH_START) / (double) (DASH_END - DASH_START), 0.0D, 1.0D));
				performer.place(state.origin.lerp(state.base, dash), state.yaw);
				if (tick < STRAIGHT_TICK) {
					state.held = state.held.lerp(state.start, 0.4D);
					if (tick == HOOK_TICK) {
						state.held = state.start.add(facing.scale(HOOK_NUDGE));
						strike(level, performer, state, HOOK_SHARE, 1.8F, MainSounds.GOLPE4.get(), facing);
						state.victim.flinch();
					}
				} else {
					if (tick == STRAIGHT_TICK) {
						state.knock = grounded(level, clearPoint(level, self, state.start.add(0.0D, 1.0D, 0.0D),
								state.start.add(facing.scale(KNOCK_DISTANCE)).add(0.0D, 1.0D, 0.0D), victim.getBbWidth() * 0.5D + 0.2D)
								.subtract(0.0D, 1.0D, 0.0D));
						strike(level, performer, state, STRAIGHT_SHARE, 2.4F, MainSounds.CRITICO2.get(), facing);
						state.victim.knockDown();
						NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.65F, 0.05F, 2, false), self);
						playSound(level, state.start, self, MainSounds.EVASION1.get(), 1.3F, 0.8F);
					}
					double slide = easeOut(Mth.clamp((tick - STRAIGHT_TICK) / (double) (SLIDE_END - STRAIGHT_TICK), 0.0D, 1.0D));
					state.held = state.start.lerp(state.knock, slide).add(0.0D, Math.sin(slide * Math.PI) * 0.6D, 0.0D);
					if (tick == SLIDE_END) {
						level.sendParticles(MainParticles.DUST.get(), state.knock.x, state.knock.y + 0.1D, state.knock.z, 14, 0.5D, 0.1D, 0.5D, 0.04D);
						playSound(level, state.knock, self, MainSounds.KNOCKBACK_CHARACTER.get(), 1.2F, 0.8F);
					}
				}
				state.victim.hold(state.held, faceBack);
				return true;
			}

			if (tick < RISE_TICK) {
				if (tick == VANISH_TICK) {
					Vec3 from = self.position();
					Vec3 wanted = state.knock.subtract(facing.scale(gap(self, victim)));
					state.base = level.noCollision(self, self.getBoundingBox().move(wanted.subtract(from))) ? wanted : from;
					NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{from},
							new float[]{state.yaw}, false), self);
					playSound(level, state.base, self, MainSounds.ZANZOKEN.get(), 1.5F, 1.2F);
				}
				performer.place(state.base, state.yaw);
				state.held = state.knock;
				if (tick == KNEE_TICK) {
					strike(level, performer, state, KNEE_SHARE, 2.2F, MainSounds.GOLPE6.get(), new Vec3(0.0D, 1.0D, 0.0D));
					state.victim.flinch();
				}
				if (tick >= KNEE_TICK) {
					double bump = easeOut(Mth.clamp((tick - KNEE_TICK) / 2.0D, 0.0D, 1.0D));
					state.held = state.knock.add(0.0D, KNEE_BUMP * bump, 0.0D);
				}
				state.victim.hold(state.held, faceBack);
				return true;
			}

			if (tick < HAMMER_TICK) {
				if (tick == RISE_TICK) {
					state.apex = clearAbove(level, victim, state.knock, LAUNCH_HEIGHT);
					state.over = state.apex.add(0.0D, victim.getBbHeight() + OVER_HEIGHT, 0.0D).subtract(facing.scale(0.4D));
					strike(level, performer, state, RISE_SHARE, 2.6F, MainSounds.CRITICO1.get(), new Vec3(0.0D, 1.0D, 0.0D));
					state.victim.launched();
					NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(state.base.x, state.base.y + 0.1D, state.base.z, 4.0F, COLOR_MAIN, 10), self);
					playSound(level, state.knock, self, MainSounds.EVASION2.get(), 1.5F, 0.7F);
				}
				double lift = easeOut(Mth.clamp((tick - RISE_TICK) / (double) (RISE_END - RISE_TICK), 0.0D, 1.0D));
				state.held = state.knock.add(0.0D, KNEE_BUMP, 0.0D).lerp(state.apex, lift);
				state.victim.hold(state.held, faceBack);
				if (tick == SECOND_VANISH_TICK) {
					NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
							new float[]{state.yaw}, false), self);
					playSound(level, state.base, self, MainSounds.ZANZOKEN.get(), 1.5F, 1.0F);
				}
				performer.place(tick >= APPEAR_ABOVE_TICK ? state.over : state.base, state.yaw);
				return true;
			}

			if (tick < STOMP_TICK) {
				if (tick == HAMMER_TICK) {
					state.ground = grounded(level, state.held);
					state.stand = state.ground.add(0.0D, STAND_HEIGHT, 0.0D);
					strike(level, performer, state, HAMMER_SHARE, 3.0F, MainSounds.CRITICO1.get(), new Vec3(0.0D, -1.0D, 0.0D));
					state.victim.knockDown();
					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.75F, 0.05F, 2, true), self);
					NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), false, 8.0F), self);
				}
				double fall = Math.pow(Mth.clamp((tick - HAMMER_TICK) / (double) (SLAM_TICK - HAMMER_TICK), 0.0D, 1.0D), 2.0D);
				state.held = state.apex.lerp(state.ground, fall);
				state.victim.hold(state.held, faceBack);
				if (tick == SLAM_TICK) groundImpact(level, self, state.ground, 5.0F);
				if (tick < DIVE_START) {
					performer.place(state.over, state.yaw);
				} else {
					double dive = Math.pow((tick - DIVE_START) / (double) (STOMP_TICK - DIVE_START), 3.0D);
					performer.place(state.over.lerp(state.stand, dive), state.yaw);
					if (tick == DIVE_START) playSound(level, state.over, self, MainSounds.EVASION2.get(), 1.6F, 0.6F);
				}
				return true;
			}

			performer.place(state.stand, state.yaw);
			state.victim.hold(state.ground, faceBack);

			if (tick == STOMP_TICK) {
				strike(level, performer, state, STOMP_SHARE, 3.0F, MainSounds.ANCHOR_SLAM.get(), new Vec3(0.0D, -1.0D, 0.0D));
				state.victim.knockOut();
				groundImpact(level, self, state.ground, 6.0F);
				NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.8F, 0.05F, 3, true), self);
			}
			for (int grind : GRIND_TICKS) {
				if (grind != tick) continue;
				strike(level, performer, state, GRIND_SHARE, 1.4F, MainSounds.GOLPE5.get(), new Vec3(0.0D, -1.0D, 0.0D));
				level.sendParticles(MainParticles.ROCK.get(), state.ground.x, state.ground.y + 0.2D, state.ground.z, 12, 0.6D, 0.2D, 0.6D, 0.15D);
			}
			if (tick == CRUSH_TICK) crush(level, performer, state);
			return true;
		}

		public static void release(State state) {
			if (state.victim != null) {
				state.victim.release();
				state.victim = null;
			}
			state.origin = null;
			state.base = null;
			state.knock = null;
			state.ground = null;
			state.land = null;
			state.crushed = false;
		}

		private static void strike(ServerLevel level, Performer performer, State state, float share, float burst, SoundEvent sound, Vec3 direction) {
			LivingEntity self = performer.self();
			LivingEntity target = state.victim.entity();
			target.invulnerableTime = 0;
			performer.strikeHit(target, performer.strikeBudget() * share, false);

			Vec3 center = target.getBoundingBox().getCenter();
			NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(center, direction, burst,
					0xFFFFFF, COLOR_MAIN, true, 8 + (int) (burst * 2.0F)), self);
			playSound(level, center, self, sound, 1.8F, 0.85F + self.getRandom().nextFloat() * 0.2F);
		}

		private static void crush(ServerLevel level, Performer performer, State state) {
			LivingEntity self = performer.self();
			LivingEntity victim = state.victim.entity();
			state.crushed = true;
			victim.invulnerableTime = 0;
			performer.strikeHit(victim, performer.strikeBudget() * CRUSH_SHARE, true);

			NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(state.ground, new Vec3(0.0D, -1.0D, 0.0D), 5.5F,
					COLOR_MAIN, COLOR_DARK, false, 18), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.85F, 0.05F, 3, false), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), true, 12.0F), self);
			groundImpact(level, self, state.ground, 8.0F);
			playSound(level, state.ground, self, MainSounds.CRITICO1.get(), 3.0F, 0.7F);

			if (!victim.isAlive()) {
				state.victim.release();
				state.victim = null;
			}
		}

		private static void leap(ServerLevel level, Performer performer, State state, int tick) {
			LivingEntity self = performer.self();
			if (state.land == null) {
				Vec3 wanted = state.ground.subtract(facing(state.yaw).scale(LEAP_BACK));
				state.land = grounded(level, wanted);
				if (Math.abs(state.land.y - state.ground.y) > 3.0D) state.land = new Vec3(wanted.x, state.ground.y, wanted.z);
			}
			if (tick < LEAP_START) {
				performer.place(state.stand, state.yaw);
				return;
			}
			double u = Mth.clamp((tick - LEAP_START) / (double) (LEAP_END - LEAP_START), 0.0D, 1.0D);
			performer.place(state.stand.lerp(state.land, u).add(0.0D, Math.sin(u * Math.PI) * LEAP_ARC, 0.0D), state.yaw);
			if (tick == LEAP_START) playSound(level, state.stand, self, MainSounds.EVASION1.get(), 1.2F, 1.2F);
			if (tick == LEAP_END) {
				level.sendParticles(MainParticles.DUST.get(), state.land.x, state.land.y + 0.1D, state.land.z, 12, 0.5D, 0.1D, 0.5D, 0.04D);
				playSound(level, state.land, self, MainSounds.KNOCKBACK_CHARACTER.get(), 0.8F, 1.3F);
			}
		}

		private static void groundImpact(ServerLevel level, LivingEntity self, Vec3 at, float scale) {
			level.sendParticles(MainParticles.ROCK.get(), at.x, at.y + 0.2D, at.z, 40, scale * 0.3D, 0.3D, scale * 0.3D, 0.25D);
			level.sendParticles(MainParticles.DUST.get(), at.x, at.y + 0.2D, at.z, 50, scale * 0.35D, 0.3D, scale * 0.35D, 0.1D);
			NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(at.x, at.y + 0.2D, at.z, scale, COLOR_MAIN, 14), self);
			playSound(level, at, self, MainSounds.KNOCKBACK_CHARACTER.get(), 2.0F, 0.7F);
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

		private static Vec3 clearPoint(ServerLevel level, LivingEntity self, Vec3 from, Vec3 wanted, double margin) {
			BlockHitResult hit = level.clip(new ClipContext(from, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
			if (hit.getType() == HitResult.Type.MISS) return wanted;
			Vec3 dir = wanted.subtract(from);
			double length = dir.length();
			if (length < 1.0E-4D) return from;
			return from.add(dir.scale(Math.max(0.0D, hit.getLocation().distanceTo(from) - margin) / length));
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

	public static final class AndroidCombo {

		public static final String TECHNIQUE_ID = "android_combo";
		public static final String ANIMATION = "skp.android_combo";
		private static final int LEAD = 4;
		public static final int DURATION = LEAD + 78;
		public static final double RANGE = 14.0D;
		public static final int COLOR_MAIN = 0xFF3B3B;
		public static final int COLOR_BORDER = 0xD10F1F;
		public static final int COLOR_OUTLINE = 0xB0001A;

		private static final int DASH_START = LEAD;
		private static final int DASH_END = LEAD + 3;
		private static final int PALM_TICK = LEAD + 5;
		private static final int HOOK_TICK = LEAD + 8;
		private static final int HAYMAKER_TICK = LEAD + 12;
		private static final int SLIDE_END = LEAD + 18;
		private static final int CHASE_START = LEAD + 16;
		private static final int CHASE_END = LEAD + 19;
		private static final int KNEE_TICK = LEAD + 20;
		private static final int[] JAB_TICKS = {LEAD + 23, LEAD + 26, LEAD + 29};
		private static final int UPPERCUT_TICK = LEAD + 34;
		private static final int FLIGHT_END = LEAD + 44;
		private static final int CHARGE_START = LEAD + 39;
		private static final int SHOUT_TICK = LEAD + 54;
		private static final int THROW_TICK = LEAD + 61;
		private static final int IMPACT_TICK = LEAD + 66;
		private static final int[] BLAST_VISUAL_TICKS = {LEAD + 66, LEAD + 70};
		private static final float[] BLAST_VISUAL_SIZES = {5.0F, 6.5F};

		private static final double VICTIM_GAP = 0.3D;
		private static final double PALM_NUDGE = 0.25D;
		private static final double HIT_NUDGE = 0.2D;
		private static final double KNOCK_DISTANCE = 3.0D;
		private static final double FLOAT_FORWARD = 3.5D;
		private static final double FLOAT_HEIGHT = 2.5D;
		private static final double CHEST_HEIGHT = 1.05D;
		private static final double CHEST_FORWARD = 0.4D;
		private static final float BALL_START_SIZE = 0.2F;
		private static final float BALL_CHEST_SIZE = 1.8F;
		private static final double MAX_DROP = 30.0D;
		private static final double MAX_VICTIM_DISTANCE = 48.0D;
		private static final float PALM_SHARE = 0.1F;
		private static final float HOOK_SHARE = 0.12F;
		private static final float HAYMAKER_SHARE = 0.18F;
		private static final float KNEE_SHARE = 0.1F;
		private static final float JAB_SHARE = 0.06F;
		private static final float UPPERCUT_SHARE = 0.32F;
		private static final float BOMB_SHARE = 1.0F;

		public interface Performer {
			LivingEntity self();

			LivingEntity target();

			void place(Vec3 position, float yaw);

			float strikeBudget();

			float kiBudget();

			void strikeHit(LivingEntity victim, float amount, boolean lethal);

			void kiHit(LivingEntity victim, float amount, boolean lethal);
		}

		public static final class State {
			private HeldVictim victim;
			private float yaw;
			private Vec3 origin;
			private Vec3 base;
			private Vec3 chase;
			private Vec3 start;
			private Vec3 held;
			private Vec3 knock;
			private Vec3 hover;
			private Vec3 throwFrom;
			private KiBlastEntity ball;
			private boolean blasted;

			public LivingEntity victim() {
				return this.victim != null ? this.victim.entity() : null;
			}
		}

		private AndroidCombo() {}

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
				if (state.origin == null) state.origin = self.position();
				state.yaw = yawTowards(self.position(), target.position());
				state.start = target.position();
				state.held = state.start;
				state.base = state.start.subtract(facing(state.yaw).scale(gap(self, target)));
				if (!level.noCollision(self, self.getBoundingBox().move(state.base.subtract(self.position())))) state.base = self.position();
				state.victim = HeldVictim.canHold(self, target) ? HeldVictim.capture(target, DURATION + 20) : HeldVictim.root(target);
				NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
						new float[]{state.yaw}, false), self);
				playSound(level, self.position(), self, MainSounds.ZANZOKEN.get(), 1.5F, 0.9F);
			}

			if (state.base == null) return false;

			if (state.blasted) {
				performer.place(state.chase, state.yaw);
				if (state.victim != null) {
					if (state.victim.entity().isAlive()) {
						state.victim.hold(state.hover, state.yaw + 180.0F);
					} else {
						state.victim.release();
						state.victim = null;
					}
				}
				for (int i = 1; i < BLAST_VISUAL_TICKS.length; i++) {
					if (BLAST_VISUAL_TICKS[i] == tick) blastVisual(level, aimPoint(state), BLAST_VISUAL_SIZES[i]);
				}
				return true;
			}

			if (state.victim == null || !state.victim.isValid(self, MAX_VICTIM_DISTANCE)) return false;

			LivingEntity victim = state.victim.entity();
			Vec3 facing = facing(state.yaw);
			float faceBack = state.yaw + 180.0F;

			if (tick < CHASE_START) {
				double dash = smooth(Mth.clamp((tick - DASH_START) / (double) (DASH_END - DASH_START), 0.0D, 1.0D));
				performer.place(state.origin.lerp(state.base, dash), state.yaw);
				if (tick < HAYMAKER_TICK) {
					state.held = state.held.lerp(state.start, 0.4D);
					if (tick == PALM_TICK) {
						state.held = state.start.add(facing.scale(PALM_NUDGE));
						strike(level, performer, state, PALM_SHARE, 1.8F, MainSounds.GOLPE3.get(), facing);
						state.victim.flinch();
					}
					if (tick == HOOK_TICK) {
						state.held = state.start.add(facing.scale(PALM_NUDGE)).add(facing(state.yaw + 90.0F).scale(HIT_NUDGE));
						strike(level, performer, state, HOOK_SHARE, 2.0F, MainSounds.GOLPE5.get(), facing(state.yaw + 90.0F));
						state.victim.flinch();
					}
				} else {
					if (tick == HAYMAKER_TICK) {
						state.knock = grounded(level, clearPoint(level, self, state.start.add(0.0D, 1.0D, 0.0D),
								state.start.add(facing.scale(KNOCK_DISTANCE)).add(0.0D, 1.0D, 0.0D), victim.getBbWidth() * 0.5D + 0.2D)
								.subtract(0.0D, 1.0D, 0.0D));
						strike(level, performer, state, HAYMAKER_SHARE, 2.6F, MainSounds.CRITICO2.get(), facing);
						state.victim.launched();
						NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.65F, 0.05F, 2, false), self);
						playSound(level, state.start, self, MainSounds.EVASION1.get(), 1.3F, 0.8F);
					}
					slide(state, tick);
				}
				state.victim.hold(state.held, faceBack);
				return true;
			}

			if (tick < UPPERCUT_TICK) {
				if (tick == CHASE_START) {
					Vec3 wanted = state.knock.subtract(facing.scale(gap(self, victim)));
					state.chase = level.noCollision(self, self.getBoundingBox().move(wanted.subtract(self.position()))) ? wanted : self.position();
					NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), false, 6.0F), self);
					playSound(level, self.position(), self, MainSounds.EVASION2.get(), 1.4F, 1.1F);
				}
				double chase = smooth(Mth.clamp((tick - CHASE_START) / (double) (CHASE_END - CHASE_START), 0.0D, 1.0D));
				performer.place(state.base.lerp(state.chase, chase), state.yaw);
				if (tick <= SLIDE_END) {
					slide(state, tick);
				} else {
					state.held = state.held.lerp(state.knock, 0.4D);
				}
				if (tick == KNEE_TICK) {
					state.held = state.knock.add(0.0D, 0.4D, 0.0D);
					strike(level, performer, state, KNEE_SHARE, 2.0F, MainSounds.GOLPE6.get(), new Vec3(0.0D, 1.0D, 0.0D));
					state.victim.flinch();
				}
				for (int jab : JAB_TICKS) {
					if (jab != tick) continue;
					state.held = state.knock.add(facing.scale(HIT_NUDGE));
					strike(level, performer, state, JAB_SHARE, 1.5F, punchSound(self), facing);
					state.victim.flinch();
				}
				state.victim.hold(state.held, faceBack);
				return true;
			}

			performer.place(state.chase, state.yaw);

			if (tick == UPPERCUT_TICK) {
				Vec3 wanted = state.knock.add(facing.scale(FLOAT_FORWARD)).add(0.0D, FLOAT_HEIGHT, 0.0D);
				state.hover = clearPoint(level, self, state.knock.add(0.0D, 1.0D, 0.0D), wanted.add(0.0D, 1.0D, 0.0D),
						victim.getBbWidth() * 0.5D + 0.3D).subtract(0.0D, 1.0D, 0.0D);
				state.start = state.held;
				strike(level, performer, state, UPPERCUT_SHARE, 3.0F, MainSounds.CRITICO1.get(), new Vec3(0.0D, 1.0D, 0.0D));
				state.victim.launched();
				NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.75F, 0.05F, 2, true), self);
				NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(state.chase.x, state.chase.y + 0.1D, state.chase.z, 4.0F, 0xFFE0A0, 10), self);
			}
			double flight = easeOut(Mth.clamp((tick - UPPERCUT_TICK) / (double) (FLIGHT_END - UPPERCUT_TICK), 0.0D, 1.0D));
			state.held = state.start.lerp(state.hover, flight);
			state.victim.hold(state.held, faceBack);

			if (tick >= CHARGE_START && tick < THROW_TICK) charge(level, self, state, tick);
			if (tick == SHOUT_TICK) {
				NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), true, 10.0F), self);
				NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(state.chase.x, state.chase.y + 0.1D, state.chase.z, 5.0F, COLOR_MAIN, 14), self);
				playSound(level, state.chase, self, MainSounds.KI_EXPLOSION_CHARGE.get(), 1.8F, 0.9F);
			}
			if (tick >= THROW_TICK && tick < IMPACT_TICK) throwBall(level, self, state, tick);
			if (tick == IMPACT_TICK) blast(level, performer, state);
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
			state.chase = null;
			state.knock = null;
			state.hover = null;
			state.blasted = false;
		}

		private static void slide(State state, int tick) {
			double slide = easeOut(Mth.clamp((tick - HAYMAKER_TICK) / (double) (SLIDE_END - HAYMAKER_TICK), 0.0D, 1.0D));
			state.held = state.start.lerp(state.knock, slide).add(0.0D, Math.sin(slide * Math.PI) * 0.8D, 0.0D);
		}

		private static void strike(ServerLevel level, Performer performer, State state, float share, float burst, SoundEvent sound, Vec3 direction) {
			LivingEntity self = performer.self();
			LivingEntity target = state.victim.entity();
			target.invulnerableTime = 0;
			performer.strikeHit(target, performer.strikeBudget() * share, false);

			Vec3 center = target.getBoundingBox().getCenter();
			NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(center, direction, burst,
					0xFFFFFF, 0xFFD27F, true, 8 + (int) (burst * 2.0F)), self);
			playSound(level, center, self, sound, 1.8F, 0.85F + self.getRandom().nextFloat() * 0.2F);
		}

		private static void charge(ServerLevel level, LivingEntity self, State state, int tick) {
			Vec3 facing = facing(state.yaw);
			Vec3 chest = state.chase.add(0.0D, CHEST_HEIGHT, 0.0D);
			if (state.ball == null) {
				KiBlastEntity ball = new KiBlastEntity(level, self);
				ball.setupKiBlast(self, 0.0F, 0.0F, COLOR_MAIN, COLOR_BORDER, COLOR_OUTLINE, BALL_START_SIZE, 1);
				ball.setMaxLife(99999);
				ball.setDetached(true);
				ball.setBlockDestructionEnabled(false);
				state.ball = ball;
			}
			float grow = Mth.clamp((tick - CHARGE_START) / (float) (THROW_TICK - CHARGE_START), 0.0F, 1.0F);
			float size = Mth.lerp((float) easeOut(grow), BALL_START_SIZE, BALL_CHEST_SIZE);
			Vec3 at = chest.add(facing.scale(CHEST_FORWARD + size * 0.5D));
			if ((tick - CHARGE_START) % 20 == 0) playSound(level, at, self, MainSounds.KI_CHARGE_LOOP.get(), 1.4F, 1.1F);
			if ((tick - CHARGE_START) % 3 == 0) {
				double spread = size * 0.9D + 0.6D;
				level.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.25F, 0.25F), 1.4F), at.x, at.y, at.z,
						6, spread, spread, spread, 0.0D);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 4, size * 0.6D, size * 0.6D, size * 0.6D, 0.15D);
			}
			state.ball.setSize(size);
			state.ball.setPos(at.x, at.y - state.ball.getBbHeight() * 0.5D, at.z);
			state.throwFrom = at;
		}

		private static void throwBall(ServerLevel level, LivingEntity self, State state, int tick) {
			if (state.ball == null) return;
			if (tick == THROW_TICK) {
				NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), false, 10.0F), self);
				playSound(level, state.throwFrom, self, MainSounds.KI_BEAM_FIRE.get(), 2.0F, 0.8F);
			}
			double u = (tick - THROW_TICK + 1) / (double) (IMPACT_TICK - THROW_TICK);
			Vec3 at = state.throwFrom.lerp(aimPoint(state), u);
			state.ball.setPos(at.x, at.y - state.ball.getBbHeight() * 0.5D, at.z);
		}

		private static void blast(ServerLevel level, Performer performer, State state) {
			LivingEntity self = performer.self();
			LivingEntity victim = state.victim.entity();
			Vec3 at = aimPoint(state);
			discardBall(state);
			state.blasted = true;

			victim.invulnerableTime = 0;
			performer.kiHit(victim, performer.kiBudget() * BOMB_SHARE, true);
			if (victim.isAlive()) {
				state.victim.knockOut();
			} else {
				state.victim.release();
				state.victim = null;
			}

			blastVisual(level, at, BLAST_VISUAL_SIZES[0]);
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.9F, 0.05F, 4, false), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(at.x, at.y, at.z, 9.0F, COLOR_BORDER, 18), self);
			playSound(level, at, self, MainSounds.KI_EXPLOSION_IMPACT.get(), 3.5F, 0.7F);
		}

		private static Vec3 aimPoint(State state) {
			LivingEntity victim = state.victim != null ? state.victim.entity() : null;
			if (victim != null && !state.victim.isPinned()) return victim.getBoundingBox().getCenter();
			double half = victim != null ? victim.getBbHeight() * 0.5D : 0.9D;
			Vec3 feet = state.hover != null ? state.hover : state.held;
			return feet.add(0.0D, half, 0.0D);
		}

		private static void blastVisual(ServerLevel level, Vec3 at, float size) {
			KiExplosionVisualEntity visual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), level);
			visual.setupExplosion(COLOR_MAIN, COLOR_BORDER, COLOR_OUTLINE, size);
			visual.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
			level.addFreshEntity(visual);
		}

		private static void discardBall(State state) {
			if (state.ball != null) {
				state.ball.discard();
				state.ball = null;
			}
		}

		private static SoundEvent punchSound(LivingEntity self) {
			return switch (self.getRandom().nextInt(3)) {
				case 0 -> MainSounds.GOLPE4.get();
				case 1 -> MainSounds.GOLPE5.get();
				default -> MainSounds.GOLPE6.get();
			};
		}

		private static Vec3 grounded(ServerLevel level, Vec3 at) {
			double floor = groundHeight(level, at.x, at.z, at.y + 0.5D);
			if (at.y - floor > MAX_DROP) return at;
			return new Vec3(at.x, floor, at.z);
		}

		private static Vec3 clearPoint(ServerLevel level, LivingEntity self, Vec3 from, Vec3 wanted, double margin) {
			BlockHitResult hit = level.clip(new ClipContext(from, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
			if (hit.getType() == HitResult.Type.MISS) return wanted;
			Vec3 dir = wanted.subtract(from);
			double length = dir.length();
			if (length < 1.0E-4D) return from;
			return from.add(dir.scale(Math.max(0.0D, hit.getLocation().distanceTo(from) - margin) / length));
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

	public static final class BeamSwordSlashCombo {

		public static final String TECHNIQUE_ID = "beam_sword_slash";
		public static final String ANIMATION = "skp.beam_sword_slash";
		private static final int LEAD = 4;
		public static final int DURATION = LEAD + 108;
		public static final double RANGE = 14.0D;

		private static final int DASH_START = LEAD;
		private static final int DASH_END = LEAD + 3;
		private static final int KICK_TICK = LEAD + 4;
		private static final int KNEE_TICK = LEAD + 8;
		private static final int PUNCH_TICK = LEAD + 12;
		private static final int SLIDE_END = LEAD + 16;
		private static final int LAUNCH_TICK = LEAD + 17;
		private static final int LAUNCH_END = LEAD + 25;
		private static final int LEAP_START = LEAD + 19;
		private static final int AIR_HIT_TICK = LEAD + 25;
		private static final int AIR_RISE_END = LEAD + 31;
		private static final int VANISH_TICK = LEAD + 29;
		private static final int APPEAR_TICK = LEAD + 30;
		private static final int HAMMER_TICK = LEAD + 32;
		private static final int SLAM_TICK = LEAD + 37;
		private static final int HOVER_END = LEAD + 41;
		private static final int CHARGE_START = LEAD + 40;
		private static final int SUMMON_TICK = LEAD + 44;
		private static final int THRUST_TICK = LEAD + 58;
		private static final int[] SKEWER_TICKS = {LEAD + 60, LEAD + 65, LEAD + 70};
		private static final int SWEEP_TICK = LEAD + 81;
		private static final int DROP_START = LEAD + 83;
		private static final int DROP_END = LEAD + 88;
		private static final int STAND_TICK = LEAD + 86;
		private static final int DASH2_START = LEAD + 90;
		private static final int DASH2_END = LEAD + 94;
		private static final int SLASH_TICK = LEAD + 99;

		private static final double VICTIM_GAP = 0.35D;
		private static final double KICK_NUDGE = 0.3D;
		private static final double KNEE_BUMP = 0.4D;
		private static final double KNOCK_DISTANCE = 2.0D;
		private static final double LAUNCH_HEIGHT = 6.0D;
		private static final double AIR_RISE = 2.5D;
		private static final double OVER_HEIGHT = 0.8D;
		private static final double SHOULDER_SIDE = 5.0D / 16.0D;
		private static final double SHOULDER_UP = 17.5D / 16.0D;
		private static final double SHOULDER_FORWARD = 13.0D / 16.0D;
		private static final double THRUST_FORWARD = 0.778D;
		private static final double THRUST_DOWN = 0.628D;
		private static final double THRUST_LENGTH = 5.5D;
		private static final double BLADE_FIST_OFFSET = 0.5D;
		private static final double BLADE_PIERCE = 0.3D;
		private static final double LYING_AIM_HEIGHT = 0.4D;
		private static final double SLASH_REACH_SHARE = 0.8D;
		private static final double DASH_DISTANCE = 2.5D;
		private static final double HAND_SIDE = 0.4D;
		private static final double HAND_UP = 0.75D;
		private static final double MAX_DROP = 30.0D;
		private static final double MAX_VICTIM_DISTANCE = 48.0D;
		private static final double FINAL_PUSH = 1.8D;
		private static final double FINAL_LIFT = 0.7D;
		private static final float KICK_SHARE = 0.08F;
		private static final float KNEE_SHARE = 0.08F;
		private static final float PUNCH_SHARE = 0.08F;
		private static final float LAUNCH_SHARE = 0.1F;
		private static final float AIR_HIT_SHARE = 0.08F;
		private static final float HAMMER_SHARE = 0.1F;
		private static final float SKEWER_SHARE = 0.06F;
		private static final float SWEEP_SHARE = 0.08F;
		private static final float SLASH_SHARE = 0.22F;

		public interface Performer {
			LivingEntity self();

			LivingEntity target();

			void place(Vec3 position, float yaw);

			float strikeBudget();

			int bladeColor();

			float modelScale();

			void strikeHit(LivingEntity victim, float amount, boolean lethal);
		}

		public static final class State {
			private HeldVictim victim;
			private LivingEntity self;
			private float yaw;
			private Vec3 origin;
			private Vec3 base;
			private Vec3 start;
			private Vec3 held;
			private Vec3 knock;
			private Vec3 apex;
			private Vec3 apex2;
			private Vec3 airPos;
			private Vec3 over;
			private Vec3 ground;
			private Vec3 hover;
			private Vec3 land;
			private Vec3 strikePos;
			private float scale = 1.0F;
			private double tipDistance;
			private boolean bladeSummoned;
			private boolean finished;

			public LivingEntity victim() {
				return this.victim != null ? this.victim.entity() : null;
			}
		}

		private BeamSwordSlashCombo() {}

		public static boolean tick(ServerLevel level, Performer performer, State state, int tick) {
			LivingEntity self = performer.self();
			self.fallDistance = 0.0F;
			state.self = self;

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
				if (state.origin == null) state.origin = self.position();
				state.yaw = yawTowards(self.position(), target.position());
				state.start = target.position();
				state.held = state.start;
				state.base = state.start.subtract(facing(state.yaw).scale(gap(self, target)));
				if (!level.noCollision(self, self.getBoundingBox().move(state.base.subtract(self.position())))) state.base = self.position();
				state.victim = HeldVictim.canHold(self, target) ? HeldVictim.capture(target, DURATION + 20) : HeldVictim.root(target);
				state.scale = Math.max(0.2F, performer.modelScale());
				NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
						new float[]{state.yaw}, false), self);
				playSound(level, self.position(), self, MainSounds.ZANZOKEN.get(), 1.5F, 0.9F);
			}

			if (state.base == null) return false;

			if (state.finished) {
				performer.place(state.strikePos, state.yaw);
				return true;
			}

			if (state.victim == null || !state.victim.isValid(self, MAX_VICTIM_DISTANCE)) return false;

			LivingEntity victim = state.victim.entity();
			Vec3 facing = facing(state.yaw);
			float faceBack = state.yaw + 180.0F;

			if (tick < LAUNCH_TICK) {
				double dash = smooth(Mth.clamp((tick - DASH_START) / (double) (DASH_END - DASH_START), 0.0D, 1.0D));
				Vec3 dashed = state.origin.lerp(state.base, dash);
				if (tick < PUNCH_TICK) {
					state.held = state.held.lerp(state.start, 0.4D);
					if (tick == KICK_TICK) {
						state.held = state.start.add(facing.scale(KICK_NUDGE));
						hit(level, performer, state, KICK_SHARE, victim.getBoundingBox().getCenter(), facing, 1.8F, MainSounds.GOLPE4.get());
						state.victim.flinch();
					}
					if (tick == KNEE_TICK) {
						state.held = state.start.add(0.0D, KNEE_BUMP, 0.0D);
						hit(level, performer, state, KNEE_SHARE, victim.getBoundingBox().getCenter(), new Vec3(0.0D, 1.0D, 0.0D), 2.0F, MainSounds.GOLPE6.get());
						state.victim.flinch();
					}
					performer.place(dashed, state.yaw);
				} else {
					if (tick == PUNCH_TICK) {
						state.knock = grounded(level, clearPoint(level, self, state.start.add(0.0D, 1.0D, 0.0D),
								state.start.add(facing.scale(KNOCK_DISTANCE)).add(0.0D, 1.0D, 0.0D), victim.getBbWidth() * 0.5D + 0.2D)
								.subtract(0.0D, 1.0D, 0.0D));
						hit(level, performer, state, PUNCH_SHARE, victim.getBoundingBox().getCenter(), facing, 2.2F, MainSounds.CRITICO2.get());
						state.victim.flinch();
					}
					double slide = easeOut(Mth.clamp((tick - PUNCH_TICK) / (double) (SLIDE_END - PUNCH_TICK), 0.0D, 1.0D));
					state.held = state.start.lerp(state.knock, slide);
					Vec3 follow = state.held.subtract(facing.scale(gap(self, victim)));
					performer.place(dashed.lerp(follow, slide), state.yaw);
				}
				state.victim.hold(state.held, faceBack);
				return true;
			}

			if (tick < HAMMER_TICK) {
				if (tick == LAUNCH_TICK) {
					state.base = self.position();
					state.apex = clearAbove(level, victim, state.knock, LAUNCH_HEIGHT);
					state.apex2 = clearAbove(level, victim, state.knock, LAUNCH_HEIGHT + AIR_RISE);
					state.airPos = state.apex.subtract(facing.scale(gap(self, victim))).subtract(0.0D, 0.5D, 0.0D);
					state.over = state.apex2.add(0.0D, victim.getBbHeight() + OVER_HEIGHT, 0.0D).subtract(facing.scale(0.3D));
					state.ground = grounded(level, state.apex2);
					state.hover = hoverPoint(state, victim);
					hit(level, performer, state, LAUNCH_SHARE, victim.getBoundingBox().getCenter(), new Vec3(0.0D, 1.0D, 0.0D), 2.6F, MainSounds.CRITICO1.get());
					state.victim.launched();
					NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(state.base.x, state.base.y + 0.1D, state.base.z, 4.0F, 0xFFE0A0, 10), self);
					playSound(level, state.knock, self, MainSounds.EVASION2.get(), 1.5F, 0.7F);
				}
				if (tick <= LAUNCH_END) {
					double lift = easeOut(Mth.clamp((tick - LAUNCH_TICK) / (double) (LAUNCH_END - LAUNCH_TICK), 0.0D, 1.0D));
					state.held = state.knock.lerp(state.apex, lift);
				} else {
					double lift = easeOut(Mth.clamp((tick - AIR_HIT_TICK) / (double) (AIR_RISE_END - AIR_HIT_TICK), 0.0D, 1.0D));
					state.held = state.apex.lerp(state.apex2, lift);
				}
				if (tick == AIR_HIT_TICK) {
					hit(level, performer, state, AIR_HIT_SHARE, victim.getBoundingBox().getCenter(), new Vec3(0.0D, 1.0D, 0.0D), 2.4F, MainSounds.CRITICO2.get());
					state.victim.launched();
				}
				state.victim.hold(state.held, faceBack);

				if (tick < LEAP_START) {
					performer.place(state.base, state.yaw);
				} else if (tick <= AIR_HIT_TICK) {
					double leap = smooth((tick - LEAP_START) / (double) (AIR_HIT_TICK - LEAP_START));
					performer.place(state.base.lerp(state.airPos, leap), state.yaw);
					if (tick == LEAP_START) playSound(level, state.base, self, MainSounds.EVASION1.get(), 1.4F, 1.0F);
				} else if (tick < APPEAR_TICK) {
					performer.place(state.airPos, state.yaw);
				} else {
					performer.place(state.over, state.yaw);
				}
				if (tick == VANISH_TICK) {
					NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
							new float[]{state.yaw}, false), self);
					playSound(level, state.airPos, self, MainSounds.ZANZOKEN.get(), 1.5F, 1.1F);
				}
				return true;
			}

			if (tick < THRUST_TICK) {
				if (tick == HAMMER_TICK) {
					hit(level, performer, state, HAMMER_SHARE, victim.getBoundingBox().getCenter(), new Vec3(0.0D, -1.0D, 0.0D), 3.0F, MainSounds.CRITICO1.get());
					state.victim.knockDown();
					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.75F, 0.05F, 2, true), self);
				}
				double fall = Math.pow(Mth.clamp((tick - HAMMER_TICK) / (double) (SLAM_TICK - HAMMER_TICK), 0.0D, 1.0D), 2.0D);
				state.held = state.apex2.lerp(state.ground, fall);
				state.victim.hold(state.held, faceBack);
				if (tick == SLAM_TICK) groundImpact(level, self, state.ground, 5.0F);

				double drift = smooth(Mth.clamp((tick - HAMMER_TICK) / (double) (HOVER_END - HAMMER_TICK), 0.0D, 1.0D));
				performer.place(state.over.lerp(state.hover, drift), state.yaw);

				if (tick >= CHARGE_START) charge(level, performer, state, tick);
				if (tick == SUMMON_TICK) {
					state.bladeSummoned = true;
					NetworkHandler.sendToTrackingEntityAndSelf(KiBladeExtendS2C.summon(self.getId(), DURATION - SUMMON_TICK + 2), self);
					NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), false, 6.0F), self);
					playSound(level, state.hover, self, MainSounds.SWORD_OUT.get(), 1.6F, 1.0F);
				}
				return true;
			}

			if (tick < DROP_START) {
				performer.place(state.hover, state.yaw);
				state.victim.hold(state.ground, faceBack);
				Vec3 center = state.ground.add(0.0D, aimHeight(victim), 0.0D);
				if (tick == THRUST_TICK) {
					double reach = shoulder(state.hover, state.yaw, state.scale).distanceTo(center) / state.scale - BLADE_FIST_OFFSET + BLADE_PIERCE;
					state.tipDistance = (reach + BLADE_FIST_OFFSET) * state.scale;
					NetworkHandler.sendToTrackingEntityAndSelf(KiBladeExtendS2C.extend(self.getId(), (float) reach, DURATION - THRUST_TICK + 2), self);
					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.75F, 0.05F, 2, true), self);
					playSound(level, state.hover, self, MainSounds.KI_BEAM_FIRE.get(), 2.0F, 1.4F);
				}
				for (int skewer : SKEWER_TICKS) {
					if (skewer == tick) hit(level, performer, state, SKEWER_SHARE, center, new Vec3(0.0D, -1.0D, 0.0D), 2.0F, MainSounds.GOLPE6.get());
				}
				if (tick == SWEEP_TICK) {
					hit(level, performer, state, SWEEP_SHARE, center, new Vec3(0.0D, 1.0D, 0.0D), 3.0F, MainSounds.SWORD_SLASH.get());
					NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), false, 8.0F), self);
				}
				return true;
			}

			if (tick < SLASH_TICK) {
				if (tick == DROP_START) {
					double slashReach = state.tipDistance * SLASH_REACH_SHARE;
					Vec3 wanted = state.ground.subtract(facing.scale(slashReach + DASH_DISTANCE));
					state.land = grounded(level, wanted);
					if (Math.abs(state.land.y - state.ground.y) > 3.0D) state.land = new Vec3(wanted.x, state.ground.y, wanted.z);
					Vec3 strikeWanted = state.ground.subtract(facing.scale(Math.max(gap(self, victim), slashReach)));
					state.strikePos = new Vec3(strikeWanted.x, state.land.y, strikeWanted.z);
					playSound(level, state.hover, self, MainSounds.EVASION1.get(), 1.2F, 1.2F);
				}
				if (tick == STAND_TICK) state.victim.flinch();
				state.victim.hold(state.ground, faceBack);
				if (tick <= DROP_END) {
					double drop = Math.pow((tick - DROP_START) / (double) (DROP_END - DROP_START), 2.0D);
					performer.place(state.hover.lerp(state.land, drop), state.yaw);
					if (tick == DROP_END) level.sendParticles(MainParticles.DUST.get(), state.land.x, state.land.y + 0.1D, state.land.z, 14, 0.5D, 0.1D, 0.5D, 0.04D);
				} else if (tick < DASH2_START) {
					performer.place(state.land, state.yaw);
				} else {
					double dash = smooth(Mth.clamp((tick - DASH2_START) / (double) (DASH2_END - DASH2_START), 0.0D, 1.0D));
					performer.place(state.land.lerp(state.strikePos, dash), state.yaw);
					if (tick == DASH2_START) {
						NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
								new float[]{state.yaw}, false), self);
						playSound(level, state.land, self, MainSounds.ZANZOKEN.get(), 1.5F, 1.1F);
					}
				}
				return true;
			}

			performer.place(state.strikePos, state.yaw);
			finish(level, performer, state);
			return true;
		}

		public static void release(State state) {
			if (state.bladeSummoned && state.self != null) {
				NetworkHandler.sendToTrackingEntityAndSelf(KiBladeExtendS2C.summon(state.self.getId(), 0), state.self);
			}
			if (state.victim != null) {
				state.victim.release();
				state.victim = null;
			}
			state.origin = null;
			state.base = null;
			state.ground = null;
			state.hover = null;
			state.land = null;
			state.strikePos = null;
			state.bladeSummoned = false;
			state.finished = false;
		}

		private static void charge(ServerLevel level, Performer performer, State state, int tick) {
			if ((tick - CHARGE_START) % 2 != 0) return;
			LivingEntity self = performer.self();
			Vec3 right = facing(state.yaw + 90.0F);
			Vec3 hand = state.hover.add(right.scale(HAND_SIDE * state.scale)).add(0.0D, HAND_UP * state.scale, 0.0D).subtract(facing(state.yaw).scale(0.2D));
			int color = performer.bladeColor();
			float r = ((color >> 16) & 0xFF) / 255.0F;
			float g = ((color >> 8) & 0xFF) / 255.0F;
			float b = (color & 0xFF) / 255.0F;
			level.sendParticles(new DustParticleOptions(new Vector3f(r, g, b), 1.3F), hand.x, hand.y, hand.z, 6, 0.25D, 0.25D, 0.25D, 0.0D);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 3, 0.2D, 0.2D, 0.2D, 0.1D);
			if ((tick - CHARGE_START) % 20 == 0) playSound(level, hand, self, MainSounds.KI_CHARGE_LOOP.get(), 1.3F, 1.3F);
		}

		private static Vec3 shoulder(Vec3 feet, float yaw, float scale) {
			Vec3 facing = facing(yaw);
			Vec3 right = facing(yaw + 90.0F);
			return feet.add(right.scale(SHOULDER_SIDE * scale)).add(0.0D, SHOULDER_UP * scale, 0.0D).add(facing.scale(SHOULDER_FORWARD * scale));
		}

		private static double aimHeight(LivingEntity victim) {
			boolean lying = victim instanceof net.minecraft.world.entity.player.Player || victim instanceof com.dragonminez.common.init.entities.sagas.DBSagasEntity;
			return lying ? Math.min(LYING_AIM_HEIGHT, victim.getBbHeight() * 0.5D) : victim.getBbHeight() * 0.5D;
		}

		private static Vec3 hoverPoint(State state, LivingEntity victim) {
			Vec3 facing = facing(state.yaw);
			Vec3 right = facing(state.yaw + 90.0F);
			Vec3 center = state.ground.add(0.0D, aimHeight(victim), 0.0D);
			Vec3 shoulder = center.subtract(facing.scale(THRUST_FORWARD * THRUST_LENGTH)).add(0.0D, THRUST_DOWN * THRUST_LENGTH, 0.0D);
			return shoulder.subtract(right.scale(SHOULDER_SIDE * state.scale)).subtract(0.0D, SHOULDER_UP * state.scale, 0.0D)
					.subtract(facing.scale(SHOULDER_FORWARD * state.scale));
		}

		private static void hit(ServerLevel level, Performer performer, State state, float share, Vec3 at, Vec3 direction, float burst, SoundEvent sound) {
			LivingEntity self = performer.self();
			LivingEntity target = state.victim.entity();
			target.invulnerableTime = 0;
			performer.strikeHit(target, performer.strikeBudget() * share, false);
			NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(at, direction, burst,
					0xFFFFFF, state.bladeSummoned ? performer.bladeColor() : 0xFFD27F, true, 8 + (int) (burst * 2.0F)), self);
			playSound(level, at, self, sound, 1.8F, 0.85F + self.getRandom().nextFloat() * 0.2F);
		}

		private static void finish(ServerLevel level, Performer performer, State state) {
			LivingEntity self = performer.self();
			LivingEntity victim = state.victim.entity();
			Vec3 center = victim.getBoundingBox().getCenter();
			Vec3 facing = facing(state.yaw);
			state.finished = true;

			victim.invulnerableTime = 0;
			performer.strikeHit(victim, performer.strikeBudget() * SLASH_SHARE, true);
			Vec3 from = center.subtract(facing.scale(0.8D));
			NetworkHandler.sendToTrackingEntityAndSelf(new SwordSlashS2C(from.x, from.y, from.z, (float) facing.x, (float) facing.y, (float) facing.z,
					0.45F, 0.0F, 3.6F, performer.bladeColor(), 0xFFFFFF, 10), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(center, facing, 4.0F, 0xFFFFFF, performer.bladeColor(), true, 18), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.85F, 0.05F, 3, false), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), true, 10.0F), self);
			playSound(level, center, self, MainSounds.CRITICO1.get(), 3.0F, 0.8F);
			playSound(level, center, self, MainSounds.SWORD_SLASH.get(), 2.0F, 0.7F);

			HeldVictim held = state.victim;
			state.victim = null;
			held.release();
			if (victim.isAlive()) KnockbackHelper.apply(victim, facing.scale(FINAL_PUSH).add(0.0D, FINAL_LIFT, 0.0D));
		}

		private static void groundImpact(ServerLevel level, LivingEntity self, Vec3 at, float scale) {
			level.sendParticles(MainParticles.ROCK.get(), at.x, at.y + 0.2D, at.z, 30, scale * 0.3D, 0.3D, scale * 0.3D, 0.25D);
			NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(at.x, at.y + 0.2D, at.z, scale, 0xFFE0A0, 12), self);
			playSound(level, at, self, MainSounds.KNOCKBACK_CHARACTER.get(), 2.0F, 0.7F);
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

		private static Vec3 clearPoint(ServerLevel level, LivingEntity self, Vec3 from, Vec3 wanted, double margin) {
			BlockHitResult hit = level.clip(new ClipContext(from, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
			if (hit.getType() == HitResult.Type.MISS) return wanted;
			Vec3 dir = wanted.subtract(from);
			double length = dir.length();
			if (length < 1.0E-4D) return from;
			return from.add(dir.scale(Math.max(0.0D, hit.getLocation().distanceTo(from) - margin) / length));
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

	public static final class PerfectRushCombo {

		public static final String TECHNIQUE_ID = "perfect_rush";
		public static final String ANIMATION = "skp.perfect_rush";
		private static final int LEAD = 4;
		public static final int DURATION = LEAD + 88;
		public static final double RANGE = 14.0D;
		public static final int COLOR_MAIN = 0xFFC93C;
		public static final int COLOR_DARK = 0xFF8A00;
		public static final int COLOR_SPARK = 0x7FC8FF;

		private static final int POWER_TICK = LEAD + 1;
		private static final int DASH_START = LEAD + 4;
		private static final int DASH_END = LEAD + 8;
		private static final int HOOK_TICK = LEAD + 9;
		private static final int BACKHAND_TICK = LEAD + 12;
		private static final int ELBOW_TICK = LEAD + 15;
		private static final int KNEE_TICK = LEAD + 18;
		private static final int LAUNCH_END = LEAD + 28;
		private static final int LEAP_TICK = LEAD + 30;
		private static final int APPEAR_TICK = LEAD + 33;
		private static final int JAB_TICK = LEAD + 38;
		private static final int STRAIGHT_TICK = LEAD + 41;
		private static final int SECOND_RISE_END = LEAD + 49;
		private static final int DASH_UP_TICK = LEAD + 49;
		private static final int APPEAR_ABOVE_TICK = LEAD + 51;
		private static final int HAMMER_TICK = LEAD + 62;
		private static final int CRASH_TICK = LEAD + 68;
		private static final int DESCEND_START = LEAD + 70;
		private static final int DESCEND_END = LEAD + 78;

		private static final double VICTIM_GAP = 0.3D;
		private static final double HIT_NUDGE = 0.15D;
		private static final double ELBOW_BUMP = 0.3D;
		private static final double KNEE_BUMP = 0.5D;
		private static final double LAUNCH_HEIGHT = 6.0D;
		private static final double SECOND_RISE = 4.0D;
		private static final double AIR_DROP = 0.3D;
		private static final double OVER_HEIGHT = 0.8D;
		private static final double LAND_BACK = 1.2D;
		private static final double MAX_DROP = 30.0D;
		private static final double MAX_VICTIM_DISTANCE = 48.0D;
		private static final float HOOK_SHARE = 0.1F;
		private static final float BACKHAND_SHARE = 0.1F;
		private static final float ELBOW_SHARE = 0.1F;
		private static final float KNEE_SHARE = 0.12F;
		private static final float JAB_SHARE = 0.12F;
		private static final float STRAIGHT_SHARE = 0.14F;
		private static final float HAMMER_SHARE = 0.14F;
		private static final float CRASH_SHARE = 0.18F;

		public interface Performer {
			LivingEntity self();

			LivingEntity target();

			void place(Vec3 position, float yaw);

			float strikeBudget();

			void strikeHit(LivingEntity victim, float amount, boolean lethal);
		}

		public static final class State {
			private HeldVictim victim;
			private float yaw;
			private Vec3 origin;
			private Vec3 base;
			private Vec3 start;
			private Vec3 held;
			private Vec3 apex;
			private Vec3 apex2;
			private Vec3 air;
			private Vec3 over;
			private Vec3 ground;
			private Vec3 land;
			private boolean crashed;

			public LivingEntity victim() {
				return this.victim != null ? this.victim.entity() : null;
			}
		}

		private PerfectRushCombo() {}

		public static boolean tick(ServerLevel level, Performer performer, State state, int tick) {
			LivingEntity self = performer.self();
			self.fallDistance = 0.0F;

			if (tick < DASH_START) {
				LivingEntity target = performer.target();
				if (!HeldVictim.canTarget(self, target)) return false;
				if (state.origin == null) state.origin = self.position();
				state.yaw = yawTowards(self.position(), target.position());
				performer.place(state.origin, state.yaw);
				if (tick == POWER_TICK) {
					NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), true, 8.0F), self);
					NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(self.getX(), self.getY() + 0.1D, self.getZ(), 3.5F, COLOR_MAIN, 10), self);
					playSound(level, self.position(), self, MainSounds.AURA_START.get(), 1.4F, 1.1F);
				}
				return true;
			}

			if (tick == DASH_START) {
				LivingEntity target = performer.target();
				if (!HeldVictim.canTarget(self, target) || target.level() != level || self.distanceTo(target) > RANGE + 6.0D) return false;
				if (state.origin == null) state.origin = self.position();
				state.yaw = yawTowards(self.position(), target.position());
				state.start = target.position();
				state.held = state.start;
				state.base = state.start.subtract(facing(state.yaw).scale(gap(self, target)));
				if (!level.noCollision(self, self.getBoundingBox().move(state.base.subtract(self.position())))) state.base = self.position();
				state.victim = HeldVictim.canHold(self, target) ? HeldVictim.capture(target, DURATION + 20) : HeldVictim.root(target);
				NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
						new float[]{state.yaw}, false), self);
				NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), false, 6.0F), self);
				playSound(level, self.position(), self, MainSounds.ZANZOKEN.get(), 1.5F, 0.9F);
			}

			if (state.base == null) return false;

			if (state.crashed) {
				if (state.victim != null) {
					if (state.victim.entity().isAlive()) {
						state.victim.hold(state.ground, state.yaw + 180.0F);
					} else {
						state.victim.release();
						state.victim = null;
					}
				}
				descend(level, performer, state, tick);
				return true;
			}

			if (state.victim == null || !state.victim.isValid(self, MAX_VICTIM_DISTANCE)) return false;

			LivingEntity victim = state.victim.entity();
			Vec3 facing = facing(state.yaw);
			Vec3 right = facing(state.yaw + 90.0F);
			float faceBack = state.yaw + 180.0F;

			if (tick < KNEE_TICK) {
				double dash = smooth(Mth.clamp((tick - DASH_START) / (double) (DASH_END - DASH_START), 0.0D, 1.0D));
				Vec3 dashed = state.origin.lerp(state.base, dash);
				performer.place(dashed, state.yaw);
				state.held = state.held.lerp(state.start, 0.4D);
				if (tick == HOOK_TICK) {
					state.held = state.start.add(right.scale(-HIT_NUDGE));
					strike(level, performer, state, HOOK_SHARE, 2.0F, MainSounds.GOLPE4.get(), facing);
					arc(level, self, victim, facing, 0.0F);
					state.victim.flinch();
				}
				if (tick == BACKHAND_TICK) {
					state.held = state.start.add(right.scale(HIT_NUDGE)).add(0.0D, -0.1D, 0.0D);
					strike(level, performer, state, BACKHAND_SHARE, 2.0F, MainSounds.GOLPE5.get(), facing);
					arc(level, self, victim, facing, 150.0F);
					state.victim.flinch();
				}
				if (tick == ELBOW_TICK) {
					state.held = state.start.add(0.0D, ELBOW_BUMP, 0.0D);
					strike(level, performer, state, ELBOW_SHARE, 2.2F, MainSounds.GOLPE6.get(), new Vec3(0.0D, 1.0D, 0.0D));
					arc(level, self, victim, facing, 80.0F);
					state.victim.flinch();
				}
				state.victim.hold(state.held, faceBack);
				return true;
			}

			if (tick < JAB_TICK) {
				if (tick == KNEE_TICK) {
					state.apex = clearAbove(level, victim, state.start, LAUNCH_HEIGHT);
					state.apex2 = clearAbove(level, victim, state.start, LAUNCH_HEIGHT + SECOND_RISE);
					if (state.apex2.y < state.apex.y) state.apex2 = state.apex;
					state.air = state.apex.subtract(facing.scale(gap(self, victim))).subtract(0.0D, AIR_DROP, 0.0D);
					strike(level, performer, state, KNEE_SHARE, 2.6F, MainSounds.CRITICO1.get(), new Vec3(0.0D, 1.0D, 0.0D));
					state.victim.launched();
					NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(state.base.x, state.base.y + 0.1D, state.base.z, 4.0F, COLOR_MAIN, 10), self);
					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.6F, 0.05F, 2, false), self);
					playSound(level, state.start, self, MainSounds.EVASION2.get(), 1.5F, 0.7F);
				}
				double lift = easeOut(Mth.clamp((tick - KNEE_TICK) / (double) (LAUNCH_END - KNEE_TICK), 0.0D, 1.0D));
				state.held = state.start.add(0.0D, KNEE_BUMP, 0.0D).lerp(state.apex, lift);
				state.victim.hold(state.held, faceBack);

				if (tick < LEAP_TICK) {
					performer.place(state.base, state.yaw);
				} else {
					if (tick == LEAP_TICK) {
						NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
								new float[]{state.yaw}, false), self);
						level.sendParticles(new DustParticleOptions(rgb(COLOR_SPARK), 1.4F), state.base.x, state.base.y + 0.6D, state.base.z,
								40, 0.7D, 0.4D, 0.7D, 0.2D);
						level.sendParticles(ParticleTypes.ELECTRIC_SPARK, state.base.x, state.base.y + 0.6D, state.base.z, 20, 0.6D, 0.4D, 0.6D, 0.3D);
						level.sendParticles(MainParticles.DUST.get(), state.base.x, state.base.y + 0.1D, state.base.z, 14, 0.5D, 0.1D, 0.5D, 0.05D);
						playSound(level, state.base, self, MainSounds.ZANZOKEN.get(), 1.5F, 1.2F);
					}
					double rise = smooth(Mth.clamp((tick - LEAP_TICK) / (double) (APPEAR_TICK - LEAP_TICK), 0.0D, 1.0D));
					performer.place(state.base.lerp(state.air, rise), state.yaw);
				}
				return true;
			}

			if (tick < HAMMER_TICK) {
				if (tick == JAB_TICK) {
					strike(level, performer, state, JAB_SHARE, 2.4F, MainSounds.CRITICO2.get(), facing);
					arc(level, self, victim, facing, 20.0F);
					state.victim.flinch();
				}
				if (tick == STRAIGHT_TICK) {
					strike(level, performer, state, STRAIGHT_SHARE, 2.8F, MainSounds.CRITICO1.get(), new Vec3(facing.x, 1.0D, facing.z));
					arc(level, self, victim, facing, -30.0F);
					state.victim.launched();
					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.65F, 0.05F, 2, false), self);
					playSound(level, state.apex, self, MainSounds.EVASION2.get(), 1.5F, 0.8F);
				}
				if (tick < STRAIGHT_TICK) {
					state.held = state.apex;
				} else {
					double rise = easeOut(Mth.clamp((tick - STRAIGHT_TICK) / (double) (SECOND_RISE_END - STRAIGHT_TICK), 0.0D, 1.0D));
					state.held = state.apex.lerp(state.apex2, rise);
				}
				state.victim.hold(state.held, faceBack);

				if (tick == DASH_UP_TICK) {
					state.over = state.apex2.add(0.0D, victim.getBbHeight() + OVER_HEIGHT, 0.0D).subtract(facing.scale(0.4D));
					NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
							new float[]{state.yaw}, false), self);
					level.sendParticles(new DustParticleOptions(rgb(COLOR_SPARK), 1.4F), self.getX(), self.getY() + 1.0D, self.getZ(),
							24, 0.5D, 0.6D, 0.5D, 0.2D);
					playSound(level, self.position(), self, MainSounds.ZANZOKEN.get(), 1.5F, 1.0F);
				}
				if (tick < DASH_UP_TICK) {
					performer.place(state.air, state.yaw);
				} else if (tick < APPEAR_ABOVE_TICK) {
					double rise = smooth((tick - DASH_UP_TICK) / (double) (APPEAR_ABOVE_TICK - DASH_UP_TICK));
					performer.place(state.air.lerp(state.over, rise), state.yaw);
				} else {
					performer.place(state.over, state.yaw);
					if ((tick - APPEAR_ABOVE_TICK) % 3 == 0) charge(level, self);
				}
				return true;
			}

			if (tick < CRASH_TICK) {
				if (tick == HAMMER_TICK) {
					state.ground = grounded(level, state.held);
					strike(level, performer, state, HAMMER_SHARE, 3.2F, MainSounds.CRITICO1.get(), new Vec3(0.0D, -1.0D, 0.0D));
					arc(level, self, victim, new Vec3(facing.x, -1.0D, facing.z), 90.0F);
					state.victim.knockDown();
					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.8F, 0.05F, 3, true), self);
					NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), false, 8.0F), self);
				}
				double fall = Math.pow(Mth.clamp((tick - HAMMER_TICK) / (double) (CRASH_TICK - HAMMER_TICK), 0.0D, 1.0D), 2.0D);
				state.held = state.apex2.lerp(state.ground, fall);
				state.victim.hold(state.held, faceBack);
				performer.place(state.over, state.yaw);
				return true;
			}

			performer.place(state.over, state.yaw);
			crash(level, performer, state);
			return true;
		}

		public static void release(State state) {
			if (state.victim != null) {
				state.victim.release();
				state.victim = null;
			}
			state.origin = null;
			state.base = null;
			state.apex = null;
			state.apex2 = null;
			state.air = null;
			state.over = null;
			state.ground = null;
			state.land = null;
			state.crashed = false;
		}

		private static void strike(ServerLevel level, Performer performer, State state, float share, float burst, SoundEvent sound, Vec3 direction) {
			LivingEntity self = performer.self();
			LivingEntity target = state.victim.entity();
			target.invulnerableTime = 0;
			performer.strikeHit(target, performer.strikeBudget() * share, false);

			Vec3 center = target.getBoundingBox().getCenter();
			NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(center, direction, burst,
					0xFFFFFF, COLOR_MAIN, true, 8 + (int) (burst * 2.0F)), self);
			playSound(level, center, self, sound, 1.8F, 0.85F + self.getRandom().nextFloat() * 0.2F);
		}

		private static void arc(ServerLevel level, LivingEntity self, LivingEntity victim, Vec3 direction, float roll) {
			Vec3 center = victim.getBoundingBox().getCenter();
			Vec3 dir = direction.normalize();
			Vec3 from = center.subtract(dir.scale(0.6D));
			NetworkHandler.sendToTrackingEntityAndSelf(new SwordSlashS2C(from.x, from.y, from.z, (float) dir.x, (float) dir.y, (float) dir.z,
					0.5F, roll, 1.6F, COLOR_MAIN, 0xFFFFFF, 8), self);
		}

		private static void charge(ServerLevel level, LivingEntity self) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, self.getX(), self.getY() + self.getBbHeight() * 0.6D, self.getZ(),
					4, 0.4D, 0.5D, 0.4D, 0.15D);
			level.sendParticles(new DustParticleOptions(rgb(COLOR_MAIN), 1.2F), self.getX(), self.getY() + self.getBbHeight() * 0.6D, self.getZ(),
					4, 0.4D, 0.5D, 0.4D, 0.0D);
		}

		private static void crash(ServerLevel level, Performer performer, State state) {
			LivingEntity self = performer.self();
			LivingEntity victim = state.victim.entity();
			state.crashed = true;
			state.victim.hold(state.ground, state.yaw + 180.0F);
			victim.invulnerableTime = 0;
			performer.strikeHit(victim, performer.strikeBudget() * CRASH_SHARE, true);
			state.victim.knockOut();

			NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(state.ground, new Vec3(0.0D, -1.0D, 0.0D), 5.5F,
					COLOR_MAIN, COLOR_DARK, false, 18), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.85F, 0.05F, 3, false), self);
			groundImpact(level, self, state.ground, 8.0F);
			playSound(level, state.ground, self, MainSounds.CRITICO1.get(), 3.0F, 0.7F);
			playSound(level, state.ground, self, MainSounds.ANCHOR_SLAM.get(), 2.5F, 0.8F);

			if (!victim.isAlive()) {
				state.victim.release();
				state.victim = null;
			}
		}

		private static void descend(ServerLevel level, Performer performer, State state, int tick) {
			LivingEntity self = performer.self();
			if (state.land == null) {
				Vec3 wanted = state.ground.subtract(facing(state.yaw).scale(gap(self, self) + LAND_BACK));
				state.land = grounded(level, wanted);
				if (Math.abs(state.land.y - state.ground.y) > 3.0D) state.land = new Vec3(wanted.x, state.ground.y, wanted.z);
			}
			if (tick < DESCEND_START) {
				performer.place(state.over, state.yaw);
				return;
			}
			double u = Mth.clamp((tick - DESCEND_START) / (double) (DESCEND_END - DESCEND_START), 0.0D, 1.0D);
			performer.place(state.over.lerp(state.land, u * u), state.yaw);
			if (tick == DESCEND_START) playSound(level, state.over, self, MainSounds.EVASION1.get(), 1.2F, 0.9F);
			if (tick == DESCEND_END) {
				level.sendParticles(MainParticles.DUST.get(), state.land.x, state.land.y + 0.1D, state.land.z, 14, 0.5D, 0.1D, 0.5D, 0.04D);
				playSound(level, state.land, self, MainSounds.KNOCKBACK_CHARACTER.get(), 0.9F, 1.2F);
			}
		}

		private static void groundImpact(ServerLevel level, LivingEntity self, Vec3 at, float scale) {
			level.sendParticles(MainParticles.ROCK.get(), at.x, at.y + 0.2D, at.z, 50, scale * 0.3D, 0.3D, scale * 0.3D, 0.3D);
			level.sendParticles(MainParticles.DUST.get(), at.x, at.y + 0.2D, at.z, 60, scale * 0.35D, 0.3D, scale * 0.35D, 0.1D);
			NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(at.x, at.y + 0.2D, at.z, scale, COLOR_MAIN, 14), self);
			playSound(level, at, self, MainSounds.KNOCKBACK_CHARACTER.get(), 2.0F, 0.7F);
		}

		private static Vector3f rgb(int color) {
			return new Vector3f(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
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

	public static final class SpiritBreakerCombo {

		public static final String TECHNIQUE_ID = "spirit_breaker";
		public static final String ANIMATION = "skp.spirit_breaker";
		private static final int LEAD = 4;
		public static final int DURATION = LEAD + 144;
		public static final double RANGE = 14.0D;
		public static final int COLOR_MAIN = 0xFFD23C;
		public static final int COLOR_DARK = 0xFF8C1A;

		private static final int DASH_START = LEAD + 2;
		private static final int DASH_END = LEAD + 5;
		private static final int GUT_TICK = LEAD + 6;
		private static final int UPPERCUT_TICK = LEAD + 26;
		private static final int LAUNCH_END = LEAD + 35;
		private static final int POWER_TICK = LEAD + 32;
		private static final int VANISH_TICK = LEAD + 38;
		private static final int APPEAR_TICK = LEAD + 40;
		private static final int[] FLURRY_TICKS = {LEAD + 41, LEAD + 45, LEAD + 49, LEAD + 53};
		private static final int FLASH_TICK = LEAD + 48;
		private static final int BIG_PUNCH_TICK = LEAD + 57;
		private static final int BLOWN_END = LEAD + 67;
		private static final int RAGE_TICK = LEAD + 64;
		private static final int RAM_START = LEAD + 70;
		private static final int RAM_TICK = LEAD + 73;
		private static final int SLAM_TICK = LEAD + 82;
		private static final int HOP_END = LEAD + 88;
		private static final int JUMP_START = LEAD + 104;
		private static final int JUMP_END = LEAD + 111;
		private static final int FLAME_START = LEAD + 112;
		private static final int FLIP_START = LEAD + 119;
		private static final int AXE_TICK = LEAD + 127;
		private static final int STEP_START = LEAD + 132;
		private static final int STEP_END = LEAD + 136;

		private static final double VICTIM_GAP = 0.3D;
		private static final double GUT_PUSH = 0.25D;
		private static final double LAUNCH_HEIGHT = 5.0D;
		private static final double LAUNCH_DISTANCE = 3.0D;
		private static final double AIR_DROP = 0.3D;
		private static final double FLURRY_PUSH = 0.6D;
		private static final double BLOWN_DISTANCE = 6.0D;
		private static final double BLOWN_RISE = 1.0D;
		private static final double RIDE_HEIGHT = 0.5D;
		private static final double STAND_BACK = 0.8D;
		private static final double HOP_ARC = 1.2D;
		private static final double AXE_HEIGHT = 3.5D;
		private static final double HEEL_HEIGHT = 0.2D;
		private static final double MAX_DROP = 30.0D;
		private static final double MAX_VICTIM_DISTANCE = 48.0D;
		private static final float GUT_SHARE = 0.08F;
		private static final float UPPERCUT_SHARE = 0.08F;
		private static final float FLURRY_SHARE = 0.06F;
		private static final float BIG_PUNCH_SHARE = 0.08F;
		private static final float RAM_SHARE = 0.1F;
		private static final float SLAM_SHARE = 0.12F;
		private static final float AXE_SHARE = 0.3F;

		public interface Performer {
			LivingEntity self();

			LivingEntity target();

			void place(Vec3 position, float yaw);

			float strikeBudget();

			void strikeHit(LivingEntity victim, float amount, boolean lethal);
		}

		public static final class State {
			private HeldVictim victim;
			private float yaw;
			private Vec3 origin;
			private Vec3 base;
			private Vec3 start;
			private Vec3 held;
			private Vec3 apex;
			private Vec3 flurry;
			private Vec3 far;
			private Vec3 brace;
			private Vec3 ram;
			private Vec3 ground;
			private Vec3 ride;
			private Vec3 stand;
			private Vec3 axeAir;
			private Vec3 heel;
			private Vec3 rest;
			private boolean finished;

			public LivingEntity victim() {
				return this.victim != null ? this.victim.entity() : null;
			}
		}

		private SpiritBreakerCombo() {}

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
				if (state.origin == null) state.origin = self.position();
				state.yaw = yawTowards(self.position(), target.position());
				state.start = target.position();
				state.held = state.start;
				state.base = state.start.subtract(facing(state.yaw).scale(gap(self, target)));
				if (!level.noCollision(self, self.getBoundingBox().move(state.base.subtract(self.position())))) state.base = self.position();
				state.victim = HeldVictim.canHold(self, target) ? HeldVictim.capture(target, DURATION + 20) : HeldVictim.root(target);
				NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
						new float[]{state.yaw}, false), self);
				playSound(level, self.position(), self, MainSounds.ZANZOKEN.get(), 1.5F, 0.9F);
			}

			if (state.base == null) return false;

			if (state.finished) {
				if (state.victim != null) {
					if (state.victim.entity().isAlive()) {
						state.victim.hold(state.ground, state.yaw + 180.0F);
					} else {
						state.victim.release();
						state.victim = null;
					}
				}
				stepBack(level, performer, state, tick);
				return true;
			}

			if (state.victim == null || !state.victim.isValid(self, MAX_VICTIM_DISTANCE)) return false;

			LivingEntity victim = state.victim.entity();
			Vec3 facing = facing(state.yaw);
			float faceBack = state.yaw + 180.0F;

			if (tick < UPPERCUT_TICK) {
				double dash = smooth(Mth.clamp((tick - DASH_START) / (double) (DASH_END - DASH_START), 0.0D, 1.0D));
				performer.place(state.origin.lerp(state.base, dash), state.yaw);
				if (tick < GUT_TICK) state.held = state.held.lerp(state.start, 0.4D);
				if (tick == GUT_TICK) {
					state.held = state.start.add(facing.scale(GUT_PUSH));
					strike(level, performer, state, GUT_SHARE, 2.8F, MainSounds.CRITICO2.get(), facing);
					state.victim.flinch();
					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.8F, 0.05F, 6, true), self);
					NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(state.held.x, state.held.y + victim.getBbHeight() * 0.5D, state.held.z,
							2.5F, COLOR_MAIN, 8), self);
				}
				state.victim.hold(state.held, faceBack);
				return true;
			}

			if (tick < APPEAR_TICK) {
				if (tick == UPPERCUT_TICK) {
					Vec3 lifted = state.held.add(0.0D, 1.0D, 0.0D);
					state.apex = clearPoint(level, self, lifted, state.held.add(facing.scale(LAUNCH_DISTANCE)).add(0.0D, LAUNCH_HEIGHT, 0.0D),
							victim.getBbWidth() * 0.5D + 0.2D);
					strike(level, performer, state, UPPERCUT_SHARE, 2.6F, MainSounds.CRITICO1.get(), new Vec3(0.0D, 1.0D, 0.0D));
					state.victim.launched();
					NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(state.held.x, state.held.y + 1.0D, state.held.z, 3.5F, 0xFFFFFF, 10), self);
					playSound(level, state.held, self, MainSounds.EVASION2.get(), 1.5F, 0.7F);
				}
				double lift = easeOut(Mth.clamp((tick - UPPERCUT_TICK) / (double) (LAUNCH_END - UPPERCUT_TICK), 0.0D, 1.0D));
				Vec3 launchFrom = state.start.add(facing.scale(GUT_PUSH));
				state.victim.hold(launchFrom.lerp(state.apex, lift), faceBack);
				if (tick == POWER_TICK) {
					NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), true, 10.0F), self);
					NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(state.base.x, state.base.y + 0.1D, state.base.z, 4.5F, COLOR_MAIN, 12), self);
					playSound(level, state.base, self, MainSounds.AURA_START.get(), 1.6F, 0.8F);
				}
				if (tick >= POWER_TICK && tick < VANISH_TICK) sparks(level, self, 0.6D);
				if (tick == VANISH_TICK) {
					state.held = state.apex;
					NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
							new float[]{state.yaw}, false), self);
					level.sendParticles(ParticleTypes.ELECTRIC_SPARK, state.base.x, state.base.y + 1.0D, state.base.z, 20, 0.5D, 0.6D, 0.5D, 0.3D);
					playSound(level, state.base, self, MainSounds.ZANZOKEN.get(), 1.5F, 1.2F);
				}
				if (tick < VANISH_TICK) {
					performer.place(state.base, state.yaw);
				} else {
					double rise = smooth((tick - VANISH_TICK) / (double) (APPEAR_TICK - VANISH_TICK));
					performer.place(state.base.lerp(airBehind(state.apex, facing, self, victim), rise), state.yaw);
				}
				return true;
			}

			if (tick < RAM_START) {
				if (tick == APPEAR_TICK) state.flurry = state.apex;
				for (int i = 0; i < FLURRY_TICKS.length; i++) {
					if (FLURRY_TICKS[i] != tick) continue;
					Vec3 pushed = clearPoint(level, self, state.flurry, state.flurry.add(facing.scale(FLURRY_PUSH)), victim.getBbWidth() * 0.5D + 0.2D);
					state.flurry = pushed;
					Vec3 direction = i % 2 == 0 ? facing.add(0.0D, 0.3D, 0.0D) : facing;
					strike(level, performer, state, FLURRY_SHARE, 2.2F, i % 2 == 0 ? MainSounds.GOLPE4.get() : MainSounds.GOLPE5.get(), direction);
					state.victim.flinch();
				}
				if (tick == FLASH_TICK) NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.6F, 0.05F, 2, false), self);

				if (tick < BIG_PUNCH_TICK) {
					state.held = state.held.lerp(state.flurry, 0.5D);
					state.victim.hold(state.held, faceBack);
					performer.place(airBehind(state.held, facing, self, victim), state.yaw);
					return true;
				}

				if (tick == BIG_PUNCH_TICK) {
					state.brace = self.position();
					state.far = clearPoint(level, self, state.flurry, state.flurry.add(facing.scale(BLOWN_DISTANCE)).add(0.0D, BLOWN_RISE, 0.0D),
							victim.getBbWidth() * 0.5D + 0.3D);
					strike(level, performer, state, BIG_PUNCH_SHARE, 3.2F, MainSounds.CRITICO1.get(), facing);
					state.victim.launched();
					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.75F, 0.05F, 3, false), self);
					NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(state.flurry.x, state.flurry.y + 1.0D, state.flurry.z, 4.0F, COLOR_MAIN, 10), self);
				}
				double blown = easeOut(Mth.clamp((tick - BIG_PUNCH_TICK) / (double) (BLOWN_END - BIG_PUNCH_TICK), 0.0D, 1.0D));
				state.held = state.flurry.lerp(state.far, blown);
				state.victim.hold(state.held, faceBack);
				performer.place(state.brace, state.yaw);
				if (tick == RAGE_TICK) {
					NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), true, 9.0F), self);
					playSound(level, state.brace, self, MainSounds.KI_CHARGE_LOOP.get(), 1.5F, 1.4F);
				}
				if (tick >= RAGE_TICK) sparks(level, self, 0.8D);
				return true;
			}

			if (tick < SLAM_TICK) {
				if (tick == RAM_START) {
					state.ram = state.far.subtract(facing.scale(gap(self, victim))).add(0.0D, RIDE_HEIGHT, 0.0D);
					state.ground = grounded(level, state.far);
					NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 30, new Vec3[]{self.position()},
							new float[]{state.yaw}, false), self);
					NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), false, 7.0F), self);
					playSound(level, state.brace, self, MainSounds.ZANZOKEN.get(), 1.5F, 0.8F);
				}
				if (tick <= RAM_TICK) {
					double dash = smooth((tick - RAM_START) / (double) (RAM_TICK - RAM_START));
					performer.place(state.brace.lerp(state.ram, dash), state.yaw);
					state.victim.hold(state.far, faceBack);
					if (tick == RAM_TICK) {
						strike(level, performer, state, RAM_SHARE, 3.6F, MainSounds.CRITICO2.get(), new Vec3(facing.x, -0.6D, facing.z));
						state.victim.knockDown();
						NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.85F, 0.05F, 3, true), self);
						NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(state.far.add(0.0D, 1.0D, 0.0D), facing, 5.0F,
								0xFFFFFF, COLOR_MAIN, false, 14), self);
					}
					return true;
				}
				double fall = Math.pow((tick - RAM_TICK) / (double) (SLAM_TICK - RAM_TICK), 2.0D);
				state.held = state.far.lerp(state.ground, fall);
				state.victim.hold(state.held, faceBack);
				state.ride = state.held.add(0.0D, RIDE_HEIGHT, 0.0D).subtract(facing.scale(0.3D));
				performer.place(state.ride, state.yaw);
				return true;
			}

			if (tick == SLAM_TICK) {
				state.victim.hold(state.ground, faceBack);
				strike(level, performer, state, SLAM_SHARE, 3.4F, MainSounds.ANCHOR_SLAM.get(), new Vec3(0.0D, -1.0D, 0.0D));
				state.victim.knockDown();
				groundImpact(level, self, state.ground, 6.0F);
				NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.75F, 0.05F, 2, false), self);
				Vec3 wanted = state.ground.subtract(facing.scale(gap(self, victim) + STAND_BACK));
				state.stand = grounded(level, wanted);
				if (Math.abs(state.stand.y - state.ground.y) > 3.0D) state.stand = new Vec3(wanted.x, state.ground.y, wanted.z);
				state.axeAir = state.ground.subtract(facing.scale(gap(self, victim) * 0.8D)).add(0.0D, AXE_HEIGHT, 0.0D);
				state.heel = state.ground.subtract(facing.scale(gap(self, victim) * 0.6D)).add(0.0D, HEEL_HEIGHT, 0.0D);
				performer.place(state.ride, state.yaw);
				return true;
			}

			state.victim.hold(state.ground, faceBack);

			if (tick <= HOP_END) {
				double u = (tick - SLAM_TICK) / (double) (HOP_END - SLAM_TICK);
				performer.place(state.ride.lerp(state.stand, u).add(0.0D, Math.sin(u * Math.PI) * HOP_ARC, 0.0D), state.yaw);
				if (tick == HOP_END) level.sendParticles(MainParticles.DUST.get(), state.stand.x, state.stand.y + 0.1D, state.stand.z, 10, 0.4D, 0.1D, 0.4D, 0.03D);
				return true;
			}

			if (tick < JUMP_START) {
				performer.place(state.stand, state.yaw);
				return true;
			}

			if (tick <= JUMP_END) {
				double u = smooth((tick - JUMP_START) / (double) (JUMP_END - JUMP_START));
				performer.place(state.stand.lerp(state.axeAir, u), state.yaw);
				if (tick == JUMP_START) playSound(level, state.stand, self, MainSounds.EVASION1.get(), 1.3F, 1.0F);
				return true;
			}

			if (tick < FLIP_START) {
				performer.place(state.axeAir, state.yaw);
				if (tick >= FLAME_START && (tick - FLAME_START) % 2 == 0) flames(level, self);
				if (tick == FLAME_START) {
					NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), true, 8.0F), self);
					playSound(level, state.axeAir, self, MainSounds.AURA_START.get(), 1.5F, 1.0F);
				}
				return true;
			}

			if (tick < AXE_TICK) {
				double u = Math.pow((tick - FLIP_START) / (double) (AXE_TICK - FLIP_START), 2.0D);
				performer.place(state.axeAir.lerp(state.heel, u), state.yaw);
				if (tick == FLIP_START || tick == FLIP_START + 4) {
					Vec3 center = self.position().add(0.0D, self.getBbHeight() * 0.5D, 0.0D);
					NetworkHandler.sendToTrackingEntityAndSelf(new SwordSlashS2C(center.x, center.y, center.z, (float) facing.x, 0.0F, (float) facing.z,
							0.6F, 90.0F, 1.8F, COLOR_MAIN, 0xFFFFFF, 8), self);
				}
				if (tick == FLIP_START) playSound(level, state.axeAir, self, MainSounds.EVASION2.get(), 1.6F, 0.6F);
				if (tick == AXE_TICK - 2) playSound(level, state.axeAir, self, MainSounds.KI_EXPLOSION_CHARGE.get(), 1.4F, 1.4F);
				return true;
			}

			performer.place(state.heel, state.yaw);
			axe(level, performer, state);
			return true;
		}

		public static void release(State state) {
			if (state.victim != null) {
				state.victim.release();
				state.victim = null;
			}
			state.origin = null;
			state.base = null;
			state.apex = null;
			state.flurry = null;
			state.far = null;
			state.brace = null;
			state.ram = null;
			state.ground = null;
			state.ride = null;
			state.stand = null;
			state.axeAir = null;
			state.heel = null;
			state.rest = null;
			state.finished = false;
		}

		private static Vec3 airBehind(Vec3 victimPos, Vec3 facing, LivingEntity self, LivingEntity victim) {
			return victimPos.subtract(facing.scale(gap(self, victim))).subtract(0.0D, AIR_DROP, 0.0D);
		}

		private static void strike(ServerLevel level, Performer performer, State state, float share, float burst, SoundEvent sound, Vec3 direction) {
			LivingEntity self = performer.self();
			LivingEntity target = state.victim.entity();
			target.invulnerableTime = 0;
			performer.strikeHit(target, performer.strikeBudget() * share, false);

			Vec3 center = target.getBoundingBox().getCenter();
			NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(center, direction, burst,
					0xFFFFFF, COLOR_MAIN, true, 8 + (int) (burst * 2.0F)), self);
			playSound(level, center, self, sound, 1.8F, 0.85F + self.getRandom().nextFloat() * 0.2F);
		}

		private static void axe(ServerLevel level, Performer performer, State state) {
			LivingEntity self = performer.self();
			LivingEntity victim = state.victim.entity();
			state.finished = true;
			victim.invulnerableTime = 0;
			performer.strikeHit(victim, performer.strikeBudget() * AXE_SHARE, true);
			state.victim.knockOut();

			Vec3 at = state.ground;
			NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(at.add(0.0D, 0.5D, 0.0D), new Vec3(0.0D, -1.0D, 0.0D), 7.5F,
					COLOR_MAIN, COLOR_DARK, false, 20), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerImpactFrameS2C(0.9F, 0.05F, 5, true), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(self.getId(), true, 14.0F), self);
			NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(at.x, at.y + 0.3D, at.z, 13.0F, 0xFFFFFF, 18), self);
			KiExplosionVisualEntity visual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), level);
			visual.setupExplosion(COLOR_MAIN, COLOR_DARK, COLOR_DARK, 5.5F);
			visual.moveTo(at.x, at.y + 0.3D, at.z, 0.0F, 0.0F);
			level.addFreshEntity(visual);
			groundImpact(level, self, at, 9.0F);
			playSound(level, at, self, MainSounds.CRITICO1.get(), 3.0F, 0.7F);
			playSound(level, at, self, MainSounds.KI_EXPLOSION_IMPACT.get(), 2.5F, 0.9F);
			playSound(level, at, self, MainSounds.ANCHOR_SLAM.get(), 3.0F, 0.6F);

			if (!victim.isAlive()) {
				state.victim.release();
				state.victim = null;
			}
		}

		private static void stepBack(ServerLevel level, Performer performer, State state, int tick) {
			LivingEntity self = performer.self();
			if (state.rest == null) {
				Vec3 wanted = state.ground.subtract(facing(state.yaw).scale(self.getBbWidth() + VICTIM_GAP + 0.4D));
				state.rest = grounded(level, wanted);
				if (Math.abs(state.rest.y - state.ground.y) > 3.0D) state.rest = new Vec3(wanted.x, state.ground.y, wanted.z);
			}
			if (tick < STEP_START) {
				performer.place(state.heel, state.yaw);
				return;
			}
			double u = Mth.clamp((tick - STEP_START) / (double) (STEP_END - STEP_START), 0.0D, 1.0D);
			performer.place(state.heel.lerp(state.rest, u).add(0.0D, Math.sin(u * Math.PI) * 0.6D, 0.0D), state.yaw);
		}

		private static void sparks(ServerLevel level, LivingEntity self, double spread) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, self.getX(), self.getY() + self.getBbHeight() * 0.6D, self.getZ(),
					3, spread * 0.5D, spread, spread * 0.5D, 0.15D);
		}

		private static void flames(ServerLevel level, LivingEntity self) {
			level.sendParticles(ParticleTypes.FLAME, self.getX(), self.getY() + self.getBbHeight() * 0.5D, self.getZ(),
					5, 0.35D, 0.6D, 0.35D, 0.02D);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, self.getX(), self.getY() + self.getBbHeight() * 0.6D, self.getZ(),
					2, 0.3D, 0.5D, 0.3D, 0.1D);
		}

		private static void groundImpact(ServerLevel level, LivingEntity self, Vec3 at, float scale) {
			level.sendParticles(MainParticles.ROCK.get(), at.x, at.y + 0.2D, at.z, 50, scale * 0.3D, 0.3D, scale * 0.3D, 0.3D);
			level.sendParticles(MainParticles.DUST.get(), at.x, at.y + 0.2D, at.z, 60, scale * 0.35D, 0.3D, scale * 0.35D, 0.1D);
			NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(at.x, at.y + 0.2D, at.z, scale, COLOR_MAIN, 14), self);
			playSound(level, at, self, MainSounds.KNOCKBACK_CHARACTER.get(), 2.0F, 0.7F);
		}

		private static Vec3 grounded(ServerLevel level, Vec3 at) {
			double floor = groundHeight(level, at.x, at.z, at.y + 0.5D);
			if (at.y - floor > MAX_DROP) return at;
			return new Vec3(at.x, floor, at.z);
		}

		private static Vec3 clearPoint(ServerLevel level, LivingEntity self, Vec3 from, Vec3 wanted, double margin) {
			BlockHitResult hit = level.clip(new ClipContext(from, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
			if (hit.getType() == HitResult.Type.MISS) return wanted;
			Vec3 dir = wanted.subtract(from);
			double length = dir.length();
			if (length < 1.0E-4D) return from;
			return from.add(dir.scale(Math.max(0.0D, hit.getLocation().distanceTo(from) - margin) / length));
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
}
