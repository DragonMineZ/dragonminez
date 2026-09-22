package com.dragonminez.server.world.worldboss;

import com.dragonminez.Reference;
import com.dragonminez.common.combat.HealContext;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.entities.worldboss.AllWorldBossesEntity;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.common.passives.PassiveEventHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class WorldBossCombatEvents {

	private WorldBossCombatEvents() {}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onDamageDealt(LivingDamageEvent event) {
		String key = resolveBossKey(event.getEntity());
		if (key == null) return;
		if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

		double applied = Math.min(event.getAmount(), event.getEntity().getHealth());
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

		WorldBossSession session = WorldBossSessions.activeFor(target);
		if (session == null) return;

		ServerPlayer healer = HealContext.getAllyHealer() instanceof ServerPlayer ally ? ally : target;
		double effective = Math.min(event.getAmount(), Math.max(0.0F, target.getMaxHealth() - target.getHealth()));
		WorldBossContribution.addHealed(session.bossKey(), healer, effective,
				healer == target ? WorldBossContribution.HealKind.SELF : WorldBossContribution.HealKind.ALLY);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) return;
		if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
		StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null || !WorldBossSessions.tryKnockOut(player, stats, event.getSource())) return;
		event.setCanceled(true);
		player.setHealth(1.0F);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onCasterHurt(LivingHurtEvent event) {
		if (event.getAmount() <= 0.0F || !(event.getEntity() instanceof ServerPlayer player)) return;
		if (WorldBossSessions.isCastingRevive(player)) WorldBossSessions.interruptRevive(player);
	}

	public static String resolveBossKey(Entity entity) {
		if (entity instanceof WorldBossEntity boss) return boss.getWorldBossKey();
		if (entity instanceof AllWorldBossesEntity.MiniJanemba) return WorldBossManager.JANEMBA;
		return null;
	}

	private static WorldBossContribution.DamageKind classify(DamageSource source, ServerPlayer attacker) {
		if (MainDamageTypes.isStrikeAttackDamage(source)) return WorldBossContribution.DamageKind.STRIKE;
		if (MainDamageTypes.isKiblastDamage(source)) return WorldBossContribution.DamageKind.KI;
		if (source.getDirectEntity() == null || source.getDirectEntity() == attacker) return WorldBossContribution.DamageKind.MELEE;
		return WorldBossContribution.DamageKind.OTHER;
	}
}
