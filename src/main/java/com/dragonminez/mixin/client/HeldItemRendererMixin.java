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

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {

    /**
     * Redirigimos la obtención del renderizador cuando Minecraft intenta dibujar los brazos.
     * Si el sistema detecta tu PlayerDMZRenderer, lo cambiaremos por el PlayerRenderer original
     * de Minecraft solo para este proceso.
     */
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