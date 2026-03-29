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

        this.setKiBlastSpeed(1.2F);
        this.setFlySpeed(0.35D);
        this.setAuraColor(0xFF00FF);

        this.setEvade(true, 60);

        this.setDBZStyle(0);
        this.setCanFly(true);

        this.setGenericWave(200, 0xD22FF5, 0x8D0FA8, 1.0f);

    }

    @Override
    protected boolean shouldTriggerTransformationOnDeath() {
        return false;
    }
}