package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.GiantTurtleModel;
import com.dragonminez.common.init.entities.animal.GiantTurtleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GiantTurtleRenderer<T extends GiantTurtleEntity> extends GeoEntityRenderer<T> {

    public GiantTurtleRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GiantTurtleModel<>());
        this.shadowRadius = 0.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(T animatable) {
        return animatable.getCurrentTexture();
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
