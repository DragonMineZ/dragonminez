package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaFriezaEntity {

    public static class SagaFriezaFirstForm extends DBSagasEntity {

        public SagaFriezaFirstForm(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(530000);
            }

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x971EE8);
            this.setKiBlastSpeed(1.4f);

            this.setKiSmall(60, 0xFF55FF);

            this.setSecondarySkill(4, 180, 1.0f);
            this.setWildSense(true, 200);
        }
    }

    public static class SagaFriezaSecondForm extends DBSagasEntity {

        private int transformTick = 0;

        public SagaFriezaSecondForm(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(1000000);
            }

            this.setKiBlastSpeed(1.4f);

            this.setCanFly(true);
            this.setDBZStyle(2);
            this.setAuraColor(0x971EE8);

            this.setGenericWave(200,0x971EE8, 0xFF55FF, 1.8f);
            this.setWildSense(true, 180);
        }

        @Override
        public void tick() {
            super.tick();

            if (this.isTransforming()) {
                this.transformTick++;

                boolean finished = this.handleTransformationLogic(this.transformTick, 80);

                if (finished && !this.level().isClientSide) {
                    DBSagasEntity nextForm = (DBSagasEntity) MainEntities.SAGA_FREEZER_THIRD.get().create(this.level());
                    this.finishTransformationSpawn(nextForm, false);
                }
            }
        }

        @Override
        protected boolean shouldTriggerTransformationOnDeath() {
            return true;
        }

    }

    public static class SagaFriezaThirdForm extends DBSagasEntity {

        public SagaFriezaThirdForm(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(2100000);
            }

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x971EE8);
            this.setKiBlastSpeed(1.4f);

            this.setTripleLaser(200, 0xFF0000, 0x990000);
            this.setSecondarySkill(10, 100, 1.0f);

            this.setWildSense(true, 160);
        }

    }

    public static class SagaFriezaFinalForm extends DBSagasEntity {

        public SagaFriezaFinalForm(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(120000000);
            }

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xAA00FF);
            this.setKiBlastSpeed(1.4f);

            this.setDeathBall(250, 0xAA00FF, 0x6C00A3);
            this.setSecondarySkill(4, 60, 1.0f);

            this.setCombo(1, 100);
            this.setWildSense(true, 100);
        }
    }

    public static class SagaFriezaFPForm extends DBSagasEntity {

        public SagaFriezaFPForm(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(120000000);
            }

            this.setCanFly(true);
            this.setDBZStyle(2);
            this.setAuraColor(0xFF00FF);
            this.setKiBlastSpeed(1.4f);

            this.setCombo(1, 100);
            this.setKienzan(100, 0xC01EE8, 1.2f);
            this.setWildSense(true, 100);
        }
    }


}