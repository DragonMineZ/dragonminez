package com.dragonminez.common.wish;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallPackManager;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.SyncWishesS2C;
import com.dragonminez.common.util.adapters.GenericItemTypeAdapter;
import com.dragonminez.common.util.adapters.WishTypeAdapter;
import com.dragonminez.common.util.types.items.GenericItemDTO;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DragonWishRegistry extends SimpleJsonResourceReloadListener {
	public static final String ROOT_DIRECTORY = "dragonminez/dragonballs";
	public static final DragonWishRegistry INSTANCE = new DragonWishRegistry();
	@Getter
	private static Map<String, List<Wish>> serverWishes = Map.of();
	private static Map<String, List<Wish>> clientWishes = Map.of();

	private DragonWishRegistry() {
		super(
				new GsonBuilder()
						.registerTypeAdapter(Wish.class, new WishTypeAdapter())
						.registerTypeAdapter(GenericItemDTO.class, new GenericItemTypeAdapter())
						.setPrettyPrinting()
						.create(),
				ROOT_DIRECTORY);
	}

	@Override
	protected void apply(@NonNull Map<ResourceLocation, JsonElement> ignored, @NonNull ResourceManager resourceManager, @NonNull ProfilerFiller profiler) {
		DragonBallPackManager.LoadedDefinitions external = DragonBallPackManager.loadAll();
		Map<String, List<Wish>> loaded = new LinkedHashMap<>(external.wishes);
		for (var dragon : DragonBallDefinitions.getDragons()) loaded.putIfAbsent(dragon.getId(), List.of());
		serverWishes = withWishScreenAliases(loaded);
		LogUtil.info(Env.COMMON, "Loaded {} dragon wish list(s) from the dragonballs system", serverWishes.size());
	}

	public static void setServerWishes(Map<String, List<Wish>> wishes) { serverWishes = withWishScreenAliases(wishes); }
	@OnlyIn(Dist.CLIENT) public static Map<String, List<Wish>> getClientWishes() { return clientWishes; }
	@OnlyIn(Dist.CLIENT) public static void setClientWishes(Map<String, List<Wish>> wishes) { clientWishes = withWishScreenAliases(wishes); }

	/**
	 * Pack files are keyed by the dragon definition ID, while the UI and grant packet use
	 * {@code wish_screen_id}. Keep both keys available so custom dragons can reuse a screen
	 * without losing the wish list loaded from their own definition file. If multiple dragons
	 * share a screen ID, the first canonical screen entry wins deterministically.
	 */
	private static Map<String, List<Wish>> withWishScreenAliases(Map<String, List<Wish>> wishes) {
		Map<String, List<Wish>> byDragonId = new LinkedHashMap<>(wishes);
		Map<String, List<Wish>> aliased = new LinkedHashMap<>(byDragonId);
		for (DragonDefinition dragon : DragonBallDefinitions.getDragons()) {
			String wishScreenId = dragon.getWishScreenId();
			List<Wish> dragonWishes = byDragonId.get(dragon.getId());
			if (wishScreenId != null && !wishScreenId.isBlank() && dragonWishes != null) {
				aliased.putIfAbsent(wishScreenId, dragonWishes);
			}
		}
		return Map.copyOf(aliased);
	}

	@SubscribeEvent
	public static void onDatapackSync(OnDatapackSyncEvent event) {
		if (event.getPlayer() != null) {
			NetworkHandler.sendToPlayer(new SyncWishesS2C(getServerWishes()), event.getPlayer());
			return;
		}
		for (ServerPlayer player : event.getPlayerList().getPlayers()) {
			NetworkHandler.sendToPlayer(new SyncWishesS2C(getServerWishes()), player);
		}
	}
}
