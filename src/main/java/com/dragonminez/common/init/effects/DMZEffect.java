package com.dragonminez.common.init.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
	public List<ItemStack> getCurativeItems() {
		return curable ? super.getCurativeItems() : new ArrayList<>();
	}

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
