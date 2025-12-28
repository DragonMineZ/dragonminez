package com.dragonminez.common.init.entities;

import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

public class SpacePodEntity extends Mob implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_OPEN = SynchedEntityData.defineId(SpacePodEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation ANIM_ABIERTO = RawAnimation.begin().then("open", Animation.LoopType.HOLD_ON_LAST_FRAME);
    private static final RawAnimation ANIM_CERRADO = RawAnimation.begin().then("close", Animation.LoopType.HOLD_ON_LAST_FRAME);

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    public SpacePodEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.setNoGravity(true);
    }

    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.ATTACK_DAMAGE, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5F)
                .add(Attributes.FLYING_SPEED, 2.4F).build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_OPEN, false);
    }

    @Override
    public void travel(Vec3 pTravelVector) {
        if (this.isAlive()) {
            if (this.getControllingPassenger() instanceof Player passenger) {
                this.setYRot(passenger.getYRot());
                this.yRotO = this.getYRot();
                this.setXRot(passenger.getXRot() * 0.5F);
                this.setRot(this.getYRot(), this.getXRot());
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();

                double speed = this.getAttributeValue(Attributes.FLYING_SPEED) * 0.45D;

                double verticalSpeed = 0;

                if (this.level().isClientSide) {
                    if (net.minecraft.client.Minecraft.getInstance().options.keyJump.isDown()) {
                        verticalSpeed = 0.35;
                    }
                    else if (KeyBinds.DESCEND_KEY.isDown()) {
                        verticalSpeed = -0.35;
                    }
                }

                float forwardInput = passenger.zza;
                float strafeInput = passenger.xxa;

                Vec3 inputVector = new Vec3(strafeInput, 0, forwardInput);
                Vec3 moveVector = inputVector.yRot((float) -Math.toRadians(this.getYRot()));

                if (moveVector.lengthSqr() > 1.0E-7D) {
                    moveVector = moveVector.normalize().scale(speed);
                }
                this.setDeltaMovement(moveVector.x, verticalSpeed, moveVector.z);
                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

                return;
            }
        }

        Vec3 currentMotion = this.getDeltaMovement();

        this.setDeltaMovement(currentMotion.x * 0.9, -0.07, currentMotion.z * 0.9);

        super.travel(pTravelVector);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof LivingEntity entity ? entity : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.4D;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            if (!isOpen()) {
                if (!player.isPassenger()) {
                    setOpenNave(true);
                    player.level().playSound(null, this.getOnPos(), MainSounds.NAVE_OPEN.get(), SoundSource.NEUTRAL, 0.5F, 1.0F);
                }
            } else {
                setOpenNave(false);
                if (!player.isPassenger()) {
                    player.startRiding(this);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    public boolean isOpen() {
        return this.entityData.get(IS_OPEN);
    }

    public void setOpenNave(boolean open) {
        this.entityData.set(IS_OPEN, open);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if ("player".equals(pSource.getMsgId()) && pSource.getEntity() instanceof Player) {
            if (!this.level().isClientSide && isAlive()) {
                this.spawnAtLocation(MainItems.NAVE_SAIYAN_ITEM.get());
                this.remove(RemovalReason.KILLED);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource pSource) {
        return !"player".equals(pSource.getMsgId()) || super.isInvulnerableTo(pSource);
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        AnimationController<?> controller = tAnimationState.getController();

        if (isOpen()) {
            controller.setAnimation(ANIM_ABIERTO);
        } else {
            controller.setAnimation(ANIM_CERRADO);
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
