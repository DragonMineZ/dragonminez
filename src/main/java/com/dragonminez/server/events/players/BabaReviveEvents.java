package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.util.BabaReviveService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class BabaReviveEvents {

	private BabaReviveEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) return;
		if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
		StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null || !BabaReviveService.tryKnockOut(player, stats, event.getSource())) return;
		event.setCanceled(true);
		player.setHealth(1.0F);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onHeal(LivingHealEvent event) {
		if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (BabaReviveService.isHealingBlocked(data)) event.setCanceled(true);
		});
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onHealthRegen(DMZEvent.HealthRegenEvent event) {
		blockRegen(event);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEnergyRegen(DMZEvent.EnergyRegenEvent event) {
		blockRegen(event);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onStaminaRegen(DMZEvent.StaminaRegenEvent event) {
		blockRegen(event);
	}

	private static void blockRegen(DMZEvent.ResourceRegenEvent event) {
		if (BabaReviveService.isHealingBlocked(event.getStatsData())) event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onChangeTarget(LivingChangeTargetEvent event) {
		LivingEntity newTarget = event.getNewAboutToBeSetTarget();
		if (!(newTarget instanceof ServerPlayer player)) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (BabaReviveService.isHiddenFromMobs(data)) event.setCanceled(true);
		});
	}
}
