package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.client.init.entities.model.ki.SPDragonFistModel;
import com.dragonminez.client.render.effects.AuraTrailRenderer;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SPDragonFistRenderer<T extends SPDragonFistEntity> extends GeoEntityRenderer<T> {
    private static final float[] TRAIL_COLOR = ColorUtils.rgbIntToFloat(0xFFC21A);
    private static final float TRAIL_ALPHA = 0.75f;
    private static final float FIST_OFFSET = 1.5f;
    private static final float TRAIL_BEHIND_CASTER = 0.75f;
    private static final float MODEL_SCALE = 3.0f;
    private static final float MODEL_CENTER_Y = 1.21f;
    private static final float MODEL_HEAD_OFFSET = 11.5f;
    private static final float EMERGE_TICKS = 6.0f;

    public SPDragonFistRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SPDragonFistModel<>());
        this.shadowRadius = 0.8f;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!entity.isFiring()) {
            return;
        }

        Entity owner = entity.getOwner();
        float anchorBack = FIST_OFFSET * Mth.cos(entity.getLockedPitch() * Mth.DEG_TO_RAD) + TRAIL_BEHIND_CASTER;
        float anchorHeight = owner != null ? owner.getBbHeight() * 0.5f : 0.9f;
        AuraTrailRenderer.submitFlightTrail(entity, poseStack.last().pose(), partialTick, TRAIL_COLOR, TRAIL_ALPHA,
                anchorBack, anchorHeight);

        poseStack.pushPose();

        float activeTick = entity.tickCount + partialTick;
        float shakeIntensity;

        if (activeTick < (entity.getMaxLife() / 2.0f)) {
            shakeIntensity = 0.15f;
        } else {
            shakeIntensity = 0.04f;
        }

        float shakeX = (entity.level().random.nextFloat() - 0.5f) * shakeIntensity;
        float shakeY = (entity.level().random.nextFloat() - 0.5f) * shakeIntensity;
        float shakeZ = (entity.level().random.nextFloat() - 0.5f) * shakeIntensity;

        poseStack.translate(shakeX, shakeY, shakeZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getLockedYaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getLockedPitch()));

        float scale = MODEL_SCALE * emergeScale(activeTick);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0f, -MODEL_CENTER_Y, -MODEL_HEAD_OFFSET);

        super.render(entity, 0.0F, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    private static float emergeScale(float activeTick) {
        float t = Mth.clamp(activeTick / EMERGE_TICKS, 0.0f, 1.0f) - 1.0f;
        float overshoot = 1.0f + 2.70158f * t * t * t + 1.70158f * t * t;
        return 0.15f + 0.85f * overshoot;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        float fadeDuration = 10.0f;
        float activeTick = animatable.tickCount + partialTick;

        float maxActiveLife = animatable.getMaxLife();
        float remainingLife = maxActiveLife - activeTick;

        float alpha = 0.5f;

        if (activeTick < fadeDuration) {
            alpha = (activeTick / fadeDuration) * 0.8f;
        }
        else if (remainingLife < fadeDuration) {
            alpha = (remainingLife / fadeDuration) * 0.8f;
        }
        else {
            alpha = 0.8f;
        }

        alpha = Mth.clamp(alpha, 0.0f, 0.8f);

        return Color.ofRGBA(1.0f, 1.0f, 1.0f, alpha);
    }
}