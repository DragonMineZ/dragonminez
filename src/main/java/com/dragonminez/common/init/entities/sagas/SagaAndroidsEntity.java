package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaAndroidsEntity {

    public static class SagaDrGeroEntity extends DBSagasEntity {

        public SagaDrGeroEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFF48A);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, 4);

            this.setMainSkill(11, 50);

        }

    }

}
