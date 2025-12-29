package com.dragonminez.mixin.client;

import com.dragonminez.client.render.PlayerDMZRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Redirect(method = "renderMapHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/HumanoidArm;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;"))
    private <T extends Entity> EntityRenderer<? super T> injected(EntityRenderDispatcher instance, T entityrenderer) {
        return null;
    }

//    @Inject(method = "renderMapHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/HumanoidArm;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"), cancellable = true)
//    private void dmz$Right$renderMapHand(PoseStack poseStack, MultiBufferSource buffer, int light, HumanoidArm arm, CallbackInfo ci) {
//        renderCustomHand(poseStack, buffer, light, arm, ci);
//    }
//
//    @Inject(method = "renderMapHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/HumanoidArm;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"), cancellable = true)
//    private void dmz$Left$renderMapHand(PoseStack poseStack, MultiBufferSource buffer, int light, HumanoidArm arm, CallbackInfo ci) {
//        renderCustomHand(poseStack, buffer, light, arm, ci);
//    }

    @Redirect(method = "renderMapHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"))
    private void redirectRightHandMap(PlayerRenderer instance, PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player) {
        this.renderCustomHandDirect(poseStack, buffer, light, HumanoidArm.RIGHT, player);
    }

    @Redirect(method = "renderMapHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"))
    private void redirectLeftHandMap(PlayerRenderer instance, PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player) {
        this.renderCustomHandDirect(poseStack, buffer, light, HumanoidArm.LEFT, player);
    }

    @Redirect(method = "renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IFFLnet/minecraft/world/entity/HumanoidArm;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;"))
    private <T extends Entity> EntityRenderer<? super T> dmz$renderPlayerArm(EntityRenderDispatcher instance, T entityrenderer) {
        return null;
    }

    @Inject(method = "renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IFFLnet/minecraft/world/entity/HumanoidArm;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"), cancellable = true)
    private void dmz$Right$renderPlayerArm(PoseStack poseStack, MultiBufferSource buffer, int light, float equippedProgress, float swingProgress, HumanoidArm arm, CallbackInfo ci) {
        renderCustomHand(poseStack, buffer, light, arm, ci);
    }

    @Inject(method = "renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IFFLnet/minecraft/world/entity/HumanoidArm;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"), cancellable = true)
    private void dmz$Left$renderPlayerArm(PoseStack poseStack, MultiBufferSource buffer, int light, float equippedProgress, float swingProgress, HumanoidArm arm, CallbackInfo ci) {
        renderCustomHand(poseStack, buffer, light, arm, ci);
    }

    private void renderCustomHandDirect(PoseStack poseStack, MultiBufferSource buffer, int light, HumanoidArm arm, AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();

        EntityRenderer<? super AbstractClientPlayer> r = mc.getEntityRenderDispatcher().getRenderer(player);

        if (r instanceof PlayerDMZRenderer dmzRenderer) {
            if (arm == HumanoidArm.RIGHT) {
                dmzRenderer.renderRightHand(poseStack, buffer, light, player);
            } else {
                dmzRenderer.renderLeftHand(poseStack, buffer, light, player);
            }
        } else {
            // FALLBACK: Si por alguna razón no es tu renderer, intentamos llamar al vanilla
            // (aunque esto requeriría acceso a la instancia original del PlayerRenderer,
            // normalmente con el cast arriba basta).
        }
    }

    private void renderCustomHand(PoseStack poseStack, MultiBufferSource buffer, int light, HumanoidArm arm, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;

        EntityRenderer<? super AbstractClientPlayer> r =
                mc.getEntityRenderDispatcher().getRenderer(player);

        if (!(r instanceof PlayerDMZRenderer dmzRenderer)) {
            ci.cancel();
            return;
        }

        if (arm == HumanoidArm.RIGHT) {
            dmzRenderer.renderRightHand(poseStack, buffer, light, player);
        } else {
            dmzRenderer.renderLeftHand(poseStack, buffer, light, player);
        }

        ci.cancel();
    }

}