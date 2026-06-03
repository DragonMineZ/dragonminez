package com.dragonminez.common.passives.handlers;

import com.dragonminez.common.passives.ClassPassives;
import com.dragonminez.common.passives.IClassPassive;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class MartialArtistPassive implements IClassPassive {

	@Override
	public String classKey() { return "martialartist"; }

	@Override
	public double strikeDamageMultiplier(StatsData attacker, LivingEntity target) {
		if (target == null || target.getMaxHealth() <= 0.0f) return 1.0;
		double hpHigh = ClassPassives.value(attacker, "hpHigh", 0.75);
		double hpLow = ClassPassives.value(attacker, "hpLow", 0.25);
		double maxBonus = ClassPassives.value(attacker, "maxBonus", 0.25);

		double hpRatio = target.getHealth() / target.getMaxHealth();
		double span = Math.max(1.0e-4, hpHigh - hpLow);
		double t = Mth.clamp((hpHigh - hpRatio) / span, 0.0, 1.0);
		return 1.0 + maxBonus * t;
	}
}
