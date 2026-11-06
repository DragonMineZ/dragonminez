package com.dragonminez.common.init.entities;

import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import com.dragonminez.common.init.entities.vehicle.VehicleProperties;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.PlayState;

public class SpacePodEntity extends VehicleEntity {

    private static final VehicleProperties.Smoke WISP = new VehicleProperties.Smoke(0.14F, 0.85F, 0.28F, 14, 0.3F, 0.25D, 1.08D);

    private static final VehicleProperties PROPERTIES = VehicleProperties.builder(VehicleProperties.Mobility.FLYING)
            .speed(1.08D)
            .verticalSpeed(0.35D)
            .model("spacepod", 1.1F)
            .seatBones(1)
            .fallbackSeat(0.0D, 10.9D, -1.0D)
            .exhaust(WISP, 0.0D, 10.0D, 18.0D)
            .tilt(8.0F, 5.0F, 0.0F, 1.0D)
            .engine(() -> MainSounds.VEHICLE_SPACEPOD_ENGINE.get(), 0.12F, 0.5F, 0.8F, 1.25F)
            .build();

    private static final EntityDataAccessor<Boolean> IS_OPEN = SynchedEntityData.defineId(SpacePodEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation ANIM_ABIERTO = RawAnimation.begin().then("open", Animation.LoopType.HOLD_ON_LAST_FRAME);
    private static final RawAnimation ANIM_CERRADO = RawAnimation.begin().then("close", Animation.LoopType.HOLD_ON_LAST_FRAME);

    public SpacePodEntity(EntityType<? extends VehicleEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel, PROPERTIES);
    }

    @Override
    protected Item getVehicleItem() {
        return MainItems.NAVE_SAIYAN_ITEM.get();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_OPEN, false);
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
                    else if (KeyBinds.SECOND_FUNCTION_KEY.isDown()) {
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
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            if (!isOpen()) {
                if (!player.isPassenger()) {
                    setOpenNave(true);
                    player.level().playSound(null, this.getOnPos(), MainSounds.NAVE_OPEN.get(), SoundSource.NEUTRAL, 0.5F, 1.0F);
                }
            } else {
                setOpenNave(false);
                tryMount(player);
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
}
