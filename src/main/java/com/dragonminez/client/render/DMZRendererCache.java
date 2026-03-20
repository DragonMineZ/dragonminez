package com.dragonminez.client.render;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.model.DMZPlayerModel;
import com.dragonminez.client.render.firstperson.DMZPOVPlayerRenderer;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DMZRendererCache {
	private DMZRendererCache() {}
	private static final Map<Integer, GeoEntityRenderer<?>> TP_RENDERERS = new HashMap<>();
	private static final Map<Integer, GeoEntityRenderer<?>> POV_RENDERERS = new HashMap<>();

	@Nullable
	private static EntityRendererProvider.Context context;

	public static void onResourceReload(EntityRendererProvider.Context ctx) {
		context = ctx;
		TP_RENDERERS.clear();
		POV_RENDERERS.clear();
		LogUtil.info(Env.CLIENT, "DMZRendererCache cleared on resource reload");
	}

	@Nullable
	public static DMZPlayerRenderer<?> getRenderer(Player player) {
		if (context == null || !(player instanceof AbstractClientPlayer acp)) return null;

		return StatsProvider.get(StatsCapability.INSTANCE, acp).map(data -> {
			var character = data.getCharacter();
			String race = character.getRaceName();
			String gender = character.getGender();
			String form = character.getActiveForm();

			boolean pov = FirstPersonManager.shouldRenderFirstPerson(player);
			int rendererId = Objects.hash(race, gender, form, pov);

			Map<Integer, GeoEntityRenderer<?>> cache = pov ? POV_RENDERERS : TP_RENDERERS;
			GeoEntityRenderer<?> renderer = cache.get(rendererId);

			if (renderer == null) {
				renderer = createRenderer(race, gender, form, pov);
				cache.put(rendererId, renderer);
			}

			return (renderer instanceof DMZPlayerRenderer<?> dmz) ? dmz : null;
		}).orElse(null);
	}

	@Nullable
	public static DMZPlayerRenderer<?> getTPRenderer(Player player) {
		if (context == null || !(player instanceof AbstractClientPlayer acp)) return null;

		return StatsProvider.get(StatsCapability.INSTANCE, acp).map(data -> {
			var character = data.getCharacter();
			String race = character.getRaceName();
			String gender = character.getGender();
			String form = character.getActiveForm();

			int rendererId = Objects.hash(race, gender, form, false);

			GeoEntityRenderer<?> renderer = TP_RENDERERS.get(rendererId);
			if (renderer == null) {
				renderer = createRenderer(race, gender, form, false);
				TP_RENDERERS.put(rendererId, renderer);
			}

			return (renderer instanceof DMZPlayerRenderer<?> dmz) ? dmz : null;
		}).orElse(null);
	}

	@Nullable
	public static EntityRendererProvider.Context getContext() {
		return context;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static GeoEntityRenderer<?> createRenderer(String race, String gender, String form, boolean pov) {
		String safeRace = race != null ? race.toLowerCase() : "human";

		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(safeRace);
		String customModel = (raceConfig != null) ? raceConfig.getCustomModel() : "";

		try {
			DMZPlayerModel model = new DMZPlayerModel<>(safeRace, customModel);
			if (pov) return new DMZPOVPlayerRenderer(context, model);
			return new DMZPlayerRenderer(context, model);
		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "Error creating renderer for: {} (pov={})", safeRace, pov);
			DMZPlayerModel fallbackModel = new DMZPlayerModel<>("human", "");
			return pov ? new DMZPOVPlayerRenderer(context, fallbackModel) : new DMZPlayerRenderer(context, fallbackModel);
		}
	}
}