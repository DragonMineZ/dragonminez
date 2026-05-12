package com.dragonminez.common.wish;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallPackManager;
import com.google.gson.Gson;
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

	private DragonWishRegistry() { super(new Gson(), ROOT_DIRECTORY); }

	@Override
	protected void apply(@NonNull Map<ResourceLocation, JsonElement> ignored, @NonNull ResourceManager resourceManager, @NonNull ProfilerFiller profiler) {
		DragonBallPackManager.LoadedDefinitions external = DragonBallPackManager.loadAll();
		Map<String, List<Wish>> loaded = new LinkedHashMap<>(external.wishes);
		for (var dragon : DragonBallDefinitions.getDragons()) loaded.putIfAbsent(dragon.getId(), List.of());
		serverWishes = Map.copyOf(loaded);
		LogUtil.info(Env.COMMON, "Loaded {} dragon wish list(s) from the dragonballs system", serverWishes.size());
	}

	public static void setServerWishes(Map<String, List<Wish>> wishes) { serverWishes = Map.copyOf(wishes); }
	@OnlyIn(Dist.CLIENT) public static Map<String, List<Wish>> getClientWishes() { return clientWishes; }
	@OnlyIn(Dist.CLIENT) public static void setClientWishes(Map<String, List<Wish>> wishes) { clientWishes = Map.copyOf(wishes); }

	@SubscribeEvent
	public static void onDatapackSync(OnDatapackSyncEvent event) {
		if (event.getPlayer() != null) {
			com.dragonminez.common.network.NetworkHandler.sendToPlayer(new com.dragonminez.common.network.S2C.SyncWishesS2C(getServerWishes()), event.getPlayer());
			return;
		}
		for (ServerPlayer player : event.getPlayerList().getPlayers()) {
			com.dragonminez.common.network.NetworkHandler.sendToPlayer(new com.dragonminez.common.network.S2C.SyncWishesS2C(getServerWishes()), player);
		}
	}
}
