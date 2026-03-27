package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaRaditzEntity extends DBSagasEntity {

    public SagaRaditzEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(1200);
        }

        this.setKiBlastDamage(12.0F);
        this.setKiBlastSpeed(1.2F);
        this.setFlySpeed(0.45D);
        this.setAuraColor(0xFF00FF);

        this.setEvade(true, 60);
//        this.setCombo(10, 100);
        this.setWildSense(true, 160);

        this.setDBZStyle(0);
        this.setCanFly(true);

        this.setKiWave(true, 160, 18.0F, 0xFF00FF, 0x880088, 1.5F);

    }

    @Override
    protected boolean shouldTriggerTransformationOnDeath() {
        return false;
    }
}