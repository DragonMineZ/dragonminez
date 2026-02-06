package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.EntitiesConfig;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityStatsHandler {

	@SubscribeEvent
	public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity entity)) return;
		if (entity.getPersistentData().getBoolean("dmz_stats_configured")) return;

		boolean isHardMode = entity.getPersistentData().getBoolean("dmz_is_hardmode");

		String registryName = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
		EntitiesConfig.EntityStats stats = ConfigManager.getEntityStats(registryName);

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
}