package com.dragonminez.mixin.client;

import com.dragonminez.client.render.DMZPlayerRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin<E extends Entity> {

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/" +
                    "minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;" +
                    "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/" +
                    "client/renderer/MultiBufferSource;I)V")
    )
    private void injectDMZRenderer(EntityRenderer<E> instance, E pEntity, float pEntityYaw, float pPartialTick,
                                   PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        if (!(pEntity instanceof AbstractClientPlayer player)) {
            instance.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBuffer, pPackedLight);
            return;
        }
        DMZPlayerRenderer.INSTANCE.render(player, pEntityYaw, pPartialTick, pPoseStack, pBuffer, pPackedLight);
    }
}
