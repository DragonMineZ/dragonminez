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
            this.setScaleVal(0.8f);

            this.addKiSkill(KiSkillType.KI_SMALL, 200, 1.0F, 0xAA00FF, 0x6C00A3);
            this.addKiSkill(KiSkillType.KI_LASER, 60, 1.0F, 0xAA00FF, 0x6C00A3);

            this.setWildSense(true, 200);
        }
    }

    public static class SagaFriezaSecondForm extends DBSagasEntity {

        public SagaFriezaSecondForm(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(1000000);
            }

            this.setKiBlastSpeed(1.4f);

            this.setCanFly(true);
            this.setDBZStyle(2);
            this.setAuraColor(0x971EE8);
            this.setScaleVal(1.2f);
            this.addKiSkill(KiSkillType.KI_LASER, 60, 1.0F, 0xAA00FF, 0x6C00A3);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 200, 1.1F, 0xC523DE, 0xC523DE);
            this.setAllowedCombos(150, ComboType.AIR);

            this.setWildSense(true, 180);

        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_FREEZER_THIRD.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return false;
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
            this.setScaleVal(1.3f);

            this.addKiSkill(KiSkillType.TRIPLE_LASER, 200, 1.1F, 0xC523DE, 0xC523DE);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.1F, 0xC523DE, 0xC523DE);

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

            this.addKiSkill(KiSkillType.DEATH_BALL, 250, 1.1F, 0xAA00FF, 0x6C00A3);
            this.addKiSkill(KiSkillType.KI_LASER, 60, 1.0F, 0xAA00FF, 0x6C00A3);

            this.setAllowedCombos(100, ComboType.AIR, ComboType.BASIC);

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

            this.setAllowedCombos(100, ComboType.AIR);

            this.addKiSkill(KiSkillType.DEATH_BALL, 250, 1.1F, 0xA623DE, 0xA623DE);
            this.addKiSkill(KiSkillType.KIENZAN, 100, 1.6F, 0xDE23D9, 0xDE23D9);

            this.setWildSense(true, 100);
        }
    }

    public static class SagaMechaFrieza extends DBSagasEntity {

        public SagaMechaFrieza(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(120000000);
            }

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xBF0F0F);
            this.setKiBlastSpeed(1.4f);

            this.addKiSkill(KiSkillType.DEATH_BALL, 250, 1.1F, 0xEB1E1E, 0x9E1818);
            this.addKiSkill(KiSkillType.KI_LASER, 60, 1.0F, 0xEB1E1E, 0x9E1818);

            this.setAllowedCombos(100, ComboType.AIR);
            this.setWildSense(true, 100);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_frieza_base";
        }
    }

    public static class SagaKingCold extends DBSagasEntity {

        public SagaKingCold(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(80000000);
            }

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xBF0F0F);
            this.setKiBlastSpeed(1.4f);
            this.setScaleVal(1.2f);

            this.addKiSkill(KiSkillType.TRIPLE_LASER, 200, 1.0F, 0xDE233F, 0xDE233F);
            this.addKiSkill(KiSkillType.KI_LASER, 60, 1.0F, 0xA623DE, 0xA623DE);

            this.setAllowedCombos(100, ComboType.AIR);
            this.setWildSense(true, 100);
        }

    }


}