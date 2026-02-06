package com.dragonminez.client.init.entities.renderer.sagas.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.SagaFreezer2ndEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DBSagasAuraLayer<T extends DBSagasEntity> extends GeoRenderLayer<T> {

    private static final ResourceLocation AURA_MODEL = new ResourceLocation(Reference.MOD_ID, "geo/entity/races/kiaura.geo.json");
    private static final ResourceLocation AURA_TEX_0 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/aura_ki_0.png");
    private static final ResourceLocation AURA_TEX_1 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/aura_ki_1.png");
    private static final ResourceLocation AURA_TEX_2 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/aura_ki_2.png");

    public DBSagasAuraLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel entityModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (!animatable.isTransforming()) return;

        BakedGeoModel auraModel = getGeoModel().getBakedModel(AURA_MODEL);
        if (auraModel == null) return;

        for (GeoBone rootBone : auraModel.topLevelBones()) {
            setHiddenRecursive(rootBone, false);
        }

        // Animación de textura (loop de 3 frames)
        long frame = (long) ((animatable.level().getGameTime() / 1.5f) % 3);
        ResourceLocation currentTexture;
        if (frame == 0) currentTexture = AURA_TEX_0;
        else if (frame == 1) currentTexture = AURA_TEX_1;
        else currentTexture = AURA_TEX_2;

        syncModelToEntity(auraModel, entityModel);

        float[] color = new float[]{1.0f, 1.0f, 1.0f};

        if (animatable instanceof SagaFreezer2ndEntity) {
            color = ColorUtils.rgbIntToFloat(0x880FFF);
        }

        RenderType auraRenderType = ModRenderTypes.energy(currentTexture);

        poseStack.pushPose();

        float scale = 1.3f;
        poseStack.scale(scale, scale, scale);

        getRenderer().reRender(auraModel, poseStack, bufferSource, animatable, auraRenderType,
                bufferSource.getBuffer(auraRenderType), partialTick, 15728880,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                color[0], color[1], color[2], 0.2f);

        poseStack.popPose();
    }

    private void setHiddenRecursive(GeoBone bone, boolean hidden) {
        bone.setHidden(hidden);
        for (GeoBone child : bone.getChildBones()) {
            setHiddenRecursive(child, hidden);
        }
    }

    private void syncModelToEntity(BakedGeoModel auraModel, BakedGeoModel entityModel) {
        for (GeoBone auraBone : auraModel.topLevelBones()) {
            syncBoneRecursively(auraBone, entityModel);
        }
    }

    private void syncBoneRecursively(GeoBone destBone, BakedGeoModel sourceModel) {
        sourceModel.getBone(destBone.getName()).ifPresent(sourceBone -> {
            destBone.setRotX(sourceBone.getRotX());
            destBone.setRotY(sourceBone.getRotY());
            destBone.setRotZ(sourceBone.getRotZ());
            destBone.setPosX(sourceBone.getPosX());
            destBone.setPosY(sourceBone.getPosY());
            destBone.setPosZ(sourceBone.getPosZ());
            destBone.setScaleX(sourceBone.getScaleX());
            destBone.setScaleY(sourceBone.getScaleY());
            destBone.setScaleZ(sourceBone.getScaleZ());
        });

        for (GeoBone child : destBone.getChildBones()) {
            syncBoneRecursively(child, sourceModel);
        }
    }
}