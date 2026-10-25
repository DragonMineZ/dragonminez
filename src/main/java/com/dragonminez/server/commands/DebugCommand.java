package com.dragonminez.server.commands;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.hair.HairStyleSlot;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.DebugFileS2C;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.world.data.PartySavedData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DebugCommand {
	private static final int SCHEMA_VERSION = 1;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().serializeSpecialFloatingPointValues().create();
	private static final List<String> STATS_SECTIONS = List.of("Stats", "BonusStats", "Resources", "Status", "Cooldowns", "Effects", "Skills");
	private static final SuggestionProvider<CommandSourceStack> DEBUG_VALUE_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(List.of("ALL", "STATS", "CHARACTER", "TECHNIQUES", "QUESTS"), builder);

	private static final long COOLDOWN_MS = 5000L;
	private static final Map<UUID, Long> LAST_USE = new ConcurrentHashMap<>();

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzdebug")
				.executes(ctx -> writeDebugFile(ctx.getSource(), ctx.getSource().getPlayerOrException(), DebugScope.ALL))
				.then(Commands.argument("value", StringArgumentType.word()).suggests(DEBUG_VALUE_SUGGESTIONS)
						.executes(ctx -> writeSelfOrNamed(ctx.getSource(), StringArgumentType.getString(ctx, "value"))))
				.then(Commands.argument("target", EntityArgument.player())
						.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.ADMIN))
						.executes(ctx -> writeDebugFile(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), DebugScope.ALL))
						.then(Commands.argument("value", StringArgumentType.word()).suggests(DEBUG_VALUE_SUGGESTIONS)
								.executes(ctx -> writeDebugFile(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "value")))))
		);
	}

	private static int writeSelfOrNamed(CommandSourceStack source, String value) throws CommandSyntaxException {
		if (DebugScope.from(value) == null && DMZPermissions.hasPermission(source, DMZPermissions.ADMIN)) {
			ServerPlayer named = source.getServer().getPlayerList().getPlayerByName(value);
			if (named != null) return writeDebugFile(source, named, DebugScope.ALL);
		}
		return writeDebugFile(source, source.getPlayerOrException(), value);
	}

	private static int writeDebugFile(CommandSourceStack source, ServerPlayer target, String rawScope) {
		DebugScope scope = DebugScope.from(rawScope);
		if (scope == null) {
			source.sendFailure(Component.translatable("command.dragonminez.debug.invalid_value", rawScope));
			return 0;
		}
		return writeDebugFile(source, target, scope);
	}

	private static int writeDebugFile(CommandSourceStack source, ServerPlayer target, DebugScope scope) {
		ServerPlayer requester = source.getPlayer();
		if (requester != null && !DMZPermissions.hasPermission(source, DMZPermissions.ADMIN)) {
			long now = System.currentTimeMillis();
			Long last = LAST_USE.get(requester.getUUID());
			if (last != null && now - last < COOLDOWN_MS) {
				long seconds = (COOLDOWN_MS - (now - last) + 999L) / 1000L;
				source.sendFailure(Component.translatable("command.dragonminez.debug.cooldown", seconds));
				return 0;
			}
			LAST_USE.put(requester.getUUID(), now);
		}

		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		return StatsProvider.get(StatsCapability.INSTANCE, target)
				.map(data -> {
					String json = GSON.toJson(buildDebugJson(target, data, scope));
					if (requester != null) {
						DebugFileS2C packet = new DebugFileS2C(target.getGameProfile().getName(), json);
						if (!packet.fitsInPacket()) {
							source.sendFailure(Component.translatable("command.dragonminez.debug.error", "dump too large to send"));
							return 0;
						}
						NetworkHandler.sendToPlayer(packet, requester);
						return 1;
					}
					try {
						Path outputPath = getOutputPath(target);
						Files.createDirectories(outputPath.getParent());
						Files.writeString(outputPath, json, StandardCharsets.UTF_8);
						Path debugDir = outputPath.getParent();
						Component clickablePath = Component.literal(debugDir.toString())
								.withStyle(style -> style
										.withColor(ChatFormatting.AQUA)
										.withUnderlined(true)
										.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, debugDir.toString()))
										.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("command.dragonminez.debug.open_folder"))));
						source.sendSuccess(() -> Component.translatable("command.dragonminez.debug.success", target.getName().getString(), clickablePath), log);
						return 1;
					} catch (IOException exception) {
						source.sendFailure(Component.translatable("command.dragonminez.debug.error", exception.getMessage()));
						return 0;
					}
				})
				.orElseGet(() -> {
					source.sendFailure(Component.translatable("command.dragonminez.debug.no_data", target.getName().getString()));
					return 0;
				});
	}

	private static Path getOutputPath(ServerPlayer target) {
		return FMLPaths.GAMEDIR.get().resolve("dmzdebug").resolve(target.getGameProfile().getName() + ".json");
	}

	private static JsonObject buildDebugJson(ServerPlayer target, StatsData data, DebugScope scope) {
		MinecraftServer server = target.getServer();
		CompoundTag root = data.save();

		JsonObject json = new JsonObject();
		json.addProperty("schemaVersion", SCHEMA_VERSION);
		json.addProperty("generatedAt", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
		json.addProperty("scope", scope.name());

		JsonObject mod = new JsonObject();
		mod.addProperty("version", ModList.get().getModContainerById(Reference.MOD_ID)
				.map(container -> container.getModInfo().getVersion().toString()).orElse("unknown"));
		mod.addProperty("mc", SharedConstants.getCurrentVersion().getName());
		mod.addProperty("loader", ModList.get().getModContainerById("neoforge")
				.map(container -> container.getModInfo().getVersion().toString()).orElse("unknown"));
		json.add("mod", mod);

		JsonObject serverInfo = new JsonObject();
		serverInfo.addProperty("dedicated", server != null && server.isDedicatedServer());
		json.add("server", serverInfo);

		JsonObject player = new JsonObject();
		player.addProperty("name", target.getGameProfile().getName());
		player.addProperty("uuid", target.getStringUUID());
		player.addProperty("online", !target.hasDisconnected());
		player.addProperty("dimension", target.level().dimension().location().toString());
		JsonArray pos = new JsonArray();
		pos.add(round2(target.getX()));
		pos.add(round2(target.getY()));
		pos.add(round2(target.getZ()));
		player.add("pos", pos);
		json.add("player", player);

		if (scope.includesStats()) {
			JsonObject derived = new JsonObject();
			derived.addProperty("battlePower", data.getBattlePowerExact());
			derived.addProperty("level", data.getLevel());
			derived.addProperty("maxHealth", data.getMaxHealth());
			derived.addProperty("maxEnergy", data.getMaxEnergy());
			derived.addProperty("maxStamina", data.getMaxStamina());
			json.add("derived", derived);
		}

		if (scope.includesQuests()) {
			json.add("party", partyJson(server, target, data.getPlayerQuestData()));
			json.add("quests", questsJson(data.getPlayerQuestData()));
		}

		JsonObject sections = new JsonObject();
		if (scope.includesStats()) {
			for (String key : STATS_SECTIONS) sections.add(key, nbtToJson(root.getCompound(key)));
		}
		if (scope.includesCharacter()) sections.add("Character", nbtToJson(withoutHairData(root.getCompound("Character"))));
		if (scope.includesTechniques()) sections.add("Techniques", nbtToJson(root.getCompound("Techniques")));
		if (scope.includesQuests()) sections.add("PlayerQuestData", nbtToJson(root.getCompound("PlayerQuestData")));
		json.add("sections", sections);

		return json;
	}

	private static double round2(double value) {
		return Math.round(value * 100) / 100.0;
	}

	private static CompoundTag withoutHairData(CompoundTag character) {
		CompoundTag copy = character.copy();
		for (HairStyleSlot slot : HairStyleSlot.values()) copy.remove(slot.getNbtKey());
		return copy;
	}

	private static JsonObject partyJson(MinecraftServer server, ServerPlayer target, PlayerQuestData quest) {
		JsonObject client = new JsonObject();
		client.addProperty("inParty", quest.isInParty());
		client.addProperty("partyId", uuidOrNull(quest.getActivePartyId()));
		client.add("leader", memberJson(server, quest.getPartyLeaderId(), false));
		client.add("members", membersJson(server, quest.getPartyMemberIds(), null));

		JsonObject serverSide = new JsonObject();
		PartySavedData.PartyInstance serverParty = server == null ? null : PartySavedData.get(server).getPartyOf(target.getUUID());
		serverSide.addProperty("inParty", serverParty != null);
		if (serverParty == null) {
			serverSide.add("members", new JsonArray());
		} else {
			serverSide.addProperty("partyId", uuidOrNull(serverParty.getPartyId()));
			serverSide.add("leader", memberJson(server, serverParty.getLeaderId(), true));
			serverSide.addProperty("pvpEnabled", serverParty.isPvpEnabled());
			serverSide.add("members", membersJson(server, serverParty.getMembers(), serverParty.getLeaderId()));
		}

		JsonObject party = new JsonObject();
		party.add("client", client);
		party.add("server", serverSide);
		PlayerQuestData.PartyInviteData invite = quest.getPendingPartyInviteData();
		if (invite == null) {
			party.add("pendingInvite", JsonNull.INSTANCE);
		} else {
			JsonObject inviteJson = new JsonObject();
			inviteJson.addProperty("name", invite.getInviterName());
			inviteJson.addProperty("uuid", uuidOrNull(invite.getInviterUUID()));
			inviteJson.addProperty("expired", invite.isExpired());
			party.add("pendingInvite", inviteJson);
		}
		return party;
	}

	private static JsonArray membersJson(MinecraftServer server, Collection<UUID> ids, UUID leaderId) {
		JsonArray members = new JsonArray();
		for (UUID id : ids) members.add(memberJson(server, id, id.equals(leaderId)));
		return members;
	}

	private static JsonElement memberJson(MinecraftServer server, UUID id, boolean leader) {
		if (id == null) return JsonNull.INSTANCE;
		JsonObject member = new JsonObject();
		member.addProperty("uuid", id.toString());
		member.addProperty("name", resolveName(server, id));
		member.addProperty("leader", leader);
		return member;
	}

	private static JsonObject questsJson(PlayerQuestData quest) {
		JsonObject quests = new JsonObject();
		quests.addProperty("tracked", quest.getTrackedQuestId());

		String lastCompleted = null;
		long lastCompletedMs = Long.MIN_VALUE;
		for (String id : quest.getCompletedQuestIds()) {
			long completedMs = quest.getLastCompletedRealMs(id);
			if (completedMs >= lastCompletedMs) {
				lastCompleted = id;
				lastCompletedMs = completedMs;
			}
		}
		if (lastCompleted == null) {
			quests.add("lastCompleted", JsonNull.INSTANCE);
		} else {
			JsonObject last = new JsonObject();
			last.addProperty("questId", lastCompleted);
			last.addProperty("status", String.valueOf(quest.getQuestStatus(lastCompleted)));
			quests.add("lastCompleted", last);
		}

		quests.add("completed", stringArray(quest.getCompletedQuestIds()));
		quests.add("inProgress", stringArray(quest.getAcceptedQuestIds()));
		quests.add("failed", stringArray(quest.getFailedQuestIds()));
		return quests;
	}

	private static JsonArray stringArray(Collection<String> values) {
		JsonArray array = new JsonArray();
		values.forEach(array::add);
		return array;
	}

	private static JsonElement nbtToJson(Tag tag) {
		if (tag instanceof CompoundTag compound) {
			JsonObject object = new JsonObject();
			for (String key : new TreeSet<>(compound.getAllKeys())) object.add(key, nbtToJson(compound.get(key)));
			return object;
		}
		if (tag instanceof CollectionTag<?> collection) {
			JsonArray array = new JsonArray();
			for (Tag element : collection) array.add(nbtToJson(element));
			return array;
		}
		if (tag instanceof NumericTag numeric) return new JsonPrimitive(numeric.getAsNumber());
		return tag == null ? JsonNull.INSTANCE : new JsonPrimitive(tag.getAsString());
	}

	private static String uuidOrNull(UUID id) {
		return id == null ? null : id.toString();
	}

	private static String resolveName(MinecraftServer server, UUID id) {
		if (server == null) return "unknown";
		ServerPlayer online = server.getPlayerList().getPlayer(id);
		if (online != null) return online.getGameProfile().getName();
		return server.getProfileCache() == null
				? "offline"
				: server.getProfileCache().get(id).map(profile -> profile.getName()).orElse("offline");
	}

	private enum DebugScope {
		ALL,
		STATS,
		CHARACTER,
		TECHNIQUES,
		QUESTS;

		private static DebugScope from(String value) {
			try {
				return DebugScope.valueOf(value.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException exception) {
				return null;
			}
		}

		private boolean includesStats() {
			return this == ALL || this == STATS;
		}

		private boolean includesCharacter() {
			return this == ALL || this == CHARACTER;
		}

		private boolean includesTechniques() {
			return this == ALL || this == TECHNIQUES;
		}

		private boolean includesQuests() {
			return this == ALL || this == QUESTS;
		}
	}
}
