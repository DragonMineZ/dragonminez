package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.redribbon.RedRibbonSoldierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class RedRibbonOutfitLayer<T extends RedRibbonSoldierEntity> extends GeoRenderLayer<T> {
    private static final ResourceLocation OUTFIT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/redribbon_outfit.png");

    public RedRibbonOutfitLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        RenderType outfitType = RenderType.entityCutoutNoCull(OUTFIT_TEXTURE);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, outfitType, bufferSource.getBuffer(outfitType), partialTick, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f);
    }
}
