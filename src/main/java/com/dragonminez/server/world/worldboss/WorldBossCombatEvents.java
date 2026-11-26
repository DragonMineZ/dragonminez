package com.dragonminez.server.world.worldboss;

import com.dragonminez.Reference;
import com.dragonminez.common.combat.HealContext;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.entities.worldboss.AllWorldBossesEntity;
import com.dragonminez.common.init.entities.worldboss.GeteStarEntities;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.common.passives.PassiveEventHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Reference.MOD_ID)
public final class WorldBossCombatEvents {

	private WorldBossCombatEvents() {}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onDamageDealt(LivingDamageEvent.Post event) {
		String key = resolveBossKey(event.getEntity());
		if (key == null) return;
		if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

		double applied = Math.min(event.getNewDamage(), event.getEntity().getHealth());
		WorldBossContribution.addDamage(key, player, applied, classify(event.getSource(), player));
	}

	@SubscribeEvent
	public static void onMitigated(DMZEvent.PlayerDamageMitigatedEvent event) {
		Entity attacker = event.getSource().getEntity();
		String key = resolveBossKey(attacker);
		if (key == null) return;

		ServerPlayer victim = event.getVictim();
		WorldBossContribution.addMitigated(key, victim, event.getDefenseMitigated(), WorldBossContribution.MitigationKind.DEFENSE);
		WorldBossContribution.addMitigated(key, victim, event.getBlockMitigated(), WorldBossContribution.MitigationKind.BLOCK);
		double received = Math.min(event.getFinalDamage(), victim.getHealth());
		WorldBossContribution.addReceived(key, victim, received, attacker.getType().getDescriptionId());
	}

	@SubscribeEvent
	public static void onBarrierAbsorb(DMZEvent.BarrierAbsorbEvent event) {
		String key = resolveBossKey(event.getAttacker());
		if (key == null) return;
		if (!(event.getOwner() instanceof ServerPlayer owner)) return;
		WorldBossContribution.addMitigated(key, owner, event.getAmount(), WorldBossContribution.MitigationKind.SHIELD);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onHeal(LivingHealEvent event) {
		if (event.getAmount() <= 0.0F) return;
		if (!(event.getEntity() instanceof ServerPlayer target)) return;
		if (HealContext.isSystemHeal() || PassiveEventHandler.suppressHealingBonus) return;

		WorldBossSession session = WorldBossManager.activeFor(target);
		if (session == null) return;

		ServerPlayer healer = HealContext.getAllyHealer() instanceof ServerPlayer ally ? ally : target;
		double effective = Math.min(event.getAmount(), Math.max(0.0F, target.getMaxHealth() - target.getHealth()));
		WorldBossContribution.addHealed(session.bossKey(), healer, target, effective,
				healer == target ? WorldBossContribution.HealKind.SELF : WorldBossContribution.HealKind.ALLY);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) return;
		if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
		StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null || !WorldBossManager.tryKnockOut(player, stats, event.getSource())) return;
		event.setCanceled(true);
		player.setHealth(1.0F);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onCasterDamaged(LivingDamageEvent.Post event) {
		if (event.getNewDamage() <= 0.0F || !(event.getEntity() instanceof ServerPlayer player)) return;
		WorldBossManager.noteReviveCasterDamage(player, event.getNewDamage());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onTruceAttack(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer victim)) return;
		if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == victim) return;
		if (WorldBossManager.areTruceAllies(attacker.getUUID(), victim.getUUID(), victim.level().getGameTime())) event.setCanceled(true);
	}

	public static String resolveBossKey(Entity entity) {
		if (entity instanceof WorldBossEntity boss) return boss.getWorldBossKey();
		if (entity instanceof AllWorldBossesEntity.MiniJanemba) return WorldBossEntity.JANEMBA;
		if (AllWorldBossesEntity.Gomah.isEscort(entity)) return WorldBossEntity.GOMAH;
		if (entity != null && GeteStarEntities.ownerOf(entity) != null) return WorldBossEntity.METAL_COOLER_CORE;
		return null;
	}

	private static WorldBossContribution.DamageKind classify(DamageSource source, ServerPlayer attacker) {
		if (MainDamageTypes.isStrikeAttackDamage(source)) return WorldBossContribution.DamageKind.STRIKE;
		if (MainDamageTypes.isKiblastDamage(source)) return WorldBossContribution.DamageKind.KI;
		if (source.getDirectEntity() == null || source.getDirectEntity() == attacker) return WorldBossContribution.DamageKind.MELEE;
		return WorldBossContribution.DamageKind.OTHER;
	}
}
