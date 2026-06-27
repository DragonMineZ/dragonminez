package com.dragonminez.common.init.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class DMZEffect extends MobEffect {

	public DMZEffect() {
		super(MobEffectCategory.NEUTRAL, 0xAAAAAA);
	}

    public DMZEffect(boolean beneficial) {
        super(beneficial ? MobEffectCategory.BENEFICIAL : MobEffectCategory.HARMFUL, beneficial ? 0x98D982 : 0xD98282);
    }

	public DMZEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
