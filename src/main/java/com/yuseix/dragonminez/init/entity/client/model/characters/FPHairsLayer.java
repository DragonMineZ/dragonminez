package com.yuseix.dragonminez.init.entity.client.model.characters;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.client.character.layer.HairsLayer;
import com.yuseix.dragonminez.client.character.models.hair.*;
import com.yuseix.dragonminez.init.MainItems;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class FPHairsLayer<T extends LivingEntity, M extends PlayerModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation SUIT_TEX = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/hairtexture.png");
    private static final ResourceLocation EARS = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/races/namek/body/ears.png");

    private float colorR, colorG, colorB;

    private GokuHairModel gokuhair;
    private FemHairModel femhair;
    private VegetaHairModel vegetahair;
    private GohanDBSHairModel gohandbshair;
    private GohanTeenHairModel gohanteenhair;
    private TrunksHairModel trunkshair;
    private EarsNamek earsNamek;
    private TailModel cola;

    public FPHairsLayer(RenderLayerParent<T, M> pRenderer) {
        super(pRenderer);
            this.gokuhair = new GokuHairModel(GokuHairModel.createBodyLayer().bakeRoot());
            this.earsNamek = new EarsNamek(EarsNamek.createBodyLayer().bakeRoot());
            this.femhair = new FemHairModel(FemHairModel.createBodyLayer().bakeRoot());
            this.cola = new TailModel(TailModel.createBodyLayer().bakeRoot());
            this.vegetahair = new VegetaHairModel(VegetaHairModel.createBodyLayer().bakeRoot());
            this.gohandbshair = new GohanDBSHairModel(GohanDBSHairModel.createBodyLayer().bakeRoot());
        this.gohanteenhair = new GohanTeenHairModel(GohanTeenHairModel.createBodyLayer().bakeRoot());
        this.trunkshair = new TrunksHairModel(TrunksHairModel.createBodyLayer().bakeRoot());
    }


    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, T t, float v, float v1, float v2, float v3, float v4, float v5) {
        poseStack.pushPose();
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.entityTranslucent(SUIT_TEX));

        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, Minecraft.getInstance().player).ifPresent(cap -> {
            var raza = cap.getIntValue("race");
            var hairColor = cap.getIntValue("haircolor");
            var hairId = cap.getIntValue("hairid");
            var bodyColor = cap.getIntValue("bodycolor");
            var genero = cap.getStringValue("gender");

            colorR = (hairColor >> 16) / 255.0F;
            colorG = ((hairColor >> 8) & 0xff) / 255.0f;
            colorB = (hairColor & 0xff) / 255.0f;

            // Si el pelo es 0 y no es Namek, no renderizamos nada.
            if (hairId == 0 && (raza != 2)) return;
            // Si el jugador tiene invisibilidad, no renderizamos nada.
            if (Minecraft.getInstance().player.hasEffect(MobEffects.INVISIBILITY)) return;
            // Si el jugador tiene un casco, no renderizamos nada.
            if (!(Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) &&
                    !Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.HEAD).is(MainItems.INVENCIBLE_ARMOR_HELMET.get())) return;

            switch (raza){
                case 0: //Humano
                    if(hairId == 1){
                        VertexConsumer gokubase = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.GOKUHAIR_TEXT1));
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.gokuhair.renderToBuffer(poseStack,gokubase, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                    } else if(hairId == 2){
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.femhair.renderToBuffer(poseStack,vertexConsumer, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                    } else if(hairId == 3){
                        VertexConsumer vegetabase = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.VEGETAHAIR_TEXT1));
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.vegetahair.renderToBuffer(poseStack,vegetabase, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                    } else if(hairId == 4){
                        VertexConsumer gohandbs = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.GOHANDBSHAIR_TEXT1));
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.gohandbshair.renderToBuffer(poseStack,gohandbs, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                    } else if(hairId == 5){
                        VertexConsumer tex = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.GOHAN_TEEN_HAIR_TEXT1));
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.gohanteenhair.renderToBuffer(poseStack,tex, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                    } else if(hairId == 6){
                        VertexConsumer tex = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.TRUNKS_HAIR_TEXT1));
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.trunkshair.renderToBuffer(poseStack,tex, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                    }
                    break;
                case 1: //Saiyan

                    //Cola
                    this.cola.renderToBuffer(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY,0.410f,0.119f,0.00410f,1.0f);
                    //Cabellos
                        if(hairId == 1){
                            VertexConsumer gokubase = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.GOKUHAIR_TEXT1));
                            this.gokuhair.renderToBuffer(poseStack,gokubase, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);

                        } else if(hairId == 2){
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.femhair.renderToBuffer(poseStack,vertexConsumer, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);

                        } else if(hairId == 3){
                            VertexConsumer cabello = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.VEGETAHAIR_TEXT1));
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.vegetahair.renderToBuffer(poseStack,cabello, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);

                        } else if(hairId == 4){
                            VertexConsumer cabello = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.GOHANDBSHAIR_TEXT1));
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.gohandbshair.renderToBuffer(poseStack,cabello, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 5){
                            VertexConsumer tex = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.GOHAN_TEEN_HAIR_TEXT1));
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.gohanteenhair.renderToBuffer(poseStack,tex, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 6){
                            VertexConsumer tex = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.TRUNKS_HAIR_TEXT1));
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.trunkshair.renderToBuffer(poseStack,tex, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        }
                    break;
                case 2: //Namek
                    if(hairId == 0){
                        colorR = (bodyColor >> 16) / 255.0F;
                        colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                        colorB = (bodyColor & 0xff) / 255.0f;
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.earsNamek.renderEars1(poseStack, multiBufferSource.getBuffer(RenderType.entityCutout(EARS)), i, OverlayTexture.NO_OVERLAY, colorR, colorG, colorB, 1.0f);
                    } else if (hairId == 1){
                        colorR = (bodyColor >> 16) / 255.0F;
                        colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                        colorB = (bodyColor & 0xff) / 255.0f;
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.earsNamek.renderEars2(poseStack, multiBufferSource.getBuffer(RenderType.entityTranslucent(EARS)), i, OverlayTexture.NO_OVERLAY, colorR, colorG, colorB, 1.0f);
                    }
                    break;
                case 5: //Majin
                    if(genero.equals("female")){
                        if(hairId == 1){
                            colorR = (bodyColor >> 16) / 255.0F;
                            colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                            colorB = (bodyColor & 0xff) / 255.0f;
                            VertexConsumer gokubase = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.GOKUHAIR_TEXT1));
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.gokuhair.renderToBuffer(poseStack,gokubase, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 2){
                            colorR = (bodyColor >> 16) / 255.0F;
                            colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                            colorB = (bodyColor & 0xff) / 255.0f;
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.femhair.renderToBuffer(poseStack,vertexConsumer, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 3){
                            colorR = (bodyColor >> 16) / 255.0F;
                            colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                            colorB = (bodyColor & 0xff) / 255.0f;
                            VertexConsumer cabello = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.VEGETAHAIR_TEXT1));
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.vegetahair.renderToBuffer(poseStack,cabello, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 4){
                            colorR = (bodyColor >> 16) / 255.0F;
                            colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                            colorB = (bodyColor & 0xff) / 255.0f;
                            VertexConsumer cabello = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.GOHANDBSHAIR_TEXT1));
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.gohandbshair.renderToBuffer(poseStack,cabello, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 5){
                            colorR = (bodyColor >> 16) / 255.0F;
                            colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                            colorB = (bodyColor & 0xff) / 255.0f;
                            VertexConsumer tex = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.GOHAN_TEEN_HAIR_TEXT1));
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.gohanteenhair.renderToBuffer(poseStack,tex, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 6){
                            colorR = (bodyColor >> 16) / 255.0F;
                            colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                            colorB = (bodyColor & 0xff) / 255.0f;
                            VertexConsumer tex = multiBufferSource.getBuffer(RenderType.entityTranslucent(HairsLayer.TRUNKS_HAIR_TEXT1));
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.trunkshair.renderToBuffer(poseStack,tex, i, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        }
                    }
                    break;
                default:
                    break;

            }

        });

        poseStack.popPose();
    }
}
