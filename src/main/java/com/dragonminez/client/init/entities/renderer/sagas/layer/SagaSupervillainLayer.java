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
		if (!entity.showsSupervillainAura() || entity.isSpectator() || entity.isInvisible()) return null;
		DBSagasEntity.SupervillainPalette palette = entity.getSupervillainPalette();
		return palette != null ? ColorUtils.rgbIntToFloat(palette.flameOuter()) : BORDER_COLOR;
	}

	public static float[] tintColor(DBSagasEntity entity) {
		if (!entity.showsSupervillainAura() || entity.isSpectator() || entity.isInvisible()) return null;
		DBSagasEntity.SupervillainPalette palette = entity.getSupervillainPalette();
		float[] tint = palette != null ? ColorUtils.rgbIntToFloat(palette.tint()) : TINT;
		float alpha = palette != null ? palette.tintAlpha() : TINT_ALPHA;
		return new float[]{tint[0], tint[1], tint[2], alpha};
	}

	public static float[] borderInnerColor(DBSagasEntity entity) {
		if (!entity.showsSupervillainAura() || entity.isSpectator() || entity.isInvisible()) return null;
		DBSagasEntity.SupervillainPalette palette = entity.getSupervillainPalette();
		return palette != null ? ColorUtils.rgbIntToFloat(palette.flameInner()) : null;
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
					   MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
					   int packedLight, int packedOverlay) {
		if (!animatable.showsSupervillainAura() || animatable.isSpectator()) return;

		DBSagasEntity.SupervillainPalette palette = animatable.getSupervillainPalette();
		float[] tint = palette != null ? ColorUtils.rgbIntToFloat(palette.tint()) : TINT;
		float tintAlpha = palette != null ? palette.tintAlpha() : TINT_ALPHA;

		ResourceLocation texture = getRenderer().getTextureLocation(animatable);
		RenderType tinted = ModRenderTypes.skinOverlayTranslucent(texture);
		TransformationMaskBufferSource maskBuffer = bufferSource instanceof TransformationMaskBufferSource mask ? mask : null;

		if (maskBuffer != null) maskBuffer.setMaskCaptureBlocked(true);
		try {
			getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, tinted,
					bufferSource.getBuffer(tinted), partialTick, packedLight, packedOverlay,
					tint[0], tint[1], tint[2], tintAlpha);
		} finally {
			if (maskBuffer != null) maskBuffer.setMaskCaptureBlocked(false);
		}
	}
}
