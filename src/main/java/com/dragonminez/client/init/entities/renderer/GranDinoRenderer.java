package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.DinoGlobalModel;
import com.dragonminez.common.init.entities.animal.DinoGlobalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GranDinoRenderer<T extends DinoGlobalEntity> extends GeoEntityRenderer<T> {

    public GranDinoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DinoGlobalModel<>());
        this.shadowRadius = 1.3f;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(1.5f,1.5f,1.5f);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
