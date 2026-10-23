package com.dragonminez.common.passives.handlers;

import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.passives.ClassPassives;
import com.dragonminez.common.passives.IClassPassive;
import com.dragonminez.common.passives.PassiveRuntimeState;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class UnversedPassive implements IClassPassive {

	@Override
	public String classKey() { return "unversed"; }

	@Override
	public double kiDamageMultiplier(StatsData data, KiAttackData ki) {
		if (data.getPlayer() == null || ki == null || ki.getEffectiveUtility() == KiAttackData.Utility.HEAL) return 1.0;
		PassiveRuntimeState state = PassiveRuntimeState.peek(data.getPlayer().getUUID());
		if (state == null || !kiPrimed(state, data.getPlayer().level().getGameTime())) return 1.0;
		return 1.0 + ClassPassives.value(data, "kiBonus", 0.15);
	}

	@Override
	public void onKiAttackFired(ServerPlayer player, StatsData data, KiAttackData ki) {
		PassiveRuntimeState state = PassiveRuntimeState.get(player.getUUID());
		long now = player.level().getGameTime();

		if (ki != null && ki.getEffectiveUtility() != KiAttackData.Utility.HEAL) state.unversedKiPrimedUntil = 0L;

		if (!physicalPrimed(state, now)) {
			state.unversedPhysicalCharges = (int) ClassPassives.value(data, "physicalCharges", 3.0);
			state.unversedPhysicalUntil = now + duration(data);
		}
	}

	@Override
	public void onStrikeCast(ServerPlayer player, StatsData data) {
		PassiveRuntimeState state = PassiveRuntimeState.get(player.getUUID());
		long now = player.level().getGameTime();
		if (physicalPrimed(state, now)) {
			state.unversedPhysicalCharges = 0;
			state.unversedPhysicalUntil = 0L;
			state.unversedStrikeBoostUntil = now + (long) ClassPassives.value(data, "strikeWindowTicks", 60.0);
		} else {
			state.unversedStrikeBoostUntil = 0L;
		}
	}

	@Override
	public double outgoingDamageMultiplier(ServerPlayer attacker, StatsData data, LivingEntity target, DMZEvent.DamageSourceType type) {
		PassiveRuntimeState state = PassiveRuntimeState.get(attacker.getUUID());
		long now = attacker.level().getGameTime();
		double bonus = 1.0 + ClassPassives.value(data, "physicalBonus", 0.15);

		if (type == DMZEvent.DamageSourceType.MELEE) {
			if (!physicalPrimed(state, now)) return 1.0;
			state.unversedPhysicalCharges--;
			if (state.unversedPhysicalCharges <= 0) state.unversedPhysicalUntil = 0L;
			return bonus;
		}

		if (type == DMZEvent.DamageSourceType.STRIKE) {
			primeKi(state, data, now);
			return now <= state.unversedStrikeBoostUntil ? bonus : 1.0;
		}

		return 1.0;
	}

	@Override
	public void onMeleeHit(ServerPlayer attacker, StatsData data, LivingEntity target, boolean blocked) {
		if (blocked) return;
		PassiveRuntimeState state = PassiveRuntimeState.get(attacker.getUUID());
		long now = attacker.level().getGameTime();

		if (now > state.unversedMeleeProgressUntil) state.unversedMeleeProgress = 0;
		state.unversedMeleeProgress++;
		state.unversedMeleeProgressUntil = now + (long) ClassPassives.value(data, "comboResetTicks", 100.0);

		if (state.unversedMeleeProgress >= (int) ClassPassives.value(data, "meleeHitsToPrime", 3.0)) {
			state.unversedMeleeProgress = 0;
			primeKi(state, data, now);
		}
	}

	@Override
	public void onPlayerTick(ServerPlayer player, StatsData data) {
		PassiveRuntimeState state = PassiveRuntimeState.peek(player.getUUID());
		if (state == null) return;
		long now = player.level().getGameTime();
		if (state.unversedPhysicalCharges > 0 && now > state.unversedPhysicalUntil) state.unversedPhysicalCharges = 0;
		if (state.unversedMeleeProgress > 0 && now > state.unversedMeleeProgressUntil) state.unversedMeleeProgress = 0;
	}

	private static void primeKi(PassiveRuntimeState state, StatsData data, long now) {
		if (kiPrimed(state, now)) return;
		state.unversedKiPrimedUntil = now + duration(data);
	}

	private static boolean kiPrimed(PassiveRuntimeState state, long now) {
		return now <= state.unversedKiPrimedUntil;
	}

	private static boolean physicalPrimed(PassiveRuntimeState state, long now) {
		return state.unversedPhysicalCharges > 0 && now <= state.unversedPhysicalUntil;
	}

	private static long duration(StatsData data) {
		return (long) ClassPassives.value(data, "chargeDurationTicks", 160.0);
	}
}
