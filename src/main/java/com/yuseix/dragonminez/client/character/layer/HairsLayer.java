package com.yuseix.dragonminez.client.character.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.client.character.models.hair.*;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;

public class HairsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    //TEXTURAS CABELLO
    private static final ResourceLocation SUIT_TEX = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/hairtexture.png");
    public static final ResourceLocation GOKUHAIR_TEXT1 = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/races/hair/goku/gokubasehair1.png");
    public static final ResourceLocation GOKUHAIR_TEXT2 = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/races/hair/goku/gokubasehair2.png");
    public static final ResourceLocation VEGETAHAIR_TEXT1 = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/races/hair/vegeta/vegetahair1.png");
    public static final ResourceLocation VEGETAHAIR_TEXT2 = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/races/hair/vegeta/vegetahair2.png");
    public static final ResourceLocation GOHANDBSHAIR_TEXT1 = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/races/hair/gohandbs/gohandbshair.png");
    public static final ResourceLocation GOHANDBSHAIR_TEXT2 = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/races/hair/gohandbs/gohandbshair2.png");
    public static final ResourceLocation GOHAN_TEEN_HAIR_TEXT1 = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/races/hair/gohandbs/gohanteen1.png");
    public static final ResourceLocation GOHAN_TEEN_HAIR_TEXT2 = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/races/hair/gohandbs/gohanteen2.png");
    public static final ResourceLocation TRUNKS_HAIR_TEXT1 = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/races/hair/trunks/trunks.png");
    public static final ResourceLocation TRUNKS_HAIR_TEXT2 = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/races/hair/trunks/trunks2.png");

    private static final ResourceLocation EARS = new ResourceLocation(DragonMineZ.MOD_ID, "textures/entity/races/namek/body/ears.png");

    private float colorR, colorG, colorB;

    private GokuHairModel gokuhair;
    private FemHairModel femhair;
    private VegetaHairModel vegetahair;
    private GohanDBSHairModel gohandbshair;
    private GohanTeenHairModel gohanteenhair;
    private TrunksHairModel trunkshair;
    private UBDynamicHair ubhair;

    private EarsNamek earsNamek;
    private TailModel cola;

    public HairsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pRenderer) {
        super(pRenderer);
        this.gokuhair = new GokuHairModel(GokuHairModel.createBodyLayer().bakeRoot());
        this.earsNamek = new EarsNamek(EarsNamek.createBodyLayer().bakeRoot());
        this.femhair = new FemHairModel(FemHairModel.createBodyLayer().bakeRoot());
        this.cola = new TailModel(TailModel.createBodyLayer().bakeRoot());
        this.vegetahair = new VegetaHairModel(VegetaHairModel.createBodyLayer().bakeRoot());
        this.gohandbshair = new GohanDBSHairModel(GohanDBSHairModel.createBodyLayer().bakeRoot());
        this.gohanteenhair = new GohanTeenHairModel(GohanTeenHairModel.createBodyLayer().bakeRoot());
        this.trunkshair = new TrunksHairModel(TrunksHairModel.createBodyLayer().bakeRoot());
        this.ubhair = new UBDynamicHair(UBDynamicHair.createBodyLayer().bakeRoot());
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, AbstractClientPlayer abstractClientPlayer, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        poseStack.pushPose();
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.entityTranslucent(SUIT_TEX));

        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE,abstractClientPlayer).ifPresent(cap -> {
            var raza = cap.getRace();
            var hairColor = cap.getHairColor();
            var hairId = cap.getHairID();
            var bodyColor = cap.getBodyColor();
            var genero = cap.getGender();
            var transformation = cap.getDmzState();

            colorR = (hairColor >> 16) / 255.0F;
            colorG = ((hairColor >> 8) & 0xff) / 255.0f;
            colorB = (hairColor & 0xff) / 255.0f;

            // Si el pelo es 0 y no es Namek, no renderizamos nada.
            if (hairId == 0 && (raza != 2)) return;
            if (abstractClientPlayer.hasEffect(MobEffects.INVISIBILITY)) return;

            switch (raza){
                case 0: //Humano
                    if(hairId == 1){
                        VertexConsumer gokubase = multiBufferSource.getBuffer(RenderType.entityTranslucent(GOKUHAIR_TEXT1));
                        this.gokuhair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.gokuhair.renderToBuffer(poseStack,gokubase, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                    } else if(hairId == 2){
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.femhair.renderToBuffer(poseStack,vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                    } else if(hairId == 3){
                        VertexConsumer vegetabase = multiBufferSource.getBuffer(RenderType.entityTranslucent(VEGETAHAIR_TEXT1));
                        this.vegetahair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.vegetahair.renderToBuffer(poseStack,vegetabase, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                    } else if(hairId == 4){
                        VertexConsumer gohandbs = multiBufferSource.getBuffer(RenderType.entityTranslucent(GOHANDBSHAIR_TEXT1));
                        this.gohandbshair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.gohandbshair.renderToBuffer(poseStack,gohandbs, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                    } else if(hairId == 5){
                        VertexConsumer gohanteen = multiBufferSource.getBuffer(RenderType.entityTranslucent(GOHAN_TEEN_HAIR_TEXT1));
                        this.gohanteenhair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.gohanteenhair.renderToBuffer(poseStack,gohanteen, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                    } else if(hairId == 6){
                        VertexConsumer trunks = multiBufferSource.getBuffer(RenderType.entityTranslucent(TRUNKS_HAIR_TEXT1));
                        this.trunkshair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.trunkshair.renderToBuffer(poseStack,trunks, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                    }
                    break;
                case 1: //Saiyan

                    //Cola
                    this.getParentModel().copyPropertiesTo(this.cola);
                    this.cola.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount,  ageInTicks, netHeadYaw,  headPitch);
                    this.cola.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,0.410f,0.119f,0.00410f,1.0f);

                    //Cabellos

                    if(transformation == 0){ //Base
                        if(hairId == 1){
                            VertexConsumer gokubase = multiBufferSource.getBuffer(RenderType.entityTranslucent(GOKUHAIR_TEXT1));
                            this.gokuhair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.gokuhair.renderToBuffer(poseStack,gokubase, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);

                        } else if(hairId == 2){
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.femhair.renderToBuffer(poseStack,vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);

                        } else if(hairId == 3){
                            VertexConsumer cabello = multiBufferSource.getBuffer(RenderType.entityTranslucent(VEGETAHAIR_TEXT1));
                            this.vegetahair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.vegetahair.renderToBuffer(poseStack,cabello, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);

                        } else if(hairId == 4){
                            VertexConsumer cabello = multiBufferSource.getBuffer(RenderType.entityTranslucent(GOHANDBSHAIR_TEXT1));
                            this.gohandbshair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.gohandbshair.renderToBuffer(poseStack,cabello, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 5){
                            VertexConsumer gohanteen = multiBufferSource.getBuffer(RenderType.entityTranslucent(GOHAN_TEEN_HAIR_TEXT1));
                            this.gohanteenhair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.gohanteenhair.renderToBuffer(poseStack,gohanteen, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 6){
                            VertexConsumer trunks = multiBufferSource.getBuffer(RenderType.entityTranslucent(TRUNKS_HAIR_TEXT1));
                            this.trunkshair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.trunkshair.renderToBuffer(poseStack,trunks, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 7) {
                            VertexConsumer ubhair = multiBufferSource.getBuffer(RenderType.entityTranslucent(GOKUHAIR_TEXT1));
                            this.ubhair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.ubhair.renderToBuffer(poseStack, ubhair, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        }

                    } else if(transformation == 1){ //Ozaru osea aqui no hacemos nada

                    } else if(transformation == 2){ //Super saiyajin 1
                        //Color del ssj (Obvio no?)
                        var colorSSJ1 = 16777114;
                        colorR = (colorSSJ1 >> 16) / 255.0F;
                        colorG = ((colorSSJ1 >> 8) & 0xff) / 255.0f;
                        colorB = (colorSSJ1 & 0xff) / 255.0f;

                        if(hairId == 1){
                            //Goku ssj
                        } else if(hairId == 2){
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.femhair.renderToBuffer(poseStack,vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);

                        } else if(hairId == 3){
                            VertexConsumer cabello = multiBufferSource.getBuffer(RenderType.entityTranslucent(VEGETAHAIR_TEXT1));
                            this.vegetahair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.vegetahair.renderToBuffer(poseStack,cabello, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);

                        } else if(hairId == 4){
                            //Gohan dbs ssj
                        }
                    } else if(transformation == 3){ //Super Saiyajin grado 2

                    } else if(transformation == 4){ //Super saiyajin grado 3

                    } else if(transformation == 5){ //Super saiyajin full power osea el ssj1

                    } else if(transformation == 6){ //Supa saiyajin 2

                    } else { //Mas transformaciones pero me dio paja seguir jeje


                    }


                    break;
                case 2: //Namek
                    if(hairId == 0){
                        colorR = (bodyColor >> 16) / 255.0F;
                        colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                        colorB = (bodyColor & 0xff) / 255.0f;
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.earsNamek.renderEars1(poseStack, multiBufferSource.getBuffer(RenderType.entityCutout(EARS)), packedLight, OverlayTexture.NO_OVERLAY, colorR, colorG, colorB, 1.0f);
                    } else if (hairId == 1){
                        colorR = (bodyColor >> 16) / 255.0F;
                        colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                        colorB = (bodyColor & 0xff) / 255.0f;
                        this.getParentModel().getHead().translateAndRotate(poseStack);
                        this.earsNamek.renderEars2(poseStack, multiBufferSource.getBuffer(RenderType.entityTranslucent(EARS)), packedLight, OverlayTexture.NO_OVERLAY, colorR, colorG, colorB, 1.0f);
                    }
                    break;
                case 5: //Majin
                    if(genero.equals("Female")){
                        if(hairId == 1){
                            colorR = (bodyColor >> 16) / 255.0F;
                            colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                            colorB = (bodyColor & 0xff) / 255.0f;
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.femhair.renderToBuffer(poseStack,vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 2){
                            colorR = (bodyColor >> 16) / 255.0F;
                            colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                            colorB = (bodyColor & 0xff) / 255.0f;
                            VertexConsumer gokubase = multiBufferSource.getBuffer(RenderType.entityTranslucent(GOKUHAIR_TEXT1));
                            this.gokuhair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.gokuhair.renderToBuffer(poseStack,gokubase, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 3){
                            colorR = (bodyColor >> 16) / 255.0F;
                            colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                            colorB = (bodyColor & 0xff) / 255.0f;
                            VertexConsumer cabello = multiBufferSource.getBuffer(RenderType.entityTranslucent(VEGETAHAIR_TEXT1));
                            this.vegetahair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.vegetahair.renderToBuffer(poseStack,cabello, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 4){
                            colorR = (bodyColor >> 16) / 255.0F;
                            colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                            colorB = (bodyColor & 0xff) / 255.0f;
                            VertexConsumer cabello = multiBufferSource.getBuffer(RenderType.entityTranslucent(GOHANDBSHAIR_TEXT1));
                            this.gohandbshair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.gohandbshair.renderToBuffer(poseStack,cabello, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 5){
                            colorR = (bodyColor >> 16) / 255.0F;
                            colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                            colorB = (bodyColor & 0xff) / 255.0f;
                            VertexConsumer gohanteen = multiBufferSource.getBuffer(RenderType.entityTranslucent(GOHAN_TEEN_HAIR_TEXT1));
                            this.gohanteenhair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.gohanteenhair.renderToBuffer(poseStack,gohanteen, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
                        } else if(hairId == 6){
                            colorR = (bodyColor >> 16) / 255.0F;
                            colorG = ((bodyColor >> 8) & 0xff) / 255.0f;
                            colorB = (bodyColor & 0xff) / 255.0f;
                            VertexConsumer trunks = multiBufferSource.getBuffer(RenderType.entityTranslucent(TRUNKS_HAIR_TEXT1));
                            this.trunkshair.setupAnim(abstractClientPlayer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                            this.getParentModel().getHead().translateAndRotate(poseStack);
                            this.trunkshair.renderToBuffer(poseStack,trunks, packedLight, OverlayTexture.NO_OVERLAY, colorR,colorG,colorB,1.0f);
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
