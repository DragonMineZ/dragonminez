package com.yuseix.dragonminez.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import com.yuseix.dragonminez.utils.TextureManager;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

@OnlyIn(Dist.CLIENT)
public class RenderManos extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private float colorR, colorG, colorB;

    public RenderManos(EntityRendererProvider.Context pContext) {
        super(pContext, new PlayerModel(pContext.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.addLayer(new PlayerItemInHandLayer(this, pContext.getItemInHandRenderer()));

    }

    public void renderRightHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer) {
        this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, ((PlayerModel)this.model).rightArm, ((PlayerModel)this.model).rightSleeve);
    }

    public void renderLeftHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer) {
        this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, ((PlayerModel)this.model).leftArm, ((PlayerModel)this.model).leftSleeve);
    }

        @Override
    protected void setupRotations(AbstractClientPlayer pEntityLiving, PoseStack pPoseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
        super.setupRotations(pEntityLiving, pPoseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
    }

    @Override
    public void render(AbstractClientPlayer pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    private void renderHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer, ModelPart pRendererArm, ModelPart pRendererArmwear) {
        PlayerModel<AbstractClientPlayer> playermodel = (PlayerModel)this.getModel();
        this.setModelProperties(pPlayer);
        playermodel.attackTime = 0.0F;
        playermodel.crouching = false;
        playermodel.swimAmount = 0.0F;
        playermodel.setupAnim(pPlayer, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        pRendererArm.xRot = 0.0F;

        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE,pPlayer).ifPresent(cap -> {

            var raza = cap.getRace();
            var bodytype = cap.getBodytype();
            var color1body = cap.getBodyColor();
            var color2body = cap.getBodyColor2();
            var color3body = cap.getBodyColor3();

            switch (raza){
                case 0:
                    //HUMANO
                    if(bodytype == 0){
                        pRendererArm.render(pPoseStack, pBuffer.getBuffer(RenderType.entitySolid(pPlayer.getSkinTextureLocation())), pCombinedLight, OverlayTexture.NO_OVERLAY);
                    } else if(bodytype == 1){
                        colorR = (color1body >> 16) / 255.0F;
                        colorG = ((color1body >> 8) & 0xff) / 255.0f;
                        colorB = (color1body & 0xff) / 255.0f;
                        pRendererArm.render(pPoseStack, pBuffer.getBuffer(RenderType.entitySolid(TextureManager.SH_BODY1)), pCombinedLight, OverlayTexture.NO_OVERLAY,colorR,colorG,colorB,1.0f);
                    }

                    break;
                case 1:
                    //SAIYAN
                    if(bodytype == 0){
                        pRendererArm.render(pPoseStack, pBuffer.getBuffer(RenderType.entitySolid(pPlayer.getSkinTextureLocation())), pCombinedLight, OverlayTexture.NO_OVERLAY);
                    } else if(bodytype == 1){
                        colorR = (color1body >> 16) / 255.0F;
                        colorG = ((color1body >> 8) & 0xff) / 255.0f;
                        colorB = (color1body & 0xff) / 255.0f;
                        pRendererArm.render(pPoseStack, pBuffer.getBuffer(RenderType.entitySolid(TextureManager.SH_BODY1)), pCombinedLight, OverlayTexture.NO_OVERLAY,colorR,colorG,colorB,1.0f);
                    }

                    break;
                case 2:
                    //NAMEK
                    colorR = (color1body >> 16) / 255.0F;
                    colorG = ((color1body >> 8) & 0xff) / 255.0f;
                    colorB = (color1body & 0xff) / 255.0f;
                    pRendererArm.render(pPoseStack, pBuffer.getBuffer(RenderType.entityTranslucent(TextureManager.N_BASE_BODY1_PART1)), pCombinedLight, OverlayTexture.NO_OVERLAY,colorR,colorG,colorB,1.0f);
                    colorR = (color2body >> 16) / 255.0F;
                    colorG = ((color2body >> 8) & 0xff) / 255.0f;
                    colorB = (color2body & 0xff) / 255.0f;
                    pRendererArm.render(pPoseStack, pBuffer.getBuffer(RenderType.entityTranslucent(TextureManager.N_BASE_BODY1_PART2)), pCombinedLight, OverlayTexture.NO_OVERLAY,colorR,colorG,colorB,1.0f);
                    colorR = (color3body >> 16) / 255.0F;
                    colorG = ((color3body >> 8) & 0xff) / 255.0f;
                    colorB = (color3body & 0xff) / 255.0f;
                    pRendererArm.render(pPoseStack, pBuffer.getBuffer(RenderType.entityTranslucent(TextureManager.N_BASE_BODY1_PART3)), pCombinedLight, OverlayTexture.NO_OVERLAY,colorR,colorG,colorB,1.0f);

                    break;
                case 3:
                    //BIOANDROID
                    colorR = (color1body >> 16) / 255.0F;
                    colorG = ((color1body >> 8) & 0xff) / 255.0f;
                    colorB = (color1body & 0xff) / 255.0f;
                    pRendererArm.render(pPoseStack, pBuffer.getBuffer(RenderType.entityTranslucent(TextureManager.B_IMPERFECT_ARMS1)), pCombinedLight, OverlayTexture.NO_OVERLAY,colorR,colorG,colorB,1.0f);
                    colorR = (color2body >> 16) / 255.0F;
                    colorG = ((color2body >> 8) & 0xff) / 255.0f;
                    colorB = (color2body & 0xff) / 255.0f;
                    pRendererArm.render(pPoseStack, pBuffer.getBuffer(RenderType.entityTranslucent(TextureManager.B_IMPERFECT_ARMS2)), pCombinedLight, OverlayTexture.NO_OVERLAY,colorR,colorG,colorB,1.0f);
                    colorR = (color3body >> 16) / 255.0F;
                    colorG = ((color3body >> 8) & 0xff) / 255.0f;
                    colorB = (color3body & 0xff) / 255.0f;
                    pRendererArm.render(pPoseStack, pBuffer.getBuffer(RenderType.entityTranslucent(TextureManager.B_IMPERFECT_ARMS3)), pCombinedLight, OverlayTexture.NO_OVERLAY,colorR,colorG,colorB,1.0f);
                    break;
                case 4:
                    break;
                case 5:
                    //MAJIN
                    colorR = (color1body >> 16) / 255.0F;
                    colorG = ((color1body >> 8) & 0xff) / 255.0f;
                    colorB = (color1body & 0xff) / 255.0f;
                    pRendererArm.render(pPoseStack, pBuffer.getBuffer(RenderType.entitySolid(TextureManager.SH_BODY1)), pCombinedLight, OverlayTexture.NO_OVERLAY,colorR,colorG,colorB,1.0f);
                    break;
                default:
                    break;
            }

        });
}

    private void setModelProperties(AbstractClientPlayer pClientPlayer) {
        PlayerModel<AbstractClientPlayer> playermodel = (PlayerModel)this.getModel();
        if (pClientPlayer.isSpectator()) {
            playermodel.setAllVisible(false);
            playermodel.head.visible = true;
            playermodel.hat.visible = true;
        } else {
            playermodel.setAllVisible(true);
            playermodel.hat.visible = pClientPlayer.isModelPartShown(PlayerModelPart.HAT);
            playermodel.jacket.visible = pClientPlayer.isModelPartShown(PlayerModelPart.JACKET);
            playermodel.leftPants.visible = pClientPlayer.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
            playermodel.rightPants.visible = pClientPlayer.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
            playermodel.leftSleeve.visible = pClientPlayer.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
            playermodel.rightSleeve.visible = pClientPlayer.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
            playermodel.crouching = pClientPlayer.isCrouching();
            HumanoidModel.ArmPose humanoidmodel$armpose = getArmPose(pClientPlayer, InteractionHand.MAIN_HAND);
            HumanoidModel.ArmPose humanoidmodel$armpose1 = getArmPose(pClientPlayer, InteractionHand.OFF_HAND);
            if (humanoidmodel$armpose.isTwoHanded()) {
                humanoidmodel$armpose1 = pClientPlayer.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            }

            if (pClientPlayer.getMainArm() == HumanoidArm.RIGHT) {
                playermodel.rightArmPose = humanoidmodel$armpose;
                playermodel.leftArmPose = humanoidmodel$armpose1;
            } else {
                playermodel.rightArmPose = humanoidmodel$armpose1;
                playermodel.leftArmPose = humanoidmodel$armpose;
            }
        }

    }

    private static HumanoidModel.ArmPose getArmPose(AbstractClientPlayer pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (itemstack.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        } else {
            if (pPlayer.getUsedItemHand() == pHand && pPlayer.getUseItemRemainingTicks() > 0) {
                UseAnim useanim = itemstack.getUseAnimation();
                if (useanim == UseAnim.BLOCK) {
                    return HumanoidModel.ArmPose.BLOCK;
                }

                if (useanim == UseAnim.BOW) {
                    return HumanoidModel.ArmPose.BOW_AND_ARROW;
                }

                if (useanim == UseAnim.SPEAR) {
                    return HumanoidModel.ArmPose.THROW_SPEAR;
                }

                if (useanim == UseAnim.CROSSBOW && pHand == pPlayer.getUsedItemHand()) {
                    return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                }

                if (useanim == UseAnim.SPYGLASS) {
                    return HumanoidModel.ArmPose.SPYGLASS;
                }

                if (useanim == UseAnim.TOOT_HORN) {
                    return HumanoidModel.ArmPose.TOOT_HORN;
                }

                if (useanim == UseAnim.BRUSH) {
                    return HumanoidModel.ArmPose.BRUSH;
                }
            } else if (!pPlayer.swinging && itemstack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemstack)) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }

            HumanoidModel.ArmPose forgeArmPose = IClientItemExtensions.of(itemstack).getArmPose(pPlayer, pHand, itemstack);
            return forgeArmPose != null ? forgeArmPose : HumanoidModel.ArmPose.ITEM;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer abstractClientPlayer) {
        return null;
    }
}