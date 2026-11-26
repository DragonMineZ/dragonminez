package com.dragonminez.common.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class HealContext {
	private static boolean allyTechniqueHeal;
	private static Entity allyHealer;
	private static boolean systemHeal;

	private HealContext() {
	}

	public static void asAllyHeal(Entity healer, LivingEntity target, Runnable heal) {
		boolean fromAlly = healer instanceof Player && healer != target;
		allyTechniqueHeal = fromAlly;
		allyHealer = fromAlly ? healer : null;
		try {
			heal.run();
		} finally {
			allyTechniqueHeal = false;
			allyHealer = null;
		}
	}

	public static void asSystemHeal(Runnable heal) {
		boolean previous = systemHeal;
		systemHeal = true;
		try {
			heal.run();
		} finally {
			systemHeal = previous;
		}
	}

	public static Entity getAllyHealer() {
		return allyHealer;
	}

	public static boolean isAllyTechniqueHeal() {
		return allyTechniqueHeal;
	}

	public static boolean isSystemHeal() {
		return systemHeal;
	}
}
