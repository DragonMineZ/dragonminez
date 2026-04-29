package com.dragonminez.common.init.entities.ki;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

public class KiAreaEntity extends AbstractKiProjectile {
    public KiAreaEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public int getMaxHits() {
        return 0;
    }
}
