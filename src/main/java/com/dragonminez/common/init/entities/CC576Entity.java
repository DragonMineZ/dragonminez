package com.dragonminez.common.init.entities;

import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import com.dragonminez.common.init.entities.vehicle.VehicleProperties;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;

public class CC576Entity extends VehicleEntity {

	private static final VehicleProperties.Smoke JET = new VehicleProperties.Smoke(0.55F, 0.55F, 0.5F, 40, 0.7F, 0.08D, 1.3D);

	private static final VehicleProperties PROPERTIES = VehicleProperties.builder(VehicleProperties.Mobility.FLYING)
			.speed(0.9D)
			.sprint(1.6D)
			.verticalSpeed(0.3D)
			.acceleration(0.06D)
			.model("vehicles/cc576", 1.0F)
			.modelOffset(-10.0D, 0.0D, -40.0D)
			.seatBones(4)
			.fallbackSeat(21.0D, 22.0D, 3.0D)
			.fallbackSeat(-1.0D, 22.0D, 3.0D)
			.fallbackSeat(21.0D, 22.0D, 25.0D)
			.fallbackSeat(-1.0D, 22.0D, 25.0D)
			.exhaust(JET, 31.0D, 46.0D, 109.5D)
			.exhaust(JET, -11.0D, 46.0D, 109.5D)
			.tilt(14.0F, 8.0F, 4.0F, 1.4D)
			.engine(MainSounds.VEHICLE_CC576_ENGINE, 0.3F, 1.0F, 0.7F, 1.2F)
			.boardSound(MainSounds.VEHICLE_DOOR)
			.dismountBeside()
			.cameraDistance(6.0D)
			.build();

	public CC576Entity(EntityType<? extends VehicleEntity> type, Level level) {
		super(type, level, PROPERTIES);
	}

	@Override
	protected Item getVehicleItem() {
		return MainItems.CC576_ITEM.get();
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
	}
}
