package com.dragonminez.mixin.common;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.model.PlayerMaleModel;
import com.dragonminez.client.model.PlayerFemaleModel;
import com.dragonminez.client.render.PlayerRenderModel;
import com.dragonminez.common.stats.Character;
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

    @Unique
    private static EntityRendererProvider.Context modRenderContext;

    @Unique
    @SuppressWarnings("rawtypes")
    private static final Map<Integer, GeoEntityRenderer> RENDERER_MAP = new HashMap<>();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void captureContext(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        if (modRenderContext == null) {
            modRenderContext = ctx;
        }
    }

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
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {

            int raceId = data.getCharacter().getRace();

            String gender = data.getCharacter().getGender();
            boolean isFemale = Character.GENDER_FEMALE.equals(gender);

            String rendererKey = raceId + "_" + gender;
            int rendererId = rendererKey.hashCode();

            @SuppressWarnings("rawtypes")
            GeoEntityRenderer morphRenderer = RENDERER_MAP.get(rendererId);

            if (morphRenderer == null) {
                if (modRenderContext == null) return;

                morphRenderer = createRendererForRace(raceId, isFemale, modRenderContext);
                RENDERER_MAP.put(rendererId, morphRenderer);
            }

            if (morphRenderer != null) {
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
    @SuppressWarnings("rawtypes")
    private GeoEntityRenderer createRendererForRace(int raceId, boolean isFemale, EntityRendererProvider.Context ctx) {
        LogUtil.info(Env.COMMON, "Raza: " + raceId + ", Femenino: " + isFemale + ". Creando renderer personalizado.");
        switch (raceId) {
            case Character.RACE_HUMAN:
                if (isFemale) {
                    return new PlayerRenderModel(ctx, new PlayerFemaleModel());
                } else {
                    return new PlayerRenderModel(ctx, new PlayerMaleModel());
                }
            case Character.RACE_SAIYAN:
                if (isFemale) {
                    return new PlayerRenderModel(ctx, new PlayerFemaleModel());
                } else {
                    return new PlayerRenderModel(ctx, new PlayerMaleModel());
                }
            case Character.RACE_NAMEKIAN:
                return new PlayerRenderModel(ctx, new PlayerMaleModel());
            case Character.RACE_COLD_DEMON:
                return new PlayerRenderModel(ctx, new PlayerMaleModel());
            case Character.RACE_BIO_ANDROID:
                return new PlayerRenderModel(ctx, new PlayerMaleModel());
            case Character.RACE_MAJIN:
                if (isFemale) {
                    return new PlayerRenderModel(ctx, new PlayerFemaleModel());
                } else {
                    return new PlayerRenderModel(ctx, new PlayerMaleModel());
                }
            default:
                return null;
        }
    }
}