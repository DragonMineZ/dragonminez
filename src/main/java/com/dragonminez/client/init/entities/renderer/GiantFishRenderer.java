package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.GiantFishModel;
import com.dragonminez.common.init.entities.animal.GiantFishEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GiantFishRenderer<T extends GiantFishEntity> extends GeoEntityRenderer<T> {

    public GiantFishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GiantFishModel<>());
        this.shadowRadius = 1.1f;
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
