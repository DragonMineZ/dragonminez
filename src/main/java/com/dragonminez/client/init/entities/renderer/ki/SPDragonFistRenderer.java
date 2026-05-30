package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.client.init.entities.model.ki.SPDragonFistModel;
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

public class SPDragonFistRenderer<T extends SPDragonFistEntity> extends GeoEntityRenderer<T> {

    public SPDragonFistRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SPDragonFistModel<>());
        this.shadowRadius = 0.8f;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!entity.isFiring()) {
            return;
        }

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
        poseStack.scale(5.0f, 5.0f, 5.0f);

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