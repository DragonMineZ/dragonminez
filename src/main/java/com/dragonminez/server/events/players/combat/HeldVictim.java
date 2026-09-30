package com.dragonminez.server.events.players.combat;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AfterimageVfxS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HeldVictim {

	private static final String PLAYER_HURT_LEFT = "base.hurt_left";
	private static final String PLAYER_HURT_RIGHT = "base.hurt_right";
	private static final String PLAYER_HURT_DOWN = "base.hurt_down";

	private final LivingEntity entity;
	private final boolean pinned;
	private String sagaPose;
	private boolean fromLeft;

	public record Approach(Vec3 destination, float yaw) {}

	private HeldVictim(LivingEntity entity, boolean pinned) {
		this.entity = entity;
		this.pinned = pinned;
	}

	public static Approach approach(ServerLevel level, LivingEntity self, LivingEntity target, double gap) {
		Vec3 from = self.position();
		Vec3 flat = new Vec3(target.getX() - from.x, 0.0D, target.getZ() - from.z);
		Vec3 dir = flat.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, self.getYRot()) : flat.normalize();
		float yaw = (float) (Mth.atan2(dir.z, dir.x) * (180.0D / Math.PI)) - 90.0F;

		Vec3 destination = new Vec3(target.getX() - dir.x * gap, target.getY(), target.getZ() - dir.z * gap);
		if (!level.noCollision(self, self.getBoundingBox().move(destination.subtract(from)))) destination = from;

		NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(self.getId(), 40, new Vec3[]{from}, new float[]{self.getYRot()}, false), self);
		level.sendParticles(ParticleTypes.CLOUD, from.x, from.y + 1.0D, from.z, 12, 0.3D, 0.6D, 0.3D, 0.02D);
		level.playSound(null, destination.x, destination.y, destination.z, MainSounds.ZANZOKEN.get(), self.getSoundSource(), 1.5F, 1.2F);
		return new Approach(destination, yaw);
	}

	public static boolean canTarget(LivingEntity holder, LivingEntity target) {
		if (target == null || target == holder || !target.isAlive() || target.isRemoved()) return false;
		return !(target instanceof Player player && (player.isCreative() || player.isSpectator()));
	}

	public static boolean canHold(LivingEntity holder, LivingEntity target) {
		if (!canTarget(holder, target)) return false;
		if (target instanceof WorldBossEntity) return false;
		return !(target instanceof DBSagasEntity saga && saga.hasHitboxParts());
	}

	public static HeldVictim capture(LivingEntity target, int stunTicks) {
		StrikeAttackHandler.setStrikeLocked(target, true);
		if (!(target instanceof ServerPlayer)) {
			target.addEffect(new MobEffectInstance(MainEffects.STUN.get(), stunTicks, 0, false, false, true));
		}
		return new HeldVictim(target, true);
	}

	public static HeldVictim root(LivingEntity target) {
		return new HeldVictim(target, false);
	}

	public LivingEntity entity() {
		return this.entity;
	}

	public boolean isPinned() {
		return this.pinned;
	}

	public boolean isValid(LivingEntity holder, double maxDistance) {
		if (!this.entity.isAlive() || this.entity.isRemoved() || this.entity.level() != holder.level()) return false;
		if (this.entity.distanceToSqr(holder) > maxDistance * maxDistance) return false;
		if (this.entity instanceof Player player) {
			if (player.isSpectator() || player.isCreative()) return false;
			return !StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> data.getStatus().isKnockedDown()).orElse(false);
		}
		return true;
	}

	public void hold(Vec3 position, float faceYaw) {
		if (!this.pinned) return;
		this.entity.fallDistance = 0.0F;
		StrikeAttackHandler.placeHeldVictim(this.entity, position.x, position.y, position.z, faceYaw);
	}

	public void flinch() {
		this.fromLeft = !this.fromLeft;
		this.pose(this.fromLeft ? PLAYER_HURT_LEFT : PLAYER_HURT_RIGHT,
				this.fromLeft ? DBSagasEntity.HURT_ANIM_LEFT : DBSagasEntity.HURT_ANIM_RIGHT);
	}

	public void knockDown() {
		this.pose(PLAYER_HURT_DOWN, DBSagasEntity.HURT_ANIM_DOWN);
	}

	public void grabbed() {
		this.pose(StrikeAttackHandler.GRABBED_ANIM, DBSagasEntity.HURT_ANIM_GRABBED);
	}

	private void pose(String playerAnim, String sagaAnim) {
		if (!this.pinned) return;
		if (this.entity instanceof ServerPlayer player) {
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(),
					TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, playerAnim), player);
		} else if (this.entity instanceof DBSagasEntity saga) {
			saga.triggerAnim(DBSagasEntity.HURT_CONTROLLER, sagaAnim);
			this.sagaPose = sagaAnim;
		}
	}

	public void release() {
		if (!this.pinned) return;
		StrikeAttackHandler.setStrikeLocked(this.entity, false);
		if (!(this.entity instanceof ServerPlayer)) this.entity.removeEffect(MainEffects.STUN.get());
		if (this.entity instanceof ServerPlayer player) {
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(),
					TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), player);
		} else if (this.entity instanceof DBSagasEntity saga && this.sagaPose != null) {
			saga.stopTriggeredAnimation(DBSagasEntity.HURT_CONTROLLER, this.sagaPose);
		}
		this.sagaPose = null;
	}
}
