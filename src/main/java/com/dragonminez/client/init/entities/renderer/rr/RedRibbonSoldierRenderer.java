package com.dragonminez.client.init.entities.renderer.rr;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.rr.RedRibbonModel;
import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;
import com.dragonminez.common.init.entities.redribbon.RedRibbonSoldierEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RedRibbonSoldierRenderer<T extends RedRibbonSoldierEntity> extends GeoEntityRenderer<T> {

    public RedRibbonSoldierRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RedRibbonModel<>());
        this.shadowRadius = 0.4f;
    }

    @Override
    public ResourceLocation getTextureLocation(T animatable) {
        return animatable.getCurrentTexture();
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }
}
