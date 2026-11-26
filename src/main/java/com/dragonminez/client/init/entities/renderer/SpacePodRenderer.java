package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.SpacePodModel;
import com.dragonminez.client.init.entities.renderer.vehicle.VehicleRenderer;
import com.dragonminez.common.init.entities.SpacePodEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class SpacePodRenderer extends VehicleRenderer<SpacePodEntity> {

    public SpacePodRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SpacePodModel<>(), 0.9F);
    }
}
