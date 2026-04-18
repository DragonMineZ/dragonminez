package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaGotenEntity {

    public static class SagaGotenKidEntity extends DBSagasEntity {

        public SagaGotenKidEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);


            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setAllowedCombos(200, ComboType.AIR);
            this.setisKid(true);

            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 0.8F);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goten";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOTEN_SSJ.get();
        }


        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }
    }

    public static class SagaGotenKidSSJEntity extends DBSagasEntity {

        public SagaGotenKidSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setisKid(true);

            this.setAllowedCombos(120, ComboType.BASIC);

            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 0.8F);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 100, 0.8f, 0xFFE657, 0xFFE657);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goten_ssj";
        }

    }

    public static class SagaGotenksBaseEntity extends DBSagasEntity {

        public SagaGotenksBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(200, ComboType.KI_CHARGE_ATTACK, ComboType.AIR);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 1.3F);
            this.addKiSkill(KiSkillType.KI_SMALL, 50, 1.5F, 0x75FFFF, 0x75FFFF);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 50, 0.7F, 0x75FFFF, 0x75FFFF);
            this.setisKid(true);
            this.setWildSense(true, 150);

        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOTENKS_SSJ.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_gotenks";
        }

    }

    public static class SagaGotenksSSJEntity extends DBSagasEntity {

        public SagaGotenksSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(200, ComboType.KI_CHARGE_ATTACK, ComboType.AIR);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 1.5F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.5F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 100, 1.5F, 0xFFE657, 0xFFE657);
            this.setisKid(true);

            this.setWildSense(true, 150);
            this.setZanzoken(true, 300);

        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOTENKS_SSJ3.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {return true;}

        @Override
        public String getGeckolibModelName() {
            return "saga_gotenks";
        }

    }

    public static class SagaGotenksSSJ3Entity extends DBSagasEntity {

        public SagaGotenksSSJ3Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setLightning(true);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(200, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 3.0F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.5F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 100, 1.5F, 0xFFE657, 0xFFE657);
            this.setisKid(true);

            this.setWildSense(true, 70);
            this.setZanzoken(true, 200);

        }


        @Override
        public String getGeckolibModelName() {
            return "saga_gotenks_ssj3";
        }

    }

}
