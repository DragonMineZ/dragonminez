package com.dragonminez.common.passives;

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
}
