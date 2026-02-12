package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.client.init.entities.model.BlackNimbusModel;
import com.dragonminez.client.init.entities.model.FlyingNimbusModel;
import com.dragonminez.common.init.entities.BlackNimbusEntity;
import com.dragonminez.common.init.entities.FlyingNimbusEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BlackNimbusRenderer extends GeoEntityRenderer<BlackNimbusEntity> {

    public BlackNimbusRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BlackNimbusModel<>());
    }

//    @Override
//    public void render(FlyingNimbusEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
//        poseStack.pushPose();
//        poseStack.scale(1.1f,1.1f,1.1f);
//        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
//        poseStack.popPose();
//    }

    @Override
    public RenderType getRenderType(BlackNimbusEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
