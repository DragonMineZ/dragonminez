package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiDiscModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.KiDiskEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class KiDiscRenderer extends EntityRenderer<KiDiskEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kidisc.png");

    private final KiDiscModel model;

    public KiDiscRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);

        this.model = new KiDiscModel(pContext.bakeLayer(KiDiscModel.LAYER_LOCATION));
    }

    @Override
    public void render(KiDiskEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float ageInTicks = entity.tickCount + partialTick;
        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        float scale = entity.getSize();

        boolean isFiring = entity.isFiring();
        float castTime = (float) entity.getCastTime();

        if (!isFiring) {
            if (castTime > 0.1F && ageInTicks <= castTime) {
                scale *= (ageInTicks / castTime);
            }
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        if (!isFiring) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }

        poseStack.scale(scale, 1.0f, scale);

        renderDisc(entity, poseStack, buffer, ageInTicks);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderDisc(KiDiskEntity entity, PoseStack poseStack, MultiBufferSource buffer, float ageInTicks) {
        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        VertexConsumer auraBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE));

        this.model.renderToBuffer(
                poseStack,
                auraBuffer,
                15728880,
                OverlayTexture.NO_OVERLAY,
                auraColor[0], auraColor[1], auraColor[2],
                1.0F
        );
    }
    @Override
    public ResourceLocation getTextureLocation(KiDiskEntity pEntity) {
        return TEXTURE;
    }
}
