package com.dragonminez.client.init.entities.renderer.vehicle;

import com.dragonminez.client.init.entities.model.VehicleModel;
import com.dragonminez.common.init.entities.CarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

public class CarRenderer extends VehicleRenderer<CarEntity> {

	private static final ResourceLocation[] TEXTURES = new ResourceLocation[CarEntity.Color.values().length];
	private static final String[] FRONT_WHEELS = {"wheel_fl", "wheel_fr"};
	private static final String[] REAR_WHEELS = {"wheel_bl", "wheel_br"};
	private static final float STEERING_WHEEL_RATIO = 2.5F;

	static {
		for (CarEntity.Color color : CarEntity.Color.values()) {
			TEXTURES[color.ordinal()] = VehicleModel.texture("vehicles/" + color.texture());
		}
	}

	public CarRenderer(EntityRendererProvider.Context context) {
		super(context, new VehicleModel<>("vehicles/car", car -> TEXTURES[car.getColor().ordinal()]), 1.1F);
	}

	@Override
	protected void poseBones(CarEntity car, BakedGeoModel model, float partialTick) {
		float spin = -car.getWheelSpin(partialTick) * Mth.DEG_TO_RAD;
		float steer = -car.getSteer(partialTick) * Mth.DEG_TO_RAD;
		for (String name : REAR_WHEELS) {
			GeoBone wheel = pose(model, name);
			if (wheel != null) wheel.setRotX(spin);
		}
		for (String name : FRONT_WHEELS) {
			GeoBone wheel = pose(model, name);
			if (wheel == null) continue;
			wheel.setRotX(spin);
			wheel.setRotY(steer);
		}
		GeoBone steeringWheel = pose(model, "steering_wheel");
		if (steeringWheel != null) steeringWheel.setRotZ(steer * STEERING_WHEEL_RATIO);
	}
}
