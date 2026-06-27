package com.dragonminez.server.events.players.combat;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.stats.character.EntityStatDebuffs;
import com.dragonminez.common.stats.character.SecondaryStatEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityStatDebuffHandler {

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void onLivingHurt(LivingHurtEvent event) {
		if (event.getEntity().level().isClientSide) return;

		float amount = event.getAmount();
		DamageSource source = event.getSource();
		boolean isKi = MainDamageTypes.isKiblastDamage(source);

		if (source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player)) {
			if (isKi) amount *= (float) EntityStatDebuffs.getMultiplier(attacker, SecondaryStatEffects.PWR);
			else if (source.getDirectEntity() == attacker) {
				amount *= (float) EntityStatDebuffs.getMultiplier(attacker, SecondaryStatEffects.STR);
			}
		}

		Entity victim = event.getEntity();
		if (victim instanceof LivingEntity living && !(victim instanceof Player)) {
			double defMult = EntityStatDebuffs.getMultiplier(living, SecondaryStatEffects.DEF);
			if (defMult < 1.0) amount *= (float) (2.0 - defMult);
		}

		if (amount != event.getAmount()) event.setAmount(Math.max(0.0f, amount));
	}

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void onLivingHeal(LivingHealEvent event) {
		if (event.getEntity().level().isClientSide) return;
		if (event.getEntity() instanceof Player) return;

		double mult = EntityStatDebuffs.getMultiplier(event.getEntity(), SecondaryStatEffects.HP_REGEN);
		if (mult < 1.0) event.setAmount(Math.max(0.0f, (float) (event.getAmount() * mult)));
	}
}
