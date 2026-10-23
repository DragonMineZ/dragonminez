package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class EnemyAiEvents {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingHurt(LivingIncomingDamageEvent event) {
		if (event.getEntity().level().isClientSide() || event.isCanceled()) return;
		Entity attacker = event.getSource().getEntity();
		if (!(attacker instanceof DBSagasEntity saga)) return;
		boolean ki = MainDamageTypes.isKiblastDamage(event.getSource())
				|| event.getSource().getDirectEntity() instanceof AbstractKiProjectile;
		saga.getCombatBrain().onDealtDamage(event.getEntity(), event.getAmount(), ki);
	}
}
