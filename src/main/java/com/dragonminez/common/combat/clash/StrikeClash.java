package com.dragonminez.common.combat.clash;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.BeamClashStateS2C;
import com.dragonminez.common.network.S2C.ImpactBurstVfxS2C;
import com.dragonminez.common.network.S2C.ShockwaveVfxS2C;
import com.dragonminez.common.network.S2C.StrikeClashHoldS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.combat.KnockbackHelper;
import com.dragonminez.server.events.players.combat.MomentumImpactHandler;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class StrikeClash implements BeamClashManager.ExternalClash {
	private static final double TRIGGER_GAP = 4.5;
	private static final double HEADING_DOT = 0.35;
	private static final double SIZE_DOMINANCE_RATIO = 2.0;
	private static final double REACH_PER_HEIGHT = 1.05 / 1.8;
	private static final double MIN_REACH = 0.9;
	private static final double FIST_HEIGHT_RATIO = 0.62;
	private static final double SLIDE_DISTANCE = 0.6;
	private static final double ANCHOR_DRIFT_SQR = 1.0;
	private static final float NPC_POWER_FACTOR = 1.5F;
	private static final int MAX_DURATION = 240;
	private static final int IDLE_TIE_TICKS = 100;
	private static final double LOSER_DAMAGE_RATIO = 0.5;
	private static final float IMPACT_DAMAGE_RATIO = 0.2F;
	private static final double LOSER_KNOCKBACK = 2.6;
	private static final double LOSER_KNOCKBACK_LIFT = 0.45;
	private static final int LOSER_EXHAUST_DELAY = 8;
	private static final double TIE_KNOCKBACK = 1.4;
	private static final double TIE_KNOCKBACK_LIFT = 0.3;
	private static final float TIE_COOLDOWN_RATIO = 0.5F;
	private static final int SHOCKWAVE_INTERVAL = 5;
	private static final int DUST_INTERVAL = 3;
	private static final int PUNCH_SOUND_INTERVAL = 9;
	private static final double DUST_GROUND_REACH = 3.0;
	private static final String POSE_ANIM = "skp.strike_clash";

	@SuppressWarnings("unchecked")
	private static final DeferredHolder<SoundEvent, ? extends SoundEvent>[] PUNCH_SOUNDS = new DeferredHolder[]{
			MainSounds.GOLPE1, MainSounds.GOLPE2, MainSounds.GOLPE3,
			MainSounds.GOLPE4, MainSounds.GOLPE5, MainSounds.GOLPE6
	};

	private static final List<StrikeClash> ACTIVE = new ArrayList<>();
	private static final List<PendingExhaust> PENDING_EXHAUST = new ArrayList<>();

	private enum Result { ONGOING, A_WINS, B_WINS, TIE, DISSOLVED }

	private record Side(LivingEntity entity, ClashParticipant participant, StrikeAttackHandler.ClashRush rush,
	                    Vec3 anchor, float yaw, int color) {}

	private record PendingExhaust(LivingEntity entity, long dueTime) {}

	private final Side a;
	private final Side b;
	private final ServerLevel level;
	private final Vec3 axis;
	private final Vec3 baseClashPoint;
	private final long startGameTime;
	private final long startNanos = System.nanoTime();
	private float bias = 0.5F;
	private int age;
	private double slide;

	private StrikeClash(ServerLevel level, Side a, Side b, Vec3 axis, Vec3 clashPoint) {
		this.level = level;
		this.a = a;
		this.b = b;
		this.axis = axis;
		this.baseClashPoint = clashPoint;
		this.startGameTime = level.getGameTime();
	}

	public static boolean tryStart(LivingEntity self) {
		if (!(self.level() instanceof ServerLevel level) || !self.isAlive()) return false;
		if (!isRushing(self) || BeamClashManager.isClashing(self.getUUID())) return false;

		AABB area = self.getBoundingBox().inflate(TRIGGER_GAP + 2.0);
		LivingEntity best = null;
		double bestGap = Double.MAX_VALUE;
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != self && e.isAlive() && isRushing(e))) {
			if (BeamClashManager.isClashing(other.getUUID()) || areAllies(self, other)) continue;
			double gap = gap(self, other);
			if (gap > TRIGGER_GAP || !headingTowards(self, other) || !headingTowards(other, self)) continue;
			if (dwarfs(other, self)) {
				cancelRush(self);
				return true;
			}
			if (dwarfs(self, other)) {
				cancelRush(other);
				continue;
			}
			if (gap < bestGap) {
				best = other;
				bestGap = gap;
			}
		}
		if (best == null) return false;
		start(level, self, best);
		return true;
	}

	public static boolean isClashing(UUID id) {
		for (StrikeClash clash : ACTIVE) {
			if (clash.involves(id)) return true;
		}
		return false;
	}

	public static boolean isRushingAt(LivingEntity rusher, LivingEntity target) {
		return isRushing(rusher) && !BeamClashManager.isClashing(rusher.getUUID())
				&& !areAllies(rusher, target) && headingTowards(rusher, target);
	}

	private static boolean isRushing(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) return StrikeAttackHandler.isRushing(player);
		return entity instanceof DBSagasEntity saga && saga.isStrikeRushing() && !saga.isStrikeClashing();
	}

	private static boolean areAllies(LivingEntity a, LivingEntity b) {
		if (a instanceof Player playerA && b instanceof Player playerB) return TargetHelper.areFriendlyPlayers(playerA, playerB);
		if (a instanceof Player player) return TargetHelper.getRelation(player, b) == TargetHelper.Relation.FRIENDLY || b.isAlliedTo(a);
		if (b instanceof Player player) return TargetHelper.getRelation(player, a) == TargetHelper.Relation.FRIENDLY || a.isAlliedTo(b);
		return a.isAlliedTo(b) || b.isAlliedTo(a);
	}

	private static boolean dwarfs(LivingEntity big, LivingEntity small) {
		return big.getBbHeight() >= small.getBbHeight() * SIZE_DOMINANCE_RATIO;
	}

	private static void cancelRush(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) StrikeAttackHandler.cancelRush(player);
		else if (entity instanceof DBSagasEntity saga) saga.stopCombo();
	}

	private static double gap(LivingEntity a, LivingEntity b) {
		double centers = a.getBoundingBox().getCenter().distanceTo(b.getBoundingBox().getCenter());
		return centers - (a.getBbWidth() + b.getBbWidth()) * 0.5;
	}

	private static boolean headingTowards(LivingEntity self, LivingEntity other) {
		Vec3 heading;
		if (self instanceof ServerPlayer player) {
			if (other.getUUID().equals(StrikeAttackHandler.rushTargetId(player))) return true;
			heading = player.getLookAngle();
		} else if (self instanceof DBSagasEntity saga) {
			LivingEntity target = saga.getStrikeRushTarget();
			if (target == other) return true;
			heading = target != null ? target.getBoundingBox().getCenter().subtract(saga.getBoundingBox().getCenter()) : saga.getLookAngle();
		} else return false;
		Vec3 toOther = other.getBoundingBox().getCenter().subtract(self.getBoundingBox().getCenter());
		if (toOther.lengthSqr() < 1.0E-6 || heading.lengthSqr() < 1.0E-6) return true;
		return heading.normalize().dot(toOther.normalize()) >= HEADING_DOT;
	}

	private static void start(ServerLevel level, LivingEntity first, LivingEntity second) {
		StrikeAttackHandler.ClashRush rushA = first instanceof ServerPlayer playerA ? StrikeAttackHandler.takeRushForClash(playerA) : null;
		StrikeAttackHandler.ClashRush rushB = second instanceof ServerPlayer playerB ? StrikeAttackHandler.takeRushForClash(playerB) : null;
		if (first instanceof DBSagasEntity sagaA) sagaA.beginStrikeClash();
		if (second instanceof DBSagasEntity sagaB) sagaB.beginStrikeClash();

		Vec3 posA = first.position();
		Vec3 posB = second.position();
		Vec3 axis = new Vec3(posB.x - posA.x, 0.0, posB.z - posA.z);
		if (axis.lengthSqr() < 1.0E-6) axis = Vec3.directionFromRotation(0.0F, first.getYRot());
		axis = axis.normalize();

		Vec3 mid = posA.add(posB).scale(0.5);
		boolean groundedA = first.onGround();
		boolean groundedB = second.onGround();
		double baseY = groundedA && !groundedB ? posA.y : groundedB && !groundedA ? posB.y : mid.y;
		Vec3 anchorA = pickAnchor(level, first, mid.x - axis.x * reach(first), groundedA ? posA.y : baseY, mid.z - axis.z * reach(first));
		Vec3 anchorB = pickAnchor(level, second, mid.x + axis.x * reach(second), groundedB ? posB.y : baseY, mid.z + axis.z * reach(second));

		float yawA = (float) (Mth.atan2(axis.z, axis.x) * Mth.RAD_TO_DEG) - 90.0F;
		float yawB = Mth.wrapDegrees(yawA + 180.0F);
		double fistY = (anchorA.y + first.getBbHeight() * FIST_HEIGHT_RATIO + anchorB.y + second.getBbHeight() * FIST_HEIGHT_RATIO) * 0.5;
		Vec3 clashPoint = new Vec3(mid.x, fistY, mid.z);

		Side sideA = new Side(first, new ClashParticipant(first, power(first)), rushA, anchorA, yawA, auraColor(first));
		Side sideB = new Side(second, new ClashParticipant(second, power(second)), rushB, anchorB, yawB, auraColor(second));
		StrikeClash clash = new StrikeClash(level, sideA, sideB, axis, clashPoint);

		clash.enter(sideA);
		clash.enter(sideB);
		ACTIVE.add(clash);
		BeamClashManager.registerExternal(clash);
		clash.playOpening();
	}

	private static double reach(LivingEntity entity) {
		return Math.max(MIN_REACH, entity.getBbHeight() * REACH_PER_HEIGHT);
	}

	private static Vec3 pickAnchor(ServerLevel level, LivingEntity entity, double x, double y, double z) {
		Vec3 shared = new Vec3(x, y, z);
		if (fits(level, entity, shared)) return shared;
		Vec3 own = new Vec3(x, entity.getY(), z);
		if (fits(level, entity, own)) return own;
		return entity.position();
	}

	private static boolean fits(ServerLevel level, LivingEntity entity, Vec3 pos) {
		return level.noCollision(entity, entity.getBoundingBox().move(pos.subtract(entity.position())));
	}

	private static double power(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			return StatsProvider.get(StatsCapability.INSTANCE, player)
					.map(stats -> stats.getMeleeDamage() + stats.getStrikeDamage())
					.orElse(1.0);
		}
		if (!entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) return 1.0;
		return entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * NPC_POWER_FACTOR;
	}

	private static int auraColor(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			return StatsProvider.get(StatsCapability.INSTANCE, player).map(stats -> {
				float[] rgb = stats.getCharacter().getActiveRgbAuraColor();
				return ColorUtils.rgbToInt(rgb[0], rgb[1], rgb[2]);
			}).orElse(0xFFFFFF);
		}
		if (entity instanceof DBSagasEntity saga) return saga.getAuraColor();
		return 0xFFFFFF;
	}

	private void enter(Side side) {
		if (side.entity() instanceof ServerPlayer player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> stats.getStatus().setAuraActive(true));
			StrikeAttackHandler.setStrikeLocked(player, true);
			player.connection.teleport(side.anchor().x, side.anchor().y, side.anchor().z, side.yaw(), 0.0F);
			NetworkHandler.sendToPlayer(new StrikeClashHoldS2C(true, side.anchor().x, side.anchor().y, side.anchor().z, side.yaw()), player);
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(),
					TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, POSE_ANIM), player);
		} else {
			side.participant().freezeOwner();
			place(side, side.anchor());
		}
	}

	private static void place(Side side, Vec3 pos) {
		LivingEntity entity = side.entity();
		entity.setPos(pos.x, pos.y, pos.z);
		entity.setDeltaMovement(Vec3.ZERO);
		entity.fallDistance = 0.0F;
		entity.setYRot(side.yaw());
		entity.setXRot(0.0F);
		entity.setYHeadRot(side.yaw());
		entity.yBodyRot = side.yaw();
	}

	private Side sideOf(UUID id) {
		if (a.entity().getUUID().equals(id)) return a;
		if (b.entity().getUUID().equals(id)) return b;
		return null;
	}

	private Side other(Side side) {
		return side == a ? b : a;
	}

	@Override
	public boolean involves(UUID ownerId) {
		return sideOf(ownerId) != null;
	}

	@Override
	public boolean handlePress(ServerPlayer player, float pressTime, float marker) {
		Side side = sideOf(player.getUUID());
		if (side == null) return false;
		float serverTime = (float) (level.getGameTime() - startGameTime);
		float realElapsed = (System.nanoTime() - startNanos) / 50_000_000.0f;
		ClashMeter.Grade grade = side.participant().registerPlayerPress(pressTime, marker, serverTime, realElapsed, player.connection.latency());
		BeamClashManager.playPressFeedback(player, grade);
		return true;
	}

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel level)) return;

		ACTIVE.removeIf(clash -> {
			if (clash.level != level) return false;
			Result result = clash.tick();
			if (result == Result.ONGOING) return false;
			BeamClashManager.unregisterExternal(clash);
			clash.conclude(result);
			return true;
		});

		long now = level.getGameTime();
		PENDING_EXHAUST.removeIf(pending -> {
			if (pending.entity().level() != level) return pending.entity().isRemoved();
			if (now < pending.dueTime()) return false;
			if (pending.entity().isAlive()) BeamClash.exhaust(pending.entity());
			return true;
		});
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		ACTIVE.removeIf(clash -> {
			if (clash.level != event.getLevel()) return false;
			BeamClashManager.unregisterExternal(clash);
			return true;
		});
		PENDING_EXHAUST.removeIf(pending -> pending.entity().level() == event.getLevel());
	}

	private boolean valid(Side side) {
		LivingEntity entity = side.entity();
		return entity.isAlive() && !entity.isRemoved() && entity.level() == level;
	}

	private Result tick() {
		if (!valid(a) || !valid(b)) return Result.DISSOLVED;
		age++;

		a.participant().tickMeter(age);
		b.participant().tickMeter(age);
		bias = BeamClash.advanceBias(bias, a.participant(), b.participant());

		float visual = BeamClash.visualBias(bias);
		slide = (visual - 0.5F) * 2.0F * SLIDE_DISTANCE;
		hold(a);
		hold(b);
		playStruggle(visual);
		sync(visual);

		if (bias >= BeamClash.WIN_THRESHOLD) return Result.A_WINS;
		if (bias <= 1.0F - BeamClash.WIN_THRESHOLD) return Result.B_WINS;
		if (Math.min(a.participant().idleTicks(), b.participant().idleTicks()) >= IDLE_TIE_TICKS) return Result.TIE;
		if (age >= MAX_DURATION) {
			if (bias > 0.5F) return Result.A_WINS;
			if (bias < 0.5F) return Result.B_WINS;
			return Result.TIE;
		}
		return Result.ONGOING;
	}

	private Vec3 slid(Vec3 pos) {
		return pos.add(axis.scale(slide));
	}

	private Vec3 clashPoint() {
		return slid(baseClashPoint);
	}

	private void hold(Side side) {
		Vec3 anchor = slid(side.anchor());
		if (!fits(level, side.entity(), anchor)) anchor = side.anchor();
		if (side.entity() instanceof ServerPlayer player) {
			player.setDeltaMovement(Vec3.ZERO);
			player.fallDistance = 0.0F;
			if (player.position().distanceToSqr(anchor) > ANCHOR_DRIFT_SQR) {
				player.connection.teleport(anchor.x, anchor.y, anchor.z, side.yaw(), 0.0F);
			}
			NetworkHandler.sendToPlayer(new StrikeClashHoldS2C(true, anchor.x, anchor.y, anchor.z, side.yaw()), player);
		} else {
			side.participant().freezeOwner();
			if (side.entity() instanceof DBSagasEntity saga) saga.getNavigation().stop();
			place(side, anchor);
		}
	}

	private void sync(float visual) {
		Vec3 point = clashPoint();
		for (Side side : new Side[]{a, b}) {
			if (!(side.entity() instanceof ServerPlayer player)) continue;
			Side foe = other(side);
			float advantage = side == a ? visual : 1.0F - visual;
			NetworkHandler.sendToPlayer(new BeamClashStateS2C(true, startGameTime, side.participant().meterSeed(), advantage,
					side.color(), foe.color(), foe.entity().getId(), point.x, point.y, point.z, 0), player);
		}
	}

	private void broadcast(Object message) {
		NetworkHandler.sendToTrackingEntityAndSelf(message, a.entity());
	}

	private void playOpening() {
		Vec3 point = clashPoint();
		broadcast(new ImpactBurstVfxS2C(point, axis, 5.0F, a.color(), b.color(), true, 16));
		broadcast(new ShockwaveVfxS2C(point.x, point.y, point.z, 6.0F, a.color(), 14));
		level.playSound(null, point.x, point.y, point.z, MainSounds.CRITICO2.get(), SoundSource.PLAYERS, 2.5F, 0.8F);
		level.playSound(null, point.x, point.y, point.z, MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.PLAYERS, 1.5F, 1.2F);
	}

	private void playStruggle(float visual) {
		Vec3 point = clashPoint();
		float decisiveness = Math.abs(visual - 0.5F) * 2.0F;

		if (age % SHOCKWAVE_INTERVAL == 0) {
			int color = (age / SHOCKWAVE_INTERVAL) % 2 == 0 ? a.color() : b.color();
			broadcast(new ShockwaveVfxS2C(point.x, point.y, point.z, 2.4F + 1.6F * decisiveness, color, 9));
		}

		if (age % DUST_INTERVAL == 0) {
			int color = level.random.nextBoolean() ? a.color() : b.color();
			for (int i = 0; i < 3; i++) {
				level.sendParticles(MainParticles.SPARKS.get(),
						point.x + (level.random.nextDouble() - 0.5) * 0.4,
						point.y + (level.random.nextDouble() - 0.5) * 0.4,
						point.z + (level.random.nextDouble() - 0.5) * 0.4,
						0, ((color >> 16) & 0xFF) / 255.0, ((color >> 8) & 0xFF) / 255.0, (color & 0xFF) / 255.0, 1.0);
			}
			double groundY = Math.min(a.anchor().y, b.anchor().y);
			BlockPos below = BlockPos.containing(point.x, groundY - 0.5, point.z);
			for (int depth = 0; depth < DUST_GROUND_REACH; depth++) {
				if (level.getBlockState(below.below(depth)).isAir()) continue;
				double dustY = below.below(depth).getY() + 1.1;
				level.sendParticles(ParticleTypes.CLOUD, point.x, dustY, point.z, 6, 1.2, 0.05, 1.2, 0.12);
				break;
			}
		}

		if (age % PUNCH_SOUND_INTERVAL == 0) {
			SoundEvent sound = PUNCH_SOUNDS[level.random.nextInt(PUNCH_SOUNDS.length)].get();
			level.playSound(null, point.x, point.y, point.z, sound, SoundSource.PLAYERS, 1.2F, 0.8F + level.random.nextFloat() * 0.3F);
		}
	}

	private void conclude(Result result) {
		switch (result) {
			case A_WINS -> finish(a, b);
			case B_WINS -> finish(b, a);
			case TIE -> tie();
			default -> {
				release(a, null);
				release(b, null);
				settle(a, TIE_COOLDOWN_RATIO);
				settle(b, TIE_COOLDOWN_RATIO);
			}
		}
	}

	private void release(Side side, Boolean won) {
		if (side.entity() instanceof ServerPlayer player) {
			NetworkHandler.sendToPlayer(StrikeClashHoldS2C.release(), player);
			NetworkHandler.sendToPlayer(BeamClashStateS2C.inactive(0), player);
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> stats.getStatus().setAuraActive(false));
			StrikeAttackHandler.setStrikeLocked(player, false);
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(),
					TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), player);
		} else {
			side.participant().unfreezeOwner();
			if (side.entity() instanceof DBSagasEntity saga) saga.endStrikeClash(won);
		}
	}

	private static void settle(Side side, float cooldownRatio) {
		if (side.entity() instanceof ServerPlayer player && side.rush() != null) {
			StrikeAttackHandler.settleClashCooldown(player, side.rush(), cooldownRatio);
		}
	}

	private void finish(Side winner, Side loser) {
		release(winner, Boolean.TRUE);
		release(loser, Boolean.FALSE);
		settle(winner, 0.0F);
		settle(loser, 1.0F);

		Vec3 point = clashPoint();
		Vec3 dir = new Vec3(loser.entity().getX() - winner.entity().getX(), 0.0, loser.entity().getZ() - winner.entity().getZ());
		dir = dir.lengthSqr() < 1.0E-6 ? axis.scale(winner == a ? 1.0 : -1.0) : dir.normalize();

		LivingEntity victim = loser.entity();
		if (victim.isAlive()) {
			double damage = winnerStrikeDamage(winner, victim) * LOSER_DAMAGE_RATIO;
			victim.invulnerableTime = 0;
			if (damage > 0.0) victim.hurt(damageSource(winner), (float) damage);
			KnockbackHelper.apply(victim, new Vec3(dir.x * LOSER_KNOCKBACK, LOSER_KNOCKBACK_LIFT, dir.z * LOSER_KNOCKBACK));
			MomentumImpactHandler.registerCollisionImpact(victim,
					victim.onGround() ? MomentumImpactHandler.CollisionImpactType.GROUND : MomentumImpactHandler.CollisionImpactType.WALL,
					(float) (damage * IMPACT_DAMAGE_RATIO), dir);
			PENDING_EXHAUST.add(new PendingExhaust(victim, level.getGameTime() + LOSER_EXHAUST_DELAY));
		}

		broadcast(new ImpactBurstVfxS2C(point, dir, 5.5F, winner.color(), loser.color(), true, 18));
		broadcast(new ShockwaveVfxS2C(point.x, point.y, point.z, 7.0F, winner.color(), 14));
		level.playSound(null, point.x, point.y, point.z, MainSounds.CRITICO1.get(), SoundSource.PLAYERS, 2.5F, 0.8F);
	}

	private void tie() {
		release(a, null);
		release(b, null);
		settle(a, TIE_COOLDOWN_RATIO);
		settle(b, TIE_COOLDOWN_RATIO);

		KnockbackHelper.apply(a.entity(), new Vec3(-axis.x * TIE_KNOCKBACK, TIE_KNOCKBACK_LIFT, -axis.z * TIE_KNOCKBACK));
		KnockbackHelper.apply(b.entity(), new Vec3(axis.x * TIE_KNOCKBACK, TIE_KNOCKBACK_LIFT, axis.z * TIE_KNOCKBACK));

		Vec3 point = clashPoint();
		broadcast(new ImpactBurstVfxS2C(point, new Vec3(0.0, 1.0, 0.0), 4.5F, a.color(), b.color(), true, 16));
		broadcast(new ShockwaveVfxS2C(point.x, point.y, point.z, 5.0F, 0xFFFFFF, 12));
		level.playSound(null, point.x, point.y, point.z, MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.PLAYERS, 2.0F, 0.9F);
	}

	private static double winnerStrikeDamage(Side winner, LivingEntity victim) {
		if (winner.entity() instanceof ServerPlayer player && winner.rush() != null) {
			return StrikeAttackHandler.clashStrikeDamage(player, victim, winner.rush().techniqueId());
		}
		return winner.participant().statPower();
	}

	private DamageSource damageSource(Side winner) {
		if (winner.entity() instanceof ServerPlayer player && winner.rush() != null) {
			return MainDamageTypes.strikeAttack(level, player, winner.rush().techniqueId());
		}
		return winner.entity().damageSources().mobAttack(winner.entity());
	}
}
