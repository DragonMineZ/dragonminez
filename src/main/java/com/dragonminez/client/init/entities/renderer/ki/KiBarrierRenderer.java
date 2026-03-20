package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.KiBarrierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class KiBarrierRenderer extends EntityRenderer<KiBarrierEntity> {

    private static final ResourceLocation TEXTURE_EXPLOSION = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");

    private final KiBallModel ballmodel;

    public KiBarrierRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
        this.ballmodel = new KiBallModel(pContext.bakeLayer(KiBallModel.LAYER_LOCATION));

    }

    @Override
    public void render(KiBarrierEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        if (!entity.isFiring()) {
            return;
        }

        poseStack.pushPose();

        float halfHeight = entity.getBbHeight() / 2.0F;
        poseStack.translate(0.0D, halfHeight, 0.0D);

        float scale = entity.getCurrentSize();

        float ageInTicks = entity.tickCount + partialTick;
        this.ballmodel.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        float[] auraColor2 = ColorUtils.rgbIntToFloat(entity.getColorBorde());

        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0D, -1.3D, 0.0D);
        VertexConsumer auraBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_EXPLOSION));
        this.ballmodel.renderToBuffer(poseStack, auraBuffer, 15728880, OverlayTexture.NO_OVERLAY,
                auraColor[0], auraColor[1], auraColor[2], 0.25F);

        poseStack.scale(1.1f, 1.1f, 1.1f);
        poseStack.translate(0.0D, -0.1D, 0.0D);
        auraBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_EXPLOSION));
        this.ballmodel.renderToBuffer(poseStack, auraBuffer, 15728880, OverlayTexture.NO_OVERLAY,
                auraColor2[0], auraColor2[1], auraColor2[2], 0.15F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KiBarrierEntity pEntity) {
        return TEXTURE_EXPLOSION;
    }
}