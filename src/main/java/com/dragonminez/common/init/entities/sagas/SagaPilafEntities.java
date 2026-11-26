package com.dragonminez.common.init.entities.sagas;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaPilafEntities {

    public static class PilafRobotEntity extends DBSagasEntity {
        public PilafRobotEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
            this.setScaleVal(0.8f);
            this.setDBZStyle(2);

            this.setDefaultMovementSpeed(0.45D);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.45D);

        }

    }

    public static class ShuRobotEntity extends DBSagasEntity {

        public ShuRobotEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
            this.addKiSkill(KiSkillType.KI_LASER, 400, 1.0F, 0xFF3B3B, 0xFF3B3B);
            this.setDBZStyle(2);

        }
    }

    public static class MaiRobotEntity extends DBSagasEntity {

        public MaiRobotEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(0);
            this.setScaleVal(1.2f);
            this.setDBZStyle(2);

        }

    }

    public static class FusedPilafRobotEntity extends DBSagasEntity {

        private static final int KI_COLOR = 0xFF3B3B;

        public FusedPilafRobotEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setDBZStyle(2);
            this.setScaleVal(1.2f);
            this.setKiBlastSpeed(1.2F);

            this.addKiSkill(KiSkillType.KI_LASER, 400, 1.0F, KI_COLOR, KI_COLOR);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, KI_COLOR, KI_COLOR);
        }

    }

}
