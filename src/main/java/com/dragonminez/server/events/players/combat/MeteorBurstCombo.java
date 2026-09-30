package com.dragonminez.server.events.players.combat;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ImpactBurstVfxS2C;
import com.dragonminez.common.network.S2C.ShockwaveVfxS2C;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class MeteorBurstCombo {

	public static final String TECHNIQUE_ID = "meteor_burst";
	public static final String ANIMATION = "skp.meteor_burst";
	public static final int DURATION = 120;
	public static final double RANGE = 14.0D;
	public static final int COLOR_MAIN = 0xA64DFF;
	public static final int COLOR_BORDER = 0x6A3BFF;
	public static final int COLOR_OUTLINE = 0x4A2CE0;

	private static final int DASH_TICK = 6;
	private static final int[] HIT_TICKS = {10, 14, 20, 30, 40};
	private static final int LIFT_START = 20;
	private static final int LIFT_END = 30;
	private static final double LIFT_HEIGHT = 3.0D;
	private static final int SLAM_TICK = 40;
	private static final int LAND_TICK = 44;
	private static final int DROP_START = 45;
	private static final int STOMP_TICK = 50;
	private static final int RISE_START = 54;
	private static final int RISE_END = 64;
	private static final double HOVER_HEIGHT = 9.0D;
	private static final double HOVER_BACK = 3.0D;
	private static final int FIRE_START = 86;
	private static final int FIRE_END = 106;
	private static final int[] PULSE_TICKS = {92, 98, 104};
	private static final int FINAL_TICK = 108;
	private static final double FINAL_RADIUS = 5.0D;
	private static final double VICTIM_GAP = 0.35D;
	private static final double MAX_VICTIM_DISTANCE = 48.0D;
	private static final float HIT_SHARE = 0.15F;
	private static final float STOMP_SHARE = 0.25F;
	private static final float SHOT_SHARE = 0.05F;
	private static final float PULSE_SHARE = 0.1875F;
	private static final float FINAL_SHARE = 0.4375F;
	private static final float SHOT_SPEED = 1.6F;
	private static final Vector3f CHARGE_DUST = new Vector3f(0.65F, 0.3F, 1.0F);

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
		private Vec3 base;
		private Vec3 ground;
		private Vec3 hover;
		private Vec3 landing;
		private float yaw;

		public LivingEntity victim() {
			return this.victim != null ? this.victim.entity() : null;
		}
	}

	private MeteorBurstCombo() {}

	public static boolean tick(ServerLevel level, Performer performer, State state, int tick) {
		LivingEntity self = performer.self();
		self.fallDistance = 0.0F;

		if (tick < DASH_TICK) {
			LivingEntity target = performer.target();
			if (!HeldVictim.canTarget(self, target)) return false;
			state.yaw = yawTowards(self, target);
			performer.place(self.position(), state.yaw);
			if (tick == 1) playSound(level, self.position(), self, MainSounds.ZANZOKEN.get(), 1.5F, 1.0F);
			return true;
		}

		if (tick == DASH_TICK) {
			LivingEntity target = performer.target();
			if (!HeldVictim.canTarget(self, target) || target.level() != level || self.distanceTo(target) > RANGE + 6.0D) return false;
			dash(level, performer, state, target);
			state.victim = HeldVictim.canHold(self, target) ? HeldVictim.capture(target, DURATION + 20) : HeldVictim.root(target);
		}

		if (state.base == null) return false;
		if (state.victim == null) {
			if (state.landing == null) return false;
			double t = Mth.clamp((tick - FINAL_TICK) / (double) (DURATION - FINAL_TICK), 0.0D, 1.0D);
			performer.place(state.hover.lerp(state.landing, t), state.yaw);
			return true;
		}
		if (!state.victim.isValid(self, MAX_VICTIM_DISTANCE) || performer.isInterrupted()) return false;

		Vec3 facing = facing(state.yaw);
		float faceBack = state.yaw + 180.0F;

		if (tick <= SLAM_TICK) {
			double lift = LIFT_HEIGHT * Mth.clamp((tick - LIFT_START) / (double) (LIFT_END - LIFT_START), 0.0D, 1.0D);
			Vec3 at = state.base.add(0.0D, lift, 0.0D);
			performer.place(at, state.yaw);

			boolean hit = contains(HIT_TICKS, tick);
			Vec3 held = at.add(facing.scale(gap(self, state.victim.entity()) + (hit ? 0.35D : 0.0D)));
			state.victim.hold(held, faceBack);
			if (hit) strikeHit(level, performer, state, tick == SLAM_TICK);
			if (tick == SLAM_TICK) state.ground = state.victim.isPinned()
					? new Vec3(held.x, groundHeight(level, held.x, held.z, held.y), held.z)
					: state.victim.entity().position();
			return true;
		}

		if (state.ground == null) return false;
		Vec3 top = state.base.add(0.0D, LIFT_HEIGHT, 0.0D);

		if (tick <= LAND_TICK) {
			double t = (tick - SLAM_TICK) / (double) (LAND_TICK - SLAM_TICK);
			performer.place(top, state.yaw);
			state.victim.hold(top.add(facing.scale(gap(self, state.victim.entity()))).lerp(state.ground, t), faceBack);
			if (tick == LAND_TICK) groundImpact(level, self, state.ground, 4.0F);
			return true;
		}

		state.victim.hold(state.ground, faceBack);
		Vec3 onVictim = state.ground.add(facing.scale(-0.5D)).add(0.0D, 0.5D, 0.0D);
		if (state.hover == null) state.hover = state.ground.add(facing.scale(-HOVER_BACK)).add(0.0D, HOVER_HEIGHT, 0.0D);

		if (tick <= STOMP_TICK) {
			double t = Mth.clamp((tick - DROP_START + 1) / (double) (STOMP_TICK - DROP_START + 1), 0.0D, 1.0D);
			performer.place(top.lerp(onVictim, t), state.yaw);
			if (tick == STOMP_TICK) stomp(level, performer, state);
			return true;
		}

		if (tick < FIRE_START) {
			double t = Mth.clamp((tick - RISE_START + 1) / (double) (RISE_END - RISE_START), 0.0D, 1.0D);
			performer.place(onVictim.lerp(state.hover, t), state.yaw);
			if (tick == RISE_END) playSound(level, state.hover, self, MainSounds.KI_EXPLOSION_CHARGE.get(), 2.0F, 0.7F);
			if (tick > RISE_END && tick % 2 == 0) chargeParticles(level, self, state);
			return true;
		}

		performer.place(state.hover, state.yaw);
		if (tick <= FIRE_END) {
			fireShots(level, performer, state, 2);
			if (tick % 2 == 0) {
				playSound(level, state.hover, self, MainSounds.KIBLAST_ATTACK.get(), 0.8F, 1.3F + self.getRandom().nextFloat() * 0.4F);
			}
			if (contains(PULSE_TICKS, tick)) {
				LivingEntity victim = state.victim.entity();
				victim.invulnerableTime = 0;
				performer.kiHit(victim, performer.kiBudget() * PULSE_SHARE, false);
			}
			return true;
		}

		if (tick == FINAL_TICK) finale(level, performer, state);
		return true;
	}

	public static void release(State state) {
		if (state.victim != null) {
			state.victim.release();
			state.victim = null;
		}
		state.base = null;
		state.ground = null;
		state.hover = null;
		state.landing = null;
	}

	private static void dash(ServerLevel level, Performer performer, State state, LivingEntity target) {
		HeldVictim.Approach approach = HeldVictim.approach(level, performer.self(), target, gap(performer.self(), target));
		state.yaw = approach.yaw();
		state.base = approach.destination();
		performer.place(state.base, state.yaw);
	}

	private static void strikeHit(ServerLevel level, Performer performer, State state, boolean slam) {
		LivingEntity self = performer.self();
		LivingEntity target = state.victim.entity();
		target.invulnerableTime = 0;
		performer.strikeHit(target, performer.strikeBudget() * HIT_SHARE, false);

		Vec3 center = target.getBoundingBox().getCenter();
		NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(center, facing(state.yaw), slam ? 1.8F : 1.2F,
				0xFFFFFF, 0xFFD27F, true, 8), self);
		RandomSource random = self.getRandom();
		SoundEvent sound = slam ? MainSounds.CRITICO2.get() : (random.nextBoolean() ? MainSounds.GOLPE1.get() : MainSounds.GOLPE3.get());
		playSound(level, center, self, sound, 1.6F, 0.9F + random.nextFloat() * 0.2F);

		if (slam) state.victim.knockDown();
		else state.victim.flinch();
	}

	private static void stomp(ServerLevel level, Performer performer, State state) {
		LivingEntity victim = state.victim.entity();
		victim.invulnerableTime = 0;
		performer.strikeHit(victim, performer.strikeBudget() * STOMP_SHARE, false);
		groundImpact(level, performer.self(), state.ground, 6.0F);
		playSound(level, state.ground, performer.self(), MainSounds.ANCHOR_SLAM.get(), 2.5F, 0.8F);
	}

	private static void groundImpact(ServerLevel level, LivingEntity self, Vec3 at, float scale) {
		level.sendParticles(MainParticles.ROCK.get(), at.x, at.y + 0.2D, at.z, 40, scale * 0.3D, 0.3D, scale * 0.3D, 0.25D);
		level.sendParticles(MainParticles.DUST.get(), at.x, at.y + 0.2D, at.z, 60, scale * 0.4D, 0.4D, scale * 0.4D, 0.1D);
		NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(at.x, at.y + 0.2D, at.z, scale, 0xFFE0A0, 12), self);
		playSound(level, at, self, MainSounds.KNOCKBACK_CHARACTER.get(), 2.0F, 0.7F);
	}

	private static Vec3 hands(LivingEntity self, State state) {
		return self.position().add(0.0D, self.getBbHeight() * 0.7D, 0.0D).add(facing(state.yaw).scale(0.6D));
	}

	private static void chargeParticles(ServerLevel level, LivingEntity self, State state) {
		Vec3 hands = hands(self, state);
		RandomSource random = self.getRandom();
		level.sendParticles(new DustParticleOptions(CHARGE_DUST, 1.5F), hands.x, hands.y, hands.z, 4, 0.25D, 0.25D, 0.25D, 0.0D);
		for (int i = 0; i < 6; i++) {
			double ox = (random.nextDouble() - 0.5D) * 3.0D;
			double oy = (random.nextDouble() - 0.5D) * 3.0D;
			double oz = (random.nextDouble() - 0.5D) * 3.0D;
			level.sendParticles(ParticleTypes.END_ROD, hands.x + ox, hands.y + oy, hands.z + oz, 0, -ox, -oy, -oz, 0.08D);
		}
	}

	private static void fireShots(ServerLevel level, Performer performer, State state, int count) {
		LivingEntity self = performer.self();
		RandomSource random = self.getRandom();
		Vec3 from = hands(self, state);
		Vec3 aim = state.ground.add(0.0D, 0.5D, 0.0D);
		float damage = performer.kiBudget() * SHOT_SHARE;

		for (int i = 0; i < count; i++) {
			Vec3 spread = new Vec3((random.nextDouble() - 0.5D) * 1.6D, (random.nextDouble() - 0.5D) * 0.6D,
					(random.nextDouble() - 0.5D) * 1.6D);
			Vec3 dir = aim.add(spread).subtract(from).normalize();

			KiBlastEntity shot = new KiBlastEntity(level, self);
			shot.setKiRenderType(0);
			shot.setSize(0.9F);
			shot.setKiSpeed(SHOT_SPEED);
			shot.setKiDamage(damage);
			if (random.nextBoolean()) shot.setColors(COLOR_MAIN, COLOR_BORDER, COLOR_OUTLINE);
			else shot.setColors(COLOR_MAIN, COLOR_OUTLINE, COLOR_BORDER);
			shot.setFiring(true);
			shot.setCastTime(0);
			shot.setMaxLife(30);
			shot.setBlockDestructionEnabled(false);
			shot.setPos(from.x, from.y, from.z);
			shot.setDeltaMovement(dir.scale(SHOT_SPEED));
			shot.hasImpulse = true;
			level.addFreshEntity(shot);
		}
	}

	private static void finale(ServerLevel level, Performer performer, State state) {
		LivingEntity self = performer.self();
		LivingEntity victim = state.victim.entity();
		Vec3 at = state.ground;
		state.victim.release();
		state.victim = null;
		state.landing = new Vec3(state.hover.x, groundHeight(level, state.hover.x, state.hover.z, state.hover.y), state.hover.z);

		float damage = performer.kiBudget() * FINAL_SHARE;
		for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(FINAL_RADIUS))) {
			if (hit == self || !hit.isAlive() || hit.isSpectator()) continue;
			if (hit != victim && !performer.canHitArea(hit)) continue;
			if (hit.getBoundingBox().getCenter().distanceToSqr(at) > FINAL_RADIUS * FINAL_RADIUS) continue;

			hit.invulnerableTime = 0;
			performer.kiHit(hit, damage, true);
			if (!hit.isAlive()) continue;
			Vec3 push = hit.position().subtract(at);
			push = push.lengthSqr() < 1.0E-4D ? Vec3.ZERO : push.normalize().scale(0.8D);
			KnockbackHelper.apply(hit, new Vec3(push.x, 0.6D, push.z));
		}

		KiExplosionVisualEntity visual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), level);
		visual.setupExplosion(COLOR_MAIN, COLOR_BORDER, COLOR_OUTLINE, 3.5F);
		visual.moveTo(at.x, at.y + 0.5D, at.z, 0.0F, 0.0F);
		level.addFreshEntity(visual);
		NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(at.x, at.y + 0.2D, at.z, (float) FINAL_RADIUS * 1.8F,
				COLOR_BORDER, 18), self);
		playSound(level, at, self, MainSounds.KI_EXPLOSION_IMPACT.get(), 3.5F, 0.7F);
	}

	private static void playSound(ServerLevel level, Vec3 at, LivingEntity self, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, at.x, at.y, at.z, sound, self.getSoundSource(), volume, pitch);
	}

	private static boolean contains(int[] ticks, int tick) {
		for (int value : ticks) {
			if (value == tick) return true;
		}
		return false;
	}

	private static double gap(LivingEntity self, LivingEntity target) {
		return self.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D + VICTIM_GAP;
	}

	private static Vec3 facing(float yaw) {
		return Vec3.directionFromRotation(0.0F, yaw);
	}

	private static float yawTowards(LivingEntity self, LivingEntity target) {
		double dx = target.getX() - self.getX();
		double dz = target.getZ() - self.getZ();
		if (dx * dx + dz * dz < 1.0E-6D) return self.getYRot();
		return (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
	}

	private static double groundHeight(ServerLevel level, double x, double z, double fromY) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		int ix = Mth.floor(x);
		int iz = Mth.floor(z);
		for (int y = Mth.floor(fromY) + 2; y >= level.getMinBuildHeight(); y--) {
			cursor.set(ix, y, iz);
			if (!level.getBlockState(cursor).isAir()) return y + 1.0D;
		}
		return fromY;
	}
}
