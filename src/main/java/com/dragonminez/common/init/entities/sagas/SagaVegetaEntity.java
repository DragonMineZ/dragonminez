package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaVegetaEntity{

    public static class SagaVegetaExplorerEntity extends DBSagasEntity {

        public SagaVegetaExplorerEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(18000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFF48A);
            this.setKiBlastSpeed(1.2F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setDash(true, 80);

            this.setAllowedCombos(120, ComboType.AIR);

            this.addKiSkill(KiSkillType.KI_SMALL, 50, 1.2F, 0xFFF48A, 0xFFF48A);
            this.addKiSkill(KiSkillType.GALICK_GUN, 400, 1.2F);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }

    }

    public static class SagaVegetaNamekEntity extends DBSagasEntity {

        public SagaVegetaNamekEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(24000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFF48A);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.2F, 0x00C0FF, 0x00C0FF);
            this.addKiSkill(KiSkillType.GALICK_GUN, 400, 1.2F);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }

    }

    public static class SagaVegetaMidBaseEntity extends DBSagasEntity {

        public SagaVegetaMidBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(13000000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.2F, 0x00C0FF, 0x00C0FF);
            this.addKiSkill(KiSkillType.GALICK_GUN, 400, 1.2F);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }


        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_MID_SSJ.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }


    }

    public static class SagaVegetaMidSSJEntity extends DBSagasEntity {

        public SagaVegetaMidSSJEntity   (EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(650000000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.GALICK_GUN, 200, 1.2F);
            this.addKiSkill(KiSkillType.BIG_BANG, 400, 1.5F, 0xE3FFFF, 0xE3FFFF);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }


        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_MID_SSG2.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return false;
        }
    }

    public static class SagaVegetaMidSSG2Entity extends DBSagasEntity {

        public SagaVegetaMidSSG2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(650000000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.BIG_BANG, 200, 1.5F, 0xE3FFFF, 0xE3FFFF);
            this.addKiSkill(KiSkillType.FINAL_FLASH, 400, 2.0F);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta_ssg2";
        }

    }
    /*
    SAGA VEGETA FIN [BASE - SSJ2]
     */

    public static class SagaVegetaEndBaseEntity extends DBSagasEntity {

        public SagaVegetaEndBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.2F, 0x00C0FF, 0x00C0FF);
            this.addKiSkill(KiSkillType.GALICK_GUN, 400, 1.2F);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }


        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_END_SSJ.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }

    }

    public static class SagaVegetaEndSSJEntity extends DBSagasEntity {

        public SagaVegetaEndSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.GALICK_GUN, 400, 1.2F);
            this.addKiSkill(KiSkillType.BIG_BANG, 200, 1.7F, 0xE3FFFF, 0xE3FFFF);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }


        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_END_SSJ2.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }

    }

    public static class SagaVegetaEndSSJ2Entity extends DBSagasEntity {

        public SagaVegetaEndSSJ2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setWildSense(true, 100);
            this.setZanzoken(true, 200);
            this.setLightning(true);

            this.setAllowedCombos(120, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 260, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.GALICK_GUN, 100, 1.2F);
            this.addKiSkill(KiSkillType.BIG_BANG, 340, 1.7F, 0xE3FFFF, 0xE3FFFF);
            this.addKiSkill(KiSkillType.FINAL_FLASH, 400, 2.2F);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta_ssj2";
        }
    }

    public static class SagaMajinVegetaEntity extends DBSagasEntity {

        private boolean hasUsedFinalExplosion = false;

        public SagaMajinVegetaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setWildSense(true, 600);
            this.setZanzoken(true, 200);
            this.setLightning(true);

            this.setAllowedCombos(120, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 260, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.BIG_BANG, 340, 1.7F, 0xE3FFFF, 0xE3FFFF);
            this.addKiSkill(KiSkillType.FINAL_FLASH, 400, 2.2F);

        }

        @Override
        public boolean hurt(DamageSource pSource, float pAmount) {
            if (this.isCasting() && this.getSkillType() == KiSkillType.KI_EXPLOSION.getId()) {
                return false;
            }

            boolean actuallyHurt = super.hurt(pSource, pAmount);

            if (actuallyHurt && !this.level().isClientSide) {
                float umbralVida = this.getMaxHealth() * 0.15F;

                if (this.getHealth() <= umbralVida && !this.hasUsedFinalExplosion) {
                    this.hasUsedFinalExplosion = true;

                    if (this.isComboing()) {
                        this.setComboing(false);
                    }
                    this.setSkillColors(0xFFE657, 0xFFE657, 0xFFFFFF);
                    this.startCasting(KiSkillType.KI_EXPLOSION.getId());

                    this.setHealth(10.0F);
                }
            }

            return actuallyHurt;
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta_ssg2";
        }

    }
}