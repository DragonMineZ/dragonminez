package com.dragonminez.client.init.entities.renderer.sagas.layer;

import com.dragonminez.client.render.shader.TransformationMaskBufferSource;
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

	private static final float[] BORDER_COLOR = ColorUtils.hexToRgb("#9A1CFF");
	private static final float[] TINT = ColorUtils.hexToRgb("#9B30D9");
	private static final float TINT_ALPHA = 0.42f;

	public SagaSupervillainLayer(GeoRenderer<T> geoRenderer) {
		super(geoRenderer);
	}

	public static float[] borderColor(DBSagasEntity entity) {
		if (!entity.isSupervillain() || entity.isSpectator() || entity.isInvisible()) return null;
		return BORDER_COLOR;
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
					   MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
					   int packedLight, int packedOverlay) {
		if (!animatable.isSupervillain() || animatable.isSpectator()) return;

		ResourceLocation texture = getRenderer().getTextureLocation(animatable);
		RenderType tinted = ModRenderTypes.skinOverlayTranslucent(texture);
		TransformationMaskBufferSource maskBuffer = bufferSource instanceof TransformationMaskBufferSource mask ? mask : null;

		if (maskBuffer != null) maskBuffer.setMaskCaptureBlocked(true);
		try {
			getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, tinted,
					bufferSource.getBuffer(tinted), partialTick, packedLight, packedOverlay,
					TINT[0], TINT[1], TINT[2], TINT_ALPHA);
		} finally {
			if (maskBuffer != null) maskBuffer.setMaskCaptureBlocked(false);
		}
	}
}
