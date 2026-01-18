package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.DinoGlobalModel;
import com.dragonminez.client.init.entities.model.NamekFrogModel;
import com.dragonminez.common.init.entities.animal.DinoGlobalEntity;
import com.dragonminez.common.init.entities.animal.NamekFrogEntity;
import com.dragonminez.common.init.entities.animal.NamekFrogGinyuEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.naming.Name;

public class NamekFrogRenderer<T extends NamekFrogEntity> extends GeoEntityRenderer<T> {

    private static final ResourceLocation GINYU_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/animal/namekfrog_0.png");

    public NamekFrogRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NamekFrogModel<>());
        this.shadowRadius = 0.25f;
    }

    @Override
    public ResourceLocation getTextureLocation(T animatable) {
        if (animatable instanceof NamekFrogGinyuEntity) {
            return GINYU_TEXTURE;
        }
        return animatable.getCurrentTexture();
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }

}
