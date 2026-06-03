package com.dragonminez.common.passives.handlers;

import com.dragonminez.common.passives.ClassPassives;
import com.dragonminez.common.passives.IClassPassive;
import com.dragonminez.common.passives.PassiveRuntimeState;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class WarriorPassive implements IClassPassive {

	@Override
	public String classKey() { return "warrior"; }

	private int stacks(StatsData data) {
		if (data.getPlayer() == null) return 0;
		PassiveRuntimeState state = PassiveRuntimeState.peek(data.getPlayer().getUUID());
		return state != null ? state.warriorStacks : 0;
	}

	@Override
	public double staminaRegenMultiplier(StatsData data) {
		return 1.0 + stacks(data) * ClassPassives.value(data, "stmRegenPerStack", 0.10);
	}

	@Override
	public double armorPenBonus(StatsData data) {
		int maxStacks = (int) ClassPassives.value(data, "maxStacks", 5.0);
		return stacks(data) >= maxStacks ? ClassPassives.value(data, "armorPenAtMax", 0.10) : 0.0;
	}

	@Override
	public void onMeleeHit(ServerPlayer attacker, StatsData data, LivingEntity target, boolean blocked) {
		PassiveRuntimeState state = PassiveRuntimeState.get(attacker.getUUID());
		long now = attacker.level().getGameTime();

		if (blocked) {
			state.warriorComboProgress = 0;
			return;
		}

		int comboHits = (int) ClassPassives.value(data, "comboHits", 3.0);
		int maxStacks = (int) ClassPassives.value(data, "maxStacks", 5.0);
		long stackDurationTicks = (long) ClassPassives.value(data, "stackDurationTicks", 100.0);
		long comboResetTicks = (long) ClassPassives.value(data, "comboResetTicks", 60.0);

		state.warriorComboProgress++;
		state.warriorComboExpireTick = now + comboResetTicks;
		if (state.warriorComboProgress >= comboHits) {
			state.warriorStacks = Math.min(maxStacks, state.warriorStacks + 1);
			state.warriorComboProgress = 0;
		}
		state.warriorStacksExpireTick = now + stackDurationTicks;
	}

	@Override
	public void onPlayerTick(ServerPlayer player, StatsData data) {
		PassiveRuntimeState state = PassiveRuntimeState.peek(player.getUUID());
		if (state == null) return;
		long now = player.level().getGameTime();
		if (state.warriorStacks > 0 && now > state.warriorStacksExpireTick) state.warriorStacks = 0;
		if (state.warriorComboProgress > 0 && now > state.warriorComboExpireTick) state.warriorComboProgress = 0;
	}
}
