package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class KiExplosionVisualRenderer extends EntityRenderer<KiExplosionVisualEntity> {

    private static final ResourceLocation TEXTURE_EXPLOSION = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");

    private final KiBallModel model;

    public KiExplosionVisualRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new KiBallModel(context.bakeLayer(KiBallModel.LAYER_LOCATION));
    }

    @Override
    public void render(KiExplosionVisualEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        float ageInTicks = entity.tickCount + partialTick;
        float lifeTime = 25.0F;

        float progress = Math.min(ageInTicks / lifeTime, 1.0F);

        float currentScale = entity.getMaxSize() * (0.5F + (progress * 0.5F));

        float fadeStart = 0.75F;
        float maxAlpha = 0.4F;
        float currentAlpha = maxAlpha;

        if (progress > fadeStart) {
            currentAlpha = maxAlpha * (1.0F - ((progress - fadeStart) / (1.0F - fadeStart)));
        }


        float[] color = entity.getRgbColorMain();

        poseStack.pushPose();
        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);


        poseStack.scale(currentScale, currentScale, currentScale);
        poseStack.translate(0.0D, -1.0D, 0.0D);

        VertexConsumer vertexConsumer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_EXPLOSION));
        this.model.renderToBuffer(poseStack, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY, color[0], color[1], color[2], currentAlpha);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(KiExplosionVisualEntity entity) {
        return TEXTURE_EXPLOSION;
    }
}