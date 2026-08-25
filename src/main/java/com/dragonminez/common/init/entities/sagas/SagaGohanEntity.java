package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SagaGohanEntity {

    public static class SagaKidGohanEntity extends DBSagasEntity {

        public SagaKidGohanEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(200000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setAllowedCombos(200, ComboType.AIR);
            this.setisKid(true);

            this.addKiSkill(KiSkillType.MASENKO, 200, 0.8F, 0xFFED6E, 0xFFED6E);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_kid_gohan";
        }

    }

    public static class SagaGohanMidBaseEntity extends DBSagasEntity {

        public SagaGohanMidBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setisKid(true);

            this.setAllowedCombos(120, ComboType.BASIC);

            this.addKiSkill(KiSkillType.MASENKO, 200, 1.2F, 0xFFE657, 0xFFE657);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_gohan_mid";
        }


        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOHAN_MID_SSJ.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }

    }

    public static class SagaGohanMidSSJEntity extends DBSagasEntity {

        public SagaGohanMidSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setisKid(true);

            this.setAllowedCombos(120, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.MASENKO, 200, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 400);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_gohan_mid";
        }


        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOHAN_MID_SSJ2.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }

    }

    public static class SagaGohanMidSSJ2Entity extends DBSagasEntity {

        public SagaGohanMidSSJ2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setLightning(true);
            this.setisKid(true);

            this.setAllowedCombos(120, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION, ComboType.AIR);

            this.addKiSkill(KiSkillType.MASENKO, 260, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 100, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 400, 2.0F, 0x45FFFF, 0x45FFFF);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_gohan_mid_ssj2";
        }

    }

    public static class SagaFutureGohanBaseEntity extends DBSagasEntity {

        public SagaFutureGohanBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.BASIC);

            this.addKiSkill(KiSkillType.MASENKO, 100, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KI_BARRIER, 300, 1.2F, 0x36E082, 0x13AD58);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_future_gohan";
        }


        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_FUTURE_GOHAN_SSJ.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }

    }

    public static class SagaFutureGohanSSJEntity extends DBSagasEntity {

        public SagaFutureGohanSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setWildSense(true, 100);
            this.setKiCharge(true);

            this.setAllowedCombos(120, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.MASENKO, 200, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 400);
            this.addKiSkill(KiSkillType.KI_BARRIER, 300, 1.2F, 0x36E082, 0x13AD58);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_future_gohanssj";
        }

    }
    /*
    SAGA BUU GOHAN
    [ BASE - ULTIMATE ]
     */
    public static class SagaGohanEndBaseEntity extends DBSagasEntity {

        public SagaGohanEndBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 50);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.BASIC);

            this.addKiSkill(KiSkillType.MASENKO, 100, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 300, 1.2F);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_gohan_end";
        }


        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOHAN_END_SSJ.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }
    }

    public static class SagaGohanEndSSJEntity extends DBSagasEntity {

        public SagaGohanEndSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.MASENKO, 200, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 280, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 400);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_gohan_end_ssj";
        }


        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOHAN_END_SSJ2.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }
    }

    public static class SagaGohanEndSSJ2Entity extends DBSagasEntity {

        public SagaGohanEndSSJ2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setLightning(true);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);

            this.addKiSkill(KiSkillType.MASENKO, 200, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 280, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 400, 1.4f);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_gohan_end_ssj2";
        }

    }

    public static class SagaGohanEndUltimateEntity extends DBSagasEntity {

        public SagaGohanEndUltimateEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.7F);
            this.setLightning(true);
            this.setDBZStyle(0);
            this.setEvade(true, 30);
            this.setWildSense(true, 60);
            this.setZanzoken(true,100);

            this.setAllowedCombos(120, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 280, 1.2F, 0xFFFFFF, 0xFFFFFF);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 2.2f);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_gohan_end_ssj2";
        }

    }

}
