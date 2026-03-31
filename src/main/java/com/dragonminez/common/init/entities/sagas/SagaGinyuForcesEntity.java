package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaGinyuForcesEntity {

    public static class SagaGuldoEntity extends DBSagasEntity {

        public SagaGuldoEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(10000);
            }
            this.setDBZStyle(0);
            this.setAuraColor(0x55FF55);

            this.setEvade(true, 100);
            this.setKiSmall(60, 0x55FF55);
        }

    }

    public static class SagaRecoomeEntity extends DBSagasEntity {

        public SagaRecoomeEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(40000);
            }
            this.setCanFly(true);
            this.setDBZStyle(2);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.3f);

            this.setCombo(1, 140);
            this.setKiExplosion(100, 0xFF3BEA, 0xE23BFF, 5.0f);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.50D);

        }

    }


    public static class SagaBurterEntity extends DBSagasEntity {

        public SagaBurterEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(40000);
            }
            this.setCanFly(true);
            this.setDBZStyle(1);
            this.setAuraColor(0x3F58FC);

            this.setFlySpeed(0.55D);
            this.setEvade(true, 120);
            this.setWildSense(true, 200);

            this.setBlueHurricane(250, 12.0f);

        }

    }

    public static class SagaJeiceEntity extends DBSagasEntity {

        public SagaJeiceEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(40000);
            }
            this.setCanFly(true);
            this.setDBZStyle(1);
            this.setAuraColor(0xFF3333);
            this.setKiBlastSpeed(2.0f);

            this.setEvade(true, 180);

            this.setKiVolley(200,0xFF3333);
            this.setKiSmall(60,0xFF3333);

        }

    }

    public static class SagaGinyuEntity extends DBSagasEntity {

        public SagaGinyuEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(120000);
            }
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xAA00FF);

            this.setCombo(2, 160);
            this.setEvade(true, 150);
            this.setWildSense(true, 200);

            this.setGenericWave(180, 0xAA00FF, 0x6600AA, 1.5f);

            this.setSecondarySkill(11, 80);
        }


    }

    public static class SagaGinyuGokuEntity extends DBSagasEntity {

        public SagaGinyuGokuEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(120000);
            }
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xAA00FF);

            this.setCombo(2, 160);
            this.setEvade(true, 150);
            this.setWildSense(true, 200);

            this.setGenericWave(180, 0xAA00FF, 0x6600AA, 1.5f);

            this.setSecondarySkill(11, 80);
        }


    }
}