package com.dragonminez.client.init.entities.renderer.vehicle;

import com.dragonminez.client.init.entities.model.VehicleModel;
import com.dragonminez.common.init.entities.CC576Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

public class CC576Renderer extends VehicleRenderer<CC576Entity> {

	public CC576Renderer(EntityRendererProvider.Context context) {
		super(context, new VehicleModel<>("vehicles/cc576"), 2.2F);
	}

	@Override
	protected void poseBones(CC576Entity entity, BakedGeoModel model, float partialTick) {
		GeoBone stairs = pose(model, "stairs");
		if (stairs != null && !entity.onGround()) stairs.setHidden(true);
	}
}
