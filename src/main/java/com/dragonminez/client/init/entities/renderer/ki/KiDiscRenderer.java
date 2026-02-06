package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallPlaneModel;
import com.dragonminez.client.init.entities.model.ki.KiDiscModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiDiscEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class KiDiscRenderer extends EntityRenderer<KiDiscEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kidisc.png");

    private final KiDiscModel model;

    public KiDiscRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);

        this.model = new KiDiscModel(pContext.bakeLayer(KiDiscModel.LAYER_LOCATION));
    }

    @Override
    public void render(KiDiscEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float yaw = entity.getYRot();
        float pitch = entity.getXRot();

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        float scale = entity.getSize();
        poseStack.scale(scale, 1.5f, scale);

        float ageInTicks = entity.tickCount + partialTick;

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());

        VertexConsumer auraBuffer = buffer.getBuffer(ModRenderTypes.glow(TEXTURE));

        this.model.renderToBuffer(
                poseStack,
                auraBuffer,
                15728880,
                OverlayTexture.NO_OVERLAY,
                auraColor[0], auraColor[1], auraColor[2],
                1.0F
        );
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KiDiscEntity pEntity) {
        return TEXTURE;
    }
}
