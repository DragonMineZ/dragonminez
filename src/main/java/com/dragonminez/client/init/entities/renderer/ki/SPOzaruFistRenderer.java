package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.client.init.entities.model.ki.SPDragonFistModel;
import com.dragonminez.client.init.entities.model.ki.SPOzaruFistModel;
import com.dragonminez.client.render.effects.AuraTrailRenderer;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.entities.ki.OzaruFistEntity;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SPOzaruFistRenderer<T extends OzaruFistEntity> extends GeoEntityRenderer<T> {
    private static final float[] TRAIL_COLOR = ColorUtils.rgbIntToFloat(0xE0A35C);
    private static final float TRAIL_ALPHA = 0.65f;
    private static final int TRAIL_SAMPLES = 24;
    private static final float TRAIL_HALF_WIDTH = 1.80f;
    private static final float TRAIL_ANCHOR_BACK = 0.00f;
    private static final float TRAIL_ANCHOR_HEIGHT = 0.50f;

    public SPOzaruFistRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SPOzaruFistModel<>());
        this.shadowRadius = 0.8f;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!entity.isFiring()) {
            return;
        }

        AuraTrailRenderer.submitEntityTrail(entity, poseStack.last().pose(), partialTick, TRAIL_COLOR, TRAIL_ALPHA,
                TRAIL_SAMPLES, TRAIL_HALF_WIDTH, TRAIL_ANCHOR_BACK, TRAIL_ANCHOR_HEIGHT);

        poseStack.pushPose();
        poseStack.scale(5.0f, 5.0f, 5.0f);
        poseStack.translate(0.0f, -7.0f, -1.0f);

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


        super.render(entity, 0.0F, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
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
        float maxAlpha = 0.75f;

        float alpha;

        if (activeTick < fadeDuration) {
            alpha = (activeTick / fadeDuration) * maxAlpha;
        }
        else if (remainingLife < fadeDuration) {
            alpha = (remainingLife / fadeDuration) * maxAlpha;
        }
        else {
            alpha = maxAlpha;
        }

        alpha = Mth.clamp(alpha, 0.0f, maxAlpha);

        return Color.ofRGBA(1.0f, 1.0f, 1.0f, alpha);
    }
}