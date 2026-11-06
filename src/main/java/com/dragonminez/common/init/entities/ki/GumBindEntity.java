package com.dragonminez.common.init.entities.ki;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.MobBattlePowerHelper;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.combat.HeldVictim;
import com.dragonminez.server.events.players.combat.KnockbackHelper;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GumBindEntity extends Entity implements GeoEntity {

	private static final EntityDataAccessor<Integer> VICTIM_ID = SynchedEntityData.defineId(GumBindEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(GumBindEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(GumBindEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> RELEASE_AT = SynchedEntityData.defineId(GumBindEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> STRUGGLES = SynchedEntityData.defineId(GumBindEntity.class, EntityDataSerializers.INT);

	public static final int MAX_BIND_TICKS = 200;
	public static final int MIN_NPC_BIND_TICKS = 40;
	public static final int STRAND_TICKS = 8;
	public static final String BOUND_ANIMATION = "evs.gum_bound";
	private static final int TICKS_PER_PRESS = 5;
	private static final int BURST_TICKS = 10;
	private static final int PROMPT_INTERVAL = 20;
	private static final int SAGA_POSE_INTERVAL = 40;
	private static final int NPC_STRUGGLE_MIN = 10;
	private static final int NPC_STRUGGLE_SPREAD = 14;
	private static final double LEASH_DISTANCE = 24.0D;
	private static final int METER_BARS = 20;
	private static final String ESCAPE_KEY = "key.sneak";
	private static final String TITLE = "message.dragonminez.technique.gum_bind.title";
	private static final String SUBTITLE = "message.dragonminez.technique.gum_bind.subtitle";
	private static final String PROGRESS = "message.dragonminez.technique.gum_bind.progress";
	private static final String ESCAPED = "message.dragonminez.technique.gum_bind.escaped";

	private static final RawAnimation WRAP = RawAnimation.begin().thenPlay("gum_bind.wrap").thenLoop("gum_bind.idle");
	private static final RawAnimation BURST = RawAnimation.begin().thenPlayAndHold("gum_bind.burst");
	private static final RawAnimation STRUGGLE = RawAnimation.begin().thenPlay("gum_bind.struggle");

	private static final Map<UUID, GumBindEntity> BOUND = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> CLIENT_BOUND = new ConcurrentHashMap<>();

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private UUID victimUuid;
	private int totalTicks;
	private int ticksLeft;
	private int nextNpcStruggle;
	private int seenStruggles;
	private boolean released;

	public GumBindEntity(EntityType<? extends GumBindEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	public static GumBindEntity bind(ServerLevel level, LivingEntity caster, LivingEntity victim, int color, int ticks) {
		GumBindEntity previous = BOUND.get(victim.getUUID());
		if (previous != null) previous.release(false);

		GumBindEntity gum = MainEntities.GUM_BIND.get().create(level);
		if (gum == null) return null;
		gum.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0.0F);
		gum.entityData.set(VICTIM_ID, victim.getId());
		gum.entityData.set(CASTER_ID, caster != null ? caster.getId() : -1);
		gum.entityData.set(COLOR, color);
		gum.victimUuid = victim.getUUID();
		gum.totalTicks = ticks;
		gum.ticksLeft = ticks;
		gum.nextNpcStruggle = NPC_STRUGGLE_MIN;
		if (!level.addFreshEntity(gum)) return null;

		BOUND.put(victim.getUUID(), gum);
		gum.lock(victim);
		gum.pose(victim);
		if (victim instanceof ServerPlayer player) gum.prompt(player);
		level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 2.0F, 0.6F);
		level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 2.0F, 0.8F);
		return gum;
	}

	public static boolean isBound(UUID entityId) {
		GumBindEntity gum = BOUND.get(entityId);
		return gum != null && gum.isActive();
	}

	public static boolean isClientBound(Entity entity) {
		Long seen = CLIENT_BOUND.get(entity.getId());
		return seen != null && entity.level().getGameTime() - seen <= 2L;
	}

	public static int bindTicks(LivingEntity caster, LivingEntity target) {
		if (target instanceof Player) return MAX_BIND_TICKS;
		double own = battlePowerOf(caster);
		double theirs = battlePowerOf(target);
		if (theirs <= own || own <= 0.0D) return MAX_BIND_TICKS;
		return Math.max(MIN_NPC_BIND_TICKS, (int) Math.round(MAX_BIND_TICKS * own / theirs));
	}

	private static double battlePowerOf(LivingEntity entity) {
		if (entity instanceof Player player) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data != null) return data.getBattlePowerExact();
		}
		if (entity instanceof IBattlePower battlePower) return battlePower.getBattlePower();
		return MobBattlePowerHelper.calculate(entity);
	}

	public static Vec3 hand(Vec3 feet, float yawDegrees, float scale) {
		float yaw = yawDegrees * Mth.DEG_TO_RAD;
		Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
		Vec3 right = new Vec3(-Mth.cos(yaw), 0.0D, -Mth.sin(yaw));
		return feet.add(0.0D, 1.0D * scale, 0.0D).add(forward.scale(1.2D * scale)).add(right.scale(0.3D * scale));
	}

	public static void struggle(ServerPlayer player) {
		GumBindEntity gum = BOUND.get(player.getUUID());
		if (gum == null || !gum.isActive() || gum.level() != player.level()) return;

		gum.ticksLeft -= TICKS_PER_PRESS;
		gum.pulse();
		if (gum.ticksLeft <= 0) {
			player.displayClientMessage(Component.translatable(ESCAPED).withStyle(ChatFormatting.GREEN), true);
			gum.release(true);
			return;
		}
		player.displayClientMessage(Component.translatable(PROGRESS, gum.meter()), true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(VICTIM_ID, -1);
		builder.define(CASTER_ID, -1);
		builder.define(COLOR, 0xF4A0B6);
		builder.define(RELEASE_AT, -1);
		builder.define(STRUGGLES, 0);
	}

	public int victimId() {
		return this.entityData.get(VICTIM_ID);
	}

	public int casterId() {
		return this.entityData.get(CASTER_ID);
	}

	public int color() {
		return this.entityData.get(COLOR);
	}

	public int releaseAt() {
		return this.entityData.get(RELEASE_AT);
	}

	public boolean isReleasing() {
		return this.releaseAt() >= 0;
	}

	private boolean isActive() {
		return !this.released && !this.isRemoved();
	}

	public LivingEntity victim() {
		return this.level().getEntity(this.victimId()) instanceof LivingEntity living ? living : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(this.level() instanceof ServerLevel level)) {
			this.clientTick();
			return;
		}

		if (this.isReleasing()) {
			if (this.tickCount - this.releaseAt() >= BURST_TICKS) this.discard();
			return;
		}

		LivingEntity victim = this.victim();
		if (!this.holds(victim)) {
			this.release(false);
			return;
		}

		this.setPos(victim.getX(), victim.getY(), victim.getZ());
		this.setYRot(victim.getYRot());
		victim.fallDistance = 0.0F;
		this.lock(victim);

		if (--this.ticksLeft <= 0) {
			this.release(false);
			return;
		}

		if (victim instanceof ServerPlayer player) {
			if (this.tickCount % PROMPT_INTERVAL == 0) {
				this.pose(player);
				this.prompt(player);
			}
		} else {
			if (this.tickCount % SAGA_POSE_INTERVAL == 0) this.pose(victim);
			if (--this.nextNpcStruggle <= 0) {
				this.nextNpcStruggle = NPC_STRUGGLE_MIN + this.random.nextInt(NPC_STRUGGLE_SPREAD);
				this.pulse();
			}
		}
		if (this.tickCount % 6 == 0) {
			level.sendParticles(new DustParticleOptions(this.dustColor(), 1.2F), victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D,
					victim.getZ(), 1, victim.getBbWidth() * 0.6D, victim.getBbHeight() * 0.3D, victim.getBbWidth() * 0.6D, 0.0D);
		}
	}

	private boolean holds(LivingEntity victim) {
		if (victim == null || !victim.isAlive() || victim.isRemoved() || this.victimUuid == null) return false;
		if (!victim.getUUID().equals(this.victimUuid) || victim.level() != this.level()) return false;
		if (victim.distanceToSqr(this) > LEASH_DISTANCE * LEASH_DISTANCE) return false;
		if (victim instanceof Player player) {
			if (player.isCreative() || player.isSpectator()) return false;
			return !StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> data.getStatus().isKnockedDown()).orElse(false);
		}
		return true;
	}

	private void lock(LivingEntity victim) {
		if (victim instanceof ServerPlayer player) {
			boolean locked = StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> data.getStatus().isStrikeLocked()).orElse(true);
			if (!locked) {
				StrikeAttackHandler.setStrikeLocked(player, true);
				this.pose(player);
			}
		} else if (!victim.hasEffect(MainEffects.STUN)) {
			victim.addEffect(new MobEffectInstance(MainEffects.STUN, Math.max(1, this.ticksLeft), 0, false, false, true));
		}
	}

	private void pose(LivingEntity victim) {
		if (victim instanceof ServerPlayer player) {
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(),
					TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, BOUND_ANIMATION), player);
		} else if (victim instanceof DBSagasEntity saga) {
			saga.triggerAnim(DBSagasEntity.HURT_CONTROLLER, DBSagasEntity.HURT_ANIM_GUM_BOUND);
		}
	}

	private void prompt(ServerPlayer player) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(0, PROMPT_INTERVAL + 10, 6));
		player.connection.send(new ClientboundSetSubtitleTextPacket(
				Component.translatable(SUBTITLE, Component.keybind(ESCAPE_KEY)).withStyle(ChatFormatting.YELLOW)));
		player.connection.send(new ClientboundSetTitleTextPacket(
				Component.translatable(TITLE).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
	}

	private Component meter() {
		int freed = Math.min(METER_BARS, Math.round(METER_BARS * (1.0F - this.ticksLeft / (float) Math.max(1, this.totalTicks))));
		return Component.literal("|".repeat(freed)).withStyle(ChatFormatting.GREEN)
				.append(Component.literal("|".repeat(METER_BARS - freed)).withStyle(ChatFormatting.DARK_GRAY));
	}

	private void pulse() {
		this.entityData.set(STRUGGLES, this.entityData.get(STRUGGLES) + 1);
		this.level().playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS,
				1.0F, 0.7F + this.random.nextFloat() * 0.5F);
	}

	private Vector3f dustColor() {
		int color = this.color();
		return new Vector3f(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
	}

	public void release(boolean escaped) {
		if (this.released) return;
		this.released = true;
		if (this.victimUuid != null) BOUND.remove(this.victimUuid, this);

		if (this.level() instanceof ServerLevel level) {
			LivingEntity victim = this.victim();
			if (victim instanceof ServerPlayer player && player.getUUID().equals(this.victimUuid)) {
				player.connection.send(new ClientboundClearTitlesPacket(true));
				if (!StrikeAttackHandler.isStrikeLockOwned(player)) {
					StrikeAttackHandler.setStrikeLocked(player, false);
					NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(),
							TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), player);
				}
				if (escaped) KnockbackHelper.apply(player, new Vec3(0.0D, 0.45D, 0.0D));
			} else if (victim instanceof DBSagasEntity saga && victim.getUUID().equals(this.victimUuid)) {
				saga.stopTriggeredAnim(DBSagasEntity.HURT_CONTROLLER, DBSagasEntity.HURT_ANIM_GUM_BOUND);
			}
			Vec3 center = victim != null ? victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D) : this.position().add(0.0D, 1.0D, 0.0D);
			level.sendParticles(new DustParticleOptions(this.dustColor(), 1.6F), center.x, center.y, center.z, 30, 0.6D, 0.7D, 0.6D, 0.1D);
			level.playSound(null, center.x, center.y, center.z, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 2.0F, escaped ? 1.2F : 0.8F);
			level.playSound(null, center.x, center.y, center.z, SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 1.6F, 0.6F);
		}
		this.entityData.set(RELEASE_AT, this.tickCount);
	}

	private void clientTick() {
		if (this.isReleasing()) CLIENT_BOUND.remove(this.victimId());
		else CLIENT_BOUND.put(this.victimId(), this.level().getGameTime());

		int struggles = this.entityData.get(STRUGGLES);
		if (struggles == this.seenStruggles) return;
		this.seenStruggles = struggles;
		if (!this.isReleasing()) this.triggerAnim("struggle", "struggle");
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!this.level().isClientSide) this.release(false);
		super.remove(reason);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "gum", 0, state -> state.setAndContinue(this.isReleasing() ? BURST : WRAP)));
		controllers.add(new AnimationController<>(this, "struggle", 0, state -> PlayState.STOP).triggerableAnim("struggle", STRUGGLE));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}

	public static class Ball extends ThrowableProjectile {

		private static final EntityDataAccessor<Integer> BALL_COLOR = SynchedEntityData.defineId(Ball.class, EntityDataSerializers.INT);

		public static final float SPEED = 1.5F;
		public static final int MAX_LIFE = 20;

		public Ball(EntityType<? extends Ball> type, Level level) {
			super(type, level);
			this.setNoGravity(true);
		}

		public static Ball launch(ServerLevel level, LivingEntity caster, int color, Vec3 from, Vec3 direction) {
			Ball ball = MainEntities.GUM_BALL.get().create(level);
			if (ball == null || direction.lengthSqr() < 1.0E-6D) return null;
			ball.setOwner(caster);
			ball.moveTo(from.x, from.y - ball.getBbHeight() * 0.5D, from.z, caster.getYRot(), 0.0F);
			ball.entityData.set(BALL_COLOR, color);
			Vec3 dir = direction.normalize();
			ball.shoot(dir.x, dir.y, dir.z, SPEED, 0.0F);
			return level.addFreshEntity(ball) ? ball : null;
		}

		@Override
		protected void defineSynchedData(SynchedEntityData.Builder builder) {
			builder.define(BALL_COLOR, 0xF4A0B6);
		}

		public int color() {
			return this.entityData.get(BALL_COLOR);
		}

		@Override
		public void tick() {
			super.tick();
			if (!this.level().isClientSide && this.isAlive() && this.tickCount > MAX_LIFE) this.splat();
		}

		@Override
		protected boolean canHitEntity(Entity entity) {
			if (!super.canHitEntity(entity) || !(entity instanceof LivingEntity living)) return false;
			Entity owner = this.getOwner();
			if (entity == owner) return false;
			if (this.level().isClientSide) return true;
			if (!(owner instanceof LivingEntity caster) || !HeldVictim.canHold(caster, living)) return false;
			if (caster instanceof Player player) return TargetHelper.getRelation(player, living) != TargetHelper.Relation.FRIENDLY;
			return !caster.isAlliedTo(living) && !(living instanceof DBSagasEntity);
		}

		@Override
		protected void onHitEntity(EntityHitResult result) {
			super.onHitEntity(result);
			if (!(this.level() instanceof ServerLevel level) || !(result.getEntity() instanceof LivingEntity victim)) return;
			if (this.getOwner() instanceof LivingEntity caster) bind(level, caster, victim, this.color(), bindTicks(caster, victim));
			this.discard();
		}

		@Override
		protected void onHitBlock(BlockHitResult result) {
			super.onHitBlock(result);
			if (!this.level().isClientSide) this.splat();
		}

		private void splat() {
			if (this.level() instanceof ServerLevel level) {
				int color = this.color();
				Vector3f dust = new Vector3f(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
				level.sendParticles(new DustParticleOptions(dust, 1.4F), this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
						16, 0.25D, 0.25D, 0.25D, 0.05D);
				level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.2F, 0.7F);
			}
			this.discard();
		}

		@Override
		public boolean shouldBeSaved() {
			return false;
		}
	}
}
