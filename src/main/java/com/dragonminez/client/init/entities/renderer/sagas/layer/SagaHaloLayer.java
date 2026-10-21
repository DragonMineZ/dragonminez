package com.dragonminez.client.init.entities.renderer.sagas.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.hair.HairColliders;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SagaHaloLayer<T extends DBSagasEntity> extends GeoRenderLayer<T> {

	private static final ResourceLocation RACES_PARTS_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/raceparts.geo.json");
	private static final ResourceLocation RACES_PARTS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/raceparts.png");
	private static final float[] HALO_COLOR = ColorUtils.hexToRgb("#FFF461");
	private static final float HALO_ALPHA = 0.75f;
	private static final float PLAYER_HEAD_TOP = 2.0f;

	public SagaHaloLayer(GeoRenderer<T> geoRenderer) {
		super(geoRenderer);
	}

	@Override
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
							  MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
							  int packedLight, int packedOverlay) {
		if (!"head".equals(bone.getName())) return;
		if (animatable.isCharacterAlive() || animatable.isSpectator() || animatable.isInvisible()) return;

		BakedGeoModel partsModel = GeckoLibCache.getBakedModels().get(RACES_PARTS_MODEL);
		if (partsModel == null) return;
		GeoBone haloBone = partsModel.getBone("halo").orElse(null);
		if (haloBone == null) return;

		float[] headBounds = HairColliders.boundsOf(bone);

		poseStack.pushPose();
		if (headBounds != null) {
			poseStack.translate((headBounds[0] + headBounds[3]) * 0.5f, headBounds[4] - PLAYER_HEAD_TOP, (headBounds[2] + headBounds[5]) * 0.5f);
		}

		RenderType haloType = ModRenderTypes.energy(RACES_PARTS_TEXTURE);
		getRenderer().renderRecursively(poseStack, animatable, haloBone, haloType, bufferSource,
				bufferSource.getBuffer(haloType), true, partialTick, packedLight, OverlayTexture.NO_OVERLAY,
				HALO_COLOR[0], HALO_COLOR[1], HALO_COLOR[2], HALO_ALPHA);
		poseStack.popPose();

		bufferSource.getBuffer(renderType);
	}
}
