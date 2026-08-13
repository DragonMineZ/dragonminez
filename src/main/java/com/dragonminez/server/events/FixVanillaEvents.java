package com.dragonminez.server.events;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Guards against NaN health/damage edge cases that can brick entities. */
public class FixVanillaEvents {

	@SubscribeEvent
	public void onLivingHurt(LivingDamageEvent.Pre e) {
		float dmg = e.getNewDamage();
		LivingEntity le = e.getEntity();
		if (Float.isNaN(dmg)) {
			e.setNewDamage(0f);
			rectify(le);
		}
	}

	@SubscribeEvent
	public void onLivingDamage(LivingDamageEvent.Post e) {
		LivingEntity le = e.getEntity();
		float dmg = e.getNewDamage();
		if (Float.isNaN(dmg)) {
			rectify(le);
		}
	}

	@SubscribeEvent
	public void onAttackEntity(LivingIncomingDamageEvent e) {
		LivingEntity le = e.getEntity();
		float dmg = e.getAmount();
		if (Float.isNaN(dmg)) {
			e.setCanceled(true);
			rectify(le);
		}
	}

	@SubscribeEvent
	public void onLivingHeal(LivingHealEvent e) {
		float amount = e.getAmount();
		LivingEntity le = e.getEntity();
		if (Float.isNaN(amount)) {
			e.setCanceled(true);
			return;
		}

		if (Float.isNaN(le.getHealth())) {
			e.setCanceled(true);
			rectify(le);
		}
	}

	@SubscribeEvent
	public void onLivingDeath(LivingDeathEvent e) {
		LivingEntity le = e.getEntity();
		float hp = le.getHealth();
		if (Float.isNaN(hp)) {
			e.setCanceled(true);
			rectify(le);
		}
	}

	private void rectify(LivingEntity le) {
		le.setHealth(le.getMaxHealth());
		le.setAbsorptionAmount(0);
	}
}
