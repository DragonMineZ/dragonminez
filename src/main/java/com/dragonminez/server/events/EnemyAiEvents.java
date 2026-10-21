package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EnemyAiEvents {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingHurt(LivingHurtEvent event) {
		if (event.getEntity().level().isClientSide() || event.isCanceled()) return;
		Entity attacker = event.getSource().getEntity();
		if (!(attacker instanceof DBSagasEntity saga)) return;
		boolean ki = MainDamageTypes.isKiblastDamage(event.getSource())
				|| event.getSource().getDirectEntity() instanceof AbstractKiProjectile;
		saga.getCombatBrain().onDealtDamage(event.getEntity(), event.getAmount(), ki);
	}
}
