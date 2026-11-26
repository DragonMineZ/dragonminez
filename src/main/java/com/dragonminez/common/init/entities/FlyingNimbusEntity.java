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
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;

public class FlyingNimbusEntity extends VehicleEntity {

    private static final int AURA_COLOR = 0xFFF852;
    private static final VehicleProperties PROPERTIES = VehicleProperties.builder(VehicleProperties.Mobility.FLYING)
            .speed(1.2D)
            .verticalSpeed(0.4D)
            .seat(0.0D, 0.9D, 0.4D)
            .seat(0.0D, 0.9D, -0.4D)
            .boost(45, 7.2D, 0.65D)
            .build();
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    public FlyingNimbusEntity(EntityType<? extends VehicleEntity> entityType, Level level) {
        super(entityType, level, PROPERTIES);
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
    protected void tickVisuals(double speed) {
        for (int i = 0; i < 15; i++) {
            double offsetX = (this.random.nextDouble() - 0.1D) * this.getBbWidth() * 0.01D;
            double offsetY = (this.random.nextDouble() - 0.02D) * this.getBbHeight() * 0.01D;
            double offsetZ = (this.random.nextDouble() - 0.1D) * this.getBbWidth() * 0.01D;
            this.level().addParticle(MainParticles.KINTON.get(), this.getX() + offsetX - 0.5D,
                    this.getY() + offsetY + 1.0D, this.getZ() + offsetZ, AURA_COLOR, 0, 0);
        }
    }

    @Override
    protected void onBoostStart() {
        Vec3 back = new Vec3(0, 0, 1).yRot((float) -Math.toRadians(this.getYRot())).scale(-1.0D);
        double centerX = this.getX() + back.x * 0.8D;
        double centerY = this.getY() + 1.0D;
        double centerZ = this.getZ() + back.z * 0.8D;
        for (int i = 0; i < 60; i++) {
            double radius = this.random.nextDouble() * 1.6D;
            double theta = this.random.nextDouble() * Math.PI * 2.0D;
            double phi = (this.random.nextDouble() - 0.5D) * Math.PI;
            this.level().addParticle(MainParticles.KINTON.get(),
                    centerX + Math.cos(theta) * Math.cos(phi) * radius,
                    centerY + Math.sin(phi) * radius,
                    centerZ + Math.sin(theta) * Math.cos(phi) * radius,
                    AURA_COLOR, 0, 0);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        animationState.getController().setAnimation(IDLE);
        return PlayState.CONTINUE;
    }
}
