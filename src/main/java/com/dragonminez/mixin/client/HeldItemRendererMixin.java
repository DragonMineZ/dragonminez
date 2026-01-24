package com.dragonminez.mixin.client;

import com.dragonminez.client.render.PlayerDMZRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {

    @Shadow
    private Minecraft minecraft;

    @Shadow
    private EntityRenderDispatcher entityRenderDispatcher;


    @Redirect(method = "renderMapHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;"))
    private <T extends Entity> EntityRenderer<? super T> dmz$redirectMapRenderer(EntityRenderDispatcher instance, T entity) {
        EntityRenderer<? super T> renderer = instance.getRenderer(entity);
        if (renderer instanceof PlayerDMZRenderer) return null;
        return renderer;
    }

    @Inject(method = "renderMapHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"), cancellable = true)
    private void dmz$renderRightMapHand(PoseStack ps, MultiBufferSource ms, int light, HumanoidArm arm, CallbackInfo ci) {
        if (this.minecraft.player != null && entityRenderDispatcher.getRenderer(this.minecraft.player) instanceof PlayerDMZRenderer dmz) {
            dmz.renderHand(ps, ms, light, (AbstractClientPlayer & GeoAnimatable) this.minecraft.player, HumanoidArm.RIGHT);
            ci.cancel();
        }
    }

    @Inject(method = "renderMapHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"), cancellable = true)
    private void dmz$renderLeftMapHand(PoseStack ps, MultiBufferSource ms, int light, HumanoidArm arm, CallbackInfo ci) {
        if (this.minecraft.player != null && entityRenderDispatcher.getRenderer(this.minecraft.player) instanceof PlayerDMZRenderer dmz) {
            dmz.renderHand(ps, ms, light, (AbstractClientPlayer & GeoAnimatable) this.minecraft.player, HumanoidArm.LEFT);
            ci.cancel();
        }
    }

    @Redirect(method = "renderPlayerArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;"))
    private <T extends Entity> EntityRenderer<? super T> dmz$redirectArmRenderer(EntityRenderDispatcher instance, T entity) {
        EntityRenderer<? super T> renderer = instance.getRenderer(entity);
        if (renderer instanceof PlayerDMZRenderer) return null;
        return renderer;
    }

    @Inject(method = "renderPlayerArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"), cancellable = true)
    private void dmz$renderRightPlayerArm(PoseStack ps, MultiBufferSource ms, int light, float ep, float sp, HumanoidArm arm, CallbackInfo ci) {
        if (this.minecraft.player != null && entityRenderDispatcher.getRenderer(this.minecraft.player) instanceof PlayerDMZRenderer dmz) {
            dmz.renderHand(ps, ms, light, (AbstractClientPlayer & GeoAnimatable) this.minecraft.player, HumanoidArm.RIGHT);
            ci.cancel();
        }
    }

    @Inject(method = "renderPlayerArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"), cancellable = true)
    private void dmz$renderLeftPlayerArm(PoseStack ps, MultiBufferSource ms, int light, float ep, float sp, HumanoidArm arm, CallbackInfo ci) {
        if (this.minecraft.player != null && entityRenderDispatcher.getRenderer(this.minecraft.player) instanceof PlayerDMZRenderer dmz) {
            dmz.renderHand(ps, ms, light, (AbstractClientPlayer & GeoAnimatable) this.minecraft.player, HumanoidArm.LEFT);
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