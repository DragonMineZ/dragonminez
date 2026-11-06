package com.dragonminez.common.racial;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public record LethalContext(float rawDamage, float finalDamage, DamageSource source, Entity attacker) {
}
