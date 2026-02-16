package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallPlaneModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiExplosionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class KiExplosionRenderer extends EntityRenderer<KiExplosionEntity> {

    private static final ResourceLocation TEXTURE_BORDER = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiexp1_border.png");
    private static final ResourceLocation TEXTURE_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiexp1.png");

    private final KiBallPlaneModel model;

    public KiExplosionRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);

        this.model = new KiBallPlaneModel(pContext.bakeLayer(KiBallPlaneModel.LAYER_LOCATION));
    }

    @Override
    public void render(KiExplosionEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.translate(0.0D, 0.5D, 0.0D);

        float growTime = (float) KiExplosionEntity.GROW_TIME;
        float ageInTicks = entity.tickCount + partialTick;
        float progress = Mth.clamp(ageInTicks / growTime, 0.0F, 1.0F);
        float maxRadius = entity.getMaxRadius();
        float currentScale = maxRadius * 2.0F * progress;

        poseStack.scale(currentScale, currentScale, currentScale);

        poseStack.translate(0.0D, -0.5D, 0.0D);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        // RENDER CORE
        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        VertexConsumer auraBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_CORE));
        this.model.renderToBuffer(poseStack, auraBuffer, 15728880, OverlayTexture.NO_OVERLAY,
                auraColor[0], auraColor[1], auraColor[2], 1.0F);

        poseStack.pushPose();
        poseStack.translate(0, 0, -0.05F);
        float[] coreColor = ColorUtils.rgbIntToFloat(entity.getColorBorde());
        VertexConsumer coreBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_BORDER));
        this.model.renderToBuffer(poseStack, coreBuffer, 15728880, OverlayTexture.NO_OVERLAY,
                coreColor[0], coreColor[1], coreColor[2], 1.0F);
        poseStack.popPose();

        poseStack.popPose();
        //super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KiExplosionEntity pEntity) {
        return TEXTURE_BORDER;
    }
}
