package com.dragonminez.common.passives;

import com.dragonminez.Reference;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PassiveEventHandler {

	private static boolean redirecting = false;

	public static boolean suppressHealingBonus = false;

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
		if (!(event.player instanceof ServerPlayer player)) return;

		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null || !data.getStatus().isHasCreatedCharacter()) return;

		IClassPassive passive = ClassPassives.get(data);
		passive.onPlayerTick(player, data);
		if (player.tickCount % 20 == 0) passive.onPlayerSecond(player, data);
	}

	@SubscribeEvent
	public static void onHealthRegen(DMZEvent.HealthRegenEvent event) {
		StatsData data = event.getStatsData();
		IClassPassive p = ClassPassives.get(data);
		double amount = (event.getAmount() * p.healthRegenMultiplier(data) + p.bonusHpRegenFromStamina(data)) * p.healingReceivedMultiplier(data);
		event.setAmount(amount);
	}

	@SubscribeEvent
	public static void onStaminaRegen(DMZEvent.StaminaRegenEvent event) {
		StatsData data = event.getStatsData();
		event.setAmount(event.getAmount() * ClassPassives.get(data).staminaRegenMultiplier(data));
	}

	@SubscribeEvent
	public static void onCritChance(DMZEvent.CritChanceEvent event) {
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, event.getPlayer()).orElse(null);
		if (data == null) return;
		event.setChance(event.getChance() + ClassPassives.get(data).critChanceBonus(data));
	}

	@SubscribeEvent
	public static void onDamageModify(DMZEvent.DamageModifyEvent event) {
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, event.getAttacker()).orElse(null);
		if (data == null) return;
		IClassPassive p = ClassPassives.get(data);

		double mult = event.getSourceType() == DMZEvent.DamageSourceType.STRIKE ? p.strikeDamageMultiplier(data, event.getVictim()) : 1.0;
		event.setAmount(event.getAmount() * mult);
		event.setDefensePenetration(event.getDefensePenetration() + p.armorPenBonus(data));
	}

	@SubscribeEvent
	public static void onDamageDealt(DMZEvent.DamageDealtEvent event) {
		if (event.getSourceType() != DMZEvent.DamageSourceType.MELEE) return;
		if (!(event.getAttacker() instanceof ServerPlayer attacker)) return;
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, attacker).orElse(null);
		if (data == null) return;

		boolean blocked = event.isBlocked() || event.isParried();
		if (blocked) ClassPassives.get(data).onMeleeHit(attacker, data, event.getVictim(), true);
		else if (event.getAmount() > 0) ClassPassives.get(data).onMeleeHit(attacker, data, event.getVictim(), false);
	}

	@SubscribeEvent
	public static void onKiAttackFire(DMZEvent.KiAttackFireEvent event) {
		StatsData data = event.getStatsData();
		double mult = ClassPassives.get(data).kiCooldownMultiplier(data, event.getKiAttack());
		event.setCooldownTicks((int) Math.max(1, Math.round(event.getCooldownTicks() * mult)));
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingHurt(LivingHurtEvent event) {
		if (event.getEntity().level().isClientSide) return;

		if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
			applyPaladinLifesteal(attacker, event.getAmount());
		}

		if (!redirecting && event.getEntity() instanceof ServerPlayer victim) {
			applyPaladinRedirect(victim, event);
		}
	}

	private static void applyPaladinLifesteal(ServerPlayer member, float damageDealt) {
		if (damageDealt <= 0.0f) return;
		ServerPlayer paladin = findPartyPaladin(member, null);
		if (paladin == null) return;
		double pct = ClassPassives.value(paladinData(paladin), "lifestealPct", 0.15);
		if (pct > 0.0) paladin.heal((float) (damageDealt * pct));
	}

	private static void applyPaladinRedirect(ServerPlayer victim, LivingHurtEvent event) {
		ServerPlayer paladin = findPartyPaladin(victim, victim);
		if (paladin == null) return;
		if (event.getSource().getEntity() == paladin) return;

		double pct = ClassPassives.value(paladinData(paladin), "redirectPct", 0.15);
		if (pct <= 0.0) return;

		boolean hasRaw = victim.getPersistentData().contains("dmz_raw_damage");
		double raw = hasRaw ? victim.getPersistentData().getDouble("dmz_raw_damage") : event.getAmount();
		double redirect = raw * pct;
		if (redirect <= 0.0) return;

		if (hasRaw) victim.getPersistentData().putDouble("dmz_raw_damage", Math.max(0.0, raw - redirect));
		else event.setAmount((float) Math.max(0.0, raw - redirect));

		redirecting = true;
		try {
			paladin.hurt(paladin.damageSources().generic(), (float) redirect);
		} finally {
			redirecting = false;
		}
	}

	private static StatsData paladinData(ServerPlayer paladin) {
		return StatsProvider.get(StatsCapability.INSTANCE, paladin).orElse(null);
	}

	private static ServerPlayer findPartyPaladin(ServerPlayer member, ServerPlayer exclude) {
		List<ServerPlayer> party = PartyManager.getAllPartyMembers(member);
		if (party == null || party.size() <= 1) return null;
		for (ServerPlayer candidate : party) {
			if (candidate == exclude) continue;
			if (!candidate.isAlive() || candidate.level() != member.level()) continue;
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, candidate).orElse(null);
			if (data != null && ClassPassives.is(data, "paladin")) return candidate;
		}
		return null;
	}

	@SubscribeEvent
	public static void onLivingHeal(LivingHealEvent event) {
		if (event.getEntity().level().isClientSide) return;
		if (suppressHealingBonus) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) return;

		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null || !data.getStatus().isHasCreatedCharacter()) return;

		double mult = ClassPassives.get(data).healingReceivedMultiplier(data);
		if (mult != 1.0) event.setAmount((float) (event.getAmount() * mult));
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		PassiveRuntimeState.clear(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof Player player) PassiveRuntimeState.clear(player.getUUID());
	}
}
