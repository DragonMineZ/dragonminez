package com.dragonminez.mixin.client;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.model.PlayerDMZModel;
import com.dragonminez.client.render.PlayerDMZRenderer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.Character;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashMap;
import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Unique
    private final Map<Integer, GeoEntityRenderer<?>> dmzRenderers = new HashMap<>();

    @Unique
    private EntityRendererProvider.Context dmzContext;

    @Inject(
            method = "getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
            at = @At("HEAD"),
            cancellable = true
    )
    public <E extends Entity> void dmz$getRenderer(E entity, CallbackInfoReturnable<EntityRenderer<? super E>> cir) {

        if (entity instanceof AbstractClientPlayer player) {

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                String race = data.getCharacter().getRace();
                String gender = data.getCharacter().getGender();
                String formGroup = data.getCharacter().getActiveFormGroup();
                String form = data.getCharacter().getActiveForm();

                String rendererKey = race + "_" + gender;
                if (formGroup != null && !formGroup.isEmpty() && form != null && !form.isEmpty()) {
                    rendererKey += "_" + formGroup + "_" + form;
                }
                int rendererId = rendererKey.hashCode();

                GeoEntityRenderer<?> morphRenderer = dmzRenderers.get(rendererId);

                if (morphRenderer == null) {
                    if (dmzContext == null) return;

                    morphRenderer = dmz$createRendererForRace(race, gender, formGroup, form, dmzContext);
                    dmzRenderers.put(rendererId, morphRenderer);
                }

                cir.setReturnValue((EntityRenderer<? super E>) morphRenderer);
            });
        }
    }

    @Inject(
            method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V",
            at = @At("TAIL"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void dmz$onResourceManagerReload(ResourceManager resourceManager, CallbackInfo ci, EntityRendererProvider.Context context) {
        this.dmzContext = context;
        this.dmzRenderers.clear();
        LogUtil.info(Env.CLIENT, "DMZ Renderers Cache cleared on Resource Reload");
    }

    @Unique
    private GeoEntityRenderer<?> dmz$createRendererForRace(String race, String gender, String formGroup, String form, EntityRendererProvider.Context ctx) {
        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(race);
        String customModel = raceConfig.getCustomModel();

        if (formGroup != null && !formGroup.isEmpty() && form != null && !form.isEmpty()) {
            var formData = ConfigManager.getForm(race, formGroup, form);
            if (formData != null && formData.hasCustomModel()) {
                customModel = formData.getCustomModel();
            }
        }

        try {
            if (Character.GENDER_FEMALE.equals(gender) && (customModel == null || customModel.isEmpty())) {
                return new PlayerDMZRenderer<>(ctx, new PlayerDMZModel<>(race, customModel));
            }
            PlayerDMZModel model = new PlayerDMZModel<>(race, customModel);
            return new PlayerDMZRenderer<>(ctx, model);
        } catch (Exception e) {
            LogUtil.error(Env.CLIENT, "Failed to create renderer for race " + race + ". Using default.");
            return new PlayerDMZRenderer<>(ctx, new PlayerDMZModel<>());
        }
    }
}
