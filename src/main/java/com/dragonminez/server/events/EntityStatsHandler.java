package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.EntitiesConfig;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityStatsHandler {

	@SubscribeEvent
	public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity entity)) return;
		if (entity.getPersistentData().getBoolean("dmz_stats_configured")) return;

		boolean isHardMode = entity.getPersistentData().getBoolean("dmz_is_hardmode");

		String sagaId = entity.getPersistentData().getString("dmz_saga_id");
		if (sagaId.isEmpty()) sagaId = "default";

		String registryName = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
		EntitiesConfig.EntityStats stats = ConfigManager.getEntityStats(sagaId, registryName);

		EntitiesConfig.HardModeSettings hardSettings = ConfigManager.getEntitiesConfig().getHardModeSettings();
		double hpMult = isHardMode ? hardSettings.getHpMultiplier() : 1.0;
		double dmgMult = isHardMode ? hardSettings.getDamageMultiplier() : 1.0;

		if (stats != null) {
			if (stats.getHealth() != null && entity.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
				double finalHealth = stats.getHealth() * hpMult;
				entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(finalHealth);
				entity.setHealth((float) finalHealth);
			}
			if (stats.getMeleeDamage() != null && entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
				double finalDmg = stats.getMeleeDamage() * dmgMult;
				entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(finalDmg);
			}
			if (entity instanceof DBSagasEntity sagaEntity && stats.getKiDamage() != null) {
				double finalKi = stats.getKiDamage() * dmgMult;
				sagaEntity.setKiBlastDamage((float) finalKi);
			}
		}

		entity.getPersistentData().putBoolean("dmz_stats_configured", true);
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
}