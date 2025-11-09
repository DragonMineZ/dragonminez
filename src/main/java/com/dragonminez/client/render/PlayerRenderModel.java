package com.dragonminez.client.render;

import com.dragonminez.client.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PlayerRenderModel<T extends AbstractClientPlayer & GeoAnimatable> extends GeoEntityRenderer<T> {

    public PlayerRenderModel(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);

        this.shadowRadius = 0.4f;

    }

}
