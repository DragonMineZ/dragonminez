package com.dragonminez.common.init.item.render;

import com.dragonminez.common.init.item.PatternStationItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PatternStationItemRenderer extends GeoItemRenderer<PatternStationItem> {

	public PatternStationItemRenderer() {
		super(new PatternStationItemModel());
	}

	@Override
	public void preRender(PoseStack poseStack, PatternStationItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
						  boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

		if (!isReRender)
			poseStack.translate(0, -0.51f, 0);
	}
}
