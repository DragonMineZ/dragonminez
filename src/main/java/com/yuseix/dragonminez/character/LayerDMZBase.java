package com.yuseix.dragonminez.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yuseix.dragonminez.DragonMineZ;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LayerDMZBase extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation OJOS = new ResourceLocation(DragonMineZ.MOD_ID, "textures/races/ojos.png");


    public LayerDMZBase(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pRenderer) {
        super(pRenderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, AbstractClientPlayer abstractClientPlayer, float v, float v1, float v2, float v3, float v4, float v5) {

        VertexConsumer vertexBuilder = multiBufferSource.getBuffer(RenderType.entityTranslucent(OJOS));


        this.getParentModel().hat.visible = false;
        this.getParentModel().rightSleeve.visible = false;
        this.getParentModel().leftSleeve.visible = false;
        this.getParentModel().rightPants.visible = false;
        this.getParentModel().leftPants.visible = false;


        poseStack.pushPose();

        this.getParentModel().head.render(poseStack, vertexBuilder, i, OverlayTexture.NO_OVERLAY, 0.920f, 0.745f, 0.745f, 1.0f);
        this.getParentModel().body.render(poseStack, vertexBuilder, i, OverlayTexture.NO_OVERLAY, 0.810f, 0.624f, 0.624f, 1.0f);
        this.getParentModel().leftArm.render(poseStack, vertexBuilder, i, OverlayTexture.NO_OVERLAY, 0.920f, 0.745f, 0.745f, 1.0f);
        this.getParentModel().leftLeg.render(poseStack, vertexBuilder, i, OverlayTexture.NO_OVERLAY, 0.920f, 0.745f, 0.745f, 1.0f);
        this.getParentModel().rightArm.render(poseStack, vertexBuilder, i, OverlayTexture.NO_OVERLAY, 0.920f, 0.745f, 0.745f, 1.0f);
        this.getParentModel().rightLeg.render(poseStack, vertexBuilder, i, OverlayTexture.NO_OVERLAY, 0.920f, 0.745f, 0.745f, 1.0f);

        poseStack.popPose();
    }
}
