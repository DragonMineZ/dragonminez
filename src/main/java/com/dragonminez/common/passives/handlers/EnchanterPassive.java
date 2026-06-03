package com.dragonminez.common.passives.handlers;

import com.dragonminez.common.passives.ClassPassives;
import com.dragonminez.common.passives.IClassPassive;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.KiAttackData;

public class EnchanterPassive implements IClassPassive {

	@Override
	public String classKey() { return "enchanter"; }

	private boolean isHealBuff(KiAttackData ki) {
		return ki != null
				&& ki.getUtility() == KiAttackData.Utility.HEAL
				&& ki.getSecondaryEffectType() == KiAttackData.SecondaryEffectType.BUFF;
	}

	@Override
	public double kiCooldownMultiplier(StatsData data, KiAttackData ki) {
		if (ki == null || ki.getUtility() != KiAttackData.Utility.HEAL) return 1.0;
		double reduction = isHealBuff(ki)
				? ClassPassives.value(data, "cdSecondary", 0.15)
				: ClassPassives.value(data, "cdPrimary", 0.20);
		return Math.max(0.0, 1.0 - reduction);
	}

	@Override
	public double secondaryDurationMultiplier(StatsData data, KiAttackData ki) {
		if (isHealBuff(ki)) return 1.0 + ClassPassives.value(data, "durationBonus", 0.25);
		return 1.0;
	}
}
