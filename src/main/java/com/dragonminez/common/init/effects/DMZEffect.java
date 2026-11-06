package com.dragonminez.common.init.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

public class DMZEffect extends MobEffect {
	private boolean curable = true;

	public DMZEffect() {
		super(MobEffectCategory.NEUTRAL, 0xAAAAAA);
	}

    public DMZEffect(boolean beneficial) {
        super(beneficial ? MobEffectCategory.BENEFICIAL : MobEffectCategory.HARMFUL, beneficial ? 0x98D982 : 0xD98282);
    }

	public DMZEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	public DMZEffect uncurable() {
		this.curable = false;
		return this;
	}

	@Override
	public void fillEffectCures(Set<EffectCure> cures, net.minecraft.world.effect.MobEffectInstance effectInstance) {
		if (curable) super.fillEffectCures(cures, effectInstance);
		else cures.clear();
	}

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
