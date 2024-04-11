package com.yuseix.dragonminez.mixins;

import com.google.common.collect.ImmutableMap;
import com.yuseix.dragonminez.character.renders.RenderPrueba;
import com.yuseix.dragonminez.events.ModEvents;
import com.yuseix.dragonminez.stats.PlayerStatsAttrProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    private Map<String, RenderPrueba> dragonRenderers = ImmutableMap.of();

    @Shadow
    public Map<EntityType<?>, EntityRenderer<?>> renderers;
    @Shadow
    private Map<String, EntityRenderer<? extends Player>> playerRenderers;

    @Inject(at = @At("HEAD"), method = "getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;", cancellable = true)
    public void drgnAscnt$getRenderer(Entity entity, CallbackInfoReturnable<EntityRenderer<? super Entity>> cir) {
        if(entity instanceof Player player) {
            Minecraft mc = Minecraft.getInstance();
            PlayerStatsAttrProvider.getCap(ModEvents.INSTANCE, player).ifPresent(cap -> {
                int bodyType = cap.getBodytype();
                if(bodyType == 1){
                    //cir.setReturnValue(new RenderPrueba(new EntityRendererProvider.Context(mc.getEntityRenderDispatcher(),mc.getItemRenderer(),mc.getBlockRenderer(),mc.getEntityRenderDispatcher().getItemInHandRenderer(), mc.getResourceManager(),mc.getEntityModels(),mc.font));
                }

            });
        }

    }


}
