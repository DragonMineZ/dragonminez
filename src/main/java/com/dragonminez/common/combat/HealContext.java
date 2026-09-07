package com.dragonminez.common.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class HealContext {
	private static boolean allyTechniqueHeal;

	private HealContext() {
	}

	public static void asAllyHeal(Entity healer, LivingEntity target, Runnable heal) {
		boolean fromAlly = healer instanceof Player && healer != target;
		allyTechniqueHeal = fromAlly;
		try {
			heal.run();
		} finally {
			allyTechniqueHeal = false;
		}
	}

	public static boolean isAllyTechniqueHeal() {
		return allyTechniqueHeal;
	}
}
