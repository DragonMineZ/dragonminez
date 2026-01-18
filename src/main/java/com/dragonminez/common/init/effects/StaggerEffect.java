package com.dragonminez.common.init.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class StaggerEffect extends MobEffect {

    public StaggerEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFAA00);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }


}
