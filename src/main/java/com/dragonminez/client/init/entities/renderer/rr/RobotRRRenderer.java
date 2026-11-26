package com.dragonminez.client.init.entities.renderer.rr;

import com.dragonminez.client.init.entities.model.rr.RedRibbonRobotModel;
import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;
import com.dragonminez.client.init.entities.renderer.rr.layer.RedRibbonSupervillainLayer;
import com.dragonminez.client.render.effects.AuraBorderRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RobotRRRenderer<T extends RedRibbonEntity> extends GeoEntityRenderer<T> {

    public RobotRRRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RedRibbonRobotModel<>());
        this.shadowRadius = 0.8f;
        this.addRenderLayer(new RedRibbonSupervillainLayer<>(this));
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        MultiBufferSource borderSource = RedRibbonSupervillainLayer.beginBorder(entity, bufferSource, partialTick);
        try {
            super.render(entity, entityYaw, partialTick, poseStack, borderSource, packedLight);
        } finally {
            AuraBorderRenderer.end(borderSource);
        }
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }
}
