package com.dragonminez.common.init.entities;

import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import com.dragonminez.common.init.entities.vehicle.VehicleProperties;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

public class SpacePodEntity extends VehicleEntity {

    private static final VehicleProperties PROPERTIES = VehicleProperties.builder(VehicleProperties.Mobility.FLYING)
            .speed(1.08D)
            .verticalSpeed(0.35D)
            .seat(0.0D, 0.4D, 0.0D)
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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_OPEN, false);
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
