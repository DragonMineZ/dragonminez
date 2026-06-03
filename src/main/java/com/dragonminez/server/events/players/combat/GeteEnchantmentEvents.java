package com.dragonminez.server.events.players.combat;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEnchants;
import com.dragonminez.server.events.players.TickHandler;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gameplay effects for Bulma's Gete-tech armor enchantments (Pillar IV).
 * <ul>
 *   <li><b>Gravity Forged</b> — reduces incoming knockback while worn.</li>
 *   <li><b>Gete Plating</b> — flat reduction of final incoming damage.</li>
 * </ul>
 * (Ki Conductivity is handled in {@link TickHandler}'s ki/energy regen.) Levels are summed across all
 * four armor slots, matching the recovery enchantments.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class GeteEnchantmentEvents {

	@SubscribeEvent
	public static void onKnockback(LivingKnockBackEvent event) {
		int level = TickHandler.getTotalArmorEnchantmentLevel(MainEnchants.GRAVITY_FORGED.get(), event.getEntity());
		if (level <= 0) return;
		double factor = Math.max(0.4, 1.0 - level * 0.15);
		event.setStrength((float) (event.getStrength() * factor));
	}

	@SubscribeEvent
	public static void onDamage(LivingDamageEvent event) {
		int level = TickHandler.getTotalArmorEnchantmentLevel(MainEnchants.GETE_PLATING.get(), event.getEntity());
		if (level <= 0) return;
		float multiplier = (float) (1.0 - Math.min(0.20, level * 0.04));
		event.setAmount(event.getAmount() * multiplier);
	}
}
