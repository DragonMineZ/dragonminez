package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.client.init.entities.model.ki.SPSkillsModel;
import com.dragonminez.common.init.entities.ki.SPBlueHurricaneEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SPBlueHurricaneRenderer<T extends SPBlueHurricaneEntity> extends GeoEntityRenderer<T> {

    public SPBlueHurricaneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SPSkillsModel<>());
        this.shadowRadius = 0.8f;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        if (!animatable.isFiring()) {
            return Color.ofRGBA(1.0f, 1.0f, 1.0f, 0.0f);
        }

        float fadeDuration = 10.0f;

        float currentTick = (animatable.tickCount - animatable.getCastTime()) + partialTick;
        float maxLife = 140.0f;
        float remainingLife = maxLife - currentTick;

        float alpha = 1.0f;

        if (currentTick < fadeDuration) {
            alpha = currentTick / fadeDuration;
        } else if (remainingLife < fadeDuration) {
            alpha = remainingLife / fadeDuration;
        }

        alpha = Mth.clamp(alpha, 0.0f, 1.0f);

        return Color.ofRGBA(1.0f, 1.0f, 1.0f, alpha);
    }
}
