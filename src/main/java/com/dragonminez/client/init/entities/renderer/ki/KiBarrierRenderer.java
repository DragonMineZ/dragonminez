package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallPlaneModel;
import com.dragonminez.client.init.entities.model.ki.KiDiscModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.KiBarrierEntity;
import com.dragonminez.common.init.entities.ki.KiDiscEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class KiBarrierRenderer extends EntityRenderer<KiBarrierEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiexp1.png");
    private static final ResourceLocation TEXTURE_2 = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiexp1_border.png");

    private final KiBallPlaneModel model;

    public KiBarrierRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
        this.model = new KiBallPlaneModel(pContext.bakeLayer(KiBallPlaneModel.LAYER_LOCATION));
    }

    @Override
    public void render(KiBarrierEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float halfHeight = entity.getBbHeight() / 2.0F;
        poseStack.translate(0.0D, halfHeight, 0.0D);

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float scale = entity.getCurrentSize();
        poseStack.scale(scale, scale, scale);

        float ageInTicks = entity.tickCount + partialTick;

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        float[] auraColor2 = ColorUtils.rgbIntToFloat(entity.getColorBorde());

        VertexConsumer auraBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE));

        poseStack.translate(0.0D, -0.2D, 0.2D);

        this.model.renderToBuffer(
                poseStack,
                auraBuffer,
                15728880,
                OverlayTexture.NO_OVERLAY,
                auraColor[0], auraColor[1], auraColor[2],
                0.7F
        );
        VertexConsumer auraBuffer2 = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_2));
        this.model.renderToBuffer(
                poseStack,
                auraBuffer2,
                15728880,
                OverlayTexture.NO_OVERLAY,
                auraColor2[0], auraColor2[1], auraColor2[2],
                0.7F
        );
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KiBarrierEntity pEntity) {
        return TEXTURE;
    }
}