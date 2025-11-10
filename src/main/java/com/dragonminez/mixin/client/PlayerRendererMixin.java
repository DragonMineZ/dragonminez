package com.dragonminez.mixin.client;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.model.PlayerBaseModel;
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
    private static EntityRendererProvider.Context dmzctx;

    @Unique
    @SuppressWarnings("rawtypes")
    private static final Map<Integer, GeoEntityRenderer> DMZ_RENDERER = new HashMap<>();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void captureContext(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        if (dmzctx == null) {
            dmzctx = ctx;
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
            boolean isSlim = player.getModelName().equals("slim");
            int bodyType = data.getCharacter().getBodyType();

            String rendererKey = raceId + "_" + gender;
            int rendererId = rendererKey.hashCode();

            @SuppressWarnings("rawtypes")
            GeoEntityRenderer morphRenderer = DMZ_RENDERER.get(rendererId);

            if (morphRenderer == null) {
                if (dmzctx == null) return;

                morphRenderer = createRendererForRace(raceId, gender, bodyType, isSlim, dmzctx);
                DMZ_RENDERER.put(rendererId, morphRenderer);
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
    private GeoEntityRenderer createRendererForRace(int raceId, String gender, int bodyType, boolean isSlim, EntityRendererProvider.Context ctx) {

        LogUtil.info(Env.COMMON, "Raza: " + raceId + ", BodyType: " + bodyType + ", isSlim: " + isSlim + ". Creando renderer.");

        if (bodyType == 0 && (raceId == Character.RACE_HUMAN || raceId == Character.RACE_SAIYAN)) {
            if (isSlim) {
                return new PlayerRenderModel(ctx, new PlayerBaseModel());
            } else {
                return new PlayerRenderModel(ctx, new PlayerBaseModel());
            }
        }

        boolean isFemale = Character.GENDER_FEMALE.equals(gender);

        switch (raceId) {
            case Character.RACE_HUMAN:
            case Character.RACE_SAIYAN:
            case Character.RACE_MAJIN:
                if (isFemale) {
                    return new PlayerRenderModel(ctx, new PlayerBaseModel());
                } else {
                    return new PlayerRenderModel(ctx, new PlayerBaseModel());
                }
            case Character.RACE_NAMEKIAN:
            case Character.RACE_COLD_DEMON:
            case Character.RACE_BIO_ANDROID:
                return new PlayerRenderModel(ctx, new PlayerBaseModel());
            default:
                return null;
        }
    }
}

