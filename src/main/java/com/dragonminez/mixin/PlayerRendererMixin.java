package com.dragonminez.mixin;

import com.dragonminez.client.model.PlayerBaseModel;
import com.dragonminez.client.render.PlayerRenderModel;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashMap;
import java.util.Map;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    // 1. Guardamos el Context (igual)
    @Unique
    private static EntityRendererProvider.Context modRenderContext;

    // 2. ¡CAMBIO! Usamos tipos crudos (raw types) para el Map.
    @Unique
    @SuppressWarnings("rawtypes") // Suprimimos la advertencia de tipo crudo
    private static final Map<Integer, GeoEntityRenderer> RENDERER_MAP = new HashMap<>();

    // 3. Capturamos el Context (igual)
    @Inject(method = "<init>", at = @At("TAIL"))
    private void captureContext(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        if (modRenderContext == null) {
            modRenderContext = ctx;
        }
    }

    // 4. El Mixin de render principal
    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRender(
            AbstractClientPlayer player,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        // Obtenemos la capability (usando .ifPresent)
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {

            int raceId = data.getCharacter().getRace();

            // Si es 0 (humano), no hacemos nada
            if (raceId == 0) {
                return;
            }

            // 1. Buscamos el renderizador (¡CAMBIO!)
            @SuppressWarnings("rawtypes") // Suprimimos la advertencia
            GeoEntityRenderer morphRenderer = RENDERER_MAP.get(raceId);

            // 2. Si no existe, lo creamos (igual)
            if (morphRenderer == null) {
                if (modRenderContext == null) return;

                morphRenderer = createRendererForRace(raceId, modRenderContext);
                RENDERER_MAP.put(raceId, morphRenderer);
            }

            // 3. Si existe, lo usamos
            if (morphRenderer != null) {
                // Cancelamos el render original
                ci.cancel();

                 morphRenderer.render(
                        (AbstractClientPlayer & GeoAnimatable) player,
                        entityYaw,
                        partialTicks,
                        poseStack,
                        bufferSource,
                        packedLight
                );
            }
        });
    }

    @Unique
    @SuppressWarnings("rawtypes") // Suprimimos la advertencia
    private GeoEntityRenderer createRendererForRace(int raceId, EntityRendererProvider.Context ctx) {
        switch (raceId) {
            case 1:
                return new PlayerRenderModel(ctx, new PlayerBaseModel());
            default:
                return null;
        }
    }
}