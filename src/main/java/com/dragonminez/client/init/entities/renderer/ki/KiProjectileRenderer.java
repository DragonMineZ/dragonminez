package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallPlaneModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class KiProjectileRenderer extends EntityRenderer<AbstractKiProjectile> {

    private static final ResourceLocation TEXTURE_BORDER = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/kiball1_border.png");
    private static final ResourceLocation TEXTURE_CORE = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/kiball1.png");

    private final KiBallPlaneModel model;

    public KiProjectileRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);

        this.model = new KiBallPlaneModel(pContext.bakeLayer(KiBallPlaneModel.LAYER_LOCATION));
    }

    @Override
    public void render(AbstractKiProjectile entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float scale = entity.getSize();
        poseStack.scale(scale, scale, scale);

        float ageInTicks = entity.tickCount + partialTick;

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());

        VertexConsumer auraBuffer = buffer.getBuffer(ModRenderTypes.energy(TEXTURE_CORE));

        this.model.renderToBuffer(
                poseStack,
                auraBuffer,
                15728880,
                OverlayTexture.NO_OVERLAY,
                auraColor[0], auraColor[1], auraColor[2],
                1.0F
        );

        poseStack.pushPose();


        poseStack.translate(0, 0, -0.01F);

        float[] coreColor = ColorUtils.rgbIntToFloat(entity.getColorBorde());

        VertexConsumer coreBuffer = buffer.getBuffer(ModRenderTypes.glow(TEXTURE_BORDER));

        this.model.renderToBuffer(
                poseStack,
                coreBuffer,
                15728880,
                OverlayTexture.NO_OVERLAY,
                coreColor[0], coreColor[1], coreColor[2],
                1.0F);

        poseStack.popPose();

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractKiProjectile pEntity) {
        return TEXTURE_BORDER;
    }
}
