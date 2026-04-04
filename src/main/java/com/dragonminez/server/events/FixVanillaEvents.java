package com.dragonminez.server.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FixVanillaEvents {

	@SubscribeEvent
	public void onLivingHurt(LivingHurtEvent e) {
		float dmg = e.getAmount();
		LivingEntity le = e.getEntity();
		if (Float.isNaN(dmg)) {
			e.setCanceled(true);
			rectify(le);
		}
	}

	@SubscribeEvent
	public void onLivingDamage(LivingDamageEvent e) {
		LivingEntity le = e.getEntity();
		float dmg = e.getAmount();
		if (Float.isNaN(dmg)) {
			e.setCanceled(true);
			rectify(le);
		}
	}

	@SubscribeEvent
	public void onAttackEntity(LivingAttackEvent e) {
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
