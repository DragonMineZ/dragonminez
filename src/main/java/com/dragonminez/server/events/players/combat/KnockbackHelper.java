package com.dragonminez.server.events.players.combat;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.KnockbackFlightS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class KnockbackHelper {

	private KnockbackHelper() {}

	private static final int SAGA_KNOCKBACK_LOCK_TICKS = 10;

	public static void apply(LivingEntity target, Vec3 velocity) {
		target.setDeltaMovement(velocity);
		target.hurtMarked = true;

		if (target instanceof ServerPlayer serverPlayer) {
			syncFlight(serverPlayer, velocity);
		} else if (target instanceof DBSagasEntity saga) {
			saga.lockKnockback(SAGA_KNOCKBACK_LOCK_TICKS);
		}
	}

	private static void syncFlight(ServerPlayer serverPlayer, Vec3 velocity) {
		boolean flying = StatsProvider.get(StatsCapability.INSTANCE, serverPlayer)
				.map(data -> data.getSkills().isSkillActive("fly"))
				.orElse(false);
		if (!flying) return;
		NetworkHandler.sendToPlayer(new KnockbackFlightS2C(velocity.x, velocity.y, velocity.z), serverPlayer);
	}
}
