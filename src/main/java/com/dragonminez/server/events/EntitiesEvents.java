package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.EntitiesConfig;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.entities.ITextureVariant;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.C2S.SummonPlayerShadowDummyC2S;
import com.dragonminez.common.init.entities.ShadowDummyEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.server.MinecraftServer;
import com.dragonminez.common.quest.QuestService;
import com.dragonminez.server.world.data.PartySavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntitiesEvents {

	private static final double QUEST_TETHER_RANGE_SQR = 31250.0;

	@SubscribeEvent
	public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity entity)) return;
		if (entity.getPersistentData().getBoolean("dmz_stats_configured")) return;

		boolean isHardMode = entity.getPersistentData().getBoolean("dmz_is_hardmode");
		EntitiesConfig.HardModeSettings hardSettings = ConfigManager.getEntitiesConfig().getHardModeSettings();
		double hpMult = isHardMode ? hardSettings.getHpMultiplier() : 1.0;
		double dmgMult = isHardMode ? hardSettings.getDamageMultiplier() : 1.0;

		boolean isQuestEntity = entity.getPersistentData().contains("dmz_quest_hp");

		if (isQuestEntity) {
			double finalHealth = entity.getPersistentData().getDouble("dmz_quest_hp") * hpMult;
			double finalMelee = entity.getPersistentData().getDouble("dmz_quest_melee") * dmgMult;
			double finalKi = entity.getPersistentData().getDouble("dmz_quest_ki") * dmgMult;

			applyStatsToEntity(entity, finalHealth, finalMelee, finalKi);

			if (entity.getPersistentData().contains("dmz_quest_texture_variant") && entity instanceof ITextureVariant variantEntity) {
				variantEntity.setTextureVariant(entity.getPersistentData().getInt("dmz_quest_texture_variant"));
			}
		} else {
			String registryName = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
			EntitiesConfig.EntityStats defaultStats = ConfigManager.getEntityStats(registryName);

			if (defaultStats != null) {
				double finalHealth = (defaultStats.getHealth() != null ? defaultStats.getHealth() : 20.0) * hpMult;
				double finalMelee = (defaultStats.getMeleeDamage() != null ? defaultStats.getMeleeDamage() : 1.0) * dmgMult;
				double finalKi = (defaultStats.getKiDamage() != null ? defaultStats.getKiDamage() : 1.0) * dmgMult;

				applyStatsToEntity(entity, finalHealth, finalMelee, finalKi);
			}
		}

		entity.getPersistentData().putBoolean("dmz_stats_configured", true);
	}

	private static void applyStatsToEntity(LivingEntity entity, double health, double melee, double ki) {
		if (entity.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
			entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
			entity.heal((float) health);
		}
		if (entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
			entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(melee);
		}
		if (entity.getAttributes().hasAttribute(EntityAttributes.KI_BLAST_DAMAGE.get())) {
			entity.getAttribute(EntityAttributes.KI_BLAST_DAMAGE.get()).setBaseValue(ki);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
			cleanupQuestEntities(player.serverLevel(), player.getUUID());
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
			ServerLevel oldLevel = player.getServer().getLevel(event.getFrom());
			if (oldLevel != null) cleanupQuestEntities(oldLevel, player.getUUID());
		}
	}

	@SubscribeEvent
	public static void onPlayerDeath(PlayerEvent.PlayerRespawnEvent event) {
		if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
			cleanupQuestEntities(player.serverLevel(), player.getUUID());
		}
	}

	@SubscribeEvent
	public static void onEntityTick(LivingEvent.LivingTickEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity.level().isClientSide() || entity.tickCount % 1000 != 0) return;

		if (!entity.getPersistentData().contains("dmz_quest_owner")) return;

		String ownerUUIDStr = entity.getPersistentData().getString("dmz_quest_owner");
		try {
			UUID ownerUUID = UUID.fromString(ownerUUIDStr);
			MinecraftServer server = entity.getServer();

			if (entity.getPersistentData().contains(QuestService.QUEST_KEY_TAG)) {
				if (!hasQuestGuardianInRange(server, entity, ownerUUID, null)) {
					entity.discard();
				}
				return;
			}

			ServerPlayer player = server.getPlayerList().getPlayer(ownerUUID);
			if (player == null || player.level() != entity.level() || entity.distanceToSqr(player) > QUEST_TETHER_RANGE_SQR) {
				entity.discard();
			}
		} catch (Exception e) {
			entity.discard();
		}
	}

	public static void cleanupQuestEntities(ServerLevel level, UUID playerUUID) {
		String uuidStr = playerUUID.toString();
		MinecraftServer server = level.getServer();

		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof LivingEntity)) continue;
			if (!entity.getPersistentData().contains("dmz_quest_owner")) continue;

			String ownerUUID = entity.getPersistentData().getString("dmz_quest_owner");
			if (!ownerUUID.equals(uuidStr)) continue;

			if (entity instanceof ShadowDummyEntity && entity.getPersistentData().getBoolean(SummonPlayerShadowDummyC2S.TAG_PLAYER_SHADOW)) {
				ServerPlayer owner = server.getPlayerList().getPlayer(playerUUID);
				if (owner != null) {
					StatsProvider.get(StatsCapability.INSTANCE, owner).ifPresent(data -> {
						if (data.getStatus().hasActiveShadowDummy() && data.getStatus().getActiveShadowDummyUUID().equals(entity.getUUID())) {
							SummonPlayerShadowDummyC2S.removePenalties(owner, data);
							data.getStatus().setActiveShadowDummyUUID(null);
							data.getStatus().setShadowDummyPercent(0);
							NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(owner), owner);
						}
					});
				}
				entity.discard();
				continue;
			}

			if (entity.getPersistentData().contains(QuestService.QUEST_KEY_TAG)
					&& hasQuestGuardianInRange(server, entity, playerUUID, playerUUID)) {
				continue;
			}

			entity.discard();
		}
	}

	private static boolean hasQuestGuardianInRange(MinecraftServer server, Entity questEntity, UUID ownerUUID, UUID excluded) {
		for (ServerPlayer guardian : questGuardians(server, ownerUUID)) {
			if (excluded != null && guardian.getUUID().equals(excluded)) continue;
			if (guardian.level() == questEntity.level() && questEntity.distanceToSqr(guardian) <= QUEST_TETHER_RANGE_SQR) {
				return true;
			}
		}
		return false;
	}

	private static List<ServerPlayer> questGuardians(MinecraftServer server, UUID ownerUUID) {
		List<ServerPlayer> guardians = new ArrayList<>();
		if (server == null || ownerUUID == null) return guardians;

		PartySavedData.PartyInstance party = PartySavedData.get(server).getPartyOf(ownerUUID);
		if (party != null) {
			for (UUID memberId : party.getMembers()) {
				ServerPlayer member = server.getPlayerList().getPlayer(memberId);
				if (member != null) guardians.add(member);
			}
			return guardians;
		}

		ServerPlayer owner = server.getPlayerList().getPlayer(ownerUUID);
		if (owner != null) guardians.add(owner);
		return guardians;
	}
	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (!event.getLevel().isClientSide && event.getTarget() instanceof MastersEntity master) {
			ServerPlayer player = (ServerPlayer) event.getEntity();
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				String dimId = player.level().dimension().location().toString();
				data.getCharacter().addInteractedMaster(master.getStringUUID(), master.getName().getString(), dimId, master.blockPosition());
				NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(player), player);
			});
		}
	}
}