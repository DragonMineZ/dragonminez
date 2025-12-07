package com.dragonminez.mixin.client;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.model.PlayerBaseModel;
import com.dragonminez.client.model.PlayerFemaleModel;
import com.dragonminez.client.render.PlayerRenderModel;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.Character;
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
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashMap;
import java.util.Map;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Unique
    private static EntityRendererProvider.Context dragonminez_context;

    @Unique
    @SuppressWarnings("rawtypes")
    private static final Map<Integer, GeoEntityRenderer> dragonminez_renderers = new HashMap<>();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        if (dragonminez_context == null) {
            dragonminez_context = ctx;
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

            String rendererKey = raceId + "_" + gender;
            int rendererId = rendererKey.hashCode();

            @SuppressWarnings("rawtypes")
            GeoEntityRenderer morphRenderer = dragonminez_renderers.get(rendererId);

            if (morphRenderer == null) {
                if (dragonminez_context == null) return;

                morphRenderer = dragonminez_createRendererForRace(raceId, gender, dragonminez_context);
                dragonminez_renderers.put(rendererId, morphRenderer);
            }

            ci.cancel();
            morphRenderer.render(player, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        });
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private GeoEntityRenderer dragonminez_createRendererForRace(int raceId, String gender, EntityRendererProvider.Context ctx) {
        String raceName = dragonminez_getRaceNameById(raceId);
        RaceCharacterConfig raceConfig = com.dragonminez.common.config.ConfigManager.getRaceCharacter(raceName);
        String customModel = raceConfig.getCustomModel();

        LogUtil.info(Env.CLIENT, "Creating renderer for race: " + raceName + ", CustomModel: " + (customModel != null ? customModel : "none"));

        try {
            if (Character.GENDER_FEMALE.equals(gender) && (customModel == null || customModel.isEmpty())) {
                 return new PlayerRenderModel(ctx, new PlayerFemaleModel(raceName, customModel));
            }
            PlayerBaseModel model = new PlayerBaseModel(raceName, customModel);
            return new PlayerRenderModel(ctx, model);
        } catch (Exception e) {
            LogUtil.error(Env.CLIENT, "Failed to create renderer for race " + raceName + ". Using default. Error: " + e.getMessage());
            return new PlayerRenderModel(ctx, new PlayerBaseModel());
        }
    }

    @Unique
    private String dragonminez_getRaceNameById(int raceId) {
        java.util.List<String> raceNames = com.dragonminez.common.config.ConfigManager.getLoadedRaces();
        if (raceId >= 0 && raceId < raceNames.size()) {
            return raceNames.get(raceId);
        }
        return "human";
    }
}
