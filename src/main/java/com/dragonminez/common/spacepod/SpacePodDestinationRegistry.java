package com.dragonminez.common.spacepod;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.SyncSpacePodDestinationsS2C;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpacePodDestinationRegistry extends SimpleJsonResourceReloadListener {

	public static final String DIRECTORY = "dragonminez/spacepod_destinations";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final SpacePodDestinationRegistry INSTANCE = new SpacePodDestinationRegistry();

	private static List<SpacePodDestinationDefinition> serverDestinations = List.of();
	private static List<SpacePodDestinationDefinition> clientDestinations = List.of();

	private SpacePodDestinationRegistry() {
		super(GSON, DIRECTORY);
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
		List<Map.Entry<ResourceLocation, JsonElement>> entries = new ArrayList<>(map.entrySet());
		entries.sort(Comparator.comparing(entry -> entry.getKey().toString()));

		List<SpacePodDestinationDefinition> loaded = new ArrayList<>();
		for (Map.Entry<ResourceLocation, JsonElement> entry : entries) {
			JsonObject root = GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey().toString());
			boolean replace = GsonHelper.getAsBoolean(root, "replace", false);
			if (replace) {
				loaded.clear();
			}

			JsonArray destinations = GsonHelper.getAsJsonArray(root, "destinations");
			for (JsonElement element : destinations) {
				if (!element.isJsonObject()) {
					throw new IllegalArgumentException("Destination entry in '" + entry.getKey() + "' must be an object");
				}
				loaded.add(SpacePodDestinationDefinition.fromJson(element.getAsJsonObject()));
			}
		}

		serverDestinations = List.copyOf(loaded);
		LogUtil.info(Env.COMMON, "Loaded {} space pod destination(s) from datapacks", serverDestinations.size());
	}

	public static List<SpacePodDestinationDefinition> getServerDestinations() {
		return serverDestinations;
	}

	@OnlyIn(Dist.CLIENT)
	public static List<SpacePodDestinationDefinition> getClientDestinations() {
		return clientDestinations;
	}

	@OnlyIn(Dist.CLIENT)
	public static void setClientDestinations(List<SpacePodDestinationDefinition> destinations) {
		clientDestinations = List.copyOf(destinations);
	}

	public static String toJson(List<SpacePodDestinationDefinition> destinations) {
		JsonObject root = new JsonObject();
		JsonArray array = new JsonArray();

		for (SpacePodDestinationDefinition destination : destinations) {
			JsonObject object = new JsonObject();
			object.addProperty("id", destination.id());
			object.addProperty("name", destination.name());
			object.addProperty("translate", destination.translate());
			object.addProperty("dimension", destination.dimension());
			if (destination.iconIndex() != null) {
				object.addProperty("icon_index", destination.iconIndex());
			}
			if (destination.iconTexture() != null) {
				object.addProperty("icon_texture", destination.iconTexture());
			}
			object.add("unlock_rules", unlockToJson(destination.unlockRules()));
			array.add(object);
		}

		root.add("destinations", array);
		return GSON.toJson(root);
	}


	private static JsonElement unlockToJson(SpacePodUnlockExpression expression) {
		if (expression instanceof SpacePodUnlockExpression.Primitive primitive) {
			return GSON.toJsonTree(primitive.rule().name());
		}
		if (expression instanceof SpacePodUnlockExpression.And andExpression) {
			JsonObject object = new JsonObject();
			JsonArray array = new JsonArray();
			for (SpacePodUnlockExpression child : andExpression.children()) {
				array.add(unlockToJson(child));
			}
			object.add("and", array);
			return object;
		}
		if (expression instanceof SpacePodUnlockExpression.Or orExpression) {
			JsonObject object = new JsonObject();
			JsonArray array = new JsonArray();
			for (SpacePodUnlockExpression child : orExpression.children()) {
				array.add(unlockToJson(child));
			}
			object.add("or", array);
			return object;
		}
		if (expression instanceof SpacePodUnlockExpression.Not notExpression) {
			JsonObject object = new JsonObject();
			object.add("not", unlockToJson(notExpression.child()));
			return object;
		}
		throw new IllegalArgumentException("Unknown unlock expression type: " + expression.getClass().getName());
	}

	public static List<SpacePodDestinationDefinition> fromJson(String json) {
		JsonObject root = GSON.fromJson(json, JsonObject.class);
		JsonArray array = GsonHelper.getAsJsonArray(root, "destinations");
		List<SpacePodDestinationDefinition> destinations = new ArrayList<>();
		for (JsonElement element : array) {
			destinations.add(SpacePodDestinationDefinition.fromJson(element.getAsJsonObject()));
		}
		return destinations;
	}

	public static void syncToPlayer(ServerPlayer player) {
		NetworkHandler.sendToPlayer(new SyncSpacePodDestinationsS2C(serverDestinations), player);
	}

	@SubscribeEvent
	public static void onDatapackSync(OnDatapackSyncEvent event) {
		if (event.getPlayer() != null) {
			syncToPlayer(event.getPlayer());
			return;
		}

		for (ServerPlayer player : event.getPlayerList().getPlayers()) {
			syncToPlayer(player);
		}
	}
}
