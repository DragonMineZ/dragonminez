package com.dragonminez.common.stats;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.server.events.players.TickHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.SyncQuestRegistryS2C;
import com.dragonminez.common.network.S2C.SyncServerConfigS2C;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.world.structure.helper.QuestStructureHints;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.bus.api.IEventBus;
import com.dragonminez.compat.capabilities.Capability;
import com.dragonminez.compat.capabilities.CapabilityManager;
import com.dragonminez.compat.capabilities.CapabilityToken;
import com.dragonminez.compat.capabilities.RegisterCapabilitiesEvent;
import com.dragonminez.compat.capabilities.AttachCapabilitiesEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class StatsCapability {
	public static final Capability<StatsData> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
	});
	private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
			DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Reference.MOD_ID);
	public static final DeferredHolder<AttachmentType<?>, AttachmentType<StatsProvider>> PLAYER_STATS =
			ATTACHMENTS.register("player_stats", () -> AttachmentType
					.serializable(holder -> new StatsProvider((Player) holder))
					.copyOnDeath()
					.build());

	private static StatsData CLIENT_CACHE;

	public static void clearClientCache() {
		CLIENT_CACHE = null;
	}

	public static void register(IEventBus modEventBus) {
		ATTACHMENTS.register(modEventBus);
	}

	@SubscribeEvent
	public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
		event.register(StatsData.class);
	}

	@SubscribeEvent
	public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof Player player) {
			// Materialize the serializable NeoForge attachment for this player.
			StatsProvider.getOrCreate(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		Player player = event.getEntity();
		Player original = event.getOriginal();

		TickHandler.registerForceKillGrace(player.getUUID());
		StatsProvider.get(INSTANCE, player).ifPresent(newData -> {
			StatsProvider.get(INSTANCE, original).ifPresent(oldData -> {
				newData.copyFrom(oldData);

				if (player.level().isClientSide) {
					if (oldData.getStatus().isHasCreatedCharacter()) CLIENT_CACHE = oldData;
					else if (CLIENT_CACHE != null) newData.copyFrom(CLIENT_CACHE);
				}
			});
			// Ensure attribute mirrors + pools on the new player entity after clone.
			if (player instanceof ServerPlayer serverPlayer) {
				com.dragonminez.server.events.players.StatsEvents.restoreStatsPoolsOnJoin(serverPlayer);
			} else {
				newData.reapplyStatAttributes();
			}
		});
		// Drop original provider mapping after clone copy.
		StatsProvider.remove(original);
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer serverPlayer) {
			List<String> availableConfigs = ConfigManager.getAvailableConfigFiles();
			boolean resetBatch = true;
			for (String file : availableConfigs) {
				if (file.equals(ConfigManager.CLIENT_ONLY_CONFIG)) continue;
				String jsonPayload = ConfigManager.getSpecificConfigJson(file);
				if (jsonPayload == null || jsonPayload.isBlank()) continue;
				NetworkHandler.sendToPlayer(new SyncServerConfigS2C(file, jsonPayload, resetBatch), serverPlayer);
				resetBatch = false;
			}
			NetworkHandler.sendToPlayer(new SyncQuestRegistryS2C(QuestRegistry.getAllSagas(), QuestRegistry.getAllQuests()), serverPlayer);

			MinecraftServer server = serverPlayer.getServer();
			if (server != null && !QuestStructureHints.isResolved()) {
				UUID playerId = serverPlayer.getUUID();
				QuestStructureHints.ensureResolvedAsync(server).thenRun(() -> server.execute(() -> {
					ServerPlayer online = server.getPlayerList().getPlayer(playerId);
					if (online != null) NetworkHandler.sendToPlayer(new SyncQuestRegistryS2C(QuestRegistry.getAllSagas(), QuestRegistry.getAllQuests()), online);
				}));
			}

			StatsProvider.get(INSTANCE, serverPlayer).ifPresent(data -> {
				markCurrentDimensionVisited(serverPlayer, data);
				PlayerQuestData questData = data.getPlayerQuestData();
				if (questData.isSagaLocked("saiyan_saga")) questData.setSagaUnlocked("saiyan_saga", true);
				TransformationsHelper.ensureSelectedFormDefault(data);
				TransformationsHelper.ensureSelectedStackFormDefault(data);

				data.getStatus().setStrikeLocked(false);
				data.getStatus().setStunEffect(false);
				data.getStatus().setKnockedDown(false);
				data.getCooldowns().removeCooldown(Cooldowns.KNOCKDOWN_DURATION);

				Map<String, String> repairedSkills = data.getSkills().repairSkillNames();
				if (!repairedSkills.isEmpty()) {
					repairedSkills.forEach((oldName, newName) -> LogUtil.info(Env.SERVER, "Repaired skill for {}: '{}' -> '{}'", serverPlayer.getGameProfile().getName(), oldName, newName));
				}
				data.getSkills().setSkillActive("kisense", false);
				// VIT/ENE fields from NBT → attributes + HP mod + reclamp max ki/stamina (like health fix).
				com.dragonminez.server.events.players.StatsEvents.restoreStatsPoolsOnJoin(serverPlayer);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
			});
		}
		event.getEntity().refreshDimensions();
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!event.getEntity().level().isClientSide) {
			StatsProvider.get(INSTANCE, event.getEntity()).ifPresent(StatsData::tick);
		}
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer serverPlayer) {
			StatsProvider.get(INSTANCE, serverPlayer).ifPresent(data -> {
				data.getResources().setCurrentEnergy(data.getMaxEnergy());
				data.getResources().setCurrentStamina(data.getMaxStamina());
				data.getStatus().setStrikeLocked(false);
				data.getStatus().setStunEffect(false);
				data.getStatus().setKnockedDown(false);
				data.getCooldowns().removeCooldown(Cooldowns.KNOCKDOWN_DURATION);
				NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(serverPlayer), serverPlayer);
			});
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer serverPlayer) {
			StatsProvider.get(INSTANCE, serverPlayer).ifPresent(data -> {
				markCurrentDimensionVisited(serverPlayer, data);
				data.getSkills().setSkillActive("kisense", false);

				data.getStatus().setStrikeLocked(false);
				data.getStatus().setStunEffect(false);
				data.getStatus().setKnockedDown(false);
				data.getCooldowns().removeCooldown(Cooldowns.KNOCKDOWN_DURATION);

				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
			});
		}
	}

	private static void markCurrentDimensionVisited(ServerPlayer player, StatsData data) {
		data.getStatus().markVisitedDimension(player.serverLevel().dimension().location().toString());
	}
}
