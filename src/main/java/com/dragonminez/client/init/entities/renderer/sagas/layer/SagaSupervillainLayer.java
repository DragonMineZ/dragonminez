package com.dragonminez.client.init.entities.renderer.sagas.layer;

import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SagaSupervillainLayer<T extends DBSagasEntity> extends GeoRenderLayer<T> {

	private static final float[] TINT = ColorUtils.hexToRgb("#9B30D9");

	public SagaSupervillainLayer(GeoRenderer<T> geoRenderer) {
		super(geoRenderer);
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
					   MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
					   int packedLight, int packedOverlay) {
		if (!animatable.isSupervillain() || animatable.isSpectator()) return;

		ResourceLocation texture = getRenderer().getTextureLocation(animatable);
		RenderType tinted = ModRenderTypes.skinOverlayCutout(texture);

		getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, tinted,
				bufferSource.getBuffer(tinted), partialTick, packedLight, packedOverlay,
				TINT[0], TINT[1], TINT[2], 1.0f);
	}
}
