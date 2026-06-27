package com.dragonminez.client.init.blocks.renderer;

import com.dragonminez.client.init.blocks.model.GravityDeviceBlockModel;
import com.dragonminez.common.init.block.entity.GravityDeviceBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class GravityDeviceBlockRenderer extends GeoBlockRenderer<GravityDeviceBlockEntity> {
	public GravityDeviceBlockRenderer(BlockEntityRendererProvider.Context context) {
		super(new GravityDeviceBlockModel());
	}
}
