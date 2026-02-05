package com.dragonminez.mixin.client;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.model.DMZPlayerModel;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.firstperson.DMZPOVPlayerRenderer;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
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
    // Cache: third-person renderers
    @Unique
    private final Map<Integer, GeoEntityRenderer<?>> dragonminez$dmzRenderers = new HashMap<>();

    // Cache: first-person POV renderers (hide head + offset)
    @Unique
    private final Map<Integer, GeoEntityRenderer<?>> dragonminez$dmzPOVRenderers = new HashMap<>();

    // Render context (needed to create renderers)
    @Unique
    private EntityRendererProvider.Context dragonminez$dmzContext;

    @Inject(method = "getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("unchecked")
    public <E extends Entity> void dmz$getRenderer(E entity, CallbackInfoReturnable<EntityRenderer<? super E>> cir) {
        if (!(entity instanceof AbstractClientPlayer player)) return;

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (dragonminez$dmzContext == null) return;

            var character = data.getCharacter();
            String race = character.getRaceName().toLowerCase();
            String gender = character.getGender().toLowerCase();
            String form = character.getActiveForm();

            String baseKey = race + "_" + gender + "_" + (form != null ? form : "base");

			boolean pov = FirstPersonManager.shouldRenderFirstPerson(player);


            // Make keys distinct so both can exist in cache for same morph
            int rendererId = (baseKey + (pov ? "_pov" : "_tp")).hashCode();

            Map<Integer, GeoEntityRenderer<?>> cache = pov ? dragonminez$dmzPOVRenderers : dragonminez$dmzRenderers;
            GeoEntityRenderer<?> renderer = cache.get(rendererId);

            if (renderer == null) {
                renderer = dmz$createRendererForRace(race, gender, form, dragonminez$dmzContext, pov);
                cache.put(rendererId, renderer);
            }

            cir.setReturnValue((EntityRenderer<? super E>) renderer);
        });
    }

	@Unique
	private boolean isCalledFromInventory() {
		for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
			if (element.getClassName().contains("InventoryScreen") && (element.getMethodName().equals("renderEntityInInventory") || element.getMethodName().equals("m_280047_"))) {
				return true;
			}
		}
		return false;
	}

    @Inject(method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void dmz$onResourceManagerReload(ResourceManager resourceManager, CallbackInfo ci, EntityRendererProvider.Context context) {
        this.dragonminez$dmzContext = context;
        this.dragonminez$dmzRenderers.clear();
        this.dragonminez$dmzPOVRenderers.clear();
        LogUtil.info(Env.CLIENT, "DMZ Renderers Cache cleared on Resource Reload");
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private GeoEntityRenderer<?> dmz$createRendererForRace(String race, String gender, String form, EntityRendererProvider.Context ctx, boolean pov) {
        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(race);
        String customModel = (raceConfig != null) ? raceConfig.getCustomModel() : "";

        try {
            DMZPlayerModel model = new DMZPlayerModel<>(race, customModel);
            if (pov) return new DMZPOVPlayerRenderer(ctx, model);
            return new DMZPlayerRenderer(ctx, model);
        } catch (Exception e) {
            LogUtil.error(Env.CLIENT, "Error creando renderizador para: " + race + " (pov=" + pov + ")");
            DMZPlayerModel fallbackModel = new DMZPlayerModel<>("human", "");
            return pov ? new DMZPOVPlayerRenderer(ctx, fallbackModel) : new DMZPlayerRenderer(ctx, fallbackModel);
        }
    }
}