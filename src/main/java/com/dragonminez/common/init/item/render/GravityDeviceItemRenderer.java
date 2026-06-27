package com.dragonminez.common.init.item.render;

import com.dragonminez.common.init.item.GravityDeviceItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GravityDeviceItemRenderer extends GeoItemRenderer<GravityDeviceItem> {

	public GravityDeviceItemRenderer() {
		super(new GravityDeviceItemModel());
	}

	@Override
	public RenderType getRenderType(GravityDeviceItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutoutNoCull(texture);
	}
}
