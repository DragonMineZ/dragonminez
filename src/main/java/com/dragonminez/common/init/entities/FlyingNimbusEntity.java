package com.dragonminez.common.init.entities;

import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import com.dragonminez.common.init.entities.vehicle.VehicleProperties;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

public class FlyingNimbusEntity extends VehicleEntity {

    private static final VehicleProperties PROPERTIES = VehicleProperties.builder(VehicleProperties.Mobility.FLYING)
            .speed(1.2D)
            .verticalSpeed(0.4D)
            .seat(0.0D, 0.9D, 0.4D)
            .seat(0.0D, 0.9D, -0.4D)
            .boost(45, 7.2D, 0.65D)
            .build();

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    public FlyingNimbusEntity(EntityType<? extends VehicleEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel, PROPERTIES);
    }

    @Override
    protected Item getVehicleItem() {
        return MainItems.NUBE_ITEM.get();
    }

    @Override
    protected boolean canBeMountedBy(Player player) {
        int alignment = StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(data -> data.getResources().getAlignment())
                .orElse(0);
        if (alignment > 66) return true;
        player.displayClientMessage(Component.translatable("message.dragonminez.nimbus.not_pure"), true);
        return false;
    }

    @Override
    protected void onBoostStart() {
        spawnBoostBurst(MainParticles.KINTON.get(), 0xFFF852);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        tAnimationState.getController().setAnimation(IDLE);
        return PlayState.CONTINUE;
    }
}
