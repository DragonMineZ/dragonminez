package com.dragonminez.client.render;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.model.DMZPlayerModel;
import com.dragonminez.client.model.KiWeaponModelLoader;
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
import java.util.UUID;

public final class DMZRendererCache {
	private DMZRendererCache() {}
	private static final Map<UUID, CachedRendererEntry> TP_RENDERERS = new HashMap<>();
	private static final Map<UUID, CachedRendererEntry> POV_RENDERERS = new HashMap<>();

	@Nullable
	private static EntityRendererProvider.Context context;

	public static void onResourceReload(EntityRendererProvider.Context ctx) {
		context = ctx;
		clear();
		KiWeaponModelLoader.clear();
		LogUtil.info(Env.CLIENT, "DMZRendererCache cleared on resource reload");
	}

	public static void clear() {
		TP_RENDERERS.clear();
		POV_RENDERERS.clear();
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
			String stateSignature = buildStateSignature(race, gender, form);

			Map<UUID, CachedRendererEntry> cache = pov ? POV_RENDERERS : TP_RENDERERS;
			CachedRendererEntry entry = cache.get(acp.getUUID());
			GeoEntityRenderer<?> renderer = (entry != null && entry.matches(stateSignature)) ? entry.renderer() : null;

			if (renderer == null) {
				renderer = createRenderer(race, gender, form, pov);
				cache.put(acp.getUUID(), new CachedRendererEntry(stateSignature, renderer));
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

			String stateSignature = buildStateSignature(race, gender, form);

			CachedRendererEntry entry = TP_RENDERERS.get(acp.getUUID());
			GeoEntityRenderer<?> renderer = (entry != null && entry.matches(stateSignature)) ? entry.renderer() : null;
			if (renderer == null) {
				renderer = createRenderer(race, gender, form, false);
				TP_RENDERERS.put(acp.getUUID(), new CachedRendererEntry(stateSignature, renderer));
			}

			return (renderer instanceof DMZPlayerRenderer<?> dmz) ? dmz : null;
		}).orElse(null);
	}

	@Nullable
	public static EntityRendererProvider.Context getContext() {
		return context;
	}

	private static String buildStateSignature(String race, String gender, String form) {
		String safeRace = race != null ? race.toLowerCase() : "human";
		String safeGender = gender != null ? gender.toLowerCase() : "male";
		String safeForm = form != null ? form.toLowerCase() : "base";
		return safeRace + "|" + safeGender + "|" + safeForm;
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

	private record CachedRendererEntry(String stateSignature, GeoEntityRenderer<?> renderer) {
		private boolean matches(String otherSignature) {
			return stateSignature.equals(otherSignature);
		}
	}
}