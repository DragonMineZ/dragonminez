package com.dragonminez.mixin.client;

import com.dragonminez.client.render.PlayerDMZRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {

    @Redirect(
            method = { "renderPlayerArm", "renderMapHand" },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;")
    )
    private <T extends Entity> EntityRenderer<? super T> dmz$useVanillaHands(EntityRenderDispatcher instance, T entity) {
        EntityRenderer<? super T> renderer = instance.getRenderer(entity);

        if (renderer instanceof PlayerDMZRenderer && entity instanceof AbstractClientPlayer player) {
            String modelName = player.getModelName();

            return (EntityRenderer<? super T>) instance.getSkinMap().get(modelName);
        }

        return renderer;
    }
}