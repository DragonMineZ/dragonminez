package com.dragonminez.server.world.worldboss;

import com.dragonminez.Reference;
import com.dragonminez.common.combat.HealContext;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.entities.worldboss.AllWorldBossesEntity;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class WorldBossCombatEvents {

	private static final double ENGAGE_RADIUS_SQR = 80.0D * 80.0D;

	private WorldBossCombatEvents() {}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onDamageDealt(LivingDamageEvent event) {
		if (!(event.getEntity() instanceof WorldBossEntity boss)) return;
		if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

		double applied = Math.min(event.getAmount(), boss.getHealth());
		WorldBossContribution.addDamage(boss.getWorldBossKey(), player, applied);
	}

	@SubscribeEvent
	public static void onBlocked(DMZEvent.PlayerBlockEvent event) {
		LivingEntity attacker = event.getAttacker();
		String key = resolveBossKey(attacker);
		if (key == null) return;

		double mitigated = event.getOriginalDamage() - event.getFinalDamage();
		WorldBossContribution.addBlocked(key, event.getVictim(), mitigated);
	}

	@SubscribeEvent
	public static void onHeal(LivingHealEvent event) {
		if (!HealContext.isAllyTechniqueHeal()) return;
		if (!(HealContext.getAllyHealer() instanceof ServerPlayer healer)) return;
		if (!(event.getEntity() instanceof ServerPlayer)) return;

		String key = nearbyBossKey(healer);
		if (key == null) return;

		WorldBossContribution.addHealed(key, healer, event.getAmount());
	}

	private static String resolveBossKey(Entity attacker) {
		if (attacker instanceof WorldBossEntity boss) return boss.getWorldBossKey();
		if (attacker instanceof AllWorldBossesEntity.MiniJanemba) return WorldBossManager.JANEMBA;
		return null;
	}

	private static String nearbyBossKey(ServerPlayer player) {
		for (Entity entity : player.serverLevel().getEntities().getAll()) {
			if (!(entity instanceof WorldBossEntity boss)) continue;
			if (boss.isBossAsleep() || !boss.isAlive()) continue;
			if (boss.distanceToSqr(player) > ENGAGE_RADIUS_SQR) continue;
			return boss.getWorldBossKey();
		}
		return null;
	}
}
