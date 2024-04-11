package com.yuseix.dragonminez.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.events.ModEvents;
import com.yuseix.dragonminez.stats.PlayerStatsAttrProvider;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;

@OnlyIn(Dist.CLIENT)
public class LayerDMZBase extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation SH_BODY1 = new ResourceLocation(DragonMineZ.MOD_ID, "textures/races/ojos.png");



    public LayerDMZBase(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pRenderer) {
        super(pRenderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, AbstractClientPlayer abstractClientPlayer, float v, float v1, float v2, float v3, float v4, float v5) {


        PlayerStatsAttrProvider.getCap(ModEvents.INSTANCE,abstractClientPlayer).ifPresent(cap -> {

            int raza = cap.getRace();
            int bodyType = cap.getBodytype();

                switch(raza){
                    case 0: //HUMANO
                        if(bodyType == 0){
                            VertexConsumer skin_default = multiBufferSource.getBuffer(RenderType.entitySolid(abstractClientPlayer.getSkinTextureLocation()));

                            poseStack.pushPose();

                            this.getParentModel().head.render(poseStack, skin_default, i, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
                            this.getParentModel().body.render(poseStack, skin_default, i, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
                            this.getParentModel().leftArm.render(poseStack, skin_default, i, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
                            this.getParentModel().leftLeg.render(poseStack, skin_default, i, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
                            this.getParentModel().rightArm.render(poseStack, skin_default, i, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
                            this.getParentModel().rightLeg.render(poseStack, skin_default, i, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);


                            poseStack.popPose();

                            abstractClientPlayer.sendSystemMessage(Component.literal(abstractClientPlayer.getModelName()));


                        } else if(bodyType == 1){
                            VertexConsumer skin_type1 = multiBufferSource.getBuffer(RenderType.entitySolid(SH_BODY1));

                            poseStack.pushPose();


                            this.getParentModel().head.render(poseStack, skin_type1, i, OverlayTexture.NO_OVERLAY, 0.920f, 0.745f, 0.745f, 1.0f);
                            this.getParentModel().body.render(poseStack, skin_type1, i, OverlayTexture.NO_OVERLAY, 0.810f, 0.624f, 0.624f, 1.0f);
                            this.getParentModel().leftArm.render(poseStack, skin_type1, i, OverlayTexture.NO_OVERLAY, 0.920f, 0.745f, 0.745f, 1.0f);
                            this.getParentModel().leftLeg.render(poseStack, skin_type1, i, OverlayTexture.NO_OVERLAY, 0.920f, 0.745f, 0.745f, 1.0f);
                            this.getParentModel().rightArm.render(poseStack, skin_type1, i, OverlayTexture.NO_OVERLAY, 0.920f, 0.745f, 0.745f, 1.0f);
                            this.getParentModel().rightLeg.render(poseStack, skin_type1, i, OverlayTexture.NO_OVERLAY, 0.920f, 0.745f, 0.745f, 1.0f);

                            poseStack.popPose();

                            this.getParentModel().setAllVisible(false);


                            abstractClientPlayer.sendSystemMessage(Component.literal(abstractClientPlayer.getModelName()));


                        }

                        break;
                    case 1: //SAIYAN
                        break;
                    case 2: //NAMEK
                        break;
                    case 3: //BIOANDROIDE
                        break;
                    case 4: //COLD DEMON
                        break;
                    case 5: //MAJIN
                        break;
                    default:
                        break;
                }


        });


    }
}
