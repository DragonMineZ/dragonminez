package com.dragonminez.common.passives;

import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public interface IClassPassive {

	String classKey();

	default void onPlayerTick(ServerPlayer player, StatsData data) {}

	default void onPlayerSecond(ServerPlayer player, StatsData data) {}

	default void onMeleeHit(ServerPlayer attacker, StatsData data, LivingEntity target, boolean blocked) {}

	default double staminaRegenMultiplier(StatsData data) { return 1.0; }

	default double healthRegenMultiplier(StatsData data) { return 1.0; }

	default double bonusHpRegenFromStamina(StatsData data) { return 0.0; }

	default double armorPenBonus(StatsData data) { return 0.0; }

	default double critChanceBonus(StatsData data) { return 0.0; }

	default double strikeDamageMultiplier(StatsData attacker, LivingEntity target) { return 1.0; }

	default double healingReceivedMultiplier(StatsData data) { return 1.0; }

	default double kiCooldownMultiplier(StatsData data, KiAttackData ki) { return 1.0; }

	default double secondaryDurationMultiplier(StatsData data, KiAttackData ki) { return 1.0; }

	default double kiDamageMultiplier(StatsData data, KiAttackData ki) { return 1.0; }

	default void onKiAttackFired(ServerPlayer player, StatsData data, KiAttackData ki) {}

	default void onStrikeCast(ServerPlayer player, StatsData data) {}

	default double outgoingDamageMultiplier(ServerPlayer attacker, StatsData data, LivingEntity target, DMZEvent.DamageSourceType type) { return 1.0; }
}
