package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.client.init.entities.model.FlyingNimbusModel;
import com.dragonminez.common.init.entities.FlyingNimbusEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FlyingNimbusRenderer extends GeoEntityRenderer<FlyingNimbusEntity> {

    public FlyingNimbusRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new FlyingNimbusModel<>());
    }

//    @Override
//    public void render(FlyingNimbusEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
//        poseStack.pushPose();
//        poseStack.scale(1.1f,1.1f,1.1f);
//        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
//        poseStack.popPose();
//    }

    @Override
    public RenderType getRenderType(FlyingNimbusEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
