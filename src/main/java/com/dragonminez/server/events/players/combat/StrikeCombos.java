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
import com.dragonminez.common.network.S2C.KiBurstVfxS2C;
import com.dragonminez.common.network.S2C.RageScreamVfxS2C;
import com.dragonminez.common.network.S2C.ShockwaveVfxS2C;
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
}
