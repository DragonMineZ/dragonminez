package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.DinoFlyModel;
import com.dragonminez.common.init.entities.animal.DinoFlyEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DinoFlyRenderer extends GeoEntityRenderer<DinoFlyEntity> {

    public DinoFlyRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DinoFlyModel());
        this.shadowRadius = 0.8f;
    }

//    @Override
//    public RenderType getRenderType(DinoFlyEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
//        return RenderType.entity(texture);
//    }
}
