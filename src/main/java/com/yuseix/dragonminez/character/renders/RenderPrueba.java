package com.yuseix.dragonminez.character.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuseix.dragonminez.character.models.ModeloPrueba;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;


@OnlyIn(Dist.CLIENT)
public class RenderPrueba extends LivingEntityRenderer<AbstractClientPlayer, ModeloPrueba<AbstractClientPlayer>> {


    public RenderPrueba(EntityRendererProvider.Context pContext) {

        super(pContext, new ModeloPrueba<>(pContext.bakeLayer(ModeloPrueba.LAYER_LOCATION)), 0.5f);
        this.addLayer(new PlayerItemInHandLayer(this, pContext.getItemInHandRenderer()));
        this.addLayer(new ArrowLayer(pContext, this));
        this.addLayer(new ElytraLayer(this, pContext.getModelSet()));
        this.addLayer(new ParrotOnShoulderLayer(this, pContext.getModelSet()));
        this.addLayer(new SpinAttackEffectLayer(this, pContext.getModelSet()));
        this.addLayer(new BeeStingerLayer(this));
    }




    @Override
    public void render(AbstractClientPlayer pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        this.setModelProperties(pEntity);

        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    private void setModelProperties(AbstractClientPlayer pClientPlayer) {
        PlayerModel<AbstractClientPlayer> playermodel = this.getModel();
        if (pClientPlayer.isSpectator()) {
            playermodel.setAllVisible(false);
            playermodel.head.visible = true;
            playermodel.hat.visible = false;
        } else {
            playermodel.setAllVisible(true);
            playermodel.hat.visible = false;
            playermodel.jacket.visible = false;
            playermodel.leftPants.visible = false;
            playermodel.rightPants.visible = false;
            playermodel.leftSleeve.visible = false;
            playermodel.rightSleeve.visible = false;
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
        return abstractClientPlayer.getSkinTextureLocation();
    }

    @Override
    protected boolean shouldShowName(AbstractClientPlayer player) {
        return !player.isCrouching() && super.shouldShowName(player);
    }

    @Override
    protected void scale(AbstractClientPlayer player, PoseStack stack, float partialTickTime) {
        stack.scale(1f, 1f, 1f);
    }
    @Override
    protected void renderNameTag(AbstractClientPlayer player, Component name, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        double d0 = this.entityRenderDispatcher.distanceToSqr(player);
        matrixStack.pushPose();
        if (d0 < 100.0D) {
            Scoreboard scoreboard = player.getScoreboard();
            Objective objective = scoreboard.getDisplayObjective(2);
            if (objective != null) {
                Score score = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
                super.renderNameTag(player, Component.literal(Integer.toString(score.getScore())).append(" ").append(objective.getDisplayName()), matrixStack, buffer, packedLight);
                matrixStack.translate(0.0D, 9.0F * 1.15F * 0.025F, 0.0D);
            }
        }

        super.renderNameTag(player, name, matrixStack, buffer, packedLight);
        matrixStack.popPose();
    }

    @Override
    protected void setupRotations(AbstractClientPlayer player, PoseStack matrixStack, float ageInTicks, float rotationYaw, float partialTicks) {
        float f = player.getSwimAmount(partialTicks);
        if (player.isFallFlying()) {
            super.setupRotations(player, matrixStack, ageInTicks, rotationYaw, partialTicks);
            float f1 = player.getFallFlyingTicks() + partialTicks;
            float f2 = Mth.clamp(f1 * f1 / 100.0F, 0.0F, 1.0F);
            if (!player.isAutoSpinAttack()) {
                matrixStack.mulPose(Axis.XP.rotationDegrees(f2 * (-90.0F - player.getXRot())));
            }

            Vec3 vector3d = player.getViewVector(partialTicks);
            Vec3 vector3d1 = player.getDeltaMovement();
            double d0 = vector3d1.horizontalDistanceSqr();
            double d1 = vector3d.horizontalDistanceSqr();
            if (d0 > 0.0D && d1 > 0.0D) {
                double d2 = (vector3d1.x * vector3d.x + vector3d1.z * vector3d.z) / Math.sqrt(d0 * d1);
                double d3 = vector3d1.x * vector3d.z - vector3d1.z * vector3d.x;
                matrixStack.mulPose(Axis.YP.rotation((float) (Math.signum(d3) * Math.acos(d2))));
            }
        } else if (f > 0.0F) {
            super.setupRotations(player, matrixStack, ageInTicks, rotationYaw, partialTicks);
            float f3 = player.isInWater() ? -90.0F - player.getXRot() : -90.0F;
            float f4 = Mth.lerp(f, 0.0F, f3);
            matrixStack.mulPose(Axis.XP.rotationDegrees(f4));
            if (player.isVisuallySwimming()) {
                matrixStack.translate(0.0D, -0.25, 0.25);
            }
        } else {
            super.setupRotations(player, matrixStack, ageInTicks, rotationYaw, partialTicks);
        }


    }
}
