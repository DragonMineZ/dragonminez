package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.MasterGlobalModel;
import com.dragonminez.common.init.entities.MastersEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MasterEntityRenderer <T extends MastersEntity> extends GeoEntityRenderer<T> {

    public MasterEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MasterGlobalModel<>());
        this.shadowRadius = 0.8f;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }
}
