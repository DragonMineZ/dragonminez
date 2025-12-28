package com.dragonminez.common.init.entities;

import com.dragonminez.client.gui.WishesScreen;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.server.events.DragonBallsHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

public class ShenronEntity extends Mob implements GeoEntity {
	private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
	private long invokingTime;
	private int despawnDelay = 20 * 5;

	private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(ShenronEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> GRANTED_WISH = SynchedEntityData.defineId(ShenronEntity.class, EntityDataSerializers.BOOLEAN);

	public ShenronEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
		this.entityData.define(OWNER_NAME, "");
		this.entityData.define(GRANTED_WISH, false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 1000.0D)
				.add(Attributes.MOVEMENT_SPEED, 2.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
	}

	public void setOwnerName(String name) {
		this.entityData.set(OWNER_NAME, name);
	}

	public String getOwnerName() {
		return this.entityData.get(OWNER_NAME);
	}

	public void setGrantedWish(boolean granted) {
		this.entityData.set(GRANTED_WISH, granted);
	}

	public boolean hasGrantedWish() {
		return this.entityData.get(GRANTED_WISH);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 35.0f));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (this.level().isClientSide && this.getOwnerName().equals(player.getName().getString())) {
			if (!this.hasGrantedWish() && Minecraft.getInstance().player.equals(player)) {
				Minecraft.getInstance().setScreen(new WishesScreen("shenron", 1));
			}
		}
		return super.mobInteract(player, hand);
	}

	public void setInvokingTime(long time) {
		this.invokingTime = time;
	}

	public long getInvokingTime() {
		return this.invokingTime;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
		controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
	}

	@Override
	public void tick() {
		super.tick();

		if (hasGrantedWish()) despawnDelay--;
		if (despawnDelay <= 0) this.discard();
	}

	private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
		tAnimationState.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
		return PlayState.CONTINUE;
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean canCollideWith(Entity pEntity) {
		return false;
	}

	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!this.level().isClientSide && reason == RemovalReason.DISCARDED) {
			onDespawn();
		}
		super.remove(reason);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC) || source.is(DamageTypes.GENERIC_KILL)) {
			return super.hurt(source, amount);
		}
		return false;
	}

	private void onDespawn() {
		if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
			serverLevel.setWeatherParameters(6000, 0, false, false);
			serverLevel.setDayTime(this.getInvokingTime());

			if (ConfigManager.getServerConfig().getWorldGen().isGenerateDragonBalls()) {
				DragonBallsHandler.scatterDragonBalls(serverLevel, false);
				ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayerByName(this.getOwnerName());
				if (owner != null) {
					DragonBallsHandler.syncRadar(owner.serverLevel());
				}
			}
		}
	}
}
