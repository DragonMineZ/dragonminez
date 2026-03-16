package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.ki.KiBallModel;
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
    private static final ResourceLocation TEXTURE_EXPLOSION = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");

    private final KiBallModel modelexp;

    public KiExplosionRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);

        this.modelexp = new KiBallModel(pContext.bakeLayer(KiBallModel.LAYER_LOCATION));

    }

    @Override
    public void render(KiExplosionEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float ageInTicks = entity.tickCount + partialTick;

        float fadeAlpha = 1.0F;
        int maxLife = entity.getMaxLife();
        int fadeTicks = 10;

        if (entity.tickCount >= maxLife - fadeTicks) {
            fadeAlpha = (maxLife - ageInTicks) / (float) fadeTicks;
            fadeAlpha = Math.max(0.0F, fadeAlpha);
        }

        renderExplosionCore(entity, partialTick, poseStack, buffer, fadeAlpha);
    }

    private void renderExplosionCore(KiExplosionEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, float fadealpha) {
        float ageInTicks = entity.tickCount + partialTick;
        float castTime = (float) entity.getCastExplosion();
        float halfCastTime = castTime / 2.0F;
        float expansionTime = 10.0F;

        float maxRadius = entity.getMaxRadius();
        float baseScale = entity.getSize();

        float currentScale;

        if (ageInTicks <= halfCastTime) {
            float progress = ageInTicks / halfCastTime;
            currentScale = baseScale * progress;
        }
        else if (ageInTicks <= castTime) {
            currentScale = baseScale;
        }
        else if (ageInTicks <= castTime + expansionTime) {
            float progress = (ageInTicks - castTime) / expansionTime;
            currentScale = baseScale + ((maxRadius - baseScale) * progress);
        }
        else {
            currentScale = maxRadius;
        }

        float[] auraColor = ColorUtils.rgbIntToFloat(entity.getColor());
        float[] brightnessColor = ColorUtils.lightenColor(auraColor,  0.5f);

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 0.0D);
        poseStack.scale(currentScale, currentScale, currentScale);
        poseStack.translate(0.0D, -1.3D, 0.0D);

        this.modelexp.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        VertexConsumer auraBuffer = buffer.getBuffer(ModRenderTypes.glow_ki(TEXTURE_EXPLOSION));

        this.modelexp.renderToBuffer(poseStack, auraBuffer, 15728880, OverlayTexture.NO_OVERLAY,
                brightnessColor[0], brightnessColor[1], brightnessColor[2], 0.5F * fadealpha);

        poseStack.scale(1.1f, 1.1f, 1.1f);
        poseStack.translate(0.0D, -0.1D, 0.0D);
        this.modelexp.renderToBuffer(poseStack, auraBuffer, 15728880, OverlayTexture.NO_OVERLAY,
                auraColor[0], auraColor[1], auraColor[2], 0.3F * fadealpha);

        poseStack.scale(1.3f, 1.3f, 1.3f);
        poseStack.translate(0.0D, -0.1D, 0.0D);
        this.modelexp.renderToBuffer(poseStack, auraBuffer, 15728880, OverlayTexture.NO_OVERLAY,
                auraColor[0], auraColor[1], auraColor[2], 0.1F * fadealpha);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(KiExplosionEntity pEntity) {
        return TEXTURE_BORDER;
    }
}
