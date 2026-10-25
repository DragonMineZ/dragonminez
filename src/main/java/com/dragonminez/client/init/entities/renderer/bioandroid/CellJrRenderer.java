package com.dragonminez.client.init.entities.renderer.bioandroid;

import com.dragonminez.client.init.entities.model.bioandroid.CellJrModel;
import com.dragonminez.common.init.entities.bioandroid.CellJrEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CellJrRenderer extends GeoEntityRenderer<CellJrEntity> {

	public CellJrRenderer(EntityRendererProvider.Context context) {
		super(context, new CellJrModel());
		this.shadowRadius = 0.3f;
	}
}
