package com.dragonminez.common.init.entities;

import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import com.dragonminez.common.init.entities.vehicle.VehicleProperties;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;

public class FriezaCapsuleEntity extends VehicleEntity {

	private static final VehicleProperties PROPERTIES = VehicleProperties.builder(VehicleProperties.Mobility.HYBRID)
			.speed(0.5D)
			.sprint(1.6D)
			.verticalSpeed(0.4D)
			.acceleration(0.25D)
			.model("vehicles/frieza_capsule", 2.5F)
			.seatBones(1)
			.fallbackSeat(0.0D, 7.0D, 0.0D)
			.tilt(10.0F, 6.0F, 7.0F, 1.0D)
			.hoverBob(0.06F)
			.engine(MainSounds.VEHICLE_FRIEZA_CAPSULE_ENGINE, 0.15F, 0.55F, 1.1F, 1.6F)
			.dismountBeside()
			.build();

	private static final int RING_PARTICLES = 24;

	public FriezaCapsuleEntity(EntityType<? extends VehicleEntity> type, Level level) {
		super(type, level, PROPERTIES);
	}

	@Override
	protected Item getVehicleItem() {
		return MainItems.FRIEZA_CAPSULE_ITEM.get();
	}

	@Override
	protected void onFlightModeChanged(boolean flying) {
		if (!(this.level() instanceof ServerLevel server)) return;
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
				flying ? MainSounds.VEHICLE_FRIEZA_CAPSULE_TAKEOFF.get() : MainSounds.VEHICLE_FRIEZA_CAPSULE_LAND.get(),
				SoundSource.NEUTRAL, 0.6F, flying ? 1.4F : 1.2F);
		double radius = this.getBbWidth() * 0.6D;
		for (int i = 0; i < RING_PARTICLES; i++) {
			double angle = Mth.TWO_PI * i / RING_PARTICLES;
			double dx = Math.cos(angle);
			double dz = Math.sin(angle);
			server.sendParticles(flying ? ParticleTypes.CLOUD : ParticleTypes.POOF,
					this.getX() + dx * radius, this.getY() + 0.1D, this.getZ() + dz * radius,
					0, dx, 0.02D, dz, flying ? 0.25D : 0.12D);
		}
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
	}
}
