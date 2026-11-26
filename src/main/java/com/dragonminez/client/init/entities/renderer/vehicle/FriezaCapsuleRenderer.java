package com.dragonminez.client.init.entities.renderer.vehicle;

import com.dragonminez.client.init.entities.model.VehicleModel;
import com.dragonminez.common.init.entities.FriezaCapsuleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class FriezaCapsuleRenderer extends VehicleRenderer<FriezaCapsuleEntity> {

	public FriezaCapsuleRenderer(EntityRendererProvider.Context context) {
		super(context, new VehicleModel<>("vehicles/frieza_capsule"), 0.7F);
	}
}
