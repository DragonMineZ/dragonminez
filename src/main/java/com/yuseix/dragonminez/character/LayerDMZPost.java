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
public class LayerDMZPost extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation OJOS = new ResourceLocation(DragonMineZ.MOD_ID, "textures/races/ojosblancos.png");

    private final FaceModel model;

    private static final ResourceLocation OJO1 = new ResourceLocation(DragonMineZ.MOD_ID, "textures/races/ojo1.png");
    private static final ResourceLocation OJO2 = new ResourceLocation(DragonMineZ.MOD_ID, "textures/races/ojo2.png");

    public LayerDMZPost(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);

        this.model = new FaceModel(FaceModel.createBodyLayer().bakeRoot());
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, AbstractClientPlayer abstractClientPlayer, float v, float v1, float v2, float v3, float v4, float v5) {



        FaceModel eye1 = new FaceModel(FaceModel.createBodyLayer().bakeRoot());
        FaceModel eye2 = new FaceModel(FaceModel.createBodyLayer().bakeRoot());

        poseStack.pushPose();

        VertexConsumer ojosblancos = multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(OJOS));

        this.model.Face.copyFrom(getParentModel().head);
        this.model.renderToBuffer(poseStack, ojosblancos, i, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.translate(0.0,0.0,0.0);

        VertexConsumer ojo1 = multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(OJO1));
        eye1.Face.copyFrom(getParentModel().head);
        eye1.renderToBuffer(poseStack, ojo1, i, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.translate(0,0.0,-0.001);

        VertexConsumer ojo2 = multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(OJO2));
        eye2.Face.copyFrom(getParentModel().head);
        eye2.renderToBuffer(poseStack, ojo2, i, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.translate(0,0.0,-0.001);

        poseStack.popPose();

    }


}
