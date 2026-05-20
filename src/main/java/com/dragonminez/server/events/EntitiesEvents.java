package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.EntitiesConfig;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
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

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntitiesEvents {

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
			entity.setHealth((float) health);
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

		if (entity.getPersistentData().contains("dmz_quest_owner")) {
			String ownerUUIDStr = entity.getPersistentData().getString("dmz_quest_owner");
			try {
				UUID ownerUUID = UUID.fromString(ownerUUIDStr);
				ServerPlayer player = entity.getServer().getPlayerList().getPlayer(ownerUUID);

				if (player == null || player.level() != entity.level() || entity.distanceToSqr(player) > 31250) {
					entity.discard();
				}
			} catch (Exception e) {
				entity.discard();
			}
		}
	}

	public static void cleanupQuestEntities(ServerLevel level, UUID playerUUID) {
		String uuidStr = playerUUID.toString();

		for (Entity entity : level.getAllEntities()) {
			if (entity instanceof LivingEntity) {
				if (entity.getPersistentData().contains("dmz_quest_owner")) {
					String ownerUUID = entity.getPersistentData().getString("dmz_quest_owner");
					if (ownerUUID.equals(uuidStr)) entity.discard();
				}
			}
		}
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