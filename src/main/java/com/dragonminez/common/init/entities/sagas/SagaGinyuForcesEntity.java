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
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.0F, 0x55FF55, 0x55FF55);

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

            this.setAllowedCombos(140, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_EXPLOSION, 300, 5.0F, 0xFF3BEA, 0xE23BFF);

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

            this.addKiSkill(KiSkillType.BLUE_HURRICANE, 100, 5.0F, 0xFF3BEA, 0xE23BFF);

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

            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.0F, 0xFF3333, 0xFF3333);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 2.0F, 0xFF3333, 0xFF3333);


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

            this.setAllowedCombos(140, ComboType.KI_CHARGE_ATTACK);

            this.setEvade(true, 150);
            this.setWildSense(true, 200);

            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 180, 1.5F, 0xAA00FF, 0x6600AA);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xAA00FF, 0x6600AA);

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

            this.setAllowedCombos(140, ComboType.KI_CHARGE_ATTACK);
            this.setEvade(true, 150);
            this.setWildSense(true, 200);

            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 180, 1.5F, 0xAA00FF, 0x6600AA);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xAA00FF, 0x6600AA);

        }


    }
}