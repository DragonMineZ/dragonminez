package com.dragonminez.mixin.client;

import com.dragonminez.client.render.DMZRenderHand;
import com.dragonminez.client.render.PlayerDMZRenderer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {

    @Unique
    private static DMZRenderHand dmz$handRenderer;

    @Unique
    private void dmz$ensureRenderer() {
		if (ConfigManager.getUserConfig().getHud().isFirstPersonAnimated()) return;
        if (dmz$handRenderer == null) {
            Minecraft mc = Minecraft.getInstance();
            EntityRendererProvider.Context context = new EntityRendererProvider.Context(mc.getEntityRenderDispatcher(), mc.getItemRenderer(), mc.getBlockRenderer(), mc.getEntityRenderDispatcher().getItemInHandRenderer(), mc.getResourceManager(), mc.getEntityModels(), mc.font);
            dmz$handRenderer = new DMZRenderHand(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false));
        }
    }

    @Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true)
    private void dmz$hookRenderArm(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, float pEquippedProgress, float pSwingProgress, HumanoidArm pSide, CallbackInfo ci) {
        this.dmz$ensureRenderer();
        AbstractClientPlayer player = Minecraft.getInstance().player;

        if (player != null) {
            pPoseStack.pushPose();
            boolean isRight = pSide == HumanoidArm.RIGHT;
            float f = isRight ? 1.0F : -1.0F;

            float f1 = Mth.sqrt(pSwingProgress);
            float f2 = -0.3F * Mth.sin(f1 * (float)Math.PI);
            float f3 = 0.4F * Mth.sin(f1 * ((float)Math.PI * 2F));
            float f4 = -0.4F * Mth.sin(pSwingProgress * (float)Math.PI);
            pPoseStack.translate(f * (f2 + 0.64000005F), f3 + -0.6F + pEquippedProgress * -0.6F, f4 + -0.71999997F);
            pPoseStack.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
            float f5 = Mth.sin(pSwingProgress * pSwingProgress * (float)Math.PI);
            float f6 = Mth.sin(f1 * (float)Math.PI);
            pPoseStack.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));
            pPoseStack.translate(f * -1.0F, 3.6F, 3.5F);
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
            pPoseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
            pPoseStack.translate(f * 5.6F, 0.0F, 0.0F);

            if (isRight) {
                dmz$handRenderer.renderRightHand(pPoseStack, pBuffer, pCombinedLight, player);
            } else {
                dmz$handRenderer.renderLeftHand(pPoseStack, pBuffer, pCombinedLight, player);
            }

            pPoseStack.popPose();
            ci.cancel();
        }
    }

    @Inject(method = "renderMapHand", at = @At("HEAD"), cancellable = true)
    private void dmz$hookRenderMapHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, HumanoidArm pSide, CallbackInfo ci) {
        this.dmz$ensureRenderer();
        AbstractClientPlayer player = Minecraft.getInstance().player;

        if (player != null) {
            pPoseStack.pushPose();
            float f = pSide == HumanoidArm.RIGHT ? 1.0F : -1.0F;

            pPoseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
            pPoseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(f * -41.0F));
            pPoseStack.translate(f * 0.3F, -1.1F, 0.45F);

            if (pSide == HumanoidArm.RIGHT) {
                dmz$handRenderer.renderRightHand(pPoseStack, pBuffer, pCombinedLight, player);
            } else {
                dmz$handRenderer.renderLeftHand(pPoseStack, pBuffer, pCombinedLight, player);
            }

            pPoseStack.popPose();
            ci.cancel();
        }
    }

//    @Redirect(
//            method = { "renderPlayerArm", "renderMapHand" },
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;")
//    )
//    private <T extends Entity> EntityRenderer<? super T> dmz$useVanillaHands(EntityRenderDispatcher instance, T entity) {
//        EntityRenderer<? super T> renderer = instance.getRenderer(entity);
//
//        if (renderer instanceof PlayerDMZRenderer && entity instanceof AbstractClientPlayer player) {
//            String modelName = player.getModelName();
//
//            return (EntityRenderer<? super T>) instance.getSkinMap().get(modelName);
//        }
//
//        return renderer;
//    }

}